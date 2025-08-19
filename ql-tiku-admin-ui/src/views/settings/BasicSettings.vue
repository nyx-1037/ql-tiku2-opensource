<template>
  <div class="basic-settings">
    <div class="card-header">
      <h3>基本设置</h3>
      <el-button type="primary" @click="saveBasicSettings">保存设置</el-button>
    </div>
    
    <el-form :model="basicSettings" label-width="120px" style="margin-top: 20px;">
      <el-form-item label="系统名称">
        <el-input v-model="basicSettings.site_name" placeholder="请输入系统名称" />
      </el-form-item>
      
      <el-form-item label="系统描述">
        <el-input
          v-model="basicSettings.site_description"
          type="textarea"
          :rows="3"
          placeholder="请输入系统描述"
        />
      </el-form-item>
      
      <el-form-item label="系统版本">
        <el-input v-model="basicSettings.system_version" placeholder="请输入系统版本" />
      </el-form-item>
      
      <el-form-item label="管理员邮箱">
        <el-input v-model="basicSettings.admin_email" placeholder="请输入管理员邮箱" />
      </el-form-item>
      
      <el-form-item label="允许注册">
        <el-switch v-model="basicSettings.allow_register" />
      </el-form-item>
      
      <el-form-item label="注册码校验">
        <el-switch v-model="basicSettings.registration_code_required" />
        <div class="form-tip">开启后用户注册时必须输入有效的注册码</div>
      </el-form-item>
      
      <el-form-item label="维护模式">
        <el-switch v-model="basicSettings.maintenance_mode" />
      </el-form-item>
      
      <el-form-item label="维护提示" v-if="basicSettings.maintenance_mode">
        <el-input
          v-model="basicSettings.maintenance_message"
          type="textarea"
          :rows="2"
          placeholder="请输入维护提示信息"
        />
      </el-form-item>
      
      <el-form-item label="系统Logo">
        <div class="logo-upload">
          <el-upload
            class="logo-uploader"
            action="/api/admin/upload/logo"
            :show-file-list="false"
            :on-success="handleLogoSuccess"
            :before-upload="beforeLogoUpload"
          >
            <img v-if="basicSettings.site_logo" :src="basicSettings.site_logo" class="logo" />
            <el-icon v-else class="logo-uploader-icon"><Plus /></el-icon>
          </el-upload>
          <div class="logo-tips">
            <p>建议尺寸：200x60像素</p>
            <p>支持格式：JPG、PNG</p>
            <p>文件大小：不超过2MB</p>
            <el-button v-if="basicSettings.site_logo" size="small" type="danger" @click="removeLogo">删除Logo</el-button>
          </div>
        </div>
      </el-form-item>
      
      <el-form-item label="ICP备案号">
        <el-input v-model="basicSettings.icp_number" placeholder="请输入ICP备案号，如：京ICP备12345678号" />
      </el-form-item>
      
      <el-form-item label="ICP备案链接">
        <el-input v-model="basicSettings.icp_url" placeholder="请输入ICP备案链接，如：http://beian.miit.gov.cn/" />
      </el-form-item>
      
      <el-form-item label="公安备案号">
        <el-input v-model="basicSettings.police_number" placeholder="请输入公安备案号，如：京公网安备11010802012345号" />
      </el-form-item>
      
      <el-form-item label="公安备案链接">
        <el-input v-model="basicSettings.police_url" placeholder="请输入公安备案链接" />
      </el-form-item>
      
      <el-form-item label="公安备案图标">
        <el-input v-model="basicSettings.police_icon" placeholder="请输入公安备案图标URL" />
      </el-form-item>
      
      <el-form-item label="版权信息">
        <el-input
          v-model="basicSettings.copyright"
          type="textarea"
          :rows="2"
          placeholder="请输入版权信息，如：© 2024 刷题网站. All rights reserved."
        />
      </el-form-item>
      
      <el-form-item label="每日一语API">
        <el-input
          v-model="basicSettings.daily_quote_api"
          placeholder="请输入每日一语API地址"
        />
        <div class="form-tip">用于获取每日一语内容的API接口地址</div>
      </el-form-item>
      
      <!-- 新增缓存管理区域 -->
      <el-divider content-position="left">缓存管理</el-divider>
      
      <el-form-item label="缓存操作">
        <div class="cache-actions">
          <el-button type="warning" @click="clearQuestionCache" :loading="clearingCache.question">
            <el-icon><Delete /></el-icon>
            清除题目缓存
          </el-button>
          <el-button type="danger" @click="clearAllCache" :loading="clearingCache.all">
            <el-icon><DeleteFilled /></el-icon>
            清除全部缓存
          </el-button>
        </div>
        <div class="form-tip">清除缓存后，系统会重新加载相关数据，可能会导致短暂性能下降</div>
      </el-form-item>
    </el-form>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Delete, DeleteFilled } from '@element-plus/icons-vue'
