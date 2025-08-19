<template>
  <BaseCard
    v-if="showResult"
    class="result-display-card"
    :theme="getResultCardTheme()"
  >
    <template #header>
      <div class="result-header">
        <div class="result-status">
          <t-icon :name="getResultIcon()" :class="getResultIconClass()" />
          <span :class="getResultTextClass()">
            {{ getResultText() }}
          </span>
        </div>
        <div class="result-actions">
          <slot name="actions">
          </slot>
        </div>
      </div>
    </template>

    <div class="result-content">
      <div class="answer-comparison">
        <div class="answer-item">
          <span class="answer-label">我的答案：</span>
          <span class="answer-value user-answer">{{ userAnswerText || '无' }}</span>
        </div>
        <div class="answer-item">
          <span class="answer-label">正确答案：</span>
          <span class="answer-value correct-answer">{{ correctAnswerText || '无' }}</span>
        </div>
      </div>
      
      <div v-if="analysis" class="analysis-section">
        <h4 class="analysis-title">题目解析</h4>
        <div class="analysis-content" v-html="formattedAnalysis"></div>
      </div>
      
      <div v-if="showScoreInfo && scoreInfo" class="score-section">
        <div class="score-item">
          <span class="score-label">得分：</span>
          <span class="score-value">{{ scoreInfo.score || 0 }}</span>
        </div>
        <div v-if="scoreInfo.maxScore" class="score-item">
          <span class="score-label">满分：</span>
          <span class="score-value">{{ scoreInfo.maxScore }}</span>
        </div>
      </div>

      <!-- 知识点显示区域 -->
      <div v-if="knowledgePoints && knowledgePoints.length > 0" class="knowledge-points-section">
        <h4 class="knowledge-points-title">相关知识点</h4>
        <div class="knowledge-points-list">
          <span 
            v-for="(point, index) in knowledgePoints" 
            :key="index"
            class="knowledge-point-tag"
          >
            {{ point }}
          </span>
        </div>
      </div>
      
    </div>
  </BaseCard>
</template>

<script setup>
import { computed, watch } from 'vue'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import BaseCard from './BaseCard.vue'

// Props
const props = defineProps({
  showResult: {
    type: Boolean,
    default: false
  },
  isCorrect: {
    type: [Boolean, null],
    default: null
  },
  userAnswer: {
    type: [String, Array],
    default: ''
  },
  correctAnswer: {
    type: String,
    default: ''
  },
  analysis: {
    type: String,
    default: ''
  },
  questionType: {
    type: Number,
    default: 0
  },
  options: {
    type: Array,
    default: () => []
  },
  showScoreInfo: {
    type: Boolean,
    default: false
  },
  scoreInfo: {
    type: Object,
    default: () => null
  },
  knowledgePoints: {
    type: Array,
    default: () => []
  }
})

// Emits
const emit = defineEmits(['next-question'])

// Computed
const userAnswerText = computed(() => {
  if (!props.userAnswer) return '无'
  
  if (Array.isArray(props.userAnswer)) {
    return props.userAnswer.join(', ')
  }
  
  // 判断题特殊处理
  if (props.questionType === 2) {
    if (props.userAnswer === 'A') return '正确'
    if (props.userAnswer === 'B') return '错误'
  }
  
  return props.userAnswer
})

const correctAnswerText = computed(() => {
  if (!props.correctAnswer) return '无'
  
  // 判断题特殊处理
  if (props.questionType === 2) {
    if (props.correctAnswer === 'true') return '正确'
    if (props.correctAnswer === 'false') return '错误'
  }
  
  return props.correctAnswer
})

const formattedAnalysis = computed(() => {
  if (!props.analysis) return ''
  
  try {
    const html = marked(props.analysis)
    return DOMPurify.sanitize(html)
  } catch (error) {
    console.error('解析Markdown失败:', error)
    return props.analysis
  }
})

// Methods

const getResultCardTheme = () => {
  if (props.isCorrect === null) return 'warning'
  return props.isCorrect ? 'success' : 'danger'
}

const getResultIcon = () => {
  if (props.isCorrect === null) return 'help-circle'
  return props.isCorrect ? 'check-circle' : 'close-circle'
}

const getResultIconClass = () => {
  if (props.isCorrect === null) return 'pending-icon'
  return props.isCorrect ? 'correct-icon' : 'wrong-icon'
}

const getResultText = () => {
  if (props.isCorrect === null) return '答案已提交，等待批改'
  return props.isCorrect ? '回答正确' : '回答错误'
}

