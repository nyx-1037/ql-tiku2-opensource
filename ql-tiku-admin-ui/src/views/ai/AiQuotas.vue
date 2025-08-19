<template>
  <div class="ai-quotas">
    <div class="header">
      <h2>AI配额管理</h2>
      <div class="header-actions">
        <el-button type="primary" @click="handleBatchQuotaUpdate">
          批量设置配额
        </el-button>
      </div>
    </div>

    <div class="search-bar">
      <el-input
        v-model="quotaSearch.keyword"
        placeholder="搜索用户名或用户ID"
        @keyup.enter="loadQuotas"
        clearable
        style="width: 300px; margin-right: 10px;"
      >
        <template #append>
          <el-button @click="loadQuotas">
            <el-icon><Search /></el-icon>
          </el-button>
        </template>
      </el-input>
      <el-select
        v-model="quotaSearch.vipLevel"
        placeholder="VIP等级"
        clearable
        style="width: 150px; margin-right: 10px;"
        :loading="membershipLoading"
      >
        <el-option 
          v-for="membership in membershipOptions" 
          :key="membership.levelCode" 
          :label="membership.levelName" 
          :value="membership.levelCode" 
        />
      </el-select>
      <el-button @click="resetQuotaSearch">重置</el-button>
    </div>

    <el-table
      :data="quotaList"
      v-loading="quotaLoading"
      stripe
      style="width: 100%"
      @selection-change="handleQuotaSelectionChange"
      class="responsive-table"
    >
      <el-table-column type="selection" width="50" fixed="left" />
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="username" label="用户名" min-width="120" show-overflow-tooltip>
        <template #default="{ row }">
          <span>{{ row.username || row.nickname || `用户${row.userId}` }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="dailyQuota" label="每日配额" width="90" align="center">
        <template #default="{ row }">
          <el-tag type="primary" size="small">{{ row.dailyQuota }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="usedDaily" label="今日已用" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.usedDaily >= row.dailyQuota ? 'danger' : 'success'" size="small">{{ row.usedDaily }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="monthlyQuota" label="月度配额" width="90" align="center">
        <template #default="{ row }">
          <el-tag type="info" size="small">{{ row.monthlyQuota }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="usedMonthly" label="本月已用" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.usedMonthly >= row.monthlyQuota ? 'danger' : 'warning'" size="small">{{ row.usedMonthly }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="vipLevel" label="VIP等级" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.vipLevel > 0 ? 'warning' : 'info'" size="small">
            {{ getMembershipName(row.vipLevel) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="lastResetDate" label="最后重置" width="110" align="center">
        <template #default="{ row }">
          <span>{{ row.lastResetDate || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="updatedTime" label="更新时间" width="160" align="center">
        <template #default="{ row }">
          <span>{{ formatDateTime(row.updatedTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" min-width="300" align="center" class-name="action-column">
        <template #default="{ row }">
          <div class="action-buttons">
            <el-button type="primary" size="small" @click="editQuota(row)">
              编辑配额
            </el-button>
            <el-button type="success" size="small" @click="editMembership(row)">
              修改等级
            </el-button>
            <el-button type="warning" size="small" @click="resetDailyQuota(row)">
              重置日配额
            </el-button>
            <el-button type="info" size="small" @click="resetMonthlyQuota(row)">
              重置月配额
            </el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination">
      <el-pagination
        v-model:current-page="quotaPagination.currentPage"
        v-model:page-size="quotaPagination.pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="quotaPagination.total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadQuotas"
        @current-change="loadQuotas"
      />
    </div>

    <!-- 配额编辑对话框 -->
    <el-dialog
      v-model="quotaDialogVisible"
      :title="quotaDialogTitle"
      width="400px"
      :before-close="handleQuotaDialogClose"
    >
      <el-form
        ref="quotaFormRef"
        :model="quotaForm"
        :rules="quotaRules"
        label-width="100px"
        v-loading="quotaDialogLoading"
      >
        <el-form-item label="每日配额" prop="dailyQuota">
          <el-input-number
            v-model="quotaForm.dailyQuota"
            :min="0"
            :max="10000"
            controls-position="right"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="月度配额" prop="monthlyQuota">
          <el-input-number
            v-model="quotaForm.monthlyQuota"
            :min="0"
            :max="100000"
            controls-position="right"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="handleQuotaDialogClose">取消</el-button>
          <el-button type="primary" @click="saveQuota" :loading="quotaDialogLoading">
            保存
          </el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 批量设置配额对话框 -->
    <el-dialog
      v-model="batchQuotaDialogVisible"
      title="批量设置配额"
      width="400px"
      :before-close="handleBatchQuotaDialogClose"
    >
      <el-form
        ref="batchQuotaFormRef"
        :model="batchQuotaForm"
        :rules="batchQuotaRules"
        label-width="100px"
        v-loading="batchQuotaDialogLoading"
      >
        <el-form-item label="每日配额" prop="dailyQuota">
          <el-input-number
            v-model="batchQuotaForm.dailyQuota"
            :min="0"
            :max="10000"
            controls-position="right"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="月度配额" prop="monthlyQuota">
          <el-input-number
            v-model="batchQuotaForm.monthlyQuota"
            :min="0"
            :max="100000"
            controls-position="right"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="handleBatchQuotaDialogClose">取消</el-button>
          <el-button type="primary" @click="saveBatchQuota" :loading="batchQuotaDialogLoading">
            保存
          </el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 会员等级编辑对话框 -->
    <el-dialog
      v-model="membershipDialogVisible"
      :title="membershipDialogTitle"
      width="400px"
      :before-close="handleMembershipDialogClose"
    >
      <el-form
        ref="membershipFormRef"
        :model="membershipForm"
        :rules="membershipRules"
        label-width="100px"
        v-loading="membershipDialogLoading"
      >
        <el-form-item label="会员等级" prop="membershipLevel">
          <el-select
            v-model="membershipForm.membershipLevel"
            placeholder="请选择会员等级"
            style="width: 100%"
            :loading="membershipLoading"
          >
            <el-option 
              v-for="membership in membershipOptions" 
              :key="membership.levelCode" 
              :label="membership.levelName" 
              :value="membership.levelCode" 
            />
          </el-select>
        </el-form-item>
        <el-alert
          title="提示"
          description="修改会员等级后将自动同步对应的AI配额到用户配额表中"
          type="info"
          :closable="false"
          style="margin-bottom: 20px;"
        />
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="handleMembershipDialogClose">取消</el-button>
          <el-button type="primary" @click="saveMembership" :loading="membershipDialogLoading">
            保存并同步配额
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { aiDataAPI } from '../../api/aiData'
import { membershipAPI } from '../../api/membership'

export default {
  name: 'AiQuotas',
  components: {
    Search
  },
  setup() {
    const selectedQuotas = ref([])
    
    // AI配额相关数据
    const quotaLoading = ref(false)
    const quotaList = ref([])
    const quotaSearch = reactive({
      keyword: '',
      vipLevel: null
    })
    const quotaPagination = reactive({
      currentPage: 1,
      pageSize: 20,
      total: 0
    })

    // 会员等级相关数据
    const membershipLoading = ref(false)
    const membershipOptions = ref([])

    // 配额编辑对话框
    const quotaDialogVisible = ref(false)
    const quotaDialogLoading = ref(false)
    const quotaDialogTitle = ref('编辑配额')
    const quotaFormRef = ref()
    const quotaForm = reactive({
      id: null,
      userId: null,
      dailyQuota: 0,
      monthlyQuota: 0
    })
    const quotaRules = {
      dailyQuota: [
        { required: true, message: '请输入每日配额', trigger: 'blur' }
      ],
      monthlyQuota: [
        { required: true, message: '请输入月度配额', trigger: 'blur' }
      ]
    }

    // 批量设置配额对话框
    const batchQuotaDialogVisible = ref(false)
    const batchQuotaDialogLoading = ref(false)
    const batchQuotaFormRef = ref()
    const batchQuotaForm = reactive({
      dailyQuota: 0,
      monthlyQuota: 0
    })
    const batchQuotaRules = {
      dailyQuota: [
        { required: true, message: '请输入每日配额', trigger: 'blur' }
      ],
      monthlyQuota: [
        { required: true, message: '请输入月度配额', trigger: 'blur' }
      ]
    }

    // 会员等级编辑对话框
    const membershipDialogVisible = ref(false)
    const membershipDialogLoading = ref(false)
    const membershipDialogTitle = ref('修改会员等级')
    const membershipFormRef = ref()
    const membershipForm = reactive({
      userId: null,
      membershipLevel: null
    })
    const membershipRules = {
      membershipLevel: [
        { required: true, message: '请选择会员等级', trigger: 'change' }
      ]
    }

    // 格式化日期时间
    const formatDateTime = (datetime) => {
      if (!datetime) return ''
      return new Date(datetime).toLocaleString('zh-CN')
    }

    // 获取会员等级名称
    const getMembershipName = (levelCode) => {
      const membership = membershipOptions.value.find(m => m.levelCode === levelCode)
      return membership ? membership.levelName : `等级${levelCode}`
    }

    // 加载会员等级选项
    const loadMembershipOptions = async () => {
      try {
        membershipLoading.value = true
        const response = await membershipAPI.getActiveMemberships()
        membershipOptions.value = response
      } catch (error) {
        ElMessage.error('加载会员等级失败：' + error.message)
      } finally {
        membershipLoading.value = false
      }
    }

    // 加载配额列表
    const loadQuotas = async () => {
      try {
        quotaLoading.value = true
        const params = {
          page: quotaPagination.currentPage,
          size: quotaPagination.pageSize,
          keyword: quotaSearch.keyword,
          vipLevel: quotaSearch.vipLevel
        }
        const response = await aiDataAPI.getUserQuotas(params)
        quotaList.value = response.records
        quotaPagination.total = response.total
      } catch (error) {
        ElMessage.error('加载配额列表失败：' + error.message)
      } finally {
        quotaLoading.value = false
      }
    }

    // 重置搜索
    const resetQuotaSearch = () => {
      quotaSearch.keyword = ''
      quotaSearch.vipLevel = null
      quotaPagination.currentPage = 1
      loadQuotas()
    }

    // 处理配额选择变化
    const handleQuotaSelectionChange = (selection) => {
      selectedQuotas.value = selection
    }

    // 编辑配额
    const editQuota = (row) => {
      quotaDialogTitle.value = `编辑配额 - ${row.username || `用户${row.userId}`}`
      quotaForm.id = row.id
      quotaForm.userId = row.userId
      quotaForm.dailyQuota = row.dailyQuota
      quotaForm.monthlyQuota = row.monthlyQuota
      quotaDialogVisible.value = true
    }

    // 保存配额
    const saveQuota = async () => {
      try {
        await quotaFormRef.value.validate()
        quotaDialogLoading.value = true
        
        await aiDataAPI.updateUserQuota(quotaForm.userId, {
          dailyQuota: quotaForm.dailyQuota,
          monthlyQuota: quotaForm.monthlyQuota
        })
        
        ElMessage.success('配额更新成功')
        quotaDialogVisible.value = false
        loadQuotas()
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error('配额更新失败：' + error.message)
        }
      } finally {
        quotaDialogLoading.value = false
      }
    }

    // 编辑会员等级
    const editMembership = (row) => {
      membershipDialogTitle.value = `修改会员等级 - ${row.username || `用户${row.userId}`}`
      membershipForm.userId = row.userId
      membershipForm.membershipLevel = row.vipLevel
      membershipDialogVisible.value = true
    }

    // 保存会员等级
    const saveMembership = async () => {
      try {
        await membershipFormRef.value.validate()
        membershipDialogLoading.value = true
        
        await aiDataAPI.updateUserMembershipAndQuota(membershipForm.userId, membershipForm.membershipLevel)
        
        ElMessage.success('会员等级修改成功，AI配额已同步')
        membershipDialogVisible.value = false
        loadQuotas()
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error('会员等级修改失败：' + error.message)
        }
      } finally {
        membershipDialogLoading.value = false
      }
    }

    // 重置日配额
    const resetDailyQuota = async (row) => {
      try {
        await ElMessageBox.confirm('确定要重置该用户的日配额吗？', '确认重置', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        
        await aiDataAPI.resetDailyQuota(row.userId)
        ElMessage.success('日配额重置成功')
        loadQuotas()
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error('重置失败：' + error.message)
        }
      }
    }

    // 重置月配额
    const resetMonthlyQuota = async (row) => {
      try {
        await ElMessageBox.confirm('确定要重置该用户的月配额吗？', '确认重置', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        
        await aiDataAPI.resetMonthlyQuota(row.userId)
        ElMessage.success('月配额重置成功')
        loadQuotas()
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error('重置失败：' + error.message)
        }
      }
    }

    // 批量设置配额
    const handleBatchQuotaUpdate = () => {
      if (selectedQuotas.value.length === 0) {
        ElMessage.warning('请先选择要设置配额的用户')
        return
      }
      batchQuotaDialogVisible.value = true
    }

    // 保存批量配额
    const saveBatchQuota = async () => {
      try {
        await batchQuotaFormRef.value.validate()
        batchQuotaDialogLoading.value = true
        
        const userIds = selectedQuotas.value.map(item => item.userId)
        await aiDataAPI.batchUpdateQuotas({
          userIds,
          dailyQuota: batchQuotaForm.dailyQuota,
          monthlyQuota: batchQuotaForm.monthlyQuota
        })
        
        ElMessage.success('批量设置配额成功')
        batchQuotaDialogVisible.value = false
        loadQuotas()
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error('批量设置失败：' + error.message)
        }
      } finally {
        batchQuotaDialogLoading.value = false
      }
    }

    // 关闭配额编辑对话框
    const handleQuotaDialogClose = () => {
      quotaDialogVisible.value = false
      quotaFormRef.value?.resetFields()
    }

    // 关闭批量配额对话框
    const handleBatchQuotaDialogClose = () => {
      batchQuotaDialogVisible.value = false
      batchQuotaFormRef.value?.resetFields()
    }

    // 关闭会员等级编辑对话框
    const handleMembershipDialogClose = () => {
      membershipDialogVisible.value = false
      membershipFormRef.value?.resetFields()
    }

    onMounted(() => {
      loadQuotas()
      loadMembershipOptions()
    })

    return {
      selectedQuotas,
      quotaLoading,
      quotaList,
      quotaSearch,
      quotaPagination,
      quotaDialogVisible,
      quotaDialogLoading,
      quotaDialogTitle,
      quotaFormRef,
      quotaForm,
      quotaRules,
      batchQuotaDialogVisible,
      batchQuotaDialogLoading,
      batchQuotaFormRef,
      batchQuotaForm,
      batchQuotaRules,
      membershipLoading,
      membershipOptions,
      membershipDialogVisible,
      membershipDialogLoading,
      membershipDialogTitle,
      membershipFormRef,
      membershipForm,
      membershipRules,
      formatDateTime,
      getMembershipName,
      loadMembershipOptions,
      loadQuotas,
      resetQuotaSearch,
      handleQuotaSelectionChange,
      editQuota,
      saveQuota,
      resetDailyQuota,
      resetMonthlyQuota,
      handleBatchQuotaUpdate,
      saveBatchQuota,
      handleQuotaDialogClose,
      handleBatchQuotaDialogClose,
      editMembership,
      saveMembership,
      handleMembershipDialogClose
    }
  }
}
</script>

<style scoped>
.ai-quotas {
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

.action-buttons {
  display: flex;
  gap: 5px;
  flex-wrap: wrap;
  justify-content: center;
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
  
  .action-buttons {
    flex-direction: column;
    gap: 2px;
  }
  
  .action-buttons .el-button {
    margin: 0;
  }
}

/* 优化操作按钮布局 */
.action-column .action-buttons {
  min-width: 300px;
}

.action-buttons .el-button {
  font-size: 12px;
  padding: 5px 8px;
  margin: 2px;
}

/* 表格样式优化 */
.responsive-table {
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

/* 对话框样式优化 */
.el-dialog {
  border-radius: 8px;
}

.el-dialog__header {
  background-color: #f5f7fa;
  border-bottom: 1px solid #e4e7ed;
}

/* 标签样式优化 */
.el-tag {
  border-radius: 4px;
  font-weight: 500;
}

/* 搜索栏样式优化 */
.search-bar .el-input,
.search-bar .el-select {
  border-radius: 6px;
}

/* 分页样式优化 */
.pagination .el-pagination {
  padding: 0;
}

/* 头部样式优化 */
.header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 20px;
}

.header h2 {
  color: white;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

/* 卡片样式 */
.ai-quotas {
  background-color: #f8f9fa;
  min-height: 100vh;
}

/* 响应式优化 */
@media (max-width: 1200px) {
  .action-buttons .el-button {
    font-size: 11px;
    padding: 4px 6px;
  }
}
</style>