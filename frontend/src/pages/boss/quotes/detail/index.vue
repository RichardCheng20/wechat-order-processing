<template>
  <view class="page boss-page">
    <view class="top-bar">
      <view class="customer-head">
        <text class="customer-name">{{ customerName || '客户报价单' }}</text>
        <text class="customer-sub">定向差异价格，录价/下单时优先使用</text>
      </view>
      <view class="search-wrap">
        <input
          v-model="keyword"
          class="search-input"
          placeholder="搜索商品名称/别名"
          confirm-type="search"
          @confirm="reload"
        />
        <text v-if="keyword" class="search-clear" @tap="clearKeyword">×</text>
      </view>
      <view class="hint-row">
        <view class="only-quoted" :class="{ active: onlyQuoted }" @tap="toggleOnlyQuoted">
          仅已设价
        </view>
        <text v-if="changedCount > 0" class="changed-tip">已改 {{ changedCount }} 项</text>
      </view>
    </view>

    <view class="main" :style="mainStyle">
      <scroll-view scroll-y class="categories" :show-scrollbar="false">
        <view
          v-for="item in sidebarItems"
          :key="item.key"
          class="cat-item"
          :class="{ active: categoryFilter === item.key }"
          @tap="switchCategory(item.key)"
        >{{ item.label }}</view>
      </scroll-view>

      <view class="product-panel">
        <view v-if="!loading && visibleLines.length > 0" class="list-head">
          <text class="head-name">商品</text>
          <text class="head-base">基础价</text>
          <text class="head-quote">客户价</text>
        </view>

        <scroll-view scroll-y class="product-scroll" :show-scrollbar="false">
          <view v-if="loading" class="state-wrap">
            <u-loading-icon text="加载中" />
          </view>
          <view v-else-if="visibleLines.length === 0" class="state-wrap">
            <text class="empty-text">暂无商品</text>
          </view>
          <view v-else class="price-list">
            <view
              v-for="line in visibleLines"
              :key="line.productId"
              class="price-row"
              :class="{ changed: isChanged(line.productId), quoted: line.hasQuote }"
            >
              <view class="row-main">
                <text class="row-name">{{ line.productName }}</text>
                <text class="row-unit">{{ line.unit }}</text>
              </view>
              <view class="row-base">
                <text>{{ formatPrice(line.basePrice) }}</text>
              </view>
              <view class="row-input-wrap">
                <input
                  class="row-input"
                  type="digit"
                  :value="priceDrafts[line.productId] ?? ''"
                  placeholder="—"
                  placeholder-class="input-placeholder"
                  @input="(e) => onPriceInput(line.productId, e)"
                />
                <view class="copy-btn" @tap="copyBasePrice(line)">↻</view>
              </view>
            </view>
          </view>
        </scroll-view>
      </view>
    </view>

    <view class="boss-bottom-bar batch-bar">
      <button class="boss-secondary-btn back-btn" @tap="handleBack">返回</button>
      <button
        class="boss-primary-btn save-btn"
        :loading="saving"
        :disabled="changedCount === 0"
        @tap="saveAll"
      >
        保存{{ changedCount > 0 ? `(${changedCount})` : '' }}
      </button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { onLoad, onReady, onShow } from '@dcloudio/uni-app'
import { fetchBossCategories, type CategoryItem } from '../../../../api/product'
import {
  fetchCustomerQuoteDetail,
  saveCustomerQuote,
  type CustomerQuoteLine,
} from '../../../../api/quote'
import { buildPrimarySidebar, matchCategoryFilter } from '../../../../utils/category'
import { useUserStore } from '../../../../stores/user'

type UniInputEvent = Event & { detail?: { value?: string } }

const userStore = useUserStore()
const customerId = ref(0)
const customerName = ref('')
const lines = ref<CustomerQuoteLine[]>([])
const categories = ref<CategoryItem[]>([])
const priceDrafts = reactive<Record<number, string>>({})
const originalDrafts = reactive<Record<number, string>>({})
const loading = ref(false)
const saving = ref(false)
const keyword = ref('')
const onlyQuoted = ref(false)
const categoryFilter = ref('all')
const mainHeight = ref(0)

