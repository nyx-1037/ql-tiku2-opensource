<template>
  <div class="storage-settings">
    <div class="card-header">
      <h3>存储设置</h3>
      <el-button type="primary" @click="saveStorageSettings">保存设置</el-button>
    </div>
    
    <el-form :model="storageSettings" label-width="120px" style="margin-top: 20px;">
      <el-form-item label="存储类型">
        <el-radio-group v-model="storageSettings.storage_type">
          <el-radio label="local">本地存储</el-radio>
          <el-radio label="oss">阿里云OSS</el-radio>
          <el-radio label="cos">腾讯云COS</el-radio>
          <el-radio label="qiniu">七牛云</el-radio>
        </el-radio-group>
      </el-form-item>
      
      <!-- 本地存储设置 -->
      <template v-if="storageSettings.storage_type === 'local'">
        <el-form-item label="存储路径">
          <el-input v-model="storageSettings.local_path" placeholder="请输入本地存储路径" />
          <div class="form-tip">文件将存储在服务器的指定目录中</div>
        </el-form-item>
        
        <el-form-item label="访问URL前缀">
          <el-input v-model="storageSettings.local_url_prefix" placeholder="请输入访问URL前缀" />
          <div class="form-tip">用于生成文件的访问链接</div>
        </el-form-item>
      </template>
      
      <!-- 阿里云OSS设置 -->
      <template v-if="storageSettings.storage_type === 'oss'">
        <el-form-item label="AccessKey ID">
          <el-input v-model="storageSettings.oss_access_key_id" placeholder="请输入AccessKey ID" />
        </el-form-item>
        
        <el-form-item label="AccessKey Secret">
          <el-input
            v-model="storageSettings.oss_access_key_secret"
            type="password"
            placeholder="请输入AccessKey Secret"
            show-password
          />
        </el-form-item>
        
        <el-form-item label="Bucket名称">
          <el-input v-model="storageSettings.oss_bucket" placeholder="请输入Bucket名称" />
        </el-form-item>
        
        <el-form-item label="地域节点">
          <el-input v-model="storageSettings.oss_endpoint" placeholder="请输入地域节点" />
        </el-form-item>
        
        <el-form-item label="自定义域名">
          <el-input v-model="storageSettings.oss_domain" placeholder="请输入自定义域名（可选）" />
        </el-form-item>
      </template>
      
      <!-- 腾讯云COS设置 -->
      <template v-if="storageSettings.storage_type === 'cos'">
        <el-form-item label="SecretId">
          <el-input v-model="storageSettings.cos_secret_id" placeholder="请输入SecretId" />
        </el-form-item>
        
        <el-form-item label="SecretKey">
          <el-input
            v-model="storageSettings.cos_secret_key"
            type="password"
            placeholder="请输入SecretKey"
            show-password
          />
        </el-form-item>
        
        <el-form-item label="Bucket名称">
          <el-input v-model="storageSettings.cos_bucket" placeholder="请输入Bucket名称" />
        </el-form-item>
        
        <el-form-item label="地域">
          <el-input v-model="storageSettings.cos_region" placeholder="请输入地域" />
        </el-form-item>
        
        <el-form-item label="自定义域名">
          <el-input v-model="storageSettings.cos_domain" placeholder="请输入自定义域名（可选）" />
        </el-form-item>
      </template>
      
      <!-- 七牛云设置 -->
      <template v-if="storageSettings.storage_type === 'qiniu'">
        <el-form-item label="AccessKey">
          <el-input v-model="storageSettings.qiniu_access_key" placeholder="请输入AccessKey" />
        </el-form-item>
        
        <el-form-item label="SecretKey">
          <el-input
            v-model="storageSettings.qiniu_secret_key"
            type="password"
            placeholder="请输入SecretKey"
            show-password
          />
        </el-form-item>
        
        <el-form-item label="存储空间">
          <el-input v-model="storageSettings.qiniu_bucket" placeholder="请输入存储空间名称" />
        </el-form-item>
        
        <el-form-item label="访问域名">
          <el-input v-model="storageSettings.qiniu_domain" placeholder="请输入访问域名" />
        </el-form-item>
      </template>
      
      <el-form-item label="最大文件大小">
        <el-input-number
          v-model="storageSettings.max_file_size"
          :min="1"
          :max="1024"
          :step="1"
        />
        <span style="margin-left: 10px;">MB</span>
      </el-form-item>
      
      <el-form-item label="允许的文件类型">
        <el-checkbox-group v-model="storageSettings.allowed_file_types">
          <el-checkbox label="jpg">JPG</el-checkbox>
          <el-checkbox label="png">PNG</el-checkbox>
          <el-checkbox label="gif">GIF</el-checkbox>
          <el-checkbox label="pdf">PDF</el-checkbox>
          <el-checkbox label="doc">DOC</el-checkbox>
          <el-checkbox label="docx">DOCX</el-checkbox>
          <el-checkbox label="xls">XLS</el-checkbox>
          <el-checkbox label="xlsx">XLSX</el-checkbox>
          <el-checkbox label="ppt">PPT</el-checkbox>
          <el-checkbox label="pptx">PPTX</el-checkbox>
          <el-checkbox label="txt">TXT</el-checkbox>
          <el-checkbox label="zip">ZIP</el-checkbox>
          <el-checkbox label="rar">RAR</el-checkbox>
        </el-checkbox-group>
      </el-form-item>
    </el-form>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { systemAPI } from '../../api/index.js'

