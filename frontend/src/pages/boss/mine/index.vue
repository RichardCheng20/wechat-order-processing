<template>
  <view class="page">
    <view class="profile-card" @tap="goProfile">
      <view class="profile-main">
        <view class="avatar">{{ avatarText }}</view>
        <view class="profile-info">
          <view class="name-row">
            <text class="shop-name">{{ displayName }}</text>
            <text class="arrow">›</text>
          </view>
          <text class="shop-sub">{{ displaySub }}</text>
        </view>
      </view>
      <view class="version-tag">开单记账版</view>
    </view>

    <view class="section-card">
      <view class="section-head">
        <text class="section-title">快捷功能</text>
      </view>

      <view class="grid">
        <view class="grid-item" @tap="goProducts">
          <AppIcon class="grid-icon" name="product" tone="orange" :size="28" :tile-size="88" :radius="22" />
          <text class="grid-label">商品管理</text>
        </view>
        <view class="grid-item" @tap="goCategories">
          <AppIcon class="grid-icon" name="category" tone="teal" :size="28" :tile-size="88" :radius="22" />
          <text class="grid-label">分类管理</text>
        </view>
        <view class="grid-item" @tap="goCustomers">
          <AppIcon class="grid-icon" name="customer" tone="green" :size="28" :tile-size="88" :radius="22" />
          <text class="grid-label">客户管理</text>
        </view>
        <view class="grid-item" @tap="goSuppliers">
          <AppIcon class="grid-icon" name="supplier" tone="purple" :size="28" :tile-size="88" :radius="22" />
          <text class="grid-label">供应商管理</text>
        </view>
        <view class="grid-item" @tap="goQuotes">
          <AppIcon class="grid-icon" name="quote" tone="red" :size="28" :tile-size="88" :radius="22" />
          <text class="grid-label">报价单管理</text>
        </view>
      </view>
    </view>

    <view class="section-card">
      <text class="section-title block">数据平台</text>
      <view class="grid compact">
        <view
          v-for="item in dataPlatformItems"
          :key="item.label"
          class="grid-item"
          @tap="handleDataPlatformTap(item)"
        >
          <AppIcon class="grid-icon" :name="item.icon" :tone="item.color" :size="28" :tile-size="88" :radius="22" />
          <text class="grid-label">{{ item.label }}</text>
        </view>
      </view>
    </view>

    <view class="section-card">
      <text class="section-title block">常用功能</text>
      <view class="grid compact">
        <view
          v-for="item in commonItems"
          :key="item.label"
          class="grid-item"
          @tap="handleCommonTap(item)"
        >
          <AppIcon class="grid-icon" :name="item.icon" :tone="item.color" :size="28" :tile-size="88" :radius="22" />
          <text class="grid-label">{{ item.label }}</text>
        </view>
      </view>
    </view>

    <BossTabbar active="mine" />
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { fetchBossProfile, type BossProfile } from '../../../api/profile'
import AppIcon from '../../../components/AppIcon.vue'
import BossTabbar from '../../../components/boss-tabbar/index.vue'
import { useUserStore } from '../../../stores/user'

const userStore = useUserStore()
const profile = ref<BossProfile>({
  merchantName: '',
  contactName: '',
  region: '',
  phone: '',
})

const displayName = computed(() => profile.value.contactName || userStore.nickname || '老板')
const displaySub = computed(() => profile.value.merchantName || profile.value.region || '点击设置企业信息')
const avatarText = computed(() => (displayName.value || '店').slice(0, 1))

const dataPlatformItems = [
  { label: '销售数据', icon: 'salesPayment', color: 'teal' as const, path: '/pages/boss/sales-data/index' },
  { label: '客户报表', icon: 'report', color: 'green' as const, path: '/pages/boss/customer-report/index' },
  { label: '客户排行', icon: 'ranking', color: 'purple' as const, path: '/pages/boss/ranking/customers/index' },
  { label: '商品排行', icon: 'product', color: 'red' as const, path: '/pages/boss/ranking/products/index' },
  { label: '供应商报表', icon: 'supplier', color: 'orange' as const },
  { label: '库存报表', icon: 'inventory', color: 'blue' as const },
]

const commonItems = [
  { label: '同事管理', icon: 'colleague', color: 'blue' as const, action: 'colleague' },
  { label: '设置', icon: 'settings', color: 'orange' as const, action: 'settings' },
]

onShow(async () => {
  if (!userStore.isLoggedIn || !userStore.isBoss) {
    uni.reLaunch({ url: '/pages/login/index' })
    return
  }
  try {
    profile.value = await fetchBossProfile()
  } catch {
    // 保留默认值
  }
})

function goProfile() {
  uni.navigateTo({ url: '/pages/boss/profile/index' })
}

function goProducts() {
  uni.navigateTo({ url: '/pages/boss/products/index' })
}

function goCategories() {
  uni.navigateTo({ url: '/pages/boss/products/categories' })
}

