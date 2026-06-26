import type { PurchaseOrderLine } from '@common/stores/purchaseOrder'

export interface PurchaseNoteRow {
  index: number
  productName: string
  unit: string
  quantity: number
  unitPrice: number | null
  subtotal: number | null
}

export interface PurchaseNoteData {
  merchantName?: string
  supplierName: string
  receiveDate: string
  rows: PurchaseNoteRow[]
  totalAmount: number | null
  remark?: string
}

export function buildPurchaseNote(input: {
  merchantName?: string
  supplierName: string
  receiveDate: string
  items: PurchaseOrderLine[]
  remark?: string
}): PurchaseNoteData {
  const rows = input.items.map((item, idx) => {
    const price = item.purchasePrice != null ? Number(item.purchasePrice) : null
    const qty = Number(item.qty)
    const subtotal = price != null ? Number((qty * price).toFixed(2)) : null
    return {
      index: idx + 1,
      productName: item.productName,
      unit: item.unit,
      quantity: qty,
      unitPrice: price,
      subtotal,
    }
  })
  const priced = rows.every((r) => r.subtotal != null)
  const totalAmount = priced
    ? Number(rows.reduce((sum, r) => sum + (r.subtotal || 0), 0).toFixed(2))
    : null
  return {
    merchantName: input.merchantName,
    supplierName: input.supplierName,
    receiveDate: input.receiveDate,
    rows,
    totalAmount,
    remark: input.remark?.trim() || '',
  }
}

export function formatMoney(value: number) {
  return Number(value || 0).toFixed(2)
}

export function formatOptionalMoney(value: number | null | undefined) {
  if (value == null) return ''
  return formatMoney(value)
}

export function formatQuantity(value: number) {
  const n = Number(value || 0)
  return n % 1 === 0 ? String(n) : n.toFixed(2)
}

const CANVAS_WIDTH = 750
const PADDING = 24
const ROW_HEIGHT = 44
const HEADER_HEIGHT = 150
const FOOTER_HEIGHT = 100

export function calcPurchaseCanvasHeight(rowCount: number) {
  return HEADER_HEIGHT + 40 + rowCount * ROW_HEIGHT + FOOTER_HEIGHT
}

export function drawPurchaseNoteCanvas(
  ctx: UniApp.CanvasContext,
  note: PurchaseNoteData,
  canvasHeight: number,
) {
  ctx.setFillStyle('#ffffff')
  ctx.fillRect(0, 0, CANVAS_WIDTH, canvasHeight)

  ctx.setFillStyle('#111111')
  ctx.setTextAlign('center')
  ctx.setFontSize(28)
  ctx.fillText('采购单', CANVAS_WIDTH / 2, 48)

  ctx.setTextAlign('left')
  ctx.setFontSize(20)
  ctx.fillText(`供应商：${note.supplierName}`, PADDING, 88)
  ctx.setTextAlign('right')
  ctx.fillText(`收货日期：${note.receiveDate}`, CANVAS_WIDTH - PADDING, 88)

  if (note.merchantName) {
    ctx.setTextAlign('left')
    ctx.setFillStyle('#666666')
    ctx.setFontSize(18)
    ctx.fillText(`采购方：${note.merchantName}`, PADDING, 114)
    ctx.setFillStyle('#111111')
  }

  const tableTop = note.merchantName ? 132 : 120
  const colX = [PADDING, 88, 250, 330, 430, 530, 620]
  const headers = ['序号', '商品名', '单位', '数量', '进价', '小计', '备注']

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
      '',
    ]
    values.forEach((text, idx) => {
      const nextX = colX[idx + 1] ?? CANVAS_WIDTH - PADDING
      const width = nextX - colX[idx]
      ctx.setTextAlign(idx === 1 ? 'left' : 'center')
      const tx = idx === 1 ? colX[idx] + 8 : colX[idx] + width / 2
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

  if (note.remark) {
    y += 32
    ctx.setFontSize(18)
    ctx.setFillStyle('#666666')
    ctx.fillText(`备注：${note.remark}`, PADDING, y)
  }

  ctx.setTextAlign('center')
  ctx.setFontSize(18)
  ctx.fillText('页码：1 / 1', CANVAS_WIDTH / 2, canvasHeight - 24)
}

export const PURCHASE_NOTE_CANVAS = {
  width: CANVAS_WIDTH,
  calcHeight: calcPurchaseCanvasHeight,
}

export function exportPurchaseNoteImage(
  note: PurchaseNoteData,
  canvasId = 'purchaseNoteCanvas',
): Promise<string> {
  const height = calcPurchaseCanvasHeight(note.rows.length)
  const ctx = uni.createCanvasContext(canvasId)
  drawPurchaseNoteCanvas(ctx, note, height)

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

export function previewPurchaseNoteImage(filePath: string) {
  uni.previewImage({ urls: [filePath], current: filePath })
}

export function savePurchaseNoteImage(filePath: string) {
  return new Promise<void>((resolve, reject) => {
    uni.saveImageToPhotosAlbum({
      filePath,
      success: () => resolve(),
      fail: (err) => reject(new Error(err.errMsg || '保存失败')),
    })
  })
}

export function sharePurchaseNoteImage(filePath: string) {
  const wxApi = (globalThis as { wx?: { showShareImageMenu?: (options: { path: string }) => void } }).wx
  if (wxApi?.showShareImageMenu) {
    wxApi.showShareImageMenu({ path: filePath })
    return Promise.resolve()
  }
  previewPurchaseNoteImage(filePath)
  return Promise.resolve()
}
