<template>
  <!-- 消息与通知页面 - QQ风格 -->
  <!-- v17: 减去底部 MobileNav 高度（约 56px），避免被底部 Tab 栏遮挡 -->
  <div class="h-[calc(100dvh-3.75rem-3.5rem)] flex flex-col">
    <!-- 顶部Tab栏 -->
    <div class="flex items-center border-b border-[var(--zh-border)] bg-[var(--zh-bg-elevated)] shrink-0">
      <button v-if="activeMainTab === 'messages' && activeConversation" class="md:hidden p-1.5 text-[var(--zh-text-secondary)] hover:bg-[var(--zh-bg-hover)] rounded-lg mr-1 shrink-0" @click="activeConversation = null">
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" /></svg>
      </button>
      <button
        v-for="tab in mainTabs"
        :key="tab.key"
        class="flex-1 py-2.5 text-sm font-medium text-center transition-colors relative"
        :class="activeMainTab === tab.key ? 'text-primary' : 'text-[var(--zh-text-secondary)] hover:text-[var(--zh-text-secondary)]'"
        @click="switchMainTab(tab.key)"
      >
        <span>{{ tab.label }}</span>
        <span v-if="tab.unread > 0" class="ml-1 inline-flex items-center justify-center min-w-[1.125rem] h-4.5 bg-danger text-white text-2xs rounded-full px-1.5">{{ tab.unread > 99 ? '99+' : tab.unread }}</span>
        <div v-if="activeMainTab === tab.key" class="absolute bottom-0 left-1/2 -translate-x-1/2 w-8 h-0.5 bg-primary rounded-full" />
      </button>
    </div>

    <!-- ===== 私信Tab ===== -->
    <template v-if="activeMainTab === 'messages'">
      <!-- v16: min-h-0 防止 flex 子项被内容撑开，确保底部输入区固定 -->
      <div class="flex-1 flex overflow-hidden min-h-0">
        <!-- 左侧会话列表 -->
        <div class="w-full md:w-80 border-r border-[var(--zh-border)] flex flex-col shrink-0" :class="{ 'hidden md:flex': activeConversation }">
          <div class="p-3 border-b border-[var(--zh-border)]">
            <input v-model="conversationSearch" type="text" class="input text-sm bg-[var(--zh-bg-hover)]" placeholder="搜索会话..." />
          </div>
          <div class="flex-1 overflow-y-auto">
            <div v-if="loadingConv" class="flex items-center justify-center py-16">
              <div class="w-6 h-6 border-2 border-primary border-t-transparent rounded-full animate-spin"></div>
            </div>
            <ErrorRetry v-else-if="conversationsError" :message="conversationsError" :on-retry="loadConversations" />
            <template v-else>
              <div
                v-for="conv in filteredConversations"
                :key="conv.id"
                role="button"
                tabindex="0"
                class="w-full flex items-center gap-3 px-3 py-3 hover:bg-[var(--zh-bg-hover)] transition-colors text-left cursor-pointer"
                :class="{ 'bg-primary-50/50': activeConversation?.id === conv.id }"
                @click="selectConversation(conv)"
                @keydown.enter="selectConversation(conv)"
                @keydown.space.prevent="selectConversation(conv)"
              >
                <div
                  role="button"
                  tabindex="0"
                  class="relative shrink-0 rounded-full hover:opacity-80 transition-opacity"
                  @click.stop="navigateToUser(conv.user?.id)"
                  @keydown.enter.stop="navigateToUser(conv.user?.id)"
                >
                  <UserAvatar :src="conv.user?.avatar" alt="" size="lg" />
                  <span v-if="conv.user?.isOnline" class="absolute bottom-0 right-0 w-2.5 h-2.5 bg-green-500 rounded-full border-2 border-white" />
                </div>
                <div class="flex-1 min-w-0">
                  <div class="flex items-center justify-between">
                    <span class="text-sm font-medium text-[var(--zh-text)] truncate">{{ conv.user?.nickname }}</span>
                    <span class="text-2xs text-[var(--zh-text-tertiary)] shrink-0 ml-1">{{ formatConvTime(conv.updatedAt) }}</span>
                  </div>
                  <p class="text-xs text-[var(--zh-text-secondary)] truncate mt-0.5">{{ getConvLastMessage(conv) }}</p>
                </div>
                <span v-if="conv.unreadCount > 0" class="w-5 h-5 bg-danger text-white text-2xs rounded-full flex items-center justify-center shrink-0">
                  {{ conv.unreadCount > 99 ? '99+' : conv.unreadCount }}
                </span>
              </div>
              <EmptyState v-if="!loadingConv && conversations.length === 0" title="暂无会话" description="开始一段新的对话吧" />
            </template>
          </div>
        </div>

        <!-- 右侧聊天区域 - v16: h-full + min-h-0 约束高度，固定底部输入区 -->
        <div class="flex-1 flex flex-col min-w-0 h-full min-h-0" :class="{ 'hidden md:flex': !activeConversation }">
          <template v-if="activeConversation">
            <!-- 聊天头部 -->
            <div class="flex items-center gap-1.5 px-2 py-1 border-b border-[var(--zh-border)] bg-[var(--zh-bg-elevated)] shrink-0 sticky top-0 z-10">
              <button class="shrink-0 rounded-full hover:opacity-80 transition-opacity" @click="navigateToUser(activeConversation.user?.id)">
                <UserAvatar :src="activeConversation.user?.avatar" alt="" size="sm" />
              </button>
              <div class="min-w-0">
                <p class="font-medium text-[var(--zh-text)] text-xs leading-tight truncate">{{ activeConversation.user?.nickname }}</p>
                <p class="text-[10px] leading-tight" :class="activeConversation.user?.isOnline ? 'text-green-500' : 'text-[var(--zh-text-tertiary)]'">{{ activeConversation.user?.isOnline ? '在线' : '离线' }}</p>
              </div>
            </div>
            <!-- 消息列表 -->
            <div ref="msgListRef" class="flex-1 overflow-y-auto px-3 py-3 space-y-2 bg-[var(--zh-bg-hover)]">
              <div class="text-center text-2xs text-[var(--zh-text-tertiary)] mb-2" v-if="messages.length > 0">以下为私信内容</div>
              <div v-for="msg in messages" :key="msg.id" class="flex" :class="isMyMsg(msg) ? 'justify-end' : 'justify-start'">
                <!-- 对方消息（左） - 2026-07-02 v11: 改用 ChatBubble 统一处理 voice/file/image/text -->
                <div v-if="!isMyMsg(msg)" class="flex items-start gap-1.5 max-w-[80%]">
                  <button class="shrink-0 rounded-full hover:opacity-80 transition-opacity" @click="navigateToUser(msg.sender?.id || msg.senderId)">
                    <UserAvatar :src="msg.sender?.avatar" alt="" size="sm" />
                  </button>
                  <div class="min-w-0 flex-1">
                    <ChatBubble
                      :content="msg.content"
                      :message-type="msg.type"
                      :is-mine="false"
                      :time="formatMsgTime(msg.createdAt)"
                      @preview-image="onNotiImagePreview"
                    />
                  </div>
                </div>
                <!-- 我的消息（右） -->
                <div v-else class="flex items-start gap-1.5 max-w-[80%]">
                  <div class="min-w-0 flex-1 flex flex-col items-end">
                    <ChatBubble
                      :content="msg.content"
                      :message-type="msg.type"
                      :is-mine="true"
                      :time="formatMsgTime(msg.createdAt)"
                      @preview-image="onNotiImagePreview"
                    />
                  </div>
                  <button class="shrink-0 rounded-full hover:opacity-80 transition-opacity" @click="navigateToUser(userStore.userInfo?.id)">
                    <UserAvatar :src="userStore.userInfo?.avatar" alt="" size="sm" />
                  </button>
                </div>
              </div>
              <div v-if="messages.length === 0" class="flex flex-col items-center justify-center py-16 text-center">
                <svg class="w-12 h-12 text-slate-300 mb-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
                </svg>
                <p class="text-xs text-[var(--zh-text-tertiary)]">还没有消息，发送第一条吧</p>
              </div>
              <!-- v15：AI 助手思考中动画（与群组页 GroupChatWindow 一致） -->
              <div v-if="notiAiReplying" class="flex justify-start">
                <div class="flex items-start gap-1.5 max-w-[80%]">
                  <img :src="AI_AVATAR_URL" class="w-7 h-7 rounded-full shrink-0" alt="AI助手" />
                  <div class="ai-thinking-bubble">
                    <span class="ai-thinking-label">AI助手</span>
                    <span class="ai-thinking-text">正在思考</span>
                    <span class="ai-thinking-dots">
                      <span class="dot"></span>
                      <span class="dot"></span>
                      <span class="dot"></span>
                    </span>
                  </div>
                </div>
              </div>
            </div>
            <!-- 输入区 -->
            <div class="noti-chat-input">
              <!-- 工具栏 -->
              <ChatToolbar
                :ai-mode="notiAiMode"
                :is-recording="notiVoiceRecorder.isRecording"
                @emoji="(emoji: string) => { inputContent += emoji }"
                @image="notiImageInputRef?.click()"
                @file="notiFileInputRef?.click()"
                @voice="startNotiVoiceRecord"
                @ai="notiAiMode = !notiAiMode"
              />
              <!-- 语音上传中 -->
              <div v-if="notiVoiceUploading" class="noti-voice-uploading">
                <div class="w-3 h-3 border-2 border-blue-500 border-t-transparent rounded-full animate-spin"></div>
                <span>语音上传中...</span>
              </div>
              <!-- 语音录制中 - 替换输入框 -->
              <VoiceRecordingBar
                v-if="notiVoiceRecorder.isRecording"
                :recording-time="notiVoiceRecorder.recordingTime"
                @finish="finishNotiVoiceRecord"
                @cancel="notiVoiceRecorder.cancelRecording()"
              />
              <!-- 输入框 + 发送 -->
              <div v-else class="noti-input-row">
                <div class="noti-input-wrap" :class="{ focused: notiInputFocused }">
                  <textarea
                    v-model="inputContent"
                    class="noti-textarea"
                    :placeholder="notiAiMode ? '向AI助手提问...' : '输入消息...'"
                    rows="1"
                    @focus="notiInputFocused = true"
                    @blur="notiInputFocused = false"
                    @keydown.enter.exact.prevent="sendMessage"
                  ></textarea>
                </div>
                <button
                  class="noti-send-btn"
                  :disabled="!inputContent.trim() || sendingMessage"
                  @click="sendMessage"
                  aria-label="发送"
                >
                  <svg class="w-5 h-5" fill="currentColor" viewBox="0 0 24 24">
                    <path d="M2.01 21L23 12 2.01 3 2 10l15 2-15 2z"/>
                  </svg>
                </button>
              </div>
            </div>
            <!-- 隐藏的 file inputs -->
            <input ref="notiImageInputRef" type="file" accept="image/*" style="display:none" @change="onNotiImageSelected" />
            <input ref="notiFileInputRef" type="file" style="display:none" @change="onNotiFileSelected" />
            <!-- v11 新增：图片预览弹窗 -->
            <Teleport to="body">
              <div v-if="notiPreviewImage" class="fixed inset-0 z-[9999] bg-black/90 flex items-center justify-center p-4" @click="closeNotiImagePreview">
                <img :src="notiPreviewImage" alt="预览" class="max-w-full max-h-full object-contain" @click.stop />
                <button class="absolute top-4 right-4 w-10 h-10 rounded-full bg-white/10 hover:bg-white/20 text-white text-xl flex items-center justify-center" @click="closeNotiImagePreview" aria-label="关闭">×</button>
              </div>
            </Teleport>
          </template>
          <!-- 空状态 -->
          <div v-else class="flex-1 flex items-center justify-center bg-[var(--zh-bg-hover)]">
            <div class="text-center">
              <svg class="w-16 h-16 text-slate-300 mx-auto mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
              </svg>
              <p class="text-[var(--zh-text-secondary)] text-sm">选择一个会话开始聊天</p>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- ===== 群组Tab ===== -->
    <template v-if="activeMainTab === 'groups'">
      <div class="flex-1 flex flex-col overflow-hidden">
        <!-- 群组内部操作栏 -->
        <div class="flex items-center justify-between px-4 py-2 border-b border-[var(--zh-border)] bg-[var(--zh-bg-elevated)] shrink-0">
          <div class="flex items-center gap-3">
            <button
              class="text-sm font-semibold transition-colors relative pb-0.5"
              :class="groupActiveTab === 'my' ? 'text-primary' : 'text-[var(--zh-text-tertiary)] hover:text-[var(--zh-text-secondary)]'"
              @click="switchGroupTab('my')"
            >
              我的群组
              <span v-if="myGroups.length && groupActiveTab !== 'my'" class="ml-0.5 inline-flex items-center justify-center min-w-[16px] h-4 px-1 rounded-full bg-primary/10 text-primary text-[10px] font-bold">{{ myGroups.length }}</span>
            </button>
            <button
              class="text-sm font-semibold transition-colors relative pb-0.5"
              :class="groupActiveTab === 'search' ? 'text-primary' : 'text-[var(--zh-text-tertiary)] hover:text-[var(--zh-text-secondary)]'"
              @click="switchGroupTab('search')"
            >发现群组</button>
          </div>
          <button
            v-if="userStore.isLoggedIn"
            class="flex items-center gap-1 px-2.5 py-1 bg-primary text-white rounded-lg text-xs font-semibold hover:bg-primary-600 active:scale-95 transition-all"
            @click="showCreateGroupModal = true"
          >
            <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" /></svg>
            创建群组
          </button>
        </div>

        <!-- 群组内容区 -->
        <div class="flex-1 overflow-y-auto">
          <Transition name="tab-fade" mode="out-in">
            <!-- 我的群组 -->
            <div v-if="groupActiveTab === 'my'" key="my" class="p-3 space-y-2.5">
              <!-- 未登录 -->
              <div v-if="!userStore.isLoggedIn" class="text-center py-16">
                <div class="w-20 h-20 mx-auto mb-6 rounded-2xl flex items-center justify-center bg-gradient-to-br from-slate-100 to-slate-200 dark:from-gray-800 dark:to-gray-750">
                  <svg class="w-11 h-11 text-slate-400 dark:text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
                  </svg>
                </div>
                <h3 class="text-[15px] font-semibold text-[var(--zh-text)] mb-2">登录后查看我的群组</h3>
                <p class="text-sm text-[var(--zh-text-tertiary)] mb-6">加入群组，与社区成员互动交流</p>
                <RouterLink to="/login" class="inline-flex items-center gap-1.5 px-5 py-2.5 bg-primary text-white rounded-xl text-sm font-semibold hover:bg-primary-600 active:scale-95 transition-all">立即登录</RouterLink>
              </div>

              <!-- 加载中 -->
              <div v-else-if="myGroupsLoading" class="space-y-2.5">
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

              <!-- 空状态 -->
              <div v-else-if="myGroups.length === 0" class="text-center py-16">
                <div class="w-20 h-20 mx-auto mb-6 rounded-2xl flex items-center justify-center bg-gradient-to-br from-primary/5 to-primary/10 dark:from-primary/10 dark:to-primary/15">
                  <svg class="w-11 h-11 text-primary/35 dark:text-primary/40" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M18.364 18.364A9 9 0 005.636 5.636m12.728 12.728A9 9 0 015.636 5.636m12.728 12.728L5.636 5.636" />
                  </svg>
                </div>
                <h3 class="text-[15px] font-semibold text-[var(--zh-text)] mb-2">还没有加入任何群组</h3>
                <p class="text-sm text-[var(--zh-text-tertiary)] mb-6">探索更多群组，找到你的兴趣圈子</p>
                <button class="inline-flex items-center gap-1.5 px-5 py-2.5 bg-primary text-white rounded-xl text-sm font-semibold hover:bg-primary-600 active:scale-95 transition-all" @click="switchGroupTab('search')">
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" /></svg>
                  发现群组
                </button>
              </div>

              <!-- 我的群组列表 -->
              <div v-else class="space-y-2.5">
                <div v-for="group in myGroups" :key="group.id" class="group-card" @click="router.push(`/groups/${group.id}`)">
                  <div class="group-card-inner">
                    <div class="relative shrink-0">
                      <img v-if="group.avatar" :src="group.avatar" :alt="group.name" class="group-card-avatar" @error="(e: Event) => { (e.target as HTMLImageElement).style.display = 'none'; (e.target as HTMLImageElement).nextElementSibling?.classList.remove('hidden') }" />
                      <div :class="[group.avatar ? 'hidden' : '', 'group-card-avatar-placeholder', avatarColor(group.name)]">{{ group.name.charAt(0) }}</div>
                    </div>
                    <div class="flex-1 min-w-0">
                      <div class="flex items-center gap-2">
                        <h3 class="text-sm font-semibold text-[var(--zh-text)] truncate">{{ group.name }}</h3>
                        <span v-if="group.myRole === 2" class="role-badge role-badge-owner">群主</span>
                        <span v-else-if="group.myRole === 1" class="role-badge role-badge-admin">管理员</span>
                      </div>
                      <p class="text-xs text-[var(--zh-text-secondary)] truncate mt-1.5">{{ getGroupLastMessagePreview(group.id) }}</p>
                      <div class="flex items-center gap-3 mt-1">
                        <span v-if="group.groupNumber" class="text-xs text-[var(--zh-text-tertiary)] font-mono tracking-wide">群号: {{ group.groupNumber }}</span>
                        <span class="flex items-center gap-1 text-xs text-[var(--zh-text-tertiary)]">
                          <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" /></svg>
                          {{ group.memberCount }} 人
                        </span>
                      </div>
                    </div>
                    <div class="flex flex-col items-end shrink-0 gap-1">
                      <span class="text-[10px] text-[var(--zh-text-tertiary)]">{{ formatRelativeTime(groupLastMessages[group.id]?.time) }}</span>
                      <svg class="group-card-arrow" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" /></svg>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- 发现群组 -->
            <div v-else key="search" class="p-3">
              <!-- 搜索栏 -->
              <div class="relative mb-5">
                <div class="search-wrapper" :class="{ 'search-wrapper-focused': groupSearchFocused }">
                  <svg class="search-icon" :class="{ 'text-primary': groupSearchFocused }" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                  </svg>
                  <input ref="groupSearchInputRef" v-model="groupSearchKeyword" type="text" class="search-input" placeholder="搜索群组名称、关键词或群号..." @focus="groupSearchFocused = true" @blur="groupSearchFocused = false" @keydown.enter="handleGroupSearch" />
                  <button v-if="groupSearchKeyword" class="search-clear-btn" @click="clearGroupSearch" title="清除搜索">
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" /></svg>
                  </button>
                  <button class="search-submit-btn" @click="handleGroupSearch" title="搜索">
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" /></svg>
                    <span class="hidden sm:inline">搜索</span>
                  </button>
                </div>
              </div>

              <!-- 搜索加载 -->
              <div v-if="groupSearchLoading && groupSearchPage === 1" class="space-y-2.5">
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
              <div v-else-if="groupSearchKeyword && groupSearchResults.length === 0 && !groupSearchLoading" class="text-center py-16">
                <div class="w-20 h-20 mx-auto mb-6 rounded-2xl flex items-center justify-center bg-gradient-to-br from-amber-50 to-orange-50 dark:from-amber-900/10 dark:to-orange-900/10">
                  <svg class="w-11 h-11 text-amber-400 dark:text-amber-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9.172 16.172a4 4 0 015.656 0M9 10h.01M15 10h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                  </svg>
                </div>
                <h3 class="text-[15px] font-semibold text-[var(--zh-text)] mb-2">未找到相关群组</h3>
                <p class="text-sm text-[var(--zh-text-tertiary)] mb-6">换个关键词试试，或者创建你自己的群组</p>
                <button v-if="userStore.isLoggedIn" class="inline-flex items-center gap-1.5 px-5 py-2.5 bg-primary text-white rounded-xl text-sm font-semibold hover:bg-primary-600 active:scale-95 transition-all" @click="showCreateGroupModal = true">
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" /></svg>
                  创建群组
                </button>
              </div>

              <!-- 无搜索关键词：推荐群组 -->
              <div v-else-if="!groupSearchKeyword">
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
                  <div class="flex items-center gap-2 mb-4">
                    <div class="w-7 h-7 rounded-lg bg-amber-100 dark:bg-amber-900/30 flex items-center justify-center">
                      <svg class="w-4 h-4 text-amber-500" fill="currentColor" viewBox="0 0 24 24"><path d="M11.48 3.499a.562.562 0 011.04 0l2.125 5.111a.563.563 0 00.475.345l5.518.442c.499.04.701.663.321.988l-4.204 3.602a.563.563 0 00-.182.557l1.285 5.385a.562.562 0 01-.84.61l-4.725-2.885a.563.563 0 00-.586 0L6.982 20.54a.562.562 0 01-.84-.61l1.285-5.386a.562.562 0 00-.182-.557l-4.204-3.602a.563.563 0 01.321-.988l5.518-.442a.563.563 0 00.475-.345L11.48 3.5z" /></svg>
                    </div>
                    <span class="text-sm font-semibold text-[var(--zh-text)]">推荐群组</span>
                    <span class="text-xs text-[var(--zh-text-tertiary)]">{{ recommendedGroups.length }} 个群组</span>
                  </div>
                  <div v-if="recommendedGroups.length === 0" class="text-center py-12">
                    <div class="w-20 h-20 mx-auto mb-6 rounded-2xl flex items-center justify-center bg-gradient-to-br from-slate-100 to-slate-200 dark:from-gray-800 dark:to-gray-750">
                      <svg class="w-11 h-11 text-slate-400 dark:text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-2.586a1 1 0 00-.707.293l-2.414 2.414a1 1 0 01-.707.293h-3.172a1 1 0 01-.707-.293l-2.414-2.414A1 1 0 006.586 13H4" /></svg>
                    </div>
                    <h3 class="text-[15px] font-semibold text-[var(--zh-text)] mb-2">暂无推荐群组</h3>
                    <p class="text-sm text-[var(--zh-text-tertiary)]">你可以通过上方搜索栏查找感兴趣的群组</p>
                  </div>
                  <div v-else class="space-y-2.5">
                    <div v-for="group in recommendedGroups" :key="group.id" class="group-card" @click="router.push(`/groups/${group.id}`)">
                      <div class="group-card-inner">
                        <div class="relative shrink-0">
                          <img v-if="group.avatar" :src="group.avatar" :alt="group.name" class="group-card-avatar" @error="(e: Event) => { (e.target as HTMLImageElement).style.display = 'none'; (e.target as HTMLImageElement).nextElementSibling?.classList.remove('hidden') }" />
                          <div :class="[group.avatar ? 'hidden' : '', 'group-card-avatar-placeholder', avatarColor(group.name)]">{{ group.name.charAt(0) }}</div>
                        </div>
                        <div class="flex-1 min-w-0">
                          <h3 class="text-sm font-semibold text-[var(--zh-text)] truncate">{{ group.name }}</h3>
                          <div class="flex items-center gap-3 mt-1.5">
                            <span v-if="group.groupNumber" class="text-xs text-[var(--zh-text-tertiary)] font-mono tracking-wide">群号: {{ group.groupNumber }}</span>
                            <span class="flex items-center gap-1 text-xs text-[var(--zh-text-tertiary)]">
                              <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" /></svg>
                              {{ group.memberCount }} 人
                            </span>
                            <span v-if="group.ownerName" class="text-xs text-[var(--zh-text-tertiary)] hidden sm:inline">群主 {{ group.ownerName }}</span>
                            <span v-if="group.description" class="text-xs text-[var(--zh-text-tertiary)] truncate hidden sm:inline">{{ group.description }}</span>
                          </div>
                        </div>
                        <button v-if="userStore.isLoggedIn && group.myRole === 0" class="join-btn" :disabled="joiningSet.has(group.id)" @click.stop="handleJoinGroup(group.id)">
                          <svg v-if="joiningSet.has(group.id)" class="w-3.5 h-3.5 animate-spin" fill="none" viewBox="0 0 24 24"><circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle><path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path></svg>
                          {{ joiningSet.has(group.id) ? '申请中' : '申请加入' }}
                        </button>
                        <span v-else-if="group.myRole && group.myRole > 0" class="joined-badge">
                          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" /></svg>
                          已加入
                        </span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <!-- 搜索结果列表 -->
              <div v-else class="space-y-2.5">
                <div class="flex items-center gap-2 mb-3 px-1">
                  <div class="w-1.5 h-1.5 rounded-full bg-primary"></div>
                  <p class="text-xs text-[var(--zh-text-tertiary)]">搜索 "<span class="text-[var(--zh-text)] font-medium">{{ groupSearchKeyword }}</span>" 共找到 <span class="text-primary font-semibold">{{ groupSearchResultTotal || groupSearchResults.length }}</span> 个群组</p>
                </div>
                <div v-for="group in groupSearchResults" :key="group.id" class="group-card" @click="router.push(`/groups/${group.id}`)">
                  <div class="group-card-inner">
                    <div class="relative shrink-0">
                      <img v-if="group.avatar" :src="group.avatar" :alt="group.name" class="group-card-avatar" @error="(e: Event) => { (e.target as HTMLImageElement).style.display = 'none'; (e.target as HTMLImageElement).nextElementSibling?.classList.remove('hidden') }" />
                      <div :class="[group.avatar ? 'hidden' : '', 'group-card-avatar-placeholder', avatarColor(group.name)]">{{ group.name.charAt(0) }}</div>
                    </div>
                    <div class="flex-1 min-w-0">
                      <h3 class="text-sm font-semibold text-[var(--zh-text)] truncate">{{ group.name }}</h3>
                      <div class="flex items-center gap-3 mt-1.5">
                        <span v-if="group.groupNumber" class="text-xs text-[var(--zh-text-tertiary)] font-mono tracking-wide">群号: {{ group.groupNumber }}</span>
                        <span class="flex items-center gap-1 text-xs text-[var(--zh-text-tertiary)]">
                          <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" /></svg>
                          {{ group.memberCount }} 人
                        </span>
                        <span v-if="group.ownerName" class="text-xs text-[var(--zh-text-tertiary)] hidden sm:inline">群主 {{ group.ownerName }}</span>
                        <span v-if="group.description" class="text-xs text-[var(--zh-text-tertiary)] truncate hidden sm:inline">{{ group.description }}</span>
                      </div>
                    </div>
                    <button v-if="userStore.isLoggedIn && group.myRole === 0" class="join-btn" :disabled="joiningSet.has(group.id)" @click.stop="handleJoinGroup(group.id)">
                      <svg v-if="joiningSet.has(group.id)" class="w-3.5 h-3.5 animate-spin" fill="none" viewBox="0 0 24 24"><circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle><path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path></svg>
                      {{ joiningSet.has(group.id) ? '申请中' : '申请加入' }}
                    </button>
                    <span v-else-if="group.myRole && group.myRole > 0" class="joined-badge">
                      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" /></svg>
                      已加入
                    </span>
                  </div>
                </div>
              </div>

              <!-- 搜索无限滚动哨兵 -->
              <div ref="groupSearchSentinelRef" class="h-0.5"></div>
              <div v-if="groupSearchLoading" class="text-center py-4">
                <span class="text-xs text-[var(--zh-text-tertiary)]">
                  <svg class="animate-spin inline w-3.5 h-3.5 mr-1" fill="none" viewBox="0 0 24 24"><circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle><path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path></svg>
                  加载中...
                </span>
              </div>
              <div v-else-if="!groupSearchHasMore && groupSearchResults.length > 0" class="text-center py-4">
                <span class="text-xs text-[var(--zh-text-tertiary)]">没有更多了</span>
              </div>
            </div>
          </Transition>
        </div>
      </div>
    </template>

    <!-- 创建群组弹窗 -->
    <Teleport to="body">
      <Transition name="modal">
        <div v-if="showCreateGroupModal" class="modal-overlay" @click.self="showCreateGroupModal = false">
          <div class="modal-backdrop"></div>
          <div class="modal-panel">
            <div class="flex items-center justify-between mb-5">
              <h3 class="text-lg font-bold text-[var(--zh-text)]">创建群组</h3>
              <button class="modal-close-btn" @click="showCreateGroupModal = false">
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" /></svg>
              </button>
            </div>
            <div class="space-y-4 mb-6">
              <div>
                <label class="input-label">群组名称 <span class="text-danger">*</span></label>
                <input ref="createGroupNameInputRef" v-model="createGroupForm.name" type="text" class="input" placeholder="给你的群组取个名字" maxlength="30" @keydown.enter="handleCreateGroup" />
                <p class="text-right text-[10px] text-[var(--zh-text-tertiary)] mt-1">{{ createGroupForm.name.length }}/30</p>
              </div>
              <div>
                <label class="input-label">群组简介 <span class="text-slate-300 font-normal">(选填)</span></label>
                <textarea v-model="createGroupForm.description" class="input resize-none" rows="3" placeholder="介绍一下这个群组..." maxlength="200"></textarea>
                <p class="text-right text-[10px] text-[var(--zh-text-tertiary)] mt-1">{{ createGroupForm.description.length }}/200</p>
              </div>
              <div>
                <label class="input-label">可见性</label>
                <div class="flex gap-2">
                  <button class="visibility-option" :class="createGroupForm.isPublic ? 'visibility-option-active' : 'visibility-option-inactive'" @click="createGroupForm.isPublic = 1">
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3.055 11H5a2 2 0 012 2v1a2 2 0 002 2 2 2 0 012 2v2.945M8 3.935V5.5A2.5 2.5 0 0010.5 8h.5a2 2 0 012 2 2 2 0 104 0 2 2 0 012-2h1.064M15 20.488V18a2 2 0 012-2h3.064M21 12a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>
                    公开
                  </button>
                  <button class="visibility-option" :class="!createGroupForm.isPublic ? 'visibility-option-active' : 'visibility-option-inactive'" @click="createGroupForm.isPublic = 0">
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" /></svg>
                    私密
                  </button>
                </div>
                <p class="text-[10px] text-[var(--zh-text-tertiary)] mt-1.5">{{ createGroupForm.isPublic ? '所有人都可以发现和加入' : '仅群主和管理员可邀请成员加入' }}</p>
              </div>
            </div>
            <div class="flex gap-3">
              <button class="flex-1 modal-cancel-btn" @click="showCreateGroupModal = false">取消</button>
              <button class="flex-1 modal-submit-btn" :disabled="!createGroupForm.name.trim() || groupCreating" @click="handleCreateGroup">
                <svg v-if="groupCreating" class="w-4 h-4 animate-spin" fill="none" viewBox="0 0 24 24"><circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle><path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path></svg>
                {{ groupCreating ? '创建中...' : '创建群组' }}
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- ===== 通知Tab ===== -->
    <template v-if="activeMainTab === 'notifications'">
      <div class="flex-1 flex flex-col overflow-hidden">
        <!-- 通知操作栏 -->
        <div class="flex items-center justify-between px-4 py-2 border-b border-[var(--zh-border)] bg-[var(--zh-bg-elevated)] shrink-0">
          <div class="flex items-center gap-1 overflow-x-auto no-scrollbar">
            <button
              v-for="tab in notiTabs"
              :key="tab.value"
              class="shrink-0 px-3 py-1.5 text-xs rounded-full transition-colors"
              :class="activeNotiTab === tab.value ? 'bg-primary text-white' : 'text-[var(--zh-text-secondary)] hover:text-[var(--zh-text-secondary)] bg-[var(--zh-bg-hover)]'"
              @click="switchNotiTab(tab.value)"
            >{{ tab.label }}</button>
          </div>
          <div class="flex items-center gap-1 shrink-0 ml-2">
            <button v-if="notiBatchMode" class="text-xs px-2 py-1 text-[var(--zh-text-secondary)] hover:text-[var(--zh-text-secondary)]" @click="exitNotiBatch">取消</button>
            <button v-if="notiBatchMode" class="text-xs px-2 py-1 text-danger" :disabled="notiSelectedIds.length === 0 || notiDeleting" @click="batchDeleteNoti">
              {{ notiDeleting ? '删除中...' : `删除(${notiSelectedIds.length})` }}
            </button>
            <button v-if="!notiBatchMode && notifications.length > 0" class="text-xs px-2 py-1 text-[var(--zh-text-secondary)] hover:text-[var(--zh-text-secondary)]" @click="enterNotiBatch">批量管理</button>
            <button v-if="!notiBatchMode && notiUnreadTotal > 0" class="text-xs px-2 py-1 text-primary hover:text-primary-600" :disabled="notiMarkingRead" @click="markAllNotiRead">
              {{ notiMarkingRead ? '标记中...' : '全部已读' }}
            </button>
          </div>
        </div>
        <!-- 通知列表 -->
        <div class="flex-1 overflow-y-auto">
          <div v-if="notiLoading && notifications.length === 0" class="flex justify-center py-16">
            <div class="w-6 h-6 border-2 border-primary border-t-transparent rounded-full animate-spin" />
          </div>
          <ErrorRetry v-else-if="notiError" :message="notiError" :on-retry="fetchNotis" />
          <EmptyState v-else-if="notifications.length === 0" title="暂无通知" />
          <div v-else>
            <div
              v-for="item in notifications"
              :key="item.id"
              class="flex items-start gap-3 px-4 py-3 hover:bg-[var(--zh-bg-hover)] transition-colors border-b border-slate-50"
              :class="{ 'bg-primary-50/30': !item.isRead }"
              @click="handleNotiClick(item)"
            >
              <input v-if="notiBatchMode" type="checkbox" :checked="notiSelectedIds.includes(item.id)" class="mt-0.5 w-4 h-4 rounded border-[var(--zh-border)] text-primary" @click.stop="toggleNotiSelect(item.id)" />
              <!-- 图标 -->
              <div class="shrink-0 w-10 h-10 rounded-full flex items-center justify-center"
                :class="{
                  'bg-[var(--zh-primary-bg)]': item.type === 1, 'bg-amber-50': item.type === 2,
                  'bg-red-50': item.type === 3, 'bg-purple-50': item.type === 4,
                  'bg-emerald-50': item.type === 5,
                }"
              >
                <svg v-if="item.type === 1" class="w-5 h-5 text-[var(--zh-primary)]" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>
                <svg v-else-if="item.type === 2" class="w-5 h-5 text-amber-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>
                <svg v-else-if="item.type === 3" class="w-5 h-5 text-red-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" /></svg>
                <svg v-else-if="item.type === 4" class="w-5 h-5 text-purple-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M18 9v3m0 0v3m0-3h3m-3 0h-3m-2-5a4 4 0 11-8 0 4 4 0 018 0zM3 20a6 6 0 0112 0v1H3v-1z" /></svg>
                <svg v-else-if="item.type === 5" class="w-5 h-5 text-emerald-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" /></svg>
              </div>
              <div class="flex-1 min-w-0">
                <div class="flex items-start justify-between gap-2">
                  <p class="text-sm leading-snug" :class="item.isRead ? 'text-[var(--zh-text-secondary)]' : 'text-[var(--zh-text)] font-medium'">{{ item.title }}</p>
                  <span v-if="!item.isRead" class="shrink-0 w-2 h-2 bg-primary rounded-full mt-1.5" />
                </div>
                <p v-if="item.content" class="text-xs text-[var(--zh-text-tertiary)] mt-1 line-clamp-2">{{ item.content }}</p>
                <span class="text-2xs text-[var(--zh-text-tertiary)] mt-1">{{ formatNotiTime(item.createdAt) }}</span>
              </div>
            </div>
            <div v-if="notiHasMore" ref="notiSentinelRef" class="flex justify-center py-3">
              <div v-if="notiLoadingMore" class="w-5 h-5 border-2 border-primary border-t-transparent rounded-full animate-spin" />
            </div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
