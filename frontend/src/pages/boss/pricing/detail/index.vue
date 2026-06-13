<template>
  <view class="page">
    <view v-if="loading" class="loading-wrap">
      <u-loading-icon text="加载中" />
    </view>

    <view v-else-if="order" class="content">
      <view class="header">
        <text class="customer-name">{{ order.customerName }}</text>
        <text class="meta">{{ order.orderNo }} · {{ order.deliveryAddressShort || '—' }}</text>
        <text v-if="order.remark" class="remark">备注：{{ order.remark }}</text>
      </view>

      <view class="items">
        <view v-for="item in order.items || []" :key="item.id" class="item-card">
          <view class="item-top">
            <text class="name">{{ item.productName }}</text>
            <text v-if="item.shortageFlag" class="shortage">缺货</text>
          </view>
          <text class="qty">
            下单 {{ item.orderQty }}{{ item.unit }}
            <text v-if="item.actualQty != null"> · 实际 {{ item.actualQty }}{{ item.unit }}</text>
          </text>
          <text class="ref">参考价 ¥{{ formatPrice(item.referencePrice) }}</text>
          <view class="price-row">
            <text class="label">成交单价</text>
            <u-input
              v-model="priceMap[item.id]"
              type="digit"
              placeholder="单价"
              :disabled="readonly"
            />
          </view>
          <text class="subtotal">小计 ¥{{ calcSubtotal(item) }}</text>
        </view>
      </view>

      <view class="footer">
        <text class="total">订单总额 ¥{{ totalAmount }}</text>
        <u-button
          v-if="!readonly"
          type="primary"
          text="确认录价"
          :loading="submitting"
          @click="handleSubmit"
        />
        <u-button v-else type="success" plain text="已完成录价" disabled />
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { onLoad } from '@dcloudio/uni-app'
import { computed, reactive, ref } from 'vue'
import { fetchPricingDetail, submitOrderPricing, type PricingLineItem, type PricingOrder } from '../../../../api/pricing'
import { useUserStore } from '../../../../stores/user'

const userStore = useUserStore()
const order = ref<PricingOrder | null>(null)
const loading = ref(false)
const submitting = ref(false)
const orderId = ref(0)
const priceMap = reactive<Record<number, string>>({})
const readonly = ref(false)

onLoad(async (query) => {
  if (!userStore.isLoggedIn || !userStore.isBoss) {
    uni.reLaunch({ url: '/pages/login/index' })
    return
  }
  orderId.value = Number(query?.id || 0)
  await loadOrder()
})

async function loadOrder() {
  if (!orderId.value) return
  loading.value = true
  try {
    order.value = await fetchPricingDetail(orderId.value)
    readonly.value = order.value.status === 'COMPLETED'
    for (const item of order.value.items || []) {
      priceMap[item.id] = String(item.dealPrice ?? item.referencePrice ?? '')
    }
  } catch (e) {
    uni.showToast({ title: e instanceof Error ? e.message : '加载失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

function qtyOf(item: PricingLineItem) {
  return item.actualQty != null ? item.actualQty : item.orderQty
}

function calcSubtotal(item: PricingLineItem) {
  const price = Number(priceMap[item.id] || 0)
  if (!price) return '--'
  return (price * qtyOf(item)).toFixed(2)
}

const totalAmount = computed(() => {
  if (!order.value?.items) return '0.00'
  if (readonly.value && order.value.amount != null) {
    return Number(order.value.amount).toFixed(2)
  }
  let sum = 0
  for (const item of order.value.items) {
    const price = Number(priceMap[item.id] || 0)
    if (price > 0) sum += price * qtyOf(item)
  }
  return sum.toFixed(2)
})

function formatPrice(value?: number) {
  if (value == null) return '--'
  return Number(value).toFixed(2)
}

async function handleSubmit() {
  if (!order.value?.items?.length) return
  const items = order.value.items.map((item) => ({
    itemId: item.id,
    dealPrice: Number(priceMap[item.id]),
  }))
  if (items.some((i) => !i.dealPrice || i.dealPrice < 0)) {
    uni.showToast({ title: '请填写所有商品成交单价', icon: 'none' })
    return
  }
  submitting.value = true
  try {
    order.value = await submitOrderPricing(orderId.value, items)
    readonly.value = true
    uni.showToast({ title: '录价完成', icon: 'success' })
    setTimeout(() => uni.navigateBack(), 600)
  } catch (e) {
    uni.showToast({ title: e instanceof Error ? e.message : '提交失败', icon: 'none', duration: 3000 })
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped lang="scss">
.page {
  min-height: 100vh;
  padding-bottom: 180rpx;
}

.loading-wrap {
  padding: 80rpx 0;
  text-align: center;
}

.content {
  padding: 24rpx;
}

.header {
  background: #fff;
  border-radius: 16rpx;
  padding: 28rpx;
  margin-bottom: 24rpx;
}

.customer-name {
  display: block;
  font-size: 36rpx;
  font-weight: 600;
}

.meta {
  display: block;
  margin-top: 12rpx;
  font-size: 26rpx;
  color: #666;
}

.remark {
  display: block;
  margin-top: 12rpx;
  font-size: 26rpx;
  color: #e67e22;
}

.item-card {
  background: #fff;
  border-radius: 16rpx;
  padding: 28rpx;
  margin-bottom: 16rpx;
}

.item-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.name {
  font-size: 30rpx;
  font-weight: 600;
}

.shortage {
  color: #e74c3c;
  font-size: 24rpx;
}

.qty,
.ref {
  display: block;
  margin-top: 8rpx;
  font-size: 24rpx;
  color: #666;
}

.price-row {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-top: 20rpx;
}

.label {
  font-size: 28rpx;
  white-space: nowrap;
}

.subtotal {
  display: block;
  margin-top: 12rpx;
  font-size: 28rpx;
  color: #e67e22;
  font-weight: 600;
}

.footer {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 24rpx 32rpx calc(24rpx + env(safe-area-inset-bottom));
  background: #fff;
  box-shadow: 0 -4rpx 20rpx rgba(0, 0, 0, 0.06);
}

.total {
  display: block;
  margin-bottom: 16rpx;
  font-size: 32rpx;
  font-weight: 600;
  color: #27ae60;
}
</style>
