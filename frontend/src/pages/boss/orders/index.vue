<template>
  <view class="page">
    <view class="search-bar">
      <view class="search-input-wrap">
        <picker
          mode="selector"
          :range="searchTypeOptions"
          range-key="label"
          :value="searchTypeIndex"
          @change="onSearchTypeChange"
        >
          <view class="search-type">
            <text class="search-type-text">{{ searchTypeOptions[searchTypeIndex].label }}</text>
            <text class="search-type-arrow">▼</text>
          </view>
        </picker>
        <view class="search-divider" />
        <AppIcon class="search-icon" name="search" tone="green" :tile="false" :size="17" />
        <input
          v-model="keyword"
          class="search-input"
          :placeholder="searchPlaceholder"
          confirm-type="search"
          @confirm="confirmFilter"
        />
      </view>
    </view>

    <view v-if="showSubscribeBar" class="subscribe-bar">
      <text class="subscribe-text">客户下单后可发微信提醒，请先开启</text>
      <view class="subscribe-btn" @tap="handleSubscribeNotify">开启提醒</view>
    </view>

    <view
      v-if="bossAlert.hasPendingConfirm"
      class="pending-alert-bar"
      @tap="goPendingConfirmFilter"
    >
      <view class="pending-alert-dot" />
      <text class="pending-alert-text">有 {{ bossAlert.pendingConfirmCount }} 笔订单待确认</text>
      <text class="pending-alert-action">去处理 ›</text>
    </view>

    <view class="flow-filter-bar">
      <view class="flow-filter-inner">
        <view
          v-for="chip in flowStepFilters"
          :key="String(chip.value)"
          class="flow-chip"
          :class="{ active: flowFilter === chip.value }"
          @tap="switchFlowFilter(chip.value)"
        >
          <text>{{ chip.label }}</text>
          <view
            v-if="chip.value === 0 && bossAlert.hasPendingConfirm"
            class="flow-chip-badge"
          >{{ bossAlert.badgeText }}</view>
        </view>
      </view>
    </view>

    <view class="date-filter-bar">
      <view
        class="date-picker-btn"
        :class="{ active: hasAppliedDateRange && !isQuickActive('today') && !isQuickActive('tomorrow') }"
        @tap="openFilterPopup"
      >
        <AppIcon class="date-cal-icon" name="calendar" tone="gray" :tile="false" :size="15" />
        <text class="date-picker-text">选择时间</text>
        <text v-if="hasAppliedDateRange && !isQuickActive('today') && !isQuickActive('tomorrow')" class="date-picker-range">{{ appliedRangeLabel }}</text>
        <text class="date-arrow">▼</text>
      </view>
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

    <view v-if="loading" class="loading-wrap">
      <u-loading-icon text="加载中" />
    </view>

    <view v-else-if="filteredDisplayOrders.length === 0" class="empty-wrap">
      <u-empty
        mode="order"
        :text="displayOrders.length === 0 ? '暂无订单' : '该步骤暂无订单'"
      />
    </view>

    <view v-else class="list">
      <view
        v-for="item in filteredDisplayOrders"
        :key="item.id"
        class="card"
        @tap="goDetail"
        :data-id="item.id"
      >
        <view class="card-main">
        <view class="card-head">
          <view class="head-main">
            <text class="customer-name">{{ item.customerName || '未知客户' }}</text>
            <text class="order-no">{{ item.orderNo }}</text>
          </view>
          <text class="pay-status" :class="payStatusClass(item)">{{ item.paymentStatusLabel || '待收款' }}</text>
        </view>

        <view class="card-meta">
          <view v-if="item.sourceLabel" class="source-tag">
            <text>{{ item.sourceLabel }}</text>
          </view>
          <text class="order-time">下单时间 {{ item.createdAtText }}</text>
        </view>

        <view class="info-line">
          <text class="info-label">信息</text>
          <text class="info-value">
            {{ item.itemCount || 0 }}种
            <text v-if="item.amountText" class="amount-text">{{ item.amountText }}</text>
            <text v-if="item.priceIncomplete" class="price-warn">(价格未录完)</text>
          </text>
        </view>

        <view v-if="showCustomerPayLine(item)" class="debt-line" :class="{ paid: isPaid(item) }">
          <text class="debt-label">{{ customerPayLineLabel(item) }}</text>
          <text v-if="!isPaid(item)" class="debt-value">¥ {{ formatCustomerDebt(item.customerOutstandingAmount) }}</text>
        </view>

        <OrderFlowBar :steps="item.flowSteps" @tap="(key) => onFlowTap(key, item)" />
        </view>
        <view class="card-arrow-col">
          <text class="card-arrow">›</text>
        </view>
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
import { onHide, onLoad, onShow } from '@dcloudio/uni-app'
import { computed, provide, ref } from 'vue'
import { fetchBossOrders, type BossOrderKeywordType, type OrderInfo } from '@common/api/order'
import { fetchMiniProgramConfig } from '@common/api/config'
import AppIcon from '@/components/AppIcon.vue'
import BossTabbar from '@/components/boss-tabbar/index.vue'
import OrderDateRangePicker from '@/components/OrderDateRangePicker.vue'
import OrderFlowBar from '@/components/OrderFlowBar.vue'
import { useBossAlertStore } from '@common/stores/bossAlert'
import { useUserStore } from '@common/stores/user'
import { useBossOrderAlertOnShow } from '@common/utils/boss-order-alert'
import {
  buildOrderFlowSteps,
  isPaid,
  isPickDone,
  isPriced,
  resolveFlowStageIndex,
  type FlowStepKey,
  type OrderFlowStep,
} from '@common/utils/order-flow'
import { requestOrderNotifySubscribe } from '@common/utils/wechat-subscribe'

