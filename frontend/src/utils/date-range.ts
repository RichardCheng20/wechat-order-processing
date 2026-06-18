export function formatDate(d: Date) {
  const y = d.getFullYear()
  const m = `${d.getMonth() + 1}`.padStart(2, '0')
  const day = `${d.getDate()}`.padStart(2, '0')
  return `${y}-${m}-${day}`
}

export function formatShortDate(value: string) {
  if (!value) return ''
  const parts = value.split('-')
  if (parts.length < 3) return value
  return `${parts[1]}-${parts[2]}`
}

export function getLastMonthsRange(months: number) {
  const to = new Date()
  const from = new Date()
  from.setMonth(from.getMonth() - months)
  return { from: formatDate(from), to: formatDate(to) }
}

export function getThisMonthRange() {
  const now = new Date()
  const from = new Date(now.getFullYear(), now.getMonth(), 1)
  const to = new Date(now.getFullYear(), now.getMonth() + 1, 0)
  return { from: formatDate(from), to: formatDate(to) }
}

export function isThisMonthRange(from: string, to: string) {
  const range = getThisMonthRange()
  return from === range.from && to === range.to
}
