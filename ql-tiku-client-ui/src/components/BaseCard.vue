<template>
  <t-card 
    :class="['base-card', cardClass]"
    :style="cardStyle"
    v-bind="$attrs"
  >
    <!-- 卡片头部 -->
    <template v-if="$slots.header || $slots['header-left'] || $slots['header-right'] || title" #header>
      <div class="base-card-header">
        <div class="header-left">
          <t-icon v-if="icon" :name="icon" class="header-icon" />
          <span v-if="title" class="header-title">{{ title }}</span>
          <slot name="header-left" />
        </div>
        <div class="header-right">
          <slot name="header-right" />
        </div>
        <!-- 直接渲染 header slot 内容 -->
        <slot name="header" />
      </div>
    </template>

    <!-- 卡片内容 -->
    <div class="base-card-content">
      <slot />
    </div>

    <!-- 卡片底部 -->
    <template v-if="$slots.footer" #footer>
      <div class="base-card-footer">
        <slot name="footer" />
      </div>
    </template>
  </t-card>
</template>

<script setup>
import { computed } from 'vue'

// Props
const props = defineProps({
  // 卡片标题
  title: {
    type: String,
    default: ''
  },
  // 卡片图标
  icon: {
    type: String,
    default: ''
  },
  // 卡片主题
  theme: {
    type: String,
    default: 'default',
    validator: (value) => ['default', 'primary', 'success', 'warning', 'danger'].includes(value)
  },
  // 卡片大小
  size: {
    type: String,
    default: 'medium',
    validator: (value) => ['small', 'medium', 'large'].includes(value)
  },
  // 是否显示阴影
  shadow: {
    type: Boolean,
    default: true
  },
  // 是否可折叠
  collapsible: {
    type: Boolean,
    default: false
  },
  // 是否默认折叠
  collapsed: {
    type: Boolean,
    default: false
  },
  // 自定义样式类
  cardClass: {
    type: [String, Array, Object],
    default: ''
  },
  // 自定义样式
  cardStyle: {
    type: Object,
    default: () => ({})
  }
})

// Computed
const cardClasses = computed(() => {
  return [
    'base-card',
    `base-card--${props.theme}`,
    `base-card--${props.size}`,
    {
      'base-card--shadow': props.shadow,
      'base-card--collapsible': props.collapsible,
      'base-card--collapsed': props.collapsed
    },
    props.cardClass
  ]
})
</script>

<style scoped>
.base-card {
  margin-bottom: 20px;
  border-radius: 8px;
  transition: all 0.3s ease;
}

.base-card--shadow {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.base-card--shadow:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
}

/* 主题样式 */
.base-card--primary {
  border-left: 4px solid #0052d9;
}

.base-card--success {
  border-left: 4px solid #00a870;
}

.base-card--warning {
  border-left: 4px solid #ed7b2f;
}

.base-card--danger {
  border-left: 4px solid #e34d59;
}

/* 大小样式 */
.base-card--small {
  font-size: 12px;
}

.base-card--small .base-card-content {
  padding: 12px;
}

.base-card--medium {
  font-size: 14px;
}

.base-card--medium .base-card-content {
  padding: 16px;
}

.base-card--large {
  font-size: 16px;
}

.base-card--large .base-card-content {
  padding: 20px;
}

/* 头部样式 */
.base-card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 0;
  position: relative;
  min-height: 40px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  padding-right: 120px; /* 为右侧按钮留出空间 */
}

.header-icon {
  font-size: 18px;
  color: #0052d9;
}

.header-title {
  font-weight: 600;
  font-size: 16px;
  color: #333;
}

.header-right {
  position: absolute;
  top: 0;
  right: 0;
  display: flex;
  align-items: center;
  gap: 8px;
  z-index: 10;
}

/* 内容样式 */
.base-card-content {
  line-height: 1.6;
}

/* 底部样式 */
.base-card-footer {
  padding: 0;
  border-top: 1px solid #e7e7e7;
  margin-top: 16px;
  padding-top: 16px;
}

/* 折叠样式 */
.base-card--collapsible .base-card-header {
  cursor: pointer;
  user-select: none;
}

.base-card--collapsed .base-card-content {
  display: none;
}

.base-card--collapsed .base-card-footer {
  display: none;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .base-card {
    margin-bottom: 16px;
  }
  
  .base-card-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
  
  .header-left,
  .header-right {
    width: 100%;
    justify-content: space-between;
  }
  
  .base-card--small .base-card-content,
  .base-card--medium .base-card-content,
  .base-card--large .base-card-content {
    padding: 12px;
  }
}

@media (max-width: 480px) {
  .base-card {
    margin-bottom: 12px;
    border-radius: 6px;
  }
  
  .header-title {
    font-size: 14px;
  }
  
  .header-icon {
    font-size: 16px;
  }
}
</style>