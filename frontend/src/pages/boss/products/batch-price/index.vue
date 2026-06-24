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
      <view class="hint-row">
        <text class="hint-text">可先复制昨日价格，再微调有变化的商品</text>
        <view class="only-unset" :class="{ active: onlyUnset }" @tap="onlyUnset = !onlyUnset">
          仅未定价
        </view>
      </view>
      <view v-if="!loading" class="stats-bar">
        <text>当前 {{ visibleProducts.length }} 个</text>
        <text v-if="changedCount > 0" class="stat-changed">· 已改 {{ changedCount }} 项</text>
      </view>
    </view>

    <view class="main" :style="mainStyle">
      <scroll-view scroll-y class="categories" :show-scrollbar="false">
        <view
          v-for="item in sidebarItems"
          :key="item.key"
          class="cat-item"
          :class="{ active: categoryFilter === item.key }"
          @tap="switchPrimaryCategory(item.key)"
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

        <view v-if="!loading && visibleProducts.length > 0" class="list-head">
          <text class="head-name">商品</text>
          <text class="head-yesterday">昨日</text>
          <text class="head-today">今日</text>
        </view>

        <scroll-view scroll-y class="product-scroll" :scroll-into-view="scrollIntoId" :show-scrollbar="false">
          <view v-if="loading" class="state-wrap">
            <u-loading-icon text="加载中" />
          </view>
          <view v-else-if="visibleProducts.length === 0" class="state-wrap">
            <text class="empty-text">没有需要录价的商品</text>
          </view>
          <view v-else class="price-list">
            <view
              v-for="item in visibleProducts"
              :id="`p-${item.id}`"
              :key="item.id"
              class="price-row"
              :class="{ changed: isChanged(item) }"
            >
              <view class="row-main">
                <text class="row-name">{{ item.name }}</text>
                <text v-if="item.aliases" class="row-alias">{{ item.aliases }}</text>
              </view>
              <view class="row-yesterday">
                <text v-if="hasYesterdayPrice(item.id)" class="yesterday-value">
                  {{ formatDraft(yesterdayPrices[item.id]) }}
                </text>
                <text v-else class="yesterday-empty">—</text>
              </view>
              <view class="row-input-wrap">
                <input
                  class="row-input"
                  type="digit"
                  :value="priceDrafts[item.id] ?? ''"
                  placeholder="—"
                  placeholder-class="input-placeholder"
                  confirm-type="next"
                  @input="(e) => onPriceInput(item.id, e)"
                  @confirm="focusNext(item.id)"
                />
                <text class="row-unit">元/{{ item.unit }}</text>
              </view>
            </view>
          </view>
        </scroll-view>
      </view>
    </view>

    <view class="boss-bottom-bar batch-bar">
      <button class="boss-secondary-btn back-btn" @tap="handleBack">返回</button>
      <button class="boss-secondary-btn copy-btn" @tap="copyYesterdayPrices">复制昨日价格</button>
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
import { onLoad, onReady, onShow } from '@dcloudio/uni-app'
import { computed, nextTick, reactive, ref } from 'vue'
import {
  fetchBossCategories,
  fetchBossDailyPrices,
  fetchBossProducts,
  updateBossProduct,
  type CategoryItem,
  type ProductItem,
} from '@common/api/product'
import { buildPrimarySidebar, getParentCategory, matchCategoryFilter } from '@common/utils/category'
import { useUserStore } from '@common/stores/user'

type UniInputEvent = Event & { detail?: { value?: string } }

const userStore = useUserStore()
const categories = ref<CategoryItem[]>([])
const allProducts = ref<ProductItem[]>([])
const priceDrafts = reactive<Record<number, string>>({})
const yesterdayPrices = reactive<Record<number, number>>({})
const loading = ref(false)
const saving = ref(false)
const keyword = ref('')
const onlyUnset = ref(false)
const categoryFilter = ref('all')
const subCategoryFilter = ref('all')
const mainHeight = ref(0)
const scrollIntoId = ref('')

const sidebarItems = computed(() => buildPrimarySidebar(categories.value))
const activeParent = computed(() => getParentCategory(categories.value, categoryFilter.value))
const mainStyle = computed(() => (mainHeight.value > 0 ? { height: `${mainHeight.value}px` } : {}))

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
    if (item.saleStatus !== 'ON') return false
    if (!matchProductCategory(item.categoryId)) return false
    if (!kw) return true
    const hay = `${item.name} ${item.aliases || ''} ${item.categoryName || ''}`.toLowerCase()
    return hay.includes(kw)
  })
})

const visibleProducts = computed(() => {
  if (!onlyUnset.value) return filteredProducts.value
  return filteredProducts.value.filter((item) => !hasPrice(item))
})

const changedCount = computed(() => visibleProducts.value.filter((item) => isChanged(item)).length)

onLoad((query) => {
  if (query?.category) {
    categoryFilter.value = String(query.category)
  }
})

onReady(() => {
  updateMainHeight()
})

