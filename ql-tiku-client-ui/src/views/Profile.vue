<template>
  <div class="profile-page">
    <div class="profile-header">
        <h2>个人中心</h2>
        <p>查看和管理您的个人信息和学习数据</p>
      </div>
      
      <!-- 瀑布流卡片布局 -->
      <div class="cards-container">
        <!-- 个人信息卡片 -->
        <div class="card-item">
          <t-card class="profile-card">
            <template #header>
              <div class="card-header">
                <span>个人信息</span>
              </div>
            </template>

            <div class="profile-info">
              <div class="avatar-section">
                <div class="avatar-container" @click="showAvatarPreview = true">
                  <t-avatar 
                    :image="userInfo.avatar || 'https://tdesign.gtimg.com/starter/default-user.png'" 
                    size="large"
                    class="clickable-avatar"
                  >
                    <span class="icon">👤</span>
                  </t-avatar>
                  <div class="avatar-overlay">
                    <span class="preview-text">点击预览</span>
                  </div>
                </div>
                <t-upload
                  class="avatar-uploader"
                  :action="uploadUrl"
                  :headers="uploadHeaders"
                  :show-file-list="false"
                  :on-success="handleAvatarSuccess"
                  :before-upload="beforeAvatarUpload"
                  accept="image/jpeg,image/png"
                  :multiple="false"
                >
                  <t-button theme="primary" variant="text" size="small">更换头像</t-button>
                </t-upload>
              </div>

              <div class="info-section">
                <div class="info-item">
                  <label>用户名</label>
                  <span>{{ userInfo.username }}</span>
                </div>
                <div class="info-item">
                  <label>邮箱</label>
                  <span>{{ userInfo.email || '未设置' }}</span>
                </div>
                <div class="info-item">
                  <label>用户类型</label>
                  <span>{{ userInfo.userType === 'ADMIN' ? '管理员' : '普通用户' }}</span>
                </div>
                <div class="info-item">
                  <label>注册时间</label>
                  <span>{{ formatDate(userInfo.createTime) }}</span>
                </div>
                <div class="info-item">
                  <label>最后登录</label>
                  <span>{{ formatDate(userInfo.lastLoginTime) }}</span>
                </div>
              </div>

              <div class="action-section">
                <t-button theme="primary" @click="showEditDialog = true">编辑信息</t-button>
                <t-button theme="default" @click="showPasswordDialog = true">修改密码</t-button>
              </div>
            </div>
          </t-card>
        </div>

        <!-- 学习统计卡片 -->
        <div class="card-item">
          <t-card class="stats-card">
            <template #header>
              <div class="card-header">
                <span>学习统计</span>
                <t-button
                  theme="primary"
                  variant="text"
                  size="small"
                  @click="getLearningStats"
                  :loading="loadingStates.learningStats"
                  :disabled="loadingStates.learningStats"
                >
                  <template #icon>
                    <t-icon name="refresh" />
                  </template>
                  刷新
                </t-button>
              </div>
            </template>

            <div v-if="loadingStates.learningStats" class="loading-content">
              <t-loading size="medium" text="正在获取学习统计..." />
            </div>

            <div v-else class="stats-grid">
              <div class="stat-item">
                <div class="stat-value">{{ learningStats.totalQuestions }}</div>
                <div class="stat-label">累计练习</div>
              </div>
              <div class="stat-item">
                <div class="stat-value">{{ learningStats.correctRate }}%</div>
                <div class="stat-label">正确率</div>
              </div>
              <div class="stat-item">
                <div class="stat-value">{{ learningStats.studyDays }}</div>
                <div class="stat-label">学习天数</div>
              </div>
              <div class="stat-item">
                <div class="stat-value">{{ learningStats.wrongQuestions }}</div>
                <div class="stat-label">错题数量</div>
              </div>
            </div>
          </t-card>
        </div>

        <!-- AI配额统计卡片 -->
        <div class="card-item">
          <t-card class="ai-quota-card">
            <template #header>
              <div class="card-header">
                <span>AI配额统计</span>
                <t-button
                  theme="primary"
                  variant="text"
                  size="small"
                  @click="loadAiQuotaInfo"
                  :loading="loadingStates.aiQuota"
                  :disabled="loadingStates.aiQuota"
                >
                  <template #icon>
                    <t-icon name="refresh" />
                  </template>
                  刷新
                </t-button>
              </div>
            </template>

            <div v-if="loadingStates.aiQuota" class="loading-content">
              <t-loading size="medium" text="正在获取AI配额信息..." />
            </div>

            <div v-else-if="aiQuotaInfo" class="quota-stats">
              <div class="quota-section">
                <h4>日配额</h4>
                <div class="quota-progress">
                  <t-progress
                    :percentage="dailyUsagePercentage"
                    :color="getDailyProgressColor()"
                    :track-color="'#e9ecef'"
                    :stroke-width="8"
                  />
                  <div class="quota-text">
                    <span class="used">已用: {{ aiQuotaInfo.usedDaily }}</span>
                    <span class="remaining">剩余: {{ dailyRemaining }}</span>
                    <span class="total">总量: {{ aiQuotaInfo.dailyQuota }}</span>
                  </div>
                </div>
              </div>

              <div class="quota-section">
                <h4>月配额</h4>
                <div class="quota-progress">
                  <t-progress
                    :percentage="monthlyUsagePercentage"
                    :color="getMonthlyProgressColor()"
                    :track-color="'#e9ecef'"
                    :stroke-width="8"
                  />
                  <div class="quota-text">
                    <span class="used">已用: {{ aiQuotaInfo.usedMonthly }}</span>
                    <span class="remaining">剩余: {{ monthlyRemaining }}</span>
                    <span class="total">总量: {{ aiQuotaInfo.monthlyQuota }}</span>
                  </div>
                </div>
              </div>

              <div class="quota-info-section">
                <div class="info-item">
                  <label>VIP等级</label>
                  <span>
                    <span :class="getVipTagClass(aiQuotaInfo.vipLevel)" class="vip-tag-profile">
                      {{ getVipLevelText(aiQuotaInfo.vipLevel) }}
                    </span>
                    <span v-if="aiQuotaInfo.vipLevel > 0" class="vip-benefits">享有更多AI配额</span>
                  </span>
                </div>
                <div class="info-item">
                  <label>最后重置日期</label>
                  <span>{{ formatDate(aiQuotaInfo.lastResetDate) }}</span>
                </div>
              </div>
            </div>

            <div v-else class="loading-content">
              <t-loading size="medium" text="正在获取AI配额信息..." />
            </div>
          </t-card>
        </div>

        <!-- 最近活动卡片 -->
        <div class="card-item">
          <t-card class="activity-card">
            <template #header>
              <div class="card-header">
                <span>最近活动</span>
                <t-button
                  theme="primary"
                  variant="text"
                  size="small"
                  @click="getRecentActivities"
                  :loading="loadingStates.recentActivities"
                  :disabled="loadingStates.recentActivities"
                >
                  <template #icon>
                    <t-icon name="refresh" />
                  </template>
                  刷新
                </t-button>
              </div>
            </template>

            <div v-if="loadingStates.recentActivities" class="loading-content">
              <t-loading size="medium" text="正在获取最近活动..." />
            </div>

            <div v-else-if="recentActivities.length === 0" class="empty-activity">
              <t-empty description="暂无活动记录" />
            </div>

            <t-list v-else>
              <t-list-item v-for="activity in recentActivities" :key="activity.id">
                <div class="activity-item">
                  <div class="activity-content">
                    <span class="activity-type">{{ getActivityTypeText(activity.type) }}</span>
                    <span class="activity-desc">{{ activity.description }}</span>
                  </div>
                  <div>
                    <t-tag :theme="activity.result && activity.result.success ? 'success' : 'danger'" size="small">
                      {{ activity.result && activity.result.success ? '通过' : '未通过' }}
                    </t-tag>
                    <span style="margin-left: 10px; color: #999; font-size: 12px;">{{ formatDate(activity.createTime) }}</span>
                  </div>
                </div>
              </t-list-item>
            </t-list>
          </t-card>
        </div>
      </div>
      
      <!-- 编辑信息对话框 -->
      <t-dialog v-model:visible="showEditDialog" header="编辑个人信息" width="500px">
        <t-form :data="editForm" :rules="editRules" ref="editFormRef" label-width="80px">
          <t-form-item label="用户名" name="username">
            <t-input v-model="editForm.username" placeholder="请输入用户名" />
          </t-form-item>
          <t-form-item label="邮箱" name="email">
            <t-input v-model="editForm.email" placeholder="请输入邮箱" />
          </t-form-item>
        </t-form>
        
        <template #footer>
          <t-button @click="showEditDialog = false">取消</t-button>
          <t-button theme="primary" @click="saveUserInfo" :loading="saving">保存</t-button>
        </template>
      </t-dialog>
      
      <!-- 修改密码对话框 -->
      <t-dialog v-model:visible="showPasswordDialog" header="修改密码" width="500px">
        <t-form :data="passwordForm" :rules="passwordRules" ref="passwordFormRef" label-width="100px">
          <t-form-item label="当前密码" name="oldPassword">
            <t-input v-model="passwordForm.oldPassword" type="password" placeholder="请输入当前密码" show-password />
          </t-form-item>
          <t-form-item label="新密码" name="newPassword">
            <t-input v-model="passwordForm.newPassword" type="password" placeholder="请输入新密码" show-password />
          </t-form-item>
          <t-form-item label="确认密码" name="confirmPassword">
            <t-input v-model="passwordForm.confirmPassword" type="password" placeholder="请确认新密码" show-password />
          </t-form-item>
        </t-form>
        
        <template #footer>
          <t-button @click="showPasswordDialog = false">取消</t-button>
          <t-button theme="primary" @click="changePassword" :loading="saving">修改</t-button>
        </template>
      </t-dialog>

      <!-- 头像预览对话框 -->
      <t-dialog 
        v-model:visible="showAvatarPreview" 
        header="头像预览" 
        width="400px"
        :show-overlay="true"
        class="avatar-preview-dialog"
      >
        <div class="avatar-preview-content">
          <img 
            :src="userInfo.avatar || 'https://tdesign.gtimg.com/starter/default-user.png'" 
            alt="头像预览"
            class="preview-avatar-image"
            @error="handleImageError"
          />
        </div>
        
        <template #footer>
          <t-button @click="showAvatarPreview = false">关闭</t-button>
          <t-button theme="primary" @click="triggerAvatarUpload">更换头像</t-button>
        </template>
      </t-dialog>
  </div>
