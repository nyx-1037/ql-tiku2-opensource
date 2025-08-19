import api from './index'

/**
 * 获取所有科目列表
 */
export function getAllSubjects() {
  return api.get('/subjects')
}

/**
 * 根据ID获取科目详情
 * @param {number} id 科目ID
 */
export function getSubjectById(id) {
  return api.get(`/subjects/${id}`)
}

/**
 * 分页查询题目列表
 * @param {Object} data 查询参数
 */
export function getQuestionPage(data) {
  return api.post('/question/page', data)
}

/**
/**
 * 获取科目统计信息
 */
export function getSubjectStatistics() {
  return api.get('/question/subject-stats')
}

/**
/**
 * 搜索题目
 * @param {string} keyword 关键词
 * @param {number} current 当前页
 * @param {number} size 每页大小
 * @param {Object} filters 其他筛选条件
 */
export function searchQuestions(keyword = '', current = 1, size = 10, filters = {}) {
  const params = {
    keyword: keyword || '',
    current,
    size,
    ...filters
  }
  console.log('🔍 [Question API] 搜索题目参数:', params)
  return api.get('/question/search', { params })
}

/**
 * 搜索题目（带完整参数）
 * @param {Object} params 查询参数
 */
export function searchQuestionsWithParams(params) {
  return api.get('/question/search', { params })
}

/**
 * 根据ID获取题目详情
 * @param {number} id 题目ID
 */
export function getQuestion(id) {
  return api.get(`/question/${id}`)
}

/**
 * 获取题目类型列表
 */
export function getQuestionTypes() {
  return api.get('/question/types')
}

/**
 * 获取难度等级列表
 */
export function getDifficulties() {
  return api.get('/question/difficulties')
}

/**
 * 获取指定科目的题目数量
 * @param {number} subjectId 科目ID
 */
export function getQuestionCountBySubject(subjectId) {
  return api.get(`/question/count/${subjectId}`)
}

/**
 * 获取练习题目
 * @param {Object} params 查询参数
 */
export function getPracticeQuestions(params) {
  return api.get('/question/practice', { params })
}

/**
 * 提交答案
 * @param {Object} data 答案数据
 */
export function submitAnswer(data) {
  return api.post('/question/submit', data)
}

/**
 * AI判题（非流式）
 * @param {Object} data 判题数据
 */
export function aiGrading(data) {
  return api.post('/question/ai-grading', data, {
    timeout: 60000 // AI判题需要更长的超时时间（60秒）
  })
}

/**
 * 获取AI判题历史记录
 * @param {number} questionId 题目ID
 */
export function getAiGradingHistory(questionId) {
  return api.get(`/question/ai-grading/history/${questionId}`)
}

/**
 * 获取科目列表（别名方法，兼容store调用）
 */
export function getSubjects() {
  return getAllSubjects()
}

export default {
  getAllSubjects,
  getSubjects, // 添加别名方法
  getSubjectById,
  getQuestion,
  getQuestionPage,
  getSubjectStatistics,
  searchQuestions,
  getQuestionTypes,
  getDifficulties,
  getQuestionCountBySubject,
  getPracticeQuestions,
  submitAnswer,
  aiGrading,
  getAiGradingHistory
}
