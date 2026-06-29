<template>
  <!-- 作品详情页 - 现代编辑感设计 -->
  <div class="article-page">
    <!-- 阅读进度条（顶部固定） -->
    <div class="read-progress" :class="{ visible: scrollProgress > 0.02 }">
      <div class="read-progress-bar" :style="{ width: `${scrollProgress * 100}%` }"></div>
    </div>

    <!-- 加载状态 -->
    <div v-if="pending" class="article-skeleton">
      <LoadingSkeleton type="article" />
    </div>

    <!-- 错误状态 -->
    <div v-else-if="articleError" class="article-error">
      <ErrorRetry :message="articleError?.message || '作品加载失败，请稍后重试'" :on-retry="() => refresh()" />
    </div>

    <!-- 作品内容 -->
    <SwipeArticle v-else-if="article">
      <article class="article-content">
        <!-- 作品主体 -->
        <div class="article-main">
          <!-- 顶部元信息：返回按钮 + 分类 + 日期 -->
          <div class="article-meta-top">
            <button
              v-if="canGoBack"
              class="article-back-btn"
              type="button"
              aria-label="返回"
              @click="goBack"
            >
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                <line x1="19" y1="12" x2="5" y2="12"/>
                <polyline points="12 19 5 12 12 5"/>
              </svg>
              <span class="article-back-btn-text">返回</span>
            </button>
            <span v-if="article.categoryName" class="article-category-badge">
              <span class="article-category-dot"></span>
              {{ article.categoryName }}
            </span>
            <span v-if="article.location" class="article-meta-item">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z" />
                <circle cx="12" cy="10" r="3" />
              </svg>
              {{ article.location }}
            </span>
            <span class="article-meta-item">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="12" cy="12" r="10" />
                <polyline points="12 6 12 12 16 14" />
              </svg>
              {{ formatTimestamp(article.createdAt) }}
            </span>
          </div>

          <!-- 标题 -->
          <h1 class="article-title">
            {{ article.title || '加载中...' }}
          </h1>

          <!-- 副标题/摘要 -->
          <p v-if="article.summary" class="article-summary">
            {{ article.summary }}
          </p>

          <!-- 作者信息栏（精致卡片） -->
          <div class="author-bar">
            <RouterLink :to="`/user/${article.author?.id}`" class="author-avatar-link">
              <UserAvatar :src="article.author?.avatar" :alt="article.author?.nickname || '用户'" size="lg" />
            </RouterLink>
            <div class="author-info">
              <RouterLink :to="`/user/${article.author?.id}`" class="author-name">
                {{ article.author?.nickname || article.authorName || '用户' }}
                <svg v-if="article.author?.role === 'ADMIN' || article.author?.role === 'SUPER_ADMIN'" class="author-badge" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M12 2l2.39 4.84 5.34.78-3.86 3.77.91 5.32L12 14.27l-4.78 2.51.91-5.32-3.86-3.77 5.34-.78L12 2z"/>
                </svg>
              </RouterLink>
              <div class="author-meta">
                <span v-if="article.deviceInfo" class="author-meta-item">
                  来自 {{ article.deviceInfo }}
                </span>
                <span class="author-meta-item">·</span>
                <span class="author-meta-item">{{ readingTime }} 分钟阅读</span>
                <span class="author-meta-item">·</span>
                <span class="author-meta-item">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="meta-icon">
                <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                <circle cx="12" cy="12" r="3" />
              </svg>
                  {{ formatViewCount(article.viewCount) }} 浏览
                </span>
              </div>
            </div>
            <button
              v-if="article.author?.id !== userStore.userInfo?.id"
              class="follow-btn"
              :class="{ 'follow-btn--active': article.author?.isFollowing }"
              type="button"
              @click="toggleFollowAuthor"
            >
              <span v-if="article.author?.isFollowing" class="follow-btn-icon">✓</span>
              <span v-else class="follow-btn-icon">+</span>
              <span>{{ article.author?.isFollowing ? '已关注' : '关注' }}</span>
            </button>
          </div>

          <!-- 作品内容（富文本渲染） -->
          <div ref="contentRef" class="article-prose" v-html="sanitizedContent" @click="handleContentClick"></div>

          <!-- 图片网格（最多9张，3列） -->
          <div v-if="article.images?.length" class="article-images">
            <div
              v-for="(img, idx) in article.images.slice(0, 9)"
              :key="idx"
              class="article-image-item"
              @click="openImageZoom(resolveUrl(img) || img, article.title)"
            >
              <img :src="resolveUrl(img) || img" :alt="`配图${idx + 1}`" loading="lazy" />
            </div>
          </div>

          <!-- 标签 -->
          <div v-if="article.tags?.length" class="article-tags">
            <TopicBadge v-for="tag in article.tags" :key="tag.id" :id="tag.id" :name="tag.name" />
          </div>

          <!-- 互动按钮栏（现代悬浮设计） -->
          <div class="interaction-bar">
            <button
              class="interaction-btn"
              :class="{ active: article.isLiked }"
              type="button"
              @click="toggleLike"
            >
              <span class="interaction-btn-icon">
                <svg viewBox="0 0 24 24" :fill="article.isLiked ? 'currentColor' : 'none'" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/>
                </svg>
              </span>
              <span class="interaction-btn-count">{{ article.likeCount || '点赞' }}</span>
            </button>
            <button
              class="interaction-btn"
              :class="{ active: article.isCollected }"
              type="button"
              @click="toggleCollect"
            >
              <span class="interaction-btn-icon">
                <svg viewBox="0 0 24 24" :fill="article.isCollected ? 'currentColor' : 'none'" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M19 21l-7-5-7 5V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2z"/>
                </svg>
              </span>
              <span class="interaction-btn-count">{{ article.collectCount || '收藏' }}</span>
            </button>
            <button class="interaction-btn" type="button" @click="scrollToComments">
              <span class="interaction-btn-icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
                </svg>
              </span>
              <span class="interaction-btn-count">{{ article.commentCount || '评论' }}</span>
            </button>
            <button v-if="isAuthor" class="interaction-btn" type="button" @click="editArticle">
              <span class="interaction-btn-icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"/>
                </svg>
              </span>
              <span class="interaction-btn-count">编辑</span>
            </button>
            <button v-else class="interaction-btn" type="button" @click="openReport">
              <span class="interaction-btn-icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/>
                  <line x1="12" y1="9" x2="12" y2="13"/>
                  <line x1="12" y1="17" x2="12.01" y2="17"/>
                </svg>
              </span>
              <span class="interaction-btn-count">举报</span>
            </button>
          </div>

          <!-- 作者管理栏 (flex items-center justify-between px-4 py-2.5) -->
          <div v-if="isAuthor" class="author-manage-bar">
            <!-- 浏览量 -->
            <button class="manage-btn" @click="openViewsSheet">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
              </svg>
              <span>{{ formatViewCount(article.viewCount) }} 浏览</span>
            </button>

            <!-- 修改权限 -->
            <button class="manage-btn" @click="openPermissionSheet">
              <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
              </svg>
              <span>{{ visibilityLabel }}</span>
            </button>

            <!-- 更多操作 -->
            <button class="manage-btn" @click="openMenuSheet">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 5v.01M12 12v.01M12 19v.01M12 6a1 1 0 110-2 1 1 0 010 2zm0 7a1 1 0 110-2 1 1 0 010 2zm0 7a1 1 0 110-2 1 1 0 010 2z" />
              </svg>
            </button>
          </div>

          <!-- 评论区 -->
          <section id="comments" class="comments-section">
            <div :ref="(el: any) => commentsLazyTrigger = el" class="h-0"></div>

            <!-- 评论加载中 -->
            <div v-if="commentsLoading" class="comment-status">
              <div class="comment-status-spinner"></div>
              <span>评论加载中…</span>
            </div>

            <!-- 评论加载失败 -->
            <ErrorRetry v-else-if="commentsError" :message="commentsError" :on-retry="retryComments" />

            <CommentSection
              v-else-if="commentsLoaded"
              :article-id="article.id"
              :comments="comments"
              :has-more="hasMoreComments"
              :total="commentTotal"
              :loading="false"
              @submit="submitComment"
              @like="likeComment"
              @delete="deleteComment"
              @report="reportComment"
              @sort-change="handleSortChange"
              @load-more="loadMoreComments"
            />
          </section>

          
        </div>

        <!-- 2xl断点右侧目录/推荐栏 -->
        <aside v-if="tocItems.length" class="article-aside">
          <div class="article-aside-sticky">
            <!-- 目录 -->
            <div v-if="tocItems.length" class="aside-card">
              <header class="aside-card-header">
                <h3 class="aside-card-title">目录</h3>
                <span class="aside-card-badge">{{ tocItems.length }}</span>
              </header>
              <nav class="aside-card-body aside-toc">
                <a
                  v-for="item in tocItems"
                  :key="item.id"
                  :href="'#' + item.id"
                  class="aside-toc-item"
                  :style="{ paddingLeft: (item.level - 1) * 10 + 'px' }"
                >
                  {{ item.text }}
                </a>
              </nav>
            </div>

            
          </div>
        </aside>
      </article>
    </SwipeArticle>

    <!-- 分享海报弹窗 -->
    <ImageZoom v-model:visible="imageZoomVisible" :src="imageZoomSrc" :alt="imageZoomAlt" />

    <!-- 举报弹窗 -->
    <ReportDialog
      :visible="reportVisible"
      type="article"
      :target-id="article?.id || 0"
      @close="reportVisible = false"
      @reported="reportVisible = false"
    />

    <!-- ==================== 作者管理底部弹出菜单 ==================== -->
    <Teleport to="body">
      <Transition name="sheet-slide">
        <div v-if="activeSheet" class="fixed inset-0 z-[250] flex flex-col justify-end">
          <div class="absolute inset-0 bg-black/50" @click="closeAllSheets" />
          <div class="relative bg-white rounded-t-2xl w-full max-h-[75vh] overflow-hidden flex flex-col">
            <div class="flex justify-center pt-3 pb-1">
              <div class="w-8 h-1 rounded-full bg-gray-200" />
            </div>

            <!-- 主菜单 -->
            <template v-if="activeSheet === 'menu'">
              <div class="px-4 py-2">
                <h3 class="text-base font-semibold text-center mb-4">更多操作</h3>
                <div class="grid grid-cols-3 gap-3">
                  <button class="flex flex-col items-center gap-2 p-3 rounded-xl hover:bg-gray-50 transition-colors" @click="openShareSheet">
                    <div class="w-12 h-12 rounded-full bg-blue-50 flex items-center justify-center">
                      <svg class="w-6 h-6 text-blue-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M8.684 13.342C8.886 12.938 9 12.482 9 12c0-.482-.114-.938-.316-1.342m0 2.684a3 3 0 110-2.684m0 2.684l6.632 3.316m-6.632-6l6.632-3.316m0 0a3 3 0 105.367-2.684 3 3 0 00-5.367 2.684zm0 9.316a3 3 0 105.368 2.684 3 3 0 00-5.368-2.684z" />
                      </svg>
                    </div>
                    <span class="text-xs text-gray-600">分享</span>
                  </button>
                  <button class="flex flex-col items-center gap-2 p-3 rounded-xl hover:bg-gray-50 transition-colors" @click="openForwardSheet">
                    <div class="w-12 h-12 rounded-full bg-green-50 flex items-center justify-center">
                      <svg class="w-6 h-6 text-green-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8" />
                      </svg>
                    </div>
                    <span class="text-xs text-gray-600">转发</span>
                  </button>
                  <button class="flex flex-col items-center gap-2 p-3 rounded-xl hover:bg-gray-50 transition-colors" @click="confirmDeleteArticle">
                    <div class="w-12 h-12 rounded-full bg-red-50 flex items-center justify-center">
                      <svg class="w-6 h-6 text-red-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                      </svg>
                    </div>
                    <span class="text-xs text-gray-600">删除</span>
                  </button>
                </div>
              </div>
              <div class="px-4 pb-6 pt-2">
                <button class="w-full py-3 rounded-xl bg-gray-100 text-sm text-gray-600 hover:bg-gray-200 transition-colors" @click="closeAllSheets">取消</button>
              </div>
            </template>

            <!-- 分享 - 分享给好友 -->
            <template v-if="activeSheet === 'share'">
              <div class="px-4 py-2 flex-shrink-0">
                <div class="flex items-center justify-between mb-3">
                  <h3 class="text-base font-semibold">分享给好友</h3>
                  <button class="text-xs text-gray-400" @click="openMenuSheet">返回</button>
                </div>
                <div class="relative mb-3">
                  <svg class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                  </svg>
                  <input
                    v-model="shareSearchQuery"
                    type="text"
                    placeholder="搜索好友..."
                    class="w-full h-10 pl-9 pr-3 rounded-lg bg-gray-50 text-sm border border-gray-200 focus:border-blue-400 focus:outline-none"
                  />
                </div>
              </div>
              <div class="flex-1 overflow-y-auto px-4 pb-4">
                <div v-if="shareUsersLoading" class="flex justify-center py-8">
                  <div class="w-6 h-6 border-2 border-gray-300 border-t-blue-500 rounded-full animate-spin" />
                </div>
                <div v-else-if="filteredShareUsers.length === 0" class="text-center py-8 text-gray-400 text-sm">
                  {{ loadShareUsersError ? '加载失败，请重试' : '暂无好友' }}
                </div>
                <div v-else class="space-y-1">
                  <button
                    v-for="user in filteredShareUsers"
                    :key="user.id"
                    class="flex items-center gap-3 w-full p-2.5 rounded-lg hover:bg-gray-50 transition-colors"
                    :disabled="shareSendingId === user.id"
                    @click="shareToUser(user)"
                  >
                    <UserAvatar :src="user.avatar" :alt="user.nickname" size="md" />
                    <div class="flex-1 min-w-0 text-left">
                      <p class="text-sm font-medium truncate">{{ user.nickname }}</p>
                      <p class="text-xs text-gray-400">
                        {{ user.isMutualFollow ? '互相关注' : user.isFollowing ? '我的粉丝' : '我关注的' }}
                      </p>
                    </div>
                    <div v-if="shareSendingId === user.id" class="w-5 h-5 border-2 border-blue-400 border-t-transparent rounded-full animate-spin" />
                  </button>
                </div>
              </div>
            </template>

            <!-- 转发 -->
            <template v-if="activeSheet === 'forward'">
              <div class="px-4 py-2">
                <div class="flex items-center justify-between mb-3">
                  <h3 class="text-base font-semibold">转发到</h3>
                  <button class="text-xs text-gray-400" @click="openMenuSheet">返回</button>
                </div>
              </div>
              <div class="px-4 pb-6 space-y-1">
                <button class="flex items-center gap-4 w-full p-3 rounded-xl hover:bg-gray-50 transition-colors" @click="forwardTo('qq')">
                  <div class="w-10 h-10 rounded-lg bg-[#12B7F5]/10 flex items-center justify-center shrink-0">
                    <svg class="w-5 h-5 text-[#12B7F5]" viewBox="0 0 24 24" fill="currentColor"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2z"/></svg>
                  </div>
                  <span class="text-sm text-gray-700">QQ</span>
                </button>
                <button class="flex items-center gap-4 w-full p-3 rounded-xl hover:bg-gray-50 transition-colors" @click="forwardTo('wechat')">
                  <div class="w-10 h-10 rounded-lg bg-[#07C160]/10 flex items-center justify-center shrink-0">
                    <svg class="w-5 h-5 text-[#07C160]" viewBox="0 0 24 24" fill="currentColor"><path d="M8.691 2.188C3.891 2.188 0 5.476 0 9.53c0 2.212 1.17 4.203 3.002 5.55a.59.59 0 01.213.665l-.39 1.48c-.019.07-.048.141-.048.213 0 .163.13.295.29.295a.326.326 0 00.167-.054l1.903-1.114a.864.864 0 01.717-.098 10.16 10.16 0 002.837.403c.276 0 .543-.027.811-.05a6.42 6.42 0 01-.246-1.79c0-3.558 3.39-6.441 7.573-6.441.258 0 .509.025.764.042C16.626 4.834 13.004 2.188 8.691 2.188z"/></svg>
                  </div>
                  <span class="text-sm text-gray-700">微信</span>
                </button>
                <button class="flex items-center gap-4 w-full p-3 rounded-xl hover:bg-gray-50 transition-colors" @click="forwardTo('weibo')">
                  <div class="w-10 h-10 rounded-lg bg-[#E6162D]/10 flex items-center justify-center shrink-0">
                    <svg class="w-5 h-5 text-[#E6162D]" viewBox="0 0 24 24" fill="currentColor"><path d="M10.09 16.82c-2.83.3-5.27-1-5.46-2.9-.19-1.9 1.95-3.69 4.78-3.99 2.83-.3 5.27 1 5.46 2.9.19 1.9-1.95 3.69-4.78 3.99z"/></svg>
                  </div>
                  <span class="text-sm text-gray-700">微博</span>
                </button>
                <button class="flex items-center gap-4 w-full p-3 rounded-xl hover:bg-gray-50 transition-colors" @click="forwardTo('link')">
                  <div class="w-10 h-10 rounded-lg bg-gray-100 flex items-center justify-center shrink-0">
                    <svg class="w-5 h-5 text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13.828 10.172a4 4 0 00-5.656 0l-4 4a4 4 0 105.656 5.656l1.102-1.101m-.758-4.899a4 4 0 005.656 0l4-4a4 4 0 00-5.656-5.656l-1.1 1.1" />
                    </svg>
                  </div>
                  <span class="text-sm text-gray-700">{{ linkCopied ? '已复制' : '复制链接' }}</span>
                </button>
              </div>
            </template>

            <!-- 修改权限 -->
            <template v-if="activeSheet === 'permission'">
              <div class="px-4 py-2">
                <h3 class="text-base font-semibold text-center mb-3">修改权限</h3>
                <div class="space-y-1">
                  <button
                    v-for="opt in visibilityOptions"
                    :key="opt.value"
                    class="flex items-center gap-3 w-full p-3 rounded-xl hover:bg-gray-50 transition-colors"
                    @click="updateVisibility(opt.value)"
                  >
                    <div class="w-10 h-10 rounded-lg flex items-center justify-center shrink-0"
                      :class="article?.visibility === opt.value ? 'bg-blue-50' : 'bg-gray-50'"
                    >
                      <div v-html="opt.icon" class="w-5 h-5" :class="article?.visibility === opt.value ? 'text-blue-500' : 'text-gray-400'" />
                    </div>
                    <div class="flex-1 text-left min-w-0">
                      <p class="text-sm font-medium">{{ opt.label }}</p>
                      <p class="text-xs text-gray-400 mt-0.5">{{ opt.desc }}</p>
                    </div>
                    <div v-if="article?.visibility === opt.value" class="w-5 h-5 rounded-full bg-blue-500 flex items-center justify-center shrink-0">
                      <svg class="w-3 h-3 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="3" d="M5 13l4 4L19 7" />
                      </svg>
                    </div>
                    <div v-if="savingVisibility === opt.value" class="w-5 h-5 border-2 border-blue-400 border-t-transparent rounded-full animate-spin shrink-0" />
                  </button>
                </div>
              </div>
              <div class="px-4 pb-6 pt-2">
                <button class="w-full py-3 rounded-xl bg-gray-100 text-sm text-gray-600 hover:bg-gray-200 transition-colors" @click="closeAllSheets">完成</button>
              </div>
            </template>

            <!-- 浏览量与互动 -->
            <template v-if="activeSheet === 'views'">
              <div class="px-4 py-2 flex-shrink-0">
                <h3 class="text-base font-semibold text-center mb-3">浏览与互动</h3>
                <div class="flex rounded-lg bg-gray-100 p-0.5 mb-3">
                  <button class="flex-1 py-2 text-xs font-medium rounded-md transition-colors"
                    :class="viewsTab === 'viewers' ? 'bg-white shadow-sm text-gray-900' : 'text-gray-500'"
                    @click="viewsTab = 'viewers'"
                  >浏览 ({{ article?.viewCount || 0 }})</button>
                  <button class="flex-1 py-2 text-xs font-medium rounded-md transition-colors"
                    :class="viewsTab === 'likers' ? 'bg-white shadow-sm text-gray-900' : 'text-gray-500'"
                    @click="viewsTab = 'likers'"
                  >点赞 ({{ article?.likeCount || 0 }})</button>
                </div>
              </div>
              <div class="flex-1 overflow-y-auto px-4 pb-6">
                <div v-if="viewsTab === 'viewers'">
                  <div v-if="viewsLoading" class="flex justify-center py-8">
                    <div class="w-6 h-6 border-2 border-gray-300 border-t-blue-500 rounded-full animate-spin" />
                  </div>
                  <div v-else-if="viewerUsers.length === 0" class="text-center py-8 text-gray-400 text-sm">暂无浏览记录</div>
                  <div v-else class="space-y-1">
                    <div v-for="user in viewerUsers" :key="user.id" class="flex items-center gap-3 p-2.5 rounded-lg">
                      <UserAvatar :src="user.avatar" :alt="user.nickname" size="sm" />
                      <div class="flex-1 min-w-0">
                        <p class="text-sm font-medium truncate">{{ user.nickname }}</p>
                        <p class="text-xs text-gray-400">{{ user.isMutualFollow ? '互相关注' : '粉丝' }}</p>
                      </div>
                    </div>
                  </div>
                </div>
                <div v-if="viewsTab === 'likers'">
                  <div v-if="likersLoading" class="flex justify-center py-8">
                    <div class="w-6 h-6 border-2 border-gray-300 border-t-blue-500 rounded-full animate-spin" />
                  </div>
                  <div v-else-if="likerUsers.length === 0" class="text-center py-8 text-gray-400 text-sm">暂无点赞</div>
                  <div v-else class="space-y-1">
                    <div v-for="user in likerUsers" :key="user.id" class="flex items-center gap-3 p-2.5 rounded-lg">
                      <UserAvatar :src="user.avatar" :alt="user.nickname" size="sm" />
                      <div class="flex-1 min-w-0">
                        <p class="text-sm font-medium truncate">{{ user.nickname }}</p>
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
          <div class="absolute inset-0 bg-black/50" @click="showDeleteConfirm = false" />
          <div class="relative bg-white rounded-2xl w-[320px] max-w-[88vw] p-6">
            <h3 class="text-lg font-bold mb-2">确认删除</h3>
            <p class="text-sm text-gray-500 mb-5">
              确定要删除作品「{{ article?.title || '无标题' }}」吗？此操作不可撤销。
            </p>
            <div class="flex gap-3">
              <button class="flex-1 py-2.5 rounded-xl text-sm font-medium text-gray-600 bg-gray-100 hover:bg-gray-200 transition-colors" @click="showDeleteConfirm = false">取消</button>
              <button
                class="flex-1 py-2.5 rounded-xl text-sm font-medium text-white bg-red-500 hover:bg-red-600 transition-colors flex items-center justify-center gap-1.5"
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
    </Teleport>
  </div>
