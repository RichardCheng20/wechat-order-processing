<template>
  <view class="page boss-page">
    <view class="top-bar">
      <view class="search-wrap">
        <input
          class="search-input"
          type="text"
          :value="keyword"
          placeholder="搜索商品名称/别名"
          confirm-type="search"
          @input="onKeywordInput"
          @confirm="onSearchConfirm"
        />
        <text v-if="keyword" class="search-clear" @tap="clearKeyword">×</text>
      </view>
      <view class="status-tabs">
        <text
          v-for="tab in statusTabs"
          :key="tab.key"
          class="status-tab"
          :class="{ active: statusFilter === tab.key }"
          @tap="statusFilter = tab.key"
        >{{ tab.label }}</text>
      </view>
      <view v-if="!loading && allProducts.length > 0" class="stats-bar">
        <text>共 {{ allProducts.length }} 个</text>
        <text class="stat-dot">·</text>
        <text class="stat-on">上架 {{ onSaleCount }}</text>
        <text class="stat-dot">·</text>
        <text>下架 {{ offSaleCount }}</text>
        <text v-if="filteredProducts.length !== allProducts.length" class="stat-filter">
          · 当前 {{ filteredProducts.length }} 个
        </text>
      </view>
    </view>

    <view class="main boss-main-fill">
      <scroll-view scroll-y class="categories" :show-scrollbar="false">
        <view
          v-for="item in sidebarItems"
          :key="item.key"
          class="cat-item"
          :class="{ active: categoryFilter === item.key }"
          @tap="switchPrimaryCategory(item.key)"
        >{{ item.label }}</view>
      </scroll-view>

      <view class="product-panel">
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

        <scroll-view scroll-y class="product-scroll boss-list-scroll-fill" :show-scrollbar="false">
        <view v-if="loading" class="state-wrap">
          <u-loading-icon text="加载中" />
        </view>

        <view v-else-if="allProducts.length === 0" class="state-wrap">
          <text class="empty-icon">📦</text>
          <text class="empty-text">还没有商品</text>
          <text class="empty-hint">点下方「新建商品」添加，配置单位后上架即可开单</text>
        </view>

        <view v-else-if="filteredProducts.length === 0" class="state-wrap">
          <text class="empty-text">没有匹配的商品</text>
          <text class="empty-hint">换个关键词，或切换左侧分类试试</text>
        </view>

        <view v-else class="list">
          <view v-for="item in filteredProducts" :key="item.id" class="card" @tap="goEdit(item)">
            <view class="card-thumb">
              <text class="thumb-icon">📦</text>
            </view>
            <view class="card-body">
              <view class="card-top">
                <view class="name-block">
                  <text class="name">{{ item.name }}</text>
                  <text v-if="item.aliases" class="alias">{{ item.aliases }}</text>
                </view>
                <view class="status-tag" :class="item.saleStatus === 'ON' ? 'on' : 'off'">
                  {{ item.saleStatus === 'ON' ? '已上架' : '未上架' }}
                </view>
              </view>
              <view class="price-line">
                <text class="price">¥{{ formatPrice(item.defaultPrice) }}</text>
                <text class="price-unit">/{{ item.unit }}</text>
              </view>
              <view class="unit-tags">
                <text
                  v-for="u in parseSaleUnits(item.saleUnits, item.unit)"
                  :key="u"
                  class="unit-tag"
                >{{ u }}</text>
              </view>
              <view class="actions" @tap.stop>
                <view class="action-btn" @tap="goEdit(item)">编辑</view>
                <template v-if="item.saleStatus === 'ON'">
                  <view class="action-btn primary" @tap="openListPopup(item)">改单位</view>
                  <view class="action-btn warn" @tap="delistProduct(item)">下架</view>
                </template>
                <template v-else>
                  <view class="action-btn solid" @tap="openListPopup(item)">上架</view>
                </template>
                <view class="action-btn danger" @tap="deleteProduct(item)">删除</view>
              </view>
            </view>
          </view>
        </view>
        </scroll-view>
      </view>
    </view>

    <view class="boss-bottom-bar boss-bottom-bar--static">
      <button class="boss-primary-btn block" @tap="goCreate">新建商品</button>
    </view>

    <!-- 上架 / 修改单位 -->
    <u-popup :show="showListPopup" mode="bottom" round="20" @close="closeListPopup">
      <view class="sheet">
        <view class="sheet-head">
          <text class="sheet-cancel" @tap="closeListPopup">取消</text>
          <text class="sheet-title">{{ listingProduct?.saleStatus === 'ON' ? '修改售卖单位' : '商品上架' }}</text>
          <text class="sheet-confirm" @tap="confirmListing">确定</text>
        </view>
        <text v-if="listingProduct" class="sheet-product">{{ listingProduct.name }}</text>
        <text class="field-hint block">勾选可售单位，开单时从中选择</text>
        <scroll-view scroll-y class="sheet-body compact">
          <view class="unit-grid">
            <view
              v-for="unit in listUnitOptions"
              :key="unit"
              class="unit-chip"
              :class="{ active: selectedUnits.includes(unit) }"
              @tap="toggleUnit(unit)"
            >
              <text v-if="selectedUnits.includes(unit)" class="chip-check">✓</text>
              <text>{{ unit }}</text>
            </view>
          </view>
          <text class="selected-summary">已选：{{ selectedUnits.join('、') || '未选择' }}</text>
        </scroll-view>
      </view>
    </u-popup>
  </view>
