<template>
  <div class="exam-page">
    <!-- 考试类型选择 -->
    <t-card class="exam-type-card" v-if="!examStarted && !examFinished">
      <div class="exam-type-header">
        <h2>选择考试类型</h2>
        <p>请选择您要参加的考试类型</p>
      </div>
      
      <div class="exam-type-options">
        <div 
          class="exam-type-option" 
          :class="{ active: examType === 'custom' }"
          @click="examType = 'custom'"
        >
          <div class="option-icon">
            <span class="icon">⚙️</span>
          </div>
          <h3>自定义配置</h3>
          <p>自由设置题目数量、难度和时长</p>
        </div>
        
        <div 
          class="exam-type-option" 
          :class="{ active: examType === 'fixed' }"
          @click="examType = 'fixed'"
        >
          <div class="option-icon">
            <span class="icon">📄</span>
          </div>
          <h3>固定试卷配置</h3>
          <p>选择预设的固定试卷进行考试</p>
        </div>
      </div>
    </t-card>
    
    <!-- 自定义配置 -->
    <t-card class="config-card" v-if="!examStarted && !examFinished && examType === 'custom'">
      <div class="config-header">
        <h2>自定义配置</h2>
        <p>请选择考试参数，开始您的模拟考试</p>
      </div>
        
        <t-form :model="examConfig" :rules="configRules" ref="configFormRef" labt-width="120px">
          <t-row :gutter="20">
            <t-col :span="12">
              <t-form-item label="选择科目" prop="subjectId">
                <Multiselect
                  v-model="examConfig.subjectId"
                  :options="subjects"
                  value-prop="id"
                  label="name"
                  placeholder="请选择科目"
                  :can-clear="true"
                  :classes="{
                    container: 'exam-multiselect-container',
                    dropdown: 'exam-select-dropdown'
                  }"
                  style="width: 100%"
                />
              </t-form-item>
            </t-col>
            <t-col :span="12">
              <t-form-item label="题目数量" prop="questionCount">
                <t-input-number
                  v-model="examConfig.questionCount"
                  :min="1"
                  :max="50"
                  placeholder="请输入题目数量"
                />
              </t-form-item>
            </t-col>
          </t-row>
          
          <t-row :gutter="20">
            <t-col :span="12">
              <t-form-item label="考试时长" prop="duration">
                <t-input-number
                  v-model="examConfig.duration"
                  :min="10"
                  :max="180"
                  placeholder="请输入考试时长（分钟）"
                />
              </t-form-item>
            </t-col>
            <t-col :span="12">
              <t-form-item label="难度分布">
                <t-checkbox-group v-model="examConfig.difficulties">
          <t-checkbox value="EASY">简单</t-checkbox>
          <t-checkbox value="MEDIUM">中等</t-checkbox>
          <t-checkbox value="HARD">困难</t-checkbox>
        </t-checkbox-group>
              </t-form-item>
            </t-col>
          </t-row>
          
          <t-form-item>
            <t-button type="primary" size="large" @click="startExam" :loading="loading">
              开始考试
            </t-button>
          </t-form-item>
        </t-form>
      </t-card>
      
      <!-- 固定试卷配置 -->
      <t-card class="config-card" v-if="!examStarted && !examFinished && examType === 'fixed'">
        <div class="config-header">
          <h2>固定试卷配置</h2>
          <p>请选择预设的固定试卷进行考试</p>
        </div>
        
        <!-- 搜索和筛选区域 -->
        <div class="search-area">
          <div class="search-controls">
            <div class="search-input">
              <t-input
                v-model="fixedPaperConfig.keyword"
                placeholder="搜索试卷名称或描述"
                clearable
                @input="handleSearch"
                @clear="handleSearch"
              >
                <template #prefix>
                  <span class="icon">🔍</span>
                </template>
              </t-input>
            </div>
            <div class="subject-select">
              <Multiselect
                v-model="fixedPaperConfig.subjectId"
                :options="subjects"
                value-prop="id"
                label="name"
                placeholder="选择科目"
                :can-clear="true"
                @change="handleSubjectChange"
                :classes="{
                  container: 'exam-multiselect-container',
                  dropdown: 'exam-select-dropdown'
                }"
              />
            </div>
            <div class="search-buttons">
              <t-button type="primary" @click="loadFixedPapers">
                搜索
              </t-button>
              <t-button @click="resetSearch">
                重置
              </t-button>
            </div>
          </div>
        </div>
        
        <div class="papers-content">
          <div v-if="loading" style="text-align: center; padding: 40px;">
            <span class="icon">⏳</span>
            <p>加载中...</p>
          </div>
          
          <div v-else-if="fixedPapers.length > 0" class="fixed-papers-grid">
            <div 
              v-for="paper in fixedPapers" 
              :key="paper.id"
              class="paper-card"
              :class="{ selected: fixedPaperConfig.selectedPaperId === paper.id }"
              @click="selectFixedPaper(paper)"
            >
              <div class="paper-header">
                <h4 class="paper-title">{{ paper.title }}</h4>
                <div class="paper-tags">
                  <t-tag class="subject-tag">{{ paper.subjectName }}</t-tag>
                  <t-tag class="exam-type-tag" :class="paper.examType === 0 ? 'simulation-tag' : 'real-tag'">
                    {{ paper.examTypeText || (paper.examType === 0 ? '模拟试卷' : '真题试卷') }}
                  </t-tag>
                </div>
              </div>
              <div class="paper-info">
                <div class="info-item">
                  <span class="label">题目数量：</span>
                  <span class="value">{{ paper.questionCount }}题</span>
                </div>
                <div class="info-item">
                  <span class="label">考试时长：</span>
                  <span class="value">{{ paper.duration }}分钟</span>
                </div>
                <div class="info-item">
                  <span class="label">总分：</span>
                  <span class="value">{{ paper.totalScore }}分</span>
                </div>
                <div class="info-item">
                  <span class="label">及格分：</span>
                  <span class="value">{{ paper.passScore }}分</span>
                </div>
              </div>
              <div class="paper-description" v-if="paper.description">
                <p>{{ paper.description }}</p>
              </div>
            </div>
          </div>
          
          <div v-else class="no-papers">
            <t-empty description="暂无固定试卷" />
          </div>
        </div>
        
        <!-- 分页组件 -->
        <div v-if="pagination.total > 0" class="pagination-wrapper">
          <CustomPagination
            :current="pagination.current"
            :page-size="pagination.size"
            :total="pagination.total"
            :page-size-options="[
              { value: 5, label: '5' },
              { value: 10, label: '10' },
              { value: 20, label: '20' },
              { value: 50, label: '50' }
            ]"
            @current-change="handleCurrentChange"
            @page-size-change="handleSizeChange"
          />
        </div>
        
        <div v-if="fixedPaperConfig.selectedPaperId" style="margin-top: 20px; text-align: center;">
           <t-button type="primary" size="large" @click="startFixedPaperExam" :loading="loading">
             开始固定试卷考试
           </t-button>
         </div>
      </t-card>
      
      <!-- 考试进行中 -->
      <div v-if="examStarted && !examFinished" class="exam-section">
        <!-- 固定考试信息栏 -->
        <t-card class="exam-info-card fixed-header">
          <div class="exam-info">
            <div class="exam-title">
              <h3>{{ currentExam.subjectName }} - 模拟考试</h3>
            </div>
            <div class="exam-stats">
              <div class="stat-item">
                <span class="label">剩余时间：</span>
                <span class="time" :class="{ 'time-warning': timeWarning }">
                  {{ formatTime(remainingTime) }}
                </span>
              </div>
              <div class="stat-item">
                <span class="label">进度：</span>
                <span class="progress">{{ currentQuestionIndex + 1 }} / {{ examQuestions.length }}</span>
              </div>
              <div class="stat-item">
                <span class="label">已答题：</span>
                <span class="answered">{{ answeredCount }}</span>
              </div>
            </div>
            <div class="exam-actions">
              <t-button @click="toggleNavigationPanel">题目导航</t-button>
              <t-button type="danger" @click="submitExam">提交考试</t-button>
            </div>
          </div>
        </t-card>
        
        <!-- 右侧悬浮导航面板 -->
        <div class="floating-navigation" :class="{ 'show': showNavigationPanel }">
          <t-card class="navigation-panel">
            <div class="navigation-header">
              <h4>题目导航</h4>
              <t-button
                type="text" 
                @click="toggleNavigationPanel" 
                class="close-btn"
              >
                <span class="icon">❌</span>
              </t-button>
            </div>
            
            <!-- 题型分类导航 -->
            <div class="question-types">
              <div 
                v-for="typeGroup in questionTypeGroups" 
                :key="typeGroup.type"
                class="type-group"
              >
                <div class="type-header">
                  <span class="type-name">{{ typeGroup.name }}</span>
                  <span class="type-count">({{ typeGroup.questions.length }}题)</span>
                </div>
                <div class="type-questions">
                   <div
                     v-for="question in typeGroup.questions"
                     :key="question.id"
                     class="nav-item"
                     :class="{
                       'answered': examAnswers[question.id],
                       'current': currentQuestionIndex === question.globalIndex
                     }"
                     @click="scrollToQuestion(question.globalIndex)"
                   >
                     {{ question.globalIndex + 1 }}
                   </div>
                 </div>
              </div>
            </div>
            
            <!-- 快速跳转 -->
            <div class="quick-jump">
              <t-input-number
                v-model="jumpToIndex" 
                :min="1" 
                :max="examQuestions.length"
                size="small"
                placeholder="题号"
                style="width: 80px;"
              />
              <t-button
                size="small" 
                type="primary" 
                @click="quickJump"
                style="margin-left: 8px;"
              >
                跳转
              </t-button>
            </div>
          </t-card>
        </div>
        
        <!-- 全卷预览模式 -->
        <div class="exam-paper">
          
          <!-- 所有题目显示 -->
          <div class="questions-container">
            <t-card
              v-for="(question, index) in examQuestions"
              :key="question.id"
              class="question-card"
              :id="`question-${index}`"
            >
              <div class="question-header">
                <div class="question-info">
                  <span class="question-number">第 {{ index + 1 }} 题</span>
                  <t-tag type="info">{{ getQuestionTypeText(question.questionType) }}</t-tag>
                  <t-tag :type="getDifficultyTagType(question.difficulty)">
                    {{ getDifficultyText(question.difficulty) }}
                  </t-tag>
                  <span class="question-score">（{{ getQuestionScore(question.questionType) }}分）</span>
                </div>
                <div class="answer-status">
                  <t-tag v-if="examAnswers[question.id]" type="success">已答</t-tag>
                  <t-tag v-else type="warning">未答</t-tag>
                </div>
              </div>
              
              <div class="question-content">
                <h3>{{ question.content }}</h3>
                
                <!-- 题目图片显示 -->
                <div v-if="question.images && question.images.length > 0" class="question-images">
                  <div class="image-grid">
                    <div 
                      v-for="(image, imgIndex) in getQuestionImages(question.images)" 
                      :key="imgIndex"
                      class="image-item"
                      @click="previewImage(image, imgIndex)"
                    >
                      <img 
                        :src="formatImageUrl(image)" 
                        :alt="`题目图片 ${imgIndex + 1}`"
                        @error="handleImageError"
                      />
                      <div class="image-overlay">
                        <span class="zoom-icon">🔍</span>
                      </div>
                    </div>
                  </div>
                </div>
                
                <!-- 单选题 -->
                <div class="options" v-if="question.questionType === 0">
                  <t-radio-group
                    v-model="examAnswers[question.id]"
                    class="option-group"
                    @change="saveQuestionAnswer(question.id, $event)"
                  >
                    <t-radio
                      v-for="(option, optIndex) in question.optionList"
                      :key="optIndex"
                      :value="option.key"
                      class="option-item"
                    >
                      {{ option.key }}. {{ option.value }}
                    </t-radio>
                  </t-radio-group>
                </div>
                
                <!-- 多选题 -->
                <div class="options" v-else-if="question.questionType === 1">
                  <t-checkbox-group
                    :model-value="getMultipleAnswers(question.id)"
                    class="option-group"
                    @change="saveMultipleAnswer(question.id, $event)"
                  >
                    <t-checkbox
                      v-for="(option, optIndex) in question.optionList"
                      :key="optIndex"
                      :value="option.key"
                      class="option-item"
                    >
                      {{ option.key }}. {{ option.value }}
                    </t-checkbox>
                  </t-checkbox-group>
                </div>
                
                <!-- 判断题 -->
                <div class="true-false-options" v-else-if="question.questionType === 2">
                  <t-radio-group
                    v-model="examAnswers[question.id]"
                    class="tf-group"
                    @change="saveQuestionAnswer(question.id, $event)"
                  >
                    <t-radio value="A" class="tf-option">正确</t-radio>
                    <t-radio value="B" class="tf-option">错误</t-radio>
                  </t-radio-group>
                </div>
                
                <!-- 填空题 -->
                <div class="fill-blank-options" v-else-if="question.questionType === 3">
                  <t-input
                    v-model="examAnswers[question.id]"
                    type="textarea"
                    :rows="3"
                    placeholder="请输入答案"
                    class="fill-input"
                    @blur="saveQuestionAnswer(question.id, examAnswers[question.id])"
                  />
                </div>
                
                <!-- 简答题 -->
                <div class="essay-options" v-else-if="question.questionType === 4">
                  <t-input
                    v-model="examAnswers[question.id]"
                    type="textarea"
                    :rows="6"
                    placeholder="请输入答案"
                    class="essay-input"
                    @blur="saveQuestionAnswer(question.id, examAnswers[question.id])"
                  />
                </div>
              </div>
            </t-card>
          </div>
        </div>
      </div>
      
      <!-- 图片预览模态框 -->
      <t-dialog
        v-model:visible="showImagePreview"
        :header="false"
        :footer="false"
        :close-on-overlay-click="true"
        :close-on-esc-key="true"
        width="auto"
        class="image-preview-dialog"
        @close="closeImagePreview"
      >
        <div class="image-preview-container">
          <img 
            :src="previewImageUrl" 
            :alt="previewImageAlt"
            class="preview-image"
            @error="handlePreviewImageError"
          />
          <div class="image-preview-actions">
            <t-button 
              type="text" 
              @click="closeImagePreview"
              class="close-preview-btn"
            >
              <span class="icon">❌</span>
            </t-button>
          </div>
        </div>
      </t-dialog>

      <!-- 考试结果 -->
      <t-card v-if="examFinished" class="result-card">
        <div class="result-header">
          <h2>考试完成</h2>
          <p>恭喜您完成了本次模拟考试</p>
        </div>
        
        <div class="result-stats">
          <t-row :gutter="20" justify="center">
            <t-col :span="6">
              <div class="stat-box">
                <div class="stat-value">{{ totalScore }}</div>
                <div class="stat-label">总分</div>
              </div>
            </t-col>
            <t-col :span="6">
              <div class="stat-box">
                <div class="stat-value">{{ examResult.correctCount }}</div>
                <div class="stat-label">正确题数</div>
              </div>
            </t-col>
            <t-col :span="6">
              <div class="stat-box">
                <div class="stat-value">{{ formattedAccuracy }}%</div>
                <div class="stat-label">正确率</div>
              </div>
            </t-col>
            <t-col :span="6">
              <div class="stat-box">
                <div class="stat-value">{{ examResult.timeUsed }}</div>
                <div class="stat-label">用时</div>
              </div>
            </t-col>
          </t-row>
        </div>
        
        <div class="result-actions">
          <t-button type="primary" @click="restartExam">重新考试</t-button>
          <t-button @click="viewDetails">查看详情</t-button>
          <t-button @click="$router.push('/')">返回首页</t-button>
        </div>
      </t-card>
      

  </div>
