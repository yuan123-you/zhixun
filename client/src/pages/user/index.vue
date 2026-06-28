<template>
  <!-- 个人中心 - 知讯优化版 -->
  <div class="user-page">
    <!-- 个人资料卡 - 渐变横幅头部 -->
    <div class="profile-banner">
      <!-- 背景纹理 -->
      <div class="banner-pattern"></div>
      <!-- 横幅内容 -->
      <div class="banner-inner">
        <!-- 头像（可点击编辑） -->
        <div class="profile-avatar-wrap">
          <div class="relative group cursor-pointer shrink-0" @click="triggerAvatarUpload">
            <UserAvatar :src="userStore.userInfo?.avatar" alt="头像" :size="isMobile ? 'lg' : 'xl'" />
            <div class="absolute inset-0 bg-black/40 rounded-full opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center">
              <svg class="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 9a2 2 0 012-2h.93a2 2 0 001.664-.89l.812-1.22A2 2 0 0110.07 4h3.86a2 2 0 011.664.89l.812 1.22A2 2 0 0018.07 7H19a2 2 0 012 2v9a2 2 0 01-2 2H5a2 2 0 01-2-2V9z" />
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 13a3 3 0 11-6 0 3 3 0 016 0z" />
              </svg>
            </div>
          </div>
        </div>
        <input ref="avatarInput" type="file" accept="image/jpeg,image/png,image/gif,image/webp,image/bmp" class="hidden" @change="handleAvatarChange" />

        <!-- 昵称 -->
        <h2 class="profile-nickname">{{ userStore.userInfo?.nickname }}</h2>

        <!-- ID 和标签 -->
        <div class="profile-meta-row">
          <span>知讯号: {{ userStore.userInfo?.uid }}</span>
          <span v-if="userStore.userInfo?.showGenderOnProfile && userStore.userInfo?.gender"> · {{ userStore.userInfo.gender === 1 ? '男' : userStore.userInfo.gender === 2 ? '女' : '' }}</span>
          <span v-if="userStore.userInfo?.province"> · {{ userStore.userInfo?.province }}</span>
          <span v-if="userStore.userInfo?.ipLocation" class="profile-ip" :title="`根据你的 IP 自动定位：${userStore.userInfo.ipLocation}`"> · IP属地：{{ userStore.userInfo?.ipLocation }}</span>
        </div>

        <!-- 简介 -->
        <p v-if="userStore.userInfo?.bio" class="profile-bio">{{ userStore.userInfo?.bio }}</p>

        <!-- 统计数据行 + 编辑资料按钮 - 水平排列 -->
        <div class="profile-actions">
          <!-- 统计数据行 - 带动画计数和分隔线 -->
          <div class="stats-row">
            <RouterLink to="/user/articles" class="stat-item stat-link">
              <span class="stat-number animate-count-up">{{ userStore.userInfo?.articleCount ?? 0 }}</span>
              <span class="stat-label">作品</span>
            </RouterLink>
            <div class="stat-divider"></div>
            <div class="stat-item">
              <span class="stat-number animate-count-up">{{ userStore.userInfo?.totalLikeCount ?? 0 }}</span>
              <span class="stat-label">获赞</span>
            </div>
            <div class="stat-divider"></div>
            <RouterLink to="/user/following" class="stat-item stat-link">
              <span class="stat-number animate-count-up">{{ userStore.userInfo?.followCount ?? 0 }}</span>
              <span class="stat-label">关注</span>
            </RouterLink>
            <div class="stat-divider"></div>
            <RouterLink to="/user/followers" class="stat-item stat-link">
              <span class="stat-number animate-count-up">{{ userStore.userInfo?.followerCount ?? 0 }}</span>
              <span class="stat-label">粉丝</span>
            </RouterLink>
          </div>

          <!-- 编辑资料按钮 -->
          <RouterLink to="/user/edit" class="btn-edit-profile">
            <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
            </svg>
            编辑资料
          </RouterLink>
        </div>
      </div>
    </div>

    <!-- Tab切换 - 药丸风格带图标和滑动指示器 -->
    <div class="tab-bar-wrapper">
      <div class="tab-bar">
        <button
          v-for="tab in tabs"
          :key="tab.key"
          class="tab-btn"
          :class="{ 'tab-active': activeTab === tab.key }"
          @click="switchTab(tab.key)"
        >
          <span class="tab-icon" v-html="getTabIcon(tab.key)"></span>
          <span class="tab-label">{{ tab.label }}</span>
          
        </button>
      </div>
    </div>

    <!-- Tab内容 - 带淡入淡出过渡 -->
    <div class="tab-content-wrapper">
      <Transition name="tab-fade" mode="out-in">
        <div :key="activeTab" class="tab-content-inner">
          <!-- 作品 -->
          <ErrorRetry v-if="activeTab === 'published' && tabError && !publishedArticles.length" :message="tabError" :on-retry="retryTabData" />
          <div v-if="activeTab === 'published' && !tabError">
            <div v-if="loading && publishedArticles.length === 0" class="article-grid">
              <div v-for="i in 6" :key="i" class="skeleton-card" />
            </div>
            <div v-else-if="!loading && publishedArticles.length === 0" class="empty-state">
              <div class="empty-illustration">
                <svg class="empty-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M19 20H5a2 2 0 01-2-2V6a2 2 0 012-2h10a2 2 0 012 2v1m2 13a2 2 0 01-2-2V7m2 13a2 2 0 002-2V9a2 2 0 00-2-2h-2m-4-3H9M7 16h6M7 8h6v4H7V8z" />
                </svg>
              </div>
              <p class="empty-title">还没有发布作品</p>
              <p class="empty-desc">记录你的想法，分享你的世界</p>
              <RouterLink to="/editor" class="empty-action">去创作</RouterLink>
            </div>
            <div v-else class="article-grid">
              <div v-for="(article, idx) in publishedArticles" :key="article.id" class="article-grid-item" :style="{ animationDelay: `${idx * 0.05}s` }">
                <ArticleGridCard :article="article" :to="`/user/preview/${article.id}`" />
                <div class="grid-hover-overlay">
                  <div class="hover-overlay-content">
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                    </svg>
                    <span v-if="article.viewCount" class="text-xs">{{ article.viewCount }}</span>
                  </div>
                </div>
              </div>
            </div>
            <div ref="sentinelRef" v-if="hasMore" class="h-px"></div>
            <div v-if="loading && publishedArticles.length > 0" class="loading-more">
              <span class="loading-spinner"></span>
              <span>加载中...</span>
            </div>
          </div>

          <!-- 草稿 -->
          <ErrorRetry v-if="activeTab === 'drafts' && tabError && !draftArticles.length" :message="tabError" :on-retry="retryTabData" />
          <div v-if="activeTab === 'drafts' && !tabError">
            <div v-if="loading && draftArticles.length === 0" class="article-grid">
              <div v-for="i in 6" :key="i" class="skeleton-card" />
            </div>
            <div v-else-if="!loading && draftArticles.length === 0" class="empty-state">
              <div class="empty-illustration">
                <svg class="empty-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                </svg>
              </div>
              <p class="empty-title">草稿箱为空</p>
              <p class="empty-desc">草稿保存30天后将自动清理</p>
              <RouterLink to="/editor" class="empty-action">去创作</RouterLink>
            </div>
            <div v-else class="article-grid">
              <div v-for="(article, idx) in draftArticles" :key="article.id" class="article-grid-item draft-grid-item" :style="{ animationDelay: `${idx * 0.05}s` }">
                <ArticleGridCard :article="article" />
                <div class="draft-hover-overlay">
                  <div class="draft-overlay-buttons">
                    <button class="draft-btn draft-btn-edit" @click.stop="editDraft(article.id)">
                      <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                      </svg>
                      编辑
                    </button>
                    <button class="draft-btn draft-btn-publish" :disabled="publishingId === article.id" @click.stop="publishDraftNow(article)">
                      <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 3l14 9-14 9V3z" />
                      </svg>
                      {{ publishingId === article.id ? '...' : '发布' }}
                    </button>
                    <button class="draft-btn draft-btn-schedule" @click.stop="openScheduleModal(article)">
                      <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                      </svg>
                      定时
                    </button>
                    <button class="draft-btn draft-btn-delete" @click.stop="handleDeleteArticle(article)" title="删除">
                      <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                      </svg>
                    </button>
                  </div>
                  <div v-if="getDraftExpiry(article.createdAt)" class="draft-expiry">{{ getDraftExpiry(article.createdAt) }}</div>
                </div>
              </div>
            </div>
            <div ref="sentinelRef" v-if="hasMore" class="h-px"></div>
            <div v-if="loading && draftArticles.length > 0" class="loading-more">
              <span class="loading-spinner"></span>
              <span>加载中...</span>
            </div>
          </div>

          <!-- 收藏 -->
          <ErrorRetry v-if="activeTab === 'collected' && tabError && !collectedArticles.length" :message="tabError" :on-retry="retryTabData" />
          <div v-if="activeTab === 'collected' && !tabError">
            <div v-if="loading && collectedArticles.length === 0" class="article-grid">
              <div v-for="i in 6" :key="i" class="skeleton-card" />
            </div>
            <div v-else-if="!loading && collectedArticles.length === 0" class="empty-state">
              <div class="empty-illustration">
                <svg class="empty-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M5 5a2 2 0 012-2h10a2 2 0 012 2v16l-7-3.5L5 21V5z" />
                </svg>
              </div>
              <p class="empty-title">暂无收藏</p>
              <p class="empty-desc">收藏你喜欢的作品，方便随时回顾</p>
            </div>
            <div v-else class="article-grid">
              <div v-for="(article, idx) in collectedArticles" :key="article.id" class="article-grid-item" :style="{ animationDelay: `${idx * 0.05}s` }">
                <ArticleGridCard :article="article" />
                <div class="grid-hover-overlay">
                  <div class="hover-overlay-content">
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                    </svg>
                    <span v-if="article.viewCount" class="text-xs">{{ article.viewCount }}</span>
                  </div>
                </div>
              </div>
            </div>
            <div ref="sentinelRef" v-if="hasMore" class="h-px"></div>
            <div v-if="loading && collectedArticles.length > 0" class="loading-more">
              <span class="loading-spinner"></span>
              <span>加载中...</span>
            </div>
          </div>

          <!-- 点赞 -->
          <ErrorRetry v-if="activeTab === 'liked' && tabError && !likedArticles.length" :message="tabError" :on-retry="retryTabData" />
          <div v-if="activeTab === 'liked' && !tabError">
            <div v-if="loading && likedArticles.length === 0" class="article-grid">
              <div v-for="i in 6" :key="i" class="skeleton-card" />
            </div>
            <div v-else-if="!loading && likedArticles.length === 0" class="empty-state">
              <div class="empty-illustration">
                <svg class="empty-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
                </svg>
              </div>
              <p class="empty-title">暂无点赞</p>
              <p class="empty-desc">为你喜欢的作品点亮一颗心</p>
            </div>
            <div v-else class="article-grid">
              <div v-for="(article, idx) in likedArticles" :key="article.id" class="article-grid-item" :style="{ animationDelay: `${idx * 0.05}s` }">
                <ArticleGridCard :article="article" />
                <div class="grid-hover-overlay">
                  <div class="hover-overlay-content">
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                    </svg>
                    <span v-if="article.viewCount" class="text-xs">{{ article.viewCount }}</span>
                  </div>
                </div>
              </div>
            </div>
            <div ref="sentinelRef" v-if="hasMore" class="h-px"></div>
            <div v-if="loading && likedArticles.length > 0" class="loading-more">
              <span class="loading-spinner"></span>
              <span>加载中...</span>
            </div>
          </div>

          <!-- 评论 -->
          <ErrorRetry v-if="activeTab === 'comments' && tabError && !myComments.length" :message="tabError" :on-retry="retryTabData" />
          <div v-if="activeTab === 'comments' && !tabError" class="comments-list">
            <div v-for="comment in myComments" :key="comment.id" class="comment-item">
              <p class="comment-content">{{ comment.content }}</p>
              <div class="comment-footer">
                <span class="comment-date">{{ formatDate(comment.createdAt) }}</span>
                <RouterLink :to="`/articles/${comment.articleId}`" class="comment-link">查看作品</RouterLink>
              </div>
            </div>
            <div v-if="!loading && myComments.length === 0" class="empty-state">
              <div class="empty-illustration">
                <svg class="empty-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
                </svg>
              </div>
              <p class="empty-title">暂无评论</p>
              <p class="empty-desc">参与讨论，留下你的观点</p>
            </div>
          </div>

          <!-- 历史 -->
          <ErrorRetry v-if="activeTab === 'history' && tabError && !historyArticles.length && !showLocalHistory" :message="tabError" :on-retry="retryTabData" />
          <!-- 本地历史回退：服务器不可达时显示本地浏览记录 -->
          <div v-if="activeTab === 'history' && showLocalHistory" class="local-history">
            <div class="local-history-notice">
              <svg class="local-history-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"/>
              </svg>
              <span>显示本地浏览记录，连接恢复后自动同步</span>
            </div>
            <div v-for="entry in localHistoryEntries" :key="entry.articleId" class="local-history-item">
              <RouterLink :to="`/articles/${entry.articleId}`" class="local-history-link">
                <div class="local-history-info">
                  <span class="local-history-title">{{ entry.title }}</span>
                  <span v-if="entry.viewDuration > 0" class="local-history-duration">阅读 {{ Math.ceil(entry.viewDuration / 60) }} 分钟</span>
                </div>
                <span class="local-history-time">{{ formatDate(new Date(entry.visitedAt).toISOString()) }}</span>
              </RouterLink>
            </div>
          </div>
          <div v-if="activeTab === 'history' && !tabError && !showLocalHistory">
            <div v-if="loading && historyArticles.length === 0" class="article-grid">
              <div v-for="i in 6" :key="i" class="skeleton-card" />
            </div>
            <div v-else-if="!loading && historyArticles.length === 0" class="empty-state">
              <div class="empty-illustration">
                <svg class="empty-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
              </div>
              <p class="empty-title">暂无浏览历史</p>
              <p class="empty-desc">你浏览过的作品会出现在这里</p>
            </div>
            <div v-else class="article-grid">
              <div v-for="(article, idx) in historyArticles" :key="article.id" class="article-grid-item" :style="{ animationDelay: `${idx * 0.05}s` }">
                <ArticleGridCard :article="article" />
                <div class="grid-hover-overlay">
                  <div class="hover-overlay-content">
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                    </svg>
                    <span v-if="article.viewCount" class="text-xs">{{ article.viewCount }}</span>
                  </div>
                </div>
              </div>
            </div>
            <div ref="sentinelRef" v-if="hasMore" class="h-px"></div>
            <div v-if="loading && historyArticles.length > 0" class="loading-more">
              <span class="loading-spinner"></span>
              <span>加载中...</span>
            </div>
          </div>
        </div>
      </Transition>
    </div>

    <!-- 定时发布弹窗 -->
    <Teleport to="body">
      <Transition name="modal-fade">
        <div v-if="showScheduleModal" class="modal-overlay" @click.self="showScheduleModal = false">
          <div class="modal-card">
            <div class="modal-header">
              <div class="modal-icon-wrap modal-icon-schedule">
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
              </div>
              <h3 class="modal-title">定时发布</h3>
            </div>
            <p class="modal-desc">作品「{{ scheduleTarget?.title || '无标题' }}」将在指定时间自动发布</p>
            <div class="modal-field">
              <label class="modal-label">选择发布时间</label>
              <input
                v-model="scheduleTime"
                type="datetime-local"
                class="modal-input"
                :min="minScheduleTime"
              />
            </div>
            <div class="modal-actions">
              <button class="modal-btn modal-btn-cancel" @click="showScheduleModal = false">取消</button>
              <button class="modal-btn modal-btn-primary" :disabled="!scheduleTime || schedulingId !== null" @click="confirmSchedulePublish">
                {{ schedulingId !== null ? '设置中...' : '确认' }}
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- 删除确认弹窗 -->
    <Teleport to="body">
      <Transition name="modal-fade">
        <div v-if="showDeleteModal" class="modal-overlay" @click.self="showDeleteModal = false">
          <div class="modal-card">
            <div class="modal-header">
              <div class="modal-icon-wrap modal-icon-danger">
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4.5c-.77-.833-2.694-.833-3.464 0L3.34 16.5c-.77.833.192 2.5 1.732 2.5z" />
                </svg>
              </div>
              <h3 class="modal-title">确认删除</h3>
            </div>
            <p class="modal-desc">确定要删除作品「{{ deleteTarget?.title || '无标题' }}」吗？此操作不可撤销。</p>
            <div class="modal-actions">
              <button class="modal-btn modal-btn-cancel" @click="showDeleteModal = false">取消</button>
              <button class="modal-btn modal-btn-danger" :disabled="deletingId !== null" @click="confirmDelete">
                {{ deletingId !== null ? '删除中...' : '确认删除' }}
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
/** 个人中心页 */
import type { Article, Comment } from '@/types'
import { userApi, articleApi } from '@/api'
import { showToast } from '@/composables/useToast'

