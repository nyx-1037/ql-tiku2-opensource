
<template>
  <div class="feedback-center">
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
      <h2>反馈中心</h2>
      <t-button theme="primary" @click="showCreateDialog = true">
        <span class="icon">➕</span>
        提交反馈
      </t-button>
    </div>

    <!-- 筛选条件 -->
    <div class="filter-section">
      <div class="filter-form">
        <div class="filter-item">
          <label class="filter-label">反馈类型</label>
          <Multiselect
            v-model="queryForm.feedbackType"
            :options="[
              { value: null, label: '全部' },
              { value: 1, label: 'Bug反馈' },
              { value: 2, label: '功能建议' },
              { value: 3, label: '其他反馈' }
            ]"
            value-prop="value"
            label="label"
            placeholder="全部类型"
            :can-clear="true"
            :classes="{
              container: 'feedback-multiselect-container',
              dropdown: 'feedback-select-dropdown'
            }"
            style="width: 180px;"
          />
        </div>
        <div class="filter-item">
          <label class="filter-label">状态</label>
          <Multiselect
            v-model="queryForm.status"
            :options="statusOptions"
            value-prop="value"
            label="label"
            placeholder="全部状态"
            :can-clear="true"
            :classes="{
              container: 'feedback-multiselect-container',
              dropdown: 'feedback-select-dropdown'
            }"
            style="width: 180px;"
          />
        </div>
        <div class="filter-item">
          <label class="filter-label">关键词</label>
          <t-input v-model="queryForm.keyword" placeholder="搜索标题或内容" clearable style="width: 180px;" />
        </div>
        <div class="filter-buttons">
          <t-button theme="primary" @click="loadFeedbacks" :loading="isLoading">查询</t-button>
          <t-button @click="resetQuery">重置</t-button>
        </div>
      </div>
    </div>

    <!-- 反馈列表 -->
    <div class="feedback-list">
      <!-- 自定义表格 -->
      <div v-if="feedbackList.length > 0" class="simple-table">
        <div class="table-header">
          <div class="header-cell">ID</div>
          <div class="header-cell">类型</div>
          <div class="header-cell">标题</div>
          <div class="header-cell">内容</div>
          <div class="header-cell">状态</div>
          <div class="header-cell">提交时间</div>
          <div class="header-cell">更新时间</div>
          <div class="header-cell">操作</div>
        </div>
        <div
          v-for="feedback in feedbackList"
          :key="feedback.id"
          class="table-row"
        >
          <div class="table-cell">{{ feedback.id }}</div>
          <div class="table-cell">{{ feedback.feedbackTypeName }}</div>
          <div class="table-cell title-cell">{{ feedback.title }}</div>
          <div class="table-cell content-cell">{{ feedback.content }}</div>
          <div class="table-cell">
            <t-tag :theme="getStatusTagType(feedback.status)">
              {{ getStatusText(feedback.status) }}
            </t-tag>
          </div>
          <div class="table-cell">{{ feedback.createTime }}</div>
          <div class="table-cell">{{ feedback.updateTime }}</div>
          <div class="table-cell action-cell">
            <div class="action-buttons">
              <t-button size="small" @click="viewFeedback(feedback)">查看</t-button>
              <t-button
                v-if="feedback.status === 0"
                size="small"
                theme="warning"
                @click="editFeedback(feedback)"
              >
                编辑
              </t-button>
              <t-button
                v-if="feedback.status === 0"
                size="small"
                theme="danger"
                @click="deleteFeedback(feedback)"
              >
                删除
              </t-button>
            </div>
          </div>
        </div>
      </div>

      <!-- 如果没有数据显示空状态 -->
      <div v-if="feedbackList.length === 0 && !loading" class="empty-state">
        <t-empty description="暂无反馈数据" />
      </div>

      <!-- 分页 -->
      <div class="pagination">
        <CustomPagination
          :current="queryForm.current"
          :page-size="queryForm.size"
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

    <!-- 创建/编辑反馈对话框 -->
    <t-dialog
      :visible="showCreateDialog"
      :title="editingFeedback ? '编辑反馈' : '提交反馈'"
      width="600px"
      @close="resetForm"
      @confirm="submitFeedback"
      @cancel="resetForm"
    >
      <t-form :model="feedbackForm" :rules="formRules" ref="feedbackFormRef" label-width="80px">
        <t-form-item label="反馈类型" prop="feedbackType">
          <t-radio-group v-model="feedbackForm.feedbackType" class="feedback-type-radio-group">
            <t-radio
              v-for="type in feedbackTypes"
              :key="type.value"
              :value="type.value"
              class="feedback-type-radio"
            >
              {{ type.label }}
            </t-radio>
          </t-radio-group>
        </t-form-item>
        <t-form-item label="标题" prop="title">
          <t-input v-model="feedbackForm.title" placeholder="请输入反馈标题" />
        </t-form-item>
        <t-form-item label="内容" prop="content">
          <t-input
            v-model="feedbackForm.content" 
            type="textarea" 
            :rows="5" 
            placeholder="请详细描述您的反馈内容"
          />
        </t-form-item>
        <t-form-item label="图片">
          <t-upload
            :action="uploadUrl"
            :headers="uploadHeaders"
            :file-list="fileList"
            @success="handleUploadSuccess"
            @fail="handleUploadError"
            @remove="handleRemoveFile"
            :before-upload="beforeUpload"
            theme="image"
            accept="image/*"
            :max="5"
          >
            <span class="icon">➕</span>
          </t-upload>
          <div class="upload-tip">支持 jpg、png、gif 格式，单个文件不超过 5MB</div>
        </t-form-item>
      </t-form>
      <template #footer>
        <t-button @click="showCreateDialog = false">取消</t-button>
        <t-button theme="primary" @click="submitFeedback" :loading="submitting">
          {{ editingFeedback ? '更新' : '提交' }}
        </t-button>
      </template>
    </t-dialog>

    <!-- 查看反馈详情对话框 -->
    <t-dialog :visible="showDetailDialog" title="反馈详情" width="700px" @close="showDetailDialog = false">
      <div v-if="currentFeedback" class="feedback-detail">
        <div class="detail-item">
          <label>反馈类型：</label>
          <span>{{ currentFeedback.feedbackTypeName }}</span>
        </div>
        <div class="detail-item">
          <label>状态：</label>
          <t-tag :type="getStatusTagType(currentFeedback.status)">{{ getStatusText(currentFeedback.status) }}</t-tag>
        </div>
        <div class="detail-item">
          <label>标题：</label>
          <span>{{ currentFeedback.title }}</span>
        </div>
        <div class="detail-item">
          <label>内容：</label>
          <div class="content">{{ currentFeedback.content }}</div>
        </div>
        <div v-if="currentFeedback.imageList && currentFeedback.imageList.length > 0" class="detail-item">
          <label>图片：</label>
          <div class="image-list">
            <t-image
              v-for="(image, index) in currentFeedback.imageList" 
              :key="index"
              :src="image" 
              :preview-src-list="currentFeedback.imageList"
              :initial-index="index"
              fit="cover"
              class="feedback-image"
            />
          </div>
        </div>
        <div v-if="currentFeedback.adminReply" class="detail-item">
          <label>管理员回复：</label>
          <div class="admin-reply">{{ currentFeedback.adminReply }}</div>
        </div>
        <div class="detail-item">
          <label>提交时间：</label>
          <span>{{ currentFeedback.createTime }}</span>
        </div>
        <div class="detail-item">
          <label>更新时间：</label>
          <span>{{ currentFeedback.updateTime }}</span>
        </div>
      </div>
    </t-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed, watch } from 'vue'