const sidebarItems = computed(() => buildPrimarySidebar(categories.value))
const mainStyle = computed(() => (mainHeight.value > 0 ? { height: `${mainHeight.value}px` } : {}))

const filteredLines = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  return lines.value.filter((line) => {
    if (!matchCategoryFilter(categories.value, line.categoryId, categoryFilter.value)) return false
    if (!kw) return true
    const hay = `${line.productName} ${line.categoryName || ''}`.toLowerCase()
    return hay.includes(kw)
  })
})

const visibleLines = computed(() => {
  if (!onlyQuoted.value) return filteredLines.value
  return filteredLines.value.filter((line) => hasDraft(line.productId) || line.hasQuote)
})

const changedCount = computed(() =>
  Object.keys(priceDrafts).filter((key) => {
    const id = Number(key)
    return (priceDrafts[id] ?? '') !== (originalDrafts[id] ?? '')
  }).length,
)

onLoad((query) => {
  customerId.value = Number(query?.customerId || 0)
})

onShow(async () => {
  if (!userStore.isLoggedIn || !userStore.isBoss) {
    uni.reLaunch({ url: '/pages/login/index' })
    return
  }
  if (!customerId.value) {
    uni.showToast({ title: '客户不存在', icon: 'none' })
    setTimeout(() => uni.navigateBack(), 500)
    return
  }
  if (!categories.value.length) {
    try {
      categories.value = await fetchBossCategories()
    } catch {
      categories.value = []
    }
  }
  await reload()
})

onReady(() => {
  const sys = uni.getSystemInfoSync()
  mainHeight.value = sys.windowHeight - uni.upx2px(320)
})

