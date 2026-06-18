<template>
  <view class="page boss-page">
    <view v-if="loading" class="loading-wrap">
      <u-loading-icon text="加载中" />
    </view>

    <template v-else-if="order">
      <view class="table-head">
        <text class="col name">商品名</text>
        <text class="col order">下单数</text>
        <text class="col actual">出库数</text>
        <text class="col unit">单位</text>
        <text class="col price">价格</text>
      </view>

      <scroll-view scroll-y class="table-body" :class="{ 'with-keypad': showKeypad }">
        <view v-for="item in order.items || []" :key="item.id" class="table-row">
          <view class="col name">
            <text class="product-name">{{ item.productName }}</text>
            <text v-if="item.pickRemark" class="product-remark">{{ item.pickRemark }}</text>
            <text v-if="item.shortageFlag === 1" class="shortage-tag">缺货</text>
          </view>
          <text class="col order">{{ item.orderQty }}{{ item.unit }}</text>
          <view
            class="col actual input-cell"
            :class="{ active: isActive(item.id, 'actualQty') }"
            @tap="focusField(item.id!, 'actualQty')"
          >
            <text>{{ displayActual(item) }}</text>
          </view>
          <text class="col unit">{{ item.unit }}</text>
          <view
            class="col price input-cell"
            :class="{ active: isActive(item.id, 'dealPrice') }"
            @tap="focusField(item.id!, 'dealPrice')"
          >
            <text>{{ displayPrice(item) }}</text>
          </view>
        </view>
      </scroll-view>

      <view v-if="showKeypad" class="keypad-wrap">
        <view class="keypad-toggle" @tap="showKeypad = false">⌄</view>
        <view class="keypad">
          <view class="keypad-left">
            <view class="key-row">
              <view class="key" @tap="inputKey('1')">1</view>
              <view class="key" @tap="inputKey('2')">2</view>
              <view class="key" @tap="inputKey('3')">3</view>
              <view class="key fn" @tap="backspace">⌫</view>
            </view>
            <view class="key-row">
              <view class="key" @tap="inputKey('4')">4</view>
              <view class="key" @tap="inputKey('5')">5</view>
              <view class="key" @tap="inputKey('6')">6</view>
              <view class="key fn shortage" @tap="markShortage">缺货</view>
            </view>
            <view class="key-row">
              <view class="key" @tap="inputKey('7')">7</view>
              <view class="key" @tap="inputKey('8')">8</view>
              <view class="key" @tap="inputKey('9')">9</view>
              <view class="key confirm" @tap="confirmField">确定</view>
            </view>
            <view class="key-row">
              <view class="key wide" @tap="inputKey('0')">0</view>
              <view class="key" @tap="inputKey('.')">.</view>
              <view class="key confirm placeholder" />
            </view>
          </view>
        </view>
      </view>

      <view class="boss-bottom-bar pick-bottom-bar dual">
        <button class="boss-outline-btn flex" :loading="fillingAll" @tap="handleFillAll">一键拣单出库</button>
        <button class="boss-primary-btn flex" :loading="completing" @tap="handleComplete">完成</button>
      </view>
    </template>
  </view>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import {
  completeBossPick,
  fetchBossPickDetail,
  fillBossPickQty,
  startBossPick,
  updateBossPickItem,
} from '../../../../api/pick'
import type { OrderInfo, OrderLineItem } from '../../../../api/order'
import { useUserStore } from '../../../../stores/user'

type FieldType = 'actualQty' | 'dealPrice'

const userStore = useUserStore()
const order = ref<OrderInfo | null>(null)
const loading = ref(false)
const completing = ref(false)
const fillingAll = ref(false)
const showKeypad = ref(false)
const orderId = ref(0)

const activeItemId = ref<number | null>(null)
const activeField = ref<FieldType>('actualQty')
const draftValues = reactive<Record<string, string>>({})

function draftKey(itemId: number, field: FieldType) {
  return `${itemId}-${field}`
}

onLoad(async (query) => {
  if (!userStore.isLoggedIn || !userStore.isBoss) {
    uni.reLaunch({ url: '/pages/login/index' })
    return
  }
  orderId.value = Number(query?.id || 0)
  await loadPick(true)
})

async function loadPick(autoStart = false) {
  if (!orderId.value) return
  loading.value = true
  try {
    if (autoStart) {
      order.value = await startBossPick(orderId.value)
    } else {
      order.value = await fetchBossPickDetail(orderId.value)
    }
    if (order.value?.customerName) {
      const no = order.value.orderNo ? ` · ${order.value.orderNo}` : ''
      uni.setNavigationBarTitle({ title: `${order.value.customerName}${no}` })
    }
    initDrafts()
  } catch (e) {
    uni.showToast({ title: e instanceof Error ? e.message : '加载失败', icon: 'none' })
    setTimeout(() => uni.navigateBack(), 800)
  } finally {
    loading.value = false
  }
}

