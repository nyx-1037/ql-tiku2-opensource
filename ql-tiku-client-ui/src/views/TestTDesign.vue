<template>
  <div class="test-page">
    <t-card title="TDesign 组件测试">
      <div class="test-section">
        <h3>单选框测试</h3>
        <t-radio-group v-model="singleAnswer" @change="onSingleChange">
          <t-radio value="A">选项A</t-radio>
          <t-radio value="B">选项B</t-radio>
          <t-radio value="C">选项C</t-radio>
        </t-radio-group>
        <p>当前选择: {{ singleAnswer }}</p>
      </div>

      <div class="test-section">
        <h3>多选框测试</h3>
        <t-checkbox-group v-model="multipleAnswers" @change="onMultipleChange">
          <t-checkbox value="A">选项A</t-checkbox>
          <t-checkbox value="B">选项B</t-checkbox>
          <t-checkbox value="C">选项C</t-checkbox>
        </t-checkbox-group>
        <p>当前选择: {{ multipleAnswers }}</p>
      </div>

      <div class="test-section">
        <h3>判断题测试</h3>
        <t-radio-group v-model="judgeAnswer" @change="onJudgeChange">
          <t-radio value="A">正确</t-radio>
          <t-radio value="B">错误</t-radio>
        </t-radio-group>
        <p>当前选择: {{ judgeAnswer }}</p>
      </div>

      <div class="test-section">
        <h3>模拟考试题目格式</h3>
        <div class="question-card">
          <div class="question-content">
            <h4>这是一道单选题测试</h4>
            <div class="options">
              <t-radio-group v-model="examSingleAnswer" @change="onExamSingleChange">
                <t-radio 
                  v-for="option in singleOptions" 
                  :key="option.key"
                  :value="option.key"
                  class="option-item"
                >
                  {{ option.key }}. {{ option.value }}
                </t-radio>
              </t-radio-group>
            </div>
          </div>
        </div>

        <div class="question-card">
          <div class="question-content">
            <h4>这是一道多选题测试</h4>
            <div class="options">
              <t-checkbox-group v-model="examMultipleAnswers" @change="onExamMultipleChange">
                <t-checkbox 
                  v-for="option in multipleOptions" 
                  :key="option.key"
                  :value="option.key"
                  class="option-item"
                >
                  {{ option.key }}. {{ option.value }}
                </t-checkbox>
              </t-checkbox-group>
            </div>
          </div>
        </div>
      </div>
    </t-card>
  </div>
</template>

<script>
import { ref } from 'vue'

export default {
  name: 'TestTDesign',
  setup() {
    const singleAnswer = ref('')
    const multipleAnswers = ref([])
    const judgeAnswer = ref('')
    const examSingleAnswer = ref('')
    const examMultipleAnswers = ref([])

    const singleOptions = [
      { key: 'A', value: '这是选项A的内容' },
      { key: 'B', value: '这是选项B的内容' },
      { key: 'C', value: '这是选项C的内容' },
      { key: 'D', value: '这是选项D的内容' }
    ]

    const multipleOptions = [
      { key: 'A', value: '多选项A的内容' },
      { key: 'B', value: '多选项B的内容' },
      { key: 'C', value: '多选项C的内容' },
      { key: 'D', value: '多选项D的内容' }
    ]

    const onSingleChange = (value) => {
      console.log('单选框变化:', value)
    }

    const onMultipleChange = (value) => {
      console.log('多选框变化:', value)
    }

    const onJudgeChange = (value) => {
      console.log('判断题变化:', value)
    }

    const onExamSingleChange = (value) => {
      console.log('考试单选题变化:', value)
    }

    const onExamMultipleChange = (value) => {
      console.log('考试多选题变化:', value)
    }

    return {
      singleAnswer,
      multipleAnswers,
      judgeAnswer,
      examSingleAnswer,
      examMultipleAnswers,
      singleOptions,
      multipleOptions,
      onSingleChange,
      onMultipleChange,
      onJudgeChange,
      onExamSingleChange,
      onExamMultipleChange
    }
  }
}
</script>

<style scoped>
.test-page {
  padding: 20px;
}

.test-section {
  margin-bottom: 30px;
  padding: 20px;
  border: 1px solid #e6e6e6;
  border-radius: 6px;
}

.question-card {
  margin-top: 20px;
  padding: 20px;
  border: 1px solid #e6e6e6;
  border-radius: 6px;
  background: #f9f9f9;
}

.options {
  margin-top: 15px;
}

.option-item {
  padding: 15px;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  margin-bottom: 12px;
  transition: all 0.3s;
  cursor: pointer;
}

.option-item:hover {
  border-color: #409EFF;
  background-color: #f0f9ff;
}

:deep(.t-radio),
:deep(.t-checkbox) {
  margin-bottom: 12px !important;
  display: flex !important;
  align-items: flex-start !important;
  cursor: pointer !important;
  user-select: none !important;
}

:deep(.t-radio__input),
:deep(.t-checkbox__input) {
  margin-right: 8px !important;
  flex-shrink: 0 !important;
}

:deep(.t-radio__label),
:deep(.t-checkbox__label) {
  flex: 1 !important;
  line-height: 1.5 !important;
  cursor: pointer !important;
}
</style>
