import request from './index'

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

  // 计算注册码剩余时间（秒）
  calculateRemainingTime(expiresAt) {
    const now = Date.now()
    const exp = new Date(expiresAt).getTime()
    return Math.max(0, Math.floor((exp - now) / 1000))
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

export default registrationCodeAPI