function initDrafts() {
  Object.keys(draftValues).forEach((k) => delete draftValues[k])
  for (const item of order.value?.items || []) {
    if (!item.id) continue
    draftValues[draftKey(item.id, 'actualQty')] = String(
      item.shortageFlag === 1 ? 0 : (item.actualQty ?? 0),
    )
    draftValues[draftKey(item.id, 'dealPrice')] = item.dealPrice != null ? String(item.dealPrice) : ''
  }
}

function displayActual(item: OrderLineItem) {
  if (!item.id) return '0'
  if (isActive(item.id, 'actualQty')) {
    return draftValues[draftKey(item.id, 'actualQty')] ?? '0'
  }
  if (item.shortageFlag === 1) return '0'
  return item.actualQty != null ? String(item.actualQty) : '0'
}

function displayPrice(item: OrderLineItem) {
  if (!item.id) return ''
  if (isActive(item.id, 'dealPrice')) {
    return draftValues[draftKey(item.id, 'dealPrice')] ?? ''
  }
  return item.dealPrice != null ? String(item.dealPrice) : '0'
}

function isActive(itemId: number | undefined, field: FieldType) {
  return itemId != null && activeItemId.value === itemId && activeField.value === field
}

function focusField(itemId: number, field: FieldType, openKeypad = true) {
  activeItemId.value = itemId
  activeField.value = field
  if (openKeypad) showKeypad.value = true
  const item = order.value?.items?.find((i) => i.id === itemId)
  if (!item) return
  if (draftValues[draftKey(itemId, field)] === undefined || draftValues[draftKey(itemId, field)] === '') {
    if (field === 'actualQty') {
      draftValues[draftKey(itemId, field)] = String(item.actualQty ?? 0)
    } else {
      draftValues[draftKey(itemId, field)] = item.dealPrice != null ? String(item.dealPrice) : ''
    }
  }
}

function inputKey(key: string) {
  if (activeItemId.value == null) return
  const k = draftKey(activeItemId.value, activeField.value)
  let val = draftValues[k] ?? ''
  if (key === '.' && val.includes('.')) return
  if (val === '0' && key !== '.') {
    val = key
  } else {
    val += key
  }
  draftValues[k] = val
}

function backspace() {
  if (activeItemId.value == null) return
  const k = draftKey(activeItemId.value, activeField.value)
  draftValues[k] = (draftValues[k] ?? '').slice(0, -1)
}

async function confirmField() {
  if (activeItemId.value == null) return
  const itemId = activeItemId.value
  const field = activeField.value
  const raw = draftValues[draftKey(itemId, field)] ?? ''
  let num = raw === '' ? undefined : Number(raw)
  const item = order.value?.items?.find((i) => i.id === itemId)
  if (field === 'actualQty' && num != null && item) {
    const maxQty = Number(item.orderQty ?? 0)
    if (num > maxQty) {
      uni.showToast({ title: `出库数不能大于下单数${maxQty}`, icon: 'none' })
      num = maxQty
      draftValues[draftKey(itemId, field)] = String(maxQty)
    }
  }
  try {
    if (field === 'actualQty') {
      order.value = await updateBossPickItem(orderId.value, itemId, { actualQty: num, shortageFlag: 0 })
    } else {
      order.value = await updateBossPickItem(orderId.value, itemId, { dealPrice: num })
    }
    initDrafts()
    moveToNextField(itemId, field)
  } catch (e) {
    uni.showToast({ title: e instanceof Error ? e.message : '保存失败', icon: 'none' })
  }
}

function moveToNextField(itemId: number, field: FieldType) {
  const items = order.value?.items || []
  const idx = items.findIndex((i) => i.id === itemId)
  if (field === 'actualQty') {
    focusField(itemId, 'dealPrice')
    return
  }
  if (idx >= 0 && idx < items.length - 1) {
    const next = items[idx + 1]
    if (next.id) focusField(next.id, 'actualQty')
  }
}

async function markShortage() {
  if (activeItemId.value == null) return
  try {
    order.value = await updateBossPickItem(orderId.value, activeItemId.value, {
      shortageFlag: 1,
      actualQty: 0,
    })
    initDrafts()
    uni.showToast({ title: '已标记缺货', icon: 'none' })
  } catch (e) {
    uni.showToast({ title: e instanceof Error ? e.message : '操作失败', icon: 'none' })
  }
}