import { MessagePlugin, DialogPlugin } from 'tdesign-vue-next'
// TDesign icons imported individually
import { getToken } from '@/api/auth'
import { useFeedbackCenterStore } from '@/store/feedbackCenter'
import { useLoadingState } from '@/composables/useLoadingState'
import { useErrorHandler } from '@/composables/useErrorHandler'
import Multiselect from '@vueform/multiselect'
import CustomPagination from '../components/CustomPagination.vue'
import LoadingStates from '@/components/LoadingStates.vue'

// 使用缓存store
const feedbackStore = useFeedbackCenterStore()

// 使用加载状态管理
const { 
  isLoading, 
  error, 
  retryCount, 
  maxRetries, 
  handleRetry, 
  clearError,
  setLoading,
  setError
} = useLoadingState()

// 响应式数据
const showCreateDialog = ref(false)
const showDetailDialog = ref(false)
const editingFeedback = ref(null)
const currentFeedback = ref(null)
const fileList = ref([])
const feedbackFormRef = ref()

// 从store获取数据
const feedbackList = computed(() => feedbackStore.feedbackList || [])
const total = computed(() => feedbackStore.total || 0)
const loading = computed(() => feedbackStore.loading || false)
const submitting = computed(() => feedbackStore.submitting || false)
const queryForm = computed(() => feedbackStore.queryForm || {})
const feedbackTypes = computed(() => feedbackStore.feedbackTypes || [
  { value: 1, label: 'Bug反馈' },
  { value: 2, label: '功能建议' },
  { value: 3, label: '其他反馈' }
])

