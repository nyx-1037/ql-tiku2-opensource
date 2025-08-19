<template>
  <div class="question-bank-detail-container">
    <!-- 返回按钮 -->
    <div class="back-button-container">
      <t-button
        theme="default"
        variant="outline"
        @click="goBack"
        class="back-button"
        size="medium"
      >
        <template #icon>
          <span class="back-icon">←</span>
        </template>
        返回上一页
      </t-button>
    </div>
    
    <t-card :title="subjectName ? `题库详情 - ${subjectName}` : '题库详情'" class="question-list-card">
      <template #actions>
        <div class="search-filter-container">
          <t-input
            v-model="searchKeyword"
            placeholder="搜索题目内容"
            clearable
            @clear="handleSearch"
            @keyup.enter="handleSearch"
            style="width: 300px; margin-right: 16px;"
          >
            <template #suffix-icon>
              <SearchIcon @click="handleSearch" />
            </template>
          </t-input>
          
          <VueformSelect
            v-model="selectedType"
            :options="questionTypes"
            placeholder="题目类型"
            clearable
            searchable
            style="width: 150px; margin-right: 16px;"
            dropdownClass="question-type-dropdown"
            @change="handleFilterChange"
          />
          
          <VueformSelect
            v-model="selectedDifficulty"
            :options="difficulties"
            placeholder="难度等级"
            clearable
            searchable
            style="width: 150px;"
            dropdownClass="difficulty-dropdown"
            @change="handleFilterChange"
          />
        </div>
      </template>
      
      <t-table
        :data="questions"
        :columns="columns"
        :loading="loading"
        row-key="id"
        hover
        stripe
        empty-text="暂无题目数据"
      >
        <template #questionType="{ row }">
          <t-tag :theme="getQuestionTypeTheme(row.questionType)">
            {{ getQuestionTypeLabel(row.questionType) }}
          </t-tag>
        </template>
        
        <template #difficulty="{ row }">
          <t-tag :theme="getDifficultyTheme(row.difficulty)">
            {{ getDifficultyLabel(row.difficulty) }}
          </t-tag>
        </template>
        
        <template #content="{ row }">
          <div class="question-content" v-html="formatContent(row.content)"></div>
        </template>
        
        <template #actions="{ row }">
          <t-button 
            theme="primary" 
            variant="text" 
            size="small"
            @click="showQuestionDetail(row)"
          >
            查看详情
          </t-button>
        </template>
      </t-table>
      
      <div class="pagination-container">
        <CustomPagination
          :current="currentPage"
          :pageSize="pageSize"
          :total="totalQuestions"
          @current-change="handlePageChange"
          @page-size-change="handlePageSizeChange"
        />
      </div>
    </t-card>

    <!-- 题目详情模态框 -->
    <t-dialog
      v-model:visible="detailDialogVisible"
      :header="detailDialogTitle"
      width="800px"
      :footer="false"
      placement="center"
      @close="closeDetailDialog"
    >
      <div v-if="selectedQuestion" class="question-detail-content">
        <!-- 基本信息 -->
        <div class="detail-section">
          <h4 class="section-title">基本信息</h4>
          <div class="info-grid">
            <div class="info-item">
              <span class="label">题目ID:</span>
              <span class="value">{{ selectedQuestion.id }}</span>
            </div>
            <div class="info-item">
              <span class="label">科目:</span>
              <span class="value">{{ selectedQuestion.subjectName || '未知科目' }}</span>
            </div>
            <div class="info-item">
              <span class="label">题目类型:</span>
              <t-tag :theme="getQuestionTypeTheme(selectedQuestion.questionType)">
                {{ getQuestionTypeLabel(selectedQuestion.questionType) }}
              </t-tag>
            </div>
            <div class="info-item">
              <span class="label">难度等级:</span>
              <t-tag :theme="getDifficultyTheme(selectedQuestion.difficulty)">
                {{ getDifficultyLabel(selectedQuestion.difficulty) }}
              </t-tag>
            </div>
            <div class="info-item">
              <span class="label">创建时间:</span>
              <span class="value">{{ formatDateTime(selectedQuestion.createTime) }}</span>
            </div>
            <div class="info-item">
              <span class="label">更新时间:</span>
              <span class="value">{{ formatDateTime(selectedQuestion.updateTime) }}</span>
            </div>
          </div>
        </div>

        <!-- 题目内容 -->
        <div class="detail-section">
          <h4 class="section-title">题目内容</h4>
          <div class="question-content-full" v-html="selectedQuestion.content"></div>
        </div>

        <!-- 题目图片 -->
        <div v-if="hasQuestionImages(selectedQuestion)" class="detail-section">
          <h4 class="section-title">题目图片</h4>
          <div class="question-images">
            <div 
              v-for="(image, index) in getQuestionImages(selectedQuestion)" 
              :key="index"
              class="image-item"
              @click="previewImage(image, index)"
            >
              <img 
                :src="formatImageUrl(image)" 
                :alt="`题目图片 ${index + 1}`"
                class="question-image"
                @error="handleImageError"
              />
              <div class="image-overlay">
                <span class="preview-text">点击放大</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 选项（根据题型显示不同内容） -->
        <div v-if="shouldShowOptions(selectedQuestion)" class="detail-section">
          <h4 class="section-title">{{ getOptionsTitle(selectedQuestion.questionType) }}</h4>
          
          <!-- 单选题/多选题选项 -->
          <div v-if="selectedQuestion.questionType === 0 || selectedQuestion.questionType === 1" class="options-list">
            <div 
              v-for="(option, index) in getOptionsArray(selectedQuestion.options)" 
              :key="index"
              class="option-item"
              :class="{ 'correct-option': isCorrectOption(option, selectedQuestion.correctAnswer) }"
            >
              <span class="option-label">{{ option.key || option.label || getOptionLabel(index) }}:</span>
              <span class="option-content" v-html="option.value || option.content || option"></span>
              <span v-if="isCorrectOption(option, selectedQuestion.correctAnswer)" class="correct-indicator">✓</span>
            </div>
          </div>
          
          <!-- 判断题选项 -->
          <div v-else-if="selectedQuestion.questionType === 2" class="options-list">
            <div class="option-item">
              <span class="option-label">A:</span>
              <span class="option-content">正确</span>
            </div>
            <div class="option-item">
              <span class="option-label">B:</span>
              <span class="option-content">错误</span>
            </div>
          </div>
          
          <!-- 简答题提示 -->
          <div v-else-if="selectedQuestion.questionType === 3" class="analysis-content">
            <t-alert theme="info" message="简答题：请在答题框中输入您的答案" />
          </div>
        </div>

        <!-- 正确答案 -->
        <div class="detail-section">
          <h4 class="section-title">正确答案</h4>
          <div class="correct-answer">
            <div v-if="selectedQuestion.questionType === 1">
              <!-- 多选题：显示多个正确答案 -->
              <t-tag 
                v-for="answer in formatMultiAnswer(selectedQuestion.correctAnswer)" 
                :key="answer"
                theme="success" 
                size="large"
                style="margin-right: 8px; margin-bottom: 4px;"
              >
                {{ answer }}
              </t-tag>
            </div>
            <div v-else-if="selectedQuestion.questionType === 2">
              <!-- 判断题：转换答案为中文 -->
              <t-tag theme="success" size="large">
                {{ formatJudgeAnswer(selectedQuestion.correctAnswer) }}
              </t-tag>
            </div>
            <div v-else-if="selectedQuestion.questionType === 0">
              <!-- 单选题：直接显示 -->
              <t-tag theme="success" size="large">{{ selectedQuestion.correctAnswer }}</t-tag>
            </div>
            <div v-else-if="selectedQuestion.questionType === 3">
              <!-- 简答题：显示完整答案内容 -->
              <div class="essay-answer">
                {{ selectedQuestion.correctAnswer }}
              </div>
            </div>
          </div>
        </div>

        <!-- 解析（如果有） -->
        <div v-if="selectedQuestion.analysis" class="detail-section">
          <h4 class="section-title">题目解析</h4>
          <div class="analysis-content" v-html="selectedQuestion.analysis"></div>
        </div>

        <!-- 知识点（如果有） -->
        <div v-if="selectedQuestion.knowledgePointList && selectedQuestion.knowledgePointList.length > 0" class="detail-section">
          <h4 class="section-title">知识点</h4>
          <div class="knowledge-points">
            <t-tag 
              v-for="point in selectedQuestion.knowledgePointList" 
              :key="point"
              theme="default"
              style="margin-right: 8px; margin-bottom: 8px;"
            >
              {{ point }}
            </t-tag>
          </div>
        </div>
      </div>
    </t-dialog>

    <!-- 图片预览模态框 -->
    <t-dialog
      v-model:visible="imagePreviewVisible"
      :header="`图片预览 - ${previewImageIndex + 1}/${previewImages.length}`"
      width="90%"
      :footer="false"
      placement="center"
      @close="closeImagePreview"
      class="image-preview-dialog"
    >
      <div class="image-preview-container">
        <div class="preview-navigation" v-if="previewImages.length > 1">
          <t-button 
            theme="default" 
            variant="outline" 
            size="small"
            @click="previousImage"
            :disabled="previewImageIndex === 0"
            class="nav-button"
          >
            <template #icon>←</template>
            上一张
          </t-button>
          <span class="image-counter">
            {{ previewImageIndex + 1 }} / {{ previewImages.length }}
          </span>
          <t-button 
            theme="default" 
            variant="outline" 
            size="small"
            @click="nextImage"
            :disabled="previewImageIndex === previewImages.length - 1"
            class="nav-button"
          >
            下一张
            <template #icon>→</template>
          </t-button>
        </div>
        
        <div class="image-wrapper">
          <img 
            :src="currentPreviewImage" 
            :alt="`预览图片 ${previewImageIndex + 1}`"
            class="preview-image"
            @click="closeImagePreview"
          />
        </div>
        
        <div class="image-actions">
          <t-button 
            theme="primary" 
            variant="outline" 
            size="small"
            @click="downloadImage(currentPreviewImage)"
          >
            <template #icon>⬇</template>
            下载图片
          </t-button>
          <t-button 
            theme="default" 
            variant="outline" 
            size="small"
            @click="closeImagePreview"
          >
            关闭
          </t-button>
        </div>
      </div>
    </t-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { SearchIcon } from 'tdesign-icons-vue-next'
