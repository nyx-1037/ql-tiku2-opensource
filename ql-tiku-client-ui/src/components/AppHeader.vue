<template>
  <aside class="sidebar" style="width: 250px;">
    <div class="sidebar-content">
      <!-- Logo区域 -->
      <div class="logo">
        <img v-if="systemConfig.siteLogo" :src="systemConfig.siteLogo" alt="Logo" class="logo-img" />
        <span v-else class="icon">📖</span>
        <span>{{ systemConfig.siteName || '在线刷题系统' }}</span>
        <!-- 移动端关闭按钮 -->
        <t-button
          variant="text"
          class="mobile-close-btn"
          @click="$emit('close-sidebar')"
          title="关闭侧边栏"
        >
          <span class="icon">❌</span>
        </t-button>
      </div>
      
      <!-- 标签区域 -->
      <div class="tags-section">
        <span class="tag beta-tag">Beta</span>
        <span class="tag client-tag">客户端</span>
        <span v-if="systemConfig.version" class="tag version-tag">系统版本：{{ systemConfig.version }}</span>
      </div>
      
      <!-- 导航菜单 -->
      <t-menu
        :value="activeMenu"
        class="nav-menu"
        @change="handleMenuSelect"
      >
        <t-menu-item value="/">
          <template #icon><span class="icon">🏠</span></template>
          首页
        </t-menu-item>
        <t-menu-item value="/practice">
          <template #icon><span class="icon">✏️</span></template>
          刷题练习
        </t-menu-item>
        <t-menu-item value="/practice-record">
          <template #icon><span class="icon">📄</span></template>
          刷题记录
        </t-menu-item>
        <t-menu-item value="/exam">
          <template #icon><span class="icon">📓</span></template>
          模拟考试
        </t-menu-item>
        <t-menu-item value="/exam-list">
          <template #icon><span class="icon">📋</span></template>
          考试记录
        </t-menu-item>
        <t-menu-item value="/wrong-book">
          <template #icon><span class="icon">📁</span></template>
          错题本
        </t-menu-item>
        <t-menu-item value="/resource-library">
          <template #icon><span class="icon">📂</span></template>
          资料库
        </t-menu-item>
        <t-menu-item value="/question-bank">
          <template #icon><span class="icon">📚</span></template>
          题库数据
        </t-menu-item>
        <t-menu-item value="/ai-chat">
          <template #icon><span class="icon">💬</span></template>
          AI助手
        </t-menu-item>
        <t-menu-item value="/analytics">
          <template #icon><span class="icon">📊</span></template>
          数据分析
        </t-menu-item>
      </t-menu>
      
      <!-- AI配额显示区域 -->
      <div class="ai-quota-section">
        <div v-if="aiQuotaInfo" class="quota-display">
          <div class="quota-header">
            <span class="icon">🤖</span>
            <span class="quota-title">AI配额</span>
          </div>
          <div class="quota-progress">
            <div class="quota-bar">
              <div 
                class="quota-fill" 
                :style="{ width: dailyUsagePercentage + '%', backgroundColor: getDailyProgressColor() }"
              ></div>
            </div>
            <div class="quota-text">{{ dailyRemaining }}/{{ aiQuotaInfo.dailyQuota }}</div>
          </div>
        </div>
        <div v-else class="quota-loading">
          <span class="icon">🤖</span>
          <span>加载中...</span>
        </div>
      </div>

      <!-- 用户信息区域 -->
      <div class="user-info">
        <div class="user-profile">
          <t-avatar v-if="userInfo.avatar" :image="userInfo.avatar" size="32px" class="user-avatar" />
          <t-avatar v-else size="32px" class="user-avatar">
            <span class="icon">👤</span>
          </t-avatar>
          <div class="user-details">
            <div class="username-row">
              <span class="username">{{ userInfo.username }}</span>
              <span v-if="aiQuotaInfo && aiQuotaInfo.vipLevel > 0" :class="getVipTagClass(aiQuotaInfo.vipLevel)" class="vip-tag">
                {{ getVipLevelText(aiQuotaInfo.vipLevel) }}
              </span>
            </div>
          </div>
        </div>

        <div class="user-actions">
          <div class="action-item" @click="handleUserCommand('profile')">
            <span class="icon">👤</span>
            <span>个人中心</span>
          </div>
          <div class="action-item logout-item" @click="handleUserCommand('logout')">
            <span class="icon">🚪</span>
            <span>退出登录</span>
          </div>
        </div>
      </div>
    </div>
  </aside>