const userStore = useUserStore()
const router = useRouter()
const { put: apiPut } = useApi()
const { isMobile } = useBreakpoints()

const avatarInput = ref<HTMLInputElement | null>(null)
const avatarUploading = ref(false)
const avatarProgress = ref(0)
let avatarAbortController: AbortController | null = null

// 允许的图片 MIME 类型
const ALLOWED_IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/gif', 'image/webp', 'image/bmp']

// 触发头像上传
const triggerAvatarUpload = () => {
  avatarInput.value?.click()
}

// 处理头像变更
const handleAvatarChange = async (e: Event) => {
  const file = (e.target as HTMLInputElement).files?.[0]
  if (!file) return

  // 文件类型校验
  if (!ALLOWED_IMAGE_TYPES.includes(file.type)) {
    const ext = file.name.split('.').pop()?.toLowerCase() || '未知'
    showToast(`不支持 ${ext} 格式，请选择 JPG、PNG、GIF、WebP、BMP 格式的图片`, 'error')
    resetAvatarInput()
    return
  }

  // 文件大小校验
  if (file.size > 5 * 1024 * 1024) {
    const sizeMB = (file.size / (1024 * 1024)).toFixed(1)
    showToast(`图片大小 ${sizeMB}MB 超过 5MB 限制，请压缩后重新上传`, 'error')
    resetAvatarInput()
    return
  }

  // 文件名安全校验
  if (!file.name || file.name.length > 255) {
    showToast('文件名不合法，请重命名后重新上传', 'error')
    resetAvatarInput()
    return
  }

  avatarUploading.value = true
  avatarProgress.value = 0
  avatarAbortController = new AbortController()

  try {
    const formData = new FormData()
    formData.append('file', file)
    const { upload } = useApi()

    const uploadRes = await upload<any>('/files/upload/image', formData, (percent) => {
      avatarProgress.value = percent
    })

    const avatarUrl = uploadRes.data?.data
    if (!avatarUrl) {
      throw new Error('服务器未返回头像地址')
    }

    await apiPut<any>('/user/profile', { avatar: avatarUrl })
    userStore.updateProfile({ avatar: avatarUrl })
    showToast('头像更新成功')
  } catch (err: any) {
    // 用户主动取消，不显示错误
    if (err?.code === 'ERR_CANCELED' || err?.name === 'CanceledError' || err?.message === 'canceled') {
      return
    }
    // 网络错误
    if (!err?.response) {
      showToast('网络连接失败，请检查网络后重试', 'error')
    } else {
      const msg = err?.response?.data?.message || '头像上传失败'
      showToast(`${msg}，请重试`, 'error')
    }
  } finally {
    avatarUploading.value = false
    avatarAbortController = null
    resetAvatarInput()
  }
}

