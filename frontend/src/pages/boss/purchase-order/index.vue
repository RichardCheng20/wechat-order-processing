<template>
  <view class="page">
    <view class="header">
      <text class="title">采购开单</text>
      <text class="subtitle">用于录入买进商品的数据，便于库存及成本的管理</text>
      <text v-if="receiveDate" class="date-hint">收货日期：{{ receiveDateLabel }}</text>
    </view>

    <view v-if="loading" class="loading-wrap">
      <u-loading-icon text="加载中" />
    </view>

    <view v-else-if="items.length === 0" class="placeholder">
      <u-empty mode="data" text="暂无待采购商品" />
      <text class="hint">请先在采购任务中选择商品，或调整收货日期</text>
      <u-button class="link-btn" type="primary" plain text="返回采购任务" @click="goProcurement" />
    </view>

    <scroll-view v-else scroll-y class="item-list">
      <view v-for="item in items" :key="item.productId" class="item-card">
        <view class="item-head">
          <text class="product-name">{{ item.productName }}</text>
          <text class="item-amount">￥{{ formatMoney(item.totalAmount) }}</text>
        </view>
        <view class="item-meta">
          <text>需采购 {{ formatQty(item.needQty) }}{{ item.unit }}</text>
          <text v-if="item.purchasePrice != null">进价 {{ formatMoney(item.purchasePrice) }}元/{{ item.unit }}</text>
        </view>
      </view>
    </scroll-view>

    <view v-if="items.length > 0" class="footer">
      <view class="footer-summary">
        <text>共 {{ items.length }} 种，预估 {{ formatMoney(totalAmount) }} 元</text>
      </view>
      <view class="footer-actions">
        <u-button plain text="供应商" @click="goSuppliers" />
        <u-button type="primary" text="提交采购单（开发中）" disabled />
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { fetchProcurementTasks, type ProcurementTaskItem } from '@common/api/procurement'
import { useUserStore } from '@common/stores/user'

const userStore = useUserStore()
const loading = ref(false)
const receiveDate = ref('')
const productIdFilter = ref<number[]>([])
const items = ref<ProcurementTaskItem[]>([])

const receiveDateLabel = computed(() => {
  if (!receiveDate.value) return '—'
  const parts = receiveDate.value.split('-')
  if (parts.length < 3) return receiveDate.value
  return `${parts[1]}-${parts[2]}`
})

const totalAmount = computed(() =>
  items.value.reduce((sum, item) => sum + (item.totalAmount || 0), 0),
)

onLoad((options) => {
  if (options?.receiveDate) {
    receiveDate.value = String(options.receiveDate)
  }
  if (options?.productIds) {
    productIdFilter.value = String(options.productIds)
      .split(',')
      .map((id) => Number(id))
      .filter((id) => !Number.isNaN(id))
  }
})

onShow(async () => {
  if (!userStore.isLoggedIn || !userStore.isBoss) {
    uni.reLaunch({ url: '/packages/common/login/index' })
    return
  }
  if (!receiveDate.value) {
    receiveDate.value = formatDate(new Date())
  }
  await loadItems()
})

async function loadItems() {
  loading.value = true
  try {
    const task = await fetchProcurementTasks({ receiveDate: receiveDate.value })
    let list = task.items || []
    if (productIdFilter.value.length > 0) {
      const filterSet = new Set(productIdFilter.value)
      list = list.filter((item) => filterSet.has(item.productId))
    }
    items.value = list
    if (task.receiveDate) {
      receiveDate.value = task.receiveDate
    }
  } catch (e) {
    items.value = []
    uni.showToast({ title: e instanceof Error ? e.message : '加载失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

function goProcurement() {
  uni.reLaunch({ url: '/pages/boss/procurement/index' })
}

function goSuppliers() {
  uni.navigateTo({ url: '/pages/boss/suppliers/index' })
}

function formatDate(date: Date) {
  const y = date.getFullYear()
  const m = `${date.getMonth() + 1}`.padStart(2, '0')
  const d = `${date.getDate()}`.padStart(2, '0')
  return `${y}-${m}-${d}`
}

function formatMoney(value?: number | null) {
  if (value == null) return '0.00'
  return Number(value).toFixed(2)
}

function formatQty(value?: number) {
  if (value == null) return '0'
  return Number(value).toFixed(2).replace(/\.?0+$/, '') || '0'
}
</script>

<style scoped lang="scss">
.page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f6f8;
  padding-bottom: calc(180rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
}

.header {
  padding: 24rpx;
  background: #fff;
}

.title {
  display: block;
  font-size: 36rpx;
  font-weight: 600;
}

.subtitle {
  display: block;
  margin-top: 12rpx;
  font-size: 26rpx;
  color: #888;
}

.date-hint {
  display: block;
  margin-top: 12rpx;
  font-size: 26rpx;
  color: #e67e22;
}

.loading-wrap,
.placeholder {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80rpx 24rpx;
}

.hint {
  display: block;
  margin-top: 16rpx;
  font-size: 24rpx;
  color: #999;
}

.link-btn {
  margin-top: 32rpx;
}

.item-list {
  flex: 1;
  height: 0;
  padding: 16rpx 24rpx;
  box-sizing: border-box;
}

.item-card {
  padding: 24rpx;
  margin-bottom: 16rpx;
  background: #fff;
  border-radius: 16rpx;
}

.item-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16rpx;
}

.product-name {
  font-size: 32rpx;
  font-weight: 600;
  color: #111;
}

.item-amount {
  flex-shrink: 0;
  font-size: 30rpx;
  font-weight: 600;
}

.item-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 20rpx;
  margin-top: 12rpx;
  font-size: 26rpx;
  color: #666;
}

.footer {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 20rpx 24rpx calc(20rpx + env(safe-area-inset-bottom));
  background: #fff;
  box-shadow: 0 -4rpx 20rpx rgba(0, 0, 0, 0.06);
}

.footer-summary {
  margin-bottom: 16rpx;
  font-size: 28rpx;
  color: #333;
}

.footer-actions {
  display: flex;
  gap: 16rpx;
}
</style>
