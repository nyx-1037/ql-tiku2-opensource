import request from './index'

// 会员等级管理API
export const membershipAPI = {
  // 获取会员等级列表（分页）
  getMemberships(params) {
    return request({
      url: '/admin/memberships',
      method: 'get',
      params
    })
  },

  // 获取所有启用的会员等级
  getActiveMemberships() {
    return request({
      url: '/admin/memberships/active',
      method: 'get'
    })
  },

  // 获取会员等级详情
  getMembership(id) {
    return request({
      url: `/admin/memberships/${id}`,
      method: 'get'
    })
  },

  // 创建会员等级
  createMembership(data) {
    return request({
      url: '/admin/memberships',
      method: 'post',
      data
    })
  },

  // 更新会员等级
  updateMembership(id, data) {
    return request({
      url: `/admin/memberships/${id}`,
      method: 'put',
      data
    })
  },

  // 删除会员等级
  deleteMembership(id) {
    return request({
      url: `/admin/memberships/${id}`,
      method: 'delete'
    })
  },

  // 批量删除会员等级
  batchDeleteMemberships(ids) {
    return request({
      url: '/admin/memberships/batch',
      method: 'delete',
      data: { ids }
    })
  },

  // 更新会员等级状态
  updateMembershipStatus(id, isActive) {
    return request({
      url: `/admin/memberships/${id}/status`,
      method: 'put',
      data: { isActive }
    })
  },

  // 更新用户会员等级
  updateUserMembership(userId, membershipLevel) {
    return request({
      url: `/admin/memberships/users/${userId}/membership`,
      method: 'put',
      data: { membershipLevel }
    })
  },

  // 同步用户AI配额
  syncUserAiQuota(userId) {
    return request({
      url: `/admin/memberships/users/${userId}/sync-quota`,
      method: 'post'
    })
  },

  // 获取会员等级统计信息
  getMembershipStats() {
    return request({
      url: '/admin/memberships/stats',
      method: 'get'
    })
  }
}