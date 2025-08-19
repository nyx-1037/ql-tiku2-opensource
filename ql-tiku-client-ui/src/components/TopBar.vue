<template>
  <div class="top-bar">
    <div class="top-bar-left">
      <t-button
        variant="text"
        @click="toggleSidebar"
        class="sidebar-toggle"
        :title="sidebarVisible ? '隐藏侧边栏' : '显示侧边栏'"
      >
        <span class="icon">{{ sidebarVisible ? '📁' : '📂' }}</span>
      </t-button>

      <t-breadcrumb separator="/" class="breadcrumb">
        <t-breadcrumb-item
          v-for="(item, index) in breadcrumbItems"
          :key="index"
          :to="item.path"
        >
          {{ item.title }}
        </t-breadcrumb-item>
      </t-breadcrumb>
    </div>
    
    <div class="top-bar-right">
      <t-button
        theme="primary"
        @click="openToolbox"
        :class="['toolbox-btn', { 'has-cache': toolboxStore.isInitialized }]"
        size="small"
        :title="toolboxButtonTitle"
      >
        <span class="icon">🔧</span>
        题库工具箱
        <span v-if="toolboxStore.isBackgroundRunning" class="running-indicator">●</span>
      </t-button>
      <t-button
        theme="default"
        @click="goToFeedback"
        class="feedback-btn"
        size="small"
      >
        <span class="icon">💬</span>
        反馈中心
      </t-button>
      <div class="current-time">{{ currentTime }}</div>
    </div>
  </div>

  <!-- 题库工具箱弹窗 -->
  <t-dialog
    v-model:visible="toolboxStore.dialogVisible"
    :header="dialogHeader"
    :width="dialogWidth"
    :top="dialogTop"
    :close-on-overlay-click="false"
    :destroy-on-close="false"
    class="toolbox-dialog"
    @close="handleDialogClosed"
  >
    <template #header>
      <div class="dialog-header">
        <span class="dialog-title">题库工具箱</span>
        <div class="dialog-status">
          <span v-if="toolboxStore.isBackgroundRunning" class="status-badge background">
            🔄 后台运行中
          </span>
          <span v-else-if="toolboxStore.isInitialized" class="status-badge cached">
            ✅ 已缓存
          </span>
          <span v-else class="status-badge loading">
            ⏳ 加载中
          </span>
          <t-button
            theme="default"
            size="small"
            @click="handleForceReload"
            :disabled="toolboxStore.shouldShowLoading"
            class="reload-btn"
            title="强制重新加载"
          >
            🔄
          </t-button>
        </div>
      </div>
    </template>
    
    <div class="toolbox-content" @click="handleContentClick">
      <div v-if="toolboxStore.shouldShowLoading" class="loading-container">
        <t-loading size="large" />
        <div class="loading-text">
          <p v-if="toolboxStore.isFirstLoad">正在加载题库工具箱...</p>
          <p v-else>正在恢复缓存状态...</p>
          <p class="loading-tip">
            {{ toolboxStore.isFirstLoad ? '首次加载可能需要几秒钟' : '即将恢复您的操作状态' }}
          </p>
          <div class="loading-stats" v-if="!toolboxStore.isFirstLoad">
            <small>会话时长: {{ formatDuration(toolboxStore.sessionDuration) }}</small>
          </div>
        </div>
      </div>
      <iframe
        v-show="toolboxStore.iframeLoaded"
        ref="toolboxIframe"
        :src="currentIframeSrc"
        frameborder="0"
        class="toolbox-iframe"
        @load="handleIframeLoad"
        @error="handleIframeError"
        allowfullscreen
        sandbox="allow-same-origin allow-scripts allow-popups allow-forms allow-downloads"
      ></iframe>
    </div>
    
    <template #footer>
      <div class="dialog-footer">
        <div class="footer-info">
          <small v-if="toolboxStore.operationHistory.length > 0">
            操作次数: {{ toolboxStore.operationHistory.length }} | 
            最后活跃: {{ formatLastActive() }}
          </small>
        </div>
        <div class="footer-actions">
          <t-button theme="default" size="small" @click="handleDialogClosed">
            最小化到后台
          </t-button>
        </div>
      </div>
    </template>
  </t-dialog>
