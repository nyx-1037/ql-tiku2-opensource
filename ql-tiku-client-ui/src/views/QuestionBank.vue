<template>
  <div class="question-bank-container">
    <!-- 加载状态组件 -->
    <LoadingStates
      :loading="isLoading"
      :error="error"
      :retry-count="retryCount"
      :max-retries="maxRetries"
      @retry="handleRetry"
      @clear-error="clearError"
    />

    <t-card title="题库数据" class="subject-list-card">
      <template #actions>
        <t-input
          v-model="searchKeyword"
          placeholder="搜索科目名称"
          clearable
          @clear="handleSearch"
          @keyup.enter="handleSearch"
        >
          <template #suffix-icon>
            <SearchIcon @click="handleSearch" />
          </template>
        </t-input>
      </template>
      
      <t-table
        :data="filteredSubjects"
        :columns="columns"
        :loading="loading"
        row-key="id"
        hover
        stripe
        :pagination="null"
        empty-text="暂无科目数据"
      >
        <template #status="{ row }">
          <t-tag :theme="row.status === 1 ? 'success' : 'warning'">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </t-tag>
        </template>
        
        <template #questionCount="{ row }">
          <span>{{ row.questionCount || 0 }}</span>
        </template>
        
        <template #operation="{ row }">
          <t-button
            theme="primary"
            variant="text"
            @click="viewSubjectDetail(row)"
          >
            查看详情
          </t-button>
        </template>
      </t-table>
      
      <div class="pagination-container">
        <CustomPagination
          :current="currentPage"
          :pageSize="pageSize"
          :total="filteredTotal"
          @current-change="handlePageChange"
          @page-size-change="handlePageSizeChange"
        />
      </div>
    </t-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { SearchIcon } from 'tdesign-icons-vue-next'
import CustomPagination from '../components/CustomPagination.vue'
import LoadingStates from '../components/LoadingStates.vue'
import { useQuestionBankStore } from '@/store/questionBank'

const router = useRouter()
// 使用缓存store
const questionBankStore = useQuestionBankStore()

const searchKeyword = ref('')

// 从store获取数据
const loading = computed(() => questionBankStore.loading)
const subjects = computed(() => questionBankStore.subjects)
const currentPage = ref(1)
const pageSize = ref(10)

// 加载状态和错误处理
const isLoading = computed(() => questionBankStore.isLoading)
const error = computed(() => questionBankStore.error)
const retryCount = computed(() => questionBankStore.retryCount)
const maxRetries = computed(() => questionBankStore.maxRetries)

// 错误处理方法
const handleRetry = () => {
  questionBankStore.retry()
}

const clearError = () => {
  questionBankStore.clearError()
}

// 表格列定义
const columns = [
  {
    colKey: 'id',
    title: 'ID',
    width: 80
  },
  {
    colKey: 'name',
    title: '科目名称',
    width: 200
  },
  {
    colKey: 'description',
    title: '科目描述',
    ellipsis: true
  },
  {
    colKey: 'status',
    title: '状态',
    width: 100,
    cell: 'status'
  },
  {
    colKey: 'questionCount',
    title: '题目数量',
    width: 100,
    cell: 'questionCount'
  },
  {
    colKey: 'operation',
    title: '操作',
    width: 120,
    fixed: 'right',
    cell: 'operation'
  }
]

// 过滤后的科目总数
const filteredTotal = computed(() => {
  if (!searchKeyword.value) {
    return subjects.value.length
  }
  
  return subjects.value.filter(subject => 
    subject.name.toLowerCase().includes(searchKeyword.value.toLowerCase()) ||
    (subject.description && subject.description.toLowerCase().includes(searchKeyword.value.toLowerCase()))
  ).length
})

// 过滤后的科目列表（带分页）
const filteredSubjects = computed(() => {
  let filtered = subjects.value
  
  if (searchKeyword.value) {
    filtered = filtered.filter(subject => 
      subject.name.toLowerCase().includes(searchKeyword.value.toLowerCase()) ||
      (subject.description && subject.description.toLowerCase().includes(searchKeyword.value.toLowerCase()))
    )
  }
  
  // 应用分页
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  
  return filtered.slice(start, end)
})

// 获取科目列表 - 使用store方法
const fetchSubjects = () => {
  questionBankStore.loadSubjects()
}

// 搜索处理
const handleSearch = () => {
  currentPage.value = 1
  // 可以在这里添加搜索逻辑，如果需要的话
}

// 页码变化处理 - 使用store方法
const handlePageChange = (page) => {
  currentPage.value = page
}

// 每页条数变化处理 - 使用store方法
const handlePageSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
}

// 查看科目详情
const viewSubjectDetail = (subject) => {
  router.push({
    name: 'QuestionBankDetail',
    params: { id: subject.id },
    query: { name: subject.name }
  })
}

// 组件挂载时初始化store
onMounted(() => {
  // 初始化数据（会优先使用缓存）
  questionBankStore.initializeData()
})

// 组件卸载时清理
onUnmounted(() => {
  // 可以在这里添加清理逻辑，如取消未完成的请求
})
</script>

<style scoped>
.question-bank-container {
  padding: 20px;
}

.subject-list-card {
  margin-bottom: 20px;
}

.pagination-container {
  margin-top: 20px;
}
</style>