<template>
  <!-- 编辑资料页 -->
  <div class="max-w-2xl mx-auto px-2 2xl:px-3 py-2">
    <!-- 加载状态 -->
    <div v-if="pageLoading" class="flex items-center justify-center py-20">
      <div class="w-8 h-8 border-2 border-primary border-t-transparent rounded-full animate-spin"></div>
    </div>

    <template v-else>
    <div>
      <button class="flex items-center gap-1 text-sm text-slate-500 hover:text-primary-600 transition-colors" @click="goBack">
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
        </svg>
        {{ '返回' }}
      </button>
      <h1 class="text-2xl font-bold text-slate-900">{{ '编辑资料' }}</h1>
      <p class="text-sm text-slate-500 mt-1">修改你的个人公开信息</p>
    </div>

    <!-- 头像 -->
    <section class="card p-3 mb-3">
      <h2 class="text-lg font-semibold text-slate-900 mb-2">{{ '头像' }}</h2>
      <div class="flex items-center gap-4">
        <div class="relative group cursor-pointer shrink-0" @click="triggerAvatarUpload">
          <UserAvatar :src="avatarPreview || userStore.userInfo?.avatar" alt="头像" size="xl" />
          <div class="absolute inset-0 bg-black/40 rounded-full opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center">
            <svg class="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 9a2 2 0 012-2h.93a2 2 0 001.664-.89l.812-1.22A2 2 0 0110.07 4h3.86a2 2 0 011.664.89l.812 1.22A2 2 0 0018.07 7H19a2 2 0 012 2v9a2 2 0 01-2 2H5a2 2 0 01-2-2V9z" />
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 13a3 3 0 11-6 0 3 3 0 016 0z" />
            </svg>
          </div>
        </div>
        <input ref="avatarInput" type="file" accept="image/*" class="hidden" @change="handleAvatarChange" />
        <div>
          <button class="btn-primary text-sm px-4 py-2" :disabled="avatarUploading" @click="triggerAvatarUpload">
            {{ avatarUploading ? '上传中...' : '上传新头像' }}
          </button>
          <p class="text-xs text-slate-400 mt-1">支持 JPG、PNG、WebP 格式，大小不超过 5MB</p>
        </div>
      </div>
    </section>

    <!-- 基本信息 -->
    <section class="card p-3 mb-3">
      <h2 class="text-lg font-semibold text-slate-900 mb-2">{{ '基本信息' }}</h2>
      <div class="space-y-3">
        <!-- 昵称 -->
        <div>
          <label class="block text-sm font-medium text-slate-700 mb-1.5">{{ '昵称' }}</label>
          <div class="flex items-center gap-2">
            <input
              v-model="nicknameInput"
              type="text"
              maxlength="30"
              placeholder="输入你的昵称"
              class="flex-1 px-3 py-2 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-primary/50 focus:border-primary"
            />
            <button
              class="btn-primary text-sm px-4 py-2 shrink-0"
              :disabled="nicknameSaving || !nicknameInput || nicknameInput === userStore.userInfo?.nickname"
              @click="saveNickname"
            >
              {{ nicknameSaving ? '保存中...' : '修改' }}
            </button>
          </div>
          <p v-if="nicknameError" class="text-xs text-red-500 mt-1">{{ nicknameError }}</p>
          <p v-if="nicknameSuccess" class="text-xs text-green-500 mt-1">{{ nicknameSuccess }}</p>
        </div>

        <!-- 简介 -->
        <div>
          <label class="block text-sm font-medium text-slate-700 mb-1.5">{{ '个人简介' }}</label>
          <textarea
            v-model="bioInput"
            maxlength="200"
            rows="3"
            placeholder="写一段简单的自我介绍..."
            class="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-primary/50 focus:border-primary resize-none"
          ></textarea>
          <div class="flex items-center justify-between mt-1">
            <span class="text-xs text-slate-400">{{ bioInput.length }}/200</span>
            <button
              class="btn-primary text-sm px-4 py-2"
              :disabled="bioSaving || bioInput === userStore.userInfo?.bio"
              @click="saveBio"
            >
              {{ bioSaving ? '保存中...' : '修改' }}
            </button>
          </div>
          <p v-if="bioError" class="text-xs text-red-500 mt-1">{{ bioError }}</p>
          <p v-if="bioSuccess" class="text-xs text-green-500 mt-1">{{ bioSuccess }}</p>
        </div>

        <!-- UID -->
        <div>
          <label class="block text-sm font-medium text-slate-700 mb-1.5">{{ '用户ID（UID）' }}</label>
          <div class="flex items-center gap-2">
            <input
              v-model="uidInput"
              type="text"
              maxlength="20"
              placeholder="输入新的UID"
              class="flex-1 px-3 py-2 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-primary/50 focus:border-primary"
              :disabled="uidSaving"
            />
            <button
              class="btn-primary text-sm px-4 py-2 shrink-0"
              :disabled="uidSaving || !uidInput || uidInput === userStore.userInfo?.uid"
              @click="saveUid"
            >
              {{ uidSaving ? '保存中...' : '修改' }}
            </button>
          </div>
          <p class="text-xs text-slate-400 mt-1">
            UID 支持大小写字母、数字和下划线，长度 4-20 个字符，每 30 天可修改一次
            <span v-if="userStore.userInfo?.uidUpdatedAt" class="text-orange-500">
              （上次修改：{{ formatDate(userStore.userInfo.uidUpdatedAt) }}）
            </span>
          </p>
          <p v-if="uidError" class="text-xs text-red-500 mt-1">{{ uidError }}</p>
          <p v-if="uidSuccess" class="text-xs text-green-500 mt-1">{{ uidSuccess }}</p>
        </div>

        <!-- 性别 -->
        <div>
          <label class="block text-sm font-medium text-slate-700 mb-1.5">{{ '性别' }}</label>
          <div class="flex space-x-1.5">
            <button
              v-for="g in genderOptions"
              :key="g.value"
              class="px-4 py-2 text-sm rounded-lg border transition-colors"
              :class="genderInput === g.value
                ? 'bg-primary text-white border-primary'
                : 'bg-white text-slate-700 border-slate-300 hover:border-primary'"
              @click="saveGender(g.value)"
            >
              {{ g.label }}
            </button>
          </div>
        </div>

        <!-- 所属省份 -->
        <div>
          <label class="block text-sm font-medium text-slate-700 mb-1.5">{{ '所属省份' }}</label>
          <div class="flex items-center gap-2">
            <input
              v-model="provinceInput"
              type="text"
              maxlength="50"
              placeholder="输入省份，如：广东、北京"
              class="flex-1 px-3 py-2 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-primary/50 focus:border-primary"
            />
            <button
              class="btn-primary text-sm px-4 py-2 shrink-0"
              :disabled="provinceSaving || provinceInput === userStore.userInfo?.province"
              @click="saveProvince"
            >
              {{ provinceSaving ? '保存中...' : '保存' }}
            </button>
          </div>
          <p class="text-xs text-slate-400 mt-1">设置你的所属省份，将显示在个人主页上</p>
        </div>
      </div>
    </section>

    <!-- 账号安全（只读信息） -->
    <section class="card p-3 mb-3">
      <h2 class="text-lg font-semibold text-slate-900 mb-2">{{ '账号信息（只读）' }}</h2>
      <div class="space-y-3">
        <div>
          <label class="block text-sm font-medium text-slate-500 mb-1">{{ '用户名' }}</label>
          <p class="text-sm text-slate-700 bg-slate-50 px-3 py-2 rounded-lg">{{ userStore.userInfo?.username || '-' }}</p>
        </div>
        <div>
          <label class="block text-sm font-medium text-slate-500 mb-1">{{ '邮箱' }}</label>
          <p class="text-sm text-slate-700 bg-slate-50 px-3 py-2 rounded-lg">{{ userStore.userInfo?.email || '-' }}</p>
        </div>
        <div>
          <label class="block text-sm font-medium text-slate-500 mb-1">{{ '手机号' }}</label>
          <p class="text-sm text-slate-700 bg-slate-50 px-3 py-2 rounded-lg">{{ userStore.userInfo?.phone || '未绑定' }}</p>
        </div>
        <div>
          <label class="block text-sm font-medium text-slate-500 mb-1">{{ '注册时间' }}</label>
          <p class="text-sm text-slate-700 bg-slate-50 px-3 py-2 rounded-lg">{{ formatDate(userStore.userInfo?.createdAt) }}</p>
        </div>
      </div>
    </section>
    </template>
  </div>
