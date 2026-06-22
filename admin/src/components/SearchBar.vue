<template>
  <el-form :model="searchForm" inline class="search-bar">
    <el-form-item v-for="field in fields" :key="field.key" :label="field.label">
      <!-- 输入框 -->
      <el-input
        v-if="field.type === 'input'"
        v-model="searchForm[field.key]"
        :placeholder="field.placeholder || `请输入${field.label}`"
        clearable
        @keyup.enter="handleSearch"
      />
      <!-- 下拉选择 -->
      <el-select
        v-else-if="field.type === 'select'"
        v-model="searchForm[field.key]"
        :placeholder="field.placeholder || `全部${field.label}`"
        clearable
      >
        <el-option
          v-for="opt in field.options"
          :key="opt.value"
          :label="opt.label"
          :value="opt.value"
        />
      </el-select>
      <!-- 日期范围 -->
      <el-date-picker
        v-else-if="field.type === 'daterange'"
        v-model="searchForm[field.key]"
        type="daterange"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        value-format="YYYY-MM-DD"
      />
    </el-form-item>
    <el-form-item>
      <el-button type="primary" @click="handleSearch">搜索</el-button>
      <el-button @click="handleReset">重置</el-button>
    </el-form-item>
  </el-form>
</template>

<script setup lang="ts">
import { reactive, watch } from 'vue'

/** 搜索字段配置 */
export interface SearchField {
  /** 字段键名 */
  key: string
  /** 字段标签 */
  label: string
  /** 字段类型 */
  type: 'input' | 'select' | 'daterange'
  /** 占位文本 */
  placeholder?: string
  /** 下拉选项（select类型） */
  options?: { label: string; value: string | number }[]
}

const props = defineProps<{
  /** 搜索字段配置列表 */
  fields: SearchField[]
  /** 搜索参数初始值 */
  modelValue: Record<string, unknown>
}>()

const emit = defineEmits<{
  (e: 'search', params: Record<string, unknown>): void
  (e: 'reset'): void
  (e: 'update:modelValue', params: Record<string, unknown>): void
}>()

/** 搜索表单数据 */
const searchForm = reactive<Record<string, unknown>>({ ...props.modelValue })

/** 同步外部数据 */
watch(
  () => props.modelValue,
  (val) => Object.assign(searchForm, val),
  { deep: true }
)

/** 搜索 */
function handleSearch() {
  emit('update:modelValue', { ...searchForm })
  emit('search', { ...searchForm })
}

/** 重置 */
function handleReset() {
  for (const field of props.fields) {
    searchForm[field.key] = undefined
  }
  emit('update:modelValue', { ...searchForm })
  emit('reset')
}
</script>

<style scoped lang="scss">
.search-bar {
  margin-bottom: 16px;
}
</style>
