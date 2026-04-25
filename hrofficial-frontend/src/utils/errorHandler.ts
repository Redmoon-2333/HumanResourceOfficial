import { ElMessage } from 'element-plus'
import { ErrorCodeMap, isTokenErrorCode, isAuthErrorCode, type ErrorLevel } from '@/types/errorCodeMap'

export function getErrorMessage(code: number, defaultMessage?: string): string {
  const entry = ErrorCodeMap[code]
  if (entry) {
    return entry.message
  }
  return defaultMessage || '未知错误'
}

export function getErrorLevel(code: number): ErrorLevel {
  const entry = ErrorCodeMap[code]
  return entry?.level || 'error'
}

export function getErrorType(code: number): string {
  const entry = ErrorCodeMap[code]
  return entry?.category || 'general'
}

export function shouldLogout(code: number): boolean {
  return isTokenErrorCode(code)
}

export function shouldRedirectToLogin(code: number): boolean {
  return isAuthErrorCode(code)
}

export function handleApiError(code: number, message?: string): void {
  const errorMessage = message || getErrorMessage(code)
  const level = getErrorLevel(code)

  switch (level) {
    case 'error':
      ElMessage.error(errorMessage)
      break
    case 'warning':
      ElMessage.warning(errorMessage)
      break
    case 'info':
      ElMessage.info(errorMessage)
      break
  }
}

export function handleApiSuccess(message: string = '操作成功'): void {
  ElMessage.success(message)
}

export function createErrorHandler() {
  return {
    getErrorMessage,
    getErrorLevel,
    getErrorType,
    shouldLogout,
    shouldRedirectToLogin,
    handleApiError,
    handleApiSuccess
  }
}

export default {
  getErrorMessage,
  getErrorLevel,
  getErrorType,
  shouldLogout,
  shouldRedirectToLogin,
  handleApiError,
  handleApiSuccess,
  createErrorHandler
}
