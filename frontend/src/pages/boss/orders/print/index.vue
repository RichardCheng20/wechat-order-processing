<template>
  <view class="page">
    <view v-if="loading" class="loading-wrap">
      <u-loading-icon text="加载中" />
    </view>

    <template v-else-if="note">
      <scroll-view scroll-y class="preview-scroll">
        <view class="sheet">
          <text class="sheet-title">{{ sheetTitle }}</text>

          <view class="sheet-meta">
            <text>收货商户：{{ note.customerName }}</text>
            <text>{{ isDetailPrintMode ? '下单时间' : '送货时间' }}：{{ metaDateText }}</text>
          </view>
          <view v-if="isDetailPrintMode && orderNo" class="sheet-sub-meta">
            <text>订单编号：{{ orderNo }}</text>
          </view>

          <view class="table">
            <view class="table-row head">
              <text class="col idx">序号</text>
              <text class="col name">商品名</text>
              <text class="col unit">单位</text>
              <text class="col qty">数量</text>
              <text v-if="!isDetailPrintMode" class="col price">单价</text>
              <text v-if="!isDetailPrintMode" class="col subtotal">小计</text>
              <text class="col remark">备注</text>
            </view>
            <view v-for="row in note.rows" :key="row.index" class="table-row">
              <text class="col idx">{{ row.index }}</text>
              <text class="col name">{{ row.productName }}</text>
              <text class="col unit">{{ row.unit }}</text>
              <text class="col qty">{{ formatQuantity(row.quantity) }}</text>
              <text v-if="!isDetailPrintMode" class="col price">{{ formatOptionalMoney(row.unitPrice) }}</text>
              <text v-if="!isDetailPrintMode" class="col subtotal">{{ formatOptionalMoney(row.subtotal) }}</text>
              <text class="col remark">{{ row.remark }}</text>
            </view>
          </view>

          <view v-if="!isDetailPrintMode" class="total-line">
            <text>合计金额：{{ formatOptionalMoney(note.totalAmount) }}</text>
          </view>

          <text class="page-no">页码：1 / 1</text>
        </view>

        <view v-if="isDetailPrintMode" class="printer-panel">
          <view class="printer-head">
            <AppIcon name="bluetooth" tone="gray" :tile="false" :size="18" />
            <text class="printer-label">蓝牙打印机</text>
          </view>
          <view class="printer-status">
            <text class="status-name">{{ connectedPrinter?.name || '未连接打印机' }}</text>
            <text v-if="connectedPrinter" class="status-tag connected">已连接</text>
            <text v-else class="status-tag">未连接</text>
          </view>
          <view class="printer-actions">
            <view class="printer-link" @tap="openPrinterPicker">{{ connectedPrinter ? '更换打印机' : '选择打印机' }}</view>
            <view v-if="connectedPrinter" class="printer-link muted" @tap="disconnectPrinter">断开</view>
          </view>
        </view>
      </scroll-view>

      <view class="footer boss-bottom-bar detail-print-bar">
        <template v-if="isDetailPrintMode">
          <view class="boss-primary-btn" @tap="handleBlePrint">打印订单</view>
          <view class="boss-secondary-btn" @tap="handleSaveImage">保存图片</view>
        </template>
        <template v-else>
          <view class="boss-primary-btn block" @tap="handleConfirmPrint">确认发送</view>
        </template>
      </view>
    </template>

    <canvas
      canvas-id="deliveryNoteCanvas"
      id="deliveryNoteCanvas"
      class="hidden-canvas"
      :style="{ width: `${canvasWidth}px`, height: `${canvasHeight}px` }"
    />

    <u-popup :show="printerPickerVisible" mode="bottom" round="16" @close="closePrinterPicker">
      <view class="picker-panel">
        <view class="picker-head">
          <text class="picker-title">选择蓝牙打印机</text>
          <text class="picker-close" @tap="closePrinterPicker">×</text>
        </view>
        <text class="picker-tip">请打开打印机并靠近手机，点击设备名称连接</text>
        <view v-if="scanning" class="picker-loading">
          <u-loading-icon text="搜索中…" />
        </view>
        <scroll-view v-else scroll-y class="device-list">
          <view v-if="devices.length === 0" class="device-empty">未发现蓝牙设备，请确认打印机已开机</view>
          <view
            v-for="device in devices"
            :key="device.deviceId"
            class="device-item"
            @tap="connectDevice(device)"
          >
            <text class="device-name">{{ device.name || device.localName || '未知设备' }}</text>
            <text class="device-id">{{ device.deviceId }}</text>
          </view>
        </scroll-view>
        <view class="picker-footer">
          <view class="boss-secondary-btn" @tap="rescanDevices">重新搜索</view>
        </view>
      </view>
    </u-popup>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad, onUnload } from '@dcloudio/uni-app'
