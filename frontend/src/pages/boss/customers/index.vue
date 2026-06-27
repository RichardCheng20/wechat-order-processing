<template>
  <view class="page">
    <view class="top-bar">
      <view class="search-row">
        <view class="search-wrap">
          <AppIcon class="search-icon" name="search" tone="green" :tile="false" :size="18" />
          <input
            class="search-input"
            type="text"
            :value="keyword"
            placeholder="搜索客户名称/编号"
            confirm-type="search"
            @input="onKeywordInput"
            @confirm="onSearchConfirm"
          />
          <text v-if="keyword" class="search-clear" @tap="clearKeyword">×</text>
        </view>
      </view>

      <view class="tabs">
        <view
          class="tab-item"
          :class="{ active: tab === 'all' }"
          @tap="tab = 'all'"
        >全部客户</view>
        <view
          class="tab-item"
          :class="{ active: tab === 'unsettled' }"
          @tap="tab = 'unsettled'"
        >未结清</view>
      </view>

      <view class="action-row">
        <view class="action-chip primary" @tap="openRegisterInvite">邀请客户注册</view>
        <view class="action-chip" @tap="goPending">
          待审核<text v-if="pendingCount > 0" class="badge">{{ pendingCount }}</text>
        </view>
      </view>

      <view v-if="tab === 'unsettled' && !loading" class="summary-bar">
        <text>共 {{ unsettledCount }} 位，共欠款：￥{{ formatMoney(totalUnsettled) }}</text>
      </view>
    </view>

    <scroll-view
      scroll-y
      class="list-scroll"
      :class="{ 'list-scroll--unsettled': tab === 'unsettled' }"
      :show-scrollbar="false"
    >
      <view v-if="loading" class="state-wrap">
        <u-loading-icon text="加载中" />
      </view>

      <view v-else-if="displayCustomers.length === 0" class="state-wrap">
        <u-empty
          mode="list"
          :text="tab === 'unsettled' ? '暂无未结清客户' : '暂无客户档案'"
        />
      </view>

      <view v-else class="list-content">
        <view class="section-title">所有客户</view>
        <view
          v-for="item in displayCustomers"
          :key="item.id"
          class="row"
          @tap="goDetail(item.id)"
          @longpress="handleDelete(item)"
        >
          <view class="row-main">
            <text class="name">{{ item.name }}</text>
            <text v-if="item.customerNo" class="customer-no">编号 {{ item.customerNo }}</text>
            <view v-if="hasDebt(item)" class="debt-tag">
              <text>欠 ¥ {{ formatMoney(item.outstandingAmount) }}</text>
            </view>
          </view>
          <view class="row-side">
            <view
              class="invite-btn"
              :class="{ bound: item.bindStatus === 'BOUND' }"
              @tap.stop="handleInvite(item)"
            >{{ item.bindStatus === 'BOUND' ? '已绑定' : '邀请下单' }}</view>
            <text class="order-date">下单: {{ formatOrderDate(item.lastOrderAt) }}</text>
          </view>
        </view>
        <view class="list-end">—— 已经到底了 ——</view>
      </view>
    </scroll-view>

    <view class="boss-bottom-bar create-bottom-bar">
      <button class="create-bottom-btn boss-primary-btn block" @tap="showCreate = true">新建客户</button>
    </view>

    <u-popup :show="showCreate" mode="bottom" round="16" @close="showCreate = false">
      <view class="form">
        <text class="form-title">新建客户</text>
        <u-input v-model="form.name" placeholder="客户名称（必填）" />
        <u-input v-model="form.contactName" placeholder="联系人" />
        <u-input v-model="form.phone" placeholder="联系电话" />
        <u-input v-model="form.address" placeholder="完整地址（管理员可见）" />
        <u-input v-model="form.addressShort" placeholder="简写地址（如城南农贸3号门）" />
        <view class="switch-row">
          <text>下单后自动确认</text>
          <switch :checked="form.autoConfirmOrder" @change="onAutoConfirmChange" />
        </view>
        <u-button type="primary" text="保存" :loading="saving" @click="submitCreate" />
      </view>
    </u-popup>

    <u-popup :show="!!inviteInfo" mode="center" round="16" @close="inviteInfo = null">
      <view v-if="inviteInfo" class="invite-popup">
        <text class="invite-title">邀请码已生成</text>
        <text class="invite-name">{{ inviteInfo.customerName }}</text>
        <text class="invite-code">{{ inviteInfo.inviteCode }}</text>
        <text class="invite-expire">有效期至 {{ formatTime(inviteInfo.inviteExpiredAt) }}</text>
        <text class="invite-tip">请将此邀请码发给客户，在小程序绑定页输入</text>
        <u-button type="primary" text="复制邀请码" @click="copyInviteCode" />
      </view>
    </u-popup>

    <u-popup :show="!!registerInvite" mode="center" round="16" @close="registerInvite = null">
      <view v-if="registerInvite" class="invite-popup register-popup">
        <view class="mp-brand">
          <view class="mp-logo">菜</view>
          <text class="mp-name">{{ registerInvite.miniProgramName || '蔬菜批发' }}</text>
          <text class="mp-tag">微信小程序</text>
        </view>
        <text class="register-subtitle">
          {{ userStore.merchantName ? `${userStore.merchantName} 邀请客户注册` : '客户扫码打开小程序，填写资料后等待审核' }}
        </text>

        <view class="qr-frame">
          <image
            v-if="registerQrSrc"
            class="qr-image"
            :src="registerQrSrc"
            mode="aspectFit"
            show-menu-by-longpress
          />
          <view v-else class="qr-placeholder">
            <text class="qr-placeholder-title">小程序码暂未生成</text>
            <text class="qr-placeholder-tip">请确认服务器已配置微信 AppSecret；本地联调可在 application-dev-local.yml 设置 env-version: develop</text>
            <u-button size="small" type="primary" text="重新生成" :loading="registerGenerating" @click="generateRegisterInvite" />
          </view>
        </view>

        <text v-if="registerQrSrc" class="scan-hint">微信扫一扫 · 打开小程序申请成为客户</text>
        <text class="scan-hint secondary">长按二维码可保存或转发给客户</text>
        <text class="invite-expire">邀请有效期至 {{ formatTime(registerInvite.expiredAt) }}</text>

        <view class="popup-actions">
          <u-button
            v-if="registerQrSrc"
            type="primary"
            text="保存二维码到相册"
            @click="saveRegisterQr"
          />
          <u-button text="重新生成" :loading="registerGenerating" @click="generateRegisterInvite" />
        </view>

        <view class="code-toggle" @tap="showRegisterCode = !showRegisterCode">
          <text>备用：复制邀请码 {{ showRegisterCode ? '▲' : '▼' }}</text>
        </view>
        <view v-if="showRegisterCode" class="code-backup">
          <text class="invite-code small">{{ registerInvite.token }}</text>
          <u-button size="small" text="复制邀请码" @click="copyRegisterToken" />
        </view>
      </view>
    </u-popup>
  </view>