</template>

<script>
import { ref, reactive, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { MessagePlugin, DialogPlugin } from 'tdesign-vue-next'
// TDesign icons imported individually
import { examAPI, subjectAPI } from '../api'
import { useExamStore } from '../store/exam'
import Multiselect from '@vueform/multiselect'
import CustomPagination from '../components/CustomPagination.vue'

export default {
  name: 'Exam',
  components: {
    Multiselect,
    CustomPagination
  },
  setup() {
    // 使用 Pinia store
    const router = useRouter()
    const examStore = useExamStore()
    
    const subjects = ref([])
    const loading = ref(false)
    const showNavigationPanel = ref(false)
    const jumpToIndex = ref(1)
    const timer = ref(null)
    
    // 图片预览相关
    const showImagePreview = ref(false)
    const previewImageUrl = ref('')
    const previewImageAlt = ref('')
    
    const configFormRef = ref()
    const examConfig = reactive({
      subjectId: '',
      questionCount: 20,
      duration: 60,
      difficulties: ['EASY', 'MEDIUM', 'HARD']
    })
    
    const configRules = {
      subjectId: [{ required: true, message: '请选择科目', trigger: 'change' }],
      questionCount: [{ required: true, message: '请输入题目数量', trigger: 'blur' }],
      duration: [{ required: true, message: '请输入考试时长', trigger: 'blur' }]
    }
    
    // 考试类型选择
    const examType = ref('custom')
    
    // 固定试卷配置
    const fixedPaperFormRef = ref()
    const fixedPapers = ref([])
    const fixedPaperConfig = reactive({
      subjectId: '',
      examId: '',
      keyword: ''
    })
    
    // 分页配置
    const pagination = reactive({
      current: 1,
      size: 10,
      total: 0
    })
    
    const fixedPaperRules = {
      subjectId: [{ required: true, message: '请选择科目', trigger: 'change' }],
      examId: [{ required: true, message: '请选择试卷', trigger: 'change' }]
    }
    
    // 从 store 获取状态
    const examStarted = computed(() => examStore.examStatus === 'in_progress' || examStore.examStatus === 'paused')
    const examFinished = computed(() => examStore.examStatus === 'completed' || examStore.examStatus === 'submitted')
    const currentExam = computed(() => examStore.examInfo)
    const examQuestions = computed(() => examStore.questions)
    // 创建响应式的答案对象，而不是computed
    const examAnswers = ref({})
    const currentQuestionIndex = computed(() => examStore.currentQuestionIndex)
    const remainingTime = computed(() => examStore.remainingTime)
    const examResult = computed(() => examStore.examResult)

    // 监听store中的userAnswers变化，同步到本地响应式对象
    watch(() => examStore.userAnswers, (newAnswers) => {
      examAnswers.value = { ...newAnswers }
    }, { immediate: true, deep: true })
    
    const currentExamQuestion = computed(() => {
      return examStore.currentQuestion
    })
    
    const answeredCount = computed(() => {
      return examStore.answeredCount
    })
    
    const timeWarning = computed(() => {
      return remainingTime.value <= 300 // 5分钟警告
    })
    
    // 计算总分
    const totalScore = computed(() => {
      if (!examQuestions.value || examQuestions.value.length === 0) {
        return 0
      }
      return examQuestions.value.reduce((total, question) => {
        return total + getQuestionScore(question.questionType)
      }, 0)
    })
    
    // 格式化正确率（保留两位小数）
    const formattedAccuracy = computed(() => {
      if (!examResult.value || !examResult.value.accuracy) {
        return '0.00'
      }
      return parseFloat(examResult.value.accuracy).toFixed(2)
    })
    
    // 题型分组
    const questionTypeGroups = computed(() => {
      if (!examQuestions.value || examQuestions.value.length === 0) {
        return []
      }
      
      const typeMap = {
        0: '单选题',
        1: '多选题', 
        2: '判断题',
        3: '填空题',
        4: '简答题'
      }
      
      const groups = {}
      
      examQuestions.value.forEach((question, globalIndex) => {
        const type = question.questionType
        if (!groups[type]) {
          groups[type] = {
            type,
            name: typeMap[type] || '其他题型',
            questions: []
          }
        }
        groups[type].questions.push({
          ...question,
          globalIndex
        })
      })
      
      return Object.values(groups).sort((a, b) => a.type - b.type)
    })
    

    
    // 获取科目列表
    const getSubjects = async () => {
      try {
        console.log('🔄 Exam.vue: 开始获取科目列表')
        const response = await subjectAPI.getEnabledSubjects()
        console.log('📊 Exam.vue: 科目API响应:', response)

        if (Array.isArray(response)) {
          // API直接返回数组格式
          subjects.value = response
          console.log('✅ Exam.vue: 科目列表加载成功 (数组格式):', subjects.value)
        } else if (response && response.code === 200 && Array.isArray(response.data)) {
          // 标准格式：包含code和data
          subjects.value = response.data
          console.log('✅ Exam.vue: 科目列表加载成功 (标准格式):', subjects.value)
        } else if (response && Array.isArray(response.data)) {
          // 兼容其他格式
          subjects.value = response.data
          console.log('✅ Exam.vue: 科目列表加载成功 (兼容格式):', subjects.value)
        } else {
          console.error('❌ Exam.vue: 获取科目数据格式错误:', response)
          subjects.value = []
        }
      } catch (error) {
        console.error('❌ Exam.vue: 获取科目列表失败:', error)
        MessagePlugin.error('获取科目列表失败: ' + (error.message || '未知错误'))
        subjects.value = []
      }
    }
    
    // 开始考试
    const startExam = async () => {
      try {
        const valid = await configFormRef.value.validate()
        if (!valid) return
        
        loading.value = true
        const exam = await examAPI.createSimulationExam(examConfig)
        
        // 使用 store 初始化考试
        examStore.initExam({
          id: exam.examId,
          title: `${exam.subjectName} - 模拟考试`,
          duration: examConfig.duration,
          questions: exam.questions
        })
        
        startTimer()
        
        MessagePlugin.success('考试开始，祝您考试顺利！')
      } catch (error) {
        console.error('开始考试失败:', error)
      } finally {
        loading.value = false
      }
    }
    
    // 开始计时
    const startTimer = () => {
      timer.value = setInterval(() => {
        if (remainingTime.value > 0) {
          examStore.updateRemainingTime(remainingTime.value - 1)
          if (remainingTime.value <= 0) {
            submitExam(true)
          }
        }
      }, 1000)
    }
    
    // 停止计时
    const stopTimer = () => {
      if (timer.value) {
        clearInterval(timer.value)
        timer.value = null
      }
    }
    
    // 格式化时间
    const formatTime = (seconds) => {
      const hours = Math.floor(seconds / 3600)
      const minutes = Math.floor((seconds % 3600) / 60)
      const secs = seconds % 60
      
      if (hours > 0) {
        return `${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
      }
      return `${minutes.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
    }
    
    // 保存单个题目答案
    const saveQuestionAnswer = (questionId, answer) => {
      console.log('🔄 保存单选/判断题答案:', { questionId, answer })
      if (answer) {
        // 同时更新store和本地响应式对象
        examStore.saveAnswer(questionId, answer)
        examAnswers.value[questionId] = answer
        console.log('✅ 答案已保存到store:', examStore.userAnswers)
        console.log('✅ 本地答案对象:', examAnswers.value)
      }
    }
    
    // 保存多选题答案
    const saveMultipleAnswer = (questionId, answers) => {
      console.log('🔄 保存多选题答案:', { questionId, answers })
      const answer = answers.join(',')
      // 同时更新store和本地响应式对象
      examStore.saveAnswer(questionId, answer)
      examAnswers.value[questionId] = answer
      console.log('✅ 多选题答案已保存到store:', examStore.userAnswers)
      console.log('✅ 本地答案对象:', examAnswers.value)
    }
    
    // 获取多选题答案数组
    const getMultipleAnswers = (questionId) => {
      const answer = examAnswers.value[questionId]
      return answer ? answer.split(',') : []
    }
    
    // 滚动到指定题目
    const scrollToQuestion = (index) => {
      examStore.setCurrentQuestionIndex(index)
      const element = document.getElementById(`question-${index}`)
      if (element) {
        // 考虑固定头部的高度偏移
        const headerHeight = 140
        const elementPosition = element.offsetTop - headerHeight
        window.scrollTo({
          top: elementPosition,
          behavior: 'smooth'
        })
      }
    }
    
    // 切换导航面板显示
    const toggleNavigationPanel = () => {
      showNavigationPanel.value = !showNavigationPanel.value
    }
    
    // 快速跳转到指定题号
    const quickJump = () => {
      if (jumpToIndex.value >= 1 && jumpToIndex.value <= examQuestions.value.length) {
        const targetIndex = jumpToIndex.value - 1
        scrollToQuestion(targetIndex)
        examStore.setCurrentQuestionIndex(targetIndex)
      } else {
        MessagePlugin.warning('请输入有效的题号')
      }
    }
    
    // 获取题目分数
    const getQuestionScore = (questionType) => {
      // 根据题目类型返回不同分数
      switch (questionType) {
        case 0: // 单选题
        case 2: // 判断题
          return 2
        case 1: // 多选题
          return 3
        case 3: // 填空题
          return 4
        case 4: // 简答题
          return 10
        default:
          return 2
      }
    }
    
    // 提交考试
    const submitExam = async (autoSubmit = false) => {
      try {
        if (!autoSubmit) {
          // 检查是否有未答题目
          const unansweredCount = examQuestions.value.length - answeredCount.value
          let confirmMessage = '确定要提交考试吗？提交后无法修改答案。'
          
          if (unansweredCount > 0) {
            confirmMessage = `还有 ${unansweredCount} 道题未作答，确定要提交考试吗？`
          }
          
          const confirmResult = await DialogPlugin.confirm({
            header: '提示',
            body: confirmMessage,
            confirmBtn: '确定提交',
            cancelBtn: '取消',
            theme: 'warning'
          })

          if (!confirmResult) {
            return
          }
        }
        
        stopTimer()
        
        // 将examAnswers对象转换为AnswerSubmitRequest数组格式
        const totalDuration = currentExam.value.isFixedPaper ? examStore.examInfo.duration * 60 : examConfig.duration * 60
        const usedTime = Math.max(0, totalDuration - Math.max(0, remainingTime.value))
        const answersArray = Object.keys(examAnswers.value).map(questionId => ({
          questionId: parseInt(questionId),
          userAnswer: examAnswers.value[questionId],
          answerTime: Math.floor(usedTime / Object.keys(examAnswers.value).length) // 平均用时
        }))
        
        // 根据考试类型选择不同的提交接口
        let result
        if (currentExam.value.isFixedPaper) {
          // 固定试卷考试使用专门的提交接口
          result = await examAPI.submitFixedPaperExam(currentExam.value.id, answersArray)
        } else if (typeof currentExam.value.id === 'string' && currentExam.value.id.startsWith('simulation_')) {
          // 模拟考试使用专门的提交接口
          result = await examAPI.submitSimulationExam(currentExam.value.id, answersArray)
        } else {
          // 正式考试使用原有接口
          result = await examAPI.submitExam(currentExam.value.id, answersArray)
        }
        
        // 计算用时 - 确保不会出现负数
        let timeUsedSeconds
        if (currentExam.value.isFixedPaper) {
          // 固定试卷考试使用考试时长计算用时
          timeUsedSeconds = Math.max(0, examStore.examInfo.duration * 60 - Math.max(0, remainingTime.value))
        } else {
          // 自定义考试使用配置时长计算用时
          timeUsedSeconds = Math.max(0, examConfig.duration * 60 - Math.max(0, remainingTime.value))
        }
        const timeUsedMinutes = Math.floor(timeUsedSeconds / 60)
        const timeUsedSecondsRemainder = timeUsedSeconds % 60
        const timeUsedFormatted = `${timeUsedMinutes}分${timeUsedSecondsRemainder}秒`
        
        // 使用 store 提交考试
        examStore.submitExam({
          ...result,
          timeUsed: timeUsedFormatted
        })
        
        if (autoSubmit) {
          MessagePlugin.warning('考试时间已到，系统自动提交')
        } else {
          MessagePlugin.success('考试提交成功')
        }
      } catch (error) {
        if (error !== 'cancel') {
          console.error('提交考试失败:', error)
        }
      }
    }
    
    // 重新考试
    const restartExam = () => {
      examStore.resetExam()
      stopTimer()
    }
    
    // 查看详情
    const viewDetails = () => {
      if (examResult.value) {
        // 检查是否为模拟考试
        if (typeof currentExam.value.id === 'string' && currentExam.value.id.startsWith('simulation_')) {
          // 模拟考试详情 - 使用模拟考试ID作为recordId
          router.push({
            name: 'ExamDetail',
            params: { examId: -1 },
            query: { recordId: currentExam.value.id }
          })
        } else if (examResult.value.examId) {
          // 正式考试详情
          router.push(`/exam-detail/${examResult.value.examId}`)
        } else {
          MessagePlugin.warning('无法获取考试详情')
        }
      } else {
        MessagePlugin.warning('无法获取考试详情')
      }
    }
    
    // 获取题目类型文本
    const getQuestionTypeText = (type) => {
      const typeMap = {
        0: '单选题',
        1: '多选题',
        2: '判断题',
        3: '填空题',
        4: '简答题',
        'SINGLE_CHOICE': '单选题',
        'MULTIPLE_CHOICE': '多选题',
        'TRUE_FALSE': '判断题'
      }
      return typeMap[type] || '未知题型'
    }

    // 图片处理函数
    const getQuestionImages = (images) => {
      if (!images) return []
      
      try {
        // 处理JSON字符串格式的图片数组
        if (typeof images === 'string') {
          const parsed = JSON.parse(images)
          return Array.isArray(parsed) ? parsed : [images]
        }
        
        // 处理数组格式
        if (Array.isArray(images)) {
          return images
        }
        
        // 处理单个字符串
        return [images]
      } catch (error) {
        // 如果不是JSON，按逗号分隔处理
        if (typeof images === 'string') {
          return images.split(',').map(img => img.trim()).filter(img => img)
        }
        return []
      }
    }

    const formatImageUrl = (url) => {
      if (!url) return ''
      
      try {
        // 如果已经是完整URL，直接返回
        if (url.startsWith('http://') || url.startsWith('https://') || url.startsWith('data:')) {
          return url
        }
        
        // 如果已经是/upload/路径，直接返回
        if (url.startsWith('/upload/')) {
          return `${process.env.VUE_APP_BASE_API}${url}`
        }
        
        // 否则添加/upload/前缀
        return `${process.env.VUE_APP_BASE_API}/upload/${url}`
      } catch (error) {
        console.error('格式化图片URL失败:', error)
        return url
      }
    }

    const handleImageError = (event) => {
      event.target.src = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMjAwIiBoZWlnaHQ9IjE1MCIgdmlld0JveD0iMCAwIDIwMCAxNTAiIGZpbGw9Im5vbmUiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+CjxyZWN0IHdpZHRoPSIyMDAiIGhlaWdodD0iMTUwIiBmaWxsPSIjRjNGNEY2Ii8+CjxwYXRoIGQ9Ik0xMDAgNzVMODUgNjBMNzAgNzVMODUgOTBMMTAwIDc1WiIgZmlsbD0iIzlDQTNBRiIvPgo8cGF0aCBkPSJNMTMwIDYwTDExNSA0NUwxMDAgNjBMODUgNDVMNzAgNjBMODUgNzVMMTAwIDYwTDExNSA3NUwxMzAgNjBaIiBmaWxsPSIjOENBNEFFIi8+Cjx0ZXh0IHg9IjEwMCIgeT0iMTIwIiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjOTY5NUE0IiBmb250LXNpemU9IjEyIj7mi6zlj5HkuIDlj5HnmoTkvZzlj6M8L3RleHQ+Cjwvc3ZnPgo='
    }

    const previewImage = (imageUrl, index) => {
      previewImageUrl.value = formatImageUrl(imageUrl)
      previewImageAlt.value = `题目图片 ${index + 1}`
      showImagePreview.value = true
    }

    const closeImagePreview = () => {
      showImagePreview.value = false
      previewImageUrl.value = ''
      previewImageAlt.value = ''
    }

    const handlePreviewImageError = () => {
      previewImageUrl.value = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNDAwIiBoZWlnaHQ9IjMwMCIgdmlld0JveD0iMCAwIDQwMCAzMDAiIGZpbGw9Im5vbmUiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+CjxyZWN0IHdpZHRoPSI0MDAiIGhlaWdodD0iMzAwIiBmaWxsPSIjRjNGNEY2Ii8+CjxwYXRoIGQ9Ik0yMDAgMTUwTDE3MCAxMjBMMTQwIDE1MEwxNzAgMTgwTDIwMCAxNTBaIiBmaWxsPSIjOUNBM0FGIi8+CjxwYXRoIGQ9Ik0yNjAgMTIwTDIzMCA5MEwyMDAgMTIwTDE3MCA5MEwxNDAgMTIwTDE3MCAxNTBMMjAwIDEyMEwyMzAgMTUwTDI2MCAxMjBaIiBmaWxsPSIjOENBNEFFIi8+Cjx0ZXh0IHg9IjIwMCIgeT0iMjQwIiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjOTY5NUE0IiBmb250LXNpemU9IjE0Ij7mi6zlj5HkuIDlj5HnmoTkvZzlj6M8L3RleHQ+Cjwvc3ZnPgo='
    }
    
    // 获取难度文本
    const getDifficultyText = (difficulty) => {
      const difficultyMap = {
        1: '简单',
        2: '中等',
        3: '困难',
        'EASY': '简单',
        'MEDIUM': '中等',
        'HARD': '困难'
      }
      return difficultyMap[difficulty] || '未知难度'
    }
    
    // 获取难度标签类型
    const getDifficultyTagType = (difficulty) => {
      const typeMap = {
        1: 'success',
        2: 'warning',
        3: 'danger',
        'EASY': 'success',
        'MEDIUM': 'warning',
        'HARD': 'danger'
      }
      return typeMap[difficulty] || 'info'
    }
    
    // 加载固定试卷列表
    const loadFixedPapers = async () => {
      try {
        loading.value = true
        console.log('🔄 开始加载固定试卷列表，当前筛选条件:', {
          subjectId: fixedPaperConfig.subjectId,
          keyword: fixedPaperConfig.keyword,
          current: pagination.current,
          size: pagination.size
        })
        
        const params = {
          current: pagination.current,
          size: pagination.size
        }
        
        if (fixedPaperConfig.subjectId) {
          params.subjectId = fixedPaperConfig.subjectId
          console.log('✅ 添加科目筛选条件:', params.subjectId)
        }
        
        if (fixedPaperConfig.keyword && fixedPaperConfig.keyword.trim()) {
          params.keyword = fixedPaperConfig.keyword.trim()
          console.log('✅ 添加关键词筛选条件:', params.keyword)
        }
        
        console.log('📤 发送请求参数:', params)
        const response = await examAPI.getFixedPapers(params)
        console.log('📥 接收到响应:', response)
        
        // 处理API返回的分页数据格式：{ records: [...], total: 123, current: 1, size: 10 }
        if (response && response.records) {
          fixedPapers.value = response.records
          pagination.total = response.total || 0
          console.log('✅ 固定试卷列表加载完成:', {
            试卷数量: fixedPapers.value.length,
            总数: pagination.total,
            当前页: response.current,
            每页条数: response.size
          })
        } else {
          console.warn('⚠️ 响应数据格式异常:', response)
          fixedPapers.value = []
          pagination.total = 0
        }
        
        fixedPaperConfig.selectedPaperId = '' // 重置选择的试卷
      } catch (error) {
        console.error('❌ 获取固定试卷列表失败:', error)
        MessagePlugin.error('获取固定试卷列表失败: ' + (error.message || '未知错误'))
        fixedPapers.value = []
        pagination.total = 0
      } finally {
        loading.value = false
      }
    }
    
    // 搜索处理
    const handleSearch = () => {
      pagination.current = 1
      loadFixedPapers()
    }
    
    // 科目变化处理
    const handleSubjectChange = (selectedSubjectId) => {
      console.log('🔄 科目选择变化:', selectedSubjectId)
      fixedPaperConfig.subjectId = selectedSubjectId
      pagination.current = 1 // 重置到第一页
      loadFixedPapers() // 自动触发搜索
    }
    
    // 重置搜索
    const resetSearch = () => {
      console.log('🔄 重置搜索条件')
      fixedPaperConfig.subjectId = ''
      fixedPaperConfig.keyword = ''
      pagination.current = 1
      loadFixedPapers()
    }
    
    // 分页大小改变
    const handleSizeChange = (size) => {
      pagination.size = size
      pagination.current = 1
      loadFixedPapers()
    }
    
    // 当前页改变
    const handleCurrentChange = (current) => {
      pagination.current = current
      loadFixedPapers()
    }
    
    // 选择固定试卷
    const selectFixedPaper = (paper) => {
      fixedPaperConfig.selectedPaperId = paper.id
      fixedPaperConfig.examId = paper.id
    }
    
    // 开始固定试卷考试
    const startFixedPaperExam = async () => {
      try {
        if (!fixedPaperConfig.selectedPaperId) {
          MessagePlugin.warning('请先选择一份试卷')
          return
        }
        
        loading.value = true
        const exam = await examAPI.startFixedPaperExam(fixedPaperConfig.selectedPaperId)
        
        // 使用 store 初始化考试
        examStore.initExam({
          id: exam.examId,
          title: exam.examTitle,
          duration: exam.duration,
          questions: exam.questions,
          isFixedPaper: true // 标记为固定试卷
        })
        
        startTimer()
        
        MessagePlugin.success('固定试卷考试开始，祝您考试顺利！')
      } catch (error) {
        console.error('开始固定试卷考试失败:', error)
        MessagePlugin.error('开始固定试卷考试失败：' + (error.message || '未知错误'))
      } finally {
        loading.value = false
      }
    }
    

    
    // 监听考试状态，自动恢复计时器
    watch(() => examStore.examStatus, (newStatus) => {
      if (newStatus === 'in_progress' && !timer.value) {
        startTimer()
      } else if (newStatus !== 'in_progress') {
        stopTimer()
      }
    })
    
    // 监听考试类型变化，自动加载固定试卷列表
    watch(() => examType.value, (newType) => {
      if (newType === 'fixed') {
        loadFixedPapers()
      }
    })
    
    onMounted(async () => {
      console.log('🎯 Exam.vue: 组件已挂载')
      
      await getSubjects()
      await nextTick() // 确保DOM更新
      
      // 强制触发下拉框重新渲染
      console.log('🔄 Exam.vue: 强制触发下拉框重新渲染')
      
      // 如果有正在进行的考试，恢复计时器
      if (examStore.examStatus === 'in_progress') {
        startTimer()
      }
    })
    
    onUnmounted(() => {
      stopTimer()
    })
    
    return {
      subjects,
      loading,
      examStarted,
      examFinished,
      configFormRef,
      examConfig,
      configRules,
      examType,
      fixedPaperFormRef,
      fixedPapers,
      fixedPaperConfig,
      fixedPaperRules,
      pagination,
      handleSearch,
      handleSizeChange,
      handleCurrentChange,
      currentExam,
      examQuestions,
      examAnswers,
      currentQuestionIndex,
      remainingTime,
      examResult,
      currentExamQuestion,
      answeredCount,
      timeWarning,
      totalScore,
      formattedAccuracy,
      startExam,
      formatTime,
      saveQuestionAnswer,
      saveMultipleAnswer,
      getMultipleAnswers,
      scrollToQuestion,
      getQuestionScore,
      submitExam,
      restartExam,
      viewDetails,
      getQuestionTypeText,
      getDifficultyText,
      getDifficultyTagType,
      loadFixedPapers,
      selectFixedPaper,
      startFixedPaperExam,
      handleSubjectChange,
      resetSearch,
      showNavigationPanel,
      jumpToIndex,
      questionTypeGroups,
      toggleNavigationPanel,
      quickJump,
      // 图片预览相关
      showImagePreview,
      previewImageUrl,
      previewImageAlt,
      getQuestionImages,
      formatImageUrl,
      handleImageError,
      previewImage,
      closeImagePreview,
      handlePreviewImageError
    }
  }
}
</script>

<style scoped>
.exam-page {
  padding: 20px;
  min-height: calc(100vh - 120px);
  background-color: #f5f7fa;
  position: relative;
}

.main-content {
  flex: 1;
  padding: 20px;
  background-color: #f5f7fa;
}

.exam-type-card {
  max-width: 800px;
  margin: 0 auto 20px auto;
}

.exam-type-header {
  text-align: center;
  margin-bottom: 30px;
}

.exam-type-header h2 {
  color: #333;
  margin-bottom: 10px;
}

.exam-type-header p {
  color: #666;
}

.exam-type-options {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin-bottom: 20px;
}

.exam-type-option {
  padding: 30px 20px;
  border: 2px solid #e4e7ed;
  border-radius: 8px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s;
  background: white;
}

.exam-type-option:hover {
  border-color: #409EFF;
  box-shadow: 0 2px 12px rgba(64, 158, 255, 0.1);
}

.exam-type-option.active {
  border-color: #409EFF;
  background-color: #f0f9ff;
}

.option-icon {
  font-size: 32px;
  color: #409EFF;
  margin-bottom: 15px;
}

.exam-type-option h3 {
  color: #333;
  margin: 0 0 10px 0;
  font-size: 18px;
}

.exam-type-option p {
  color: #666;
  margin: 0;
  font-size: 14px;
}

.config-card {
  max-width: 800px;
  margin: 0 auto;
}

.fixed-papers-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
  margin: 20px 0;
}

.paper-card {
  border: 2px solid #e4e7ed;
  border-radius: 8px;
  padding: 20px;
  cursor: pointer;
  transition: all 0.3s;
  background: white;
}

.paper-card:hover {
  border-color: #409EFF;
  box-shadow: 0 2px 12px rgba(64, 158, 255, 0.1);
}

.paper-card.selected {
  border-color: #409EFF;
  background-color: #f0f9ff;
}

.paper-header {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 15px;
}

.paper-title {
  margin: 0;
  color: #333;
  font-size: 16px;
  font-weight: 600;
  line-height: 1.4;
  word-wrap: break-word;
  word-break: break-all;
  hyphens: auto;
}

.paper-tags {
  display: flex;
  gap: 8px;
  align-items: flex-start;
  flex-wrap: wrap;
}

/* 科目标签样式 - 蓝色底色 */
:deep(.subject-tag) {
  background-color: #e3f2fd !important;
  color: #1976d2 !important;
  border: 1px solid #bbdefb !important;
  font-size: 12px;
  padding: 4px 8px;
  border-radius: 4px;
  font-weight: 500;
}

:deep(.subject-tag:hover) {
  background-color: #bbdefb !important;
  color: #0d47a1 !important;
}

/* 模拟试卷标签样式 - 橙色底色 */
:deep(.simulation-tag) {
  background-color: #fff3e0 !important;
  color: #f57c00 !important;
  border: 1px solid #ffcc02 !important;
  font-size: 12px;
  padding: 4px 8px;
  border-radius: 4px;
  font-weight: 500;
}

:deep(.simulation-tag:hover) {
  background-color: #ffcc02 !important;
  color: #e65100 !important;
}

/* 真题试卷标签样式 - 红色底色 */
:deep(.real-tag) {
  background-color: #ffebee !important;
  color: #d32f2f !important;
  border: 1px solid #ffcdd2 !important;
  font-size: 12px;
  padding: 4px 8px;
  border-radius: 4px;
  font-weight: 500;
}

:deep(.real-tag:hover) {
  background-color: #ffcdd2 !important;
  color: #b71c1c !important;
}

/* 确保标签在小屏幕上也能正常换行 */
@media (max-width: 768px) {
  .paper-header {
    gap: 10px;
  }
  
  .paper-title {
    font-size: 15px;
    line-height: 1.3;
  }
  
  .paper-tags {
    gap: 6px;
  }
  
  :deep(.subject-tag),
  :deep(.simulation-tag),
  :deep(.real-tag) {
    font-size: 11px;
    padding: 3px 6px;
  }
}

.paper-info {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  margin-bottom: 15px;
}

.info-item {
  display: flex;
  justify-content: space-between;
}

.info-item .label {
  color: #666;
  font-size: 14px;
}

.info-item .value {
  color: #333;
  font-weight: 500;
  font-size: 14px;
}

.paper-description {
  color: #666;
  font-size: 14px;
  line-height: 1.5;
}

.paper-description p {
  margin: 0;
}

.search-area {
  margin-bottom: 20px;
  padding: 20px;
  background: #f8f9fa;
  border-radius: 6px;
}

.pagination-wrapper {
  margin-top: 20px;
}

.no-papers {
  text-align: center;
  padding: 40px 0;
}

.config-header {
  text-align: center;
  margin-bottom: 30px;
}

.config-header h2 {
  color: #333;
  margin-bottom: 10px;
}

.config-header p {
  color: #666;
}

/* 悬浮导航面板样式 */
.floating-navigation {
  position: fixed;
  top: 50%;
  right: -350px;
  transform: translateY(-50%);
  width: 320px;
  height: 70vh;
  z-index: 999;
  transition: right 0.3s ease-in-out;
}

.floating-navigation.show {
  right: 20px;
}

.navigation-panel {
  height: 100%;
  overflow: hidden;
  box-shadow: -2px 0 12px rgba(0, 0, 0, 0.15);
}

.navigation-panel .t-card__body {
  height: 100%;
  padding: 16px;
  display: flex;
  flex-direction: column;
}

.navigation-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
  padding-bottom: 10px;
  border-bottom: 1px solid #ebeef5;
  flex-shrink: 0;
}

.navigation-header h4 {
  margin: 0;
  color: #303133;
  font-size: 16px;
}

.close-btn {
  padding: 4px;
  color: #909399;
}

.close-btn:hover {
  color: #409eff;
}

/* 题型分组样式 */
.question-types {
  flex: 1;
  overflow-y: auto;
  margin-bottom: 15px;
}

.type-group {
  margin-bottom: 20px;
}

.type-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: #f5f7fa;
  border-radius: 6px;
  margin-bottom: 8px;
}