/** 消息中心 - QQ风格：私信 + 群组 + 通知 */
import type { Conversation, Message, Notification } from '@/types'
import { notificationApi } from '@/api/notification'
import { socialApi } from '@/api'
import { groupApi } from '@/api/group'
import type { GroupInfo, GroupMessage } from '@/api/group'
import { avatarColor } from '@/utils/color'
import { showToast } from '@/composables/useToast'
import VoiceMessage from '@/components/VoiceMessage.vue'
import ChatToolbar from '@/components/chat/ChatToolbar.vue'
import VoiceRecordingBar from '@/components/chat/VoiceRecordingBar.vue'
import ChatBubble from '@/components/chat/ChatBubble.vue'
import { useVoiceRecorder } from '@/composables/useVoiceRecorder'
import { fileApi } from '@/api/file'
import { sanitizeText } from '@/utils/sanitize'
import { AI_AVATAR_URL } from '@/composables/chat/useChatConstants'

const userStore = useUserStore()
const notificationStore = useNotificationStore()
const { cachedRequest } = useRequestCache({ ttl: 5 * 60 * 1000 })
const router = useRouter()

/** 解析语音消息内容 - 兼容 JSON {url, duration} 和纯 URL 字符串 */
const getVoiceUrl = (content: string): string => {
  try { const data = JSON.parse(content); return data.url || content } catch { return content }
}
const getVoiceDuration = (content: string): number => {
  try { const data = JSON.parse(content); return data.duration || 0 } catch { return 0 }
}

