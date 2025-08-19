import axios from 'axios'
import { MessagePlugin } from 'tdesign-vue-next'
import cacheManager from '@/utils/cacheManager'
import { getCacheConfigByUrl } from '@/config/cacheConfig'
import cacheMonitor from '@/utils/cacheMonitor'

// 使用全局缓存管理器
const globalCacheManager = cacheManager

// 可缓存的请求方法
const CACHEABLE_METHODS = ['GET']

// 需要缓存的API路径模式
const CACHEABLE_PATHS = [
  '/api/subjects',
  '/api/questions',
  '/api/practice-records',
  '/api/files',
  '/api/feedback',
  '/api/statistics',
  '/api/analytics'
]

// 缓存配置映射
const CACHE_CONFIG = {
  '/api/subjects': { ttl: 30 * 60 * 1000 }, // 30分钟
  '/api/questions': { ttl: 15 * 60 * 1000 }, // 15分钟
  '/api/practice-records': { ttl: 5 * 60 * 1000 }, // 5分钟
  '/api/files': { ttl: 20 * 60 * 1000 }, // 20分钟
  '/api/feedback': { ttl: 10 * 60 * 1000 }, // 10分钟
  '/api/statistics': { ttl: 5 * 60 * 1000 }, // 5分钟
  '/api/analytics': { ttl: 10 * 60 * 1000 } // 10分钟
}

/**
 * 生成缓存键
 * @param {Object} config - axios请求配置
 * @returns {string} 缓存键
 */
const generateCacheKey = (config) => {
  const { method, url, params, data } = config
  const baseKey = `${method}_${url}`
  
  // 添加查询参数到缓存键
  if (params && Object.keys(params).length > 0) {
    const sortedParams = Object.keys(params)
      .sort()
      .map(key => `${key}=${params[key]}`)
      .join('&')
    return `${baseKey}?${sortedParams}`
  }
  
  // 对于POST请求，添加请求体到缓存键
  if (data && typeof data === 'object') {
    const sortedData = JSON.stringify(data, Object.keys(data).sort())
    return `${baseKey}_${btoa(sortedData).slice(0, 20)}`
  }
  
  return baseKey
}

/**
 * 检查请求是否可缓存
 * @param {Object} config - axios请求配置
 * @returns {boolean} 是否可缓存
 */
const isCacheable = (config) => {
  const { method, url } = config
  
  // 检查请求方法
  if (!CACHEABLE_METHODS.includes(method?.toUpperCase())) {
    return false
  }
  
  // 检查URL路径
  return CACHEABLE_PATHS.some(path => url?.includes(path))
}

/**
 * 获取缓存配置
 * @param {string} url - 请求URL
 * @returns {Object} 缓存配置
 */
const getCacheConfig = (url) => {
  // 优先使用配置文件中的设置
  const configFromFile = getCacheConfigByUrl(url)
  if (configFromFile && configFromFile.ttl) {
    return { ttl: configFromFile.ttl }
  }
  
  // 回退到默认配置
  for (const [path, config] of Object.entries(CACHE_CONFIG)) {
    if (url?.includes(path)) {
      return config
    }
  }
  return { ttl: 10 * 60 * 1000 } // 默认10分钟
}

/**
 * 创建带缓存功能的axios实例
 * @param {Object} baseConfig - 基础配置
 * @returns {Object} axios实例
 */
