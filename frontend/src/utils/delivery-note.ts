import type { OrderInfo, OrderLineItem } from '../api/order'

export interface DeliveryNoteRow {
  index: number
  productName: string
  unit: string
  quantity: number
  unitPrice: number | null
  subtotal: number | null
  remark: string
}

export interface DeliveryNoteData {
  customerName: string
  deliveryDate: string
  rows: DeliveryNoteRow[]
  totalAmount: number | null
  priceIncomplete: boolean
}

function lineQty(item: OrderLineItem) {
  if (item.shortageFlag === 1) {
    return item.actualQty ?? 0
  }
  return item.actualQty ?? item.orderQty
}

function lineSubtotal(item: OrderLineItem): number | null {
  if (item.subtotalAmount != null) {
    return Number(item.subtotalAmount)
  }
  if (item.dealPrice == null) {
    return null
  }
  const qty = lineQty(item)
  return Number((qty * item.dealPrice).toFixed(2))
}

export function buildDeliveryNote(order: OrderInfo): DeliveryNoteData {
  const items = order.items || []
  const rows = items.map((item, idx) => ({
    index: idx + 1,
    productName: item.productName || '未知商品',
    unit: item.unit || '斤',
    quantity: Number(lineQty(item)),
    unitPrice: item.dealPrice != null ? Number(item.dealPrice) : null,
    subtotal: lineSubtotal(item),
    remark: item.pickRemark?.trim() || '',
  }))

  const totalAmount = order.amount != null ? Number(order.amount) : null

  const priceIncomplete = order.priceIncomplete
    ?? items.some((item) => item.dealPrice == null)

  return {
    customerName: order.customerName || '未知客户',
    deliveryDate: order.deliveryDate || formatToday(),
    rows,
    totalAmount,
    priceIncomplete,
  }
}

function formatToday() {
  const d = new Date()
  const y = d.getFullYear()
  const m = `${d.getMonth() + 1}`.padStart(2, '0')
  const day = `${d.getDate()}`.padStart(2, '0')
  return `${y}-${m}-${day}`
}

export function formatMoney(value: number) {
  return Number(value || 0).toFixed(2)
}

export function formatOptionalMoney(value: number | null | undefined) {
  if (value == null) return ''
  return formatMoney(value)
}

export function formatQuantity(value: number) {
  return Number(value || 0).toFixed(2)
}

const CANVAS_WIDTH = 750
const PADDING = 24
const ROW_HEIGHT = 44
const HEADER_HEIGHT = 170
const FOOTER_HEIGHT = 80

export function calcCanvasHeight(rowCount: number) {
  return HEADER_HEIGHT + 40 + rowCount * ROW_HEIGHT + FOOTER_HEIGHT
}

