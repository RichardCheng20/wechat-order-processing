<template>
  <view class="page">
    <scroll-view scroll-y class="form-scroll">
      <view class="form-body">
        <view v-if="isEdit && workerCode" class="id-card">
          <text class="id-label">配送员ID</text>
          <text class="id-value">{{ workerCode }}</text>
          <text class="id-hint">拣单后将记录此编号，订单详情可溯源</text>
        </view>

        <view class="field-block">
          <text class="field-label required">姓名 (必填)</text>
          <input
            v-model="form.name"
            class="field-input"
            :class="{ error: errors.name }"
            placeholder="请输入人员姓名"
            maxlength="50"
            @input="errors.name = ''"
          />
          <text v-if="errors.name" class="field-error">{{ errors.name }}</text>
        </view>

        <view class="field-block">
          <text class="field-label">电话号码 (必填)</text>
          <input
            v-model="form.phone"
            class="field-input"
            :class="{ error: errors.phone }"
            type="number"
            maxlength="11"
            placeholder="请输入电话号码，此号码将用于人员登录"
            @input="errors.phone = ''"
          />
          <text v-if="errors.phone" class="field-error">{{ errors.phone }}</text>
        </view>

        <view class="role-section">
          <text class="section-label">人员角色</text>
          <view
            v-for="role in PERSONNEL_ROLE_OPTIONS"
            :key="role.value"
            class="role-item"
            @tap="selectRole(role.value)"
          >
            <view class="role-check" :class="{ checked: form.jobRole === role.value }">
              <text v-if="form.jobRole === role.value" class="role-check-mark">✓</text>
            </view>
            <view class="role-main">
              <text class="role-name">{{ role.label }}</text>
              <text class="role-desc">{{ role.desc }}</text>
            </view>
          </view>
          <text v-if="errors.jobRole" class="field-error">{{ errors.jobRole }}</text>
        </view>

        <view v-if="isEdit && !bound" class="activation-section">
          <text class="section-label">微信登录激活</text>
          <text class="activation-hint">生成激活码后，让员工/管理员用微信打开登录页扫码或输入激活码完成绑定</text>
          <button class="activation-btn" :loading="activating" @tap="handleGenerateActivation">
            生成登录激活码
          </button>
          <view v-if="activationInfo" class="activation-result">
            <text class="activation-code">{{ activationInfo.token }}</text>
            <text class="activation-expire">有效期至 {{ formatTime(activationInfo.expiredAt) }}</text>
            <text class="activation-path">登录路径：{{ activationInfo.loginPath }}</text>
            <button class="copy-btn" @tap="copyActivation">复制激活码</button>
          </view>
        </view>

        <view v-if="isEdit && bound" class="bound-tip">该人员已绑定微信，无需激活码</view>

        <view v-if="isEdit" class="danger-zone">
          <view class="danger-btn" @tap="handleDisable">停用该人员</view>
        </view>
      </view>
    </scroll-view>

    <view class="footer-bar">
      <button class="save-btn" :loading="saving" :disabled="saving" @tap="handleSave">保存</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { onLoad } from '@dcloudio/uni-app'
import { computed, reactive, ref } from 'vue'
import {
  PERSONNEL_ROLE_OPTIONS,
  createPersonnel,
  disablePersonnel,
  fetchPersonnelList,
  normalizePersonnelJobRole,
  updatePersonnel,
  type PersonnelJobRole,
} from '@common/api/personnel'
import { generatePersonnelActivationToken, type ActivationTokenResult } from '@common/api/auth'
import { useUserStore } from '@common/stores/user'

const userStore = useUserStore()
const personnelId = ref(0)
const workerCode = ref('')
const bound = ref(false)
const saving = ref(false)
const activating = ref(false)
const activationInfo = ref<ActivationTokenResult | null>(null)
const form = reactive({
  name: '',
  phone: '',
  jobRole: 'DELIVERY' as PersonnelJobRole,
})
const errors = reactive({
  name: '',
  phone: '',
  jobRole: '',
})

const isEdit = computed(() => personnelId.value > 0)

onLoad(async (query) => {
  if (!userStore.isLoggedIn || !userStore.isBoss) {
    uni.reLaunch({ url: '/packages/common/login/index' })
    return
  }
  personnelId.value = Number(query?.id || 0)
  if (personnelId.value) {
    uni.setNavigationBarTitle({ title: '编辑人员' })
    await loadPersonnel()
  } else {
    uni.setNavigationBarTitle({ title: '添加人员' })
    form.jobRole = 'DELIVERY'
  }
})