// ===== 主Tab =====
const activeMainTab = ref<'messages' | 'groups' | 'notifications'>('messages')
// 未读私信数直接从会话列表计算，避免接口异常导致角标错误
const msgUnread = computed(() => conversations.value.reduce((sum, c) => sum + (c.unreadCount || 0), 0))

const mainTabs = computed(() => [
  { key: 'messages' as const, label: '私信', unread: msgUnread.value },
  { key: 'groups' as const, label: '群组', unread: 0 },
  { key: 'notifications' as const, label: '通知', unread: notiUnreadTotal.value },
])

const switchMainTab = (key: 'messages' | 'groups' | 'notifications') => {
  activeMainTab.value = key
  if (key === 'groups' && userStore.isLoggedIn && myGroups.value.length === 0 && !myGroupsLoading.value) {
    loadMyGroups()
  }
}

// ===== 私信模块 =====
const conversations = ref<Conversation[]>([])
const activeConversation = ref<Conversation | null>(null)
const messages = ref<Message[]>([])
const conversationSearch = ref('')
const loadingConv = ref(false)
const conversationsError = ref('')
const inputContent = ref('')
const msgListRef = ref<HTMLElement | null>(null)
const sendingMessage = ref(false)
const notiImageInputRef = ref<HTMLInputElement | null>(null)
const notiFileInputRef = ref<HTMLInputElement | null>(null)
const notiAiMode = ref(false)
// v15：AI 助手思考中状态（与群组页 GroupChatWindow 一致）
const notiAiReplying = ref(false)
const notiVoiceRecorder = reactive(useVoiceRecorder())
const notiVoiceUploading = ref(false)

