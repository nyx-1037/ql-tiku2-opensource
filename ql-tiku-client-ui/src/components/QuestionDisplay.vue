<template>
  <BaseCard
    class="question-display-card"
    theme="default"
  >
    <template #header>
      <div class="question-header">
        <div class="question-info">
          <t-tag theme="default">{{ question.questionTypeName }}</t-tag>
          <t-tag :theme="getDifficultyTagType(question.difficulty)">
            {{ question.difficultyName }}
          </t-tag>
          <span class="subject-name">{{ question.subjectName }}</span>
        </div>
        <div class="question-actions">
          <slot name="actions">
            <t-button variant="outline" @click="$emit('exit-practice')">
              退出练习
            </t-button>
          </slot>
        </div>
      </div>
    </template>

    <div class="question-content">
      <h3 class="question-title">{{ question.title }}</h3>
      
      <!-- 题目内容 -->
      <div v-if="question.content" class="question-text">
        {{ question.content }}
      </div>
      
      <!-- 题目图片展示 -->
      <div v-if="hasImages" class="question-images">
        <div 
          v-for="(imageUrl, index) in question.imageList" 
          :key="index"
          class="question-image"
        >
          <img 
            :src="imageUrl" 
            :alt="`题目图片 ${index + 1}`"
            @click="previewImage(imageUrl)"
            class="question-image-preview"
          />
        </div>
      </div>
      
      <!-- 题目选项展示（如果有） -->
      <div v-if="hasOptions" class="question-options">
        <div 
          v-for="(option, index) in question.optionList"
          :key="index"
          class="option-display"
        >
          <span class="option-key">{{ option.key }}.</span>
          <span class="option-value">{{ option.value }}</span>
        </div>
      </div>
      
      <!-- 题目附加信息 -->
      <div v-if="showQuestionInfo" class="question-meta">
        <div class="meta-item">
          <span class="meta-label">题目ID:</span>
          <span class="meta-value">{{ question.id }}</span>
        </div>
        <div v-if="question.createTime" class="meta-item">
          <span class="meta-label">创建时间:</span>
          <span class="meta-value">{{ formatDate(question.createTime) }}</span>
        </div>
      </div>
    </div>
  </BaseCard>
</template>

<script setup>
import { computed } from 'vue'
import BaseCard from './BaseCard.vue'
import { DialogPlugin } from 'tdesign-vue-next'
import * as Vue from 'vue'

// Props
const props = defineProps({
  question: {
    type: Object,
    required: true,
    default: () => ({})
  },
  showQuestionInfo: {
    type: Boolean,
    default: false
  }
})

// Emits
const emit = defineEmits(['exit-practice'])

// Computed
const hasOptions = computed(() => {
  return props.question.optionList && props.question.optionList.length > 0
})

const hasImages = computed(() => {
  return props.question.imageList && props.question.imageList.length > 0
})

// Methods
const getDifficultyTagType = (difficulty) => {
  const difficultyMap = {
    1: 'success',  // 简单
    2: 'warning',  // 中等
    3: 'danger'    // 困难
  }
  return difficultyMap[difficulty] || 'default'
}

// 图片预览功能
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

const formatDate = (dateString) => {
  if (!dateString) return ''
  const date = new Date(dateString)
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}
</script>

<style scoped>
.question-display-card {
  margin-bottom: 16px;
}

.question-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.question-info {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.subject-name {
  color: var(--td-text-color-secondary);
  font-size: 14px;
  margin-left: 8px;
}

.question-actions {
  display: flex;
  gap: 8px;
}

.question-content {
  padding: 0;
}

.question-title {
  font-size: 20px;
  font-weight: 700;
  color: var(--td-text-color-primary);
  line-height: 1.6;
  margin: 0 0 16px 0;
  word-break: break-word;
}

.question-text {
  font-size: 16px;
  line-height: 1.6;
  color: var(--td-text-color-primary);
  margin-bottom: 16px;
  word-break: break-word;
}

.question-images {
  margin: 20px 0;
}

.question-image {
  text-align: center;
  margin-bottom: 12px;
}

.question-image:last-child {
  margin-bottom: 0;
}

.question-image-preview {
  max-width: 100%;
  height: auto;
  border-radius: var(--td-radius-default);
  box-shadow: var(--td-shadow-1);
  cursor: pointer;
  transition: transform 0.2s ease;
}

.question-image-preview:hover {
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
  
  .question-title {
    font-size: 18px;
  }
}

.question-options {
  margin: 20px 0;
}

.option-display {
  display: flex;
  align-items: flex-start;
  margin-bottom: 12px;
  padding: 12px;
  background: var(--td-bg-color-container-hover);
  border-radius: var(--td-radius-default);
  transition: all 0.2s ease;
}

.option-display:hover {
  background: var(--td-bg-color-container-active);
}

.option-key {
  font-weight: 600;
  color: var(--td-brand-color);
  margin-right: 8px;
  min-width: 20px;
  flex-shrink: 0;
}

.option-value {
  color: var(--td-text-color-primary);
  line-height: 1.5;
  word-break: break-word;
  flex: 1;
}

.question-meta {
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid var(--td-border-level-1-color);
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.meta-label {
  color: var(--td-text-color-secondary);
  font-size: 12px;
}

.meta-value {
  color: var(--td-text-color-primary);
  font-size: 12px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .question-header {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .question-info {
    width: 100%;
  }
  
  .question-actions {
    width: 100%;
    justify-content: flex-end;
  }
  
  .question-title {
    font-size: 16px;
  }
  
  .option-display {
    padding: 10px;
  }
  
  .question-meta {
    flex-direction: column;
    gap: 8px;
  }
}
</style>