import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { ApiResponse } from '@/types'

/**
 * 通用 CRUD 操作 composable
 * 封装管理端重复的增删改查模式
 */

interface CrudOptions<T, CreateParams, UpdateParams> {
  /** 列表加载函数 */
  loadList: (params: any) => Promise<void>
  /** 创建 API */
  createApi?: (data: CreateParams) => Promise<ApiResponse<T>>
  /** 更新 API */
  updateApi?: (data: UpdateParams) => Promise<ApiResponse<T>>
  /** 删除 API */
  deleteApi?: (id: number | string) => Promise<ApiResponse<any>>
  /** 缓存失效回调 */
  invalidateCache?: () => void
  /** 消息前缀（用于成功提示） */
  itemName?: string
}

export function useCrudOperations<T, CreateParams = any, UpdateParams = any>(options: CrudOptions<T, CreateParams, UpdateParams>) {
  const { loadList, createApi, updateApi, deleteApi, invalidateCache, itemName = '数据' } = options

  const saving = ref(false)

  /**
   * 通用提交（创建或更新）
   */
  const handleSubmit = async (
    form: Record<string, any>,
    isEdit: boolean,
    validate: () => Promise<boolean>,
    onSuccess?: () => void,
  ) => {
    if (!(await validate())) return
    saving.value = true
    try {
      if (isEdit && updateApi) {
        await updateApi(form as unknown as UpdateParams)
        ElMessage.success(`${itemName}更新成功`)
      } else if (createApi) {
        await createApi(form as unknown as CreateParams)
        ElMessage.success(`${itemName}创建成功`)
      }
      invalidateCache?.()
      loadList({})
      onSuccess?.()
    } catch (error: any) {
      const msg = error?.response?.data?.message || error?.message || '操作失败'
      ElMessage.error(msg)
    } finally {
      saving.value = false
    }
  }

  /**
   * 通用删除（带确认弹窗）
   */
  const handleDelete = async (id: number | string, name?: string) => {
    if (!deleteApi) return
    const label = name ? `"${name}"` : itemName
    try {
      await ElMessageBox.confirm(`确定要删除${label}吗？此操作不可恢复。`, '确认删除', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      })
      saving.value = true
      await deleteApi(id)
      ElMessage.success(`${itemName}删除成功`)
      invalidateCache?.()
      loadList({})
    } catch (error: any) {
      if (error === 'cancel' || error === 'close') return
      ElMessage.error(error?.response?.data?.message || `${itemName}删除失败`)
    } finally {
      saving.value = false
    }
  }

  /**
   * 通用确认操作（带确认弹窗）
   */
  const handleConfirmAction = async (
    action: () => Promise<any>,
    options: {
      confirmMessage: string
      successMessage: string
      errorMessage?: string
      title?: string
    },
  ) => {
    try {
      await ElMessageBox.confirm(options.confirmMessage, options.title || '确认操作', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      })
      saving.value = true
      await action()
      ElMessage.success(options.successMessage)
      invalidateCache?.()
      loadList({})
    } catch (error: any) {
      if (error === 'cancel' || error === 'close') return
      ElMessage.error(options.errorMessage || error?.response?.data?.message || '操作失败')
    } finally {
      saving.value = false
    }
  }

  return {
    saving,
    handleSubmit,
    handleDelete,
    handleConfirmAction,
  }
}
