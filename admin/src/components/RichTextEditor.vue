<template>
  <div class="rich-text-editor">
    <div ref="editorRef" class="editor-content" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'

/**
 * 富文本编辑器组件
 * 基于原生 contenteditable 实现，可替换为 WangEditor / TinyMCE 等
 */

const props = withDefaults(
  defineProps<{
    /** 编辑器内容（v-model） */
    modelValue?: string
    /** 占位文本 */
    placeholder?: string
  }>(),
  {
    modelValue: '',
    placeholder: '请输入内容...',
  }
)

const emit = defineEmits<{
  (e: 'update:modelValue', val: string): void
}>()

const editorRef = ref<HTMLElement | null>(null)

/** 监听外部数据变化 */
watch(
  () => props.modelValue,
  (val) => {
    if (editorRef.value && editorRef.value.innerHTML !== val) {
      editorRef.value.innerHTML = val
    }
  }
)

/** 初始化编辑器 */
onMounted(() => {
  if (editorRef.value) {
    editorRef.value.innerHTML = props.modelValue
    editorRef.value.setAttribute('data-placeholder', props.placeholder)

    // 监听输入事件
    editorRef.value.addEventListener('input', handleInput)
  }
})

/** 清理事件监听 */
onBeforeUnmount(() => {
  if (editorRef.value) {
    editorRef.value.removeEventListener('input', handleInput)
  }
})

/** 输入事件处理 */
function handleInput() {
  if (editorRef.value) {
    emit('update:modelValue', editorRef.value.innerHTML)
  }
}
</script>

<style scoped lang="scss">
.rich-text-editor {
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  overflow: hidden;

  .editor-content {
    min-height: 300px;
    padding: 12px;
    outline: none;
    font-size: 14px;
    line-height: 1.6;
    overflow-y: auto;

    /* 空内容占位符 */
    &:empty::before {
      content: attr(data-placeholder);
      color: #c0c4cc;
    }
  }
}
</style>
