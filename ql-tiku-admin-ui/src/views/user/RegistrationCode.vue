<template>
  <div class="registration-code">
    <!-- 操作栏 -->
    <el-card class="operation-card">
      <el-row :gutter="20" class="search-row">
        <el-col :xs="24" :sm="16" :md="16" :lg="16" class="search-col">
          <el-row :gutter="10">
            <el-col :xs="24" :sm="8" :md="6" :lg="6" class="search-item">
              <el-input
                v-model="searchForm.keyword"
                placeholder="搜索注册码"
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
                <el-option label="未使用" value="unused" />
                <el-option label="已使用" value="used" />
                <el-option label="已过期" value="expired" />
              </el-select>
            </el-col>
            <el-col :xs="24" :sm="8" :md="6" :lg="6" class="search-item">
              <el-date-picker
                v-model="searchForm.dateRange"
                type="daterange"
                range-separator="至"
                start-placeholder="创建开始日期"
                end-placeholder="创建结束日期"
                size="default"
                @change="handleSearch"
                style="width: 100%"
              />
            </el-col>
          </el-row>
        </el-col>
        <el-col :xs="24" :sm="8" :md="8" :lg="8" class="operation-buttons">
          <el-button type="primary" icon="Plus" @click="handleGenerateSingle">生成注册码</el-button>
          <el-button type="warning" icon="Plus" @click="handleBatchGenerate">批量生成</el-button>
          <el-button
            type="danger"
            icon="Delete"
            :disabled="selectedCodes.length === 0"
            @click="handleBatchDelete"
          >
            批量删除
          </el-button>
        </el-col>
      </el-row>
    </el-card>

    <!-- 注册码列表 -->
    <el-card class="table-card">
      <el-table
        v-loading="loading"
        :data="codes"
        @selection-change="handleSelectionChange"
        stripe
        style="width: 100%"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="code" label="注册码" min-width="180" />
        <el-table-column prop="membershipLevelName" label="会员等级" width="100">
          <template #default="{ row }">
            <el-tag :type="row.membershipLevel > 0 ? 'warning' : 'info'" size="small">
              {{ row.membershipLevelName || '普通' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="isActive" label="状态" width="100">
          <template #default="{ row }">
            <el-switch
              v-model="row.isActive"
              :active-value="1"
              :inactive-value="0"
              active-color="#67C23A"
              inactive-color="#909399"
              @change="(val) => handleStatusChange(row, val)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="usedCount" label="使用次数" width="100">
          <template #default="{ row }">
            <span :class="row.usedCount === 0 ? 'text-info' : 'text-success'">
              {{ row.usedCount }}/{{ row.maxUses }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="剩余时间" width="120">
          <template #default="{ row }">
            <span :class="calculateRemainingTime(row.createdTime, row.validUntil) === '已过期' ? 'text-danger' : 'text-info'">
              {{ calculateRemainingTime(row.createdTime, row.validUntil) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="createdByUsername" label="创建人" width="120" />
        <el-table-column prop="createdTime" label="创建时间" width="180" />
        <el-table-column prop="validUntil" label="过期时间" width="180" />

        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleViewUsage(row)">查看使用记录</el-button>
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

    <!-- 生成注册码对话框 -->
    <el-dialog
      v-model="generateDialogVisible"
      :title="generateMode === 'single' ? '生成注册码' : '批量生成注册码'"
      width="500px"
      :before-close="handleGenerateDialogClose"
    >
      <el-form
        ref="generateFormRef"
        :model="generateForm"
        :rules="generateRules"
        label-width="120px"
      >
        <el-form-item label="有效期" prop="expiredDays">
          <el-input-number
            v-model="generateForm.expiredDays"
            :min="1"
            :max="365"
            style="width: 100%"
            controls-position="right"
          />
          <div class="form-tip">注册码的有效天数</div>
        </el-form-item>

        <el-form-item label="最大使用次数" prop="maxUsage">
          <el-input-number
            v-model="generateForm.maxUsage"
            :min="1"
            :max="100"
            style="width: 100%"
            controls-position="right"
          />
          <div class="form-tip">每个注册码可被使用的最大次数</div>
        </el-form-item>

        <el-form-item label="生成数量" prop="count" v-if="generateMode === 'batch'">
          <el-input-number
            v-model="generateForm.count"
            :min="1"
            :max="100"
            style="width: 100%"
            controls-position="right"
          />
          <div class="form-tip">一次性生成的注册码数量</div>
        </el-form-item>

        <el-form-item label="备注" prop="remark">
          <el-input
            v-model="generateForm.remark"
            type="textarea"
            :rows="3"
            placeholder="请输入备注信息"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="会员等级" prop="membershipLevel">
          <el-select v-model="generateForm.membershipLevel" placeholder="请选择会员等级" style="width: 100%">
            <el-option 
              v-for="level in membershipLevels" 
              :key="level.levelCode" 
              :label="level.levelName" 
              :value="level.levelCode"
            >
              <span>{{ level.levelName }}</span>
              <span style="float: right; color: #8492a6; font-size: 13px">
                日配额:{{ level.dailyQuota }} 月配额:{{ level.monthlyQuota }}
              </span>
            </el-option>
          </el-select>
          <div class="form-tip">注册此码的用户将获得的会员等级和AI配额</div>
        </el-form-item>
      </el-form>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="handleGenerateDialogClose">取消</el-button>
          <el-button type="primary" :loading="generateLoading" @click="handleGenerateSubmit">
            生成
          </el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 使用记录对话框 -->
    <el-dialog v-model="usageDialogVisible" title="注册码使用记录" width="700px">
      <div v-if="selectedCode">
        <div class="code-info">
          <p><strong>注册码:</strong> {{ selectedCode.code }}</p>
          <p><strong>状态:</strong> 
            <el-tag :type="selectedCode.isActive ? 'success' : 'info'">
              {{ selectedCode.isActive ? '启用' : '禁用' }}
            </el-tag>
          </p>
          <p><strong>使用次数:</strong> {{ selectedCode.usedCount }}/{{ selectedCode.maxUses }}</p>
          <p><strong>创建人:</strong> {{ selectedCode.createdByUsername || '未知用户' }}</p>
        </div>

        <el-table
          v-loading="usageLoading"
          :data="usageRecords"
          stripe
          style="width: 100%"
          max-height="400"
        >
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="username" label="用户名" width="120" />
          <el-table-column prop="email" label="邮箱" min-width="150" />
          <el-table-column prop="usedTime" label="使用时间" width="180" />
          <el-table-column prop="ip" label="IP地址" width="120" />
        </el-table>

        <div class="pagination-container">
          <el-pagination
            v-model:current-page="usagePagination.page"
            v-model:page-size="usagePagination.size"
            :page-sizes="[10, 20, 50]"
            :total="usagePagination.total"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleUsageSizeChange"
            @current-change="handleUsageCurrentChange"
          />
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { registrationCodeAPI, userAPI } from '../../api'

export default {
  name: 'RegistrationCode',
  setup() {
    const loading = ref(false)
    const generateLoading = ref(false)
    const usageLoading = ref(false)
    const generateDialogVisible = ref(false)
    const usageDialogVisible = ref(false)
    const generateMode = ref('single') // single, batch
    const generateFormRef = ref()

    const codes = ref([])
    const selectedCodes = ref([])
    const selectedCode = ref(null)
    const usageRecords = ref([])

    const searchForm = reactive({
      keyword: '',
      status: '',
      dateRange: []
    })

    const pagination = reactive({
      page: 1,
      size: 20,
      total: 0
    })

    const usagePagination = reactive({
      page: 1,
      size: 10,
      total: 0
    })

    const generateForm = reactive({
      expiredDays: 30,
      maxUsage: 1,
      count: 10,
      remark: '',
      membershipLevel: 0
    })

    const generateRules = {
      expiredDays: [
        { required: true, message: '请输入有效期', trigger: 'blur' },
        { type: 'number', min: 1, max: 365, message: '有效期必须在1-365天之间', trigger: 'blur' }
      ],
      maxUsage: [
        { required: true, message: '请输入最大使用次数', trigger: 'blur' },
        { type: 'number', min: 1, max: 100, message: '最大使用次数必须在1-100之间', trigger: 'blur' }
      ],
      count: [
        { required: true, message: '请输入生成数量', trigger: 'blur' },
        { type: 'number', min: 1, max: 100, message: '生成数量必须在1-100之间', trigger: 'blur' }
      ],
      membershipLevel: [
        { required: true, message: '请选择会员等级', trigger: 'change' }
      ]
    }



    const membershipLevels = ref([])
    
    // 获取会员等级列表
    const getMembershipLevels = async () => {
      try {
        const response = await userAPI.getMemberships()
        console.log('获取到的会员等级数据:', response)
        // 确保数据结构正确，处理可能的包装格式
        if (Array.isArray(response)) {
          membershipLevels.value = response
        } else if (response && Array.isArray(response.data)) {
          membershipLevels.value = response.data
        } else if (response && Array.isArray(response.records)) {
          membershipLevels.value = response.records
        } else {
          membershipLevels.value = []
          console.warn('会员等级数据格式不正确:', response)
        }
      } catch (error) {
        console.error('获取会员等级失败:', error)
        ElMessage.error('获取会员等级失败')
        membershipLevels.value = []
      }
    }

    // 计算剩余时间
    const calculateRemainingTime = (createdTime, validUntil) => {
      if (!createdTime || !validUntil) return 'N/A'
      
      const now = new Date()
      const expired = new Date(validUntil)
      
      if (expired <= now) {
        return '已过期'
      }
      
      const diffMs = expired - now
      const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24))
      const diffHours = Math.floor((diffMs % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60))
      const diffMinutes = Math.floor((diffMs % (1000 * 60 * 60)) / (1000 * 60))
      
      let result = ''
      if (diffDays > 0) {
        result += `${diffDays}天`
      }
      if (diffHours > 0) {
        result += `${diffHours}小时`
      }
      if (diffMinutes > 0 || result === '') {
        result += `${diffMinutes}分钟`
      }
      
      return result || '即将过期'
    }

    // 用户名缓存，避免重复请求
    const usernameCache = ref({})

    // 获取用户名
    const getUsername = async (userId) => {
      if (!userId) return '未知用户'
      
      // 检查缓存
      if (usernameCache.value[userId]) {
        return usernameCache.value[userId]
      }
      
      try {
        const user = await userAPI.getUser(userId)
        const username = user.username || '未知用户'
        usernameCache.value[userId] = username
        return username
      } catch (error) {
        console.error('获取用户名失败:', error)
        return '未知用户'
      }
    }

    // 获取注册码列表
    const getCodes = async () => {
      try {
        loading.value = true
        const response = await registrationCodeAPI.getRegistrationCodes({
          current: pagination.page,
          size: pagination.size,
          keyword: searchForm.keyword,
          status: searchForm.status ? (searchForm.status === 'unused' ? 0 : searchForm.status === 'used' ? 1 : 2) : undefined,
          startTime: searchForm.dateRange?.[0],
          endTime: searchForm.dateRange?.[1]
        })

        // 确保isActive字段转换为数字类型（0或1），避免Boolean类型导致的问题
        const codesData = (response.records || []).map(item => ({
          ...item,
          isActive: item.isActive ? 1 : 0
        }))
        
        // 异步获取创建人用户名和会员等级名称
        for (let code of codesData) {
          if (code.createdBy) {
            code.createdByUsername = await getUsername(code.createdBy)
          } else {
            code.createdByUsername = '未知用户'
          }
          
          // 获取会员等级名称
          const level = membershipLevels.value.find(l => l.levelCode === code.membershipLevel)
          code.membershipLevelName = level ? level.levelName : '普通用户'
        }
        
        codes.value = codesData
        pagination.total = response.total || 0
      } catch (error) {
        console.error('获取注册码列表失败:', error)
        ElMessage.error('获取注册码列表失败')
        codes.value = []
        pagination.total = 0
      } finally {
        loading.value = false
      }
    }

    // 搜索
    const handleSearch = () => {
      pagination.page = 1
      getCodes()
    }

    // 分页大小改变
    const handleSizeChange = (size) => {
      pagination.size = size
      getCodes()
    }

    // 当前页改变
    const handleCurrentChange = (page) => {
      pagination.page = page
      getCodes()
    }

    // 选择改变
    const handleSelectionChange = (selection) => {
      selectedCodes.value = selection
    }

    // 生成单个注册码
    const handleGenerateSingle = () => {
      generateMode.value = 'single'
      resetGenerateForm()
      generateDialogVisible.value = true
    }

    // 批量生成注册码
    const handleBatchGenerate = () => {
      generateMode.value = 'batch'
      resetGenerateForm()
      generateDialogVisible.value = true
    }

    // 查看使用记录
    const handleViewUsage = async (row) => {
      selectedCode.value = row
      usagePagination.page = 1
      await getUsageRecords()
      usageDialogVisible.value = true
    }

    // 删除注册码
    const handleDelete = async (row) => {
      try {
        await ElMessageBox.confirm(`确定要删除注册码 "${row.code}" 吗？`, '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })

        await registrationCodeAPI.deleteRegistrationCode(row.id)
        ElMessage.success('删除成功')
        getCodes()
      } catch (error) {
        if (error !== 'cancel') {
          console.error('删除注册码失败:', error)
          ElMessage.error('删除注册码失败')
        }
      }
    }

    // 更新注册码状态
    const handleStatusChange = async (row, newStatus) => {
      try {
        // 确保传递的是数字类型的status参数，值为1或0
        const status = Number(newStatus)
        if (status !== 0 && status !== 1) {
          throw new Error('状态值必须是0或1')
        }
        await registrationCodeAPI.updateRegistrationCodeStatus(row.id, status)
        ElMessage.success('状态更新成功')
      } catch (error) {
        console.error('状态更新失败:', error)
        ElMessage.error('状态更新失败')
        // 恢复原来的状态（注意：newStatus是当前选中的值，需要恢复为相反的值）
        row.isActive = newStatus === 0 ? 1 : 0
      }
    }

    // 批量删除注册码
    const handleBatchDelete = async () => {
      if (selectedCodes.value.length === 0) {
        ElMessage.warning('请选择要删除的注册码')
        return
      }

      try {
        await ElMessageBox.confirm(`确定要删除选中的 ${selectedCodes.value.length} 个注册码吗？`, '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })

        const ids = selectedCodes.value.map(code => code.id)
        await registrationCodeAPI.batchDeleteRegistrationCodes(ids)
        ElMessage.success('批量删除成功')
        getCodes()
      } catch (error) {
        if (error !== 'cancel') {
          console.error('批量删除注册码失败:', error)
          ElMessage.error('批量删除注册码失败')
        }
      }
    }

    // 获取使用记录
    const getUsageRecords = async () => {
      try {
        usageLoading.value = true
        const response = await registrationCodeAPI.getRegistrationCodeUsage({
          codeId: selectedCode.value.id,
          current: usagePagination.page,
          size: usagePagination.size
        })

        usageRecords.value = response.records || []
        usagePagination.total = response.total || 0
      } catch (error) {
        console.error('获取使用记录失败:', error)
        ElMessage.error('获取使用记录失败')
        usageRecords.value = []
        usagePagination.total = 0
      } finally {
        usageLoading.value = false
      }
    }

    // 使用记录分页
    const handleUsageSizeChange = (size) => {
      usagePagination.size = size
      getUsageRecords()
    }

    const handleUsageCurrentChange = (page) => {
      usagePagination.page = page
      getUsageRecords()
    }

    // 生成注册码
    const handleGenerateSubmit = async () => {
      if (!generateFormRef.value) return

      try {
        await generateFormRef.value.validate()
        generateLoading.value = true

        const params = {
          validDays: generateForm.expiredDays,
          maxUses: generateForm.maxUsage,
          remark: generateForm.remark,
          membershipLevel: generateForm.membershipLevel
        }

        if (generateMode.value === 'batch') {
          params.count = generateForm.count
        }

        const response = generateMode.value === 'batch' 
          ? await registrationCodeAPI.batchGenerateRegistrationCodes(params)
          : await registrationCodeAPI.generateRegistrationCode(params)
        
        ElMessage.success(
          generateMode.value === 'single' 
            ? '注册码生成成功' 
            : `成功生成 ${response.length} 个注册码`
        )
        
        generateDialogVisible.value = false
        getCodes()
      } catch (error) {
        console.error('生成注册码失败:', error)
        ElMessage.error('生成注册码失败')
      } finally {
        generateLoading.value = false
      }
    }

    // 重置生成表单
    const resetGenerateForm = () => {
      Object.assign(generateForm, {
        expiredDays: 30,
        maxUsage: 1,
        count: 10,
        remark: ''
      })
    }

    // 关闭生成对话框
    const handleGenerateDialogClose = () => {
    generateDialogVisible.value = false
    if (generateFormRef.value) {
      generateFormRef.value.resetFields()
    }
  }

    onMounted(() => {
      // 先加载会员等级数据，再加载注册码列表
      getMembershipLevels().then(() => {
        getCodes()
      }).catch(error => {
        console.error('初始化数据失败:', error)
        getCodes() // 即使会员等级加载失败，也要加载注册码列表
      })
    })

    return {
      loading,
      generateLoading,
      usageLoading,
      generateDialogVisible,
      usageDialogVisible,
      generateMode,
      generateFormRef,
      codes,
      selectedCodes,
      selectedCode,
      usageRecords,
      searchForm,
      pagination,
      usagePagination,
      generateForm,
      generateRules,
      membershipLevels,
      getCodes,
      handleSearch,
      handleSizeChange,
      handleCurrentChange,
      handleSelectionChange,
      handleGenerateSingle,
      handleBatchGenerate,
      handleViewUsage,
      handleDelete,
      handleBatchDelete,
      handleGenerateSubmit,
      handleGenerateDialogClose,
      handleUsageSizeChange,
      handleUsageCurrentChange,
      handleStatusChange,
      calculateRemainingTime
    }
  }
}
</script>

<style scoped>
.registration-code {
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

.code-info {
  margin-bottom: 20px;
  padding: 15px;
  background: #f5f7fa;
  border-radius: 8px;
}

.code-info p {
  margin: 8px 0;
  color: #606266;
}

.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.text-info {
  color: #909399;
}

.text-success {
  color: #67C23A;
}

.text-danger {
  color: #F56C6C;
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
  .registration-code {
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
  
  .code-info {
    padding: 10px;
    margin-bottom: 15px;
  }
  
  .code-info p {
    margin: 6px 0;
    font-size: 14px;
  }
}

@media screen and (max-width: 480px) {
  .registration-code {
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
  
  .code-info {
    padding: 8px;
    margin-bottom: 10px;
  }
  
  .code-info p {
    margin: 4px 0;
    font-size: 13px;
  }
  
  .form-tip {
    font-size: 11px;
  }
}
</style>