</template>

<script>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
// TDesign icons removed for compatibility
import { useToolboxStore } from '../store/toolbox'

export default {
  name: 'TopBar',
  components: {
    // 图标组件已移除
  },
  props: {
    sidebarVisible: {
      type: Boolean,
      default: true
    }
  },
  emits: ['toggle-sidebar'],
  setup(props, { emit }) {
    const route = useRoute()
    const router = useRouter()
    const toolboxStore = useToolboxStore()
    const toolboxIframe = ref(null)
    const currentTime = ref('')
    const iframeSrc = ref('https://tikutools.nie1037.cn/')
    let timeInterval = null
    let loadingTimeout = null
    
    // 路由标题映射
    const routeTitleMap = {
      '/': '首页',
      '/practice': '刷题练习',
      '/practice-record': '刷题记录',
      '/exam': '模拟考试',
      '/exam-list': '考试记录',
      '/wrong-book': '错题本',
      '/ai-chat': 'AI助手',
      '/analytics': '数据分析',
      '/profile': '个人中心',
      '/resource-library': '资料库',
      '/feedback': '反馈中心',
      '/question-bank': '题库数据'
    }
    
    // 面包屑导航
    const breadcrumbItems = computed(() => {
      const path = route.path
      const items = [{ title: '首页', path: '/' }]
      
      if (path === '/') {
        return items
      }
      
      // 处理题库详情页面
      if (route.name === 'QuestionBankDetail') {
        // 添加题库数据页面
        items.push({ title: '题库数据', path: '/question-bank' })
        
        // 获取科目名称
        const subjectName = route.query.name || `科目${route.params.id}`
        items.push({ 
          title: `${subjectName}`, 
          path: route.path 
        })
        
        return items
      }
      
      // 处理考试详情页面
      if (route.name === 'ExamDetail') {
        items.push({ title: '模拟考试', path: '/exam' })
        items.push({ 
          title: `考试详情`, 
          path: route.path 
        })
        
        return items
      }
      
      // 处理普通路由
      const title = routeTitleMap[path] || '未知页面'
      items.push({ title, path })
      
      return items
    })
    
    // 切换侧边栏
    const toggleSidebar = () => {
      emit('toggle-sidebar')
    }
    
    // 响应式弹窗尺寸 - 进一步增大宽度
    const dialogWidth = computed(() => {
      const width = window.innerWidth
      if (width < 768) return '99%'      // 移动端：99%
      if (width < 1024) return '95%'     // 平板端：95%
      if (width < 1440) return '90%'     // 小桌面：90%
      if (width < 1920) return '85%'     // 中桌面：85%
      return '82%'                       // 大桌面：82%
    })

    const dialogTop = computed(() => {
      const height = window.innerHeight
      if (height < 600) return '2vh'     // 小屏幕：2vh
      if (height < 800) return '5vh'     // 中屏幕：5vh
      return '8vh'                       // 大屏幕：8vh
    })

    // 当前iframe源地址
    const currentIframeSrc = computed(() => {
      return toolboxStore.cachedState.url || iframeSrc.value
    })

    // 对话框标题
    const dialogHeader = computed(() => {
      const stats = toolboxStore.getOperationStats()
      return `题库工具箱 ${stats.isBackgroundRunning ? '(后台运行)' : ''}`
    })

    // 工具箱按钮标题
    const toolboxButtonTitle = computed(() => {
      if (toolboxStore.isBackgroundRunning) {
        return '题库工具箱正在后台运行，点击恢复窗口'
      } else if (toolboxStore.isInitialized) {
        return '题库工具箱已缓存，点击快速打开'
      } else {
        return '打开题库工具箱'
      }
    })

    // 打开工具箱
    const openToolbox = () => {
      console.log('🔧 TopBar.vue: 打开工具箱')
      toolboxStore.openToolbox()

      // 只有在首次加载或未初始化时才设置超时
      if (toolboxStore.isFirstLoad || !toolboxStore.isInitialized) {
        if (loadingTimeout) {
          clearTimeout(loadingTimeout)
        }
        loadingTimeout = setTimeout(() => {
          console.log('🔧 TopBar.vue: 加载超时，强制显示iframe')
          toolboxStore.setIframeLoaded(toolboxIframe.value)
        }, 15000) // 增加到15秒，给更多时间加载
      }
    }

    // 强制重新加载
    const handleForceReload = () => {
      console.log('🔧 TopBar.vue: 强制重新加载工具箱')
      toolboxStore.forceReload()
      
      // 重新设置iframe src以触发重新加载
      if (toolboxIframe.value) {
        toolboxIframe.value.src = iframeSrc.value + '?t=' + Date.now()
      }
      
      // 设置加载超时
      if (loadingTimeout) {
        clearTimeout(loadingTimeout)
      }
      loadingTimeout = setTimeout(() => {
        console.log('🔧 TopBar.vue: 强制重新加载超时')
        toolboxStore.setIframeLoaded(toolboxIframe.value)
      }, 15000)
    }

    // 内容点击处理（更新交互时间）
    const handleContentClick = () => {
      toolboxStore.updateInteractionTime()
    }

    // 跳转到反馈中心
    const goToFeedback = () => {
      router.push('/feedback')
    }

    // iframe加载完成
    const handleIframeLoad = () => {
      console.log('🔧 TopBar.vue: iframe加载完成')
      if (loadingTimeout) {
        clearTimeout(loadingTimeout)
        loadingTimeout = null
      }
      
      // 传递iframe元素引用给store
      toolboxStore.setIframeLoaded(toolboxIframe.value)
      
      // 添加iframe内容交互监听（如果可能）
      try {
        if (toolboxIframe.value && toolboxIframe.value.contentWindow) {
          // 尝试监听iframe内的点击事件（同源情况下）
          toolboxIframe.value.contentWindow.addEventListener('click', () => {
            toolboxStore.updateInteractionTime()
          })
          
          // 监听iframe内的滚动事件
          toolboxIframe.value.contentWindow.addEventListener('scroll', () => {
            toolboxStore.updateInteractionTime()
          })
        }
      } catch (e) {
        console.log('🔧 TopBar.vue: 无法监听iframe内部事件（跨域限制）')
      }
    }

    // iframe加载错误
    const handleIframeError = () => {
      console.error('🔧 TopBar.vue: iframe加载失败')
      if (loadingTimeout) {
        clearTimeout(loadingTimeout)
        loadingTimeout = null
      }
      // 即使加载失败也显示iframe，让用户看到错误页面
      toolboxStore.setIframeLoaded(toolboxIframe.value)
    }

    // 弹窗关闭处理（保持后台运行）
    const handleDialogClosed = () => {
      console.log('🔧 TopBar.vue: 最小化工具箱到后台')
      if (loadingTimeout) {
        clearTimeout(loadingTimeout)
        loadingTimeout = null
      }
      
      // 保存当前状态并关闭弹窗
      toolboxStore.closeToolbox()
    }

    // 格式化时长
    const formatDuration = (milliseconds) => {
      if (!milliseconds) return '0秒'
      
      const seconds = Math.floor(milliseconds / 1000)
      const minutes = Math.floor(seconds / 60)
      const hours = Math.floor(minutes / 60)
      
      if (hours > 0) {
        return `${hours}小时${minutes % 60}分钟`
      } else if (minutes > 0) {
        return `${minutes}分钟${seconds % 60}秒`
      } else {
        return `${seconds}秒`
      }
    }

    // 格式化最后活跃时间
    const formatLastActive = () => {
      if (!toolboxStore.lastInteractionTime) return '未知'
      
      const now = Date.now()
      const diff = now - toolboxStore.lastInteractionTime
      
      if (diff < 60000) return '刚刚'
      if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
      if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
      return `${Math.floor(diff / 86400000)}天前`
    }

    // 更新时间
    const updateTime = () => {
      const now = new Date()
      currentTime.value = now.toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit'
      })
    }

    onMounted(() => {
      updateTime()
      timeInterval = setInterval(updateTime, 1000)
    })

    onUnmounted(() => {
      if (timeInterval) {
        clearInterval(timeInterval)
      }
    })

    return {
      breadcrumbItems,
      currentTime,
      toggleSidebar,
      toolboxStore,
      toolboxIframe,
      iframeSrc,
      currentIframeSrc,
      dialogHeader,
      toolboxButtonTitle,
      dialogWidth,
      dialogTop,
      openToolbox,
      handleForceReload,
      handleContentClick,
      handleIframeLoad,
      handleIframeError,
      handleDialogClosed,
      goToFeedback,
      formatDuration,
      formatLastActive
    }
  }
}
</script>

