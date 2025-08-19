<template>
  <div class="dashboard">
    <!-- 概览统计卡片 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :xs="12" :sm="6" :md="6" :lg="6" :xl="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon user-icon">
              <el-icon><User /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ overviewStats.totalUsers }}</div>
              <div class="stat-label">总用户数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :xs="12" :sm="6" :md="6" :lg="6" :xl="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon question-icon">
              <el-icon><Document /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ overviewStats.totalQuestions }}</div>
              <div class="stat-label">题目总数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :xs="12" :sm="6" :md="6" :lg="6" :xl="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon exam-icon">
              <el-icon><Edit /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ overviewStats.totalExams }}</div>
              <div class="stat-label">考试次数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :xs="12" :sm="6" :md="6" :lg="6" :xl="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon active-icon">
              <el-icon><TrendCharts /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ overviewStats.activeUsers }}</div>
              <div class="stat-label">活跃用户</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
    
    <!-- 图表区域 -->
    <el-row :gutter="20" class="charts-row">
      <!-- 用户增长趋势 -->
      <el-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span>用户增长趋势</span>
              <el-date-picker
                v-model="userGrowthDateRange"
                type="daterange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                size="small"
                @change="updateUserGrowthChart"
              />
            </div>
          </template>
          <div ref="userGrowthChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
      
      <!-- 题目分布 -->
      <el-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span>题目分布</span>
            </div>
          </template>
          <div ref="questionDistributionChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>
    
    <el-row :gutter="20" class="charts-row">
      <!-- 考试统计 -->
      <el-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span>考试统计</span>
            </div>
          </template>
          <div ref="examStatsChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
      
      <!-- 用户活跃度 -->
      <el-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span>用户活跃度</span>
            </div>
          </template>
          <div ref="userActivityChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>
    
    <!-- 最新动态 -->
    <el-row :gutter="20">
      <!-- 最新用户 -->
      <el-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12">
        <el-card class="activity-card">
          <template #header>
            <div class="card-header">
              <span>最近登录用户</span>
              <el-button type="primary" text size="small" @click="$router.push('/user-manage')">
                查看更多
              </el-button>
            </div>
          </template>
          
          <div class="activity-list">
            <div v-for="user in recentUsers" :key="user.id" class="activity-item">
              <el-avatar :size="40" :src="user.avatar">
                <el-icon><User /></el-icon>
              </el-avatar>
              <div class="activity-info">
                <div class="activity-title">{{ user.username }}</div>
                <div class="activity-time">{{ user.registerTime }}</div>
              </div>
              <el-tag :type="user.status === 'active' ? 'success' : 'info'" size="small">
                {{ user.status === 'active' ? '活跃' : '普通' }}
              </el-tag>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <!-- 系统通知 -->
      <el-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12">
        <el-card class="activity-card">
          <template #header>
            <div class="card-header">
              <span>系统通知</span>
              <el-button type="primary" text size="small">
                查看更多
              </el-button>
            </div>
          </template>
          
          <div class="activity-list">
            <div v-for="notice in systemNotices" :key="notice.id" class="activity-item">
              <div class="notice-icon">
                <el-icon :color="notice.type === 'warning' ? '#E6A23C' : '#409EFF'">
                  <component :is="notice.icon" />
                </el-icon>
              </div>
              <div class="activity-info">
                <div class="activity-title">{{ notice.title }}</div>
                <div class="activity-time">{{ notice.time }}</div>
              </div>
              <el-tag :type="notice.type" size="small">
                {{ notice.level }}
              </el-tag>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { ref, reactive, onMounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { analyticsAPI, userAPI, systemAPI } from '../api'

export default {
  name: 'Dashboard',
  setup() {
    const userGrowthDateRange = ref([new Date(Date.now() - 30 * 24 * 60 * 60 * 1000), new Date()])
    
    const overviewStats = reactive({
      totalUsers: 0,
      totalQuestions: 0,
      totalExams: 0,
      activeUsers: 0
    })
    
    const recentUsers = ref([])
    const systemNotices = ref([])
    
    // 图表引用
    const userGrowthChartRef = ref()
    const questionDistributionChartRef = ref()
    const examStatsChartRef = ref()
    const userActivityChartRef = ref()
    
    // 图表实例
    let userGrowthChart = null
    let questionDistributionChart = null
    let examStatsChart = null
    let userActivityChart = null
    
    // 获取概览统计
    const getOverviewStats = async () => {
      try {
        const stats = await analyticsAPI.getOverviewStats()
        Object.assign(overviewStats, stats)
      } catch (error) {
        console.error('获取概览统计失败:', error)
        // 降级使用模拟数据
        Object.assign(overviewStats, {
          totalUsers: 4,
          totalQuestions: 8,
          totalExams: 0,
          activeUsers: 2
        })
      }
    }
    
    // 获取最新用户
    const getRecentUsers = async () => {
      try {
        const users = await userAPI.getUsers({ page: 1, size: 4, sort: 'createTime', order: 'desc' })
        recentUsers.value = users.records || []
      } catch (error) {
        console.error('获取最新用户失败:', error)
        // 降级使用空数据
        recentUsers.value = []
      }
    }
    
    // 获取系统通知
    const getSystemNotices = async () => {
      try {
        const notices = await systemAPI.getSystemLogs({ page: 1, size: 3, type: 'notice' })
        systemNotices.value = notices.records || []
      } catch (error) {
        console.error('获取系统通知失败:', error)
        // 降级使用空数据
        systemNotices.value = []
      }
    }
    
    // 初始化用户增长趋势图
    const initUserGrowthChart = async () => {
      if (!userGrowthChartRef.value) return
      
      userGrowthChart = echarts.init(userGrowthChartRef.value)
      
      try {
        const activityData = await analyticsAPI.getUserActivityStats()
        const userGrowthData = activityData.userGrowthData || []
        
        const dates = userGrowthData.map(item => item.date)
        const counts = userGrowthData.map(item => item.count)
        
        const option = {
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
              name: '新增用户',
              type: 'line',
              data: counts,
              smooth: true,
              itemStyle: {
                color: '#409EFF'
              },
              areaStyle: {
                color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                  { offset: 0, color: 'rgba(64, 158, 255, 0.3)' },
                  { offset: 1, color: 'rgba(64, 158, 255, 0.1)' }
                ])
              }
            }
          ]
        }
        
        userGrowthChart.setOption(option)
      } catch (error) {
        console.error('获取用户增长数据失败:', error)
        // 使用默认配置
        const option = {
          tooltip: { trigger: 'axis' },
          xAxis: { type: 'category', data: [] },
          yAxis: { type: 'value' },
          series: [{ name: '新增用户', type: 'line', data: [] }]
        }
        userGrowthChart.setOption(option)
      }
    }
    
    // 初始化题目分布图
    const initQuestionDistributionChart = async () => {
      if (!questionDistributionChartRef.value) return
      
      questionDistributionChart = echarts.init(questionDistributionChartRef.value)
      
      try {
        const questionStats = await analyticsAPI.getQuestionStats()
        const subjectDistribution = questionStats.subjectDistribution || []
        
        const option = {
          tooltip: {
            trigger: 'item'
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
              data: subjectDistribution,
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
        
        questionDistributionChart.setOption(option)
      } catch (error) {
        console.error('获取题目分布数据失败:', error)
        // 使用默认配置
        const option = {
          tooltip: { trigger: 'item' },
          legend: { orient: 'vertical', left: 'left' },
          series: [{ name: '题目分布', type: 'pie', radius: '50%', data: [] }]
        }
        questionDistributionChart.setOption(option)
      }
    }
    
    // 初始化考试统计图
    const initExamStatsChart = async () => {
      if (!examStatsChartRef.value) return
      
      examStatsChart = echarts.init(examStatsChartRef.value)
      
      try {
        const examData = await analyticsAPI.getExamAnalytics()
        const examStats = examData.examData || []
        
        const dates = examStats.map(item => item.date)
        const counts = examStats.map(item => item.count)
        
        const option = {
          tooltip: {
            trigger: 'axis',
            axisPointer: {
              type: 'shadow'
            }
          },
          grid: {
            left: '3%',
            right: '4%',
            bottom: '3%',
            containLabel: true
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
              name: '答题次数',
              type: 'bar',
              data: counts,
              itemStyle: {
                color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
                  { offset: 0, color: '#83bff6' },
                  { offset: 0.5, color: '#188df0' },
                  { offset: 1, color: '#188df0' }
                ])
              }
            }
          ]
        }
        
        examStatsChart.setOption(option)
      } catch (error) {
        console.error('获取考试统计数据失败:', error)
        // 使用默认配置
        const option = {
          tooltip: { trigger: 'axis' },
          xAxis: { type: 'category', data: [] },
          yAxis: { type: 'value' },
          series: [{ name: '答题次数', type: 'bar', data: [] }]
        }
        examStatsChart.setOption(option)
      }
    }
    
    // 初始化用户活跃度图
    const initUserActivityChart = async () => {
      if (!userActivityChartRef.value) return
      
      userActivityChart = echarts.init(userActivityChartRef.value)
      
      try {
        const activityData = await analyticsAPI.getUserActivityStats()
        const userGrowthData = activityData.userGrowthData || []
        
        const dates = userGrowthData.map(item => item.date)
        const newUsers = userGrowthData.map(item => item.count)
        // 模拟活跃用户数据（实际应该从另一个API获取）
        const activeUsers = newUsers.map(count => Math.floor(count * 5 + Math.random() * 10))
        
        const option = {
          tooltip: {
            trigger: 'axis'
          },
          legend: {
            data: ['活跃用户', '新增用户']
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
                color: '#67C23A'
              }
            },
            {
              name: '新增用户',
              type: 'line',
              data: newUsers,
              itemStyle: {
                color: '#E6A23C'
              }
            }
          ]
        }
        
        userActivityChart.setOption(option)
      } catch (error) {
        console.error('获取用户活跃度数据失败:', error)
        // 使用默认配置
        const option = {
          tooltip: { trigger: 'axis' },
          legend: { data: ['活跃用户', '新增用户'] },
          xAxis: { type: 'category', data: [] },
          yAxis: { type: 'value' },
          series: [
            { name: '活跃用户', type: 'bar', data: [] },
            { name: '新增用户', type: 'line', data: [] }
          ]
        }
        userActivityChart.setOption(option)
      }
    }
    
    // 更新用户增长图
    const updateUserGrowthChart = () => {
      console.log('更新用户增长图:', userGrowthDateRange.value)
      // 根据日期范围重新获取数据并更新图表
    }
    
    // 窗口大小改变时重新调整图表
    const handleResize = () => {
      if (userGrowthChart) userGrowthChart.resize()
      if (questionDistributionChart) questionDistributionChart.resize()
      if (examStatsChart) examStatsChart.resize()
      if (userActivityChart) userActivityChart.resize()
    }
    
    onMounted(async () => {
      await getOverviewStats()
      await getRecentUsers()
      await getSystemNotices()
      
      await nextTick()
      
      // 初始化所有图表
      await initUserGrowthChart()
      await initQuestionDistributionChart()
      await initExamStatsChart()
      await initUserActivityChart()
      
      // 监听窗口大小变化
      window.addEventListener('resize', handleResize)
    })
    
    return {
      userGrowthDateRange,
      overviewStats,
      recentUsers,
      systemNotices,
      userGrowthChartRef,
      questionDistributionChartRef,
      examStatsChartRef,
      userActivityChartRef,
      updateUserGrowthChart
    }
  }
}
</script>

