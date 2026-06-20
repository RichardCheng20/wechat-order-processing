<template>
  <view class="page boss-page customer-order-page">
    <view class="page-head">
      <view class="banner">
        <view class="banner-top">
          <text class="welcome">你好，{{ userStore.displayName }}</text>
        </view>
        <text v-if="userStore.customerId" class="hint">欢迎您选品下单，祝生意兴隆！</text>
        <text v-else class="hint">临时下单：购物车中填写店铺名称后提交</text>
        <text v-if="!userStore.customerId" class="bind-link" @tap="goBind">已有邀请码？去绑定客户档案</text>
      </view>

      <view class="search-box">
        <view class="search-wrap">
          <text class="search-icon">⌕</text>
          <input
            class="search-input"
            type="text"
            :value="keyword"
            placeholder="搜索商品名称/别名"
            confirm-type="search"
            @input="onKeywordInput"
            @confirm="loadProducts"
          />
          <text v-if="keyword" class="search-clear" @tap="clearKeyword">×</text>
        </view>
      </view>
    </view>

    <view class="main" :style="mainStyle">
      <view class="category-col">
        <scroll-view scroll-y class="categories" :show-scrollbar="false">
          <view
            v-for="cat in categoryTabs"
            :key="cat.key"
            class="cat-item"
            :class="{ active: activeCategoryKey === cat.key }"
            @tap="switchCategory(cat.key)"
          >
            {{ cat.label }}
          </view>
        </scroll-view>
      </view>

      <view class="product-panel" :class="{ 'has-sub-cats': subCategoryTabs.length > 0 }">
        <scroll-view
          v-if="subCategoryTabs.length > 0"
          scroll-x
          class="sub-cat-scroll"
          :show-scrollbar="false"
        >
          <view class="sub-cat-row">
            <text
              v-for="tab in subCategoryTabs"
              :key="tab.key"
              class="sub-cat-chip"
              :class="{ active: subCategoryFilter === tab.key }"
              @tap="switchSubCategory(tab.key)"
            >{{ tab.label }}</text>
          </view>
        </scroll-view>

        <scroll-view scroll-y class="product-scroll" :show-scrollbar="false">
          <view v-if="loading" class="loading-wrap">
            <u-loading-icon text="加载中" />
          </view>
          <view v-else-if="loadError" class="empty-wrap">
            <text class="empty-title">{{ loadError }}</text>
            <view class="empty-btn" @tap="handleRetry">重新加载</view>
          </view>
          <view v-else-if="products.length === 0" class="empty-wrap">
            <text class="empty-title">{{ emptyTitle }}</text>
            <text class="empty-hint">{{ emptyHint }}</text>
          </view>
          <view v-else class="product-list">
            <view
              v-for="item in products"
              :key="item.id"
              class="product-row"
              :class="{ selected: cartCount(item.id) > 0 }"
              @tap="openEntry(item)"
            >
              <image
                v-if="item.imageUrl"
                class="product-thumb"
                :src="resolveMediaUrl(item.imageUrl)"
                mode="aspectFill"
              />
              <AppIcon v-else class="product-thumb-icon" name="product" tone="green" :size="22" />
              <view class="product-info">
                <text class="product-name">{{ item.name }}</text>
                <text class="product-meta">{{ formatSaleUnits(item) }}</text>
              </view>
              <view class="add-btn" :class="{ picked: cartCount(item.id) > 0 }" @tap.stop="openEntry(item)">+</view>
            </view>
          </view>
        </scroll-view>
      </view>
    </view>

    <view class="boss-bottom-bar">
      <view class="boss-tool-item" @tap="openBatchModal">
        <AppIcon class="boss-tool-icon" name="batch" tone="green" :size="22" :tile-size="54" :radius="14" />
        <text class="boss-tool-label">批量录入</text>
      </view>
      <view class="boss-tool-item" @tap="openVoiceModal">
        <AppIcon class="boss-tool-icon" name="voice" tone="green" :size="22" :tile-size="54" :radius="14" />
        <text class="boss-tool-label">语音录入</text>
      </view>
      <view class="boss-tool-item" @tap="openPhotoImportMenu">
        <AppIcon class="boss-tool-icon" name="camera" tone="green" :size="22" :tile-size="54" :radius="14" />
        <text class="boss-tool-label">拍照导入</text>
      </view>
      <button class="boss-primary-btn" @tap="goCart">
        购物车{{ cartStore.totalKinds > 0 ? `(${cartStore.totalKinds})` : '' }}
      </button>
    </view>

    <CustomerTabBar active="home" />

    <u-popup :show="showBatchModal" mode="bottom" round="16" @close="closeBatchModal">
      <view class="batch-panel">
        <view class="batch-head">
          <text class="batch-close" @tap="closeBatchModal">×</text>
          <text class="batch-title">批量录入商品</text>
          <text class="batch-single" @tap="closeBatchModal">返回选品</text>
        </view>
        <textarea
          class="batch-textarea"
          :value="batchText"
          placeholder="粘贴示例：土豆5斤，番茄12.5斤..."
          maxlength="-1"
          :disabled="batchParsing"
          @input="onBatchTextInput"
        />
        <button class="batch-recognize-btn" :loading="batchParsing" @tap="recognizeBatchText">识别并确认</button>
      </view>
    </u-popup>

    <u-popup :show="showConfirmModal" mode="bottom" round="16" @close="closeConfirmModal">
      <view class="confirm-panel">
        <view class="batch-head">
          <text class="batch-close" @tap="closeConfirmModal">×</text>
          <text class="batch-title">确认识别结果</text>
          <text class="batch-single" @tap="closeConfirmModal">取消</text>
        </view>
        <image
          v-if="confirmImage"
          class="confirm-image"
          :src="confirmImage"
          mode="aspectFit"
        />
        <text class="confirm-hint">{{ confirmHint }}</text>
        <textarea
          class="batch-textarea confirm-textarea"
          :value="confirmText"
          placeholder="请输入或修正识别文本，如：土豆5斤，番茄2斤"
          maxlength="-1"
          :disabled="confirmApplying"
          @input="onConfirmTextInput"
        />
        <view v-if="confirmPreview.length > 0" class="confirm-preview">
          <view
            v-for="(line, index) in confirmPreview"
            :key="`${line.name}-${index}`"
            class="confirm-line"
            :class="{ unmatched: !line.matched }"
          >
            <text class="confirm-line-name">{{ line.matched ? line.productName : line.name }}</text>
            <text class="confirm-line-qty">{{ line.qty }}{{ line.unit }}</text>
            <text class="confirm-line-status">{{ line.matched ? '已匹配' : '未匹配' }}</text>
          </view>
        </view>
        <view v-else-if="confirmText.trim()" class="confirm-empty">未识别到有效商品行，请检查格式</view>
        <view class="confirm-actions">
          <button class="confirm-secondary-btn" :disabled="confirmApplying" @tap="reparseConfirmText">重新识别</button>
          <button class="confirm-primary-btn" :loading="confirmApplying" @tap="confirmImport">确认录入</button>
        </view>
      </view>
    </u-popup>

    <u-popup :show="showVoiceModal" mode="bottom" round="16" @close="closeVoiceModal">
      <view class="batch-panel">
        <view class="batch-head">
          <text class="batch-close" @tap="closeVoiceModal">×</text>
          <text class="batch-title">语音录入</text>
        </view>
        <text class="voice-hint">点击输入框，使用键盘「语音」说话，如：土豆5斤，番茄2斤</text>
        <textarea
          class="batch-textarea voice-textarea"
          :value="voiceText"
          placeholder="说完后点下方「识别」"
          maxlength="-1"
          :disabled="voiceParsing"
          :focus="voiceInputFocus"
          @input="onVoiceTextInput"
        />
        <button class="batch-recognize-btn" :loading="voiceParsing" @tap="recognizeVoiceText">识别并确认</button>
      </view>
    </u-popup>

    <u-popup :show="showUnitPicker" mode="bottom" round="16" @close="closeUnitPicker">
      <view class="unit-panel">
        <view class="unit-head">
          <text class="unit-cancel" @tap="closeUnitPicker">取消</text>
          <text class="unit-title">选择单位</text>
          <text class="unit-placeholder" />
        </view>
        <text v-if="entryProduct" class="unit-product">{{ entryProduct.name }}</text>
        <scroll-view scroll-y class="unit-grid-wrap">
          <view class="unit-grid">
            <view
              v-for="unit in entryUnits"
              :key="unit"
              class="unit-chip"
              :class="{ active: entryUnit === unit }"
              @tap="applyEntryUnit(unit)"
            >
              {{ unit }}
            </view>
          </view>
        </scroll-view>
      </view>
    </u-popup>

    <view v-if="showEntry && entryProduct" class="order-mask">
      <view class="order-sheet">
        <view class="order-head">
          <text class="order-close" @tap="closeEntry">×</text>
          <text class="order-title">商品下单</text>
          <view class="order-unit" @tap="openUnitFromEntry">
            <text>{{ entryUnit }}</text>
            <text class="order-unit-arrow">▼</text>
          </view>
        </view>

        <view class="order-product-row">
          <text class="order-product-name">{{ entryProduct.name }}</text>
        </view>

        <view class="order-fields">
          <view class="order-field full" @tap="focusEntryQty">
            <text class="field-label">下单数 ({{ entryUnit }})</text>
            <view class="field-box active">
              <text v-if="entryQty" class="field-value">{{ entryQty }}</text>
              <text v-else class="field-placeholder">0</text>
            </view>
          </view>
        </view>

        <view class="order-footer-row">
          <text class="remark-link" @tap="openEntryRemark">添加备注</text>
        </view>
        <text v-if="entryRemark" class="order-remark-preview">备注：{{ entryRemark }}</text>

        <view class="keypad">
          <view class="keypad-grid">
            <view class="key" @tap="inputEntryKey('1')">1</view>
            <view class="key" @tap="inputEntryKey('2')">2</view>
            <view class="key" @tap="inputEntryKey('3')">3</view>
            <view class="key fn" @tap="backspaceEntry">⌫</view>

            <view class="key" @tap="inputEntryKey('4')">4</view>
            <view class="key" @tap="inputEntryKey('5')">5</view>
            <view class="key" @tap="inputEntryKey('6')">6</view>
            <view class="key fn" @tap="clearEntryQty">清零</view>

            <view class="key" @tap="inputEntryKey('7')">7</view>
            <view class="key" @tap="inputEntryKey('8')">8</view>
            <view class="key" @tap="inputEntryKey('9')">9</view>
            <view class="key confirm" @tap="confirmEntry">确定</view>

            <view class="key" @tap="inputEntryKey('.')">.</view>
            <view class="key" @tap="inputEntryKey('0')">0</view>
            <view class="key blank" />
            <view class="key blank" />
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, nextTick, ref } from 'vue'
import { onLoad, onReady, onShow } from '@dcloudio/uni-app'
import { parseOrderExcel } from '../../../api/orderImport'
import { fetchBindStatus } from '../../../api/customer'
import { fetchCustomerCategories, fetchCustomerProducts, type CategoryItem, type ProductItem } from '../../../api/product'
import AppIcon from '../../../components/AppIcon.vue'
import CustomerTabBar from '../../../components/CustomerTabBar.vue'
import { parseSaleUnits } from '../../../constants/units'
import { useCartStore } from '../../../stores/cart'
import { useUserStore } from '../../../stores/user'
import { buildPrimarySidebar, getParentCategory } from '../../../utils/category'
import { customerTabBarHeightPx } from '../../../utils/customer-nav'
import { chooseImportExcel, chooseImportImage, openPhotoImportMenu as showPhotoImportMenu } from '../../../utils/orderImport'
import { resolveMediaUrl } from '../../../utils/media'
import {
  applyParsedLines,
  parseOrderText,
  previewParsedLines,
  type ParsedPreviewLine,
} from '../../../utils/parseOrderText'

