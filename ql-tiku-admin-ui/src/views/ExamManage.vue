<template>
  <div class="exam-manage">
    <!-- 页面标题 -->
    <div class="page-header">
      <h2>考试管理</h2>
      <p>管理系统中的所有考试</p>
    </div>
    
    <!-- 操作栏 -->
    <el-card class="operation-card">
      <el-row :gutter="20">
        <el-col :span="16">
          <el-row :gutter="10">
            <el-col :span="6">
              <el-input
                v-model="searchForm.keyword"
                placeholder="搜索考试名称"
                clearable
                @keyup.enter="handleSearch"
              >
                <template #append>
                  <el-button icon="Search" @click="handleSearch" />
                </template>
              </el-input>
            </el-col>
            <el-col :span="4">
              <el-select v-model="searchForm.subjectId" placeholder="科目" clearable @change="handleSearch">
                <el-option label="全部科目" value="" />
                <el-option
                  v-for="subject in subjects"
                  :key="subject.id"
                  :label="subject.name"
                  :value="subject.id"
                />
              </el-select>
            </el-col>
            <el-col :span="4">
              <el-select v-model="searchForm.status" placeholder="状态" clearable @change="handleSearch">
                <el-option label="全部状态" value="" />
                <el-option label="草稿" :value="0" />
                <el-option label="已发布" :value="1" />
                <el-option label="已结束" :value="2" />
              </el-select>
            </el-col>
            <el-col :span="6">
              <el-date-picker
                v-model="searchForm.dateRange"
                type="daterange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                size="default"
                @change="handleSearch"
              />
            </el-col>
          </el-row>
        </el-col>
        <el-col :span="8" class="operation-buttons">
          <el-button type="primary" icon="Plus" @click="handleAdd">新增考试</el-button>
          <el-button
            type="danger"
            icon="Delete"
            :disabled="selectedExams.length === 0"
            @click="handleBatchDelete"
          >
            批量删除
          </el-button>
        </el-col>
      </el-row>
    </el-card>
    
    <!-- 考试列表 -->
    <el-card class="table-card">
      <el-table
        v-loading="loading"
        :data="exams"
        @selection-change="handleSelectionChange"
        stripe
        style="width: 100%"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="title" label="考试名称" min-width="200" />
        <el-table-column prop="subjectName" label="科目" width="120" />
        <el-table-column prop="questionCount" label="题目数量" width="100">
          <template #default="{ row }">
            <el-tag type="info">{{ row.questionCount }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="duration" label="考试时长" width="100">
          <template #default="{ row }">
            <span>{{ row.duration }}分钟</span>
          </template>
        </el-table-column>
        <el-table-column prop="totalScore" label="总分" width="80">
          <template #default="{ row }">
            <el-tag type="warning">{{ row.totalScore }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="participantCount" label="参与人数" width="100">
          <template #default="{ row }">
            <el-tag type="success">{{ row.participantCount }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="startTime" label="开始时间" width="180" />
        <el-table-column prop="endTime" label="结束时间" width="180" />
        <el-table-column label="操作" width="350" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleView(row)">查看</el-button>
            <el-button type="warning" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="info" size="small" @click="handleResults(row)">成绩</el-button>
            <el-button type="success" size="small" @click="handleExport(row)" icon="Download">导出</el-button>
            <el-button type="success" size="small" @click="handlePublish(row)" v-if="row.status === 0">发布</el-button>
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
    
    <!-- 考试编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="800px"
      :before-close="handleDialogClose"
    >
      <el-form
        ref="examFormRef"
        :model="examForm"
        :rules="examRules"
        label-width="100px"
        :disabled="dialogMode === 'view'"
      >
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="考试名称" prop="title">
              <el-input
                v-model="examForm.title"
                placeholder="请输入考试名称"
                maxlength="100"
                show-word-limit
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="科目" prop="subjectId">
              <el-select v-model="examForm.subjectId" placeholder="请选择科目" style="width: 100%">
                <el-option
                  v-for="subject in subjects"
                  :key="subject.id"
                  :label="subject.name"
                  :value="subject.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="题目数量" prop="questionCount">
              <el-input-number
                v-model="examForm.questionCount"
                :min="1"
                :max="200"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="考试时长" prop="duration">
              <el-input-number
                v-model="examForm.duration"
                :min="10"
                :max="300"
                style="width: 100%"
              />
              <span style="margin-left: 8px; color: #999;">分钟</span>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="总分" prop="totalScore">
              <el-input-number
                v-model="examForm.totalScore"
                :min="1"
                :max="1000"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="及格分数" prop="passScore">
              <el-input-number
                v-model="examForm.passScore"
                :min="1"
                :max="1000"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="开始时间" prop="startTime">
              <el-date-picker
                v-model="examForm.startTime"
                type="datetime"
                placeholder="选择开始时间"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结束时间" prop="endTime">
              <el-date-picker
                v-model="examForm.endTime"
                type="datetime"
                placeholder="选择结束时间"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-form-item label="考试说明" prop="description">
          <el-input
            v-model="examForm.description"
            type="textarea"
            :rows="4"
            placeholder="请输入考试说明"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
        
        <el-form-item label="考试类型" prop="examType">
          <el-radio-group v-model="examForm.examType" @change="handleExamTypeChange">
            <el-radio :label="0">随机题目</el-radio>
            <el-radio :label="1">自选题目</el-radio>
          </el-radio-group>
        </el-form-item>
        
        <!-- 随机题目配置 -->
        <div v-if="examForm.examType === 0">
          <el-form-item label="题目配置">
            <div class="question-config">
              <el-row :gutter="10" v-for="(config, index) in examForm.questionConfigs" :key="index">
                <el-col :span="5">
                  <el-select v-model="config.questionType" placeholder="题目类型">
                    <el-option label="单选题" :value="0" />
                    <el-option label="多选题" :value="1" />
                    <el-option label="判断题" :value="2" />
                    <el-option label="简答题" :value="3" />
                  </el-select>
                </el-col>
                <el-col :span="5">
                  <el-select v-model="config.difficulty" placeholder="难度">
                    <el-option label="简单" :value="1" />
                    <el-option label="中等" :value="2" />
                    <el-option label="困难" :value="3" />
                  </el-select>
                </el-col>
                <el-col :span="4">
                  <el-input-number v-model="config.count" :min="1" placeholder="数量" style="width: 100%" />
                </el-col>
                <el-col :span="4">
                  <el-input-number v-model="config.score" :min="1" placeholder="分值" style="width: 100%" />
                </el-col>
                <el-col :span="6">
                  <el-button type="danger" size="small" @click="removeQuestionConfig(index)">删除</el-button>
                </el-col>
              </el-row>
              <el-button type="primary" size="small" @click="addQuestionConfig">添加配置</el-button>
            </div>
          </el-form-item>
        </div>
        
        <!-- 自选题目配置 -->
        <div v-if="examForm.examType === 1">
          <el-form-item label="题目选择">
            <el-button type="primary" @click="showQuestionSelector">选择题目</el-button>
            <span style="margin-left: 10px; color: #666;">已选择 {{ examForm.selectedQuestionIds ? examForm.selectedQuestionIds.length : 0 }} 道题目</span>
          </el-form-item>
        </div>
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
    
    <!-- 考试成绩对话框 -->
    <el-dialog v-model="resultsDialogVisible" title="考试成绩" width="1000px">
      <div v-if="selectedExam">
        <div class="exam-stats">
          <el-row :gutter="20">
            <el-col :span="6">
              <div class="stat-item">
                <div class="stat-value">{{ selectedExam.participantCount }}</div>
                <div class="stat-label">参与人数</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="stat-item">
                <div class="stat-value">{{ selectedExam.avgScore }}分</div>
                <div class="stat-label">平均分</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="stat-item">
                <div class="stat-value">{{ selectedExam.passRate }}%</div>
                <div class="stat-label">及格率</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="stat-item">
                <div class="stat-value">{{ selectedExam.maxScore }}分</div>
                <div class="stat-label">最高分</div>
              </div>
            </el-col>
          </el-row>
        </div>
        
        <el-table :data="examResults" stripe style="width: 100%; margin-top: 20px;">
          <el-table-column prop="rank" label="排名" width="80" />
          <el-table-column prop="username" label="用户名" width="120" />
          <el-table-column prop="score" label="得分" width="100">
            <template #default="{ row }">
              <el-tag :type="getScoreType(row.score, selectedExam.totalScore)">{{ row.score }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="accuracy" label="正确率" width="100">
            <template #default="{ row }">
              <span>{{ row.accuracy }}%</span>
            </template>
          </el-table-column>
          <el-table-column prop="duration" label="用时" width="100">
            <template #default="{ row }">
              <span>{{ row.duration }}分钟</span>
            </template>
          </el-table-column>
          <el-table-column prop="submitTime" label="提交时间" width="180" />
          <el-table-column label="操作" width="120">
            <template #default="{ row }">
              <el-button type="primary" size="small" @click="viewAnswerSheet(row)">查看答卷</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-dialog>
    
    <!-- 题目选择器对话框 -->
    <el-dialog
      v-model="questionSelectorVisible"
      title="选择题目 - 题库管理"
      width="1400px"
      :before-close="handleQuestionSelectorClose"
      top="5vh"
    >
      <!-- 操作栏 -->
      <el-card class="operation-card" style="margin-bottom: 20px;">
        <el-row :gutter="20">
          <el-col :span="18">
            <el-row :gutter="10">
              <el-col :span="5">
                <el-select v-model="questionSearchForm.subjectId" placeholder="选择科目" clearable @change="searchQuestions">
                  <el-option label="全部科目" value="" />
                  <el-option
                    v-for="subject in subjects"
                    :key="subject.id"
                    :label="subject.name"
                    :value="subject.id"
                  />
                </el-select>
              </el-col>
              <el-col :span="5">
                <el-select v-model="questionSearchForm.questionType" placeholder="题目类型" clearable @change="searchQuestions">
                  <el-option label="全部类型" value="" />
                  <el-option label="单选题" :value="0" />
                  <el-option label="多选题" :value="1" />
                  <el-option label="判断题" :value="2" />
                  <el-option label="简答题" :value="3" />
                </el-select>
              </el-col>
              <el-col :span="5">
                <el-select v-model="questionSearchForm.difficulty" placeholder="难度" clearable @change="searchQuestions">
                  <el-option label="全部难度" value="" />
                  <el-option label="简单" :value="1" />
                  <el-option label="中等" :value="2" />
                  <el-option label="困难" :value="3" />
                </el-select>
              </el-col>
              <el-col :span="9">
                <el-input
                  v-model="questionSearchForm.keyword"
                  placeholder="搜索题目内容、知识点"
                  clearable
                  @keyup.enter="searchQuestions"
                >
                  <template #append>
                    <el-button icon="Search" @click="searchQuestions" />
                  </template>
                </el-input>
              </el-col>
            </el-row>
          </el-col>
          <el-col :span="6" class="operation-buttons">
            <el-button type="primary" @click="searchQuestions">搜索</el-button>
            <el-button @click="resetQuestionSearch">重置</el-button>
            <el-tag type="success" size="large">已选择: {{ selectedQuestions.length }} 道题目</el-tag>
          </el-col>
        </el-row>
      </el-card>
      
      <!-- 题目列表 -->
      <el-card class="table-card">
        <el-table
          v-loading="questionLoading"
          :data="questions"
          @selection-change="handleQuestionSelectionChange"
          stripe
          style="width: 100%"
          max-height="500px"
          :row-key="'id'"
        >
          <el-table-column type="selection" width="55" :reserve-selection="true" />
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="content" label="题目内容" min-width="350">
            <template #default="{ row }">
              <div class="question-content" v-html="row.content" style="max-height: 60px; overflow: hidden;"></div>
            </template>
          </el-table-column>
          <el-table-column prop="subjectName" label="科目" width="120" />
          <el-table-column prop="questionType" label="类型" width="100">
            <template #default="{ row }">
              <el-tag :type="getQuestionTypeTagType(row.questionType)">{{ getQuestionTypeText(row.questionType) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="difficulty" label="难度" width="100">
            <template #default="{ row }">
              <el-tag :type="getDifficultyType(row.difficulty)">{{ getDifficultyText(row.difficulty) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="knowledgePoints" label="知识点" width="180" show-overflow-tooltip />
          <el-table-column prop="createTime" label="创建时间" width="180" />
          <el-table-column label="操作" width="100">
            <template #default="{ row }">
              <el-button type="primary" size="small" @click="previewQuestion(row)">预览</el-button>
            </template>
          </el-table-column>
        </el-table>
        
        <!-- 分页 -->
        <div class="pagination-container" style="margin-top: 20px;">
          <el-pagination
            v-model:current-page="questionPagination.page"
            v-model:page-size="questionPagination.size"
            :page-sizes="[10, 20, 50, 100]"
            :total="questionPagination.total"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleQuestionSizeChange"
            @current-change="handleQuestionCurrentChange"
          />
        </div>
      </el-card>
      
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="handleQuestionSelectorClose">取消</el-button>
          <el-button type="primary" @click="confirmQuestionSelection" :disabled="selectedQuestions.length === 0">
            确定选择 ({{ selectedQuestions.length }})
          </el-button>
        </span>
      </template>
    </el-dialog>
    
    <!-- 题目预览对话框 -->
    <el-dialog
      v-model="questionPreviewVisible"
      title="题目预览"
      width="600px"
    >
      <div v-if="previewQuestionData">
        <div class="question-preview">
          <div class="question-header">
            <el-tag :type="getQuestionTypeTagType(previewQuestionData.questionType)">{{ getQuestionTypeText(previewQuestionData.questionType) }}</el-tag>
            <el-tag :type="getDifficultyType(previewQuestionData.difficulty)" style="margin-left: 10px;">{{ getDifficultyText(previewQuestionData.difficulty) }}</el-tag>
          </div>
          <div class="question-content" v-html="previewQuestionData.content" style="margin: 15px 0;"></div>
          <div v-if="previewQuestionData.options && previewQuestionData.options.length > 0" class="question-options">
            <div v-for="(option, index) in previewQuestionData.options" :key="index" class="option-item">
              <span class="option-label">{{ String.fromCharCode(65 + index) }}.</span>
              <span v-html="option.content"></span>
              <el-tag v-if="option.isCorrect" type="success" size="small" style="margin-left: 10px;">正确答案</el-tag>
            </div>
          </div>
          <div v-if="previewQuestionData.correctAnswer" class="correct-answer">
            <strong>正确答案：</strong>{{ previewQuestionData.correctAnswer }}
          </div>
          <div v-if="previewQuestionData.analysis" class="question-analysis">
            <strong>解析：</strong>{{ previewQuestionData.analysis }}
          </div>
          <div v-if="previewQuestionData.knowledgePoints" class="knowledge-points">
            <strong>知识点：</strong>{{ previewQuestionData.knowledgePoints }}
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { examAPI, subjectAPI } from '../api'

export default {
  name: 'ExamManage',
  setup() {
    const loading = ref(false)
    const submitLoading = ref(false)
    const dialogVisible = ref(false)
    const resultsDialogVisible = ref(false)
    const dialogMode = ref('add') // add, edit, view
    const examFormRef = ref()
    
    const exams = ref([])
    const subjects = ref([])
    const selectedExams = ref([])
    const selectedExam = ref(null)
    const examResults = ref([])
    
    // 题目选择器相关
    const questionSelectorVisible = ref(false)
    const questionLoading = ref(false)
    const questions = ref([])
    const selectedQuestions = ref([])
    
    // 题目预览相关
    const questionPreviewVisible = ref(false)
    const previewQuestionData = ref(null)
    
    const questionSearchForm = reactive({
      keyword: '',
      subjectId: '',
      questionType: '',
      difficulty: ''
    })
    
    const questionPagination = reactive({
      page: 1,
      size: 20,
      total: 0
    })
    
    const searchForm = reactive({
      keyword: '',
      subjectId: '',
      status: '',
      dateRange: []
    })
    
    const pagination = reactive({
      page: 1,
      size: 20,
      total: 0
    })
    
    const examForm = reactive({
      id: null,
      title: '',
      subjectId: '',
      questionCount: 50,
      duration: 120,
      totalScore: 100,
      passScore: 60,
      startTime: '',
      endTime: '',
      description: '',
      examType: 0, // 0-随机题目，1-自选题目
      selectedQuestionIds: [], // 自选题目ID列表
      questionConfigs: [ // 随机题目配置
        { questionType: 0, difficulty: 1, count: 20, score: 2 },
        { questionType: 1, difficulty: 2, count: 15, score: 3 },
        { questionType: 2, difficulty: 1, count: 15, score: 1 }
      ]
    })
    
    const examRules = {
      title: [
        { required: true, message: '请输入考试名称', trigger: 'blur' },
        { min: 2, max: 100, message: '考试名称长度在 2 到 100 个字符', trigger: 'blur' }
      ],
      subjectId: [
        { required: true, message: '请选择科目', trigger: 'change' }
      ],
      questionCount: [
        { required: true, message: '请输入题目数量', trigger: 'blur' }
      ],
      duration: [
        { required: true, message: '请输入考试时长', trigger: 'blur' }
      ],
      totalScore: [
        { required: true, message: '请输入总分', trigger: 'blur' }
      ],
      passScore: [
        { required: true, message: '请输入及格分数', trigger: 'blur' }
      ],
      startTime: [
        { required: true, message: '请选择开始时间', trigger: 'change' }
      ],
      endTime: [
        { required: true, message: '请选择结束时间', trigger: 'change' }
      ]
    }
    
    const dialogTitle = computed(() => {
      const titles = {
        add: '新增考试',
        edit: '编辑考试',
        view: '查看考试'
      }
      return titles[dialogMode.value]
    })
    
    // 获取状态类型
    const getStatusType = (status) => {
      const types = {
        0: 'info',     // 草稿
        1: 'success',  // 已发布
        2: 'danger'    // 已结束
      }
      return types[status] || 'info'
    }
    
    // 获取状态文本
    const getStatusText = (status) => {
      const texts = {
        0: '草稿',
        1: '已发布',
        2: '已结束'
      }
      return texts[status] || '未知'
    }
    
    // 获取分数类型
    const getScoreType = (score, totalScore) => {
      const percentage = (score / totalScore) * 100
      if (percentage >= 90) return 'success'
      if (percentage >= 80) return 'warning'
      if (percentage >= 60) return 'info'
      return 'danger'
    }
    
    // 获取科目列表
    const getSubjects = async () => {
      try {
        const response = await subjectAPI.getSubjects()
        // 后端返回的是IPage对象，使用records字段
        subjects.value = response.records || []
      } catch (error) {
        console.error('获取科目列表失败:', error)
        ElMessage.error('获取科目列表失败')
        subjects.value = []
      }
    }
    
    // 获取考试列表
    const getExams = async () => {
      try {
        loading.value = true
        const response = await examAPI.getExams({
          ...searchForm,
          current: pagination.page,
          size: pagination.size
        })
        // 后端返回的是IPage对象，使用records和total字段
        exams.value = response.records || []
        pagination.total = response.total || 0
      } catch (error) {
        console.error('获取考试列表失败:', error)
        ElMessage.error('获取考试列表失败')
        exams.value = []
        pagination.total = 0
      } finally {
        loading.value = false
      }
    }
    
    // 搜索
    const handleSearch = () => {
      pagination.page = 1
      getExams()
    }
    
    // 分页大小改变
    const handleSizeChange = (size) => {
      pagination.size = size
      getExams()
    }
    
    // 当前页改变
    const handleCurrentChange = (page) => {
      pagination.page = page
      getExams()
    }
    
    // 选择改变
    const handleSelectionChange = (selection) => {
      selectedExams.value = selection
    }
    
    // 新增考试
    const handleAdd = () => {
      dialogMode.value = 'add'
      resetExamForm()
      dialogVisible.value = true
    }
    
    // 查看考试
    const handleView = (row) => {
      dialogMode.value = 'view'
      loadExamForm(row)
      dialogVisible.value = true
    }
    
    // 编辑考试
    const handleEdit = (row) => {
      dialogMode.value = 'edit'
      loadExamForm(row)
      dialogVisible.value = true
    }
    
    // 查看成绩
    const handleResults = async (row) => {
      try {
        selectedExam.value = row
        const results = await examAPI.getExamResults({ examId: row.id })
        examResults.value = results.list || []
        
        resultsDialogVisible.value = true
      } catch (error) {
        console.error('获取考试成绩失败:', error)
        ElMessage.error('获取考试成绩失败')
        examResults.value = []
      }
    }
    
    // 发布考试
    const handlePublish = async (row) => {
      try {
        await ElMessageBox.confirm(`确定要发布考试 "${row.title}" 吗？`, '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        
        await examAPI.publishExam(row.id)
        ElMessage.success('发布成功')
        getExams()
      } catch (error) {
        if (error !== 'cancel') {
          console.error('发布考试失败:', error)
          ElMessage.error('发布考试失败')
        }
      }
    }
    
    // 删除考试
    const handleDelete = async (row) => {
      try {
        await ElMessageBox.confirm(`确定要删除考试 "${row.title}" 吗？`, '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        
        await examAPI.deleteExam(row.id)
        ElMessage.success('删除成功')
        getExams()
      } catch (error) {
        if (error !== 'cancel') {
          console.error('删除考试失败:', error)
          ElMessage.error('删除考试失败')
        }
      }
    }
    
    // 批量删除
    const handleBatchDelete = async () => {
      if (selectedExams.value.length === 0) {
        ElMessage.warning('请选择要删除的考试')
        return
      }
      
      try {
        await ElMessageBox.confirm(`确定要删除选中的 ${selectedExams.value.length} 个考试吗？`, '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        
        const ids = selectedExams.value.map(item => item.id)
        await Promise.all(ids.map(id => examAPI.deleteExam(id)))
        ElMessage.success('批量删除成功')
        selectedExams.value = []
        getExams()
      } catch (error) {
        if (error !== 'cancel') {
          console.error('批量删除失败:', error)
          ElMessage.error('批量删除失败')
        }
      }
    }
    
    // 导出数据
    const handleExport = async (exam) => {
      try {
        const token = localStorage.getItem('adminToken') || localStorage.getItem('token')
        const response = await fetch(`/api/admin/exams/${exam.id}/export`, {
          method: 'GET',
          headers: {
            'Authorization': `Bearer ${token}`
          }
        })
        
        if (response.ok) {
          const blob = await response.blob()
          const url = window.URL.createObjectURL(blob)
          const a = document.createElement('a')
          a.href = url
          a.download = `${exam.title}_考试数据.xlsx`
          document.body.appendChild(a)
          a.click()
          window.URL.revokeObjectURL(url)
          document.body.removeChild(a)
          ElMessage.success('导出成功')
        } else {
          ElMessage.error('导出失败')
        }
      } catch (error) {
        console.error('导出失败:', error)
        ElMessage.error('导出失败')
      }
    }
    
    // 查看答卷
    // eslint-disable-next-line no-unused-vars
    const viewAnswerSheet = (row) => {
      ElMessage.info('查看答卷功能开发中...')
    }
    

    
    // 重置表单
    const resetExamForm = () => {
      Object.assign(examForm, {
        id: null,
        title: '',
        subjectId: '',
        questionCount: 50,
        duration: 120,
        totalScore: 100,
        passScore: 60,
        startTime: '',
        endTime: '',
        description: '',
        examType: 0,
        selectedQuestionIds: [],
        questionConfigs: [
          { questionType: 0, difficulty: 1, count: 20, score: 2 },
          { questionType: 1, difficulty: 2, count: 15, score: 3 },
          { questionType: 2, difficulty: 1, count: 15, score: 1 }
        ]
      })
    }
    
    // 加载表单数据
    const loadExamForm = (row) => {
      Object.assign(examForm, {
        id: row.id,
        title: row.title,
        subjectId: row.subjectId || 1,
        questionCount: row.questionCount,
        duration: row.duration,
        totalScore: row.totalScore,
        passScore: row.passScore || 60,
        startTime: row.startTime,
        endTime: row.endTime,
        description: row.description || '',
        examType: row.examType || 0,
        selectedQuestionIds: row.selectedQuestionIds || [],
        questionConfigs: row.questionConfigs || [
          { questionType: 0, difficulty: 1, count: 20, score: 2 },
          { questionType: 1, difficulty: 2, count: 15, score: 3 },
          { questionType: 2, difficulty: 1, count: 15, score: 1 }
        ]
      })
    }
    
    // 关闭对话框
    const handleDialogClose = () => {
      dialogVisible.value = false
      examFormRef.value?.resetFields()
    }
    
    // 提交表单
    const handleSubmit = async () => {
      if (!examFormRef.value) return
      
      try {
        await examFormRef.value.validate()
        submitLoading.value = true
        
        // 格式化日期时间
        const formatDateTime = (date) => {
          if (!date) return null
          const d = new Date(date)
          const year = d.getFullYear()
          const month = String(d.getMonth() + 1).padStart(2, '0')
          const day = String(d.getDate()).padStart(2, '0')
          const hours = String(d.getHours()).padStart(2, '0')
          const minutes = String(d.getMinutes()).padStart(2, '0')
          const seconds = String(d.getSeconds()).padStart(2, '0')
          return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
        }
        
        // 准备提交数据，转换日期格式
        const submitData = {
          ...examForm,
          startTime: formatDateTime(examForm.startTime),
          endTime: formatDateTime(examForm.endTime)
        }
        
        if (dialogMode.value === 'add') {
          await examAPI.createExam(submitData)
          ElMessage.success('创建成功')
        } else {
          await examAPI.updateExam(submitData.id, submitData)
          ElMessage.success('更新成功')
        }
        
        handleDialogClose()
        getExams()
      } catch (error) {
        console.error('提交失败:', error)
        ElMessage.error(dialogMode.value === 'add' ? '创建失败' : '更新失败')
      } finally {
        submitLoading.value = false
      }
    }
    
    // 考试类型改变
    const handleExamTypeChange = (value) => {
      if (value === 0) {
        // 切换到随机题目模式，重置自选题目
        examForm.selectedQuestionIds = []
      } else {
        // 切换到自选题目模式，重置随机配置
        examForm.questionConfigs = [
          { questionType: 0, difficulty: 1, count: 20, score: 2 },
          { questionType: 1, difficulty: 2, count: 15, score: 3 },
          { questionType: 2, difficulty: 1, count: 15, score: 1 }
        ]
      }
    }
    
    // 添加题目配置
    const addQuestionConfig = () => {
      examForm.questionConfigs.push({
        questionType: 0,
        difficulty: 1,
        count: 1,
        score: 1
      })
    }
    
    // 删除题目配置
    const removeQuestionConfig = (index) => {
      examForm.questionConfigs.splice(index, 1)
    }
    
    // 显示题目选择器
    const showQuestionSelector = () => {
      questionSearchForm.subjectId = examForm.subjectId
      questionSelectorVisible.value = true
      getQuestions()
    }
    
    // 获取题目列表
    const getQuestions = async () => {
      try {
        questionLoading.value = true
        const response = await examAPI.getQuestionsForExam({
          ...questionSearchForm,
          current: questionPagination.page,
          size: questionPagination.size
        })
        questions.value = response.records || []
        questionPagination.total = response.total || 0
      } catch (error) {
        console.error('获取题目列表失败:', error)
        ElMessage.error('获取题目列表失败')
        questions.value = []
        questionPagination.total = 0
      } finally {
        questionLoading.value = false
      }
    }
    
    // 搜索题目
    const searchQuestions = () => {
      questionPagination.page = 1
      getQuestions()
    }
    
    // 重置题目搜索
    const resetQuestionSearch = () => {
      Object.assign(questionSearchForm, {
        keyword: '',
        questionType: '',
        difficulty: ''
      })
      searchQuestions()
    }
    
    // 题目分页大小改变
    const handleQuestionSizeChange = (size) => {
      questionPagination.size = size
      getQuestions()
    }
    
    // 题目当前页改变
    const handleQuestionCurrentChange = (page) => {
      questionPagination.page = page
      getQuestions()
    }
    
    // 题目选择改变
    const handleQuestionSelectionChange = (selection) => {
      selectedQuestions.value = selection
    }
    
    // 确认题目选择
    const confirmQuestionSelection = () => {
      examForm.selectedQuestionIds = selectedQuestions.value.map(q => q.id)
      examForm.questionCount = selectedQuestions.value.length
      
      // 计算总分
      let totalScore = 0
      selectedQuestions.value.forEach(question => {
        // 根据题目类型设置默认分值
        switch (question.questionType) {
          case 0: // 单选题
            totalScore += 2
            break
          case 1: // 多选题
            totalScore += 3
            break
          case 2: // 判断题
            totalScore += 1
            break
          case 3: // 简答题
            totalScore += 5
            break
          default:
            totalScore += 2
        }
      })
      examForm.totalScore = totalScore
      
      questionSelectorVisible.value = false
      ElMessage.success(`已选择 ${selectedQuestions.value.length} 道题目`)
    }
    
    // 关闭题目选择器
    const handleQuestionSelectorClose = () => {
      questionSelectorVisible.value = false
      selectedQuestions.value = []
    }
    
    // 获取难度类型
    const getDifficultyType = (difficulty) => {
      const types = {
        1: 'success',
        2: 'warning', 
        3: 'danger'
      }
      return types[difficulty] || 'info'
    }
    
    // 获取难度文本
    const getDifficultyText = (difficulty) => {
      const texts = { 1: '简单', 2: '中等', 3: '困难' }
      return texts[difficulty] || '未知'
    }
    
    // 获取题目类型标签样式
    const getQuestionTypeTagType = (type) => {
      const types = { 0: 'primary', 1: 'success', 2: 'warning', 3: 'info' }
      return types[type] || 'info'
    }
    
    // 获取题目类型文本
    const getQuestionTypeText = (type) => {
      const texts = { 0: '单选题', 1: '多选题', 2: '判断题', 3: '简答题' }
      return texts[type] || '未知'
    }
    
    // 预览题目
    const previewQuestion = (question) => {
      previewQuestionData.value = question
      questionPreviewVisible.value = true
    }
    
    onMounted(() => {
      getSubjects()
      getExams()
    })
    
    return {
      loading,
      submitLoading,
      dialogVisible,
      resultsDialogVisible,
      dialogMode,
      dialogTitle,
      examFormRef,
      exams,
      subjects,
      selectedExams,
      selectedExam,
      examResults,
      searchForm,
      pagination,
      examForm,
      examRules,
      // 题目选择器相关
      questionSelectorVisible,
      questionLoading,
      questions,
      selectedQuestions,
      questionSearchForm,
      questionPagination,
      getStatusType,
      getStatusText,
      getScoreType,
      getDifficultyType,
      getDifficultyText,
      getQuestionTypeTagType,
      getQuestionTypeText,
      previewQuestion,
      questionPreviewVisible,
      previewQuestionData,
      handleSearch,
      handleSizeChange,
      handleCurrentChange,
      handleSelectionChange,
      handleAdd,
      handleView,
      handleEdit,
      handleResults,
      handlePublish,
      handleDelete,
      handleBatchDelete,
      handleExport,
      viewAnswerSheet,
      handleExamTypeChange,
      addQuestionConfig,
      removeQuestionConfig,
      showQuestionSelector,
      getQuestions,
      searchQuestions,
      resetQuestionSearch,
      handleQuestionSizeChange,
      handleQuestionCurrentChange,
      handleQuestionSelectionChange,
      confirmQuestionSelection,
      handleQuestionSelectorClose,
      handleDialogClose,
      handleSubmit
    }
  }
}
</script>

<style scoped>
.exam-manage {
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

.pagination-container {
  margin-top: 20px;
  text-align: right;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.question-config {
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  padding: 15px;
  background-color: #f8f9fa;
}

.question-config .el-row {
  margin-bottom: 10px;
}

.exam-stats {
  margin-bottom: 20px;
}

.stat-item {
  text-align: center;
  padding: 20px;
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

/* 题目预览样式 */
.question-preview {
  padding: 20px;
}

.question-header {
  margin-bottom: 15px;
}

.question-content {
  font-size: 16px;
  line-height: 1.6;
  color: #303133;
  padding: 15px;
  background-color: #f8f9fa;
  border-radius: 6px;
  border-left: 4px solid #409eff;
}

.question-options {
  margin: 20px 0;
}

.option-item {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
  padding: 10px;
  background-color: #fafbfc;
  border-radius: 4px;
  border: 1px solid #e4e7ed;
}

.option-label {
  font-weight: bold;
  margin-right: 10px;
  color: #409eff;
  min-width: 20px;
}

.correct-answer {
  margin: 15px 0;
  padding: 10px;
  background-color: #f0f9ff;
  border-radius: 4px;
  border-left: 4px solid #67c23a;
}

.question-analysis {
  margin: 15px 0;
  padding: 10px;
  background-color: #fdf6ec;
  border-radius: 4px;
  border-left: 4px solid #e6a23c;
}

.knowledge-points {
  margin: 15px 0;
  padding: 10px;
  background-color: #f4f4f5;
  border-radius: 4px;
  border-left: 4px solid #909399;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .exam-manage {
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
  .operation-card .el-input,
  .operation-card .el-select,
  .operation-card .el-date-picker {
    width: 200px !important;
    min-width: 200px !important;
  }
  
  .operation-card .el-input__inner,
  .operation-card .el-select .el-input__inner {
    width: 100% !important;
    font-size: 16px;
    padding: 12px 15px;
    border-radius: 6px;
  }
  
  .operation-card .el-select__wrapper {
    width: 100% !important;
  }
  
  .operation-card .el-date-picker .el-input__inner {
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
  .el-dialog .el-textarea,
  .el-dialog .el-input-number {
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
  
  /* 题目配置优化 */
  .question-config .el-row {
    flex-direction: column;
    margin: 0 !important;
  }
  
  .question-config .el-col {
    margin-bottom: 10px;
    padding: 0 !important;
  }
  
  .question-config .el-input,
  .question-config .el-select,
  .question-config .el-input-number {
    width: 100% !important;
  }
  
  /* 题目选择器对话框优化 */
  .el-dialog .question-selector {
    width: 100% !important;
  }
  
  .el-dialog .question-selector .el-input,
  .el-dialog .question-selector .el-select {
    width: 100% !important;
  }
}
</style>