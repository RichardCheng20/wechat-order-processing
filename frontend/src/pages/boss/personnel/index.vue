<template>
  <view class="page">
    <view v-if="loading" class="state-wrap">
      <u-loading-icon text="加载中" />
    </view>

    <view v-else-if="personnel.length === 0" class="state-wrap">
      <u-empty mode="list" text="暂无人员" />
      <text class="hint">添加档口员工，分配角色与登录手机号</text>
    </view>

    <scroll-view v-else scroll-y class="list-scroll">
      <view
        v-for="item in personnel"
        :key="item.id"
        class="row"
        @tap="goEdit(item.id)"
      >
        <view class="row-main">
          <text class="name">{{ item.name }}</text>
          <text v-if="item.workerCode" class="worker-code">配送员ID {{ item.workerCode }}</text>
          <text class="phone">{{ item.phone || '—' }}</text>
        </view>
        <view class="row-side">
          <text class="role-tag">{{ item.jobRoleLabel || '员工' }}</text>
          <text v-if="item.bound" class="bind-tag">已绑定微信</text>
          <text class="arrow">›</text>
        </view>
      </view>
      <view class="list-end">—— 已经到底了 ——</view>
    </scroll-view>

    <view class="boss-bottom-bar">
      <button class="boss-primary-btn block" @tap="goAdd">添加人员</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { onShow } from '@dcloudio/uni-app'
import { ref } from 'vue'
import { fetchPersonnelList, type PersonnelItem } from '@common/api/personnel'
import { useUserStore } from '@common/stores/user'

const userStore = useUserStore()
const personnel = ref<PersonnelItem[]>([])
const loading = ref(false)

onShow(async () => {
  if (!userStore.isLoggedIn || !userStore.isBoss) {
    uni.reLaunch({ url: '/pages/login/index' })
    return
  }
  await loadList()
})

async function loadList() {
  loading.value = true
  try {
    personnel.value = await fetchPersonnelList()
  } catch (e) {
    uni.showToast({ title: e instanceof Error ? e.message : '加载失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

function goAdd() {
  uni.navigateTo({ url: '/pages/boss/personnel/form' })
}

function goEdit(id: number) {
  uni.navigateTo({ url: `/pages/boss/personnel/form?id=${id}` })
}
</script>

<style scoped lang="scss">
@import '../../../styles/boss-footer.scss';

.page {
  min-height: 100vh;
  background: #f5f7f3;
  padding-bottom: calc(140rpx + env(safe-area-inset-bottom));
}

.state-wrap {
  padding: 160rpx 32rpx;
  text-align: center;
}

.hint {
  display: block;
  margin-top: 16rpx;
  font-size: 26rpx;
  color: #999;
}

.list-scroll {
  height: calc(100vh - 140rpx - env(safe-area-inset-bottom));
  padding: 16rpx 24rpx 0;
  box-sizing: border-box;
}

.row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
  padding: 28rpx 24rpx;
  margin-bottom: 16rpx;
  background: #fff;
  border-radius: 14rpx;
  border: 1rpx solid #dce6df;
}

.row-main {
  flex: 1;
  min-width: 0;
}

.name {
  display: block;
  font-size: 32rpx;
  font-weight: 700;
  color: #17211b;
}

.worker-code {
  display: block;
  margin-top: 6rpx;
  font-size: 24rpx;
  font-weight: 600;
  color: #07c160;
}

.phone {
  display: block;
  margin-top: 8rpx;
  font-size: 26rpx;
  color: #66736b;
}

.row-side {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8rpx;
  flex-shrink: 0;
}

.role-tag {
  font-size: 24rpx;
  color: #07c160;
  background: #ecfdf3;
  padding: 4rpx 14rpx;
  border-radius: 999rpx;
}

.bind-tag {
  font-size: 22rpx;
  color: #999;
}

.arrow {
  font-size: 36rpx;
  color: #ccc;
  line-height: 1;
}

.list-end {
  padding: 32rpx 0 48rpx;
  text-align: center;
  font-size: 24rpx;
  color: #bbb;
}
</style>