const userStore = useUserStore()
const cartStore = useCartStore()
const categories = ref<CategoryItem[]>([])
const products = ref<ProductItem[]>([])
const allProducts = ref<ProductItem[]>([])
const activeCategoryKey = ref('all')
const subCategoryFilter = ref('all')
const keyword = ref('')
const loading = ref(false)
const loadError = ref('')
const showBatchModal = ref(false)
const batchText = ref('')
const batchParsing = ref(false)
const showVoiceModal = ref(false)
const voiceText = ref('')
const voiceParsing = ref(false)
const voiceInputFocus = ref(false)
const showConfirmModal = ref(false)
const confirmText = ref('')
const confirmImage = ref('')
const confirmPreview = ref<ParsedPreviewLine[]>([])
const confirmApplying = ref(false)
const confirmSource = ref<'batch' | 'voice' | 'photo' | 'excel'>('batch')
const showEntry = ref(false)
const showUnitPicker = ref(false)
const entryProduct = ref<ProductItem | null>(null)
const entryUnit = ref('斤')
const entryQty = ref('')
const entryRemark = ref('')
const mainHeight = ref(0)
let fetching = false

const mainStyle = computed(() => (mainHeight.value > 0 ? { height: `${mainHeight.value}px` } : {}))

const categoryTabs = computed(() => buildPrimarySidebar(categories.value))

