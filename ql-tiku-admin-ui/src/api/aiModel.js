import request from './index'

// AI模型管理API（管理端）
export const aiModelAPI = {
  // 分页查询所有模型列表
  getAllModels(params = {}) {
    return request({
      url: '/admin/ai-models',
      method: 'get',
      params
    })
  },

  // 根据ID获取模型详情
  getModelById(id) {
    return request({
      url: `/admin/ai-models/${id}`,
      method: 'get'
    })
  },

  // 创建AI模型
  createModel(data) {
    return request({
      url: '/admin/ai-models',
      method: 'post',
      data
    })
  },

  // 更新AI模型
  updateModel(id, data) {
    return request({
      url: `/admin/ai-models/${id}`,
      method: 'put',
      data
    })
  },

  // 删除AI模型
  deleteModel(id) {
    return request({
      url: `/admin/ai-models/${id}`,
      method: 'delete'
    })
  },

  // 批量删除AI模型
  batchDeleteModels(ids) {
    return request({
      url: '/admin/ai-models/batch',
      method: 'delete',
      data: ids
    })
  },

  // 启用/禁用AI模型
  toggleModelStatus(id, enabled) {
    return request({
      url: `/admin/ai-models/${id}/status`,
      method: 'put',
      data: { enabled }
    })
  }
}

// 兼容旧的导出方式
export function getAiModelList(params) {
  return aiModelAPI.getAllModels(params)
}

export function getAiModelById(id) {
  return aiModelAPI.getModelById(id)
}

export function createAiModel(data) {
  return aiModelAPI.createModel(data)
}

export function updateAiModel(id, data) {
  return aiModelAPI.updateModel(id, data)
}

export function deleteAiModel(id) {
  return aiModelAPI.deleteModel(id)
}

export function batchDeleteAiModels(ids) {
  return aiModelAPI.batchDeleteModels(ids)
}

export function toggleAiModelStatus(id, enabled) {
  return aiModelAPI.toggleModelStatus(id, enabled)
}

export default aiModelAPI