<template>
  <Multiselect
    :model-value="modelValue"
    @update:model-value="handleUpdate"
    :options="finalOptions"
    :placeholder="placeholder"
    :disabled="disabled"
    :can-clear="clearable"
    :searchable="searchable"
    :mode="mode"
    :value-prop="valueProp"
    :label="labelProp"
    :classes="customClasses"
    v-bind="$attrs"
    @change="handleChange"
    @select="handleSelect"
    @deselect="handleDeselect"
    @open="handleOpen"
    @close="handleClose"
  />
</template>

<script>
import Multiselect from '@vueform/multiselect'

export default {
  name: 'CustomSelect',
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
    // TDesign兼容属性
    popupProps: {
      type: Object,
      default: () => ({})
    },
    // 自定义属性映射
    valueProp: {
      type: String,
      default: 'value'
    },
    labelProp: {
      type: String,
      default: 'label'
    }
  },
  computed: {
    mode() {
      return this.multiple ? 'multiple' : 'single'
    },
    finalOptions() {
      // 优先使用props中的options
      if (this.options && this.options.length > 0) {
        return this.processedOptions
      }

      // 如果没有options prop，则从slot中解析t-option
      return this.slotOptions
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
    slotOptions() {
      // 从默认插槽中解析t-option组件
      const options = []
      if (this.$slots.default) {
        const vnodes = this.$slots.default()
        vnodes.forEach(vnode => {
          if (vnode.type?.name === 'TOption' || vnode.type?.name === 't-option') {
            const props = vnode.props || {}
            options.push({
              [this.valueProp]: props.value,
              [this.labelProp]: props.label || props.value
            })
          }
        })
      }
      return options
    },
    customClasses() {
      const baseClasses = {
        container: 'custom-select-container',
        dropdown: 'custom-select-dropdown',
        option: 'custom-select-option',
        optionPointed: 'custom-select-option-pointed',
        optionSelected: 'custom-select-option-selected'
      }

      // 合并TDesign的popupProps中的overlayClassName
      if (this.popupProps?.overlayClassName) {
        baseClasses.dropdown += ` ${this.popupProps.overlayClassName}`
      }

      return baseClasses
    }
  },
  methods: {
    handleUpdate(value) {
      this.$emit('update:modelValue', value)
    },
    handleChange(value) {
      this.$emit('change', value)
    },
    handleSelect(value, option) {
      this.$emit('select', value, option)
    },
    handleDeselect(value, option) {
      this.$emit('deselect', value, option)
    },
    handleOpen() {
      this.$emit('open')
    },
    handleClose() {
      this.$emit('close')
    }
  }
}
</script>

<style src="@vueform/multiselect/themes/default.css"></style>

<style scoped>
/* 自定义样式以匹配TDesign外观 */
.custom-select-container {
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  transition: all 0.2s;
}

.custom-select-container:hover {
  border-color: #4dabf7;
}

.custom-select-container.is-active {
  border-color: #0052d9;
  box-shadow: 0 0 0 2px rgba(0, 82, 217, 0.1);
}

.custom-select-dropdown {
  border: 1px solid #e6e6e6;
  border-radius: 6px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  z-index: 99999 !important;
}

.custom-select-option {
  padding: 8px 12px;
  transition: background-color 0.2s;
}

.custom-select-option-pointed {
  background-color: #f3f3f3;
}

.custom-select-option-selected {
  background-color: #0052d9;
  color: white;
}

/* 确保下拉框层级 */
:deep(.multiselect-dropdown) {
  z-index: 99999 !important;
}
</style>