const subCategoryTabs = computed(() => {
  const parent = getParentCategory(categories.value, activeCategoryKey.value)
  if (!parent?.children?.length) return []
  return [
    { key: 'all', label: '全部' },
    ...parent.children.map((child) => ({ key: String(child.id), label: child.name })),
  ]
})

const emptyTitle = computed(() => {
  if (keyword.value.trim()) return '未找到匹配商品'
  if (activeCategoryKey.value !== 'all') return '该分类暂无可下单商品'
  return '暂无可下单商品'
})

const emptyHint = computed(() => {
  if (keyword.value.trim()) return '换个关键词试试'
  return ''
})

const confirmHint = computed(() => {
  if (confirmImage.value) {
    return '请对照图片，在下方输入或修正识别文本'
  }
  if (confirmSource.value === 'excel') {
    return 'Excel 已解析为文本，请确认或修改后再录入'
  }
  return '请确认识别原文，可手动修改后再录入购物车'
})

const entryUnits = computed(() => {
  if (!entryProduct.value) return []
  return parseSaleUnits(entryProduct.value.saleUnits, entryProduct.value.unit)
})

onLoad(async () => {
  if (!userStore.isLoggedIn || !userStore.isCustomer) {
    uni.reLaunch({ url: '/pages/login/index' })
    return
  }
  await syncCustomerProfile()
  await refreshPage()
})

