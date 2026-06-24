<template>
  <view class="page">
    <view class="banner">
      <view class="title-wrap">
        <text class="title">你好，{{ userStore.nickname || '配送员' }}</text>
        <text v-if="userStore.workerCode" class="worker-code">ID {{ userStore.workerCode }}</text>
      </view>
      <text class="hint">老板已确认的待拣单，装货完成后请标记已拣单</text>
    </view>

    <view class="date-bar">
      <view class="quick-tabs">
        <text class="quick-tab" :class="{ active: presetDays === 1 }" @tap="applyPreset(1)">今天</text>
        <text class="quick-tab" :class="{ active: presetDays === 7 }" @tap="applyPreset(7)">近7日</text>
        <text class="quick-tab" :class="{ active: presetDays === 30 }" @tap="applyPreset(30)">近30日</text>
      </view>
      <view class="calendar-btn" @tap="openRangePicker">
        <AppIcon name="calendar" tone="gray" :size="16" :tile="false" />
        <text v-if="rangeLabel" class="range-label">{{ rangeLabel }}</text>
      </view>
    </view>

    <view v-if="loading" class="loading-wrap">
      <u-loading-icon text="加载中" />
    </view>

    <view v-else-if="tasks.length === 0" class="empty-wrap">
      <u-empty mode="data" text="所选日期内暂无待拣单" />
    </view>

    <view v-else class="list">
      <view
        v-for="item in tasks"
        :key="item.id"
        class="card"
        @tap="goDetail"
        :data-id="item.id"
      >
        <view class="card-main">
          <view class="card-top">
            <text class="customer-name">{{ item.customerName }}</text>
            <u-tag :text="item.statusLabel" :type="statusType(item.status)" size="mini" />
          </view>
          <text class="meta">{{ item.deliveryAddressShort || '—' }}</text>
          <text class="meta">{{ item.itemCount || 0 }} 种商品 · {{ item.orderNo }}</text>
        </view>
        <text class="chevron">›</text>
      </view>
    </view>

    <WorkerTabBar active="pending" />

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
import { onShow } from '@dcloudio/uni-app'
import { computed, ref } from 'vue'
import { fetchWorkerTasks, type WorkerTask } from '@common/api/worker'
import AppIcon from '@/components/AppIcon.vue'
import OrderDateRangePicker from '@/components/OrderDateRangePicker.vue'
import WorkerTabBar from '@/components/WorkerTabBar.vue'
import { formatShortDate, getLastNDaysRange, getTodayRange } from '@common/utils/date-range'
import { useUserStore } from '@common/stores/user'

const userStore = useUserStore()
const tasks = ref<WorkerTask[]>([])
const loading = ref(false)
const dateFrom = ref('')
const dateTo = ref('')
const presetDays = ref(1)
const rangePickerVisible = ref(false)

const rangeLabel = computed(() => {
  if (!dateFrom.value || presetDays.value > 0) return ''
  const from = formatShortDate(dateFrom.value)
  const to = formatShortDate(dateTo.value)
  return from === to ? from : `${from}~${to}`
})

onShow(async () => {
  if (!userStore.isLoggedIn || userStore.role !== 'WORKER') {
    uni.reLaunch({ url: '/packages/common/login/index' })
    return
  }
  if (!dateFrom.value) {
    applyPreset(1, false)
  }
  await loadTasks()
})

function applyPreset(days: number, reload = true) {
  presetDays.value = days
  const range = days === 1 ? getTodayRange() : getLastNDaysRange(days)
  dateFrom.value = range.from
  dateTo.value = range.to
  if (reload) {
    loadTasks()
  }
}

function openRangePicker() {
  rangePickerVisible.value = true
}

function onRangeConfirm(payload: { from: string; to: string }) {
  rangePickerVisible.value = false
  dateFrom.value = payload.from
  dateTo.value = payload.to
  presetDays.value = 0
  loadTasks()
}

async function loadTasks() {
  if (!dateFrom.value || !dateTo.value) return
  loading.value = true
  try {
    tasks.value = await fetchWorkerTasks({
      dateFrom: dateFrom.value,
      dateTo: dateTo.value,
    })
  } catch (e) {
    uni.showToast({ title: e instanceof Error ? e.message : '加载失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

function goDetail(e: { currentTarget: { dataset: { id?: string | number } } }) {
  const id = e.currentTarget.dataset.id
  uni.navigateTo({ url: `/pages/worker/task-detail/index?id=${id}` })
}

function statusType(status: string) {
  if (status === 'PENDING_PICK') return 'warning'
  if (status === 'PICKING') return 'primary'
  return 'info'
}
</script>

<style scoped lang="scss">
.page {
  min-height: 100vh;
  padding: 24rpx;
  padding-bottom: calc(120rpx + env(safe-area-inset-bottom));
  background: #f5f7f3;
  box-sizing: border-box;
}

.banner {
  background: #fff;
  border-radius: 16rpx;
  padding: 32rpx;
  margin-bottom: 24rpx;
}

.title-wrap {
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}

.title {
  font-size: 34rpx;
  font-weight: 600;
}

.worker-code {
  font-size: 26rpx;
  font-weight: 600;
  color: #07c160;
}

.hint {
  display: block;
  margin-top: 12rpx;
  font-size: 26rpx;
  color: #888;
}

.date-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12rpx;
  margin-bottom: 20rpx;
}

.quick-tabs {
  display: flex;
  gap: 8rpx;
  flex: 1;
}

.quick-tab {
  padding: 10rpx 20rpx;
  font-size: 24rpx;
  color: #66736b;
  background: #fff;
  border-radius: 999rpx;
}

.quick-tab.active {
  color: #07c160;
  background: #e8f8ef;
  font-weight: 600;
}

.calendar-btn {
  display: flex;
  align-items: center;
  gap: 8rpx;
  padding: 10rpx 16rpx;
  background: #fff;
  border-radius: 999rpx;
  flex-shrink: 0;
}

.range-label {
  font-size: 22rpx;
  color: #66736b;
  max-width: 160rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.loading-wrap,
.empty-wrap {
  padding: 80rpx 0;
  text-align: center;
}

.card {
  display: flex;
  align-items: center;
  background: #fff;
  border-radius: 16rpx;
  padding: 28rpx 20rpx 28rpx 28rpx;
  margin-bottom: 16rpx;
}

.card-main {
  flex: 1;
  min-width: 0;
}

.card-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16rpx;
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

.chevron {
  flex-shrink: 0;
  margin-left: 12rpx;
  font-size: 44rpx;
  line-height: 1;
  color: #c8c8c8;
  font-weight: 300;
}
</style>
