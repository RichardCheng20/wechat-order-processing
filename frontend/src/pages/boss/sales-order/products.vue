<template>
  <view class="page">
    <view class="search-box">
      <view class="search-wrap">
        <text class="search-icon">⌕</text>
        <input
          class="search-input"
          type="text"
          :value="keyword"
          placeholder="搜索商品名称/产品编号"
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
        >
          {{ cat.label }}
        </view>
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

        <scroll-view scroll-y class="products boss-list-scroll-fill">
        <view v-if="loading" class="loading-wrap">
          <u-loading-icon text="加载中" />
        </view>
        <view v-else-if="products.length === 0" class="empty-wrap">
          <text class="empty-title">{{ emptyTitle }}</text>
          <text class="empty-hint">{{ emptyHint }}</text>
          <view v-if="showGoManage" class="empty-btn" @tap="goNewProduct">去商品管理</view>
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
              <text v-if="showProductPrice(item)" class="product-price">
                {{ formatPrice(item.defaultPrice) }} 元/{{ formatSaleUnits(item) }}
              </text>
            </view>
            <view class="add-btn" :class="{ picked: cartCount(item.id) > 0 }" @tap.stop="openEntry(item)">+</view>
          </view>
        </view>
        </scroll-view>
      </view>
    </view>

    <view class="bottom-bar boss-bottom-bar">
      <view class="selected-wrap" @tap="toggleSelectedPanel">
        <text class="selected">已选{{ salesOrder.totalKinds }}种</text>
        <text class="selected-arrow">{{ showSelectedPanel ? '▲' : '▼' }}</text>
      </view>
      <view class="new-product boss-outline-btn" @tap="goNewProduct">新建商品</view>
      <button class="done-btn boss-primary-btn" @tap="handleDone">选好了</button>
    </view>

    <!-- 已选商品浮层 -->
    <view v-if="showSelectedPanel" class="selected-mask" @tap="showSelectedPanel = false">
      <view class="selected-panel" @tap.stop>
        <view class="selected-panel-head">
          <text>已选商品</text>
          <text class="selected-close" @tap="showSelectedPanel = false">×</text>
        </view>
        <scroll-view v-if="salesOrder.items.length" scroll-y class="selected-list">
          <view v-for="line in salesOrder.items" :key="line.lineKey" class="selected-line">
            <view class="selected-line-main">
              <text class="selected-name">{{ line.productName }}</text>
              <text class="selected-meta">{{ line.orderQty }}{{ line.unit }}<text v-if="line.dealPrice != null"> · ¥{{ line.dealPrice }}</text></text>
            </view>
            <text class="selected-del" @tap="removeLine(line.lineKey)">删除</text>
          </view>
        </scroll-view>
        <view v-else class="selected-empty">暂未选择商品</view>
      </view>
    </view>

    <!-- 单位选择 -->
    <u-popup :show="showUnitPicker" mode="bottom" round="16" @close="closeUnitPicker">
      <view class="unit-panel">
        <view class="unit-head">
          <text class="unit-cancel" @tap="closeUnitPicker">取消</text>
          <text class="unit-title">选择单位</text>
          <text class="unit-placeholder" />
        </view>
        <text v-if="entryProduct" class="unit-product">{{ entryProduct.name }}</text>
        <scroll-view scroll-y class="unit-grid-wrap">
          <view class="unit-grid">
            <view
              v-for="unit in filteredUnits"
              :key="unit"
              class="unit-chip"
              :class="{ active: pendingUnit === unit }"
              @tap="applyUnit(unit)"
            >
              {{ unit }}
            </view>
          </view>
        </scroll-view>
      </view>
    </u-popup>

    <!-- 商品下单（参考截图：弹层 + 内置键盘） -->
    <view v-if="showEntry && entryProduct" class="order-mask">
      <view class="order-sheet">
        <view class="order-head">
          <text class="order-close" @tap="closeEntry">×</text>
          <text class="order-title">商品下单</text>
          <view class="order-unit" @tap="openUnitFromEntry">
            <text>{{ entryUnit }}</text>
            <text class="order-unit-arrow">▼</text>
          </view>
        </view>

        <view class="order-product-row">
          <text class="order-product-name">{{ entryProduct.name }}</text>
        </view>

        <view class="order-fields">
          <view class="order-field" @tap="focusField('qty')">
            <text class="field-label">下单数 ({{ entryUnit }})</text>
            <view class="field-box" :class="{ active: activeField === 'qty' }">
              <text v-if="entryQty" class="field-value">{{ entryQty }}</text>
              <text v-else class="field-placeholder">0</text>
            </view>
          </view>
          <view class="order-field" @tap="focusField('price')">
            <text class="field-label">单价 (元/{{ entryUnit }})</text>
            <view class="field-box" :class="{ active: activeField === 'price' }">
              <text v-if="entryPrice" class="field-value">{{ entryPrice }}</text>
              <text v-else class="field-placeholder">选填</text>
            </view>
          </view>
        </view>

        <view class="order-footer-row">
          <text class="remark-link" @tap="openRemarkInput">添加备注</text>
          <text class="order-total">共：¥ {{ entryTotal }}</text>
        </view>
        <text v-if="entryRemark" class="order-remark-preview">备注：{{ entryRemark }}</text>

        <view class="keypad">
          <view class="keypad-grid">
            <view class="key" @tap="inputKey('1')">1</view>
            <view class="key" @tap="inputKey('2')">2</view>
            <view class="key" @tap="inputKey('3')">3</view>
            <view class="key fn" @tap="backspace">⌫</view>

            <view class="key" @tap="inputKey('4')">4</view>
            <view class="key" @tap="inputKey('5')">5</view>
            <view class="key" @tap="inputKey('6')">6</view>
            <view class="key fn" @tap="nextField">下一个</view>

            <view class="key" @tap="inputKey('7')">7</view>
            <view class="key" @tap="inputKey('8')">8</view>
            <view class="key" @tap="inputKey('9')">9</view>
            <view class="key confirm" @tap="confirmEntry">确定</view>

            <view class="key" @tap="inputKey('.')">.</view>
            <view class="key" @tap="inputKey('0')">0</view>
            <view class="key blank" />
            <view class="key blank" />
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { filterUnits, parseSaleUnits } from '@common/constants/units'
import { fetchBossCategories, fetchBossProducts, type CategoryItem, type ProductItem } from '@common/api/product'
import { buildPrimarySidebar, getParentCategory } from '@common/utils/category'
import { resolveMediaUrl } from '@common/utils/media'
import { useSalesOrderStore } from '@common/stores/salesOrder'
import { useUserStore } from '@common/stores/user'

