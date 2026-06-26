<template>
  <view class="page" :class="{ 'with-keypad': showKeypad }">
    <view class="top-bar">
      <view class="search-wrap">
        <AppIcon class="search-icon" name="search" tone="green" :tile="false" :size="18" />
        <input
          v-model="keyword"
          class="search-input"
          placeholder="搜索商品"
          confirm-type="search"
          @confirm="loadProducts"
        />
      </view>
      <view v-if="products.length > 0" class="summary-bar">
        <text>共 {{ products.length }} 种商品 · 点击仓存可修改</text>
        <text class="help-btn" @tap.stop="showStockHelp">?</text>
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
          <u-empty mode="data" text="暂无商品" />
        </view>

        <scroll-view v-else scroll-y class="product-scroll" :show-scrollbar="false">
          <view
            v-for="item in displayItems"
            :key="item.id"
            class="card"
            @tap="openEditor(item)"
          >
            <view class="card-head">
              <text class="name">{{ item.name }}</text>
            </view>
            <view class="stock-row" @tap.stop="openEditor(item)">
              <view class="stock-main">
                <text class="stock-label">仓存</text>
                <text class="stock-value">{{ formatQty(item.physicalStockQty) }}{{ item.unit }}</text>
              </view>
              <view v-if="item.reservedQty && item.reservedQty > 0" class="stock-sub">
                可用 {{ formatQty(item.availableStockQty) }} · 占用 {{ formatQty(item.reservedQty) }}
              </view>
              <text class="edit-hint">点击修改</text>
            </view>
          </view>
          <view class="list-tail">—— 没有更多了 ——</view>
        </scroll-view>
      </view>
    </view>

    <view v-if="showKeypad" class="keypad-wrap">
      <view class="edit-context-bar">
        <text class="edit-title">{{ editingItem?.name }} · 实物库存</text>
        <text class="edit-qty">{{ stockDraft || '0' }} {{ editingItem?.unit }}</text>
      </view>
      <view class="keypad-toggle" @tap="closeKeypad">收起键盘 ⌄</view>
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
          <view class="key confirm-side" @tap="confirmSave">保存</view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { fetchInventoryProducts, updateInventoryStock, type InventoryProduct } from '@common/api/inventory'
import { fetchBossCategories, type CategoryItem } from '@common/api/product'
import AppIcon from '@/components/AppIcon.vue'
import { useUserStore } from '@common/stores/user'
import { buildPrimarySidebar, getParentCategory } from '@common/utils/category'

const userStore = useUserStore()
const products = ref<InventoryProduct[]>([])
const categories = ref<CategoryItem[]>([])
const loading = ref(false)
const saving = ref(false)
const keyword = ref('')
const categoryFilter = ref('all')
const subCategoryFilter = ref('all')
const showKeypad = ref(false)
const stockDraft = ref('')
const editingItem = ref<InventoryProduct | null>(null)

const sidebarItems = computed(() => buildPrimarySidebar(categories.value))
const activeParent = computed(() => getParentCategory(categories.value, categoryFilter.value))
const subCategoryTabs = computed(() => {
  const parent = activeParent.value
  if (!parent?.children?.length) return []
  return [{ key: 'all', label: '全部' }, ...parent.children.map((c) => ({ key: String(c.id), label: c.name }))]
})

const displayItems = computed(() => {
  let items = products.value
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
  try {
    categories.value = await fetchBossCategories()
  } catch {
    categories.value = []
  }
  await loadProducts()
})