import { systemAPI, cacheAPI } from '../../api/index.js'

export default {
  name: 'BasicSettings',
  components: {
    Plus,
    Delete,
    DeleteFilled
  },
  setup() {
    const basicSettings = ref({
      site_name: '',
      site_description: '',
      system_version: '',
      admin_email: '',
      allow_register: true,
      registration_code_required: false,
      maintenance_mode: false,
      maintenance_message: '',
      site_logo: '',
      icp_number: '',
      icp_url: '',
      police_number: '',
      police_url: '',
      police_icon: '',
      copyright: '',
      daily_quote_api: ''
    })

    const clearingCache = ref({
      question: false,
      all: false
    })

    const loadBasicSettings = async () => {
      try {
        const response = await systemAPI.getBasicSettings()
        const data = response.data || response
        
        // 从统一配置中提取基本设置相关的数据
        const extractedData = {
          site_name: data['sys.site.name'] || data.site_name || '',
          site_description: data['sys.site.description'] || data.site_description || '',
          system_version: data['sys.site.version'] || data.system_version || '',
          admin_email: data['sys.admin.email'] || data.admin_email || '',
          allow_register: data['sys.allow.register'] === 'true' || data.allow_register || false,
          registration_code_required: data['sys.registration.code.required'] === 'true' || data.registration_code_required || false,
          maintenance_mode: data['sys.maintenance.mode'] === 'true' || data.maintenance_mode || false,
          maintenance_message: data['sys.maintenance.message'] || data.maintenance_message || '',
          site_logo: data['sys.site.logo'] || data.site_logo || '',
          icp_number: data['sys.site.icp'] || data.icp_number || '',
          icp_url: data['sys.site.icp.url'] || data.icp_url || '',
          police_number: data['sys.site.police'] || data.police_number || '',
          police_url: data['sys.site.police.url'] || data.police_url || '',
          police_icon: data['sys.site.police.icon'] || data.police_icon || '',
          copyright: data['sys.site.copyright'] || data.copyright || '',
          daily_quote_api: data['sys.daily.quote.api'] || data.daily_quote_api || ''
        }
        
        Object.assign(basicSettings.value, extractedData)
      } catch (error) {
        console.error('加载基本设置失败:', error)
        ElMessage.error('加载基本设置失败')
      }
    }

    const saveBasicSettings = async () => {
      try {
        const settingsToSave = {
          'sys.site.name': String(basicSettings.value.site_name || ''),
          'sys.site.description': String(basicSettings.value.site_description || ''),
          'sys.site.version': String(basicSettings.value.system_version || ''),
          'sys.admin.email': String(basicSettings.value.admin_email || ''),
          'sys.allow.register': String(basicSettings.value.allow_register),
          'sys.registration.code.required': String(basicSettings.value.registration_code_required),
          'sys.maintenance.mode': String(basicSettings.value.maintenance_mode),
          'sys.maintenance.message': String(basicSettings.value.maintenance_message || ''),
          'sys.site.logo': String(basicSettings.value.site_logo || ''),
          'sys.site.icp': String(basicSettings.value.icp_number || ''),
          'sys.site.icp.url': String(basicSettings.value.icp_url || ''),
          'sys.site.police': String(basicSettings.value.police_number || ''),
          'sys.site.police.url': String(basicSettings.value.police_url || ''),
          'sys.site.police.icon': String(basicSettings.value.police_icon || ''),
          'sys.site.copyright': String(basicSettings.value.copyright || ''),
          'sys.daily.quote.api': String(basicSettings.value.daily_quote_api || '')
        }
        
        await systemAPI.saveBasicSettings(settingsToSave)
        ElMessage.success('基本设置保存成功')
      } catch (error) {
        ElMessage.error('保存失败')
        console.error('保存基本设置失败:', error)
      }
    }

    const handleLogoSuccess = (response) => {
      if (response.code === 200) {
        basicSettings.value.site_logo = response.data.url
        ElMessage.success('Logo上传成功')
      } else {
        ElMessage.error(response.message || 'Logo上传失败')
      }
    }

    const beforeLogoUpload = (file) => {
      const isJPG = file.type === 'image/jpeg' || file.type === 'image/png'
      const isLt2M = file.size / 1024 / 1024 < 2

      if (!isJPG) {
        ElMessage.error('Logo只能是 JPG/PNG 格式!')
        return false
      }
      if (!isLt2M) {
        ElMessage.error('Logo大小不能超过 2MB!')
        return false
      }
      return true
    }

    const removeLogo = () => {
      basicSettings.value.site_logo = ''
      ElMessage.success('Logo已删除')
    }

    // 新增缓存清除方法
    const clearQuestionCache = async () => {
      try {
        await ElMessageBox.confirm(
          '确定要清除题目缓存吗？清除后系统会重新加载题目数据，可能会导致短暂性能下降。',
          '确认清除',
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
          }
        )
        
        clearingCache.value.question = true
        await cacheAPI.clearQuestionCache()
        ElMessage.success('题目缓存清除成功')
      } catch (error) {
        if (error !== 'cancel') {
          console.error('清除题目缓存失败:', error)
          ElMessage.error('清除题目缓存失败')
        }
      } finally {
        clearingCache.value.question = false
      }
    }

    const clearAllCache = async () => {
      try {
        await ElMessageBox.confirm(
          '确定要清除所有缓存吗？这将清除包括题目、用户、配置等所有缓存数据，可能会导致系统短暂性能下降。',
          '危险操作',
          {
            confirmButtonText: '确定清除',
            cancelButtonText: '取消',
            type: 'error'
          }
        )
        
        clearingCache.value.all = true
        await cacheAPI.clearAllCache()
        ElMessage.success('所有缓存清除成功')
      } catch (error) {
        if (error !== 'cancel') {
          console.error('清除所有缓存失败:', error)
          ElMessage.error('清除所有缓存失败')
        }
      } finally {
        clearingCache.value.all = false
      }
    }

    onMounted(() => {
      loadBasicSettings()
    })

    return {
      basicSettings,
      clearingCache,
      saveBasicSettings,
      handleLogoSuccess,
      beforeLogoUpload,
      removeLogo,
      clearQuestionCache,
      clearAllCache
    }
  }
}
</script>

<style scoped>
.basic-settings {
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

.logo-upload {
  display: flex;
  align-items: flex-start;
  gap: 20px;
}

.logo-uploader {
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  width: 200px;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.logo-uploader:hover {
  border-color: #409eff;
}

.logo-uploader-icon {
  font-size: 28px;
  color: #8c939d;
}

.logo {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.logo-tips {
  flex: 1;
}

.logo-tips p {
  margin: 0 0 5px 0;
  font-size: 12px;
  color: #909399;
}

.cache-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 5px;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .basic-settings {
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
  
  .logo-upload {
    flex-direction: column;
    gap: 10px;
  }
  
  .logo-uploader {
    width: 100%;
    max-width: 200px;
  }
  
  .cache-actions {
    flex-direction: column;
  }
  
  .cache-actions .el-button {
    width: 100%;
  }
}

@media (max-width: 480px) {
  .basic-settings {
    padding: 5px;
  }
  
  .card-header h3 {
    font-size: 16px;
  }
}
</style>