const getResultTextClass = () => {
  if (props.isCorrect === null) return 'pending-text'
  return props.isCorrect ? 'correct-text' : 'wrong-text'
}

// 监听 isCorrect 状态变化，用于调试和确保状态同步
watch(() => props.isCorrect, (newValue, oldValue) => {
  console.log('🔍 ResultDisplay: isCorrect状态变化:', {
    oldValue,
    newValue,
    showResult: props.showResult,
    timestamp: new Date().toLocaleTimeString()
  })
  
  // 当状态从 null 变为 true/false 时，表示AI判题完成
  if (oldValue === null && newValue !== null) {
    console.log('✅ ResultDisplay: AI判题完成，状态已更新为:', newValue ? '正确' : '错误')
  }
}, { immediate: true })

// 监听 showResult 状态变化
watch(() => props.showResult, (newValue, oldValue) => {
  console.log('🔍 ResultDisplay: showResult状态变化:', {
    oldValue,
    newValue,
    isCorrect: props.isCorrect,
    timestamp: new Date().toLocaleTimeString()
  })
}, { immediate: true })
</script>

<style scoped>
.result-display-card {
  margin-bottom: 16px;
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.result-status {
  display: flex;
  align-items: center;
  gap: 8px;
  justify-content: flex-start;
}

.result-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.correct-icon {
  color: var(--td-success-color);
  font-size: 20px;
}

.wrong-icon {
  color: var(--td-error-color);
  font-size: 20px;
}

.pending-icon {
  color: var(--td-warning-color);
  font-size: 20px;
}

.correct-text {
  color: var(--td-success-color);
  font-weight: 600;
  font-size: 16px;
}

.wrong-text {
  color: var(--td-error-color);
  font-weight: 600;
  font-size: 16px;
}

.pending-text {
  color: var(--td-warning-color);
  font-weight: 600;
  font-size: 16px;
}

.result-content {
  padding: 0;
}

.answer-comparison {
  margin-bottom: 20px;
}

.answer-item {
  display: flex;
  align-items: flex-start;
  margin-bottom: 12px;
  padding: 12px;
  background: var(--td-bg-color-container-hover);
  border-radius: var(--td-radius-default);
}

.answer-label {
  font-weight: 600;
  color: var(--td-text-color-secondary);
  min-width: 80px;
  flex-shrink: 0;
}

.answer-value {
  color: var(--td-text-color-primary);
  line-height: 1.5;
  word-break: break-word;
  flex: 1;
}

.user-answer {
  color: var(--td-text-color-primary);
}

.correct-answer {
  color: var(--td-success-color);
  font-weight: 500;
}

.analysis-section {
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid var(--td-border-level-1-color);
}

.analysis-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--td-text-color-primary);
  margin: 0 0 12px 0;
}

.analysis-content {
  color: var(--td-text-color-primary);
  line-height: 1.6;
  word-break: break-word;
}

.analysis-content :deep(p) {
  margin: 8px 0;
}

.analysis-content :deep(ul),
.analysis-content :deep(ol) {
  margin: 8px 0;
  padding-left: 20px;
}

.analysis-content :deep(li) {
  margin: 4px 0;
}

.analysis-content :deep(code) {
  background: var(--td-bg-color-component);
  padding: 2px 4px;
  border-radius: 3px;
  font-family: 'Courier New', monospace;
}

.analysis-content :deep(pre) {
  background: var(--td-bg-color-component);
  padding: 12px;
  border-radius: 6px;
  overflow-x: auto;
  margin: 12px 0;
}

.score-section {
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px solid var(--td-border-level-1-color);
  display: flex;
  gap: 20px;
  flex-wrap: wrap;
}

.score-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.score-label {
  color: var(--td-text-color-secondary);
  font-size: 14px;
}

.score-value {
  color: var(--td-brand-color);
  font-size: 16px;
  font-weight: 600;
}

.knowledge-points-section {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid var(--td-border-level-1-color);
}

.knowledge-points-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--td-text-color-primary);
  margin-bottom: 12px;
}

.knowledge-points-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.knowledge-point-tag {
  display: inline-block;
  padding: 4px 12px;
  background-color: var(--td-brand-color-light);
  color: var(--td-brand-color);
  border-radius: 16px;
  font-size: 14px;
  border: 1px solid var(--td-brand-color-light);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .result-header {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .result-actions {
    width: 100%;
    justify-content: flex-end;
  }
  
  .answer-item {
    flex-direction: column;
    gap: 4px;
  }
  
  .answer-label {
    min-width: auto;
  }
  
  .score-section {
    flex-direction: column;
    gap: 8px;
  }
}
</style>