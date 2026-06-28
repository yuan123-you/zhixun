<template>
  <!-- 编辑资料页 -->
  <div class="max-w-2xl mx-auto px-4 py-4">

    <!-- 加载状态 -->
    <div v-if="pageLoading" class="flex items-center justify-center py-20">
      <div class="w-8 h-8 border-2 border-primary border-t-transparent rounded-full animate-spin"></div>
    </div>

    <template v-else>
    <!-- 头像 -->
    <section class="card p-2.5 mb-2">
      <h2 class="text-lg font-semibold text-[var(--zh-text)] mb-2">{{ '头像' }}</h2>
      <!-- 头像上传错误提示 -->
      <div v-if="avatarError" class="field-error-banner mb-2">
        <svg class="w-4 h-4 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
        <span>{{ avatarError }}</span>
      </div>
      <div class="flex items-center gap-4">
        <div class="relative group cursor-pointer shrink-0" :class="{ 'avatar-error-shake': avatarShake }" @click="triggerAvatarUpload">
          <UserAvatar :src="avatarPreview || userStore.userInfo?.avatar" alt="头像" size="xl" />
          <div class="absolute inset-0 bg-black/40 rounded-full opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center">
            <svg class="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 9a2 2 0 012-2h.93a2 2 0 001.664-.89l.812-1.22A2 2 0 0110.07 4h3.86a2 2 0 011.664.89l.812 1.22A2 2 0 0018.07 7H19a2 2 0 012 2v9a2 2 0 01-2 2H5a2 2 0 01-2-2V9z" />
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 13a3 3 0 11-6 0 3 3 0 016 0z" />
            </svg>
          </div>
        </div>
        <input ref="avatarInput" type="file" accept="image/jpeg,image/png,image/gif,image/webp,image/bmp" class="hidden" @change="handleAvatarChange" />
        <div class="flex flex-col gap-1.5">
          <div class="flex items-center gap-2">
            <button class="btn-primary text-sm px-4 py-2" :disabled="avatarUploading" @click="triggerAvatarUpload">
              <span v-if="avatarUploading" class="flex items-center gap-1.5">
                <span class="inline-block w-3.5 h-3.5 border-2 border-white border-t-transparent rounded-full animate-spin"></span>
                上传中 {{ avatarProgress }}%
              </span>
              <span v-else>上传新头像</span>
            </button>
            <button v-if="avatarUploading" class="btn-secondary text-sm px-3 py-2 !text-red-500 hover:!text-red-600 hover:!border-red-300" @click="cancelAvatarUpload">
              取消上传
            </button>
          </div>
          <!-- 上传进度条 -->
          <div v-if="avatarUploading" class="w-full max-w-[200px] bg-[var(--zh-bg-hover)] rounded-full h-1.5 overflow-hidden">
            <div class="h-full bg-primary rounded-full transition-all duration-300 ease-out" :style="{ width: avatarProgress + '%' }"></div>
          </div>
          <p v-if="!avatarUploading" class="text-xs text-[var(--zh-text-tertiary)]">支持 JPG、PNG、GIF、WebP、BMP 格式，大小不超过 5MB</p>
        </div>
      </div>
    </section>

    <!-- 基本信息 -->
    <section class="card p-2.5 mb-2">
      <h2 class="text-lg font-semibold text-[var(--zh-text)] mb-1.5">{{ '基本信息' }}</h2>
      <div class="space-y-2.5">
        <!-- 昵称 -->
        <div>
          <label class="block text-sm font-medium text-[var(--zh-text-secondary)] mb-1.5">{{ '昵称' }}</label>
          <div class="flex items-center gap-2">
            <input
              v-model="nicknameInput"
              type="text"
              maxlength="30"
              placeholder="输入你的昵称"
              class="flex-1 px-3 py-2 border rounded-lg text-sm outline-none transition-all duration-150"
              :class="nicknameError ? 'field-input-error' : 'border-[var(--zh-border)] focus:ring-[3px] focus:ring-input-accent/10 focus:border-input-accent'"
              @input="nicknameError && clearFieldError('nickname')"
            />
            <button
              class="btn-primary text-sm px-4 py-2 shrink-0"
              :disabled="nicknameSaving || !nicknameInput || nicknameInput === userStore.userInfo?.nickname"
              @click="saveNickname"
            >
              {{ nicknameSaving ? '保存中...' : '修改' }}
            </button>
          </div>
          <!-- 错误信息 -->
          <div v-if="nicknameError" class="field-error mt-1">
            <svg class="w-3.5 h-3.5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            <span>{{ nicknameError }}</span>
          </div>
          <!-- 成功信息 -->
          <div v-if="nicknameSuccess" class="field-success mt-1">
            <svg class="w-3.5 h-3.5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
            </svg>
            <span>{{ nicknameSuccess }}</span>
          </div>
        </div>

        <!-- 简介 -->
        <div>
          <label class="block text-sm font-medium text-[var(--zh-text-secondary)] mb-1.5">{{ '个人简介' }}</label>
          <textarea
            v-model="bioInput"
            maxlength="200"
            rows="3"
            placeholder="写一段简单的自我介绍..."
            class="w-full px-3 py-2 border rounded-lg text-sm outline-none transition-all duration-150 resize-none"
            :class="bioError ? 'field-input-error' : 'border-[var(--zh-border)] focus:ring-[3px] focus:ring-input-accent/10 focus:border-input-accent'"
            @input="bioError && clearFieldError('bio')"
          ></textarea>
          <div class="flex items-center justify-between mt-1">
            <span class="text-xs text-[var(--zh-text-tertiary)]">{{ bioInput.length }}/200</span>
            <button
              class="btn-primary text-sm px-4 py-2"
              :disabled="bioSaving || bioInput === userStore.userInfo?.bio"
              @click="saveBio"
            >
              {{ bioSaving ? '保存中...' : '修改' }}
            </button>
          </div>
          <div v-if="bioError" class="field-error mt-1">
            <svg class="w-3.5 h-3.5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            <span>{{ bioError }}</span>
          </div>
          <div v-if="bioSuccess" class="field-success mt-1">
            <svg class="w-3.5 h-3.5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
            </svg>
            <span>{{ bioSuccess }}</span>
          </div>
        </div>

        <!-- UID -->
        <div>
          <label class="block text-sm font-medium text-[var(--zh-text-secondary)] mb-1.5">{{ '用户ID（UID）' }}</label>
          <div class="flex items-center gap-2">
            <input
              v-model="uidInput"
              type="text"
              maxlength="20"
              placeholder="输入新的UID"
              class="flex-1 px-3 py-2 border rounded-lg text-sm outline-none transition-all duration-150"
              :class="uidError ? 'field-input-error' : 'border-[var(--zh-border)] focus:ring-[3px] focus:ring-input-accent/10 focus:border-input-accent'"
              :disabled="uidSaving"
              @input="uidError && clearFieldError('uid')"
            />
            <button
              class="btn-primary text-sm px-4 py-2 shrink-0"
              :disabled="uidSaving || !uidInput || uidInput === userStore.userInfo?.uid"
              @click="saveUid"
            >
              {{ uidSaving ? '保存中...' : '修改' }}
            </button>
          </div>
          <p class="text-xs text-[var(--zh-text-tertiary)] mt-1">
            UID 支持大小写字母、数字和下划线，长度 4-20 个字符，每 30 天可修改一次
            <span v-if="userStore.userInfo?.uidUpdatedAt" class="text-orange-500">
              （上次修改：{{ formatDate(userStore.userInfo.uidUpdatedAt) }}）
            </span>
          </p>
          <div v-if="uidError" class="field-error mt-1">
            <svg class="w-3.5 h-3.5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            <span>{{ uidError }}</span>
          </div>
          <div v-if="uidSuccess" class="field-success mt-1">
            <svg class="w-3.5 h-3.5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
            </svg>
            <span>{{ uidSuccess }}</span>
          </div>
        </div>

        <!-- 性别 -->
        <div>
          <label class="block text-sm font-medium text-[var(--zh-text-secondary)] mb-1.5">{{ '性别' }}</label>
          <div class="flex space-x-1.5">
            <button
              v-for="g in genderOptions"
              :key="g.value"
              class="px-4 py-2 text-sm rounded-lg border transition-colors"
              :class="genderInput === g.value
                ? 'bg-primary text-white border-primary'
                : 'bg-[var(--zh-bg-elevated)] text-[var(--zh-text-secondary)] border-[var(--zh-border)] hover:border-primary'"
              :disabled="genderSaving"
              @click="saveGender(g.value)"
            >
              {{ g.label }}
            </button>
          </div>
          <div v-if="genderError" class="field-error mt-1.5">
            <svg class="w-3.5 h-3.5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            <span>{{ genderError }}</span>
          </div>
          <!-- 性别展示开关 -->
          <div class="flex items-center gap-2 mt-3">
            <label class="relative inline-flex items-center cursor-pointer">
              <input type="checkbox" v-model="showGenderOnProfile" class="sr-only peer" :disabled="genderDisplaySaving" @change="saveGenderDisplay" />
              <div class="w-9 h-5 bg-slate-300 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-[var(--zh-bg-elevated)] after:rounded-full after:h-4 after:w-4 after:transition-all peer-checked:bg-primary peer-disabled:opacity-50"></div>
            </label>
            <span class="text-sm text-[var(--zh-text-secondary)]">{{ '在个人主页展示性别' }}</span>
          </div>
          <div v-if="genderDisplayError" class="field-error mt-1.5">
            <svg class="w-3.5 h-3.5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            <span>{{ genderDisplayError }}</span>
          </div>
        </div>

        <!-- 所在地区 -->
        <div>
          <label class="block text-sm font-medium text-[var(--zh-text-secondary)] mb-1.5">{{ '所在地区' }}</label>
          <RegionSelector
            ref="regionSelectorRef"
            :show-auto-locate="true"
            :locating="regionLocating"
            :locating-text="regionLocatingText"
            :locate-error="regionLocateError"
            :disabled="regionSaving"
            @change="handleRegionChange"
            @clear="handleRegionClear"
            @auto-locate="autoLocateRegion"
          />
          <div v-if="regionError" class="field-error mt-1">
            <svg class="w-3.5 h-3.5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            <span>{{ regionError }}</span>
          </div>
          <p class="text-xs text-[var(--zh-text-tertiary)] mt-1">选择你所在的省市，将显示在个人主页上</p>
        </div>
      </div>
    </section>

    <!-- 账号安全（只读信息） -->
    <section class="card p-2.5 mb-2">
      <h2 class="text-lg font-semibold text-[var(--zh-text)] mb-1.5">{{ '账号信息（只读）' }}</h2>
      <div class="space-y-2.5">
        <div>
          <label class="block text-sm font-medium text-[var(--zh-text-secondary)] mb-1">{{ '用户名' }}</label>
          <p class="text-sm text-[var(--zh-text-secondary)] bg-[var(--zh-bg-hover)] px-3 py-2 rounded-lg">{{ userStore.userInfo?.username || '-' }}</p>
        </div>
        <div>
          <label class="block text-sm font-medium text-[var(--zh-text-secondary)] mb-1">{{ '邮箱' }}</label>
          <p class="text-sm text-[var(--zh-text-secondary)] bg-[var(--zh-bg-hover)] px-3 py-2 rounded-lg">{{ userStore.userInfo?.email || '-' }}</p>
        </div>
        <div>
          <label class="block text-sm font-medium text-[var(--zh-text-secondary)] mb-1">{{ '手机号' }}</label>
          <p class="text-sm text-[var(--zh-text-secondary)] bg-[var(--zh-bg-hover)] px-3 py-2 rounded-lg">{{ userStore.userInfo?.phone || '未绑定' }}</p>
        </div>
        <div>
          <label class="block text-sm font-medium text-[var(--zh-text-secondary)] mb-1">{{ '注册时间' }}</label>
          <p class="text-sm text-[var(--zh-text-secondary)] bg-[var(--zh-bg-hover)] px-3 py-2 rounded-lg">{{ formatDate(userStore.userInfo?.createdAt) }}</p>
        </div>
      </div>
    </section>
    </template>
  </div>
