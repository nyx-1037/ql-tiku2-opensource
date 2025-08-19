import { defineStore } from 'pinia'
import { authAPI } from '../api'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    // 用户认证信息
    token: localStorage.getItem('token') || null,
    userInfo: null,
    isLoggedIn: false,
    
    // 登录状态
    loginLoading: false,
    
    // 初始化状态
    initialized: false
  }),
  
  getters: {
    // 检查是否已登录
    isAuthenticated: (state) => {
      return !!state.token && !!state.userInfo
    },
    
    // 获取用户ID
    userId: (state) => {
      return state.userInfo?.id || state.userInfo?.userId || null
    },
    
    // 获取用户名
    username: (state) => {
      return state.userInfo?.username || ''
    },
    
    // 获取用户角色
    userRole: (state) => {
      return state.userInfo?.role || 'user'
    }
  },
  
  actions: {
    // 初始化认证状态
    async initAuth() {
      console.log('AuthStore: 开始初始化认证状态')
      
      if (this.initialized) {
        console.log('AuthStore: 已初始化，跳过')
        return
      }
      
      try {
        // 如果有token，尝试获取用户信息
        if (this.token) {
          console.log('AuthStore: 发现token，获取用户信息')
          await this.getCurrentUser()
        } else {
          console.log('AuthStore: 无token，设置为未登录状态')
          this.isLoggedIn = false
        }
      } catch (error) {
        console.error('AuthStore: 初始化失败:', error)
        // 清除无效的token
        this.logout()
      } finally {
        this.initialized = true
        console.log('AuthStore: 初始化完成')
      }
    },
    
    // 登录
    async login(credentials) {
      console.log('AuthStore: 开始登录')
      this.loginLoading = true
      
      try {
        const response = await authAPI.login(credentials)
        
        // 由于响应拦截器已经处理了数据提取，response 就是实际的 LoginResponse 数据
        const loginData = response
        
        // 保存认证信息
        this.token = loginData.accessToken
        this.userInfo = loginData.userInfo
        this.isLoggedIn = true
        
        // 同步到localStorage（只保存token）
        localStorage.setItem('token', this.token)
        localStorage.setItem('userInfo', JSON.stringify(this.userInfo))
        
        console.log('AuthStore: 登录成功', { token: this.token?.substring(0, 20) + '...', userInfo: this.userInfo })
        return loginData
      } catch (error) {
        console.error('AuthStore: 登录失败:', error)
        throw error
      } finally {
        this.loginLoading = false
      }
    },
    
    // 获取当前用户信息
    async getCurrentUser() {
      try {
        console.log('AuthStore: 获取当前用户信息')
        const response = await authAPI.getCurrentUser()
        
        // 由于响应拦截器已经处理了数据提取，response 就是实际的 UserInfoVO 数据
        const userInfo = response
        
        this.userInfo = userInfo
        this.isLoggedIn = true
        
        // 更新localStorage中的用户信息
        localStorage.setItem('userInfo', JSON.stringify(userInfo))
        
        console.log('AuthStore: 用户信息获取成功', userInfo)
        return userInfo
      } catch (error) {
        console.error('AuthStore: 获取用户信息失败:', error)
        // 如果获取失败，可能token已过期
        this.logout()
        throw error
      }
    },
    
    // 登出
    logout() {
      console.log('AuthStore: 用户登出')
      
      // 清除状态
      this.token = null
      this.userInfo = null
      this.isLoggedIn = false
      
      // 清除localStorage
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      
      // 可以调用后端登出接口
      try {
        authAPI.logout().catch(error => {
          console.warn('AuthStore: 后端登出失败:', error)
        })
      } catch (error) {
        console.warn('AuthStore: 调用登出接口失败:', error)
      }
    },
    
    // 更新用户信息
    updateUserInfo(userInfo) {
      this.userInfo = { ...this.userInfo, ...userInfo }
      localStorage.setItem('userInfo', JSON.stringify(this.userInfo))
    },
    
    // 检查token有效性
    async checkTokenValidity() {
      if (!this.token) {
        return false
      }
      
      try {
        await this.getCurrentUser()
        return true
      } catch (error) {
        console.warn('AuthStore: Token无效:', error)
        this.logout()
        return false
      }
    }
  }

  // 移除persist配置 - 改为手动管理token，避免Pinia持久化问题
})