<template>
  <div class="ai-settings">
    <div class="card-header">
          <h3>AI设置</h3>
          <div>
            <el-button type="warning" @click="resetDailyQuota">重置每日配额</el-button>
            <el-button type="danger" @click="resetMonthlyQuota">重置每月配额</el-button>
            <el-button type="primary" @click="saveAiSettings">保存设置</el-button>
          </div>
        </div>
    
    <el-form :model="aiSettings" label-width="120px" style="margin-top: 20px;">
      <el-form-item label="AI API密钥">
        <el-input v-model="aiSettings.apiKey" type="password" placeholder="请输入AI服务的API密钥" show-password />
        <div class="form-tip">AI服务的API密钥，用于身份验证</div>
      </el-form-item>
      
      <el-form-item label="AI服务地址">
        <el-input v-model="aiSettings.apiUrl" placeholder="请输入AI服务的API地址" />
        <div class="form-tip">AI服务的API接口地址</div>
      </el-form-item>
      
      <el-form-item label="AI模型配置">
        <el-input v-model="aiSettings.modelName" placeholder="请输入AI模型名称" />
        <div class="form-tip">当前使用的AI模型名称</div>
      </el-form-item>
      
      <!-- 通用设置 -->
      <el-form-item label="启用AI功能">
        <el-switch v-model="aiSettings.enableAi" />
        <div class="form-tip">是否启用AI相关功能</div>
      </el-form-item>
      
      <el-form-item label="最大Token数">
        <el-input-number v-model="aiSettings.maxTokens" :min="100" :max="8000" />
        <div class="form-tip">AI响应的最大Token数量，影响回答长度</div>
      </el-form-item>
      
      <el-form-item label="温度参数">
        <el-input-number v-model="aiSettings.temperature" :min="0" :max="2" :step="0.1" :precision="1" />
        <div class="form-tip">控制AI回答的随机性，0-2之间，值越高越随机</div>
      </el-form-item>
      
      <el-form-item label="请求超时">
        <el-input-number v-model="aiSettings.timeout" :min="10" :max="120" :step="5" />
        <span style="margin-left: 10px;">秒</span>
        <div class="form-tip">AI请求的超时时间</div>
      </el-form-item>
      
      <el-form-item label="重试次数">
        <el-input-number v-model="aiSettings.retryCount" :min="0" :max="5" />
        <div class="form-tip">AI请求失败时的重试次数</div>
      </el-form-item>
      

      
      <el-form-item label="AI解析前缀">
        <el-input
          v-model="aiSettings.analysisPrefix"
          type="textarea"
          :rows="3"
          placeholder="请输入AI解析题目时的前缀提示语"
        />
        <div class="form-tip">用于AI解析题目时的前缀提示语，影响AI解析的风格和内容</div>
      </el-form-item>
      
      <el-form-item label="AI判题前缀">
        <el-input
          v-model="aiSettings.gradingPrefix"
          type="textarea"
          :rows="4"
          placeholder="请输入AI判题时的前缀提示语，支持{question}、{userAnswer}、{correctAnswer}占位符"
        />
        <div class="form-tip">用于AI判题时的前缀模板，支持占位符：{question}题目内容、{userAnswer}用户答案、{correctAnswer}参考答案</div>
      </el-form-item>
      
      <el-form-item label="AI正确性判断前缀">
        <el-input
          v-model="aiSettings.correctnessPrefix"
          type="textarea"
          :rows="3"
          placeholder="请输入AI正确性判断前缀提示语，支持占位符：{question}、{userAnswer}、{correctAnswer}"
        />
        <div class="form-tip">用于AI正确性判断时的前缀模板，支持占位符：{question}题目内容、{userAnswer}用户答案、{correctAnswer}参考答案</div>
      </el-form-item>
    </el-form>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { systemAPI } from '../../api/index.js'

