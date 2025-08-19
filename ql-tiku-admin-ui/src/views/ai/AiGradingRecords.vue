<template>
  <div class="ai-grading-records">
    <div class="header">
      <h2>AI评分记录管理</h2>
      <div class="header-actions">
        <el-button type="danger" :disabled="selectedRecords.length === 0" @click="handleBatchDelete">
          <el-icon><Delete /></el-icon>
          批量删除
        </el-button>
      </div>
    </div>

    <div class="search-bar">
      <el-input
        v-model="gradingSearch.keyword"
        placeholder="搜索用户ID或题目ID"
        @keyup.enter="loadGradingRecords"
        clearable
        style="width: 300px; margin-right: 10px;"
      >
        <template #append>
          <el-button @click="loadGradingRecords">
            <el-icon><Search /></el-icon>
          </el-button>
        </template>
      </el-input>
      <el-select
        v-model="gradingSearch.isCorrect"
        placeholder="是否正确"
        clearable
        style="width: 150px; margin-right: 10px;"
      >
        <el-option label="正确" :value="1" />
        <el-option label="错误" :value="0" />
      </el-select>
      <el-button @click="resetGradingSearch">重置</el-button>
    </div>

    <el-table
      :data="gradingList"
      v-loading="gradingLoading"
      stripe
      style="width: 100%"
      @selection-change="handleGradingSelectionChange"
    >
      <el-table-column type="selection" width="55" />
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="username" label="用户名" width="120">
        <template #default="{ row }">
          <span>{{ row.username || row.nickname || `用户${row.userId}` }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="questionId" label="题目ID" width="100" />
      <el-table-column prop="userAnswer" label="用户答案" min-width="200">
        <template #default="{ row }">
          <div class="content-preview">
            {{ row.userAnswer.length > 50 ? row.userAnswer.substring(0, 50) + '...' : row.userAnswer }}
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="isCorrect" label="是否正确" width="100">
        <template #default="{ row }">
          <el-tag :type="row.isCorrect ? 'success' : 'danger'">
            {{ row.isCorrect ? '正确' : '错误' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="180">
        <template #default="{ row }">
          <span>{{ formatDateTime(row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" size="small" @click="viewGradingDetails(row)">
            查看详情
          </el-button>
          <el-button type="danger" size="small" @click="deleteGrading(row)">
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination">
      <el-pagination
        v-model:current-page="gradingPagination.currentPage"
        v-model:page-size="gradingPagination.pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="gradingPagination.total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadGradingRecords"
        @current-change="loadGradingRecords"
      />
    </div>

    <!-- 评分详情对话框 -->
    <el-dialog
      v-model="gradingDetailVisible"
      title="评分详情"
      width="60%"
      :before-close="handleGradingDetailClose"
    >
      <div class="grading-detail" v-if="currentGrading">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="ID">{{ currentGrading.id }}</el-descriptions-item>
          <el-descriptions-item label="用户名">{{ currentGrading.username || `用户${currentGrading.userId}` }}</el-descriptions-item>
          <el-descriptions-item label="题目ID">{{ currentGrading.questionId }}</el-descriptions-item>
          <el-descriptions-item label="是否正确">
            <el-tag :type="currentGrading.isCorrect ? 'success' : 'danger'">
              {{ currentGrading.isCorrect ? '正确' : '错误' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatDateTime(currentGrading.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="用户答案">
            <div class="answer-detail">{{ currentGrading.userAnswer }}</div>
          </el-descriptions-item>
        </el-descriptions>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="handleGradingDetailClose">关闭</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Search } from '@element-plus/icons-vue'
import { aiDataAPI } from '../../api/aiData'

export default {
  name: 'AiGradingRecords',
  components: {
    Delete,
    Search
  },
  setup() {
    const selectedRecords = ref([])
    
    // 评分记录相关数据
    const gradingLoading = ref(false)
    const gradingList = ref([])
    const gradingSearch = reactive({
      keyword: '',
      isCorrect: null
    })
    const gradingPagination = reactive({
      currentPage: 1,
      pageSize: 20,
      total: 0
    })
    
    // 评分详情对话框
    const gradingDetailVisible = ref(false)
    const currentGrading = ref(null)

    // 格式化日期时间
    const formatDateTime = (datetime) => {
      if (!datetime) return ''
      return new Date(datetime).toLocaleString('zh-CN')
    }

    // 加载评分记录
    const loadGradingRecords = async () => {
      try {
        gradingLoading.value = true
        const params = {
          page: gradingPagination.currentPage,
          size: gradingPagination.pageSize,
          keyword: gradingSearch.keyword,
          isCorrect: gradingSearch.isCorrect
        }
        const response = await aiDataAPI.getGradingRecords(params)
        gradingList.value = response.records
        gradingPagination.total = response.total
      } catch (error) {
        ElMessage.error('加载评分记录失败：' + error.message)
      } finally {
        gradingLoading.value = false
      }
    }

    // 重置搜索
    const resetGradingSearch = () => {
      gradingSearch.keyword = ''
      gradingSearch.isCorrect = null
      gradingPagination.currentPage = 1
      loadGradingRecords()
    }

    // 处理评分记录选择变化
    const handleGradingSelectionChange = (selection) => {
      selectedRecords.value = selection
    }

    // 查看评分详情
    const viewGradingDetails = (row) => {
      currentGrading.value = row
      gradingDetailVisible.value = true
    }

    // 删除评分记录
    const deleteGrading = async (row) => {
      try {
        await ElMessageBox.confirm('确定要删除该评分记录吗？', '确认删除', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        await aiDataAPI.deleteGradingRecord(row.id)
        ElMessage.success('删除成功')
        loadGradingRecords()
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error('删除失败：' + error.message)
        }
      }
    }

    // 批量删除
    const handleBatchDelete = async () => {
      if (selectedRecords.value.length === 0) return
      
      try {
        await ElMessageBox.confirm(
          `确定要删除选中的 ${selectedRecords.value.length} 个评分记录吗？`,
          '确认批量删除',
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
          }
        )
        
        const ids = selectedRecords.value.map(item => item.id)
        await aiDataAPI.batchDeleteGradingRecords(ids)
        ElMessage.success('批量删除成功')
        loadGradingRecords()
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error('批量删除失败：' + error.message)
        }
      }
    }

    // 关闭评分详情对话框
    const handleGradingDetailClose = () => {
      gradingDetailVisible.value = false
      currentGrading.value = null
    }

    onMounted(() => {
      loadGradingRecords()
    })

    return {
      selectedRecords,
      gradingLoading,
      gradingList,
      gradingSearch,
      gradingPagination,
      gradingDetailVisible,
      currentGrading,
      formatDateTime,
      loadGradingRecords,
      resetGradingSearch,
      handleGradingSelectionChange,
      viewGradingDetails,
      deleteGrading,
      handleBatchDelete,
      handleGradingDetailClose
    }
  }
}
</script>

<style scoped>
.ai-grading-records {
  padding: 20px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.header h2 {
  margin: 0;
  color: #303133;
}

.search-bar {
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.content-preview {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.answer-detail {
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 200px;
  overflow-y: auto;
}

.grading-detail {
  max-height: 500px;
  overflow-y: auto;
}

@media (max-width: 768px) {
  .search-bar {
    flex-direction: column;
    align-items: stretch;
  }
  
  .search-bar .el-input,
  .search-bar .el-select {
    width: 100% !important;
    margin-right: 0 !important;
    margin-bottom: 10px;
  }
}
</style>