<template>
  <view class="page">
    <view v-if="loading" class="loading-wrap">
      <u-loading-icon text="加载中" />
    </view>
    <view v-else-if="messages.length === 0" class="empty-wrap">
      <u-empty mode="message" text="暂无消息" />
      <text class="hint">系统更新、订单提醒等通知将显示在这里</text>
    </view>
    <view v-else class="list">
      <view v-for="item in messages" :key="item.id" class="card">
        <text class="title">{{ item.title }}</text>
        <text class="content">{{ item.content }}</text>
        <text class="time">{{ item.time }}</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { onShow } from '@dcloudio/uni-app'
import { ref } from 'vue'
import { useUserStore } from '@common/stores/user'

interface MessageItem {
  id: number
  title: string
  content: string
  time: string
}

const userStore = useUserStore()
const loading = ref(false)
const messages = ref<MessageItem[]>([])

onShow(async () => {
  if (!userStore.isLoggedIn || !userStore.isBoss) {
    uni.reLaunch({ url: '/pages/login/index' })
    return
  }
  loading.value = true
  try {
    messages.value = []
  } finally {
    loading.value = false
  }
})
</script>

<style scoped lang="scss">
.page {
  min-height: 100vh;
  padding: 24rpx;
}

.loading-wrap,
.empty-wrap {
  padding: 120rpx 24rpx;
  text-align: center;
}

.hint {
  display: block;
  margin-top: 16rpx;
  font-size: 24rpx;
  color: #999;
}

.card {
  background: #fff;
  border-radius: 16rpx;
  padding: 28rpx;
  margin-bottom: 16rpx;
}

.title {
  display: block;
  font-size: 30rpx;
  font-weight: 600;
}

.content {
  display: block;
  margin-top: 12rpx;
  font-size: 26rpx;
  color: #666;
}

.time {
  display: block;
  margin-top: 12rpx;
  font-size: 22rpx;
  color: #999;
}
</style>
