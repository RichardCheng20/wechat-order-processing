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
        <view
          v-for="item in cartStore.items"
          :key="item.custom ? `custom-${item.name}-${item.unit}` : `${item.productId}-${item.unit}`"
          class="card"
        >
          <view class="card-main" @tap="openEdit(item)">
            <AppIcon class="cart-item-icon" name="product" tone="green" :size="25" />
            <view class="main">
              <view class="name-row">
                <text class="name">{{ item.name }}</text>
                <text v-if="item.custom" class="custom-tag">自定义</text>
              </view>
              <text class="unit">{{ item.qty }}{{ item.unit }}</text>
              <text v-if="item.remark" class="item-remark">备注：{{ item.remark }}</text>
              <text v-else class="edit-hint">点击修改数量或添加备注</text>
            </view>
          </view>
          <view class="card-actions">
            <view class="delete-btn" @tap.stop="confirmRemove(item)">
              <AppIcon name="delete" tone="gray" :size="18" :tile="false" />
              <text>删除</text>
            </view>
          </view>
        </view>
      </view>

      <view class="remark-box">
        <text class="label">整单备注</text>
        <u-input v-model="remark" placeholder="如有特殊要求请填写" />
      </view>

      <view class="footer">
        <text class="summary">共 {{ cartStore.totalKinds }} 种，{{ cartStore.totalQty }} 件</text>
        <u-button type="primary" text="提交订单" :loading="submitting" @click="handleSubmit" />
      </view>
    </view>

    <view v-if="showEdit && editingItem" class="order-mask" @tap="closeEdit">
      <view class="order-sheet" @tap.stop>
        <view class="order-head">
          <text class="order-close" @tap="closeEdit">×</text>
          <text class="order-title">编辑商品</text>
          <text class="order-unit">{{ editingItem.unit }}</text>
        </view>

        <view class="order-product-row">
          <text class="order-product-name">{{ editingItem.name }}</text>
        </view>

        <view class="order-fields">
          <view class="order-field full">
            <text class="field-label">下单数 ({{ editingItem.unit }})</text>
            <view class="field-box active">
              <text v-if="editQty" class="field-value">{{ editQty }}</text>
              <text v-else class="field-placeholder">0</text>
            </view>
          </view>
        </view>

        <view class="order-footer-row">
          <text class="remark-link" @tap="openEditRemark">添加备注</text>
          <text class="delete-link" @tap="confirmRemove(editingItem)">删除商品</text>
        </view>
        <text v-if="editRemark" class="order-remark-preview">备注：{{ editRemark }}</text>

        <view class="keypad">
          <view class="keypad-grid">
            <view class="key" @tap="inputKey('1')">1</view>
            <view class="key" @tap="inputKey('2')">2</view>
            <view class="key" @tap="inputKey('3')">3</view>
            <view class="key fn" @tap="backspaceKey">⌫</view>

            <view class="key" @tap="inputKey('4')">4</view>
            <view class="key" @tap="inputKey('5')">5</view>
            <view class="key" @tap="inputKey('6')">6</view>
            <view class="key fn" @tap="clearQty">清零</view>

            <view class="key" @tap="inputKey('7')">7</view>
            <view class="key" @tap="inputKey('8')">8</view>
            <view class="key" @tap="inputKey('9')">9</view>
            <view class="key confirm" @tap="confirmEdit">确定</view>

            <view class="key" @tap="inputKey('.')">.</view>
            <view class="key" @tap="inputKey('0')">0</view>
            <view class="key blank" />
            <view class="key blank" />
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { onShow } from '@dcloudio/uni-app'
import { ref } from 'vue'
import { createCustomerOrder } from '@common/api/order'
import AppIcon from '@/components/AppIcon.vue'
import { useCartStore, type CartItem } from '@common/stores/cart'
import { useUserStore } from '@common/stores/user'

const userStore = useUserStore()
const cartStore = useCartStore()
const remark = ref('')
const shopName = ref('')
const submitting = ref(false)
const showEdit = ref(false)
const editingItem = ref<CartItem | null>(null)
const editQty = ref('')
const editRemark = ref('')

onShow(() => {
  if (!userStore.isLoggedIn || !userStore.isCustomer) {
    uni.reLaunch({ url: '/packages/common/login/index' })
    return
  }
  if (!userStore.customerId) {
    shopName.value = cartStore.guestShopName || userStore.nickname || ''
  }
})

function onShopNameChange() {
  cartStore.setGuestShopName(shopName.value)
}

function openEdit(item: CartItem) {
  editingItem.value = { ...item }
  editQty.value = String(item.qty)
  editRemark.value = item.remark || ''
  showEdit.value = true
}

function closeEdit() {
  showEdit.value = false
  editingItem.value = null
  editQty.value = ''
  editRemark.value = ''
}

function inputKey(key: string) {
  if (key === '.' && editQty.value.includes('.')) return
  if (editQty.value === '0' && key !== '.') {
    editQty.value = key
    return
  }
  editQty.value += key
}

function backspaceKey() {
  editQty.value = editQty.value.slice(0, -1)
}

function clearQty() {
  editQty.value = ''
}

function openEditRemark() {
  uni.showModal({
    title: '商品备注',
    editable: true,
    placeholderText: '如：要大一点的、小土豆',
    content: editRemark.value,
    success: (res) => {
      if (res.confirm && res.content != null) {
        editRemark.value = res.content.trim()
      }
    },
  })
}

