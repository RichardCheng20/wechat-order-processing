<template>
  <view class="page">
    <view class="section-card">
      <view class="section-head">
        <text class="section-title">经营概览</text>
        <text class="section-link" @tap="goStats">营收统计 ›</text>
      </view>
      <view class="stats-row">
        <view class="stat-box">
          <view class="label-row">
            <text class="stat-label">今日销售</text>
            <text class="help-btn" @tap.stop="showHelp('todaySales')">?</text>
          </view>
          <text class="stat-value sales">¥ {{ formatMoney(dashboard.todaySales) }}</text>
        </view>
        <view class="stat-divider" />
        <view class="stat-box">
          <view class="label-row">
            <text class="stat-label">今日利润</text>
            <text class="help-btn" @tap.stop="showHelp('todayProfit')">?</text>
          </view>
          <text class="stat-value profit">¥ {{ formatMoney(dashboard.todayProfit) }}</text>
        </view>
      </view>
      <view class="stats-hint-row">
        <text class="stats-hint">累计销售 ¥ {{ formatMoney(totalSales) }}（应收 + 已收）</text>
        <text class="help-btn small" @tap.stop="showHelp('totalSales')">?</text>
      </view>
    </view>

    <view class="section-card">
      <text class="section-title block">资金概况</text>
      <view class="receivable-row">
        <view class="receivable-item receivable" @tap="goReceivable">
          <view class="label-row">
            <text class="receivable-label">累计应收</text>
            <text class="help-btn light" @tap.stop="showHelp('totalReceivable')">?</text>
          </view>
          <text class="receivable-value debt">¥ {{ formatMoney(dashboard.totalReceivable) }}</text>
        </view>
        <view class="receivable-item received" @tap="goReceived">
          <view class="label-row">
            <text class="receivable-label">累计已收</text>
            <text class="help-btn light" @tap.stop="showHelp('totalReceived')">?</text>
          </view>
          <text class="receivable-value income">¥ {{ formatMoney(dashboard.totalReceived) }}</text>
        </view>
      </view>
      <view class="receivable-row single">
        <view class="receivable-item payable" @tap="goPurchasePayment">
          <view class="label-row">
            <text class="receivable-label">累计应付</text>
            <text class="help-btn light" @tap.stop="showHelp('totalPayable')">?</text>
          </view>
          <text class="receivable-value debt">¥ {{ formatMoney(dashboard.totalPayable) }}</text>
        </view>
      </view>
    </view>

    <view class="section-card">
      <text class="section-title block">数据报表</text>
      <view class="grid">
        <view class="grid-item" @tap="goStats">
          <AppIcon class="grid-icon" name="report" tone="green" :size="28" :tile-size="88" :radius="22" />
          <text class="grid-label">营收统计</text>
        </view>
        <view class="grid-item" @tap="goCustomerRanking">
          <AppIcon class="grid-icon" name="ranking" tone="purple" :size="28" :tile-size="88" :radius="22" />
          <text class="grid-label">客户排行</text>
        </view>
        <view class="grid-item" @tap="goProductRanking">
          <AppIcon class="grid-icon" name="product" tone="red" :size="28" :tile-size="88" :radius="22" />
          <text class="grid-label">商品排行</text>
        </view>
      </view>
    </view>

    <u-popup :show="helpVisible" mode="center" round="16" @close="closeHelp">
      <view class="help-panel">
        <text class="help-title">{{ helpTitle }}</text>
        <scroll-view scroll-y class="help-body">
          <text class="help-text">{{ helpContent }}</text>
        </scroll-view>
        <button class="help-ok" @tap="closeHelp">知道了</button>
      </view>
    </u-popup>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { fetchBossDashboard, type BossDashboard } from '@common/api/dashboard'
import AppIcon from '@/components/AppIcon.vue'
import { useUserStore } from '@common/stores/user'
import { guardOwnerAdminPage } from '@common/utils/boss-access'

