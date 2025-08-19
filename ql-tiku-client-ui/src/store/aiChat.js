import { defineStore } from 'pinia'
import { aiAPI } from '@/api'

export const useAiChatStore = defineStore('aiChat', {
  state: () => ({
    // 当前会话列表
    sessions: [],
    // 当前活跃会话ID
    currentSessionId: '',
    // 聊天消息记录 { sessionId: [messages] }
    chatHistory: {},
    // 是否正在加载
    loading: false,
    // 错误信息
    error: null,
    // 当前用户ID（用于检测用户切换）
    currentUserId: null
  }),

  // 配置持久化
  persist: {
    key: 'ai-chat-store',
    storage: localStorage,
    paths: ['sessions', 'currentSessionId', 'chatHistory', 'currentUserId'],
    beforeRestore: (context) => {
      // 在恢复缓存前检查用户是否切换
      const userInfo = localStorage.getItem('userInfo')
      if (userInfo) {
        try {
          const user = JSON.parse(userInfo)
          const currentUserId = user.id || user.userId
          const storedUserId = context.store.currentUserId
          
          // 如果用户ID不匹配，不恢复缓存
          if (storedUserId && storedUserId !== currentUserId) {
            return false
          }
        } catch (e) {
          console.error('检查用户缓存失败:', e)
        }
      }
      return true
    }
  },

  getters: {
    // 获取当前会话的消息
    currentMessages: (state) => {
      return state.chatHistory[state.currentSessionId] || []
    },

    // 获取会话数量
    sessionCount: (state) => {
      return state.sessions.length
    },

    // 检查是否有活跃会话
    hasActiveSession: (state) => {
      return !!state.currentSessionId
    }
  },

  actions: {
    // 创建新会话
    async createSession() {
      try {
        this.loading = true
        console.log('🆕 AiChatStore: 开始创建新会话')
        
        const response = await aiAPI.createSession()
        console.log('🆕 AiChatStore: 后端响应:', response)
        
        // 后端返回的数据结构：{ sessionId, title, id }
        const sessionId = response.sessionId || response.session_id || response || `session-${Date.now()}`
        const title = response.title || '新对话'
        const id = response.id
        
        console.log('🆕 AiChatStore: 解析会话信息:', { sessionId, title, id })
        
        // 添加到会话列表
        const newSession = {
          id: id,
          sessionId: sessionId,
          title: title,
          createTime: new Date().toISOString(),
          updateTime: new Date().toISOString(),
          lastMessage: '',
          messageCount: 0
        }
        
        this.sessions.unshift(newSession)
        this.currentSessionId = sessionId
        this.chatHistory[sessionId] = []
        
        console.log('✅ AiChatStore: 新会话创建成功:', sessionId)
        return sessionId
      } catch (error) {
        console.error('❌ AiChatStore: 创建会话失败:', error)
        this.error = '创建会话失败'
        // 创建临时会话
        const tempSessionId = `temp-session-${Date.now()}`
        const tempSession = {
          id: null,
          sessionId: tempSessionId,
          title: '临时对话',
          createTime: new Date().toISOString(),
          updateTime: new Date().toISOString(),
          lastMessage: '',
          messageCount: 0
        }
        
        this.sessions.unshift(tempSession)
        this.currentSessionId = tempSessionId
        this.chatHistory[tempSessionId] = []
        
        console.log('⚠️ AiChatStore: 创建临时会话:', tempSessionId)
        return tempSessionId
      } finally {
        this.loading = false
      }
    },

    // 切换会话
    async switchSession(sessionId) {
      console.log('🔄 AiChatStore: 切换会话:', sessionId)
      this.currentSessionId = sessionId

      // 确保会话历史存在
      if (!this.chatHistory[sessionId]) {
        this.chatHistory[sessionId] = []
      }

      // 强制从服务器重新加载历史消息，确保数据最新
      console.log('🌐 AiChatStore: 强制重新加载会话历史消息')
      await this.loadSessionMessages(sessionId)

      console.log('✅ AiChatStore: 会话切换完成:', sessionId, '历史消息数:', this.chatHistory[sessionId].length)
    },

    // 添加消息到当前会话
    addMessage(message) {
      if (!this.currentSessionId) {
        return
      }
      
      if (!this.chatHistory[this.currentSessionId]) {
        this.chatHistory[this.currentSessionId] = []
      }
      
      const messageWithId = {
        id: `msg-${Date.now()}-${Math.random()}`,
        ...message,
        timestamp: message.timestamp || new Date()
      }
      
      this.chatHistory[this.currentSessionId].push(messageWithId)
      
      // 更新会话的最后消息时间
      const session = this.sessions.find(s => s.sessionId === this.currentSessionId)
      if (session) {
        session.updateTime = messageWithId.timestamp
        session.lastMessage = message.content.substring(0, 50) + (message.content.length > 50 ? '...' : '')
        session.messageCount = this.chatHistory[this.currentSessionId].length
        // 如果是用户消息且会话标题还是默认的，更新标题
        if (message.type === 'user' && session.title === '新对话') {
          session.title = message.content.substring(0, 20) + (message.content.length > 20 ? '...' : '')
        }
      }
    },

    // 更新消息内容（用于流式响应）
    updateMessage(messageId, content) {
      if (!this.currentSessionId || !this.chatHistory[this.currentSessionId]) {
        return
      }
      
      const message = this.chatHistory[this.currentSessionId].find(m => m.id === messageId)
      if (message) {
        message.content = content
      }
    },

    // 删除会话
    async deleteSession(sessionId) {
      try {
        await aiAPI.deleteSession(sessionId)
        
        // 从会话列表中移除
        this.sessions = this.sessions.filter(s => s.sessionId !== sessionId)
        
        // 删除聊天历史
        delete this.chatHistory[sessionId]
        
        // 如果删除的是当前会话，切换到其他会话或清空当前会话ID
        if (this.currentSessionId === sessionId) {
          if (this.sessions.length > 0) {
            this.currentSessionId = this.sessions[0].sessionId
          } else {
            this.currentSessionId = ''
          }
        }
        
        return true
      } catch (error) {
        console.error('删除会话失败:', error)
        this.error = '删除会话失败'
        throw error
      }
    },

    // 更新会话标题
    async updateSessionTitle(sessionId, title) {
      try {
        await aiAPI.updateSessionTitle(sessionId, title)
        
        // 更新本地会话标题
        const session = this.sessions.find(s => s.sessionId === sessionId)
        if (session) {
          session.title = title
        }
        
        return true
      } catch (error) {
        console.error('更新会话标题失败:', error)
        this.error = '更新会话标题失败'
        throw error
      }
    },

    // 清空当前会话
    clearCurrentSession() {
      if (this.currentSessionId) {
        this.chatHistory[this.currentSessionId] = []
      }
    },



    // 加载会话列表
    async loadSessionList() {
      try {
        this.loading = true
        console.log('🌐 AiChatStore: 开始加载会话列表')

        const sessions = await aiAPI.getSessionList()
        console.log('📋 AiChatStore: 获取到会话列表:', sessions.length, '个会话')

        // 按更新时间排序
        this.sessions = sessions.sort((a, b) => new Date(b.updateTime) - new Date(a.updateTime))

        // 不再预加载每个会话的详细消息历史，只在点击时加载
        // 只为会话创建空的历史记录占位
        for (const session of this.sessions) {
          if (!this.chatHistory[session.sessionId]) {
            this.chatHistory[session.sessionId] = []
          }
        }

        console.log('✅ AiChatStore: 会话列表加载完成（仅加载列表，不预加载详细内容）')
        return sessions
      } catch (error) {
        console.error('❌ AiChatStore: 加载会话列表失败:', error)
        this.error = '加载会话列表失败'
        return []
      } finally {
        this.loading = false
      }
    },

    // 加载单个会话的消息历史（点击会话时调用）
    async loadSessionMessages(sessionId) {
      try {
        this.loading = true
        console.log('📥 AiChatStore: 开始加载会话消息:', sessionId)
        
        // 强制从服务器重新加载，确保数据最新
        console.log('🌐 AiChatStore: 从服务器加载会话消息历史')
        
        // 从服务器加载消息历史
        const response = await aiAPI.getSessionHistory(sessionId)
        const messages = response.data || response || []
        
        console.log('📨 AiChatStore: 服务器返回消息数量:', messages.length)
        console.log('📨 AiChatStore: 原始消息数据:', messages)
        
        // 转换后端消息格式为前端格式
        const formattedMessages = messages.map(msg => {
          console.log('🔄 转换消息格式:', {
            原始id: msg.id,
            原始messageType: msg.messageType,
            原始message_type: msg.message_type,
            原始content: msg.content?.substring(0, 50) + '...',
            原始createTime: msg.createTime,
            原始create_time: msg.create_time
          })
          
          // 处理消息类型字段，支持多种字段名
          let messageType = msg.messageType || msg.message_type
          if (typeof messageType === 'string') {
            messageType = parseInt(messageType)
          }
          
          // 确保正确的类型映射：数据库中1=用户，2=AI
          const messageRole = messageType === 1 ? 'user' : 'assistant'
          
          // 处理时间字段，支持多种时间格式
          let timestamp = msg.createTime || msg.create_time || msg.timestamp
          
          // 如果时间是字符串格式，需要正确解析
          if (typeof timestamp === 'string') {
            // 处理MySQL datetime格式 "2025-08-07 19:56:23"
            if (timestamp.includes(' ') && !timestamp.includes('T')) {
              // 将MySQL格式转换为ISO格式，但不添加Z（保持本地时区）
              timestamp = timestamp.replace(' ', 'T')
            }
          }
          
          console.log('✅ 消息映射结果:', {
            原始messageType: msg.messageType || msg.message_type,
            转换后messageType: messageType,
            最终role: messageRole,
            原始时间: msg.createTime || msg.create_time,
            处理后时间: timestamp
          })
          
          return {
            id: msg.id || `msg-${Date.now()}-${Math.random()}`,
            role: messageRole, // 使用role字段
            type: messageRole, // 同时保留type字段以兼容
            content: msg.content || '',
            timestamp: timestamp,
            status: 'success'
          }
        })
        
        console.log('📋 AiChatStore: 格式化后的消息:', formattedMessages)
        
        // 更新会话的消息历史
        this.chatHistory[sessionId] = formattedMessages
        
        console.log('✅ AiChatStore: 会话消息加载完成:', sessionId, '消息数:', formattedMessages.length)
        return formattedMessages
      } catch (error) {
        console.error('❌ AiChatStore: 加载会话消息失败:', error)
        this.error = '加载会话消息失败'
        // 确保即使失败也有空数组
        if (!this.chatHistory[sessionId]) {
          this.chatHistory[sessionId] = []
        }
        return []
      } finally {
        this.loading = false
      }
    },

    // 清除错误状态
    clearError() {
      this.error = null
    },

    // 重置store状态
    resetStore() {
      this.sessions = []
      this.currentSessionId = ''
      this.chatHistory = {}
      this.loading = false
      this.error = null
      this.currentUserId = null
    },

    // 检查并处理用户切换
    checkUserSwitch() {
      const userInfo = localStorage.getItem('userInfo')
      if (userInfo) {
        try {
          const user = JSON.parse(userInfo)
          const currentUserId = user.id || user.userId
          
          // 如果用户ID发生变化，重置store
          if (this.currentUserId && this.currentUserId !== currentUserId) {
            console.log('🔄 检测到用户切换，重置AI聊天状态')
            this.resetStore()
          }
          
          this.currentUserId = currentUserId
        } catch (e) {
          console.error('检查用户切换失败:', e)
        }
      }
    },

    // 清除用户缓存（退出登录时调用）
    clearUserCache() {
      console.log('🧹 AiChatStore: 清除用户缓存')
      this.resetStore()
      
      // 清除持久化存储中的数据
      try {
        localStorage.removeItem('ai-chat-store')
        console.log('✅ AiChatStore: 用户缓存清除完成')
      } catch (error) {
        console.error('❌ AiChatStore: 清除用户缓存失败:', error)
      }
    }
  }
})
