<template>
  <div class="ai-grading-chat-container" id="ai-grading-chat-container" style="display: block !important;">
    <t-card class="ai-grading-card">
      <template #header>
        <div class="ai-grading-header">
          <div class="header-left">
            <t-icon name="robot" class="ai-icon" />
            <span class="ai-title">AI智能判题</span>
            <t-tag v-if="hasHistoryRecord" theme="primary" size="small">有历史记录</t-tag>
          </div>
          <div class="header-actions">
            <t-button 
              theme="primary" 
              size="small" 
              @click="handleRegrade"
              :loading="props.isGrading"
              :disabled="props.isGrading"
            >
              <t-icon :name="hasAnyGradingData ? 'refresh' : 'play-circle'" />
              {{ hasAnyGradingData ? '重新判题' : '开始判题' }}
            </t-button>
            
          </div>
        </div>
      </template>

      <div class="ai-grading-content">
        <!-- 禁用状态提示 -->
        <div v-if="disabled" class="disabled-notice">
          <t-alert theme="info" :close="false">
            <template #icon>
              <t-icon name="info-circle" />
            </template>
            AI智能判题功能仅支持简答题类型，当前题目类型不支持AI判题。
          </t-alert>
        </div>
        
        <!-- AI判断结果标识 -->
        <div v-if="gradingResult && !disabled" class="ai-result-badge">
          <t-tag 
            :theme="gradingResult.isCorrect ? 'success' : 'warning'"
            size="large"
            variant="light"
          >
            <t-icon :name="gradingResult.isCorrect ? 'check-circle' : 'error-circle'" />
            {{ gradingResult.isCorrect ? '答案正确' : '答案需要改进' }}
          </t-tag>
          <div v-if="gradingResult.score !== undefined" class="score-display">
            <span class="score-label">得分:</span>
            <span class="score-value">{{ gradingResult.score }}</span>
          </div>
        </div>

        <!-- 消息堆栈显示区域 -->
        <div class="grading-content-area" ref="contentRef">
          
          <!-- 当前流式消息显示区域（最新，显示在顶部） -->
          <div v-if="currentStreamingMessage && (displayedText || props.isGrading)" class="grading-message current-streaming">
            <div class="message-avatar">
              <div class="ai-avatar current">
                <t-icon name="robot" />
              </div>
            </div>
            <div class="message-content">
              <div v-if="props.isGrading" class="typing-indicator">
                <span></span>
                <span></span>
                <span></span>
              </div>
              <div v-else class="message-text" v-html="renderMarkdown(displayedText)"></div>
              <div class="message-time">
                {{ formatTime(currentStreamingMessage.timestamp) }}
                <span class="current-badge">当前</span>
              </div>
            </div>
          </div>

          <!-- 历史消息堆栈（最多显示2条，按时间倒序） -->
          <div v-for="(message, index) in messageStack.slice().reverse()" :key="message.id" 
               :class="['grading-message', 'history-message', `history-${index}`]">
            <div class="message-avatar">
              <div class="ai-avatar history">
                AI
              </div>
            </div>
            <div class="message-content">
              <div class="message-text history-text" v-html="renderMarkdown(message.content)"></div>
              <div class="message-time">
                {{ formatTime(message.timestamp) }}
                <t-tag :theme="message.isCorrect ? 'success' : 'warning'" size="small">
                  {{ message.isCorrect ? '正确' : '错误' }}
                </t-tag>
              </div>
            </div>
          </div>

          <!-- 选中的历史记录显示区域 -->
          <div v-if="selectedHistoryRecord" class="grading-message selected-history">
            <div class="message-avatar">
              <div class="ai-avatar selected">
                AI
              </div>
            </div>
            <div class="message-content">
              <div class="message-text selected-text" v-html="renderMarkdown(selectedHistoryRecord.aiResult || selectedHistoryRecord.gradingResult)"></div>
              <div class="message-time">
                {{ formatTime(selectedHistoryRecord.createTime) }}
                <span class="selected-badge">已选择</span>
              </div>
            </div>
          </div>

          <!-- 无内容提示 -->
          <div v-if="!currentStreamingMessage && messageStack.length === 0 && !selectedHistoryRecord && !props.isGrading" class="no-content">
            <div class="message-avatar">
              <div class="ai-avatar">
                <t-icon name="robot" />
              </div>
            </div>
            <div class="message-content">
              <div class="message-text">暂无AI判题结果，点击"重新判题"开始分析</div>
            </div>
          </div>
        </div>

        <!-- 历史记录展示 -->
        <div v-if="hasHistoryRecord && showHistory" class="history-section">
          <t-divider>历史判题记录</t-divider>
          <div class="history-list">
            <div 
              v-for="(record, index) in historyRecords" 
              :key="index"
              class="history-item"
              @click="selectHistoryRecord(record)"
              :class="{'history-item-active': selectedHistoryRecord && selectedHistoryRecord.id === record.id}"
            >
              <div class="history-header">
                <span class="history-time">{{ formatTime(record.createTime) }}</span>
                <t-tag 
                  :theme="record.isCorrect ? 'success' : 'warning'"
                  size="small"
                >
                  {{ record.isCorrect ? '正确' : '错误' }}
                </t-tag>
              </div>
              <div class="history-content" v-html="renderMarkdown(record.gradingResult || record.aiResult)"></div>
            </div>
          </div>
        </div>

        <!-- 历史记录切换按钮 -->
        <div v-if="hasHistoryRecord" class="history-toggle">
          <t-button 
            theme="default" 
            variant="text" 
            size="small"
            @click="toggleHistory"
          >
            <t-icon :name="showHistory ? 'chevron-up' : 'chevron-down'" />
            {{ showHistory ? '隐藏' : '查看' }}历史记录
          </t-button>
        </div>
      </div>
    </t-card>
  </div>
