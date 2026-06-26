import { defineStore } from 'pinia'

export interface SalesOrderLine {
  lineKey: string
  productId: number
  productName: string
  unit: string
  orderQty: number
  dealPrice?: number
  pickRemark?: string
}

export interface SalesOrderCustomer {
  id?: number
  name: string
  temporary?: boolean
}

function lineKey(productId: number, unit: string) {
  return `${productId}-${unit}`
}

export const useSalesOrderStore = defineStore('salesOrder', {
  state: () => ({
    editOrderId: null as number | null,
    customer: null as SalesOrderCustomer | null,
    items: [] as SalesOrderLine[],
    remark: '',
    deliveryDay: 'today' as 'today' | 'tomorrow',
  }),

  getters: {
    totalKinds: (state) => state.items.length,
    hasCustomer: (state) => !!state.customer?.name,
    customerDisplayName: (state) => state.customer?.name || '',
  },

  actions: {
    setCustomer(customer: SalesOrderCustomer) {
      this.customer = {
        id: customer.id,
        name: customer.name.trim(),
        temporary: customer.temporary ?? !customer.id,
      }
    },

    setTemporaryCustomer(name: string) {
      const trimmed = name.trim()
      if (!trimmed) return
      this.customer = { name: trimmed, temporary: true }
    },

    clearCustomer() {
      this.customer = null
    },

    setDeliveryDay(day: 'today' | 'tomorrow') {
      this.deliveryDay = day
    },

    setRemark(remark: string) {
      this.remark = remark
    },

    upsertLine(line: Omit<SalesOrderLine, 'lineKey'> & { lineKey?: string }) {
      const key = line.lineKey || lineKey(line.productId, line.unit)
      const existing = this.items.find((i) => i.lineKey === key)
      if (existing) {
        existing.orderQty = line.orderQty
        existing.dealPrice = line.dealPrice
        existing.pickRemark = line.pickRemark
        existing.productName = line.productName
      } else {
        this.items.push({ ...line, lineKey: key })
      }
    },

    removeLine(lineKey: string) {
      this.items = this.items.filter((i) => i.lineKey !== lineKey)
    },

    setLinePrice(lineKey: string, dealPrice?: number) {
      const line = this.items.find((i) => i.lineKey === lineKey)
      if (!line) return
      line.dealPrice = dealPrice
    },

    setLineQty(lineKey: string, orderQty: number) {
      const line = this.items.find((i) => i.lineKey === lineKey)
      if (!line) return
      line.orderQty = orderQty
    },

    updateLineUnit(oldLineKey: string, newUnit: string): boolean {
      const idx = this.items.findIndex((i) => i.lineKey === oldLineKey)
      if (idx === -1) return false
      const line = this.items[idx]
      const unit = newUnit.trim()
      if (!unit || unit === line.unit) return true
      const newKey = lineKey(line.productId, unit)
      if (this.items.some((i) => i.lineKey === newKey)) {
        return false
      }
      this.items[idx] = { ...line, unit, lineKey: newKey }
      return true
    },

    reset() {
      this.editOrderId = null
      this.customer = null
      this.items = []
      this.remark = ''
      this.deliveryDay = 'today'
    },

    loadFromOrder(order: {
      id: number
      customerId?: number
      customerName?: string
      contactName?: string
      remark?: string
      deliveryDate?: string
      items?: {
        productId: number
        productName?: string
        orderQty: number
        unit: string
        dealPrice?: number
        pickRemark?: string
      }[]
    }) {
      this.editOrderId = order.id
      const name = (order.customerName || order.contactName || '').trim()
      if (order.customerId) {
        this.setCustomer({
          id: order.customerId,
          name,
          temporary: false,
        })
      } else if (name) {
        this.setTemporaryCustomer(name)
      } else {
        this.customer = null
      }
      this.remark = order.remark || ''
      if (order.deliveryDate) {
        const tomorrow = deliveryDateString('tomorrow')
        this.deliveryDay = order.deliveryDate === tomorrow ? 'tomorrow' : 'today'
      } else {
        this.deliveryDay = 'today'
      }
      this.items = (order.items || []).map((item) => ({
        lineKey: lineKey(item.productId, item.unit),
        productId: item.productId,
        productName: item.productName || '',
        unit: item.unit,
        orderQty: Number(item.orderQty),
        pickRemark: item.pickRemark,
      }))
    },
  },
})

export function formatDeliveryLabel(day: 'today' | 'tomorrow') {
  const base = new Date()
  if (day === 'tomorrow') {
    base.setDate(base.getDate() + 1)
  }
  const mm = String(base.getMonth() + 1).padStart(2, '0')
  const dd = String(base.getDate()).padStart(2, '0')
  return `${mm}-${dd} 22:30`
}

export function deliveryDateString(day: 'today' | 'tomorrow') {
  const base = new Date()
  if (day === 'tomorrow') {
    base.setDate(base.getDate() + 1)
  }
  const y = base.getFullYear()
  const mm = String(base.getMonth() + 1).padStart(2, '0')
  const dd = String(base.getDate()).padStart(2, '0')
  return `${y}-${mm}-${dd}`
}
