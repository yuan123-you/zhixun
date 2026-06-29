<template>
  <el-container class="admin-layout">
    <!-- 侧边栏 -->
    <el-aside :width="sidebarCollapsed ? '64px' : '220px'" class="sidebar">
      <div class="logo">
        <img src="/vite.svg" alt="Logo" class="logo-img" />
        <span v-show="!sidebarCollapsed" class="logo-text">知讯管理后台</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        :collapse="sidebarCollapsed"
        :collapse-transition="false"
        router
        background-color="#001529"
        text-color="#ffffffa6"
        active-text-color="#ffffff"
        class="sidebar-menu"
      >
        <el-menu-item index="/dashboard">
          <el-icon><DataAnalysis /></el-icon>
          <template #title>数据概览</template>
        </el-menu-item>

        <el-sub-menu index="content">
          <template #title>
            <el-icon><Document /></el-icon>
            <span>内容管理</span>
          </template>
          <el-menu-item index="/articles">作品管理</el-menu-item>
          <el-menu-item index="/articles/pending">待审核作品</el-menu-item>
          <el-menu-item index="/categories">分类管理</el-menu-item>
          <el-menu-item index="/tags">标签管理</el-menu-item>
          <el-menu-item index="/topics">话题管理</el-menu-item>
          <el-menu-item index="/templates">模板管理</el-menu-item>
        </el-sub-menu>

        <el-menu-item index="/comments">
          <el-icon><ChatDotRound /></el-icon>
          <template #title>评论管理</template>
        </el-menu-item>

        <el-menu-item index="/reports">
          <el-icon><WarningFilled /></el-icon>
          <template #title>举报管理</template>
        </el-menu-item>

        <el-menu-item index="/users">
          <el-icon><User /></el-icon>
          <template #title>用户管理</template>
        </el-menu-item>

        <el-sub-menu index="system">
          <template #title>
            <el-icon><Setting /></el-icon>
            <span>系统管理</span>
          </template>
          <el-menu-item index="/sensitive-words">敏感词管理</el-menu-item>
          <el-menu-item index="/operation-logs">操作日志</el-menu-item>
          <el-menu-item index="/notifications">通知管理</el-menu-item>
          <el-menu-item index="/settings">系统设置</el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>

    <!-- 右侧主区域 -->
    <el-container class="main-container">
      <!-- 顶部导航栏 -->
      <el-header class="header">
        <div class="header-left">
          <!-- 折叠按钮 -->
          <el-icon class="collapse-btn" @click="toggleSidebar">
            <Fold v-if="!sidebarCollapsed" />
            <Expand v-else />
          </el-icon>
          <!-- 面包屑导航 -->
          <el-breadcrumb separator="/" class="breadcrumb">
            <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-if="currentRoute?.meta?.title">
              {{ currentRoute.meta.title }}
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <!-- 通知 -->
          <el-badge :value="3" class="notification-badge">
            <el-icon :size="20" class="header-icon" @click="router.push('/notifications')"><Bell /></el-icon>
          </el-badge>
          <!-- 用户菜单 -->
          <el-dropdown trigger="click" @command="handleUserCommand">
            <div class="user-info">
              <el-avatar :size="32" :src="userStore.userInfo?.avatar">
                {{ userStore.userInfo?.nickname?.charAt(0) || '管' }}
              </el-avatar>
              <span class="username">{{ userStore.userInfo?.nickname || '管理员' }}</span>
              <el-icon><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="password">修改密码</el-dropdown-item>
                <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 主内容区 -->
      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>

    <!-- 修改密码弹窗 -->
    <el-dialog
      v-model="passwordDialogVisible"
      title="修改密码"
      width="420px"
      :close-on-click-modal="false"
      @closed="resetPasswordForm"
    >
      <el-form
        ref="passwordFormRef"
        :model="passwordForm"
        :rules="passwordRules"
        label-width="90px"
      >
        <el-form-item label="原密码" prop="oldPassword">
          <el-input v-model="passwordForm.oldPassword" type="password" show-password placeholder="请输入原密码" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="passwordForm.newPassword" type="password" show-password placeholder="请输入新密码（至少6位）" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="passwordForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="passwordSaving" @click="handleChangePassword">确认修改</el-button>
      </template>
    </el-dialog>

    <!-- 返回顶部 -->
    <BackToTop />
  </el-container>
