import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../store/auth'

// 导入页面组件
import Login from '../views/Login.vue'
import Layout from '../views/Layout.vue'
import Dashboard from '../views/Dashboard.vue'
import QuestionBank from '../views/QuestionBank.vue'
import SubjectManage from '../views/SubjectManage.vue'
import UserList from '../views/user/UserList.vue'
import OnlineUsers from '../views/user/OnlineUsers.vue'
import RegistrationCode from '../views/user/RegistrationCode.vue'
import ExamManage from '../views/ExamManage.vue'
import ResourceManage from '../views/ResourceManage.vue'
import MembershipManagement from '../views/user/MembershipManagement.vue'
import AiChatSessions from '../views/ai/AiChatSessions.vue'
import AiChatRecords from '../views/ai/AiChatRecords.vue'
import AiGradingRecords from '../views/ai/AiGradingRecords.vue'
import AiQuotas from '../views/ai/AiQuotas.vue'
import ModelManagement from '../views/ai/ModelManagement.vue'
import DataAnalytics from '../views/DataAnalytics.vue'
import AnnouncementManage from '../views/AnnouncementManage.vue'
import FeedbackManage from '../views/FeedbackManage.vue'
import BasicSettings from '../views/settings/BasicSettings.vue'
import EmailSettings from '../views/settings/EmailSettings.vue'
import StorageSettings from '../views/settings/StorageSettings.vue'
import SecuritySettings from '../views/settings/SecuritySettings.vue'
import BackupSettings from '../views/settings/BackupSettings.vue'
import AiSettings from '../views/settings/AiSettings.vue'
import LogManage from '../views/LogManage.vue'
import ProfileSettings from '../views/settings/ProfileSettings.vue'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: Login,
    meta: { requiresAuth: false }
  },
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    meta: { requiresAuth: true },
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: Dashboard,
        meta: { title: '仪表盘', icon: 'Odometer' }
      },
      {
        path: 'question-bank',
        name: 'QuestionBank',
        component: QuestionBank,
        meta: { title: '题库管理', icon: 'Document' }
      },
      {
        path: 'subject-manage',
        name: 'SubjectManage',
        component: SubjectManage,
        meta: { title: '科目管理', icon: 'Collection' }
      },
      {
        path: 'user-manage',
        name: 'UserManage',
        redirect: '/user-manage/user-list',
        meta: { title: '用户管理', icon: 'User' },
        children: [
          {
            path: 'user-list',
            name: 'UserList',
            component: UserList,
            meta: { title: '用户列表', icon: 'User' }
          },
          {
            path: 'online-users',
            name: 'OnlineUsers',
            component: OnlineUsers,
            meta: { title: '在线用户管理', icon: 'Connection' }
          },
          {
            path: 'registration-code',
            name: 'RegistrationCode',
            component: RegistrationCode,
            meta: { title: '注册码管理', icon: 'Key' }
          },
          {
            path: 'membership-management',
            name: 'MembershipManagement',
            component: MembershipManagement,
            meta: { title: '会员管理', icon: 'Star' }
          }
        ]
      },
      {
        path: 'exam-manage',
        name: 'ExamManage',
        component: ExamManage,
        meta: { title: '考试管理', icon: 'Edit' }
      },
      {
        path: 'resource-manage',
        name: 'ResourceManage',
        component: ResourceManage,
        meta: { title: '资料库管理', icon: 'FolderOpened' }
      },
      {
        path: 'ai-data-manage',
        name: 'AiDataManage',
        redirect: '/ai-data-manage/chat-sessions',
        meta: { title: 'AI数据管理', icon: 'Cpu' },
        children: [
          {
            path: 'chat-sessions',
            name: 'AiChatSessions',
            component: AiChatSessions,
            meta: { title: 'AI聊天会话', icon: 'ChatDotRound' }
          },
          {
            path: 'chat-records',
            name: 'AiChatRecords',
            component: AiChatRecords,
            meta: { title: 'AI聊天记录', icon: 'ChatLineSquare' }
          },
          {
            path: 'grading-records',
            name: 'AiGradingRecords',
            component: AiGradingRecords,
            meta: { title: 'AI评分记录', icon: 'Star' }
          },
          {
            path: 'quotas',
            name: 'AiQuotas',
            component: AiQuotas,
            meta: { title: 'AI配额管理', icon: 'Coin' }
          },
          {
            path: 'model-management',
            name: 'ModelManagement',
            component: ModelManagement,
            meta: { title: '模型管理', icon: 'Cpu' }
          }
        ]
      },
      {
        path: 'data-analysis',
        name: 'DataAnalysis',
        component: DataAnalytics,
        meta: { title: '数据分析', icon: 'TrendCharts' }
      },
      {
        path: 'announcement-manage',
        name: 'AnnouncementManage',
        component: AnnouncementManage,
        meta: { title: '公告管理', icon: 'Bell' }
      },
      {
        path: 'feedback-manage',
        name: 'FeedbackManage',
        component: FeedbackManage,
        meta: { title: '反馈管理', icon: 'ChatLineRound' }
      },
      {
        path: 'log-manage',
        name: 'LogManage',
        component: LogManage,
        meta: { title: '日志管理', icon: 'Document' }
      },
      {
        path: 'system-settings/basic',
        name: 'BasicSettings',
        component: BasicSettings,
        meta: { title: '基本设置', icon: 'Setting' }
      },
      {
        path: 'system-settings/email',
        name: 'EmailSettings',
        component: EmailSettings,
        meta: { title: '邮件设置', icon: 'Message' }
      },
      {
        path: 'system-settings/storage',
        name: 'StorageSettings',
        component: StorageSettings,
        meta: { title: '存储设置', icon: 'FolderOpened' }
      },
      {
        path: 'system-settings/security',
        name: 'SecuritySettings',
        component: SecuritySettings,
        meta: { title: '安全设置', icon: 'Lock' }
      },
      {
        path: 'system-settings/backup',
        name: 'BackupSettings',
        component: BackupSettings,
        meta: { title: '备份管理', icon: 'Download' }
      },
      {
        path: 'system-settings/ai',
        name: 'AiSettings',
        component: AiSettings,
        meta: { title: 'AI设置', icon: 'ChatDotRound' }
      },
      {
        path: 'system-settings/profile',
        name: 'ProfileSettings',
        component: ProfileSettings,
        meta: { title: '个人信息', icon: 'User' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const authStore = useAuthStore()
  
  // 每次导航都重新初始化认证状态，确保与localStorage同步
  authStore.initAuth()
  
  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    ElMessage.warning('请先登录')
    next('/login')
  } else if (to.path === '/login' && authStore.isAuthenticated) {
    next('/')
  } else {
    next()
  }
})

export default router