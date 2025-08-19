<template>
  <div class="loading-states">
    <!-- 初始加载状态 -->
    <div v-if="initialLoading" class="loading-container initial-loading">
      <div class="loading-content">
        <t-loading size="large" :loading="true" text="正在加载数据..." />
        <div class="loading-tips">
          <p>首次加载可能需要几秒钟</p>
          <p v-if="showProgress">{{ loadingProgress }}%</p>
        </div>
      </div>
    </div>
    
    <!-- 刷新加载状态 -->
    <div v-if="refreshing" class="loading-container refresh-loading">
      <div class="refresh-indicator">
        <t-loading size="small" :loading="true" />
        <span class="refresh-text">正在刷新...</span>
      </div>
    </div>
    
    <!-- 加载更多状态 -->
    <div v-if="loadingMore" class="loading-container load-more">
      <t-loading size="medium" :loading="true" text="加载更多..." />
    </div>
    
    <!-- 网络错误状态 -->
    <div v-if="networkError" class="error-container network-error">
      <div class="error-content">
        <t-icon name="wifi-off" size="48px" class="error-icon" />
        <h3>网络连接失败</h3>
        <p>请检查网络设置后重试</p>
        <div class="error-actions">
          <t-button theme="primary" @click="$emit('retry')">
            重试
          </t-button>
          <t-button theme="default" @click="$emit('refresh')">
            刷新页面
          </t-button>
        </div>
      </div>
    </div>
    
    <!-- 超时错误状态 -->
    <div v-if="timeoutError" class="error-container timeout-error">
      <div class="error-content">
        <t-icon name="time" size="48px" class="error-icon" />
        <h3>请求超时</h3>
        <p>服务器响应时间过长，请稍后重试</p>
        <div class="error-actions">
          <t-button theme="primary" @click="$emit('retry')">
            重试
          </t-button>
        </div>
      </div>
    </div>
    
    <!-- 服务器错误状态 -->
    <div v-if="serverError" class="error-container server-error">
      <div class="error-content">
        <t-icon name="server" size="48px" class="error-icon" />
        <h3>服务器错误</h3>
        <p>{{ errorMessage || '服务器暂时无法响应，请稍后重试' }}</p>
        <div class="error-actions">
          <t-button theme="primary" @click="$emit('retry')">
            重试
          </t-button>
          <t-button theme="default" @click="$emit('report-error')">
            报告问题
          </t-button>
        </div>
      </div>
    </div>
    
    <!-- 空数据状态 -->
    <div v-if="isEmpty && !isLoading" class="empty-container">
      <div class="empty-content">
        <t-icon name="inbox" size="64px" class="empty-icon" />
        <h3>{{ emptyTitle || '暂无数据' }}</h3>
        <p>{{ emptyDescription || '当前没有可显示的内容' }}</p>
        <div v-if="showEmptyAction" class="empty-actions">
          <t-button theme="primary" @click="$emit('empty-action')">
            {{ emptyActionText || '刷新数据' }}
          </t-button>
        </div>
      </div>
    </div>
    
    <!-- 缓存状态指示器 -->
    <div v-if="showCacheStatus" class="cache-status" :class="cacheStatusClass">
      <t-icon :name="cacheStatusIcon" size="16px" />
      <span>{{ cacheStatusText }}</span>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { Loading as TLoading, Button as TButton, Icon as TIcon } from 'tdesign-vue-next'

const props = defineProps({
  // 加载状态
  initialLoading: {
    type: Boolean,
    default: false
  },
  refreshing: {
    type: Boolean,
    default: false
  },
  loadingMore: {
    type: Boolean,
    default: false
  },
  
  // 错误状态
  networkError: {
    type: Boolean,
    default: false
  },
  timeoutError: {
    type: Boolean,
    default: false
  },
  serverError: {
    type: Boolean,
    default: false
  },
  errorMessage: {
    type: String,
    default: ''
  },
  
  // 空数据状态
  isEmpty: {
    type: Boolean,
    default: false
  },
  emptyTitle: {
    type: String,
    default: ''
  },
  emptyDescription: {
    type: String,
    default: ''
  },
  showEmptyAction: {
    type: Boolean,
    default: true
  },
  emptyActionText: {
    type: String,
    default: ''
  },
  
  // 进度相关
  showProgress: {
    type: Boolean,
    default: false
  },
  loadingProgress: {
    type: Number,
    default: 0
  },
  
  // 缓存状态
  showCacheStatus: {
    type: Boolean,
    default: false
  },
  cacheStatus: {
    type: String,
    default: 'fresh', // fresh, cached, updating, error
    validator: (value) => ['fresh', 'cached', 'updating', 'error'].includes(value)
  }
})