export function drawDeliveryNoteCanvas(
  ctx: UniApp.CanvasContext,
  note: DeliveryNoteData,
  canvasHeight: number,
) {
  ctx.setFillStyle('#ffffff')
  ctx.fillRect(0, 0, CANVAS_WIDTH, canvasHeight)

  ctx.setFillStyle('#111111')
  ctx.setTextAlign('center')
  ctx.setFontSize(28)
  ctx.fillText('配送单', CANVAS_WIDTH / 2, 48)

  ctx.setTextAlign('left')
  ctx.setFontSize(20)
  ctx.fillText(`收货商户：${note.customerName}`, PADDING, 92)
  ctx.setTextAlign('right')
  ctx.fillText(`送货时间：${note.deliveryDate}`, CANVAS_WIDTH - PADDING, 92)

  const tableTop = 120
  const colX = [PADDING, 88, 250, 330, 430, 530, 620]
  const headers = ['序号', '商品名', '单位', '数量', '单价', '小计', '备注']

  ctx.setStrokeStyle('#333333')
  ctx.setLineWidth(1)
  ctx.strokeRect(PADDING, tableTop, CANVAS_WIDTH - PADDING * 2, 36)

  ctx.setFillStyle('#111111')
  ctx.setFontSize(18)
  ctx.setTextAlign('center')
  headers.forEach((text, idx) => {
    const nextX = colX[idx + 1] ?? CANVAS_WIDTH - PADDING
    const width = nextX - colX[idx]
    ctx.fillText(text, colX[idx] + width / 2, tableTop + 24)
    if (idx > 0) {
      ctx.beginPath()
      ctx.moveTo(colX[idx], tableTop)
      ctx.lineTo(colX[idx], tableTop + 36)
      ctx.stroke()
    }
  })

  let y = tableTop + 36
  note.rows.forEach((row) => {
    ctx.strokeRect(PADDING, y, CANVAS_WIDTH - PADDING * 2, ROW_HEIGHT)
    const values = [
      String(row.index),
      row.productName,
      row.unit,
      formatQuantity(row.quantity),
      formatOptionalMoney(row.unitPrice),
      formatOptionalMoney(row.subtotal),
      row.remark,
    ]
    values.forEach((text, idx) => {
      const nextX = colX[idx + 1] ?? CANVAS_WIDTH - PADDING
      const width = nextX - colX[idx]
      ctx.setTextAlign(idx === 1 || idx === 6 ? 'left' : 'center')
      const tx = idx === 1 || idx === 6 ? colX[idx] + 8 : colX[idx] + width / 2
      const display = text.length > 8 && idx === 1 ? `${text.slice(0, 8)}…` : text
      ctx.fillText(display, tx, y + 28)
      if (idx > 0) {
        ctx.beginPath()
        ctx.moveTo(colX[idx], y)
        ctx.lineTo(colX[idx], y + ROW_HEIGHT)
        ctx.stroke()
      }
    })
    y += ROW_HEIGHT
  })

  y += 24
  ctx.setTextAlign('left')
  ctx.setFontSize(20)
  const totalText = note.totalAmount != null ? formatMoney(note.totalAmount) : ''
  ctx.fillText(`合计金额：${totalText}`, PADDING, y)

  ctx.setTextAlign('center')
  ctx.setFontSize(18)
  ctx.setFillStyle('#666666')
  ctx.fillText('页码：1 / 1', CANVAS_WIDTH / 2, canvasHeight - 24)
}

export const DELIVERY_NOTE_CANVAS = {
  width: CANVAS_WIDTH,
  calcHeight: calcCanvasHeight,
}

export function exportDeliveryNoteImage(note: DeliveryNoteData): Promise<string> {
  const canvasId = 'deliveryNoteCanvas'
  const height = calcCanvasHeight(note.rows.length)
  const ctx = uni.createCanvasContext(canvasId)
  drawDeliveryNoteCanvas(ctx, note, height)

  return new Promise((resolve, reject) => {
    ctx.draw(false, () => {
      setTimeout(() => {
        uni.canvasToTempFilePath({
          canvasId,
          width: CANVAS_WIDTH,
          height,
          destWidth: CANVAS_WIDTH * 2,
          destHeight: height * 2,
          fileType: 'png',
          success: (res) => resolve(res.tempFilePath),
          fail: (err) => reject(new Error(err.errMsg || '生成图片失败')),
        })
      }, 300)
    })
  })
}

export function previewDeliveryNoteImage(filePath: string) {
  uni.previewImage({
    urls: [filePath],
    current: filePath,
  })
}

export function saveDeliveryNoteImage(filePath: string) {
  return new Promise<void>((resolve, reject) => {
    uni.saveImageToPhotosAlbum({
      filePath,
      success: () => resolve(),
      fail: (err) => reject(new Error(err.errMsg || '保存失败')),
    })
  })
}

export function shareDeliveryNoteImage(filePath: string) {
  const wxApi = (globalThis as { wx?: { showShareImageMenu?: (options: { path: string }) => void } }).wx
  if (wxApi?.showShareImageMenu) {
    wxApi.showShareImageMenu({ path: filePath })
    return Promise.resolve()
  }
  previewDeliveryNoteImage(filePath)
  return Promise.resolve()
}