</template>

<script setup lang="ts">
/** 编辑资料页 - 修改个人公开信息 */
import RegionSelector from '@/components/RegionSelector.vue'
import { showToast } from '@/composables/useToast'
import { useLocation } from '@/composables/useLocation'
import { formatDate } from '@/utils/format'

const { setTitle } = usePageHeaderTitle()
setTitle('编辑资料')

const userStore = useUserStore()
const pageLoading = ref(true)

// ======================== 统一错误状态管理 ========================
type FieldName = 'nickname' | 'bio' | 'uid' | 'gender' | 'genderDisplay' | 'avatar' | 'region'
const fieldErrors = reactive<Record<FieldName, string>>({
  nickname: '',
  bio: '',
  uid: '',
  gender: '',
  genderDisplay: '',
  avatar: '',
  region: '',
})
const fieldSuccess = reactive<Record<FieldName, string>>({
  nickname: '',
  bio: '',
  uid: '',
  gender: '',
  genderDisplay: '',
  avatar: '',
  region: '',
})

/** 清除指定字段的错误 */
const clearFieldError = (field: FieldName) => {
  fieldErrors[field] = ''
}

/** 清除指定字段的成功提示 */
const clearFieldSuccess = (field: FieldName) => {
  fieldSuccess[field] = ''
}

