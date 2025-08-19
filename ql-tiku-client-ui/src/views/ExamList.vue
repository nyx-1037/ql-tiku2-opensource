<template>
  <div class="exam-list-page">
    <t-card class="exam-list-card">
        <div class="card-header">
          <h2>我的考试记录</h2>
          <p>查看您的所有考试记录和成绩</p>
        </div>
        
        <!-- 搜索和筛选 -->
        <div class="search-section">
          <t-form :model="searchForm" inline class="search-form">
            <t-form-item label="关键词">
              <t-input
                v-model="searchForm.keyword"
                placeholder="请输入考试名称"
                clearable
                style="width: 200px"
                @keyup.enter="handle"
              />
            </t-form-item>
            <!-- 修复下拉框的显示控制 -->
            <t-form-item label="科目">
              <Multiselect
                v-model="searchForm.subjectId"
                :options="subjects"
                value-prop="id"
                label="name"
                placeholder="请选择科目"
                :can-clear="true"
                :searchable="true"
                :close-on-select="true"
                :classes="{
                  container: 'exam-multiselect-container',
                  dropdown: 'exam-select-dropdown'
                }"
                style="width: 150px"
              />
            </t-form-item>
            <t-form-item label="状态">
              <Multiselect
                v-model="searchForm.status"
                :options="[
                  { value: 1, label: '已通过' },
                  { value: 0, label: '未通过' }
                ]"
                value-prop="value"
                label="label"
                placeholder="请选择状态"
                :can-clear="true"
                :searchable="false"
                :close-on-select="true"
                :classes="{
                  container: 'exam-multiselect-container',
                  dropdown: 'exam-select-dropdown'
                }"
                style="width: 150px"
              />
            </t-form-item>
            <t-form-item>
              <t-button theme="primary" @click="handle" :loading="loading">
                <span class="icon">🔍</span>
                搜索
              </t-button>
              <t-button theme="default" @click="handleReset">
                <span class="icon">🔄</span>
                重置
              </t-button>
            </t-form-item>
          </t-form>
        </div>
        
        <!-- 考试记录列表 -->
        <div class="exam-records">
          <!-- 自定义表格 -->
          <div v-if="examRecords.length > 0" class="simple-table">
            <div class="table-header">
              <div class="header-cell">考试名称</div>
              <div class="header-cell">得分</div>
              <div class="header-cell">正确率</div>
              <div class="header-cell">答题情况</div>
              <div class="header-cell">状态</div>
              <div class="header-cell">提交时间</div>
              <div class="header-cell">操作</div>
            </div>
            <!-- 修改表格结构，确保历史记录只在对应行显示 -->
            <div v-for="(record, recordIndex) in examRecords" :key="record._rowKey" class="record-container">
              <!-- 主记录行 -->
              <div
                class="table-row"
                :class="{ 'expanded': expandedRecords.has(record._rowKey) }"
              >
                <div class="table-cell exam-info-cell">
                  <div class="exam-name">{{ record.examTitle }}</div>
                  <div class="tags">
                    <t-tag size="small" class="subject-tag">{{ record.subjectName }}</t-tag>
                    <t-tag 
                      size="small" 
                      class="exam-type-tag"
                      :class="getExamTypeTagClass(record)"
                    >
                      {{ getExamTypeText(record) }}
                    </t-tag>
                  </div>
                </div>
                <div class="table-cell">
                  <span class="score" :class="{ 'passed': record.passed }">
                    {{ record.totalScore }}
                  </span>
                </div>
                <div class="table-cell">
                  <span class="accuracy">{{ record.accuracy ? record.accuracy.toFixed(1) : 0 }}%</span>
                </div>
                <div class="table-cell">
                  <span>{{ record.correctCount }}/{{ record.totalCount }}</span>
                </div>
                <div class="table-cell">
                  <t-tag :theme="record.passed ? 'success' : 'danger'" size="small">
                    {{ record.passed ? '已通过' : '未通过' }}
                  </t-tag>
                </div>
                <div class="table-cell">
                  <span>{{ formatDateTime(record.submitTime) }}</span>
                </div>
                <div class="table-cell">
                  <t-button
                    v-if="record.examType === 'simulation'"
                    theme="primary"
                    size="small"
                    @click="viewExamDetail(record)"
                    style="margin-right: 8px;"
                  >
                    查看详情
                  </t-button>
                  <t-button
                    v-if="record.examType !== 'simulation'"
                    theme="default"
                    size="small"
                    @click="toggleRecordExpansion(record)"
                  >
                    {{ expandedRecords.has(record._rowKey) ? '收起' : '展开历史' }}
                  </t-button>
                </div>
              </div>
              
              <!-- 历史记录展开区域 - 只在当前记录展开时显示 -->
              <div 
                v-if="expandedRecords.has(record._rowKey)" 
                class="history-records-row"
              >
                <div class="history-records-container">
                  <div class="history-records-header">
                    <h4>{{ record.examTitle }} - 历史考试记录</h4>
                  </div>
                  <div v-if="recordHistoryLoading.has(record._examKey)" class="loading-records">
                    <t-loading text="加载中..."></t-loading>
                  </div>
                  <div v-else-if="recordHistoryData.has(record._examKey) && recordHistoryData.get(record._examKey).length > 0" class="history-records-list">
                    <div
                      v-for="(historyRecord, index) in recordHistoryData.get(record._examKey)"
                      :key="`history-${record._examKey}-${historyRecord.negativeRecordId || historyRecord.id || index}`"
                      class="history-record-item"
                    >
                      <div class="record-info">
                        <div class="record-title">
                          第{{ index + 1 }}次考试
                          <t-tag :theme="historyRecord.passed ? 'success' : 'danger'" size="small">
                            {{ historyRecord.passed ? '已通过' : '未通过' }}
                          </t-tag>
                        </div>
                        <div class="record-details">
                          <div class="detail-item">
                            <span class="label">得分：</span>
                            <span class="value">{{ historyRecord.formattedScore }}</span>
                          </div>
                          <div class="detail-item">
                            <span class="label">正确率：</span>
                            <span class="value">{{ historyRecord.formattedAccuracy }}</span>
                          </div>
                          <div class="detail-item">
                            <span class="label">答题情况：</span>
                            <span class="value">{{ historyRecord.correctCount }}/{{ historyRecord.totalCount }}</span>
                          </div>
                          <div class="detail-item">
                            <span class="label">提交时间：</span>
                            <span class="value">{{ historyRecord.formattedTime }}</span>
                          </div>
                        </div>
                      </div>
                      <div class="record-actions">
                        <t-button
                          theme="primary"
                          size="small"
                          @click="viewHistoryRecordDetail(historyRecord)"
                        >
                          查看详情
                        </t-button>
                      </div>
                    </div>
                  </div>
                  <div v-else class="empty-records">
                    <t-empty description="暂无历史考试记录" />
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 如果没有数据显示空状态 -->
          <div v-if="examRecords.length === 0 && !loading" class="empty-state">
            <t-empty description="暂无考试记录" />
          </div>
        </div>
        
        <!-- 分页 -->
        <div class="pagination-section">
          <CustomPagination
            :current="pagination.current"
            :page-size="pagination.size"
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
  </div>