async function handleFillAll() {
  uni.showModal({
    title: '确认拣单出库',
    content: '确认拣单即确认已配好客户需要的货物，会相应减少对应产品库存。',
    confirmText: '确认',
    cancelText: '取消',
    success: async (res) => {
      if (!res.confirm) return
      fillingAll.value = true
      try {
        order.value = await fillBossPickQty(orderId.value)
        initDrafts()
        activeItemId.value = null
        showKeypad.value = false
        uni.showToast({ title: '拣单出库完成', icon: 'success' })
      } catch (e) {
        uni.showToast({ title: e instanceof Error ? e.message : '操作失败', icon: 'none' })
      } finally {
        fillingAll.value = false
      }
    },
  })
}

async function handleComplete() {
  completing.value = true
  try {
    if (activeItemId.value != null) {
      await confirmField()
    }
    await completeBossPick(orderId.value)
    uni.showToast({ title: '分拣完成', icon: 'success' })
    setTimeout(() => uni.navigateBack(), 500)
  } catch (e) {
    uni.showToast({ title: e instanceof Error ? e.message : '完成失败', icon: 'none' })
  } finally {
    completing.value = false
  }
}
</script>

<style scoped lang="scss">
@import '../../../../styles/boss-footer.scss';

.page {
  background: #fff;
}

.loading-wrap {
  padding: 120rpx 0;
  text-align: center;
}

.table-head,
.table-row {
  display: flex;
  align-items: flex-start;
  padding: 16rpx 20rpx;
  font-size: 24rpx;
}

.table-head {
  background: #f5f6f7;
  color: #999;
  flex-shrink: 0;
}

.table-body {
  flex: 1;
  overflow: hidden;
  box-sizing: border-box;
}

.table-body.with-keypad {
  max-height: calc(100vh - 520rpx);
}

.table-row {
  border-bottom: 1rpx solid #f2f3f5;
}

.col {
  flex-shrink: 0;
}

.col.name { flex: 2.2; min-width: 0; }
.col.order { width: 120rpx; text-align: center; color: #666; }
.col.actual { width: 100rpx; }
.col.unit { width: 64rpx; text-align: center; color: #666; }
.col.price { width: 100rpx; }

.product-name {
  display: block;
  font-size: 28rpx;
  color: #333;
}

.product-remark {
  display: block;
  margin-top: 6rpx;
  font-size: 22rpx;
  color: #e67e22;
}

.shortage-tag {
  display: inline-block;
  margin-top: 6rpx;
  padding: 2rpx 10rpx;
  font-size: 20rpx;
  color: #e74c3c;
  background: #ffecec;
  border-radius: 6rpx;
}

.input-cell {
  min-height: 56rpx;
  line-height: 56rpx;
  text-align: center;
  background: #f5f6f7;
  border-radius: 8rpx;
  border: 2rpx solid transparent;
  font-size: 28rpx;
}

.input-cell.active {
  border-color: #07c160;
  background: #e8f8ef;
}

.keypad-wrap {
  flex-shrink: 0;
  background: #eef0f2;
  border-top: 1rpx solid #ddd;
}

.keypad-toggle {
  text-align: center;
  padding: 8rpx;
  color: #999;
  font-size: 28rpx;
}

.keypad {
  padding: 0 8rpx 8rpx;
}

.key-row {
  display: flex;
  gap: 8rpx;
  margin-bottom: 8rpx;
}

.key {
  flex: 1;
  height: 96rpx;
  line-height: 96rpx;
  text-align: center;
  background: #fff;
  border-radius: 8rpx;
  font-size: 36rpx;
  font-weight: 500;
}

.key.wide {
  flex: 2.05;
}

.key.fn {
  font-size: 28rpx;
  color: #666;
}

.key.shortage {
  font-size: 26rpx;
  color: #e74c3c;
}

.key.confirm {
  background: #07c160;
  color: #fff;
  font-size: 30rpx;
}

.key.confirm.placeholder {
  background: transparent;
}

.pick-bottom-bar.dual {
  gap: 16rpx;
}

.pick-bottom-bar .flex {
  flex: 1;
  margin: 0;
}

.boss-outline-btn {
  height: 88rpx;
  line-height: 88rpx;
  padding: 0 16rpx;
  font-size: 28rpx;
  font-weight: 600;
  color: #07c160;
  background: #fff;
  border: 2rpx solid #07c160;
  border-radius: 12rpx;
}

.boss-outline-btn::after {
  border: none;
}
</style>
