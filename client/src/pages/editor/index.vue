<template>
  <!-- 作品编辑器 - 优化版 -->
  <div class="max-w-[1200px] 2xl:max-w-[1400px] mx-auto px-1.5 2xl:px-2 py-1.5 animate-fade-in-up">

    <!-- 页面标题栏 - 渐变背景 -->
    <div class="editor-header relative overflow-hidden rounded-xl mb-1.5 md:mb-2 px-3 py-2 md:px-4 md:py-3">
      <!-- 渐变背景装饰 -->
      <div class="absolute inset-0 bg-gradient-to-r from-primary/8 via-primary/5 to-transparent pointer-events-none"></div>
      <div class="absolute top-0 right-0 w-32 h-32 bg-gradient-to-bl from-primary/10 via-transparent to-transparent rounded-full blur-3xl pointer-events-none"></div>
      <!-- 内容 -->
      <div class="relative flex items-center justify-between">
        <div class="flex items-center gap-2">
          <div class="w-8 h-8 md:w-9 md:h-9 rounded-lg bg-gradient-to-br from-primary to-primary-dark flex items-center justify-center shadow-lg shadow-primary/20 flex-shrink-0">
            <svg class="w-4 h-4 md:w-5 md:h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
            </svg>
          </div>
          <div>
            <h1 class="text-base md:text-lg font-bold text-[var(--zh-text)]">创作中心</h1>
            <div class="hidden sm:flex items-center gap-1 mt-0.5">
              <span class="w-1.5 h-1.5 rounded-full" :class="writingStatusDotClass"></span>
              <span class="text-[10px]" :class="writingStatusTextClass">{{ writingStatusText }}</span>
            </div>
          </div>
        </div>
        <!-- 字数统计 -->
        <div class="hidden sm:flex items-center gap-2 text-xs text-[var(--zh-text-tertiary)]">
          <div class="text-right">
            <div class="text-sm font-bold text-[var(--zh-text)] leading-none">{{ form.content.length }}</div>
            <div class="text-[9px] mt-0.5">正文</div>
          </div>
          <span class="w-px h-6 bg-[var(--zh-border)]"></span>
          <div class="text-right">
            <div class="text-sm font-bold text-primary leading-none">{{ totalCharCount }}</div>
            <div class="text-[9px] mt-0.5">总计</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 编辑器区域 -->
    <div class="mb-1.5">
      <div class="editor-container editor-container-glow border border-[var(--zh-border)] rounded-xl shadow-[var(--zh-shadow-sm)] transition-all duration-300 hover:shadow-[var(--zh-shadow-md)]">
        <!-- 工具栏容器 - 两行布局 -->
        <div class="flex flex-col">
          <!-- 主工具栏 - 文本格式 + 插入 -->
          <div class="editor-toolbar flex items-center gap-0.5 px-1 py-0 bg-[var(--zh-bg)] border-b border-[var(--zh-border)] flex-nowrap overflow-x-auto scrollbar-thin">
          <!-- 文本格式组 -->
          <div class="toolbar-group shrink-0">
            <button class="toolbar-btn" data-tooltip="加粗 Ctrl+B" @click="insertMarkdown('**', '**')">
              <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="3" d="M6 4h8a4 4 0 014 4 4 4 0 01-4 4H6z M6 12h9a4 4 0 014 4 4 4 0 01-4 4H6z" /></svg>
              <span class="toolbar-kbd">B</span>
            </button>
            <button class="toolbar-btn" data-tooltip="斜体 Ctrl+I" @click="insertMarkdown('*', '*')">
              <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="3" d="M10 4h4m-2 0v16m-4 0h8" /></svg>
              <span class="toolbar-kbd">I</span>
            </button>
            <button class="toolbar-btn" data-tooltip="标题 H2" @click="insertMarkdown('## ', '')">
              <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h8m-8 6h16" /></svg>
            </button>
            <button class="toolbar-btn" data-tooltip="行内代码" @click="insertMarkdown('`', '`')">
              <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 20l4-16m4 4l4 4-4 4M6 16l-4-4 4-4" /></svg>
            </button>
          </div>

          <span class="toolbar-divider shrink-0"></span>

          <!-- 插入组 -->
          <div class="toolbar-group shrink-0">
            <button class="toolbar-btn" data-tooltip="插入链接" @click="insertMarkdown('[', '](url)')">
              <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13.828 10.172a4 4 0 00-5.656 0l-4 4a4 4 0 105.656 5.656l1.102-1.101m-.758-4.899a4 4 0 005.656 0l4-4a4 4 0 00-5.656-5.656l-1.1 1.1" /></svg>
            </button>
            <label class="toolbar-btn cursor-pointer" :class="{ 'opacity-50 pointer-events-none': imageUploading }" data-tooltip="上传图片到正文">
              <svg v-if="!imageUploading" class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" /></svg>
              <span v-else class="inline-block w-3 h-3 border-2 border-primary border-t-transparent rounded-full animate-spin"></span>
              <input type="file" accept="image/jpeg,image/png,image/gif,image/webp,image/bmp" class="hidden" :disabled="imageUploading" @change="handleEditorImageUpload" />
            </label>
            <EmojiPicker @select="(emoji: string) => insertAtCursor(emoji)" />
          </div>

          <!-- 右侧：自动保存指示器 -->
          <div class="ml-auto hidden md:flex items-center gap-1.5 shrink-0 pl-1">
            <div v-if="autoSavePending" class="flex items-center gap-0.5 text-[9px] text-[var(--zh-text-tertiary)]">
              <span class="w-1 h-1 rounded-full bg-amber-400 animate-breathe"></span>
              <span>保存中</span>
            </div>
            <div v-else-if="lastAutoSaveTime" class="flex items-center gap-0.5 text-[9px] text-[var(--zh-text-tertiary)]">
              <svg class="w-2 h-2 text-green-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M5 13l4 4L19 7"/></svg>
              <span>已保存 {{ lastAutoSaveTime }}</span>
            </div>
          </div>
        </div>

          <!-- AI工具栏 - 独立第二行 -->
          <div class="ai-toolbar flex items-center gap-1 px-1 py-0.5 bg-[var(--zh-bg)] border-b border-[var(--zh-border)]">
            <span class="toolbar-ai-label">AI</span>
            <button class="ai-tool-btn ai-tool-expand" :disabled="aiLoading" @click="handleAIWrite('expand')" data-tooltip="选中文本后AI帮你扩展完善内容">
              <svg class="w-2 h-2" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z"/></svg>
              扩写
            </button>
            <button class="ai-tool-btn ai-tool-polish" :disabled="aiLoading" @click="handleAIWrite('polish')" data-tooltip="选中文本后AI帮你优化文笔表达">润色</button>
            <button class="ai-tool-btn ai-tool-review" :disabled="aiLoading" @click="handleAIWrite('review')" data-tooltip="选中文本后AI帮你检查内容合规性">审核</button>
            <!-- AI加载指示器 -->
            <div v-if="aiLoading" class="ml-auto flex items-center gap-0.5 text-[9px] text-purple-600">
              <span class="w-2 h-2 border-2 border-purple-400 border-t-transparent rounded-full animate-spin"></span>
              <span>AI处理中</span>
            </div>
          </div>
        </div>

        <!-- 已上传图片缩略图列表 -->
        <div v-if="form.images.length > 0" class="flex items-center gap-1.5 px-3 py-1.5 bg-[var(--zh-bg)] border-b border-[var(--zh-border)] overflow-x-auto">
          <div
            v-for="(img, idx) in form.images"
            :key="idx"
            class="thumb-card relative w-12 h-12 shrink-0 group cursor-pointer"
            @click="insertImageAtCursor(img)"
            title="点击插入到正文"
          >
            <img :src="resolveUrl(img) || img" alt="" class="w-full h-full object-cover rounded" />
            <div class="absolute inset-0 bg-black/0 group-hover:bg-black/30 transition-colors duration-200 flex items-center justify-center rounded">
              <span class="text-white text-[10px] opacity-0 group-hover:opacity-100 transition-opacity">插入</span>
            </div>
            <button
              class="absolute -top-1 -right-1 w-4 h-4 bg-red-500 text-white rounded-full flex items-center justify-center text-[10px] opacity-0 group-hover:opacity-100 transition-all duration-200 shadow-sm hover:bg-red-600"
              @click.stop="removeImage(idx)"
            >×</button>
          </div>
          <label
            v-if="form.images.length < 9"
            class="thumb-add w-12 h-12 shrink-0 border-2 border-dashed border-[var(--zh-border)] rounded-lg flex items-center justify-center text-[var(--zh-text-tertiary)] hover:border-primary hover:text-primary cursor-pointer transition-all duration-200 hover:bg-primary-50/50"
            :class="{ 'opacity-50 pointer-events-none': imageUploading }"
          >
            <svg v-if="!imageUploading" class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" /></svg>
            <span v-else class="inline-block w-3.5 h-3.5 border-2 border-primary border-t-transparent rounded-full animate-spin"></span>
            <input type="file" accept="image/jpeg,image/png,image/gif,image/webp,image/bmp" class="hidden" :disabled="imageUploading" @change="handleEditorImageUpload" />
          </label>
        </div>

        <!-- 编辑区（已拆分为独立页面，预览区请通过"预览效果"按钮在新页面打开） -->
        <div>
          <div class="hidden md:flex items-center justify-between px-3 py-1 bg-[var(--zh-bg)] border-b border-[var(--zh-border)] text-xs text-[var(--zh-text-secondary)] font-medium">
            <span class="flex items-center gap-1">
              <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" /></svg>
              编辑
            </span>
            <span class="text-[var(--zh-text-tertiary)]">Markdown 语法支持 · <a href="/editor/preview" target="_blank" class="text-primary hover:underline">新页面预览</a></span>
          </div>
          <div class="flex flex-col">
            <input
              v-model="form.title"
              type="text"
              class="title-input w-full px-3 md:px-4 pt-2 pb-1.5 md:pt-2 md:pb-1.5 text-base md:text-xl font-bold text-[var(--zh-text)] bg-[var(--zh-bg-elevated)] outline-none placeholder:text-gray-300 dark:placeholder:text-gray-600 border-b border-[var(--zh-border-light)] transition-all duration-300"
              placeholder="请输入作品标题..."
              maxlength="50"
            />
            <div class="flex items-center justify-between px-3 md:px-4 py-0.5 bg-[var(--zh-bg-elevated)]">
              <span class="text-[10px] text-[var(--zh-text-tertiary)]">{{ form.title.length }}/50</span>
            </div>
            <MentionInput
              ref="editorRef"
              v-model="form.content"
              placeholder="开始写作..."
              :rows="isMobile ? 4 : 14"
              class="editor-textarea w-full min-h-[100px] md:min-h-[300px] p-2 md:p-4 bg-[var(--zh-bg-elevated)] text-[var(--zh-text)] resize-none outline-none font-mono text-sm leading-relaxed"
            />
          </div>
        </div>
      </div>
    </div>

    <!-- 分类和标签选择 -->
    <div class="grid grid-cols-1 md:grid-cols-2 gap-1.5 md:gap-2 mb-1.5 md:mb-2">
      <div>
        <label class="block text-xs md:text-sm font-medium text-[var(--zh-text-secondary)] mb-1 flex items-center gap-1">
          <svg class="w-3.5 h-3.5 text-[var(--zh-text-tertiary)]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A1.994 1.994 0 013 12V7a4 4 0 014-4z" />
          </svg>
          分类
        </label>
        <input
          v-model="form.categoryName"
          type="text"
          class="input !h-9 !text-sm !px-3"
          placeholder="填写分类名称（选填，不填则默认分类）"
          maxlength="20"
        />
      </div>

      <div>
        <label class="block text-xs md:text-sm font-medium text-[var(--zh-text-secondary)] mb-1 flex items-center gap-1">
          <svg class="w-3.5 h-3.5 text-[var(--zh-text-tertiary)]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A1.994 1.994 0 013 12V7a4 4 0 014-4z" />
          </svg>
          标签
        </label>
        <div class="flex gap-2">
          <input
            v-model="tagInput"
            type="text"
            class="input !h-9 !text-sm !px-3 flex-1"
            placeholder="添加标签"
            @keydown.enter.prevent="addTag"
          />
          <button
            class="px-3 py-1.5 text-sm bg-[var(--zh-bg-hover)] hover:bg-primary-50 hover:text-primary rounded-lg transition-colors font-medium border border-[var(--zh-border)]"
            @click="addTag"
            :disabled="!tagInput.trim()"
          >添加</button>
        </div>
        <div v-if="form.tags.length" class="flex flex-wrap gap-1 mt-1.5">
          <span v-for="tag in form.tags" :key="tag" class="inline-flex items-center gap-1 px-2 py-0.5 text-xs font-medium bg-primary-50 text-primary-700 rounded-full border border-primary-100">
            <span># {{ tag }}</span>
            <button class="hover:text-danger transition-colors" @click="removeTag(tag)">×</button>
          </span>
        </div>
      </div>
    </div>

    <!-- 发布位置 -->
    <div class="mb-1.5 md:mb-2">
      <label class="block text-xs font-medium text-[var(--zh-text-secondary)] mb-1 flex items-center gap-1">
        <svg class="w-3.5 h-3.5 text-[var(--zh-text-tertiary)]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
        </svg>
        发布位置
        <span class="text-[10px] text-[var(--zh-text-tertiary)] font-normal ml-1">(选填)</span>
      </label>
      <RegionSelector
        ref="regionSelectorRef"
        :locating="locating"
        :locating-text="locatingStageText"
        :show-auto-locate="true"
        :locate-error="locateErrorText"
        @change="onRegionChange"
        @auto-locate="autoLocate"
      />
    </div>

    <!-- 操作按钮区域 - 优化版 -->
    <div class="sticky bottom-0 md:static bg-[var(--zh-bg)]/95 backdrop-blur-sm md:bg-transparent md:backdrop-blur-none -mx-1.5 px-1.5 py-2 md:mx-0 md:px-0 md:py-0 border-t md:border-t-0 border-[var(--zh-border)]">
      <div class="flex flex-col md:flex-row md:items-center md:justify-between gap-2">
        <!-- 左侧：定时发布 -->
        <div class="flex items-center gap-2">
          <label class="relative inline-flex items-center cursor-pointer" title="开启后可设置作品在指定时间自动发布">
            <input type="checkbox" v-model="scheduledPublish" class="sr-only peer" />
            <div class="w-8 h-4 bg-slate-300 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-[var(--zh-bg-elevated)] after:rounded-full after:h-3 after:w-3 after:transition-all peer-checked:bg-primary"></div>
          </label>
          <span class="text-xs text-[var(--zh-text-secondary)] select-none">定时发布</span>
          <input
            v-if="scheduledPublish"
            type="datetime-local"
            v-model="publishAt"
            class="input !h-8 !text-xs !w-auto"
            :min="minPublishAt"
          />
        </div>

        <!-- 右侧：操作按钮 -->
        <div class="flex items-center gap-1.5">
          <button class="btn-secondary text-xs" @click="saveDraft" :disabled="saving">
            <span v-if="!saving" class="flex items-center gap-1">
              <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7H5a2 2 0 00-2 2v9a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-3m-1 4l-3 3m0 0l-3-3m3 3V4"/></svg>
              保存草稿
            </span>
            <span v-else class="flex items-center gap-1">
              <span class="inline-block w-3.5 h-3.5 border-2 border-slate-400 border-t-transparent rounded-full animate-spin"></span>
              保存中...
            </span>
          </button>
          <button class="btn-secondary text-xs" @click="openPreview">
            <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" /><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" /></svg>
            预览
          </button>
          <button class="btn-primary text-xs" :disabled="!canPublish || publishing" @click="publishArticle">
            <span v-if="!publishing" class="flex items-center gap-1">
              <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8"/></svg>
              {{ scheduledPublish ? '定时发布' : '发布' }}
            </span>
            <span v-else class="flex items-center gap-1">
              <span class="inline-block w-3.5 h-3.5 border-2 border-white border-t-transparent rounded-full animate-spin"></span>
              {{ scheduledPublish ? '设置中...' : '发布中...' }}
            </span>
          </button>
        </div>
      </div>
      <p v-if="scheduledPublish && publishAt" class="text-xs text-[var(--zh-text-tertiary)] mt-2 text-right">
        <svg class="w-3.5 h-3.5 inline mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>
        作品将在 {{ publishAt }} 自动发布
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
/** 作品编辑器页 - 优化版 */
import { useLocation } from '@/composables/useLocation'
import { useBreakpoints } from '@/composables/useBreakpoints'
import { showToast } from '@/composables/useToast'
import { aiApi } from '@/api/ai'

