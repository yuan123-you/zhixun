<template>
  <div class="max-w-[840px] mx-auto px-3 py-3 md:px-5 md:py-4">
    <!-- ==================== 顶部区域：标题 + 创建按钮 ==================== -->
    <div class="flex items-center justify-between mb-3">
      <div class="flex items-center gap-3">
        <button
          class="flex items-center justify-center w-8 h-8 rounded-lg text-[var(--zh-text-secondary)] hover:bg-[var(--zh-bg-secondary)] transition-colors"
          @click="router.back()"
          title="返回"
        >
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
          </svg>
        </button>
        <div>
          <h1 class="text-lg md:text-xl font-bold text-[var(--zh-text)] tracking-tight leading-tight">群组广场</h1>
          <p class="text-xs text-[var(--zh-text-tertiary)] mt-0.5">发现感兴趣的群组，与志同道合的人交流</p>
        </div>
      </div>
      <button
        v-if="userStore.isLoggedIn"
        class="flex items-center gap-1.5 px-3 py-1.5 bg-primary text-white rounded-lg text-xs font-semibold hover:bg-primary-600 active:scale-95 transition-all duration-200 shadow-[var(--shadow-sm)] hover:shadow-[var(--shadow-md)]"
        @click="showCreateModal = true"
      >
        <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
        </svg>
        创建群组
      </button>
    </div>

    <!-- ==================== Tab 切换 ==================== -->
    <div class="flex items-center gap-1 border-b border-[var(--zh-border)] dark:border-gray-700/60 mb-5">
      <button
        class="relative px-1 pb-3 text-sm font-semibold transition-colors duration-200"
        :class="activeTab === 'my' ? 'text-primary' : 'text-[var(--zh-text-tertiary)] hover:text-[var(--zh-text-secondary)] dark:hover:text-gray-300'"
        @click="switchTab('my')"
      >
        <span class="flex items-center gap-1.5">
          我的群组
          <span
            v-if="myGroups.length && activeTab !== 'my'"
            class="inline-flex items-center justify-center min-w-[18px] h-[18px] px-1 rounded-full bg-primary/10 text-primary text-[10px] font-bold"
          >{{ myGroups.length }}</span>
        </span>
        <div
          class="absolute bottom-0 left-1/2 -translate-x-1/2 w-full h-0.5 rounded-full bg-primary transition-transform duration-300 origin-center"
          :class="activeTab === 'my' ? 'scale-x-100' : 'scale-x-0'"
        ></div>
      </button>
      <span class="pb-3 text-[var(--zh-text-tertiary)] dark:text-gray-600 text-sm font-medium px-2 select-none">·</span>
      <button
        class="relative px-1 pb-3 text-sm font-semibold transition-colors duration-200"
        :class="activeTab === 'search' ? 'text-primary' : 'text-[var(--zh-text-tertiary)] hover:text-[var(--zh-text-secondary)] dark:hover:text-gray-300'"
        @click="switchTab('search')"
      >
        发现群组
        <div
          class="absolute bottom-0 left-1/2 -translate-x-1/2 w-full h-0.5 rounded-full bg-primary transition-transform duration-300 origin-center"
          :class="activeTab === 'search' ? 'scale-x-100' : 'scale-x-0'"
        ></div>
      </button>
    </div>

    <!-- ==================== 我的群组 Tab ==================== -->
    <Transition name="tab-fade" mode="out-in">
      <div v-if="activeTab === 'my'" key="my">
        <!-- 未登录 -->
        <div v-if="!userStore.isLoggedIn" class="empty-state">
          <div class="empty-state-icon">
            <div class="empty-state-icon-bg bg-gradient-to-br from-slate-100 to-slate-200 dark:from-gray-800 dark:to-gray-750">
              <svg class="w-11 h-11 text-slate-400 dark:text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
              </svg>
            </div>
          </div>
          <h3 class="empty-state-title">登录后查看我的群组</h3>
          <p class="empty-state-desc">加入群组，与社区成员互动交流</p>
          <RouterLink to="/login" class="empty-state-action">立即登录</RouterLink>
        </div>

        <div v-else>
          <!-- 加载骨架屏 -->
          <div v-if="myGroupsLoading" class="space-y-2.5">
            <div v-for="i in 5" :key="i" class="card animate-pulse overflow-hidden">
              <div class="flex items-center gap-3.5 p-4">
                <div class="w-12 h-12 bg-[var(--zh-border)] dark:bg-gray-700 rounded-xl shrink-0"></div>
                <div class="flex-1 space-y-2.5">
                  <div class="h-4 bg-[var(--zh-border)] dark:bg-gray-700 rounded w-40"></div>
                  <div class="flex items-center gap-3">
                    <div class="h-3 bg-[var(--zh-border)] dark:bg-gray-700 rounded w-16"></div>
                    <div class="h-3 bg-[var(--zh-border)] dark:bg-gray-700 rounded w-12"></div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 空状态：没有加入群组 -->
          <div v-else-if="myGroups.length === 0" class="empty-state">
            <div class="empty-state-icon">
              <div class="empty-state-icon-bg bg-gradient-to-br from-primary/5 to-primary/10 dark:from-primary/10 dark:to-primary/15">
                <svg class="w-11 h-11 text-primary/35 dark:text-primary/40" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M18.364 18.364A9 9 0 005.636 5.636m12.728 12.728A9 9 0 015.636 5.636m12.728 12.728L5.636 5.636" />
                </svg>
              </div>
            </div>
            <h3 class="empty-state-title">还没有加入任何群组</h3>
            <p class="empty-state-desc">探索更多群组，找到你的兴趣圈子</p>
            <button class="empty-state-action" @click="switchTab('search')">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
              </svg>
              发现群组
            </button>
          </div>

          <!-- 我的群组列表 -->
          <div v-else class="space-y-2.5">
            <div
              v-for="group in myGroups"
              :key="group.id"
              class="group-card"
              @click="navigateTo(`/groups/${group.id}`)"
            >
              <div class="group-card-inner">
                <!-- 头像 -->
                <div class="relative shrink-0">
                  <img
                    v-if="resolveUrl(group.avatar)"
                    :src="resolveUrl(group.avatar) as string"
                    :alt="group.name"
                    class="group-card-avatar"
                    @error="(e: Event) => { (e.target as HTMLImageElement).style.display = 'none'; (e.target as HTMLImageElement).nextElementSibling?.classList.remove('hidden') }"
                  />
                  <div
                    :class="[resolveUrl(group.avatar) ? 'hidden' : '', 'group-card-avatar-placeholder', avatarColor(group.name)]"
                  >{{ group.name.charAt(0) }}</div>
                </div>

                <!-- 左侧内容区 -->
                <div class="flex-1 min-w-0">
                  <div class="flex items-center gap-2 min-w-0">
                    <h3 class="text-sm font-semibold text-[var(--zh-text)] truncate">{{ group.name }}</h3>
                    <span
                      v-if="group.myRole === 2"
                      class="role-badge role-badge-owner"
                    >群主</span>
                    <span
                      v-else-if="group.myRole === 1"
                      class="role-badge role-badge-admin"
                    >管理员</span>
                    <span class="flex items-center gap-1 text-xs text-[var(--zh-text-tertiary)] flex-shrink-0">
                      <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" />
                      </svg>
                      {{ group.memberCount }} 人
                    </span>
                  </div>
                  <p class="text-xs text-[var(--zh-text-secondary)] truncate mt-1.5">
                    {{ getGroupLastMessagePreview(group.id) }}
                  </p>
                </div>

                <!-- 右侧：时间 + 箭头 -->
                <div class="flex flex-col items-end shrink-0 gap-1">
                  <span class="text-[10px] text-[var(--zh-text-tertiary)]">
                    {{ formatRelativeTime(groupLastMessages[group.id]?.time) }}
                  </span>
                  <svg class="group-card-arrow" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
                  </svg>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- ==================== 发现群组 Tab ==================== -->
      <div v-else key="search">
        <!-- 搜索栏 -->
        <div class="relative mb-5">
          <div class="search-wrapper" :class="{ 'search-wrapper-focused': groupSearchFocused }">
            <svg class="search-icon" :class="{ 'text-primary': groupSearchFocused }" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
            <input
              ref="searchInputRef"
              v-model="searchKeyword"
              type="text"
              class="search-input"
              placeholder="搜索群组名称、关键词或群号..."
              @focus="groupSearchFocused = true"
              @blur="groupSearchFocused = false"
              @keydown.enter="handleSearch"
            />
            <button
              v-if="searchKeyword"
              class="search-clear-btn"
              @click="clearSearch"
              title="清除搜索"
            >
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
            <button
              class="search-submit-btn"
              @click="handleSearch"
              title="搜索"
            >
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
              </svg>
              <span class="hidden sm:inline">搜索</span>
            </button>
          </div>
        </div>

        <!-- 搜索加载状态 -->
        <div v-if="searchLoading && searchPage === 1" class="space-y-2.5">
          <div v-for="i in 4" :key="i" class="card animate-pulse overflow-hidden">
            <div class="flex items-center gap-3.5 p-4">
              <div class="w-12 h-12 bg-[var(--zh-border)] dark:bg-gray-700 rounded-xl shrink-0"></div>
              <div class="flex-1 space-y-2.5">
                <div class="h-4 bg-[var(--zh-border)] dark:bg-gray-700 rounded w-40"></div>
                <div class="flex items-center gap-3">
                  <div class="h-3 bg-[var(--zh-border)] dark:bg-gray-700 rounded w-24"></div>
                  <div class="h-3 bg-[var(--zh-border)] dark:bg-gray-700 rounded w-16"></div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 搜索无结果 -->
        <div v-else-if="searchKeyword && searchResults.length === 0 && !searchLoading" class="empty-state">
          <div class="empty-state-icon">
            <div class="empty-state-icon-bg bg-gradient-to-br from-amber-50 to-orange-50 dark:from-amber-900/10 dark:to-orange-900/10">
              <svg class="w-11 h-11 text-amber-400 dark:text-amber-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9.172 16.172a4 4 0 015.656 0M9 10h.01M15 10h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
            </div>
          </div>
          <h3 class="empty-state-title">未找到相关群组</h3>
          <p class="empty-state-desc">换个关键词试试，或者创建你自己的群组</p>
          <button
            v-if="userStore.isLoggedIn"
            class="empty-state-action"
            @click="showCreateModal = true"
          >
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
            </svg>
            创建群组
          </button>
        </div>

        <!-- 无搜索关键词：展示推荐群组 -->
        <div v-else-if="!searchKeyword">
          <div v-if="discoverLoading" class="space-y-2.5">
            <div v-for="i in 4" :key="i" class="card animate-pulse overflow-hidden">
              <div class="flex items-center gap-3.5 p-4">
                <div class="w-12 h-12 bg-[var(--zh-border)] dark:bg-gray-700 rounded-xl shrink-0"></div>
                <div class="flex-1 space-y-2.5">
                  <div class="h-4 bg-[var(--zh-border)] dark:bg-gray-700 rounded w-40"></div>
                  <div class="h-3 bg-[var(--zh-border)] dark:bg-gray-700 rounded w-32"></div>
                </div>
              </div>
            </div>
          </div>
          <div v-else>
            <!-- 推荐标签 -->
            <div class="flex items-center gap-2 mb-4">
              <div class="w-7 h-7 rounded-lg bg-amber-100 dark:bg-amber-900/30 flex items-center justify-center">
                <svg class="w-4 h-4 text-amber-500" fill="currentColor" viewBox="0 0 24 24">
                  <path d="M11.48 3.499a.562.562 0 011.04 0l2.125 5.111a.563.563 0 00.475.345l5.518.442c.499.04.701.663.321.988l-4.204 3.602a.563.563 0 00-.182.557l1.285 5.385a.562.562 0 01-.84.61l-4.725-2.885a.563.563 0 00-.586 0L6.982 20.54a.562.562 0 01-.84-.61l1.285-5.386a.562.562 0 00-.182-.557l-4.204-3.602a.563.563 0 01.321-.988l5.518-.442a.563.563 0 00.475-.345L11.48 3.5z" />
                </svg>
              </div>
              <span class="text-sm font-semibold text-[var(--zh-text)]">推荐群组</span>
              <span class="text-xs text-[var(--zh-text-tertiary)]">{{ recommendedGroups.length }} 个群组</span>
            </div>

            <!-- 推荐群组为空 -->
            <div v-if="recommendedGroups.length === 0" class="empty-state py-12">
              <div class="empty-state-icon">
                <div class="empty-state-icon-bg bg-gradient-to-br from-slate-100 to-slate-200 dark:from-gray-800 dark:to-gray-750">
                  <svg class="w-11 h-11 text-slate-400 dark:text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-2.586a1 1 0 00-.707.293l-2.414 2.414a1 1 0 01-.707.293h-3.172a1 1 0 01-.707-.293l-2.414-2.414A1 1 0 006.586 13H4" />
                  </svg>
                </div>
              </div>
              <h3 class="empty-state-title">暂无推荐群组</h3>
              <p class="empty-state-desc">你可以通过上方搜索栏查找感兴趣的群组</p>
            </div>

            <!-- 推荐群组列表 -->
            <div v-else class="space-y-2.5">
              <div
                v-for="group in recommendedGroups"
                :key="group.id"
                class="group-card"
                @click="navigateTo(`/groups/${group.id}`)"
              >
                <div class="group-card-inner">
                  <div class="relative shrink-0">
                    <img
                      v-if="resolveUrl(group.avatar)"
                      :src="resolveUrl(group.avatar) as string"
                      :alt="group.name"
                      class="group-card-avatar"
                      @error="(e: Event) => { (e.target as HTMLImageElement).style.display = 'none'; (e.target as HTMLImageElement).nextElementSibling?.classList.remove('hidden') }"
                    />
                    <div
                      :class="[resolveUrl(group.avatar) ? 'hidden' : '', 'group-card-avatar-placeholder', avatarColor(group.name)]"
                    >{{ group.name.charAt(0) }}</div>
                  </div>
                  <div class="flex-1 min-w-0">
                    <div class="flex items-center gap-2 min-w-0">
                      <h3 class="text-sm font-semibold text-[var(--zh-text)] truncate">{{ group.name }}</h3>
                      <span class="flex items-center gap-1 text-xs text-[var(--zh-text-tertiary)] flex-shrink-0">
                        <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" />
                        </svg>
                        {{ group.memberCount }} 人
                      </span>
                    </div>
                    <p class="text-xs text-[var(--zh-text-secondary)] truncate mt-1.5">
                      {{ getGroupLastMessagePreview(group.id) }}
                    </p>
                  </div>
                  <div class="flex flex-col items-end shrink-0 gap-1">
                    <span class="text-[10px] text-[var(--zh-text-tertiary)]">
                      {{ formatRelativeTime(groupLastMessages[group.id]?.time) }}
                    </span>
                    <button
                      v-if="userStore.isLoggedIn && group.myRole === 0"
                      class="join-btn"
                      :disabled="joiningSet.has(group.id)"
                      @click.stop="handleJoin(group.id)"
                    >
                      <svg v-if="joiningSet.has(group.id)" class="w-3.5 h-3.5 animate-spin" fill="none" viewBox="0 0 24 24">
                        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
                      </svg>
                      {{ joiningSet.has(group.id) ? '申请中' : '申请加入' }}
                    </button>
                    <span
                      v-else-if="group.myRole && group.myRole > 0"
                      class="joined-badge"
                    >
                      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
                      </svg>
                      已加入
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 搜索结果列表 -->
        <div v-else class="space-y-2.5">
          <div class="flex items-center gap-2 mb-3 px-1">
            <div class="w-1.5 h-1.5 rounded-full bg-primary"></div>
            <p class="text-xs text-[var(--zh-text-tertiary)]">
              搜索 "<span class="text-[var(--zh-text)] font-medium">{{ searchKeyword }}</span>" 共找到 <span class="text-primary font-semibold">{{ searchResultTotal || searchResults.length }}</span> 个群组
            </p>
          </div>
          <div
            v-for="group in searchResults"
            :key="group.id"
            class="group-card"
            @click="navigateTo(`/groups/${group.id}`)"
          >
            <div class="group-card-inner">
              <div class="relative shrink-0">
                <img
                  v-if="resolveUrl(group.avatar)"
                  :src="resolveUrl(group.avatar) as string"
                  :alt="group.name"
                  class="group-card-avatar"
                  @error="(e: Event) => { (e.target as HTMLImageElement).style.display = 'none'; (e.target as HTMLImageElement).nextElementSibling?.classList.remove('hidden') }"
                />
                <div
                  :class="[resolveUrl(group.avatar) ? 'hidden' : '', 'group-card-avatar-placeholder', avatarColor(group.name)]"
                >{{ group.name.charAt(0) }}</div>
              </div>
              <div class="flex-1 min-w-0">
                <div class="flex items-center gap-2 min-w-0">
                  <h3 class="text-sm font-semibold text-[var(--zh-text)] truncate">{{ group.name }}</h3>
                  <span class="flex items-center gap-1 text-xs text-[var(--zh-text-tertiary)] flex-shrink-0">
                    <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" />
                    </svg>
                    {{ group.memberCount }} 人
                  </span>
                </div>
                <p class="text-xs text-[var(--zh-text-secondary)] truncate mt-1.5">
                  {{ getGroupLastMessagePreview(group.id) }}
                </p>
              </div>
              <div class="flex flex-col items-end shrink-0 gap-1">
                <span class="text-[10px] text-[var(--zh-text-tertiary)]">
                  {{ formatRelativeTime(groupLastMessages[group.id]?.time) }}
                </span>
                <button
                  v-if="userStore.isLoggedIn && group.myRole === 0"
                  class="join-btn"
                  :disabled="joiningSet.has(group.id)"
                  @click.stop="handleJoin(group.id)"
                >
                  <svg v-if="joiningSet.has(group.id)" class="w-3.5 h-3.5 animate-spin" fill="none" viewBox="0 0 24 24">
                    <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                    <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
                  </svg>
                  {{ joiningSet.has(group.id) ? '申请中' : '申请加入' }}
                </button>
                <span
                  v-else-if="group.myRole && group.myRole > 0"
                  class="joined-badge"
                >
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
                  </svg>
                  已加入
                </span>
              </div>
            </div>
          </div>
        </div>

        <!-- 滑动加载更多（搜索模式） -->
        <div ref="searchSentinelRef" class="h-0.5"></div>
        <div v-if="searchLoading" class="text-center py-4">
          <span class="text-xs text-[var(--zh-text-tertiary)]">
            <svg class="animate-spin inline w-3.5 h-3.5 mr-1" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
            </svg>
            加载中...
          </span>
        </div>
        <div v-else-if="!searchHasMore && searchResults.length > 0" class="text-center py-4">
          <span class="text-xs text-[var(--zh-text-tertiary)]">没有更多了</span>
        </div>
      </div>
    </Transition>

    <!-- ==================== 创建群组弹窗 ==================== -->
    <Teleport to="body">
      <Transition name="modal">
        <div v-if="showCreateModal" class="modal-overlay" @click.self="showCreateModal = false">
          <div class="modal-backdrop"></div>
          <div class="modal-panel">
            <!-- 弹窗头部 -->
            <div class="flex items-center justify-between mb-5">
              <h3 class="text-lg font-bold text-[var(--zh-text)]">创建群组</h3>
              <button
                class="modal-close-btn"
                @click="showCreateModal = false"
              >
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </div>

            <!-- 表单区域 -->
            <div class="space-y-4 mb-6">
              <div>
                <label class="input-label">群组名称 <span class="text-danger">*</span></label>
                <input
                  ref="createNameInputRef"
                  v-model="createForm.name"
                  type="text"
                  class="input"
                  placeholder="给你的群组取个名字"
                  maxlength="30"
                  @keydown.enter="handleCreate"
                />
                <p class="text-right text-[10px] text-[var(--zh-text-tertiary)] mt-1">{{ createForm.name.length }}/30</p>
              </div>
              <div>
                <label class="input-label">群组简介 <span class="text-slate-300 font-normal">(选填)</span></label>
                <textarea
                  v-model="createForm.description"
                  class="input resize-none"
                  rows="3"
                  placeholder="介绍一下这个群组..."
                  maxlength="200"
                ></textarea>
                <p class="text-right text-[10px] text-[var(--zh-text-tertiary)] mt-1">{{ createForm.description.length }}/200</p>
              </div>

              <!-- 群组可见性 -->
              <div>
                <label class="input-label">可见性</label>
                <div class="flex gap-2">
                  <button
                    class="visibility-option"
                    :class="createForm.isPublic ? 'visibility-option-active' : 'visibility-option-inactive'"
                    @click="createForm.isPublic = 1"
                  >
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3.055 11H5a2 2 0 012 2v1a2 2 0 002 2 2 2 0 012 2v2.945M8 3.935V5.5A2.5 2.5 0 0010.5 8h.5a2 2 0 012 2 2 2 0 104 0 2 2 0 012-2h1.064M15 20.488V18a2 2 0 012-2h3.064M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                    </svg>
                    公开
                  </button>
                  <button
                    class="visibility-option"
                    :class="!createForm.isPublic ? 'visibility-option-active' : 'visibility-option-inactive'"
                    @click="createForm.isPublic = 0"
                  >
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
                    </svg>
                    私密
                  </button>
                </div>
                <p class="text-[10px] text-[var(--zh-text-tertiary)] mt-1.5">
                  {{ createForm.isPublic ? '所有人都可以发现和加入' : '仅群主和管理员可邀请成员加入' }}
                </p>
              </div>
            </div>

            <!-- 底部按钮 -->
            <div class="flex gap-3">
              <button class="flex-1 modal-cancel-btn" @click="showCreateModal = false">取消</button>
              <button
                class="flex-1 modal-submit-btn"
                :disabled="!createForm.name.trim() || creating"
                @click="handleCreate"
              >
                <svg v-if="creating" class="w-4 h-4 animate-spin" fill="none" viewBox="0 0 24 24">
                  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                  <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
                </svg>
                {{ creating ? '创建中...' : '创建群组' }}
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
/** 群组广场页 */
import { groupApi } from '@/api/group'
import type { GroupInfo, GroupMessage } from '@/api/group'
import { showToast } from '@/composables/useToast'
import { avatarColor } from '@/utils/color'
import { useResourceUrl } from '@/composables/useResourceUrl'

