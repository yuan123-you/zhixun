<template>
  <!-- QQ风格群组聊天窗口 v2 -->
  <div class="qq-chat">
    <!-- 头部 -->
    <div class="qq-chat-header">
      <button class="qq-back-btn" @click="goBack" aria-label="返回">
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
        </svg>
      </button>
      <div class="qq-header-info">
        <h3 class="qq-chat-title">{{ group?.name }}</h3>
        <p class="qq-chat-subtitle">
          <span v-if="group?.groupNumber" class="qq-group-num">{{ group.groupNumber }}</span>
          <span class="qq-member-count">{{ group?.memberCount }}人</span>
        </p>
      </div>
      <!-- 右上角更多菜单 -->
      <div class="qq-header-actions">
        <button class="qq-header-btn" @click="$emit('toggleMembers')" aria-label="成员列表">
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" />
          </svg>
        </button>
        <button class="qq-header-btn" @click="showMenu = !showMenu" aria-label="更多">
          <svg class="w-5 h-5" fill="currentColor" viewBox="0 0 24 24">
            <circle cx="12" cy="5" r="2"/><circle cx="12" cy="12" r="2"/><circle cx="12" cy="19" r="2"/>
          </svg>
        </button>
        <!-- 下拉菜单 -->
        <Teleport to="body">
          <Transition name="menu-fade">
            <div v-if="showMenu" class="qq-menu-overlay" @click="showMenu = false">
              <div class="qq-menu" @click.stop>
                <!-- 群管理功能 -->
                <div v-if="canManageGroup" class="qq-menu-section">
                  <div class="qq-menu-section-title">群管理</div>
                  <button class="qq-menu-item" @click="openRenameDialog">
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" /></svg>
                    <span>修改群名</span>
                  </button>
                  <button class="qq-menu-item" @click="openAvatarDialog">
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" /></svg>
                    <span>修改群头像</span>
                  </button>
                </div>
                <!-- 群主专属 -->
                <div v-if="isOwner" class="qq-menu-section">
                  <div class="qq-menu-section-title">群主专属</div>
                  <button class="qq-menu-item" @click="openAdminDialog">
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" /></svg>
                    <span>管理管理员</span>
                  </button>
                  <button class="qq-menu-item qq-menu-item-danger" @click="handleDismissGroup">
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" /></svg>
                    <span>解散群组</span>
                  </button>
                </div>
                <!-- 通用功能 -->
                <div class="qq-menu-section">
                  <div class="qq-menu-section-title">聊天</div>
                  <button class="qq-menu-item" @click="showSearch = true; showMenu = false">
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" /></svg>
                    <span>查找聊天记录</span>
                  </button>
                  <button v-if="!isOwner" class="qq-menu-item qq-menu-item-danger" @click="handleLeaveGroup">
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" /></svg>
                    <span>退出群组</span>
                  </button>
                </div>
              </div>
            </div>
          </Transition>
        </Teleport>
      </div>
    </div>

    <!-- 修改群名弹窗 -->
    <Teleport to="body">
      <Transition name="modal-fade">
        <div v-if="showRenameDialog" class="qq-modal-overlay" @click="showRenameDialog = false">
          <div class="qq-modal" @click.stop>
            <div class="qq-modal-header">
              <h3 class="qq-modal-title">修改群名</h3>
              <button class="qq-modal-close" @click="showRenameDialog = false">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" /></svg>
              </button>
            </div>
            <div class="qq-modal-body">
              <div class="qq-form-group">
                <label class="qq-form-label">群名称</label>
                <input
                  v-model="newGroupName"
                  type="text"
                  class="qq-form-input"
                  placeholder="请输入新的群名称"
                  maxlength="20"
                  @keydown.enter="confirmRename"
                />
                <span class="qq-form-hint">{{ newGroupName.length }}/20</span>
              </div>
            </div>
            <div class="qq-modal-footer">
              <button class="qq-btn qq-btn-ghost" @click="showRenameDialog = false">取消</button>
              <button class="qq-btn qq-btn-primary" :disabled="!newGroupName.trim() || renaming" @click="confirmRename">
                {{ renaming ? '保存中...' : '确认修改' }}
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- 修改群头像弹窗 -->
    <Teleport to="body">
      <Transition name="modal-fade">
        <div v-if="showAvatarDialog" class="qq-modal-overlay" @click="closeAvatarDialog">
          <div class="qq-modal" @click.stop>
            <div class="qq-modal-header">
              <h3 class="qq-modal-title">修改群头像</h3>
              <button class="qq-modal-close" @click="closeAvatarDialog">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" /></svg>
              </button>
            </div>
            <div class="qq-modal-body">
              <div class="qq-avatar-upload" @click="triggerAvatarUpload">
                <img v-if="avatarPreview" :src="avatarPreview" class="qq-avatar-preview" />
                <div v-else class="qq-avatar-placeholder">
                  <svg class="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" /></svg>
                  <span>点击上传头像</span>
                </div>
              </div>
              <input ref="avatarInput" type="file" accept="image/*" style="display:none" @change="handleAvatarSelect" />
            </div>
            <div class="qq-modal-footer">
              <button class="qq-btn qq-btn-ghost" @click="closeAvatarDialog">取消</button>
              <button class="qq-btn qq-btn-primary" :disabled="!pendingAvatarFile || avatarUploading" @click="confirmAvatar">
                {{ avatarUploading ? '上传中...' : '确认修改' }}
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- 管理管理员弹窗 -->
    <Teleport to="body">
      <Transition name="modal-fade">
        <div v-if="showAdminDialog" class="qq-modal-overlay" @click="showAdminDialog = false">
          <div class="qq-modal qq-modal-wide" @click.stop>
            <div class="qq-modal-header">
              <h3 class="qq-modal-title">管理管理员</h3>
              <button class="qq-modal-close" @click="showAdminDialog = false">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" /></svg>
              </button>
            </div>
            <div class="qq-modal-body">
              <p class="qq-admin-hint">点击按钮切换管理员状态</p>
              <div class="qq-admin-list">
                <!-- 管理员列表 -->
                <div v-if="adminMembers.length > 0" class="qq-admin-section">
                  <div class="qq-admin-section-title">当前管理员</div>
                  <div v-for="m in adminMembers" :key="m.userId" class="qq-admin-item">
                    <img :src="m.userAvatar || defaultAvatar" class="qq-admin-avatar" />
                    <span class="qq-admin-name">{{ m.nickname || m.userName }}</span>
                    <button
                      class="qq-admin-toggle qq-admin-toggle-remove"
                      :disabled="adminLoading === m.userId"
                      @click="toggleAdmin(m, false)"
                    >
                      <svg v-if="adminLoading === m.userId" class="w-3.5 h-3.5 animate-spin" fill="none" viewBox="0 0 24 24"><circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle><path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path></svg>
                      <template v-else>取消管理员</template>
                    </button>
                  </div>
                </div>
                <!-- 普通成员列表 -->
                <div v-if="regularMembers.length > 0" class="qq-admin-section">
                  <div class="qq-admin-section-title">普通成员</div>
                  <div v-for="m in regularMembers" :key="m.userId" class="qq-admin-item">
                    <img :src="m.userAvatar || defaultAvatar" class="qq-admin-avatar" />
                    <span class="qq-admin-name">{{ m.nickname || m.userName }}</span>
                    <button
                      class="qq-admin-toggle qq-admin-toggle-add"
                      :disabled="adminLoading === m.userId"
                      @click="toggleAdmin(m, true)"
                    >
                      <svg v-if="adminLoading === m.userId" class="w-3.5 h-3.5 animate-spin" fill="none" viewBox="0 0 24 24"><circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle><path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path></svg>
                      <template v-else>设为管理员</template>
                    </button>
                  </div>
                </div>
                <div v-if="adminMembers.length === 0 && regularMembers.length === 0" class="qq-admin-empty">
                  暂无可管理的成员
                </div>
              </div>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- 搜索面板 -->
    <GroupSearchPanel
      v-if="showSearch"
      :group-id="group!.id"
      :members="members || []"
      @close="showSearch = false"
      @locate-message="handleLocateMessage"
    />

    <!-- 消息列表 -->
    <div ref="msgContainer" class="qq-chat-msgs" @scroll="onScroll">
      <!-- 加载更多 -->
      <div v-if="loadingMore" class="qq-load-more">
        <svg class="animate-spin w-4 h-4 text-[var(--zh-text-tertiary)]" fill="none" viewBox="0 0 24 24">
          <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
          <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
        </svg>
        <span class="text-xs text-[var(--zh-text-tertiary)] ml-1.5">加载中...</span>
      </div>
      <div v-else-if="!hasMore && messages.length > 0" class="qq-no-more">
        <span>—— 以上是全部消息 ——</span>
      </div>

      <!-- 消息列表 -->
      <template v-for="(msg, idx) in messages" :key="msg.id">
        <!-- 时间分隔线 -->
        <div v-if="shouldShowTimeSeparator(messages, idx)" class="qq-time-sep">
          <span>{{ formatChatTime(msg.createdAt) }}</span>
        </div>

        <!-- 系统消息 -->
        <div v-if="msg.messageType === 'system'" class="qq-system-msg">
          <span>{{ msg.content }}</span>
        </div>

        <!-- 普通消息 -->
        <div v-else class="qq-msg-row" :class="{ mine: msg.senderId === currentUserId }" :data-msg-id="msg.id">
          <!-- 他人消息（左） -->
          <template v-if="msg.senderId !== currentUserId">
            <img :src="msg.senderId === 0 ? AI_AVATAR_URL : (msg.senderAvatar || DEFAULT_AVATAR_URL)" class="qq-avatar qq-avatar-clickable" :alt="msg.senderName" @click="msg.senderId !== 0 && navigateToUser(msg.senderId)" />
            <ChatBubble
              :content="msg.content"
              :message-type="msg.messageType"
              :is-mine="false"
              :time="formatMsgTime(msg.createdAt)"
              :show-sender="true"
              :sender-name="msg.senderId === 0 ? 'AI助手' : (msg.senderName || '未知用户')"
              @preview-image="previewImage"
            >
              <template v-if="msg.messageType === 'text'" #default>
                <span v-html="renderMentions(msg.content, msg.mentionedUserIds)"></span>
              </template>
            </ChatBubble>
          </template>
          <!-- 自己消息（右） -->
          <template v-else>
            <ChatBubble
              :content="msg.content"
              :message-type="msg.messageType"
              :is-mine="true"
              :time="formatMsgTime(msg.createdAt)"
              :show-sender="true"
              :sender-name="currentUserName || '我'"
              @preview-image="previewImage"
            >
              <template v-if="msg.messageType === 'text'" #default>
                <span v-html="renderMentions(msg.content, msg.mentionedUserIds)"></span>
              </template>
            </ChatBubble>
            <img :src="currentUserAvatar || DEFAULT_AVATAR_URL" class="qq-avatar qq-avatar-clickable" alt="我" @click="navigateToUser(currentUserId)" />
          </template>
        </div>
      </template>

      <!-- AI助手思考中动画 -->
      <div v-if="aiReplying" class="qq-msg-row">
        <img :src="AI_AVATAR_URL" class="qq-avatar" alt="AI助手" />
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

      <div v-if="messages.length === 0 && !loadingMore" class="qq-empty">
        <svg class="w-12 h-12 text-[var(--zh-text-tertiary)] opacity-40 mb-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
        </svg>
        <p>暂无消息，发送第一条吧</p>
      </div>
    </div>

    <!-- 图片预览 -->
    <ImagePreviewOverlay :src="previewSrc" @close="previewSrc = ''" />

    <!-- 输入区域 -->
    <div class="qq-chat-input">
      <!-- 工具栏 -->
      <ChatToolbar
        :ai-mode="aiMode"
        :is-recording="voiceRecorder.isRecording"
        @emoji="(emoji: string) => insertEmoji(emoji)"
        @image="triggerImageUpload"
        @file="triggerFileUpload"
        @voice="startVoiceRecord"
        @ai="toggleAIMode"
      />
      <!-- 语音上传中提示 -->
      <div v-if="voiceUploading" class="qq-voice-uploading">
        <div class="w-3 h-3 border-2 border-blue-500 border-t-transparent rounded-full animate-spin"></div>
        <span>语音上传中...</span>
      </div>
      <!-- 语音录制中 - 替换输入框 -->
      <VoiceRecordingBar
        v-if="voiceRecorder.isRecording"
        :recording-time="voiceRecorder.recordingTime"
        @finish="finishVoiceRecord"
        @cancel="cancelVoiceRecord"
      />
      <!-- 输入框 + 发送 -->
      <div v-else class="qq-input-row">
        <div class="qq-input-wrap" :class="{ focused: inputFocused }">
          <!-- @提及下拉框 -->
          <div v-if="showMentionList" class="qq-mention-dropdown">
            <!-- AI助手选项 -->
            <div
              v-if="isAIMentionMatch"
              class="qq-mention-item qq-mention-ai"
              :class="{ active: mentionIndex === 0 }"
              @mousedown.prevent="selectAIMention()"
            >
              <img :src="AI_AVATAR_URL" class="qq-mention-avatar" />
              <span class="qq-mention-name">AI助手</span>
              <span class="qq-mention-badge">AI</span>
            </div>
            <div
              v-for="(member, mIdx) in filteredMembers"
              :key="member.userId"
              class="qq-mention-item"
              :class="{ active: mentionIndex === mIdx + (isAIMentionMatch ? 1 : 0) }"
              @mousedown.prevent="selectMention(member)"
            >
              <img :src="member.userAvatar || defaultAvatar" class="qq-mention-avatar" />
              <span class="qq-mention-name">{{ member.nickname || member.userName }}</span>
            </div>
            <div v-if="filteredMembers.length === 0 && !isAIMentionMatch" class="qq-mention-empty">无匹配成员</div>
          </div>
          <textarea
            ref="inputRef"
            v-model="inputText"
            class="qq-textarea"
            :placeholder="aiMode ? '向AI助手提问...' : '输入消息...'"
            rows="1"
            @focus="inputFocused = true"
            @blur="inputFocused = false"
            @keydown.enter.exact.prevent="sendMsg"
            @keydown.tab.prevent="handleMentionTab"
            @input="onInputChange"
          ></textarea>
        </div>
        <button
          class="qq-send-btn"
          :disabled="!canSend"
          @click="sendMsg"
          aria-label="发送"
        >
          <svg class="w-5 h-5" fill="currentColor" viewBox="0 0 24 24">
            <path d="M2.01 21L23 12 2.01 3 2 10l15 2-15 2z"/>
          </svg>
        </button>
      </div>
    </div>

    <!-- 隐藏的file input -->
    <input ref="imageInput" type="file" accept="image/*" style="display:none" @change="handleImageSelect" />
    <input ref="fileInput" type="file" accept=".doc,.docx,.pdf,.txt,.xls,.xlsx,.ppt,.pptx,.zip,.rar,.7z,.tar,.gz" style="display:none" @change="handleFileSelect" />

    <!-- 上传进度 -->
    <UploadOverlay :uploading="uploading" :progress="uploadProgress" />
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, watch, computed, onMounted } from 'vue'
import { groupApi } from '@/api/group'
import type { GroupInfo, GroupMessage, GroupMember } from '@/api/group'
import { fileApi } from '@/api/file'
import { sanitizeText } from '@/utils/sanitize'
import { showToast } from '@/composables/useToast'
import GroupSearchPanel from '@/components/GroupSearchPanel.vue'
import { useVoiceRecorder } from '@/composables/useVoiceRecorder'
import { useResourceUrl } from '@/composables/useResourceUrl'
import { useChatMedia } from '@/composables/chat/useChatMedia'
import { formatMsgTime, shouldShowTimeSeparator, formatChatTime } from '@/composables/chat/useChatTimestamp'
import { AI_AVATAR_URL, DEFAULT_AVATAR_URL } from '@/composables/chat/useChatConstants'
import ChatBubble from '@/components/chat/ChatBubble.vue'
import ChatToolbar from '@/components/chat/ChatToolbar.vue'
import VoiceRecordingBar from '@/components/chat/VoiceRecordingBar.vue'
import ImagePreviewOverlay from '@/components/chat/ImagePreviewOverlay.vue'
import UploadOverlay from '@/components/chat/UploadOverlay.vue'