type EntryField = 'qty' | 'price'

const userStore = useUserStore()
const salesOrder = useSalesOrderStore()
const loading = ref(false)
const keyword = ref('')
const categories = ref<CategoryItem[]>([])
const products = ref<ProductItem[]>([])
const activeCategoryKey = ref('all')
const subCategoryFilter = ref('all')
const showUnitPicker = ref(false)
const showEntry = ref(false)
const showSelectedPanel = ref(false)
const pendingUnit = ref('')
const unitKeyword = ref('')
const entryProduct = ref<ProductItem | null>(null)
const entryUnit = ref('斤')
const entryQty = ref('')
const entryPrice = ref('')
const entryRemark = ref('')
const activeField = ref<EntryField>('qty')

const emptyTitle = computed(() => {
  if (keyword.value.trim()) return '未找到匹配商品'
  if (activeCategoryKey.value !== 'all') return '该分类暂无在售商品'
  return '暂无可售商品'
})

const emptyHint = computed(() => {
  if (keyword.value.trim()) return '换个关键词试试'
  return '请先在商品管理中创建并上架商品'
})

const showGoManage = computed(() => !keyword.value.trim())

const categoryTabs = computed(() => buildPrimarySidebar(categories.value))

const subCategoryTabs = computed(() => {
  const parent = getParentCategory(categories.value, activeCategoryKey.value)
  if (!parent?.children?.length) return []
  return [
    { key: 'all', label: '全部' },
    ...parent.children.map((child) => ({ key: String(child.id), label: child.name })),
  ]
})

const filteredUnits = computed(() => {
  const product = entryProduct.value
  if (!product) return []
  const allowed = parseSaleUnits(product.saleUnits, product.unit)
  return filterUnits(allowed, unitKeyword.value)
})

