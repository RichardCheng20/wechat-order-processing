<template>
  <view class="page" :class="{ 'with-keypad': showKeypad || showQtyKeypad }">
    <view v-if="loading" class="loading-wrap">
      <u-loading-icon text="加载中" />
    </view>

    <template v-else-if="detail">
      <scroll-view scroll-y class="scroll-body">
        <view class="head-card">
          <text class="product-name">{{ detail.productName }}</text>
          <text class="receive-hint">收货 {{ receiveDateLabel }}</text>
        </view>

        <view class="summary-card">
          <view class="summary-item">
            <text class="summary-label">客户需求</text>
            <text class="summary-value">{{ formatQty(detail.demandQty) }}{{ detail.unit }}</text>
          </view>
          <view v-if="!detail.customItem" class="summary-item editable" @tap="openStockEditor">
            <text class="summary-label">现有库存</text>
            <text class="summary-value stock">{{ formatQty(detail.stockQty) }}{{ detail.unit }}</text>
            <text class="edit-hint">点击修改</text>
          </view>
          <view class="summary-item need">
            <text class="summary-label">需采购</text>
            <text class="summary-value">{{ formatQty(detail.needQty) }}{{ detail.unit }}</text>
          </view>
        </view>

        <view v-if="detail.customItem" class="price-card readonly-card">
          <view class="price-head">
            <text class="section-title">代采进价</text>
            <view v-if="detail.priced" class="priced-tag">已录</view>
          </view>
          <text v-if="detail.purchasePrice != null" class="readonly-price">
            ¥{{ formatMoney(detail.purchasePrice) }} / {{ detail.unit }}
          </text>
          <text v-else class="readonly-hint">尚未录价，请至「录价」页填写成本价与售价</text>
          <text class="readonly-note">进价已在录价环节录入，此处仅作采购汇总展示</text>
        </view>

        <view v-else class="price-card">
          <view class="price-head">
            <text class="section-title">今晚进价（统一）</text>
            <view v-if="detail.priced" class="priced-tag">已录</view>
          </view>
          <view class="price-input-row" @tap="openPriceKeypad">
            <text v-if="priceDraft" class="price-value">{{ priceDraft }}</text>
            <text v-else class="price-placeholder">点击录入进价</text>
            <text class="price-unit">元/{{ detail.unit }}</text>
          </view>
          <view class="price-input-row qty-row" @tap="openPurchaseQtyKeypad">
            <text class="qty-label">今日采购</text>
            <text v-if="purchaseQtyDraft" class="price-value">{{ purchaseQtyDraft }}</text>
            <text v-else class="price-placeholder">点击录入数量</text>
            <text class="price-unit">{{ detail.unit }}</text>
          </view>
          <view class="price-actions">
            <view class="action-btn outline" @tap="handleFetchPrice">获取进价</view>
            <view class="action-btn primary" @tap="handleSubmitPrice">确认采购</view>
          </view>
          <text v-if="detail.referencePurchasePrice != null" class="ref-hint">
            参考进价 ¥{{ formatMoney(detail.referencePurchasePrice) }}
          </text>
        </view>

        <view class="customer-card">
          <text class="section-title">客户下单明细（{{ detail.customerLines.length }}家）</text>
          <view v-if="detail.customerLines.length === 0" class="customer-empty">
            <text>该收货日暂无客户下单</text>
          </view>
          <view
            v-for="(line, idx) in detail.customerLines"
            :key="`${line.customerId || 'guest'}-${idx}`"
            class="customer-row"
          >
            <view class="customer-main">
              <text class="customer-name">{{ line.customerName }}</text>
              <text class="customer-sub">{{ line.orderCount || 1 }}笔订单</text>
            </view>
            <text class="customer-qty">{{ formatQty(line.totalQty) }}{{ detail.unit }}</text>
          </view>
        </view>
      </scroll-view>
    </template>

    <view v-if="showKeypad" class="keypad-wrap">
      <view class="edit-context-bar">
        <text class="edit-title">{{ detail?.productName }} · 进价</text>
        <text class="edit-price">{{ priceDraft || '0' }} 元/{{ detail?.unit }}</text>
      </view>
      <view class="keypad-toggle" @tap="closePriceKeypad">收起键盘 ⌄</view>
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
          <view class="key confirm-side" @tap="confirmPriceKeypad">确定</view>
        </view>
      </view>
    </view>

    <view v-if="showQtyKeypad" class="keypad-wrap">
      <view class="edit-context-bar">
        <text class="edit-title">{{ detail?.productName }} · {{ qtyKeypadLabel }}</text>
        <text class="edit-price">{{ qtyDraft || '0' }} {{ detail?.unit }}</text>
      </view>
      <view class="keypad-toggle" @tap="closeQtyKeypad">收起键盘 ⌄</view>
      <view class="keypad-body">
        <view class="keypad-main">
          <view class="key-row">
            <view class="key" @tap="inputQtyKey('1')">1</view>
            <view class="key" @tap="inputQtyKey('2')">2</view>
            <view class="key" @tap="inputQtyKey('3')">3</view>
            <view class="key fn" @tap="backspaceQty">⌫</view>
          </view>
          <view class="key-row">
            <view class="key" @tap="inputQtyKey('4')">4</view>
            <view class="key" @tap="inputQtyKey('5')">5</view>
            <view class="key" @tap="inputQtyKey('6')">6</view>
          </view>
          <view class="key-row">
            <view class="key" @tap="inputQtyKey('7')">7</view>
            <view class="key" @tap="inputQtyKey('8')">8</view>
            <view class="key" @tap="inputQtyKey('9')">9</view>
          </view>
          <view class="key-row">
            <view class="key wide" @tap="inputQtyKey('0')">0</view>
            <view class="key" @tap="inputQtyKey('.')">.</view>
            <view class="key fn" @tap="clearQtyDraft">清零</view>
          </view>
        </view>
        <view class="keypad-side">
          <view class="key confirm-side" @tap="confirmQtyKeypad">确定</view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import {
  fetchCustomProcurementDetail,
  fetchProcurementProductDetail,
  fetchProcurementReferencePrice,
  submitProcurementPurchasePrice,
  updateProcurementStock,
  type ProcurementProductDetail,
} from '@common/api/procurement'
import { useUserStore } from '@common/stores/user'

