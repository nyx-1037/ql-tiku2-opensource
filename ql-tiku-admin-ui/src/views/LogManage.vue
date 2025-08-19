<template>
  <div class="log-manage">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span>操作日志管理</span>
        </div>
      </template>
      
      <!-- 搜索表单 -->
      <el-form :model="searchForm" :inline="true" class="search-form">
        <el-form-item label="用户名">
          <el-input
            v-model="searchForm.username"
            placeholder="请输入用户名"
            clearable
            style="width: 200px"
          />
        </el-form-item>
        
        <el-form-item label="操作方法">
          <el-input
            v-model="searchForm.operationMethod"
            placeholder="请输入操作方法"
            clearable
            style="width: 200px"
          />
        </el-form-item>
        
        <el-form-item label="IP地址">
          <el-input
            v-model="searchForm.ipAddress"
            placeholder="请输入IP地址"
            clearable
            style="width: 200px"
          />
        </el-form-item>
        
        <el-form-item label="状态码">
          <el-select
            v-model="searchForm.statusCode"
            placeholder="请选择状态码"
            clearable
            style="width: 150px"
          >
            <el-option label="200 - 成功" value="200" />
            <el-option label="400 - 请求错误" value="400" />
            <el-option label="401 - 未授权" value="401" />
            <el-option label="403 - 禁止访问" value="403" />
            <el-option label="404 - 未找到" value="404" />
            <el-option label="500 - 服务器错误" value="500" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="searchForm.timeRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 350px"
          />
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
      
      <!-- 操作按钮 -->
      <div class="operation-buttons">
        <el-button
          type="danger"
          :disabled="selectedRows.length === 0"
          @click="handleBatchDelete"
        >
          <el-icon><Delete /></el-icon>
          批量删除
        </el-button>
      </div>
      
      <!-- 数据表格 -->
      <el-table
        :data="tableData"
        v-loading="loading"
        @selection-change="handleSelectionChange"
        style="width: 100%"
        stripe
      >
        <el-table-column type="selection" width="55" />
        
        <el-table-column prop="id" label="ID" width="80" />
        
        <el-table-column prop="username" label="用户名" width="120" />
        
        <el-table-column prop="operationMethod" label="操作方法" width="200" show-overflow-tooltip />
        
        <el-table-column prop="requestMethod" label="请求方式" width="100">
          <template #default="scope">
            <el-tag
              :type="getMethodTagType(scope.row.requestMethod)"
              size="small"
            >
              {{ scope.row.requestMethod }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="requestUrl" label="请求URL" width="250" show-overflow-tooltip />
        
        <el-table-column prop="ipAddress" label="IP地址" width="140" />
        
        <el-table-column prop="location" label="地理位置" width="150" show-overflow-tooltip />
        
        <el-table-column prop="statusCode" label="状态码" width="100">
          <template #default="scope">
            <el-tag
              :type="getStatusTagType(scope.row.statusCode)"
              size="small"
            >
              {{ scope.row.statusCode }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="executionTime" label="执行时间(ms)" width="120" />
        
        <el-table-column prop="createTime" label="操作时间" width="180" />
        
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="scope">
            <el-button
              type="primary"
              size="small"
              @click="handleViewDetail(scope.row)"
            >
              详情
            </el-button>
            <el-button
              type="danger"
              size="small"
              @click="handleDelete(scope.row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <!-- 分页 -->
      <div class="pagination">
        <el-pagination
          v-model:current-page="pagination.current"
          v-model:page-size="pagination.size"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>
    
    <!-- 详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="操作日志详情"
      width="800px"
      :before-close="handleDetailDialogClose"
    >
      <div v-if="currentLog" class="log-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="ID">{{ currentLog.id }}</el-descriptions-item>
          <el-descriptions-item label="用户名">{{ currentLog.username }}</el-descriptions-item>
          <el-descriptions-item label="用户ID">{{ currentLog.userId || '未知' }}</el-descriptions-item>
          <el-descriptions-item label="操作方法">{{ currentLog.operationMethod }}</el-descriptions-item>
          <el-descriptions-item label="请求方式">
            <el-tag :type="getMethodTagType(currentLog.requestMethod)" size="small">
              {{ currentLog.requestMethod }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="状态码">
            <el-tag :type="getStatusTagType(currentLog.statusCode)" size="small">
              {{ currentLog.statusCode }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="IP地址">{{ currentLog.ipAddress }}</el-descriptions-item>
          <el-descriptions-item label="地理位置">{{ currentLog.location || '未知' }}</el-descriptions-item>
          <el-descriptions-item label="执行时间">{{ currentLog.executionTime }}ms</el-descriptions-item>
          <el-descriptions-item label="操作时间">{{ currentLog.createTime }}</el-descriptions-item>
          <el-descriptions-item label="请求URL" :span="2">{{ currentLog.requestUrl }}</el-descriptions-item>
          <el-descriptions-item label="User-Agent" :span="2">{{ currentLog.userAgent || '未知' }}</el-descriptions-item>
        </el-descriptions>
        
        <div class="detail-section">
          <h4>请求参数</h4>
          <el-input
            v-model="currentLog.requestParams"
            type="textarea"
            :rows="4"
            readonly
            placeholder="无请求参数"
          />
        </div>
        
        <div class="detail-section">
          <h4>响应数据</h4>
          <el-input
            v-model="currentLog.responseData"
            type="textarea"
            :rows="4"
            readonly
            placeholder="无响应数据"
          />
        </div>
        
        <div v-if="currentLog.errorMessage" class="detail-section">
          <h4>错误信息</h4>
          <el-input
            v-model="currentLog.errorMessage"
            type="textarea"
            :rows="3"
            readonly
          />
        </div>
      </div>
      
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="handleDetailDialogClose">关闭</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { logAPI } from '../api/log'

export default {
  name: 'LogManage',
  setup() {
    const loading = ref(false)
    const tableData = ref([])
    const selectedRows = ref([])
    const detailDialogVisible = ref(false)
    const currentLog = ref(null)
    
    const searchForm = reactive({
      username: '',
      operationMethod: '',
      ipAddress: '',
      statusCode: '',
      timeRange: []
    })
    
    const pagination = reactive({
      current: 1,
      size: 10,
      total: 0
    })
    
    // 获取日志列表
    const getLogList = async () => {
      loading.value = true
      try {
        const params = {
          current: pagination.current,
          size: pagination.size,
          username: searchForm.username || undefined,
          operationMethod: searchForm.operationMethod || undefined,
          ipAddress: searchForm.ipAddress || undefined,
          statusCode: searchForm.statusCode || undefined
        }
        
        if (searchForm.timeRange && searchForm.timeRange.length === 2) {
          params.startTime = searchForm.timeRange[0]
          params.endTime = searchForm.timeRange[1]
        }
        
        const response = await logAPI.getLogPage(params)
        if (response.code === 200) {
          tableData.value = response.data.records
          pagination.total = response.data.total
        } else {
          ElMessage.error(response.message || '获取日志列表失败')
        }
      } catch (error) {
        console.error('获取日志列表失败:', error)
        ElMessage.error('获取日志列表失败')
      } finally {
        loading.value = false
      }
    }
    
    // 搜索
    const handleSearch = () => {
      pagination.current = 1
      getLogList()
    }
    
    // 重置
    const handleReset = () => {
      Object.assign(searchForm, {
        username: '',
        operationMethod: '',
        ipAddress: '',
        statusCode: '',
        timeRange: []
      })
      pagination.current = 1
      getLogList()
    }
    
    // 选择变化
    const handleSelectionChange = (selection) => {
      selectedRows.value = selection
    }
    
    // 查看详情
    const handleViewDetail = async (row) => {
      try {
        const response = await logAPI.getLogById(row.id)
        if (response.code === 200) {
          currentLog.value = response.data
          detailDialogVisible.value = true
        } else {
          ElMessage.error(response.message || '获取日志详情失败')
        }
      } catch (error) {
        console.error('获取日志详情失败:', error)
        ElMessage.error('获取日志详情失败')
      }
    }
    
    // 删除单个
    const handleDelete = async (row) => {
      try {
        await ElMessageBox.confirm('确定要删除这条操作日志吗？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        
        const response = await logAPI.deleteLog(row.id)
        if (response.code === 200) {
          ElMessage.success('删除成功')
          getLogList()
        } else {
          ElMessage.error(response.message || '删除失败')
        }
      } catch (error) {
        if (error !== 'cancel') {
          console.error('删除失败:', error)
          ElMessage.error('删除失败')
        }
      }
    }
    
    // 批量删除
    const handleBatchDelete = async () => {
      try {
        await ElMessageBox.confirm(`确定要删除选中的 ${selectedRows.value.length} 条操作日志吗？`, '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        
        const ids = selectedRows.value.map(row => row.id)
        const response = await logAPI.batchDeleteLog(ids)
        if (response.code === 200) {
          ElMessage.success('批量删除成功')
          getLogList()
        } else {
          ElMessage.error(response.message || '批量删除失败')
        }
      } catch (error) {
        if (error !== 'cancel') {
          console.error('批量删除失败:', error)
          ElMessage.error('批量删除失败')
        }
      }
    }
    
    // 分页大小变化
    const handleSizeChange = (size) => {
      pagination.size = size
      pagination.current = 1
      getLogList()
    }
    
    // 当前页变化
    const handleCurrentChange = (current) => {
      pagination.current = current
      getLogList()
    }
    
    // 关闭详情对话框
    const handleDetailDialogClose = () => {
      detailDialogVisible.value = false
      currentLog.value = null
    }
    
    // 获取请求方法标签类型
    const getMethodTagType = (method) => {
      const typeMap = {
        'GET': 'success',
        'POST': 'primary',
        'PUT': 'warning',
        'DELETE': 'danger'
      }
      return typeMap[method] || 'info'
    }
    
    // 获取状态码标签类型
    const getStatusTagType = (status) => {
      if (status >= 200 && status < 300) {
        return 'success'
      } else if (status >= 400 && status < 500) {
        return 'warning'
      } else if (status >= 500) {
        return 'danger'
      }
      return 'info'
    }
    
    onMounted(() => {
      getLogList()
    })
    
    return {
      loading,
      tableData,
      selectedRows,
      searchForm,
      pagination,
      detailDialogVisible,
      currentLog,
      handleSearch,
      handleReset,
      handleSelectionChange,
      handleViewDetail,
      handleDelete,
      handleBatchDelete,
      handleSizeChange,
      handleCurrentChange,
      handleDetailDialogClose,
      getMethodTagType,
      getStatusTagType
    }
  }
}
</script>

<style scoped>
.log-manage {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
  font-size: 16px;
}

.search-form {
  margin-bottom: 20px;
  padding: 20px;
  background-color: #f8f9fa;
  border-radius: 4px;
}

.operation-buttons {
  margin-bottom: 20px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

.log-detail {
  max-height: 600px;
  overflow-y: auto;
}

.detail-section {
  margin-top: 20px;
}

.detail-section h4 {
  margin-bottom: 10px;
  color: #333;
  font-size: 14px;
  font-weight: 600;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .search-form {
    padding: 10px;
  }
  
  .search-form .el-form-item {
    margin-bottom: 10px;
  }
  
  .el-table {
    font-size: 12px;
  }
}
</style>