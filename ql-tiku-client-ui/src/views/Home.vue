<template>
  <div class="home-page">
    <div class="welcome-section">
        <h1>欢迎使用在线刷题系统</h1>
        <p>提升您的学习效率，掌握更多知识</p>
        
        <!-- 每日一语 -->
        <div class="daily-quote-section" v-if="dailyQuote || loadingQuote">
          <div class="daily-quote-card" v-if="dailyQuote && !loadingQuote">
            <div class="quote-icon">
              <t-icon name="chat" />
            </div>
            <div class="quote-content">
              <p class="quote-text">{{ dailyQuote }}</p>
              <span class="quote-label">每日一语</span>
            </div>
          </div>
          <div class="daily-quote-loading" v-else-if="loadingQuote">
            <t-skeleton :row-col="[{ size: 'small', col: 12 }, { size: 'small', col: 8 }]" />
          </div>
        </div>
      </div>
      
      <!-- 功能卡片 -->
      <div class="feature-cards">
        <t-row :gutter="[16, 16]" justify="center">
          <t-col :xs="12" :sm="6" :md="3" :lg="3">
            <t-card class="feature-card" hover-shadow @click="$router.push('/practice')">
              <div class="card-content">
                <span class="icon">✏️</span>
                <h3>刷题练习</h3>
                <p>海量题库，随时练习</p>
              </div>
            </t-card>
          </t-col>
          
          <t-col :xs="12" :sm="6" :md="3" :lg="3">
            <t-card class="feature-card" hover-shadow @click="$router.push('/exam')">
              <div class="card-content">
                <span class="icon">📄</span>
                <h3>模拟考试</h3>
                <p>真实考试环境体验</p>
              </div>
            </t-card>
          </t-col>
          
          <t-col :xs="12" :sm="6" :md="3" :lg="3">
            <t-card class="feature-card" hover-shadow @click="$router.push('/wrong-book')">
              <div class="card-content">
                <span class="icon">📁</span>
                <h3>错题本</h3>
                <p>记录错题，重点复习</p>
              </div>
            </t-card>
          </t-col>
          
          <t-col :xs="12" :sm="6" :md="3" :lg="3">
            <t-card class="feature-card" hover-shadow @click="$router.push('/analytics')">
              <div class="card-content">
                <t-icon name="chart" class="card-icon" color="#F56C6C" />
                <h3>数据分析</h3>
                <p>学习进度可视化</p>
              </div>
            </t-card>
          </t-col>
        </t-row>
      </div>
      
      <!-- 统计信息和公告栏 -->
      <div class="stats-announcements-container">
        <!-- 统计卡片区域 -->
        <div class="stats-grid">
          <t-card class="stat-card">
            <t-statistic title="今日练习" :value="todayStats.practiceCount" suffix="题" />
          </t-card>
          <t-card class="stat-card">
            <t-statistic title="今日正确率" :value="todayStats.accuracy" suffix="%" />
          </t-card>
          <t-card class="stat-card">
            <t-statistic title="累计练习" :value="totalStats.totalCount" suffix="题" />
          </t-card>
        </div>
        
        <!-- 公告栏区域 -->
        <div class="announcements-section">
          <t-card>
            <template #header>
              <div class="card-header">
                <t-icon name="notification" />
                <span>系统公告</span>
              </div>
            </template>
            
            <div v-if="announcements.length === 0" class="no-announcements">
              <t-empty description="暂无公告" />
            </div>
            
            <div v-else class="announcements-list">
              <div 
                v-for="announcement in announcements" 
                :key="announcement.id" 
                class="announcement-item"
                @click="showAnnouncementDetail(announcement)"
              >
                <div class="announcement-header">
                  <t-tag 
                    :theme="getAnnouncementTagType(announcement.type)" 
                    size="small"
                  >
                    {{ announcement.typeText }}
                  </t-tag>
                  <span class="announcement-time">{{ formatTime(announcement.createTime) }}</span>
                </div>
                <h4 class="announcement-title">{{ announcement.title }}</h4>
                <p class="announcement-content">{{ announcement.content }}</p>
              </div>
            </div>
          </t-card>
        </div>
      </div>
    </div>
</template>

<script>
import { reactive, ref, onMounted, nextTick } from 'vue'
import { DialogPlugin } from 'tdesign-vue-next'
import { statisticsAPI, announcementAPI, publicAPI } from '../api'