const router = useRouter()
const { resolveUrl } = useResourceUrl()
const { post: apiPost } = useApi()
const { isMobile } = useBreakpoints()

// 写作状态
const lastEditTime = ref(Date.now())
const writingStatusText = computed(() => {
  const diff = Date.now() - lastEditTime.value
  if (diff < 30000) return '正在编辑'
  if (diff < 120000) return '刚刚编辑'
  return '空闲中'
})
const writingStatusTextClass = computed(() => {
  const diff = Date.now() - lastEditTime.value
  if (diff < 30000) return 'text-green-600 dark:text-green-400'
  return 'text-[var(--zh-text-tertiary)]'
})
const writingStatusDotClass = computed(() => {
  const diff = Date.now() - lastEditTime.value
  if (diff < 30000) return 'bg-green-500 animate-pulse'
  return 'bg-[var(--zh-text-tertiary)]'
})

// 总字数
const totalCharCount = computed(() => {
  return form.title.length + form.content.length + form.summary.length
})

// 表单数据
const form = reactive({
  title: '',
  content: '',
  summary: '',
  categoryName: '',
  tags: [] as string[],
  images: [] as string[],
  location: '',
})

// 监听内容变化，更新编辑时间
watch(() => form.content, () => { lastEditTime.value = Date.now() })
watch(() => form.title, () => { lastEditTime.value = Date.now() })

