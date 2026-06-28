<template>
  <!-- 搜索页 -->
  <div class="search-page max-w-[1200px] 2xl:max-w-[1400px] mx-auto px-0.5 py-0.5">
    <!-- 移动端：固定顶栏（返回按钮 + 搜索框） -->
    <div class="md:hidden fixed top-0 left-0 right-0 z-[60] h-10 flex items-center gap-1.5 px-1.5 bg-[var(--zh-bg-elevated)]/85 dark:bg-gray-800/85 backdrop-blur-md border-b border-[var(--zh-border)]/60 dark:border-gray-700/60">
      <button
        class="w-8 h-8 flex items-center justify-center text-[var(--zh-text-secondary)] dark:text-gray-200 hover:text-primary dark:hover:text-primary-400 active:scale-95 transition no-tap-highlight touch-target shrink-0"
        aria-label="返回上一页"
        @click="goBack"
      >
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.2" d="M15 19l-7-7 7-7" />
        </svg>
      </button>
      <div class="search-box-mobile flex-1 min-w-0 flex items-center bg-[var(--zh-bg)] dark:bg-gray-700 rounded-full px-2.5 py-1 border-1.5" :class="mobileSearchFocused ? 'search-box-mobile--focused' : 'border-[var(--zh-border)] dark:border-gray-600'">
        <svg v-if="autoSearching" class="w-4 h-4 text-primary animate-spin shrink-0" fill="none" viewBox="0 0 24 24">
          <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
          <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
        </svg>
        <svg v-else class="search-box-mobile__icon w-4 h-4 transition-all duration-300 shrink-0" :class="mobileSearchFocused ? 'text-[var(--zh-text-secondary)] scale-110' : 'text-[var(--zh-text-tertiary)]'" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
        </svg>
        <input
          ref="searchInputRef"
          v-model="keyword"
          type="text"
          class="flex-1 bg-transparent border-none outline-none ml-1.5 text-sm text-[var(--zh-text)] dark:text-[var(--zh-text)] placeholder-[var(--zh-text-placeholder)] min-w-0"
          placeholder="搜索作品、用户..."
          @focus="mobileSearchFocused = true"
          @blur="mobileSearchFocused = false"
          @input="handleInput"
          @keydown.enter="doSearch"
        />
        <button v-show="keyword" class="search-box-mobile__clear p-0.5 text-[var(--zh-text-tertiary)] hover:text-[var(--zh-text-secondary)] shrink-0" @click="clearKeyword">
          <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>
      </div>
    </div>

    <!-- 移动端：为固定顶栏留出空间 -->
    <div class="md:hidden h-10"></div>

    <!-- 桌面端搜索框 -->
    <div class="hidden md:block mb-1 md:mb-1.5">
      <div class="search-box-desktop flex items-center bg-[var(--zh-bg)] rounded-full px-3 md:px-4 py-2.5 md:py-3 shadow-sm border-1.5" :class="desktopSearchFocused ? 'search-box-desktop--focused' : 'border-[var(--zh-border)]'">
        <!-- 加载指示器 -->
        <svg v-if="autoSearching" class="w-5 h-5 md:w-5.5 md:h-5.5 text-primary animate-spin shrink-0" fill="none" viewBox="0 0 24 24">
          <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
          <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
        </svg>
        <svg v-else class="search-box-desktop__icon w-5 h-5 md:w-5.5 md:h-5.5 transition-all duration-300 shrink-0" :class="desktopSearchFocused ? 'text-[var(--zh-text-secondary)] scale-110' : 'text-[var(--zh-text-tertiary)]'" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
        </svg>
        <input
          ref="searchInputDesktopRef"
          v-model="keyword"
          type="text"
          class="flex-1 bg-transparent border-none outline-none ml-1.5 md:ml-2 text-sm md:text-base text-[var(--zh-text)] placeholder-[var(--zh-text-placeholder)]"
          placeholder="搜索作品、用户..."
          @focus="desktopSearchFocused = true"
          @blur="desktopSearchFocused = false"
          @input="handleInput"
          @keydown.enter="doSearch"
        />
        <button v-show="keyword" class="search-box-desktop__clear p-0.5 md:p-1 text-[var(--zh-text-tertiary)] hover:text-[var(--zh-text-secondary)] shrink-0" @click="clearKeyword">
          <svg class="w-3.5 h-3.5 md:w-4 md:h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>
        <!-- 清除按钮占位（保持搜索按钮位置固定） -->
        <span v-show="!keyword" class="w-5 md:w-6 h-3.5 md:h-4 shrink-0"></span>
        <!-- 搜索按钮 -->
        <button class="search-btn ml-1.5 md:ml-2 px-3 md:px-4 py-1.5 md:py-2 text-white text-sm md:text-base rounded-full shrink-0" @click="doSearch">
          搜索
        </button>
      </div>

    </div>

    <!-- 搜索建议/热门搜索（未搜索时） -->
    <div v-if="!hasSearched" class="text-xs md:text-sm">
      <!-- 搜索建议 -->
      <div v-if="suggestions.length > 0" class="mb-3 md:mb-6">
        <h3 class="text-xs md:text-sm font-medium text-[var(--zh-text-secondary)] mb-2 md:mb-3">搜索建议</h3>
        <div class="space-y-0.5 md:space-y-1">
          <button
            v-for="item in suggestions"
            :key="`${item.type}-${item.id}`"
            class="suggest-item w-full text-left flex items-center px-2.5 md:px-4 py-1.5 md:py-2 text-xs md:text-sm text-[var(--zh-text-secondary)] rounded-lg"
            @click="selectSuggestion(item)"
          >
            <!-- 类型图标 -->
            <span class="suggest-item__icon w-5 h-5 md:w-6 md:h-6 mr-1.5 md:mr-2 shrink-0 flex items-center justify-center rounded-md" :class="'suggest-item__icon--' + item.type">
              <svg v-if="item.type === 'user'" class="w-3.5 h-3.5 md:w-4 md:h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
              </svg>
              <svg v-else-if="item.type === 'article'" class="w-3.5 h-3.5 md:w-4 md:h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
              </svg>
              <svg v-else class="w-3.5 h-3.5 md:w-4 md:h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A1.994 1.994 0 013 12V7a4 4 0 014-4z" />
              </svg>
            </span>
            <!-- 用户头像 -->
            <UserAvatar v-if="item.type === 'user'" :src="item.avatar" :alt="item.text" size="xs" class="mr-1.5 md:mr-2" />
            <span v-html="highlightKeyword(item.text)"></span>
            <span class="ml-auto text-[10px] md:text-xs text-[var(--zh-text-tertiary)]">{{ item.type === 'user' ? '用户' : item.type === 'article' ? '作品' : '标签' }}</span>
          </button>
        </div>
      </div>

      <!-- 搜索历史 -->
      <div v-if="searchHistory.length > 0" class="mb-3 md:mb-6">
        <div class="flex items-center justify-between mb-2 md:mb-3">
          <h3 class="text-xs md:text-sm font-medium text-[var(--zh-text-secondary)]">搜索历史</h3>
          <button class="text-[10px] md:text-xs text-[var(--zh-text-tertiary)] hover:text-danger transition-colors duration-150" @click="clearHistory">清除全部</button>
        </div>
        <div class="flex flex-wrap gap-1.5 md:gap-2">
          <div v-for="item in searchHistory" :key="item" class="history-tag">
            <button class="history-tag__text" @click="keyword = item; doSearch()">{{ item }}</button>
            <button class="history-tag__delete" @click.stop="removeHistoryItem(item)" aria-label="删除历史记录">
              <svg class="w-2.5 h-2.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>
        </div>
      </div>

      <!-- 热门搜索 -->
      <div v-if="hotSearches.length > 0">
        <h3 class="text-xs md:text-sm font-medium text-[var(--zh-text-secondary)] mb-1.5 md:mb-2">热门搜索</h3>
        <div class="space-y-0.5 md:space-y-1">
          <button v-for="(item, index) in hotSearches" :key="item" class="hot-item w-full text-left flex items-center px-2.5 md:px-4 py-1.5 md:py-2 text-xs md:text-sm text-[var(--zh-text-secondary)] rounded-lg" @click="keyword = item; doSearch()">
            <span
              class="hot-rank w-4.5 md:w-5.5 text-center text-[10px] md:text-xs font-bold rounded-full shrink-0"
              :class="index < 3 ? 'hot-rank--' + (index + 1) : 'hot-rank--normal'"
            >{{ index + 1 }}</span>
            <span class="ml-2 md:ml-3">{{ item }}</span>
          </button>
        </div>
      </div>
    </div>

    <!-- 搜索结果（已搜索时） -->
    <div v-else class="text-xs md:text-sm">
      <!-- Tab切换 -->
      <div class="search-tabs relative flex items-center border-b border-[var(--zh-border)] mb-2 md:mb-4">
        <button
          v-for="tab in searchTabs"
          :key="tab.key"
          class="search-tabs__btn px-2.5 md:px-4 py-2 md:py-3 text-xs md:text-sm font-medium transition-colors duration-200"
          :class="activeTab === tab.key
            ? 'text-primary'
            : 'text-[var(--zh-text-secondary)] hover:text-[var(--zh-text)]'"
          @click="switchTab(tab.key)"
        >
          {{ tab.label }}
          <span v-if="tabCounts[tab.key]" class="ml-0.5 md:ml-1 text-[10px] md:text-xs text-[var(--zh-text-tertiary)]">({{ tabCounts[tab.key] }})</span>
        </button>
        <!-- 滑动指示器 -->
        <div class="search-tabs__indicator" :style="tabIndicatorStyle"></div>
      </div>

      <!-- 筛选栏（紧凑型） -->
      <div class="filter-bar flex items-center flex-wrap gap-1 mb-1 md:mb-1.5">
        <!-- 分类筛选 -->
        <select v-model="filterCategoryId" class="filter-select" @change="doSearch()">
          <option :value="undefined">全部分类</option>
          <option v-for="cat in categories" :key="cat.id" :value="cat.id">{{ cat.name }}</option>
        </select>

        <!-- 时间范围筛选 -->
        <select v-model="filterTimeRange" class="filter-select" @change="doSearch()">
          <option :value="undefined">全部时间</option>
          <option value="24h">24小时</option>
          <option value="7d">7天</option>
          <option value="30d">30天</option>
        </select>

        <!-- 排序 -->
        <select v-model="sortBy" class="filter-select" @change="doSearch()">
          <option value="relevance">相关度</option>
          <option value="latest">最新</option>
          <option value="popular">最热</option>
        </select>
      </div>

      <!-- 搜索中骨架屏 -->
      <div v-if="loading">
        <!-- 综合Tab骨架屏 -->
        <template v-if="activeTab === 'all'">
          <div class="mb-2 md:mb-3">
            <div class="h-3 md:h-4 bg-[var(--zh-border)] rounded w-16 md:w-20 mb-2 md:mb-3 animate-pulse"></div>
            <div class="space-y-1.5 md:space-y-2">
              <LoadingSkeleton v-for="i in 2" :key="'a'+i" type="article" />
            </div>
          </div>
          <div class="mb-2 md:mb-3">
            <div class="h-3 md:h-4 bg-[var(--zh-border)] rounded w-16 md:w-20 mb-2 md:mb-3 animate-pulse"></div>
            <div class="space-y-1.5 md:space-y-2">
              <LoadingSkeleton v-for="i in 2" :key="'u'+i" type="user" />
            </div>
          </div>
          <div>
            <div class="h-3 md:h-4 bg-[var(--zh-border)] rounded w-16 md:w-20 mb-2 md:mb-3 animate-pulse"></div>
            <div class="grid grid-cols-2 sm:grid-cols-3 gap-2 md:gap-3">
              <div v-for="i in 6" :key="'img'+i" class="aspect-square bg-[var(--zh-border)] rounded-lg animate-pulse"></div>
            </div>
          </div>
        </template>
        <!-- 作品Tab骨架屏 -->
        <template v-else-if="activeTab === 'articles'">
          <div class="space-y-2 md:space-y-3">
            <LoadingSkeleton v-for="i in 5" :key="i" type="article" />
          </div>
        </template>
        <!-- 用户Tab骨架屏 -->
        <template v-else-if="activeTab === 'users'">
          <div class="space-y-2 md:space-y-3">
            <LoadingSkeleton v-for="i in 5" :key="i" type="user" />
          </div>
        </template>
        <!-- 图片Tab骨架屏 -->
        <template v-else-if="activeTab === 'images'">
          <div class="grid grid-cols-2 sm:grid-cols-3 gap-2 md:gap-3">
            <div v-for="i in 9" :key="i" class="aspect-square bg-[var(--zh-border)] rounded-lg animate-pulse"></div>
          </div>
        </template>
      </div>

      <!-- 搜索失败 -->
      <ErrorRetry v-else-if="searchError" :message="searchError" :on-retry="retrySearch" />

      <!-- 搜索结果列表 -->
      <div v-else-if="hasAnyResult">
        <!-- ===== 综合Tab ===== -->
        <template v-if="activeTab === 'all'">
          <!-- 用户区块 -->
          <div v-if="allUserResults.length > 0" class="mb-2 md:mb-3">
            <div class="flex items-center justify-between mb-1.5 md:mb-2">
              <h3 class="text-xs md:text-sm font-medium text-[var(--zh-text-secondary)]">相关用户</h3>
              <button v-if="tabCounts.users > 3" class="text-[10px] md:text-xs text-primary hover:underline" @click="switchTab('users')">查看全部 ({{ tabCounts.users }})</button>
            </div>
            <div class="result-list space-y-2 md:space-y-3">
              <UserCard v-for="user in allUserResults" :key="'u-'+user.id" :user="user" :show-follow-button="true" @toggle-follow="toggleFollow" />
            </div>
          </div>

          <!-- 作品区块 -->
          <div v-if="allArticleResults.length > 0" class="mb-2 md:mb-3">
            <div class="flex items-center justify-between mb-1.5 md:mb-2">
              <h3 class="text-xs md:text-sm font-medium text-[var(--zh-text-secondary)]">相关作品</h3>
              <button v-if="tabCounts.articles > 5" class="text-[10px] md:text-xs text-primary hover:underline" @click="switchTab('articles')">查看全部 ({{ tabCounts.articles }})</button>
            </div>
            <div class="result-list space-y-2 md:space-y-3">
              <ArticleCard v-for="item in allArticleResults" :key="'a-'+item.id" :article="item" />
            </div>
          </div>

          <!-- 图片区块 -->
          <div v-if="allImageResults.length > 0" class="mb-2 md:mb-3">
            <div class="flex items-center justify-between mb-1.5 md:mb-2">
              <h3 class="text-xs md:text-sm font-medium text-[var(--zh-text-secondary)]">相关图片</h3>
              <button v-if="tabCounts.images > 6" class="text-[10px] md:text-xs text-primary hover:underline" @click="switchTab('images')">查看全部 ({{ tabCounts.images }})</button>
            </div>
            <ImageGrid :images="allImageResults" @click="handleImageClick" />
          </div>
        </template>

        <!-- ===== 作品Tab ===== -->
        <template v-if="activeTab === 'articles'">
          <div class="result-list space-y-2 md:space-y-3">
            <ArticleCard v-for="item in articleResults" :key="item.id" :article="item" />
          </div>
          <!-- 加载更多 -->
          <div ref="articleSentinel" class="h-0.5 md:h-1"></div>
          <div v-if="loadingMore" class="py-3 md:py-4 text-center text-xs md:text-sm text-[var(--zh-text-tertiary)]">
            <svg class="animate-spin inline w-3.5 h-3.5 md:w-4 md:h-4 mr-1" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
            </svg>
            加载中...
          </div>
          <div v-if="!hasMoreArticles && articleResults.length > 0" class="py-3 md:py-4 text-center text-xs md:text-sm text-[var(--zh-text-tertiary)]">没有更多了</div>
        </template>

        <!-- ===== 用户Tab ===== -->
        <template v-if="activeTab === 'users'">
          <div class="result-list space-y-2 md:space-y-3">
            <UserCard v-for="user in userResults" :key="user.id" :user="user" :show-follow-button="true" @toggle-follow="toggleFollow" />
          </div>
          <!-- 加载更多 -->
          <div ref="userSentinel" class="h-0.5 md:h-1"></div>
          <div v-if="loadingMore" class="py-3 md:py-4 text-center text-xs md:text-sm text-[var(--zh-text-tertiary)]">
            <svg class="animate-spin inline w-3.5 h-3.5 md:w-4 md:h-4 mr-1" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
            </svg>
            加载中...
          </div>
          <div v-if="!hasMoreUsers && userResults.length > 0" class="py-3 md:py-4 text-center text-xs md:text-sm text-[var(--zh-text-tertiary)]">没有更多了</div>
        </template>

        <!-- ===== 图片Tab ===== -->
        <template v-if="activeTab === 'images'">
          <ImageGrid :images="imageResults" @click="handleImageClick" />
          <!-- 加载更多 -->
          <div ref="imageSentinel" class="h-0.5 md:h-1"></div>
          <div v-if="loadingMore" class="py-3 md:py-4 text-center text-xs md:text-sm text-[var(--zh-text-tertiary)]">
            <svg class="animate-spin inline w-3.5 h-3.5 md:w-4 md:h-4 mr-1" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
            </svg>
            加载中...
          </div>
          <div v-if="!hasMoreImages && imageResults.length > 0" class="py-3 md:py-4 text-center text-xs md:text-sm text-[var(--zh-text-tertiary)]">没有更多了</div>
        </template>
      </div>

      <!-- 空结果 -->
      <div v-else class="search-empty py-6 md:py-10 text-center">
        <div class="search-empty__illustration mb-3 md:mb-4">
          <svg class="w-24 h-24 md:w-32 md:h-32 mx-auto" viewBox="0 0 120 120" fill="none" xmlns="http://www.w3.org/2000/svg">
            <!-- 放大镜 -->
            <circle cx="50" cy="50" r="28" stroke="currentColor" stroke-width="3" stroke-linecap="round" class="text-[var(--zh-border)]"/>
            <path d="M70 70L90 90" stroke="currentColor" stroke-width="3" stroke-linecap="round" class="text-[var(--zh-border)]"/>
            <!-- 问号 -->
            <text x="50" y="56" text-anchor="middle" font-size="28" font-weight="700" fill="currentColor" class="text-[var(--zh-text-tertiary)]">?</text>
            <!-- 装饰小点 -->
            <circle cx="30" cy="30" r="3" class="fill-[var(--zh-primary-light)] opacity-40"/>
            <circle cx="85" cy="35" r="2" class="fill-[var(--zh-primary-light)] opacity-30"/>
            <circle cx="20" cy="70" r="2.5" class="fill-[var(--zh-primary-light)] opacity-25"/>
            <circle cx="95" cy="65" r="2" class="fill-[var(--zh-primary-light)] opacity-35"/>
          </svg>
        </div>
        <p class="text-sm md:text-base font-medium text-[var(--zh-text-secondary)] mb-1">未找到相关结果</p>
        <p class="text-xs md:text-sm text-[var(--zh-text-tertiary)]">换个关键词试试吧</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
