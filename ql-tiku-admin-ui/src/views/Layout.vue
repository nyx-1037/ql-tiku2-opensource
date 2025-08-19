<template>
  <div class="layout-container">
    <el-container>
      <!-- 侧边栏 -->
      <el-aside :width="isCollapse ? '64px' : '200px'" 
            class="sidebar"
            :class="{ 'mobile-show': mobileShow, 'mobile-sidebar': isMobile }">
        <div class="logo">
          <template v-if="!isCollapse">
            <div class="logo-content">
              <div class="logo-main">
                <img v-if="siteConfig.siteLogo" :src="siteConfig.siteLogo" class="logo-img" alt="Logo" />
                <el-icon v-else class="logo-icon"><Reading /></el-icon>
                <span class="logo-text">{{ siteConfig.siteName }}</span>
              </div>
              <el-tag type="primary" size="small" class="admin-tag">管理端</el-tag>
            </div>
          </template>
          <template v-else>
            <div class="logo-collapsed">
              <img v-if="siteConfig.siteLogo" :src="siteConfig.siteLogo" class="logo-img-collapsed" alt="Logo" />
              <el-icon v-else class="logo-icon-collapsed"><Reading /></el-icon>
              <el-tag type="primary" size="small" class="admin-tag-collapsed">管</el-tag>
            </div>
          </template>
          <el-button
            type="primary"
            link
            @click="closeSidebar"
            class="mobile-close-btn"
          >
            <el-icon><Close /></el-icon>
          </el-button>
        </div>
        
        <el-menu
          :default-active="$route.path"
          class="sidebar-menu"
          :collapse="isCollapse"
          :unique-opened="true"
          router
        >
          <el-menu-item index="/dashboard">
            <el-icon><Odometer /></el-icon>
            <template #title>仪表盘</template>
          </el-menu-item>
          
          <el-menu-item index="/question-bank">
            <el-icon><Document /></el-icon>
            <template #title>题库管理</template>
          </el-menu-item>
          
          <el-menu-item index="/subject-manage">
            <el-icon><Collection /></el-icon>
            <template #title>科目管理</template>
          </el-menu-item>
          
          <el-sub-menu index="/user-manage">
            <template #title>
              <el-icon><User /></el-icon>
              <span>用户管理</span>
            </template>
            <el-menu-item index="/user-manage/user-list">
              <el-icon><User /></el-icon>
              <template #title>用户列表</template>
            </el-menu-item>
            <el-menu-item index="/user-manage/online-users">
              <el-icon><Connection /></el-icon>
              <template #title>在线用户管理</template>
            </el-menu-item>
            <el-menu-item index="/user-manage/registration-code">
              <el-icon><Key /></el-icon>
              <template #title>注册码管理</template>
            </el-menu-item>
            <el-menu-item index="/user-manage/membership-management">
              <el-icon><Star /></el-icon>
              <template #title>会员管理</template>
            </el-menu-item>
          </el-sub-menu>
          
          <el-menu-item index="/exam-manage">
            <el-icon><Edit /></el-icon>
            <template #title>考试管理</template>
          </el-menu-item>
          
          <el-menu-item index="/resource-manage">
            <el-icon><FolderOpened /></el-icon>
            <template #title>资料库管理</template>
          </el-menu-item>
          
          <el-menu-item index="/data-analysis">
            <el-icon><TrendCharts /></el-icon>
            <template #title>数据分析</template>
          </el-menu-item>
          
          <el-sub-menu index="/ai-data-manage">
            <template #title>
              <el-icon><Cpu /></el-icon>
              <span>AI数据管理</span>
            </template>
            <el-menu-item index="/ai-data-manage/chat-sessions">
              <el-icon><ChatDotRound /></el-icon>
              <template #title>AI聊天会话</template>
            </el-menu-item>
            <el-menu-item index="/ai-data-manage/chat-records">
              <el-icon><ChatLineSquare /></el-icon>
              <template #title>AI聊天记录</template>
            </el-menu-item>
            <el-menu-item index="/ai-data-manage/grading-records">
              <el-icon><Star /></el-icon>
              <template #title>AI评分记录</template>
            </el-menu-item>
            <el-menu-item index="/ai-data-manage/quotas">
              <el-icon><Coin /></el-icon>
              <template #title>AI配额管理</template>
            </el-menu-item>
            <el-menu-item index="/ai-data-manage/model-management">
              <el-icon><Cpu /></el-icon>
              <template #title>模型管理</template>
            </el-menu-item>
          </el-sub-menu>
          
          <el-menu-item index="/announcement-manage">
            <el-icon><Bell /></el-icon>
            <template #title>公告管理</template>
          </el-menu-item>
          
          <el-menu-item index="/feedback-manage">
            <el-icon><ChatLineRound /></el-icon>
            <template #title>反馈管理</template>
          </el-menu-item>
          
          <el-menu-item index="/log-manage">
            <el-icon><Document /></el-icon>
            <template #title>日志管理</template>
          </el-menu-item>
          
          <el-sub-menu index="/system-settings">
            <template #title>
              <el-icon><Setting /></el-icon>
              <span>系统设置</span>
            </template>
            <el-menu-item index="/system-settings/basic">
              <el-icon><Setting /></el-icon>
              <template #title>基本设置</template>
            </el-menu-item>
            <el-menu-item index="/system-settings/email">
              <el-icon><Message /></el-icon>
              <template #title>邮件设置</template>
            </el-menu-item>
            <el-menu-item index="/system-settings/storage">
              <el-icon><FolderOpened /></el-icon>
              <template #title>存储设置</template>
            </el-menu-item>
            <el-menu-item index="/system-settings/security">
              <el-icon><Lock /></el-icon>
              <template #title>安全设置</template>
            </el-menu-item>
            <el-menu-item index="/system-settings/backup">
              <el-icon><Download /></el-icon>
              <template #title>备份管理</template>
            </el-menu-item>
            <el-menu-item index="/system-settings/ai">
              <el-icon><Connection /></el-icon>
              <template #title>AI设置</template>
            </el-menu-item>
          </el-sub-menu>
        </el-menu>
      </el-aside>
      
      <!-- 主要内容区域 -->
        <el-container :class="['main-container', { collapsed: isCollapse }]">
        <!-- 顶部导航栏 -->
        <el-header class="header" :class="{ 'mobile-header': isMobile }">
          <div class="header-left" :class="{ 'mobile-header-left': isMobile }">
            <el-button
              type="text"
              class="collapse-btn"
              @click="toggleCollapse"
            >
              <el-icon><Expand v-if="isCollapse" /><Fold v-else /></el-icon>
            </el-button>
            
            <el-breadcrumb separator="/" class="breadcrumb">
              <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
              <el-breadcrumb-item>{{ currentPageTitle }}</el-breadcrumb-item>
            </el-breadcrumb>
          </div>
          
          <div class="header-right" :class="{ 'mobile-header-right': isMobile }">
            <!-- 通知 -->
            <el-badge :value="12" class="notification">
              <el-button type="primary" text class="header-btn">
                <el-icon><Bell /></el-icon>
              </el-button>
            </el-badge>
            
            <!-- 用户信息 -->
            <el-dropdown @command="handleCommand" class="user-dropdown">
              <div class="user-info">
                <el-avatar :size="32" :src="adminInfo.avatar">
                  <el-icon><User /></el-icon>
                </el-avatar>
                <span class="username">{{ adminInfo.username }}</span>
                <el-icon class="dropdown-icon"><ArrowDown /></el-icon>
              </div>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="profile">
                    <el-icon><User /></el-icon>
                    个人信息
                  </el-dropdown-item>
                  <el-dropdown-item command="password">
                    <el-icon><Lock /></el-icon>
                    修改密码
                  </el-dropdown-item>
                  <el-dropdown-item command="logout" divided>
                    <el-icon><SwitchButton /></el-icon>
                    退出登录
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </el-header>
        
        <!-- 主要内容 -->
        <el-main class="main-content">
          <router-view />
        </el-main>
      </el-container>
    </el-container>
    
    <!-- 修改密码对话框 -->
    <el-dialog
      v-model="passwordDialogVisible"
      title="修改密码"
      width="400px"
      :before-close="handlePasswordDialogClose"
    >
      <el-form
        ref="passwordFormRef"
        :model="passwordForm"
        :rules="passwordRules"
        label-width="80px"
      >
        <el-form-item label="原密码" prop="oldPassword">
          <el-input
            v-model="passwordForm.oldPassword"
            type="password"
            placeholder="请输入原密码"
            show-password
          />
        </el-form-item>
        
        <el-form-item label="新密码" prop="newPassword">
          <el-input
            v-model="passwordForm.newPassword"
            type="password"
            placeholder="请输入新密码"
            show-password
          />
        </el-form-item>
        
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="passwordForm.confirmPassword"
            type="password"
            placeholder="请确认新密码"
            show-password
          />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="handlePasswordDialogClose">取消</el-button>
          <el-button type="primary" :loading="passwordLoading" @click="handlePasswordSubmit">
            确定
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  Reading,
  Odometer,
  Document,
  Collection,
  User,
  Edit,
  FolderOpened,
  TrendCharts,
  Bell,
  Setting,
  Expand,
  Fold,
  ArrowDown,
  Lock,
  Close,
  ChatLineRound,
  Message,
  Download,
  Key,
  Cpu,
  ChatDotRound,
  ChatLineSquare,
  Star,
  Coin,
  Connection,
  SwitchButton
} from '@element-plus/icons-vue'
import { useSystemStore } from '../store/system'
import { useAuthStore } from '../store/auth'

