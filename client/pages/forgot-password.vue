<template>
  <!-- 忘记密码页 -->
  <div class="w-full max-w-md mx-auto px-4">
    <div class="card p-8">
      <!-- Logo和标题 -->
      <div class="text-center mb-8">
        <div class="w-16 h-16 bg-primary rounded-2xl flex items-center justify-center mx-auto mb-4">
          <span class="text-white font-bold text-2xl">知</span>
        </div>
        <h1 class="text-2xl font-bold text-gray-900 dark:text-white">{{ $t('auth.forgotTitle') }}</h1>
        <p class="text-sm text-gray-500 dark:text-gray-400 mt-2">{{ $t('auth.forgotDesc') }}</p>
      </div>

      <!-- 重置密码表单 -->
      <form class="space-y-4" @submit.prevent="handleReset">
        <!-- 用户名 -->
        <div>
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">{{ $t('user.username') }}</label>
          <input
            v-model="form.username"
            type="text"
            class="input"
            :placeholder="$t('auth.enterUsername')"
            required
          />
        </div>

        <!-- 邮箱 -->
        <div>
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">{{ $t('user.email') }}</label>
          <input
            v-model="form.email"
            type="email"
            class="input"
            :placeholder="$t('auth.enterRegisteredEmail')"
            required
          />
        </div>

        <!-- 图形验证码 -->
        <div>
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">{{ $t('auth.graphCaptcha') }}</label>
          <div class="flex gap-2 items-center">
            <input
              v-model="form.captchaAnswer"
              type="text"
              class="input flex-1"
              :placeholder="$t('auth.enterCaptchaResult')"
              required
            />
            <div class="cursor-pointer flex-shrink-0 h-[40px] rounded-md overflow-hidden border border-gray-200 dark:border-gray-700" @click="refreshGraphCaptcha" :title="$t('auth.clickToRefresh')">
              <img v-if="graphCaptchaImage" :src="graphCaptchaImage" :alt="$t('auth.graphCaptcha')" class="h-full w-[120px] object-cover" />
              <div v-else class="h-full w-[120px] flex items-center justify-center bg-gray-100 dark:bg-gray-800 text-xs text-gray-400">
                {{ $t('auth.clickToGet') }}
              </div>
            </div>
          </div>
        </div>

        <!-- 邮箱验证码 -->
        <div>
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">{{ $t('auth.emailCaptcha') }}</label>
          <div class="flex gap-2">
            <input
              v-model="form.code"
              type="text"
              class="input flex-1"
              :placeholder="$t('auth.enter6DigitCode')"
              required
              maxlength="6"
            />
            <button
              type="button"
              class="btn-outline whitespace-nowrap disabled:opacity-50 disabled:cursor-not-allowed"
              :disabled="codeCooldown > 0 || !form.email || !form.captchaAnswer"
              @click="handleSendCode"
            >
              {{ codeCooldown > 0 ? `${codeCooldown}s` : $t('auth.getCode') }}
            </button>
          </div>
        </div>

        <!-- 新密码 -->
        <div>
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">{{ $t('user.newPassword') }}</label>
          <div class="relative">
            <input
              v-model="form.newPassword"
              :type="showPassword ? 'text' : 'password'"
              class="input pr-10"
              :placeholder="$t('auth.enterNewPassword')"
              required
              minlength="8"
              maxlength="32"
            />
            <button
              type="button"
              class="absolute right-3 top-1/2 -translate-y-1/2 w-6 h-6 flex items-center justify-center text-gray-400 hover:text-gray-600 dark:hover:text-gray-300"
              @click="showPassword = !showPassword"
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
          <p class="text-xs text-gray-400 mt-1">{{ $t('auth.passwordLengthRequirement') }}</p>
        </div>

        <!-- 确认密码 -->
        <div>
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">{{ $t('user.confirmPassword') }}</label>
          <div class="relative">
            <input
              v-model="form.confirmPassword"
              :type="showConfirmPassword ? 'text' : 'password'"
              class="input pr-10"
              :placeholder="$t('auth.enterNewPasswordAgain')"
              required
            />
            <button
              type="button"
              class="absolute right-3 top-1/2 -translate-y-1/2 w-6 h-6 flex items-center justify-center text-gray-400 hover:text-gray-600 dark:hover:text-gray-300"
              @click="showConfirmPassword = !showConfirmPassword"
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
          {{ loading ? $t('auth.submitting') : $t('auth.forgotTitle') }}
        </button>
      </form>

      <!-- 跳转登录 -->
      <p class="text-center text-sm text-gray-500 dark:text-gray-400 mt-6">
        {{ $t('auth.rememberPassword') }}
        <NuxtLink to="/login" class="text-primary hover:text-primary-600 font-medium">{{ $t('auth.backToLogin') }}</NuxtLink>
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
const { t } = useI18n()

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
    showAlert(err.message || t('auth.getCaptchaFailed'))
  }
}

// 页面加载时获取图形验证码
onMounted(() => {
  refreshGraphCaptcha()
})

// 发送邮箱验证码
const handleSendCode = async () => {
  if (!form.email) {
    showAlert(t('auth.enterEmailFirst'))
    return
  }
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) {
    showAlert(t('auth.invalidEmail'))
    return
  }
  if (!form.captchaAnswer) {
    showAlert(t('auth.completeCaptchaFirst'))
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
    // 发送成功后刷新图形验证码
    refreshGraphCaptcha()
    form.captchaAnswer = ''
  } catch (err: any) {
    showAlert(err.message || t('auth.sendCodeFailed'))
    // 失败后也刷新图形验证码
    refreshGraphCaptcha()
    form.captchaAnswer = ''
  }
}

// 处理重置密码
const handleReset = async () => {
  if (!form.username) {
    showAlert(t('auth.inputUsername'))
    return
  }
  if (!form.email) {
    showAlert(t('auth.inputEmail'))
    return
  }
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) {
    showAlert(t('auth.invalidEmail'))
    return
  }
  if (!form.code || form.code.length !== 6) {
    showAlert(t('auth.input6DigitCode'))
    return
  }
  if (form.newPassword.length < 8 || form.newPassword.length > 32) {
    showAlert(t('auth.passwordLengthError'))
    return
  }
  if (!/[a-zA-Z]/.test(form.newPassword) || !/\d/.test(form.newPassword)) {
    showAlert(t('auth.passwordRequirementError'))
    return
  }
  if (form.newPassword !== form.confirmPassword) {
    showAlert(t('auth.passwordMismatch'))
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
    showAlert(err.message || t('auth.resetFailed'))
  } finally {
    loading.value = false
  }
}

useHead({
  title: () => `${t('auth.forgotTitle')} - 知讯`,
})

onUnmounted(() => {
  if (cooldownTimer) {
    clearInterval(cooldownTimer)
  }
})
</script>