</template>

<script>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { MessagePlugin } from 'tdesign-vue-next'
import { examAPI, subjectAPI } from '../api'
import Multiselect from '@vueform/multiselect'
import CustomPagination from '../components/CustomPagination.vue'

export default {
  name: 'ExamList',
  components: {
    Multiselect,
    CustomPagination
  },
  setup() {
    const router = useRouter()
    
    const loading = ref(false)
    const subjects = ref([])
    const examRecords = ref([])
    
    // 展开状态管理
    const expandedRecords = ref(new Set())
    const recordHistoryData = ref(new Map())
    const recordHistoryLoading = ref(new Set())
    
    const searchForm = reactive({
      keyword: '',
      subjectId: null,
      status: null
    })
    
    const pagination = reactive({
      current: 1,
      size: 10,
      total: 0
    })
    
    // 获取科目列表
    const getSubjects = async () => {
      try {
        const response = await subjectAPI.getEnabledSubjects()
        if (Array.isArray(response)) {
          subjects.value = response
        } else if (response.code === 200 && Array.isArray(response.data)) {
          subjects.value = response.data
        } else {
          console.error('获取科目数据格式错误:', response)
          subjects.value = []
        }
      } catch (error) {
        console.error('获取科目列表失败:', error)
        subjects.value = []
      }
    }
    
    // 获取考试记录
    const getExamRecords = async () => {
      try {
        loading.value = true
        const params = {
          current: pagination.current,
          size: pagination.size,
          keyword: searchForm.keyword || undefined,
          subjectId: searchForm.subjectId || undefined,
          status: searchForm.status !== null ? searchForm.status : undefined
        }
        
        const result = await examAPI.getUserExamRecords(params)
        console.log('📋 ExamList.vue: API响应数据', result)
        
        // 适配不同的API返回格式
        if (result && Array.isArray(result.records)) {
          examRecords.value = result.records
          pagination.total = result.total || 0
        } else if (result && Array.isArray(result.content)) {
          examRecords.value = result.content
          pagination.total = result.totalElements || result.total || 0
        } else if (result && Array.isArray(result.data)) {
          examRecords.value = result.data
          pagination.total = result.total || 0
        } else if (result && Array.isArray(result)) {
          examRecords.value = result
          pagination.total = result.length
        } else {
          console.warn('⚠️ ExamList.vue: 未识别的API返回格式', result)
          examRecords.value = []
          pagination.total = 0
        }
        
        // 生成每行唯一key与考试key，避免展开状态串行
        examRecords.value = (examRecords.value || []).map((r, i) => ({
          ...r,
          _examKey: r.examId ?? r.id,
          _rowKey: `${(r.examId ?? r.id)}-${r.submitTime ?? r.createTime ?? r.recordTime ?? ''}-${i}`
        }))
        console.log('📋 ExamList.vue: 考试记录数据', examRecords.value, '总数:', pagination.total)
      } catch (error) {
        console.error('获取考试记录失败:', error)
        MessagePlugin.error('获取考试记录失败')
        examRecords.value = []
        pagination.total = 0
      } finally {
        loading.value = false
      }
    }
    
    // 切换记录展开状态
    const toggleRecordExpansion = async (record) => {
      const rowKey = record._rowKey || `${(record.examId || record.id)}-${record.submitTime || record.createTime || ''}`
      const examKey = record._examKey || record.examId || record.id
      
      if (expandedRecords.value.has(rowKey)) {
        // 收起当前项
        expandedRecords.value.delete(rowKey)
      } else {
        // 展开当前项（允许多个同时展开，避免清空所有状态）
        expandedRecords.value.add(rowKey)
        
        // 如果还没有加载历史记录，则加载
        if (!recordHistoryData.value.has(examKey)) {
          recordHistoryLoading.value.add(examKey)
          
          try {
            const examId = examKey
            const records = await examAPI.getExamRecordBatches(examId)
            console.log('📋 获取到的历史考试记录:', records)
            
            if (records && records.length > 0) {
              // 去重：基于recordId或id
              const uniqueRecords = [];
              const seenIds = new Set();
              
              records.forEach(record => {
                const recordId = record.recordId || record.id || record.negativeRecordId;
                if (recordId && !seenIds.has(recordId)) {
                  seenIds.add(recordId);
                  uniqueRecords.push(record);
                } else if (!recordId) {
                  // 如果没有ID，使用内容哈希作为临时标识
                  const contentHash = JSON.stringify({
                    score: record.totalScore,
                    time: record.createTime || record.recordTime,
                    count: record.correctCount
                  });
                  if (!seenIds.has(contentHash)) {
                    seenIds.add(contentHash);
                    uniqueRecords.push(record);
                  }
                }
              });
              
              // 按时间倒序排序（最新的在前面）
              uniqueRecords.sort((a, b) => {
                const timeA = new Date(a.createTime || a.recordTime || a.submitTime).getTime();
                const timeB = new Date(b.createTime || b.recordTime || b.submitTime).getTime();
                return timeB - timeA;
              });
              
              // 限制显示最近10条记录，避免过多数据
              const limitedRecords = uniqueRecords.slice(0, 10);
              
              // 格式化记录数据
              limitedRecords.forEach((record, index) => {
                record.index = index + 1
                record.formattedTime = formatDateTime(record.createTime || record.recordTime || record.submitTime)
                record.formattedScore = record.totalScore != null ? `${record.totalScore}/${record.maxScore || 100}` : '-'
                record.formattedAccuracy = record.accuracy != null ? `${record.accuracy.toFixed(1)}%` : '-'
              })
              
              recordHistoryData.value.set(examKey, limitedRecords)
            } else {
              recordHistoryData.value.set(examKey, [])
            }
          } catch (error) {
            console.error('获取历史考试记录失败:', error)
            MessagePlugin.error('获取历史考试记录失败')
            recordHistoryData.value.set(examKey, [])
          } finally {
            recordHistoryLoading.value.delete(examKey)
          }
        }
      }
    }
    
    // 搜索
    const handle = () => {
      pagination.current = 1
      getExamRecords()
    }
    
    // 重置
    const handleReset = () => {
      searchForm.keyword = ''
      searchForm.subjectId = null
      searchForm.status = null
      pagination.current = 1
      getExamRecords()
    }
    
    // 分页大小改变
    const handleSizeChange = (size) => {
      pagination.size = size
      pagination.current = 1
      getExamRecords()
    }
    
    // 当前页改变
    const handleCurrentChange = (current) => {
      pagination.current = current
      getExamRecords()
    }
    
    // 查看考试详情
    const viewExamDetail = (row) => {
      console.log('查看考试详情:', row)
      
      if (row.examType === 'simulation') {
        // 模拟考试详情
        let recordId = row.recordId || row.examId
        if (recordId && recordId.startsWith('simulation_')) {
          recordId = recordId.substring('simulation_'.length)
        }
        router.push({
          name: 'ExamDetail',
          params: { examId: 'simulation' },
          query: { recordId: recordId }
        })
      } else {
        // 正式考试详情 - 默认查看最新记录
        const examId = row.examId || row.id
        if (examId) {
          router.push({
            name: 'ExamDetail',
            params: { examId: examId }
          })
        } else {
          console.error('缺少examId参数:', row)
          MessagePlugin.error('无法获取考试ID')
        }
      }
    }
    
    // 查看历史记录详情
    const viewHistoryRecordDetail = (record) => {
      console.log('📋 查看历史记录详情:', record)
      if (record && record.negativeRecordId) {
        // 直接使用negativeRecordId作为recordId参数（可以是负数）
        const recordId = record.negativeRecordId
        router.push({
          name: 'ExamDetail',
          params: { examId: 'history' },
          query: { recordId: recordId }
        })
      } else {
        MessagePlugin.error('记录ID无效')
      }
    }
    
    // 格式化日期时间
    const formatDateTime = (dateTime) => {
      if (!dateTime) return '-'
      const date = new Date(dateTime)
      return date.toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
      })
    }
    
    // 获取考试类型文本
    const getExamTypeText = (record) => {
      if (record.examType === 'simulation') {
        return '自定义试卷'
      } else if (record.examType === 'formal') {
        if (record.examTypeText) {
          return record.examTypeText
        }
        return '固定试卷'
      }
      return '未知类型'
    }
    
    // 获取考试类型标签样式类
    const getExamTypeTagClass = (record) => {
      if (record.examType === 'simulation') {
        return 'custom-exam-tag'
      } else if (record.examType === 'formal') {
        if (record.examTypeText === '模拟试卷') {
          return 'simulation-paper-tag'
        } else if (record.examTypeText === '真题试卷') {
          return 'real-paper-tag'
        }
        return 'formal-exam-tag'
      }
      return 'unknown-exam-tag'
    }
    
    onMounted(async () => {
      console.log('🎯 ExamList.vue: 组件已挂载')

      await getSubjects()
      await nextTick()
      await getExamRecords()
    })
    
    return {
      loading,
      subjects,
      examRecords,
      expandedRecords,
      recordHistoryData,
      recordHistoryLoading,
      searchForm,
      pagination,
      handle,
      handleReset,
      handleSizeChange,
      handleCurrentChange,
      viewExamDetail,
      viewHistoryRecordDetail,
      toggleRecordExpansion,
      formatDateTime,
      getExamTypeText,
      getExamTypeTagClass
    }
  }
}
</script>

