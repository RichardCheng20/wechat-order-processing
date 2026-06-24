<template>
  <view class="page">
    <view v-if="loading" class="state-wrap">
      <u-loading-icon text="加载中" />
    </view>

    <template v-else-if="customer">
      <view class="stats-card">
        <view class="stat-item">
          <text class="stat-label">销售总额</text>
          <text class="stat-value">¥ {{ formatOrderMoney(customer.totalSalesAmount) }}</text>
        </view>
        <view class="stat-divider" />
        <view class="stat-item">
          <text class="stat-label">总欠款</text>
          <text class="stat-value debt">¥ {{ formatOrderMoney(customer.outstandingAmount) }}</text>
        </view>
      </view>

      <view class="menu-card">
        <view class="menu-item" @tap="goOrderReconcile">
          <text class="menu-label">订单对账</text>
          <text class="menu-arrow">›</text>
        </view>
        <view class="menu-item" @tap="goPaymentRecords">
          <text class="menu-label">收款记录</text>
          <text class="menu-arrow">›</text>
        </view>
        <view class="menu-item" @tap="goVouchers">
          <text class="menu-label">结款凭证</text>
          <text class="menu-arrow">›</text>
        </view>
      </view>

      <view class="settings-card">
        <view class="setting-row">
          <text class="setting-label">客户编号</text>
          <text class="setting-value">{{ customer.customerNo || customer.id }}</text>
        </view>
        <view class="setting-row">
          <text class="setting-label">结款方式</text>
          <text class="setting-value">{{ settlementLabel(customer.settlementType) }}</text>
        </view>
        <view class="setting-row">
          <text class="setting-label">联系人</text>
          <text class="setting-value">{{ customer.contactName || '—' }}</text>
        </view>
        <view class="setting-row">
          <text class="setting-label">联系电话</text>
          <text class="setting-value">{{ customer.phone || '—' }}</text>
        </view>
        <view class="setting-row">
          <text class="setting-label">简写地址</text>
          <text class="setting-value">{{ customer.addressShort || '—' }}</text>
        </view>
        <view class="setting-row">
          <text class="setting-label">绑定状态</text>
          <text class="setting-value">{{ customer.bindStatus === 'BOUND' ? '已绑定微信' : '未绑定' }}</text>
        </view>
        <view class="setting-row">
          <text class="setting-label">自动确认订单</text>
          <text class="setting-value">{{ customer.autoConfirmOrder ? '是' : '否' }}</text>
        </view>
        <view v-if="customer.remark" class="setting-row">
          <text class="setting-label">备注</text>
          <text class="setting-value">{{ customer.remark }}</text>
        </view>
      </view>
    </template>

    <view v-if="customer" class="bottom-bar">
      <view class="bottom-tool" @tap="handleDelete">
        <AppIcon name="delete" tone="gray" :size="22" :tile-size="54" :radius="14" />
        <text>删除</text>
      </view>
      <view class="bottom-tool" @tap="showEdit = true">
        <AppIcon name="pricing" tone="gray" :size="22" :tile-size="54" :radius="14" />
        <text>编辑</text>
      </view>
      <button class="outline-btn" @tap="goCollectPayment">收款</button>
      <button class="primary-btn" @tap="goSalesOrder">代下单</button>
    </view>

    <u-popup :show="showEdit" mode="bottom" round="16" @close="showEdit = false">
      <view class="form">
        <text class="form-title">编辑客户</text>
        <u-input v-model="form.name" placeholder="客户名称（必填）" />
        <u-input v-model="form.contactName" placeholder="联系人" />
        <u-input v-model="form.phone" placeholder="联系电话" />
        <u-input v-model="form.address" placeholder="完整地址" />
        <u-input v-model="form.addressShort" placeholder="简写地址" />
        <u-input v-model="form.remark" placeholder="备注" />
        <view class="switch-row">
          <text>下单后自动确认</text>
          <switch :checked="form.autoConfirmOrder" @change="onAutoConfirmChange" />
        </view>
        <u-button type="primary" text="保存" :loading="saving" @click="submitEdit" />
      </view>
    </u-popup>
  </view>
</template>

<script setup lang="ts">
import { onLoad, onShow } from '@dcloudio/uni-app'
import { reactive, ref } from 'vue'
import {
  deleteBossCustomer,
  fetchBossCustomerDetail,
  updateBossCustomer,
  type CustomerItem,
} from '@common/api/customer'
import AppIcon from '@/components/AppIcon.vue'
import { useSalesOrderStore } from '@common/stores/salesOrder'
import { useUserStore } from '@common/stores/user'
import { formatOrderMoney } from '@common/utils/order-settlement'

const userStore = useUserStore()
const salesOrder = useSalesOrderStore()
const customer = ref<CustomerItem | null>(null)
const loading = ref(false)
const saving = ref(false)
const showEdit = ref(false)
const customerId = ref(0)

const form = reactive({
  name: '',
  contactName: '',
  phone: '',
  address: '',
  addressShort: '',
  remark: '',
  autoConfirmOrder: false,
})

