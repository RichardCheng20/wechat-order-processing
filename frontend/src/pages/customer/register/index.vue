<template>
  <view class="page">
    <view class="header">
      <text class="title">申请成为客户</text>
      <text class="subtitle">填写店铺信息后提交，老板审核通过即可下单</text>
    </view>

    <view v-if="loading" class="state-wrap">
      <u-loading-icon text="加载中" />
    </view>

    <view v-else-if="status?.pendingReview" class="card pending-card">
      <text class="pending-title">已提交，等待老板审核</text>
      <text class="pending-meta">提交时间：{{ formatTime(status.submittedAt) }}</text>
      <text class="pending-tip">审核通过后会自动进入下单页，请稍后再试</text>
      <u-button class="mt" text="刷新状态" @click="refreshStatus" />
    </view>

    <view v-else class="card">
      <view v-if="status?.lastRequestStatus === 'REJECTED'" class="reject-box">
        <text class="reject-title">上次申请未通过</text>
        <text v-if="status.rejectReason" class="reject-reason">{{ status.rejectReason }}</text>
        <text class="reject-tip">可修改信息后重新提交</text>
      </view>

      <u-input v-model="form.shopName" placeholder="客户/店铺名称（必填）" />
      <u-input v-model="form.contactName" placeholder="联系人" />
      <u-input v-model="form.phone" placeholder="联系电话" />
      <u-input v-model="form.address" placeholder="完整地址（选填）" />
      <u-input v-model="form.addressShort" placeholder="简写地址（如城南农贸3号门）" />

      <u-button
        class="submit-btn"
        type="primary"
        text="提交申请"
        :loading="submitting"
        @click="handleSubmit"
      />
    </view>

    <view class="footer-link" @tap="goShop">暂不申请，先逛逛商品</view>
  </view>
</template>

<script setup lang="ts">
import { onShow } from '@dcloudio/uni-app'
import { reactive, ref } from 'vue'
import {
  fetchCustomerRegisterStatus,
  submitCustomerRegisterApply,
  type CustomerRegisterStatus,
} from '@common/api/customer'
import { useUserStore } from '@common/stores/user'
import { getEntryContext } from '@common/utils/tenant'

const userStore = useUserStore()
const loading = ref(true)
const submitting = ref(false)
const status = ref<CustomerRegisterStatus | null>(null)
const form = reactive({
  shopName: '',
  contactName: '',
  phone: '',
  address: '',
  addressShort: '',
})

onShow(async () => {
  if (!userStore.isLoggedIn || !userStore.isCustomer) {
    uni.reLaunch({ url: '/pages/login/index' })
    return
  }
  await refreshStatus()
})

async function refreshStatus() {
  loading.value = true
  try {
    status.value = await fetchCustomerRegisterStatus()
    if (status.value.bound && status.value.customerId) {
      userStore.applyCustomerBind(status.value.customerId, status.value.customerName)
      uni.redirectTo({ url: '/pages/customer/home/index' })
      return
    }
  } catch (e) {
    uni.showToast({ title: e instanceof Error ? e.message : '加载失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

async function handleSubmit() {
  const registerToken = getEntryContext().registerToken
  if (!registerToken) {
    uni.showToast({ title: '请通过老板提供的邀请二维码进入', icon: 'none' })
    return
  }
  const shopName = form.shopName.trim()
  if (!shopName) {
    uni.showToast({ title: '请填写客户名称', icon: 'none' })
    return
  }
  submitting.value = true
  try {
    await submitCustomerRegisterApply({
      registerToken,
      shopName,
      contactName: form.contactName.trim() || undefined,
      phone: form.phone.trim() || undefined,
      address: form.address.trim() || undefined,
      addressShort: form.addressShort.trim() || undefined,
    })
    uni.showToast({ title: '已提交，等待审核', icon: 'success' })
    await refreshStatus()
  } catch (e) {
    uni.showToast({ title: e instanceof Error ? e.message : '提交失败', icon: 'none', duration: 3000 })
  } finally {
    submitting.value = false
  }
}

function goShop() {
  uni.redirectTo({ url: '/pages/customer/home/index' })
}

function formatTime(value?: string) {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}
</script>

<style scoped lang="scss">
.page {
  min-height: 100vh;
  padding: 48rpx 32rpx 80rpx;
  box-sizing: border-box;
}

.header {
  margin-bottom: 32rpx;
}

.title {
  display: block;
  font-size: 44rpx;
  font-weight: 600;
}

.subtitle {
  display: block;
  margin-top: 12rpx;
  font-size: 28rpx;
  color: #888;
  line-height: 1.6;
}

.card {
  background: #fff;
  border-radius: 16rpx;
  padding: 32rpx;
}

.state-wrap {
  padding: 80rpx 0;
  text-align: center;
}

.pending-card {
  text-align: center;
}

.pending-title {
  display: block;
  font-size: 34rpx;
  font-weight: 600;
  color: #07c160;
}

.pending-meta,
.pending-tip {
  display: block;
  margin-top: 16rpx;
  font-size: 26rpx;
  color: #888;
}

.reject-box {
  margin-bottom: 24rpx;
  padding: 20rpx;
  background: #fff7f0;
  border-radius: 12rpx;
}

.reject-title {
  display: block;
  font-size: 28rpx;
  font-weight: 600;
  color: #e67e22;
}

.reject-reason,
.reject-tip {
  display: block;
  margin-top: 8rpx;
  font-size: 24rpx;
  color: #996633;
}

.submit-btn {
  margin-top: 32rpx;
}

.mt {
  margin-top: 24rpx;
}

.footer-link {
  margin-top: 48rpx;
  text-align: center;
  font-size: 28rpx;
  color: #07c160;
}
</style>