<style scoped>
.top-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 50px;
  padding: 0 20px;
  background: white;
  border-bottom: 1px solid #e4e7ed;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.1);
  position: sticky;
  top: 0;
  z-index: 999;
}

.top-bar-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.sidebar-toggle {
  padding: 8px;
  border-radius: 4px;
  transition: all 0.3s;
}

.sidebar-toggle:hover {
  background-color: #f5f7fa;
}

.sidebar-toggle .t-icon {
  font-size: 18px;
  color: #606266;
}

.breadcrumb {
  font-size: 14px;
}

.breadcrumb :deep(.t-breadcrumb__item) {
  font-weight: 500;
}

.breadcrumb :deep(.t-breadcrumb__item:last-child .t-breadcrumb__inner) {
  color: #409eff;
}

.top-bar-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.current-time {
  font-size: 14px;
  color: #606266;
  font-family: 'Courier New', monospace;
}

.toolbox-btn {
  margin-right: 8px;
  font-weight: 500;
}

.toolbox-btn .t-icon {
  margin-right: 4px;
}

.feedback-btn {
  margin-right: 16px;
  font-weight: 500;
}

.feedback-btn .t-icon {
  margin-right: 4px;
}

/* 工具箱弹窗样式 */
.toolbox-dialog {
  border-radius: 8px;
  overflow: hidden;
}

