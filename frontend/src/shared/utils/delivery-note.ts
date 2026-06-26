import type { OrderInfo, OrderLineItem } from '@common/api/order'
import type { WorkerTask } from '@common/api/worker'
import { isPaid } from './order-flow'
import { buildEscPosFromRgba } from './escpos-raster'

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
  merchantName?: string
  customerName: string
  deliveryDate: string
  rows: DeliveryNoteRow[]
  totalAmount: number | null
  priceIncomplete: boolean
  paymentStamp?: 'paid' | null
  /** 预览模式：不展示价格列 */
  previewMode?: boolean
  orderNo?: string
  orderTime?: string
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
    paymentStamp: isPaid(order) ? 'paid' : null,
  }
}

export function buildDeliveryNoteFromWorkerTask(task: WorkerTask): DeliveryNoteData {
  const items = task.items || []
  const rows = items.map((item, idx) => ({
    index: idx + 1,
    productName: item.productName || '未知商品',
    unit: item.unit || '斤',
    quantity: Number(item.orderQty),
    unitPrice: null,
    subtotal: null,
    remark: item.pickRemark?.trim() || '',
  }))

  return {
    merchantName: task.merchantName,
    customerName: task.customerName || '未知客户',
    deliveryDate: task.deliveryDate || formatToday(),
    rows,
    totalAmount: null,
    priceIncomplete: true,
    previewMode: true,
    orderNo: task.orderNo,
    orderTime: task.createdAt ? task.createdAt.replace('T', ' ').slice(0, 16) : '',
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

export function calcCanvasHeight(rowCount: number, previewMode = false) {
  return (previewMode ? HEADER_HEIGHT + 16 : HEADER_HEIGHT) + 40 + rowCount * ROW_HEIGHT + FOOTER_HEIGHT
}

export function drawDeliveryNoteCanvas(
  ctx: UniApp.CanvasContext,
  note: DeliveryNoteData,
  canvasHeight: number,
) {
  const preview = !!note.previewMode
  ctx.setFillStyle('#ffffff')
  ctx.fillRect(0, 0, CANVAS_WIDTH, canvasHeight)

  ctx.setFillStyle('#111111')
  ctx.setTextAlign('center')
  ctx.setFontSize(28)
  const title = note.merchantName?.trim() || '配送单'
  ctx.fillText(title, CANVAS_WIDTH / 2, 48)

  ctx.setTextAlign('left')
  ctx.setFontSize(20)
  ctx.fillText(`收货商户：${note.customerName}`, PADDING, 92)
  ctx.setTextAlign('right')
  const dateLabel = preview ? '下单时间' : '送货时间'
  const dateValue = preview ? (note.orderTime || note.deliveryDate) : note.deliveryDate
  ctx.fillText(`${dateLabel}：${dateValue}`, CANVAS_WIDTH - PADDING, 92)

  if (preview && note.orderNo) {
    ctx.setTextAlign('left')
    ctx.setFillStyle('#666666')
    ctx.setFontSize(18)
    ctx.fillText(`订单编号：${note.orderNo}`, PADDING, 114)
    ctx.setFillStyle('#111111')
  }

  const tableTop = preview && note.orderNo ? 136 : 120
  const colX = preview
    ? [PADDING, 88, 300, 380, 480]
    : [PADDING, 88, 250, 330, 430, 530, 620]
  const headers = preview
    ? ['序号', '商品名', '单位', '数量', '备注']
    : ['序号', '商品名', '单位', '数量', '单价', '小计', '备注']

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
    const values = preview
      ? [
          String(row.index),
          row.productName,
          row.unit,
          formatQuantity(row.quantity),
          row.remark,
        ]
      : [
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
      ctx.setTextAlign(idx === 1 || idx === values.length - 1 ? 'left' : 'center')
      const tx = idx === 1 || idx === values.length - 1 ? colX[idx] + 8 : colX[idx] + width / 2
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
  if (!preview) {
    const totalText = note.totalAmount != null ? formatMoney(note.totalAmount) : ''
    ctx.fillText(`合计金额：${totalText}`, PADDING, y)
  }

  ctx.setTextAlign('center')
  ctx.setFontSize(18)
  ctx.setFillStyle('#666666')
  ctx.fillText('页码：1 / 1', CANVAS_WIDTH / 2, canvasHeight - 24)

  if (note.paymentStamp === 'paid') {
    drawPaidStamp(ctx, CANVAS_WIDTH, canvasHeight)
  }
}

function drawPaidStamp(ctx: UniApp.CanvasContext, canvasWidth: number, canvasHeight: number) {
  const cx = canvasWidth - 150
  const cy = canvasHeight - 130
  const radius = 72

  ctx.save()
  ctx.setGlobalAlpha(0.88)
  ctx.setStrokeStyle('#07c160')
  ctx.setLineWidth(5)
  ctx.beginPath()
  ctx.arc(cx, cy, radius, 0, 2 * Math.PI)
  ctx.stroke()

  ctx.setFillStyle('#07c160')
  ctx.setFontSize(24)
  ctx.setTextAlign('center')
  ctx.fillText('已收款', cx, cy - 10)

  ctx.setLineWidth(6)
  ctx.setLineCap('round')
  ctx.setLineJoin('round')
  ctx.beginPath()
  ctx.moveTo(cx - 30, cy + 16)
  ctx.lineTo(cx - 8, cy + 38)
  ctx.lineTo(cx + 34, cy - 8)
  ctx.stroke()
  ctx.restore()
}

export const DELIVERY_NOTE_CANVAS = {
  width: CANVAS_WIDTH,
  calcHeight: (rowCount: number, previewMode = false) => calcCanvasHeight(rowCount, previewMode),
}

export function exportDeliveryNoteImage(
  note: DeliveryNoteData,
  canvasId = 'deliveryNoteCanvas',
): Promise<string> {
  const height = calcCanvasHeight(note.rows.length, !!note.previewMode)
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

function getCanvasImageData(canvasId: string, width: number, height: number) {
  return new Promise<UniApp.CanvasGetImageDataSuccessCallbackResult>((resolve, reject) => {
    uni.canvasGetImageData({
      canvasId,
      x: 0,
      y: 0,
      width,
      height,
      success: resolve,
      fail: (err) => reject(new Error(err.errMsg || '读取画布失败')),
    })
  })
}

/** 将订单详情画布转为 ESC/POS 位图指令（蓝牙热敏打印） */
export async function exportDeliveryNoteEscPos(
  note: DeliveryNoteData,
  canvasId = 'deliveryNoteCanvas',
): Promise<Uint8Array> {
  const height = calcCanvasHeight(note.rows.length, !!note.previewMode)
  const ctx = uni.createCanvasContext(canvasId)
  drawDeliveryNoteCanvas(ctx, note, height)
  await new Promise<void>((resolve) => {
    ctx.draw(false, () => setTimeout(resolve, 350))
  })
  const image = await getCanvasImageData(canvasId, CANVAS_WIDTH, height)
  return buildEscPosFromRgba(image.data, CANVAS_WIDTH, height)
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
