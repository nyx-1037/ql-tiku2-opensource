<template>
  <div class="online-users">
    <div class="page-header">
      <h2>在线用户管理</h2>
      <p>管理当前在线的用户，可以查看登录状态和强制下线</p>
    </div>

    <div class="toolbar">
      <el-button 
        type="primary" 
        @click="refreshData"
        :loading="loading"
        icon="Refresh"
      >
        刷新
      </el-button>
      <el-button 
        type="danger" 
        @click="batchForceLogout"
        :disabled="selectedUsers.length === 0"
        icon="SwitchButton"
      >
        批量强制下线 ({{ selectedUsers.length }})
      </el-button>
    </div>

    <el-card class="data-card">
      <div class="stats-row">
        <div class="stat-item">
          <div class="stat-number">{{ onlineUsers.length }}</div>
          <div class="stat-label">在线用户数</div>
        </div>
        <div class="stat-item">
          <div class="stat-number">{{ selectedUsers.length }}</div>
          <div class="stat-label">已选择</div>
        </div>
      </div>

      <el-table
        :data="onlineUsers"
        v-loading="loading"
        @selection-change="handleSelectionChange"
        stripe
        style="width: 100%"
        :table-layout="'auto'"
        :fit="true"
        class="online-users-table"
      >
        <el-table-column type="selection" width="55" />
        
        <el-table-column prop="username" label="用户名" min-width="150">
          <template #default="{ row }">
            <div class="user-info">
              <el-tag :type="getUserTypeTag(row.userType)" size="small">
                {{ getUserTypeText(row.userType) }}
              </el-tag>
              <span class="username">{{ row.username }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="userId" label="用户ID" width="100" />

        <el-table-column prop="loginTime" label="登录时间" min-width="180">
          <template #default="{ row }">
            <div class="time-info">
              <el-icon><Clock /></el-icon>
              {{ formatTime(row.loginTime) }}
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="expireTime" label="Token到期时间" min-width="180">
          <template #default="{ row }">
            <div class="time-info">
              <el-icon><Timer /></el-icon>
              {{ formatTime(row.expireTime) }}
            </div>
          </template>
        </el-table-column>

        <el-table-column label="在线时长" min-width="120">
          <template #default="{ row }">
            <el-tag type="info" size="small">
              {{ getOnlineDuration(row.loginTime) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="剩余时间" min-width="120">
          <template #default="{ row }">
            <el-tag 
              :type="getTimeLeftTag(row.expireTime)" 
              size="small"
            >
              {{ getTimeLeft(row.expireTime) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="Token状态" min-width="100">
          <template #default="{ row }">
            <el-tag 
              :type="isTokenExpiringSoon(row.expireTime) ? 'warning' : 'success'" 
              size="small"
            >
              {{ isTokenExpiringSoon(row.expireTime) ? '即将过期' : '正常' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button
              type="danger"
              size="small"
              @click="forceLogout(row)"
              icon="SwitchButton"
            >
              强制下线
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 确认对话框 -->
    <el-dialog
      v-model="confirmDialogVisible"
      title="确认操作"
      width="400px"
    >
      <p>{{ confirmMessage }}</p>
      <template #footer>
        <el-button @click="confirmDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="confirmAction">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Clock, Timer } from '@element-plus/icons-vue'
import api from '@/api'

export default {
  name: 'OnlineUsers',
  components: {
    Clock,
    Timer
  },
  setup() {
    const loading = ref(false)
    const onlineUsers = ref([])
    const selectedUsers = ref([])
    const confirmDialogVisible = ref(false)
    const confirmMessage = ref('')
    const confirmCallback = ref(null)
    let refreshTimer = null

    // 获取在线用户列表
    const fetchOnlineUsers = async () => {
      try {
        loading.value = true
        const response = await api.get('/admin/users/online')
        console.log('API响应:', response)
        
        // 直接使用响应数据，因为拦截器已经处理了
        onlineUsers.value = response || []
      } catch (error) {
        console.error('获取在线用户列表失败:', error)
        ElMessage.error(error.message || '获取在线用户列表失败')
      } finally {
        loading.value = false
      }
    }

    // 刷新数据
    const refreshData = () => {
      fetchOnlineUsers()
    }

    // 处理选择变化
    const handleSelectionChange = (selection) => {
      selectedUsers.value = selection
    }

    // 强制单个用户下线
    const forceLogout = (user) => {
      confirmMessage.value = `确定要强制用户 "${user.username}" 下线吗？`
      confirmCallback.value = async () => {
        try {
          const response = await api.post(`/admin/users/force-logout/${user.username}`)
          console.log('强制下线响应:', response)
          ElMessage.success('用户已强制下线')
          fetchOnlineUsers()
        } catch (error) {
          console.error('强制下线失败:', error)
          ElMessage.error(error.message || '强制下线失败')
        }
        confirmDialogVisible.value = false
      }
      confirmDialogVisible.value = true
    }

    // 批量强制下线
    const batchForceLogout = () => {
      if (selectedUsers.value.length === 0) {
        ElMessage.warning('请先选择要下线的用户')
        return
      }

      const usernames = selectedUsers.value.map(user => user.username)
      confirmMessage.value = `确定要强制 ${usernames.length} 个用户下线吗？\n用户：${usernames.join(', ')}`
      confirmCallback.value = async () => {
        try {
          const response = await api.post('/admin/users/batch-force-logout', {
            usernames: usernames
          })
          console.log('批量强制下线响应:', response)
          ElMessage.success(response || '批量强制下线成功')
          selectedUsers.value = []
          fetchOnlineUsers()
        } catch (error) {
          console.error('批量强制下线失败:', error)
          ElMessage.error(error.message || '批量强制下线失败')
        }
        confirmDialogVisible.value = false
      }
      confirmDialogVisible.value = true
    }

    // 确认操作
    const confirmAction = () => {
      if (confirmCallback.value) {
        confirmCallback.value()
      }
    }

    // 获取用户类型标签
    const getUserTypeTag = (userType) => {
      switch (userType) {
        case 0: return 'info'
        case 1: return 'success'
        case 2: return 'warning'
        default: return 'info'
      }
    }

    // 获取用户类型文本
    const getUserTypeText = (userType) => {
      switch (userType) {
        case 0: return '普通用户'
        case 1: return '管理员'
        case 2: return 'VIP用户'
        default: return '未知'
      }
    }

    // 格式化时间
    const formatTime = (time) => {
      if (!time) return '-'
      const date = new Date(time)
      return date.toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit'
      })
    }

    // 获取在线时长
    const getOnlineDuration = (loginTime) => {
      if (!loginTime) return '-'
      const now = new Date()
      const login = new Date(loginTime)
      const diff = now - login
      
      const hours = Math.floor(diff / (1000 * 60 * 60))
      const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60))
      
      if (hours > 0) {
        return `${hours}小时${minutes}分钟`
      } else {
        return `${minutes}分钟`
      }
    }

    // 获取剩余时间
    const getTimeLeft = (expireTime) => {
      if (!expireTime) return '-'
      const now = new Date()
      const expire = new Date(expireTime)
      const diff = expire - now
      
      if (diff <= 0) return '已过期'
      
      const days = Math.floor(diff / (1000 * 60 * 60 * 24))
      const hours = Math.floor((diff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60))
      const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60))
      
      if (days > 0) {
        return `${days}天${hours}小时`
      } else if (hours > 0) {
        return `${hours}小时${minutes}分钟`
      } else {
        return `${minutes}分钟`
      }
    }

    // 获取剩余时间标签类型
    const getTimeLeftTag = (expireTime) => {
      if (!expireTime) return 'info'
      const now = new Date()
      const expire = new Date(expireTime)
      const diff = expire - now
      
      if (diff <= 0) return 'danger'
      if (diff <= 60 * 60 * 1000) return 'danger' // 1小时内
      if (diff <= 24 * 60 * 60 * 1000) return 'warning' // 1天内
      return 'success'
    }

    // 检查token是否即将过期
    const isTokenExpiringSoon = (expireTime) => {
      if (!expireTime) return false
      const now = new Date()
      const expire = new Date(expireTime)
      const diff = expire - now
      return diff <= 24 * 60 * 60 * 1000 // 24小时内
    }

    // 启动自动刷新
    const startAutoRefresh = () => {
      refreshTimer = setInterval(() => {
        fetchOnlineUsers()
      }, 30000) // 每30秒刷新一次
    }

    // 停止自动刷新
    const stopAutoRefresh = () => {
      if (refreshTimer) {
        clearInterval(refreshTimer)
        refreshTimer = null
      }
    }

    onMounted(() => {
      fetchOnlineUsers()
      startAutoRefresh()
    })

    onUnmounted(() => {
      stopAutoRefresh()
    })

    return {
      loading,
      onlineUsers,
      selectedUsers,
      confirmDialogVisible,
      confirmMessage,
      refreshData,
      handleSelectionChange,
      forceLogout,
      batchForceLogout,
      confirmAction,
      getUserTypeTag,
      getUserTypeText,
      formatTime,
      getOnlineDuration,
      getTimeLeft,
      getTimeLeftTag,
      isTokenExpiringSoon
    }
  }
}
</script>

<style scoped>
.online-users {
  padding: 20px;
}

.page-header {
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0 0 8px 0;
  color: #303133;
  font-size: 24px;
  font-weight: 600;
}

.page-header p {
  margin: 0;
  color: #606266;
  font-size: 14px;
}

.toolbar {
  margin-bottom: 20px;
  display: flex;
  gap: 12px;
}

.data-card {
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.stats-row {
  display: flex;
  gap: 40px;
  margin-bottom: 20px;
  padding: 20px;
  background: #f8f9fa;
  border-radius: 6px;
}

.stat-item {
  text-align: center;
}

.stat-number {
  font-size: 28px;
  font-weight: 600;
  color: #409eff;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 14px;
  color: #606266;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.username {
  font-weight: 500;
}

.time-info {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
}

.time-info .el-icon {
  color: #909399;
}

/* 表格样式优化 */
.online-users-table {
  width: 100% !important;
}

:deep(.el-table) {
  font-size: 13px;
  width: 100% !important;
}

:deep(.el-table__body-wrapper) {
  width: 100% !important;
}

:deep(.el-table th) {
  background-color: #fafafa;
  font-weight: 600;
}

:deep(.el-table td) {
  padding: 12px 8px;
}

:deep(.el-table .cell) {
  padding: 0 8px;
  word-break: break-word;
}

:deep(.el-button--small) {
  padding: 5px 8px;
  font-size: 12px;
}

:deep(.el-tag--small) {
  font-size: 11px;
  padding: 0 6px;
  height: 20px;
  line-height: 18px;
}

:deep(.el-dialog__body) {
  padding: 20px;
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-line;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .online-users {
    padding: 15px;
  }
  
  .stats-row {
    flex-direction: column;
    gap: 20px;
    text-align: center;
  }
  
  .toolbar {
    flex-direction: column;
    gap: 8px;
  }
  
  :deep(.el-table) {
    font-size: 12px;
  }
  
  :deep(.el-table td) {
    padding: 8px 4px;
  }
  
  :deep(.el-table .cell) {
    padding: 0 4px;
  }
}

@media (max-width: 480px) {
  .online-users {
    padding: 10px;
  }
  
  .page-header h2 {
    font-size: 20px;
  }
  
  .stat-number {
    font-size: 24px;
  }
}
</style>