<template>
  <view class="page">
    <view class="search-bar">
      <view class="search-input-wrap">
        <text class="search-icon">🔍</text>
        <input
          v-model="keyword"
          class="search-input"
          placeholder="搜索客户名称"
          confirm-type="search"
          @confirm="refresh"
        />
      </view>
      <view class="filter-btn" @tap="showFilterTip">
        <text class="filter-icon">☰</text>
      </view>
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
        <text class="batch-icon">☰</text>
        <text>批量</text>
      </view>
    </view>

    <view class="date-row">
      <view class="date-range" @tap="showDateTip">
        <text>配送：{{ dateRangeLabel }}</text>
        <text class="arrow">▼</text>
      </view>
      <view class="date-quick">
        <view class="quick-btn" :class="{ active: quickDate === 'today' }" @tap="setToday">今日</view>
        <view class="quick-btn" :class="{ active: quickDate === 'tomorrow' }" @tap="setTomorrow">明日</view>
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
          <text class="customer-name">{{ item.customerName || '未知客户' }}</text>
          <text class="pay-status">{{ item.paymentStatusLabel || '未支付' }}</text>
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

        <view class="card-foot" @tap.stop>
          <text class="print-status">{{ item.printed ? '已打印' : '未打印' }}</text>
          <view class="foot-actions">
            <view class="btn-outline" @tap="handlePrint(item)">打印单据</view>
            <view
              v-if="item.showPickBtn"
              class="btn-primary"
              @tap="goPick(item.id)"
            >
              分拣({{ item.pickedItemCount || 0 }}/{{ item.itemCount || 0 }})
            </view>
          </view>
        </view>
      </view>
    </view>

    <BossTabbar active="orders" />

    <u-modal
      :show="priceModalVisible"
      title="提示"
      content="打印订单中存在价格未录完的商品，是否继续打印？"
      :show-cancel-button="true"
      cancel-text="继续打印"
      confirm-text="返回改价"
      confirm-color="#22c55e"
      @cancel="continuePrint"
      @confirm="goPricingFromModal"
      @close="priceModalVisible = false"
    />
  </view>
</template>

<script setup lang="ts">
import { onShow } from '@dcloudio/uni-app'
import { computed, ref } from 'vue'
import { fetchBossOrders, type OrderInfo } from '../../../api/order'
import BossTabbar from '../../../components/boss-tabbar/index.vue'
import { useUserStore } from '../../../stores/user'

interface OrderDisplay extends OrderInfo {
  createdAtText: string
  deliveryAtText: string
  amountText: string
  showPickBtn: boolean
}

const userStore = useUserStore()
const orders = ref<OrderInfo[]>([])
const loading = ref(false)
const keyword = ref('')
const pickFilter = ref<'ALL' | 'UNPICKED' | 'PICKED'>('ALL')
const quickDate = ref<'week' | 'today' | 'tomorrow'>('week')
const deliveryFrom = ref('')
const deliveryTo = ref('')
const priceModalVisible = ref(false)
const pendingPrintOrderId = ref(0)

const pickTabs = [
  { label: '全部订单', value: 'ALL' as const },
  { label: '未拣完', value: 'UNPICKED' as const },
  { label: '已拣完', value: 'PICKED' as const },
]

const dateRangeLabel = computed(() => {
  if (!deliveryFrom.value || !deliveryTo.value) return '—'
  return `${formatMonthDay(deliveryFrom.value)}至${formatMonthDay(deliveryTo.value)}`
})

const displayOrders = computed<OrderDisplay[]>(() =>
  orders.value.map((item) => ({
    ...item,
    createdAtText: formatDateTime(item.createdAt),
    deliveryAtText: formatDelivery(item.deliveryDate),
    amountText: formatAmount(item.amount),
    showPickBtn: calcShowPickBtn(item),
  })),
)

onShow(async () => {
  if (!userStore.isLoggedIn || !userStore.isBoss) {
    uni.reLaunch({ url: '/pages/login/index' })
    return
  }
  if (!deliveryFrom.value) {
    applyWeekRange(new Date())
  }
  await refresh()
})