onReady(() => {
  updateMainHeight()
})

onShow(async () => {
  if (!userStore.isLoggedIn || !userStore.isCustomer) {
    uni.reLaunch({ url: '/pages/login/index' })
    return
  }
  await nextTick()
  updateMainHeight()
})

async function syncCustomerProfile() {
  try {
    const status = await fetchBindStatus()
    if (status.bound && status.customerId) {
      userStore.applyCustomerBind(status.customerId, status.customerName)
    }
  } catch {
    // ignore
  }
}

async function refreshPage() {
  if (fetching) return
  fetching = true
  try {
    await loadCategories()
    await loadProducts()
  } finally {
    fetching = false
    await nextTick()
    updateMainHeight()
  }
}

async function loadCategories() {
  categories.value = await fetchCustomerCategories()
}

function resolveCategoryId(): number | undefined {
  if (activeCategoryKey.value === 'all') return undefined
  if (subCategoryFilter.value !== 'all') {
    return Number(subCategoryFilter.value)
  }
  if (activeCategoryKey.value === 'uncategorized') return undefined
  return Number(activeCategoryKey.value)
}

async function loadProducts() {
  loading.value = true
  loadError.value = ''
  try {
    if (activeCategoryKey.value === 'uncategorized') {
      products.value = (await fetchCustomerProducts({ keyword: keyword.value || undefined }))
        .filter((item) => !item.categoryId)
      return
    }
    const categoryId = resolveCategoryId()
    products.value = await fetchCustomerProducts({
      categoryId,
      keyword: keyword.value || undefined,
    })
  } catch (e) {
    products.value = []
    loadError.value = e instanceof Error ? e.message : '加载失败'
  } finally {
    loading.value = false
  }
}

async function ensureAllProducts() {
  if (allProducts.value.length > 0) return
  allProducts.value = await fetchCustomerProducts()
}

function switchCategory(key: string) {
  activeCategoryKey.value = key
  subCategoryFilter.value = 'all'
  loadProducts()
}

function switchSubCategory(key: string) {
  subCategoryFilter.value = key
  loadProducts()
}

function onKeywordInput(e: { detail: { value: string } }) {
  keyword.value = e.detail.value
}

function clearKeyword() {
  keyword.value = ''
  loadProducts()
}

function handleRetry() {
  refreshPage()
}

function formatSaleUnits(item: ProductItem) {
  return parseSaleUnits(item.saleUnits, item.unit).join('/')
}

function cartCount(productId: number) {
  return cartStore.items
    .filter((item) => item.productId === productId)
    .reduce((sum, item) => sum + item.qty, 0)
}

function openEntry(item: ProductItem) {
  const allowed = parseSaleUnits(item.saleUnits, item.unit)
  if (allowed.length > 1) {
    const existingLines = cartStore.items.filter((line) => line.productId === item.id)
    const labels = allowed.map((unit) => {
      const line = existingLines.find((entry) => entry.unit === unit)
      return line ? `${unit}（已选${line.qty}）` : unit
    })
    uni.showActionSheet({
      itemList: labels,
      success: (res) => {
        openEntryWithUnit(item, allowed[res.tapIndex])
      },
    })
    return
  }
  openEntryWithUnit(item, allowed[0] || item.unit || '斤')
}

function openEntryWithUnit(item: ProductItem, unit: string) {
  const existing = cartStore.items.find((line) => line.productId === item.id && line.unit === unit)
  entryProduct.value = item
  entryUnit.value = unit
  entryQty.value = existing ? String(existing.qty) : ''
  entryRemark.value = existing?.remark || ''
  showEntry.value = true
}

function closeEntry() {
  showEntry.value = false
  entryProduct.value = null
  entryQty.value = ''
  entryRemark.value = ''
}

function focusEntryQty() {}

function inputEntryKey(key: string) {
  if (key === '.' && entryQty.value.includes('.')) return
  if (entryQty.value === '0' && key !== '.') {
    entryQty.value = key
    return
  }
  entryQty.value += key
}

function backspaceEntry() {
  entryQty.value = entryQty.value.slice(0, -1)
}

function clearEntryQty() {
  entryQty.value = ''
}

function openUnitFromEntry() {
  if (entryUnits.value.length <= 1) {
    uni.showToast({ title: '仅一种售卖单位', icon: 'none' })
    return
  }
  showUnitPicker.value = true
}

function closeUnitPicker() {
  showUnitPicker.value = false
}

function applyEntryUnit(unit: string) {
  if (!entryProduct.value) return
  const existing = cartStore.items.find(
    (line) => line.productId === entryProduct.value!.id && line.unit === unit,
  )
  entryUnit.value = unit
  entryQty.value = existing ? String(existing.qty) : entryQty.value
  entryRemark.value = existing?.remark || entryRemark.value
  closeUnitPicker()
}