// 自动保存
const autoSavePending = ref(false)
const lastAutoSaveTime = ref('')
let autoSaveTimer: ReturnType<typeof setTimeout> | null = null

const triggerAutoSave = () => {
  // 必须同时有标题和内容才允许自动保存（后端 @NotBlank 校验会拒绝 400）
  if (!form.title.trim() || !form.content.trim()) return
  if (autoSaveTimer) clearTimeout(autoSaveTimer)
  autoSaveTimer = setTimeout(async () => {
    autoSavePending.value = true
    try {
      const requestData = buildArticleRequest(0)
      await apiPost<any>('/articles', requestData)
      const now = new Date()
      lastAutoSaveTime.value = `${now.getHours().toString().padStart(2, '0')}:${now.getMinutes().toString().padStart(2, '0')}`
    } catch {
      // 静默失败，自动保存不打断用户
    } finally {
      autoSavePending.value = false
    }
  }, 3000) // 3秒防抖
}

watch(() => [form.title, form.content], () => {
  triggerAutoSave()
}, { deep: false })

onUnmounted(() => {
  if (autoSaveTimer) clearTimeout(autoSaveTimer)
})

const tagInput = ref('')
const editorRef = ref<any>(null)
const locating = ref(false)
const saving = ref(false)
const publishing = ref(false)

