/**
 * 缓存配置文件
 * 统一管理各个页面和API的缓存策略
 */

// 缓存时间常量（毫秒）
export const CACHE_DURATION = {
  VERY_SHORT: 2 * 60 * 1000,    // 2分钟 - 实时性要求高的数据
  SHORT: 5 * 60 * 1000,         // 5分钟 - 一般实时数据
  MEDIUM: 10 * 60 * 1000,       // 10分钟 - 中等更新频率
  LONG: 30 * 60 * 1000,         // 30分钟 - 相对稳定的数据
  VERY_LONG: 60 * 60 * 1000     // 1小时 - 很少变化的数据
}

// 页面缓存配置
export const PAGE_CACHE_CONFIG = {
  // 分析页面
  analytics: {
    defaultTTL: CACHE_DURATION.SHORT,
    apis: {
      '/api/statistics/personal': CACHE_DURATION.SHORT,
      '/api/statistics/overview': CACHE_DURATION.SHORT,
      '/api/statistics/trend': CACHE_DURATION.MEDIUM,
      '/api/statistics/accuracy': CACHE_DURATION.MEDIUM,
      '/api/statistics/subject-distribution': CACHE_DURATION.MEDIUM,
      '/api/statistics/difficulty': CACHE_DURATION.LONG,
      '/api/statistics/wrong-analysis': CACHE_DURATION.SHORT,
      '/api/statistics/suggestions': CACHE_DURATION.MEDIUM
    }
  },

  // 反馈中心
  feedbackCenter: {
    defaultTTL: CACHE_DURATION.SHORT,
    apis: {
      '/api/feedback': CACHE_DURATION.SHORT,
      '/api/feedback/search': CACHE_DURATION.VERY_SHORT
    }
  },

  // 练习记录
  practiceRecord: {
    defaultTTL: CACHE_DURATION.VERY_SHORT,
    apis: {
      '/api/practice-record/list': CACHE_DURATION.VERY_SHORT,
      '/api/practice-record/details': CACHE_DURATION.SHORT,
      '/api/practice-record/stats': CACHE_DURATION.SHORT
    }
  },

  // 资源库
  resourceLibrary: {
    defaultTTL: CACHE_DURATION.MEDIUM,
    apis: {
      '/api/files': CACHE_DURATION.MEDIUM,
      '/api/files/search': CACHE_DURATION.SHORT
    }
  },

  // 题库
  questionBank: {
    defaultTTL: CACHE_DURATION.MEDIUM,
    apis: {
      '/api/subjects': CACHE_DURATION.LONG,
      '/api/question': CACHE_DURATION.MEDIUM,
      '/api/question/search': CACHE_DURATION.SHORT,
      '/api/question/random': CACHE_DURATION.VERY_SHORT,
      '/api/question/practice': CACHE_DURATION.VERY_SHORT
    }
  }
}

// API缓存策略配置
export const API_CACHE_STRATEGIES = {
  // 高频访问，短缓存
  HIGH_FREQUENCY: {
    ttl: CACHE_DURATION.VERY_SHORT,
    description: '高频访问数据，如练习记录、实时统计'
  },

  // 中频访问，中等缓存
  MEDIUM_FREQUENCY: {
    ttl: CACHE_DURATION.MEDIUM,
    description: '中频访问数据，如题目列表、文件列表'
  },

  // 低频访问，长缓存
  LOW_FREQUENCY: {
    ttl: CACHE_DURATION.LONG,
    description: '低频访问数据，如科目列表、系统配置'
  },

  // 静态数据，超长缓存
  STATIC_DATA: {
    ttl: CACHE_DURATION.VERY_LONG,
    description: '静态数据，如难度分析、历史趋势'
  }
}

