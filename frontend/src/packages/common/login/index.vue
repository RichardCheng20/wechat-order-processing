<template>
  <view class="page">
    <view class="header">
      <text class="title">{{ entryPreview?.merchantName || '蔬菜批发' }}</text>
      <text class="subtitle">{{ entryHint }}</text>
    </view>

    <view v-if="entryPreview?.workerName" class="entry-tag">
      人员：{{ entryPreview.workerName }}
    </view>

    <!-- #ifndef H5 -->
    <view class="card">
      <u-button type="primary" text="微信登录" :loading="loading" @click="handleWechatLogin" />
    </view>
    <!-- #endif -->

    <!-- #ifdef H5 -->
    <view class="card h5-card">
      <text class="h5-title">H5 内测</text>
      <text class="h5-hint">浏览器无法使用微信登录，请用下方快捷账号进入系统。</text>
    </view>
    <!-- #endif -->

    <view v-if="showDevLogin" class="dev-section">
      <text class="dev-title">{{ devSectionTitle }}</text>
      <view class="dev-host-row">
        <text class="dev-host-label">{{ apiHostLabel }}</text>
        <u-input
          v-model="devApiHost"
          class="dev-host-input"
          :placeholder="apiHostPlaceholder"
          @blur="saveDevApiHost"
        />
      </view>
      <text class="dev-host-hint">{{ apiHostHint }}</text>
      <u-button class="dev-btn" text="老板登录" @click="loginAsOwner" />
      <u-button class="dev-btn" text="档口经理登录" @click="loginAsStallManager" />
      <u-button class="dev-btn" text="配送员A（PS000001）" @click="loginAsWorkerA" />
      <u-button class="dev-btn" text="配送员B（PS000002）" @click="loginAsWorkerB" />
      <u-button class="dev-btn" text="客户A（微信用户1）" @click="loginAsCustomerA" />
      <u-button class="dev-btn" text="客户B（微信用户2）" @click="loginAsCustomerB" />
      <u-button class="dev-btn" text="客户C（微信用户3）" @click="loginAsCustomerC" />
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { fetchLoginEntryPreview, type LoginEntryPreview } from '@common/api/auth'
import { useUserStore, type UserRole } from '@common/stores/user'
import {
  SHOW_DEV_LOGIN,
  USE_WECHAT_LOGIN,
  getApiBaseUrl,
  getDevApiHostOverride,
  setDevApiHostOverride,
} from '@common/utils/config'
import { applyEntryQuery, getEntryContext } from '@common/utils/tenant'

const userStore = useUserStore()
const loading = ref(false)
const showDevLogin = SHOW_DEV_LOGIN
const devApiHost = ref('')
const entryPreview = ref<LoginEntryPreview | null>(null)

// #ifdef H5
const apiBaseUrl = computed(() => getApiBaseUrl() || `${window.location.origin}（代理 /api）`)
const devSectionTitle = '快捷登录（内测）'
const apiHostLabel = '后端地址'
const apiHostPlaceholder = '留空=本机代理；或填 https://api.example.com'
const apiHostHint = computed(() => {
  const base = getApiBaseUrl()
  if (!base) {
    return `当前：${window.location.origin} 代理 → 127.0.0.1:8080 · 改地址后需重新登录`
  }
  return `当前：${base} · 改地址后需重新登录`
})
// #endif

// #ifndef H5
const apiBaseUrl = computed(() => getApiBaseUrl())
const devSectionTitle = '本地开发快捷登录（联调后端）'
const apiHostLabel = '后端 IP'
const apiHostPlaceholder = '如 172.20.10.2'
const apiHostHint = computed(() => `当前：${apiBaseUrl.value} · 换网络后改 IP 再登录`)
// #endif

const entryHint = computed(() => {
  if (entryPreview.value?.entryHint) return entryPreview.value.entryHint
  return '客户下单 · 老板确认 · 录价推送'
})

onLoad((query) => {
  applyEntryQuery(query as Record<string, string | undefined>)
  loadEntryPreview()
})

onShow(() => {
  devApiHost.value = getDevApiHostOverride()
})

async function loadEntryPreview() {
  const ctx = getEntryContext()
  try {
    entryPreview.value = await fetchLoginEntryPreview({
      merchantId: ctx.merchantId,
      activationToken: ctx.activationToken,
    })
  } catch {
    entryPreview.value = null
  }
}

function saveDevApiHost() {
  // #ifdef H5
  const trimmed = devApiHost.value.trim()
  if (!trimmed) {
    setDevApiHostOverride('')
    return
  }
  const withScheme = /^https?:\/\//.test(trimmed) ? trimmed : `http://${trimmed}`
  setDevApiHostOverride(withScheme.replace(/\/$/, ''))
  // #endif
  // #ifndef H5
  setDevApiHostOverride(devApiHost.value)
  // #endif
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

function loginAsStallManager() {
  devAs('dev-stall-manager-001', '档口经理', 'STALL_MANAGER')
}

function loginAsWorkerA() {
  devAs('dev-worker-001', '配送员A', 'WORKER')
}

function loginAsWorkerB() {
  devAs('dev-worker-002', '配送员B', 'WORKER')
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
  margin-bottom: 32rpx;
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
  line-height: 1.5;
  color: #66736b;
}

.entry-tag {
  margin-bottom: 24rpx;
  padding: 16rpx 20rpx;
  font-size: 26rpx;
  color: #07c160;
  background: #e8f8ef;
  border-radius: 12rpx;
}

.card {
  background: #fff;
  border-radius: 16rpx;
  padding: 40rpx;
}

.h5-card {
  display: flex;
  flex-direction: column;
  gap: 12rpx;
}

.h5-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #17211b;
}

.h5-hint {
  font-size: 26rpx;
  line-height: 1.5;
  color: #66736b;
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
