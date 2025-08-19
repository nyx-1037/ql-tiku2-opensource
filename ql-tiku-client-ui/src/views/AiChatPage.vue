<template>
  <div class="ai-chat-page">
        <div class="chat-layout">
          <!-- 左侧会话列表 -->
          <div class="session-sidebar" v-show="!hideSidebar">
            <div class="sidebar-header">
              <div>
                <h3>对话历史</h3>
                <div class="header-actions">
                  <t-button
                    theme="primary" 
                    size="small" 
                    @click="createNewSession"
                    :loading="creating"
                    :disabled="remainingQuota <= 0"
                  >
                    <span class="icon">➕</span>
                    新建对话
                  </t-button>
                  <t-button
                    size="small" 
                    @click="toggleChatSidebar"
                    title="隐藏对话历史"
                  >
                    <t-icon>《</t-icon>
                  </t-button>
                </div>
              </div>
              <div class="quota-info" v-if="quotaInfo && quotaInfo.dailyQuota">
                <t-tag size="small" theme="info">
                  今日剩余: {{ remainingQuota }}/{{ quotaInfo.dailyQuota }} 次
                </t-tag>
              </div>
            </div>
            
            <div class="session-list">
              <div 
                v-for="session in sessionList" 
                :key="session.sessionId"
                :class="['session-item', { active: session.sessionId === currentSessionId }]"
                @click="switchToSession(session.sessionId)"
              >
                <div class="session-content">
                  <div class="session-title">{{ session.title }}</div>
                  <div class="session-preview">{{ session.lastMessage || '暂无消息' }}</div>
                  <div class="session-time">{{ formatTime(session.updateTime) }}</div>
                </div>
                <div class="session-actions" @click.stop="toggleSessionMenu(session.sessionId)">
                  <t-icon name="more" class="more-icon" />
                  <div v-if="activeMenuSessionId === session.sessionId" class="session-menu" @click.stop>
                    <div class="menu-item" @click="handleSessionAction('rename', session)">重命名</div>
                    <div class="menu-item" @click="handleSessionAction('delete', session)">删除</div>
                  </div>
                </div>
              </div>
              
              <div v-if="sessionList.length === 0" class="empty-sessions">
                <t-empty description="暂无对话历史" />
              </div>
            </div>
          </div>
          
          <!-- 右侧聊天区域 -->
          <div class="chat-main">
            <t-card class="ai-chat-large-card">
              <template #header>
                <div class="ai-chat-header">
                  <div class="header-left">
                    <t-button
                      v-if="hideSidebar"
                      size="small" 
                      @click="toggleChatSidebar"
                      title="显示对话历史"
                      style="margin-right: 12px;"
                    >
                      <t-icon name="chevron-right" />
                    </t-button>
                    <t-icon name="chat" class="ai-icon" />
                    <span class="title">{{ currentSessionTitle }}</span>
                  </div>
                  <div class="header-right">
                    <t-button
                      v-if="aiChatRef && aiChatRef.isTyping"
                      size="small"
                      theme="warning"
                      variant="base"
                      @click="stopAiGeneration"
                    >
                      <t-icon name="pause" />
                      暂停
                    </t-button>
                    <t-button
                      size="small" 
                      @click="refreshSessions"
                      :loading="loading"
                    >
                      <t-icon name="refresh" />
                      刷新
                    </t-button>
                  </div>
                </div>
              </template>
              
              <div class="ai-chat-wrapper">
                <!-- 模型选择器放在最上方 -->
                <div class="model-selector-wrapper">
                  <div class="model-selector-label">AI模型:</div>
                <Multiselect
                  v-model="selectedModelId"
                  :options="modelOptions"
                  value-prop="value"
                  label="label"
                  placeholder="选择AI模型"
                  :can-clear="false"
                  :searchable="false"
                  @change="handleModelChange"
                  :classes="{
                    container: 'ai-chat-multiselect-container',
                    dropdown: 'ai-chat-select-dropdown'
                  }"
                  class="model-selector"
                />
                </div>
                
                <TDesignAiChat
                  ref="aiChatRef"
                  :session-id="currentSessionId"
                  :user-avatar="userAvatar"
                  :model-id="String(selectedModelId)"
                  @session-cleared="handleSessionCleared"
                  @message-sent="handleMessageSent"
                  @typing-status="handleTypingStatus"
                />
              </div>
            </t-card>
          </div>
        </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, onUnmounted } from 'vue'
