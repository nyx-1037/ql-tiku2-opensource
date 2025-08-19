import axios from 'axios'
import { MessagePlugin } from 'tdesign-vue-next'
import router from '../router'
import { createCachedAxios, cacheUtils } from './interceptors'

// 创建带缓存功能的axios实例
const api = createCachedAxios({
  baseURL: process.env.VUE_APP_BASE_API || '/api',
  timeout: 10000
})

// 导出缓存工具供外部使用
export { cacheUtils }

// 注释：管理员API实例（客户端暂不使用）
// const adminApi = axios.create({
//   baseURL: process.env.NODE_ENV === 'production' ? '/api/admin' : 'http://localhost:8888/api/admin',
//   timeout: 10000
// })

// 请求拦截器
api.interceptors.request.use(
  config => {
    console.log('🚀 [Client API Request]', {
      method: config.method?.toUpperCase(),
      url: config.url,
      baseURL: config.baseURL,
      fullURL: `${config.baseURL}${config.url}`,
      data: config.data,
      params: config.params,
      headers: config.headers
    })
    
    // 添加详细的环境变量日志
    console.log('🔧 [Environment Variables]', {
      NODE_ENV: process.env.NODE_ENV,
      VUE_APP_ENV: process.env.VUE_APP_ENV,
      VUE_APP_BASE_API: process.env.VUE_APP_BASE_API,
      VUE_APP_BACKEND_URL: process.env.VUE_APP_BACKEND_URL
    })
    
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
      console.log('🔑 [Token]', token.substring(0, 20) + '...')
    }
    return config
  },
  error => {
    console.error('❌ [Client API Request Error]', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
api.interceptors.response.use(
  response => {
    console.log('✅ [Client API Response]', {
      status: response.status,
      url: response.config.url,
      data: response.data
    })
    
    // 如果是blob响应，直接返回
    if (response.config.responseType === 'blob') {
      return response
    }
    
    const { code, message, data } = response.data
    
    if (code === 200) {
      // 返回实际数据，而不是完整响应对象
      return data
    } else {
      console.error('❌ [Client API Business Error]', { code, message, data })
      MessagePlugin.error(message || '请求失败')
      return Promise.reject(new Error(message || '请求失败'))
    }
  },
  error => {
    console.error('❌ [Client API Response Error]', {
      status: error.response?.status,
      statusText: error.response?.statusText,
      url: error.config?.url,
      message: error.message,
      response: error.response?.data
    })
    
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      router.push('/login')
      MessagePlugin.error('登录已过期，请重新登录')
    } else {
      MessagePlugin.error(error.message || '网络错误')
    }
    return Promise.reject(error)
  }
)

// 用户认证API
export const authAPI = {
  // 登录
  login(data) {
    return api.post('/auth/login', data)
  },
  // 注册
  register(data) {
    return api.post('/auth/register', data)
  },
  // 获取当前用户信息
  getCurrentUser() {
    return api.get('/auth/user')
  },
  // 退出登录
  logout() {
    return api.post('/auth/logout')
  },
  // 修改密码
  changePassword(data) {
    return api.post('/auth/change-password', data)
  },
  // 更新头像
  updateAvatar(data) {
    return api.post('/auth/update-avatar', data)
  }
}

// 题目API
export const questionAPI = {
  // 获取题目列表
  getQuestions(params) {
    return api.get('/question', { params })
  },
  // 获取题目详情
  getQuestion(id) {
    return api.get(`/question/${id}`)
  },
  // 获取随机题目
  getRandomQuestions(params) {
    return api.get('/question/random', { params })
  },
  // 获取练习题目（支持顺序和随机不重复）
  getPracticeQuestions(params) {
    return api.get('/question/practice', { params })
  },
  // 搜索题目
  searchQuestions(params) {
    return api.get('/question/search', { params })
  },
  // 提交答案
  submitAnswer(data) {
    return api.post('/question/submit', data)
  },
  // AI判题
  aiGrading(data) {
    return api.post('/question/ai-grading', data, {
      timeout: 60000 // AI判题需要更长的超时时间（60秒）
    })
  },
  // 获取AI判题历史记录
   getAiGradingHistory(questionId) {
     return api.get(`/question/ai-grading/history/${questionId}`)
   },
   
   // 重置练习缓存
   resetPracticeCache(params) {
     return api.delete('/question/practice/cache', { params })
   }
}

// 科目API
export const subjectAPI = {
  // 获取科目列表
  getSubjects() {
    return api.get('/subjects')
  },
  // 获取启用的科目
  getEnabledSubjects() {
    return api.get('/subjects/enabled')
  }
}

// 考试API
export const examAPI = {
  // 创建考试
  createExam(data) {
    return api.post('/exams', data)
  },
  // 创建模拟考试
  createSimulationExam(data) {
    return api.post('/exams/simulation', data)
  },
  // 开始考试
  startExam(id) {
    return api.post(`/exams/${id}/start`)
  },
  // 提交考试
  submitExam(id, data) {
    return api.post(`/exams/${id}/submit`, data)
  },
  // 提交模拟考试
  submitSimulationExam(examId, data) {
    return api.post(`/exams/simulation/${examId}/submit`, data)
  },
  // 获取考试结果
  getExamResult(id) {
    return api.get(`/exams/${id}/result`)
  },
  // 获取可用考试列表（支持分页和搜索）
  getAvailableExams(params) {
    return api.get('/exams', { params })
  },
  // 获取用户考试记录列表（带分页和筛选）
  getUserExamRecords(params) {
    return api.get('/exams/records', { params })
  },
  // 获取考试记录详情（支持重复考试记录）
  getExamRecordDetail(recordId) {
    return api.get(`/exams/records/${recordId}`)
  },
  // 获取特定考试的所有历史记录批次
  getExamRecordBatches(examId) {
    return api.get(`/exams/user-records/${examId}`)
  },
  // 获取模拟考试记录详情
  getSimulationExamRecordDetail(recordId) {
    return api.get(`/exams/simulation/records/${recordId}`)
  },
  // 获取固定试卷列表（支持分页和搜索）
  getFixedPapers(params) {
    return api.get('/exams/fixed-papers', { params })
  },
  // 开始固定试卷考试
  startFixedPaperExam(examId) {
    return api.post(`/exams/fixed-papers/${examId}/start`)
  },
  // 提交固定试卷考试
  submitFixedPaperExam(examId, data) {
    return api.post(`/exams/fixed-papers/${examId}/submit`, data)
  }
}

// 错题本API
export const wrongBookAPI = {
  // 获取错题列表
  getWrongQuestions(params) {
    return api.get('/wrong-book', { params })
  },
  // 移除错题
  removeWrongQuestion(id) {
    return api.delete(`/wrong-book/${id}`)
  },
  // 添加错题
  addWrongQuestion(data) {
    return api.post('/wrong-book', data)
  },
  // 清空错题本
  clearWrongQuestions() {
    return api.delete('/wrong-book/clear')
  }
}

// 统计API
export const statisticsAPI = {
  // 获取个人统计
  getPersonalStats() {
    return api.get('/statistics/personal')
  },
  // 获取学习报告
  getLearningReport(params) {
    return api.get('/statistics/report', { params })
  },
  // 获取概览统计
  getOverviewStats(params) {
    return api.get('/statistics/overview', { params })
  },
  // 获取学习趋势数据
  getTrendData(params) {
    return api.get('/statistics/trend', { params })
  },
  // 获取正确率分析
  getAccuracyAnalysis(params) {
    return api.get('/statistics/accuracy', { params })
  },
  // 获取科目分布
  getSubjectDistribution(params) {
    return api.get('/statistics/subject-distribution', { params })
  },
  // 获取难度分析
  getDifficultyAnalysis(params) {
    return api.get('/statistics/difficulty', { params })
  },
  // 获取错题分析
  getWrongQuestionAnalysis(params) {
    return api.get('/statistics/wrong-analysis', { params })
  },
  // 获取学习建议
  getLearningSuggestions(params) {
    return api.get('/statistics/suggestions', { params })
  }
}

// 刷题记录API
export const practiceRecordAPI = {
  // 获取练习记录列表
  getPracticeRecords(params) {
    return api.get('/practice-record/list', { params })
  },
  
  // 获取练习记录详情
  getPracticeRecordDetails(recordId) {
    return api.get(`/practice-record/details/${recordId}`)
  },
  
  // 创建练习记录
  createPracticeRecord(data) {
    return api.post('/practice-record/create', data)
  },
  
  // 更新练习记录
  updatePracticeRecord(recordId, data) {
    return api.put('/practice-record/update', data)
  },
  
  // 获取练习记录统计
  getPracticeRecordStats(recordId) {
    return api.get(`/practice-record/stats/${recordId}`)
  },
  
  // 添加答题记录
  addAnswerRecord(data) {
    return api.post('/practice-record/add-answer', data)
  },

  // === 数据分析相关API ===
  // 获取学习统计数据
  getStudyStats(params) {
    return api.get('/statistics/personal', { params })
  },
  
  // 获取学习趋势数据
  getStudyTrends(params) {
    return api.get('/statistics/trend', { params })
  },
  
  // 获取科目分析数据
  getSubjectAnalysis(params) {
    return api.get('/statistics/subject-distribution', { params })
  },
  
  // 获取错题统计数据
  getWrongQuestionStats(params) {
    return api.get('/statistics/wrong-analysis', { params })
  },
  
  // 获取学习建议
  getStudyRecommendations(params) {
    return api.get('/statistics/suggestions', { params })
  }
}

// AI聊天API
export const aiAPI = {
  // 发送消息到AI（流式响应）
  sendMessage(data, signal) {
    const token = localStorage.getItem('token')
    console.log('🚀 [AI API] 发送聊天消息:', {
      url: `${process.env.VUE_APP_BASE_API || '/api'}/ai/chat/stream`,
      data: data,
      hasToken: !!token
    })
    
    return fetch(`${process.env.VUE_APP_BASE_API || '/api'}/ai/chat/stream`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': token ? `Bearer ${token}` : '',
        'Accept': 'text/event-stream',
        'Cache-Control': 'no-cache',
        'Connection': 'keep-alive'
      },
      body: JSON.stringify(data),
      signal: signal
    }).then(response => {
      console.log('✅ [AI API] 聊天响应状态:', response.status)
      console.log('📋 [AI API] 响应头:', Object.fromEntries(response.headers.entries()))
      return response
    }).catch(error => {
      console.error('❌ [AI API] 聊天请求失败:', error)
      throw error
    })
  },
  
  // 分析题目（流式响应）
  analyzeQuestion(data, signal) {
    const token = localStorage.getItem('token')
    console.log('🚀 [AI API] 发送题目分析请求:', {
      url: `${process.env.VUE_APP_BASE_API || '/api'}/ai/analyze/stream`,
      data: data,
      hasToken: !!token
    })
    
    return fetch(`${process.env.VUE_APP_BASE_API || '/api'}/ai/analyze/stream`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': token ? `Bearer ${token}` : '',
        'Accept': 'text/event-stream',
        'Cache-Control': 'no-cache',
        'Connection': 'keep-alive'
      },
      body: JSON.stringify(data),
      signal: signal
    }).then(response => {
      console.log('✅ [AI API] 分析响应状态:', response.status)
      console.log('📋 [AI API] 响应头:', Object.fromEntries(response.headers.entries()))
      return response
    }).catch(error => {
      console.error('❌ [AI API] 分析请求失败:', error)
      throw error
    })
  },
  
  // 测试流式连接
  testStream(data, signal) {
    const token = localStorage.getItem('token')
    console.log('🚀 [AI API] 发送测试连接请求:', {
      url: `${process.env.VUE_APP_BASE_API || '/api'}/ai/test/stream`,
      data: data,
      hasToken: !!token
    })
    
    return fetch(`${process.env.VUE_APP_BASE_API || '/api'}/ai/test/stream`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': token ? `Bearer ${token}` : '',
        'Accept': 'text/event-stream',
        'Cache-Control': 'no-cache',
        'Connection': 'keep-alive'
      },
      body: JSON.stringify(data),
      signal: signal
    }).then(response => {
      console.log('✅ [AI API] 测试响应状态:', response.status)
      console.log('📋 [AI API] 响应头:', Object.fromEntries(response.headers.entries()))
      return response
    }).catch(error => {
      console.error('❌ [AI API] 测试请求失败:', error)
      throw error
    })
  },
  // 获取聊天历史
  getChatHistory(params) {
    return api.get('/ai/chat/history', { params })
  },
  // 获取最近会话
  getRecentSessions(params) {
    return api.get('/ai/sessions', { params })
  },
  // 创建新会话
  createSession(data = {}) {
    console.log('🚀 [AI API] 创建新会话请求:', data)
    return api.post('/ai/session/new', data)
  },
  // 获取会话列表
  getSessionList() {
    return api.get('/ai/sessions/list')
  },
  // 获取会话历史记录
  getSessionHistory(sessionId) {
    return api.get('/ai/chat/history', { params: { sessionId } })
  },
  // 删除会话
  deleteSession(sessionId) {
    return api.delete(`/ai/session/${sessionId}`)
  },
  // 更新会话标题
  updateSessionTitle(sessionId, title) {
    return api.put(`/ai/session/${sessionId}/title`, { title })
  }
}

