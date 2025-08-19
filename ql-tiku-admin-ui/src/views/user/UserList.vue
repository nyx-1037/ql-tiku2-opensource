<template>
  <div class="user-list">
    <!-- 操作栏 -->
    <el-card class="operation-card">
      <el-row :gutter="20" class="search-row">
        <el-col :xs="24" :sm="16" :md="16" :lg="16" class="search-col">
          <el-row :gutter="10">
            <el-col :xs="24" :sm="8" :md="6" :lg="6" class="search-item">
              <el-input
                v-model="searchForm.keyword"
                placeholder="搜索用户名/邮箱"
                clearable
                @keyup.enter="handleSearch"
              >
                <template #append>
                  <el-button icon="Search" @click="handleSearch" />
                </template>
              </el-input>
            </el-col>
            <el-col :xs="24" :sm="8" :md="4" :lg="4" class="search-item">
              <el-select v-model="searchForm.status" placeholder="状态" clearable @change="handleSearch">
                <el-option label="全部状态" value="" />
                <el-option label="正常" value="active" />
                <el-option label="禁用" value="disabled" />
              </el-select>
            </el-col>
            <el-col :xs="24" :sm="8" :md="6" :lg="6" class="search-item">
              <el-date-picker
                v-model="searchForm.registerDateRange"
                type="daterange"
                range-separator="至"
                start-placeholder="注册开始日期"
                end-placeholder="注册结束日期"
                size="default"
                @change="handleSearch"
                style="width: 100%"
              />
            </el-col>
          </el-row>
        </el-col>
        <el-col :xs="24" :sm="8" :md="8" :lg="8" class="operation-buttons">
          <el-button type="primary" icon="Plus" @click="handleAdd">新增用户</el-button>
          <el-button type="warning" icon="Download" @click="handleExport">导出用户</el-button>
          <el-button
            type="danger"
            icon="Delete"
            :disabled="selectedUsers.length === 0"
            @click="handleBatchDelete"
          >
            批量删除
          </el-button>
        </el-col>
      </el-row>
    </el-card>

    <!-- 用户列表 -->
    <el-card class="table-card">
      <el-table
        v-loading="loading"
        :data="users"
        @selection-change="handleSelectionChange"
        stripe
        style="width: 100%"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="avatar" label="头像" width="80">
          <template #default="{ row }">
            <el-avatar :size="40" :src="row.avatar">
              <el-icon><User /></el-icon>
            </el-avatar>
          </template>
        </el-table-column>
        <el-table-column prop="username" label="用户名" min-width="120" />
        <el-table-column prop="email" label="邮箱" min-width="180" />
        <el-table-column prop="userTypeName" label="用户角色" width="100">
          <template #default="{ row }">
            <el-tag :type="row.userType === 0 ? 'primary' : row.userType === 1 ? 'success' : 'warning'">
              {{ row.userTypeName }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="practiceCount" label="练习题数" width="120">
          <template #default="{ row }">
            <el-tag type="info">{{ row.practiceCount }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="examCount" label="考试次数" width="120">
          <template #default="{ row }">
            <el-tag type="success">{{ row.examCount }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-switch
              v-model="row.statusValue"
              active-value="active"
              inactive-value="disabled"
              @change="handleStatusChange(row)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="lastLoginTime" label="最后登录" width="180" />
        <el-table-column prop="registerTime" label="注册时间" width="180" />
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleView(row)">查看</el-button>
            <el-button type="warning" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="info" size="small" @click="handleResetPassword(row)">重置密码</el-button>
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

    <!-- 用户详情/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
      :before-close="handleDialogClose"
    >
      <el-form
        ref="userFormRef"
        :model="userForm"
        :rules="userRules"
        label-width="80px"
        :disabled="dialogMode === 'view'"
      >
        <el-form-item label="用户名" prop="username">
          <el-input
            v-model="userForm.username"
            placeholder="请输入用户名"
            maxlength="20"
            show-word-limit
            :disabled="dialogMode === 'edit'"
          />
        </el-form-item>

        <el-form-item label="邮箱" prop="email">
          <el-input
            v-model="userForm.email"
            placeholder="请输入邮箱"
            maxlength="50"
          />
        </el-form-item>

        <el-form-item label="密码" prop="password" v-if="dialogMode === 'add'">
          <el-input
            v-model="userForm.password"
            type="password"
            placeholder="请输入密码"
            show-password
          />
        </el-form-item>

        <el-form-item label="确认密码" prop="confirmPassword" v-if="dialogMode === 'add'">
          <el-input
            v-model="userForm.confirmPassword"
            type="password"
            placeholder="请确认密码"
            show-password
          />
        </el-form-item>

        <el-form-item label="用户角色" prop="userType">
          <el-select v-model="userForm.userType" placeholder="请选择用户角色" style="width: 100%">
            <el-option label="学生" :value="0" />
            <el-option label="教师" :value="1" />
            <el-option label="管理员" :value="2" />
          </el-select>
        </el-form-item>

        <el-form-item label="头像" prop="avatar">
          <div style="display: flex; align-items: center; gap: 10px;">
            <el-upload
              class="avatar-uploader"
              action="/api/upload/avatar"
              :show-file-list="false"
              :on-success="handleAvatarSuccess"
              :on-error="handleAvatarError"
              :before-upload="beforeAvatarUpload"
              :headers="uploadHeaders"
            >
              <el-avatar v-if="userForm.avatar" :size="60" :src="userForm.avatar" />
              <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
            </el-upload>
            <div style="flex: 1;">
              <el-input
                v-model="userForm.avatar"
                placeholder="头像URL（可直接输入或点击左侧上传）"
                maxlength="200"
              />
              <div style="font-size: 12px; color: #999; margin-top: 4px;">
                支持JPG、PNG格式，文件大小不超过2MB
              </div>
            </div>
          </div>
        </el-form-item>

        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="userForm.status">
            <el-radio label="active">正常</el-radio>
            <el-radio label="disabled">禁用</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="备注" prop="remark">
          <el-input
            v-model="userForm.remark"
            type="textarea"
            :rows="3"
            placeholder="请输入备注信息"
            maxlength="200"
            show-word-limit
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

    <!-- 用户详细信息对话框 -->
    <el-dialog v-model="detailDialogVisible" title="用户详细信息" width="800px">
      <div v-if="selectedUser" class="user-detail">
        <el-row :gutter="20">
          <el-col :span="8">
            <div class="user-avatar">
              <el-avatar :size="100" :src="selectedUser.avatar">
                <el-icon><User /></el-icon>
              </el-avatar>
            </div>
          </el-col>
          <el-col :span="16">
            <div class="user-info">
              <h3>{{ selectedUser.username }}</h3>
              <p><strong>邮箱:</strong> {{ selectedUser.email }}</p>
              <p><strong>用户角色:</strong>
                <el-tag :type="selectedUser.userType === 0 ? 'primary' : selectedUser.userType === 1 ? 'success' : 'warning'">
                  {{ selectedUser.userTypeName }}
                </el-tag>
              </p>
              <p><strong>状态:</strong>
                <el-tag :type="selectedUser.statusValue === 'active' ? 'success' : 'danger'">
                  {{ selectedUser.statusValue === 'active' ? '正常' : '禁用' }}
                </el-tag>
              </p>
              <p><strong>练习次数:</strong> {{ selectedUser.practiceCount || 0 }}</p>
              <p><strong>考试次数:</strong> {{ selectedUser.examCount || 0 }}</p>
              <p><strong>正确率:</strong> {{ selectedUser.correctRate || 0 }}%</p>
              <p><strong>学习天数:</strong> {{ selectedUser.studyDays || 0 }}天</p>
              <p><strong>注册时间:</strong> {{ selectedUser.registerTime }}</p>
              <p><strong>最后登录:</strong> {{ selectedUser.lastLoginTime }}</p>
            </div>
          </el-col>
        </el-row>

        <el-divider>学习统计</el-divider>

        <el-row :gutter="20" class="stats-row">
          <el-col :span="6">
            <div class="stat-item">
              <div class="stat-value">{{ selectedUser.practiceCount }}</div>
              <div class="stat-label">练习题数</div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="stat-item">
              <div class="stat-value">{{ selectedUser.examCount }}</div>
              <div class="stat-label">考试次数</div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="stat-item">
              <div class="stat-value">{{ selectedUser.correctRate }}%</div>
              <div class="stat-label">正确率</div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="stat-item">
              <div class="stat-value">{{ selectedUser.studyDays }}</div>
              <div class="stat-label">学习天数</div>
            </div>
          </el-col>
        </el-row>

        <el-divider>最近活动</el-divider>

        <div v-if="selectedUser.recentActivities && selectedUser.recentActivities.length > 0">
          <el-timeline>
            <el-timeline-item
              v-for="activity in selectedUser.recentActivities"
              :key="activity.id"
              :timestamp="activity.time"
            >
              {{ activity.description }}
            </el-timeline-item>
          </el-timeline>
        </div>
        <p v-else>暂无数据</p>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { userAPI } from '../../api'
import { useAuthStore } from '../../store/auth'

export default {
  name: 'UserList',
  components: {
    Plus
  },
  setup() {
    const authStore = useAuthStore()
    const loading = ref(false)
    const submitLoading = ref(false)
    const dialogVisible = ref(false)
    const detailDialogVisible = ref(false)
    const dialogMode = ref('add') // add, edit, view
    const userFormRef = ref()

    // 上传请求头
    const uploadHeaders = computed(() => ({
      'Authorization': `Bearer ${authStore.token}`
    }))

    const users = ref([])
    const selectedUsers = ref([])
    const selectedUser = ref(null)

    const searchForm = reactive({
      keyword: '',
      status: '',
      registerDateRange: []
    })

    const pagination = reactive({
      page: 1,
      size: 20,
      total: 0
    })

    const userForm = reactive({
      id: null,
      username: '',
      email: '',
      password: '',
      confirmPassword: '',
      userType: 0,
      avatar: '',
      status: 'active',
      remark: ''
    })

    const userRules = computed(() => {
      const isEdit = dialogMode.value === 'edit'
      return {
        username: [
          { required: true, message: '请输入用户名', trigger: 'blur' },
          { min: 3, max: 20, message: '用户名长度在 3 到 20 个字符', trigger: 'blur' },
          { pattern: /^[a-zA-Z0-9_]+$/, message: '用户名只能包含字母、数字和下划线', trigger: 'blur' }
        ],
        email: [
          { required: true, message: '请输入邮箱', trigger: 'blur' },
          { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
        ],
        password: isEdit ? [
          { min: 6, max: 20, message: '密码长度在 6 到 20 个字符', trigger: 'blur' }
        ] : [
          { required: true, message: '请输入密码', trigger: 'blur' },
          { min: 6, max: 20, message: '密码长度在 6 到 20 个字符', trigger: 'blur' }
        ],
        confirmPassword: isEdit ? [
          {
            validator: (rule, value, callback) => {
              if (userForm.password && value !== userForm.password) {
                callback(new Error('两次输入密码不一致'))
              } else {
                callback()
              }
            },
            trigger: 'blur'
          }
        ] : [
          { required: true, message: '请确认密码', trigger: 'blur' },
          {
            validator: (rule, value, callback) => {
              if (value !== userForm.password) {
                callback(new Error('两次输入密码不一致'))
              } else {
                callback()
              }
            },
            trigger: 'blur'
          }
        ],
        userType: [
          { required: true, message: '请选择用户角色', trigger: 'change' }
        ],
        status: [
          { required: true, message: '请选择状态', trigger: 'change' }
        ]
      }
    })

    const dialogTitle = computed(() => {
      const titles = {
        add: '新增用户',
        edit: '编辑用户',
        view: '查看用户'
      }
      return titles[dialogMode.value]
    })

    // 获取用户列表
    const getUsers = async () => {
      try {
        loading.value = true
        const response = await userAPI.getUsers({
          current: pagination.page,
          size: pagination.size,
          keyword: searchForm.keyword,
          status: searchForm.status === 'active' ? 1 : searchForm.status === 'disabled' ? 0 : undefined,
          startTime: searchForm.registerDateRange?.[0],
          endTime: searchForm.registerDateRange?.[1]
        })

        users.value = response.records || []
        pagination.total = response.total || 0
      } catch (error) {
        console.error('获取用户列表失败:', error)
        ElMessage.error('获取用户列表失败')
        // 失败时使用空数据
        users.value = []
        pagination.total = 0
      } finally {
        loading.value = false
      }
    }

    // 搜索
    const handleSearch = () => {
      pagination.page = 1
      getUsers()
    }

    // 分页大小改变
    const handleSizeChange = (size) => {
      pagination.size = size
      getUsers()
    }

    // 当前页改变
    const handleCurrentChange = (page) => {
      pagination.page = page
      getUsers()
    }

    // 选择改变
    const handleSelectionChange = (selection) => {
      selectedUsers.value = selection
    }

    // 状态改变
    const handleStatusChange = async (row) => {
      try {
        await userAPI.updateUserStatus(row.id, row.statusValue === 'active' ? 1 : 0)
        ElMessage.success(`${row.statusValue === 'active' ? '启用' : '禁用'}成功`)
        // 同步更新status字段
        row.status = row.statusValue === 'active' ? 1 : 0
      } catch (error) {
        // 恢复原状态
        row.statusValue = row.statusValue === 'active' ? 'disabled' : 'active'
        console.error('更新状态失败:', error)
        ElMessage.error('更新状态失败')
      }
    }

    // 新增用户
    const handleAdd = () => {
      dialogMode.value = 'add'
      resetUserForm()
      dialogVisible.value = true
    }

    // 查看用户
    const handleView = async (row) => {
      try {
        const userDetail = await userAPI.getUser(row.id)
        selectedUser.value = userDetail
        detailDialogVisible.value = true
      } catch (error) {
        console.error('获取用户详情失败:', error)
        ElMessage.error('获取用户详情失败')
        // 失败时使用当前行数据
        selectedUser.value = row
        detailDialogVisible.value = true
      }
    }

    // 编辑用户
    const handleEdit = (row) => {
      dialogMode.value = 'edit'
      loadUserForm(row)
      dialogVisible.value = true
    }

    // 重置密码
    const handleResetPassword = async (row) => {
      try {
        await ElMessageBox.confirm(`确定要重置用户 "${row.username}" 的密码吗？`, '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })

        await userAPI.resetPassword(row.id)
        ElMessage.success('密码重置成功，新密码已发送到用户邮箱')
      } catch (error) {
        if (error !== 'cancel') {
          console.error('重置密码失败:', error)
          ElMessage.error('重置密码失败')
        }
      }
    }

    // 删除用户
    const handleDelete = async (row) => {
      try {
        await ElMessageBox.confirm(`确定要删除用户 "${row.username}" 吗？`, '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })

        await userAPI.deleteUser(row.id)
        ElMessage.success('删除成功')
        getUsers()
      } catch (error) {
        if (error !== 'cancel') {
          console.error('删除用户失败:', error)
          ElMessage.error('删除用户失败')
        }
      }
    }

    // 批量删除
    const handleBatchDelete = async () => {
      if (selectedUsers.value.length === 0) {
        ElMessage.warning('请选择要删除的用户')
        return
      }

      try {
        await ElMessageBox.confirm(`确定要删除选中的 ${selectedUsers.value.length} 个用户吗？`, '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })

        const ids = selectedUsers.value.map(user => user.id)
        await userAPI.batchDeleteUsers(ids)
        ElMessage.success('批量删除成功')
        getUsers()
      } catch (error) {
        if (error !== 'cancel') {
          console.error('批量删除用户失败:', error)
          ElMessage.error('批量删除用户失败')
        }
      }
    }

    // 导出用户
    const handleExport = async () => {
      try {
        loading.value = true
        await userAPI.exportUsers({
          keyword: searchForm.keyword,
          status: searchForm.status === 'active' ? 1 : searchForm.status === 'disabled' ? 0 : undefined,
          startTime: searchForm.registerDateRange?.[0],
          endTime: searchForm.registerDateRange?.[1]
        })
        ElMessage.success('导出成功，文件开始下载')
      } catch (error) {
        console.error('导出用户失败:', error)
        ElMessage.error('导出用户失败')
      } finally {
        loading.value = false
      }
    }

    // 重置表单
    const resetUserForm = () => {
      Object.assign(userForm, {
        id: null,
        username: '',
        email: '',
        password: '',
        confirmPassword: '',
        userType: 0,
        avatar: '',
        status: 'active',
        remark: ''
      })
    }

    // 加载用户数据到表单
    const loadUserForm = (row) => {
      Object.assign(userForm, {
        id: row.id,
        username: row.username,
        email: row.email,
        password: '',
        confirmPassword: '',
        userType: row.userType,
        avatar: row.avatar,
        status: row.status === 1 ? 'active' : 'disabled',
        remark: row.remark || ''
      })
    }

    // 提交表单
    const handleSubmit = async () => {
      if (!userFormRef.value) return

      try {
        await userFormRef.value.validate()
        submitLoading.value = true

        const formData = { ...userForm }
        if (dialogMode.value === 'edit') {
          delete formData.password
          delete formData.confirmPassword
        }
        formData.status = formData.status === 'active' ? 1 : 0

        if (dialogMode.value === 'add') {
          await userAPI.createUser(formData)
          ElMessage.success('创建用户成功')
        } else {
          await userAPI.updateUser(formData)
          ElMessage.success('更新用户成功')
        }

        dialogVisible.value = false
        getUsers()
      } catch (error) {
        console.error('提交失败:', error)
        ElMessage.error('提交失败')
      } finally {
        submitLoading.value = false
      }
    }

    // 关闭对话框
    const handleDialogClose = () => {
      dialogVisible.value = false
      if (userFormRef.value) {
        userFormRef.value.resetFields()
      }
    }

    // 头像上传成功
    const handleAvatarSuccess = (response) => {
      if (response && response.url) {
        userForm.avatar = response.url
      }
    }

    // 头像上传失败
    const handleAvatarError = () => {
      ElMessage.error('头像上传失败')
    }

    // 头像上传前验证
    const beforeAvatarUpload = (file) => {
      const isJPG = file.type === 'image/jpeg' || file.type === 'image/png'
      const isLt2M = file.size / 1024 / 1024 < 2

      if (!isJPG) {
        ElMessage.error('头像只能是 JPG/PNG 格式!')
      }
      if (!isLt2M) {
        ElMessage.error('头像大小不能超过 2MB!')
      }
      return isJPG && isLt2M
    }

    onMounted(() => {
      getUsers()
    })

    return {
      loading,
      submitLoading,
      dialogVisible,
      detailDialogVisible,
      dialogMode,
      userFormRef,
      users,
      selectedUsers,
      selectedUser,
      searchForm,
      pagination,
      userForm,
      userRules,
      dialogTitle,
      uploadHeaders,
      getUsers,
      handleSearch,
      handleSizeChange,
      handleCurrentChange,
      handleSelectionChange,
      handleStatusChange,
      handleAdd,
      handleView,
      handleEdit,
      handleResetPassword,
      handleDelete,
      handleBatchDelete,
      handleExport,
      handleSubmit,
      handleDialogClose,
      handleAvatarSuccess,
      handleAvatarError,
      beforeAvatarUpload
    }
  }
}
</script>

<style scoped>
.user-list {
  padding: 20px;
}

.operation-card {
  margin-bottom: 20px;
}

.operation-buttons {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.table-card {
  margin-bottom: 20px;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.user-detail {
  padding: 20px;
}

.user-avatar {
  text-align: center;
  margin-bottom: 20px;
}

.user-info h3 {
  margin: 0 0 15px 0;
  color: #303133;
}

.user-info p {
  margin: 8px 0;
  color: #606266;
}

.stats-row {
  margin: 20px 0;
}

.stat-item {
  text-align: center;
  padding: 15px;
  background: #f5f7fa;
  border-radius: 8px;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: #409eff;
  margin-bottom: 5px;
}

.stat-label {
  font-size: 14px;
  color: #909399;
}

/* 响应式搜索栏样式 */
.search-row {
  width: 100%;
}

.search-col {
  margin-bottom: 0;
}

.search-item {
  margin-bottom: 10px;
}

/* 移动端响应式样式 */
@media screen and (max-width: 768px) {
  .user-list {
    padding: 10px;
  }
  
  .operation-card {
    margin-bottom: 15px;
  }
  
  .search-col {
    margin-bottom: 15px;
  }
  
  .search-item {
    margin-bottom: 8px;
  }
  
  .operation-buttons {
    flex-direction: column;
    align-items: stretch;
    gap: 8px;
  }
  
  .operation-buttons .el-button {
    width: 100%;
    margin: 0 !important;
  }
  
  .table-card {
    margin-bottom: 15px;
  }
  
  .pagination-container {
    flex-direction: column;
    align-items: center;
    gap: 10px;
  }
  
  .user-detail {
    padding: 10px;
  }
  
  .user-avatar {
    margin-bottom: 15px;
  }
  
  .user-info h3 {
    margin: 0 0 10px 0;
    font-size: 18px;
  }
  
  .user-info p {
    margin: 6px 0;
    font-size: 13px;
  }
  
  .stats-row {
    margin: 15px 0;
  }
  
  .stat-item {
    padding: 10px;
    margin-bottom: 10px;
  }
  
  .stat-value {
    font-size: 20px;
  }
  
  .stat-label {
    font-size: 12px;
  }
}

@media screen and (max-width: 480px) {
  .user-list {
    padding: 8px;
  }
  
  .operation-card {
    margin-bottom: 10px;
  }
  
  .operation-buttons {
    gap: 6px;
  }
  
  .operation-buttons .el-button {
    padding: 8px 12px;
    font-size: 12px;
  }
  
  .table-card {
    margin-bottom: 10px;
  }
  
  .user-detail {
    padding: 8px;
  }
  
  .user-avatar {
    margin-bottom: 10px;
  }
  
  .user-info h3 {
    font-size: 16px;
  }
  
  .user-info p {
    margin: 4px 0;
    font-size: 12px;
  }
  
  .stats-row {
    margin: 10px 0;
  }
  
  .stat-item {
    padding: 8px;
  }
  
  .stat-value {
    font-size: 18px;
  }
  
  .stat-label {
    font-size: 11px;
  }
}
</style>