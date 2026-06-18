<template>
  <view class="page">
    <view v-if="loading" class="loading-wrap">
      <u-loading-icon text="加载中" />
    </view>

    <template v-else-if="task">
      <view class="table-head">
        <text class="col name">商品名</text>
        <text class="col order">下单数</text>
        <text class="col actual">出库数</text>
        <text class="col unit">单位</text>
      </view>

      <scroll-view scroll-y class="table-body" :class="{ 'with-keypad': showKeypad }">
        <view v-for="item in task.items || []" :key="item.id" class="table-row">
          <view class="col name">
            <text class="product-name">{{ item.productName }}</text>
            <text v-if="item.pickRemark" class="product-remark">{{ item.pickRemark }}</text>
            <text v-if="item.shortageFlag === 1" class="shortage-tag">缺货</text>
          </view>
          <text class="col order">{{ item.orderQty }}{{ item.unit }}</text>
          <view
            class="col actual input-cell"
            :class="{ active: activeItemId === item.id }"
            @tap="focusItem(item.id)"
          >
            <text>{{ displayActual(item) }}</text>
          </view>
          <text class="col unit">{{ item.unit }}</text>
        </view>
      </scroll-view>

      <view v-if="showKeypad && canPick" class="keypad-wrap">
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

      <view class="footer">
        <template v-if="canPick">
          <button class="outline-btn" :loading="fillingAll" @tap="handleFillAll">一键下单出库</button>
          <button class="primary-btn" :loading="acting" @tap="handleComplete">完成</button>
        </template>
        <button
          v-else-if="task.status === 'PICKED'"
          class="primary-btn block"
          :loading="acting"
          @tap="handleDelivered"
        >
          标记已送达
        </button>
      </view>
    </template>
  </view>
</template>

<script setup lang="ts">
import { onLoad } from '@dcloudio/uni-app'
import { computed, reactive, ref } from 'vue'
import {
  fetchWorkerTaskDetail,
  fillWorkerTaskQty,
  markWorkerTaskDelivered,
  markWorkerTaskPicked,
  startWorkerTask,
  updateWorkerTaskItem,
  type WorkerTask,
  type WorkerTaskItem,
} from '../../../api/worker'
import { useUserStore } from '../../../stores/user'

const userStore = useUserStore()
const task = ref<WorkerTask | null>(null)
const loading = ref(false)
const acting = ref(false)
const fillingAll = ref(false)
const showKeypad = ref(false)
const taskId = ref(0)
const activeItemId = ref<number | null>(null)
const draftValues = reactive<Record<number, string>>({})

const canPick = computed(() => {
  const status = task.value?.status
  return status === 'PENDING_PICK' || status === 'PICKING'
})

onLoad(async (query) => {
  if (!userStore.isLoggedIn || userStore.role !== 'WORKER') {
    uni.reLaunch({ url: '/pages/login/index' })
    return
  }
  taskId.value = Number(query?.id || 0)
  await loadTask()
})

