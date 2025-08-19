<template>
  <div class="membership-management">
    <!-- 操作栏 -->
    <el-card class="operation-card">
      <el-row :gutter="20" class="search-row">
        <el-col :xs="24" :sm="16" :md="16" :lg="16" class="search-col">
          <el-row :gutter="10">
            <el-col :xs="24" :sm="8" :md="6" :lg="6" class="search-item">
              <el-input
                v-model="searchForm.keyword"
                placeholder="搜索会员等级"
                clearable
                @keyup.enter="handleSearch"
              >
                <template #append>
                  <el-button icon="Search" @click="handleSearch" />
                </template>
              </el-input>
            </el-col>
            <el-col :xs="24" :sm="8" :md="4" :lg="4" class="search-item">
              <el-select v-model="searchForm.isActive" placeholder="状态" clearable @change="handleSearch">
                <el-option label="全部状态" value="" />
                <el-option label="启用" :value="true" />
                <el-option label="禁用" :value="false" />
              </el-select>
            </el-col>
          </el-row>
        </el-col>
        <el-col :xs="24" :sm="8" :md="8" :lg="8" class="operation-buttons">
          <el-button type="primary" icon="Plus" @click="handleCreate">新增会员等级</el-button>
          <el-button
            type="danger"
            icon="Delete"
            :disabled="selectedMemberships.length === 0"
            @click="handleBatchDelete"
          >
            批量删除
          </el-button>
        </el-col>
      </el-row>
    </el-card>

    <!-- 会员等级列表 -->
    <el-card class="table-card">
      <el-table
        v-loading="loading"
        :data="memberships"
        @selection-change="handleSelectionChange"
        stripe
        style="width: 100%"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="levelName" label="等级名称" min-width="150" />
        <el-table-column prop="levelCode" label="等级代码" width="120" />
        <el-table-column label="AI配额" width="200">
          <template #default="{ row }">
            <div>
              <div>日配额: {{ row.dailyQuota }}</div>
              <div>月配额: {{ row.monthlyQuota }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="price" label="价格(元)" width="100">
          <template #default="{ row }">
            <span>¥{{ row.price || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="isActive" label="状态" width="100">
          <template #default="{ row }">
            <el-switch
              v-model="row.isActive"
              :active-value="true"
              :inactive-value="false"
              active-color="#67C23A"
              inactive-color="#909399"
              @change="(val) => handleStatusChange(row, val)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="sortOrder" label="排序" width="80" />
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="createdTime" label="创建时间" width="180" />
        <el-table-column prop="updatedTime" label="更新时间" width="180" />

        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
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

    <!-- 新增/编辑会员等级对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogType === 'create' ? '新增会员等级' : '编辑会员等级'"
      width="600px"
      :before-close="handleDialogClose"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="formRules"
        label-width="120px"
      >
        <el-form-item label="等级名称" prop="levelName">
          <el-input
            v-model="form.levelName"
            placeholder="请输入会员等级名称"
            maxlength="50"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="等级代码" prop="levelCode">
          <el-input
            v-model="form.levelCode"
            placeholder="请输入等级代码（唯一标识）"
            maxlength="20"
            show-word-limit
            :disabled="dialogType === 'edit'"
          />
        </el-form-item>

        <el-form-item label="日AI配额" prop="dailyQuota">
          <el-input-number
            v-model="form.dailyQuota"
            :min="0"
            :max="10000"
            style="width: 100%"
            controls-position="right"
          />
          <div class="form-tip">每日可使用的AI次数</div>
        </el-form-item>

        <el-form-item label="月AI配额" prop="monthlyQuota">
          <el-input-number
            v-model="form.monthlyQuota"
            :min="0"
            :max="100000"
            style="width: 100%"
            controls-position="right"
          />
          <div class="form-tip">每月可使用的AI次数</div>
        </el-form-item>

        <el-form-item label="价格" prop="price">
          <el-input-number
            v-model="form.price"
            :min="0"
            :max="99999"
            :precision="2"
            :step="0.1"
            style="width: 100%"
            controls-position="right"
          />
          <div class="form-tip">会员等级价格（元）</div>
        </el-form-item>

        <el-form-item label="排序" prop="sortOrder">
          <el-input-number
            v-model="form.sortOrder"
            :min="0"
            :max="999"
            style="width: 100%"
            controls-position="right"
          />
          <div class="form-tip">数字越小排序越靠前</div>
        </el-form-item>

        <el-form-item label="状态" prop="isActive">
          <el-switch
            v-model="form.isActive"
            :active-value="true"
            :inactive-value="false"
            active-text="启用"
            inactive-text="禁用"
          />
        </el-form-item>

        <el-form-item label="描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="请输入会员等级描述"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="handleDialogClose">取消</el-button>
          <el-button type="primary" :loading="submitLoading" @click="handleSubmit">
            {{ dialogType === 'create' ? '创建' : '更新' }}
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { userAPI } from '../../api'

export default {
  name: 'MembershipManagement',
  setup() {
    const loading = ref(false)
    const submitLoading = ref(false)
    const dialogVisible = ref(false)
    const dialogType = ref('create') // create, edit
    const formRef = ref()

    const memberships = ref([])
    const selectedMemberships = ref([])

    const searchForm = reactive({
      keyword: '',
      isActive: ''
    })

    const pagination = reactive({
      page: 1,
      size: 20,
      total: 0
    })

    const form = reactive({
      id: null,
      levelName: '',
      levelCode: '',
      dailyQuota: 100,
      monthlyQuota: 1000,
      price: 0,
      sortOrder: 0,
      isActive: true,
      description: ''
    })

    const formRules = {
      levelName: [
        { required: true, message: '请输入会员等级名称', trigger: 'blur' },
        { min: 1, max: 50, message: '长度在 1 到 50 个字符', trigger: 'blur' }
      ],
      levelCode: [
        { required: true, message: '请输入等级代码', trigger: 'blur' },
        { min: 1, max: 20, message: '长度在 1 到 20 个字符', trigger: 'blur' },
        { pattern: /^[a-zA-Z0-9_]+$/, message: '只能包含字母、数字和下划线', trigger: 'blur' }
      ],
      dailyQuota: [
        { required: true, message: '请输入日AI配额', trigger: 'blur' },
        { type: 'number', min: 0, max: 10000, message: '必须在 0 到 10000 之间', trigger: 'blur' }
      ],
      monthlyQuota: [
        { required: true, message: '请输入月AI配额', trigger: 'blur' },
        { type: 'number', min: 0, max: 100000, message: '必须在 0 到 100000 之间', trigger: 'blur' }
      ],
      price: [
        { required: true, message: '请输入价格', trigger: 'blur' },
        { type: 'number', min: 0, max: 99999, message: '必须在 0 到 99999 之间', trigger: 'blur' }
      ],
      sortOrder: [
        { required: true, message: '请输入排序值', trigger: 'blur' },
        { type: 'number', min: 0, max: 999, message: '必须在 0 到 999 之间', trigger: 'blur' }
      ]
    }

    // 获取会员等级列表
    const getMemberships = async () => {
      try {
        loading.value = true
        const params = {
          current: pagination.page,
          size: pagination.size,
          keyword: searchForm.keyword,
          isActive: searchForm.isActive === '' ? undefined : searchForm.isActive
        }
        
        const response = await userAPI.getMemberships(params)
        console.log('获取会员等级列表:', response)
        
        if (response && Array.isArray(response.records)) {
          memberships.value = response.records
          pagination.total = response.total || 0
        } else if (Array.isArray(response)) {
          memberships.value = response
          pagination.total = response.length
        } else {
          memberships.value = []
          pagination.total = 0
        }
      } catch (error) {
        console.error('获取会员等级列表失败:', error)
        ElMessage.error('获取会员等级列表失败')
        memberships.value = []
        pagination.total = 0
      } finally {
        loading.value = false
      }
    }

    // 搜索
    const handleSearch = () => {
      pagination.page = 1
      getMemberships()
    }

    // 分页大小改变
    const handleSizeChange = (size) => {
      pagination.size = size
      getMemberships()
    }

    // 当前页改变
    const handleCurrentChange = (page) => {
      pagination.page = page
      getMemberships()
    }

    // 选择改变
    const handleSelectionChange = (selection) => {
      selectedMemberships.value = selection
    }

    // 重置表单
    const resetForm = () => {
      if (formRef.value) {
        formRef.value.resetFields()
      }
      Object.assign(form, {
        id: null,
        levelName: '',
        levelCode: '',
        dailyQuota: 100,
        monthlyQuota: 1000,
        price: 0,
        sortOrder: 0,
        isActive: true,
        description: ''
      })
    }

    // 创建会员等级
    const handleCreate = () => {
      dialogType.value = 'create'
      resetForm()
      dialogVisible.value = true
    }

    // 编辑会员等级
    const handleEdit = (row) => {
      dialogType.value = 'edit'
      Object.assign(form, {
        id: row.id,
        levelName: row.levelName,
        levelCode: row.levelCode,
        dailyQuota: row.dailyQuota || 0,
        monthlyQuota: row.monthlyQuota || 0,
        price: row.price || 0,
        sortOrder: row.sortOrder || 0,
        isActive: row.isActive,
        description: row.description || ''
      })
      dialogVisible.value = true
    }

    // 删除会员等级
    const handleDelete = async (row) => {
      try {
        await ElMessageBox.confirm(`确定要删除会员等级 "${row.levelName}" 吗？`, '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })

        await userAPI.deleteMembership(row.id)
        ElMessage.success('删除成功')
        getMemberships()
      } catch (error) {
        if (error !== 'cancel') {
          console.error('删除会员等级失败:', error)
          ElMessage.error('删除会员等级失败')
        }
      }
    }

    // 更新状态
    const handleStatusChange = async (row, newStatus) => {
      try {
        await userAPI.updateMembershipStatus(row.id, newStatus)
        ElMessage.success('状态更新成功')
        getMemberships()
      } catch (error) {
        console.error('状态更新失败:', error)
        ElMessage.error('状态更新失败')
        // 恢复原来的状态
        row.isActive = !newStatus
      }
    }

    // 批量删除
    const handleBatchDelete = async () => {
      if (selectedMemberships.value.length === 0) {
        ElMessage.warning('请选择要删除的会员等级')
        return
      }

      try {
        await ElMessageBox.confirm(`确定要删除选中的 ${selectedMemberships.value.length} 个会员等级吗？`, '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })

        const ids = selectedMemberships.value.map(item => item.id)
        await userAPI.batchDeleteMemberships(ids)
        ElMessage.success('批量删除成功')
        getMemberships()
      } catch (error) {
        if (error !== 'cancel') {
          console.error('批量删除会员等级失败:', error)
          ElMessage.error('批量删除会员等级失败')
        }
      }
    }

    // 关闭对话框
    const handleDialogClose = () => {
      dialogVisible.value = false
      resetForm()
    }

    // 提交表单
    const handleSubmit = async () => {
      if (!formRef.value) return

      try {
        await formRef.value.validate()
        submitLoading.value = true

        const data = {
          levelName: form.levelName,
          levelCode: form.levelCode,
          dailyQuota: form.dailyQuota,
          monthlyQuota: form.monthlyQuota,
          price: form.price,
          sortOrder: form.sortOrder,
          isActive: form.isActive,
          description: form.description
        }

        if (dialogType.value === 'create') {
          await userAPI.createMembership(data)
          ElMessage.success('创建成功')
        } else {
          await userAPI.updateMembership(form.id, data)
          ElMessage.success('更新成功')
        }

        dialogVisible.value = false
        getMemberships()
      } catch (error) {
        console.error('提交失败:', error)
        ElMessage.error('提交失败')
      } finally {
        submitLoading.value = false
      }
    }

    onMounted(() => {
      getMemberships()
    })

    return {
      loading,
      submitLoading,
      dialogVisible,
      dialogType,
      formRef,
      memberships,
      selectedMemberships,
      searchForm,
      pagination,
      form,
      formRules,
      getMemberships,
      handleSearch,
      handleSizeChange,
      handleCurrentChange,
      handleSelectionChange,
      handleCreate,
      handleEdit,
      handleDelete,
      handleStatusChange,
      handleBatchDelete,
      handleDialogClose,
      handleSubmit
    }
  }
}
</script>

<style scoped>
.membership-management {
  padding: 20px;
}

.operation-card {
  margin-bottom: 20px;
}

.search-row {
  align-items: center;
}

.search-col {
  display: flex;
  align-items: center;
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
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

@media (max-width: 768px) {
  .operation-buttons {
    justify-content: flex-start;
    margin-top: 10px;
  }
}
</style>