function openEntryRemark() {
  uni.showModal({
    title: '商品备注',
    editable: true,
    placeholderText: '如：要大一点的、小土豆',
    content: entryRemark.value,
    success: (res) => {
      if (res.confirm && res.content != null) {
        entryRemark.value = res.content.trim()
      }
    },
  })
}

function confirmEntry() {
  if (!entryProduct.value) return
  const qty = Number(entryQty.value)
  if (!qty || qty <= 0) {
    uni.showToast({ title: '请输入下单数量', icon: 'none' })
    return
  }
  cartStore.upsertLine({
    productId: entryProduct.value.id,
    name: entryProduct.value.name,
    unit: entryUnit.value,
    qty,
    remark: entryRemark.value.trim() || undefined,
  })
  closeEntry()
  uni.showToast({ title: '已加入购物车', icon: 'success' })
}

function goCart() {
  if (cartStore.items.length === 0) {
    uni.showToast({ title: '请先选择商品', icon: 'none' })
    return
  }
  uni.navigateTo({ url: '/pages/customer/cart/index' })
}

function openBatchModal() {
  showBatchModal.value = true
}

function closeBatchModal() {
  showBatchModal.value = false
}

function onBatchTextInput(e: { detail: { value: string } }) {
  batchText.value = e.detail.value
}

function openVoiceModal() {
  voiceText.value = ''
  showVoiceModal.value = true
  voiceInputFocus.value = false
  setTimeout(() => {
    voiceInputFocus.value = true
  }, 200)
}

function closeVoiceModal() {
  voiceInputFocus.value = false
  showVoiceModal.value = false
}

function onVoiceTextInput(e: { detail: { value: string } }) {
  voiceText.value = e.detail.value
}

async function handleRecognizedText(text: string) {
  const parsed = parseOrderText(text)
  if (parsed.length === 0) {
    uni.showToast({ title: '未识别到商品，请检查格式', icon: 'none' })
    return false
  }
  await ensureAllProducts()
  const result = applyParsedLines(parsed, allProducts.value, (line) => {
    cartStore.upsertLine({
      productId: line.productId,
      name: line.productName,
      unit: line.unit,
      qty: line.orderQty,
    })
  })
  if (result.added === 0) {
    uni.showToast({
      title: `未匹配：${result.unmatched.slice(0, 3).join('、')}`,
      icon: 'none',
    })
    return false
  }
  const extra = result.unmatched.length > 0 ? `，${result.unmatched.length}项未匹配` : ''
  uni.showToast({ title: `已录入${result.added}种商品${extra}`, icon: 'success' })
  return true
}

function openConfirmModal(
  text: string,
  source: 'batch' | 'voice' | 'photo' | 'excel',
  imagePath = '',
) {
  confirmText.value = text
  confirmImage.value = imagePath
  confirmSource.value = source
  confirmPreview.value = []
  showConfirmModal.value = true
  reparseConfirmText()
}

function closeConfirmModal() {
  showConfirmModal.value = false
  confirmText.value = ''
  confirmImage.value = ''
  confirmPreview.value = []
}

function onConfirmTextInput(e: { detail: { value: string } }) {
  confirmText.value = e.detail.value
}

async function reparseConfirmText() {
  const text = confirmText.value.trim()
  if (!text) {
    confirmPreview.value = []
    return
  }
  const parsed = parseOrderText(text)
  if (parsed.length === 0) {
    confirmPreview.value = []
    uni.showToast({ title: '未识别到商品，请检查格式', icon: 'none' })
    return
  }
  await ensureAllProducts()
  confirmPreview.value = previewParsedLines(parsed, allProducts.value)
}

async function confirmImport() {
  const text = confirmText.value.trim()
  if (!text) {
    uni.showToast({ title: '请先输入识别文本', icon: 'none' })
    return
  }
  confirmApplying.value = true
  try {
    const ok = await handleRecognizedText(text)
    if (!ok) return
    batchText.value = ''
    voiceText.value = ''
    closeBatchModal()
    closeVoiceModal()
    closeConfirmModal()
  } finally {
    confirmApplying.value = false
  }
}

function openPhotoImportMenu() {
  showPhotoImportMenu((index) => {
    if (index === 0) pickImportImage(['camera'])
    else if (index === 1) pickImportImage(['album'])
    else if (index === 2) pickImportExcel()
    else openConfirmModal('', 'photo')
  })
}

async function pickImportImage(sourceType: ('camera' | 'album')[]) {
  try {
    const path = await chooseImportImage(sourceType)
    openConfirmModal('', 'photo', path)
  } catch (e) {
    if (e instanceof Error && e.message === '已取消') return
    uni.showToast({ title: e instanceof Error ? e.message : '选择图片失败', icon: 'none' })
  }
}

