<template>
  <!-- 登录页 -->
  <div class="w-full max-w-md mx-auto px-4">
    <div class="card p-8">
      <!-- Logo和标题 -->
      <div class="text-center mb-8">
        <div class="w-16 h-16 bg-primary rounded-2xl flex items-center justify-center mx-auto mb-4">
          <span class="text-white font-bold text-2xl">知</span>
        </div>
        <h1 class="text-2xl font-bold text-gray-900 dark:text-white">登录知讯</h1>
        <p class="text-sm text-gray-500 dark:text-gray-400 mt-2">发现优质内容，分享知识与见解</p>
      </div>

      <!-- 登录表单 -->
      <form class="space-y-4" @submit.prevent="handleLogin">
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
            placeholder="请输入密码"
            required
          />
        </div>

        <!-- 忘记密码 -->
        <div class="text-right">
          <NuxtLink to="/forgot-password" class="text-sm text-primary hover:text-primary-600">忘记密码？</NuxtLink>
        </div>

        <!-- 错误提示 -->
        <p v-if="error" class="text-sm text-danger">{{ error }}</p>

        <!-- 登录按钮 -->
        <button
          type="submit"
          class="btn-primary w-full"
          :disabled="loading"
        >
          {{ loading ? '登录中...' : '登录' }}
        </button>
      </form>

      <!-- 跳转注册 -->
      <p class="text-center text-sm text-gray-500 dark:text-gray-400 mt-6">
        还没有账号？
        <NuxtLink to="/register" class="text-primary hover:text-primary-600 font-medium">立即注册</NuxtLink>
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
const route = useRoute()

const form = reactive({
  username: '',
  password: '',
})

const loading = ref(false)
const error = ref('')

// 处理登录
const handleLogin = async () => {
  error.value = ''
  loading.value = true

  try {
    await login(form)
    const redirect = (route.query.redirect as string) || '/'
    navigateTo(redirect)
  } catch (err: any) {
    error.value = err.message || '登录失败，请重试'
  } finally {
    loading.value = false
  }
}

// 页面元信息
useHead({
  title: '登录 - 知讯',
})
</script>