const userStore = useUserStore()
const detail = ref<ProcurementProductDetail | null>(null)
const loading = ref(false)
const submitting = ref(false)
const productId = ref(0)
const customName = ref('')
const receiveDate = ref('')
const priceDraft = ref('')
const purchaseQtyDraft = ref('')
const stockDraft = ref('')
const showKeypad = ref(false)
const showQtyKeypad = ref(false)
const qtyKeypadMode = ref<'stock' | 'purchase'>('purchase')

const qtyKeypadLabel = computed(() => (qtyKeypadMode.value === 'stock' ? '现有库存' : '今日采购'))

const qtyDraft = computed({
  get: () => (qtyKeypadMode.value === 'stock' ? stockDraft.value : purchaseQtyDraft.value),
  set: (val: string) => {
    if (qtyKeypadMode.value === 'stock') stockDraft.value = val
    else purchaseQtyDraft.value = val
  },
})

const receiveDateLabel = computed(() => {
  if (!receiveDate.value) return '—'
  const parts = receiveDate.value.split('-')
  if (parts.length < 3) return receiveDate.value
  return `${parts[1]}-${parts[2]}`
})

onLoad((query) => {
  productId.value = Number(query?.productId || 0)
  customName.value = query?.customName ? decodeURIComponent(String(query.customName)) : ''
  if (query?.receiveDate) {
    receiveDate.value = String(query.receiveDate)
  }
})

onShow(async () => {
  if (!userStore.isLoggedIn || !userStore.isBoss) {
    uni.reLaunch({ url: '/packages/common/login/index' })
    return
  }
  if (!receiveDate.value) {
    receiveDate.value = formatDate(new Date())
  }
  await loadDetail()
})