async function loadPersonnel() {
  try {
    const list = await fetchPersonnelList()
    const item = list.find((entry) => entry.id === personnelId.value)
    if (!item) {
      uni.showToast({ title: '人员不存在', icon: 'none' })
      setTimeout(() => uni.navigateBack(), 600)
      return
    }
    form.name = item.name
    form.phone = item.phone || ''
    form.jobRole = normalizePersonnelJobRole(item.jobRole)
    workerCode.value = item.workerCode || ''
    bound.value = !!item.bound
  } catch (e) {
    uni.showToast({ title: e instanceof Error ? e.message : '加载失败', icon: 'none' })
  }
}

function selectRole(role: PersonnelJobRole) {
  form.jobRole = role
  errors.jobRole = ''
}

function validate() {
  errors.name = ''
  errors.phone = ''
  errors.jobRole = ''
  let ok = true

  if (!form.name.trim()) {
    errors.name = '请输入人员姓名'
    ok = false
  }
  if (!form.phone.trim()) {
    errors.phone = '请输入电话号码'
    ok = false
  } else if (!/^1\d{10}$/.test(form.phone.trim())) {
    errors.phone = '请输入11位手机号'
    ok = false
  }
  if (!form.jobRole) {
    errors.jobRole = '请选择人员角色'
    ok = false
  }
  return ok
}

async function handleSave() {
  if (saving.value || !validate()) return
  saving.value = true
  try {
    const payload = {
      name: form.name.trim(),
      phone: form.phone.trim(),
      jobRole: form.jobRole,
    }
    if (isEdit.value) {
      await updatePersonnel(personnelId.value, payload)
    } else {
      const created = await createPersonnel(payload)
      if (created.workerCode) {
        workerCode.value = created.workerCode
        personnelId.value = created.id
        uni.showModal({
          title: '已添加',
          content: `配送员ID：${created.workerCode}\n请妥善记录，拣单后将写入订单。`,
          showCancel: false,
        })
        return
      }
    }
    uni.showToast({ title: '已保存', icon: 'success' })
    setTimeout(() => uni.navigateBack(), 400)
  } catch (e) {
    uni.showToast({ title: e instanceof Error ? e.message : '保存失败', icon: 'none' })
  } finally {
    saving.value = false
  }
}

function handleDisable() {
  uni.showModal({
    title: '停用人员',
    content: `确定停用「${form.name}」？停用后将不再出现在人员列表。`,
    confirmColor: '#e53935',
    success: async (res) => {
      if (!res.confirm) return
      try {
        await disablePersonnel(personnelId.value)
        uni.showToast({ title: '已停用', icon: 'success' })
        setTimeout(() => uni.navigateBack(), 400)
      } catch (e) {
        uni.showToast({ title: e instanceof Error ? e.message : '操作失败', icon: 'none' })
      }
    },
  })
}

async function handleGenerateActivation() {
  if (!personnelId.value || activating.value) return
  activating.value = true
  try {
    activationInfo.value = await generatePersonnelActivationToken(personnelId.value)
    uni.showToast({ title: '已生成', icon: 'success' })
  } catch (e) {
    uni.showToast({ title: e instanceof Error ? e.message : '生成失败', icon: 'none' })
  } finally {
    activating.value = false
  }
}

function copyActivation() {
  if (!activationInfo.value?.token) return
  uni.setClipboardData({
    data: activationInfo.value.token,
    success: () => uni.showToast({ title: '已复制激活码', icon: 'success' }),
  })
}

function formatTime(value?: string) {
  if (!value) return '—'
  return value.replace('T', ' ').slice(0, 16)
}
</script>

<style scoped lang="scss">
.page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #fff;
}

.form-scroll {
  flex: 1;
  height: 0;
}

.form-body {
  padding: 24rpx 32rpx 32rpx;
}

.id-card {
  margin-bottom: 32rpx;
  padding: 28rpx;
  background: #ecfdf3;
  border: 1rpx solid #b7ebc6;
  border-radius: 12rpx;
}

.id-label {
  display: block;
  font-size: 26rpx;
  color: #66736b;
}

.id-value {
  display: block;
  margin-top: 8rpx;
  font-size: 40rpx;
  font-weight: 700;
  color: #07c160;
  letter-spacing: 2rpx;
}

