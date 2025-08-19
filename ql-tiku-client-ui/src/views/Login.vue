<template>
  <div v-if="!pageLoading" class="login-container">
    <div class="content-wrapper">
      <div class="login-box">
      <div class="login-header">
        <img v-if="systemConfig.siteLogo" :src="systemConfig.siteLogo" alt="Logo" class="login-logo" />
        <h2>{{ systemConfig.siteName || '在线刷题系统' }}</h2>
        <p>{{ systemConfig.description || '欢迎登录' }}</p>
      </div>
      
      <t-form
        ref="loginFormRef"
        :data="loginForm"
        :rules="loginRules"
        class="login-form"
        @submit.prevent="handleLogin"
      >
        <t-form-item name="username">
          <t-input
            v-model="loginForm.username"
            placeholder="👤 请输入用户名"
            size="large"
            :disabled="systemConfig.maintenanceMode"
          />
        </t-form-item>

        <t-form-item name="password">
          <t-input
            v-model="loginForm.password"
            type="password"
            placeholder="🔒 请输入密码"
            size="large"
            :disabled="systemConfig.maintenanceMode"
          />
        </t-form-item>
        
        <!-- 验证码 -->
        <t-form-item v-if="captchaConfig.enabled" name="captchaCode">
          <div class="captcha-container">
            <t-input
              v-model="loginForm.captchaCode"
              placeholder="🖼️ 请输入验证码"
              size="large"
              style="flex: 1; margin-right: 10px;"
              :disabled="systemConfig.maintenanceMode"
            />
            <div class="captcha-image" @click="refreshCaptcha">
              <img v-if="captchaImage" :src="captchaImage" alt="验证码" />
              <span v-else class="captcha-loading">加载中...</span>
            </div>
          </div>
        </t-form-item>

        <!-- 维护模式提示 -->
        <t-alert
          v-if="systemConfig.maintenanceMode"
          :message="systemConfig.maintenanceMessage || '系统正在维护中，请稍后再试'"
          theme="warning"
          :close="false"
          style="margin-bottom: 20px;"
        />

        <t-form-item>
          <t-button
            theme="primary"
            size="large"
            class="login-btn"
            :loading="loading"
            :disabled="systemConfig.maintenanceMode"
            @click="handleLogin"
          >
            {{ systemConfig.maintenanceMode ? '系统维护中' : '登录' }}
          </t-button>
        </t-form-item>

        <div class="login-footer" v-if="registerAllowed">
          <span>还没有账号？</span>
          <t-link theme="primary" @click="$router.push('/register')">
            立即注册
          </t-link>
        </div>
      </t-form>
      </div>
      
      <!-- Copyright -->
      <div class="copyright">
      <p class="copyright-text">{{ systemConfig.copyright || 'Mr.Nie' }}</p>
      <p v-if="systemConfig.version" class="version-info">
        系统版本：{{ systemConfig.version }}
      </p>
      <p v-if="systemConfig.icpNumber" class="beian-info">
        <a :href="systemConfig.icpLink || 'http://beian.miit.gov.cn/'" target="_blank" rel="noopener">
          {{ systemConfig.icpNumber }}
        </a>
      </p>
      <p v-if="systemConfig.publicSecurityNumber" class="beian-info">
        <img v-if="systemConfig.publicSecurityIcon" :src="systemConfig.publicSecurityIcon" width="20" style="vertical-align: middle;" />
        <a :href="systemConfig.publicSecurityLink" rel="noreferrer" target="_blank" style="margin-left: 5px;">
          {{ systemConfig.publicSecurityNumber }}
        </a>
      </p>
      </div>
    </div>
  </div>
  
  <!-- 加载状态 -->
  <div v-else class="loading-mask">
      <div class="loading-spinner">
        <t-loading text="加载中..." />
      </div>
    </div>
</template>

<script>
import { ref, reactive, onMounted, getCurrentInstance } from 'vue'
import { useRouter } from 'vue-router'
import { MessagePlugin } from 'tdesign-vue-next'
// 使用TDesign内置图标或文本替代
import { publicAPI } from '../api'
import { useAuthStore } from '@/store/auth'
import { useAiChatStore } from '@/store/aiChat'