<!-- 修复下拉框样式问题 -->
<style scoped>
/* Exam Multiselect 自定义样式 - 参考 Exam.vue */
:deep(.exam-multiselect-container) {
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  min-height: 32px;
  background: white;
  transition: all 0.2s;
  position: relative;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  box-sizing: border-box;
}

:deep(.exam-multiselect-container:hover) {
  border-color: #4dabf7;
}

:deep(.exam-multiselect-container.is-active) {
  border-color: #0052d9;
  box-shadow: 0 0 0 2px rgba(0, 82, 217, 0.1);
}

:deep(.exam-select-dropdown) {
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

:deep(.exam-select-dropdown.is-hidden) {
  display: none !important;
}

/* 确保 ExamList 页面的 multiselect 基础样式正确 */
:deep(.exam-multiselect-container .multiselect) {
  min-height: 32px;
  height: 32px;
  width: 100%;
}

:deep(.exam-multiselect-container .multiselect-single-label) {
  padding-left: 12px;
  padding-right: 40px;
  line-height: 30px;
}

:deep(.exam-multiselect-container .multiselect-placeholder) {
  padding-left: 12px;
  line-height: 30px;
  color: #bbb;
}

:deep(.exam-multiselect-container .multiselect-caret) {
  margin-right: 12px;
}

/* 修复 ExamList 页面可能的样式冲突 */
.exam-list-page :deep(.multiselect) {
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif !important;
  font-size: 14px !important;
  line-height: 1.5 !important;
}

:deep(.exam-list-select-dropdown .multiselect-options) {
  padding: 4px 0;
  max-height: 200px;
  overflow-y: auto;
}

:deep(.exam-list-select-dropdown .multiselect-option) {
  padding: 8px 12px;
  font-size: 14px;
  cursor: pointer;
  color: var(--td-text-color-primary);
  line-height: 1.5;
  display: flex;
  align-items: center;
  min-height: 32px;
}

:deep(.exam-list-select-dropdown .multiselect-option:hover) {
  background: var(--td-bg-color-container-hover);
}

:deep(.exam-list-select-dropdown .multiselect-option.is-selected) {
  background: var(--td-brand-color-light);
  color: var(--td-brand-color);
}

:deep(.exam-list-select-dropdown .multiselect-option-label) {
  display: block;
  color: var(--td-text-color-primary);
  font-size: 14px;
  line-height: 1.5;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 修复搜索栏中的下拉框定位 */
.search-section :deep(.t-form-item) {
  position: relative;
}

.search-section :deep(.exam-list-multiselect-container) {
  position: relative;
  z-index: 100;
}

.search-section :deep(.multiselect-dropdown) {
  position: absolute !important;
  top: 100% !important;
  left: 0 !important;
  right: 0 !important;
  z-index: 1000 !important;
  width: 100% !important;
  max-width: none !important;
  display: block !important;
}

.search-section :deep(.multiselect-options) {
  max-height: 200px;
  overflow-y: auto;
}

/* 确保下拉选项可见 */
:deep(.multiselect-options) {
  padding: 4px 0;
  max-height: 200px;
  overflow-y: auto;
}

:deep(.multiselect-option) {
  padding: 8px 12px;
  font-size: 14px;
  cursor: pointer;
  color: var(--td-text-color-primary);
  line-height: 1.5;
  display: flex;
  align-items: center;
  min-height: 32px;
}

:deep(.multiselect-option:hover) {
  background: var(--td-bg-color-container-hover);
}

:deep(.multiselect-option.is-selected) {
  background: var(--td-brand-color-light);
  color: var(--td-brand-color);
}

/* 确保下拉框在搜索区域正确显示 */
.search-section {
  position: relative;
  z-index: 1;
}

.search-section :deep(.t-form-item) {
  position: relative;
  z-index: 10;
}

/* 修复下拉框层级问题 */
:deep(.multiselect-dropdown) {
  z-index: 1001 !important;
}

/* 确保Multiselect组件的下拉箭头和清除按钮正常显示 */
:deep(.multiselect-caret) {
  border-color: var(--td-text-color-secondary) transparent transparent;
  border-style: solid;
  border-width: 5px 5px 0;
  margin-top: -2px;
  right: 12px;
  top: 50%;
  position: absolute;
  transform: translateY(-50%);
  transition: transform 0.3s;
}

:deep(.multiselect-caret.is-open) {
  transform: translateY(-50%) rotate(180deg);
}

:deep(.multiselect-clear) {
  position: absolute;
  right: 32px;
  top: 50%;
  transform: translateY(-50%);
  cursor: pointer;
  color: var(--td-text-color-secondary);
  font-size: 16px;
  line-height: 1;
  z-index: 10;
}

:deep(.multiselect-clear:hover) {
  color: var(--td-text-color-primary);
}

:deep(.exam-list-select-dropdown .multiselect-option) {
  padding: 8px 12px;
  font-size: 14px;
  cursor: pointer;
  color: var(--td-text-color-primary);
  line-height: 1.5;
  display: flex;
  align-items: center;
  min-height: 32px;
}

:deep(.exam-list-select-dropdown .multiselect-option:hover) {
  background: var(--td-bg-color-container-hover);
}

:deep(.exam-list-select-dropdown .multiselect-option.is-selected) {
  background: var(--td-brand-color-light);
  color: var(--td-brand-color);
}

:deep(.exam-list-select-dropdown .multiselect-option-label) {
  display: block;
  color: var(--td-text-color-primary);
  font-size: 14px;
  line-height: 1.5;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 记录容器样式 */
.record-container {
  border-bottom: 1px solid var(--td-border-level-1-color);
}

.record-container:last-child {
  border-bottom: none;
}

/* 修复表格行样式 */
.table-row {
  display: grid;
  grid-template-columns: 2fr 1fr 1fr 1fr 1fr 1.5fr 1.5fr;
  transition: all 0.3s ease;
  position: relative;
}

.table-row:hover {
  background: var(--td-bg-color-container-hover);
}

.table-row.expanded {
  background: var(--td-bg-color-secondarycomponent);
}

/* 历史记录行样式 */
.history-records-row {
  display: block;
  width: 100%;
  background: var(--td-bg-color-secondarycomponent);
  border-top: 1px solid var(--td-border-level-2-color);
  animation: slideDown 0.3s ease-out;
}

@keyframes slideDown {
  from {
    opacity: 0;
    max-height: 0;
    overflow: hidden;
  }
  to {
    opacity: 1;
    max-height: 500px;
    overflow: visible;
  }
}

/* 其他样式保持不变 */
.exam-records {
  min-height: 200px;
}

.simple-table {
  width: 100%;
  border: 1px solid var(--td-border-level-1-color);
  border-radius: 8px;
  overflow: hidden;
}

.table-header {
  display: grid;
  grid-template-columns: 2fr 1fr 1fr 1fr 1fr 1.5fr 1.5fr;
  background: var(--td-bg-color-container-hover);
  border-bottom: 1px solid var(--td-border-level-1-color);
}

.header-cell {
  padding: 12px 16px;
  font-weight: 600;
  font-size: 14px;
  color: var(--td-text-color-primary);
  border-right: 1px solid var(--td-border-level-1-color);
}

.header-cell:last-child {
  border-right: none;
}

.table-cell {
  padding: 16px;
  border-right: 1px solid var(--td-border-level-1-color);
  display: flex;
  align-items: center;
  font-size: 14px;
}

.table-cell:last-child {
  border-right: none;
}

.exam-info-cell {
  flex-direction: column;
  align-items: flex-start;
}

.exam-name {
  font-weight: 600;
  margin-bottom: 8px;
  color: var(--td-text-color-primary);
}

.tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.subject-tag {
  background: var(--td-brand-color-light);
  color: var(--td-brand-color);
  border: none;
}

.exam-type-tag {
  border: none;
}

.score {
  font-weight: 600;
  color: var(--td-error-color);
}

.score.passed {
  color: var(--td-success-color);
}

.accuracy {
  font-weight: 600;
  color: var(--td-brand-color);
}

.history-records-container {
  padding: 20px;
}

.history-records-header {
  margin-bottom: 16px;
}

.history-records-header h4 {
  margin: 0 0 8px 0;
  color: var(--td-text-color-primary);
  font-size: 16px;
  font-weight: 600;
}

.history-records-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.history-record-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  background: var(--td-bg-color-container);
  border: 1px solid var(--td-border-level-1-color);
  border-radius: 8px;
  transition: all 0.3s ease;
}

.history-record-item:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.record-info {
  flex: 1;
}

.record-title {
  font-weight: 600;
  margin-bottom: 8px;
  color: var(--td-text-color-primary);
  display: flex;
  align-items: center;
  gap: 8px;
}

.record-details {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}

.detail-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.detail-item .label {
  color: var(--td-text-color-secondary);
  font-size: 13px;
}

.detail-item .value {
  color: var(--td-text-color-primary);
  font-weight: 500;
  font-size: 13px;
}

.record-actions {
  display: flex;
  gap: 8px;
}

.loading-records {
  padding: 40px;
  text-align: center;
}

.empty-records {
  padding: 40px;
  text-align: center;
}

.empty-state {
  padding: 60px 0;
  text-align: center;
}

.pagination-section {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.card-header {
  margin-bottom: 24px;
}

.card-header h2 {
  margin: 0 0 8px 0;
  color: var(--td-text-color-primary);
  font-size: 24px;
  font-weight: 600;
}

.card-header p {
  margin: 0;
  color: var(--td-text-color-secondary);
  font-size: 14px;
}

.search-section {
  margin-bottom: 24px;
  padding: 20px;
  background: var(--td-bg-color-container);
  border-radius: 8px;
  border: 1px solid var(--td-border-level-1-color);
}

.search-form {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  align-items: flex-end;
}

.search-form .t-form__item {
  margin-bottom: 0;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .search-form {
    flex-direction: column;
    align-items: stretch;
  }
  
  .search-form .t-form__item {
    width: 100%;
  }
  
  .search-form .t-input,
  .search-form .multiselect {
    width: 100% !important;
  }
  
  .table-header,
  .table-row {
    grid-template-columns: 1fr;
    gap: 8px;
  }
  
  .table-cell {
    padding: 8px;
    border-right: none;
    border-bottom: 1px solid var(--td-border-level-1-color);
  }
  
  .history-record-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
  
  .record-actions {
    align-self: flex-end;
  }
}

@media (max-width: 1200px) {
  .table-header,
  .table-row {
    grid-template-columns: 1.5fr 0.8fr 0.8fr 0.8fr 0.8fr 1.2fr 1.2fr;
    font-size: 13px;
  }
  
  .table-cell {
    padding: 12px 8px;
  }
}
</style>

<!-- 移除默认样式引入，避免冲突 -->
<style src="@vueform/multiselect/themes/default.css"></style>
