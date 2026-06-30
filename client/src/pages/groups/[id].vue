<template>
  <!-- QQ风格群组聊天页 -->
  <div class="group-page">
    <!-- 加载状态 -->
    <div v-if="loading" class="group-page-loading">
      <div class="loading-spinner"></div>
    </div>

    <!-- 群组不存在 -->
    <div v-else-if="!group" class="group-page-empty">
      <svg class="w-16 h-16 text-[var(--zh-text-tertiary)] opacity-30 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
      </svg>
      <p class="text-[var(--zh-text-tertiary)] mb-3">群组不存在或已解散</p>
      <RouterLink to="/groups" class="text-primary hover:underline text-sm">返回群组广场</RouterLink>
    </div>

    <!-- 非成员：显示群组信息 + 申请加入按钮 -->
    <div v-else-if="!isMember" class="group-page-non-member">
      <div class="group-info-card">
        <img v-if="group.avatar" :src="group.avatar" class="group-info-avatar" alt="群头像" />
        <div v-else class="group-info-avatar group-info-avatar-placeholder">
          {{ group.name?.charAt(0) || '?' }}
        </div>
        <h2 class="group-info-name">{{ group.name }}</h2>
        <p v-if="group.description" class="group-info-desc">{{ group.description }}</p>
        <div class="group-info-meta">
          <span>群号：{{ group.groupNumber }}</span>
          <span>{{ group.memberCount }}/{{ group.maxMembers }} 人</span>
        </div>
        <button
          class="group-join-btn"
          :disabled="requestSent"
          @click="handleRequestJoin"
        >
          {{ requestSent ? '申请已发送，等待审批' : '申请加入群组' }}
        </button>
        <p v-if="joinError" class="group-join-error">{{ joinError }}</p>
      </div>
    </div>

    <!-- 聊天主界面（仅成员可见） -->
    <ClientOnly v-else>
      <div class="group-page-layout">
        <!-- 聊天区域（中栏） -->
        <div class="group-page-chat">
          <GroupChatWindow
            ref="chatWindow"
            :group="group"
            :current-user-id="currentUserId"
            :current-user-avatar="currentUserAvatar"
            :current-user-name="currentUserName"
            :members="members"
            @close="goBack"
            @toggle-members="showMembers = !showMembers"
            @message-sent="onMessageSent"
            @leave="handleLeave"
            @dismiss="handleDismiss"
            @group-updated="onGroupUpdated"
          />
        </div>

        <!-- 成员面板（右栏 - 桌面端常驻，移动端抽屉） -->
        <div v-if="showMembers || !isMobile" class="group-page-members">
          <GroupMemberPanel
            :group="group"
            :members="members"
            :loading="membersLoading"
            :current-user-id="currentUserId"
            :mobile-open="showMembers && isMobile"
            @close="showMembers = false"
            @leave="handleLeave"
            @dismiss="handleDismiss"
            @member-changed="onMemberChanged"
          />
        </div>
      </div>
    </ClientOnly>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch, onUnmounted } from 'vue'
import { groupApi } from '@/api/group'
import type { GroupInfo, GroupMessage, GroupMember } from '@/api/group'
import GroupChatWindow from '@/components/GroupChatWindow.vue'
import GroupMemberPanel from '@/components/GroupMemberPanel.vue'
import { useGroupWebSocket } from '@/composables/useGroupWebSocket'
import { showToast } from '@/composables/useToast'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const groupId = computed(() => Number(route.params.id))
const currentUserId = computed(() => userStore.userInfo?.id || 0)
const currentUserAvatar = computed(() => userStore.userInfo?.avatar || '')
const currentUserName = computed(() => userStore.userInfo?.nickname || '')
const isMobile = ref(window.innerWidth <= 768)

const group = ref<GroupInfo | null>(null)
const loading = ref(true)
const showMembers = ref(false)
const members = ref<GroupMember[]>([])
const membersLoading = ref(false)
const chatWindow = ref<InstanceType<typeof GroupChatWindow> | null>(null)

// 非成员申请入群相关
const requestSent = ref(false)
const joinError = ref('')

// 判断当前用户是否为群成员（myRole 为 null/undefined 表示非成员）
const isMember = computed(() => group.value?.myRole != null)

let wsCleanup: (() => void) | null = null

// 窗口尺寸监听
function onResize() { isMobile.value = window.innerWidth <= 768 }
onMounted(() => window.addEventListener('resize', onResize))
onUnmounted(() => window.removeEventListener('resize', onResize))

// WebSocket 消息回调
function onWsMessage(msg: GroupMessage) {
  chatWindow.value?.addExternalMessage(msg)
}

async function loadGroup() {
  loading.value = true
  try {
    const { data } = await groupApi.getGroupDetail(groupId.value)
    group.value = data?.data || null
  } catch {
    group.value = null
  } finally {
    loading.value = false
  }
}

async function loadMembers() {
  if (!group.value) return
  membersLoading.value = true
  try {
    const { data } = await groupApi.getMembers(group.value.id)
    members.value = data?.data || []
  } catch {
    members.value = []
  } finally {
    membersLoading.value = false
  }
}

function connectWebSocket() {
  if (wsCleanup) wsCleanup()
  if (!group.value?.id) return

  const { disconnect } = useGroupWebSocket(
    group.value.id,
    onWsMessage,
    (content) => {
      // 系统消息
      chatWindow.value?.addExternalMessage({
        id: Date.now(),
        groupId: group.value!.id,
        senderId: 0,
        senderName: '',
        senderAvatar: '',
        content,
        messageType: 'system',
        mentionedUserIds: [],
        createdAt: new Date().toISOString(),
      })
    },
    userStore.token || undefined,
  )
  wsCleanup = disconnect
}

