<template>
  <div id="app">
    <!-- 添加全局加载状态 -->
    <div v-if="appLoading" class="app-loading">
      <t-loading text="加载中..." />
    </div>
    
    <!-- 错误状态显示 -->
    <div v-else-if="initError" class="app-error">
      <div class="error-content">
        <div class="error-icon">⚠️</div>
        <h3>应用加载出错</h3>
        <p>{{ initError }}</p>
        <t-button theme="primary" @click="location.reload()">刷新页面</t-button>
      </div>
    </div>
    
    <!-- 应用内容 -->
    <div v-else>
      <!-- 登录页面直接显示，不包含侧边栏 -->
      <template v-if="isLoginPage">
        <router-view v-slot="{ Component }">
          <!-- <transition name="fade" mode="out-in"> -->
            <component :is="Component" />
          <!-- </transition> -->
        </router-view>
      </template>
      
      <!-- 主应用布局 -->
      <template v-else>
        <div class="app-layout">
          <!-- 侧边栏 -->
          <AppHeader 
            :class="{ 'sidebar-hidden': !sidebarVisible }" 
            @close-sidebar="toggleSidebar"
          />
          
          <!-- 主要内容区域 -->
          <div class="main-wrapper" :class="{ 'sidebar-hidden': !sidebarVisible }">
            <!-- 顶栏 -->
            <TopBar :sidebar-visible="sidebarVisible" @toggle-sidebar="toggleSidebar" />
            
            <!-- 主内容区域 - 应用过渡动画 -->
            <main class="main-content">
              <router-view v-slot="{ Component }">
              <transition name="fade" mode="out-in">
                <component :is="Component" />
              </transition>
            </router-view>
            </main>
          </div>
        </div>
      </template>
    </div>
    
    <!-- 页面底部信息 (排除登录页面) -->
    <footer v-if="!isLoginPage" class="app-footer">
      <div class="footer-content">
        <div class="footer-info">
          <span v-if="systemConfig.copyrightInfo" class="copyright">
            {{ systemConfig.copyrightInfo }}
          </span>
          <span v-if="systemConfig.icpNumber" class="icp">
            <a v-if="systemConfig.icpUrl" :href="systemConfig.icpUrl" target="_blank" rel="noopener noreferrer" class="icp-link">
              {{ systemConfig.icpNumber }}
            </a>
            <span v-else>
              {{ systemConfig.icpNumber }}
            </span>
          </span>
          <span v-if="systemConfig.policeNumber" class="police">
            <img v-if="systemConfig.policeIcon" :src="systemConfig.policeIcon" alt="公安备案" class="police-icon" />
            <a v-if="systemConfig.policeUrl" :href="systemConfig.policeUrl" target="_blank" rel="noopener noreferrer" class="police-link">
              {{ systemConfig.policeNumber }}
            </a>
            <span v-else>
              {{ systemConfig.policeNumber }}
            </span>
          </span>
        </div>
      </div>
    </footer>
  </div>
</template>

