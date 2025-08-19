<template>
  <div class="exam-detail-page">
    <div class="exam-detail" v-loading="loading">
        <!-- 考试基本信息 -->
        <t-card class="exam-info-card">
          <div class="exam-header">
            <div class="exam-title">
              <h2>{{ examDetail.examTitle || '考试详情' }}</h2>
              <p class="exam-description">{{ examDetail.examDescription || '' }}</p>
            </div>
            <div class="exam-status">
              <t-tag :type="examDetail.passed ? 'success' : 'danger'" size="large">
                {{ examDetail.passed ? '已通过' : '未通过' }}
              </t-tag>
            </div>
            <div class="exam-actions">
              <t-button @click="goBack">
                <span class="icon">⬅️</span>
                返回列表
              </t-button>
            </div>
          </div>
          
          <div class="exam-stats">
            <t-row :gutter="20">
              <t-col :span="6">
                <div class="stat-item">
                  <div class="stat-value" :class="{ 'passed': examDetail.passed }">
                    {{ examDetail.totalScore || 0 }}
                  </div>
                  <div class="stat-label">总分</div>
                </div>
              </t-col>
              <t-col :span="6">
                <div class="stat-item">
                  <div class="stat-value">{{ examDetail.passScore || 0 }}</div>
                  <div class="stat-label">及格分</div>
                </div>
              </t-col>
              <t-col :span="6">
                <div class="stat-item">
                  <div class="stat-value">{{ examDetail.correctCount || 0 }}/{{ examDetail.totalCount || 0 }}</div>
                  <div class="stat-label">正确题数</div>
                </div>
              </t-col>
              <t-col :span="6">
                <div class="stat-item">
                  <div class="stat-value">{{ examDetail.accuracy ? examDetail.accuracy.toFixed(1) : '0.0' }}%</div>
                  <div class="stat-label">正确率</div>
                </div>
              </t-col>
            </t-row>
          </div>
          
          <div class="exam-meta">
            <div class="meta-item">
              <span class="meta-label">科目：</span>
              <span class="meta-value">{{ examDetail.subjectName }}</span>
            </div>
            <div class="meta-item">
              <span class="meta-label">考试时长：</span>
              <span class="meta-value">{{ examDetail.duration }} 分钟</span>
            </div>
            <div class="meta-item">
              <span class="meta-label">提交时间：</span>
              <span class="meta-value">{{ formatDateTime(examDetail.submitTime) }}</span>
            </div>
          </div>
        </t-card>
        
        <!-- 答题详情 -->
        <t-card class="answer-details-card">
          <div class="card-header">
            <h3>答题详情</h3>
            <div class="filter-tabs">
              <div class="custom-radio-group">
                <button
                  v-for="option in filterOptions"
                  :key="option.value"
                  class="filter-button"
                  :class="{ 'active': filterType === option.value }"
                  @click="handleFilterChange(option.value)"
                >
                  {{ option.label }}
                  <span v-if="option.count !== undefined" class="count-badge">{{ option.count }}</span>
                </button>
              </div>
            </div>
          </div>
          
          <div class="questions-list">
            <div
              v-for="question in filteredQuestions"
              :key="question.questionId"
              class="question-item"
              :class="{ 'correct': question.isCorrect, 'wrong': !question.isCorrect }"
            >
              <div class="question-header">
                <div class="question-info">
                  <span class="question-number">第 {{ getOriginalIndex(question) }} 题</span>
                  <t-tag type="info" size="small">{{ getQuestionTypeText(question.questionType) }}</t-tag>
                  <span class="question-score">{{ question.score }} 分</span>
                </div>
                <div class="answer-status">
                  <t-tag :type="question.isCorrect ? 'success' : 'danger'" size="small">
                    {{ question.isCorrect ? '正确' : '错误' }}
                  </t-tag>
                </div>
              </div>
              
              <div class="question-content">
                <h4>{{ question.questionContent }}</h4>
                
                <!-- 题目图片显示 -->
                <div v-if="getQuestionImages(question.questionImages).length > 0" class="question-images">
                  <div class="image-grid">
                    <div 
                      v-for="(image, imgIndex) in getQuestionImages(question.questionImages)" 
                      :key="imgIndex"
                      class="image-item"
                      @click="previewImage(image, imgIndex)"
                    >
                      <img 
                        :src="formatImageUrl(image)" 
                        :alt="`题目图片 ${imgIndex + 1}`"
                        @error="handleImageError"
                      />
                      <div class="image-overlay">
                        <span class="zoom-icon">🔍</span>
                      </div>
                    </div>
                  </div>
                </div>
                
                <!-- 选择题选项 -->
                <div v-if="question.questionType <= 2" class="options">
                  <div
                    v-for="option in question.options"
                    :key="option.key"
                    class="option-item"
                    :class="{
                      'user-selected': isUserSelected(question, option.key),
                      'correct-answer': isCorrectAnswer(question, option.key),
                      'wrong-selected': isUserSelected(question, option.key) && !isCorrectAnswer(question, option.key)
                    }"
                  >
                    <span class="option-key">{{ option.key }}.</span>
                    <span class="option-text">{{ option.value }}</span>
                    <div class="option-indicators">
                      <span v-if="isCorrectAnswer(question, option.key)" class="icon correct-icon">✅</span>
                      <span v-if="isUserSelected(question, option.key) && !isCorrectAnswer(question, option.key)" class="icon wrong-icon">❌</span>
                    </div>
                  </div>
                </div>
                
                <!-- 填空题和简答题 -->
                <div v-else class="text-answers">
                  <div class="answer-section">
                    <div class="answer-label">您的答案：</div>
                    <div class="answer-content user-answer">{{ question.userAnswer || '未作答' }}</div>
                  </div>
                  <div class="answer-section">
                    <div class="answer-label">正确答案：</div>
                    <div class="answer-content correct-answer">{{ question.correctAnswer }}</div>
                  </div>
                </div>
                
                <!-- 解析 -->
                <div v-if="question.explanation" class="explanation">
                  <div class="explanation-label">
                    <span class="icon">ℹ️</span>
                    题目解析
                  </div>
                  <div class="explanation-content">{{ question.explanation }}</div>
                </div>
              </div>
            </div>
          </div>
          
          <!-- 空状态 -->
          <t-empty v-if="filteredQuestions.length === 0" description="没有找到相关题目" />
        </t-card>
    </div>
    
    <!-- 图片预览模态框 -->
    <t-dialog
      v-model:visible="showImagePreview"
      :header="false"
      :footer="false"
      :close-on-overlay-click="true"
      :close-on-esc-key="true"
      width="auto"
      class="image-preview-dialog"
      @close="closeImagePreview"
    >
      <div class="image-preview-container">
        <img 
          :src="previewImageUrl" 
          :alt="previewImageAlt"
          class="preview-image"
          @error="handlePreviewImageError"
        />
        <div class="image-preview-actions">
          <t-button 
            type="text" 
            @click="closeImagePreview"
            class="close-preview-btn"
          >
            <span class="icon">❌</span>
          </t-button>
        </div>
      </div>
    </t-dialog>
  </div>
