import CustomSelect from './CustomSelect.vue'
import TOption from './TOption.vue'

export default {
  install(app) {
    // 注册为全局组件，可以直接替换t-select
    app.component('TSelect', CustomSelect)
    app.component('t-select', CustomSelect)
    app.component('CustomSelect', CustomSelect)

    // 注册TOption组件
    app.component('TOption', TOption)
    app.component('t-option', TOption)
  }
}

export { CustomSelect, TOption }
