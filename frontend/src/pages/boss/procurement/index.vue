<template>
  <view class="page">
    <view class="top-bar">
      <view class="search-wrap">
        <AppIcon class="search-icon" name="search" tone="green" :tile="false" :size="18" />
        <input
          v-model="keyword"
          class="search-input"
          placeholder="搜索商品"
          confirm-type="search"
          @confirm="loadTasks"
        />
      </view>
      <view class="date-row">
        <text class="date-label">收货：{{ receiveDateLabel }}</text>
        <view class="day-nav">
          <text class="nav-btn" @tap="shiftDay(-1)">前一天</text>
          <text class="nav-btn" @tap="shiftDay(1)">后一天</text>
        </view>
      </view>
      <view v-if="task" class="summary-bar">
        <text>{{ task.productCount }}种 · 需采购 {{ formatQty(task.totalNeedQty) }} · 预估 ¥{{ formatMoney(task.totalAmount) }}</text>
        <text class="help-btn" @tap.stop="showMetricHelp('overview')">?</text>
      </view>
    </view>

    <view class="main">
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

        <view v-if="loading" class="state-wrap">
          <u-loading-icon text="加载中" />
        </view>

        <view v-else-if="displayItems.length === 0" class="state-wrap">
          <u-empty mode="data" text="暂无采购任务" />
          <text class="empty-hint">调整收货日期，或确认当日是否有客户下单</text>
        </view>

        <scroll-view v-else scroll-y class="product-scroll" :show-scrollbar="false">
          <view
            v-for="item in displayItems"
            :key="item.customItem ? `c-${item.customName}` : item.productId"
            class="card"
            :class="{ idle: !item.demandQty, custom: item.customItem }"
            @tap="goDetail(item)"
          >
            <view class="card-head">
              <text class="name">{{ item.productName }}</text>
              <text v-if="item.customItem" class="custom-tag">代采</text>
              <view v-if="item.priced" class="priced-tag">已录进价</view>
            </view>
            <view class="metric-row">
              <view class="metric">
                <view class="metric-label-row">
                  <text class="metric-label">客户需求</text>
                  <text class="help-btn tiny" @tap.stop="showMetricHelp('demand')">?</text>
                </view>
                <text class="metric-value">{{ formatQty(item.demandQty) }}{{ item.unit }}</text>
              </view>
              <view class="metric">
                <view class="metric-label-row">
                  <text class="metric-label">仓存</text>
                  <text class="help-btn tiny" @tap.stop="showMetricHelp('available')">?</text>
                </view>
                <text class="metric-value">{{ formatQty(stockRemain(item)) }}{{ item.unit }}</text>
              </view>
              <view class="metric highlight">
                <view class="metric-label-row">
                  <text class="metric-label">需采购</text>
                  <text class="help-btn tiny" @tap.stop="showMetricHelp('need')">?</text>
                </view>
                <text class="metric-value need">{{ formatQty(item.needQty) }}{{ item.unit }}</text>
              </view>
            </view>
            <view class="card-foot">
              <text class="foot-meta">
                <template v-if="item.demandQty > 0">{{ item.customerCount || 0 }}家客户 · {{ item.orderCount || 0 }}笔订单</template>
                <template v-else>今日暂无下单</template>
              </text>
              <text v-if="item.purchasePrice != null" class="foot-price">进价 ¥{{ formatMoney(item.purchasePrice) }}</text>
              <text v-else class="foot-price muted">进价未录</text>
            </view>
          </view>
          <view class="list-tail">—— 没有更多了 ——</view>
        </scroll-view>
      </view>
    </view>

    <BossTabbar active="procurement" />

    <u-popup :show="helpVisible" mode="center" round="16" @close="closeMetricHelp">
      <view class="help-panel">
        <text class="help-title">{{ helpTitle }}</text>
        <scroll-view scroll-y class="help-body">
          <text class="help-text">{{ helpContent }}</text>
        </scroll-view>
        <button class="help-ok" @tap="closeMetricHelp">知道了</button>
      </view>
    </u-popup>
  </view>
</template>