const entryTotal = computed(() => {
  const qty = Number(entryQty.value || 0)
  const price = Number(entryPrice.value || 0)
  if (!qty || !price) return '0'
  return (qty * price).toFixed(2)
})

onShow(async () => {
  if (!userStore.isLoggedIn || !userStore.isBoss) {
    uni.reLaunch({ url: '/packages/common/login/index' })
    return
  }
  categories.value = await fetchBossCategories()
  await loadProducts()
})

function onKeywordInput(e: { detail: { value: string } }) {
  keyword.value = e.detail.value
}

function clearKeyword() {
  keyword.value = ''
  loadProducts()
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
  if (subCategoryFilter.value !== 'all') {
    return Number(subCategoryFilter.value)
  }
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

function formatPrice(value?: number) {
  if (value == null) return '0'
  return Number(value).toFixed(0)
}

function showProductPrice(item: ProductItem) {
  return item.defaultPrice != null && Number(item.defaultPrice) > 0
}

function cartCount(productId: number) {
  return salesOrder.items.filter((i) => i.productId === productId).length
}

function toggleSelectedPanel() {
  showSelectedPanel.value = !showSelectedPanel.value
}

function removeLine(lineKey: string) {
  salesOrder.removeLine(lineKey)
  if (salesOrder.items.length === 0) {
    showSelectedPanel.value = false
  }
}

function formatSaleUnits(item: ProductItem) {
  return parseSaleUnits(item.saleUnits, item.unit).join('/')
}

function openEntry(item: ProductItem) {
  const allowed = parseSaleUnits(item.saleUnits, item.unit)
  if (allowed.length > 1) {
    const existingLines = salesOrder.items.filter((i) => i.productId === item.id)
    const labels = allowed.map((unit) => {
      const line = existingLines.find((i) => i.unit === unit)
      return line ? `${unit}（已选${line.orderQty}）` : unit
    })
    uni.showActionSheet({
      itemList: labels,
      success: (res) => {
        openEntryWithUnit(item, allowed[res.tapIndex])
      },
    })
    return
  }
  openEntryWithUnit(item, allowed[0] || item.unit || '斤')
}

function openEntryWithUnit(item: ProductItem, unit: string) {
  const existing = salesOrder.items.find((i) => i.productId === item.id && i.unit === unit)
  entryProduct.value = item
  entryUnit.value = unit
  entryQty.value = existing ? String(existing.orderQty) : ''
  entryPrice.value = existing?.dealPrice != null
    ? String(existing.dealPrice)
    : (item.defaultPrice != null ? String(item.defaultPrice) : '')
  entryRemark.value = existing?.pickRemark || ''
  activeField.value = 'qty'
  showEntry.value = true
  showSelectedPanel.value = false
}

function closeEntry() {
  showEntry.value = false
  entryProduct.value = null
}

function focusField(field: EntryField) {
  activeField.value = field
}

function nextField() {
  activeField.value = activeField.value === 'qty' ? 'price' : 'qty'
}

function currentDraft() {
  return activeField.value === 'qty' ? entryQty : entryPrice
}

function inputKey(key: string) {
  const draft = currentDraft()
  if (key === '.' && draft.value.includes('.')) return
  if (draft.value === '0' && key !== '.') {
    draft.value = key
    return
  }
  draft.value += key
}

function backspace() {
  const draft = currentDraft()
  draft.value = draft.value.slice(0, -1)
}

function openUnitFromEntry() {
  const allowed = parseSaleUnits(entryProduct.value?.saleUnits, entryProduct.value?.unit)
  if (allowed.length <= 1) {
    uni.showToast({ title: '仅一种售卖单位', icon: 'none' })
    return
  }
  pendingUnit.value = entryUnit.value
  unitKeyword.value = ''
  showUnitPicker.value = true
}

function closeUnitPicker() {
  showUnitPicker.value = false
  pendingUnit.value = ''
  unitKeyword.value = ''
}

function applyUnit(unit: string) {
  entryUnit.value = unit
  closeUnitPicker()
}

function openRemarkInput() {
  uni.showModal({
    title: '分拣备注',
    editable: true,
    placeholderText: '如：要大一点的、小土豆',
    content: entryRemark.value,
    success: (res) => {
      if (res.confirm && res.content != null) {
        entryRemark.value = res.content.trim()
      }
    },
  })
}

function confirmEntry() {
  if (!entryProduct.value) return
  const qty = Number(entryQty.value)
  if (!qty || qty <= 0) {
    uni.showToast({ title: '请输入下单数量', icon: 'none' })
    activeField.value = 'qty'
    return
  }
  const price = entryPrice.value ? Number(entryPrice.value) : undefined
  salesOrder.upsertLine({
    productId: entryProduct.value.id,
    productName: entryProduct.value.name,
    unit: entryUnit.value,
    orderQty: qty,
    dealPrice: price,
    pickRemark: entryRemark.value.trim() || undefined,
  })
  closeEntry()
  uni.showToast({ title: '已添加', icon: 'success' })
}

function handleDone() {
  if (!salesOrder.customer?.name && !salesOrder.editOrderId) {
    uni.showToast({ title: '返回后请先选择客户', icon: 'none' })
  }
  uni.navigateBack()
}

function goNewProduct() {
  uni.navigateTo({ url: '/pages/boss/products/form' })
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
  box-sizing: border-box;
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
  padding: 0 64rpx 0 64rpx;
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

.main {
  flex: none;
  height: calc(100vh - 104rpx - 128rpx - env(safe-area-inset-bottom));
  min-height: 0;
  display: flex;
  flex-direction: row;
  align-items: stretch;
  overflow: hidden;
  box-sizing: border-box;
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

.sub-cat-scroll {
  flex-shrink: 0;
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

.products {
  flex: 1;
  height: 0;
  min-height: 0;
  background: #fff;
}

.product-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24rpx 24rpx;
  border-bottom: 1rpx solid #f0f0f0;
  gap: 16rpx;
  min-height: 96rpx;
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

.product-info {
  flex: 1;
  min-width: 0;
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

.product-price {
  display: block;
  margin-top: 8rpx;
  font-size: 26rpx;
  color: #999;
  line-height: 1.35;
}

.add-btn {
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

.bottom-bar {
  gap: 16rpx;
}

.selected-wrap {
  display: flex;
  align-items: center;
  gap: 6rpx;
  min-width: 140rpx;
}

.selected {
  font-size: 28rpx;
  color: #333;
}

.selected-arrow {
  font-size: 20rpx;
  color: #999;
}

.new-product {
  flex-shrink: 0;
  height: 88rpx;
  line-height: 84rpx;
  padding: 0 24rpx;
  font-size: 28rpx;
}

.done-btn {
  margin: 0;
}

.selected-mask {
  position: fixed;
  inset: 0;
  z-index: 90;
  background: rgba(0, 0, 0, 0.35);
}

.selected-panel {
  position: absolute;
  left: 0;
  right: 0;
  bottom: calc(128rpx + env(safe-area-inset-bottom));
  max-height: 50vh;
  background: #fff;
  border-radius: 24rpx 24rpx 0 0;
  padding: 24rpx 32rpx;
}

.selected-panel-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16rpx;
  font-size: 30rpx;
  font-weight: 600;
}

.selected-close {
  font-size: 40rpx;
  color: #999;
  padding: 0 8rpx;
}

.selected-list {
  max-height: 40vh;
}

.selected-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20rpx 0;
  border-bottom: 1rpx solid #f2f3f5;
}

.selected-name {
  display: block;
  font-size: 28rpx;
}

.selected-meta {
  display: block;
  margin-top: 6rpx;
  font-size: 24rpx;
  color: #999;
}

.selected-del {
  font-size: 26rpx;
  color: #e74c3c;
}

.selected-empty {
  padding: 40rpx 0;
  text-align: center;
  color: #999;
  font-size: 28rpx;
}

.loading-wrap,
.empty-wrap {
  padding: 80rpx 40rpx;
  text-align: center;
}

.empty-icon {
  display: flex;
  margin: 0 auto 18rpx;
  width: 84rpx;
  height: 84rpx;
}

.empty-title {
  display: block;
  font-size: 30rpx;
  color: #666;
  font-weight: 500;
}

.empty-hint {
  display: block;
  margin-top: 12rpx;
  font-size: 24rpx;
  color: #999;
  line-height: 1.5;
}

.empty-btn {
  display: inline-block;
  margin-top: 32rpx;
  padding: 18rpx 40rpx;
  font-size: 28rpx;
  color: #07c160;
  border: 2rpx solid #07c160;
  border-radius: 999rpx;
}

.unit-panel {
  max-height: 60vh;
  display: flex;
  flex-direction: column;
  background: #fff;
  padding-bottom: env(safe-area-inset-bottom);
}

.unit-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 28rpx 32rpx 12rpx;
}

.unit-cancel {
  min-width: 80rpx;
  font-size: 28rpx;
  color: #666;
}

.unit-title {
  font-size: 32rpx;
  font-weight: 600;
}

.unit-placeholder {
  min-width: 80rpx;
}

.unit-product {
  display: block;
  padding: 0 32rpx 16rpx;
  font-size: 28rpx;
  color: #666;
}

.unit-grid-wrap {
  max-height: 48vh;
  padding: 0 24rpx 24rpx;
}

.unit-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
}

.unit-chip {
  width: calc((100% - 32rpx) / 3);
  height: 80rpx;
  line-height: 80rpx;
  text-align: center;
  font-size: 28rpx;
  color: #333;
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

.unit-empty {
  padding: 80rpx 0;
  text-align: center;
  color: #999;
  font-size: 28rpx;
}

.order-mask {
  position: fixed;
  inset: 0;
  z-index: 200;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
}

.order-sheet {
  background: #fff;
  border-radius: 24rpx 24rpx 0 0;
  padding-bottom: env(safe-area-inset-bottom);
}

.order-head {
  display: flex;
  align-items: center;
  padding: 24rpx 32rpx 8rpx;
}

.order-close {
  width: 80rpx;
  font-size: 48rpx;
  color: #999;
  line-height: 1;
}

.order-title {
  flex: 1;
  text-align: center;
  font-size: 34rpx;
  font-weight: 600;
}

.order-unit {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 4rpx;
  min-width: 80rpx;
  font-size: 28rpx;
  color: #07c160;
}

.order-unit-arrow {
  font-size: 18rpx;
}

.order-product-row {
  padding: 8rpx 32rpx 20rpx;
}

.order-product-name {
  font-size: 30rpx;
  color: #666;
}

.order-fields {
  display: flex;
  gap: 20rpx;
  padding: 0 32rpx;
}

.order-field {
  flex: 1;
}

.field-label {
  display: block;
  margin-bottom: 12rpx;
  font-size: 24rpx;
  color: #999;
}

.field-box {
  height: 96rpx;
  padding: 0 24rpx;
  display: flex;
  align-items: center;
  background: #f5f6f7;
  border-radius: 12rpx;
  border: 2rpx solid transparent;
}

.field-box.active {
  background: #fff;
  border-color: #07c160;
}

.field-value {
  font-size: 40rpx;
  font-weight: 600;
  color: #222;
}

.field-placeholder {
  font-size: 32rpx;
  color: #ccc;
}

.order-footer-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24rpx 32rpx 8rpx;
}

.remark-link {
  font-size: 28rpx;
  color: #666;
}

.order-total {
  font-size: 30rpx;
  color: #333;
}

.order-remark-preview {
  display: block;
  padding: 0 32rpx 16rpx;
  font-size: 24rpx;
  color: #e67e22;
}

.keypad {
  background: #eef0f2;
  padding: 12rpx;
  border-top: 1rpx solid #ddd;
}

.keypad-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  grid-template-rows: repeat(4, 96rpx);
  gap: 8rpx;
}

.key {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
  border-radius: 8rpx;
  font-size: 36rpx;
  font-weight: 500;
}

.key.fn {
  font-size: 26rpx;
  color: #666;
}

.key.confirm {
  grid-row: 3 / 5;
  grid-column: 4;
  background: #07c160;
  color: #fff;
  font-size: 32rpx;
  font-weight: 600;
}

.key.blank {
  visibility: hidden;
  pointer-events: none;
}
</style>
