<template>
  <div class="profile-settings">
    <el-card class="settings-card">
      <template #header>
        <div class="card-header">
          <el-icon><User /></el-icon>
          <span>个人信息设置</span>
        </div>
      </template>

      <!-- 管理员基本信息 -->
      <el-form
        ref="profileFormRef"
        :model="profileForm"
        :rules="profileRules"
        label-width="100px"
        class="profile-form"
      >
        <h3 class="section-title">基本信息</h3>
        
        <!-- 头像上传 -->
        <el-form-item label="头像">
          <el-upload
            class="avatar-uploader"
            :action="uploadAction"
            :show-file-list="false"
            :on-success="handleAvatarSuccess"
            :before-upload="beforeAvatarUpload"
            :headers="uploadHeaders"
            name="file"
          >
            <img v-if="profileForm.avatar" :src="profileForm.avatar" class="avatar" />
            <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
          </el-upload>
          <div class="upload-tip">支持 JPG、PNG 格式，大小不超过 2MB</div>
        </el-form-item>

        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="profileForm.nickname" placeholder="请输入昵称" />
        </el-form-item>

        <el-form-item label="邮箱" prop="email">
          <el-input v-model="profileForm.email" placeholder="请输入邮箱" />
        </el-form-item>

        <el-form-item label="手机号" prop="phone">
          <el-input v-model="profileForm.phone" placeholder="请输入手机号" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleProfileSubmit" :loading="profileLoading">
            保存信息
          </el-button>
        </el-form-item>
      </el-form>

      <!-- 修改密码 -->
      <el-form
        ref="passwordFormRef"
        :model="passwordForm"
        :rules="passwordRules"
        label-width="100px"
        class="password-form"
      >
        <h3 class="section-title">修改密码</h3>
        
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
            placeholder="请输入新密码（6-18位，需包含数字和字母）"
            show-password
          />
        </el-form-item>

        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="passwordForm.confirmPassword"
            type="password"
            placeholder="请再次输入新密码"
            show-password
          />
        </el-form-item>

        <el-form-item>
          <el-button type="warning" @click="changePassword" :loading="changingPassword">
            修改密码
          </el-button>
          <el-button @click="resetPasswordForm">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue'
import { authAPI } from '@/api'
import { ElMessage } from 'element-plus'
import { User, Plus } from '@element-plus/icons-vue'