const { resolveUrl } = useResourceUrl()

const { setTitle } = usePageHeaderTitle()
setTitle('群组广场')

const userStore = useUserStore()
const router = useRouter()
const navigateTo = (path: string) => router.push(path)

const activeTab = ref<'my' | 'search'>('my')
const searchKeyword = ref('')
const searchInputRef = ref<HTMLInputElement | null>(null)
const searchSentinelRef = ref<HTMLElement | null>(null)
const groupSearchFocused = ref(false)

// 我的群组
const myGroups = ref<GroupInfo[]>([])
const myGroupsLoading = ref(false)

// 推荐群组 / 发现群组
const recommendedGroups = ref<GroupInfo[]>([])
const discoverLoading = ref(false)

// 搜索
const searchResults = ref<GroupInfo[]>([])
const searchLoading = ref(false)
const searchPage = ref(1)
const searchHasMore = ref(false)
const searchResultTotal = ref(0)

// 创建弹窗
const showCreateModal = ref(false)
const creating = ref(false)
const createNameInputRef = ref<HTMLInputElement | null>(null)
const createForm = reactive({ name: '', description: '', isPublic: 1 })

// 群组最后一条消息信息
const groupLastMessages = reactive<Record<number, { content: string; time: string; type?: string }>>({})

// 批量获取群组最新消息
const fetchGroupLastMessages = async (groups: GroupInfo[]) => {
  if (!groups.length) return
  // 仅获取用户已加入的群组的最新消息，避免非成员调用getMessages返回403
  const joinedGroups = groups.filter(g => g.myRole != null && g.myRole !== undefined)
  await Promise.all(joinedGroups.map(async (g) => {
    if (groupLastMessages[g.id]) return // 已缓存
    try {
      const { data } = await groupApi.getMessages(g.id, 0, 50)
      const raw = data?.data || data
      const msgs: GroupMessage[] = Array.isArray(raw) ? raw : []
      if (msgs.length > 0) {
        // 取最后一条作为最新消息（无论正序/倒序，取首或尾中时间最新的那条）
        const first = msgs[0] as GroupMessage
        const last = msgs[msgs.length - 1] as GroupMessage
        const target = new Date(first.createdAt || 0).getTime() > new Date(last.createdAt || 0).getTime() ? first : last
        groupLastMessages[g.id] = {
          content: target.content || '',
          time: target.createdAt || '',
          type: target.messageType,
        }
      }
    } catch {
      // 静默失败
    }
  }))
}

