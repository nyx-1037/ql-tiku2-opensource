import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import pinia from './store'

// 导入TDesign Vue Next
import TDesign from 'tdesign-vue-next'
import 'tdesign-vue-next/es/style/index.css'

// 导入TDesign样式修复
import './styles/tdesign-fixes.css'

// 导入响应式布局修复
import './styles/responsive-layout.css'

// 导入自定义组件
import CustomComponents from './components'

// 导入缓存插件
import cachePlugin, { createCacheRouterGuard } from './plugins/cachePlugin'
import cacheWarmup from './utils/cacheWarmup'

// 白屏检测和修复机制
let whiteScreenTimeout = null
let appMounted = false

// 创建白屏检测元素
const createWhiteScreenDetector = () => {
  const detector = document.createElement('div')
  detector.id = 'white-screen-detector'
  detector.style.cssText = `
    position: fixed;
    top: 0;
    left: 0;
    width: 100vw;
    height: 100vh;
    background: #f5f7fa;
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 10000;
    font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', sans-serif;
  `
  detector.innerHTML = `
    <div style="text-align: center; padding: 20px;">
      <div style="font-size: 48px; margin-bottom: 20px;">🔄</div>
      <h3 style="color: #333; margin-bottom: 10px;">应用加载中...</h3>
      <p style="color: #666; margin-bottom: 20px;">如果长时间无响应，请尝试刷新页面</p>
      <button onclick="location.reload()" style="
        background: #409eff;
        color: white;
        border: none;
        padding: 10px 20px;
        border-radius: 4px;
        cursor: pointer;
        font-size: 14px;
      ">刷新页面</button>
    </div>
  `
  return detector
}

// 移除白屏检测器
const removeWhiteScreenDetector = () => {
  const detector = document.getElementById('white-screen-detector')
  if (detector) {
    detector.remove()
  }
  if (whiteScreenTimeout) {
    clearTimeout(whiteScreenTimeout)
  }
}

// 添加白屏检测器到页面
const whiteScreenDetector = createWhiteScreenDetector()
document.body.appendChild(whiteScreenDetector)

// 设置白屏超时检测（10秒）
whiteScreenTimeout = setTimeout(() => {
  if (!appMounted) {
    console.error('白屏检测：应用加载超时，显示错误提示')
    const detector = document.getElementById('white-screen-detector')
    if (detector) {
      detector.innerHTML = `
        <div style="text-align: center; padding: 20px;">
          <div style="font-size: 48px; margin-bottom: 20px;">⚠️</div>
          <h3 style="color: #333; margin-bottom: 10px;">应用加载失败</h3>
          <p style="color: #666; margin-bottom: 20px;">可能是网络问题或浏览器兼容性问题</p>
          <button onclick="location.reload()" style="
            background: #f56c6c;
            color: white;
            border: none;
            padding: 10px 20px;
            border-radius: 4px;
            cursor: pointer;
            font-size: 14px;
            margin-right: 10px;
          ">重试</button>
          <button onclick="window.open('/', '_blank')" style="
            background: #67c23a;
            color: white;
            border: none;
            padding: 10px 20px;
            border-radius: 4px;
            cursor: pointer;
            font-size: 14px;
          ">新窗口打开</button>
        </div>
      `
    }
  }
}, 10000)

// 全局错误处理 - 增强版本
const globalErrorHandler = (event) => {
  console.error('全局错误捕获:', {
    error: event.error,
    message: event.message,
    filename: event.filename,
    lineno: event.lineno,
    colno: event.colno
  })
  
  // 如果是Pinia相关错误，尝试恢复
  if (event.error && event.error.message && event.error.message.includes('pinia')) {
    console.warn('检测到Pinia相关错误，尝试恢复...')
    // 可以尝试重新加载页面
    setTimeout(() => {
      if (!appMounted) {
        location.reload()
      }
    }, 2000)
  }
}

const globalRejectionHandler = (event) => {
  console.error('未处理的Promise拒绝:', {
    reason: event.reason,
    promise: event.promise
  })
  
  // 处理可能的异步加载错误
  if (event.reason && event.reason.message && event.reason.message.includes('import')) {
    console.warn('检测到模块加载错误，可能是网络问题')
  }
}

window.addEventListener('error', globalErrorHandler)
window.addEventListener('unhandledrejection', globalRejectionHandler)

console.log('=== Vue应用启动 ===')
console.log('main.js: 开始创建Vue应用')

const app = createApp(App)