const { resolveUrl } = useResourceUrl()
const { resolveMsgUrl, getVoiceUrl, getVoiceDuration } = useChatMedia()
const router = useRouter()
const route = useRoute()

/** 点击头像跳转用户主页（携带来源页面信息，确保返回按钮能回到群组页） */
const navigateToUser = (userId: number) => {
  router.push({ path: `/user/${userId}`, state: { from: route.fullPath } })
}

const props = defineProps<{
  group: GroupInfo | null
  currentUserId: number
  currentUserAvatar?: string
  currentUserName?: string
  initialMessages?: GroupMessage[]
  members?: GroupMember[]
}>()

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'toggleMembers'): void
  (e: 'messageSent', msg: GroupMessage): void
  (e: 'messagesLoaded', msgs: GroupMessage[]): void
  (e: 'leave'): void
  (e: 'dismiss'): void
  (e: 'groupUpdated'): void
}>()

const messages = ref<GroupMessage[]>([])
const inputText = ref('')
const msgContainer = ref<HTMLElement | null>(null)
const inputRef = ref<HTMLTextAreaElement | null>(null)
const imageInput = ref<HTMLInputElement | null>(null)
const fileInput = ref<HTMLInputElement | null>(null)
const loadingMore = ref(false)
const hasMore = ref(true)
const inputFocused = ref(false)
const showMenu = ref(false)
const showSearch = ref(false)
const previewSrc = ref('')
const aiMode = ref(false)
const aiReplying = ref(false)
const uploading = ref(false)
const uploadProgress = ref(0)
let offset = 0

