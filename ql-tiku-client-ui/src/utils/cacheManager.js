import { CACHE_CLEANUP_CONFIG, CACHE_MONITORING_CONFIG } from '@/config/cacheConfig'

/**
 * 缓存管理器
 * 负责缓存的生命周期管理、过期检查、内存监控等
 */
class CacheManager {
  constructor() {
    this.cache = new Map()
    this.accessTimes = new Map() // 记录访问时间
    this.hitCount = 0
    this.missCount = 0
    this.cleanupTimer = null
    this.monitoringTimer = null
    this.errorCount = 0
    this.totalRequests = 0
    
    this.init()
  }

  init() {
    // 启动自动清理
    this.startAutoCleanup()
    
    // 启动性能监控
    if (CACHE_MONITORING_CONFIG.enabled) {
      this.startMonitoring()
    }

    // 监听内存压力事件
    this.setupMemoryPressureHandling()
  }

  /**
   * 设置缓存
   * @param {string} key - 缓存键
   * @param {any} value - 缓存值
   * @param {number} ttl - 生存时间（毫秒）
   */
  set(key, value, ttl = 300000) { // 默认5分钟
    const now = Date.now()
    const item = {
      value,
      timestamp: now,
      ttl,
      expiresAt: now + ttl,
      accessCount: 0,
      lastAccessed: now,
      size: this.calculateItemSize(key, value)
    }
    
    this.cache.set(key, item)
    this.accessTimes.set(key, now)
    
    // 检查内存使用
    this.checkMemoryUsage()
  }

  /**
   * 获取缓存
   * @param {string} key - 缓存键
   * @returns {any|null} 缓存值或null
   */
  get(key) {
    this.totalRequests++
    const item = this.cache.get(key)
    
    if (!item) {
      this.missCount++
      return null
    }
    
    // 检查是否过期
    if (this.isExpired(item)) {
      this.delete(key)
      this.missCount++
      return null
    }
    
    // 更新访问信息
    item.accessCount++
    item.lastAccessed = Date.now()
    this.accessTimes.set(key, item.lastAccessed)
    this.hitCount++
    
    return item.value
  }

  /**
   * 检查缓存项是否过期
   * @param {Object} item - 缓存项
   * @returns {boolean} 是否过期
   */
  isExpired(item) {
    return Date.now() > item.expiresAt
  }

  /**
   * 删除缓存
   * @param {string} key - 缓存键
   */
  delete(key) {
    this.cache.delete(key)
    this.accessTimes.delete(key)
  }

  /**
   * 清空所有缓存
   */
  clear() {
    this.cache.clear()
    this.accessTimes.clear()
    this.hitCount = 0
    this.missCount = 0
    this.errorCount = 0
    this.totalRequests = 0
  }

  /**
   * 检查缓存是否存在且未过期
   * @param {string} key - 缓存键
   * @returns {boolean} 是否存在有效缓存
   */
  has(key) {
    const item = this.cache.get(key)
    if (!item) return false
    
    if (this.isExpired(item)) {
      this.delete(key)
      return false
    }
    
    return true
  }

  /**
   * 批量删除缓存
   * @param {Array<string>} keys - 缓存键数组
   */
  deleteMany(keys) {
    let deletedCount = 0
    keys.forEach(key => {
      if (this.cache.has(key)) {
        this.delete(key)
        deletedCount++
      }
    })
    return deletedCount
  }

  /**
   * 根据模式删除缓存
   * @param {string|RegExp} pattern - 匹配模式
   */
  deleteByPattern(pattern) {
    const regex = pattern instanceof RegExp ? pattern : new RegExp(pattern.replace('*', '.*'))
    const keysToDelete = []
    
    for (const key of this.cache.keys()) {
      if (regex.test(key)) {
        keysToDelete.push(key)
      }
    }
    
    return this.deleteMany(keysToDelete)
  }

  /**
   * 获取缓存统计信息
   * @returns {Object} 统计信息
   */
  getStats() {
    const total = this.hitCount + this.missCount
    const memoryUsage = this.getMemoryUsage()
    
    return {
      size: this.cache.size,
      hitCount: this.hitCount,
      missCount: this.missCount,
      totalRequests: this.totalRequests,
      hitRate: total > 0 ? parseFloat((this.hitCount / total).toFixed(3)) : 0,
      errorRate: this.totalRequests > 0 ? parseFloat((this.errorCount / this.totalRequests).toFixed(3)) : 0,
      memoryUsage: parseFloat(memoryUsage),
      averageItemSize: this.cache.size > 0 ? parseFloat((memoryUsage / this.cache.size).toFixed(2)) : 0,
      oldestItem: this.getOldestItemAge(),
      newestItem: this.getNewestItemAge()
    }
  }

