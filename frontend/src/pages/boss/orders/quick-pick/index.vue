<template>
  <view class="page boss-page">
    <u-loading-icon text="跳转中…" />
  </view>
</template>

<script setup lang="ts">
import { onLoad } from '@dcloudio/uni-app'
import { useUserStore } from '@common/stores/user'

const userStore = useUserStore()

onLoad((query) => {
  if (!userStore.isLoggedIn || !userStore.isBoss) {
    uni.reLaunch({ url: '/packages/common/login/index' })
    return
  }
  const id = query?.id
  if (!id) {
    uni.navigateBack()
    return
  }
  uni.redirectTo({ url: `/pages/boss/orders/detail/index?id=${id}` })
})
</script>

<style scoped lang="scss">
.page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
