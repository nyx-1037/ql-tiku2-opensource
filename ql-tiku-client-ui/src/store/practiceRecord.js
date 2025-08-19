import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { practiceRecordAPI } from '@/api/index'

export const usePracticeRecordStore = defineStore('practiceRecord', () => {
  // 状态数据
  const practiceRecords = ref([])
  const total = ref(0)
  const loading = ref(false)
  const submitting = ref(false)
  
  // 错误处理状态
  const error = ref(null)
  const retryCount = ref(0)
  const maxRetries = ref(3)
  
  // 查询表单
  const queryForm = ref({
    current: 1,
    size: 10,
    subjectId: null,
    questionType: null,
    difficulty: null,
    startDate: null,
    endDate: null,
    keyword: ''
  })

  // 缓存相关
  const cacheTimestamp = ref(0)
  const CACHE_DURATION = 5 * 60 * 1000 // 5分钟缓存

  // 计算属性
  const isLoading = computed(() => loading.value)
  const isEmpty = computed(() => practiceRecords.value.length === 0)
  const hasData = computed(() => practiceRecords.value.length > 0)

  // 缓存验证
  const isCacheValid = () => {
    return Date.now() - cacheTimestamp.value < CACHE_DURATION
  }

  // 设置加载状态
  const setLoading = (state) => {
    loading.value = state
  }

  // 设置数据和缓存
  const setData = (data, totalCount = 0) => {
    practiceRecords.value = data || []
    total.value = totalCount
    cacheTimestamp.value = Date.now()
    error.value = null // 清除错误状态
  }

  // 清除缓存
  const clearCache = () => {
    practiceRecords.value = []
    total.value = 0
    cacheTimestamp.value = 0
    error.value = null
  }

  // 错误处理
  const setError = (err) => {
    error.value = err
    loading.value = false
  }

  const clearError = () => {
    error.value = null
    retryCount.value = 0
  }

  const retry = async () => {
    if (retryCount.value < maxRetries.value) {
      retryCount.value++
      clearError()
      return await loadPracticeRecords(false)
    }
  }

  // 获取练习记录列表
  const loadPracticeRecords = async (useCache = true) => {
    try {
      console.log('🔄 [PracticeRecord Store] 开始加载练习记录', { useCache, cacheValid: isCacheValid() })
      
      // 如果有有效缓存且允许使用缓存，直接返回
      if (useCache && isCacheValid() && practiceRecords.value.length > 0) {
        console.log('✅ [PracticeRecord Store] 使用缓存数据')
        return { records: practiceRecords.value, total: total.value }
      }

      setLoading(true)
      clearError()

      const params = {
        page: queryForm.value.current,
        size: queryForm.value.size,
        subjectId: queryForm.value.subjectId,
        questionType: queryForm.value.questionType,
        difficulty: queryForm.value.difficulty,
        startDate: queryForm.value.startDate,
        endDate: queryForm.value.endDate,
        keyword: queryForm.value.keyword
      }

      // 清理空值参数
      Object.keys(params).forEach(key => {
        if (params[key] === null || params[key] === undefined || params[key] === '') {
          delete params[key]
        }
      })

      console.log('📤 [PracticeRecord Store] 发送请求参数:', params)

      const response = await practiceRecordAPI.getPracticeRecords(params)
      console.log('📥 [PracticeRecord Store] 收到响应:', response)
      
      // 处理分页响应数据
      if (response && typeof response === 'object') {
        let records = []
        let totalCount = 0
        
        // 处理不同的响应格式
        if (response.records && Array.isArray(response.records)) {
          // MyBatis Plus Page 格式
          records = response.records
          totalCount = response.total || 0
        } else if (Array.isArray(response)) {
          // 直接数组格式
          records = response
          totalCount = response.length
        } else if (response.data) {
          // 嵌套 data 格式
          if (response.data.records && Array.isArray(response.data.records)) {
            records = response.data.records
            totalCount = response.data.total || 0
          } else if (Array.isArray(response.data)) {
            records = response.data
            totalCount = response.data.length
          }
        }
        
        console.log('✅ [PracticeRecord Store] 处理后的数据:', { records: records.length, total: totalCount })
        setData(records, totalCount)
        return { records, total: totalCount }
      } else {
        throw new Error('响应数据格式错误')
      }
    } catch (error) {
      console.error('❌ [PracticeRecord Store] 加载练习记录失败:', error)
      setError(error)
      throw new Error('获取练习记录失败')
    } finally {
      setLoading(false)
    }
  }

  // 获取练习记录详情
  const getPracticeRecordDetail = async (recordId) => {
    try {
      console.log('🔄 [PracticeRecord Store] 获取练习记录详情:', recordId)
      const response = await practiceRecordAPI.getPracticeRecordDetails(recordId)
      console.log('📥 [PracticeRecord Store] 练习记录详情响应:', response)
      return response || []
    } catch (error) {
      console.error('❌ [PracticeRecord Store] 获取练习记录详情失败:', error)
      throw error
    }
  }

  // 重置查询条件
  const resetQuery = () => {
    queryForm.value = {
      current: 1,
      size: 10,
      subjectId: null,
      questionType: null,
      difficulty: null,
      startDate: null,
      endDate: null,
      keyword: ''
    }
    clearCache()
  }

  // 分页处理
  const handleCurrentChange = (page) => {
    queryForm.value.current = page
    loadPracticeRecords(false) // 分页时不使用缓存
  }

  const handleSizeChange = (size) => {
    queryForm.value.size = size
    queryForm.value.current = 1
    loadPracticeRecords(false) // 改变页面大小时不使用缓存
  }

  // 初始化方法 - 实现缓存优先策略
  const initialize = async () => {
    try {
      console.log('🚀 [PracticeRecord Store] 初始化开始')
      
      // 先检查是否有有效缓存
      if (isCacheValid() && practiceRecords.value.length > 0) {
        console.log('✅ [PracticeRecord Store] 使用缓存，后台静默更新')
        // 有缓存，后台静默更新
        loadPracticeRecords(false).catch(error => {
          console.warn('⚠️ [PracticeRecord Store] 后台更新练习记录失败:', error)
        })
        return { records: practiceRecords.value, total: total.value }
      } else {
        console.log('🔄 [PracticeRecord Store] 无缓存，直接加载')
        // 无缓存，直接加载
        return await loadPracticeRecords(false)
      }
    } catch (error) {
      console.error('❌ [PracticeRecord Store] 初始化练习记录失败:', error)
      setError(error)
      throw error
    }
  }

  // 刷新数据
  const refreshData = () => {
    clearError()
    return loadPracticeRecords(false)
  }

  return {
    // 状态
    practiceRecords: computed(() => practiceRecords.value),
    total: computed(() => total.value),
    loading: computed(() => loading.value),
    submitting: computed(() => submitting.value),
    queryForm: computed(() => queryForm.value),
    
    // 错误处理状态
    error: computed(() => error.value),
    retryCount: computed(() => retryCount.value),
    maxRetries: computed(() => maxRetries.value),
    
    // 计算属性
    isLoading,
    isEmpty,
    hasData,
    
    // 方法
    loadPracticeRecords,
    getPracticeRecordDetail,
    resetQuery,
    handleCurrentChange,
    handleSizeChange,
    initialize,
    refreshData,
    clearCache,
    setLoading,
    setData,
    isCacheValid,
    
    // 错误处理方法
    clearError,
    retry,
    setError
  }
})