/** 搜索页：搜索框、搜索建议、搜索历史、热门搜索、综合搜索结果展示 */
import type { Article, User, Category } from '@/types'
import type { SuggestionItem, SearchParams } from '@/api/search'
import { sanitizeHtml } from '@/utils/sanitize'

const route = useRoute()
const router = useRouter()
const { resolveUrl } = useResourceUrl()
const userStore = useUserStore()

// 返回上一页
const goBack = () => {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push('/')
  }
}

// 请求缓存：搜索建议用短TTL，热门搜索和分类用长TTL
const { cachedRequest: cachedRequestShort } = useRequestCache({ ttl: 2 * 60 * 1000 })
const { cachedRequest: cachedRequestLong } = useRequestCache({ ttl: 10 * 60 * 1000 })

// 搜索Tab
const searchTabs = [
  { key: 'all', label: computed(() => '综合') },
  { key: 'articles', label: computed(() => '作品') },
  { key: 'users', label: computed(() => '用户') },
  { key: 'images', label: computed(() => '图片') },
]

const keyword = ref((route.query.keyword as string) || '')
const activeTab = ref('all')
const sortBy = ref('relevance')
const filterCategoryId = ref<number | undefined>(undefined)
const filterTimeRange = ref<'24h' | '7d' | '30d' | undefined>(undefined)
const hasSearched = ref(false)