/** 自动定位 composable（统一封装腾讯地图 IP 定位 / 浏览器 GPS 逆地理编码） */
const locationApi = useLocation()

/** 定位失败时的展示文案（用于在 RegionSelector 下方显示） */
const locateErrorText = ref('')

/** 定位阶段提示文案 */
const locatingStageText = computed(() => {
  switch (locationApi.stage.value) {
    case 'gps': return '正在获取位置...'
    case 'reverse': return '正在解析地址...'
    case 'ip': return '正在通过 IP 定位...'
    default: return '获取中...'
  }
})

// 定时发布
const scheduledPublish = ref(false)
const publishAt = ref('')

const minPublishAt = computed(() => {
  const now = new Date()
  const offset = now.getTimezoneOffset()
  const local = new Date(now.getTime() - offset * 60000)
  return local.toISOString().slice(0, 16)
})

const canPublish = computed(() => {
  return form.title.trim() && form.content.trim()
})

// 添加标签
const addTag = () => {
  const tag = tagInput.value.trim()
  if (tag && !form.tags.includes(tag)) {
    form.tags.push(tag)
  }
  tagInput.value = ''
}

const removeTag = (tag: string) => {
  form.tags = form.tags.filter((t) => t !== tag)
}

// 获取当前聚焦的编辑器 textarea 引用（确保返回原生 HTMLTextAreaElement）
const getEditorTextarea = (): HTMLTextAreaElement | null => {
  const componentEl = editorRef.value
  if (!componentEl) {
    const editorArea = document.querySelector('.editor-textarea textarea') as HTMLTextAreaElement | null
    if (editorArea) return editorArea
    return document.querySelector('textarea')
  }
  // MentionInput 暴露的 textareaRef 实际是 el-input 组件实例
  const elInputComp = (componentEl as any)?.textareaRef
  if (elInputComp) {
    // el-input 的 .textarea 属性指向原生 textarea DOM
    if (elInputComp.textarea instanceof HTMLTextAreaElement) return elInputComp.textarea
    // 从 el-input 的 DOM 根元素查找
    const fromEl = elInputComp.$el?.querySelector?.('textarea')
    if (fromEl) return fromEl
  }
  const el = (componentEl as any)?.$el
  if (el) {
    if (el.tagName === 'TEXTAREA') return el
    const nested = el.querySelector?.('textarea')
    if (nested) return nested
  }
  // 全局兜底：查找 .editor-textarea 内的 textarea
  return document.querySelector('.editor-textarea textarea') ?? document.querySelector('textarea')
}

// 插入Markdown语法
const insertMarkdown = (prefix: string, suffix: string) => {
  const textarea = getEditorTextarea()
  if (!textarea) return
  const start = textarea.selectionStart ?? 0
  const end = textarea.selectionEnd ?? 0
  const selectedText = form.content.substring(start, end)

  form.content =
    form.content.substring(0, start) +
    prefix + selectedText + suffix +
    form.content.substring(end)

  nextTick(() => {
    const ta = getEditorTextarea()
    ta?.focus()
    if (ta && typeof ta.setSelectionRange === 'function') {
      ta.setSelectionRange(start + prefix.length, start + prefix.length + selectedText.length)
    }
  })
}

// 在光标处插入文本
const insertAtCursor = (text: string) => {
  const textarea = getEditorTextarea()
  if (!textarea) return
  const pos = textarea?.selectionStart ?? form.content.length
  form.content = form.content.substring(0, pos) + text + form.content.substring(pos)
  nextTick(() => {
    const ta = getEditorTextarea()
    ta?.focus()
    if (ta && typeof ta.setSelectionRange === 'function') {
      ta.setSelectionRange(pos + text.length, pos + text.length)
    }
  })
}

// 编辑器内图片上传
const imageUploading = ref(false)
const imageUploadProgress = ref(0)
let imageAbortController: AbortController | null = null

// 允许的图片 MIME 类型
const EDITOR_ALLOWED_IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/gif', 'image/webp', 'image/bmp']

