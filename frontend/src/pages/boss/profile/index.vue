<template>
  <view class="page boss-page">
    <scroll-view scroll-y class="body boss-page-scroll boss-scroll-with-footer">
      <view class="section">
        <view class="section-head">
          <view class="section-bar" />
          <text class="section-title">企业信息</text>
        </view>
        <view class="form-card">
          <view class="form-row">
            <text class="label">企业名称</text>
            <input
              v-model="form.merchantName"
              class="input"
              placeholder="请输入企业名称"
              placeholder-class="placeholder"
            />
          </view>
          <picker mode="region" :value="regionPickerValue" @change="onRegionChange">
            <view class="form-row picker-row">
              <text class="label">所在地区</text>
              <view class="picker-value">
                <text :class="{ muted: !form.region }">{{ form.region || '请选择' }}</text>
                <text class="arrow">›</text>
              </view>
            </view>
          </picker>
        </view>
      </view>

      <view class="section">
        <view class="section-head">
          <view class="section-bar" />
          <text class="section-title">个人信息</text>
        </view>
        <view class="form-card">
          <view class="form-row">
            <text class="label">姓名</text>
            <input
              v-model="form.contactName"
              class="input"
              placeholder="请输入姓名"
              placeholder-class="placeholder"
            />
          </view>
          <view class="form-row">
            <text class="label">联系电话</text>
            <input
              v-model="form.phone"
              class="input"
              type="number"
              maxlength="11"
              placeholder="请输入联系电话"
              placeholder-class="placeholder"
            />
          </view>
        </view>
      </view>
    </scroll-view>

    <view class="boss-bottom-bar">
      <button class="boss-primary-btn block" :loading="saving" @tap="handleSave">保存</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { onShow } from '@dcloudio/uni-app'
import { reactive, ref } from 'vue'
import { fetchBossProfile, updateBossProfile } from '@common/api/profile'
import { useUserStore } from '@common/stores/user'

const userStore = useUserStore()
const saving = ref(false)
const regionPickerValue = ref<string[]>(['广东省', '广州市', '天河区'])

const form = reactive({
  merchantName: '',
  region: '',
  contactName: '',
  phone: '',
})

onShow(async () => {
  if (!userStore.isLoggedIn || !userStore.isBoss) {
    uni.reLaunch({ url: '/pages/login/index' })
    return
  }
  await loadProfile()
})

async function loadProfile() {
  try {
    const profile = await fetchBossProfile()
    form.merchantName = profile.merchantName || ''
    form.region = profile.region || ''
    form.contactName = profile.contactName || ''
    form.phone = profile.phone || ''
    if (profile.region) {
      const parts = profile.region.split(/\s+/).filter(Boolean)
      if (parts.length >= 3) {
        regionPickerValue.value = parts.slice(0, 3)
      }
    }
  } catch (e) {
    uni.showToast({ title: e instanceof Error ? e.message : '加载失败', icon: 'none' })
  }
}

function onRegionChange(e: { detail: { value: string[] } }) {
  const value = e.detail.value
  regionPickerValue.value = value
  form.region = value.join(' ')
}

async function handleSave() {
  if (!form.merchantName.trim()) {
    uni.showToast({ title: '请填写企业名称', icon: 'none' })
    return
  }
  if (!form.contactName.trim()) {
    uni.showToast({ title: '请填写姓名', icon: 'none' })
    return
  }
  saving.value = true
  try {
    const profile = await updateBossProfile({
      merchantName: form.merchantName.trim(),
      region: form.region.trim() || undefined,
      contactName: form.contactName.trim(),
      phone: form.phone.trim() || undefined,
    })
    userStore.syncLocalProfile(profile)
    uni.showToast({ title: '保存成功', icon: 'success' })
    setTimeout(() => uni.navigateBack(), 400)
  } catch (e) {
    uni.showToast({ title: e instanceof Error ? e.message : '保存失败', icon: 'none' })
  } finally {
    saving.value = false
  }
}
</script>

<style scoped lang="scss">
@import '../../../styles/boss-footer.scss';

.body {
  padding: 24rpx;
}

.section {
  margin-bottom: 24rpx;
}

.section-head {
  display: flex;
  align-items: center;
  margin-bottom: 16rpx;
}

.section-bar {
  width: 6rpx;
  height: 28rpx;
  border-radius: 4rpx;
  background: #07c160;
  margin-right: 12rpx;
}

.section-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #333;
}

.form-card {
  background: #fff;
  border-radius: 16rpx;
  overflow: hidden;
}

.form-row {
  display: flex;
  align-items: center;
  min-height: 96rpx;
  padding: 0 28rpx;
  border-bottom: 1rpx solid #f2f3f5;
}

.form-row:last-child {
  border-bottom: none;
}

.label {
  width: 180rpx;
  flex-shrink: 0;
  font-size: 30rpx;
  color: #333;
}

.input {
  flex: 1;
  font-size: 30rpx;
  color: #333;
  text-align: right;
}

.placeholder {
  color: #ccc;
}

.picker-row {
  width: 100%;
}

.picker-value {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  font-size: 30rpx;
  color: #333;
}

.picker-value .muted {
  color: #ccc;
}

.arrow {
  margin-left: 8rpx;
  color: #ccc;
  font-size: 32rpx;
}
</style>