// Tab滑动指示器样式
const tabIndicatorStyle = computed(() => {
  const tabIndex = searchTabs.findIndex((t) => t.key === activeTab.value)
  const tabWidth = 100 / searchTabs.length
  return {
    width: `${tabWidth}%`,
    transform: `translateX(${tabIndex * 100}%)`,
  }
})
const loading = ref(false)
const loadingMore = ref(false)
const searchError = ref('')
const suggestions = ref<SuggestionItem[]>([])
const hotSearches = ref<string[]>([])
const searchHistory = ref<string[]>([])
const categories = ref<Category[]>([])

// 搜索结果
const articleResults = ref<Article[]>([])
const userResults = ref<User[]>([])
const imageResults = ref<{ url: string; title: string; articleTitle?: string; author?: string }[]>([])

// 综合Tab截取的展示数据
const allArticleResults = computed(() => articleResults.value.slice(0, 5))
const allUserResults = computed(() => userResults.value.slice(0, 3))
const allImageResults = computed(() => imageResults.value.slice(0, 6))

// 各Tab计数（综合搜索时从后端返回的total取）
const tabCounts = ref<Record<string, number>>({ all: 0, articles: 0, users: 0, images: 0 })

// 分页
const currentPage = ref(1)
const pageSize = 20
const totalResults = ref(0)

