<template>
  <view class="page">
    <view class="header">
      <text class="title">蔬菜批发</text>
      <text class="subtitle">客户下单 · 老板确认 · 录价推送</text>
    </view>

    <view class="card">
      <u-button type="primary" text="微信登录" :loading="loading" @click="handleWechatLogin" />
    </view>

    <view v-if="showDevLogin" class="dev-section">
      <text class="dev-title">本地开发快捷登录（联调后端）</text>
      <view class="dev-host-row">
        <text class="dev-host-label">后端 IP</text>
        <u-input
          v-model="devApiHost"
          class="dev-host-input"
          placeholder="如 172.20.10.2"
          @blur="saveDevApiHost"
        />
      </view>
      <text class="dev-host-hint">当前：{{ apiBaseUrl }} · 换网络后改 IP 再登录</text>
      <u-button class="dev-btn" text="老板登录" @click="loginAsOwner" />
      <u-button class="dev-btn" text="合伙人登录" @click="loginAsPartner" />
      <u-button class="dev-btn" text="客户A（微信用户1）" @click="loginAsCustomerA" />
      <u-button class="dev-btn" text="客户B（微信用户2）" @click="loginAsCustomerB" />
      <u-button class="dev-btn" text="客户C（微信用户3）" @click="loginAsCustomerC" />
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useUserStore, type UserRole } from '../../stores/user'
import {
  SHOW_DEV_LOGIN,
  USE_WECHAT_LOGIN,
  getApiBaseUrl,
  getDevApiHostOverride,
  setDevApiHostOverride,
} from '../../utils/config'

const userStore = useUserStore()
const loading = ref(false)
const showDevLogin = SHOW_DEV_LOGIN
const devApiHost = ref('')
const apiBaseUrl = computed(() => getApiBaseUrl())

onShow(() => {
  devApiHost.value = getDevApiHostOverride()
})

function saveDevApiHost() {
  setDevApiHostOverride(devApiHost.value)
}

async function handleWechatLogin() {
  saveDevApiHost()
  loading.value = true
  try {
    if (USE_WECHAT_LOGIN) {
      await userStore.loginWithWechat()
    } else if (showDevLogin) {
      await userStore.loginWithDev('dev-customer-001', '测试客户', 'CUSTOMER')
    } else {
      await userStore.loginWithWechat()
    }
    await userStore.navigateHome()
  } catch (e) {
    const msg = e instanceof Error ? e.message : '登录失败'
    uni.showToast({ title: msg, icon: 'none', duration: 3000 })
  } finally {
    loading.value = false
  }
}

async function devAs(openid: string, nickname: string, role: UserRole) {
  saveDevApiHost()
  loading.value = true
  try {
    await userStore.loginWithDev(openid, nickname, role)
    await userStore.navigateHome()
  } catch (e) {
    const msg = e instanceof Error ? e.message : '登录失败'
    uni.showToast({ title: msg, icon: 'none', duration: 3000 })
  } finally {
    loading.value = false
  }
}

function loginAsOwner() {
  devAs('dev-owner-001', '老板', 'OWNER_ADMIN')
}

function loginAsPartner() {
  devAs('dev-partner-001', '合伙人', 'PARTNER_ADMIN')
}

function loginAsCustomerA() {
  devAs('dev-customer-001', '微信用户1', 'CUSTOMER')
}

function loginAsCustomerB() {
  devAs('dev-customer-002', '微信用户2', 'CUSTOMER')
}

function loginAsCustomerC() {
  devAs('dev-customer-003', '微信用户3', 'CUSTOMER')
}
</script>

<style scoped lang="scss">
.page {
  min-height: 100vh;
  padding: 80rpx 40rpx;
}

.header {
  margin-bottom: 60rpx;
}

.title {
  display: block;
  font-size: 48rpx;
  font-weight: 600;
  color: #1a1a1a;
}

.subtitle {
  display: block;
  margin-top: 16rpx;
  font-size: 28rpx;
  color: #888;
}

.card {
  background: #fff;
  border-radius: 16rpx;
  padding: 40rpx;
}

.dev-section {
  margin-top: 48rpx;
}

.dev-title {
  display: block;
  margin-bottom: 16rpx;
  font-size: 26rpx;
  color: #999;
}

.dev-host-row {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-bottom: 8rpx;
  padding: 16rpx 20rpx;
  background: #fff;
  border-radius: 12rpx;
}

.dev-host-label {
  flex-shrink: 0;
  font-size: 26rpx;
  color: #666;
}

.dev-host-input {
  flex: 1;
}

.dev-host-hint {
  display: block;
  margin-bottom: 20rpx;
  font-size: 22rpx;
  color: #bbb;
  word-break: break-all;
}

.dev-btn {
  margin-bottom: 16rpx;
}
</style>
