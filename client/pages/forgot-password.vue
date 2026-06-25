<template>
  <!-- 忘记密码页 -->
  <div class="w-full max-w-md mx-auto px-2">
    <div class="card p-4 md:p-6">
      <!-- Logo和标题 -->
      <div class="text-center mb-4">
        <div class="w-12 h-12 bg-primary rounded-2xl flex items-center justify-center mx-auto mb-3">
          <span class="text-white font-bold text-2xl">知</span>
        </div>
        <h1 class="text-2xl font-bold text-slate-900">{{ '重置密码' }}</h1>
        <p class="text-sm text-slate-500 mt-2">{{ '通过邮箱验证重置你的密码' }}</p>
      </div>

      <!-- 重置密码表单 -->
      <form class="space-y-3" @submit.prevent="handleReset">
        <!-- 用户名 -->
        <div>
          <label class="block text-sm font-medium text-slate-700 mb-1">{{ '用户名' }}</label>
          <input
            v-model="form.username"
            type="text"
            class="input"
            placeholder="请输入用户名"
            required
          />
        </div>

        <!-- 邮箱 -->
        <div>
          <label class="block text-sm font-medium text-slate-700 mb-1">{{ '邮箱' }}</label>
          <input
            v-model="form.email"
            type="email"
            class="input"
            placeholder="请输入注册时使用的邮箱"
            required
          />
        </div>

        <!-- 图形验证码 -->
        <div>
          <label class="block text-sm font-medium text-slate-700 mb-1">{{ '图形验证码' }}</label>
          <div class="flex gap-2 items-center">
            <input
              v-model="form.captchaAnswer"
              type="text"
              class="input flex-1"
              placeholder="请计算结果"
              required
            />
            <div class="cursor-pointer flex-shrink-0 h-[40px] rounded-md overflow-hidden border border-slate-200" @click="refreshGraphCaptcha" title="点击刷新">
              <img v-if="graphCaptchaImage" :src="graphCaptchaImage" alt="图形验证码" class="h-full w-[120px] object-cover" />
              <div v-else class="h-full w-[120px] flex items-center justify-center bg-slate-50 text-xs text-gray-400">
                {{ '点击获取' }}
              </div>
            </div>
          </div>
        </div>

        <!-- 邮箱验证码 -->
        <div>
          <label class="block text-sm font-medium text-slate-700 mb-1">{{ '邮箱验证码' }}</label>
          <div class="flex gap-2">
            <input
              v-model="form.code"
              type="text"
              class="input flex-1"
              placeholder="请输入6位验证码"
              required
              maxlength="6"
            />
            <button
              type="button"
              class="btn-outline whitespace-nowrap disabled:opacity-50 disabled:cursor-not-allowed"
              :disabled="codeCooldown > 0 || !form.email || !form.captchaAnswer"
              @click="handleSendCode"
            >
              {{ codeCooldown > 0 ? `${codeCooldown}s` : '获取验证码' }}
            </button>
          </div>
        </div>

        <!-- 新密码 -->
        <div>
          <label class="block text-sm font-medium text-slate-700 mb-1">{{ '新密码' }}</label>
          <div class="relative">
            <input
              v-model="form.newPassword"
              :type="showPassword ? 'text' : 'password'"
              class="input pr-10"
              placeholder="请输入新密码（需包含字母和数字，8-32位）"
              required
              minlength="8"
              maxlength="32"
            />
            <button
              type="button"
              class="absolute right-3 top-1/2 -translate-y-1/2 w-5 h-5 flex items-center justify-center text-gray-400 hover:text-slate-600 select-none"
              @mousedown.prevent="showPassword = !showPassword"
              @touchstart.prevent="showPassword = !showPassword"
            >
              <svg v-if="!showPassword" xmlns="http://www.w3.org/2000/svg" class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.5">
                <path stroke-linecap="round" stroke-linejoin="round" d="M2.036 12.322a1.012 1.012 0 010-.639C3.423 7.51 7.36 4.5 12 4.5c4.638 0 8.573 3.007 9.963 7.178.07.207.07.431 0 .639C20.577 16.49 16.64 19.5 12 19.5c-4.638 0-8.573-3.007-9.963-7.178z" />
                <path stroke-linecap="round" stroke-linejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
              </svg>
              <svg v-else xmlns="http://www.w3.org/2000/svg" class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.5">
                <path stroke-linecap="round" stroke-linejoin="round" d="M3.98 8.223A10.477 10.477 0 001.934 12C3.226 16.338 7.244 19.5 12 19.5c.993 0 1.953-.138 2.863-.395M6.228 6.228A10.45 10.45 0 0112 4.5c4.756 0 8.773 3.162 10.065 7.498a10.523 10.523 0 01-4.293 5.774M6.228 6.228L3 3m3.228 3.228l3.65 3.65m7.894 7.894L21 21m-3.228-3.228l-3.65-3.65m0 0a3 3 0 10-4.243-4.243m4.242 4.242L9.88 9.88" />
              </svg>
            </button>
          </div>
          <p class="text-xs text-gray-400 mt-1">{{ '密码需包含字母和数字，长度8-32位' }}</p>
        </div>

        <!-- 确认密码 -->
        <div>
          <label class="block text-sm font-medium text-slate-700 mb-1">{{ '确认密码' }}</label>
          <div class="relative">
            <input
              v-model="form.confirmPassword"
              :type="showConfirmPassword ? 'text' : 'password'"
              class="input pr-10"
              placeholder="请再次输入新密码"
              required
            />
            <button
              type="button"
              class="absolute right-3 top-1/2 -translate-y-1/2 w-5 h-5 flex items-center justify-center text-gray-400 hover:text-slate-600 select-none"
              @mousedown.prevent="showConfirmPassword = !showConfirmPassword"
              @touchstart.prevent="showConfirmPassword = !showConfirmPassword"
            >
              <svg v-if="!showConfirmPassword" xmlns="http://www.w3.org/2000/svg" class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.5">
                <path stroke-linecap="round" stroke-linejoin="round" d="M2.036 12.322a1.012 1.012 0 010-.639C3.423 7.51 7.36 4.5 12 4.5c4.638 0 8.573 3.007 9.963 7.178.07.207.07.431 0 .639C20.577 16.49 16.64 19.5 12 19.5c-4.638 0-8.573-3.007-9.963-7.178z" />
                <path stroke-linecap="round" stroke-linejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
              </svg>
              <svg v-else xmlns="http://www.w3.org/2000/svg" class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.5">
                <path stroke-linecap="round" stroke-linejoin="round" d="M3.98 8.223A10.477 10.477 0 001.934 12C3.226 16.338 7.244 19.5 12 19.5c.993 0 1.953-.138 2.863-.395M6.228 6.228A10.45 10.45 0 0112 4.5c4.756 0 8.773 3.162 10.065 7.498a10.523 10.523 0 01-4.293 5.774M6.228 6.228L3 3m3.228 3.228l3.65 3.65m7.894 7.894L21 21m-3.228-3.228l-3.65-3.65m0 0a3 3 0 10-4.243-4.243m4.242 4.242L9.88 9.88" />
              </svg>
            </button>
          </div>
        </div>

        <!-- 重置按钮 -->
        <button
          type="submit"
          class="btn-primary w-full"
          :disabled="loading"
        >
          {{ loading ? '提交中...' : '重置密码' }}
        </button>
      </form>

      <!-- 跳转登录 -->
      <p class="text-center text-sm text-slate-500 mt-4">
        {{ '记起密码了？' }}
        <NuxtLink to="/login" class="text-primary hover:text-primary-600 font-medium">{{ '返回登录' }}</NuxtLink>
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
/** 忘记密码页 */

