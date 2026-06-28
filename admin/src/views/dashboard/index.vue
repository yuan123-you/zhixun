<template>
  <div class="dashboard">
    <!-- 时间维度选择器 -->
    <div class="period-selector">
      <el-radio-group v-model="period" @change="loadDashboard">
        <el-radio-button value="daily">日</el-radio-button>
        <el-radio-button value="weekly">周</el-radio-button>
        <el-radio-button value="monthly">月</el-radio-button>
      </el-radio-group>
    </div>

    <!-- 核心指标卡片 -->
    <el-row :gutter="20" class="stat-cards">
      <el-col :xs="24" :sm="12" :md="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-info">
              <p class="stat-label">用户总量</p>
              <p class="stat-value">{{ formatNumber(dashboardData.userTotal) }}</p>
            </div>
            <el-icon class="stat-icon" style="color: #409eff"><User /></el-icon>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-info">
              <p class="stat-label">作品总量</p>
              <p class="stat-value">{{ formatNumber(dashboardData.articleTotal) }}</p>
            </div>
            <el-icon class="stat-icon" style="color: #67c23a"><Document /></el-icon>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-info">
              <p class="stat-label">日活用户</p>
              <p class="stat-value">{{ formatNumber(dashboardData.todayDau) }}</p>
            </div>
            <el-icon class="stat-icon" style="color: #e6a23c"><TrendCharts /></el-icon>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-info">
              <p class="stat-label">今日浏览</p>
              <p class="stat-value">{{ formatNumber(dashboardData.todayView) }}</p>
            </div>
            <el-icon class="stat-icon" style="color: #f56c6c"><View /></el-icon>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-info">
              <p class="stat-label">今日点赞</p>
              <p class="stat-value">{{ formatNumber(dashboardData.todayLike) }}</p>
            </div>
            <el-icon class="stat-icon" style="color: #e040fb"><StarFilled /></el-icon>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-info">
              <p class="stat-label">今日评论</p>
              <p class="stat-value">{{ formatNumber(dashboardData.todayComment) }}</p>
            </div>
            <el-icon class="stat-icon" style="color: #00bcd4"><ChatDotRound /></el-icon>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 趋势图和热门作品 -->
    <el-row :gutter="20" class="chart-section">
      <el-col :xs="24" :lg="16">
        <el-card shadow="hover">
          <template #header>
            <span>数据趋势</span>
          </template>
          <v-chart :option="trendChartOption" autoresize style="height: 350px" />
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="8">
        <el-card shadow="hover">
          <template #header>
            <span>热门作品排行</span>
          </template>
          <div class="hot-articles">
            <div
              v-for="article in dashboardData.hotArticleRanks"
              :key="article.articleId"
              class="hot-article-item"
            >
              <span class="rank" :class="{ 'top3': article.rank <= 3 }">{{ article.rank }}</span>
              <div class="article-info">
                <span class="title">{{ article.title }}</span>
                <span class="author">{{ article.authorName }}</span>
              </div>
              <span class="views">{{ formatNumber(article.viewCount) }}</span>
            </div>
            <el-empty v-if="!dashboardData.hotArticleRanks?.length" description="暂无数据" />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 留存率图表和活跃度分布 -->
    <el-row :gutter="20" class="chart-section">
      <el-col :xs="24" :lg="12">
        <el-card shadow="hover">
          <template #header>
            <span>用户留存率</span>
          </template>
          <v-chart :option="retentionChartOption" autoresize style="height: 300px" />
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="12">
        <el-card shadow="hover">
          <template #header>
            <span>活跃度分布</span>
          </template>
          <v-chart :option="activityPieChartOption" autoresize style="height: 300px" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 分类分布 -->
    <el-row :gutter="20" class="chart-section">
      <el-col :span="24">
        <el-card shadow="hover">
          <template #header>
            <span>分类分布</span>
          </template>
          <v-chart :option="categoryBarChartOption" autoresize style="height: 300px" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 增长趋势 -->
    <el-row :gutter="20" class="chart-section">
      <el-col :span="24">
        <el-card shadow="hover">
          <template #header>
            <span>增长趋势</span>
          </template>
          <v-chart :option="growthTrendChartOption" autoresize style="height: 300px" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 创作者排行 -->
    <el-card shadow="hover" class="table-section">
      <template #header>
        <span>创作者排行</span>
      </template>
      <el-table :data="dashboardData.creatorRanks" stripe style="width: 100%">
        <el-table-column prop="rank" label="排名" width="70" align="center" />
        <el-table-column label="创作者" min-width="180">
          <template #default="{ row }">
            <div class="creator-cell">
              <el-avatar :src="row.avatar" :size="32" />
              <span class="creator-name">{{ row.nickname }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="articleCount" label="作品数" width="100" align="center" />
        <el-table-column label="总浏览量" width="120" align="center">
          <template #default="{ row }">
            {{ formatNumber(row.totalViews) }}
          </template>
        </el-table-column>
        <el-table-column label="总点赞" width="100" align="center">
          <template #default="{ row }">
            {{ formatNumber(row.totalLikes) }}
          </template>
        </el-table-column>
        <el-table-column prop="followerCount" label="粉丝数" width="100" align="center" />
      </el-table>
      <el-empty v-if="!dashboardData.creatorRanks?.length" description="暂无数据" />
    </el-card>

    <!-- 最新待审核 -->
    <el-card shadow="hover" class="pending-section">
      <template #header>
        <div class="card-header">
          <span>最新待审核作品</span>
          <el-button type="primary" link @click="$router.push('/articles/pending')">
            查看全部
          </el-button>
        </div>
      </template>
      <el-table :data="dashboardData.pendingArticles" stripe style="width: 100%">
        <el-table-column prop="title" label="作品标题" min-width="200" />
        <el-table-column prop="authorName" label="作者" width="120" />
        <el-table-column prop="categoryName" label="分类" width="120" />
        <el-table-column prop="createdAt" label="提交时间" width="180" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleAudit(row)">
              审核
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!dashboardData.pendingArticles?.length" description="暂无待审核作品" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, BarChart, PieChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent,
} from 'echarts/components'
import type { EnhancedDashboardData, Article } from '@/types'
import { getEnhancedDashboardData } from '@/api/dashboard'
import { formatNumber } from '@/utils/format'
import { useRequestCache } from '@/composables/useRequestCache'