// 弹窗状态
const showRenameDialog = ref(false)
const newGroupName = ref('')
const renaming = ref(false)
const showAvatarDialog = ref(false)
const avatarPreview = ref('')
const avatarUploading = ref(false)
const avatarInput = ref<HTMLInputElement | null>(null)
const showAdminDialog = ref(false)
const adminLoading = ref<number | null>(null)
const defaultAvatar = 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSI0MCIgaGVpZ2h0PSI0MCIgdmlld0JveD0iMCAwIDQwIDQwIj48cmVjdCB3aWR0aD0iNDAiIGhlaWdodD0iNDAiIGZpbGw9IiNlMmU4ZjAiIHJ4PSIyMCIvPjx0ZXh0IHg9IjIwIiB5PSIyNSIgdGV4dC1hbmNob3I9Im1pZGRsZSIgZmlsbD0iIzk0YTNiOCIgZm9udC1zaXplPSIxNiI+PzwvdGV4dD48L3N2Zz4='

// 权限判断
const isOwner = computed(() => props.group?.ownerId === props.currentUserId)
const isAdmin = computed(() => {
  const me = props.members?.find(m => m.userId === props.currentUserId)
  return me && me.role >= 1
})
const canManageGroup = computed(() => isOwner.value || isAdmin.value)

// 管理员弹窗用的成员列表
const adminMembers = computed(() => (props.members || []).filter(m => m.role === 1 && m.userId !== props.group?.ownerId))
const regularMembers = computed(() => (props.members || []).filter(m => m.role === 0))

function goBack() {
  router.push('/groups')
}

function openRenameDialog() {
  showMenu.value = false
  newGroupName.value = props.group?.name || ''
  showRenameDialog.value = true
}

async function confirmRename() {
  const name = newGroupName.value.trim()
  if (!name || !props.group) return
  renaming.value = true
  try {
    await groupApi.updateGroup(props.group.id, { name })
    showToast('群名已修改', 'success', { position: 'top-center' })
    showRenameDialog.value = false
    emit('groupUpdated')
  } catch (e: any) {
    showToast(e?.message || e?.response?.data?.message || '修改失败', 'error', { position: 'top-center' })
  } finally {
    renaming.value = false
  }
}