onShow(async () => {
  if (!userStore.isLoggedIn || !userStore.isBoss) {
    uni.reLaunch({ url: '/packages/common/login/index' })
    return
  }
  await loadData()
  await nextTick()
  updateMainHeight()
})

async function loadData() {
  loading.value = true
  try {
    categories.value = await fetchBossCategories()
    allProducts.value = await fetchBossProducts()
    syncDraftsFromProducts()
    await loadYesterdayPrices()
  } finally {
    loading.value = false
  }
}

function formatDateOffset(days: number) {
  const d = new Date()
  d.setDate(d.getDate() + days)
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

async function loadYesterdayPrices() {
  const date = formatDateOffset(-1)
  const raw = await fetchBossDailyPrices(date)
  Object.keys(yesterdayPrices).forEach((key) => {
    delete yesterdayPrices[Number(key)]
  })
  for (const [id, price] of Object.entries(raw)) {
    const num = Number(price)
    if (!Number.isNaN(num) && num > 0) {
      yesterdayPrices[Number(id)] = num
    }
  }
}

function hasYesterdayPrice(id: number) {
  return yesterdayPrices[id] != null && yesterdayPrices[id] > 0
}

function copyYesterdayPrices() {
  let count = 0
  for (const item of visibleProducts.value) {
    const price = yesterdayPrices[item.id]
    if (price == null || price <= 0) continue
    priceDrafts[item.id] = formatDraft(price)
    count += 1
  }
  if (count === 0) {
    uni.showToast({ title: '当前列表暂无昨日价格', icon: 'none' })
    return
  }
  uni.showToast({ title: `已复制 ${count} 项`, icon: 'success' })
}

function syncDraftsFromProducts() {
  for (const item of allProducts.value) {
    if (!(item.id in priceDrafts)) {
      priceDrafts[item.id] = formatDraft(item.defaultPrice)
    }
  }
}

function formatDraft(value?: number) {
  if (value == null || Number(value) <= 0) return ''
  return Number(value).toFixed(2)
}

function hasPrice(item: ProductItem) {
  return item.defaultPrice != null && Number(item.defaultPrice) > 0
}

function normalizePrice(value: string) {
  const trimmed = value.trim()
  if (!trimmed) return null
  const num = Number(trimmed)
  if (Number.isNaN(num) || num < 0) return undefined
  return num
}

function isChanged(item: ProductItem) {
  const draft = normalizePrice(priceDrafts[item.id] ?? '')
  if (draft === undefined) return false
  if (draft === null) return hasPrice(item)
  const original = item.defaultPrice != null ? Number(item.defaultPrice) : 0
  return Math.abs(draft - original) > 0.001
}

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

function onPriceInput(id: number, e: UniInputEvent) {
  const target = e.target as HTMLInputElement | null
  priceDrafts[id] = e.detail?.value ?? target?.value ?? ''
}

function focusNext(currentId: number) {
  const list = visibleProducts.value
  const idx = list.findIndex((item) => item.id === currentId)
  if (idx < 0 || idx >= list.length - 1) return
  scrollIntoId.value = `p-${list[idx + 1].id}`
}

function updateMainHeight() {
  const { windowHeight } = uni.getSystemInfoSync()
  uni
    .createSelectorQuery()
    .select('.top-bar')
    .boundingClientRect((rect) => {
      const topBarHeight = Array.isArray(rect) ? rect[0]?.height : rect?.height
      if (typeof topBarHeight !== 'number') return
      mainHeight.value = Math.max(0, windowHeight - topBarHeight)
    })
    .exec()
}

function handleBack() {
  if (changedCount.value > 0) {
    uni.showModal({
      title: '未保存的修改',
      content: `还有 ${changedCount.value} 项价格未保存，确定离开？`,
      success: (res) => {
        if (res.confirm) uni.navigateBack()
      },
    })
    return
  }
  uni.navigateBack()
}

async function saveAll() {
  const changes = visibleProducts.value
    .map((item) => {
      const price = normalizePrice(priceDrafts[item.id] ?? '')
      if (price === undefined || !isChanged(item)) return null
      return { id: item.id, price }
    })
    .filter((item): item is { id: number; price: number | null } => item != null)

  if (changes.length === 0) {
    uni.showToast({ title: '没有需要保存的修改', icon: 'none' })
    return
  }

  const invalid = changes.find((item) => item.price === null)
  if (invalid) {
    uni.showToast({ title: '请填写有效价格', icon: 'none' })
    return
  }

  saving.value = true
  uni.showLoading({ title: '保存中' })
  try {
    let ok = 0
    for (const change of changes) {
      if (change.price == null) continue
      await updateBossProduct(change.id, { defaultPrice: change.price })
      const target = allProducts.value.find((p) => p.id === change.id)
      if (target) target.defaultPrice = change.price
      ok += 1
    }
    uni.showToast({ title: `已保存 ${ok} 项`, icon: 'success' })
  } catch (e) {
    uni.showToast({ title: e instanceof Error ? e.message : '保存失败', icon: 'none' })
  } finally {
    saving.value = false
    uni.hideLoading()
  }
}
</script>

<style scoped lang="scss">
@import '../../../../styles/boss-footer.scss';
@import '../../../../styles/boss-ui.scss';

.page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  overflow: hidden;
  box-sizing: border-box;
}