const userStore = useUserStore()
const dashboard = ref<BossDashboard>({
  todaySales: 0,
  todayProfit: 0,
  todayPurchaseCost: 0,
  totalReceivable: 0,
  totalReceived: 0,
  totalPayable: 0,
})

const totalSales = computed(
  () => (dashboard.value.totalReceivable || 0) + (dashboard.value.totalReceived || 0),
)

type HelpKey = 'todaySales' | 'todayProfit' | 'totalSales' | 'totalReceivable' | 'totalReceived' | 'totalPayable'

const METRIC_HELP: Record<HelpKey, { title: string; content: string }> = {
  todaySales: {
    title: '今日销售',
    content:
      '统计配送日期为今天的订单。\n\n' +
      '计入条件：订单未取消，且已打印配送单或订单状态为已完成，并且有可结算金额。\n\n' +
      '金额取值：优先使用对账后的应收金额，否则使用订单录价金额。\n\n' +
      '将符合条件的订单金额相加，得到今日销售。',
  },
  todayProfit: {
    title: '今日利润',
    content:
      '今日利润 = 今日销售 − 今日采购成本。\n\n' +
      '今日采购成本：取今日销售涉及订单的明细，按实际分拣数量（无则用下单数量）× 当日商品进价 逐行相加。\n\n' +
      '进价优先取当日采购价记录，没有则使用商品默认进价。',
  },
  totalSales: {
    title: '累计销售',
    content:
      '累计销售 = 累计应收 + 累计已收。\n\n' +
      '表示历史上所有有效销售订单的应收总额（含已结清和未结清部分）。',
  },
  totalReceivable: {
    title: '累计应收',
    content:
      '统计所有未取消、且已对账（已打印配送单）或已完成的订单。\n\n' +
      '每笔订单：应收金额（优先对账后金额）− 已收金额 = 未收欠款。\n\n' +
      '将所有订单未收欠款大于 0 的部分相加，得到累计应收。',
  },
  totalReceived: {
    title: '累计已收',
    content:
      '统计所有计入应收的有效订单上，客户已登记收款的累计合计。\n\n' +
      '即历史客户实际到账金额总和。',
  },
  totalPayable: {
    title: '累计应付',
    content:
      '统计所有供应商的未结清采购应付款。\n\n' +
      '每个供应商：应付总额 − 已付总额 = 未付金额。\n\n' +
      '将所有供应商未付金额大于 0 的部分相加，得到累计应付。',
  },
}

const helpVisible = ref(false)
const helpTitle = ref('')
const helpContent = ref('')

function showHelp(key: HelpKey) {
  const item = METRIC_HELP[key]
  helpTitle.value = item.title
  helpContent.value = item.content
  helpVisible.value = true
}

function closeHelp() {
  helpVisible.value = false
}

onShow(async () => {
  if (!(await guardOwnerAdminPage())) return
  try {
    dashboard.value = await fetchBossDashboard()
  } catch {
    // 保留默认值
  }
})

function formatMoney(value?: number) {
  return Number(value || 0).toFixed(2)
}

function goStats() {
  uni.navigateTo({ url: '/pages/boss/stats/index' })
}

function goReceivable() {
  uni.navigateTo({ url: '/pages/boss/receivable/index' })
}

function goReceived() {
  uni.navigateTo({ url: '/pages/boss/received/index' })
}

function goPurchasePayment() {
  uni.navigateTo({ url: '/pages/boss/purchase-payment/index' })
}

function goCustomerRanking() {
  uni.navigateTo({ url: '/pages/boss/ranking/customers/index' })
}

function goProductRanking() {
  uni.navigateTo({ url: '/pages/boss/ranking/products/index' })
}
</script>

<style scoped lang="scss">
.page {
  min-height: 100vh;
  padding: 24rpx 24rpx calc(24rpx + env(safe-area-inset-bottom));
  background: #f5f7f3;
}