</template>

<script>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { MessagePlugin } from 'tdesign-vue-next'
// TDesign icons imported individually
import { examAPI } from '../api'

export default {
  name: 'ExamDetail',
  components: {
    // 图标组件已移除，使用Emoji替代
  },
  setup() {
    const router = useRouter()
    const route = useRoute()
    
    const loading = ref(false)
    const examDetail = ref({})
    const filterType = ref('all')
    
    // 图片预览相关
    const showImagePreview = ref(false)
    const previewImageUrl = ref('')
    const previewImageAlt = ref('')
    
    // 过滤选项
    const filterOptions = computed(() => {
      // 确保 examDetail.value 和 answerDetails 都存在
      if (!examDetail.value || !examDetail.value.answerDetails || !Array.isArray(examDetail.value.answerDetails)) {
        return [
          { value: 'all', label: '全部题目', count: 0 },
          { value: 'correct', label: '答对题目', count: 0 },
          { value: 'wrong', label: '答错题目', count: 0 }
        ]
      }
      
      const questions = examDetail.value.answerDetails
      const correctCount = questions.filter(q => q && q.isCorrect).length
      const wrongCount = questions.filter(q => q && !q.isCorrect).length
      
      return [
        { value: 'all', label: '全部题目', count: questions.length },
        { value: 'correct', label: '答对题目', count: correctCount },
        { value: 'wrong', label: '答错题目', count: wrongCount }
      ]
    })
    
    // 过滤后的题目列表
    const filteredQuestions = computed(() => {
      if (!examDetail.value || !examDetail.value.answerDetails || !Array.isArray(examDetail.value.answerDetails)) {
        return []
      }
      
      const questions = examDetail.value.answerDetails
      switch (filterType.value) {
        case 'correct':
          return questions.filter(q => q && q.isCorrect)
        case 'wrong':
          return questions.filter(q => q && !q.isCorrect)
        default:
          return questions
      }
    })
    
    // 获取考试详情
    const getExamDetail = async () => {
      try {
        loading.value = true
        const examId = route.params.examId
        const recordId = route.query.recordId
        
        console.log('获取考试详情参数:', { examId, recordId })
        
        if (examId === 'simulation' && recordId) {
          // 模拟考试详情
          examDetail.value = await examAPI.getSimulationExamRecordDetail(recordId)
        } else if (examId && examId !== 'simulation') {
          // 正式考试详情 - 支持负数ID获取历史记录
          const targetExamId = recordId ? parseInt(recordId) : parseInt(examId)
          examDetail.value = await examAPI.getExamRecordDetail(targetExamId)
        } else {
          throw new Error('缺少必要的路由参数')
        }
      } catch (error) {
        console.error('获取考试详情失败:', error)
        MessagePlugin.error('获取考试详情失败: ' + error.message)
        router.push('/exam-list')
      } finally {
        loading.value = false
      }
    }
    
    // 返回列表
    const goBack = () => {
      router.push('/exam-list')
    }
    
    // 过滤类型改变
    const handleFilterChange = (newFilterType) => {
      filterType.value = newFilterType
      console.log('筛选类型改变:', newFilterType)
    }
    
    // 获取题目类型文本
    const getQuestionTypeText = (type) => {
      const typeMap = {
        0: '单选题',
        1: '多选题',
        2: '判断题',
        3: '填空题',
        4: '简答题'
      }
      return typeMap[type] || '未知类型'
    }
    
    // 获取原始题目序号
    const getOriginalIndex = (question) => {
      if (!examDetail.value || !examDetail.value.answerDetails || !Array.isArray(examDetail.value.answerDetails) || !question) {
        return 0
      }
      const index = examDetail.value.answerDetails.findIndex(q => q && q.questionId === question.questionId)
      return index >= 0 ? index + 1 : 0
    }
    
    // 判断用户是否选择了该选项
    const isUserSelected = (question, optionKey) => {
      if (!question.userAnswer) return false
      
      if (question.questionType === 1) { // 多选题
        const userAnswers = question.userAnswer.split(',')
        return userAnswers.includes(optionKey)
      } else { // 单选题和判断题
        return question.userAnswer === optionKey
      }
    }
    
    // 判断是否为正确答案
    const isCorrectAnswer = (question, optionKey) => {
      if (!question.correctAnswer) return false
      
      if (question.questionType === 1) { // 多选题
        const correctAnswers = question.correctAnswer.split(',')
        return correctAnswers.includes(optionKey)
      } else { // 单选题和判断题
        return question.correctAnswer === optionKey
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
        minute: '2-digit',
        second: '2-digit'
      })
    }
    
    // 图片处理相关函数
    const getQuestionImages = (images) => {
      if (!images) return []
      
      try {
        // 处理JSON字符串格式的图片数组
        if (typeof images === 'string') {
          const parsed = JSON.parse(images)
          return Array.isArray(parsed) ? parsed : [parsed]
        }
        
        // 处理数组格式
        if (Array.isArray(images)) {
          return images
        }
        
        // 处理单个字符串
        return [images]
      } catch (error) {
        console.error('解析题目图片失败:', error)
        return []
      }
    }
    
    const formatImageUrl = (imageUrl) => {
      if (!imageUrl) return ''
      
      // 处理完整的URL
      if (imageUrl.startsWith('http')) {
        return imageUrl
      }
      
      // 处理相对路径
      if (imageUrl.startsWith('/')) {
        return `${import.meta.env.VITE_API_BASE_URL || ''}${imageUrl}`
      }
      
      // 处理数据库中的相对路径
      return `${import.meta.env.VITE_API_BASE_URL || ''}/uploads/${imageUrl}`
    }
    
    const handleImageError = (event) => {
      event.target.src = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMjAwIiBoZWlnaHQ9IjE1MCIgdmlld0JveD0iMCAwIDIwMCAxNTAiIGZpbGw9Im5vbmUiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+CjxyZWN0IHdpZHRoPSIyMDAiIGhlaWdodD0iMTUwIiBmaWxsPSIjRjNGNEY2Ii8+CjxwYXRoIGQ9Ik0xMDAgNzVMODUgNjBMNzAgNzVMMTAwIDEwNUwxMzAgNzVMMTE1IDYwTDEwMCA3NVoiIGZpbGw9IiM5Q0EzQUYiLz4KPHBhdGggZD0iTTUwIDkwTDQwIDgwTDQwIDEyMEwxNjAgMTIwTDE2MCA4MEwxNTAgOTBMNTAgOTBaIiBmaWxsPSIjQzBDNUMwIi8+Cjwvc3ZnPgo='
    }
    
    const previewImage = (imageUrl, index) => {
      previewImageUrl.value = formatImageUrl(imageUrl)
      previewImageAlt.value = `题目图片 ${index + 1}`
      showImagePreview.value = true
    }
    
    const closeImagePreview = () => {
      showImagePreview.value = false
      previewImageUrl.value = ''
      previewImageAlt.value = ''
    }
    
    const handlePreviewImageError = () => {
      previewImageUrl.value = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNDAwIiBoZWlnaHQ9IjMwMCIgdmlld0JveD0iMCAwIDQwMCAzMDAiIGZpbGw9Im5vbmUiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+CjxyZWN0IHdpZHRoPSI0MDAiIGhlaWdodD0iMzAwIiBmaWxsPSIjRjNGNEY2Ii8+CjxwYXRoIGQ9Ik0yMDAgMTUwTDE3MCAxMjBMMTQwIDE1MEwyMDAgMjEwTDI2MCAxNTBMMjMwIDEyMEwyMDAgMTUwWiIgZmlsbD0iIzlDQTNBRiIvPgo8cGF0aCBkPSJNMTAwIDEyMEw4MCAxMDBMODAgMjIwTDMyMCAyMjBMMzIwIDEwMEwzMDAgMTIwTDEwMCAxMjBaIiBmaWxsPSIjQzBDNUMwIi8+Cjwvc3ZnPgo='
    }
    
    onMounted(() => {
      getExamDetail()
    })
    
    return {
      loading,
      examDetail,
      filterType,
      filterOptions,
      filteredQuestions,
      goBack,
      handleFilterChange,
      getQuestionTypeText,
      getOriginalIndex,
      isUserSelected,
      isCorrectAnswer,
      formatDateTime,
      // 图片预览相关
      showImagePreview,
      previewImageUrl,
      previewImageAlt,
      getQuestionImages,
      formatImageUrl,
      handleImageError,
      previewImage,
      closeImagePreview,
      handlePreviewImageError
    }
  }
}
</script>