// 重置文件输入
const resetAvatarInput = () => {
  if (avatarInput.value) {
    avatarInput.value.value = ''
  }
}

const tabs = [
  { key: 'published', label: '作品' },
  { key: 'drafts', label: '草稿' },
  { key: 'collected', label: '收藏' },
  { key: 'liked', label: '点赞' },
  { key: 'comments', label: '评论' },
  { key: 'history', label: '历史' },
]

const activeTab = ref('published')
const loading = ref(false)
const hasMore = ref(true)
const currentPage = ref(1)
const pageSize = 10
const tabError = ref<string | null>(null)
const sentinelRef = ref<HTMLElement | null>(null)

const { cachedRequest } = useRequestCache({ ttl: 5 * 60 * 1000 })

const publishedArticles = ref<Article[]>([])
const draftArticles = ref<Article[]>([])
const collectedArticles = ref<Article[]>([])
const likedArticles = ref<Article[]>([])
const myComments = ref<Comment[]>([])
const historyArticles = ref<Article[]>([])
const localHistoryEntries = ref<Array<{ articleId: number; title: string; visitedAt: number; viewDuration: number }>>([])
const showLocalHistory = ref(false)

// 发布中状态
const publishingId = ref<number | null>(null)
const schedulingId = ref<number | null>(null)

