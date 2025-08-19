<template>
  <div class="tdesign-ai-chat-container">
    <!-- 聊天头部操作区 -->
    <div class="chat-header">
      <div class="chat-operations">
        <t-button
          v-if="isTyping"
          variant="base"
          theme="warning"
          size="small"
          @click="stopGeneration"
        >
          <t-icon name="stop" />
          停止
        </t-button>
        <t-button
          variant="outline"
          theme="default"
          size="small"
          @click="clearChat"
          :disabled="chatMessages.length === 0"
        >
          <t-icon name="refresh" />
          清空
        </t-button>
      </div>
    </div>

    <!-- 聊天消息区域 -->
    <div class="chat-messages-area" ref="messagesContainer">
      <div
        v-for="(message, index) in chatMessages"
        :key="index"
        :class="['message-item', message.role]"
      >
        <div class="message-avatar">
          <div v-if="message.role === 'user'" class="user-avatar">
            <img v-if="userAvatar" :src="userAvatar" alt="用户头像" class="avatar-img" />
            <t-icon v-else name="user" />
          </div>
          <div v-else class="ai-avatar">
            <span class="ai-text">AI</span>
          </div>
        </div>
        <div class="message-content">
          <div class="message-text">
            <div v-if="message.role === 'user'" v-html="message.content"></div>
            <div v-else v-html="renderMarkdown(message.content)"></div>
          </div>
          <div class="message-time">{{ formatTime(message.timestamp) }}</div>
        </div>
      </div>

      <!-- AI正在输入提示 -->
      <div v-if="isTyping" class="message-item assistant typing">
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
    <div class="chat-input-area">
      <t-textarea
        v-model="inputValue"
        :placeholder="placeholder"
        :disabled="isTyping"
        :autosize="{ minRows: 2, maxRows: 6 }"
        @keydown.enter.prevent="handleEnter"
        class="chat-textarea"
      />
      <div class="input-actions">
        <t-button
          theme="primary"
          :loading="isTyping"
          :disabled="!inputValue.trim()"
          @click="handleSend"
          class="send-button"
        >
          <t-icon name="send" />
          发送
        </t-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, nextTick, onMounted, onUnmounted } from 'vue'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import { MessagePlugin } from 'tdesign-vue-next'
import { aiAPI } from '@/api'
import { useAiChatStore } from '@/store/aiChat'

// Props
const props = defineProps({
  sessionId: {
    type: String,
    default: ''
  },
  modelId: {
    type: String,
    default: ''
  },
  placeholder: {
    type: String,
    default: '请输入您的问题...'
  },
  userAvatar: {
    type: String,
    default: ''
  }
})

// Emits
const emit = defineEmits(['session-cleared', 'message-sent', 'typing-status'])

// Store
const aiChatStore = useAiChatStore()

// Refs
const messagesContainer = ref(null)
const inputValue = ref('')
const isTyping = ref(false)
const abortController = ref(null)

// Computed
const chatMessages = computed(() => {
  const messages = aiChatStore.chatHistory[props.sessionId] || []
  console.log('🔍 TDesignAiChat: 处理消息列表:', messages.length, '条消息')
  
  return messages.map(msg => {
    // 处理不同的消息类型格式
    let role = 'assistant' // 默认为AI消息
    
    // 优先检查role字段（store中已经正确映射）
    if (msg.role) {
      role = msg.role
    } else {
      // 处理messageType字段（后端返回的字段名）
      let messageType = msg.messageType || msg.message_type
      if (typeof messageType === 'string') {
        messageType = parseInt(messageType)
      }
      
      // 根据后端数据库定义：1=用户消息，2=AI消息
      if (msg.type === 'user' || messageType === 1) {
        role = 'user'
      } else if (msg.type === 'assistant' || messageType === 2) {
        role = 'assistant'
      }
    }
    
    console.log('📝 TDesignAiChat消息类型映射:', {
      原始type: msg.type,
      原始role: msg.role,
      原始messageType: msg.messageType,
      原始message_type: msg.message_type,
      最终role: role,
      时间戳: msg.timestamp,
      内容预览: msg.content?.substring(0, 20) + '...'
    })
    
    return {
      id: msg.id,
      role: role,
      content: msg.content,
      timestamp: msg.timestamp,
      status: msg.status || 'success'
    }
  })
})