// 注册 ECharts 组件
use([
  CanvasRenderer,
  LineChart,
  BarChart,
  PieChart,
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent,
])

const router = useRouter()

/** 时间维度 */
const period = ref<'daily' | 'weekly' | 'monthly'>('daily')

/** 仪表盘缓存实例 */
const dashboardCache = useRequestCache<EnhancedDashboardData>({
  ttl: 2 * 60 * 1000,
  staleWhileRevalidate: true,
})

/** 仪表盘数据 */
const dashboardData = ref<EnhancedDashboardData>({
  userTotal: 0,
  articleTotal: 0,
  todayDau: 0,
  todayView: 0,
  todayLike: 0,
  todayComment: 0,
  trend: { dates: [], views: [], users: [] },
  retentionRates: [],
  activityDistributions: [],
  growthTrends: [],
  categoryDistributions: [],
  hotArticleRanks: [],
  creatorRanks: [],
  pendingArticles: [],
})

/** 趋势图配置 */
const trendChartOption = computed(() => ({
  tooltip: {
    trigger: 'axis',
  },
  legend: {
    data: ['浏览量', '活跃用户'],
  },
  grid: {
    left: '3%',
    right: '4%',
    bottom: '3%',
    containLabel: true,
  },
  xAxis: {
    type: 'category',
    boundaryGap: false,
    data: dashboardData.value.trend.dates,
  },
  yAxis: {
    type: 'value',
  },
  series: [
    {
      name: '浏览量',
      type: 'line',
      smooth: true,
      data: dashboardData.value.trend.views,
      itemStyle: { color: '#e6a23c' },
    },
    {
      name: '活跃用户',
      type: 'line',
      smooth: true,
      data: dashboardData.value.trend.users,
      itemStyle: { color: '#409eff' },
    },
  ],
}))

/** 留存率图表配置 */
const retentionChartOption = computed(() => ({
  tooltip: {
    trigger: 'axis',
    formatter: (params: any) => {
      const item = Array.isArray(params) ? params[0] : params
      return `第${item.axisValue}天：${(item.value * 100).toFixed(1)}%`
    },
  },
  grid: {
    left: '3%',
    right: '4%',
    bottom: '3%',
    containLabel: true,
  },
  xAxis: {
    type: 'category',
    data: dashboardData.value.retentionRates.map((item) => `第${item.day}天`),
  },
  yAxis: {
    type: 'value',
    axisLabel: {
      formatter: (value: number) => `${(value * 100).toFixed(0)}%`,
    },
    max: 1,
  },
  series: [
    {
      name: '留存率',
      type: 'line',
      smooth: true,
      data: dashboardData.value.retentionRates.map((item) => item.rate),
      itemStyle: { color: '#67c23a' },
      areaStyle: {
        color: {
          type: 'linear',
          x: 0,
          y: 0,
          x2: 0,
          y2: 1,
          colorStops: [
            { offset: 0, color: 'rgba(103, 194, 58, 0.3)' },
            { offset: 1, color: 'rgba(103, 194, 58, 0.05)' },
          ],
        },
      },
    },
  ],
}))