</template>

<script setup lang="ts">
/** 作品详情页：SSR渲染 - 现代编辑感设计 */
import type { Comment, User } from '@/types'
import { articleApi } from '@/api'
import { formatTimestamp } from '@/utils/format'
import { sanitizeHtml } from '@/utils/sanitize'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// 返回按钮：优先使用浏览器历史回退，若无历史或来自外部则返回首页
const canGoBack = ref(false)
const goBack = () => {
  if (typeof window !== 'undefined' && window.history.length > 1) {
    router.back()
  } else {
    router.push('/')
  }
}
const { setTitle } = usePageHeaderTitle()
const { resolveUrl } = useResourceUrl()
const { invalidateArticle, invalidateUser } = useCacheInvalidation()
const { recordView, updateDuration } = useViewHistory()
const { isTablet, isLandscape, isMobile } = useBreakpoints()
const { promptOrientationLock, showOrientationPrompt } = useOrientation()

const articleId = computed(() => {
  const id = Number(route.params.id)
  return isNaN(id) ? null : id
})

// 作品数据
const comments = ref<Comment[]>([])
const hasMoreComments = ref(true)
const commentTotal = ref(0)
const commentSort = ref('latest')

interface TocItem { id: string; text: string; level: number }
const tocItems = ref<TocItem[]>([])

