import { defineStore } from 'pinia'
import { authAPI } from '../api'

export const useAuthStore = defineStore('auth', {
  state: () => {
    const adminInfo = localStorage.getItem('adminInfo')
    let userInfo = {}
    
    if (adminInfo) {
      try {
        userInfo = JSON.parse(adminInfo)
      } catch (error) {
        console.error('解析用户信息失败:', error)
        userInfo = {}
      }
    }
    
    return {
      token: localStorage.getItem('adminToken') || '',
      userInfo,
      isLoggedIn: false
    }
  },
  
  getters: {
    isAuthenticated: (state) => !!state.token,
    username: (state) => state.userInfo.username || '',
    avatar: (state) => state.userInfo.avatar || ''
  },
  
  actions: {
    setToken(token) {
      this.token = token
      this.isLoggedIn = true
      localStorage.setItem('adminToken', token)
    },
    
    setUserInfo(userInfo) {
      this.userInfo = userInfo
      localStorage.setItem('adminInfo', JSON.stringify(userInfo))
    },
    
    async login(loginData) {
      try {
        // 调用登录API
        const response = await authAPI.adminLogin(loginData)
        
        // 后端返回格式为 { accessToken, userInfo, tokenType, expiresIn }
        const { accessToken, userInfo } = response
        
        this.setToken(accessToken)
        this.setUserInfo(userInfo)
        
        return { token: accessToken, userInfo }
      } catch (error) {
        console.error('登录失败:', error)
        throw error
      }
    },
    
    logout() {
      this.token = ''
      this.userInfo = {}
      this.isLoggedIn = false
      localStorage.removeItem('adminToken')
      localStorage.removeItem('adminInfo')
    },
    
    // 初始化认证状态
    initAuth() {
      const token = localStorage.getItem('adminToken')
      const userInfo = localStorage.getItem('adminInfo')
      
      if (token) {
        this.token = token
        this.isLoggedIn = true
      }
      
      if (userInfo) {
        try {
          this.userInfo = JSON.parse(userInfo)
        } catch (error) {
          console.error('解析用户信息失败:', error)
          this.userInfo = {}
        }
      }
    }
  },
  
  persist: {
    key: 'admin-auth',
    storage: localStorage,
    paths: ['token', 'userInfo', 'isLoggedIn']
  }
})