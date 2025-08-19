<template>
  <div class="resource-manage">
    <div class="header">
      <h2>资料库管理</h2>
      <div class="header-actions">
        <el-button type="primary" @click="showUploadDialog = true">
          <el-icon><Upload /></el-icon>
          上传文件
        </el-button>
        <el-button type="info" @click="showCdnManageDialog = true">
          <el-icon><Setting /></el-icon>
          CDN配置
        </el-button>
        <el-button type="danger" :disabled="selectedFiles.length === 0" @click="handleBatchDelete">
          <el-icon><Delete /></el-icon>
          批量删除
        </el-button>
      </div>
    </div>

    <div class="search-bar">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索文件名或描述"
        @keyup.enter="handleSearch"
        clearable
        style="width: 300px; margin-right: 10px;"
      >
        <template #append>
          <el-button @click="handleSearch">
            <el-icon><Search /></el-icon>
          </el-button>
        </template>
      </el-input>
      <el-button @click="resetSearch">重置</el-button>
    </div>

    <div class="content">
      <el-table
        :data="fileList"
        v-loading="loading"
        stripe
        style="width: 100%"
        @selection-change="handleSelectionChange"
        class="responsive-table"
      >
        <el-table-column type="selection" width="55" />
        
        <el-table-column prop="originalFileName" label="文件名" min-width="200">
          <template #default="{ row }">
            <div class="file-name">
              <el-icon class="file-icon">
                <Document v-if="isDocument(row.fileExtension)" />
                <Picture v-else-if="isImage(row.fileExtension)" />
                <Folder v-else />
              </el-icon>
              <span>{{ row.originalFileName }}</span>
            </div>
          </template>
        </el-table-column>
        
        <el-table-column prop="description" label="描述" min-width="200">
          <template #default="{ row }">
            <span>{{ row.description || '暂无描述' }}</span>
          </template>
        </el-table-column>
        
        <el-table-column prop="fileSize" label="文件大小" min-width="100">
          <template #default="{ row }">
            <span>{{ formatFileSize(row.fileSize) }}</span>
          </template>
        </el-table-column>
        
        <el-table-column prop="uploadTime" label="上传时间" min-width="150">
          <template #default="{ row }">
            <span>{{ formatDateTime(row.uploadTime) }}</span>
          </template>
        </el-table-column>
        
        <el-table-column prop="downloadCount" label="下载次数" min-width="80">
          <template #default="{ row }">
            <span>{{ row.downloadCount }}</span>
          </template>
        </el-table-column>
        
        <el-table-column label="状态" min-width="80">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === 1"
              @update:model-value="(val) => handleStatusChange(row, val)"
            />
          </template>
        </el-table-column>
        
        <el-table-column label="操作" min-width="200" fixed="right">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button
                type="primary"
                size="small"
                @click="copyUrl(row)"
                title="复制链接"
              >
                复制
              </el-button>
              <el-button
                type="primary"
                size="small"
                @click="previewFile(row)"
                title="在线预览"
              >
                预览
              </el-button>
              <el-button
                type="success"
                size="small"
                @click="downloadFile(row)"
                title="下载文件"
              >
                下载
              </el-button>
              <el-button
                type="danger"
                size="small"
                @click="handleDelete(row)"
                title="删除文件"
              >
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </div>

    <!-- 上传文件对话框 -->
    <el-dialog
      v-model="showUploadDialog"
      title="上传文件"
      width="500px"
      :before-close="handleUploadDialogClose"
    >
      <el-form :model="uploadForm" label-width="80px">
        <el-form-item label="选择文件">
          <el-upload
            v-if="showUploadDialog"
            ref="uploadRef"
            :key="showUploadDialog"
            :auto-upload="false"
            :on-change="handleFileChange"
            :limit="1"
            drag
            accept=".pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt,.jpg,.jpeg,.png,.gif,.zip,.rar,.7z"
          >
            <el-icon class="el-icon--upload"><upload-filled /></el-icon>
            <div class="el-upload__text">
              将文件拖到此处，或<em>点击上传</em>
            </div>
            <template #tip>
              <div class="el-upload__tip">
                支持 pdf, doc, docx, xls, xlsx, ppt, pptx, txt, jpg, jpeg, png, gif, zip, rar 格式，文件大小不超过 10MB
              </div>
            </template>
          </el-upload>
        </el-form-item>
        
        <el-form-item label="CDN前缀">
          <el-select
            v-model="uploadForm.cdnPrefixId"
            placeholder="选择CDN前缀（可选，默认使用系统默认CDN）"
            clearable
            style="width: 100%"
          >
            <el-option
              v-for="cdn in cdnPrefixList"
              :key="cdn.id"
              :label="`${cdn.name} - ${cdn.prefix}`"
              :value="cdn.id"
            />
          </el-select>
        </el-form-item>
        
        <el-form-item label="文件描述">
          <el-input
            v-model="uploadForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入文件描述（可选）"
          />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="handleUploadDialogClose">取消</el-button>
          <el-button type="primary" :loading="uploading" @click="handleUpload">
            上传
          </el-button>
        </span>
      </template>
    </el-dialog>

    <!-- CDN配置管理对话框 -->
    <el-dialog
      v-model="showCdnManageDialog"
      title="CDN配置管理"
      width="80%"
      :before-close="handleCdnManageDialogClose"
    >
      <div class="cdn-manage-content">
        <div class="cdn-manage-header">
          <el-button type="primary" @click="showCdnDialog = true">
            <el-icon><Plus /></el-icon>
            添加CDN前缀
          </el-button>
        </div>
        
        <el-table :data="cdnPrefixList" stripe style="width: 100%; margin-top: 20px;">
          <el-table-column prop="name" label="名称" width="150" />
          <el-table-column prop="prefix" label="CDN前缀" min-width="200" />
          <el-table-column prop="description" label="描述" min-width="200" />
          
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag v-if="row.isDefault" type="success">默认</el-tag>
              <el-tag v-else-if="row.isActive" type="primary">启用</el-tag>
              <el-tag v-else type="info">禁用</el-tag>
            </template>
          </el-table-column>
          
          <el-table-column label="操作" width="250">
            <template #default="{ row }">
              <el-button
                type="info"
                size="small"
                @click="editCdnPrefix(row)"
              >
                编辑
              </el-button>
              <el-button
                v-if="!row.isDefault"
                type="primary"
                size="small"
                @click="setDefaultCdn(row.id)"
              >
                设为默认
              </el-button>
              <el-button
                type="danger"
                size="small"
                :disabled="row.isDefault"
                @click="deleteCdnPrefix(row.id)"
              >
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-dialog>

    <!-- CDN前缀配置对话框 -->
    <el-dialog
      v-model="showCdnDialog"
      :title="cdnForm.id ? '编辑CDN前缀' : '添加CDN前缀'"
      width="500px"
      :before-close="handleCdnDialogClose"
    >
      <el-form :model="cdnForm" :rules="cdnRules" ref="cdnFormRef" label-width="100px">
        <el-form-item label="前缀名称" prop="name">
          <el-input v-model="cdnForm.name" placeholder="请输入前缀名称" />
        </el-form-item>
        
        <el-form-item label="CDN前缀" prop="prefix">
          <el-input v-model="cdnForm.prefix" placeholder="请输入CDN前缀URL" />
        </el-form-item>
        
        <el-form-item label="描述">
          <el-input
            v-model="cdnForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入描述（可选）"
          />
        </el-form-item>
        
        <el-form-item>
          <el-checkbox v-model="cdnForm.isDefault">设为默认前缀</el-checkbox>
        </el-form-item>
        
        <el-form-item>
          <el-checkbox v-model="cdnForm.isActive">启用</el-checkbox>
        </el-form-item>
      </el-form>
      
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showCdnDialog = false">取消</el-button>
          <el-button type="primary" @click="handleCdnSubmit">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  Upload, 
  Delete, 
  Search, 
  Document, 
  Picture, 
  Folder, 
  UploadFilled,
  Plus,
  Setting
} from '@element-plus/icons-vue'
import { adminFileAPI, cdnPrefixAPI } from '../api/file.js'

