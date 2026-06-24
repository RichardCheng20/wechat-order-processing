<template>
  <view class="page">
    <view class="top-bar">
      <view class="search-wrap">
        <AppIcon class="search-icon" name="search" tone="green" :tile="false" :size="18" />
        <input
          v-model="keyword"
          class="search-input"
          placeholder="搜索客户名称"
          confirm-type="search"
          @confirm="loadData"
        />
        <text v-if="keyword" class="search-clear" @tap="clearKeyword">×</text>
      </view>
      <text class="hint">为指定客户设置商品定向差异价格</text>
    </view>

    <scroll-view scroll-y class="list-scroll" :show-scrollbar="false">
      <view v-if="loading" class="state-wrap">
        <u-loading-icon text="加载中" />
      </view>
      <view v-else-if="customers.length === 0" class="state-wrap">
        <u-empty mode="list" text="暂无客户档案" />
      </view>
      <view v-else class="list-content">
        <view
          v-for="item in customers"
          :key="item.customerId"
          class="row"
          @tap="goDetail(item.customerId)"
        >
          <view class="row-main">
            <text class="name">{{ item.customerName }}</text>
            <text class="sub">已设 {{ item.quotedProductCount || 0 }} 个商品价</text>
          </view>
          <text class="arrow">›</text>
        </view>
      </view>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { fetchCustomerQuoteSummaries, type CustomerQuoteSummary } from '@common/api/quote'
import AppIcon from '@/components/AppIcon.vue'
import { useUserStore } from '@common/stores/user'

const userStore = useUserStore()
const loading = ref(false)
const keyword = ref('')
const customers = ref<CustomerQuoteSummary[]>([])

onShow(() => {
  if (!userStore.isLoggedIn || !userStore.isBoss) {
    uni.reLaunch({ url: '/packages/common/login/index' })
    return
  }
  loadData()
})

async function loadData() {
  loading.value = true
  try {
    customers.value = await fetchCustomerQuoteSummaries(keyword.value.trim() || undefined)
  } catch (err) {
    customers.value = []
    uni.showToast({ title: err instanceof Error ? err.message : '加载失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

function clearKeyword() {
  keyword.value = ''
  loadData()
}

function goDetail(customerId: number) {
  uni.navigateTo({ url: `/pages/boss/quotes/detail/index?customerId=${customerId}` })
}
</script>

<style scoped lang="scss">
.page {
  min-height: 100vh;
  background: #f5f7f3;
  display: flex;
  flex-direction: column;
}

.top-bar {
  padding: 16rpx 24rpx 20rpx;
  background: #fff;
  border-bottom: 1rpx solid #eee;
}

.search-wrap {
  display: flex;
  align-items: center;
  height: 72rpx;
  padding: 0 20rpx;
  background: #f5f6f8;
  border-radius: 36rpx;
}

.search-icon {
  margin-right: 12rpx;
}

.search-input {
  flex: 1;
  font-size: 28rpx;
}

.search-clear {
  padding: 0 8rpx;
  font-size: 36rpx;
  color: #999;
}

.hint {
  display: block;
  margin-top: 12rpx;
  font-size: 24rpx;
  color: #66736b;
}

.list-scroll {
  flex: 1;
  height: 0;
  min-height: calc(100vh - 180rpx);
}

.state-wrap {
  padding: 120rpx 0;
  text-align: center;
}

.list-content {
  padding: 16rpx 24rpx calc(24rpx + env(safe-area-inset-bottom));
}

.row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 28rpx 24rpx;
  margin-bottom: 16rpx;
  background: #fff;
  border-radius: 16rpx;
  border: 1rpx solid #ececec;
}

.name {
  display: block;
  font-size: 32rpx;
  font-weight: 600;
  color: #17211b;
}

.sub {
  display: block;
  margin-top: 8rpx;
  font-size: 24rpx;
  color: #66736b;
}

.arrow {
  font-size: 36rpx;
  color: #cbd5e1;
}
</style>
