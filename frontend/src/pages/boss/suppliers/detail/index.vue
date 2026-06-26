<template>
  <view class="page">
    <view v-if="loading" class="state-wrap">
      <u-loading-icon text="加载中" />
    </view>

    <template v-else-if="supplier">
      <view class="stats-card">
        <view class="stat-item">
          <text class="stat-label">累计应付</text>
          <text class="stat-value">¥ {{ formatMoney(supplier.payableAmount) }}</text>
        </view>
        <view class="stat-divider" />
        <view class="stat-item">
          <text class="stat-label">未结清应付</text>
          <text class="stat-value debt">¥ {{ formatMoney(supplier.outstandingPayable) }}</text>
        </view>
      </view>

      <view class="settings-card">
        <view class="setting-row">
          <text class="setting-label">供应商编号</text>
          <text class="setting-value">{{ supplier.supplierNo || '—' }}</text>
        </view>
        <view class="setting-row">
          <text class="setting-label">联系人</text>
          <text class="setting-value">{{ supplier.contactName || '—' }}</text>
        </view>
        <view class="setting-row">
          <text class="setting-label">联系电话</text>
          <text class="setting-value">{{ supplier.phone || '—' }}</text>
        </view>
        <view class="setting-row">
          <text class="setting-label">状态</text>
          <text class="setting-value">{{ supplier.status === 0 ? '已停用' : '正常' }}</text>
        </view>
        <view v-if="supplier.remark" class="setting-row">
          <text class="setting-label">备注</text>
          <text class="setting-value">{{ supplier.remark }}</text>
        </view>
      </view>

      <view class="purchase-card">
        <view class="purchase-head">
          <text class="section-title">采购明细</text>
          <text class="purchase-hint">今日向该供应商下的货</text>
        </view>
        <view v-if="purchaseLoading" class="purchase-loading">
          <u-loading-icon size="20" />
        </view>
        <view v-else-if="purchaseLines.length === 0" class="purchase-empty">
          <text>今日暂无采购记录</text>
        </view>
        <view v-else>
          <view v-for="line in purchaseLines" :key="line.id" class="purchase-row">
            <view class="purchase-main">
              <text class="purchase-product">{{ line.productName }}</text>
              <text class="purchase-sub">¥{{ formatMoney(line.purchasePrice) }}/{{ line.unit }}</text>
            </view>
            <view class="purchase-meta">
              <text class="purchase-qty">{{ formatQty(line.purchasedQty) }}{{ line.unit }}</text>
              <text v-if="line.lineAmount != null" class="purchase-amount">¥{{ formatMoney(line.lineAmount) }}</text>
            </view>
          </view>
          <view class="purchase-summary">
            <text>合计 {{ purchaseLines.length }} 种 · ¥{{ formatMoney(purchaseTotalAmount) }}</text>
          </view>
        </view>
      </view>
    </template>

    <view v-if="supplier" class="bottom-bar">
      <view class="bottom-tool" @tap="handleDelete">
        <AppIcon name="delete" tone="gray" :size="22" :tile-size="54" :radius="14" />
        <text>删除</text>
      </view>
      <view class="bottom-tool" @tap="showEdit = true">
        <AppIcon name="pricing" tone="gray" :size="22" :tile-size="54" :radius="14" />
        <text>编辑</text>
      </view>
      <button class="primary-btn" @tap="goPurchasePayment">采购记账</button>
    </view>

    <u-popup :show="showEdit" mode="bottom" round="16" @close="showEdit = false">
      <view class="form">
        <text class="form-title">编辑供应商</text>
        <u-input v-model="form.name" placeholder="供应商名称（必填）" />
        <u-input v-model="form.contactName" placeholder="联系人" />
        <u-input v-model="form.phone" placeholder="联系电话" />
        <u-input v-model="form.remark" placeholder="备注" />
        <view class="switch-row">
          <text>启用</text>
          <switch :checked="form.enabled" @change="onEnabledChange" />
        </view>
        <u-button type="primary" text="保存" :loading="saving" @click="submitEdit" />
      </view>
    </u-popup>
  </view>
</template>