export default {
  name: 'Layout',
  components: {
    Reading,
    Odometer,
    Document,
    Collection,
    User,
    Edit,
    FolderOpened,
    TrendCharts,
    Bell,
    Setting,
    Expand,
    Fold,
    ArrowDown,
    Lock,
    Close,
    ChatLineRound,
    Message,
    Download,
    Key,
    Cpu,
    ChatDotRound,
    ChatLineSquare,
    Star,
    Coin,
    Connection,
    SwitchButton
  },
  setup() {
    const router = useRouter()
    const route = useRoute()
    const systemStore = useSystemStore()
    const authStore = useAuthStore()
    
    const isCollapse = ref(false)
    const mobileShow = ref(false)
    const isMobile = ref(false)
    const passwordDialogVisible = ref(false)
    const passwordFormRef = ref()
    const passwordLoading = ref(false)
    
    const passwordForm = reactive({
      oldPassword: '',
      newPassword: '',
      confirmPassword: ''
    })
    
    const passwordRules = {
      oldPassword: [
        { required: true, message: '请输入原密码', trigger: 'blur' }
      ],
      newPassword: [
        { required: true, message: '请输入新密码', trigger: 'blur' },
        { min: 6, max: 20, message: '密码长度在 6 到 20 个字符', trigger: 'blur' }
      ],
      confirmPassword: [
        { required: true, message: '请确认新密码', trigger: 'blur' },
        {
          validator: (rule, value, callback) => {
            if (value !== passwordForm.newPassword) {
              callback(new Error('两次输入密码不一致'))
            } else {
              callback()
            }
          },
          trigger: 'blur'
        }
      ]
    }
    
    // 当前页面标题
    const currentPageTitle = computed(() => {
      const routeMap = {
        '/dashboard': '仪表盘',
        '/question-bank': '题库管理',
        '/subject-manage': '科目管理',
        '/user-manage': '用户管理',
        '/user-manage/user-list': '用户列表',
        '/user-manage/online-users': '在线用户管理',
        '/user-manage/registration-code': '注册码管理',
        '/user-manage/membership-management': '会员管理',
        '/exam-manage': '考试管理',
        '/resource-manage': '资料库管理',
        '/data-analysis': '数据分析',
        '/announcement-manage': '公告管理',
        '/feedback-manage': '反馈管理',
        '/log-manage': '日志管理',
        '/system-settings': '系统设置',
        '/system-settings/basic': '基本设置',
        '/system-settings/email': '邮件设置',
        '/system-settings/storage': '存储设置',
        '/system-settings/security': '安全设置',
        '/system-settings/backup': '备份管理',
        '/system-settings/ai': 'AI设置',
        '/system-settings/profile': '个人信息',
        '/ai-data-manage': 'AI数据管理',
        '/ai-data-manage/chat-sessions': 'AI聊天会话',
        '/ai-data-manage/chat-records': 'AI聊天记录',
        '/ai-data-manage/grading-records': 'AI评分记录',
        '/ai-data-manage/quotas': 'AI配额管理',
        '/ai-data-manage/model-management': '模型管理'
      }
      return routeMap[route.path] || '未知页面'
    })
    

    
    // 切换侧边栏折叠状态
    const toggleCollapse = () => {
      if (window.innerWidth <= 768) {
        mobileShow.value = !mobileShow.value
      } else {
        isCollapse.value = !isCollapse.value
      }
    }
    
    // 处理窗口大小变化
    const handleResize = () => {
      const mobile = window.innerWidth <= 768
      isMobile.value = mobile
      
      if (mobile) {
        isCollapse.value = false // 移动端时不折叠
        mobileShow.value = false // 默认隐藏
      } else {
        mobileShow.value = false // 非移动端隐藏移动菜单
      }
    }
    
    // 用户操作处理
    const handleCommand = (command) => {
      if (command === 'profile') {
        router.push('/system-settings/profile')
      } else if (command === 'password') {
        passwordDialogVisible.value = true
      } else if (command === 'logout') {
        handleLogout()
      }
    }
    
    // 退出登录
    const handleLogout = async () => {
      try {
        await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        
        authStore.logout()
        ElMessage.success('退出登录成功')
        router.push('/login')
      } catch (error) {
        if (error !== 'cancel') {
          console.error('退出登录失败:', error)
        }
      }
    }
    
    // 关闭修改密码对话框
    const handlePasswordDialogClose = () => {
      passwordDialogVisible.value = false
      passwordFormRef.value?.resetFields()
      Object.assign(passwordForm, {
        oldPassword: '',
        newPassword: '',
        confirmPassword: ''
      })
    }
    
    // 提交修改密码
    const handlePasswordSubmit = async () => {
      if (!passwordFormRef.value) return
      
      try {
        await passwordFormRef.value.validate()
        passwordLoading.value = true
        
        // await authAPI.changePassword({
        //   oldPassword: passwordForm.oldPassword,
        //   newPassword: passwordForm.newPassword
        // })
        
        ElMessage.success('密码修改成功')
        handlePasswordDialogClose()
      } catch (error) {
        if (error.message) {
          ElMessage.error(error.message)
        }
      } finally {
        passwordLoading.value = false
      }
    }
    
    // 关闭侧边栏
    const closeSidebar = () => {
      mobileShow.value = false
    }
    
    onMounted(async () => {
      authStore.initAuth()
      await systemStore.loadSystemConfig()
      handleResize() // 初始化时检查窗口大小
      window.addEventListener('resize', handleResize)
    })
    
    onUnmounted(() => {
      window.removeEventListener('resize', handleResize)
    })
    
    return {
      isCollapse,
      mobileShow,
      isMobile,
      adminInfo: authStore.userInfo,
      currentPageTitle,
      passwordDialogVisible,
      passwordFormRef,
      passwordForm,
      passwordRules,
      passwordLoading,
      siteConfig: systemStore.siteConfig,
      toggleCollapse,
      closeSidebar,
      handleCommand,  // 使用handleCommand而不是handleUserCommand
      handleLogout,
      handlePasswordDialogClose,
      handlePasswordSubmit
    }
  }
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
  display: flex;
}