// 定时发布弹窗
const showScheduleModal = ref(false)
const scheduleTarget = ref<Article | null>(null)
const scheduleTime = ref('')

// 删除确认弹窗
const showDeleteModal = ref(false)
const deleteTarget = ref<Article | null>(null)
const deletingId = ref<number | null>(null)

// 最小可选时间
const minScheduleTime = computed(() => {
  const now = new Date()
  const offset = now.getTimezoneOffset()
  const local = new Date(now.getTime() - offset * 60000)
  return local.toISOString().slice(0, 16)
})

// 切换Tab
const switchTab = (key: string) => {
  if (activeTab.value === key) return
  activeTab.value = key
  hasMore.value = true
  currentPage.value = 1
  tabError.value = null
  showLocalHistory.value = false
  localHistoryEntries.value = []
  loadTabData()
}

// 重试加载Tab数据
const retryTabData = () => {
  tabError.value = null
  loadTabData()
}

// 加载Tab数据
const loadTabData = async () => {
  loading.value = true
  tabError.value = null
  try {
    const params = { page: currentPage.value, pageSize }
    let list: any[] = []
    let total = 0
    switch (activeTab.value) {
      case 'published': {
        const { data } = await userApi.getMyArticles(params)
        list = data?.data?.list || []
        total = data?.data?.total || list.length
        publishedArticles.value = list
        hasMore.value = list.length < total
        break
      }
      case 'drafts': {
        const { data } = await userApi.getMyDrafts(params)
        list = data?.data?.list || []
        total = data?.data?.total || list.length
        draftArticles.value = list
        hasMore.value = list.length < total
        break
      }
      case 'collected': {
        const { data } = await userApi.getMyCollections(params)
        list = data?.data?.list || []
        total = data?.data?.total || list.length
        collectedArticles.value = list
        hasMore.value = list.length < total
        break
      }
      case 'liked': {
        const { data } = await userApi.getMyLikes(params)
        list = data?.data?.list || []
        total = data?.data?.total || list.length
        likedArticles.value = list
        hasMore.value = list.length < total
        break
      }
      case 'comments': {
        const { data } = await userApi.getMyComments(params)
        list = data?.data?.list || []
        total = data?.data?.total || list.length
        myComments.value = list
        hasMore.value = list.length < total
        break
      }
      case 'history': {
        // 先同步本地浏览记录到服务器，确保历史列表数据完整
        const { syncToServer, getLocalHistory } = useViewHistory()
        await syncToServer()
        try {
          const { data } = await userApi.getViewHistory(params)
          list = data?.data?.list || []
          total = data?.data?.total || list.length
          historyArticles.value = list
          hasMore.value = list.length < total
          showLocalHistory.value = false
          localHistoryEntries.value = []
        } catch {
          // 服务器不可达时，回退到本地浏览记录
          const localEntries = getLocalHistory()
          if (localEntries.length > 0) {
            localHistoryEntries.value = localEntries
              .sort((a, b) => b.visitedAt - a.visitedAt)
              .slice(0, 50)
            showLocalHistory.value = true
            tabError.value = null
          } else {
            tabError.value = '数据加载失败，请稍后重试'
          }
        }
        break
      }
    }
  } catch {
    tabError.value = '数据加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

// 加载更多
const loadMore = async () => {
  if (loading.value || !hasMore.value) return
  currentPage.value++
  loading.value = true
  try {
    const params = { page: currentPage.value, pageSize }
    let list: any[] = []
    let total = 0
    switch (activeTab.value) {
      case 'published': {
        const { data } = await userApi.getMyArticles(params)
        list = data?.data?.list || []
        total = data?.data?.total || list.length
        publishedArticles.value.push(...list)
        hasMore.value = publishedArticles.value.length < total
        break
      }
      case 'drafts': {
        const { data } = await userApi.getMyDrafts(params)
        list = data?.data?.list || []
        total = data?.data?.total || list.length
        draftArticles.value.push(...list)
        hasMore.value = draftArticles.value.length < total
        break
      }
      case 'collected': {
        const { data } = await userApi.getMyCollections(params)
        list = data?.data?.list || []
        total = data?.data?.total || list.length
        collectedArticles.value.push(...list)
        hasMore.value = collectedArticles.value.length < total
        break
      }
      case 'liked': {
        const { data } = await userApi.getMyLikes(params)
        list = data?.data?.list || []
        total = data?.data?.total || list.length
        likedArticles.value.push(...list)
        hasMore.value = likedArticles.value.length < total
        break
      }
      case 'comments': {
        const { data } = await userApi.getMyComments(params)
        list = data?.data?.list || []
        total = data?.data?.total || list.length
        myComments.value.push(...list)
        hasMore.value = myComments.value.length < total
        break
      }
      case 'history': {
        const { syncToServer } = useViewHistory()
        await syncToServer()
        const { data } = await userApi.getViewHistory(params)
        list = data?.data?.list || []
        total = data?.data?.total || list.length
        historyArticles.value.push(...list)
        hasMore.value = historyArticles.value.length < total
        break
      }
    }
  } catch {
    currentPage.value--
    tabError.value = '加载更多失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

// 发布草稿（立即）
const publishDraftNow = async (article: Article) => {
  publishingId.value = article.id
  try {
    await articleApi.publishDraft(article.id)
    showToast('作品已发布')
    // 从草稿列表移除
    draftArticles.value = draftArticles.value.filter(a => a.id !== article.id)
    // 如果当前在作品Tab，刷新作品列表
    if (activeTab.value === 'published') {
      currentPage.value = 1
      await loadTabData()
    }
  } catch (err: any) {
    showToast(err?.response?.data?.message || '发布失败，请稍后重试', 'error')
  } finally {
    publishingId.value = null
  }
}

// 打开定时发布弹窗
const openScheduleModal = (article: Article) => {
  scheduleTarget.value = article
  scheduleTime.value = ''
  showScheduleModal.value = true
}

// 确认定时发布
const confirmSchedulePublish = async () => {
  if (!scheduleTarget.value || !scheduleTime.value) return
  schedulingId.value = scheduleTarget.value.id
  try {
    const publishAt = new Date(scheduleTime.value).toISOString()
    await articleApi.publishDraft(scheduleTarget.value.id, publishAt)
    showToast('已设置为定时发布')
    draftArticles.value = draftArticles.value.filter(a => a.id !== scheduleTarget.value!.id)
    showScheduleModal.value = false
    scheduleTarget.value = null
  } catch (err: any) {
    showToast(err?.response?.data?.message || '操作失败，请稍后重试', 'error')
  } finally {
    schedulingId.value = null
  }
}

// 编辑草稿
const editDraft = (articleId: number) => {
  router.push(`/editor?id=${articleId}`)
}

// 删除确认
const handleDeleteArticle = (article: Article) => {
  deleteTarget.value = article
  showDeleteModal.value = true
}

// 确认删除
const confirmDelete = async () => {
  if (!deleteTarget.value) return
  deletingId.value = deleteTarget.value.id
  try {
    await articleApi.deleteArticle(deleteTarget.value.id)
    showToast('作品已删除')
    const id = deleteTarget.value.id
    publishedArticles.value = publishedArticles.value.filter(a => a.id !== id)
    draftArticles.value = draftArticles.value.filter(a => a.id !== id)
    showDeleteModal.value = false
    deleteTarget.value = null
  } catch (err: any) {
    showToast(err?.response?.data?.message || '删除失败，请稍后重试', 'error')
  } finally {
    deletingId.value = null
  }
}

// 计算草稿剩余天数
const getDraftExpiry = (createdAt: string) => {
  const created = new Date(createdAt)
  const expiry = new Date(created.getTime() + 30 * 24 * 60 * 60 * 1000)
  const now = new Date()
  const daysLeft = Math.ceil((expiry.getTime() - now.getTime()) / (24 * 60 * 60 * 1000))
  if (daysLeft <= 0) return '即将清理'
  if (daysLeft <= 7) return `${daysLeft}天后清理`
  return ''
}



// 格式化日期
const formatDate = (date: string) => {
  return new Date(date).toLocaleDateString('zh-CN')
}

// 页面加载时获取默认Tab数据
let observer: IntersectionObserver | null = null

// 设置 IntersectionObserver 监听哨兵元素，实现滑动加载更多
const setupObserver = () => {
  if (observer) {
    observer.disconnect()
    observer = null
  }
  nextTick(() => {
    if (!sentinelRef.value) return
    observer = new IntersectionObserver((entries) => {
      if (entries[0]?.isIntersecting && hasMore.value && !loading.value && !tabError.value) {
        loadMore()
      }
    }, { rootMargin: '200px' })
    observer.observe(sentinelRef.value)
  })
}

// 监听 sentinelRef 变化（切换 Tab 时重新绑定）
watch(sentinelRef, () => {
  setupObserver()
})

onMounted(async () => {
  loadTabData()
  // 获取完整个人资料（含统计数字）
  try {
    const { data: profileData } = await userApi.getProfile()
    const profile = profileData?.data
    if (profile) {
      userStore.updateProfile(profile)
    }
  } catch {
    // 静默失败，不影响主流程
  }
  // 自动更新IP属地
  try {
    const { userApi } = await import('~/api')
    const res = await userApi.updateIpLocation()
    const ipLocation = res.data?.data
    if (ipLocation && userStore.userInfo) {
      userStore.updateProfile({ ipLocation })
    }
  } catch {
    // 静默失败，不影响主流程
  }
  // 初始化无限滚动监听
  setupObserver()
})

onUnmounted(() => {
  if (observer) {
    observer.disconnect()
    observer = null
  }
})

// 页面元信息
useHead({
  title: () => '个人中心' + ' - 知讯',
})

// ========== 模板辅助方法（仅用于UI展示，不影响业务逻辑） ==========

// Tab图标SVG
const getTabIcon = (key: string): string => {
  const icons: Record<string, string> = {
    published: '<svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10"/></svg>',
    drafts: '<svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/></svg>',
    collected: '<svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 5a2 2 0 012-2h10a2 2 0 012 2v16l-7-3.5L5 21V5z"/></svg>',
    liked: '<svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z"/></svg>',
    comments: '<svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z"/></svg>',
    history: '<svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>',
  }
  return icons[key] || ''
}


</script>

<style scoped>
/* ========== 用户页面容器 ========== */
.user-page {
  max-width: 1200px;
  margin: 0 auto;
  min-height: 100vh;
}

@media (min-width: 1536px) {
  .user-page {
    max-width: 1400px;
  }
}

/* ========== 个人资料横幅 ========== */
.profile-banner {
  position: relative;
  margin: 0.75rem 0.5rem 0;
  border-radius: 1rem;
  overflow: hidden;
  background: #fff;
  border: 1px solid var(--zh-border-light);
  box-shadow: var(--zh-shadow-sm);
}

@media (min-width: 768px) {
  .profile-banner {
    margin: 1rem 0.75rem 0;
    border-radius: 1.25rem;
  }
}

/* 顶部装饰条已移除 */

.banner-inner {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 1rem 0.5rem 0.75rem;
  z-index: 1;
}

@media (min-width: 768px) {
  .banner-inner {
    padding: 1.25rem 0.75rem 1rem;
  }
}

/* ========== 头像区域 ========== */
.profile-avatar-wrap {
  position: relative;
  margin-bottom: 0.375rem;
}

/* ========== 昵称 ========== */
.profile-nickname {
  font-size: 1rem;
  font-weight: 700;
  color: var(--zh-text);
  margin-bottom: 0.25rem;
}

@media (min-width: 768px) {
  .profile-nickname {
    font-size: 1.125rem;
  }
}

/* ========== 元信息行 ========== */
.profile-meta-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  justify-content: center;
  gap: 0;
  font-size: 0.6875rem;
  color: var(--zh-text-tertiary);
  margin-bottom: 0.5rem;
}

/* ========== 简介 ========== */
.profile-bio {
  font-size: 0.6875rem;
  color: var(--zh-text-secondary);
  text-align: center;
  max-width: 280px;
  line-height: 1.5;
  margin-bottom: 0.75rem;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* ========== 统计数据行 ========== */
.stats-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0;
  margin-bottom: 0;
  background: var(--zh-bg-hover);
  border-radius: 0.625rem;
  padding: 0.375rem 0.25rem;
  width: auto;
  max-width: none;
  flex: 1;
}

@media (min-width: 768px) {
  .stats-row {
    gap: 0;
    padding: 0.375rem 0.375rem;
    max-width: none;
  }
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex: 1;
  padding: 0 0.125rem;
  animation: statFadeIn 0.5s ease-out both;
}

.stat-item:nth-child(1) { animation-delay: 0s; }
.stat-item:nth-child(3) { animation-delay: 0.1s; }
.stat-item:nth-child(5) { animation-delay: 0.2s; }
.stat-item:nth-child(7) { animation-delay: 0.3s; }

.stat-link {
  cursor: pointer;
  transition: transform 0.2s ease;
}

.stat-link:hover {
  transform: scale(1.05);
}

.stat-number {
  font-size: 1rem;
  font-weight: 700;
  color: var(--zh-text);
  line-height: 1.2;
  transition: color 0.3s ease, transform 0.3s ease;
}

.stat-item:hover .stat-number {
  color: var(--zh-primary);
  transform: scale(1.08);
}

@media (min-width: 768px) {
  .stat-number {
    font-size: 1.125rem;
  }
}

.stat-label {
  font-size: 0.5625rem;
  color: var(--zh-text-tertiary);
  margin-top: 0.0625rem;
}

.stat-divider {
  width: 1px;
  height: 20px;
  background: var(--zh-border);
  flex-shrink: 0;
}

@keyframes statFadeIn {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* ========== 操作按钮行 ========== */
.profile-actions {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  width: 100%;
  max-width: 360px;
  margin: 0 auto;
}

@media (min-width: 768px) {
  .profile-actions {
    max-width: 420px;
  }
}

.btn-edit-profile {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.25rem;
  padding: 0.375rem 0.875rem;
  font-size: 0.75rem;
  font-weight: 500;
  color: var(--zh-primary);
  background: var(--zh-primary-bg);
  border: 1px solid var(--zh-primary);
  border-radius: 9999px;
  transition: all 0.25s ease;
  text-decoration: none;
  white-space: nowrap;
  flex-shrink: 0;
}

.btn-edit-profile:hover {
  background: var(--zh-primary);
  color: #fff;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.25);
}

/* ========== Tab 栏 ========== */
.tab-bar-wrapper {
  position: sticky;
  top: 0;
  z-index: 20;
  background: var(--zh-bg);
  margin: 0 0.5rem;
}

@media (min-width: 768px) {
  .tab-bar-wrapper {
    margin: 0 0.75rem;
  }
}

.tab-bar {
  position: relative;
  display: flex;
  align-items: center;
  gap: 0.125rem;
  background: var(--zh-bg-hover);
  border-radius: 0.75rem;
  padding: 0.25rem;
  overflow: hidden;
  -ms-overflow-style: none;
  scrollbar-width: none;
}

.tab-bar::-webkit-scrollbar {
  display: none;
}

.tab-btn {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.25rem;
  padding: 0.375rem 0.25rem;
  font-size: 0.75rem;
  font-weight: 500;
  color: var(--zh-text-tertiary);
  background: transparent;
  border: none;
  border-radius: 0.5rem;
  white-space: nowrap;
  cursor: pointer;
  transition: color 0.3s ease, background 0.3s ease;
  z-index: 1;
  flex: 1;
  min-width: 0;
}

.tab-btn:hover {
  color: var(--zh-text);
}

@media (min-width: 768px) {
  .tab-btn {
    padding: 0.5rem 0.5rem;
    font-size: 0.8125rem;
    gap: 0.25rem;
  }
}

.tab-icon {
  display: flex;
  align-items: center;
  flex-shrink: 0;
}

.tab-active {
  color: var(--zh-primary);
}

.tab-label {
  display: inline;
}

/* ========== Tab 内容过渡 ========== */

.tab-content-inner {
  min-height: 200px;
}

.tab-fade-enter-active {
  transition: opacity 0.25s ease, transform 0.25s ease;
}

.tab-fade-leave-active {
  transition: opacity 0.15s ease, transform 0.15s ease;
}

.tab-fade-enter-from {
  opacity: 0;
  transform: translateY(8px);
}

.tab-fade-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

/* ========== 文章网格 ========== */
.article-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0;
}

@media (min-width: 768px) {
  .article-grid {
    gap: 0;
  }
}

/* ========== 网格项 - 交错入场动画 ========== */
.article-grid-item {
  position: relative;
  border-radius: 0;
  overflow: hidden;
  animation: gridItemIn 0.4s ease-out both;
  box-shadow: var(--zh-shadow-sm);
}

.article-grid-item:hover :deep(img) {
  transform: scale(1.08);
  transition: transform 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.article-grid-item :deep(img) {
  transition: transform 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

@keyframes gridItemIn {
  from {
    opacity: 0;
    transform: translateY(16px) scale(0.96);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

/* ========== 网格悬浮覆盖层 ========== */
.grid-hover-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(to top, rgba(0, 0, 0, 0.55) 0%, rgba(0, 0, 0, 0.1) 50%, transparent 100%);
  opacity: 0;
  transition: opacity 0.3s ease;
  display: flex;
  align-items: flex-end;
  justify-content: flex-end;
  padding: 0.5rem;
  border-radius: 0.5rem;
  pointer-events: none;
}

.article-grid-item:hover .grid-hover-overlay {
  opacity: 1;
}

.hover-overlay-content {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  color: rgba(255, 255, 255, 0.9);
  font-weight: 500;
}

/* ========== 骨架屏 ========== */
.skeleton-card {
  aspect-ratio: 3 / 4;
  background: var(--zh-bg-hover);
  border-radius: 0.5rem;
  animation: skeletonPulse 1.5s ease-in-out infinite;
}

@keyframes skeletonPulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
}

/* ========== 草稿悬浮覆盖层 ========== */
.draft-hover-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.35);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  opacity: 0;
  transition: opacity 0.3s ease;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  padding: 0.5rem;
  border-radius: 0.5rem;
}

.draft-grid-item:hover .draft-hover-overlay {
  opacity: 1;
}

.draft-overlay-buttons {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  flex-wrap: wrap;
}

.draft-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.2rem;
  padding: 0.3rem 0.5rem;
  font-size: 0.65rem;
  font-weight: 500;
  border-radius: 9999px;
  border: none;
  cursor: pointer;
  transition: all 0.2s ease;
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
}

.draft-btn-edit {
  background: rgba(255, 255, 255, 0.9);
  color: var(--zh-text-secondary);
}

.draft-btn-edit:hover {
  background: #fff;
  color: var(--zh-primary);
}

.draft-btn-publish {
  background: rgba(79, 70, 229, 0.9);
  color: #fff;
}

.draft-btn-publish:hover {
  background: var(--zh-primary);
}

.draft-btn-publish:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.draft-btn-schedule {
  background: rgba(245, 158, 11, 0.9);
  color: #fff;
}

.draft-btn-schedule:hover {
  background: var(--zh-warning);
}

.draft-btn-delete {
  background: rgba(255, 255, 255, 0.9);
  color: var(--zh-text-secondary);
  padding: 0.3rem;
}

.draft-btn-delete:hover {
  background: #fff;
  color: #ef4444;
}

.draft-expiry {
  font-size: 0.6rem;
  color: rgba(255, 255, 255, 0.55);
  margin-top: 0.25rem;
}

/* ========== 空状态 ========== */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 3rem 1rem;
  text-align: center;
}

.empty-illustration {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: var(--zh-bg-hover);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 1rem;
}

.empty-icon {
  width: 28px;
  height: 28px;
  color: var(--zh-text-tertiary);
}

.empty-title {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--zh-text);
  margin-bottom: 0.25rem;
}