<script>
import { reactive, computed, ref, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
// 使用TDesign内置图标
import AppHeader from './components/AppHeader.vue'
import TopBar from './components/TopBar.vue'
import { publicAPI } from './api'

export default {
  name: 'App',
  components: {
    AppHeader,
    TopBar
  },
  setup() {
    console.log('App.vue: setup()开始执行')
    const route = useRoute()
    const sidebarVisible = ref(true)
    const appLoading = ref(false) // 立即显示页面，不显示加载状态
    const routeReady = ref(true) // 立即设置路由准备就绪
    const initError = ref(null)
    
    const systemConfig = reactive({
      copyrightInfo: '',
      icpNumber: '',
      icpUrl: '',
      policeNumber: '',
      policeIcon: '',
      policeUrl: ''
    })
    
    // 获取系统公共配置
    const loadPublicConfig = async () => {
      try {
        console.log('App.vue: 开始加载公共配置')
        const config = await publicAPI.getPublicConfig()
        console.log('App.vue: 获取到公共配置:', config)
        
        // 映射后端返回的配置到前端使用的字段
        Object.assign(systemConfig, {
          copyrightInfo: config.copyright || '© 2025 七洛题库 保留所有权利',
          icpNumber: config.icpBeian || '',
          icpUrl: config.icpBeianUrl || 'http://beian.miit.gov.cn/',
          policeNumber: config.policeBeian || '',
          policeIcon: config.policeBeianIcon || '',
          policeUrl: config.policeBeianUrl || ''
        })
        
        console.log('App.vue: 系统配置已更新:', systemConfig)
      } catch (error) {
        console.error('App.vue: 获取公共配置失败:', error)
        // 使用默认配置
        Object.assign(systemConfig, {
          copyrightInfo: '© 2025 七洛题库 保留所有权利',
          icpNumber: '',
          icpUrl: '',
          policeNumber: '',
          policeIcon: '',
          policeUrl: ''
        })
      }
    }
    
    console.log('App.vue: 初始化状态完成', {
      sidebarVisible: sidebarVisible.value,
      appLoading: appLoading.value,
      routeReady: routeReady.value
    })
    
    // 判断是否为登录页面 - 修复竞态条件问题
    const isLoginPage = computed(() => {
      try {
        // 始终优先使用当前URL路径，避免路由状态不一致
        const currentPath = window.location.pathname
        const result = currentPath === '/login' || currentPath === '/register'
        console.log('App.vue: isLoginPage计算结果:', result, '当前路径:', currentPath, 'route.path:', route.path)
        return result
      } catch (error) {
        console.error('App.vue: isLoginPage计算出错:', error)
        // 降级处理，使用URL判断
        const fallbackResult = window.location.pathname === '/login' || window.location.pathname === '/register'
        console.log('App.vue: isLoginPage降级处理结果:', fallbackResult)
        return fallbackResult
      }
    })
    
    // 切换侧边栏
     const toggleSidebar = () => {
       sidebarVisible.value = !sidebarVisible.value
     }
     


     
     // 处理窗口大小变化
     const handleResize = () => {
       const isMobile = window.innerWidth <= 768
       sidebarVisible.value = !isMobile
     }
     
     onMounted(async () => {
      console.log('App.vue: 组件已挂载')
      console.log('App.vue: 当前URL:', window.location.href)
      console.log('App.vue: 当前pathname:', window.location.pathname)

      // 立即设置路由准备状态和清除加载状态
      routeReady.value = true
      appLoading.value = false

      // 加载系统公共配置
      await loadPublicConfig()

      // 处理窗口大小
      handleResize()
      window.addEventListener('resize', handleResize)
      console.log('App.vue: 初始化完成')
    })
     
     onUnmounted(() => {
       window.removeEventListener('resize', handleResize)
     })
     
     return {
      appLoading,
      routeReady,
      isLoginPage,
      sidebarVisible,
      systemConfig,
      toggleSidebar,
      initError
    } 
   }
}
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

#app {
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', '微软雅黑', Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  height: 100vh;
  background-color: #f5f7fa;
}

body {
  margin: 0;
  padding: 0;
}

/* TDesign 下拉框样式修复 */
.t-select-dropdown,
.practice-select-dropdown,
.custom-select-dropdown,
.test-select-dropdown,
.exam-select-dropdown,
.analytics-select-dropdown,
.profile-select-dropdown,
.wrong-book-select-dropdown,
.practice-record-select-dropdown,
.exam-list-select-dropdown,
.feedback-select-dropdown {
  z-index: 99999 !important;
  position: absolute !important;
  background-color: #fff !important;
  border: 1px solid #d9d9d9 !important;
  border-radius: 6px !important;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1) !important;
}

/* 确保下拉菜单选项可见 */
.el-select-dropdown__item {
  padding: 0 20px !important;
  position: relative !important;
  white-space: nowrap !important;
  overflow: hidden !important;
  text-overflow: ellipsis !important;
  color: #606266 !important;
  height: 34px !important;
  line-height: 34px !important;
  box-sizing: border-box !important;
  cursor: pointer !important;
  background-color: #fff !important;
}

.el-select-dropdown__item:hover {
  background-color: #f5f7fa !important;
}

.el-select-dropdown__item.selected {
  color: #409eff !important;
  font-weight: bold !important;
}

