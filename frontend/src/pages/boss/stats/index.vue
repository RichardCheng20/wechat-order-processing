<template>
  <view class="page">
    <view class="main-tabs">
      <view
        v-for="tab in mainTabs"
        :key="tab.key"
        class="main-tab"
        :class="{ active: activeTab === tab.key }"
        @tap="switchTab(tab.key)"
      >
        {{ tab.label }}
      </view>
    </view>

    <view class="filter-bar">
      <view class="quick-tabs">
        <text
          class="quick-tab"
          :class="{ active: presetDays === 7 }"
          @tap="applyPreset(7)"
        >近7日</text>
        <text
          class="quick-tab"
          :class="{ active: presetDays === 30 }"
          @tap="applyPreset(30)"
        >近30日</text>
      </view>
      <view class="calendar-btn" @tap="openRangePicker">
        <AppIcon name="order" tone="gray" :size="18" :tile="false" />
      </view>
    </view>

    <view v-if="loading" class="state-wrap">
      <u-loading-icon text="加载中" />
    </view>

    <scroll-view v-else scroll-y class="body-scroll" :show-scrollbar="false">
      <template v-if="activeTab === 'summary'">
        <view class="card-row">
          <view class="metric-card sales">
            <text class="metric-label">销售总额</text>
            <text class="metric-value">¥ {{ formatMoney(revenueStats?.totalSales) }}</text>
          </view>
          <view class="metric-card profit">
            <text class="metric-label">总利润</text>
            <text class="metric-value">¥ {{ formatMoney(revenueStats?.totalProfit) }}</text>
          </view>
        </view>

        <view class="chart-card">
          <view class="chart-wrap">
            <view class="chart-grid">
              <view v-for="(tick, idx) in yTicks" :key="idx" class="grid-line">
                <text class="y-label">{{ tick }}</text>
              </view>
            </view>
            <view class="chart-bars">
              <view
                v-for="row in chartRows"
                :key="row.date"
                class="bar-col"
              >
                <view class="bar-stack">
                  <view
                    class="bar-fill"
                    :style="{ height: `${barHeight(row.salesAmount)}%` }"
                  />
                </view>
                <text v-if="row.showLabel" class="x-label">{{ formatChartDate(row.date) }}</text>
              </view>
            </view>
          </view>
        </view>

        <view class="table-card">
          <view class="table-head">
            <text class="col-date">下单日期</text>
            <text class="col-sales">销售额(元)</text>
            <text class="col-cost">参考成本</text>
          </view>
          <view
            v-for="row in tableRows"
            :key="row.date"
            class="table-row"
          >
            <text class="col-date">{{ formatTableDate(row.date) }}</text>
            <text class="col-sales">{{ formatMoney(row.salesAmount) }}</text>
            <text class="col-cost">{{ formatMoney(row.purchaseCost) }}</text>
          </view>
          <view v-if="tableRows.length === 0" class="empty-tip">所选区间暂无销售数据</view>
        </view>
      </template>

      <template v-else-if="activeTab === 'payments'">
        <view class="card-row single">
          <view class="metric-card received">
            <text class="metric-label">收款总额</text>
            <text class="metric-value">¥ {{ formatMoney(paymentStats?.totalReceived) }}</text>
          </view>
        </view>

        <view class="chart-card">
          <view class="chart-wrap payment">
            <view class="chart-bars">
              <view
                v-for="row in paymentChartRows"
                :key="row.date"
                class="bar-col"
              >
                <view class="bar-stack">
                  <view
                    class="bar-fill payment"
                    :style="{ height: `${paymentBarHeight(row.amount)}%` }"
                  />
                </view>
                <text v-if="row.showLabel" class="x-label">{{ formatChartDate(row.date) }}</text>
              </view>
            </view>
          </view>
        </view>

        <view class="table-card">
          <view class="table-head payment">
            <text class="col-date">收款日期</text>
            <text class="col-sales">收款金额(元)</text>
          </view>
          <view
            v-for="row in paymentTableRows"
            :key="row.date"
            class="table-row payment"
          >
            <text class="col-date">{{ formatTableDate(row.date) }}</text>
            <text class="col-sales green">+{{ formatMoney(row.amount) }}</text>
          </view>
          <view v-if="paymentTableRows.length === 0" class="empty-tip">所选区间暂无收款记录</view>
        </view>
      </template>
    </scroll-view>

    <OrderDateRangePicker
      :show="rangePickerVisible"
      :start-date="dateFrom"
      :end-date="dateTo"
      @close="rangePickerVisible = false"
      @confirm="onRangeConfirm"
    />
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import {
  fetchBossPaymentStats,
  fetchBossRevenueStats,
  type BossPaymentStats,
  type BossRevenueStats,
} from '../../../api/dashboard'
import AppIcon from '../../../components/AppIcon.vue'
import OrderDateRangePicker from '../../../components/OrderDateRangePicker.vue'
import { getLastNDaysRange, formatShortMonthDay } from '../../../utils/date-range'
import { useUserStore } from '../../../stores/user'

