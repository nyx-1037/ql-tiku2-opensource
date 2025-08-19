<template>
  <BaseCard
    class="question-answer-card"
    theme="default"
  >
    <!-- 使用 BaseCard 暴露的 header-left / header-right 插槽，确保显示 -->
    <template #header-left>
      <div class="question-info">
        <div class="question-title-row">
          <h3 class="question-title">{{ question.title || '题目' }}</h3>
        </div>
        <div class="question-meta">
          <t-tag theme="default" size="small">ID: {{ getQuestionId() }}</t-tag>
          <t-tag theme="primary" size="small">{{ question.subjectName || '未知科目' }}</t-tag>
          <t-tag theme="warning" size="small">{{ question.difficultyName || '未知难度' }}</t-tag>
          <t-tag theme="success" size="small">{{ question.questionTypeName || '未知类型' }}</t-tag>
        </div>
      </div>
    </template>
    <template #header-right>
      <div class="question-actions">
        <t-button
          theme="danger"
          size="small"
          @click="showExitConfirm"
        >
          <t-icon name="close" />
          退出练习
        </t-button>
      </div>
    </template>

    <div class="question-answer-content">
      <!-- 题目内容 -->
      <div class="question-section">
        <div class="question-content" v-html="question.content"></div>
        
        <!-- 题目图片 -->
        <div v-if="question.imageList && question.imageList.length > 0" class="question-images">
          <div v-for="(imageUrl, index) in question.imageList" :key="index" class="question-image">
            <img :src="imageUrl" :alt="`题目图片 ${index + 1}`" @click="previewImage(imageUrl)" />
          </div>
        </div>
        <div v-else-if="question.imageUrl" class="question-image">
          <img :src="question.imageUrl" :alt="question.content" @click="previewImage(question.imageUrl)" />
        </div>
      </div>

      <!-- 答题区域 -->
      <div class="answer-section">
        <div class="answer-header">
          <h4 class="answer-title">请选择答案</h4>
          <div class="answer-actions">
            <t-button 
              theme="primary" 
              :disabled="!canSubmit"
              :loading="submitting"
              @click="handleSubmit"
            >
              {{ submitButtonText }}
            </t-button>
            <t-button 
              theme="default" 
              :disabled="submitting"
              @click="handleSkip"
            >
              {{ showResult ? '下一题' : '跳过题目' }}
            </t-button>
          </div>
        </div>

        <!-- 单选题 -->
        <div v-if="questionType === 1" class="single-choice">
          <t-radio-group 
            v-model="selectedAnswer" 
            @change="handleAnswerChange"
          >
            <t-radio 
              v-for="option in question.optionList"
              :key="option.key"
              :value="option.key"
              class="answer-option"
            >
              <div class="option-content">
                <span class="option-key">{{ option.key }}.</span>
                <span class="option-text">{{ option.value }}</span>
              </div>
            </t-radio>
          </t-radio-group>
        </div>

        <!-- 多选题 -->
        <div v-else-if="questionType === 2" class="multiple-choice">
          <t-checkbox-group 
            v-model="selectedAnswers" 
            @change="handleAnswerChange"
          >
            <t-checkbox 
              v-for="option in question.optionList"
              :key="option.key"
              :value="option.key"
              class="answer-option"
            >
              <div class="option-content">
                <span class="option-key">{{ option.key }}.</span>
                <span class="option-text">{{ option.value }}</span>
              </div>
            </t-checkbox>
          </t-checkbox-group>
        </div>

        <!-- 判断题 -->
        <div v-else-if="questionType === 3" class="true-false">
          <t-radio-group 
            v-model="selectedAnswer" 
            @change="handleAnswerChange"
          >
            <t-radio value="A" class="answer-option">
              <div class="option-content">
                <span class="option-key">A.</span>
                <span class="option-text">正确</span>
              </div>
            </t-radio>
            <t-radio value="B" class="answer-option">
              <div class="option-content">
                <span class="option-key">B.</span>
                <span class="option-text">错误</span>
              </div>
            </t-radio>
          </t-radio-group>
        </div>

        <!-- 填空题/简答题 -->
        <div v-else-if="questionType >= 4" class="fill-blank">
          <t-textarea
            v-model="fillAnswer"
            placeholder="请输入答案..."
            :autosize="{ minRows: 4, maxRows: 8 }"
            @input="handleTextareaInput"
            @compositionstart="handleCompositionStart"
            @compositionupdate="handleCompositionUpdate"
            @compositionend="handleCompositionEnd"
            class="fill-input"
          />
        </div>

        <!-- 答题提示 -->
        <div class="answer-hint">
          <t-alert theme="info" :close="false">
            <template #icon>
              <t-icon name="info-circle" />
            </template>
            {{ getHintText() }}
          </t-alert>
        </div>
      </div>
    </div>
  </BaseCard>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { DialogPlugin } from 'tdesign-vue-next'