const startNotiVoiceRecord = async () => {
  await notiVoiceRecorder.startRecording()
  if (notiVoiceRecorder.recordError) {
    showToast(notiVoiceRecorder.recordError, 'error')
  }
}
const notiInputFocused = ref(false)

// 点击头像跳转用户主页
const navigateToUser = (userId?: number) => {
  if (userId) router.push(`/user/${userId}`)
}

const filteredConversations = computed(() => {
  const kw = conversationSearch.value.trim().toLowerCase()
  return kw ? conversations.value.filter(c => c.user?.nickname?.toLowerCase().includes(kw)) : conversations.value
})

const loadConversations = async () => {
  loadingConv.value = true
  conversationsError.value = ''
  try {
    const { data } = await socialApi.getConversations({ page: 1, pageSize: 100 })
    // 兼容后端返回的扁平结构（userId/nickname/avatar）与旧版本的嵌套结构（user.id/user.nickname/...）
    conversations.value = (data.data.list || []).map((c: any) => {
      if (c.user) return c
      return {
        ...c,
        user: {
          id: c.userId,
          nickname: c.nickname,
          avatar: c.avatar,
          isOnline: !!c.isOnline,
        },
      }
    })
  } catch {
    conversationsError.value = '私信列表加载失败，请检查网络连接后重试'
  } finally {
    loadingConv.value = false
  }
}