.section-card {
  background: #fff;
  border-radius: 16rpx;
  padding: 26rpx 24rpx;
  margin-bottom: 24rpx;
  border: 1rpx solid #dce6df;
}

.section-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24rpx;
}

.section-title {
  font-size: 32rpx;
  font-weight: 700;
  color: #17211b;
}

.section-title.block {
  display: block;
  margin-bottom: 24rpx;
}

.section-link {
  font-size: 26rpx;
  color: #0b7f3a;
}

.stats-row {
  display: flex;
  align-items: center;
}

.stat-box {
  flex: 1;
}

.label-row {
  display: flex;
  align-items: center;
  gap: 8rpx;
}

.stat-label {
  font-size: 28rpx;
  color: #66736b;
}

.help-btn {
  width: 32rpx;
  height: 32rpx;
  line-height: 32rpx;
  text-align: center;
  font-size: 22rpx;
  font-weight: 700;
  color: #66736b;
  background: #eef2ef;
  border-radius: 50%;
}

.help-btn.small {
  width: 28rpx;
  height: 28rpx;
  line-height: 28rpx;
  font-size: 20rpx;
}

.help-btn.light {
  background: rgba(255, 255, 255, 0.75);
}

.stat-value {
  display: block;
  margin-top: 12rpx;
  font-size: 40rpx;
  font-weight: 700;
  color: #17211b;
}

.stat-value.sales {
  color: #0b7f3a;
}

.stats-hint-row {
  display: flex;
  align-items: center;
  gap: 8rpx;
  margin-top: 16rpx;
}

.stats-hint {
  font-size: 24rpx;
  color: #999;
}

.stat-divider {
  width: 1rpx;
  height: 72rpx;
  background: #eee;
  margin: 0 32rpx;
}

.receivable-row {
  display: flex;
  gap: 16rpx;
  margin-bottom: 16rpx;
}

.receivable-row.single {
  margin-bottom: 0;
}

.receivable-row.single .receivable-item {
  flex: 1;
}

.receivable-item {
  flex: 1;
  border-radius: 16rpx;
  padding: 24rpx;
}

.receivable-item:active {
  opacity: 0.9;
}

.receivable-item.receivable {
  background: #fff0ee;
}

.receivable-item.received {
  background: #ecfdf3;
}

.receivable-item.payable {
  background: #fff3df;
}

.receivable-label {
  font-size: 26rpx;
  color: #66736b;
}

.help-panel {
  width: 620rpx;
  max-width: 86vw;
  padding: 32rpx 28rpx 24rpx;
  background: #fff;
  box-sizing: border-box;
}

.help-title {
  display: block;
  font-size: 34rpx;
  font-weight: 700;
  color: #17211b;
  margin-bottom: 20rpx;
}

.help-body {
  max-height: 52vh;
}

.help-text {
  display: block;
  font-size: 28rpx;
  color: #444;
  line-height: 1.65;
  white-space: pre-wrap;
}

.help-ok {
  margin-top: 24rpx;
  height: 80rpx;
  line-height: 80rpx;
  background: #0b7f3a;
  color: #fff;
  font-size: 30rpx;
  font-weight: 600;
  border-radius: 12rpx;
  border: none;
}

.help-ok::after {
  border: none;
}

.receivable-value {
  display: block;
  margin-top: 8rpx;
  font-size: 34rpx;
  font-weight: 700;
}

.receivable-value.debt {
  color: #e74c3c;
}

.receivable-value.income {
  color: #16a34a;
}

.grid {
  display: flex;
  flex-wrap: wrap;
}

.grid-item {
  width: 33.33%;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16rpx 0;
}

.grid-icon {
  width: 88rpx;
  height: 88rpx;
  border-radius: 22rpx;
}

.grid-label {
  margin-top: 10rpx;
  font-size: 26rpx;
  color: #17211b;
  font-weight: 600;
}
</style>