<script setup lang="ts">
import { computed, provide, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { fetchProcurementTasks, type ProcurementTask, type ProcurementTaskItem } from '@common/api/procurement'
import { fetchBossCategories, type CategoryItem } from '@common/api/product'
import AppIcon from '@/components/AppIcon.vue'
import BossTabbar from '@/components/boss-tabbar/index.vue'
import { useUserStore } from '@common/stores/user'
import { useBossOrderAlertOnShow } from '@common/utils/boss-order-alert'
import { useBossAlertStore } from '@common/stores/bossAlert'
import { buildPrimarySidebar, getParentCategory } from '@common/utils/category'
import {
  getProcurementMetricHelp,
  type ProcurementMetricHelpKey,
} from '@common/utils/procurement-metric-help'

const userStore = useUserStore()
const bossAlert = useBossAlertStore()
provide('bossAlert', bossAlert)
const { onBossPageShow } = useBossOrderAlertOnShow()
const task = ref<ProcurementTask | null>(null)
const categories = ref<CategoryItem[]>([])
const loading = ref(false)
const keyword = ref('')
const receiveDate = ref('')
const categoryFilter = ref('all')
const subCategoryFilter = ref('all')

const helpVisible = ref(false)
const helpTitle = ref('')
const helpContent = ref('')

function showMetricHelp(key: ProcurementMetricHelpKey) {
  const item = getProcurementMetricHelp(key)
  helpTitle.value = item.title
  helpContent.value = item.content
  helpVisible.value = true
}

function closeMetricHelp() {
  helpVisible.value = false
}

const sidebarItems = computed(() => buildPrimarySidebar(categories.value))
const activeParent = computed(() => getParentCategory(categories.value, categoryFilter.value))
const subCategoryTabs = computed(() => {
  const parent = activeParent.value
  if (!parent?.children?.length) return []
  return [{ key: 'all', label: '全部' }, ...parent.children.map((c) => ({ key: String(c.id), label: c.name }))]
})

const receiveDateLabel = computed(() => {
  if (!receiveDate.value) return '—'
  const parts = receiveDate.value.split('-')
  if (parts.length < 3) return receiveDate.value
  return `${parts[1]}-${parts[2]}`
})

const displayItems = computed(() => {
  let items = task.value?.items || []
  if (categoryFilter.value === 'uncategorized') {
    items = items.filter((item) => item.customItem || !item.categoryId)
  }
  if (subCategoryFilter.value !== 'all') {
    const catId = Number(subCategoryFilter.value)
    items = items.filter((item) => item.categoryId === catId)
  }
  return items
})

onShow(async () => {
  if (!userStore.isLoggedIn || !userStore.isBoss) {
    uni.reLaunch({ url: '/pages/login/index' })
    return
  }
  if (!receiveDate.value) {
    receiveDate.value = formatDate(new Date())
  }
  try {
    categories.value = await fetchBossCategories()
  } catch {
    categories.value = []
  }
  await onBossPageShow()
  await loadTasks()
})

async function loadTasks() {
  loading.value = true
  try {
    const categoryId = resolveCategoryId()
    task.value = await fetchProcurementTasks({
      receiveDate: receiveDate.value,
      keyword: keyword.value.trim() || undefined,
      categoryId,
    })
    if (task.value?.receiveDate) {
      receiveDate.value = task.value.receiveDate
    }
  } catch (e) {
    task.value = null
    uni.showToast({ title: e instanceof Error ? e.message : '加载失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

function resolveCategoryId() {
  if (categoryFilter.value === 'all' || categoryFilter.value === 'uncategorized') {
    return undefined
  }
  if (subCategoryFilter.value !== 'all') {
    return Number(subCategoryFilter.value)
  }
  return Number(categoryFilter.value)
}

function switchCategory(key: string) {
  categoryFilter.value = key
  subCategoryFilter.value = 'all'
  loadTasks()
}

function shiftDay(delta: number) {
  const base = parseDate(receiveDate.value)
  base.setDate(base.getDate() + delta)
  receiveDate.value = formatDate(base)
  loadTasks()
}

function goDetail(item: ProcurementTaskItem) {
  if (item.customItem && item.customName) {
    uni.navigateTo({
      url: `/pages/boss/procurement/detail/index?customName=${encodeURIComponent(item.customName)}&receiveDate=${receiveDate.value}`,
    })
    return
  }
  uni.navigateTo({
    url: `/pages/boss/procurement/detail/index?productId=${item.productId}&receiveDate=${receiveDate.value}`,
  })
}

function formatDate(date: Date) {
  const y = date.getFullYear()
  const m = `${date.getMonth() + 1}`.padStart(2, '0')
  const d = `${date.getDate()}`.padStart(2, '0')
  return `${y}-${m}-${d}`
}

function parseDate(value: string) {
  const parts = value.split('-').map(Number)
  if (parts.length >= 3) return new Date(parts[0], parts[1] - 1, parts[2])
  return new Date()
}

function formatMoney(value?: number | null) {
  if (value == null) return '0.00'
  return Number(value).toFixed(2)
}

function stockRemain(item: ProcurementTaskItem) {
  if (item.customItem) return 0
  return item.physicalStockQty ?? item.stockQty ?? 0
}

function formatQty(value?: number) {
  if (value == null) return '0'
  return Number(value).toFixed(2).replace(/\.?0+$/, '') || '0'
}
</script>

<style scoped lang="scss">
@import '../../../styles/boss-ui.scss';

.page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: $boss-bg;
  padding-bottom: calc(140rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
}

.top-bar {
  background: $boss-surface;
  padding: 16rpx 20rpx 0;
}

.search-wrap {
  display: flex;
  align-items: center;
  height: 72rpx;
  padding: 0 20rpx;
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

.date-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16rpx 4rpx;
  font-size: 28rpx;
}

.day-nav {
  display: flex;
  gap: 16rpx;
}

.nav-btn {
  color: #2979ff;
  font-size: 26rpx;
}

.summary-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12rpx;
  margin: 0 -20rpx;
  padding: 16rpx 20rpx;
  background: #fff7e6;
  color: #e67e22;
  font-size: 26rpx;
  border-top: 1rpx solid #ffe8c7;
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

.product-panel.has-sub-cats .product-scroll {
  height: calc(100vh - 420rpx);
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

.empty-hint {
  margin-top: 12rpx;
  font-size: 24rpx;
  color: #999;
}

.product-scroll {
  flex: 1;
  height: calc(100vh - 360rpx);
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

.card.idle .metric-value.need {
  color: #ccc;
}

.custom-tag {
  flex-shrink: 0;
  padding: 4rpx 12rpx;
  font-size: 20rpx;
  color: #e67e22;
  background: #fff7e6;
  border-radius: 999rpx;
}

.card.custom {
  border: 1rpx solid #ffe8c7;
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12rpx;
}

.name {
  font-size: 30rpx;
  font-weight: 600;
  color: #111;
}

.priced-tag {
  flex-shrink: 0;
  padding: 4rpx 12rpx;
  font-size: 20rpx;
  color: $boss-green-deep;
  background: #ecfdf3;
  border-radius: 999rpx;
}

.metric-row {
  display: flex;
  gap: 8rpx;
  margin-top: 16rpx;
}

.metric {
  flex: 1;
  padding: 12rpx 8rpx;
  background: #fff;
  border-radius: 8rpx;
  text-align: center;
}

.metric.highlight {
  background: #fff7e6;
}

.metric-label-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4rpx;
}

.metric-label {
  font-size: 20rpx;
  color: #999;
}

.metric-value {
  display: block;
  margin-top: 6rpx;
  font-size: 26rpx;
  font-weight: 600;
  color: #333;
}

.metric-value.need {
  color: #e67e22;
}

.card-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 12rpx;
  font-size: 22rpx;
}

.foot-meta {
  color: #999;
}

.foot-price {
  color: $boss-green-deep;
  font-weight: 600;
}

.foot-price.muted {
  color: #bbb;
  font-weight: 400;
}

.list-tail {
  padding: 24rpx 0 40rpx;
  text-align: center;
  font-size: 24rpx;
  color: #ccc;
}

.help-btn {
  flex-shrink: 0;
  width: 36rpx;
  height: 36rpx;
  line-height: 36rpx;
  text-align: center;
  font-size: 24rpx;
  font-weight: 600;
  color: #e67e22;
  background: rgba(255, 255, 255, 0.85);
  border: 1rpx solid #ffd591;
  border-radius: 50%;
}

.help-btn.tiny {
  width: 28rpx;
  height: 28rpx;
  line-height: 28rpx;
  font-size: 20rpx;
  color: #999;
  background: #f5f5f5;
  border-color: #e8e8e8;
}

.help-panel {
  width: 620rpx;
  max-width: 86vw;
  padding: 32rpx 28rpx 24rpx;
  box-sizing: border-box;
}

.help-title {
  display: block;
  font-size: 32rpx;
  font-weight: 600;
  color: #111;
  text-align: center;
}

.help-body {
  max-height: 52vh;
  margin-top: 20rpx;
}

.help-text {
  display: block;
  font-size: 26rpx;
  line-height: 1.65;
  color: #555;
  white-space: pre-wrap;
}

.help-ok {
  margin-top: 24rpx;
  height: 80rpx;
  line-height: 80rpx;
  font-size: 28rpx;
  color: #fff;
  background: $boss-green;
  border: none;
  border-radius: 12rpx;
}
</style>