function openAvatarDialog() {
  showMenu.value = false
  // 用 resolveUrl 确保旧头像 URL 能正确预览（兼容旧 MinIO 直链等格式）
  avatarPreview.value = resolveUrl(props.group?.avatar) || ''
  pendingAvatarFile.value = null  // 每次打开弹窗重置，避免上次取消后残留
  showAvatarDialog.value = true
}

function closeAvatarDialog() {
  showAvatarDialog.value = false
  // 释放 blob URL 避免内存泄漏
  if (avatarPreview.value.startsWith('blob:')) {
    URL.revokeObjectURL(avatarPreview.value)
  }
  avatarPreview.value = ''
  pendingAvatarFile.value = null
}

function triggerAvatarUpload() {
  avatarInput.value?.click()
}

async function handleAvatarSelect(e: Event) {
  const file = (e.target as HTMLInputElement).files?.[0]
  if (!file) return
  ;(e.target as HTMLInputElement).value = ''
  // 释放旧的 blob URL
  if (avatarPreview.value.startsWith('blob:')) {
    URL.revokeObjectURL(avatarPreview.value)
  }
  // 预览
  avatarPreview.value = URL.createObjectURL(file)
  // 暂存文件用于上传
  pendingAvatarFile.value = file
}

const pendingAvatarFile = ref<File | null>(null)

async function confirmAvatar() {
  if (!props.group) return
  // 没有选择新文件，不需要更新
  if (!pendingAvatarFile.value) {
    closeAvatarDialog()
    return
  }
  avatarUploading.value = true
  try {
    const avatarUrl = await fileApi.uploadSingleImage(pendingAvatarFile.value)
    if (!avatarUrl) {
      showToast('图片上传失败，服务器未返回地址', 'error', { position: 'top-center' })
      return
    }
    await groupApi.updateGroup(props.group.id, { avatar: avatarUrl })
    showToast('群头像已修改', 'success', { position: 'top-center' })
    closeAvatarDialog()
    emit('groupUpdated')
  } catch (e: any) {
    showToast(e?.message || e?.response?.data?.message || '修改失败', 'error', { position: 'top-center' })
  } finally {
    avatarUploading.value = false
  }
}

function openAdminDialog() {
  showMenu.value = false
  showAdminDialog.value = true
}

async function toggleAdmin(member: GroupMember, setAdmin: boolean) {
  if (!props.group) return
  adminLoading.value = member.userId
  try {
    await groupApi.setAdmin(props.group.id, member.userId, setAdmin)
    showToast(setAdmin ? '已设为管理员' : '已取消管理员', 'success', { position: 'top-center' })
    emit('groupUpdated')
  } catch (e: any) {
    showToast(e?.message || e?.response?.data?.message || '操作失败', 'error', { position: 'top-center' })
  } finally {
    adminLoading.value = null
  }
}

function handleDismissGroup() {
  showMenu.value = false
  if (confirm(`确定要解散群组「${props.group?.name}」吗？此操作不可恢复。`)) {
    emit('dismiss')
  }
}

/** 语音录制 — reactive() 包裹使模板中 voiceRecorder.isRecording 自动解包为 boolean */
const voiceRecorder = reactive(useVoiceRecorder())
const voiceUploading = ref(false)

const startVoiceRecord = async () => {
  await voiceRecorder.startRecording()
  if (voiceRecorder.recordError) {
    showToast(voiceRecorder.recordError, 'error', { position: 'top-center' })
  }
}

const finishVoiceRecord = async () => {
  const duration = voiceRecorder.recordingTime
  const finalBlob = await voiceRecorder.stopRecording()
  if (!finalBlob) { cancelVoiceRecord(); return }
  voiceUploading.value = true
  try {
    const voiceUrl = await fileApi.uploadSingleVoice(finalBlob)
    if (!voiceUrl) throw new Error('语音上传失败')
    const content = JSON.stringify({ url: voiceUrl, duration: duration || 0 })
    if (props.group) {
      const res = await groupApi.sendMessage(props.group.id, content, 'voice')
      const msg = res.data.data
      if (msg && !messages.value.some(m => m.id === msg.id)) {
        messages.value.push(msg)
        emit('messageSent', msg)
      }
      await nextTick()
      scrollToBottom()
    }
  } catch (err: any) {
    showToast(err.message || '语音发送失败', 'error', { position: 'top-center' })
  } finally {
    voiceUploading.value = false
    voiceRecorder.cancelRecording()
  }
}

const cancelVoiceRecord = () => { voiceRecorder.cancelRecording() }

// @提及相关
const showMentionList = ref(false)
const mentionIndex = ref(0)
const mentionQuery = ref('')
const mentionedIds = ref<number[]>([])
let mentionStartPos = 0

const canSend = computed(() => inputText.value.trim().length > 0)

/** 判断 @ 输入是否匹配 AI助手（空查询或输入包含 'ai'/'AI'/'助手' 等关键词时显示） */
const isAIMentionMatch = computed(() => {
  const q = mentionQuery.value.toLowerCase()
  if (!q) return true // 空查询时总是显示 AI助手 选项
  return 'ai助手'.includes(q) || 'ai'.includes(q) || '助手'.includes(q)
})

const filteredMembers = computed(() => {
  if (!props.members) return []
  const q = mentionQuery.value.toLowerCase()
  return props.members.filter(m => {
    if (m.userId === props.currentUserId) return false
    const name = (m.nickname || m.userName || '').toLowerCase()
    return !q || name.includes(q)
  }).slice(0, 6)
})

// 外部传入消息（WebSocket推送）
function addExternalMessage(msg: GroupMessage) {
  if (messages.value.some(m => m.id === msg.id)) return
  messages.value.push(msg)
  // AI回复到达时清除加载指示器
  if (msg.messageType === 'ai_reply' || msg.senderId === 0) {
    aiReplying.value = false
  }
  nextTick(scrollToBottom)
}

// 滚动到指定消息
function scrollToMessage(msgId: number) {
  const el = msgContainer.value
  if (!el) return
  const target = el.querySelector(`[data-msg-id="${msgId}"]`) as HTMLElement
  if (target) {
    target.scrollIntoView({ behavior: 'smooth', block: 'center' })
    target.classList.add('qq-highlight-flash')
    setTimeout(() => target.classList.remove('qq-highlight-flash'), 2000)
  }
}

defineExpose({ addExternalMessage, scrollToMessage })

async function loadMessages(gid: number) {
  try {
    const res = await groupApi.getMessages(gid, offset, 50)
    const data = res.data.data
    if (offset === 0) {
      messages.value = data.reverse()
      emit('messagesLoaded', messages.value)
    } else {
      messages.value = [...data.reverse(), ...messages.value]
    }
    hasMore.value = data.length === 50
    if (offset === 0) {
      await nextTick()
      scrollToBottom()
    }
  } catch { /* silent */ }
}

