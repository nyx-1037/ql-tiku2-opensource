<template>
  <div class="custom-pagination">
    <div class="pagination-info">
      <span>共 {{ total }} 条数据</span>
    </div>
    
    <div class="pagination-controls">
      <!-- 页面大小选择 -->
      <div class="page-size-selector">
        <Multiselect
          v-model="currentPageSize"
          :options="pageSizeOptions"
          value-prop="value"
          label="label"
          placeholder="选择每页条数"
          :can-clear="false"
          :searchable="false"
          :classes="{
            container: 'pagination-multiselect-container',
            dropdown: 'pagination-select-dropdown'
          }"
          @change="handlePageSizeChange"
        />
        <span class="page-size-label">条/页</span>
      </div>
      
      <!-- 分页按钮 -->
      <div class="pagination-buttons">
        <t-button
          size="small"
          :disabled="currentPage <= 1"
          @click="handlePageChange(currentPage - 1)"
        >
          上一页
        </t-button>
        
        <div class="page-numbers">
          <t-button
            v-for="page in visiblePages"
            :key="page"
            size="small"
            :theme="page === currentPage ? 'primary' : 'default'"
            @click="handlePageChange(page)"
          >
            {{ page }}
          </t-button>
        </div>
        
        <t-button
          size="small"
          :disabled="currentPage >= totalPages"
          @click="handlePageChange(currentPage + 1)"
        >
          下一页
        </t-button>
      </div>
      
      <!-- 跳转 -->
      <div class="page-jumper">
        <span>跳至</span>
        <t-input
          v-model="jumpPage"
          size="small"
          style="width: 60px; margin: 0 8px;"
          @keyup.enter="handleJump"
        />
        <span>页</span>
        <t-button size="small" @click="handleJump">确定</t-button>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, computed, watch } from 'vue'
import Multiselect from '@vueform/multiselect'

export default {
  name: 'CustomPagination',
  components: {
    Multiselect
  },
  props: {
    current: {
      type: Number,
      default: 1
    },
    pageSize: {
      type: Number,
      default: 10
    },
    total: {
      type: Number,
      default: 0
    },
    pageSizeOptions: {
      type: Array,
      default: () => [
        { value: 10, label: '10' },
        { value: 20, label: '20' },
        { value: 50, label: '50' },
        { value: 100, label: '100' }
      ]
    }
  },
  emits: ['current-change', 'page-size-change'],
  setup(props, { emit }) {
    const currentPage = ref(props.current)
    const currentPageSize = ref(props.pageSize)
    const jumpPage = ref('')

    // 计算总页数
    const totalPages = computed(() => {
      return Math.ceil(props.total / currentPageSize.value)
    })

    // 计算可见页码
    const visiblePages = computed(() => {
      const pages = []
      const total = totalPages.value
      const current = currentPage.value
      
      if (total <= 7) {
        for (let i = 1; i <= total; i++) {
          pages.push(i)
        }
      } else {
        if (current <= 4) {
          for (let i = 1; i <= 5; i++) {
            pages.push(i)
          }
          pages.push('...')
          pages.push(total)
        } else if (current >= total - 3) {
          pages.push(1)
          pages.push('...')
          for (let i = total - 4; i <= total; i++) {
            pages.push(i)
          }
        } else {
          pages.push(1)
          pages.push('...')
          for (let i = current - 1; i <= current + 1; i++) {
            pages.push(i)
          }
          pages.push('...')
          pages.push(total)
        }
      }
      
      return pages.filter(page => page !== '...' || pages.indexOf(page) === pages.lastIndexOf(page))
    })

    // 监听 props 变化
    watch(() => props.current, (newVal) => {
      currentPage.value = newVal
    })

    watch(() => props.pageSize, (newVal) => {
      currentPageSize.value = newVal
    })

    // 处理页码变化
    const handlePageChange = (page) => {
      if (page >= 1 && page <= totalPages.value && page !== currentPage.value) {
        currentPage.value = page
        emit('current-change', page)
      }
    }

    // 处理页面大小变化
    const handlePageSizeChange = (size) => {
      currentPageSize.value = size
      emit('page-size-change', size)
    }

    // 处理跳转
    const handleJump = () => {
      const page = parseInt(jumpPage.value)
      if (page >= 1 && page <= totalPages.value) {
        handlePageChange(page)
        jumpPage.value = ''
      }
    }

    return {
      currentPage,
      currentPageSize,
      jumpPage,
      totalPages,
      visiblePages,
      handlePageChange,
      handlePageSizeChange,
      handleJump
    }
  }
}
</script>

<style scoped>
.custom-pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 0;
  border-top: 1px solid #e6e6e6;
  margin-top: 16px;
}

.pagination-info {
  color: #666;
  font-size: 14px;
}

.pagination-controls {
  display: flex;
  align-items: center;
  gap: 16px;
}

.page-size-selector {
  display: flex;
  align-items: center;
  gap: 8px;
}

.page-size-label {
  color: #666;
  font-size: 14px;
  white-space: nowrap;
}

.pagination-buttons {
  display: flex;
  align-items: center;
  gap: 8px;
}

.page-numbers {
  display: flex;
  gap: 4px;
}

.page-jumper {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #666;
  font-size: 14px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .custom-pagination {
    flex-direction: column;
    gap: 12px;
    align-items: stretch;
  }
  
  .pagination-controls {
    flex-direction: column;
    gap: 12px;
  }
  
  .pagination-info {
    text-align: center;
  }
}
</style>

