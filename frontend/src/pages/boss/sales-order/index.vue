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
          <text v-if="salesOrder.customer?.temporary" class="temp-tag">临时</text>
        </view>
        <view class="arrow-btn" @tap.stop="openCustomerPicker">
          <text class="arrow">›</text>
        </view>
      </view>
      <view
        v-if="showCustomerSuggestions"
        class="customer-suggestions"
      >
        <view
          v-for="item in suggestionCustomers"
          :key="item.id"
          class="suggestion-item"
          @tap="selectCustomer(item)"
        >
          <text class="suggestion-name">{{ item.name }}</text>
          <text v-if="hasCustomerDebt(item)" class="suggestion-debt">
            欠款：￥{{ formatCustomerMoney(item.outstandingAmount) }}
          </text>
        </view>
        <view
          v-if="canUseTemporary"
          class="suggestion-item temp"
          @tap="selectTemporaryCustomer"
        >
          使用「{{ customerKeyword.trim() }}」临时开单
        </view>
      </view>
      <view class="delivery-row">
        <text class="delivery-label">配送：{{ deliveryLabel }}</text>
        <view class="day-tabs">
          <text
            class="day-tab"
            :class="{ active: salesOrder.deliveryDay === 'today' }"
            @tap="salesOrder.setDeliveryDay('today')"
          >今日</text>
          <text
            class="day-tab"
            :class="{ active: salesOrder.deliveryDay === 'tomorrow' }"
            @tap="salesOrder.setDeliveryDay('tomorrow')"
          >明日</text>
        </view>
      </view>
    </view>

    <view class="main boss-main-fill">
      <view class="items-card">
        <view class="items-head">
          <text class="items-title">共{{ salesOrder.totalKinds }}种商品</text>
          <text class="items-action" @tap="showComingSoon('获取价格')">获取价格</text>
        </view>
        <view class="table-head">
          <text class="col-name">商品名</text>
          <text class="col-qty">下单数</text>
          <text class="col-unit">单位</text>
          <text class="col-price">单价(元)</text>
          <text class="col-op" />
        </view>

        <scroll-view scroll-y class="items-scroll boss-list-scroll-fill">
          <view v-if="salesOrder.items.length === 0" class="empty-items">
            <text class="empty-text">暂无商品，快去录入商品吧</text>
            <text class="empty-link" @tap="goProducts">去商品库选品</text>
          </view>

          <view v-for="line in salesOrder.items" :key="line.lineKey" class="table-row">
            <view class="col-name">
              <text class="line-name">{{ line.productName }}</text>
              <text v-if="line.pickRemark" class="line-remark">备注：{{ line.pickRemark }}</text>
            </view>
            <view
              class="col-qty edit-cell"
              :class="{ active: editingLineKey === line.lineKey && numEditorMode === 'qty' }"
              @tap="openQtyEditor(line)"
            >
              <text class="cell-value">{{ line.orderQty }}</text>
            </view>
            <view
              class="col-unit edit-cell"
              :class="{ active: unitEditingLineKey === line.lineKey }"
              @tap="openUnitEditor(line)"
            >
              <text class="cell-value">{{ line.unit }}</text>
            </view>
            <view
              class="col-price price-cell"
              :class="{ empty: line.dealPrice == null, active: editingLineKey === line.lineKey && numEditorMode === 'price' }"
              @tap="openPriceEditor(line)"
            >
              <text v-if="line.dealPrice != null" class="price-value">{{ line.dealPrice }}</text>
              <text v-else class="price-placeholder">录入</text>
            </view>
            <text class="col-op del" @tap="removeLine(line.lineKey)">删</text>
          </view>

          <view v-if="salesOrder.items.length > 0" class="add-item-row" @tap="goProducts">
            <text class="add-item-plus">+</text>
            <text class="add-item-text">添加商品</text>
          </view>
        </scroll-view>
      </view>

      <view class="remark-bar" @tap="openRemarkInput">
        <text>{{ salesOrder.remark || '添加订单备注' }}</text>
      </view>
    </view>

    <view class="boss-bottom-bar">
      <view class="boss-tool-item" @tap="goProducts">
        <AppIcon class="boss-tool-icon" name="product" tone="green" :size="22" :tile-size="54" :radius="14" />
        <text class="boss-tool-label">商品库</text>
      </view>
      <view class="boss-tool-item" @tap="openBatchModal">
        <AppIcon class="boss-tool-icon" name="batch" tone="green" :size="22" :tile-size="54" :radius="14" />
        <text class="boss-tool-label">批量录入</text>
      </view>
      <view class="boss-tool-item" @tap="openVoiceModal">
        <AppIcon class="boss-tool-icon" name="voice" tone="green" :size="22" :tile-size="54" :radius="14" />
        <text class="boss-tool-label">语音录入</text>
      </view>
      <button class="boss-primary-btn" :loading="submitting" @tap="handleSubmit">{{ submitLabel }}</button>
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
            v-if="canUseTemporaryInPicker"
            class="customer-option temp"
            @tap="selectTemporaryCustomer"
          >
            使用「{{ pickerKeyword.trim() }}」临时开单
          </view>
          <view
            v-for="item in pickerCustomers"
            :key="item.id"
            class="customer-option"
            @tap="selectCustomer(item)"
          >
            <text class="customer-option-name">{{ item.name }}</text>
            <text v-if="hasCustomerDebt(item)" class="customer-option-debt">
              欠款：￥{{ formatCustomerMoney(item.outstandingAmount) }}
            </text>
          </view>
          <view v-if="pickerCustomers.length === 0 && !canUseTemporaryInPicker" class="no-match">
            暂无客户，请先新建
          </view>
        </scroll-view>
        <view class="picker-footer">
          <text class="picker-create" @tap="goCreateCustomerPage">新建客户</text>
        </view>
      </view>
    </u-popup>

    <!-- 数量/单价录入键盘 -->
    <view v-if="showNumEditor && editingLine" class="price-mask" @tap="closeNumEditor">
      <view class="price-sheet" @tap.stop>
        <view class="price-head">
          <text class="price-close" @tap="closeNumEditor">×</text>
          <text class="price-title">{{ numEditorMode === 'qty' ? '修改数量' : '录入单价' }}</text>
          <text class="price-unit">{{ numEditorMode === 'qty' ? editingLine.unit : `元/${editingLine.unit}` }}</text>
        </view>
        <text class="price-product">{{ editingLine.productName }}</text>
        <view class="price-field">
          <text v-if="numDraft" class="price-draft">{{ numDraft }}</text>
          <text v-else class="price-draft placeholder">0</text>
        </view>
        <view class="keypad-grid">
          <view class="key" @tap="inputNumKey('1')">1</view>
          <view class="key" @tap="inputNumKey('2')">2</view>
          <view class="key" @tap="inputNumKey('3')">3</view>
          <view class="key fn" @tap="backspaceNum">⌫</view>

          <view class="key" @tap="inputNumKey('4')">4</view>
          <view class="key" @tap="inputNumKey('5')">5</view>
          <view class="key" @tap="inputNumKey('6')">6</view>
          <view class="key fn" @tap="clearNum">清零</view>

          <view class="key" @tap="inputNumKey('7')">7</view>
          <view class="key" @tap="inputNumKey('8')">8</view>
          <view class="key" @tap="inputNumKey('9')">9</view>
          <view class="key confirm" @tap="confirmNumEditor">确定</view>

          <view class="key" @tap="inputNumKey('.')">.</view>
          <view class="key" @tap="inputNumKey('0')">0</view>
          <view class="key blank" />
          <view class="key blank" />
        </view>
      </view>
    </view>

    <!-- 单位选择 -->
    <u-popup :show="showUnitEditor" mode="bottom" round="16" @close="closeUnitEditor">
      <view class="unit-panel">
        <view class="unit-head">
          <text class="unit-cancel" @tap="closeUnitEditor">取消</text>
          <text class="unit-title">修改单位</text>
          <text class="unit-new" @tap="createCustomUnit">新建单位</text>
        </view>
        <text v-if="unitEditingLine" class="unit-product">{{ unitEditingLine.productName }}</text>
        <view class="unit-search-wrap">
          <input
            class="unit-search-input"
            type="text"
            :value="unitKeyword"
            placeholder="搜索单位名称"
            @input="onUnitKeywordInput"
          />
        </view>
        <scroll-view scroll-y class="unit-grid-wrap">
          <view class="unit-grid">
            <view
              v-for="unit in filteredUnits"
              :key="unit"
              class="unit-chip"
              :class="{ active: unitEditingLine?.unit === unit }"
              @tap="applyUnit(unit)"
            >
              {{ unit }}
            </view>
          </view>
          <view v-if="filteredUnits.length === 0" class="unit-empty">未找到匹配单位</view>
        </scroll-view>
      </view>
    </u-popup>

    <u-popup :show="showBatchModal" mode="bottom" round="16" @close="closeBatchModal">
      <view class="batch-panel">
        <view class="batch-head">
          <text class="batch-close" @tap="closeBatchModal">×</text>
          <text class="batch-title">批量录入商品</text>
          <text class="batch-single" @tap="goSingleProductFromBatch">单个录入</text>
        </view>
        <textarea
          class="batch-textarea"
          :value="batchText"
          placeholder="粘贴示例：土豆5斤，番茄12.5斤..."
          maxlength="-1"
          :disabled="batchParsing"
          @input="onBatchTextInput"
        />
        <button class="batch-recognize-btn" :loading="batchParsing" @tap="recognizeBatchText">识别</button>
      </view>
    </u-popup>

    <u-popup :show="showVoiceModal" mode="bottom" round="16" @close="closeVoiceModal">
      <view class="batch-panel">
        <view class="batch-head">
          <text class="batch-close" @tap="closeVoiceModal">×</text>
          <text class="batch-title">语音录入</text>
        </view>
        <text class="voice-hint">点击输入框，使用键盘「语音」说话，如：土豆5斤，番茄2斤6元</text>
        <textarea
          class="batch-textarea voice-textarea"
          :value="voiceText"
          placeholder="说完后点下方「识别」"
          maxlength="-1"
          :disabled="voiceParsing"
          :focus="voiceInputFocus"
          @input="onVoiceTextInput"
        />
        <button class="batch-recognize-btn" :loading="voiceParsing" @tap="recognizeVoiceText">识别</button>
      </view>
    </u-popup>
  </view>
