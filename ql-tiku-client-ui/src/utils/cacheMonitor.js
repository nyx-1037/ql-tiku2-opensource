import cacheManager from './cacheManager'
import { CACHE_MONITORING_CONFIG } from '@/config/cacheConfig'

/**
 * 缓存监控工具
 * 提供实时监控、性能分析、调试工具等功能
 */
class CacheMonitor {
  constructor() {
    this.isMonitoring = false
    this.performanceHistory = []
    this.maxHistoryLength = 100
    this.alertCallbacks = []
  }

  /**
   * 开始监控
   */
  start() {
    if (this.isMonitoring) return
    
    this.isMonitoring = true
    console.log('🔍 缓存监控已启动')
    
    // 定期收集性能数据
    this.collectInterval = setInterval(() => {
      this.collectPerformanceData()
    }, 30000) // 每30秒收集一次
    
    // 实时监控告警
    this.monitorInterval = setInterval(() => {
      this.checkAlerts()
    }, 10000) // 每10秒检查一次告警
  }

  /**
   * 停止监控
   */
  stop() {
    if (!this.isMonitoring) return
    
    this.isMonitoring = false
    
    if (this.collectInterval) {
      clearInterval(this.collectInterval)
      this.collectInterval = null
    }
    
    if (this.monitorInterval) {
      clearInterval(this.monitorInterval)
      this.monitorInterval = null
    }
    
    console.log('🔍 缓存监控已停止')
  }

  /**
   * 收集性能数据
   */
  collectPerformanceData() {
    const stats = cacheManager.getStats()
    const timestamp = Date.now()
    
    const performanceData = {
      timestamp,
      time: new Date(timestamp).toLocaleTimeString(),
      ...stats
    }
    
    this.performanceHistory.push(performanceData)
    
    // 保持历史记录在限制范围内
    if (this.performanceHistory.length > this.maxHistoryLength) {
      this.performanceHistory.shift()
    }
  }

  /**
   * 检查告警条件
   */
  checkAlerts() {
    const stats = cacheManager.getStats()
    const thresholds = CACHE_MONITORING_CONFIG.thresholds
    const alerts = []
    
    // 检查各项指标
    if (stats.hitRate < thresholds.hitRate) {
      alerts.push({
        type: 'hitRate',
        level: 'warning',
        message: `缓存命中率过低: ${(stats.hitRate * 100).toFixed(1)}%`,
        value: stats.hitRate,
        threshold: thresholds.hitRate,
        suggestion: '考虑调整缓存策略或增加缓存时间'
      })
    }
    
    if (stats.memoryUsage > thresholds.memoryUsage) {
      alerts.push({
        type: 'memoryUsage',
        level: 'error',
        message: `内存使用过高: ${stats.memoryUsage}MB`,
        value: stats.memoryUsage,
        threshold: thresholds.memoryUsage,
        suggestion: '执行缓存清理或减少缓存项数量'
      })
    }
    
    if (stats.errorRate > thresholds.errorRate) {
      alerts.push({
        type: 'errorRate',
        level: 'error',
        message: `错误率过高: ${(stats.errorRate * 100).toFixed(1)}%`,
        value: stats.errorRate,
        threshold: thresholds.errorRate,
        suggestion: '检查API请求或缓存逻辑是否有问题'
      })
    }
    
    // 触发告警回调
    if (alerts.length > 0) {
      this.triggerAlerts(alerts)
    }
  }

  /**
   * 触发告警
   * @param {Array} alerts - 告警列表
   */
  triggerAlerts(alerts) {
    alerts.forEach(alert => {
      console.warn(`⚠️ 缓存告警 [${alert.type}]:`, alert.message)
      
      // 执行注册的告警回调
      this.alertCallbacks.forEach(callback => {
        try {
          callback(alert)
        } catch (error) {
          console.error('告警回调执行失败:', error)
        }
      })
    })
  }

  /**
   * 注册告警回调
   * @param {Function} callback - 告警回调函数
   */
  onAlert(callback) {
    if (typeof callback === 'function') {
      this.alertCallbacks.push(callback)
    }
  }

  /**
   * 获取性能历史数据
   * @returns {Array} 性能历史
   */
  getPerformanceHistory() {
    return [...this.performanceHistory]
  }

