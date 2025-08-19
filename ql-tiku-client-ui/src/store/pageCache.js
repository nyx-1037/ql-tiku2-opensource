import { defineStore } from 'pinia'
import { ref } from 'vue'

export const usePageCacheStore = defineStore('pageCache', () => {
  // 缓存数据结构
  const cacheData = ref({
    analytics: {
      data: null,
      timestamp: null,
      loading: false
    },
    feedbackCenter: {
      data: null,
      timestamp: null,
      loading: false,
      queryForm: {
        current: 1,
        size: 10,
        feedbackType: null,
        status: null,
        keyword: ''
      }
    },
    practiceRecord: {
      data: null,
      timestamp: null,
      loading: false,
      queryForm: {
        current: 1,
        size: 10,
        subjectId: null,
        startDate: null,
        endDate: null
      }
    },
    resourceLibrary: {
      data: null,
      timestamp: null,
      loading: false,
      queryForm: {
        current: 1,
        size: 10,
        keyword: ''
      }
    },
    questionBank: {
      data: null,
      timestamp: null,
      loading: false,
      searchKeyword: '',
      currentPage: 1,
      pageSize: 10
    }
  })

  // 缓存过期时间（5分钟）
  const CACHE_EXPIRE_TIME = 5 * 60 * 1000

  // 检查缓存是否有效
  const isCacheValid = (pageName) => {
    const cache = cacheData.value[pageName]
    if (!cache || !cache.data || !cache.timestamp) {
      return false
    }
    return Date.now() - cache.timestamp < CACHE_EXPIRE_TIME
  }

  // 获取缓存数据
  const getCacheData = (pageName) => {
    if (isCacheValid(pageName)) {
      return cacheData.value[pageName].data
    }
    return null
  }

  // 设置缓存数据
  const setCacheData = (pageName, data) => {
    if (!cacheData.value[pageName]) {
      cacheData.value[pageName] = {
        data: null,
        timestamp: null,
        loading: false
      }
    }
    cacheData.value[pageName].data = data
    cacheData.value[pageName].timestamp = Date.now()
  }

  // 设置加载状态
  const setLoading = (pageName, loading) => {
    if (!cacheData.value[pageName]) {
      cacheData.value[pageName] = {
        data: null,
        timestamp: null,
        loading: false
      }
    }
    cacheData.value[pageName].loading = loading
  }

  // 获取加载状态
  const getLoading = (pageName) => {
    return cacheData.value[pageName]?.loading || false
  }

  // 清除指定页面缓存
  const clearCache = (pageName) => {
    if (cacheData.value[pageName]) {
      cacheData.value[pageName].data = null
      cacheData.value[pageName].timestamp = null
    }
  }

  // 清除所有缓存
  const clearAllCache = () => {
    Object.keys(cacheData.value).forEach(pageName => {
      clearCache(pageName)
    })
  }

  // 获取查询表单缓存
  const getQueryForm = (pageName) => {
    return cacheData.value[pageName]?.queryForm || {}
  }

  // 设置查询表单缓存
  const setQueryForm = (pageName, queryForm) => {
    if (!cacheData.value[pageName]) {
      cacheData.value[pageName] = {
        data: null,
        timestamp: null,
        loading: false
      }
    }
    cacheData.value[pageName].queryForm = { ...queryForm }
  }

  return {
    // 通用方法
    isCacheValid,
    getCacheData,
    setCacheData,
    setLoading,
    getLoading,
    clearCache,
    clearAllCache,
    getQueryForm,
    setQueryForm,
    
    // 缓存过期时间
    CACHE_EXPIRE_TIME
  }
})