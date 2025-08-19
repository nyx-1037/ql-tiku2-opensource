import request from './request'

// 文件相关API
export const fileAPI = {
  // 获取文件列表
  getFiles(params) {
    return request({
      url: '/api/files',
      method: 'get',
      params
    })
  },

  // 根据ID获取文件详情
  getFileById(id) {
    return request({
      url: `/api/files/${id}`,
      method: 'get'
    })
  },

  // 搜索文件
  searchFiles(params) {
    return request({
      url: '/api/files/search',
      method: 'get',
      params
    })
  },

  // 增加下载次数
  incrementDownloadCount(id) {
    return request({
      url: `/api/files/${id}/download`,
      method: 'post'
    })
  },

  // 下载文件
  downloadFile(id) {
    return request({
      url: `/api/files/download/${id}`,
      method: 'get',
      responseType: 'blob'
    })
  }
}

// 管理端文件API
export const adminFileAPI = {
  // 获取文件列表
  getFiles(params) {
    return request({
      url: '/api/admin/files',
      method: 'get',
      params
    })
  },

  // 根据ID获取文件详情
  getFileById(id) {
    return request({
      url: `/api/admin/files/${id}`,
      method: 'get'
    })
  },

  // 上传文件
  uploadFile(formData) {
    return request({
      url: '/api/admin/files/upload',
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
      url: `/api/admin/files/${id}`,
      method: 'delete'
    })
  },

  // 批量删除文件
  batchDeleteFiles(ids) {
    return request({
      url: '/api/admin/files/batch',
      method: 'delete',
      data: ids
    })
  },

  // 搜索文件
  searchFiles(params) {
    return request({
      url: '/api/admin/files/search',
      method: 'get',
      params
    })
  },

  // 获取统计信息
  getStatistics() {
    return request({
      url: '/api/admin/files/statistics',
      method: 'get'
    })
  }
}

// CDN前缀管理API
export const cdnPrefixAPI = {
  // 获取所有CDN前缀
  getAllPrefixes() {
    return request({
      url: '/api/admin/cdn-prefix',
      method: 'get'
    })
  },

  // 获取启用的CDN前缀
  getActivePrefixes() {
    return request({
      url: '/api/admin/cdn-prefix/active',
      method: 'get'
    })
  },

  // 获取默认CDN前缀
  getDefaultPrefix() {
    return request({
      url: '/api/admin/cdn-prefix/default',
      method: 'get'
    })
  },

  // 根据ID获取CDN前缀
  getPrefixById(id) {
    return request({
      url: `/api/admin/cdn-prefix/${id}`,
      method: 'get'
    })
  },

  // 添加CDN前缀
  addPrefix(data) {
    return request({
      url: '/api/admin/cdn-prefix',
      method: 'post',
      data
    })
  },

  // 更新CDN前缀
  updatePrefix(id, data) {
    return request({
      url: `/api/admin/cdn-prefix/${id}`,
      method: 'put',
      data
    })
  },

  // 设置默认CDN前缀
  setDefaultPrefix(id) {
    return request({
      url: `/api/admin/cdn-prefix/${id}/set-default`,
      method: 'put'
    })
  },

  // 删除CDN前缀
  deletePrefix(id) {
    return request({
      url: `/api/admin/cdn-prefix/${id}`,
      method: 'delete'
    })
  }
}