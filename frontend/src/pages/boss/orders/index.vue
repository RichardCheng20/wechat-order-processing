<template>
  <view class="page">
    <view class="toolbar">
      <view class="toolbar-left">
        <u-button size="small" text="商品管理" type="primary" plain @click="goProducts" />
        <u-button size="small" text="客户管理" type="primary" plain @click="goCustomers" />
        <u-button size="small" text="录价" type="warning" plain @click="goPricing" />
      </view>
      <text class="logout" @click="handleLogout">退出</text>
    </view>

    <view class="stats">
      <view class="stat-item">
        <text class="stat-value">{{ summary.todayTotal }}</text>
        <text class="stat-label">今日订单</text>
      </view>
      <view class="stat-item">
        <text class="stat-value">{{ summary.pendingConfirm }}</text>
        <text class="stat-label">待确认</text>
      </view>
      <view class="stat-item">
        <text class="stat-value">{{ summary.pendingPick }}</text>
        <text class="stat-label">待分拣</text>
      </view>
      <view class="stat-item">
        <text class="stat-value warn">{{ summary.pendingPrice }}</text>
        <text class="stat-label">待录价</text>
      </view>
    </view>

    <scroll-view scroll-x class="tabs">
      <view
        v-for="tab in tabs"
        :key="tab.value"
        class="tab-item"
        :class="{ active: activeTab === tab.value }"
        :data-value="tab.value"
        @tap="onTabTap"
      >
        {{ tab.label }}
      </view>
    </scroll-view>

    <view v-if="loading" class="loading-wrap">
      <u-loading-icon text="加载中" />
    </view>

    <view v-else-if="orders.length === 0" class="empty-wrap">
      <u-empty mode="order" text="暂无订单" />
    </view>

    <view v-else class="list">
      <view v-for="item in orders" :key="item.id" class="card">
        <view class="card-top">
          <view>
            <text class="customer-name">{{ item.customerName }}</text>
            <text class="order-no">{{ item.orderNo }}</text>
          </view>
          <u-tag :text="item.statusLabel" :type="statusType(item.status)" size="mini" />
        </view>
        <text class="meta">{{ item.deliveryAddressShort || '—' }} · {{ item.itemCount || 0 }} 种</text>
        <text v-if="item.assignedWorkerName" class="meta">工人：{{ item.assignedWorkerName }}</text>
        <text class="meta">{{ formatTime(item.createdAt) }}</text>
        <view class="actions">
          <u-button
            v-if="item.status === 'PENDING_CONFIRM'"
            size="small"
            type="primary"
            text="确认订单"
            @click="handleConfirm(item.id)"
          />
          <u-button
            v-if="item.status === 'PENDING_PICK' || item.status === 'PICKING'"
            size="small"
            type="primary"
            plain
            :text="item.assignedWorkerName ? '改派工人' : '派单'"
            @click="openAssign(item.id)"
          />
          <u-button
            v-if="item.status === 'PENDING_PRICE'"
            size="small"
            type="warning"
            text="去录价"
            @click="goPricingDetail(item.id)"
          />
        </view>
      </view>
    </view>

    <u-popup :show="showAssign" mode="bottom" round="16" @close="showAssign = false">
      <view class="assign-popup">
        <text class="assign-title">选择工人</text>
        <view v-if="workers.length === 0" class="assign-empty">暂无工人，请先用工人账号登录一次</view>
        <view
          v-for="worker in workers"
          :key="worker.id"
          class="worker-item"
          @tap="handleAssign"
          :data-id="worker.id"
        >
          <text>{{ worker.name }}</text>
        </view>
      </view>
    </u-popup>
  </view>
</template>

<script setup lang="ts">
import { onShow } from '@dcloudio/uni-app'
import { ref } from 'vue'
import { confirmBossOrder, fetchBossOrders, fetchBossOrderSummary, type OrderInfo } from '../../../api/order'
import { assignOrderToWorker, fetchBossWorkers, type WorkerItem } from '../../../api/worker'
import { useUserStore } from '../../../stores/user'

const userStore = useUserStore()
const orders = ref<OrderInfo[]>([])
const summary = ref({ todayTotal: 0, pendingConfirm: 0, pendingPick: 0, pendingPrice: 0 })
const workers = ref<WorkerItem[]>([])
const loading = ref(false)
const activeTab = ref('')
const showAssign = ref(false)
const assignOrderId = ref<number | null>(null)

const tabs = [
  { label: '全部', value: '' },
  { label: '待确认', value: 'PENDING_CONFIRM' },
  { label: '待分拣', value: 'PENDING_PICK' },
  { label: '待录价', value: 'PENDING_PRICE' },
]