interface OrderDisplay extends OrderInfo {
  createdAtText: string
  amountText: string
  flowSteps: OrderFlowStep[]
}

type DateType = 'ORDER' | 'DELIVERY'
type PresetKey = 'today' | 'tomorrow' | 'yesterday' | 'last7' | 'last30' | 'custom' | ''
type FlowFilterValue = 'ALL' | 0 | 1 | 2 | 3 | 4 | 5
type SearchTypeOption = { label: string; value: BossOrderKeywordType }

const userStore = useUserStore()
const bossAlert = useBossAlertStore()
provide('bossAlert', bossAlert)
const { onBossPageShow, onBossPageHide } = useBossOrderAlertOnShow({ poll: true })
const orders = ref<OrderInfo[]>([])
const loading = ref(false)
const keyword = ref('')
const searchTypeIndex = ref(0)
const searchTypeOptions: SearchTypeOption[] = [
  { label: '搜客户', value: 'CUSTOMER' },
  { label: '搜单号', value: 'ORDER_NO' },
]
const flowFilter = ref<FlowFilterValue>('ALL')
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

const flowStepFilters: { label: string; value: FlowFilterValue }[] = [
  { label: '全部', value: 'ALL' },
  { label: '待确认', value: 0 },
  { label: '待拣单', value: 1 },
  { label: '待录价', value: 2 },
  { label: '待对账', value: 3 },
  { label: '待收款', value: 4 },
  { label: '已收款', value: 5 },
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
    amountText: formatAmount(item.amount),
    flowSteps: buildOrderFlowSteps(item),
  })),
)

const filteredDisplayOrders = computed(() => {
  if (flowFilter.value === 'ALL') return displayOrders.value
  return displayOrders.value.filter(
    (item) => resolveFlowStageIndex(item) === flowFilter.value,
  )
})

const searchPlaceholder = computed(() =>
  searchTypeOptions[searchTypeIndex.value]?.value === 'ORDER_NO'
    ? '输入订单号'
    : '输入客户名称',
)