<style scoped>
.exam-detail-page {
  padding: 20px;
  min-height: calc(100vh - 120px);
  background-color: #f5f7fa;
  display: flex;
  justify-content: center;
}

.main-wrapper.sidebar-hidden {
  margin-left: 0;
}

.main-content {
  flex: 1;
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
}

.exam-detail {
  display: flex;
  flex-direction: column;
  gap: 20px;
  max-width: 800px;
  margin: 0 auto;
}

.exam-info-card {
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.exam-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
  padding-bottom: 20px;
  border-bottom: 1px solid #eee;
  flex-wrap: wrap;
  gap: 15px;
}

.exam-title h2 {
  color: #333;
  margin-bottom: 10px;
  font-size: 22px;
}

.exam-description {
  color: #666;
  font-size: 14px;
  margin: 0;
}

.exam-stats {
  margin-bottom: 30px;
}

.exam-stats .t-row {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 15px;
}

.exam-stats .t-col {
  width: 100%;
  max-width: none;
}

.stat-item {
  text-align: center;
  padding: 15px;
  border: 1px solid #eee;
  border-radius: 8px;
  background-color: #fafafa;
  min-height: 100px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
  color: #333;
  margin-bottom: 6px;
}

.stat-value.passed {
  color: #67c23a;
}