</template>

<script setup lang="ts">
import { onLoad, onShow } from '@dcloudio/uni-app'
import { computed, reactive, ref } from 'vue'
import {
  createBossCustomer,
  deleteBossCustomer,
  fetchBossCustomers,
  fetchCurrentCustomerRegisterInvite,
  fetchPendingBindRequestCount,
  generateCustomerInvite,
  generateCustomerRegisterInvite,
  type CustomerItem,
  type CustomerRegisterInviteResult,
  type InviteCodeResult,
} from '@common/api/customer'
import AppIcon from '@/components/AppIcon.vue'
import { useUserStore } from '@common/stores/user'

const userStore = useUserStore()
const customers = ref<CustomerItem[]>([])
const keyword = ref('')
const tab = ref<'all' | 'unsettled'>('all')
const loading = ref(false)
const saving = ref(false)
const showCreate = ref(false)
const inviteInfo = ref<InviteCodeResult | null>(null)
const registerInvite = ref<CustomerRegisterInviteResult | null>(null)
const registerGenerating = ref(false)
const showRegisterCode = ref(false)
const pendingCount = ref(0)
const registerQrSrc = computed(() =>
  registerInvite.value?.qrCodeBase64
    ? `data:image/png;base64,${registerInvite.value.qrCodeBase64}`
    : '',
)
const form = reactive({
  name: '',
  contactName: '',
  phone: '',
  address: '',
  addressShort: '',
  autoConfirmOrder: false,
})

const filteredByKeyword = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  if (!kw) return customers.value
  return customers.value.filter((item) => {
    const hay = `${item.name} ${item.customerNo || ''} ${item.contactName || ''} ${item.phone || ''}`.toLowerCase()
    return hay.includes(kw)
  })
})

const displayCustomers = computed(() => {
  if (tab.value === 'unsettled') {
    return filteredByKeyword.value.filter((item) => hasDebt(item))
  }
  return filteredByKeyword.value
})

const unsettledCount = computed(() =>
  customers.value.filter((item) => hasDebt(item)).length,
)