import AppIcon from '@/components/AppIcon.vue'
import { fetchBossOrderDetail, markBossOrderPrinted } from '@common/api/order'
import { fetchBossProfile } from '@common/api/profile'
import { uploadPaymentVoucher } from '@common/api/payment'
import {
  clearSavedPrinter,
  closeAdapter,
  connectPrinter,
  loadSavedPrinter,
  printBuffer,
  scanPrinters,
  type SavedPrinter,
} from '@common/utils/bluetooth-printer'
import {
  buildDeliveryNote,
  DELIVERY_NOTE_CANVAS,
  exportDeliveryNoteEscPos,
  exportDeliveryNoteImage,
  formatOptionalMoney,
  formatQuantity,
  previewDeliveryNoteImage,
  saveDeliveryNoteImage,
  type DeliveryNoteData,
} from '@common/utils/delivery-note'

const orderId = ref(0)
const pageMode = ref<'detail' | 'send'>('send')
const loading = ref(false)
const exporting = ref(false)
const printing = ref(false)
const note = ref<DeliveryNoteData | null>(null)
const orderNo = ref('')
const merchantName = ref('')
const createdAtText = ref('')
const connectedPrinter = ref<SavedPrinter | null>(loadSavedPrinter())
const printerPickerVisible = ref(false)
const scanning = ref(false)
const devices = ref<UniApp.BluetoothDevice[]>([])
const canvasWidth = DELIVERY_NOTE_CANVAS.width

const isDetailPrintMode = computed(() => pageMode.value === 'detail')

const sheetTitle = computed(() => merchantName.value.trim() || '配送单')

const canvasHeight = computed(() =>
  note.value ? DELIVERY_NOTE_CANVAS.calcHeight(note.value.rows.length, !!note.value.previewMode) : 800,
)

const metaDateText = computed(() => {
  if (isDetailPrintMode.value && createdAtText.value) return createdAtText.value
  return note.value?.deliveryDate || '—'
})

onLoad((query) => {
  orderId.value = Number(query?.id || 0)
  const mode = String(query?.mode || '')
  pageMode.value = mode === 'detail' || mode === 'preview' ? 'detail' : 'send'
  uni.setNavigationBarTitle({
    title: isDetailPrintMode.value ? '订单详情打印' : '发送对账单',
  })
  if (!orderId.value) {
    uni.showToast({ title: '订单不存在', icon: 'none' })
    return
  }
  loadOrder()
})

onUnload(() => {
  void closeAdapter()
})

async function loadOrder() {
  loading.value = true
  try {
    const [order, profile] = await Promise.all([
      fetchBossOrderDetail(orderId.value),
      fetchBossProfile().catch(() => ({ merchantName: '', contactName: '', phone: '' })),
    ])
    merchantName.value = profile.merchantName || ''
    orderNo.value = order.orderNo || ''
    createdAtText.value = formatDateTime(order.createdAt)
    note.value = {
      ...buildDeliveryNote(order),
      merchantName: merchantName.value,
      previewMode: isDetailPrintMode.value,
      orderNo: order.orderNo,
      orderTime: formatDateTime(order.createdAt),
    }
  } catch (err) {
    uni.showToast({ title: err instanceof Error ? err.message : '加载失败', icon: 'none' })
    setTimeout(() => uni.navigateBack(), 1200)
  } finally {
    loading.value = false
  }
}

function formatDateTime(value?: string) {
  if (!value) return '—'
  return value.replace('T', ' ').slice(0, 16)
}

