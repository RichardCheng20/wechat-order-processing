<template>
  <view class="page">
    <view class="top-bar">
      <view class="filter-row">
        <view class="quick-tabs">
          <text class="quick-tab" :class="{ active: presetDays === 7 }" @tap="applyPreset(7)">近7日</text>
          <text class="quick-tab" :class="{ active: presetDays === 30 }" @tap="applyPreset(30)">近30日</text>
        </view>
        <view class="calendar-btn" @tap="openRangePicker">
          <AppIcon name="order" tone="gray" :size="18" :tile="false" />
        </view>
      </view>
      <view v-if="dateFrom && dateTo" class="range-hint">
        <text>{{ dateFrom }} 至 {{ dateTo }} · 进/销按区间 · 存/可用为当前</text>
      </view>
      <view class="search-wrap">
        <AppIcon class="search-icon" name="search" tone="green" :tile="false" :size="18" />
        <input
          v-model="keyword"
          class="search-input"
          placeholder="搜索商品"
          confirm-type="search"
        />
      </view>
      <view v-if="summary" class="summary-bar">
        <text>共 {{ summary.productCount }} 种 · 进货 {{ formatQty(summary.inboundQty) }} · 销货 {{ formatQty(summary.outboundQty) }}</text>
      </view>
    </view>

    <view v-if="loading" class="state-wrap page-state">
      <u-loading-icon text="加载中" />
    </view>

    <view v-else class="main">
      <scroll-view scroll-y class="categories" :show-scrollbar="false">
        <view
          v-for="item in sidebarItems"
          :key="item.key"
          class="cat-item"
          :class="{ active: categoryFilter === item.key }"
          @tap="switchCategory(item.key)"
        >{{ item.label }}</view>
      </scroll-view>

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
              @tap="subCategoryFilter = tab.key"
            >{{ tab.label }}</text>
          </view>
        </scroll-view>

        <view v-if="displayItems.length === 0" class="state-wrap">
          <u-empty mode="data" text="暂无商品" />
        </view>

        <scroll-view v-else scroll-y class="product-scroll" :show-scrollbar="false">
          <view
            v-for="row in displayItems"
            :key="row.productId"
            class="card"
            :class="{ idle: !hasActivity(row) }"
            @tap="goProduct(row)"
          >
            <view class="card-head">
              <text class="name">{{ row.productName }}</text>
              <text v-if="row.unit" class="unit-tag">{{ row.unit }}</text>
            </view>
            <view class="metric-row">
              <view class="metric">
                <text class="metric-label">进货</text>
                <text class="metric-value inbound">{{ formatQty(row.inboundQty) }}</text>
              </view>
              <view class="metric">
                <text class="metric-label">销货</text>
                <text class="metric-value outbound">{{ formatQty(row.outboundQty) }}</text>
              </view>
              <view class="metric">
                <text class="metric-label">现存</text>
                <text class="metric-value stock">{{ formatQty(row.stockQty) }}</text>
              </view>
              <view class="metric highlight">
                <text class="metric-label">可用</text>
                <text class="metric-value available">{{ formatQty(row.availableQty) }}</text>
              </view>
            </view>
          </view>
          <view class="list-tail">—— 没有更多了 ——</view>
        </scroll-view>
      </view>
    </view>

    <OrderDateRangePicker
      :show="rangePickerVisible"
      :start-date="dateFrom"
      :end-date="dateTo"
      @close="rangePickerVisible = false"
      @confirm="onRangeConfirm"
    />
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import {
  fetchBossInventoryReport,
  type InventoryReportRow,
  type InventoryReportSummary,
} from '@common/api/dashboard'
import { fetchBossCategories, type CategoryItem } from '@common/api/product'
import AppIcon from '@/components/AppIcon.vue'
import OrderDateRangePicker from '@/components/OrderDateRangePicker.vue'
import { getLastNDaysRange } from '@common/utils/date-range'
import { guardOwnerAdminPage } from '@common/utils/boss-access'
import { buildPrimarySidebar, getParentCategory, matchCategoryFilter } from '@common/utils/category'

