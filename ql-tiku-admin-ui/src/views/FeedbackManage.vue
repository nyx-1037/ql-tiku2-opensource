<template>
  <div class="feedback-manage">
    <div class="page-header">
      <h2>反馈管理</h2>
      <div class="header-actions">
        <el-button type="primary" @click="showStatistics = true">
          <el-icon><DataAnalysis /></el-icon>
          统计信息
        </el-button>
        <el-button type="danger" @click="batchDelete" :disabled="selectedIds.length === 0">
          <el-icon><Delete /></el-icon>
          批量删除
        </el-button>
      </div>
    </div>

    <!-- 筛选条件 -->
    <div class="filter-section">
      <el-form :model="queryForm" inline>
        <el-form-item label="反馈类型">
          <el-select v-model="queryForm.feedbackType" placeholder="全部类型" clearable>
            <el-option label="全部" :value="null" />
            <el-option label="Bug反馈" :value="1" />
            <el-option label="功能建议" :value="2" />
            <el-option label="其他反馈" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" placeholder="全部状态" clearable>
            <el-option label="全部" :value="null" />
            <el-option label="待处理" :value="0" />
            <el-option label="已受理" :value="1" />
            <el-option label="已处理" :value="2" />
            <el-option label="已修复" :value="3" />
            <el-option label="已采纳" :value="4" />
            <el-option label="已失效" :value="5" />
            <el-option label="已撤销" :value="6" />
          </el-select>
        </el-form-item>
        <el-form-item label="用户名">
          <el-input v-model="queryForm.username" placeholder="搜索用户名" clearable />
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="queryForm.keyword" placeholder="搜索标题或内容" clearable />
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="dateRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
            @change="handleDateChange"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadFeedbacks">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 反馈列表 -->
    <div class="feedback-list">
      <el-table 
        :data="feedbackList" 
        v-loading="loading" 
        stripe
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户" width="120" />
        <el-table-column prop="feedbackTypeName" label="类型" width="120" />
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="content" label="内容" min-width="250" show-overflow-tooltip />
        <el-table-column prop="statusName" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.status)">{{ row.statusName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="提交时间" width="180" />
        <el-table-column prop="updateTime" label="更新时间" width="180" />
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="viewFeedback(row)">查看</el-button>
            <el-button size="small" type="warning" @click="handleFeedback(row)">处理</el-button>
            <el-button size="small" type="danger" @click="deleteFeedback(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination">
        <el-pagination
          v-model:current-page="queryForm.current"
          v-model:page-size="queryForm.size"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadFeedbacks"
          @current-change="loadFeedbacks"
        />
      </div>
    </div>

    <!-- 查看反馈详情对话框 -->
    <el-dialog v-model="showDetailDialog" title="反馈详情" width="800px">
      <div v-if="currentFeedback" class="feedback-detail">
        <div class="detail-row">
          <div class="detail-item">
            <label>用户：</label>
            <span>{{ currentFeedback.username }}</span>
          </div>
          <div class="detail-item">
            <label>反馈类型：</label>
            <span>{{ currentFeedback.feedbackTypeName }}</span>
          </div>
          <div class="detail-item">
            <label>状态：</label>
            <el-tag :type="getStatusTagType(currentFeedback.status)">{{ currentFeedback.statusName }}</el-tag>
          </div>
        </div>
        <div class="detail-item full-width">
          <label>标题：</label>
          <span>{{ currentFeedback.title }}</span>
        </div>
        <div class="detail-item full-width">
          <label>内容：</label>
          <div class="content">{{ currentFeedback.content }}</div>
        </div>
        <div v-if="currentFeedback.imageList && currentFeedback.imageList.length > 0" class="detail-item full-width">
          <label>图片：</label>
          <div class="image-list">
            <el-image 
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
        <div v-if="currentFeedback.adminReply" class="detail-item full-width">
          <label>管理员回复：</label>
          <div class="admin-reply">{{ currentFeedback.adminReply }}</div>
        </div>
        <div class="detail-row">
          <div class="detail-item">
            <label>提交时间：</label>
            <span>{{ currentFeedback.createTime }}</span>
          </div>
          <div class="detail-item">
            <label>更新时间：</label>
            <span>{{ currentFeedback.updateTime }}</span>
          </div>
        </div>
      </div>
    </el-dialog>

    <!-- 处理反馈对话框 -->
    <el-dialog v-model="showHandleDialog" title="处理反馈" width="600px">
      <el-form :model="handleForm" :rules="handleRules" ref="handleFormRef" label-width="80px">
        <el-form-item label="当前状态">
          <el-tag :type="getStatusTagType(handleForm.currentStatus)">{{ getStatusName(handleForm.currentStatus) }}</el-tag>
        </el-form-item>
        <el-form-item label="新状态" prop="status">
          <el-select v-model="handleForm.status" placeholder="请选择新状态">
            <el-option label="待处理" :value="0" />
            <el-option label="已受理" :value="1" />
            <el-option label="已处理" :value="2" />
            <el-option label="已修复" :value="3" />
            <el-option label="已采纳" :value="4" />
            <el-option label="已失效" :value="5" />
            <el-option label="已撤销" :value="6" />
          </el-select>
        </el-form-item>
        <el-form-item label="管理员回复">
          <el-input 
            v-model="handleForm.adminReply" 
            type="textarea" 
            :rows="4" 
            placeholder="请输入回复内容（可选）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showHandleDialog = false">取消</el-button>
        <el-button type="primary" @click="submitHandle" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>

    <!-- 统计信息对话框 -->
    <el-dialog v-model="showStatistics" title="反馈统计" width="700px">
      <div v-if="statistics" class="statistics-content">
        <div class="stat-card">
          <h3>总体统计</h3>
          <div class="stat-item">
            <span class="stat-label">总反馈数：</span>
            <span class="stat-value">{{ statistics.totalCount }}</span>
          </div>
        </div>
        
        <div class="stat-card">
          <h3>状态分布</h3>
          <div class="stat-grid">
            <div v-for="(count, status) in statistics.statusStats" :key="status" class="stat-item">
              <span class="stat-label">{{ status }}：</span>
              <span class="stat-value">{{ count }}</span>
            </div>
          </div>
        </div>
        
        <div class="stat-card">
          <h3>类型分布</h3>
          <div class="stat-grid">
            <div v-for="(count, type) in statistics.typeStats" :key="type" class="stat-item">
              <span class="stat-label">{{ type }}：</span>
              <span class="stat-value">{{ count }}</span>
            </div>
          </div>
        </div>
      </div>
    </el-dialog>

    <!-- 批量操作对话框 -->
    <el-dialog v-model="showBatchDialog" title="批量操作" width="500px">
      <el-form :model="batchForm" label-width="80px">
        <el-form-item label="操作类型">
          <el-radio-group v-model="batchForm.action">
            <el-radio label="status">更新状态</el-radio>
            <el-radio label="delete">删除</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="batchForm.action === 'status'" label="新状态">
          <el-select v-model="batchForm.status" placeholder="请选择状态">
            <el-option label="待处理" :value="0" />
            <el-option label="已受理" :value="1" />
            <el-option label="已处理" :value="2" />
            <el-option label="已修复" :value="3" />
            <el-option label="已采纳" :value="4" />
            <el-option label="已失效" :value="5" />
            <el-option label="已撤销" :value="6" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="batchForm.action === 'status'" label="回复内容">
          <el-input v-model="batchForm.adminReply" type="textarea" :rows="3" placeholder="批量回复内容（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showBatchDialog = false">取消</el-button>
        <el-button type="primary" @click="submitBatch" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { DataAnalysis, Delete } from '@element-plus/icons-vue'