// 状态选项
const statusOptions = computed(() => [
  { value: null, label: '全部' },
  { value: 0, label: '待处理' },
  { value: 1, label: '已受理' },
  { value: 2, label: '已处理' },
  { value: 3, label: '已修复' },
  { value: 4, label: '已采纳' },
  { value: 5, label: '已失效' },
  { value: 6, label: '已撤销' }
])

// 反馈表单
const feedbackForm = reactive({
  feedbackType: 1, // 默认选中第一个
  title: '',
  content: '',
  imageList: []
})

// 表单验证规则
const formRules = {
  feedbackType: [{ required: true, message: '请选择反馈类型', trigger: 'change' }],
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入反馈内容', trigger: 'blur' }]
}

// 上传配置
const uploadUrl = ref('/api/feedback/upload/image')
const uploadHeaders = ref({
  'Authorization': `Bearer ${getToken()}`
})

// 加载反馈列表 - 使用store方法
const loadFeedbacks = () => {
  feedbackStore.loadFeedbacks()
}

// 重置查询 - 使用store方法
const resetQuery = () => {
  feedbackStore.resetQuery()
}

// 分页处理方法 - 使用store方法
const handleCurrentChange = (page) => {
  feedbackStore.handleCurrentChange(page)
}

const handleSizeChange = (size) => {
  feedbackStore.handleSizeChange(size)
}

// 监听查询表单变化，自动搜索
watch(() => [queryForm.value.feedbackType, queryForm.value.status, queryForm.value.keyword], 
  () => {
    // 重置到第一页并搜索
    feedbackStore.queryForm.current = 1
    loadFeedbacks()
  }, 
  { deep: true }
)

// 创建反馈
const createFeedback = () => {
  resetForm()
  showCreateDialog.value = true
}

// 查看反馈详情
const viewFeedback = (feedback) => {
  console.log('📝 FeedbackCenter.vue: 查看反馈详情', feedback)
  currentFeedback.value = feedback
  showDetailDialog.value = true
  console.log('📝 FeedbackCenter.vue: 详情模态框状态', showDetailDialog.value)
}

// 编辑反馈
const editFeedback = (feedback) => {
  console.log('🔍 Editing feedback:', feedback)
  console.log('🔍 Feedback imageList:', feedback.imageList)

  editingFeedback.value = feedback
  feedbackForm.feedbackType = feedback.feedbackType
  feedbackForm.title = feedback.title
  feedbackForm.content = feedback.content
  feedbackForm.imageList = feedback.imageList || []

  // 设置文件列表 - 修复TDesign上传组件的文件格式
  fileList.value = (feedback.imageList || []).map((url, index) => ({
    uid: `existing_${index}_${Date.now()}`, // 添加唯一ID
    name: `image_${index}.jpg`, // 添加文件名
    url: url, // 图片URL
    status: 'success', // 设置状态为成功
    size: 0, // 文件大小（编辑时不重要）
    type: 'image/jpeg', // 文件类型
    response: { code: 200, data: url } // 模拟响应数据
  }))

  console.log('🔍 Generated fileList:', fileList.value)

  showCreateDialog.value = true
}