.stat-label {
  color: #666;
  font-size: 13px;
}

.exam-meta {
  display: flex;
  gap: 30px;
  flex-wrap: wrap;
}

.meta-item {
  display: flex;
  align-items: center;
}

.meta-label {
  color: #666;
  margin-right: 8px;
}

.meta-value {
  color: #333;
  font-weight: 500;
}

.answer-details-card {
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid #eee;
}

.card-header h3 {
  color: #333;
  margin: 0;
}

/* 自定义单选框组件样式 */
.custom-radio-group {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.filter-button {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 8px 16px;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  background: white;
  color: #666;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
  min-height: 32px;
  min-width: 100px;
  flex: 1;
  max-width: 140px;
}

.filter-button:hover {
  border-color: #409eff;
  color: #409eff;
  background: #f0f8ff;
}

.filter-button.active {
  border-color: #409eff;
  background: #409eff;
  color: white;
  font-weight: 500;
}

.filter-button.active:hover {
  background: #337ecc;
  border-color: #337ecc;
}

.count-badge {
  background: rgba(255, 255, 255, 0.2);
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
  min-width: 22px;
  text-align: center;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 20px;
  flex-shrink: 0;
}

.filter-button.active .count-badge {
  background: rgba(255, 255, 255, 0.3);
  color: white;
}

.filter-button:not(.active) .count-badge {
  background: #f0f0f0;
  color: #666;
}

.questions-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.question-item {
  border: 1px solid #eee;
  border-radius: 8px;
  padding: 20px;
  background-color: #fff;
}

.question-item.correct {
  border-left: 4px solid #67c23a;
}

.question-item.wrong {
  border-left: 4px solid #f56c6c;
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
}

.question-score {
  color: #409eff;
  font-weight: 500;
}

.question-content h4 {
  color: #333;
  margin-bottom: 15px;
  line-height: 1.6;
}

.options {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 15px;
}

.option-item {
  display: flex;
  align-items: center;
  padding: 12px 15px;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  background-color: #fafafa;
  position: relative;
}

.option-item.correct-answer {
  background-color: #f0f9ff;
  border-color: #67c23a;
}

.option-item.user-selected {
  background-color: #e6f7ff;
  border-color: #409eff;
}

.option-item.wrong-selected {
  background-color: #fef0f0;
  border-color: #f56c6c;
}

.option-key {
  font-weight: 600;
  margin-right: 8px;
  color: #333;
}

.option-text {
  flex: 1;
  color: #333;
}

.option-indicators {
  display: flex;
  gap: 5px;
}

.correct-icon {
  color: #67c23a;
}

.wrong-icon {
  color: #f56c6c;
}

.text-answers {
  display: flex;
  flex-direction: column;
  gap: 15px;
  margin-bottom: 15px;
}

.answer-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.answer-label {
  font-weight: 600;
  color: #333;
}

.answer-content {
  padding: 12px 15px;
  border-radius: 6px;
  line-height: 1.6;
}

.user-answer {
  background-color: #e6f7ff;
  border: 1px solid #409eff;
}

.correct-answer {
  background-color: #f0f9ff;
  border: 1px solid #67c23a;
}

.explanation {
  background-color: #f8f9fa;
  border: 1px solid #e9ecef;
  border-radius: 6px;
  padding: 15px;
}

.explanation-label {
  display: flex;
  align-items: center;
  gap: 5px;
  font-weight: 600;
  color: #409eff;
  margin-bottom: 10px;
}

.explanation-content {
  color: #333;
  line-height: 1.6;
}

/* 题目图片样式 */
.question-images {
  margin: 15px 0;
}

.image-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 10px;
}