</template>

<script setup>
import { ref, computed, watch, onUnmounted, nextTick } from 'vue'
import { marked } from 'marked'
import DOMPurify from 'dompurify'

// Props
const props = defineProps({
  gradingResult: {
    type: Object,
    default: null
  },
  gradingText: {
    type: String,
    default: ''
  },
  isGrading: {
    type: Boolean,
    default: false
  },
  hasHistoryRecord: {
    type: Boolean,
    default: false
  },
  historyRecords: {
    type: Array,
    default: () => []
  },
  disabled: {
    type: Boolean,
    default: false
  }
})

// Emits
const emit = defineEmits(['regrade', 'close'])

// Refs
const contentRef = ref(null)
const showHistory = ref(false)
const displayedText = ref('')
const typewriterTimer = ref(null)
const selectedHistoryRecord = ref(null)
const messageStack = ref([]) // 消息堆栈，最多显示2条历史记录
const currentStreamingMessage = ref(null) // 当前流式消息
const isNewGradingStarted = ref(false) // 是否开始新的判题

// Computed
const chatMessages = computed(() => {
  if (!props.gradingText) return []
  
  return [{
    id: 'grading-result',
    role: 'assistant',
    content: props.gradingText,
    timestamp: new Date().toISOString()
  }]
})

// 判断是否有任何判题数据（用于动态按钮文字）
const hasAnyGradingData = computed(() => {
  // 1. 检查当前是否有流式消息内容
  if (currentStreamingMessage.value && currentStreamingMessage.value.content) {
    console.log('🔍 hasAnyGradingData: 检测到当前流式消息内容')
    return true
  }
  
  // 2. 检查消息堆栈是否有历史消息
  if (messageStack.value.length > 0) {
    console.log('🔍 hasAnyGradingData: 检测到消息堆栈中有历史消息')
    return true
  }
  
  // 3. 检查是否有选中的历史记录
  if (selectedHistoryRecord.value) {
    console.log('🔍 hasAnyGradingData: 检测到选中的历史记录')
    return true
  }
  
  // 4. 检查props中是否有判题文本
  if (props.gradingText && props.gradingText.trim()) {
    console.log('🔍 hasAnyGradingData: 检测到props中有判题文本')
    return true
  }
  
  // 5. 检查是否有显示的文本
  if (displayedText.value && displayedText.value.trim()) {
    console.log('🔍 hasAnyGradingData: 检测到显示文本')
    return true
  }
  
  // 6. 检查是否有历史记录数据
  if (props.hasHistoryRecord && props.historyRecords && props.historyRecords.length > 0) {
    console.log('🔍 hasAnyGradingData: 检测到历史记录数据')
    return true
  }
  
  console.log('🔍 hasAnyGradingData: 没有检测到任何判题数据')
  return false
})