import request from '@/api'

// 响应式数据
const loading = ref(false)
const submitting = ref(false)
const showDetailDialog = ref(false)
const showHandleDialog = ref(false)
const showStatistics = ref(false)
const showBatchDialog = ref(false)
const currentFeedback = ref(null)
const feedbackList = ref([])
const total = ref(0)
const selectedIds = ref([])
const statistics = ref(null)
const dateRange = ref([])
const handleFormRef = ref()

// 查询表单
const queryForm = reactive({
  current: 1,
  size: 10,
  feedbackType: null,
  status: null,
  username: '',
  keyword: '',
  startTime: '',
  endTime: ''
})

// 处理表单
const handleForm = reactive({
  id: null,
  currentStatus: null,
  status: null,
  adminReply: ''
})

// 批量操作表单
const batchForm = reactive({
  action: 'status',
  status: null,
  adminReply: ''
})

// 表单验证规则
const handleRules = {
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

// 加载反馈列表
const loadFeedbacks = async () => {
  loading.value = true
  try {
    const response = await request.post('/api/admin/feedback/page', queryForm)
    console.log('API响应:', response)
    if (response && response.records) {
      feedbackList.value = response.records
      total.value = response.total
    } else {
      ElMessage.error('加载反馈列表失败')
    }
  } catch (error) {
    ElMessage.error('加载反馈列表失败')
    console.error(error)
  } finally {
    loading.value = false
  }
}

// 重置查询
const resetQuery = () => {
  queryForm.current = 1
  queryForm.size = 10
  queryForm.feedbackType = null
  queryForm.status = null
  queryForm.username = ''
  queryForm.keyword = ''
  queryForm.startTime = ''
  queryForm.endTime = ''
  dateRange.value = []
  loadFeedbacks()
}

// 处理时间范围变化
const handleDateChange = (dates) => {
  if (dates && dates.length === 2) {
    queryForm.startTime = dates[0]
    queryForm.endTime = dates[1]
  } else {
    queryForm.startTime = ''
    queryForm.endTime = ''
  }
}

// 查看反馈详情
const viewFeedback = (feedback) => {
  currentFeedback.value = feedback
  showDetailDialog.value = true
}

// 处理反馈
const handleFeedback = (feedback) => {
  handleForm.id = feedback.id
  handleForm.currentStatus = feedback.status
  handleForm.status = feedback.status
  handleForm.adminReply = feedback.adminReply || ''
  showHandleDialog.value = true
}

// 提交处理
const submitHandle = async () => {
  try {
    await handleFormRef.value.validate()
    
    submitting.value = true
    
    const response = await request.put(`/api/admin/feedback/${handleForm.id}/status`, {
      status: handleForm.status,
      adminReply: handleForm.adminReply
    })
    
    if (response) {
      ElMessage.success('处理成功')
      showHandleDialog.value = false
      loadFeedbacks()
    } else {
      ElMessage.error('处理失败')
    }
  } catch (error) {
    ElMessage.error('处理失败')
    console.error(error)
  } finally {
    submitting.value = false
  }
}

// 删除反馈
const deleteFeedback = async (feedback) => {
  try {
    await ElMessageBox.confirm('确定要删除这条反馈吗？', '确认删除', {
      type: 'warning'
    })
    
    const response = await request.delete(`/api/admin/feedback/${feedback.id}`)
    if (response) {
      ElMessage.success('删除成功')
      loadFeedbacks()
    } else {
      ElMessage.error('删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
      console.error(error)
    }
  }
}

// 选择变化
const handleSelectionChange = (selection) => {
  selectedIds.value = selection.map(item => item.id)
}

// 批量删除
const batchDelete = async () => {
  if (selectedIds.value.length === 0) {
    ElMessage.warning('请选择要删除的反馈')
    return
  }
  
  try {
    await ElMessageBox.confirm(`确定要删除选中的 ${selectedIds.value.length} 条反馈吗？`, '确认删除', {
      type: 'warning'
    })
    
    const response = await request.delete('/api/admin/feedback/batch', {
      data: { ids: selectedIds.value }
    })
    
    if (response) {
      ElMessage.success('批量删除成功')
      selectedIds.value = []
      loadFeedbacks()
    } else {
      ElMessage.error('批量删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('批量删除失败')
      console.error(error)
    }
  }
}

// 批量操作
const submitBatch = async () => {
  if (selectedIds.value.length === 0) {
    ElMessage.warning('请选择要操作的反馈')
    return
  }
  
  try {
    submitting.value = true
    
    if (batchForm.action === 'delete') {
      await batchDelete()
    } else if (batchForm.action === 'status') {
      if (!batchForm.status && batchForm.status !== 0) {
        ElMessage.warning('请选择状态')
        return
      }
      
      const response = await request.put('/api/admin/feedback/batch/status', {
        ids: selectedIds.value,
        status: batchForm.status,
        adminReply: batchForm.adminReply
      })
      
      if (response) {
        ElMessage.success('批量更新成功')
        showBatchDialog.value = false
        selectedIds.value = []
        loadFeedbacks()
      } else {
        ElMessage.error('批量更新失败')
      }
    }
  } catch (error) {
    ElMessage.error('批量操作失败')
    console.error(error)
  } finally {
    submitting.value = false
  }
}

// 加载统计信息
const loadStatistics = async () => {
  try {
    const response = await request.get('/api/admin/feedback/statistics')
    if (response) {
      statistics.value = response
    } else {
      ElMessage.error('加载统计信息失败')
    }
  } catch (error) {
    ElMessage.error('加载统计信息失败')
    console.error(error)
  }
}

// 获取状态标签类型
const getStatusTagType = (status) => {
  const typeMap = {
    0: '',
    1: 'warning',
    2: 'success',
    3: 'success',
    4: 'success',
    5: 'info',
    6: 'danger'
  }
  return typeMap[status] || ''
}

// 获取状态名称
const getStatusName = (status) => {
  const nameMap = {
    0: '待处理',
    1: '已受理',
    2: '已处理',
    3: '已修复',
    4: '已采纳',
    5: '已失效',
    6: '已撤销'
  }
  return nameMap[status] || '未知'
}

// 监听统计对话框显示
const watchStatistics = () => {
  if (showStatistics.value && !statistics.value) {
    loadStatistics()
  }
}

// 组件挂载时加载数据
onMounted(() => {
  loadFeedbacks()
})

// 监听统计对话框
watch(() => showStatistics.value, watchStatistics)
</script>

<style scoped>
.feedback-manage {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
  color: #303133;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.filter-section {
  background: #f5f7fa;
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 20px;
}

.feedback-list {
  background: white;
  border-radius: 8px;
  padding: 20px;
}

.pagination {
  margin-top: 20px;
  text-align: right;
}

.feedback-detail {
  padding: 10px 0;
}

.detail-row {
  display: flex;
  gap: 20px;
  margin-bottom: 15px;
}

.detail-item {
  display: flex;
  align-items: flex-start;
  margin-bottom: 15px;
  flex: 1;
}

.detail-item.full-width {
  flex: none;
  width: 100%;
}

.detail-item label {
  font-weight: bold;
  min-width: 100px;
  color: #606266;
  flex-shrink: 0;
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

.statistics-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.stat-card {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 16px;
}

.stat-card h3 {
  margin: 0 0 12px 0;
  color: #303133;
  font-size: 16px;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 12px;
}

.stat-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
}

.stat-label {
  color: #606266;
  font-size: 14px;
}

.stat-value {
  color: #303133;
  font-weight: 600;
  font-size: 16px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .feedback-manage {
    padding: 12px;
  }
  
  .page-header {
    flex-direction: column;
    gap: 12px;
    align-items: stretch;
  }
  
  .header-actions {
    justify-content: center;
  }
  
  .detail-row {
    flex-direction: column;
    gap: 0;
  }
  
  .stat-grid {
    grid-template-columns: 1fr;
  }
}
</style>