<template>
  <div class="profile-page">
    <PageHeader title="个人中心" description="查看和修改个人资料" />

    <el-row :gutter="20">
      <!-- 左侧信息卡片 -->
      <el-col :xs="24" :md="8">
        <el-card shadow="hover" class="info-card">
          <div class="avatar-section">
            <el-avatar :size="80" :src="profile.avatar">
              {{ profile.nickname?.charAt(0) || '管' }}
            </el-avatar>
            <el-upload
              :show-file-list="false"
              :before-upload="beforeAvatarUpload"
              :http-request="handleAvatarUpload"
              accept="image/*"
              class="avatar-upload"
            >
              <el-button type="primary" link size="small">更换头像</el-button>
            </el-upload>
          </div>

          <el-descriptions :column="1" border class="info-desc">
            <el-descriptions-item label="用户名">{{ profile.username }}</el-descriptions-item>
            <el-descriptions-item label="UID">{{ profile.uid }}</el-descriptions-item>
            <el-descriptions-item label="角色">
              <el-tag :type="profile.role === 'SUPER_ADMIN' ? 'danger' : 'primary'" size="small">
                {{ roleLabel }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="注册时间">{{ profile.createdAt }}</el-descriptions-item>
            <el-descriptions-item label="IP属地">{{ profile.ipLocation || '-' }}</el-descriptions-item>
          </el-descriptions>

          <div class="stat-row">
            <div class="stat-item">
              <span class="stat-num">{{ profile.articleCount ?? 0 }}</span>
              <span class="stat-label">作品</span>
            </div>
            <div class="stat-item">
              <span class="stat-num">{{ profile.followCount ?? 0 }}</span>
              <span class="stat-label">关注</span>
            </div>
            <div class="stat-item">
              <span class="stat-num">{{ profile.followerCount ?? 0 }}</span>
              <span class="stat-label">粉丝</span>
            </div>
            <div class="stat-item">
              <span class="stat-num">{{ profile.totalLikeCount ?? 0 }}</span>
              <span class="stat-label">获赞</span>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 右侧编辑表单 -->
      <el-col :xs="24" :md="16">
        <el-card shadow="hover">
          <template #header>
            <span>编辑资料</span>
          </template>

          <el-form
            ref="formRef"
            :model="editForm"
            :rules="formRules"
            label-width="80px"
            style="max-width: 500px"
            v-loading="loading"
          >
            <el-form-item label="昵称" prop="nickname">
              <el-input v-model="editForm.nickname" placeholder="请输入昵称" maxlength="20" show-word-limit />
            </el-form-item>

            <el-form-item label="邮箱" prop="email">
              <el-input v-model="editForm.email" placeholder="请输入邮箱" />
            </el-form-item>

            <el-form-item label="手机号" prop="phone">
              <el-input v-model="editForm.phone" placeholder="请输入手机号" />
            </el-form-item>

            <el-form-item label="性别" prop="gender">
              <el-radio-group v-model="editForm.gender">
                <el-radio :value="0">未知</el-radio>
                <el-radio :value="1">男</el-radio>
                <el-radio :value="2">女</el-radio>
              </el-radio-group>
            </el-form-item>

            <el-form-item label="省份" prop="province">
              <el-input v-model="editForm.province" placeholder="请输入所在省份" maxlength="50" />
            </el-form-item>

            <el-form-item label="简介" prop="bio">
              <el-input
                v-model="editForm.bio"
                type="textarea"
                :rows="4"
                placeholder="介绍一下自己"
                maxlength="500"
                show-word-limit
              />
            </el-form-item>

            <el-form-item>
              <el-button type="primary" :loading="saving" @click="handleSave">保存修改</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules, UploadRequestOptions } from 'element-plus'
import { get, put } from '@/api/request'
import { uploadImage } from '@/api/file'
import { useUserStore } from '@/stores/user'
import PageHeader from '@/components/PageHeader.vue'

const userStore = useUserStore()

/** 个人资料 */
const profile = ref<any>({})
const loading = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()

const roleLabel = computed(() => {
  const map: Record<string, string> = {
    SUPER_ADMIN: '超级管理员',
    ADMIN: '管理员',
    USER: '普通用户',
  }
  return map[profile.value.role] || profile.value.role || '-'
})

/** 编辑表单 */
const editForm = reactive({
  nickname: '',
  email: '',
  phone: '',
  gender: 0,
  province: '',
  bio: '',
  avatar: '',
})

/** 表单校验 */
const formRules: FormRules = {
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
}

/** 头像上传前校验 */
function beforeAvatarUpload(file: File) {
  const isLt2M = file.size / 1024 / 1024 < 2
  if (!isLt2M) {
    ElMessage.error('头像大小不能超过 2MB')
    return false
  }
  return true
}

/** 头像上传 */
async function handleAvatarUpload(options: UploadRequestOptions) {
  try {
    const res = await uploadImage(options.file)
    if (res.code === 0 || res.code === 200) {
      editForm.avatar = res.data.url
      profile.value.avatar = res.data.url
      ElMessage.success('头像上传成功')
    }
  } catch {
    ElMessage.error('头像上传失败')
  }
}

/** 加载个人资料 */
async function loadProfile() {
  loading.value = true
  try {
    const res = await get<any>('/user/profile')
    const data = res.data
    profile.value = data
    editForm.nickname = data.nickname || ''
    editForm.email = data.email || ''
    editForm.phone = data.phone || ''
    editForm.gender = data.gender ?? 0
    editForm.province = data.province || ''
    editForm.bio = data.bio || ''
    editForm.avatar = data.avatar || ''
  } catch {
    ElMessage.error('加载个人资料失败')
  } finally {
    loading.value = false
  }
}

/** 保存修改 */
async function handleSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    await put('/user/profile', {
      nickname: editForm.nickname,
      email: editForm.email,
      phone: editForm.phone,
      gender: editForm.gender,
      province: editForm.province,
      bio: editForm.bio,
      avatar: editForm.avatar,
    } as any)
    ElMessage.success('保存成功')
    // 同步更新顶部导航的用户信息
    if (userStore.userInfo) {
      userStore.userInfo.nickname = editForm.nickname
      userStore.userInfo.avatar = editForm.avatar
    }
  } catch {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  loadProfile()
})
</script>

<style scoped lang="scss">
.profile-page {
  .info-card {
    .avatar-section {
      display: flex;
      flex-direction: column;
      align-items: center;
      margin-bottom: 20px;

      .avatar-upload {
        margin-top: 12px;
      }
    }

    .info-desc {
      margin-bottom: 20px;
    }

    .stat-row {
      display: flex;
      justify-content: space-around;
      padding: 16px 0;
      border-top: 1px solid #f0f0f0;

      .stat-item {
        display: flex;
        flex-direction: column;
        align-items: center;

        .stat-num {
          font-size: 20px;
          font-weight: 600;
          color: #333;
        }

        .stat-label {
          font-size: 12px;
          color: #999;
          margin-top: 4px;
        }
      }
    }
  }
}
</style>
