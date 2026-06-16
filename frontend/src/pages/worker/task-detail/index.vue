<template>
  <view class="page">
    <view v-if="loading" class="loading-wrap">
      <u-loading-icon text="加载中" />
    </view>

    <view v-else-if="task" class="content">
      <view class="header">
        <text class="customer-name">{{ task.customerName }}</text>
        <u-tag :text="task.statusLabel" :type="statusType(task.status)" size="mini" />
        <text class="meta">{{ task.deliveryAddressShort || '—' }}</text>
        <text v-if="task.remark" class="remark">备注：{{ task.remark }}</text>
      </view>

      <view class="items">
        <view v-for="item in task.items || []" :key="item.id" class="item-card">
          <view class="item-main">
            <text class="name">{{ item.productName }}</text>
            <text class="qty">下单 {{ item.orderQty }}{{ item.unit }}</text>
          </view>
          <view v-if="task.status === 'PICKING'" class="item-edit">
            <u-input
              v-model="actualQtyMap[item.id]"
              type="digit"
              placeholder="实际数量"
              @blur="saveItem(item)"
            />
            <view class="shortage-row">
              <text>缺货</text>
              <switch
                :checked="!!item.shortageFlag"
                @change="toggleShortage(item, $event)"
              />
            </view>
          </view>
          <view v-else class="item-readonly">
            <text v-if="item.actualQty != null">实际 {{ item.actualQty }}{{ item.unit }}</text>
            <text v-if="item.shortageFlag" class="shortage-tag">缺货</text>
          </view>
        </view>
      </view>

      <view class="footer">
        <u-button
          v-if="task.status === 'PENDING_PICK' || task.status === 'PICKING'"
          type="primary"
          text="一键完成分拣"
          :loading="acting"
          @click="handleCompletePick"
        />
        <u-button
          v-if="task.status === 'PENDING_PICK'"
          type="primary"
          plain
          text="开始分拣（逐项核对）"
          :loading="acting"
          @click="handleStart"
        />
        <u-button
          v-if="task.status === 'PICKING'"
          type="primary"
          plain
          text="标记已拣完"
          :loading="acting"
          @click="handlePicked"
        />
        <u-button
          v-if="task.status === 'PICKED'"
          type="success"
          text="标记已送达"
          :loading="acting"
          @click="handleDelivered"
        />
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { onLoad } from '@dcloudio/uni-app'
import { reactive, ref } from 'vue'
import {
  completeWorkerTaskPick,
  fetchWorkerTaskDetail,
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
const taskId = ref(0)
const actualQtyMap = reactive<Record<number, string>>({})

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
    for (const item of task.value.items || []) {
      actualQtyMap[item.id] = String(item.actualQty ?? item.orderQty)
    }
  } catch (e) {
    uni.showToast({ title: e instanceof Error ? e.message : '加载失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

async function handleCompletePick() {
  acting.value = true
  try {
    task.value = await completeWorkerTaskPick(taskId.value)
    uni.showToast({ title: '本单分拣已完成', icon: 'success' })
    setTimeout(() => uni.navigateBack(), 500)
  } catch (e) {
    uni.showToast({ title: e instanceof Error ? e.message : '操作失败', icon: 'none' })
  } finally {
    acting.value = false
  }
}

async function handleStart() {
  acting.value = true
  try {
    task.value = await startWorkerTask(taskId.value)
    await loadTask()
    uni.showToast({ title: '已开始分拣', icon: 'success' })
  } catch (e) {
    uni.showToast({ title: e instanceof Error ? e.message : '操作失败', icon: 'none' })
  } finally {
    acting.value = false
  }
}

async function saveItem(item: WorkerTaskItem) {
  const qty = Number(actualQtyMap[item.id])
  if (!qty || qty <= 0) return
  try {
    task.value = await updateWorkerTaskItem(taskId.value, item.id, { actualQty: qty })
  } catch {
    // ignore blur save errors
  }
}

async function toggleShortage(item: WorkerTaskItem, e: { detail: { value: boolean } }) {
  try {
    task.value = await updateWorkerTaskItem(taskId.value, item.id, {
      shortageFlag: e.detail.value ? 1 : 0,
    })
    await loadTask()
  } catch (err) {
    uni.showToast({ title: err instanceof Error ? err.message : '更新失败', icon: 'none' })
  }
}

async function handlePicked() {
  acting.value = true
  try {
    task.value = await markWorkerTaskPicked(taskId.value)
    uni.showToast({ title: '已标记拣完', icon: 'success' })
    setTimeout(() => uni.navigateBack(), 500)
  } catch (e) {
    uni.showToast({ title: e instanceof Error ? e.message : '操作失败', icon: 'none' })
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

function statusType(status: string) {
  if (status === 'PENDING_PICK') return 'warning'
  if (status === 'PICKING') return 'primary'
  if (status === 'PICKED') return 'success'
  return 'info'
}
</script>

<style scoped lang="scss">
.page {
  min-height: 100vh;
  padding-bottom: 160rpx;
}

.loading-wrap {
  padding: 80rpx 0;
  text-align: center;
}

.content {
  padding: 24rpx;
}

.header {
  background: #fff;
  border-radius: 16rpx;
  padding: 28rpx;
  margin-bottom: 24rpx;
}

.customer-name {
  display: block;
  font-size: 36rpx;
  font-weight: 600;
}

.meta {
  display: block;
  margin-top: 12rpx;
  font-size: 26rpx;
  color: #666;
}

.remark {
  display: block;
  margin-top: 12rpx;
  font-size: 26rpx;
  color: #e67e22;
}

.item-card {
  background: #fff;
  border-radius: 16rpx;
  padding: 28rpx;
  margin-bottom: 16rpx;
}

.name {
  display: block;
  font-size: 30rpx;
  font-weight: 600;
}

.qty {
  display: block;
  margin-top: 8rpx;
  font-size: 26rpx;
  color: #666;
}

.item-edit {
  margin-top: 20rpx;
}

.shortage-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 16rpx;
  font-size: 28rpx;
}

.item-readonly {
  margin-top: 16rpx;
  font-size: 26rpx;
  color: #666;
}

.shortage-tag {
  margin-left: 16rpx;
  color: #e74c3c;
}

.footer {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 24rpx 32rpx calc(24rpx + env(safe-area-inset-bottom));
  background: #fff;
  box-shadow: 0 -4rpx 20rpx rgba(0, 0, 0, 0.06);
}
</style>