// 添加全局错误处理
app.config.errorHandler = (err, instance, info) => {
  // 过滤掉TDesign组件内部的已知无害错误
  if (err.message && err.message.includes("Cannot use 'in' operator to search for 'key'")) {
    console.warn('TDesign组件内部错误（已知问题，不影响功能）:', err.message)
    return
  }

  console.error('Vue全局错误:', {
    error: err,
    component: instance?.$options?.name,
    info: info,
    stack: err.stack
  })

  // 关键错误处理
  if (err.message && (
    err.message.includes('Cannot read properties') ||
    err.message.includes('undefined') ||
    err.message.includes('null')
  )) {
    console.warn('检测到可能的初始化错误，尝试延迟重载')
    setTimeout(() => {
      if (!appMounted) {
        location.reload()
      }
    }, 1000)
  }
}

// 设置警告处理
app.config.warnHandler = (msg, instance, trace) => {
  console.warn('Vue警告:', msg, trace)
}

console.log('main.js: Vue应用创建完成')

// 关键：确保Pinia在路由之前注册，避免时序问题
try {
  app.use(pinia)
  console.log('main.js: Pinia状态管理注册完成')
} catch (error) {
  console.error('Pinia注册失败:', error)
  // 如果Pinia注册失败，显示错误提示
  document.getElementById('white-screen-detector').innerHTML = `
    <div style="text-align: center; padding: 20px;">
      <div style="font-size: 48px; margin-bottom: 20px;">❌</div>
      <h3 style="color: #333; margin-bottom: 10px;">应用初始化失败</h3>
      <p style="color: #666; margin-bottom: 20px;">${error.message}</p>
      <button onclick="location.reload()" style="
        background: #409eff;
        color: white;
        border: none;
        padding: 10px 20px;
        border-radius: 4px;
        cursor: pointer;
        font-size: 14px;
      ">重试</button>
    </div>
  `
  throw error
}

// 注册TDesign Vue Next
app.use(TDesign)
console.log('main.js: TDesign Vue Next注册完成')

// 注册自定义组件（包括替换的下拉框）
app.use(CustomComponents)
console.log('main.js: 自定义组件注册完成')

// 注册路由
app.use(router)
console.log('main.js: Vue Router注册完成')

// 注册缓存插件
app.use(cachePlugin)
console.log('main.js: 缓存插件注册完成')

// 添加路由缓存守卫
router.beforeEach(createCacheRouterGuard())
console.log('main.js: 路由缓存守卫注册完成')

// 监听路由错误
router.onError((error) => {
  console.error('Router错误:', error)
  if (error.message.includes('Failed to fetch')) {
    console.warn('检测到路由加载失败，可能是网络问题')
  }
})

// 应用挂载完成后的处理
app.config.globalProperties.$appReady = false

// 初始化缓存预热
const initCacheWarmup = async () => {
  try {
    // 应用启动预热
    await cacheWarmup.onAppStart()
    
    // 监听路由变化，执行页面预热
    router.afterEach((to) => {
      const pageName = to.name || to.path.replace('/', '')
      if (pageName) {
        // 延迟执行，避免阻塞页面渲染
        setTimeout(() => {
          cacheWarmup.onPageVisit(pageName)
        }, 500)
      }
    })
    
    // 监听用户登录状态变化
    const authStore = pinia._s.get('auth')
    if (authStore) {
      authStore.$subscribe((mutation, state) => {
        if (state.isAuthenticated && mutation.type === 'direct') {
          // 用户登录后预热
          setTimeout(() => {
            cacheWarmup.onUserLogin()
          }, 1000)
        }
      })
    }
    
  } catch (error) {
    console.error('缓存预热初始化失败:', error)
  }
}

// 延迟挂载，确保所有依赖都准备好
setTimeout(() => {
  try {
    app.mount('#app')
    appMounted = true
    removeWhiteScreenDetector()
    app.config.globalProperties.$appReady = true
    console.log('main.js: 应用挂载完成')
    
    // 初始化缓存预热
    initCacheWarmup()
    
    
  } catch (error) {
    console.error('应用挂载失败:', error)
    document.getElementById('white-screen-detector').innerHTML = `
      <div style="text-align: center; padding: 20px;">
        <div style="font-size: 48px; margin-bottom: 20px;">💥</div>
        <h3 style="color: #333; margin-bottom: 10px;">应用启动失败</h3>
        <p style="color: #666; margin-bottom: 20px;">${error.message}</p>
        <button onclick="location.reload()" style="
          background: #f56c6c;
          color: white;
          border: none;
          padding: 10px 20px;
          border-radius: 4px;
          cursor: pointer;
          font-size: 14px;
        ">重试</button>
      </div>
    `
  }
}, 100)
