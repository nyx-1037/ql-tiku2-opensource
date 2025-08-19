<template>
  <div class="login-container">
    <div class="login-box">
      <div class="login-header">
        <img v-if="siteConfig.siteLogo" :src="siteConfig.siteLogo" alt="Logo" class="login-logo" />
        <h2>管理员登录</h2>
        <p>{{ siteConfig.siteName }} - 后台管理</p>
      </div>
      
      <el-form
        ref="loginFormRef"
        :model="loginForm"
        :rules="loginRules"
        class="login-form"
        @keyup.enter="handleLogin"
      >
        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            placeholder="请输入管理员账号"
            size="large"
            prefix-icon="User"
          />
        </el-form-item>
        
        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
            size="large"
            prefix-icon="Lock"
            show-password
          />
        </el-form-item>
        
        <!-- 验证码 -->
        <el-form-item v-if="captchaConfig.enabled" prop="captchaCode">
          <div class="captcha-container">
            <el-input
              v-model="loginForm.captchaCode"
              placeholder="请输入验证码"
              size="large"
              prefix-icon="Picture"
              style="flex: 1; margin-right: 10px;"
            />
            <div class="captcha-image" @click="refreshCaptcha">
              <img v-if="captchaImage" :src="captchaImage" alt="验证码" />
              <span v-else class="captcha-loading">加载中...</span>
            </div>
          </div>
        </el-form-item>
        
        <el-form-item>
          <el-button
            type="primary"
            size="large"
            class="login-btn"
            :loading="loading"
            @click="handleLogin"
          >
            {{ loading ? '登录中...' : '登录' }}
          </el-button>
        </el-form-item>
      </el-form>
      
      <div class="login-footer">
        <p>{{ siteConfig.copyright }}</p>
        <div v-if="siteConfig.icpNumber" class="beian-info">
          <a :href="siteConfig.icpUrl" target="_blank" class="beian-link">
            {{ siteConfig.icpNumber }}
          </a>
        </div>
        <div v-if="siteConfig.policeNumber" class="police-beian">
          <a :href="siteConfig.policeUrl" target="_blank" class="beian-link">
            <img v-if="siteConfig.policeIcon" :src="siteConfig.policeIcon" alt="公安备案" class="police-icon" />
            {{ siteConfig.policeNumber }}
          </a>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useSystemStore } from '../store/system'
import { useAuthStore } from '../store/auth'

