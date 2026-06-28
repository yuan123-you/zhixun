<template>
  <div class="w-full max-w-sm mx-auto px-3">
    <div class="card-auth">
      <div class="auth-brand">
        <div class="auth-logo">知</div>
        <h1 class="auth-title">{{ '重置密码' }}</h1>
        <p class="auth-subtitle">{{ '通过邮箱验证重置你的密码' }}</p>
      </div>

      <form class="auth-form" @submit.prevent="handleReset">
        <!-- 用户名 -->
        <div class="input-group">
          <label class="input-label">
            <el-icon :size="16" class="input-label-icon"><User /></el-icon>用户名
          </label>
          <div class="input-field" :class="{ focused: activeField === 'username' }">
            <el-icon class="input-prefix" :size="18"><User /></el-icon>
            <input v-model="form.username" type="text" class="input-control"
              placeholder="请输入用户名" autocomplete="username" required
              @focus="activeField = 'username'" @blur="activeField = ''" />
          </div>
        </div>

        <!-- 邮箱 -->
        <div class="input-group">
          <label class="input-label">
            <el-icon :size="16" class="input-label-icon"><Message /></el-icon>邮箱
          </label>
          <div class="input-field" :class="{ focused: activeField === 'email' }">
            <el-icon class="input-prefix" :size="18"><Message /></el-icon>
            <input v-model="form.email" type="email" class="input-control"
              placeholder="注册时使用的邮箱" required
              @focus="activeField = 'email'" @blur="activeField = ''" />
          </div>
        </div>

        <!-- 图形验证码 -->
        <div class="input-group">
          <label class="input-label">
            <el-icon :size="16" class="input-label-icon"><Key /></el-icon>验证
          </label>
          <div class="flex gap-2">
            <div class="input-field flex-1" :class="{ focused: activeField === 'captcha' }">
              <el-icon class="input-prefix" :size="18"><Key /></el-icon>
              <input v-model="form.captchaAnswer" type="text" class="input-control"
                placeholder="计算结果" required
                @focus="activeField = 'captcha'" @blur="activeField = ''" />
            </div>
            <div class="captcha-img" @click="refreshGraphCaptcha" title="点击刷新">
              <img v-if="graphCaptchaImage" :src="graphCaptchaImage" alt="验证码" class="h-full w-full object-cover" />
              <div v-else class="h-full w-full flex items-center justify-center bg-[var(--zh-bg-hover)] text-xs text-[var(--zh-text-tertiary)]">点击获取</div>
            </div>
          </div>
        </div>

        <!-- 邮箱验证码 -->
        <div class="input-group">
          <label class="input-label">
            <el-icon :size="16" class="input-label-icon"><Timer /></el-icon>验证码
          </label>
          <div class="flex gap-2">
            <div class="input-field flex-1" :class="{ focused: activeField === 'code' }">
              <el-icon class="input-prefix" :size="18"><Unlock /></el-icon>
              <input v-model="form.code" type="text" class="input-control"
                placeholder="6位验证码" required maxlength="6"
                @focus="activeField = 'code'" @blur="activeField = ''" />
            </div>
            <button type="button" class="code-btn"
              :disabled="codeCooldown > 0 || !form.email || !form.captchaAnswer"
              @click="handleSendCode">
              {{ codeCooldown > 0 ? `${codeCooldown}s` : '获取验证码' }}
            </button>
          </div>
        </div>

        <!-- 新密码 -->
        <div class="input-group">
          <label class="input-label">
            <el-icon :size="16" class="input-label-icon"><Lock /></el-icon>新密码
          </label>
          <div class="input-field" :class="{ focused: activeField === 'password' }">
            <el-icon class="input-prefix" :size="18"><Lock /></el-icon>
            <input v-model="form.newPassword" :type="showPassword ? 'text' : 'password'"
              class="input-control" placeholder="字母+数字，8-32位"
              autocomplete="new-password" required minlength="8" maxlength="32"
              @focus="activeField = 'password'" @blur="activeField = ''" />
            <button type="button" tabindex="-1" class="input-suffix"
              @mousedown.prevent="showPassword = !showPassword"
              @touchstart.prevent="showPassword = !showPassword">
              <el-icon :size="18"><View v-if="!showPassword" /><Hide v-else /></el-icon>
            </button>
          </div>
          <p class="text-xs text-[var(--zh-text-tertiary)] mt-1 ml-1">8-32位，需含字母和数字</p>
        </div>

        <!-- 确认密码 -->
        <div class="input-group">
          <div class="input-field" :class="{ focused: activeField === 'confirm' }">
            <el-icon class="input-prefix" :size="18"><Lock /></el-icon>
            <input v-model="form.confirmPassword" :type="showConfirmPassword ? 'text' : 'password'"
              class="input-control" placeholder="再次输入密码"
              autocomplete="new-password" required
              @focus="activeField = 'confirm'" @blur="activeField = ''" />
            <button type="button" tabindex="-1" class="input-suffix"
              @mousedown.prevent="showConfirmPassword = !showConfirmPassword"
              @touchstart.prevent="showConfirmPassword = !showConfirmPassword">
              <el-icon :size="18"><View v-if="!showConfirmPassword" /><Hide v-else /></el-icon>
            </button>
          </div>
        </div>

        <button type="submit" class="auth-btn" :disabled="loading">
          <span v-if="!loading">{{ '重置密码' }}</span>
          <el-icon v-else class="is-loading" :size="20"><Loading /></el-icon>
        </button>
      </form>

      <p class="auth-footer">
        {{ '记起密码了？' }}
        <RouterLink to="/login" class="auth-link font-semibold">{{ '返回登录' }}</RouterLink>
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { User, Lock, View, Hide, Loading, Message, Key, Timer, Unlock } from '@element-plus/icons-vue'