async function sendMsg() {
  const rawText = inputText.value.trim()
  if (!rawText || !props.group) return

  // 检测是否为@AI助手模式
  const isAIBotMention = rawText.includes('@AI助手')
  if (aiMode.value || isAIBotMention) {
    // 构造带 @AI助手 前缀的消息内容
    const question = rawText.replace(/@AI助手\s*/g, '').trim()
    if (!question) return
    const contentWithPrefix = '@AI助手 ' + question

    // 1. 先发送用户消息（作为普通群消息，所有人可见）
    const sanitizedText = sanitizeText(contentWithPrefix)
    try {
      const res = await groupApi.sendMessage(props.group.id, sanitizedText, 'text')
      const msg = res.data.data
      if (msg && !messages.value.some(m => m.id === msg.id)) {
        messages.value.push(msg)
        emit('messageSent', msg)
      }
    } catch { /* silent */ }

    // 2. 显示AI思考指示器
    aiReplying.value = true
    resetInput()
    await nextTick()
    scrollToBottom()

    // 3. 异步触发AI回复（不等待HTTP响应，AI回复通过WebSocket推送）
    triggerAIReply(question)
    return
  }

  const sanitizedText = sanitizeText(rawText)
  const ids = mentionedIds.value.length > 0 ? [...mentionedIds.value] : undefined
  try {
    const res = await groupApi.sendMessage(props.group.id, sanitizedText, 'text', ids)
    const msg = res.data.data
    if (msg && !messages.value.some(m => m.id === msg.id)) {
      messages.value.push(msg)
      emit('messageSent', msg)
    }
    resetInput()
  } catch { /* silent */ }
}

/** 异步触发AI回复（fire-and-forget），AI回复通过WebSocket广播到群 */
function triggerAIReply(question: string) {
  if (!props.group) return
  groupApi.sendAIMessage(props.group.id, question).catch(() => {
    // AI触发失败时清除加载指示器并提示
    aiReplying.value = false
    showToast('AI回复请求失败，请稍后重试', 'error', { position: 'top-center' })
  })
}

function resetInput() {
  inputText.value = ''
  mentionedIds.value = []
  aiMode.value = false
  if (inputRef.value) inputRef.value.style.height = 'auto'
  await_nextTick_scroll()
}

async function await_nextTick_scroll() {
  await nextTick()
  scrollToBottom()
}

function scrollToBottom() {
  if (msgContainer.value) {
    msgContainer.value.scrollTop = msgContainer.value.scrollHeight
  }
}

function onScroll() {
  const el = msgContainer.value
  if (!el || !hasMore.value || loadingMore.value) return
  if (el.scrollTop < 50) {
    loadingMore.value = true
    const prevHeight = el.scrollHeight
    offset += 50
    loadMessages(props.group!.id).then(() => {
      nextTick(() => {
        if (el) el.scrollTop = el.scrollHeight - prevHeight
        loadingMore.value = false
      })
    })
  }
}

function autoResize() {
  const el = inputRef.value
  if (!el) return
  el.style.height = 'auto'
  el.style.height = Math.min(el.scrollHeight, 120) + 'px'
}

// ========== @提及 ==========

function onInputChange() {
  autoResize()
  detectMention()
}

function detectMention() {
  const text = inputText.value
  const cursorPos = inputRef.value?.selectionStart || text.length
  const before = text.substring(0, cursorPos)
  const match = before.match(/@([^\s@]*)$/)
  if (match) {
    showMentionList.value = true
    mentionQuery.value = match[1]
    mentionStartPos = cursorPos - match[0].length
    mentionIndex.value = 0
  } else {
    showMentionList.value = false
    mentionQuery.value = ''
  }
}

function selectMention(member: GroupMember) {
  const name = member.nickname || member.userName || '未知'
  const text = inputText.value
  const before = text.substring(0, mentionStartPos)
  const after = text.substring(inputRef.value?.selectionStart || text.length)
  inputText.value = before + '@' + name + ' ' + after
  mentionedIds.value.push(member.userId)
  showMentionList.value = false
  nextTick(() => {
    if (inputRef.value) {
      const pos = before.length + name.length + 2
      inputRef.value.selectionStart = pos
      inputRef.value.selectionEnd = pos
      inputRef.value.focus()
    }
  })
}

/** 选择 @AI助手 从提及列表 */
function selectAIMention() {
  const text = inputText.value
  const before = text.substring(0, mentionStartPos)
  const after = text.substring(inputRef.value?.selectionStart || text.length)
  inputText.value = before + '@AI助手 ' + after
  showMentionList.value = false
  aiMode.value = true
  nextTick(() => {
    if (inputRef.value) {
      const pos = before.length + 7 // '@AI助手 ' 长度
      inputRef.value.selectionStart = pos
      inputRef.value.selectionEnd = pos
      inputRef.value.focus()
    }
  })
}

function handleMentionTab() {
  if (!showMentionList.value) return
  const aiOffset = isAIMentionMatch.value ? 1 : 0
  if (mentionIndex.value === 0 && isAIMentionMatch.value) {
    selectAIMention()
  } else {
    const memberIdx = mentionIndex.value - aiOffset
    if (memberIdx >= 0 && memberIdx < filteredMembers.value.length) {
      selectMention(filteredMembers.value[memberIdx])
    }
  }
}

function isMentioned(msg: GroupMessage): boolean {
  return msg.mentionedUserIds?.includes(props.currentUserId) || false
}

function renderMentions(content: string, userIds?: number[]): string {
  if (!content) return ''
  // 高亮 @AI助手 和 @用户名 文本
  return content
    .replace(/@AI助手/g, '<span class="qq-mention-tag qq-mention-ai-tag">@AI助手</span>')
    .replace(/@(\S+?)(?=\s|$)/g, (match, name) => {
      if (name === 'AI助手') return match // 已经处理过
      return `<span class="qq-mention-tag">@${name}</span>`
    })
}

// ========== 工具栏 ==========

function insertEmoji(emoji: string) {
  inputText.value += emoji
  nextTick(() => inputRef.value?.focus())
}

function toggleAIMode() {
  aiMode.value = !aiMode.value
  nextTick(() => inputRef.value?.focus())
}

function triggerImageUpload() {
  imageInput.value?.click()
}

function triggerFileUpload() {
  fileInput.value?.click()
}