/** 设置字段错误，带自动清除定时器 */
const setFieldError = (field: FieldName, message: string, autoClearMs = 0) => {
  fieldErrors[field] = message
  fieldSuccess[field] = ''
  if (autoClearMs > 0) {
    setTimeout(() => { fieldErrors[field] = '' }, autoClearMs)
  }
}

/** 设置字段成功提示，带自动清除定时器 */
const setFieldSuccess = (field: FieldName, message: string, autoClearMs = 3000) => {
  fieldSuccess[field] = message
  fieldErrors[field] = ''
  if (autoClearMs > 0) {
    setTimeout(() => { fieldSuccess[field] = '' }, autoClearMs)
  }
}

// 便捷访问 computed
const nicknameError = computed(() => fieldErrors.nickname)
const nicknameSuccess = computed(() => fieldSuccess.nickname)
const bioError = computed(() => fieldErrors.bio)
const bioSuccess = computed(() => fieldSuccess.bio)
const uidError = computed(() => fieldErrors.uid)
const uidSuccess = computed(() => fieldSuccess.uid)
const genderError = computed(() => fieldErrors.gender)
const genderDisplayError = computed(() => fieldErrors.genderDisplay)
const avatarError = computed(() => fieldErrors.avatar)
const regionError = computed(() => fieldErrors.region)

