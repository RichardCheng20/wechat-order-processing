<template>
  <view class="page">
    <view v-if="loading" class="state-wrap">
      <u-loading-icon text="加载中" />
    </view>

    <view v-else-if="payments.length === 0" class="state-wrap">
      <u-empty mode="list" text="暂无收款记录" />
    </view>

    <scroll-view v-else scroll-y class="list-scroll" :show-scrollbar="false">
      <view class="list">
        <view v-for="item in payments" :key="item.id" class="card">
          <view class="card-head">
            <text class="amount">¥ {{ formatMoney(item.amount) }}</text>
            <text class="method">{{ methodLabel(item.method) }}</text>
          </view>
          <text class="time">{{ formatDateTime(item.paidAt) }}</text>
          <text v-if="item.remark" class="remark">{{ item.remark }}</text>
          <view v-if="item.voucherUrls?.length" class="vouchers">
            <image
              v-for="(url, idx) in item.voucherUrls"
              :key="idx"
              :src="url"
              mode="aspectFill"
              class="voucher-img"
              @tap="previewVoucher(item.voucherUrls!, idx)"
            />
          </view>
        </view>
        <view class="list-end">没有更多了</view>
      </view>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { fetchBossPayments, type PaymentInfo } from '../../../../api/payment'
import { useUserStore } from '../../../../stores/user'

const userStore = useUserStore()
const customerId = ref(0)
const loading = ref(false)
const payments = ref<PaymentInfo[]>([])

onLoad((query) => {
  customerId.value = Number(query?.customerId || 0)
})

onShow(async () => {
  if (!userStore.isLoggedIn || !userStore.isBoss) {
    uni.reLaunch({ url: '/pages/login/index' })
    return
  }
  await loadPayments()
})

async function loadPayments() {
  if (!customerId.value) return
  loading.value = true
  try {
    payments.value = await fetchBossPayments(customerId.value)
  } catch (e) {
    payments.value = []
    uni.showToast({ title: e instanceof Error ? e.message : '加载失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

function formatMoney(value?: number) {
  return Number(value || 0).toFixed(2)
}

function formatDateTime(value?: string) {
  if (!value) return '-'
  const d = new Date(value.replace(' ', 'T'))
  if (Number.isNaN(d.getTime())) return value
  const y = d.getFullYear()
  const m = `${d.getMonth() + 1}`.padStart(2, '0')
  const day = `${d.getDate()}`.padStart(2, '0')
  const h = `${d.getHours()}`.padStart(2, '0')
  const min = `${d.getMinutes()}`.padStart(2, '0')
  return `${y}.${m}.${day} ${h}:${min}`
}

function methodLabel(method?: string) {
  const map: Record<string, string> = {
    CASH: '现金',
    WECHAT: '微信',
    BANK_TRANSFER: '银行转账',
    OTHER: '其他',
  }
  return method ? (map[method] || method) : '其他'
}

function previewVoucher(urls: string[], index: number) {
  uni.previewImage({ urls, current: urls[index] })
}
</script>

<style scoped lang="scss">
.page {
  min-height: 100vh;
  background: #f5f6f7;
}

.state-wrap {
  padding: 160rpx 0;
}

.list-scroll {
  height: 100vh;
}

.list {
  padding: 16rpx 24rpx;
}

.card {
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 16rpx;
}

.card-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8rpx;
}

.amount {
  font-size: 36rpx;
  font-weight: 700;
  color: #2ecc71;
}

.method {
  font-size: 24rpx;
  color: #999;
  padding: 4rpx 16rpx;
  background: #f5f5f5;
  border-radius: 20rpx;
}

.time {
  display: block;
  font-size: 24rpx;
  color: #666;
  margin-bottom: 8rpx;
}

.remark {
  display: block;
  font-size: 26rpx;
  color: #333;
  margin-bottom: 12rpx;
}

.vouchers {
  display: flex;
  gap: 12rpx;
  flex-wrap: wrap;
}

.voucher-img {
  width: 120rpx;
  height: 120rpx;
  border-radius: 8rpx;
  background: #f0f0f0;
}

.list-end {
  text-align: center;
  color: #ccc;
  font-size: 24rpx;
  padding: 24rpx 0 40rpx;
}
</style>
