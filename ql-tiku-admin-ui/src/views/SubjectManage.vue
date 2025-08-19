<template>
  <div class="subject-manage">
    <!-- 页面标题 -->
    <div class="page-header">
      <h2>科目管理</h2>
      <p>管理系统中的所有科目分类</p>
    </div>
    
    <!-- 操作栏 -->
    <el-card class="operation-card">
      <el-row :gutter="20">
        <el-col :span="16">
          <el-row :gutter="10">
            <el-col :span="8">
              <el-input
                v-model="searchForm.keyword"
                placeholder="搜索科目名称"
                clearable
                @keyup.enter="handleSearch"
              >
                <template #append>
                  <el-button icon="Search" @click="handleSearch" />
                </template>
              </el-input>
            </el-col>
            <el-col :span="6">
              <el-select v-model="searchForm.status" placeholder="状态" clearable @change="handleSearch">
                <el-option label="全部状态" value="" />
                <el-option label="启用" value="enabled" />
                <el-option label="禁用" value="disabled" />
              </el-select>
            </el-col>
          </el-row>
        </el-col>
        <el-col :span="8" class="operation-buttons">
          <el-button type="primary" icon="Plus" @click="handleAdd">新增科目</el-button>
          <el-button
            type="danger"
            icon="Delete"
            :disabled="selectedSubjects.length === 0"
            @click="handleBatchDelete"
          >
            批量删除
          </el-button>
        </el-col>
      </el-row>
    </el-card>
    
    <!-- 科目列表 -->
    <el-card class="table-card">
      <el-table
        v-loading="loading"
        :data="subjects"
        @selection-change="handleSelectionChange"
        stripe
        style="width: 100%"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="科目名称" min-width="200" />
        <el-table-column prop="description" label="描述" min-width="300" show-overflow-tooltip />
        <el-table-column prop="questionCount" label="题目数量" width="120">
          <template #default="{ row }">
            <el-tag type="info">{{ row.questionCount }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-switch
              v-model="row.status"
              active-value="enabled"
              inactive-value="disabled"
              @change="handleStatusChange(row)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleView(row)">查看</el-button>
            <el-button type="warning" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>
    
    <!-- 科目详情/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
      :before-close="handleDialogClose"
    >
      <el-form
        ref="subjectFormRef"
        :model="subjectForm"
        :rules="subjectRules"
        label-width="80px"
        :disabled="dialogMode === 'view'"
      >
        <el-form-item label="科目名称" prop="name">
          <el-input
            v-model="subjectForm.name"
            placeholder="请输入科目名称"
            maxlength="50"
            show-word-limit
          />
        </el-form-item>
        
        <el-form-item label="科目描述" prop="description">
          <el-input
            v-model="subjectForm.description"
            type="textarea"
            :rows="4"
            placeholder="请输入科目描述"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
        
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="subjectForm.status">
            <el-radio label="enabled">启用</el-radio>
            <el-radio label="disabled">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        
        <el-form-item label="排序" prop="sort">
          <el-input-number
            v-model="subjectForm.sort"
            :min="0"
            :max="999"
            placeholder="排序值，数字越小越靠前"
          />
        </el-form-item>
      </el-form>
      
      <template #footer v-if="dialogMode !== 'view'">
        <span class="dialog-footer">
          <el-button @click="handleDialogClose">取消</el-button>
          <el-button type="primary" :loading="submitLoading" @click="handleSubmit">
            {{ dialogMode === 'add' ? '创建' : '更新' }}
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { subjectAPI } from '../api'

export default {
  name: 'SubjectManage',
  setup() {
    const loading = ref(false)
    const submitLoading = ref(false)
    const dialogVisible = ref(false)
    const dialogMode = ref('add') // add, edit, view
    const subjectFormRef = ref()
    
    const subjects = ref([])
    const selectedSubjects = ref([])
    
    const searchForm = reactive({
      keyword: '',
      status: ''
    })
    
    const pagination = reactive({
      page: 1,
      size: 20,
      total: 0
    })
    
    const subjectForm = reactive({
      id: null,
      name: '',
      description: '',
      status: 'enabled',
      sort: 0
    })
    
    const subjectRules = {
      name: [
        { required: true, message: '请输入科目名称', trigger: 'blur' },
        { min: 2, max: 50, message: '科目名称长度在 2 到 50 个字符', trigger: 'blur' }
      ],
      description: [
        { required: true, message: '请输入科目描述', trigger: 'blur' },
        { max: 200, message: '描述长度不能超过 200 个字符', trigger: 'blur' }
      ],
      status: [
        { required: true, message: '请选择状态', trigger: 'change' }
      ],
      sort: [
        { required: true, message: '请输入排序值', trigger: 'blur' },
        { type: 'number', min: 0, max: 999, message: '排序值必须在 0 到 999 之间', trigger: 'blur' }
      ]
    }
    
    const dialogTitle = computed(() => {
      const titles = {
        add: '新增科目',
        edit: '编辑科目',
        view: '查看科目'
      }
      return titles[dialogMode.value]
    })
    
    // 获取科目列表
    const getSubjects = async () => {
      try {
        loading.value = true
        // 转换前端状态到后端格式
        const requestParams = {
          ...searchForm,
          current: pagination.page,
          size: pagination.size
        }
        
        // 将字符串状态转换为数字
        if (requestParams.status === 'enabled') {
          requestParams.status = 1
        } else if (requestParams.status === 'disabled') {
          requestParams.status = 0
        } else if (requestParams.status === '') {
          delete requestParams.status // 删除空字符串，让后端接收到null
        }
        
        const response = await subjectAPI.getSubjects(requestParams)
        
        // 处理响应数据，转换字段映射
        const processedList = (response.records || response.list || []).map(item => ({
          ...item,
          sort: item.sortOrder, // 映射 sortOrder 到 sort
          status: item.status === 1 ? 'enabled' : 'disabled' // 映射数字状态到字符串
        }))
        
        subjects.value = processedList
        pagination.total = response.total || 0
      } catch (error) {
        console.error('获取科目列表失败:', error)
        ElMessage.error('获取科目列表失败')
        subjects.value = []
        pagination.total = 0
      } finally {
        loading.value = false
      }
    }
    
    // 搜索
    const handleSearch = () => {
      pagination.page = 1
      getSubjects()
    }
    
    // 分页大小改变
    const handleSizeChange = (size) => {
      pagination.size = size
      getSubjects()
    }
    
    // 当前页改变
    const handleCurrentChange = (page) => {
      pagination.page = page
      getSubjects()
    }
    
    // 选择改变
    const handleSelectionChange = (selection) => {
      selectedSubjects.value = selection
    }
    
    // 状态改变
    const handleStatusChange = async (row) => {
      try {
        // 转换前端状态到后端格式
        const backendStatus = row.status === 'enabled' ? 1 : 0
        await subjectAPI.updateSubjectStatus(row.id, backendStatus)
        ElMessage.success(`${row.status === 'enabled' ? '启用' : '禁用'}成功`)
      } catch (error) {
        // 恢复原状态
        row.status = row.status === 'enabled' ? 'disabled' : 'enabled'
        console.error('更新状态失败:', error)
        ElMessage.error('状态更新失败')
      }
    }
    
    // 新增科目
    const handleAdd = () => {
      dialogMode.value = 'add'
      resetSubjectForm()
      dialogVisible.value = true
    }
    
    // 查看科目
    const handleView = (row) => {
      dialogMode.value = 'view'
      loadSubjectForm(row)
      dialogVisible.value = true
    }
    
    // 编辑科目
    const handleEdit = (row) => {
      dialogMode.value = 'edit'
      loadSubjectForm(row)
      dialogVisible.value = true
    }
    
    // 删除科目
    const handleDelete = async (row) => {
      if (row.questionCount > 0) {
        ElMessage.warning('该科目下还有题目，无法删除')
        return
      }
      
      try {
        await ElMessageBox.confirm(`确定要删除科目 "${row.name}" 吗？`, '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        
        await subjectAPI.deleteSubject(row.id)
        ElMessage.success('删除成功')
        getSubjects()
      } catch (error) {
        if (error !== 'cancel') {
          console.error('删除科目失败:', error)
          ElMessage.error('删除科目失败')
        }
      }
    }
    
    // 批量删除
    const handleBatchDelete = async () => {
      const hasQuestions = selectedSubjects.value.some(item => item.questionCount > 0)
      if (hasQuestions) {
        ElMessage.warning('选中的科目中有包含题目的科目，无法删除')
        return
      }
      
      try {
        await ElMessageBox.confirm(`确定要删除选中的 ${selectedSubjects.value.length} 个科目吗？`, '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        
        const ids = selectedSubjects.value.map(item => item.id)
        await subjectAPI.batchDeleteSubjects(ids)
        ElMessage.success('批量删除成功')
        getSubjects()
      } catch (error) {
        if (error !== 'cancel') {
          console.error('批量删除失败:', error)
          ElMessage.error('批量删除失败')
        }
      }
    }
    
    // 重置表单
    const resetSubjectForm = () => {
      Object.assign(subjectForm, {
        id: null,
        name: '',
        description: '',
        status: 'enabled',
        sort: 0
      })
    }
    
    // 加载表单数据
    const loadSubjectForm = (row) => {
      Object.assign(subjectForm, {
        id: row.id,
        name: row.name,
        description: row.description,
        status: row.status, // 已经在getSubjects中转换过了
        sort: row.sort || row.sortOrder || 0 // 兼容处理
      })
    }
    
    // 关闭对话框
    const handleDialogClose = () => {
      dialogVisible.value = false
      subjectFormRef.value?.resetFields()
    }
    
    // 提交表单
    const handleSubmit = async () => {
      if (!subjectFormRef.value) return
      
      try {
        await subjectFormRef.value.validate()
        submitLoading.value = true
        
        // 转换前端数据格式到后端格式
        const submitData = {
          ...subjectForm,
          status: subjectForm.status === 'enabled' ? 1 : 0, // 转换状态
          sortOrder: subjectForm.sort // 映射sort到sortOrder
        }
        delete submitData.sort // 删除前端字段
        
        if (dialogMode.value === 'add') {
          await subjectAPI.createSubject(submitData)
          ElMessage.success('创建成功')
        } else {
          await subjectAPI.updateSubject(submitData.id, submitData)
          ElMessage.success('更新成功')
        }
        
        handleDialogClose()
        getSubjects()
      } catch (error) {
        console.error('提交失败:', error)
        ElMessage.error(dialogMode.value === 'add' ? '创建失败' : '更新失败')
      } finally {
        submitLoading.value = false
      }
    }
    
    onMounted(() => {
      getSubjects()
    })
    
    return {
      loading,
      submitLoading,
      dialogVisible,
      dialogMode,
      dialogTitle,
      subjectFormRef,
      subjects,
      selectedSubjects,
      searchForm,
      pagination,
      subjectForm,
      subjectRules,
      handleSearch,
      handleSizeChange,
      handleCurrentChange,
      handleSelectionChange,
      handleStatusChange,
      handleAdd,
      handleView,
      handleEdit,
      handleDelete,
      handleBatchDelete,
      handleDialogClose,
      handleSubmit
    }
  }
}
</script>

<style scoped>
.subject-manage {
  padding: 0;
}

.page-header {
  margin-bottom: 20px;
}

.page-header h2 {
  color: #333;
  margin-bottom: 8px;
}

.page-header p {
  color: #666;
  margin: 0;
}

.operation-card {
  margin-bottom: 20px;
}

.operation-buttons {
  text-align: right;
}

.table-card {
  min-height: 400px;
}

.pagination-container {
  margin-top: 20px;
  text-align: right;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .subject-manage {
    padding: 10px;
  }
  
  .operation-card .el-row {
    flex-direction: column;
    margin: 0 !important;
  }
  
  .operation-card .el-col {
    padding: 0 !important;
    margin-bottom: 12px;
  }
  
  /* 输入框、下拉框、搜索框宽度定宽优化 */
  .operation-card .el-input,
  .operation-card .el-select {
    width: 200px !important;
    min-width: 200px !important;
  }
  
  .operation-card .el-input__inner,
  .operation-card .el-select .el-input__inner {
    width: 100% !important;
    font-size: 16px;
    padding: 12px 15px;
    border-radius: 6px;
  }
  
  .operation-card .el-select__wrapper {
    width: 100% !important;
  }
  
  .operation-buttons {
    text-align: left;
    margin-top: 15px;
  }
  
  .operation-buttons .el-button {
    margin-bottom: 10px;
    width: 100%;
    padding: 12px;
    font-size: 14px;
  }
  
  /* 表格优化 */
  .el-table {
    font-size: 13px;
  }
  
  .el-table th,
  .el-table td {
    padding: 8px 4px;
  }
  
  .el-table .el-button {
    padding: 4px 8px;
    font-size: 12px;
    margin: 2px;
  }
  
  /* 对话框优化 */
  .el-dialog {
    width: 95% !important;
    margin: 20px auto;
  }
  
  .el-dialog .el-form-item__label {
    width: 100% !important;
    text-align: left !important;
    margin-bottom: 8px;
    font-weight: 500;
  }
  
  .el-dialog .el-form-item__content {
    margin-left: 0 !important;
  }
  
  .el-dialog .el-input,
  .el-dialog .el-select,
  .el-dialog .el-textarea,
  .el-dialog .el-input-number {
    width: 100% !important;
  }
  
  .el-dialog .el-input__inner,
  .el-dialog .el-textarea__inner {
    font-size: 16px;
    padding: 12px;
    border-radius: 6px;
  }
  
  /* 分页优化 */
  .pagination-container {
    text-align: center;
    margin-top: 15px;
  }
  
  .el-pagination {
    justify-content: center;
  }
  
  .el-pagination .el-pager li {
    min-width: 35px;
    height: 35px;
    line-height: 35px;
  }
}
</style>