<script setup lang="ts">
import { onLoad, onShow } from '@dcloudio/uni-app'
import { computed, reactive, ref } from 'vue'
import {
  deleteBossSupplier,
  fetchBossSupplierDetail,
  fetchSupplierPurchaseLines,
  updateBossSupplier,
  type SupplierItem,
  type SupplierPurchaseLine,
} from '@common/api/supplier'
import AppIcon from '@/components/AppIcon.vue'
import { useUserStore } from '@common/stores/user'

const userStore = useUserStore()
const supplier = ref<SupplierItem | null>(null)
const loading = ref(false)
const purchaseLoading = ref(false)
const purchaseLines = ref<SupplierPurchaseLine[]>([])
const saving = ref(false)
const showEdit = ref(false)
const supplierId = ref(0)

const form = reactive({
  name: '',
  contactName: '',
  phone: '',
  remark: '',
  enabled: true,
})

const purchaseTotalAmount = computed(() =>
  purchaseLines.value.reduce((sum, line) => sum + (line.lineAmount || 0), 0),
)

function todayDateStr() {
  const d = new Date()
  const y = d.getFullYear()
  const m = `${d.getMonth() + 1}`.padStart(2, '0')
  const day = `${d.getDate()}`.padStart(2, '0')
  return `${y}-${m}-${day}`
}

onLoad((query) => {
  supplierId.value = Number(query?.id || 0)
})

onShow(async () => {
  if (!userStore.isLoggedIn || !userStore.isBoss) {
    uni.reLaunch({ url: '/pages/login/index' })
    return
  }
  await loadDetail()
  await loadPurchaseLines()
})

async function loadPurchaseLines() {
  if (!supplierId.value) return
  purchaseLoading.value = true
  try {
    const today = todayDateStr()
    purchaseLines.value = await fetchSupplierPurchaseLines(supplierId.value, {
      dateFrom: today,
      dateTo: today,
    })
  } catch {
    purchaseLines.value = []
  } finally {
    purchaseLoading.value = false
  }
}

async function loadDetail() {
  if (!supplierId.value) return
  loading.value = true
  try {
    supplier.value = await fetchBossSupplierDetail(supplierId.value)
    uni.setNavigationBarTitle({ title: supplier.value.name })
    fillForm(supplier.value)
  } catch (e) {
    uni.showToast({ title: e instanceof Error ? e.message : '加载失败', icon: 'none' })
    setTimeout(() => uni.navigateBack(), 600)
  } finally {
    loading.value = false
  }
}

function fillForm(item: SupplierItem) {
  form.name = item.name
  form.contactName = item.contactName || ''
  form.phone = item.phone || ''
  form.remark = item.remark || ''
  form.enabled = item.status !== 0
}

function goPurchasePayment() {
  if (!supplier.value) return
  uni.navigateTo({
    url: `/pages/boss/purchase-payment/index?supplierId=${supplier.value.id}&supplierName=${encodeURIComponent(supplier.value.name)}`,
  })
}

function handleDelete() {
  if (!supplier.value) return
  uni.showModal({
    title: '确认删除',
    content: `确定删除「${supplier.value.name}」？删除后不可恢复。`,
    confirmColor: '#e74c3c',
    success: async (res) => {
      if (!res.confirm || !supplier.value) return
      try {
        await deleteBossSupplier(supplier.value.id)
        uni.showToast({ title: '已删除', icon: 'success' })
        setTimeout(() => uni.navigateBack(), 500)
      } catch (err) {
        uni.showToast({ title: err instanceof Error ? err.message : '删除失败', icon: 'none' })
      }
    },
  })
}

function onEnabledChange(e: { detail: { value: boolean } }) {
  form.enabled = e.detail.value
}

async function submitEdit() {
  if (!supplier.value || !form.name.trim()) {
    uni.showToast({ title: '请填写供应商名称', icon: 'none' })
    return
  }
  saving.value = true
  try {
    supplier.value = await updateBossSupplier(supplier.value.id, {
      name: form.name.trim(),
      contactName: form.contactName || undefined,
      phone: form.phone || undefined,
      remark: form.remark || undefined,
      status: form.enabled ? 1 : 0,
    })
    uni.setNavigationBarTitle({ title: supplier.value.name })
    showEdit.value = false
    uni.showToast({ title: '已保存', icon: 'success' })
  } catch (e) {
    uni.showToast({ title: e instanceof Error ? e.message : '保存失败', icon: 'none' })
  } finally {
    saving.value = false
  }
}

