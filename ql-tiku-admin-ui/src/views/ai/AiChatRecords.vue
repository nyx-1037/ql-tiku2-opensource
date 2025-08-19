<template>
  <div class="ai-chat-records">
    <div class="header">
      <h2>AI聊天记录管理</h2>
      <div class="header-actions">
        <el-button type="danger" :disabled="selectedRecords.length === 0" @click="handleBatchDelete">
          <el-icon><Delete /></el-icon>
          批量删除
        </el-button>
      </div>
    </div>

    <div class="search-bar">
      <el-input
        v-model="recordSearch.keyword"
        placeholder="搜索用户ID或会话ID"
        @keyup.enter="loadChatRecords"
        clearable
        style="width: 300px; margin-right: 10px;"
      >
        <template #append>
          <el-button @click="loadChatRecords">
            <el-icon><Search /></el-icon>
          </el-button>
        </template>
      </el-input>
      <el-select
        v-model="recordSearch.messageType"
        placeholder="消息类型"
        clearable
        style="width: 150px; margin-right: 10px;"
      >
        <el-option label="用户消息" value="user" />
        <el-option label="AI回复" value="assistant" />
      </el-select>
      <el-button @click="resetRecordSearch">重置</el-button>
    </div>

    <el-table
      :data="recordList"
      v-loading="recordLoading"
      stripe
      style="width: 100%"
      @selection-change="handleRecordSelectionChange"
    >
      <el-table-column type="selection" width="55" />
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="username" label="用户名" width="120">
        <template #default="{ row }">
          <span>{{ row.username || row.nickname || `用户${row.userId}` }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="sessionId" label="会话ID" width="200" />
      <el-table-column prop="questionId" label="题目ID" width="100" />
      <el-table-column prop="messageType" label="消息类型" width="100">
        <template #default="{ row }">
          <el-tag :type="row.messageType === 'user' ? 'primary' : 'success'">
            {{ row.messageType === 'user' ? '用户' : 'AI' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="content" label="消息内容" min-width="300">
        <template #default="{ row }">
          <div class="content-preview">
            {{ row.content.length > 100 ? row.content.substring(0, 100) + '...' : row.content }}
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="180">
        <template #default="{ row }">
          <span>{{ formatDateTime(row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" size="small" @click="viewRecordContent(row)">
            查看内容
          </el-button>
          <el-button type="danger" size="small" @click="deleteRecord(row)">
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination">
      <el-pagination
        v-model:current-page="recordPagination.currentPage"
        v-model:page-size="recordPagination.pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="recordPagination.total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadChatRecords"
        @current-change="loadChatRecords"
      />
    </div>

    <!-- 内容查看对话框 -->
    <el-dialog
      v-model="contentDialogVisible"
      :title="contentDialogTitle"
      width="60%"
      :before-close="handleContentDialogClose"
    >
      <div class="content-detail">
        <pre>{{ contentDialogContent }}</pre>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="handleContentDialogClose">关闭</el-button>
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
  name: 'AiChatRecords',
  components: {
    Delete,
    Search
  },
  setup() {
    const selectedRecords = ref([])
    
    // 聊天记录相关数据
    const recordLoading = ref(false)
    const recordList = ref([])
    const recordSearch = reactive({
      keyword: '',
      messageType: ''
    })
    const recordPagination = reactive({
      currentPage: 1,
      pageSize: 20,
      total: 0
    })
    
    // 对话框相关
    const contentDialogVisible = ref(false)
    const contentDialogTitle = ref('')
    const contentDialogContent = ref('')

    // 格式化日期时间
    const formatDateTime = (datetime) => {
      if (!datetime) return ''
      return new Date(datetime).toLocaleString('zh-CN')
    }

    // 加载聊天记录
    const loadChatRecords = async () => {
      try {
        recordLoading.value = true
        const params = {
          page: recordPagination.currentPage,
          size: recordPagination.pageSize,
          keyword: recordSearch.keyword,
          messageType: recordSearch.messageType
        }
        const response = await aiDataAPI.getChatRecords(params)
        recordList.value = response.records
        recordPagination.total = response.total
      } catch (error) {
        ElMessage.error('加载聊天记录失败：' + error.message)
      } finally {
        recordLoading.value = false
      }
    }

    // 重置搜索
    const resetRecordSearch = () => {
      recordSearch.keyword = ''
      recordSearch.messageType = ''
      recordPagination.currentPage = 1
      loadChatRecords()
    }

    // 处理聊天记录选择变化
    const handleRecordSelectionChange = (selection) => {
      selectedRecords.value = selection
    }

    // 查看记录内容
    const viewRecordContent = (row) => {
      contentDialogVisible.value = true
      contentDialogTitle.value = `${row.messageType === 'user' ? '用户消息' : 'AI回复'} - ${row.username || `用户${row.userId}`}`
      contentDialogContent.value = row.content
    }

    // 删除记录
    const deleteRecord = async (row) => {
      try {
        await ElMessageBox.confirm('确定要删除该记录吗？', '确认删除', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        await aiDataAPI.deleteChatRecord(row.id)
        ElMessage.success('删除成功')
        loadChatRecords()
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
          `确定要删除选中的 ${selectedRecords.value.length} 个记录吗？`,
          '确认批量删除',
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
          }
        )
        
        const ids = selectedRecords.value.map(item => item.id)
        await aiDataAPI.batchDeleteChatRecords(ids)
        ElMessage.success('批量删除成功')
        loadChatRecords()
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error('批量删除失败：' + error.message)
        }
      }
    }

    // 关闭内容对话框
    const handleContentDialogClose = () => {
      contentDialogVisible.value = false
      contentDialogContent.value = ''
    }

    onMounted(() => {
      loadChatRecords()
    })

    return {
      selectedRecords,
      recordLoading,
      recordList,
      recordSearch,
      recordPagination,
      contentDialogVisible,
      contentDialogTitle,
      contentDialogContent,
      formatDateTime,
      loadChatRecords,
      resetRecordSearch,
      handleRecordSelectionChange,
      viewRecordContent,
      deleteRecord,
      handleBatchDelete,
      handleContentDialogClose
    }
  }
}
</script>

<style scoped>
.ai-chat-records {
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

.content-detail pre {
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 400px;
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