async function loadProducts() {
  loading.value = true
  try {
    products.value = await fetchInventoryProducts({
      keyword: keyword.value.trim() || undefined,
      categoryId: resolveCategoryId(),
    })
  } catch (e) {
    products.value = []
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
  loadProducts()
}

function openEditor(item: InventoryProduct) {
  editingItem.value = item
  stockDraft.value = String(item.physicalStockQty ?? 0)
  showKeypad.value = true
}

function closeKeypad() {
  showKeypad.value = false
  editingItem.value = null
}

function inputKey(key: string) {
  let val = stockDraft.value
  if (key === '.' && val.includes('.')) return
  if (val === '0' && key !== '.') val = key
  else val += key
  stockDraft.value = val
}

function backspace() {
  stockDraft.value = stockDraft.value.slice(0, -1)
}

function clearDraft() {
  stockDraft.value = ''
}

async function confirmSave() {
  if (saving.value || !editingItem.value) return
  const stockQty = Number(stockDraft.value || 0)
  if (stockQty < 0) {
    uni.showToast({ title: '库存不能为负数', icon: 'none' })
    return
  }
  saving.value = true
  try {
    const updated = await updateInventoryStock(editingItem.value.id, stockQty)
    const idx = products.value.findIndex((p) => p.id === updated.id)
    if (idx >= 0) {
      products.value[idx] = updated
    }
    closeKeypad()
    uni.showToast({ title: '库存已更新', icon: 'success' })
  } catch (e) {
    uni.showToast({ title: e instanceof Error ? e.message : '保存失败', icon: 'none' })
  } finally {
    saving.value = false
  }
}

function showStockHelp() {
  uni.showModal({
    title: '仓存说明',
    content: '仓存为仓库实物库存。修改后可用库存 = 仓存 − 订单占用。员工可在此盘点并更新数量。',
    showCancel: false,
  })
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
  box-sizing: border-box;
}

.page.with-keypad {
  padding-bottom: calc(520rpx + env(safe-area-inset-bottom));
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

.summary-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12rpx;
  margin: 0 -20rpx;
  padding: 16rpx 20rpx;
  background: #ecfdf3;
  color: #059669;
  font-size: 26rpx;
  border-top: 1rpx solid #d1fae5;
}

.help-btn {
  width: 32rpx;
  height: 32rpx;
  line-height: 32rpx;
  text-align: center;
  font-size: 22rpx;
  color: #059669;
  background: #fff;
  border-radius: 50%;
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
  height: calc(100vh - 320rpx);
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
  height: calc(100vh - 260rpx);
}

.card {
  margin: 16rpx;
  padding: 24rpx;
  background: #fff;
  border: 1rpx solid #f0f0f0;
  border-radius: 16rpx;
}

.card-head {
  margin-bottom: 16rpx;
}

.name {
  font-size: 32rpx;
  font-weight: 700;
  color: #111;
}

.stock-row {
  padding: 20rpx;
  background: #ecfdf3;
  border-radius: 12rpx;
}

.stock-main {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
}

.stock-label {
  font-size: 26rpx;
  color: #666;
}

.stock-value {
  font-size: 40rpx;
  font-weight: 700;
  color: #059669;
}

.stock-sub {
  display: block;
  margin-top: 8rpx;
  font-size: 22rpx;
  color: #888;
}

.edit-hint {
  display: block;
  margin-top: 8rpx;
  font-size: 22rpx;
  color: #22c55e;
}

.list-tail {
  padding: 24rpx;
  text-align: center;
  font-size: 24rpx;
  color: #ccc;
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

.edit-context-bar {
  padding: 20rpx 24rpx;
  background: #fff;
  border-bottom: 1rpx solid #e8e8e8;
}

.edit-title {
  display: block;
  font-size: 26rpx;
  color: #666;
}

.edit-qty {
  display: block;
  margin-top: 8rpx;
  font-size: 40rpx;
  font-weight: 700;
  color: #22c55e;
}

.keypad-toggle {
  text-align: center;
  padding: 12rpx;
  color: #666;
  font-size: 26rpx;
  background: #f7f8f9;
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

.key {
  flex: 1;
  height: 88rpx;
  line-height: 88rpx;
  text-align: center;
  background: #fff;
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
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #22c55e;
  color: #fff;
  font-size: 30rpx;
  font-weight: 600;
  border-radius: 8rpx;
}
</style>