export default {
  name: 'Home',
  setup() {
    console.log('Home.vue setup() started')
    
    // 添加页面加载状态
    const pageLoading = ref(true)
    const hasError = ref(false)
    
    const todayStats = reactive({
      practiceCount: 0,
      accuracy: 0
    })
    
    const totalStats = reactive({
      totalCount: 0
    })
    
    const announcements = ref([])
    
    // 每日一语相关状态
    const dailyQuote = ref('')
    const loadingQuote = ref(false)
    
    // 获取统计数据
    const getStats = async () => {
      console.log('Home.vue: 开始获取统计数据')
      try {
        const response = await statisticsAPI.getPersonalStats()
        console.log('Home.vue: 统计数据获取成功', response)
        // API拦截器已经处理了响应，直接使用返回的数据
        if (response) {
          Object.assign(todayStats, response.today || {})
          Object.assign(totalStats, response.total || {})
        }
      } catch (error) {
        console.error('Home.vue: 获取统计数据失败:', error)
        // 设置默认值，确保页面能正常显示
        Object.assign(todayStats, { practiceCount: 0, accuracy: 0 })
        Object.assign(totalStats, { totalCount: 0 })
      }
    }
    
    // 获取每日一语
    const getDailyQuote = async () => {
      console.log('Home.vue: 开始获取每日一语')
      loadingQuote.value = true
      try {
        const response = await publicAPI.getDailyQuote()
        console.log('Home.vue: 每日一语获取成功', response)
        if (response && typeof response === 'string' && response.trim() !== '') {
          dailyQuote.value = response
        } else {
          // 如果API返回空数据，设置一个默认值
          dailyQuote.value = '学而时习之，不亦说乎？'
        }
      } catch (error) {
        console.error('Home.vue: 获取每日一语失败:', error)
        // 设置默认值以避免空白
        dailyQuote.value = '学而时习之，不亦说乎？'
      } finally {
        loadingQuote.value = false
      }
    }
    
    // 获取公告列表
    const getAnnouncements = async () => {
      try {
        const response = await announcementAPI.getLatestAnnouncements(10)
        // API拦截器已经处理了响应，直接使用返回的数据
        if (response && Array.isArray(response)) {
          announcements.value = response
        }
      } catch (error) {
        console.error('获取公告列表失败:', error)
        // 设置默认值
        announcements.value = []
      }
    }
    
    // 获取公告标签类型
    const getAnnouncementTagType = (type) => {
      switch (type) {
        case 1: return 'info'     // 普通公告
        case 2: return 'warning'  // 重要公告
        case 3: return 'danger'   // 系统维护
        default: return 'info'
      }
    }
    
    // 格式化时间
    const formatTime = (timeStr) => {
      if (!timeStr) return ''
      const date = new Date(timeStr)
      const now = new Date()
      const diff = now - date
      const days = Math.floor(diff / (1000 * 60 * 60 * 24))
      
      if (days === 0) {
        const hours = Math.floor(diff / (1000 * 60 * 60))
        if (hours === 0) {
          const minutes = Math.floor(diff / (1000 * 60))
          return minutes <= 0 ? '刚刚' : `${minutes}分钟前`
        }
        return `${hours}小时前`
      } else if (days === 1) {
        return '昨天'
      } else if (days < 7) {
        return `${days}天前`
      } else {
        return date.toLocaleDateString()
      }
    }
    
    // 显示公告详情
    const showAnnouncementDetail = (announcement) => {
      DialogPlugin.alert({
        header: announcement.title,
        body: announcement.content,
        theme: getAnnouncementTagType(announcement.type) === 'danger' ? 'warning' : 'info',
        confirmBtn: '确定'
      })
    }
    
    // 初始化页面数据
    const initPageData = async () => {
      try {
        pageLoading.value = true
        hasError.value = false
        
        // 等待DOM更新
        await nextTick()
        
        // 优先加载每日一语，确保用户首先看到内容
        try {
          await getDailyQuote()
        } catch (error) {
          console.error('每日一语加载失败:', error)
          // 即使每日一语失败也继续加载其他数据
          dailyQuote.value = '学而时习之，不亦说乎？'
        }
        
        // 并行执行其他API调用
        const promises = [
          getStats().catch(err => console.error('统计数据加载失败:', err)),
          getAnnouncements().catch(err => console.error('公告数据加载失败:', err))
        ]
        
        await Promise.allSettled(promises)
        
      } catch (error) {
        console.error('页面初始化失败:', error)
        hasError.value = true
      } finally {
        pageLoading.value = false
      }
    }
    
    onMounted(async () => {
      console.log('Home.vue: onMounted - 组件已挂载')
      // 立即初始化页面数据，确保每日一语及时加载
      await initPageData()
    })
    
    return {
      pageLoading,
      hasError,
      todayStats,
      totalStats,
      announcements,
      dailyQuote,
      loadingQuote,
      getAnnouncementTagType,
      formatTime,
      showAnnouncementDetail,
      initPageData
    }
  }
}
</script>

<style scoped>
.home-page {
  padding: 24px;
  min-height: 100vh;
  max-width: 100%;
  overflow-x: hidden;
  box-sizing: border-box;
}

/* 每日一语样式 */
.daily-quote-section {
  margin-top: 32px;
}