type MainTab = 'summary' | 'payments'

const userStore = useUserStore()
const activeTab = ref<MainTab>('summary')
const loading = ref(false)
const presetDays = ref(7)
const dateFrom = ref('')
const dateTo = ref('')
const rangePickerVisible = ref(false)
const revenueStats = ref<BossRevenueStats | null>(null)
const paymentStats = ref<BossPaymentStats | null>(null)

const mainTabs = [
  { key: 'summary' as const, label: '汇总统计' },
  { key: 'payments' as const, label: '收款统计' },
]

const chartRows = computed(() => {
  const rows = revenueStats.value?.rows || []
  const step = rows.length > 14 ? Math.ceil(rows.length / 7) : 1
  return rows.map((row, idx) => ({
    ...row,
    showLabel: idx % step === 0 || idx === rows.length - 1,
  }))
})

const tableRows = computed(() =>
  [...(revenueStats.value?.rows || [])]
    .filter((row) => Number(row.salesAmount || 0) > 0)
    .sort((a, b) => b.date.localeCompare(a.date)),
)

const paymentChartRows = computed(() => {
  const rows = paymentStats.value?.rows || []
  const step = rows.length > 14 ? Math.ceil(rows.length / 7) : 1
  return rows.map((row, idx) => ({
    ...row,
    showLabel: idx % step === 0 || idx === rows.length - 1,
  }))
})

const paymentTableRows = computed(() =>
  [...(paymentStats.value?.rows || [])]
    .filter((row) => Number(row.amount || 0) > 0)
    .sort((a, b) => b.date.localeCompare(a.date)),
)

const maxSales = computed(() => {
  const values = (revenueStats.value?.rows || []).map((r) => Number(r.salesAmount || 0))
  return Math.max(...values, 1)
})

const maxPayment = computed(() => {
  const values = (paymentStats.value?.rows || []).map((r) => Number(r.amount || 0))
  return Math.max(...values, 1)
})

const yTicks = computed(() => {
  const max = maxSales.value
  const step = max <= 100 ? 25 : max <= 500 ? 100 : Math.ceil(max / 4 / 100) * 100
  return [step * 4, step * 3, step * 2, step, 0].map((v) => (v >= 1000 ? `${v / 1000}k` : String(v)))
})

onShow(async () => {
  if (!userStore.isLoggedIn || !userStore.isBoss) {
    uni.reLaunch({ url: '/pages/login/index' })
    return
  }
  if (!dateFrom.value) {
    applyPreset(7)
    return
  }
  await loadData()
})

function switchTab(key: MainTab) {
  if (activeTab.value === key) return
  activeTab.value = key
  loadData()
}

function applyPreset(days: number) {
  presetDays.value = days
  const range = getLastNDaysRange(days)
  dateFrom.value = range.from
  dateTo.value = range.to
  loadData()
}

function openRangePicker() {
  rangePickerVisible.value = true
}

function onRangeConfirm(payload: { from: string; to: string }) {
  rangePickerVisible.value = false
  dateFrom.value = payload.from
  dateTo.value = payload.to
  presetDays.value = 0
  loadData()
}