async function reload() {
  loading.value = true
  try {
    const data = await fetchCustomerQuoteDetail(customerId.value, {
      keyword: keyword.value.trim() || undefined,
      onlyQuoted: onlyQuoted.value,
    })
    customerName.value = data.customerName
    lines.value = data.lines || []
    syncDrafts(lines.value)
  } catch (err) {
    lines.value = []
    uni.showToast({ title: err instanceof Error ? err.message : '加载失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

function syncDrafts(items: CustomerQuoteLine[]) {
  Object.keys(priceDrafts).forEach((key) => delete priceDrafts[Number(key)])
  Object.keys(originalDrafts).forEach((key) => delete originalDrafts[Number(key)])
  for (const line of items) {
    const val = line.customerPrice != null ? String(line.customerPrice) : ''
    priceDrafts[line.productId] = val
    originalDrafts[line.productId] = val
  }
}

function switchCategory(key: string) {
  categoryFilter.value = key
}

function toggleOnlyQuoted() {
  onlyQuoted.value = !onlyQuoted.value
  reload()
}

function clearKeyword() {
  keyword.value = ''
  reload()
}

function hasDraft(productId: number) {
  const val = priceDrafts[productId]
  return val != null && val !== ''
}

function isChanged(productId: number) {
  return (priceDrafts[productId] ?? '') !== (originalDrafts[productId] ?? '')
}

function onPriceInput(productId: number, e: UniInputEvent) {
  priceDrafts[productId] = e.detail?.value ?? ''
}

function copyBasePrice(line: CustomerQuoteLine) {
  if (line.basePrice == null) {
    uni.showToast({ title: '暂无基础价', icon: 'none' })
    return
  }
  priceDrafts[line.productId] = String(line.basePrice)
}

function formatPrice(value?: number) {
  if (value == null) return '—'
  return Number(value).toFixed(2)
}

function handleBack() {
  uni.navigateBack()
}

async function saveAll() {
  if (changedCount.value === 0 || saving.value) return
  const items = Object.keys(priceDrafts)
    .map((key) => {
      const productId = Number(key)
      if ((priceDrafts[productId] ?? '') === (originalDrafts[productId] ?? '')) return null
      const raw = priceDrafts[productId]?.trim()
      return {
        productId,
        price: raw ? Number(raw) : null,
      }
    })
    .filter(Boolean) as { productId: number; price: number | null }[]

  if (!items.length) return
  saving.value = true
  try {
    const data = await saveCustomerQuote(customerId.value, items)
    customerName.value = data.customerName
    lines.value = data.lines || []
    syncDrafts(lines.value)
    uni.showToast({ title: '保存成功', icon: 'success' })
  } catch (err) {
    uni.showToast({ title: err instanceof Error ? err.message : '保存失败', icon: 'none' })
  } finally {
    saving.value = false
  }
}
</script>

<style scoped lang="scss">
.page {
  min-height: 100vh;
  background: #f5f6f8;
  padding-bottom: calc(120rpx + env(safe-area-inset-bottom));
}

.top-bar {
  background: #fff;
  padding: 20rpx 24rpx 16rpx;
  border-bottom: 1rpx solid #eee;
}

.customer-head {
  margin-bottom: 16rpx;
}

.customer-name {
  display: block;
  font-size: 34rpx;
  font-weight: 700;
  color: #17211b;
}

.customer-sub {
  display: block;
  margin-top: 6rpx;
  font-size: 24rpx;
  color: #66736b;
}

.search-wrap {
  display: flex;
  align-items: center;
  height: 72rpx;
  padding: 0 20rpx;
  background: #f5f6f8;
  border-radius: 12rpx;
}

.search-input {
  flex: 1;
  font-size: 28rpx;
}

.search-clear {
  font-size: 36rpx;
  color: #999;
}

.hint-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 12rpx;
}

.only-quoted {
  padding: 8rpx 20rpx;
  font-size: 24rpx;
  color: #66736b;
  background: #f5f6f8;
  border-radius: 8rpx;
}

.only-quoted.active {
  color: #0b7f3a;
  background: #ecfdf3;
}

.changed-tip {
  font-size: 24rpx;
  color: #0b7f3a;
}

.main {
  display: flex;
  min-height: 500rpx;
}

.categories {
  width: 168rpx;
  flex-shrink: 0;
  background: #fafafa;
  border-right: 1rpx solid #eee;
}

.cat-item {
  padding: 28rpx 12rpx;
  text-align: center;
  font-size: 26rpx;
  color: #66736b;
  border-bottom: 1rpx solid #f0f0f0;
}

.cat-item.active {
  background: #fff;
  color: #0b7f3a;
  font-weight: 600;
  border-left: 6rpx solid #0b7f3a;
}

.product-panel {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.list-head,
.price-row {
  display: flex;
  align-items: center;
  padding: 16rpx 12rpx;
}

.list-head {
  background: #f3f4f6;
  font-size: 22rpx;
  color: #66736b;
}

.head-name,
.row-main {
  flex: 1.1;
  min-width: 0;
}

.head-base,
.row-base {
  width: 120rpx;
  text-align: center;
  font-size: 24rpx;
  color: #66736b;
}

.head-quote,
.row-input-wrap {
  width: 180rpx;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8rpx;
}

.product-scroll {
  flex: 1;
  height: 0;
}

.price-row {
  border-bottom: 1rpx solid #f0f0f0;
  background: #fff;
}

.price-row.changed {
  background: #f0fdf4;
}

.price-row.quoted .row-name {
  color: #0b7f3a;
}

.row-name {
  display: block;
  font-size: 28rpx;
  color: #17211b;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.row-unit {
  display: block;
  margin-top: 4rpx;
  font-size: 22rpx;
  color: #94a3b8;
}

.row-input {
  width: 112rpx;
  height: 60rpx;
  padding: 0 8rpx;
  text-align: center;
  font-size: 28rpx;
  background: #fff;
  border: 1rpx solid #ddd;
  border-radius: 8rpx;
}

.copy-btn {
  width: 48rpx;
  height: 48rpx;
  line-height: 48rpx;
  text-align: center;
  font-size: 28rpx;
  color: #2979ff;
  background: #eef5ff;
  border-radius: 8rpx;
}

.state-wrap {
  padding: 80rpx 0;
  text-align: center;
}

.empty-text {
  color: #94a3b8;
  font-size: 28rpx;
}

.batch-bar {
  display: flex;
  gap: 16rpx;
}

.back-btn,
.save-btn {
  flex: 1;
}
</style>
