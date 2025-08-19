<template>
  <div class="email-settings">
    <div class="card-header">
      <h3>邮件设置</h3>
      <div>
        <el-button @click="testEmail">测试邮件</el-button>
        <el-button type="primary" @click="saveEmailSettings">保存设置</el-button>
      </div>
    </div>
    
    <el-form :model="emailSettings" label-width="120px" style="margin-top: 20px;">
      <el-form-item label="SMTP服务器">
        <el-input v-model="emailSettings.smtpHost" placeholder="请输入SMTP服务器地址" />
      </el-form-item>
      
      <el-form-item label="SMTP端口">
        <el-input-number v-model="emailSettings.smtpPort" :min="1" :max="65535" />
      </el-form-item>
      
      <el-form-item label="发件人邮箱">
        <el-input v-model="emailSettings.fromEmail" placeholder="请输入发件人邮箱" />
      </el-form-item>
      
      <el-form-item label="发件人名称">
        <el-input v-model="emailSettings.fromName" placeholder="请输入发件人名称" />
      </el-form-item>
      
      <el-form-item label="邮箱密码">
        <el-input
          v-model="emailSettings.password"
          type="password"
          placeholder="请输入邮箱密码或授权码"
          show-password
        />
      </el-form-item>
      
      <el-form-item label="启用TLS">
        <el-switch v-model="emailSettings.enableTLS" />
      </el-form-item>
    </el-form>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { systemAPI } from '../../api/index.js'

export default {
  name: 'EmailSettings',
  setup() {
    const emailSettings = ref({
      smtpHost: '',
      smtpPort: 587,
      fromEmail: '',
      fromName: '',
      password: '',
      enableTLS: true
    })

    const loadEmailSettings = async () => {
      try {
        const response = await systemAPI.getEmailSettings()
        const data = response.data || response
        
        // 从统一配置中提取邮件设置相关的数据
        const extractedData = {
          smtpHost: data['sys.email.host'] || data.smtpHost || '',
          smtpPort: parseInt(data['sys.email.port'] || data.smtpPort || 587),
          fromEmail: data['sys.email.from'] || data.fromEmail || '',
          fromName: data['sys.email.name'] || data.fromName || '',
          password: data['sys.email.password'] || data.password || '',
          enableTLS: data['sys.email.tls'] === 'true' || data.enableTLS || true
        }
        
        Object.assign(emailSettings.value, extractedData)
      } catch (error) {
        console.error('加载邮件设置失败:', error)
        ElMessage.error('加载邮件设置失败')
      }
    }

    const saveEmailSettings = async () => {
      try {
        const settingsToSave = {
          'sys.email.host': emailSettings.value.smtpHost,
          'sys.email.port': emailSettings.value.smtpPort.toString(),
          'sys.email.from': emailSettings.value.fromEmail,
          'sys.email.name': emailSettings.value.fromName,
          'sys.email.password': emailSettings.value.password,
          'sys.email.tls': emailSettings.value.enableTLS.toString(),
          'sys.email.ssl': 'false'
        }
        await systemAPI.saveEmailSettings(settingsToSave)
        ElMessage.success('邮件设置保存成功')
      } catch (error) {
        ElMessage.error('保存失败')
        console.error('保存邮件设置失败:', error)
      }
    }

    const testEmail = async () => {
      try {
        await systemAPI.testEmail(emailSettings.value)
        ElMessage.success('测试邮件发送成功')
      } catch (error) {
        ElMessage.error('测试邮件发送失败')
        console.error('测试邮件失败:', error)
      }
    }

    onMounted(() => {
      loadEmailSettings()
    })

    return {
      emailSettings,
      saveEmailSettings,
      testEmail
    }
  }
}
</script>

<style scoped>
.email-settings {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid #ebeef5;
}

.card-header h3 {
  margin: 0;
  font-size: 18px;
  color: #303133;
}

.card-header div {
  display: flex;
  gap: 10px;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .email-settings {
    padding: 10px;
  }
  
  .card-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
  
  .card-header div {
    width: 100%;
    flex-direction: column;
  }
  
  .card-header .el-button {
    width: 100%;
  }
}

@media (max-width: 480px) {
  .email-settings {
    padding: 5px;
  }
  
  .card-header h3 {
    font-size: 16px;
  }
}
</style>
