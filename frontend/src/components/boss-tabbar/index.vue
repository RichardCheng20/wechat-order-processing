<template>
  <view class="boss-tabbar-shell">
    <view class="boss-tabbar">
      <view
        v-for="item in tabs"
        :key="item.key"
        class="tab-item"
        :class="{ center: item.key === 'add', active: active === item.key }"
        @tap="handleTap(item)"
      >
        <view v-if="item.key === 'add'" class="add-btn">
          <text class="add-icon">+</text>
        </view>
        <template v-else>
          <AppIcon
            class="tab-icon"
            :name="item.icon"
            tone="green"
            :size="23"
            :tile-size="50"
            :radius="16"
            :active="active === item.key"
          />
          <text class="tab-text" :class="{ active: active === item.key }">{{ item.label }}</text>
        </template>
      </view>
    </view>

    <u-popup
      :show="showQuickMenu"
      mode="bottom"
      round="20"
      :safe-area-inset-bottom="true"
      @close="closeQuickMenu"
    >
      <view class="quick-panel">
        <view class="quick-grid">
          <view
            v-for="item in quickItems"
            :key="item.key"
            class="quick-item"
            @tap="handleQuickTap(item)"
          >
            <AppIcon
              class="quick-icon"
              :name="item.icon"
              :tone="item.color"
              :size="32"
              :tile-size="112"
              :radius="24"
            />
            <text class="quick-label">{{ item.label }}</text>
          </view>
        </view>
        <view class="quick-cancel" @tap="closeQuickMenu">取消</view>
      </view>
    </u-popup>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import AppIcon from '../AppIcon.vue'

export type BossTabKey = 'orders' | 'pricing' | 'procurement' | 'mine'

interface TabItem {
  key: BossTabKey | 'add'
  label: string
  icon: string
  url?: string
}

interface QuickItem {
  key: string
  label: string
  icon: string
  color: 'green' | 'blue' | 'orange' | 'purple'
  url: string
}

const props = defineProps<{
  active: BossTabKey
}>()

const showQuickMenu = ref(false)

const tabs: TabItem[] = [
  { key: 'orders', label: '订单', icon: 'order', url: '/pages/boss/orders/index' },
  { key: 'pricing', label: '录价', icon: 'pricing', url: '/pages/boss/pricing/index' },
  { key: 'add', label: '', icon: '+' },
  { key: 'procurement', label: '采购', icon: 'procurement', url: '/pages/boss/procurement/index' },
  { key: 'mine', label: '我的', icon: 'mine', url: '/pages/boss/mine/index' },
]

const quickItems: QuickItem[] = [
  { key: 'sales-order', label: '销售开单', icon: 'salesOrder', color: 'green', url: '/pages/boss/sales-order/index' },
  { key: 'sales-payment', label: '销售记账', icon: 'salesPayment', color: 'blue', url: '/pages/boss/sales-payment/index' },
  { key: 'purchase-order', label: '采购开单', icon: 'purchaseOrder', color: 'orange', url: '/pages/boss/purchase-order/index' },
  { key: 'purchase-payment', label: '采购记账', icon: 'purchasePayment', color: 'purple', url: '/pages/boss/purchase-payment/index' },
]

function handleTap(item: TabItem) {
  if (item.key === 'add') {
    showQuickMenu.value = true
    return
  }
  if (item.key === props.active || !item.url) return
  uni.reLaunch({ url: item.url })
}

function closeQuickMenu() {
  showQuickMenu.value = false
}

function handleQuickTap(item: QuickItem) {
  closeQuickMenu()
  uni.navigateTo({ url: item.url })
}
</script>

<style scoped lang="scss">
.boss-tabbar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 999;
  display: flex;
  align-items: center;
  justify-content: space-around;
  height: calc(98rpx + env(safe-area-inset-bottom));
  padding: 6rpx 8rpx calc(6rpx + env(safe-area-inset-bottom));
  background: #fff;
  border-top: 1rpx solid #dce6df;
  box-shadow: 0 -4rpx 24rpx rgba(23, 33, 27, 0.08);
}

.tab-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 82rpx;
  gap: 4rpx;
}

.tab-item.center {
  position: relative;
  top: -22rpx;
}

.tab-icon {
  width: 50rpx;
  height: 50rpx;
  border-radius: 16rpx;
}

.tab-text {
  font-size: 24rpx;
  color: #66736b;
  line-height: 1.15;
}

.tab-text.active {
  color: #17211b;
  font-weight: 700;
}

.add-btn {
  width: 92rpx;
  height: 92rpx;
  border-radius: 50%;
  background: #0b7f3a;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8rpx 28rpx rgba(7, 193, 96, 0.45);
}

.add-icon {
  font-size: 54rpx;
  color: #fff;
  line-height: 1;
  margin-top: -4rpx;
}

.quick-panel {
  background: #fff;
  padding: 48rpx 32rpx calc(24rpx + env(safe-area-inset-bottom));
}

.quick-grid {
  display: flex;
  justify-content: space-between;
  padding: 0 8rpx 32rpx;
}

.quick-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 150rpx;
}

.quick-icon {
  width: 112rpx;
  height: 112rpx;
  border-radius: 24rpx;
}

.quick-label {
  margin-top: 16rpx;
  font-size: 28rpx;
  color: #17211b;
  font-weight: 600;
}

.quick-cancel {
  height: 96rpx;
  line-height: 96rpx;
  text-align: center;
  font-size: 32rpx;
  color: #17211b;
  background: #f5f7f3;
  border-radius: 16rpx;
}
</style>
