import { ref, type Ref } from 'vue'
import { ElMessage } from 'element-plus'

interface AsyncOptions {
  loadingMessage?: string
  successMessage?: string
  errorMessage?: string
  showError?: boolean
}

/**
 * 异步操作封装
 * 统一处理loading状态、错误处理、消息提示
 */
export function useAsync<T>() {
  const loading = ref(false)
  const error: Ref<Error | null> = ref(null)
  const data: Ref<T | null> = ref(null)

  const execute = async (
    asyncFn: () => Promise<T>,
    options: AsyncOptions = {}
  ): Promise<T | null> => {
    const {
      successMessage,
      errorMessage = '操作失败',
      showError = true
    } = options

    loading.value = true
    error.value = null

    try {
      const result = await asyncFn()
      data.value = result

      if (successMessage) {
        ElMessage.success(successMessage)
      }

      return result
    } catch (err: any) {
      error.value = err
      if (showError) {
        ElMessage.error(err.message || errorMessage)
      }
      return null
    } finally {
      loading.value = false
    }
  }

  const reset = () => {
    loading.value = false
    error.value = null
    data.value = null
  }

  return {
    loading,
    error,
    data,
    execute,
    reset
  }
}
