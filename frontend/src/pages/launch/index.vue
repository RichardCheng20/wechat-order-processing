<template>
  <view class="launch" />
</template>

<script setup lang="ts">
import { onLoad } from '@dcloudio/uni-app'
import { applyEntryQuery } from '@common/utils/tenant'

function homeByRole(role: string): string {
  switch (role) {
    case 'OWNER_ADMIN':
    case 'STALL_OWNER':
    case 'STALL_MANAGER':
      return '/pages/boss/orders/index'
    case 'WORKER':
      return '/pages/worker/tasks/index'
    default:
      return '/pages/customer/home/index'
  }
}

onLoad((query) => {
  applyEntryQuery(query as Record<string, string | undefined>)
  const jump = () => {
    try {
      const token = uni.getStorageSync('token')
      const raw = uni.getStorageSync('user_profile')
      if (token && raw) {
        const data = JSON.parse(raw as string) as { role?: string }
        if (data.role) {
          uni.reLaunch({ url: homeByRole(data.role) })
          return
        }
      }
    } catch {
      // ignore
    }
    uni.reLaunch({ url: '/pages/login/index' })
  }
  setTimeout(jump, 50)
})
</script>

<style scoped>
.launch {
  min-height: 100vh;
  background: #f5f7f3;
}
</style>