// ======================== 头像上传 ========================
const avatarInput = ref<HTMLInputElement | null>(null)
const avatarUploading = ref(false)
const avatarPreview = ref('')
const avatarShake = ref(false)
const avatarProgress = ref(0)
let avatarAbortController: AbortController | null = null

const triggerAvatarUpload = () => {
  clearFieldError('avatar')
  avatarInput.value?.click()
}

const triggerAvatarShake = () => {
  avatarShake.value = true
  setTimeout(() => { avatarShake.value = false }, 500)
}

// 允许的图片 MIME 类型
const ALLOWED_IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/gif', 'image/webp', 'image/bmp']

const cancelAvatarUpload = () => {
  if (avatarAbortController) {
    avatarAbortController.abort()
    avatarAbortController = null
  }
  avatarUploading.value = false
  avatarProgress.value = 0
  if (avatarInput.value) {
    avatarInput.value.value = ''
  }
  showToast('已取消上传', 'info')
}

const handleAvatarChange = async (e: Event) => {
  const file = (e.target as HTMLInputElement).files?.[0]
  if (!file) return

  clearFieldError('avatar')

  // 文件类型校验
  if (!ALLOWED_IMAGE_TYPES.includes(file.type)) {
    const ext = file.name.split('.').pop()?.toLowerCase() || '未知'
    setFieldError('avatar', `不支持 ${ext} 格式，请选择 JPG、PNG、GIF、WebP、BMP 格式的图片`)
    triggerAvatarShake()
    // 重置 input，确保同文件再次选择时也能触发 change
    if (avatarInput.value) avatarInput.value.value = ''
    return
  }

  // 文件大小校验
  if (file.size > 5 * 1024 * 1024) {
    const sizeMB = (file.size / (1024 * 1024)).toFixed(1)
    setFieldError('avatar', `图片大小 ${sizeMB}MB 超过 5MB 限制，请压缩后重新上传`)
    triggerAvatarShake()
    if (avatarInput.value) avatarInput.value.value = ''
    return
  }

  // 文件名安全校验
  if (!file.name || file.name.length > 255) {
    setFieldError('avatar', '文件名不合法，请重命名后重新上传')
    triggerAvatarShake()
    if (avatarInput.value) avatarInput.value.value = ''
    return
  }

  avatarUploading.value = true
  avatarProgress.value = 0
  avatarAbortController = new AbortController()

  let avatarUrl = ''
  try {
    try {
      const formData = new FormData()
      formData.append('file', file)
      const { upload } = useApi()

      const uploadRes = await upload<any>('/files/upload/image', formData, (percent) => {
        avatarProgress.value = percent
      })

      avatarUrl = uploadRes.data?.data
      if (!avatarUrl) {
        throw new Error('服务器未返回头像地址')
      }
    } catch (err: any) {
      // 用户主动取消，不显示错误
      if (err?.code === 'ERR_CANCELED' || err?.name === 'CanceledError' || err?.message === 'canceled') {
        return
      }
      // 网络错误
      if (!err?.response) {
        setFieldError('avatar', '网络连接失败，请检查网络后重试')
      } else {
        const msg = err?.response?.data?.message || '头像上传失败'
        setFieldError('avatar', `${msg}，请重试`)
      }
      triggerAvatarShake()
      return
    }

    // 上传成功，先预加载新头像再更新 UI，避免闪烁
    try {
      await new Promise<void>((resolve, reject) => {
        const img = new Image()
        img.onload = () => resolve()
        img.onerror = () => reject(new Error('头像加载失败'))
        img.src = avatarUrl
      })
    } catch {
      // 预加载失败不阻塞流程
    }

    // 本地预览
    avatarPreview.value = avatarUrl

    // 更新用户资料
    try {
      const { put: apiPut } = useApi()
      await apiPut<any>('/user/profile', { avatar: avatarUrl })
      userStore.updateProfile({ avatar: avatarUrl })
      showToast('头像更新成功')
      clearFieldError('avatar')
    } catch (err: any) {
      // 资料更新失败，回退预览
      avatarPreview.value = ''
      const msg = err?.response?.data?.message || '头像保存失败'
      setFieldError('avatar', `${msg}，请重试`)
      triggerAvatarShake()
    }
  } finally {
    avatarUploading.value = false
    avatarAbortController = null
    if (avatarInput.value) {
      avatarInput.value.value = ''
    }
  }
}

