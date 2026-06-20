<template>
  <view class="page">
    <view class="search-bar">
      <view class="search-input-wrap">
        <AppIcon class="search-icon" name="search" tone="green" :tile="false" :size="19" />
        <input
          v-model="keyword"
          class="search-input"
          placeholder="搜索客户名称"
          confirm-type="search"
          @confirm="confirmFilter"
        />
      </view>
      <view class="filter-btn" @tap="toggleFilterPanel">
        <AppIcon class="filter-icon" name="filter" tone="gray" :size="20" :tile-size="48" :radius="14" />
      </view>
    </view>

    <view v-if="showSubscribeBar" class="subscribe-bar">
      <text class="subscribe-text">客户下单后可发微信提醒，请先开启</text>
      <view class="subscribe-btn" @tap="handleSubscribeNotify">开启提醒</view>
    </view>

    <view class="tab-row">
      <view class="main-tabs">
        <view
          v-for="tab in pickTabs"
          :key="tab.value"
          class="main-tab"
          :class="{ active: pickFilter === tab.value }"
          @tap="switchPickTab(tab.value)"
        >
          {{ tab.label }}
        </view>
      </view>
      <view class="batch-btn" @tap="showBatchTip">
        <AppIcon class="batch-icon" name="batch" tone="gray" :tile="false" :size="18" />
        <text>批量</text>
      </view>
    </view>

    <view class="date-filter-bar">
      <view class="date-dropdown" @tap="openFilterPopup">
        <text class="date-prefix">{{ appliedDateTypeLabel }}：</text>
        <text class="date-range">{{ appliedRangeLabel }}</text>
        <text class="date-arrow">▼</text>
      </view>
      <view class="quick-day-tabs">
        <text
          class="quick-day"
          :class="{ active: isQuickActive('today') }"
          @tap="applyQuickDay('today')"
        >今日</text>
        <text
          class="quick-day"
          :class="{ active: isQuickActive('tomorrow') }"
          @tap="applyQuickDay('tomorrow')"
        >明日</text>
      </view>
    </view>

    <view v-if="loading" class="loading-wrap">
      <u-loading-icon text="加载中" />
    </view>

    <view v-else-if="displayOrders.length === 0" class="empty-wrap">
      <u-empty mode="order" text="暂无订单" />
    </view>

    <view v-else class="list">
      <view
        v-for="item in displayOrders"
        :key="item.id"
        class="card"
        @tap="goDetail"
        :data-id="item.id"
      >
        <view class="card-head">
          <view class="head-main">
            <text class="customer-name">{{ item.customerName || '未知客户' }}</text>
            <text class="order-no">{{ item.orderNo }}</text>
          </view>
          <text class="pay-status" :class="payStatusClass(item)">{{ item.paymentStatusLabel || '待收款' }}</text>
        </view>

        <view v-if="item.sourceLabel" class="source-tag">
          <text>{{ item.sourceLabel }}</text>
        </view>

        <view class="info-line">
          <text class="info-label">下单时间</text>
          <text class="info-value">{{ item.createdAtText }}</text>
        </view>
        <view class="info-line">
          <text class="info-label">配送</text>
          <text class="info-value">{{ item.deliveryAtText }}</text>
        </view>
        <view class="info-line">
          <text class="info-label">信息</text>
          <text class="info-value">
            {{ item.itemCount || 0 }}种
            <text v-if="item.amountText" class="amount-text">{{ item.amountText }}</text>
            <text v-if="item.priceIncomplete" class="price-warn">(价格未录完)</text>
          </text>
        </view>

        <view v-if="showCustomerDebt(item)" class="debt-line">
          <text class="debt-label">客户欠款</text>
          <text class="debt-value">¥ {{ formatCustomerDebt(item.customerOutstandingAmount) }}</text>
        </view>

        <OrderFlowBar :steps="item.flowSteps" @tap="(key) => onFlowTap(key, item)" />
      </view>
      <view class="list-end">—— 没有更多了 ——</view>
    </view>

    <BossTabbar active="orders" />

    <u-popup :show="filterPopupVisible" mode="bottom" round="16" @close="closeFilterPopup">
      <view class="filter-panel">
        <view class="filter-panel-head">
          <text class="filter-title">日期筛选</text>
          <text class="filter-close" @tap="closeFilterPopup">×</text>
        </view>
        <view class="type-row">
          <view
            class="type-btn"
            :class="{ active: draftDateType === 'ORDER' }"
            @tap="switchDraftDateType('ORDER')"
          >下单时间</view>
          <view
            class="type-btn"
            :class="{ active: draftDateType === 'DELIVERY' }"
            @tap="switchDraftDateType('DELIVERY')"
          >配送时间</view>
        </view>

        <view class="preset-grid">
          <view
            v-for="item in currentPresets"
            :key="item.key"
            class="preset-btn"
            :class="{ active: draftPreset === item.key }"
            @tap="applyDraftPreset(item.key)"
          >
            <text class="preset-label">{{ item.label }}</text>
            <text v-if="item.sub" class="preset-sub">{{ item.sub }}</text>
          </view>
        </view>

        <text class="custom-title">自定义日期</text>
        <view class="custom-row" @tap="openRangePicker">
          <text class="custom-date" :class="{ placeholder: !draftFrom }">{{ formatCustomDate(draftFrom) || '起始日期' }}</text>
          <text class="custom-sep">至</text>
          <text class="custom-date" :class="{ placeholder: !draftTo }">{{ formatCustomDate(draftTo) || '截止日期' }}</text>
        </view>

        <view class="filter-actions">
          <view class="reset-btn" @tap="resetFilter">
            <text class="reset-icon">⌫</text>
            <text>重置</text>
          </view>
          <button class="confirm-btn" @tap="confirmFilter">确定</button>
        </view>
      </view>
    </u-popup>

    <OrderDateRangePicker
      :show="rangePickerVisible"
      :start-date="draftFrom"
      :end-date="draftTo"
      @close="rangePickerVisible = false"
      @confirm="onRangeConfirm"
    />

    <u-modal
      :show="priceModalVisible"
      title="提示"
      content="对账单中存在价格未录完的商品，是否继续发送？"
      :show-cancel-button="true"
      cancel-text="继续发送"
      confirm-text="返回改价"
      confirm-color="#22c55e"
      @cancel="continuePrint"
      @confirm="goPricingFromModal"
      @close="priceModalVisible = false"
    />
  </view>