.empty-desc {
  font-size: 0.75rem;
  color: var(--zh-text-tertiary);
  margin-bottom: 1rem;
}

.empty-action {
  display: inline-flex;
  align-items: center;
  padding: 0.5rem 1.5rem;
  font-size: 0.8125rem;
  font-weight: 500;
  color: #fff;
  background: var(--zh-primary-gradient);
  border-radius: 9999px;
  text-decoration: none;
  transition: all 0.25s ease;
  box-shadow: 0 2px 8px rgba(79, 70, 229, 0.3);
}

.empty-action:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 16px rgba(79, 70, 229, 0.4);
}

/* ========== 加载更多 ========== */
.loading-more {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 0.75rem 0;
  font-size: 0.8125rem;
  color: var(--zh-text-tertiary);
}

.loading-spinner {
  width: 14px;
  height: 14px;
  border: 2px solid var(--zh-border);
  border-top-color: var(--zh-primary);
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

/* ========== 评论列表 ========== */
.comments-list {
  padding: 0 0.5rem;
}

.comment-item {
  padding: 0.75rem 0;
  border-bottom: 1px solid var(--zh-border-light);
}

.comment-content {
  font-size: 0.8125rem;
  color: var(--zh-text-secondary);
  line-height: 1.5;
}

.comment-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 0.5rem;
}

