<template>
  <view class="page">
    <view class="header">
      <text class="title">绑定客户档案</text>
      <text class="subtitle">请输入老板提供的邀请码，完成绑定后即可下单</text>
    </view>

    <view class="card">
      <u-input
        v-model="inviteCode"
        placeholder="请输入8位邀请码"
        maxlength="8"
        :custom-style="{ fontSize: '36rpx', letterSpacing: '4rpx' }"
      />
      <u-button
        class="submit-btn"
        type="primary"
        text="确认绑定"
        :loading="loading"
        @click="handleBind"
      />
    </view>

    <view class="tip">
      <text>邀请码默认 7 天有效，过期请联系老板重新生成</text>
    </view>

    <view class="logout" @tap="handleLogout">退出登录</view>
  </view>
</template>

<script setup lang="ts">
import { onShow } from '@dcloudio/uni-app'
import { ref } from 'vue'
import { bindCustomerByInvite, fetchBindStatus } from '../../../api/customer'
import { useUserStore } from '../../../stores/user'

const userStore = useUserStore()
const inviteCode = ref('')
const loading = ref(false)

onShow(async () => {
  if (!userStore.isLoggedIn || !userStore.isCustomer) {
    uni.reLaunch({ url: '/pages/login/index' })
    return
  }
  if (userStore.customerId) {
    uni.redirectTo({ url: '/pages/customer/home/index' })
    return
  }
  try {
    const status = await fetchBindStatus()
    if (status.bound && status.customerId) {
      userStore.applyCustomerBind(status.customerId, status.customerName)
      uni.redirectTo({ url: '/pages/customer/home/index' })
    }
  } catch {
    // ignore
  }
})

async function handleBind() {
  const code = inviteCode.value.trim().toUpperCase()
  if (!code) {
    uni.showToast({ title: '请输入邀请码', icon: 'none' })
    return
  }
  loading.value = true
  try {
    const customer = await bindCustomerByInvite(code)
    userStore.applyCustomerBind(customer.id, customer.name)
    uni.showToast({ title: '绑定成功', icon: 'success' })
    setTimeout(() => {
      uni.redirectTo({ url: '/pages/customer/home/index' })
    }, 500)
  } catch (e) {
    uni.showToast({ title: e instanceof Error ? e.message : '绑定失败', icon: 'none', duration: 3000 })
  } finally {
    loading.value = false
  }
}

function handleLogout() {
  userStore.signOut()
}
</script>

<style scoped lang="scss">
.page {
  min-height: 100vh;
  padding: 80rpx 40rpx;
}

.header {
  margin-bottom: 48rpx;
}

.title {
  display: block;
  font-size: 44rpx;
  font-weight: 600;
}

.subtitle {
  display: block;
  margin-top: 16rpx;
  font-size: 28rpx;
  color: #888;
  line-height: 1.6;
}

.card {
  background: #fff;
  border-radius: 16rpx;
  padding: 40rpx;
}

.submit-btn {
  margin-top: 32rpx;
}

.tip {
  margin-top: 32rpx;
  text-align: center;
  font-size: 24rpx;
  color: #999;
}

.logout {
  margin-top: 80rpx;
  text-align: center;
  font-size: 26rpx;
  color: #999;
}
</style>
