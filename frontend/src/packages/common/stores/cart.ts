import { defineStore } from 'pinia'

export interface CartItem {
  productId: number
  name: string
  unit: string
  qty: number
  remark?: string
  /** 档口暂无、需老板配货的自定义商品 */
  custom?: boolean
}

const CART_KEY = 'customer_cart'
const GUEST_SHOP_KEY = 'guest_shop_name'

function cartLineKey(item: Pick<CartItem, 'productId' | 'unit' | 'name' | 'custom'>) {
  if (item.custom) {
    return `custom:${item.name}:${item.unit}`
  }
  return `${item.productId}:${item.unit}`
}

function loadGuestShopName(): string {
  try {
    return (uni.getStorageSync(GUEST_SHOP_KEY) as string) || ''
  } catch {
    return ''
  }
}

function saveGuestShopName(name: string) {
  uni.setStorageSync(GUEST_SHOP_KEY, name)
}

function loadCart(): CartItem[] {
  try {
    const raw = uni.getStorageSync(CART_KEY)
    if (!raw) return []
    return JSON.parse(raw as string) as CartItem[]
  } catch {
    return []
  }
}

function saveCart(items: CartItem[]) {
  uni.setStorageSync(CART_KEY, JSON.stringify(items))
}

export const useCartStore = defineStore('cart', {
  state: () => ({
    items: loadCart() as CartItem[],
    guestShopName: loadGuestShopName(),
  }),

  getters: {
    totalQty: (state) => state.items.reduce((sum, item) => sum + item.qty, 0),
    totalKinds: (state) => state.items.length,
  },

  actions: {
    addProduct(product: { id: number; name: string; unit: string }, delta = 1) {
      const existing = this.items.find(
        (item) => !item.custom && item.productId === product.id && item.unit === product.unit,
      )
      if (existing) {
        existing.qty = Math.max(0, existing.qty + delta)
        if (existing.qty === 0) {
          this.items = this.items.filter((item) => cartLineKey(item) !== cartLineKey(existing))
        }
      } else if (delta > 0) {
        this.items.push({
          productId: product.id,
          name: product.name,
          unit: product.unit,
          qty: delta,
        })
      }
      saveCart(this.items)
    },

    addCustomLine(line: { name: string; unit: string; qty: number; remark?: string }) {
      const name = line.name.trim()
      if (!name) return
      const unit = line.unit.trim() || '斤'
      const existing = this.items.find(
        (item) => item.custom && item.name === name && item.unit === unit,
      )
      if (existing) {
        existing.qty = line.qty
        existing.remark = line.remark
      } else if (line.qty > 0) {
        this.items.push({
          productId: 0,
          name,
          unit,
          qty: line.qty,
          remark: line.remark,
          custom: true,
        })
      }
      this.items = this.items.filter((i) => i.qty > 0)
      saveCart(this.items)
    },

    setQty(productId: number, qty: number, unit?: string) {
      const item = unit
        ? this.items.find((i) => !i.custom && i.productId === productId && i.unit === unit)
        : this.items.find((i) => !i.custom && i.productId === productId)
      if (!item) return
      if (qty <= 0) {
        this.items = this.items.filter((i) => cartLineKey(i) !== cartLineKey(item))
      } else {
        item.qty = qty
      }
      saveCart(this.items)
    },

    upsertLine(line: { productId: number; name: string; unit: string; qty: number; remark?: string; custom?: boolean }) {
      const key = cartLineKey(line)
      const existing = this.items.find((i) => cartLineKey(i) === key)
      if (existing) {
        existing.qty = line.qty
        existing.name = line.name
        existing.remark = line.remark
      } else if (line.qty > 0) {
        this.items.push({
          productId: line.productId,
          name: line.name,
          unit: line.unit,
          qty: line.qty,
          remark: line.remark,
          custom: line.custom,
        })
      }
      this.items = this.items.filter((i) => i.qty > 0)
      saveCart(this.items)
    },

    removeLine(productId: number, unit: string, custom?: boolean, name?: string) {
      const key = custom
        ? `custom:${name || ''}:${unit}`
        : `${productId}:${unit}`
      this.items = this.items.filter((i) => cartLineKey(i) !== key)
      saveCart(this.items)
    },

    clear() {
      this.items = []
      saveCart(this.items)
    },

    setGuestShopName(name: string) {
      this.guestShopName = name.trim()
      saveGuestShopName(this.guestShopName)
    },
  },
})
