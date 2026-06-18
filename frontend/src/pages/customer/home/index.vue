<template>
  <view class="page boss-page customer-order-page">
    <view class="page-head">
      <view class="banner">
        <view class="banner-top">
          <text class="welcome">你好，{{ userStore.displayName }}</text>
          <view class="banner-actions">
            <text class="link" @tap="goOrders">我的订单</text>
            <text class="link" @tap="handleLogout">退出</text>
          </view>
        </view>
        <text v-if="userStore.customerId" class="hint">选品下单即可，价格由老板录价后确认</text>
        <text v-else class="hint">临时下单：提交时填写店铺名称即可</text>
        <text v-if="!userStore.customerId" class="bind-link" @tap="goBind">已有邀请码？去绑定客户档案</text>
      </view>

      <view class="search-box">
        <view class="search-wrap">
          <text class="search-icon">⌕</text>
          <input
            class="search-input"
            type="text"
            :value="keyword"
            placeholder="搜索商品名称/别名"
            confirm-type="search"
            @input="onKeywordInput"
            @confirm="loadProducts"
          />
          <text v-if="keyword" class="search-clear" @tap="clearKeyword">×</text>
        </view>
      </view>
    </view>

    <view class="main" :style="mainStyle">
      <view class="category-col">
        <scroll-view scroll-y class="categories" :show-scrollbar="false">
          <view
            v-for="cat in categoryTabs"
            :key="cat.key"
            class="cat-item"
            :class="{ active: activeCategoryKey === cat.key }"
            @tap="switchCategory(cat.key)"
          >
            {{ cat.label }}
          </view>
        </scroll-view>
      </view>

      <view class="product-panel" :class="{ 'has-sub-cats': subCategoryTabs.length > 0 }">
        <scroll-view
          v-if="subCategoryTabs.length > 0"
          scroll-x
          class="sub-cat-scroll"
          :show-scrollbar="false"
        >
          <view class="sub-cat-row">
            <text
              v-for="tab in subCategoryTabs"
              :key="tab.key"
              class="sub-cat-chip"
              :class="{ active: subCategoryFilter === tab.key }"
              @tap="switchSubCategory(tab.key)"
            >{{ tab.label }}</text>
          </view>
        </scroll-view>

        <scroll-view scroll-y class="product-scroll" :show-scrollbar="false">
          <view v-if="loading" class="loading-wrap">
            <u-loading-icon text="加载中" />
          </view>
          <view v-else-if="loadError" class="empty-wrap">
            <text class="empty-title">{{ loadError }}</text>
            <view class="empty-btn" @tap="handleRetry">重新加载</view>
          </view>
          <view v-else-if="products.length === 0" class="empty-wrap">
            <text class="empty-title">{{ emptyTitle }}</text>
            <text class="empty-hint">{{ emptyHint }}</text>
          </view>
          <view v-else class="product-list">
            <view
              v-for="item in products"
              :key="item.id"
              class="product-row"
              :class="{ selected: cartCount(item.id) > 0 }"
              @tap="addProduct(item)"
            >
              <image
                v-if="item.imageUrl"
                class="product-thumb"
                :src="resolveMediaUrl(item.imageUrl)"
                mode="aspectFill"
              />
              <AppIcon v-else class="product-thumb-icon" name="product" tone="green" :size="22" />
              <view class="product-info">
                <text class="product-name">{{ item.name }}</text>
                <text class="product-meta">{{ formatSaleUnits(item) }}</text>
              </view>
              <view class="add-btn" :class="{ picked: cartCount(item.id) > 0 }" @tap.stop="addProduct(item)">
                <text v-if="cartCount(item.id) > 0" class="add-badge">{{ cartCount(item.id) }}</text>
                <text v-else>+</text>
              </view>
            </view>
          </view>
        </scroll-view>
      </view>
    </view>

    <view class="boss-bottom-bar">
      <view class="boss-tool-item" @tap="focusProductLibrary">
        <AppIcon class="boss-tool-icon" name="product" tone="green" :size="22" :tile-size="54" :radius="14" />
        <text class="boss-tool-label">商品库</text>
      </view>
      <view class="boss-tool-item" @tap="openBatchModal">
        <AppIcon class="boss-tool-icon" name="batch" tone="green" :size="22" :tile-size="54" :radius="14" />
        <text class="boss-tool-label">批量录入</text>
      </view>
      <view class="boss-tool-item" @tap="openVoiceModal">
        <AppIcon class="boss-tool-icon" name="voice" tone="green" :size="22" :tile-size="54" :radius="14" />
        <text class="boss-tool-label">语音录入</text>
      </view>
      <button class="boss-primary-btn" :loading="submitting" @tap="handleSubmit">
        提交{{ cartStore.totalKinds > 0 ? `(${cartStore.totalKinds})` : '' }}
      </button>
    </view>

    <u-popup :show="showBatchModal" mode="bottom" round="16" @close="closeBatchModal">
      <view class="batch-panel">
        <view class="batch-head">
          <text class="batch-close" @tap="closeBatchModal">×</text>
          <text class="batch-title">批量录入商品</text>
          <text class="batch-single" @tap="closeBatchModal">返回选品</text>
        </view>
        <textarea
          class="batch-textarea"
          :value="batchText"
          placeholder="粘贴示例：土豆5斤，番茄12.5斤..."
          maxlength="-1"
          :disabled="batchParsing"
          @input="onBatchTextInput"
        />
        <button class="batch-recognize-btn" :loading="batchParsing" @tap="recognizeBatchText">识别</button>
      </view>
    </u-popup>

    <u-popup :show="showVoiceModal" mode="bottom" round="16" @close="closeVoiceModal">
      <view class="batch-panel">
        <view class="batch-head">
          <text class="batch-close" @tap="closeVoiceModal">×</text>
          <text class="batch-title">语音录入</text>
        </view>
        <text class="voice-hint">点击输入框，使用键盘「语音」说话，如：土豆5斤，番茄2斤</text>
        <textarea
          class="batch-textarea voice-textarea"
          :value="voiceText"
          placeholder="说完后点下方「识别」"
          maxlength="-1"
          :disabled="voiceParsing"
          :focus="voiceInputFocus"
          @input="onVoiceTextInput"
        />
        <button class="batch-recognize-btn" :loading="voiceParsing" @tap="recognizeVoiceText">识别</button>
      </view>
    </u-popup>

    <u-popup :show="showGuestModal" mode="center" round="16" @close="closeGuestModal">
      <view class="guest-panel">
        <text class="guest-title">填写店铺/客户名称</text>
        <input
          class="guest-input"
          type="text"
          :value="guestShopName"
          placeholder="请输入您的店铺或客户名称"
          @input="onGuestNameInput"
        />
        <view class="guest-actions">
          <button class="guest-cancel" @tap="closeGuestModal">取消</button>
          <button class="guest-confirm" :loading="submitting" @tap="confirmGuestSubmit">确认提交</button>
        </view>
      </view>
    </u-popup>
  </view>
