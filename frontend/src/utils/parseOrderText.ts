import { PRESET_UNITS, parseSaleUnits } from '../constants/units'
import type { ProductItem } from '../api/product'

export interface ParsedOrderLine {
  name: string
  qty: number
  unit: string
  price?: number
}

export interface ApplyParseResult {
  added: number
  unmatched: string[]
}

const UNIT_PATTERN = [...PRESET_UNITS]
  .sort((a, b) => b.length - a.length)
  .map((unit) => unit.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'))
  .join('|')

function splitSegments(text: string) {
  return text
    .split(/[\n,，、;；]+/)
    .map((segment) => segment.trim())
    .filter(Boolean)
}

function parseSegment(segment: string): ParsedOrderLine | null {
  let rest = segment.trim()
  if (!rest) return null

  let price: number | undefined
  const priceTail = rest.match(/(\d+(?:\.\d+)?)\s*元\s*$/)
  if (priceTail) {
    price = Number(priceTail[1])
    rest = rest.slice(0, -priceTail[0].length).trim()
  }

  const unitRe = new RegExp(`^(.+?)(\\d+(?:\\.\\d+)?)\\s*(${UNIT_PATTERN})\\s*$`)
  const unitMatch = rest.match(unitRe)
  if (unitMatch) {
    const name = unitMatch[1].trim()
    const qty = Number(unitMatch[2])
    const unit = unitMatch[3]
    if (!name || !qty || qty <= 0) return null
    return { name, qty, unit, price }
  }

  const defaultRe = /^(.+?)(\d+(?:\.\d+)?)\s*$/
  const defaultMatch = rest.match(defaultRe)
  if (defaultMatch) {
    const name = defaultMatch[1].trim()
    const qty = Number(defaultMatch[2])
    if (!name || !qty || qty <= 0) return null
    return { name, qty, unit: '斤', price }
  }

  return null
}

export function parseOrderText(text: string): ParsedOrderLine[] {
  const lines: ParsedOrderLine[] = []
  for (const segment of splitSegments(text)) {
    const parsed = parseSegment(segment)
    if (parsed) lines.push(parsed)
  }
  return lines
}

function aliasList(product: ProductItem) {
  if (!product.aliases) return [] as string[]
  return product.aliases.split(/[,，/、]/).map((item) => item.trim()).filter(Boolean)
}

export function matchOrderProduct(name: string, products: ProductItem[]) {
  const target = name.trim()
  if (!target) return null
  const onSale = products.filter((item) => item.saleStatus === 'ON')

  const exact = onSale.find((item) => item.name === target)
  if (exact) return exact

  const aliasExact = onSale.find((item) => aliasList(item).includes(target))
  if (aliasExact) return aliasExact

  const contains = onSale.find((item) => {
    if (item.name.includes(target) || target.includes(item.name)) return true
    return aliasList(item).some((alias) => alias.includes(target) || target.includes(alias))
  })
  return contains || null
}

export function applyParsedLines(
  lines: ParsedOrderLine[],
  products: ProductItem[],
  upsert: (line: {
    productId: number
    productName: string
    unit: string
    orderQty: number
    dealPrice?: number
  }) => void,
): ApplyParseResult {
  const unmatched: string[] = []
  let added = 0

  for (const line of lines) {
    const product = matchOrderProduct(line.name, products)
    if (!product) {
      unmatched.push(line.name)
      continue
    }
    const allowed = parseSaleUnits(product.saleUnits, product.unit)
    const unit = allowed.includes(line.unit) ? line.unit : (allowed[0] || product.unit)
    upsert({
      productId: product.id,
      productName: product.name,
      unit,
      orderQty: line.qty,
      dealPrice: line.price,
    })
    added += 1
  }

  return { added, unmatched }
}