const selectConversation = async (conv: Conversation) => {
  activeConversation.value = conv
  // 兼容扁平结构与嵌套结构
  const targetUserId = (conv as any).user?.id ?? (conv as any).userId
  if (!targetUserId) return
  try {
    const { data } = await socialApi.getMessages(targetUserId, { page: 1, pageSize: 30 })
    const me = userStore.userInfo
    const rawList = (data.data && data.data.list) || []
    messages.value = rawList.map((m: any) => transformMsg(m, me))
  } catch {
    showToast('消息记录加载失败，请重试', 'error')
    messages.value = []
  }
  if (conv.unreadCount > 0) {
    try {
      await socialApi.markConversationRead(targetUserId)
      conv.unreadCount = 0
    } catch {
      // 标记已读失败不影响浏览消息，静默处理
    }
  }
  await nextTick()
  if (msgListRef.value) msgListRef.value.scrollTop = msgListRef.value.scrollHeight
}

const sendMessage = async () => {
  if (!inputContent.value.trim() || !activeConversation.value || sendingMessage.value) return
  const targetUserId = (activeConversation.value as any).user?.id ?? (activeConversation.value as any).userId
  if (!targetUserId) return
  const rawContent = inputContent.value.trim()
  const me = userStore.userInfo

  // AI助手模式
  const isAIMention = rawContent.includes('@AI助手')
  if (notiAiMode.value || isAIMention) {
    const question = rawContent.replace(/@AI助手\s*/g, '').trim()
    if (!question) return
    sendingMessage.value = true
    inputContent.value = ''
    const contentWithPrefix = sanitizeText('@AI助手 ' + question)
    const tempId = -Date.now()
    messages.value.push({
      id: tempId, conversationId: 0, senderId: me?.id || 0,
      sender: me || { id: 0, uid: '0', username: '', nickname: '', avatar: '', bio: '', email: '', phone: '', gender: 0 as any, birthday: '', followCount: 0, followerCount: 0, articleCount: 0, likeCount: 0, isFollowing: false, createdAt: '' },
      content: contentWithPrefix, type: 0 as any, isRead: true, createdAt: new Date().toISOString(),
    })
    nextTick(() => { if (msgListRef.value) msgListRef.value.scrollTop = msgListRef.value.scrollHeight })
    // v15：先显示"AI 思考中"指示器
    notiAiReplying.value = true
    try {
      const { data } = await socialApi.sendMessage(targetUserId, { content: contentWithPrefix })
      if (data && data.data) {
        const idx = messages.value.findIndex(m => m.id === tempId)
        if (idx >= 0) messages.value.splice(idx, 1, transformMsg(data.data, me))
      }
      const aiRes = await socialApi.sendAIMessage(targetUserId, question)
      const raw: any = aiRes.data?.data
      if (raw) {
        const aiMsg: Message = {
          id: Number(raw.id) || Date.now(), conversationId: Number(raw.conversationId) || 0, senderId: Number(raw.senderId) || targetUserId,
          sender: { id: 0, uid: '0', username: raw.senderNickname || 'AI助手', nickname: raw.senderNickname || 'AI助手', avatar: raw.senderAvatar || AI_AVATAR_URL, bio: '', email: '', phone: '', gender: 0 as any, birthday: '', followCount: 0, followerCount: 0, articleCount: 0, likeCount: 0, isFollowing: false, createdAt: '' },
          content: raw.content || '', type: 'ai_reply', isRead: false, createdAt: raw.createdAt || new Date().toISOString(),
        }
        if (!messages.value.some(m => m.id === aiMsg.id)) messages.value.push(aiMsg)
      }
      notiAiMode.value = false
      nextTick(() => { if (msgListRef.value) msgListRef.value.scrollTop = msgListRef.value.scrollHeight })
    } catch (e: any) {
      inputContent.value = rawContent
      showToast(e?.message || 'AI回复失败', 'error')
    } finally {
      sendingMessage.value = false
      notiAiReplying.value = false
    }
    return
  }

  // 普通文本消息
  const content = sanitizeText(rawContent)
  const tempId = -Date.now()
  messages.value.push({
    id: tempId, conversationId: 0, senderId: me?.id || 0,
    sender: me || { id: 0, uid: '0', username: '', nickname: '', avatar: '', bio: '', email: '', phone: '', gender: 0 as any, birthday: '', followCount: 0, followerCount: 0, articleCount: 0, likeCount: 0, isFollowing: false, createdAt: '' },
    content, type: 0 as any, isRead: true, createdAt: new Date().toISOString(),
  })
  inputContent.value = ''
  sendingMessage.value = true
  nextTick(() => { if (msgListRef.value) msgListRef.value.scrollTop = msgListRef.value.scrollHeight })
  try {
    const { data } = await socialApi.sendMessage(targetUserId, { content })
    if (data && data.data) {
      const idx = messages.value.findIndex(m => m.id === tempId)
      if (idx >= 0) messages.value.splice(idx, 1, transformMsg(data.data, me))
      const conv = conversations.value.find(c => c.user?.id === targetUserId)
      if (conv) (conv as any).lastMessage = content
    }
    nextTick(() => { if (msgListRef.value) msgListRef.value.scrollTop = msgListRef.value.scrollHeight })
  } catch (e: any) {
    const idx = messages.value.findIndex(m => m.id === tempId)
    if (idx >= 0) messages.value.splice(idx, 1)
    inputContent.value = rawContent
    const errMsg = e?.response?.data?.message || e?.message || '消息发送失败'
    showToast(`${errMsg}，请检查网络后重试`, 'error')
  } finally { sendingMessage.value = false }
}