export const createCachedAxios = (baseConfig = {}) => {
  const instance = axios.create({
    timeout: 30000,
    ...baseConfig
  })

  // 请求拦截器 - 检查缓存
  instance.interceptors.request.use(
    (config) => {
      console.log('🌐 API请求拦截器: 处理请求', config.method?.toUpperCase(), config.url)
      
      // 添加请求时间戳和监控信息
      config.metadata = {
        startTime: Date.now(),
        cacheKey: generateCacheKey(config)
      }
      
      // 检查是否可缓存
      if (isCacheable(config)) {
        const cacheKey = config.metadata.cacheKey
        const cachedResponse = globalCacheManager.get(cacheKey)
        
        if (cachedResponse) {
          console.log('📦 API请求拦截器: 使用缓存响应', cacheKey)
          
          // 创建一个Promise来模拟axios响应
          config.adapter = () => {
            return Promise.resolve({
              data: cachedResponse.data,
              status: 200,
              statusText: 'OK',
              headers: cachedResponse.headers || {},
              config,
              request: {},
              fromCache: true
            })
          }
        }
      }
      
      return config
    },
    (error) => {
      console.error('❌ API请求拦截器: 请求错误', error)
      return Promise.reject(error)
    }
  )

  // 响应拦截器 - 缓存响应
  instance.interceptors.response.use(
    (response) => {
      const { config } = response
      const duration = Date.now() - (config.metadata?.startTime || 0)
      
      console.log(
        `✅ API响应拦截器: 请求完成`,
        config.method?.toUpperCase(),
        config.url,
        `${duration}ms`,
        response.fromCache ? '(来自缓存)' : '(来自服务器)'
      )
      
      // 性能监控已简化，移除不存在的方法调用
      
      // 如果不是来自缓存且可缓存，则存储到缓存
      if (!response.fromCache && isCacheable(config)) {
        const cacheKey = config.metadata?.cacheKey
        const cacheConfig = getCacheConfig(config.url)
        
        if (cacheKey && response.data) {
          globalCacheManager.set(cacheKey, {
            data: response.data,
            headers: response.headers,
            timestamp: Date.now()
          }, cacheConfig.ttl)
          
          console.log('💾 API响应拦截器: 缓存响应', cacheKey, `TTL: ${cacheConfig.ttl}ms`)
        }
      }
      
      // 内存使用量监控已简化，移除不存在的方法调用
      
      return response
    },
    (error) => {
      const { config } = error
      const duration = Date.now() - (config?.metadata?.startTime || 0)
      
      console.error(
        '❌ API响应拦截器: 请求失败',
        config?.method?.toUpperCase(),
        config?.url,
        `${duration}ms`,
        error.message
      )
      
      // 记录错误请求
      if (config?.metadata?.requestTracker) {
        // 性能监控已简化，移除不存在的方法调用
      }
      
      // 处理不同类型的错误
      if (error.response) {
        // 服务器响应错误
        const { status, data } = error.response
        
        switch (status) {
          case 401:
            console.warn('🔐 API响应拦截器: 未授权，可能需要重新登录')
            MessagePlugin.warning('登录已过期，请重新登录')
            // 可以在这里触发登录页面跳转
            break
          case 403:
            console.warn('🚫 API响应拦截器: 权限不足')
            MessagePlugin.error('权限不足')
            break
          case 404:
            console.warn('🔍 API响应拦截器: 资源未找到')
            MessagePlugin.error('请求的资源不存在')
            break
          case 500:
            console.error('💥 API响应拦截器: 服务器内部错误')
            MessagePlugin.error('服务器内部错误，请稍后重试')
            break
          default:
            console.error('⚠️ API响应拦截器: 其他服务器错误', status, data)
            MessagePlugin.error(data?.message || '请求失败，请稍后重试')
        }
      } else if (error.request) {
        // 网络错误
        console.error('🌐 API响应拦截器: 网络错误', error.request)
        MessagePlugin.error('网络连接失败，请检查网络设置')
      } else {
        // 其他错误
        console.error('❓ API响应拦截器: 未知错误', error.message)
        MessagePlugin.error('请求失败，请稍后重试')
      }
      
      return Promise.reject(error)
    }
  )

  return instance
}

/**
 * 缓存管理工具
 */
export const cacheUtils = {
  /**
   * 清除指定模式的缓存
   * @param {string} pattern - 缓存键模式
   */
  clearByPattern: (pattern) => {
    console.log('🗑️ 缓存工具: 清除缓存模式', pattern)
    globalCacheManager.clearByPattern(pattern)
  },

  /**
   * 清除所有API缓存
   */
  clearAll: () => {
    console.log('🗑️ 缓存工具: 清除所有缓存')
    globalCacheManager.clear()
  },

  /**
   * 获取缓存统计信息
   * @returns {Object} 缓存统计
   */
  getStats: () => {
    const stats = globalCacheManager.getStats()
    console.log('📊 缓存工具: 缓存统计', stats)
    return stats
  },

  /**
   * 预热缓存 - 预先加载常用数据
   * @param {Array} requests - 请求配置数组
   */
  warmup: async (requests = []) => {
    console.log('🔥 缓存工具: 开始预热缓存', requests.length, '个请求')
    
    const warmupPromises = requests.map(async (requestConfig) => {
      try {
        const instance = createCachedAxios()
        await instance(requestConfig)
        console.log('✅ 缓存预热成功:', requestConfig.url)
      } catch (error) {
        console.warn('⚠️ 缓存预热失败:', requestConfig.url, error.message)
      }
    })
    
    await Promise.allSettled(warmupPromises)
    console.log('🔥 缓存工具: 缓存预热完成')
  },

  /**
   * 刷新指定URL的缓存
   * @param {string} url - 要刷新的URL
   * @param {Object} config - 请求配置
   */
  refresh: async (url, config = {}) => {
    console.log('🔄 缓存工具: 刷新缓存', url)
    
    // 清除相关缓存
    globalCacheManager.clearByPattern(url)
    
    // 重新请求数据
    try {
      const instance = createCachedAxios()
      const response = await instance({ url, ...config })
      console.log('✅ 缓存刷新成功:', url)
      return response
    } catch (error) {
      console.error('❌ 缓存刷新失败:', url, error.message)
      throw error
    }
  },

  /**
   * 获取性能监控报告
   * @returns {Object} 性能报告
   */
  getPerformanceReport: () => {
    return cacheMonitor.getPerformanceReport()
  },

  /**
   * 重置性能监控数据
   */
  resetMonitoring: () => {
    cacheMonitor.reset()
  }
}

/**
 * 默认导出带缓存的axios实例
 */
export default createCachedAxios()