definePageMeta({
  layout: 'blank',
  middleware: 'guest',
})

const { forgotPassword, sendCode, getGraphCaptcha } = useAuth()

const form = reactive({
  username: '',
  email: '',
  captchaAnswer: '',
  code: '',
  newPassword: '',
  confirmPassword: '',
})

const loading = ref(false)
const showPassword = ref(false)
const showConfirmPassword = ref(false)
const codeCooldown = ref(0)
const graphCaptchaKey = ref('')
const graphCaptchaImage = ref('')
let cooldownTimer: ReturnType<typeof setInterval> | null = null

// 获取图形验证码
const refreshGraphCaptcha = async () => {
  try {
    const data = await getGraphCaptcha()
    graphCaptchaKey.value = data.captchaKey
    graphCaptchaImage.value = data.image
  } catch (err: any) {
    showAlert(err.message || '验证码获取失败')
  }
}

// 页面加载时获取图形验证码
onMounted(() => {
  refreshGraphCaptcha()
})

// 发送邮箱验证码
const handleSendCode = async () => {
  if (!form.email) {
    showAlert('请先填写邮箱')
    return
  }
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) {
    showAlert('邮箱格式不对哦')
    return
  }
  if (!form.captchaAnswer) {
    showAlert('请先完成图形验证')
    return
  }
  try {
    await sendCode({
      email: form.email,
      purpose: 'resetPassword',
      captchaKey: graphCaptchaKey.value,
      captchaAnswer: form.captchaAnswer.replace(/[０-９]/g, c => String.fromCharCode(c.charCodeAt(0) - 0xFEE0)).replace(/−/g, '-').replace(/＋/g, '+'),
    })
    // 开始倒计时
    codeCooldown.value = 60
    cooldownTimer = setInterval(() => {
      codeCooldown.value--
      if (codeCooldown.value <= 0 && cooldownTimer) {
        clearInterval(cooldownTimer)
        cooldownTimer = null
      }
    }, 1000)
    // 发送成功后刷新图形验证码（验证码key已被后端消费），但不清空用户输入
    refreshGraphCaptcha()
  } catch (err: any) {
    const errMsg = err.message || '验证码发送失败'
    showAlert(errMsg)
    // 判断是否为图形验证码错误，仅此时清空图形验证码输入
    const isCaptchaError = errMsg.includes('图形验证码') || errMsg.includes('验证码错误') || errMsg.includes('验证码过期')
    if (isCaptchaError) {
      form.captchaAnswer = ''
    }
    // 失败后刷新图形验证码
    refreshGraphCaptcha()
  }
}

// 处理重置密码
const handleReset = async () => {
  if (!form.username) {
    showAlert('请填写用户名')
    return
  }
  if (!form.email) {
    showAlert('请填写邮箱')
    return
  }
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) {
    showAlert('邮箱格式不对哦')
    return
  }
  if (!form.code || form.code.length !== 6) {
    showAlert('请填写6位验证码')
    return
  }
  if (form.newPassword.length < 8 || form.newPassword.length > 32) {
    showAlert('密码长度需为8-32位')
    return
  }
  if (!/[a-zA-Z]/.test(form.newPassword) || !/\d/.test(form.newPassword)) {
    showAlert('密码需包含字母和数字')
    return
  }
  if (form.newPassword !== form.confirmPassword) {
    showAlert('两次密码不一致')
    return
  }

  loading.value = true

  try {
    await forgotPassword({
      username: form.username,
      email: form.email,
      code: form.code,
      newPassword: form.newPassword,
    })
    navigateTo('/login')
  } catch (err: any) {
    showAlert(err.message || '重置失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

useHead({
  title: () => `${'重置密码'} - 知讯`,
})

onUnmounted(() => {
  if (cooldownTimer) {
    clearInterval(cooldownTimer)
  }
})
</script>
