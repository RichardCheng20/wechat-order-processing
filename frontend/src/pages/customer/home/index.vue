<template>
  <view class="page">
    <view class="banner">
      <view class="banner-top">
        <text class="welcome">你好，{{ userStore.displayName }}</text>
        <view class="banner-actions">
          <text class="link" @tap="goOrders">我的订单</text>
          <text class="link" @tap="handleLogout">退出</text>
        </view>
      </view>
      <text v-if="userStore.customerId" class="hint">选品下单即可，价格由老板录价后确认</text>
      <text v-else class="hint">临时下单：结算时填写店铺名称即可</text>
      <text v-if="!userStore.customerId" class="bind-link" @tap="goBind">已有邀请码？去绑定客户档案</text>
    </view>

    <view class="search-box">
      <u-search
        v-model="keyword"
        placeholder="搜索商品名称/别名"
        :show-action="false"
        @search="handleSearch"
        @clear="handleSearch"
      />
    </view>

    <scroll-view scroll-x class="category-bar" enable-flex>
      <view
        class="category-item"
        :class="{ active: activeCategoryId === null }"
        data-id=""
        @tap="onCategoryTap"
      >
        全部
      </view>
      <view
        v-for="item in categories"
        :key="item.id"
        class="category-item"
        :class="{ active: activeCategoryId === item.id }"
        :data-id="item.id"
        @tap="onCategoryTap"
      >
        {{ item.name }}
      </view>
    </scroll-view>

    <view v-if="loading" class="loading-wrap">
      <u-loading-icon text="加载中" />
    </view>

    <view v-else-if="loadError" class="empty-wrap">
      <u-empty mode="wifi" :text="loadError" />
      <u-button class="retry-btn" type="primary" text="重新加载" @click="handleRetry" />
    </view>

    <view v-else-if="products.length === 0" class="empty-wrap">
      <u-empty mode="list" text="暂无可下单商品" />
    </view>

    <view v-else class="product-list">
      <view v-for="item in products" :key="item.id" class="product-card">
        <AppIcon class="product-icon" name="product" tone="green" :size="25" />
        <view class="product-main">
          <text class="product-name">{{ item.name }}</text>
          <text v-if="item.aliases" class="product-alias">{{ item.aliases }}</text>
          <text class="product-meta">{{ item.categoryName }} · {{ formatSaleUnits(item) }}</text>
        </view>
        <view class="product-side">
          <view class="qty-box">
            <view
              v-if="getQty(item.id) > 0"
              class="qty-btn"
              @tap="changeQty(item, -1)"
            >-</view>
            <text v-if="getQty(item.id) > 0" class="qty-value">{{ getQty(item.id) }}</text>
            <view class="qty-btn add" @tap="changeQty(item, 1)">+</view>
          </view>
        </view>
      </view>
    </view>

    <view v-if="cartStore.totalQty > 0" class="cart-bar">
      <view class="cart-info" @tap="goCart">
        <AppIcon class="cart-icon" name="cart" tone="white" :tile="false" :size="24" />
        <text class="cart-count">已选 {{ cartStore.totalKinds }} 种 / {{ cartStore.totalQty }} 件</text>
      </view>
      <view class="cart-submit" @tap="goCart">去结算</view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { onLoad, onShow } from '@dcloudio/uni-app'
import { nextTick, ref } from 'vue'
import { fetchBindStatus } from '../../../api/customer'
import { fetchCustomerCategories, fetchCustomerProducts, type CategoryItem, type ProductItem } from '../../../api/product'
import AppIcon from '../../../components/AppIcon.vue'
import { parseSaleUnits } from '../../../constants/units'
import { useCartStore } from '../../../stores/cart'
import { useUserStore } from '../../../stores/user'

const userStore = useUserStore()
const cartStore = useCartStore()
const categories = ref<CategoryItem[]>([])
const products = ref<ProductItem[]>([])
const activeCategoryId = ref<number | null>(null)
const keyword = ref('')
const loading = ref(false)
const loadError = ref('')
let fetching = false

onLoad(async () => {
  if (!userStore.isLoggedIn || !userStore.isCustomer) {
    uni.reLaunch({ url: '/pages/login/index' })
    return
  }
  await nextTick()
  await syncCustomerProfile()
  await refreshPage()
})

async function syncCustomerProfile() {
  try {
    const status = await fetchBindStatus()
    if (status.bound && status.customerId) {
      userStore.applyCustomerBind(status.customerId, status.customerName)
    }
  } catch {
    // ignore
  }
}

onShow(() => {
  if (!userStore.isLoggedIn || !userStore.isCustomer) {
    uni.reLaunch({ url: '/pages/login/index' })
  }
})

async function refreshPage() {
  if (fetching) return
  fetching = true
  try {
    await loadCategories()
    await loadProducts()
  } finally {
    fetching = false
  }
}

async function loadCategories() {
  categories.value = await fetchCustomerCategories()
}

async function loadProducts() {
  loading.value = true
  loadError.value = ''
  try {
    products.value = await fetchCustomerProducts({
      categoryId: activeCategoryId.value ?? undefined,
      keyword: keyword.value || undefined,
    })
  } catch (e) {
    products.value = []
    loadError.value = e instanceof Error ? e.message : '加载失败'
  } finally {
    loading.value = false
  }
}

function getQty(productId: number) {
  return cartStore.items
    .filter((item) => item.productId === productId)
    .reduce((sum, item) => sum + item.qty, 0)
}

function formatSaleUnits(item: ProductItem) {
  return parseSaleUnits(item.saleUnits, item.unit).join('/')
}