// 消息堆栈管理
const addToMessageStack = (message) => {
  console.log('📝 添加消息到堆栈:', message)
  
  // 如果堆栈已满（2条），移除最旧的消息
  if (messageStack.value.length >= 2) {
    messageStack.value.shift() // 移除第一个（最旧的）
    console.log('🗑️ 移除最旧的消息，当前堆栈长度:', messageStack.value.length)
  }
  
  // 添加新消息到堆栈末尾
  messageStack.value.push({
    id: Date.now() + Math.random(),
    content: message.content,
    timestamp: message.timestamp || new Date().toISOString(),
    isCorrect: message.isCorrect,
    type: 'history'
  })
  
  console.log('✅ 消息已添加到堆栈，当前堆栈长度:', messageStack.value.length)
}

// 开始新的判题流程
const startNewGrading = () => {
  console.log('🔄 开始新的判题流程')
  
  // 如果当前有流式消息，将其添加到历史堆栈
  if (currentStreamingMessage.value && currentStreamingMessage.value.content) {
    console.log('📚 将当前流式消息添加到历史堆栈，isCorrect状态:', currentStreamingMessage.value.isCorrect)
    
    // 确保从props.gradingResult中获取最新的正确性状态
    let isCorrect = currentStreamingMessage.value.isCorrect
    if (props.gradingResult && props.gradingResult.isCorrect !== undefined) {
      isCorrect = props.gradingResult.isCorrect
      console.log('📊 从props.gradingResult更新isCorrect状态:', isCorrect)
    }
    
    addToMessageStack({
      content: currentStreamingMessage.value.content,
      timestamp: currentStreamingMessage.value.timestamp,
      isCorrect: isCorrect
    })
  }
  
  // 重置当前流式消息
  currentStreamingMessage.value = {
    id: Date.now() + Math.random(),
    content: '',
    timestamp: new Date().toISOString(),
    isCorrect: null,
    type: 'streaming'
  }
  
  displayedText.value = ''
  isNewGradingStarted.value = true
  
  console.log('✅ 新判题流程已开始，重置显示文本')
}

// 打字机效果实现
const typewriterEffect = async (newText) => {
  return new Promise((resolve) => {
    if (!newText) {
      resolve()
      return
    }
    
    const currentLength = displayedText.value.length
    const targetText = displayedText.value + newText
    let index = currentLength
    
    const typeInterval = setInterval(() => {
      if (index >= targetText.length) {
        clearInterval(typeInterval)
        resolve()
        return
      }
      
      // 每次添加1-3个字符，模拟真实打字效果
      const charsToAdd = Math.min(Math.floor(Math.random() * 3) + 1, targetText.length - index)
      const newContent = targetText.substring(0, index + charsToAdd)
      displayedText.value = newContent
      
      // 同时更新当前流式消息
      if (currentStreamingMessage.value) {
        currentStreamingMessage.value.content = newContent
      }
      
      index += charsToAdd
      
      // 滚动到底部
      nextTick(() => {
        if (contentRef.value) {
          contentRef.value.scrollTop = contentRef.value.scrollHeight
        }
      })
    }, 50) // 每50ms更新一次，创造打字效果
  })
}

// Methods
const handleRegrade = () => {
  console.log('🔄 AiGradingChat: 点击重新判题按钮')
  
  // 开始新的判题流程
  startNewGrading()
  
  emit('regrade')
  console.log('✅ AiGradingChat: 已发送regrade事件')
}

const handleToggle = () => {
  console.log('🔍 AiGradingChat: 点击收起按钮')
  emit('close')
  console.log('✅ AiGradingChat: 已发送close事件')
}

const handleClose = () => {
  emit('close')
}

const toggleHistory = () => {
  showHistory.value = !showHistory.value
  console.log('🔍 AiGradingChat: 切换历史记录显示状态:', showHistory.value)
  console.log('🔍 历史记录数量:', props.historyRecords?.length)
}