const handleEditorImageUpload = async (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return

  // 数量限制
  if (form.images.length >= 9) {
    showToast('最多只能上传 9 张图片，请先删除部分图片再试', 'error')
    target.value = ''
    return
  }

  // 文件类型校验
  if (!EDITOR_ALLOWED_IMAGE_TYPES.includes(file.type)) {
    const ext = file.name.split('.').pop()?.toLowerCase() || '该格式'
    showToast(`暂不支持 ${ext} 格式的图片，请使用 JPG、PNG、GIF、WebP 或 BMP 格式`, 'error')
    target.value = ''
    return
  }

  // 文件大小校验
  if (file.size > 5 * 1024 * 1024) {
    const sizeMB = (file.size / (1024 * 1024)).toFixed(1)
    showToast(`图片太大啦（${sizeMB}MB），单张图片不能超过 5MB，请压缩后再试`, 'error')
    target.value = ''
    return
  }

  // 文件名安全校验
  if (!file.name || file.name.length > 255) {
    showToast('文件名不合法（过长或为空），请重命名后再上传', 'error')
    target.value = ''
    return
  }

  imageUploading.value = true
  imageUploadProgress.value = 0
  imageAbortController = new AbortController()

  try {
    const formData = new FormData()
    formData.append('file', file)
    const { upload } = useApi()

    const uploadRes = await upload<any>('/files/upload/image', formData, (percent) => {
      imageUploadProgress.value = percent
    })

    const imageUrl = uploadRes.data?.data
    if (imageUrl) {
      form.images.push(imageUrl)
      const textarea = getEditorTextarea()
      if (textarea) {
        insertAtCursor(`\n![图片](${imageUrl})\n`)
        showToast('图片上传成功，已自动插入到正文中', 'success')
      } else {
        showToast('图片上传成功，点击下方缩略图可插入正文', 'success')
      }
    } else {
      showToast('图片上传失败，请稍后重试', 'error')
    }
  } catch (err: any) {
    // 用户主动取消，不显示错误
    if (err?.code === 'ERR_CANCELED' || err?.name === 'CanceledError' || err?.message === 'canceled') {
      return
    }
    // 网络错误
    if (!err?.response) {
      showToast('网络好像断开了，请检查网络后重试', 'error')
    } else {
      // 后端 message 已经是用户可见的话术时直接展示；否则走友好兜底
      const msg = err?.response?.data?.message
      showToast(msg && typeof msg === 'string' ? msg : '图片上传失败，请稍后重试', 'error')
    }
  } finally {
    imageUploading.value = false
    imageUploadProgress.value = 0
    imageAbortController = null
    target.value = ''
  }
}

const removeImage = (idx: number) => {
  form.images.splice(idx, 1)
}

const insertImageAtCursor = (imageUrl: string) => {
  insertAtCursor(`\n![图片](${imageUrl})\n`)
}

// AI 写作
const aiLoading = ref(false)

/**
 * 判断审核结果是否属于"不通过 / 存在风险"。
 * 后端审核会返回包含 ⚠️ / ❌ / 风险 / 违规 / 敏感 / 不通过 等关键词的提示。
 */
const isReviewFailed = (text: string): boolean => {
  if (!text) return false
  return /不通过|违规|敏感|风险|警告|⚠️|❌|reject|rejected|denied|敏感内容|请稍后再试|请稍后重试|暂时不可用|请求过于频繁/.test(text)
}

const handleAIWrite = async (mode: string) => {
  const content = form.content.trim()
  if (!content) {
    showToast('请先在正文中输入内容', 'error')
    return
  }
  aiLoading.value = true
  try {
    // 审核模式：读取正文内容，显示通过/不通过 toast
    if (mode === 'review') {
      const { data } = await aiApi.reviewContent(content)
      const result = data?.data?.content || ''
      const usage = data?.data?.usage || ''
      const looksLikeMock =
        usage.includes('mock') ||
        result.startsWith('[AI ') ||
        result.startsWith('[passed]') ||
        result.includes('此内容由AI生成')
      if (looksLikeMock) {
        showToast('AI 写作功能暂未启用，请联系管理员', 'error')
        return
      }
      if (usage.startsWith('error:') || /请稍后再试|请稍后重试|暂时不可用|请求过于频繁/.test(result)) {
        showToast(result || 'AI 服务暂时不可用，请稍后重试', 'error')
        return
      }
      if (isReviewFailed(result)) {
        showToast(`AI 审核未通过：${result}`, 'error', { duration: 5000 })
      } else {
        showToast(`AI 审核通过：${result}`, 'success', { duration: 5000 })
      }
      return
    }

    // 扩写/润色模式：读取正文全部内容，AI 输出直接覆盖正文
    const { data } = await aiApi.generateText(content, form.title.trim() || undefined, mode)
    const result = data?.data?.content || ''
    const usage = data?.data?.usage || ''
    if (result) {
      const isAiError =
        result.startsWith('AI 服务调用失败') ||
        result.includes('请稍后再试') ||
        result.includes('请稍后重试') ||
        result.includes('暂时不可用') ||
        result.includes('请求过于频繁')
      if (isAiError) {
        showToast(result, 'error')
        return
      }
      if (usage.startsWith('error:') || result.includes('AI 服务未配置 API Key')) {
        showToast('AI 写作功能暂未启用，请联系管理员', 'error')
        return
      }
      const isMock =
        usage.includes('mock') ||
        result.startsWith('[AI ') ||
        result.startsWith('[passed]') ||
        result.includes('此内容由AI生成')
      if (isMock) {
        showToast('AI 写作功能暂未启用，请联系管理员', 'error')
        return
      }
      form.content = result
      const label = mode === 'expand' ? '扩写' : '润色'
      showToast(`AI ${label}完成，已更新正文内容`, 'success')
    } else {
      showToast('AI 这次没有返回内容，请稍后再试', 'error')
    }
  } catch (err: any) {
    const raw = err?.response?.data?.message
    const friendly = raw && !/^Request failed|^Error:|^\d{3}$/i.test(String(raw))
      ? String(raw)
      : null
    showToast(friendly || 'AI 处理失败，请稍后重试', 'error')
  } finally { aiLoading.value = false }
}

const regionSelectorRef = ref<InstanceType<typeof import('~/components/RegionSelector.vue')['default']> | null>(null)

const onRegionChange = (location: string) => {
  form.location = location
}