</template>

<script setup lang="ts">
/** 编辑资料页 - 修改个人公开信息 */
definePageMeta({
  middleware: 'auth',
})

const router = useRouter()
const userStore = useUserStore()
const pageLoading = ref(true)

// 头像上传
const avatarInput = ref<HTMLInputElement | null>(null)
const avatarUploading = ref(false)
const avatarPreview = ref('')

const triggerAvatarUpload = () => {
  avatarInput.value?.click()
}

const handleAvatarChange = async (e: Event) => {
  const file = (e.target as HTMLInputElement).files?.[0]
  if (!file) return

  if (!file.type.startsWith('image/')) {
    showToast('请选择图片文件', 'error')
    return
  }
  if (file.size > 5 * 1024 * 1024) {
    showToast('图片大小不能超过 5MB', 'error')
    return
  }

  avatarUploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', file)
    const { post: apiPost } = useApi()
    const uploadRes = await apiPost<any>('/files/upload/image', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    const avatarUrl = uploadRes.data?.data
    if (avatarUrl) {
      avatarPreview.value = avatarUrl
      const { put: apiPut } = useApi()
      await apiPut<any>('/user/profile', { avatar: avatarUrl })
      userStore.updateProfile({ avatar: avatarUrl })
      showToast('头像更新成功')
    }
  } catch (err: any) {
    showToast(err?.response?.data?.message || '头像上传失败，请稍后重试', 'error')
  } finally {
    avatarUploading.value = false
    if (avatarInput.value) {
      avatarInput.value.value = ''
    }
  }
}

// 昵称
const nicknameInput = ref('')
const nicknameSaving = ref(false)
const nicknameError = ref('')
const nicknameSuccess = ref('')

