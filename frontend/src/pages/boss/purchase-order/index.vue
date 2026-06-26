<template>
  <view class="page boss-page">
    <view class="header">
      <text class="title">采购开单</text>
      <text class="subtitle">选择供应商与商品，生成采购单图片发给供应商</text>
    </view>

    <view class="supplier-card">
      <view class="supplier-row">
        <view class="supplier-input-wrap">
          <input
            class="supplier-input"
            type="text"
            :value="supplierKeyword"
            placeholder="输入或选择供应商"
            confirm-type="done"
            @input="onSupplierNameInput"
            @blur="syncSupplierFromKeyword"
            @confirm="syncSupplierFromKeyword"
          />
        </view>
        <view class="arrow-btn" @tap.stop="openSupplierPicker">
          <text class="arrow">›</text>
        </view>
      </view>
      <view v-if="showSupplierSuggestions" class="supplier-suggestions">
        <view
          v-for="item in suggestionSuppliers"
          :key="item.id"
          class="suggestion-item"
          @tap="selectSupplier(item)"
        >
          <text class="suggestion-name">{{ item.name }}</text>
        </view>
        <view v-if="canUseNewSupplier" class="suggestion-item new-supplier" @tap="useNewSupplierName">
          使用「{{ supplierKeyword.trim() }}」新建供应商
        </view>
      </view>
      <view class="delivery-row">
        <text class="delivery-label">收货：{{ receiveLabel }}</text>
        <view class="day-tabs">
          <text class="day-tab" :class="{ active: purchaseOrder.receiveDay === 'today' }" @tap="setReceiveDay('today')">今日</text>
          <text class="day-tab" :class="{ active: purchaseOrder.receiveDay === 'tomorrow' }" @tap="setReceiveDay('tomorrow')">明日</text>
        </view>
      </view>
    </view>

    <view class="summary-card">
      <view class="summary-row">
        <text class="summary-label">已选商品</text>
        <text class="summary-value">{{ purchaseOrder.totalKinds }} 种</text>
      </view>
      <view class="summary-row">
        <text class="summary-label">预估金额</text>
        <text class="summary-value amount">¥{{ formatMoney(purchaseOrder.totalAmount) }}</text>
      </view>
      <text class="summary-hint">选品时可参考采购任务页的「需采购」数量</text>
    </view>

    <view class="action-card">
      <button class="action-btn primary" :disabled="!purchaseOrder.hasSupplier" @tap="goProducts">
        选择采购商品
      </button>
      <button
        class="action-btn outline"
        :disabled="!canPreview"
        @tap="goConfirm"
      >
        预览采购单
      </button>
      <button v-if="purchaseOrder.totalKinds > 0" class="action-btn text" @tap="clearItems">
        清空已选
      </button>
    </view>

    <u-popup :show="showSupplierPicker" mode="bottom" round="16" @close="closeSupplierPicker">
      <view class="picker-panel">
        <view class="picker-head">
          <text class="picker-title">选择供应商</text>
          <text class="picker-close" @tap="closeSupplierPicker">×</text>
        </view>
        <view class="picker-search-wrap">
          <input
            class="picker-search"
            type="text"
            :value="pickerKeyword"
            placeholder="搜索供应商"
            @input="onPickerKeywordInput"
          />
        </view>
        <scroll-view scroll-y class="supplier-list">
          <view
            v-if="canUseNewSupplierInPicker"
            class="supplier-option new-supplier"
            @tap="useNewSupplierFromPicker"
          >
            新建「{{ pickerKeyword.trim() }}」
          </view>
          <view
            v-for="item in pickerSuppliers"
            :key="item.id"
            class="supplier-option"
            @tap="selectSupplier(item)"
          >
            <text class="supplier-option-name">{{ item.name }}</text>
          </view>
        </scroll-view>
      </view>
    </u-popup>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { fetchBossSuppliers, type SupplierItem } from '@common/api/supplier'
import { formatReceiveLabel, usePurchaseOrderStore } from '@common/stores/purchaseOrder'
import { useUserStore } from '@common/stores/user'