async function handleImageSelect(e: Event) {
  const file = (e.target as HTMLInputElement).files?.[0]
  if (!file || !props.group) return
  // 重置input
  (e.target as HTMLInputElement).value = ''
  // 校验文件类型和大小
  if (!file.type.startsWith('image/')) {
    showToast('请选择图片文件', 'error', { position: 'top-center' })
    return
  }
  if (file.size > 5 * 1024 * 1024) {
    showToast('图片大小不能超过5MB', 'error', { position: 'top-center' })
    return
  }
  uploading.value = true
  uploadProgress.value = 0
  try {
    const url = await fileApi.uploadSingleImage(file, (p) => { uploadProgress.value = p })
    const res = await groupApi.sendMessage(props.group.id, url, 'image')
    const msg = res.data.data
    if (msg && !messages.value.some(m => m.id === msg.id)) {
      messages.value.push(msg)
      emit('messageSent', msg)
    }
    await nextTick()
    scrollToBottom()
  } catch {
    showToast('图片上传失败', 'error', { position: 'top-center' })
  } finally {
    uploading.value = false
    uploadProgress.value = 0
  }
}

async function handleFileSelect(e: Event) {
  const file = (e.target as HTMLInputElement).files?.[0]
  if (!file || !props.group) return
  (e.target as HTMLInputElement).value = ''

  // 校验文件大小（20MB）
  if (file.size > 20 * 1024 * 1024) {
    showToast('文件大小不能超过20MB', 'error', { position: 'top-center' })
    return
  }

  // 校验文件扩展名
  const allowedExts = ['doc', 'docx', 'pdf', 'txt', 'xls', 'xlsx', 'ppt', 'pptx', 'zip', 'rar', '7z', 'tar', 'gz']
  const ext = file.name.split('.').pop()?.toLowerCase() || ''
  if (!allowedExts.includes(ext)) {
    showToast('不支持该文件格式，仅支持 Word/PDF/TXT/Excel/PPT/压缩包', 'error', { position: 'top-center' })
    return
  }

  uploading.value = true
  uploadProgress.value = 0
  try {
    const formData = new FormData()
    formData.append('file', file)
    const res = await fileApi.uploadFile(formData, (p) => { uploadProgress.value = p })
    const url = res.data.data
    const content = JSON.stringify({ url, name: file.name, size: file.size })
    const sendRes = await groupApi.sendMessage(props.group.id, content, 'file')
    const msg = sendRes.data.data
    if (msg && !messages.value.some(m => m.id === msg.id)) {
      messages.value.push(msg)
      emit('messageSent', msg)
    }
    await nextTick()
    scrollToBottom()
  } catch {
    showToast('文件上传失败', 'error', { position: 'top-center' })
  } finally {
    uploading.value = false
    uploadProgress.value = 0
  }
}

function previewImage(src: string) {
  previewSrc.value = src
}

function handleLeaveGroup() {
  showMenu.value = false
  if (confirm(`确定要退出群组「${props.group?.name}」吗？`)) {
    emit('leave')
  }
}

function handleLocateMessage(msgId: number) {
  showSearch.value = false
  scrollToMessage(msgId)
}

// 监听group变化重新加载
watch(() => props.group?.id, (id) => {
  if (id) {
    offset = 0
    messages.value = []
    hasMore.value = true
    loadMessages(id)
  }
}, { immediate: true })
</script>

