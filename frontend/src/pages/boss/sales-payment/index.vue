<template>
  <view class="page boss-page">
    <view class="customer-card">
      <view class="customer-row">
        <view class="customer-input-wrap">
          <input
            class="customer-input"
            type="text"
            :value="customerKeyword"
            placeholder="输入客户名称"
            confirm-type="done"
            @input="onCustomerNameInput"
            @blur="syncCustomerFromKeyword"
            @confirm="syncCustomerFromKeyword"
          />
        </view>
        <view class="arrow-btn" @tap.stop="openCustomerPicker">
          <text class="arrow">›</text>
        </view>
      </view>
      <view v-if="showCustomerSuggestions" class="customer-suggestions">
        <view
          v-for="item in suggestionCustomers"
          :key="item.id"
          class="suggestion-item"
          @tap="selectCustomer(item)"
        >
          <text class="suggestion-name">{{ item.name }}</text>
          <text v-if="hasCustomerDebt(item)" class="suggestion-debt">
            欠款：￥{{ formatMoney(item.outstandingAmount) }}
          </text>
        </view>
      </view>
      <view class="delivery-row">
        <text class="delivery-label">配送：{{ deliveryLabel }}</text>
        <view class="day-tabs">
          <text
            class="day-tab"
            :class="{ active: deliveryDay === 'today' }"
            @tap="deliveryDay = 'today'"
          >今日</text>
          <text
            class="day-tab"
            :class="{ active: deliveryDay === 'tomorrow' }"
            @tap="deliveryDay = 'tomorrow'"
          >明日</text>
        </view>
      </view>
    </view>

    <view class="main boss-main-fill">
      <view class="amount-card" @tap="openAmountEditor">
        <text v-if="amountDraft" class="amount-value">￥{{ amountDraft }}</text>
        <text v-else class="amount-placeholder">输入记账金额</text>
      </view>

      <view class="ticket-divider" />

      <scroll-view scroll-y class="voucher-scroll boss-list-scroll-fill">
        <view v-if="voucherPreviews.length === 0" class="voucher-empty">
          <text class="voucher-empty-icon">📋</text>
          <text class="voucher-empty-text">暂无凭证</text>
        </view>
        <view v-else class="voucher-grid">
          <view v-for="(item, index) in voucherPreviews" :key="item.localPath" class="voucher-item">
            <image class="voucher-image" :src="item.localPath" mode="aspectFill" @tap="previewVoucher(index)" />
            <text class="voucher-remove" @tap.stop="removeVoucher(index)">×</text>
          </view>
        </view>
      </scroll-view>

      <view class="remark-bar" @tap="openRemarkInput">
        <text>{{ remark || '备注' }}</text>
      </view>
    </view>

    <view class="boss-bottom-bar">
      <view class="boss-tool-item" @tap="pickVoucher">
        <AppIcon class="boss-tool-icon" name="camera" tone="blue" :size="22" :tile-size="54" :radius="14" />
        <text class="boss-tool-label">拍凭证</text>
      </view>
      <view class="boss-tool-item" @tap="showComingSoon('拍照算账')">
        <AppIcon class="boss-tool-icon" name="scan" tone="green" :size="22" :tile-size="54" :radius="14" />
        <text class="boss-tool-label">拍照算账</text>
      </view>
      <button class="boss-primary-btn" :loading="submitting" @tap="handleSubmit">提交</button>
    </view>

    <u-popup :show="showCustomerPicker" mode="bottom" round="16" @close="closeCustomerPicker">
      <view class="picker-panel">
        <view class="picker-head">
          <text class="picker-title">选择客户</text>
          <text class="picker-close" @tap="closeCustomerPicker">×</text>
        </view>
        <view class="picker-search-wrap">
          <input
            class="picker-search"
            type="text"
            :value="pickerKeyword"
            placeholder="搜索客户名称"
            @input="onPickerKeywordInput"
          />
        </view>
        <text class="picker-sub">所有客户</text>
        <scroll-view scroll-y class="customer-list">
          <view
            v-for="item in pickerCustomers"
            :key="item.id"
            class="customer-option"
            @tap="selectCustomer(item)"
          >
            <text class="customer-option-name">{{ item.name }}</text>
            <text v-if="hasCustomerDebt(item)" class="customer-option-debt">
              欠款：￥{{ formatMoney(item.outstandingAmount) }}
            </text>
          </view>
          <view v-if="pickerCustomers.length === 0" class="no-match">暂无客户，请先新建</view>
        </scroll-view>
      </view>
    </u-popup>

    <view v-if="showAmountEditor" class="amount-mask" @tap="closeAmountEditor">
      <view class="amount-sheet" @tap.stop>
        <view class="amount-head">
          <text class="amount-close" @tap="closeAmountEditor">×</text>
          <text class="amount-title">输入记账金额</text>
          <text class="amount-unit">元</text>
        </view>
        <view class="amount-field">
          <text v-if="amountEditorDraft" class="amount-draft">￥{{ amountEditorDraft }}</text>
          <text v-else class="amount-draft placeholder">0</text>
        </view>
        <view class="keypad-grid">
          <view class="key" @tap="inputAmountKey('1')">1</view>
          <view class="key" @tap="inputAmountKey('2')">2</view>
          <view class="key" @tap="inputAmountKey('3')">3</view>
          <view class="key fn" @tap="backspaceAmount">⌫</view>

          <view class="key" @tap="inputAmountKey('4')">4</view>
          <view class="key" @tap="inputAmountKey('5')">5</view>
          <view class="key" @tap="inputAmountKey('6')">6</view>
          <view class="key fn" @tap="clearAmount">清零</view>

          <view class="key" @tap="inputAmountKey('7')">7</view>
          <view class="key" @tap="inputAmountKey('8')">8</view>
          <view class="key" @tap="inputAmountKey('9')">9</view>
          <view class="key confirm" @tap="confirmAmountEditor">确定</view>

          <view class="key" @tap="inputAmountKey('.')">.</view>
          <view class="key" @tap="inputAmountKey('0')">0</view>
          <view class="key blank" />
          <view class="key blank" />
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { fetchBossCustomers, type CustomerItem } from '../../../api/customer'
import { createBossPayment, uploadPaymentVoucher } from '../../../api/payment'
import AppIcon from '../../../components/AppIcon.vue'
import { deliveryDateString } from '../../../stores/salesOrder'
import { useUserStore } from '../../../stores/user'

