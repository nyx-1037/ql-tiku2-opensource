<template>
  <div class="announcement-manage">
    <!-- 页面标题 -->
    <div class="page-header">
      <h2>公告管理</h2>
      <p>管理系统公告信息</p>
    </div>

    <!-- 操作栏 -->
    <div class="toolbar">
      <el-button type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon>
        新增公告
      </el-button>
      <el-button type="danger" :disabled="!selectedIds.length" @click="handleBatchDelete">
        <el-icon><Delete /></el-icon>
        批量删除
      </el-button>
    </div>

    <!-- 搜索栏 -->
    <div class="search-bar">
      <el-form :model="searchForm" inline>
        <el-form-item label="标题">
          <el-input
            v-model="searchForm.title"
            placeholder="请输入公告标题"
            clearable
            style="width: 200px"
          />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="searchForm.type" placeholder="请选择类型" clearable style="width: 150px">
            <el-option label="普通公告" :value="1" />
            <el-option label="重要公告" :value="2" />
            <el-option label="系统维护" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="请选择状态" clearable style="width: 120px">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button @click="handleReset">
            <el-icon><Refresh /></el-icon>
            重置
          </el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 数据表格 -->
    <div class="table-container">
      <el-table
        v-loading="loading"
        :data="tableData"
        @selection-change="handleSelectionChange"
        stripe
        border
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="content" label="内容" min-width="300" show-overflow-tooltip />
        <el-table-column prop="type" label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="getTypeTagType(row.type)">{{ getTypeText(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-switch
              v-model="row.status"
              :active-value="1"
              :inactive-value="0"
              @change="handleStatusChange(row)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="priority" label="优先级" width="80" />
        <el-table-column prop="startTime" label="开始时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.startTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="endTime" label="结束时间" width="180">
          <template #default="{ row }">
            {{ row.endTime ? formatDateTime(row.endTime) : '永久有效' }}
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleEdit(row)">
              <el-icon><Edit /></el-icon>
              编辑
            </el-button>
            <el-button type="danger" size="small" @click="handleDelete(row)">
              <el-icon><Delete /></el-icon>
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 分页 -->
    <div class="pagination">
      <el-pagination
        v-model:current-page="pagination.current"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
      :before-close="handleDialogClose"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="formRules"
        label-width="80px"
      >
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入公告标题" />
        </el-form-item>
        
        <el-form-item label="内容" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="4"
            placeholder="请输入公告内容"
          />
        </el-form-item>
        
        <el-form-item label="类型" prop="type">
          <el-select v-model="form.type" placeholder="请选择类型" style="width: 100%">
            <el-option label="普通公告" :value="1" />
            <el-option label="重要公告" :value="2" />
            <el-option label="系统维护" :value="3" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="优先级" prop="priority">
          <el-input-number
            v-model="form.priority"
            :min="0"
            :max="100"
            placeholder="数字越大优先级越高"
            style="width: 100%"
          />
        </el-form-item>
        
        <el-form-item label="开始时间" prop="startTime">
          <el-date-picker
            v-model="form.startTime"
            type="datetime"
            placeholder="选择开始时间"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        
        <el-form-item label="结束时间">
          <el-date-picker
            v-model="form.endTime"
            type="datetime"
            placeholder="选择结束时间（可选）"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="handleDialogClose">取消</el-button>
          <el-button type="primary" :loading="submitLoading" @click="handleSubmit">
            确定
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminAnnouncementAPI } from '../api'

export default {
  name: 'AnnouncementManage',
  setup() {
    const loading = ref(false)
    const dialogVisible = ref(false)
    const submitLoading = ref(false)
    const formRef = ref()
    const selectedIds = ref([])

    
    const searchForm = reactive({
      title: '',
      type: '',
      status: ''
    })
    
    const pagination = reactive({
      current: 1,
      size: 10,
      total: 0
    })
    
    const tableData = ref([])
    
    const form = reactive({
      id: null,
      title: '',
      content: '',
      type: 1,
      status: 1,
      priority: 0,
      startTime: '',
      endTime: ''
    })
    
    const formRules = {
      title: [
        { required: true, message: '请输入公告标题', trigger: 'blur' },
        { min: 1, max: 255, message: '标题长度在 1 到 255 个字符', trigger: 'blur' }
      ],
      content: [
        { required: true, message: '请输入公告内容', trigger: 'blur' }
      ],
      type: [
        { required: true, message: '请选择公告类型', trigger: 'change' }
      ],
      priority: [
        { required: true, message: '请输入优先级', trigger: 'blur' }
      ],
      startTime: [
        { required: true, message: '请选择开始时间', trigger: 'change' }
      ],
      status: [
        { required: true, message: '请选择状态', trigger: 'change' }
      ]
    }
    
    const dialogTitle = ref('新增公告')
    
    // 数据加载标志，防止自动触发状态更新
    const isDataLoading = ref(true) // 初始化为true，防止页面加载时触发
    
    // 获取公告列表
    const getAnnouncementList = async () => {
      loading.value = true
      isDataLoading.value = true

      try {
        const params = {
          current: pagination.current,
          size: pagination.size,
          ...searchForm
        }
        const response = await adminAnnouncementAPI.getAnnouncementList(params)
        // API拦截器已经处理了响应，直接使用返回的数据
        if (response) {
          tableData.value = response.records || []
          pagination.total = response.total || 0
        }
      } catch (error) {
        ElMessage.error('获取公告列表失败：' + error.message)
      } finally {
        loading.value = false
        // 数据加载完成后，延迟重置标志，避免自动触发change事件
        setTimeout(() => {
          isDataLoading.value = false
          console.log('Data loading completed, status change events enabled')
        }, 500) // 增加延迟时间
      }
    }
    
    // 获取类型标签类型
    const getTypeTagType = (type) => {
      switch (type) {
        case 1: return 'info'
        case 2: return 'warning'
        case 3: return 'danger'
        default: return 'info'
      }
    }
    
    // 获取类型文本
    const getTypeText = (type) => {
      switch (type) {
        case 1: return '普通公告'
        case 2: return '重要公告'
        case 3: return '系统维护'
        default: return '未知类型'
      }
    }
    
    // 格式化日期时间
    const formatDateTime = (dateTime) => {
      if (!dateTime) return ''
      return new Date(dateTime).toLocaleString('zh-CN')
    }
    
    // 搜索
    const handleSearch = () => {
      pagination.current = 1
      getAnnouncementList()
    }
    
    // 重置搜索
    const handleReset = () => {
      Object.assign(searchForm, {
        title: '',
        type: '',
        status: ''
      })
      pagination.current = 1
      getAnnouncementList()
    }
    
    // 新增
    const handleAdd = () => {
      dialogTitle.value = '新增公告'
      Object.assign(form, {
        id: null,
        title: '',
        content: '',
        type: 1,
        status: 1,
        priority: 0,
        startTime: '',
        endTime: ''
      })
      dialogVisible.value = true
    }
    
    // 编辑
    const handleEdit = (row) => {
      dialogTitle.value = '编辑公告'
      Object.assign(form, { ...row })
      dialogVisible.value = true
    }
    
    // 删除
    const handleDelete = async (row) => {
      try {
        await ElMessageBox.confirm('确定要删除这条公告吗？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        
        const response = await adminAnnouncementAPI.deleteAnnouncement(row.id)
        if (response) {
          ElMessage.success('删除成功')
          getAnnouncementList()
        }
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error('删除失败：' + error.message)
        }
      }
    }
    
    // 批量删除
    const handleBatchDelete = async () => {
      try {
        await ElMessageBox.confirm(`确定要删除选中的 ${selectedIds.value.length} 条公告吗？`, '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        
        const response = await adminAnnouncementAPI.batchDeleteAnnouncement(selectedIds.value)
        if (response) {
          ElMessage.success('批量删除成功')
          selectedIds.value = []
          getAnnouncementList()
        }
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error('批量删除失败：' + error.message)
        }
      }
    }
    
    // 状态变更
    const handleStatusChange = async (row) => {
      console.log('handleStatusChange called for row:', row.id, 'new status:', row.status, 'isDataLoading:', isDataLoading.value)
      
      // 如果正在加载数据，忽略状态变更事件
      if (isDataLoading.value) {
        console.log('Ignoring status change during data loading for row:', row.id)
        return
      }
      
      const originalStatus = row.status === 1 ? 0 : 1
      
      try {
        console.log('Sending API request for row:', row.id, 'status:', row.status)
        const response = await adminAnnouncementAPI.updateAnnouncementStatus(row.id, { status: row.status })
        console.log('API response:', response)
        if (response !== undefined) {
          ElMessage.success('状态更新成功')
        }
      } catch (error) {
        console.error('Status update error:', error)
        ElMessage.error('状态更新失败：' + error.message)
        // 恢复原状态
        row.status = originalStatus
      }
    }
    
    // 选择变更
    const handleSelectionChange = (selection) => {
      selectedIds.value = selection.map(item => item.id)
    }
    
    // 分页大小变更
    const handleSizeChange = (size) => {
      pagination.size = size
      pagination.current = 1
      getAnnouncementList()
    }
    
    // 当前页变更
    const handleCurrentChange = (current) => {
      pagination.current = current
      getAnnouncementList()
    }
    
    // 关闭对话框
    const handleDialogClose = () => {
      dialogVisible.value = false
      formRef.value?.resetFields()
    }
    
    // 提交表单
    const handleSubmit = async () => {
      if (!formRef.value) return
      
      try {
        await formRef.value.validate()
        submitLoading.value = true
        
        const isEdit = !!form.id
        const response = isEdit 
          ? await adminAnnouncementAPI.updateAnnouncement(form.id, form)
          : await adminAnnouncementAPI.createAnnouncement(form)
        
        if (response) {
          ElMessage.success(isEdit ? '更新成功' : '创建成功')
          handleDialogClose()
          getAnnouncementList()
        }
      } catch (error) {
        if (error.message) {
          ElMessage.error(error.message)
        }
      } finally {
        submitLoading.value = false
      }
    }
    
    onMounted(() => {
      getAnnouncementList()
    })
    
    return {
      loading,
      dialogVisible,
      submitLoading,
      formRef,
      selectedIds,
      isDataLoading,
      searchForm,
      pagination,
      tableData,
      form,
      formRules,
      dialogTitle,
      getTypeTagType,
      getTypeText,
      formatDateTime,
      handleSearch,
      handleReset,
      handleAdd,
      handleEdit,
      handleDelete,
      handleBatchDelete,
      handleStatusChange,
      handleSelectionChange,
      handleSizeChange,
      handleCurrentChange,
      handleDialogClose,
      handleSubmit
    }
  }
}
</script>

<style scoped>
.announcement-manage {
  padding: 20px;
}

.page-header {
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0 0 8px 0;
  color: #333;
  font-size: 24px;
}

.page-header p {
  margin: 0;
  color: #666;
  font-size: 14px;
}

.toolbar {
  margin-bottom: 20px;
  display: flex;
  gap: 10px;
}

.search-bar {
  background: #f8f9fa;
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 20px;
}

.table-container {
  background: white;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .announcement-manage {
    padding: 10px;
  }
  
  .search-bar .el-form {
    display: block;
  }
  
  .search-bar .el-form-item {
    margin-bottom: 15px;
    display: block;
  }
  
  .toolbar {
    flex-direction: column;
  }
  
  .table-container {
    overflow-x: auto;
  }
}
</style>