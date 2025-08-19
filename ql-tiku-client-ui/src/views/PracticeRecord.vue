<template>
  <div class="practice-record-page page-container">
    <div class="practice-record">
      <!-- 加载状态组件 -->
      <LoadingStates
        :loading="isLoading"
        :error="error"
        :retry-count="retryCount"
        :max-retries="maxRetries"
        @retry="handleRetry"
        @clear-error="clearError"
      />

      <t-card class="header-card">
      <div class="header-content">
        <h2>刷题记录</h2>
        <p>查看您的刷题历史和练习统计</p>
      </div>
      </t-card>

      <!-- 统计卡片 -->
      <div class="stats-cards">
      <t-row :gutter="20">
        <t-col :span="6">
          <t-card class="stat-card">
            <div class="stat-item">
              <div class="stat-value">{{ totalPractices }}</div>
              <div class="stat-label">总练习次数</div>
            </div>
          </t-card>
        </t-col>
        <t-col :span="6">
          <t-card class="stat-card">
            <div class="stat-item">
              <div class="stat-value">{{ totalQuestions }}</div>
              <div class="stat-label">总刷题数</div>
            </div>
          </t-card>
        </t-col>
        <t-col :span="6">
          <t-card class="stat-card">
            <div class="stat-item">
              <div class="stat-value">{{ correctRate }}%</div>
              <div class="stat-label">正确率</div>
            </div>
          </t-card>
        </t-col>
        <t-col :span="6">
          <t-card class="stat-card">
            <div class="stat-item">
              <div class="stat-value">{{ totalTime }}</div>
              <div class="stat-label">总用时</div>
            </div>
          </t-card>
        </t-col>
        </t-row>
      </div>

      <!-- 筛选条件 -->
      <t-card class="filter-card">
        <div class="filter-form">
          <div class="filter-row">
            <div class="filter-item">
              <label class="filter-label">科目</label>
              <Multiselect
                v-model="filters.subjectId"
                :options="subjects"
                value-prop="id"
                label="name"
                placeholder="请选择科目"
                :can-clear="true"
                :classes="{
                  container: 'practice-record-multiselect-container',
                  dropdown: 'practice-record-select-dropdown'
                }"
                class="filter-select"
              />
            </div>
            <div class="filter-item">
              <label class="filter-label">题目类型</label>
              <Multiselect
                v-model="filters.questionType"
                :options="[
                  { value: '0', label: '单选题' },
                  { value: '1', label: '多选题' },
                  { value: '2', label: '判断题' },
                  { value: '3', label: '简答题' }
                ]"
                value-prop="value"
                label="label"
                placeholder="请选择题目类型"
                :can-clear="true"
                :classes="{
                  container: 'practice-record-multiselect-container',
                  dropdown: 'practice-record-select-dropdown'
                }"
                class="filter-select"
              />
            </div>
            <div class="filter-item">
              <label class="filter-label">难度</label>
              <Multiselect
                v-model="filters.difficulty"
                :options="[
                  { value: '1', label: '简单' },
                  { value: '2', label: '中等' },
                  { value: '3', label: '困难' }
                ]"
                value-prop="value"
                label="label"
                placeholder="请选择难度"
                :can-clear="true"
                :classes="{
                  container: 'practice-record-multiselect-container',
                  dropdown: 'practice-record-select-dropdown'
                }"
                class="filter-select"
              />
            </div>
            <div class="filter-actions">
              <t-button theme="primary" @click="loadPracticeRecords">查询</t-button>
              <t-button theme="default" @click="resetFilters">重置</t-button>
            </div>
          </div>
        </div>
      </t-card>

      <!-- 练习记录列表 -->
      <t-card class="record-card">
      <template #header>
        <div class="card-header">
          <span>练习记录</span>
        </div>
      </template>


      <!-- 简单的数据列表显示 -->
      <div v-if="practiceRecords.length > 0" class="simple-table">
        <div class="table-header">
          <div class="header-cell">记录ID</div>
          <div class="header-cell">科目</div>
          <div class="header-cell">题目类型</div>
          <div class="header-cell">难度</div>
          <div class="header-cell">题目数量</div>
          <div class="header-cell">正确数</div>
          <div class="header-cell">正确率</div>
          <div class="header-cell">用时</div>
          <div class="header-cell">练习时间</div>
          <div class="header-cell">状态</div>
          <div class="header-cell">操作</div>
        </div>
        <div
          v-for="record in practiceRecords"
          :key="record.id"
          class="table-row"
        >
          <div class="table-cell" data-label="记录ID">{{ record.id }}</div>
          <div class="table-cell" data-label="科目">{{ record.subjectName }}</div>
          <div class="table-cell" data-label="题目类型">
            <t-tag :theme="getQuestionTypeTagType(record.questionType)" size="small">
              {{ getQuestionTypeText(record.questionType) }}
            </t-tag>
          </div>
          <div class="table-cell" data-label="难度">
            <t-tag :theme="getDifficultyTagType(record.difficulty)" size="small">
              {{ getDifficultyText(record.difficulty) }}
            </t-tag>
          </div>
          <div class="table-cell" data-label="题目数量">{{ record.totalQuestions }}</div>
          <div class="table-cell" data-label="正确数">{{ record.correctCount }}</div>
          <div class="table-cell" data-label="正确率">
            <span :class="getAccuracyClass(record.accuracyRate || record.accuracy || 0)">{{ (record.accuracyRate !== undefined ? record.accuracyRate : (record.accuracy !== undefined ? record.accuracy : 0)) }}%</span>
          </div>
          <div class="table-cell" data-label="用时">{{ record.duration }}秒</div>
          <div class="table-cell" data-label="练习时间">{{ formatTime(record.createTime || record.createdAt || record.create_time) }}</div>
          <div class="table-cell" data-label="状态">
            <t-tag :theme="getStatusTagType(record.status)" size="small">
              {{ getStatusText(record.status) }}
            </t-tag>
          </div>
          <div class="table-cell action-cell" data-label="操作">
            <t-button
              theme="primary"
              size="small"
              @click.stop="viewRecordDetails(record)"
            >
              查看详情
            </t-button>
          </div>
        </div>
      </div>

      <!-- 如果没有数据显示空状态 -->
      <div v-if="practiceRecords.length === 0 && !loading" class="empty-state">
        <t-empty description="暂无练习记录" />
      </div>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <CustomPagination
          :current="pagination.currentPage"
          :page-size="pagination.pageSize"
          :total="pagination.total"
          :page-size-options="[
            { value: 10, label: '10' },
            { value: 20, label: '20' },
            { value: 50, label: '50' },
            { value: 100, label: '100' }
          ]"
          @current-change="handleCurrentChange"
          @page-size-change="handleSizeChange"
        />
        </div>
      </t-card>

      <!-- 练习详情对话框 -->
      <div v-if="detailDialogVisible" class="modal-overlay" @click="handleDetailDialogClose">
        <div class="modal-container" @click.stop>
          <div class="modal-header">
            <h3>练习详情</h3>
            <t-button theme="default" variant="text" @click="handleDetailDialogClose">
              ✕
            </t-button>
          </div>
          <div class="modal-body">
            <div v-if="currentRecord" class="record-detail">
              <!-- 练习信息 -->
              <t-card class="detail-info-card">
                <template #header>
                  <span>练习信息</span>
                </template>
                <t-descriptions :column="3" border>
                  <t-descriptions-item label="记录ID">{{ currentRecord.id }}</t-descriptions-item>
                  <t-descriptions-item label="科目">{{ currentRecord.subjectName }}</t-descriptions-item>
                  <t-descriptions-item label="题目类型">
                    <t-tag :theme="getQuestionTypeTagType(currentRecord.questionType)" size="small">
                      {{ getQuestionTypeText(currentRecord.questionType) }}
                    </t-tag>
                  </t-descriptions-item>
                  <t-descriptions-item label="难度">
                    <t-tag :theme="getDifficultyTagType(currentRecord.difficulty)" size="small">
                      {{ getDifficultyText(currentRecord.difficulty) }}
                    </t-tag>
                  </t-descriptions-item>
                  <t-descriptions-item label="题目数量">{{ currentRecord.totalQuestions }}</t-descriptions-item>
                  <t-descriptions-item label="正确数量">{{ currentRecord.correctCount }}</t-descriptions-item>
                  <t-descriptions-item label="错误数量">{{ currentRecord.wrongCount }}</t-descriptions-item>
                  <t-descriptions-item label="正确率">
                    <span :class="getAccuracyClass(currentRecord.accuracyRate)">{{ currentRecord.accuracyRate }}%</span>
                  </t-descriptions-item>
                  <t-descriptions-item label="总用时">{{ formatDuration(currentRecord.duration) }}</t-descriptions-item>
                  <t-descriptions-item label="练习时间">{{ formatTime(currentRecord.createTime) }}</t-descriptions-item>
                  <t-descriptions-item label="状态">
                    <t-tag :theme="getStatusTagType(currentRecord.status)" size="small">
                      {{ getStatusText(currentRecord.status) }}
                    </t-tag>
                  </t-descriptions-item>
                </t-descriptions>
              </t-card>

              <!-- 答题详情 -->
              <t-card class="detail-questions-card">
                <template #header>
                  <span>答题详情</span>
                </template>
                <div v-if="detailLoading" class="loading-container">
                  <t-loading size="large" text="加载中..." />
                </div>
                <div v-else-if="recordDetails.length === 0" class="empty-container">
                  <t-empty description="暂无答题记录" />
                </div>
                <div v-else class="detail-table-container">
                  <div class="detail-table-header">
                    <div class="detail-header-cell">序号</div>
                    <div class="detail-header-cell">题目内容</div>
                    <div class="detail-header-cell">题目类型</div>
                    <div class="detail-header-cell">难度</div>
                    <div class="detail-header-cell">我的答案</div>
                    <div class="detail-header-cell">正确答案</div>
                    <div class="detail-header-cell">结果</div>
                    <div class="detail-header-cell">用时(秒)</div>
                    <div class="detail-header-cell">答题时间</div>
                  </div>
                  <div class="detail-table-body">
                    <div
                      v-for="(item, index) in recordDetails"
                      :key="index"
                      class="detail-table-row"
                    >
                      <div class="detail-table-cell">{{ index + 1 }}</div>
                      <div class="detail-table-cell" :title="item.questionContent">
                        {{ item.questionContent }}
                      </div>
                      <div class="detail-table-cell">
                        <t-tag :theme="getQuestionTypeTagType(item.questionType)" size="small">
                          {{ getQuestionTypeText(item.questionType) }}
                        </t-tag>
                      </div>
                      <div class="detail-table-cell">
                        <t-tag :theme="getDifficultyTagType(item.difficulty)" size="small">
                          {{ getDifficultyText(item.difficulty) }}
                        </t-tag>
                      </div>
                      <div class="detail-table-cell">{{ item.user_answer }}</div>
                      <div class="detail-table-cell">{{ item.correctAnswer }}</div>
                      <div class="detail-table-cell">
                        <t-tag :theme="item.is_correct ? 'success' : 'danger'" size="small">
                          {{ item.is_correct ? '正确' : '错误' }}
                        </t-tag>
                      </div>
                      <div class="detail-table-cell">{{ item.timeSpent }}</div>
                      <div class="detail-table-cell">{{ formatTime(item.answeredAt) }}</div>
                    </div>
                  </div>
                </div>
              </t-card>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, reactive, onMounted, computed, nextTick, watch } from 'vue'