export default {
  name: 'ProfileSettings',
  components: {
    User,
    Plus
  },
  setup() {
    // 表单引用
    const profileFormRef = ref()
    const passwordFormRef = ref()
    
    // 个人信息表单
    const profileForm = reactive({
      nickname: '',
      email: '',
      phone: '',
      avatar: ''
    })

    // 密码表单
    const passwordForm = reactive({
      oldPassword: '',
      newPassword: '',
      confirmPassword: ''
    })

    // 加载状态
    const profileLoading = ref(false)
    const savingProfile = ref(false)
    const changingPassword = ref(false)

    // 上传配置
    const uploadAction = '/api/upload/avatar'
    const uploadHeaders = {
      'Authorization': localStorage.getItem('token') || ''
    }

    // 表单验证规则
    const profileRules = {
      nickname: [
        { required: true, message: '请输入昵称', trigger: 'blur' },
        { min: 2, max: 20, message: '长度在 2 到 20 个字符', trigger: 'blur' }
      ],
      email: [
        { required: true, message: '请输入邮箱', trigger: 'blur' },
        { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
      ],
      phone: [
        { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
      ]
    }

    const passwordRules = {
      oldPassword: [
        { required: true, message: '请输入原密码', trigger: 'blur' }
      ],
      newPassword: [
        { required: true, message: '请输入新密码', trigger: 'blur' },
        { min: 6, max: 18, message: '长度在 6 到 18 个字符', trigger: 'blur' },
        {
          pattern: /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]{6,18}$/,
          message: '密码必须包含数字和字母的组合',
          trigger: 'blur'
        }
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

    // 获取管理员信息
    const fetchAdminInfo = async () => {
      try {
        const response = await authAPI.getAdminInfo()
        // API返回的是直接的数据，不需要检查code
        if (response) {
          const info = response
          profileForm.nickname = info.nickname || ''
          profileForm.email = info.email || ''
          profileForm.phone = info.phone || ''
          profileForm.avatar = info.avatar || ''
        }
      } catch (error) {
        console.error('获取管理员信息失败:', error)
        ElMessage.error('获取管理员信息失败')
      }
    }

    // 头像上传成功处理
    const handleAvatarSuccess = (response) => {
      if (response && response.url) {
        profileForm.avatar = response.url
        ElMessage.success('头像上传成功')
      } else {
        ElMessage.error('头像上传失败')
      }
    }

    // 头像上传前验证
    const beforeAvatarUpload = (file) => {
      const isJPGorPNG = file.type === 'image/jpeg' || file.type === 'image/png'
      const isLt2M = file.size / 1024 / 1024 < 2

      if (!isJPGorPNG) {
        ElMessage.error('头像只能是 JPG 或 PNG 格式!')
        return false
      }
      if (!isLt2M) {
        ElMessage.error('头像大小不能超过 2MB!')
        return false
      }
      return true
    }

    // 使用handleProfileSubmit函数，移除saveProfile函数
    const handleProfileSubmit = async () => {
      if (!profileFormRef.value) return
      
      try {
        await profileFormRef.value.validate()
        profileLoading.value = true
        await authAPI.updateProfile({
          nickname: profileForm.nickname,
          email: profileForm.email,
          phone: profileForm.phone,
          avatar: profileForm.avatar
        })
        
        ElMessage.success('信息更新成功')
        await fetchAdminInfo() // 重新获取最新信息
      } catch (error) {
        console.error('信息更新失败:', error)
        ElMessage.error('信息更新失败')
      } finally {
        profileLoading.value = false
      }
    }

    // 修改密码
    const changePassword = async () => {
      if (!passwordFormRef.value) return
      
      try {
        await passwordFormRef.value.validate()
        changingPassword.value = true
        await authAPI.changePassword({
          oldPassword: passwordForm.oldPassword,
          newPassword: passwordForm.newPassword
        })
        
        ElMessage.success('密码修改成功')
        resetPasswordForm()
      } catch (error) {
        console.error('修改密码失败:', error)
        if (error !== false) { // 如果不是表单验证错误
          ElMessage.error('修改密码失败')
        }
      } finally {
        changingPassword.value = false
      }
    }

    // 重置密码表单
    const resetPasswordForm = () => {
      passwordForm.oldPassword = ''
      passwordForm.newPassword = ''
      passwordForm.confirmPassword = ''
    }

    // 组件挂载时加载管理员信息
    onMounted(() => {
      fetchAdminInfo()
    })

    return {
      profileFormRef,
      passwordFormRef,
      profileForm,
      passwordForm,
      profileRules,
      passwordRules,
      profileLoading,
      savingProfile,
      changingPassword,
      uploadAction,
      uploadHeaders,
      handleAvatarSuccess,
      beforeAvatarUpload,
      handleProfileSubmit,
      changePassword,
      resetPasswordForm
    }
  }
}
</script>

<style scoped>
.profile-settings {
  padding: 20px;
  max-width: 800px;
  margin: 0 auto;
}

.settings-card {
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.card-header {
  display: flex;
  align-items: center;
  font-size: 18px;
  font-weight: bold;
}

.card-header .el-icon {
  margin-right: 8px;
  font-size: 20px;
}

.section-title {
  margin: 20px 0 16px 0;
  padding-left: 10px;
  border-left: 4px solid #409eff;
  font-size: 16px;
  color: #303133;
}

.profile-form,
.password-form {
  margin-bottom: 30px;
}

:deep(.el-form-item__label) {
  font-weight: bold;
}

:deep(.el-input) {
  max-width: 400px;
}

:deep(.el-button) {
  margin-right: 10px;
}

/* 头像上传样式 */
.avatar-uploader {
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: var(--el-transition-duration-fast);
  width: 120px;
  height: 120px;
  display: inline-block;
}

.avatar-uploader:hover {
  border-color: var(--el-color-primary);
}

.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 120px;
  height: 120px;
  text-align: center;
  line-height: 120px;
}

.avatar {
  width: 120px;
  height: 120px;
  display: block;
  object-fit: cover;
}

.upload-tip {
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
  line-height: 1.4;
}

@media screen and (max-width: 768px) {
  .profile-settings {
    padding: 10px;
  }
  
  :deep(.el-form-item__label) {
    width: 80px !important;
  }
  
  :deep(.el-form-item__content) {
    margin-left: 80px !important;
  }
}
</style>

// 移除未使用的ElMessageBox引用