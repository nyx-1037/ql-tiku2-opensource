<template>
  <div class="security-settings">
    <div class="card-header">
      <h3>安全设置</h3>
      <el-button type="primary" @click="saveSecuritySettings">保存设置</el-button>
    </div>
    
    <el-form :model="securitySettings" label-width="120px" style="margin-top: 20px;">
      <el-form-item label="密码最小长度">
        <el-input-number
          v-model="securitySettings.min_password_length"
          :min="6"
          :max="20"
          :step="1"
        />
        <div class="form-tip">用户密码的最小长度要求</div>
      </el-form-item>
      
      <el-form-item label="密码复杂度">
        <el-checkbox-group v-model="securitySettings.password_requirements">
          <el-checkbox label="uppercase">包含大写字母</el-checkbox>
          <el-checkbox label="lowercase">包含小写字母</el-checkbox>
          <el-checkbox label="numbers">包含数字</el-checkbox>
          <el-checkbox label="symbols">包含特殊字符</el-checkbox>
        </el-checkbox-group>
      </el-form-item>
      
      <el-form-item label="登录失败锁定">
        <el-switch v-model="securitySettings.enable_login_lock" />
        <div class="form-tip">启用后，连续登录失败将临时锁定账户</div>
      </el-form-item>
      
      <template v-if="securitySettings.enable_login_lock">
        <el-form-item label="最大失败次数">
          <el-input-number
            v-model="securitySettings.max_login_attempts"
            :min="3"
            :max="10"
            :step="1"
          />
          <div class="form-tip">连续登录失败的最大次数</div>
        </el-form-item>
        
        <el-form-item label="锁定时间">
          <el-input-number
            v-model="securitySettings.lock_duration"
            :min="5"
            :max="1440"
            :step="5"
          />
          <span style="margin-left: 10px;">分钟</span>
          <div class="form-tip">账户被锁定的时间长度</div>
        </el-form-item>
      </template>
      
      <el-form-item label="会话超时">
        <el-input-number
          v-model="securitySettings.session_timeout"
          :min="30"
          :max="1440"
          :step="30"
        />
        <span style="margin-left: 10px;">分钟</span>
        <div class="form-tip">用户无操作时的会话超时时间</div>
      </el-form-item>
      
      <el-form-item label="强制HTTPS">
        <el-switch v-model="securitySettings.force_https" />
        <div class="form-tip">强制使用HTTPS协议访问系统</div>
      </el-form-item>
      
      <el-form-item label="启用验证码">
        <el-switch v-model="securitySettings.enable_captcha" />
        <div class="form-tip">登录时是否需要验证码验证</div>
      </el-form-item>
      
      <el-form-item label="IP白名单">
        <el-input
          v-model="securitySettings.ip_whitelist"
          type="textarea"
          :rows="3"
          placeholder="请输入允许访问的IP地址，每行一个，支持CIDR格式"
        />
        <div class="form-tip">留空表示允许所有IP访问，支持格式：192.168.1.1 或 192.168.1.0/24</div>
      </el-form-item>
      
      <el-form-item label="API访问限制">
        <el-switch v-model="securitySettings.enable_api_rate_limit" />
        <div class="form-tip">启用API访问频率限制</div>
      </el-form-item>
      
      <template v-if="securitySettings.enable_api_rate_limit">
        <el-form-item label="每分钟请求数">
          <el-input-number
            v-model="securitySettings.api_rate_limit"
            :min="10"
            :max="1000"
            :step="10"
          />
          <div class="form-tip">每个IP每分钟允许的API请求次数</div>
        </el-form-item>
      </template>
      
      <el-form-item label="数据加密">
        <el-switch v-model="securitySettings.enable_data_encryption" />
        <div class="form-tip">启用敏感数据加密存储</div>
      </el-form-item>
      
      <el-form-item label="操作日志">
        <el-switch v-model="securitySettings.enable_audit_log" />
        <div class="form-tip">记录用户的重要操作日志</div>
      </el-form-item>
      
      <el-form-item label="双因子认证">
        <el-switch v-model="securitySettings.enable_2fa" />
        <div class="form-tip">启用双因子认证增强安全性</div>
      </el-form-item>
    </el-form>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { systemAPI } from '../../api/index.js'

