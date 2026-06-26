$ErrorActionPreference = 'Continue'
$enc = [System.Text.UTF8Encoding]::new($false)
function W($P, $C) { $d = Split-Path $P -Parent; if (!(Test-Path $d)) { mkdir $d -Force | Out-Null }; [IO.File]::WriteAllText($P, $C, $enc); Write-Host "OK: $P" }
$ba = 'd:/zhixun/admin/src/api'
$bv = 'd:/zhixun/admin/src/views'
$br = 'd:/zhixun/admin/src/router'

# ============================================================
# API MODULES
# ============================================================
W "$ba/report.ts" @'
import { get, put } from './request'
import type { ApiResponse } from '@/types'

export interface ReportVO {
  id: number
  type: string
  targetId: number
  targetTitle: string
  reporterId: number
  reporterName: string
  reason: string
  description: string
  status: number
  handledBy?: number
  handlerName?: string
  handledAt?: string
  createdAt: string
}

export interface PageResult<T> {
  list: T[]
  total: number
  page: number
  pageSize: number
}

export function getPendingReports(type: string, page = 1, pageSize = 20): Promise<ApiResponse<PageResult<ReportVO>>> {
  return get('/reports/pending', { type, page, pageSize } as any)
}

export function handleReport(id: number, status: number, type = 'article'): Promise<ApiResponse<void>> {
  return put(`/reports/${id}/handle`, { status, type } as any)
}
'@

W "$ba/topic.ts" @'
import { get, post, put, del } from './request'
import type { ApiResponse } from '@/types'

export interface TopicVO {
  id: number
  name: string
  description: string
  coverImage: string
  articleCount: number
  followCount: number
  hotScore: number
  isOfficial: boolean
  isFollowed: boolean
}

export interface PageResult<T> {
  list: T[]
  total: number
  page: number
  pageSize: number
}

export function getTopics(page = 1, pageSize = 20): Promise<ApiResponse<PageResult<TopicVO>>> {
  return get('/topics', { page, pageSize, orderBy: 'hot' } as any)
}

export function createOfficialTopic(name: string, description?: string): Promise<ApiResponse<number>> {
  return post('/topics', { name, description, isOfficial: true } as any)
}

export function deleteTopic(id: number): Promise<ApiResponse<void>> {
  return del(`/topics/${id}`)
}

export function toggleTopicStatus(id: number, status: number): Promise<ApiResponse<void>> {
  return put(`/topics/${id}/status`, { status } as any)
}
'@

W "$ba/template.ts" @'
import { get, post, del } from './request'
import type { ApiResponse } from '@/types'

export interface TemplateVO {
  id: number
  name: string
  description: string
  coverImage: string
  category: string
  content: string
  tags: string
  useCount: number
  creatorName: string
}

export interface PageResult<T> {
  list: T[]
  total: number
  page: number
  pageSize: number
}

export function getTemplates(page = 1, pageSize = 20): Promise<ApiResponse<PageResult<TemplateVO>>> {
  return get('/templates', { page, pageSize } as any)
}

export function createOfficialTemplate(data: { name: string; description?: string; content: string; category?: string }): Promise<ApiResponse<number>> {
  return post('/templates', data as any)
}

export function deleteTemplate(id: number): Promise<ApiResponse<void>> {
  return del(`/templates/${id}`)
}
'@

W "$ba/badge.ts" @'
import { get, post, del } from './request'
import type { ApiResponse } from '@/types'

export interface BadgeVO {
  id: number
  name: string
  description: string
  icon: string
  category: string
}

export interface PageResult<T> {
  list: T[]
  total: number
  page: number
  pageSize: number
}

export function getAllBadges(): Promise<ApiResponse<BadgeVO[]>> {
  return get('/incentive/badges' as any)
}

export function createBadge(data: { name: string; description: string; icon?: string; category: string; condition?: string }): Promise<ApiResponse<number>> {
  return post('/admin/badges', data as any)
}

export function deleteBadge(id: number): Promise<ApiResponse<void>> {
  return del(`/admin/badges/${id}`)
}
'@

# ============================================================
# VIEWS - REPORT MANAGEMENT (举报管理)
# ============================================================
W "$bv/reports/index.vue" @'
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
            <el-button type="success" size="small" @click="handleReport(row.id, 1)">忽略</el-button>
            <el-button type="danger" size="small" @click="handleReport(row.id, 2)">处理</el-button>
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
    await ElMessageBox.confirm(`确定要${actionText}？`, '确认操作', { type: 'warning' })
    await handleReport(id, status, activeTab.value)
    ElMessage.success('处理成功')
    fetchReports()
  } catch (e) { /* cancelled or error */ }
}