async function pickImportExcel() {
  try {
    const path = await chooseImportExcel()
    uni.showLoading({ title: '解析中' })
    const result = await parseOrderExcel(path)
    openConfirmModal(result.text, 'excel')
  } catch (e) {
    if (e instanceof Error && e.message === '已取消') return
    uni.showToast({ title: e instanceof Error ? e.message : 'Excel 解析失败', icon: 'none' })
  } finally {
    uni.hideLoading()
  }
}

async function recognizeBatchText() {
  const text = batchText.value.trim()
  if (!text) {
    uni.showToast({ title: '请先粘贴或输入商品', icon: 'none' })
    return
  }
  batchParsing.value = true
  try {
    closeBatchModal()
    openConfirmModal(text, 'batch')
  } finally {
    batchParsing.value = false
  }
}

async function recognizeVoiceText() {
  const text = voiceText.value.trim()
  if (!text) {
    uni.showToast({ title: '请先说话或输入内容', icon: 'none' })
    return
  }
  voiceParsing.value = true
  try {
    closeVoiceModal()
    openConfirmModal(text, 'voice')
  } finally {
    voiceParsing.value = false
  }
}

function goBind() {
  uni.navigateTo({ url: '/pages/customer/bind/index' })
}

function updateMainHeight() {
  const { windowHeight } = uni.getSystemInfoSync()
  uni
    .createSelectorQuery()
    .select('.page-head')
    .boundingClientRect()
    .select('.boss-bottom-bar')
    .boundingClientRect()
    .exec((res) => {
      const headRect = res[0]
      const bottomRect = res[1]
      const headHeight = Array.isArray(headRect) ? headRect[0]?.height : headRect?.height
      const bottomHeight = Array.isArray(bottomRect) ? bottomRect[0]?.height : bottomRect?.height
      if (typeof headHeight !== 'number') return
      const footerHeight = typeof bottomHeight === 'number' ? bottomHeight : uni.upx2px(128)
      const tabBarHeight = customerTabBarHeightPx()
      mainHeight.value = Math.max(0, windowHeight - headHeight - footerHeight - tabBarHeight)
    })
}
</script>

<style scoped lang="scss">
@import '../../../styles/boss-footer.scss';

.customer-order-page .boss-bottom-bar {
  bottom: calc(100rpx + env(safe-area-inset-bottom));
  padding: 12rpx 24rpx;
  z-index: 85;
}

.customer-order-page.boss-page {
  height: 100%;
  min-height: auto;
  max-height: 100%;
  overflow: hidden;
  background: #fff;
}

.page-head {
  flex-shrink: 0;
}

.main {
  flex: none;
  flex-direction: row;
  align-items: stretch;
  display: flex;
  min-height: 0;
  overflow: hidden;
  box-sizing: border-box;
}

.category-col {
  width: 168rpx;
  flex-shrink: 0;
  height: 100%;
  background: #f5f6f7;
}

.categories {
  width: 100%;
  height: 100%;
  background: #f5f6f7;
}

.banner {
  flex-shrink: 0;
  margin: 16rpx 24rpx 0;
  background: #0b7f3a;
  padding: 24rpx 28rpx;
  color: #fff;
  border-radius: 16rpx;
}

.banner-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.welcome {
  font-size: 34rpx;
  font-weight: 700;
}

.hint {
  display: block;
  margin-top: 12rpx;
  font-size: 24rpx;
  line-height: 1.45;
  opacity: 0.92;
}

.bind-link {
  display: block;
  margin-top: 12rpx;
  font-size: 24rpx;
  font-weight: 600;
  text-decoration: underline;
}

.search-box {
  padding: 16rpx 24rpx;
  background: #f5f6f7;
  flex-shrink: 0;
}

.search-wrap {
  position: relative;
  display: flex;
  align-items: center;
}

.search-icon {
  position: absolute;
  left: 24rpx;
  font-size: 28rpx;
  color: #999;
  z-index: 1;
}

.search-input {
  flex: 1;
  height: 72rpx;
  padding: 0 64rpx;
  background: #fff;
  border-radius: 999rpx;
  font-size: 28rpx;
  color: #222;
  box-sizing: border-box;
}

.search-clear {
  position: absolute;
  right: 20rpx;
  width: 40rpx;
  height: 40rpx;
  line-height: 40rpx;
  text-align: center;
  font-size: 32rpx;
  color: #bbb;
}

.cat-item {
  padding: 32rpx 12rpx;
  font-size: 26rpx;
  color: #666;
  text-align: center;
  line-height: 1.3;
}

.cat-item.active {
  background: #fff;
  color: #07c160;
  font-weight: 600;
}

.product-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
  height: 100%;
  background: #fff;
}

.product-scroll {
  flex: none;
  height: 100%;
  width: 100%;
  background: #fff;
  box-sizing: border-box;
}