const loading = ref(false)
const presetDays = ref(7)
const dateFrom = ref('')
const dateTo = ref('')
const rangePickerVisible = ref(false)
const summary = ref<InventoryReportSummary | null>(null)
const rows = ref<InventoryReportRow[]>([])
const categories = ref<CategoryItem[]>([])
const keyword = ref('')
const categoryFilter = ref('all')
const subCategoryFilter = ref('all')

const sidebarItems = computed(() => buildPrimarySidebar(categories.value))
const activeParent = computed(() => getParentCategory(categories.value, categoryFilter.value))
const subCategoryTabs = computed(() => {
  const parent = activeParent.value
  if (!parent?.children?.length) return []
  return [{ key: 'all', label: '全部' }, ...parent.children.map((c) => ({ key: String(c.id), label: c.name }))]
})

const displayItems = computed(() => {
  let items = rows.value
  const kw = keyword.value.trim().toLowerCase()
  if (kw) {
    items = items.filter((item) => item.productName.toLowerCase().includes(kw))
  }
  if (categoryFilter.value === 'uncategorized') {
    items = items.filter((item) => !item.categoryId)
  } else if (categoryFilter.value !== 'all') {
    if (subCategoryFilter.value !== 'all') {
      const catId = Number(subCategoryFilter.value)
      items = items.filter((item) => item.categoryId === catId)
    } else {
      items = items.filter((item) => matchCategoryFilter(item.categoryId, categoryFilter.value, categories.value))
    }
  }
  return items
})

onShow(async () => {
  if (!(await guardOwnerAdminPage())) return
  try {
    categories.value = await fetchBossCategories()
  } catch {
    categories.value = []
  }
  if (!dateFrom.value) {
    applyPreset(7)
    return
  }
  await loadData()
})

function applyPreset(days: number) {
  presetDays.value = days
  const range = getLastNDaysRange(days)
  dateFrom.value = range.from
  dateTo.value = range.to
  loadData()
}

function openRangePicker() {
  rangePickerVisible.value = true
}

function onRangeConfirm(payload: { from: string; to: string }) {
  rangePickerVisible.value = false
  dateFrom.value = payload.from
  dateTo.value = payload.to
  presetDays.value = 0
  loadData()
}

function switchCategory(key: string) {
  categoryFilter.value = key
  subCategoryFilter.value = 'all'
}

async function loadData() {
  if (!dateFrom.value || !dateTo.value) return
  loading.value = true
  try {
    const data = await fetchBossInventoryReport({
      dateFrom: dateFrom.value,
      dateTo: dateTo.value,
      dateType: 'DELIVERY',
    })
    summary.value = data.summary
    rows.value = data.rows || []
  } catch {
    summary.value = null
    rows.value = []
  } finally {
    loading.value = false
  }
}

function hasActivity(row: InventoryReportRow) {
  return (row.inboundQty || 0) > 0 || (row.outboundQty || 0) > 0
}

function goProduct(row: InventoryReportRow) {
  if (!row.productId) return
  uni.navigateTo({ url: `/pages/boss/products/form?id=${row.productId}` })
}

function formatQty(value?: number) {
  if (value == null) return '0'
  return Number(value).toFixed(2).replace(/\.?0+$/, '') || '0'
}
</script>

<style scoped lang="scss">
@import '../../../styles/boss-ui.scss';

.page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: $boss-bg;
  overflow: hidden;
}

.top-bar {
  flex-shrink: 0;
  background: $boss-surface;
  padding: 16rpx 20rpx 0;
}

.filter-row {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-bottom: 8rpx;
}

.quick-tabs {
  flex: 1;
  display: flex;
  justify-content: center;
  gap: 48rpx;
}

.quick-tab {
  font-size: 28rpx;
  color: #66736b;
  padding-bottom: 8rpx;
}