const router = useRouter()
const { forgotPassword, sendCode, getGraphCaptcha } = useAuth()

const form = reactive({
  username: '', email: '', captchaAnswer: '',
  code: '', newPassword: '', confirmPassword: '',
})
const loading = ref(false)
const showPassword = ref(false)
const showConfirmPassword = ref(false)
const codeCooldown = ref(0)
const graphCaptchaKey = ref('')
const graphCaptchaImage = ref('')
const activeField = ref('')
let cooldownTimer: ReturnType<typeof setInterval> | null = null

const refreshGraphCaptcha = async () => {
  try {
    const data = await getGraphCaptcha()
    graphCaptchaKey.value = data.captchaKey
    graphCaptchaImage.value = data.image
  } catch (err: any) { showAlert(err.message || '获取失败') }
}
onMounted(() => refreshGraphCaptcha())

const handleSendCode = async () => {
  if (!form.email) return showAlert('请填写邮箱')
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) return showAlert('邮箱格式不对')
  if (!form.captchaAnswer) return showAlert('请完成图形验证')
  try {
    await sendCode({ email: form.email, purpose: 'resetPassword', captchaKey: graphCaptchaKey.value, captchaAnswer: form.captchaAnswer.replace(/[０-９]/g, c => String.fromCharCode(c.charCodeAt(0) - 0xFEE0)).replace(/−/g, '-').replace(/＋/g, '+') })
    codeCooldown.value = 60
    cooldownTimer = setInterval(() => { codeCooldown.value--; if (codeCooldown.value <= 0 && cooldownTimer) { clearInterval(cooldownTimer); cooldownTimer = null } }, 1000)
    refreshGraphCaptcha()
  } catch (err: any) { showAlert(err.message || '发送失败'); refreshGraphCaptcha() }
}

const handleReset = async () => {
  if (!form.username) return showAlert('请填写用户名')
  if (!form.email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) return showAlert('邮箱格式不对')
  if (!form.code || form.code.length !== 6) return showAlert('请填写6位验证码')
  if (form.newPassword.length < 8 || form.newPassword.length > 32) return showAlert('密码8-32位')
  if (!/[a-zA-Z]/.test(form.newPassword) || !/\d/.test(form.newPassword)) return showAlert('密码需含字母和数字')
  if (form.newPassword !== form.confirmPassword) return showAlert('两次密码不一致')
  loading.value = true
  try { await forgotPassword({ username: form.username, email: form.email, code: form.code, newPassword: form.newPassword }); router.push('/login') }
  catch (err: any) { showAlert(err.message || '重置失败') }
  finally { loading.value = false }
}

