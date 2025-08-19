import axios from 'axios'

// 创建专用于非流式响应的axios实例
const aiAxios = axios.create({
  baseURL: process.env.VUE_APP_BASE_API || '/api',
  timeout: 60000
})

// 添加请求拦截器
aiAxios.interceptors.request.use(
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

// 获取基础URL和token的辅助函数
function getBaseURL() {
  return process.env.VUE_APP_BASE_API || '/api'
}

function getAuthHeaders() {
  const token = localStorage.getItem('token')
  return token ? { 'Authorization': `Bearer ${token}` } : {}
}

// 流式响应函数 - 使用fetch API
export function analyzeQuestion(data, signal) {
  console.log('🚀 [AI API] 发送题目分析请求 (ai.js):', {
    url: `${getBaseURL()}/ai/analyze/stream`,
    data: data,
    modelId: data.modelId
  })
  
  return fetch(`${getBaseURL()}/ai/analyze/stream`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...getAuthHeaders()
    },
    body: JSON.stringify(data),
    signal
  })
}

export function sendMessage(data, signal) {
  console.log('🚀 [AI API] 发送聊天消息 (ai.js):', {
    url: `${getBaseURL()}/ai/chat/stream`,
    data: data,
    modelId: data.modelId
  })
  
  return fetch(`${getBaseURL()}/ai/chat/stream`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...getAuthHeaders()
    },
    body: JSON.stringify(data),
    signal
  })
}

// 创建会话（非流式）
export function createSession(title = '新对话') {
  return aiAxios.post('/ai/session/new', { title })
    .then(response => {
      const { code, message, data } = response.data
      if (code === 200) {
        return data
      } else {
        throw new Error(message || '创建会话失败')
      }
    })
}

// 获取会话列表
export function getSessionList() {
  return aiAxios.get('/ai/sessions/list')
    .then(response => {
      const { code, message, data } = response.data
      if (code === 200) {
        return data
      } else {
        throw new Error(message || '获取会话列表失败')
      }
    })
}

// 删除会话
export function deleteSession(sessionId) {
  return aiAxios.delete(`/ai/session/${sessionId}`)
    .then(response => {
      const { code, message } = response.data
      if (code === 200) {
        return true
      } else {
        throw new Error(message || '删除会话失败')
      }
    })
}

// 更新会话标题
export function updateSessionTitle(sessionId, title) {
  return aiAxios.put(`/ai/session/${sessionId}/title`, { title })
    .then(response => {
      const { code, message } = response.data
      if (code === 200) {
        return true
      } else {
        throw new Error(message || '更新会话标题失败')
      }
    })
}

export function getChatHistory(params) {
  return aiAxios.get('/ai/chat/history', {
    params: {
      sessionId: params.sessionId,
      limit: params.limit || 20
    }
  })
    .then(response => {
      const { code, message, data } = response.data
      if (code === 200) {
        return data
      } else {
        throw new Error(message || '获取聊天历史失败')
      }
    })
}