async function refresh() {
  loading.value = true
  try {
    orders.value = await fetchBossOrders({
      keyword: keyword.value.trim() || undefined,
      pickFilter: pickFilter.value,
      deliveryFrom: deliveryFrom.value || undefined,
      deliveryTo: deliveryTo.value || undefined,
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

function applyWeekRange(base: Date) {
  const range = getWeekRange(base)
  deliveryFrom.value = range.from
  deliveryTo.value = range.to
  quickDate.value = 'week'
}

function setToday() {
  const today = formatDate(new Date())
  deliveryFrom.value = today
  deliveryTo.value = today
  quickDate.value = 'today'
  refresh()
}

function setTomorrow() {
  const d = new Date()
  d.setDate(d.getDate() + 1)
  const tomorrow = formatDate(d)
  deliveryFrom.value = tomorrow
  deliveryTo.value = tomorrow
  quickDate.value = 'tomorrow'
  refresh()
}

function getWeekRange(base: Date) {
  const d = new Date(base)
  const day = d.getDay() === 0 ? 7 : d.getDay()
  const monday = new Date(d)
  monday.setDate(d.getDate() - day + 1)
  const sunday = new Date(monday)
  sunday.setDate(monday.getDate() + 6)
  return { from: formatDate(monday), to: formatDate(sunday) }
}

function formatDate(date: Date) {
  const y = date.getFullYear()
  const m = `${date.getMonth() + 1}`.padStart(2, '0')
  const d = `${date.getDate()}`.padStart(2, '0')
  return `${y}-${m}-${d}`
}

function formatMonthDay(value: string) {
  const parts = value.split('-')
  if (parts.length < 3) return value
  return `${parts[1]}-${parts[2]}`
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

function calcShowPickBtn(item: OrderInfo) {
  const total = item.itemCount || 0
  if (total === 0) return false
  return !['COMPLETED', 'CANCELLED'].includes(item.status)
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

function showFilterTip() {
  uni.showToast({ title: '筛选功能开发中', icon: 'none' })
}

function showDateTip() {
  uni.showToast({ title: '请使用今日/明日快捷筛选', icon: 'none' })
}
</script>

<style scoped lang="scss">
.page {
  min-height: 100vh;
  background: #f5f6f8;
  padding-bottom: calc(140rpx + env(safe-area-inset-bottom));
}

.search-bar {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 16rpx 24rpx;
  background: #fff;
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
  font-size: 28rpx;
  color: #999;
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
  font-size: 36rpx;
  color: #666;
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
  font-size: 28rpx;
}

.date-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20rpx 24rpx;
  background: #fff;
  margin-bottom: 16rpx;
}

.date-range {
  display: flex;
  align-items: center;
  gap: 8rpx;
  font-size: 28rpx;
  color: #333;
}

.arrow {
  font-size: 20rpx;
  color: #999;
}

.date-quick {
  display: flex;
  gap: 12rpx;
}

.quick-btn {
  padding: 10rpx 24rpx;
  font-size: 26rpx;
  color: #666;
  background: #f5f6f8;
  border-radius: 8rpx;
}

.quick-btn.active {
  color: #22c55e;
  background: #ecfdf3;
}

.loading-wrap,
.empty-wrap {
  padding: 120rpx 0;
  text-align: center;
}

.list {
  padding: 0 24rpx;
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
  align-items: center;
}

.customer-name {
  font-size: 34rpx;
  font-weight: 600;
  color: #111;
}

.pay-status {
  font-size: 28rpx;
  color: #999;
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

.card-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 24rpx;
  padding-top: 20rpx;
  border-top: 1rpx solid #f0f0f0;
}

.print-status {
  font-size: 26rpx;
  color: #999;
}

.foot-actions {
  display: flex;
  gap: 16rpx;
}

.btn-outline,
.btn-primary {
  padding: 12rpx 28rpx;
  font-size: 26rpx;
  border-radius: 8rpx;
}

.btn-outline {
  color: #22c55e;
  border: 1rpx solid #22c55e;
  background: #fff;
}

.btn-primary {
  color: #fff;
  background: #22c55e;
}
</style>