// 无限滚动sentinel
const articleSentinel = ref<HTMLElement | null>(null)
const userSentinel = ref<HTMLElement | null>(null)
const imageSentinel = ref<HTMLElement | null>(null)
const searchInputRef = ref<HTMLInputElement | null>(null)
const searchInputDesktopRef = ref<HTMLInputElement | null>(null)
const mobileSearchFocused = ref(false)
const desktopSearchFocused = ref(false)

// 是否有更多数据
const hasMoreArticles = computed(() => articleResults.value.length < tabCounts.value.articles)
const hasMoreUsers = computed(() => userResults.value.length < tabCounts.value.users)
const hasMoreImages = computed(() => imageResults.value.length < tabCounts.value.images)

// 是否有任何结果
const hasAnyResult = computed(() => {
  if (activeTab.value === 'all') {
    return articleResults.value.length > 0 || userResults.value.length > 0 || imageResults.value.length > 0
  }
  if (activeTab.value === 'articles') return articleResults.value.length > 0
  if (activeTab.value === 'users') return userResults.value.length > 0
  if (activeTab.value === 'images') return imageResults.value.length > 0
  return false
})

// 防抖定时器
let suggestTimer: ReturnType<typeof setTimeout> | null = null
let autoSearchTimer: ReturnType<typeof setTimeout> | null = null
// AbortController 取消进行中的请求
let abortController: AbortController | null = null

// IntersectionObserver实例
let articleObserver: IntersectionObserver | null = null
let userObserver: IntersectionObserver | null = null
let imageObserver: IntersectionObserver | null = null

// 自动搜索中指示
const autoSearching = ref(false)