// 公共API
export const publicAPI = {
  // 获取公共配置
  getPublicConfig() {
    return api.get('/public/config')
  },
  // 检查注册是否允许
  checkRegisterAllowed() {
    return api.get('/public/register-allowed')
  },
  // 获取注册配置
  getRegisterConfig() {
    return api.get('/public/register/check')
  },
  // 获取验证码配置
  getCaptchaConfig() {
    return api.get('/public/captcha/config')
  },
  // 生成验证码
  generateCaptcha() {
    return api.get('/public/captcha/generate')
  },
  // 获取每日一语
  getDailyQuote() {
    return api.get('/public/daily-quote')
  }
}

// AI配额API
export const aiQuotaAPI = {
  // 获取用户AI配额信息
  getQuotaInfo() {
    console.log('🔍 调用AI配额信息接口: /ai-quota/info')
    return api.get('/ai-quota/info')
  },
  // 获取今日剩余AI次数
  getRemainingQuota() {
    console.log('🔍 调用剩余配额接口: /ai-quota/remaining')
    return api.get('/ai-quota/remaining')
  },
  // 检查是否有AI使用配额
  checkQuota(aiType = 'analyze') {
    console.log('🔍 检查AI配额:', aiType)
    return api.get('/ai-quota/check', { params: { aiType } })
  },
  // 消耗AI配额
  consumeQuota(aiType = 'analyze', tokens = 1) {
    console.log('🔍 消耗AI配额:', { aiType, tokens })
    return api.post('/ai-quota/consume', null, { params: { aiType, tokens } })
  }
}

// 注册码API
export const registrationCodeAPI = {
  // 验证注册码
  validateCode(code) {
    return api.post('/registration-code/validate', null, { params: { code } })
  },
  // 使用注册码
  useCode(code) {
    return api.post('/registration-code/use', null, { params: { code } })
  }
}

// 公告API
export const announcementAPI = {
  // 获取最新公告
  getLatestAnnouncements(limit = 10) {
    return api.get(`/announcements/latest?limit=${limit}`)
  }
}

// 文件相关API
export const fileAPI = {
  // 获取文件列表
  getFiles(params) {
    return api.get('/files', { params })
  },

  // 根据ID获取文件详情
  getFileById(id) {
    return api.get(`/files/${id}`)
  },

  // 搜索文件
  searchFiles(params) {
    return api.get('/files/search', { params })
  },

  // 增加下载次数
  incrementDownloadCount(id) {
    return api.post(`/files/${id}/download`)
  },

  // 直接下载文件（通过后端FTP服务器）
  downloadFile(id) {
    return api.get(`/files/download/${id}`, {
      responseType: 'blob'
    })
  }
}

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

export default api
