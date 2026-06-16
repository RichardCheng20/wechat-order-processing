<template>
  <view class="page">
    <view v-if="cartStore.items.length === 0" class="empty-wrap">
      <u-empty mode="car" text="购物车是空的" />
      <u-button type="primary" text="去选购" @click="goHome" />
    </view>

    <view v-else>
      <view v-if="!userStore.customerId" class="shop-card">
        <text class="shop-label">店铺/客户名称</text>
        <u-input
          v-model="shopName"
          placeholder="请输入您的店铺或客户名称"
          @change="onShopNameChange"
        />
        <text class="shop-tip">未绑定客户档案时必填，老板将按此名称处理订单</text>
      </view>

      <view class="list">
        <view v-for="item in cartStore.items" :key="`${item.productId}-${item.unit}`" class="card">
          <view class="main">
            <text class="name">{{ item.name }}</text>
            <text class="unit">{{ item.unit }}</text>
          </view>
          <view class="side">
            <view class="qty-btn" @tap="changeQty(item.productId, item.unit, -1)">-</view>
            <text class="qty">{{ item.qty }}</text>
            <view class="qty-btn add" @tap="changeQty(item.productId, item.unit, 1)">+</view>
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
import { onShow } from '@dcloudio/uni-app'
import { ref } from 'vue'
import { createCustomerOrder } from '../../../api/order'
import { useCartStore } from '../../../stores/cart'
import { useUserStore } from '../../../stores/user'

const userStore = useUserStore()
const cartStore = useCartStore()
const remark = ref('')
const shopName = ref('')
const submitting = ref(false)

onShow(() => {
  if (!userStore.isLoggedIn || !userStore.isCustomer) {
    uni.reLaunch({ url: '/pages/login/index' })
    return
  }
  if (!userStore.customerId) {
    shopName.value = cartStore.guestShopName || userStore.nickname || ''
  }
})

function onShopNameChange() {
  cartStore.setGuestShopName(shopName.value)
}

function changeQty(productId: number, unit: string, delta: number) {
  const item = cartStore.items.find((i) => i.productId === productId && i.unit === unit)
  if (!item) return
  cartStore.setQty(productId, item.qty + delta, unit)
}

async function handleSubmit() {
  if (cartStore.items.length === 0) return

  if (!userStore.customerId) {
    const name = shopName.value.trim()
    if (!name) {
      uni.showToast({ title: '请输入店铺/客户名称', icon: 'none' })
      return
    }
    cartStore.setGuestShopName(name)
  }

  submitting.value = true
  try {
    const payload = {
      items: cartStore.items.map((item) => ({
        productId: item.productId,
        orderQty: item.qty,
        unit: item.unit,
      })),
      remark: remark.value || undefined,
      ...(userStore.customerId ? {} : { customerName: shopName.value.trim() }),
    }
    const order = await createCustomerOrder(payload)
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

.shop-card {
  background: #fff;
  border-radius: 16rpx;
  padding: 28rpx;
  margin-bottom: 16rpx;
}

.shop-label {
  display: block;
  margin-bottom: 16rpx;
  font-size: 28rpx;
  font-weight: 600;
  color: #333;
}

.shop-tip {
  display: block;
  margin-top: 12rpx;
  font-size: 22rpx;
  color: #999;
  line-height: 1.5;
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
