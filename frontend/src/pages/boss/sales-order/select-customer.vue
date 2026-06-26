<template>
  <view class="page boss-page">
    <view class="search-box">
      <view class="search-wrap">
        <text class="search-icon">⌕</text>
        <input
          class="search-input"
          type="text"
          :value="keyword"
          placeholder="搜索客户名称"
          confirm-type="search"
          focus
          @input="onKeywordInput"
          @confirm="loadCustomers"
        />
        <text v-if="keyword" class="search-clear" @tap="clearKeyword">×</text>
      </view>
    </view>

    <scroll-view scroll-y class="list-scroll boss-page-scroll">
      <view v-if="loading" class="state-wrap">
        <u-loading-icon text="加载中" />
      </view>

      <view v-else-if="displayList.length === 0 && !canUseTemporary" class="state-wrap">
        <text class="empty-text">没有匹配的客户</text>
        <text class="empty-hint">可新建客户，或使用输入的名称临时开单</text>
      </view>

      <view v-else class="list-block">
        <view class="section-title">所有客户</view>

        <view
          v-if="canUseTemporary"
          class="row temp"
          @tap="pickTemporary"
        >
          <text class="name">使用「{{ keyword.trim() }}」临时开单</text>
        </view>

        <view
          v-for="item in displayList"
          :key="item.id"
          class="row"
          @tap="pickCustomer(item)"
        >
          <view class="row-main">
            <text class="name">{{ item.name }}</text>
            <text v-if="hasDebt(item)" class="debt">欠款：￥{{ formatMoney(item.outstandingAmount) }}</text>
          </view>
        </view>
      </view>
    </scroll-view>

    <view class="footer-bar">
      <text class="create-btn" @tap="showCreate = true">新建客户</text>
    </view>

    <u-popup :show="showCreate" mode="bottom" round="16" @close="showCreate = false">
      <view class="form">
        <text class="form-title">新建客户</text>
        <input
          class="form-input"
          type="text"
          :value="createName"
          placeholder="客户名称（必填）"
          @input="onCreateNameInput"
        />
        <button class="form-submit" :loading="saving" @tap="submitCreate">保存并选择</button>
      </view>
    </u-popup>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { createBossCustomer, fetchBossCustomers, type CustomerItem } from '@common/api/customer'
import { useSalesOrderStore } from '@common/stores/salesOrder'
import { useUserStore } from '@common/stores/user'

const userStore = useUserStore()
const salesOrder = useSalesOrderStore()
const keyword = ref('')
const customers = ref<CustomerItem[]>([])
const loading = ref(false)
const saving = ref(false)
const showCreate = ref(false)
const createName = ref('')

const displayList = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  if (!kw) return customers.value
  return customers.value.filter((item) => {
    const hay = `${item.name} ${item.contactName || ''} ${item.phone || ''}`.toLowerCase()
    return hay.includes(kw)
  })
})

const canUseTemporary = computed(() => {
  const name = keyword.value.trim()
  if (!name) return false
  return !customers.value.some((item) => item.name === name)
})

onLoad((query) => {
  if (query?.keyword) {
    keyword.value = decodeURIComponent(String(query.keyword))
  }
  createName.value = keyword.value.trim()
})

onShow(async () => {
  if (!userStore.isLoggedIn || !userStore.isBoss) {
    uni.reLaunch({ url: '/pages/login/index' })
    return
  }
  await loadCustomers()
})

async function loadCustomers() {
  loading.value = true
  try {
    customers.value = await fetchBossCustomers(keyword.value.trim() || undefined)
  } finally {
    loading.value = false
  }
}

function onKeywordInput(e: { detail: { value: string } }) {
  keyword.value = e.detail.value
}

function clearKeyword() {
  keyword.value = ''
}

function hasDebt(item: CustomerItem) {
  return (item.outstandingAmount || 0) > 0
}

function formatMoney(value?: number) {
  return Number(value || 0).toFixed(2)
}

function pickCustomer(item: CustomerItem) {
  salesOrder.setCustomer({ id: item.id, name: item.name, temporary: false })
  uni.navigateBack()
}

function pickTemporary() {
  const name = keyword.value.trim()
  if (!name) return
  salesOrder.setTemporaryCustomer(name)
  uni.navigateBack()
}

function onCreateNameInput(e: { detail: { value: string } }) {
  createName.value = e.detail.value
}

async function submitCreate() {
  const name = createName.value.trim()
  if (!name) {
    uni.showToast({ title: '请输入客户名称', icon: 'none' })
    return
  }
  saving.value = true
  try {
    const created = await createBossCustomer({ name })
    salesOrder.setCustomer({ id: created.id, name: created.name, temporary: false })
    showCreate.value = false
    uni.navigateBack()
  } catch (e) {
    uni.showToast({ title: e instanceof Error ? e.message : '创建失败', icon: 'none' })
  } finally {
    saving.value = false
  }
}
</script>

<style scoped lang="scss">
@import '../../../styles/boss-footer.scss';

.page {
  background: #fff;
}

.search-box {
  flex-shrink: 0;
  padding: 16rpx 24rpx;
  background: #f5f6f7;
  border-bottom: 1rpx solid #eee;
}

.search-wrap {
  position: relative;
}

.search-icon {
  position: absolute;
  left: 24rpx;
  top: 50%;
  transform: translateY(-50%);
  font-size: 28rpx;
  color: #999;
}

.search-input {
  height: 72rpx;
  padding: 0 72rpx 0 64rpx;
  background: #fff;
  border-radius: 999rpx;
  font-size: 28rpx;
  color: #222;
  box-sizing: border-box;
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

.list-scroll {
  background: #fff;
}

.state-wrap {
  padding: 120rpx 40rpx;
  text-align: center;
}

.empty-text {
  display: block;
  font-size: 28rpx;
  color: #666;
}

.empty-hint {
  display: block;
  margin-top: 12rpx;
  font-size: 24rpx;
  color: #999;
}

.section-title {
  padding: 20rpx 32rpx 12rpx;
  font-size: 26rpx;
  color: #999;
  background: #f5f6f7;
}

.row {
  padding: 28rpx 32rpx;
  border-bottom: 1rpx solid #f0f0f0;
}

.row.temp .name {
  color: #07c160;
}

.row-main {
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}

.name {
  font-size: 32rpx;
  color: #222;
}

.debt {
  font-size: 26rpx;
  color: #e67e22;
}

.footer-bar {
  flex-shrink: 0;
  padding: 28rpx 24rpx calc(28rpx + env(safe-area-inset-bottom));
  background: #fff;
  border-top: 1rpx solid #eee;
  text-align: center;
}

.create-btn {
  font-size: 32rpx;
  color: #07c160;
  font-weight: 600;
}

.form {
  padding: 32rpx;
}

.form-title {
  display: block;
  margin-bottom: 24rpx;
  font-size: 34rpx;
  font-weight: 600;
}

.form-input {
  height: 80rpx;
  padding: 0 24rpx;
  background: #f5f6f7;
  border-radius: 12rpx;
  font-size: 28rpx;
  margin-bottom: 24rpx;
}

.form-submit {
  height: 88rpx;
  line-height: 88rpx;
  background: #07c160;
  color: #fff;
  font-size: 32rpx;
  border-radius: 12rpx;
  border: none;
}

.form-submit::after {
  border: none;
}
</style>