</template>

<script setup lang="ts">
import { onLoad, onShow } from '@dcloudio/uni-app'
import { computed, ref } from 'vue'
import { COMMON_SALE_UNITS, mergeUnits, parseSaleUnits } from '../../../constants/units'
import {
  deleteBossProduct,
  fetchBossCategories,
  fetchBossProducts,
  updateBossProductSaleStatus,
  type CategoryItem,
  type ProductItem,
} from '../../../api/product'
import { buildPrimarySidebar, getParentCategory, matchCategoryFilter } from '../../../utils/category'
import { useUserStore } from '../../../stores/user'

const userStore = useUserStore()
const categories = ref<CategoryItem[]>([])
const allProducts = ref<ProductItem[]>([])
const loading = ref(false)
const listing = ref(false)
const keyword = ref('')
const statusFilter = ref<'all' | 'ON' | 'OFF'>('all')
const categoryFilter = ref('all')
const subCategoryFilter = ref('all')
const showListPopup = ref(false)
const listingProduct = ref<ProductItem | null>(null)
const selectedUnits = ref<string[]>([])
const pendingCategoryId = ref<number | null>(null)

const statusTabs = [
  { key: 'all' as const, label: '全部' },
  { key: 'ON' as const, label: '已上架' },
  { key: 'OFF' as const, label: '未上架' },
]

const onSaleCount = computed(() => allProducts.value.filter((p) => p.saleStatus === 'ON').length)
const offSaleCount = computed(() => allProducts.value.filter((p) => p.saleStatus === 'OFF').length)

const sidebarItems = computed(() => buildPrimarySidebar(categories.value))

const activeParent = computed(() => getParentCategory(categories.value, categoryFilter.value))

const subCategoryTabs = computed(() => {
  const parent = activeParent.value
  if (!parent?.children?.length) return []
  return [
    { key: 'all', label: '全部' },
    ...parent.children.map((child) => ({ key: String(child.id), label: child.name })),
  ]
})

const filteredProducts = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  return allProducts.value.filter((item) => {
    if (statusFilter.value !== 'all' && item.saleStatus !== statusFilter.value) return false
    if (!matchProductCategory(item.categoryId)) return false
    if (!kw) return true
    const hay = `${item.name} ${item.aliases || ''} ${item.categoryName || ''}`.toLowerCase()
    return hay.includes(kw)
  })
})