onLoad((query) => {
  customerId.value = Number(query?.id || 0)
})

onShow(async () => {
  if (!userStore.isLoggedIn || !userStore.isBoss) {
    uni.reLaunch({ url: '/packages/common/login/index' })
    return
  }
  await loadDetail()
})

async function loadDetail() {
  if (!customerId.value) return
  loading.value = true
  try {
    customer.value = await fetchBossCustomerDetail(customerId.value)
    uni.setNavigationBarTitle({ title: customer.value.name })
    fillForm(customer.value)
  } catch (e) {
    uni.showToast({ title: e instanceof Error ? e.message : '加载失败', icon: 'none' })
    setTimeout(() => uni.navigateBack(), 600)
  } finally {
    loading.value = false
  }
}

function fillForm(item: CustomerItem) {
  form.name = item.name
  form.contactName = item.contactName || ''
  form.phone = item.phone || ''
  form.address = item.address || ''
  form.addressShort = item.addressShort || ''
  form.remark = item.remark || ''
  form.autoConfirmOrder = !!item.autoConfirmOrder
}

function settlementLabel(type?: string) {
  const map: Record<string, string> = {
    CASH: '现结',
    DAILY: '日结',
    WEEKLY: '周结',
    MONTHLY: '月结',
    CREDIT: '先货后款',
  }
  return type ? (map[type] || type) : '现结'
}

function goOrderReconcile() {
  if (!customer.value) return
  const name = encodeURIComponent(customer.value.name)
  uni.navigateTo({
    url: `/pages/boss/customers/reconcile/index?customerId=${customer.value.id}&customerName=${name}`,
  })
}

function goPaymentRecords() {
  if (!customer.value) return
  uni.navigateTo({
    url: `/pages/boss/customers/payments/index?customerId=${customer.value.id}`,
  })
}

function goVouchers() {
  if (!customer.value) return
  const name = encodeURIComponent(customer.value.name)
  uni.navigateTo({
    url: `/pages/boss/customers/vouchers/index?customerId=${customer.value.id}&customerName=${name}`,
  })
}

function goCollectPayment() {
  if (!customer.value) return
  uni.navigateTo({
    url: `/pages/boss/sales-payment/index?customerId=${customer.value.id}&customerName=${encodeURIComponent(customer.value.name)}`,
  })
}

function goSalesOrder() {
  if (!customer.value) return
  salesOrder.reset()
  salesOrder.setCustomer({ id: customer.value.id, name: customer.value.name, temporary: false })
  uni.navigateTo({ url: '/pages/boss/sales-order/index' })
}

function handleDelete() {
  if (!customer.value) return
  uni.showModal({
    title: '确认删除',
    content: `确定删除「${customer.value.name}」？删除后不可恢复。`,
    confirmColor: '#e74c3c',
    success: async (res) => {
      if (!res.confirm || !customer.value) return
      try {
        await deleteBossCustomer(customer.value.id)
        uni.showToast({ title: '已删除', icon: 'success' })
        setTimeout(() => uni.navigateBack(), 500)
      } catch (err) {
        uni.showToast({ title: err instanceof Error ? err.message : '删除失败', icon: 'none' })
      }
    },
  })
}

function onAutoConfirmChange(e: { detail: { value: boolean } }) {
  form.autoConfirmOrder = e.detail.value
}

async function submitEdit() {
  if (!customer.value || !form.name.trim()) {
    uni.showToast({ title: '请填写客户名称', icon: 'none' })
    return
  }
  saving.value = true
  try {
    customer.value = await updateBossCustomer(customer.value.id, {
      name: form.name.trim(),
      contactName: form.contactName || undefined,
      phone: form.phone || undefined,
      address: form.address || undefined,
      addressShort: form.addressShort || undefined,
      remark: form.remark || undefined,
      autoConfirmOrder: form.autoConfirmOrder,
    })
    uni.setNavigationBarTitle({ title: customer.value.name })
    showEdit.value = false
    uni.showToast({ title: '已保存', icon: 'success' })
  } catch (e) {
    uni.showToast({ title: e instanceof Error ? e.message : '保存失败', icon: 'none' })
  } finally {
    saving.value = false
  }
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

.menu-card,
.settings-card {
  margin: 0 24rpx 24rpx;
  background: #fff;
  border-radius: 16rpx;
  overflow: hidden;
}

.menu-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 32rpx 28rpx;
  border-bottom: 1rpx solid #f0f0f0;
}

.menu-item:last-child {
  border-bottom: none;
}

.menu-label {
  font-size: 30rpx;
  color: #222;
}

.menu-arrow {
  font-size: 36rpx;
  color: #ccc;
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

.outline-btn,
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
}

.outline-btn {
  background: #fff;
  color: #07c160;
  border: 2rpx solid #07c160;
}

.primary-btn {
  background: #07c160;
  color: #fff;
}

.outline-btn::after,
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
