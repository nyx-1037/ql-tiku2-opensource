import request from './index.js'

// 管理端文件API
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
  },

  // 下载文件
  downloadFile(id) {
    return request({
      url: `/admin/files/download/${id}`,
      method: 'get',
      responseType: 'blob'
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