// 缓存预热配置
export const CACHE_WARMUP_CONFIG = {
  // 应用启动时预热的请求（仅包含无需认证的公共API）
  onAppStart: [
    { url: '/api/public/config', method: 'GET' },
    { url: '/api/captcha/config', method: 'GET' }
  ],

  // 用户登录后预热的请求（需要认证的API）
  onUserLogin: [
    { url: '/api/subjects', method: 'GET', requireAuth: true },
    { url: '/api/statistics/personal', method: 'GET', requireAuth: true },
    { url: '/api/ai-quota/info', method: 'GET', requireAuth: true }
  ],

  // 页面访问时预热的请求
  onPageVisit: {
    analytics: [
      { url: '/api/statistics/overview', method: 'GET', requireAuth: true },
      { url: '/api/statistics/trend', method: 'GET', requireAuth: true }
    ],
    questionBank: [
      { url: '/api/subjects', method: 'GET', requireAuth: true }
    ],
    practiceRecord: [
      { url: '/api/practice-record/list', method: 'GET', params: { page: 1, size: 10 }, requireAuth: true }
    ]
  }
}

// 缓存清理策略
export const CACHE_CLEANUP_CONFIG = {
  // 自动清理间隔（毫秒）
  autoCleanupInterval: 30 * 60 * 1000, // 30分钟

  // 内存使用阈值（MB）
  memoryThreshold: 50,

  // 缓存项数量阈值
  itemCountThreshold: 1000,

  // 需要定期清理的缓存模式
  cleanupPatterns: [
    'GET_/api/question/random*',
    'GET_/api/question/practice*',
    'GET_/api/practice-record/list*'
  ]
}

// 缓存性能监控配置
export const CACHE_MONITORING_CONFIG = {
  // 是否启用性能监控
  enabled: process.env.NODE_ENV === 'development',

  // 监控指标
  metrics: {
    hitRate: true,        // 缓存命中率
    responseTime: true,   // 响应时间
    memoryUsage: true,    // 内存使用量
    errorRate: true       // 错误率
  },

  // 性能报告间隔（毫秒）
  reportInterval: 5 * 60 * 1000, // 5分钟

  // 性能阈值告警
  thresholds: {
    hitRate: process.env.NODE_ENV === 'development' ? 0.1 : 0.7,  // 开发环境10%，生产环境70%
    responseTime: 1000,   // 响应时间超过1秒告警
    memoryUsage: 100,     // 内存使用超过100MB告警
    errorRate: 0.05       // 错误率超过5%告警
  }
}

/**
 * 根据URL获取缓存配置
 * @param {string} url - API URL
 * @returns {Object} 缓存配置
 */
export const getCacheConfigByUrl = (url) => {
  // 遍历页面配置，查找匹配的API
  for (const [pageName, pageConfig] of Object.entries(PAGE_CACHE_CONFIG)) {
    for (const [apiPath, ttl] of Object.entries(pageConfig.apis)) {
      if (url.includes(apiPath)) {
        return {
          ttl,
          strategy: getStrategyByTTL(ttl),
          page: pageName
        }
      }
    }
  }

  // 默认配置
  return {
    ttl: CACHE_DURATION.MEDIUM,
    strategy: 'MEDIUM_FREQUENCY',
    page: 'unknown'
  }
}

/**
 * 根据TTL获取缓存策略名称
 * @param {number} ttl - 缓存时间
 * @returns {string} 策略名称
 */
const getStrategyByTTL = (ttl) => {
  if (ttl <= CACHE_DURATION.VERY_SHORT) return 'HIGH_FREQUENCY'
  if (ttl <= CACHE_DURATION.MEDIUM) return 'MEDIUM_FREQUENCY'
  if (ttl <= CACHE_DURATION.LONG) return 'LOW_FREQUENCY'
  return 'STATIC_DATA'
}

/**
 * 获取页面预热配置
 * @param {string} pageName - 页面名称
 * @returns {Array} 预热请求配置
 */
export const getWarmupConfig = (pageName) => {
  return CACHE_WARMUP_CONFIG.onPageVisit[pageName] || []
}

export default {
  CACHE_DURATION,
  PAGE_CACHE_CONFIG,
  API_CACHE_STRATEGIES,
  CACHE_WARMUP_CONFIG,
  CACHE_CLEANUP_CONFIG,
  CACHE_MONITORING_CONFIG,
  getCacheConfigByUrl,
  getWarmupConfig
}