import TDesignAiChat from '@/components/TDesignAiChat.vue'
import Multiselect from '@vueform/multiselect'
import { useAiChatStore } from '@/store/aiChat'
import { MessagePlugin, DialogPlugin } from 'tdesign-vue-next'
import { aiQuotaAPI, aiModelAPI } from '@/api'
import axios from 'axios'

// 使用Pinia store
const aiChatStore = useAiChatStore()

// 响应式数据
const aiChatRef = ref(null)
const creating = ref(false)
const hideSidebar = ref(false)
const quotaInfo = ref(null)
const remainingQuota = ref(0)
const userAvatar = ref('')
const activeMenuSessionId = ref(null)
const selectedModelId = ref('')
const modelOptions = ref([])

// 计算属性
const currentSessionTitle = computed(() => {
  const session = aiChatStore.sessions.find(s => s.sessionId === aiChatStore.currentSessionId)
  return session ? session.title : 'AI智能助手'
})

const sessionList = computed(() => aiChatStore.sessions)
const currentSessionId = computed(() => aiChatStore.currentSessionId)
const loading = computed(() => aiChatStore.loading)

// 获取当前用户ID
const currentUserId = computed(() => {
  const userInfo = localStorage.getItem('userInfo')
  if (userInfo) {
    try {
      const user = JSON.parse(userInfo)
      return user.id || user.userId
    } catch (e) {
      console.error('解析用户信息失败:', e)
    }
  }
  return null
})

