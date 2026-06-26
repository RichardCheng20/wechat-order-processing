/** 常用下单单位（与行业批发场景一致） */
export const PRESET_UNITS = [
  '斤', '公斤', '千克', 'kg', 'KG', '克', 'g', '吨', '两',
  '桶', '包', '瓶', '个', '张', '件', '箱', '只', '条',
  '盒', '双', '把', '根', '板', '袋', '份', '筐', '捆',
  '提', '扎', '支', '听', '头', '杯', '盆', '碗', '束',
  '套', '块', '打', '对', '朵', '副', '罐', '棵', '颗',
  '本', '串', '锅', '节', '卷', '枚', '片', '筒', '组',
  '次', '付', '排', '台', '小包', '叠', '底', '顶', '桌',
  '枝', '连', '米', '厘米', '千米', '毫升', '升', 'L', 'ML',
] as const

/** 商品建档/上架常用售卖单位 */
export const COMMON_SALE_UNITS = ['斤', '扎', '包', '个', '块', '袋', '箱', '把', '份', '两', '根', '串'] as const


export function mergeUnits(customUnits: string[]) {
  const seen = new Set<string>()
  const result: string[] = []
  for (const unit of [...customUnits, ...PRESET_UNITS]) {
    const trimmed = unit.trim()
    if (!trimmed || seen.has(trimmed)) continue
    seen.add(trimmed)
    result.push(trimmed)
  }
  return result
}

export function filterUnits(units: string[], keyword: string) {
  const kw = keyword.trim().toLowerCase()
  if (!kw) return units
  return units.filter((unit) => unit.toLowerCase().includes(kw))
}

export function parseSaleUnits(raw?: string[] | string, defaultUnit?: string): string[] {
  if (Array.isArray(raw) && raw.length > 0) {
    return [...new Set(raw.map((u) => u.trim()).filter(Boolean))]
  }
  if (typeof raw === 'string' && raw.trim()) {
    return [...new Set(raw.split(/[,，/、]/).map((u) => u.trim()).filter(Boolean))]
  }
  return defaultUnit ? [defaultUnit] : ['斤']
}

export function normalizeUnit(value: string) {
  return value.trim()
}