import BaseCard from './BaseCard.vue'
import * as Vue from 'vue'

// Props
const props = defineProps({
  question: {
    type: Object,
    required: true
  },
  submitting: {
    type: Boolean,
    default: false
  },
  showResult: {
    type: Boolean,
    default: false
  }
})

// Emits
const emit = defineEmits(['answer-change', 'submit-answer', 'skip-question', 'exit-practice'])

// Reactive data
const selectedAnswer = ref('')
const selectedAnswers = ref([])
const fillAnswer = ref('')
const isComposing = ref(false) // 中文输入法状态标记

// Computed
const questionType = computed(() => {
  // 原始类型：0-单选，1-多选，2-判断，3+-填空/简答
  // 转换为：1-单选，2-多选，3-判断，4-填空
  const typeMap = {
    0: 1, // 单选
    1: 2, // 多选
    2: 3, // 判断
  }
  return typeMap[props.question.questionType] || 4 // 默认为填空
})

const canSubmit = computed(() => {
  switch (questionType.value) {
    case 1: // 单选
    case 3: // 判断
      return selectedAnswer.value !== ''
    case 2: // 多选
      return selectedAnswers.value.length > 0
    case 4: // 填空
      return fillAnswer.value && fillAnswer.value.trim() !== ''
    default:
      return false
  }
})

const submitButtonText = computed(() => {
  if (props.submitting) return '提交中...'
  return '提交答案'
})

// Methods
const handleAnswerChange = () => {
  let answer = ''
  
  switch (questionType.value) {
    case 1: // 单选
    case 3: // 判断
      answer = selectedAnswer.value || ''
      break
    case 2: // 多选
      answer = selectedAnswers.value.sort().join('')
      break
    case 4: // 填空
      // 确保 fillAnswer.value 是字符串类型
      const fillValue = fillAnswer.value
      if (typeof fillValue === 'string') {
        answer = fillValue.trim()
      } else {
        answer = ''
        // 如果不是字符串，重置为空字符串
        fillAnswer.value = ''
      }
      break
  }
  
  emit('answer-change', answer)
}

// 处理中文输入法开始
const handleCompositionStart = () => {
  console.log('🎯 中文输入法开始')
  isComposing.value = true
}

// 处理中文输入法更新
const handleCompositionUpdate = (event) => {
  console.log('🎯 中文输入法更新:', event.data)
  // 在输入法过程中不触发答案变化事件
}

// 处理中文输入法结束
const handleCompositionEnd = (event) => {
  console.log('🎯 中文输入法结束:', event.target.value)
  isComposing.value = false
  // 确保在输入法结束后正确更新值并触发事件
  const newValue = event.target.value || ''
  fillAnswer.value = newValue
  handleAnswerChange()
}

// 处理文本框输入事件
const handleTextareaInput = (value, context) => {
  console.log('🎯 文本框输入事件:', { value, isComposing: isComposing.value })
  
  // 获取实际的字符串值
  let actualValue = ''
  if (typeof value === 'string') {
    actualValue = value
  } else if (value && value.target && typeof value.target.value === 'string') {
    // 如果是事件对象，从 target.value 获取值
    actualValue = value.target.value
  } else {
    actualValue = ''
  }
  
  // 如果正在使用中文输入法，不立即触发答案变化
  if (!isComposing.value) {
    fillAnswer.value = actualValue
    handleAnswerChange()
  }
}