const getGroupLastMessagePreview = (groupId: number) => {
  const msg = groupLastMessages[groupId]
  if (!msg || !msg.content) return ''
  if (msg.type === 'image') return '[图片]'
  return msg.content.length > 30 ? msg.content.slice(0, 30) + '...' : msg.content
}

/** 相对时间格式化 */
const formatRelativeTime = (timeStr?: string) => {
  if (!timeStr) return ''
  const now = Date.now()
  const time = new Date(timeStr).getTime()
  if (isNaN(time)) return ''
  const diff = now - time

  if (diff < 60 * 1000) return '刚刚'
  if (diff < 60 * 60 * 1000) return `${Math.floor(diff / (60 * 1000))}分钟前`
  if (diff < 24 * 60 * 60 * 1000) return `${Math.floor(diff / (60 * 60 * 1000))}小时前`

  const date = new Date(timeStr)
  const nowDate = new Date()
  const pad = (n: number) => n.toString().padStart(2, '0')

  if (date.getFullYear() === nowDate.getFullYear()) {
    if (date.getDate() === nowDate.getDate() - 1) {
      return `昨天 ${pad(date.getHours())}:${pad(date.getMinutes())}`
    }
    return `${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
  }
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

const switchTab = (tab: 'my' | 'search') => {
  activeTab.value = tab
  if (tab === 'my') {
    loadMyGroups()
  } else {
    loadRecommendedGroups()
    nextTick(() => searchInputRef.value?.focus())
  }
}

// ======= 我的群组 =======
const loadMyGroups = async () => {
  myGroupsLoading.value = true
  try {
    const { data } = await groupApi.getMyGroups()
    myGroups.value = data?.data?.list || data?.data || []
    await fetchGroupLastMessages(myGroups.value)
  } catch {
    myGroups.value = []
    showToast('加载失败，请稍后重试', 'error', { position: 'top-center' })
  } finally {
    myGroupsLoading.value = false
  }
}

// ======= 推荐群组 =======
const loadRecommendedGroups = async () => {
  if (recommendedGroups.value.length > 0) return
  discoverLoading.value = true
  try {
    const { data } = await groupApi.searchGroups('', 1, 20)
    const list = data?.data?.list || data?.data || []
    recommendedGroups.value = Array.isArray(list) ? list : []
    await fetchGroupLastMessages(recommendedGroups.value)
  } catch {
    recommendedGroups.value = []
  } finally {
    discoverLoading.value = false
  }
}

// ======= 搜索 =======
const clearSearch = () => {
  searchKeyword.value = ''
  searchResults.value = []
  searchHasMore.value = false
  searchResultTotal.value = 0
  searchInputRef.value?.focus()
}

const handleSearch = async () => {
  const keyword = searchKeyword.value.trim()
  if (!keyword) { clearSearch(); return }
  searchPage.value = 1
  searchLoading.value = true
  try {
    const { data } = await groupApi.searchGroups(keyword, 1, 20)
    const list = data?.data?.list || data?.data || []
    searchResults.value = Array.isArray(list) ? list : []
    searchHasMore.value = searchResults.value.length >= 20
    searchResultTotal.value = data?.data?.total || searchResults.value.length
    await fetchGroupLastMessages(searchResults.value)
  } catch {
    searchResults.value = []
    showToast('搜索失败，请稍后重试', 'error', { position: 'top-center' })
  } finally {
    searchLoading.value = false
  }
}

const loadMoreSearch = async () => {
  searchPage.value++
  searchLoading.value = true
  try {
    const { data } = await groupApi.searchGroups(searchKeyword.value.trim(), searchPage.value, 20)
    const list = data?.data?.list || data?.data || []
    const arr = Array.isArray(list) ? list : []
    searchResults.value.push(...arr)
    searchHasMore.value = arr.length >= 20
    await fetchGroupLastMessages(arr)
  } catch {
    searchPage.value--
    showToast('加载失败，请稍后重试', 'error', { position: 'top-center' })
  } finally {
    searchLoading.value = false
  }
}

// 搜索防抖
let searchTimer: ReturnType<typeof setTimeout> | null = null
watch(searchKeyword, (val) => {
  if (searchTimer) clearTimeout(searchTimer)
  if (!val.trim()) { searchResults.value = []; searchHasMore.value = false; searchResultTotal.value = 0; return }
  searchTimer = setTimeout(handleSearch, 350)
})

// ======= 申请加入群组 =======
const joiningSet = ref(new Set<number>())
const handleJoin = async (groupId: number) => {
  if (joiningSet.value.has(groupId)) return
  joiningSet.value.add(groupId)
  try {
    await groupApi.requestJoin(groupId)
    showToast('申请已提交，等待群主审批', 'success', { position: 'top-center' })
  } catch (e: any) {
    const msg = e?.message || ''
    if (msg.includes('已提交') || msg.includes('已是')) {
      showToast(msg, 'info', { position: 'top-center' })
    } else {
      showToast('申请失败，请稍后重试', 'error', { position: 'top-center' })
    }
  } finally {
    joiningSet.value.delete(groupId)
  }
}

// ======= 创建群组 =======
const handleCreate = async () => {
  if (!createForm.name.trim()) return
  creating.value = true
  try {
    const { data } = await groupApi.createGroup({
      name: createForm.name.trim(),
      description: createForm.description.trim() || undefined,
      isPublic: createForm.isPublic,
    })
    showToast('群组创建成功', 'success', { position: 'top-center' })
    showCreateModal.value = false
    createForm.name = ''
    createForm.description = ''
    createForm.isPublic = 1
    loadMyGroups()
    navigateTo(`/groups/${data?.data}`)
  } catch {
    showToast('创建失败，请稍后重试', 'error', { position: 'top-center' })
  } finally {
    creating.value = false
  }
}

// 监听创建弹窗打开，自动聚焦
watch(showCreateModal, (val) => {
  if (val) nextTick(() => createNameInputRef.value?.focus())
})

// 搜索无限滚动监听
let searchObserver: IntersectionObserver | null = null

onMounted(() => {
  if (userStore.isLoggedIn) loadMyGroups()
  // 设置搜索结果的无限滚动
  nextTick(() => {
    if (searchSentinelRef.value) {
      searchObserver = new IntersectionObserver((entries) => {
        if (entries[0]?.isIntersecting && searchHasMore.value && !searchLoading.value) {
          loadMoreSearch()
        }
      }, { rootMargin: '200px' })
      searchObserver.observe(searchSentinelRef.value)
    }
  })
})

onUnmounted(() => {
  searchObserver?.disconnect()
})

useHead({ title: '群组广场 - 知讯' })
</script>

<style scoped>
/* ==================== Tab 切换动画 ==================== */
.tab-fade-enter-active,
.tab-fade-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}
.tab-fade-enter-from {
  opacity: 0;
  transform: translateY(6px);
}
.tab-fade-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}

/* ==================== 空状态通用样式 ==================== */
.empty-state {
  @apply text-center py-16 md:py-20;
}

.empty-state-icon {
  @apply mb-6;
}

.empty-state-icon-bg {
  @apply w-20 h-20 mx-auto rounded-2xl flex items-center justify-center;
}

.empty-state-title {
  @apply text-[15px] font-semibold text-[var(--zh-text)] mb-2;
}

.empty-state-desc {
  @apply text-sm text-[var(--zh-text-tertiary)] mb-6;
}

.empty-state-action {
  @apply inline-flex items-center gap-1.5 px-5 py-2.5 bg-primary text-white rounded-xl text-sm font-semibold
         hover:bg-primary-600 active:scale-95 transition-all duration-200 shadow-[var(--shadow-sm)] hover:shadow-[var(--shadow-md)];
}

/* ==================== 搜索栏 ==================== */
.search-wrapper {
  @apply relative flex items-center;
}
.search-wrapper-focused .search-input {
  border-color: var(--zh-primary);
  box-shadow: 0 0 0 3px rgba(var(--zh-primary-rgb), 0.08);
}

.search-icon {
  @apply absolute left-3.5 top-1/2 -translate-y-1/2 w-5 h-5 text-[var(--zh-text-tertiary)] pointer-events-none z-10 transition-colors duration-200;
}

.search-input {
  @apply w-full h-11 pl-11 pr-24 rounded-xl border-1.5 border-[var(--zh-border)] dark:border-gray-600
         bg-[var(--zh-bg)] dark:bg-gray-800 text-sm text-[var(--zh-text)]
         placeholder:text-[var(--zh-text-tertiary)] placeholder:text-sm
         focus:outline-none
         transition-all duration-200;
}

.search-clear-btn {
  @apply absolute right-[72px] top-1/2 -translate-y-1/2 w-7 h-7 flex items-center justify-center
         rounded-lg text-[var(--zh-text-tertiary)] hover:text-[var(--zh-text-secondary)]
         hover:bg-slate-100 dark:hover:bg-gray-700 transition-colors z-10;
}

.search-submit-btn {
  @apply absolute right-1.5 top-1/2 -translate-y-1/2 h-8 flex items-center gap-1 px-3
         rounded-lg bg-primary text-white text-xs font-semibold
         hover:bg-primary-600 active:scale-95 transition-all duration-200 z-10;
}

/* ==================== 群组卡片 ==================== */
.group-card {
  @apply card cursor-pointer overflow-hidden;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  border-left: 3px solid transparent;
}

.group-card:hover {
  border-left-color: var(--color-primary, #6366F1);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06), 0 1px 3px rgba(0, 0, 0, 0.04);
  transform: translateX(3px);
}

.group-card:active {
  transform: scale(0.995);
  transition: transform 0.1s ease;
}

.group-card-inner {
  @apply flex items-center gap-3.5 p-4;
}

.group-card-avatar {
  @apply w-12 h-12 rounded-xl object-cover ring-2 ring-[var(--zh-border)] dark:ring-gray-600;
}

.group-card-avatar-placeholder {
  @apply w-12 h-12 rounded-xl flex items-center justify-center text-white text-base font-bold
         ring-2 ring-white/20;
}

.group-card-arrow {
  @apply w-5 h-5 text-slate-300 dark:text-gray-500 shrink-0 transition-transform duration-200;
}

.group-card:hover .group-card-arrow {
  transform: translateX(2px);
  color: var(--color-primary, #6366F1);
}

/* ==================== 角色徽章 ==================== */
.role-badge {
  @apply shrink-0 text-[10px] px-1.5 py-0.5 rounded-md font-semibold;
}

.role-badge-owner {
  @apply bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400;
}

.role-badge-admin {
  @apply bg-blue-50 text-blue-600 dark:bg-blue-900/30 dark:text-blue-400;
}

/* ==================== 加入按钮 ==================== */
.join-btn {
  @apply shrink-0 flex items-center gap-1 px-4 py-1.5 rounded-lg text-xs font-semibold
         bg-primary text-white hover:bg-primary-600 active:scale-95
         transition-all duration-200 shadow-sm disabled:opacity-60 disabled:cursor-not-allowed;
}

.joined-badge {
  @apply shrink-0 flex items-center gap-1 text-xs text-emerald-600 dark:text-emerald-400 font-medium;
}

/* ==================== 弹窗 ==================== */
.modal-overlay {
  @apply fixed inset-0 z-50 flex items-center justify-center p-4;
}

.modal-backdrop {
  @apply absolute inset-0 bg-black/50 backdrop-blur-sm;
}

.modal-panel {
  @apply relative bg-[var(--zh-bg-elevated)] dark:bg-gray-800 rounded-2xl p-6 w-full max-w-md
         shadow-2xl border border-[var(--zh-border)] dark:border-gray-700;
}

.modal-close-btn {
  @apply w-8 h-8 flex items-center justify-center rounded-lg
         text-[var(--zh-text-tertiary)] hover:text-[var(--zh-text-secondary)]
         hover:bg-slate-100 dark:hover:bg-gray-700 transition-colors;
}

.input-label {
  @apply block text-xs font-semibold text-[var(--zh-text-secondary)] dark:text-gray-300 mb-1.5;
}

.visibility-option {
  @apply flex-1 flex items-center justify-center gap-2 px-3 py-2.5 rounded-xl text-sm font-medium
         border-2 transition-all duration-200;
}

.visibility-option-active {
  @apply border-primary bg-primary/5 text-primary;
}

.visibility-option-inactive {
  @apply border-[var(--zh-border)] dark:border-gray-600
         text-[var(--zh-text-secondary)] dark:text-gray-400
         hover:border-gray-300 dark:hover:border-gray-500;
}

.modal-cancel-btn {
  @apply py-2.5 rounded-xl text-sm font-semibold
         text-[var(--zh-text-secondary)] dark:text-gray-300
         bg-slate-100 dark:bg-gray-700
         hover:bg-slate-200 dark:hover:bg-gray-600
         transition-colors duration-200;
}

.modal-submit-btn {
  @apply py-2.5 rounded-xl text-sm font-semibold
         bg-primary text-white
         hover:bg-primary-600 active:scale-[0.98]
         transition-all duration-200
         disabled:opacity-50 disabled:cursor-not-allowed disabled:active:scale-100
         flex items-center justify-center gap-1.5;
}

/* ==================== 弹窗动画 ==================== */
.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.25s ease;
}
.modal-enter-active .modal-panel,
.modal-leave-active .modal-panel {
  transition: transform 0.25s cubic-bezier(0.4, 0, 0.2, 1), opacity 0.25s ease;
}
.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}
.modal-enter-from .modal-panel {
  transform: scale(0.95) translateY(10px);
  opacity: 0;
}
.modal-leave-to .modal-panel {
  transform: scale(0.95) translateY(10px);
  opacity: 0;
}

/* ==================== 响应式 ==================== */
@media (max-width: 640px) {
  .search-input {
    @apply pl-10 pr-20 text-sm;
  }

  .search-submit-btn {
    @apply px-2;
  }

  .search-submit-btn span {
    @apply hidden;
  }

  .search-clear-btn {
    right: 56px;
  }

  .empty-state {
    @apply py-12;
  }

  .empty-state-icon-bg {
    @apply w-16 h-16 rounded-xl;
  }

  .empty-state-icon-bg svg {
    @apply w-9 h-9;
  }

  .group-card-inner {
    @apply p-3;
  }

  .group-card-avatar,
  .group-card-avatar-placeholder {
    @apply w-10 h-10 rounded-lg;
  }

  .join-btn {
    @apply px-3 py-1 text-[11px];
  }
}
</style>