// ======================== 昵称 ========================
const nicknameInput = ref('')
const nicknameSaving = ref(false)

const saveNickname = async () => {
  const val = nicknameInput.value.trim()
  clearFieldError('nickname')
  clearFieldSuccess('nickname')

  if (!val) {
    setFieldError('nickname', '昵称不能为空，请输入 2-30 个字符的昵称')
    return
  }
  if (val.length < 2) {
    setFieldError('nickname', '昵称至少需要 2 个字符，当前仅输入了 1 个字符')
    return
  }

  nicknameSaving.value = true
  try {
    const { put: apiPut } = useApi()
    await apiPut('/user/profile', { nickname: val })
    userStore.updateProfile({ nickname: val })
    setFieldSuccess('nickname', '昵称已修改成功')
  } catch (err: any) {
    const msg = err?.response?.data?.message || '昵称修改失败'
    setFieldError('nickname', `${msg}，请检查后重试`)
  } finally {
    nicknameSaving.value = false
  }
}

// ======================== 简介 ========================
const bioInput = ref('')
const bioSaving = ref(false)

const saveBio = async () => {
  clearFieldError('bio')
  clearFieldSuccess('bio')
  bioSaving.value = true
  try {
    const { put: apiPut } = useApi()
    await apiPut('/user/profile', { bio: bioInput.value.trim() })
    userStore.updateProfile({ bio: bioInput.value.trim() })
    setFieldSuccess('bio', '简介已修改成功')
  } catch (err: any) {
    const msg = err?.response?.data?.message || '简介修改失败'
    setFieldError('bio', `${msg}，请检查后重试`)
  } finally {
    bioSaving.value = false
  }
}