function matchProductCategory(productCategoryId?: number) {
  if (categoryFilter.value === 'all') return true
  if (categoryFilter.value === 'uncategorized') return !productCategoryId
  if (subCategoryFilter.value !== 'all') {
    return productCategoryId === Number(subCategoryFilter.value)
  }
  return matchCategoryFilter(productCategoryId, categoryFilter.value, categories.value)
}

function switchPrimaryCategory(key: string) {
  categoryFilter.value = key
  subCategoryFilter.value = 'all'
}

const listUnitOptions = computed(() => mergeUnits([...selectedUnits.value, ...COMMON_SALE_UNITS]))

type UniInputEvent = Event & {
  detail?: { value?: string }
}

onLoad((query) => {
  if (query?.categoryId) {
    const id = Number(query.categoryId)
    if (id) pendingCategoryId.value = id
  }
})

onShow(async () => {
  if (!userStore.isLoggedIn || !userStore.isBoss) {
    uni.reLaunch({ url: '/pages/login/index' })
    return
  }
  await loadData()
  if (pendingCategoryId.value) {
    categoryFilter.value = String(pendingCategoryId.value)
    subCategoryFilter.value = 'all'
    pendingCategoryId.value = null
  }
})

async function loadData() {
  loading.value = true
  try {
    categories.value = await fetchBossCategories()
    allProducts.value = await fetchBossProducts()
  } finally {
    loading.value = false
  }
}

function onKeywordInput(e: UniInputEvent) {
  const target = e.target as HTMLInputElement | null
  keyword.value = e.detail?.value ?? target?.value ?? ''
}

function onSearchConfirm() {
  keyword.value = keyword.value.trim()
}

function clearKeyword() {
  keyword.value = ''
}

function goCreate() {
  uni.navigateTo({ url: '/pages/boss/products/form' })
}

function goEdit(item: ProductItem) {
  uni.navigateTo({ url: `/pages/boss/products/form?id=${item.id}` })
}

function openListPopup(item: ProductItem) {
  listingProduct.value = item
  selectedUnits.value = parseSaleUnits(item.saleUnits, item.unit)
  if (selectedUnits.value.length === 0) {
    selectedUnits.value = ['斤']
  }
  showListPopup.value = true
}

function closeListPopup() {
  showListPopup.value = false
  listingProduct.value = null
  selectedUnits.value = []
}

function toggleUnit(unit: string) {
  const idx = selectedUnits.value.indexOf(unit)
  if (idx >= 0) {
    if (selectedUnits.value.length <= 1) {
      uni.showToast({ title: '至少保留一个单位', icon: 'none' })
      return
    }
    selectedUnits.value = selectedUnits.value.filter((u) => u !== unit)
    return
  }
  selectedUnits.value = [...selectedUnits.value, unit]
}

async function confirmListing() {
  if (!listingProduct.value) return
  if (selectedUnits.value.length === 0) {
    uni.showToast({ title: '请至少选择一个售卖单位', icon: 'none' })
    return
  }
  const wasOn = listingProduct.value.saleStatus === 'ON'
  listing.value = true
  try {
    await updateBossProductSaleStatus(listingProduct.value.id, {
      saleStatus: 'ON',
      saleUnits: selectedUnits.value,
    })
    closeListPopup()
    await loadData()
    uni.showToast({ title: wasOn ? '单位已更新' : '已上架', icon: 'success' })
  } catch (e) {
    uni.showToast({ title: e instanceof Error ? e.message : '操作失败', icon: 'none' })
  } finally {
    listing.value = false
  }
}

async function delistProduct(item: ProductItem) {
  uni.showModal({
    title: '确认下架',
    content: `下架后开单和客户将无法选择「${item.name}」`,
    success: async (res) => {
      if (!res.confirm) return
      try {
        await updateBossProductSaleStatus(item.id, { saleStatus: 'OFF' })
        await loadData()
        uni.showToast({ title: '已下架', icon: 'success' })
      } catch (e) {
        uni.showToast({ title: e instanceof Error ? e.message : '下架失败', icon: 'none' })
      }
    },
  })
}