  /**
   * 获取性能趋势分析
   * @returns {Object} 趋势分析
   */
  getPerformanceTrends() {
    if (this.performanceHistory.length < 2) {
      return { message: '数据不足，无法分析趋势' }
    }
    
    const recent = this.performanceHistory.slice(-10) // 最近10个数据点
    const older = this.performanceHistory.slice(-20, -10) // 之前10个数据点
    
    if (older.length === 0) {
      return { message: '历史数据不足，无法比较趋势' }
    }
    
    const recentAvg = {
      hitRate: recent.reduce((sum, item) => sum + item.hitRate, 0) / recent.length,
      memoryUsage: recent.reduce((sum, item) => sum + item.memoryUsage, 0) / recent.length,
      size: recent.reduce((sum, item) => sum + item.size, 0) / recent.length
    }
    
    const olderAvg = {
      hitRate: older.reduce((sum, item) => sum + item.hitRate, 0) / older.length,
      memoryUsage: older.reduce((sum, item) => sum + item.memoryUsage, 0) / older.length,
      size: older.reduce((sum, item) => sum + item.size, 0) / older.length
    }
    
    return {
      hitRate: {
        current: recentAvg.hitRate,
        previous: olderAvg.hitRate,
        trend: recentAvg.hitRate > olderAvg.hitRate ? 'improving' : 'declining',
        change: ((recentAvg.hitRate - olderAvg.hitRate) * 100).toFixed(2) + '%'
      },
      memoryUsage: {
        current: recentAvg.memoryUsage,
        previous: olderAvg.memoryUsage,
        trend: recentAvg.memoryUsage < olderAvg.memoryUsage ? 'improving' : 'increasing',
        change: (recentAvg.memoryUsage - olderAvg.memoryUsage).toFixed(2) + 'MB'
      },
      cacheSize: {
        current: recentAvg.size,
        previous: olderAvg.size,
        trend: recentAvg.size > olderAvg.size ? 'growing' : 'shrinking',
        change: Math.round(recentAvg.size - olderAvg.size) + ' items'
      }
    }
  }

  /**
   * 生成性能报告
   * @returns {Object} 性能报告
   */
  generateReport() {
    const stats = cacheManager.getStats()
    const trends = this.getPerformanceTrends()
    const debugInfo = cacheManager.getDebugInfo()
    
    return {
      timestamp: Date.now(),
      summary: {
        status: this.getOverallStatus(stats),
        ...stats
      },
      trends,
      topCachedItems: debugInfo.slice(0, 10), // 前10个最常访问的缓存项
      recommendations: this.generateRecommendations(stats, trends),
      alerts: this.getActiveAlerts(stats)
    }
  }

  /**
   * 获取整体状态
   * @param {Object} stats - 统计数据
   * @returns {string} 状态
   */
  getOverallStatus(stats) {
    const thresholds = CACHE_MONITORING_CONFIG.thresholds
    
    if (stats.errorRate > thresholds.errorRate) return 'error'
    if (stats.memoryUsage > thresholds.memoryUsage) return 'warning'
    if (stats.hitRate < thresholds.hitRate) return 'warning'
    
    return 'healthy'
  }

  /**
   * 生成优化建议
   * @param {Object} stats - 统计数据
   * @param {Object} trends - 趋势数据
   * @returns {Array} 建议列表
   */
  generateRecommendations(stats, trends) {
    const recommendations = []
    const thresholds = CACHE_MONITORING_CONFIG.thresholds
    
    // 命中率建议
    if (stats.hitRate < thresholds.hitRate) {
      recommendations.push({
        type: 'hitRate',
        priority: 'high',
        title: '提升缓存命中率',
        description: `当前命中率${(stats.hitRate * 100).toFixed(1)}%，建议调整缓存策略`,
        actions: [
          '增加常用API的缓存时间',
          '实现缓存预热机制',
          '优化缓存键的设计'
        ]
      })
    }
    
    // 内存使用建议
    if (stats.memoryUsage > thresholds.memoryUsage * 0.8) {
      recommendations.push({
        type: 'memory',
        priority: stats.memoryUsage > thresholds.memoryUsage ? 'high' : 'medium',
        title: '优化内存使用',
        description: `内存使用${stats.memoryUsage}MB，接近或超过阈值`,
        actions: [
          '减少缓存项的TTL时间',
          '清理低频访问的缓存项',
          '压缩缓存数据结构'
        ]
      })
    }
    
    // 趋势建议
    if (trends.hitRate && trends.hitRate.trend === 'declining') {
      recommendations.push({
        type: 'trend',
        priority: 'medium',
        title: '命中率下降趋势',
        description: '缓存命中率呈下降趋势，需要关注',
        actions: [
          '分析最近的API访问模式变化',
          '检查缓存失效策略是否合理',
          '考虑调整缓存预热策略'
        ]
      })
    }
    
    return recommendations
  }

