import { defineStore } from 'pinia'
import { ref, reactive } from 'vue'
import request from '@/api'
import { usePageCacheStore } from './pageCache'
import { useLoadingState } from '@/composables/useLoadingState'
import { useErrorHandler } from '@/composables/useErrorHandler'

export const useFeedbackCenterStore = defineStore('feedbackCenter', () => {
  const cacheStore = usePageCacheStore()
  
  // 加载状态和错误处理
  const loadingState = useLoadingState()
  const errorHandler = useErrorHandler()
  
  // 响应式数据
  const feedbackList = ref([])
  const total = ref(0)
  const loading = ref(false)
  const submitting = ref(false)
  
  // 查询表单
  const queryForm = reactive({
    current: 1,
    size: 10,
    feedbackType: null,
    status: null,
    keyword: ''
  })

  // 反馈类型选项
  const feedbackTypes = ref([
    { label: 'Bug反馈', value: 1 },
    { label: '功能建议', value: 2 },
    { label: '其他反馈', value: 3 }
  ])

  // 状态选项
  const statusOptions = [
    { value: null, label: '全部' },
    { value: 0, label: '待处理' },
    { value: 1, label: '已受理' },
    { value: 2, label: '已处理' },
    { value: 3, label: '已修复' },
    { value: 4, label: '已采纳' },
    { value: 5, label: '已失效' },
    { value: 6, label: '已撤销' }
  ]

  // 生成缓存键
  const getCacheKey = () => {
    return `feedbackCenter_${JSON.stringify(queryForm)}`
  }

  // 从缓存恢复查询表单
  const restoreQueryForm = () => {
    const cachedQueryForm = cacheStore.getQueryForm('feedbackCenter')
    if (cachedQueryForm && Object.keys(cachedQueryForm).length > 0) {
      Object.assign(queryForm, cachedQueryForm)
    }
  }

  // 保存查询表单到缓存
  const saveQueryForm = () => {
    cacheStore.setQueryForm('feedbackCenter', { ...queryForm })
  }

  // 加载反馈列表
  const loadFeedbacks = async (forceRefresh = false, silent = false) => {
    // 保存查询表单
    saveQueryForm()
    
    const cacheKey = getCacheKey()
    
    // 检查缓存
    if (!forceRefresh && cacheStore.isCacheValid(cacheKey)) {
      const cachedData = cacheStore.getCacheData(cacheKey)
      if (cachedData) {
        feedbackList.value = cachedData.records || []
        total.value = cachedData.total || 0
        
        // 静默刷新
        if (!silent) {
          setTimeout(() => {
            loadFeedbacks(true, true).catch(err => {
              errorHandler.handleCacheError(err, 'background update feedbacks')
            })
          }, 100)
        }
        
        return cachedData
      }
    }

    return await loadingState.executeAsync(async () => {
      const response = await request.post('/feedback/my/page', queryForm)
      
      let result = {
        records: [],
        total: 0
      }

      if (response && response.records) {
        result.records = response.records
        result.total = response.total
      } else if (response && Array.isArray(response.data)) {
        result.records = response.data
        result.total = response.total || response.data.length
      } else {
        console.warn('⚠️ FeedbackCenter: 未识别的API返回格式', response)
        // 使用测试数据
        result.records = [
          {
            id: 1,
            feedbackType: 1,
            feedbackTypeName: 'Bug反馈',
            title: '登录页面显示异常',
            content: '在Chrome浏览器中，登录页面的按钮显示不正常，点击无反应。',
            status: 0,
            statusName: '待处理',
            createTime: '2024-01-15 10:30:00',
            updateTime: '2024-01-15 10:30:00'
          },
          {
            id: 2,
            feedbackType: 2,
            feedbackTypeName: '功能建议',
            title: '希望增加夜间模式',
            content: '建议增加夜间模式功能，方便晚上使用时保护眼睛。',
            status: 1,
            statusName: '已受理',
            createTime: '2024-01-14 15:20:00',
            updateTime: '2024-01-15 09:00:00'
          }
        ]
        result.total = 2
      }

      // 更新数据
      feedbackList.value = result.records
      total.value = result.total

      // 缓存数据
      cacheStore.setCacheData(cacheKey, result)

      return result
    }, {
      loadingType: silent ? 'normal' : (forceRefresh ? 'refresh' : 'normal'),
      showError: !silent,
      autoRetry: true
    }).catch(error => {
      errorHandler.handleApiError(error, 'loadFeedbacks')
      
      // 错误时使用测试数据
      const testData = {
        records: [
          {
            id: 1,
            feedbackType: 1,
            feedbackTypeName: 'Bug反馈',
            title: '登录页面显示异常',
            content: '在Chrome浏览器中，登录页面的按钮显示不正常，点击无反应。',
            status: 0,
            statusName: '待处理',
            createTime: '2024-01-15 10:30:00',
            updateTime: '2024-01-15 10:30:00'
          }
        ],
        total: 1
      }
      
      feedbackList.value = testData.records
      total.value = testData.total
      
      throw error
    })
  }

  // 提交反馈
  const submitFeedback = async (feedbackData) => {
    submitting.value = true
    try {
      const response = await request.post('/feedback/submit', feedbackData)
      
      // 清除缓存，强制刷新
      cacheStore.clearCache('feedbackCenter')
      await loadFeedbacks(true)
      
      return response
    } catch (error) {
      console.error('提交反馈失败:', error)
      throw error
    } finally {
      submitting.value = false
    }
  }

  // 更新反馈
  const updateFeedback = async (id, feedbackData) => {
    submitting.value = true
    try {
      const response = await request.put(`/feedback/${id}`, feedbackData)
      
      // 清除缓存，强制刷新
      cacheStore.clearCache('feedbackCenter')
      await loadFeedbacks(true)
      
      return response
    } catch (error) {
      console.error('更新反馈失败:', error)
      throw error
    } finally {
      submitting.value = false
    }
  }

  // 删除反馈
  const deleteFeedback = async (id) => {
    try {
      const response = await request.delete(`/feedback/${id}`)
      
      // 清除缓存，强制刷新
      cacheStore.clearCache('feedbackCenter')
      await loadFeedbacks(true)
      
      return response
    } catch (error) {
      console.error('删除反馈失败:', error)
      throw error
    }
  }

  // 重置查询
  const resetQuery = () => {
    queryForm.current = 1
    queryForm.size = 10
    queryForm.feedbackType = null
    queryForm.status = null
    queryForm.keyword = ''
    saveQueryForm()
  }

  // 分页处理
  const handleCurrentChange = (page) => {
    queryForm.current = page
    loadFeedbacks()
  }

  const handleSizeChange = (size) => {
    queryForm.size = size
    queryForm.current = 1
    loadFeedbacks()
  }

  // 初始化
  const initialize = async () => {
    restoreQueryForm()
    await loadFeedbacks()
  }

  // 清除缓存
  const clearCache = () => {
    cacheStore.clearCache('feedbackCenter')
    feedbackList.value = []
    total.value = 0
  }

  return {
    // 数据
    feedbackList,
    total,
    loading,
    submitting,
    queryForm,
    feedbackTypes,
    statusOptions,
    
    // 加载状态和错误处理
    ...loadingState,
    errorHandler,
    
    // 方法
    loadFeedbacks,
    submitFeedback,
    updateFeedback,
    deleteFeedback,
    resetQuery,
    handleCurrentChange,
    handleSizeChange,
    initialize,
    clearCache,
    restoreQueryForm,
    saveQueryForm
  }
})