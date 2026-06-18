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
            :key="item.productId"
            class="card"
            :class="{ idle: !item.demandQty }"
            @tap="goDetail(item.productId)"
          >
            <view class="card-head">
              <text class="name">{{ item.productName }}</text>
              <view v-if="item.priced" class="priced-tag">已录进价</view>
            </view>
            <view class="metric-row">
              <view class="metric">
                <text class="metric-label">客户需求</text>
                <text class="metric-value">{{ formatQty(item.demandQty) }}{{ item.unit }}</text>
              </view>
              <view class="metric">
                <text class="metric-label">库存</text>
                <text class="metric-value">{{ formatQty(item.stockQty) }}{{ item.unit }}</text>
              </view>
              <view class="metric highlight">
                <text class="metric-label">需采购</text>
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
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { fetchProcurementTasks, type ProcurementTask, type ProcurementTaskItem } from '../../../api/procurement'
import { fetchBossCategories, type CategoryItem } from '../../../api/product'
import AppIcon from '../../../components/AppIcon.vue'
import BossTabbar from '../../../components/boss-tabbar/index.vue'
import { useUserStore } from '../../../stores/user'
import { buildPrimarySidebar, getParentCategory } from '../../../utils/category'

const userStore = useUserStore()
const task = ref<ProcurementTask | null>(null)
const categories = ref<CategoryItem[]>([])
const loading = ref(false)
const keyword = ref('')
const receiveDate = ref('')
const categoryFilter = ref('all')
const subCategoryFilter = ref('all')

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
    items = items.filter((item) => !item.categoryId)
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

function goDetail(productId: number) {
  uni.navigateTo({
    url: `/pages/boss/procurement/detail/index?productId=${productId}&receiveDate=${receiveDate.value}`,
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

.metric-label {
  display: block;
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
</style>