.type-name {
  font-weight: 600;
  color: #303133;
  font-size: 14px;
}

.type-count {
  font-size: 12px;
  color: #909399;
}

.type-questions {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(36px, 1fr));
  gap: 6px;
  padding: 0 4px;
}

.nav-item {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid #dcdfe6;
  border-radius: 6px;
  cursor: pointer;
  font-weight: 500;
  font-size: 12px;
  transition: all 0.3s ease;
  background: white;
}

.nav-item:hover {
  border-color: #409eff;
  color: #409eff;
  transform: scale(1.05);
}

.nav-item.answered {
  background: #67c23a;
  border-color: #67c23a;
  color: white;
}

.nav-item.current {
  background: #409eff;
  border-color: #409eff;
  color: white;
  transform: scale(1.1);
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.3);
}

/* 快速跳转样式 */
.quick-jump {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 12px 0;
  border-top: 1px solid #ebeef5;
  flex-shrink: 0;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .floating-navigation {
    width: 280px;
    height: 60vh;
  }
  
  .floating-navigation.show {
    right: 10px;
  }
  
  .fixed-header {
    width: 100%;
  }
  
  .fixed-header .t-card__body {
    padding: 15px;
  }
  
  .exam-section {
    padding-top: 140px;
  }
  
  .type-questions {
    grid-template-columns: repeat(auto-fill, minmax(32px, 1fr));
  }
  
  .nav-item {
    width: 32px;
    height: 32px;
    font-size: 11px;
  }
}