const extractToc = (html: string) => {
  const items: TocItem[] = []
  const regex = /<h([1-3])[^>]*id=["']([^"']+)["'][^>]*>(.*?)<\/h\1>/gi
  let match
  while ((match = regex.exec(html)) !== null) {
    const level = parseInt(match[1])
    const id = match[2]
    const text = match[3].replace(/<[^>]+>/g, '').trim()
    if (id && text) items.push({ id, text, level })
  }
  return items
}

let viewStartTime = 0
let durationTimer: ReturnType<typeof setInterval> | null = null
let accumulatedDuration = 0
let viewRecorded = false // 防止同一次加载重复记录
const scrollProgress = ref(0)

const reportVisible = ref(false)
const contentRef = ref<HTMLElement>()
const imageZoomVisible = ref(false)
const imageZoomSrc = ref('')
const imageZoomAlt = ref('')

// ==================== 作者管理 ====================
const isAuthor = computed(() => {
  return article.value?.author?.id === userStore.userInfo?.id
})

const { get: apiGet } = useApi()

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
const linkCopied = ref(false)

// 权限
const savingVisibility = ref<number | null>(null)
const visibilityOptions = [
  { value: 0, label: '公开', desc: '所有人可见', icon: '<svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M3.055 11H5a2 2 0 012 2v1a2 2 0 002 2 2 2 0 012 2v2.945M8 3.935V5.5A2.5 2.5 0 0010.5 8h.5a2 2 0 012 2 2 2 0 104 0 2 2 0 012-2h1.064M15 20.488V18a2 2 0 012-2h3.064M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>' },
  { value: 2, label: '互相关注', desc: '仅互相关注的用户可见', icon: '<svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0z"/></svg>' },
  { value: 1, label: '粉丝', desc: '仅关注你的人可见', icon: '<svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197m13.5-9a2.5 2.5 0 11-5 0 2.5 2.5 0 015 0z"/></svg>' },
  { value: 3, label: '私密', desc: '仅自己可见', icon: '<svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"/></svg>' },
]
const visibilityLabel = computed(() => {
  const v = article.value?.visibility
  if (v === 1) return '粉丝可见'
  if (v === 2) return '互关可见'
  if (v === 3) return '仅自己'
  return '公开'
})

