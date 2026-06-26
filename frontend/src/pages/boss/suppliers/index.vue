<template>
  <view class="page">
    <view class="top-bar">
      <view class="search-row">
        <view class="search-wrap">
          <AppIcon class="search-icon" name="search" tone="green" :tile="false" :size="18" />
          <input
            class="search-input"
            type="text"
            :value="keyword"
            placeholder="搜索供应商名称/编号"
            confirm-type="search"
            @input="onKeywordInput"
            @confirm="onSearchConfirm"
          />
          <text v-if="keyword" class="search-clear" @tap="clearKeyword">×</text>
        </view>
      </view>

      <view class="tabs">
        <view
          class="tab-item"
          :class="{ active: tab === 'all' }"
          @tap="tab = 'all'"
        >全部供应商</view>
        <view
          class="tab-item"
          :class="{ active: tab === 'unsettled' }"
          @tap="tab = 'unsettled'"
        >未结清</view>
      </view>

      <view v-if="tab === 'unsettled' && !loading" class="summary-bar">
        <text>共 {{ unsettledCount }} 家，共应付：￥{{ formatMoney(totalUnsettled) }}</text>
      </view>
    </view>

    <scroll-view
      scroll-y
      class="list-scroll"
      :class="{ 'list-scroll--unsettled': tab === 'unsettled' }"
      :show-scrollbar="false"
    >
      <view v-if="loading" class="state-wrap">
        <u-loading-icon text="加载中" />
      </view>

      <view v-else-if="displaySuppliers.length === 0" class="state-wrap">
        <u-empty
          mode="list"
          :text="tab === 'unsettled' ? '暂无未结清供应商' : '暂无供应商档案'"
        />
      </view>

      <view v-else class="list-content">
        <view class="section-title">所有供应商</view>
        <view
          v-for="item in displaySuppliers"
          :key="item.id"
          class="row"
          @tap="goDetail(item.id)"
          @longpress="handleDelete(item)"
        >
          <view class="row-main">
            <text class="name">{{ item.name }}</text>
            <text v-if="item.supplierNo" class="supplier-no">编号 {{ item.supplierNo }}</text>
            <view v-if="hasPayable(item)" class="payable-tag">
              <text>应付 ¥ {{ formatMoney(item.outstandingPayable) }}</text>
            </view>
          </view>
          <view class="row-side">
            <view class="status-tag" :class="{ disabled: item.status === 0 }">
              {{ item.status === 0 ? '已停用' : '正常' }}
            </view>
            <text v-if="item.contactName || item.phone" class="contact-hint">
              {{ item.contactName || item.phone }}
            </text>
          </view>
        </view>
        <view class="list-end">—— 已经到底了 ——</view>
      </view>
    </scroll-view>

    <view class="boss-bottom-bar create-bottom-bar">
      <button class="create-bottom-btn boss-primary-btn block" @tap="showCreate = true">新建供应商</button>
    </view>

    <u-popup :show="showCreate" mode="bottom" round="16" @close="showCreate = false">
      <view class="form">
        <text class="form-title">新建供应商</text>
        <u-input v-model="form.name" placeholder="供应商名称（必填）" />
        <u-input v-model="form.contactName" placeholder="联系人" />
        <u-input v-model="form.phone" placeholder="联系电话" />
        <u-input v-model="form.remark" placeholder="备注" />
        <u-button type="primary" text="保存" :loading="saving" @click="submitCreate" />
      </view>
    </u-popup>
  </view>
</template>

<script setup lang="ts">
import { onLoad, onShow } from '@dcloudio/uni-app'
import { computed, reactive, ref } from 'vue'
import {
  createBossSupplier,
  deleteBossSupplier,
  fetchBossSuppliers,
  type SupplierItem,
} from '@common/api/supplier'
import AppIcon from '@/components/AppIcon.vue'
import { useUserStore } from '@common/stores/user'

const userStore = useUserStore()
const suppliers = ref<SupplierItem[]>([])
const keyword = ref('')
const tab = ref<'all' | 'unsettled'>('all')
const loading = ref(false)
const saving = ref(false)
const showCreate = ref(false)
const form = reactive({
  name: '',
  contactName: '',
  phone: '',
  remark: '',
})

const displaySuppliers = computed(() => {
  if (tab.value === 'unsettled') {
    return suppliers.value.filter((item) => hasPayable(item))
  }
  return suppliers.value
})

const unsettledCount = computed(() =>
  suppliers.value.filter((item) => hasPayable(item)).length,
)

const totalUnsettled = computed(() =>
  suppliers.value.reduce((sum, item) => sum + (item.outstandingPayable || 0), 0),
)

onLoad((query) => {
  if (query?.tab === 'unsettled') {
    tab.value = 'unsettled'
  }
})

onShow(async () => {
  if (!userStore.isLoggedIn || !userStore.isBoss) {
    uni.reLaunch({ url: '/pages/login/index' })
    return
  }
  await loadData()
})

async function loadData() {
  loading.value = true
  try {
    suppliers.value = await fetchBossSuppliers(keyword.value || undefined) || []
  } catch {
    suppliers.value = []
  } finally {
    loading.value = false
  }
}

function hasPayable(item: SupplierItem) {
  return (item.outstandingPayable || 0) > 0
}

function onKeywordInput(e: { detail: { value: string } }) {
  keyword.value = e.detail.value
}

function onSearchConfirm() {
  keyword.value = keyword.value.trim()
  loadData()
}

