<template>
  <view class="page">
    <view class="top-hint">
      <text class="hint-supplier">{{ purchaseOrder.supplierName || '未选供应商' }}</text>
      <text class="hint-date">收货 {{ receiveLabel }}</text>
    </view>

    <view class="search-box">
      <view class="search-wrap">
        <text class="search-icon">⌕</text>
        <input
          class="search-input"
          type="text"
          :value="keyword"
          placeholder="搜索商品名称"
          confirm-type="search"
          @input="onKeywordInput"
          @confirm="loadProducts"
        />
        <text v-if="keyword" class="search-clear" @tap="clearKeyword">×</text>
      </view>
    </view>

    <view class="main">
      <scroll-view scroll-y class="categories">
        <view
          v-for="cat in categoryTabs"
          :key="cat.key"
          class="cat-item"
          :class="{ active: activeCategoryKey === cat.key }"
          @tap="switchCategory(cat.key)"
        >{{ cat.label }}</view>
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
              @tap="switchSubCategory(tab.key)"
            >{{ tab.label }}</text>
          </view>
        </scroll-view>

        <scroll-view scroll-y class="products">
          <view v-if="loading" class="loading-wrap">
            <u-loading-icon text="加载中" />
          </view>
          <view v-else-if="products.length === 0" class="empty-wrap">
            <text class="empty-title">{{ emptyTitle }}</text>
          </view>
          <view v-else class="product-list">
            <view
              v-for="item in products"
              :key="item.id"
              class="product-row"
              :class="{ selected: cartCount(item.id) > 0 }"
              @tap="openEntry(item)"
            >
              <image
                v-if="item.imageUrl"
                class="product-thumb"
                :src="resolveMediaUrl(item.imageUrl)"
                mode="aspectFill"
              />
              <view class="product-info">
                <text class="product-name">{{ item.name }}</text>
                <text v-if="needHint(item.id)" class="product-ref need">需采 {{ needHint(item.id) }}</text>
                <text v-if="priceHint(item.id)" class="product-ref">进价 {{ priceHint(item.id) }}</text>
              </view>
              <view class="add-btn" :class="{ picked: cartCount(item.id) > 0 }" @tap.stop="openEntry(item)">+</view>
            </view>
          </view>
        </scroll-view>
      </view>
    </view>

    <view class="bottom-bar boss-bottom-bar">
      <view class="import-btn boss-outline-btn" @tap="importNeedItems">带入需采购</view>
      <view class="selected-wrap" @tap="toggleSelectedPanel">
        <text class="selected">已选{{ purchaseOrder.totalKinds }}种</text>
        <text class="selected-arrow">{{ showSelectedPanel ? '▲' : '▼' }}</text>
      </view>
      <button class="done-btn boss-primary-btn" @tap="handleDone">选好了</button>
    </view>

    <view v-if="showSelectedPanel" class="selected-mask" @tap="showSelectedPanel = false">
      <view class="selected-panel" @tap.stop>
        <view class="selected-panel-head">
          <text>已选商品</text>
          <text class="selected-close" @tap="showSelectedPanel = false">×</text>
        </view>
        <scroll-view v-if="purchaseOrder.items.length" scroll-y class="selected-list">
          <view v-for="line in purchaseOrder.items" :key="line.lineKey" class="selected-line">
            <view class="selected-line-main">
              <text class="selected-name">{{ line.productName }}</text>
              <text class="selected-meta">
                {{ line.qty }}{{ line.unit }}
                <text v-if="line.purchasePrice != null"> · ¥{{ line.purchasePrice }}</text>
              </text>
            </view>
            <text class="selected-del" @tap="removeLine(line.lineKey)">删除</text>
          </view>
        </scroll-view>
        <view v-else class="selected-empty">暂未选择商品</view>
      </view>
    </view>

    <view v-if="showEntry && entryProduct" class="order-mask">
      <view class="order-sheet">
        <view class="order-head">
          <text class="order-close" @tap="closeEntry">×</text>
          <text class="order-title">采购数量</text>
          <text class="order-unit">{{ entryUnit }}</text>
        </view>
        <text class="order-product">{{ entryProduct.name }}</text>
        <text v-if="entryNeedRef > 0" class="order-ref">参考需采购：{{ formatQty(entryNeedRef) }}{{ entryUnit }}</text>
        <view class="order-fields">
          <view class="field-row" :class="{ active: activeField === 'qty' }" @tap="activeField = 'qty'">
            <text class="field-label">数量</text>
            <text class="field-value">{{ entryQty || '0' }}</text>
          </view>
          <view class="field-row" :class="{ active: activeField === 'price' }" @tap="activeField = 'price'">
            <text class="field-label">进价</text>
            <text class="field-value">{{ entryPrice || '—' }}</text>
          </view>
        </view>
        <view class="keypad">
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
              <view class="key fn" @tap="clearDraft">清零</view>
            </view>
            <view class="key-row">
              <view class="key" @tap="inputKey('7')">7</view>
              <view class="key" @tap="inputKey('8')">8</view>
              <view class="key" @tap="inputKey('9')">9</view>
              <view class="key confirm-side" @tap="confirmEntry">确定</view>
            </view>
            <view class="key-row">
              <view class="key wide" @tap="inputKey('0')">0</view>
              <view class="key" @tap="inputKey('.')">.</view>
            </view>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { fetchProcurementTasks } from '@common/api/procurement'
