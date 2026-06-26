<template>
  <view class="page">
    <view class="section">
      <text class="section-title">数据平台查看密码</text>
      <text class="section-desc">
        设置 6 位数字密码后，每次查看数据平台前都需输入密码。
      </text>

      <view v-if="loading" class="state-wrap">
        <u-loading-icon text="加载中" />
      </view>

      <template v-else>
        <view v-if="passwordEnabled" class="status-tag enabled">已启用密码保护</view>
        <view v-else class="status-tag disabled">尚未设置密码</view>

        <view v-if="passwordEnabled && !resetMode" class="field">
          <text class="label">原密码</text>
          <input v-model="oldPassword" class="input" type="number" password maxlength="6" placeholder="修改或关闭时填写" />
        </view>

        <view v-if="passwordEnabled && !resetMode" class="forgot-row">
          <text class="forgot-link" @tap="startReset">忘记原密码？</text>
        </view>

        <view v-if="resetMode" class="reset-tip">
          您已登录老板账号，可直接设置新密码，无需原密码。本应用暂未接入短信验证。
        </view>

        <view class="field">
          <text class="label">{{ resetMode ? '新密码' : passwordEnabled ? '新密码' : '设置密码' }}</text>
          <input v-model="newPassword" class="input" type="number" password maxlength="6" placeholder="6 位数字" />
        </view>

        <view class="field">
          <text class="label">确认密码</text>
          <input v-model="confirmPassword" class="input" type="number" password maxlength="6" placeholder="再次输入 6 位数字" />
        </view>

        <button class="primary-btn" :loading="saving" @tap="handleSave">
          {{ resetMode ? '重置密码' : passwordEnabled ? '修改密码' : '启用密码保护' }}
        </button>

        <button v-if="resetMode" class="ghost-btn" @tap="cancelReset">取消重置</button>

        <button v-if="passwordEnabled && !resetMode" class="danger-btn" :loading="disabling" @tap="handleDisable">
          关闭密码保护
        </button>
      </template>
    </view>
  </view>
</template>

<script setup lang="ts">
import { onShow } from '@dcloudio/uni-app'
import { ref } from 'vue'
import {
  disableDataPlatformPassword,
  fetchDataPlatformPasswordStatus,
  resetDataPlatformPassword,
  setDataPlatformPassword,
} from '@common/api/data-platform'
import { useUserStore } from '@common/stores/user'

const userStore = useUserStore()
const loading = ref(false)
const saving = ref(false)
const disabling = ref(false)
const passwordEnabled = ref(false)
const resetMode = ref(false)
const oldPassword = ref('')
const newPassword = ref('')
const confirmPassword = ref('')

onShow(async () => {
  if (!userStore.isLoggedIn || !userStore.canViewDataPlatform) {
    uni.reLaunch({ url: '/pages/login/index' })
    return
  }
  await loadStatus()
})

