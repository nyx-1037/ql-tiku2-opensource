<template>
  <div class="register-container">
    <div class="content-wrapper">
      <div class="register-box">
      <div class="register-header">
        <img v-if="systemConfig.logo" :src="systemConfig.logo" alt="Logo" class="register-logo" />
        <h2>{{ systemConfig.siteName || '在线刷题系统' }} - 用户注册</h2>
        <p>{{ systemConfig.description || '创建您的账号' }}</p>
      </div>
      
      <t-form
        ref="registerFormRef"
        :data="registerForm"
        :rules="registerRules"
        class="register-form"
        @submit.prevent="handleRegister"
      >
        <t-form-item name="username">
          <t-input
            v-model="registerForm.username"
            placeholder="👤 请输入用户名"
            size="large"
            :disabled="systemConfig.maintenanceMode"
          />
        </t-form-item>
        
        <t-form-item name="email">
          <t-input
            v-model="registerForm.email"
            placeholder="📧 请输入邮箱"
            size="large"
            :disabled="systemConfig.maintenanceMode"
          />
        </t-form-item>
        
        <t-form-item name="password">
          <t-input
            v-model="registerForm.password"
            type="password"
            placeholder="🔒 请输入密码"
            size="large"
            :disabled="systemConfig.maintenanceMode"
          />
        </t-form-item>

        <t-form-item name="confirmPassword">
          <t-input
            v-model="registerForm.confirmPassword"
            type="password"
            placeholder="🔒 请确认密码"
            size="large"
            :disabled="systemConfig.maintenanceMode"
            @keyup.enter="handleRegister"
          />
        </t-form-item>
        
        <!-- 验证码 -->
        <t-form-item v-if="captchaConfig.enabled" name="captchaCode">
          <div class="captcha-container">
            <t-input
              v-model="registerForm.captchaCode"
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
        
        <!-- 注册码 -->
        <t-form-item v-if="systemConfig.registrationCodeRequired" name="registrationCode">
          <t-input
            v-model="registerForm.registrationCode"
            placeholder="🔑 请输入注册码"
            size="large"
            :disabled="systemConfig.maintenanceMode"
          />
        </t-form-item>
        <t-form-item v-else name="registrationCode">
          <t-input
            v-model="registerForm.registrationCode"
            placeholder="🔑 请输入注册码（可选）"
            size="large"
            :disabled="systemConfig.maintenanceMode"
          />
        </t-form-item>
        
        <!-- 维护模式提示 -->
        <t-alert
          v-if="systemConfig.maintenanceMode"
          theme="warning"
          :close="false"
          style="margin-bottom: 20px;"
        >
          {{ systemConfig.maintenanceMessage || '系统正在维护中，请稍后再试' }}
        </t-alert>
        
        <t-form-item>
          <t-button
            theme="primary"
            size="large"
            class="register-btn"
            :loading="loading"
            :disabled="systemConfig.maintenanceMode"
            @click="handleRegister"
          >
            {{ systemConfig.maintenanceMode ? '系统维护中' : '注册' }}
          </t-button>
        </t-form-item>
        
        <div class="register-footer">
          <span>已有账号？</span>
          <t-link theme="primary" @click="$router.push('/login')">
            立即登录
          </t-link>
        </div>
      </t-form>
      </div>
      
      <!-- Copyright -->
      <div class="copyright">
      <p class="copyright-text">{{ systemConfig.copyright || '© 2025 Mr.Nie 保留所有权利' }}</p>
      <p v-if="systemConfig.version" class="version-text">系统版本：{{ systemConfig.version }}</p>
      <p v-if="systemConfig.icpNumber" class="beian-text">
        <a :href="systemConfig.icpLink || 'http://beian.miit.gov.cn/'" target="_blank" rel="noopener">
          {{ systemConfig.icpNumber }}
        </a>
      </p>
      <p v-if="systemConfig.publicSecurityNumber" class="beian-text">
        <img v-if="systemConfig.publicSecurityIcon" :src="systemConfig.publicSecurityIcon" width="20" style="vertical-align: middle;" />
        <a :href="systemConfig.publicSecurityLink" rel="noreferrer" target="_blank" style="margin-left: 5px;">
          {{ systemConfig.publicSecurityNumber }}
        </a>
      </p>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { MessagePlugin } from 'tdesign-vue-next'
import { authAPI, publicAPI, registrationCodeAPI } from '../api'

