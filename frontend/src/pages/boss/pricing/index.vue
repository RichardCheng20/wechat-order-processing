<template>
  <view class="page" :class="{ 'with-keypad': showKeypad }">
    <view class="search-bar">
      <view class="search-input-wrap">
        <AppIcon class="search-icon" name="search" tone="green" :tile="false" :size="19" />
        <input
          v-model="keyword"
          class="search-input"
          placeholder="搜索商品名称"
          confirm-type="search"
          @confirm="reloadProducts"
        />
      </view>
    </view>

    <view class="tab-row">
      <view
        v-for="tab in priceTabs"
        :key="tab.value"
        class="tab-item"
        :class="{ active: priceFilter === tab.value }"
        @tap="switchPriceTab(tab.value)"
      >
        {{ tab.label }}
      </view>
    </view>

    <view class="date-row">
      <view class="date-range">
        <text>配送：{{ dateRangeLabel }}</text>
      </view>
      <view class="date-quick">
        <view class="quick-btn" :class="{ active: quickDate === 'today' }" @tap="setToday">今日</view>
        <view class="quick-btn" :class="{ active: quickDate === 'tomorrow' }" @tap="setTomorrow">明日</view>
      </view>
    </view>

    <view v-if="loading" class="loading-wrap">
      <u-loading-icon text="加载中" />
    </view>

    <view v-else-if="products.length === 0" class="empty-wrap">
      <u-empty mode="order" text="暂无待录价商品" />
    </view>

    <view v-else class="workspace">
      <scroll-view scroll-y class="product-sidebar">
        <view
          v-for="item in products"
          :key="item.productId"
          class="product-item"
          :class="{ active: selectedProductId === item.productId }"
          @tap="selectProduct(item.productId)"
        >
          <text class="product-name">{{ item.productName }}</text>
          <view v-if="item.pendingCount > 0" class="badge">{{ item.pendingCount }}</view>
        </view>
      </scroll-view>

      <view class="detail-panel">
        <view v-if="detailLoading" class="detail-loading">
          <u-loading-icon text="加载中" size="20" />
        </view>

        <view v-else-if="!detail" class="detail-empty">
          <text>请选择左侧商品</text>
        </view>

        <template v-else>
          <view class="detail-head">
            <text class="summary">
              共{{ detail.orderCount }}笔订单，{{ formatQty(detail.totalQty) }}{{ detail.unit }}
            </text>
            <view class="head-actions">
              <view class="action-btn outline" @tap="handleFetchPrices">获取价格</view>
              <view class="action-btn primary" @tap="handleSubmit">录价</view>
            </view>
          </view>

          <scroll-view scroll-y class="line-list">
            <view v-for="line in detail.lines" :key="line.itemId" class="line-card">
              <view class="line-top">
                <text class="customer-name">{{ line.customerName || '未知客户' }}</text>
                <text v-if="line.priced" class="priced-tag">已录</text>
              </view>
              <text class="line-meta">配送：{{ formatDelivery(line.deliveryDate) }}</text>
              <text class="line-meta remark">备注：{{ displayLineRemark(line) }}</text>
              <view class="line-bottom">
                <text class="qty">{{ formatQty(line.quantity) }}{{ line.unit }}</text>
                <view class="price-input-wrap">
                  <view
                    class="price-input"
                    :class="{ active: activeLine?.itemId === line.itemId && showKeypad }"
                    @tap="focusLine(line)"
                  >
                    <text v-if="displayPrice(line.itemId)" class="price-value">{{ displayPrice(line.itemId) }}</text>
                    <text v-else class="price-placeholder">单价</text>
                  </view>
                  <text class="unit-label">元/{{ line.unit }}</text>
                  <view class="ref-btn" @tap.stop="applyReference(line)">↻</view>
                </view>
              </view>
            </view>
          </scroll-view>
        </template>
      </view>
    </view>

    <BossTabbar v-if="!showKeypad" active="pricing" />

    <view v-if="showKeypad" class="keypad-wrap">
      <view class="keypad-toggle" @tap="closeKeypad">⌄</view>
      <view class="keypad-body">
        <view class="keypad-main">
          <view class="key-row">
            <view class="key" @tap="inputKey('1')">1</view>
            <view class="key" @tap="inputKey('2')">2</view>
            <view class="key" @tap="inputKey('3')">3</view>
            <view class="key fn" @tap="backspace">⌫</view>
          </view>
          <view class="key-row">
            <view class="key" @tap="inputKey('4')">4</view>
            <view class="key" @tap="inputKey('5')">5</view>
            <view class="key" @tap="inputKey('6')">6</view>
          </view>
          <view class="key-row">
            <view class="key" @tap="inputKey('7')">7</view>
            <view class="key" @tap="inputKey('8')">8</view>
            <view class="key" @tap="inputKey('9')">9</view>
          </view>
          <view class="key-row">
            <view class="key wide" @tap="inputKey('0')">0</view>
            <view class="key" @tap="inputKey('.')">.</view>
            <view class="key fn" @tap="clearDraft">清零</view>
          </view>
        </view>
        <view class="keypad-side">
          <view class="key confirm-side" @tap="confirmKeypad">确定</view>
        </view>
      </view>
    </view>

    <view v-if="submitStatus !== 'idle'" class="submit-overlay" @tap.stop>
      <view class="submit-box">
        <template v-if="submitStatus === 'submitting'">
          <u-loading-icon mode="circle" size="28" />
          <text class="submit-text">正在录价</text>
        </template>
        <template v-else>
          <text class="success-icon">✓</text>
          <text class="submit-text success">录价成功</text>
        </template>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import {
  displayLineRemark,
  fetchPricingProducts,
  fetchProductPricingDetail,
  fetchProductReferencePrices,
  submitProductPricing,
  type PricingProductDetail,
  type PricingProductLine,
  type PricingProductSummary,
} from '../../../api/pricing'
import AppIcon from '../../../components/AppIcon.vue'
import BossTabbar from '../../../components/boss-tabbar/index.vue'
import { useUserStore } from '../../../stores/user'

