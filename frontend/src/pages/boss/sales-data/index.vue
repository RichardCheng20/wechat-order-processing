<template>
  <view class="page">
    <view class="section-card">
      <view class="section-head">
        <text class="section-title">经营概览</text>
        <text class="section-link" @tap="goStats">营收统计 ›</text>
      </view>
      <view class="stats-row">
        <view class="stat-box">
          <text class="stat-label">今日销售</text>
          <text class="stat-value sales">¥ {{ formatMoney(dashboard.todaySales) }}</text>
        </view>
        <view class="stat-divider" />
        <view class="stat-box">
          <text class="stat-label">今日利润</text>
          <text class="stat-value profit">¥ {{ formatMoney(dashboard.todayProfit) }}</text>
        </view>
      </view>
      <text class="stats-hint">累计销售 ¥ {{ formatMoney(totalSales) }}（应收 + 已收）</text>
    </view>

    <view class="section-card">
      <text class="section-title block">资金概况</text>
      <view class="receivable-row">
        <view class="receivable-item receivable" @tap="goReceivable">
          <text class="receivable-label">累计应收</text>
          <text class="receivable-value debt">¥ {{ formatMoney(dashboard.totalReceivable) }}</text>
        </view>
        <view class="receivable-item received" @tap="goReceived">
          <text class="receivable-label">累计已收</text>
          <text class="receivable-value income">¥ {{ formatMoney(dashboard.totalReceived) }}</text>
        </view>
      </view>
      <view class="receivable-row single">
        <view class="receivable-item payable" @tap="goPurchasePayment">
          <text class="receivable-label">累计应付</text>
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
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { fetchBossDashboard, type BossDashboard } from '../../../api/dashboard'
import AppIcon from '../../../components/AppIcon.vue'
import { useUserStore } from '../../../stores/user'

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

onShow(async () => {
  if (!userStore.isLoggedIn || !userStore.isBoss) {
    uni.reLaunch({ url: '/pages/login/index' })
    return
  }
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

.stat-label {
  display: block;
  font-size: 28rpx;
  color: #66736b;
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

.stats-hint {
  display: block;
  margin-top: 16rpx;
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
  display: block;
  font-size: 26rpx;
  color: #66736b;
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