import { fetchBossCategories, fetchBossProducts, type CategoryItem, type ProductItem } from '@common/api/product'
import { formatReceiveLabel, usePurchaseOrderStore } from '@common/stores/purchaseOrder'
import { useUserStore } from '@common/stores/user'
import { buildPrimarySidebar, getParentCategory } from '@common/utils/category'
import { resolveMediaUrl } from '@common/utils/media'

type EntryField = 'qty' | 'price'

interface NeedRef {
  needQty: number
  purchasePrice?: number
  unit: string
}

const userStore = useUserStore()
const purchaseOrder = usePurchaseOrderStore()

const categories = ref<CategoryItem[]>([])
const products = ref<ProductItem[]>([])
const needRefMap = ref<Record<number, NeedRef>>({})
const keyword = ref('')
const loading = ref(false)
const activeCategoryKey = ref('all')
const subCategoryFilter = ref('all')
const showSelectedPanel = ref(false)
const showEntry = ref(false)
const entryProduct = ref<ProductItem | null>(null)
const entryUnit = ref('斤')
const entryQty = ref('')
const entryPrice = ref('')
const entryNeedRef = ref(0)
const activeField = ref<EntryField>('qty')

const receiveLabel = computed(() => formatReceiveLabel(purchaseOrder.receiveDay))

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
  return '该分类暂无在售商品'
})

onShow(async () => {
  if (!userStore.isLoggedIn || !userStore.isBoss) {
    uni.reLaunch({ url: '/pages/login/index' })
    return
  }
  if (!purchaseOrder.hasSupplier) {
    uni.showToast({ title: '请先选择供应商', icon: 'none' })
    setTimeout(() => uni.navigateBack(), 800)
    return
  }
  categories.value = await fetchBossCategories()
  await Promise.all([loadProducts(), loadNeedRefs()])
})

async function loadNeedRefs() {
  try {
    const task = await fetchProcurementTasks({ receiveDate: purchaseOrder.receiveDate })
    const map: Record<number, NeedRef> = {}
    for (const item of task.items || []) {
      if (item.customItem) continue
      map[item.productId] = {
        needQty: item.needQty,
        purchasePrice: item.purchasePrice,
        unit: item.unit,
      }
    }
    needRefMap.value = map
  } catch {
    needRefMap.value = {}
  }
}

async function loadProducts() {
  loading.value = true
  try {
    if (activeCategoryKey.value === 'uncategorized') {
      products.value = (await fetchBossProducts({ keyword: keyword.value || undefined }) || [])
        .filter((item) => !item.categoryId && item.saleStatus === 'ON')
      return
    }
    const categoryId = resolveCategoryId()
    const all = await fetchBossProducts({ categoryId, keyword: keyword.value || undefined }) || []
    products.value = all.filter((item) => item.saleStatus === 'ON')
  } catch {
    products.value = []
  } finally {
    loading.value = false
  }
}