.id-hint {
  display: block;
  margin-top: 12rpx;
  font-size: 24rpx;
  color: #888;
}

.field-block {
  margin-bottom: 36rpx;
}

.field-label {
  display: block;
  margin-bottom: 16rpx;
  font-size: 30rpx;
  color: #333;
}

.field-label.required {
  color: #e53935;
}

.field-input {
  width: 100%;
  height: 88rpx;
  padding: 0 24rpx;
  font-size: 30rpx;
  color: #222;
  background: #fff;
  border: 2rpx solid #e5e5e5;
  border-radius: 8rpx;
  box-sizing: border-box;
}

.field-input.error {
  border-color: #e53935;
}

.field-error {
  display: block;
  margin-top: 10rpx;
  font-size: 24rpx;
  color: #e53935;
}

.role-section {
  margin-top: 12rpx;
}

.section-label {
  display: block;
  margin-bottom: 20rpx;
  font-size: 30rpx;
  color: #333;
  font-weight: 600;
}

.role-item {
  display: flex;
  align-items: flex-start;
  gap: 20rpx;
  padding: 24rpx 0;
  border-bottom: 1rpx solid #f0f0f0;
}

.role-check {
  width: 40rpx;
  height: 40rpx;
  margin-top: 4rpx;
  border: 2rpx solid #ccc;
  border-radius: 8rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.role-check.checked {
  border-color: #07c160;
  background: #07c160;
}

.role-check-mark {
  color: #fff;
  font-size: 24rpx;
  font-weight: 700;
}

.role-main {
  flex: 1;
  min-width: 0;
}

.role-name {
  display: block;
  font-size: 32rpx;
  color: #222;
  font-weight: 600;
}

.role-desc {
  display: block;
  margin-top: 8rpx;
  font-size: 26rpx;
  color: #999;
  line-height: 1.4;
}

.danger-zone {
  margin-top: 48rpx;
  padding-top: 32rpx;
  border-top: 1rpx solid #f0f0f0;
}

.activation-section {
  margin-top: 40rpx;
  padding-top: 32rpx;
  border-top: 1rpx solid #f0f0f0;
}

.activation-hint {
  display: block;
  margin: 12rpx 0 20rpx;
  font-size: 24rpx;
  line-height: 1.5;
  color: #66736b;
}

.activation-btn {
  height: 84rpx;
  line-height: 84rpx;
  margin: 0;
  font-size: 28rpx;
  color: #07c160;
  background: #e8f8ef;
  border: none;
  border-radius: 12rpx;
}

.activation-btn::after {
  border: none;
}

.activation-result {
  margin-top: 20rpx;
  padding: 20rpx;
  background: #f7f8fa;
  border-radius: 12rpx;
}

.activation-code {
  display: block;
  font-size: 40rpx;
  font-weight: 700;
  letter-spacing: 4rpx;
  color: #17211b;
}

.activation-expire,
.activation-path {
  display: block;
  margin-top: 10rpx;
  font-size: 22rpx;
  color: #66736b;
  word-break: break-all;
}

.copy-btn {
  margin-top: 16rpx;
  height: 72rpx;
  line-height: 72rpx;
  font-size: 26rpx;
  color: #fff;
  background: #07c160;
  border: none;
  border-radius: 10rpx;
}

.copy-btn::after {
  border: none;
}

.bound-tip {
  margin-top: 32rpx;
  padding: 16rpx 20rpx;
  font-size: 26rpx;
  color: #07c160;
  background: #e8f8ef;
  border-radius: 10rpx;
}

.danger-btn {
  height: 88rpx;
  line-height: 88rpx;
  text-align: center;
  font-size: 30rpx;
  color: #e53935;
  border: 2rpx solid #ffcdd2;
  border-radius: 8rpx;
  background: #fff5f5;
}

.footer-bar {
  flex-shrink: 0;
  padding: 16rpx 32rpx calc(16rpx + env(safe-area-inset-bottom));
  background: #fff;
  border-top: 1rpx solid #eee;
  box-shadow: 0 -4rpx 20rpx rgba(0, 0, 0, 0.06);
}

.save-btn {
  width: 100%;
  height: 88rpx;
  line-height: 88rpx;
  margin: 0;
  padding: 0;
  font-size: 32rpx;
  font-weight: 600;
  color: #fff;
  background: #07c160;
  border: none;
  border-radius: 12rpx;
}

.save-btn::after {
  border: none;
}

.save-btn[disabled] {
  opacity: 0.6;
}
</style>