async function loadData() {
  if (!dateFrom.value || !dateTo.value) return
  loading.value = true
  try {
    if (activeTab.value === 'payments') {
      paymentStats.value = await fetchBossPaymentStats({
        dateFrom: dateFrom.value,
        dateTo: dateTo.value,
      })
    } else {
      revenueStats.value = await fetchBossRevenueStats({
        dateFrom: dateFrom.value,
        dateTo: dateTo.value,
        dateType: 'DELIVERY',
      })
    }
  } catch (e) {
    uni.showToast({ title: e instanceof Error ? e.message : '加载失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

function barHeight(value?: number) {
  const num = Number(value || 0)
  if (num <= 0) return 0
  return Math.max(4, Math.round((num / maxSales.value) * 100))
}

function paymentBarHeight(value?: number) {
  const num = Number(value || 0)
  if (num <= 0) return 0
  return Math.max(4, Math.round((num / maxPayment.value) * 100))
}

function formatMoney(value?: number) {
  return Number(value || 0).toFixed(2)
}

function formatChartDate(value: string) {
  return formatShortMonthDay(value)
}

function formatTableDate(value: string) {
  return formatShortMonthDay(value)
}
</script>

<style scoped lang="scss">
.page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f7f3;
  overflow: hidden;
}

.main-tabs {
  display: flex;
  background: #fff;
  border-bottom: 1rpx solid #eee;
  flex-shrink: 0;
}

.main-tab {
  flex: 1;
  text-align: center;
  padding: 24rpx 8rpx;
  font-size: 26rpx;
  color: #66736b;
  white-space: nowrap;
}

.main-tab.active {
  color: #0b7f3a;
  font-weight: 700;
  box-shadow: inset 0 -4rpx 0 #0b7f3a;
}

.filter-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16rpx 24rpx;
  background: #fff;
  border-bottom: 1rpx solid #f0f0f0;
  flex-shrink: 0;
}

.quick-tabs {
  display: flex;
  gap: 32rpx;
}

.quick-tab {
  font-size: 28rpx;
  color: #66736b;
  padding-bottom: 8rpx;
}

.quick-tab.active {
  color: #0b7f3a;
  font-weight: 700;
  box-shadow: inset 0 -4rpx 0 #0b7f3a;
}

.calendar-btn {
  width: 64rpx;
  height: 64rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f6f7;
  border-radius: 12rpx;
}

.body-scroll {
  flex: 1;
  height: 0;
  padding: 20rpx 24rpx calc(24rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
}

.state-wrap {
  padding: 80rpx 0;
  text-align: center;
}

.card-row {
  display: flex;
  gap: 16rpx;
  margin-bottom: 20rpx;
}

.card-row.single .metric-card {
  flex: 1;
}

.metric-card {
  flex: 1;
  border-radius: 16rpx;
  padding: 28rpx 24rpx;
  min-height: 140rpx;
  box-sizing: border-box;
}

.metric-card.sales {
  background: linear-gradient(135deg, #dbeafe 0%, #eff6ff 100%);
}

.metric-card.profit {
  background: linear-gradient(135deg, #ffedd5 0%, #fff7ed 100%);
}

.metric-card.received {
  background: linear-gradient(135deg, #dcfce7 0%, #f0fdf4 100%);
}

.metric-label {
  display: block;
  font-size: 26rpx;
  color: #66736b;
}

.metric-value {
  display: block;
  margin-top: 16rpx;
  font-size: 44rpx;
  font-weight: 700;
  color: #17211b;
}

.chart-card {
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx 16rpx 16rpx;
  margin-bottom: 20rpx;
  border: 1rpx solid #ececec;
}

.chart-wrap {
  position: relative;
  height: 320rpx;
}

.chart-wrap.payment {
  height: 280rpx;
}

.chart-grid {
  position: absolute;
  inset: 0 0 40rpx 48rpx;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  pointer-events: none;
}

.grid-line {
  border-top: 1rpx dashed #eee;
  position: relative;
}

.y-label {
  position: absolute;
  left: -48rpx;
  top: -14rpx;
  width: 44rpx;
  text-align: right;
  font-size: 20rpx;
  color: #ccc;
}

.chart-bars {
  position: absolute;
  left: 48rpx;
  right: 0;
  bottom: 0;
  top: 0;
  display: flex;
  align-items: flex-end;
  gap: 4rpx;
  padding-bottom: 40rpx;
}

.bar-col {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  height: 100%;
  justify-content: flex-end;
}

.bar-stack {
  flex: 1;
  width: 70%;
  max-width: 36rpx;
  display: flex;
  align-items: flex-end;
  margin-bottom: 8rpx;
}

.bar-fill {
  width: 100%;
  border-radius: 6rpx 6rpx 0 0;
  background: linear-gradient(180deg, #fdba74 0%, #fed7aa 100%);
  min-height: 0;
}

.bar-fill.payment {
  background: linear-gradient(180deg, #4ade80 0%, #bbf7d0 100%);
}

.x-label {
  font-size: 20rpx;
  color: #999;
  white-space: nowrap;
}

.table-card {
  background: #fff;
  border-radius: 16rpx;
  overflow: hidden;
  border: 1rpx solid #ececec;
}

.table-head,
.table-row {
  display: flex;
  align-items: center;
  padding: 24rpx;
  font-size: 26rpx;
}

.table-head {
  background: #fafafa;
  color: #66736b;
  font-weight: 600;
}

.table-row {
  border-top: 1rpx solid #f3f3f3;
  color: #17211b;
}

.table-head.payment,
.table-row.payment {
  .col-sales {
    flex: 1;
    text-align: right;
  }
}

.col-date {
  width: 160rpx;
  flex-shrink: 0;
}

.col-sales {
  flex: 1;
  text-align: center;
}

.col-cost {
  width: 160rpx;
  text-align: right;
  flex-shrink: 0;
}

.col-sales.green {
  color: #16a34a;
  font-weight: 600;
}

.empty-tip {
  padding: 48rpx 24rpx;
  text-align: center;
  font-size: 26rpx;
  color: #999;
}
</style>