const userStore = useUserStore()
const purchaseOrder = usePurchaseOrderStore()

const supplierKeyword = ref('')
const showSupplierPicker = ref(false)
const pickerKeyword = ref('')
const allSuppliers = ref<SupplierItem[]>([])
const filteredSuppliers = ref<SupplierItem[]>([])

const receiveLabel = computed(() => formatReceiveLabel(purchaseOrder.receiveDay))

const canUseNewSupplier = computed(() => {
  const name = supplierKeyword.value.trim()
  return !!name && !allSuppliers.value.some((s) => s.name === name)
})

const showSupplierSuggestions = computed(() => {
  const kw = supplierKeyword.value.trim()
  if (!kw) return false
  return filteredSuppliers.value.length > 0 || canUseNewSupplier.value
})

const suggestionSuppliers = computed(() => filteredSuppliers.value.slice(0, 8))

const pickerSuppliers = computed(() => {
  const kw = pickerKeyword.value.trim().toLowerCase()
  if (!kw) return allSuppliers.value
  return allSuppliers.value.filter((s) => s.name.toLowerCase().includes(kw))
})

const canUseNewSupplierInPicker = computed(() => {
  const name = pickerKeyword.value.trim()
  return !!name && !allSuppliers.value.some((s) => s.name === name)
})

const canPreview = computed(
  () => purchaseOrder.hasSupplier && purchaseOrder.totalKinds > 0,
)

onShow(async () => {
  if (!userStore.isLoggedIn || !userStore.isBoss) {
    uni.reLaunch({ url: '/pages/login/index' })
    return
  }
  allSuppliers.value = await fetchBossSuppliers()
  filterSuppliers()
  if (purchaseOrder.supplier?.name) {
    supplierKeyword.value = purchaseOrder.supplier.name
  }
})

function filterSuppliers() {
  const kw = supplierKeyword.value.trim().toLowerCase()
  if (!kw) {
    filteredSuppliers.value = allSuppliers.value
    return
  }
  filteredSuppliers.value = allSuppliers.value.filter((s) => {
    const hay = `${s.name} ${s.contactName || ''} ${s.phone || ''}`.toLowerCase()
    return hay.includes(kw)
  })
}

function onSupplierNameInput(e: { detail: { value: string } }) {
  supplierKeyword.value = e.detail.value
  filterSuppliers()
}

function syncSupplierFromKeyword() {
  const name = supplierKeyword.value.trim()
  if (!name) {
    purchaseOrder.supplier = null
    return
  }
  const matched = allSuppliers.value.find((s) => s.name === name)
  if (matched) {
    purchaseOrder.setSupplier({ id: matched.id, name: matched.name })
  } else {
    purchaseOrder.setSupplier({ name })
  }
}

function selectSupplier(item: SupplierItem) {
  supplierKeyword.value = item.name
  purchaseOrder.setSupplier({ id: item.id, name: item.name })
  showSupplierPicker.value = false
  filterSuppliers()
}

function useNewSupplierName() {
  const name = supplierKeyword.value.trim()
  if (!name) return
  purchaseOrder.setSupplier({ name })
}

function openSupplierPicker() {
  pickerKeyword.value = supplierKeyword.value
  showSupplierPicker.value = true
}

function closeSupplierPicker() {
  showSupplierPicker.value = false
}

function onPickerKeywordInput(e: { detail: { value: string } }) {
  pickerKeyword.value = e.detail.value
}

function useNewSupplierFromPicker() {
  const name = pickerKeyword.value.trim()
  if (!name) return
  supplierKeyword.value = name
  purchaseOrder.setSupplier({ name })
  closeSupplierPicker()
}

function setReceiveDay(day: 'today' | 'tomorrow') {
  purchaseOrder.setReceiveDay(day)
}

function goProducts() {
  if (!purchaseOrder.hasSupplier) {
    uni.showToast({ title: '请先选择供应商', icon: 'none' })
    return
  }
  uni.navigateTo({ url: '/pages/boss/purchase-order/products' })
}

function goConfirm() {
  if (!canPreview.value) return
  uni.navigateTo({ url: '/pages/boss/purchase-order/confirm' })
}