const autoLocate = async () => {
  if (!true) return
  locating.value = true
  locateErrorText.value = ''
  try {
    const result = await locationApi.locate({ useCache: false })
    if (!result) {
      const reason = locationApi.errorMessage.value || '定位失败，请手动选择位置'
      showToast(reason, 'error')
      locateErrorText.value = reason
      return
    }
    const { province, city, district } = result
    if (province && regionSelectorRef.value) {
      regionSelectorRef.value.setRegion({ province, city, district })
      const locationDisplay = district && district !== city
        ? `${province}·${city}·${district}`
        : `${province}${city ? ' · ' + city : ''}`
      showToast(`已定位到：${locationDisplay}`, 'success')
      locateErrorText.value = ''
      // 用户切换过选择后清除缓存，避免下次自动覆盖
      locationApi.clearCache()
    } else {
      const msg = '定位结果无效，请手动选择位置'
      showToast(msg, 'error')
      locateErrorText.value = msg
    }
  } catch (err: any) {
    const msg = err?.message || '定位失败，请手动选择位置'
    showToast(msg, 'error')
    locateErrorText.value = msg
  } finally {
    locating.value = false
  }
}

const openPreview = () => {
  if (!true) return
  sessionStorage.setItem('editor_preview_data', JSON.stringify({
    title: form.title,
    content: form.content,
    summary: form.summary,
    images: form.images,
    tags: form.tags,
    location: form.location,
  }))
  window.open('/editor/preview', '_blank')
}

const saveDraft = async () => {
  if (!form.title.trim()) {
    showToast('请先填写作品标题再保存草稿', 'error')
    return
  }
  saving.value = true
  try {
    const requestData = buildArticleRequest(0)
    await apiPost<any>('/articles', requestData)
    showToast('草稿已保存，仅自己可见', 'success')
  } catch (error: any) {
    const raw = error.response?.data?.message
    const msg = typeof raw === 'string' && raw ? raw : ''
    if (msg.includes('敏感词') || msg.includes('禁止')) {
      showToast(`草稿保存失败：${msg}`, 'error')
    } else if (msg) {
      showToast(`草稿保存失败：${msg}`, 'error')
    } else {
      showToast('草稿保存失败，请稍后重试', 'error')
    }
  } finally {
    saving.value = false
  }
}

const getDeviceInfo = (): string | undefined => {
  if (!true) return undefined
  const ua = navigator.userAgent
  const mobileMatch = ua.match(/\b(Android|iPhone|iPad|iPod)\b[^;]*/i)
  if (mobileMatch) {
    const androidBuild = ua.match(/;\s*([^;)]+)\s+Build\//)
    if (androidBuild) return androidBuild[1].trim()
    const iosMatch = ua.match(/(iPhone|iPad|iPod)(?:\s*OS\s*[\d_]+)?/i)
    if (iosMatch) return iosMatch[0].replace(/_/g, '.')
    return mobileMatch[0]
  }
  if (ua.includes('Windows')) return 'Windows'
  if (ua.includes('Mac OS')) return 'macOS'
  if (ua.includes('Linux')) return 'Linux'
  return undefined
}

function buildArticleRequest(status: number): Record<string, any> {
  const data: Record<string, any> = {
    title: form.title.trim(),
    content: form.content,
    summary: form.summary.trim() || undefined,
    categoryName: form.categoryName.trim() || '默认分类',
    tagNames: form.tags.length > 0 ? form.tags : undefined,
    images: form.images.length > 0 ? form.images : undefined,
    location: form.location || undefined,
    status,
  }
  Object.keys(data).forEach((key) => {
    if (data[key] === undefined) delete data[key]
  })
  return data
}

const publishArticle = async () => {
  if (!canPublish.value) return
  publishing.value = true
  try {
    const data = buildArticleRequest(1)
    data.deviceInfo = getDeviceInfo()
    if (scheduledPublish.value && publishAt.value) {
      data.publishAt = new Date(publishAt.value).toISOString()
    }
    const response = await apiPost<any>('/articles', data)
    const articleId = response.data?.data?.id || response.data?.data
    if (scheduledPublish.value && publishAt.value) {
      showToast('已设置定时发布，到点将自动发布', 'success')
      router.push('/user')
    } else {
      if (articleId) {
        navigateTo(`/articles/${articleId}`)
      } else {
        showToast('发布成功，正在跳转...', 'success')
        router.push('/user')
      }
    }
  } catch (error: any) {
    const raw = error.response?.data?.message
    const msg = typeof raw === 'string' && raw ? raw : ''
    if (msg.includes('敏感词') || msg.includes('禁止')) {
      showToast(`发布未通过：${msg}`, 'error')
    } else if (msg) {
      showToast(`发布失败：${msg}`, 'error')
    } else {
      showToast('发布失败，请稍后重试', 'error')
    }
  } finally {
    publishing.value = false
  }
}

useHead({
  title: () => '创作' + ' - 知讯',
})
</script>

<style scoped>
/* ========== 页面整体进入动画 ========== */
@keyframes editor-page-enter {
  0% {
    opacity: 0;
    transform: translateY(12px) scale(0.98);
  }
  100% {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}
.animate-fade-in-up {
  animation: editor-page-enter 0.5s cubic-bezier(0.22, 0.61, 0.36, 1) both;
}

/* ========== 头部渐变背景 ========== */
.editor-header {
  background: linear-gradient(135deg, var(--zh-bg-elevated) 0%, var(--zh-bg) 100%);
  border: 1px solid var(--zh-border);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04), 0 1px 2px rgba(0, 0, 0, 0.02);
}

/* ========== 编辑器容器 ========== */
.editor-container {
  transition: box-shadow 0.3s ease, border-color 0.3s ease;
}

/* 编辑器容器聚焦发光效果 */
.editor-container-glow:focus-within {
  border-color: var(--zh-primary);
  box-shadow: 0 0 0 3px rgba(var(--zh-primary-rgb, 59 130 246), 0.12), var(--zh-shadow-md);
}