const emit = defineEmits([
  'retry',
  'refresh',
  'report-error',
  'empty-action'
])

// 计算属性
const isLoading = computed(() => {
  return props.initialLoading || props.refreshing || props.loadingMore
})

const cacheStatusClass = computed(() => {
  return `cache-status--${props.cacheStatus}`
})

const cacheStatusIcon = computed(() => {
  const icons = {
    fresh: 'refresh',
    cached: 'time',
    updating: 'loading',
    error: 'error-circle'
  }
  return icons[props.cacheStatus] || 'refresh'
})

const cacheStatusText = computed(() => {
  const texts = {
    fresh: '数据最新',
    cached: '缓存数据',
    updating: '更新中',
    error: '缓存错误'
  }
  return texts[props.cacheStatus] || '未知状态'
})
</script>

<style scoped>
.loading-states {
  position: relative;
  width: 100%;
  min-height: 200px;
}

/* 加载状态样式 */
.loading-container {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
}

.initial-loading {
  min-height: 300px;
  background: var(--td-bg-color-container);
  border-radius: 8px;
}

.loading-content {
  text-align: center;
}

.loading-tips {
  margin-top: 16px;
  color: var(--td-text-color-secondary);
  font-size: 14px;
}

.loading-tips p {
  margin: 4px 0;
}

.refresh-loading {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(2px);
  z-index: 10;
  min-height: 60px;
}

.refresh-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: var(--td-bg-color-container);
  border-radius: 6px;
  box-shadow: var(--td-shadow-2);
}

.refresh-text {
  font-size: 14px;
  color: var(--td-text-color-secondary);
}

.load-more {
  padding: 20px;
  border-top: 1px solid var(--td-border-level-1-color);
}

/* 错误状态样式 */
.error-container {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 300px;
  padding: 40px 20px;
}

.error-content {
  text-align: center;
  max-width: 400px;
}

.error-icon {
  color: var(--td-text-color-placeholder);
  margin-bottom: 16px;
}

.error-content h3 {
  margin: 0 0 8px 0;
  color: var(--td-text-color-primary);
  font-size: 18px;
  font-weight: 500;
}

.error-content p {
  margin: 0 0 24px 0;
  color: var(--td-text-color-secondary);
  font-size: 14px;
  line-height: 1.5;
}

.error-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}

.network-error .error-icon {
  color: var(--td-warning-color);
}

.timeout-error .error-icon {
  color: var(--td-warning-color);
}

.server-error .error-icon {
  color: var(--td-error-color);
}

/* 空数据状态样式 */
.empty-container {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 300px;
  padding: 40px 20px;
}

.empty-content {
  text-align: center;
  max-width: 400px;
}

.empty-icon {
  color: var(--td-text-color-placeholder);
  margin-bottom: 16px;
}

.empty-content h3 {
  margin: 0 0 8px 0;
  color: var(--td-text-color-primary);
  font-size: 18px;
  font-weight: 500;
}

.empty-content p {
  margin: 0 0 24px 0;
  color: var(--td-text-color-secondary);
  font-size: 14px;
  line-height: 1.5;
}

.empty-actions {
  display: flex;
  justify-content: center;
}

/* 缓存状态指示器 */
.cache-status {
  position: absolute;
  top: 12px;
  right: 12px;
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  background: var(--td-bg-color-container);
  border-radius: 4px;
  font-size: 12px;
  z-index: 5;
}

.cache-status--fresh {
  color: var(--td-success-color);
  border: 1px solid var(--td-success-color-light);
}

.cache-status--cached {
  color: var(--td-warning-color);
  border: 1px solid var(--td-warning-color-light);
}

.cache-status--updating {
  color: var(--td-brand-color);
  border: 1px solid var(--td-brand-color-light);
}

.cache-status--error {
  color: var(--td-error-color);
  border: 1px solid var(--td-error-color-light);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .loading-container,
  .error-container,
  .empty-container {
    padding: 20px 16px;
    min-height: 200px;
  }
  
  .error-actions,
  .empty-actions {
    flex-direction: column;
    align-items: center;
  }
  
  .cache-status {
    position: static;
    margin-bottom: 12px;
    align-self: flex-end;
  }
}
</style>