.image-item {
  position: relative;
  cursor: pointer;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #e4e7ed;
  transition: all 0.3s ease;
}

.image-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.image-item img {
  width: 100%;
  height: 150px;
  object-fit: cover;
  display: block;
}

.image-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.image-item:hover .image-overlay {
  opacity: 1;
}

.zoom-icon {
  font-size: 24px;
  color: white;
  background: rgba(0, 0, 0, 0.7);
  padding: 8px;
  border-radius: 50%;
}

/* 图片预览模态框样式 */
.image-preview-dialog {
  max-width: 90vw;
  max-height: 90vh;
}

.image-preview-container {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 300px;
}

.preview-image {
  max-width: 85vw;
  max-height: 85vh;
  object-fit: contain;
  border-radius: 8px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
}

.image-preview-actions {
  position: absolute;
  top: 10px;
  right: 10px;
  z-index: 1001;
}

.close-preview-btn {
  background: rgba(0, 0, 0, 0.5) !important;
  color: white !important;
  border: none !important;
  border-radius: 50% !important;
  width: 40px !important;
  height: 40px !important;
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  font-size: 16px !important;
}

.close-preview-btn:hover {
  background: rgba(0, 0, 0, 0.7) !important;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .exam-header {
    flex-direction: column;
    gap: 15px;
    align-items: flex-start;
  }
  
  .exam-meta {
    flex-direction: column;
    gap: 10px;
  }
  
  .card-header {
    flex-direction: column;
    gap: 15px;
    align-items: flex-start;
  }
  
  .exam-stats .t-row {
    grid-template-columns: 1fr;
  }
  
  .stat-item {
    padding: 12px;
    min-height: 80px;
  }
  
  .stat-value {
    font-size: 20px;
  }
  
  .exam-title h2 {
    font-size: 20px;
  }
  
  .custom-radio-group {
    flex-direction: column;
    gap: 8px;
    max-width: none;
  }
  
  .filter-button {
    padding: 12px 16px;
    font-size: 15px;
    justify-content: space-between;
    min-width: 120px;
    max-width: none;
    flex: none;
  }
  
  .count-badge {
    margin-left: auto;
    min-width: 24px;
    height: 22px;
  }
}
</style>