const userStore = useUserStore()
const products = ref<PricingProductSummary[]>([])
const detail = ref<PricingProductDetail | null>(null)
const selectedProductId = ref(0)
const loading = ref(false)
const detailLoading = ref(false)
const submitting = ref(false)
const keyword = ref('')
const priceFilter = ref<'ALL' | 'UNPRICED' | 'PRICED'>('ALL')
const quickDate = ref<'week' | 'today' | 'tomorrow'>('week')
const deliveryFrom = ref('')
const deliveryTo = ref('')
const priceMap = reactive<Record<number, string>>({})
const showKeypad = ref(false)
const keyboardDraft = ref('')
const activeLine = ref<PricingProductLine | null>(null)
const submitStatus = ref<'idle' | 'submitting' | 'success'>('idle')

const priceTabs = [
  { label: '全部商品', value: 'ALL' as const },
  { label: '未录完', value: 'UNPRICED' as const },
  { label: '已录完', value: 'PRICED' as const },
]

const dateRangeLabel = computed(() => {
  if (!deliveryFrom.value || !deliveryTo.value) return '—'
  return `${formatMonthDay(deliveryFrom.value)}至${formatMonthDay(deliveryTo.value)}`
})

const queryOptions = computed(() => ({
  keyword: keyword.value.trim() || undefined,
  priceFilter: priceFilter.value,
  deliveryFrom: deliveryFrom.value || undefined,
  deliveryTo: deliveryTo.value || undefined,
}))

onShow(async () => {
  if (!userStore.isLoggedIn || !userStore.isBoss) {
    uni.reLaunch({ url: '/pages/login/index' })
    return
  }
  if (!deliveryFrom.value) {
    applyWeekRange(new Date())
  }
  await reloadProducts()
})

async function refreshSidebar() {
  try {
    products.value = await fetchPricingProducts(queryOptions.value)
  } catch {
    // ignore sidebar refresh errors
  }
}