// ======================== UID ========================
const uidInput = ref('')
const uidSaving = ref(false)

const saveUid = async () => {
  const newUid = uidInput.value.trim()
  clearFieldError('uid')
  clearFieldSuccess('uid')

  if (!newUid) {
    setFieldError('uid', 'UID 不能为空，请输入 4-20 个字符的 UID')
    return
  }
  if (!/^[a-zA-Z0-9_]{4,20}$/.test(newUid)) {
    setFieldError('uid', 'UID 格式不正确，仅允许字母、数字和下划线，长度 4-20 个字符')
    return
  }

  uidSaving.value = true
  try {
    const { userApi } = await import('~/api')
    await userApi.updateUid(newUid)
    userStore.updateUid(newUid)
    setFieldSuccess('uid', 'UID 已修改成功')
  } catch (err: any) {
    const msg = err?.response?.data?.message || 'UID 修改失败'
    setFieldError('uid', `${msg}，请检查后重试`)
  } finally {
    uidSaving.value = false
  }
}

// ======================== 性别 ========================
const genderInput = ref<number>(0)
const genderSaving = ref(false)

const genderOptions = [
  { label: '未知', value: 0 },
  { label: '男', value: 1 },
  { label: '女', value: 2 },
]

const saveGender = async (gender: number) => {
  if (genderSaving.value) return
  if (gender === genderInput.value && gender === (userStore.userInfo?.gender ?? 0)) return

  clearFieldError('gender')
  const previousGender = genderInput.value
  genderInput.value = gender
  genderSaving.value = true

  try {
    const { put: apiPut } = useApi()
    await apiPut('/user/profile', { gender })
    userStore.updateProfile({ gender: gender as any })
    showToast('性别修改成功')
  } catch {
    showToast('性别修改失败，请重试', 'error')
    genderInput.value = previousGender
  } finally {
    genderSaving.value = false
  }
}

// ======================== 性别展示开关 ========================
const showGenderOnProfile = ref(false)
const genderDisplaySaving = ref(false)

const saveGenderDisplay = async () => {
  clearFieldError('genderDisplay')
  genderDisplaySaving.value = true
  try {
    const { put: apiPut } = useApi()
    await apiPut('/user/profile', { showGenderOnProfile: showGenderOnProfile.value })
    userStore.updateProfile({ showGenderOnProfile: showGenderOnProfile.value } as any)
    showToast('性别展示设置已保存')
  } catch {
    showToast('设置保存失败，请重试', 'error')
    showGenderOnProfile.value = !showGenderOnProfile.value
  } finally {
    genderDisplaySaving.value = false
  }
}

// ======================== 地区选择 ========================
const regionSelectorRef = ref<InstanceType<typeof RegionSelector> | null>(null)
const regionSaving = ref(false)
const regionLocating = ref(false)
const regionLocateError = ref('')
const { locate: locateAction } = useLocation()

/** 定位阶段提示 */
const regionLocatingText = computed(() => {
  // 直接借用 locationApi 的 stage（避免 ref 覆盖）
  return '获取中...'
})

/** 自动定位并写入所在地区 */
const autoLocateRegion = async () => {
  if (regionLocating.value || regionSaving.value) return
  clearFieldError('region')
  regionLocating.value = true
  regionLocateError.value = ''
  try {
    const result = await locateAction({ useCache: false })
    if (!result?.province) {
      const msg = '自动定位失败，请手动选择地区'
      regionLocateError.value = msg
      showToast(msg, 'error')
      return
    }
    const locationStr = result.district && result.district !== result.city
      ? `${result.province}·${result.city}·${result.district}`
      : `${result.province}${result.city ? '·' + result.city : ''}`
    regionSelectorRef.value?.setRegion({
      province: result.province,
      city: result.city,
      district: result.district,
    })
    // 走 change 处理保存
    await handleRegionChange(locationStr)
  } catch (err: any) {
    const msg = err?.message || '自动定位失败，请手动选择地区'
    regionLocateError.value = msg
    showToast(msg, 'error')
  } finally {
    regionLocating.value = false
  }
}

