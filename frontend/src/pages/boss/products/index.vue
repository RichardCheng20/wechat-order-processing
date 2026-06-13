<template>
  <view class="page">
    <view class="header">
      <text class="title">商品管理</text>
      <u-button size="small" type="primary" text="新建" @click="showCreate = true" />
    </view>

    <view v-if="loading" class="loading-wrap">
      <u-loading-icon text="加载中" />
    </view>

    <view v-else class="list">
      <view v-for="item in products" :key="item.id" class="card">
        <view class="main">
          <text class="name">{{ item.name }}</text>
          <text class="meta">{{ item.categoryName }} · {{ item.unit }} · 默认价 ¥{{ formatPrice(item.defaultPrice) }}</text>
        </view>
        <view class="actions">
          <u-tag :text="item.saleStatus === 'ON' ? '上架' : '下架'" :type="item.saleStatus === 'ON' ? 'success' : 'info'" />
          <text class="toggle" @click="toggleSaleStatus(item)">
            {{ item.saleStatus === 'ON' ? '下架' : '上架' }}
          </text>
        </view>
      </view>
    </view>

    <u-popup :show="showCreate" mode="bottom" round="16" @close="showCreate = false">
      <view class="form">
        <text class="form-title">新建商品</text>
        <picker :range="categories" range-key="name" @change="onCategoryChange">
          <view class="picker-line">分类：{{ selectedCategoryName || '请选择' }}</view>
        </picker>
        <u-input v-model="form.name" placeholder="商品名称" />
        <u-input v-model="form.aliases" placeholder="别名，逗号分隔" />
        <u-input v-model="form.unit" placeholder="单位，默认斤" />
        <u-input v-model="form.defaultPrice" type="digit" placeholder="默认售价" />
        <u-button type="primary" text="保存" :loading="saving" @click="submitCreate" />
      </view>
    </u-popup>
  </view>
</template>

<script setup lang="ts">
import { onShow } from '@dcloudio/uni-app'
import { computed, reactive, ref } from 'vue'
import {
  createBossProduct,
  fetchBossCategories,
  fetchBossProducts,
  updateBossProductSaleStatus,
  type CategoryItem,
  type ProductItem,
} from '../../../api/product'
import { useUserStore } from '../../../stores/user'

const userStore = useUserStore()
const categories = ref<CategoryItem[]>([])
const products = ref<ProductItem[]>([])
const loading = ref(false)
const saving = ref(false)
const showCreate = ref(false)
const form = reactive({
  categoryId: 0,
  name: '',
  aliases: '',
  unit: '斤',
  defaultPrice: '',
})

const selectedCategoryName = computed(() =>
  categories.value.find((item) => item.id === form.categoryId)?.name || '',
)

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
    categories.value = await fetchBossCategories()
    products.value = await fetchBossProducts()
    if (!form.categoryId && categories.value.length > 0) {
      form.categoryId = categories.value[0].id
    }
  } finally {
    loading.value = false
  }
}

function onCategoryChange(e: { detail: { value: string } }) {
  const index = Number(e.detail.value)
  form.categoryId = categories.value[index]?.id || 0
}

async function submitCreate() {
  if (!form.categoryId || !form.name.trim()) {
    uni.showToast({ title: '请填写分类和名称', icon: 'none' })
    return
  }
  saving.value = true
  try {
    await createBossProduct({
      categoryId: form.categoryId,
      name: form.name.trim(),
      aliases: form.aliases || undefined,
      unit: form.unit || '斤',
      defaultPrice: form.defaultPrice ? Number(form.defaultPrice) : undefined,
    })
    showCreate.value = false
    form.name = ''
    form.aliases = ''
    form.defaultPrice = ''
    await loadData()
    uni.showToast({ title: '创建成功', icon: 'success' })
  } finally {
    saving.value = false
  }
}

async function toggleSaleStatus(item: ProductItem) {
  const next = item.saleStatus === 'ON' ? 'OFF' : 'ON'
  await updateBossProductSaleStatus(item.id, next)
  await loadData()
}

function formatPrice(value?: number) {
  if (value == null) return '--'
  return Number(value).toFixed(2)
}
</script>

<style scoped lang="scss">
.page {
  min-height: 100vh;
  padding: 24rpx;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24rpx;
}

.title {
  font-size: 36rpx;
  font-weight: 600;
}

.loading-wrap {
  padding: 80rpx 0;
}

.card {
  background: #fff;
  border-radius: 16rpx;
  padding: 28rpx;
  margin-bottom: 16rpx;
}

.name {
  display: block;
  font-size: 32rpx;
  font-weight: 600;
}

.meta {
  display: block;
  margin-top: 8rpx;
  color: #666;
  font-size: 24rpx;
}

.actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 20rpx;
}

.toggle {
  color: #27ae60;
  font-size: 26rpx;
}

.form {
  padding: 32rpx;
}

.form-title {
  display: block;
  font-size: 32rpx;
  font-weight: 600;
  margin-bottom: 24rpx;
}

.picker-line {
  padding: 24rpx 0;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 16rpx;
}
</style>
