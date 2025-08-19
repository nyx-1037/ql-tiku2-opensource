<template>
  <BaseCard
    v-if="showCompletion"
    class="completion-card"
    theme="success"
    size="large"
  >
    <div class="completion-content">
      <div class="completion-icon">
        <t-icon name="check-circle" class="success-icon" />
      </div>
      
      <h2 class="completion-title">{{ completionTitle }}</h2>
      
      <div v-if="showStats" class="completion-stats">
        <div class="stat-item">
          <span class="stat-label">总题数</span>
          <span class="stat-value">{{ stats.totalQuestions || 0 }}</span>
        </div>
        <div class="stat-item">
          <span class="stat-label">正确数</span>
          <span class="stat-value correct">{{ stats.correctCount || 0 }}</span>
        </div>
        <div class="stat-item">
          <span class="stat-label">错误数</span>
          <span class="stat-value wrong">{{ stats.wrongCount || 0 }}</span>
        </div>
        <div class="stat-item">
          <span class="stat-label">正确率</span>
          <span class="stat-value">{{ stats.accuracyRate || 0 }}%</span>
        </div>
        <div v-if="stats.score !== undefined" class="stat-item">
          <span class="stat-label">得分</span>
          <span class="stat-value score">{{ stats.score || 0 }}</span>
        </div>
      </div>
      
      <div class="completion-actions">
        <slot name="actions">
          <t-button 
            theme="primary" 
            size="large" 
            @click="$emit('restart')"
            :loading="loading"
          >
            <template #icon>
              <t-icon name="refresh" />
            </template>
            再来一次
          </t-button>
          <t-button 
            size="large" 
            @click="$emit('go-home')"
          >
            <template #icon>
              <t-icon name="home" />
            </template>
            返回首页
          </t-button>
          <t-button 
            size="large" 
            @click="$emit('reset-settings')"
          >
            <template #icon>
              <t-icon name="setting" />
            </template>
            重新设置
          </t-button>
        </slot>
      </div>
      
      <div v-if="showEncouragement" class="encouragement-section">
        <p class="encouragement-text">{{ getEncouragementText() }}</p>
      </div>
    </div>
  </BaseCard>
</template>

<script setup>
import { computed } from 'vue'
import BaseCard from './BaseCard.vue'

// Props
const props = defineProps({
  showCompletion: {
    type: Boolean,
    default: false
  },
  completionTitle: {
    type: String,
    default: '恭喜！当前条件下的题目已全部完成'
  },
  stats: {
    type: Object,
    default: () => ({
      totalQuestions: 0,
      correctCount: 0,
      wrongCount: 0,
      accuracyRate: 0,
      score: 0
    })
  },
  showStats: {
    type: Boolean,
    default: true
  },
  showEncouragement: {
    type: Boolean,
    default: true
  },
  loading: {
    type: Boolean,
    default: false
  }
})

// Emits
const emit = defineEmits(['restart', 'go-home', 'reset-settings'])

// Computed
const accuracyRate = computed(() => {
  return props.stats.accuracyRate || 0
})

// Methods
const getEncouragementText = () => {
  const rate = accuracyRate.value
  
  if (rate >= 90) {
    return '太棒了！你的表现非常出色，继续保持这种学习状态！'
  } else if (rate >= 80) {
    return '很好！你已经掌握了大部分知识点，再接再厉！'
  } else if (rate >= 70) {
    return '不错的成绩！继续努力，你会做得更好的！'
  } else if (rate >= 60) {
    return '还有提升空间，多练习一些题目会帮助你进步！'
  } else {
    return '没关系，学习是一个过程，多练习多总结，你一定会进步的！'
  }
}
</script>

<style scoped>
.completion-card {
  max-width: 600px;
  margin: 40px auto;
  text-align: center;
  border: 2px solid var(--td-success-color);
  box-shadow: 0 8px 24px rgba(103, 194, 58, 0.15);
}

.completion-content {
  padding: 40px 30px;
}

.completion-icon {
  margin-bottom: 20px;
}

.success-icon {
  font-size: 48px;
  color: var(--td-success-color);
}

.completion-title {
  color: var(--td-success-color);
  font-size: 24px;
  font-weight: 600;
  margin: 0 0 30px 0;
  line-height: 1.4;
}

.completion-stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: 20px;
  margin-bottom: 30px;
  padding: 20px;
  background: var(--td-bg-color-container-hover);
  border-radius: var(--td-radius-default);
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.stat-label {
  font-size: 14px;
  color: var(--td-text-color-secondary);
  font-weight: 500;
}

.stat-value {
  font-size: 20px;
  font-weight: 600;
  color: var(--td-text-color-primary);
}

.stat-value.correct {
  color: var(--td-success-color);
}

.stat-value.wrong {
  color: var(--td-error-color);
}

.stat-value.score {
  color: var(--td-brand-color);
}

.completion-actions {
  display: flex;
  justify-content: center;
  gap: 15px;
  flex-wrap: wrap;
  margin-bottom: 20px;
}

.completion-actions .t-button {
  min-width: 120px;
}

.encouragement-section {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid var(--td-border-level-1-color);
}

.encouragement-text {
  color: var(--td-text-color-secondary);
  font-size: 14px;
  line-height: 1.6;
  margin: 0;
  font-style: italic;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .completion-card {
    max-width: 95%;
    margin: 20px auto;
  }
  
  .completion-content {
    padding: 25px 20px;
  }
  
  .completion-title {
    font-size: 20px;
  }
  
  .completion-stats {
    grid-template-columns: repeat(2, 1fr);
    gap: 15px;
    padding: 15px;
  }
  
  .completion-actions {
    flex-direction: column;
    align-items: center;
  }
  
  .completion-actions .t-button {
    width: 100%;
    max-width: 200px;
  }
}

@media (max-width: 480px) {
  .completion-content {
    padding: 20px 15px;
  }
  
  .completion-title {
    font-size: 18px;
  }
  
  .completion-stats {
    grid-template-columns: 1fr;
    gap: 12px;
  }
  
  .stat-value {
    font-size: 18px;
  }
  
  .stat-label {
    font-size: 13px;
  }
  
  .success-icon {
    font-size: 40px;
  }
}
</style>