<template>
  <view class="page">
    <view v-if="loading" class="loading-wrap">
      <u-loading-icon text="加载中" />
    </view>

    <view v-else-if="order" class="content">
      <view v-if="(order.customItemCount || 0) > 0" class="custom-alert">
        <text class="custom-alert-title">本单含 {{ order.customItemCount }} 项非本档口商品</text>
        <text class="custom-alert-desc">代采商品请同时填写成本价与售价，客户仅能看到售价</text>
      </view>
      <view class="header">
        <text class="customer-name">{{ order.customerName }}</text>
        <text class="meta">{{ order.orderNo }} · {{ order.deliveryAddressShort || '—' }}</text>
        <text v-if="order.remark" class="remark">备注：{{ order.remark }}</text>
      </view>

      <view class="items">
        <view v-for="item in order.items || []" :key="item.id" class="item-card" :class="{ custom: item.customItem }">
          <view class="item-top">
            <text class="name">{{ item.productName }}</text>
            <text v-if="item.customItem" class="custom-tag">代采</text>
            <text v-if="item.shortageFlag" class="shortage">缺货</text>
          </view>
          <text class="qty">
            下单 {{ item.orderQty }}{{ item.unit }}
            <text v-if="item.actualQty != null"> · 实际 {{ item.actualQty }}{{ item.unit }}</text>
          </text>
          <text v-if="item.pickRemark" class="pick-remark">分拣备注：{{ item.pickRemark }}</text>
          <text v-if="!item.customItem" class="ref">参考价 ¥{{ formatPrice(item.referencePrice) }}</text>
          <view v-if="item.customItem" class="price-row">
            <text class="label">成本价</text>
            <u-input
              v-model="costMap[item.id]"
              type="digit"
              placeholder="拿货价"
              :disabled="readonly"
            />
            <text class="unit-hint">元/{{ item.unit }}</text>
          </view>
          <view class="price-row">
            <text class="label">{{ item.customItem ? '售价' : '成交单价' }}</text>
            <u-input
              v-model="priceMap[item.id]"
              type="digit"
              placeholder="单价"
              :disabled="readonly"
            />
            <text v-if="item.customItem" class="unit-hint">元/{{ item.unit }}</text>
          </view>
          <text v-if="item.customItem" class="profit-hint">毛利 ¥{{ calcProfit(item) }}</text>
          <text class="subtotal">小计 ¥{{ calcSubtotal(item) }}</text>
        </view>
      </view>

      <view class="footer boss-bottom-bar column">
        <text class="total">订单总额 ¥{{ totalAmount }}</text>
        <u-button
          v-if="!readonly"
          type="primary"
          text="确认录价"
          :loading="submitting"
          @click="handleSubmit"
        />
        <u-button v-else type="success" plain text="已录价" disabled />
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { onLoad } from '@dcloudio/uni-app'
import { computed, reactive, ref } from 'vue'
import { fetchPricingDetail, submitOrderPricing, type PricingLineItem, type PricingOrder } from '@common/api/pricing'
import { useUserStore } from '@common/stores/user'

const userStore = useUserStore()
const order = ref<PricingOrder | null>(null)
const loading = ref(false)
const submitting = ref(false)
const orderId = ref(0)
const priceMap = reactive<Record<number, string>>({})
const costMap = reactive<Record<number, string>>({})
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
    readonly.value = order.value.status === 'PRICED' || order.value.status === 'COMPLETED'
    for (const item of order.value.items || []) {
      priceMap[item.id] = String(item.dealPrice ?? item.referencePrice ?? '')
      costMap[item.id] = item.costPrice != null ? String(item.costPrice) : ''
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

function calcProfit(item: PricingLineItem) {
  const cost = Number(costMap[item.id] || 0)
  const price = Number(priceMap[item.id] || 0)
  if (!cost || !price) return '--'
  return ((price - cost) * qtyOf(item)).toFixed(2)
}

async function handleSubmit() {
  if (!order.value?.items?.length) return
  const items = order.value.items.map((item) => {
    const payload: { itemId: number; dealPrice: number; costPrice?: number } = {
      itemId: item.id,
      dealPrice: Number(priceMap[item.id]),
    }
    if (item.customItem) {
      payload.costPrice = Number(costMap[item.id])
    }
    return payload
  })
  if (items.some((i) => !i.dealPrice || i.dealPrice < 0)) {
    uni.showToast({ title: '请填写所有商品售价', icon: 'none' })
    return
  }
  if (items.some((i, idx) => order.value!.items![idx].customItem && (!i.costPrice || i.costPrice < 0))) {
    uni.showToast({ title: '代采商品请填写成本价', icon: 'none' })
    return
  }
  submitting.value = true
  try {
    await submitOrderPricing(orderId.value, items)
    uni.showToast({ title: '录价完成，即将返回上一页', icon: 'success' })
    setTimeout(() => {
      uni.navigateBack({
        fail: () => {
          uni.redirectTo({ url: '/pages/boss/pricing/index' })
        },
      })
    }, 800)
  } catch (e) {
    uni.showToast({ title: e instanceof Error ? e.message : '提交失败', icon: 'none', duration: 3000 })
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped lang="scss">
@import '../../../../styles/boss-footer.scss';

.page {
  min-height: 100vh;
  background: #f5f6f7;
}

.loading-wrap {
  padding: 80rpx 0;
  text-align: center;
}

.content {
  padding: 24rpx;
  padding-bottom: calc(180rpx + env(safe-area-inset-bottom));
}

.header {
  margin-bottom: 24rpx;
}

.customer-name {
  display: block;
  font-size: 36rpx;
  font-weight: 600;
}

.meta {
  display: block;
  margin-top: 8rpx;
  font-size: 26rpx;
  color: #888;
}

.remark {
  display: block;
  margin-top: 8rpx;
  font-size: 26rpx;
  color: #e67e22;
}

.custom-alert {
  margin-bottom: 20rpx;
  padding: 20rpx 24rpx;
  background: #fff7e6;
  border-radius: 12rpx;
  border: 1rpx solid #ffe8c7;
}

.custom-alert-title {
  display: block;
  font-size: 28rpx;
  font-weight: 600;
  color: #e67e22;
}

.custom-alert-desc {
  display: block;
  margin-top: 8rpx;
  font-size: 24rpx;
  color: #996633;
}

.item-card.custom {
  border: 1rpx solid #ffe8c7;
}

.custom-tag {
  font-size: 22rpx;
  color: #e67e22;
  background: #fff7e6;
  padding: 4rpx 12rpx;
  border-radius: 999rpx;
}

.unit-hint {
  font-size: 24rpx;
  color: #999;
}

.profit-hint {
  display: block;
  margin-top: 8rpx;
  font-size: 26rpx;
  color: #666;
}

.item-card {
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
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
  font-size: 22rpx;
  color: #c2352a;
}

.qty,
.pick-remark,
.ref {
  display: block;
  margin-top: 8rpx;
  font-size: 26rpx;
  color: #666;
}

.price-row {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-top: 16rpx;
}

.label {
  font-size: 28rpx;
  color: #333;
}

.subtotal {
  display: block;
  margin-top: 12rpx;
  font-size: 28rpx;
  color: #22c55e;
  font-weight: 600;
}

.footer.column {
  flex-direction: column;
  gap: 16rpx;
}

.total {
  font-size: 32rpx;
  font-weight: 700;
  text-align: center;
}
</style>