onMounted(fetchReports)
</script>
'@

# ============================================================
# VIEWS - TOPIC MANAGEMENT (话题管理)
# ============================================================
W "$bv/topics/index.vue" @'
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
    await ElMessageBox.confirm('确定要删除该话题？', '确认删除', { type: 'warning' })
    await deleteTopic(id)
    ElMessage.success('删除成功')
    fetchTopics()
  } catch (e) { /* ignore */ }
}

onMounted(fetchTopics)
</script>
'@

# ============================================================
# VIEWS - TEMPLATE MANAGEMENT (模板管理)
# ============================================================
W "$bv/templates/index.vue" @'
<template>
  <div class="templates-page">
    <PageHeader title="模板管理" description="管理内容创作模板，创建官方模板">
      <template #actions>
        <el-button type="primary" @click="showCreateDialog">创建官方模板</el-button>
      </template>
    </PageHeader>

    <el-card shadow="never">
      <el-table :data="templateList" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="name" label="模板名称" min-width="150" />
        <el-table-column prop="category" label="分类" width="100">
          <template #default="{ row }">
            <el-tag size="small">{{ row.category || '未分类' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="useCount" label="使用次数" width="100" />
        <el-table-column prop="creatorName" label="创建者" width="100" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="previewTemplate(row)">预览</el-button>
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
          @current-change="fetchTemplates"
        />
      </div>
    </el-card>

    <!-- Create Dialog -->
    <el-dialog v-model="dialogVisible" title="创建官方模板" width="600px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="模板名称" required>
          <el-input v-model="form.name" placeholder="输入模板名称" maxlength="50" />
        </el-form-item>
        <el-form-item label="分类">
          <el-input v-model="form.category" placeholder="如：新闻、博客、公告" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="模板描述" />
        </el-form-item>
        <el-form-item label="模板内容" required>
          <el-input v-model="form.content" type="textarea" :rows="8" placeholder="Markdown 格式的模板内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="doCreate" :disabled="!form.name || !form.content">创建</el-button>
      </template>
    </el-dialog>

    <!-- Preview Dialog -->
    <el-dialog v-model="previewVisible" title="模板预览" width="700px">
      <div class="prose max-w-none bg-gray-50 rounded-lg p-4" v-html="previewContent" />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getTemplates, createOfficialTemplate, deleteTemplate } from '@/api/template'
import type { TemplateVO } from '@/api/template'
import PageHeader from '@/components/PageHeader.vue'

const templateList = ref<TemplateVO[]>([])
const loading = ref(false)
const page = ref(1)
const pageSize = 20
const total = ref(0)
const dialogVisible = ref(false)
const previewVisible = ref(false)
const previewContent = ref('')
const form = ref({ name: '', description: '', content: '', category: '' })

async function fetchTemplates() {
  loading.value = true
  try {
    const res = await getTemplates(page.value, pageSize)
    templateList.value = res.data.list
    total.value = res.data.total
  } catch (e) { /* ignore */ }
  loading.value = false
}

function showCreateDialog() { form.value = { name: '', description: '', content: '', category: '' }; dialogVisible.value = true }

async function doCreate() {
  try {
    await createOfficialTemplate(form.value)
    ElMessage.success('模板创建成功')
    dialogVisible.value = false
    fetchTemplates()
  } catch (e) { /* ignore */ }
}

function previewTemplate(tpl: TemplateVO) {
  previewContent.value = tpl.content.replace(/\n/g, '<br/>')
  previewVisible.value = true
}

async function doDelete(id: number) {
  try {
    await ElMessageBox.confirm('确定要删除该模板？', '确认删除', { type: 'warning' })
    await deleteTemplate(id)
    ElMessage.success('删除成功')
    fetchTemplates()
  } catch (e) { /* ignore */ }
}

onMounted(fetchTemplates)
</script>
'@

# ============================================================
# VIEWS - BADGE MANAGEMENT (徽章管理)
# ============================================================
W "$bv/badges/index.vue" @'
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
    await ElMessageBox.confirm('确定要删除该徽章？', '确认删除', { type: 'warning' })
    await deleteBadge(id)
    ElMessage.success('删除成功')
    fetchBadges()
  } catch (e) { /* ignore */ }
}

onMounted(fetchBadges)
</script>
'@

Write-Host "=== Views created ==="
