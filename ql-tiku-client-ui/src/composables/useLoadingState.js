import { ref, computed, readonly } from 'vue'

/**
 * 加载状态管理组合式函数
 * 提供统一的加载状态、错误处理和重试机制
 */
export function useLoadingState() {
  // 加载状态
  const loading = ref(false)
  const initialLoading = ref(false) // 首次加载
  const refreshing = ref(false) // 刷新加载
  const loadingMore = ref(false) // 加载更多
  
  // 错误状态
  const error = ref(null)
  const networkError = ref(false)
  const timeoutError = ref(false)
  
  // 重试相关
  const retryCount = ref(0)
  const maxRetries = ref(3)
  const retryDelay = ref(1000) // 重试延迟（毫秒）
  
  // 计算属性
  const hasError = computed(() => !!error.value)
  const canRetry = computed(() => retryCount.value < maxRetries.value)
  const isLoading = computed(() => loading.value || initialLoading.value || refreshing.value || loadingMore.value)
  
  /**
   * 设置加载状态
   * @param {string} type - 加载类型：'initial', 'refresh', 'loadMore', 'normal'
   * @param {boolean} state - 加载状态
   */
  const setLoading = (type = 'normal', state = true) => {
    switch (type) {
      case 'initial':
        initialLoading.value = state
        break
      case 'refresh':
        refreshing.value = state
        break
      case 'loadMore':
        loadingMore.value = state
        break
      default:
        loading.value = state
    }
    
    // 开始加载时清除错误
    if (state) {
      clearError()
    }
  }
  
  /**
   * 设置错误状态
   * @param {Error|string} err - 错误对象或错误信息
   * @param {string} type - 错误类型：'network', 'timeout', 'server', 'unknown'
   */
  const setError = (err, type = 'unknown') => {
    error.value = err
    networkError.value = type === 'network'
    timeoutError.value = type === 'timeout'
    
    // 停止所有加载状态
    loading.value = false
    initialLoading.value = false
    refreshing.value = false
    loadingMore.value = false
    
    console.error(`[LoadingState] ${type} error:`, err)
  }
  
  /**
   * 清除错误状态
   */
  const clearError = () => {
    error.value = null
    networkError.value = false
    timeoutError.value = false
    retryCount.value = 0
  }
  
  /**
   * 执行异步操作，自动处理加载状态和错误
   * @param {Function} asyncFn - 异步函数
   * @param {Object} options - 配置选项
   */
  const executeAsync = async (asyncFn, options = {}) => {
    const {
      loadingType = 'normal',
      showError = true,
      autoRetry = true,
      timeout = 30000
    } = options
    
    try {
      setLoading(loadingType, true)
      
      // 设置超时
      const timeoutPromise = new Promise((_, reject) => {
        setTimeout(() => reject(new Error('Request timeout')), timeout)
      })
      
      const result = await Promise.race([asyncFn(), timeoutPromise])
      
      setLoading(loadingType, false)
      clearError()
      
      return result
    } catch (err) {
      setLoading(loadingType, false)
      
      // 判断错误类型
      let errorType = 'unknown'
      if (err.message === 'Request timeout') {
        errorType = 'timeout'
      } else if (err.code === 'NETWORK_ERROR' || !navigator.onLine) {
        errorType = 'network'
      } else if (err.response?.status >= 500) {
        errorType = 'server'
      }
      
      if (showError) {
        setError(err, errorType)
      }
      
      // 自动重试
      if (autoRetry && canRetry.value && (errorType === 'network' || errorType === 'timeout')) {
        retryCount.value++
        console.log(`[LoadingState] Auto retry ${retryCount.value}/${maxRetries.value}`)
        
        await new Promise(resolve => setTimeout(resolve, retryDelay.value * retryCount.value))
        return executeAsync(asyncFn, options)
      }
      
      throw err
    }
  }
  
  /**
   * 手动重试
   * @param {Function} asyncFn - 要重试的异步函数
   * @param {Object} options - 配置选项
   */
  const retry = async (asyncFn, options = {}) => {
    if (!canRetry.value) {
      console.warn('[LoadingState] Max retries exceeded')
      return
    }
    
    retryCount.value++
    return executeAsync(asyncFn, { ...options, autoRetry: false })
  }
  
  /**
   * 重置状态
   */
  const reset = () => {
    loading.value = false
    initialLoading.value = false
    refreshing.value = false
    loadingMore.value = false
    clearError()
  }
  
  return {
    // 状态
    loading: readonly(loading),
    initialLoading: readonly(initialLoading),
    refreshing: readonly(refreshing),
    loadingMore: readonly(loadingMore),
    isLoading,
    
    // 错误状态
    error: readonly(error),
    hasError,
    networkError: readonly(networkError),
    timeoutError: readonly(timeoutError),
    
    // 重试相关
    retryCount: readonly(retryCount),
    canRetry,
    maxRetries,
    retryDelay,
    
    // 方法
    setLoading,
    setError,
    clearError,
    executeAsync,
    retry,
    reset
  }
}

/**
 * 全局加载状态管理
 * 用于管理应用级别的加载状态
 */
export function useGlobalLoadingState() {
  const globalLoading = ref(false)
  const loadingTasks = ref(new Set())
  
  const startLoading = (taskId = 'default') => {
    loadingTasks.value.add(taskId)
    globalLoading.value = true
  }
  
  const stopLoading = (taskId = 'default') => {
    loadingTasks.value.delete(taskId)
    if (loadingTasks.value.size === 0) {
      globalLoading.value = false
    }
  }
  
  const clearAllLoading = () => {
    loadingTasks.value.clear()
    globalLoading.value = false
  }
  
  return {
    globalLoading: readonly(globalLoading),
    startLoading,
    stopLoading,
    clearAllLoading
  }
}