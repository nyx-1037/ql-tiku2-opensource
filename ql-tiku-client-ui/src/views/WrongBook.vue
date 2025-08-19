<template>
  <div class="wrong-book-page">
    <div class="wrong-book-header">
        <h2>我的错题本</h2>
        <p>重点复习错题，提高学习效率</p>
    </div>
      
    <!-- 筛选条件 -->
      <t-card class="filter-card">
        <div class="filter-form">
          <div class="filter-item">
            <label class="filter-label">科目</label>
            <Multiselect
              v-model="filters.subjectId"
              :options="subjects"
              value-prop="id"
              label="name"
              placeholder="选择科目"
              :can-clear="true"
              @change="getWrongQuestions"
              :classes="{
                container: 'wrong-book-multiselect-container',
                dropdown: 'wrong-book-select-dropdown'
              }"
              style="width: 180px;"
            />
          </div>
          <div class="filter-item">
            <label class="filter-label">题目类型</label>
            <Multiselect
              v-model="filters.type"
              :options="[
                { value: 'SINGLE_CHOICE', label: '单选题' },
                { value: 'MULTIPLE_CHOICE', label: '多选题' },
                { value: 'TRUE_FALSE', label: '判断题' }
              ]"
              value-prop="value"
              label="label"
              placeholder="题目类型"
              :can-clear="true"
              @change="getWrongQuestions"
              :classes="{
                container: 'wrong-book-multiselect-container',
                dropdown: 'wrong-book-select-dropdown'
              }"
              style="width: 180px;"
            />
          </div>
          <div class="filter-item">
            <label class="filter-label">难度等级</label>
            <Multiselect
              v-model="filters.difficulty"
              :options="[
                { value: 'EASY', label: '简单' },
                { value: 'MEDIUM', label: '中等' },
                { value: 'HARD', label: '困难' }
              ]"
              value-prop="value"
              label="label"
              placeholder="难度等级"
              :can-clear="true"
              @change="getWrongQuestions"
              :classes="{
                container: 'wrong-book-multiselect-container',
                dropdown: 'wrong-book-select-dropdown'
              }"
              style="width: 180px;"
            />
          </div>
          <div class="filter-item">
            <label class="filter-label">错题类型</label>
            <Multiselect
              v-model="filters.wrongType"
              :options="[
                { value: 'PRACTICE', label: '练习题目' },
                { value: 'EXAM_CUSTOM', label: '自定义试卷' },
                { value: 'EXAM_FIXED', label: '固定试卷' }
              ]"
              value-prop="value"
              label="label"
              placeholder="错题类型"
              :can-clear="true"
              @change="getWrongQuestions"
              :classes="{
                container: 'wrong-book-multiselect-container',
                dropdown: 'wrong-book-select-dropdown'
              }"
              style="width: 180px;"
            />
          </div>
          <div class="filter-buttons">
            <t-button theme="primary" @click="startWrongQuestionPractice" :disabled="wrongQuestions.length === 0">
              开始练习
            </t-button>
            <t-button @click="getWrongQuestions">
              查询
            </t-button>
            <t-button @click="resetFilters">
              重置
            </t-button>
          </div>
        </div>
      </t-card>
      
      <!-- 统计信息 -->
      <div class="stats-container">
        <div class="stat-card-wrapper">
          <t-card class="stat-card">
            <t-statistic title="错题总数" :value="totalWrongCount" suffix="题" />
          </t-card>
        </div>
        <div class="stat-card-wrapper">
          <t-card class="stat-card">
            <t-statistic title="已复习" :value="reviewedCount" suffix="题" />
          </t-card>
        </div>
        <div class="stat-card-wrapper">
          <t-card class="stat-card">
            <t-statistic title="复习率" :value="reviewRate" suffix="%" :precision="1" />
          </t-card>
        </div>
      </div>
      
      <!-- 错题列表 -->
      <t-card class="question-list-card">
        <template #header>
          <div class="card-header">
            <span>错题列表</span>
            <t-button theme="danger" size="small" @click="clearAllWrongQuestions" :disabled="wrongQuestions.length === 0">
              清空错题本
            </t-button>
          </div>
        </template>
        
        <div v-if="loading" class="loading-container">
          <t-skeleton :rows="5" animation />
        </div>
        
        <div v-else-if="wrongQuestions.length === 0" class="empty-container">
          <t-empty description="暂无错题">
            <t-button theme="primary" @click="$router.push('/practice')">去刷题</t-button>
          </t-empty>
        </div>
        
        <div v-else class="question-list">
          <div
            v-for="(question, index) in wrongQuestions"
            :key="question.id"
            class="question-item"
          >
            <div class="question-header">
              <div class="question-info">
                <span class="question-number">{{ index + 1 }}.</span>
                <t-tag theme="info" size="small">{{ getQuestionTypeText(question.type) }}</t-tag>
                <t-tag :theme="getDifficultyTagType(question.difficulty)" size="small">
                  {{ getDifficultyText(question.difficulty) }}
                </t-tag>
                <t-tag :theme="getWrongTypeTagType(question.wrongType)" size="small">
                  {{ getWrongTypeText(question.wrongType) }}
                </t-tag>
                <t-tag theme="warning" size="small">错误{{ question.wrongCount }}次</t-tag>
                <span class="subject-name">{{ question.subjectName }}</span>
              </div>
              <div class="question-actions">
                <t-button variant="text" size="small" @click="toggleQuestionDetail(question.id)">
                  {{ expandedQuestions.includes(question.id) ? '收起' : '展开' }}
                </t-button>
                <t-button variant="text" size="small" @click="removeWrongQuestion(question.id)">
                  移除
                </t-button>
              </div>
            </div>
            
            <div class="question-content">
              <p class="question-text">{{ question.content }}</p>
              
              <div v-if="expandedQuestions.includes(question.id)" class="question-detail">
                <!-- 选项显示 -->
                <div v-if="question.type !== 'TRUE_FALSE'" class="options">
                  <div
                    v-for="(option, optionIndex) in question.options"
                    :key="optionIndex"
                    class="option-item"
                    :class="{
                      'correct-option': question.correctAnswer.includes(option.key),
                      'wrong-option': question.userAnswer.includes(option.key) && !question.correctAnswer.includes(option.key)
                    }"
                  >
                    <span class="option-key">{{ option.key }}.</span>
                    <span class="option-value">{{ option.value }}</span>
                    <t-icon v-if="question.correctAnswer.includes(option.key)" class="correct-icon">
                      ✅
                    </t-icon>
                    <t-icon v-if="question.userAnswer.includes(option.key) && !question.correctAnswer.includes(option.key)" class="wrong-icon">
                      ❌
                    </t-icon>
                  </div>
                </div>
                
                <!-- 判断题显示 -->
                <div v-else class="true-false-options">
                  <div class="tf-option" :class="{ 'correct-option': question.correctAnswer === 'true' }">
                    <span>正确</span>
                    <t-icon v-if="question.correctAnswer === 'true'" class="correct-icon">
                      ✅
                    </t-icon>
                    <t-icon v-if="question.userAnswer === 'true' && question.correctAnswer !== 'true'" class="wrong-icon">
                      ❌
                    </t-icon>
                  </div>
                  <div class="tf-option" :class="{ 'correct-option': question.correctAnswer === 'false' }">
                    <span>错误</span>
                    <t-icon v-if="question.correctAnswer === 'false'" class="correct-icon">
                      ✅
                    </t-icon>
                    <t-icon v-if="question.userAnswer === 'false' && question.correctAnswer !== 'false'" class="wrong-icon">
                      ❌
                    </t-icon>
                  </div>
                </div>
                
                <!-- 答案解析 -->
                <div class="answer-analysis">
                  <div class="answer-info">
                    <p><strong>您的答案：</strong><span class="user-answer">{{ formatAnswer(question.userAnswer, question.type) }}</span></p>
                    <p><strong>正确答案：</strong><span class="correct-answer">{{ formatAnswer(question.correctAnswer, question.type) }}</span></p>
                  </div>
                  <div v-if="question.explanation" class="explanation">
                    <p><strong>解析：</strong></p>
                    <p class="explanation-text">{{ question.explanation }}</p>
                  </div>
                  <div class="wrong-info">
                    <p class="wrong-time">错误时间：{{ formatDate(question.wrongTime) }}</p>
                    <p class="wrong-type">错题来源：{{ getWrongTypeText(question.wrongType) }}</p>
                    <p class="wrong-count">错误次数：{{ question.wrongCount }}次</p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
        
        <!-- 分页 -->
        <div v-if="wrongQuestions.length > 0" class="pagination">
          <CustomPagination
            :current="pagination.currentPage"
            :page-size="pagination.pageSize"
            :total="pagination.total"
            :page-size-options="[
              { value: 10, label: '10' },
              { value: 20, label: '20' },
              { value: 50, label: '50' }
            ]"
            @current-change="handleCurrentChange"
            @page-size-change="handleSizeChange"
          />
        </div>
      </t-card>
  </div>