// 浏览量/点赞用户
const viewsTab = ref<'viewers' | 'likers'>('viewers')
const viewerUsers = ref<(User & { isMutualFollow?: boolean })[]>([])
const likerUsers = ref<User[]>([])
const viewsLoading = ref(false)
const likersLoading = ref(false)

const filteredShareUsers = computed(() => {
  if (!shareSearchQuery.value.trim()) return shareUsers.value
  const q = shareSearchQuery.value.toLowerCase()
  return shareUsers.value.filter(u => u.nickname.toLowerCase().includes(q))
})

const shareUrl = computed(() => {
  if (typeof window === 'undefined') return ''
  return `${window.location.origin}/articles/${articleId.value}`
})

const openImageZoom = (src: string, alt?: string) => {
  imageZoomSrc.value = src
  imageZoomAlt.value = alt || ''
  imageZoomVisible.value = true
}

const handleContentClick = (e: MouseEvent) => {
  const target = e.target as HTMLElement
  if (target.tagName === 'IMG') {
    e.preventDefault()
    const img = target as HTMLImageElement
    openImageZoom(img.src, img.alt)
  }
}

// 估算阅读时长（按 300 字/分钟）
const readingTime = computed(() => {
  if (!article.value?.content) return 1
  const text = article.value.content.replace(/<[^>]+>/g, '').trim()
  const minutes = Math.max(1, Math.ceil(text.length / 300))
  return minutes
})

// 浏览量格式化
const formatViewCount = (n: number | undefined): string => {
  if (!n) return '0'
  if (n >= 10000) return `${(n / 10000).toFixed(1)}w`
  if (n >= 1000) return `${(n / 1000).toFixed(1)}k`
  return String(n)
}

// SSR获取作品详情
const { data: article, pending, error: articleError, refresh } = useAsyncData(
  () => `article-${articleId.value}`,
  async () => {
    if (articleId.value === null) throw new Error('作品信息有误，请返回首页')
    const { articleApi } = await import('~/api')
    const response = await articleApi.getArticleDetail(articleId.value!)
    return response.data.data
  },
  { server: true, lazy: false, watch: true },
)

