<template>
  <div class="resource-library">
    <!-- 加载状态组件 -->
    <LoadingStates
      :loading="isLoading"
      :error="error"
      :retry-count="retryCount"
      :max-retries="maxRetries"
      @retry="handleRetry"
      @clear-error="clearError"
    />

    <div class="header">
      <h2>资料库</h2>
      <div class="search-box">
        <t-input
          v-model="searchKeyword"
          placeholder="搜索文件名或描述"
          @keyup.enter="handle"
          clearable
        >
          <template #suffix>
            <t-button @click="handle" theme="primary" :loading="isLoading">
              <span class="icon">🔍</span>
            </t-button>
          </template>
        </t-input>
      </div>
    </div>

    <div class="content">
      <!-- 自定义表格 -->
      <div v-if="fileList.length > 0" class="simple-table">
        <div class="table-header">
          <div class="header-cell">文件名</div>
          <div class="header-cell">描述</div>
          <div class="header-cell">文件大小</div>
          <div class="header-cell">上传时间</div>
          <div class="header-cell">下载次数</div>
          <div class="header-cell">操作</div>
        </div>
        <div
          v-for="file in fileList"
          :key="file.id"
          class="table-row"
        >
          <div class="table-cell file-name-cell">
            <div class="file-name">
              <t-icon name="file-text" v-if="isDocument(file.fileExtension)" class="file-icon" />
              <t-icon name="image" v-else-if="isImage(file.fileExtension)" class="file-icon" />
              <t-icon name="folder" v-else class="file-icon" />
              <span>{{ file.originalFileName }}</span>
            </div>
          </div>
          <div class="table-cell">
            <span>{{ file.description || '暂无描述' }}</span>
          </div>
          <div class="table-cell">
            <span>{{ formatFileSize(file.fileSize) }}</span>
          </div>
          <div class="table-cell">
            <span>{{ formatDateTime(file.uploadTime) }}</span>
          </div>
          <div class="table-cell">
            <span>{{ file.downloadCount }}</span>
          </div>
          <div class="table-cell action-cell">
            <div class="action-buttons">
              <t-button
                theme="primary"
                size="small"
                @click="copyUrl(file)"
              >
                复制链接
              </t-button>
              <t-button
                theme="success"
                size="small"
                @click="previewFile(file)"
              >
                预览
              </t-button>
              <t-button
                theme="default"
                size="small"
                @click="downloadFile(file)"
              >
                下载
              </t-button>
            </div>
          </div>
        </div>
      </div>

      <!-- 如果没有数据显示空状态 -->
      <div v-if="fileList.length === 0 && !loading" class="empty-state">
        <t-empty description="暂无文件" />
      </div>

      <div class="pagination">
        <CustomPagination
          :current="currentPage"
          :page-size="pageSize"
          :total="total"
          :page-size-options="[
            { value: 10, label: '10' },
            { value: 20, label: '20' },
            { value: 50, label: '50' },
            { value: 100, label: '100' }
          ]"
          @current-change="handleCurrentChange"
          @page-size-change="handleSizeChange"
        />
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { MessagePlugin } from 'tdesign-vue-next'
import { useResourceLibraryStore } from '@/store/resourceLibrary'
import CustomPagination from '../components/CustomPagination.vue'
import LoadingStates from '../components/LoadingStates.vue'

