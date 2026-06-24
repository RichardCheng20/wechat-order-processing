<template>
  <view class="flow-bar">
    <view v-for="(step, idx) in steps" :key="step.key" class="flow-cell">
      <view
        v-if="idx > 0"
        class="flow-connector"
        :class="connectorPhase(steps[idx - 1])"
      />
      <view
        class="flow-step"
        :class="step.phase"
        @tap.stop="emit('tap', step.key)"
      >
        <text class="flow-label">{{ step.label }}</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
export type FlowStepKey = 'confirm' | 'confirmed' | 'pick' | 'price' | 'reconcile' | 'pay'

export interface OrderFlowStep {
  key: FlowStepKey
  label: string
  phase: 'pending' | 'current' | 'done' | 'cancelled' | 'danger'
}

defineProps<{
  steps: OrderFlowStep[]
}>()

const emit = defineEmits<{
  tap: [key: FlowStepKey]
}>()

function connectorPhase(prev: OrderFlowStep): 'done' | 'pending' {
  return prev.phase === 'done' ? 'done' : 'pending'
}
</script>

<style scoped lang="scss">
.flow-bar {
  display: flex;
  align-items: center;
  margin-top: 20rpx;
  padding-top: 20rpx;
  border-top: 1rpx solid #f0f0f0;
}

.flow-cell {
  flex: 1;
  display: flex;
  align-items: center;
  min-width: 0;
}

.flow-connector {
  flex: 1;
  height: 0;
  min-width: 8rpx;
  border-top: 2rpx dashed #ddd;
  margin: 0 4rpx;
}

.flow-connector.done {
  border-color: #86efac;
}

.flow-step {
  flex-shrink: 0;
  padding: 8rpx 6rpx;
  border-radius: 8rpx;
  border: 1rpx solid #e5e5e5;
  background: #f5f5f5;
  text-align: center;
  max-width: 100%;
}

.flow-label {
  display: block;
  font-size: 20rpx;
  line-height: 1.3;
  color: #999;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.flow-step.pending {
  background: #f5f5f5;
  border-color: #e5e5e5;
}

.flow-step.pending .flow-label {
  color: #bbb;
}

.flow-step.current {
  background: #fff7e6;
  border-color: #f59e0b;
}

.flow-step.current .flow-label {
  color: #d97706;
  font-weight: 600;
}

.flow-step.done {
  background: #ecfdf3;
  border-color: #86efac;
}

.flow-step.done .flow-label {
  color: #16a34a;
  font-weight: 600;
}

.flow-step.danger {
  background: #fef2f2;
  border-color: #f87171;
}

.flow-step.danger .flow-label {
  color: #dc2626;
  font-weight: 600;
}

.flow-step.cancelled {
  background: #fef2f2;
  border-color: #fecaca;
}

.flow-step.cancelled .flow-label {
  color: #ef4444;
}
</style>
