import { defineStore } from 'pinia'
import { publicAPI, subjectAPI } from '../api'

export const useSystemStore = defineStore('system', {
  state: () => ({
    siteConfig: {
      siteName: '在线刷题系统',
      siteLogo: '',
      siteDescription: '',
      copyright: '',
      policeBeian: '',
      allowRegister: true,
      maintenanceMode: false,
      maintenanceMessage: '',
      systemVersion: 'v2.1.0'
    },
    loading: false,

  }),
  
  getters: {
    // 检查是否允许注册
    isRegistrationAllowed: (state) => {
      return state.siteConfig.allowRegister && !state.siteConfig.maintenanceMode
    },
    
    // 检查是否在维护模式
    isMaintenanceMode: (state) => {
      return state.siteConfig.maintenanceMode
    },
    
    // 获取系统配置
    systemConfig: (state) => state.siteConfig
  },
  
  actions: {
    // 加载系统配置 - 强化错误处理和降级方案
    async loadSystemConfig() {
      // 避免重复加载
      if (this.loading) {
        console.log('SystemStore: 配置正在加载中，跳过重复请求')
        return this.siteConfig
      }
      
      // 设置默认配置，确保基础功能可用
      const defaultConfig = {
        siteName: '七洛题库',
        siteLogo: '',
        siteDescription: '在线刷题系统',
        copyright: '© 2025 七洛题库 保留所有权利',
        policeBeian: '',
        icpBeian: '',
        icpUrl: '',
        policeIcon: '',
        policeUrl: '',
        allowRegister: true,
        registrationCodeRequired: false,
        maintenanceMode: false,
        maintenanceMessage: ''
      }
      
      try {
        this.loading = true
        console.log('SystemStore: 开始加载系统配置')
        
        // 使用更短的超时时间，快速失败
        const timeoutPromise = new Promise((_, reject) => {
          setTimeout(() => reject(new Error('配置加载超时')), 3000)
        })
        
        const configPromise = publicAPI.getPublicConfig()
        
        // 竞速：3秒内获取配置，否则使用默认配置
        const config = await Promise.race([configPromise, timeoutPromise])
        
        console.log('SystemStore: 获取到系统配置:', config)
        
        // 更新配置状态
        this.siteConfig = {
          siteName: config.siteName || defaultConfig.siteName,
          siteLogo: config.siteLogo || defaultConfig.siteLogo,
          siteDescription: config.siteDescription || defaultConfig.siteDescription,
          copyright: config.copyright || defaultConfig.copyright,
          policeBeian: config.policeBeian || defaultConfig.policeBeian,
          icpBeian: config.icpBeian || defaultConfig.icpBeian,
          icpUrl: config.icpUrl || defaultConfig.icpUrl,
          policeIcon: config.policeIcon || defaultConfig.policeIcon,
          policeUrl: config.policeUrl || defaultConfig.policeUrl,
          allowRegister: config.allowRegister !== undefined ? config.allowRegister : defaultConfig.allowRegister,
          registrationCodeRequired: config.registrationCodeRequired || defaultConfig.registrationCodeRequired,
          maintenanceMode: config.maintenanceMode || defaultConfig.maintenanceMode,
          maintenanceMessage: config.maintenanceMessage || defaultConfig.maintenanceMessage,
          version: config.siteVersion || config.version || this.siteConfig.systemVersion || 'v2.1.0'
        }
        
        console.log('SystemStore: 系统配置加载成功')
        return this.siteConfig
        
      } catch (error) {
        console.warn('SystemStore: 系统配置加载失败，使用默认配置:', error.message)
        
        // 使用默认配置，确保应用正常运行
        this.siteConfig = defaultConfig
        console.log('SystemStore: 已应用默认配置，应用将正常运行')
        return this.siteConfig
        
      } finally {
        this.loading = false
        console.log('SystemStore: 配置加载流程完成')
      }
    },
    
    // 重置配置
    resetConfig() {
      this.siteConfig = {
        siteName: '在线刷题系统',
        siteLogo: '',
        siteDescription: '',
        copyright: '',
        policeBeian: '',
        icpBeian: '',
        icpUrl: '',
        policeIcon: '',
        policeUrl: '',
        allowRegister: true,
        registrationCodeRequired: false,
        maintenanceMode: false,
        maintenanceMessage: ''
      }
      this.loading = false
    },


  }

  // 移除持久化配置 - 系统配置每次重新获取，避免缓存问题
})