// 作品数据就绪时：记录浏览 + 提取目录 + 启动时长计时器
// （useAsyncData 把 fetch 推迟到 nextTick，onMounted 时 article.value 通常为 undefined，
//  所以必须用 watch 等待数据就绪再触发）
watch(article, (val) => {
  if (!val || viewRecorded) return
  viewRecorded = true
  recordView(val.id, val.title)
  if (val.content) tocItems.value = extractToc(val.content)
  viewStartTime = Date.now()
  durationTimer = setInterval(() => {
    accumulatedDuration = Math.floor((Date.now() - viewStartTime) / 1000)
  }, 1000)
})

// 切换路由（作品 ID 变化）时重置计时状态，让上面的 watch 能再次触发新作品的记录
watch(() => articleId.value, () => {
  viewRecorded = false
  accumulatedDuration = 0
  if (durationTimer) { clearInterval(durationTimer); durationTimer = null }
})

const sanitizedContent = computed(() => {
  return article.value?.content ? sanitizeHtml(article.value.content) : ''
})

onMounted(() => {
  if (true && article.value) {
    const hasIncompleteData =
      !article.value.title ||
      !article.value.content ||
      (!article.value.author?.nickname && !article.value.authorName)
    if (hasIncompleteData) refresh()
  }
})

watch(article, (val) => { if (val?.title) setTitle(val.title) }, { immediate: true })

// 核心修复：动态路由 articleId 变化时（点击作品切换详情）重置所有相关本地状态
watch(() => articleId.value, async (newId, oldId) => {
  if (newId === oldId || newId == null) return
  // 重置评论相关状态（避免上一个作品的评论残留）
  comments.value = []
  hasMoreComments.value = true
  commentTotal.value = 0
  commentSort.value = 'latest'
  tocItems.value = []
  imageZoomVisible.value = false
  scrollProgress.value = 0
  // 重置管理菜单状态
  activeSheet.value = null
  showDeleteConfirm.value = false
  viewerUsers.value = []
  likerUsers.value = []
  shareUsers.value = []
  // 重置懒加载评论：清空 loaded 标志，下次进入视口会重新加载
  resetComments()
  // 滚动到顶部
  window.scrollTo({ top: 0, behavior: 'instant' as ScrollBehavior })
})

// 点赞
const toggleLike = async () => {
  if (!article.value) return
  if (!userStore.isLoggedIn) { router.push('/login'); return }
  try {
    const { interactionApi } = await import('~/api')
    const response = await interactionApi.toggleLike(article.value.id)
    article.value.isLiked = response.data.data.isLiked
    article.value.likeCount = response.data.data.likeCount
    invalidateArticle()
  } catch (e: any) {
    console.error('点赞操作失败:', e?.message || e)
    ElMessage.error(e?.message || '操作失败，请稍后重试')
  }
}

// 收藏
const toggleCollect = async () => {
  if (!article.value) return
  if (!userStore.isLoggedIn) { router.push('/login'); return }
  try {
    const { interactionApi } = await import('~/api')
    const response = await interactionApi.toggleCollect(article.value.id)
    article.value.isCollected = response.data.data.isCollected
    article.value.collectCount = response.data.data.collectCount
    invalidateArticle()
  } catch (e: any) {
    console.error('收藏操作失败:', e?.message || e)
    ElMessage.error(e?.message || '操作失败，请稍后重试')
  }
}

// 关注作者
const toggleFollowAuthor = async () => {
  if (!article.value?.author) return
  if (!userStore.isLoggedIn) { router.push('/login'); return }
  try {
    const { socialApi } = await import('~/api')
    await socialApi.toggleFollow(article.value.author.id)
    article.value.author.isFollowing = !article.value.author.isFollowing
    invalidateUser()
  } catch (error: any) {
    console.error('关注操作失败，请稍后重试')
  }
}

// 提交评论
const submitComment = async (data: { content?: string; parentId?: number; replyUserId?: number; images?: string[] }) => {
  if (!article.value) return
  if (!userStore.isLoggedIn) { navigateTo({ path: '/login' }); return }
  const { interactionApi } = await import('~/api')
  const payload: { content?: string; parentId?: number; replyUserId?: number; images?: string[] } = {}
  if (data.content) payload.content = data.content
  if (data.parentId) payload.parentId = data.parentId
  if (data.replyUserId) payload.replyUserId = data.replyUserId
  if (data.images && data.images.length) payload.images = data.images
  const response = await interactionApi.createComment(article.value.id, payload)
  comments.value.unshift(response.data.data)
  article.value.commentCount++
  invalidateArticle()
}

const likeComment = async (commentId: number) => {
  if (!userStore.isLoggedIn) { router.push('/login'); return }
  try {
    const { interactionApi } = await import('~/api')
    const response = await interactionApi.likeComment(commentId)
    const { isLiked, likeCount } = response.data.data
    const updateCommentInList = (list: Comment[]) => {
      for (const comment of list) {
        if (comment.id === commentId) {
          comment.isLiked = isLiked
          comment.likeCount = likeCount
          return true
        }
        if (comment.replies?.length && updateCommentInList(comment.replies)) return true
      }
      return false
    }
    updateCommentInList(comments.value)
  } catch { /* 静默 */ }
}

const deleteComment = async (commentId: number) => {
  try {
    const { interactionApi } = await import('~/api')
    await interactionApi.deleteComment(commentId)
    const removeCommentFromList = (list: Comment[]): Comment[] => {
      return list.filter(comment => {
        if (comment.id === commentId) return false
        if (comment.replies?.length) comment.replies = removeCommentFromList(comment.replies)
        return true
      })
    }
    comments.value = removeCommentFromList(comments.value)
    if (article.value) article.value.commentCount--
    commentTotal.value = Math.max(0, commentTotal.value - 1)
  } catch { /* 静默 */ }
}

const openReport = () => {
  if (!userStore.isLoggedIn) { router.push('/login'); return }
  reportVisible.value = true
}

const reportComment = async (data: { commentId: number; reason?: string }) => {
  if (!userStore.isLoggedIn) { router.push('/login'); return }
  try {
    const { interactionApi } = await import('~/api')
    await interactionApi.reportComment(data.commentId, { reason: data.reason })
  } catch { /* 静默 */ }
}

const handleSortChange = async (sort: string) => {
  commentSort.value = sort
  if (!article.value) return
  try {
    const { interactionApi } = await import('~/api')
    const response = await interactionApi.getComments(article.value.id, {
      page: 1, pageSize: 10, sort,
    })
    const data = response.data.data
    comments.value = data.list
    commentTotal.value = data.total
    hasMoreComments.value = comments.value.length < data.total
  } catch { /* 静默 */ }
}

const loadMoreComments = async () => {
  if (!article.value) return
  try {
    const { interactionApi } = await import('~/api')
    const response = await interactionApi.getComments(article.value.id, {
      page: Math.ceil(comments.value.length / 10) + 1,
      pageSize: 10,
      sort: commentSort.value,
    })
    const data = response.data.data
    comments.value.push(...data.list)
    hasMoreComments.value = comments.value.length < data.total
  } catch { /* 静默 */ }
}