export default {
  name: 'Login',
  components: {
    // 移除图标组件，使用内置图标
  },
  setup() {
    const router = useRouter()
    const loginFormRef = ref(null)
    const loading = ref(false)
    const pageLoading = ref(false) // 立即显示登录页面，不等待配置加载
    const registerAllowed = ref(false)
    
    const loginForm = reactive({
      username: '',
      password: '',
      captchaCode: '',
      captchaId: ''
    })
    
    const systemConfig = reactive({
      siteName: '在线刷题系统',
      siteLogo: '',
      description: '欢迎登录',
      copyright: 'Mr.Nie',
      icpNumber: '',
      icpLink: '',
      publicSecurityNumber: '',
      publicSecurityIcon: '',
      publicSecurityLink: '',
      maintenanceMode: false,
      maintenanceMessage: '',
      version: ''
    })
    
    const captchaConfig = ref({
      enabled: false,
      type: 'image'
    })
    const captchaImage = ref('')
    
    const loginRules = reactive({
      username: [
        { required: true, message: '请输入用户名', trigger: 'blur' }
      ],
      password: [
        { required: true, message: '请输入密码', trigger: 'blur' }
      ],
      captchaCode: [
        { 
          required: false,
          message: '请输入验证码',
          trigger: 'blur',
          validator: (val) => {
            if (captchaConfig.value.enabled && !val) {
              return { result: false, message: '请输入验证码', type: 'error' }
            }
            return { result: true }
          }
        }
      ]
    })
    
    // 获取验证码配置 - 添加超时控制
    const getCaptchaConfig = async () => {
      try {
        console.log('Login: 开始加载验证码配置')
        
        // 设置超时控制
        const timeoutPromise = new Promise((_, reject) => {
          setTimeout(() => reject(new Error('验证码配置加载超时')), 2000)
        })
        
        const fetchPromise = fetch('/api/captcha/config')
        
        let response
        try {
          response = await Promise.race([fetchPromise, timeoutPromise])
          const result = await response.json()
          
          if (result.code === 200) {
            captchaConfig.value = result.data
            if (result.data.enabled) {
              await generateCaptcha()
            }
            console.log('Login: 验证码配置加载成功')
          } else {
            throw new Error('验证码配置加载失败')
          }
        } catch (raceError) {
          console.warn('Login: 验证码配置加载超时，默认不启用')
          captchaConfig.value = { enabled: false, type: 'image' }
        }
      } catch (error) {
        console.warn('Login: 获取验证码配置失败，默认不启用:', error.message)
        captchaConfig.value = { enabled: false, type: 'image' }
      }
    }
    
    // 生成验证码
    const generateCaptcha = async () => {
      try {
        const response = await fetch('/api/captcha/generate')
        const result = await response.json()
        if (result.code === 200 && result.data.enabled) {
          captchaImage.value = result.data.captchaImage
          loginForm.captchaId = result.data.captchaId
          loginForm.captchaCode = ''
        }
      } catch (error) {
        console.error('生成验证码失败:', error)
        MessagePlugin.error('生成验证码失败')
      }
    }
    
    // 刷新验证码
    const refreshCaptcha = () => {
      if (captchaConfig.value.enabled) {
        generateCaptcha()
      }
    }
    
    // 获取系统配置 - 强化错误处理和超时控制
    const getSystemConfig = async () => {
      try {
        console.log('Login: 开始加载系统配置')
        
        // 设置超时控制，防止配置加载卡死
        const timeoutPromise = new Promise((_, reject) => {
          setTimeout(() => reject(new Error('配置加载超时')), 2000) // 2秒超时
        })
        
        const configPromise = publicAPI.getPublicConfig()
        
        let config
        try {
          // 竞速：2秒内获取配置，否则使用默认配置
          config = await Promise.race([configPromise, timeoutPromise])
          console.log('Login: 获取到系统配置:', config)
        } catch (raceError) {
          console.warn('Login: 系统配置加载超时，使用默认配置')
          config = null
        }
        
        if (config) {
          Object.assign(systemConfig, {
            siteName: config.siteName || '七洛题库',
            siteLogo: config.siteLogo || '',
            description: config.description || '欢迎登录',
            copyright: config.copyright || '© 2025 七洛题库 保留所有权利',
            icpNumber: config.icpBeian || '',
            icpLink: config.icpBeianUrl || 'http://beian.miit.gov.cn/',
            publicSecurityNumber: config.policeBeian || '',
            publicSecurityIcon: config.policeBeianIcon || '',
            publicSecurityLink: config.policeBeianUrl || '',
            maintenanceMode: config.maintenanceMode || false,
            maintenanceMessage: config.maintenanceMessage || '',
            version: config.siteVersion || '1.0.0'
          })
        } else {
          // 使用默认配置
          Object.assign(systemConfig, {
            siteName: '七洛题库',
            siteLogo: '',
            description: '欢迎登录',
            copyright: '© 2025 七洛题库 保留所有权利',
            icpNumber: '',
            icpLink: 'http://beian.miit.gov.cn/',
            publicSecurityNumber: '',
            publicSecurityIcon: '',
            publicSecurityLink: '',
            maintenanceMode: false,
            maintenanceMessage: '',
            version: '1.0.0'
          })
        }
        
        // 检查注册权限 - 也加上超时控制
        try {
          const registerTimeoutPromise = new Promise((_, reject) => {
            setTimeout(() => reject(new Error('注册权限检查超时')), 1000)
          })
          
          const registerConfigPromise = publicAPI.getRegisterConfig()
          const registerConfig = await Promise.race([registerConfigPromise, registerTimeoutPromise])
          
          console.log('Login: 注册权限检查结果:', registerConfig)
          registerAllowed.value = registerConfig === true || (registerConfig && registerConfig.enabled)
        } catch (error) {
          console.warn('Login: 检查注册权限失败，默认允许注册:', error.message)
          registerAllowed.value = true
        }
        
        console.log('Login: 系统配置加载完成')
      } catch (error) {
        console.warn('Login: 系统配置加载失败，使用默认配置:', error.message)
        
        // 确保使用默认配置，不影响页面显示
        Object.assign(systemConfig, {
          siteName: '七洛题库',
          siteLogo: '',
          description: '欢迎登录',
          copyright: '© 2025 七洛题库 保留所有权利',
          icpNumber: '',
          icpLink: 'http://beian.miit.gov.cn/',
          publicSecurityNumber: '',
          publicSecurityIcon: '',
          publicSecurityLink: '',
          maintenanceMode: false,
          maintenanceMessage: ''
        })
        registerAllowed.value = true
      }
    }
    
    // 初始化页面 - 简化加载逻辑，防止白屏
    const initPage = async () => {
      try {
        console.log('Login: 开始初始化页面')

        // 在后台异步加载配置，不阻塞页面显示
        Promise.allSettled([
          getSystemConfig(),
          getCaptchaConfig()
        ]).then(() => {
          console.log('Login: 所有配置加载完成')
        }).catch(error => {
          console.warn('Login: 配置加载失败，但页面将正常显示:', error)
        })
        
      } catch (error) {
        console.error('Login: 页面初始化失败:', error)
        // 确保页面能正常显示
        pageLoading.value = false
      }
    }
    
    const handleLogin = async () => {
      try {
        // 检查维护模式
        if (systemConfig.maintenanceMode) {
          MessagePlugin.warning(systemConfig.maintenanceMessage || '系统正在维护中，请稍后再试')
          return
        }
        
        // 手动验证表单
        if (!loginForm.username || !loginForm.password) {
          MessagePlugin.error('请填写完整的登录信息')
          return
        }

        if (captchaConfig.value.enabled && !loginForm.captchaCode) {
          MessagePlugin.error('请输入验证码')
          return
        }
        
        loading.value = true

        const authStore = useAuthStore()
        const response = await authStore.login(loginForm)

        // 初始化 AI 聊天 store 的用户ID
        const aiChatStore = useAiChatStore()
        aiChatStore.currentUserId = response.userInfo.id || response.userInfo.userId

        MessagePlugin.success('登录成功')
        router.push('/')
      } catch (error) {
        console.error('登录失败:', error)
        
        // 确保错误处理能正常工作
        try {
          let errorMessage = error.message || '登录失败，请检查用户名和密码'
          console.log('处理错误信息:', errorMessage)
          
          // 创建自定义提示框函数
          const showErrorMessage = (message) => {
            console.log('尝试显示错误提示:', message)
            
            // 方法1: 使用 MessagePlugin
            try {
              MessagePlugin.error(message)
              console.log('MessagePlugin 调用成功')
            } catch (e) {
              console.error('MessagePlugin 调用失败:', e)
            }
            
            // 方法2: 使用 TDesign 全局方法
            try {
              if (window.$message) {
                window.$message.error(message)
                console.log('全局 $message 调用成功')
              }
            } catch (e) {
              console.error('全局 $message 调用失败:', e)
            }
            
            // 方法3: 创建自定义提示框
            const createCustomMessage = () => {
              // 移除已存在的提示框
              const existingMsg = document.querySelector('.custom-error-message')
              if (existingMsg) {
                existingMsg.remove()
              }
              
              const messageBox = document.createElement('div')
              messageBox.className = 'custom-error-message'
              messageBox.style.cssText = `
                position: fixed;
                top: 20px;
                left: 50%;
                transform: translateX(-50%);
                background: #f56c6c;
                color: white;
                padding: 12px 20px;
                border-radius: 4px;
                box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
                z-index: 9999;
                font-size: 14px;
                max-width: 400px;
                text-align: center;
                animation: slideDown 0.3s ease;
              `
              messageBox.textContent = message
              
              // 添加动画样式
              const style = document.createElement('style')
              style.textContent = `
                @keyframes slideDown {
                  from { opacity: 0; transform: translateX(-50%) translateY(-20px); }
                  to { opacity: 1; transform: translateX(-50%) translateY(0); }
                }
              `
              document.head.appendChild(style)
              
              document.body.appendChild(messageBox)
              
              // 3秒后自动消失
              setTimeout(() => {
                messageBox.style.animation = 'slideDown 0.3s ease reverse'
                setTimeout(() => {
                  if (messageBox.parentNode) {
                    messageBox.remove()
                  }
                }, 300)
              }, 3000)
              
              console.log('自定义提示框创建成功')
            }
            
            createCustomMessage()
          }
          
          // 根据不同的错误类型提供更具体的提示
          if (errorMessage.includes('用户名或密码错误') || errorMessage.includes('用户不存在') || errorMessage.includes('密码错误')) {
            console.log('显示密码错误提示')
            showErrorMessage('用户名或密码错误，请检查后重试')
          } else if (errorMessage.includes('验证码')) {
            console.log('显示验证码错误提示')
            showErrorMessage('验证码错误，请重新输入')
            // 验证码错误时自动刷新验证码
            if (captchaConfig.value.enabled) {
              refreshCaptcha()
            }
          } else if (errorMessage.includes('账户被锁定') || errorMessage.includes('账户已禁用')) {
            console.log('显示账户状态异常提示')
            showErrorMessage('账户状态异常，请联系管理员')
          } else if (errorMessage.includes('网络') || errorMessage.includes('超时')) {
            console.log('显示网络异常提示')
            showErrorMessage('网络连接异常，请检查网络后重试')
          } else if (errorMessage.includes('服务器') || errorMessage.includes('500')) {
            console.log('显示服务器异常提示')
            showErrorMessage('服务器异常，请稍后重试')
          } else {
            console.log('显示通用错误提示')
            // 显示具体的错误信息，但限制长度
            const displayMessage = errorMessage.length > 50 ? errorMessage.substring(0, 50) + '...' : errorMessage
            showErrorMessage(displayMessage)
          }
          
        } catch (msgError) {
          console.error('显示错误提示失败:', msgError)
          // 如果所有方法都失败，使用原生 alert 作为最后备选
          alert('登录失败：' + (error.message || '请检查用户名和密码'))
        }
      } finally {
        loading.value = false
      }
    }
    
    onMounted(() => {
      // 立即调用初始化，因为配置加载是异步的，不会阻塞页面显示
      initPage()
    })
    
    return {
      loginFormRef,
      loginForm,
      loginRules,
      systemConfig,
      registerAllowed,
      captchaConfig,
      captchaImage,
      loading,
      pageLoading,
      handleLogin,
      refreshCaptcha
    }
  }
}
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(-45deg, 
    #e3f2fd, #f3e5f5, #e8f5e8, #fff9c4, #ffffff, #e3f2fd
  );
  background-size: 600% 600%;
  animation: gradientFlow 25s ease-in-out infinite;
  padding: 20px 0;
}

