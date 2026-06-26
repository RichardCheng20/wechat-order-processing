<template>
  <view class="page">
    <view v-if="loading" class="loading-wrap">
      <u-loading-icon text="加载中" />
    </view>

    <template v-else-if="note">
      <scroll-view scroll-y class="preview-scroll">
        <view class="sheet">
          <text class="sheet-title">采购单</text>
          <view class="sheet-meta">
            <text>供应商：{{ note.supplierName }}</text>
            <text>收货日期：{{ note.receiveDate }}</text>
          </view>
          <view v-if="note.merchantName" class="sheet-sub-meta">
            <text>采购方：{{ note.merchantName }}</text>
          </view>

          <view class="table">
            <view class="table-row head">
              <text class="col idx">序号</text>
              <text class="col name">商品名</text>
              <text class="col unit">单位</text>
              <text class="col qty">数量</text>
              <text class="col price">进价</text>
              <text class="col subtotal">小计</text>
            </view>
            <view v-for="row in note.rows" :key="row.index" class="table-row">
              <text class="col idx">{{ row.index }}</text>
              <text class="col name">{{ row.productName }}</text>
              <text class="col unit">{{ row.unit }}</text>
              <text class="col qty">{{ formatQuantity(row.quantity) }}</text>
              <text class="col price">{{ formatOptionalMoney(row.unitPrice) }}</text>
              <text class="col subtotal">{{ formatOptionalMoney(row.subtotal) }}</text>
            </view>
          </view>

          <view class="total-line">
            <text>合计金额：{{ formatOptionalMoney(note.totalAmount) }}</text>
          </view>
          <view v-if="note.remark" class="remark-line">
            <text>备注：{{ note.remark }}</text>
          </view>
        </view>
      </scroll-view>

      <view class="footer boss-bottom-bar">
        <view class="boss-secondary-btn" @tap="goBackEdit">继续选品</view>
        <view class="boss-primary-btn" @tap="handleShare">发给供应商</view>
        <view class="boss-secondary-btn" @tap="handleSaveImage">保存图片</view>
      </view>
    </template>

    <canvas
      canvas-id="purchaseNoteCanvas"
      id="purchaseNoteCanvas"
      class="hidden-canvas"
      :style="{ width: `${canvasWidth}px`, height: `${canvasHeight}px` }"
    />
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { fetchBossProfile } from '@common/api/profile'
import { usePurchaseOrderStore } from '@common/stores/purchaseOrder'
import { useUserStore } from '@common/stores/user'
import {
  buildPurchaseNote,
  exportPurchaseNoteImage,
  formatOptionalMoney,
  formatQuantity,
  PURCHASE_NOTE_CANVAS,
  savePurchaseNoteImage,
  sharePurchaseNoteImage,
  type PurchaseNoteData,
} from '@common/utils/purchase-note'

const userStore = useUserStore()
const purchaseOrder = usePurchaseOrderStore()

const loading = ref(false)
const exporting = ref(false)
const note = ref<PurchaseNoteData | null>(null)
const canvasWidth = PURCHASE_NOTE_CANVAS.width

const canvasHeight = computed(() =>
  note.value ? PURCHASE_NOTE_CANVAS.calcHeight(note.value.rows.length) : 800,
)

onLoad(() => {
  if (!userStore.isLoggedIn || !userStore.isBoss) {
    uni.reLaunch({ url: '/pages/login/index' })
    return
  }
  if (!purchaseOrder.hasSupplier || purchaseOrder.totalKinds === 0) {
    uni.showToast({ title: '请先选择供应商和商品', icon: 'none' })
    setTimeout(() => uni.navigateBack(), 800)
    return
  }
  loadNote()
})

async function loadNote() {
  loading.value = true
  try {
    const profile = await fetchBossProfile().catch(() => ({ merchantName: '', contactName: '', phone: '' }))
    note.value = buildPurchaseNote({
      merchantName: profile.merchantName,
      supplierName: purchaseOrder.supplierName,
      receiveDate: purchaseOrder.receiveDate,
      items: purchaseOrder.items,
      remark: purchaseOrder.remark,
    })
  } finally {
    loading.value = false
  }
}

async function createImage() {
  if (!note.value || exporting.value) return ''
  exporting.value = true
  try {
    return await exportPurchaseNoteImage(note.value)
  } finally {
    exporting.value = false
  }
}

async function handleSaveImage() {
  try {
    uni.showLoading({ title: '生成中' })
    const path = await createImage()
    if (!path) return
    await savePurchaseNoteImage(path)
    uni.showToast({ title: '已保存到相册', icon: 'success' })
  } catch (e) {
    uni.showToast({ title: e instanceof Error ? e.message : '保存失败', icon: 'none' })
  } finally {
    uni.hideLoading()
  }
}

async function handleShare() {
  try {
    uni.showLoading({ title: '生成中' })
    const path = await createImage()
    if (!path) return
    uni.hideLoading()
    await sharePurchaseNoteImage(path)
  } catch (e) {
    uni.showToast({ title: e instanceof Error ? e.message : '分享失败', icon: 'none' })
  } finally {
    uni.hideLoading()
  }
}

function goBackEdit() {
  uni.navigateBack()
}
</script>

<style scoped lang="scss">
@import '../../../styles/boss-footer.scss';

.page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f6f7;
  padding-bottom: calc(140rpx + env(safe-area-inset-bottom));
}

.loading-wrap {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.preview-scroll {
  flex: 1;
  height: 0;
  padding: 24rpx;
  box-sizing: border-box;
}

.sheet {
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
}

.sheet-title {
  display: block;
  text-align: center;
  font-size: 36rpx;
  font-weight: 600;
}

.sheet-meta {
  display: flex;
  justify-content: space-between;
  margin-top: 20rpx;
  font-size: 26rpx;
  color: #333;
}

.sheet-sub-meta {
  margin-top: 8rpx;
  font-size: 24rpx;
  color: #666;
}

.table {
  margin-top: 20rpx;
  border: 1rpx solid #ddd;
  border-radius: 8rpx;
  overflow: hidden;
}

.table-row {
  display: flex;
  border-bottom: 1rpx solid #eee;
  font-size: 22rpx;
}

.table-row.head {
  background: #fafafa;
  font-weight: 600;
}

.table-row:last-child {
  border-bottom: none;
}

.col {
  padding: 12rpx 6rpx;
  text-align: center;
  border-right: 1rpx solid #eee;
}

.col:last-child {
  border-right: none;
}

.col.idx { width: 8%; }
.col.name { width: 28%; text-align: left; }
.col.unit { width: 10%; }
.col.qty { width: 14%; }
.col.price { width: 18%; }
.col.subtotal { width: 22%; }

.total-line {
  margin-top: 20rpx;
  font-size: 28rpx;
  font-weight: 600;
  color: #e67e22;
}

.remark-line {
  margin-top: 12rpx;
  font-size: 24rpx;
  color: #666;
}

.footer {
  flex-wrap: wrap;
  gap: 12rpx;
}

.footer .boss-primary-btn,
.footer .boss-secondary-btn {
  flex: 1;
  min-width: 200rpx;
}

.hidden-canvas {
  position: fixed;
  left: -9999px;
  top: -9999px;
}
</style>
