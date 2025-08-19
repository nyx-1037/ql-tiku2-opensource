<template>
  <div class="ai-chat-sessions">
    <div class="header">
      <h2>AI聊天会话管理</h2>
      <div class="header-actions">
        <el-button type="danger" :disabled="selectedRecords.length === 0" @click="handleBatchDelete">
          <el-icon><Delete /></el-icon>
          批量删除
        </el-button>
      </div>
    </div>

    <div class="search-bar">
      <el-input
        v-model="sessionSearch.keyword"
        placeholder="搜索会话标题或用户ID"
        @keyup.enter="loadSessions"
        clearable
        style="width: 300px; margin-right: 10px;"
      >
        <template #append>
          <el-button @click="loadSessions">
            <el-icon><Search /></el-icon>
          </el-button>
        </template>
      </el-input>
      <el-button @click="resetSessionSearch">重置</el-button>
    </div>

    <el-table
      :data="sessionList"
      v-loading="sessionLoading"
      stripe
      style="width: 100%"
      @selection-change="handleSessionSelectionChange"
    >
      <el-table-column type="selection" width="55" />
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="sessionId" label="会话ID" width="200" />
      <el-table-column prop="username" label="用户名" width="120">
        <template #default="{ row }">
          <span>{{ row.username || row.nickname || `用户${row.userId}` }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="title" label="会话标题" min-width="200" />
      <el-table-column prop="createTime" label="创建时间" width="180">
        <template #default="{ row }">
          <span>{{ formatDateTime(row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="updateTime" label="更新时间" width="180">
        <template #default="{ row }">
          <span>{{ formatDateTime(row.updateTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" size="small" @click="viewSessionDetails(row)">
            查看详情
          </el-button>
          <el-button type="danger" size="small" @click="deleteSession(row)">
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination">
      <el-pagination
        v-model:current-page="sessionPagination.currentPage"
        v-model:page-size="sessionPagination.pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="sessionPagination.total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadSessions"
        @current-change="loadSessions"
      />
    </div>

    <!-- 会话详情对话框 -->
    <el-dialog
      v-model="sessionDetailVisible"
      title="会话详情"
      width="80%"
      :before-close="handleSessionDetailClose"
    >
      <div class="session-detail">
        <el-table
          :data="sessionDetailRecords"
          v-loading="sessionDetailLoading"
          stripe
          style="width: 100%"
        >
          <el-table-column prop="messageType" label="类型" width="80">
            <template #default="{ row }">
              <el-tag :type="row.messageType === 'user' ? 'primary' : 'success'">
                {{ row.messageType === 'user' ? '用户' : 'AI' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="content" label="消息内容" min-width="400">
            <template #default="{ row }">
              <div class="message-content">
                {{ row.content }}
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="时间" width="180">
            <template #default="{ row }">
              <span>{{ formatDateTime(row.createTime) }}</span>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="handleSessionDetailClose">关闭</el-button>
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
  name: 'AiChatSessions',
  components: {
    Delete,
    Search
  },
  setup() {
    const selectedRecords = ref([])
    
    // 会话相关数据
    const sessionLoading = ref(false)
    const sessionList = ref([])
    const sessionSearch = reactive({
      keyword: ''
    })
    const sessionPagination = reactive({
      currentPage: 1,
      pageSize: 20,
      total: 0
    })
    
    // 会话详情对话框
    const sessionDetailVisible = ref(false)
    const sessionDetailLoading = ref(false)
    const sessionDetailRecords = ref([])

    // 格式化日期时间
    const formatDateTime = (datetime) => {
      if (!datetime) return ''
      return new Date(datetime).toLocaleString('zh-CN')
    }

    // 加载会话列表
    const loadSessions = async () => {
      try {
        sessionLoading.value = true
        const params = {
          page: sessionPagination.currentPage,
          size: sessionPagination.pageSize,
          keyword: sessionSearch.keyword
        }
        const response = await aiDataAPI.getSessions(params)
        sessionList.value = response.records
        sessionPagination.total = response.total
      } catch (error) {
        ElMessage.error('加载会话列表失败：' + error.message)
      } finally {
        sessionLoading.value = false
      }
    }

    // 重置搜索
    const resetSessionSearch = () => {
      sessionSearch.keyword = ''
      sessionPagination.currentPage = 1
      loadSessions()
    }

    // 处理会话选择变化
    const handleSessionSelectionChange = (selection) => {
      selectedRecords.value = selection
    }

    // 查看会话详情
    const viewSessionDetails = async (row) => {
      try {
        sessionDetailLoading.value = true
        sessionDetailVisible.value = true
        const response = await aiDataAPI.getSessionRecords(row.sessionId)
        sessionDetailRecords.value = response
      } catch (error) {
        ElMessage.error('加载会话详情失败：' + error.message)
      } finally {
        sessionDetailLoading.value = false
      }
    }

    // 删除会话
    const deleteSession = async (row) => {
      try {
        await ElMessageBox.confirm('确定要删除该会话吗？', '确认删除', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        await aiDataAPI.deleteSession(row.id)
        ElMessage.success('删除成功')
        loadSessions()
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
          `确定要删除选中的 ${selectedRecords.value.length} 个会话吗？`,
          '确认批量删除',
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
          }
        )
        
        const ids = selectedRecords.value.map(item => item.id)
        await aiDataAPI.batchDeleteSessions(ids)
        ElMessage.success('批量删除成功')
        loadSessions()
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error('批量删除失败：' + error.message)
        }
      }
    }

    // 关闭会话详情对话框
    const handleSessionDetailClose = () => {
      sessionDetailVisible.value = false
      sessionDetailRecords.value = []
    }

    onMounted(() => {
      loadSessions()
    })

    return {
      selectedRecords,
      sessionLoading,
      sessionList,
      sessionSearch,
      sessionPagination,
      sessionDetailVisible,
      sessionDetailLoading,
      sessionDetailRecords,
      formatDateTime,
      loadSessions,
      resetSessionSearch,
      handleSessionSelectionChange,
      viewSessionDetails,
      deleteSession,
      handleBatchDelete,
      handleSessionDetailClose
    }
  }
}
</script>

<style scoped>
.ai-chat-sessions {
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

.message-content {
  white-space: pre-wrap;
  word-break: break-all;
}

@media (max-width: 768px) {
  .search-bar {
    flex-direction: column;
    align-items: stretch;
  }
  
  .search-bar .el-input {
    width: 100% !important;
    margin-right: 0 !important;
    margin-bottom: 10px;
  }
}
</style>