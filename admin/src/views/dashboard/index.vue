<template>
  <div class="dashboard">
    <!-- 核心指标卡片 -->
    <el-row :gutter="20" class="stat-cards">
      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-info">
              <p class="stat-label">用户总量</p>
              <p class="stat-value">{{ formatNumber(dashboardData.userCount) }}</p>
            </div>
            <el-icon class="stat-icon" style="color: #409eff"><User /></el-icon>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-info">
              <p class="stat-label">文章总量</p>
              <p class="stat-value">{{ formatNumber(dashboardData.articleCount) }}</p>
            </div>
            <el-icon class="stat-icon" style="color: #67c23a"><Document /></el-icon>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-info">
              <p class="stat-label">日活用户</p>
              <p class="stat-value">{{ formatNumber(dashboardData.dailyActive) }}</p>
            </div>
            <el-icon class="stat-icon" style="color: #e6a23c"><TrendCharts /></el-icon>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-info">
              <p class="stat-label">浏览量</p>
              <p class="stat-value">{{ formatNumber(dashboardData.viewCount) }}</p>
            </div>
            <el-icon class="stat-icon" style="color: #f56c6c"><View /></el-icon>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 趋势图和热门文章 -->
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
            <span>热门文章排行</span>
          </template>
          <div class="hot-articles">
            <div
              v-for="(article, index) in dashboardData.hotArticles"
              :key="article.id"
              class="hot-article-item"
            >
              <span class="rank" :class="{ 'top3': index < 3 }">{{ index + 1 }}</span>
              <span class="title">{{ article.title }}</span>
              <span class="views">{{ formatNumber(article.viewCount) }}</span>
            </div>
            <el-empty v-if="!dashboardData.hotArticles?.length" description="暂无数据" />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 最新待审核 -->
    <el-card shadow="hover" class="pending-section">
      <template #header>
        <div class="card-header">
          <span>最新待审核文章</span>
          <el-button type="primary" link @click="$router.push('/articles/pending')">
            查看全部
          </el-button>
        </div>
      </template>
      <el-table :data="dashboardData.pendingArticles" stripe style="width: 100%">
        <el-table-column prop="title" label="文章标题" min-width="200" />
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
      <el-empty v-if="!dashboardData.pendingArticles?.length" description="暂无待审核文章" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent,
} from 'echarts/components'
import type { DashboardData, Article } from '@/types'
import { getDashboardData } from '@/api/dashboard'
import { formatNumber } from '@/utils/format'
import { useRequestCache } from '@/composables/useRequestCache'

// 注册 ECharts 组件
use([
  CanvasRenderer,
  LineChart,
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent,
])

const router = useRouter()

/** 仪表盘缓存实例 */
const dashboardCache = useRequestCache<DashboardData>({
  ttl: 2 * 60 * 1000,
  staleWhileRevalidate: true,
})

/** 仪表盘数据 */
const dashboardData = ref<DashboardData>({
  userCount: 0,
  articleCount: 0,
  dailyActive: 0,
  viewCount: 0,
  interactionCount: 0,
  trendData: [],
  hotArticles: [],
  pendingArticles: [],
})

/** 趋势图配置 */
const trendChartOption = computed(() => ({
  tooltip: {
    trigger: 'axis',
  },
  legend: {
    data: ['新增用户', '新增文章', '浏览量'],
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
    data: dashboardData.value.trendData.map((item) => item.date),
  },
  yAxis: {
    type: 'value',
  },
  series: [
    {
      name: '新增用户',
      type: 'line',
      smooth: true,
      data: dashboardData.value.trendData.map((item) => item.userCount),
      itemStyle: { color: '#409eff' },
    },
    {
      name: '新增文章',
      type: 'line',
      smooth: true,
      data: dashboardData.value.trendData.map((item) => item.articleCount),
      itemStyle: { color: '#67c23a' },
    },
    {
      name: '浏览量',
      type: 'line',
      smooth: true,
      data: dashboardData.value.trendData.map((item) => item.viewCount),
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
    const result = await dashboardCache.request('/dashboard')
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

      .title {
        flex: 1;
        font-size: 14px;
        color: #333;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .views {
        font-size: 12px;
        color: #999;
        margin-left: 12px;
        flex-shrink: 0;
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