const saveNickname = async () => {
  const val = nicknameInput.value.trim()
  if (!val) {
    nicknameError.value = '昵称不能为空'
    return
  }
  if (val.length < 2) {
    nicknameError.value = '昵称至少需要2个字符'
    return
  }
  nicknameError.value = ''
  nicknameSuccess.value = ''
  nicknameSaving.value = true
  try {
    const { put: apiPut } = useApi()
    await apiPut('/user/profile', { nickname: val })
    userStore.updateProfile({ nickname: val })
    nicknameSuccess.value = '昵称修改成功'
    showToast('昵称修改成功')
  } catch (err: any) {
    nicknameError.value = err?.response?.data?.message || '昵称修改失败，请稍后重试'
  } finally {
    nicknameSaving.value = false
  }
}

// 简介
const bioInput = ref('')
const bioSaving = ref(false)
const bioError = ref('')
const bioSuccess = ref('')

const saveBio = async () => {
  bioError.value = ''
  bioSuccess.value = ''
  bioSaving.value = true
  try {
    const { put: apiPut } = useApi()
    await apiPut('/user/profile', { bio: bioInput.value.trim() })
    userStore.updateProfile({ bio: bioInput.value.trim() })
    bioSuccess.value = '简介修改成功'
    showToast('简介修改成功')
  } catch (err: any) {
    bioError.value = err?.response?.data?.message || '简介修改失败，请稍后重试'
  } finally {
    bioSaving.value = false
  }
}

// UID
const uidInput = ref('')
const uidSaving = ref(false)
const uidError = ref('')
const uidSuccess = ref('')

const saveUid = async () => {
  const newUid = uidInput.value.trim()
  if (!newUid) {
    uidError.value = 'UID不能为空'
    return
  }
  if (!/^[a-zA-Z0-9_]{4,20}$/.test(newUid)) {
    uidError.value = 'UID仅允许大小写字母、数字和下划线，长度4-20个字符'
    return
  }
  uidError.value = ''
  uidSuccess.value = ''
  uidSaving.value = true
  try {
    const { userApi } = await import('~/api')
    await userApi.updateUid(newUid)
    userStore.updateUid(newUid)
    uidSuccess.value = 'UID修改成功'
    showToast('UID修改成功')
  } catch (err: any) {
    uidError.value = err?.response?.data?.message || 'UID修改失败，请稍后重试'
  } finally {
    uidSaving.value = false
  }
}

// 性别
const genderInput = ref<number>(0)

const genderOptions = [
  { label: '未知', value: 0 },
  { label: '男', value: 1 },
  { label: '女', value: 2 },
]

const saveGender = async (gender: number) => {
  if (gender === genderInput.value && gender === (userStore.userInfo?.gender ?? 0)) return
  genderInput.value = gender
  try {
    const { put: apiPut } = useApi()
    await apiPut('/user/profile', { gender })
    userStore.updateProfile({ gender: gender as any })
    showToast('性别修改成功')
  } catch {
    showToast('性别修改失败', 'error')
    genderInput.value = userStore.userInfo?.gender ?? 0
  }
}

// 省份
const provinceInput = ref('')
const provinceSaving = ref(false)

const saveProvince = async () => {
  provinceSaving.value = true
  try {
    const { put: apiPut } = useApi()
    await apiPut('/user/profile', { province: provinceInput.value.trim() })
    userStore.updateProfile({ province: provinceInput.value.trim() })
    showToast('所属省份保存成功')
  } catch {
    showToast('保存失败，请稍后重试', 'error')
  } finally {
    provinceSaving.value = false
  }
}

// 初始化表单数据
const initForm = () => {
  nicknameInput.value = userStore.userInfo?.nickname || ''
  bioInput.value = userStore.userInfo?.bio || ''
  uidInput.value = userStore.userInfo?.uid || ''
  provinceInput.value = userStore.userInfo?.province || ''
  genderInput.value = userStore.userInfo?.gender ?? 0
}

// 格式化日期
const formatDate = (date?: string) => {
  if (!date) return '-'
  return new Date(date).toLocaleDateString('zh-CN')
}

// 返回上一页
const goBack = () => {
  if (window.history.length > 1) {
    router.back()
  } else {
    navigateTo('/user')
  }
}

// Toast提示
const showToast = (message: string, type: 'success' | 'error' = 'success') => {
  if (!import.meta.client) return
  const toast = document.createElement('div')
  toast.className = `fixed top-4 right-4 z-50 px-4 py-3 rounded-lg shadow-lg text-white text-sm transition-all duration-300 transform translate-x-0 opacity-100 ${
    type === 'success' ? 'bg-green-500' : 'bg-red-500'
  }`
  toast.textContent = message
  document.body.appendChild(toast)
  setTimeout(() => {
    toast.style.opacity = '0'
    toast.style.transform = 'translateX(100%)'
    setTimeout(() => toast.remove(), 300)
  }, 2000)
}

onMounted(() => {
  initForm()
  pageLoading.value = false
})

useHead({
  title: () => '编辑资料' + ' - 知讯',
})
</script>