async function createImage() {
  if (!note.value || exporting.value) {
    return ''
  }
  exporting.value = true
  try {
    return await exportDeliveryNoteImage(note.value)
  } finally {
    exporting.value = false
  }
}

async function handleSaveImage() {
  if (!note.value) return
  uni.showLoading({ title: '生成中' })
  try {
    const filePath = await createImage()
    uni.hideLoading()
    if (!filePath) return
    try {
      await saveDeliveryNoteImage(filePath)
      uni.showToast({ title: '已保存到相册', icon: 'success' })
    } catch (err) {
      previewDeliveryNoteImage(filePath)
      uni.showToast({
        title: err instanceof Error ? err.message : '请授权相册权限',
        icon: 'none',
      })
    }
  } catch (err) {
    uni.hideLoading()
    uni.showToast({ title: err instanceof Error ? err.message : '生成失败', icon: 'none' })
  }
}

async function handleBlePrint() {
  if (!note.value || printing.value) return
  if (!connectedPrinter.value) {
    openPrinterPicker()
    return
  }
  printing.value = true
  uni.showLoading({ title: '打印中' })
  try {
    const buffer = await exportDeliveryNoteEscPos(note.value)
    await printBuffer(buffer, connectedPrinter.value)
    uni.hideLoading()
    uni.showToast({ title: '已发送到打印机', icon: 'success' })
  } catch (err) {
    uni.hideLoading()
    uni.showToast({ title: err instanceof Error ? err.message : '打印失败', icon: 'none' })
  } finally {
    printing.value = false
  }
}

function openPrinterPicker() {
  printerPickerVisible.value = true
  rescanDevices()
}

function closePrinterPicker() {
  printerPickerVisible.value = false
}

async function rescanDevices() {
  scanning.value = true
  devices.value = []
  try {
    devices.value = await scanPrinters(9000)
  } catch (err) {
    uni.showToast({ title: err instanceof Error ? err.message : '搜索失败', icon: 'none' })
  } finally {
    scanning.value = false
  }
}

async function connectDevice(device: UniApp.BluetoothDevice) {
  uni.showLoading({ title: '连接中' })
  try {
    connectedPrinter.value = await connectPrinter(device)
    closePrinterPicker()
    uni.hideLoading()
    uni.showToast({ title: '打印机已连接', icon: 'success' })
  } catch (err) {
    uni.hideLoading()
    uni.showToast({ title: err instanceof Error ? err.message : '连接失败', icon: 'none' })
  }
}

function disconnectPrinter() {
  clearSavedPrinter()
  connectedPrinter.value = null
  uni.showToast({ title: '已断开', icon: 'none' })
}

async function handleConfirmPrint() {
  if (!note.value) return
  uni.showLoading({ title: '发送中' })
  try {
    const filePath = await createImage()
    if (!filePath) return
    const statementImageUrl = await uploadPaymentVoucher(filePath)
    await markBossOrderPrinted(orderId.value, { statementImageUrl })
    uni.hideLoading()
    uni.showModal({
      title: '发送成功',
      content: '配送单已标记为已对账，客户侧将看到订单金额。可保存图片后发给客户支付货款。',
      confirmText: '保存图片',
      cancelText: '知道了',
      success: async (res) => {
        if (res.confirm) {
          try {
            await saveDeliveryNoteImage(filePath)
            uni.showToast({ title: '已保存到相册', icon: 'success' })
          } catch {
            previewDeliveryNoteImage(filePath)
          }
        }
      },
    })
  } catch (err) {
    uni.hideLoading()
    uni.showToast({ title: err instanceof Error ? err.message : '发送失败', icon: 'none' })
  }
}
</script>

<style scoped lang="scss">
@import '../../../../styles/boss-footer.scss';

.page {
  height: 100vh;
  background: #f5f6f8;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.loading-wrap {
  padding: 120rpx 0;
  text-align: center;
}

