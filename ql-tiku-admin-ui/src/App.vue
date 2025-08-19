<template>
  <div id="app">
    <!-- 登录页面直接显示，不包含侧边栏 -->
    <template v-if="isLoginPage">
      <transition name="fade" mode="out-in">
        <router-view />
      </transition>
    </template>
    
    <!-- 主应用布局 -->
    <template v-else>
      <div class="app-layout">
        <!-- 侧边栏和主内容区域 -->
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </div>
    </template>
    
    <!-- 底部版权信息 -->
    <footer v-if="!isLoginPage" class="app-footer">
      <div class="footer-content">
        <div class="footer-info">
          <span v-if="systemStore.siteConfig.copyright" class="copyright">
            {{ systemStore.siteConfig.copyright }}
          </span>
          <span v-if="systemStore.siteConfig.icpNumber" class="icp">
            <a v-if="systemStore.siteConfig.icpUrl" :href="systemStore.siteConfig.icpUrl" target="_blank" rel="noopener noreferrer" class="icp-link">
              {{ systemStore.siteConfig.icpNumber }}
            </a>
            <span v-else>
              {{ systemStore.siteConfig.icpNumber }}
            </span>
          </span>
          <span v-if="systemStore.siteConfig.policeNumber" class="police">
            <img v-if="systemStore.siteConfig.policeIcon" :src="systemStore.siteConfig.policeIcon" alt="公安备案" class="police-icon" />
            <a v-if="systemStore.siteConfig.policeUrl" :href="systemStore.siteConfig.policeUrl" target="_blank" rel="noopener noreferrer" class="police-link">
              {{ systemStore.siteConfig.policeNumber }}
            </a>
            <span v-else>
              {{ systemStore.siteConfig.policeNumber }}
            </span>
          </span>
        </div>
      </div>
    </footer>
  </div>
</template>

<script>
import { onMounted, watch, computed } from 'vue'
import { useRoute } from 'vue-router'
import { useSystemStore } from './store/system'
import { useAuthStore } from './store/auth'

export default {
  name: 'App',
  setup() {
    const route = useRoute()
    const systemStore = useSystemStore()
    const authStore = useAuthStore()
    
    // 判断是否为登录页面
    const isLoginPage = computed(() => {
      return route.path === '/login'
    })
    
    // 获取系统配置并设置页面标题
    const updatePageTitle = () => {
      const siteName = systemStore.siteConfig.siteName || '管理后台'
      
      // 根据路由设置不同的页面标题
      let pageTitle = siteName
      if (route.meta?.title) {
        pageTitle = `${route.meta.title} - ${siteName}`
      } else if (route.name) {
        const routeTitles = {
          'Login': '登录',
          'Dashboard': '仪表盘',
          'QuestionManagement': '题目管理',
          'SubjectManagement': '科目管理',
          'UserManagement': '用户管理',
          'ExamManagement': '考试管理',
          'SystemSettings': '系统设置'
        }
        const routeTitle = routeTitles[route.name]
        if (routeTitle) {
          pageTitle = `${routeTitle} - ${siteName}`
        }
      }
      
      document.title = pageTitle
    }
    
    // 监听路由变化
    watch(route, () => {
      updatePageTitle()
    }, { immediate: true })
    
    // 监听系统配置变化
    watch(() => systemStore.siteConfig.siteName, () => {
      updatePageTitle()
    })
    
    onMounted(async () => {
      // 初始化认证状态
      authStore.initAuth()
      
      // 加载系统配置
      await systemStore.loadSystemConfig()
      
      // 更新页面标题
      updatePageTitle()
    })
    
    return {
      systemStore,
      authStore,
      isLoginPage
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
  background-color: #f0f2f5;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

body {
  margin: 0;
  padding: 0;
}

.app-content {
  flex: 1;
}

/* 底部版权信息样式 - 自适应高度 */
.app-footer {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background-color: #f8f9fa;
  border-top: 1px solid #e9ecef;
  padding: 12px 0;
  z-index: 100;
  box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.1);
  min-height: auto;
  height: auto;
}

.footer-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
}

.footer-info {
  display: flex;
  justify-content: center;
  align-items: center;
  flex-wrap: wrap;
  gap: 20px;
  font-size: 13px;
  color: #6c757d;
}

.footer-info span {
  display: flex;
  align-items: center;
  gap: 4px;
}

.icp-link,
.police-link {
  color: #6c757d;
  text-decoration: none;
  transition: color 0.3s ease;
}

.icp-link:hover,
.police-link:hover {
  color: #007bff;
  text-decoration: underline;
}

.police-icon {
  width: 14px;
  height: 14px;
  margin-right: 4px;
}

/* 为主要内容添加底部边距，避免被浮动底部遮挡 */
body {
  padding-bottom: 70px; /* 增加一些空间以适应自适应高度 */
}

/* 响应式设计 */
@media (max-width: 768px) {
  .footer-info {
    flex-direction: column;
    gap: 6px;
    text-align: center;
    padding: 0 10px;
  }
  
  .footer-info span {
    justify-content: center;
    font-size: 12px;
    line-height: 1.4;
  }
  
  .app-footer {
    padding: 8px 0;
    min-height: auto;
  }
  
  .footer-content {
    padding: 0 15px;
  }
  
  body {
    padding-bottom: 90px; /* 移动端需要更多空间 */
  }
  
  .police-icon {
    width: 12px;
    height: 12px;
    margin-right: 3px;
  }
}

/* 超小屏幕优化 */
@media (max-width: 480px) {
  .footer-info {
    gap: 4px;
  }
  
  .footer-info span {
    font-size: 11px;
  }
  
  .app-footer {
    padding: 6px 0;
  }
  
  .footer-content {
    padding: 0 10px;
  }
  
  body {
    padding-bottom: 100px;
  }
}

.page-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.main-content {
  flex: 1;
  padding: 20px;
  background-color: #f0f2f5;
}

/* 应用布局 */
.app-layout {
  height: 100vh;
  overflow: hidden;
}

/* 渐入渐出过渡动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