.content-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 100%;
  max-width: 450px;
}

@keyframes gradientFlow {
  0% {
    background-position: 0% 50%;
  }
  50% {
    background-position: 100% 50%;
  }
  100% {
    background-position: 0% 50%;
  }
}

.login-box {
  width: 100%;
  max-width: 450px;
  padding: 32px 32px;
  background: white;
  border-radius: 10px;
  box-shadow: 0 15px 35px rgba(0, 0, 0, 0.1);
  margin: 0 20px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .login-box {
    max-width: 90%;
    padding: 28px 24px;
    margin: 0 15px;
  }

  .login-header h2 {
    font-size: 22px;
  }

  .login-btn {
    height: 42px;
    font-size: 15px;
  }
}

@media (max-width: 480px) {
  .login-box {
    max-width: 95%;
    padding: 24px 20px;
    margin: 0 10px;
    border-radius: 8px;
  }
  
  .login-header h2 {
    font-size: 20px;
  }
  
  .login-header p {
    font-size: 13px;
  }
  
  .login-btn {
    height: 40px;
    font-size: 14px;
  }
  
  .captcha-container {
    flex-direction: column;
    align-items: stretch;
  }
  
  .captcha-image {
    width: 100%;
    margin-top: 10px;
  }
}

.login-header {
  text-align: center;
  margin-bottom: 30px;
}