@media (max-width: 480px) {
  .floating-navigation {
    width: calc(100vw - 20px);
    right: -100vw;
    top: 60%;
    height: 50vh;
  }
  
  .floating-navigation.show {
    right: 10px;
  }
  
  .exam-page {
    padding: 10px;
  }
  
  .fixed-header {
    width: 100%;
  }
  
  .fixed-header .t-card__body {
    padding: 10px;
  }
}

.exam-info-card {
  margin-bottom: 20px;
}

.fixed-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  width: 100%;
  z-index: 1000;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.fixed-header .t-card__body {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.exam-section {
  padding-top: 120px; /* 为固定头部留出空间 */
}

.exam-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.exam-title h3 {
  color: #333;
  margin: 0;
}

.exam-stats {
  display: flex;
  gap: 30px;
}

.stat-item {
  display: flex;
  align-items: center;
}

.label {
  color: #666;
  margin-right: 5px;
}

.time {
  font-weight: 600;
  color: #409EFF;
}

.time-warning {
  color: #F56C6C;
}

.progress, .answered {
  font-weight: 600;
  color: #333;
}

.question-card {
  margin-bottom: 20px;
}

.question-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid #eee;
}

.question-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.question-number {
  font-weight: 600;
  color: #333;
}

.question-content h3 {
  font-size: 18px;
  line-height: 1.6;
  margin-bottom: 20px;
  color: #333;
}