function goCustomers() {
  uni.navigateTo({ url: '/pages/boss/customers/index' })
}

function goSuppliers() {
  uni.navigateTo({ url: '/pages/boss/suppliers/index' })
}

function goQuotes() {
  uni.navigateTo({ url: '/pages/boss/quotes/index' })
}

function showComingSoon(name: string) {
  uni.showToast({ title: `${name}即将上线`, icon: 'none' })
}

function handleDataPlatformTap(item: { label: string; path?: string }) {
  if (item.path) {
    uni.navigateTo({ url: item.path })
    return
  }
  showComingSoon(item.label)
}

function handleCommonTap(item: { label: string; action: string }) {
  switch (item.action) {
    case 'settings':
      uni.navigateTo({ url: '/pages/boss/settings/index' })
      break
    default:
      showComingSoon(item.label)
      break
  }
}
</script>

<style scoped lang="scss">
.page {
  min-height: 100vh;
  padding: 24rpx 24rpx calc(140rpx + env(safe-area-inset-bottom));
  background: #f5f7f3;
}

.profile-card {
  background: #fff;
  border-radius: 16rpx;
  padding: 32rpx;
  margin-bottom: 24rpx;
  border: 1rpx solid #dce6df;
}

.profile-card:active {
  opacity: 0.92;
}

.profile-main {
  display: flex;
  align-items: center;
}

.avatar {
  width: 96rpx;
  height: 96rpx;
  border-radius: 50%;
  background: #0b7f3a;
  color: #fff;
  font-size: 40rpx;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}

.profile-info {
  flex: 1;
  margin-left: 24rpx;
}

.name-row {
  display: flex;
  align-items: center;
}

.shop-name {
  font-size: 36rpx;
  font-weight: 700;
  color: #17211b;
}

.arrow {
  margin-left: 8rpx;
  color: #ccc;
  font-size: 32rpx;
}

.shop-sub {
  display: block;
  margin-top: 8rpx;
  font-size: 26rpx;
  color: #66736b;
}

.version-tag {
  display: inline-block;
  margin-top: 20rpx;
  padding: 6rpx 16rpx;
  font-size: 22rpx;
  color: #66736b;
  background: #eef2ed;
  border-radius: 999rpx;
}

.section-card {
  background: #fff;
  border-radius: 16rpx;
  padding: 26rpx 24rpx;
  margin-bottom: 24rpx;
  border: 1rpx solid #dce6df;
}

.section-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24rpx;
}

.section-title {
  font-size: 32rpx;
  font-weight: 700;
  color: #17211b;
}

.section-title.block {
  display: block;
  margin-bottom: 24rpx;
}

.section-link {
  font-size: 26rpx;
  color: #0b7f3a;
}

.stats-row {
  display: flex;
  align-items: center;
}

.stat-box {
  flex: 1;
}

.stat-label {
  display: block;
  font-size: 28rpx;
  color: #66736b;
}

.stat-value {
  display: block;
  margin-top: 12rpx;
  font-size: 40rpx;
  font-weight: 700;
  color: #17211b;
}

.stat-value.sales {
  color: #0b7f3a;
}

.stat-value.profit {
  color: #17211b;
}

.stats-hint {
  display: block;
  margin-top: 16rpx;
  font-size: 24rpx;
  color: #999;
}

.stat-divider {
  width: 1rpx;
  height: 72rpx;
  background: #eee;
  margin: 0 32rpx;
}

.receivable-row {
  display: flex;
  gap: 16rpx;
  margin-bottom: 16rpx;
}

.receivable-row.single {
  margin-bottom: 28rpx;
}

.receivable-row.single .receivable-item {
  flex: 1;
}

.receivable-item {
  flex: 1;
  border-radius: 16rpx;
  padding: 24rpx;
}

.receivable-item:active {
  opacity: 0.9;
}

.receivable-item.receivable {
  background: #fff0ee;
}

.receivable-item.received {
  background: #ecfdf3;
}

.receivable-item.payable {
  background: #fff3df;
}

.receivable-label {
  display: block;
  font-size: 26rpx;
  color: #66736b;
}

.receivable-value {
  display: block;
  margin-top: 8rpx;
  font-size: 34rpx;
  font-weight: 700;
}

.receivable-value.debt {
  color: #e74c3c;
}

.receivable-value.income {
  color: #16a34a;
}

.grid {
  display: flex;
  flex-wrap: wrap;
  row-gap: 6rpx;
}

.grid.compact .grid-item {
  width: 25%;
}

.grid-item {
  width: 33.33%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-start;
  min-height: 142rpx;
  padding: 16rpx 0 18rpx;
}

.grid-icon {
  width: 88rpx;
  height: 88rpx;
  border-radius: 22rpx;
}

.grid-label {
  width: 100%;
  margin-top: 10rpx;
  font-size: 26rpx;
  color: #17211b;
  font-weight: 600;
  text-align: center;
  line-height: 1.25;
}
</style>
