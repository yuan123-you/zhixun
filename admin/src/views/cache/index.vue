<template>
  <div class="cache-management">
    <PageHeader title="缓存管理" description="管理多级缓存（L1 Caffeine + L2 Redis），检查缓存一致性，必要时清空缓存" />

    <el-card class="cache-card">
      <template #header>
        <span class="card-header-title">缓存操作</span>
      </template>

      <div class="cache-actions">
        <div class="action-item">
          <el-button
            type="primary"
            :loading="checking"
            :icon="Search"
            @click="handleCheckConsistency"
          >
            检查缓存一致性
          </el-button>
          <span class="action-desc">检查 L1 和 L2 缓存中文章详情版本是否与数据库一致</span>
        </div>

        <div class="action-item">
          <el-button
            type="danger"
            :loading="clearing"
            :icon="Delete"
            @click="handleClearAllCaches"
          >
            清空所有缓存
          </el-button>
          <span class="action-desc">清空所有 L1 Caffeine 和 L2 Redis 缓存数据</span>
        </div>
      </div>
    </el-card>

    <!-- 一致性检查结果对话框 -->
    <el-dialog
      v-model="resultDialogVisible"
      title="缓存一致性检查结果"
      width="650px"
      :close-on-click-modal="false"
    >
      <div v-if="consistencyResult" class="consistency-result">
        <!-- 一致 -->
        <el-result
          v-if="consistencyResult.consistent"
          icon="success"
          title="缓存一致性正常"
          sub-title="所有缓存数据与数据库版本一致，无需处理"
        >
          <template #extra>
            <el-tag type="success" size="large">
              已检查 {{ consistencyResult.articleDetail.checkedCount }} 项
            </el-tag>
          </template>
        </el-result>

        <!-- 不一致 -->
        <div v-else>
          <el-result
            icon="warning"
            title="发现缓存不一致"
            :sub-title="`已检查 ${consistencyResult.articleDetail.checkedCount} 项，其中 ${consistencyResult.articleDetail.inconsistentCount} 项不一致`"
          />

          <el-alert
            type="error"
            :closable="false"
            show-icon
            class="inconsistency-alert"
          >
            <template #title>
              <span class="alert-title">以下文章的缓存版本与数据库版本不一致，建议清空缓存后重建</span>
            </template>
          </el-alert>

          <el-table
            :data="consistencyResult.articleDetail.details"
            stripe
            border
            class="inconsistency-table"
          >
            <el-table-column prop="id" label="文章 ID" width="100" align="center" />
            <el-table-column prop="cacheVersion" label="缓存版本" width="150" align="center">
              <template #default="{ row }">
                <el-tag type="warning">{{ row.cacheVersion }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="dbVersion" label="数据库版本" width="150" align="center">
              <template #default="{ row }">
                <el-tag type="success">{{ row.dbVersion }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <!-- 错误信息 -->
        <div v-if="consistencyResult.error" class="error-message">
          <el-alert type="error" :closable="false" show-icon :title="consistencyResult.error" />
        </div>
      </div>

      <template #footer>
        <el-button @click="resultDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Delete } from '@element-plus/icons-vue'
import type { CacheConsistencyResult } from '@/types'
import { checkCacheConsistency, clearAllCaches } from '@/api/cache'
import PageHeader from '@/components/PageHeader.vue'

const checking = ref(false)
const clearing = ref(false)
const resultDialogVisible = ref(false)
const consistencyResult = ref<CacheConsistencyResult | null>(null)

/** 检查缓存一致性 */
async function handleCheckConsistency() {
  checking.value = true
  try {
    const res = await checkCacheConsistency()
    consistencyResult.value = res.data
    resultDialogVisible.value = true
  } catch {
    // 错误已在拦截器中处理
  } finally {
    checking.value = false
  }
}

/** 清空所有缓存 */
async function handleClearAllCaches() {
  try {
    await ElMessageBox.confirm(
      '确定要清空所有缓存吗？此操作将清空 L1（Caffeine）和 L2（Redis）的所有缓存数据，可能会短暂影响系统性能。',
      '清空缓存确认',
      {
        confirmButtonText: '确定清空',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )
  } catch {
    return
  }

  clearing.value = true
  try {
    const res = await clearAllCaches()
    ElMessage.success(res.data.message || '所有缓存已清空')
  } catch {
    // 错误已在拦截器中处理
  } finally {
    clearing.value = false
  }
}
</script>

<style scoped lang="scss">
.cache-management {
  .cache-card {
    :deep(.el-card__header) {
      .card-header-title {
        font-size: 16px;
        font-weight: 600;
      }
    }
  }

  .cache-actions {
    .action-item {
      display: flex;
      align-items: center;
      gap: 16px;
      padding: 16px 0;

      & + .action-item {
        border-top: 1px solid #ebeef5;
      }

      .action-desc {
        font-size: 13px;
        color: #909399;
      }
    }
  }

  .consistency-result {
    .inconsistency-alert {
      margin: 20px 0 16px;

      .alert-title {
        font-weight: 500;
      }
    }

    .inconsistency-table {
      margin-top: 12px;
    }

    .error-message {
      margin-top: 16px;
    }
  }
}
</style>