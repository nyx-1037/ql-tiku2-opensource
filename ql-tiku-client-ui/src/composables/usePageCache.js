import { ref, onMounted } from 'vue'
import { usePageCacheStore } from '@/store/pageCache'

/**
 * 页面缓存组合函数
 * @param {string} pageName - 页面名称
 * @param {Function} fetchDataFn - 获取数据的函数
 * @param {Object} options - 配置选项
 */
export function usePageCache(pageName, fetchDataFn, options = {}) {
  const cacheStore = usePageCacheStore()
  const {
    enableCache = true,
    showLoadingOnFirstLoad = true,
    showLoadingOnRefresh = false,
    silentRefresh = true // 是否静默刷新
  } = options

  // 响应式数据
  const data = ref(null)
  const loading = ref(false)
  const isFirstLoad = ref(true)
  const error = ref(null)

  // 加载数据的核心函数
  const loadData = async (forceRefresh = false, silent = false) => {
    try {
      error.value = null
      
      // 如果不强制刷新且有有效缓存，直接使用缓存
      if (!forceRefresh && enableCache && cacheStore.isCacheValid(pageName)) {
        const cachedData = cacheStore.getCacheData(pageName)
        if (cachedData) {
          data.value = cachedData
          isFirstLoad.value = false
          
          // 如果启用静默刷新，后台更新数据
          if (silentRefresh && !silent) {
            setTimeout(() => {
              loadData(true, true).catch(err => {
                console.warn(`${pageName} 静默刷新失败:`, err)
              })
            }, 100)
          }
          
          return cachedData
        }
      }

      // 设置加载状态
      const shouldShowLoading = silent ? false : (isFirstLoad.value ? showLoadingOnFirstLoad : showLoadingOnRefresh)
      if (shouldShowLoading) {
        loading.value = true
        cacheStore.setLoading(pageName, true)
      }

      // 调用数据获取函数
      const result = await fetchDataFn()
      
      // 更新数据和缓存
      data.value = result
      if (enableCache) {
        cacheStore.setCacheData(pageName, result)
      }
      
      isFirstLoad.value = false
      return result
    } catch (err) {
      error.value = err
      console.error(`${pageName} 数据加载失败:`, err)
      throw err
    } finally {
      loading.value = false
      cacheStore.setLoading(pageName, false)
    }
  }

  // 刷新数据
  const refreshData = () => {
    return loadData(true)
  }

  // 清除缓存
  const clearCache = () => {
    cacheStore.clearCache(pageName)
    data.value = null
  }

  // 初始化数据加载
  const initializeData = async () => {
    await loadData()
  }

  return {
    data,
    loading,
    error,
    isFirstLoad,
    loadData,
    refreshData,
    clearCache,
    initializeData
  }
}

/**
 * 带查询表单的页面缓存组合函数
 * @param {string} pageName - 页面名称
 * @param {Function} fetchDataFn - 获取数据的函数，接收查询参数
 * @param {Object} defaultQueryForm - 默认查询表单
 * @param {Object} options - 配置选项
 */
export function usePageCacheWithQuery(pageName, fetchDataFn, defaultQueryForm = {}, options = {}) {
  const cacheStore = usePageCacheStore()
  const {
    enableCache = true,
    showLoadingOnFirstLoad = true,
    showLoadingOnRefresh = false,
    silentRefresh = true
  } = options

  // 响应式数据
  const data = ref(null)
  const loading = ref(false)
  const isFirstLoad = ref(true)
  const error = ref(null)
  const queryForm = ref({ ...defaultQueryForm })

  // 从缓存恢复查询表单
  const restoreQueryForm = () => {
    const cachedQueryForm = cacheStore.getQueryForm(pageName)
    if (cachedQueryForm && Object.keys(cachedQueryForm).length > 0) {
      queryForm.value = { ...defaultQueryForm, ...cachedQueryForm }
    }
  }

  // 保存查询表单到缓存
  const saveQueryForm = () => {
    cacheStore.setQueryForm(pageName, queryForm.value)
  }

  // 加载数据的核心函数
  const loadData = async (forceRefresh = false, silent = false) => {
    try {
      error.value = null
      
      // 保存当前查询表单
      saveQueryForm()
      
      // 生成缓存键（包含查询参数）
      const cacheKey = `${pageName}_${JSON.stringify(queryForm.value)}`
      
      // 如果不强制刷新且有有效缓存，直接使用缓存
      if (!forceRefresh && enableCache && cacheStore.isCacheValid(cacheKey)) {
        const cachedData = cacheStore.getCacheData(cacheKey)
        if (cachedData) {
          data.value = cachedData
          isFirstLoad.value = false
          
          // 如果启用静默刷新，后台更新数据
          if (silentRefresh && !silent) {
            setTimeout(() => {
              loadData(true, true).catch(err => {
                console.warn(`${pageName} 静默刷新失败:`, err)
              })
            }, 100)
          }
          
          return cachedData
        }
      }

      // 设置加载状态
      const shouldShowLoading = silent ? false : (isFirstLoad.value ? showLoadingOnFirstLoad : showLoadingOnRefresh)
      if (shouldShowLoading) {
        loading.value = true
        cacheStore.setLoading(pageName, true)
      }

      // 调用数据获取函数
      const result = await fetchDataFn(queryForm.value)
      
      // 更新数据和缓存
      data.value = result
      if (enableCache) {
        cacheStore.setCacheData(cacheKey, result)
      }
      
      isFirstLoad.value = false
      return result
    } catch (err) {
      error.value = err
      console.error(`${pageName} 数据加载失败:`, err)
      throw err
    } finally {
      loading.value = false
      cacheStore.setLoading(pageName, false)
    }
  }

  // 刷新数据
  const refreshData = () => {
    return loadData(true)
  }

  // 重置查询表单
  const resetQueryForm = () => {
    queryForm.value = { ...defaultQueryForm }
    cacheStore.setQueryForm(pageName, queryForm.value)
  }

  // 清除缓存
  const clearCache = () => {
    cacheStore.clearCache(pageName)
    data.value = null
  }

  // 初始化
  const initializeData = async () => {
    restoreQueryForm()
    await loadData()
  }

  return {
    data,
    loading,
    error,
    isFirstLoad,
    queryForm,
    loadData,
    refreshData,
    resetQueryForm,
    clearCache,
    initializeData,
    restoreQueryForm,
    saveQueryForm
  }
}