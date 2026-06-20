import { getApiBaseUrl } from './config'

export function resolveMediaUrl(url?: string | null) {
  if (!url) return ''
  if (url.startsWith('http://') || url.startsWith('https://')) return url
  const base = getApiBaseUrl()
  if (url.startsWith('/')) return `${base}${url}`
  return `${base}/${url}`
}