function clearKeyword() {
  keyword.value = ''
  loadData()
}

async function submitCreate() {
  if (!form.name.trim()) {
    uni.showToast({ title: '请填写供应商名称', icon: 'none' })
    return
  }
  saving.value = true
  try {
    await createBossSupplier({
      name: form.name.trim(),
      contactName: form.contactName || undefined,
      phone: form.phone || undefined,
      remark: form.remark || undefined,
    })
    showCreate.value = false
    form.name = ''
    form.contactName = ''
    form.phone = ''
    form.remark = ''
    await loadData()
    uni.showToast({ title: '创建成功', icon: 'success' })
  } catch (e) {
    uni.showToast({ title: e instanceof Error ? e.message : '创建失败', icon: 'none' })
  } finally {
    saving.value = false
  }
}

function goDetail(id: number) {
  uni.navigateTo({ url: `/pages/boss/suppliers/detail/index?id=${id}` })
}

function handleDelete(item: SupplierItem) {
  uni.showModal({
    title: '确认删除',
    content: `确定删除「${item.name}」？删除后不可恢复。`,
    confirmColor: '#e74c3c',
    success: async (res) => {
      if (!res.confirm) return
      try {
        await deleteBossSupplier(item.id)
        uni.showToast({ title: '已删除', icon: 'success' })
        await loadData()
      } catch (err) {
        uni.showToast({ title: err instanceof Error ? err.message : '删除失败', icon: 'none' })
      }
    },
  })
}

function formatMoney(value?: number) {
  if (value == null || value <= 0) return '0.00'
  return Number(value).toFixed(2)
}
</script>

<style scoped lang="scss">
@import '../../../styles/boss-footer.scss';

.page {
  height: 100vh;
  overflow: hidden;
  box-sizing: border-box;
  background: #f5f6f7;
}

.top-bar {
  flex-shrink: 0;
  background: #fff;
}

.search-row {
  display: flex;
  align-items: center;
  padding: 16rpx 24rpx;
  background: #f5f6f7;
}

.search-wrap {
  position: relative;
  flex: 1;
  display: flex;
  align-items: center;
  min-width: 0;
  height: 72rpx;
  padding: 0 72rpx 0 20rpx;
  background: #fff;
  border-radius: 999rpx;
}

.search-icon {
  flex-shrink: 0;
  margin-right: 12rpx;
}

.search-input {
  flex: 1;
  height: 72rpx;
  font-size: 28rpx;
  color: #222;
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

.create-bottom-bar {
  z-index: 80;
}

.create-bottom-btn {
  width: 100%;
}

.create-bottom-btn::after {
  border: none;
}

.tabs {
  display: flex;
  border-bottom: 1rpx solid #f0f0f0;
}

.tab-item {
  flex: 1;
  text-align: center;
  padding: 24rpx 0;
  font-size: 30rpx;
  color: #666;
  position: relative;
}

.tab-item.active {
  color: #07c160;
  font-weight: 600;
}

.tab-item.active::after {
  content: '';
  position: absolute;
  left: 50%;
  bottom: 0;
  transform: translateX(-50%);
  width: 48rpx;
  height: 6rpx;
  background: #07c160;
  border-radius: 3rpx;
}

.summary-bar {
  padding: 20rpx 24rpx;
  background: #fff8e6;
  font-size: 28rpx;
  color: #e67e22;
  font-weight: 500;
}

.list-scroll {
  height: calc(100vh - 176rpx - 128rpx - env(safe-area-inset-bottom));
  background: #f5f6f7;
  box-sizing: border-box;
}

.list-scroll--unsettled {
  height: calc(100vh - 248rpx - 128rpx - env(safe-area-inset-bottom));
}

.list-content {
  padding-bottom: 16rpx;
}

.state-wrap {
  padding: 80rpx 0;
  text-align: center;
}

.section-title {
  padding: 20rpx 24rpx 12rpx;
  font-size: 26rpx;
  color: #999;
  background: #f5f6f7;
}

.row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 28rpx 24rpx;
  border-bottom: 1rpx solid #f0f0f0;
  gap: 16rpx;
  background: #fff;
}

.row-main {
  flex: 1;
  min-width: 0;
}

.name {
  display: block;
  font-size: 32rpx;
  font-weight: 600;
  color: #222;
}

.supplier-no {
  display: block;
  margin-top: 6rpx;
  font-size: 24rpx;
  color: #888;
}

.payable-tag {
  display: inline-block;
  margin-top: 10rpx;
  padding: 4rpx 14rpx;
  background: #fdecea;
  border-radius: 8rpx;
  font-size: 24rpx;
  color: #e74c3c;
}

.row-side {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 12rpx;
}

.status-tag {
  padding: 10rpx 24rpx;
  font-size: 26rpx;
  color: #07c160;
  border: 2rpx solid #07c160;
  border-radius: 999rpx;
  background: #fff;
}

.status-tag.disabled {
  color: #999;
  border-color: #ddd;
}

.contact-hint {
  font-size: 22rpx;
  color: #bbb;
}

.list-end {
  padding: 24rpx 0 8rpx;
  text-align: center;
  font-size: 24rpx;
  color: #ccc;
}

.form {
  padding: 40rpx 32rpx 60rpx;
}

.form-title {
  display: block;
  margin-bottom: 24rpx;
  font-size: 32rpx;
  font-weight: 600;
}
</style>
