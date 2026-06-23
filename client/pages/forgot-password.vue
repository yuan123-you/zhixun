<template>
  <!-- 忘记密码页 -->
  <div class="w-full max-w-md mx-auto px-4">
    <div class="card p-8">
      <!-- Logo和标题 -->
      <div class="text-center mb-8">
        <div class="w-16 h-16 bg-primary rounded-2xl flex items-center justify-center mx-auto mb-4">
          <span class="text-white font-bold text-2xl">知</span>
        </div>
        <h1 class="text-2xl font-bold text-gray-900 dark:text-white">忘记密码</h1>
        <p class="text-sm text-gray-500 dark:text-gray-400 mt-2">通过邮箱验证码重置密码</p>
      </div>

      <!-- 重置密码表单 -->
      <form class="space-y-4" @submit.prevent="handleResetPassword">
        <!-- 用户名 -->
        <div>
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">用户名</label>
          <input
            v-model="form.username"
            type="text"
            class="input"
            placeholder="请输入注册时的用户名"
            required
          />
        </div>

        <!-- 邮箱 -->
        <div>
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">邮箱</label>
          <input
            v-model="form.email"
            type="email"
            class="input"
            placeholder="请输入注册时的邮箱"
            required
          />
        </div>

        <!-- 邮箱验证码 -->
        <div>
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">验证码</label>
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
              :disabled="codeCooldown > 0 || !form.email"
              @click="handleSendCode"
            >
              {{ codeCooldown > 0 ? `${codeCooldown}s` : '获取验证码' }}
            </button>
          </div>
        </div>

        <!-- 新密码 -->
        <div>
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">新密码</label>
          <input
            v-model="form.newPassword"
            type="password"
            class="input"
            placeholder="请输入新密码（需包含大小写字母和数字，至少8位）"
            required
            minlength="8"
          />
          <p class="text-xs text-gray-400 mt-1">密码需包含大写字母、小写字母和数字</p>
        </div>

        <!-- 确认新密码 -->
        <div>
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">确认新密码</label>
          <input
            v-model="form.confirmPassword"
            type="password"
            class="input"
            placeholder="请再次输入新密码"
            required
          />
        </div>

        <!-- 错误提示 -->
        <p v-if="error" class="text-sm text-danger">{{ error }}</p>

        <!-- 成功提示 -->
        <p v-if="success" class="text-sm text-green-600">{{ success }}</p>

        <!-- 重置按钮 -->
        <button
          type="submit"
          class="btn-primary w-full"
          :disabled="loading"
        >
          {{ loading ? '重置中...' : '重置密码' }}
        </button>
      </form>

      <!-- 跳转登录 -->
      <p class="text-center text-sm text-gray-500 dark:text-gray-400 mt-6">
        记起密码了？
        <NuxtLink to="/login" class="text-primary hover:text-primary-600 font-medium">返回登录</NuxtLink>
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

const { sendCode, forgotPassword } = useAuth()

const form = reactive({
  username: '',
  email: '',
  code: '',
  newPassword: '',
  confirmPassword: '',
})

const loading = ref(false)
const error = ref('')
const success = ref('')
const codeCooldown = ref(0)
let cooldownTimer: ReturnType<typeof setInterval> | null = null

// 发送邮箱验证码
const handleSendCode = async () => {
  if (!form.email) {
    error.value = '请先输入邮箱'
    return
  }
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) {
    error.value = '邮箱格式不正确'
    return
  }
  error.value = ''
  try {
    await sendCode({ email: form.email, purpose: 'resetPassword' })
    codeCooldown.value = 60
    cooldownTimer = setInterval(() => {
      codeCooldown.value--
      if (codeCooldown.value <= 0 && cooldownTimer) {
        clearInterval(cooldownTimer)
        cooldownTimer = null
      }
    }, 1000)
  } catch (err: any) {
    error.value = err.message || '验证码发送失败'
  }
}

// 重置密码
const handleResetPassword = async () => {
  error.value = ''
  success.value = ''

  if (!form.username) {
    error.value = '请输入用户名'
    return
  }
  if (!form.email) {
    error.value = '请输入邮箱'
    return
  }
  if (!form.code || form.code.length !== 6) {
    error.value = '请输入6位验证码'
    return
  }
  if (form.newPassword !== form.confirmPassword) {
    error.value = '两次输入的密码不一致'
    return
  }
  if (form.newPassword.length < 8) {
    error.value = '密码长度至少8位'
    return
  }
  if (!/[a-z]/.test(form.newPassword) || !/[A-Z]/.test(form.newPassword) || !/\d/.test(form.newPassword)) {
    error.value = '密码需包含大写字母、小写字母和数字'
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
    success.value = '密码重置成功，即将跳转到登录页'
    setTimeout(() => {
      navigateTo('/login')
    }, 2000)
  } catch (err: any) {
    error.value = err.message || '重置密码失败，请重试'
  } finally {
    loading.value = false
  }
}

// 页面元信息
useHead({
  title: '忘记密码 - 知讯',
})

// 清理定时器
onUnmounted(() => {
  if (cooldownTimer) {
    clearInterval(cooldownTimer)
  }
})
</script>
