<template>
  <view class="page">
    <view class="table-head">
      <text class="col-name">{{ parentId ? '二级分类名称' : '一级分类名称' }}</text>
      <text class="col-pin">置顶</text>
      <text class="col-sort">排序</text>
    </view>

    <scroll-view scroll-y class="list-scroll" :show-scrollbar="false">
      <view v-if="loading" class="state-wrap">
        <u-loading-icon text="加载中" />
      </view>
      <view v-else-if="list.length === 0" class="state-wrap">
        <u-empty mode="list" text="暂无分类" />
      </view>
      <view v-else>
        <view
          v-for="(item, index) in list"
          :key="item.id"
          class="sort-row"
          :class="{ dragging: dragIndex === index, 'drag-over': dragOverIndex === index && dragIndex !== index }"
        >
          <view class="col-name" @tap="goChildSort(item)">
            <text class="name">{{ item.name }}</text>
            <text v-if="!parentId && item.children?.length" class="sub-hint">›</text>
          </view>
          <view class="col-pin" @tap="pinTop(index)">
            <text class="pin-icon">⤒</text>
          </view>
          <view
            class="col-sort drag-handle"
            @touchstart="onDragStart(index, $event)"
            @touchmove.stop.prevent="onDragMove"
            @touchend="onDragEnd"
            @touchcancel="onDragEnd"
          >
            <AppIcon name="filter" tone="gray" :size="20" :tile="false" />
          </view>
        </view>
      </view>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import {
  fetchBossCategories,
  sortBossCategories,
  type CategoryItem,
} from '@common/api/product'
import AppIcon from '@/components/AppIcon.vue'
import { useUserStore } from '@common/stores/user'

const userStore = useUserStore()
const parentId = ref<number | null>(null)
const parentName = ref('')
const parentChildrenMap = ref<Record<number, CategoryItem[]>>({})
const list = ref<CategoryItem[]>([])
const loading = ref(false)
const saving = ref(false)

const dragIndex = ref(-1)
const dragOverIndex = ref(-1)
let listTop = 0
let rowHeight = 56

onLoad((query) => {
  if (query?.parentId) {
    parentId.value = Number(query.parentId)
  }
  if (query?.parentName) {
    parentName.value = decodeURIComponent(String(query.parentName))
  }
})

onShow(async () => {
  if (!userStore.isLoggedIn || !userStore.isBoss) {
    uni.reLaunch({ url: '/packages/common/login/index' })
    return
  }
  uni.setNavigationBarTitle({
    title: parentName.value || '分类排序',
  })
  await loadList()
})

async function loadList() {
  loading.value = true
  try {
    const tree = await fetchBossCategories()
    parentChildrenMap.value = Object.fromEntries(
      tree.map((p) => [p.id, p.children || []]),
    )
    if (parentId.value) {
      const parent = tree.find((p) => p.id === parentId.value)
      list.value = parent?.children ? [...parent.children] : []
    } else {
      list.value = tree.map((p) => ({
        id: p.id,
        name: p.name,
        sortOrder: p.sortOrder,
        children: p.children,
      }))
    }
  } finally {
    loading.value = false
  }
}

async function saveOrder() {
  if (saving.value || list.value.length === 0) return
  saving.value = true
  try {
    await sortBossCategories(
      list.value.map((item) => item.id),
      parentId.value ?? undefined,
    )
  } catch (e) {
    uni.showToast({ title: e instanceof Error ? e.message : '保存失败', icon: 'none' })
    await loadList()
  } finally {
    saving.value = false
  }
}

function moveItem(from: number, to: number) {
  if (from === to || from < 0 || to < 0 || from >= list.value.length || to >= list.value.length) {
    return
  }
  const arr = [...list.value]
  const [item] = arr.splice(from, 1)
  arr.splice(to, 0, item)
  list.value = arr
  saveOrder()
}

function pinTop(index: number) {
  if (index <= 0) return
  moveItem(index, 0)
}

function goChildSort(item: CategoryItem) {
  if (parentId.value) return
  const children = item.children || parentChildrenMap.value[item.id] || []
  if (children.length === 0) return
  uni.navigateTo({
    url: `/pages/boss/products/categories/sort/index?parentId=${item.id}&parentName=${encodeURIComponent(item.name)}`,
  })
}

function onDragStart(index: number, e: TouchEvent) {
  dragIndex.value = index
  dragOverIndex.value = index
  const touch = e.touches[0]
  uni.createSelectorQuery()
    .select('.list-scroll')
    .boundingClientRect((rect) => {
      if (rect && !Array.isArray(rect)) {
        listTop = rect.top
      }
    })
    .select('.sort-row')
    .boundingClientRect((rect) => {
      if (rect && !Array.isArray(rect) && rect.height) {
        rowHeight = rect.height
      }
    })
    .exec()
  if (touch) {
    updateDragTarget(touch.clientY)
  }
}

function onDragMove(e: TouchEvent) {
  if (dragIndex.value < 0) return
  const touch = e.touches[0]
  if (touch) {
    updateDragTarget(touch.clientY)
  }
}

function updateDragTarget(clientY: number) {
  let target = Math.floor((clientY - listTop) / rowHeight)
  target = Math.max(0, Math.min(list.value.length - 1, target))
  dragOverIndex.value = target
}

async function onDragEnd() {
  if (dragIndex.value >= 0 && dragOverIndex.value >= 0 && dragIndex.value !== dragOverIndex.value) {
    moveItem(dragIndex.value, dragOverIndex.value)
  }
  dragIndex.value = -1
  dragOverIndex.value = -1
}
</script>

<style scoped lang="scss">
.page {
  min-height: 100vh;
  background: #f5f6f7;
  display: flex;
  flex-direction: column;
}

.table-head {
  display: flex;
  align-items: center;
  padding: 20rpx 32rpx;
  background: #f0f1f2;
  font-size: 26rpx;
  color: #888;
}

.col-name {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 8rpx;
}

.col-pin {
  width: 100rpx;
  text-align: center;
}

.col-sort {
  width: 100rpx;
  text-align: center;
}

.list-scroll {
  flex: 1;
  height: calc(100vh - 72rpx);
  background: #fff;
}

.state-wrap {
  padding: 120rpx 0;
}

.sort-row {
  display: flex;
  align-items: center;
  padding: 28rpx 32rpx;
  border-bottom: 1rpx solid #f0f0f0;
  background: #fff;
  transition: background 0.15s;

  &.dragging {
    opacity: 0.6;
    background: #f8faf8;
  }

  &.drag-over {
    background: #eef8f0;
  }
}

.name {
  font-size: 30rpx;
  color: #222;
}

.sub-hint {
  font-size: 32rpx;
  color: #ccc;
}

.pin-icon {
  font-size: 36rpx;
  color: #666;
  line-height: 1;
}

.drag-handle {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 8rpx 0;
}
</style>