import VueformSelect from '../components/VueformSelect.vue'
import CustomPagination from '../components/CustomPagination.vue'
import questionAPI from '../api/question'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const questions = ref([])
const searchKeyword = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const totalQuestions = ref(0)
const subjectId = ref(null)
const subjectName = ref('')
const questionTypes = ref([])
const difficulties = ref([])
const selectedType = ref(null)
const selectedDifficulty = ref(null)

// 详情模态框相关
const detailDialogVisible = ref(false)
const selectedQuestion = ref(null)
const detailDialogTitle = computed(() => {
  return selectedQuestion.value ? `题目详情 - ID: ${selectedQuestion.value.id}` : '题目详情'
})

// 图片预览相关
const imagePreviewVisible = ref(false)
const previewImages = ref([])
const previewImageIndex = ref(0)
const currentPreviewImage = computed(() => {
  return previewImages.value[previewImageIndex.value] || ''
})

// 表格列定义
const columns = [
  {
    colKey: 'id',
    title: 'ID',
    width: 80
  },
  {
    colKey: 'content',
    title: '题目内容',
    ellipsis: true,
    cell: 'content'
  },
  {
    colKey: 'questionType',
    title: '题目类型',
    width: 100,
    cell: 'questionType'
  },
  {
    colKey: 'difficulty',
    title: '难度等级',
    width: 100,
    cell: 'difficulty'
  },
  {
    colKey: 'correctAnswer',
    title: '正确答案',
    width: 120
  },
  {
    colKey: 'actions',
    title: '操作',
    width: 120,
    fixed: 'right',
    cell: 'actions'
  }
]

