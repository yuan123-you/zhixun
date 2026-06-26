<template>
  <div class="reports-page">
    <PageHeader title="举报管理" description="处理用户提交的文章和用户举报" />

    <!-- Tab switcher -->
    <el-tabs v-model="activeTab" @tab-change="onTabChange" class="mb-4">
      <el-tab-pane label="文章举报" name="article" />
      <el-tab-pane label="用户举报" name="user" />
    </el-tabs>

    <!-- Pending Reports Table -->
    <el-card shadow="never">
      <el-table :data="reportList" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="reporterName" label="举报人" width="100" />
        <el-table-column label="被举报内容" min-width="200">
          <template #default="{ row }">
            <span class="text-blue-600">{{ row.targetTitle }}</span>
            <span class="text-gray-400 ml-1">(ID: {{ row.targetId }})</span>
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="举报原因" width="120">
          <template #default="{ row }">
            <el-tag type="danger" size="small">{{ row.reason }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="详细说明" min-width="150" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="举报时间" width="160" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button type="success" size="small" @click="doHandleReport(row.id, 1)">忽略</el-button>
            <el-button type="danger" size="small" @click="doHandleReport(row.id, 2)">处理</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="flex justify-end mt-4">
        <el-pagination
          v-model:current-page="page"
          :page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="fetchReports"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getPendingReports, handleReport } from '@/api/report'
import type { ReportVO } from '@/api/report'
import PageHeader from '@/components/PageHeader.vue'

const activeTab = ref('article')
const reportList = ref<ReportVO[]>([])
const loading = ref(false)
const page = ref(1)
const pageSize = 20
const total = ref(0)

async function fetchReports() {
  loading.value = true
  try {
    const res = await getPendingReports(activeTab.value, page.value, pageSize)
    reportList.value = res.data.list
    total.value = res.data.total
  } catch (e) { /* handled by interceptor */ }
  loading.value = false
}

function onTabChange() {
  page.value = 1
  fetchReports()
}

async function doHandleReport(id: number, status: number) {
  const actionText = status === 1 ? '忽略' : (activeTab.value === 'user' ? '封禁该用户' : '删除该文章')
  try {
    await ElMessageBox.confirm(`确定要${actionText}吗？`, '确认操作', { type: 'warning' })
    await handleReport(id, status, activeTab.value)
    ElMessage.success('处理成功')
    fetchReports()
  } catch (e) { /* cancelled or error */ }
}

onMounted(fetchReports)
</script>
