import { defineStore } from 'pinia'

export const useToolboxStore = defineStore('toolbox', {
  state: () => ({
    // 工具箱弹窗状态
    dialogVisible: false,
    // iframe加载状态
    iframeLoaded: false,
    // 首次加载标记
    isFirstLoad: true,
    // iframe实例缓存
    iframeElement: null,
    // 缓存的iframe状态数据
    cachedState: {
      url: 'https://tikutools.nie1037.cn/',
      lastActiveTime: null,
      sessionData: null,
      scrollPosition: { x: 0, y: 0 }
    },
    // 后台运行状态
    isBackgroundRunning: false,
    // 用户操作历史
    operationHistory: [],
    // 最后交互时间
    lastInteractionTime: null
  }),

  getters: {
    // 是否已经初始化过
    isInitialized: (state) => !state.isFirstLoad && state.iframeElement !== null,
    
    // 是否需要显示加载状态
    shouldShowLoading: (state) => state.isFirstLoad || (!state.iframeLoaded && state.dialogVisible),
    
    // 获取缓存的会话时长
    sessionDuration: (state) => {
      if (!state.lastInteractionTime) return 0
      return Date.now() - state.lastInteractionTime
    }
  },

  actions: {
    // 打开工具箱
    openToolbox() {
      console.log('🔧 toolbox store: 打开工具箱', {
        dialogVisible: this.dialogVisible,
        iframeLoaded: this.iframeLoaded,
        isFirstLoad: this.isFirstLoad,
        isInitialized: this.isInitialized
      })
      
      this.dialogVisible = true
      this.lastInteractionTime = Date.now()
      
      // 记录操作历史
      this.addOperationHistory('open_toolbox')
      
      // 如果不是首次加载且已经初始化，直接显示缓存的iframe
      if (this.isInitialized) {
        this.iframeLoaded = true
        console.log('🔧 toolbox store: 使用缓存的iframe，直接显示')
      } else {
        // 首次加载或未初始化，需要重新加载
        this.iframeLoaded = false
        console.log('🔧 toolbox store: 首次加载或未初始化，需要重新加载iframe')
      }
    },

    // 关闭工具箱（不销毁iframe，保持后台运行）
    closeToolbox() {
      console.log('🔧 toolbox store: 关闭工具箱（保持后台运行）')
      this.dialogVisible = false
      this.isBackgroundRunning = true
      this.lastInteractionTime = Date.now()
      
      // 记录操作历史
      this.addOperationHistory('close_toolbox')
      
      // 保存当前状态
      this.saveCurrentState()
    },

    // 设置iframe加载完成
    setIframeLoaded(iframeElement = null) {
      console.log('🔧 toolbox store: 设置iframe加载完成')
      this.iframeLoaded = true
      this.isFirstLoad = false
      this.lastInteractionTime = Date.now()
      
      // 缓存iframe元素引用
      if (iframeElement) {
        this.iframeElement = iframeElement
        console.log('🔧 toolbox store: 缓存iframe元素引用')
      }
      
      // 记录操作历史
      this.addOperationHistory('iframe_loaded')
      
      // 尝试恢复之前的状态
      this.restoreState()
    },

    // 保存当前状态
    saveCurrentState() {
      try {
        if (this.iframeElement && this.iframeElement.contentWindow) {
          // 尝试获取iframe内的滚动位置（如果同源）
          try {
            const scrollX = this.iframeElement.contentWindow.scrollX || 0
            const scrollY = this.iframeElement.contentWindow.scrollY || 0
            this.cachedState.scrollPosition = { x: scrollX, y: scrollY }
            console.log('🔧 toolbox store: 保存滚动位置', this.cachedState.scrollPosition)
          } catch (e) {
            console.log('🔧 toolbox store: 无法获取iframe滚动位置（跨域限制）')
          }
          
          // 保存当前URL
          try {
            this.cachedState.url = this.iframeElement.contentWindow.location.href
            console.log('🔧 toolbox store: 保存当前URL', this.cachedState.url)
          } catch (e) {
            console.log('🔧 toolbox store: 无法获取iframe URL（跨域限制）')
          }
        }
        
        this.cachedState.lastActiveTime = Date.now()
        console.log('🔧 toolbox store: 状态保存完成')
      } catch (error) {
        console.error('🔧 toolbox store: 保存状态失败', error)
      }
    },

    // 恢复状态
    restoreState() {
      try {
        if (this.cachedState.lastActiveTime && this.iframeElement) {
          console.log('🔧 toolbox store: 尝试恢复之前的状态')
          
          // 如果有保存的滚动位置，尝试恢复
          if (this.cachedState.scrollPosition.x || this.cachedState.scrollPosition.y) {
            setTimeout(() => {
              try {
                if (this.iframeElement.contentWindow) {
                  this.iframeElement.contentWindow.scrollTo(
                    this.cachedState.scrollPosition.x,
                    this.cachedState.scrollPosition.y
                  )
                  console.log('🔧 toolbox store: 恢复滚动位置成功')
                }
              } catch (e) {
                console.log('🔧 toolbox store: 恢复滚动位置失败（跨域限制）')
              }
            }, 1000) // 延迟1秒确保iframe内容加载完成
          }
        }
      } catch (error) {
        console.error('🔧 toolbox store: 恢复状态失败', error)
      }
    },

    // 添加操作历史
    addOperationHistory(operation) {
      const historyItem = {
        operation,
        timestamp: Date.now(),
        sessionId: this.generateSessionId()
      }
      
      this.operationHistory.push(historyItem)
      
      // 限制历史记录数量，避免内存泄漏
      if (this.operationHistory.length > 100) {
        this.operationHistory = this.operationHistory.slice(-50)
      }
      
      console.log('🔧 toolbox store: 添加操作历史', historyItem)
    },

    // 生成会话ID
    generateSessionId() {
      return `toolbox_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`
    },

    // 获取操作统计
    getOperationStats() {
      const stats = {
        totalOperations: this.operationHistory.length,
        sessionDuration: this.sessionDuration,
        lastActiveTime: this.lastInteractionTime,
        isBackgroundRunning: this.isBackgroundRunning,
        operationTypes: {}
      }
      
      // 统计操作类型
      this.operationHistory.forEach(item => {
        stats.operationTypes[item.operation] = (stats.operationTypes[item.operation] || 0) + 1
      })
      
      return stats
    },

    // 清除缓存和历史
    clearCache() {
      console.log('🔧 toolbox store: 清除缓存和历史')
      this.iframeElement = null
      this.iframeLoaded = false
      this.isFirstLoad = true
      this.isBackgroundRunning = false
      this.operationHistory = []
      this.lastInteractionTime = null
      this.cachedState = {
        url: 'https://tikutools.nie1037.cn/',
        lastActiveTime: null,
        sessionData: null,
        scrollPosition: { x: 0, y: 0 }
      }
    },

    // 强制重新加载
    forceReload() {
      console.log('🔧 toolbox store: 强制重新加载')
      this.clearCache()
      this.addOperationHistory('force_reload')
    },

    // 更新交互时间
    updateInteractionTime() {
      this.lastInteractionTime = Date.now()
      if (this.isBackgroundRunning) {
        this.isBackgroundRunning = false
        console.log('🔧 toolbox store: 从后台运行状态恢复到活跃状态')
      }
    }
  },

  // 启用持久化存储（仅存储必要的状态）
  persist: {
    key: 'toolbox-cache',
    storage: localStorage,
    paths: ['cachedState', 'operationHistory', 'lastInteractionTime', 'isFirstLoad']
  }
})
