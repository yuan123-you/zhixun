<template>
  <!-- 全屏作品预览页 - 抖音风格 -->
  <div class="fixed inset-0 z-[200] bg-black flex flex-col">
    <!-- 顶部返回按钮 -->
    <div class="absolute top-0 left-0 right-0 z-30 h-12 flex items-center px-2 bg-gradient-to-b from-black/60 to-transparent pointer-events-none">
      <button
        class="pointer-events-auto w-9 h-9 flex items-center justify-center rounded-full bg-white/10 backdrop-blur-sm text-white active:bg-white/20 transition-colors"
        @click="goBack"
      >
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M15 19l-7-7 7-7" />
        </svg>
      </button>
      <span class="ml-2 text-sm text-white/80 pointer-events-none">{{ article?.title || '作品预览' }}</span>
    </div>

    <!-- 主内容区 - 可滚动 -->
    <div
      ref="contentScrollRef"
      class="flex-1 overflow-y-auto overflow-x-hidden overscroll-contain"
      :class="{ 'pt-14 pb-20': true }"
    >
      <!-- 加载中 -->
      <div v-if="loading" class="flex items-center justify-center h-full">
        <div class="w-8 h-8 border-2 border-white border-t-transparent rounded-full animate-spin" />
      </div>

      <!-- 错误 -->
      <div v-else-if="error" class="flex flex-col items-center justify-center h-full text-white/70 px-6">
        <svg class="w-12 h-12 mb-3 opacity-50" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M12 9v2m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
        <p class="text-sm mb-3">{{ error }}</p>
        <button class="px-4 py-2 bg-white/10 rounded-lg text-sm active:bg-white/20 transition-colors" @click="fetchArticle">
          重新加载
        </button>
      </div>

      <!-- 内容 -->
      <div v-else-if="article" class="min-h-full">
        <!-- 作品封面图（如有） -->
        <div v-if="article.coverImage" class="w-full">
          <img
            :src="resolveUrl(article.coverImage) || ''"
            :alt="article.title"
            class="w-full object-contain max-h-[60vh]"
            loading="eager"
          />
        </div>

        <!-- 图片网格 -->
        <div v-if="article.images?.length" class="w-full">
          <div class="grid grid-cols-3 gap-0 w-full">
            <div
              v-for="(img, idx) in article.images.slice(0, 9)"
              :key="idx"
              class="aspect-square overflow-hidden"
              @click="openImageZoom(resolveUrl(img) || img)"
            >
              <img :src="resolveUrl(img) || img" alt="" class="w-full h-full object-cover" loading="lazy" />
            </div>
          </div>
        </div>

        <!-- 标题和作者信息 -->
        <div class="px-4 pt-4 pb-2">
          <h1 class="text-lg font-bold text-white mb-2">{{ article.title }}</h1>
          <div class="flex items-center gap-2.5">
            <UserAvatar :src="article.author?.avatar" :alt="article.author?.nickname || '用户'" size="sm" />
            <div class="flex-1 min-w-0">
              <p class="text-sm text-white/90 font-medium truncate">
                {{ article.author?.nickname || article.authorName || '用户' }}
              </p>
              <p class="text-xs text-white/50">
                {{ formatTimestamp(article.createdAt) }}
                <template v-if="article.location">
                  · {{ article.location }}
                </template>
              </p>
            </div>
          </div>
        </div>

        <!-- 富文本正文 -->
        <div
          v-if="article.content"
          class="px-4 pb-6 prose prose-invert prose-sm max-w-none text-white/90"
          v-html="article.content"
          @click="handleContentClick"
        />

        <!-- 标签 -->
        <div v-if="article.tags?.length" class="px-4 pb-4 flex flex-wrap gap-1.5">
          <span
            v-for="tag in article.tags"
            :key="tag.id"
            class="px-2.5 py-1 text-xs rounded-full bg-white/10 text-white/70"
          >
            #{{ tag.name }}
          </span>
        </div>

        <!-- 底部留白 -->
        <div class="h-16" />
      </div>
    </div>

    <!-- 底部固定栏 -->
    <div class="absolute bottom-0 left-0 right-0 z-30 bg-black/90 backdrop-blur-lg border-t border-white/10">
      <div class="flex items-center justify-between px-4 py-2.5">
        <!-- 浏览量按钮 -->
        <button
          class="flex items-center gap-1 text-xs text-white/60 active:text-white/90 transition-colors"
          @click="openViewsSheet"
        >
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
          </svg>
          <span>{{ formatCount(article?.viewCount) }}</span>
        </button>

        <!-- 修改权限按钮 -->
        <button
          class="flex items-center gap-1 px-3 py-1.5 rounded-full bg-white/10 text-xs text-white/70 active:bg-white/20 transition-colors"
          @click="openPermissionSheet"
        >
          <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
          </svg>
          <span>{{ visibilityLabel }}</span>
        </button>

        <!-- 菜单按钮 -->
        <button
          class="flex items-center gap-1 px-3 py-1.5 rounded-full bg-white/10 text-xs text-white/70 active:bg-white/20 transition-colors"
          @click="openMenuSheet"
        >
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 5v.01M12 12v.01M12 19v.01M12 6a1 1 0 110-2 1 1 0 010 2zm0 7a1 1 0 110-2 1 1 0 010 2zm0 7a1 1 0 110-2 1 1 0 010 2z" />
          </svg>
        </button>
      </div>
    </div>

    <!-- ==================== 底部弹出菜单 ==================== -->
    <Teleport to="body">
      <Transition name="sheet-slide">
        <div v-if="activeSheet" class="fixed inset-0 z-[250] flex flex-col justify-end">
          <!-- 遮罩 -->
          <div class="absolute inset-0 bg-black/50" @click="closeAllSheets" />
          <!-- 菜单内容 -->
          <div class="relative bg-gray-900 rounded-t-2xl w-full max-h-[75vh] overflow-hidden flex flex-col">
            <!-- 拖拽指示条 -->
            <div class="flex justify-center pt-3 pb-1">
              <div class="w-8 h-1 rounded-full bg-white/20" />
            </div>

            <!-- 主菜单 -->
            <template v-if="activeSheet === 'menu'">
              <div class="px-4 py-2">
                <h3 class="text-base font-semibold text-white text-center mb-4">更多操作</h3>
                <div class="grid grid-cols-3 gap-3">
                  <!-- 分享 -->
                  <button class="flex flex-col items-center gap-2 p-3 rounded-xl active:bg-white/5 transition-colors" @click="openShareSheet">
                    <div class="w-12 h-12 rounded-full bg-blue-500/20 flex items-center justify-center">
                      <svg class="w-6 h-6 text-blue-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M8.684 13.342C8.886 12.938 9 12.482 9 12c0-.482-.114-.938-.316-1.342m0 2.684a3 3 0 110-2.684m0 2.684l6.632 3.316m-6.632-6l6.632-3.316m0 0a3 3 0 105.367-2.684 3 3 0 00-5.367 2.684zm0 9.316a3 3 0 105.368 2.684 3 3 0 00-5.368-2.684z" />
                      </svg>
                    </div>
                    <span class="text-xs text-white/70">分享</span>
                  </button>
                  <!-- 转发 -->
                  <button class="flex flex-col items-center gap-2 p-3 rounded-xl active:bg-white/5 transition-colors" @click="openForwardSheet">
                    <div class="w-12 h-12 rounded-full bg-green-500/20 flex items-center justify-center">
                      <svg class="w-6 h-6 text-green-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8" />
                      </svg>
                    </div>
                    <span class="text-xs text-white/70">转发</span>
                  </button>
                  <!-- 删除 -->
                  <button class="flex flex-col items-center gap-2 p-3 rounded-xl active:bg-white/5 transition-colors" @click="confirmDeleteArticle">
                    <div class="w-12 h-12 rounded-full bg-red-500/20 flex items-center justify-center">
                      <svg class="w-6 h-6 text-red-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                      </svg>
                    </div>
                    <span class="text-xs text-white/70">删除</span>
                  </button>
                </div>
              </div>
              <!-- 取消按钮 -->
              <div class="px-4 pb-6 pt-2">
                <button class="w-full py-3 rounded-xl bg-white/5 text-sm text-white/70 active:bg-white/10 transition-colors" @click="closeAllSheets">
                  取消
                </button>
              </div>
            </template>

            <!-- 分享菜单 - 分享给平台好友 -->
            <template v-if="activeSheet === 'share'">
              <div class="px-4 py-2 flex-shrink-0">
                <div class="flex items-center justify-between mb-3">
                  <h3 class="text-base font-semibold text-white">分享给好友</h3>
                  <button class="text-xs text-white/40 active:text-white/70" @click="openMenuSheet">返回</button>
                </div>
                <!-- 搜索框 -->
                <div class="relative mb-3">
                  <svg class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-white/30" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                  </svg>
                  <input
                    v-model="shareSearchQuery"
                    type="text"
                    placeholder="搜索好友..."
                    class="w-full h-10 pl-9 pr-3 rounded-lg bg-white/5 text-sm text-white placeholder:text-white/30 border border-white/10 focus:border-white/30 focus:outline-none"
                  />
                </div>
              </div>
              <!-- 好友列表 -->
              <div class="flex-1 overflow-y-auto overscroll-contain px-4 pb-4">
                <div v-if="shareUsersLoading" class="flex justify-center py-8">
                  <div class="w-6 h-6 border-2 border-white border-t-transparent rounded-full animate-spin" />
                </div>
                <div v-else-if="filteredShareUsers.length === 0" class="text-center py-8 text-white/40 text-sm">
                  {{ loadShareUsersError ? '加载失败，请重试' : '暂无好友' }}
                </div>
                <div v-else class="space-y-1">
                  <button
                    v-for="user in filteredShareUsers"
                    :key="user.id"
                    class="flex items-center gap-3 w-full p-2.5 rounded-lg active:bg-white/5 transition-colors"
                    :disabled="shareSendingId === user.id"
                    @click="shareToUser(user)"
                  >
                    <UserAvatar :src="user.avatar" :alt="user.nickname" size="md" />
                    <div class="flex-1 min-w-0 text-left">
                      <p class="text-sm text-white font-medium truncate">{{ user.nickname }}</p>
                      <p class="text-xs text-white/40">
                        {{ user.isMutualFollow ? '互相关注' : user.isFollowing ? '我的粉丝' : '我关注的' }}
                      </p>
                    </div>
                    <div v-if="shareSendingId === user.id" class="w-5 h-5 border-2 border-blue-400 border-t-transparent rounded-full animate-spin" />
                    <svg v-else class="w-5 h-5 text-white/30 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8" />
                    </svg>
                  </button>
                </div>
              </div>
            </template>

            <!-- 转发菜单 -->
            <template v-if="activeSheet === 'forward'">
              <div class="px-4 py-2">
                <div class="flex items-center justify-between mb-3">
                  <h3 class="text-base font-semibold text-white">转发到</h3>
                  <button class="text-xs text-white/40 active:text-white/70" @click="openMenuSheet">返回</button>
                </div>
              </div>
              <div class="px-4 pb-6 space-y-1">
                <!-- QQ -->
                <button class="flex items-center gap-4 w-full p-3 rounded-xl active:bg-white/5 transition-colors" @click="forwardTo('qq')">
                  <div class="w-10 h-10 rounded-lg bg-[#12B7F5]/20 flex items-center justify-center shrink-0">
                    <svg class="w-5 h-5 text-[#12B7F5]" viewBox="0 0 24 24" fill="currentColor">
                      <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm4.64 13.2c-.18.53-.5.98-.93 1.33.27.12.58.2.9.2.16 0 .28-.01.39-.04-.32.3-.74.5-1.2.56-.16.02-.3.03-.45.03-.36 0-.7-.08-1.01-.22-.32.08-.65.13-1 .13s-.68-.05-1-.13c-.31.14-.65.22-1.01.22-.15 0-.3-.01-.45-.03-.46-.06-.88-.26-1.2-.56.11.03.23.04.39.04.32 0 .63-.08.9-.2-.43-.35-.75-.8-.93-1.33.38.32.86.52 1.39.52.24 0 .47-.04.68-.12-.3-.28-.5-.66-.5-1.09 0-.36.13-.69.35-.95-.53-.19-.91-.69-.91-1.28 0-.3.1-.58.28-.81-.1-.33-.16-.69-.16-1.07 0-1.82 1.17-3.29 2.61-3.29s2.61 1.47 2.61 3.29c0 .38-.06.74-.16 1.07.18.23.28.51.28.81 0 .59-.38 1.09-.91 1.28.22.26.35.59.35.95 0 .43-.2.81-.5 1.09.21.08.44.12.68.12.53 0 1.01-.2 1.39-.52z"/>
                    </svg>
                  </div>
                  <span class="text-sm text-white/80">QQ</span>
                </button>
                <!-- 微信 -->
                <button class="flex items-center gap-4 w-full p-3 rounded-xl active:bg-white/5 transition-colors" @click="forwardTo('wechat')">
                  <div class="w-10 h-10 rounded-lg bg-[#07C160]/20 flex items-center justify-center shrink-0">
                    <svg class="w-5 h-5 text-[#07C160]" viewBox="0 0 24 24" fill="currentColor">
                      <path d="M8.691 2.188C3.891 2.188 0 5.476 0 9.53c0 2.212 1.17 4.203 3.002 5.55a.59.59 0 01.213.665l-.39 1.48c-.019.07-.048.141-.048.213 0 .163.13.295.29.295a.326.326 0 00.167-.054l1.903-1.114a.864.864 0 01.717-.098 10.16 10.16 0 002.837.403c.276 0 .543-.027.811-.05a6.42 6.42 0 01-.246-1.79c0-3.558 3.39-6.441 7.573-6.441.258 0 .509.025.764.042C16.626 4.834 13.004 2.188 8.691 2.188zm-2.6 4.408c.56 0 1.015.46 1.015 1.025 0 .566-.455 1.025-1.014 1.025-.56 0-1.015-.46-1.015-1.025 0-.566.456-1.025 1.015-1.025zm5.144 0c.56 0 1.015.46 1.015 1.025 0 .566-.456 1.025-1.015 1.025-.56 0-1.014-.46-1.014-1.025 0-.566.455-1.025 1.014-1.025z"/>
                    </svg>
                  </div>
                  <span class="text-sm text-white/80">微信</span>
                </button>
                <!-- 微博 -->
                <button class="flex items-center gap-4 w-full p-3 rounded-xl active:bg-white/5 transition-colors" @click="forwardTo('weibo')">
                  <div class="w-10 h-10 rounded-lg bg-[#E6162D]/20 flex items-center justify-center shrink-0">
                    <svg class="w-5 h-5 text-[#E6162D]" viewBox="0 0 24 24" fill="currentColor">
                      <path d="M10.09 16.82c-2.83.3-5.27-1-5.46-2.9-.19-1.9 1.95-3.69 4.78-3.99 2.83-.3 5.27 1 5.46 2.9.19 1.9-1.95 3.69-4.78 3.99zm7.71-5.1c-.23-.7-.92-1.08-1.54-.85-.62.23-.94.94-.71 1.59.23.65.92 1.03 1.54.8.62-.22.94-.89.71-1.54z"/>
                    </svg>
                  </div>
                  <span class="text-sm text-white/80">微博</span>
                </button>
                <!-- 复制链接 -->
                <button class="flex items-center gap-4 w-full p-3 rounded-xl active:bg-white/5 transition-colors" @click="forwardTo('link')">
                  <div class="w-10 h-10 rounded-lg bg-white/10 flex items-center justify-center shrink-0">
                    <svg class="w-5 h-5 text-white/70" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13.828 10.172a4 4 0 00-5.656 0l-4 4a4 4 0 105.656 5.656l1.102-1.101m-.758-4.899a4 4 0 005.656 0l4-4a4 4 0 00-5.656-5.656l-1.1 1.1" />
                    </svg>
                  </div>
                  <span class="text-sm text-white/80">{{ linkCopied ? '已复制' : '复制链接' }}</span>
                </button>
              </div>
            </template>

            <!-- 修改权限菜单 -->
            <template v-if="activeSheet === 'permission'">
              <div class="px-4 py-2">
                <h3 class="text-base font-semibold text-white text-center mb-3">修改权限</h3>
                <div class="space-y-1">
                  <button
                    v-for="opt in visibilityOptions"
                    :key="opt.value"
                    class="flex items-center gap-3 w-full p-3 rounded-xl active:bg-white/5 transition-colors"
                    @click="updateVisibility(opt.value)"
                  >
                    <div class="w-10 h-10 rounded-lg flex items-center justify-center shrink-0"
                      :class="article?.visibility === opt.value ? 'bg-blue-500/20' : 'bg-white/5'"
                    >
                      <div v-html="opt.icon" class="w-5 h-5" :class="article?.visibility === opt.value ? 'text-blue-400' : 'text-white/50'" />
                    </div>
                    <div class="flex-1 text-left min-w-0">
                      <p class="text-sm text-white font-medium">{{ opt.label }}</p>
                      <p class="text-xs text-white/40 mt-0.5">{{ opt.desc }}</p>
                    </div>
                    <div
                      v-if="article?.visibility === opt.value"
                      class="w-5 h-5 rounded-full bg-blue-500 flex items-center justify-center shrink-0"
                    >
                      <svg class="w-3 h-3 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="3" d="M5 13l4 4L19 7" />
                      </svg>
                    </div>
                    <div v-if="savingVisibility === opt.value" class="w-5 h-5 border-2 border-blue-400 border-t-transparent rounded-full animate-spin shrink-0" />
                  </button>
                </div>
              </div>
              <div class="px-4 pb-6 pt-2">
                <button class="w-full py-3 rounded-xl bg-white/5 text-sm text-white/70 active:bg-white/10 transition-colors" @click="closeAllSheets">
                  完成
                </button>
              </div>
            </template>

            <!-- 浏览量菜单 -->
            <template v-if="activeSheet === 'views'">
              <div class="px-4 py-2 flex-shrink-0">
                <h3 class="text-base font-semibold text-white text-center mb-3">浏览与互动</h3>
                <!-- Tab 切换 -->
                <div class="flex rounded-lg bg-white/5 p-0.5 mb-3">
                  <button
                    class="flex-1 py-2 text-xs font-medium rounded-md transition-colors"
                    :class="viewsTab === 'viewers' ? 'bg-white/10 text-white' : 'text-white/50'"
                    @click="viewsTab = 'viewers'"
                  >
                    浏览 ({{ article?.viewCount || 0 }})
                  </button>
                  <button
                    class="flex-1 py-2 text-xs font-medium rounded-md transition-colors"
                    :class="viewsTab === 'likers' ? 'bg-white/10 text-white' : 'text-white/50'"
                    @click="viewsTab = 'likers'"
                  >
                    点赞 ({{ article?.likeCount || 0 }})
                  </button>
                </div>
              </div>
              <div class="flex-1 overflow-y-auto overscroll-contain px-4 pb-6">
                <!-- 浏览用户 -->
                <div v-if="viewsTab === 'viewers'">
                  <div v-if="viewsLoading" class="flex justify-center py-8">
                    <div class="w-6 h-6 border-2 border-white border-t-transparent rounded-full animate-spin" />
                  </div>
                  <div v-else-if="viewerUsers.length === 0" class="text-center py-8 text-white/40 text-sm">
                    暂无浏览记录
                  </div>
                  <div v-else class="space-y-1">
                    <div
                      v-for="user in viewerUsers"
                      :key="user.id"
                      class="flex items-center gap-3 p-2.5 rounded-lg"
                    >
                      <UserAvatar :src="user.avatar" :alt="user.nickname" size="sm" />
                      <div class="flex-1 min-w-0">
                        <p class="text-sm text-white font-medium truncate">{{ user.nickname }}</p>
                        <p class="text-xs text-white/40">
                          {{ user.isMutualFollow ? '互相关注' : '粉丝' }}
                        </p>
                      </div>
                    </div>
                  </div>
                </div>
                <!-- 点赞用户 -->
                <div v-if="viewsTab === 'likers'">
                  <div v-if="likersLoading" class="flex justify-center py-8">
                    <div class="w-6 h-6 border-2 border-white border-t-transparent rounded-full animate-spin" />
                  </div>
                  <div v-else-if="likerUsers.length === 0" class="text-center py-8 text-white/40 text-sm">
                    暂无点赞
                  </div>
                  <div v-else class="space-y-1">
                    <div
                      v-for="user in likerUsers"
                      :key="user.id"
                      class="flex items-center gap-3 p-2.5 rounded-lg"
                    >
                      <UserAvatar :src="user.avatar" :alt="user.nickname" size="sm" />
                      <div class="flex-1 min-w-0">
                        <p class="text-sm text-white font-medium truncate">{{ user.nickname }}</p>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </template>
          </div>
        </div>
      </Transition>

      <!-- 删除确认弹窗 -->
      <Transition name="sheet-slide">
        <div v-if="showDeleteConfirm" class="fixed inset-0 z-[260] flex items-center justify-center">
          <div class="absolute inset-0 bg-black/60" @click="showDeleteConfirm = false" />
          <div class="relative bg-gray-900 rounded-2xl w-[320px] max-w-[88vw] p-6">
            <h3 class="text-lg font-bold text-white mb-2">确认删除</h3>
            <p class="text-sm text-white/60 mb-5">
              确定要删除作品「{{ article?.title || '无标题' }}」吗？此操作不可撤销。
            </p>
            <div class="flex gap-3">
              <button
                class="flex-1 py-2.5 rounded-xl text-sm font-medium text-white/70 bg-white/5 active:bg-white/10 transition-colors"
                @click="showDeleteConfirm = false"
              >
                取消
              </button>
              <button
                class="flex-1 py-2.5 rounded-xl text-sm font-medium text-white bg-red-500 active:bg-red-600 transition-colors flex items-center justify-center gap-1.5"
                :disabled="deleting"
                @click="executeDelete"
              >
                <div v-if="deleting" class="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
                <span>{{ deleting ? '删除中...' : '确认删除' }}</span>
              </button>
            </div>
          </div>
        </div>
      </Transition>

      <!-- 图片全屏缩放 -->
      <Transition name="sheet-slide">
        <div
          v-if="imageZoomVisible"
          class="fixed inset-0 z-[300] bg-black flex items-center justify-center"
          @click="imageZoomVisible = false"
        >
          <button class="absolute top-4 right-4 w-10 h-10 flex items-center justify-center rounded-full bg-white/10 text-white/70 active:bg-white/20 z-10" @click="imageZoomVisible = false">
            <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
          <img :src="imageZoomSrc" class="max-w-full max-h-full object-contain" @click.stop />
        </div>
      </Transition>
    </Teleport>

    <!-- Toast -->
    <Transition name="sheet-slide">
      <div
        v-if="toastVisible"
        class="fixed top-16 left-1/2 -translate-x-1/2 z-[400] px-4 py-2.5 bg-gray-900/90 backdrop-blur-md text-white text-sm rounded-xl shadow-lg whitespace-nowrap"
      >
        {{ toastMessage }}
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
/**
 * 全屏作品预览页 - 抖音风格
 * 支持分享、转发、删除、权限修改、浏览量/点赞用户查看
 */