// 从URL参数初始化搜索
onMounted(async () => {
  if (keyword.value) {
    doSearch()
  }
  // 自动聚焦（根据屏幕宽度选择正确的输入框）
  nextTick(() => {
    const isMobile = window.innerWidth < 768
    if (isMobile) {
      searchInputRef.value?.focus()
    } else {
      searchInputDesktopRef.value?.focus()
    }
  })

  // 加载热门搜索
  loadHotSearches()
  // 加载搜索历史
  loadSearchHistory()
  // 加载分类列表
  loadCategories()
})

onUnmounted(() => {
  articleObserver?.disconnect()
  userObserver?.disconnect()
  imageObserver?.disconnect()
})

// 输入处理：建议快速展示200ms，自动搜索防抖500ms
const handleInput = () => {
  const trimmed = keyword.value.trim()

  // 清除旧的定时器
  if (suggestTimer) clearTimeout(suggestTimer)
  if (autoSearchTimer) clearTimeout(autoSearchTimer)

  // 取消进行中的搜索请求
  if (abortController) {
    abortController.abort()
    abortController = null
  }

  if (!trimmed) {
    suggestions.value = []
    autoSearching.value = false
    return
  }

  // 搜索建议（短延迟，快速展示）
  suggestTimer = setTimeout(async () => {
    try {
      const { searchApi } = await import('~/api')
      const response = await cachedRequestShort(
        () => searchApi.getSuggestions(trimmed),
        '/search/suggestions',
        { keyword: trimmed }
      )
      const data = response.data.data
      suggestions.value = data?.completions || []
    } catch {
      suggestions.value = []
    }
  }, 200)

  // 自动搜索（较长延迟，不为空即触发）
  autoSearchTimer = setTimeout(() => {
    autoSearching.value = true
    doAutoSearch()
  }, 500)
}

// 自动搜索（带 AbortController 取消机制）
const doAutoSearch = async () => {
  const trimmed = keyword.value.trim()
  if (!trimmed) {
    autoSearching.value = false
    return
  }

  // 创建新的 AbortController
  abortController = new AbortController()
  const signal = abortController.signal

  try {
    const { searchApi } = await import('~/api')
    const params: SearchParams = {
      page: 1,
      pageSize,
      sort: sortBy.value as SearchParams['sort'],
      categoryId: filterCategoryId.value,
      timeRange: filterTimeRange.value,
    }

    // 检查是否已被取消
    if (signal.aborted) return

    hasSearched.value = true
    loading.value = true
    searchError.value = ''
    currentPage.value = 1
    articleResults.value = []
    userResults.value = []
    imageResults.value = []
    tabCounts.value = { all: 0, articles: 0, users: 0, images: 0 }

    saveHistory(trimmed)

    const response = await searchApi.search(trimmed, activeTab.value as any, params)

    // 请求完成后检查是否已被取消
    if (signal.aborted) return

    const result = response.data.data

    if (activeTab.value === 'all') {
      articleResults.value = (result.articles || []) as Article[]
      userResults.value = (result.users || []) as User[]
      imageResults.value = (result.images || []).map((img: any) => ({
        url: resolveUrl(img.coverImage) || '',
        title: img.title || '',
        articleTitle: img.title || '',
        author: img.authorName || img.author?.nickname || '',
      }))
      tabCounts.value.articles = result.articleTotal ?? articleResults.value.length
      tabCounts.value.users = result.userTotal ?? userResults.value.length
      tabCounts.value.images = result.imageTotal ?? imageResults.value.length
      tabCounts.value.all = tabCounts.value.articles + tabCounts.value.users + tabCounts.value.images
    } else if (activeTab.value === 'articles') {
      articleResults.value = (result.articles || []) as Article[]
      tabCounts.value.articles = result.total || articleResults.value.length
    } else if (activeTab.value === 'users') {
      userResults.value = (result.users || []) as User[]
      tabCounts.value.users = result.total || userResults.value.length
    } else if (activeTab.value === 'images') {
      imageResults.value = (result.images || []).map((img: any) => ({
        url: resolveUrl(img.coverImage) || '',
        title: img.title || '',
        articleTitle: img.title || '',
        author: img.authorName || img.author?.nickname || '',
      }))
      tabCounts.value.images = result.total || imageResults.value.length
    }

    nextTick(() => setupInfiniteScroll())
    router.replace({ query: { keyword: trimmed } })
  } catch (err: any) {
    if (signal.aborted) return
    articleResults.value = []
    userResults.value = []
    imageResults.value = []
    // 如果是自动搜索触发的，不显示错误提示（用户可能还在输入）
  } finally {
    if (!signal.aborted) {
      loading.value = false
      autoSearching.value = false
    }
  }
}