function clearItems() {
  uni.showModal({
    title: '清空已选',
    content: '确定清空当前已选采购商品？',
    success: (res) => {
      if (res.confirm) purchaseOrder.resetItems()
    },
  })
}

function formatMoney(value: number) {
  return Number(value || 0).toFixed(2)
}
</script>

<style scoped lang="scss">
@import '../../../styles/boss-ui.scss';

.page {
  min-height: 100vh;
  background: $boss-bg;
  padding: 24rpx;
  padding-bottom: calc(40rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
}

.header {
  margin-bottom: 20rpx;
}

.title {
  display: block;
  font-size: 36rpx;
  font-weight: 600;
  color: #111;
}

.subtitle {
  display: block;
  margin-top: 8rpx;
  font-size: 26rpx;
  color: #888;
}

.supplier-card,
.summary-card,
.action-card {
  margin-bottom: 20rpx;
  padding: 24rpx;
  background: #fff;
  border-radius: 16rpx;
}

.supplier-row {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.supplier-input-wrap {
  flex: 1;
}

.supplier-input {
  height: 72rpx;
  padding: 0 20rpx;
  background: #f5f6f7;
  border-radius: 12rpx;
  font-size: 28rpx;
}

.arrow-btn {
  width: 72rpx;
  height: 72rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f6f7;
  border-radius: 12rpx;
}

.arrow {
  font-size: 36rpx;
  color: #999;
}

.supplier-suggestions {
  margin-top: 12rpx;
  border: 1rpx solid #eee;
  border-radius: 12rpx;
  overflow: hidden;
}

.suggestion-item {
  padding: 20rpx;
  font-size: 28rpx;
  border-bottom: 1rpx solid #f0f0f0;
}

.suggestion-item.new-supplier {
  color: $boss-green;
}

.delivery-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 20rpx;
}

.delivery-label {
  font-size: 28rpx;
  color: #e67e22;
  font-weight: 500;
}

.day-tabs {
  display: flex;
  gap: 12rpx;
}

.day-tab {
  padding: 8rpx 24rpx;
  font-size: 26rpx;
  color: #666;
  background: #f5f6f7;
  border-radius: 999rpx;
}

.day-tab.active {
  color: #fff;
  background: $boss-green;
}

.summary-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8rpx 0;
}

.summary-label {
  font-size: 28rpx;
  color: #666;
}

.summary-value {
  font-size: 30rpx;
  font-weight: 600;
  color: #111;
}

.summary-value.amount {
  color: #e67e22;
}

.summary-hint {
  display: block;
  margin-top: 12rpx;
  font-size: 24rpx;
  color: #999;
}

.action-btn {
  margin-bottom: 16rpx;
  height: 88rpx;
  line-height: 88rpx;
  font-size: 30rpx;
  border-radius: 12rpx;
}

.action-btn:last-child {
  margin-bottom: 0;
}

.action-btn.primary {
  color: #fff;
  background: $boss-green;
}

.action-btn.outline {
  color: $boss-green;
  background: #fff;
  border: 1rpx solid $boss-green;
}

.action-btn.text {
  color: #999;
  background: transparent;
}

.action-btn[disabled] {
  opacity: 0.45;
}

.picker-panel {
  padding: 24rpx 24rpx calc(24rpx + env(safe-area-inset-bottom));
}

.picker-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16rpx;
}

.picker-title {
  font-size: 32rpx;
  font-weight: 600;
}

.picker-close {
  font-size: 40rpx;
  color: #999;
}

.picker-search {
  height: 72rpx;
  padding: 0 20rpx;
  background: #f5f6f7;
  border-radius: 12rpx;
  font-size: 28rpx;
}

.supplier-list {
  max-height: 50vh;
  margin-top: 16rpx;
}

.supplier-option {
  padding: 24rpx 8rpx;
  border-bottom: 1rpx solid #f0f0f0;
}

.supplier-option.new-supplier {
  color: $boss-green;
}

.supplier-option-name {
  font-size: 28rpx;
}
</style>