// 移除不需要的computed属性

// Methods
const handleInputChange = (value) => {
  inputValue.value = value
}

// 修复handleSend方法中的流式请求处理
const handleSend = async () => {
  if (!inputValue.value.trim() || isTyping.value) return
  
  const message = inputValue.value.trim()
  inputValue.value = ''
  
  try {
      isTyping.value = true
      emit('typing-status', true)
      
      // 设置当前会话ID并添加用户消息
      aiChatStore.currentSessionId = props.sessionId
      await aiChatStore.addMessage({
        type: 'user',
        content: message,
        timestamp: new Date().toISOString()
      })
      
      // 创建AbortController用于取消请求
      abortController.value = new AbortController()


      // 调用AI聊天API（流式响应）
      console.log('=== 发送AI聊天请求 ===')
      console.log('消息内容:', message)
      console.log('会话ID:', props.sessionId)
      console.log('模型ID:', props.modelId)
      
      const response = await aiAPI.sendMessage({
        sessionId: props.sessionId,
        message: message,
        modelId: props.modelId
      }, abortController.value.signal)

      console.log('=== AI响应状态 ===')
      console.log('状态码:', response.status)
      console.log('响应头:', Object.fromEntries(response.headers.entries()))
      
      if (!response.ok) {
        console.error('HTTP错误响应:', response.status, response.statusText)
        throw new Error(`HTTP error! status: ${response.status}`)
      }
      
      if (!response.body) {
        console.error('响应体为空，无法读取流式数据')
        throw new Error('响应体为空')
      }
      
      // 创建流式读取器
      const reader = response.body.getReader()
      const decoder = new TextDecoder('utf-8')
      let aiContent = ''
      let chunkCount = 0
      
      // 添加一个空的AI消息用于累积内容
      const aiMessageId = Date.now().toString() + Math.random().toString(36).substr(2, 9)
      await aiChatStore.addMessage({
        type: 'assistant',
        content: '',
        timestamp: new Date().toISOString(),
        id: aiMessageId
      })

      console.log('开始读取流式数据...')
      
      try {
        let buffer = '' // 添加缓冲区处理不完整的数据
        
        while (true) {
          const { done, value } = await reader.read()
          
          if (done) {
            console.log('=== 流式数据读取完成 ===，总共接收到', chunkCount, '个数据块') 
            break
          }
          
          if (!value || value.length === 0) {
            console.log('接收到空数据块，跳过')
            continue
          }
          
          chunkCount++
          const chunk = decoder.decode(value, { stream: true })
          console.log(`接收到第${chunkCount}个数据块:`, JSON.stringify(chunk))
          buffer += chunk
          
          // 按行分割，保留最后一个可能不完整的行
          const lines = buffer.split('\n')
          buffer = lines.pop() || '' // 保留最后一行（可能不完整）
          
          for (let i = 0; i < lines.length; i++) {
            const line = lines[i].trim()
            
            if (!line) continue
            
            console.log('接收到数据行:', JSON.stringify(line)) // 添加调试日志
            
            // 处理重复的data:前缀问题
            let processedLine = line
            if (line.startsWith('data:data: ')) {
              processedLine = line.substring(5) // 去掉第一个"data:"
              console.log('修正重复前缀后的行:', JSON.stringify(processedLine))
            }
            
            // 处理SSE格式数据
            if (processedLine.startsWith('data: ')) {
              let data = processedLine.slice(6).trim()
              
              console.log('解析的数据:', JSON.stringify(data)) // 添加调试日志
              
              if (data === '[DONE]') {
                console.log('=== 收到结束标记 [DONE] ===') 
                break
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
                
                // 添加到AI内容并使用打字机效果
                aiContent += data
                console.log('累积的AI内容长度:', aiContent.length)
                
                // 使用打字机效果逐字显示
                await typewriterEffect(aiMessageId, data)
                
                scrollToBottom()
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
              
              aiContent += data
              aiChatStore.updateMessage(aiMessageId, aiContent)
            }
          }
        }
        
        console.log('📤 TDesignAiChat: 消息发送完成事件:', {
          message: message,
          responseLength: aiContent.length,
          responsePreview: aiContent.substring(0, 50) + '...'
        })
        
        // 发送消息完成事件
        emit('message-sent', { message, response: aiContent })
        
      } catch (error) {
        console.error('读取流式数据失败:', error)
        throw error
      } finally {
        reader.releaseLock()
      }
      
  } catch (error) {
    if (error.name !== 'AbortError') {
      console.error('发送消息失败:', error)
      MessagePlugin.error('发送消息失败，请重试')
    }
  } finally {
    isTyping.value = false
    emit('typing-status', false)
    abortController.value = null
    scrollToBottom()
  }
}

const handleEnter = (event) => {
  if (!event.shiftKey && !event.ctrlKey && !event.altKey) {
    event.preventDefault()
    handleSend()
  }
}

const handleScroll = (event) => {
  // 处理滚动事件，可以用于加载历史消息等
  console.log('Chat scrolled:', event)
}

const stopGeneration = () => {
  if (abortController.value) {
    abortController.value.abort()
    abortController.value = null
  }
  isTyping.value = false
  emit('typing-status', false)
}

const clearChat = async () => {
  try {
    // 设置当前会话ID并清空
    aiChatStore.currentSessionId = props.sessionId
    // 直接清空聊天历史
    if (aiChatStore.chatHistory[props.sessionId]) {
      aiChatStore.chatHistory[props.sessionId] = []
    }
    emit('session-cleared')
    MessagePlugin.success('对话已清空')
  } catch (error) {
    console.error('清空对话失败:', error)
    MessagePlugin.error('清空对话失败')
  }
}

const scrollToBottom = () => {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}

// 修复testApiConnection方法中的Authorization问题
const testApiConnection = async () => {
  console.log('=== 开始测试API连接 ===')
  
  try {
    const token = localStorage.getItem('token')
    if (!token) {
      MessagePlugin.warning('请先登录后再测试连接')
      return
    }

    // 测试配额检查
    console.log('1. 测试配额检查...')
    const quotaResponse = await fetch(`${process.env.VUE_APP_BASE_API || '/api'}/ai-quota/check?aiType=chat`, {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    })
    console.log('配额检查状态:', quotaResponse.status)
    
    if (!quotaResponse.ok) {
      MessagePlugin.error(`配额检查失败: ${quotaResponse.status}`)
      return
    }
    
    // 测试流式响应API
    console.log('2. 测试流式响应API...')
    const streamResponse = await aiAPI.testStream({
      message: '测试连接'
    })
    
    console.log('流式API状态:', streamResponse.status)
    console.log('流式API响应头:', Object.fromEntries(streamResponse.headers.entries()))
    
    if (!streamResponse.ok) {
      MessagePlugin.error(`流式API连接失败: ${streamResponse.status}`)
      return
    }
    
    if (!streamResponse.body) {
      console.error('响应体为空')
      MessagePlugin.error('响应体为空，无法读取流式数据')
      return
    }
    
    // 读取流式响应测试
    const reader = streamResponse.body.getReader()
    const decoder = new TextDecoder('utf-8')
    let testMessage = ''
    let hasData = false
    let chunkCount = 0
    
    try {
      let buffer = ''
      
      while (true) {
        const { done, value } = await reader.read()
        
        if (done) {
          console.log('测试流式响应读取完成，总共接收到', chunkCount, '个数据块')
          break
        }
        
        if (!value || value.length === 0) {
          console.log('接收到空数据块，跳过')
          continue
        }
        
        chunkCount++
        const chunk = decoder.decode(value, { stream: true })
        console.log(`接收到第${chunkCount}个数据块:`, JSON.stringify(chunk))
        buffer += chunk
        
        const lines = buffer.split('\n')
        buffer = lines.pop() || ''
        
        for (const line of lines) {
          if (!line.trim()) continue
          
          console.log('测试接收到数据行:', JSON.stringify(line))
          
          if (line.startsWith('data: ')) {
            let data = line.slice(6).trim()
            
            console.log('解析的数据内容:', JSON.stringify(data))
            
            if (data === '[DONE]') {
              console.log('测试流式响应结束')
              hasData = true
              break
            }
            
            if (data && data !== '') {
              testMessage += data
              hasData = true
              console.log('累积的测试消息:', testMessage)
            }
          }
        }
        
        if (hasData && buffer.includes('[DONE]')) {
          break
        }
      }
      
      // 处理缓冲区中剩余的数据
      if (buffer.trim()) {
        console.log('处理缓冲区剩余数据:', JSON.stringify(buffer))
        if (buffer.startsWith('data: ')) {
          let data = buffer.slice(6).trim()
          if (data && data !== '[DONE]') {
            testMessage += data
            hasData = true
          }
        }
      }
      
      reader.releaseLock()
      
      if (hasData) {
        console.log('✅ API连接测试成功，收到响应:', testMessage)
        MessagePlugin.success('API连接测试成功！流式响应正常工作')
      } else {
        console.log('⚠️ API连接成功，但未收到有效响应')
        MessagePlugin.warning('API连接成功，但未收到有效响应')
      }
      
    } catch (streamError) {
      console.error('流式响应读取失败:', streamError)
      MessagePlugin.error(`流式响应读取失败: ${streamError.message}`)
    }
    
  } catch (error) {
    console.error('❌ API测试失败:', error)
    MessagePlugin.error(`API测试失败: ${error.message}`)
  }
}

const renderMarkdown = (content) => {
  if (!content) return ''
  try {
    const html = marked(content)
    return DOMPurify.sanitize(html)
  } catch (error) {
    console.error('Markdown渲染失败:', error)
    return content
  }
}

const formatTime = (timestamp) => {
  if (!timestamp) return ''
  
  console.log('🕐 TDesignAiChat格式化时间:', {
    原始timestamp: timestamp,
    timestamp类型: typeof timestamp
  })
  
  let date
  try {
    // 处理不同的时间格式
    if (typeof timestamp === 'string') {
      // 处理MySQL datetime格式 "2025-08-08 12:05:09"
      if (timestamp.includes(' ') && timestamp.includes('-')) {
        // MySQL datetime格式，假设是本地时间（中国时区）
        // 直接解析，不添加Z后缀，让浏览器按本地时区处理
        const isoString = timestamp.replace(' ', 'T')
        date = new Date(isoString)
        console.log('🕐 MySQL时间格式转换（本地时区）:', {
          原始: timestamp,
          转换后: isoString,
          解析结果: date.toISOString(),
          本地时间: date.toLocaleString('zh-CN')
        })
      } else {
        // 其他字符串格式
        date = new Date(timestamp)
      }
    } else if (typeof timestamp === 'number') {
      // 如果是数字，可能是时间戳
      date = new Date(timestamp)
    } else {
      // 如果已经是Date对象
      date = new Date(timestamp)
    }
    
    // 检查日期是否有效
    if (isNaN(date.getTime())) {
      console.error('无效的时间格式:', timestamp)
      return '时间未知'
    }
    
    const now = new Date()
    const diff = now - date
    
    console.log('🕐 TDesignAiChat时间计算:', {
      解析后的date: date.toISOString(),
      当前时间: now.toISOString(),
      时间差毫秒: diff,
      时间差分钟: Math.floor(diff / 60000),
      时间差小时: Math.floor(diff / 3600000)
    })
    
    // 如果时间差为负数，说明是未来时间，可能是时区问题
    if (diff < 0) {
      console.warn('检测到未来时间，可能存在时区问题:', {
        消息时间: date.toISOString(),
        当前时间: now.toISOString(),
        时间差: diff
      })
      return '刚刚' // 对于未来时间，显示为刚刚
    }
    
    if (diff < 60000) { // 1分钟内
      return '刚刚'
    } else if (diff < 3600000) { // 1小时内
      const minutes = Math.floor(diff / 60000)
      return `${minutes}分钟前`
    } else if (diff < 86400000) { // 1天内
      const hours = Math.floor(diff / 3600000)
      return `${hours}小时前`
    } else if (diff < 86400000 * 7) { // 1周内
      const days = Math.floor(diff / 86400000)
      return `${days}天前`
    } else {
      // 超过1周，显示具体日期
      return date.toLocaleDateString('zh-CN', {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      })
    }
  } catch (error) {
    console.error('TDesignAiChat时间格式化失败:', error, timestamp)
    return '时间解析失败'
  }
}

// 打字机效果实现
const typewriterEffect = async (messageId, newText) => {
  return new Promise((resolve) => {
    const currentMessage = aiChatStore.chatHistory[props.sessionId]?.find(msg => msg.id === messageId)
    if (!currentMessage) {
      resolve()
      return
    }
    
    let currentContent = currentMessage.content || ''
    const targetContent = currentContent + newText
    let index = currentContent.length
    
    const typeInterval = setInterval(() => {
      if (index >= targetContent.length) {
        clearInterval(typeInterval)
        resolve()
        return
      }
      
      // 每次添加1-3个字符，模拟真实打字效果
      const charsToAdd = Math.min(Math.floor(Math.random() * 3) + 1, targetContent.length - index)
      const nextContent = targetContent.substring(0, index + charsToAdd)
      
      aiChatStore.updateMessage(messageId, nextContent)
      index += charsToAdd
      
      // 滚动到底部
      scrollToBottom()
    }, 50) // 每50ms更新一次，创造打字效果
  })
}

// AI解析题目方法
// 修复analyzeQuestion方法中的流式请求处理
const analyzeQuestion = async (questionContent) => {
  if (!questionContent.trim() || isTyping.value) return

  try {
    isTyping.value = true
    emit('typing-status', true)
    
    // 设置当前会话ID并添加用户消息
    aiChatStore.currentSessionId = props.sessionId
    await aiChatStore.addMessage({
      type: 'user',
      content: questionContent,
      timestamp: new Date().toISOString()
    })
    
    // 创建AbortController用于取消请求
    abortController.value = new AbortController()


    console.log('=== 开始分析题目 ===')
    console.log('题目内容:', questionContent)
    console.log('会话ID:', props.sessionId)
    console.log('模型ID:', props.modelId)

    const response = await aiAPI.analyzeQuestion({
      sessionId: props.sessionId,
      question: questionContent,
      modelId: props.modelId
    }, abortController.value.signal)

    console.log('=== AI分析响应状态 ===')
    console.log('状态码:', response.status)
    
    if (!response.ok) {
      console.error('HTTP错误响应:', response.status, response.statusText)
      throw new Error(`HTTP error! status: ${response.status}`)
    }
    
    if (!response.body) {
      console.error('响应体为空，无法读取流式数据')
      throw new Error('响应体为空')
    }
    
    // 创建流式读取器
    const reader = response.body.getReader()
    const decoder = new TextDecoder('utf-8')
    let aiContent = ''
    
    // 添加一个空的AI消息用于累积内容
    const aiMessageId = Date.now().toString() + Math.random().toString(36).substr(2, 9)
    await aiChatStore.addMessage({
      type: 'assistant',
      content: '',
      timestamp: new Date().toISOString(),
      id: aiMessageId
    })

    console.log('开始读取分析结果...')
    
      try {
        let buffer = '' // 添加缓冲区处理不完整的数据
        
        while (true) {
          const { done, value } = await reader.read()
          
          if (done) {
            console.log('=== 流式数据读取完成 ===') 
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
          
          for (let i = 0; i < lines.length; i++) {
            const line = lines[i].trim()
            
            if (!line) continue
            
            console.log('接收到数据行:', line) // 添加调试日志
            
            // 处理重复的data:前缀问题
            let processedLine = line
            if (line.startsWith('data:data: ')) {
              processedLine = line.substring(5) // 去掉第一个"data:"
              console.log('修正重复前缀后的行:', processedLine)
            }
            
            // 处理SSE格式数据
            if (processedLine.startsWith('data: ')) {
              let data = processedLine.slice(6).trim()
              
              console.log('解析的数据:', data) // 添加调试日志
              
              if (data === '[DONE]') {
                console.log('=== 收到结束标记 [DONE] ===') 
                break
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
                
                // 添加到AI内容并使用打字机效果
                aiContent += data
                
                // 使用打字机效果逐字显示
                await typewriterEffect(aiMessageId, data)
                
                scrollToBottom()
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
              
              aiContent += data
              aiChatStore.updateMessage(aiMessageId, aiContent)
            }
          }
        }
      
      console.log('📤 TDesignAiChat: 题目分析完成事件:', {
        question: questionContent,
        responseLength: aiContent.length,
        responsePreview: aiContent.substring(0, 50) + '...'
      })
      
      // 发送消息完成事件
      emit('message-sent', { message: questionContent, response: aiContent })
      
    } catch (error) {
      console.error('读取流式数据失败:', error)
      throw error
    } finally {
      reader.releaseLock()
    }
    
  } catch (error) {
    if (error.name !== 'AbortError') {
      console.error('分析题目失败:', error)
      MessagePlugin.error('分析题目失败，请重试')
    }
  } finally {
    isTyping.value = false
    emit('typing-status', false)
    abortController.value = null
    scrollToBottom()
  }
}

// 暴露方法给父组件
defineExpose({
  isTyping: computed(() => isTyping.value),
  stopGeneration,
  clearChat,
  scrollToBottom,
  analyzeQuestion
})

// 监听消息变化，自动滚动到底部
watch(chatMessages, () => {
  scrollToBottom()
}, { deep: true })

// 监听sessionId变化，确保正确显示历史记录
watch(() => props.sessionId, async (newSessionId, oldSessionId) => {
  if (newSessionId && newSessionId !== oldSessionId) {
    console.log('🔄 TDesignAiChat: 会话ID变化:', oldSessionId, '->', newSessionId)

    // 确保新会话的历史记录已加载
    if (!aiChatStore.chatHistory[newSessionId] || aiChatStore.chatHistory[newSessionId].length === 0) {
      console.log('📥 TDesignAiChat: 加载新会话历史记录')
      await aiChatStore.loadSessionMessages(newSessionId)
    }

    // 滚动到底部
    scrollToBottom()
  }
}, { immediate: true })

// 组件卸载时清理
onUnmounted(() => {
  if (abortController.value) {
    abortController.value.abort()
  }
})
</script>

<style scoped>
.tdesign-ai-chat-container {
  height: 100%;
  display: flex;
  flex-direction: column;
  border: 1px solid #e7e7e7;
  border-radius: 8px;
  overflow: hidden;
}

.chat-header {
  padding: 12px 16px;
  border-bottom: 1px solid #e7e7e7;
  background: #fafafa;
}

.chat-operations {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}

.chat-messages-area {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  background: #fff;
}

.message-item {
    display: flex;
    gap: 12px;
    margin-bottom: 16px;
    align-items: flex-start;
  }

  .message-item.user {
    flex-direction: row-reverse;
    justify-content: flex-start;
  }

  .message-item.assistant {
    flex-direction: row;
    justify-content: flex-start;
  }



.message-avatar {
  flex-shrink: 0;
}

.user-avatar,
.ai-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 500;
}

.user-avatar {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.ai-avatar {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  color: white;
}

.ai-text {
  font-size: 12px;
  font-weight: bold;
  color: white;
}

.avatar-img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
}

.message-content {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
  }

  .message-item.user .message-content {
    align-items: flex-end;
  }

  .message-item.assistant .message-content {
    align-items: flex-start;
  }

.message-text {
    padding: 12px 16px;
    border-radius: 12px;
    line-height: 1.6;
    word-wrap: break-word;
    max-width: 70%;
    min-width: 40px;
  }

  .message-item.user .message-text {
    background: #0052d9;
    color: white;
  }

  .message-item.assistant .message-text {
    background: #f3f3f3;
    color: #333;
  }

.message-time {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

.message-item.user .message-time {
  text-align: right;
}

.message-item.assistant .message-time {
  text-align: left;
}

.chat-input-area {
  display: flex;
  gap: 12px;
  align-items: flex-end;
  padding: 16px;
  border-top: 1px solid #e7e7e7;
  background: #fafafa;
}

.chat-textarea {
  flex: 1;
}

.input-actions {
  display: flex;
  gap: 8px;
}

.send-button {
  height: 40px;
  min-width: 80px;
}

.typing-indicator {
  display: flex;
  gap: 4px;
  padding: 12px 16px;
  background: #f3f3f3;
  border-radius: 12px;
}

.typing-indicator span {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #0052d9;
  animation: typing 1.4s infinite ease-in-out;
}

.typing-indicator span:nth-child(1) { animation-delay: -0.32s; }
.typing-indicator span:nth-child(2) { animation-delay: -0.16s; }

@keyframes typing {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1); }
}

/* 移动端响应式样式 */
@media (max-width: 768px) {
  .tdesign-ai-chat-container {
    height: 75vh;
    max-height: 600px;
    min-height: 450px;
    display: flex;
    flex-direction: column;
  }
  
  .chat-header {
    flex-shrink: 0;
    padding: 10px 12px;
    border-bottom: 1px solid #e7e7e7;
  }
  
  .chat-messages-area {
    flex: 1;
    padding: 12px;
    overflow-y: auto;
    min-height: 250px;
  }
  
  .message-text {
    max-width: 85%;
    font-size: 14px;
    padding: 10px 14px;
  }
  
  .chat-input-area {
    flex-shrink: 0;
    padding: 16px 12px;
    gap: 8px;
    background: #fff;
    border-top: 1px solid #e7e7e7;
  }
  
  .chat-textarea {
    margin-bottom: 8px;
  }
  
  .input-actions {
    display: flex;
    justify-content: flex-end;
  }
  
  .send-button {
    min-width: 70px;
    height: 40px;
    font-size: 14px;
  }
  
  .chat-operations {
    gap: 6px;
    flex-wrap: wrap;
  }
  
  .chat-operations .t-button {
    padding: 6px 10px;
    font-size: 12px;
    min-height: 32px;
  }
}

/* 平板端响应式样式 */
@media (min-width: 769px) and (max-width: 1024px) {
  .tdesign-ai-chat-container:not(.collapsed) {
    height: 50vh;
    max-height: 500px;
  }
  
  .message-text {
    max-width: 75%;
  }
}

/* 动画效果 */
.chat-header,
.chat-messages-area,
.chat-input-area {
  transition: all 0.3s ease;
}

.tdesign-ai-chat-container.collapsed .chat-header,
.tdesign-ai-chat-container.collapsed .chat-messages-area,
.tdesign-ai-chat-container.collapsed .chat-input-area {
  opacity: 0;
  transform: translateY(-10px);
}
</style>