</template>

<script setup lang="ts">
import { onLoad, onShow } from '@dcloudio/uni-app'
import { computed, ref } from 'vue'
import { fetchBossOrders, type OrderInfo } from '../../../api/order'
import { fetchMiniProgramConfig } from '../../../api/config'
import AppIcon from '../../../components/AppIcon.vue'
import BossTabbar from '../../../components/boss-tabbar/index.vue'
import OrderDateRangePicker from '../../../components/OrderDateRangePicker.vue'
import OrderFlowBar from '../../../components/OrderFlowBar.vue'
import { useUserStore } from '../../../stores/user'
import {
  buildOrderFlowSteps,
  isPaid,
  isPickDone,
  isPriced,
  type FlowStepKey,
  type OrderFlowStep,
} from '../../../utils/order-flow'
import { requestOrderNotifySubscribe } from '../../../utils/wechat-subscribe'

interface OrderDisplay extends OrderInfo {
  createdAtText: string
  deliveryAtText: string
  amountText: string
  flowSteps: OrderFlowStep[]
}

type DateType = 'ORDER' | 'DELIVERY'
type PresetKey = 'today' | 'tomorrow' | 'yesterday' | 'last7' | 'last30' | 'custom' | ''

const userStore = useUserStore()
const orders = ref<OrderInfo[]>([])
const loading = ref(false)
const keyword = ref('')
const pickFilter = ref<'ALL' | 'UNPICKED' | 'PICKED'>('ALL')
const filterPopupVisible = ref(false)
const rangePickerVisible = ref(false)
const priceModalVisible = ref(false)
const pendingPrintOrderId = ref(0)
const showSubscribeBar = ref(false)

