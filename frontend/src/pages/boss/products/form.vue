<template>
  <view class="page boss-page">
    <scroll-view scroll-y class="body boss-page-scroll boss-scroll-with-footer">
      <view class="form-card">
        <view class="form-row">
          <text class="label">商品名称</text>
          <input
            class="value-input"
            type="text"
            :value="form.name"
            placeholder="输入商品名称"
            placeholder-class="placeholder"
            @input="onNameInput"
          />
        </view>
        <view class="form-row" @tap="editAlias">
          <text class="label">商品别名</text>
          <text v-if="form.aliases" class="value-text">{{ form.aliases }}</text>
          <text v-else class="value-text placeholder">添加商品别名</text>
          <text class="arrow">›</text>
        </view>
        <view class="form-row" @tap="openCategoryPicker">
          <text class="label">分类</text>
          <text class="value-text">{{ selectedCategoryName || '请选择分类' }}</text>
          <text class="arrow">›</text>
        </view>
      </view>

      <view class="form-card unit-card">
        <view class="unit-section-head">
          <view>
            <text class="section-title">单位与价格</text>
            <text class="section-sub">基本单位填默认售价，辅助单位开单时可选</text>
          </view>
        </view>
        <view class="unit-head-row">
          <text class="col-unit">单位</text>
          <text class="col-price">价格(元)</text>
        </view>

        <view class="unit-data-row">
          <view class="unit-left">
            <text class="unit-badge">基本</text>
            <view class="unit-name-cell" @tap="openUnitPicker('basic')">
              <text>{{ primaryUnit }}</text>
              <text class="unit-arrow">▼</text>
            </view>
          </view>
          <input
            class="price-input"
            type="digit"
            :value="form.defaultPrice"
            placeholder="0"
            placeholder-class="placeholder"
            @input="onPriceInput"
          />
        </view>

        <view v-for="(unit, index) in auxiliaryUnits" :key="unit + index" class="unit-data-row">
          <view class="unit-left">
            <text class="unit-badge aux">辅助</text>
            <view class="unit-name-cell" @tap="openUnitPicker('auxiliary', index)">
              <text>{{ unit }}</text>
              <text class="unit-arrow">▼</text>
            </view>
          </view>
          <text class="price-muted">—</text>
          <text class="unit-remove" @tap="removeAuxUnit(index)">×</text>
        </view>

        <view class="add-row">
          <text class="add-link" @tap="openUnitPicker('add-aux')">+ 辅助单位</text>
          <text class="add-link" @tap="openCustomUnit('auxiliary')">+ 自定义单位</text>
        </view>
      </view>

      <view class="form-card">
        <view class="form-row switch-row">
          <view class="label-wrap">
            <text class="label">上架</text>
            <text class="label-tip">开启后可在开单与客户端选购</text>
          </view>
          <switch :checked="form.listOn" color="#07c160" @change="onListToggle" />
        </view>
      </view>
    </scroll-view>

    <view class="boss-bottom-bar">
      <button class="boss-primary-btn block" :loading="saving" @tap="submit">保存</button>
    </view>

    <!-- 分类选择：左侧一级、右侧二级 -->
    <u-popup :show="showCategoryPicker" mode="bottom" round="16" @close="showCategoryPicker = false">
      <view class="picker-sheet cat-picker">
        <view class="picker-head">
          <text class="picker-cancel" @tap="showCategoryPicker = false">取消</text>
          <text class="picker-title">选择分类</text>
          <text class="picker-action" @tap="createCategoryFromPicker">新建</text>
        </view>
        <view class="cat-picker-body">
          <scroll-view scroll-y class="cat-picker-left">
            <view class="cat-add-top" @tap="createRootCategory">+ 一级分类</view>
            <view
              v-for="parent in categories"
              :key="parent.id"
              class="cat-parent-item"
              :class="{ active: pickerParentId === parent.id }"
              @tap="pickerParentId = parent.id"
            >{{ parent.name }}</view>
          </scroll-view>
          <scroll-view scroll-y class="cat-picker-right">
            <view v-if="!pickerParentId" class="picker-empty">请选择左侧一级分类</view>
            <template v-else-if="pickerChildren.length > 0">
              <view
                v-for="child in pickerChildren"
                :key="child.id"
                class="picker-item"
                :class="{ active: form.categoryId === child.id }"
                @tap="selectCategory(child.id)"
              >
                <text>{{ child.name }}</text>
                <text v-if="form.categoryId === child.id" class="picker-check">✓</text>
              </view>
              <view class="cat-add-child" @tap="createChildCategory">+ 添加二级分类</view>
            </template>
            <view
              v-else
              class="picker-item"
              :class="{ active: form.categoryId === pickerParentId }"
              @tap="selectCategory(pickerParentId!)"
            >
              <text>{{ pickerParentName }}（直接选用）</text>
              <text v-if="form.categoryId === pickerParentId" class="picker-check">✓</text>
            </view>
          </scroll-view>
        </view>
      </view>
    </u-popup>

    <!-- 单位选择 -->
    <u-popup :show="showUnitPicker" mode="bottom" round="16" @close="closeUnitPicker">
      <view class="picker-sheet tall">
        <view class="picker-head">
          <text class="picker-cancel" @tap="closeUnitPicker">取消</text>
          <text class="picker-title">选择单位</text>
          <text class="picker-action" @tap="openCustomUnit(unitPickerMode === 'basic' ? 'basic' : 'auxiliary')">新建单位</text>
        </view>
        <view class="picker-search">
          <input
            class="search-input"
            type="text"
            :value="unitKeyword"
            placeholder="搜索单位名称"
            @input="onUnitKeywordInput"
          />
        </view>
        <scroll-view scroll-y class="unit-grid-wrap">
          <view class="unit-grid">
            <view
              v-for="unit in filteredPickerUnits"
              :key="unit"
              class="unit-chip"
              :class="{ active: isPickerUnitActive(unit) }"
              @tap="applyPickerUnit(unit)"
            >
              {{ unit }}
            </view>
          </view>
          <view v-if="filteredPickerUnits.length === 0" class="picker-empty">未找到匹配单位</view>
        </scroll-view>
      </view>
    </u-popup>
  </view>