export default {
  name: 'AiSettings',
  setup() {
    const aiSettings = ref({
      apiKey: '',
      apiUrl: '',
      modelName: 'qwen-turbo',
      enableAi: true,
      temperature: 0.7,
      maxTokens: 2000,
      timeout: 30,
      retryCount: 3,
      analysisPrefix: '请分析以下题目并做出精简的回答：',
      gradingPrefix: '请对以下简答题进行判题评分：\n题目：{question}\n用户答案：{userAnswer}\n参考答案：{correctAnswer}\n请给出评分和详细的评价。',
      correctnessPrefix: '请判断以下简答题答案是否正确：\n题目：{question}\n用户答案：{userAnswer}\n参考答案：{correctAnswer}\n请只回答：正确 或 错误'
    })

    const loadAiSettings = async () => {
      try {
        const response = await systemAPI.getAiSettings()
        const data = response.data || response
        
        // 从统一配置中提取AI设置相关的数据
        const extractedData = {
          provider: 'aliyun',
          // Qwen AI配置
          apiKey: data['ai.qwen.api.key'] || data.aliyun_api_key || '',
          apiUrl: data['ai.qwen.api.url'] || data.apiUrl || 'api.nie1037.cn',
          modelName: data['ai.qwen.model.name'] || data.modelName || 'qwen-turbo',
          // 通用设置
          enableAi: data['ai.enable'] === 'true' || data.enable_ai || true,
          temperature: parseFloat(data['ai.qwen.temperature'] || data.temperature || 0.7),
          maxTokens: parseInt(data['ai.qwen.max.tokens'] || data.maxTokens || 2000),
          timeout: parseInt(data['ai.qwen.timeout'] || data.timeout || 30),
          retryCount: parseInt(data['ai.qwen.retry.count'] || data.retryCount || 3),
          analysisPrefix: data['ai.system.prompt'] || data.system_prompt || '请分析以下题目并做出精简的回答：',
          gradingPrefix: data['ai.grading.prefix'] || '请对以下简答题进行判题评分：\n题目：{question}\n用户答案：{userAnswer}\n参考答案：{correctAnswer}\n请给出评分和详细的评价。',
          correctnessPrefix: data['ai.grading.correctness.prefix'] || '请判断以下简答题答案是否正确：\n题目：{question}\n用户答案：{userAnswer}\n参考答案：{correctAnswer}\n请只回答：正确 或 错误'
        }
        
        Object.assign(aiSettings.value, extractedData)
      } catch (error) {
        console.error('加载AI设置失败:', error)
        ElMessage.error('加载AI设置失败')
      }
    }

    const saveAiSettings = async () => {
      try {
        const settingsToSave = {
          'ai.enable': aiSettings.value.enableAi.toString(),
          'ai.qwen.api.key': aiSettings.value.apiKey,
          'ai.qwen.api.url': aiSettings.value.apiUrl,
          'ai.qwen.model.name': aiSettings.value.modelName,
          'ai.qwen.temperature': aiSettings.value.temperature.toString(),
          'ai.qwen.max.tokens': aiSettings.value.maxTokens.toString(),
          'ai.qwen.timeout': aiSettings.value.timeout.toString(),
          'ai.qwen.retry.count': aiSettings.value.retryCount.toString(),
          'ai.system.prompt': aiSettings.value.analysisPrefix,
          'ai.grading.prefix': aiSettings.value.gradingPrefix,
          'ai.grading.correctness.prefix': aiSettings.value.correctnessPrefix
        }
        await systemAPI.saveAiSettings(settingsToSave)
        ElMessage.success('AI设置保存成功')
      } catch (error) {
        ElMessage.error('保存失败')
        console.error('保存AI设置失败:', error)
      }
    }



    const resetDailyQuota = async () => {
      try {
        await systemAPI.resetDailyQuota()
        // 由于响应拦截器已经处理了格式，直接显示成功消息
        ElMessage.success('每日配额重置成功')
      } catch (error) {
        console.error('重置每日配额失败:', error)
        ElMessage.error(error.message || '每日配额重置失败')
      }
    }

    const resetMonthlyQuota = async () => {
      try {
        await systemAPI.resetMonthlyQuota()
        // 由于响应拦截器已经处理了格式，直接显示成功消息
        ElMessage.success('每月配额重置成功')
      } catch (error) {
        console.error('重置每月配额失败:', error)
        ElMessage.error(error.message || '每月配额重置失败')
      }
    }

    onMounted(() => {
      loadAiSettings()
    })

    return {
      aiSettings,
      saveAiSettings,
      resetDailyQuota,
      resetMonthlyQuota
    }
  }
}
</script>

<style scoped>
.ai-settings {
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

.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 5px;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .ai-settings {
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
  .ai-settings {
    padding: 5px;
  }
  
  .card-header h3 {
    font-size: 16px;
  }
}
</style>
