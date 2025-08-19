<template>
  <div class="question-bank">
    <!-- 页面标题 -->
    <div class="page-header">
      <h2>题库管理</h2>
      <p>管理系统中的所有题目</p>
    </div>
    
    <!-- 操作栏 -->
    <el-card class="operation-card">
      <el-row :gutter="20">
        <el-col :span="16">
          <el-row :gutter="10">
            <el-col :span="6">
              <el-select v-model="searchForm.subjectId" placeholder="选择科目" clearable @change="handleSearch">
                <el-option label="全部科目" value="" />
                <el-option
                  v-for="subject in subjects"
                  :key="subject.id"
                  :label="subject.name"
                  :value="subject.id"
                />
              </el-select>
            </el-col>
            <el-col :span="6">
              <el-select v-model="searchForm.type" placeholder="题目类型" clearable @change="handleSearch">
                <el-option label="全部类型" value="" />
                <el-option label="单选题" :value="0" />
                <el-option label="多选题" :value="1" />
                <el-option label="判断题" :value="2" />
                <el-option label="简答题" :value="3" />
              </el-select>
            </el-col>
            <el-col :span="6">
              <el-select v-model="searchForm.difficulty" placeholder="难度" clearable @change="handleSearch">
                <el-option label="全部难度" value="" />
                <el-option label="简单" value="1" />
                <el-option label="中等" value="2" />
                <el-option label="困难" value="3" />
              </el-select>
            </el-col>
            <el-col :span="6">
              <el-input
                v-model="searchForm.keyword"
                placeholder="搜索题目内容"
                clearable
                @keyup.enter="handleSearch"
              >
                <template #append>
                  <el-button icon="Search" @click="handleSearch" />
                </template>
              </el-input>
            </el-col>
          </el-row>
        </el-col>
        <el-col :span="8" class="operation-buttons">
          <el-button type="primary" icon="Plus" @click="handleAdd">新增题目</el-button>
          <el-button type="success" icon="Upload" @click="handleImport">批量导入</el-button>
          <el-button type="warning" icon="Download" @click="handleExport">导出题目</el-button>
          <el-button
            type="danger"
            icon="Delete"
            :disabled="selectedQuestions.length === 0"
            @click="handleBatchDelete"
          >
            批量删除
          </el-button>
        </el-col>
      </el-row>
    </el-card>
    
    <!-- 题目列表 -->
    <el-card class="table-card">
      <el-table
        v-loading="loading"
        :data="questions"
        @selection-change="handleSelectionChange"
        stripe
        style="width: 100%"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="content" label="题目内容" min-width="300">
          <template #default="{ row }">
            <div class="question-content" v-html="row.content"></div>
          </template>
        </el-table-column>
        <el-table-column prop="subjectName" label="科目" width="120" />
        <el-table-column prop="questionType" label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="getTypeTagType(row.questionType)">{{ getTypeText(row.questionType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="difficulty" label="难度" width="100">
          <template #default="{ row }">
            <span :class="getDifficultyClass(row.difficulty)" class="difficulty-tag">
              {{ getDifficultyText(row.difficulty) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="knowledgePoints" label="知识点" width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.knowledgePoints" class="knowledge-points">
              {{ row.knowledgePoints }}
            </span>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleView(row)">查看</el-button>
            <el-button type="warning" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>
    
    <!-- 题目详情/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="800px"
      :before-close="handleDialogClose"
    >
      <el-form
        ref="questionFormRef"
        :model="questionForm"
        :rules="questionRules"
        label-width="80px"
        :disabled="dialogMode === 'view'"
      >
        <el-form-item label="科目" prop="subjectId">
          <el-select v-model="questionForm.subjectId" placeholder="请选择科目">
            <el-option
              v-for="subject in subjects"
              :key="subject.id"
              :label="subject.name"
              :value="subject.id"
            />
          </el-select>
        </el-form-item>
        
        <el-form-item label="题目类型" prop="type">
          <el-radio-group v-model="questionForm.type">
            <el-radio :label="0">单选题</el-radio>
            <el-radio :label="1">多选题</el-radio>
            <el-radio :label="2">判断题</el-radio>
            <el-radio :label="3">简答题</el-radio>
          </el-radio-group>
        </el-form-item>
        
        <el-form-item label="难度" prop="difficulty">
          <el-radio-group v-model="questionForm.difficulty">
            <el-radio :label="1">简单</el-radio>
            <el-radio :label="2">中等</el-radio>
            <el-radio :label="3">困难</el-radio>
          </el-radio-group>
        </el-form-item>
        
        <el-form-item label="题目内容" prop="content">
          <el-input
            v-model="questionForm.content"
            type="textarea"
            :rows="4"
            placeholder="请输入题目内容"
          />
        </el-form-item>
        
        <el-form-item label="选项" v-if="questionForm.type !== 2 && questionForm.type !== 3">
          <div v-for="(option, index) in questionForm.options" :key="index" class="option-item">
            <el-input
              v-model="option.content"
              :placeholder="`选项 ${String.fromCharCode(65 + index)}`"
              class="option-input"
            />
            <el-radio-group v-if="questionForm.type === 0" v-model="singleCorrectIndex" @change="handleSingleCorrectChange">
              <el-radio :label="index">正确答案</el-radio>
            </el-radio-group>
            <el-checkbox
              v-else-if="questionForm.type === 1"
              v-model="option.isCorrect"
              class="option-checkbox"
            >
              正确答案
            </el-checkbox>
            <el-button
              type="danger"
              size="small"
              icon="Delete"
              @click="removeOption(index)"
              :disabled="questionForm.options.length <= 2"
            />
          </div>
          <el-button type="primary" size="small" icon="Plus" @click="addOption" :disabled="questionForm.options.length >= 6">
            添加选项
          </el-button>
        </el-form-item>
        
        <el-form-item label="正确答案" v-if="questionForm.type === 2" prop="correctAnswer">
          <el-radio-group v-model="questionForm.correctAnswer">
            <el-radio :label="true">正确</el-radio>
            <el-radio :label="false">错误</el-radio>
          </el-radio-group>
        </el-form-item>
        
        <el-form-item label="答案" v-if="questionForm.type === 3" prop="correctAnswer">
          <el-input
            v-model="questionForm.correctAnswer"
            type="textarea"
            :rows="3"
            placeholder="请输入简答题答案"
          />
        </el-form-item>
        
        <el-form-item label="知识点" prop="knowledgePoints">
          <el-input
            v-model="questionForm.knowledgePoints"
            type="textarea"
            :rows="2"
            placeholder="请输入题目涉及的知识点"
          />
        </el-form-item>
        
        <el-form-item label="解析" prop="explanation">
          <el-input
            v-model="questionForm.explanation"
            type="textarea"
            :rows="3"
            placeholder="请输入题目解析"
          />
        </el-form-item>
        
        <el-form-item label="题目图片">
          <el-upload
            ref="imageUploadRef"
            class="question-image-upload"
            :action="uploadImageUrl"
            :headers="uploadHeaders"
            :on-success="handleImageSuccess"
            :on-error="handleImageError"
            :before-upload="beforeImageUpload"
            :on-remove="handleImageRemove"
            :file-list="questionForm.imageList"
            list-type="picture-card"
            :limit="5"
            :multiple="true"
            accept="image/*"
          >
            <el-icon><Plus /></el-icon>
            <template #file="{ file }">
              <div class="upload-file-item">
                <img
                  class="el-upload-list__item-thumbnail"
                  :src="file.url"
                  alt=""
                />
                <span class="el-upload-list__item-actions">
                  <span
                    class="el-upload-list__item-preview"
                    @click="handleImagePreview(file)"
                  >
                    <el-icon><ZoomIn /></el-icon>
                  </span>
                  <span
                    class="el-upload-list__item-delete"
                    @click="handleManualImageRemove(file)"
                  >
                    <el-icon><Delete /></el-icon>
                  </span>
                </span>
              </div>
            </template>
          </el-upload>
          <div class="upload-tip">
            支持JPG、PNG、GIF格式，单张图片不超过5MB，最多上传5张
          </div>
        </el-form-item>
      </el-form>
      
      <template #footer v-if="dialogMode !== 'view'">
        <span class="dialog-footer">
          <el-button @click="handleDialogClose">取消</el-button>
          <el-button type="primary" :loading="submitLoading" @click="handleSubmit">
            {{ dialogMode === 'add' ? '创建' : '更新' }}
          </el-button>
        </span>
      </template>
    </el-dialog>
    
    <!-- 导入对话框 -->
    <el-dialog v-model="importDialogVisible" title="批量导入题目" width="600px">
      <div class="import-content">
        <el-alert
          title="导入说明"
          type="info"
          :closable="false"
          show-icon
        >
          <template #default>
            <p>1. 请下载模板文件，按照模板格式填写题目信息</p>
            <p>2. 支持Excel(.xlsx)和CSV(.csv)格式</p>
            <p>3. 单次最多导入1000道题目</p>
          </template>
        </el-alert>
        
        <div class="import-actions">
          <el-button type="primary" @click="downloadTemplate">下载模板</el-button>
          <el-upload
            ref="uploadRef"
            :auto-upload="false"
            :on-change="handleFileChange"
            :before-upload="beforeUpload"
            accept=".xlsx,.csv"
            :limit="1"
          >
            <el-button type="success">选择文件</el-button>
          </el-upload>
        </div>
        
        <div v-if="uploadFile" class="upload-info">
          <p>已选择文件: {{ uploadFile.name }}</p>
          <el-button type="primary" :loading="importLoading" @click="handleImportSubmit">
            开始导入
          </el-button>
        </div>
      </div>
    </el-dialog>
    
    <!-- 图片预览对话框 -->
    <el-dialog
      v-model="imagePreviewVisible"
      title="图片预览"
      width="800px"
      center
      :close-on-click-modal="true"
      :close-on-press-escape="true"
      @closed="handlePreviewClose"
    >
      <div class="image-preview-container">
        <img 
          v-if="previewImageUrl" 
          :src="previewImageUrl" 
          alt="题目图片" 
          class="preview-image"
          @error="handlePreviewImageError"
        />
        <div v-else class="preview-error">
          <el-icon><Picture /></el-icon>
          <p>图片加载失败</p>
        </div>
      </div>
    </el-dialog>
    
    <!-- 导入对话框 -->
    <el-dialog
      v-model="importDialogVisible"
      title="批量导入题目"
      width="600px"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
    >
      <div class="import-content">
        <div class="import-tips">
          <el-alert
            title="导入说明"
            type="info"
            :closable="false"
            show-icon
          >
            <template #default>
              <div class="tips-content">
                <p>1. 请先下载模板文件，按照模板格式填写题目数据</p>
                <p>2. 支持Excel(.xlsx/.xls)和CSV文件格式</p>
                <p>3. 单次最多导入1000道题目</p>
                <p>4. 科目名称必须是系统中已存在的科目</p>
                <p>5. 文件大小不能超过10MB</p>
              </div>
            </template>
          </el-alert>
        </div>
        
        <div class="import-actions">
          <el-button
            type="primary"
            @click="downloadTemplate"
            :disabled="importLoading"
          >
            <el-icon><Download /></el-icon>
            下载模板
          </el-button>
        </div>
        
        <div class="import-upload">
          <el-upload
            ref="uploadRef"
            class="upload-demo"
            drag
            :auto-upload="false"
            :on-change="handleFileChange"
            :before-upload="() => false"
            :disabled="importLoading"
            accept=".xlsx,.xls,.csv"
            :limit="1"
          >
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">
              将文件拖到此处，或<em>点击选择文件</em>
            </div>
            <template #tip>
              <div class="el-upload__tip">
                支持 .xlsx/.xls/.csv 格式文件，文件大小不超过10MB
              </div>
            </template>
          </el-upload>
        </div>
        
        <!-- 已选择的文件信息 -->
        <div v-if="selectedFile && !importLoading" class="selected-file-info">
          <div class="file-item">
            <el-icon><Document /></el-icon>
            <span class="file-name">{{ selectedFile.name }}</span>
            <span class="file-size">({{ formatFileSize(selectedFile.size) }})</span>
            <el-button
              type="primary"
              link
              @click="removeSelectedFile"
              :disabled="importLoading"
            >
              <el-icon><Close /></el-icon>
            </el-button>
          </div>
        </div>
        
        <!-- 导入进度 -->
        <div v-if="importLoading" class="import-progress">
          <div class="progress-info">
            <span class="progress-text">{{ importStatus }}</span>
            <span class="progress-percent">{{ importProgress }}%</span>
          </div>
          <el-progress
            :percentage="importProgress"
            :stroke-width="8"
            :show-text="false"
            status="success"
          />
        </div>
      </div>
      
      <template #footer>
        <div class="dialog-footer">
          <el-button
            @click="cancelImport"
            :disabled="importLoading"
          >
            取消
          </el-button>
          <el-button
            type="primary"
            @click="startImport"
            :disabled="!selectedFile || importLoading"
            :loading="importLoading"
          >
            {{ importLoading ? '导入中...' : '开始导入' }}
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Download, UploadFilled, Document, Close, Plus, ZoomIn, Delete, Picture } from '@element-plus/icons-vue'
import { subjectAPI, questionAPI } from '../api'
import { useAuthStore } from '../store/auth'
import * as XLSX from 'xlsx'

export default {
  name: 'QuestionBank',
  components: {
    Download,
    UploadFilled,
    Document,
    Close,
    Plus,
    ZoomIn,
    Delete,
    Picture
  },
  setup() {
    const loading = ref(false)
    const submitLoading = ref(false)
    const importLoading = ref(false)
    const dialogVisible = ref(false)
    const importDialogVisible = ref(false)
    const dialogMode = ref('add') // add, edit, view
    const questionFormRef = ref()
    const uploadRef = ref()
    const uploadFile = ref(null)
    const selectedFile = ref(null)
    
    // 导入相关
    const importProgress = ref(0)
    const importStatus = ref('')
    
    // 图片上传相关
    const imageUploadRef = ref()
    const imagePreviewVisible = ref(false)
    const previewImageUrl = ref('')
    const uploadImageUrl = '/api/admin/questions/upload/image'
    const authStore = useAuthStore()
    const uploadHeaders = computed(() => ({
      Authorization: authStore.token ? `Bearer ${authStore.token}` : ''
    }))
    
    const subjects = ref([])
    const questions = ref([])
    const selectedQuestions = ref([])
    
    const searchForm = reactive({
      subjectId: '',
      type: '',
      difficulty: '',
      keyword: ''
    })
    
    const pagination = reactive({
      page: 1,
      size: 20,
      total: 0
    })
    
    const questionForm = reactive({
      id: null,
      subjectId: '',
      type: 0,
      difficulty: 1,
      content: '',
      options: [
        { content: '', isCorrect: false },
        { content: '', isCorrect: false }
      ],
      correctAnswer: true,
      explanation: '',
      imageList: [],
      knowledgePoints: ''
    })
    
    // 单选题正确答案索引
    const singleCorrectIndex = ref(-1)
    
    const questionRules = {
      subjectId: [{ required: true, message: '请选择科目', trigger: 'change' }],
      type: [{ required: true, message: '请选择题目类型', trigger: 'change' }],
      difficulty: [{ required: true, message: '请选择难度', trigger: 'change' }],
      content: [{ required: true, message: '请输入题目内容', trigger: 'blur' }],
      explanation: [{ required: true, message: '请输入题目解析', trigger: 'blur' }],
      knowledgePoints: [{ required: true, message: '请输入知识点', trigger: 'blur' }]
    }
    
    const dialogTitle = computed(() => {
      const titles = {
        add: '新增题目',
        edit: '编辑题目',
        view: '查看题目'
      }
      return titles[dialogMode.value]
    })
    
    // 获取科目列表
    const getSubjects = async () => {
      try {
        subjects.value = await subjectAPI.getEnabledSubjects()
      } catch (error) {
        console.error('获取科目列表失败:', error)
      }
    }
    
    // 获取题目列表
    const getQuestions = async () => {
      try {
        loading.value = true
        const response = await questionAPI.getQuestions({
          subjectId: searchForm.subjectId,
          questionType: searchForm.type,
          difficulty: searchForm.difficulty,
          keyword: searchForm.keyword,
          current: pagination.page,
          size: pagination.size
        })
        questions.value = response.list || response.records || response
        pagination.total = response.total || response.length || 0
      } catch (error) {
        console.error('获取题目列表失败:', error)
        ElMessage.error('获取题目列表失败')
      } finally {
        loading.value = false
      }
    }
    
    // 搜索
    const handleSearch = () => {
      pagination.page = 1
      getQuestions()
    }
    
    // 分页大小改变
    const handleSizeChange = (size) => {
      pagination.size = size
      getQuestions()
    }
    
    // 当前页改变
    const handleCurrentChange = (page) => {
      pagination.page = page
      getQuestions()
    }
    
    // 选择改变
    const handleSelectionChange = (selection) => {
      selectedQuestions.value = selection
    }
    
    // 新增题目
    const handleAdd = () => {
      dialogMode.value = 'add'
      resetQuestionForm()
      dialogVisible.value = true
    }
    
    // 查看题目
    const handleView = (row) => {
      dialogMode.value = 'view'
      loadQuestionForm(row)
      dialogVisible.value = true
    }
    
    // 编辑题目
    const handleEdit = (row) => {
      dialogMode.value = 'edit'
      loadQuestionForm(row)
      dialogVisible.value = true
    }
    
    // 删除题目
    const handleDelete = async (row) => {
      try {
        await ElMessageBox.confirm(`确定要删除题目 "${row.content}" 吗？`, '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        
        await questionAPI.deleteQuestion(row.id)
        ElMessage.success('删除成功')
        getQuestions()
      } catch (error) {
        if (error !== 'cancel') {
          console.error('删除题目失败:', error)
          ElMessage.error('删除题目失败')
        }
      }
    }
    
    // 批量删除
    const handleBatchDelete = async () => {
      try {
        await ElMessageBox.confirm(`确定要删除选中的 ${selectedQuestions.value.length} 道题目吗？`, '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        
        const ids = selectedQuestions.value.map(item => item.id)
        await questionAPI.batchDeleteQuestions(ids)
        ElMessage.success('批量删除成功')
        getQuestions()
      } catch (error) {
        if (error !== 'cancel') {
          console.error('批量删除失败:', error)
          ElMessage.error('批量删除失败')
        }
      }
    }
    
    // 导入题目
    const handleImport = () => {
      importDialogVisible.value = true
      uploadFile.value = null
    }
    
    // 处理文件选择
    const handleFileChange = (file) => {
      const fileType = file.name.split('.').pop().toLowerCase()
      if (!['xlsx', 'xls', 'csv'].includes(fileType)) {
        ElMessage.error('只支持Excel(.xlsx/.xls)和CSV文件格式')
        uploadRef.value?.clearFiles()
        return false
      }
      
      if (file.size > 10 * 1024 * 1024) {
        ElMessage.error('文件大小不能超过10MB')
        uploadRef.value?.clearFiles()
        return false
      }
      
      selectedFile.value = file
      ElMessage.success('文件选择成功，请点击开始导入')
    }
    
    // 开始导入
    const startImport = () => {
      if (!selectedFile.value) {
        ElMessage.error('请先选择文件')
        return
      }
      // Element Plus上传组件的文件对象需要访问raw属性获取原始File对象
      const rawFile = selectedFile.value.raw || selectedFile.value
      parseExcelFile(rawFile)
    }
    
    // 移除选择的文件
    const removeSelectedFile = () => {
      selectedFile.value = null
      uploadRef.value?.clearFiles()
    }
    
    // 格式化文件大小
    const formatFileSize = (size) => {
      if (size < 1024) {
        return size + 'B'
      } else if (size < 1024 * 1024) {
        return (size / 1024).toFixed(1) + 'KB'
      } else {
        return (size / (1024 * 1024)).toFixed(1) + 'MB'
      }
    }
    
    // 解析Excel文件
    const parseExcelFile = (file) => {
      importLoading.value = true
      importStatus.value = '正在解析文件...'
      importProgress.value = 10
      
      const reader = new FileReader()
      reader.onload = (e) => {
        try {
          const data = new Uint8Array(e.target.result)
          const workbook = XLSX.read(data, { type: 'array' })
          const sheetName = workbook.SheetNames[0]
          const worksheet = workbook.Sheets[sheetName]
          const jsonData = XLSX.utils.sheet_to_json(worksheet)
          
          importProgress.value = 30
          importStatus.value = '正在验证数据...'
          
          // 过滤掉说明行
          const filteredData = jsonData.filter(row => 
            row['科目名称'] && 
            !String(row['科目名称'] || '').includes('===') &&
            row['题目标题'] && 
            row['题目内容']
          )
          
          if (filteredData.length === 0) {
            throw new Error('未找到有效的题目数据')
          }
          
          if (filteredData.length > 1000) {
            throw new Error('单次最多导入1000道题目')
          }
          
          importProgress.value = 50
          importStatus.value = '正在转换数据格式...'
          
          // 转换数据格式
          convertAndImportData(filteredData)
          
        } catch (error) {
          console.error('解析文件失败:', error)
          ElMessage.error('解析文件失败: ' + error.message)
          resetImportState()
        }
      }
      
      reader.onerror = () => {
        ElMessage.error('读取文件失败')
        resetImportState()
      }
      
      reader.readAsArrayBuffer(file)
    }
    
    // 转换并导入数据
    const convertAndImportData = async (data) => {
      try {
        // 获取科目映射
        const subjectMap = new Map()
        subjects.value.forEach(subject => {
          subjectMap.set(subject.name, subject.id)
        })
        
        // 题目类型映射
        const typeMap = {
          '单选题': 0,
          '多选题': 1,
          '判断题': 2,
          '简答题': 3,
          '填空题': 4
        }
        
        // 难度映射
        const difficultyMap = {
          '简单': 1,
          '中等': 2,
          '困难': 3
        }
        
        const questions = []
        const errors = []
        
        importProgress.value = 60
        importStatus.value = '正在验证题目数据...'
        
        for (let i = 0; i < data.length; i++) {
          const row = data[i]
          const rowNum = i + 1
          
          try {
            // 验证必填字段
            if (!row['科目名称']) {
              errors.push(`第${rowNum}行：科目名称不能为空`)
              continue
            }
            
            if (!subjectMap.has(row['科目名称'])) {
              errors.push(`第${rowNum}行：科目"${row['科目名称']}"不存在`)
              continue
            }
            
            if (!row['题目类型'] || !Object.prototype.hasOwnProperty.call(typeMap, row['题目类型'])) {
               errors.push(`第${rowNum}行：题目类型"${row['题目类型']}"无效`)
               continue
             }
             
             if (!row['难度'] || !Object.prototype.hasOwnProperty.call(difficultyMap, row['难度'])) {
               errors.push(`第${rowNum}行：难度"${row['难度']}"无效`)
               continue
             }
            
            if (!row['题目标题']) {
              errors.push(`第${rowNum}行：题目标题不能为空`)
              continue
            }
            
            if (!row['题目内容']) {
              errors.push(`第${rowNum}行：题目内容不能为空`)
              continue
            }
            
            if (!row['正确答案']) {
              errors.push(`第${rowNum}行：正确答案不能为空`)
              continue
            }
            
            const questionType = typeMap[row['题目类型']]
            const subjectId = subjectMap.get(row['科目名称'])
            
            // 构建选项
            const options = []
            if (questionType === 0 || questionType === 1) { // 单选题或多选题
              const optionKeys = ['A', 'B', 'C', 'D', 'E', 'F']
              const correctAnswers = row['正确答案'].trim().split(',').map(ans => ans.trim())
              
              for (const key of optionKeys) {
                const optionValue = row[`选项${key}`]
                // 确保optionValue是字符串类型
                const stringValue = String(optionValue || '').trim()
                if (stringValue) {
                  options.push({
                    key: key,
                    value: stringValue,
                    isCorrect: correctAnswers.includes(key)
                  })
                }
              }
              
              if (options.length < 2) {
                errors.push(`第${rowNum}行：选择题至少需要2个选项`)
                continue
              }
              
              // 验证正确答案是否有效
              const validCorrectAnswers = correctAnswers.filter(ans => 
                options.some(opt => opt.key === ans)
              )
              if (validCorrectAnswers.length === 0) {
                errors.push(`第${rowNum}行：正确答案"${row['正确答案']}"与选项不匹配`)
                continue
              }
            }
            
            // 构建题目对象
            const question = {
              subjectId: subjectId,
              questionType: questionType,
              title: String(row['题目标题'] || '').trim(),
              content: String(row['题目内容'] || '').trim(),
              options: options,
              correctAnswer: String(row['正确答案'] || '').trim(),
              analysis: row['题目解析'] ? String(row['题目解析'] || '').trim() : '',
              difficulty: difficultyMap[row['难度']],
              knowledgePoints: row['知识点'] ? String(row['知识点'] || '').trim() : ''
            }
            
            questions.push(question)
            
          } catch (error) {
            errors.push(`第${rowNum}行：数据处理错误 - ${error.message}`)
          }
        }
        
        if (errors.length > 0) {
          console.error('数据验证错误:', errors)
          ElMessage.error(`发现${errors.length}个错误，请检查数据格式`)
          // 显示前5个错误
          const errorMsg = errors.slice(0, 5).join('\n')
          ElMessageBox.alert(errorMsg, '数据验证失败', {
            type: 'error',
            customClass: 'import-error-dialog'
          })
          resetImportState()
          return
        }
        
        if (questions.length === 0) {
          ElMessage.error('没有有效的题目数据')
          resetImportState()
          return
        }
        
        importProgress.value = 80
        importStatus.value = `正在导入${questions.length}道题目...`
        
        // 批量导入题目
        await batchImportQuestions(questions)
        
      } catch (error) {
        console.error('转换数据失败:', error)
        ElMessage.error('转换数据失败: ' + error.message)
        resetImportState()
      }
    }
    
    // 批量导入题目
    const batchImportQuestions = async (questions) => {
      try {
        const batchSize = 50 // 每批处理50道题目
        const batches = []
        
        for (let i = 0; i < questions.length; i += batchSize) {
          batches.push(questions.slice(i, i + batchSize))
        }
        
        let successCount = 0
        let failCount = 0
        
        for (let i = 0; i < batches.length; i++) {
          const batch = batches[i]
          const progress = 80 + (i / batches.length) * 15
          importProgress.value = Math.round(progress)
          importStatus.value = `正在导入第${i + 1}批题目 (${successCount + failCount + 1}-${successCount + failCount + batch.length})...`
          
          try {
            // 调用批量导入API
            await questionAPI.importQuestions(batch)
            // API成功调用说明导入成功（响应拦截器已处理错误情况）
            successCount += batch.length
          } catch (error) {
            failCount += batch.length
            console.error('批量导入异常:', error)
          }
          
          // 添加延迟，避免请求过于频繁
          if (i < batches.length - 1) {
            await new Promise(resolve => setTimeout(resolve, 200))
          }
        }
        
        importProgress.value = 100
        importStatus.value = '导入完成'
        
        if (successCount > 0) {
          ElMessage.success(`成功导入${successCount}道题目${failCount > 0 ? `，失败${failCount}道` : ''}`)
          // 刷新题目列表
          await getQuestions()
        } else {
          ElMessage.error('导入失败，请检查数据格式或网络连接')
        }
        
        setTimeout(() => {
          resetImportState()
          importDialogVisible.value = false
        }, 2000)
        
      } catch (error) {
        console.error('批量导入失败:', error)
        ElMessage.error('批量导入失败: ' + error.message)
        resetImportState()
      }
    }
    
    // 重置导入状态
    const resetImportState = () => {
      importLoading.value = false
      importProgress.value = 0
      importStatus.value = ''
      selectedFile.value = null
      if (uploadRef.value) {
        uploadRef.value.clearFiles()
      }
    }
    
    // 取消导入
    const cancelImport = () => {
      resetImportState()
      importDialogVisible.value = false
    }
    
    // 导出题目
    const handleExport = async () => {
      try {
        await questionAPI.exportQuestions({
          subjectId: searchForm.subjectId,
          questionType: searchForm.type,
          difficulty: searchForm.difficulty
        })
        ElMessage.success('导出成功')
      } catch (error) {
        console.error('导出失败:', error)
        ElMessage.error('导出失败')
      }
    }
    
    // 重置表单
    const resetQuestionForm = () => {
      Object.assign(questionForm, {
        id: null,
        subjectId: '',
        type: 0,
        difficulty: 1,
        content: '',
        options: [
          { content: '', isCorrect: false },
          { content: '', isCorrect: false }
        ],
        correctAnswer: true,
        explanation: '',
        imageList: [],
        knowledgePoints: ''
      })
      singleCorrectIndex.value = -1
    }
    
    // 加载表单数据
    const loadQuestionForm = async (row) => {
      try {
        const questionDetail = await questionAPI.getQuestion(row.id)
        const options = questionDetail.optionList || questionDetail.options || [
          { content: '', isCorrect: false },
          { content: '', isCorrect: false }
        ]
        
        // 处理图片数据：将字符串URL数组转换为文件对象数组
        const imageList = questionDetail.imageList || []
        const processedImageList = imageList.map((url, index) => {
          if (typeof url === 'string') {
            return {
              name: `图片_${index + 1}`,
              url: url.trim(), // 移除可能的空格和引号
              uid: `img_${Date.now()}_${index}_${Math.random().toString(36).substr(2, 9)}` // 确保唯一性
            }
          } else if (url && typeof url === 'object') {
            return {
              name: url.name || `图片_${index + 1}`,
              url: (url.url || url).toString().trim(),
              uid: url.uid || `img_${Date.now()}_${index}_${Math.random().toString(36).substr(2, 9)}`
            }
          }
          return null
        }).filter(item => item && item.url) // 过滤掉无效项

        Object.assign(questionForm, {
          id: questionDetail.id,
          subjectId: questionDetail.subjectId,
          type: questionDetail.questionType,
          difficulty: questionDetail.difficulty,
          content: questionDetail.content,
          options: options.map(opt => ({
            content: opt.value || opt.content || '',
            isCorrect: opt.isCorrect || false
          })),
          correctAnswer: questionDetail.questionType === 2 
            ? (questionDetail.correctAnswer === 'true' || questionDetail.correctAnswer === true)
            : (questionDetail.questionType === 3 ? (questionDetail.correctAnswer || '') : true),
          explanation: questionDetail.analysis || questionDetail.explanation || '',
          imageList: processedImageList,
          knowledgePoints: questionDetail.knowledgePoints || ''
        })
        
        // 设置单选题的正确答案索引
        if (questionDetail.questionType === 0) {
          const correctIndex = questionForm.options.findIndex(opt => opt.isCorrect)
          singleCorrectIndex.value = correctIndex >= 0 ? correctIndex : -1
        } else {
          singleCorrectIndex.value = -1
        }
        
      } catch (error) {
        console.error('获取题目详情失败:', error)
        ElMessage.error('获取题目详情失败')
        // 使用基本信息作为后备
        const imageList = row.imageList || []
        const processedImageList = imageList.map((url, index) => {
          if (typeof url === 'string') {
            return {
              name: `图片_${index + 1}`,
              url: url.trim(),
              uid: `img_${Date.now()}_${index}_${Math.random().toString(36).substr(2, 9)}`
            }
          } else if (url && typeof url === 'object') {
            return {
              name: url.name || `图片_${index + 1}`,
              url: (url.url || url).toString().trim(),
              uid: url.uid || `img_${Date.now()}_${index}_${Math.random().toString(36).substr(2, 9)}`
            }
          }
          return null
        }).filter(item => item && item.url)

        Object.assign(questionForm, {
          id: row.id,
          subjectId: row.subjectId || '',
          type: row.questionType,
          difficulty: row.difficulty,
          content: row.content,
          options: [
            { content: '', isCorrect: false },
            { content: '', isCorrect: false }
          ],
          correctAnswer: row.questionType === 3 ? '' : true,
          explanation: '',
          imageList: processedImageList,
          knowledgePoints: row.knowledgePoints || ''
        })
        singleCorrectIndex.value = -1
      }
    }
    
    // 关闭对话框
    const handleDialogClose = () => {
      dialogVisible.value = false
      questionFormRef.value?.resetFields()
    }
    
    // 提交表单
    const handleSubmit = async () => {
      if (!questionFormRef.value) return
      
      try {
        await questionFormRef.value.validate()
        
        // 验证选择题答案
        if (questionForm.type === 0 || questionForm.type === 1) {
          const correctCount = getCorrectOptionsCount()
          if (correctCount === 0) {
            ElMessage.error('请至少选择一个正确答案')
            return
          }
          if (questionForm.type === 0 && correctCount > 1) {
            ElMessage.error('单选题只能有一个正确答案')
            return
          }
        }
        
        // 验证简答题答案
        if (questionForm.type === 3 && !questionForm.correctAnswer?.trim()) {
          ElMessage.error('请输入简答题答案')
          return
        }
        
        submitLoading.value = true
        
        // 构造后端期望的数据格式
        const requestData = {
          subjectId: questionForm.subjectId,
          questionType: questionForm.type,
          title: questionForm.content, // 使用content作为title
          content: questionForm.content,
          options: questionForm.options.map((option, index) => ({
            key: String.fromCharCode(65 + index), // A, B, C, D
            value: option.content,
            isCorrect: option.isCorrect
          })),
          correctAnswer: questionForm.type === 2 
             ? (questionForm.correctAnswer ? 'true' : 'false')
             : (questionForm.type === 3 ? questionForm.correctAnswer : questionForm.options
                 .filter(option => option.isCorrect)
                 .map((option) => String.fromCharCode(65 + questionForm.options.findIndex(opt => opt === option)))
                 .join(',')),
          analysis: questionForm.explanation,
          difficulty: questionForm.difficulty,
          images: questionForm.imageList.map(img => img.url || img),
          knowledgePoints: questionForm.knowledgePoints
        }
        
        if (dialogMode.value === 'add') {
          await questionAPI.createQuestion(requestData)
          ElMessage.success('创建成功')
        } else {
          await questionAPI.updateQuestion(questionForm.id, requestData)
          ElMessage.success('更新成功')
        }
        
        handleDialogClose()
        getQuestions()
      } catch (error) {
        if (error.message) {
          ElMessage.error(error.message)
        }
      } finally {
        submitLoading.value = false
      }
    }
    
    // 添加选项
    const addOption = () => {
      questionForm.options.push({ content: '', isCorrect: false })
    }
    
    // 删除选项
    const removeOption = (index) => {
      questionForm.options.splice(index, 1)
    }
    
    // 获取正确答案数量
    const getCorrectOptionsCount = () => {
      return questionForm.options.filter(option => option.isCorrect).length
    }
    
    // 处理单选题正确答案变化
    const handleSingleCorrectChange = (index) => {
      // 清除所有选项的正确答案标记
      questionForm.options.forEach(option => {
        option.isCorrect = false
      })
      // 设置选中的选项为正确答案
      if (index >= 0 && index < questionForm.options.length) {
        questionForm.options[index].isCorrect = true
      }
    }
    
    // 上传前验证
    const beforeUpload = (file) => {
      const isExcel = file.type === 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
      const isCsv = file.type === 'text/csv'
      
      if (!isExcel && !isCsv) {
        ElMessage.error('只支持Excel和CSV格式文件')
        return false
      }
      
      const isLt10M = file.size / 1024 / 1024 < 10
      if (!isLt10M) {
        ElMessage.error('文件大小不能超过10MB')
        return false
      }
      
      return true
    }
    
    // 下载模板
    const downloadTemplate = () => {
      try {
        // 创建模板数据
        const templateData = [
          {
            '科目名称': '计算机基础',
            '题目类型': '单选题',
            '难度': '简单',
            '题目标题': '计算机的核心部件是什么？',
            '题目内容': '计算机的核心部件是什么？',
            '选项A': 'CPU',
            '选项B': '内存',
            '选项C': '硬盘',
            '选项D': '显卡',
            '选项E': '',
            '选项F': '',
            '正确答案': 'A',
            '题目解析': 'CPU是计算机的中央处理器，是计算机的核心部件。',
            '知识点': 'CPU,计算机组成'
          },
          {
            '科目名称': '计算机基础',
            '题目类型': '多选题',
            '难度': '中等',
            '题目标题': '以下哪些是计算机的输入设备？',
            '题目内容': '以下哪些是计算机的输入设备？',
            '选项A': '键盘',
            '选项B': '鼠标',
            '选项C': '显示器',
            '选项D': '打印机',
            '选项E': '扫描仪',
            '选项F': '',
            '正确答案': 'A,B,E',
            '题目解析': '键盘、鼠标、扫描仪都是输入设备，显示器和打印机是输出设备。',
            '知识点': '输入设备,输出设备'
          },
          {
            '科目名称': '计算机基础',
            '题目类型': '判断题',
            '难度': '简单',
            '题目标题': 'CPU的主频越高，计算机的运行速度就越快。',
            '题目内容': 'CPU的主频越高，计算机的运行速度就越快。',
            '选项A': '',
            '选项B': '',
            '选项C': '',
            '选项D': '',
            '选项E': '',
            '选项F': '',
            '正确答案': 'true',
            '题目解析': '一般情况下，CPU主频越高，处理速度越快，但还受到其他因素影响。',
            '知识点': 'CPU,主频'
          },
          {
            '科目名称': '计算机基础',
            '题目类型': '简答题',
            '难度': '困难',
            '题目标题': '请简述计算机的工作原理。',
            '题目内容': '请简述计算机的工作原理。',
            '选项A': '',
            '选项B': '',
            '选项C': '',
            '选项D': '',
            '选项E': '',
            '选项F': '',
            '正确答案': '计算机通过输入设备接收数据，CPU进行处理，将结果通过输出设备显示。',
            '题目解析': '计算机工作原理包括：输入→处理→输出→存储四个基本环节。',
            '知识点': '计算机原理,工作流程'
          }
        ]
        
        // 添加说明行
        const instructions = [
          {
            '科目名称': '=== 导入说明 ===',
            '题目类型': '单选题/多选题/判断题/简答题',
            '难度': '简单/中等/困难',
            '题目标题': '题目的简短标题',
            '题目内容': '完整的题目描述',
            '选项A': '第一个选项内容',
            '选项B': '第二个选项内容',
            '选项C': '第三个选项内容（可选）',
            '选项D': '第四个选项内容（可选）',
            '选项E': '第五个选项内容（可选）',
            '选项F': '第六个选项内容（可选）',
            '正确答案': '单选：A 多选：A,B 判断：true/false 简答：参考答案',
            '题目解析': '题目的详细解析说明',
            '知识点': '多个知识点用逗号分隔'
          },
          {
            '科目名称': '=== 注意事项 ===',
            '题目类型': '1. 科目名称必须是系统中已存在的科目',
            '难度': '2. 选项A和B是必填的，其他选项可选',
            '题目标题': '3. 判断题和简答题不需要填写选项',
            '题目内容': '4. 单次最多导入1000道题目',
            '选项A': '5. 请删除说明行后再导入',
            '选项B': '',
            '选项C': '',
            '选项D': '',
            '选项E': '',
            '选项F': '',
            '正确答案': '',
            '题目解析': '',
            '知识点': ''
          },
          ...templateData
        ]
        
        // 创建工作簿
        const ws = XLSX.utils.json_to_sheet(instructions)
        const wb = XLSX.utils.book_new()
        XLSX.utils.book_append_sheet(wb, ws, '题目导入模板')
        
        // 设置列宽
        const colWidths = [
          { wch: 12 }, // 科目名称
          { wch: 10 }, // 题目类型
          { wch: 8 },  // 难度
          { wch: 30 }, // 题目标题
          { wch: 40 }, // 题目内容
          { wch: 20 }, // 选项A
          { wch: 20 }, // 选项B
          { wch: 20 }, // 选项C
          { wch: 20 }, // 选项D
          { wch: 20 }, // 选项E
          { wch: 20 }, // 选项F
          { wch: 15 }, // 正确答案
          { wch: 30 }, // 题目解析
          { wch: 20 }  // 知识点
        ]
        ws['!cols'] = colWidths
        
        // 下载文件
        XLSX.writeFile(wb, '题目导入模板.xlsx')
        ElMessage.success('模板下载成功')
      } catch (error) {
        console.error('下载模板失败:', error)
        ElMessage.error('下载模板失败')
      }
    }
    
    // 导入提交
    const handleImportSubmit = async () => {
      if (!uploadFile.value) {
        ElMessage.error('请选择要导入的文件')
        return
      }
      
      try {
        importLoading.value = true
        
        const formData = new FormData()
        formData.append('file', uploadFile.value.raw)
        
        await questionAPI.importQuestions(formData)
        ElMessage.success('导入成功')
        importDialogVisible.value = false
        getQuestions()
      } catch (error) {
        console.error('导入失败:', error)
      } finally {
        importLoading.value = false
      }
    }
    
    // 获取类型标签类型
    const getTypeTagType = (type) => {
      const types = {
        0: 'primary',
        1: 'success',
        2: 'warning',
        3: 'info'
      }
      return types[type] || 'info'
    }
    
    // 获取类型文本
    const getTypeText = (type) => {
      const types = {
        0: '单选题',
        1: '多选题',
        2: '判断题',
        3: '简答题'
      }
      return types[type] || '未知'
    }
    
    // 获取难度标签类型
    const getDifficultyTagType = (difficulty) => {
      const types = {
        easy: 'success',
        medium: 'warning',
        hard: 'danger'
      }
      return types[difficulty] || 'info'
    }
    
    // 获取难度文本
    const getDifficultyText = (difficulty) => {
      const types = {
        1: '简单',
        2: '中等',
        3: '困难'
      }
      return types[difficulty] || '未知'
    }

    // 获取难度样式类
    const getDifficultyClass = (difficulty) => {
      const classes = {
        1: 'difficulty-easy',
        2: 'difficulty-medium',
        3: 'difficulty-hard'
      }
      return classes[difficulty] || 'difficulty-unknown'
    }
    
    // 图片上传相关方法
    const handleImageSuccess = (response, file, fileList) => {
      console.log('图片上传响应:', response);
      console.log('文件列表:', fileList);
      
      if (response.code === 200) {
        // 确保获取正确的图片URL
        let imageUrl = '';
        
        // 处理不同的返回数据结构
        if (typeof response.data === 'string') {
          imageUrl = response.data;
        } else if (response.data && typeof response.data === 'object' && response.data.url) {
          imageUrl = response.data.url;
        } else {
          console.error('图片URL格式错误:', response.data);
          ElMessage.error('图片上传成功，但返回的URL格式错误');
          return;
        }
        
        if (imageUrl && typeof imageUrl === 'string') {
          // 将新上传的图片添加到imageList
          const newImage = {
            name: file.name,
            url: imageUrl,
            uid: file.uid || `upload_${Date.now()}_${Math.random().toString(36).substr(2, 9)}` // 确保唯一性
          };
          
          // 更新表单中的图片列表
          questionForm.imageList = [...questionForm.imageList, newImage];
          
          // 同时更新fileList以确保el-upload组件正确显示
          file.url = imageUrl;
          file.status = 'success';
          
          ElMessage.success('图片上传成功');
        } else {
          console.error('图片URL格式错误:', imageUrl);
          ElMessage.error('图片上传成功，但返回的URL格式错误');
        }
      } else {
        ElMessage.error(response.message || '图片上传失败');
      }
    }
    
    const handleImageError = (error) => {
      ElMessage.error('图片上传失败')
      console.error('图片上传错误:', error)
    }
    
    const beforeImageUpload = (file) => {
      const isImage = file.type.startsWith('image/')
      const isLt5M = file.size / 1024 / 1024 < 5
      
      if (!isImage) {
        ElMessage.error('只能上传图片文件')
        return false
      }
      if (!isLt5M) {
        ElMessage.error('图片大小不能超过5MB')
        return false
      }
      return true
    }
    
    const handleImageRemove = (file, fileList) => {
      try {
        console.log('el-upload on-remove事件:', {
          要删除的文件: file,
          提供的fileList: JSON.parse(JSON.stringify(fileList))
        })
        
        // 直接使用el-upload提供的fileList
        if (!fileList) {
          console.warn('handleImageRemove: fileList为空')
          return
        }
        
        // 确保返回的是有效的文件对象数组
        questionForm.imageList = fileList.map(f => ({
          name: f?.name || '未命名图片',
          url: f?.url || '',
          uid: f?.uid || `remove_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`
        })).filter(f => f && f.url)
        
        console.log('handleImageRemove: 图片已移除，最终列表:', questionForm.imageList)
        
      } catch (error) {
        console.error('handleImageRemove: 移除图片时发生错误', error)
        ElMessage.error('移除图片失败')
      }
    }
    
    const handleManualImageRemove = (file) => {
      try {
        console.log('手动删除图片:', {
          要删除的文件: file,
          当前图片列表: JSON.parse(JSON.stringify(questionForm.imageList))
        })
        
        if (!file || !questionForm.imageList) {
          console.warn('handleManualImageRemove: file或imageList为空')
          return
        }
        
        // 找到要删除的文件索引，优先使用uid匹配
        const removeIndex = questionForm.imageList.findIndex(item => {
          if (!item) return false
          
          // 优先使用uid匹配
          if (file.uid && item.uid) {
            return item.uid === file.uid
          }
          
          // 回退到url匹配
          if (file.url && item.url) {
            return item.url === file.url
          }
          
          return false
        })
        
        console.log('找到的删除索引:', removeIndex)
        
        if (removeIndex === -1) {
          console.warn('handleManualImageRemove: 未找到要删除的文件')
          return
        }
        
        // 创建新的数组，排除要删除的文件
        questionForm.imageList = questionForm.imageList.filter((_, index) => index !== removeIndex)
        console.log('handleManualImageRemove: 图片已移除，最终列表:', questionForm.imageList)
        
      } catch (error) {
        console.error('handleManualImageRemove: 移除图片时发生错误', error)
        ElMessage.error('移除图片失败')
      }
    }
    
    const handleImagePreview = (file) => {
      if (file && file.url) {
        previewImageUrl.value = file.url
        imagePreviewVisible.value = true
      } else {
        console.error('预览图片失败：文件或URL无效', file)
        ElMessage.error('无法预览图片')
      }
    }
    
    const handlePreviewClose = () => {
      // 对话框关闭时清理预览URL，避免内存泄漏
      previewImageUrl.value = ''
    }
    
    const handlePreviewImageError = () => {
      ElMessage.error('图片加载失败，请检查图片链接')
      console.error('图片加载失败：', previewImageUrl.value)
    }
    
    onMounted(() => {
      getSubjects()
      getQuestions()
    })
    
    return {
      loading,
      submitLoading,
      importLoading,
      dialogVisible,
      importDialogVisible,
      dialogMode,
      dialogTitle,
      questionFormRef,
      uploadRef,
      uploadFile,
      selectedFile,
      importProgress,
      importStatus,
      imageUploadRef,
      imagePreviewVisible,
      previewImageUrl,
      uploadImageUrl,
      uploadHeaders,
      subjects,
      questions,
      selectedQuestions,
      searchForm,
      pagination,
      questionForm,
      questionRules,
      singleCorrectIndex,
      handleSearch,
      handleSizeChange,
      handleCurrentChange,
      handleSelectionChange,
      handleAdd,
      handleView,
      handleEdit,
      handleDelete,
      handleBatchDelete,
      handleImport,
      handleExport,
      handleDialogClose,
      handleSubmit,
      addOption,
      removeOption,
      getCorrectOptionsCount,
      handleSingleCorrectChange,
      handleFileChange,
      beforeUpload,
      downloadTemplate,
      handleImportSubmit,
      startImport,
      removeSelectedFile,
      formatFileSize,
      cancelImport,
      getTypeTagType,
      getTypeText,
      getDifficultyTagType,
      getDifficultyText,
      getDifficultyClass,
      handleImageSuccess,
      handleImageError,
      beforeImageUpload,
      handleImageRemove,
      handleManualImageRemove,
      handleImagePreview,
      handlePreviewClose,
      handlePreviewImageError
    }
  }
}
</script>

<style scoped>
.question-bank {
  padding: 0;
}

.page-header {
  margin-bottom: 20px;
}

.page-header h2 {
  color: #333;
  margin-bottom: 8px;
}

.page-header p {
  color: #666;
  margin: 0;
}

.operation-card {
  margin-bottom: 20px;
}

.operation-buttons {
  text-align: right;
}

.table-card {
  min-height: 400px;
}

.question-content {
  max-width: 300px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pagination-container {
  margin-top: 20px;
  text-align: right;
}

.option-item {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
  gap: 10px;
}

.option-input {
  flex: 1;
}

.option-checkbox {
  width: 80px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.import-content {
  padding: 20px 0;
}

.import-actions {
  margin: 20px 0;
  display: flex;
  gap: 10px;
}

.upload-info {
  margin-top: 20px;
  padding: 15px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.upload-info p {
  margin-bottom: 10px;
  color: #333;
}

/* 导入对话框样式 */
.import-tips {
  margin-bottom: 20px;
}

.tips-content p {
  margin: 5px 0;
  font-size: 14px;
  line-height: 1.5;
}

/* 图片上传样式 */
.question-image-upload {
  width: 100%;
}

.upload-tip {
  font-size: 12px;
  color: #666;
  margin-top: 5px;
}

.image-preview-container {
  text-align: center;
  padding: 20px;
}

.preview-image {
  max-width: 100%;
  max-height: 600px;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.preview-error {
  text-align: center;
  padding: 40px 20px;
  color: #909399;
}

.preview-error .el-icon {
  font-size: 48px;
  margin-bottom: 10px;
}

.preview-error p {
  margin: 0;
  font-size: 16px;
}

.upload-file-item {
  position: relative;
  width: 100%;
  height: 100%;
}

.el-upload-list__item-actions {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.5);
  opacity: 0;
  transition: opacity 0.3s;
}

.el-upload-list__item:hover .el-upload-list__item-actions {
  opacity: 1;
}

.el-upload-list__item-preview,
.el-upload-list__item-delete {
  cursor: pointer;
  color: #fff;
  margin: 0 8px;
  font-size: 16px;
}

.el-upload-list__item-preview:hover,
.el-upload-list__item-delete:hover {
  color: #409eff;
}

.import-upload {
  margin-bottom: 20px;
}

.upload-demo {
  width: 100%;
}

.selected-file-info {
  margin: 15px 0;
  padding: 12px;
  background-color: #f8f9fa;
  border: 1px solid #e9ecef;
  border-radius: 6px;
}

.file-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.file-name {
  flex: 1;
  font-weight: 500;
  color: #333;
}

/* 知识点相关样式 */
.knowledge-points {
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.knowledge-points-content {
  font-size: 14px;
  line-height: 1.4;
}

.knowledge-points-empty {
  color: #909399;
  font-style: italic;
}

.file-size {
  color: #666;
  font-size: 12px;
}

.import-progress {
  margin-top: 20px;
  padding: 15px;
  background-color: #f8f9fa;
  border-radius: 6px;
  border: 1px solid #e9ecef;
}

.progress-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.progress-text {
  font-size: 14px;
  color: #333;
}

.progress-status {
  font-size: 12px;
  color: #666;
}

/* 上传组件样式调整 */
:deep(.el-upload-dragger) {
  padding: 40px 20px;
}

:deep(.el-icon--upload) {
  font-size: 48px;
  color: #c0c4cc;
  margin-bottom: 16px;
}

:deep(.el-upload__text) {
  color: #606266;
  font-size: 14px;
}

:deep(.el-upload__text em) {
  color: #409eff;
  font-style: normal;
}

:deep(.el-upload__tip) {
  color: #909399;
  font-size: 12px;
  margin-top: 8px;
}

/* 难度标签样式 */
.difficulty-tag {
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
  color: white;
  display: inline-block;
  min-width: 40px;
  text-align: center;
}

.difficulty-easy {
  background-color: #67c23a; /* 绿色 */
}

.difficulty-medium {
  background-color: #e6a23c; /* 黄色 */
}

.difficulty-hard {
  background-color: #f56c6c; /* 红色 */
}

.difficulty-unknown {
  background-color: #909399; /* 灰色 */
}

/* 响应式设计 */
@media (max-width: 768px) {
  .question-bank {
    padding: 10px;
  }
  
  .operation-card .el-row {
    flex-direction: column;
    margin: 0 !important;
  }
  
  .operation-card .el-col {
    padding: 0 !important;
    margin-bottom: 12px;
  }
  
  /* 输入框、下拉框、搜索框宽度定宽优化 */
  .operation-card .el-select,
  .operation-card .el-input {
    width: 200px !important;
    min-width: 200px !important;
  }
  
  .operation-card .el-select .el-input,
  .operation-card .el-select .el-input__inner {
    width: 100% !important;
  }
  
  .operation-card .el-input__inner {
    width: 100% !important;
    font-size: 16px;
    padding: 12px 15px;
    border-radius: 6px;
  }
  
  .operation-card .el-select__wrapper {
    width: 100% !important;
  }
  
  .operation-buttons {
    text-align: left;
    margin-top: 15px;
  }
  
  .operation-buttons .el-button {
    margin-bottom: 10px;
    width: 100%;
    padding: 12px;
    font-size: 14px;
  }
  
  /* 表格优化 */
  .el-table {
    font-size: 13px;
  }
  
  .el-table th,
  .el-table td {
    padding: 8px 4px;
  }
  
  .el-table .el-button {
    padding: 4px 8px;
    font-size: 12px;
    margin: 2px;
  }
  
  /* 对话框优化 */
  .el-dialog {
    width: 95% !important;
    margin: 20px auto;
  }
  
  .el-dialog .el-form-item__label {
    width: 100% !important;
    text-align: left !important;
    margin-bottom: 8px;
    font-weight: 500;
  }
  
  .el-dialog .el-form-item__content {
    margin-left: 0 !important;
  }
  
  .el-dialog .el-input,
  .el-dialog .el-select,
  .el-dialog .el-textarea {
    width: 100% !important;
  }
  
  .el-dialog .el-input__inner,
  .el-dialog .el-textarea__inner {
    font-size: 16px;
    padding: 12px;
    border-radius: 6px;
  }
  
  /* 分页优化 */
  .pagination-container {
    text-align: center;
    margin-top: 15px;
  }
  
  .el-pagination {
    justify-content: center;
  }
  
  .el-pagination .el-pager li {
    min-width: 35px;
    height: 35px;
    line-height: 35px;
  }
}
</style>