</template>

<script setup lang="ts">
import { onLoad, onShow } from '@dcloudio/uni-app'
import { computed, ref } from 'vue'
import { filterUnits, mergeUnits, normalizeUnit, PRESET_UNITS } from '@common/constants/units'
import { fetchBossCustomers, type CustomerItem } from '@common/api/customer'
import { createBossOrder, fetchBossOrderDetail, updateBossOrder } from '@common/api/order'
import { fetchBossProducts, type ProductItem } from '@common/api/product'
import AppIcon from '@/components/AppIcon.vue'
import { deliveryDateString, formatDeliveryLabel, useSalesOrderStore, type SalesOrderLine } from '@common/stores/salesOrder'
import { useUserStore } from '@common/stores/user'
import { applyParsedLines, parseOrderText } from '@common/utils/parseOrderText'

const userStore = useUserStore()
const salesOrder = useSalesOrderStore()
const submitting = ref(false)
const showCustomerPicker = ref(false)
const pickerKeyword = ref('')
const customerKeyword = ref('')
const allCustomers = ref<CustomerItem[]>([])
const filteredCustomers = ref<CustomerItem[]>([])
const showNumEditor = ref(false)
const numEditorMode = ref<'qty' | 'price'>('price')
const editingLineKey = ref('')
const editingLine = ref<SalesOrderLine | null>(null)
const numDraft = ref('')
const showUnitEditor = ref(false)
const unitEditingLineKey = ref('')
const unitEditingLine = ref<SalesOrderLine | null>(null)
const unitKeyword = ref('')
const customUnits = ref<string[]>([])
const editOrderId = ref(0)
const allProducts = ref<ProductItem[]>([])
const showBatchModal = ref(false)
const batchText = ref('')
const batchParsing = ref(false)
const showVoiceModal = ref(false)
const voiceText = ref('')
const voiceParsing = ref(false)
const voiceInputFocus = ref(false)