const {
  data: commentsData,
  loading: commentsLoading,
  error: commentsError,
  loaded: commentsLoaded,
  triggerRef: commentsLazyTrigger,
  retry: retryComments,
  reset: resetComments,
} = useLazyData<{ list: Comment[]; total: number }>({
  fetchFn: async () => {
    const { interactionApi } = await import('~/api')
    const response = await interactionApi.getComments(article.value!.id, { page: 1, pageSize: 10, sort: commentSort.value })
    return response.data.data
  },
  defaultData: { list: [], total: 0 },
})

watch(commentsData, (val) => {
  if (val) {
    comments.value = val.list
    commentTotal.value = val.total
    hasMoreComments.value = val.list.length < val.total
  }
})

const scrollToComments = () => {
  const el = document.getElementById('comments')
  if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

// ==================== 作者管理操作 ====================
const editArticle = () => {
  if (article.value) router.push(`/editor?id=${article.value.id}`)
}

// 菜单操作
const closeAllSheets = () => {
  activeSheet.value = null
  shareSearchQuery.value = ''
}
const openMenuSheet = () => { activeSheet.value = 'menu' }
const openShareSheet = () => { activeSheet.value = 'share'; loadShareUsers() }
const openForwardSheet = () => { activeSheet.value = 'forward' }
const openPermissionSheet = () => { activeSheet.value = 'permission' }
const openViewsSheet = () => { activeSheet.value = 'views'; loadViewerUsers(); loadLikerUsers() }

// 分享给好友
const loadShareUsers = async () => {
  if (shareUsers.value.length > 0) return
  shareUsersLoading.value = true
  loadShareUsersError.value = false
  try {
    const userId = userStore.userInfo?.id
    if (!userId) { loadShareUsersError.value = true; return }
    const { socialApi } = await import('~/api')
    const [followersRes, followingRes] = await Promise.all([
      socialApi.getFollowers(userId, { page: 1, pageSize: 200 }),
      socialApi.getFollowing(userId, { page: 1, pageSize: 200 }),
    ])
    const followers = followersRes.data.data?.list || []
    const following = followingRes.data.data?.list || []
    const userMap = new Map<number, User & { isMutualFollow?: boolean }>()
    for (const u of followers) userMap.set(u.id, { ...u, isMutualFollow: false })
    for (const u of following) {
      const existing = userMap.get(u.id)
      if (existing) existing.isMutualFollow = true
      else userMap.set(u.id, { ...u, isMutualFollow: false })
    }
    shareUsers.value = Array.from(userMap.values())
  } catch { loadShareUsersError.value = true }
  finally { shareUsersLoading.value = false }
}

const shareToUser = async (user: User) => {
  if (!article.value || shareSendingId.value) return
  shareSendingId.value = user.id
  try {
    const { socialApi } = await import('~/api')
    const cardContent = JSON.stringify({
      card: 'article_share', articleId: article.value.id,
      articleTitle: article.value.title,
      coverImage: article.value.coverImage || '',
      authorName: article.value.author?.nickname || article.value.authorName || '',
      sharedAt: new Date().toISOString(),
    })
    await socialApi.sendMessage(user.id, { content: cardContent, type: 0 })
    ElMessage.success(`已分享给 ${user.nickname}`)
    closeAllSheets()
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || '分享失败，请稍后重试')
  } finally { shareSendingId.value = null }
}

// 转发
const forwardTo = async (platform: string) => {
  if (platform === 'link') {
    try {
      await navigator.clipboard.writeText(shareUrl.value)
      linkCopied.value = true
      ElMessage.success('链接已复制')
      setTimeout(() => { linkCopied.value = false }, 2000)
    } catch { ElMessage.error('复制失败') }
    return
  }
  try {
    const { buildQQShareUrl, buildWeiboShareUrl } = await import('~/composables/useShare')
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
    ElMessage.success('转发成功')
  } catch { ElMessage.error('转发失败') }
}

// 修改权限
const updateVisibility = async (visibility: number) => {
  if (!article.value || savingVisibility.value !== null) return
  savingVisibility.value = visibility
  try {
    await articleApi.updateVisibility(article.value.id, visibility)
    article.value.visibility = visibility
    ElMessage.success('权限已更新')
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || '操作失败，请稍后重试')
  } finally { savingVisibility.value = null }
}

// 浏览量/点赞用户
const loadViewerUsers = async () => {
  if (viewerUsers.value.length > 0) return
  viewsLoading.value = true
  try {
    const res = await apiGet<any>(`/articles/${articleId.value}/viewers`, { page: 1, pageSize: 100 })
    viewerUsers.value = res.data?.data?.list || []
  } catch { viewerUsers.value = [] }
  finally { viewsLoading.value = false }
}

const loadLikerUsers = async () => {
  if (likerUsers.value.length > 0) return
  likersLoading.value = true
  try {
    const res = await apiGet<any>(`/articles/${articleId.value}/likers`, { page: 1, pageSize: 100 })
    likerUsers.value = res.data?.data?.list || []
  } catch { likerUsers.value = [] }
  finally { likersLoading.value = false }
}

// 删除
const confirmDeleteArticle = () => {
  activeSheet.value = null
  showDeleteConfirm.value = true
}

const executeDelete = async () => {
  if (!article.value || deleting.value) return
  deleting.value = true
  try {
    await articleApi.deleteArticle(article.value.id)
    ElMessage.success('作品已删除')
    setTimeout(() => {
      if (window.history.length > 1) router.back()
      else router.push('/user')
    }, 500)
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || '删除失败，请稍后重试')
    showDeleteConfirm.value = false
  } finally { deleting.value = false }
}

// 滚动监听：阅读进度 + 滚动保存
const handleScroll = () => {
  const docEl = document.documentElement
  const totalHeight = docEl.scrollHeight - docEl.clientHeight
  scrollProgress.value = totalHeight > 0 ? Math.min(1, docEl.scrollTop / totalHeight) : 0
}

let scrollHandler: (() => void) | null = null
onMounted(() => {
  // 初始化返回按钮状态：只要有历史就允许返回
  canGoBack.value = window.history.length > 1

  if (window.location.hash === '#comments') {
    setTimeout(() => {
      const el = document.getElementById('comments')
      if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' })
    }, 500)
  }

  scrollHandler = handleScroll
  window.addEventListener('scroll', scrollHandler, { passive: true })

  if (isTablet.value && !isLandscape.value) {
    promptOrientationLock('landscape')
  }
})

onUnmounted(() => {
  if (durationTimer) { clearInterval(durationTimer); durationTimer = null }
  if (scrollHandler) window.removeEventListener('scroll', scrollHandler)
  if (article.value && accumulatedDuration > 0) {
    updateDuration(article.value.id, accumulatedDuration)
  }
})

useHead({
  title: () => article.value ? `${article.value.title} - 知讯` : '作品无法加载 - 知讯',
})
</script>

<style scoped>
/* ========== 页面容器 ========== */
.article-page {
  position: relative;
  max-width: 1200px;
  margin: 0 auto;
  padding: 4px 16px 12px;
  /* 移动端为底部评论输入框预留空间（输入框约 56px + tabbar 60px + 安全区） */
  padding-bottom: calc(140px + env(safe-area-inset-bottom, 0px));
}
@media (min-width: 768px) {
  .article-page {
    padding: 8px 24px 16px;
    padding-bottom: calc(16px + env(safe-area-inset-bottom, 0px));
  }
}
@media (min-width: 1536px) {
  .article-page {
    max-width: 1320px;
  }
}

