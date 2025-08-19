<template>
  <div class="model-management">
    <el-card>
      <template #header>
        <div class="header-container">
          <div class="header-left">
            <el-icon class="header-icon"><Setting /></el-icon>
            <span class="header-title">AI模型管理</span>
          </div>
          <div class="header-right">
            <el-button type="primary" @click="handleAdd">
              <el-icon><Plus /></el-icon>
              新增模型
            </el-button>
          </div>
        </div>
      </template>

      <!-- 搜索区域 -->
      <div class="search-container">
        <el-form :model="searchForm" inline @submit.prevent="handleSearch">
          <el-form-item label="模型名称" prop="name">
            <el-input
              v-model="searchForm.name"
              placeholder="请输入模型名称"
              clearable
              style="width: 200px"
            />
          </el-form-item>
          <el-form-item label="状态" prop="enabled">
            <el-select
              v-model="searchForm.enabled"
              placeholder="请选择状态"
              clearable
              style="width: 120px"
            >
              <el-option :value="true" label="启用" />
              <el-option :value="false" label="禁用" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSearch" :loading="loading">
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
      <el-table
        :data="tableData"
        v-loading="loading"
        stripe
        style="width: 100%"
        row-key="id"
      >
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column prop="name" label="模型名称" width="150" />
        <el-table-column prop="code" label="模型代码" width="150" />
        <el-table-column prop="provider" label="提供商" width="120" />
        <el-table-column prop="description" label="描述" width="180" show-overflow-tooltip />
        <el-table-column label="配置" width="120">
          <template #default="{ row }">
            <el-tooltip :content="row.config" placement="top">
              <el-tag type="info">
                {{ getConfigSummary(row.config) }}
              </el-tag>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-switch
              v-model="row.enabled"
              @change="handleStatusChange(row)"
              :loading="row.statusLoading"
            />
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="160">
          <template #default="{ row }">
            {{ formatTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="更新时间" width="160">
          <template #default="{ row }">
            {{ formatTime(row.updateTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center" fixed="right">
          <template #default="{ row }">
            <el-button
              type="primary"
              :link="true"
              size="small"
              @click="handleEdit(row)"
            >
              编辑
            </el-button>
            <el-button
              type="danger"
              :link="true"
              size="small"
              @click="handleDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="pagination.current"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handlePageSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
      @close="handleCancel"
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="100px"
      >
        <el-form-item label="模型名称" prop="name">
          <el-input
            v-model="formData.name"
            placeholder="请输入模型名称"
            clearable
          />
        </el-form-item>
        
        <el-form-item label="模型代码" prop="code">
          <el-input
            v-model="formData.code"
            placeholder="请输入模型代码，如：qwen-turbo"
            clearable
          />
        </el-form-item>
        
        <el-form-item label="模型提供商">
          <el-input
            value="阿里云百炼"
            disabled
            style="width: 100%"
          />
        </el-form-item>
        
        <el-form-item label="模型描述" prop="description">
          <el-input
            v-model="formData.description"
            type="textarea"
            placeholder="请输入模型描述"
            :rows="2"
          />
        </el-form-item>
        
        <el-form-item label="模型配置" prop="config">
          <el-input
            v-model="formData.config"
            type="textarea"
            placeholder="请输入JSON格式的模型配置"
            :rows="4"
          />
          <div class="form-tips">
            配置示例：{"model": "qwen-turbo", "temperature": 0.7, "maxTokens": 2000}
          </div>
        </el-form-item>
        
        <el-form-item label="启用状态" prop="enabled">
          <el-switch v-model="formData.enabled" />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="handleCancel">取消</el-button>
          <el-button type="primary" @click="handleSubmit" :loading="submitLoading">
            确定
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Refresh, Setting } from '@element-plus/icons-vue'
import { aiModelAPI } from '@/api/aiModel'

// 响应式数据
const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const formRef = ref(null)
const tableData = ref([])

// 搜索表单
const searchForm = reactive({
  name: '',
  enabled: null
})

// 分页配置
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0
})

// 表单数据
const formData = reactive({
  id: null,
  name: '',
  code: '',
  provider: '',
  description: '',
  config: '',
  enabled: true,
  sortOrder: 0
})

// JSON配置验证函数
const validateJsonConfig = (rule, value, callback) => {
  try {
    JSON.parse(value)
    callback()
  } catch (e) {
    callback(new Error('请输入有效的JSON格式'))
  }
}

// 表单验证规则
const formRules = {
  name: [
    { required: true, message: '请输入模型名称', trigger: 'blur' }
  ],
  code: [
    { required: true, message: '请输入模型代码', trigger: 'blur' }
  ],
  config: [
    { validator: validateJsonConfig, trigger: 'blur' }
  ]
}

// 计算属性
const dialogTitle = computed(() => {
  return formData.id ? '编辑模型' : '新增模型'
})

// 方法
const loadData = async () => {
  try {
    loading.value = true
    const params = {
      current: pagination.current,
      size: pagination.pageSize,
      ...searchForm
    }
    
    const response = await aiModelAPI.getAllModels(params)
    tableData.value = response.records || []
    pagination.total = response.total || 0
    
    // 为每行添加状态加载标识
    tableData.value.forEach(row => {
      row.statusLoading = false
    })
  } catch (error) {
    console.error('加载数据失败:', error)
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.current = 1
  loadData()
}

const handleReset = () => {
  Object.assign(searchForm, {
    name: '',
    enabled: null
  })
  pagination.current = 1
  loadData()
}

const handlePageChange = (current) => {
  pagination.current = current
  loadData()
}

const handlePageSizeChange = (pageSize) => {
  pagination.pageSize = pageSize
  pagination.current = 1
  loadData()
}

const handleAdd = () => {
  resetForm()
  dialogVisible.value = true
}

const handleEdit = (row) => {
  Object.assign(formData, {
    id: row.id,
    name: row.name,
    code: row.code,
    provider: 'dashscope',
    description: row.description,
    config: row.config,
    enabled: row.enabled,
    sortOrder: row.sortOrder || 0
  })
  dialogVisible.value = true
}

const handleDelete = (row) => {
  ElMessageBox.confirm(
    `确定要删除模型"${row.name}"吗？此操作不可恢复。`,
    '确认删除',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    }
  ).then(async () => {
    try {
      await aiModelAPI.deleteModel(row.id)
      ElMessage.success('删除成功')
      loadData()
    } catch (error) {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
  }).catch(() => {
    // 用户取消删除
  })
}

const handleStatusChange = async (row) => {
  try {
    row.statusLoading = true
    await aiModelAPI.toggleModelStatus(row.id, row.enabled)
    ElMessage.success(`${row.enabled ? '启用' : '禁用'}成功`)
  } catch (error) {
    console.error('状态切换失败:', error)
    ElMessage.error('状态切换失败')
    // 恢复原状态
    row.enabled = !row.enabled
  } finally {
    row.statusLoading = false
  }
}

const handleSubmit = async () => {
  try {
    const valid = await formRef.value?.validate()
    if (!valid) return
    
    submitLoading.value = true
    
    if (formData.id) {
      await aiModelAPI.updateModel(formData.id, formData)
      ElMessage.success('更新成功')
    } else {
      await aiModelAPI.createModel(formData)
      ElMessage.success('创建成功')
    }
    
    dialogVisible.value = false
    loadData()
  } catch (error) {
    console.error('提交失败:', error)
    ElMessage.error('提交失败')
  } finally {
    submitLoading.value = false
  }
}

const handleCancel = () => {
  dialogVisible.value = false
  resetForm()
}

const resetForm = () => {
  Object.assign(formData, {
    id: null,
    name: '',
    code: '',
    provider: 'alibaba',
    description: '',
    config: '{\n  "model": "qwen-turbo",\n  "temperature": 0.7,\n  "maxTokens": 2000\n}',
    enabled: true,
    sortOrder: 0
  })
  formRef.value?.clearValidate()
}

const getConfigSummary = (config) => {
  try {
    const configObj = JSON.parse(config)
    return configObj.model || '未知模型'
  } catch (e) {
    return '配置错误'
  }
}

const formatTime = (timeStr) => {
  if (!timeStr) return '-'
  return new Date(timeStr).toLocaleString('zh-CN')
}

// 生命周期
onMounted(() => {
  loadData()
})
</script>

<style scoped>
.model-management {
  padding: 20px;
}

.header-container {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-icon {
  font-size: 20px;
  color: #409eff;
}

.header-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.search-container {
  margin-bottom: 20px;
  padding: 20px;
  background: #f8f9fa;
  border-radius: 6px;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

.form-tips {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
  line-height: 1.4;
}

:deep(.el-card) {
  border-radius: 6px;
}

:deep(.el-table th) {
  background: #f8f9fa;
  font-weight: 600;
}

:deep(.el-dialog__body) {
  padding: 20px 24px;
}

:deep(.el-form-item__label) {
  font-weight: 500;
}

:deep(.el-textarea__inner) {
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 13px;
}
</style>