const draftDateType = ref<DateType>('DELIVERY')
const draftPreset = ref<PresetKey>('')
const draftFrom = ref('')
const draftTo = ref('')

const appliedDateType = ref<DateType>('DELIVERY')
const appliedFrom = ref('')
const appliedTo = ref('')

const pickTabs = [
  { label: '全部订单', value: 'ALL' as const },
  { label: '未拣完', value: 'UNPICKED' as const },
  { label: '已拣完', value: 'PICKED' as const },
]

const deliveryPresets = computed(() => [
  { key: 'today' as const, label: '今日' },
  { key: 'tomorrow' as const, label: '明日' },
  { key: 'last7' as const, label: '近7日', sub: presetRangeLabel(7) },
  { key: 'last30' as const, label: '近30日', sub: presetRangeLabel(30) },
])

const orderPresets = computed(() => [
  { key: 'today' as const, label: '今日' },
  { key: 'yesterday' as const, label: '昨日' },
  { key: 'last7' as const, label: '近7日', sub: presetRangeLabel(7) },
  { key: 'last30' as const, label: '近30日', sub: presetRangeLabel(30) },
])

const currentPresets = computed(() =>
  draftDateType.value === 'ORDER' ? orderPresets.value : deliveryPresets.value,
)

const displayOrders = computed<OrderDisplay[]>(() =>
  orders.value.map((item) => ({
    ...item,
    createdAtText: formatDateTime(item.createdAt),
    deliveryAtText: formatDelivery(item.deliveryDate),
    amountText: formatAmount(item.amount),
    flowSteps: buildOrderFlowSteps(item),
  })),
)

const appliedDateTypeLabel = computed(() =>
  appliedDateType.value === 'ORDER' ? '下单' : '配送',
)

const appliedRangeLabel = computed(() => {
  if (!appliedFrom.value || !appliedTo.value) return '全部'
  const from = formatShortDate(appliedFrom.value)
  const to = formatShortDate(appliedTo.value)
  if (from === to) return from
  return `${from}至${to}`
})

onLoad((query) => {
  if (query?.keyword) {
    keyword.value = decodeURIComponent(String(query.keyword))
  }
})

onShow(async () => {
  if (!userStore.isLoggedIn || !userStore.isBoss) {
    uni.reLaunch({ url: '/pages/login/index' })
    return
  }
  try {
    const config = await fetchMiniProgramConfig()
    showSubscribeBar.value = !!config.orderNotifyTemplateId
  } catch {
    showSubscribeBar.value = false
  }
  await refresh()
})

function handleSubscribeNotify() {
  requestOrderNotifySubscribe()
}

async function refresh() {
  loading.value = true
  try {
    const hasDateFilter = !!(appliedFrom.value && appliedTo.value)
    orders.value = await fetchBossOrders({
      keyword: keyword.value.trim() || undefined,
      pickFilter: pickFilter.value,
      dateType: hasDateFilter ? appliedDateType.value : undefined,
      dateFrom: hasDateFilter ? appliedFrom.value : undefined,
      dateTo: hasDateFilter ? appliedTo.value : undefined,
    })
  } catch (err) {
    orders.value = []
    uni.showToast({
      title: err instanceof Error ? err.message : '加载失败',
      icon: 'none',
    })
  } finally {
    loading.value = false
  }
}

function switchPickTab(value: 'ALL' | 'UNPICKED' | 'PICKED') {
  if (pickFilter.value === value) return
  pickFilter.value = value
  refresh()
}

function switchDraftDateType(type: DateType) {
  if (draftDateType.value === type) return
  draftDateType.value = type
  draftPreset.value = ''
  draftFrom.value = ''
  draftTo.value = ''
}

