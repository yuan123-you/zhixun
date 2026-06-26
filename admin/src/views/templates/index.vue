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
    await ElMessageBox.confirm('确定要删除该模板吗？', '确认删除', { type: 'warning' })
    await deleteTemplate(id)
    ElMessage.success('删除成功')
    fetchTemplates()
  } catch (e) { /* ignore */ }
}

onMounted(fetchTemplates)
</script>