async function reloadProducts() {
  loading.value = true
  try {
    products.value = await fetchPricingProducts(queryOptions.value)
    if (products.value.length === 0) {
      selectedProductId.value = 0
      detail.value = null
      return
    }
    const stillExists = products.value.some((p) => p.productId === selectedProductId.value)
    if (!stillExists) {
      selectedProductId.value = products.value[0].productId
    }
    await loadDetail(selectedProductId.value)
  } catch (err) {
    products.value = []
    detail.value = null
    uni.showToast({ title: err instanceof Error ? err.message : '加载失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

async function loadDetail(productId: number) {
  if (!productId) return
  detailLoading.value = true
  try {
    detail.value = await fetchProductPricingDetail(productId, queryOptions.value)
    syncPriceMap(detail.value.lines)
  } catch (err) {
    detail.value = null
    uni.showToast({ title: err instanceof Error ? err.message : '加载明细失败', icon: 'none' })
  } finally {
    detailLoading.value = false
  }
}

function syncPriceMap(lines: PricingProductLine[]) {
  Object.keys(priceMap).forEach((key) => delete priceMap[Number(key)])
  for (const line of lines) {
    priceMap[line.itemId] = line.dealPrice != null ? String(line.dealPrice) : ''
  }
}

function selectProduct(productId: number) {
  if (selectedProductId.value === productId) return
  closeKeypad()
  selectedProductId.value = productId
  loadDetail(productId)
}

function switchPriceTab(value: 'ALL' | 'UNPRICED' | 'PRICED') {
  if (priceFilter.value === value) return
  priceFilter.value = value
  reloadProducts()
}

function applyWeekRange(base: Date) {
  const range = getWeekRange(base)
  deliveryFrom.value = range.from
  deliveryTo.value = range.to
  quickDate.value = 'week'
}

function setToday() {
  const today = formatDate(new Date())
  deliveryFrom.value = today
  deliveryTo.value = today
  quickDate.value = 'today'
  reloadProducts()
}

function setTomorrow() {
  const d = new Date()
  d.setDate(d.getDate() + 1)
  const tomorrow = formatDate(d)
  deliveryFrom.value = tomorrow
  deliveryTo.value = tomorrow
  quickDate.value = 'tomorrow'
  reloadProducts()
}

function applyReference(line: PricingProductLine) {
  if (line.referencePrice == null) {
    uni.showToast({ title: '暂无参考价', icon: 'none' })
    return
  }
  priceMap[line.itemId] = String(line.referencePrice)
  if (activeLine.value?.itemId === line.itemId) {
    keyboardDraft.value = String(line.referencePrice)
  }
}

function displayPrice(itemId: number) {
  if (activeLine.value?.itemId === itemId && showKeypad.value) {
    return keyboardDraft.value
  }
  return priceMap[itemId] ?? ''
}

function focusLine(line: PricingProductLine) {
  activeLine.value = line
  keyboardDraft.value = priceMap[line.itemId] ?? ''
  showKeypad.value = true
}

function closeKeypad() {
  if (activeLine.value) {
    priceMap[activeLine.value.itemId] = keyboardDraft.value
  }
  showKeypad.value = false
  activeLine.value = null
  keyboardDraft.value = ''
}

function inputKey(key: string) {
  if (!activeLine.value) return
  let val = keyboardDraft.value
  if (key === '.' && val.includes('.')) return
  if (val === '0' && key !== '.') {
    val = key
  } else {
    val += key
  }
  keyboardDraft.value = val
  priceMap[activeLine.value.itemId] = val
}

function backspace() {
  if (!activeLine.value) return
  keyboardDraft.value = keyboardDraft.value.slice(0, -1)
  priceMap[activeLine.value.itemId] = keyboardDraft.value
}

function clearDraft() {
  if (!activeLine.value) return
  keyboardDraft.value = ''
  priceMap[activeLine.value.itemId] = ''
}

async function confirmKeypad() {
  if (!activeLine.value || !selectedProductId.value || submitting.value) return
  const dealPrice = Number(keyboardDraft.value)
  if (!dealPrice || dealPrice <= 0) {
    uni.showToast({ title: '请输入有效单价', icon: 'none' })
    return
  }

  const currentLine = activeLine.value
  submitStatus.value = 'submitting'
  submitting.value = true
  try {
    detail.value = await submitProductPricing(
      selectedProductId.value,
      [{ itemId: currentLine.itemId, dealPrice }],
      queryOptions.value,
    )
    syncPriceMap(detail.value.lines)
    submitStatus.value = 'success'
    await refreshSidebar()
    setTimeout(() => {
      submitStatus.value = 'idle'
      moveToNextLine(currentLine.itemId)
    }, 900)
  } catch (err) {
    submitStatus.value = 'idle'
    uni.showToast({ title: err instanceof Error ? err.message : '录价失败', icon: 'none' })
  } finally {
    submitting.value = false
  }
}

function moveToNextLine(currentItemId: number) {
  const lines = detail.value?.lines || []
  const idx = lines.findIndex((line) => line.itemId === currentItemId)
  const next = lines.slice(idx + 1).find((line) => !line.priced) || lines.find((line) => !line.priced)
  if (next) {
    focusLine(next)
    return
  }
  closeKeypad()
}

async function handleFetchPrices() {
  if (!selectedProductId.value) return
  detailLoading.value = true
  try {
    detail.value = await fetchProductReferencePrices(selectedProductId.value, queryOptions.value)
    syncPriceMap(detail.value.lines)
    uni.showToast({ title: '已填入参考价', icon: 'success' })
  } catch (err) {
    uni.showToast({ title: err instanceof Error ? err.message : '获取失败', icon: 'none' })
  } finally {
    detailLoading.value = false
  }
}

async function handleSubmit() {
  if (!detail.value?.lines.length || submitting.value) return
  closeKeypad()
  const items = detail.value.lines
    .map((line) => ({
      itemId: line.itemId,
      dealPrice: Number(priceMap[line.itemId]),
    }))
    .filter((item) => item.dealPrice > 0)

  if (items.length === 0) {
    uni.showToast({ title: '请至少填写一条单价', icon: 'none' })
    return
  }

  submitting.value = true
  submitStatus.value = 'submitting'
  try {
    detail.value = await submitProductPricing(selectedProductId.value, items, queryOptions.value)
    syncPriceMap(detail.value.lines)
    await reloadProducts()
    submitStatus.value = 'success'
    setTimeout(() => {
      submitStatus.value = 'idle'
    }, 900)
  } catch (err) {
    submitStatus.value = 'idle'
    uni.showToast({ title: err instanceof Error ? err.message : '录价失败', icon: 'none' })
  } finally {
    submitting.value = false
  }
}

function formatDate(date: Date) {
  const y = date.getFullYear()
  const m = `${date.getMonth() + 1}`.padStart(2, '0')
  const d = `${date.getDate()}`.padStart(2, '0')
  return `${y}-${m}-${d}`
}

function formatMonthDay(value: string) {
  const parts = value.split('-')
  if (parts.length < 3) return value
  return `${parts[1]}-${parts[2]}`
}

function formatDelivery(value?: string) {
  if (!value) return '—'
  return formatMonthDay(value)
}

function formatQty(value?: number) {
  if (value == null) return '0'
  return Number(value).toFixed(2).replace(/\.?0+$/, '') || '0'
}

function getWeekRange(base: Date) {
  const d = new Date(base)
  const day = d.getDay() === 0 ? 7 : d.getDay()
  const monday = new Date(d)
  monday.setDate(d.getDate() - day + 1)
  const sunday = new Date(monday)
  sunday.setDate(monday.getDate() + 6)
  return { from: formatDate(monday), to: formatDate(sunday) }
}
</script>

<style scoped lang="scss">
.page {
  min-height: 100vh;
  background: #f5f6f8;
  padding-bottom: calc(140rpx + env(safe-area-inset-bottom));
}

.page.with-keypad {
  padding-bottom: calc(420rpx + env(safe-area-inset-bottom));
}

.search-bar {
  padding: 16rpx 24rpx;
  background: #fff;
}

.search-input-wrap {
  display: flex;
  align-items: center;
  height: 72rpx;
  padding: 0 24rpx;
  background: #f5f6f8;
  border-radius: 36rpx;
}

.search-icon {
  margin-right: 12rpx;
  width: 42rpx;
  height: 42rpx;
  border-radius: 12rpx;
  background: #eef2ed;
}

.search-input {
  flex: 1;
  font-size: 28rpx;
}

.tab-row {
  display: flex;
  gap: 40rpx;
  padding: 0 24rpx;
  background: #fff;
  border-bottom: 1rpx solid #eee;
}

.tab-item {
  position: relative;
  padding: 24rpx 0;
  font-size: 30rpx;
  color: #666;
}

.tab-item.active {
  color: #111;
  font-weight: 600;
}

.tab-item.active::after {
  content: '';
  position: absolute;
  left: 50%;
  bottom: 0;
  width: 48rpx;
  height: 6rpx;
  margin-left: -24rpx;
  background: #22c55e;
  border-radius: 3rpx;
}

.date-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20rpx 24rpx;
  background: #fff;
  margin-bottom: 16rpx;
  font-size: 28rpx;
}

.date-quick {
  display: flex;
  gap: 12rpx;
}

.quick-btn {
  padding: 10rpx 24rpx;
  font-size: 26rpx;
  color: #666;
  background: #f5f6f8;
  border-radius: 8rpx;
}

.quick-btn.active {
  color: #22c55e;
  background: #ecfdf3;
}

.loading-wrap,
.empty-wrap {
  padding: 120rpx 0;
  text-align: center;
}

.workspace {
  display: flex;
  height: calc(100vh - 320rpx - 140rpx - env(safe-area-inset-bottom));
  min-height: 500rpx;
  margin: 0 16rpx;
  background: #fff;
  border-radius: 16rpx;
  overflow: hidden;
}

.product-sidebar {
  width: 200rpx;
  flex-shrink: 0;
  background: #fafafa;
  border-right: 1rpx solid #eee;
}

.product-item {
  position: relative;
  padding: 28rpx 16rpx;
  border-bottom: 1rpx solid #f0f0f0;
}

.product-item.active {
  background: #fff;
  border-left: 6rpx solid #22c55e;
}

.product-name {
  display: block;
  font-size: 28rpx;
  color: #333;
  line-height: 1.4;
  padding-right: 36rpx;
}

.product-item.active .product-name {
  color: #22c55e;
  font-weight: 600;
}

.badge {
  position: absolute;
  top: 20rpx;
  right: 12rpx;
  min-width: 32rpx;
  height: 32rpx;
  padding: 0 8rpx;
  line-height: 32rpx;
  text-align: center;
  font-size: 20rpx;
  color: #fff;
  background: #f59e0b;
  border-radius: 16rpx;
}

.detail-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.detail-loading,
.detail-empty {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #999;
  font-size: 28rpx;
}

.detail-head {
  padding: 20rpx 24rpx;
  border-bottom: 1rpx solid #f0f0f0;
}

.summary {
  display: block;
  font-size: 28rpx;
  color: #333;
  margin-bottom: 16rpx;
}

.head-actions {
  display: flex;
  gap: 16rpx;
}

.action-btn {
  padding: 12rpx 28rpx;
  font-size: 26rpx;
  border-radius: 8rpx;
}

.action-btn.outline {
  color: #333;
  border: 1rpx solid #ddd;
  background: #fff;
}

.action-btn.primary {
  color: #fff;
  background: #22c55e;
}

.line-list {
  flex: 1;
  height: 0;
  padding: 16rpx;
}

.line-card {
  padding: 24rpx;
  margin-bottom: 16rpx;
  background: #fafafa;
  border-radius: 12rpx;
}

.line-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.customer-name {
  font-size: 32rpx;
  font-weight: 600;
  color: #111;
}

.priced-tag {
  font-size: 22rpx;
  color: #22c55e;
}

.line-meta {
  display: block;
  margin-top: 12rpx;
  font-size: 26rpx;
  color: #666;
}

.line-meta.remark {
  color: #e67e22;
}

.line-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 20rpx;
}

.qty {
  font-size: 30rpx;
  font-weight: 600;
  color: #111;
}

.price-input-wrap {
  display: flex;
  align-items: center;
  gap: 8rpx;
}

.price-input {
  min-width: 120rpx;
  height: 64rpx;
  padding: 0 16rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
  border: 2rpx solid #ddd;
  border-radius: 8rpx;
}

.price-input.active {
  border-color: #22c55e;
  background: #ecfdf3;
}

.price-value {
  font-size: 30rpx;
  color: #111;
}

.price-placeholder {
  font-size: 26rpx;
  color: #bbb;
}

.unit-label {
  font-size: 24rpx;
  color: #666;
}

.ref-btn {
  width: 56rpx;
  height: 56rpx;
  line-height: 56rpx;
  text-align: center;
  font-size: 32rpx;
  color: #2979ff;
  background: #eef5ff;
  border-radius: 8rpx;
}

.keypad-wrap {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 200;
  background: #eef0f2;
  border-top: 1rpx solid #ddd;
  padding-bottom: env(safe-area-inset-bottom);
}

.keypad-toggle {
  text-align: center;
  padding: 12rpx;
  color: #999;
  font-size: 28rpx;
}

.keypad-body {
  display: flex;
  gap: 8rpx;
  padding: 0 12rpx 12rpx;
}

.keypad-main {
  flex: 3;
}

.keypad-side {
  flex: 1;
  display: flex;
}

.key-row {
  display: flex;
  gap: 8rpx;
  margin-bottom: 8rpx;
}

.key-row:last-child {
  margin-bottom: 0;
}

.key {
  flex: 1;
  height: 96rpx;
  line-height: 96rpx;
  text-align: center;
  background: #fff;
  border-radius: 8rpx;
  font-size: 36rpx;
  font-weight: 500;
}

.key.wide {
  flex: 2.05;
}

.key.fn {
  font-size: 28rpx;
  color: #666;
}

.confirm-side {
  flex: 1;
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #22c55e;
  color: #fff;
  font-size: 32rpx;
  font-weight: 600;
  border-radius: 8rpx;
}

.submit-overlay {
  position: fixed;
  inset: 0;
  z-index: 300;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.35);
}

.submit-box {
  min-width: 280rpx;
  padding: 48rpx 56rpx;
  background: #fff;
  border-radius: 16rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20rpx;
}

.submit-text {
  font-size: 30rpx;
  color: #333;
}

.submit-text.success {
  color: #22c55e;
  font-weight: 600;
}

.success-icon {
  width: 72rpx;
  height: 72rpx;
  line-height: 72rpx;
  text-align: center;
  font-size: 40rpx;
  color: #fff;
  background: #22c55e;
  border-radius: 50%;
}
</style>
