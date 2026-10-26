import { v4 as uuidv4 } from 'uuid'

const DEVICE_ID_KEY = 'notask-flow-device-id'

/**
 * 解析当前浏览器设备标识。
 *
 * 首次调用生成 UUID 并持久化到 localStorage，后续登录复用同一设备标识，
 * 以支撑单设备登录复用与设备管理能力。
 *
 * @returns 设备标识
 */
export function resolveDeviceId() {
  const existing = localStorage.getItem(DEVICE_ID_KEY)
  if (existing) {
    return existing
  }

  const generated = typeof crypto !== 'undefined' && crypto.randomUUID
    ? crypto.randomUUID()
    : uuidv4()
  localStorage.setItem(DEVICE_ID_KEY, generated)
  return generated
}

/**
 * 解析当前浏览器设备名称。
 *
 * @returns 设备名称
 */
export function resolveDeviceName() {
  return navigator.userAgent || 'Browser'
}
