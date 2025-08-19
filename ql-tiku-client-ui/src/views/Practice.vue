<template>
  <div class="practice-page page-container">
    <!-- 练习设置卡片 -->
    <PracticeSettings
      v-if="!currentQuestion"
      :subjects="subjects"
      :filters="practiceStore.filters"
      :practice-mode="practiceMode"
      :current-practice-record-id="currentPracticeRecordId"
      @start-practice="startPractice"
      @reset-filters="resetFilters"
      @reset-practice="resetPractice"
      @update:filters="updateFilters"
      @update:practice-mode="updatePracticeMode"
    />
    
    <!-- 练习完成卡片 -->
    <CompletionCard
      :show-completion="showCompletionCard"
      :stats="practiceStats"
      :loading="isRestarting"
      @restart="restartPractice"
      @go-home="goToHome"
      @reset-settings="resetPractice"
    />
    
    <!-- 题目显示区域 -->
    <div v-if="currentQuestion && !showCompletionCard" class="question-section">
      <div class="question-layout">
        <!-- 题目和答题区域 -->
        <div class="question-main">
          <!-- 题目答题合并卡片 -->
          <QuestionAnswerCard
            :question="currentQuestion"
            :user-answer="currentUserAnswer"
            :submitting="isSubmitting"
            :show-result="showResult"
            @answer-change="handleAnswerChange"
            @submit-answer="handleSubmitAnswer"
            @skip-question="handleSkipQuestion"
            @exit-practice="exitPractice"
            ref="answerAreaRef"
          />
          
          <!-- 答案解析卡片 -->
          <ResultDisplay
            :show-result="showResult"
            :is-correct="isCorrect"
            :show-ai-grading-button="!showAiGradingCard && currentQuestion?.questionType >= 3"
            :ai-grading-loading="isAiGrading"
            @toggle-ai-grading="handleToggleAiGrading"
            :user-answer="currentUserAnswer"
            :correct-answer="currentQuestion?.correctAnswer"
            :analysis="currentQuestion?.analysis"
            :question-type="currentQuestion?.questionType"
            :options="currentQuestion?.optionList || []"
            :knowledge-points="currentQuestion?.knowledgePointList || []"
            @next-question="getNextQuestion"
            @ai-grading="aiGrading"
          />

          <!-- AI判题展开/收起按钮 -->
            <div v-if="currentQuestion?.questionType >= 3" class="ai-grading-toggle">
              <t-button 
                @click="toggleAiGradingCard"
                variant="outline"
                size="small"
                class="toggle-ai-grading-btn"
                >
              <t-icon :name="showAiGradingCard ? 'chevron-up' : 'chevron-down'" />
                {{ showAiGradingCard ? '收起AI智能判题卡片' : '展开AI智能判题卡片' }}
                </t-button>
            </div>

          <!-- AI判题结果卡片 - 根据showAiGradingCard状态显示/隐藏 -->
          <AiGradingChat
            v-if="currentQuestion && showAiGradingCard"
            :key="`ai-grading-${currentQuestion.id}-${Date.now()}`"
            :gradingText="aiGradingText"
            :isGrading="isAiGrading"
            :gradingResult="aiGradingResult"
            :hasHistoryRecord="hasHistoryRecord"
            :historyRecords="historyRecords"
            :disabled="currentQuestion?.questionType < 3"
            @regrade="handleRegrade"
            @close="closeAiGradingCard"
          />
        </div>
        
        <!-- AI助手卡片展开/收起按钮 -->
        <div v-if="currentQuestion" class="ai-assistant-toggle">
          <t-button 
            @click="toggleAiAssistantCard"
            variant="outline"
            size="small"
            class="toggle-ai-assistant-btn"
          >
            <t-icon :name="showAiAssistantCard ? 'chevron-up' : 'chevron-down'" />
            {{ showAiAssistantCard ? '收起AI助手' : '展开AI助手' }}
          </t-button>
        </div>

        <!-- AI聊天卡片 -->
        <div v-if="showAiAssistantCard" class="ai-chat-sidebar">
          <t-card class="ai-chat-card">
            <template #header>
              <div class="ai-card-header">
                <div class="ai-header-content">
                  <div class="ai-header-title">AI助手</div>
                  <!-- AI操作区域 - 按钮和模型选择器并排 -->
                  <div class="ai-controls-row">
                    <!-- AI解析按钮 - 靠左 -->
                    <t-button 
                      @click="handleAiAction"
                      :disabled="!currentQuestion || isAnalyzing || isAiGrading"
                      :loading="isAnalyzing || isAiGrading"
                      class="ai-analyze-btn"
                    >
                      <t-icon name="lightbulb" />
                      AI一键解析
                    </t-button>
                    
                    <!-- 模型选择器 - 靠右 -->
                    <div class="model-selector-wrapper">
                      <div class="model-selector-label">模型:</div>
                      <Multiselect
                        v-model="selectedModelId"
                        :options="modelOptions"
                        value-prop="value"
                        label="label"
                        placeholder="选择AI模型"
                        :can-clear="false"
                        :searchable="false"
                        @change="handleModelChange"
                        :classes="{
                          container: 'ai-chat-multiselect-container',
                          dropdown: 'ai-chat-select-dropdown'
                        }"
                        class="model-selector"
                      />
                    </div>
                  </div>
                  

                </div>
              </div>
            </template>
            
            <div class="ai-chat-wrapper">
              <TDesignAiChat 
                ref="aiChatRef"
                :session-id="aiChatStore.currentSessionId"
                :model-id="String(selectedModelId)"
                @session-cleared="handleSessionCleared"
              />
            </div>
          </t-card>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted, watch, onUnmounted, nextTick } from 'vue'
import { MessagePlugin } from 'tdesign-vue-next'
import { questionAPI, wrongBookAPI, subjectAPI, practiceRecordAPI, aiQuotaAPI, aiModelAPI } from '@/api'
import * as questionApi from '@/api/question'
import axios from 'axios'
import Multiselect from '@vueform/multiselect'

// 导入组件
import PracticeSettings from '../components/PracticeSettings.vue'
import QuestionAnswerCard from '../components/QuestionAnswerCard.vue'
import ResultDisplay from '../components/ResultDisplay.vue'
import CompletionCard from '../components/CompletionCard.vue'
import TDesignAiChat from '../components/TDesignAiChat.vue'
import AiGradingChat from '../components/AiGradingChat.vue'

// 导入store
import { usePracticeStore } from '../store/practice'
import { useAiChatStore } from '../store/aiChat'