export default {
  name: 'StorageSettings',
  setup() {
    const storageSettings = ref({
      storage_type: 'local',
      // 本地存储
      local_path: '/uploads',
      local_url_prefix: '/uploads',
      // 阿里云OSS
      oss_access_key_id: '',
      oss_access_key_secret: '',
      oss_bucket: '',
      oss_endpoint: '',
      oss_domain: '',
      // 腾讯云COS
      cos_secret_id: '',
      cos_secret_key: '',
      cos_bucket: '',
      cos_region: '',
      cos_domain: '',
      // 七牛云
      qiniu_access_key: '',
      qiniu_secret_key: '',
      qiniu_bucket: '',
      qiniu_domain: '',
      // 通用设置
      max_file_size: 10,
      allowed_file_types: ['jpg', 'png', 'gif', 'pdf', 'doc', 'docx']
    })

    const loadStorageSettings = async () => {
      try {
        const response = await systemAPI.getStorageSettings()
        const data = response.data || response
        
        // 从统一配置中提取存储设置相关的数据
        const extractedData = {
          storage_type: data['sys.storage.type'] || data.storage_type || 'local',
          // 本地存储
          local_path: data['sys.storage.path'] || data.local_path || '/uploads',
          local_url_prefix: data['sys.storage.domain'] || data.local_url_prefix || '/uploads',
          // 阿里云OSS - 使用本地存储配置作为默认值
          oss_access_key_id: '',
          oss_access_key_secret: '',
          oss_bucket: '',
          oss_endpoint: '',
          oss_domain: '',
          // 腾讯云COS - 使用本地存储配置作为默认值
          cos_secret_id: '',
          cos_secret_key: '',
          cos_bucket: '',
          cos_region: '',
          cos_domain: '',
          // 七牛云
          qiniu_access_key: '',
          qiniu_secret_key: '',
          qiniu_bucket: '',
          qiniu_domain: data['sys.qiniu.domain'] || '',
          // 通用设置
          max_file_size: parseInt(data['sys.storage.maxsize'] || data.max_file_size || 10),
          allowed_file_types: data['sys.storage.types'] ? 
            (typeof data['sys.storage.types'] === 'string' ? 
              data['sys.storage.types'].split(',') : 
              data['sys.storage.types']) : 
            (data.allowed_file_types || ['jpg', 'png', 'gif', 'pdf', 'doc', 'docx'])
        }
        
        Object.assign(storageSettings.value, extractedData)
      } catch (error) {
        console.error('加载存储设置失败:', error)
        ElMessage.error('加载存储设置失败')
      }
    }

    const saveStorageSettings = async () => {
      try {
        const settingsToSave = {
          'sys.storage.type': storageSettings.value.storage_type,
          'sys.storage.path': storageSettings.value.local_path,
          'sys.storage.domain': storageSettings.value.local_url_prefix,
          'sys.storage.maxsize': storageSettings.value.max_file_size.toString(),
          'sys.storage.types': Array.isArray(storageSettings.value.allowed_file_types) ? 
            storageSettings.value.allowed_file_types.join(',') : 
            storageSettings.value.allowed_file_types,
          'sys.oss.accesskey': storageSettings.value.oss_access_key_id,
          'sys.oss.secretkey': storageSettings.value.oss_access_key_secret,
          'sys.oss.bucket': storageSettings.value.oss_bucket,
          'sys.oss.region': storageSettings.value.oss_endpoint,
          'sys.cos.secretid': storageSettings.value.cos_secret_id,
          'sys.cos.secretkey': storageSettings.value.cos_secret_key,
          'sys.cos.bucket': storageSettings.value.cos_bucket,
          'sys.cos.region': storageSettings.value.cos_region,
          'sys.qiniu.accesskey': storageSettings.value.qiniu_access_key,
          'sys.qiniu.secretkey': storageSettings.value.qiniu_secret_key,
          'sys.qiniu.bucket': storageSettings.value.qiniu_bucket,
          'sys.qiniu.domain': storageSettings.value.qiniu_domain
        }
        await systemAPI.saveStorageSettings(settingsToSave)
        ElMessage.success('存储设置保存成功')
      } catch (error) {
        ElMessage.error('保存失败')
        console.error('保存存储设置失败:', error)
      }
    }

    onMounted(() => {
      loadStorageSettings()
    })

    return {
      storageSettings,
      saveStorageSettings
    }
  }
}
</script>

<style scoped>
.storage-settings {
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

.el-radio-group .el-radio {
  margin-right: 20px;
  margin-bottom: 10px;
}

.el-checkbox-group .el-checkbox {
  margin-right: 15px;
  margin-bottom: 10px;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .storage-settings {
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
  
  .el-radio-group .el-radio {
    display: block;
    margin-right: 0;
    margin-bottom: 15px;
    padding: 10px;
    background: #f5f7fa;
    border-radius: 4px;
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
  .storage-settings {
    padding: 5px;
  }
  
  .card-header h3 {
    font-size: 16px;
  }
}
</style>