.product-panel.has-sub-cats .product-scroll {
  height: calc(100% - 82rpx);
}

.sub-cat-scroll {
  flex-shrink: 0;
  height: 82rpx;
  white-space: nowrap;
  background: #fff;
  border-bottom: 1rpx solid #f0f0f0;
}

.sub-cat-row {
  display: inline-flex;
  gap: 16rpx;
  padding: 16rpx 20rpx;
}

.sub-cat-chip {
  flex-shrink: 0;
  padding: 10rpx 24rpx;
  font-size: 26rpx;
  color: #666;
  background: #f5f6f7;
  border-radius: 999rpx;
}

.sub-cat-chip.active {
  color: #07c160;
  background: #e8f8ef;
  font-weight: 600;
}

.loading-wrap,
.empty-wrap {
  padding: 80rpx 24rpx;
  text-align: center;
}

.empty-title {
  display: block;
  font-size: 30rpx;
  color: #333;
  font-weight: 600;
}

.empty-hint {
  display: block;
  margin-top: 12rpx;
  font-size: 26rpx;
  color: #999;
}

.empty-btn {
  display: inline-block;
  margin-top: 24rpx;
  padding: 16rpx 40rpx;
  font-size: 28rpx;
  color: #07c160;
  border: 2rpx solid #07c160;
  border-radius: 999rpx;
}

.product-row {
  display: flex;
  align-items: center;
  padding: 24rpx;
  border-bottom: 1rpx solid #f0f0f0;
  gap: 16rpx;
  box-sizing: border-box;
}

.product-row.selected {
  background: #f8fcf9;
}

.product-thumb {
  flex-shrink: 0;
  width: 72rpx;
  height: 72rpx;
  border-radius: 14rpx;
  background: #f5f6f7;
}

.product-thumb-icon {
  flex-shrink: 0;
  width: 72rpx;
  height: 72rpx;
}

.product-info {
  flex: 1;
  min-width: 0;
}

.product-name {
  display: block;
  font-size: 32rpx;
  font-weight: 500;
  color: #222;
  line-height: 1.35;
}

.product-meta {
  display: block;
  margin-top: 8rpx;
  font-size: 26rpx;
  color: #999;
  line-height: 1.35;
}

.add-btn {
  flex-shrink: 0;
  width: 60rpx;
  height: 60rpx;
  border-radius: 50%;
  background: #07c160;
  color: #fff;
  font-size: 44rpx;
  line-height: 56rpx;
  text-align: center;
}

.add-btn.picked {
  background: #05a850;
  box-shadow: 0 4rpx 12rpx rgba(7, 193, 96, 0.35);
}

.unit-panel {
  max-height: 60vh;
  display: flex;
  flex-direction: column;
  background: #fff;
  padding-bottom: env(safe-area-inset-bottom);
}

.unit-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 28rpx 32rpx 12rpx;
}

.unit-cancel {
  min-width: 80rpx;
  font-size: 28rpx;
  color: #666;
}

.unit-title {
  font-size: 32rpx;
  font-weight: 600;
}

.unit-placeholder {
  min-width: 80rpx;
}

.unit-product {
  display: block;
  padding: 0 32rpx 16rpx;
  font-size: 28rpx;
  color: #666;
}

.unit-grid-wrap {
  max-height: 48vh;
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
  border-radius: 14rpx;
  border: 2rpx solid transparent;
  box-sizing: border-box;
}

.unit-chip.active {
  color: #07c160;
  background: #e8f8ef;
  border-color: #07c160;
  font-weight: 600;
}

.order-mask {
  position: fixed;
  inset: 0;
  z-index: 200;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
}

.order-sheet {
  background: #fff;
  border-radius: 24rpx 24rpx 0 0;
  padding-bottom: env(safe-area-inset-bottom);
}

.order-head {
  display: flex;
  align-items: center;
  padding: 24rpx 32rpx 8rpx;
}

.order-close {
  width: 80rpx;
  font-size: 48rpx;
  color: #999;
  line-height: 1;
}

.order-title {
  flex: 1;
  text-align: center;
  font-size: 34rpx;
  font-weight: 600;
}

.order-unit {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 4rpx;
  min-width: 80rpx;
  font-size: 28rpx;
  color: #07c160;
}

.order-unit-arrow {
  font-size: 18rpx;
}

.order-product-row {
  padding: 8rpx 32rpx 20rpx;
}

.order-product-name {
  font-size: 30rpx;
  color: #666;
}

.order-fields {
  display: flex;
  gap: 20rpx;
  padding: 0 32rpx;
}

.order-field {
  flex: 1;
}

.order-field.full {
  flex: 1;
}

.field-label {
  display: block;
  margin-bottom: 12rpx;
  font-size: 24rpx;
  color: #999;
}

