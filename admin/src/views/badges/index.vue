<template>
  <div class="badges-page">
    <PageHeader title="徽章管理" description="管理系统徽章" />

    <el-card shadow="never">
      <el-table :data="badgeList" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="name" label="徽章名称" min-width="120" />
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="category" label="分类" width="100">
          <template #default="{ row }">
            <el-tag size="small">{{ categoryMap[row.category] || row.category }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="icon" label="图标" width="80">
          <template #default="{ row }">
            <span v-if="row.icon" class="text-2xl">🏅</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button type="danger" size="small" @click="doDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAllBadges, deleteBadge } from '@/api/badge'
import type { BadgeVO } from '@/api/badge'
import PageHeader from '@/components/PageHeader.vue'

const badgeList = ref<BadgeVO[]>([])
const loading = ref(false)
const categoryMap: Record<string, string> = { sign_in: '签到', content: '创作', social: '社交', achievement: '成就', special: '特殊' }

async function fetchBadges() {
  loading.value = true
  try {
    const res = await getAllBadges()
    badgeList.value = res.data || []
  } catch (e) { /* ignore */ }
  loading.value = false
}

async function doDelete(id: number) {
  try {
    await ElMessageBox.confirm('确定要删除该徽章吗？', '确认删除', { type: 'warning' })
    await deleteBadge(id)
    ElMessage.success('删除成功')
    fetchBadges()
  } catch (e) { /* ignore */ }
}

onMounted(fetchBadges)
</script>
