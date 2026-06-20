<template>
  <view class="page">
    <view v-if="!loading" class="summary-bar">
      <text>累计已收：¥ {{ formatMoney(totalReceived) }}</text>
    </view>

    <scroll-view scroll-y class="list-scroll" :show-scrollbar="false">
      <view v-if="loading" class="state-wrap">
        <u-loading-icon text="加载中" />
      </view>
      <view v-else-if="payments.length === 0" class="state-wrap">
        <u-empty mode="list" text="暂无收款记录" />
      </view>
      <view v-else class="list">
        <view v-for="item in payments" :key="item.id" class="row">
          <view class="row-main">
            <text class="name">{{ item.customerName || '未知客户' }}</text>
            <text class="meta">{{ formatTime(item.paidAt) }} · {{ methodLabel(item.method) }}</text>
            <text v-if="item.remark" class="remark">{{ item.remark }}</text>
          </view>
          <text class="amount">+¥ {{ formatMoney(item.amount) }}</text>
        </view>
        <view class="list-end">—— 没有更多了 ——</view>
      </view>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { fetchBossDashboard } from '../../../api/dashboard'
import { fetchBossPayments, type PaymentInfo } from '../../../api/payment'
import { useUserStore } from '../../../stores/user'

const userStore = useUserStore()
const payments = ref<PaymentInfo[]>([])
const totalReceived = ref(0)
const loading = ref(false)

const METHOD_LABELS: Record<string, string> = {
  WECHAT: '微信',
  CASH: '现金',
  BANK_TRANSFER: '银行转账',
  OTHER: '其他',
}

onShow(async () => {
  if (!userStore.isLoggedIn || !userStore.isBoss) {
    uni.reLaunch({ url: '/pages/login/index' })
    return
  }
  loading.value = true
  try {
    const [dash, list] = await Promise.all([fetchBossDashboard(), fetchBossPayments()])
    totalReceived.value = dash.totalReceived
    payments.value = list
  } catch {
    payments.value = []
  } finally {
    loading.value = false
  }
})

function formatMoney(value?: number) {
  return Number(value || 0).toFixed(2)
}

function formatTime(value?: string) {
  if (!value) return '—'
  return value.replace('T', ' ').slice(0, 16)
}

function methodLabel(method?: string) {
  return METHOD_LABELS[method || ''] || method || '其他'
}
</script>

<style scoped lang="scss">
.page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f7f3;
}

.summary-bar {
  padding: 20rpx 24rpx;
  background: #ecfdf3;
  font-size: 28rpx;
  color: #17211b;
  font-weight: 600;
  border-bottom: 1rpx solid #d1fae5;
}

.list-scroll {
  flex: 1;
  height: 0;
}

.state-wrap {
  padding: 80rpx 0;
  text-align: center;
}

.list {
  background: #fff;
}

.row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16rpx;
  padding: 28rpx 24rpx;
  border-bottom: 1rpx solid #f0f0f0;
}

.row-main {
  flex: 1;
  min-width: 0;
}

.name {
  display: block;
  font-size: 30rpx;
  font-weight: 600;
  color: #17211b;
}

.meta {
  display: block;
  margin-top: 8rpx;
  font-size: 24rpx;
  color: #999;
}

.remark {
  display: block;
  margin-top: 6rpx;
  font-size: 24rpx;
  color: #66736b;
}

.amount {
  flex-shrink: 0;
  font-size: 32rpx;
  font-weight: 700;
  color: #16a34a;
}

.list-end {
  padding: 32rpx 0;
  text-align: center;
  font-size: 24rpx;
  color: #ccc;
  background: #fff;
}
</style>
