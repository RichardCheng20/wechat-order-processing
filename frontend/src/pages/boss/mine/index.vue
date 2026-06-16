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
        <text class="section-title">数据统计</text>
        <text class="section-link" @tap="showComingSoon('详细报表')">查看更多 ›</text>
      </view>
      <view class="stats-row">
        <view class="stat-box">
          <text class="stat-label">今日销售</text>
          <text class="stat-value sales">¥ {{ formatMoney(dashboard.todaySales) }}</text>
        </view>
        <view class="stat-divider" />
        <view class="stat-box">
          <text class="stat-label">今日利润</text>
          <text class="stat-value">¥ {{ formatMoney(dashboard.todayProfit) }}</text>
        </view>
      </view>
    </view>

    <view class="section-card">
      <view class="section-head">
        <text class="section-title">快捷功能</text>
      </view>
      <view class="receivable-row">
        <view class="receivable-item receivable" @tap="goUnsettledCustomers">
          <text class="receivable-label">累计应收</text>
          <text class="receivable-value">¥ {{ formatMoney(dashboard.totalReceivable) }}</text>
        </view>
        <view class="receivable-item payable">
          <text class="receivable-label">累计应付</text>
          <text class="receivable-value">¥ {{ formatMoney(dashboard.totalPayable) }}</text>
        </view>
      </view>

      <view class="grid">
        <view class="grid-item" @tap="goProducts">
          <view class="grid-icon orange">📦</view>
          <text class="grid-label">商品管理</text>
        </view>
        <view class="grid-item" @tap="goCategories">
          <view class="grid-icon teal">🗂️</view>
          <text class="grid-label">分类管理</text>
        </view>
        <view class="grid-item" @tap="showComingSoon('单位管理')">
          <view class="grid-icon pink">🏷️</view>
          <text class="grid-label">单位管理</text>
        </view>
        <view class="grid-item" @tap="goCustomers">
          <view class="grid-icon green">👥</view>
          <text class="grid-label">客户管理</text>
        </view>
        <view class="grid-item" @tap="goSuppliers">
          <view class="grid-icon purple">🏭</view>
          <text class="grid-label">供应商管理</text>
        </view>
        <view class="grid-item" @tap="showComingSoon('报价单管理')">
          <view class="grid-icon red">💰</view>
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
          @tap="showComingSoon(item.label)"
        >
          <view class="grid-icon" :class="item.color">{{ item.icon }}</view>
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
          <view class="grid-icon" :class="item.color">{{ item.icon }}</view>
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
import { fetchBossDashboard, type BossDashboard } from '../../../api/dashboard'
import { fetchBossProfile, type BossProfile } from '../../../api/profile'
import BossTabbar from '../../../components/boss-tabbar/index.vue'
import { useUserStore } from '../../../stores/user'

const userStore = useUserStore()
const dashboard = ref<BossDashboard>({
  todaySales: 0,
  todayProfit: 0,
  totalReceivable: 0,
  totalPayable: 0,
})
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
  { label: '客户报表', icon: '¥', color: 'green' },
  { label: '客户排行', icon: '📊', color: 'purple' },
  { label: '商品排行', icon: '📈', color: 'red' },
  { label: '供应商报表', icon: '¥', color: 'orange' },
  { label: '库存报表', icon: '📊', color: 'blue' },
]

const commonItems = [
  { label: '订货邀请', icon: '🛍️', color: 'green', action: 'invite' },
  { label: '同事管理', icon: '📇', color: 'blue', action: 'colleague' },
  { label: '设置', icon: '⚙️', color: 'orange', action: 'settings' },
  { label: '帮助中心', icon: '❓', color: 'red', action: 'help' },
  { label: '消息通知', icon: '🔔', color: 'orange', action: 'messages' },
  { label: '退出登录', icon: '🚪', color: 'gray', action: 'logout' },
]

onShow(async () => {
  if (!userStore.isLoggedIn || !userStore.isBoss) {
    uni.reLaunch({ url: '/pages/login/index' })
    return
  }
  try {
    const [dash, prof] = await Promise.all([fetchBossDashboard(), fetchBossProfile()])
    dashboard.value = dash
    profile.value = prof
  } catch {
    // 保留默认值
  }
})

function formatMoney(value?: number) {
  const num = Number(value || 0)
  return num.toFixed(2)
}

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

function goUnsettledCustomers() {
  uni.navigateTo({ url: '/pages/boss/customers/index?tab=unsettled' })
}

function goSuppliers() {
  uni.navigateTo({ url: '/pages/boss/suppliers/index' })
}

function showComingSoon(name: string) {
  uni.showToast({ title: `${name}即将上线`, icon: 'none' })
}

function handleCommonTap(item: { label: string; action: string }) {
  switch (item.action) {
    case 'settings':
      uni.navigateTo({ url: '/pages/boss/settings/index' })
      break
    case 'messages':
      uni.navigateTo({ url: '/pages/boss/messages/index' })
      break
    case 'logout':
      userStore.signOut()
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
  background: #f5f6f7;
}

.profile-card {
  background: #fff;
  border-radius: 20rpx;
  padding: 32rpx;
  margin-bottom: 24rpx;
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
  background: linear-gradient(135deg, #07c160, #06ad56);
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
}

.arrow {
  margin-left: 8rpx;
  color: #ccc;
  font-size: 32rpx;
}

.shop-sub {
  display: block;
  margin-top: 8rpx;
  font-size: 24rpx;
  color: #999;
}

.version-tag {
  display: inline-block;
  margin-top: 20rpx;
  padding: 6rpx 16rpx;
  font-size: 22rpx;
  color: #666;
  background: #f3f4f6;
  border-radius: 999rpx;
}

.section-card {
  background: #fff;
  border-radius: 20rpx;
  padding: 28rpx;
  margin-bottom: 24rpx;
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
}

.section-title.block {
  display: block;
  margin-bottom: 24rpx;
}

.section-link {
  font-size: 24rpx;
  color: #999;
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
  font-size: 26rpx;
  color: #888;
}

.stat-value {
  display: block;
  margin-top: 12rpx;
  font-size: 40rpx;
  font-weight: 700;
  color: #333;
}

.stat-value.sales {
  color: #07c160;
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
  margin-bottom: 28rpx;
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
  background: #fff5f5;
}

.receivable-item.payable {
  background: #fff8f0;
}

.receivable-label {
  display: block;
  font-size: 24rpx;
  color: #888;
}

.receivable-value {
  display: block;
  margin-top: 8rpx;
  font-size: 34rpx;
  font-weight: 700;
  color: #e74c3c;
}

.grid {
  display: flex;
  flex-wrap: wrap;
}

.grid.compact .grid-item {
  width: 25%;
}

.grid-item {
  width: 33.33%;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20rpx 0;
}

.grid-icon {
  width: 88rpx;
  height: 88rpx;
  border-radius: 24rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36rpx;
  background: #f5f6f7;
}

.grid-icon.green { background: #e8f8ef; }
.grid-icon.orange { background: #fff3e6; }
.grid-icon.purple { background: #f3ecff; }
.grid-icon.teal { background: #e8f7f4; }
.grid-icon.pink { background: #ffeef5; }
.grid-icon.red { background: #ffecec; }
.grid-icon.blue { background: #eef4ff; }
.grid-icon.gray { background: #f0f0f0; }

.grid-label {
  margin-top: 12rpx;
  font-size: 24rpx;
  color: #555;
  text-align: center;
}
</style>