const handleSubmit = () => {
  if (!canSubmit.value) return
  
  let answer = ''
  switch (questionType.value) {
    case 1: // 单选
    case 3: // 判断
      answer = selectedAnswer.value || ''
      break
    case 2: // 多选
      answer = selectedAnswers.value.sort().join('')
      break
    case 4: // 填空
      const fillValue = fillAnswer.value
      if (typeof fillValue === 'string') {
        answer = fillValue.trim()
      } else {
        answer = ''
      }
      break
  }
  
  console.log('QuestionAnswerCard handleSubmit:', answer)
  emit('submit-answer', answer)
}

const handleSkip = () => {
  console.log('QuestionAnswerCard handleSkip')
  emit('skip-question')
}

// 显示退出确认对话框
const showExitConfirm = () => {
  const confirmDialog = DialogPlugin.confirm({
    header: '确认退出练习',
    body: '确定要退出当前练习吗？退出后当前进度将不会保存。',
    confirmBtn: '确认退出',
    cancelBtn: '取消',
    theme: 'warning',
    onConfirm: () => {
      emit('exit-practice')
      confirmDialog.destroy()
    },
    onCancel: () => {
      confirmDialog.destroy()
    }
  })
}

const getHintText = () => {
  const hintMap = {
    1: '请选择一个正确答案',
    2: '请选择一个或多个正确答案',
    3: '请判断题目描述是否正确',
    4: '请在文本框中输入答案'
  }
  return hintMap[questionType.value] || '请选择答案'
}

// 获取题目ID
const getQuestionId = () => {
  console.log('=== 获取题目ID调试 ===')
  console.log('question对象:', props.question)
  console.log('question.id:', props.question.id)
  console.log('question.questionId:', props.question.questionId)
  console.log('所有属性:', Object.keys(props.question || {}))
  console.log('====================')
  
  const id = props.question.id || props.question.questionId
  if (id !== undefined && id !== null) {
    return String(id)
  }
  return '未知'
}

// 获取题目类型文本
const getQuestionTypeText = () => {
  console.log('=== 获取题目类型调试 ===')
  console.log('question对象:', props.question)
  console.log('questionType:', props.question.questionType)
  console.log('questionTypeName:', props.question.questionTypeName)
  console.log('所有属性:', Object.keys(props.question || {}))
  console.log('=====================')
  
  const typeMap = {
    0: '单选题',
    1: '多选题', 
    2: '判断题',
    3: '简答题',
    4: '填空题'
  }
  
  // 优先使用questionTypeName
  if (props.question.questionTypeName) {
    console.log('使用questionTypeName:', props.question.questionTypeName)
    return props.question.questionTypeName
  }
  
  // 使用映射
  const mappedType = typeMap[props.question.questionType]
  console.log('使用映射类型:', mappedType, '原始类型:', props.question.questionType)
  
  if (mappedType) {
    return mappedType
  }
  
  // 最后的兜底
  return '未知类型'
}

// 重置答案
const resetAnswer = () => {
  selectedAnswer.value = ''
  selectedAnswers.value = []
  fillAnswer.value = ''
}

// 图片预览
const previewImage = (imageUrl) => {
  if (!imageUrl) return
  
  // 创建图片预览的VNode
  const { h } = Vue
  const previewVNode = h('div', { 
    style: { 
      textAlign: 'center', 
      padding: '20px',
      margin: '0 auto'
    } 
  }, [
    h('img', {
      src: imageUrl,
      style: {
        maxWidth: '100%',
        maxHeight: '70vh',
        borderRadius: '8px',
        boxShadow: '0 4px 12px rgba(0,0,0,0.1)',
        display: 'block',
        margin: '0 auto'
      },
      alt: '题目图片',
      onError: (e) => {
        e.target.style.display = 'none'
        e.target.parentElement.innerHTML += '<p style="color: red; margin-top: 20px;">图片加载失败，请检查网络连接</p>'
      }
    })
  ])

  const imagePreview = DialogPlugin.alert({
    header: '图片预览',
    body: () => previewVNode,
    confirmBtn: '关闭',
    closeOnEscKeydown: true,
    closeOnOverlayClick: true,
    width: 'auto',
    top: '5vh',
    placement: 'center',
    onConfirm: () => {
      imagePreview.destroy()
    }
  })
}

// 暴露方法给父组件
defineExpose({
  resetAnswer
})

// 监听题目变化，重置答案
watch(() => props.question, () => {
  resetAnswer()
}, { deep: true })
</script>