function formatMoney(value?: number) {
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
  padding-bottom: calc(140rpx + env(safe-area-inset-bottom));
  background: #f5f6f7;
}

.state-wrap {
  padding: 120rpx 0;
  text-align: center;
}

.stats-card {
  display: flex;
  align-items: center;
  margin: 24rpx;
  padding: 40rpx 24rpx;
  background: #fff;
  border-radius: 16rpx;
}

.stat-item {
  flex: 1;
  text-align: center;
}

.stat-divider {
  width: 1rpx;
  height: 72rpx;
  background: #eee;
}

.stat-label {
  display: block;
  font-size: 26rpx;
  color: #999;
}

.stat-value {
  display: block;
  margin-top: 16rpx;
  font-size: 40rpx;
  font-weight: 700;
  color: #222;
}

.stat-value.debt {
  color: #e74c3c;
}

.settings-card {
  margin: 0 24rpx 24rpx;
  background: #fff;
  border-radius: 16rpx;
  overflow: hidden;
}

.setting-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24rpx;
  padding: 28rpx;
  border-bottom: 1rpx solid #f0f0f0;
}

.setting-row:last-child {
  border-bottom: none;
}

.setting-label {
  flex-shrink: 0;
  font-size: 28rpx;
  color: #999;
}

.setting-value {
  flex: 1;
  text-align: right;
  font-size: 28rpx;
  color: #333;
}

.purchase-card {
  margin: 0 24rpx 24rpx;
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
}

.purchase-head {
  margin-bottom: 8rpx;
}

.section-title {
  font-size: 28rpx;
  font-weight: 600;
  color: #333;
}

.purchase-hint {
  display: block;
  margin-top: 6rpx;
  font-size: 24rpx;
  color: #999;
}

.purchase-loading,
.purchase-empty {
  padding: 32rpx 0;
  text-align: center;
  font-size: 26rpx;
  color: #999;
}

.purchase-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20rpx 0;
  border-bottom: 1rpx solid #f0f0f0;
}

.purchase-row:last-of-type {
  border-bottom: none;
}

.purchase-product {
  display: block;
  font-size: 30rpx;
  font-weight: 600;
  color: #111;
}

.purchase-sub {
  display: block;
  margin-top: 6rpx;
  font-size: 24rpx;
  color: #888;
}

.purchase-meta {
  text-align: right;
}

.purchase-qty {
  display: block;
  font-size: 28rpx;
  font-weight: 600;
  color: #22c55e;
}

.purchase-amount {
  display: block;
  margin-top: 4rpx;
  font-size: 24rpx;
  color: #666;
}

.purchase-summary {
  margin-top: 16rpx;
  padding-top: 16rpx;
  border-top: 1rpx solid #f0f0f0;
  text-align: right;
  font-size: 26rpx;
  color: #333;
  font-weight: 600;
}

.bottom-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 80;
  display: flex;
  align-items: center;
  gap: 12rpx;
  padding: 20rpx 24rpx calc(20rpx + env(safe-area-inset-bottom));
  background: #fff;
  border-top: 1rpx solid #eee;
  box-shadow: 0 -2rpx 16rpx rgba(0, 0, 0, 0.06);
}

.bottom-tool {
  width: 88rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4rpx;
  font-size: 22rpx;
  color: #666;
}

.primary-btn {
  flex: 1;
  height: 80rpx;
  line-height: 80rpx;
  margin: 0;
  padding: 0;
  font-size: 30rpx;
  font-weight: 600;
  border-radius: 12rpx;
  border: none;
  background: #07c160;
  color: #fff;
}

.primary-btn::after {
  border: none;
}

.form {
  padding: 40rpx 32rpx 60rpx;
}

.form-title {
  display: block;
  margin-bottom: 24rpx;
  font-size: 32rpx;
  font-weight: 600;
}

.switch-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 24rpx 0;
  font-size: 28rpx;
}
</style>
