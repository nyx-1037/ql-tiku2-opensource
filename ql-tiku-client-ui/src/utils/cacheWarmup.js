import cacheManager from './cacheManager'
import { CACHE_WARMUP_CONFIG, getCacheConfigByUrl } from '@/config/cacheConfig'
import { useAuthStore } from '@/store/auth'

/**
 * 缓存预热工具
 * 负责在应用启动、用户登录、页面访问时预加载数据
 */
class CacheWarmup {
  constructor() {
    this.isWarming = false
    this.warmupQueue = []
    this.completedRequests = new Set()
  }

  /**
   * 应用启动时的缓存预热
   */
  async onAppStart() {
    if (this.isWarming) return
    
    console.log('🔥 开始应用启动缓存预热...')
    this.isWarming = true
    
    try {
      const requests = CACHE_WARMUP_CONFIG.onAppStart
      await this.executeWarmupRequests(requests, 'app-start')
      console.log('✅ 应用启动缓存预热完成')
    } catch (error) {
      console.error('❌ 应用启动缓存预热失败:', error)
    } finally {
      this.isWarming = false
    }
  }

  /**
   * 用户登录后的缓存预热
   */
  async onUserLogin() {
    if (this.isWarming) return
    
    console.log('🔥 开始用户登录缓存预热...')
    this.isWarming = true
    
    try {
      const requests = CACHE_WARMUP_CONFIG.onUserLogin
      await this.executeWarmupRequests(requests, 'user-login')
      console.log('✅ 用户登录缓存预热完成')
    } catch (error) {
      console.error('❌ 用户登录缓存预热失败:', error)
    } finally {
      this.isWarming = false
    }
  }

  /**
   * 页面访问时的缓存预热
   * @param {string} pageName - 页面名称
   */
  async onPageVisit(pageName) {
    if (this.isWarming) return
    
    const requests = CACHE_WARMUP_CONFIG.onPageVisit[pageName]
    if (!requests || requests.length === 0) return
    
    console.log(`🔥 开始页面 ${pageName} 缓存预热...`)
    this.isWarming = true
    
    try {
      await this.executeWarmupRequests(requests, `page-${pageName}`)
      console.log(`✅ 页面 ${pageName} 缓存预热完成`)
    } catch (error) {
      console.error(`❌ 页面 ${pageName} 缓存预热失败:`, error)
    } finally {
      this.isWarming = false
    }
  }

  /**
   * 执行预热请求
   * @param {Array} requests - 请求列表
   * @param {string} context - 上下文标识
   */
  async executeWarmupRequests(requests, context) {
    const authStore = useAuthStore()
    const promises = []
    
    for (const request of requests) {
      // 生成请求标识
      const requestId = this.generateRequestId(request, context)
      
      // 避免重复请求
      if (this.completedRequests.has(requestId)) {
        continue
      }
      
      // 检查是否需要认证
      if (request.requireAuth && !authStore.isAuthenticated) {
        console.log(`⏭️ 跳过需要认证的请求: ${request.url}`)
        continue
      }
      
      // 创建预热请求
      const promise = this.createWarmupRequest(request, requestId)
      promises.push(promise)
    }
    
    // 并发执行，但限制并发数
    await this.executeConcurrentRequests(promises, 3)
  }