</template>

<script>
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { MessagePlugin } from 'tdesign-vue-next'
import { authAPI, subjectAPI, statisticsAPI, examAPI, wrongBookAPI, aiQuotaAPI } from '@/api'

export default {
  name: 'Profile',
  setup() {
    
    const subjects = ref([])
    const showEditDialog = ref(false)
    const showPasswordDialog = ref(false)
    const showAvatarPreview = ref(false) // 头像预览对话框状态
    const saving = ref(false)
    const editFormRef = ref()
    const passwordFormRef = ref()

    // 加载状态管理
    const loadingStates = reactive({
      learningStats: false,
      aiQuota: false,
      recentActivities: false
    })
    
    // 头像上传相关
    const uploadUrl = ref(process.env.VUE_APP_BASE_API + '/upload/avatar')
    const uploadHeaders = computed(() => {
      const token = localStorage.getItem('token')
      return {
        'Authorization': 'Bearer ' + (token || '')
      }
    })
    
    // 用户信息
    const userInfo = reactive({
      id: null,
      username: '',
      nickname: '',
      email: '',
      phone: '',
      avatar: '',
      userType: null,
      status: null,
      createTime: null,
      lastLoginTime: null
    })
    
    const learningStats = reactive({
      totalQuestions: 0,
      correctRate: 0,
      studyDays: 0,
      wrongQuestions: 0
    })
    
    const settings = reactive({
      defaultSubject: '',
      dailyTarget: 20,
      preferredDifficulties: ['EASY', 'MEDIUM'],
      dailyReminder: true,
      wrongQuestionReview: true
    })
    
    const editForm = reactive({
      username: '',
      email: ''
    })
    
    const passwordForm = reactive({
      oldPassword: '',
      newPassword: '',
      confirmPassword: ''
    })
    
    const recentActivities = ref([])
    
    const editRules = {
      username: [
        { required: true, message: '请输入用户名', trigger: 'blur' },
        { min: 3, max: 20, message: '用户名长度在 3 到 20 个字符', trigger: 'blur' }
      ],
      email: [
        { required: true, message: '请输入邮箱', trigger: 'blur' },
        { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
      ]
    }
    
    // 新密码验证函数
    const validateNewPassword = (value) => {
      if (!value) {
        return { result: false, message: '请输入新密码', type: 'error' }
      }
      
      // 长度验证：6-18位
      if (value.length < 6 || value.length > 18) {
        return { result: false, message: '密码长度必须在6-18个字符之间', type: 'error' }
      }
      
      // 正则验证：必须包含数字和英文字母
      const passwordRegex = /^(?=.*[0-9])(?=.*[a-zA-Z])[0-9a-zA-Z]{6,18}$/
      if (!passwordRegex.test(value)) {
        return { result: false, message: '密码必须包含数字和英文字母，长度6-18位', type: 'error' }
      }
      
      return { result: true }
    }

    const validateConfirmPassword = (value) => {
      if (value !== passwordForm.newPassword) {
        return { result: false, message: '两次输入密码不一致', type: 'error' }
      }
      return { result: true }
    }
    
    const passwordRules = {
      oldPassword: [
        { required: true, message: '请输入当前密码', trigger: 'blur' }
      ],
      newPassword: [
        { validator: validateNewPassword, trigger: 'blur' }
      ],
      confirmPassword: [
        { required: true, message: '请确认新密码', trigger: 'blur' },
        { validator: validateConfirmPassword, trigger: 'blur' }
      ]
    }
    
    // 获取用户信息
    const getUserInfo = async () => {
      try {
        const response = await authAPI.getCurrentUser()
        Object.assign(userInfo, response)
        // 同步到编辑表单
        Object.assign(editForm, {
          username: response.username,
          email: response.email
        })
      } catch (error) {
        console.error('获取用户信息失败:', error)
        MessagePlugin.error('获取用户信息失败')
      }
    }
    
    // 获取科目列表
    const getSubjects = async () => {
      try {
        const response = await subjectAPI.getEnabledSubjects()
        if (Array.isArray(response)) {
          // API直接返回数组格式
          subjects.value = response
        } else if (response.code === 200 && Array.isArray(response.data)) {
          // 标准格式：包含code和data
          subjects.value = response.data
        } else {
          console.error('获取科目数据格式错误:', response)
          subjects.value = []
        }
      } catch (error) {
        console.error('获取科目列表失败:', error)
        subjects.value = []
      }
    }
    
    // 获取学习统计
    const getLearningStats = async () => {
      loadingStates.learningStats = true

      try {
        // 使用与Analytics页面相同的API接口获取完整统计数据
        const stats = await statisticsAPI.getOverviewStats({
          dateRange: '', // 空字符串表示获取全部数据
          subjectId: null
        })
        
        console.log('🔍 Profile.vue: 获取到的统计数据', stats)
        
        // 使用与Analytics页面相同的字段映射
        Object.assign(learningStats, {
          totalQuestions: stats.totalQuestions || 0,
          correctRate: stats.correctRate || 0, // 使用与Analytics页面相同的正确率字段
          studyDays: stats.studyDays || 0,
          studyTime: stats.studyTime || 0,
          wrongQuestions: 0 // 需要从错题本API获取
        })

        // 获取错题数量
        try {
          const wrongData = await wrongBookAPI.getWrongQuestions({ current: 1, size: 1 })
          learningStats.wrongQuestions = wrongData.total || 0
        } catch (wrongError) {
          console.warn('获取错题数量失败:', wrongError)
        }

        // 延迟0.5秒以提供良好的交互效果
        await new Promise(resolve => setTimeout(resolve, 500))
      } catch (error) {
        console.error('获取学习统计失败:', error)
        // 保留默认值
        Object.assign(learningStats, {
          totalQuestions: 0,
          correctRate: 0,
          studyDays: 0,
          studyTime: 0,
          wrongQuestions: 0
        })

        // 即使出错也要延迟0.5秒
        await new Promise(resolve => setTimeout(resolve, 500))
      } finally {
        loadingStates.learningStats = false
      }
    }
    
    // 获取最近活动
    const getRecentActivities = async () => {
      loadingStates.recentActivities = true

      try {
        const response = await examAPI.getUserExamRecords({ current: 1, size: 10 })
        // 后端返回分页数据，需要取records字段
        const activities = response.records || []

        // 转换数据格式以匹配前端显示
        recentActivities.value = activities.map(activity => ({
          id: activity.examId || activity.id,
          type: activity.examType === 'simulation' ? 'PRACTICE' : 'EXAM',
          description: activity.examTitle || activity.description || '考试记录',
          createTime: activity.submitTime || activity.startTime || activity.createTime,
          result: {
            success: activity.passed || activity.status === 1 // 使用后端返回的passed字段或status字段
          }
        }))

        // 延迟1秒以提供良好的交互效果
        await new Promise(resolve => setTimeout(resolve, 1000))
      } catch (error) {
        console.error('获取最近活动失败:', error)
        recentActivities.value = []

        // 即使出错也要延迟1秒
        await new Promise(resolve => setTimeout(resolve, 1000))
      } finally {
        loadingStates.recentActivities = false
      }
    }
    
    // 保存用户信息
    const saveUserInfo = async () => {
      try {
        const valid = await editFormRef.value.validate()
        if (!valid) return
        
        saving.value = true
        // 这里应该调用更新用户信息API
        // await authAPI.updateUserInfo(editForm)
        
        // Object.assign(userInfo, editForm)
        localStorage.setItem('userInfo', JSON.stringify(editForm))
        
        MessagePlugin.success('保存成功')
        showEditDialog.value = false
      } catch (error) {
        console.error('保存用户信息失败:', error)
      } finally {
        saving.value = false
      }
    }
    
    // 修改密码
    const changePassword = async () => {
      try {
        const valid = await passwordFormRef.value.validate()
        if (!valid) {
          console.log('表单验证失败')
          return
        }
        
        saving.value = true
        await authAPI.changePassword({
          oldPassword: passwordForm.oldPassword,
          newPassword: passwordForm.newPassword
        })
        
        MessagePlugin.success('密码修改成功')
        showPasswordDialog.value = false
        
        // 重置表单
        Object.assign(passwordForm, {
          oldPassword: '',
          newPassword: '',
          confirmPassword: ''
        })
      } catch (error) {
        console.error('修改密码失败:', error)
        MessagePlugin.error(error.message || '修改密码失败，请稍后重试')
      } finally {
        saving.value = false
      }
    }
    
    // 保存设置
    const saveSettings = async () => {
      try {
        // 这里应该调用保存设置API
        // await settingsAPI.saveSettings(settings)
        
        MessagePlugin.success('设置保存成功')
      } catch (error) {
        console.error('保存设置失败:', error)
      }
    }
    
    // 头像上传前验证
    const beforeAvatarUpload = (file) => {
      const isJPG = file.type === 'image/jpeg' || file.type === 'image/png'
      const isLt2M = file.size / 1024 / 1024 < 2
      
      if (!isJPG) {
        MessagePlugin.error('上传头像图片只能是 JPG/PNG 格式!')
        return false
      }
      if (!isLt2M) {
        MessagePlugin.error('上传头像图片大小不能超过 2MB!')
        return false
      }
      return true
    }
    
    // 头像上传成功回调
    const handleAvatarSuccess = async (response) => {
      try {
        if (response.code === 200) {
          userInfo.avatar = response.data.url
          // 更新用户头像到后端
          await authAPI.updateAvatar({ avatarUrl: response.data.url })
          MessagePlugin.success('头像更新成功')
        } else {
          MessagePlugin.error(response.message || '头像上传失败')
        }
      } catch (error) {
        console.error('更新头像失败:', error)
        MessagePlugin.error('头像更新失败')
      }
    }

    // 触发头像上传（从预览对话框）
    const triggerAvatarUpload = () => {
      showAvatarPreview.value = false
      // 触发文件选择
      const fileInput = document.createElement('input')
      fileInput.type = 'file'
      fileInput.accept = 'image/jpeg,image/png'
      fileInput.onchange = async (e) => {
        const file = e.target.files[0]
        if (file && beforeAvatarUpload(file)) {
          // 手动上传文件
          const formData = new FormData()
          formData.append('file', file)
          
          try {
            const response = await fetch(uploadUrl.value, {
              method: 'POST',
              headers: uploadHeaders.value,
              body: formData
            })
            const result = await response.json()
            await handleAvatarSuccess(result)
          } catch (error) {
            console.error('上传失败:', error)
            MessagePlugin.error('头像上传失败')
          }
        }
      }
      fileInput.click()
    }

    // 处理图片加载错误
    const handleImageError = (e) => {
      e.target.src = 'https://tdesign.gtimg.com/starter/default-user.png'
    }
    
    // 格式化日期
    const formatDate = (dateString) => {
      if (!dateString) return '-'
      const date = new Date(dateString)
      return date.toLocaleString('zh-CN')
    }
    
    // 获取活动类型文本
    const getActivityTypeText = (type) => {
      const typeMap = {
        'PRACTICE': '练习',
        'EXAM': '考试',
        'REVIEW': '复习'
      }
      return typeMap[type] || type
    }
    
    // AI配额相关功能简化实现
    const dailyRemaining = computed(() => {
      if (!aiQuotaInfo.value) return 0
      return Math.max(0, aiQuotaInfo.value.dailyQuota - aiQuotaInfo.value.usedDaily)
    })
    
    const monthlyRemaining = computed(() => {
      if (!aiQuotaInfo.value) return 0
      return Math.max(0, aiQuotaInfo.value.monthlyQuota - aiQuotaInfo.value.usedMonthly)
    })
    
    // 加载AI配额信息
    const loadAiQuotaInfo = async () => {
      loadingStates.aiQuota = true

      try {
        const response = await aiQuotaAPI.getQuotaInfo()
        // API 响应拦截器已经处理了 code 和 data，直接使用返回的数据
        aiQuotaInfo.value = response

        // 延迟0.5秒以提供良好的交互效果
        await new Promise(resolve => setTimeout(resolve, 500))
      } catch (error) {
        console.error('加载AI配额信息失败:', error)
        MessagePlugin.error('获取AI配额信息失败，请稍后重试')
        // 不设置默认值，保持为null以显示加载状态

        // 即使出错也要延迟0.5秒
        await new Promise(resolve => setTimeout(resolve, 500))
      } finally {
        loadingStates.aiQuota = false
      }
    }
    
    // 获取日配额进度条颜色
    const getDailyProgressColor = () => {
      if (!aiQuotaInfo.value) return '#67C23A'
      const percentage = (aiQuotaInfo.value.usedDaily / aiQuotaInfo.value.dailyQuota) * 100
      if (percentage >= 90) return '#F56C6C' // 红色，危险
      if (percentage >= 70) return '#E6A23C' // 橙色，警告
      return '#67C23A' // 绿色，正常
    }
    
    // 获取月配额进度条颜色
    const getMonthlyProgressColor = () => {
      if (!aiQuotaInfo.value) return '#67C23A'
      const percentage = (aiQuotaInfo.value.usedMonthly / aiQuotaInfo.value.monthlyQuota) * 100
      if (percentage >= 90) return '#F56C6C' // 红色，危险
      if (percentage >= 70) return '#E6A23C' // 橙色，警告
      return '#67C23A' // 绿色，正常
    }
    
    // AI配额相关状态
    const aiQuotaInfo = ref(null)
    
    // 计算属性
    const dailyUsagePercentage = computed(() => {
      if (!aiQuotaInfo.value) return 0
      return Math.round((aiQuotaInfo.value.usedDaily / aiQuotaInfo.value.dailyQuota) * 100)
    })
    
    const monthlyUsagePercentage = computed(() => {
      if (!aiQuotaInfo.value) return 0
      return Math.round((aiQuotaInfo.value.usedMonthly / aiQuotaInfo.value.monthlyQuota) * 100)
    })
    
    // 获取VIP标签类名
    const getVipTagClass = (vipLevel) => {
      if (vipLevel >= 4) return 'vip-tag-svip' // 超级VIP使用渐变色
      if (vipLevel >= 1) return 'vip-tag-normal' // VIP1-3使用红色
      return 'vip-tag-normal'
    }
    
    // 获取VIP等级文本
    const getVipLevelText = (vipLevel) => {
      const levelMap = {
        0: '普通用户',
        1: 'VIP1',
        2: 'VIP2', 
        3: 'VIP3',
        4: '超级VIP'
      }
      return levelMap[vipLevel] || '未知'
    }
    
    onMounted(async () => {
      console.log('🎯 Profile.vue: 组件已挂载')

      // 先加载基础信息
      await getUserInfo()
      await getSubjects()
      await nextTick() // 确保DOM更新

      // 强制触发下拉框重新渲染
      console.log('🔄 Profile.vue: 强制触发下拉框重新渲染')

      // 并行加载三个卡片的数据，每个都有加载效果
      Promise.all([
        getLearningStats(),
        loadAiQuotaInfo(),
        getRecentActivities()
      ]).catch(error => {
        console.error('加载数据时出现错误:', error)
      })
    })
    
    return {
      userInfo,
      subjects,
      showEditDialog,
      showPasswordDialog,
      showAvatarPreview, // 头像预览状态
      saving,
      editFormRef,
      passwordFormRef,
      learningStats,
      settings,
      editForm,
      passwordForm,
      recentActivities,
      editRules,
      passwordRules,
      uploadUrl,
      uploadHeaders,
      // 加载状态
      loadingStates,
      getUserInfo,
      getSubjects,
      getLearningStats,
      saveUserInfo,
      changePassword,
      saveSettings,
      beforeAvatarUpload,
      handleAvatarSuccess,
      triggerAvatarUpload, // 头像上传触发
      handleImageError, // 图片错误处理
      getRecentActivities,
      formatDate,
      getActivityTypeText,
      // AI配额相关
      aiQuotaInfo,
      dailyUsagePercentage,
      monthlyUsagePercentage,
      dailyRemaining,
      monthlyRemaining,
      loadAiQuotaInfo,
      getDailyProgressColor,
      getMonthlyProgressColor,
      getVipTagClass,
      getVipLevelText
    }
  }
}
</script>

<style scoped>
.profile-page {
  padding: 20px;
  min-height: 100vh;
  background-color: #f5f7fa;
  max-width: 1600px;
  margin: 0 auto;
  width: 100%;
}

/* 头像容器样式 */
.avatar-container {
  position: relative;
  display: inline-block;
  cursor: pointer;
  transition: all 0.3s ease;
}

.avatar-container:hover {
  transform: scale(1.05);
}

.clickable-avatar {
  transition: all 0.3s ease;
}

.avatar-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.avatar-container:hover .avatar-overlay {
  opacity: 1;
}

.preview-text {
  color: white;
  font-size: 12px;
  font-weight: 500;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.5);
}