async function loadStatus() {
  loading.value = true
  try {
    const status = await fetchDataPlatformPasswordStatus()
    passwordEnabled.value = status.passwordEnabled
  } catch (e) {
    uni.showToast({ title: e instanceof Error ? e.message : '加载失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  const next = newPassword.value.trim()
  const confirm = confirmPassword.value.trim()
  if (!/^\d{6}$/.test(next)) {
    uni.showToast({ title: '密码必须为 6 位数字', icon: 'none' })
    return
  }
  if (next !== confirm) {
    uni.showToast({ title: '两次密码不一致', icon: 'none' })
    return
  }
  if (passwordEnabled.value && !resetMode.value && !oldPassword.value.trim()) {
    uni.showToast({ title: '请输入原密码', icon: 'none' })
    return
  }
  saving.value = true
  const wasReset = resetMode.value
  try {
    if (wasReset) {
      await resetDataPlatformPassword(next)
    } else {
      await setDataPlatformPassword(next, passwordEnabled.value ? oldPassword.value.trim() : undefined)
    }
    oldPassword.value = ''
    newPassword.value = ''
    confirmPassword.value = ''
    resetMode.value = false
    passwordEnabled.value = true
    uni.showToast({ title: wasReset ? '密码已重置' : '密码已保存', icon: 'success' })
  } catch (e) {
    uni.showToast({ title: e instanceof Error ? e.message : '保存失败', icon: 'none' })
  } finally {
    saving.value = false
  }
}

function startReset() {
  uni.showModal({
    title: '重置数据平台密码',
    content: '确认使用当前已登录的老板账号重置密码？重置后请立即设置新的 6 位数字密码。',
    confirmText: '继续重置',
    success: (res) => {
      if (!res.confirm) return
      resetMode.value = true
      oldPassword.value = ''
      newPassword.value = ''
      confirmPassword.value = ''
    },
  })
}

function cancelReset() {
  resetMode.value = false
  newPassword.value = ''
  confirmPassword.value = ''
}

function handleDisable() {
  if (!oldPassword.value.trim()) {
    uni.showToast({ title: '请输入原密码', icon: 'none' })
    return
  }
  uni.showModal({
    title: '关闭密码保护',
    content: '关闭后查看数据平台将不再需要密码，确定继续？',
    success: async (res) => {
      if (!res.confirm) return
      disabling.value = true
      try {
        await disableDataPlatformPassword(oldPassword.value.trim())
        oldPassword.value = ''
        newPassword.value = ''
        confirmPassword.value = ''
        passwordEnabled.value = false
        uni.showToast({ title: '已关闭密码保护', icon: 'success' })
      } catch (e) {
        uni.showToast({ title: e instanceof Error ? e.message : '操作失败', icon: 'none' })
      } finally {
        disabling.value = false
      }
    },
  })
}
</script>

<style scoped lang="scss">
.page {
  min-height: 100vh;
  padding: 24rpx;
  background: #f5f7f3;
}

.section {
  background: #fff;
  border-radius: 16rpx;
  padding: 32rpx 28rpx;
}

.section-title {
  display: block;
  font-size: 32rpx;
  font-weight: 700;
  color: #111;
}

.section-desc {
  display: block;
  margin-top: 12rpx;
  font-size: 26rpx;
  line-height: 1.6;
  color: #666;
}

.state-wrap {
  padding: 60rpx 0;
  text-align: center;
}

.status-tag {
  display: inline-block;
  margin-top: 24rpx;
  padding: 8rpx 20rpx;
  border-radius: 999rpx;
  font-size: 24rpx;
}

.status-tag.enabled {
  background: #ecfdf3;
  color: #059669;
}

.status-tag.disabled {
  background: #fff7e6;
  color: #e67e22;
}

.field {
  margin-top: 28rpx;
}

.forgot-row {
  margin-top: 12rpx;
  text-align: right;
}

.forgot-link {
  font-size: 26rpx;
  color: #07c160;
}

.reset-tip {
  margin-top: 24rpx;
  padding: 20rpx 24rpx;
  font-size: 24rpx;
  line-height: 1.6;
  color: #666;
  background: #f5f6f8;
  border-radius: 12rpx;
}

.label {
  display: block;
  margin-bottom: 12rpx;
  font-size: 26rpx;
  color: #666;
}

.input {
  height: 88rpx;
  padding: 0 24rpx;
  font-size: 30rpx;
  background: #f5f6f8;
  border-radius: 12rpx;
}

.primary-btn,
.danger-btn,
.ghost-btn {
  margin-top: 32rpx;
  height: 88rpx;
  line-height: 88rpx;
  font-size: 30rpx;
  border-radius: 12rpx;
  border: none;
}

.primary-btn {
  background: #07c160;
  color: #fff;
}

.danger-btn {
  background: #fff;
  color: #e74c3c;
  border: 1rpx solid #fecaca;
}

.ghost-btn {
  background: #f3f4f6;
  color: #333;
}

.primary-btn::after,
.danger-btn::after,
.ghost-btn::after {
  border: none;
}
</style>
