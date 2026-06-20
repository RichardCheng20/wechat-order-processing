<template>
  <view class="page">
    <view class="search-row">
      <view class="search-wrap">
        <AppIcon class="search-icon" name="search" tone="green" :tile="false" :size="18" />
        <input
          class="search-input"
          type="text"
          :value="keyword"
          placeholder="搜索客户名称"
          confirm-type="search"
          @input="onKeywordInput"
        />
      </view>
    </view>

    <view v-if="!loading" class="summary-bar">
      <text>共 {{ debtCustomers.length }} 位，共欠款：¥ {{ formatMoney(totalDebt) }}</text>
    </view>

    <scroll-view scroll-y class="list-scroll" :show-scrollbar="false">
      <view v-if="loading" class="state-wrap">
        <u-loading-icon text="加载中" />
      </view>
      <view v-else-if="displayCustomers.length === 0" class="state-wrap">
        <u-empty mode="list" text="暂无欠款客户" />
      </view>
      <view v-else class="list">
        <view
          v-for="item in displayCustomers"
          :key="item.id"
          class="row"
          @tap="goCustomerDetail(item.id)"
        >
          <view class="row-main">
            <text class="name">{{ item.name }}</text>
            <view class="debt-tag">
              <text>欠 ¥ {{ formatMoney(item.outstandingAmount) }}</text>
            </view>
          </view>
          <text class="order-date">下单: {{ formatOrderDate(item.lastOrderAt) }}</text>
        </view>
        <view class="list-end">—— 没有更多了 ——</view>
      </view>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { fetchBossCustomers, type CustomerItem } from '../../../api/customer'
import AppIcon from '../../../components/AppIcon.vue'
import { useUserStore } from '../../../stores/user'

const userStore = useUserStore()
const customers = ref<CustomerItem[]>([])
const keyword = ref('')
const loading = ref(false)

const debtCustomers = computed(() =>
  customers.value.filter((item) => (item.outstandingAmount || 0) > 0),
)

const displayCustomers = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  if (!kw) return debtCustomers.value
  return debtCustomers.value.filter((item) => item.name.toLowerCase().includes(kw))
})

const totalDebt = computed(() =>
  debtCustomers.value.reduce((sum, item) => sum + (item.outstandingAmount || 0), 0),
)

onShow(async () => {
  if (!userStore.isLoggedIn || !userStore.isBoss) {
    uni.reLaunch({ url: '/pages/login/index' })
    return
  }
  loading.value = true
  try {
    customers.value = await fetchBossCustomers()
  } catch {
    customers.value = []
  } finally {
    loading.value = false
  }
})

function onKeywordInput(e: { detail: { value: string } }) {
  keyword.value = e.detail.value
}

function formatMoney(value?: number) {
  return Number(value || 0).toFixed(2)
}

function formatOrderDate(value?: string) {
  if (!value) return '-'
  return value.replace('T', ' ').slice(0, 10).replace(/-/g, '.')
}

function goCustomerDetail(id: number) {
  uni.navigateTo({ url: `/pages/boss/customers/detail/index?id=${id}` })
}
</script>

<style scoped lang="scss">
.page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f7f3;
}

.search-row {
  padding: 20rpx 24rpx 0;
  background: #fff;
}

.search-wrap {
  display: flex;
  align-items: center;
  gap: 12rpx;
  padding: 0 24rpx;
  height: 72rpx;
  background: #f5f6f7;
  border-radius: 999rpx;
}

.search-input {
  flex: 1;
  font-size: 28rpx;
}

.summary-bar {
  padding: 20rpx 24rpx;
  background: #fff7e6;
  font-size: 28rpx;
  color: #17211b;
  border-bottom: 1rpx solid #f0e6d8;
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
  align-items: center;
  justify-content: space-between;
  padding: 28rpx 24rpx;
  border-bottom: 1rpx solid #f0f0f0;
}

.row:active {
  background: #fafafa;
}

.row-main {
  flex: 1;
  min-width: 0;
}

.name {
  display: block;
  font-size: 32rpx;
  font-weight: 600;
  color: #17211b;
}

.debt-tag {
  display: inline-block;
  margin-top: 10rpx;
  padding: 4rpx 14rpx;
  background: #fff0ee;
  border-radius: 8rpx;
  font-size: 24rpx;
  color: #e74c3c;
  font-weight: 600;
}

.order-date {
  flex-shrink: 0;
  margin-left: 16rpx;
  font-size: 24rpx;
  color: #999;
}

.list-end {
  padding: 32rpx 0;
  text-align: center;
  font-size: 24rpx;
  color: #ccc;
  background: #fff;
}
</style>