const hasAppliedDateRange = computed(
  () => !!(appliedFrom.value && appliedTo.value),
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
  if (query?.flow === 'confirm') {
    flowFilter.value = 0
  }
})

onShow(async () => {
  if (!userStore.isLoggedIn || !userStore.isBoss) {
    uni.reLaunch({ url: '/packages/common/login/index' })
    return
  }
  try {
    const config = await fetchMiniProgramConfig()
    showSubscribeBar.value = !!config.orderNotifyTemplateId
  } catch {
    showSubscribeBar.value = false
  }
  await onBossPageShow()
  await refresh()
})

onHide(() => {
  onBossPageHide()
})

function goPendingConfirmFilter() {
  switchFlowFilter(0)
}

function handleSubscribeNotify() {
  requestOrderNotifySubscribe()
}

async function refresh() {
  loading.value = true
  try {
    const hasDateFilter = !!(appliedFrom.value && appliedTo.value)
    const searchType = searchTypeOptions[searchTypeIndex.value]?.value || 'CUSTOMER'
    orders.value = await fetchBossOrders({
      keyword: keyword.value.trim() || undefined,
      keywordType: searchType,
      pickFilter: 'ALL',
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

function switchFlowFilter(value: FlowFilterValue) {
  flowFilter.value = value
}

function onSearchTypeChange(e: { detail: { value: string | number } }) {
  const next = Number(e.detail.value)
  if (Number.isNaN(next) || next === searchTypeIndex.value) return
  searchTypeIndex.value = next
  if (keyword.value.trim()) {
    refresh()
  }
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

function formatAmount(amount?: number) {
  if (amount == null) return ''
  return ` ¥${Number(amount).toFixed(2)}`
}

function formatCustomerDebt(amount?: number) {
  return Number(amount || 0).toFixed(2)
}

function showCustomerPayLine(item: OrderInfo) {
  if (!item.printed) return false
  if (isPaid(item)) return true
  return (item.customerOutstandingAmount || 0) > 0
}

function customerPayLineLabel(item: OrderInfo) {
  return isPaid(item) ? '客户已支付' : '客户欠款'
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
    case 'confirmed':
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
    case 'reconcile':
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
  goDetailById(id)
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
  uni.navigateTo({ url: `/pages/boss/orders/print/index?id=${id}&mode=send` })
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

.pending-alert-bar {
  display: flex;
  align-items: center;
  gap: 12rpx;
  padding: 18rpx 24rpx;
  background: #fff1f0;
  border-bottom: 1rpx solid #ffccc7;
}

.pending-alert-dot {
  width: 14rpx;
  height: 14rpx;
  border-radius: 50%;
  background: #e53935;
  flex-shrink: 0;
}

.pending-alert-text {
  flex: 1;
  font-size: 26rpx;
  color: #c62828;
  font-weight: 600;
}

.pending-alert-action {
  font-size: 24rpx;
  color: #e53935;
  font-weight: 600;
}

.search-input-wrap {
  flex: 1;
  display: flex;
  align-items: center;
  height: 72rpx;
  padding: 0 20rpx 0 12rpx;
  background: #f5f6f8;
  border-radius: 36rpx;
}

.search-type {
  display: flex;
  align-items: center;
  gap: 4rpx;
  flex-shrink: 0;
  padding: 0 8rpx 0 4rpx;
}

.search-type-text {
  font-size: 24rpx;
  color: #333;
  font-weight: 500;
}

.search-type-arrow {
  font-size: 16rpx;
  color: #999;
}

.search-divider {
  width: 1rpx;
  height: 32rpx;
  margin: 0 12rpx 0 8rpx;
  background: #ddd;
  flex-shrink: 0;
}

.search-icon {
  margin-right: 10rpx;
  width: 36rpx;
  height: 36rpx;
  border-radius: 10rpx;
  background: #eef2ed;
  flex-shrink: 0;
}

.search-input {
  flex: 1;
  min-width: 0;
  font-size: 26rpx;
}

.flow-filter-bar {
  background: #fff;
  border-bottom: 1rpx solid #f0f0f0;
  padding: 10rpx 12rpx 8rpx;
}

.flow-filter-inner {
  display: flex;
  gap: 6rpx;
}

.flow-chip {
  position: relative;
  flex: 1;
  min-width: 0;
  padding: 8rpx 2rpx;
  font-size: 22rpx;
  color: #666;
  background: #f5f6f8;
  border-radius: 999rpx;
  border: 2rpx solid transparent;
  text-align: center;
  white-space: nowrap;
  display: flex;
  align-items: center;
  justify-content: center;
}

.flow-chip-badge {
  position: absolute;
  top: -10rpx;
  right: -4rpx;
  min-width: 28rpx;
  height: 28rpx;
  padding: 0 6rpx;
  border-radius: 14rpx;
  background: #e53935;
  color: #fff;
  font-size: 18rpx;
  font-weight: 700;
  line-height: 28rpx;
  text-align: center;
  border: 2rpx solid #fff;
}

.flow-chip.active {
  color: #07c160;
  background: #ecfdf3;
  border-color: #07c160;
  font-weight: 600;
}

.date-filter-bar {
  display: flex;
  align-items: center;
  gap: 12rpx;
  padding: 0 24rpx 16rpx;
  background: #fff;
  border-bottom: 1rpx solid #eee;
}

.date-picker-btn {
  display: flex;
  align-items: center;
  gap: 6rpx;
  flex-shrink: 0;
  padding: 8rpx 20rpx;
  background: #f5f6f8;
  border-radius: 999rpx;
  border: 2rpx solid transparent;
}

.date-picker-btn.active {
  color: #07c160;
  background: #ecfdf3;
  border-color: #07c160;
}

.date-cal-icon {
  flex-shrink: 0;
}

.date-picker-text {
  font-size: 26rpx;
  color: #333;
}

.date-picker-btn.active .date-picker-text {
  color: #07c160;
  font-weight: 600;
}

.date-picker-range {
  max-width: 160rpx;
  font-size: 24rpx;
  color: #07c160;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.date-arrow {
  margin-left: 2rpx;
  font-size: 18rpx;
  color: #999;
  flex-shrink: 0;
}

.quick-day {
  flex-shrink: 0;
  padding: 8rpx 24rpx;
  font-size: 24rpx;
  color: #666;
  background: #f5f6f8;
  border-radius: 999rpx;
  border: 2rpx solid transparent;
}

.quick-day.active {
  color: #07c160;
  background: #ecfdf3;
  border-color: #07c160;
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
  display: flex;
  align-items: stretch;
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx 16rpx 16rpx 24rpx;
  margin-bottom: 20rpx;
}

.card-main {
  flex: 1;
  min-width: 0;
}

.card-arrow-col {
  display: flex;
  align-items: center;
  flex-shrink: 0;
  padding-left: 8rpx;
}

.card-arrow {
  font-size: 48rpx;
  line-height: 1;
  color: #22c55e;
  font-weight: 300;
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
  padding: 4rpx 12rpx;
  border: 1rpx solid #22c55e;
  border-radius: 6rpx;
  flex-shrink: 0;
}

.source-tag text {
  font-size: 22rpx;
  color: #22c55e;
}

.card-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12rpx;
  margin-top: 12rpx;
}

.order-time {
  font-size: 24rpx;
  color: #666;
}

.info-line {
  display: flex;
  margin-top: 12rpx;
  font-size: 26rpx;
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

.debt-line.paid {
  background: #f0faf4;
}

.debt-label {
  font-size: 26rpx;
  color: #666;
}

.debt-line.paid .debt-label {
  color: #07c160;
  font-weight: 600;
}

.debt-value {
  font-size: 28rpx;
  font-weight: 600;
  color: #e67e22;
}
</style>
