<template>
  <div class="ai-chat-container">
    <!-- 聊天头部 -->
    <div class="chat-header">
      <div class="header-left">
        <i class="t-icon-chat-dot-round"></i>
        <span class="title">AI助手</span>
      </div>
      <div class="header-right">
        <t-button
          v-if="isTyping"
          size="small" 
          type="warning" 
          @click="stopGeneration"
          icon="t-icon-video-pause"
        >
          暂停
        </t-button>
        <t-button
          size="small" 
          type="text" 
          @click="clearChat"
          :disabled="messages.length === 0"
        >
          新建对话
        </t-button>
      </div>
    </div>

    <!-- 聊天消息区域 -->
    <div class="chat-messages" ref="messagesContainer">
      <div 
        v-for="(message, index) in messages" 
        :key="index" 
        :class="['message', message.type]"
      >
        <div class="message-avatar">
          <div v-if="message.type === 'user'" class="user-avatar">
            <img v-if="userAvatar" :src="userAvatar" alt="用户头像" class="avatar-img" />
            <i v-else class="t-icon-user"></i>
          </div>
          <div v-else class="ai-avatar">
            <span class="ai-text">AI</span>
          </div>
        </div>
        <div class="message-content">
          <div class="message-text">
            <div v-if="message.type === 'user'" v-html="message.content"></div>
            <div v-else v-html="renderMarkdown(message.content)"></div>
          </div>
          <div class="message-time">{{ formatTime(message.timestamp) }}</div>
        </div>
      </div>
      
      <!-- AI正在输入提示 -->
      <div v-if="isTyping" class="message ai typing">
        <div class="message-avatar">
          <div class="ai-avatar">
            <span class="ai-text">AI</span>
          </div>
        </div>
        <div class="message-content">
          <div class="typing-indicator">
            <span></span>
            <span></span>
            <span></span>
          </div>
        </div>
      </div>
    </div>

    <!-- 输入区域 -->
    <div class="chat-input">
      <t-input
        v-model="inputMessage"
        type="textarea"
        :rows="2"
        placeholder="请输入您的问题..."
        @keydown.enter.prevent="handleEnter"
        :disabled="isTyping"
      ></t-input>
      <t-button
        type="primary" 
        @click="sendMessage"
        :loading="isTyping"
        :disabled="!inputMessage.trim()"
        class="send-btn"
      >
        发送
      </t-button>
    </div>
  </div>
</template>

<script>
import { aiAPI } from '@/api'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import { useAiChatStore } from '@/store/aiChat'
import { mapState, mapActions } from 'pinia'

