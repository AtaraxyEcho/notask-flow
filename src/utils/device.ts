import { v4 as uuidv4 } from 'uuid';

const DEVICE_ID_KEY = 'notask-flow-device-id'

export function resolveDeviceId() {
  const existing = localStorage.getItem(DEVICE_ID_KEY)
  if (existing) {
    return existing
  }

  if (typeof crypto !== 'undefined' && crypto.randomUUID) {
        return crypto.randomUUID();
  }
  const nextId = uuidv4()
  localStorage.setItem(DEVICE_ID_KEY, nextId)
  return nextId
}

export function resolveDeviceName() {
  return navigator.userAgent || 'Browser'
}