function confirmEdit() {
  const item = editingItem.value
  if (!item) return
  const qty = Number(editQty.value)
  if (!qty || qty <= 0) {
    uni.showModal({
      title: '提示',
      content: '数量为 0 将删除该商品，是否继续？',
      success: (res) => {
        if (res.confirm) {
          removeItem(item)
          closeEdit()
        }
      },
    })
    return
  }
  cartStore.upsertLine({
    productId: item.productId,
    name: item.name,
    unit: item.unit,
    qty,
    remark: editRemark.value.trim() || undefined,
  })
  closeEdit()
  uni.showToast({ title: '已更新', icon: 'success' })
}

function confirmRemove(item: CartItem) {
  uni.showModal({
    title: '删除商品',
    content: `确定从购物车移除「${item.name}」？`,
    confirmColor: '#e53935',
    success: (res) => {
      if (!res.confirm) return
      removeItem(item)
      closeEdit()
    },
  })
}

function removeItem(item: CartItem) {
  cartStore.removeLine(item.productId, item.unit, item.custom, item.name)
  uni.showToast({ title: '已删除', icon: 'none' })
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
      items: cartStore.items.map((item) => {
        if (item.custom) {
          return {
            customName: item.name,
            orderQty: item.qty,
            unit: item.unit,
            pickRemark: item.remark || undefined,
          }
        }
        return {
          productId: item.productId,
          orderQty: item.qty,
          unit: item.unit,
          pickRemark: item.remark || undefined,
        }
      }),
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
  align-items: stretch;
  background: #fff;
  border-radius: 14rpx;
  margin-bottom: 14rpx;
  border: 1rpx solid #dce6df;
  overflow: hidden;
}

.card-main {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 18rpx;
  padding: 24rpx;
}

.cart-item-icon {
  flex-shrink: 0;
}

.main {
  flex: 1;
  min-width: 0;
}

.name-row {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.name {
  display: block;
  font-size: 34rpx;
  font-weight: 800;
  color: #17211b;
}

.custom-tag {
  flex-shrink: 0;
  padding: 2rpx 10rpx;
  font-size: 20rpx;
  color: #07c160;
  background: #e8f8ef;
  border-radius: 6rpx;
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

.edit-hint {
  display: block;
  margin-top: 8rpx;
  font-size: 22rpx;
  color: #999;
}

.card-actions {
  display: flex;
  flex-direction: column;
  justify-content: center;
  border-left: 1rpx solid #eef2ed;
}

.delete-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6rpx;
  width: 96rpx;
  height: 100%;
  min-height: 120rpx;
  color: #999;
  font-size: 22rpx;
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

.order-mask {
  position: fixed;
  inset: 0;
  z-index: 200;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
}

.order-sheet {
  background: #fff;
  border-radius: 24rpx 24rpx 0 0;
  padding-bottom: env(safe-area-inset-bottom);
}

.order-head {
  display: flex;
  align-items: center;
  padding: 24rpx 32rpx 8rpx;
}

.order-close {
  width: 80rpx;
  font-size: 48rpx;
  color: #999;
  line-height: 1;
}

.order-title {
  flex: 1;
  text-align: center;
  font-size: 34rpx;
  font-weight: 600;
}

.order-unit {
  min-width: 80rpx;
  text-align: right;
  font-size: 28rpx;
  color: #07c160;
}

.order-product-row {
  padding: 8rpx 32rpx 20rpx;
}

.order-product-name {
  font-size: 30rpx;
  color: #666;
}

.order-fields {
  padding: 0 32rpx;
}

.field-label {
  display: block;
  margin-bottom: 12rpx;
  font-size: 24rpx;
  color: #999;
}

.field-box {
  height: 96rpx;
  padding: 0 24rpx;
  display: flex;
  align-items: center;
  background: #f5f6f7;
  border-radius: 12rpx;
  border: 2rpx solid transparent;
}

.field-box.active {
  background: #fff;
  border-color: #07c160;
}

.field-value {
  font-size: 40rpx;
  font-weight: 600;
  color: #222;
}

.field-placeholder {
  font-size: 32rpx;
  color: #ccc;
}

.order-footer-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24rpx 32rpx 8rpx;
}

.remark-link {
  font-size: 28rpx;
  color: #666;
}

.delete-link {
  font-size: 28rpx;
  color: #e53935;
}

.order-remark-preview {
  display: block;
  padding: 0 32rpx 16rpx;
  font-size: 24rpx;
  color: #e67e22;
}

.keypad {
  background: #eef0f2;
  padding: 12rpx;
  border-top: 1rpx solid #ddd;
}

.keypad-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  grid-template-rows: repeat(4, 96rpx);
  gap: 8rpx;
}

.key {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
  border-radius: 8rpx;
  font-size: 36rpx;
  font-weight: 500;
}

.key.fn {
  font-size: 26rpx;
  color: #666;
}

.key.confirm {
  grid-row: 3 / 5;
  grid-column: 4;
  background: #07c160;
  color: #fff;
  font-size: 32rpx;
  font-weight: 600;
}

.key.blank {
  visibility: hidden;
  pointer-events: none;
}
</style>
