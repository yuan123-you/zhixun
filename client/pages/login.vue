<template>
  <!-- 登录页 -->
  <div class="w-full max-w-md mx-auto px-4">
    <div class="card p-8">
      <!-- Logo和标题 -->
      <div class="text-center mb-8">
        <div class="w-16 h-16 bg-primary rounded-2xl flex items-center justify-center mx-auto mb-4">
          <span class="text-white font-bold text-2xl">知</span>
        </div>
        <h1 class="text-2xl font-bold text-gray-900 dark:text-white">{{ t('auth.loginTitle') }}</h1>
        <p class="text-sm text-gray-500 dark:text-gray-400 mt-2">{{ t('auth.loginDesc') }}</p>
      </div>

      <!-- 登录表单 -->
      <form class="space-y-4" @submit.prevent="handleLogin">
        <!-- 用户名 -->
        <div>
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">{{ t('user.username') }}</label>
          <input
            v-model="form.username"
            type="text"
            class="input"
            :placeholder="t('auth.enterUsername')"
            required
          />
        </div>

        <!-- 密码 -->
        <div>
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">{{ t('user.password') }}</label>
          <div class="relative">
            <input
              v-model="form.password"
              :type="showPassword ? 'text' : 'password'"
              class="input pr-10 w-full"
              :placeholder="t('auth.enterPassword')"
              autocomplete="current-password"
              required
            />
            <button
              type="button"
              tabindex="-1"
              class="absolute right-3 top-1/2 -translate-y-1/2 w-6 h-6 flex items-center justify-center text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 select-none"
              @mousedown.prevent="showPassword = !showPassword"
              @touchstart.prevent="showPassword = !showPassword"
            >
              <svg v-if="!showPassword" xmlns="http://www.w3.org/2000/svg" class="w-5 h-5 pointer-events-none" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.5">
                <path stroke-linecap="round" stroke-linejoin="round" d="M2.036 12.322a1.012 1.012 0 010-.639C3.423 7.51 7.36 4.5 12 4.5c4.638 0 8.573 3.007 9.963 7.178.07.207.07.431 0 .639C20.577 16.49 16.64 19.5 12 19.5c-4.638 0-8.573-3.007-9.963-7.178z" />
                <path stroke-linecap="round" stroke-linejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
              </svg>
              <svg v-else xmlns="http://www.w3.org/2000/svg" class="w-5 h-5 pointer-events-none" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.5">
                <path stroke-linecap="round" stroke-linejoin="round" d="M3.98 8.223A10.477 10.477 0 001.934 12C3.226 16.338 7.244 19.5 12 19.5c.993 0 1.953-.138 2.863-.395M6.228 6.228A10.45 10.45 0 0112 4.5c4.756 0 8.773 3.162 10.065 7.498a10.523 10.523 0 01-4.293 5.774M6.228 6.228L3 3m3.228 3.228l3.65 3.65m7.894 7.894L21 21m-3.228-3.228l-3.65-3.65m0 0a3 3 0 10-4.243-4.243m4.242 4.242L9.88 9.88" />
              </svg>
            </button>
          </div>
        </div>

        <!-- 忘记密码 -->
        <div class="text-right">
          <NuxtLink to="/forgot-password" class="text-sm text-primary hover:text-primary-600">{{ t('auth.forgotPassword') }}</NuxtLink>
        </div>

        <!-- 登录按钮 -->
        <button
          type="submit"
          class="btn-primary w-full transition-all duration-200"
          :disabled="loading"
        >
          {{ loading ? t('auth.logining') : t('common.login') }}
        </button>
      </form>

      <!-- 跳转注册 -->
      <p class="text-center text-sm text-gray-500 dark:text-gray-400 mt-6">
        {{ t('auth.noAccount') }}
        <NuxtLink to="/register" class="text-primary hover:text-primary-600 font-medium">{{ t('auth.registerNow') }}</NuxtLink>
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
/** 登录页 */

definePageMeta({
  layout: 'blank',
  middleware: 'guest',
})

const { login } = useAuth()
const { t } = useI18n()
const route = useRoute()

const form = reactive({
  username: '',
  password: '',
})

const loading = ref(false)
const showPassword = ref(false)

// 处理登录
const handleLogin = async () => {
  loading.value = true

  try {
    await login(form)
    const redirect = (route.query.redirect as string) || '/'
    navigateTo(redirect)
  } catch (err: any) {
    showAlert(err.message || t('auth.loginFailed'))
  } finally {
    loading.value = false
  }
}

// 页面元信息
useHead({
  title: () => `${t('common.login')} - 知讯`,
})
</script>