</template>

<script>
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { MessagePlugin, DialogPlugin } from 'tdesign-vue-next'
import { wrongBookAPI, subjectAPI } from '../api'
import Multiselect from '@vueform/multiselect'
import CustomPagination from '../components/CustomPagination.vue'

export default {
  name: 'WrongBook',
  components: {
    Multiselect,
    CustomPagination
  },
  setup() {
    const router = useRouter()
    

    
    const subjects = ref([])
    const wrongQuestions = ref([])
    const loading = ref(false)
    const expandedQuestions = ref([])
    
    const filters = reactive({
      subjectId: '',
      type: '',
      difficulty: '',
      wrongType: ''
    })
    
    const pagination = reactive({
      currentPage: 1,
      pageSize: 10,
      total: 0
    })
    
    const stats = reactive({
      totalWrongCount: 0,
      reviewedCount: 0
    })
    
    const totalWrongCount = computed(() => stats.totalWrongCount)
    const reviewedCount = computed(() => stats.reviewedCount)
    const reviewRate = computed(() => {
      return totalWrongCount.value > 0 ? (reviewedCount.value / totalWrongCount.value * 100) : 0
    })
    

    
    // 获取科目列表
    const getSubjects = async () => {
      try {
        const response = await subjectAPI.getEnabledSubjects()
        if (Array.isArray(response)) {
          // API直接返回数组格式
          subjects.value = response
        } else if (response.code === 200 && Array.isArray(response.data)) {
          // 标准格式：包含code和data
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
    
    // 获取错题列表
    const getWrongQuestions = async () => {
      try {
        loading.value = true

        // 构建查询参数，只包含有值的筛选条件
        const params = {
          page: pagination.currentPage,
          size: pagination.pageSize
        }

        // 只添加有值的筛选条件
        if (filters.subjectId) {
          params.subjectId = filters.subjectId
        }
        if (filters.type) {
          params.type = filters.type
        }
        if (filters.difficulty) {
          params.difficulty = filters.difficulty
        }
        if (filters.wrongType) {
          params.wrongType = filters.wrongType
        }

        console.log('📚 WrongBook.vue: 查询参数', params)

        const result = await wrongBookAPI.getWrongQuestions(params)
        console.log('📚 WrongBook.vue: API响应数据', result)
        console.log('📚 WrongBook.vue: API响应类型', typeof result)
        console.log('📚 WrongBook.vue: API响应结构', Object.keys(result || {}))

        // 适配不同的API返回格式
        if (result && result.code === 200) {
          // 标准响应格式
          if (Array.isArray(result.data)) {
            wrongQuestions.value = result.data
            pagination.total = result.total || result.data.length
          } else if (result.data && Array.isArray(result.data.content)) {
            wrongQuestions.value = result.data.content
            pagination.total = result.data.total || 0
          } else if (result.data && Array.isArray(result.data.wrongQuestions)) {
            wrongQuestions.value = result.data.wrongQuestions
            pagination.total = result.data.total || 0
          } else {
            console.warn('⚠️ WrongBook.vue: 标准格式但数据结构未知', result.data)
            wrongQuestions.value = []
            pagination.total = 0
          }
        } else if (result && Array.isArray(result.content)) {
          wrongQuestions.value = result.content
          pagination.total = result.total || 0
        } else if (result && Array.isArray(result.wrongQuestions)) {
          wrongQuestions.value = result.wrongQuestions
          pagination.total = result.total || 0
        } else if (result && Array.isArray(result.data)) {
          wrongQuestions.value = result.data
          pagination.total = result.total || 0
        } else if (result && Array.isArray(result)) {
          wrongQuestions.value = result
          pagination.total = result.length
        } else {
          console.warn('⚠️ WrongBook.vue: 未识别的API返回格式', result)
          wrongQuestions.value = []
          pagination.total = 0
        }

        // 如果没有数据且没有应用筛选条件，添加测试数据
        if (wrongQuestions.value.length === 0 && !params.subjectId && !params.type && !params.difficulty && !params.wrongType) {
          console.log('🧪 WrongBook.vue: 添加测试数据')
          wrongQuestions.value = [
            {
              id: 1,
              content: '下列哪个选项是正确的？',
              type: 'SINGLE_CHOICE',
              difficulty: 2,
              wrongType: 1,
              wrongCount: 3,
              subjectName: '数学',
              options: [
                { key: 'A', value: '选项A' },
                { key: 'B', value: '选项B' },
                { key: 'C', value: '选项C' },
                { key: 'D', value: '选项D' }
              ],
              correctAnswer: ['B'],
              userAnswer: ['A'],
              explanation: '这是解析内容...'
            },
            {
              id: 2,
              content: '判断题：这个说法是正确的。',
              type: 'TRUE_FALSE',
              difficulty: 1,
              wrongType: 2,
              wrongCount: 1,
              subjectName: '语文',
              correctAnswer: 'true',
              userAnswer: 'false',
              explanation: '这是判断题的解析...'
            }
          ]
          pagination.total = 2
          console.log('🧪 WrongBook.vue: 测试数据已添加', wrongQuestions.value)
        }
        
        // 更新统计信息
        stats.totalWrongCount = result.totalWrongCount || pagination.total || 0
        stats.reviewedCount = result.reviewedCount || 0
        
        console.log('📚 WrongBook.vue: 错题数据', wrongQuestions.value, '总数:', pagination.total)
      } catch (error) {
        console.error('获取错题列表失败:', error)
        MessagePlugin.error('获取错题列表失败')
        wrongQuestions.value = []
        pagination.total = 0
      } finally {
        loading.value = false
      }
    }
    
    // 开始错题练习
    const startWrongQuestionPractice = () => {
      // 跳转到练习页面，并传递错题练习参数
      router.push({
        path: '/practice',
        query: {
          mode: 'wrong',
          ...filters
        }
      })
    }

    // 重置筛选条件
    const resetFilters = () => {
      filters.subjectId = null
      filters.type = null
      filters.difficulty = null
      filters.wrongType = null
      getWrongQuestions()
    }
    
    // 移除错题
    const removeWrongQuestion = async (questionId) => {
      try {
        const confirmDialog = DialogPlugin.confirm({
          header: '提示',
          body: '确定要从错题本中移除这道题吗？',
          theme: 'warning',
          confirmBtn: '确定',
          cancelBtn: '取消',
          onConfirm: async () => {
            try {
              await wrongBookAPI.removeWrongQuestion(questionId)
              MessagePlugin.success('移除成功')
              getWrongQuestions()
            } catch (error) {
              console.error('移除错题失败:', error)
            }
          }
        })
      } catch (error) {
        console.error('移除错题失败:', error)
      }
    }
    
    // 清空错题本
    const clearAllWrongQuestions = async () => {
      try {
        const confirmDialog = DialogPlugin.confirm({
          header: '警告',
          body: '确定要清空整个错题本吗？此操作不可恢复。',
          theme: 'warning',
          confirmBtn: '确定清空',
          cancelBtn: '取消',
          onConfirm: async () => {
            try {
              await wrongBookAPI.clearWrongQuestions()
              MessagePlugin.success('清空错题本成功')
              getWrongQuestions()
            } catch (error) {
              console.error('清空错题本失败:', error)
            }
          }
        })
      } catch (error) {
        console.error('清空错题本失败:', error)
      }
    }
    
    // 切换题目详情显示
    const toggleQuestionDetail = (questionId) => {
      const index = expandedQuestions.value.indexOf(questionId)
      if (index > -1) {
        expandedQuestions.value.splice(index, 1)
      } else {
        expandedQuestions.value.push(questionId)
      }
    }
    
    // 格式化答案显示
    const formatAnswer = (answer, type) => {
      if (type === 'TRUE_FALSE') {
        return answer === 'true' ? '正确' : '错误'
      }
      return answer
    }
    
    // 格式化日期
    const formatDate = (dateString) => {
      const date = new Date(dateString)
      return date.toLocaleString('zh-CN')
    }
    
    // 获取题目类型文本
    const getQuestionTypeText = (type) => {
      const typeMap = {
        'SINGLE_CHOICE': '单选题',
        'MULTIPLE_CHOICE': '多选题',
        'TRUE_FALSE': '判断题'
      }
      return typeMap[type] || type
    }
    
    // 获取难度文本
    const getDifficultyText = (difficulty) => {
      const difficultyMap = {
        'EASY': '简单',
        'MEDIUM': '中等',
        'HARD': '困难'
      }
      return difficultyMap[difficulty] || difficulty
    }
    
    // 获取难度标签类型
    const getDifficultyTagType = (difficulty) => {
      const typeMap = {
        'EASY': 'success',
        'MEDIUM': 'warning',
        'HARD': 'danger'
      }
      return typeMap[difficulty] || 'info'
    }
    
    // 获取错题类型文本
    const getWrongTypeText = (wrongType) => {
      const typeMap = {
        'PRACTICE': '练习题目',
        'EXAM_CUSTOM': '自定义试卷',
        'EXAM_FIXED': '固定试卷'
      }
      return typeMap[wrongType] || wrongType
    }
    
    // 获取错题类型标签类型
    const getWrongTypeTagType = (wrongType) => {
      const typeMap = {
        'PRACTICE': '',
        'EXAM_CUSTOM': 'success',
        'EXAM_FIXED': 'warning'
      }
      return typeMap[wrongType] || 'info'
    }
    

    
    // 分页处理方法
    const handleCurrentChange = (page) => {
      pagination.currentPage = page
      getWrongQuestions()
    }

    const handleSizeChange = (size) => {
      pagination.pageSize = size
      pagination.currentPage = 1
      getWrongQuestions()
    }

    onMounted(async () => {
      console.log('🎯 WrongBook.vue: 组件已挂载')

      // 检查用户登录状态
      const token = localStorage.getItem('token')
      if (!token) {
        MessagePlugin.warning('请先登录')
        router.push('/login')
        return
      }

      await getSubjects()
      await nextTick() // 确保DOM更新

      // 强制触发下拉框重新渲染
      console.log('🔄 WrongBook.vue: 强制触发下拉框重新渲染')

      await getWrongQuestions()
    })
    
    return {
      subjects,
      wrongQuestions,
      loading,
      expandedQuestions,
      filters,
      pagination,
      totalWrongCount,
      reviewedCount,
      reviewRate,
      getWrongQuestions,
      startWrongQuestionPractice,
      removeWrongQuestion,
      clearAllWrongQuestions,
      toggleQuestionDetail,
      formatAnswer,
      formatDate,
      getQuestionTypeText,
      getDifficultyText,
      getDifficultyTagType,
      getWrongTypeText,
      getWrongTypeTagType,
      handleCurrentChange,
      handleSizeChange,
      resetFilters
    }
  }
}
</script>

<style scoped>
.wrong-book-page {
  padding: 20px;
  min-height: calc(100vh - 120px);
  background-color: #f5f7fa;
  max-width: 1200px;
  margin: 0 auto;
}

.wrong-book-header {
  max-width: 100%;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .wrong-book-page {
    padding: 15px;
    max-width: 95%;
  }
}

@media (max-width: 768px) {
  .wrong-book-page {
    padding: 10px;
    max-width: 100%;
  }
  
  .filter-card .t-row {
    flex-direction: column;
  }
  
  .filter-card .t-col {
    flex: 0 0 100% !important;
    max-width: 100% !important;
    margin-bottom: 10px;
  }
  
  .stats-row .t-col {
    flex: 0 0 100% !important;
    max-width: 100% !important;
    margin-bottom: 10px;
  }
  
  .question-header {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .question-actions {
    margin-top: 10px;
    align-self: flex-end;
  }
  
  .question-info {
    flex-wrap: wrap;
  }
  
  .true-false-options {
    flex-direction: column;
    gap: 10px;
  }
  
  .tf-option {
    width: 100%;
  }
}

@media (max-width: 480px) {
  .wrong-book-page {
    padding: 8px;
  }
  
  .question-item {
    padding: 15px;
    margin-bottom: 12px;
  }
  
  .question-text {
    font-size: 15px;
  }
  
  .option-item {
    padding: 8px;
    font-size: 14px;
  }
  
  .question-number {
    font-size: 14px;
  }
}

.wrong-book-header {
  text-align: center;
  margin-bottom: 30px;
}

.wrong-book-header h2 {
  color: #333;
  margin-bottom: 10px;
}

.wrong-book-header p {
  color: #666;
}

.filter-card {
  margin-bottom: 20px;
  width: 100%;
}

.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  text-align: center;
}

.question-list-card {
  min-height: 400px;
  width: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.loading-container,
.empty-container {
  padding: 40px 0;
}

.question-list {
  margin-bottom: 20px;
}

.question-item {
  border: 1px solid #eee;
  border-radius: 6px;
  margin-bottom: 15px;
  padding: 20px;
  transition: all 0.3s;
  width: 100%;
}

.question-item:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.question-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.question-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.question-number {
  font-weight: 600;
  color: #333;
  margin-right: 10px;
}

.subject-name {
  color: #666;
  font-size: 14px;
}

.question-actions {
  display: flex;
  gap: 10px;
}

.question-content {
  line-height: 1.6;
}

.question-text {
  font-size: 16px;
  color: #333;
  margin-bottom: 15px;
}

.question-detail {
  border-top: 1px solid #eee;
  padding-top: 15px;
  margin-top: 15px;
}

.options {
  margin-bottom: 20px;
}

.option-item {
  display: flex;
  align-items: center;
  padding: 10px;
  margin-bottom: 8px;
  border-radius: 4px;
  background-color: #f9f9f9;
  position: relative;
}

.option-item.correct-option {
  background-color: #f0f9ff;
  border: 1px solid #67C23A;
}

.option-item.wrong-option {
  background-color: #fef0f0;
  border: 1px solid #F56C6C;
}

.option-key {
  font-weight: 600;
  margin-right: 8px;
}

.option-value {
  flex: 1;
}

.correct-icon {
  color: #67C23A;
  margin-left: 10px;
}

.wrong-icon {
  color: #F56C6C;
  margin-left: 10px;
}

.true-false-options {
  display: flex;
  gap: 20px;
  margin-bottom: 20px;
}

.tf-option {
  display: flex;
  align-items: center;
  padding: 10px 20px;
  border-radius: 4px;
  background-color: #f9f9f9;
  position: relative;
}

.tf-option.correct-option {
  background-color: #f0f9ff;
  border: 1px solid #67C23A;
}

.answer-analysis {
  background-color: #f8f9fa;
  padding: 15px;
  border-radius: 6px;
}

.answer-info {
  margin-bottom: 15px;
}

.answer-info p {
  margin-bottom: 8px;
}

.user-answer {
  color: #F56C6C;
  font-weight: 600;
  margin-left: 5px;
}

.correct-answer {
  color: #67C23A;
  font-weight: 600;
  margin-left: 5px;
}

.explanation {
  margin-bottom: 15px;
}

.explanation-text {
  color: #666;
  line-height: 1.6;
  margin-top: 5px;
}

.wrong-info {
  border-top: 1px solid #eee;
  padding-top: 10px;
}

.wrong-time,
.wrong-type,
.wrong-count {
  color: #999;
  font-size: 12px;
  margin: 0;
  margin-bottom: 5px;
}

.wrong-type {
  color: #666;
}

.wrong-count {
  color: #e6a23c;
  font-weight: 500;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

/* WrongBook Multiselect 自定义样式 */
:deep(.wrong-book-multiselect-container) {
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

:deep(.wrong-book-multiselect-container:hover) {
  border-color: #4dabf7;
}

:deep(.wrong-book-multiselect-container.is-active) {
  border-color: #0052d9;
  box-shadow: 0 0 0 2px rgba(0, 82, 217, 0.1);
}

:deep(.wrong-book-select-dropdown) {
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

:deep(.wrong-book-select-dropdown.is-hidden) {
  display: none !important;
}

/* 确保 WrongBook 页面的 multiselect 基础样式正确 */
:deep(.wrong-book-multiselect-container .multiselect) {
  min-height: 32px;
  height: 32px;
  width: 100%;
}

:deep(.wrong-book-multiselect-container .multiselect-single-label) {
  padding-left: 12px;
  padding-right: 40px;
  line-height: 30px;
}

:deep(.wrong-book-multiselect-container .multiselect-placeholder) {
  padding-left: 12px;
  line-height: 30px;
  color: #bbb;
}

:deep(.wrong-book-multiselect-container .multiselect-caret) {
  margin-right: 12px;
}

/* 修复 WrongBook 页面可能的样式冲突 */
.wrong-book-page :deep(.multiselect) {
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif !important;
  font-size: 14px !important;
  line-height: 1.5 !important;
}

/* 统计卡片样式 */
.stats-container {
  display: flex;
  gap: 16px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.stat-card-wrapper {
  flex: 1;
  min-width: 200px;
  max-width: 300px;
}

.stat-card {
  height: 100px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
}

/* 筛选表单样式 */
.filter-form {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  align-items: flex-end;
  padding: 16px;
}

.filter-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.filter-label {
  font-size: 14px;
  color: #606266;
  font-weight: 500;
  margin-bottom: 4px;
}

.filter-buttons {
  display: flex;
  gap: 8px;
  align-items: flex-end;
}

.filter-buttons .t-button {
  height: 32px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .stats-container {
    flex-direction: column;
    gap: 12px;
  }

  .stat-card-wrapper {
    min-width: auto;
    max-width: none;
  }

  .stat-card {
    height: 80px;
  }

  .filter-form {
    flex-direction: column;
    gap: 12px;
    align-items: stretch;
  }

  .filter-item {
    width: 100%;
  }

  .filter-buttons {
    justify-content: center;
    margin-top: 8px;
  }
}

@media (max-width: 480px) {
  .stats-container {
    gap: 8px;
  }

  .stat-card {
    height: 70px;
  }

  .filter-form {
    padding: 12px;
  }

  .filter-buttons {
    flex-direction: column;
    gap: 8px;
  }

  .filter-buttons .t-button {
    width: 100%;
  }
}
</style>

<style src="@vueform/multiselect/themes/default.css"></style>