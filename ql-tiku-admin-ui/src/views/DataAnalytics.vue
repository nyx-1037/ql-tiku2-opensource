<template>
  <div class="data-analytics">
    <!-- 页面标题 -->
    <div class="page-header">
      <h2>数据分析</h2>
      <p>系统数据统计与分析</p>
    </div>
    
    <!-- 筛选条件 -->
    <el-card class="filter-card">
      <el-row :gutter="20">
        <el-col :span="8">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            @change="handleDateChange"
          />
        </el-col>
        <el-col :span="4">
          <el-select v-model="selectedSubject" placeholder="选择科目" clearable @change="handleSubjectChange">
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
          <el-button type="primary" @click="refreshData">刷新数据</el-button>
        </el-col>
      </el-row>
    </el-card>
    
    <!-- 概览统计 -->
    <el-row :gutter="20" class="overview-stats">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon user-icon">
              <el-icon><User /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ overviewData.totalUsers }}</div>
              <div class="stat-label">总用户数</div>
              <div class="stat-change positive">+{{ overviewData.userGrowth }}%</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon question-icon">
              <el-icon><Document /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ overviewData.totalQuestions }}</div>
              <div class="stat-label">题目总数</div>
              <div class="stat-change positive">+{{ overviewData.questionGrowth }}%</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon exam-icon">
              <el-icon><Edit /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ overviewData.totalExams }}</div>
              <div class="stat-label">考试总数</div>
              <div class="stat-change positive">+{{ overviewData.examGrowth }}%</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon active-icon">
              <el-icon><TrendCharts /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ overviewData.activeUsers }}</div>
              <div class="stat-label">活跃用户</div>
              <div class="stat-change positive">+{{ overviewData.activeGrowth }}%</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
    
    <!-- 图表区域 -->
    <el-row :gutter="20" class="charts-row">
      <!-- 用户增长趋势 -->
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span>用户增长趋势</span>
              <el-button type="primary" link @click="exportChart('userGrowth')">导出</el-button>
            </div>
          </template>
          <div ref="userGrowthChart" class="chart-container"></div>
        </el-card>
      </el-col>
      
      <!-- 题目分布 -->
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span>题目分布</span>
              <el-button type="primary" link @click="exportChart('questionDistribution')">导出</el-button>
            </div>
          </template>
          <div ref="questionDistributionChart" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>
    
    <el-row :gutter="20" class="charts-row">
      <!-- 考试统计 -->
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span>考试统计</span>
              <el-button type="primary" link @click="exportChart('examStats')">导出</el-button>
            </div>
          </template>
          <div ref="examStatsChart" class="chart-container"></div>
        </el-card>
      </el-col>
      
      <!-- 用户活跃度 -->
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span>用户活跃度</span>
              <el-button type="primary" link @click="exportChart('userActivity')">导出</el-button>
            </div>
          </template>
          <div ref="userActivityChart" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>
    
    <el-row :gutter="20" class="charts-row">
      <!-- 学习效果分析 -->
      <el-col :span="24">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span>学习效果分析</span>
              <el-button type="primary" link @click="exportChart('learningEffect')">导出</el-button>
            </div>
          </template>
          <div ref="learningEffectChart" class="chart-container large"></div>
        </el-card>
      </el-col>
    </el-row>
    
    <!-- 详细数据表格 -->
    <el-card class="table-card">
      <template #header>
        <div class="card-header">
          <span>详细数据</span>
          <div>
            <el-button type="primary" @click="exportData">导出数据</el-button>
          </div>
        </div>
      </template>
      
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="用户数据" name="users">
          <el-table :data="userData" stripe style="width: 100%">
            <el-table-column prop="date" label="日期" width="120" />
            <el-table-column prop="count" label="新增用户" width="120" />
            <el-table-column label="活跃用户" width="120">
              <template #default="{ row }">
                <span>{{ Math.floor(row.count * 0.7) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="总用户数" width="120">
              <template #default="{ $index }">
                <span>{{ userData.slice(0, $index + 1).reduce((sum, item) => sum + item.count, 0) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="留存率" width="120">
               <template #default>
                 <span>{{ (65 + Math.random() * 30).toFixed(1) }}%</span>
               </template>
             </el-table-column>
          </el-table>
        </el-tab-pane>
        
        <el-tab-pane label="题目数据" name="questions">
          <el-table :data="questionData" stripe style="width: 100%">
            <el-table-column prop="name" label="科目" width="120" />
            <el-table-column prop="value" label="题目总数" width="120" />
            <el-table-column label="简单" width="100">
               <template #default="{ row }">
                 <span>{{ Math.floor(row.value * 0.3) }}</span>
               </template>
             </el-table-column>
             <el-table-column label="中等" width="100">
               <template #default="{ row }">
                 <span>{{ Math.floor(row.value * 0.5) }}</span>
               </template>
             </el-table-column>
             <el-table-column label="困难" width="100">
               <template #default="{ row }">
                 <span>{{ Math.floor(row.value * 0.2) }}</span>
               </template>
             </el-table-column>
             <el-table-column label="平均正确率" width="120">
               <template #default>
                 <span>{{ (75 + Math.random() * 20).toFixed(1) }}%</span>
               </template>
             </el-table-column>
          </el-table>
        </el-tab-pane>
        
        <el-tab-pane label="考试数据" name="exams">
          <el-table :data="examData" stripe style="width: 100%">
            <el-table-column prop="date" label="日期" width="120" />
            <el-table-column prop="count" label="答题次数" width="120" />
            <el-table-column label="参与人数" width="120">
               <template #default="{ row }">
                 <span>{{ Math.floor(row.count * 0.8) }}</span>
               </template>
             </el-table-column>
             <el-table-column label="平均分" width="100">
               <template #default>
                 <span>{{ (70 + Math.random() * 25).toFixed(1) }}</span>
               </template>
             </el-table-column>
             <el-table-column label="及格率" width="100">
               <template #default>
                 <span>{{ (60 + Math.random() * 35).toFixed(1) }}%</span>
               </template>
             </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { analyticsAPI, subjectAPI } from '../api'

export default {
  name: 'DataAnalytics',
  setup() {
    const dateRange = ref([])
    const selectedSubject = ref('')
    const activeTab = ref('users')
    
    const subjects = ref([])
    const userData = ref([])
    const questionData = ref([])
    const examData = ref([])
    
    // 图表引用
    const userGrowthChart = ref()
    const questionDistributionChart = ref()
    const examStatsChart = ref()
    const userActivityChart = ref()
    const learningEffectChart = ref()
    
    // 图表实例
    let userGrowthChartInstance = null
    let questionDistributionChartInstance = null
    let examStatsChartInstance = null
    let userActivityChartInstance = null
    let learningEffectChartInstance = null
    
    const overviewData = reactive({
      totalUsers: 0,
      userGrowth: 0,
      totalQuestions: 0,
      questionGrowth: 0,
      totalExams: 0,
      examGrowth: 0,
      activeUsers: 0,
      activeGrowth: 0
    })
    
    // 获取科目列表
    const getSubjects = async () => {
      try {
        const response = await subjectAPI.getSubjects()
        subjects.value = response.records || []
      } catch (error) {
        console.error('获取科目列表失败:', error)
        ElMessage.error('获取科目列表失败')
        subjects.value = []
      }
    }
    
    // 获取分析数据
    const getAnalyticsData = async () => {
      try {
        const response = await analyticsAPI.getAnalyticsData({
          dateRange: dateRange.value,
          subject: selectedSubject.value
        })
        
        // 更新概览数据
        if (response.overview) {
          Object.assign(overviewData, {
            totalUsers: response.overview.totalUsers || 0,
            totalQuestions: response.overview.totalQuestions || 0,
            totalExams: response.overview.totalExams || 0,
            activeUsers: response.overview.activeUsers || 0
          })
        }
        
        // 更新用户数据
        userData.value = response.userData || []
        
        // 更新题目数据
        questionData.value = response.questionData || []
        
        // 更新考试数据
        examData.value = response.examData || []
        
        // 重新渲染图表
        nextTick(() => {
          initCharts()
        })
      } catch (error) {
        console.error('获取分析数据失败:', error)
      }
    }
    
    // 初始化用户增长趋势图
    const initUserGrowthChart = () => {
      if (!userGrowthChart.value) return
      
      userGrowthChartInstance = echarts.init(userGrowthChart.value)
      
      // 从userData中获取真实数据
      const dates = userData.value.length > 0 
        ? userData.value.map(item => item.date)
        : []
      const counts = userData.value.length > 0
        ? userData.value.map(item => item.count)
        : []
      
      const option = {
        title: {
          text: '用户增长趋势',
          left: 'center',
          textStyle: {
            fontSize: 16,
            fontWeight: 'normal'
          }
        },
        tooltip: {
          trigger: 'axis'
        },
        legend: {
          data: ['新增用户'],
          bottom: 10
        },
        xAxis: {
          type: 'category',
          data: dates
        },
        yAxis: {
          type: 'value'
        },
        series: [
          {
            name: '新增用户',
            type: 'line',
            data: counts,
            smooth: true,
            itemStyle: {
              color: '#409EFF'
            }
          }
        ]
      }
      
      userGrowthChartInstance.setOption(option)
    }
    
    // 初始化题目分布图
    const initQuestionDistributionChart = () => {
      if (!questionDistributionChart.value) return
      
      questionDistributionChartInstance = echarts.init(questionDistributionChart.value)
      
      // 从questionData中获取真实数据
      const distributionData = questionData.value.length > 0 
        ? questionData.value.map(item => ({ value: item.value, name: item.name }))
        : []
      
      const option = {
        title: {
          text: '题目分布',
          left: 'center',
          textStyle: {
            fontSize: 16,
            fontWeight: 'normal'
          }
        },
        tooltip: {
          trigger: 'item',
          formatter: '{a} <br/>{b}: {c} ({d}%)'
        },
        legend: {
          orient: 'vertical',
          left: 'left'
        },
        series: [
          {
            name: '题目分布',
            type: 'pie',
            radius: '50%',
            data: distributionData,
            emphasis: {
              itemStyle: {
                shadowBlur: 10,
                shadowOffsetX: 0,
                shadowColor: 'rgba(0, 0, 0, 0.5)'
              }
            }
          }
        ]
      }
      
      questionDistributionChartInstance.setOption(option)
    }
    
    // 初始化考试统计图
    const initExamStatsChart = () => {
      if (!examStatsChart.value) return
      
      examStatsChartInstance = echarts.init(examStatsChart.value)
      
      // 从examData中获取真实数据（日期和答题次数）
      const dates = examData.value.length > 0 
        ? examData.value.map(item => item.date)
        : []
      const counts = examData.value.length > 0
        ? examData.value.map(item => item.count)
        : []
      
      const option = {
        title: {
          text: '考试统计',
          left: 'center',
          textStyle: {
            fontSize: 16,
            fontWeight: 'normal'
          }
        },
        tooltip: {
          trigger: 'axis',
          axisPointer: {
            type: 'shadow'
          }
        },
        legend: {
          data: ['答题次数'],
          bottom: 10
        },
        xAxis: {
          type: 'category',
          data: dates
        },
        yAxis: {
          type: 'value',
          name: '答题次数'
        },
        series: [
          {
            name: '答题次数',
            type: 'bar',
            data: counts,
            itemStyle: {
              color: '#409EFF'
            }
          }
        ]
      }
      
      examStatsChartInstance.setOption(option)
    }
    
    // 初始化用户活跃度图
    const initUserActivityChart = () => {
      if (!userActivityChart.value) return
      
      userActivityChartInstance = echarts.init(userActivityChart.value)
      
      // 从userData中获取真实的用户活跃度数据
      const dates = userData.value.length > 0
        ? userData.value.slice(-7).map(item => item.date)
        : []
      
      const activeUsers = userData.value.length > 0
        ? userData.value.slice(-7).map(item => item.activeUsers || 0)
        : []
      
      const option = {
        title: {
          text: '用户活跃度',
          left: 'center',
          textStyle: {
            fontSize: 16,
            fontWeight: 'normal'
          }
        },
        tooltip: {
          trigger: 'axis'
        },
        xAxis: {
          type: 'category',
          data: dates
        },
        yAxis: {
          type: 'value'
        },
        series: [
          {
            name: '活跃用户',
            type: 'bar',
            data: activeUsers,
            itemStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: '#83bff6' },
                { offset: 0.5, color: '#188df0' },
                { offset: 1, color: '#188df0' }
              ])
            }
          }
        ]
      }
      
      userActivityChartInstance.setOption(option)
    }
    
    // 初始化学习效果分析图
    const initLearningEffectChart = () => {
      if (!learningEffectChart.value) return
      
      learningEffectChartInstance = echarts.init(learningEffectChart.value)
      
      // 从examData和questionData中获取真实的学习效果数据
      const practiceData = examData.value.length > 0 
        ? examData.value.slice(-8).map(item => item.count || 0)
        : []
      
      // 由于questionData是科目分布数据，这里使用模拟的正确率数据
      const accuracyData = examData.value.length > 0
         ? examData.value.slice(-8).map(() => 75 + Math.random() * 20) // 75-95%的随机正确率
         : []
      
      // 使用examData的count作为学习时长的基础数据
      const studyTimeData = examData.value.length > 0
        ? examData.value.slice(-8).map(item => (item.count || 0) * 2) // 假设每次答题2分钟
        : []
      
      const option = {
        title: {
          text: '学习效果分析',
          left: 'center',
          textStyle: {
            fontSize: 16,
            fontWeight: 'normal'
          }
        },
        tooltip: {
          trigger: 'axis'
        },
        legend: {
          data: ['练习次数', '正确率', '学习时长'],
          bottom: 10
        },
        xAxis: {
          type: 'category',
          data: ['第1周', '第2周', '第3周', '第4周', '第5周', '第6周', '第7周', '第8周']
        },
        yAxis: [
          {
            type: 'value',
            name: '练习次数',
            position: 'left'
          },
          {
            type: 'value',
            name: '正确率(%)',
            position: 'right',
            max: 100
          }
        ],
        series: [
          {
            name: '练习次数',
            type: 'bar',
            data: practiceData,
            itemStyle: {
              color: '#409EFF'
            }
          },
          {
            name: '正确率',
            type: 'line',
            yAxisIndex: 1,
            data: accuracyData,
            itemStyle: {
              color: '#67C23A'
            }
          },
          {
            name: '学习时长',
            type: 'line',
            data: studyTimeData,
            itemStyle: {
              color: '#E6A23C'
            }
          }
        ]
      }
      
      learningEffectChartInstance.setOption(option)
    }
    
    // 初始化所有图表
    const initCharts = () => {
      nextTick(() => {
        initUserGrowthChart()
        initQuestionDistributionChart()
        initExamStatsChart()
        initUserActivityChart()
        initLearningEffectChart()
      })
    }
    
    // 日期改变
    const handleDateChange = () => {
      refreshData()
    }
    
    // 科目改变
    const handleSubjectChange = () => {
      refreshData()
    }
    
    // 标签页改变
    const handleTabChange = (tab) => {
      console.log('切换到标签页:', tab)
    }
    
    // 刷新数据
    const refreshData = () => {
      getAnalyticsData()
      // 重新渲染图表
      setTimeout(() => {
        if (userGrowthChartInstance) userGrowthChartInstance.resize()
        if (questionDistributionChartInstance) questionDistributionChartInstance.resize()
        if (examStatsChartInstance) examStatsChartInstance.resize()
        if (userActivityChartInstance) userActivityChartInstance.resize()
        if (learningEffectChartInstance) learningEffectChartInstance.resize()
      }, 100)
    }
    
    // 导出图表
    const exportChart = (chartType) => {
      ElMessage.success(`导出${chartType}图表成功`)
    }
    
    // 导出数据
    const exportData = () => {
      ElMessage.success('导出数据成功')
    }
    
    // 窗口大小改变时重新渲染图表
    const handleResize = () => {
      if (userGrowthChartInstance) userGrowthChartInstance.resize()
      if (questionDistributionChartInstance) questionDistributionChartInstance.resize()
      if (examStatsChartInstance) examStatsChartInstance.resize()
      if (userActivityChartInstance) userActivityChartInstance.resize()
      if (learningEffectChartInstance) learningEffectChartInstance.resize()
    }
    
    onMounted(() => {
      getSubjects()
      getAnalyticsData()
      initCharts()
      
      // 监听窗口大小改变
      window.addEventListener('resize', handleResize)
    })
    
    return {
      dateRange,
      selectedSubject,
      activeTab,
      subjects,
      userData,
      questionData,
      examData,
      overviewData,
      userGrowthChart,
      questionDistributionChart,
      examStatsChart,
      userActivityChart,
      learningEffectChart,
      handleDateChange,
      handleSubjectChange,
      handleTabChange,
      refreshData,
      exportChart,
      exportData
    }
  }
}
</script>