export default {
  name: 'ResourceLibrary',
  components: {
    CustomPagination,
    LoadingStates
  },
  setup() {
    // 使用缓存store
    const resourceStore = useResourceLibraryStore()
    
    const searchKeyword = ref('')

    // 从store获取数据
    const fileList = computed(() => resourceStore.resources)
    const total = computed(() => resourceStore.pagination.total)
    const loading = computed(() => resourceStore.loading)

    // 加载状态和错误处理
    const isLoading = computed(() => resourceStore.loading || false)
    const error = computed(() => null) // 暂时设为null，因为store中没有error属性
    const retryCount = computed(() => 0)
    const maxRetries = computed(() => 3)

    // 错误处理方法
    const handleRetry = () => {
      resourceStore.loadResources()
    }

    const clearError = () => {
      // 暂时空实现
    }
    
    // 本地分页状态（用于UI绑定）
    const currentPage = ref(1)
    const pageSize = ref(12)

    // 获取文件列表 - 使用store方法
    const getFileList = () => {
      const params = {
        keyword: searchKeyword.value.trim() || '',
        page: currentPage.value,
        size: pageSize.value
      }
      
      resourceStore.loadResources(params)
    }

    // 搜索
    const handle = () => {
      currentPage.value = 1
      getFileList()
    }

    // 分页大小改变
    const handleSizeChange = (size) => {
      pageSize.value = size
      currentPage.value = 1
      getFileList()
    }

    // 当前页改变
    const handleCurrentChange = (page) => {
      currentPage.value = page
      getFileList()
    }

    // 复制链接
    const copyUrl = async (file) => {
      try {
        await navigator.clipboard.writeText(file.fullUrl)
        MessagePlugin.success('链接已复制到剪贴板')
      } catch (error) {
        // 降级方案
        const textArea = document.createElement('textarea')
        textArea.value = file.fullUrl
        document.body.appendChild(textArea)
        textArea.select()
        document.execCommand('copy')
        document.body.removeChild(textArea)
        MessagePlugin.success('链接已复制到剪贴板')
      }
    }

    // 预览文件
    const previewFile = async (file) => {
      try {
        // 增加下载次数
        await resourceStore.incrementDownloadCount(file.id)
        
        // 打开预览链接
        window.open(file.fullUrl, '_blank')
        
        // 更新本地显示的下载次数
        file.downloadCount = (file.downloadCount || 0) + 1
      } catch (error) {
        console.error('预览失败:', error)
        MessagePlugin.error('预览失败')
      }
    }

    // 下载文件 - 使用fileAPI的downloadFile方法
    const downloadFile = async (file) => {
      try {
        MessagePlugin.info('正在准备下载...')
        
        console.log('🚀 [ResourceLibrary] 开始下载文件:', {
          fileId: file.id,
          fileName: file.originalFileName
        })
        
        // 使用fileAPI的downloadFile方法，它已经配置了正确的responseType: 'blob'
        const response = await resourceStore.downloadFileFromServer(file.id)
        
        // 获取文件名（从响应头或使用原始文件名）
        const contentDisposition = response.headers.get('Content-Disposition') || response.headers.get('content-disposition')
        let fileName = file.originalFileName
        if (contentDisposition) {
          const fileNameMatch = contentDisposition.match(/filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/)
          if (fileNameMatch && fileNameMatch[1]) {
            fileName = decodeURIComponent(fileNameMatch[1].replace(/['"]/g, ''))
          }
        }
        
        // 获取文件blob
        const blob = await response.blob()
        
        // 创建下载链接
        const url = window.URL.createObjectURL(blob)
        const link = document.createElement('a')
        link.href = url
        link.download = fileName
        link.style.display = 'none'
        
        // 触发下载
        document.body.appendChild(link)
        link.click()
        document.body.removeChild(link)
        
        // 清理URL对象
        window.URL.revokeObjectURL(url)
        
        // 更新本地显示的下载次数（后端已经自动增加了）
        file.downloadCount = (file.downloadCount || 0) + 1
        
        console.log('✅ [ResourceLibrary] 文件下载成功:', fileName)
        MessagePlugin.success('文件下载成功')
      } catch (error) {
        console.error('❌ [ResourceLibrary] 下载失败:', error)
        
        // 检查是否是文件不存在的错误
        if (error.message.includes('404') || error.message.includes('Not Found')) {
          MessagePlugin.error('文件不存在或已被删除')
        } else if (error.message.includes('500')) {
          MessagePlugin.error('服务器内部错误，请稍后重试或联系管理员')
        } else {
          MessagePlugin.error(`下载失败: ${error.message}`)
        }
      }
    }

    // 判断是否为文档类型
    const isDocument = (extension) => {
      const docTypes = ['.pdf', '.doc', '.docx', '.xls', '.xlsx', '.ppt', '.pptx', '.txt']
      return docTypes.includes(extension?.toLowerCase())
    }

    // 判断是否为图片类型
    const isImage = (extension) => {
      const imageTypes = ['.jpg', '.jpeg', '.png', '.gif', '.bmp', '.webp']
      return imageTypes.includes(extension?.toLowerCase())
    }

    // 格式化文件大小
    const formatFileSize = (size) => {
      if (!size) return '0 B'
      const units = ['B', 'KB', 'MB', 'GB']
      let index = 0
      let fileSize = size
      
      while (fileSize >= 1024 && index < units.length - 1) {
        fileSize /= 1024
        index++
      }
      
      return `${fileSize.toFixed(1)} ${units[index]}`
    }

    // 格式化日期时间
    const formatDateTime = (dateTime) => {
      if (!dateTime) return ''
      const date = new Date(dateTime)
      return date.toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
      })
    }

    // 组件挂载时初始化store
    onMounted(() => {
      // 初始化数据（会优先使用缓存）
      const params = {
        page: currentPage.value,
        size: pageSize.value
      }
      
      if (typeof resourceStore.initializeData === 'function') {
        resourceStore.initializeData(params).catch(err => {
          console.error('初始化失败，使用降级方案:', err)
          // 降级到直接调用loadResources
          resourceStore.loadResources(params).catch(loadErr => {
            console.error('加载资源失败:', loadErr)
          })
        })
      } else {
        // 如果没有initializeData方法，直接调用loadResources
        resourceStore.loadResources(params).catch(err => {
          console.error('加载资源失败:', err)
        })
      }
    })

    // 组件卸载时清理
    onUnmounted(() => {
      // 可以在这里添加清理逻辑，如取消未完成的请求
    })

    return {
      loading,
      searchKeyword,
      fileList,
      currentPage,
      pageSize,
      total,
      getFileList,
      handle,
      handleSizeChange,
      handleCurrentChange,
      copyUrl,
      previewFile,
      downloadFile,
      isDocument,
      isImage,
      formatFileSize,
      formatDateTime,
      // 加载状态和错误处理
      isLoading,
      error,
      retryCount,
      maxRetries,
      handleRetry,
      clearError
    }
  }
}
</script>

<style scoped>
.resource-library {
  padding: 20px;
  max-width: 1400px;
  margin: 0 auto;
  box-sizing: border-box;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 15px;
}

.header h2 {
  margin: 0;
  color: #333;
  flex-shrink: 0;
}

.search-box {
  width: 400px;
  max-width: 100%;
}

.content {
  background: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  overflow-x: auto;
  width: 100%;
}

.file-name {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.file-name span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-icon {
  font-size: 16px;
  color: #409EFF;
  flex-shrink: 0;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

.t-table {
  margin-bottom: 20px;
  width: 100%;
  max-width: 100%;
}

.t-button {
  margin-right: 8px;
}

.t-button:last-child {
  margin-right: 0;
}

/* 响应式布局 */
@media screen and (max-width: 1200px) {
  .resource-library {
    padding: 15px;
  }
  
  .content {
    padding: 15px;
  }
}

@media screen and (max-width: 768px) {
  .resource-library {
    padding: 10px;
  }
  
  .header {
    flex-direction: column;
    align-items: stretch;
  }
  
  .search-box {
    width: 100%;
  }
  
  .content {
    padding: 10px;
  }
  
  .t-table {
    font-size: 14px;
  }
  
  .t-button {
    padding: 8px 12px;
    font-size: 12px;
  }
}

@media screen and (max-width: 480px) {
  .resource-library {
    padding: 8px;
  }
  
  .content {
    padding: 8px;
  }
  
  .t-table {
    font-size: 12px;
  }
  
  .t-button {
    padding: 6px 10px;
    font-size: 11px;
    margin-right: 4px;
  }
}

/* 防止表格溢出 */
.resource-library * {
  box-sizing: border-box;
}

.t-table__body-wrapper {
  overflow-anchor: none;
}

.responsive-table {
  width: 100% !important;
  max-width: 100%;
  table-layout: fixed;
}

/* 完全弹性响应式布局 */
.resource-library {
  contain: layout style;
  max-width: 100%;
  margin: 0;
  padding: 20px 10px;
}

.content {
  width: 100%;
  max-width: 100%;
  margin: 0;
  padding: 15px;
  box-sizing: border-box;
  overflow-x: auto;
}

.responsive-table {
  width: 100%;
  max-width: 100%;
  table-layout: auto;
}

/* 弹性列宽 */
.t-table__header th,
.t-table__body td {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 响应式断点 - 使用弹性百分比 */
@media screen and (max-width: 1200px) {
  .resource-library {
    padding: 15px 8px;
  }
  
  .content {
    padding: 12px;
  }
}

@media screen and (max-width: 768px) {
  .resource-library {
    padding: 12px 5px;
  }
  
  .header {
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
  }
  
  .search-box {
    width: 100%;
    max-width: 100%;
  }
  
  .content {
    padding: 10px 5px;
    overflow-x: auto;
  }
  
  .responsive-table {
    font-size: 14px;
    table-layout: auto;
  }
  
  .t-button {
    padding: 6px 8px;
    font-size: 12px;
    margin-right: 3px;
  }
  
  .t-table__header th,
  .t-table__body td {
    padding: 8px 4px;
    font-size: 13px;
  }
  
  /* 移动端列宽优化 */
  .t-table-column {
    min-width: auto !important;
  }
}

@media screen and (max-width: 480px) {
  .resource-library {
    padding: 8px 2px;
  }
  
  .content {
    padding: 8px 2px;
  }
  
  .responsive-table {
    font-size: 12px;
  }
  
  .t-button {
    padding: 4px 6px;
    font-size: 11px;
    margin-right: 2px;
  }
  
  .t-table__header th,
  .t-table__body td {
    padding: 6px 3px;
    font-size: 11px;
  }
}

/* 防止ResizeObserver循环 */
.resource-library * {
  box-sizing: border-box;
}

.t-table__body-wrapper {
  overflow-anchor: none;
  contain: layout style;
}

/* 强制表格适应容器 */
.t-table {
  width: 100% !important;
  max-width: 100% !important;
}

.t-table__header,
.t-table__body {
  width: 100% !important;
}

/* 隐藏错误覆盖层 */
:global(.webpack-dev-server-overlay) {
  display: none !important;
}

/* 简单表格样式 */
.simple-table {
  border: 1px solid #e6e6e6;
  border-radius: 6px;
  overflow: hidden;
  background: white;
}

.table-header {
  display: grid;
  grid-template-columns: 2fr 1.5fr 1fr 1.5fr 1fr 2fr;
  background: #f5f7fa;
  border-bottom: 1px solid #e6e6e6;
}

.header-cell {
  padding: 12px 8px;
  font-weight: 500;
  color: #303133;
  text-align: center;
  border-right: 1px solid #e6e6e6;
}

.header-cell:last-child {
  border-right: none;
}

.table-row {
  display: grid;
  grid-template-columns: 2fr 1.5fr 1fr 1.5fr 1fr 2fr;
  border-bottom: 1px solid #e6e6e6;
  transition: background-color 0.2s;
}

.table-row:hover {
  background-color: #f5f7fa;
}

.table-row:last-child {
  border-bottom: none;
}

.table-cell {
  padding: 12px 8px;
  color: #606266;
  text-align: center;
  border-right: 1px solid #e6e6e6;
  display: flex;
  align-items: center;
  justify-content: center;
  word-break: break-all;
  overflow: hidden;
}

.table-cell:last-child {
  border-right: none;
}

.file-name-cell {
  justify-content: flex-start;
  text-align: left;
}

.file-name {
  display: flex;
  align-items: center;
  gap: 8px;
}

.file-icon {
  color: #409eff;
  font-size: 16px;
}

.action-cell {
  flex-direction: column;
  gap: 4px;
}

.action-buttons {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
  justify-content: center;
}

/* 调试信息样式 */
.debug-info {
  background: #fff3cd;
  border: 1px solid #ffeaa7;
  border-radius: 6px;
  padding: 16px;
  margin: 16px 0;
  color: #856404;
}

.debug-info p {
  margin: 4px 0;
}

/* 空状态样式 */
.empty-state {
  padding: 40px;
  text-align: center;
}

/* 响应式表格 */
@media (max-width: 768px) {
  .table-header,
  .table-row {
    grid-template-columns: 1fr;
    gap: 0;
  }

  .header-cell,
  .table-cell {
    border-right: none;
    border-bottom: 1px solid #e6e6e6;
    text-align: left;
    padding: 8px 12px;
  }

  .file-name-cell,
  .action-cell {
    justify-content: flex-start;
  }

  .action-buttons {
    justify-content: flex-start;
  }
}
</style>