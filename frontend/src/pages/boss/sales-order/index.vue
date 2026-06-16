<template>
  <view class="page boss-page">
    <scroll-view scroll-y class="boss-page-scroll boss-scroll-with-footer">
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
        <text class="arrow" @tap="openCustomerPicker">›</text>
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
        <text class="col-op del" @tap="removeLine(line.lineKey)">🗑</text>
      </view>
    </view>

    <view class="remark-bar" @tap="openRemarkInput">
      <text>{{ salesOrder.remark || '添加订单备注' }}</text>
    </view>
    </scroll-view>

    <view class="boss-bottom-bar">
      <view class="boss-tool-item" @tap="goProducts">
        <text class="boss-tool-icon">📚</text>
        <text class="boss-tool-label">商品库</text>
      </view>
      <view class="boss-tool-item" @tap="showComingSoon('批量录入')">
        <text class="boss-tool-icon">▦</text>
        <text class="boss-tool-label">批量录入</text>
      </view>
      <view class="boss-tool-item" @tap="showComingSoon('语音录入')">
        <text class="boss-tool-icon">🎤</text>
        <text class="boss-tool-label">语音录入</text>
      </view>
      <button class="boss-primary-btn" :loading="submitting" @tap="handleSubmit">{{ submitLabel }}</button>
    </view>

    <u-popup :show="showCustomerPicker" mode="bottom" round="16" @close="closeCustomerPicker">
      <view class="picker-panel">
        <view class="picker-current">
          <text class="picker-current-label">当前输入</text>
          <text class="picker-current-name">{{ customerKeyword.trim() || '未输入' }}</text>
          <text class="picker-current-tip">名称在上方输入框直接修改</text>
        </view>
        <text class="picker-sub">客户匹配</text>
        <scroll-view scroll-y class="customer-list">
          <view
            v-if="canUseTemporary"
            class="customer-option temp"
            @tap="selectTemporaryCustomer"
          >
            使用「{{ customerKeyword.trim() }}」临时开单
          </view>
          <view
            v-for="item in filteredCustomers"
            :key="item.id"
            class="customer-option"
            @tap="selectCustomer(item)"
          >
            {{ item.name }}
          </view>
          <view v-if="filteredCustomers.length === 0 && !canUseTemporary" class="no-match">
            输入客户名称后可临时开单
          </view>
        </scroll-view>
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
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { filterUnits, mergeUnits, normalizeUnit, PRESET_UNITS } from '../../../constants/units'
import { fetchBossCustomers, type CustomerItem } from '../../../api/customer'
import { createBossOrder, fetchBossOrderDetail, updateBossOrder } from '../../../api/order'
import { deliveryDateString, formatDeliveryLabel, useSalesOrderStore, type SalesOrderLine } from '../../../stores/salesOrder'
import { useUserStore } from '../../../stores/user'

const userStore = useUserStore()
const salesOrder = useSalesOrderStore()
const submitting = ref(false)
const showCustomerPicker = ref(false)
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

onLoad((query) => {
  if (!userStore.isLoggedIn || !userStore.isBoss) {
    uni.reLaunch({ url: '/pages/login/index' })
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
    uni.reLaunch({ url: '/pages/login/index' })
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
})

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
  } else {
    salesOrder.setTemporaryCustomer(name)
  }
}

function openCustomerPicker() {
  filterCustomers()
  showCustomerPicker.value = true
}

function closeCustomerPicker() {
  showCustomerPicker.value = false
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
  closeCustomerPicker()
  filteredCustomers.value = allCustomers.value
}

function selectTemporaryCustomer() {
  const name = customerKeyword.value.trim()
  if (!name) return
  salesOrder.setTemporaryCustomer(name)
  closeCustomerPicker()
  filteredCustomers.value = allCustomers.value
}

function goProducts() {
  syncCustomerFromKeyword()
  if (!salesOrder.customer?.name) {
    uni.showToast({ title: '请先输入客户名称', icon: 'none' })
    openCustomerPicker()
    return
  }
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
    openCustomerPicker()
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
        dealPrice: line.dealPrice,
        pickRemark: line.pickRemark,
      })),
    }
    if (isEditMode.value && salesOrder.editOrderId) {
      const payload = salesOrder.customer?.id && !salesOrder.customer.temporary
        ? basePayload
        : { ...basePayload, customerName: salesOrder.customer?.name }
      await updateBossOrder(salesOrder.editOrderId, payload)
      const editId = salesOrder.editOrderId
      salesOrder.reset()
      uni.showToast({ title: '已保存', icon: 'success' })
      setTimeout(() => {
        uni.redirectTo({ url: `/pages/boss/orders/detail/index?id=${editId}` })
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
  background: linear-gradient(180deg, #e8f8ef 0%, #f5f6f7 280rpx);
}

.customer-card,
.items-card {
  margin: 24rpx;
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

.arrow {
  color: #ccc;
  font-size: 36rpx;
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

.empty-items {
  padding: 80rpx 32rpx;
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

.remark-bar {
  margin: 0 24rpx 24rpx;
  padding: 24rpx 32rpx;
  background: #eef0f2;
  border-radius: 12rpx;
  font-size: 26rpx;
  color: #888;
}

.picker-panel {
  padding: 32rpx 24rpx 48rpx;
  max-height: 70vh;
}

.picker-current {
  padding: 20rpx 8rpx 24rpx;
  border-bottom: 1rpx solid #f2f3f5;
  margin-bottom: 8rpx;
}

.picker-current-label {
  display: block;
  font-size: 24rpx;
  color: #999;
}

.picker-current-name {
  display: block;
  margin-top: 8rpx;
  font-size: 34rpx;
  font-weight: 600;
  color: #222;
}

.picker-current-tip {
  display: block;
  margin-top: 8rpx;
  font-size: 24rpx;
  color: #07c160;
}

.picker-sub {
  display: block;
  margin: 24rpx 8rpx 12rpx;
  font-size: 26rpx;
  color: #999;
}

.customer-list {
  max-height: 480rpx;
}

.customer-option.temp {
  color: #e67e22;
  font-weight: 600;
  border-bottom: 1rpx solid #fdebd0;
}

.customer-option {
  padding: 24rpx 8rpx;
  font-size: 32rpx;
  color: #07c160;
  border-bottom: 1rpx solid #f5f6f7;
}

.no-match {
  padding: 40rpx;
  text-align: center;
  color: #999;
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
</style>
