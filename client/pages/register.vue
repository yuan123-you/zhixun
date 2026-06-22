<template>
  <!-- 注册页 -->
  <div class="w-full max-w-md mx-auto px-4">
    <div class="card p-8">
      <!-- Logo和标题 -->
      <div class="text-center mb-8">
        <div class="w-16 h-16 bg-primary rounded-2xl flex items-center justify-center mx-auto mb-4">
          <span class="text-white font-bold text-2xl">知</span>
        </div>
        <h1 class="text-2xl font-bold text-gray-900 dark:text-white">注册知讯</h1>
        <p class="text-sm text-gray-500 dark:text-gray-400 mt-2">加入知讯，开启你的知识之旅</p>
      </div>

      <!-- 注册表单 -->
      <form class="space-y-4" @submit.prevent="handleRegister">
        <!-- 用户名 -->
        <div>
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">用户名</label>
          <input
            v-model="form.username"
            type="text"
            class="input"
            placeholder="请输入用户名"
            required
          />
        </div>

        <!-- 密码 -->
        <div>
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">密码</label>
          <input
            v-model="form.password"
            type="password"
            class="input"
            placeholder="请输入密码（至少6位）"
            required
            minlength="6"
          />
        </div>

        <!-- 确认密码 -->
        <div>
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">确认密码</label>
          <input
            v-model="form.confirmPassword"
            type="password"
            class="input"
            placeholder="请再次输入密码"
            required
          />
        </div>

        <!-- 错误提示 -->
        <p v-if="error" class="text-sm text-danger">{{ error }}</p>

        <!-- 注册按钮 -->
        <button
          type="submit"
          class="btn-primary w-full"
          :disabled="loading"
        >
          {{ loading ? '注册中...' : '注册' }}
        </button>
      </form>

      <!-- 跳转登录 -->
      <p class="text-center text-sm text-gray-500 dark:text-gray-400 mt-6">
        已有账号？
        <NuxtLink to="/login" class="text-primary hover:text-primary-600 font-medium">立即登录</NuxtLink>
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
/** 注册页 */

definePageMeta({
  layout: 'blank',
  middleware: 'guest',
})

const { register } = useAuth()

const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
})

const loading = ref(false)
const error = ref('')

// 处理注册
const handleRegister = async () => {
  error.value = ''

  // 验证密码一致
  if (form.password !== form.confirmPassword) {
    error.value = '两次输入的密码不一致'
    return
  }

  // 验证密码长度
  if (form.password.length < 6) {
    error.value = '密码长度至少6位'
    return
  }

  loading.value = true

  try {
    await register(form)
    navigateTo('/')
  } catch (err: any) {
    error.value = err.message || '注册失败，请重试'
  } finally {
    loading.value = false
  }
}

// 页面元信息
useHead({
  title: '注册 - 知讯',
})
</script>
