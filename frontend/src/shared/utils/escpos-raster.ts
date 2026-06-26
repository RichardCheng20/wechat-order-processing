/** ESC/POS 位图与文本指令（58mm 纸宽约 384 点） */

export const PAPER_WIDTH_58MM = 384

function concatBytes(chunks: Uint8Array[]): Uint8Array {
  const total = chunks.reduce((sum, c) => sum + c.length, 0)
  const out = new Uint8Array(total)
  let offset = 0
  chunks.forEach((c) => {
    out.set(c, offset)
    offset += c.length
  })
  return out
}

export function escPosInit(): Uint8Array {
  return new Uint8Array([0x1b, 0x40])
}

export function escPosFeed(lines = 4): Uint8Array {
  return new Uint8Array([0x1b, 0x64, lines])
}

export function escPosCut(): Uint8Array {
  return new Uint8Array([0x1d, 0x56, 0x42, 0x00])
}

/** 将 RGBA 缩放到目标宽度并二值化，返回每行字节数与位图数据 */
export function rasterizeImageData(
  rgba: Uint8ClampedArray,
  srcWidth: number,
  srcHeight: number,
  targetWidth = PAPER_WIDTH_58MM,
): { widthBytes: number; height: number; bitmap: Uint8Array } {
  const targetHeight = Math.max(1, Math.round((srcHeight * targetWidth) / srcWidth))
  const widthBytes = Math.ceil(targetWidth / 8)
  const bitmap = new Uint8Array(widthBytes * targetHeight)

  for (let y = 0; y < targetHeight; y += 1) {
    const srcY = Math.min(srcHeight - 1, Math.floor((y * srcHeight) / targetHeight))
    for (let x = 0; x < targetWidth; x += 1) {
      const srcX = Math.min(srcWidth - 1, Math.floor((x * srcWidth) / targetWidth))
      const idx = (srcY * srcWidth + srcX) * 4
      const r = rgba[idx]
      const g = rgba[idx + 1]
      const b = rgba[idx + 2]
      const a = rgba[idx + 3]
      const gray = a < 16 ? 255 : 0.299 * r + 0.587 * g + 0.114 * b
      if (gray < 160) {
        const byteIndex = y * widthBytes + (x >> 3)
        bitmap[byteIndex] |= 0x80 >> (x & 7)
      }
    }
  }

  return { widthBytes, height: targetHeight, bitmap }
}

/** GS v 0 光栅位图 */
export function buildEscPosRaster(bitmap: Uint8Array, widthBytes: number, height: number): Uint8Array {
  const xL = widthBytes & 0xff
  const xH = (widthBytes >> 8) & 0xff
  const yL = height & 0xff
  const yH = (height >> 8) & 0xff
  const header = new Uint8Array([0x1d, 0x76, 0x30, 0x00, xL, xH, yL, yH])
  return concatBytes([header, bitmap])
}

export function buildEscPosFromRgba(
  rgba: Uint8ClampedArray,
  srcWidth: number,
  srcHeight: number,
  targetWidth = PAPER_WIDTH_58MM,
): Uint8Array {
  const { widthBytes, height, bitmap } = rasterizeImageData(rgba, srcWidth, srcHeight, targetWidth)
  return concatBytes([
    escPosInit(),
    buildEscPosRaster(bitmap, widthBytes, height),
    escPosFeed(3),
    escPosCut(),
  ])
}

export function encodeUtf8(text: string): Uint8Array {
  const encoded = encodeURIComponent(text)
  const bytes: number[] = []
  for (let i = 0; i < encoded.length; i += 1) {
    const ch = encoded[i]
    if (ch === '%') {
      bytes.push(parseInt(encoded.slice(i + 1, i + 3), 16))
      i += 2
    } else {
      bytes.push(ch.charCodeAt(0))
    }
  }
  return new Uint8Array(bytes)
}

/** 文本兜底打印（部分打印机中文可能乱码，优先用位图） */
export function buildEscPosText(lines: string[]): Uint8Array {
  const chunks: Uint8Array[] = [escPosInit(), new Uint8Array([0x1b, 0x61, 0x01])]
  lines.forEach((line) => {
    chunks.push(encodeUtf8(`${line}\n`))
  })
  chunks.push(escPosFeed(3), escPosCut())
  return concatBytes(chunks)
}