</template>

<script setup lang="ts">
import { computed, ref, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'
import { changePasswordApi } from '@/api/auth'

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()
const userStore = useUserStore()

/** 当前激活的菜单项 */
const activeMenu = computed(() => route.path)

/** 当前路由信息 */
const currentRoute = computed(() => route)

/** 侧边栏折叠状态 */
const sidebarCollapsed = computed(() => appStore.sidebarCollapsed)

/** 切换侧边栏折叠 */
function toggleSidebar() {
  appStore.toggleSidebar()
}

// ========== 修改密码 ==========

const passwordDialogVisible = ref(false)
const passwordFormRef = ref<FormInstance>()
const passwordSaving = ref(false)

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const passwordRules: FormRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (_rule: any, value: string, callback: Function) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
}

function resetPasswordForm() {
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
  passwordFormRef.value?.resetFields()
}

async function handleChangePassword() {
  const valid = await passwordFormRef.value?.validate().catch(() => false)
  if (!valid) return

  passwordSaving.value = true
  try {
    await changePasswordApi({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword,
    })
    ElMessage.success('密码修改成功，请重新登录')
    passwordDialogVisible.value = false
    resetPasswordForm()
    userStore.logout()
  } catch {
    // 错误已在拦截器中处理
  } finally {
    passwordSaving.value = false
  }
}

/** 用户菜单操作 */
function handleUserCommand(command: string) {
  switch (command) {
    case 'profile':
      router.push('/profile')
      break
    case 'password':
      resetPasswordForm()
      passwordDialogVisible.value = true
      break
    case 'logout':
      userStore.logout()
      break
  }
}
</script>

<style scoped lang="scss">
.admin-layout {
  height: 100vh;
  overflow: hidden;
}

/* 侧边栏 */
.sidebar {
  background-color: #001529;
  transition: width 0.3s;
  overflow: hidden;

  .logo {
    height: 60px;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 0 16px;
    border-bottom: 1px solid #ffffff1a;

    .logo-img {
      width: 32px;
      height: 32px;
    }

    .logo-text {
      color: #fff;
      font-size: 16px;
      font-weight: 600;
      margin-left: 10px;
      white-space: nowrap;
    }
  }

  .sidebar-menu {
    border-right: none;
    height: calc(100vh - 60px);
    overflow-y: auto;

    /* 滚动条样式 */
    &::-webkit-scrollbar {
      width: 6px;
    }
    &::-webkit-scrollbar-thumb {
      background: #ffffff33;
      border-radius: 3px;
    }
  }
}

/* 主容器 */
.main-container {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* 顶部导航栏 */
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
  padding: 0 20px;
  height: 60px;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);

  .header-left {
    display: flex;
    align-items: center;

    .collapse-btn {
      font-size: 20px;
      cursor: pointer;
      margin-right: 16px;
      color: #333;
      &:hover {
        color: var(--el-color-primary);
      }
    }

    .breadcrumb {
      line-height: 60px;
    }
  }

  .header-right {
    display: flex;
    align-items: center;
    gap: 20px;

    .header-icon {
      cursor: pointer;
      color: #666;
      &:hover {
        color: var(--el-color-primary);
      }
    }

    .notification-badge {
      line-height: 1;
    }

    .user-info {
      display: flex;
      align-items: center;
      cursor: pointer;
      gap: 8px;

      .username {
        font-size: 14px;
        color: #333;
      }
    }
  }
}

/* 主内容区 */
.main-content {
  background: #f5f5f5;
  overflow-y: auto;
  padding: 20px;
}
</style>
