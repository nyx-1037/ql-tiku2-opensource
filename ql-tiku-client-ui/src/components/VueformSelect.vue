<template>
  <Multiselect
    :model-value="modelValue"
    @update:model-value="$emit('update:modelValue', $event)"
    :options="processedOptions"
    :placeholder="placeholder"
    :disabled="disabled"
    :can-clear="clearable"
    :searchable="searchable"
    :mode="mode"
    :value-prop="valueProp"
    :label="labelProp"
    :classes="customClasses"
    v-bind="$attrs"
    @change="$emit('change', $event)"
    @select="$emit('select', $event)"
    @deselect="$emit('deselect', $event)"
    @open="$emit('open', $event)"
    @close="$emit('close', $event)"
  />
</template>

<script>
import Multiselect from '@vueform/multiselect'

export default {
  name: 'VueformSelect',
  components: {
    Multiselect
  },
  inheritAttrs: false,
  emits: ['update:modelValue', 'change', 'select', 'deselect', 'open', 'close'],
  props: {
    modelValue: {
      type: [String, Number, Array, Object],
      default: null
    },
    options: {
      type: Array,
      default: () => []
    },
    placeholder: {
      type: String,
      default: ''
    },
    disabled: {
      type: Boolean,
      default: false
    },
    clearable: {
      type: Boolean,
      default: false
    },
    searchable: {
      type: Boolean,
      default: false
    },
    multiple: {
      type: Boolean,
      default: false
    },
    // 自定义属性映射
    valueProp: {
      type: String,
      default: 'value'
    },
    labelProp: {
      type: String,
      default: 'label'
    },
    // 自定义样式类名
    dropdownClass: {
      type: String,
      default: ''
    }
  },
  computed: {
    mode() {
      return this.multiple ? 'multiple' : 'single'
    },
    processedOptions() {
      // 如果options是简单数组，转换为对象数组
      if (this.options.length > 0 && typeof this.options[0] === 'string') {
        return this.options.map(option => ({
          [this.valueProp]: option,
          [this.labelProp]: option
        }))
      }
      return this.options
    },
    customClasses() {
      const baseClasses = {
        container: 'practice-multiselect-container',
        dropdown: `practice-select-dropdown ${this.dropdownClass}`
      }
      
      return baseClasses
    }
  }
}
</script>

<style src="@vueform/multiselect/themes/default.css"></style>

<style scoped>
/* 参考Analytics页面的正确样式实现 */
:deep(.practice-multiselect-container) {
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  min-height: 32px;
  background: white;
  transition: all 0.2s;
  position: relative;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  box-sizing: border-box;
}

:deep(.practice-multiselect-container:hover) {
  border-color: #4dabf7;
}

:deep(.practice-multiselect-container.is-active) {
  border-color: #0052d9;
  box-shadow: 0 0 0 2px rgba(0, 82, 217, 0.1);
}

:deep(.practice-select-dropdown) {
  border: 1px solid #e6e6e6;
  border-radius: 6px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  z-index: 99999 !important;
  background: white;
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  max-height: 200px;
  overflow-y: auto;
}

:deep(.practice-select-dropdown.is-hidden) {
  display: none !important;
}

/* 确保 Practice 页面的 multiselect 基础样式正确 */
:deep(.practice-multiselect-container .multiselect) {
  min-height: 32px;
  height: 32px;
  width: 100%;
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif !important;
  font-size: 14px !important;
  line-height: 1.5 !important;
}

:deep(.practice-multiselect-container .multiselect-single-label) {
  padding-left: 12px;
  padding-right: 40px;
  line-height: 30px;
}

:deep(.practice-multiselect-container .multiselect-placeholder) {
  padding-left: 12px;
  line-height: 30px;
  color: #bbb;
}

:deep(.practice-multiselect-container .multiselect-caret) {
  margin-right: 12px;
}

/* 选项样式 */
:deep(.multiselect-option) {
  padding: 8px 12px;
  background: white;
  color: #333;
  cursor: pointer;
  transition: background-color 0.2s;
}

:deep(.multiselect-option:hover),
:deep(.multiselect-option.is-pointed) {
  background-color: #f3f3f3;
}

:deep(.multiselect-option.is-selected) {
  background-color: #0052d9;
  color: white;
}

/* 修复可能的遮挡问题 */
:deep(.multiselect-wrapper) {
  position: relative;
}

:deep(.multiselect-caret) {
  z-index: 10;
}

:deep(.multiselect-clear) {
  z-index: 10;
}
</style>