  /**
   * 创建预热请求
   * @param {Object} request - 请求配置
   * @param {string} requestId - 请求ID
   */
  async createWarmupRequest(request, requestId) {
    try {
      const { url, method = 'GET', params, headers, timeout = 5000 } = request
      
      // 构建完整URL
      const fullUrl = this.buildUrl(url, params)
      const cacheKey = `${method}_${fullUrl}`
      
      // 检查缓存是否已存在且有效
      if (cacheManager.has(cacheKey)) {
        this.completedRequests.add(requestId)
        return { success: true, cached: true, url: fullUrl }
      }
      
      // 发起请求
      const response = await this.fetchWithTimeout(fullUrl, {
        method,
        headers: {
          'Content-Type': 'application/json',
          ...headers
        }
      }, timeout)
      
      if (response.ok) {
        const data = await response.json()
        
        // 获取缓存配置
        const cacheConfig = getCacheConfigByUrl(url)
        
        // 存储到缓存
        cacheManager.set(cacheKey, data, cacheConfig.ttl)
        
        this.completedRequests.add(requestId)
        return { success: true, cached: false, url: fullUrl, size: JSON.stringify(data).length }
      } else {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`)
      }
    } catch (error) {
      console.warn(`预热请求失败 [${requestId}]:`, error.message)
      return { success: false, error: error.message, url: request.url }
    }
  }

  /**
   * 并发执行请求（限制并发数）
   * @param {Array} promises - Promise数组
   * @param {number} concurrency - 并发数限制
   */
  async executeConcurrentRequests(promises, concurrency = 3) {
    const results = []
    
    for (let i = 0; i < promises.length; i += concurrency) {
      const batch = promises.slice(i, i + concurrency)
      const batchResults = await Promise.allSettled(batch)
      
      batchResults.forEach((result, index) => {
        if (result.status === 'fulfilled') {
          results.push(result.value)
        } else {
          console.warn(`预热请求批次失败 [${i + index}]:`, result.reason)
          results.push({ success: false, error: result.reason.message })
        }
      })
      
      // 批次间稍作延迟，避免过度压力
      if (i + concurrency < promises.length) {
        await this.delay(100)
      }
    }
    
    // 输出预热统计
    this.logWarmupStats(results)
    return results
  }

  /**
   * 构建URL
   * @param {string} baseUrl - 基础URL
   * @param {Object} params - 查询参数
   */
  buildUrl(baseUrl, params) {
    if (!params) return baseUrl
    
    const url = new URL(baseUrl, window.location.origin)
    Object.entries(params).forEach(([key, value]) => {
      url.searchParams.append(key, value)
    })
    
    return url.toString()
  }

  /**
   * 带超时的fetch请求
   * @param {string} url - 请求URL
   * @param {Object} options - 请求选项
   * @param {number} timeout - 超时时间
   */
  async fetchWithTimeout(url, options, timeout) {
    const controller = new AbortController()
    const timeoutId = setTimeout(() => controller.abort(), timeout)
    
    try {
      const response = await fetch(url, {
        ...options,
        signal: controller.signal
      })
      clearTimeout(timeoutId)
      return response
    } catch (error) {
      clearTimeout(timeoutId)
      throw error
    }
  }

  /**
   * 生成请求ID
   * @param {Object} request - 请求配置
   * @param {string} context - 上下文
   */
  generateRequestId(request, context) {
    const { url, method = 'GET', params } = request
    const paramStr = params ? JSON.stringify(params) : ''
    return `${context}_${method}_${url}_${paramStr}`
  }

  /**
   * 延迟函数
   * @param {number} ms - 延迟毫秒数
   */
  delay(ms) {
    return new Promise(resolve => setTimeout(resolve, ms))
  }

  /**
   * 输出预热统计信息
   * @param {Array} results - 结果数组
   */
  logWarmupStats(results) {
    const stats = {
      total: results.length,
      success: results.filter(r => r.success).length,
      failed: results.filter(r => !r.success).length,
      cached: results.filter(r => r.success && r.cached).length,
      fetched: results.filter(r => r.success && !r.cached).length,
      totalSize: results
        .filter(r => r.success && r.size)
        .reduce((sum, r) => sum + r.size, 0)
    }
    
    console.log('📊 缓存预热统计:', {
      ...stats,
      successRate: `${((stats.success / stats.total) * 100).toFixed(1)}%`,
      totalSizeKB: `${(stats.totalSize / 1024).toFixed(1)}KB`
    })
  }

  /**
   * 智能预热（根据用户行为预测）
   * @param {Array} recentPages - 最近访问的页面
   */
  async smartWarmup(recentPages = []) {
    if (this.isWarming) return
    
    console.log('🧠 开始智能缓存预热...')
    this.isWarming = true
    
    try {
      const requests = []
      
      // 基于最近访问页面预热
      recentPages.forEach(pageName => {
        const pageRequests = CACHE_WARMUP_CONFIG.onPageVisit[pageName]
        if (pageRequests) {
          requests.push(...pageRequests.map(req => ({ ...req, priority: 'high' })))
        }
      })
      
      // 添加高频访问的通用数据
      const commonRequests = [
        { url: '/api/subjects', method: 'GET', priority: 'medium' },
        { url: '/api/public/config', method: 'GET', priority: 'medium' }
      ]
      requests.push(...commonRequests)
      
      // 按优先级排序
      requests.sort((a, b) => {
        const priorityOrder = { high: 3, medium: 2, low: 1 }
        return (priorityOrder[b.priority] || 1) - (priorityOrder[a.priority] || 1)
      })
      
      await this.executeWarmupRequests(requests, 'smart-warmup')
      console.log('✅ 智能缓存预热完成')
    } catch (error) {
      console.error('❌ 智能缓存预热失败:', error)
    } finally {
      this.isWarming = false
    }
  }

  /**
   * 清理预热状态
   */
  reset() {
    this.completedRequests.clear()
    this.warmupQueue = []
    this.isWarming = false
  }

  /**
   * 获取预热状态
   */
  getStatus() {
    return {
      isWarming: this.isWarming,
      completedCount: this.completedRequests.size,
      queueLength: this.warmupQueue.length
    }
  }
}

// 创建全局预热实例
const cacheWarmup = new CacheWarmup()

export default cacheWarmup