function deleteProduct(item: ProductItem) {
  uni.showModal({
    title: '确认删除',
    content: `确定删除「${item.name}」？删除后不可恢复。`,
    confirmColor: '#e74c3c',
    success: async (res) => {
      if (!res.confirm) return
      try {
        await deleteBossProduct(item.id)
        await loadData()
        uni.showToast({ title: '已删除', icon: 'success' })
      } catch (e) {
        uni.showToast({ title: e instanceof Error ? e.message : '删除失败', icon: 'none' })
      }
    },
  })
}

function formatPrice(value?: number) {
  if (value == null) return '0'
  return Number(value).toFixed(2)
}
</script>

<style scoped lang="scss">
@import '../../../styles/boss-footer.scss';

.page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  overflow: hidden;
  box-sizing: border-box;
}

.top-bar {
  flex-shrink: 0;
  padding: 16rpx 24rpx 12rpx;
  background: $boss-surface;
  border-bottom: 1rpx solid $boss-border;
}

.search-wrap {
  position: relative;
}

.search-input {
  @include boss-search-input;
}

.search-clear {
  position: absolute;
  right: 20rpx;
  top: 50%;
  transform: translateY(-50%);
  width: 40rpx;
  height: 40rpx;
  line-height: 40rpx;
  text-align: center;
  font-size: 32rpx;
  color: $boss-ink-muted;
}

.status-tabs {
  display: flex;
  gap: 10rpx;
  margin-top: 12rpx;
}

.status-tab {
  @include boss-chip(false);
}

.status-tab.active {
  @include boss-chip(true);
}

.stats-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  margin-top: 12rpx;
  font-size: 22rpx;
  color: $boss-ink-muted;
}

.stat-dot {
  margin: 0 8rpx;
}

.stat-on {
  color: $boss-green;
}

.stat-filter {
  color: $boss-ink-secondary;
}

.main {
  flex-direction: row;
  align-items: stretch;
}

.categories {
  width: 176rpx;
  flex-shrink: 0;
  height: 100%;
  background: $boss-bg;
  border-right: 1rpx solid $boss-border;
}

.product-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
  height: 100%;
  background: $boss-surface;
}

.product-scroll {
  background: $boss-surface;
}

.sub-cat-scroll {
  flex-shrink: 0;
  white-space: nowrap;
  background: $boss-surface;
  border-bottom: 1rpx solid $boss-border;
}

.sub-cat-row {
  display: inline-flex;
  gap: 12rpx;
  padding: 14rpx 20rpx;
}

.sub-cat-chip {
  flex-shrink: 0;
  @include boss-chip(false);
  font-size: 26rpx;
}

.sub-cat-chip.active {
  @include boss-chip(true);
  font-size: 26rpx;
}

.cat-item {
  padding: 28rpx 16rpx;
  font-size: 26rpx;
  color: $boss-ink-secondary;
  text-align: center;
  line-height: 1.35;
  word-break: break-all;
}

.cat-item.active {
  @include boss-cat-active;
}

.state-wrap {
  padding: 100rpx 40rpx;
  text-align: center;
}

.name-block {
  flex: 1;
  min-width: 0;
}

.empty-icon {
  display: block;
  font-size: 56rpx;
  margin-bottom: 12rpx;
}

.empty-text {
  display: block;
  font-size: 28rpx;
  color: $boss-ink;
  font-weight: 500;
}

.empty-hint {
  display: block;
  margin-top: 10rpx;
  font-size: 24rpx;
  color: $boss-ink-muted;
  line-height: 1.5;
}

.list {
  padding: 0;
}

.card {
  display: flex;
  gap: 16rpx;
  padding: 20rpx;
  border-bottom: 1rpx solid $boss-border;
  box-sizing: border-box;
}