.option-group {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

/* 确保TDesign组件能正常显示和交互 */
:deep(.t-radio),
:deep(.t-checkbox) {
  margin-bottom: 12px !important;
  display: flex !important;
  align-items: flex-start !important;
  cursor: pointer !important;
  user-select: none !important;
}

:deep(.t-radio__input),
:deep(.t-checkbox__input) {
  margin-right: 8px !important;
  flex-shrink: 0 !important;
}

:deep(.t-radio__label),
:deep(.t-checkbox__label) {
  flex: 1 !important;
  line-height: 1.5 !important;
  cursor: pointer !important;
}

.option-item {
  padding: 15px;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  transition: all 0.3s;
  margin-bottom: 12px;
}

.option-item:hover {
  border-color: #409EFF;
  background-color: #f0f9ff;
}

/* 确保选中状态正确显示 */
:deep(.t-radio.t-is-checked .t-radio__input),
:deep(.t-checkbox.t-is-checked .t-checkbox__input) {
  color: #0052d9 !important;
}

/* 确保单选框和多选框的点击区域 */
:deep(.t-radio__input-wrap),
:deep(.t-checkbox__input-wrap) {
  cursor: pointer !important;
  display: inline-flex !important;
  align-items: center !important;
  justify-content: center !important;
}

.true-false-options {
  margin: 20px 0;
}

.tf-group {
  display: flex;
  justify-content: center;
  gap: 40px;
}

.tf-option {
  padding: 15px 30px;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  transition: all 0.3s;
  cursor: pointer !important;
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
}

.tf-option:hover {
  border-color: #409EFF;
  background-color: #f0f9ff;
}

/* 确保判断题的单选框能正常工作 */
:deep(.tf-group .t-radio) {
  margin: 0 !important;
  width: auto !important;
}

:deep(.tf-group .t-radio__input-wrap) {
  margin-right: 8px !important;
}

.fill-blank-options {
  margin: 20px 0;
}

.fill-input {
  width: 100%;
  font-size: 16px;
}

.essay-options {
  margin: 20px 0;
}

.essay-input {
  width: 100%;
  font-size: 16px;
}

.question-footer {
  text-align: center;
  margin-top: 30px;
  padding-top: 20px;
  border-top: 1px solid #eee;
}

.result-card {
  max-width: 800px;
  margin: 0 auto;
}

.result-header {
  text-align: center;
  margin-bottom: 30px;
}

.result-header h2 {
  color: #333;
  margin-bottom: 10px;
}

.result-header p {
  color: #666;
}

.result-stats {
  margin-bottom: 30px;
}

.stat-box {
  text-align: center;
  padding: 20px;
  border: 1px solid #eee;
  border-radius: 6px;
  width: 200px;
  margin: 0 auto;
}

.stat-value {
  font-size: 32px;
  font-weight: 600;
  color: #409EFF;
  margin-bottom: 5px;
}

.stat-label {
  color: #666;
  font-size: 14px;
}

.result-actions {
  text-align: center;
}

.question-list {
  display: grid;
  grid-template-columns: repeat(10, 1fr);
  gap: 10px;
  padding: 20px 0;
}

.question-item {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.3s;
  position: relative;
}

.question-item:hover {
  border-color: #409EFF;
  background-color: #f0f9ff;
}

.question-item.current {
  border-color: #409EFF;
  background-color: #409EFF;
  color: white;
}

.question-item.answered {
  background-color: #67C23A;
  border-color: #67C23A;
  color: white;
}

.question-num {
  font-size: 14px;
  font-weight: 600;
}

.question-status {
  position: absolute;
  top: -5px;
  right: -5px;
  background: #67C23A;
  border-radius: 50%;
  width: 16px;
  height: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.question-status .t-icon {
  font-size: 10px;
  color: white;
}

/* Exam Multiselect 自定义样式 */
:deep(.exam-multiselect-container) {
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

:deep(.exam-multiselect-container:hover) {
  border-color: #4dabf7;
}

:deep(.exam-multiselect-container.is-active) {
  border-color: #0052d9;
  box-shadow: 0 0 0 2px rgba(0, 82, 217, 0.1);
}

:deep(.exam-select-dropdown) {
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

:deep(.exam-select-dropdown.is-hidden) {
  display: none !important;
}

/* 确保 Exam 页面的 multiselect 基础样式正确 */
:deep(.exam-multiselect-container .multiselect) {
  min-height: 32px;
  height: 32px;
  width: 100%;
}

:deep(.exam-multiselect-container .multiselect-single-label) {
  padding-left: 12px;
  padding-right: 40px;
  line-height: 30px;
}

:deep(.exam-multiselect-container .multiselect-placeholder) {
  padding-left: 12px;
  line-height: 30px;
  color: #bbb;
}

:deep(.exam-multiselect-container .multiselect-caret) {
  margin-right: 12px;
}

/* 修复 Exam 页面可能的样式冲突 */
.exam-page :deep(.multiselect) {
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif !important;
  font-size: 14px !important;
  line-height: 1.5 !important;
}

/* 搜索区域布局样式 */
.search-area {
  margin-bottom: 20px;
}

.search-controls {
  display: flex;
  gap: 16px;
  align-items: center;
  flex-wrap: wrap;
}

.search-input {
  flex: 1;
  min-width: 200px;
  max-width: 400px;
}

.subject-select {
  width: 180px;
  flex-shrink: 0;
}

.search-button {
  flex-shrink: 0;
}

/* 移动端响应式 */
@media (max-width: 768px) {
  .search-controls {
    flex-direction: column;
    align-items: stretch;
  }

  .search-input,
  .subject-select,
  .search-button {
    width: 100%;
    max-width: none;
  }

  .search-button {
    margin-top: 8px;
  }
}

/* 平板端响应式 */
@media (max-width: 1024px) and (min-width: 769px) {
  .search-controls {
    flex-wrap: wrap;
  }

  .search-input {
    flex: 1;
    min-width: 250px;
  }

  .subject-select {
    width: 160px;
  }
}

/* 题目图片样式 */
.question-images {
  margin: 16px 0;
}

.image-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 12px;
  margin-top: 12px;
}

.image-item {
  position: relative;
  overflow: hidden;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1px solid #e4e7ed;
}

.image-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  border-color: #409eff;
}

.image-item img {
  width: 100%;
  height: 150px;
  object-fit: cover;
  display: block;
}

.image-item:hover img {
  opacity: 0.9;
}

/* 图片预览模态框样式 */
.image-preview-modal {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.8);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  cursor: pointer;
}

.image-preview-container {
  position: relative;
  max-width: 90%;
  max-height: 90%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.image-preview-container img {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
  border-radius: 8px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
}

.image-preview-close {
  position: absolute;
  top: -40px;
  right: 0;
  background: rgba(0, 0, 0, 0.6);
  color: white;
  border: none;
  border-radius: 50%;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s ease;
}

.image-preview-close:hover {
  background: rgba(0, 0, 0, 0.8);
  transform: scale(1.1);
}

/* 移动端图片样式 */
@media (max-width: 768px) {
  .image-grid {
    grid-template-columns: 1fr;
    gap: 8px;
  }
  
  .image-item img {
    height: 120px;
  }
  
  .image-preview-container {
    max-width: 95%;
    max-height: 95%;
  }
}

@media (max-width: 480px) {
  .image-grid {
    grid-template-columns: 1fr;
  }
  
  .question-images {
    margin: 12px 0;
  }
}
</style>

<style src="@vueform/multiselect/themes/default.css"></style>