export default {
  name: 'AiChat',
  props: {
    // 初始问题（用于题目解析）
    initialQuestion: {
      type: String,
      default: ''
    },
    // 会话ID
    sessionId: {
      type: String,
      default: ''
    },
    // 是否为题目解析模式
    isQuestionMode: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      inputMessage: '',
      isTyping: false,
      currentMessageId: null, // 用于流式响应时的消息ID
      abortController: null, // 用于取消请求
      currentReader: null // 当前的流式读取器
    }
  },
  computed: {
    ...mapState(useAiChatStore, ['currentMessages', 'currentSessionId', 'loading']),
    messages() {
      return this.currentMessages
    },
    userAvatar() {
      // 从localStorage获取用户信息
      const userInfo = localStorage.getItem('userInfo')
      if (userInfo) {
        try {
          const user = JSON.parse(userInfo)
          return user.avatar || null
        } catch (e) {
          console.error('解析用户信息失败:', e)
          return null
        }
      }
      return null
    }
  },
  mounted() {
    this.initChat()
  },
  watch: {
    initialQuestion(newVal, oldVal) {
      // 只有在值真正改变且不为空时才触发分析
      if (newVal && this.isQuestionMode && newVal !== oldVal) {
        this.analyzeQuestion(newVal)
      }
    }
  },
  methods: {
    ...mapActions(useAiChatStore, [
      'createSession',
      'switchSession', 
      'addMessage',
      'updateMessage',
      'clearCurrentSession',
      'saveChatRecord',
      'loadSessionHistory',
      'checkUserSwitch'
    ]),
    // 初始化聊天
    async initChat(sessionId = null) {
      // 检查用户是否切换
      const userSwitched = this.checkUserSwitch()
      
      if (userSwitched) {
        // 用户已切换，清理当前状态
        this.inputMessage = ''
        this.isTyping = false
        this.currentMessageId = null
        
        // 如果有传入的sessionId，使用它；否则等待父组件处理
        if (sessionId) {
          this.switchSession(sessionId)
          await this.loadSessionHistory(sessionId)
        }
        
        this.$nextTick(() => {
          this.scrollToBottom()
        })
        return
      }
      
      // 如果传入了sessionId参数或props中有sessionId，切换到该会话并加载历史记录
      const targetSessionId = sessionId || this.sessionId
      if (targetSessionId) {
        this.switchSession(targetSessionId)
        await this.loadSessionHistory(targetSessionId)
      } else if (!this.currentSessionId) {
        // 如果没有当前会话，创建新会话
        await this.createSession()
      } else {
        // 如果有当前会话，加载其历史记录
        await this.loadSessionHistory(this.currentSessionId)
      }
      
      if (this.initialQuestion && this.isQuestionMode) {
        await this.analyzeQuestion(this.initialQuestion)
      }
      
      this.$nextTick(() => {
        this.scrollToBottom()
      })
    },

    // 分析题目
    async analyzeQuestion(question) {
      // 添加用户消息到store
      this.addMessage({
        type: 'user',
        content: question
      })
      
      this.isTyping = true
      
      // 创建AbortController用于取消请求
      this.abortController = new AbortController()
      
      try {
        // 确保有会话ID
        await this.ensureSessionId()
        
        // 解析题目内容和选项
        const lines = question.split('\n')
        const questionContent = lines[0].replace('题目：', '')
        const optionsStart = lines.findIndex(line => line.includes('选项：'))
        let options = ''
        
        if (optionsStart !== -1) {
          options = lines.slice(optionsStart + 1).join('\n')
        }
        
        const response = await aiAPI.analyzeQuestion({
          sessionId: this.currentSessionId,
          questionContent: questionContent,
          options: options,
          questionId: 1 // 临时ID
        }, this.abortController.signal)
        
        await this.handleStreamResponse(response)
      } catch (error) {
        if (error.name === 'AbortError') {
          console.log('题目分析已被用户取消')
          this.updateMessage(this.currentMessageId, '分析已暂停')
        } else {
          console.error('题目分析失败:', error)
          this.addMessage({
            type: 'ai',
            content: '抱歉，题目分析失败，请稍后重试。'
          })
        }
      } finally {
        this.isTyping = false
        this.abortController = null
        this.currentReader = null
      }
    },

    // 发送消息
    async sendMessage() {
      if (!this.inputMessage.trim() || this.isTyping) return
      
      const userMessage = this.inputMessage.trim()
      
      // 添加用户消息到store
      this.addMessage({
        type: 'user',
        content: userMessage
      })
      
      this.inputMessage = ''
      this.isTyping = true
      
      // 创建AbortController用于取消请求
      this.abortController = new AbortController()
      
      try {
        // 确保有会话ID
        await this.ensureSessionId()
        
        const response = await aiAPI.sendMessage({
          sessionId: this.currentSessionId,
          message: userMessage,
          questionId: null
        }, this.abortController.signal)
        
        await this.handleStreamResponse(response)
      } catch (error) {
        if (error.name === 'AbortError') {
          console.log('请求已被用户取消')
          this.updateMessage(this.currentMessageId, '对话已暂停')
        } else {
          console.error('发送消息失败:', error)
          this.addMessage({
            type: 'ai',
            content: '抱歉，发送消息失败，请稍后重试。'
          })
        }
      } finally {
        this.isTyping = false
        this.abortController = null
        this.currentReader = null
      }
    },

    // 处理流式响应
    async handleStreamResponse(response) {
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`)
      }
      
      const reader = response.body.getReader()
      this.currentReader = reader // 保存reader引用用于暂停
      const decoder = new TextDecoder('utf-8')
      let aiMessage = ''
      
      // 添加AI消息占位符到store
      this.addMessage({
        type: 'ai',
        content: ''
      })
      
      // 获取刚添加的消息ID
      const currentMessages = this.currentMessages
      const lastMessage = currentMessages[currentMessages.length - 1]
      this.currentMessageId = lastMessage.id
      
        try {
          let buffer = '' // 添加缓冲区处理不完整的数据
          
          // eslint-disable-next-line no-constant-condition
          while (true) {
            const { done, value } = await reader.read()
            if (done) {
              console.log('流式响应读取完成')
              break
            }
            
            if (!value || value.length === 0) {
              continue
            }
            
            const chunk = decoder.decode(value, { stream: true })
            buffer += chunk
            
            // 按行分割，保留最后一个可能不完整的行
            const lines = buffer.split('\n')
            buffer = lines.pop() || '' // 保留最后一行（可能不完整）
            
            for (const line of lines) {
              if (!line.trim()) continue
              
              console.log('接收到数据行:', line) // 添加调试日志
              
              if (line.startsWith('data: ')) {
                let data = line.slice(6).trim() // 移除'data: '
                
                console.log('解析的数据:', data) // 添加调试日志
                
                if (data === '[DONE]') {
                  console.log('流式响应结束')
                  // 流式响应结束，确保最终消息已更新
                  if (aiMessage.trim()) {
                    this.updateMessage(this.currentMessageId, aiMessage)
                  }
                  return
                }
                
                if (data && data !== '') {
                  try {
                    // 尝试解析JSON格式的响应
                    const parsedData = JSON.parse(data)
                    if (parsedData.content) {
                      // 如果是JSON格式，使用content字段
                      data = parsedData.content
                      console.log('解析JSON成功，内容:', JSON.stringify(data))
                    } else if (parsedData.error) {
                      // 处理错误响应
                      console.error('后端返回错误:', parsedData.error)
                      throw new Error(parsedData.error)
                    }
                  } catch (e) {
                    // 如果不是JSON，直接使用文本内容
                    console.log('非JSON格式，直接使用文本:', JSON.stringify(data))
                  }
                  
                  // 添加到AI内容
                  aiMessage += data
                  this.updateMessage(this.currentMessageId, aiMessage)
                  
                  this.$nextTick(() => {
                    this.scrollToBottom()
                  })
                }
              }
            }
          }
          
          // 处理缓冲区中剩余的数据
          if (buffer.trim()) {
            console.log('处理缓冲区剩余数据:', buffer)
            if (buffer.startsWith('data: ')) {
              let data = buffer.slice(6).trim()
              
              if (data && data !== '[DONE]') {
                try {
                  // 尝试解析JSON格式的响应
                  const parsedData = JSON.parse(data)
                  if (parsedData.content) {
                    // 如果是JSON格式，使用content字段
                    data = parsedData.content
                  } else if (parsedData.error) {
                    // 处理错误响应
                    console.error('后端返回错误:', parsedData.error)
                    throw new Error(parsedData.error)
                  }
                } catch (e) {
                  // 如果不是JSON，直接使用文本内容
                  console.log('缓冲区非JSON格式，直接使用文本:', JSON.stringify(data))
                }
                
                aiMessage += data
                this.updateMessage(this.currentMessageId, aiMessage)
              }
            }
          }
      } catch (error) {
        if (error.name === 'AbortError') {
          console.log('流式响应已被用户取消')
        } else {
          console.error('处理流式响应失败:', error)
          this.updateMessage(this.currentMessageId, '响应处理失败，请重试。')
        }
      } finally {
        reader.releaseLock()
      }
    },

    // 新建对话
    async clearChat() {
      try {
        await this.createSession()
        this.$emit('session-cleared')
      } catch (error) {
        console.error('创建新对话失败:', error)
      }
    },

    // 处理回车键
    handleEnter(event) {
      if (!event.shiftKey) {
        this.sendMessage()
      }
    },

    // 渲染Markdown
    renderMarkdown(content) {
      // 检查content是否为有效值
      if (!content || typeof content !== 'string') {
        return ''
      }
      try {
        const html = marked(content)
        return DOMPurify.sanitize(html)
      } catch (error) {
        console.error('Markdown渲染失败:', error)
        return content // 如果渲染失败，返回原始内容
      }
    },

    // 格式化时间
    formatTime(timestamp) {
      const now = new Date()
      const time = new Date(timestamp)
      const diff = now - time
      
      if (diff < 60000) { // 1分钟内
        return '刚刚'
      } else if (diff < 3600000) { // 1小时内
        return `${Math.floor(diff / 60000)}分钟前`
      } else if (diff < 86400000) { // 24小时内
        return `${Math.floor(diff / 3600000)}小时前`
      } else {
        return time.toLocaleDateString()
      }
    },

    // 滚动到底部
    scrollToBottom() {
      const container = this.$refs.messagesContainer
      if (container) {
        container.scrollTop = container.scrollHeight
      }
    },

    // 确保有会话ID
    async ensureSessionId() {
      if (!this.currentSessionId) {
        await this.createSession()
      }
    },

    // 暂停AI生成
    stopGeneration() {
      if (this.abortController) {
        this.abortController.abort()
      }
      if (this.currentReader) {
        this.currentReader.cancel()
      }
      this.isTyping = false
      this.abortController = null
      this.currentReader = null
    }
  }
}
</script>

<style scoped>
.ai-chat-container {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid #ebeef5;
  background: #f8f9fa;
  border-radius: 8px 8px 0 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-left i {
  font-size: 18px;
  color: #409eff;
}

.title {
  font-weight: 600;
  color: #303133;
}

.chat-messages {
  flex: 1;
  padding: 16px;
  overflow-y: auto;
  max-height: 400px;
}

.message {
  display: flex;
  margin-bottom: 16px;
  align-items: flex-start;
}

.message.user {
  flex-direction: row-reverse;
}

.message-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 8px;
  flex-shrink: 0;
}

.user-avatar {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #409eff;
  color: white;
}

.avatar-img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
}

.ai-avatar {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #67c23a;
  color: white;
}

.ai-text {
  font-size: 12px;
  font-weight: bold;
}

.message-content {
  max-width: 70%;
  flex: 1;
}

.message.user .message-content {
  text-align: right;
}

.message-text {
  padding: 12px 16px;
  border-radius: 12px;
  word-wrap: break-word;
  line-height: 1.5;
}

.message.user .message-text {
  background: #409eff;
  color: white;
  border-bottom-right-radius: 4px;
}

.message.ai .message-text {
  background: #f4f4f5;
  color: #303133;
  border-bottom-left-radius: 4px;
  font-size: 13px;
}

.message-time {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.typing-indicator {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 12px 16px;
  background: #f4f4f5;
  border-radius: 12px;
  border-bottom-left-radius: 4px;
}

.typing-indicator span {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #909399;
  animation: typing 1.4s infinite;
}

.typing-indicator span:nth-child(2) {
  animation-delay: 0.2s;
}

.typing-indicator span:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes typing {
  0%, 60%, 100% {
    transform: translateY(0);
    opacity: 0.5;
  }
  30% {
    transform: translateY(-10px);
    opacity: 1;
  }
}

.chat-input {
  display: flex;
  padding: 12px 16px 8px 16px; /* 减少下方padding */
  border-top: 1px solid #ebeef5;
  gap: 12px;
  align-items: flex-end;
}

.chat-input .t-textarea {
  flex: 1;
}

.send-btn {
  height: 40px;
}

/* Markdown样式 */
.message-text :deep(h1),
.message-text :deep(h2),
.message-text :deep(h3),
.message-text :deep(h4),
.message-text :deep(h5),
.message-text :deep(h6) {
  margin: 8px 0 4px 0;
  font-weight: 600;
}

.message-text :deep(p) {
  margin: 4px 0;
}

.message-text :deep(code) {
  background: rgba(0, 0, 0, 0.1);
  padding: 2px 4px;
  border-radius: 3px;
  font-family: 'Courier New', monospace;
}

.message-text :deep(pre) {
  background: rgba(0, 0, 0, 0.05);
  padding: 8px;
  border-radius: 4px;
  overflow-x: auto;
  margin: 8px 0;
}

.message-text :deep(ul),
.message-text :deep(ol) {
  margin: 8px 0;
  padding-left: 20px;
}

.message-text :deep(blockquote) {
  border-left: 4px solid #ddd;
  margin: 8px 0;
  padding-left: 12px;
  color: #666;
}
</style>