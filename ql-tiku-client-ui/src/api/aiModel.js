import axios from 'axios'

// 创建axios实例
const api = axios.create({
  baseURL: process.env.VUE_APP_BASE_API || '/api',
  timeout: 10000
})

// 请求拦截器
api.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 响应拦截器
api.interceptors.response.use(
  response => {
    const { code, message, data } = response.data
    if (code === 200) {
      return data
    } else {
      throw new Error(message || '请求失败')
    }
  },
  error => {
    throw error
  }
)

// AI模型管理API
export const aiModelAPI = {
  // 获取启用的模型列表（客户端使用）
  getEnabledModels() {
    console.log('🔍 获取启用的AI模型列表')
    return api.get('/ai-models/enabled')
  },
  
  // 获取所有模型列表（管理端使用）
  getAllModels(params = {}) {
    console.log('🔍 获取所有AI模型列表:', params)
    return api.get('/ai-models', { params })
  },
  
  // 获取模型详情
  getModelById(id) {
    console.log('🔍 获取AI模型详情:', id)
    return api.get(`/ai-models/${id}`)
  },
  
  // 创建模型
  createModel(data) {
    console.log('➕ 创建AI模型:', data)
    return api.post('/ai-models', data)
  },
  
  // 更新模型
  updateModel(id, data) {
    console.log('✏️ 更新AI模型:', id, data)
    return api.put(`/ai-models/${id}`, data)
  },
  
  // 删除模型
  deleteModel(id) {
    console.log('🗑️ 删除AI模型:', id)
    return api.delete(`/ai-models/${id}`)
  },
  
  // 启用/禁用模型
  toggleModelStatus(id, enabled) {
    console.log('🔄 切换AI模型状态:', id, enabled)
    return api.put(`/ai-models/${id}/status`, { enabled })
  }
}

export default aiModelAPI