</template>

<script setup lang="ts">
import { onLoad } from '@dcloudio/uni-app'
import { computed, reactive, ref } from 'vue'
import {
  COMMON_SALE_UNITS,
  filterUnits,
  mergeUnits,
  normalizeUnit,
  parseSaleUnits,
} from '../../../constants/units'
import {
  createBossCategory,
  createBossProduct,
  fetchBossCategories,
  fetchBossProducts,
  updateBossProduct,
  type CategoryItem,
  type ProductItem,
} from '../../../api/product'
import { getCategoryDisplayName, getLeafCategories } from '../../../utils/category'
import { useUserStore } from '../../../stores/user'

type UnitPickerMode = 'basic' | 'auxiliary' | 'add-aux'

const userStore = useUserStore()
const productId = ref<number | null>(null)
const categories = ref<CategoryItem[]>([])
const saving = ref(false)
const showCategoryPicker = ref(false)
const pickerParentId = ref<number | null>(null)
const showUnitPicker = ref(false)
const unitPickerMode = ref<UnitPickerMode>('basic')
const editingAuxIndex = ref(-1)
const unitKeyword = ref('')
const customUnits = ref<string[]>([])
const primaryUnit = ref('斤')
const auxiliaryUnits = ref<string[]>([])

const form = reactive({
  categoryId: 0,
  name: '',
  aliases: '',
  defaultPrice: '',
  listOn: true,
})

const selectedCategoryName = computed(() =>
  getCategoryDisplayName(categories.value, form.categoryId),
)

const pickerChildren = computed(() => {
  if (!pickerParentId.value) return []
  return categories.value.find((c) => c.id === pickerParentId.value)?.children || []
})

const pickerParentName = computed(() =>
  categories.value.find((c) => c.id === pickerParentId.value)?.name || '',
)

const allPickerUnits = computed(() =>
  mergeUnits([primaryUnit.value, ...auxiliaryUnits.value, ...customUnits.value, ...COMMON_SALE_UNITS]),
)

const filteredPickerUnits = computed(() => filterUnits(allPickerUnits.value, unitKeyword.value))

const saleUnits = computed(() => {
  const units = [primaryUnit.value, ...auxiliaryUnits.value.filter((u) => u !== primaryUnit.value)]
  return [...new Set(units)]
})

onLoad(async (query) => {
  if (!userStore.isLoggedIn || !userStore.isBoss) {
    uni.reLaunch({ url: '/pages/login/index' })
    return
  }
  if (query?.id) {
    productId.value = Number(query.id)
    uni.setNavigationBarTitle({ title: '编辑商品' })
  } else {
    uni.setNavigationBarTitle({ title: '新建商品' })
  }
  await loadFormData()
})