/* 头像预览对话框样式 */
.avatar-preview-dialog .avatar-preview-content {
  text-align: center;
  padding: 20px;
}

.preview-avatar-image {
  max-width: 100%;
  max-height: 300px;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  transition: transform 0.3s ease;
}

.preview-avatar-image:hover {
  transform: scale(1.02);
}

/* 瀑布流卡片容器 */
.cards-container {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(380px, 1fr));
  gap: 20px;
  align-items: start;
}

/* 卡片项目 */
.card-item {
  width: 100%;
  break-inside: avoid;
}

/* 响应式设计 */
@media (max-width: 1400px) {
  .profile-page {
    max-width: 95%;
  }

  .cards-container {
    grid-template-columns: repeat(auto-fit, minmax(350px, 1fr));
  }
}

@media (max-width: 1200px) {
  .profile-page {
    padding: 15px;
    max-width: 95%;
  }

  .cards-container {
    grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
    gap: 15px;
  }
}

@media (max-width: 992px) {
  .profile-page {
    padding: 15px;
    max-width: 95%;
  }

  .cards-container {
    grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
    gap: 15px;
  }

  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .quota-section {
    margin-bottom: 16px;
  }
}

@media (max-width: 768px) {
  .profile-page {
    padding: 10px;
    max-width: 100%;
  }

  .cards-container {
    grid-template-columns: 1fr !important;
    gap: 15px;
  }

  .action-section {
    flex-direction: column;
    align-items: center;
    gap: 8px;
  }

  .stats-grid {
    grid-template-columns: 1fr;
    gap: 15px;
  }

  .stat-item {
    padding: 12px;
  }

  .stat-value {
    font-size: 22px;
  }

  .stat-label {
    font-size: 13px;
  }
}