// 执行搜索
const doSearch = async (loadMore = false) => {
  if (!keyword.value.trim()) return
  hasSearched.value = true

  // 取消进行中的自动搜索
  if (!loadMore && abortController) {
    abortController.abort()
    abortController = null
  }
  if (!loadMore && autoSearchTimer) {
    clearTimeout(autoSearchTimer)
    autoSearching.value = false
  }
  if (!loadMore && suggestTimer) {
    clearTimeout(suggestTimer)
  }

  if (!loadMore) {
    loading.value = true
    currentPage.value = 1
    articleResults.value = []
    userResults.value = []
    imageResults.value = []
    tabCounts.value = { all: 0, articles: 0, users: 0, images: 0 }
    searchError.value = ''
  } else {
    loadingMore.value = true
    currentPage.value++
  }

  saveHistory(keyword.value.trim())

  try {
    const { searchApi } = await import('~/api')
    const params: SearchParams = {
      page: currentPage.value,
      pageSize,
      sort: sortBy.value as SearchParams['sort'],
      categoryId: filterCategoryId.value,
      timeRange: filterTimeRange.value,
    }

    const response = await searchApi.search(keyword.value.trim(), activeTab.value as any, params)
    const result = response.data.data

    // 更新计数 & 填充结果
    if (activeTab.value === 'all') {
      // 综合搜索：所有结果一次性返回，使用后端返回的各分类总数
      articleResults.value = (result.articles || []) as Article[]
      userResults.value = (result.users || []) as User[]
      imageResults.value = (result.images || []).map((img: any) => ({
        url: resolveUrl(img.coverImage) || '',
        title: img.title || '',
        articleTitle: img.title || '',
        author: img.authorName || img.author?.nickname || '',
      }))
      tabCounts.value.articles = result.articleTotal ?? articleResults.value.length
      tabCounts.value.users = result.userTotal ?? userResults.value.length
      tabCounts.value.images = result.imageTotal ?? imageResults.value.length
      tabCounts.value.all = tabCounts.value.articles + tabCounts.value.users + tabCounts.value.images
    } else if (activeTab.value === 'articles') {
      const newArticles = (result.articles || []) as Article[]
      if (loadMore) {
        articleResults.value = [...articleResults.value, ...newArticles]
      } else {
        articleResults.value = newArticles
      }
      tabCounts.value.articles = result.total || articleResults.value.length
    } else if (activeTab.value === 'users') {
      const newUsers = (result.users || []) as User[]
      if (loadMore) {
        userResults.value = [...userResults.value, ...newUsers]
      } else {
        userResults.value = newUsers
      }
      tabCounts.value.users = result.total || userResults.value.length
    } else if (activeTab.value === 'images') {
      const newImages = (result.images || []).map((img: any) => ({
        url: resolveUrl(img.coverImage) || '',
        title: img.title || '',
        articleTitle: img.title || '',
        author: img.authorName || img.author?.nickname || '',
      }))
      if (loadMore) {
        imageResults.value = [...imageResults.value, ...newImages]
      } else {
        imageResults.value = newImages
      }
      tabCounts.value.images = result.total || imageResults.value.length
    }

    // 设置无限滚动观察器
    nextTick(() => {
      setupInfiniteScroll()
    })
  } catch {
    if (!loadMore) {
      articleResults.value = []
      userResults.value = []
      imageResults.value = []
      searchError.value = '搜索失败，请检查网络后重试'
    }
  } finally {
    loading.value = false
    loadingMore.value = false
  }

  // 更新URL
  router.replace({ query: { keyword: keyword.value.trim() } })
}

// 重试搜索
const retrySearch = async () => {
  searchError.value = ''
  await doSearch()
}

// 设置无限滚动
const setupInfiniteScroll = () => {
  // 断开旧观察器
  articleObserver?.disconnect()
  userObserver?.disconnect()
  imageObserver?.disconnect()

  const observerCallback = (entries: IntersectionObserverEntry[]) => {
    if (entries[0]?.isIntersecting && !loadingMore.value) {
      doSearch(true)
    }
  }

  if (activeTab.value === 'articles' && articleSentinel.value && hasMoreArticles.value) {
    articleObserver = new IntersectionObserver(observerCallback, { rootMargin: '200px' })
    articleObserver.observe(articleSentinel.value)
  }
  if (activeTab.value === 'users' && userSentinel.value && hasMoreUsers.value) {
    userObserver = new IntersectionObserver(observerCallback, { rootMargin: '200px' })
    userObserver.observe(userSentinel.value)
  }
  if (activeTab.value === 'images' && imageSentinel.value && hasMoreImages.value) {
    imageObserver = new IntersectionObserver(observerCallback, { rootMargin: '200px' })
    imageObserver.observe(imageSentinel.value)
  }
}

// 切换Tab
const switchTab = (key: string) => {
  activeTab.value = key
  doSearch()
}

// 选择搜索建议
const selectSuggestion = (item: SuggestionItem) => {
  keyword.value = item.text
  doSearch()
}

// 清除关键词
const clearKeyword = () => {
  keyword.value = ''
  suggestions.value = []
  hasSearched.value = false
  autoSearching.value = false
  if (suggestTimer) clearTimeout(suggestTimer)
  if (autoSearchTimer) clearTimeout(autoSearchTimer)
  if (abortController) {
    abortController.abort()
    abortController = null
  }
}

// 关键词高亮（使用 DOMPurify 净化）
const highlightKeyword = (text: string) => {
  if (!keyword.value.trim()) return sanitizeHtml(text)
  // 先净化基础内容，然后添加高亮标记
  const sanitized = sanitizeHtml(text)
  const kw = keyword.value.trim()
  const regex = new RegExp(`(${kw.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')})`, 'gi')
  // 高亮标记 <span> 是安全的，DOMPurify 已允许
  return sanitized.replace(regex, '<span class="text-primary font-medium">$1</span>')
}

// 搜索历史管理（使用统一 composable）
const { getHistory, addHistory, removeHistory: removeSearchHistory, clearHistory: clearSearchHistory } = useSearchHistory()

// 保存搜索历史
const saveHistory = (kw: string) => {
  addHistory(kw)
  // 同步更新本地显示列表
  searchHistory.value = getHistory().map((e) => e.keyword)
}

// 清除搜索历史
const clearHistory = () => {
  clearSearchHistory()
  searchHistory.value = []
}

// 删除单个搜索历史
const removeHistoryItem = (kw: string) => {
  removeSearchHistory(kw)
  searchHistory.value = getHistory().map((e) => e.keyword)
}

// 加载热门搜索
// 注意：使用 forceRequest 避免 useRequestCache 中持久化的 stale 缓存（空值）污染首屏
const loadHotSearches = async () => {
  try {
    const { searchApi } = await import('~/api')
    const response = await cachedRequestLong(
      () => searchApi.getHotSearches(),
      '/search/hot',
      undefined,
      { force: true } as any
    )
    // 后端返回 data 是 string[]，包装在 R.ok 中
    const data = Array.isArray(response?.data) ? response.data : (response?.data?.data ?? [])
    hotSearches.value = Array.isArray(data) ? data : []
  } catch (e) {
    console.warn('[search] 加载热门搜索失败:', e)
    hotSearches.value = []
  }
}