.toolbox-dialog :deep(.t-dialog__header) {
  padding: 12px 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.dialog-title {
  font-size: 16px;
  font-weight: 600;
}

.dialog-status {
  display: flex;
  align-items: center;
  gap: 8px;
}

.status-badge {
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
  white-space: nowrap;
}

.status-badge.background {
  background: rgba(255, 193, 7, 0.2);
  color: #ffc107;
  border: 1px solid rgba(255, 193, 7, 0.3);
}

.status-badge.cached {
  background: rgba(40, 167, 69, 0.2);
  color: #28a745;
  border: 1px solid rgba(40, 167, 69, 0.3);
}

.status-badge.loading {
  background: rgba(0, 123, 255, 0.2);
  color: #007bff;
  border: 1px solid rgba(0, 123, 255, 0.3);
}

.reload-btn {
  padding: 4px 8px !important;
  min-width: auto !important;
  height: 24px !important;
  font-size: 12px;
  border: 1px solid rgba(255, 255, 255, 0.3) !important;
  background: rgba(255, 255, 255, 0.1) !important;
  color: white !important;
}

.reload-btn:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.2) !important;
  border-color: rgba(255, 255, 255, 0.5) !important;
}

.reload-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.toolbox-content {
  height: 75vh;
  min-height: 450px;
  max-height: 80vh;
  position: relative;
  cursor: pointer;
}

.toolbox-iframe {
  width: 100%;
  height: 100%;
  border: none;
  border-radius: 4px;
  background: white;
  transition: opacity 0.3s ease;
}

.toolbox-iframe:not([src]) {
  opacity: 0;
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #909399;
  background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
  border-radius: 4px;
  position: relative;
}

.loading-container::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: url('data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><defs><pattern id="grid" width="10" height="10" patternUnits="userSpaceOnUse"><path d="M 10 0 L 0 0 0 10" fill="none" stroke="%23e0e0e0" stroke-width="0.5"/></pattern></defs><rect width="100" height="100" fill="url(%23grid)"/></svg>');
  opacity: 0.3;
  border-radius: 4px;
}

.loading-text {
  text-align: center;
  margin-top: 16px;
  position: relative;
  z-index: 1;
}

.loading-text p {
  margin: 8px 0;
  font-size: 14px;
  font-weight: 500;
}

.loading-tip {
  color: #6c757d;
  font-size: 12px;
  font-style: italic;
}