</template>

<script>
import { reactive, computed, onMounted, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { MessagePlugin, DialogPlugin } from 'tdesign-vue-next'
// TDesign icons removed for compatibility
import { authAPI, aiQuotaAPI } from '../api'
import { useSystemStore } from '../store/system'

export default {
  name: 'AppHeader',
  components: {
    // 图标组件已移除
  },
  setup() {
    const router = useRouter()
    const route = useRoute()
    const systemStore = useSystemStore()
    
    const userInfo = reactive({
      username: '',
      avatar: ''
    })
    
    const aiQuotaInfo = ref(null)
    
    const activeMenu = computed(() => route.path)
    const systemConfig = computed(() => systemStore.siteConfig)
    
    // AI配额相关计算属性
    const dailyUsagePercentage = computed(() => {
      if (!aiQuotaInfo.value) return 0
      return Math.round((aiQuotaInfo.value.usedDaily / aiQuotaInfo.value.dailyQuota) * 100)
    })
    
    const dailyRemaining = computed(() => {
      if (!aiQuotaInfo.value) return 0
      return Math.max(0, aiQuotaInfo.value.dailyQuota - aiQuotaInfo.value.usedDaily)
    })
    
    // 获取用户信息
    const getUserInfo = async () => {
      const savedUserInfo = localStorage.getItem('userInfo')
      if (savedUserInfo) {
        Object.assign(userInfo, JSON.parse(savedUserInfo))
      }
      
      // 检查是否有token，只有登录用户才从服务器获取最新用户信息
      const token = localStorage.getItem('token')
      if (token) {
        try {
          const currentUser = await authAPI.getCurrentUser()
          Object.assign(userInfo, currentUser)
          localStorage.setItem('userInfo', JSON.stringify(currentUser))
        } catch (error) {
          console.error('获取用户信息失败:', error)
          // 如果获取失败，可能是token过期，清理本地存储
          localStorage.removeItem('token')
          localStorage.removeItem('userInfo')
        }
      } else {
        // 未登录用户显示默认信息
        Object.assign(userInfo, {
          username: '游客',
          avatar: ''
        })
      }
    }
    
    // 获取系统配置
    const getSystemConfig = async () => {
      await systemStore.loadSystemConfig()
    }
    
    // 获取AI配额信息
    const loadAiQuotaInfo = async () => {
      try {
        const response = await aiQuotaAPI.getQuotaInfo()
        aiQuotaInfo.value = response
      } catch (error) {
        console.error('获取AI配额信息失败:', error)
        aiQuotaInfo.value = null
      }
    }
    
    // 获取日配额进度条颜色
    const getDailyProgressColor = () => {
      if (!aiQuotaInfo.value) return '#67C23A'
      const percentage = (aiQuotaInfo.value.usedDaily / aiQuotaInfo.value.dailyQuota) * 100
      if (percentage >= 90) return '#F56C6C' // 红色，危险
      if (percentage >= 70) return '#E6A23C' // 橙色，警告
      return '#67C23A' // 绿色，正常
    }
    
    // 获取VIP标签类名
    const getVipTagClass = (vipLevel) => {
      if (vipLevel >= 4) return 'vip-tag-svip' // 超级VIP使用渐变色
      if (vipLevel >= 1) return 'vip-tag-normal' // VIP1-3使用红色
      return 'vip-tag-normal'
    }
    
    // 获取VIP等级文本
    const getVipLevelText = (vipLevel) => {
      const levelMap = {
        0: '普通用户',
        1: 'VIP1',
        2: 'VIP2', 
        3: 'VIP3',
        4: '超级VIP'
      }
      return levelMap[vipLevel] || '未知'
    }
    
    // 菜单选择处理
    const handleMenuSelect = (index) => {
      router.push(index)
    }
    
    // 用户操作处理
    const handleUserCommand = (command) => {
      if (command === 'profile') {
        router.push('/profile')
      } else if (command === 'logout') {
        const confirmDialog = DialogPlugin.confirm({
          header: '提示',
          body: '确定要退出登录吗？',
          confirmBtn: '确定',
          cancelBtn: '取消',
          theme: 'warning',
          onConfirm: async () => {
            try {
              // 手动关闭对话框
              confirmDialog.destroy()
              
              // 清理 AI 聊天缓存
              try {
                const { useAiChatStore } = await import('@/store/aiChat')
                const aiChatStore = useAiChatStore()
                if (typeof aiChatStore.clearUserCache === 'function') {
                  aiChatStore.clearUserCache()
                }
              } catch (error) {
                console.warn('清理AI聊天缓存失败:', error)
              }
              
              // 执行退出登录
              try {
                await authAPI.logout()
              } catch (error) {
                console.warn('服务器退出登录失败:', error)
              }
              
              // 清理本地存储
              localStorage.removeItem('token')
              localStorage.removeItem('userInfo')
              localStorage.removeItem('ai-chat-store')
              
              MessagePlugin.success('退出登录成功')
              router.push('/login')
            } catch (error) {
              console.error('退出登录失败:', error)
              MessagePlugin.error('退出登录失败，请重试')
            }
          },
          onCancel: () => {
            // 手动关闭对话框
            confirmDialog.destroy()
            console.log('用户取消退出登录')
          }
        })
      }
    }
    
    onMounted(() => {
      getUserInfo()
      loadAiQuotaInfo()
      // 如果没有缓存的配置，则从服务器加载
      if (!systemStore.siteConfig.siteName || systemStore.siteConfig.siteName === '在线刷题系统') {
        getSystemConfig()
      }
    })
    
    return {
      userInfo,
      aiQuotaInfo,
      systemConfig,
      activeMenu,
      dailyUsagePercentage,
      dailyRemaining,
      handleMenuSelect,
      handleUserCommand,
      getDailyProgressColor,
      getVipTagClass,
      getVipLevelText
    }
  }
}
</script>

<style scoped>
.sidebar {
  background: #001529;
  height: 100vh;
  position: fixed;
  left: 0;
  top: 0;
  z-index: 1000;
  overflow-y: auto;
  transition: transform 0.3s ease;
}

.sidebar.sidebar-hidden {
  transform: translateX(-100%);
}

.sidebar-content {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 0;
}

.logo {
  display: flex;
  align-items: center;
  padding: 20px 16px;
  font-size: 18px;
  font-weight: 600;
  color: white;
  border-bottom: 1px solid #1f1f1f;
  margin-bottom: 0;
  position: relative;
}

/* 标签区域样式 */
.tags-section {
  display: flex;
  gap: 8px;
  padding: 8px 16px 16px 16px;
  border-bottom: 1px solid #1f1f1f;
  margin-bottom: 8px;
}

.tag {
  font-size: 11px;
  padding: 3px 8px;
  border-radius: 12px;
  font-weight: 500;
  white-space: nowrap;
}

.beta-tag {
  background-color: #ff6b35;
  color: white;
}

.client-tag {
  background-color: #1890ff;
  color: white;
}

.version-tag {
  background-color: #52c41a;
  color: white;
}

.logo .t-icon {
  margin-right: 8px;
  font-size: 24px;
  color: #1890ff;
}

.logo-img {
  height: 32px;
  width: auto;
  margin-right: 8px;
}

.nav-menu {
  flex: 1;
  border: none;
  background: transparent;
}

.nav-menu .t-menu-item {
  color: white !important;
  border-radius: 0;
  margin: 0;
  height: 48px;
  line-height: 48px;
}

.nav-menu .t-menu-item:hover {
  background-color: #1890ff !important;
  color: white !important;
}

.nav-menu .t-menu-item.is-active {
  background-color: #1890ff !important;
  color: white !important;
}

.nav-menu .t-menu-item .t-icon {
  margin-right: 8px;
  font-size: 16px;
  color: white !important;
}

/* 强制覆盖TDesign默认样式 */
:deep(.t-menu__item) {
  color: white !important;
}

:deep(.t-menu__item:hover) {
  color: white !important;
  background-color: #1890ff !important;
}

:deep(.t-menu__item.t-is-active) {
  color: white !important;
  background-color: #1890ff !important;
}

:deep(.t-menu__item .t-icon) {
  color: white !important;
}

:deep(.t-menu__item .t-menu__content) {
  color: white !important;
}

:deep(.t-menu__item .t-menu__title) {
  color: white !important;
}

:deep(.t-menu__item span) {
  color: white !important;
}

/* AI配额显示区域 */
.ai-quota-section {
  border-top: 1px solid #1f1f1f;
  padding: 12px 16px;
  margin-top: auto;
}

.quota-display {
  color: white;
}

.quota-header {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
  font-size: 14px;
}

.quota-header .icon {
  margin-right: 6px;
  font-size: 16px;
}

.quota-title {
  font-weight: 500;
}

.quota-progress {
  margin-bottom: 4px;
}

.quota-bar {
  width: 100%;
  height: 6px;
  background-color: #1f1f1f;
  border-radius: 3px;
  overflow: hidden;
  margin-bottom: 4px;
}

.quota-fill {
  height: 100%;
  transition: all 0.3s ease;
  border-radius: 3px;
}

.quota-text {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
  text-align: center;
}

.quota-loading {
  display: flex;
  align-items: center;
  color: rgba(255, 255, 255, 0.7);
  font-size: 12px;
}

.quota-loading .icon {
  margin-right: 6px;
}

.user-info {
  border-top: 1px solid #1f1f1f;
}

.user-profile {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #1f1f1f;
}

.user-avatar {
  margin-right: 12px;
  flex-shrink: 0;
}

.user-details {
  flex: 1;
  min-width: 0;
}

.username-row {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.username {
  color: white;
  font-size: 14px;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.vip-tag {
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 8px;
  font-weight: bold;
  white-space: nowrap;
}

.vip-tag-normal {
  background-color: #f56c6c;
  color: white;
}

.vip-tag-svip {
  background: linear-gradient(45deg, #dc2626, #1d4ed8, #ca8a04, #dc2626);
  background-size: 400% 400%;
  animation: gradient-shift 2.5s ease infinite;
  color: white;
  font-weight: bold;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.6);
  border: 1px solid rgba(255, 255, 255, 0.3);
}

@keyframes gradient-shift {
  0% { background-position: 0% 50%; }
  25% { background-position: 100% 0%; }
  50% { background-position: 100% 100%; }
  75% { background-position: 0% 100%; }
  100% { background-position: 0% 50%; }
}

.user-actions {
  padding: 8px 0;
}

.action-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  color: rgba(255, 255, 255, 0.85) !important;
  cursor: pointer;
  transition: all 0.3s;
  border-radius: 0;
}

.action-item:hover {
  background-color: rgba(255, 255, 255, 0.1) !important;
  color: white !important;
}

.action-item .t-icon {
  margin-right: 8px;
  font-size: 16px;
  color: rgba(255, 255, 255, 0.85) !important;
}

.action-item span {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.85) !important;
}

.logout-item {
  color: #f56c6c !important;
}

.logout-item:hover {
  background-color: rgba(245, 108, 108, 0.1) !important;
  color: #f56c6c !important;
}

.logout-item .icon,
.logout-item span {
  color: #f56c6c !important;
}

/* 移动端关闭按钮 */
.mobile-close-btn {
  display: none;
  position: absolute;
  right: 16px;
  top: 50%;
  transform: translateY(-50%);
  padding: 8px;
  border-radius: 4px;
  color: rgba(255, 255, 255, 0.65);
  transition: all 0.3s;
}

.mobile-close-btn:hover {
  background-color: rgba(255, 255, 255, 0.1);
  color: white;
}

.mobile-close-btn .t-icon {
  font-size: 18px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .mobile-close-btn {
    display: block;
  }
  
  .username-row {
    flex-direction: column;
    align-items: flex-start;
    gap: 4px;
  }
  
  .ai-quota-section {
    padding: 10px 16px;
  }
  
  .quota-header {
    font-size: 13px;
  }
  
  .quota-text {
    font-size: 11px;
  }
}

/* 滚动条样式 */
.sidebar::-webkit-scrollbar {
  width: 6px;
}

.sidebar::-webkit-scrollbar-track {
  background: #001529;
}

.sidebar::-webkit-scrollbar-thumb {
  background: #1f1f1f;
  border-radius: 3px;
}

.sidebar::-webkit-scrollbar-thumb:hover {
  background: #333;
}
</style>