import { ElMessage } from 'element-plus'

export type ToastTone = 'ok' | 'info' | 'danger'
export interface ToastItem { id: number; msg: string; tone: ToastTone }

export const toasts: ToastItem[] = []

export function pushToast(msg: string, tone: ToastTone = 'ok') {
  ElMessage({
    message: msg,
    type: tone === 'ok' ? 'success' : tone === 'danger' ? 'error' : 'info',
    duration: 2600,
    showClose: true,
  })
}

/** 与原型 useToast() 等价的调用入口 */
export function useToast() {
  return pushToast
}