  /**
   * 计算缓存项大小
   * @param {string} key - 缓存键
   * @param {any} value - 缓存值
   * @returns {number} 大小（字节）
   */
  calculateItemSize(key, value) {
    try {
      const keySize = key.length * 2 // Unicode字符占2字节
      const valueSize = JSON.stringify(value).length * 2
      const metadataSize = 200 // 元数据开销估算
      return keySize + valueSize + metadataSize
    } catch (error) {
      this.errorCount++
      return 1000 // 默认大小
    }
  }

  /**
   * 估算内存使用量（MB）
   * @returns {number} 内存使用量
   */
  getMemoryUsage() {
    let totalSize = 0
    
    for (const [key, item] of this.cache) {
      totalSize += item.size || this.calculateItemSize(key, item.value)
    }
    
    return (totalSize / 1024 / 1024).toFixed(2) // 转换为MB
  }

  /**
   * 获取最老缓存项的年龄（分钟）
   * @returns {number} 年龄
   */
  getOldestItemAge() {
    if (this.cache.size === 0) return 0
    
    let oldest = Date.now()
    for (const item of this.cache.values()) {
      if (item.timestamp < oldest) {
        oldest = item.timestamp
      }
    }
    
    return Math.floor((Date.now() - oldest) / 60000) // 转换为分钟
  }

  /**
   * 获取最新缓存项的年龄（分钟）
   * @returns {number} 年龄
   */
  getNewestItemAge() {
    if (this.cache.size === 0) return 0
    
    let newest = 0
    for (const item of this.cache.values()) {
      if (item.timestamp > newest) {
        newest = item.timestamp
      }
    }
    
    return Math.floor((Date.now() - newest) / 60000) // 转换为分钟
  }

  /**
   * 检查内存使用情况
   */
  checkMemoryUsage() {
    const usage = parseFloat(this.getMemoryUsage())
    
    if (usage > CACHE_CLEANUP_CONFIG.memoryThreshold) {
      console.warn(`缓存内存使用过高: ${usage}MB，开始清理...`)
      this.performCleanup('memory')
    }
    
    if (this.cache.size > CACHE_CLEANUP_CONFIG.itemCountThreshold) {
      console.warn(`缓存项数量过多: ${this.cache.size}，开始清理...`)
      this.performCleanup('count')
    }
  }

  /**
   * 设置内存压力处理
   */
  setupMemoryPressureHandling() {
    // 监听浏览器内存压力事件（如果支持）
    if (typeof window !== 'undefined' && 'memory' in performance) {
      const checkMemoryPressure = () => {
        const memInfo = performance.memory
        const usedMB = memInfo.usedJSHeapSize / 1024 / 1024
        const limitMB = memInfo.jsHeapSizeLimit / 1024 / 1024
        
        // 如果内存使用超过80%，执行激进清理
        if (usedMB / limitMB > 0.8) {
          console.warn('检测到内存压力，执行激进缓存清理')
          this.performAggressiveCleanup()
        }
      }
      
      // 每30秒检查一次内存压力
      setInterval(checkMemoryPressure, 30000)
    }
  }

  /**
   * 执行激进清理（内存压力时）
   */
  performAggressiveCleanup() {
    const beforeSize = this.cache.size
    
    // 1. 清理所有过期项
    this.cleanupExpired()
    
    // 2. 清理访问次数少于2次的项
    let cleanedCount = 0
    for (const [key, item] of this.cache) {
      if (item.accessCount < 2) {
        this.delete(key)
        cleanedCount++
      }
    }
    
    // 3. 如果还是太多，清理最久未访问的50%
    if (this.cache.size > 100) {
      const entries = Array.from(this.cache.entries())
        .sort((a, b) => a[1].lastAccessed - b[1].lastAccessed)
      
      const targetSize = Math.floor(this.cache.size * 0.5)
      let index = 0
      
      while (this.cache.size > targetSize && index < entries.length) {
        const [key] = entries[index]
        this.delete(key)
        cleanedCount++
        index++
      }
    }
    
    console.log(`激进清理完成: ${beforeSize} -> ${this.cache.size} (清理${cleanedCount}项)`)
  }

  /**
   * 启动自动清理
   */
  startAutoCleanup() {
    if (this.cleanupTimer) {
      clearInterval(this.cleanupTimer)
    }
    
    this.cleanupTimer = setInterval(() => {
      this.performCleanup('auto')
    }, CACHE_CLEANUP_CONFIG.autoCleanupInterval)
  }

  /**
   * 停止自动清理
   */
  stopAutoCleanup() {
    if (this.cleanupTimer) {
      clearInterval(this.cleanupTimer)
      this.cleanupTimer = null
    }
  }

