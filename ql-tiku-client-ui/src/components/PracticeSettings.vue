<template>
  <BaseCard
    title="练习设置"
    icon="setting"
    theme="primary"
    class="practice-settings-card"
  >
    <div class="filter-section">
      <t-row :gutter="20" class="practice-filter-row">
        <t-col :span="6">
          <t-form-item label="选择科目">
            <VueformSelect
              v-model="localFilters.subjectId"
              :options="subjects"
              value-prop="id"
              label-prop="name"
              placeholder="请选择科目"
              :clearable="true"
              style="width: 100%;"
              @update:model-value="handleFilterChange('subjectId', $event)"
            />
          </t-form-item>
        </t-col>
        <t-col :span="6">
          <t-form-item label="题目类型">
            <VueformSelect
              v-model="localFilters.type"
              :options="questionTypes"
              value-prop="value"
              label-prop="label"
              placeholder="请选择题目类型"
              :clearable="true"
              style="width: 100%;"
              @update:model-value="handleFilterChange('type', $event)"
            />
          </t-form-item>
        </t-col>
        <t-col :span="6">
          <t-form-item label="难度等级">
            <VueformSelect
              v-model="localFilters.difficulty"
              :options="difficultyLevels"
              value-prop="value"
              label-prop="label"
              placeholder="请选择难度"
              :clearable="true"
              style="width: 100%;"
              @update:model-value="handleFilterChange('difficulty', $event)"
            />
          </t-form-item>
        </t-col>
        <t-col :span="6">
          <t-form-item label="刷题模式">
            <VueformSelect
              v-model="localPracticeMode"
              :options="practiceModes"
              value-prop="value"
              label-prop="label"
              placeholder="请选择刷题模式"
              style="width: 100%;"
              @update:model-value="handleModeChange"
            />
          </t-form-item>
        </t-col>
      </t-row>
      
      <div class="filter-actions">
        <t-button theme="primary" @click="handleStartPractice" :loading="isStarting">
          开始练习
        </t-button>
        <t-button @click="handleResetFilters">
          重置条件
        </t-button>
        <t-button 
          theme="warning" 
          @click="handleResetPractice" 
          v-if="hasCurrentPractice"
        >
          <t-icon name="refresh" />
          重置练习
        </t-button>
      </div>
    </div>
  </BaseCard>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import BaseCard from './BaseCard.vue'
import VueformSelect from './VueformSelect.vue'

// Props
const props = defineProps({
  // 科目列表
  subjects: {
    type: Array,
    default: () => []
  },
  // 筛选条件
  filters: {
    type: Object,
    default: () => ({
      subjectId: null,
      type: null,
      difficulty: null
    })
  },
  // 练习模式
  practiceMode: {
    type: String,
    default: 'random'
  },
  // 是否有当前练习
  hasCurrentPractice: {
    type: Boolean,
    default: false
  },
  // 是否正在开始练习
  isStarting: {
    type: Boolean,
    default: false
  }
})

// Emits
const emit = defineEmits([
  'update:filters',
  'update:practiceMode',
  'start-practice',
  'reset-filters',
  'reset-practice'
])

// Refs
const localFilters = ref({ ...props.filters })
const localPracticeMode = ref(props.practiceMode)

// 题目类型选项
const questionTypes = ref([
  { value: 'SINGLE_CHOICE', label: '单选题' },
  { value: 'MULTIPLE_CHOICE', label: '多选题' },
  { value: 'TRUE_FALSE', label: '判断题' }
])

// 难度等级选项
const difficultyLevels = ref([
  { value: 'EASY', label: '简单' },
  { value: 'MEDIUM', label: '中等' },
  { value: 'HARD', label: '困难' }
])

// 练习模式选项
const practiceModes = ref([
  { value: 'random', label: '随机刷题' },
  { value: 'sequential', label: '顺序刷题' }
])

// Methods
const handleFilterChange = (key, value) => {
  localFilters.value[key] = value
  emit('update:filters', { ...localFilters.value })
}

const handleModeChange = (value) => {
  localPracticeMode.value = value
  emit('update:practiceMode', value)
}

const handleStartPractice = () => {
  console.log('🎯 PracticeSettings: 开始练习，当前筛选条件:', localFilters.value)
  
  // 在开始练习前，确保最新的筛选条件已同步到父组件
  emit('update:filters', { ...localFilters.value })
  
  // 等待一个tick确保数据同步完成，然后触发开始练习
  setTimeout(() => {
    emit('start-practice')
  }, 0)
}

const handleResetFilters = () => {
  localFilters.value = {
    subjectId: null,
    type: null,
    difficulty: null
  }
  emit('reset-filters')
}

const handleResetPractice = () => {
  emit('reset-practice')
}

// 监听props变化，同步本地状态
watch(() => props.filters, (newFilters) => {
  localFilters.value = { ...newFilters }
}, { deep: true })

watch(() => props.practiceMode, (newMode) => {
  localPracticeMode.value = newMode
})
</script>

<style scoped>
.practice-settings-card {
  margin-bottom: 20px;
}

.filter-section {
  padding: 0;
}

.practice-filter-row {
  margin-bottom: 20px;
}

.filter-actions {
  text-align: center;
  display: flex;
  justify-content: center;
  gap: 12px;
  flex-wrap: wrap;
}

/* VueformSelect样式 - 添加蓝色轮廓 */
:deep(.practice-multiselect-container) {
  border: 2px solid #409eff !important;
  border-radius: 6px !important;
  box-shadow: 0 2px 4px rgba(64, 158, 255, 0.1) !important;
  transition: all 0.3s ease !important;
  min-height: 40px !important;
  background: white !important;
}

:deep(.practice-multiselect-container:hover) {
  border-color: #337ecc !important;
  box-shadow: 0 4px 8px rgba(64, 158, 255, 0.2) !important;
}

:deep(.practice-multiselect-container.is-active) {
  border-color: #1890ff !important;
  box-shadow: 0 0 0 3px rgba(64, 158, 255, 0.15), 0 4px 8px rgba(64, 158, 255, 0.25) !important;
}

:deep(.practice-multiselect-container .multiselect) {
  width: 100%;
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif !important;
  font-size: 14px !important;
  border: none !important;
  box-shadow: none !important;
  min-height: 36px !important;
}

:deep(.practice-multiselect-container .multiselect-single-label) {
  color: #2d3748 !important;
  font-weight: 500 !important;
  padding-left: 12px !important;
  line-height: 36px !important;
}

:deep(.practice-multiselect-container .multiselect-placeholder) {
  color: #718096 !important;
  padding-left: 12px !important;
  line-height: 36px !important;
}

:deep(.practice-multiselect-container .multiselect-caret) {
  margin-right: 12px !important;
  color: #409eff !important;
}

/* 确保下拉框在卡片内正确显示 */
:deep(.multiselect-dropdown) {
  z-index: 9999 !important;
}

/* 响应式设计 */
@media (max-width: 992px) {
  .practice-filter-row :deep(.t-col) {
    flex: 0 0 50% !important;
    max-width: 50% !important;
    margin-bottom: 16px;
  }
}

@media (max-width: 768px) {
  .practice-filter-row :deep(.t-col) {
    flex: 0 0 100% !important;
    max-width: 100% !important;
    margin-bottom: 12px;
  }
  
  .filter-actions {
    flex-direction: column;
    align-items: center;
    gap: 8px;
  }
  
  .filter-actions .t-button {
    width: 100%;
    max-width: 200px;
  }
}
</style>