function applyDraftPreset(key: PresetKey) {
  draftPreset.value = key
  if (key === 'today') {
    const today = formatDate(new Date())
    draftFrom.value = today
    draftTo.value = today
    return
  }
  if (key === 'tomorrow') {
    const d = shiftDate(new Date(), 1)
    draftFrom.value = d
    draftTo.value = d
    return
  }
  if (key === 'yesterday') {
    const d = shiftDate(new Date(), -1)
    draftFrom.value = d
    draftTo.value = d
    return
  }
  if (key === 'last7') {
    const range = getLastNDaysRange(7)
    draftFrom.value = range.from
    draftTo.value = range.to
    return
  }
  if (key === 'last30') {
    const range = getLastNDaysRange(30)
    draftFrom.value = range.from
    draftTo.value = range.to
  }
}

function resetFilter() {
  draftDateType.value = 'DELIVERY'
  draftPreset.value = ''
  draftFrom.value = ''
  draftTo.value = ''
  appliedDateType.value = 'DELIVERY'
  appliedFrom.value = ''
  appliedTo.value = ''
  closeFilterPopup()
  refresh()
}

function confirmFilter() {
  if ((draftFrom.value && !draftTo.value) || (!draftFrom.value && draftTo.value)) {
    uni.showToast({ title: '请完整选择日期范围', icon: 'none' })
    return
  }
  appliedDateType.value = draftDateType.value
  appliedFrom.value = draftFrom.value
  appliedTo.value = draftTo.value
  closeFilterPopup()
  refresh()
}

function openFilterPopup() {
  draftDateType.value = appliedDateType.value
  draftFrom.value = appliedFrom.value
  draftTo.value = appliedTo.value
  draftPreset.value = inferDraftPreset()
  filterPopupVisible.value = true
}

function closeFilterPopup() {
  filterPopupVisible.value = false
}

function applyQuickDay(day: 'today' | 'tomorrow') {
  const d = day === 'today' ? formatDate(new Date()) : shiftDate(new Date(), 1)
  appliedDateType.value = 'DELIVERY'
  appliedFrom.value = d
  appliedTo.value = d
  draftDateType.value = 'DELIVERY'
  draftFrom.value = d
  draftTo.value = d
  draftPreset.value = day
  refresh()
}

function isQuickActive(day: 'today' | 'tomorrow') {
  const d = day === 'today' ? formatDate(new Date()) : shiftDate(new Date(), 1)
  return appliedDateType.value === 'DELIVERY'
    && appliedFrom.value === d
    && appliedTo.value === d
}

function inferDraftPreset(): PresetKey {
  if (!draftFrom.value || !draftTo.value) return ''
  const today = formatDate(new Date())
  const tomorrow = shiftDate(new Date(), 1)
  const yesterday = shiftDate(new Date(), -1)
  if (draftFrom.value === today && draftTo.value === today) return 'today'
  if (draftFrom.value === tomorrow && draftTo.value === tomorrow) return 'tomorrow'
  if (draftDateType.value === 'ORDER' && draftFrom.value === yesterday && draftTo.value === yesterday) {
    return 'yesterday'
  }
  const last7 = getLastNDaysRange(7)
  if (draftFrom.value === last7.from && draftTo.value === last7.to) return 'last7'
  const last30 = getLastNDaysRange(30)
  if (draftFrom.value === last30.from && draftTo.value === last30.to) return 'last30'
  return 'custom'
}

function openRangePicker() {
  rangePickerVisible.value = true
}

function onRangeConfirm(payload: { from: string; to: string }) {
  draftFrom.value = payload.from
  draftTo.value = payload.to
  draftPreset.value = 'custom'
}

function toggleFilterPanel() {
  openFilterPopup()
}

function formatShortDate(value: string) {
  const parts = value.split('-')
  if (parts.length < 3) return value
  return `${parts[1]}-${parts[2]}`
}

function getLastNDaysRange(days: number) {
  const to = new Date()
  const from = new Date()
  from.setDate(to.getDate() - (days - 1))
  return { from: formatDate(from), to: formatDate(to) }
}

function shiftDate(base: Date, offset: number) {
  const d = new Date(base)
  d.setDate(d.getDate() + offset)
  return formatDate(d)
}

function presetRangeLabel(days: number) {
  const range = getLastNDaysRange(days)
  return `${formatDotMonthDay(range.from)}-${formatDotMonthDay(range.to)}`
}