<style scoped>
.qq-chat {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: var(--zh-bg-elevated, #fff);
  overflow: hidden;
}

/* ===== 头部 ===== */
.qq-chat-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  border-bottom: 1px solid var(--zh-border, #e5e7eb);
  flex-shrink: 0;
  background: var(--zh-bg-elevated, #fff);
  z-index: 2;
  position: sticky;
  top: 0;
}
.qq-back-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px; height: 36px;
  border: none; border-radius: 10px;
  background: transparent;
  color: var(--zh-text-secondary, #64748b);
  cursor: pointer; transition: background 0.2s;
  flex-shrink: 0;
}
.qq-back-btn:hover { background: var(--zh-bg-hover, #f1f5f9); }
.qq-header-info { flex: 1; min-width: 0; }
.qq-chat-title {
  font-weight: 700; font-size: 15px;
  color: var(--zh-text, #1e293b);
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.qq-chat-subtitle {
  font-size: 11px; color: var(--zh-text-tertiary, #94a3b8);
  display: flex; gap: 6px; margin-top: 1px;
}
.qq-group-num { font-family: 'JetBrains Mono', monospace; }
.qq-header-actions { display: flex; gap: 4px; flex-shrink: 0; }
.qq-header-btn {
  display: flex; align-items: center; justify-content: center;
  width: 36px; height: 36px;
  border: none; border-radius: 10px;
  background: transparent;
  color: var(--zh-text-tertiary, #94a3b8);
  cursor: pointer; transition: all 0.2s;
  flex-shrink: 0;
}
.qq-header-btn:hover {
  background: var(--zh-bg-hover, #f1f5f9);
  color: var(--zh-text-secondary, #64748b);
}

/* ===== 下拉菜单 ===== */
.qq-menu-overlay {
  position: fixed; inset: 0; z-index: 9998;
  background: rgba(0, 0, 0, 0.15);
}
.qq-menu {
  position: absolute; top: 52px; right: 14px;
  background: var(--zh-bg-elevated, #fff);
  border: 1px solid var(--zh-border, #e5e7eb);
  border-radius: 14px;
  box-shadow: 0 12px 36px rgba(0,0,0,0.15);
  padding: 6px; min-width: 180px;
  z-index: 9999;
  backdrop-filter: blur(20px);
}
.qq-menu-section { padding: 2px 0; }
.qq-menu-section + .qq-menu-section {
  border-top: 1px solid var(--zh-border, #f1f5f9);
  margin-top: 2px; padding-top: 4px;
}
.qq-menu-section-title {
  font-size: 10px; font-weight: 600;
  color: var(--zh-text-tertiary, #94a3b8);
  padding: 4px 12px 2px;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}
.qq-menu-item {
  display: flex; align-items: center; gap: 8px;
  width: 100%; padding: 9px 12px;
  border: none; border-radius: 8px;
  background: transparent;
  color: var(--zh-text, #1e293b);
  font-size: 13px; cursor: pointer;
  transition: background 0.15s;
}
.qq-menu-item:hover { background: var(--zh-bg-hover, #f1f5f9); }
.qq-menu-item-danger { color: #ef4444; }
.qq-menu-item-danger:hover { background: #fef2f2; }

/* 菜单动画 */
.menu-fade-enter-active,
.menu-fade-leave-active {
  transition: opacity 0.15s ease;
}
.menu-fade-enter-from,
.menu-fade-leave-to {
  opacity: 0;
}

/* ===== 弹窗通用 ===== */
.qq-modal-overlay {
  position: fixed; inset: 0; z-index: 10000;
  background: rgba(0, 0, 0, 0.45);
  display: flex; align-items: center; justify-content: center;
  padding: 16px;
  backdrop-filter: blur(4px);
}
.qq-modal {
  background: var(--zh-bg-elevated, #fff);
  border-radius: 18px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.2);
  width: 100%; max-width: 400px;
  overflow: hidden;
  animation: modalSlideIn 0.25s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.qq-modal-wide { max-width: 480px; }
@keyframes modalSlideIn {
  from { opacity: 0; transform: scale(0.92) translateY(10px); }
  to { opacity: 1; transform: scale(1) translateY(0); }
}
.qq-modal-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 18px 20px 0;
}
.qq-modal-title {
  font-size: 16px; font-weight: 700;
  color: var(--zh-text, #1e293b);
  margin: 0;
}
.qq-modal-close {
  display: flex; align-items: center; justify-content: center;
  width: 28px; height: 28px;
  border: none; border-radius: 8px;
  background: transparent;
  color: var(--zh-text-tertiary, #94a3b8);
  cursor: pointer; transition: all 0.15s;
}
.qq-modal-close:hover {
  background: var(--zh-bg-hover, #f1f5f9);
  color: var(--zh-text-secondary, #64748b);
}
.qq-modal-body { padding: 16px 20px; }
.qq-modal-footer {
  display: flex; gap: 10px; justify-content: flex-end;
  padding: 12px 20px 18px;
}

/* 弹窗动画 */
.modal-fade-enter-active,
.modal-fade-leave-active {
  transition: opacity 0.2s ease;
}
.modal-fade-enter-from,
.modal-fade-leave-to {
  opacity: 0;
}

/* ===== 表单 ===== */
.qq-form-group {
  display: flex; flex-direction: column; gap: 6px;
}
.qq-form-label {
  font-size: 13px; font-weight: 600;
  color: var(--zh-text-secondary, #64748b);
}
.qq-form-input {
  width: 100%; padding: 10px 14px;
  border: 1.5px solid var(--zh-border, #e5e7eb);
  border-radius: 12px;
  font-size: 14px; color: var(--zh-text, #1e293b);
  background: var(--zh-bg, #f8fafc);
  outline: none; transition: border-color 0.2s, box-shadow 0.2s;
}
.qq-form-input:focus {
  border-color: var(--zh-primary, #6366f1);
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.08);
}
.qq-form-input::placeholder {
  color: var(--zh-text-placeholder, #cbd5e1);
}
.qq-form-hint {
  font-size: 11px; color: var(--zh-text-tertiary, #94a3b8);
  text-align: right;
}

/* ===== 按钮 ===== */
.qq-btn {
  padding: 8px 20px;
  border: none; border-radius: 10px;
  font-size: 13px; font-weight: 600;
  cursor: pointer; transition: all 0.2s;
  min-height: 0;
}
.qq-btn-ghost {
  background: transparent;
  color: var(--zh-text-secondary, #64748b);
}
.qq-btn-ghost:hover { background: var(--zh-bg-hover, #f1f5f9); }
.qq-btn-primary {
  background: var(--zh-primary, #6366f1);
  color: #fff;
}
.qq-btn-primary:hover:not(:disabled) { opacity: 0.9; }
.qq-btn-primary:disabled { opacity: 0.4; cursor: not-allowed; }

/* ===== 头像上传 ===== */
.qq-avatar-upload {
  width: 120px; height: 120px;
  margin: 0 auto;
  border-radius: 20px;
  border: 2px dashed var(--zh-border, #e5e7eb);
  overflow: hidden; cursor: pointer;
  transition: border-color 0.2s;
  display: flex; align-items: center; justify-content: center;
}
.qq-avatar-upload:hover { border-color: var(--zh-primary, #6366f1); }
.qq-avatar-preview {
  width: 100%; height: 100%;
  object-fit: cover;
}
.qq-avatar-placeholder {
  display: flex; flex-direction: column;
  align-items: center; gap: 6px;
  color: var(--zh-text-tertiary, #94a3b8);
  font-size: 12px;
}

/* ===== 管理员管理 ===== */
.qq-admin-hint {
  font-size: 12px; color: var(--zh-text-tertiary, #94a3b8);
  margin: 0 0 12px;
}
.qq-admin-list {
  max-height: 320px; overflow-y: auto;
}
.qq-admin-section { margin-bottom: 12px; }
.qq-admin-section-title {
  font-size: 11px; font-weight: 700;
  color: var(--zh-text-tertiary, #94a3b8);
  padding: 4px 0;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}
.qq-admin-item {
  display: flex; align-items: center; gap: 10px;
  padding: 8px 0;
  border-bottom: 1px solid var(--zh-border, #f1f5f9);
}
.qq-admin-item:last-child { border-bottom: none; }
.qq-admin-avatar {
  width: 32px; height: 32px;
  border-radius: 50%; object-fit: cover;
  flex-shrink: 0;
}
.qq-admin-name {
  flex: 1; font-size: 13px; font-weight: 500;
  color: var(--zh-text, #1e293b);
  white-space: nowrap; overflow: hidden;
  text-overflow: ellipsis;
}
.qq-admin-toggle {
  padding: 5px 12px; border: none;
  border-radius: 8px; font-size: 11px;
  font-weight: 600; cursor: pointer;
  transition: all 0.2s; white-space: nowrap;
  display: flex; align-items: center; gap: 4px;
  min-height: 0;
}
.qq-admin-toggle:disabled { opacity: 0.5; cursor: not-allowed; }
.qq-admin-toggle-add {
  background: rgba(59, 130, 246, 0.1);
  color: #3b82f6;
}
.qq-admin-toggle-add:hover:not(:disabled) {
  background: rgba(59, 130, 246, 0.2);
}
.qq-admin-toggle-remove {
  background: rgba(245, 158, 11, 0.1);
  color: #d97706;
}
.qq-admin-toggle-remove:hover:not(:disabled) {
  background: rgba(245, 158, 11, 0.2);
}
.qq-admin-empty {
  text-align: center; padding: 20px;
  font-size: 12px; color: var(--zh-text-tertiary, #94a3b8);
}

/* ===== 消息列表 ===== */
.qq-chat-msgs {
  flex: 1;
  min-height: 0; /* 2026-07-02 v8: 修复内容超出时撑高容器导致 input 错位 */
  overflow-y: auto;
  overflow-x: hidden;
  padding: 8px 14px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  -webkit-overflow-scrolling: touch;
}
.qq-load-more, .qq-no-more {
  text-align: center; padding: 10px 0;
  font-size: 12px; color: var(--zh-text-tertiary, #94a3b8);
  display: flex; align-items: center; justify-content: center;
}

/* 时间分隔线 */
.qq-time-sep { text-align: center; padding: 12px 0 6px; }
.qq-time-sep span {
  font-size: 11px; color: var(--zh-text-tertiary, #94a3b8);
  background: var(--zh-bg, #f8fafc);
  padding: 2px 10px; border-radius: 10px;
}

/* 系统消息 */
.qq-system-msg { text-align: center; padding: 6px 0; }
.qq-system-msg span {
  font-size: 12px; color: var(--zh-text-tertiary, #94a3b8);
  background: var(--zh-bg-hover, #f1f5f9);
  padding: 3px 12px; border-radius: 12px;
}

/* 消息行 - 与私信保持一致 */
.qq-msg-row {
  display: flex; align-items: flex-start; gap: 6px; padding: 4px 0;
}
.qq-msg-row.mine { justify-content: flex-end; }

/* 头像 */
.qq-avatar {
  width: 32px; height: 32px;
  border-radius: 50%; flex-shrink: 0;
  object-fit: cover; margin-top: 2px;
}
.qq-avatar-clickable {
  cursor: pointer;
  transition: opacity 0.15s ease;
}
.qq-avatar-clickable:hover {
  opacity: 0.8;
}

/* @提及标签 */
:deep(.qq-mention-tag) {
  color: var(--zh-primary, #6366f1);
  font-weight: 500;
}

/* AI助手提及标签（特殊颜色） */
:deep(.qq-mention-ai-tag) {
  color: #6366f1;
  font-weight: 600;
  background: rgba(99, 102, 241, 0.1);
  padding: 1px 4px;
  border-radius: 4px;
}

/* 提及列表中的AI选项 */
.qq-mention-ai {
  border-bottom: 1px solid var(--zh-border, #e5e7eb);
}
.qq-mention-badge {
  margin-left: auto;
  font-size: 10px;
  font-weight: 700;
  color: #fff;
  background: var(--zh-primary, #6366f1);
  padding: 1px 6px;
  border-radius: 8px;
  line-height: 1.4;
}

/* 空状态 */
.qq-empty {
  display: flex; flex-direction: column;
  align-items: center; justify-content: center;
  flex: 1; color: var(--zh-text-tertiary, #94a3b8); font-size: 14px;
}

/* ===== 输入区域 ===== */
.qq-chat-input {
  padding: 6px 14px 10px;
  padding-bottom: calc(10px + env(safe-area-inset-bottom, 0px));
  border-top: 1px solid var(--zh-border, #e5e7eb);
  background: var(--zh-bg-elevated, #fff);
  flex-shrink: 0;
}

.qq-input-row {
  display: flex; align-items: flex-end; gap: 8px;
}
.qq-input-wrap {
  flex: 1; min-height: 38px; max-height: 120px;
  border: 1.5px solid var(--zh-border, #e5e7eb);
  border-radius: 20px;
  background: var(--zh-bg, #f8fafc);
  overflow: visible; position: relative;
  transition: border-color 0.2s, box-shadow 0.2s;
}
.qq-input-wrap.focused {
  border-color: var(--zh-primary, #6366f1);
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.08);
}
.qq-textarea {
  width: 100%; min-height: 38px; max-height: 120px;
  border: none; outline: none; background: transparent;
  padding: 8px 16px;
  font-size: 14px; color: var(--zh-text, #1e293b);
  font-family: inherit; resize: none; line-height: 1.5;
}
.qq-textarea::placeholder {
  color: var(--zh-text-placeholder, #cbd5e1); font-size: 13px;
}
.qq-send-btn {
  display: flex; align-items: center; justify-content: center;
  width: 38px; height: 38px;
  border: none; border-radius: 50%;
  background: var(--zh-primary, #6366f1);
  color: #fff; cursor: pointer;
  transition: opacity 0.2s, transform 0.15s;
  flex-shrink: 0; min-height: 0; min-width: 0;
}
.qq-send-btn:hover:not(:disabled) { opacity: 0.9; transform: scale(1.05); }
.qq-send-btn:active:not(:disabled) { transform: scale(0.95); }
.qq-send-btn:disabled { opacity: 0.35; cursor: not-allowed; }

/* ===== @提及下拉框 ===== */
.qq-mention-dropdown {
  position: absolute; bottom: 100%; left: 8px; right: 8px;
  background: var(--zh-bg-elevated, #fff);
  border: 1px solid var(--zh-border, #e5e7eb);
  border-radius: 10px;
  box-shadow: 0 -4px 16px rgba(0,0,0,0.1);
  max-height: 200px; overflow-y: auto;
  z-index: 10; margin-bottom: 4px;
}
.qq-mention-item {
  display: flex; align-items: center; gap: 8px;
  padding: 8px 12px; cursor: pointer;
  transition: background 0.1s;
}
.qq-mention-item:hover, .qq-mention-item.active {
  background: var(--zh-bg-hover, #f1f5f9);
}
.qq-mention-avatar {
  width: 24px; height: 24px; border-radius: 50%;
  object-fit: cover;
}
.qq-mention-name {
  font-size: 13px; color: var(--zh-text, #1e293b);
}
.qq-mention-empty {
  padding: 12px; text-align: center;
  font-size: 12px; color: var(--zh-text-tertiary, #94a3b8);
}

/* ===== 高亮闪烁 ===== */
:deep(.qq-highlight-flash) {
  animation: highlightPulse 2s ease-out;
}
@keyframes highlightPulse {
  0% { background: rgba(99, 102, 241, 0.2); }
  100% { background: transparent; }
}

/* 移动端适配 */
@media (max-width: 768px) {
  .qq-chat-header { padding: 8px 10px; }
  .qq-chat-msgs { padding: 6px 10px; }
  .qq-chat-input { padding: 6px 10px 8px; padding-bottom: calc(8px + env(safe-area-inset-bottom, 0px)); }
  .qq-avatar { width: 28px; height: 28px; }
  .qq-menu { top: 48px; right: 10px; }
}

/* 语音上传中 */
.qq-voice-uploading {
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
.dark .qq-voice-uploading {
  background: rgba(37, 99, 235, 0.15);
  color: #93c5fd;
}

/* AI思考中动画 */
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
  font-weight: 600;
  color: var(--zh-primary, #6366f1);
}
.ai-thinking-text {
  font-size: 13px;
  color: var(--zh-text-secondary, #64748b);
}
.ai-thinking-dots {
  display: inline-flex;
  align-items: center;
  gap: 3px;
}
.ai-thinking-dots .dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: var(--zh-primary, #6366f1);
  animation: aiDotBounce 1.2s ease-in-out infinite;
}
.ai-thinking-dots .dot:nth-child(2) { animation-delay: 0.15s; }
.ai-thinking-dots .dot:nth-child(3) { animation-delay: 0.3s; }
@keyframes aiDotBounce {
  0%, 60%, 100% { transform: translateY(0); opacity: 0.4; }
  30% { transform: translateY(-4px); opacity: 1; }
}
</style>