async function loadFormData() {
  categories.value = await fetchBossCategories()
  if (productId.value) {
    const products = await fetchBossProducts()
    const item = products.find((p) => p.id === productId.value)
    if (!item) {
      uni.showToast({ title: '商品不存在', icon: 'none' })
      setTimeout(() => uni.navigateBack(), 500)
      return
    }
    fillFromProduct(item)
    return
  }
  if (categories.value.length > 0) {
    const leaves = getLeafCategories(categories.value)
    if (leaves.length > 0) {
      form.categoryId = leaves[0].id
      pickerParentId.value = leaves[0].parentId || leaves[0].id
    }
  }
}

function fillFromProduct(item: ProductItem) {
  form.categoryId = item.categoryId
  form.name = item.name
  form.aliases = item.aliases || ''
  form.defaultPrice = item.defaultPrice != null ? String(item.defaultPrice) : ''
  form.listOn = item.saleStatus === 'ON'
  const units = parseSaleUnits(item.saleUnits, item.unit)
  primaryUnit.value = units[0] || '斤'
  auxiliaryUnits.value = units.slice(1)
  syncPickerFromForm()
}

function syncPickerFromForm() {
  for (const parent of categories.value) {
    if (parent.id === form.categoryId) {
      pickerParentId.value = parent.id
      return
    }
    for (const child of parent.children || []) {
      if (child.id === form.categoryId) {
        pickerParentId.value = parent.id
        return
      }
    }
  }
}

function openCategoryPicker() {
  syncPickerFromForm()
  if (!pickerParentId.value && categories.value.length > 0) {
    pickerParentId.value = categories.value[0].id
  }
  showCategoryPicker.value = true
}

function onNameInput(e: { detail: { value: string } }) {
  form.name = e.detail.value
}

function onPriceInput(e: { detail: { value: string } }) {
  form.defaultPrice = e.detail.value
}

function onListToggle(e: { detail: { value: boolean } }) {
  form.listOn = e.detail.value
}

function editAlias() {
  uni.showModal({
    title: '商品别名',
    editable: true,
    placeholderText: '多个别名用逗号分隔',
    content: form.aliases,
    success: (res) => {
      if (res.confirm && res.content != null) {
        form.aliases = res.content.trim()
      }
    },
  })
}

function selectCategory(id: number) {
  form.categoryId = id
  showCategoryPicker.value = false
}

function createCategoryFromPicker() {
  if (pickerParentId.value) {
    createChildCategory()
    return
  }
  createRootCategory()
}

function createRootCategory() {
  uni.showModal({
    title: '新建一级分类',
    editable: true,
    placeholderText: '如：蔬菜、菌菇',
    success: async (res) => {
      if (!res.confirm || !res.content?.trim()) return
      try {
        const created = await createBossCategory(res.content.trim())
        categories.value = await fetchBossCategories()
        pickerParentId.value = created.id
        uni.showToast({ title: '一级分类已添加', icon: 'success' })
      } catch (e) {
        uni.showToast({ title: e instanceof Error ? e.message : '添加失败', icon: 'none' })
      }
    },
  })
}

function createChildCategory() {
  if (!pickerParentId.value) {
    uni.showToast({ title: '请先选择一级分类', icon: 'none' })
    return
  }
  uni.showModal({
    title: '新建二级分类',
    editable: true,
    placeholderText: '如：蘑菇、叶菜',
    success: async (res) => {
      if (!res.confirm || !res.content?.trim()) return
      try {
        const created = await createBossCategory(res.content.trim(), pickerParentId.value!)
        categories.value = await fetchBossCategories()
        form.categoryId = created.id
        uni.showToast({ title: '二级分类已添加', icon: 'success' })
      } catch (e) {
        uni.showToast({ title: e instanceof Error ? e.message : '添加失败', icon: 'none' })
      }
    },
  })
}

function openUnitPicker(mode: UnitPickerMode, auxIndex = -1) {
  unitPickerMode.value = mode
  editingAuxIndex.value = auxIndex
  unitKeyword.value = ''
  showUnitPicker.value = true
}

function closeUnitPicker() {
  showUnitPicker.value = false
  unitKeyword.value = ''
}

function onUnitKeywordInput(e: { detail: { value: string } }) {
  unitKeyword.value = e.detail.value
}

function isPickerUnitActive(unit: string) {
  if (unitPickerMode.value === 'basic') return primaryUnit.value === unit
  if (unitPickerMode.value === 'auxiliary' && editingAuxIndex.value >= 0) {
    return auxiliaryUnits.value[editingAuxIndex.value] === unit
  }
  return false
}