// 加载搜索历史（从统一 composable 读取）
const loadSearchHistory = () => {
  searchHistory.value = getHistory().map((e) => e.keyword)
}

// 加载分类列表
const loadCategories = async () => {
  try {
    const { get } = useApi()
    const response = await cachedRequestLong(
      () => get<Category[]>('/categories'),
      '/categories',
      undefined,
      { force: true } as any
    )
    const data = response?.data?.data ?? response?.data ?? []
    categories.value = Array.isArray(data) ? data : []
  } catch (e) {
    console.warn('[search] 加载分类列表失败:', e)
    categories.value = []
  }
}

// 关注/取关用户
const toggleFollow = async (userId: number) => {
  if (!userStore.isLoggedIn) {
    router.push('/login')
    return
  }
  try {
    const { socialApi } = await import('~/api')
    const response = await socialApi.toggleFollow(userId)
    const result = response.data.data
    // 更新本地状态
    const user = userResults.value.find((u) => u.id === userId)
    if (user) {
      user.isFollowing = result.followed
      user.followerCount = result.followerCount
    }
  } catch (error: any) {
    console.error('关注操作失败，请稍后重试')
  }
}

// 图片点击
const handleImageClick = (image: any) => {
  // 可根据需要跳转到作品详情
}

// 页面元信息
useHead({
  title: () => keyword.value ? `${'搜索'} "${keyword.value}" - 知讯` : `${'搜索'} - 知讯`,
})
</script>

<style scoped>
/* ==========================================================================
   搜索页 - 现代化样式优化
   使用项目现有CSS变量（--zh-*）
   ========================================================================== */

/* ---- 页面进入动画 ---- */
.search-page {
  animation: search-page-enter 0.45s cubic-bezier(0.22, 1, 0.36, 1) both;
}

