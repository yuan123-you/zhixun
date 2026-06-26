<template>
  <div class="topics-page">
    <PageHeader title="话题管理" description="管理平台话题，创建官方话题">
      <template #actions>
        <el-button type="primary" @click="showCreateDialog">创建官方话题</el-button>
      </template>
    </PageHeader>

    <el-card shadow="never">
      <el-table :data="topicList" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="name" label="话题名称" min-width="150">
          <template #default="{ row }">
            <span class="font-semibold">#{{ row.name }}#</span>
            <el-tag v-if="row.isOfficial" type="warning" size="small" class="ml-2">官方</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="articleCount" label="作品数" width="80" />
        <el-table-column prop="followCount" label="关注数" width="80" />
        <el-table-column prop="hotScore" label="热度" width="80" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'success' : 'info'" size="small">{{ row.status === 0 ? '正常' : '已隐藏' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 0" type="warning" size="small" @click="toggleStatus(row)">隐藏</el-button>
            <el-button v-else type="success" size="small" @click="toggleStatus(row)">恢复</el-button>
            <el-button type="danger" size="small" @click="doDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="flex justify-end mt-4">
        <el-pagination
          v-model:current-page="page"
          :page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="fetchTopics"
        />
      </div>
    </el-card>

    <!-- Create Dialog -->
    <el-dialog v-model="dialogVisible" title="创建官方话题" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="话题名称" required>
          <el-input v-model="form.name" placeholder="输入话题名称" maxlength="50" />
        </el-form-item>
        <el-form-item label="话题描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="描述话题内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="doCreate" :disabled="!form.name">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getTopics, createOfficialTopic, deleteTopic, toggleTopicStatus } from '@/api/topic'
import type { TopicVO } from '@/api/topic'
import PageHeader from '@/components/PageHeader.vue'

const topicList = ref<TopicVO[]>([])
const loading = ref(false)
const page = ref(1)
const pageSize = 20
const total = ref(0)
const dialogVisible = ref(false)
const form = ref({ name: '', description: '' })

async function fetchTopics() {
  loading.value = true
  try {
    const res = await getTopics(page.value, pageSize)
    topicList.value = res.data.list
    total.value = res.data.total
  } catch (e) { /* ignore */ }
  loading.value = false
}

function showCreateDialog() { form.value = { name: '', description: '' }; dialogVisible.value = true }

async function doCreate() {
  try {
    await createOfficialTopic(form.value.name, form.value.description)
    ElMessage.success('话题创建成功')
    dialogVisible.value = false
    fetchTopics()
  } catch (e) { /* ignore */ }
}

async function toggleStatus(topic: TopicVO) {
  const newStatus = topic.status === 0 ? 1 : 0
  try {
    await toggleTopicStatus(topic.id, newStatus)
    ElMessage.success('状态已更新')
    fetchTopics()
  } catch (e) { /* ignore */ }
}

async function doDelete(id: number) {
  try {
    await ElMessageBox.confirm('确定要删除该话题吗？', '确认删除', { type: 'warning' })
    await deleteTopic(id)
    ElMessage.success('删除成功')
    fetchTopics()
  } catch (e) { /* ignore */ }
}

onMounted(fetchTopics)
</script>