@media (max-width: 480px) {
  .profile-page {
    padding: 8px;
  }

  .cards-container {
    gap: 12px;
  }

  .card-item {
    margin-bottom: 0;
  }

  .profile-info {
    padding: 0 10px;
  }

  .avatar-section .t-avatar {
    width: 70px !important;
    height: 70px !important;
  }

  .stat-value {
    font-size: 20px;
  }

  .info-item {
    flex-direction: column;
    align-items: flex-start;
    padding: 10px 0;
  }

  .info-item label {
    margin-bottom: 4px;
  }

  .action-section .t-button {
    width: 100%;
    margin-bottom: 8px;
  }

  .action-section .t-button:last-child {
    margin-bottom: 0;
  }
}

.profile-header {
  text-align: center;
  margin-bottom: 30px;
}

.profile-header h2 {
  color: #333;
  margin-bottom: 10px;
}

.profile-header p {
  color: #666;
}

/* 卡片样式优化 */
.profile-card,
.stats-card,
.ai-quota-card,
.activity-card {
  transition: all 0.3s ease;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.profile-card:hover,
.stats-card:hover,
.ai-quota-card:hover,
.activity-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.profile-info {
  text-align: center;
}

.avatar-section {
  margin-bottom: 20px;
}

.avatar-section .t-button {
  margin-top: 10px;
}

.info-section {
  text-align: left;
  margin-bottom: 20px;
}

.info-item {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
}

.info-item:last-child {
  border-bottom: none;
}

.info-item label {
  color: #666;
  font-weight: 500;
}

.action-section {
  display: flex;
  gap: 10px;
  justify-content: center;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
}

.stat-item {
  text-align: center;
  padding: 15px;
  background-color: #f8f9fa;
  border-radius: 6px;
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
  color: #409EFF;
  margin-bottom: 5px;
}

.stat-label {
  color: #666;
  font-size: 14px;
}

.form-tip {
  margin-left: 10px;
  color: #666;
}

.empty-activity {
  padding: 40px 0;
}

.activity-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.activity-content {
  flex: 1;
}

.activity-type {
  font-weight: 600;
  color: #409EFF;
  margin-right: 10px;
}

.activity-desc {
  color: #666;
}

.activity-result {
  margin-left: 10px;
}

.ai-quota-card {
  margin-bottom: 20px;
}

.quota-stats {
  padding: 16px 0;
}

.quota-section {
  margin-bottom: 24px;
}

.quota-section h4 {
  margin: 0 0 12px 0;
  color: #303133;
  font-size: 16px;
}

.quota-progress {
  margin-bottom: 8px;
}

.quota-text {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
  flex-wrap: wrap;
}

.quota-text .used {
  color: #E6A23C;
}

.quota-text .total {
  color: #606266;
}

.quota-text .remaining {
  color: #67C23A;
}

.quota-info-section {
  border-top: 1px solid #f0f0f0;
  padding-top: 16px;
}

.quota-info-section .info-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 0;
}

