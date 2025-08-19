import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getSubjects, getQuestionPage, getQuestion } from '@/api/question'
import cacheManager from '@/utils/cacheManager'

export const useQuestionBankStore = defineStore('questionBank', () => {
  // 缓存管理器
  const questionBankCacheManager = cacheManager.createNamespace('questionBank', 5 * 60 * 1000) // 5分钟缓存

  // 响应式状态
  const loading = ref(false)
  const error = ref(null)
  const subjects = ref([])
  const questions = ref([])
  const total = ref(0)
  const currentSubject = ref(null)
  
  // 查询表单状态
  const queryForm = ref({
    subjectId: null,
    keyword: '',
    difficulty: '',
    questionType: '',
    current: 1,
    size: 20
  })

  // 计算属性
  const hasData = computed(() => questions.value.length > 0)
  const isEmpty = computed(() => !loading.value && questions.value.length === 0)
  const currentPage = computed(() => queryForm.value.current)
  const pageSize = computed(() => queryForm.value.size)

  // 生成缓存键
  const generateCacheKey = (type, params = {}) => {
    switch (type) {
      case 'subjects':
        return 'subjects_list'
      case 'questions':
        return `questions_${params.subjectId || 'all'}_${params.keyword || ''}_${params.difficulty || ''}_${params.questionType || ''}_${params.current || 1}_${params.size || 20}`
      default:
        return type
    }
  }

  // 加载学科列表
  const loadSubjects = async (useCache = true) => {
    const cacheKey = generateCacheKey('subjects')
    
    // 尝试从缓存获取数据
    if (useCache) {
      const cachedData = questionBankCacheManager.get(cacheKey)
      if (cachedData) {
        console.log('📦 QuestionBank Store: 使用缓存的学科数据')
        subjects.value = cachedData.data
        return cachedData.data
      }
    }

    try {
      loading.value = true
      error.value = null
      
      console.log('🌐 QuestionBank Store: 请求学科数据...')
      const response = await getSubjects()
      
      let subjectList = []
      if (response && Array.isArray(response.data)) {
        subjectList = response.data
      } else if (response && Array.isArray(response)) {
        subjectList = response
      } else {
        console.warn('⚠️ QuestionBank Store: 未识别的学科API返回格式', response)
      }
      
      subjects.value = subjectList
      
      // 缓存数据
      questionBankCacheManager.set(cacheKey, {
        data: subjectList,
        timestamp: Date.now()
      })
      
      console.log('✅ QuestionBank Store: 学科数据加载完成', subjectList.length, '个学科')
      return subjectList
      
    } catch (err) {
      console.error('❌ QuestionBank Store: 加载学科失败', err)
      error.value = err.message || '加载学科失败'
      subjects.value = []
      throw err
    } finally {
      loading.value = false
    }
  }

  // 加载题目列表
  const loadQuestions = async (useCache = true) => {
    const cacheKey = generateCacheKey('questions', queryForm.value)
    
    // 尝试从缓存获取数据
    if (useCache) {
      const cachedData = questionBankCacheManager.get(cacheKey)
      if (cachedData) {
        console.log('📦 QuestionBank Store: 使用缓存的题目数据')
        questions.value = cachedData.data
        total.value = cachedData.total
        return { data: cachedData.data, total: cachedData.total }
      }
    }

    try {
      loading.value = true
      error.value = null
      
      const params = {
        page: queryForm.value.current,
        size: queryForm.value.size
      }
      
      // 添加可选参数
      if (queryForm.value.subjectId) {
        params.subjectId = queryForm.value.subjectId
      }
      if (queryForm.value.keyword?.trim()) {
        params.keyword = queryForm.value.keyword.trim()
      }
      if (queryForm.value.difficulty) {
        params.difficulty = queryForm.value.difficulty
      }
      if (queryForm.value.questionType) {
        params.questionType = queryForm.value.questionType
      }
      
      console.log('🌐 QuestionBank Store: 请求题目数据...', params)
      const response = await getQuestionPage(params)
      
      let questionList = []
      let totalCount = 0
      
      // 适配不同的API返回格式
      if (response && Array.isArray(response.content)) {
        questionList = response.content
        totalCount = response.totalElements || response.total || 0
      } else if (response && Array.isArray(response.data)) {
        questionList = response.data
        totalCount = response.total || 0
      } else if (response && Array.isArray(response)) {
        questionList = response
        totalCount = response.length
      } else {
        console.warn('⚠️ QuestionBank Store: 未识别的题目API返回格式', response)
      }
      
      questions.value = questionList
      total.value = totalCount
      
      // 缓存数据
      questionBankCacheManager.set(cacheKey, {
        data: questionList,
        total: totalCount,
        timestamp: Date.now()
      })
      
      console.log('✅ QuestionBank Store: 题目数据加载完成', questionList.length, '条，总数:', totalCount)
      return { data: questionList, total: totalCount }
      
    } catch (err) {
      console.error('❌ QuestionBank Store: 加载题目失败', err)
      error.value = err.message || '加载题目失败'
      questions.value = []
      total.value = 0
      throw err
    } finally {
      loading.value = false
    }
  }

  // 搜索题目
  const searchQuestions = async (searchParams = {}) => {
    // 更新查询参数
    Object.assign(queryForm.value, searchParams)
    queryForm.value.current = 1 // 重置到第一页
    
    // 清除相关缓存
    const pattern = `questions_${queryForm.value.subjectId || 'all'}_`
    cacheManager.clearByPattern(pattern)
    
    return await loadQuestions(false) // 搜索时不使用缓存
  }

  // 切换学科
  const switchSubject = async (subjectId) => {
    if (queryForm.value.subjectId === subjectId) {
      return // 相同学科，无需切换
    }
    
    queryForm.value.subjectId = subjectId
    queryForm.value.current = 1 // 重置到第一页
    queryForm.value.keyword = '' // 清空搜索关键词
    queryForm.value.difficulty = '' // 清空难度筛选
    queryForm.value.questionType = '' // 清空题型筛选
    
    // 更新当前学科
    currentSubject.value = subjects.value.find(s => s.id === subjectId) || null
    
    return await loadQuestions(true) // 切换学科时使用缓存
  }

  // 分页处理
  const handleCurrentChange = async (page) => {
    queryForm.value.current = page
    return await loadQuestions(true)
  }

  const handleSizeChange = async (size) => {
    queryForm.value.size = size
    queryForm.value.current = 1 // 重置到第一页
    return await loadQuestions(true)
  }

  // 获取题目详情
  const getQuestionDetail = async (questionId, useCache = true) => {
    const cacheKey = `question_detail_${questionId}`
    
    // 尝试从缓存获取
    if (useCache) {
      const cachedData = cacheManager.get(cacheKey)
      if (cachedData) {
        console.log('📦 QuestionBank Store: 使用缓存的题目详情')
        return cachedData.data
      }
    }

    try {
      console.log('🌐 QuestionBank Store: 请求题目详情...', questionId)
      const response = await getQuestion(questionId)
      
      let questionDetail = null
      if (response && response.data) {
        questionDetail = response.data
      } else if (response) {
        questionDetail = response
      }
      
      // 缓存数据
      if (questionDetail) {
        cacheManager.set(cacheKey, {
          data: questionDetail,
          timestamp: Date.now()
        })
      }
      
      console.log('✅ QuestionBank Store: 题目详情加载完成')
      return questionDetail
      
    } catch (err) {
      console.error('❌ QuestionBank Store: 获取题目详情失败', err)
      throw err
    }
  }

  // 初始化数据
  const initializeData = async () => {
    try {
      console.log('🚀 QuestionBank Store: 初始化数据...')
      
      // 并行加载学科和题目数据
      const promises = [
        loadSubjects(true), // 优先使用缓存
      ]
      
      // 如果有选中的学科，加载对应题目
      if (queryForm.value.subjectId) {
        promises.push(loadQuestions(true))
      }
      
      await Promise.all(promises)
      
      console.log('✅ QuestionBank Store: 数据初始化完成')
    } catch (err) {
      console.error('❌ QuestionBank Store: 初始化失败', err)
      error.value = err.message || '初始化失败'
    }
  }

  // 刷新数据
  const refreshData = async () => {
    console.log('🔄 QuestionBank Store: 刷新数据...')
    
    // 清除所有缓存
    cacheManager.clear()
    
    // 重新加载数据
    await initializeData()
  }

  // 清除缓存
  const clearCache = () => {
    console.log('🗑️ QuestionBank Store: 清除缓存')
    cacheManager.clear()
  }

  // 重置状态
  const resetState = () => {
    console.log('🔄 QuestionBank Store: 重置状态')
    loading.value = false
    error.value = null
    subjects.value = []
    questions.value = []
    total.value = 0
    currentSubject.value = null
    queryForm.value = {
      subjectId: null,
      keyword: '',
      difficulty: '',
      questionType: '',
      current: 1,
      size: 20
    }
  }

  return {
    // 状态
    loading,
    error,
    subjects,
    questions,
    total,
    currentSubject,
    queryForm,
    
    // 计算属性
    hasData,
    isEmpty,
    currentPage,
    pageSize,
    
    // 方法
    loadSubjects,
    loadQuestions,
    searchQuestions,
    switchSubject,
    handleCurrentChange,
    handleSizeChange,
    getQuestionDetail,
    initializeData,
    refreshData,
    clearCache,
    resetState
  }
})