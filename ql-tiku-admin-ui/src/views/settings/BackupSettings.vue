<template>
  <div class="backup-settings">
    <div class="card-header">
      <h3>备份管理</h3>
      <div>
        <el-button @click="createBackup">立即备份</el-button>
        <el-button type="primary" @click="saveBackupSettings">保存设置</el-button>
      </div>
    </div>
    
    <!-- 自动备份设置 -->
    <div class="backup-section">
      <h4>自动备份设置</h4>
      <el-form :model="backupSettings" label-width="120px">
        <el-form-item label="启用自动备份">
          <el-switch v-model="backupSettings.enable_auto_backup" />
        </el-form-item>
        
        <template v-if="backupSettings.enable_auto_backup">
          <el-form-item label="备份频率">
            <el-select v-model="backupSettings.backup_frequency">
              <el-option label="每天" value="daily" />
              <el-option label="每周" value="weekly" />
              <el-option label="每月" value="monthly" />
            </el-select>
          </el-form-item>
          
          <el-form-item label="备份时间">
            <el-time-picker
              v-model="backupSettings.backup_time"
              format="HH:mm"
              value-format="HH:mm"
              placeholder="选择备份时间"
            />
          </el-form-item>
          
          <el-form-item label="保留份数">
            <el-input-number
              v-model="backupSettings.keep_backups"
              :min="1"
              :max="30"
              :step="1"
            />
            <div class="form-tip">超过此数量的旧备份将被自动删除</div>
          </el-form-item>
        </template>
        
        <el-form-item label="备份内容">
          <el-checkbox-group v-model="backupSettings.backup_content">
            <el-checkbox label="database">数据库</el-checkbox>
            <el-checkbox label="files">上传文件</el-checkbox>
            <el-checkbox label="config">配置文件</el-checkbox>
            <el-checkbox label="logs">日志文件</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        
        <el-form-item label="压缩备份">
          <el-switch v-model="backupSettings.compress_backup" />
          <div class="form-tip">启用压缩可以减少备份文件大小</div>
        </el-form-item>
      </el-form>
    </div>
    
    <!-- 备份历史 -->
    <div class="backup-section">
      <h4>备份历史</h4>
      <el-table :data="backupHistory" style="width: 100%">
        <el-table-column prop="filename" label="文件名" />
        <el-table-column prop="size" label="大小" />
        <el-table-column prop="type" label="类型" />
        <el-table-column prop="created_at" label="创建时间" />
        <el-table-column label="操作" width="200">
          <template #default="scope">
            <el-button size="small" @click="downloadBackup(scope.row)">下载</el-button>
            <el-button size="small" type="warning" @click="restoreBackup(scope.row)">恢复</el-button>
            <el-button size="small" type="danger" @click="deleteBackup(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <div class="pagination-container">
        <el-pagination
          :current-page="currentPage"
          :page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :small="isMobile"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { systemAPI } from '../../api/index.js'