function formatDate(date: Date) {
  const y = date.getFullYear()
  const m = `${date.getMonth() + 1}`.padStart(2, '0')
  const d = `${date.getDate()}`.padStart(2, '0')
  return `${y}-${m}-${d}`
}

function formatDotMonthDay(value: string) {
  const parts = value.split('-')
  if (parts.length < 3) return value
  return `${parts[1]}.${parts[2]}`
}

function formatCustomDate(value: string) {
  if (!value) return ''
  return value.replace(/-/g, '.')
}

function formatDateTime(value?: string) {
  if (!value) return '—'
  return value.replace('T', ' ').slice(0, 16)
}

function formatDelivery(deliveryDate?: string) {
  if (deliveryDate) {
    return `${deliveryDate} 23:00`
  }
  return '—'
}

function formatAmount(amount?: number) {
  if (amount == null) return ''
  return ` ¥${Number(amount).toFixed(2)}`
}

function formatCustomerDebt(amount?: number) {
  return Number(amount || 0).toFixed(2)
}

function showCustomerDebt(item: OrderInfo) {
  return !!item.printed && (item.customerOutstandingAmount || 0) > 0
}

function payStatusClass(item: OrderInfo) {
  if (isPaid(item)) return 'paid'
  if (item.amount != null && !isPaid(item)) return 'unpaid'
  return ''
}

const PICKABLE_STATUSES = ['PENDING_CONFIRM', 'PENDING_PICK', 'PICKING', 'PICKED', 'PENDING_PRICE']

function canPick(item: OrderInfo) {
  const total = item.itemCount || 0
  if (total === 0) return false
  if (!PICKABLE_STATUSES.includes(item.status)) return false
  if (item.status === 'PENDING_PRICE' && item.amount != null) return false
  return true
}

function onFlowTap(key: FlowStepKey, item: OrderInfo) {
  switch (key) {
    case 'confirm':
      goDetailById(item.id)
      break
    case 'pick':
      if (canPick(item)) {
        goPick(item.id)
      } else if (isPickDone(item)) {
        uni.showToast({ title: '分拣已完成', icon: 'none' })
      } else {
        uni.showToast({ title: '当前状态不可分拣', icon: 'none' })
      }
      break
    case 'price':
      if (!isPickDone(item)) {
        uni.showToast({ title: '请先完成分拣', icon: 'none' })
        return
      }
      if (isPriced(item)) {
        goDetailById(item.id)
      } else {
        uni.navigateTo({ url: `/pages/boss/pricing/detail/index?id=${item.id}` })
      }
      break
    case 'print':
      handlePrint(item)
      break
    case 'pay':
      goDetailById(item.id)
      break
  }
}

function goDetailById(id: number) {
  uni.navigateTo({ url: `/pages/boss/orders/detail/index?id=${id}` })
}

function goPick(id: number) {
  uni.navigateTo({ url: `/pages/boss/orders/pick/index?id=${id}` })
}

function goDetail(e: { currentTarget: { dataset: { id?: string | number } } }) {
  const id = Number(e.currentTarget.dataset.id)
  uni.navigateTo({ url: `/pages/boss/orders/detail/index?id=${id}` })
}

function handlePrint(item: OrderInfo) {
  pendingPrintOrderId.value = item.id
  if (item.priceIncomplete || item.amount == null) {
    priceModalVisible.value = true
    return
  }
  goPrintPage(item.id)
}

function continuePrint() {
  priceModalVisible.value = false
  if (pendingPrintOrderId.value) {
    goPrintPage(pendingPrintOrderId.value)
  }
}

function goPricingFromModal() {
  priceModalVisible.value = false
  if (pendingPrintOrderId.value) {
    uni.navigateTo({ url: `/pages/boss/pricing/detail/index?id=${pendingPrintOrderId.value}` })
  }
}

function goPrintPage(id: number) {
  uni.navigateTo({ url: `/pages/boss/orders/print/index?id=${id}` })
}

function showBatchTip() {
  uni.showToast({ title: '批量功能开发中', icon: 'none' })
}
</script>