// 获取题目列表
const fetchQuestions = async () => {
  if (!subjectId.value) return;

  loading.value = true;
  try {
    // 一次性构建所有请求参数，确保清晰无误
    const params = {
      current: currentPage.value,
      size: pageSize.value,
      keyword: searchKeyword.value || '',
      subjectId: subjectId.value,
      questionType: selectedType.value,
      difficulty: selectedDifficulty.value,
    };

    // [调试日志] 打印发送到后端的参数
    console.log('Fetching questions with params:', JSON.stringify(params, null, 2));

    // 调用正确的分页查询接口
    const response = await questionAPI.getQuestionPage(params);

    if (response && response.records) {
      questions.value = response.records || [];
      totalQuestions.value = response.total || 0;
    } else {
      console.error('获取题目列表失败: 响应格式不正确', response);
      questions.value = [];
      totalQuestions.value = 0;
    }
  } catch (error) {
    console.error('获取题目列表失败:', error);
    questions.value = [];
    totalQuestions.value = 0;
  } finally {
    loading.value = false;
  }
};

// 获取题目类型列表
const fetchQuestionTypes = async () => {
  try {
    const response = await questionAPI.getQuestionTypes()
    questionTypes.value = response && Array.isArray(response) ? response : [
      { value: 0, label: '单选题' },
      { value: 1, label: '多选题' },
      { value: 2, label: '判断题' },
      { value: 3, label: '简答题' }
    ]
  } catch (error) {
    console.error('获取题目类型失败:', error)
    questionTypes.value = [
      { value: 0, label: '单选题' },
      { value: 1, label: '多选题' },
      { value: 2, label: '判断题' },
      { value: 3, label: '简答题' }
    ]
  }
}