.comment-date {
  font-size: 0.6875rem;
  color: var(--zh-text-tertiary);
}

.comment-link {
  font-size: 0.6875rem;
  color: var(--zh-primary);
  text-decoration: none;
  transition: color 0.2s;
}

.comment-link:hover {
  color: var(--zh-primary-dark);
}

/* ========== 弹窗 ========== */
.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 50;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(4px);
}

.modal-card {
  background: var(--zh-bg-elevated);
  border-radius: 1rem;
  box-shadow: var(--zh-shadow-md), 0 0 0 1px rgba(0, 0, 0, 0.05);
  padding: 1.5rem;
  width: 100%;
  max-width: 28rem;
  margin: 0 1rem;
  animation: modalIn 0.3s ease-out;
}

@keyframes modalIn {
  from {
    opacity: 0;
    transform: scale(0.95) translateY(8px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

.modal-header {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 1rem;
}

.modal-icon-wrap {
  width: 40px;
  height: 40px;
  border-radius: 0.75rem;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.modal-icon-schedule {
  background: rgba(79, 70, 229, 0.1);
  color: var(--zh-primary);
}

.modal-icon-danger {
  background: rgba(239, 68, 68, 0.1);
  color: #ef4444;
}

.modal-title {
  font-size: 1.125rem;
  font-weight: 700;
  color: var(--zh-text);
}

.modal-desc {
  font-size: 0.8125rem;
  color: var(--zh-text-secondary);
  margin-bottom: 1rem;
  line-height: 1.5;
}

.modal-field {
  margin-bottom: 1.25rem;
}

.modal-label {
  display: block;
  font-size: 0.8125rem;
  color: var(--zh-text-secondary);
  margin-bottom: 0.375rem;
  font-weight: 500;
}

.modal-input {
  width: 100%;
  border: 1px solid var(--zh-border);
  border-radius: 0.75rem;
  padding: 0.625rem 0.75rem;
  font-size: 0.875rem;
  color: var(--zh-text);
  background: var(--zh-bg);
  transition: all 0.2s ease;
  outline: none;
}

.modal-input:focus {
  border-color: var(--zh-primary);
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.1);
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
}

.modal-btn {
  padding: 0.5rem 1.25rem;
  font-size: 0.8125rem;
  font-weight: 500;
  border-radius: 0.75rem;
  border: none;
  cursor: pointer;
  transition: all 0.2s ease;
}

.modal-btn-cancel {
  background: var(--zh-bg-hover);
  color: var(--zh-text-secondary);
}

.modal-btn-cancel:hover {
  background: var(--zh-border);
}

.modal-btn-primary {
  background: var(--zh-primary-gradient);
  color: #fff;
  box-shadow: 0 2px 8px rgba(79, 70, 229, 0.3);
}

.modal-btn-primary:hover {
  box-shadow: 0 4px 16px rgba(79, 70, 229, 0.4);
  transform: translateY(-1px);
}

.modal-btn-primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
}

.modal-btn-danger {
  background: #ef4444;
  color: #fff;
  box-shadow: 0 2px 8px rgba(239, 68, 68, 0.3);
}

.modal-btn-danger:hover {
  background: #dc2626;
  box-shadow: 0 4px 16px rgba(239, 68, 68, 0.4);
  transform: translateY(-1px);
}

.modal-btn-danger:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
}

/* 弹窗过渡动画 */
.modal-fade-enter-active {
  transition: opacity 0.25s ease;
}

.modal-fade-leave-active {
  transition: opacity 0.15s ease;
}

.modal-fade-enter-from,
.modal-fade-leave-to {
  opacity: 0;
}

/* ========== 本地历史回退样式 ========== */
.local-history {
  padding: 0 0.5rem;
}

.local-history-notice {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1rem;
  margin-bottom: 0.5rem;
  background: var(--zh-primary-bg, #f0edff);
  border-radius: 0.75rem;
  font-size: 0.8125rem;
  color: var(--zh-primary, #4f46e5);
}

.local-history-icon {
  width: 1.25rem;
  height: 1.25rem;
  flex-shrink: 0;
}

.local-history-item {
  border-bottom: 1px solid var(--zh-border-light, #f0f0f0);
}

.local-history-link {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.75rem 0;
  text-decoration: none;
  transition: background 0.15s;
}

.local-history-link:hover {
  background: var(--zh-bg-hover, #fafafa);
}

.local-history-info {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  min-width: 0;
}

.local-history-title {
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--zh-text, #1a1a1a);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.local-history-duration {
  font-size: 0.75rem;
  color: var(--zh-text-tertiary, #999);
}

.local-history-time {
  font-size: 0.75rem;
  color: var(--zh-text-tertiary, #999);
  flex-shrink: 0;
  margin-left: 1rem;
}


</style>