// 选择历史记录
const selectHistoryRecord = (record) => {
  console.log('🔍 AiGradingChat: 选择历史记录:', record)
  
  // 如果点击的是当前选中的记录，则取消选择
  if (selectedHistoryRecord.value && selectedHistoryRecord.value.id === record.id) {
    selectedHistoryRecord.value = null
    console.log('🔍 取消选择历史记录')
    return
  }
  
  // 设置选中的历史记录
  selectedHistoryRecord.value = record
  console.log('🔍 已选择历史记录:', selectedHistoryRecord.value)
  
  // 滚动到内容区域顶部
  nextTick(() => {
    if (contentRef.value) {
      contentRef.value.scrollTop = 0
      console.log('📜 已滚动到内容区域顶部')
    }
  })
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
  const date = new Date(timestamp)
  const now = new Date()
  const diff = now - date
  
  if (diff < 60000) {
    return '刚刚'
  } else if (diff < 3600000) {
    return `${Math.floor(diff / 60000)}分钟前`
  } else if (diff < 86400000) {
    return `${Math.floor(diff / 3600000)}小时前`
  } else {
    return date.toLocaleDateString()
  }
}

// 获取显示文本的方法（用于备用显示）
const getDisplayText = () => {
  console.log('🔍 getDisplayText 调用:', {
    gradingText: props.gradingText,
    gradingTextLength: props.gradingText?.length,
    gradingResult: props.gradingResult,
    gradingResultText: props.gradingResult?.gradingText,
    displayedText: displayedText.value,
    displayedTextLength: displayedText.value?.length
  })
  
  // 如果已经有displayedText，优先使用
  if (displayedText.value) {
    return displayedText.value
  }
  
  // 1. 从gradingText获取（流式数据）
  if (props.gradingText && props.gradingText.length > 0) {
    console.log('🎯 从gradingText获取:', props.gradingText.substring(0, 100))
    return props.gradingText
  }
  
  // 2. 从gradingResult获取（非流式判题结果）
  if (props.gradingResult && props.gradingResult.gradingText) {
    console.log('🎯 从gradingResult获取:', props.gradingResult.gradingText.substring(0, 100))
    return props.gradingResult.gradingText
  }
  
  return ''
}

