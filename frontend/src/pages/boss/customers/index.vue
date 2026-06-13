<template>
  <view class="page">
    <view class="header">
      <text class="title">客户管理</text>
      <u-button size="small" type="primary" text="新建" @click="showCreate = true" />
    </view>

    <view class="search-box">
      <u-search
        v-model="keyword"
        placeholder="搜索客户名称/联系人/电话"
        :show-action="false"
        @search="loadData"
        @clear="loadData"
      />
    </view>

    <view v-if="loading" class="loading-wrap">
      <u-loading-icon text="加载中" />
    </view>

    <view v-else-if="customers.length === 0" class="empty-wrap">
      <u-empty mode="list" text="暂无客户档案" />
    </view>

    <view v-else class="list">
      <view v-for="item in customers" :key="item.id" class="card">
        <view class="main">
          <text class="name">{{ item.name }}</text>
          <text class="meta">{{ item.contactName || '—' }} · {{ item.phone || '无电话' }}</text>
          <text class="meta">{{ item.addressShort || item.address || '未填地址' }}</text>
        </view>
        <view class="side">
          <u-tag :text="bindStatusText(item.bindStatus)" :type="bindTagType(item.bindStatus)" size="mini" />
          <text v-if="item.autoConfirmOrder" class="tag-extra">自动确认</text>
          <view class="invite-btn" @tap="handleInvite" :data-id="item.id">生成邀请码</view>
        </view>
      </view>
    </view>

    <u-popup :show="showCreate" mode="bottom" round="16" @close="showCreate = false">
      <view class="form">
        <text class="form-title">新建客户</text>
        <u-input v-model="form.name" placeholder="客户名称（必填）" />
        <u-input v-model="form.contactName" placeholder="联系人" />
        <u-input v-model="form.phone" placeholder="联系电话" />
        <u-input v-model="form.address" placeholder="完整地址（管理员可见）" />
        <u-input v-model="form.addressShort" placeholder="简写地址（工人可见，如城南农贸3号门）" />
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
  </view>
</template>

<script setup lang="ts">
import { onShow } from '@dcloudio/uni-app'
import { reactive, ref } from 'vue'
import {
  createBossCustomer,
  fetchBossCustomers,
  generateCustomerInvite,
  type CustomerItem,
  type InviteCodeResult,
} from '../../../api/customer'
import { useUserStore } from '../../../stores/user'

const userStore = useUserStore()
const customers = ref<CustomerItem[]>([])
const keyword = ref('')
const loading = ref(false)
const saving = ref(false)
const showCreate = ref(false)
const inviteInfo = ref<InviteCodeResult | null>(null)
const form = reactive({
  name: '',
  contactName: '',
  phone: '',
  address: '',
  addressShort: '',
  autoConfirmOrder: false,
})

onShow(async () => {
  if (!userStore.isLoggedIn || !userStore.isBoss) {
    uni.reLaunch({ url: '/pages/login/index' })
    return
  }
  await loadData()
})

async function loadData() {
  loading.value = true
  try {
    customers.value = await fetchBossCustomers(keyword.value || undefined)
  } finally {
    loading.value = false
  }
}

function bindStatusText(status: string) {
  switch (status) {
    case 'BOUND': return '已绑定'
    case 'INVITED': return '已邀请'
    case 'DISABLED': return '已停用'
    default: return '未邀请'
  }
}

function bindTagType(status: string) {
  switch (status) {
    case 'BOUND': return 'success'
    case 'INVITED': return 'warning'
    default: return 'info'
  }
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

async function handleInvite(e: { currentTarget: { dataset: { id?: string | number } } }) {
  const id = Number(e.currentTarget.dataset.id)
  if (!id) return
  try {
    inviteInfo.value = await generateCustomerInvite(id)
    await loadData()
  } catch (err) {
    uni.showToast({ title: err instanceof Error ? err.message : '生成失败', icon: 'none' })
  }
}

function copyInviteCode() {
  if (!inviteInfo.value) return
  uni.setClipboardData({
    data: inviteInfo.value.inviteCode,
    success: () => uni.showToast({ title: '已复制', icon: 'success' }),
  })
}

function formatTime(value: string) {
  if (!value) return '—'
  return value.replace('T', ' ').slice(0, 16)
}
</script>

<style scoped lang="scss">
.page {
  min-height: 100vh;
  padding: 24rpx;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24rpx;
}

.title {
  font-size: 36rpx;
  font-weight: 600;
}

.search-box {
  margin-bottom: 24rpx;
}

.loading-wrap,
.empty-wrap {
  padding: 80rpx 0;
  text-align: center;
}

.card {
  display: flex;
  justify-content: space-between;
  background: #fff;
  border-radius: 16rpx;
  padding: 28rpx;
  margin-bottom: 16rpx;
}

.name {
  display: block;
  font-size: 32rpx;
  font-weight: 600;
}

.meta {
  display: block;
  margin-top: 8rpx;
  font-size: 24rpx;
  color: #666;
}

.side {
  text-align: right;
  min-width: 180rpx;
}

.tag-extra {
  display: block;
  margin-top: 8rpx;
  font-size: 22rpx;
  color: #27ae60;
}

.invite-btn {
  margin-top: 16rpx;
  font-size: 24rpx;
  color: #2979ff;
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
</style>