@keyframes search-page-enter {
  from {
    opacity: 0;
    transform: translateY(16px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* ---- 搜索框渐变聚焦环（移动端） ---- */
.search-box-mobile {
  position: relative;
  transition: border-color var(--zh-transition-base), box-shadow var(--zh-transition-base);
}

.search-box-mobile--focused {
  background: var(--zh-bg-elevated);
}

/* 清除按钮 hover 效果 */
.search-box-mobile__clear:hover {
  color: var(--zh-text);
  transform: scale(1.1);
}

/* ---- 搜索框渐变聚焦环（桌面端） ---- */
.search-box-desktop {
  position: relative;
  transition: border-color var(--zh-transition-base), box-shadow var(--zh-transition-base);
}

.search-box-desktop--focused {
  background: var(--zh-bg-elevated);
}

.search-box-desktop__clear:hover {
  color: var(--zh-text);
  transform: scale(1.1);
}

/* 搜索按钮渐变 */
.search-btn {
  background: var(--zh-primary-gradient);
  transition: transform var(--zh-transition-fast), box-shadow var(--zh-transition-fast), opacity var(--zh-transition-fast);
}

.search-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(var(--zh-primary-rgb), 0.35);
}

.search-btn:active {
  transform: translateY(0) scale(0.97);
}

/* ---- 搜索建议项 ---- */
.suggest-item {
  transition: background var(--zh-transition-fast), padding-left var(--zh-transition-fast);
  position: relative;
}

.suggest-item:hover {
  background: var(--zh-bg-hover);
  padding-left: 1.25rem;
}

@media (min-width: 768px) {
  .suggest-item:hover {
    padding-left: 1.5rem;
  }
}

/* 建议项左侧类型图标容器 */
.suggest-item__icon {
  background: var(--zh-bg-hover);
  transition: background var(--zh-transition-fast), transform var(--zh-transition-fast);
}

.suggest-item:hover .suggest-item__icon {
  transform: scale(1.1);
}

.suggest-item__icon--user {
  background: rgba(var(--zh-primary-rgb), 0.08);
  color: var(--zh-primary);
}

.suggest-item__icon--article {
  background: rgba(16, 185, 129, 0.08);
  color: var(--zh-success);
}

/* 标签类型默认 */
.suggest-item__icon:not(.suggest-item__icon--user):not(.suggest-item__icon--article) {
  background: rgba(245, 158, 11, 0.08);
  color: var(--zh-warning);
}

/* ---- 搜索历史标签 ---- */
.history-tag {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  background: var(--zh-bg-hover);
  border: 1px solid var(--zh-border);
  border-radius: var(--zh-radius-full);
  overflow: hidden;
  transition: background var(--zh-transition-fast), border-color var(--zh-transition-fast), box-shadow var(--zh-transition-fast);
}

.history-tag:hover {
  background: var(--zh-bg-active);
  border-color: var(--zh-border-focus);
  box-shadow: var(--zh-shadow-xs);
}

.history-tag__text {
  padding: 0.25rem 0.3rem 0.25rem 0.75rem;
  font-size: 0.6875rem;
  color: var(--zh-text-secondary);
  background: none;
  border: none;
  cursor: pointer;
  white-space: nowrap;
  transition: color var(--zh-transition-fast);
  line-height: 1.4;
}

.history-tag:hover .history-tag__text {
  color: var(--zh-text);
}

.history-tag__delete {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 1.25rem;
  height: 1.25rem;
  margin-right: 0.2rem;
  border-radius: var(--zh-radius-full);
  border: none;
  background: transparent;
  color: var(--zh-text-tertiary);
  cursor: pointer;
  opacity: 0;
  transform: scale(0.8);
  transition: all var(--zh-transition-fast);
}

.history-tag:hover .history-tag__delete {
  opacity: 1;
  transform: scale(1);
}

.history-tag__delete:hover {
  background: rgba(239, 68, 68, 0.1);
  color: var(--zh-danger);
}

@media (min-width: 768px) {
  .history-tag__text {
    padding: 0.35rem 0.4rem 0.35rem 0.9rem;
    font-size: 0.8125rem;
  }
  .history-tag__delete {
    width: 1.35rem;
    height: 1.35rem;
    margin-right: 0.25rem;
  }
}

/* ---- 热门搜索项 ---- */
.hot-item {
  transition: background var(--zh-transition-fast), padding-left var(--zh-transition-fast);
}

.hot-item:hover {
  background: var(--zh-bg-hover);
  padding-left: 1.25rem;
}

@media (min-width: 768px) {
  .hot-item:hover {
    padding-left: 1.5rem;
  }
}

/* 热门排名徽章 */
.hot-rank {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 1.25rem;
  height: 1.25rem;
  padding: 0 0.3rem;
  font-size: 0.625rem;
  line-height: 1;
  color: #fff;
}

@media (min-width: 768px) {
  .hot-rank {
    min-width: 1.5rem;
    height: 1.5rem;
    font-size: 0.6875rem;
  }
}

/* 第1名：金色渐变 */
.hot-rank--1 {
  background: linear-gradient(135deg, #f59e0b 0%, #d97706 50%, #b45309 100%);
  box-shadow: 0 2px 8px rgba(245, 158, 11, 0.35);
}

/* 第2名：银色渐变 */
.hot-rank--2 {
  background: linear-gradient(135deg, #94a3b8 0%, #64748b 50%, #475569 100%);
  box-shadow: 0 2px 8px rgba(148, 163, 184, 0.35);
}

/* 第3名：铜色渐变 */
.hot-rank--3 {
  background: linear-gradient(135deg, #d97706 0%, #b45309 50%, #92400e 100%);
  box-shadow: 0 2px 8px rgba(217, 119, 6, 0.3);
}

/* 其余排名：统一灰色 */
.hot-rank--normal {
  background: var(--zh-bg-active);
  color: var(--zh-text-tertiary);
  font-weight: 600;
}

/* ---- Tab切换栏 + 滑动指示器 ---- */
.search-tabs {
  position: relative;
}

.search-tabs__btn {
  position: relative;
  flex: 1;
  text-align: center;
  white-space: nowrap;
}

.search-tabs__indicator {
  position: absolute;
  bottom: -1px;
  left: 0;
  height: 2.5px;
  background: var(--zh-primary-gradient);
  border-radius: 9999px 9999px 0 0;
  transition: transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}

/* ---- 筛选栏 ---- */
.filter-bar {
  gap: 0.375rem;
}

.filter-select {
  appearance: none;
  -webkit-appearance: none;
  height: 2rem;
  padding: 0 1.75rem 0 0.6rem;
  font-size: 0.6875rem;
  font-family: inherit;
  color: var(--zh-text-secondary);
  background-color: var(--zh-bg-elevated);
  border: 1px solid var(--zh-border);
  border-radius: var(--zh-radius-sm);
  cursor: pointer;
  outline: none;
  transition: border-color var(--zh-transition-fast), box-shadow var(--zh-transition-fast), background var(--zh-transition-fast);
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 24 24' fill='none' stroke='%2394a3b8' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='m6 9 6 6 6-6'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 0.4rem center;
  min-width: 4.5rem;
}

.filter-select:hover {
  border-color: var(--zh-border-focus);
  background-color: var(--zh-bg-hover);
}

.filter-select:focus {
  border-color: var(--zh-primary);
  box-shadow: 0 0 0 2px rgba(var(--zh-primary-rgb), 0.1);
}

@media (min-width: 768px) {
  .filter-select {
    height: 2.25rem;
    padding: 0 2rem 0 0.75rem;
    font-size: 0.75rem;
    background-position: right 0.5rem center;
    min-width: 5rem;
  }
}

/* 暗色模式 */
:global(.dark) .filter-select {
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 24 24' fill='none' stroke='%2364748b' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='m6 9 6 6 6-6'/%3E%3C/svg%3E");
}

/* ---- 搜索结果卡片 ---- */
.result-list {
  /* 卡片间距通过 space-y 控制，额外添加一些视觉呼吸感 */
}

/* 给结果列表中的卡片添加微妙的hover提升效果 */
.result-list :deep(.card-base),
.result-list :deep(.article-card) {
  transition: transform var(--zh-transition-fast), box-shadow var(--zh-transition-fast);
}

.result-list :deep(.card-base):hover,
.result-list :deep(.article-card):hover {
  transform: translateY(-1px);
  box-shadow: var(--zh-shadow-md);
}

/* ---- 空结果状态 ---- */
.search-empty {
  animation: search-empty-enter 0.5s cubic-bezier(0.22, 1, 0.36, 1) both;
}

@keyframes search-empty-enter {
  from {
    opacity: 0;
    transform: scale(0.95) translateY(10px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

.search-empty__illustration {
  animation: search-empty-float 3s ease-in-out infinite;
}

@keyframes search-empty-float {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-6px);
  }
}

/* ---- 响应式移动端适配 ---- */
@media (max-width: 767.98px) {
  .search-tabs__btn {
    font-size: 0.6875rem;
    padding: 0.5rem 0.5rem;
  }

  .search-tabs__indicator {
    height: 2px;
  }

  .filter-select {
    height: 1.75rem;
    font-size: 0.625rem;
    padding: 0 1.5rem 0 0.5rem;
    min-width: 3.75rem;
  }
}

/* ---- 暗色模式适配 ---- */
:global(.dark) .history-tag {
  background: var(--zh-bg-hover);
  border-color: var(--zh-border);
}

:global(.dark) .history-tag:hover {
  background: var(--zh-bg-active);
  border-color: rgba(var(--zh-primary-rgb), 0.3);
}

:global(.dark) .hot-rank--normal {
  background: var(--zh-bg-active);
  color: var(--zh-text-tertiary);
}

:global(.dark) .filter-select {
  background-color: var(--zh-bg-elevated);
  border-color: var(--zh-border);
}

:global(.dark) .filter-select:hover {
  border-color: rgba(var(--zh-primary-rgb), 0.3);
}
</style>