// 获取难度等级列表
const fetchDifficulties = async () => {
  try {
    const response = await questionAPI.getDifficulties()
    difficulties.value = response && Array.isArray(response) ? response : [
      { value: 1, label: '简单' },
      { value: 2, label: '中等' },
      { value: 3, label: '困难' }
    ]
  } catch (error) {
    console.error('获取难度等级失败:', error)
    difficulties.value = [
      { value: 1, label: '简单' },
      { value: 2, label: '中等' },
      { value: 3, label: '困难' }
    ]
  }
}

// 获取题目类型标签
const getQuestionTypeLabel = (type) => {
  const found = questionTypes.value.find(item => item.value === type)
  return found ? found.label : '未知类型'
}

// 获取题目类型主题色
const getQuestionTypeTheme = (type) => {
  const themeMap = {
    0: 'primary', // 单选题
    1: 'success', // 多选题
    2: 'warning', // 判断题
    3: 'danger'   // 简答题
  }
  return themeMap[type] || 'default'
}

// 获取难度等级标签
const getDifficultyLabel = (difficulty) => {
  const found = difficulties.value.find(item => item.value === difficulty)
  return found ? found.label : '未知难度'
}

// 获取难度等级主题色
const getDifficultyTheme = (difficulty) => {
  const themeMap = {
    1: 'success', // 简单
    2: 'warning', // 中等
    3: 'danger'   // 困难
  }
  return themeMap[difficulty] || 'default'
}

// 格式化题目内容
const formatContent = (content) => {
  if (!content) return ''
  
  // 截断过长的内容
  if (content.length > 100) {
    return content.substring(0, 100) + '...'
  }
  
  return content
}

// 格式化日期时间
const formatDateTime = (dateTime) => {
  if (!dateTime) return '未知'
  
  try {
    const date = new Date(dateTime)
    return date.toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    })
  } catch (error) {
    return dateTime
  }
}

