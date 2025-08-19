<template>
  <div class="analytics-page">
    <div class="analytics-header">
        <h2>数据分析</h2>
        <p>可视化您的学习进度和成果</p>
      </div>

      <!-- 统一加载状态和错误处理 - 只在有问题时显示 -->
      <LoadingStates
        v-if="initialLoading || refreshing || networkError || timeoutError || hasError || (!loading && overviewStats.totalQuestions === 0)"
        :initial-loading="initialLoading"
        :refreshing="refreshing"
        :network-error="networkError"
        :timeout-error="timeoutError"
        :server-error="hasError"
        :error-message="hasError ? '数据加载失败' : ''"
        :is-empty="!loading && overviewStats.totalQuestions === 0"
        empty-title="暂无学习数据"
        empty-description="开始练习后，这里将显示您的学习分析"
        :show-cache-status="true"
        :cache-status="cacheStatus"
        @retry="handleRetry"
        @refresh="refreshAllData"
        @report-error="reportError"
        @empty-action="goToPractice"
      />

      <!-- 主要内容区域 - 只在数据正常加载完成时显示 -->
      <div v-if="!initialLoading && !refreshing && !networkError && !timeoutError && !hasError && overviewStats.totalQuestions > 0">
        <!-- 时间范围选择 -->
        <t-card class="filter-card">
        <t-row :gutter="24">
          <t-col :span="9">
            <div style="display: flex; gap: 8px; align-items: center; width: 100%;">
              <t-date-picker
                v-model="startDate"
                format="YYYY-MM-DD"
                placeholder="开始日期"
                :disable-date="disableStartDate"
                clearable
                @change="onStartDateChange"
                style="flex: 1;"
              />
              <span style="color: #999; font-size: 12px;">至</span>
              <t-date-picker
                v-model="endDate"
                format="YYYY-MM-DD"
                placeholder="结束日期"
                :disable-date="disableEndDate"
                clearable
                @change="onEndDateChange"
                style="flex: 1;"
              />
            </div>
          </t-col>
          <t-col :span="9">
            <Multiselect
              v-model="selectedSubject"
              :options="[
                { id: '', name: '全部科目' },
                ...subjects
              ]"
              value-prop="id"
              label="name"
              placeholder="选择科目"
              :can-clear="true"
              @change="updateCharts"
              :classes="{
                container: 'analytics-multiselect-container',
                dropdown: 'analytics-select-dropdown'
              }"
              style="width: 100%;"
            />
          </t-col>
          <t-col :span="6">
            <t-button theme="primary" @click="exportReport" style="width: 100%;">导出所有报告</t-button>
          </t-col>
        </t-row>
      </t-card>

      <!-- 概览统计 -->
      <t-row :gutter="20" class="overview-stats">
        <t-col :span="6">
          <t-card class="stat-card">
            <div class="stat-content">
              <div class="stat-icon">
                <span class="icon">✏️</span>
              </div>
              <div class="stat-info">
                <div class="stat-value">{{ overviewStats.totalQuestions }}</div>
                <div class="stat-label">总练习题数</div>
              </div>
            </div>
          </t-card>
        </t-col>
        <t-col :span="6">
          <t-card class="stat-card">
            <div class="stat-content">
              <div class="stat-icon">
                <span class="icon">✅</span>
              </div>
              <div class="stat-info">
                <div class="stat-value">{{ overviewStats.correctRate }}%</div>
                <div class="stat-label">平均正确率</div>
              </div>
            </div>
          </t-card>
        </t-col>
        <t-col :span="6">
          <t-card class="stat-card">
            <div class="stat-content">
              <div class="stat-icon">
                <span class="icon">📅</span>
              </div>
              <div class="stat-info">
                <div class="stat-value">{{ overviewStats.studyDays }}</div>
                <div class="stat-label">学习天数</div>
              </div>
            </div>
          </t-card>
        </t-col>
        <t-col :span="6">
          <t-card class="stat-card">
            <div class="stat-content">
              <div class="stat-icon">
                <span class="icon">🕐</span>
              </div>
              <div class="stat-info">
                <div class="stat-value">{{ overviewStats.studyTime }}</div>
                <div class="stat-label">学习时长(小时)</div>
              </div>
            </div>
          </t-card>
        </t-col>
      </t-row>

      <!-- 图表区域 -->
      <t-row :gutter="20">
        <!-- 学习趋势图 -->
        <t-col :span="12">
          <t-card class="chart-card">
            <template #header>
              <div class="card-header">
                <span>学习趋势</span>
                <div>
                  <t-radio-group v-model="trendType" size="small" @change="updateTrendChart">
                    <t-radio-button label="daily">按天</t-radio-button>
                  <t-radio-button label="weekly">按周</t-radio-button>
                  <t-radio-button label="monthly">按月</t-radio-button>
                </t-radio-group>
                <t-button size="small" variant="outline" @click="exportSingleChart('trend')" style="margin-left: 10px;">导出图表</t-button>
                </div>
              </div>
            </template>
            <div ref="trendChartRef" class="chart-container"></div>
          </t-card>
        </t-col>

        <!-- 正确率分析 -->
        <t-col :span="12">
          <t-card class="chart-card">
            <template #header>
              <div class="card-header">
                <span>正确率分析</span>
                <t-button size="small" variant="outline" @click="exportSingleChart('accuracy')" style="margin-left: 10px;">导出图表</t-button>
              </div>
            </template>
            <div ref="accuracyChartRef" class="chart-container"></div>
          </t-card>
        </t-col>
      </t-row>

      <t-row :gutter="20">
        <!-- 科目分布 -->
        <t-col :span="12">
          <t-card class="chart-card">
            <template #header>
              <div class="card-header">
                <span>科目练习分布</span>
                <t-button size="small" variant="outline" @click="exportSingleChart('subject')" style="margin-left: 10px;">导出图表</t-button>
              </div>
            </template>
            <div ref="subjectChartRef" class="chart-container"></div>
          </t-card>
        </t-col>

        <!-- 难度分析 -->
        <t-col :span="12">
          <t-card class="chart-card">
            <template #header>
              <div class="card-header">
                <span>难度分析</span>
                <t-button size="small" variant="outline" @click="exportSingleChart('difficulty')" style="margin-left: 10px;">导出图表</t-button>
              </div>
            </template>
            <div ref="difficultyChartRef" class="chart-container"></div>
          </t-card>
        </t-col>
      </t-row>

      <!-- 错题分析 -->
      <t-row :gutter="20">
        <t-col :span="12">
          <t-card class="chart-card">
            <template #header>
              <div class="card-header">
                <span>错题分析</span>
                <t-button size="small" variant="outline" @click="exportSingleChart('wrong')" style="margin-left: 10px;">导出图表</t-button>
              </div>
            </template>
            <div ref="wrongChartRef" class="chart-container"></div>
          </t-card>
        </t-col>
      </t-row>

      <!-- 学习建议 -->
      <t-card class="suggestion-card">
        <template #header>
          <div class="card-header">
            <span>学习建议</span>
          </div>
        </template>

        <div class="suggestions">
          <div v-for="(suggestion, index) in suggestions" :key="index" class="suggestion-item">
            <t-icon class="suggestion-icon" :color="suggestion.color">
              <component :is="suggestion.icon" />
            </t-icon>
            <div class="suggestion-content">
              <h4>{{ suggestion.title }}</h4>
              <p>{{ suggestion.content }}</p>
          </div>
        </div>
        </div> <!-- 闭合主要内容区域 -->
      </t-card>
      </div>
  </div>
