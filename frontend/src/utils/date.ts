import dayjs from 'dayjs'
import customParseFormat from 'dayjs/plugin/customParseFormat'
import type { Dayjs } from 'dayjs'

dayjs.extend(customParseFormat)

/** 界面展示用日期时间格式（不含秒，符合中文与 ISO 阅读习惯）。 */
export const DATE_TIME_FORMAT = 'YYYY-MM-DD HH:mm'

/** 界面展示用完整日期时间格式。 */
export const DATE_TIME_SECONDS_FORMAT = 'YYYY-MM-DD HH:mm:ss'

/** 界面展示用简短日期时间格式。 */
export const SHORT_DATE_TIME_FORMAT = 'MM-DD HH:mm'

/** 界面展示用日期格式。 */
export const DATE_FORMAT = 'YYYY-MM-DD'

/**
 * 与服务端约定的传输格式。
 *
 * 后端 Jackson 统一配置为 `yyyy-MM-dd:HH:mm:ss`，界面展示与传输格式在此解耦。
 */
export const TRANSPORT_DATE_TIME_FORMAT = 'YYYY-MM-DD:HH:mm:ss'

const ACCEPTED_DATE_TIME_FORMATS = [
  DATE_TIME_FORMAT,
  DATE_TIME_SECONDS_FORMAT,
  TRANSPORT_DATE_TIME_FORMAT,
  'YYYY-MM-DD:HH:mm',
  'YYYY-MM-DD HH:mm:ss',
  'YYYY-MM-DD HH:mm',
  'YYYY-MM-DDTHH:mm:ss',
  'YYYY-MM-DDTHH:mm',
]

const LOCALE_STORAGE_KEY = 'notask-flow-locale'

/**
 * 解析当前界面语言。
 *
 * @returns BCP 47 语言标签
 */
function resolveLocale(): string {
  return window.localStorage.getItem(LOCALE_STORAGE_KEY) === 'en-US' ? 'en-US' : 'zh-CN'
}

/**
 * 格式化完整日期时间。
 *
 * @param value 原始日期时间
 * @param fallback 兜底文案
 * @returns 格式化结果
 */
export function formatDateTime(value?: string | null, fallback = '--') {
  if (!value) {
    return fallback
  }

  const target = parseDateTime(value)
  if (!target.isValid()) {
    return fallback
  }

  return target.format(DATE_TIME_FORMAT)
}

/**
 * 格式化日期。
 *
 * @param value 原始日期
 * @param fallback 兜底文案
 * @returns 格式化结果
 */
export function formatDate(value?: string | null, fallback = '--') {
  if (!value) {
    return fallback
  }

  const target = dayjs(value)
  if (!target.isValid()) {
    return fallback
  }

  return target.format(DATE_FORMAT)
}

/**
 * 格式化简短日期时间（用于列表卡片）。
 *
 * @param value 原始日期时间
 * @param fallback 兜底文案
 * @returns 格式化结果
 */
export function formatShortDate(value?: string | null, fallback = '--') {
  if (!value) {
    return fallback
  }

  const target = parseDateTime(value)
  if (!target.isValid()) {
    return fallback
  }

  return target.format(SHORT_DATE_TIME_FORMAT)
}

/**
 * 格式化为相对时间（如「3 分钟前」）。
 *
 * @param value 原始日期时间
 * @param fallback 兜底文案
 * @returns 相对时间描述
 */
export function fromNow(value?: string | null, fallback = '--') {
  if (!value) {
    return fallback
  }

  const target = parseDateTime(value)
  if (!target.isValid()) {
    return fallback
  }

  const diffSeconds = Math.round((target.valueOf() - Date.now()) / 1000)
  const absoluteSeconds = Math.abs(diffSeconds)
  const formatter = new Intl.RelativeTimeFormat(resolveLocale(), { numeric: 'auto' })

  if (absoluteSeconds < 60) {
    return formatter.format(0, 'second')
  }
  if (absoluteSeconds < 3600) {
    return formatter.format(Math.round(diffSeconds / 60), 'minute')
  }
  if (absoluteSeconds < 86400) {
    return formatter.format(Math.round(diffSeconds / 3600), 'hour')
  }
  if (absoluteSeconds < 2592000) {
    return formatter.format(Math.round(diffSeconds / 86400), 'day')
  }
  if (absoluteSeconds < 31536000) {
    return formatter.format(Math.round(diffSeconds / 2592000), 'month')
  }
  return formatter.format(Math.round(diffSeconds / 31536000), 'year')
}

/**
 * 将服务端时间转换为日期选择器可用的传输格式。
 *
 * @param value 服务端时间
 * @returns 传输格式时间
 */
export function toLocalInputDateTime(value?: string | null) {
  if (!value) {
    return ''
  }

  const target = parseDateTime(value)
  if (!target.isValid()) {
    return ''
  }

  return target.format(TRANSPORT_DATE_TIME_FORMAT)
}

/**
 * 将日期选择器的值转换为服务端传输格式。
 *
 * @param value 选择器值
 * @returns 传输格式时间
 */
export function fromLocalInputDateTime(value?: string | null) {
  if (!value) {
    return undefined
  }

  const target = parseDateTime(value)
  if (!target.isValid()) {
    return undefined
  }

  return target.format(TRANSPORT_DATE_TIME_FORMAT)
}

/**
 * 将时间转换为毫秒时间戳。
 *
 * @param value 原始时间
 * @returns 毫秒时间戳，非法时返回 0
 */
export function toTimestamp(value?: string | null) {
  if (!value) {
    return 0
  }

  const target = parseDateTime(value)
  return target.isValid() ? target.valueOf() : 0
}

/**
 * 解析日期时间字符串。
 *
 * @param value 原始值
 * @returns dayjs 实例
 */
function parseDateTime(value: string): Dayjs {
  const parsed = dayjs(value, ACCEPTED_DATE_TIME_FORMATS, true)
  if (parsed.isValid()) {
    return parsed
  }

  return dayjs(value)
}
