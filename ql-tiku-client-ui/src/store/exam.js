import { defineStore } from 'pinia'

export const useExamStore = defineStore('exam', {
  state: () => ({
    // 考试基本信息
    examInfo: {
      id: null,
      title: '',
      duration: 0, // 考试时长（分钟）
      totalQuestions: 0,
      startTime: null,
      endTime: null
    },
    
    // 题目列表
    questions: [],
    
    // 当前题目索引
    currentQuestionIndex: 0,
    
    // 用户答案 { questionId: answer }
    userAnswers: {},
    
    // 考试状态
    examStatus: 'not_started', // not_started, in_progress, paused, completed, submitted
    
    // 剩余时间（秒）
    remainingTime: 0,
    
    // 是否已提交
    isSubmitted: false,
    
    // 考试结果
    examResult: {
      score: 0,
      correctCount: 0,
      wrongCount: 0,
      totalTime: 0
    }
  }),
  
  getters: {
    // 获取当前题目
    currentQuestion: (state) => {
      return state.questions[state.currentQuestionIndex] || null
    },
    
    // 获取已答题数量
    answeredCount: (state) => {
      return Object.keys(state.userAnswers).length
    },
    
    // 获取未答题数量
    unansweredCount: (state) => {
      return state.questions.length - Object.keys(state.userAnswers).length
    },
    
    // 检查是否所有题目都已作答
    isAllAnswered: (state) => {
      return state.questions.length > 0 && Object.keys(state.userAnswers).length === state.questions.length
    },
    
    // 格式化剩余时间
    formattedRemainingTime: (state) => {
      const hours = Math.floor(state.remainingTime / 3600)
      const minutes = Math.floor((state.remainingTime % 3600) / 60)
      const seconds = state.remainingTime % 60
      return `${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`
    }
  },
  
  actions: {
    // 初始化考试
    initExam(examData) {
      this.examInfo = {
        id: examData.id,
        title: examData.title,
        duration: examData.duration,
        totalQuestions: examData.questions?.length || 0,
        startTime: new Date().toISOString(),
        endTime: null
      }
      this.questions = examData.questions || []
      this.currentQuestionIndex = 0
      this.userAnswers = {}
      this.examStatus = 'in_progress'
      this.remainingTime = examData.duration * 60 // 转换为秒
      this.isSubmitted = false
      this.examResult = {
        score: 0,
        correctCount: 0,
        wrongCount: 0,
        totalTime: 0
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
    saveAnswer(questionId, answer) {
      this.userAnswers[questionId] = answer
    },
    
    // 更新剩余时间
    updateRemainingTime(seconds) {
      this.remainingTime = Math.max(0, seconds)
      if (this.remainingTime === 0 && this.examStatus === 'in_progress') {
        this.examStatus = 'completed'
      }
    },
    
    // 暂停考试
    pauseExam() {
      if (this.examStatus === 'in_progress') {
        this.examStatus = 'paused'
      }
    },
    
    // 恢复考试
    resumeExam() {
      if (this.examStatus === 'paused') {
        this.examStatus = 'in_progress'
      }
    },
    
    // 完成考试
    completeExam() {
      this.examStatus = 'completed'
      this.examInfo.endTime = new Date().toISOString()
    },
    
    // 提交考试
    submitExam(result) {
      this.isSubmitted = true
      this.examStatus = 'submitted'
      this.examResult = result
      this.examInfo.endTime = new Date().toISOString()
    },
    
    // 重置考试状态
    resetExam() {
      this.examInfo = {
        id: null,
        title: '',
        duration: 0,
        totalQuestions: 0,
        startTime: null,
        endTime: null
      }
      this.questions = []
      this.currentQuestionIndex = 0
      this.userAnswers = {}
      this.examStatus = 'not_started'
      this.remainingTime = 0
      this.isSubmitted = false
      this.examResult = {
        score: 0,
        correctCount: 0,
        wrongCount: 0,
        totalTime: 0
      }
    }
  }

  // 移除持久化配置 - 考试状态不应该持久化，避免状态混乱
})