<template>
  <view class="page">
    <view v-if="loading" class="state-wrap">
      <u-loading-icon text="加载中" />
    </view>

    <view v-else-if="requests.length === 0" class="state-wrap">
      <u-empty mode="list" text="暂无待审核申请" />
    </view>

    <scroll-view v-else scroll-y class="list-scroll" :show-scrollbar="false">
      <view v-for="item in requests" :key="item.id" class="card">
        <view class="card-head">
          <text class="shop-name">{{ item.shopName }}</text>
          <text class="time">{{ formatTime(item.createdAt) }}</text>
        </view>
        <text v-if="item.applicantNickname" class="meta">微信昵称：{{ item.applicantNickname }}</text>
        <text v-if="item.contactName" class="meta">联系人：{{ item.contactName }}</text>
        <text v-if="item.phone" class="meta">电话：{{ item.phone }}</text>
        <text v-if="item.addressShort" class="meta">简写地址：{{ item.addressShort }}</text>
        <text v-if="item.address" class="meta">地址：{{ item.address }}</text>
        <view class="actions">
          <button class="btn reject" :loading="actingId === item.id && actingType === 'reject'" @tap="handleReject(item)">
            拒绝
          </button>
          <button class="btn approve" :loading="actingId === item.id && actingType === 'approve'" @tap="handleApprove(item)">
            同意
          </button>
        </view>
      </view>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { onShow } from '@dcloudio/uni-app'
import { ref } from 'vue'
import {
  approveCustomerBindRequest,
  fetchCustomerBindRequests,
  rejectCustomerBindRequest,
  type CustomerBindRequestItem,
} from '@common/api/customer'
import { useUserStore } from '@common/stores/user'

const userStore = useUserStore()
const loading = ref(false)
const requests = ref<CustomerBindRequestItem[]>([])
const actingId = ref(0)
const actingType = ref<'approve' | 'reject' | ''>('')

onShow(async () => {
  if (!userStore.isLoggedIn || !userStore.isBoss) {
    uni.reLaunch({ url: '/pages/login/index' })
    return
  }
  await loadData()
})

async function loadData() {
  loading.value = true
  try {
    requests.value = await fetchCustomerBindRequests('PENDING') || []
  } catch {
    requests.value = []
  } finally {
    loading.value = false
  }
}

async function handleApprove(item: CustomerBindRequestItem) {
  actingId.value = item.id
  actingType.value = 'approve'
  try {
    await approveCustomerBindRequest(item.id)
    uni.showToast({ title: '已通过', icon: 'success' })
    await loadData()
  } catch (e) {
    uni.showToast({ title: e instanceof Error ? e.message : '操作失败', icon: 'none' })
  } finally {
    actingId.value = 0
    actingType.value = ''
  }
}

function handleReject(item: CustomerBindRequestItem) {
  uni.showModal({
    title: '拒绝申请',
    editable: true,
    placeholderText: '可选：填写拒绝原因',
    success: async (res) => {
      if (!res.confirm) return
      actingId.value = item.id
      actingType.value = 'reject'
      try {
        await rejectCustomerBindRequest(item.id, res.content?.trim() || undefined)
        uni.showToast({ title: '已拒绝', icon: 'none' })
        await loadData()
      } catch (e) {
        uni.showToast({ title: e instanceof Error ? e.message : '操作失败', icon: 'none' })
      } finally {
        actingId.value = 0
        actingType.value = ''
      }
    },
  })
}

function formatTime(value?: string) {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}
</script>

<style scoped lang="scss">
.page {
  min-height: 100vh;
  background: #f5f7f3;
}

.state-wrap {
  padding: 120rpx 0;
  text-align: center;
}

.list-scroll {
  height: 100vh;
  padding: 24rpx;
  box-sizing: border-box;
}

.card {
  background: #fff;
  border-radius: 16rpx;
  padding: 28rpx;
  margin-bottom: 20rpx;
}

.card-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16rpx;
  margin-bottom: 12rpx;
}

.shop-name {
  font-size: 34rpx;
  font-weight: 600;
}

.time {
  font-size: 22rpx;
  color: #999;
  flex-shrink: 0;
}

.meta {
  display: block;
  margin-top: 8rpx;
  font-size: 26rpx;
  color: #666;
}

.actions {
  display: flex;
  gap: 16rpx;
  margin-top: 24rpx;
}

.btn {
  flex: 1;
  margin: 0;
  font-size: 28rpx;
  border-radius: 12rpx;
}

.btn.reject {
  background: #fff;
  color: #666;
  border: 1rpx solid #ddd;
}

.btn.approve {
  background: #07c160;
  color: #fff;
}
</style>