function changeQty(product: ProductItem, delta: number) {
  if (delta > 0) {
    const units = parseSaleUnits(product.saleUnits, product.unit)
    if (units.length > 1) {
      uni.showActionSheet({
        itemList: units.map((u) => `按${u}购买`),
        success: (res) => {
          const unit = units[res.tapIndex]
          cartStore.addProduct({ id: product.id, name: product.name, unit }, delta)
        },
      })
      return
    }
    cartStore.addProduct({ id: product.id, name: product.name, unit: units[0] || product.unit }, delta)
    return
  }
  const lines = cartStore.items.filter((item) => item.productId === product.id && item.qty > 0)
  if (lines.length === 0) return
  const target = lines[lines.length - 1]
  cartStore.addProduct({ id: product.id, name: product.name, unit: target.unit }, delta)
}

function onCategoryTap(e: { currentTarget: { dataset: { id?: string | number } } }) {
  const id = e.currentTarget.dataset.id
  activeCategoryId.value = id === '' || id === undefined ? null : Number(id)
  loadProducts()
}

function handleSearch() {
  loadProducts()
}

function handleRetry() {
  refreshPage()
}

function goCart() {
  uni.navigateTo({ url: '/pages/customer/cart/index' })
}

function goOrders() {
  uni.navigateTo({ url: '/pages/customer/orders/index' })
}

function goBind() {
  uni.navigateTo({ url: '/pages/customer/bind/index' })
}

function handleLogout() {
  userStore.signOut()
}
</script>

<style scoped lang="scss">
.page {
  min-height: 100vh;
  padding-bottom: 140rpx;
  background: #f5f7f3;
}

.banner {
  margin: 20rpx 24rpx 0;
  background: #0b7f3a;
  padding: 32rpx;
  color: #fff;
  border-radius: 18rpx;
}

.banner-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.banner-actions {
  display: flex;
  gap: 12rpx;
}

.welcome {
  font-size: 38rpx;
  font-weight: 800;
}

.link {
  padding: 10rpx 16rpx;
  font-size: 26rpx;
  color: #fff;
  background: rgba(255, 255, 255, 0.14);
  border-radius: 999rpx;
}

.hint {
  display: block;
  margin-top: 16rpx;
  font-size: 28rpx;
  line-height: 1.45;
  opacity: 0.92;
}

.bind-link {
  display: block;
  margin-top: 16rpx;
  font-size: 28rpx;
  font-weight: 700;
  text-decoration: underline;
}

.search-box {
  padding: 20rpx 24rpx 16rpx;
  background: #f5f7f3;
}

.category-bar {
  white-space: nowrap;
  background: #f5f7f3;
  padding: 0 24rpx 20rpx;
}

.category-item {
  display: inline-block;
  padding: 14rpx 28rpx;
  margin-right: 12rpx;
  border-radius: 999rpx;
  background: #fff;
  color: #66736b;
  font-size: 28rpx;
  border: 1rpx solid #dce6df;
}

.category-item.active {
  background: #e7f4ea;
  color: #0b7f3a;
  border-color: #0b7f3a;
  font-weight: 700;
}

.loading-wrap,
.empty-wrap {
  padding: 80rpx 0;
  text-align: center;
}

.retry-btn {
  margin-top: 24rpx;
  width: 240rpx;
}

.product-list {
  padding: 0 24rpx;
}

.product-card {
  display: flex;
  gap: 18rpx;
  align-items: center;
  background: #fff;
  border-radius: 14rpx;
  padding: 24rpx;
  margin-bottom: 14rpx;
  border: 1rpx solid #dce6df;
  box-sizing: border-box;
}

.product-icon {
  flex-shrink: 0;
}

.product-name {
  display: block;
  font-size: 34rpx;
  font-weight: 800;
  color: #17211b;
}

.product-alias {
  display: block;
  margin-top: 6rpx;
  font-size: 26rpx;
  color: #66736b;
}

.product-meta {
  display: block;
  margin-top: 8rpx;
  font-size: 26rpx;
  color: #66736b;
}

.product-main {
  flex: 1;
  min-width: 0;
}

.product-side {
  flex-shrink: 0;
  text-align: right;
}

.qty-box {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 16rpx;
}

.qty-btn {
  width: 64rpx;
  height: 64rpx;
  line-height: 64rpx;
  text-align: center;
  border-radius: 50%;
  background: #eef2ed;
  font-size: 38rpx;
  color: #17211b;
  font-weight: 700;
}

.qty-btn.add {
  background: #0b7f3a;
  color: #fff;
}

.qty-value {
  min-width: 48rpx;
  text-align: center;
  font-size: 32rpx;
  font-weight: 800;
  color: #17211b;
}

.cart-bar {
  position: fixed;
  left: 24rpx;
  right: 24rpx;
  bottom: 28rpx;
  display: flex;
  align-items: center;
  background: #17211b;
  color: #fff;
  border-radius: 18rpx;
  overflow: hidden;
  z-index: 10;
  box-shadow: 0 8rpx 26rpx rgba(23, 33, 27, 0.18);
}

.cart-info {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 14rpx;
  padding: 26rpx 28rpx;
}

.cart-icon {
  width: 52rpx;
  height: 52rpx;
  line-height: 52rpx;
  text-align: center;
  border-radius: 14rpx;
  background: rgba(255, 255, 255, 0.14);
  font-size: 28rpx;
  font-weight: 800;
}

.cart-count {
  font-size: 30rpx;
  font-weight: 700;
}

.cart-submit {
  padding: 28rpx 42rpx;
  background: #0b7f3a;
  font-size: 32rpx;
  font-weight: 800;
}
</style>