</template>

<script setup lang="ts">
import { computed, nextTick, ref } from 'vue'
import { onLoad, onReady, onShow } from '@dcloudio/uni-app'
import { fetchBindStatus } from '../../../api/customer'
import { createCustomerOrder } from '../../../api/order'
import { fetchCustomerCategories, fetchCustomerProducts, type CategoryItem, type ProductItem } from '../../../api/product'
import AppIcon from '../../../components/AppIcon.vue'
import { parseSaleUnits } from '../../../constants/units'
import { useCartStore } from '../../../stores/cart'
import { useUserStore } from '../../../stores/user'
import { buildPrimarySidebar, getParentCategory } from '../../../utils/category'
import { resolveMediaUrl } from '../../../utils/media'
import { applyParsedLines, parseOrderText } from '../../../utils/parseOrderText'

const userStore = useUserStore()
const cartStore = useCartStore()
const categories = ref<CategoryItem[]>([])
const products = ref<ProductItem[]>([])
const allProducts = ref<ProductItem[]>([])
const activeCategoryKey = ref('all')
const subCategoryFilter = ref('all')
const keyword = ref('')
const loading = ref(false)
const loadError = ref('')
const submitting = ref(false)
const showBatchModal = ref(false)
const batchText = ref('')
const batchParsing = ref(false)
const showVoiceModal = ref(false)
const voiceText = ref('')
const voiceParsing = ref(false)
const voiceInputFocus = ref(false)
const showGuestModal = ref(false)
const guestShopName = ref('')
const mainHeight = ref(0)
let fetching = false