async function loadDetail() {
  if (!productId.value && !customName.value) return
  loading.value = true
  try {
    detail.value = customName.value
      ? await fetchCustomProcurementDetail(customName.value, receiveDate.value)
      : await fetchProcurementProductDetail(productId.value, receiveDate.value)
    if (detail.value.receiveDate) {
      receiveDate.value = detail.value.receiveDate
    }
    priceDraft.value = detail.value.purchasePrice != null ? String(detail.value.purchasePrice) : ''
    if (detail.value.purchasedQtyToday != null && detail.value.purchasedQtyToday > 0) {
      purchaseQtyDraft.value = String(detail.value.purchasedQtyToday)
    } else if (!purchaseQtyDraft.value && detail.value.needQty > 0) {
      purchaseQtyDraft.value = String(detail.value.needQty)
    }
    stockDraft.value = detail.value.stockQty != null ? String(detail.value.stockQty) : '0'
    uni.setNavigationBarTitle({ title: detail.value.productName })
  } catch (e) {
    detail.value = null
    uni.showToast({ title: e instanceof Error ? e.message : '加载失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

function openPriceKeypad() {
  showQtyKeypad.value = false
  showKeypad.value = true
}

function closePriceKeypad() {
  showKeypad.value = false
}

function openPurchaseQtyKeypad() {
  showKeypad.value = false
  qtyKeypadMode.value = 'purchase'
  if (!purchaseQtyDraft.value && detail.value?.needQty) {
    purchaseQtyDraft.value = String(detail.value.needQty)
  }
  showQtyKeypad.value = true
}

function openStockEditor() {
  showKeypad.value = false
  qtyKeypadMode.value = 'stock'
  stockDraft.value = detail.value?.stockQty != null ? String(detail.value.stockQty) : '0'
  showQtyKeypad.value = true
}

function closeQtyKeypad() {
  showQtyKeypad.value = false
}

function inputKey(key: string) {
  let val = priceDraft.value
  if (key === '.' && val.includes('.')) return
  if (val === '0' && key !== '.') val = key
  else val += key
  priceDraft.value = val
}

function backspace() {
  priceDraft.value = priceDraft.value.slice(0, -1)
}

function clearDraft() {
  priceDraft.value = ''
}

function confirmPriceKeypad() {
  closePriceKeypad()
}

function inputQtyKey(key: string) {
  let val = qtyDraft.value
  if (key === '.' && val.includes('.')) return
  if (val === '0' && key !== '.') val = key
  else val += key
  qtyDraft.value = val
}

function backspaceQty() {
  qtyDraft.value = qtyDraft.value.slice(0, -1)
}

function clearQtyDraft() {
  qtyDraft.value = ''
}

async function confirmQtyKeypad() {
  if (qtyKeypadMode.value === 'stock') {
    const stockQty = Number(stockDraft.value || 0)
    if (stockQty < 0) {
      uni.showToast({ title: '库存不能为负数', icon: 'none' })
      return
    }
    try {
      detail.value = await updateProcurementStock(productId.value, stockQty, receiveDate.value)
      stockDraft.value = String(detail.value.stockQty)
      closeQtyKeypad()
      uni.showToast({ title: '库存已更新', icon: 'success' })
    } catch (e) {
      uni.showToast({ title: e instanceof Error ? e.message : '更新失败', icon: 'none' })
    }
    return
  }
  closeQtyKeypad()
}

async function handleFetchPrice() {
  try {
    const data = await fetchProcurementReferencePrice(productId.value, receiveDate.value)
    detail.value = data
    if (data.purchasePrice != null) {
      priceDraft.value = String(data.purchasePrice)
    } else if (data.referencePurchasePrice != null) {
      priceDraft.value = String(data.referencePurchasePrice)
    }
    uni.showToast({ title: '已填入参考进价', icon: 'success' })
  } catch (e) {
    uni.showToast({ title: e instanceof Error ? e.message : '获取失败', icon: 'none' })
  }
}

async function handleSubmitPrice() {
  if (submitting.value) return
  const purchasePrice = Number(priceDraft.value)
  if (!purchasePrice || purchasePrice < 0) {
    uni.showToast({ title: '请输入有效进价', icon: 'none' })
    return
  }
  const purchasedQty = Number(purchaseQtyDraft.value)
  if (!purchasedQty || purchasedQty < 0) {
    uni.showToast({ title: '请输入今日采购数量', icon: 'none' })
    return
  }
  submitting.value = true
  try {
    detail.value = await submitProcurementPurchasePrice(
      productId.value,
      purchasePrice,
      receiveDate.value,
      purchasedQty,
    )
    priceDraft.value = String(purchasePrice)
    purchaseQtyDraft.value = String(detail.value.purchasedQtyToday ?? purchasedQty)
    stockDraft.value = String(detail.value.stockQty)
    uni.showToast({ title: '采购已保存', icon: 'success' })
  } catch (e) {
    uni.showToast({ title: e instanceof Error ? e.message : '保存失败', icon: 'none' })
  } finally {
    submitting.value = false
  }
}

function formatDate(date: Date) {
  const y = date.getFullYear()
  const m = `${date.getMonth() + 1}`.padStart(2, '0')
  const d = `${date.getDate()}`.padStart(2, '0')
  return `${y}-${m}-${d}`
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
.page {
  min-height: 100vh;
  background: #f5f6f8;
}

.page.with-keypad {
  padding-bottom: calc(520rpx + env(safe-area-inset-bottom));
}

.loading-wrap {
  padding: 120rpx 0;
  text-align: center;
}

.scroll-body {
  height: 100vh;
  padding: 16rpx 20rpx calc(40rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
}

.readonly-card {
  background: #fffaf2;
}

.readonly-price {
  display: block;
  margin-top: 16rpx;
  font-size: 40rpx;
  font-weight: 700;
  color: #e67e22;
}

.readonly-hint,
.readonly-note {
  display: block;
  margin-top: 12rpx;
  font-size: 26rpx;
  color: #888;
  line-height: 1.5;
}

.head-card,
.summary-card,
.price-card,
.customer-card {
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 16rpx;
}

.product-name {
  display: block;
  font-size: 36rpx;
  font-weight: 700;
  color: #111;
}

.receive-hint {
  display: block;
  margin-top: 8rpx;
  font-size: 26rpx;
  color: #e67e22;
}

.summary-card {
  display: flex;
  gap: 12rpx;
}

.summary-item {
  flex: 1;
  padding: 16rpx 12rpx;
  background: #fafafa;
  border-radius: 12rpx;
  text-align: center;
}

.summary-item.need {
  background: #fff7e6;
}

.summary-item.editable {
  background: #ecfdf3;
}

.summary-label {
  display: block;
  font-size: 22rpx;
  color: #999;
}

.summary-value {
  display: block;
  margin-top: 8rpx;
  font-size: 32rpx;
  font-weight: 700;
  color: #111;
}

.summary-item.need .summary-value {
  color: #e67e22;
}

.edit-hint {
  display: block;
  margin-top: 6rpx;
  font-size: 20rpx;
  color: #22c55e;
}

.section-title {
  font-size: 28rpx;
  font-weight: 600;
  color: #333;
}

.price-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.priced-tag {
  padding: 4rpx 12rpx;
  font-size: 20rpx;
  color: #22c55e;
  background: #ecfdf3;
  border-radius: 999rpx;
}

.price-input-row {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-top: 20rpx;
  padding: 24rpx;
  background: #fafafa;
  border: 2rpx solid #ddd;
  border-radius: 12rpx;
}

.qty-row {
  margin-top: 12rpx;
}

.qty-label {
  font-size: 26rpx;
  color: #666;
  min-width: 120rpx;
}

.price-value {
  flex: 1;
  font-size: 40rpx;
  font-weight: 700;
  color: #111;
}

.price-placeholder {
  flex: 1;
  font-size: 28rpx;
  color: #bbb;
}

.price-unit {
  font-size: 26rpx;
  color: #666;
}

.price-actions {
  display: flex;
  gap: 16rpx;
  margin-top: 20rpx;
}

.action-btn {
  flex: 1;
  height: 72rpx;
  line-height: 72rpx;
  text-align: center;
  font-size: 28rpx;
  border-radius: 8rpx;
}

.action-btn.outline {
  color: #333;
  border: 1rpx solid #ddd;
  background: #fff;
}

.action-btn.primary {
  color: #fff;
  background: #22c55e;
  font-weight: 600;
}

.ref-hint {
  display: block;
  margin-top: 12rpx;
  font-size: 24rpx;
  color: #999;
}

.customer-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20rpx 0;
  border-bottom: 1rpx solid #f0f0f0;
}

.customer-row:last-child {
  border-bottom: none;
}

.customer-name {
  display: block;
  font-size: 30rpx;
  font-weight: 600;
  color: #111;
}

.customer-sub {
  display: block;
  margin-top: 6rpx;
  font-size: 22rpx;
  color: #999;
}

.customer-qty {
  font-size: 30rpx;
  font-weight: 600;
  color: #22c55e;
}

.customer-empty {
  padding: 32rpx 0;
  text-align: center;
  font-size: 26rpx;
  color: #999;
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

.edit-price {
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
