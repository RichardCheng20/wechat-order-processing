import { defineStore } from 'pinia'
import { deliveryDateString } from './salesOrder'

export interface PurchaseOrderSupplier {
  id?: number
  name: string
}

export interface PurchaseOrderLine {
  lineKey: string
  productId: number
  productName: string
  unit: string
  qty: number
  purchasePrice?: number
  /** 加入时采购任务的需采购参考量 */
  needQtyRef?: number
}

function lineKey(productId: number, unit: string) {
  return `${productId}-${unit}`
}

export const usePurchaseOrderStore = defineStore('purchaseOrder', {
  state: () => ({
    supplier: null as PurchaseOrderSupplier | null,
    receiveDay: 'today' as 'today' | 'tomorrow',
    items: [] as PurchaseOrderLine[],
    remark: '',
  }),

  getters: {
    hasSupplier: (state) => !!state.supplier?.name,
    supplierName: (state) => state.supplier?.name || '',
    receiveDate: (state) => deliveryDateString(state.receiveDay),
    totalKinds: (state) => state.items.length,
    totalAmount: (state) =>
      state.items.reduce((sum, line) => {
        const price = line.purchasePrice != null ? Number(line.purchasePrice) : 0
        return sum + price * line.qty
      }, 0),
  },

  actions: {
    setSupplier(supplier: PurchaseOrderSupplier) {
      this.supplier = {
        id: supplier.id,
        name: supplier.name.trim(),
      }
    },

    setReceiveDay(day: 'today' | 'tomorrow') {
      this.receiveDay = day
    },

    setRemark(remark: string) {
      this.remark = remark
    },

    upsertLine(line: Omit<PurchaseOrderLine, 'lineKey'> & { lineKey?: string }) {
      const key = line.lineKey || lineKey(line.productId, line.unit)
      const existing = this.items.find((i) => i.lineKey === key)
      if (existing) {
        existing.qty = line.qty
        existing.purchasePrice = line.purchasePrice
        existing.productName = line.productName
        existing.needQtyRef = line.needQtyRef
      } else {
        this.items.push({ ...line, lineKey: key })
      }
    },

    removeLine(lineKey: string) {
      this.items = this.items.filter((i) => i.lineKey !== lineKey)
    },

    importNeedItems(
      rows: {
        productId: number
        productName: string
        unit: string
        needQty: number
        purchasePrice?: number
      }[],
    ) {
      for (const row of rows) {
        if (row.needQty <= 0) continue
        this.upsertLine({
          productId: row.productId,
          productName: row.productName,
          unit: row.unit,
          qty: row.needQty,
          purchasePrice: row.purchasePrice,
          needQtyRef: row.needQty,
        })
      }
    },

    reset() {
      this.supplier = null
      this.receiveDay = 'today'
      this.items = []
      this.remark = ''
    },

    resetItems() {
      this.items = []
    },
  },
})

export function formatReceiveLabel(day: 'today' | 'tomorrow') {
  const base = new Date()
  if (day === 'tomorrow') {
    base.setDate(base.getDate() + 1)
  }
  const mm = String(base.getMonth() + 1).padStart(2, '0')
  const dd = String(base.getDate()).padStart(2, '0')
  return `${mm}-${dd}`
}