.sidebar {
  background-color: #304156;
  transition: width 0.3s;
  position: fixed;
  left: 0;
  top: 0;
  height: 100vh;
  z-index: 1000;
  overflow-y: auto;
}

.logo {
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #2b3a4b;
  color: white;
  font-size: 18px;
  font-weight: 600;
  position: relative;
}

.logo-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.logo-main {
  display: flex;
  align-items: center;
}

.logo-collapsed {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.logo-icon {
  font-size: 24px;
  margin-right: 8px;
}

.logo-icon-collapsed {
  font-size: 20px;
}

.logo-img {
  height: 28px;
  width: auto;
  margin-right: 8px;
  object-fit: contain;
}

.logo-img-collapsed {
  height: 24px;
  width: auto;
  object-fit: contain;
}

.admin-tag {
  font-size: 10px;
  height: 16px;
  line-height: 14px;
  padding: 0 4px;
  border-radius: 2px;
}

.admin-tag-collapsed {
  font-size: 8px;
  height: 12px;
  line-height: 10px;
  padding: 0 2px;
  border-radius: 2px;
  margin-top: 2px;
}

.sidebar-menu {
  border-right: none;
  background-color: #304156;
}

.sidebar-menu .el-menu-item {
  color: #bfcbd9;
  border-bottom: 1px solid #263445;
}

.sidebar-menu .el-menu-item:hover {
  background-color: #263445;
  color: #409EFF;
}

.sidebar-menu .el-menu-item.is-active {
  background-color: #409EFF;
  color: white;
}

/* 子菜单样式 */
.sidebar-menu .el-sub-menu {
  background-color: #304156;
}

.sidebar-menu .el-sub-menu .el-sub-menu__title {
  color: #bfcbd9 !important;
  border-bottom: 1px solid #263445;
}

/* 确保子菜单标题文本颜色 */
.sidebar-menu .el-sub-menu .el-sub-menu__title,
.sidebar-menu .el-sub-menu .el-sub-menu__title span,
.sidebar-menu .el-sub-menu .el-sub-menu__title .el-icon {
  color: #bfcbd9 !important;
}

/* 强制设置系统设置文本颜色 */
.sidebar-menu .el-sub-menu[index="/system-settings"] .el-sub-menu__title,
.sidebar-menu .el-sub-menu[index="/system-settings"] .el-sub-menu__title span,
.sidebar-menu .el-sub-menu[index="/system-settings"] .el-sub-menu__title .el-icon {
  color: #bfcbd9 !important;
}

/* 设置下拉箭头颜色 */
.sidebar-menu .el-sub-menu .el-sub-menu__title .el-sub-menu__icon-arrow,
.sidebar-menu .el-sub-menu .el-sub-menu__title .el-icon {
  color: #ffffff !important;
}

.sidebar-menu .el-sub-menu.is-active .el-sub-menu__title .el-sub-menu__icon-arrow,
.sidebar-menu .el-sub-menu.is-opened .el-sub-menu__title .el-sub-menu__icon-arrow {
  color: #409EFF !important;
}

/* 悬停和激活状态 */
.sidebar-menu .el-sub-menu .el-sub-menu__title:hover,
.sidebar-menu .el-sub-menu .el-sub-menu__title:hover span,
.sidebar-menu .el-sub-menu .el-sub-menu__title:hover .el-icon,
.sidebar-menu .el-sub-menu.is-active .el-sub-menu__title,
.sidebar-menu .el-sub-menu.is-active .el-sub-menu__title span,
.sidebar-menu .el-sub-menu.is-active .el-sub-menu__title .el-icon,
.sidebar-menu .el-sub-menu.is-opened .el-sub-menu__title,
.sidebar-menu .el-sub-menu.is-opened .el-sub-menu__title span,
.sidebar-menu .el-sub-menu.is-opened .el-sub-menu__title .el-icon {
  color: #409EFF !important;
}

.sidebar-menu .el-sub-menu .el-sub-menu__title:hover {
  background-color: #263445;
  color: #409EFF !important;
}

.sidebar-menu .el-sub-menu.is-active .el-sub-menu__title,
.sidebar-menu .el-sub-menu.is-opened .el-sub-menu__title {
  color: #409EFF !important;
}

.sidebar-menu .el-sub-menu .el-menu {
  background-color: #263445;
}

.sidebar-menu .el-sub-menu .el-menu-item {
  background-color: #263445;
  color: #bfcbd9;
  border-bottom: 1px solid #1f2d3d;
  padding-left: 50px !important;
}

.sidebar-menu .el-sub-menu .el-menu-item .el-icon {
  color: inherit;
}

.sidebar-menu .el-sub-menu .el-menu-item:hover {
  background-color: #1f2d3d;
  color: #409EFF;
}

.sidebar-menu .el-sub-menu .el-menu-item.is-active {
  background-color: #409EFF;
  color: white;
}

/* Ensure text color in submenu items */
.sidebar-menu .el-sub-menu [class^="el-icon-"],
.sidebar-menu .el-sub-menu [class*=" el-icon-"] {
  color: inherit;
}

.header {
  background-color: white;
  border-bottom: 1px solid #e6e6e6;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
}

.header-left {
  display: flex;
  align-items: center;
}

.collapse-btn {
  font-size: 18px;
  margin-right: 20px;
}

.breadcrumb {
  font-size: 14px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.notification {
  cursor: pointer;
}

.header-btn {
  font-size: 18px;
  color: #666;
}

.user-dropdown {
  cursor: pointer;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 5px 10px;
  border-radius: 4px;
  transition: background-color 0.3s;
}

.user-info:hover {
  background-color: #f5f5f5;
}

.username {
  font-size: 14px;
  color: #333;
}

.dropdown-icon {
  font-size: 12px;
  color: #999;
}

.main-container {
  margin-left: 200px;
  transition: margin-left 0.3s;
  flex: 1;
  display: flex;
  flex-direction: column;
  height: 100vh;
}

.main-container.collapsed {
  margin-left: 64px;
}

.main-content {
  background-color: #f0f2f5;
  padding: 20px;
  padding-bottom: 80px; /* 为浮动底部留出空间 */
  overflow-y: auto;
  flex: 1;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

/* 移动端关闭按钮 */
.mobile-close-btn {
  display: none;
  position: absolute;
  right: 10px;
  top: 50%;
  transform: translateY(-50%);
  color: white;
  font-size: 18px;
}

.mobile-close-btn:hover {
  color: #409EFF;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .sidebar {
    transform: translateX(-100%);
    transition: transform 0.3s;
    width: 280px !important; /* 移动端侧边栏宽度 */
  }
  
  .sidebar.mobile-show {
    transform: translateX(0);
    box-shadow: 2px 0 8px rgba(0, 0, 0, 0.15);
  }
  
  .main-container {
    margin-left: 0;
  }
  
  .main-container.collapsed {
    margin-left: 0;
  }
  
  .header {
    padding: 0 15px;
  }
  
  .header-left {
    flex: 1;
  }
  
  .header-left .breadcrumb {
    display: none;
  }
  
  .header-right {
    gap: 15px;
  }
  
  .user-info .username {
    display: none;
  }
  
  .user-info {
    padding: 5px;
  }
  
  .logo {
    position: relative;
    padding: 0 15px;
  }
  
  .logo-text {
    font-size: 16px;
  }
  
  .mobile-close-btn {
    display: block;
  }
  
  .main-content {
    padding: 15px;
    padding-bottom: 100px;
  }
  
  .collapse-btn {
    margin-right: 15px;
  }
}

/* 超小屏幕优化 */
@media (max-width: 480px) {
  .sidebar {
    width: 260px !important;
  }
  
  .header {
    padding: 0 10px;
  }
  
  .header-right {
    gap: 10px;
  }
  
  .main-content {
    padding: 10px;
    padding-bottom: 110px;
  }
  
  .collapse-btn {
    margin-right: 10px;
    font-size: 16px;
  }
  
  .header-btn {
    font-size: 16px;
  }
}
</style>