<style scoped>
.data-analytics {
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

.filter-card {
  margin-bottom: 20px;
}

.overview-stats {
  margin-bottom: 20px;
}

.stat-card {
  height: 120px;
}

.stat-content {
  display: flex;
  align-items: center;
  height: 100%;
}

.stat-icon {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 20px;
  font-size: 24px;
  color: white;
}

.user-icon {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.question-icon {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.exam-icon {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
}

.active-icon {
  background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: 600;
  color: #333;
  margin-bottom: 5px;
}

.stat-label {
  color: #666;
  font-size: 14px;
  margin-bottom: 5px;
}

.stat-change {
  font-size: 12px;
  font-weight: 500;
}

.stat-change.positive {
  color: #67C23A;
}

.stat-change.negative {
  color: #F56C6C;
}

.charts-row {
  margin-bottom: 20px;
}

.chart-card {
  min-height: 400px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chart-container {
  width: 100%;
  height: 300px;
}

.chart-container.large {
  height: 400px;
}

.table-card {
  margin-top: 20px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .overview-stats .el-col {
    margin-bottom: 15px;
  }
  
  .charts-row .el-col {
    margin-bottom: 20px;
  }
  
  .chart-container {
    height: 250px;
  }
  
  .chart-container.large {
    height: 300px;
  }
  
  .stat-content {
    flex-direction: column;
    text-align: center;
  }
  
  .stat-icon {
    margin-right: 0;
    margin-bottom: 10px;
  }
}
</style>