const totalUnsettled = computed(() =>
  customers.value.reduce((sum, item) => sum + (item.outstandingAmount || 0), 0),
)

onLoad((query) => {
  if (query?.tab === 'unsettled') {
    tab.value = 'unsettled'
  }
})

onShow(async () => {
  if (!userStore.isLoggedIn || !userStore.isBoss) {
    uni.reLaunch({ url: '/pages/login/index' })
    return
  }
  await Promise.all([loadData(), loadPendingCount()])
})

async function loadPendingCount() {
  try {
    const res = await fetchPendingBindRequestCount()
    pendingCount.value = res?.count || 0
  } catch {
    pendingCount.value = 0
  }
}

async function loadData() {
  loading.value = true
  try {
    customers.value = await fetchBossCustomers(keyword.value || undefined) || []
  } catch {
    customers.value = []
  } finally {
    loading.value = false
  }
}

function hasDebt(item: CustomerItem) {
  return (item.outstandingAmount || 0) > 0
}

function onKeywordInput(e: { detail: { value: string } }) {
  keyword.value = e.detail.value
}

function onSearchConfirm() {
  keyword.value = keyword.value.trim()
  loadData()
}

function clearKeyword() {
  keyword.value = ''
  loadData()
}

function onAutoConfirmChange(e: { detail: { value: boolean } }) {
  form.autoConfirmOrder = e.detail.value
}

async function submitCreate() {
  if (!form.name.trim()) {
    uni.showToast({ title: '请填写客户名称', icon: 'none' })
    return
  }
  saving.value = true
  try {
    await createBossCustomer({
      name: form.name.trim(),
      contactName: form.contactName || undefined,
      phone: form.phone || undefined,
      address: form.address || undefined,
      addressShort: form.addressShort || undefined,
      autoConfirmOrder: form.autoConfirmOrder,
    })
    showCreate.value = false
    form.name = ''
    form.contactName = ''
    form.phone = ''
    form.address = ''
    form.addressShort = ''
    form.autoConfirmOrder = false
    await loadData()
    uni.showToast({ title: '创建成功', icon: 'success' })
  } catch (e) {
    uni.showToast({ title: e instanceof Error ? e.message : '创建失败', icon: 'none' })
  } finally {
    saving.value = false
  }
}

function goDetail(id: number) {
  uni.navigateTo({ url: `/pages/boss/customers/detail/index?id=${id}` })
}

async function handleInvite(item: CustomerItem) {
  if (item.bindStatus === 'BOUND') {
    uni.showToast({ title: '客户已绑定微信', icon: 'none' })
    return
  }
  try {
    inviteInfo.value = await generateCustomerInvite(item.id)
    await loadData()
  } catch (err) {
    uni.showToast({ title: err instanceof Error ? err.message : '生成失败', icon: 'none' })
  }
}

function handleDelete(item: CustomerItem) {
  uni.showModal({
    title: '确认删除',
    content: `确定删除「${item.name}」？删除后不可恢复。`,
    confirmColor: '#e74c3c',
    success: async (res) => {
      if (!res.confirm) return
      try {
        await deleteBossCustomer(item.id)
        uni.showToast({ title: '已删除', icon: 'success' })
        await loadData()
      } catch (err) {
        uni.showToast({ title: err instanceof Error ? err.message : '删除失败', icon: 'none' })
      }
    },
  })
}

function copyInviteCode() {
  if (!inviteInfo.value) return
  uni.setClipboardData({
    data: inviteInfo.value.inviteCode,
    success: () => uni.showToast({ title: '已复制', icon: 'success' }),
  })
}

async function openRegisterInvite() {
  showRegisterCode.value = false
  try {
    const current = await fetchCurrentCustomerRegisterInvite()
    if (current?.token) {
      registerInvite.value = current
      return
    }
    await generateRegisterInvite()
  } catch (e) {
    uni.showToast({ title: e instanceof Error ? e.message : '加载失败', icon: 'none' })
  }
}

async function generateRegisterInvite() {
  registerGenerating.value = true
  try {
    registerInvite.value = await generateCustomerRegisterInvite()
    showRegisterCode.value = false
  } catch (e) {
    uni.showToast({ title: e instanceof Error ? e.message : '生成失败', icon: 'none' })
  } finally {
    registerGenerating.value = false
  }
}

