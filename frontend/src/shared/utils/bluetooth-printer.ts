const STORAGE_KEY = 'boss_ble_printer_v1'
const CHUNK_SIZE = 20
const WRITE_DELAY_MS = 15

const COMMON_WRITE_UUIDS = [
  '0000FFE1-0000-1000-8000-00805F9B34FB',
  '49535343-8841-43F4-A8D4-ECBE34729BB3',
  '0000FF02-0000-1000-8000-00805F9B34FB',
]

export interface SavedPrinter {
  deviceId: string
  name: string
  serviceId: string
  characteristicId: string
}

function sleep(ms: number) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

function normalizeUuid(uuid: string) {
  return uuid.replace(/-/g, '').toUpperCase()
}

function isWritable(prop: UniApp.BLECharacteristicProperties | undefined) {
  return !!(prop?.write || prop?.writeNoResponse)
}

export function loadSavedPrinter(): SavedPrinter | null {
  try {
    const raw = uni.getStorageSync(STORAGE_KEY)
    if (!raw || typeof raw !== 'object') return null
    const p = raw as SavedPrinter
    if (!p.deviceId || !p.serviceId || !p.characteristicId) return null
    return p
  } catch {
    return null
  }
}

export function savePrinter(printer: SavedPrinter) {
  uni.setStorageSync(STORAGE_KEY, printer)
}

export function clearSavedPrinter() {
  uni.removeStorageSync(STORAGE_KEY)
}

let adapterOpened = false

export async function openAdapter(): Promise<void> {
  if (adapterOpened) return
  await new Promise<void>((resolve, reject) => {
    uni.openBluetoothAdapter({
      success: () => {
        adapterOpened = true
        resolve()
      },
      fail: (err) => reject(new Error(err.errMsg || '请打开手机蓝牙')),
    })
  })
}

export async function closeAdapter(): Promise<void> {
  if (!adapterOpened) return
  adapterOpened = false
  await new Promise<void>((resolve) => {
    uni.closeBluetoothAdapter({ complete: () => resolve() })
  })
}

export async function scanPrinters(timeoutMs = 10000): Promise<UniApp.BluetoothDevice[]> {
  await openAdapter()
  const found = new Map<string, UniApp.BluetoothDevice>()

  await new Promise<void>((resolve, reject) => {
    uni.startBluetoothDevicesDiscovery({
      allowDuplicatesKey: false,
      success: () => resolve(),
      fail: (err) => reject(new Error(err.errMsg || '搜索蓝牙设备失败')),
    })
  })

  const handler = (res: UniApp.OnBluetoothDeviceFoundCallbackResult) => {
    res.devices?.forEach((d) => {
      if (!d.deviceId) return
      const name = (d.name || d.localName || '').trim()
      if (!name) return
      found.set(d.deviceId, { ...d, name: name || d.deviceId })
    })
  }
  uni.onBluetoothDeviceFound(handler)

  await sleep(timeoutMs)

  uni.offBluetoothDeviceFound(handler)
  await new Promise<void>((resolve) => {
    uni.stopBluetoothDevicesDiscovery({ complete: () => resolve() })
  })

  const list = Array.from(found.values())
  list.sort((a, b) => (a.name || '').localeCompare(b.name || '', 'zh-CN'))
  return list
}

async function findWritableCharacteristic(deviceId: string) {
  const services = await new Promise<UniApp.GetBLEDeviceServicesSuccessCallbackResult>((resolve, reject) => {
    uni.getBLEDeviceServices({
      deviceId,
      success: resolve,
      fail: (err) => reject(new Error(err.errMsg || '获取蓝牙服务失败')),
    })
  })

  let fallback: { serviceId: string; characteristicId: string } | null = null

  for (const service of services.services || []) {
    if (!service.uuid) continue
    const chars = await new Promise<UniApp.GetBLEDeviceCharacteristicsSuccessCallbackResult>((resolve, reject) => {
      uni.getBLEDeviceCharacteristics({
        deviceId,
        serviceId: service.uuid,
        success: resolve,
        fail: (err) => reject(new Error(err.errMsg || '获取蓝牙特征值失败')),
      })
    })

    for (const ch of chars.characteristics || []) {
      if (!ch.uuid || !isWritable(ch.properties)) continue
      const nu = normalizeUuid(ch.uuid)
      if (COMMON_WRITE_UUIDS.some((u) => normalizeUuid(u) === nu)) {
        return { serviceId: service.uuid, characteristicId: ch.uuid }
      }
      if (!fallback) {
        fallback = { serviceId: service.uuid, characteristicId: ch.uuid }
      }
    }
  }

  if (fallback) return fallback
  throw new Error('未找到可写入的打印通道，请确认是 ESC/POS 蓝牙打印机')
}

export async function connectPrinter(device: UniApp.BluetoothDevice): Promise<SavedPrinter> {
  await openAdapter()
  const deviceId = device.deviceId
  if (!deviceId) throw new Error('设备无效')

  await new Promise<void>((resolve, reject) => {
    uni.createBLEConnection({
      deviceId,
      timeout: 10000,
      success: () => resolve(),
      fail: (err) => reject(new Error(err.errMsg || '连接打印机失败')),
    })
  })

  await sleep(500)

  const writable = await findWritableCharacteristic(deviceId)
  const saved: SavedPrinter = {
    deviceId,
    name: device.name || device.localName || '蓝牙打印机',
    serviceId: writable.serviceId,
    characteristicId: writable.characteristicId,
  }
  savePrinter(saved)
  return saved
}

async function ensureConnected(printer: SavedPrinter) {
  await openAdapter()
  await new Promise<void>((resolve, reject) => {
    uni.createBLEConnection({
      deviceId: printer.deviceId,
      timeout: 10000,
      success: () => resolve(),
      fail: (err) => reject(new Error(err.errMsg || '连接打印机失败，请重新选择')),
    })
  })
  await sleep(300)
}

async function writeChunk(printer: SavedPrinter, chunk: Uint8Array) {
  const buffer = chunk.buffer.slice(chunk.byteOffset, chunk.byteOffset + chunk.byteLength)
  await new Promise<void>((resolve, reject) => {
    uni.writeBLECharacteristicValue({
      deviceId: printer.deviceId,
      serviceId: printer.serviceId,
      characteristicId: printer.characteristicId,
      value: buffer,
      success: () => resolve(),
      fail: (err) => reject(new Error(err.errMsg || '写入打印机失败')),
    })
  })
}

export async function printBuffer(data: ArrayBuffer | Uint8Array, printer?: SavedPrinter | null) {
  const target = printer || loadSavedPrinter()
  if (!target) {
    throw new Error('请先选择并连接打印机')
  }

  await ensureConnected(target)

  const bytes = data instanceof Uint8Array ? data : new Uint8Array(data)
  for (let i = 0; i < bytes.length; i += CHUNK_SIZE) {
    const slice = bytes.subarray(i, Math.min(i + CHUNK_SIZE, bytes.length))
    await writeChunk(target, slice)
    if (i + CHUNK_SIZE < bytes.length) {
      await sleep(WRITE_DELAY_MS)
    }
  }
}