function applyPickerUnit(unit: string) {
  if (unitPickerMode.value === 'basic') {
    if (auxiliaryUnits.value.includes(unit)) {
      auxiliaryUnits.value = auxiliaryUnits.value.filter((u) => u !== unit)
    }
    primaryUnit.value = unit
    closeUnitPicker()
    return
  }
  if (unitPickerMode.value === 'auxiliary' && editingAuxIndex.value >= 0) {
    if (unit === primaryUnit.value) {
      uni.showToast({ title: '与基本单位重复', icon: 'none' })
      return
    }
    const next = [...auxiliaryUnits.value]
    next[editingAuxIndex.value] = unit
    auxiliaryUnits.value = next
    closeUnitPicker()
    return
  }
  if (unit === primaryUnit.value || auxiliaryUnits.value.includes(unit)) {
    uni.showToast({ title: '单位已存在', icon: 'none' })
    return
  }
  auxiliaryUnits.value = [...auxiliaryUnits.value, unit]
  closeUnitPicker()
}

function openCustomUnit(target: 'basic' | 'auxiliary') {
  uni.showModal({
    title: '新建单位',
    editable: true,
    placeholderText: '请输入单位名称',
    success: (res) => {
      if (!res.confirm || !res.content) return
      const unit = normalizeUnit(res.content)
      if (!unit) return
      if (!customUnits.value.includes(unit)) {
        customUnits.value = [unit, ...customUnits.value]
      }
      if (target === 'basic') {
        if (auxiliaryUnits.value.includes(unit)) {
          auxiliaryUnits.value = auxiliaryUnits.value.filter((u) => u !== unit)
        }
        primaryUnit.value = unit
      } else {
        applyPickerUnit(unit)
      }
      closeUnitPicker()
    },
  })
}

function removeAuxUnit(index: number) {
  auxiliaryUnits.value = auxiliaryUnits.value.filter((_, i) => i !== index)
}