export default {
  name: 'ResourceManage',
  components: {
    Upload,
    Delete,
    Search,
    Document,
    Picture,
    Folder,
    UploadFilled,
    Plus,
    Setting
  },
  setup() {
    const loading = ref(false)
    const uploading = ref(false)
    const searchKeyword = ref('')
    const fileList = ref([])
    const selectedFiles = ref([])
    const currentPage = ref(1)
    const pageSize = ref(10)
    const total = ref(0)
    
    const showUploadDialog = ref(false)
    const uploadRef = ref()
    const uploadForm = reactive({
      file: null,
      description: '',
      cdnPrefixId: null
    })
    
    const showCdnDialog = ref(false)
    const showCdnManageDialog = ref(false)
    const cdnFormRef = ref()
    const cdnPrefixList = ref([])
    const cdnForm = reactive({
      id: null,
      name: '',
      prefix: '',
      description: '',
      isDefault: false,
      isActive: true
    })
    
    const cdnRules = {
      name: [{ required: true, message: '请输入前缀名称', trigger: 'blur' }],
      prefix: [{ required: true, message: '请输入CDN前缀URL', trigger: 'blur' }]
    }

    // 获取文件列表
    const getFileList = async () => {
      loading.value = true
      try {
        const params = {
          page: currentPage.value,
          size: pageSize.value
        }
        
        if (searchKeyword.value.trim()) {
          params.keyword = searchKeyword.value.trim()
        }
        
        const response = await adminFileAPI.getFiles(params)
        fileList.value = response.files || []
        total.value = response.total || 0
      } catch (error) {
        console.error('获取文件列表失败:', error)
        ElMessage.error('获取文件列表失败')
      } finally {
        loading.value = false
      }
    }

    // 获取CDN前缀列表
    const getCdnPrefixList = async () => {
      try {
        const response = await cdnPrefixAPI.getAllPrefixes()
        cdnPrefixList.value = response || []
      } catch (error) {
        console.error('获取CDN前缀列表失败:', error)
        ElMessage.error('获取CDN前缀列表失败')
      }
    }

    // 搜索
    const handleSearch = () => {
      currentPage.value = 1
      getFileList()
    }

    // 重置搜索
    const resetSearch = () => {
      searchKeyword.value = ''
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

    // 选择文件改变
    const handleSelectionChange = (selection) => {
      selectedFiles.value = selection
    }

    // 文件选择改变
    const handleFileChange = (file) => {
      const rawFile = file?.raw;
      if (!rawFile) {
        return;
      }
      const allowedTypes = [
        'application/pdf',
        'application/msword',
        'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
        'application/vnd.ms-excel',
        'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
        'application/vnd.ms-powerpoint',
        'application/vnd.openxmlformats-officedocument.presentationml.presentation',
        'text/plain',
        'image/jpeg',
        'image/png',
        'image/gif',
        'application/zip',
        'application/x-rar-compressed',
        'application/x-7z-compressed'
      ];
      const maxSize = 10 * 1024 * 1024; // 10MB

      if (!allowedTypes.includes(rawFile.type)) {
        ElMessage.error('不支持的文件类型！请上传 pdf, doc, docx, xls, xlsx, ppt, pptx, txt, jpg, jpeg, png, gif, zip, rar, 7z 格式的文件。');
        uploadForm.file = null;
        uploadRef.value?.clearFiles();
        return;
      }

      if (rawFile.size > maxSize) {
        ElMessage.error('文件大小不能超过 10MB！');
        uploadForm.file = null;
        uploadRef.value?.clearFiles();
        return;
      }

      uploadForm.file = rawFile;
    }

    // 上传文件
    const handleUpload = async () => {
      if (!uploadForm.file) {
        ElMessage.warning('请选择要上传的文件');
        return;
      }
      
      uploading.value = true
      try {
        const formData = new FormData()
        formData.append('file', uploadForm.file)
        if (uploadForm.description) {
          formData.append('description', uploadForm.description)
        }
        if (uploadForm.cdnPrefixId) {
          formData.append('cdnPrefixId', uploadForm.cdnPrefixId)
        }
        
        await adminFileAPI.uploadFile(formData)
        ElMessage.success('文件上传成功');
        showUploadDialog.value = false;
        uploadRef.value?.clearFiles(); // 清除已选择的文件
        uploadForm.file = null; // 重置文件
        uploadForm.description = ''; // 重置描述
        uploadForm.cdnPrefixId = null; // 重置CDN前缀
        getFileList();
      } catch (error) {
        console.error('文件上传失败:', error)
        ElMessage.error('文件上传失败：' + (error.message || '未知错误'));
        uploadRef.value?.clearFiles(); // 清除已选择的文件
        uploadForm.file = null; // 重置文件
        uploadForm.description = ''; // 重置描述
        uploadForm.cdnPrefixId = null; // 重置CDN前缀
      } finally {
        uploading.value = false;
      }
    }

    // 上传对话框关闭时
    const handleUploadDialogClose = () => {
      showUploadDialog.value = false;
      uploadForm.file = null;
      uploadForm.cdnPrefixId = null;
      uploadForm.description = '';
      // 确保在对话框关闭后再清除文件，避免潜在的渲染问题
      if (uploadRef.value) {
        uploadRef.value.clearFiles();
      }
    }
    
    // 状态切换
    const handleStatusChange = async (file, newValue) => {
      const newStatus = newValue ? 1 : 0
      const oldStatus = file.status
      
      // 先更新本地状态
      file.status = newStatus
      
      try {
        await adminFileAPI.updateFileStatus(file.id, newStatus)
        ElMessage.success('文件状态更新成功')
      } catch (error) {
        console.error('更新文件状态失败:', error)
        ElMessage.error('更新文件状态失败')
        // 恢复原状态
        file.status = oldStatus
      }
    }

    // 删除文件
    const handleDelete = async (file) => {
      try {
        await ElMessageBox.confirm('确定要删除这个文件吗？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        
        await adminFileAPI.deleteFile(file.id)
        ElMessage.success('文件删除成功')
        getFileList()
      } catch (error) {
        if (error !== 'cancel') {
          console.error('删除文件失败:', error)
          ElMessage.error('删除文件失败')
        }
      }
    }

    // 批量删除
    const handleBatchDelete = async () => {
      if (selectedFiles.value.length === 0) {
        ElMessage.warning('请选择要删除的文件')
        return
      }
      
      try {
        await ElMessageBox.confirm(`确定要删除选中的 ${selectedFiles.value.length} 个文件吗？`, '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        
        const ids = selectedFiles.value.map(file => file.id)
        await adminFileAPI.batchDeleteFiles(ids)
        ElMessage.success('批量删除成功')
        getFileList()
      } catch (error) {
        if (error !== 'cancel') {
          console.error('批量删除失败:', error)
          ElMessage.error('批量删除失败')
        }
      }
    }

    // 复制链接
    const copyUrl = async (file) => {
      try {
        await navigator.clipboard.writeText(file.fullUrl)
        ElMessage.success('链接已复制到剪贴板')
      } catch (error) {
        // 降级方案
        const textArea = document.createElement('textarea')
        textArea.value = file.fullUrl
        document.body.appendChild(textArea)
        textArea.select()
        document.execCommand('copy')
        document.body.removeChild(textArea)
        ElMessage.success('链接已复制到剪贴板')
      }
    }

    // 在线预览文件
    const previewFile = (file) => {
      window.open(file.fullUrl, '_blank')
    }

    // 下载文件（通过后端FTP服务器）
    const downloadFile = async (file) => {
      try {
        const response = await adminFileAPI.downloadFile(file.id)
        
        // 创建下载链接
        const url = window.URL.createObjectURL(new Blob([response.data]))
        const link = document.createElement('a')
        link.href = url
        link.download = file.originalFileName
        document.body.appendChild(link)
        link.click()
        document.body.removeChild(link)
        window.URL.revokeObjectURL(url)
        
        ElMessage.success('文件下载成功')
      } catch (error) {
        console.error('下载失败:', error)
        ElMessage.error('下载失败')
      }
    }

    // 关闭CDN管理对话框
    const handleCdnManageDialogClose = () => {
      showCdnManageDialog.value = false
    }

    // 关闭CDN配置对话框
    const handleCdnDialogClose = () => {
      showCdnDialog.value = false
      // 重置表单
      Object.assign(cdnForm, {
        id: null,
        name: '',
        prefix: '',
        description: '',
        isDefault: false,
        isActive: true
      })
    }

    // 编辑CDN前缀
    const editCdnPrefix = (row) => {
      Object.assign(cdnForm, {
        id: row.id,
        name: row.name,
        prefix: row.prefix,
        description: row.description || '',
        isDefault: row.isDefault,
        isActive: row.isActive
      })
      showCdnDialog.value = true
    }

    // 提交CDN配置
    const handleCdnSubmit = async () => {
      try {
        await cdnFormRef.value.validate()
        
        if (cdnForm.id) {
          // 更新CDN前缀
          await cdnPrefixAPI.updatePrefix(cdnForm.id, cdnForm)
          ElMessage.success('CDN前缀更新成功')
        } else {
          // 添加CDN前缀
          await cdnPrefixAPI.addPrefix(cdnForm)
          ElMessage.success('CDN前缀添加成功')
        }
        
        showCdnDialog.value = false
        
        // 重置表单
        Object.assign(cdnForm, {
          id: null,
          name: '',
          prefix: '',
          description: '',
          isDefault: false,
          isActive: true
        })
        
        getCdnPrefixList()
      } catch (error) {
        console.error('CDN前缀操作失败:', error)
        ElMessage.error('CDN前缀操作失败：' + (error.message || '未知错误'))
      }
    }

    // 设置默认CDN
    const setDefaultCdn = async (id) => {
      try {
        await cdnPrefixAPI.setDefaultPrefix(id)
        ElMessage.success('默认CDN设置成功')
        getCdnPrefixList()
      } catch (error) {
        console.error('设置默认CDN失败:', error)
        ElMessage.error('设置默认CDN失败')
      }
    }

    // 删除CDN前缀
    const deleteCdnPrefix = async (id) => {
      try {
        await ElMessageBox.confirm('确定要删除这个CDN前缀吗？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        
        await cdnPrefixAPI.deletePrefix(id)
        ElMessage.success('CDN前缀删除成功')
        getCdnPrefixList()
      } catch (error) {
        if (error !== 'cancel') {
          console.error('删除CDN前缀失败:', error)
          ElMessage.error('删除CDN前缀失败')
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

    onMounted(() => {
      getFileList()
      getCdnPrefixList()
      
      // 全局抑制 ResizeObserver 警告
      const resizeObserverErrorHandler = (e) => {
        if (e.message && e.message.includes('ResizeObserver')) {
          e.stopImmediatePropagation()
        }
      }
      window.addEventListener('error', resizeObserverErrorHandler)
      window.addEventListener('unhandledrejection', resizeObserverErrorHandler)
    })

    return {
      loading,
      uploading,
      searchKeyword,
      fileList,
      selectedFiles,
      currentPage,
      pageSize,
      total,
      showUploadDialog,
      uploadRef,
      uploadForm,
      showCdnDialog,
      showCdnManageDialog,
      cdnFormRef,
      cdnPrefixList,
      cdnForm,
      cdnRules,
      getFileList,
      handleSearch,
      resetSearch,
      handleSizeChange,
      handleCurrentChange,
      handleSelectionChange,
      handleFileChange,
      handleUpload,
      handleUploadDialogClose,
      handleStatusChange,
      handleDelete,
      handleBatchDelete,
      copyUrl,
      previewFile,
      downloadFile,
      handleCdnManageDialogClose,
      handleCdnDialogClose,
      editCdnPrefix,
      handleCdnSubmit,
      setDefaultCdn,
      deleteCdnPrefix,
      isDocument,
      isImage,
      formatFileSize,
      formatDateTime
    }
  }
}
</script>

<style scoped>
.resource-manage {
  padding: 20px;
  width: 100%;
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

.header-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.search-bar {
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.content {
  background: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  width: 100%;
  box-sizing: border-box;
  overflow-x: auto;
}

.file-name {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
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
  flex-wrap: wrap;
}

.cdn-config {
  background: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  width: 100%;
  box-sizing: border-box;
}

.cdn-config h3 {
  margin: 0 0 20px 0;
  color: #333;
}

.el-table {
  margin-bottom: 20px;
  width: 100%;
  min-width: 800px;
}

.el-button {
  margin-right: 8px;
}

.el-button:last-child {
  margin-right: 0;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
}

.action-buttons {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
  justify-content: flex-start;
}

.action-buttons .el-button {
  margin-right: 0;
  margin-bottom: 4px;
}

/* 响应式布局 */
@media screen and (max-width: 1200px) {
  .resource-manage {
    padding: 15px;
  }
  
  .header {
    flex-direction: column;
    align-items: flex-start;
    gap: 15px;
  }
  
  .header-actions {
    width: 100%;
    justify-content: flex-start;
  }
  
  .search-bar {
    flex-direction: column;
    align-items: stretch;
  }
  
  .search-bar .el-input {
    width: 100% !important;
    margin-right: 0 !important;
  }
}

@media screen and (max-width: 768px) {
  .resource-manage {
    padding: 10px;
  }
  
  .header {
    padding: 0 5px;
  }
  
  .header h2 {
    font-size: 18px;
  }
  
  .header-actions {
    flex-direction: column;
    width: 100%;
  }
  
  .header-actions .el-button {
    width: 100%;
    margin-right: 0;
    margin-bottom: 8px;
  }
  
  .content {
    padding: 15px 10px;
    margin: 0 -10px;
    border-radius: 0;
  }
  
  .el-table {
    font-size: 14px;
    min-width: 600px;
  }
  
  .el-table__body-wrapper {
    overflow-x: auto;
  }
  
  .action-buttons {
    gap: 2px;
  }
  
  .action-buttons .el-button {
    padding: 5px 8px;
    font-size: 12px;
  }
  
  .pagination {
    margin-top: 15px;
    flex-wrap: wrap;
    justify-content: center;
  }
  
  .el-pagination {
    flex-wrap: wrap;
    justify-content: center;
  }
  
  .el-pagination__sizes,
  .el-pagination__jumper {
    margin-top: 8px;
  }
}

@media screen and (max-width: 480px) {
  .resource-manage {
    padding: 5px;
  }
  
  .header {
    padding: 0 2px;
    gap: 8px;
  }
  
  .header h2 {
    font-size: 16px;
  }
  
  .content {
    padding: 10px 5px;
    margin: 0 -5px;
  }
  
  .el-table {
    font-size: 12px;
    min-width: 500px;
  }
  
  .action-buttons {
    flex-direction: column;
    gap: 2px;
  }
  
  .action-buttons .el-button {
    width: 100%;
    margin-bottom: 2px;
    padding: 6px 8px;
    font-size: 11px;
  }
  
  .file-name {
    flex-direction: column;
    align-items: flex-start;
    gap: 4px;
  }
}

/* 弹性布局优化 */
.resource-manage > * {
  max-width: 100%;
  box-sizing: border-box;
}

/* 响应式表格容器 */
.responsive-table {
  width: 100%;
  transition: all 0.3s ease;
}

/* 表格响应式滚动 - 修复ResizeObserver警告 */
@media screen and (max-width: 768px) {
  .content {
    overflow-x: auto;
    position: relative;
    -webkit-overflow-scrolling: touch;
  }
  
  .responsive-table {
    min-width: 600px;
    width: 100%;
    table-layout: auto;
  }
  
  .el-table__header-wrapper,
  .el-table__body-wrapper {
    min-width: 100%;
  }
}

/* 抑制ResizeObserver警告 */
.resource-manage * {
  box-sizing: border-box;
}

/* 防止表格容器抖动和ResizeObserver循环 */
.el-table__body-wrapper {
  overflow-anchor: none;
  will-change: transform;
  contain: layout style;
}

/* 全局抑制ResizeObserver警告 */
:global(.resize-observer-error) {
  display: none !important;
}

/* 平滑过渡效果 */
.el-table {
  transition: none !important;
}

/* 防止表格尺寸计算循环 */
.responsive-table {
  width: 100%;
  table-layout: fixed;
}

/* 移动端优化 */
@media screen and (max-width: 480px) {
  .responsive-table {
    font-size: 12px;
    min-width: 500px;
  }
  
  .el-table__cell {
    padding: 8px 4px;
  }
}

/* 强制固定布局防止抖动 */
.el-table__header,
.el-table__body {
  width: 100% !important;
}

/* 隐藏webpack-dev-server的overlay错误 */
#webpack-dev-server-client-overlay {
  display: none !important;
}
</style>