import type { Article, User } from '~/types'
import { articleApi } from '~/api'
import {
  buildQQShareUrl,
  buildWeiboShareUrl,
  copyToClipboard,
  isMobile,
} from '~/composables/useShare'

definePageMeta({
  layout: 'blank',
  middleware: 'auth',
})

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const { resolveUrl } = useResourceUrl()
const { get: apiGet, post: apiPost, put: apiPut, delete: apiDel } = useApi()

const articleId = computed(() => {
  const id = Number(route.params.id)
  return isNaN(id) ? null : id
})

// ==================== 状态 ====================
const article = ref<Article | null>(null)
const loading = ref(true)
const error = ref<string | null>(null)

// 菜单状态
const activeSheet = ref<'menu' | 'share' | 'forward' | 'permission' | 'views' | null>(null)
const showDeleteConfirm = ref(false)
const deleting = ref(false)

// 分享
const shareUsers = ref<(User & { isMutualFollow?: boolean })[]>([])
const shareUsersLoading = ref(false)
const loadShareUsersError = ref(false)
const shareSearchQuery = ref('')
const shareSendingId = ref<number | null>(null)

// 转发
const linkCopied = ref(false)

// 权限
const savingVisibility = ref<number | null>(null)
const visibilityOptions = [
  { value: 0, label: '公开', desc: '所有人可见', icon: '<svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M3.055 11H5a2 2 0 012 2v1a2 2 0 002 2 2 2 0 012 2v2.945M8 3.935V5.5A2.5 2.5 0 0010.5 8h.5a2 2 0 012 2 2 2 0 104 0 2 2 0 012-2h1.064M15 20.488V18a2 2 0 012-2h3.064M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>' },
  { value: 2, label: '互相关注', desc: '仅互相关注的用户可见', icon: '<svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0z"/></svg>' },
  { value: 1, label: '粉丝', desc: '仅关注你的人可见', icon: '<svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197m13.5-9a2.5 2.5 0 11-5 0 2.5 2.5 0 015 0z"/></svg>' },
  { value: 3, label: '私密', desc: '仅自己可见', icon: '<svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"/></svg>' },
]