function resolveCategoryId(): number | undefined {
  if (activeCategoryKey.value === 'all') return undefined
  if (subCategoryFilter.value !== 'all') return Number(subCategoryFilter.value)
  if (activeCategoryKey.value === 'uncategorized') return undefined
  return Number(activeCategoryKey.value)
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

function needHint(productId: number) {
  const ref = needRefMap.value[productId]
  if (!ref || ref.needQty <= 0) return ''
  return `${formatQty(ref.needQty)}${ref.unit}`
}

function priceHint(productId: number) {
  const ref = needRefMap.value[productId]
  if (!ref?.purchasePrice) return ''
  return `¥${Number(ref.purchasePrice).toFixed(2)}/${ref.unit}`
}

function cartCount(productId: number) {
  return purchaseOrder.items.filter((i) => i.productId === productId).length
}

function openEntry(item: ProductItem) {
  const ref = needRefMap.value[item.id]
  const unit = ref?.unit || item.unit || '斤'
  const existing = purchaseOrder.items.find((i) => i.productId === item.id && i.unit === unit)
  entryProduct.value = item
  entryUnit.value = unit
  entryNeedRef.value = ref?.needQty || 0
  entryQty.value = existing
    ? String(existing.qty)
    : (ref?.needQty && ref.needQty > 0 ? String(ref.needQty) : '')
  entryPrice.value = existing?.purchasePrice != null
    ? String(existing.purchasePrice)
    : (ref?.purchasePrice != null ? String(ref.purchasePrice) : '')
  activeField.value = 'qty'
  showEntry.value = true
  showSelectedPanel.value = false
}

function closeEntry() {
  showEntry.value = false
  entryProduct.value = null
}

function currentDraft() {
  return activeField.value === 'qty' ? entryQty : entryPrice
}

function inputKey(key: string) {
  const draft = currentDraft()
  if (key === '.' && draft.value.includes('.')) return
  if (draft.value === '0' && key !== '.') draft.value = key
  else draft.value += key
}

function backspace() {
  const draft = currentDraft()
  draft.value = draft.value.slice(0, -1)
}

function clearDraft() {
  currentDraft().value = ''
}

function confirmEntry() {
  if (!entryProduct.value) return
  const qty = Number(entryQty.value)
  if (!qty || qty <= 0) {
    uni.showToast({ title: '请输入采购数量', icon: 'none' })
    activeField.value = 'qty'
    return
  }
  const price = entryPrice.value ? Number(entryPrice.value) : undefined
  purchaseOrder.upsertLine({
    productId: entryProduct.value.id,
    productName: entryProduct.value.name,
    unit: entryUnit.value,
    qty,
    purchasePrice: price,
    needQtyRef: entryNeedRef.value > 0 ? entryNeedRef.value : undefined,
  })
  closeEntry()
  uni.showToast({ title: '已添加', icon: 'success' })
}

function toggleSelectedPanel() {
  showSelectedPanel.value = !showSelectedPanel.value
}

function removeLine(lineKey: string) {
  purchaseOrder.removeLine(lineKey)
  if (purchaseOrder.items.length === 0) showSelectedPanel.value = false
}

function importNeedItems() {
  fetchProcurementTasks({ receiveDate: purchaseOrder.receiveDate })
    .then((task) => {
      const rows = (task.items || [])
        .filter((item) => !item.customItem && item.needQty > 0)
        .map((item) => ({
          productId: item.productId,
          productName: item.productName,
          unit: item.unit,
          needQty: item.needQty,
          purchasePrice: item.purchasePrice,
        }))
      if (rows.length === 0) {
        uni.showToast({ title: '当前收货日无需采购商品', icon: 'none' })
        return
      }
      purchaseOrder.importNeedItems(rows)
      uni.showToast({ title: `已带入 ${rows.length} 种`, icon: 'success' })
    })
    .catch(() => {
      uni.showToast({ title: '加载采购任务失败', icon: 'none' })
    })
}

function handleDone() {
  if (purchaseOrder.totalKinds === 0) {
    uni.showToast({ title: '请先选择商品', icon: 'none' })
    return
  }
  uni.navigateTo({ url: '/pages/boss/purchase-order/confirm' })
}

function formatQty(value: number) {
  return Number(value).toFixed(2).replace(/\.?0+$/, '') || '0'
}
</script>

<style scoped lang="scss">
@import '../../../styles/boss-footer.scss';

.page {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 100vh;
  overflow: hidden;
  background: #fff;
}

.top-hint {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12rpx 24rpx;
  background: #fff7e6;
  font-size: 24rpx;
  flex-shrink: 0;
}

.hint-supplier {
  color: #333;
  font-weight: 500;
}

.hint-date {
  color: #e67e22;
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
}

.search-clear {
  position: absolute;
  right: 20rpx;
  font-size: 32rpx;
  color: #bbb;
}

.main {
  flex: 1;
  min-height: 0;
  display: flex;
  overflow: hidden;
}

.categories {
  width: 168rpx;
  flex-shrink: 0;
  background: #f5f6f7;
  height: 100%;
}

.cat-item {
  padding: 32rpx 12rpx;
  font-size: 26rpx;
  color: #666;
  text-align: center;
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
}

.sub-cat-scroll {
  flex-shrink: 0;
  border-bottom: 1rpx solid #f0f0f0;
}

.sub-cat-row {
  display: inline-flex;
  gap: 16rpx;
  padding: 16rpx 20rpx;
}

.sub-cat-chip {
  padding: 10rpx 24rpx;
  font-size: 26rpx;
  color: #666;
  background: #f5f6f7;
  border-radius: 999rpx;
}

.sub-cat-chip.active {
  color: #07c160;
  background: #e8f8ef;
}

.products {
  flex: 1;
  height: 0;
}

.product-row {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 24rpx;
  border-bottom: 1rpx solid #f0f0f0;
}

.product-row.selected {
  background: #f8fcf9;
}

.product-thumb {
  width: 72rpx;
  height: 72rpx;
  border-radius: 14rpx;
  background: #f5f6f7;
  flex-shrink: 0;
}

.product-info {
  flex: 1;
  min-width: 0;
}

.product-name {
  display: block;
  font-size: 30rpx;
  font-weight: 500;
  color: #222;
}

.product-ref {
  display: block;
  margin-top: 6rpx;
  font-size: 24rpx;
  color: #999;
}

.product-ref.need {
  color: #e67e22;
}

.add-btn {
  width: 60rpx;
  height: 60rpx;
  border-radius: 50%;
  background: #07c160;
  color: #fff;
  font-size: 44rpx;
  line-height: 56rpx;
  text-align: center;
  flex-shrink: 0;
}

.add-btn.picked {
  background: #05a850;
}

.bottom-bar {
  gap: 12rpx;
}

.import-btn {
  flex-shrink: 0;
  height: 88rpx;
  line-height: 84rpx;
  padding: 0 16rpx;
  font-size: 24rpx;
}

.selected-wrap {
  display: flex;
  align-items: center;
  gap: 6rpx;
  min-width: 120rpx;
}

.selected {
  font-size: 26rpx;
  color: #333;
}

.selected-arrow {
  font-size: 20rpx;
  color: #999;
}

.done-btn {
  margin: 0;
}

.loading-wrap,
.empty-wrap {
  padding: 80rpx 24rpx;
  text-align: center;
  color: #999;
}

.selected-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  z-index: 100;
  display: flex;
  align-items: flex-end;
}