import { MessagePlugin } from 'tdesign-vue-next'
import { usePracticeRecordStore } from '@/store/practiceRecord'
import { subjectAPI, practiceRecordAPI } from '@/api'
import Multiselect from '@vueform/multiselect'
import CustomPagination from '../components/CustomPagination.vue'
import LoadingStates from '../components/LoadingStates.vue'

export default {
  name: 'PracticeRecord',
  components: {
    Multiselect,
    CustomPagination,
    LoadingStates
  },
  setup() {
    // 使用缓存store
    const practiceRecordStore = usePracticeRecordStore()
    
    const detailLoading = ref(false)
    const detailDialogVisible = ref(false)
    const recordDetails = ref([])
    const currentRecord = ref(null)
    const subjects = ref([])

    // 从store获取数据
    const practiceRecords = computed(() => practiceRecordStore.practiceRecords)
    const total = computed(() => practiceRecordStore.total)
    const loading = computed(() => practiceRecordStore.loading)
    const queryForm = computed(() => practiceRecordStore.queryForm)

    // 加载状态和错误处理
    const isLoading = computed(() => practiceRecordStore.isLoading)
    const error = computed(() => practiceRecordStore.error)
    const retryCount = computed(() => practiceRecordStore.retryCount)
    const maxRetries = computed(() => practiceRecordStore.maxRetries)

    // 错误处理方法
    const handleRetry = () => {
      practiceRecordStore.retry()
    }

    const clearError = () => {
      practiceRecordStore.clearError()
    }

    // 本地筛选条件（用于UI绑定）
    const filters = reactive({
      subjectId: '',
      questionType: '',
      difficulty: ''
    })

    const pagination = reactive({
      currentPage: 1,
      pageSize: 10,
      total: 0
    })

    // 同步分页数据
    watch(() => practiceRecordStore.total, (newTotal) => {
      pagination.total = newTotal
    })

    watch(() => practiceRecordStore.queryForm.current, (newPage) => {
      pagination.currentPage = newPage
    })

    watch(() => practiceRecordStore.queryForm.size, (newSize) => {
      pagination.pageSize = newSize
    })

    // 统计数据
    const totalPractices = computed(() => practiceRecords.value.length)
    const totalQuestions = computed(() => {
      return practiceRecords.value.reduce((sum, record) => sum + (record.totalQuestions || 0), 0)
    })
    const correctRate = computed(() => {
      const totalCorrect = practiceRecords.value.reduce((sum, record) => sum + record.correctCount, 0)
      const totalCount = totalQuestions.value
      return totalCount > 0 ? Math.round((totalCorrect / totalCount) * 100) : 0
    })
    const totalTime = computed(() => {
      const totalSeconds = practiceRecords.value.reduce((sum, record) => {
        return sum + (record.duration || 0)
      }, 0)
      return formatDuration(totalSeconds)
    })

    // 加载科目列表
    const loadSubjects = async () => {
      try {
        const response = await subjectAPI.getSubjects()
        if (Array.isArray(response)) {
          subjects.value = response
        } else if (response.code === 200 && Array.isArray(response.data)) {
          subjects.value = response.data
        } else {
          console.error('获取科目数据格式错误:', response)
          subjects.value = []
        }
      } catch (error) {
        console.error('加载科目失败:', error)
        subjects.value = []
      }
    }

    // 加载练习记录 - 使用store方法
    const loadPracticeRecords = () => {
      // 同步筛选条件到store
      practiceRecordStore.queryForm.subjectId = filters.subjectId || null
      practiceRecordStore.queryForm.questionType = filters.questionType || null
      practiceRecordStore.queryForm.difficulty = filters.difficulty || null
      practiceRecordStore.queryForm.current = pagination.currentPage
      practiceRecordStore.queryForm.size = pagination.pageSize
      
      practiceRecordStore.loadPracticeRecords(false) // 筛选时不使用缓存
    }

    // 查看记录详情
    const viewRecordDetails = async (record) => {
      currentRecord.value = record
      detailDialogVisible.value = true
      detailLoading.value = true
      
      try {
        const response = await practiceRecordStore.getPracticeRecordDetail(record.id)
        recordDetails.value = response || []
        MessagePlugin.success('练习详情加载成功')
      } catch (error) {
        console.error('加载练习详情失败:', error)
        MessagePlugin.error('加载练习详情失败: ' + (error.message || '未知错误'))
        recordDetails.value = []
      } finally {
        detailLoading.value = false
      }
    }

    // 重置筛选条件 - 使用store方法
    const resetFilters = () => {
      Object.assign(filters, {
        subjectId: '',
        questionType: '',
        difficulty: ''
      })
      pagination.currentPage = 1
      practiceRecordStore.resetQuery()
    }

    // 分页处理 - 使用store方法
    const handleSizeChange = (size) => {
      pagination.pageSize = size
      pagination.currentPage = 1
      practiceRecordStore.handleSizeChange(size)
    }

    const handleCurrentChange = (page) => {
      pagination.currentPage = page
      practiceRecordStore.handleCurrentChange(page)
    }

    // 关闭详情对话框
    const handleDetailDialogClose = () => {
      detailDialogVisible.value = false
      currentRecord.value = null
      recordDetails.value = []
    }

    // 工具函数
    const getQuestionTypeText = (type) => {
      const types = {
        0: '单选题',
        1: '多选题',
        2: '判断题',
        3: '简答题',
        4: '简答题'
      }
      return types[type] || '未知'
    }

    const getQuestionTypeTagType = (type) => {
      const tagTypes = {
        '0': 'primary',  // 单选题
        '1': 'success',  // 多选题
        '2': 'warning',  // 判断题
        '3': 'default',  // 填空题
        '4': 'danger'    // 简答题
      }
      return tagTypes[String(type)] || 'default'
    }

    const getDifficultyText = (difficulty) => {
      const difficulties = {
        '1': '简单',
        '2': '中等',
        '3': '困难'
      }
      return difficulties[String(difficulty)] || '未知'
    }

    const getDifficultyTagType = (difficulty) => {
      const tagTypes = {
        '1': 'success',
        '2': 'warning',
        '3': 'danger'
      }
      return tagTypes[String(difficulty)] || 'default'
    }

    const getStatusText = (status) => {
      const statuses = {
        '1': '进行中',
        '2': '已完成',
        '3': '已暂停'
      }
      return statuses[String(status)] || '未知'
    }

    const getStatusTagType = (status) => {
      const tagTypes = {
        '1': 'warning',
        '2': 'success',
        '3': 'default'
      }
      return tagTypes[String(status)] || 'default'
    }

    const getAccuracyClass = (accuracy) => {
      if (accuracy >= 80) return 'accuracy-high'
      if (accuracy >= 60) return 'accuracy-medium'
      return 'accuracy-low'
    }

    const formatTime = (timeStr) => {
      if (!timeStr) return '-'
      const date = new Date(timeStr)
      return date.toLocaleString('zh-CN')
    }

    const formatDuration = (seconds) => {
      if (seconds < 60) return `${seconds}秒`
      if (seconds < 3600) return `${Math.floor(seconds / 60)}分${seconds % 60}秒`
      const hours = Math.floor(seconds / 3600)
      const minutes = Math.floor((seconds % 3600) / 60)
      const secs = seconds % 60
      return `${hours}时${minutes}分${secs}秒`
    }

    // 组件挂载时初始化数据 - 使用store的初始化方法
    onMounted(async () => {
      console.log('🎯 PracticeRecord.vue: 组件已挂载')

      await loadSubjects()
      await nextTick() // 确保DOM更新

      // 使用store的初始化方法
      await practiceRecordStore.initialize()
    })

    return {
      loading,
      detailLoading,
      detailDialogVisible,
      practiceRecords,
      recordDetails,
      currentRecord,
      subjects,
      filters,
      pagination,
      totalPractices,
      totalQuestions,
      correctRate,
      totalTime,
      loadPracticeRecords,
      viewRecordDetails,
      resetFilters,
      handleSizeChange,
      handleCurrentChange,
      handleDetailDialogClose,
      getQuestionTypeText,
      getQuestionTypeTagType,
      getDifficultyText,
      getDifficultyTagType,
      getStatusText,
      getStatusTagType,
      getAccuracyClass,
      formatTime,
      formatDuration,
      // 错误处理相关
      isLoading,
      error,
      retryCount,
      maxRetries,
      handleRetry,
      clearError
    }
  }
}
</script>

