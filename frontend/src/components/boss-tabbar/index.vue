<template>
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
        <text class="tab-icon">{{ item.icon }}</text>
        <text class="tab-text" :class="{ active: active === item.key }">{{ item.label }}</text>
      </template>
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
            <view class="quick-icon" :class="item.color">
              <text class="quick-icon-text">{{ item.icon }}</text>
            </view>
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
  color: 'green' | 'blue' | 'yellow' | 'purple'
  url: string
}

const props = defineProps<{
  active: BossTabKey
}>()

const showQuickMenu = ref(false)

const tabs: TabItem[] = [
  { key: 'orders', label: '订单', icon: '📋', url: '/pages/boss/orders/index' },
  { key: 'pricing', label: '录价', icon: '✏️', url: '/pages/boss/pricing/index' },
  { key: 'add', label: '', icon: '+' },
  { key: 'procurement', label: '采购', icon: '🛒', url: '/pages/boss/procurement/index' },
  { key: 'mine', label: '我的', icon: '👤', url: '/pages/boss/mine/index' },
]

const quickItems: QuickItem[] = [
  { key: 'sales-order', label: '销售开单', icon: '📝', color: 'green', url: '/pages/boss/sales-order/index' },
  { key: 'sales-payment', label: '销售记账', icon: '¥', color: 'blue', url: '/pages/boss/sales-payment/index' },
  { key: 'purchase-order', label: '采购开单', icon: '🛒', color: 'yellow', url: '/pages/boss/purchase-order/index' },
  { key: 'purchase-payment', label: '采购记账', icon: '¥', color: 'purple', url: '/pages/boss/purchase-payment/index' },
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
  align-items: flex-end;
  justify-content: space-around;
  height: calc(112rpx + env(safe-area-inset-bottom));
  padding: 0 8rpx env(safe-area-inset-bottom);
  background: #2f3033;
  border-radius: 24rpx 24rpx 0 0;
  box-shadow: 0 -4rpx 24rpx rgba(0, 0, 0, 0.12);
}

.tab-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 96rpx;
}

.tab-item.center {
  position: relative;
  top: -30rpx;
}

.tab-icon {
  font-size: 38rpx;
  line-height: 1;
  opacity: 0.85;
}

.tab-item.active .tab-icon {
  opacity: 1;
}

.tab-text {
  margin-top: 6rpx;
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.55);
}

.tab-text.active {
  color: #fff;
  font-weight: 600;
}

.add-btn {
  width: 104rpx;
  height: 104rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, #07c160, #06ad56);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8rpx 28rpx rgba(7, 193, 96, 0.45);
}

.add-icon {
  font-size: 60rpx;
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
  justify-content: space-around;
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
  border-radius: 28rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.quick-icon.green { background: #07c160; }
.quick-icon.blue { background: #3b8cff; }
.quick-icon.yellow { background: #f5b731; }
.quick-icon.purple { background: #9b59ff; }

.quick-icon-text {
  font-size: 44rpx;
  color: #fff;
  font-weight: 600;
  line-height: 1;
}

.quick-label {
  margin-top: 16rpx;
  font-size: 26rpx;
  color: #333;
}

.quick-cancel {
  height: 96rpx;
  line-height: 96rpx;
  text-align: center;
  font-size: 32rpx;
  color: #333;
  background: #f5f6f7;
  border-radius: 16rpx;
}
</style>