export default {
  name: 'Login',
  setup() {
    const router = useRouter()
    const systemStore = useSystemStore()
    const authStore = useAuthStore()
    const loginFormRef = ref()
    const loading = ref(false)
    
    const loginForm = reactive({
      username: '',
      password: '',
      captchaCode: '',
      captchaId: ''
    })
    
    const captchaConfig = ref({ enabled: false })
    const captchaImage = ref('')
    
    const loginRules = reactive({
      username: [
        { required: true, message: '请输入管理员账号', trigger: 'blur' },
        { min: 3, max: 20, message: '账号长度在 3 到 20 个字符', trigger: 'blur' }
      ],
      password: [
        { required: true, message: '请输入密码', trigger: 'blur' },
        { min: 6, max: 20, message: '密码长度在 6 到 20 个字符', trigger: 'blur' }
      ],
      captchaCode: [
        { 
          validator: (rule, value, callback) => {
            if (captchaConfig.value.enabled && !value) {
              callback(new Error('请输入验证码'))
            } else {
              callback()
            }
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
          loginForm.captchaId = result.data.captchaId
          loginForm.captchaCode = ''
        }
      } catch (error) {
        console.error('生成验证码失败:', error)
        ElMessage.error('生成验证码失败')
      }
    }
    
    // 刷新验证码
    const refreshCaptcha = () => {
      if (captchaConfig.value.enabled) {
        generateCaptcha()
      }
    }
    
    const handleLogin = async () => {
      if (!loginFormRef.value) return
      
      try {
        await loginFormRef.value.validate()
        loading.value = true
        
        await authStore.login(loginForm)
        
        ElMessage.success('登录成功')
        
        // 确保token已经设置完成后再跳转
        await new Promise(resolve => setTimeout(resolve, 100))
        router.push('/')
      } catch (error) {
        const errorMessage = error.message || '登录失败，请检查用户名和密码'
        
        // 检查是否为验证码错误
        if (errorMessage.includes('验证码')) {
          ElMessage.error(errorMessage)
          // 验证码错误时自动刷新验证码
          if (captchaConfig.value.enabled) {
            await refreshCaptcha()
          }
        } else {
          ElMessage.error(errorMessage)
        }
      } finally {
        loading.value = false
      }
    }
    
    onMounted(async () => {
      await systemStore.loadSystemConfig()
      await getCaptchaConfig()
    })
    
    return {
      loginFormRef,
      loginForm,
      loginRules,
      loading,
      captchaConfig,
      captchaImage,
      siteConfig: systemStore.siteConfig,
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
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.login-box {
  width: 100%;
  max-width: 400px;
  background: white;
  border-radius: 10px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.2);
  overflow: hidden;
}

.login-header {
  text-align: center;
  padding: 40px 30px 30px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.login-header h2 {
  margin-bottom: 10px;
  font-size: 24px;
  font-weight: 600;
}

.login-header p {
  opacity: 0.9;
  font-size: 14px;
}

.login-logo {
  height: 60px;
  width: auto;
  margin-bottom: 20px;
  object-fit: contain;
}

.login-form {
  padding: 30px;
}

.login-form .el-form-item {
  margin-bottom: 20px;
}

.login-btn {
  width: 100%;
  height: 45px;
  font-size: 16px;
  font-weight: 600;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
}

.captcha-container {
  display: flex;
  align-items: center;
  width: 100%;
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

.login-btn:hover {
  background: linear-gradient(135deg, #5a6fd8 0%, #6a4190 100%);
}

.login-footer {
  text-align: center;
  padding: 20px;
  background-color: #f8f9fa;
  border-top: 1px solid #eee;
}

.login-footer p {
  color: #666;
  font-size: 12px;
  margin: 0;
}

.beian-info, .police-beian {
  margin-top: 5px;
}

.beian-link {
  color: #666;
  text-decoration: none;
  font-size: 12px;
}

.beian-link:hover {
  color: #409eff;
  text-decoration: underline;
}

.police-icon {
  width: 14px;
  height: 14px;
  margin-right: 4px;
  vertical-align: middle;
}

/* 响应式设计 - 平板和小屏幕 */
@media (max-width: 768px) {
  .login-container {
    padding: 15px;
    align-items: flex-start;
    padding-top: 10vh;
  }
  
  .login-box {
    max-width: 100%;
    margin: 0;
    box-shadow: 0 5px 15px rgba(0, 0, 0, 0.15);
  }
  
  .login-header {
    padding: 35px 25px 25px;
  }
  
  .login-header h2 {
    font-size: 22px;
  }
  
  .login-header p {
    font-size: 13px;
  }
  
  .login-logo {
    height: 50px;
    margin-bottom: 15px;
  }
  
  .login-form {
    padding: 25px;
  }
  
  .login-form .el-form-item {
    margin-bottom: 18px;
  }
  
  .captcha-container {
    flex-direction: column;
    gap: 10px;
  }
  
  .captcha-container .el-input {
    margin-right: 0 !important;
  }
  
  .captcha-image {
    width: 100%;
    height: 45px;
    align-self: stretch;
  }
  
  .login-footer {
    padding: 15px;
  }
  
  .login-footer p {
    font-size: 11px;
  }
  
  .beian-link {
    font-size: 11px;
  }
}

/* 超小屏幕优化 */
@media (max-width: 480px) {
  .login-container {
    padding: 10px;
    padding-top: 5vh;
  }
  
  .login-box {
    border-radius: 8px;
  }
  
  .login-header {
    padding: 25px 20px 20px;
  }
  
  .login-header h2 {
    font-size: 20px;
    margin-bottom: 8px;
  }
  
  .login-header p {
    font-size: 12px;
  }
  
  .login-logo {
    height: 45px;
    margin-bottom: 12px;
  }
  
  .login-form {
    padding: 20px;
  }
  
  .login-form .el-form-item {
    margin-bottom: 16px;
  }
  
  .login-btn {
    height: 42px;
    font-size: 15px;
  }
  
  .captcha-image {
    height: 42px;
  }
  
  .login-footer {
    padding: 12px;
  }
}

/* 超小屏幕竖屏优化 */
@media (max-width: 360px) {
  .login-container {
    padding: 8px;
  }
  
  .login-header {
    padding: 20px 15px 15px;
  }
  
  .login-form {
    padding: 15px;
  }
  
  .login-footer {
    padding: 10px;
  }
}
</style>