  /**
   * 执行缓存清理
   * @param {string} reason - 清理原因
   */
  performCleanup(reason = 'manual') {
    const beforeSize = this.cache.size
    const beforeMemory = this.getMemoryUsage()
    let cleanedCount = 0
    
    // 1. 清理过期项
    cleanedCount += this.cleanupExpired()
    
    // 2. 根据清理原因执行不同策略
    switch (reason) {
      case 'memory':
        cleanedCount += this.cleanupByMemory()
        break
      case 'count':
        cleanedCount += this.cleanupByCount()
        break
      case 'auto':
        cleanedCount += this.cleanupByPattern()
        cleanedCount += this.cleanupLowFrequency()
        break
    }
    
    const afterSize = this.cache.size
    const afterMemory = this.getMemoryUsage()
    
    if (cleanedCount > 0) {
      console.log(`缓存清理完成 [${reason}]:`, {
        清理项数: cleanedCount,
        清理前: `${beforeSize}项/${beforeMemory}MB`,
        清理后: `${afterSize}项/${afterMemory}MB`,
        节省内存: `${(beforeMemory - afterMemory).toFixed(2)}MB`
      })
    }
  }

  /**
   * 清理过期缓存项
   * @returns {number} 清理的项数
   */
  cleanupExpired() {
    let cleanedCount = 0
    const now = Date.now()
    
    for (const [key, item] of this.cache) {
      if (now > item.expiresAt) {
        this.delete(key)
        cleanedCount++
      }
    }
    
    return cleanedCount
  }

  /**
   * 清理低频访问项
   * @returns {number} 清理的项数
   */
  cleanupLowFrequency() {
    let cleanedCount = 0
    const now = Date.now()
    const oneHourAgo = now - 60 * 60 * 1000
    
    // 清理1小时内访问次数少于2次的项
    for (const [key, item] of this.cache) {
      if (item.lastAccessed < oneHourAgo && item.accessCount < 2) {
        this.delete(key)
        cleanedCount++
      }
    }
    
    return cleanedCount
  }

  /**
   * 基于内存压力清理缓存
   * @returns {number} 清理的项数
   */
  cleanupByMemory() {
    // 按最后访问时间排序，清理最久未访问的项
    const entries = Array.from(this.cache.entries())
      .sort((a, b) => a[1].lastAccessed - b[1].lastAccessed)
    
    let cleanedCount = 0
    const targetSize = Math.floor(this.cache.size * 0.7) // 清理到70%
    
    while (this.cache.size > targetSize && cleanedCount < entries.length) {
      const [key] = entries[cleanedCount]
      this.delete(key)
      cleanedCount++
    }
    
    return cleanedCount
  }

  /**
   * 基于数量压力清理缓存
   * @returns {number} 清理的项数
   */
  cleanupByCount() {
    // 按访问频率排序，清理访问频率最低的项
    const entries = Array.from(this.cache.entries())
      .sort((a, b) => a[1].accessCount - b[1].accessCount)
    
    let cleanedCount = 0
    const targetCount = CACHE_CLEANUP_CONFIG.itemCountThreshold * 0.8 // 清理到80%
    
    while (this.cache.size > targetCount && cleanedCount < entries.length) {
      const [key] = entries[cleanedCount]
      this.delete(key)
      cleanedCount++
    }
    
    return cleanedCount
  }

  /**
   * 基于模式清理缓存
   * @returns {number} 清理的项数
   */
  cleanupByPattern() {
    let cleanedCount = 0
    
    for (const pattern of CACHE_CLEANUP_CONFIG.cleanupPatterns) {
      cleanedCount += this.deleteByPattern(pattern)
    }
    
    return cleanedCount
  }

  /**
   * 启动性能监控
   */
  startMonitoring() {
    if (this.monitoringTimer) {
      clearInterval(this.monitoringTimer)
    }
    
    this.monitoringTimer = setInterval(() => {
      this.reportPerformance()
    }, CACHE_MONITORING_CONFIG.reportInterval)
  }

  /**
   * 停止性能监控
   */
  stopMonitoring() {
    if (this.monitoringTimer) {
      clearInterval(this.monitoringTimer)
      this.monitoringTimer = null
    }
  }

  /**
   * 报告性能指标
   */
  reportPerformance() {
    const stats = this.getStats()
    const thresholds = CACHE_MONITORING_CONFIG.thresholds
    
    // 检查性能阈值
    const alerts = []
    
    if (stats.hitRate < thresholds.hitRate) {
      alerts.push(`缓存命中率过低: ${stats.hitRate} < ${thresholds.hitRate}`)
    }
    
    if (stats.memoryUsage > thresholds.memoryUsage) {
      alerts.push(`内存使用过高: ${stats.memoryUsage}MB > ${thresholds.memoryUsage}MB`)
    }
    
    if (stats.errorRate > thresholds.errorRate) {
      alerts.push(`错误率过高: ${stats.errorRate} > ${thresholds.errorRate}`)
    }
    
    // 输出性能报告
    console.group('🚀 缓存性能报告')
    console.table(stats)
    
    if (alerts.length > 0) {
      console.warn('⚠️ 性能告警:', alerts)
    }
    
    console.groupEnd()
  }