<style>
/* Pagination Multiselect 自定义样式 */
.pagination-multiselect-container {
  border: 1px solid #d9d9d9 !important;
  border-radius: 6px !important;
  min-height: 32px !important;
  width: 80px !important;
  background: white !important;
  transition: all 0.2s !important;
  position: relative !important;
  cursor: pointer !important;
  display: block !important;
  box-sizing: border-box !important;
}

.pagination-multiselect-container:hover {
  border-color: #4dabf7 !important;
}

.pagination-multiselect-container.is-active {
  border-color: #0052d9 !important;
  box-shadow: 0 0 0 2px rgba(0, 82, 217, 0.1) !important;
}

.pagination-select-dropdown {
  border: 1px solid #e6e6e6 !important;
  border-radius: 6px !important;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1) !important;
  z-index: 99999 !important;
  background: white !important;
  position: absolute !important;
  top: 100% !important;
  left: 0 !important;
  right: 0 !important;
  max-height: 200px !important;
  overflow-y: auto !important;
}

.pagination-select-dropdown.is-hidden {
  display: none !important;
}

/* 确保下拉框默认隐藏 */
.pagination-multiselect-container .multiselect-dropdown {
  position: absolute !important;
  left: 0 !important;
  right: 0 !important;
  top: 100% !important;
  transform: translateY(0) !important;
  border: 1px solid #d1d5db !important;
  margin-top: -1px !important;
  overflow-y: auto !important;
  z-index: 99999 !important;
  background-color: white !important;
  display: flex !important;
  flex-direction: column !important;
  border-radius: 0 0 6px 6px !important;
  max-height: 160px !important;
}

.pagination-multiselect-container .multiselect-dropdown.is-hidden {
  display: none !important;
}

/* 确保下拉选项正确显示 */
.pagination-multiselect-container .multiselect-options {
  flex-direction: column !important;
  padding: 0 !important;
  margin: 0 !important;
  list-style: none !important;
  display: flex !important;
}

.pagination-multiselect-container .multiselect-option {
  display: flex !important;
  align-items: center !important;
  justify-content: flex-start !important;
  box-sizing: border-box !important;
  text-align: left !important;
  cursor: pointer !important;
  line-height: 1.375 !important;
  padding: 8px 12px !important;
  color: #374151 !important;
  text-decoration: none !important;
  white-space: nowrap !important;
  overflow: hidden !important;
  text-overflow: ellipsis !important;
}

.pagination-multiselect-container .multiselect-option.is-pointed {
  background-color: #f3f4f6 !important;
  color: #1f2937 !important;
}

.pagination-multiselect-container .multiselect-option.is-selected {
  background-color: #0052d9 !important;
  color: white !important;
}

.pagination-multiselect-container .multiselect {
  min-height: 32px !important;
  height: 32px !important;
  width: 100% !important;
  position: relative !important;
  margin: 0 !important;
  display: block !important;
  box-sizing: border-box !important;
  cursor: pointer !important;
  outline: none !important;
  border: none !important;
  background: transparent !important;
}

.pagination-multiselect-container .multiselect-single-label {
  display: flex !important;
  align-items: center !important;
  height: 100% !important;
  position: absolute !important;
  left: 0 !important;
  top: 0 !important;
  pointer-events: none !important;
  background: transparent !important;
  line-height: 1.375 !important;
  padding-left: 12px !important;
  padding-right: 30px !important;
  box-sizing: border-box !important;
  max-width: 100% !important;
  white-space: nowrap !important;
  overflow: hidden !important;
  text-overflow: ellipsis !important;
}

.pagination-multiselect-container .multiselect-caret {
  background-image: url("data:image/svg+xml,%3csvg viewBox='0 0 16 16' fill='%23999' xmlns='http://www.w3.org/2000/svg'%3e%3cpath d='m7.247 4.86-4.796 5.481c-.566.647-.106 1.659.753 1.659h9.592a1 1 0 0 0 .753-1.659l-4.796-5.48a1 1 0 0 0-1.506 0z'/%3e%3c/svg%3e") !important;
  background-position: center !important;
  background-repeat: no-repeat !important;
  width: 10px !important;
  height: 16px !important;
  position: relative !important;
  z-index: 10 !important;
  flex-shrink: 0 !important;
  flex-grow: 0 !important;
  pointer-events: none !important;
  margin-right: 8px !important;
  transition: transform 0.2s !important;
}

.pagination-multiselect-container .multiselect-caret.is-open {
  transform: rotate(180deg) !important;
}

/* 重要：确保单选标签文本正确显示 */
.pagination-multiselect-container .multiselect-single-label-text {
  overflow: hidden !important;
  display: block !important;
  white-space: nowrap !important;
  max-width: 100% !important;
  text-overflow: ellipsis !important;
}

/* 修复可能的样式冲突 */
.pagination-multiselect-container .multiselect * {
  box-sizing: border-box !important;
}

/* 确保 wrapper 正确包含内容 */
.pagination-multiselect-container .multiselect-wrapper {
  position: relative !important;
  margin: 0 !important;
  width: 100% !important;
  display: block !important;
  box-sizing: border-box !important;
  cursor: pointer !important;
  outline: none !important;
}

/* 确保占位符正确显示 */
.pagination-multiselect-container .multiselect-placeholder {
  display: flex !important;
  align-items: center !important;
  height: 100% !important;
  position: absolute !important;
  left: 0 !important;
  top: 0 !important;
  pointer-events: none !important;
  background: transparent !important;
  line-height: 1.375 !important;
  padding-left: 12px !important;
  color: #bbb !important;
  box-sizing: border-box !important;
  white-space: nowrap !important;
  overflow: hidden !important;
  text-overflow: ellipsis !important;
}
</style>

<style src="@vueform/multiselect/themes/default.css">
</style>