/* 确保下拉菜单在所有情况下都有最高层级 */
.el-select-dropdown,
.el-select-dropdown__wrap,
.el-scrollbar,
.el-select-dropdown__list {
  z-index: 99999 !important;
}

/* TDesign 组件层级修复 */
.t-select {
  position: relative !important;
  z-index: 1 !important;
}

.t-select.is-focus {
  z-index: 1000 !important;
}

/* 确保下拉菜单不被其他元素遮挡 */
.t-card,
.t-form,
.t-form-item,
.content-wrapper {
  position: relative !important;
  z-index: auto !important;
}

/* 特殊处理可能有高z-index的元素 */
.t-header,
.t-aside {
  z-index: auto !important;
}

/* 修复主内容区域的层级 */
.main-content {
  z-index: auto !important;
}

.page-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.main-content {
  flex: 1;
  padding: 20px;
}

/* 底部信息样式 */
.app-footer {
  background-color: #f8f9fa;
  border-top: 1px solid #e9ecef;
  padding: 15px 0;
  margin-top: auto;
  font-size: 12px;
  color: #6c757d;
}

.footer-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
}

.footer-info {
  text-align: center;
  line-height: 1.5;
}

.footer-info span {
  margin: 0 10px;
  display: inline-block;
}

.footer-info .copyright {
  font-weight: 500;
}

.footer-info .icp,
.footer-info .police {
  color: #868e96;
}

.footer-info .icp-link {
  color: #868e96;
  text-decoration: none;
}

.footer-info .icp-link:hover {
  color: #495057;
  text-decoration: underline;
}

.footer-info .police-icon {
  width: 16px;
  height: 16px;
  vertical-align: middle;
  margin-right: 4px;
}

.footer-info .police-link {
  color: #868e96;
  text-decoration: none;
}

.footer-info .police-link:hover {
  color: #495057;
  text-decoration: underline;
}

@media (max-width: 768px) {
  .footer-info span {
    display: block;
    margin: 5px 0;
  }
}

/* 应用布局 */
.app-layout {
  display: flex;
  height: 100vh;
}

/* 主内容区域 */
.main-wrapper {
  flex: 1;
  margin-left: 250px;
  display: flex;
  flex-direction: column;
  transition: margin-left 0.3s ease;
}

.main-wrapper.sidebar-hidden {
  margin-left: 0;
}

.main-content {
  flex: 1;
  padding: 0;
  overflow-y: auto;
  background-color: #f5f7fa;
  transition: all 0.3s ease;
  min-height: calc(100vh - 60px);
  overflow-x: auto;
}

/* 渐入渐出过渡动画 - 已禁用 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}


/* 响应式设计 */
@media (max-width: 768px) {
  .app-layout {
    flex-direction: column;
  }
  
  .main-wrapper {
    margin-left: 0;
    width: 100%;
  }
  
  .main-wrapper.sidebar-hidden {
    margin-left: 0;
  }
  
  .sidebar {
    position: fixed;
    z-index: 1001;
    transform: translateX(-100%);
    transition: transform 0.3s ease;
  }
  
  .sidebar:not(.sidebar-hidden) {
    transform: translateX(0);
  }
  
  .main-content {
    padding: 10px;
  }
}

@media (max-width: 480px) {
  .main-content {
    padding: 5px;
  }
}

/* 禁用ResizeObserver相关警告 */
body > div:last-child {
  display: none !important;
}

/* 隐藏webpack-dev-server的overlay */
body > iframe {
  display: none !important;
}

/* 全局加载状态样式 */
.app-loading {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background: #f5f5f5;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
}

.app-loading .t-loading-mask {
  background: rgba(245, 245, 245, 0.9);
}

/* 错误状态样式 */
.app-error {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background: #f5f5f5;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
}

.error-content {
  text-align: center;
  padding: 40px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  max-width: 400px;
}

.error-icon {
  font-size: 48px;
  color: #f56c6c;
  margin-bottom: 16px;
}

.error-content h3 {
  color: #303133;
  margin-bottom: 12px;
  font-size: 18px;
}

.error-content p {
  color: #606266;
  margin-bottom: 24px;
  line-height: 1.5;
}
</style>
