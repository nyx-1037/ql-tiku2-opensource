import request from './index'

// AI数据管理API
export const aiDataAPI = {
  // 获取AI聊天会话列表
  getSessions(params) {
    return request({
      url: '/admin/ai/sessions',
      method: 'get',
      params
    })
  },

  // 获取AI聊天记录列表
  getChatRecords(params) {
    return request({
      url: '/admin/ai/chat-records',
      method: 'get',
      params
    })
  },

  // 获取AI评分记录列表
  getGradingRecords(params) {
    return request({
      url: '/admin/ai/grading-records',
      method: 'get',
      params
    })
  },

  // 获取会话的聊天记录
  getSessionRecords(sessionId) {
    return request({
      url: `/admin/ai/sessions/${sessionId}/records`,
      method: 'get'
    })
  },

  // 删除AI聊天会话
  deleteSession(id) {
    return request({
      url: `/admin/ai/sessions/${id}`,
      method: 'delete'
    })
  },

  // 删除AI聊天记录
  deleteChatRecord(id) {
    return request({
      url: `/admin/ai/chat-records/${id}`,
      method: 'delete'
    })
  },

  // 删除AI评分记录
  deleteGradingRecord(id) {
    return request({
      url: `/admin/ai/grading-records/${id}`,
      method: 'delete'
    })
  },

  // 批量删除AI聊天会话
  batchDeleteSessions(ids) {
    return request({
      url: '/admin/ai/sessions/batch',
      method: 'delete',
      data: { ids }
    })
  },

  // 批量删除AI聊天记录
  batchDeleteChatRecords(ids) {
    return request({
      url: '/admin/ai/chat-records/batch',
      method: 'delete',
      data: { ids }
    })
  },

  // 批量删除AI评分记录
  batchDeleteGradingRecords(ids) {
    return request({
      url: '/admin/ai/grading-records/batch',
      method: 'delete',
      data: { ids }
    })
  },

  // AI配额管理相关接口
  // 获取用户AI配额列表
  getUserQuotas(params) {
    return request({
      url: '/admin/ai/quotas',
      method: 'get',
      params
    })
  },

  // 获取用户AI配额详情
  getUserQuota(userId) {
    return request({
      url: `/admin/ai/quotas/${userId}`,
      method: 'get'
    })
  },

  // 更新用户AI配额
  updateUserQuota(userId, data) {
    return request({
      url: `/admin/ai/quotas/${userId}`,
      method: 'put',
      data
    })
  },

  // 重置用户每日配额
  resetDailyQuota(userId) {
    return request({
      url: `/admin/ai/quotas/${userId}/reset-daily`,
      method: 'post'
    })
  },

  // 重置用户月度配额
  resetMonthlyQuota(userId) {
    return request({
      url: `/admin/ai/quotas/${userId}/reset-monthly`,
      method: 'post'
    })
  },

  // 批量更新AI配额
  batchUpdateQuotas(data) {
    return request({
      url: '/admin/ai/quotas/batch',
      method: 'put',
      data
    })
  },

  // 获取AI配额统计信息
  getQuotaStats() {
    return request({
      url: '/admin/ai/quotas/stats',
      method: 'get'
    })
  },

  // 初始化用户配额
  initializeUserQuota(userId) {
    return request({
      url: `/admin/ai/quotas/${userId}/initialize`,
      method: 'post'
    })
  },

  // 一键修改用户会员等级并同步AI配额
  updateUserMembershipAndQuota(userId, membershipLevel) {
    return request({
      url: `/admin/memberships/users/${userId}/membership`,
      method: 'put',
      data: { membershipLevel }
    }).then(() => {
      // 同步AI配额
      return request({
        url: `/admin/memberships/users/${userId}/sync-quota`,
        method: 'post'
      })
    })
  }
}