.daily-quote-card {
  background: white;
  border-radius: 16px;
  padding: 32px;
  color: #333;
  display: flex;
  align-items: center;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  transition: transform 0.3s ease;
}

.daily-quote-card:hover {
  transform: translateY(-2px);
}

.quote-icon {
  font-size: 32px;
  margin-right: 20px;
  opacity: 0.9;
  flex-shrink: 0;
}

.quote-content {
  flex: 1;
}

.quote-text {
  font-size: 18px;
  line-height: 1.6;
  margin: 0 0 8px 0;
  font-weight: 700;
  color: #333;
}

.quote-label {
  font-size: 14px;
  color: #666;
  font-weight: 300;
}

.daily-quote-loading {
  background: #f8f9fa;
  border-radius: 16px;
  padding: 32px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .home-page {
    padding: 16px;
  }
  
  .welcome-section h1 {
    font-size: 28px;
    margin-bottom: 8px;
  }
  
  .welcome-section p {
    font-size: 16px;
  }
  
  .daily-quote-card {
    padding: 20px;
    flex-direction: column;
    text-align: center;
  }
  
  .quote-icon {
    font-size: 28px;
    margin-right: 0;
    margin-bottom: 12px;
  }
  
  .quote-text {
    font-size: 16px;
  }
  
  .card-content {
    padding: 24px 16px;
    min-height: 140px;
  }
  
  .card-content .icon,
  .card-icon {
    font-size: 40px;
    margin-bottom: 12px;
  }
  
  .card-content h3 {
    font-size: 18px;
  }
  
  .announcement-item {
    padding: 16px;
  }
}

@media (max-width: 480px) {
  .home-page {
    padding: 12px;
  }
  
  .welcome-section h1 {
    font-size: 24px;
  }
  
  .welcome-section p {
    font-size: 14px;
  }
  
  .daily-quote-card {
    padding: 16px;
  }
  
  .quote-text {
    font-size: 14px;
  }
  
  .card-content {
    padding: 20px 12px;
    min-height: 120px;
  }
  
  .card-content .icon,
  .card-icon {
    font-size: 36px;
    margin-bottom: 8px;
  }
  
  .card-content h3 {
    font-size: 16px;
  }
  
  .card-content p {
    font-size: 13px;
  }
}


.welcome-section {
  text-align: center;
  margin-bottom: 40px;
}

.welcome-section h1 {
  font-size: 32px;
  color: #333;
  margin-bottom: 12px;
  font-weight: 600;
}

.welcome-section p {
  font-size: 18px;
  color: #666;
  margin-bottom: 20px;
}

.feature-cards {
  margin-bottom: 40px;
}

.stats-announcements-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  width: 100%;
}

.stat-card {
  width: 100%;
  min-width: auto;
  transition: all 0.3s ease;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

.feature-card {
  cursor: pointer;
  transition: all 0.3s ease;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.feature-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

.card-content {
  text-align: center;
  padding: 16px 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 100px;
}

.card-icon {
  font-size: 24px;
  margin-bottom: 6px;
  color: #1890ff;
}

.card-content .icon {
  font-size: 24px;
  margin-bottom: 6px;
  display: block;
}

.card-content h3 {
  font-size: 14px;
  color: #333;
  margin-bottom: 3px;
  font-weight: 600;
}

.card-content p {
  color: #666;
  font-size: 11px;
  line-height: 1.3;
  margin: 0;
}



.announcements-section {
  margin-top: 0;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: #333;
  font-size: 16px;
}

.no-announcements {
  text-align: center;
  padding: 40px 20px;
}

.announcements-list {
  max-height: 400px;
  overflow-y: auto;
}

.announcement-item {
  padding: 20px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: all 0.3s ease;
  border-radius: 8px;
  margin-bottom: 8px;
}

.announcement-item:hover {
  background-color: #f8f9fa;
  transform: translateY(-1px);
}

.announcement-item:last-child {
  border-bottom: none;
  margin-bottom: 0;
}

.announcement-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  flex-wrap: wrap;
  gap: 8px;
}

.announcement-time {
  font-size: 12px;
  color: #999;
  white-space: nowrap;
}

.announcement-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin: 0 0 8px 0;
  line-height: 1.4;
}

.announcement-content {
  font-size: 14px;
  color: #666;
  line-height: 1.6;
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 响应式设计 - 统计卡片和公告部分 */
@media (max-width: 768px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }
  
  .stats-announcements-container {
    gap: 16px;
  }
  
  .announcements-section {
    margin-top: 0;
  }
  
  .announcement-item {
    padding: 12px;
  }
  
  .announcement-title {
    font-size: 14px;
  }
  
  .announcement-content {
    font-size: 13px;
  }
}

@media (max-width: 480px) {
  .stats-grid {
    grid-template-columns: 1fr;
    gap: 8px;
  }
  
  .stats-announcements-container {
    gap: 12px;
  }
  
  .stat-card {
    padding: 12px;
  }
}
</style>