.card:active {
  background: #fafcfa;
}

.card-thumb {
  flex-shrink: 0;
  width: 88rpx;
  height: 88rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: $boss-bg;
  border-radius: $boss-radius;
}

.thumb-icon {
  font-size: 38rpx;
}

.card-body {
  flex: 1;
  min-width: 0;
}

.card-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12rpx;
}

.name {
  display: block;
  font-size: 30rpx;
  font-weight: 600;
  color: $boss-ink;
}

.alias {
  display: block;
  margin-top: 4rpx;
  font-size: 22rpx;
  color: $boss-ink-muted;
}

.status-tag {
  flex-shrink: 0;
  padding: 4rpx 12rpx;
  font-size: 20rpx;
  border-radius: $boss-radius-pill;
}

.status-tag.on {
  color: $boss-green-deep;
  background: $boss-green-soft;
}

.status-tag.off {
  color: $boss-ink-muted;
  background: $boss-bg;
}

.price-line {
  margin-top: 10rpx;
}

.price {
  font-size: 28rpx;
  color: $boss-price;
  font-weight: 700;
}

.price-unit {
  font-size: 22rpx;
  color: $boss-ink-muted;
}

.unit-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8rpx;
  margin-top: 8rpx;
}

.unit-tag {
  padding: 2rpx 12rpx;
  font-size: 22rpx;
  color: $boss-green-deep;
  background: $boss-green-soft;
  border-radius: $boss-radius-pill;
}

.actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10rpx;
  margin-top: 14rpx;
}

.action-btn {
  padding: 8rpx 20rpx;
  font-size: 24rpx;
  border-radius: 8rpx;
  color: $boss-ink-secondary;
  background: $boss-bg;
}

.action-btn.primary {
  color: #1677ff;
  background: #eef5ff;
}

.action-btn.warn {
  color: $boss-warn;
  background: #fff8e6;
}

.action-btn.solid {
  color: #fff;
  background: $boss-green;
  font-weight: 600;
}

.action-btn.danger {
  color: $boss-danger;
  background: #fff1f0;
}

.sheet {
  max-height: 88vh;
  display: flex;
  flex-direction: column;
  background: #fff;
}

.sheet-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 28rpx 32rpx 16rpx;
  border-bottom: 1rpx solid #f2f3f5;
}

.sheet-cancel {
  min-width: 80rpx;
  font-size: 28rpx;
  color: #666;
}

.sheet-title {
  font-size: 34rpx;
  font-weight: 600;
}

.sheet-confirm {
  min-width: 80rpx;
  text-align: right;
  font-size: 28rpx;
  color: #07c160;
  font-weight: 600;
}

.sheet-product {
  display: block;
  padding: 16rpx 32rpx 0;
  font-size: 30rpx;
  color: #333;
  font-weight: 500;
}

.sheet-body {
  flex: 1;
  max-height: 56vh;
  padding: 8rpx 32rpx 24rpx;
}

.sheet-body.compact {
  max-height: 50vh;
}

.field-hint {
  display: block;
  margin-bottom: 16rpx;
  font-size: 24rpx;
  color: #999;
  line-height: 1.5;
}

.field-hint.block {
  padding: 0 32rpx 8rpx;
}

.unit-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
}

.unit-chip {
  min-width: calc((100% - 32rpx) / 3);
  height: 76rpx;
  padding: 0 12rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6rpx;
  font-size: 28rpx;
  color: #555;
  background: #f7f8fa;
  border-radius: 14rpx;
  border: 2rpx solid transparent;
  box-sizing: border-box;
}

.unit-chip.active {
  color: #07c160;
  background: #e8f8ef;
  border-color: #07c160;
  font-weight: 600;
}

.chip-check {
  font-size: 22rpx;
}

.selected-summary {
  display: block;
  margin-top: 20rpx;
  font-size: 26rpx;
  color: #07c160;
}
</style>