// 判断是否应该显示选项
const shouldShowOptions = (question) => {
  if (!question) return false
  
  // 单选题和多选题：只要有选项就显示
  if (question.questionType === 0 || question.questionType === 1) {
    return question.options && getOptionsArray(question.options).length > 0
  }
  
  // 判断题：始终显示固定选项
  if (question.questionType === 2) {
    return true
  }
  
  // 简答题：显示提示信息
  if (question.questionType === 3) {
    return true
  }
  
  return false
}

// 获取选项标题
const getOptionsTitle = (questionType) => {
  switch (questionType) {
    case 0:
      return '单选题选项'
    case 1:
      return '多选题选项'
    case 2:
      return '判断题选项'
    case 3:
      return '答题说明'
    default:
      return '选项'
  }
}

// 解析选项数据
const getOptionsArray = (options) => {
  if (!options) return []
  
  try {
    // 如果是字符串，尝试解析JSON
    if (typeof options === 'string') {
      const parsed = JSON.parse(options)
      return Array.isArray(parsed) ? parsed : []
    }
    
    // 如果是数组，直接返回
    if (Array.isArray(options)) {
      return options
    }
    
    // 如果是对象，转换为数组
    if (typeof options === 'object') {
      return [options]
    }
    
    return []
  } catch (error) {
    console.error('解析选项数据失败:', error, options)
    return []
  }
}

// 格式化多选题答案
const formatMultiAnswer = (correctAnswer) => {
  if (!correctAnswer) return []
  
  // 处理逗号分隔的答案字符串
  if (typeof correctAnswer === 'string') {
    return correctAnswer.split(',').map(item => item.trim()).filter(item => item)
  }
  
  // 处理数组格式的答案
  if (Array.isArray(correctAnswer)) {
    return correctAnswer
  }
  
  // 单个答案
  return [correctAnswer]
}

// 获取选项标签（A、B、C、D等）
const getOptionLabel = (index) => {
  return String.fromCharCode(65 + index) // A=65, B=66, C=67...
}

// 判断是否为正确选项
const isCorrectOption = (option, correctAnswer) => {
  if (!option || !correctAnswer) return false
  
  const optionKey = option.key || option.label
  if (!optionKey) return false
  
  // 处理多选题答案（逗号分隔）
  if (typeof correctAnswer === 'string' && correctAnswer.includes(',')) {
    const answers = correctAnswer.split(',').map(item => item.trim())
    return answers.includes(optionKey)
  }
  
  // 处理单选题答案
  return optionKey === correctAnswer
}

// 格式化判断题答案
const formatJudgeAnswer = (correctAnswer) => {
  if (!correctAnswer) return '未知'
  
  const answer = correctAnswer.toString().toLowerCase()
  
  // 处理各种可能的答案格式
  if (answer === 'a' || answer === 'true' || answer === '正确' || answer === '1') {
    return '正确'
  } else if (answer === 'b' || answer === 'false' || answer === '错误' || answer === '0') {
    return '错误'
  }
  
  // 如果都不匹配，返回原值
  return correctAnswer
}

// 搜索处理
const handleSearch = () => {
  currentPage.value = 1
  fetchQuestions()
}

// 页码变化处理
const handlePageChange = (page) => {
  currentPage.value = page
  fetchQuestions()
}

// 每页条数变化处理
const handlePageSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
  fetchQuestions()
}

// 获取科目详情
const fetchSubjectDetail = async () => {
  if (!subjectId.value) return
  
  try {
    // 先尝试从路由参数获取科目名称
    if (route.query.name) {
      subjectName.value = route.query.name
      return
    }
    
    // 如果路由参数没有名称，则调用API获取
    const response = await questionAPI.getSubjectById(subjectId.value)
    if (response) {
      // 支持多种字段名
      subjectName.value = response.name || response.subjectName || response.title || `科目${subjectId.value}`
    } else {
      console.warn('获取科目详情失败，使用默认名称')
      subjectName.value = `科目${subjectId.value}`
    }
  } catch (error) {
    console.warn('获取科目详情失败，使用默认名称:', error.message || error)
    subjectName.value = `科目${subjectId.value}`
  }
}

