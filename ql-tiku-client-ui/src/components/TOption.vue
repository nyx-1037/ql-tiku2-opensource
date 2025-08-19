<template>
  <!-- TOption组件不需要渲染任何内容，只是用来传递数据 -->
</template>

<script>
export default {
  name: 'TOption',
  props: {
    label: {
      type: [String, Number],
      required: true
    },
    value: {
      type: [String, Number, Object],
      required: true
    },
    disabled: {
      type: Boolean,
      default: false
    }
  }
}
</script>