export default {
  name: 'BackupSettings',
  setup() {
    const backupSettings = ref({
      enable_auto_backup: false,
      backup_frequency: 'daily',
      backup_time: '02:00',
      keep_backups: 7,
      backup_content: ['database', 'files'],
      compress_backup: true
    })

    const backupHistory = ref([])
    const currentPage = ref(1)
    const pageSize = ref(10)
    const total = ref(0)

    const isMobile = computed(() => {
      return window.innerWidth <= 768
    })

    const loadBackupSettings = async () => {
      try {
        const response = await systemAPI.getBackupSettings()
        const data = response.data || response
        
        // 从统一配置中提取备份设置相关的数据
        const extractedData = {
          enable_auto_backup: data['backup.auto.enable'] === 'true' || data.enable_auto_backup || false,
          backup_frequency: data['backup.frequency'] || data.backup_frequency || 'daily',
          backup_time: data['backup.time'] || data.backup_time || '02:00',
          keep_backups: parseInt(data['backup.keep.count'] || data.keep_backups || 7),
          backup_content: data['backup.content'] ? 
            (typeof data['backup.content'] === 'string' ? 
              data['backup.content'].split(',') : 
              data['backup.content']) : 
            (data.backup_content || ['database', 'files']),
          compress_backup: data['backup.compress'] === 'true' || data.compress_backup || true
        }
        
        Object.assign(backupSettings.value, extractedData)
      } catch (error) {
        console.error('加载备份设置失败:', error)
        ElMessage.error('加载备份设置失败')
      }
    }

    const saveBackupSettings = async () => {
      try {
        const settingsToSave = {
          'backup.auto.enable': String(backupSettings.value.enable_auto_backup),
          'backup.frequency': String(backupSettings.value.backup_frequency),
          'backup.time': String(backupSettings.value.backup_time),
          'backup.keep.count': String(backupSettings.value.keep_backups),
          'backup.content': Array.isArray(backupSettings.value.backup_content) ? 
            backupSettings.value.backup_content.join(',') : 
            String(backupSettings.value.backup_content || ''),
          'backup.compress': String(backupSettings.value.compress_backup)
        }
        
        await systemAPI.saveBackupSettings(settingsToSave)
        ElMessage.success('备份设置保存成功')
      } catch (error) {
        ElMessage.error('保存失败')
        console.error('保存备份设置失败:', error)
      }
    }

    const loadBackupHistory = async () => {
      try {
        const data = await systemAPI.getBackupHistory()
        backupHistory.value = data
      } catch (error) {
        console.error('加载备份历史失败:', error)
      }
    }

    const createBackup = async () => {
      try {
        await systemAPI.createBackup()
        ElMessage.success('备份创建成功')
        loadBackupHistory()
      } catch (error) {
        ElMessage.error('备份创建失败')
        console.error('创建备份失败:', error)
      }
    }

    const downloadBackup = (backup) => {
      window.open(`/api/admin/backup/download/${backup.id}`)
    }

    const restoreBackup = async (backupId) => {
      try {
        await ElMessageBox.confirm('确定要恢复到此备份吗？此操作不可逆！', '警告', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        await systemAPI.restoreBackup(backupId)
        ElMessage.success('备份恢复成功')
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error('备份恢复失败')
          console.error('恢复备份失败:', error)
        }
      }
    }

    const deleteBackup = async (backupId) => {
      try {
        await ElMessageBox.confirm('确定要删除此备份吗？', '警告', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        await systemAPI.deleteBackup(backupId)
        ElMessage.success('备份删除成功')
        loadBackupHistory()
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error('备份删除失败')
          console.error('删除备份失败:', error)
        }
      }
    }

    const handleSizeChange = (val) => {
      pageSize.value = val
      loadBackupHistory()
    }

    const handleCurrentChange = (val) => {
      currentPage.value = val
      loadBackupHistory()
    }

    onMounted(() => {
      loadBackupSettings()
      loadBackupHistory()
    })

    return {
      backupSettings,
      backupHistory,
      currentPage,
      pageSize,
      total,
      isMobile,
      saveBackupSettings,
      createBackup,
      downloadBackup,
      restoreBackup,
      deleteBackup,
      handleSizeChange,
      handleCurrentChange
    }
  }
}
</script>

<style scoped>
.backup-settings {
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

.backup-section {
  margin-bottom: 30px;
}

.backup-section h4 {
  font-size: 16px;
  color: #303133;
  margin-bottom: 15px;
  padding-bottom: 10px;
  border-bottom: 1px solid #f0f0f0;
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

.pagination-container {
  margin-top: 20px;
  text-align: center;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .backup-settings {
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
  
  .backup-section h4 {
    font-size: 15px;
  }
  
  .el-checkbox-group .el-checkbox {
    display: block;
    margin-right: 0;
    margin-bottom: 10px;
    padding: 8px;
    background: #f5f7fa;
    border-radius: 4px;
  }
  
  .el-table .el-button {
    padding: 4px 8px;
    font-size: 12px;
    margin: 2px;
  }
}

@media (max-width: 480px) {
  .backup-settings {
    padding: 5px;
  }
  
  .card-header h3 {
    font-size: 16px;
  }
  
  .backup-section h4 {
    font-size: 14px;
  }
  
  .el-table .el-button {
    padding: 3px 6px;
    font-size: 11px;
  }
}
</style>
