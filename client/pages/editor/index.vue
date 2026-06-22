<template>
  <!-- 文章编辑器 -->
  <div class="max-w-4xl mx-auto px-4 py-6">
    <h1 class="text-2xl font-bold text-gray-900 dark:text-white mb-6">写文章</h1>

    <!-- 标题输入 -->
    <input
      v-model="form.title"
      type="text"
      class="input text-2xl font-bold mb-6"
      placeholder="请输入文章标题..."
    />

    <!-- 富文本编辑器（Markdown模式） -->
    <div class="mb-6">
      <div class="border border-gray-300 dark:border-gray-600 rounded-lg overflow-hidden">
        <!-- 工具栏 -->
        <div class="flex items-center space-x-1 p-2 bg-gray-50 dark:bg-gray-700 border-b border-gray-300 dark:border-gray-600">
          <button class="p-2 text-gray-600 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-600 rounded" title="加粗" @click="insertMarkdown('**', '**')">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="3" d="M6 4h8a4 4 0 014 4 4 4 0 01-4 4H6z M6 12h9a4 4 0 014 4 4 4 0 01-4 4H6z" /></svg>
          </button>
          <button class="p-2 text-gray-600 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-600 rounded" title="斜体" @click="insertMarkdown('*', '*')">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="3" d="M10 4h4m-2 0v16m-4 0h8" /></svg>
          </button>
          <button class="p-2 text-gray-600 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-600 rounded" title="标题" @click="insertMarkdown('## ', '')">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h8m-8 6h16" /></svg>
          </button>
          <button class="p-2 text-gray-600 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-600 rounded" title="链接" @click="insertMarkdown('[', '](url)')">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13.828 10.172a4 4 0 00-5.656 0l-4 4a4 4 0 105.656 5.656l1.102-1.101m-.758-4.899a4 4 0 005.656 0l4-4a4 4 0 00-5.656-5.656l-1.1 1.1" /></svg>
          </button>
          <button class="p-2 text-gray-600 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-600 rounded" title="图片" @click="insertMarkdown('![alt](', ')')">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" /></svg>
          </button>
          <button class="p-2 text-gray-600 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-600 rounded" title="代码" @click="insertMarkdown('`', '`')">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 20l4-16m4 4l4 4-4 4M6 16l-4-4 4-4" /></svg>
          </button>
        </div>

        <!-- 编辑区域 -->
        <textarea
          ref="editorRef"
          v-model="form.content"
          class="w-full min-h-[400px] p-4 bg-white dark:bg-gray-800 text-gray-900 dark:text-white resize-none outline-none font-mono text-sm"
          placeholder="开始写作..."
        ></textarea>
      </div>
    </div>

    <!-- 分类和标签选择 -->
    <div class="grid grid-cols-1 md:grid-cols-2 gap-4 mb-6">
      <!-- 分类选择 -->
      <div>
        <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">分类</label>
        <select v-model="form.categoryId" class="input">
          <option value="">请选择分类</option>
          <option v-for="category in categories" :key="category.id" :value="category.id">
            {{ category.name }}
          </option>
        </select>
      </div>

      <!-- 标签选择 -->
      <div>
        <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">标签</label>
        <input
          v-model="tagInput"
          type="text"
          class="input"
          placeholder="输入标签，按回车添加"
          @keydown.enter.prevent="addTag"
        />
        <div v-if="form.tags.length" class="flex flex-wrap gap-2 mt-2">
          <span v-for="tag in form.tags" :key="tag" class="badge-primary flex items-center space-x-1">
            <span>{{ tag }}</span>
            <button class="hover:text-danger" @click="removeTag(tag)">×</button>
          </span>
        </div>
      </div>
    </div>

    <!-- 封面图上传 -->
    <div class="mb-6">
      <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">封面图</label>
      <div class="flex items-center space-x-4">
        <div v-if="form.coverImage" class="relative w-40 h-28 rounded-lg overflow-hidden">
          <img :src="form.coverImage" alt="封面" class="w-full h-full object-cover" />
          <button class="absolute top-1 right-1 w-6 h-6 bg-black/50 text-white rounded-full flex items-center justify-center text-xs" @click="form.coverImage = ''">×</button>
        </div>
        <label class="btn-secondary cursor-pointer">
          <span>选择图片</span>
          <input type="file" accept="image/*" class="hidden" @change="handleCoverUpload" />
        </label>
      </div>
    </div>

    <!-- 操作按钮 -->
    <div class="flex items-center justify-end space-x-4">
      <button class="btn-ghost" @click="saveDraft">保存草稿</button>
      <button class="btn-primary" :disabled="!canPublish" @click="publishArticle">发布文章</button>
    </div>
  </div>
</template>

<script setup lang="ts">
/** 文章编辑器页 */
import type { Category, Tag } from '~/types'

definePageMeta({
  middleware: 'auth',
})

// 表单数据
const form = reactive({
  title: '',
  content: '',
  categoryId: '',
  tags: [] as string[],
  coverImage: '',
})

const tagInput = ref('')
const categories = ref<Category[]>([])
const editorRef = ref<HTMLTextAreaElement | null>(null)

// 是否可以发布
const canPublish = computed(() => {
  return form.title.trim() && form.content.trim() && form.categoryId
})

// 添加标签
const addTag = () => {
  const tag = tagInput.value.trim()
  if (tag && !form.tags.includes(tag)) {
    form.tags.push(tag)
  }
  tagInput.value = ''
}

// 移除标签
const removeTag = (tag: string) => {
  form.tags = form.tags.filter((t) => t !== tag)
}

// 插入Markdown语法
const insertMarkdown = (prefix: string, suffix: string) => {
  const textarea = editorRef.value
  if (!textarea) return

  const start = textarea.selectionStart
  const end = textarea.selectionEnd
  const selectedText = form.content.substring(start, end)

  form.content =
    form.content.substring(0, start) +
    prefix + selectedText + suffix +
    form.content.substring(end)

  // 恢复光标位置
  nextTick(() => {
    textarea.focus()
    textarea.setSelectionRange(start + prefix.length, start + prefix.length + selectedText.length)
  })
}

// 封面图上传
const handleCoverUpload = (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return
  // TODO: 调用上传接口
  const reader = new FileReader()
  reader.onload = (e) => {
    form.coverImage = e.target?.result as string
  }
  reader.readAsDataURL(file)
}

// 保存草稿
const saveDraft = async () => {
  try {
    const { articleApi } = await import('~/api')
    await articleApi.createArticle({ ...form, status: 0 } as any)
    ElMessage?.success?.('草稿已保存') || alert('草稿已保存')
  } catch (error: any) {
    alert(error.message || '保存失败')
  }
}

// 发布文章
const publishArticle = async () => {
  if (!canPublish.value) return
  try {
    const { articleApi } = await import('~/api')
    const response = await articleApi.createArticle({ ...form, status: 1 } as any)
    navigateTo(`/articles/${response.data.data.id}`)
  } catch (error: any) {
    alert(error.message || '发布失败')
  }
}

// 页面元信息
useHead({
  title: '写文章 - 知讯',
})
</script>