interface VoucherPreview {
  localPath: string
  remoteUrl?: string
}

const userStore = useUserStore()
const submitting = ref(false)
const showCustomerPicker = ref(false)
const pickerKeyword = ref('')
const customerKeyword = ref('')
const selectedCustomer = ref<CustomerItem | null>(null)
const allCustomers = ref<CustomerItem[]>([])
const filteredCustomers = ref<CustomerItem[]>([])
const deliveryDay = ref<'today' | 'tomorrow'>('today')
const amountDraft = ref('')
const amountEditorDraft = ref('')
const showAmountEditor = ref(false)
const remark = ref('')
const voucherPreviews = ref<VoucherPreview[]>([])
const presetCustomerId = ref(0)
const presetCustomerName = ref('')
const presetAmount = ref('')
let customerPresetApplied = false

const deliveryLabel = computed(() => formatPaymentLabel(deliveryDay.value))

const showCustomerSuggestions = computed(() => {
  const kw = customerKeyword.value.trim()
  if (!kw) return false
  return filteredCustomers.value.length > 0
})

const suggestionCustomers = computed(() => filteredCustomers.value.slice(0, 8))

const pickerCustomers = computed(() => {
  const kw = pickerKeyword.value.trim().toLowerCase()
  if (!kw) return allCustomers.value
  return allCustomers.value.filter((c) => {
    const hay = `${c.name} ${c.contactName || ''} ${c.phone || ''}`.toLowerCase()
    return hay.includes(kw)
  })
})

onLoad((query) => {
  presetCustomerId.value = Number(query?.customerId || 0)
  if (query?.customerName) {
    presetCustomerName.value = decodeURIComponent(String(query.customerName))
  }
  if (query?.amount) {
    presetAmount.value = String(query.amount)
  }
})

onShow(async () => {
  if (!userStore.isLoggedIn || !userStore.isBoss) {
    uni.reLaunch({ url: '/pages/login/index' })
    return
  }
  allCustomers.value = await fetchBossCustomers()
  if (!customerPresetApplied && presetCustomerId.value) {
    const found = allCustomers.value.find((c) => c.id === presetCustomerId.value)
    if (found) {
      selectCustomer(found)
    } else if (presetCustomerName.value) {
      customerKeyword.value = presetCustomerName.value
      selectedCustomer.value = {
        id: presetCustomerId.value,
        name: presetCustomerName.value,
        bindStatus: 'NOT_INVITED',
      }
    }
    if (presetAmount.value) {
      amountDraft.value = presetAmount.value
    }
    customerPresetApplied = true
  }
  filterCustomers()
})

