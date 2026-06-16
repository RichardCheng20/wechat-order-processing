<template>
  <view class="page">
    <view class="banner">
      <view class="banner-top">
        <text class="title">你好，{{ userStore.nickname || '员工' }}</text>
        <text class="logout" @tap="handleLogout">退出</text>
      </view>
      <text v-if="userStore.openid" class="dev-tag">{{ userStore.openid }}</text>
      <text class="hint">按客户逐个分拣，完成后一键标记</text>
    </view>

    <view v-if="loading" class="loading-wrap">
      <u-loading-icon text="加载中" />
    </view>

    <view v-else-if="tasks.length === 0" class="empty-wrap">
      <u-empty mode="data" text="暂无派发任务" />
    </view>

    <view v-else class="list">
      <view
        v-for="item in tasks"
        :key="item.id"
        class="card"
        @tap="goDetail"
        :data-id="item.id"
      >
        <view class="card-top">
          <text class="customer-name">{{ item.customerName }}</text>
          <u-tag :text="item.statusLabel" :type="statusType(item.status)" size="mini" />
        </view>
        <text class="meta">{{ item.deliveryAddressShort || '—' }}</text>
        <text class="meta">{{ item.itemCount || 0 }} 种商品 · {{ item.orderNo }}</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { onShow } from '@dcloudio/uni-app'
import { ref } from 'vue'
import { fetchWorkerTasks, type WorkerTask } from '../../../api/worker'
import { useUserStore } from '../../../stores/user'

const userStore = useUserStore()
const tasks = ref<WorkerTask[]>([])
const loading = ref(false)

onShow(async () => {
  if (!userStore.isLoggedIn || userStore.role !== 'WORKER') {
    uni.reLaunch({ url: '/pages/login/index' })
    return
  }
  loading.value = true
  try {
    tasks.value = await fetchWorkerTasks()
  } catch (e) {
    uni.showToast({ title: e instanceof Error ? e.message : '加载失败', icon: 'none' })
  } finally {
    loading.value = false
  }
})

function goDetail(e: { currentTarget: { dataset: { id?: string | number } } }) {
  const id = e.currentTarget.dataset.id
  uni.navigateTo({ url: `/pages/worker/task-detail/index?id=${id}` })
}

function statusType(status: string) {
  if (status === 'PENDING_PICK') return 'warning'
  if (status === 'PICKING') return 'primary'
  if (status === 'PICKED') return 'success'
  return 'info'
}

function handleLogout() {
  userStore.signOut()
}
</script>

<style scoped lang="scss">
.page {
  padding: 24rpx;
}

.banner {
  background: #fff;
  border-radius: 16rpx;
  padding: 32rpx;
  margin-bottom: 24rpx;
}

.banner-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.title {
  font-size: 34rpx;
  font-weight: 600;
}

.logout {
  font-size: 26rpx;
  color: #999;
}

.hint {
  display: block;
  margin-top: 12rpx;
  font-size: 26rpx;
  color: #888;
}

.dev-tag {
  display: block;
  margin-top: 8rpx;
  font-size: 22rpx;
  color: #999;
}

.loading-wrap,
.empty-wrap {
  padding: 80rpx 0;
  text-align: center;
}

.card {
  background: #fff;
  border-radius: 16rpx;
  padding: 28rpx;
  margin-bottom: 16rpx;
}

.card-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.customer-name {
  font-size: 32rpx;
  font-weight: 600;
}

.meta {
  display: block;
  margin-top: 12rpx;
  font-size: 24rpx;
  color: #666;
}
</style>