// 从Props对象中提取判题文本
const extractGradingTextFromProps = () => {
  try {
    console.log('🔍 extractGradingTextFromProps 开始提取')
    
    // 1. 首先尝试从historyRecords中提取
    if (props.historyRecords && props.historyRecords.length > 0) {
      const latestRecord = props.historyRecords[props.historyRecords.length - 1]
      if (latestRecord && latestRecord.aiResult) {
        console.log('🎯 从historyRecords直接提取:', latestRecord.aiResult.substring(0, 100))
        return latestRecord.aiResult
      }
    }
    
    // 2. 从JSON字符串中解析提取
    const propsStr = JSON.stringify(props)
    console.log('🔍 props字符串长度:', propsStr.length)
    
    // 查找aiResult字段
    const aiResultMatch = propsStr.match(/"aiResult":"([^"]*)"/)
    if (aiResultMatch && aiResultMatch[1]) {
      const text = aiResultMatch[1].replace(/\\n/g, '\n').replace(/\\"/g, '"')
      console.log('🎯 从JSON中提取aiResult:', text.substring(0, 100))
      return text
    }
    
    // 查找包含评分内容的字段
    if (propsStr.includes('评分：') || propsStr.includes('详细评价：')) {
      const match = propsStr.match(/"gradingText":"([^"]*评分[^"]*)"/)
      if (match && match[1]) {
        const text = match[1].replace(/\\n/g, '\n').replace(/\\"/g, '"')
        console.log('🎯 从JSON中提取gradingText:', text.substring(0, 100))
        return text
      }
    }
    
    return ''
  } catch (error) {
    console.error('提取文本失败:', error)
    return ''
  }
}

// 获取最新的AI判题结果
const getLatestAiResult = () => {
  if (props.historyRecords && props.historyRecords.length > 0) {
    const latestRecord = props.historyRecords[props.historyRecords.length - 1]
    return latestRecord?.aiResult || ''
  }
  return ''
}

// 监听判题文本变化，使用打字机效果显示
watch(() => props.gradingText, async (newText, oldText) => {
  console.log('🔍 AiGradingChat: gradingText变化详细调试:', {
    旧文本: oldText,
    旧文本长度: oldText?.length || 0,
    新文本: newText,
    新文本长度: newText?.length || 0,
    新文本类型: typeof newText,
    新文本预览: newText?.substring(0, 100) + '...',
    当前displayedText: displayedText.value,
    当前displayedText长度: displayedText.value?.length || 0,
    是否有新内容: newText !== oldText,
    当前流式消息: currentStreamingMessage.value?.content?.length || 0
  })
  
  // 如果是新的判题开始（从空到有内容）
  if (!oldText && newText) {
    console.log('🔄 AiGradingChat: 检测到新判题开始')
    if (!isNewGradingStarted.value) {
      startNewGrading()
    }
  }
  
  // 如果有新增内容，使用打字机效果显示
  if (newText && newText.length > displayedText.value.length) {
    const newContent = newText.substring(displayedText.value.length)
    console.log('📝 AiGradingChat: 使用打字机效果显示新内容:', newContent.substring(0, 50) + '...')
    await typewriterEffect(newContent)
  } else if (newText !== displayedText.value) {
    // 如果文本完全不同，直接设置（比如切换历史记录）
    console.log('🔄 AiGradingChat: 直接设置文本内容')
    displayedText.value = newText || ''
    if (currentStreamingMessage.value) {
      currentStreamingMessage.value.content = newText || ''
    }
  }
  
  // 重置新判题标志
  if (newText && isNewGradingStarted.value) {
    isNewGradingStarted.value = false
  }
  
  // 确保DOM更新后滚动到底部
  nextTick(() => {
    if (contentRef.value) {
      contentRef.value.scrollTop = contentRef.value.scrollHeight
      console.log('📜 AiGradingChat: 已滚动到底部，scrollHeight:', contentRef.value.scrollHeight)
    }
  })
}, { immediate: true, flush: 'post' })

// 监听判题结果变化，更新当前流式消息的正确性
watch(() => props.gradingResult, (newResult, oldResult) => {
  if (newResult && currentStreamingMessage.value) {
    console.log('📊 AiGradingChat: 更新当前流式消息的判题结果:', {
      旧结果: oldResult?.isCorrect,
      新结果: newResult.isCorrect,
      当前消息ID: currentStreamingMessage.value.id,
      当前消息内容长度: currentStreamingMessage.value.content?.length || 0
    })
    currentStreamingMessage.value.isCorrect = newResult.isCorrect
    
    // 如果消息堆栈中有消息，也需要更新最新的消息状态（防止状态不同步）
    if (messageStack.value.length > 0) {
      const latestMessage = messageStack.value[messageStack.value.length - 1]
      if (latestMessage && latestMessage.isCorrect !== newResult.isCorrect) {
        console.log('📊 同时更新消息堆栈中最新消息的状态:', {
          消息ID: latestMessage.id,
          旧状态: latestMessage.isCorrect,
          新状态: newResult.isCorrect
        })
        latestMessage.isCorrect = newResult.isCorrect
      }
    }
  }
}, { immediate: true })

// 额外添加一个监听器来监听整个props对象的变化
watch(() => props, (newProps) => {
  console.log('🔍 AiGradingChat: 整个props对象变化:', {
    props: newProps,
    gradingText: newProps.gradingText,
    gradingText长度: newProps.gradingText?.length || 0,
    isGrading: newProps.isGrading,
    hasHistoryRecord: newProps.hasHistoryRecord
  })
}, { deep: true, immediate: true })

// 组件卸载时清理定时器
onUnmounted(() => {
  if (typewriterTimer.value) {
    clearInterval(typewriterTimer.value)
  }
})
</script>

<style scoped>
.ai-grading-chat-container {
  width: 100%;
}

.ai-grading-card {
  border: 2px solid #0052d9;
  box-shadow: 0 4px 12px rgba(0, 82, 217, 0.15);
}

.ai-grading-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.ai-icon {
  color: #0052d9;
  font-size: 18px;
}

.ai-title {
  font-weight: 600;
  color: #0052d9;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.ai-grading-content {
  padding: 16px 0;
}

.disabled-notice {
  margin-bottom: 16px;
}

.ai-result-badge {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  padding: 12px;
  background: #f8f9fa;
  border-radius: 8px;
}

.score-display {
  display: flex;
  align-items: center;
  gap: 4px;
}

.score-label {
  color: #666;
  font-size: 14px;
}

.score-value {
  font-weight: 600;
  color: #0052d9;
  font-size: 16px;
}

.grading-content-area {
  min-height: 250px;
  max-height: 500px;
  overflow-y: auto;
  padding: 16px;
  background: #fff;
}

.grading-message {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  transition: all 0.3s ease;
}

.grading-message.current-streaming {
  border-left: 3px solid #0052d9;
  padding-left: 12px;
  margin-left: -12px;
  background: linear-gradient(90deg, rgba(0, 82, 217, 0.05) 0%, transparent 100%);
  border-radius: 8px;
}

.grading-message.history-message {
  opacity: 0.8;
  transform: translateY(0);
  animation: slideDown 0.3s ease-out;
}

.grading-message.history-message.history-0 {
  opacity: 0.9;
  border-left: 2px solid #52c41a;
  padding-left: 10px;
  margin-left: -10px;
  background: rgba(82, 196, 26, 0.03);
}

.grading-message.history-message.history-1 {
  opacity: 0.7;
  border-left: 2px solid #faad14;
  padding-left: 10px;
  margin-left: -10px;
  background: rgba(250, 173, 20, 0.03);
}

.grading-message.selected-history {
  border-left: 3px solid #722ed1;
  padding-left: 12px;
  margin-left: -12px;
  background: rgba(114, 46, 209, 0.05);
  border-radius: 8px;
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 0.8;
    transform: translateY(0);
  }
}

