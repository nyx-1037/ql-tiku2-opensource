import { createPinia } from 'pinia'
import { createPersistedState } from 'pinia-plugin-persistedstate'

const pinia = createPinia()

// 配置持久化插件，只对特定store启用
pinia.use(createPersistedState({
  // 全局配置
  storage: localStorage,
  // 只对指定的store启用持久化
  auto: false,
  // 序列化配置
  serializer: {
    serialize: JSON.stringify,
    deserialize: JSON.parse,
  },
  // 错误处理
  beforeRestore: (context) => {
    console.log('🔧 Pinia持久化: 开始恢复状态', context.store.$id)
  },
  afterRestore: (context) => {
    console.log('🔧 Pinia持久化: 状态恢复完成', context.store.$id)
  }
}))

export default pinia