const parseRegion = (regionStr: string): { province: string; city: string } => {
  if (!regionStr) return { province: '', city: '' }
  const parts = regionStr.split('·')
  return {
    province: parts[0] || '',
    city: parts[1] || '',
  }
}

const handleRegionChange = async (location: string) => {
  if (!location || regionSaving.value) return
  clearFieldError('region')
  regionSaving.value = true
  try {
    const { put: apiPut } = useApi()
    await apiPut('/user/profile', { province: location })
    userStore.updateProfile({ province: location })
    showToast('地区保存成功')
  } catch {
    setFieldError('region', '地区保存失败，请检查网络后重试')
  } finally {
    regionSaving.value = false
  }
}

const handleRegionClear = async () => {
  if (regionSaving.value) return
  clearFieldError('region')
  regionSaving.value = true
  try {
    const { put: apiPut } = useApi()
    await apiPut('/user/profile', { province: '' })
    userStore.updateProfile({ province: '' })
    showToast('地区已清除')
  } catch {
    setFieldError('region', '地区清除失败，请检查网络后重试')
  } finally {
    regionSaving.value = false
  }
}

// ======================== 初始化表单数据 ========================
const initForm = () => {
  nicknameInput.value = userStore.userInfo?.nickname || ''
  bioInput.value = userStore.userInfo?.bio || ''
  uidInput.value = userStore.userInfo?.uid || ''
  genderInput.value = userStore.userInfo?.gender ?? 0
  showGenderOnProfile.value = (userStore.userInfo as any)?.showGenderOnProfile ?? false
  nextTick(() => {
    const savedRegion = userStore.userInfo?.province || ''
    if (savedRegion && regionSelectorRef.value) {
      const { province, city } = parseRegion(savedRegion)
      regionSelectorRef.value.setRegion({
        province,
        city,
        district: '',
      })
    }
  })
}

onMounted(() => {
  initForm()
  pageLoading.value = false
})

useHead({
  title: () => '编辑资料' + ' - 知讯',
})
</script>

<style scoped>
/* ======================== 字段级错误信息样式 ======================== */
.field-error {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  font-size: 12px;
  line-height: 1.5;
  color: #ef4444;
  padding: 6px 10px;
  background: rgb(254 242 242);
  border: 1px solid rgb(254 226 226);
  border-radius: 8px;
  animation: field-error-in 0.25s var(--zh-transition-base) both;
}

.field-error svg {
  margin-top: 1px;
}

.field-success {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  font-size: 12px;
  line-height: 1.5;
  color: #10b981;
  padding: 6px 10px;
  background: rgb(236 253 245);
  border: 1px solid rgb(209 250 229);
  border-radius: 8px;
  animation: field-success-in 0.25s var(--zh-transition-base) both;
}

.field-success svg {
  margin-top: 1px;
}

/* 错误横幅（头像上传等区块级错误） */
.field-error-banner {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  font-size: 13px;
  line-height: 1.5;
  color: #ef4444;
  padding: 10px 14px;
  background: rgb(254 242 242);
  border: 1px solid rgb(254 226 226);
  border-radius: 10px;
  animation: field-error-in 0.3s var(--zh-transition-base) both;
}

.field-error-banner svg {
  margin-top: 1px;
}

/* 输入框错误状态 */
.field-input-error {
  border-color: #ef4444 !important;
  background: rgb(254 242 242) !important;
}

.field-input-error:focus {
  border-color: #ef4444 !important;
  box-shadow: 0 0 0 3px rgba(239, 68, 68, 0.12) !important;
}

/* 错误抖动动画 */
.avatar-error-shake {
  animation: error-shake 0.5s var(--zh-transition-base);
}

/* 动画关键帧 */
@keyframes field-error-in {
  from {
    opacity: 0;
    transform: translateY(-6px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes field-success-in {
  from {
    opacity: 0;
    transform: translateY(-4px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes error-shake {
  0%, 100% { transform: translateX(0); }
  15% { transform: translateX(-6px); }
  30% { transform: translateX(6px); }
  45% { transform: translateX(-4px); }
  60% { transform: translateX(4px); }
  75% { transform: translateX(-2px); }
  90% { transform: translateX(2px); }
}
</style>
