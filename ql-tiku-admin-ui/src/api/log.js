import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../store/auth'

// 使用与index.js相同的axios实例配置
const request = axios.create({
  baseURL: process.env.VUE_APP_BASE_API || '/api',
  timeout: 10000
})

// 请求拦截器
request.interceptors.request.use(
  config => {
    const authStore = useAuthStore()
    if (authStore.token) {
      config.headers.Authorization = `Bearer ${authStore.token}`
    }
    
    // 添加用户信息到请求头，用于操作日志记录
    if (authStore.userInfo && authStore.userInfo.id) {
      config.headers['User-Id'] = authStore.userInfo.id.toString()
    }
    if (authStore.userInfo && authStore.userInfo.username) {
      config.headers['Username'] = authStore.userInfo.username
    }
    
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  response => {
    return response.data
  },
  error => {
    if (error.response?.status === 401) {
      ElMessage.error('登录已过期，请重新登录')
    } else {
      ElMessage.error(error.message || '网络错误')
    }
    return Promise.reject(error)
  }
)

/**
 * 日志管理API
 */
export const logAPI = {
  /**
   * 分页查询操作日志
   * @param {Object} params 查询参数
   * @returns {Promise}
   */
  getLogPage(params) {
    return request({
      url: '/operation-log/page',
      method: 'get',
      params
    })
  },

  /**
   * 根据ID查询操作日志详情
   * @param {number} id 日志ID
   * @returns {Promise}
   */
  getLogById(id) {
    return request({
      url: `/operation-log/${id}`,
      method: 'get'
    })
  },

  /**
   * 删除操作日志
   * @param {number} id 日志ID
   * @returns {Promise}
   */
  deleteLog(id) {
    return request({
      url: `/operation-log/${id}`,
      method: 'delete'
    })
  },

  /**
   * 批量删除操作日志
   * @param {Array} ids 日志ID数组
   * @returns {Promise}
   */
  batchDeleteLog(ids) {
    return request({
      url: '/operation-log/batch',
      method: 'delete',
      data: ids
    })
  }
}

export default logAPI