/* ========== 顶部阅读进度条 ========== */
.read-progress {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  z-index: 90;
  pointer-events: none;
  background: transparent;
  transition: opacity 200ms ease;
  opacity: 0;
}
.read-progress.visible { opacity: 1; }
.read-progress-bar {
  height: 100%;
  background: linear-gradient(90deg, var(--zh-primary) 0%, #a78bfa 100%);
  transition: width 80ms linear;
  box-shadow: 0 0 8px rgba(var(--zh-primary-rgb), 0.4);
}

/* ========== 加载/错误状态 ========== */
.article-skeleton,
.article-error {
  padding: 16px 0;
}

/* ========== 作品主体布局 ========== */
.article-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
  animation: article-in 400ms cubic-bezier(0.16, 1, 0.3, 1);
}
@keyframes article-in {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

.article-main {
  flex: 1;
  min-width: 0;
  max-width: 800px;
  width: 100%;
  margin: 0 auto;
}

@media (min-width: 1536px) {
  .article-content {
    flex-direction: row;
    align-items: flex-start;
  }
  .article-main {
    margin: 0;
  }
}

/* ========== 顶部元信息 ========== */
.article-meta-top {
  position: sticky;
  top: 3px; /* 低于阅读进度条(3px) */
  z-index: 80;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 8px;
  padding: 4px 8px;
  font-size: 10px;
  color: var(--zh-text-tertiary);
  background: color-mix(in srgb, var(--zh-bg) 85%, transparent);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border-radius: 8px;
}
/* 返回按钮 */
.article-back-btn {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  padding: 2px 6px 2px 4px;
  background: var(--zh-bg-elevated);
  color: var(--zh-text-secondary);
  border: 1px solid var(--zh-border);
  border-radius: 999px;
  font-size: 10px;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 150ms ease, color 150ms ease, border-color 150ms ease, transform 120ms ease;
}
.article-back-btn:hover {
  background: var(--zh-primary-bg);
  color: var(--zh-primary);
  border-color: var(--zh-primary);
}
.article-back-btn:active {
  transform: scale(0.96);
}
.article-back-btn svg {
  width: 10px;
  height: 10px;
}
.article-back-btn-text {
  letter-spacing: 0.02em;
}
.article-category-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 6px;
  background: var(--zh-primary-bg);
  color: var(--zh-primary);
  border-radius: 999px;
  font-weight: 600;
  font-size: 10px;
  letter-spacing: 0.02em;
}
.article-category-dot {
  width: 4px;
  height: 4px;
  background: var(--zh-primary);
  border-radius: 50%;
  animation: dot-pulse 2s ease-in-out infinite;
}
@keyframes dot-pulse {
  0%, 100% { transform: scale(1); opacity: 1; }
  50% { transform: scale(1.3); opacity: 0.6; }
}
.article-meta-item {
  display: inline-flex;
  align-items: center;
  gap: 3px;
}
.article-meta-item svg {
  width: 10px;
  height: 10px;
  opacity: 0.7;
}

/* ========== 标题 ========== */
.article-title {
  font-size: clamp(1.25rem, 4vw, 1.875rem);
  font-weight: 800;
  line-height: 1.25;
  letter-spacing: -0.025em;
  color: var(--zh-text);
  margin: 0 0 8px;
  font-feature-settings: 'kern' 1, 'liga' 1;
  text-wrap: balance;
}

/* ========== 副标题/摘要 ========== */
.article-summary {
  font-size: 13px;
  line-height: 1.55;
  color: var(--zh-text-secondary);
  margin: 0 0 14px;
  padding: 8px 12px;
  background: var(--zh-bg-hover);
  border-left: 3px solid var(--zh-primary);
  border-radius: 0 var(--zh-radius-md) var(--zh-radius-md) 0;
  font-weight: 400;
}

/* ========== 作者信息栏 ========== */
.author-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  margin-bottom: 14px;
  background: var(--zh-bg-elevated);
  border: 1px solid var(--zh-border);
  border-radius: var(--zh-radius-lg);
  box-shadow: var(--zh-shadow-xs);
  transition: box-shadow 200ms ease, border-color 200ms ease;
}
.author-bar:hover {
  border-color: var(--zh-border-focus);
  box-shadow: var(--zh-shadow-sm);
}
.author-avatar-link {
  flex-shrink: 0;
  transition: transform 200ms cubic-bezier(0.16, 1, 0.3, 1);
}
.author-avatar-link:hover { transform: scale(1.05); }
.author-info { flex: 1; min-width: 0; }
.author-name {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  font-size: 13px;
  font-weight: 700;
  color: var(--zh-text);
  text-decoration: none;
  margin-bottom: 2px;
  letter-spacing: -0.01em;
}
.author-name:hover { color: var(--zh-primary); }
.author-badge {
  width: 12px;
  height: 12px;
  color: var(--zh-warning);
}
.author-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 3px;
  font-size: 11px;
  color: var(--zh-text-tertiary);
}
.author-meta-item { display: inline-flex; align-items: center; gap: 3px; }
.meta-icon { width: 11px; height: 11px; opacity: 0.7; }

.follow-btn {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  padding: 5px 12px;
  background: var(--zh-primary);
  color: #fff;
  border: none;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: background 200ms ease, transform 150ms ease, box-shadow 200ms ease;
  box-shadow: 0 2px 8px rgba(var(--zh-primary-rgb), 0.25);
}
.follow-btn:hover {
  background: var(--zh-primary-light);
  box-shadow: 0 4px 12px rgba(var(--zh-primary-rgb), 0.35);
}
.follow-btn:active { transform: scale(0.97); }
.follow-btn--active {
  background: var(--zh-bg-hover);
  color: var(--zh-text-secondary);
  box-shadow: none;
  border: 1px solid var(--zh-border);
}
.follow-btn--active:hover {
  background: var(--zh-bg-active);
  color: var(--zh-text);
}
.follow-btn-icon {
  font-size: 12px;
  font-weight: 700;
  line-height: 1;
}