// 处理筛选条件变化
const handleFilterChange = () => {
  currentPage.value = 1
  fetchQuestions()
}

// 显示题目详情
const showQuestionDetail = async (question) => {
  try {
    const response = await questionAPI.getQuestion(question.id)
    selectedQuestion.value = response || question
    detailDialogVisible.value = true
  } catch (error) {
    console.error('获取题目详情失败:', error)
    selectedQuestion.value = question
    detailDialogVisible.value = true
  }
}

// 关闭详情对话框
const closeDetailDialog = () => {
  detailDialogVisible.value = false
  selectedQuestion.value = null
}

// 图片处理相关方法
const hasQuestionImages = (question) => {
  if (!question) return false
  
  // 检查题目是否有图片字段
  const images = getQuestionImages(question)
  return images && images.length > 0
}

const getQuestionImages = (question) => {
  if (!question) return []
  
  // 支持多种图片字段格式
  const imageFields = ['images', 'imageList', 'pictures', 'attachments']
  let images = []
  
  for (const field of imageFields) {
    if (question[field]) {
      if (Array.isArray(question[field])) {
        images = images.concat(question[field])
      } else if (typeof question[field] === 'string') {
        try {
          // 尝试解析JSON格式的字符串
          if (question[field].startsWith('[') && question[field].endsWith(']')) {
            const parsedImages = JSON.parse(question[field])
            if (Array.isArray(parsedImages)) {
              images = images.concat(parsedImages)
            }
          } else {
            // 处理逗号分隔的字符串
            const imageList = question[field].split(',').map(img => img.trim()).filter(img => img)
            images = images.concat(imageList)
          }
        } catch (e) {
          // JSON解析失败，按逗号分隔处理
          const imageList = question[field].split(',').map(img => img.trim()).filter(img => img)
          images = images.concat(imageList)
        }
      }
    }
  }
  
  // 去重并过滤无效图片
  return [...new Set(images)].filter(img => img && img.length > 0)
}

// 图片URL处理工具函数
const formatImageUrl = (imageUrl) => {
  if (!imageUrl) return ''

  try {
    // 处理相对路径和绝对路径
    if (typeof imageUrl !== 'string') {
      return String(imageUrl)
    }

    const url = imageUrl.trim()

    // 处理绝对路径 - 包括http, https, data协议
    if (url.startsWith('http') || url.startsWith('data:') || url.startsWith('//')) {
      return url
    }

    // 如果已经是完整的相对路径，直接返回
    if (url.startsWith('/upload/')) {
      const baseUrl = process?.env?.VUE_APP_BASE_API || ''
      return baseUrl ? `${baseUrl}${url}` : url
    }

    // 获取基础URL - 使用Vue CLI的环境变量
    let baseUrl = ''
    try {
      // 兼容不同环境变量命名
      baseUrl = process?.env?.VUE_APP_BASE_API || ''
    } catch (e) {
      // 如果process未定义，使用空字符串
      baseUrl = ''
    }

    // 处理相对路径
    if (url.startsWith('/')) {
      return baseUrl ? `${baseUrl}${url}` : url
    } else {
      // 处理文件名，添加/upload/前缀
      return baseUrl ? `${baseUrl}/upload/${url}` : `/upload/${url}`
    }
  } catch (error) {
    console.error('格式化图片URL失败:', error, imageUrl)
    return imageUrl || '' // 返回原始URL作为降级方案
  }
}

