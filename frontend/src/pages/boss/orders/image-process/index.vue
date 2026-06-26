<template>
  <view class="page boss-page">
    <view v-if="loading" class="loading-wrap">
      <u-loading-icon text="加载中" />
    </view>

    <template v-else-if="order">
      <scroll-view scroll-y class="scroll-body">
        <view class="head-card">
          <view class="head-row">
            <text class="customer-name">{{ order.customerName || '未知客户' }}</text>
            <text class="status-tag">待处理图片</text>
          </view>
          <text class="meta">订单号 {{ order.orderNo }}</text>
          <text class="meta">下单时间 {{ formatTime(order.createdAt) }}</text>
          <text v-if="order.remark" class="remark">备注：{{ order.remark }}</text>
        </view>

        <view class="section-card">
          <text class="section-title">客户原始下单图片</text>
          <image
            v-if="sourceImageSrc"
            class="source-image"
            :src="sourceImageSrc"
            mode="widthFix"
            @tap="previewImage"
          />
          <text v-else class="empty-tip">暂无图片</text>
          <text v-if="sourceImageSrc" class="image-tip">点击图片可放大查看</text>
        </view>

        <view class="section-card">
          <view class="section-head">
            <text class="section-title">订单明细</text>
            <text class="section-action" @tap="goEdit">录入商品</text>
          </view>
          <view v-if="(order.items || []).length === 0" class="empty-items">
            <text>尚未录入商品，请对照上方图片添加明细</text>
          </view>
          <view v-else class="item-list">
            <view v-for="line in order.items" :key="line.id || `${line.productId}-${line.unit}`" class="item-row">
              <text class="item-name">{{ line.productName }}</text>
              <text class="item-qty">{{ line.orderQty }}{{ line.unit }}</text>
            </view>
          </view>
        </view>
      </scroll-view>

      <view class="boss-bottom-bar">
        <view class="boss-secondary-btn" @tap="goDetail">查看完整订单</view>
        <view
          class="boss-primary-btn"
          :class="{ disabled: !canConfirm }"
          @tap="handleConfirm"
        >
          {{ canConfirm ? '确认订单' : '请先录入商品' }}
        </view>
      </view>
    </template>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { confirmBossOrder, fetchBossOrderDetail, type OrderInfo } from '@common/api/order'
import { useUserStore } from '@common/stores/user'
import { resolveMediaUrl } from '@common/utils/media'
import { refreshBossOrderAlert } from '@common/utils/boss-order-alert'

const userStore = useUserStore()
const orderId = ref(0)
const order = ref<OrderInfo | null>(null)
const loading = ref(false)
const confirming = ref(false)

const sourceImageSrc = computed(() => resolveMediaUrl(order.value?.sourceImageUrl))
const canConfirm = computed(() => {
  const o = order.value
  if (!o || o.status !== 'PENDING_CONFIRM') return false
  return (o.items || []).length > 0
})

onLoad((query) => {
  orderId.value = Number(query?.id || 0)
})

onShow(async () => {
  if (!userStore.isLoggedIn || !userStore.isBoss) {
    uni.reLaunch({ url: '/pages/login/index' })
    return
  }
  if (!orderId.value) return
  await loadOrder()
})

async function loadOrder() {
  loading.value = true
  try {
    order.value = await fetchBossOrderDetail(orderId.value)
  } catch (e) {
    uni.showToast({ title: e instanceof Error ? e.message : '加载失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

function formatTime(value?: string) {
  if (!value) return '—'
  return value.replace('T', ' ').slice(0, 16)
}

function previewImage() {
  const url = sourceImageSrc.value
  if (!url) return
  uni.previewImage({ urls: [url], current: url })
}

function goEdit() {
  uni.navigateTo({ url: `/pages/boss/sales-order/index?orderId=${orderId.value}` })
}

function goDetail() {
  uni.navigateTo({ url: `/pages/boss/orders/detail/index?id=${orderId.value}` })
}

function handleConfirm() {
  if (!canConfirm.value || confirming.value) return
  uni.showModal({
    title: '确认订单',
    content: '确认后将进入待分拣，并按明细占用库存',
    success: async (res) => {
      if (!res.confirm) return
      confirming.value = true
      try {
        order.value = await confirmBossOrder(orderId.value)
        await refreshBossOrderAlert({ notify: false })
        uni.showToast({ title: '已确认', icon: 'success' })
        setTimeout(() => {
          uni.navigateBack({
            fail: () => {
              uni.redirectTo({ url: `/pages/boss/orders/detail/index?id=${orderId.value}` })
            },
          })
        }, 400)
      } catch (e) {
        uni.showToast({ title: e instanceof Error ? e.message : '确认失败', icon: 'none' })
      } finally {
        confirming.value = false
      }
    },
  })
}
</script>

<style scoped lang="scss">
@import '../../../../styles/boss-footer.scss';

.page {
  min-height: 100vh;
  background: #f5f7f3;
}

.loading-wrap {
  padding: 120rpx 0;
  text-align: center;
}

.scroll-body {
  height: calc(100vh - 140rpx - env(safe-area-inset-bottom));
  padding: 24rpx;
  box-sizing: border-box;
}

.head-card,
.section-card {
  background: #fff;
  border-radius: 16rpx;
  padding: 28rpx;
  margin-bottom: 24rpx;
  border: 1rpx solid #dce6df;
}

.head-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
}

.customer-name {
  font-size: 34rpx;
  font-weight: 700;
  color: #17211b;
}

.status-tag {
  flex-shrink: 0;
  padding: 6rpx 16rpx;
  font-size: 22rpx;
  color: #e67e22;
  background: #fff7e6;
  border-radius: 999rpx;
}

.meta {
  display: block;
  margin-top: 10rpx;
  font-size: 26rpx;
  color: #66736b;
}

.remark {
  display: block;
  margin-top: 16rpx;
  padding: 16rpx;
  font-size: 26rpx;
  color: #e67e22;
  background: #fffaf0;
  border-radius: 12rpx;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16rpx;
}

.section-title {
  font-size: 30rpx;
  font-weight: 700;
  color: #17211b;
}

.section-action {
  font-size: 26rpx;
  color: #0b7f3a;
}

.source-image {
  display: block;
  width: 100%;
  border-radius: 12rpx;
  background: #f5f6f8;
}

.image-tip,
.empty-tip {
  display: block;
  margin-top: 12rpx;
  font-size: 24rpx;
  color: #999;
}

.empty-items {
  padding: 32rpx 0;
  text-align: center;
  font-size: 26rpx;
  color: #999;
}

.item-list {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.item-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16rpx 0;
  border-bottom: 1rpx solid #f2f3f5;
}

.item-row:last-child {
  border-bottom: none;
}

.item-name {
  flex: 1;
  font-size: 28rpx;
  color: #17211b;
}

.item-qty {
  font-size: 28rpx;
  color: #66736b;
}

.boss-bottom-bar {
  display: flex;
  gap: 16rpx;
}

.boss-primary-btn.disabled {
  opacity: 0.5;
}
</style>