/* ========== 作品正文 ========== */
.article-prose {
  font-size: 14px;
  line-height: 1.75;
  color: var(--zh-text);
  word-wrap: break-word;
  font-feature-settings: 'kern' 1, 'liga' 1;
  margin-bottom: 14px;
}
.article-prose :deep(p) {
  margin: 0 0 0.9em;
  letter-spacing: 0.01em;
}
.article-prose :deep(h1),
.article-prose :deep(h2),
.article-prose :deep(h3),
.article-prose :deep(h4) {
  font-weight: 700;
  letter-spacing: -0.015em;
  margin: 1.3em 0 0.5em;
  scroll-margin-top: 80px;
}
.article-prose :deep(h1) { font-size: 1.4em; }
.article-prose :deep(h2) { font-size: 1.25em; }
.article-prose :deep(h3) { font-size: 1.1em; }
.article-prose :deep(a) {
  color: var(--zh-primary);
  text-decoration: none;
  border-bottom: 1px solid rgba(var(--zh-primary-rgb), 0.3);
  transition: border-color 150ms ease, color 150ms ease;
}
.article-prose :deep(a:hover) {
  color: var(--zh-primary-light);
  border-bottom-color: var(--zh-primary);
}
.article-prose :deep(blockquote) {
  margin: 1.2em 0;
  padding: 8px 14px;
  border-left: 3px solid var(--zh-primary);
  background: var(--zh-primary-bg);
  border-radius: 0 var(--zh-radius-md) var(--zh-radius-md) 0;
  color: var(--zh-text-secondary);
  font-style: italic;
}
.article-prose :deep(blockquote p) { margin: 0; }
.article-prose :deep(code) {
  padding: 1px 5px;
  background: var(--zh-bg-hover);
  border-radius: 4px;
  font-family: 'JetBrains Mono', 'SFMono-Regular', Consolas, monospace;
  font-size: 0.88em;
  color: var(--zh-primary-dark);
}
:global(.dark) .article-prose :deep(code) { color: var(--zh-primary-light); }
.article-prose :deep(pre) {
  margin: 1.2em 0;
  padding: 12px;
  background: var(--zh-bg-hover);
  border-radius: var(--zh-radius-md);
  overflow-x: auto;
  font-size: 0.85em;
  line-height: 1.5;
}
.article-prose :deep(pre code) {
  padding: 0;
  background: transparent;
  color: inherit;
}
.article-prose :deep(ul),
.article-prose :deep(ol) {
  margin: 0.8em 0;
  padding-left: 1.4em;
}
.article-prose :deep(li) {
  margin: 0.35em 0;
}
.article-prose :deep(img) {
  max-width: 100%;
  height: auto;
  border-radius: var(--zh-radius-md);
  margin: 0.8em 0;
  display: block;
}
.article-prose :deep(strong) {
  color: var(--zh-text);
  font-weight: 700;
}
.article-prose :deep(hr) {
  border: none;
  height: 1px;
  background: var(--zh-border);
  margin: 1.6em 0;
}
.article-prose :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 1.2em 0;
  font-size: 0.9em;
}
.article-prose :deep(th),
.article-prose :deep(td) {
  padding: 6px 10px;
  border: 1px solid var(--zh-border);
  text-align: left;
}
.article-prose :deep(th) {
  background: var(--zh-bg-hover);
  font-weight: 600;
}

/* ========== 图片网格 ========== */
.article-images {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 4px;
  margin: 12px 0 14px;
  border-radius: var(--zh-radius-md);
  overflow: hidden;
}
.article-image-item {
  position: relative;
  aspect-ratio: 1;
  overflow: hidden;
  background: var(--zh-bg-hover);
  cursor: pointer;
  transition: opacity 200ms ease;
}
.article-image-item:hover { opacity: 0.92; }
.article-image-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 400ms cubic-bezier(0.16, 1, 0.3, 1);
}
.article-image-item:hover img { transform: scale(1.05); }

/* ========== 标签 ========== */
.article-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
  margin: 12px 0 14px;
}

/* ========== 互动按钮栏 ========== */
.interaction-bar {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 6px;
  padding: 8px;
  margin: 16px 0;
  background: var(--zh-bg-elevated);
  border: 1px solid var(--zh-border);
  border-radius: var(--zh-radius-lg);
  box-shadow: var(--zh-shadow-xs);
}
.interaction-btn {
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 3px;
  padding: 6px 4px;
  background: transparent;
  border: none;
  border-radius: var(--zh-radius-md);
  color: var(--zh-text-secondary);
  font-size: 11px;
  cursor: pointer;
  transition: background 150ms ease, color 150ms ease, transform 120ms ease;
  min-height: 44px;
}
.interaction-btn:hover {
  background: var(--zh-bg-hover);
  color: var(--zh-text);
}
.interaction-btn:active { transform: scale(0.96); }
.interaction-btn.active {
  color: var(--zh-primary);
  background: var(--zh-primary-bg);
}
.interaction-btn-icon svg {
  width: 18px;
  height: 18px;
  stroke-width: 2;
  transition: transform 200ms cubic-bezier(0.16, 1, 0.3, 1);
}
.interaction-btn:hover .interaction-btn-icon svg {
  transform: translateY(-1px);
}
.interaction-btn.active .interaction-btn-icon svg {
  transform: scale(1.1);
}
.interaction-btn-count {
  font-weight: 500;
  font-size: 11px;
  font-variant-numeric: tabular-nums;
}

/* ========== 评论区 ========== */
.comments-section {
  margin: 16px 0;
  /* 移动端需要为底部输入框预留空间 */
  padding-bottom: calc(80px + env(safe-area-inset-bottom, 0px));
}
@media (min-width: 768px) {
  .comments-section {
    padding-bottom: 0;
  }
}

.comment-status {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 24px 0;
  color: var(--zh-text-tertiary);
  font-size: 12px;
}
.comment-status-spinner {
  width: 14px;
  height: 14px;
  border: 2px solid var(--zh-border);
  border-top-color: var(--zh-primary);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

/* ========== 2xl 侧边栏 ========== */
.article-aside {
  width: 100%;
}
@media (min-width: 1536px) {
  .article-aside {
    width: 280px;
    flex-shrink: 0;
  }
}
.article-aside-sticky {
  position: sticky;
  top: 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.aside-card {
  background: var(--zh-bg-elevated);
  border: 1px solid var(--zh-border);
  border-radius: var(--zh-radius-lg);
  overflow: hidden;
  box-shadow: var(--zh-shadow-xs);
}
.aside-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  border-bottom: 1px solid var(--zh-border-light);
  background: var(--zh-bg-hover);
}
.aside-card-title {
  font-size: 12px;
  font-weight: 700;
  color: var(--zh-text);
  margin: 0;
  letter-spacing: 0.02em;
  text-transform: uppercase;
}
.aside-card-badge {
  font-size: 10px;
  color: var(--zh-text-tertiary);
  font-weight: 500;
  font-variant-numeric: tabular-nums;
}
.aside-card-body {
  padding: 6px;
  max-height: 60vh;
  overflow-y: auto;
}
.aside-toc {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.aside-toc-item {
  display: block;
  padding: 4px 8px;
  font-size: 11.5px;
  color: var(--zh-text-secondary);
  text-decoration: none;
  border-radius: var(--zh-radius-sm);
  transition: background 150ms ease, color 150ms ease;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.aside-toc-item:hover {
  background: var(--zh-primary-bg);
  color: var(--zh-primary);
}

/* ========== 作者管理栏 ========== */
.author-manage-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.625rem 1rem;
  margin: 0 0 16px;
  background: var(--zh-bg-elevated);
  border: 1px solid var(--zh-border);
  border-radius: var(--zh-radius-lg);
  box-shadow: var(--zh-shadow-xs);
}
.manage-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.375rem 0.75rem;
  border-radius: 9999px;
  background: var(--zh-bg-hover);
  color: var(--zh-text-secondary);
  font-size: 0.75rem;
  font-weight: 500;
  border: none;
  cursor: pointer;
  transition: all 200ms ease;
}
.manage-btn:hover {
  background: var(--zh-primary-bg);
  color: var(--zh-primary);
}
.manage-btn:active {
  transform: scale(0.96);
}
.manage-btn svg {
  flex-shrink: 0;
}

/* ========== 底部弹出菜单过渡 ========== */
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
</style>