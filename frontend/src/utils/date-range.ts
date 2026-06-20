export function formatIsoDate(date: Date) {
  const y = date.getFullYear()
  const m = `${date.getMonth() + 1}`.padStart(2, '0')
  const d = `${date.getDate()}`.padStart(2, '0')
  return `${y}-${m}-${d}`
}

export function getLastNDaysRange(days: number) {
  const to = new Date()
  const from = new Date()
  from.setDate(from.getDate() - (days - 1))
  return { from: formatIsoDate(from), to: formatIsoDate(to) }
}

export function getTodayRange() {
  const today = formatIsoDate(new Date())
  return { from: today, to: today }
}

export function formatShortMonthDay(value: string) {
  const parts = value.split('-')
  if (parts.length < 3) return value
  return `${parts[1]}-${parts[2]}`
}

export function formatShortDate(value: string) {
  return formatShortMonthDay(value)
}

export function getThisMonthRange() {
  const now = new Date()
  const from = new Date(now.getFullYear(), now.getMonth(), 1)
  const to = new Date(now.getFullYear(), now.getMonth() + 1, 0)
  return { from: formatIsoDate(from), to: formatIsoDate(to) }
}

export function getLastMonthsRange(months: number) {
  const to = new Date()
  const from = new Date(to.getFullYear(), to.getMonth() - (months - 1), 1)
  return { from: formatIsoDate(from), to: formatIsoDate(to) }
}

export function isThisMonthRange(from: string, to: string) {
  const range = getThisMonthRange()
  return from === range.from && to === range.to
}

export function formatDotDate(value: string) {
  if (!value) return ''
  return value.replace(/-/g, '.')
}