async function loadTask() {
  if (!taskId.value) return
  loading.value = true
  try {
    task.value = await fetchWorkerTaskDetail(taskId.value)
    if (task.value.status === 'PENDING_PICK') {
      task.value = await startWorkerTask(taskId.value)
    }
    if (task.value.customerName) {
      uni.setNavigationBarTitle({ title: task.value.customerName })
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
  Object.keys(draftValues).forEach((k) => delete draftValues[Number(k)])
  for (const item of task.value?.items || []) {
    draftValues[item.id] = String(
      item.shortageFlag === 1 ? 0 : (item.actualQty ?? item.orderQty ?? ''),
    )
  }
}

function displayActual(item: WorkerTaskItem) {
  if (activeItemId.value === item.id) {
    return draftValues[item.id] ?? ''
  }
  if (item.shortageFlag === 1) return '0'
  const val = item.actualQty ?? item.orderQty
  return val != null ? String(val) : ''
}

function focusItem(itemId: number) {
  if (!canPick.value) return
  activeItemId.value = itemId
  showKeypad.value = true
  const item = task.value?.items?.find((i) => i.id === itemId)
  if (!item) return
  if (!draftValues[itemId]) {
    draftValues[itemId] = String(item.actualQty ?? item.orderQty ?? '')
  }
}

function inputKey(key: string) {
  if (activeItemId.value == null) return
  const itemId = activeItemId.value
  let val = draftValues[itemId] ?? ''
  if (key === '.' && val.includes('.')) return
  if (val === '0' && key !== '.') {
    val = key
  } else {
    val += key
  }
  draftValues[itemId] = val
}

function backspace() {
  if (activeItemId.value == null) return
  const itemId = activeItemId.value
  draftValues[itemId] = (draftValues[itemId] ?? '').slice(0, -1)
}

async function confirmField() {
  if (activeItemId.value == null) return
  const itemId = activeItemId.value
  const raw = draftValues[itemId] ?? ''
  let num = raw === '' ? undefined : Number(raw)
  const item = task.value?.items?.find((i) => i.id === itemId)
  if (num != null && item) {
    const maxQty = Number(item.orderQty ?? 0)
    if (num > maxQty) {
      uni.showToast({ title: `出库数不能大于下单数${maxQty}`, icon: 'none' })
      num = maxQty
      draftValues[itemId] = String(maxQty)
    }
  }
  try {
    task.value = await updateWorkerTaskItem(taskId.value, itemId, {
      actualQty: num,
      shortageFlag: 0,
    })
    initDrafts()
    moveToNextItem(itemId)
  } catch (e) {
    uni.showToast({ title: e instanceof Error ? e.message : '保存失败', icon: 'none' })
  }
}

function moveToNextItem(itemId: number) {
  const items = task.value?.items || []
  const idx = items.findIndex((i) => i.id === itemId)
  if (idx >= 0 && idx < items.length - 1) {
    focusItem(items[idx + 1].id)
  }
}

async function markShortage() {
  if (activeItemId.value == null) return
  try {
    task.value = await updateWorkerTaskItem(taskId.value, activeItemId.value, {
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
  fillingAll.value = true
  try {
    task.value = await fillWorkerTaskQty(taskId.value)
    initDrafts()
    activeItemId.value = null
    showKeypad.value = false
    uni.showToast({ title: '已按下单数出库', icon: 'success' })
  } catch (e) {
    uni.showToast({ title: e instanceof Error ? e.message : '确认失败', icon: 'none' })
  } finally {
    fillingAll.value = false
  }
}

async function handleComplete() {
  acting.value = true
  try {
    if (activeItemId.value != null) {
      await confirmField()
    }
    task.value = await markWorkerTaskPicked(taskId.value)
    uni.showToast({ title: '分拣完成', icon: 'success' })
    setTimeout(() => uni.navigateBack(), 500)
  } catch (e) {
    uni.showToast({ title: e instanceof Error ? e.message : '完成失败', icon: 'none' })
  } finally {
    acting.value = false
  }
}

async function handleDelivered() {
  acting.value = true
  try {
    await markWorkerTaskDelivered(taskId.value)
    uni.showToast({ title: '已标记送达', icon: 'success' })
    setTimeout(() => uni.navigateBack(), 500)
  } catch (e) {
    uni.showToast({ title: e instanceof Error ? e.message : '操作失败', icon: 'none' })
  } finally {
    acting.value = false
  }
}
</script>

<style scoped lang="scss">
.page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #fff;
  padding-bottom: calc(120rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
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

.key-row {
  display: flex;
  gap: 8rpx;
  margin-bottom: 8rpx;
  padding: 0 8rpx;
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

.footer {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  gap: 16rpx;
  padding: 16rpx 24rpx calc(16rpx + env(safe-area-inset-bottom));
  background: #fff;
  box-shadow: 0 -4rpx 20rpx rgba(0, 0, 0, 0.06);
  box-sizing: border-box;
}

.outline-btn,
.primary-btn {
  flex: 1;
  height: 88rpx;
  line-height: 88rpx;
  margin: 0;
  padding: 0;
  font-size: 28rpx;
  font-weight: 600;
  border-radius: 12rpx;
}

.outline-btn {
  color: #07c160;
  background: #fff;
  border: 2rpx solid #07c160;
}

.primary-btn {
  color: #fff;
  background: #07c160;
  border: none;
}

.primary-btn.block {
  flex: none;
  width: 100%;
}

.outline-btn::after,
.primary-btn::after {
  border: none;
}
</style>