  /**
   * 获取当前活跃告警
   * @param {Object} stats - 统计数据
   * @returns {Array} 告警列表
   */
  getActiveAlerts(stats) {
    const alerts = []
    const thresholds = CACHE_MONITORING_CONFIG.thresholds
    
    if (stats.hitRate < thresholds.hitRate) {
      alerts.push({
        type: 'hitRate',
        level: 'warning',
        message: `缓存命中率过低: ${(stats.hitRate * 100).toFixed(1)}%`
      })
    }
    
    if (stats.memoryUsage > thresholds.memoryUsage) {
      alerts.push({
        type: 'memoryUsage',
        level: 'error',
        message: `内存使用过高: ${stats.memoryUsage}MB`
      })
    }
    
    if (stats.errorRate > thresholds.errorRate) {
      alerts.push({
        type: 'errorRate',
        level: 'error',
        message: `错误率过高: ${(stats.errorRate * 100).toFixed(1)}%`
      })
    }
    
    return alerts
  }

  /**
   * 导出监控数据
   * @returns {Object} 监控数据
   */
  exportData() {
    return {
      performanceHistory: this.performanceHistory,
      currentStats: cacheManager.getStats(),
      exportTime: Date.now(),
      monitoringConfig: CACHE_MONITORING_CONFIG
    }
  }

  /**
   * 清空历史数据
   */
  clearHistory() {
    this.performanceHistory = []
    console.log('监控历史数据已清空')
  }

  /**
   * 获取实时监控面板数据
   * @returns {Object} 面板数据
   */
  getDashboardData() {
    const stats = cacheManager.getStats()
    const trends = this.getPerformanceTrends()
    
    return {
      // 核心指标
      metrics: {
        hitRate: {
          value: stats.hitRate,
          display: `${(stats.hitRate * 100).toFixed(1)}%`,
          status: stats.hitRate >= 0.7 ? 'good' : 'warning'
        },
        memoryUsage: {
          value: stats.memoryUsage,
          display: `${stats.memoryUsage}MB`,
          status: stats.memoryUsage <= 50 ? 'good' : 'warning'
        },
        cacheSize: {
          value: stats.size,
          display: `${stats.size} 项`,
          status: stats.size <= 1000 ? 'good' : 'warning'
        },
        errorRate: {
          value: stats.errorRate,
          display: `${(stats.errorRate * 100).toFixed(1)}%`,
          status: stats.errorRate <= 0.05 ? 'good' : 'error'
        }
      },
      
      // 趋势指标
      trends: trends.message ? null : {
        hitRate: trends.hitRate,
        memoryUsage: trends.memoryUsage,
        cacheSize: trends.cacheSize
      },
      
      // 最近性能数据（用于图表）
      chartData: this.performanceHistory.slice(-20).map(item => ({
        time: item.time,
        hitRate: item.hitRate * 100,
        memoryUsage: item.memoryUsage,
        size: item.size
      })),
      
      // 告警状态
      alerts: this.getActiveAlerts(stats),
      
      // 系统状态
      systemStatus: this.getOverallStatus(stats)
    }
  }
}

// 创建全局监控实例
const cacheMonitor = new CacheMonitor()

// 开发环境自动启动监控
if (process.env.NODE_ENV === 'development') {
  cacheMonitor.start()
  
  // 添加全局调试方法
  if (typeof window !== 'undefined') {
    window.cacheMonitor = cacheMonitor
    window.cacheManager = cacheManager
    
    // 添加控制台快捷命令
    console.log('🔧 缓存调试工具已加载:')
    console.log('  - window.cacheMonitor: 缓存监控工具')
    console.log('  - window.cacheManager: 缓存管理器')
    console.log('  - cacheMonitor.generateReport(): 生成性能报告')
    console.log('  - cacheManager.getDebugInfo(): 获取缓存详情')
  }
}

export default cacheMonitor