// 浏览量/点赞
const viewsTab = ref<'viewers' | 'likers'>('viewers')
const viewerUsers = ref<(User & { isMutualFollow?: boolean })[]>([])
const likerUsers = ref<User[]>([])
const viewsLoading = ref(false)
const likersLoading = ref(false)

// Toast
const toastVisible = ref(false)
const toastMessage = ref('')
let toastTimer: ReturnType<typeof setTimeout> | null = null

// 图片缩放
const imageZoomVisible = ref(false)
const imageZoomSrc = ref('')

// ==================== 计算属性 ====================
const visibilityLabel = computed(() => {
  const v = article.value?.visibility
  if (v === 1) return '粉丝可见'
  if (v === 2) return '互关可见'
  if (v === 3) return '仅自己'
  return '公开'
})

const filteredShareUsers = computed(() => {
  if (!shareSearchQuery.value.trim()) return shareUsers.value
  const q = shareSearchQuery.value.toLowerCase()
  return shareUsers.value.filter(u => u.nickname.toLowerCase().includes(q))
})

const shareUrl = computed(() => {
  if (!import.meta.client) return ''
  return `${window.location.origin}/articles/${articleId.value}`
})

// ==================== 数据获取 ====================
const fetchArticle = async () => {
  if (!articleId.value) {
    error.value = '无效的作品ID'
    loading.value = false
    return
  }
  loading.value = true
  error.value = null
  try {
    const { data } = await articleApi.getArticleDetail(articleId.value)
    article.value = data.data
  } catch (err: any) {
    error.value = err?.response?.data?.message || '作品加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

// ==================== 导航 ====================
const goBack = () => {
  if (window.history.length > 1) {
    router.back()
  } else {
    navigateTo('/user')
  }
}

// ==================== 菜单操作 ====================
const closeAllSheets = () => {
  activeSheet.value = null
  shareSearchQuery.value = ''
}

const openMenuSheet = () => { activeSheet.value = 'menu' }
const openShareSheet = () => { activeSheet.value = 'share'; loadShareUsers() }
const openForwardSheet = () => { activeSheet.value = 'forward' }
const openPermissionSheet = () => { activeSheet.value = 'permission' }
const openViewsSheet = () => { activeSheet.value = 'views'; loadViewerUsers(); loadLikerUsers() }

// ==================== 分享给好友 ====================
const loadShareUsers = async () => {
  if (shareUsers.value.length > 0) return
  shareUsersLoading.value = true
  loadShareUsersError.value = false
  try {
    const userId = userStore.userInfo?.id
    if (!userId) { loadShareUsersError.value = true; return }
    const { socialApi } = await import('~/api')
    // 并行获取粉丝和关注
    const [followersRes, followingRes] = await Promise.all([
      socialApi.getFollowers(userId, { page: 1, pageSize: 200 }),
      socialApi.getFollowing(userId, { page: 1, pageSize: 200 }),
    ])
    const followers = followersRes.data.data?.list || []
    const following = followingRes.data.data?.list || []

    // 合并去重
    const userMap = new Map<number, User & { isMutualFollow?: boolean }>()
    for (const u of followers) {
      userMap.set(u.id, { ...u, isMutualFollow: false })
    }
    for (const u of following) {
      const existing = userMap.get(u.id)
      if (existing) {
        existing.isMutualFollow = true
      } else {
        userMap.set(u.id, { ...u, isMutualFollow: false })
      }
    }
    shareUsers.value = Array.from(userMap.values())
  } catch {
    loadShareUsersError.value = true
  } finally {
    shareUsersLoading.value = false
  }
}

const shareToUser = async (user: User) => {
  if (!article.value || shareSendingId.value) return
  shareSendingId.value = user.id
  try {
    const { socialApi } = await import('~/api')
    const cardContent = JSON.stringify({
      card: 'article_share',
      articleId: article.value.id,
      articleTitle: article.value.title,
      coverImage: article.value.coverImage || '',
      authorName: article.value.author?.nickname || article.value.authorName || '',
      sharedAt: new Date().toISOString(),
    })
    await socialApi.sendMessage(user.id, { content: cardContent, type: 0 })
    showToast(`已分享给 ${user.nickname}`)
    closeAllSheets()
  } catch (err: any) {
    showToast(err?.response?.data?.message || '分享失败，请稍后重试')
  } finally {
    shareSendingId.value = null
  }
}

// ==================== 转发 ====================
const forwardTo = async (platform: string) => {
  if (platform === 'link') {
    const result = await copyToClipboard(shareUrl.value)
    if (result.success) {
      linkCopied.value = true
      showToast('链接已复制到剪贴板')
      setTimeout(() => { linkCopied.value = false }, 2000)
    } else {
      showToast('复制失败')
    }
    return
  }
  try {
    if (platform === 'qq') {
      const qqUrl = buildQQShareUrl({ url: shareUrl.value, title: article.value?.title || '', summary: article.value?.summary || '' })
      window.open(qqUrl, '_blank', 'noopener,noreferrer')
    } else if (platform === 'weibo') {
      const wbUrl = buildWeiboShareUrl({ url: shareUrl.value, title: article.value?.title || '', image: article.value?.coverImage || '' })
      window.open(wbUrl, '_blank', 'noopener,noreferrer')
    } else if (platform === 'wechat') {
      const { tryOpenWechatApp } = await import('~/composables/useShare')
      tryOpenWechatApp(shareUrl.value)
    }
    showToast('转发成功')
  } catch {
    showToast('转发失败')
  }
}

// ==================== 权限 ====================
const updateVisibility = async (visibility: number) => {
  if (!article.value || savingVisibility.value !== null) return
  savingVisibility.value = visibility
  try {
    await articleApi.updateVisibility(article.value.id, visibility)
    article.value.visibility = visibility
    showToast('权限已更新')
  } catch (err: any) {
    showToast(err?.response?.data?.message || '操作失败，请稍后重试')
  } finally {
    savingVisibility.value = null
  }
}

// ==================== 浏览量/点赞用户 ====================
const loadViewerUsers = async () => {
  if (viewerUsers.value.length > 0) return
  viewsLoading.value = true
  try {
    const res = await apiGet<any>(`/articles/${articleId.value}/viewers`, { page: 1, pageSize: 100 })
    viewerUsers.value = res.data?.data?.list || []
  } catch {
    viewerUsers.value = []
  } finally {
    viewsLoading.value = false
  }
}

const loadLikerUsers = async () => {
  if (likerUsers.value.length > 0) return
  likersLoading.value = true
  try {
    const res = await apiGet<any>(`/articles/${articleId.value}/likers`, { page: 1, pageSize: 100 })
    likerUsers.value = res.data?.data?.list || []
  } catch {
    likerUsers.value = []
  } finally {
    likersLoading.value = false
  }
}

// ==================== 删除 ====================
const confirmDeleteArticle = () => {
  activeSheet.value = null
  showDeleteConfirm.value = true
}

const executeDelete = async () => {
  if (!article.value || deleting.value) return
  deleting.value = true
  try {
    await articleApi.deleteArticle(article.value.id)
    showToast('作品已删除')
    setTimeout(() => {
      navigateTo('/user')
    }, 500)
  } catch (err: any) {
    showToast(err?.response?.data?.message || '删除失败，请稍后重试')
    showDeleteConfirm.value = false
  } finally {
    deleting.value = false
  }
}

// ==================== 图片缩放 ====================
const openImageZoom = (src: string) => {
  imageZoomSrc.value = src
  imageZoomVisible.value = true
}

const handleContentClick = (e: MouseEvent) => {
  const target = e.target as HTMLElement
  if (target.tagName === 'IMG') {
    e.preventDefault()
    const img = target as HTMLImageElement
    openImageZoom(img.src)
  }
}

// ==================== 工具函数 ====================
const formatCount = (count: number | undefined) => {
  if (count == null) return '0'
  if (count >= 10000) return `${(count / 10000).toFixed(1)}万`
  if (count >= 1000) return `${(count / 1000).toFixed(1)}k`
  return count.toString()
}

const formatTimestamp = (date: string) => {
  if (!date) return ''
  const d = new Date(date)
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const h = String(d.getHours()).padStart(2, '0')
  const min = String(d.getMinutes()).padStart(2, '0')
  return `${y}-${m}-${day} ${h}:${min}`
}

const showToast = (message: string) => {
  toastMessage.value = message
  toastVisible.value = true
  if (toastTimer) clearTimeout(toastTimer)
  toastTimer = setTimeout(() => {
    toastVisible.value = false
  }, 2000)
}

// ==================== 键盘事件 ====================
const handleKeydown = (e: KeyboardEvent) => {
  if (e.key === 'Escape') {
    if (imageZoomVisible.value) {
      imageZoomVisible.value = false
    } else if (showDeleteConfirm.value) {
      showDeleteConfirm.value = false
    } else if (activeSheet.value) {
      closeAllSheets()
    } else {
      goBack()
    }
  }
}

onMounted(() => {
  fetchArticle()
  document.addEventListener('keydown', handleKeydown)
  // 隐藏 body 滚动
  document.body.style.overflow = 'hidden'
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
  document.body.style.overflow = ''
  if (toastTimer) clearTimeout(toastTimer)
})

useHead({
  title: () => article.value ? `${article.value.title} - 作品预览` : '作品预览 - 知讯',
})
</script>

<style scoped>
/* 底部弹出菜单过渡动画 */
.sheet-slide-enter-active,
.sheet-slide-leave-active {
  transition: opacity 0.25s ease;
}
.sheet-slide-enter-active > :first-child {
  transition: opacity 0.25s ease;
}
.sheet-slide-enter-active > :nth-child(2),
.sheet-slide-leave-active > :nth-child(2) {
  transition: transform 0.3s cubic-bezier(0.32, 0.72, 0, 1), opacity 0.25s ease;
}
.sheet-slide-enter-from,
.sheet-slide-leave-to {
  opacity: 0;
}
.sheet-slide-enter-from > :nth-child(2),
.sheet-slide-leave-to > :nth-child(2) {
  transform: translateY(100%);
}

/* 滚动条样式 */
::-webkit-scrollbar {
  width: 0;
}

/* prose 暗色覆盖 */
:deep(.prose) {
  --tw-prose-body: rgba(255, 255, 255, 0.85);
  --tw-prose-headings: rgba(255, 255, 255, 0.95);
  --tw-prose-links: #60a5fa;
  --tw-prose-bold: rgba(255, 255, 255, 0.95);
  --tw-prose-counters: rgba(255, 255, 255, 0.5);
  --tw-prose-bullets: rgba(255, 255, 255, 0.4);
  --tw-prose-hr: rgba(255, 255, 255, 0.1);
  --tw-prose-quotes: rgba(255, 255, 255, 0.75);
  --tw-prose-quote-borders: rgba(255, 255, 255, 0.2);
  --tw-prose-captions: rgba(255, 255, 255, 0.5);
  --tw-prose-code: rgba(255, 255, 255, 0.85);
  --tw-prose-pre-code: rgba(255, 255, 255, 0.85);
  --tw-prose-pre-bg: rgba(255, 255, 255, 0.05);
  --tw-prose-th-borders: rgba(255, 255, 255, 0.1);
  --tw-prose-td-borders: rgba(255, 255, 255, 0.1);
}
</style>