  /**
   * 获取缓存详细信息（调试用）
   * @returns {Array} 缓存项详情
   */
  getDebugInfo() {
    const items = []
    
    for (const [key, item] of this.cache) {
      items.push({
        key,
        size: item.size || JSON.stringify(item.value).length,
        created: new Date(item.timestamp).toLocaleString(),
        expires: new Date(item.expiresAt).toLocaleString(),
        ttl: item.ttl,
        accessCount: item.accessCount,
        lastAccessed: new Date(item.lastAccessed).toLocaleString(),
        isExpired: this.isExpired(item)
      })
    }
    
    return items.sort((a, b) => b.accessCount - a.accessCount)
  }

  /**
   * 预热缓存
   * @param {Array} requests - 预热请求列表
   */
  async warmup(requests) {
    console.log('开始缓存预热...', requests.length, '个请求')
    
    const promises = requests.map(async (request) => {
      try {
        // 这里需要配合API拦截器实现
        const response = await fetch(request.url, {
          method: request.method || 'GET',
          ...request.options
        })
        
        if (response.ok) {
          const data = await response.json()
          const cacheKey = `${request.method || 'GET'}_${request.url}`
          this.set(cacheKey, data, request.ttl || 300000)
        }
      } catch (error) {
        console.warn('预热请求失败:', request.url, error)
        this.errorCount++
      }
    })
    
    await Promise.allSettled(promises)
    console.log('缓存预热完成')
  }

  /**
   * 导出缓存数据（用于持久化）
   * @returns {Object} 缓存数据
   */
  export() {
    const data = {
      cache: {},
      metadata: {
        exportTime: Date.now(),
        hitCount: this.hitCount,
        missCount: this.missCount,
        totalRequests: this.totalRequests,
        errorCount: this.errorCount
      }
    }
    
    for (const [key, item] of this.cache) {
      // 只导出未过期的项
      if (!this.isExpired(item)) {
        data.cache[key] = item
      }
    }
    
    return data
  }

  /**
   * 导入缓存数据（用于恢复）
   * @param {Object} data - 缓存数据
   */
  import(data) {
    if (!data || !data.cache) return
    
    // 清空现有缓存
    this.clear()
    
    // 导入缓存项
    for (const [key, item] of Object.entries(data.cache)) {
      // 检查是否过期
      if (!this.isExpired(item)) {
        this.cache.set(key, item)
        this.accessTimes.set(key, item.lastAccessed)
      }
    }
    
    // 恢复统计信息
    if (data.metadata) {
      this.hitCount = data.metadata.hitCount || 0
      this.missCount = data.metadata.missCount || 0
      this.totalRequests = data.metadata.totalRequests || 0
      this.errorCount = data.metadata.errorCount || 0
    }
    
    console.log('缓存数据导入完成:', this.cache.size, '项')
  }

  /**
   * 创建命名空间缓存管理器
   * @param {string} namespace - 命名空间
   * @returns {Object} 命名空间缓存管理器
   */
  createNamespace(namespace) {
    const prefix = `${namespace}:`
    
    return {
      set: (key, value, ttl) => this.set(`${prefix}${key}`, value, ttl),
      get: (key) => this.get(`${prefix}${key}`),
      has: (key) => this.has(`${prefix}${key}`),
      delete: (key) => this.delete(`${prefix}${key}`),
      clear: () => this.deleteByPattern(`^${prefix}`),
      getStats: () => {
        const allStats = this.getStats()
        let namespaceCount = 0
        for (const key of this.cache.keys()) {
          if (key.startsWith(prefix)) {
            namespaceCount++
          }
        }
        return {
          ...allStats,
          namespaceSize: namespaceCount
        }
      }
    }
  }

  /**
   * 页面访问回调（用于路由缓存预热）
   * @param {string} pageName - 页面名称
   * @param {Function} preloadFn - 预加载函数
   */
  onPageVisit(pageName, preloadFn) {
    try {
      console.log(`🔥 缓存预热: ${pageName}`)
      if (typeof preloadFn === 'function') {
        preloadFn()
      }
    } catch (error) {
      console.error(`缓存预热失败 ${pageName}:`, error)
      this.errorCount++
    }
  }

  /**
   * 销毁缓存管理器
   */
  destroy() {
    this.stopAutoCleanup()
    this.stopMonitoring()
    this.clear()
  }
}

// 创建全局缓存管理器实例
const cacheManager = new CacheManager()

// 在页面卸载时清理资源
if (typeof window !== 'undefined') {
  window.addEventListener('beforeunload', () => {
    cacheManager.destroy()
  })
}

export default cacheManager