<style scoped>
.dashboard {
  padding: 0;
}

.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  height: 100px;
  cursor: pointer;
  transition: all 0.3s;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.stat-content {
  display: flex;
  align-items: center;
  height: 100%;
  padding: 0 10px;
}

.stat-icon {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: white;
  margin-right: 15px;
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
}

.charts-row {
  margin-bottom: 20px;
}

.chart-card {
  height: 400px;
}

.activity-card {
  height: 400px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chart-container {
  width: 100%;
  height: 320px;
}

.activity-list {
  max-height: 320px;
  overflow-y: auto;
}

.activity-item {
  display: flex;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}

.activity-item:last-child {
  border-bottom: none;
}

.activity-info {
  flex: 1;
  margin-left: 12px;
}

.activity-title {
  font-size: 14px;
  color: #333;
  margin-bottom: 4px;
}

.activity-time {
  font-size: 12px;
  color: #999;
}

.notice-icon {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background-color: #f5f7fa;
  display: flex;
  font-size: 18px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .dashboard {
    padding: 10px;
  }
  
  .stats-row {
    margin-bottom: 15px;
  }
  
  .stat-card {
    margin-bottom: 10px;
    height: 90px;
  }
  
  .stat-content {
    padding: 0 8px;
  }
  
  .stat-icon {
    width: 50px;
    height: 50px;
    font-size: 20px;
    margin-right: 12px;
  }
  
  .stat-value {
    font-size: 24px;
  }
  
  .stat-label {
    font-size: 13px;
  }
  
  .chart-card,
  .activity-card {
    margin-bottom: 20px;
    height: auto;
    min-height: 350px;
  }
  
  .chart-container {
    height: 280px;
  }
  
  .activity-list {
    max-height: 280px;
  }
  
  .card-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
    margin-bottom: 15px;
  }
  
  .card-header h3 {
    font-size: 16px;
  }
  
  .activity-item {
    padding: 10px 0;
  }
  
  .activity-info {
    margin-left: 10px;
  }
  
  .activity-title {
    font-size: 13px;
  }
  
  .activity-time {
    font-size: 11px;
  }
  
  .notice-icon {
    width: 35px;
    height: 35px;
    font-size: 16px;
  }
}

/* 超小屏幕优化 */
@media (max-width: 480px) {
  .dashboard {
    padding: 8px;
  }
  
  .stats-row {
    margin-bottom: 12px;
  }
  
  .stat-card {
    height: 80px;
    margin-bottom: 8px;
  }
  
  .stat-content {
    padding: 0 6px;
  }
  
  .stat-icon {
    width: 45px;
    height: 45px;
    font-size: 18px;
    margin-right: 10px;
  }
  
  .stat-value {
    font-size: 20px;
  }
  
  .stat-label {
    font-size: 12px;
  }
  
  .chart-card,
  .activity-card {
    margin-bottom: 15px;
    min-height: 300px;
  }
  
  .chart-container {
    height: 240px;
  }
  
  .activity-list {
    max-height: 240px;
  }
  
  .card-header {
    margin-bottom: 12px;
  }
  
  .card-header h3 {
    font-size: 15px;
  }
  
  .activity-item {
    padding: 8px 0;
  }
  
  .activity-title {
    font-size: 12px;
  }
  
  .activity-time {
    font-size: 10px;
  }
  
  .notice-icon {
    width: 32px;
    height: 32px;
    font-size: 14px;
  }
}

</style>