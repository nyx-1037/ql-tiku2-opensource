import { defineStore } from 'pinia'

export const usePracticeStore = defineStore('practice', {
  state: () => ({
    // 练习基本信息
    practiceInfo: {
      id: null,
      title: '',
      subject: '',
      difficulty: '',
      questionType: '', // single, multiple, judge, fill
      totalQuestions: 0,
      startTime: null
    },
    
    // 题目列表
    questions: [],
    
    // 当前题目索引
    currentQuestionIndex: 0,
    
    // 用户答案 { questionId: { answer, isCorrect, explanation } }
    userAnswers: {},
    
    // 练习状态
    practiceStatus: 'not_started', // not_started, in_progress, paused, completed
    
    // 练习模式
    practiceMode: 'normal', // normal, review, random
    
    // 是否显示答案解析
    showExplanation: false,
    
    // 练习统计
    practiceStats: {
      totalAnswered: 0,
      correctCount: 0,
      wrongCount: 0,
      accuracy: 0,
      timeSpent: 0 // 秒
    },
    
    // 错题记录
    wrongQuestions: [],
    
    // 收藏题目
    favoriteQuestions: [],
    
    // 练习设置
    practiceSettings: {
      autoNext: false, // 答题后自动跳转下一题
      showResult: true, // 答题后立即显示结果
      randomOrder: false, // 随机题目顺序
      timeLimit: 0 // 单题时间限制（秒，0表示无限制）
    },
    
    // 筛选条件
    filters: {
      subjectId: '',
      type: '',
      difficulty: ''
    },
    
    // 当前题目
    currentQuestion: null,
    
    // 显示结果
    showResult: false,
    
    // 是否正确
    isCorrect: false,
    
    // 答题状态缓存 { questionId: answerState }
    answerStates: {}
  }),
  
  getters: {
    
    // 获取已答题数量
    answeredCount: (state) => {
      return Object.keys(state.userAnswers).length
    },
    
    // 获取未答题数量
    unansweredCount: (state) => {
      return state.questions.length - Object.keys(state.userAnswers).length
    },
    
    // 检查当前题目是否已作答
    isCurrentQuestionAnswered: (state) => {
      const currentQuestion = state.questions[state.currentQuestionIndex]
      return currentQuestion ? !!state.userAnswers[currentQuestion.id] : false
    },
    
    // 获取当前题目的答案
    currentQuestionAnswer: (state) => {
      const currentQuestion = state.questions[state.currentQuestionIndex]
      return currentQuestion ? state.userAnswers[currentQuestion.id] : null
    },
    
    // 计算正确率
    accuracy: (state) => {
      const total = Object.keys(state.userAnswers).length
      if (total === 0) return 0
      const correct = Object.values(state.userAnswers).filter(answer => answer.isCorrect).length
      return Math.round((correct / total) * 100)
    },
    
    // 检查是否所有题目都已作答
    isAllAnswered: (state) => {
      return state.questions.length > 0 && Object.keys(state.userAnswers).length === state.questions.length
    },
    
    // 获取进度百分比
    progressPercentage: (state) => {
      if (state.questions.length === 0) return 0
      return Math.round((Object.keys(state.userAnswers).length / state.questions.length) * 100)
    }
  },
  
  actions: {
    // 初始化练习
    initPractice(practiceData) {
      this.practiceInfo = {
        id: practiceData.id,
        title: practiceData.title || '刷题练习',
        subject: practiceData.subject || '',
        difficulty: practiceData.difficulty || '',
        questionType: practiceData.questionType || '',
        totalQuestions: practiceData.questions?.length || 0,
        startTime: new Date().toISOString()
      }
      
      // 根据设置决定是否随机排序
      this.questions = this.practiceSettings.randomOrder 
        ? this.shuffleArray([...practiceData.questions]) 
        : practiceData.questions || []
      
      this.currentQuestionIndex = 0
      this.userAnswers = {}
      this.practiceStatus = 'in_progress'
      this.practiceStats = {
        totalAnswered: 0,
        correctCount: 0,
        wrongCount: 0,
        accuracy: 0,
        timeSpent: 0
      }
    },
    
    // 设置当前题目索引
    setCurrentQuestionIndex(index) {
      if (index >= 0 && index < this.questions.length) {
        this.currentQuestionIndex = index
      }
    },
    
    // 下一题
    nextQuestion() {
      if (this.currentQuestionIndex < this.questions.length - 1) {
        this.currentQuestionIndex++
      }
    },
    
    // 上一题
    prevQuestion() {
      if (this.currentQuestionIndex > 0) {
        this.currentQuestionIndex--
      }
    },
    
    // 保存答案
    saveAnswer(questionId, answer, isCorrect, explanation = '') {
      this.userAnswers[questionId] = {
        answer,
        isCorrect,
        explanation,
        answeredAt: new Date().toISOString()
      }
      
      // 更新统计
      this.updateStats()
      
      // 如果答错了，添加到错题记录
      if (!isCorrect) {
        const question = this.questions.find(q => q.id === questionId)
        if (question && !this.wrongQuestions.find(wq => wq.id === questionId)) {
          this.wrongQuestions.push({
            ...question,
            userAnswer: answer,
            wrongAt: new Date().toISOString()
          })
        }
      }
      
      // 如果设置了自动跳转下一题
      if (this.practiceSettings.autoNext && this.currentQuestionIndex < this.questions.length - 1) {
        setTimeout(() => {
          this.nextQuestion()
        }, 1000) // 延迟1秒跳转
      }
    },
    
    // 更新统计信息
    updateStats() {
      const answers = Object.values(this.userAnswers)
      this.practiceStats.totalAnswered = answers.length
      this.practiceStats.correctCount = answers.filter(a => a.isCorrect).length
      this.practiceStats.wrongCount = answers.filter(a => !a.isCorrect).length
      this.practiceStats.accuracy = this.accuracy
    },
    
    // 切换答案解析显示
    toggleExplanation() {
      this.showExplanation = !this.showExplanation
    },
    
    // 添加/移除收藏题目
    toggleFavorite(questionId) {
      const question = this.questions.find(q => q.id === questionId)
      if (!question) return
      
      const index = this.favoriteQuestions.findIndex(fq => fq.id === questionId)
      if (index > -1) {
        this.favoriteQuestions.splice(index, 1)
      } else {
        this.favoriteQuestions.push({
          ...question,
          favoriteAt: new Date().toISOString()
        })
      }
    },
    
    // 检查题目是否已收藏
    isFavorite(questionId) {
      return this.favoriteQuestions.some(fq => fq.id === questionId)
    },
    
    // 暂停练习
    pausePractice() {
      if (this.practiceStatus === 'in_progress') {
        this.practiceStatus = 'paused'
      }
    },
    
    // 恢复练习
    resumePractice() {
      if (this.practiceStatus === 'paused') {
        this.practiceStatus = 'in_progress'
      }
    },
    
    // 完成练习
    completePractice() {
      this.practiceStatus = 'completed'
      this.updateStats()
    },
    
    // 重置练习状态
    resetPractice() {
      this.practiceInfo = {
        id: null,
        title: '',
        subject: '',
        difficulty: '',
        questionType: '',
        totalQuestions: 0,
        startTime: null
      }
      this.questions = []
      this.currentQuestionIndex = 0
      this.userAnswers = {}
      this.practiceStatus = 'not_started'
      this.practiceStats = {
        totalAnswered: 0,
        correctCount: 0,
        wrongCount: 0,
        accuracy: 0,
        timeSpent: 0
      }
    },
    
    // 更新练习设置
    updateSettings(settings) {
      this.practiceSettings = { ...this.practiceSettings, ...settings }
    },
    
    // 数组随机排序工具函数
    shuffleArray(array) {
      const shuffled = [...array]
      for (let i = shuffled.length - 1; i > 0; i--) {
        const j = Math.floor(Math.random() * (i + 1))
        ;[shuffled[i], shuffled[j]] = [shuffled[j], shuffled[i]]
      }
      return shuffled
    },
    
    // 跳转到指定题目
    goToQuestion(index) {
      this.setCurrentQuestionIndex(index)
    },
    
    // 清除错题记录
    clearWrongQuestions() {
      this.wrongQuestions = []
    },
    
    // 清除收藏题目
    clearFavoriteQuestions() {
      this.favoriteQuestions = []
    },
    
    // 设置筛选条件
    setFilters(filters) {
      this.filters = { ...this.filters, ...filters }
    },
    
    // 重置筛选条件
    resetFilters() {
      this.filters = {
        subjectId: '',
        type: '',
        difficulty: ''
      }
    },
    
    // 设置当前题目
    setCurrentQuestion(question) {
      this.currentQuestion = question
    },
    
    // 设置显示结果
    setShowResult(show) {
      this.showResult = show
    },
    
    // 设置是否正确
    setIsCorrect(correct) {
      this.isCorrect = correct
    },
    
    // 保存当前答题状态
    saveCurrentAnswerState(answerState) {
      if (answerState.questionId) {
        this.answerStates[answerState.questionId] = answerState
      }
    },
    
    // 获取当前答题状态
    getCurrentAnswerState(questionId) {
      return this.answerStates[questionId] || null
    },
    
    // 清除答题状态缓存
    clearAnswerStates() {
      this.answerStates = {}
    }
  },

  // 添加持久化配置 - 持久化关键状态
  persist: {
    key: 'practice-store',
    storage: localStorage,
    paths: [
      'practiceInfo',
      'questions',
      'currentQuestionIndex',
      'userAnswers',
      'practiceStatus',
      'practiceMode',
      'practiceStats',
      'practiceSettings',
      'answerStates',
      'filters',
      'currentQuestion',
      'showResult',
      'isCorrect',
      'wrongQuestions',
      'favoriteQuestions'
    ],
    // 自定义序列化，避免循环引用
    serializer: {
      serialize: (state) => {
        try {
          // 创建一个安全的状态副本，避免循环引用
          const safeState = {
            ...state,
            // 确保时间字段正确序列化
            practiceInfo: {
              ...state.practiceInfo,
              startTime: state.practiceInfo.startTime
            }
          }
          return JSON.stringify(safeState)
        } catch (error) {
          console.warn('Practice store序列化失败:', error)
          return '{}'
        }
      },
      deserialize: (value) => {
        try {
          const parsed = JSON.parse(value)
          // 验证数据完整性
          if (parsed && typeof parsed === 'object') {
            return parsed
          }
          return {}
        } catch (error) {
          console.warn('Practice store反序列化失败:', error)
          return {}
        }
      }
    },
    // 添加恢复前的验证
    beforeRestore: (context) => {
      try {
        // 检查数据是否有效
        const state = context.store
        if (state && state.practiceStatus === 'in_progress' && state.currentQuestion) {
          console.log('🔄 Practice Store: 恢复练习状态')
          return true
        }
        console.log('ℹ️ Practice Store: 无有效练习状态需要恢复')
        return true
      } catch (error) {
        console.error('Practice Store恢复验证失败:', error)
        return false
      }
    }
  }
})