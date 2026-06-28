<template>
  <div class="w-full max-w-sm mx-auto px-3">
    <div class="card-auth">
      <!-- 品牌 -->
      <div class="auth-brand">
        <div class="auth-logo">知</div>
        <h1 class="auth-title">{{ '欢迎回来' }}</h1>
        <p class="auth-subtitle">{{ '登录你的知讯账号' }}</p>
      </div>

      <!-- 表单 -->
      <form class="auth-form" @submit.prevent="handleLogin">
        <!-- 用户名 -->
        <div class="input-group">
          <label class="input-label">
            <el-icon :size="16" class="input-label-icon"><User /></el-icon>
            用户名
          </label>
          <div class="input-field" :class="{ focused: activeField === 'username' }">
            <el-icon class="input-prefix" :size="18"><User /></el-icon>
            <input
              v-model="form.username"
              type="text"
              class="input-control"
              placeholder="请输入用户名"
              autocomplete="username"
              required
              @focus="activeField = 'username'"
              @blur="activeField = ''"
            />
          </div>
        </div>

        <!-- 密码 -->
        <div class="input-group">
          <label class="input-label">
            <el-icon :size="16" class="input-label-icon"><Lock /></el-icon>
            密码
          </label>
          <div class="input-field" :class="{ focused: activeField === 'password' }">
            <el-icon class="input-prefix" :size="18"><Lock /></el-icon>
            <input
              v-model="form.password"
              :type="showPassword ? 'text' : 'password'"
              class="input-control"
              placeholder="请输入密码"
              autocomplete="current-password"
              required
              @focus="activeField = 'password'"
              @blur="activeField = ''"
            />
            <button type="button" tabindex="-1" class="input-suffix"
              @mousedown.prevent="showPassword = !showPassword"
              @touchstart.prevent="showPassword = !showPassword">
              <el-icon :size="18"><View v-if="!showPassword" /><Hide v-else /></el-icon>
            </button>
          </div>
          <div class="text-right mt-1.5">
            <RouterLink to="/forgot-password" class="auth-link">{{ '忘记密码？' }}</RouterLink>
          </div>
        </div>

        <!-- 登录按钮 -->
        <button type="submit" class="auth-btn" :disabled="loading">
          <span v-if="!loading">{{ '登 录' }}</span>
          <el-icon v-else class="is-loading" :size="20"><Loading /></el-icon>
        </button>
      </form>

      <!-- 注册引导 -->
      <p class="auth-footer">
        {{ '还没有账号？' }}
        <RouterLink to="/register" class="auth-link font-semibold">{{ '立即注册' }}</RouterLink>
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { User, Lock, View, Hide, Loading } from '@element-plus/icons-vue'

const { login } = useAuth()
const route = useRoute()

const form = reactive({ username: '', password: '' })
const loading = ref(false)
const showPassword = ref(false)
const activeField = ref('')

const handleLogin = async () => {
  loading.value = true
  try {
    await login(form)
    const redirect = (route.query.redirect as string) || '/'
    navigateTo(redirect)
  } catch (err: any) {
    showAlert(err.message || '账号或密码不正确')
  } finally {
    loading.value = false
  }
}

useHead({ title: () => `登录 - 知讯` })
</script>

<style scoped>
/* 卡片 */
.card-auth {
  background: var(--zh-bg-elevated);
  border: 1px solid var(--zh-border);
  border-radius: var(--zh-radius-xl);
  box-shadow: var(--zh-shadow-lg);
  padding: 28px 24px 24px;
}

/* 品牌区 */
.auth-brand { text-align: center; margin-bottom: 24px; }
.auth-logo {
  width: 48px; height: 48px;
  border-radius: var(--zh-radius-lg);
  background: var(--zh-primary-gradient);
  color: #fff;
  font-size: 24px; font-weight: 800;
  display: inline-flex;
  align-items: center; justify-content: center;
  box-shadow: 0 4px 14px rgba(var(--zh-primary-rgb), 0.3);
  margin-bottom: 14px;
}
.auth-title {
  font-size: 22px; font-weight: 700;
  color: var(--zh-text);
  letter-spacing: -0.5px;
}
.auth-subtitle {
  font-size: 13px; color: var(--zh-text-tertiary);
  margin-top: 6px;
}

/* 表单 */
.auth-form { display: flex; flex-direction: column; gap: 16px; }

/* 输入组 */
.input-group { display: flex; flex-direction: column; gap: 6px; }
.input-label {
  font-size: 13px; font-weight: 600;
  color: var(--zh-text-secondary);
  display: inline-flex; align-items: center; gap: 6px;
}
.input-label-icon { color: var(--zh-text-tertiary); }

/* 输入框容器 */
.input-field {
  display: flex; align-items: center;
  height: 44px;
  border: 1.5px solid var(--zh-border);
  border-radius: var(--zh-radius-md);
  background: var(--zh-bg);
  transition: border-color var(--zh-transition-fast), box-shadow var(--zh-transition-fast);
  overflow: hidden;
}
.input-field.focused {
  border-color: var(--zh-primary);
  box-shadow: 0 0 0 3px rgba(var(--zh-primary-rgb), 0.08);
}

.input-prefix {
  color: var(--zh-text-tertiary);
  margin-left: 12px;
  flex-shrink: 0;
  transition: color var(--zh-transition-fast);
}
.input-field.focused .input-prefix { color: var(--zh-primary); }

.input-control {
  flex: 1; height: 100%;
  border: none; outline: none;
  background: transparent;
  padding: 0 10px;
  font-size: 14px; color: var(--zh-text);
  font-family: inherit;
}
.input-control::placeholder { color: var(--zh-text-placeholder); font-size: 13px; }

.input-suffix {
  display: flex; align-items: center; justify-content: center;
  width: 40px; height: 100%;
  background: none; border: none;
  color: var(--zh-text-tertiary);
  cursor: pointer;
  transition: color var(--zh-transition-fast);
  flex-shrink: 0;
}
.input-suffix:hover { color: var(--zh-text-secondary); }

/* 按钮 */
.auth-btn {
  width: 100%; height: 44px;
  border: none; border-radius: var(--zh-radius-md);
  background: var(--zh-primary-gradient);
  color: #fff;
  font-size: 15px; font-weight: 600;
  letter-spacing: 2px;
  cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  transition: opacity var(--zh-transition-fast), transform var(--zh-transition-fast);
  box-shadow: 0 4px 14px rgba(var(--zh-primary-rgb), 0.25);
  margin-top: 4px;
}
.auth-btn:hover:not(:disabled) { opacity: 0.92; transform: translateY(-1px); }
.auth-btn:active:not(:disabled) { transform: scale(0.98); }
.auth-btn:disabled { opacity: 0.6; cursor: not-allowed; }

/* 链接 */
.auth-link {
  font-size: 13px; color: var(--zh-primary);
  text-decoration: none;
  transition: opacity var(--zh-transition-fast);
}
.auth-link:hover { opacity: 0.75; }

/* 底部 */
.auth-footer {
  text-align: center; font-size: 13px;
  color: var(--zh-text-tertiary);
  margin-top: 20px; padding-top: 16px;
  border-top: 1px solid var(--zh-border-light);
}
</style>