onShow(async () => {
  if (!userStore.isLoggedIn || !userStore.isBoss) {
    uni.reLaunch({ url: '/pages/login/index' })
    return
  }
  await refresh()
})

async function refresh() {
  loading.value = true
  try {
    summary.value = await fetchBossOrderSummary()
    orders.value = await fetchBossOrders(activeTab.value || undefined)
    workers.value = await fetchBossWorkers()
  } finally {
    loading.value = false
  }
}

function onTabTap(e: { currentTarget: { dataset: { value?: string } } }) {
  activeTab.value = e.currentTarget.dataset.value || ''
  refresh()
}

async function handleConfirm(id: number) {
  try {
    await confirmBossOrder(id)
    uni.showToast({ title: '已确认', icon: 'success' })
    await refresh()
  } catch (err) {
    uni.showToast({ title: err instanceof Error ? err.message : '确认失败', icon: 'none' })
  }
}

function openAssign(orderId: number) {
  assignOrderId.value = orderId
  showAssign.value = true
}

async function handleAssign(e: { currentTarget: { dataset: { id?: string | number } } }) {
  const workerId = Number(e.currentTarget.dataset.id)
  const orderId = assignOrderId.value
  if (!workerId || !orderId) return
  try {
    await assignOrderToWorker(orderId, workerId)
    showAssign.value = false
    uni.showToast({ title: '派单成功', icon: 'success' })
    await refresh()
  } catch (err) {
    uni.showToast({ title: err instanceof Error ? err.message : '派单失败', icon: 'none' })
  }
}

function statusType(status: string) {
  if (status === 'PENDING_CONFIRM') return 'warning'
  if (status === 'PENDING_PICK') return 'primary'
  if (status === 'PENDING_PRICE') return 'warning'
  if (status === 'COMPLETED') return 'success'
  return 'info'
}

function formatTime(value?: string) {
  if (!value) return '—'
  return value.replace('T', ' ').slice(0, 16)
}

function goProducts() {
  uni.navigateTo({ url: '/pages/boss/products/index' })
}

function goCustomers() {
  uni.navigateTo({ url: '/pages/boss/customers/index' })
}

function goPricing() {
  uni.navigateTo({ url: '/pages/boss/pricing/index' })
}

function goPricingDetail(id: number) {
  uni.navigateTo({ url: `/pages/boss/pricing/detail/index?id=${id}` })
}

function handleLogout() {
  userStore.signOut()
}
</script>

<style scoped lang="scss">
.page {
  padding: 24rpx;
}

.toolbar-left {
  display: flex;
  gap: 16rpx;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24rpx;
}

.logout {
  font-size: 26rpx;
  color: #999;
}

.stats {
  display: flex;
  background: #fff;
  border-radius: 16rpx;
  padding: 32rpx 0;
  margin-bottom: 24rpx;
}

.stat-item {
  flex: 1;
  text-align: center;
}

.stat-value {
  display: block;
  font-size: 36rpx;
  font-weight: 600;
  color: #27ae60;
}

.stat-value.warn {
  color: #e67e22;
}

.stat-label {
  display: block;
  margin-top: 8rpx;
  font-size: 24rpx;
  color: #999;
}

.tabs {
  white-space: nowrap;
  margin-bottom: 24rpx;
}

.tab-item {
  display: inline-block;
  padding: 12rpx 28rpx;
  margin-right: 16rpx;
  border-radius: 999rpx;
  background: #fff;
  color: #666;
  font-size: 26rpx;
}

.tab-item.active {
  background: #e8f8ef;
  color: #27ae60;
}

.loading-wrap,
.empty-wrap {
  padding: 60rpx 0;
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
  align-items: flex-start;
}

.customer-name {
  display: block;
  font-size: 32rpx;
  font-weight: 600;
}

.order-no {
  display: block;
  margin-top: 8rpx;
  font-size: 24rpx;
  color: #999;
}

.meta {
  display: block;
  margin-top: 12rpx;
  font-size: 24rpx;
  color: #666;
}

.actions {
  display: flex;
  gap: 16rpx;
  margin-top: 20rpx;
}

.assign-popup {
  padding: 32rpx;
  max-height: 60vh;
}

.assign-title {
  display: block;
  margin-bottom: 24rpx;
  font-size: 32rpx;
  font-weight: 600;
}

.assign-empty {
  color: #999;
  font-size: 26rpx;
  padding: 24rpx 0;
}

.worker-item {
  padding: 28rpx 0;
  border-bottom: 1rpx solid #f2f3f5;
  font-size: 30rpx;
}
</style>