function saveRegisterQr() {
  const base64 = registerInvite.value?.qrCodeBase64
  if (!base64) {
    uni.showToast({ title: '暂无二维码', icon: 'none' })
    return
  }
  // #ifdef MP-WEIXIN
  const filePath = `${wx.env.USER_DATA_PATH}/customer-register-qr.png`
  wx.getFileSystemManager().writeFile({
    filePath,
    data: base64,
    encoding: 'base64',
    success: () => {
      uni.saveImageToPhotosAlbum({
        filePath,
        success: () => uni.showToast({ title: '已保存到相册', icon: 'success' }),
        fail: () => uni.showToast({ title: '保存失败，请长按图片保存', icon: 'none' }),
      })
    },
    fail: () => uni.showToast({ title: '保存失败，请长按图片保存', icon: 'none' }),
  })
  // #endif
  // #ifndef MP-WEIXIN
  uni.showToast({ title: '请长按图片保存', icon: 'none' })
  // #endif
}

function copyRegisterToken() {
  if (!registerInvite.value) return
  uni.setClipboardData({
    data: registerInvite.value.token,
    success: () => uni.showToast({ title: '已复制邀请码', icon: 'success' }),
  })
}

function goPending() {
  uni.navigateTo({ url: '/pages/boss/customers/pending/index' })
}

function formatMoney(value?: number) {
  if (value == null || value <= 0) return '0.00'
  return Number(value).toFixed(2)
}

function formatOrderDate(value?: string) {
  if (!value) return '-'
  const date = value.replace('T', ' ').slice(0, 10)
  return date.replace(/-/g, '.')
}

function formatTime(value: string) {
  if (!value) return '—'
  return value.replace('T', ' ').slice(0, 16)
}
</script>

<style scoped lang="scss">
@import '../../../styles/boss-footer.scss';

.page {
  height: 100vh;
  overflow: hidden;
  box-sizing: border-box;
  background: #f5f6f7;
}

.top-bar {
  flex-shrink: 0;
  background: #fff;
}

.search-row {
  display: flex;
  align-items: center;
  padding: 16rpx 24rpx;
  background: #f5f6f7;
}

.search-wrap {
  position: relative;
  flex: 1;
  display: flex;
  align-items: center;
  min-width: 0;
  height: 72rpx;
  padding: 0 72rpx 0 20rpx;
  background: #fff;
  border-radius: 999rpx;
}

.search-icon {
  flex-shrink: 0;
  margin-right: 12rpx;
}

.search-input {
  flex: 1;
  height: 72rpx;
  font-size: 28rpx;
  color: #222;
}

.search-clear {
  position: absolute;
  right: 20rpx;
  top: 50%;
  transform: translateY(-50%);
  width: 40rpx;
  height: 40rpx;
  line-height: 40rpx;
  text-align: center;
  font-size: 32rpx;
  color: #bbb;
}

.create-bottom-bar {
  z-index: 80;
}

.create-bottom-btn {
  width: 100%;
}

.create-bottom-btn::after {
  border: none;
}

.tabs {
  display: flex;
  gap: 16rpx;
  margin-bottom: 16rpx;
  border-bottom: 1rpx solid #f0f0f0;
}

.tab-item {
  flex: 1;
  text-align: center;
  padding: 24rpx 0;
  font-size: 30rpx;
  color: #666;
  position: relative;
}

.tab-item.active {
  color: #07c160;
  font-weight: 600;
}

.action-row {
  display: flex;
  gap: 16rpx;
  margin-bottom: 16rpx;
}

.action-chip {
  flex: 1;
  padding: 16rpx 20rpx;
  text-align: center;
  font-size: 26rpx;
  color: #333;
  background: #fff;
  border-radius: 12rpx;
}

.action-chip.primary {
  color: #07c160;
  background: #e8f8ef;
  font-weight: 600;
}

.badge {
  margin-left: 6rpx;
  padding: 0 10rpx;
  font-size: 22rpx;
  color: #fff;
  background: #e74c3c;
  border-radius: 999rpx;
}

.tab-item.active::after {
  content: '';
  position: absolute;
  left: 50%;
  bottom: 0;
  transform: translateX(-50%);
  width: 48rpx;
  height: 6rpx;
  background: #07c160;
  border-radius: 3rpx;
}

.summary-bar {
  padding: 20rpx 24rpx;
  background: #fff8e6;
  font-size: 28rpx;
  color: #e67e22;
  font-weight: 500;
}

.list-scroll {
  height: calc(100vh - 176rpx - 128rpx - env(safe-area-inset-bottom));
  background: #f5f6f7;
  box-sizing: border-box;
}

.list-scroll--unsettled {
  height: calc(100vh - 248rpx - 128rpx - env(safe-area-inset-bottom));
}

.list-content {
  padding-bottom: 16rpx;
}