.message-avatar {
  flex-shrink: 0;
}

.ai-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, #0052d9 0%, #0034a3 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
}

.ai-avatar.current {
  background: linear-gradient(135deg, #0052d9 0%, #0034a3 100%);
  animation: pulse 2s infinite;
}

.ai-avatar.history {
  background: linear-gradient(135deg, #52c41a 0%, #389e0d 100%);
}

.ai-avatar.selected {
  background: linear-gradient(135deg, #722ed1 0%, #531dab 100%);
}

@keyframes pulse {
  0% { box-shadow: 0 0 0 0 rgba(0, 82, 217, 0.4); }
  70% { box-shadow: 0 0 0 10px rgba(0, 82, 217, 0); }
  100% { box-shadow: 0 0 0 0 rgba(0, 82, 217, 0); }
}

.message-content {
  flex: 1;
  min-width: 0;
}

.message-text {
  background: #f3f3f3;
  padding: 12px 16px;
  border-radius: 12px;
  line-height: 1.6;
  word-wrap: break-word;
}

.history-text {
  background: rgba(245, 245, 245, 0.8);
  border: 1px solid rgba(0, 0, 0, 0.06);
}

.selected-text {
  background: rgba(114, 46, 209, 0.08);
  border: 1px solid rgba(114, 46, 209, 0.2);
}

.message-time {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

.current-badge {
  background: #0052d9;
  color: white;
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 10px;
  margin-left: 6px;
}

.selected-badge {
  background: #722ed1;
  color: white;
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 10px;
  margin-left: 6px;
}

.typing-indicator {
  display: flex;
  gap: 4px;
  margin-bottom: 8px;
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

.history-section {
  margin-top: 20px;
}

.history-list {
  max-height: 300px;
  overflow-y: auto;
}

.history-item {
  padding: 12px;
  border: 1px solid #e7e7e7;
  border-radius: 8px;
  margin-bottom: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.history-item:hover {
  background-color: #f5f9ff;
  border-color: #c2d7ff;
}

.history-item-active {
  background-color: #e8f4fd;
  border-color: #0052d9;
  box-shadow: 0 2px 8px rgba(0, 82, 217, 0.15);
}

.history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.history-time {
  font-size: 12px;
  color: #999;
}

.history-content {
  font-size: 14px;
  line-height: 1.5;
  color: #666;
  max-height: 60px;
  overflow: hidden;
  text-overflow: ellipsis;
}

.history-toggle {
  text-align: center;
  margin-top: 12px;
}

.no-content {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  color: #999;
  font-size: 14px;
}

.no-content .message-text {
  background: transparent;
  padding: 0;
  color: #999;
}

/* 深度样式修改 */
:deep(.t-divider) {
  margin: 16px 0;
}

:deep(.t-tag) {
  margin-left: 8px;
}
</style>