const mainStyle = computed(() => (mainHeight.value > 0 ? { height: `${mainHeight.value}px` } : {}))

const categoryTabs = computed(() => buildPrimarySidebar(categories.value))

const subCategoryTabs = computed(() => {
  const parent = getParentCategory(categories.value, activeCategoryKey.value)
  if (!parent?.children?.length) return []
  return [
    { key: 'all', label: '全部' },
    ...parent.children.map((child) => ({ key: String(child.id), label: child.name })),
  ]
})

const emptyTitle = computed(() => {
  if (keyword.value.trim()) return '未找到匹配商品'
  if (activeCategoryKey.value !== 'all') return '该分类暂无可下单商品'
  return '暂无可下单商品'
})

const emptyHint = computed(() => {
  if (keyword.value.trim()) return '换个关键词试试'
  return ''
})

onLoad(async () => {
  if (!userStore.isLoggedIn || !userStore.isCustomer) {
    uni.reLaunch({ url: '/pages/login/index' })
    return
  }
  await syncCustomerProfile()
  await refreshPage()
})

onReady(() => {
  updateMainHeight()
})

onShow(async () => {
  if (!userStore.isLoggedIn || !userStore.isCustomer) {
    uni.reLaunch({ url: '/pages/login/index' })
    return
  }
  await nextTick()
  updateMainHeight()
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

async function refreshPage() {
  if (fetching) return
  fetching = true
  try {
    await loadCategories()
    await loadProducts()
  } finally {
    fetching = false
    await nextTick()
    updateMainHeight()
  }
}

async function loadCategories() {
  categories.value = await fetchCustomerCategories()
}

function resolveCategoryId(): number | undefined {
  if (activeCategoryKey.value === 'all') return undefined
  if (subCategoryFilter.value !== 'all') {
    return Number(subCategoryFilter.value)
  }
  if (activeCategoryKey.value === 'uncategorized') return undefined
  return Number(activeCategoryKey.value)
}

async function loadProducts() {
  loading.value = true
  loadError.value = ''
  try {
    if (activeCategoryKey.value === 'uncategorized') {
      products.value = (await fetchCustomerProducts({ keyword: keyword.value || undefined }))
        .filter((item) => !item.categoryId)
      return
    }
    const categoryId = resolveCategoryId()
    products.value = await fetchCustomerProducts({
      categoryId,
      keyword: keyword.value || undefined,
    })
  } catch (e) {
    products.value = []
    loadError.value = e instanceof Error ? e.message : '加载失败'
  } finally {
    loading.value = false
  }
}

async function ensureAllProducts() {
  if (allProducts.value.length > 0) return
  allProducts.value = await fetchCustomerProducts()
}

function switchCategory(key: string) {
  activeCategoryKey.value = key
  subCategoryFilter.value = 'all'
  loadProducts()
}

function switchSubCategory(key: string) {
  subCategoryFilter.value = key
  loadProducts()
}

function onKeywordInput(e: { detail: { value: string } }) {
  keyword.value = e.detail.value
}

function clearKeyword() {
  keyword.value = ''
  loadProducts()
}

function handleRetry() {
  refreshPage()
}

function formatSaleUnits(item: ProductItem) {
  return parseSaleUnits(item.saleUnits, item.unit).join('/')
}

function cartCount(productId: number) {
  return cartStore.items
    .filter((item) => item.productId === productId)
    .reduce((sum, item) => sum + item.qty, 0)
}

function addProduct(product: ProductItem) {
  const units = parseSaleUnits(product.saleUnits, product.unit)
  if (units.length > 1) {
    const labels = units.map((unit) => {
      const line = cartStore.items.find((i) => i.productId === product.id && i.unit === unit)
      return line ? `${unit}（已选${line.qty}）` : unit
    })
    uni.showActionSheet({
      itemList: labels,
      success: (res) => {
        const unit = units[res.tapIndex]
        cartStore.addProduct({ id: product.id, name: product.name, unit }, 1)
      },
    })
    return
  }
  cartStore.addProduct({ id: product.id, name: product.name, unit: units[0] || product.unit }, 1)
}

function focusProductLibrary() {
  activeCategoryKey.value = 'all'
  subCategoryFilter.value = 'all'
  keyword.value = ''
  loadProducts()
}

function openBatchModal() {
  showBatchModal.value = true
}

function closeBatchModal() {
  showBatchModal.value = false
}

function onBatchTextInput(e: { detail: { value: string } }) {
  batchText.value = e.detail.value
}

function openVoiceModal() {
  voiceText.value = ''
  showVoiceModal.value = true
  voiceInputFocus.value = false
  setTimeout(() => {
    voiceInputFocus.value = true
  }, 200)
}

function closeVoiceModal() {
  voiceInputFocus.value = false
  showVoiceModal.value = false
}

function onVoiceTextInput(e: { detail: { value: string } }) {
  voiceText.value = e.detail.value
}

async function handleRecognizedText(text: string) {
  const parsed = parseOrderText(text)
  if (parsed.length === 0) {
    uni.showToast({ title: '未识别到商品，请检查格式', icon: 'none' })
    return false
  }
  await ensureAllProducts()
  const result = applyParsedLines(parsed, allProducts.value, (line) => {
    cartStore.upsertLine({
      productId: line.productId,
      name: line.productName,
      unit: line.unit,
      qty: line.orderQty,
    })
  })
  if (result.added === 0) {
    uni.showToast({
      title: `未匹配：${result.unmatched.slice(0, 3).join('、')}`,
      icon: 'none',
    })
    return false
  }
  const extra = result.unmatched.length > 0 ? `，${result.unmatched.length}项未匹配` : ''
  uni.showToast({ title: `已录入${result.added}种商品${extra}`, icon: 'success' })
  return true
}

async function recognizeBatchText() {
  const text = batchText.value.trim()
  if (!text) {
    uni.showToast({ title: '请先粘贴或输入商品', icon: 'none' })
    return
  }
  batchParsing.value = true
  try {
    const ok = await handleRecognizedText(text)
    if (ok) {
      batchText.value = ''
      closeBatchModal()
    }
  } finally {
    batchParsing.value = false
  }
}

async function recognizeVoiceText() {
  const text = voiceText.value.trim()
  if (!text) {
    uni.showToast({ title: '请先说话或输入内容', icon: 'none' })
    return
  }
  voiceParsing.value = true
  try {
    const ok = await handleRecognizedText(text)
    if (ok) closeVoiceModal()
  } finally {
    voiceParsing.value = false
  }
}

function closeGuestModal() {
  showGuestModal.value = false
}

function onGuestNameInput(e: { detail: { value: string } }) {
  guestShopName.value = e.detail.value
}

async function handleSubmit() {
  if (cartStore.items.length === 0) {
    uni.showToast({ title: '请先选择商品', icon: 'none' })
    return
  }
  if (!userStore.customerId) {
    guestShopName.value = cartStore.guestShopName || userStore.nickname || ''
    showGuestModal.value = true
    return
  }
  await submitOrder()
}

async function confirmGuestSubmit() {
  const name = guestShopName.value.trim()
  if (!name) {
    uni.showToast({ title: '请输入店铺/客户名称', icon: 'none' })
    return
  }
  cartStore.setGuestShopName(name)
  showGuestModal.value = false
  await submitOrder(name)
}

async function submitOrder(customerName?: string) {
  submitting.value = true
  try {
    const payload = {
      items: cartStore.items.map((item) => ({
        productId: item.productId,
        orderQty: item.qty,
        unit: item.unit,
      })),
      ...(userStore.customerId ? {} : { customerName: customerName || cartStore.guestShopName }),
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

function goOrders() {
  uni.navigateTo({ url: '/pages/customer/orders/index' })
}

function goBind() {
  uni.navigateTo({ url: '/pages/customer/bind/index' })
}

function handleLogout() {
  userStore.signOut()
}

function updateMainHeight() {
  const { windowHeight } = uni.getSystemInfoSync()
  uni
    .createSelectorQuery()
    .select('.page-head')
    .boundingClientRect()
    .select('.boss-bottom-bar')
    .boundingClientRect()
    .exec((res) => {
      const headRect = res[0]
      const bottomRect = res[1]
      const headHeight = Array.isArray(headRect) ? headRect[0]?.height : headRect?.height
      const bottomHeight = Array.isArray(bottomRect) ? bottomRect[0]?.height : bottomRect?.height
      if (typeof headHeight !== 'number') return
      const footerHeight = typeof bottomHeight === 'number' ? bottomHeight : uni.upx2px(128)
      mainHeight.value = Math.max(0, windowHeight - headHeight - footerHeight)
    })
}
</script>

<style scoped lang="scss">
@import '../../../styles/boss-footer.scss';

.customer-order-page.boss-page {
  height: 100%;
  min-height: auto;
  max-height: 100%;
  overflow: hidden;
  background: #fff;
}

.page-head {
  flex-shrink: 0;
}

.main {
  flex: none;
  flex-direction: row;
  align-items: stretch;
  display: flex;
  min-height: 0;
  overflow: hidden;
  box-sizing: border-box;
}

.category-col {
  width: 168rpx;
  flex-shrink: 0;
  height: 100%;
  background: #f5f6f7;
}

.categories {
  width: 100%;
  height: 100%;
  background: #f5f6f7;
}

.banner {
  flex-shrink: 0;
  margin: 16rpx 24rpx 0;
  background: #0b7f3a;
  padding: 24rpx 28rpx;
  color: #fff;
  border-radius: 16rpx;
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
  font-size: 34rpx;
  font-weight: 700;
}

.link {
  padding: 8rpx 14rpx;
  font-size: 24rpx;
  color: #fff;
  background: rgba(255, 255, 255, 0.14);
  border-radius: 999rpx;
}

.hint {
  display: block;
  margin-top: 12rpx;
  font-size: 24rpx;
  line-height: 1.45;
  opacity: 0.92;
}

.bind-link {
  display: block;
  margin-top: 12rpx;
  font-size: 24rpx;
  font-weight: 600;
  text-decoration: underline;
}

.search-box {
  padding: 16rpx 24rpx;
  background: #f5f6f7;
  flex-shrink: 0;
}

.search-wrap {
  position: relative;
  display: flex;
  align-items: center;
}

.search-icon {
  position: absolute;
  left: 24rpx;
  font-size: 28rpx;
  color: #999;
  z-index: 1;
}

.search-input {
  flex: 1;
  height: 72rpx;
  padding: 0 64rpx;
  background: #fff;
  border-radius: 999rpx;
  font-size: 28rpx;
  color: #222;
  box-sizing: border-box;
}

.search-clear {
  position: absolute;
  right: 20rpx;
  width: 40rpx;
  height: 40rpx;
  line-height: 40rpx;
  text-align: center;
  font-size: 32rpx;
  color: #bbb;
}

.cat-item {
  padding: 32rpx 12rpx;
  font-size: 26rpx;
  color: #666;
  text-align: center;
  line-height: 1.3;
}

.cat-item.active {
  background: #fff;
  color: #07c160;
  font-weight: 600;
}

.product-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
  height: 100%;
  background: #fff;
}

.product-scroll {
  flex: none;
  height: 100%;
  width: 100%;
  background: #fff;
  box-sizing: border-box;
}

.product-panel.has-sub-cats .product-scroll {
  height: calc(100% - 82rpx);
}

.sub-cat-scroll {
  flex-shrink: 0;
  height: 82rpx;
  white-space: nowrap;
  background: #fff;
  border-bottom: 1rpx solid #f0f0f0;
}

.sub-cat-row {
  display: inline-flex;
  gap: 16rpx;
  padding: 16rpx 20rpx;
}

.sub-cat-chip {
  flex-shrink: 0;
  padding: 10rpx 24rpx;
  font-size: 26rpx;
  color: #666;
  background: #f5f6f7;
  border-radius: 999rpx;
}

.sub-cat-chip.active {
  color: #07c160;
  background: #e8f8ef;
  font-weight: 600;
}

.loading-wrap,
.empty-wrap {
  padding: 80rpx 24rpx;
  text-align: center;
}

.empty-title {
  display: block;
  font-size: 30rpx;
  color: #333;
  font-weight: 600;
}

.empty-hint {
  display: block;
  margin-top: 12rpx;
  font-size: 26rpx;
  color: #999;
}

.empty-btn {
  display: inline-block;
  margin-top: 24rpx;
  padding: 16rpx 40rpx;
  font-size: 28rpx;
  color: #07c160;
  border: 2rpx solid #07c160;
  border-radius: 999rpx;
}

.product-row {
  display: flex;
  align-items: center;
  padding: 24rpx;
  border-bottom: 1rpx solid #f0f0f0;
  gap: 16rpx;
  box-sizing: border-box;
}

.product-row.selected {
  background: #f8fcf9;
}

.product-thumb {
  flex-shrink: 0;
  width: 72rpx;
  height: 72rpx;
  border-radius: 14rpx;
  background: #f5f6f7;
}

.product-thumb-icon {
  flex-shrink: 0;
  width: 72rpx;
  height: 72rpx;
}

.product-info {
  flex: 1;
  min-width: 0;
}

.product-name {
  display: block;
  font-size: 32rpx;
  font-weight: 500;
  color: #222;
  line-height: 1.35;
}

.product-meta {
  display: block;
  margin-top: 8rpx;
  font-size: 26rpx;
  color: #999;
  line-height: 1.35;
}

.add-btn {
  position: relative;
  flex-shrink: 0;
  width: 60rpx;
  height: 60rpx;
  border-radius: 50%;
  background: #07c160;
  color: #fff;
  font-size: 44rpx;
  line-height: 56rpx;
  text-align: center;
}

.add-btn.picked {
  background: #05a850;
  box-shadow: 0 4rpx 12rpx rgba(7, 193, 96, 0.35);
}

.add-badge {
  font-size: 28rpx;
  font-weight: 700;
  line-height: 60rpx;
}

.batch-panel {
  padding: 0 32rpx calc(32rpx + env(safe-area-inset-bottom));
  background: #fff;
}

.batch-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 28rpx 0 20rpx;
}

.batch-close {
  width: 56rpx;
  font-size: 44rpx;
  color: #999;
  line-height: 1;
}

.batch-title {
  font-size: 34rpx;
  font-weight: 600;
  color: #222;
}

.batch-single {
  min-width: 120rpx;
  text-align: right;
  font-size: 28rpx;
  color: #2979ff;
}

.batch-textarea {
  width: 100%;
  min-height: 280rpx;
  padding: 24rpx;
  background: #f5f6f7;
  border-radius: 16rpx;
  font-size: 28rpx;
  color: #333;
  box-sizing: border-box;
}

.batch-recognize-btn {
  width: 100%;
  height: 88rpx;
  line-height: 88rpx;
  margin: 24rpx 0 0;
  padding: 0;
  background: #07c160;
  color: #fff;
  font-size: 32rpx;
  font-weight: 600;
  border-radius: 12rpx;
  border: none;
}

.batch-recognize-btn::after {
  border: none;
}

.voice-hint {
  display: block;
  margin-bottom: 16rpx;
  font-size: 26rpx;
  line-height: 1.5;
  color: #66736b;
}

.voice-textarea {
  min-height: 200rpx;
}

.guest-panel {
  width: 600rpx;
  padding: 40rpx 32rpx 32rpx;
  box-sizing: border-box;
}

.guest-title {
  display: block;
  font-size: 34rpx;
  font-weight: 600;
  color: #222;
  text-align: center;
}

.guest-input {
  width: 100%;
  height: 80rpx;
  margin-top: 28rpx;
  padding: 0 24rpx;
  background: #f5f6f7;
  border-radius: 12rpx;
  font-size: 28rpx;
  box-sizing: border-box;
}

.guest-actions {
  display: flex;
  gap: 20rpx;
  margin-top: 32rpx;
}

.guest-cancel,
.guest-confirm {
  flex: 1;
  height: 80rpx;
  line-height: 80rpx;
  margin: 0;
  padding: 0;
  font-size: 30rpx;
  border-radius: 12rpx;
  border: none;
}

.guest-cancel {
  background: #f5f6f7;
  color: #333;
}

.guest-cancel::after,
.guest-confirm::after {
  border: none;
}

.guest-confirm {
  background: #07c160;
  color: #fff;
  font-weight: 600;
}
</style>