const handleImageError = (event) => {
  // 图片加载失败时的处理
  console.warn('图片加载失败:', event.target.src)
  // 显示占位符而不是隐藏元素
  event.target.src = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMjAwIiBoZWlnaHQ9IjE1MCIgdmlld0JveD0iMCAwIDIwMCAxNTAiIGZpbGw9Im5vbmUiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+CjxyZWN0IHdpZHRoPSIyMDAiIGhlaWdodD0iMTUwIiBmaWxsPSIjRjNGNEY2Ii8+CjxwYXRoIGQ9Ik0xMDAgNzVDMTA1LjUyMyA3NSAxMTAgNzAuNTIzIDEwIDY1QzExMCA1OS40NzcgMTA1LjUyMyA1NSAxMDAgNTVDOTQuNDc3IDU1IDkwIDU5LjQ3NyA5MCA2NUM5MCA3MC41MjMgOTQuNDc3IDc1IDEwMCA3NVoiIGZpbGw9IiM5Q0EzQUYiLz4KPHBhdGggZD0iTTg1IDk1SDExNUwxMDUgODVMOTUgOTVIMTE1TDk1IDg1TDg1IDk1WiIgZmlsbD0iIzlDQTNBRSIvPgo8L3N2Zz4K'
  event.target.alt = '图片加载失败'
}

const previewImage = (imageUrl, index) => {
  const images = getQuestionImages(selectedQuestion.value)
  previewImages.value = images.map(img => formatImageUrl(img))
  previewImageIndex.value = index
  imagePreviewVisible.value = true
}

const closeImagePreview = () => {
  imagePreviewVisible.value = false
  previewImageIndex.value = 0
  previewImages.value = []
}

const previousImage = () => {
  if (previewImageIndex.value > 0) {
    previewImageIndex.value--
  }
}

const nextImage = () => {
  if (previewImageIndex.value < previewImages.value.length - 1) {
    previewImageIndex.value++
  }
}

const downloadImage = (imageUrl) => {
  if (!imageUrl) return
  
  // 创建下载链接
  const link = document.createElement('a')
  link.href = imageUrl
  link.download = `question-image-${Date.now()}.jpg`
  link.target = '_blank'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}

// 返回上一页
const goBack = () => {
  // 优先使用浏览器历史记录返回
  if (window.history.length > 1) {
    router.go(-1)
  } else {
    // 如果没有历史记录，则跳转到题库列表页面
    router.push('/question-bank')
  }
}

onMounted(() => {
  // 从路由参数获取科目ID
  subjectId.value = parseInt(route.params.id)
  
  // 获取数据
  fetchSubjectDetail()
  fetchQuestionTypes()
  fetchDifficulties()
  fetchQuestions()
})
</script>

<style scoped>
.question-bank-detail-container {
  padding: 20px;
}

.back-button-container {
  margin-bottom: 16px;
}

.back-button {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
  transition: all 0.3s ease;
}

