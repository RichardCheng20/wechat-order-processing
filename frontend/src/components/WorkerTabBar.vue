<template>
  <view class="worker-tab-bar">
    <view
      v-for="item in tabs"
      :key="item.key"
      class="tab-item"
      :class="{ active: active === item.key }"
      @tap="onTap(item.key)"
    >
      <AppIcon
        class="tab-icon"
        :name="item.icon"
        :tone="active === item.key ? 'green' : 'gray'"
        :size="22"
        :tile="false"
      />
      <text class="tab-label">{{ item.label }}</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import AppIcon from './AppIcon.vue'
import { switchWorkerTab, type WorkerTab } from '@/utils/worker-nav'

defineProps<{
  active: WorkerTab
}>()

const tabs: { key: WorkerTab; label: string; icon: string }[] = [
  { key: 'pending', label: '待拣单', icon: 'order' },
  { key: 'picked', label: '已拣单', icon: 'batch' },
  { key: 'mine', label: '我的', icon: 'mine' },
]

function onTap(key: WorkerTab) {
  switchWorkerTab(key)
}
</script>

<style scoped lang="scss">
.worker-tab-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 90;
  display: flex;
  align-items: stretch;
  height: calc(100rpx + env(safe-area-inset-bottom));
  padding-bottom: env(safe-area-inset-bottom);
  background: #fff;
  border-top: 1rpx solid #e8e8e8;
  box-shadow: 0 -2rpx 12rpx rgba(0, 0, 0, 0.04);
  box-sizing: border-box;
}

.tab-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4rpx;
}

.tab-label {
  font-size: 22rpx;
  color: #999;
  font-weight: 500;
}

.tab-item.active .tab-label {
  color: #07c160;
  font-weight: 600;
}
</style>