.state-wrap {
  padding: 80rpx 0;
  text-align: center;
}

.section-title {
  padding: 20rpx 24rpx 12rpx;
  font-size: 26rpx;
  color: #999;
  background: #f5f6f7;
}

.row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 28rpx 24rpx;
  border-bottom: 1rpx solid #f0f0f0;
  gap: 16rpx;
  background: #fff;
}

.row-main {
  flex: 1;
  min-width: 0;
}

.name {
  display: block;
  font-size: 32rpx;
  font-weight: 600;
  color: #222;
}

.customer-no {
  display: block;
  margin-top: 6rpx;
  font-size: 24rpx;
  color: #888;
}

.debt-tag {
  display: inline-block;
  margin-top: 10rpx;
  padding: 4rpx 14rpx;
  background: #fdecea;
  border-radius: 8rpx;
  font-size: 24rpx;
  color: #e74c3c;
}

.row-side {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 12rpx;
}

.invite-btn {
  padding: 10rpx 24rpx;
  font-size: 26rpx;
  color: #07c160;
  border: 2rpx solid #07c160;
  border-radius: 999rpx;
  background: #fff;
}

.invite-btn.bound {
  color: #999;
  border-color: #ddd;
}

.order-date {
  font-size: 22rpx;
  color: #bbb;
}

.list-end {
  padding: 24rpx 0 8rpx;
  text-align: center;
  font-size: 24rpx;
  color: #ccc;
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

.invite-popup {
  width: 560rpx;
  padding: 48rpx 40rpx;
  text-align: center;
}

.register-popup {
  width: 620rpx;
  padding: 40rpx 36rpx 48rpx;
}

.mp-brand {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8rpx;
  margin-bottom: 16rpx;
}

.mp-logo {
  width: 88rpx;
  height: 88rpx;
  line-height: 88rpx;
  border-radius: 20rpx;
  background: linear-gradient(135deg, #07c160, #05a050);
  color: #fff;
  font-size: 40rpx;
  font-weight: 700;
}

.mp-name {
  font-size: 36rpx;
  font-weight: 600;
  color: #222;
}

.mp-tag {
  font-size: 22rpx;
  color: #07c160;
  padding: 4rpx 16rpx;
  border: 1rpx solid #b7eb8f;
  border-radius: 999rpx;
  background: #f6ffed;
}

.register-subtitle {
  display: block;
  margin-bottom: 24rpx;
  font-size: 26rpx;
  color: #666;
  line-height: 1.5;
}

.qr-frame {
  padding: 24rpx;
  background: #fff;
  border: 1rpx solid #eee;
  border-radius: 16rpx;
  box-shadow: 0 8rpx 32rpx rgba(0, 0, 0, 0.06);
}

.qr-image {
  display: block;
  width: 420rpx;
  height: 420rpx;
  margin: 0 auto;
}

.qr-placeholder {
  padding: 48rpx 24rpx;
  text-align: center;
}

.qr-placeholder-title {
  display: block;
  font-size: 28rpx;
  font-weight: 600;
  color: #666;
}

.qr-placeholder-tip {
  display: block;
  margin: 16rpx 0 24rpx;
  font-size: 22rpx;
  color: #999;
  line-height: 1.6;
}

.scan-hint {
  display: block;
  margin-top: 20rpx;
  font-size: 28rpx;
  font-weight: 500;
  color: #333;
}

.scan-hint.secondary {
  margin-top: 8rpx;
  font-size: 24rpx;
  font-weight: 400;
  color: #999;
}

.code-toggle {
  margin-top: 28rpx;
  font-size: 24rpx;
  color: #999;
}

.code-backup {
  margin-top: 16rpx;
  padding-top: 16rpx;
  border-top: 1rpx dashed #eee;
}

.invite-code.small {
  margin: 0 0 16rpx;
  font-size: 36rpx;
  letter-spacing: 2rpx;
}

.invite-title {
  display: block;
  font-size: 32rpx;
  font-weight: 600;
}

.invite-name {
  display: block;
  margin-top: 16rpx;
  color: #666;
}

.invite-code {
  display: block;
  margin: 32rpx 0 16rpx;
  font-size: 48rpx;
  font-weight: 700;
  letter-spacing: 4rpx;
  color: #27ae60;
}

.invite-expire {
  display: block;
  font-size: 24rpx;
  color: #999;
}

.invite-tip {
  display: block;
  margin: 24rpx 0;
  font-size: 24rpx;
  color: #666;
}

.popup-actions {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
  margin-top: 24rpx;
}
</style>
