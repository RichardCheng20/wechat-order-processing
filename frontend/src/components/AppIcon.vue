<template>
  <view class="app-icon" :class="[toneClass, { tile, active }]" :style="rootStyle">
    <u-icon :name="resolvedName" :size="size" :color="iconColor" />
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue'

type IconTone = 'green' | 'blue' | 'orange' | 'purple' | 'red' | 'teal' | 'pink' | 'gray' | 'white'

const props = withDefaults(
  defineProps<{
    name: string
    tone?: IconTone
    size?: number
    tileSize?: number
    radius?: number
    tile?: boolean
    active?: boolean
  }>(),
  {
    tone: 'green',
    size: 24,
    tileSize: 76,
    radius: 20,
    tile: true,
    active: false,
  },
)

const iconMap: Record<string, string> = {
  order: 'order',
  pricing: 'edit-pen',
  procurement: 'shopping-cart',
  mine: 'account',
  salesOrder: 'file-text',
  salesPayment: 'rmb-circle',
  purchaseOrder: 'shopping-cart',
  purchasePayment: 'coupon',
  product: 'bag',
  category: 'grid',
  unit: 'tags',
  customer: 'account',
  supplier: 'server-man',
  quote: 'rmb',
  report: 'file-text',
  ranking: 'level',
  inventory: 'folder',
  invite: 'share-square',
  colleague: 'plus-people-fill',
  settings: 'setting',
  help: 'question-circle',
  message: 'bell',
  logout: 'arrow-rightward',
  search: 'search',
  filter: 'list-dot',
  calendar: 'calendar',
  batch: 'list',
  voice: 'mic',
  camera: 'camera-fill',
  scan: 'scan',
  scale: 'level',
  bluetooth: 'wifi',
  tag: 'tags',
  delete: 'trash',
  cart: 'shopping-cart',
  plus: 'plus',
}

const toneColors: Record<IconTone, { color: string; active: string }> = {
  green: { color: '#0b7f3a', active: '#ffffff' },
  blue: { color: '#1f6feb', active: '#ffffff' },
  orange: { color: '#b86b00', active: '#ffffff' },
  purple: { color: '#6f4bb8', active: '#ffffff' },
  red: { color: '#c2352a', active: '#ffffff' },
  teal: { color: '#087a6a', active: '#ffffff' },
  pink: { color: '#a9356a', active: '#ffffff' },
  gray: { color: '#66736b', active: '#ffffff' },
  white: { color: '#ffffff', active: '#ffffff' },
}

const resolvedName = computed(() => iconMap[props.name] || props.name)
const toneClass = computed(() => `tone-${props.tone}`)
const rootStyle = computed(() => {
  if (!props.tile) return {}
  return {
    width: `${props.tileSize}rpx`,
    height: `${props.tileSize}rpx`,
    borderRadius: `${props.radius}rpx`,
  }
})
const iconColor = computed(() => {
  const tone = toneColors[props.tone]
  return props.active ? tone.active : tone.color
})
</script>

<style scoped lang="scss">
.app-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
  box-sizing: border-box;
}

.app-icon.tile {
  width: 76rpx;
  height: 76rpx;
  border-radius: 20rpx;
  border: 1rpx solid rgba(23, 33, 27, 0.06);
}

.tone-green.tile { background: #e7f4ea; }
.tone-blue.tile { background: #eaf2ff; }
.tone-orange.tile { background: #fff3df; }
.tone-purple.tile { background: #f0ebff; }
.tone-red.tile { background: #fff0ee; }
.tone-teal.tile { background: #e6f5f2; }
.tone-pink.tile { background: #ffeef6; }
.tone-gray.tile { background: #eef2ed; }
.tone-white.tile { background: rgba(255, 255, 255, 0.16); }

.tone-green.tile.active {
  background: #0b7f3a;
  border-color: #0b7f3a;
}

.tone-blue.tile.active {
  background: #1f6feb;
  border-color: #1f6feb;
}

.tone-gray.tile.active {
  background: #17211b;
  border-color: #17211b;
}
</style>