function formatPaymentLabel(day: 'today' | 'tomorrow') {
  const base = new Date()
  if (day === 'tomorrow') {
    base.setDate(base.getDate() + 1)
  }
  const mm = String(base.getMonth() + 1).padStart(2, '0')
  const dd = String(base.getDate()).padStart(2, '0')
  return `${mm}-${dd} 20:30`
}

function paidAtString(day: 'today' | 'tomorrow') {
  return `${deliveryDateString(day)}T20:30:00`
}

function formatMoney(value?: number) {
  return Number(value || 0).toFixed(2)
}

function hasCustomerDebt(item: CustomerItem) {
  return (item.outstandingAmount || 0) > 0
}

function onCustomerNameInput(e: { detail: { value: string } }) {
  customerKeyword.value = e.detail.value
  filterCustomers()
}

function syncCustomerFromKeyword() {
  const name = customerKeyword.value.trim()
  if (!name) {
    selectedCustomer.value = null
    return
  }
  const exact = allCustomers.value.find((c) => c.name === name)
  if (exact) {
    selectedCustomer.value = exact
    return
  }
  if (filteredCustomers.value.length === 1) {
    selectedCustomer.value = filteredCustomers.value[0]
    customerKeyword.value = selectedCustomer.value.name
  } else {
    selectedCustomer.value = null
  }
}

function filterCustomers() {
  const kw = customerKeyword.value.trim().toLowerCase()
  if (!kw) {
    filteredCustomers.value = allCustomers.value
    return
  }
  filteredCustomers.value = allCustomers.value.filter((c) =>
    c.name.toLowerCase().includes(kw)
    || (c.contactName || '').toLowerCase().includes(kw)
    || (c.phone || '').includes(kw),
  )
}

function openCustomerPicker() {
  pickerKeyword.value = customerKeyword.value
  showCustomerPicker.value = true
}

function closeCustomerPicker() {
  showCustomerPicker.value = false
}

function onPickerKeywordInput(e: { detail: { value: string } }) {
  pickerKeyword.value = e.detail.value
}

function selectCustomer(item: CustomerItem) {
  customerKeyword.value = item.name
  selectedCustomer.value = item
  filteredCustomers.value = allCustomers.value
  closeCustomerPicker()
}

function openAmountEditor() {
  amountEditorDraft.value = amountDraft.value
  showAmountEditor.value = true
}

function closeAmountEditor() {
  showAmountEditor.value = false
  amountEditorDraft.value = ''
}

function inputAmountKey(key: string) {
  if (key === '.' && amountEditorDraft.value.includes('.')) return
  const parts = amountEditorDraft.value.split('.')
  if (parts[1] && parts[1].length >= 2 && key !== '.') return
  if (amountEditorDraft.value === '0' && key !== '.') {
    amountEditorDraft.value = key
    return
  }
  amountEditorDraft.value += key
}

function backspaceAmount() {
  amountEditorDraft.value = amountEditorDraft.value.slice(0, -1)
}

function clearAmount() {
  amountEditorDraft.value = ''
}

function confirmAmountEditor() {
  const value = amountEditorDraft.value.trim()
  if (!value) {
    uni.showToast({ title: '请输入金额', icon: 'none' })
    return
  }
  const amount = Number(value)
  if (!amount || amount <= 0) {
    uni.showToast({ title: '请输入有效金额', icon: 'none' })
    return
  }
  amountDraft.value = value
  closeAmountEditor()
}

function openRemarkInput() {
  uni.showModal({
    title: '备注',
    editable: true,
    placeholderText: '请输入备注',
    content: remark.value,
    success: (res) => {
      if (res.confirm && res.content != null) {
        remark.value = res.content.trim()
      }
    },
  })
}

function pickVoucher() {
  uni.chooseImage({
    count: 9 - voucherPreviews.value.length,
    sizeType: ['compressed'],
    sourceType: ['album', 'camera'],
    success: (res) => {
      const paths = Array.isArray(res.tempFilePaths) ? res.tempFilePaths : [res.tempFilePaths]
      voucherPreviews.value = [
        ...voucherPreviews.value,
        ...paths.map((localPath) => ({ localPath })),
      ]
    },
  })
}

function removeVoucher(index: number) {
  voucherPreviews.value = voucherPreviews.value.filter((_, i) => i !== index)
}