export default {
  name: 'Register',
  setup() {
    const router = useRouter()
    const registerFormRef = ref()
    const loading = ref(false)
    
    const registerForm = reactive({
      username: '',
      nickname: '',
      email: '',
      password: '',
      confirmPassword: '',
      captchaCode: '',
      captchaId: '',
      registrationCode: ''
    })
    
    const systemConfig = reactive({
      siteName: '',
      logo: '',
      description: '',
      copyright: '',
      version: '',
      icpNumber: '',
      icpLink: '',
      publicSecurityNumber: '',
      publicSecurityLink: '',
      publicSecurityIcon: '',
      maintenanceMode: false,
      maintenanceMessage: '',
      registrationCodeRequired: false
    })
    
    const captchaConfig = ref({ enabled: false })
    const captchaImage = ref('')
    
    const registerRules = reactive({
      username: [
        { required: true, message: '请输入用户名', trigger: 'blur' },
        { min: 3, max: 20, message: '用户名长度在 3 到 20 个字符', trigger: 'blur' }
      ],
      email: [
        { required: true, message: '请输入邮箱', trigger: 'blur' },
        { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
      ],
      registrationCode: [
        { 
          validator: (val) => {
            if (systemConfig.registrationCodeRequired && !val) {
              return { result: false, message: '请输入注册码', type: 'error' }
            }
            return { result: true }
          }, 
          trigger: 'blur' 
        },
        { max: 50, message: '注册码长度不能超过50个字符', trigger: 'blur' }
      ],
      password: [
        { required: true, message: '请输入密码', trigger: 'blur' },
        { min: 6, max: 18, message: '密码长度必须在6-18个字符之间', trigger: 'blur' },
        { 
          validator: (val) => {
            if (!val) return { result: true }
            const passwordRegex = /^(?=.*[0-9])(?=.*[a-zA-Z])[0-9a-zA-Z]{6,18}$/
            if (!passwordRegex.test(val)) {
              return { result: false, message: '密码必须包含数字和英文字母，长度6-18位', type: 'error' }
            }
            return { result: true }
          }, 
          trigger: 'blur' 
        }
      ],
      confirmPassword: [
        { required: true, message: '请确认密码', trigger: 'blur' },
        { 
          validator: (val) => {
            if (val !== registerForm.password) {
              return { result: false, message: '两次输入密码不一致', type: 'error' }
            }
            return { result: true }
          }, 
          trigger: 'blur' 
        }
      ],
      captchaCode: [
        { 
          validator: (val) => {
            if (captchaConfig.value.enabled && !val) {
              return { result: false, message: '请输入验证码', type: 'error' }
            }
            return { result: true }
          }, 
          trigger: 'blur' 
        }
      ]
    })
    
    // 获取验证码配置
    const getCaptchaConfig = async () => {
      try {
        const response = await fetch('/api/captcha/config')
        const result = await response.json()
        if (result.code === 200) {
          captchaConfig.value = result.data
          if (result.data.enabled) {
            await generateCaptcha()
          }
        }
      } catch (error) {
        console.error('获取验证码配置失败:', error)
      }
    }
    
    // 生成验证码
    const generateCaptcha = async () => {
      try {
        const response = await fetch('/api/captcha/generate')
        const result = await response.json()
        if (result.code === 200 && result.data.enabled) {
          captchaImage.value = result.data.captchaImage
          registerForm.captchaId = result.data.captchaId
          registerForm.captchaCode = ''
        }
      } catch (error) {
        console.error('生成验证码失败:', error)
        MessagePlugin.error('生成验证码失败')
      }
    }
    
    // 刷新验证码
    const refreshCaptcha = async () => {
      await generateCaptcha()
    }
    
    // 获取系统配置
    const getSystemConfig = async () => {
      try {
        const config = await publicAPI.getPublicConfig()
        // 映射配置字段
        systemConfig.siteName = config.siteName || ''
        systemConfig.logo = config.siteLogo || ''
        systemConfig.description = config.siteDescription || ''
        systemConfig.copyright = config.copyright || ''
        systemConfig.version = config.siteVersion || ''
        systemConfig.icpNumber = config.icpBeian || ''
        systemConfig.icpLink = config.icpBeianUrl || ''
        systemConfig.publicSecurityNumber = config.policeBeian || ''
        systemConfig.publicSecurityLink = config.policeBeianUrl || ''
        systemConfig.publicSecurityIcon = config.policeBeianIcon || ''
        
        // 设置维护模式配置
        systemConfig.maintenanceMode = config.maintenanceMode || false
        systemConfig.maintenanceMessage = config.maintenanceMessage || '系统正在维护中，请稍后再试'
        systemConfig.registrationCodeRequired = config.registrationCodeRequired || false
      } catch (error) {
        console.error('获取系统配置失败:', error)
      }
    }
    
    const handleRegister = async () => {
      try {
        // 检查维护模式
        if (systemConfig.maintenanceMode) {
          MessagePlugin.warning(systemConfig.maintenanceMessage || '系统正在维护中，请稍后再试')
          return
        }
        
        // 手动验证表单
        if (!registerForm.username || !registerForm.email || !registerForm.password || !registerForm.confirmPassword) {
          MessagePlugin.error('请填写完整的注册信息')
          return
        }

        if (captchaConfig.value.enabled && !registerForm.captchaCode) {
          MessagePlugin.error('请输入验证码')
          return
        }

        if (systemConfig.registrationCodeRequired && !registerForm.registrationCode) {
          MessagePlugin.error('请输入注册码')
          return
        }

        // 密码确认验证
        if (registerForm.password !== registerForm.confirmPassword) {
          MessagePlugin.error('两次输入密码不一致')
          return
        }

        // 密码格式验证
        const passwordRegex = /^(?=.*[0-9])(?=.*[a-zA-Z])[0-9a-zA-Z]{6,18}$/
        if (!passwordRegex.test(registerForm.password)) {
          MessagePlugin.error('密码必须包含数字和英文字母，长度6-18位')
          return
        }
        
        loading.value = true
        const registerData = {
          username: registerForm.username,
          nickname: registerForm.username, // 使用用户名作为昵称
          email: registerForm.email,
          password: registerForm.password,
          confirmPassword: registerForm.confirmPassword,
          registrationCode: registerForm.registrationCode
        }
        
        // 如果启用验证码，添加验证码信息
        if (captchaConfig.value.enabled) {
          registerData.captchaId = registerForm.captchaId
          registerData.captchaCode = registerForm.captchaCode
        }
        
        // 直接注册用户，所有验证（包括注册码）都在后端进行
        await authAPI.register(registerData)
        
        // 显示成功提示
        console.log('注册成功，显示成功提示')
        
        // 创建自定义成功提示函数
        const showSuccessMessage = (message) => {
          console.log('尝试显示成功提示:', message)
          
          // 方法1: 使用 MessagePlugin
          try {
            MessagePlugin.success(message)
            console.log('MessagePlugin.success 调用成功')
          } catch (e) {
            console.error('MessagePlugin.success 调用失败:', e)
          }
          
          // 方法2: 使用 TDesign 全局方法
          try {
            if (window.$message) {
              window.$message.success(message)
              console.log('全局 $message.success 调用成功')
            }
          } catch (e) {
            console.error('全局 $message.success 调用失败:', e)
          }
          
          // 方法3: 创建自定义成功提示框
          const createCustomSuccessMessage = () => {
            // 移除已存在的提示框
            const existingMsg = document.querySelector('.custom-success-message')
            if (existingMsg) {
              existingMsg.remove()
            }
            
            const messageBox = document.createElement('div')
            messageBox.className = 'custom-success-message'
            messageBox.style.cssText = `
              position: fixed;
              top: 20px;
              left: 50%;
              transform: translateX(-50%);
              background: #67c23a;
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
            
            console.log('自定义成功提示框创建成功')
          }
          
          createCustomSuccessMessage()
        }
        
        showSuccessMessage('注册成功，即将跳转到登录页面')
        
        // 跳转到登录页面
        setTimeout(() => {
          router.push('/login')
        }, 1500) // 延迟1.5秒跳转，让用户看到成功提示
        
      } catch (error) {
        console.error('注册失败:', error)
        
        // 确保错误处理能正常工作
        try {
          let errorMessage = error.message || '注册失败，请稍后重试'
          console.log('处理注册错误信息:', errorMessage)
          
          // 创建自定义错误提示函数
          const showErrorMessage = (message) => {
            console.log('尝试显示注册错误提示:', message)
            
            // 方法1: 使用 MessagePlugin
            try {
              MessagePlugin.error(message)
              console.log('MessagePlugin.error 调用成功')
            } catch (e) {
              console.error('MessagePlugin.error 调用失败:', e)
            }
            
            // 方法2: 使用 TDesign 全局方法
            try {
              if (window.$message) {
                window.$message.error(message)
                console.log('全局 $message.error 调用成功')
              }
            } catch (e) {
              console.error('全局 $message.error 调用失败:', e)
            }
            
            // 方法3: 创建自定义错误提示框
            const createCustomErrorMessage = () => {
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
              
              console.log('自定义错误提示框创建成功')
            }
            
            createCustomErrorMessage()
          }
          
          // 根据不同的错误类型提供更具体的提示
          if (errorMessage.includes('用户名已存在')) {
            console.log('显示用户名已存在提示')
            showErrorMessage('用户名已存在，请更换用户名')
          } else if (errorMessage.includes('邮箱已被注册') || errorMessage.includes('邮箱已存在')) {
            console.log('显示邮箱已存在提示')
            showErrorMessage('邮箱已被注册，请更换邮箱')
          } else if (errorMessage.includes('注册码') && errorMessage.includes('无效')) {
            console.log('显示注册码无效提示')
            showErrorMessage('注册码无效或已过期，请检查注册码')
          } else if (errorMessage.includes('注册码') && errorMessage.includes('已使用')) {
            console.log('显示注册码已使用提示')
            showErrorMessage('注册码已被使用，请使用其他注册码')
          } else if (errorMessage.includes('验证码')) {
            console.log('显示验证码错误提示')
            showErrorMessage('验证码错误，请重新输入')
            // 验证码错误时自动刷新验证码
            if (captchaConfig.value.enabled) {
              await refreshCaptcha()
            }
          } else if (errorMessage.includes('密码') && errorMessage.includes('不一致')) {
            console.log('显示密码不一致提示')
            showErrorMessage('两次输入的密码不一致，请重新输入')
          } else if (errorMessage.includes('密码') && (errorMessage.includes('格式') || errorMessage.includes('包含'))) {
            console.log('显示密码格式错误提示')
            showErrorMessage('密码必须包含数字和英文字母，长度6-18位')
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
          console.error('显示注册错误提示失败:', msgError)
          // 如果所有方法都失败，使用原生 alert 作为最后备选
          alert('注册失败：' + (error.message || '请检查注册信息'))
        }
      } finally {
        loading.value = false
      }
    }
    
    onMounted(async () => {
      await getSystemConfig()
      await getCaptchaConfig()
    })
    
    return {
      registerFormRef,
      registerForm,
      registerRules,
      systemConfig,
      captchaConfig,
      captchaImage,
      loading,
      handleRegister,
      refreshCaptcha
    }
  }
}
</script>

<style scoped>
.register-container {
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

.register-box {
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
  .register-box {
    max-width: 90%;
    padding: 28px 24px;
    margin: 0 15px;
  }
  
  .register-header h2 {
    font-size: 22px;
  }
  
  .register-btn {
    height: 42px;
    font-size: 15px;
  }
}

@media (max-width: 480px) {
  .register-box {
    max-width: 95%;
    padding: 24px 20px;
    margin: 0 10px;
    border-radius: 8px;
  }
  
  .register-header h2 {
    font-size: 20px;
  }
  
  .register-header p {
    font-size: 13px;
  }
  
  .register-btn {
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

.register-header {
  text-align: center;
  margin-bottom: 30px;
}

.register-header h2 {
  color: #333;
  margin-bottom: 10px;
  font-size: 24px;
  font-weight: 600;
}

.register-header p {
  color: #666;
  font-size: 14px;
}

.register-form {
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

.register-btn {
  width: 100%;
  height: 45px;
  font-size: 16px;
  font-weight: 500;
}

/* 确保表单元素居中，移除所有不必要的边距 */
:deep(.t-form-item) {
  margin-bottom: 20px;
  padding: 0;
  margin-left: 0;
  margin-right: 0;
  width: 100%;
}

:deep(.t-form-item__content) {
  margin: 0;
  padding: 0;
  width: 100%;
}

:deep(.t-form-item__label) {
  margin: 0;
  padding: 0;
}

:deep(.t-input) {
  width: 100%;
  box-sizing: border-box;
  margin: 0;
  padding: 0;
}

:deep(.t-input__inner) {
  width: 100%;
  box-sizing: border-box;
}

.register-footer {
  text-align: center;
  margin-top: 20px;
  color: #666;
  font-size: 14px;
}

.register-footer .t-link {
  margin-left: 5px;
}

.register-logo {
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

/* 版权信息样式 - 深灰色加粗 */
.copyright-text {
  color: #333333 !important;
  font-weight: bold;
}

/* 版本信息样式 - 深灰色 */
.version-text {
  color: #333333 !important;
  font-size: 11px;
  opacity: 0.8;
}

/* 备案信息样式 - 浅灰色 */
.beian-text {
  color: #999999 !important;
}

.beian-text a {
  color: #999999 !important;
  text-decoration: none;
}

.beian-text a:hover {
  color: #cccccc !important;
  text-decoration: underline;
}

.captcha-container {
  display: flex;
  align-items: center;
  width: 100%;
  gap: 10px;
}

.captcha-container :deep(.t-input) {
  flex: 1;
  margin-right: 0;
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
</style>