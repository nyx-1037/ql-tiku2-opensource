import { createRouter, createWebHistory } from 'vue-router'
import Home from '../views/Home.vue'
import Login from '../views/Login.vue'
import Register from '../views/Register.vue'
import Practice from '../views/Practice.vue'
import Exam from '../views/Exam.vue'
import ExamList from '../views/ExamList.vue'
import ExamDetail from '../views/ExamDetail.vue'
import WrongBook from '../views/WrongBook.vue'
import Profile from '../views/Profile.vue'
import Analytics from '../views/Analytics.vue'
import AiChatPage from '../views/AiChatPage.vue'
import PracticeRecord from '../views/PracticeRecord.vue'
import ResourceLibrary from '../views/ResourceLibrary.vue'
import FeedbackCenter from '../views/FeedbackCenter.vue'
import TestTDesign from '../views/TestTDesign.vue'
import QuestionBank from '../views/QuestionBank.vue'
import QuestionBankDetail from '../views/QuestionBankDetail.vue'


const routes = [
  {
    path: '/',
    name: 'Home',
    component: Home,
    meta: { requiresAuth: true}
  },
  {
    path: '/login',
    name: 'Login',
    component: Login
  },
  {
    path: '/register',
    name: 'Register',
    component: Register
  },
  {
    path: '/practice',
    name: 'Practice',
    component: Practice,
    meta: { requiresAuth: true }
  },
  {
    path: '/exam',
    name: 'Exam',
    component: Exam,
    meta: { requiresAuth: true }
  },
  {
    path: '/exam-list',
    name: 'ExamList',
    component: ExamList,
    meta: { requiresAuth: true }
  },
  {
    path: '/exam-detail/:examId',
    name: 'ExamDetail',
    component: ExamDetail,
    meta: { requiresAuth: true }
  },
  {
    path: '/wrong-book',
    name: 'WrongBook',
    component: WrongBook,
    meta: { requiresAuth: true }
  },
  {
    path: '/profile',
    name: 'Profile',
    component: Profile,
    meta: { requiresAuth: true }
  },
  {
    path: '/analytics',
    name: 'Analytics',
    component: Analytics,
    meta: { requiresAuth: true }
  },
  {
    path: '/ai-chat',
    name: 'AiChatPage',
    component: AiChatPage,
    meta: { requiresAuth: true }
  },
  {
    path: '/practice-record',
    name: 'PracticeRecord',
    component: PracticeRecord,
    meta: { requiresAuth: true }
  },
  {
    path: '/resource-library',
    name: 'ResourceLibrary',
    component: ResourceLibrary,
    meta: { requiresAuth: true }
  },
  {
    path: '/feedback',
    name: 'FeedbackCenter',
    component: FeedbackCenter,
    meta: { requiresAuth: true }
  },
  {
    path: '/test-tdesign',
    name: 'TestTDesign',
    component: TestTDesign,
    meta: { requiresAuth: false }
  },
  {
    path: '/question-bank',
    name: 'QuestionBank',
    component: QuestionBank,
    meta: { requiresAuth: true }
  },
  {
    path: '/question-bank/:id',
    name: 'QuestionBankDetail',
    component: QuestionBankDetail,
    meta: { requiresAuth: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫 - 极致简化，避免任何可能的时序问题
router.beforeEach((to, from, next) => {
  console.log('Router: 路由守卫触发', to.name, to.path)
  
  // 同步检查，避免异步问题
  const token = localStorage.getItem('token')
  
  // 需要认证但无token
  if (to.meta.requiresAuth && !token) {
    next('/login')
    return
  }
  
  // 已登录用户访问登录/注册页
  if ((to.name === 'Login' || to.name === 'Register') && token) {
    next('/')
    return
  }
  
  // 正常访问
  next()
})

// 路由错误处理
router.onError((error) => {
  console.error('Router: 路由错误:', error)
  // 可以在这里添加错误上报或用户提示
})

export default router