useHead({ title: () => `重置密码 - 知讯` })
onUnmounted(() => { if (cooldownTimer) clearInterval(cooldownTimer) })
</script>

<style scoped>
.card-auth {
  background: var(--zh-bg-elevated);
  border: 1px solid var(--zh-border);
  border-radius: var(--zh-radius-xl);
  box-shadow: var(--zh-shadow-lg);
  padding: 28px 24px 24px;
}
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
.auth-title { font-size: 22px; font-weight: 700; color: var(--zh-text); letter-spacing: -0.5px; }
.auth-subtitle { font-size: 13px; color: var(--zh-text-tertiary); margin-top: 6px; }
.auth-form { display: flex; flex-direction: column; gap: 16px; }
.input-group { display: flex; flex-direction: column; gap: 6px; }
.input-label {
  font-size: 13px; font-weight: 600; color: var(--zh-text-secondary);
  display: inline-flex; align-items: center; gap: 6px;
}
.input-label-icon { color: var(--zh-text-tertiary); }
.input-field {
  display: flex; align-items: center; height: 44px;
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
.input-prefix { color: var(--zh-text-tertiary); margin-left: 12px; flex-shrink: 0; transition: color var(--zh-transition-fast); }
.input-field.focused .input-prefix { color: var(--zh-primary); }
.input-control {
  flex: 1; height: 100%; border: none; outline: none;
  background: transparent; padding: 0 10px;
  font-size: 14px; color: var(--zh-text); font-family: inherit;
}
.input-control::placeholder { color: var(--zh-text-placeholder); font-size: 13px; }
.input-suffix {
  display: flex; align-items: center; justify-content: center;
  width: 40px; height: 100%; background: none; border: none;
  color: var(--zh-text-tertiary); cursor: pointer;
  transition: color var(--zh-transition-fast); flex-shrink: 0;
}
.input-suffix:hover { color: var(--zh-text-secondary); }
.auth-btn {
  width: 100%; height: 44px; border: none; border-radius: var(--zh-radius-md);
  background: var(--zh-primary-gradient); color: #fff;
  font-size: 15px; font-weight: 600; letter-spacing: 2px;
  cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  transition: opacity var(--zh-transition-fast), transform var(--zh-transition-fast);
  box-shadow: 0 4px 14px rgba(var(--zh-primary-rgb), 0.25);
  margin-top: 4px;
}
.auth-btn:hover:not(:disabled) { opacity: 0.92; transform: translateY(-1px); }
.auth-btn:active:not(:disabled) { transform: scale(0.98); }
.auth-btn:disabled { opacity: 0.6; cursor: not-allowed; }
.auth-link { font-size: 13px; color: var(--zh-primary); text-decoration: none; transition: opacity var(--zh-transition-fast); }
.auth-link:hover { opacity: 0.75; }
.auth-footer {
  text-align: center; font-size: 13px; color: var(--zh-text-tertiary);
  margin-top: 20px; padding-top: 16px;
  border-top: 1px solid var(--zh-border-light);
}
.captcha-img {
  width: 120px; height: 44px; flex-shrink: 0;
  border-radius: var(--zh-radius-md); overflow: hidden;
  border: 1.5px solid var(--zh-border);
  cursor: pointer; transition: border-color var(--zh-transition-fast);
}
.captcha-img:hover { border-color: var(--zh-primary); }
.code-btn {
  height: 44px; padding: 0 16px; flex-shrink: 0;
  border: 1.5px solid var(--zh-border);
  border-radius: var(--zh-radius-md);
  background: var(--zh-bg-elevated);
  color: var(--zh-primary);
  font-size: 13px; font-weight: 600;
  cursor: pointer;
  transition: all var(--zh-transition-fast);
  white-space: nowrap;
}
.code-btn:hover:not(:disabled) {
  border-color: var(--zh-primary);
  background: var(--zh-primary-bg);
}
.code-btn:disabled { opacity: 0.5; cursor: not-allowed; color: var(--zh-text-placeholder); }
</style>