.login-header h2 {
  color: #333;
  margin-bottom: 10px;
  font-size: 24px;
  font-weight: 600;
}

.login-header p {
  color: #666;
  font-size: 14px;
}

.login-form {
  margin-top: 20px;
  width: 100%;
  padding: 0;
}

/* 移除 TDesign 表单的默认边距 */
:deep(.t-form) {
  margin: 0;
  padding: 0;
}

/* 强制重置所有可能的边距和内边距 */
:deep(.t-form-item__wrapper) {
  margin: 0;
  padding: 0;
}

:deep(.t-form-item__help) {
  margin: 0;
  padding: 0;
}

:deep(.t-input__wrap) {
  margin: 0;
  padding: 0;
}

:deep(.t-input-group) {
  margin: 0;
  padding: 0;
}

/* 确保按钮也没有额外边距 */
:deep(.t-button) {
  margin: 0;
  box-sizing: border-box;
}

.login-btn {
  width: 100%;
  height: 45px;
  font-size: 16px;
  font-weight: 500;
}

/* 确保表单元素居中，移除所有不必要的边距 */
:deep(.t-form-item) {
  margin-bottom: 20px;
  padding: 0 !important;
  margin-left: 0 !important;
  margin-right: 0 !important;
  width: 100% !important;
  display: block !important;
}