const allUnits = computed(() => mergeUnits(customUnits.value))
const filteredUnits = computed(() => filterUnits(allUnits.value, unitKeyword.value))

const deliveryLabel = computed(() => formatDeliveryLabel(salesOrder.deliveryDay))
const isEditMode = computed(() => salesOrder.editOrderId != null)
const submitLabel = computed(() => (isEditMode.value ? '保存' : '提交'))

const canUseTemporary = computed(() => {
  const name = customerKeyword.value.trim()
  if (!name) return false
  const exact = allCustomers.value.some((c) => c.name === name)
  return !exact
})

const showCustomerSuggestions = computed(() => {
  const kw = customerKeyword.value.trim()
  if (!kw) return false
  if (salesOrder.customer?.name && kw === salesOrder.customer.name) return false
  return filteredCustomers.value.length > 0 || canUseTemporary.value
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

const canUseTemporaryInPicker = computed(() => {
  const name = pickerKeyword.value.trim()
  if (!name) return false
  return !allCustomers.value.some((c) => c.name === name)
})

onLoad((query) => {
  if (!userStore.isLoggedIn || !userStore.isBoss) {
    uni.reLaunch({ url: '/packages/common/login/index' })
    return
  }
  editOrderId.value = Number(query?.orderId || 0)
})

async function loadEditOrder() {
  if (!editOrderId.value) return
  if (salesOrder.editOrderId === editOrderId.value) {
    customerKeyword.value = salesOrder.customer?.name || ''
    return
  }
  const order = await fetchBossOrderDetail(editOrderId.value)
  salesOrder.loadFromOrder(order)
  customerKeyword.value = salesOrder.customer?.name || ''
  uni.setNavigationBarTitle({ title: '修改订单' })
}

onShow(async () => {
  if (!userStore.isLoggedIn || !userStore.isBoss) {
    uni.reLaunch({ url: '/packages/common/login/index' })
    return
  }
  allCustomers.value = await fetchBossCustomers()
  if (editOrderId.value) {
    try {
      await loadEditOrder()
    } catch (e) {
      uni.showToast({ title: e instanceof Error ? e.message : '加载失败', icon: 'none' })
      setTimeout(() => uni.navigateBack(), 500)
      return
    }
  } else {
    customerKeyword.value = salesOrder.customer?.name || ''
  }
  filterCustomers()
  await loadProducts()
})

async function loadProducts() {
  try {
    allProducts.value = await fetchBossProducts()
  } catch {
    allProducts.value = []
  }
}

async function ensureProducts() {
  if (allProducts.value.length > 0) return
  await loadProducts()
}

async function handleRecognizedText(text: string) {
  const parsed = parseOrderText(text)
  if (parsed.length === 0) {
    uni.showToast({ title: '未识别到商品，请检查格式', icon: 'none' })
    return false
  }
  await ensureProducts()
  const result = applyParsedLines(parsed, allProducts.value, (line) => {
    salesOrder.upsertLine(line)
  })
  if (result.added === 0) {
    uni.showToast({
      title: `未匹配：${result.unmatched.slice(0, 3).join('、')}`,
      icon: 'none',
    })
    return false
  }
  const extra = result.unmatched.length > 0 ? `，${result.unmatched.length}项未匹配` : ''
  uni.showToast({ title: `已录入${result.added}种商品${extra}`, icon: 'success' })
  return true
}

function openBatchModal() {
  syncCustomerFromKeyword()
  showBatchModal.value = true
}

function closeBatchModal() {
  showBatchModal.value = false
}

function onBatchTextInput(e: { detail: { value: string } }) {
  batchText.value = e.detail.value
}

async function recognizeBatchText() {
  const text = batchText.value.trim()
  if (!text) {
    uni.showToast({ title: '请先粘贴或输入商品', icon: 'none' })
    return
  }
  batchParsing.value = true
  try {
    const ok = await handleRecognizedText(text)
    if (ok) {
      batchText.value = ''
      closeBatchModal()
    }
  } finally {
    batchParsing.value = false
  }
}

function goSingleProductFromBatch() {
  closeBatchModal()
  goProducts()
}

function openVoiceModal() {
  syncCustomerFromKeyword()
  voiceText.value = ''
  showVoiceModal.value = true
  voiceInputFocus.value = false
  setTimeout(() => {
    voiceInputFocus.value = true
  }, 200)
}

function closeVoiceModal() {
  voiceInputFocus.value = false
  showVoiceModal.value = false
}

function onVoiceTextInput(e: { detail: { value: string } }) {
  voiceText.value = e.detail.value
}

async function recognizeVoiceText() {
  const text = voiceText.value.trim()
  if (!text) {
    uni.showToast({ title: '请先说话或输入内容', icon: 'none' })
    return
  }
  voiceParsing.value = true
  try {
    const ok = await handleRecognizedText(text)
    if (ok) closeVoiceModal()
  } finally {
    voiceParsing.value = false
  }
}

function onCustomerNameInput(e: { detail: { value: string } }) {
  customerKeyword.value = e.detail.value
  filterCustomers()
}

function syncCustomerFromKeyword() {
  const name = customerKeyword.value.trim()
  if (!name) {
    salesOrder.clearCustomer()
    return
  }
  const exact = allCustomers.value.find((c) => c.name === name)
  if (exact) {
    salesOrder.setCustomer({ id: exact.id, name: exact.name, temporary: false })
  } else if (filteredCustomers.value.length === 1) {
    const only = filteredCustomers.value[0]
    customerKeyword.value = only.name
    salesOrder.setCustomer({ id: only.id, name: only.name, temporary: false })
  } else {
    salesOrder.setTemporaryCustomer(name)
  }
}

function openCustomerPicker() {
  pickerKeyword.value = customerKeyword.value
  filterCustomers()
  showCustomerPicker.value = true
}

function closeCustomerPicker() {
  showCustomerPicker.value = false
}

function onPickerKeywordInput(e: { detail: { value: string } }) {
  pickerKeyword.value = e.detail.value
}

function goSelectCustomer() {
  openCustomerPicker()
}

function goCreateCustomerPage() {
  closeCustomerPicker()
  const kw = encodeURIComponent(pickerKeyword.value.trim() || customerKeyword.value.trim())
  uni.navigateTo({ url: `/pages/boss/sales-order/select-customer?keyword=${kw}` })
}

function hasCustomerDebt(item: CustomerItem) {
  return (item.outstandingAmount || 0) > 0
}

function formatCustomerMoney(value?: number) {
  return Number(value || 0).toFixed(2)
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

function selectCustomer(item: CustomerItem) {
  customerKeyword.value = item.name
  salesOrder.setCustomer({ id: item.id, name: item.name, temporary: false })
  filteredCustomers.value = allCustomers.value
  closeCustomerPicker()
}

function selectTemporaryCustomer() {
  const name = (pickerKeyword.value || customerKeyword.value).trim()
  if (!name) return
  customerKeyword.value = name
  salesOrder.setTemporaryCustomer(name)
  filteredCustomers.value = allCustomers.value
  closeCustomerPicker()
}

function goProducts() {
  syncCustomerFromKeyword()
  uni.navigateTo({ url: '/pages/boss/sales-order/products' })
}

function removeLine(lineKey: string) {
  salesOrder.removeLine(lineKey)
}

function openQtyEditor(line: SalesOrderLine) {
  editingLineKey.value = line.lineKey
  editingLine.value = line
  numEditorMode.value = 'qty'
  numDraft.value = String(line.orderQty)
  showNumEditor.value = true
}

function openPriceEditor(line: SalesOrderLine) {
  editingLineKey.value = line.lineKey
  editingLine.value = line
  numEditorMode.value = 'price'
  numDraft.value = line.dealPrice != null ? String(line.dealPrice) : ''
  showNumEditor.value = true
}

function closeNumEditor() {
  showNumEditor.value = false
  editingLineKey.value = ''
  editingLine.value = null
  numDraft.value = ''
}

function inputNumKey(key: string) {
  if (key === '.' && numDraft.value.includes('.')) return
  if (numDraft.value === '0' && key !== '.') {
    numDraft.value = key
    return
  }
  numDraft.value += key
}

function backspaceNum() {
  numDraft.value = numDraft.value.slice(0, -1)
}

function clearNum() {
  numDraft.value = ''
}

function confirmNumEditor() {
  if (!editingLine.value) return
  const value = numDraft.value.trim()
  if (numEditorMode.value === 'qty') {
    if (!value) {
      uni.showToast({ title: '请输入数量', icon: 'none' })
      return
    }
    const qty = Number(value)
    if (!qty || qty <= 0) {
      uni.showToast({ title: '请输入有效数量', icon: 'none' })
      return
    }
    salesOrder.setLineQty(editingLine.value.lineKey, qty)
    closeNumEditor()
    return
  }
  if (!value) {
    salesOrder.setLinePrice(editingLine.value.lineKey, undefined)
    closeNumEditor()
    return
  }
  const price = Number(value)
  if (price < 0) {
    uni.showToast({ title: '请输入有效单价', icon: 'none' })
    return
  }
  salesOrder.setLinePrice(editingLine.value.lineKey, price)
  closeNumEditor()
}

function openUnitEditor(line: SalesOrderLine) {
  unitEditingLineKey.value = line.lineKey
  unitEditingLine.value = line
  unitKeyword.value = ''
  showUnitEditor.value = true
}

function closeUnitEditor() {
  showUnitEditor.value = false
  unitEditingLineKey.value = ''
  unitEditingLine.value = null
  unitKeyword.value = ''
}

function onUnitKeywordInput(e: { detail: { value: string } }) {
  unitKeyword.value = e.detail.value
}

function applyUnit(unit: string) {
  if (!unitEditingLine.value) return
  const oldKey = unitEditingLine.value.lineKey
  const ok = salesOrder.updateLineUnit(oldKey, unit)
  if (!ok) {
    uni.showToast({ title: '该商品此单位已存在', icon: 'none' })
    return
  }
  closeUnitEditor()
}

function createCustomUnit() {
  uni.showModal({
    title: '新建单位',
    editable: true,
    placeholderText: '请输入单位名称',
    success: (res) => {
      if (!res.confirm || !res.content) return
      const unit = normalizeUnit(res.content)
      if (!unit) return
      if (!PRESET_UNITS.includes(unit as typeof PRESET_UNITS[number]) && !customUnits.value.includes(unit)) {
        customUnits.value = [unit, ...customUnits.value]
      }
      applyUnit(unit)
    },
  })
}

function openRemarkInput() {
  uni.showModal({
    title: '订单备注',
    editable: true,
    placeholderText: '请输入备注',
    content: salesOrder.remark,
    success: (res) => {
      if (res.confirm && res.content != null) {
        salesOrder.setRemark(res.content.trim())
      }
    },
  })
}

function showComingSoon(name: string) {
  uni.showToast({ title: `${name}即将上线`, icon: 'none' })
}

async function handleSubmit() {
  syncCustomerFromKeyword()
  if (!salesOrder.customer?.name) {
    uni.showToast({ title: '请输入客户名称', icon: 'none' })
    goSelectCustomer()
    return
  }
  if (salesOrder.items.length === 0) {
    uni.showToast({ title: '请添加商品', icon: 'none' })
    return
  }
  submitting.value = true
  try {
    const basePayload = {
      deliveryDate: deliveryDateString(salesOrder.deliveryDay),
      remark: salesOrder.remark || undefined,
      items: salesOrder.items.map((line) => ({
        productId: line.productId,
        orderQty: line.orderQty,
        unit: line.unit,
        pickRemark: line.pickRemark,
        ...(isEditMode.value ? {} : { dealPrice: line.dealPrice }),
      })),
    }
    if (isEditMode.value && salesOrder.editOrderId) {
      const payload = salesOrder.customer?.id && !salesOrder.customer.temporary
        ? basePayload
        : { ...basePayload, customerName: salesOrder.customer?.name }
      const updated = await updateBossOrder(salesOrder.editOrderId, payload)
      const newId = updated.id
      salesOrder.reset()
      uni.showToast({ title: '改单成功，已生成新订单', icon: 'success' })
      setTimeout(() => {
        uni.redirectTo({ url: `/pages/boss/orders/detail/index?id=${newId}` })
      }, 400)
      return
    }
    const order = await createBossOrder(
      salesOrder.customer.id && !salesOrder.customer.temporary
        ? { ...basePayload, customerId: salesOrder.customer.id }
        : { ...basePayload, customerName: salesOrder.customer.name },
    )
    const customerName = salesOrder.customer.name
    salesOrder.reset()
    uni.redirectTo({
      url: `/pages/boss/sales-order/success?id=${order.id}&customer=${encodeURIComponent(customerName)}`,
    })
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
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 100vh;
  overflow: hidden;
  box-sizing: border-box;
  background: linear-gradient(180deg, #e8f8ef 0%, #f5f6f7 280rpx);
}

.customer-card {
  flex-shrink: 0;
  margin: 24rpx 24rpx 0;
  background: #fff;
  border-radius: 16rpx;
  overflow: hidden;
}

.main {
  flex: none;
  height: calc(100vh - 220rpx - 128rpx - env(safe-area-inset-bottom));
  min-height: 0;
  display: flex;
  flex-direction: column;
  padding: 16rpx 24rpx 0;
  box-sizing: border-box;
}

.items-card {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
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
  display: flex;
  align-items: center;
  gap: 12rpx;
  min-width: 0;
}

.customer-input {
  flex: 1;
  height: 64rpx;
  font-size: 34rpx;
  font-weight: 600;
  color: #222;
}

.customer-name-wrap {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.customer-name {
  font-size: 34rpx;
  font-weight: 600;
}

.temp-tag {
  flex-shrink: 0;
  padding: 4rpx 12rpx;
  font-size: 22rpx;
  color: #e67e22;
  border: 1rpx solid #e67e22;
  border-radius: 6rpx;
}

.customer-placeholder {
  font-size: 34rpx;
  color: #ccc;
}

.arrow-btn {
  flex-shrink: 0;
  min-width: 72rpx;
  min-height: 72rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: -12rpx -16rpx -12rpx 0;
}

.arrow {
  color: #ccc;
  font-size: 40rpx;
  line-height: 1;
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
  color: #222;
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

.customer-option.temp {
  color: #07c160;
  font-weight: 600;
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

.picker-footer {
  padding: 20rpx 32rpx 8rpx;
  text-align: center;
  border-top: 1rpx solid #f0f0f0;
}

.picker-create {
  font-size: 32rpx;
  color: #07c160;
  font-weight: 600;
}

.customer-suggestions {
  border-top: 1rpx solid #f2f3f5;
  background: #fafbfc;
}

.suggestion-item {
  padding: 20rpx 32rpx;
  border-bottom: 1rpx solid #f0f0f0;
}

.suggestion-item.temp {
  color: #07c160;
  font-size: 28rpx;
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

.items-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24rpx 32rpx 12rpx;
}

.items-title {
  font-size: 28rpx;
  font-weight: 600;
  color: #07c160;
}

.items-action {
  font-size: 26rpx;
  color: #2979ff;
}

.table-head,
.table-row {
  display: flex;
  align-items: flex-start;
  padding: 16rpx 32rpx;
  font-size: 24rpx;
}

.table-head {
  color: #999;
  border-bottom: 1rpx solid #f2f3f5;
}

.col-name { flex: 2; }
.col-qty { flex: 1; text-align: center; }
.col-unit { flex: 1; text-align: center; }
.col-price { flex: 1; text-align: center; }
.col-op { width: 48rpx; text-align: right; }

.edit-cell,
.price-cell {
  min-height: 48rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: -4rpx;
  padding: 4rpx 8rpx;
  border-radius: 8rpx;
  border: 2rpx solid transparent;
  background: #f5f6f7;
}

.edit-cell.active,
.price-cell.active {
  border-color: #07c160;
  background: #e8f8ef;
}

.cell-value,
.price-value {
  font-size: 28rpx;
  color: #333;
}

.price-cell.empty {
  background: #f5f6f7;
}

.price-placeholder {
  font-size: 24rpx;
  color: #bbb;
}

.line-name {
  display: block;
  font-size: 28rpx;
  color: #333;
}

.line-remark {
  display: block;
  margin-top: 8rpx;
  font-size: 22rpx;
  color: #e67e22;
}

.del {
  font-size: 28rpx;
}

.items-scroll {
  flex: 1;
  height: 0;
  background: #fff;
}

.empty-items {
  padding: 48rpx 32rpx;
  text-align: center;
}

.empty-text {
  display: block;
  font-size: 26rpx;
  color: #999;
}

.empty-link {
  display: block;
  margin-top: 12rpx;
  font-size: 28rpx;
  color: #2979ff;
}

.add-item-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
  margin: 16rpx 32rpx 24rpx;
  padding: 24rpx;
  border: 2rpx dashed #07c160;
  border-radius: 12rpx;
  background: #f8fdf9;
}

.add-item-plus {
  font-size: 36rpx;
  color: #07c160;
  font-weight: 600;
  line-height: 1;
}

.add-item-text {
  font-size: 28rpx;
  color: #07c160;
  font-weight: 600;
}

.remark-bar {
  flex-shrink: 0;
  margin: 16rpx 0 16rpx;
  padding: 24rpx 32rpx;
  background: #eef0f2;
  border-radius: 12rpx;
  font-size: 26rpx;
  color: #888;
}

.price-mask {
  position: fixed;
  inset: 0;
  z-index: 200;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
}

.price-sheet {
  background: #fff;
  border-radius: 24rpx 24rpx 0 0;
  padding-bottom: env(safe-area-inset-bottom);
}

.price-head {
  display: flex;
  align-items: center;
  padding: 24rpx 32rpx 8rpx;
}

.price-close {
  width: 80rpx;
  font-size: 48rpx;
  color: #999;
  line-height: 1;
}

.price-title {
  flex: 1;
  text-align: center;
  font-size: 34rpx;
  font-weight: 600;
}

.price-unit {
  min-width: 80rpx;
  text-align: right;
  font-size: 26rpx;
  color: #999;
}

.price-product {
  display: block;
  padding: 8rpx 32rpx 16rpx;
  font-size: 28rpx;
  color: #666;
}

.price-field {
  margin: 0 32rpx 24rpx;
  height: 96rpx;
  padding: 0 24rpx;
  display: flex;
  align-items: center;
  background: #f5f6f7;
  border-radius: 12rpx;
  border: 2rpx solid #07c160;
}

.price-draft {
  font-size: 44rpx;
  font-weight: 600;
  color: #222;
}

.price-draft.placeholder {
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

.unit-panel {
  height: 72vh;
  display: flex;
  flex-direction: column;
  background: #fff;
}

.unit-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24rpx 32rpx 12rpx;
}

.unit-cancel {
  font-size: 30rpx;
  color: #666;
  min-width: 80rpx;
}

.unit-title {
  font-size: 32rpx;
  font-weight: 600;
}

.unit-new {
  font-size: 30rpx;
  color: #2979ff;
  min-width: 80rpx;
  text-align: right;
}

.unit-product {
  display: block;
  padding: 0 32rpx 12rpx;
  font-size: 28rpx;
  color: #666;
}

.unit-search-wrap {
  padding: 0 24rpx 16rpx;
}

.unit-search-input {
  height: 72rpx;
  padding: 0 24rpx;
  background: #f5f6f7;
  border-radius: 12rpx;
  font-size: 28rpx;
}

.unit-grid-wrap {
  flex: 1;
  height: 0;
  padding: 0 24rpx calc(24rpx + env(safe-area-inset-bottom));
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
  background: #f5f6f7;
  border-radius: 12rpx;
  border: 2rpx solid transparent;
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

.batch-panel {
  padding: 0 32rpx calc(32rpx + env(safe-area-inset-bottom));
  background: #fff;
}

.batch-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 28rpx 0 20rpx;
}

.batch-close {
  width: 56rpx;
  font-size: 44rpx;
  color: #999;
  line-height: 1;
}

.batch-title {
  font-size: 34rpx;
  font-weight: 600;
  color: #222;
}

.batch-single {
  min-width: 120rpx;
  text-align: right;
  font-size: 28rpx;
  color: #2979ff;
}

.batch-textarea {
  width: 100%;
  min-height: 280rpx;
  padding: 24rpx;
  background: #f5f6f7;
  border-radius: 16rpx;
  font-size: 28rpx;
  color: #333;
  box-sizing: border-box;
}

.batch-recognize-btn {
  width: 100%;
  height: 88rpx;
  line-height: 88rpx;
  margin: 24rpx 0 0;
  padding: 0;
  background: #07c160;
  color: #fff;
  font-size: 32rpx;
  font-weight: 600;
  border-radius: 12rpx;
  border: none;
}

.batch-recognize-btn::after {
  border: none;
}

.voice-hint {
  display: block;
  margin-bottom: 16rpx;
  font-size: 26rpx;
  line-height: 1.5;
  color: #66736b;
}

.voice-textarea {
  min-height: 200rpx;
}
</style>