.quick-tab.active {
  color: #1677ff;
  font-weight: 700;
  box-shadow: inset 0 -4rpx 0 #1677ff;
}

.calendar-btn {
  width: 64rpx;
  height: 64rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f6f7;
  border-radius: 12rpx;
  flex-shrink: 0;
}

.range-hint {
  padding: 8rpx 4rpx 12rpx;
  font-size: 22rpx;
  color: #94a3b8;
}

.search-wrap {
  display: flex;
  align-items: center;
  height: 72rpx;
  padding: 0 20rpx;
  margin-bottom: 12rpx;
  background: $boss-bg;
  border-radius: 36rpx;
}

.search-icon {
  margin-right: 10rpx;
}

.search-input {
  flex: 1;
  font-size: 28rpx;
}

.summary-bar {
  margin: 0 -20rpx;
  padding: 16rpx 20rpx;
  background: #eef4ff;
  color: #1677ff;
  font-size: 26rpx;
  border-top: 1rpx solid #dbeafe;
}

.page-state {
  flex: 1;
}

.main {
  flex: 1;
  display: flex;
  min-height: 0;
  margin: 16rpx;
  background: $boss-surface;
  border-radius: 16rpx;
  overflow: hidden;
}

.categories {
  width: 168rpx;
  flex-shrink: 0;
  background: #fafafa;
  border-right: 1rpx solid #eee;
}

.cat-item {
  padding: 28rpx 12rpx;
  font-size: 26rpx;
  color: #666;
  text-align: center;
  border-bottom: 1rpx solid #f0f0f0;
}

.cat-item.active {
  background: #fff;
  color: $boss-green-deep;
  font-weight: 600;
  border-left: 6rpx solid $boss-green;
}

.product-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.sub-cat-scroll {
  flex-shrink: 0;
  white-space: nowrap;
  border-bottom: 1rpx solid #f0f0f0;
}

.sub-cat-row {
  display: inline-flex;
  padding: 12rpx 16rpx;
  gap: 12rpx;
}

.sub-cat-chip {
  padding: 8rpx 20rpx;
  font-size: 24rpx;
  color: #666;
  background: #f5f6f8;
  border-radius: 999rpx;
}

.sub-cat-chip.active {
  color: $boss-green-deep;
  background: #ecfdf3;
}

.state-wrap {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80rpx 24rpx;
}

.product-scroll {
  flex: 1;
  height: 0;
  padding: 12rpx 16rpx;
  box-sizing: border-box;
}

.card {
  padding: 20rpx;
  margin-bottom: 12rpx;
  background: #fafafa;
  border-radius: 12rpx;
}

.card.idle {
  opacity: 0.72;
  background: #f5f5f5;
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12rpx;
}

.name {
  flex: 1;
  min-width: 0;
  font-size: 30rpx;
  font-weight: 600;
  color: #111;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.unit-tag {
  flex-shrink: 0;
  font-size: 22rpx;
  color: #999;
}

.metric-row {
  display: flex;
  gap: 8rpx;
  margin-top: 16rpx;
}

.metric {
  flex: 1;
  padding: 12rpx 6rpx;
  background: #fff;
  border-radius: 8rpx;
  text-align: center;
}

.metric.highlight {
  background: #eef4ff;
}

.metric-label {
  display: block;
  font-size: 20rpx;
  color: #999;
}

.metric-value {
  display: block;
  margin-top: 6rpx;
  font-size: 24rpx;
  font-weight: 600;
  color: #333;
}

.metric-value.inbound {
  color: #1677ff;
}

.metric-value.outbound {
  color: #e67e22;
}

.metric-value.stock {
  color: #17211b;
}

.metric-value.available {
  color: #1677ff;
}

.list-tail {
  padding: 24rpx 0 40rpx;
  text-align: center;
  font-size: 24rpx;
  color: #ccc;
}
</style>