.field-box {
  height: 96rpx;
  padding: 0 24rpx;
  display: flex;
  align-items: center;
  background: #f5f6f7;
  border-radius: 12rpx;
  border: 2rpx solid transparent;
}

.field-box.active {
  background: #fff;
  border-color: #07c160;
}

.field-value {
  font-size: 40rpx;
  font-weight: 600;
  color: #222;
}

.field-placeholder {
  font-size: 32rpx;
  color: #ccc;
}

.order-footer-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24rpx 32rpx 8rpx;
}

.remark-link {
  font-size: 28rpx;
  color: #666;
}

.order-remark-preview {
  display: block;
  padding: 0 32rpx 16rpx;
  font-size: 24rpx;
  color: #e67e22;
}

.keypad {
  background: #eef0f2;
  padding: 12rpx;
  border-top: 1rpx solid #ddd;
}

.keypad-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  grid-template-rows: repeat(4, 96rpx);
  gap: 8rpx;
}

.key {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
  border-radius: 8rpx;
  font-size: 36rpx;
  font-weight: 500;
}

.key.fn {
  font-size: 26rpx;
  color: #666;
}

.key.confirm {
  grid-row: 3 / 5;
  grid-column: 4;
  background: #07c160;
  color: #fff;
  font-size: 32rpx;
  font-weight: 600;
}

.key.blank {
  visibility: hidden;
  pointer-events: none;
}

.batch-panel {
  padding: 0 32rpx calc(32rpx + env(safe-area-inset-bottom));
  background: #fff;
}

.batch-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 28rpx 0 20rpx;
}

.batch-close {
  width: 56rpx;
  font-size: 44rpx;
  color: #999;
  line-height: 1;
}

.batch-title {
  font-size: 34rpx;
  font-weight: 600;
  color: #222;
}

.batch-single {
  min-width: 120rpx;
  text-align: right;
  font-size: 28rpx;
  color: #2979ff;
}

.batch-textarea {
  width: 100%;
  min-height: 280rpx;
  padding: 24rpx;
  background: #f5f6f7;
  border-radius: 16rpx;
  font-size: 28rpx;
  color: #333;
  box-sizing: border-box;
}

.batch-recognize-btn {
  width: 100%;
  height: 88rpx;
  line-height: 88rpx;
  margin: 24rpx 0 0;
  padding: 0;
  background: #07c160;
  color: #fff;
  font-size: 32rpx;
  font-weight: 600;
  border-radius: 12rpx;
  border: none;
}

.batch-recognize-btn::after {
  border: none;
}

.voice-hint {
  display: block;
  margin-bottom: 16rpx;
  font-size: 26rpx;
  line-height: 1.5;
  color: #66736b;
}

.voice-textarea {
  min-height: 200rpx;
}

.customer-order-page .boss-tool-item {
  width: 88rpx;
}

.customer-order-page .boss-tool-label {
  font-size: 22rpx;
}

.confirm-panel {
  padding: 0 32rpx calc(32rpx + env(safe-area-inset-bottom));
  background: #fff;
}

.confirm-image {
  width: 100%;
  height: 280rpx;
  margin-bottom: 16rpx;
  border-radius: 12rpx;
  background: #f5f6f7;
}

.confirm-hint {
  display: block;
  margin-bottom: 16rpx;
  font-size: 26rpx;
  line-height: 1.5;
  color: #66736b;
}

.confirm-textarea {
  min-height: 200rpx;
}

.confirm-preview {
  margin-top: 20rpx;
  max-height: 320rpx;
  overflow-y: auto;
}

.confirm-line {
  display: flex;
  align-items: center;
  gap: 12rpx;
  padding: 16rpx 0;
  border-bottom: 1rpx solid #eef2ed;
}

.confirm-line.unmatched {
  background: #fff8f0;
}

.confirm-line-name {
  flex: 1;
  font-size: 28rpx;
  color: #222;
}

.confirm-line-qty {
  font-size: 28rpx;
  color: #0b7f3a;
  font-weight: 600;
}

.confirm-line-status {
  font-size: 24rpx;
  color: #0b7f3a;
}

.confirm-line.unmatched .confirm-line-status {
  color: #c2352a;
}

.confirm-empty {
  margin-top: 20rpx;
  font-size: 26rpx;
  color: #999;
  text-align: center;
}

.confirm-actions {
  display: flex;
  gap: 20rpx;
  margin-top: 24rpx;
}

.confirm-secondary-btn,
.confirm-primary-btn {
  flex: 1;
  height: 88rpx;
  line-height: 88rpx;
  margin: 0;
  padding: 0;
  font-size: 30rpx;
  border-radius: 12rpx;
  border: none;
}

.confirm-secondary-btn {
  background: #f5f6f7;
  color: #333;
}

.confirm-primary-btn {
  background: #07c160;
  color: #fff;
  font-weight: 600;
}

.confirm-secondary-btn::after,
.confirm-primary-btn::after {
  border: none;
}
</style>
