<template>
  <view class="page">
    <view v-if="loading" class="loading-wrap">
      <u-loading-icon text="加载中" />
    </view>

    <template v-else-if="note">
      <scroll-view scroll-y class="preview-scroll">
        <view class="sheet">
          <text class="sheet-title">配送单</text>

          <view class="sheet-meta">
            <text>收货商户：{{ note.customerName }}</text>
            <text>送货时间：{{ note.deliveryDate }}</text>
          </view>

          <view class="table">
            <view class="table-row head">
              <text class="col idx">序号</text>
              <text class="col name">商品名</text>
              <text class="col unit">单位</text>
              <text class="col qty">数量</text>
              <text class="col price">单价</text>
              <text class="col subtotal">小计</text>
              <text class="col remark">备注</text>
            </view>
            <view v-for="row in note.rows" :key="row.index" class="table-row">
              <text class="col idx">{{ row.index }}</text>
              <text class="col name">{{ row.productName }}</text>
              <text class="col unit">{{ row.unit }}</text>
              <text class="col qty">{{ formatQuantity(row.quantity) }}</text>
              <text class="col price">{{ formatOptionalMoney(row.unitPrice) }}</text>
              <text class="col subtotal">{{ formatOptionalMoney(row.subtotal) }}</text>
              <text class="col remark">{{ row.remark }}</text>
            </view>
          </view>

          <view class="total-line">
            <text>合计金额：{{ formatOptionalMoney(note.totalAmount) }}</text>
          </view>

          <text class="page-no">页码：1 / 1</text>
        </view>
      </scroll-view>

      <view class="footer boss-bottom-bar">
        <view class="boss-primary-btn" @tap="handleConfirmPrint">确认发送</view>
        <view class="boss-secondary-btn" @tap="handleShareImage">分享图片</view>
      </view>
    </template>

    <canvas
      canvas-id="deliveryNoteCanvas"
      id="deliveryNoteCanvas"
      class="hidden-canvas"
      :style="{ width: `${canvasWidth}px`, height: `${canvasHeight}px` }"
    />
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { fetchBossOrderDetail, markBossOrderPrinted } from '../../../../api/order'
import { uploadPaymentVoucher } from '../../../../api/payment'
import {
  buildDeliveryNote,
  DELIVERY_NOTE_CANVAS,
  exportDeliveryNoteImage,
  formatOptionalMoney,
  formatQuantity,
  previewDeliveryNoteImage,
  saveDeliveryNoteImage,
  shareDeliveryNoteImage,
  type DeliveryNoteData,
} from '../../../../utils/delivery-note'

const orderId = ref(0)
const loading = ref(false)
const exporting = ref(false)
const note = ref<DeliveryNoteData | null>(null)
const canvasWidth = DELIVERY_NOTE_CANVAS.width

const canvasHeight = computed(() =>
  note.value ? DELIVERY_NOTE_CANVAS.calcHeight(note.value.rows.length) : 800,
)

onLoad((query) => {
  orderId.value = Number(query?.id || 0)
  if (!orderId.value) {
    uni.showToast({ title: '订单不存在', icon: 'none' })
    return
  }
  loadOrder()
})

async function loadOrder() {
  loading.value = true
  try {
    const order = await fetchBossOrderDetail(orderId.value)
    note.value = buildDeliveryNote(order)
  } catch (err) {
    uni.showToast({ title: err instanceof Error ? err.message : '加载失败', icon: 'none' })
    setTimeout(() => uni.navigateBack(), 1200)
  } finally {
    loading.value = false
  }
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
      content: '配送单将标记为已对账，请将订单图片发送给对应客户，客户侧将会看到订单的金额；',
      confirmText: '保存图片',
      cancelText: '知道了',
      success: async (res) => {
        if (res.confirm) {
          try {
            await saveDeliveryNoteImage(filePath)
            uni.showToast({ title: '已保存到手机相册', icon: 'success' })
          } catch (err) {
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

async function handleShareImage() {
  if (!note.value) return
  uni.showLoading({ title: '生成中' })
  try {
    const filePath = await createImage()
    uni.hideLoading()
    if (!filePath) return
    await shareDeliveryNoteImage(filePath)
  } catch (err) {
    uni.hideLoading()
    uni.showToast({ title: err instanceof Error ? err.message : '分享失败', icon: 'none' })
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
  margin: 24rpx;
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

.hidden-canvas {
  position: fixed;
  left: -9999px;
  top: -9999px;
  opacity: 0;
  pointer-events: none;
}
</style>
