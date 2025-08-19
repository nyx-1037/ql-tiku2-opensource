import { ref, readonly } from 'vue'
import { MessagePlugin } from 'tdesign-vue-next'

/**
 * 错误处理组合式函数
 * 提供统一的错误处理、用户提示和错误恢复机制
 */
export function useErrorHandler() {
  const errors = ref([])
  const errorHistory = ref([])
  
  /**
   * 错误类型映射
   */
  const ERROR_TYPES = {
    NETWORK: 'network',
    TIMEOUT: 'timeout',
    SERVER: 'server',
    VALIDATION: 'validation',
    PERMISSION: 'permission',
    CACHE: 'cache',
    UNKNOWN: 'unknown'
  }
  
  /**
   * 错误消息映射
   */
  const ERROR_MESSAGES = {
    [ERROR_TYPES.NETWORK]: '网络连接失败，请检查网络设置',
    [ERROR_TYPES.TIMEOUT]: '请求超时，请稍后重试',
    [ERROR_TYPES.SERVER]: '服务器错误，请稍后重试',
    [ERROR_TYPES.VALIDATION]: '数据验证失败，请检查输入',
    [ERROR_TYPES.PERMISSION]: '权限不足，请联系管理员',
    [ERROR_TYPES.CACHE]: '缓存操作失败',
    [ERROR_TYPES.UNKNOWN]: '未知错误，请稍后重试'
  }
  
  /**
   * 分析错误类型
   * @param {Error} error - 错误对象
   * @returns {string} 错误类型
   */
  const analyzeErrorType = (error) => {
    if (!error) return ERROR_TYPES.UNKNOWN
    
    // 网络错误
    if (error.code === 'NETWORK_ERROR' || !navigator.onLine) {
      return ERROR_TYPES.NETWORK
    }
    
    // 超时错误
    if (error.message?.includes('timeout') || error.code === 'ECONNABORTED') {
      return ERROR_TYPES.TIMEOUT
    }
    
    // HTTP状态码错误
    if (error.response?.status) {
      const status = error.response.status
      if (status >= 500) return ERROR_TYPES.SERVER
      if (status === 401 || status === 403) return ERROR_TYPES.PERMISSION
      if (status === 400 || status === 422) return ERROR_TYPES.VALIDATION
    }
    
    // 缓存错误
    if (error.message?.includes('cache') || error.name === 'CacheError') {
      return ERROR_TYPES.CACHE
    }
    
    return ERROR_TYPES.UNKNOWN
  }
  
  /**
   * 处理错误
   * @param {Error} error - 错误对象
   * @param {Object} options - 处理选项
   */
  const handleError = (error, options = {}) => {
    const {
      showMessage = true,
      logError = true,
      context = '',
      silent = false,
      customMessage = ''
    } = options
    
    const errorType = analyzeErrorType(error)
    const errorInfo = {
      id: Date.now(),
      type: errorType,
      message: error.message || '未知错误',
      context,
      timestamp: new Date(),
      stack: error.stack,
      handled: false
    }
    
    // 记录错误
    if (logError) {
      console.error(`[ErrorHandler] ${context}:`, error)
      errorHistory.value.push(errorInfo)
      
      // 限制历史记录数量
      if (errorHistory.value.length > 100) {
        errorHistory.value = errorHistory.value.slice(-50)
      }
    }
    
    // 显示用户提示
    if (showMessage && !silent) {
      const message = customMessage || ERROR_MESSAGES[errorType] || error.message
      
      switch (errorType) {
        case ERROR_TYPES.NETWORK:
          MessagePlugin.error({
            content: message,
            duration: 5000,
            showClose: true
          })
          break
        case ERROR_TYPES.TIMEOUT:
          MessagePlugin.warning({
            content: message,
            duration: 4000,
            showClose: true
          })
          break
        case ERROR_TYPES.SERVER:
          MessagePlugin.error({
            content: message,
            duration: 6000,
            showClose: true
          })
          break
        case ERROR_TYPES.PERMISSION:
          MessagePlugin.error({
            content: message,
            duration: 0, // 不自动关闭
            showClose: true
          })
          break
        default:
          MessagePlugin.error({
            content: message,
            duration: 4000,
            showClose: true
          })
      }
    }
    
    // 添加到当前错误列表
    errors.value.push(errorInfo)
    
    return errorInfo
  }
  
  /**
   * 处理API错误
   * @param {Error} error - API错误
   * @param {string} apiName - API名称
   * @param {Object} options - 处理选项
   */
  const handleApiError = (error, apiName = '', options = {}) => {
    const context = `API: ${apiName}`
    
    // 特殊处理某些API错误
    const specialHandling = {
      401: () => {
        // 未授权，可能需要重新登录
        MessagePlugin.error('登录已过期，请重新登录')
        // 这里可以触发登录跳转
      },
      403: () => {
        MessagePlugin.error('权限不足，无法访问该资源')
      },
      404: () => {
        MessagePlugin.warning('请求的资源不存在')
      },
      429: () => {
        MessagePlugin.warning('请求过于频繁，请稍后再试')
      }
    }
    
    const status = error.response?.status
    if (status && specialHandling[status]) {
      specialHandling[status]()
    }
    
    return handleError(error, { ...options, context })
  }
  
  /**
   * 处理缓存错误
   * @param {Error} error - 缓存错误
   * @param {string} operation - 缓存操作
   * @param {Object} options - 处理选项
   */
  const handleCacheError = (error, operation = '', options = {}) => {
    const context = `Cache: ${operation}`
    
    // 缓存错误通常不需要显示给用户
    return handleError(error, {
      ...options,
      context,
      showMessage: false,
      silent: true
    })
  }
  
  /**
   * 清除错误
   * @param {number} errorId - 错误ID，不传则清除所有
   */
  const clearError = (errorId) => {
    if (errorId) {
      const index = errors.value.findIndex(err => err.id === errorId)
      if (index > -1) {
        errors.value.splice(index, 1)
      }
    } else {
      errors.value = []
    }
  }
  
  /**
   * 获取错误统计
   */
  const getErrorStats = () => {
    const stats = {}
    errorHistory.value.forEach(error => {
      stats[error.type] = (stats[error.type] || 0) + 1
    })
    return stats
  }
  
  /**
   * 导出错误日志
   */
  const exportErrorLog = () => {
    const log = {
      timestamp: new Date().toISOString(),
      userAgent: navigator.userAgent,
      url: window.location.href,
      errors: errorHistory.value
    }
    
    const blob = new Blob([JSON.stringify(log, null, 2)], { type: 'application/json' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `error-log-${Date.now()}.json`
    a.click()
    URL.revokeObjectURL(url)
  }
  
  return {
    errors: readonly(errors),
    errorHistory: readonly(errorHistory),
    ERROR_TYPES,
    ERROR_MESSAGES,
    
    // 方法
    handleError,
    handleApiError,
    handleCacheError,
    clearError,
    getErrorStats,
    exportErrorLog,
    analyzeErrorType
  }
}

/**
 * 全局错误处理器
 */
export function setupGlobalErrorHandler() {
  const { handleError } = useErrorHandler()
  
  // 捕获未处理的Promise错误
  window.addEventListener('unhandledrejection', (event) => {
    handleError(event.reason, {
      context: 'Unhandled Promise Rejection',
      showMessage: false
    })
  })
  
  // 捕获JavaScript错误
  window.addEventListener('error', (event) => {
    handleError(new Error(event.message), {
      context: `Script Error: ${event.filename}:${event.lineno}`,
      showMessage: false
    })
  })
  
  // Vue错误处理
  const handleVueError = (error, instance, info) => {
    handleError(error, {
      context: `Vue Error: ${info}`,
      showMessage: false
    })
  }
  
  return {
    handleVueError
  }
}