.back-button:hover {
  transform: translateX(-2px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.back-icon {
  font-size: 16px;
  font-weight: bold;
  transition: transform 0.3s ease;
}

.back-button:hover .back-icon {
  transform: translateX(-2px);
}

.question-list-card {
  margin-bottom: 20px;
}

.search-filter-container {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.question-content {
  max-height: 80px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: normal;
}

/* 题目详情模态框样式 */
.question-detail-content {
  max-height: 70vh;
  overflow-y: auto;
}

.detail-section {
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #e7e7e7;
}

.detail-section:last-child {
  border-bottom: none;
  margin-bottom: 0;
}

.section-title {
  margin: 0 0 16px 0;
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 12px;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.label {
  font-weight: 500;
  color: #6b7280;
  min-width: 80px;
}

.value {
  color: #1f2937;
}

.question-content-full {
  padding: 16px;
  background-color: #f9fafb;
  border-radius: 6px;
  border: 1px solid #e5e7eb;
  line-height: 1.6;
}

.options-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.option-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 12px;
  background-color: #f9fafb;
  border-radius: 6px;
  border: 1px solid #e5e7eb;
}

.option-label {
  font-weight: 600;
  color: #374151;
  min-width: 24px;
}

.option-content {
  flex: 1;
  color: #1f2937;
  line-height: 1.5;
}

.correct-option {
  background-color: #f0f9ff !important;
  border-color: #0ea5e9 !important;
}

.correct-indicator {
  color: #10b981;
  font-weight: bold;
  font-size: 16px;
  margin-left: 8px;
}

.correct-answer {
  display: flex;
  align-items: center;
  gap: 8px;
}

.analysis-content {
  padding: 16px;
  background-color: #f0f9ff;
  border-radius: 6px;
  border: 1px solid #bae6fd;
  line-height: 1.6;
  color: #1e40af;
}

.essay-answer {
  padding: 16px;
  background-color: #f0f9ff;
  border-radius: 6px;
  border: 1px solid #bae6fd;
  line-height: 1.6;
  color: #1e40af;
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 14px;
  min-height: 60px;
  width: 100%;
}

.knowledge-points {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.pagination-container {
  margin-top: 20px;
}

/* 题目图片样式 */
.question-images {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
  margin-top: 12px;
}

.image-item {
  position: relative;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
  background-color: #f9fafb;
  border: 1px solid #e5e7eb;
}

.image-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.question-image {
  width: 100%;
  height: 150px;
  object-fit: cover;
  display: block;
}

.image-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.image-item:hover .image-overlay {
  opacity: 1;
}

.preview-text {
  color: white;
  font-size: 14px;
  font-weight: 500;
  background: rgba(0, 0, 0, 0.7);
  padding: 6px 12px;
  border-radius: 4px;
}

/* 图片预览模态框样式 */
.image-preview-dialog :deep(.t-dialog) {
  max-width: 95vw;
  max-height: 95vh;
}

.image-preview-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 20px;
}

.preview-navigation {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}

.nav-button {
  min-width: 80px;
}

.image-counter {
  font-weight: 500;
  color: #6b7280;
  font-size: 14px;
}

.image-wrapper {
  max-width: 100%;
  max-height: 70vh;
  display: flex;
  align-items: center;
  justify-content: center;
}

.preview-image {
  max-width: 100%;
  max-height: 70vh;
  object-fit: contain;
  border-radius: 8px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
  cursor: pointer;
}

.image-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
  margin-top: 16px;
}

/* 响应式图片网格 */
@media (max-width: 768px) {
  .question-images {
    grid-template-columns: 1fr;
  }
  
  .preview-navigation {
    flex-direction: column;
    gap: 8px;
  }
  
  .image-preview-container {
    padding: 10px;
  }
}

/* 修复VueformSelect下拉框定位问题 */
.search-filter-container :deep(.vueform-select) {
  position: relative !important;
}

.search-filter-container :deep(.multiselect-wrapper) {
  position: relative !important;
}

.search-filter-container :deep(.multiselect-dropdown) {
  position: absolute !important;
  top: calc(100% + 2px) !important;
  left: 0 !important;
  right: auto !important;
  z-index: 9999 !important;
  width: 150px !important;
  background: white !important;
  border: 1px solid #d1d5db !important;
  border-radius: 6px !important;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15) !important;
  max-height: 200px !important;
  overflow-y: auto !important;
}

.search-filter-container :deep(.multiselect-options) {
  padding: 4px 0 !important;
}

.search-filter-container :deep(.multiselect-option) {
  padding: 8px 12px !important;
  cursor: pointer !important;
  white-space: nowrap !important;
  font-size: 14px !important;
  color: #374151 !important;
  transition: background-color 0.2s !important;
}

.search-filter-container :deep(.multiselect-option:hover) {
  background-color: #f3f4f6 !important;
}

.search-filter-container :deep(.multiselect-option.is-selected) {
  background-color: #0052d9 !important;
  color: white !important;
}

.search-filter-container :deep(.multiselect-option.is-pointed) {
  background-color: #e5f3ff !important;
}

@media (max-width: 768px) {
  .search-filter-container {
    flex-direction: column;
    align-items: stretch;
  }
  
  .search-filter-container > * {
    width: 100% !important;
    margin-right: 0 !important;
    margin-bottom: 10px;
  }
  
  .info-grid {
    grid-template-columns: 1fr;
  }
  
  .option-item {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>