<template>
  <view class="page">
    <view v-if="loading" class="loading-wrap">
      <u-loading-icon text="加载中" />
    </view>

    <template v-else-if="note">
      <scroll-view scroll-y class="preview-scroll">
        <view class="worker-banner">
          <text class="worker-label">当前配送员</text>
          <text class="worker-code">{{ workerCodeText }}</text>
        </view>

        <view class="sheet">
          <text class="sheet-title">{{ sheetTitle }}</text>

          <view class="sheet-meta">
            <text>收货商户：{{ note.customerName }}</text>
            <text>下单时间：{{ metaDateText }}</text>
          </view>
          <view v-if="note.orderNo" class="sheet-sub-meta">
            <text>订单编号：{{ note.orderNo }}</text>
          </view>

          <view class="table">
            <view class="table-row head">
              <text class="col idx">序号</text>
              <text class="col name">商品名</text>
              <text class="col unit">单位</text>
              <text class="col qty">数量</text>
              <text class="col remark">备注</text>
            </view>
            <view v-for="row in note.rows" :key="row.index" class="table-row">
              <text class="col idx">{{ row.index }}</text>
              <text class="col name">{{ row.productName }}</text>
              <text class="col unit">{{ row.unit }}</text>
              <text class="col qty">{{ formatQuantity(row.quantity) }}</text>
              <text class="col remark">{{ row.remark }}</text>
            </view>
          </view>

          <text class="page-no">页码：1 / 1</text>
        </view>
      </scroll-view>

      <view v-if="canMarkPicked" class="footer">
        <button class="primary-btn" :loading="acting" @tap="handleMarkPicked">标记已拣单</button>
      </view>
      <view v-else-if="taskStatus" class="footer readonly">
        <text class="readonly-text">已拣单 · {{ statusLabel }}</text>
      </view>
    </template>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { fetchWorkerTaskDetail, markWorkerTaskPicked } from '@common/api/worker'
import {
  buildDeliveryNoteFromWorkerTask,
  formatQuantity,
  type DeliveryNoteData,
} from '@common/utils/delivery-note'
import { switchWorkerTab } from '@common/utils/worker-nav'
import { useUserStore } from '@common/stores/user'

const userStore = useUserStore()
const note = ref<DeliveryNoteData | null>(null)
const loading = ref(false)
const acting = ref(false)
const taskId = ref(0)
const taskStatus = ref('')
const statusLabel = ref('')

const sheetTitle = computed(() => note.value?.merchantName?.trim() || '配送单')
const metaDateText = computed(() => note.value?.orderTime || note.value?.deliveryDate || '—')
const workerCodeText = computed(() => userStore.workerCode || '未分配编号')
const canMarkPicked = computed(() => taskStatus.value === 'PENDING_PICK' || taskStatus.value === 'PICKING')

onLoad(async (query) => {
  if (!userStore.isLoggedIn || userStore.role !== 'WORKER') {
    uni.reLaunch({ url: '/packages/common/login/index' })
    return
  }
  taskId.value = Number(query?.id || 0)
  await loadTask()
})

async function loadTask() {
  if (!taskId.value) return
  loading.value = true
  try {
    const task = await fetchWorkerTaskDetail(taskId.value)
    taskStatus.value = task.status || ''
    statusLabel.value = task.statusLabel || ''
    note.value = buildDeliveryNoteFromWorkerTask(task)
    if (task.customerName) {
      uni.setNavigationBarTitle({ title: task.customerName })
    }
  } catch (e) {
    uni.showToast({ title: e instanceof Error ? e.message : '加载失败', icon: 'none' })
    setTimeout(() => uni.navigateBack(), 800)
  } finally {
    loading.value = false
  }
}

function handleMarkPicked() {
  const code = userStore.workerCode || '当前账号'
  uni.showModal({
    title: '标记已拣单',
    content: `确认由配送员 ${code} 完成装货？`,
    success: async (res) => {
      if (!res.confirm) return
      acting.value = true
      try {
        await markWorkerTaskPicked(taskId.value)
        uni.showToast({ title: '已拣单', icon: 'success' })
        setTimeout(() => switchWorkerTab('picked'), 500)
      } catch (e) {
        uni.showToast({ title: e instanceof Error ? e.message : '操作失败', icon: 'none' })
      } finally {
        acting.value = false
      }
    },
  })
}
</script>

<style scoped lang="scss">
.page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f3f5f2;
  padding-bottom: calc(120rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
}

.loading-wrap {
  padding: 120rpx 0;
  text-align: center;
}

.preview-scroll {
  flex: 1;
  height: calc(100vh - 120rpx - env(safe-area-inset-bottom));
}

.worker-banner {
  margin: 24rpx 24rpx 0;
  padding: 20rpx 28rpx;
  background: #fff;
  border-radius: 12rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.worker-label {
  font-size: 26rpx;
  color: #66736b;
}

.worker-code {
  font-size: 30rpx;
  font-weight: 700;
  color: #07c160;
}

.sheet {
  margin: 16rpx 24rpx 24rpx;
  padding: 32rpx 28rpx 40rpx;
  background: #fff;
  border-radius: 12rpx;
  box-shadow: 0 4rpx 24rpx rgba(0, 0, 0, 0.04);
}

.sheet-title {
  display: block;
  text-align: center;
  font-size: 36rpx;
  font-weight: 700;
  color: #111;
}

.sheet-meta {
  display: flex;
  justify-content: space-between;
  gap: 16rpx;
  margin-top: 28rpx;
  font-size: 24rpx;
  color: #333;
}

.sheet-sub-meta {
  margin-top: 12rpx;
  font-size: 24rpx;
  color: #666;
}

.table {
  margin-top: 24rpx;
  border: 1rpx solid #333;
}

.table-row {
  display: flex;
  border-top: 1rpx solid #333;
}

.table-row.head {
  border-top: none;
  background: #fafafa;
  font-weight: 600;
}

.col {
  padding: 16rpx 8rpx;
  font-size: 22rpx;
  color: #111;
  border-left: 1rpx solid #333;
  word-break: break-all;
}

.col:first-child {
  border-left: none;
}

.col.idx { width: 8%; text-align: center; }
.col.name { width: 28%; }
.col.unit { width: 12%; text-align: center; }
.col.qty { width: 14%; text-align: center; }
.col.remark { flex: 1; }

.page-no {
  display: block;
  margin-top: 48rpx;
  text-align: center;
  font-size: 24rpx;
  color: #666;
}

.footer {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 16rpx 24rpx calc(16rpx + env(safe-area-inset-bottom));
  background: #fff;
  box-shadow: 0 -4rpx 20rpx rgba(0, 0, 0, 0.06);
}

.primary-btn {
  width: 100%;
  height: 88rpx;
  line-height: 88rpx;
  margin: 0;
  padding: 0;
  font-size: 30rpx;
  font-weight: 600;
  color: #fff;
  background: #07c160;
  border: none;
  border-radius: 12rpx;
}

.primary-btn::after {
  border: none;
}

.footer.readonly {
  display: flex;
  align-items: center;
  justify-content: center;
}

.readonly-text {
  font-size: 28rpx;
  color: #07c160;
  font-weight: 600;
}
</style>
