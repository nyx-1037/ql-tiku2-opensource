import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { practiceRecordAPI, questionAPI, statisticsAPI } from '@/api'
import { useLoadingState } from '@/composables/useLoadingState'
import { useErrorHandler } from '@/composables/useErrorHandler'

export const useAnalyticsStore = defineStore('analytics', () => {
  // 内置缓存管理器
  const analyticsCacheManager = {
    cache: new Map(),
    ttl: 10 * 60 * 1000, // 10分钟缓存
    
    set(key, value) {
      this.cache.set(key, {
        data: value,
        timestamp: Date.now()
      })
    },
    
    get(key) {
      const item = this.cache.get(key)
      if (!item) return null
      
      if (Date.now() - item.timestamp > this.ttl) {
        this.cache.delete(key)
        return null
      }
      
      return item
    },
    
    delete(key) {
      this.cache.delete(key)
    },
    
    clear() {
      this.cache.clear()
    }
  }

  // 加载状态和错误处理
  const loadingState = useLoadingState()
  const errorHandler = useErrorHandler()

  // 响应式状态
  const loading = ref(false)
  const error = ref(null)
  
  // 统计数据
  const studyStats = ref({
    totalQuestions: 0,
    correctQuestions: 0,
    totalStudyTime: 0,
    studyDays: 0,
    averageAccuracy: 0,
    recentAccuracy: 0
  })
  
  // 学习趋势数据
  const studyTrends = ref([])
  
  // 科目分析数据
  const subjectAnalysis = ref([])
  
  // 错题分析数据
  const wrongQuestionStats = ref({
    totalWrongQuestions: 0,
    reviewedQuestions: 0,
    reviewProgress: 0,
    commonMistakes: []
  })
  
  // 学习建议
  const studyRecommendations = ref([])

  // 计算属性
  const hasData = computed(() => studyStats.value.totalQuestions > 0)
  const isEmpty = computed(() => !loading.value && studyStats.value.totalQuestions === 0)
  const accuracyRate = computed(() => {
    if (studyStats.value.totalQuestions === 0) return 0
    return Math.round((studyStats.value.correctQuestions / studyStats.value.totalQuestions) * 100)
  })

  // 生成缓存键
  const generateCacheKey = (type, params = {}) => {
    switch (type) {
      case 'studyStats':
        return 'study_statistics'
      case 'studyTrends':
        return `study_trends_${params.days || 30}`
      case 'subjectAnalysis':
        return 'subject_analysis'
      case 'wrongQuestionStats':
        return 'wrong_question_statistics'
      case 'studyRecommendations':
        return 'study_recommendations'
      default:
        return type
    }
  }

  // 加载学习统计数据 - 使用getOverviewStats获取完整统计数据
  const loadStudyStats = async (useCache = true) => {
    const cacheKey = generateCacheKey('studyStats')
    
    // 尝试从缓存获取数据
    if (useCache) {
      const cachedData = analyticsCacheManager.get(cacheKey)
      if (cachedData) {
        console.log('📦 Analytics Store: 使用缓存的学习统计数据')
        studyStats.value = cachedData.data
        
        // 后台静默更新
        setTimeout(() => {
          loadStudyStats(false).catch(err => {
            errorHandler.handleCacheError(err, 'background update studyStats')
          })
        }, 100)
        
        return cachedData.data
      }
    }

    return await loadingState.executeAsync(async () => {
      console.log('🌐 Analytics Store: 请求学习统计数据...')
      // 使用getOverviewStats接口获取完整的统计数据
      const response = await statisticsAPI.getOverviewStats({
        dateRange: '', // 空字符串表示获取全部数据
        subjectId: null
      })
      
      let stats = {
        totalQuestions: 0,
        correctQuestions: 0,
        totalStudyTime: 0,
        studyDays: 0,
        averageAccuracy: 0,
        recentAccuracy: 0
      }
      
      // 处理后端getOverviewStats返回的数据结构
      if (response) {
        console.log('🔍 Analytics Store: 原始API响应数据', response)
        
        // getOverviewStats直接返回统计数据，确保字段名匹配
        stats.totalQuestions = response.totalQuestions || 0
        stats.correctRate = response.correctRate || 0  // 修正字段名
        stats.averageAccuracy = response.correctRate || 0
        stats.recentAccuracy = response.correctRate || 0
        stats.studyDays = response.studyDays || 0
        stats.studyTime = response.studyTime || 0  // 修正字段名
        stats.totalStudyTime = response.studyTime || 0
        
        // 根据总题数和正确率计算正确题数
        if (stats.totalQuestions > 0 && stats.averageAccuracy > 0) {
          stats.correctQuestions = Math.round(stats.totalQuestions * stats.averageAccuracy / 100)
        } else {
          stats.correctQuestions = 0
        }
        
        console.log('📊 Analytics Store: 处理后的统计数据', stats)
      }
      
      studyStats.value = stats
      
      // 缓存数据
      analyticsCacheManager.set(cacheKey, {
        data: stats,
        timestamp: Date.now()
      })
      
      console.log('✅ Analytics Store: 学习统计数据加载完成', stats)
      return stats
      
    }, {
      loadingType: useCache ? 'normal' : 'refresh',
      showError: true,
      autoRetry: true
    }).catch(err => {
      console.error('加载学习统计失败:', err)
      
      // 使用默认数据
      studyStats.value = {
        totalQuestions: 0,
        correctQuestions: 0,
        totalStudyTime: 0,
        studyDays: 0,
        averageAccuracy: 0,
        recentAccuracy: 0
      }
      throw err
    })
  }

  // 加载学习趋势数据
  const loadStudyTrends = async (days = 30, useCache = true) => {
    const cacheKey = generateCacheKey('studyTrends', { days })
    
    // 尝试从缓存获取数据
    if (useCache) {
      const cachedData = analyticsCacheManager.get(cacheKey)
      if (cachedData) {
        console.log('📦 Analytics Store: 使用缓存的学习趋势数据')
        studyTrends.value = cachedData.data
        
        // 后台静默更新
        setTimeout(() => {
          loadStudyTrends(days, false).catch(err => {
            console.error('后台更新学习趋势失败:', err)
          })
        }, 100)
        
        return cachedData.data
      }
    }

    return await loadingState.executeAsync(async () => {
      console.log('🌐 Analytics Store: 请求学习趋势数据...', days, '天')
      const response = await practiceRecordAPI.getStudyTrends({ days })
      
      let trends = []
      if (response && Array.isArray(response.data)) {
        trends = response.data
      } else if (response && Array.isArray(response)) {
        trends = response
      } else {
        console.warn('⚠️ Analytics Store: 未识别的学习趋势API返回格式', response)
      }
      
      studyTrends.value = trends
      
      // 缓存数据
      analyticsCacheManager.set(cacheKey, {
        data: trends,
        timestamp: Date.now()
      })
      
      console.log('✅ Analytics Store: 学习趋势数据加载完成', trends.length, '条记录')
      return trends
      
    }, {
      loadingType: useCache ? 'normal' : 'refresh',
      showError: true,
      autoRetry: true
    }).catch(err => {
      errorHandler.handleApiError(err, 'loadStudyTrends')
      studyTrends.value = []
      throw err
    })
  }

  // 加载科目分析数据
  const loadSubjectAnalysis = async (useCache = true) => {
    const cacheKey = generateCacheKey('subjectAnalysis')
    
    // 尝试从缓存获取数据
    if (useCache) {
      const cachedData = analyticsCacheManager.get(cacheKey)
      if (cachedData) {
        console.log('📦 Analytics Store: 使用缓存的科目分析数据')
        subjectAnalysis.value = cachedData.data
        return cachedData.data
      }
    }

    try {
      console.log('🌐 Analytics Store: 请求科目分析数据...')
      const response = await practiceRecordAPI.getSubjectAnalysis()
      
      let analysis = []
      if (response && Array.isArray(response.data)) {
        analysis = response.data
      } else if (response && Array.isArray(response)) {
        analysis = response
      } else {
        console.warn('⚠️ Analytics Store: 未识别的科目分析API返回格式', response)
      }
      
      subjectAnalysis.value = analysis
      
      // 缓存数据
      analyticsCacheManager.set(cacheKey, {
        data: analysis,
        timestamp: Date.now()
      })
      
      console.log('✅ Analytics Store: 科目分析数据加载完成', analysis.length, '个科目')
      return analysis
      
    } catch (err) {
      console.error('❌ Analytics Store: 加载科目分析失败', err)
      subjectAnalysis.value = []
      throw err
    }
  }

  // 加载错题统计数据
  const loadWrongQuestionStats = async (useCache = true) => {
    const cacheKey = generateCacheKey('wrongQuestionStats')
    
    // 尝试从缓存获取数据
    if (useCache) {
      const cachedData = analyticsCacheManager.get(cacheKey)
      if (cachedData) {
        console.log('📦 Analytics Store: 使用缓存的错题统计数据')
        wrongQuestionStats.value = cachedData.data
        return cachedData.data
      }
    }

    try {
      console.log('🌐 Analytics Store: 请求错题统计数据...')
      const response = await practiceRecordAPI.getWrongQuestionStats()
      
      let stats = {
        totalWrongQuestions: 0,
        reviewedQuestions: 0,
        reviewProgress: 0,
        commonMistakes: []
      }
      
      if (response && response.data) {
        stats = { ...stats, ...response.data }
      } else if (response) {
        stats = { ...stats, ...response }
      }
      
      wrongQuestionStats.value = stats
      
      // 缓存数据
      analyticsCacheManager.set(cacheKey, {
        data: stats,
        timestamp: Date.now()
      })
      
      console.log('✅ Analytics Store: 错题统计数据加载完成', stats)
      return stats
      
    } catch (err) {
      console.error('❌ Analytics Store: 加载错题统计失败', err)
      wrongQuestionStats.value = {
        totalWrongQuestions: 0,
        reviewedQuestions: 0,
        reviewProgress: 0,
        commonMistakes: []
      }
      throw err
    }
  }

  // 加载学习建议
  const loadStudyRecommendations = async (useCache = true) => {
    const cacheKey = generateCacheKey('studyRecommendations')
    
    // 尝试从缓存获取数据
    if (useCache) {
      const cachedData = analyticsCacheManager.get(cacheKey)
      if (cachedData) {
        console.log('📦 Analytics Store: 使用缓存的学习建议数据')
        studyRecommendations.value = cachedData.data
        return cachedData.data
      }
    }

    try {
      console.log('🌐 Analytics Store: 请求学习建议数据...')
      const response = await practiceRecordAPI.getStudyRecommendations()
      
      let recommendations = []
      if (response && Array.isArray(response.data)) {
        recommendations = response.data
      } else if (response && Array.isArray(response)) {
        recommendations = response
      } else {
        console.warn('⚠️ Analytics Store: 未识别的学习建议API返回格式', response)
      }
      
      studyRecommendations.value = recommendations
      
      // 缓存数据
      analyticsCacheManager.set(cacheKey, {
        data: recommendations,
        timestamp: Date.now()
      })
      
      console.log('✅ Analytics Store: 学习建议数据加载完成', recommendations.length, '条建议')
      return recommendations
      
    } catch (err) {
      console.error('❌ Analytics Store: 加载学习建议失败', err)
      studyRecommendations.value = []
      throw err
    }
  }

  // 初始化所有数据
  const initializeData = async () => {
    return await loadingState.executeAsync(async () => {
      console.log('🚀 Analytics Store: 初始化数据...')
      
      // 并行加载所有数据
      await Promise.all([
        loadStudyStats(true),
        loadStudyTrends(30, true),
        loadSubjectAnalysis(true),
        loadWrongQuestionStats(true),
        loadStudyRecommendations(true)
      ])
      
      console.log('✅ Analytics Store: 数据初始化完成')
    }, {
      loadingType: 'initial',
      showError: true,
      autoRetry: false
    }).catch(err => {
      errorHandler.handleApiError(err, 'initializeData')
      error.value = err.message || '初始化失败'
      throw err
    })
  }

  // 刷新所有数据
  const refreshAllData = async () => {
    console.log('🔄 Analytics Store: 刷新所有数据...')
    
    // 清除所有缓存
    analyticsCacheManager.clear()
    
    // 重新加载数据
    await initializeData()
  }

  // 刷新特定数据
  const refreshData = async (dataType) => {
    console.log('🔄 Analytics Store: 刷新数据类型:', dataType)
    
    switch (dataType) {
      case 'studyStats':
        analyticsCacheManager.delete(generateCacheKey('studyStats'))
        return await loadStudyStats(false)
      case 'studyTrends':
        analyticsCacheManager.delete(generateCacheKey('studyTrends', { days: 30 }))
        return await loadStudyTrends(30, false)
      case 'subjectAnalysis':
        analyticsCacheManager.delete(generateCacheKey('subjectAnalysis'))
        return await loadSubjectAnalysis(false)
      case 'wrongQuestionStats':
        analyticsCacheManager.delete(generateCacheKey('wrongQuestionStats'))
        return await loadWrongQuestionStats(false)
      case 'studyRecommendations':
        analyticsCacheManager.delete(generateCacheKey('studyRecommendations'))
        return await loadStudyRecommendations(false)
      default:
        console.warn('⚠️ Analytics Store: 未知的数据类型', dataType)
    }
  }

  // 清除缓存
  const clearCache = () => {
    console.log('🗑️ Analytics Store: 清除缓存')
    analyticsCacheManager.clear()
  }

  // 重置状态
  const resetState = () => {
    console.log('🔄 Analytics Store: 重置状态')
    loading.value = false
    error.value = null
    studyStats.value = {
      totalQuestions: 0,
      correctQuestions: 0,
      totalStudyTime: 0,
      studyDays: 0,
      averageAccuracy: 0,
      recentAccuracy: 0
    }
    studyTrends.value = []
    subjectAnalysis.value = []
    wrongQuestionStats.value = {
      totalWrongQuestions: 0,
      reviewedQuestions: 0,
      reviewProgress: 0,
      commonMistakes: []
    }
    studyRecommendations.value = []
  }

  return {
    // 状态
    loading,
    error,
    studyStats,
    studyTrends,
    subjectAnalysis,
    wrongQuestionStats,
    studyRecommendations,
    
    // 加载状态
    ...loadingState,
    
    // 错误处理
    errorHandler,
    
    // 计算属性
    hasData,
    isEmpty,
    accuracyRate,
    
    // 方法
    loadStudyStats,
    loadStudyTrends,
    loadSubjectAnalysis,
    loadWrongQuestionStats,
    loadStudyRecommendations,
    initializeData,
    refreshAllData,
    refreshData,
    clearCache,
    resetState
  }
})