.top-bar {
  flex-shrink: 0;
  padding: 12rpx 20rpx 8rpx;
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

.hint-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12rpx;
  margin-top: 10rpx;
}

.hint-text {
  flex: 1;
  font-size: 22rpx;
  color: $boss-ink-muted;
  line-height: 1.4;
}

.only-unset {
  flex-shrink: 0;
  padding: 6rpx 16rpx;
  font-size: 22rpx;
  color: $boss-ink-secondary;
  background: $boss-bg;
  border-radius: $boss-radius-pill;
}

.only-unset.active {
  color: #1677ff;
  background: #eef5ff;
  font-weight: 600;
}

.stats-bar {
  margin-top: 8rpx;
  font-size: 22rpx;
  color: $boss-ink-muted;
}

.stat-changed {
  color: #1677ff;
  font-weight: 600;
}

.main {
  flex: none;
  height: calc(100vh - 174rpx);
  flex-direction: row;
  display: flex;
  min-height: 0;
  overflow: hidden;
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
  height: 100%;
  background: $boss-surface;
}

.product-scroll {
  flex: 1;
  min-height: 0;
  height: 0;
  box-sizing: border-box;
  padding-bottom: calc(128rpx + env(safe-area-inset-bottom));
}

.sub-cat-scroll {
  flex-shrink: 0;
  height: 82rpx;
  white-space: nowrap;
  border-bottom: 1rpx solid $boss-border;
}

.sub-cat-row {
  display: inline-flex;
  gap: 12rpx;
  padding: 14rpx 20rpx;
}

.sub-cat-chip {
  @include boss-chip(false);
  font-size: 26rpx;
}

.sub-cat-chip.active {
  @include boss-chip(true);
  font-size: 26rpx;
}

.cat-item {
  padding: 24rpx 14rpx;
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
  padding: 80rpx 40rpx;
  text-align: center;
}

.empty-text {
  font-size: 28rpx;
  color: $boss-ink-muted;
}

.price-list {
  padding: 0;
}

.list-head {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 12rpx;
  padding: 12rpx 20rpx;
  background: #fafafa;
  border-bottom: 1rpx solid $boss-border;
  font-size: 22rpx;
  color: $boss-ink-muted;
  z-index: 2;
}

.head-name {
  flex: 1;
  min-width: 0;
}

.head-yesterday {
  width: 88rpx;
  text-align: center;
  flex-shrink: 0;
}

.head-today {
  width: 200rpx;
  text-align: right;
  flex-shrink: 0;
  padding-right: 8rpx;
}

.price-row {
  display: flex;
  align-items: center;
  gap: 12rpx;
  padding: 18rpx 20rpx;
  border-bottom: 1rpx solid $boss-border;
}

.price-row.changed {
  background: #f0f7ff;
}

.row-main {
  flex: 1;
  min-width: 0;
}

.row-name {
  display: block;
  font-size: 28rpx;
  font-weight: 600;
  color: $boss-ink;
}

.row-alias {
  display: block;
  margin-top: 2rpx;
  font-size: 20rpx;
  color: $boss-ink-muted;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.row-yesterday {
  width: 88rpx;
  flex-shrink: 0;
  text-align: center;
}

.yesterday-value {
  font-size: 26rpx;
  color: $boss-ink-secondary;
  font-weight: 600;
}

.yesterday-empty {
  font-size: 26rpx;
  color: #ccc;
}

.row-input-wrap {
  width: 200rpx;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 6rpx;
}

.row-input {
  width: 112rpx;
  height: 64rpx;
  padding: 0 16rpx;
  text-align: right;
  font-size: 30rpx;
  font-weight: 700;
  color: $boss-price;
  background: #f7f8fa;
  border-radius: 12rpx;
  border: 2rpx solid transparent;
}

.price-row.changed .row-input {
  background: #fff;
  border-color: #1677ff;
}

.input-placeholder {
  color: #ccc;
  font-weight: 400;
}

.row-unit {
  font-size: 20rpx;
  color: $boss-ink-muted;
  white-space: nowrap;
  min-width: 56rpx;
}

.batch-bar {
  gap: 12rpx;

  .back-btn {
    flex: 0 0 120rpx;
    padding: 0 8rpx;
    font-size: 26rpx;
  }

  .copy-btn {
    flex: 1;
    min-width: 0;
    padding: 0 12rpx;
    font-size: 26rpx;
    color: #1677ff;
    background: #eef5ff;
    font-weight: 600;
  }

  .save-btn {
    flex: 0 0 160rpx;
    padding: 0 8rpx;
    font-size: 28rpx;
  }
}
</style>
