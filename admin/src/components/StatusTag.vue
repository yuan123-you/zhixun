<template>
  <el-tag :type="tagType" size="small">
    {{ label }}
  </el-tag>
</template>

<script setup lang="ts">
import { computed } from 'vue'

/** 状态标签组件，根据状态值自动显示对应标签样式 */

const props = defineProps<{
  /** 状态值（支持字符串或数字） */
  status: string | number
}>()

/** 状态配置映射（字符串 + 数字键） */
const statusConfig: Record<string, { label: string; type: 'success' | 'warning' | 'danger' | 'info' }> = {
  // 字符串状态（前端枚举值）
  draft: { label: '草稿', type: 'info' },
  pending: { label: '待审核', type: 'warning' },
  published: { label: '已发布', type: 'success' },
  offline: { label: '已下架', type: 'danger' },
  rejected: { label: '已驳回', type: 'danger' },
  active: { label: '正常', type: 'success' },
  banned: { label: '封禁', type: 'danger' },
  deleted: { label: '已删除', type: 'info' },
  // 数字状态（后端 ArticleStatusEnum: 0=草稿,1=待审核,2=已发布,3=已拒绝,4=已下线）
  '0': { label: '草稿', type: 'info' },
  '1': { label: '待审核', type: 'warning' },
  '2': { label: '已发布', type: 'success' },
  '3': { label: '已驳回', type: 'danger' },
  '4': { label: '已下架', type: 'danger' },
}

/** 状态键（统一转为字符串查找） */
const statusKey = computed(() => String(props.status))

/** 标签类型 */
const tagType = computed<'success' | 'warning' | 'danger' | 'info'>(() => statusConfig[statusKey.value]?.type || 'info')

/** 标签文本 */
const label = computed(() => statusConfig[statusKey.value]?.label || statusKey.value)
</script>