.vip-tag-profile {
  font-size: 12px;
  padding: 4px 8px;
  border-radius: 12px;
  font-weight: bold;
  white-space: nowrap;
  display: inline-block;
}

.vip-tag-normal {
  background-color: #f56c6c;
  color: white;
}

.vip-tag-svip {
  background: linear-gradient(45deg, #dc2626, #1d4ed8, #ca8a04, #dc2626);
  background-size: 400% 400%;
  animation: gradient-shift 2.5s ease infinite;
  color: white;
  font-weight: bold;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.6);
  border: 1px solid rgba(255, 255, 255, 0.3);
}

@keyframes gradient-shift {
  0% { background-position: 0% 50%; }
  25% { background-position: 100% 0%; }
  50% { background-position: 100% 100%; }
  75% { background-position: 0% 100%; }
  100% { background-position: 0% 50%; }
}

.vip-benefits {
  font-size: 12px;
  color: #909399;
  margin-left: 8px;
}

/* 加载状态样式 */
.loading-content {
  padding: 40px 20px;
  text-align: center;
  min-height: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.loading-quota {
  padding: 20px;
}

@media (max-width: 768px) {
  .quota-info-section .info-item {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .quota-info-section .info-item label {
    margin-bottom: 5px;
  }
  
  .vip-benefits {
    display: block;
    margin-top: 5px;
    margin-left: 0;
  }

  .quota-text {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .quota-text .used,
  .quota-text .remaining,
  .quota-text .total {
    margin-bottom: 5px;
  }
  
  .quota-section {
    margin-bottom: 20px;
  }
  
  .loading-quota {
    height: 150px;
  }
}
</style>
