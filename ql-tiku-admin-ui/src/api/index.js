import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'
import { useAuthStore } from '../store/auth'

// 创建axios实例
const request = axios.create({
  baseURL: process.env.VUE_APP_BASE_API || '/api',
  timeout: 10000
})

// 请求拦截器
request.interceptors.request.use(
  config => {
    console.log('🚀 [Admin API Request]', {
      method: config.method?.toUpperCase(),
      url: config.url,
      baseURL: config.baseURL,
      fullURL: `${config.baseURL}${config.url}`,
      data: config.data,
      params: config.params
    })
    
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
    console.error('❌ [Admin API Request Error]', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  response => {
    console.log('✅ [Admin API Response]', {
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
      return data
    } else {
      console.error('❌ [Admin API Business Error]', { code, message, data })
      ElMessage.error(message || '请求失败')
      return Promise.reject(new Error(message || '请求失败'))
    }
  },
  error => {
    console.error('❌ [Admin API Response Error]', {
      status: error.response?.status,
      statusText: error.response?.statusText,
      url: error.config?.url,
      message: error.message,
      response: error.response?.data
    })
    
    if (error.response?.status === 401) {
      ElMessage.error('登录已过期，请重新登录')
      const authStore = useAuthStore()
      authStore.logout()
      router.push('/login')
    } else {
      ElMessage.error(error.message || '网络错误')
    }
    return Promise.reject(error)
  }
)

// 管理员认证API
export const authAPI = {
  // 管理员登录
  adminLogin: (data) => {
    return request({
      url: '/admin/auth/login',
      method: 'post',
      data
    })
  },
  
  // 获取管理员信息
  getAdminInfo: () => {
    return request({
      url: '/admin/auth/info',
      method: 'get'
    })
  },
  
  // 更新管理员信息
  updateProfile: (data) => {
    return request({
      url: '/admin/auth/info',
      method: 'put',
      data
    })
  },
  
  // 修改密码
  changePassword: (data) => {
    return request({
      url: '/admin/auth/password',
      method: 'put',
      data
    })
  },
  
  // 管理员退出登录
  adminLogout: () => {
    return request({
      url: '/admin/auth/logout',
      method: 'post'
    })
  }
}

// 题库管理API
export const questionAPI = {
  // 获取题目列表
  getQuestions: (params) => request.get('/admin/questions', { params }),
  
  // 获取题目详情
  getQuestion: (id) => request.get(`/admin/questions/${id}`),
  
  // 创建题目
  createQuestion: (data) => request.post('/admin/questions', data),
  
  // 更新题目
  updateQuestion: (id, data) => request.put(`/admin/questions/${id}`, data),
  
  // 删除题目
  deleteQuestion: (id) => request.delete(`/admin/questions/${id}`),
  
  // 批量删除题目
  batchDeleteQuestions: (ids) => request.delete('/admin/questions/batch', { data: ids }),
  
  // 导入题目
  importQuestions: (data) => request.post('/admin/questions/import', data),
  
  // 导出题目
  exportQuestions: (params) => request.get('/admin/questions/export', { params, responseType: 'blob' }),
  
  // 上传题目图片
  uploadQuestionImage: (data) => request.post('/admin/questions/upload/image', data, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 科目管理API
export const subjectAPI = {
  // 获取科目列表
  getSubjects: (params) => request.get('/admin/subjects', { params }),
  
  // 获取所有启用的科目
  getEnabledSubjects: () => request.get('/admin/subjects/enabled'),
  
  // 创建科目
  createSubject: (data) => request.post('/admin/subjects', data),
  
  // 更新科目
  updateSubject: (id, data) => request.put(`/admin/subjects/${id}`, data),
  
  // 删除科目
  deleteSubject: (id) => request.delete(`/admin/subjects/${id}`),
  
  // 批量删除科目
  batchDeleteSubjects: (ids) => request.delete('/admin/subjects/batch', { data: ids }),
  
  // 更新科目状态
  updateSubjectStatus: (id, status) => request.put(`/admin/subjects/${id}/status`, { status })
}

// 用户管理API
export const userAPI = {
  // 获取用户列表
  getUsers: (params) => request.get('/admin/users', { params }),
  
  // 获取用户详情
  getUser: (id) => request.get(`/admin/users/${id}`),
  
  // 创建用户
  createUser: (data) => request.post('/admin/users', data),
  
  // 更新用户
  updateUser: (id, data) => request.put(`/admin/users/${id}`, data),
  
  // 删除用户
  deleteUser: (id) => request.delete(`/admin/users/${id}`),
  
  // 重置用户密码
  resetPassword: (id) => request.put(`/admin/users/${id}/reset-password`),
  
  // 更新用户状态
  updateUserStatus: (id, status) => request.put(`/admin/users/${id}/status`, { status }),
  
  // 获取会员等级列表
  getMemberships: (params) => request.get('/admin/memberships', { params }),
  getMembershipById: (id) => request.get(`/admin/memberships/${id}`),
  createMembership: (data) => request.post('/admin/memberships', data),
  updateMembership: (id, data) => request.put(`/admin/memberships/${id}`, data),
  deleteMembership: (id) => request.delete(`/admin/memberships/${id}`),
  updateMembershipStatus: (id, isActive) => request.put(`/admin/memberships/${id}/status`, { isActive }),
  batchDeleteMemberships: (ids) => request.delete('/admin/memberships/batch', { data: { ids } }),
  
  // 获取启用的会员等级（用于下拉框）
  getActiveMemberships: () => request.get('/admin/memberships/active')
}

// 考试管理API
export const examAPI = {
  // 获取考试列表
  getExams: (params) => request.get('/admin/exams', { params }),
  
  // 获取考试详情
  getExam: (id) => request.get(`/admin/exams/${id}`),
  
  // 创建考试
  createExam: (data) => request.post('/admin/exams', data),
  
  // 更新考试
  updateExam: (id, data) => request.put(`/admin/exams/${id}`, data),
  
  // 删除考试
  deleteExam: (id) => request.delete(`/admin/exams/${id}`),
  
  // 发布考试
  publishExam: (id) => request.put(`/admin/exams/${id}/publish`),
  
  // 获取考试结果
  getExamResults: (params) => request.get('/admin/exam-results', { params }),
  
  // 获取考试记录列表
  getExamRecords: (params) => request.get('/admin/exam-records', { params }),
  
  // 获取考试记录详情
  getExamRecord: (id) => request.get(`/admin/exam-records/${id}`),
  
  // 删除考试记录
  deleteExamRecord: (id) => request.delete(`/admin/exam-records/${id}`),
  
  // 获取考试统计
  getExamStats: (params) => request.get('/admin/exam-records/stats', { params }),
  
  // 获取题目列表（用于考试题目选择）
  getQuestionsForExam: (params) => request.get('/admin/exams/questions', { params })
}

// 数据分析API
export const analyticsAPI = {
  // 获取分析数据（综合数据）
  getAnalyticsData: (params) => request.get('/admin/analytics/data', { params }),
  
  // 获取系统概览统计
  getOverviewStats: () => request.get('/admin/analytics/overview'),
  
  // 获取用户活跃度统计
  getUserActivityStats: (params) => request.get('/admin/analytics/user-activity', { params }),
  
  // 获取题目统计
  getQuestionStats: (params) => request.get('/admin/analytics/questions', { params }),
  
  // 获取考试统计
  getExamAnalytics: (params) => request.get('/admin/analytics/exams', { params }),
  
  // 获取错题统计
  getWrongQuestionStats: (params) => request.get('/admin/analytics/wrong-questions', { params })
}

// 公告管理API
export const adminAnnouncementAPI = {
  // 获取公告列表
  getAnnouncementList: (params) => request.get('/admin/announcements', { params }),
  
  // 获取公告详情
  getAnnouncementDetail: (id) => request.get(`/admin/announcements/${id}`),
  
  // 创建公告
  createAnnouncement: (data) => request.post('/admin/announcements', data),
  
  // 更新公告
  updateAnnouncement: (id, data) => request.put(`/admin/announcements/${id}`, data),
  
  // 删除公告
  deleteAnnouncement: (id) => request.delete(`/admin/announcements/${id}`),
  
  // 批量删除公告
  batchDeleteAnnouncement: (ids) => request.delete('/admin/announcements/batch', { data: { ids } }),
  
  // 更新公告状态
  updateAnnouncementStatus: (id, data) => request.put(`/admin/announcements/${id}/status`, data)
}

// 系统设置API
export const systemAPI = {
  // 获取系统配置
  getSystemConfig: () => request.get('/admin/system/config'),
  
  // 更新系统配置
  updateSystemConfig: (data) => request.put('/admin/system/config', data),
  
  // 获取配置列表
  getConfigList: () => request.get('/admin/system/config/list'),
  
  // 获取站点信息
  getSiteInfo: () => request.get('/admin/system/site-info'),
  
  // 保存或更新配置
  saveOrUpdateConfig: (data) => request.post('/admin/system/config/save', data),
  
  // 删除配置
  deleteConfig: (id) => request.delete(`/admin/system/config/${id}`),
  
  // 获取基础设置 - 使用统一配置接口
  getBasicSettings: () => request.get('/admin/system/config'),
  
  // 保存基础设置
  saveBasicSettings: (data) => request.put('/admin/system/settings/basic', data),
  
  // 获取邮件设置 - 使用统一配置接口
  getEmailSettings: () => request.get('/admin/system/config'),
  
  // 保存邮件设置
  saveEmailSettings: (data) => request.put('/admin/system/settings/email', data),
  
  // 测试邮件
  testEmail: (data) => request.post('/admin/system/settings/email/test', data),
  
  // 获取存储设置 - 使用统一配置接口
  getStorageSettings: () => request.get('/admin/system/config'),
  
  // 保存存储设置
  saveStorageSettings: (data) => request.put('/admin/system/settings/storage', data),
  
  // 获取安全设置 - 使用统一配置接口
  getSecuritySettings: () => request.get('/admin/system/config'),
  
  // 保存安全设置
  saveSecuritySettings: (data) => request.put('/admin/system/settings/security', data),
  
  // 获取备份设置 - 使用统一配置接口
  getBackupSettings: () => request.get('/admin/system/config'),
  
  // 保存备份设置
  saveBackupSettings: (data) => request.put('/admin/system/settings/backup', data),
  
  // 创建备份
  createBackup: () => request.post('/admin/system/backup'),
  
  // 获取备份历史
  getBackupHistory: () => request.get('/admin/system/backups'),
  
  // 获取备份列表
  getBackupList: () => request.get('/admin/system/backups'),
  
  // 恢复备份
  restoreBackup: (id) => request.post(`/admin/system/backup/${id}/restore`),
  
  // 获取系统日志
  getSystemLogs: (params) => request.get('/admin/system/logs', { params }),
  
  // 清空日志
  clearLogs: () => request.delete('/admin/system/logs'),
  
  // 获取系统通知
  getSystemNotices: (params) => request.get('/admin/system/notices', { params }),
  
  // 清理系统日志
  clearSystemLogs: () => request.delete('/admin/system/logs'),
  
  // 系统备份
  backupSystem: () => request.post('/admin/system/backup'),
  
  // 获取AI设置 - 使用统一配置接口
  getAiSettings: () => request.get('/admin/system/config'),
  
  // 保存AI设置
  saveAiSettings: (data) => request.put('/admin/system/settings/ai', data),
  
  // 测试AI连接
  testAiConnection: (data) => request.post('/admin/system/settings/ai/test', data),
  
  // 重置每日配额
  resetDailyQuota: () => request.post('/admin/ai-quota/reset-daily'),
  
  // 重置每月配额
  resetMonthlyQuota: () => request.post('/admin/ai-quota/reset-monthly')
}

// 文件管理API
export const adminFileAPI = {
  // 获取文件列表
  getFiles(params) {
    return request({
      url: '/admin/files',
      method: 'get',
      params
    })
  },

  // 根据ID获取文件详情
  getFileById(id) {
    return request({
      url: `/admin/files/${id}`,
      method: 'get'
    })
  },

  // 上传文件
  uploadFile(formData) {
    return request({
      url: '/admin/files/upload',
      method: 'post',
      data: formData,
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
  },

  // 删除文件
  deleteFile(id) {
    return request({
      url: `/admin/files/${id}`,
      method: 'delete'
    })
  },

  // 批量删除文件
  batchDeleteFiles(ids) {
    return request({
      url: '/admin/files/batch',
      method: 'delete',
      data: ids
    })
  },

  // 搜索文件
  searchFiles(params) {
    return request({
      url: '/admin/files/search',
      method: 'get',
      params
    })
  },

  // 获取统计信息
  getStatistics() {
    return request({
      url: '/admin/files/statistics',
      method: 'get'
    })
  },

  // 更新文件状态
  updateFileStatus(id, status) {
    return request({
      url: `/admin/files/${id}/status`,
      method: 'put',
      params: { status }
    })
  }
}

// CDN前缀管理API
export const cdnPrefixAPI = {
  // 获取所有CDN前缀
  getAllPrefixes() {
    return request({
      url: '/admin/cdn-prefix',
      method: 'get'
    })
  },

  // 获取启用的CDN前缀
  getActivePrefixes() {
    return request({
      url: '/admin/cdn-prefix/active',
      method: 'get'
    })
  },

  // 获取默认CDN前缀
  getDefaultPrefix() {
    return request({
      url: '/admin/cdn-prefix/default',
      method: 'get'
    })
  },

  // 根据ID获取CDN前缀
  getPrefixById(id) {
    return request({
      url: `/admin/cdn-prefix/${id}`,
      method: 'get'
    })
  },

  // 添加CDN前缀
  addPrefix(data) {
    return request({
      url: '/admin/cdn-prefix',
      method: 'post',
      data
    })
  },

  // 更新CDN前缀
  updatePrefix(id, data) {
    return request({
      url: `/admin/cdn-prefix/${id}`,
      method: 'put',
      data
    })
  },

  // 设置默认CDN前缀
  setDefaultPrefix(id) {
    return request({
      url: `/admin/cdn-prefix/${id}/set-default`,
      method: 'put'
    })
  },

  // 删除CDN前缀
  deletePrefix(id) {
    return request({
      url: `/admin/cdn-prefix/${id}`,
      method: 'delete'
    })
  }
}

// 注册码管理API
export const registrationCodeAPI = {
  // 获取注册码列表
  getRegistrationCodes(params) {
    return request({
      url: '/admin/registration-codes',
      method: 'get',
      params
    })
  },

  // 获取注册码详情
  getRegistrationCode(id) {
    return request({
      url: `/admin/registration-codes/${id}`,
      method: 'get'
    })
  },

  // 生成注册码
  generateRegistrationCode(data) {
    return request({
      url: '/admin/registration-codes/generate',
      method: 'post',
      data
    })
  },

  // 批量生成注册码
  batchGenerateRegistrationCodes(data) {
    return request({
      url: '/admin/registration-codes/batch-generate',
      method: 'post',
      data
    })
  },

  // 更新注册码状态
  updateRegistrationCodeStatus(id, status) {
    return request({
      url: `/admin/registration-codes/${id}/status`,
      method: 'put',
      data: { status }
    })
  },

  // 删除注册码
  deleteRegistrationCode(id) {
    return request({
      url: `/admin/registration-codes/${id}`,
      method: 'delete'
    })
  },

  // 批量删除注册码
  batchDeleteRegistrationCodes(ids) {
    return request({
      url: '/admin/registration-codes/batch',
      method: 'delete',
      data: { ids }
    })
  },

  // 获取注册码使用记录
  getRegistrationCodeUsage(params) {
    return request({
      url: '/admin/registration-codes/usage',
      method: 'get',
      params
    })
  },

  // 验证注册码
  validateRegistrationCode(code) {
    return request({
      url: '/admin/registration-codes/validate',
      method: 'post',
      data: { code }
    })
  },

  // 获取注册码统计信息
  getRegistrationCodeStats() {
    return request({
      url: '/admin/registration-codes/stats',
      method: 'get'
    })
  }
}

// 缓存管理API
export const cacheAPI = {
  // 清除题目缓存
  clearQuestionCache() {
    return request({
      url: '/admin/cache/clear/question',
      method: 'post'
    })
  },
  
  // 清除所有缓存
  clearAllCache() {
    return request({
      url: '/admin/cache/clear/all',
      method: 'post'
    })
  }
}

export default request