// 格式化时间
const formatTime = (timeStr) => {
  if (!timeStr) return ''
  
  console.log('🕐 AiChatPage格式化时间:', {
    原始timeStr: timeStr,
    timeStr类型: typeof timeStr
  })
  
  let date
  try {
    // 处理不同的时间格式
    if (typeof timeStr === 'string') {
      // 如果是字符串，直接解析
      date = new Date(timeStr)
    } else if (typeof timeStr === 'number') {
      // 如果是数字，可能是时间戳
      date = new Date(timeStr)
    } else {
      // 如果已经是Date对象
      date = new Date(timeStr)
    }
    
    // 检查日期是否有效
    if (isNaN(date.getTime())) {
      console.error('无效的时间格式:', timeStr)
      return '时间未知'
    }
    
    const now = new Date()
    const diff = now - date
    
    console.log('🕐 AiChatPage时间计算:', {
      解析后的date: date.toISOString(),
      当前时间: now.toISOString(),
      时间差毫秒: diff,
      时间差分钟: Math.floor(diff / 60000)
    })
    
    if (diff < 60000) { // 1分钟内
      return '刚刚'
    } else if (diff < 3600000) { // 1小时内
      return `${Math.floor(diff / 60000)}分钟前`
    } else if (diff < 86400000) { // 1天内
      return `${Math.floor(diff / 3600000)}小时前`
    } else if (diff < 86400000 * 7) { // 1周内
      return `${Math.floor(diff / 86400000)}天前`
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
    console.error('AiChatPage时间格式化失败:', error, timeStr)
    return '时间解析失败'
  }
}

// 获取会话列表
const loadSessionList = async () => {
  try {
    await aiChatStore.loadSessionList()
  } catch (error) {
    console.error('获取会话列表失败:', error)
    MessagePlugin.error('获取会话列表失败')
  }
}

// 创建新会话
const createNewSession = async () => {
  try {
    creating.value = true
    console.log('🆕 AiChatPage: 开始创建新会话')
    
    // 检查AI配额
    console.log('🔍 AiChatPage: 检查AI配额')
    const hasQuota = await checkAiQuota()
    if (!hasQuota) {
      console.log('❌ AiChatPage: 配额不足，取消创建会话')
      return
    }
    console.log('✅ AiChatPage: 配额检查通过')
    
    // 调用store创建会话
    console.log('📞 AiChatPage: 调用store创建会话')
    const sessionId = await aiChatStore.createSession()
    console.log('✅ AiChatPage: 新会话创建成功:', sessionId)
    
    // 重新加载会话列表
    console.log('🔄 AiChatPage: 重新加载会话列表')
    await loadSessionList()
    
    // 确保切换到新创建的会话
    if (sessionId && sessionId !== aiChatStore.currentSessionId) {
      console.log('🔄 AiChatPage: 切换到新创建的会话:', sessionId)
      await switchToSession(sessionId)
    }
    
    MessagePlugin.success('新会话创建成功')
  } catch (error) {
    console.error('❌ AiChatPage: 创建新会话失败:', error)
    MessagePlugin.error('创建新会话失败: ' + error.message)
  } finally {
    creating.value = false
  }
}

// 切换到指定会话
const switchToSession = async (sessionId) => {
  if (sessionId === aiChatStore.currentSessionId) return

  console.log('🔄 AiChatPage: 切换到会话:', sessionId)
  // 使用store的switchSession方法切换会话，该方法会自动加载消息历史
  await aiChatStore.switchSession(sessionId)
  console.log('✅ AiChatPage: 会话切换完成')
}

// 阻止事件冒泡
const stopPropagation = (event) => {
  if (event && event.stopPropagation) {
    event.stopPropagation()
  }
}

// 切换会话菜单显示
const toggleSessionMenu = (sessionId) => {
  if (activeMenuSessionId.value === sessionId) {
    activeMenuSessionId.value = null
  } else {
    activeMenuSessionId.value = sessionId
  }
}

// 点击页面其他地方关闭菜单
const handleClickOutside = (event) => {
  if (!event.target.closest('.session-actions')) {
    activeMenuSessionId.value = null
  }
}

// 处理会话操作
const handleSessionAction = async (command, session) => {
  // 关闭菜单
  activeMenuSessionId.value = null
  
  if (command === 'delete') {
    const dialog = DialogPlugin.confirm({
      header: '确认删除',
      body: `确定要删除会话"${session.title}"吗？此操作不可恢复。`,
      confirmBtn: '确定',
      cancelBtn: '取消',
      theme: 'warning',
      onConfirm: async () => {
        try {
          await aiChatStore.deleteSession(session.sessionId)
          dialog.hide() // 显式关闭对话框
          MessagePlugin.success('会话删除成功')

          // 重新加载会话列表
          await loadSessionList()

          // 如果删除的是当前会话，切换到第一个会话或创建新会话
          if (session.sessionId === aiChatStore.currentSessionId) {
            if (aiChatStore.sessions.length > 0) {
              await switchToSession(aiChatStore.sessions[0].sessionId)
            } else {
              await createNewSession()
            }
          }
        } catch (error) {
          console.error('删除会话失败:', error)
          MessagePlugin.error('删除会话失败')
          dialog.hide() // 错误时也关闭对话框
        }
      },
      onCancel: () => {
        dialog.hide() // 取消时也关闭对话框
      }
    })
  } else if (command === 'rename') {
    const dialog = DialogPlugin.prompt({
      header: '重命名会话',
      body: '请输入新的会话标题：',
      confirmBtn: '确定',
      cancelBtn: '取消',
      theme: 'info',
      inputValue: session.title,
      onConfirm: async (newTitle) => {
        if (newTitle && newTitle.trim() && newTitle.trim() !== session.title) {
          try {
            await aiChatStore.updateSessionTitle(session.sessionId, newTitle.trim())
            dialog.hide() // 显式关闭对话框
            MessagePlugin.success('会话标题更新成功')

            // 重新加载会话列表以更新显示
            await loadSessionList()
          } catch (error) {
            console.error('更新会话标题失败:', error)
            MessagePlugin.error('更新会话标题失败')
            dialog.hide() // 错误时也关闭对话框
          }
        } else if (!newTitle || !newTitle.trim()) {
          MessagePlugin.warning('会话标题不能为空')
          return false // 阻止对话框关闭
        } else {
          dialog.hide() // 标题未改变，直接关闭
        }
      },
      onCancel: () => {
        dialog.hide() // 取消时也关闭对话框
      }
    })
  }
}

// 刷新会话列表
const refreshSessions = () => {
  loadSessionList()
}

// 切换AI对话记录侧边栏显示/隐藏
const toggleChatSidebar = () => {
  hideSidebar.value = !hideSidebar.value
}

// 处理会话清空
const handleSessionCleared = () => {
  MessagePlugin.success('会话已清空')
  // 重新加载会话列表以更新消息计数
  loadSessionList()
}

// 处理消息发送事件
const handleMessageSent = (data) => {
  console.log('消息已发送:', data)
  // 可以在这里更新配额信息
  loadQuotaInfo()
}

// 处理打字状态变化
const handleTypingStatus = (isTyping) => {
  console.log('打字状态:', isTyping)
  // 可以在这里更新UI状态
}

// 获取用户头像
const getUserAvatar = () => {
  const userInfo = localStorage.getItem('userInfo')
  if (userInfo) {
    try {
      const user = JSON.parse(userInfo)
      userAvatar.value = user.avatar || ''
    } catch (e) {
      console.error('解析用户信息失败:', e)
    }
  }
}

// 停止AI生成
const stopAiGeneration = () => {
  if (aiChatRef.value) {
    aiChatRef.value.stopGeneration()
  }
}

// 加载AI模型列表
const loadModelOptions = async () => {
  try {
    console.log('🤖 AiChatPage: 开始加载AI模型列表')
    const models = await aiModelAPI.getEnabledModels()
    console.log('🤖 AiChatPage: 模型列表响应:', models)
    
    // aiModelAPI已经在响应拦截器中处理了数据格式，直接使用返回的data
    if (models && Array.isArray(models) && models.length > 0) {
      modelOptions.value = models.map(model => ({
        value: model.id, // 保持原始数字类型，让VueformSelect组件处理
        label: model.name,
        description: model.description
      }))
      
      // 设置默认选中第一个模型
      if (modelOptions.value.length > 0 && !selectedModelId.value) {
        selectedModelId.value = modelOptions.value[0].value
        console.log('🤖 AiChatPage: 设置默认模型:', selectedModelId.value)
      }
    } else {
      // 如果没有启用的模型，提供默认选项
      console.warn('⚠️ AiChatPage: 没有找到启用的AI模型，使用默认配置')
      modelOptions.value = [
        { value: 1, label: '通义千问-Turbo', description: '快速响应模型' },
        { value: 2, label: '通义千问-Plus', description: '平衡性能模型' },
        { value: 3, label: '通义千问-Max', description: '高性能模型' }
      ]
      selectedModelId.value = 1
    }
    
    console.log('🤖 AiChatPage: 最终模型选项:', modelOptions.value)
    console.log('🤖 AiChatPage: 模型选项数量:', modelOptions.value.length)
    console.log('🤖 AiChatPage: 选中的模型ID:', selectedModelId.value)
    
    // 添加调试信息：检查每个选项的结构
    modelOptions.value.forEach((option, index) => {
      console.log(`🔍 AiChatPage: 选项${index}:`, {
        value: option.value,
        label: option.label,
        valueType: typeof option.value,
        labelType: typeof option.label
      })
    })
  } catch (error) {
    console.error('❌ AiChatPage: 加载AI模型列表失败:', error)
    // 提供默认模型选项作为后备
    modelOptions.value = [
      { value: 1, label: '通义千问-Turbo', description: '快速响应模型' },
      { value: 2, label: '通义千问-Plus', description: '平衡性能模型' },
      { value: 3, label: '通义千问-Max', description: '高性能模型' }
    ]
    selectedModelId.value = 1
    MessagePlugin.warning('使用默认AI模型配置')
  }
}

// 处理模型切换
const handleModelChange = (value) => {
  console.log('🔄 AiChatPage: 模型切换事件触发:', value, typeof value)
  
  // @vueform/multiselect 的 change 事件可能传递不同格式的参数
  // 需要根据实际情况处理
  let modelId = value
  
  // 如果传递的是对象，提取 value 属性
  if (value && typeof value === 'object' && value.value !== undefined) {
    modelId = value.value
  }
  
  console.log('🔄 AiChatPage: 解析后的模型ID:', modelId, typeof modelId)
  
  if (modelId !== undefined && modelId !== null) {
    selectedModelId.value = modelId
    
    // 查找对应的模型信息并显示提示
    const selectedModel = modelOptions.value.find(model => model.value === modelId)
    if (selectedModel) {
      console.log('✅ AiChatPage: 模型切换成功:', selectedModel.label)
      MessagePlugin.info(`已切换到模型: ${selectedModel.label}`)
    }
  } else {
    console.warn('⚠️ AiChatPage: 模型切换参数无效:', value)
  }
}

// 加载AI配额信息
const loadQuotaInfo = async () => {
  try {
    console.log('正在加载AI配额信息...')
    const response = await aiQuotaAPI.getQuotaInfo()
    console.log('配额信息响应:', response)
    quotaInfo.value = response.data
    
    const remainingResponse = await aiQuotaAPI.getRemainingQuota()
    console.log('剩余配额响应:', remainingResponse)
    remainingQuota.value = remainingResponse.data
  } catch (error) {
    console.error('加载AI配额信息失败:', error)
    // 如果配额接口失败，提供默认值
    quotaInfo.value = { dailyQuota: 10, usedQuota: 0 }
    remainingQuota.value = 10
  }
}

// 检查AI配额
const checkAiQuota = async () => {
  try {
    const response = await aiQuotaAPI.checkQuota('chat')
    console.log('🔍 AiChatPage: 配额检查响应:', response)
    
    // 检查响应结构，response.data应该是boolean值
    // 如果data为true表示有配额，false表示无配额
    if (response.data === false) {
      MessagePlugin.warning('今日AI对话次数已用完，请明天再试或联系管理员')
      return false
    }
    return true
  } catch (error) {
    console.error('检查AI配额失败:', error)
    MessagePlugin.error('检查AI配额失败')
    return false
  }
}

// 组件挂载时加载会话列表
onMounted(async () => {
  console.log('🎯 AiChatPage: 组件挂载开始')

  // 同步当前用户ID到store
  if (currentUserId.value) {
    aiChatStore.currentUserId = currentUserId.value
  }

  // 加载AI配额信息
  await loadQuotaInfo()

  // 加载AI模型列表
  await loadModelOptions()

  // 获取用户头像
  getUserAvatar()

  // 检查用户是否切换
  const userSwitched = aiChatStore.checkUserSwitch()
  console.log('👤 AiChatPage: 用户切换检查:', userSwitched)

  if (userSwitched) {
    console.log('🔄 AiChatPage: 用户已切换，重新加载数据')
    // 用户已切换，清空本地缓存并重新从服务器加载
    aiChatStore.$reset()
    await loadSessionList()

    if (aiChatStore.sessions.length > 0) {
      await switchToSession(aiChatStore.sessions[0].sessionId)
    } else {
      await createNewSession()
    }
    return
  }

  // 检查本地缓存的有效性
  const hasCachedSessions = aiChatStore.sessions.length > 0
  const hasCachedCurrentSession = aiChatStore.currentSessionId &&
    aiChatStore.sessions.find(s => s.sessionId === aiChatStore.currentSessionId)

  console.log('💾 AiChatPage: 缓存状态检查:', {
    hasCachedSessions,
    hasCachedCurrentSession,
    sessionsCount: aiChatStore.sessions.length,
    currentSessionId: aiChatStore.currentSessionId
  })

  if (hasCachedSessions && hasCachedCurrentSession) {
    console.log('✅ AiChatPage: 使用缓存的会话数据')
    // 有有效的缓存数据，直接使用
    // 但需要同步服务器数据以获取最新的会话列表（不阻塞UI）
    loadSessionList().catch(error => {
      console.warn('⚠️ AiChatPage: 后台同步会话列表失败:', error)
    })
    return
  }

  console.log('🌐 AiChatPage: 从服务器加载会话数据')
  // 没有有效缓存，从服务器加载
  await loadSessionList()

  if (aiChatStore.sessions.length > 0) {
    // 如果有现有会话，选择第一个并强制加载其历史消息
    const firstSessionId = aiChatStore.sessions[0].sessionId
    console.log('📋 AiChatPage: 切换到现有会话:', firstSessionId)
    
    // 强制加载历史消息
    await aiChatStore.loadSessionMessages(firstSessionId)
    await switchToSession(firstSessionId)
  } else {
    // 只有在完全没有会话时才创建新会话
    console.log('➕ AiChatPage: 创建新会话')
    await createNewSession()
  }

  console.log('🎯 AiChatPage: 组件挂载完成')

  // 添加全局点击事件监听器
  document.addEventListener('click', handleClickOutside)
})

// 组件卸载时清理事件监听器
onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<style scoped>
.ai-chat-page {
  width: 100%;
  height: calc(100vh - 50px);
  padding: 20px;
}

.chat-layout {
  display: flex;
  height: 100%;
  gap: 20px;
  transition: all 0.3s ease;
}

.session-sidebar {
  width: 300px;
  background: #f8f9fa;
  border-radius: 8px;
  padding: 16px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.sidebar-header {
  display: flex;
  flex-direction: column;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #e4e7ed;
}

.sidebar-header > div:first-child {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.quota-info {
  margin-bottom: 8px;
  padding: 4px 8px;
  background-color: #f0f9ff;
  border-radius: 4px;
  border-left: 3px solid #409eff;
  text-align: center;
}

.header-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

.sidebar-header h3 {
  margin: 0;
  font-size: 16px;
  color: #303133;
}

.session-list {
  flex: 1;
  overflow-y: auto;
}

.session-item {
  display: flex;
  align-items: center;
  padding: 12px;
  margin-bottom: 8px;
  background: white;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid #e4e7ed;
}

.session-item:hover {
  background: #f0f9ff;
  border-color: #409eff;
}

.session-item.active {
  background: #e6f7ff;
  border-color: #409eff;
}

.session-content {
  flex: 1;
  min-width: 0;
}

.session-title {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.session-preview {
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.session-time {
  font-size: 11px;
  color: #c0c4cc;
}

.session-actions {
  margin-left: 8px;
  position: relative;
}

.more-icon {
  font-size: 16px;
  color: #909399;
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  transition: all 0.2s;
}

.more-icon:hover {
  background: #f0f0f0;
  color: #409eff;
}

.session-menu {
  position: absolute;
  top: 100%;
  right: 0;
  background: white;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  z-index: 1000;
  min-width: 90px;
  margin-top: 4px;
  overflow: hidden;
}

.menu-item {
  padding: 8px 12px;
  font-size: 13px;
  color: #606266;
  cursor: pointer;
  transition: background-color 0.2s;
  text-align: center;
}

.menu-item:hover {
  background-color: #f5f7fa;
}

.menu-item:last-child {
  color: #f56c6c;
}

.empty-sessions {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 200px;
}

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  transition: all 0.3s ease;
  min-width: 0; /* 确保flex子项可以收缩 */
}

.ai-chat-large-card {
  height: 100%;
  display: flex;
  flex-direction: column;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
  border-radius: 12px;
  min-height: 600px;
}

.ai-chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.ai-icon {
  font-size: 24px;
  color: #409eff;
}

.title {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.ai-chat-wrapper {
  flex: 1;
  height: calc(100vh - 220px);
  overflow: hidden;
  max-width: none; /* 移除最大宽度限制 */
  display: flex;
  flex-direction: column;
  position: relative;
}

.ai-chat-wrapper :deep(.tdesign-ai-chat-container) {
  height: 100%;
  box-shadow: none;
  border-radius: 0;
  display: flex;
  flex-direction: column;
  border: none;
}

.ai-chat-wrapper :deep(.chat-header) {
  display: flex;
} 

.ai-chat-wrapper :deep(.chat-messages-area) {
  flex: 1;
  max-height: none;
  padding: 20px;
  overflow-y: auto;
}

.ai-chat-wrapper :deep(.chat-input-area) {
  margin-top: 0;
  padding: 0;
  border-top: none;
  background: transparent;
  flex-shrink: 0;
}

.model-selector-wrapper {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  background: #f8f9fa;
  border-bottom: 1px solid #ebeef5;
  gap: 12px;
  order: 1; /* 确保在聊天组件前面 */
}

.model-selector-label {
  font-size: 13px;
  color: #606266;
  font-weight: 500;
  white-space: nowrap;
}

.model-selector {
  width: 160px;
  flex-shrink: 0;
}

.model-selector :deep(.vs__dropdown-toggle) {
  min-height: 32px;
  font-size: 13px;
}

.model-selector :deep(.vs__selected) {
  font-size: 13px;
}

/* 重新排列聊天组件的顺序 */
.ai-chat-wrapper :deep(.tdesign-ai-chat-container) {
  order: 2;
  flex: 1;
}

/* 隐藏侧边栏时的样式 */
.chat-layout.hide-sidebar .session-sidebar {
  transform: translateX(-100%);
}

.chat-layout.hide-sidebar .chat-main {
  margin-left: 0;
  width: 100%; /* 隐藏侧边栏时占据全宽 */
}

/* AI Chat Multiselect 自定义样式 */
:deep(.ai-chat-multiselect-container) {
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  min-height: 32px;
  background: white;
  transition: all 0.2s;
  position: relative;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  box-sizing: border-box;
}

:deep(.ai-chat-multiselect-container:hover) {
  border-color: #4dabf7;
}

:deep(.ai-chat-multiselect-container.is-active) {
  border-color: #0052d9;
  box-shadow: 0 0 0 2px rgba(0, 82, 217, 0.1);
}

:deep(.ai-chat-select-dropdown) {
  border: 1px solid #e6e6e6;
  border-radius: 6px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  z-index: 99999 !important;
  background: white;
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  max-height: 200px;
  overflow-y: auto;
}

:deep(.ai-chat-select-dropdown.is-hidden) {
  display: none !important;
}

:deep(.ai-chat-multiselect-container .multiselect) {
  min-height: 32px;
  height: 32px;
  width: 100%;
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif !important;
  font-size: 14px !important;
  line-height: 1.5 !important;
}

:deep(.ai-chat-multiselect-container .multiselect-single-label) {
  padding-left: 12px;
  padding-right: 40px;
  line-height: 30px;
}

:deep(.ai-chat-multiselect-container .multiselect-placeholder) {
  padding-left: 12px;
  line-height: 30px;
  color: #bbb;
}

:deep(.ai-chat-multiselect-container .multiselect-caret) {
  margin-right: 12px;
}

/* 选项样式 */
:deep(.multiselect-option) {
  padding: 8px 12px;
  background: white;
  color: #333;
  cursor: pointer;
  transition: background-color 0.2s;
}

:deep(.multiselect-option:hover),
:deep(.multiselect-option.is-pointed) {
  background-color: #f3f3f3;
}

:deep(.multiselect-option.is-selected) {
  background-color: #0052d9;
  color: white;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .main-content {
    max-width: 100%;
    padding: 16px;
  }
  
  .ai-chat-page {
    padding: 16px;
  }
  
  .session-sidebar {
    width: 280px;
  }
  
  .ai-chat-wrapper :deep(.chat-messages) {
    flex: 1;
    max-height: none;
  }
}

@media (max-width: 768px) {
  .main-content {
    padding: 10px;
  }
  
  .ai-chat-page {
    height: calc(100vh - 120px);
  }
  
  .chat-layout {
    flex-direction: column;
    height: auto;
  }
  
  .session-sidebar {
    width: 100%;
    height: 200px;
    margin-bottom: 16px;
  }
  
  .ai-chat-large-card {
    height: calc(100vh - 360px);
  }
  
  .ai-chat-wrapper {
    height: calc(100vh - 200px);
  }
  
  .ai-chat-wrapper :deep(.chat-messages) {
    flex: 1;
    max-height: none;
  }
  
  .title {
    font-size: 18px;
  }
}
</style>

<style src="@vueform/multiselect/themes/default.css"></style>