.loading-stats {
  margin-top: 12px;
  padding: 8px 12px;
  background: rgba(255, 255, 255, 0.8);
  border-radius: 6px;
  border: 1px solid #e9ecef;
}

.loading-stats small {
  color: #495057;
  font-weight: 500;
}

.dialog-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0 0 0;
  border-top: 1px solid #e9ecef;
  margin-top: 16px;
}

.footer-info {
  flex: 1;
}

.footer-info small {
  color: #6c757d;
  font-size: 12px;
}

.footer-actions {
  display: flex;
  gap: 8px;
}

.loading-icon {
  font-size: 24px;
  margin-bottom: 8px;
  animation: rotate 1s linear infinite;
}

@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* 工具箱按钮增强样式 */
.toolbox-btn {
  position: relative;
  overflow: hidden;
}

.toolbox-btn::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent);
  transition: left 0.5s;
}

.toolbox-btn:hover::before {
  left: 100%;
}

/* 缓存状态指示器 */
.toolbox-btn::after {
  content: '';
  position: absolute;
  top: 2px;
  right: 2px;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #28a745;
  opacity: 0;
  transition: opacity 0.3s;
}

.toolbox-btn.has-cache::after {
  opacity: 1;
}

/* 后台运行指示器 */
.running-indicator {
  display: inline-block;
  margin-left: 6px;
  color: #28a745;
  font-size: 8px;
  animation: pulse 2s infinite;
  vertical-align: middle;
}

@keyframes pulse {
  0% { opacity: 1; }
  50% { opacity: 0.5; }
  100% { opacity: 1; }
}

/* 工具箱按钮状态样式 */
.toolbox-btn.has-cache {
  background: linear-gradient(135deg, #28a745 0%, #20c997 100%);
  border-color: #28a745;
}

.toolbox-btn.has-cache:hover {
  background: linear-gradient(135deg, #218838 0%, #1ea085 100%);
  border-color: #1e7e34;
  transform: translateY(-1px);
  box-shadow: 0 4px 8px rgba(40, 167, 69, 0.3);
}

/* 响应式优化 */
@media (max-width: 768px) {
  .running-indicator {
    margin-left: 4px;
    font-size: 6px;
  }
  
  .toolbox-btn .running-indicator {
    display: none;
  }
  
  .toolbox-btn.has-cache::after {
    width: 8px;
    height: 8px;
    top: 1px;
    right: 1px;
  }
}

/* 响应式设计 */
@media (max-width: 768px) {
  .top-bar {
    padding: 0 12px;
  }
  
  .top-bar-left {
    gap: 12px;
  }
  
  .toolbox-btn {
    margin-right: 8px;
    padding: 6px 12px;
    font-size: 12px;
  }
  
  .current-time {
    display: none;
  }
  
  .toolbox-content {
    height: 78vh;
    min-height: 380px;
    max-height: 83vh;
  }
  
  .dialog-header {
    flex-direction: column;
    gap: 8px;
    align-items: flex-start;
  }
  
  .dialog-status {
    width: 100%;
    justify-content: space-between;
  }
}

@media (max-width: 480px) {
  .toolbox-btn {
    margin-right: 4px;
    padding: 4px 8px;
  }
  
  .toolbox-btn .t-button__text {
    display: none;
  }
  
  .toolbox-btn .t-icon {
    margin-right: 0;
  }
  
  .toolbox-content {
    height: 80vh;
    min-height: 320px;
    max-height: 85vh;
  }
  
  .status-badge {
    font-size: 10px;
    padding: 1px 6px;
  }
  
  .reload-btn {
    padding: 2px 6px !important;
    height: 20px !important;
    font-size: 10px;
  }
}

/* 大屏幕优化 */
@media (min-width: 1920px) {
  .toolbox-content {
    height: 77vh;
    min-height: 550px;
    max-height: 82vh;
  }
  
  .dialog-header {
    padding: 0 4px;
  }
  
  .dialog-title {
    font-size: 18px;
  }
  
  .status-badge {
    font-size: 13px;
    padding: 3px 10px;
  }
}

/* 超宽屏优化 */
@media (min-width: 2560px) {
  .toolbox-content {
    height: 78vh;
    min-height: 600px;
    max-height: 83vh;
  }
}
</style>