/**
 * 缓存插件
 * 将缓存管理器集成到Vue应用中
 */

import cacheManager from '@/utils/cacheManager'
import { cacheUtils } from '@/api/interceptors'
import cacheMonitor from '@/utils/cacheMonitor'

export default {
  install(app, options = {}) {
    console.log('🔌 缓存插件: 开始安装')

    // 将缓存管理器添加到全局属性
    app.config.globalProperties.$cache = {
      manager: cacheManager,
      utils: cacheUtils,
      monitor: cacheMonitor
    }

    // 提供缓存相关的组合式API
    app.provide('cache', {
      manager: cacheManager,
      utils: cacheUtils,
      monitor: cacheMonitor
    })

    // 应用挂载后初始化缓存
    app.mixin({
      async mounted() {
        // 只在根组件中初始化一次
        if (this.$root === this && !this.$cache._initialized) {
          try {
            await cacheManager.init()
            this.$cache._initialized = true
            console.log('✅ 缓存插件: 初始化完成')
          } catch (error) {
            console.error('❌ 缓存插件: 初始化失败', error)
          }
        }
      }
    })

    // 添加全局方法
    app.config.globalProperties.$cacheWarmup = (pageName) => {
      return cacheManager.onPageVisit(pageName)
    }

    app.config.globalProperties.$cacheRefresh = (pageName) => {
      return cacheManager.refreshPage(pageName)
    }

    app.config.globalProperties.$cacheCleanup = (type = 'expired', pattern = null) => {
      return cacheManager.cleanup(type, pattern)
    }

    console.log('✅ 缓存插件: 安装完成')
  }
}

/**
 * 组合式API - 使用缓存
 */
export function useCache() {
  const { inject } = require('vue')
  
  const cache = inject('cache')
  
  if (!cache) {
    throw new Error('缓存插件未正确安装，请在main.js中使用app.use(cachePlugin)')
  }

  return {
    // 缓存管理器
    manager: cache.manager,
    
    // 缓存工具
    utils: cache.utils,
    
    // 性能监控
    monitor: cache.monitor,
    
    // 便捷方法
    warmupPage: (pageName) => cache.manager.onPageVisit ? cache.manager.onPageVisit(pageName) : Promise.resolve(),
    refreshPage: (pageName) => cache.manager.refreshPage ? cache.manager.refreshPage(pageName) : Promise.resolve(),
    cleanup: (type, pattern) => cache.manager.cleanup ? cache.manager.cleanup(type, pattern) : Promise.resolve(),
    getStatus: () => cache.manager.getStatus ? cache.manager.getStatus() : {},
    getStats: () => cache.utils.getStats(),
    getReport: () => cache.monitor.getPerformanceReport ? cache.monitor.getPerformanceReport() : {}
  }
}

/**
 * 页面缓存混入
 * 为页面组件提供自动缓存预热功能
 */
export const pageCacheMixin = {
  data() {
    return {
      _cachePageName: null
    }
  },
  
  async created() {
    // 获取页面名称（从路由或组件名称）
    const pageName = this._cachePageName || 
                    this.$route?.name || 
                    this.$options.name || 
                    'unknown'
    
    if (pageName !== 'unknown') {
      console.log('🔥 页面缓存混入: 预热页面缓存', pageName)
      try {
        await cacheManager.onPageVisit(pageName)
      } catch (error) {
        console.warn('⚠️ 页面缓存混入: 预热失败', pageName, error)
      }
    }
  },
  
  methods: {
    /**
     * 刷新当前页面缓存
     */
    async refreshPageCache() {
      const pageName = this._cachePageName || 
                      this.$route?.name || 
                      this.$options.name
      
      if (pageName) {
        console.log('🔄 页面缓存混入: 刷新页面缓存', pageName)
        await cacheManager.refreshPage(pageName)
      }
    },
    
    /**
     * 清理当前页面缓存
     */
    clearPageCache() {
      const pageName = this._cachePageName || 
                      this.$route?.name || 
                      this.$options.name
      
      if (pageName) {
        console.log('🗑️ 页面缓存混入: 清理页面缓存', pageName)
        cacheManager.cleanup('pattern', pageName)
      }
    }
  }
}

/**
 * 路由缓存守卫
 * 在路由切换时自动预热目标页面缓存
 */
export const createCacheRouterGuard = () => {
  return async (to, from, next) => {
    console.log('🛣️ 路由缓存守卫: 路由切换', from.name, '->', to.name)
    
    // 预热目标页面缓存
    if (to.name) {
      try {
        await cacheManager.onPageVisit(to.name)
        console.log('✅ 路由缓存守卫: 预热完成', to.name)
      } catch (error) {
        console.warn('⚠️ 路由缓存守卫: 预热失败', to.name, error)
      }
    }
    
    next()
  }
}