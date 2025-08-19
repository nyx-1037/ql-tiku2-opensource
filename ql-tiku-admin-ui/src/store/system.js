import { defineStore } from 'pinia'

export const useSystemStore = defineStore('system', {
  state: () => ({
    siteConfig: {
      siteName: '管理后台',
      siteLogo: '',
      copyright: '',
      icpNumber: '',
      icpUrl: '',
      policeNumber: '',
      policeUrl: '',
      policeIcon: ''
    },
    loading: false
  }),
  
  actions: {
    async loadSystemConfig() {
      if (this.loading) return
      
      try {
        this.loading = true
        // 使用公共接口获取系统配置
        const response = await fetch('/api/public/config')
        const result = await response.json()
        
        if (result.code === 200) {
          const config = result.data
          this.siteConfig = {
            siteName: config.siteName || '管理后台',
            siteLogo: config.siteLogo || '',
            copyright: config.copyright || '',
            icpNumber: config.icpBeian || '',
            icpUrl: config.icpBeianUrl || 'http://beian.miit.gov.cn/',
            policeNumber: config.policeBeian || '',
            policeUrl: config.policeBeianUrl || '',
            policeIcon: config.policeBeianIcon || ''
          }
        }
      } catch (error) {
        console.error('获取系统配置失败:', error)
      } finally {
        this.loading = false
      }
    }
  },
  
  persist: {
    key: 'system-config',
    storage: localStorage,
    paths: ['siteConfig']
  }
})