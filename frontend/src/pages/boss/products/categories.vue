<template>
  <view class="page boss-page">
    <view class="search-wrap">
      <input
        class="search-input"
        type="text"
        :value="keyword"
        placeholder="搜索分类名称"
        @input="onKeywordInput"
      />
    </view>

    <scroll-view scroll-y class="list-scroll boss-page-scroll">
      <view v-if="loading" class="state-wrap">
        <u-loading-icon text="加载中" />
      </view>

      <view v-else-if="displayGroups.length === 0" class="state-wrap">
        <u-empty mode="list" text="暂无分类" />
      </view>

      <view v-else>
        <view v-for="group in displayGroups" :key="group.parent.id" class="group">
          <view class="row parent">
            <text class="name">{{ group.parent.name }}</text>
            <AppIcon
              class="delete"
              name="delete"
              tone="red"
              :size="18"
              :tile-size="48"
              :radius="12"
              @tap="handleDelete(group.parent)"
            />
          </view>
          <view
            v-for="child in group.children"
            :key="child.id"
            class="row child"
          >
            <text class="name">{{ child.name }}</text>
            <AppIcon
              class="delete"
              name="delete"
              tone="red"
              :size="18"
              :tile-size="48"
              :radius="12"
              @tap="handleDelete(child)"
            />
          </view>
          <view class="add-child" @tap="createChild(group.parent.id)">+ 添加二级分类</view>
        </view>
      </view>
    </scroll-view>

    <view class="boss-bottom-bar boss-bottom-bar--static">
      <button class="boss-primary-btn block" @tap="createRoot">新建分类</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { onShow } from '@dcloudio/uni-app'
import { computed, ref } from 'vue'
import {
  createBossCategory,
  deleteBossCategory,
  fetchBossCategories,
  type CategoryItem,
} from '../../../api/product'
import AppIcon from '../../../components/AppIcon.vue'
import { useUserStore } from '../../../stores/user'

const userStore = useUserStore()
const categories = ref<CategoryItem[]>([])
const keyword = ref('')
const loading = ref(false)

const displayGroups = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  return categories.value
    .map((parent) => {
      const children = (parent.children || []).filter((child) => {
        if (!kw) return true
        return child.name.toLowerCase().includes(kw) || parent.name.toLowerCase().includes(kw)
      })
      if (kw && !parent.name.toLowerCase().includes(kw) && children.length === 0) {
        return null
      }
      return { parent, children }
    })
    .filter((g): g is { parent: CategoryItem; children: CategoryItem[] } => g != null)
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
    categories.value = await fetchBossCategories()
  } finally {
    loading.value = false
  }
}

function onKeywordInput(e: { detail: { value: string } }) {
  keyword.value = e.detail.value
}

function createRoot() {
  uni.showModal({
    title: '新建一级分类',
    editable: true,
    placeholderText: '如：蔬菜',
    success: async (res) => {
      if (!res.confirm || !res.content?.trim()) return
      try {
        await createBossCategory(res.content.trim())
        await loadData()
        uni.showToast({ title: '已添加', icon: 'success' })
      } catch (e) {
        uni.showToast({ title: e instanceof Error ? e.message : '添加失败', icon: 'none' })
      }
    },
  })
}

function createChild(parentId: number) {
  uni.showModal({
    title: '新建二级分类',
    editable: true,
    placeholderText: '如：蘑菇',
    success: async (res) => {
      if (!res.confirm || !res.content?.trim()) return
      try {
        await createBossCategory(res.content.trim(), parentId)
        await loadData()
        uni.showToast({ title: '已添加', icon: 'success' })
      } catch (e) {
        uni.showToast({ title: e instanceof Error ? e.message : '添加失败', icon: 'none' })
      }
    },
  })
}

function handleDelete(item: CategoryItem) {
  uni.showModal({
    title: '确认删除',
    content: `确定删除分类「${item.name}」？`,
    confirmColor: '#e74c3c',
    success: async (res) => {
      if (!res.confirm) return
      try {
        await deleteBossCategory(item.id)
        await loadData()
        uni.showToast({ title: '已删除', icon: 'success' })
      } catch (e) {
        const msg = e instanceof Error ? e.message : '删除失败'
        if (msg.includes('仍有商品')) {
          uni.showModal({
            title: '无法删除',
            content: `分类「${item.name}」下还有商品，请先到商品管理删除或移走后再删分类。`,
            confirmText: '去商品管理',
            success: (nav) => {
              if (nav.confirm) {
                uni.navigateTo({ url: `/pages/boss/products/index?categoryId=${item.id}` })
              }
            },
          })
          return
        }
        uni.showToast({ title: msg, icon: 'none' })
      }
    },
  })
}
</script>

<style scoped lang="scss">
@import '../../../styles/boss-footer.scss';

.search-wrap {
  flex-shrink: 0;
  padding: 16rpx 24rpx;
  background: #f5f6f7;
}

.search-input {
  height: 72rpx;
  padding: 0 28rpx;
  background: #fff;
  border-radius: 999rpx;
  font-size: 28rpx;
}

.list-scroll {
  background: #fff;
}

.state-wrap {
  padding: 80rpx 0;
  text-align: center;
}

.group {
  border-bottom: 16rpx solid #f5f6f7;
}

.row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 28rpx 32rpx;
  border-bottom: 1rpx solid #f0f0f0;
}

.row.child {
  padding-left: 64rpx;
}

.name {
  font-size: 30rpx;
  color: #222;
}

.row.parent .name {
  font-weight: 600;
}

.delete {
  width: 56rpx;
  height: 48rpx;
  border-radius: 12rpx;
}

.add-child {
  padding: 24rpx 32rpx 24rpx 64rpx;
  font-size: 28rpx;
  color: #2979ff;
}
</style>