export default {
  name: 'SecuritySettings',
  setup() {
    const securitySettings = ref({
      min_password_length: 8,
      password_requirements: ['lowercase', 'numbers'],
      enable_login_lock: true,
      max_login_attempts: 5,
      lock_duration: 30,
      session_timeout: 120,
      force_https: false,
      enable_captcha: true,
      ip_whitelist: '',
      enable_api_rate_limit: true,
      api_rate_limit: 100,
      enable_data_encryption: true,
      enable_audit_log: true,
      enable_2fa: false
    })

    const loadSecuritySettings = async () => {
      try {
        const response = await systemAPI.getSecuritySettings()
        const data = response.data || response
        
        // 从数据库加载安全设置
        const extractedData = {
          min_password_length: parseInt(data['sys.password.minlength']) || 8,
          password_requirements: [
            data['sys.password.uppercase'] === 'true' ? 'uppercase' : null,
            data['sys.password.lowercase'] === 'true' ? 'lowercase' : null,
            data['sys.password.numbers'] === 'true' ? 'numbers' : null,
            data['sys.password.symbols'] === 'true' ? 'symbols' : null
          ].filter(Boolean),
          enable_login_lock: data['sys.login.lockout'] !== '0',
          max_login_attempts: parseInt(data['sys.login.maxattempts']) || 5,
          lock_duration: parseInt(data['sys.login.lockout']) || 15,
          session_timeout: parseInt(data['sys.session.timeout']) || 120,
          force_https: data['sys.force.https'] === 'true',
          enable_captcha: data['sys.captcha.enable'] === 'true',
          ip_whitelist: data['sys.ip.whitelist'] || '',
          enable_api_rate_limit: data['sys.api.rate.limit.enable'] === 'true',
          api_rate_limit: parseInt(data['sys.api.rate.limit']) || 100,
          enable_data_encryption: data['sys.data.encryption.enable'] === 'true',
          enable_audit_log: data['sys.audit.log.enable'] === 'true',
          enable_2fa: data['sys.twofactor.enable'] === 'true'
        }
        
        Object.assign(securitySettings.value, extractedData)
      } catch (error) {
        console.error('加载安全设置失败:', error)
        ElMessage.error('加载安全设置失败')
      }
    }

    const saveSecuritySettings = async () => {
      try {
        const settingsToSave = {
          'sys.password.minlength': securitySettings.value.min_password_length.toString(),
          'sys.password.uppercase': securitySettings.value.password_requirements.includes('uppercase').toString(),
          'sys.password.lowercase': securitySettings.value.password_requirements.includes('lowercase').toString(),
          'sys.password.numbers': securitySettings.value.password_requirements.includes('numbers').toString(),
          'sys.password.symbols': securitySettings.value.password_requirements.includes('symbols').toString(),
          'sys.login.maxattempts': securitySettings.value.max_login_attempts.toString(),
          'sys.login.lockout': securitySettings.value.enable_login_lock ? securitySettings.value.lock_duration.toString() : '0',
          'sys.session.timeout': securitySettings.value.session_timeout.toString(),
          'sys.force.https': securitySettings.value.force_https.toString(),
          'sys.captcha.enable': securitySettings.value.enable_captcha.toString(),
          'sys.ip.whitelist': securitySettings.value.ip_whitelist,
          'sys.api.rate.limit.enable': securitySettings.value.enable_api_rate_limit.toString(),
          'sys.api.rate.limit': securitySettings.value.api_rate_limit.toString(),
          'sys.data.encryption.enable': securitySettings.value.enable_data_encryption.toString(),
          'sys.audit.log.enable': securitySettings.value.enable_audit_log.toString(),
          'sys.twofactor.enable': securitySettings.value.enable_2fa.toString()
        }
        await systemAPI.saveSecuritySettings(settingsToSave)
        ElMessage.success('安全设置保存成功')
      } catch (error) {
        ElMessage.error('保存失败')
        console.error('保存安全设置失败:', error)
      }
    }

    onMounted(() => {
      loadSecuritySettings()
    })

    return {
      securitySettings,
      saveSecuritySettings
    }
  }
}
</script>

<style scoped>
.security-settings {
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

.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 5px;
}

.el-checkbox-group .el-checkbox {
  margin-right: 15px;
  margin-bottom: 10px;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .security-settings {
    padding: 10px;
  }
  
  .card-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
  
  .card-header .el-button {
    width: 100%;
  }
  
  .el-checkbox-group .el-checkbox {
    display: block;
    margin-right: 0;
    margin-bottom: 10px;
    padding: 8px;
    background: #f5f7fa;
    border-radius: 4px;
  }
}

@media (max-width: 480px) {
  .security-settings {
    padding: 5px;
  }
  
  .card-header h3 {
    font-size: 16px;
  }
}
</style>