/** 语音录制完成 → 上传 → 发送 */
const finishNotiVoiceRecord = async () => {
  const duration = notiVoiceRecorder.recordingTime
  const finalBlob = await notiVoiceRecorder.stopRecording()
  if (!finalBlob) { notiVoiceRecorder.cancelRecording(); return }
  if (!activeConversation.value) { notiVoiceRecorder.cancelRecording(); return }
  const targetUserId = (activeConversation.value as any).user?.id ?? (activeConversation.value as any).userId
  if (!targetUserId) { notiVoiceRecorder.cancelRecording(); return }

  notiVoiceUploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', finalBlob, 'voice.webm')
    const { upload } = useApi()
    const uploadRes = await upload<any>('/files/upload/voice', formData)
    const voiceUrl = uploadRes.data?.data
    if (voiceUrl) {
      const content = JSON.stringify({ url: voiceUrl, duration: duration || 0 })
      const { data } = await socialApi.sendMessage(targetUserId, { content, type: 'voice' })
      const me = userStore.userInfo
      if (data && data.data) {
        messages.value.push(transformMsg(data.data, me))
        nextTick(() => { if (msgListRef.value) msgListRef.value.scrollTop = msgListRef.value.scrollHeight })
        const conv = conversations.value.find(c => c.user?.id === targetUserId)
        if (conv) (conv as any).lastMessage = '[语音]'
      }
    } else {
      showToast('语音上传失败', 'error')
    }
  } catch (e: any) {
    showToast(e?.message || '语音发送失败', 'error')
  } finally {
    notiVoiceUploading.value = false
    notiVoiceRecorder.cancelRecording()
  }
}

/** 图片选择 → 上传 → 发送 */
const onNotiImageSelected = async (e: Event) => {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file || !activeConversation.value) return
  input.value = ''
  if (!file.type.startsWith('image/')) { showToast('请选择图片文件', 'error'); return }
  if (file.size > 5 * 1024 * 1024) { showToast('图片不能超过5MB', 'error'); return }
  const targetUserId = (activeConversation.value as any).user?.id ?? (activeConversation.value as any).userId
  if (!targetUserId) return
  sendingMessage.value = true
  try {
    const imageUrl = await fileApi.uploadSingleImage(file)
    if (!imageUrl) throw new Error('上传失败')
    const { data } = await socialApi.sendMessage(targetUserId, { content: imageUrl, type: 'image' })
    const me = userStore.userInfo
    if (data && data.data) {
      messages.value.push(transformMsg(data.data, me))
      nextTick(() => { if (msgListRef.value) msgListRef.value.scrollTop = msgListRef.value.scrollHeight })
      const conv = conversations.value.find(c => c.user?.id === targetUserId)
      if (conv) (conv as any).lastMessage = '[图片]'
    }
  } catch (err: any) { showToast(err.message || '图片发送失败', 'error') }
  finally { sendingMessage.value = false }
}

/** 文件选择 → 上传 → 发送 */
const onNotiFileSelected = async (e: Event) => {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file || !activeConversation.value) return
  input.value = ''
  const targetUserId = (activeConversation.value as any).user?.id ?? (activeConversation.value as any).userId
  if (!targetUserId) return
  sendingMessage.value = true
  try {
    const formData = new FormData()
    formData.append('file', file)
    const res = await fileApi.uploadFile(formData)
    const url = res.data.data
    const content = JSON.stringify({ url, name: file.name, size: file.size })
    const { data } = await socialApi.sendMessage(targetUserId, { content, type: 'file' })
    const me = userStore.userInfo
    if (data && data.data) {
      messages.value.push(transformMsg(data.data, me))
      nextTick(() => { if (msgListRef.value) msgListRef.value.scrollTop = msgListRef.value.scrollHeight })
      const conv = conversations.value.find(c => c.user?.id === targetUserId)
      if (conv) (conv as any).lastMessage = '[文件]'
    }
  } catch (err: any) { showToast(err.message || '文件发送失败', 'error') }
  finally { sendingMessage.value = false }
}

const isMyMsg = (msg: Message) => msg.senderId === userStore.userInfo?.id

const formatConvTime = (t: string) => {
  if (!t) return ''
  const d = new Date(t)
  if (isNaN(d.getTime())) return ''
  const n = new Date(), diff = n.getTime() - d.getTime()
  if (diff < 86400000) return d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  return d.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}

/**
 * 会话列表中的最后消息预览
 * 后端 ConversationVO.lastMessage 为 String（已解密的内容文本），
 * 但老数据可能是 Message 对象；两者都做兼容。
 */
const getConvLastMessage = (conv: any): string => {
  if (!conv) return '暂无消息'
  const lm = conv.lastMessage
  if (lm == null) return '暂无消息'
  if (typeof lm === 'string') return lm || '暂无消息'
  if (typeof lm === 'object') {
    const type = lm.type
    const content = lm.content || ''
    // 2026-07-02 v13: 解析 content 中的 JSON 字符串（voice/file 消息），提取可读预览
    let parsed: any = null
    if (content.startsWith('{')) {
      try { parsed = JSON.parse(content) } catch { /* ignore */ }
    }
    if (type === 1 || type === 'image') return '[图片]'
    if (type === 2 || type === 'system') return '[系统消息]'
    if (type === 'voice') {
      const dur = parsed?.duration ? `${parsed.duration}''` : ''
      return dur ? `[语音] ${dur}` : '[语音]'
    }
    if (type === 'file') {
      const name = parsed?.name || '文件'
      return `[文件] ${name}`
    }
    if (type === 'ai_reply') return '[AI回复]'
    if (type === 'text') return content || '暂无消息'
    // 未知类型：尝试从 JSON 中提取 name 或截取纯文本
    if (parsed?.name) return `[文件] ${parsed.name}`
    return content || '暂无消息'
  }
  return '暂无消息'
}