function previewVoucher(index: number) {
  uni.previewImage({
    current: index,
    urls: voucherPreviews.value.map((item) => item.localPath),
  })
}

function showComingSoon(name: string) {
  uni.showToast({ title: `${name}即将上线`, icon: 'none' })
}

async function uploadVouchers() {
  const urls: string[] = []
  for (const item of voucherPreviews.value) {
    if (item.remoteUrl) {
      urls.push(item.remoteUrl)
      continue
    }
    const url = await uploadPaymentVoucher(item.localPath)
    item.remoteUrl = url
    urls.push(url)
  }
  return urls
}

function resetForm() {
  customerKeyword.value = ''
  selectedCustomer.value = null
  deliveryDay.value = 'today'
  amountDraft.value = ''
  remark.value = ''
  voucherPreviews.value = []
}

async function handleSubmit() {
  syncCustomerFromKeyword()
  if (!selectedCustomer.value?.id) {
    uni.showToast({ title: '请选择已登记客户', icon: 'none' })
    openCustomerPicker()
    return
  }
  const amount = Number(amountDraft.value)
  if (!amount || amount <= 0) {
    uni.showToast({ title: '请输入记账金额', icon: 'none' })
    openAmountEditor()
    return
  }

  submitting.value = true
  try {
    const voucherUrls = voucherPreviews.value.length > 0 ? await uploadVouchers() : undefined
    await createBossPayment({
      customerId: selectedCustomer.value.id,
      amount,
      paidAt: paidAtString(deliveryDay.value),
      remark: remark.value || undefined,
      voucherUrls,
    })
    const customerName = selectedCustomer.value.name
    resetForm()
    uni.showToast({ title: `${customerName} 收款已登记`, icon: 'success' })
    setTimeout(() => {
      uni.navigateBack()
    }, 600)
  } catch (e) {
    uni.showToast({ title: e instanceof Error ? e.message : '提交失败', icon: 'none' })
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped lang="scss">
@import '../../../styles/boss-footer.scss';

.page {
  background: linear-gradient(180deg, #e8f8ef 0%, #f5f6f7 280rpx);
}

.main {
  flex-direction: column;
  padding: 16rpx 24rpx 0;
}

.customer-card {
  flex-shrink: 0;
  margin: 24rpx 24rpx 0;
  background: #fff;
  border-radius: 16rpx;
  overflow: hidden;
}

.customer-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20rpx 32rpx;
  border-bottom: 1rpx solid #f2f3f5;
}

.customer-input-wrap {
  flex: 1;
  min-width: 0;
}

.customer-input {
  width: 100%;
  height: 64rpx;
  font-size: 34rpx;
  font-weight: 600;
  color: #222;
}

.arrow-btn {
  flex-shrink: 0;
  min-width: 72rpx;
  min-height: 72rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.arrow {
  color: #ccc;
  font-size: 40rpx;
}

.customer-suggestions {
  border-top: 1rpx solid #f2f3f5;
  background: #fafbfc;
}

.suggestion-item {
  padding: 20rpx 32rpx;
  border-bottom: 1rpx solid #f0f0f0;
}

.suggestion-name {
  display: block;
  font-size: 30rpx;
  color: #222;
}

.suggestion-debt {
  display: block;
  margin-top: 6rpx;
  font-size: 24rpx;
  color: #e67e22;
}

.delivery-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20rpx 32rpx;
}

.delivery-label {
  font-size: 26rpx;
  color: #666;
}

.day-tabs {
  display: flex;
  gap: 12rpx;
}

.day-tab {
  padding: 8rpx 24rpx;
  font-size: 24rpx;
  color: #666;
  background: #f5f6f7;
  border-radius: 999rpx;
}

.day-tab.active {
  color: #07c160;
  background: #e8f8ef;
  font-weight: 600;
}

.amount-card {
  flex-shrink: 0;
  margin-bottom: 16rpx;
  padding: 40rpx 32rpx;
  background: #fff;
  border-radius: 16rpx;
  text-align: center;
}

.amount-value {
  font-size: 56rpx;
  font-weight: 700;
  color: #222;
}

.amount-placeholder {
  font-size: 34rpx;
  color: #ccc;
}

.ticket-divider {
  flex-shrink: 0;
  height: 24rpx;
  margin: 0 8rpx 16rpx;
  background: repeating-linear-gradient(
    90deg,
    #ff6b6b 0,
    #ff6b6b 16rpx,
    transparent 16rpx,
    transparent 24rpx,
    #4dabf7 24rpx,
    #4dabf7 40rpx,
    transparent 40rpx,
    transparent 48rpx
  );
  mask-image: radial-gradient(circle at 0 50%, transparent 12rpx, #000 12rpx),
    radial-gradient(circle at 100% 50%, transparent 12rpx, #000 12rpx);
  mask-composite: intersect;
}

.voucher-scroll {
  flex: 1;
  min-height: 0;
  background: #fff;
  border-radius: 16rpx;
}

.voucher-empty {
  padding: 80rpx 32rpx;
  text-align: center;
}

.voucher-empty-icon {
  display: block;
  font-size: 72rpx;
  margin-bottom: 16rpx;
}

.voucher-empty-text {
  font-size: 28rpx;
  color: #999;
}

.voucher-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
  padding: 24rpx;
}

.voucher-item {
  position: relative;
  width: calc((100% - 32rpx) / 3);
  aspect-ratio: 1;
}

.voucher-image {
  width: 100%;
  height: 100%;
  border-radius: 12rpx;
  background: #f5f6f7;
}

.voucher-remove {
  position: absolute;
  top: -8rpx;
  right: -8rpx;
  width: 40rpx;
  height: 40rpx;
  line-height: 36rpx;
  text-align: center;
  background: rgba(0, 0, 0, 0.55);
  color: #fff;
  border-radius: 50%;
  font-size: 28rpx;
}

.remark-bar {
  flex-shrink: 0;
  margin: 16rpx 0;
  padding: 24rpx 32rpx;
  background: #eef0f2;
  border-radius: 12rpx;
  font-size: 26rpx;
  color: #888;
}

.picker-panel {
  padding: 0 0 calc(16rpx + env(safe-area-inset-bottom));
  max-height: 78vh;
  display: flex;
  flex-direction: column;
  background: #fff;
}

.picker-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 28rpx 32rpx 16rpx;
}

.picker-title {
  font-size: 34rpx;
  font-weight: 600;
}

.picker-close {
  width: 48rpx;
  height: 48rpx;
  line-height: 48rpx;
  text-align: center;
  font-size: 40rpx;
  color: #999;
}

.picker-search-wrap {
  padding: 0 32rpx 16rpx;
}

.picker-search {
  height: 72rpx;
  padding: 0 24rpx;
  background: #f5f6f7;
  border-radius: 999rpx;
  font-size: 28rpx;
}

.picker-sub {
  display: block;
  padding: 8rpx 32rpx 12rpx;
  font-size: 26rpx;
  color: #999;
  background: #f5f6f7;
}

.customer-list {
  flex: 1;
  max-height: 52vh;
}

.customer-option {
  padding: 24rpx 32rpx;
  border-bottom: 1rpx solid #f0f0f0;
}

.customer-option-name {
  display: block;
  font-size: 32rpx;
  color: #222;
}

.customer-option-debt {
  display: block;
  margin-top: 6rpx;
  font-size: 26rpx;
  color: #e67e22;
}

.no-match {
  padding: 48rpx;
  text-align: center;
  color: #999;
  font-size: 28rpx;
}

.amount-mask {
  position: fixed;
  inset: 0;
  z-index: 200;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
}

.amount-sheet {
  background: #fff;
  border-radius: 24rpx 24rpx 0 0;
  padding-bottom: env(safe-area-inset-bottom);
}

.amount-head {
  display: flex;
  align-items: center;
  padding: 24rpx 32rpx 8rpx;
}

.amount-close {
  width: 80rpx;
  font-size: 48rpx;
  color: #999;
}

.amount-title {
  flex: 1;
  text-align: center;
  font-size: 34rpx;
  font-weight: 600;
}

.amount-unit {
  min-width: 80rpx;
  text-align: right;
  font-size: 26rpx;
  color: #999;
}

.amount-field {
  margin: 0 32rpx 24rpx;
  height: 96rpx;
  padding: 0 24rpx;
  display: flex;
  align-items: center;
  background: #f5f6f7;
  border-radius: 12rpx;
  border: 2rpx solid #07c160;
}

.amount-draft {
  font-size: 44rpx;
  font-weight: 600;
  color: #222;
}

.amount-draft.placeholder {
  color: #ccc;
  font-weight: 400;
}

.keypad-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  grid-template-rows: repeat(4, 96rpx);
  gap: 8rpx;
  padding: 12rpx;
  background: #eef0f2;
  border-top: 1rpx solid #ddd;
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