</template>

<script>
import { ref, reactive, onMounted, onUnmounted, nextTick, computed } from 'vue'
import { useRouter } from 'vue-router'
import { MessagePlugin } from 'tdesign-vue-next'
import * as echarts from 'echarts'
import { useAnalyticsStore } from '@/store/analytics'
import { subjectAPI, statisticsAPI } from '../api'
import LoadingStates from '@/components/LoadingStates.vue'
import Multiselect from '@vueform/multiselect'
import JSZip from 'jszip'

export default {
  name: 'Analytics',
  components: {
    Multiselect,
    LoadingStates
  },
  setup() {
    const router = useRouter()
    
    // 使用缓存store
    const analyticsStore = useAnalyticsStore()

    const startDate = ref(new Date(Date.now() - 30 * 24 * 60 * 60 * 1000))
    const endDate = ref(new Date())
    const dateRange = ref([new Date(Date.now() - 30 * 24 * 60 * 60 * 1000), new Date()])
    const selectedSubject = ref('')
    const trendType = ref('daily')

    // 从store获取数据，确保有默认值
    const subjects = ref([])
    const loading = computed(() => analyticsStore.loading || false)
    const overviewStats = computed(() => {
      const stats = analyticsStore.studyStats || {}
      return {
        totalQuestions: stats.totalQuestions || 0,
        correctRate: stats.correctRate || 0,
        studyDays: stats.studyDays || 0,
        studyTime: stats.studyTime || 0
      }
    })
    const suggestions = computed(() => analyticsStore.studyRecommendations || [])
    
    // 加载状态和错误处理
    const initialLoading = computed(() => analyticsStore.initialLoading || false)
    const refreshing = computed(() => analyticsStore.refreshing || false)
    const networkError = computed(() => analyticsStore.networkError || false)
    const timeoutError = computed(() => analyticsStore.timeoutError || false)
    const hasError = computed(() => analyticsStore.hasError || false)
    const errorHandler = analyticsStore.errorHandler
    
    // 缓存状态
    const cacheStatus = ref('fresh')

    // 图表引用
    const trendChartRef = ref()
    const accuracyChartRef = ref()
    const subjectChartRef = ref()
    const difficultyChartRef = ref()
    const wrongChartRef = ref()

    // 图表实例
    let trendChart = null
    let accuracyChart = null
    let subjectChart = null
    let difficultyChart = null
    let wrongChart = null



    // 获取科目列表 - 使用store方法
    const getSubjects = async () => {
      try {
        const response = await subjectAPI.getEnabledSubjects()
        if (Array.isArray(response)) {
          subjects.value = response
        } else if (response.code === 200 && Array.isArray(response.data)) {
          subjects.value = response.data
        } else {
          console.error('获取科目数据格式错误:', response)
          subjects.value = []
        }
      } catch (error) {
        console.error('获取科目列表失败:', error)
        subjects.value = []
      }
    }
    // 确保日期是有效的 Date 对象
    const ensureDate = (date) => {
      if (!date) return null
      if (date instanceof Date) return date
      if (typeof date === 'string') {
        const parsed = new Date(date)
        return isNaN(parsed) ? null : parsed
      }
      return null
    }

    // 统一构造后端需要的 dateRange 字符串（YYYY-MM-DD,YYYY-MM-DD）
    const buildDateRangeParam = () => {
      if (!Array.isArray(dateRange.value) || dateRange.value.length !== 2) return ''
      const [start, end] = dateRange.value
      const startDate = ensureDate(start)
      const endDate = ensureDate(end)
      if (!startDate || !endDate) return ''
      return `${formatDate(startDate)},${formatDate(endDate)}`
    }


    // 格式化日期为 yyyy-MM-dd 格式
    const formatDate = (date) => {
      if (!date) return ''
      if (typeof date === 'string') return date
      const year = date.getFullYear()
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      return `${year}-${month}-${day}`
    }

    // 禁用开始日期：不能选择未来日期
    const disableStartDate = (current) => {
      const cur = ensureDate(current)
      if (!cur) return false

      const today = new Date()
      const endOfToday = new Date(today.getFullYear(), today.getMonth(), today.getDate(), 23, 59, 59)
      return cur.getTime() > endOfToday.getTime()
    }

    // 禁用结束日期：不能选择未来日期，且不能早于开始日期
    const disableEndDate = (current) => {
      const cur = ensureDate(current)
      if (!cur) return false

      const today = new Date()
      const endOfToday = new Date(today.getFullYear(), today.getMonth(), today.getDate(), 23, 59, 59)
      // 禁用未来日期
      if (cur.getTime() > endOfToday.getTime()) return true

      // 如果有开始日期，禁用早于开始日期的日期
      const startDateVal = ensureDate(startDate.value)
      if (startDateVal) {
        const start = new Date(startDateVal.getFullYear(), startDateVal.getMonth(), startDateVal.getDate())
        return cur.getTime() < start.getTime()
      }
      return false
    }

    // 禁用未来日期（保留原函数以兼容）
    const disableDate = disableStartDate

    // 获取概览统计
    // 开始日期变更
    const onStartDateChange = async (val) => {
      const dateVal = ensureDate(val)
      if (!dateVal) {
        console.warn('Invalid start date:', val)
        return
      }

      startDate.value = dateVal
      // 如果开始日期晚于结束日期，自动调整结束日期
      const endDateVal = ensureDate(endDate.value)
      if (endDateVal && dateVal.getTime() > endDateVal.getTime()) {
        MessagePlugin.warning('开始日期不能晚于结束日期，已为您自动调整结束日期')
        endDate.value = new Date(dateVal)
      }
      // 更新 dateRange 以保持兼容性
      dateRange.value = [startDate.value, endDate.value]
      await updateCharts()
    }

    // 结束日期变更
    const onEndDateChange = async (val) => {
      const dateVal = ensureDate(val)
      if (!dateVal) {
        console.warn('Invalid end date:', val)
        return
      }

      const today = new Date()
      const endOfToday = new Date(today.getFullYear(), today.getMonth(), today.getDate(), 23, 59, 59)

      // 校验：结束日期不得超过今天
      if (dateVal.getTime() > endOfToday.getTime()) {
        MessagePlugin.warning('结束日期不能超过今天，已为您重置为今天')
        const todayDate = new Date(today.getFullYear(), today.getMonth(), today.getDate())
        endDate.value = todayDate
      } else {
        endDate.value = dateVal
      }

      // 如果结束日期早于开始日期，自动调整开始日期
      const startDateVal = ensureDate(startDate.value)
      if (startDateVal && startDateVal.getTime() > endDate.value.getTime()) {
        MessagePlugin.warning('结束日期不能早于开始日期，已为您自动调整开始日期')
        startDate.value = new Date(endDate.value)
      }

      // 更新 dateRange 以保持兼容性
      dateRange.value = [startDate.value, endDate.value]
      await updateCharts()
    }

    // 日期范围变更（保留以兼容现有代码）
    const onDateRangeChange = async (val) => {
      if (Array.isArray(val) && val.length === 2) {
        startDate.value = val[0]
        endDate.value = val[1]
        dateRange.value = val
        await updateCharts()
      }
    }

    const getOverviewStats = async () => {
      try {
        const stats = await statisticsAPI.getOverviewStats({
          dateRange: buildDateRangeParam(),
          subjectId: selectedSubject.value
        })
        Object.assign(overviewStats, stats)
      } catch (error) {
        console.error('获取概览统计失败:', error)
        // 使用默认数据作为后备
        Object.assign(overviewStats, {
          totalQuestions: 0,
          correctRate: 0,
          studyDays: 0,
          studyTime: 0
        })
      }
    }

    // 初始化学习趋势图
    const initTrendChart = async () => {
      if (!trendChartRef.value) return

      try {
        const trendData = await statisticsAPI.getTrendData({
           dateRange: buildDateRangeParam(),
           trendType: trendType.value,
           subjectId: selectedSubject.value
         })

        trendChart = echarts.init(trendChartRef.value)

        const option = {
          tooltip: {
            trigger: 'axis'
          },
          legend: {
            data: ['练习题数', '正确率']
          },
          xAxis: {
            type: 'category',
            data: trendData.categories || []
          },
          yAxis: [
            {
              type: 'value',
              name: '题数',
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
              name: '练习题数',
              type: 'bar',
              data: trendData.practiceData || [],
              itemStyle: {
                color: '#409EFF'
              }
            },
            {
              name: '正确率',
              type: 'line',
              yAxisIndex: 1,
              data: trendData.accuracyData || [],
              itemStyle: {
                color: '#67C23A'
              }
            }
          ]
        }

        trendChart.setOption(option)
      } catch (error) {
        console.error('获取学习趋势数据失败:', error)
        // 使用默认数据作为后备
        trendChart = echarts.init(trendChartRef.value)
        const defaultOption = {
          tooltip: { trigger: 'axis' },
          legend: { data: ['练习题数', '正确率'] },
          xAxis: { type: 'category', data: [] },
          yAxis: [{ type: 'value', name: '题数' }, { type: 'value', name: '正确率(%)' }],
          series: [{ name: '练习题数', type: 'bar', data: [] }, { name: '正确率', type: 'line', data: [] }]
        }
        trendChart.setOption(defaultOption)
      }
    }

    // 初始化正确率分析图
    const initAccuracyChart = async () => {
      if (!accuracyChartRef.value) return

      accuracyChart = echarts.init(accuracyChartRef.value)

      try {
        const accuracyData = await statisticsAPI.getAccuracyAnalysis({
           dateRange: buildDateRangeParam(),
           subjectId: selectedSubject.value
         })

        // 处理后端返回的数据结构
        const distribution = accuracyData?.distribution || {}
        const pieData = Object.keys(distribution).map(key => ({
          name: key,
          value: distribution[key] || 0
        }))

        // 如果没有数据，使用默认结构
        const finalPieData = pieData.length > 0 ? pieData : [
          { name: '暂无数据', value: 1 }
        ]

        const option = {
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
                name: '正确率分布',
                type: 'pie',
                radius: '50%',
                data: finalPieData,
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

        accuracyChart.setOption(option)
      } catch (error) {
        console.error('获取正确率分析数据失败:', error)
        // 使用默认数据作为后备
        const defaultOption = {
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
              name: '正确率分布',
              type: 'pie',
              radius: '50%',
              data: [
                { name: '暂无数据', value: 1 }
              ]
            }
          ]
        }
        accuracyChart.setOption(defaultOption)
      }
    }

    // 初始化科目分布图
    const initSubjectChart = async () => {
      if (!subjectChartRef.value) return

      subjectChart = echarts.init(subjectChartRef.value)

      try {
        const subjectData = await statisticsAPI.getSubjectDistribution({
           dateRange: buildDateRangeParam(),
           subjectId: selectedSubject.value || undefined
         })

        // 确保数据格式正确
        const distribution = subjectData?.distribution || {}
        const subjects = Object.keys(distribution)
        const counts = Object.values(distribution)

        // 如果没有数据，使用默认数据
        const finalSubjects = subjects.length > 0 ? subjects : ['暂无数据']
        const finalCounts = counts.length > 0 ? counts : [0]

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
            type: 'value'
          },
          yAxis: {
            type: 'category',
            data: finalSubjects
          },
          series: [
            {
              name: '练习题数',
              type: 'bar',
              data: finalCounts,
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

        subjectChart.setOption(option)
      } catch (error) {
        console.error('获取科目分布数据失败:', error)
        // 使用默认数据作为后备
        const defaultOption = {
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
            type: 'value'
          },
          yAxis: {
            type: 'category',
            data: ['暂无数据']
          },
          series: [
            {
              name: '练习题数',
              type: 'bar',
              data: [0],
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
        subjectChart.setOption(defaultOption)
      }
    }

    // 初始化难度分析图
    const initDifficultyChart = async () => {
      if (!difficultyChartRef.value) return

      difficultyChart = echarts.init(difficultyChartRef.value)

      try {
        const difficultyData = await statisticsAPI.getDifficultyAnalysis({
           dateRange: buildDateRangeParam(),
           subjectId: selectedSubject.value
         })

        // 确保数据格式正确
        const distribution = difficultyData?.distribution || {}
        const pieData = Object.keys(distribution).map(key => ({
          name: key,
          value: distribution[key] || 0
        }))

        // 如果没有数据，使用默认数据
        const finalData = pieData.length > 0 ? pieData : [
          { name: '简单', value: 0 },
          { name: '中等', value: 0 },
          { name: '困难', value: 0 }
        ]

        const option = {
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
              name: '难度分布',
              type: 'pie',
              radius: ['40%', '70%'],
              avoidLabelOverlap: false,
              data: finalData,
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

        difficultyChart.setOption(option)
      } catch (error) {
        console.error('获取难度分析数据失败:', error)
        // 使用默认数据作为后备
        const defaultOption = {
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
              name: '难度分布',
              type: 'pie',
              radius: ['40%', '70%'],
              avoidLabelOverlap: false,
              data: [
                { name: '暂无数据', value: 1 }
              ]
            }
          ]
        }
        difficultyChart.setOption(defaultOption)
      }
    }

    // 初始化错题分析图
    const initWrongChart = async () => {
      if (!wrongChartRef.value) return

      wrongChart = echarts.init(wrongChartRef.value)

      try {
        const wrongData = await statisticsAPI.getWrongQuestionAnalysis({
           dateRange: buildDateRangeParam(),
           subjectId: selectedSubject.value || undefined
         })

        // 确保数据格式正确
        const trend = wrongData?.trend || {}
        const dates = Object.keys(trend)
        const counts = Object.values(trend)

        // 如果没有数据，使用默认数据
        const finalDates = dates.length > 0 ? dates : ['暂无数据']
        const finalCounts = counts.length > 0 ? counts : [0]

        const option = {
          tooltip: {
            trigger: 'axis'
          },
          legend: {
            data: ['错题数量']
          },
          xAxis: {
            type: 'category',
            data: finalDates
          },
          yAxis: {
            type: 'value'
          },
          series: [
            {
              name: '错题数量',
              type: 'line',
              data: finalCounts,
              smooth: true,
              itemStyle: {
                color: '#ff6b6b'
              }
            }
          ]
        }

        wrongChart.setOption(option)
      } catch (error) {
        console.error('获取错题分析数据失败:', error)
        // 显示用户友好的错误提示
        MessagePlugin.warning('错题分析数据加载失败，显示默认数据')
        // 使用默认数据作为后备
        const defaultOption = {
          tooltip: {
            trigger: 'axis'
          },
          legend: {
            data: ['错题数量']
          },
          xAxis: {
            type: 'category',
            data: ['暂无数据']
          },
          yAxis: {
            type: 'value'
          },
          series: [
            {
              name: '错题数量',
              type: 'line',
              data: [0],
              smooth: true,
              itemStyle: {
                color: '#ff6b6b'
              }
            }
          ]
        }
        wrongChart.setOption(defaultOption)
      }
    }

    // 更新趋势图
    const updateTrendChart = async () => {
      if (!trendChart) return

      try {
        const trendData = await statisticsAPI.getTrendData({
           dateRange: buildDateRangeParam(),
           trendType: trendType.value,
           subjectId: selectedSubject.value
         })

        trendChart.setOption({
          xAxis: {
            data: trendData.categories || []
          },
          series: [
            {
              data: trendData.practiceData || []
            },
            {
              data: trendData.accuracyData || []
            }
          ]
        })
      } catch (error) {
        console.error('更新趋势图失败:', error)
      }
    }

    // 更新所有图表
    const updateCharts = async () => {
      await getOverviewStats()
      await updateTrendChart()
      await initAccuracyChart()
      await initSubjectChart()
      await initDifficultyChart()
      await initWrongChart()
      await getSuggestions()
    }

    // 获取学习建议
    const getSuggestions = async () => {
      try {
        const data = await statisticsAPI.getLearningSuggestions({
          dateRange: buildDateRangeParam(),
          subjectId: selectedSubject.value
        })
        suggestions.value = data
      } catch (error) {
        console.error('获取学习建议失败:', error)
        // 使用默认建议作为后备
        suggestions.value = [
          {
            icon: 'TrendCharts',
            color: '#67C23A',
            title: '学习进度良好',
            content: '您的学习进度保持稳定，建议继续保持当前的学习节奏。'
          },
          {
            icon: 'Warning',
            color: '#E6A23C',
            title: '错题率偏高',
            content: '数据结构和算法的错题率较高，建议加强这两个科目的练习。'
          },
          {
            icon: 'Clock',
            color: '#409EFF',
            title: '学习时间建议',
            content: '建议每天保持1-2小时的学习时间，有助于知识的巩固。'
          }
        ]
      }
    }

    // 导出单个图表
    const exportSingleChart = (chartType) => {
      try {
        let chartInstance = null
        let chartName = ''

        switch (chartType) {
          case 'trend':
            chartInstance = trendChart
            chartName = '学习趋势'
            break
          case 'accuracy':
            chartInstance = accuracyChart
            chartName = '正确率分析'
            break
          case 'subject':
            chartInstance = subjectChart
            chartName = '科目练习分布'
            break
          case 'difficulty':
            chartInstance = difficultyChart
            chartName = '难度分析'
            break
          case 'wrong':
            chartInstance = wrongChart
            chartName = '错题分析'
            break
          default:
            MessagePlugin.warning('未知的图表类型')
            return
        }

        // 检查图表实例是否存在
        if (!chartInstance) {
          MessagePlugin.warning('图表尚未加载，请稍后再试')
          return
        }

        // 使用echarts的导出图片功能
        const url = chartInstance.getDataURL({
          type: 'png',
          pixelRatio: 2,
          backgroundColor: '#fff'
        })

        // 创建下载链接
        const link = document.createElement('a')
        link.href = url
        link.download = `${chartName}.png`
        link.click()

        MessagePlugin.success('图表导出成功')
      } catch (error) {
        console.error('导出图表失败:', error)
        MessagePlugin.error('导出图表失败，请稍后再试')
      }
    }

    // 导出报告
    const exportReport = () => {
      try {
        // 确保所有图表实例都已初始化
        if (!trendChart || !accuracyChart || !subjectChart || !difficultyChart || !wrongChart) {
          MessagePlugin.warning('图表尚未完全加载，请稍后再试')
          return
        }

        // 创建一个包含所有图表的数组
        const charts = [
          { name: '学习趋势', instance: trendChart },
          { name: '正确率分析', instance: accuracyChart },
          { name: '科目练习分布', instance: subjectChart },
          { name: '难度分析', instance: difficultyChart },
          { name: '错题分析', instance: wrongChart }
        ]

        // 创建一个zip文件对象
        const zip = new JSZip()
        const folder = zip.folder('学习分析报告')

        // 获取当前日期作为文件名的一部分
        const now = new Date()
        const dateStr = `${now.getFullYear()}${String(now.getMonth() + 1).padStart(2, '0')}${String(now.getDate()).padStart(2, '0')}`
        const timeStr = `${String(now.getHours()).padStart(2, '0')}${String(now.getMinutes()).padStart(2, '0')}`

        // 为每个图表创建一个导出任务
        const exportTasks = charts.map((chart, index) => {
          return new Promise((resolve) => {
            // 使用echarts的导出图片功能
            const url = chart.instance.getDataURL({
              type: 'png',
              pixelRatio: 2,
              backgroundColor: '#fff'
            })

            // 将base64图片数据添加到zip文件中
            const base64Data = url.split(',')[1]
            folder.file(`${index + 1}-${chart.name}-${dateStr}.png`, base64Data, { base64: true })
            resolve()
          })
        })

        // 等待所有导出任务完成
        Promise.all(exportTasks).then(() => {
          // 生成zip文件并下载
          zip.generateAsync({ type: 'blob' }).then((content) => {
            // 创建下载链接
            const link = document.createElement('a')
            link.href = URL.createObjectURL(content)
            link.download = `学习分析报告-${dateStr}${timeStr}.zip`
            link.click()

            // 释放URL对象
            setTimeout(() => {
              URL.revokeObjectURL(link.href)
            }, 100)

            MessagePlugin.success('报告导出成功')
          })
        })
      } catch (error) {
        console.error('导出报告失败:', error)
        MessagePlugin.error('导出报告失败，请稍后再试')
      }
    }



    // 窗口大小改变时重新调整图表
    const handleResize = () => {
      // 如果是 format 模式，确保 dateRange 为字符串数组
      if (Array.isArray(dateRange.value) && dateRange.value.length === 2) {
        // 保持现有值
      } else if (Array.isArray(dateRange.value)) {
        dateRange.value = [formatDate(new Date(Date.now() - 30*24*60*60*1000)), formatDate(new Date())]
      } else {
        dateRange.value = [formatDate(new Date(Date.now() - 30*24*60*60*1000)), formatDate(new Date())]
      }

      // 使用setTimeout确保DOM已经完成调整
      setTimeout(() => {
        if (trendChart) trendChart.resize()
        if (accuracyChart) accuracyChart.resize()
        if (subjectChart) subjectChart.resize()
        if (difficultyChart) difficultyChart.resize()
        if (wrongChart) wrongChart.resize()
      }, 200)
    }

    // 处理重试
    const handleRetry = async () => {
      try {
        cacheStatus.value = 'updating'
        await analyticsStore.initializeData()
        cacheStatus.value = 'fresh'
        MessagePlugin.success('数据重新加载成功')
      } catch (err) {
        cacheStatus.value = 'error'
        MessagePlugin.error('重试失败，请稍后再试')
      }
    }

    // 刷新所有数据
    const refreshAllData = async () => {
      try {
        cacheStatus.value = 'updating'
        await analyticsStore.refreshAllData()
        cacheStatus.value = 'fresh'
        MessagePlugin.success('数据刷新成功')
        
        // 重新初始化图表
        await nextTick()
        await initTrendChart()
        await initAccuracyChart()
        await initSubjectChart()
        await initDifficultyChart()
        await initWrongChart()
      } catch (err) {
        console.error('Failed to refresh analytics data:', err)
        cacheStatus.value = 'error'
        MessagePlugin.error('数据刷新失败')
      }
    }

    // 报告错误
    const reportError = () => {
      const errorInfo = errorHandler.getErrorStats()
      console.log('Error report:', errorInfo)
      MessagePlugin.info('错误报告已记录，感谢您的反馈')
    }

    // 跳转到练习页面
    const goToPractice = () => {
      router.push('/practice')
    }

    // 组件挂载时初始化store和图表
    onMounted(async () => {
      try {
        // 初始化默认日期范围为最近30天
        const todayInit = new Date()
        const startInit = new Date(Date.now() - 30 * 24 * 60 * 60 * 1000)

        // 同步设置所有日期相关的 ref
        startDate.value = startInit
        endDate.value = todayInit
        dateRange.value = [startInit, todayInit]

        cacheStatus.value = 'updating'
        
        // 初始化store数据（会优先使用缓存）
        try {
          if (typeof analyticsStore.initializeData === 'function') {
            await analyticsStore.initializeData()
          } else if (typeof analyticsStore.loadAnalyticsData === 'function') {
            await analyticsStore.loadAnalyticsData()
          } else {
            // 如果store方法不存在，直接调用API更新图表
            console.log('Store methods not available, loading data directly')
            await updateCharts()
          }
        } catch (storeError) {
          console.warn('Store initialization failed, loading data directly:', storeError)
          await updateCharts()
        }
        
        await getSubjects()

        await nextTick()

        // 初始化所有图表
        await initTrendChart()
        await initAccuracyChart()
        await initSubjectChart()
        await initDifficultyChart()
        await initWrongChart()

        cacheStatus.value = 'fresh'
        
        // 监听窗口大小变化
        window.addEventListener('resize', handleResize)
      } catch (err) {
        console.error('Analytics page initialization failed:', err)
        cacheStatus.value = 'error'
        MessagePlugin.error('页面初始化失败，请刷新重试')
      }
    })

    // 组件卸载时清理
    onUnmounted(() => {
      // 移除窗口大小变化监听器
      window.removeEventListener('resize', handleResize)
      
      // 销毁图表实例
      if (trendChart) {
        trendChart.dispose()
        trendChart = null
      }
      if (accuracyChart) {
        accuracyChart.dispose()
        accuracyChart = null
      }
      if (subjectChart) {
        subjectChart.dispose()
        subjectChart = null
      }
      if (difficultyChart) {
        difficultyChart.dispose()
        difficultyChart = null
      }
      if (wrongChart) {
        wrongChart.dispose()
        wrongChart = null
      }
    })

    return {
      subjects,
      dateRange,
      startDate,
      endDate,
      selectedSubject,
      trendType,
      overviewStats,
      suggestions,
      trendChartRef,
      accuracyChartRef,
      subjectChartRef,
      difficultyChartRef,
      wrongChartRef,
      updateTrendChart,
      updateCharts,
      exportReport,
      exportSingleChart,
      disableDate,
      disableStartDate,
      disableEndDate,
      onDateRangeChange,
      onStartDateChange,
      onEndDateChange,
      
      // 加载状态和错误处理
      loading,
      initialLoading,
      refreshing,
      networkError,
      timeoutError,
      hasError,
      cacheStatus,
      handleRetry,
      refreshAllData,
      reportError,
      goToPractice
    }
  }
}
</script>

<style scoped>
.analytics-page {
  padding: 20px;
  background-color: #f5f7fa;
  max-width: 1200px;
  margin: 0 auto;
  overflow-x: hidden;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .analytics-page {
    padding: 15px;
    max-width: 95%;
  }
}

@media (max-width: 768px) {
  .analytics-page {
    padding: 10px;
    max-width: 100%;
  }

  .overview-stats .t-col {
    margin-bottom: 10px;
    flex: 0 0 100% !important;
    max-width: 100% !important;
  }

  .filter-card .t-row {
    flex-direction: column;
  }

  .filter-card .t-col {
    margin-bottom: 10px;
    flex: 0 0 100% !important;
    max-width: 100% !important;
  }

  .chart-container {
    height: 250px;
  }

  .suggestions {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 480px) {
  .analytics-page {
    padding: 8px;
  }

  .stat-card {
    height: 90px;
  }

  .stat-icon {
    font-size: 32px;
    margin-right: 12px;
  }

  .stat-value {
    font-size: 20px;
  }

  .stat-label {
    font-size: 13px;
  }

  .chart-container {
    height: 220px;
  }
}

.analytics-header {
  text-align: center;
  margin-bottom: 15px;
  padding: 0;
}

.analytics-header h2 {
  color: #333;
  margin-bottom: 10px;
}

.analytics-header p {
  color: #666;
}

.filter-card {
  margin-bottom: 20px;
}

.filter-card .t-date-editor {
  width: 100%;
}

.filter-card .t-select {
  width: 100%;
}

.overview-stats {
  margin-bottom: 20px;
}

.stat-card {
  height: 100px;
}

.stat-content {
  display: flex;
  align-items: center;
  height: 100%;
}

.stat-icon {
  font-size: 40px;
  margin-right: 15px;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
  color: #333;
  margin-bottom: 5px;
}

.stat-label {
  color: #666;
  font-size: 14px;
}

.chart-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chart-container {
  width: 100%;
  height: 300px;
  min-height: 250px;
  overflow: hidden;
}

@media (max-width: 768px) {
  .chart-container {
    height: 250px;
  }
}

.suggestion-card {
  margin-top: 20px;
}

.suggestions {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 20px;
}

.suggestion-item {
  display: flex;
  align-items: flex-start;
  padding: 15px;
  background-color: #f8f9fa;
  border-radius: 6px;
  border-left: 4px solid #409EFF;
}

.suggestion-icon {
  font-size: 24px;
  margin-right: 15px;
  margin-top: 5px;
}

.suggestion-content h4 {
  color: #333;
  margin-bottom: 8px;
  font-size: 16px;
}

.suggestion-content p {
  color: #666;
  line-height: 1.5;
  margin: 0;
}

/* Analytics Multiselect 自定义样式 */
:deep(.analytics-multiselect-container) {
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  min-height: 32px;
  background: white;
  transition: all 0.2s;
  position: relative;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  box-sizing: border-box;
}

:deep(.analytics-multiselect-container:hover) {
  border-color: #4dabf7;
}

:deep(.analytics-multiselect-container.is-active) {
  border-color: #0052d9;
  box-shadow: 0 0 0 2px rgba(0, 82, 217, 0.1);
}

:deep(.analytics-select-dropdown) {
  border: 1px solid #e6e6e6;
  border-radius: 6px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  z-index: 99999 !important;
  background: white;
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  max-height: 200px;
  overflow-y: auto;
}

:deep(.analytics-select-dropdown.is-hidden) {
  display: none !important;
}

/* 确保 Analytics 页面的 multiselect 基础样式正确 */
:deep(.analytics-multiselect-container .multiselect) {
  min-height: 32px;
  height: 32px;
  width: 100%;
}

:deep(.analytics-multiselect-container .multiselect-single-label) {
  padding-left: 12px;
  padding-right: 40px;
  line-height: 30px;
}

:deep(.analytics-multiselect-container .multiselect-placeholder) {
  padding-left: 12px;
  line-height: 30px;
  color: #bbb;
}

:deep(.analytics-multiselect-container .multiselect-caret) {
  margin-right: 12px;
}

/* 修复 Analytics 页面可能的样式冲突 */
.analytics-page :deep(.multiselect) {
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif !important;
  font-size: 14px !important;
  line-height: 1.5 !important;
}
</style>

<style src="@vueform/multiselect/themes/default.css"></style>