async function submit() {
  if (!form.categoryId || !form.name.trim()) {
    uni.showToast({ title: '请填写分类和名称', icon: 'none' })
    return
  }
  if (saleUnits.value.length === 0) {
    uni.showToast({ title: '请设置基本单位', icon: 'none' })
    return
  }
  saving.value = true
  try {
    const payload = {
      categoryId: form.categoryId,
      name: form.name.trim(),
      aliases: form.aliases.trim() || undefined,
      unit: primaryUnit.value,
      saleUnits: saleUnits.value,
      defaultPrice: form.defaultPrice ? Number(form.defaultPrice) : undefined,
      saleStatus: form.listOn ? 'ON' : 'OFF',
    }
    if (productId.value) {
      await updateBossProduct(productId.value, payload)
    } else {
      await createBossProduct(payload)
    }
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

.page {
  background: #f5f6f7;
}

.body {
  padding: 0;
}

.form-card {
  margin-bottom: 16rpx;
  background: #fff;
}

.unit-card {
  margin-bottom: 16rpx;
}

.unit-section-head {
  padding: 24rpx 32rpx 8rpx;
  border-bottom: 1rpx solid #f2f3f5;
}

.section-title {
  display: block;
  font-size: 28rpx;
  color: #666;
  font-weight: 600;
}

.section-sub {
  display: block;
  margin-top: 4rpx;
  font-size: 22rpx;
  color: #bbb;
}

.form-row {
  display: flex;
  align-items: center;
  min-height: 100rpx;
  padding: 0 32rpx;
  border-bottom: 1rpx solid #f2f3f5;
}

.form-row:last-child {
  border-bottom: none;
}

.label {
  flex-shrink: 0;
  width: 180rpx;
  font-size: 30rpx;
  color: #333;
}

.value-input {
  flex: 1;
  font-size: 30rpx;
  color: #222;
  text-align: right;
}

.value-text {
  flex: 1;
  font-size: 30rpx;
  color: #222;
  text-align: right;
}

.placeholder {
  color: #c8c8c8;
}

.arrow {
  margin-left: 12rpx;
  font-size: 32rpx;
  color: #ccc;
}

.section-bar {
  padding: 28rpx 32rpx 12rpx;
}

.section-title {
  display: block;
  font-size: 26rpx;
  color: #999;
}

.section-sub {
  display: block;
  margin-top: 6rpx;
  font-size: 22rpx;
  color: #bbb;
}

.unit-head-row,
.unit-data-row {
  display: flex;
  align-items: center;
  padding: 0 32rpx;
  min-height: 96rpx;
  border-bottom: 1rpx solid #f2f3f5;
}

.unit-head-row {
  min-height: 72rpx;
  background: #fafafa;
  font-size: 26rpx;
  color: #999;
}

.col-unit {
  flex: 1;
}

.col-price {
  width: 180rpx;
  text-align: right;
}

.unit-left {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.unit-badge {
  flex-shrink: 0;
  padding: 6rpx 14rpx;
  font-size: 22rpx;
  color: #07c160;
  background: #e8f8ef;
  border-radius: 8rpx;
}

.unit-badge.aux {
  color: #2979ff;
  background: #eef5ff;
}

.unit-name-cell {
  display: flex;
  align-items: center;
  gap: 8rpx;
  min-width: 120rpx;
  padding: 12rpx 20rpx;
  background: #f7f8fa;
  border-radius: 10rpx;
  font-size: 30rpx;
  color: #333;
}

.unit-arrow {
  font-size: 18rpx;
  color: #999;
}

.price-input {
  width: 180rpx;
  font-size: 30rpx;
  color: #222;
  text-align: right;
}

.price-muted {
  width: 180rpx;
  text-align: right;
  font-size: 30rpx;
  color: #ddd;
}

.unit-remove {
  margin-left: 16rpx;
  width: 44rpx;
  height: 44rpx;
  line-height: 44rpx;
  text-align: center;
  font-size: 36rpx;
  color: #ccc;
}

.add-row {
  display: flex;
  flex-wrap: wrap;
  gap: 32rpx;
  padding: 28rpx 32rpx;
}

.add-link {
  font-size: 28rpx;
  color: #2979ff;
}

.switch-row {
  justify-content: space-between;
}

.label-wrap {
  flex: 1;
}

.label-tip {
  display: block;
  margin-top: 6rpx;
  font-size: 22rpx;
  color: #bbb;
}

.picker-sheet {
  max-height: 70vh;
  display: flex;
  flex-direction: column;
  background: #fff;
  padding-bottom: env(safe-area-inset-bottom);
}

.picker-sheet.tall {
  max-height: 78vh;
}

.picker-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 28rpx 32rpx 16rpx;
  border-bottom: 1rpx solid #f2f3f5;
}

.picker-cancel {
  min-width: 100rpx;
  font-size: 28rpx;
  color: #666;
}

.picker-title {
  font-size: 32rpx;
  font-weight: 600;
}

.picker-action {
  min-width: 100rpx;
  text-align: right;
  font-size: 28rpx;
  color: #2979ff;
}

.picker-list {
  max-height: 56vh;
}

.section-bar {
  padding: 28rpx 32rpx 12rpx;
}

.section-title {
  display: block;
  font-size: 26rpx;
  color: #999;
}

.section-sub {
  display: block;
  margin-top: 6rpx;
  font-size: 22rpx;
  color: #bbb;
}

.cat-picker {
  max-height: 72vh;
}

.cat-picker-body {
  display: flex;
  height: 52vh;
}

.cat-picker-left {
  width: 220rpx;
  flex-shrink: 0;
  background: #f5f6f7;
}

.cat-picker-right {
  flex: 1;
  background: #fff;
}

.cat-parent-item {
  padding: 28rpx 16rpx;
  text-align: center;
  font-size: 28rpx;
  color: #666;
}

.cat-parent-item.active {
  background: #fff;
  color: #07c160;
  font-weight: 600;
}

.cat-add-top,
.cat-add-child {
  padding: 24rpx;
  text-align: center;
  font-size: 26rpx;
  color: #2979ff;
}

.picker-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 32rpx;
  font-size: 30rpx;
  color: #333;
  border-bottom: 1rpx solid #f5f5f5;
}

.picker-item.active {
  color: #07c160;
  font-weight: 600;
}

.picker-check {
  font-size: 28rpx;
}

.picker-search {
  padding: 16rpx 24rpx;
}

.search-input {
  height: 72rpx;
  padding: 0 24rpx;
  background: #f5f6f7;
  border-radius: 999rpx;
  font-size: 28rpx;
}

.unit-grid-wrap {
  max-height: 52vh;
  padding: 0 24rpx 24rpx;
}

.unit-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
}

.unit-chip {
  width: calc((100% - 32rpx) / 3);
  height: 80rpx;
  line-height: 80rpx;
  text-align: center;
  font-size: 28rpx;
  color: #333;
  background: #f7f8fa;
  border-radius: 12rpx;
  border: 2rpx solid transparent;
  box-sizing: border-box;
}

.unit-chip.active {
  color: #07c160;
  background: #e8f8ef;
  border-color: #07c160;
  font-weight: 600;
}

.picker-empty {
  padding: 60rpx 0;
  text-align: center;
  color: #999;
  font-size: 28rpx;
}
</style>