/**
 * 单条消息的发送时间（紧凑显示）
 */
const formatMsgTime = (s: string) => {
  if (!s) return ''
  const d = new Date(s)
  if (isNaN(d.getTime())) return ''
  const n = new Date()
  const pad = (x: number) => x.toString().padStart(2, '0')
  const sameDay = d.toDateString() === n.toDateString()
  if (sameDay) return `${pad(d.getHours())}:${pad(d.getMinutes())}`
  return `${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

/**
 * 将后端 MessageVO 转换为前端 Message 类型
 * 兼容：嵌套 sender 字段为空、扁平 senderId/senderNickname/senderAvatar
 */
const transformMsg = (raw: any, me: any): Message => {
  const senderId = Number(raw?.senderId) || 0
  const senderInfo: any = raw?.sender || {}
  const senderNickname = senderInfo.nickname ?? raw?.senderNickname ?? me?.nickname ?? '用户'
  const senderAvatar = senderInfo.avatar ?? raw?.senderAvatar ?? me?.avatar ?? ''
  const sender = senderId === me?.id && me
    ? me
    : {
        id: senderId,
        uid: String(senderId),
        username: senderNickname,
        nickname: senderNickname,
        avatar: senderAvatar,
        bio: '',
        email: '',
        phone: '',
        gender: 0 as any,
        birthday: '',
        followCount: 0,
        followerCount: 0,
        articleCount: 0,
        likeCount: 0,
        isFollowing: false,
        createdAt: '',
      }
  return {
    id: Number(raw?.id) || 0,
    conversationId: Number(raw?.conversationId) || 0,
    senderId,
    sender,
    content: raw?.content || '',
    type: raw?.type != null ? String(raw.type) : 'text',
    isRead: raw?.isRead === 1 || raw?.isRead === true,
    createdAt: raw?.createdAt || new Date().toISOString(),
  }
}

// ===== 群组模块 =====
const groupActiveTab = ref<'my' | 'search'>('my')
const groupSearchKeyword = ref('')
const groupSearchInputRef = ref<HTMLInputElement | null>(null)
const groupSearchSentinelRef = ref<HTMLElement | null>(null)
const groupSearchFocused = ref(false)

// 我的群组
const myGroups = ref<GroupInfo[]>([])
const myGroupsLoading = ref(false)

// 推荐群组 / 发现群组
const recommendedGroups = ref<GroupInfo[]>([])
const discoverLoading = ref(false)

// 搜索
const groupSearchResults = ref<GroupInfo[]>([])
const groupSearchLoading = ref(false)
const groupSearchPage = ref(1)
const groupSearchHasMore = ref(false)
const groupSearchResultTotal = ref(0)

// 群组最后消息
const groupLastMessages = reactive<Record<number, { content: string; time: string; type?: string }>>({})

const fetchGroupLastMessages = async (groups: any[]) => {
  if (!groups.length) return
  await Promise.all(groups.map(async (g: any) => {
    if (groupLastMessages[g.id]) return
    try {
      const { data } = await groupApi.getMessages(g.id, 0, 50)
      const raw = data?.data || data
      const msgs = Array.isArray(raw) ? raw : (raw?.list || [])
      if (msgs.length > 0) {
        const first = msgs[0] as any
        const last = msgs[msgs.length - 1] as any
        const target = new Date(first.createdAt || 0).getTime() > new Date(last.createdAt || 0).getTime() ? first : last
        groupLastMessages[g.id] = {
          content: target.content || '',
          time: target.createdAt || '',
          type: target.messageType,
        }
      }
    } catch {
      // silent
    }
  }))
}

const getGroupLastMessagePreview = (groupId: number) => {
  const msg = groupLastMessages[groupId]
  if (!msg || !msg.content) return '暂无消息'
  if (msg.type === 'image') return '[图片]'
  return msg.content.length > 30 ? msg.content.slice(0, 30) + '...' : msg.content
}

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
    return `${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
  }
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

// 创建弹窗
const showCreateGroupModal = ref(false)
const groupCreating = ref(false)
const createGroupNameInputRef = ref<HTMLInputElement | null>(null)
const createGroupForm = reactive({ name: '', description: '', isPublic: 1 })

const switchGroupTab = (tab: 'my' | 'search') => {
  groupActiveTab.value = tab
  if (tab === 'my') {
    loadMyGroups()
  } else {
    loadRecommendedGroups()
    nextTick(() => groupSearchInputRef.value?.focus())
  }
}

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

const loadRecommendedGroups = async () => {
  if (recommendedGroups.value.length > 0) return
  discoverLoading.value = true
  try {
    const { data } = await groupApi.searchGroups('', 1, 20)
    const list = data?.data?.list || data?.data || []
    recommendedGroups.value = Array.isArray(list) ? list : []
  } catch {
    recommendedGroups.value = []
  } finally {
    discoverLoading.value = false
  }
}

const clearGroupSearch = () => {
  groupSearchKeyword.value = ''
  groupSearchResults.value = []
  groupSearchHasMore.value = false
  groupSearchResultTotal.value = 0
  groupSearchInputRef.value?.focus()
}

const handleGroupSearch = async () => {
  const keyword = groupSearchKeyword.value.trim()
  if (!keyword) { clearGroupSearch(); return }
  groupSearchPage.value = 1
  groupSearchLoading.value = true
  try {
    const { data } = await groupApi.searchGroups(keyword, 1, 20)
    const list = data?.data?.list || data?.data || []
    groupSearchResults.value = Array.isArray(list) ? list : []
    groupSearchHasMore.value = groupSearchResults.value.length >= 20
    groupSearchResultTotal.value = data?.data?.total || groupSearchResults.value.length
  } catch {
    groupSearchResults.value = []
    showToast('搜索失败，请稍后重试', 'error', { position: 'top-center' })
  } finally {
    groupSearchLoading.value = false
  }
}

const loadMoreGroupSearch = async () => {
  groupSearchPage.value++
  groupSearchLoading.value = true
  try {
    const { data } = await groupApi.searchGroups(groupSearchKeyword.value.trim(), groupSearchPage.value, 20)
    const list = data?.data?.list || data?.data || []
    const arr = Array.isArray(list) ? list : []
    groupSearchResults.value.push(...arr)
    groupSearchHasMore.value = arr.length >= 20
  } catch {
    groupSearchPage.value--
    showToast('加载失败，请稍后重试', 'error', { position: 'top-center' })
  } finally {
    groupSearchLoading.value = false
  }
}

// 搜索防抖
let groupSearchTimer: ReturnType<typeof setTimeout> | null = null
watch(groupSearchKeyword, (val) => {
  if (groupSearchTimer) clearTimeout(groupSearchTimer)
  if (!val.trim()) { groupSearchResults.value = []; groupSearchHasMore.value = false; groupSearchResultTotal.value = 0; return }
  groupSearchTimer = setTimeout(handleGroupSearch, 350)
})

// 申请加入群组
const joiningSet = ref(new Set<number>())
const handleJoinGroup = async (groupId: number) => {
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

// 创建群组
const handleCreateGroup = async () => {
  if (!createGroupForm.name.trim()) return
  groupCreating.value = true
  try {
    const { data } = await groupApi.createGroup({
      name: createGroupForm.name.trim(),
      description: createGroupForm.description.trim() || undefined,
      isPublic: createGroupForm.isPublic,
    })
    showToast('群组创建成功', 'success', { position: 'top-center' })
    showCreateGroupModal.value = false
    createGroupForm.name = ''
    createGroupForm.description = ''
    createGroupForm.isPublic = 1
    loadMyGroups()
    router.push(`/groups/${data?.data}`)
  } catch {
    showToast('创建失败，请稍后重试', 'error', { position: 'top-center' })
  } finally {
    groupCreating.value = false
  }
}

watch(showCreateGroupModal, (val) => {
  if (val) nextTick(() => createGroupNameInputRef.value?.focus())
})

// 搜索结果无限滚动
let groupSearchObserver: IntersectionObserver | null = null
onMounted(() => {
  nextTick(() => {
    if (groupSearchSentinelRef.value) {
      groupSearchObserver = new IntersectionObserver((entries) => {
        if (entries[0]?.isIntersecting && groupSearchHasMore.value && !groupSearchLoading.value) {
          loadMoreGroupSearch()
        }
      }, { rootMargin: '200px' })
      groupSearchObserver.observe(groupSearchSentinelRef.value)
    }
  })
})
onUnmounted(() => groupSearchObserver?.disconnect())

// ===== 通知模块 =====
const notiTabs = [
  { label: '全部', value: 0 }, { label: '系统', value: 1 }, { label: '审核', value: 2 },
  { label: '互动', value: 3 }, { label: '关注', value: 4 }, { label: '私信', value: 5 },
]
const activeNotiTab = ref(0)
const notifications = ref<Notification[]>([])
const notiLoading = ref(false)
const notiLoadingMore = ref(false)
const notiError = ref('')
const notiPage = ref(1)
const notiTotal = ref(0)
const notiBatchMode = ref(false)
const notiSelectedIds = ref<number[]>([])
const notiSentinelRef = ref<HTMLElement | null>(null)
const notiUnreadTotal = computed(() => notificationStore.unreadCount)
const notiHasMore = computed(() => notifications.value.length < notiTotal.value)
const notiMarkingRead = ref(false)
const notiDeleting = ref(false)

const switchNotiTab = (type: number) => {
  activeNotiTab.value = type; notiPage.value = 1; notifications.value = []; notiSelectedIds.value = []; fetchNotis()
}

const fetchNotis = async () => {
  notiLoading.value = true; notiError.value = ''
  try {
    const params: any = { page: notiPage.value, pageSize: 20 }
    if (activeNotiTab.value) params.type = activeNotiTab.value
    const { data } = await notificationApi.getNotifications(params)
    const d = data.data; notifications.value = d?.list || []; notiTotal.value = d?.total || 0
  } catch {
    notiError.value = '通知列表加载失败，请检查网络连接后重试'
  } finally {
    notiLoading.value = false
  }
}

const loadMoreNoti = async () => {
  notiLoadingMore.value = true; notiPage.value++
  try {
    const params: any = { page: notiPage.value, pageSize: 20 }
    if (activeNotiTab.value) params.type = activeNotiTab.value
    const { data } = await notificationApi.getNotifications(params)
    notifications.value.push(...(data.data?.list || [])); notiTotal.value = data.data?.total || 0
  } catch {
    notiPage.value--
    showToast('加载更多通知失败，请下滑重试', 'error')
  } finally {
    notiLoadingMore.value = false
  }
}

const handleNotiClick = async (n: Notification) => {
  if (notiBatchMode.value) { toggleNotiSelect(n.id); return }
  if (!n.isRead) {
    try {
      await notificationApi.markAsRead(n.id)
      notificationStore.markAsRead(n.id)
      n.isRead = true
    } catch {
      showToast('标记已读失败，请重试', 'error')
    }
  }
}

const markAllNotiRead = async () => {
  if (notiMarkingRead.value) return
  notiMarkingRead.value = true
  try {
    await notificationApi.markAllAsRead()
    notificationStore.markAllAsRead()
    notifications.value.forEach(n => n.isRead = true)
    showToast('已全部标记为已读')
  } catch {
    showToast('全部标记已读失败，请重试', 'error')
  } finally {
    notiMarkingRead.value = false
  }
}

const enterNotiBatch = () => { notiBatchMode.value = true; notiSelectedIds.value = [] }
const exitNotiBatch = () => { notiBatchMode.value = false; notiSelectedIds.value = [] }
const toggleNotiSelect = (id: number) => {
  const i = notiSelectedIds.value.indexOf(id)
  i > -1 ? notiSelectedIds.value.splice(i, 1) : notiSelectedIds.value.push(id)
}

const batchDeleteNoti = async () => {
  if (!notiSelectedIds.value.length || notiDeleting.value) return
  notiDeleting.value = true
  try {
    await notificationApi.batchDeleteNotifications(notiSelectedIds.value)
    const set = new Set(notiSelectedIds.value)
    notifications.value = notifications.value.filter(n => !set.has(n.id))
    notiTotal.value -= set.size
    notiSelectedIds.value = []
    showToast(`已删除 ${set.size} 条通知`)
  } catch {
    showToast('批量删除失败，请重试', 'error')
  } finally {
    notiDeleting.value = false
  }
}

const formatNotiTime = (s: string) => {
  if (!s) return ''
  const d = new Date(s), n = new Date(), m = Math.floor((n.getTime() - d.getTime()) / 60000)
  if (m < 1) return '刚刚'; if (m < 60) return `${m}分钟前`
  const h = Math.floor(m / 60); if (h < 24) return `${h}小时前`
  const days = Math.floor(h / 24); if (days < 7) return `${days}天前`
  return d.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}

// 无限滚动
let io: IntersectionObserver | null = null
onMounted(() => {
  loadConversations()
  fetchNotis()
  if (true) {
    io = new IntersectionObserver(([e]) => { if (e?.isIntersecting && notiHasMore.value && !notiLoadingMore.value) loadMoreNoti() }, { rootMargin: '200px' })
    watch(notiSentinelRef, el => { io?.disconnect(); if (el) io?.observe(el) }, { immediate: true })
  }
})
onUnmounted(() => io?.disconnect())

useHead({ title: () => '消息' + ' - 知讯' })
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

/* ==================== 私信输入区（复用群组样式） ==================== */
.noti-chat-input {
  padding: 6px 14px 0;
  border-top: 1px solid var(--zh-border, #e5e7eb);
  background: var(--zh-bg-elevated, #fff);
  flex-shrink: 0;
}
.noti-input-row {
  display: flex;
  align-items: flex-end;
  gap: 8px;
}
.noti-input-wrap {
  flex: 1;
  min-height: 38px;
  max-height: 120px;
  border: 1.5px solid var(--zh-border, #e5e7eb);
  border-radius: 20px;
  background: var(--zh-bg, #f8fafc);
  overflow: visible;
  position: relative;
  transition: border-color 0.2s, box-shadow 0.2s;
}
.noti-input-wrap.focused {
  border-color: var(--zh-primary, #6366f1);
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.08);
}
.noti-textarea {
  width: 100%;
  min-height: 38px;
  max-height: 120px;
  border: none;
  outline: none;
  background: transparent;
  padding: 8px 16px;
  font-size: 14px;
  color: var(--zh-text, #1e293b);
  font-family: inherit;
  resize: none;
  line-height: 1.5;
}
.noti-textarea::placeholder {
  color: var(--zh-text-placeholder, #94a3b8);
}
.noti-send-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  border: none;
  border-radius: 50%;
  background: var(--zh-primary, #6366f1);
  color: #fff;
  cursor: pointer;
  transition: opacity 0.2s, transform 0.2s;
  flex-shrink: 0;
  min-height: 0 !important;
  min-width: 0 !important;
}
.noti-send-btn:hover:not(:disabled) {
  opacity: 0.9;
  transform: scale(1.05);
}
.noti-send-btn:active:not(:disabled) {
  transform: scale(0.95);
}
.noti-send-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
.noti-voice-uploading {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  margin-bottom: 4px;
  font-size: 12px;
  color: #2563eb;
  background: #eff6ff;
  border-radius: 8px;
}
.dark .noti-voice-uploading {
  background: rgba(37, 99, 235, 0.15);
  color: #93c5fd;
}

/* v15：AI 助手思考中动画（与群组页 GroupChatWindow 一致） */
.ai-thinking-bubble {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 10px 16px;
  border-radius: 18px;
  border-bottom-left-radius: 6px;
  background: rgba(99, 102, 241, 0.06);
  border-left: 3px solid var(--zh-primary, #6366f1);
  max-width: 75%;
}
.ai-thinking-label {
  font-size: 11px;
  color: var(--zh-primary, #6366f1);
  font-weight: 600;
}
.ai-thinking-text {
  font-size: 13px;
  color: var(--zh-text-secondary, #64748b);
}
.ai-thinking-dots {
  display: inline-flex;
  gap: 3px;
  align-items: center;
}
.ai-thinking-dots .dot {
  width: 6px;
  height: 6px;
  background: var(--zh-primary, #6366f1);
  border-radius: 50%;
  display: inline-block;
  animation: aiThinkingBounce 1.2s infinite ease-in-out both;
}
.ai-thinking-dots .dot:nth-child(2) { animation-delay: 0.15s; }
.ai-thinking-dots .dot:nth-child(3) { animation-delay: 0.3s; }
@keyframes aiThinkingBounce {
  0%, 80%, 100% { transform: translateY(0); opacity: 0.4; }
  40% { transform: translateY(-4px); opacity: 1; }
}
</style>
