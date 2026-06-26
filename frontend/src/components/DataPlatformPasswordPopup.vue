<template>
  <u-popup
    :show="visible"
    mode="center"
    round="16"
    :close-on-click-overlay="false"
    @close="onClose"
  >
    <view class="panel">
      <text class="title">数据平台验证</text>
      <text class="desc">请输入 6 位数字密码后查看经营数据</text>
      <input
        v-model="passwordDraft"
        class="password-input"
        type="number"
        password
        maxlength="6"
        placeholder="请输入密码"
        :focus="visible"
        @confirm="submit"
      />
      <text v-if="popupHint" class="error">{{ popupHint }}</text>
      <view class="actions">
        <button class="btn cancel" @tap="cancel">取消</button>
        <button class="btn confirm" :loading="verifying" @tap="submit">确认</button>
      </view>
    </view>
  </u-popup>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { storeToRefs } from 'pinia'
import { useDataPlatformAccessStore } from '@common/stores/dataPlatformAccess'

const store = useDataPlatformAccessStore()
const { popupVisible, popupHint, verifying } = storeToRefs(store)
const passwordDraft = ref('')

const visible = computed(() => popupVisible.value)

watch(popupVisible, (show) => {
  if (show) {
    passwordDraft.value = ''
  }
})

function cancel() {
  store.cancelPopup()
}

function onClose() {
  if (popupVisible.value) {
    store.cancelPopup()
  }
}

async function submit() {
  store.passwordDraft = passwordDraft.value
  await store.submitPopup()
}
</script>

<style scoped lang="scss">
.panel {
  width: 620rpx;
  max-width: 86vw;
  padding: 36rpx 32rpx 28rpx;
  box-sizing: border-box;
}

.title {
  display: block;
  font-size: 34rpx;
  font-weight: 700;
  color: #111;
  text-align: center;
}

.desc {
  display: block;
  margin-top: 16rpx;
  font-size: 26rpx;
  line-height: 1.6;
  color: #666;
}

.password-input {
  margin-top: 28rpx;
  height: 88rpx;
  padding: 0 24rpx;
  font-size: 32rpx;
  background: #f5f6f8;
  border-radius: 12rpx;
}

.error {
  display: block;
  margin-top: 12rpx;
  font-size: 24rpx;
  color: #e74c3c;
}

.actions {
  display: flex;
  gap: 16rpx;
  margin-top: 28rpx;
}

.btn {
  flex: 1;
  height: 80rpx;
  line-height: 80rpx;
  margin: 0;
  padding: 0;
  font-size: 28rpx;
  border-radius: 12rpx;
}

.btn::after {
  border: none;
}

.cancel {
  background: #f3f4f6;
  color: #333;
}

.confirm {
  background: #07c160;
  color: #fff;
}
</style>