<style scoped lang="scss">
.page {
  min-height: 100vh;
  background: #f5f6f8;
  padding-bottom: calc(116rpx + env(safe-area-inset-bottom));
}

.search-bar {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 16rpx 24rpx;
  background: #fff;
}

.subscribe-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
  padding: 16rpx 24rpx;
  background: #fff7e8;
  border-bottom: 1rpx solid #f0e4cc;
}

.subscribe-text {
  flex: 1;
  font-size: 24rpx;
  color: #8a6a2f;
  line-height: 1.4;
}

.subscribe-btn {
  flex-shrink: 0;
  padding: 10rpx 20rpx;
  font-size: 24rpx;
  color: #fff;
  background: #07c160;
  border-radius: 999rpx;
}

.search-input-wrap {
  flex: 1;
  display: flex;
  align-items: center;
  height: 72rpx;
  padding: 0 24rpx;
  background: #f5f6f8;
  border-radius: 36rpx;
}

.search-icon {
  margin-right: 12rpx;
  width: 42rpx;
  height: 42rpx;
  border-radius: 12rpx;
  background: #eef2ed;
}

.search-input {
  flex: 1;
  font-size: 28rpx;
}

.filter-btn {
  width: 72rpx;
  height: 72rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.filter-icon {
  width: 48rpx;
  height: 48rpx;
  border-radius: 14rpx;
}

.tab-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24rpx;
  background: #fff;
  border-bottom: 1rpx solid #eee;
}

.main-tabs {
  display: flex;
  gap: 40rpx;
}

.main-tab {
  position: relative;
  padding: 24rpx 0;
  font-size: 30rpx;
  color: #666;
}

.main-tab.active {
  color: #111;
  font-weight: 600;
}

.main-tab.active::after {
  content: '';
  position: absolute;
  left: 50%;
  bottom: 0;
  width: 48rpx;
  height: 6rpx;
  margin-left: -24rpx;
  background: #22c55e;
  border-radius: 3rpx;
}

.batch-btn {
  display: flex;
  align-items: center;
  gap: 8rpx;
  font-size: 28rpx;
  color: #333;
}

.batch-icon {
  width: 42rpx;
  height: 42rpx;
  border-radius: 12rpx;
  background: #eef2ed;
}

.date-filter-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16rpx 24rpx;
  background: #fff;
  border-bottom: 1rpx solid #eee;
}

.date-dropdown {
  display: flex;
  align-items: center;
  min-width: 0;
  flex: 1;
}

.date-prefix {
  font-size: 26rpx;
  color: #666;
  flex-shrink: 0;
}

.date-range {
  font-size: 26rpx;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.date-arrow {
  margin-left: 8rpx;
  font-size: 20rpx;
  color: #999;
  flex-shrink: 0;
}

.quick-day-tabs {
  display: flex;
  gap: 12rpx;
  flex-shrink: 0;
  margin-left: 16rpx;
}

.quick-day {
  padding: 8rpx 24rpx;
  font-size: 24rpx;
  color: #666;
  background: #f5f6f8;
  border-radius: 999rpx;
}

.quick-day.active {
  color: #07c160;
  background: #e8f8ef;
  font-weight: 600;
}

.filter-panel {
  padding: 24rpx 24rpx calc(20rpx + env(safe-area-inset-bottom));
  background: #fff;
  max-height: 78vh;
  overflow-y: auto;
}

.filter-panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20rpx;
}

.filter-close {
  width: 48rpx;
  height: 48rpx;
  line-height: 48rpx;
  text-align: center;
  font-size: 40rpx;
  color: #999;
}

.filter-title {
  display: block;
  font-size: 30rpx;
  font-weight: 600;
  color: #111;
}

.type-row {
  display: flex;
  gap: 20rpx;
  margin-bottom: 20rpx;
}

.type-btn {
  flex: 1;
  height: 72rpx;
  line-height: 72rpx;
  text-align: center;
  font-size: 28rpx;
  color: #666;
  background: #f5f6f8;
  border-radius: 12rpx;
  border: 2rpx solid transparent;
}