/** 活跃度分布饼图配置 */
const activityPieChartOption = computed(() => ({
  tooltip: {
    trigger: 'item',
    formatter: '{b}: {c}人 ({d}%)',
  },
  legend: {
    orient: 'vertical',
    left: 'left',
  },
  series: [
    {
      name: '活跃度分布',
      type: 'pie',
      radius: ['40%', '70%'],
      center: ['55%', '50%'],
      avoidLabelOverlap: false,
      itemStyle: {
        borderRadius: 4,
        borderColor: '#fff',
        borderWidth: 2,
      },
      label: {
        show: true,
        formatter: '{b}\n{d}%',
      },
      data: dashboardData.value.activityDistributions.map((item) => ({
        name: item.level,
        value: item.count,
      })),
      color: ['#409eff', '#e6a23c', '#f56c6c'],
    },
  ],
}))

/** 分类分布柱状图配置 */
const categoryBarChartOption = computed(() => ({
  tooltip: {
    trigger: 'axis',
    axisPointer: { type: 'shadow' },
  },
  grid: {
    left: '3%',
    right: '4%',
    bottom: '3%',
    containLabel: true,
  },
  xAxis: {
    type: 'category',
    data: dashboardData.value.categoryDistributions.map((item) => item.categoryName),
    axisLabel: {
      rotate: 30,
    },
  },
  yAxis: {
    type: 'value',
    name: '作品数',
  },
  series: [
    {
      name: '作品数',
      type: 'bar',
      data: dashboardData.value.categoryDistributions.map((item) => item.articleCount),
      itemStyle: {
        color: {
          type: 'linear',
          x: 0,
          y: 0,
          x2: 0,
          y2: 1,
          colorStops: [
            { offset: 0, color: '#409eff' },
            { offset: 1, color: '#79bbff' },
          ],
        },
        borderRadius: [4, 4, 0, 0],
      },
      barMaxWidth: 50,
    },
  ],
}))

/** 增长趋势图表配置 */
const growthTrendChartOption = computed(() => ({
  tooltip: {
    trigger: 'axis',
  },
  legend: {
    data: ['新增用户', '新增作品', '浏览量'],
  },
  grid: {
    left: '3%',
    right: '4%',
    bottom: '3%',
    containLabel: true,
  },
  xAxis: {
    type: 'category',
    data: dashboardData.value.growthTrends.map((item) => item.periodLabel),
  },
  yAxis: {
    type: 'value',
  },
  series: [
    {
      name: '新增用户',
      type: 'bar',
      data: dashboardData.value.growthTrends.map((item) => item.newUserCount),
      itemStyle: { color: '#409eff' },
    },
    {
      name: '新增作品',
      type: 'bar',
      data: dashboardData.value.growthTrends.map((item) => item.newArticleCount),
      itemStyle: { color: '#67c23a' },
    },
    {
      name: '浏览量',
      type: 'bar',
      data: dashboardData.value.growthTrends.map((item) => item.viewCount),
      itemStyle: { color: '#e6a23c' },
    },
  ],
}))

/** 跳转审核 */
function handleAudit(article: Article) {
  router.push('/articles/pending')
}

/** 加载仪表盘数据 */
async function loadDashboard() {
  try {
    const result = await dashboardCache.request('/admin/dashboard/overview', {
      period: period.value,
    } as any)
    dashboardData.value = result
  } catch {
    // 错误已在拦截器中处理
  }
}

onMounted(() => {
  loadDashboard()
})
</script>

<style scoped lang="scss">
.dashboard {
  .period-selector {
    margin-bottom: 20px;
    text-align: center;
  }

  .stat-cards {
    margin-bottom: 20px;
  }

  .stat-card {
    margin-bottom: 12px;

    .stat-content {
      display: flex;
      align-items: center;
      justify-content: space-between;

      .stat-info {
        .stat-label {
          font-size: 14px;
          color: #999;
          margin: 0 0 8px;
        }
        .stat-value {
          font-size: 28px;
          font-weight: 700;
          color: #333;
          margin: 0;
        }
      }

      .stat-icon {
        font-size: 48px;
        opacity: 0.8;
      }
    }
  }

  .chart-section {
    margin-bottom: 20px;
  }

  .hot-articles {
    .hot-article-item {
      display: flex;
      align-items: center;
      padding: 10px 0;
      border-bottom: 1px solid #f5f5f5;

      &:last-child {
        border-bottom: none;
      }

      .rank {
        width: 24px;
        height: 24px;
        border-radius: 50%;
        background: #f0f0f0;
        color: #999;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 12px;
        margin-right: 12px;
        flex-shrink: 0;

        &.top3 {
          background: #409eff;
          color: #fff;
        }
      }

      .article-info {
        flex: 1;
        min-width: 0;
        display: flex;
        flex-direction: column;

        .title {
          font-size: 14px;
          color: #333;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }

        .author {
          font-size: 12px;
          color: #999;
          margin-top: 2px;
        }
      }

      .views {
        font-size: 12px;
        color: #999;
        margin-left: 12px;
        flex-shrink: 0;
      }
    }
  }

  .table-section {
    margin-bottom: 20px;

    .creator-cell {
      display: flex;
      align-items: center;
      gap: 8px;

      .creator-name {
        font-size: 14px;
        color: #333;
      }
    }
  }

  .pending-section {
    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
  }
}
</style>