:deep(.t-form-item__content) {
  margin: 0 !important;
  padding: 0 !important;
  width: 100% !important;
  display: block !important;
}

:deep(.t-form-item__label) {
  margin: 0 !important;
  padding: 0 !important;
}

:deep(.t-input) {
  width: 100% !important;
  box-sizing: border-box !important;
  margin: 0 !important;
  padding: 0 !important;
  display: block !important;
}

:deep(.t-input__inner) {
  width: 100% !important;
  box-sizing: border-box !important;
  margin: 0 !important;
  padding: 12px 16px !important;
}

:deep(.t-input__wrap) {
  width: 100% !important;
  margin: 0 !important;
  padding: 0 !important;
}

/* 强制重置所有可能影响布局的样式 */
:deep(.t-input-group) {
  width: 100% !important;
  margin: 0 !important;
  padding: 0 !important;
}

:deep(.t-input-group__prepend),
:deep(.t-input-group__append) {
  margin: 0 !important;
  padding: 0 !important;
}

.login-footer {
  text-align: center;
  margin-top: 20px;
  color: #666;
  font-size: 14px;
}

.login-footer .t-link {
  margin-left: 5px;
}

.login-logo {
  height: 60px;
  width: auto;
  margin-bottom: 20px;
}

.copyright {
  text-align: center;
  font-size: 12px;
  line-height: 1.5;
  margin-top: 230px;
  padding: 20px;
  width: 100%;
}

.copyright p {
  margin: 5px 0;
}

.copyright-text {
  color: #333333 !important;
  font-weight: bold;
}

.version-info {
  color: #333333 !important;
  font-size: 11px;
  opacity: 0.8;
}

.beian-info {
  color: #999999 !important;
}

.beian-info a {
  color: #999999 !important;
  text-decoration: none;
  transition: color 0.3s ease;
}

.beian-info a:hover {
  color: #cccccc !important;
  text-decoration: underline;
}

.captcha-container {
  display: flex !important;
  align-items: center !important;
  width: 100% !important;
  gap: 10px !important;
  margin: 0 !important;
  padding: 0 !important;
  box-sizing: border-box !important;
}

.captcha-container :deep(.t-input) {
  flex: 1 !important;
  margin: 0 !important;
  padding: 0 !important;
  width: auto !important;
  min-width: 0 !important;
}

.captcha-container :deep(.t-input__inner) {
  width: 100% !important;
  margin: 0 !important;
  padding: 12px 16px !important;
}

.captcha-image {
  width: 120px;
  height: 40px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f5f7fa;
  transition: border-color 0.3s;
  flex-shrink: 0;
}

.captcha-image:hover {
  border-color: #409eff;
}

.captcha-image img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.captcha-loading {
  font-size: 12px;
  color: #909399;
}

/* 加载状态样式 */
.loading-mask {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
}

.loading-spinner {
  background: rgba(255, 255, 255, 0.9);
  border-radius: 10px;
  padding: 20px;
  min-width: 100px;
  min-height: 100px;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>