.preview-scroll {
  flex: 1;
  height: 0;
  padding-bottom: calc(140rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
}

.sheet {
  margin: 24rpx 24rpx 0;
  padding: 32rpx 24rpx 40rpx;
  background: #fff;
  border-radius: 12rpx;
}

.sheet-title {
  display: block;
  text-align: center;
  font-size: 40rpx;
  font-weight: 700;
  color: #111;
}

.sheet-meta {
  display: flex;
  justify-content: space-between;
  gap: 16rpx;
  margin-top: 28rpx;
  font-size: 24rpx;
  color: #333;
}

.sheet-sub-meta {
  margin-top: 12rpx;
  font-size: 24rpx;
  color: #666;
}

.table {
  margin-top: 24rpx;
  border: 1rpx solid #333;
}

.table-row {
  display: flex;
  border-top: 1rpx solid #333;
}

.table-row.head {
  border-top: none;
  background: #fafafa;
  font-weight: 600;
}

.col {
  padding: 16rpx 8rpx;
  font-size: 22rpx;
  color: #111;
  border-left: 1rpx solid #333;
  word-break: break-all;
}

.col:first-child {
  border-left: none;
}

.col.idx { width: 8%; text-align: center; }
.col.name { width: 22%; }
.col.unit { width: 10%; text-align: center; }
.col.qty { width: 12%; text-align: center; }
.col.price { width: 12%; text-align: center; }
.col.subtotal { width: 12%; text-align: center; }
.col.remark { flex: 1; }

.total-line {
  margin-top: 24rpx;
  font-size: 28rpx;
  color: #111;
}

.page-no {
  display: block;
  margin-top: 48rpx;
  text-align: center;
  font-size: 24rpx;
  color: #666;
}

.printer-panel {
  margin: 20rpx 24rpx 24rpx;
  padding: 24rpx;
  background: #fff;
  border-radius: 12rpx;
}

.printer-head {
  display: flex;
  align-items: center;
  gap: 10rpx;
}

.printer-label {
  font-size: 28rpx;
  font-weight: 600;
  color: #111;
}

.printer-status {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-top: 16rpx;
}

.status-name {
  flex: 1;
  font-size: 26rpx;
  color: #333;
}

.status-tag {
  flex-shrink: 0;
  padding: 4rpx 14rpx;
  font-size: 22rpx;
  color: #999;
  background: #f5f6f8;
  border-radius: 999rpx;
}

.status-tag.connected {
  color: #07c160;
  background: #ecfdf3;
}

.printer-actions {
  display: flex;
  gap: 24rpx;
  margin-top: 16rpx;
}

.printer-link {
  font-size: 26rpx;
  color: #07c160;
}

.printer-link.muted {
  color: #999;
}

.detail-print-bar {
  flex-wrap: wrap;
  gap: 12rpx;
}

.detail-print-bar .boss-primary-btn,
.detail-print-bar .boss-secondary-btn {
  flex: 1;
  min-width: 200rpx;
}

.hidden-canvas {
  position: fixed;
  left: -9999px;
  top: -9999px;
  opacity: 0;
  pointer-events: none;
}

.picker-panel {
  padding: 24rpx 24rpx calc(20rpx + env(safe-area-inset-bottom));
  background: #fff;
  max-height: 70vh;
}

.picker-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.picker-title {
  font-size: 32rpx;
  font-weight: 600;
}

.picker-close {
  width: 48rpx;
  height: 48rpx;
  line-height: 48rpx;
  text-align: center;
  font-size: 40rpx;
  color: #999;
}

.picker-tip {
  display: block;
  margin-top: 12rpx;
  font-size: 24rpx;
  color: #999;
}

.picker-loading {
  padding: 48rpx 0;
  text-align: center;
}

.device-list {
  max-height: 420rpx;
  margin-top: 16rpx;
}

.device-empty {
  padding: 40rpx 0;
  text-align: center;
  font-size: 26rpx;
  color: #999;
}

.device-item {
  padding: 20rpx 0;
  border-bottom: 1rpx solid #f0f0f0;
}

.device-name {
  display: block;
  font-size: 28rpx;
  color: #111;
}

.device-id {
  display: block;
  margin-top: 6rpx;
  font-size: 22rpx;
  color: #999;
}

.picker-footer {
  margin-top: 20rpx;
}
</style>
