<template>
  <el-tag :type="tagType" size="small">
    {{ label }}
  </el-tag>
</template>

<script setup lang="ts">
import { computed } from 'vue'

/** 状态标签组件，根据状态值自动显示对应标签样式 */

const props = defineProps<{
  /** 状态值 */
  status: string
}>()

/** 状态配置映射 */
const statusConfig: Record<string, { label: string; type: string }> = {
  draft: { label: '草稿', type: 'info' },
  pending: { label: '待审核', type: 'warning' },
  published: { label: '已发布', type: 'success' },
  offline: { label: '已下架', type: 'danger' },
  rejected: { label: '已驳回', type: 'danger' },
  active: { label: '正常', type: 'success' },
  banned: { label: '封禁', type: 'danger' },
  deleted: { label: '已删除', type: 'info' },
}

/** 标签类型 */
const tagType = computed(() => statusConfig[props.status]?.type || 'info')

/** 标签文本 */
const label = computed(() => statusConfig[props.status]?.label || props.status)
</script>