export default {
  name: 'Practice',
  components: {
    PracticeSettings,
    QuestionAnswerCard,
    ResultDisplay,
    CompletionCard,
    TDesignAiChat,
    AiGradingChat,
    Multiselect
  },
  setup() {
    const practiceStore = usePracticeStore()
    const aiChatStore = useAiChatStore()
    
    // 获取当前用户ID
    const currentUserId = computed(() => {
      const userInfo = localStorage.getItem('userInfo')
      if (userInfo) {
        try {
          const user = JSON.parse(userInfo)
          return user.id || user.userId
        } catch (e) {
          console.error('解析用户信息失败:', e)
        }
      }
      return null
    })
    
    // 监听用户ID变化，同步到aiChatStore
    watch(currentUserId, (newUserId) => {
      if (newUserId) {
        aiChatStore.currentUserId = newUserId
      }
    })
    
    // 基础数据
    const subjects = ref([])
    const practiceMode = ref('random')
    const showCompletionCard = ref(false)
    const practiceStats = ref({
      totalQuestions: 0,
      correctCount: 0,
      wrongCount: 0,
      accuracyRate: 0,
      score: 0
    })
    
    // 答题相关状态
    const currentUserAnswer = ref('')
    const isSubmitting = ref(false)
    const isRestarting = ref(false)
    
    // AI相关状态
    const aiChatRef = ref(null)
    const answerAreaRef = ref(null)
    const isAnalyzing = ref(false)
    const isAiGrading = ref(false)
    const quotaInfo = ref(null)
    const remainingQuota = ref(0)
    const selectedModelId = ref('')
    const modelOptions = ref([])
    
    // AI判题结果状态
    const aiGradingResult = ref(null)
    const showAiGradingCard = ref(true) // 默认显示AI判题卡片
    const aiGradingText = ref('')
    const displayedText = ref('')
    const typewriterTimer = ref(null)
    
    // AI助手卡片显示状态
    const showAiAssistantCard = ref(true) // 默认显示AI助手卡片
    
    // 切换AI助手卡片显示状态
    const toggleAiAssistantCard = () => {
      console.log('🔄 toggleAiAssistantCard被调用，当前状态:', showAiAssistantCard.value)
      showAiAssistantCard.value = !showAiAssistantCard.value
      console.log('🔄 切换后的状态:', showAiAssistantCard.value)
    }
     
    // AI判题历史记录状态
    const hasHistoryRecord = ref(false)
    const historyRecord = ref(null)
    const historyRecords = ref([])
     
    // 刷题记录状态
    const currentPracticeRecordId = ref(null)
    const practiceStartTime = ref(null)
    
    // 使用store中的状态
    const currentQuestion = computed(() => practiceStore.currentQuestion)
    const showResult = computed(() => practiceStore.showResult)
    const isCorrect = computed(() => practiceStore.isCorrect)
    const filters = computed(() => practiceStore.filters)
    
    // 获取科目列表
    const getSubjects = async () => {
      try {
        console.log('🌐 Practice.vue: 开始获取科目列表')
        
        const response = await subjectAPI.getEnabledSubjects()
        console.log('✅ Practice.vue: 科目数据获取成功:', response)
        
        if (Array.isArray(response)) {
          console.log('📊 Practice.vue: 科目数量:', response.length)
          subjects.value = response
        } else if (response.code === 200 && Array.isArray(response.data)) {
          console.log('📊 Practice.vue: 科目数量:', response.data.length)
          subjects.value = response.data
        } else {
          console.error('❌ Practice.vue: 获取科目数据格式错误:', response)
          subjects.value = [
            { id: 1, name: '语文', code: 'chinese', enabled: true },
            { id: 2, name: '数学', code: 'math', enabled: true },
            { id: 3, name: '英语', code: 'english', enabled: true },
            { id: 4, name: '物理', code: 'physics', enabled: true },
            { id: 5, name: '化学', code: 'chemistry', enabled: true }
          ]
        }
        
        // 确保题目显示正确的科目名称
        if (currentQuestion.value && !currentQuestion.value.subjectName) {
          const subject = subjects.value.find(s => s.id === currentQuestion.value.subjectId)
          if (subject) {
            currentQuestion.value.subjectName = subject.name
          }
        }
      } catch (error) {
        console.error('❌ Practice.vue: 获取科目列表失败:', error)
        subjects.value = [
          { id: 1, name: '语文', code: 'chinese', enabled: true },
          { id: 2, name: '数学', code: 'math', enabled: true },
          { id: 3, name: '英语', code: 'english', enabled: true },
          { id: 4, name: '物理', code: 'physics', enabled: true },
          { id: 5, name: '化学', code: 'chemistry', enabled: true }
        ]
      }
    }
    
    // 更新筛选条件
    const updateFilters = async (newFilters) => {
      console.log('🔄 Practice.vue: 更新筛选条件', newFilters)
      console.log('🔍 更新前的filters:', practiceStore.filters)
      
      // 确保数据正确同步
      Object.assign(practiceStore.filters, newFilters)
      
      // 重置练习缓存
      try {
        const params = {
          mode: practiceMode.value,
          subjectId: newFilters.subjectId || practiceStore.filters.subjectId
        }
        
        if (newFilters.questionType) {
          const typeMap = {
            'SINGLE_CHOICE': 0,
            'MULTIPLE_CHOICE': 1,
            'TRUE_FALSE': 2
          }
          params.questionType = typeMap[newFilters.questionType]
        }
        
        if (newFilters.difficulty) {
          const difficultyMap = {
            'EASY': 1,
            'MEDIUM': 2,
            'HARD': 3
          }
          params.difficulty = difficultyMap[newFilters.difficulty]
        }
        
        await questionAPI.resetPracticeCache(params)
        console.log('✅ 练习缓存已重置')
      } catch (error) {
        console.warn('重置练习缓存失败:', error)
      }
      
      console.log('✅ 更新后的filters:', practiceStore.filters)
    }
    
    // 更新练习模式
    const updatePracticeMode = (mode) => {
      practiceMode.value = mode
      // 切换模式时重置练习记录，让后端重新生成缓存
      resetPractice()
    }
    
    // 处理答案变化
    const handleAnswerChange = (answer) => {
      currentUserAnswer.value = answer
    }

    // 安全的消息提示包装函数
    const showMessage = (type, message, options = {}) => {
      try {
        // 确保在DOM更新后显示消息
        nextTick(() => {
          MessagePlugin[type](message, options)
        })
      } catch (error) {
        console.error('MessagePlugin调用失败:', error)
        // 降级方案：使用原生alert
        alert(message)
      }
    }
    
    // 处理提交答案
    const handleSubmitAnswer = async (answer) => {
      await submitAnswer(answer)
    }
    
    // 处理跳过题目
    const handleSkipQuestion = async () => {
      await getNextQuestion()
    }
    
    // 开始练习
    const startPractice = async () => {
      try {
        console.log('🎯 Practice.vue: 开始练习被调用')
        console.log('🔍 当前筛选条件:', filters.value)
        
        // 验证科目选择 - 检查多个可能的值
        const subjectId = filters.value.subjectId || practiceStore.filters.subjectId
        console.log('🔍 科目ID检查:', {
          'filters.value.subjectId': filters.value.subjectId,
          'practiceStore.filters.subjectId': practiceStore.filters.subjectId,
          'final subjectId': subjectId
        })
        
        if (!subjectId) {
          console.log('❌ 科目未选择，显示警告')
          MessagePlugin.warning('请先选择科目')
          return
        }
        
        // 确保使用正确的科目ID
        if (subjectId && !filters.value.subjectId) {
          console.log('🔧 修复科目ID:', subjectId)
          practiceStore.filters.subjectId = subjectId
        }
        
        console.log('✅ 科目验证通过，开始获取题目')
        console.log('🎯 开始练习 - 当前筛选条件:', {
          subjectId: filters.value.subjectId,
          type: filters.value.type,
          difficulty: filters.value.difficulty,
          mode: practiceMode.value
        })
        
        // 重置完成状态
        showCompletionCard.value = false
        
        if (!currentPracticeRecordId.value) {
          console.log('📝 创建练习记录...')
          await createPracticeRecord()
        }
        
        const params = {
          count: 1,
          mode: practiceMode.value,
          subjectId: filters.value.subjectId // 确保科目ID始终传递
        }
        
        if (filters.value.type) {
          const typeMap = {
            'SINGLE_CHOICE': 0,
            'MULTIPLE_CHOICE': 1,
            'TRUE_FALSE': 2
          }
          params.questionType = typeMap[filters.value.type]
          console.log('📋 题目类型映射:', filters.value.type, '->', params.questionType)
        }
        
        if (filters.value.difficulty) {
          const difficultyMap = {
            'EASY': 1,
            'MEDIUM': 2,
            'HARD': 3
          }
          params.difficulty = difficultyMap[filters.value.difficulty]
          console.log('⭐ 难度映射:', filters.value.difficulty, '->', params.difficulty)
        }
        
        console.log('📡 发送题目请求参数:', params)
        
        const questions = await questionAPI.getPracticeQuestions(params)
        
        console.log('📥 获取到的题目数量:', questions.length)
        console.log('📥 获取到的题目详情:', questions)
        
        if (questions.length > 0) {
          const question = questions[0]
          console.log('✅ 设置当前题目:', {
            id: question.id,
            title: question.title,
            subjectId: question.subjectId,
            subjectName: question.subjectName,
            content: question.content?.substring(0, 50) + '...',
            knowledgePoints: question.knowledgePoints,
            knowledgePointList: question.knowledgePointList
          })
          
          practiceStore.setCurrentQuestion(question)
          resetAnswerArea()
          console.log('🎉 题目设置完成，应该显示题目界面')
        } else {
          console.log('⚠️ 没有找到符合条件的题目')
          MessagePlugin.warning('没有找到符合条件的题目，请调整筛选条件')
          showCompletionCard.value = true
          await fetchPracticeStats()
        }
      } catch (error) {
        console.error('❌ 获取题目失败:', error)
        MessagePlugin.error('获取题目失败: ' + error.message)
      }
    }
    
    // 获取下一题
    const getNextQuestion = async () => {
      try {
        // 切换题目时不要隐藏结果显示，让用户能看到上一题的状态
        // practiceStore.setShowResult(false)
        console.log('🔄 切换题目时保持结果显示状态')
        if (typewriterTimer.value) {
          clearInterval(typewriterTimer.value)
          typewriterTimer.value = null
        }
        
        // 验证科目选择
        if (!filters.value.subjectId) {
          MessagePlugin.warning('科目信息丢失，请重新选择科目')
          practiceStore.setCurrentQuestion(null)
          return
        }
        
        if (!currentPracticeRecordId.value) {
          await createPracticeRecord()
        }
        
        const params = {
          count: 1,
          mode: practiceMode.value,
          subjectId: filters.value.subjectId // 确保科目ID始终传递
        }
        
        if (filters.value.type) {
          const typeMap = {
            'SINGLE_CHOICE': 0,
            'MULTIPLE_CHOICE': 1,
            'TRUE_FALSE': 2
          }
          params.questionType = typeMap[filters.value.type]
        }
        
        if (filters.value.difficulty) {
          const difficultyMap = {
            'EASY': 1,
            'MEDIUM': 2,
            'HARD': 3
          }
          params.difficulty = difficultyMap[filters.value.difficulty]
        }
        
        console.log('📡 获取下一题参数:', params)
        
        const questions = await questionAPI.getPracticeQuestions(params)
        
        console.log('📥 获取到的下一题:', questions)
        
        if (questions.length > 0) {
          const question = questions[0]
          
          // 确保题目有正确的科目名称
          if (!question.subjectName && question.subjectId) {
            const subject = subjects.value.find(s => s.id === question.subjectId)
            if (subject) {
              question.subjectName = subject.name
            }
          }
          
          console.log('✅ 下一题信息:', {
            id: question.id,
            title: question.title,
            subjectId: question.subjectId,
            subjectName: question.subjectName,
            content: question.content?.substring(0, 50) + '...'
          })
          
          practiceStore.setCurrentQuestion(question)
          resetAnswerArea()
        } else {
          console.log('⚠️ 没有更多题目了')
          MessagePlugin.info('恭喜！您已完成所有符合条件的题目')
          showCompletionCard.value = true
          practiceStore.setCurrentQuestion(null)
          await fetchPracticeStats()
        }
      } catch (error) {
        console.error('❌ 获取下一题失败:', error)
        MessagePlugin.error('获取下一题失败: ' + error.message)
      }
    }
    
    // 提交答案
    const submitAnswer = async (userAnswer) => {
      try {
        isSubmitting.value = true
        
        if (!currentPracticeRecordId.value) {
          await createPracticeRecord()
        }
        
        console.log('提交答案调试信息:', {
          questionId: currentQuestion.value.id,
          questionType: currentQuestion.value.questionType,
          userAnswer: userAnswer,
          correctAnswer: currentQuestion.value.correctAnswer
        })
        
        let isCorrect = false
        
        if (currentQuestion.value.questionType >= 3) {
          // 简答题：不自动判题，显示"待批改"状态，等待用户手动点击AI判题
          // 确保用户答案被正确保存
          currentUserAnswer.value = userAnswer
          practiceStore.setIsCorrect(null)
          practiceStore.setShowResult(true)
          console.log('🔍 简答题状态设置:', { 
            showResult: true, 
            isCorrect: null, 
            userAnswer: userAnswer,
            currentUserAnswer: currentUserAnswer.value 
          })
          isCorrect = null
          
          // 显示待批改提示
          showMessage('info', '✏️ 答案已提交，待交给AI智能判题')
          
          // 记录简答题答题详情（不判对错，等待AI判题）
          try {
            await addPracticeDetail(userAnswer, null)
          } catch (e) {
            console.warn('记录简答题答题详情失败（忽略）:', e)
          }
          
          // 不自动调用 aiGrading，交由用户手动触发
          return
        } else {
          const result = await questionAPI.submitAnswer({
            questionId: currentQuestion.value.id,
            userAnswer
          })
          
          console.log('后端返回结果:', result)
          
          // 确保正确设置结果状态
          isCorrect = result.data ? result.data.correct : result.correct
          practiceStore.setIsCorrect(isCorrect)
          practiceStore.setShowResult(true)
          console.log('🔍 其他题型状态设置:', { showResult: true, isCorrect, result })

          console.log('设置答题结果:', {
            isCorrect,
            showResult: practiceStore.showResult
          })

          // 显示答题结果提示
          if (isCorrect) {
            showMessage('success', '🎉 回答正确！太棒了！')
          } else {
            showMessage('error', '❌ 回答错误，继续加油！')
          }

          // 记录答题详情（客观题）
          try {
            await addPracticeDetail(userAnswer, isCorrect)
          } catch (e) {
            console.warn('记录客观题答题详情失败（忽略）:', e)
          }

          if (!isCorrect) {
            try {
              await wrongBookAPI.addWrongQuestion({
                questionId: currentQuestion.value.id,
                userAnswer: userAnswer,
                correctAnswer: currentQuestion.value.correctAnswer
              })
            } catch (error) {
              console.error('添加错题到错题本失败:', error)
            }
          }

          practiceStore.updateStats()
        }
        
        await updatePracticeRecord()
        
        // 立即刷新统计数据
        await fetchPracticeStats()
        
      } catch (error) {
        console.error('提交答案失败:', error)
        showMessage('error', '提交答案失败')
      } finally {
        isSubmitting.value = false
      }
    }
    
    // 退出练习
    const exitPractice = () => {
      practiceStore.resetPractice()
      practiceStore.setCurrentQuestion(null)
      // 重置练习时才隐藏结果显示
      practiceStore.setShowResult(false)
      resetAnswerArea()
      console.log('🔄 重置练习，隐藏结果显示')
    }
    
    // 重置答题区域
    const resetAnswerArea = () => {
      currentUserAnswer.value = ''
      if (answerAreaRef.value && answerAreaRef.value.resetAnswer) {
        answerAreaRef.value.resetAnswer()
      }
    }
    
    // 重置筛选条件
    const resetFilters = () => {
      practiceStore.resetFilters()
    }
    
    // 获取当前练习统计数据
    const fetchPracticeStats = async () => {
      try {
        if (currentPracticeRecordId.value) {
          const stats = await practiceRecordAPI.getPracticeRecordStats(currentPracticeRecordId.value)
          console.log('📊 获取到的统计数据:', stats)
          
          // 支持多种可能的字段名映射
          practiceStats.value = {
            totalQuestions: stats.totalQuestions || stats.total_questions || stats.questionCount || 0,
            correctCount: stats.correctCount || stats.correct_count || stats.rightCount || 0,
            wrongCount: stats.wrongCount || stats.wrong_count || stats.errorCount || 0,
            accuracyRate: stats.accuracyRate || stats.accuracy_rate || stats.correctRate || 0,
            score: stats.score || stats.totalScore || 0
          }
          
          // 确保正确率是一个合理的百分比值
          if (practiceStats.value.accuracyRate > 1 && practiceStats.value.accuracyRate <= 100) {
            practiceStats.value.accuracyRate = practiceStats.value.accuracyRate
          } else if (practiceStats.value.accuracyRate > 0 && practiceStats.value.accuracyRate <= 1) {
            practiceStats.value.accuracyRate = Math.round(practiceStats.value.accuracyRate * 100)
          }
          
          // 计算错题数（如果没有提供）
          if (practiceStats.value.wrongCount === 0 && practiceStats.value.totalQuestions > 0) {
            practiceStats.value.wrongCount = practiceStats.value.totalQuestions - practiceStats.value.correctCount
          }
        } else {
          practiceStats.value = {
            totalQuestions: 0,
            correctCount: 0,
            wrongCount: 0,
            accuracyRate: 0,
            score: 0
          }
        }
      } catch (error) {
        console.error('获取练习统计失败:', error)
        practiceStats.value = {
          totalQuestions: 0,
          correctCount: 0,
          wrongCount: 0,
          accuracyRate: 0,
          score: 0
        }
      }
    }
    
    // 重置练习
    const resetPractice = () => {
      currentPracticeRecordId.value = null
      practiceStartTime.value = null
      practiceStore.setCurrentQuestion(null)
      showCompletionCard.value = false
      showMessage('success', '已重置练习，可以重新开始刷题')
    }
     
    // 再来一次
    const restartPractice = async () => {
      isRestarting.value = true
      try {
        showCompletionCard.value = false
        resetPractice()
        await startPractice()
      } finally {
        isRestarting.value = false
      }
    }
     
    // 返回首页
    const goToHome = () => {
      window.location.href = '/'
    }
    
    // 处理AI操作 - AI一键解析功能（适用于所有题目类型）
    const handleAiAction = async () => {
      if (!currentQuestion.value) {
        showMessage('warning', '请先选择题目')
        return
      }
      
      // AI一键解析 - 适用于所有题目类型
      // 这个功能是分析题目内容、提供解题思路，不是判题
      await analyzeCurrentQuestion()
    }
    
    // AI解析当前题目
    const analyzeCurrentQuestion = async () => {
      if (!currentQuestion.value) {
        showMessage('warning', '请先选择题目')
        return
      }
      
      isAnalyzing.value = true
      
      try {
        // 构建题目内容
        let questionContent = `题目：${currentQuestion.value.content}\n`
        
        // 添加选项（如果有）
        if (currentQuestion.value.optionList && currentQuestion.value.optionList.length > 0) {
          questionContent += '选项：\n'
          currentQuestion.value.optionList.forEach(option => {
            questionContent += `${option.key}. ${option.value}\n`
          })
        }
        
        // 添加题目类型和难度信息
        questionContent += `\n题目类型：${currentQuestion.value.questionTypeName || '未知'}\n`
        questionContent += `难度等级：${currentQuestion.value.difficultyName || '未知'}\n`
        questionContent += `科目：${currentQuestion.value.subjectName || '未知'}`
        
        // 如果已经答题，添加用户答案和正确答案
        if (showResult.value) {
          questionContent += `\n\n我的答案：${currentUserAnswer.value}`
          questionContent += `\n正确答案：${currentQuestion.value.correctAnswer}`
        }
        
        console.log('=== 开始AI解析题目 ===')
        console.log('题目内容:', questionContent)
        console.log('当前会话ID:', aiChatStore.currentSessionId)
        
        // 确保有AI会话ID
        if (!aiChatStore.currentSessionId) {
          console.log('创建新的AI会话...')
          await aiChatStore.createSession()
          console.log('新会话ID:', aiChatStore.currentSessionId)
        }
        
        // 调用AI聊天组件的analyzeQuestion方法
        if (aiChatRef.value && typeof aiChatRef.value.analyzeQuestion === 'function') {
          console.log('调用AI聊天组件的analyzeQuestion方法...')
          await aiChatRef.value.analyzeQuestion(questionContent)
          console.log('AI解析调用完成')
          MessagePlugin.success('AI正在分析题目，请查看右侧聊天窗口')
          
        } else {
          // 降级方案 - 直接调用后端接口
          console.log('AI聊天组件方法不可用，使用降级方案直接调用后端接口')
          
          try {
            const token = localStorage.getItem('token')
            const response = await fetch('/api/ai/analyze/stream', {
              method: 'POST',
              headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
              },
              body: JSON.stringify({
                sessionId: aiChatStore.currentSessionId,
                questionContent: questionContent,
                questionId: currentQuestion.value.id
              })
            })
            
            if (response.ok) {
              console.log('直接调用后端AI解析接口成功')
              MessagePlugin.success('AI正在分析题目，请查看右侧聊天窗口')
              
              // 解析成功后刷新配额信息
              await getQuotaInfo()
            } else {
              throw new Error(`HTTP ${response.status}`)
            }
          } catch (backendError) {
            console.error('直接调用后端接口也失败:', backendError)
            MessagePlugin.error('AI解析服务暂时不可用，请稍后重试')
          }
        }
        
      } catch (error) {
        console.error('AI解析失败:', error)
        MessagePlugin.error('AI解析失败，请稍后重试: ' + error.message)
      } finally {
        isAnalyzing.value = false
      }
    }
    
    // 处理会话清空
    const handleSessionCleared = async () => {
      try {
        await aiChatStore.createSession()
        MessagePlugin.success('新对话已创建')
      } catch (error) {
        console.error('创建新对话失败:', error)
        MessagePlugin.error('创建新对话失败')
      }
    }
    
    // 获取AI配额信息
    const getQuotaInfo = async () => {
      try {
        console.log('正在加载AI配额信息...')
        const response = await aiQuotaAPI.getQuotaInfo()
        console.log('配额信息响应:', response)
        quotaInfo.value = response.data
        
        const remainingResponse = await aiQuotaAPI.getRemainingQuota()
        console.log('剩余配额响应:', remainingResponse)
        remainingQuota.value = remainingResponse.data
      } catch (error) {
        console.error('加载AI配额信息失败:', error)
        quotaInfo.value = { dailyQuota: 10, usedQuota: 0 }
        remainingQuota.value = 10
      }
    }
    
    // 加载AI模型列表
    const loadModelOptions = async () => {
      try {
        console.log('🤖 Practice.vue: 开始加载AI模型列表')
        const models = await aiModelAPI.getEnabledModels()
        console.log('🤖 Practice.vue: 模型列表响应:', models)
        
        if (models && Array.isArray(models) && models.length > 0) {
          modelOptions.value = models.map(model => ({
            value: model.id,
            label: model.name,
            description: model.description
          }))
          
          if (modelOptions.value.length > 0 && !selectedModelId.value) {
            selectedModelId.value = modelOptions.value[0].value
            console.log('🤖 Practice.vue: 设置默认模型:', selectedModelId.value)
          }
        } else {
          console.warn('⚠️ Practice.vue: 没有找到启用的AI模型，使用默认配置')
          modelOptions.value = [
            { value: 1, label: '通义千问-Turbo', description: '快速响应模型' },
            { value: 2, label: '通义千问-Plus', description: '平衡性能模型' },
            { value: 3, label: '通义千问-Max', description: '高性能模型' }
          ]
          selectedModelId.value = 1
        }
        
        console.log('🤖 Practice.vue: 最终模型选项:', modelOptions.value)
      } catch (error) {
        console.error('❌ Practice.vue: 加载AI模型列表失败:', error)
        modelOptions.value = [
          { value: 1, label: '通义千问-Turbo', description: '快速响应模型' },
          { value: 2, label: '通义千问-Plus', description: '平衡性能模型' },
          { value: 3, label: '通义千问-Max', description: '高性能模型' }
        ]
        selectedModelId.value = 1
        MessagePlugin.warning('使用默认AI模型配置')
      }
    }
    
    // 处理模型切换
    const handleModelChange = (value) => {
      console.log('🔄 Practice.vue: 模型切换事件触发:', value, typeof value)
      
      let modelId = value
      if (value && typeof value === 'object' && value.value !== undefined) {
        modelId = value.value
      }
      
      console.log('🔄 Practice.vue: 解析后的模型ID:', modelId, typeof modelId)
      
      if (modelId !== undefined && modelId !== null) {
        selectedModelId.value = modelId
        
        const selectedModel = modelOptions.value.find(model => model.value === modelId)
        if (selectedModel) {
          console.log('✅ Practice.vue: 模型切换成功:', selectedModel.label)
          MessagePlugin.info(`已切换到模型: ${selectedModel.label}`)
        }
      } else {
        console.warn('⚠️ Practice.vue: 模型切换参数无效:', value)
      }
    }
    
    // 切换AI判题卡片显示状态
    const toggleAiGradingCard = () => {
      console.log('🔄 toggleAiGradingCard被调用，当前状态:', showAiGradingCard.value)
      showAiGradingCard.value = !showAiGradingCard.value
      console.log('🔄 切换后的状态:', showAiGradingCard.value)
      
      if (typewriterTimer.value) {
        clearInterval(typewriterTimer.value)
        typewriterTimer.value = null
      }
    }
    
    // 处理展开AI判题卡片按钮点击
    const handleToggleAiGrading = () => {
      console.log('🔄 Practice.vue: handleToggleAiGrading被调用，当前状态:', showAiGradingCard.value)
      showAiGradingCard.value = true
      console.log('✅ Practice.vue: AI判题卡片已展开')
    }
    
    // 关闭AI判题卡片
    const closeAiGradingCard = () => {
      console.log('🔍 Practice.vue: closeAiGradingCard被调用')
      showAiGradingCard.value = false
      
      if (typewriterTimer.value) {
        clearInterval(typewriterTimer.value)
        typewriterTimer.value = null
      }
    }
    
    // 重新判题方法
    const handleRegrade = () => {
      console.log('🔄 Practice.vue: 收到重新判题请求')
      aiGradingText.value = ''
      aiGradingResult.value = null
      aiGrading()
    }

    // AI判题功能
    const aiGrading = async () => {
      if (!currentQuestion.value || currentQuestion.value.questionType < 3) {
        MessagePlugin.warning('只有简答题支持AI判题')
        return
      }
      
      if (!currentUserAnswer.value.trim()) {
        MessagePlugin.warning('请先输入答案')
        return
      }
      
      showAiGradingCard.value = true
      isAiGrading.value = true
      aiGradingText.value = ''
      aiGradingResult.value = null
      
      try {
        console.log('=== 开始AI流式判题 ===')
        
        const token = localStorage.getItem('token')
        const base = process.env.VUE_APP_BASE_API || '/api'
        
        // 先调用非流式判题获取结果
        console.log('🔄 步骤1: 调用非流式AI判题接口获取结果...')
        const gradingResult = await handleNonStreamingGrading()
        
        if (gradingResult) {
          console.log('🎯 非流式判题结果:', gradingResult)
          
          // 更新 ResultDisplay 状态
          practiceStore.setIsCorrect(gradingResult.isCorrect)
          aiGradingResult.value = gradingResult
          
          console.log('✅ ResultDisplay 状态已更新:', {
            isCorrect: gradingResult.isCorrect,
            showResult: practiceStore.showResult
          })
          
              // 显示AI判题结果提示
          if (gradingResult.isCorrect) {
            showMessage('success', '🎉 AI判题完成：回答正确！')
          } else {
            showMessage('error', '❌ AI判题完成：回答错误，继续努力！')
          }
          
          // 如果非流式判题返回了gradingText，直接显示
          if (gradingResult.gradingText) {
            aiGradingText.value = gradingResult.gradingText
            console.log('📝 设置AI判题文本:', gradingResult.gradingText.substring(0, 100) + '...')
          }
        }
        
        // 然后启动流式显示（如果需要）
        console.log('🔄 步骤2: 启动流式显示...')
        try {
          await handleStreamingGrading(token, base)
        } catch (streamError) {
          console.warn('⚠️ 流式显示失败，但非流式判题已完成:', streamError)
        }
        
        // 记录答题详情
        if (!currentPracticeRecordId.value) {
          await createPracticeRecord()
        }
        
        await addPracticeDetail(
          currentUserAnswer.value.trim(),
          gradingResult?.isCorrect || false,
          aiGradingText.value,
          gradingResult?.score || 0
        )
        
        await updatePracticeRecord()
        
        // AI判题完成后立即刷新统计数据
        await fetchPracticeStats()
        
      } catch (error) {
        console.error('AI判题失败:', error)
        aiGradingText.value = '抱歉，AI判题服务暂时不可用，请稍后重试。'
      } finally {
        isAiGrading.value = false
      }
    }
    
    // 处理流式判题显示
    const handleStreamingGrading = async (token, base) => {
      const response = await fetch(`${base}/question/ai-grading-stream`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json',
          'Accept': 'text/event-stream',
          'Cache-Control': 'no-cache'
        },
        body: JSON.stringify({
          questionId: currentQuestion.value.id,
          questionContent: currentQuestion.value.content,
          userAnswer: currentUserAnswer.value.trim(),
          correctAnswer: currentQuestion.value.correctAnswer
        })
      })
      
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`)
      }
      
      const reader = response.body.getReader()
      const decoder = new TextDecoder('utf-8')
      let gradingContent = ''
      
      try {
        let buffer = ''
        
        while (true) {
          const { done, value } = await reader.read()
          
          if (done) break
          
          if (!value || value.length === 0) continue
          
          const chunk = decoder.decode(value, { stream: true })
          buffer += chunk
          
          const lines = buffer.split('\n')
          buffer = lines.pop() || ''
          
          for (const line of lines) {
            if (!line.trim()) continue
            
            if (line.startsWith('data: ')) {
              let data = line.slice(6).trim()
              
              if (data === '[DONE]') break
              
              if (data && data !== '') {
                try {
                  const parsedData = JSON.parse(data)
                  
                  if (parsedData.content) {
                    gradingContent += parsedData.content
                    aiGradingText.value = gradingContent
                    
                    if (!showAiGradingCard.value) {
                      showAiGradingCard.value = true
                    }
                  }
                } catch (e) {
                  gradingContent += data
                  aiGradingText.value = gradingContent
                }
              }
            }
          }
        }
        
        if (buffer.trim()) {
          if (buffer.startsWith('data: ')) {
            let data = buffer.slice(6).trim()
            if (data && data !== '[DONE]') {
              try {
                const parsedData = JSON.parse(data)
                if (parsedData.content) {
                  gradingContent += parsedData.content
                  aiGradingText.value = gradingContent
                }
              } catch (e) {
                gradingContent += data
                aiGradingText.value = gradingContent
              }
            }
          }
        }
        
        reader.releaseLock()
        
      } catch (streamError) {
        console.error('AI判题流式读取失败:', streamError)
        throw streamError
      }
    }
    
    // 处理非流式判题（用于状态更新）
    const handleNonStreamingGrading = async () => {
      try {
        console.log('🔄 调用非流式AI判题接口...')
        const response = await questionAPI.aiGrading({
          questionId: currentQuestion.value.id,
          questionContent: currentQuestion.value.content,
          userAnswer: currentUserAnswer.value.trim(),
          correctAnswer: currentQuestion.value.correctAnswer
        })
        
        console.log('📥 非流式判题响应:', response)
        
        console.log('📥 非流式判题完整响应:', response)
        
        // 处理不同的响应格式
        if (response.code === 200 && response.data) {
          // 标准格式
          const data = response.data
          return {
            isCorrect: data.isCorrect,
            score: data.score,
            gradingText: data.gradingResult || data.gradingText,
            recordId: data.recordId
          }
        } else if (response.recordId && response.gradingResult) {
          // 直接格式（从日志看到的格式）
          return {
            isCorrect: response.isCorrect,
            score: response.score,
            gradingText: response.gradingResult,
            recordId: response.recordId
          }
        }
        
        return null
      } catch (error) {
        console.error('非流式AI判题失败:', error)
        return null
      }
    }
    
    // 获取AI判题历史记录
    const getAiGradingHistory = async () => {
      try {
        const data = await questionAPI.getAiGradingHistory(currentQuestion.value.id)
        hasHistoryRecord.value = data.hasHistory
        historyRecord.value = data.latestRecord
        historyRecords.value = data.historyRecords || []
      } catch (error) {
        console.error('获取AI判题历史记录失败:', error)
      }
    }
     
    // 创建刷题记录
    const createPracticeRecord = async () => {
      if (!currentQuestion.value) return
      
      try {
        const token = localStorage.getItem('token')
        
        let subjectId = currentQuestion.value.subjectId || practiceStore.filters.subjectId || 1
        let subjectName = '随机练习'
        
        if (subjectId && subjects.value.length > 0) {
          const subject = subjects.value.find(s => s.id === subjectId)
          if (subject) {
            subjectName = subject.name
          }
        }
        
        if (currentQuestion.value.subjectName) {
          subjectName = currentQuestion.value.subjectName
        }
        
        const response = await axios.post('/api/practice-record/create', null, {
          params: {
            subjectId: subjectId,
            subjectName: subjectName,
            questionType: currentQuestion.value.questionType,
            difficulty: currentQuestion.value.difficulty || 1
          },
          headers: {
            'Authorization': `Bearer ${token}`
          }
        })
        
        currentPracticeRecordId.value = response.data.data
        practiceStartTime.value = new Date()
      } catch (error) {
        console.error('创建刷题记录失败:', error)
      }
    }
     
    // 添加答题详情到刷题记录
    const addPracticeDetail = async (userAnswer, isCorrect, aiAnalysis = null, aiScore = null) => {
      if (!currentPracticeRecordId.value || !currentQuestion.value) return
      
      try {
        const token = localStorage.getItem('token')
        const timeSpent = practiceStartTime.value ? 
          Math.floor((new Date() - practiceStartTime.value) / 1000) : 0
        
        // 修复：后端接口不需要 practiceRecordId 参数，直接传递答题记录参数
        const params = {
          questionId: currentQuestion.value.id,
          userAnswer: userAnswer,
          isCorrect: isCorrect !== null ? isCorrect : false, // 确保 isCorrect 不为 null
          timeSpent: timeSpent,
          practiceType: 1
        }
        
        console.log('📝 添加答题详情参数:', params)
        
        await axios.post('/api/practice-record/add-answer', null, {
          params: params,
          headers: {
            'Authorization': `Bearer ${token}`
          }
        })
        
        console.log('✅ 答题详情添加成功')
      } catch (error) {
        console.error('添加答题详情失败:', error)
        throw error // 重新抛出错误以便上层处理
      }
    }
     
    // 更新刷题记录
    const updatePracticeRecord = async () => {
      if (!currentPracticeRecordId.value) return
      
      try {
        const token = localStorage.getItem('token')
        const totalTime = practiceStartTime.value ? 
          Math.floor((new Date() - practiceStartTime.value) / 1000) : 0
        
        await axios.put('/api/practice-record/update', null, {
          params: {
            practiceRecordId: currentPracticeRecordId.value,
            totalQuestions: 1,
            correctCount: practiceStore.isCorrect ? 1 : 0,
            wrongCount: practiceStore.isCorrect ? 0 : 1,
            totalTime: totalTime,
            status: 2
          },
          headers: {
            'Authorization': `Bearer ${token}`
          }
        })
      } catch (error) {
        console.error('更新刷题记录失败:', error)
      }
    }

    // 监听题目变化
    watch(
      () => practiceStore.currentQuestion,
      async (newQuestion) => {
        if (newQuestion) {
          currentPracticeRecordId.value = null
          practiceStartTime.value = null
          
          resetAnswerArea()
          
          aiGradingResult.value = null
          aiGradingText.value = ''
          hasHistoryRecord.value = false
          historyRecords.value = []
          
          if (newQuestion.questionType >= 3) {
            await getAiGradingHistory()
            
            if (hasHistoryRecord.value && historyRecords.value.length > 0) {
              const latestRecord = historyRecords.value[0]
              aiGradingResult.value = {
                isCorrect: latestRecord.isCorrect,
                score: latestRecord.score || (latestRecord.isCorrect ? 1 : 0)
              }
              aiGradingText.value = latestRecord.gradingResult || latestRecord.aiAnalysis || ''
            }
          }
        }
      },
      { immediate: true }
    )

    onMounted(async () => {
      console.log('🎯 Practice.vue: 组件已挂载')
      
      if (currentUserId.value) {
        aiChatStore.currentUserId = currentUserId.value
      }
      
      await getSubjects()
      await getQuotaInfo()
      await loadModelOptions()
      await nextTick()

      if (subjects.value.length === 0) {
        await getSubjects()
      }

      if (subjects.value.length === 0) {
        subjects.value = [
          { id: 1, name: '语文', code: 'chinese', enabled: true },
          { id: 2, name: '数学', code: 'math', enabled: true },
          { id: 3, name: '英语', code: 'english', enabled: true },
          { id: 4, name: '物理', code: 'physics', enabled: true },
          { id: 5, name: '化学', code: 'chemistry', enabled: true }
        ]
        await nextTick()
      }
    })

    onUnmounted(() => {
      if (typewriterTimer.value) {
        clearInterval(typewriterTimer.value)
      }
    })
    
    return {
      practiceStore,
      aiChatStore,
      subjects,
      currentQuestion,
      practiceMode,
      showResult,
      isCorrect,
      filters,
      currentUserAnswer,
      isSubmitting,
      isRestarting,
      currentPracticeRecordId,
      practiceStartTime,
      aiChatRef,
      answerAreaRef,
      isAnalyzing,
      isAiGrading,
      aiGradingResult,
      aiGradingText,
      showAiGradingCard,
      displayedText,
      hasHistoryRecord,
      historyRecord,
      historyRecords,
      quotaInfo,
      remainingQuota,
      selectedModelId,
      modelOptions,
      showCompletionCard,
      practiceStats,
      showAiAssistantCard,
      updateFilters,
      updatePracticeMode,
      handleAnswerChange,
      handleSubmitAnswer,
      handleSkipQuestion,
      startPractice,
      getNextQuestion,
      exitPractice,
      resetFilters,
      resetPractice,
      restartPractice,
      goToHome,
      handleAiAction,
      analyzeCurrentQuestion,
      handleSessionCleared,
      aiGrading,
      closeAiGradingCard,
      handleRegrade,
      handleToggleAiGrading,
      toggleAiGradingCard,
      toggleAiAssistantCard,
      getQuotaInfo,
      loadModelOptions,
      handleModelChange
    }
  }
}
</script>

<style scoped>
.practice-page {
  padding: 20px;
  min-height: calc(100vh - 120px);
  background-color: #f5f7fa;
  max-width: 100%;
  overflow-x: auto;
}

.question-section {
  max-width: 1400px;
  margin: 0 auto;
}

.question-layout {
  display: flex;
  gap: 20px;
  align-items: flex-start;
}

.question-main {
  flex: 1;
  min-width: 0;
}

.ai-chat-sidebar {
  width: 480px;
  flex-shrink: 0;
  position: sticky;
  top: 20px;
  max-height: calc(100vh - 100px);
  display: flex;
  flex-direction: column;
}

.ai-chat-card {
  height: 900px; /* 增加到900px，约为原来的1.5倍 */
  width: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden; /* Web端需要hidden */
}

.ai-chat-card :deep(.t-card__body) {
  padding: 0;
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden; /* Web端需要hidden */
}

.ai-card-header {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.ai-header-content {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.ai-header-title {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 12px;
}

/* AI控制区域 - 按钮和模型选择器并排 */
.ai-controls-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.ai-analyze-btn {
  flex-shrink: 0;
  height: 36px;
  padding: 0 16px;
  font-size: 14px;
  font-weight: 600;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
  transition: all 0.3s ease;
  color: white;
  border-radius: 6px;
}

.ai-analyze-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(102, 126, 234, 0.4);
  background: linear-gradient(135deg, #5a6fd8 0%, #6a4190 100%);
}

.ai-analyze-btn:disabled {
  background: #ccc !important;
  box-shadow: none;
  transform: none;
  color: #666 !important;
}

.model-selector-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  min-width: 0;
}

.model-selector-label {
  font-size: 12px;
  color: #606266;
  font-weight: 500;
  white-space: nowrap;
}

.model-selector {
  flex: 1;
  min-width: 150px;
}

/* AI判题展开/收起按钮区域 */
.ai-grading-toggle {
  width: 100%;
}

.toggle-ai-grading-btn {
  width: 100%;
  height: 32px;
  font-size: 12px;
}

/* AI助手展开/收起按钮区域 */
.ai-assistant-toggle {
  position: fixed;
  top: 50%;
  right: 20px;
  transform: translateY(-50%);
  z-index: 1000;
  transition: all 0.3s ease;
}

.toggle-ai-assistant-btn {
  writing-mode: vertical-rl;
  text-orientation: mixed;
  height: 120px;
  width: 32px;
  font-size: 12px;
  font-weight: 500;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  color: white;
  border-radius: 16px 0 0 16px;
  box-shadow: -2px 0 8px rgba(102, 126, 234, 0.3);
  transition: all 0.3s ease;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  cursor: pointer;
}

.toggle-ai-assistant-btn:hover {
  background: linear-gradient(135deg, #5a6fd8 0%, #6a4190 100%);
  box-shadow: -4px 0 12px rgba(102, 126, 234, 0.4);
  transform: translateX(-2px);
}

.toggle-ai-assistant-btn .t-icon {
  font-size: 14px;
  margin: 2px 0;
}

/* 移动端响应式 - AI助手按钮 */
@media (max-width: 1200px) {
  .ai-assistant-toggle {
    position: static;
    right: auto;
    top: auto;
    transform: none;
    margin-bottom: 16px;
    width: 100%;
  }
  
  .toggle-ai-assistant-btn {
    writing-mode: initial;
    text-orientation: initial;
    height: 40px;
    width: 100%;
    border-radius: 6px;
    flex-direction: row;
    gap: 8px;
  }
  
  .toggle-ai-assistant-btn .t-icon {
    font-size: 16px;
    margin: 0;
  }
}

.ai-chat-wrapper {
  flex: 1;
  height: 100%; /* 使用100%高度适应父容器 */
  overflow: hidden; /* Web端需要hidden */
  display: flex;
  flex-direction: column;
}

/* AI Chat Multiselect 自定义样式 */
:deep(.ai-chat-multiselect-container) {
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

:deep(.ai-chat-multiselect-container:hover) {
  border-color: #4dabf7;
}

:deep(.ai-chat-multiselect-container.is-active) {
  border-color: #0052d9;
  box-shadow: 0 0 0 2px rgba(0, 82, 217, 0.1);
}

:deep(.ai-chat-select-dropdown) {
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

:deep(.ai-chat-select-dropdown.is-hidden) {
  display: none !important;
}

:deep(.ai-chat-multiselect-container .multiselect) {
  min-height: 32px;
  height: 32px;
  width: 100%;
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif !important;
  font-size: 14px !important;
  line-height: 1.5 !important;
}

:deep(.ai-chat-multiselect-container .multiselect-single-label) {
  padding-left: 12px;
  padding-right: 40px;
  line-height: 30px;
}

:deep(.ai-chat-multiselect-container .multiselect-placeholder) {
  padding-left: 12px;
  line-height: 30px;
  color: #bbb;
}

:deep(.ai-chat-multiselect-container .multiselect-caret) {
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

@media (max-width: 1200px) {
  .question-layout {
    flex-direction: column;
  }
  
  .ai-chat-sidebar {
    width: 100%;
    position: static;
    max-height: none;
  }
  
  .ai-chat-card {
    height: auto; /* 移动端使用auto高度 */
    max-height: 80vh;
    min-height: 500px;
    overflow: visible; /* 移动端需要visible防止遮挡 */
  }
  
  .ai-chat-card :deep(.t-card__body) {
    overflow: visible; /* 移动端需要visible防止遮挡 */
    padding: 0;
  }
  
  .ai-chat-wrapper {
    height: auto; /* 移动端使用auto高度 */
    min-height: 450px;
    overflow: visible; /* 移动端需要visible防止遮挡 */
  }
}
</style>

<style src="@vueform/multiselect/themes/default.css"></style>

// 在setup函数中添加一个安全的消息提示包装函数
const showMessage = (type, message, options = {}) => {
  try {
    // 确保在DOM更新后显示消息
    nextTick(() => {
      MessagePlugin[type](message, options)
    })
  } catch (error) {
    console.error('MessagePlugin调用失败:', error)
    // 降级方案：使用原生alert
    alert(message)
  }
}

// 替换原有的MessagePlugin调用
// 在submitAnswer函数中替换：
// 原：MessagePlugin.success('🎉 回答正确！太棒了！')
// 替换为：showMessage('success', '🎉 回答正确！太棒了！')

// 原：MessagePlugin.error('❌ 回答错误，继续加油！')
// 替换为：showMessage('error', '❌ 回答错误，继续加油！')

// 原：MessagePlugin.info('✏️ 答案已提交，待交给AI智能判题')
// 替换为：showMessage('info', '✏️ 答案已提交，待交给AI智能判题')
