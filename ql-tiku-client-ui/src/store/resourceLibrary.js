import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { fileAPI } from '@/api'

export const useResourceLibraryStore = defineStore('resourceLibrary', () => {
  // 状态
  const resources = ref([])
  const loading = ref(false)
  const error = ref(null)
  const searchKeyword = ref('')
  const selectedCategory = ref('')
  const pagination = ref({
    current: 1,
    pageSize: 12,
    total: 0,
    totalPages: 0
  })

  // 缓存配置
  const CACHE_KEY = 'resourceLibrary'
  const CACHE_TTL = 5 * 60 * 1000 // 5分钟

  // 简化的缓存管理器
  const simpleCache = {
    get(key) {
      try {
        const item = localStorage.getItem(key)
        return item ? JSON.parse(item) : null
      } catch (e) {
        console.warn('缓存读取失败:', e)
        return null
      }
    },
    set(key, value, ttl) {
      try {
        const item = {
          data: value,
          timestamp: Date.now(),
          ttl: ttl
        }
        localStorage.setItem(key, JSON.stringify(item))
      } catch (e) {
        console.warn('缓存写入失败:', e)
      }
    },
    isExpired(key) {
      const item = this.get(key)
      if (!item) return true
      return Date.now() - item.timestamp > item.ttl
    },
    delete(key) {
      try {
        localStorage.removeItem(key)
      } catch (e) {
        console.warn('缓存删除失败:', e)
      }
    },
    clear(prefix) {
      try {
        const keys = Object.keys(localStorage)
        keys.forEach(key => {
          if (key.startsWith(prefix)) {
            localStorage.removeItem(key)
          }
        })
      } catch (e) {
        console.warn('缓存清理失败:', e)
      }
    }
  }

  // 计算属性
  const filteredResources = computed(() => {
    let filtered = resources.value

    if (searchKeyword.value) {
      const keyword = searchKeyword.value.toLowerCase()
      filtered = filtered.filter(resource => 
        resource.originalFileName?.toLowerCase().includes(keyword) ||
        resource.description?.toLowerCase().includes(keyword)
      )
    }

    if (selectedCategory.value) {
      filtered = filtered.filter(resource => 
        resource.category === selectedCategory.value
      )
    }

    return filtered
  })

  const hasData = computed(() => resources.value.length > 0)
  const isEmpty = computed(() => !loading.value && resources.value.length === 0)

  // 加载资源列表
  const loadResources = async (params = {}) => {
    try {
      loading.value = true
      error.value = null
      
      const defaultParams = {
        page: 1,
        size: 12,
        ...params
      }
      
      console.log('📋 [ResourceLibrary Store] 请求参数:', defaultParams)
      
      // 直接使用fetch调用，绕过响应拦截器的问题
      const token = localStorage.getItem('token')
      const response = await fetch(`${process.env.VUE_APP_BASE_API || '/api'}/files?page=${defaultParams.page}&size=${defaultParams.size}`, {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': token ? `Bearer ${token}` : ''
        }
      })
      
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`)
      }
      
      const result = await response.json()
      console.log('📋 [ResourceLibrary Store] API响应:', result)
      
      // 处理响应数据
      if (result.code === 200 && result.data) {
        const data = result.data
        // 如果响应包含分页信息
        if (data.files && Array.isArray(data.files)) {
          resources.value = data.files
          pagination.value = {
            current: defaultParams.page,
            pageSize: defaultParams.size,
            total: data.total || 0,
            totalPages: data.totalPages || Math.ceil((data.total || 0) / defaultParams.size)
          }
        } 
        // 如果响应直接是数组
        else if (Array.isArray(data)) {
          resources.value = data
          pagination.value = {
            current: defaultParams.page,
            pageSize: defaultParams.size,
            total: data.length,
            totalPages: 1
          }
        }
        // 其他情况
        else {
          console.warn('📋 [ResourceLibrary Store] 未知的响应格式:', data)
          resources.value = []
          pagination.value = {
            current: 1,
            pageSize: 12,
            total: 0,
            totalPages: 0
          }
        }
      } else {
        throw new Error(result.message || '获取资源列表失败')
      }
      
      console.log('📋 [ResourceLibrary Store] 处理后的数据:', {
        resourcesCount: resources.value.length,
        pagination: pagination.value
      })
      
    } catch (err) {
      console.error('📋 [ResourceLibrary Store] 加载资源列表失败:', err)
      error.value = err.message || '获取资源列表失败'
      resources.value = []
      throw new Error('获取资源列表失败')
    } finally {
      loading.value = false
    }
  }

  // 搜索资源
  const searchResources = async (params = {}) => {
    try {
      loading.value = true
      error.value = null
      
      const searchParams = {
        page: 1,
        size: 12,
        ...params
      }
      
      console.log('🔍 [ResourceLibrary Store] 搜索参数:', searchParams)
      
      // 直接使用fetch调用搜索API
      const token = localStorage.getItem('token')
      const queryString = new URLSearchParams(searchParams).toString()
      const response = await fetch(`${process.env.VUE_APP_BASE_API || '/api'}/files/search?${queryString}`, {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': token ? `Bearer ${token}` : ''
        }
      })
      
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`)
      }
      
      const result = await response.json()
      console.log('🔍 [ResourceLibrary Store] 搜索响应:', result)
      
      if (result.code === 200 && result.data) {
        const data = result.data
        if (data.files && Array.isArray(data.files)) {
          resources.value = data.files
          pagination.value = {
            current: searchParams.page,
            pageSize: searchParams.size,
            total: data.total || 0,
            totalPages: data.totalPages || Math.ceil((data.total || 0) / searchParams.size)
          }
        } else if (Array.isArray(data)) {
          resources.value = data
          pagination.value = {
            current: searchParams.page,
            pageSize: searchParams.size,
            total: data.length,
            totalPages: 1
          }
        } else {
          resources.value = []
          pagination.value = {
            current: 1,
            pageSize: 12,
            total: 0,
            totalPages: 0
          }
        }
      } else {
        throw new Error(result.message || '搜索失败')
      }
      
    } catch (err) {
      console.error('🔍 [ResourceLibrary Store] 搜索失败:', err)
      error.value = err.message || '搜索失败'
      resources.value = []
      throw err
    } finally {
      loading.value = false
    }
  }

  // 增加下载次数
  const incrementDownloadCount = async (id) => {
    try {
      const token = localStorage.getItem('token')
      const response = await fetch(`${process.env.VUE_APP_BASE_API || '/api'}/files/${id}/download`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': token ? `Bearer ${token}` : ''
        }
      })
      
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`)
      }
      
      const result = await response.json()
      if (result.code === 200) {
        // 更新本地数据中的下载次数
        const resource = resources.value.find(r => r.id === id)
        if (resource) {
          resource.downloadCount = (resource.downloadCount || 0) + 1
        }
        return result.data
      } else {
        throw new Error(result.message || '增加下载次数失败')
      }
    } catch (err) {
      console.error('📋 [ResourceLibrary Store] 增加下载次数失败:', err)
      throw err
    }
  }

  // 直接下载文件（通过后端FTP服务器）
  const downloadFileFromServer = async (fileId) => {
    try {
      const token = localStorage.getItem('token')
      const response = await fetch(`${process.env.VUE_APP_BASE_API || '/api'}/files/download/${fileId}`, {
        method: 'GET',
        headers: {
          'Authorization': token ? `Bearer ${token}` : ''
        }
      })
      
      if (!response.ok) {
        throw new Error(`下载失败: ${response.status} ${response.statusText}`)
      }
      
      return response
    } catch (err) {
      console.error('📋 [ResourceLibrary Store] 文件下载失败:', err)
      throw err
    }
  }

  // 缓存优先的数据初始化
  const initializeData = async (params = {}) => {
    try {
      console.log('🚀 [ResourceLibrary Store] 开始初始化数据')
      
      // 生成缓存键
      const cacheKey = `${CACHE_KEY}_${JSON.stringify(params)}`
      
      // 尝试从缓存获取数据
      const cachedItem = simpleCache.get(cacheKey)
      
      if (cachedItem && !simpleCache.isExpired(cacheKey)) {
        console.log('📦 [ResourceLibrary Store] 使用缓存数据')
        const cachedData = cachedItem.data
        resources.value = cachedData.resources || []
        pagination.value = cachedData.pagination || {
          current: 1,
          pageSize: 12,
          total: 0,
          totalPages: 0
        }
        
        // 后台静默更新
        loadResources(params).then(() => {
          // 更新缓存
          simpleCache.set(cacheKey, {
            resources: resources.value,
            pagination: pagination.value,
            timestamp: Date.now()
          }, CACHE_TTL)
          console.log('🔄 [ResourceLibrary Store] 后台更新完成')
        }).catch(err => {
          console.warn('🔄 [ResourceLibrary Store] 后台更新失败:', err)
        })
      } else {
        console.log('🌐 [ResourceLibrary Store] 从服务器加载数据')
        await loadResources(params)
        
        // 缓存数据
        simpleCache.set(cacheKey, {
          resources: resources.value,
          pagination: pagination.value,
          timestamp: Date.now()
        }, CACHE_TTL)
      }
      
      console.log('✅ [ResourceLibrary Store] 数据初始化完成')
    } catch (err) {
      console.error('❌ [ResourceLibrary Store] 初始化资源库失败:', err)
      // 如果有缓存数据，即使过期也使用
      const cacheKey = `${CACHE_KEY}_${JSON.stringify(params)}`
      const cachedItem = simpleCache.get(cacheKey)
      if (cachedItem && cachedItem.data) {
        console.log('🆘 [ResourceLibrary Store] 使用过期缓存数据作为降级方案')
        const cachedData = cachedItem.data
        resources.value = cachedData.resources || []
        pagination.value = cachedData.pagination || {
          current: 1,
          pageSize: 12,
          total: 0,
          totalPages: 0
        }
      }
      throw err
    }
  }

  // 刷新数据
  const refreshData = async (params = {}) => {
    // 清除相关缓存
    const cacheKey = `${CACHE_KEY}_${JSON.stringify(params)}`
    simpleCache.delete(cacheKey)
    
    // 重新加载数据
    await loadResources(params)
    
    // 更新缓存
    simpleCache.set(cacheKey, {
      resources: resources.value,
      pagination: pagination.value,
      timestamp: Date.now()
    }, CACHE_TTL)
  }

  // 清除缓存
  const clearCache = () => {
    simpleCache.clear(CACHE_KEY)
  }

  // 重置状态
  const resetState = () => {
    resources.value = []
    loading.value = false
    error.value = null
    searchKeyword.value = ''
    selectedCategory.value = ''
    pagination.value = {
      current: 1,
      pageSize: 12,
      total: 0,
      totalPages: 0
    }
  }

  return {
    // 状态
    resources,
    loading,
    error,
    searchKeyword,
    selectedCategory,
    pagination,
    
    // 计算属性
    filteredResources,
    hasData,
    isEmpty,
    
    // 方法
    loadResources,
    searchResources,
    incrementDownloadCount,
    downloadFileFromServer,
    initializeData,
    refreshData,
    clearCache,
    resetState
  }
})