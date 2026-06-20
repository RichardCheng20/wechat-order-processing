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
        <view v-for="item in cartStore.items" :key="`${item.productId}-${item.unit}`" class="card" @tap="editCartItem(item)">
          <AppIcon class="cart-item-icon" name="product" tone="green" :size="25" />
          <view class="main">
            <text class="name">{{ item.name }}</text>
            <text class="unit">{{ item.qty }}{{ item.unit }}</text>
            <text v-if="item.remark" class="item-remark">备注：{{ item.remark }}</text>
          </view>
          <view class="side" @tap.stop>
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
import AppIcon from '../../../components/AppIcon.vue'
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

function editCartItem(item: { productId: number; unit: string; remark?: string }) {
  uni.showModal({
    title: '商品备注',
    editable: true,
    placeholderText: '如：要大一点的、小土豆',
    content: item.remark || '',
    success: (res) => {
      if (!res.confirm || res.content == null) return
      const line = cartStore.items.find((entry) => entry.productId === item.productId && entry.unit === item.unit)
      if (!line) return
      cartStore.upsertLine({
        productId: line.productId,
        name: line.name,
        unit: line.unit,
        qty: line.qty,
        remark: res.content.trim() || undefined,
      })
    },
  })
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
        pickRemark: item.remark || undefined,
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
  uni.navigateTo({ url: '/pages/customer/home/index' })
}
</script>

<style scoped lang="scss">
.page {
  min-height: 100vh;
  padding: 24rpx;
  padding-bottom: 180rpx;
  background: #f5f7f3;
}

.empty-wrap {
  padding: 120rpx 0;
  text-align: center;
}

.shop-card {
  background: #fff;
  border-radius: 14rpx;
  padding: 28rpx;
  margin-bottom: 16rpx;
  border: 1rpx solid #dce6df;
}

.shop-label {
  display: block;
  margin-bottom: 16rpx;
  font-size: 30rpx;
  font-weight: 600;
  color: #17211b;
}

.shop-tip {
  display: block;
  margin-top: 12rpx;
  font-size: 26rpx;
  color: #66736b;
  line-height: 1.5;
}

.list {
  margin-bottom: 24rpx;
}

.card {
  display: flex;
  align-items: center;
  gap: 18rpx;
  background: #fff;
  border-radius: 14rpx;
  padding: 24rpx;
  margin-bottom: 14rpx;
  border: 1rpx solid #dce6df;
  box-sizing: border-box;
}

.cart-item-icon {
  flex-shrink: 0;
}

.main {
  flex: 1;
  min-width: 0;
}

.name {
  display: block;
  font-size: 34rpx;
  font-weight: 800;
  color: #17211b;
}

.unit {
  display: block;
  margin-top: 8rpx;
  font-size: 28rpx;
  color: #66736b;
}

.item-remark {
  display: block;
  margin-top: 8rpx;
  font-size: 24rpx;
  color: #e67e22;
  line-height: 1.4;
}

.side {
  display: flex;
  align-items: center;
  gap: 14rpx;
  flex-shrink: 0;
}

.qty-btn {
  width: 64rpx;
  height: 64rpx;
  line-height: 64rpx;
  text-align: center;
  border-radius: 50%;
  background: #eef2ed;
  color: #17211b;
  font-size: 38rpx;
  font-weight: 800;
}

.qty-btn.add {
  background: #0b7f3a;
  color: #fff;
}

.qty {
  min-width: 48rpx;
  text-align: center;
  font-size: 34rpx;
  font-weight: 800;
  color: #17211b;
}

.remark-box {
  background: #fff;
  border-radius: 14rpx;
  padding: 28rpx;
  border: 1rpx solid #dce6df;
}

.label {
  display: block;
  margin-bottom: 16rpx;
  font-size: 30rpx;
  color: #17211b;
  font-weight: 700;
}

.footer {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  background: #fff;
  padding: 24rpx 32rpx calc(24rpx + env(safe-area-inset-bottom));
  border-top: 1rpx solid #dce6df;
  box-shadow: 0 -4rpx 20rpx rgba(23, 33, 27, 0.08);
}

.summary {
  display: block;
  margin-bottom: 16rpx;
  font-size: 30rpx;
  color: #17211b;
  font-weight: 800;
}
</style>