async function handleRequestJoin() {
  if (!group.value || requestSent.value) return
  joinError.value = ''
  try {
    await groupApi.requestJoin(group.value.id)
    requestSent.value = true
    showToast('申请已发送', 'success', { position: 'top-center' })
  } catch (e: any) {
    const msg = e?.response?.data?.message || e?.message || '申请失败'
    joinError.value = msg
    showToast(msg, 'error', { position: 'top-center' })
  }
}

function onMessageSent(_msg: GroupMessage) {
  // 消息已发送，由组件内部添加到列表
}

async function onMemberChanged() {
  // 成员变动（入群申请通过后），刷新成员列表和群组信息
  await Promise.all([loadMembers(), loadGroup()])
}

function goBack() {
  // 优先使用导航时携带的 from 状态，确保返回到来源页面
  const from = (window.history.state as any)?.from
  if (from && typeof from === 'string') {
    router.replace(from)
  } else {
    router.push('/groups')
  }
}

async function handleLeave() {
  if (!group.value) return
  try {
    await groupApi.leaveGroup(group.value.id)
    showToast('已退出群组', 'success', { position: 'top-center' })
    router.push('/groups')
  } catch {
    showToast('退出失败', 'error', { position: 'top-center' })
  }
}

async function handleDismiss() {
  if (!group.value) return
  try {
    await groupApi.dismissGroup(group.value.id)
    showToast('群组已解散', 'success', { position: 'top-center' })
    router.push('/groups')
  } catch {
    showToast('解散失败', 'error', { position: 'top-center' })
  }
}

async function onGroupUpdated() {
  await Promise.all([loadGroup(), loadMembers()])
}

onMounted(async () => {
  await loadGroup()
  // 仅群成员才加载消息列表、成员列表和 WebSocket
  if (group.value && isMember.value) {
    await loadMembers()
    connectWebSocket()
  }
})

// groupId变化时重新加载
watch(() => groupId.value, async (newId, oldId) => {
  if (newId === oldId || !newId || isNaN(newId)) return
  group.value = null
  members.value = []
  showMembers.value = false
  requestSent.value = false
  joinError.value = ''
  if (wsCleanup) { wsCleanup(); wsCleanup = null }
  loading.value = true
  await loadGroup()
  if (group.value && isMember.value) {
    await loadMembers()
    connectWebSocket()
  }
})

onUnmounted(() => {
  if (wsCleanup) wsCleanup()
})

useHead({
  title: () => group.value ? `${group.value.name} - 知讯` : '群组 - 知讯',
})
</script>

<style scoped>
.group-page {
  height: calc(100dvh - 50px);
  overflow: hidden;
}

@media (min-width: 769px) {
  .group-page {
    height: calc(100dvh - 54px);
  }
}

.group-page-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
}
.loading-spinner {
  width: 32px;
  height: 32px;
  border: 2.5px solid var(--zh-border, #e5e7eb);
  border-top-color: var(--zh-primary, #6366f1);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

.group-page-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
}

.group-page-layout {
  display: flex;
  height: 100%;
}

.group-page-chat {
  flex: 1;
  min-width: 0;
  overflow: hidden;
}

.group-page-members {
  flex-shrink: 0;
}

/* 桌面端：成员面板固定宽度 */
@media (min-width: 769px) {
  .group-page-members {
    width: 260px;
  }
}

/* 移动端：成员面板通过抽屉显示 */
@media (max-width: 768px) {
  .group-page-layout {
    flex-direction: column;
  }
  .group-page-members {
    position: fixed;
    top: 0;
    right: 0;
    bottom: 0;
    z-index: 50;
    width: auto;
  }
}

/* 非成员视图：群组信息卡片 + 申请加入按钮 */
.group-page-non-member {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  padding: 1.5rem;
}

.group-info-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  max-width: 360px;
  width: 100%;
  padding: 2rem;
  border-radius: 16px;
  background: var(--zh-bg-secondary, #fff);
  border: 1px solid var(--zh-border, #e5e7eb);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.group-info-avatar {
  width: 72px;
  height: 72px;
  border-radius: 16px;
  object-fit: cover;
  margin-bottom: 1rem;
}

.group-info-avatar-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--zh-primary, #6366f1);
  color: #fff;
  font-size: 1.75rem;
  font-weight: 600;
}

.group-info-name {
  font-size: 1.25rem;
  font-weight: 600;
  color: var(--zh-text-primary, #111);
  margin: 0 0 0.5rem;
}

.group-info-desc {
  font-size: 0.875rem;
  color: var(--zh-text-secondary, #6b7280);
  line-height: 1.5;
  margin: 0 0 1rem;
}

.group-info-meta {
  display: flex;
  gap: 1rem;
  font-size: 0.8rem;
  color: var(--zh-text-tertiary, #9ca3af);
  margin-bottom: 1.5rem;
}

.group-join-btn {
  width: 100%;
  padding: 0.65rem 1.5rem;
  border: none;
  border-radius: 10px;
  background: var(--zh-primary, #6366f1);
  color: #fff;
  font-size: 0.95rem;
  font-weight: 500;
  cursor: pointer;
  transition: opacity 0.2s;
  min-height: 0;
}

.group-join-btn:hover:not(:disabled) {
  opacity: 0.9;
}

.group-join-btn:disabled {
  background: var(--zh-text-tertiary, #9ca3af);
  cursor: default;
}

.group-join-error {
  margin-top: 0.75rem;
  font-size: 0.8rem;
  color: #ef4444;
}
</style>
