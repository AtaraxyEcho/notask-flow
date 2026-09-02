import dayjs from 'dayjs'
import customParseFormat from 'dayjs/plugin/customParseFormat'
import relativeTime from 'dayjs/plugin/relativeTime'
import type { Dayjs } from 'dayjs'

dayjs.extend(customParseFormat)
dayjs.extend(relativeTime)

export const DATE_TIME_FORMAT = 'YYYY-MM-DD:HH:mm:ss'
export const DATE_FORMAT = 'YYYY-MM-DD'

const ACCEPTED_DATE_TIME_FORMATS = [
  DATE_TIME_FORMAT,
  'YYYY-MM-DD:HH:mm',
  'YYYY-MM-DD HH:mm:ss',
  'YYYY-MM-DD HH:mm',
  'YYYY-MM-DDTHH:mm:ss',
  'YYYY-MM-DDTHH:mm',
]

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

export function formatShortDate(value?: string | null, fallback = '--') {
  return formatDateTime(value, fallback)
}

export function fromNow(value?: string | null, fallback = '--') {
  return formatDateTime(value, fallback)
}

export function toLocalInputDateTime(value?: string | null) {
  if (!value) {
    return ''
  }

  const target = parseDateTime(value)
  if (!target.isValid()) {
    return ''
  }

  return target.format(DATE_TIME_FORMAT)
}

export function fromLocalInputDateTime(value?: string | null) {
  if (!value) {
    return undefined
  }

  const target = parseDateTime(value)
  if (!target.isValid()) {
    return undefined
  }

  return target.format(DATE_TIME_FORMAT)
}

export function toTimestamp(value?: string | null) {
  if (!value) {
    return 0
  }

  const target = parseDateTime(value)
  return target.isValid() ? target.valueOf() : 0
}

function parseDateTime(value: string): Dayjs {
  const parsed = dayjs(value, ACCEPTED_DATE_TIME_FORMATS, true)
  if (parsed.isValid()) {
    return parsed
  }

  return dayjs(value)
}
