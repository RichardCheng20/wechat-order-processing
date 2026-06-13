<template>
  <view class="page">
    <view v-if="cartStore.items.length === 0" class="empty-wrap">
      <u-empty mode="car" text="购物车是空的" />
      <u-button type="primary" text="去选购" @click="goHome" />
    </view>

    <view v-else>
      <view class="list">
        <view v-for="item in cartStore.items" :key="item.productId" class="card">
          <view class="main">
            <text class="name">{{ item.name }}</text>
            <text class="unit">{{ item.unit }}</text>
          </view>
          <view class="side">
            <view class="qty-btn" @tap="changeQty(item.productId, -1)">-</view>
            <text class="qty">{{ item.qty }}</text>
            <view class="qty-btn add" @tap="changeQty(item.productId, 1)">+</view>
          </view>
        </view>
      </view>

      <view class="remark-box">
        <text class="label">备注</text>
        <u-input v-model="remark" placeholder="如有特殊要求请填写" />
      </view>

      <view class="footer">
        <text class="summary">共 {{ cartStore.totalKinds }} 种，{{ cartStore.totalQty }} 件</text>
        <u-button type="primary" text="提交订单" :loading="submitting" @click="handleSubmit" />
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { createCustomerOrder } from '../../../api/order'
import { useCartStore } from '../../../stores/cart'

const cartStore = useCartStore()
const remark = ref('')
const submitting = ref(false)

function changeQty(productId: number, delta: number) {
  const item = cartStore.items.find((i) => i.productId === productId)
  if (!item) return
  cartStore.setQty(productId, item.qty + delta)
}

async function handleSubmit() {
  if (cartStore.items.length === 0) return
  submitting.value = true
  try {
    const order = await createCustomerOrder({
      items: cartStore.items.map((item) => ({
        productId: item.productId,
        orderQty: item.qty,
      })),
      remark: remark.value || undefined,
    })
    cartStore.clear()
    uni.showToast({ title: '下单成功', icon: 'success' })
    setTimeout(() => {
      uni.redirectTo({ url: `/pages/customer/orders/index?id=${order.id}` })
    }, 500)
  } catch (e) {
    uni.showToast({ title: e instanceof Error ? e.message : '提交失败', icon: 'none', duration: 3000 })
  } finally {
    submitting.value = false
  }
}

function goHome() {
  uni.navigateBack()
}
</script>

<style scoped lang="scss">
.page {
  min-height: 100vh;
  padding: 24rpx;
  padding-bottom: 180rpx;
}

.empty-wrap {
  padding: 120rpx 0;
  text-align: center;
}

.list {
  margin-bottom: 24rpx;
}

.card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fff;
  border-radius: 16rpx;
  padding: 28rpx;
  margin-bottom: 16rpx;
}

.name {
  display: block;
  font-size: 32rpx;
  font-weight: 600;
}

.unit {
  display: block;
  margin-top: 8rpx;
  font-size: 24rpx;
  color: #999;
}

.side {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.qty-btn {
  width: 52rpx;
  height: 52rpx;
  line-height: 52rpx;
  text-align: center;
  border-radius: 50%;
  background: #f2f3f5;
  font-size: 32rpx;
}

.qty-btn.add {
  background: #27ae60;
  color: #fff;
}

.qty {
  min-width: 40rpx;
  text-align: center;
}

.remark-box {
  background: #fff;
  border-radius: 16rpx;
  padding: 28rpx;
}

.label {
  display: block;
  margin-bottom: 16rpx;
  font-size: 28rpx;
  color: #666;
}

.footer {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  background: #fff;
  padding: 24rpx 32rpx calc(24rpx + env(safe-area-inset-bottom));
  box-shadow: 0 -4rpx 20rpx rgba(0, 0, 0, 0.06);
}

.summary {
  display: block;
  margin-bottom: 16rpx;
  font-size: 28rpx;
  color: #666;
}
</style>
