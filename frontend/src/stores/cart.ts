import { defineStore } from 'pinia'

export interface CartItem {
  productId: number
  name: string
  unit: string
  qty: number
}

const CART_KEY = 'customer_cart'
const GUEST_SHOP_KEY = 'guest_shop_name'

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
      const existing = this.items.find((item) => item.productId === product.id && item.unit === product.unit)
      if (existing) {
        existing.qty = Math.max(0, existing.qty + delta)
        if (existing.qty === 0) {
          this.items = this.items.filter((item) => !(item.productId === product.id && item.unit === product.unit))
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

    setQty(productId: number, qty: number, unit?: string) {
      const item = unit
        ? this.items.find((i) => i.productId === productId && i.unit === unit)
        : this.items.find((i) => i.productId === productId)
      if (!item) return
      if (qty <= 0) {
        this.items = this.items.filter((i) => !(i.productId === productId && i.unit === item.unit))
      } else {
        item.qty = qty
      }
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