<style scoped>
.question-answer-card {
  margin-bottom: 16px;
  position: relative;
}

.question-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  flex-wrap: wrap;
  gap: 16px;
  position: relative;
}

.question-info {
  flex: 1;
  padding-right: 120px; /* 为右上角按钮留出空间 */
}

.question-title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  flex-wrap: wrap;
  gap: 12px;
}

.question-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: var(--td-text-color-primary);
}

.question-id-type {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
}

.question-meta {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.question-actions {
  flex-shrink: 0;
}

.question-answer-content {
  padding: 0;
}

.question-section {
  padding-bottom: 24px;
  border-bottom: 1px solid var(--td-border-level-1-color);
  margin-bottom: 24px;
}

.question-content {
  font-size: 16px;
  line-height: 1.6;
  color: var(--td-text-color-primary);
  margin-bottom: 16px;
}

.question-content :deep(p) {
  margin: 0 0 12px 0;
}

.question-content :deep(p:last-child) {
  margin-bottom: 0;
}

.question-images {
  margin-top: 16px;
}

.question-image {
  text-align: center;
  margin-bottom: 12px;
}

.question-image:last-child {
  margin-bottom: 0;
}

.question-image img {
  max-width: 100%;
  height: auto;
  border-radius: var(--td-radius-default);
  box-shadow: var(--td-shadow-1);
  cursor: pointer;
  transition: transform 0.2s ease;
}

.question-image img:hover {
  transform: scale(1.02);
}

/* 多张图片时的网格布局 */
.question-images {
  display: grid;
  gap: 12px;
}

.question-images .question-image:nth-child(1):nth-last-child(1) {
  /* 单张图片时保持居中 */
  justify-self: center;
}

.question-images .question-image:nth-child(n+2) {
  /* 多张图片时左对齐 */
  justify-self: start;
}

@media (max-width: 768px) {
  .question-images {
    gap: 8px;
  }
  
  .question-image {
    margin-bottom: 8px;
  }
}

.answer-section {
  padding: 0;
}

.answer-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 20px;
}

.answer-title {
  margin: 0;
  font-size: 16px;
  font-weight: 500;
  color: var(--td-text-color-primary);
}

.answer-actions {
  display: flex;
  gap: 8px;
}

.single-choice,
.multiple-choice,
.true-false {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 20px;
}

.answer-option {
  padding: 12px;
  border: 1px solid var(--td-border-level-1-color);
  border-radius: var(--td-radius-default);
  transition: all 0.2s ease;
  cursor: pointer;
  text-align: left;
  width: 100%;
}

.answer-option:hover {
  border-color: var(--td-brand-color);
  background: var(--td-brand-color-light);
}

.option-content {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  text-align: left;
  width: 100%;
}

.option-key {
  font-weight: 600;
  color: var(--td-brand-color);
  min-width: 20px;
  flex-shrink: 0;
}

.option-text {
  color: var(--td-text-color-primary);
  line-height: 1.5;
  word-break: break-word;
  flex: 1;
  text-align: left;
}

.fill-blank {
  margin-bottom: 20px;
}

.fill-input {
  width: 100%;
}

.answer-hint {
  margin-top: 16px;
}

/* 选中状态样式 */
:deep(.t-radio__input:checked + .t-radio__label .answer-option),
:deep(.t-checkbox__input:checked + .t-checkbox__label .answer-option) {
  border-color: var(--td-brand-color);
  background: var(--td-brand-color-light);
}

/* 确保单选框和多选框组件垂直排列 */
:deep(.t-radio-group),
:deep(.t-checkbox-group) {
  display: flex;
  flex-direction: column;
  gap: 12px;
  width: 100%;
}

:deep(.t-radio),
:deep(.t-checkbox) {
  width: 100%;
  margin: 0;
}

:deep(.t-radio__label),
:deep(.t-checkbox__label) {
  width: 100%;
  padding: 0;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .question-header {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .question-title-row {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
  
  .question-id-type {
    flex-wrap: wrap;
  }
  
  .answer-header {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .answer-actions {
    width: 100%;
    justify-content: flex-end;
  }
  
  .answer-option {
    padding: 10px;
  }
  
  .option-content {
    flex-direction: column;
    gap: 4px;
  }
  
  .option-key {
    min-width: auto;
  }
}
</style>