.selected-panel {
  width: 100%;
  max-height: 60vh;
  background: #fff;
  border-radius: 24rpx 24rpx 0 0;
  padding: 24rpx;
}

.selected-panel-head {
  display: flex;
  justify-content: space-between;
  font-size: 30rpx;
  font-weight: 600;
}

.selected-close {
  font-size: 40rpx;
  color: #999;
}

.selected-list {
  max-height: 45vh;
  margin-top: 16rpx;
}

.selected-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16rpx 0;
  border-bottom: 1rpx solid #f0f0f0;
}

.selected-name {
  font-size: 28rpx;
  color: #111;
}

.selected-meta {
  display: block;
  margin-top: 4rpx;
  font-size: 24rpx;
  color: #888;
}

.selected-del {
  color: #f56c6c;
  font-size: 26rpx;
}

.selected-empty {
  padding: 40rpx 0;
  text-align: center;
  color: #999;
}

.order-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  z-index: 200;
  display: flex;
  align-items: flex-end;
}

.order-sheet {
  width: 100%;
  background: #fff;
  border-radius: 24rpx 24rpx 0 0;
  padding: 24rpx 24rpx calc(24rpx + env(safe-area-inset-bottom));
}

.order-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.order-close {
  font-size: 40rpx;
  color: #999;
}

.order-title {
  font-size: 32rpx;
  font-weight: 600;
}

.order-unit {
  font-size: 26rpx;
  color: #07c160;
}

.order-product {
  display: block;
  margin-top: 16rpx;
  font-size: 30rpx;
  font-weight: 500;
}

.order-ref {
  display: block;
  margin-top: 8rpx;
  font-size: 24rpx;
  color: #e67e22;
}

.order-fields {
  display: flex;
  gap: 16rpx;
  margin: 24rpx 0;
}

.field-row {
  flex: 1;
  padding: 20rpx;
  background: #f5f6f7;
  border-radius: 12rpx;
  border: 2rpx solid transparent;
}

.field-row.active {
  border-color: #07c160;
  background: #f0fdf4;
}

.field-label {
  display: block;
  font-size: 22rpx;
  color: #999;
}

.field-value {
  display: block;
  margin-top: 8rpx;
  font-size: 36rpx;
  font-weight: 600;
  color: #111;
}

.key-row {
  display: flex;
  gap: 8rpx;
  margin-bottom: 8rpx;
}

.key {
  flex: 1;
  height: 88rpx;
  line-height: 88rpx;
  text-align: center;
  background: #f5f6f7;
  border-radius: 8rpx;
  font-size: 34rpx;
}

.key.wide {
  flex: 2.05;
}

.key.fn {
  font-size: 26rpx;
  color: #666;
}

.confirm-side {
  flex: 1;
  height: 88rpx;
  line-height: 88rpx;
  text-align: center;
  background: #07c160;
  color: #fff;
  border-radius: 8rpx;
  font-size: 28rpx;
}
</style>