.type-btn.active {
  color: #07c160;
  background: #ecfdf3;
  border-color: #07c160;
  font-weight: 600;
}

.preset-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16rpx;
  margin-bottom: 24rpx;
}

.preset-btn {
  min-height: 88rpx;
  padding: 12rpx 8rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: #f5f6f8;
  border-radius: 12rpx;
  border: 2rpx solid transparent;
  box-sizing: border-box;
}

.preset-btn.active {
  background: #ecfdf3;
  border-color: #07c160;
}

.preset-label {
  font-size: 28rpx;
  color: #333;
}

.preset-btn.active .preset-label {
  color: #07c160;
  font-weight: 600;
}

.preset-sub {
  margin-top: 4rpx;
  font-size: 22rpx;
  color: #999;
}

.preset-btn.active .preset-sub {
  color: #07c160;
}

.custom-title {
  display: block;
  margin-bottom: 16rpx;
  font-size: 28rpx;
  color: #666;
}

.custom-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 24rpx;
  padding: 24rpx;
  background: #f5f6f8;
  border-radius: 12rpx;
}

.custom-date {
  flex: 1;
  text-align: center;
  font-size: 28rpx;
  color: #333;
}

.custom-date.placeholder {
  color: #bbb;
}

.custom-sep {
  font-size: 28rpx;
  color: #999;
}

.filter-actions {
  display: flex;
  align-items: center;
  gap: 20rpx;
  margin-top: 24rpx;
}

.reset-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 96rpx;
  font-size: 24rpx;
  color: #666;
}

.reset-icon {
  font-size: 32rpx;
  line-height: 1;
  margin-bottom: 4rpx;
}

.confirm-btn {
  flex: 1;
  height: 88rpx;
  line-height: 88rpx;
  margin: 0;
  padding: 0;
  background: #07c160;
  color: #fff;
  font-size: 32rpx;
  font-weight: 600;
  border-radius: 12rpx;
  border: none;
}

.confirm-btn::after {
  border: none;
}

.loading-wrap,
.empty-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 88rpx 24rpx 40rpx;
  text-align: center;
  box-sizing: border-box;
}

.list {
  padding: 0 24rpx;
}

.list-end {
  padding: 32rpx 0 16rpx;
  text-align: center;
  font-size: 24rpx;
  color: #ccc;
}

.card {
  background: #fff;
  border-radius: 16rpx;
  padding: 28rpx 28rpx 20rpx;
  margin-bottom: 20rpx;
}

.card-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.head-main {
  flex: 1;
  min-width: 0;
}

.customer-name {
  display: block;
  font-size: 34rpx;
  font-weight: 600;
  color: #111;
}

.order-no {
  display: block;
  margin-top: 6rpx;
  font-size: 22rpx;
  color: #999;
  font-family: monospace;
}

.pay-status {
  font-size: 28rpx;
  color: #999;
}

.pay-status.unpaid {
  color: #dc2626;
  font-weight: 600;
}

.pay-status.paid {
  color: #16a34a;
  font-weight: 600;
}

.source-tag {
  display: inline-flex;
  margin-top: 16rpx;
  padding: 4rpx 16rpx;
  border: 1rpx solid #22c55e;
  border-radius: 6rpx;
}

.source-tag text {
  font-size: 22rpx;
  color: #22c55e;
}

.info-line {
  display: flex;
  margin-top: 16rpx;
  font-size: 28rpx;
  line-height: 1.5;
}

.info-label {
  width: 140rpx;
  color: #999;
  flex-shrink: 0;
}

.info-value {
  flex: 1;
  color: #333;
}

.amount-text {
  color: #333;
}

.price-warn {
  color: #f59e0b;
}

.debt-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 12rpx;
  padding: 12rpx 16rpx;
  background: #fff7f0;
  border-radius: 10rpx;
}

.debt-label {
  font-size: 26rpx;
  color: #666;
}

.debt-value {
  font-size: 28rpx;
  font-weight: 600;
  color: #e67e22;
}
</style>