<style scoped>
.practice-record-page {
  padding: 20px;
  min-height: calc(100vh - 120px);
  background-color: #f5f7fa;
  max-width: 100%;
  width: 100%;
  box-sizing: border-box;
}

.practice-record {
  background: #f5f5f5;
  min-height: 100vh;
  width: 100%;
  max-width: 100%;
  box-sizing: border-box;
}

.practice-record .header-card {
  margin: 0 0 20px 0;
  border-radius: 8px;
}

.practice-record .header-card .t-card__body {
  padding: 20px;
}

.practice-record .stats-cards,
.practice-record .filter-card,
.practice-record .record-card {
  margin: 0 0 20px 0;
  width: 100%;
}

.header-card {
  margin-bottom: 20px;
  width: 100%;
}

.header-content {
  text-align: center;
  position: relative;
}

.header-actions {
  position: absolute;
  top: 0;
  right: 0;
}
.header-content h2 {
  margin: 0 0 8px 0;
  color: #303133;
  font-size: 24px;
}

.header-content p {
  margin: 0;
  color: #909399;
  font-size: 14px;
}

.stats-cards {
  margin-bottom: 20px;
  width: 100%;
}

.stat-card {
  text-align: center;
  cursor: pointer;
  transition: all 0.3s ease;
  height: 100%;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.stat-item {
  padding: 10px;
}

.stat-value {
  font-size: 28px;
  font-weight: 600;
  color: #409EFF;
  margin-bottom: 8px;
}

.stat-label {
  font-size: 14px;
  color: #909399;
}

.filter-card {
  margin-bottom: 20px;
  width: 100%;
}

.filter-form {
  padding: 0;
}

.filter-row {
  display: flex;
  align-items: flex-end;
  gap: 20px;
  flex-wrap: wrap;
}

.filter-item {
  display: flex;
  flex-direction: column;
  width: 200px;
  flex-shrink: 0;
}

.filter-label {
  font-size: 14px;
  color: #606266;
  margin-bottom: 8px;
  font-weight: 500;
}

.filter-select {
  width: 200px;
}

.filter-actions {
  display: flex;
  gap: 8px;
  align-items: flex-end;
  margin-left: auto;
  flex-shrink: 0;
}

.filter-actions .t-button {
  height: 32px;
}

/* 响应式设计 - 移动端三个搜索栏变为三行 */
@media (max-width: 768px) {
  .filter-row {
    flex-direction: column;
    align-items: stretch;
    gap: 16px;
  }
  
  .filter-item {
    width: 100%;
  }
  
  .filter-select {
    width: 100%;
  }
  
  .filter-actions {
    margin-left: 0;
    justify-content: flex-start;
    flex-direction: column;
    gap: 8px;
  }
  
  .filter-actions .t-button {
    width: 100%;
  }
}

/* 平板端适配 */
@media (min-width: 769px) and (max-width: 1024px) {
  .filter-row {
    justify-content: flex-start;
  }
  
  .filter-actions {
    margin-left: 20px;
  }
}

.record-card {
  margin-bottom: 20px;
  width: 100%;
  overflow-x: auto;
}

.card-header {
  font-weight: 600;
  font-size: 16px;
}

.record-table {
  cursor: pointer;
}

.record-table .t-table__row:hover {
  background-color: #f5f7fa;
}

.pagination-wrapper {
  margin-top: 20px;
  text-align: center;
  width: 100%;
}

.record-detail {
  max-height: 70vh;
  overflow-y: auto;
}

.detail-info-card {
  margin-bottom: 20px;
}

.detail-questions-card {
  margin-bottom: 20px;
}

.accuracy-high {
  color: #67C23A;
  font-weight: 600;
}

.accuracy-medium {
  color: #E6A23C;
  font-weight: 600;
}

.accuracy-low {
  color: #F56C6C;
  font-weight: 600;
}

/* PracticeRecord Multiselect 自定义样式 */
:deep(.practice-record-multiselect-container) {
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  min-height: 32px;
  width: 100% !important;
  min-width: 180px !important;
  background: white;
  transition: all 0.2s;
  position: relative;
  cursor: pointer;
  display: block !important;
  box-sizing: border-box;
}

:deep(.practice-record-multiselect-container:hover) {
  border-color: #4dabf7;
}

:deep(.practice-record-multiselect-container.is-active) {
  border-color: #0052d9;
  box-shadow: 0 0 0 2px rgba(0, 82, 217, 0.1);
}

:deep(.practice-record-select-dropdown) {
  border: 1px solid #e6e6e6;
  border-radius: 6px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  z-index: 99999 !important;
  background: white;
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  max-height: 200px;
  overflow-y: auto;
}

:deep(.practice-record-select-dropdown.is-hidden) {
  display: none !important;
}

/* 确保 PracticeRecord 页面的 multiselect 基础样式正确 */
:deep(.practice-record-multiselect-container .multiselect) {
  min-height: 32px;
  height: 32px;
  width: 100% !important;
  min-width: 180px !important;
  position: relative;
  margin: 0;
  display: block !important;
  box-sizing: border-box;
  cursor: pointer;
  outline: none;
  border: none;
  background: transparent;
}

:deep(.practice-record-multiselect-container .multiselect-wrapper) {
  position: relative;
  margin: 0;
  width: 100% !important;
  min-width: 180px !important;
  display: block !important;
  box-sizing: border-box;
  cursor: pointer;
  outline: none;
}

:deep(.practice-record-multiselect-container .multiselect-single-label) {
  display: flex;
  align-items: center;
  height: 100%;
  position: absolute;
  left: 0;
  top: 0;
  pointer-events: none;
  background: transparent;
  line-height: 1.375;
  padding-left: 12px;
  padding-right: 40px;
  box-sizing: border-box;
  max-width: 100%;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

:deep(.practice-record-multiselect-container .multiselect-placeholder) {
  display: flex;
  align-items: center;
  height: 100%;
  position: absolute;
  left: 0;
  top: 0;
  pointer-events: none;
  background: transparent;
  line-height: 1.375;
  padding-left: 12px;
  color: #bbb;
  box-sizing: border-box;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

:deep(.practice-record-multiselect-container .multiselect-caret) {
  background-image: url("data:image/svg+xml,%3csvg viewBox='0 0 16 16' fill='%23999' xmlns='http://www.w3.org/2000/svg'%3e%3cpath d='m7.247 4.86-4.796 5.481c-.566.647-.106 1.659.753 1.659h9.592a1 1 0 0 0 .753-1.659l-4.796-5.48a1 1 0 0 0-1.506 0z'/%3e%3c/svg%3e");
  background-position: center;
  background-repeat: no-repeat;
  width: 10px;
  height: 16px;
  position: relative;
  z-index: 10;
  flex-shrink: 0;
  flex-grow: 0;
  pointer-events: none;
  margin-right: 12px;
  transition: transform 0.2s;
}

:deep(.practice-record-multiselect-container .multiselect-caret.is-open) {
  transform: rotate(180deg);
}

/* 确保下拉框默认隐藏 */
:deep(.practice-record-multiselect-container .multiselect-dropdown) {
  position: absolute;
  left: 0;
  right: 0;
  top: 100%;
  transform: translateY(0);
  border: 1px solid #d1d5db;
  margin-top: -1px;
  overflow-y: auto;
  z-index: 50;
  background-color: white;
  display: flex;
  flex-direction: column;
  border-radius: 0 0 6px 6px;
  max-height: 160px;
}

/* 调整清除按钮位置到右边 */
:deep(.practice-record-multiselect-container .multiselect-clear) {
  position: absolute !important;
  right: 32px !important; /* 给下拉箭头留出空间 */
  top: 50% !important;
  transform: translateY(-50%) !important;
  width: 16px !important;
  height: 16px !important;
  cursor: pointer !important;
  z-index: 10 !important;
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  background: transparent !important;
  border: none !important;
  color: #999 !important;
  font-size: 14px !important;
  opacity: 1 !important;
  visibility: visible !important;
}

:deep(.practice-record-multiselect-container .multiselect-clear:hover) {
  color: #666 !important;
}

/* 确保清除按钮内容显示 */
:deep(.practice-record-multiselect-container .multiselect-clear::before) {
  content: "×" !important;
  font-size: 16px !important;
  line-height: 1 !important;
  color: inherit !important;
}

/* 或者使用SVG图标 */
:deep(.practice-record-multiselect-container .multiselect-clear span) {
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  width: 100% !important;
  height: 100% !important;
  font-size: 14px !important;
}

/* 确保下拉箭头在最右边 */
:deep(.practice-record-multiselect-container .multiselect-caret) {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  background-image: url("data:image/svg+xml,%3csvg viewBox='0 0 16 16' fill='%23999' xmlns='http://www.w3.org/2000/svg'%3e%3cpath d='m7.247 4.86-4.796 5.481c-.566.647-.106 1.659.753 1.659h9.592a1 1 0 0 0 .753-1.659l-4.796-5.48a1 1 0 0 0-1.506 0z'/%3e%3c/svg%3e");
  background-position: center;
  background-repeat: no-repeat;
  width: 10px;
  height: 16px;
  z-index: 10;
  flex-shrink: 0;
  flex-grow: 0;
  pointer-events: none;
  transition: transform 0.2s;
  margin-right: 0;
}

:deep(.practice-record-multiselect-container .multiselect-dropdown.is-hidden) {
  display: none !important;
}

/* 确保下拉选项正确显示 */
:deep(.practice-record-multiselect-container .multiselect-options) {
  flex-direction: column;
  padding: 0;
  margin: 0;
  list-style: none;
  display: flex;
}

:deep(.practice-record-multiselect-container .multiselect-option) {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  box-sizing: border-box;
  text-align: left;
  cursor: pointer;
  line-height: 1.375;
  padding: 8px 12px;
  color: #374151;
  text-decoration: none;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

:deep(.practice-record-multiselect-container .multiselect-option.is-pointed) {
  background-color: #f3f4f6;
  color: #1f2937;
}

:deep(.practice-record-multiselect-container .multiselect-option.is-selected) {
  background-color: #0052d9;
  color: white;
}

/* 修复 PracticeRecord 页面可能的样式冲突 */
.practice-record-page :deep(.multiselect) {
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif !important;
  font-size: 14px !important;
  line-height: 1.5 !important;
}

/* 重要：确保单选标签文本正确显示 */
:deep(.practice-record-multiselect-container .multiselect-single-label-text) {
  overflow: hidden;
  display: block;
  white-space: nowrap;
  max-width: 100%;
  text-overflow: ellipsis;
}

/* 修复可能的样式冲突 */
:deep(.practice-record-multiselect-container .multiselect *) {
  box-sizing: border-box;
}

/* 修复表单项中的样式 */
.t-form-item :deep(.practice-record-multiselect-container .multiselect) {
  width: 100% !important;
}

/* 简单表格样式 - 响应式设计 */
.simple-table {
  border: 1px solid #e6e6e6;
  border-radius: 6px;
  overflow-x: auto;
  overflow-y: hidden;
  background: white;
  width: 100%;
  min-width: 1200px; /* 减少最小宽度，提高适应性 */
}

.table-header {
  display: grid;
  grid-template-columns: 80px minmax(200px, 1fr) 120px 80px 100px 100px 100px 120px 180px 100px 140px;
  background: #f5f7fa;
  border-bottom: 1px solid #e6e6e6;
}

.status-with-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  justify-content: center;
}

.header-cell {
  padding: 12px 8px;
  font-weight: 500;
  color: #303133;
  text-align: center;
  border-right: 1px solid #e6e6e6;
  white-space: nowrap;
}

.header-cell:last-child {
  border-right: none;
}

.table-row {
  display: grid;
  grid-template-columns: 80px minmax(200px, 1fr) 120px 80px 100px 100px 100px 120px 180px 100px 140px;
  border-bottom: 1px solid #e6e6e6;
  transition: background-color 0.2s;
}

.table-row:hover {
  background-color: #f5f7fa;
}

.table-row:last-child {
  border-bottom: none;
}

.table-cell {
  padding: 12px 8px;
  color: #606266;
  text-align: center;
  border-right: 1px solid #e6e6e6;
  display: flex;
  align-items: center;
  justify-content: center;
  word-break: break-all;
  overflow: hidden;
  min-width: 0;
}
/* 详情对话框样式 */
.record-detail {
  padding: 16px 0;
}

.detail-info-card {
  margin-bottom: 16px;
}

.detail-questions-card {
  margin-bottom: 16px;
}

.loading-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 200px;
}

/* 详情表格样式 */
.detail-table-container {
  border: 1px solid #e6e6e6;
  border-radius: 6px;
  overflow: hidden;
  background: white;
  max-height: 400px;
  overflow-y: auto;
}

.detail-table-header {
  display: grid;
  grid-template-columns: 60px 2fr 100px 80px 120px 120px 80px 100px 180px;
  background: #f5f7fa;
  border-bottom: 1px solid #e6e6e6;
  position: sticky;
  top: 0;
  z-index: 1;
}

.detail-header-cell {
  padding: 12px 8px;
  font-weight: 500;
  color: #303133;
  text-align: center;
  border-right: 1px solid #e6e6e6;
  font-size: 14px;
}

.detail-header-cell:last-child {
  border-right: none;
}

.detail-table-body {
  max-height: 350px;
  overflow-y: auto;
}

.detail-table-row {
  display: grid;
  grid-template-columns: 60px 2fr 100px 80px 120px 120px 80px 100px 180px;
  border-bottom: 1px solid #e6e6e6;
  transition: background-color 0.2s;
}

.detail-table-row:hover {
  background-color: #f5f7fa;
}

.detail-table-row:last-child {
  border-bottom: none;
}

.detail-table-cell {
  padding: 12px 8px;
  color: #606266;
  text-align: center;
  border-right: 1px solid #e6e6e6;
  font-size: 14px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.detail-table-cell:nth-child(2) {
  text-align: left;
  white-space: normal;
  word-break: break-word;
  line-height: 1.4;
}

/* 自定义模态框样式 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 10000;
}

.modal-container {
  background: white;
  border-radius: 8px;
  width: 90%;
  max-width: 1200px;
  max-height: 90vh;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  border-bottom: 1px solid #e6e6e6;
  background: #f5f7fa;
}

.modal-header h3 {
  margin: 0;
  color: #303133;
  font-size: 18px;
  font-weight: 500;
}

.modal-body {
  padding: 24px;
  max-height: calc(90vh - 80px);
  overflow-y: auto;
}
.detail-table-cell:last-child {
  border-right: none;
}
.empty-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 200px;
}

.table-cell:last-child {
  border-right: none;
}

.action-cell .t-button {
  min-width: 88px;
}


/* 调试信息样式 */
.debug-info {
  background: #fff3cd;
  border: 1px solid #ffeaa7;
  border-radius: 6px;
  padding: 16px;
  margin: 16px 0;
  color: #856404;
}

.debug-info p {
  margin: 4px 0;
}

/* 空状态样式 */
.empty-state {
  padding: 40px;
  text-align: center;
}

/* 响应式表格 */
@media (max-width: 768px) {
  .table-header,
  .table-row {
    grid-template-columns: 1fr;
    gap: 0;
  }

  .header-cell,
  .table-cell {
    border-right: none;
    border-bottom: 1px solid #e6e6e6;
    text-align: left;
    padding: 8px 12px;
  }

  .header-cell:before,
  .table-cell:before {
    content: attr(data-label);
    font-weight: bold;
    margin-right: 8px;
    min-width: 80px;
    display: inline-block;
  }
}

/* 修复分页组件下拉框问题 */
.pagination-wrapper :deep(.t-pagination) {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.pagination-wrapper :deep(.t-select) {
  position: relative !important;
  z-index: 1 !important;
}

.pagination-wrapper :deep(.t-select-dropdown) {
  z-index: 99999 !important;
  position: absolute !important;
  background: white !important;
  border: 1px solid #d9d9d9 !important;
  border-radius: 6px !important;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1) !important;
  max-height: 200px !important;
  overflow-y: auto !important;
}

.pagination-wrapper :deep(.t-select-option) {
  padding: 8px 12px !important;
  cursor: pointer !important;
  transition: background-color 0.2s !important;
  white-space: nowrap !important;
}

.pagination-wrapper :deep(.t-select-option:hover) {
  background-color: #f3f3f3 !important;
}

.pagination-wrapper :deep(.t-select-option.is-selected) {
  background-color: #0052d9 !important;
  color: white !important;
}

/* 确保分页组件正确显示 */
.pagination-wrapper {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #e6e6e6;
  width: 100%;
}

/* 响应式分页 */
@media (max-width: 768px) {
  .pagination-wrapper {
    margin-top: 15px;
    padding-top: 15px;
  }

  .pagination-wrapper :deep(.t-pagination) {
    flex-wrap: wrap;
    justify-content: center;
    gap: 4px;
  }

  .pagination-wrapper :deep(.t-pagination__total) {
    order: -1;
    width: 100%;
    text-align: center;
    margin-bottom: 8px;
  }

  .pagination-wrapper :deep(.t-pagination__jump) {
    order: 1;
    width: 100%;
    justify-content: center;
    margin-top: 8px;
  }
}

/* 响应式统计卡片 */
@media (max-width: 768px) {
  .stats-cards :deep(.t-row) {
    flex-direction: column;
    gap: 16px;
  }
  
  .stats-cards :deep(.t-col) {
    width: 100% !important;
    flex: none !important;
  }
}

@media (min-width: 769px) and (max-width: 1024px) {
  .stats-cards :deep(.t-col) {
    width: 50% !important;
    flex: 0 0 50% !important;
  }
}

/* 响应式表格 - 移动端卡片式布局 */
@media (max-width: 768px) {
  .simple-table {
    min-width: auto;
    border: none;
    background: transparent;
  }
  
  .table-header {
    display: none;
  }
  
  .table-row {
    display: block;
    background: white;
    border: 1px solid #e6e6e6;
    border-radius: 8px;
    margin-bottom: 16px;
    padding: 16px;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  }
  
  .table-cell {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 8px 0;
    border-right: none;
    border-bottom: 1px solid #f0f0f0;
    text-align: left;
  }
  
  .table-cell:last-child {
    border-bottom: none;
    justify-content: flex-start;
  }
  
  .table-cell:before {
    content: attr(data-label);
    font-weight: 500;
    color: #303133;
    min-width: 80px;
    flex-shrink: 0;
  }
  
  .action-cell {
    justify-content: flex-start;
    margin-top: 8px;
  }
}

/* 页面整体响应式 */
@media (max-width: 480px) {
  .practice-record-page {
    padding: 16px;
  }
  
  .header-content h2 {
    font-size: 20px;
  }
  
  .stat-value {
    font-size: 24px;
  }
}
</style>

<style src="@vueform/multiselect/themes/default.css"></style>