/* 标题输入框聚焦发光 */
.title-input:focus {
  border-bottom-color: var(--zh-primary);
  box-shadow: inset 0 -1px 0 0 var(--zh-primary);
}

/* ========== 工具栏 - 容器溢出处理 ========== */
.editor-toolbar {
  /* 允许横向滚动，避免按钮被压缩 */
  -webkit-overflow-scrolling: touch;
  scrollbar-width: thin;
  position: relative;
  z-index: 30;
}
.editor-toolbar::-webkit-scrollbar {
  height: 4px;
}
.editor-toolbar::-webkit-scrollbar-track {
  background: transparent;
}
.editor-toolbar::-webkit-scrollbar-thumb {
  background: var(--zh-border);
  border-radius: 9999px;
}
.editor-toolbar:hover {
  /* 提升层级避免 tooltip 被裁切 */
  z-index: 60;
}
.scrollbar-thin {
  scrollbar-width: thin;
}

/* ========== AI 工具栏 - 独立行 ========== */
.ai-toolbar {
  position: relative;
  z-index: 1;
}
.ai-toolbar:hover {
  z-index: 60;
}

/* ========== 工具栏分组 ========== */
.toolbar-group {
  @apply flex items-center gap-0 px-0;
  position: relative;
}
.toolbar-divider {
  @apply w-px h-3 bg-[var(--zh-border)] mx-0.5;
}
.toolbar-ai-label {
  @apply text-[9px] font-semibold px-0.5 uppercase tracking-wider;
  background: linear-gradient(135deg, #8b5cf6, #6366f1);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

/* 快捷键提示 */
.toolbar-kbd {
  display: none;
  font-size: 8px;
  font-weight: 600;
  color: var(--zh-text-tertiary);
  background: var(--zh-bg-hover);
  border: 1px solid var(--zh-border);
  border-radius: 2px;
  padding: 0 2px;
  line-height: 1.3;
  margin-left: 1px;
}
@media (min-width: 768px) {
  .toolbar-kbd { display: inline; }
}

/* ========== 工具栏按钮 ========== */
.toolbar-btn {
  @apply p-0.5 text-[var(--zh-text-secondary)] hover:bg-[var(--zh-bg-hover)] hover:text-[var(--zh-text)] rounded transition-all duration-150;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  position: relative;
  font-size: 11px;
}
.toolbar-btn:active {
  transform: scale(0.95);
}
.toolbar-btn:hover .toolbar-kbd {
  color: var(--zh-primary);
  border-color: var(--zh-primary);
}

/* 工具栏按钮 hover tooltip - 优化溢出处理 */
.toolbar-btn[data-tooltip]::after {
  content: attr(data-tooltip);
  position: absolute;
  bottom: calc(100% + 6px);
  left: 50%;
  transform: translateX(-50%);
  background: #1e293b;
  color: #f1f5f9;
  font-size: 11px;
  font-weight: 500;
  padding: 4px 10px;
  border-radius: 6px;
  white-space: nowrap;
  pointer-events: none;
  opacity: 0;
  transition: opacity 0.2s ease, transform 0.2s ease;
  z-index: 9999;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  max-width: 200px;
  overflow: visible;
}
.toolbar-btn[data-tooltip]::before {
  content: '';
  position: absolute;
  bottom: calc(100% + 1px);
  left: 50%;
  transform: translateX(-50%);
  border: 5px solid transparent;
  border-top-color: #1e293b;
  pointer-events: none;
  opacity: 0;
  transition: opacity 0.2s ease;
  z-index: 9999;
}
.toolbar-btn:hover[data-tooltip]::after {
  opacity: 1;
  transform: translateX(-50%) translateY(-2px);
}
.toolbar-btn:hover[data-tooltip]::before {
  opacity: 1;
}
/* 当按钮靠近左/右边缘时，调整 tooltip 位置避免溢出 */
.toolbar-group:first-child .toolbar-btn:first-child[data-tooltip]::after {
  left: 0;
  transform: translateX(0);
}
.toolbar-group:first-child .toolbar-btn:first-child[data-tooltip]::before {
  left: 16px;
  transform: translateX(0);
}
.toolbar-group:first-child .toolbar-btn:first-child:hover[data-tooltip]::after {
  transform: translateX(0) translateY(-2px);
}
.toolbar-group:last-child .toolbar-btn:last-child[data-tooltip]::after {
  left: auto;
  right: 0;
  transform: translateX(0);
}
.toolbar-group:last-child .toolbar-btn:last-child[data-tooltip]::before {
  left: auto;
  right: 8px;
  transform: translateX(0);
}
.toolbar-group:last-child .toolbar-btn:last-child:hover[data-tooltip]::after {
  transform: translateX(0) translateY(-2px);
}
/* AI工具栏内审核按钮 tooltip 右边缘校正 */
.ai-toolbar .ai-tool-review[data-tooltip]::after {
  left: auto;
  right: 0;
  transform: translateX(0);
}
.ai-toolbar .ai-tool-review[data-tooltip]::before {
  left: auto;
  right: 8px;
  transform: translateX(0);
}
.ai-toolbar .ai-tool-review:hover[data-tooltip]::after {
  transform: translateX(0) translateY(-2px);
}

/* ========== AI 工具按钮 - 渐变紫色 ========== */
.ai-tool-btn {
  @apply px-1.5 py-0.5 text-[10px] font-medium rounded transition-all duration-200;
  display: inline-flex;
  align-items: center;
  gap: 2px;
  color: #fff;
  border: 1px solid transparent;
  position: relative;
  line-height: 1.3;
  min-height: 0;
  min-width: 0;
}

/* 扩写 - 紫色渐变 */
.ai-tool-expand {
  background: linear-gradient(135deg, #8b5cf6 0%, #7c3aed 100%);
  box-shadow: 0 1px 4px rgba(139, 92, 246, 0.3);
}
.ai-tool-expand:hover:not(:disabled) {
  background: linear-gradient(135deg, #9b6cf7 0%, #8b4aed 100%);
  box-shadow: 0 3px 10px rgba(139, 92, 246, 0.45);
  transform: translateY(-1px);
}

/* 润色 - 靛蓝渐变 */
.ai-tool-polish {
  background: linear-gradient(135deg, #6366f1 0%, #4f46e5 100%);
  box-shadow: 0 1px 4px rgba(99, 102, 241, 0.3);
}
.ai-tool-polish:hover:not(:disabled) {
  background: linear-gradient(135deg, #7375f5 0%, #5b55e5 100%);
  box-shadow: 0 3px 10px rgba(99, 102, 241, 0.45);
  transform: translateY(-1px);
}

/* 审核 - 橙色渐变 */
.ai-tool-review {
  background: linear-gradient(135deg, #f97316 0%, #ea580c 100%);
  box-shadow: 0 1px 4px rgba(249, 115, 22, 0.3);
}
.ai-tool-review:hover:not(:disabled) {
  background: linear-gradient(135deg, #fb923c 0%, #f07016 100%);
  box-shadow: 0 3px 10px rgba(249, 115, 22, 0.45);
  transform: translateY(-1px);
}

.ai-tool-btn:disabled {
  @apply opacity-50 cursor-not-allowed;
  transform: none !important;
  box-shadow: none !important;
}
.ai-tool-btn:active:not(:disabled) {
  transform: scale(0.96) !important;
}

/* AI 按钮 tooltip */
.ai-tool-btn[data-tooltip]::after {
  content: attr(data-tooltip);
  position: absolute;
  bottom: calc(100% + 6px);
  left: 50%;
  transform: translateX(-50%);
  background: #1e293b;
  color: #f1f5f9;
  font-size: 11px;
  font-weight: 500;
  padding: 4px 10px;
  border-radius: 6px;
  white-space: nowrap;
  pointer-events: none;
  opacity: 0;
  transition: opacity 0.2s ease, transform 0.2s ease;
  z-index: 9999;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}
.ai-tool-btn[data-tooltip]::before {
  content: '';
  position: absolute;
  bottom: calc(100% + 1px);
  left: 50%;
  transform: translateX(-50%);
  border: 5px solid transparent;
  border-top-color: #1e293b;
  pointer-events: none;
  opacity: 0;
  transition: opacity 0.2s ease;
  z-index: 9999;
}
.ai-tool-btn:hover[data-tooltip]::after {
  opacity: 1;
  transform: translateX(-50%) translateY(-2px);
}
.ai-tool-btn:hover[data-tooltip]::before {
  opacity: 1;
}

/* ========== 图片缩略图 ========== */
.thumb-card {
  border-radius: 12px;
  overflow: hidden;
  border: 2px solid var(--zh-border);
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.06);
  transition: all 0.25s cubic-bezier(0.22, 0.61, 0.36, 1);
}
.thumb-card:hover {
  border-color: var(--zh-primary);
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.1), 0 0 0 2px rgba(var(--zh-primary-rgb, 59 130 246), 0.15);
  transform: translateY(-2px);
}

.thumb-add {
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  transition: all 0.25s cubic-bezier(0.22, 0.61, 0.36, 1);
}
.thumb-add:hover:not(.opacity-50) {
  border-color: var(--zh-primary);
  box-shadow: 0 3px 10px rgba(0, 0, 0, 0.08);
  transform: translateY(-1px);
}

/* ========== 移动端 Tab 切换 ========== */
.mobile-tab-switch {
  position: relative;
}
.mobile-tab-btn {
  position: relative;
  z-index: 1;
}
.mobile-tab-active {
  position: relative;
  z-index: 2;
}

/* ========== 按钮样式 ========== */
.btn-primary {
  @apply px-5 py-2.5 text-sm font-semibold text-white rounded-xl transition-all duration-200;
  background: linear-gradient(135deg, var(--zh-primary) 0%, var(--zh-primary-dark) 100%);
  box-shadow: 0 2px 8px rgba(var(--zh-primary-rgb), 0.25);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}
.btn-primary:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 16px rgba(var(--zh-primary-rgb), 0.35);
}
.btn-primary:active:not(:disabled) {
  transform: translateY(0);
}
.btn-primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-secondary {
  @apply px-4 py-2.5 text-sm font-medium text-[var(--zh-text-secondary)] rounded-xl transition-all duration-200;
  background: var(--zh-bg-elevated);
  border: 1.5px solid var(--zh-border);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}
.btn-secondary:hover:not(:disabled) {
  border-color: var(--zh-border-focus);
  background: var(--zh-bg-hover);
  transform: translateY(-1px);
  box-shadow: var(--zh-shadow-sm);
}
.btn-secondary:active:not(:disabled) {
  transform: scale(0.98);
}
.btn-secondary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* ========== 编辑器 textarea ========== */
.editor-textarea::placeholder {
  color: var(--zh-text-placeholder);
  font-style: italic;
}

/* ========== 预览区 prose 增强 ========== */
:deep(.prose) {
  max-width: none;
  line-height: 1.8;
}
:deep(.prose h1) { font-size: 1.5rem; margin-top: 1.5rem; margin-bottom: 0.75rem; }
:deep(.prose h2) { font-size: 1.25rem; margin-top: 1.25rem; margin-bottom: 0.5rem; }
:deep(.prose h3) { font-size: 1.1rem; margin-top: 1rem; margin-bottom: 0.5rem; }
:deep(.prose p) { margin-bottom: 0.75rem; }
:deep(.prose img) { border-radius: 8px; max-width: 100%; }
:deep(.prose code) {
  background: var(--zh-bg-hover);
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 0.875em;
}
:deep(.prose pre) {
  background: #1e293b;
  color: #e2e8f0;
  padding: 1rem;
  border-radius: 8px;
  overflow-x: auto;
}
:deep(.prose blockquote) {
  border-left: 3px solid var(--zh-primary);
  padding-left: 1rem;
  color: var(--zh-text-secondary);
  font-style: italic;
}
</style>