// 删除反馈
const deleteFeedback = async (feedback) => {
  try {
    await DialogPlugin.confirm({
      header: '确认删除',
      body: '确定要删除这条反馈吗？',
      theme: 'warning'
    })
    
    const response = await request.delete(`/feedback/${feedback.id}`)
    if (response) {
      MessagePlugin.success('删除成功')
      loadFeedbacks()
    } else {
      MessagePlugin.error('删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      MessagePlugin.error('删除失败')
      console.error(error)
    }
  }
}

// 提交反馈 - 使用store方法
const submitFeedback = async () => {
  try {
    await feedbackFormRef.value.validate()

    const data = {
      ...feedbackForm,
      imageList: feedbackForm.imageList
    }

    console.log('🔍 Submitting feedback data:', data)

    let response
    if (editingFeedback.value) {
      response = await feedbackStore.updateFeedback(editingFeedback.value.id, data)
    } else {
      response = await feedbackStore.submitFeedback(data)
    }

    console.log('🔍 Submit response:', response)

    if (response) {
      MessagePlugin.success(editingFeedback.value ? '更新成功' : '提交成功')
      showCreateDialog.value = false
      resetForm()
    } else {
      MessagePlugin.error('操作失败')
    }
  } catch (error) {
    MessagePlugin.error('操作失败')
    console.error('🔍 Submit error:', error)
  }
}

// 重置表单
const resetForm = () => {
  editingFeedback.value = null
  feedbackForm.feedbackType = 1
  feedbackForm.title = ''
  feedbackForm.content = ''
  feedbackForm.imageList = []
  fileList.value = []
  showCreateDialog.value = false
  if (feedbackFormRef.value) {
    feedbackFormRef.value.clearValidate()
  }
}

// 上传前检查
const beforeUpload = (file) => {
  const isImage = file.type.startsWith('image/')
  const isLt5M = file.size / 1024 / 1024 < 5
  
  if (!isImage) {
    MessagePlugin.error('只能上传图片文件')
    return false
  }
  if (!isLt5M) {
    MessagePlugin.error('图片大小不能超过 5MB')
    return false
  }
  return true
}

// 上传成功 - TDesign格式
const handleUploadSuccess = (context) => {
  console.log('🔍 TDesign Upload context:', context)

  // TDesign的上传成功事件参数格式
  const { response, file } = context

  console.log('🔍 Upload response:', response)
  console.log('🔍 Upload file:', file)
  console.log('🔍 Current imageList before:', [...feedbackForm.imageList])

  // 检查响应格式
  if (response && response.code === 200) {
    const imageUrl = response.data
    if (imageUrl) {
      feedbackForm.imageList.push(imageUrl)
      // 更新文件列表中的URL
      const fileIndex = fileList.value.findIndex(f => f.uid === file.uid)
      if (fileIndex > -1) {
        fileList.value[fileIndex].url = imageUrl
      }
      MessagePlugin.success('图片上传成功')
      console.log('🔍 Image uploaded successfully:', imageUrl)
      console.log('🔍 Current imageList after:', [...feedbackForm.imageList])
    } else {
      MessagePlugin.error('上传成功但未返回图片URL')
      console.error('🔍 No imageUrl in response.data:', response.data)
    }
  } else {
    const errorMsg = response?.message || response?.msg || '图片上传失败'
    MessagePlugin.error(errorMsg)
    console.error('🔍 Upload failed - response:', response)
  }
}

// 上传失败 - TDesign格式
const handleUploadError = (context) => {
  console.error('🔍 TDesign Upload error context:', context)

  const { error, file, response } = context
  console.error('🔍 Upload error:', error)
  console.error('🔍 Upload file:', file)
  console.error('🔍 Upload response:', response)

  const errorMsg = response?.message || error?.message || '图片上传失败，请重试'
  MessagePlugin.error(errorMsg)
}

// 移除文件 - TDesign格式
const handleRemoveFile = (context) => {
  console.log('🔍 TDesign Remove context:', context)

  // TDesign可能传递不同的参数格式
  const file = context.file || context
  console.log('🔍 Removing file:', file)
  console.log('🔍 Current imageList before remove:', [...feedbackForm.imageList])

  // 从图片列表中移除
  if (file.url) {
    const index = feedbackForm.imageList.indexOf(file.url)
    if (index > -1) {
      feedbackForm.imageList.splice(index, 1)
      console.log('🔍 Removed from imageList:', file.url)
    }
  }

  // 从文件列表中移除 - 不需要手动移除，TDesign会自动处理
  console.log('🔍 Current imageList after remove:', [...feedbackForm.imageList])
}

// 获取状态标签类型
const getStatusTagType = (status) => {
  // 处理 null、undefined、空字符串等情况
  if (status === null || status === undefined || status === '') {
    return 'default'
  }

  const statusValue = parseInt(status)
  if (isNaN(statusValue)) {
    return 'default'
  }

  const typeMap = {
    0: 'default',
    1: 'warning',
    2: 'success',
    3: 'success',
    4: 'success',
    5: 'info',
    6: 'danger'
  }
  return typeMap[statusValue] || 'default'
}

// 获取状态文本
const getStatusText = (status) => {
  // 处理 null、undefined、空字符串等情况
  if (status === null || status === undefined || status === '') {
    return '未知'
  }

  // 处理字符串类型的状态值
  const statusValue = parseInt(status)

  // 检查是否为有效数字
  if (isNaN(statusValue)) {
    return '未知'
  }

  const textMap = {
    0: '待处理',
    1: '已受理',
    2: '已处理',
    3: '已修复',
    4: '已采纳',
    5: '已失效',
    6: '已撤销'
  }

  return textMap[statusValue] || '未知'
}

// 组件挂载时初始化数据 - 使用store的初始化方法
onMounted(async () => {
  await feedbackStore.initialize()
})
</script>

<style scoped>
.feedback-center {
  padding: 20px;
  min-height: 100vh;
  max-width: 100%;
  overflow-x: auto;
}

@media (max-width: 768px) {
  .feedback-center {
    padding: 10px;
  }
}

@media (max-width: 480px) {
  .feedback-center {
    padding: 5px;
  }
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

@media (max-width: 768px) {
  .header {
    flex-direction: column;
    align-items: flex-start;
  }

  .header h2 {
    margin-bottom: 10px;
  }
}

.header h2 {
  margin: 0;
  color: #303133;
}

.filter-section {
  background: #f5f7fa;
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 20px;
}

@media (max-width: 768px) {
  .filter-section {
    padding: 15px;
  }
  
  .filter-section .t-form-item {
    margin-bottom: 15px;
  }
}

@media (max-width: 480px) {
  .filter-section {
    padding: 10px;
  }
}

.feedback-list {
  background: white;
  border-radius: 8px;
  padding: 20px;
  overflow-x: auto;
}

@media (max-width: 768px) {
  .feedback-list {
    padding: 15px;
  }
}

@media (max-width: 480px) {
  .feedback-list {
    padding: 10px;
  }
}

.pagination {
  margin-top: 20px;
  text-align: right;
}

@media (max-width: 768px) {
  .pagination {
    text-align: center;
  }
}

.feedback-detail {
  padding: 10px 0;
}

.detail-item {
  margin-bottom: 15px;
  display: flex;
  align-items: flex-start;
}

.detail-item label {
  font-weight: bold;
  min-width: 100px;
  color: #606266;
}

.detail-item .content,
.detail-item .admin-reply {
  flex: 1;
  padding: 10px;
  background: #f5f7fa;
  border-radius: 4px;
  white-space: pre-wrap;
  word-break: break-word;
}

.detail-item .admin-reply {
  background: #e8f4fd;
  border-left: 4px solid #409eff;
}

.image-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.feedback-image {
  width: 100px;
  height: 100px;
  border-radius: 4px;
  cursor: pointer;
}

.upload-tip {
  color: #909399;
  font-size: 12px;
  margin-top: 5px;
}

:deep(.t-upload--picture-card) {
  width: 100px;
  height: 100px;
}

:deep(.t-upload-list--picture-card .t-upload-list__item) {
  width: 100px;
  height: 100px;
}

.feedback-type-radio-group {
  display: flex;
  width: 100%;
}

.feedback-type-radio-button {
  flex: 1;
  margin: 0 5px;
}

/* 反馈类型单选框样式 */
.feedback-type-radio-group {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.feedback-type-radio {
  margin-right: 16px;
  margin-bottom: 8px;
}

/* 调整按钮内部填充 */
.feedback-type-radio-button :deep(.t-radio-button__inner) {
  width: 100%;
  border-radius: 4px !important;
  border-left: 1px solid var(--t-border-color) !important;
}

.feedback-type-radio-button:first-child {
  margin-left: 0;
}

.feedback-type-radio-button:last-child {
  margin-right: 0;
}

/* FeedbackCenter Multiselect 自定义样式 */
:deep(.feedback-multiselect-container) {
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  min-height: 32px;
  background: white;
  transition: all 0.2s;
  position: relative;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  box-sizing: border-box;
}

:deep(.feedback-multiselect-container:hover) {
  border-color: #4dabf7;
}

:deep(.feedback-multiselect-container.is-active) {
  border-color: #0052d9;
  box-shadow: 0 0 0 2px rgba(0, 82, 217, 0.1);
}

:deep(.feedback-select-dropdown) {
  border: 1px solid #e6e6e6;
  border-radius: 6px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  z-index: 99999 !important;
  background: white;
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  max-height: 200px;
  overflow-y: auto;
}

:deep(.feedback-select-dropdown.is-hidden) {
  display: none !important;
}

/* 确保 FeedbackCenter 页面的 multiselect 基础样式正确 */
:deep(.feedback-multiselect-container .multiselect) {
  min-height: 32px;
  height: 32px;
  width: 100%;
}

:deep(.feedback-multiselect-container .multiselect-single-label) {
  padding-left: 12px;
  padding-right: 40px;
  line-height: 30px;
}

:deep(.feedback-multiselect-container .multiselect-placeholder) {
  padding-left: 12px;
  line-height: 30px;
  color: #bbb;
}

:deep(.feedback-multiselect-container .multiselect-caret) {
  margin-right: 12px;
}

/* 修复 FeedbackCenter 页面可能的样式冲突 */
.feedback-center-page :deep(.multiselect) {
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif !important;
  font-size: 14px !important;
  line-height: 1.5 !important;
}

/* 筛选表单样式 */
.filter-form {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  align-items: flex-end;
  padding: 16px;
}

.filter-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.filter-label {
  font-size: 14px;
  color: #606266;
  font-weight: 500;
  margin-bottom: 4px;
}

.filter-buttons {
  display: flex;
  gap: 8px;
  align-items: flex-end;
}

.filter-buttons .t-button {
  height: 32px;
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
  grid-template-columns: 80px 120px 2fr 2fr 100px 1.5fr 1.5fr 2fr;
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
  grid-template-columns: 80px 120px 2fr 2fr 100px 1.5fr 1.5fr 2fr;
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

.title-cell,
.content-cell {
  text-align: left;
  justify-content: flex-start;
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

/* 响应式设计 */
@media (max-width: 768px) {
  .filter-form {
    flex-direction: column;
    gap: 12px;
    align-items: stretch;
  }

  .filter-item {
    width: 100%;
  }

  .filter-buttons {
    justify-content: center;
    margin-top: 8px;
  }

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

  .action-cell {
    align-items: flex-start;
  }

  .action-buttons {
    justify-content: flex-start;
  }
}
</style>

<style src="@vueform/multiselect/themes/default.css"></style>