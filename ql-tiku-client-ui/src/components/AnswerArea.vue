<template>
  <BaseCard
    class="answer-area-card"
    theme="default"
  >
    <template #header>
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
            跳过题目
          </t-button>
        </div>
      </div>
    </template>

    <div class="answer-content">
      <!-- 单选题 -->
      <div v-if="questionType === 0" class="single-choice">
        <t-radio-group 
          v-model="selectedAnswer" 
          @change="handleAnswerChange"
        >
          <t-radio 
            v-for="option in options"
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
      <div v-else-if="questionType === 1" class="multiple-choice">
        <t-checkbox-group 
          v-model="selectedAnswers" 
          @change="handleAnswerChange"
        >
          <t-checkbox 
            v-for="option in options"
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
      <div v-else-if="questionType === 2" class="true-false">
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

      <!-- 填空题 -->
      <div v-else-if="questionType === 3" class="fill-blank">
        <t-textarea
          v-model="fillAnswer"
          placeholder="请输入答案..."
          :autosize="{ minRows: 3, maxRows: 6 }"
          @input="handleTextareaInput"
          @compositionstart="handleCompositionStart"
          @compositionupdate="handleCompositionUpdate"
          @compositionend="handleCompositionEnd"
          class="fill-input"
        />
      </div>

      <!-- 答题提示 -->
      <div v-if="showHint" class="answer-hint">
        <t-alert theme="info" :close="false">
          <template #icon>
            <t-icon name="info-circle" />
          </template>
          {{ getHintText() }}
        </t-alert>
      </div>
    </div>
  </BaseCard>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import BaseCard from './BaseCard.vue'

// Props
const props = defineProps({
  questionType: {
    type: Number,
    required: true
  },
  options: {
    type: Array,
    default: () => []
  },
  submitting: {
    type: Boolean,
    default: false
  },
  showHint: {
    type: Boolean,
    default: true
  }
})

// Emits
const emit = defineEmits(['answer-change', 'submit', 'skip'])

// Reactive data
const selectedAnswer = ref('')
const selectedAnswers = ref([])
const fillAnswer = ref('')

// Computed
const canSubmit = computed(() => {
  switch (props.questionType) {
    case 0: // 单选
    case 2: // 判断
      return selectedAnswer.value !== ''
    case 1: // 多选
      return selectedAnswers.value.length > 0
    case 3: // 填空
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
  
  switch (props.questionType) {
    case 0: // 单选
    case 2: // 判断
      answer = selectedAnswer.value
      break
    case 1: // 多选
      answer = selectedAnswers.value.sort().join('')
      break
    case 3: // 填空
      answer = fillAnswer.value.trim()
      break
  }
  
  emit('answer-change', answer)
}

// 处理中文输入法开始
const handleCompositionStart = () => {
  console.log('🎯 AnswerArea: 中文输入法开始')
  isComposing.value = true
}

// 处理中文输入法更新
const handleCompositionUpdate = (event) => {
  console.log('🎯 AnswerArea: 中文输入法更新:', event.data)
  // 在输入法过程中不触发答案变化事件
}

// 处理中文输入法结束
const handleCompositionEnd = (event) => {
  console.log('🎯 AnswerArea: 中文输入法结束:', event.target.value)
  isComposing.value = false
  // 确保在输入法结束后正确更新值并触发事件
  fillAnswer.value = event.target.value || ''
  handleAnswerChange()
}

// 处理文本框输入事件
const handleTextareaInput = (value, context) => {
  console.log('🎯 AnswerArea: 文本框输入事件:', { value, isComposing: isComposing.value })
  
  // TDesign 的 t-textarea 组件的 @input 事件直接传递字符串值
  // 如果正在使用中文输入法，不立即触发答案变化
  if (!isComposing.value) {
    fillAnswer.value = typeof value === 'string' ? value : (value || '')
    handleAnswerChange()
  }
}

const handleSubmit = () => {
  if (!canSubmit.value) return
  
  let answer = ''
  switch (props.questionType) {
    case 0: // 单选
    case 2: // 判断
      answer = selectedAnswer.value
      break
    case 1: // 多选
      answer = selectedAnswers.value.sort().join('')
      break
    case 3: // 简答/填空
      answer = fillAnswer.value ? fillAnswer.value.trim() : ''
      break
  }
  
  emit('submit', answer)
}

const handleSkip = () => {
  emit('skip')
}

const getHintText = () => {
  const hintMap = {
    0: '请选择一个正确答案',
    1: '请选择一个或多个正确答案',
    2: '请判断题目描述是否正确',
    3: '请在文本框中输入答案'
  }
  return hintMap[props.questionType] || '请选择答案'
}

// 重置答案
const resetAnswer = () => {
  selectedAnswer.value = ''
  selectedAnswers.value = []
  fillAnswer.value = ''
}

// 暴露方法给父组件
defineExpose({
  resetAnswer
})

// 监听题目类型变化，重置答案
watch(() => props.questionType, () => {
  resetAnswer()
})
</script>

<style scoped>
.answer-area-card {
  margin-bottom: 16px;
}

.answer-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
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

.answer-content {
  padding: 0;
}

.single-choice,
.multiple-choice,
.true-false {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.answer-option {
  padding: 12px;
  border: 1px solid var(--td-border-level-1-color);
  border-radius: var(--td-radius-default);
  transition: all 0.2s ease;
  cursor: pointer;
}

.answer-option:hover {
  border-color: var(--td-brand-color);
  background: var(--td-brand-color-light);
}

.option-content {
  display: flex;
  align-items: flex-start;
  gap: 8px;
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
}

.fill-blank {
  margin-bottom: 16px;
}

.fill-input {
  width: 100%;
}

.answer-hint {
  margin-top: 16px;
}

/* 选中状态样式 */
:deep(.t-radio__input:checked + .t-radio__label),
:deep(.t-checkbox__input:checked + .t-checkbox__label) {
  .answer-option {
    border-color: var(--td-brand-color);
    background: var(--td-brand-color-light);
  }
}

/* 响应式设计 */
@media (max-width: 768px) {
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