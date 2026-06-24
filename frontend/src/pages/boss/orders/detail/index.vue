<template>
  <view class="page">
    <view v-if="loading" class="loading-wrap">
      <u-loading-icon text="加载中" />
    </view>

    <template v-else-if="order">
      <scroll-view scroll-y class="scroll-body">
        <view class="status-row">
          <view class="status-chip">
            <AppIcon name="order" tone="gray" :size="16" :tile-size="36" :radius="10" :tile="true" />
            <text>{{ order.printed ? '已对账' : '待对账' }}</text>
          </view>
          <view class="status-chip pay-display" :class="paymentStatusClass">
            <AppIcon
              name="salesPayment"
              :tone="isOrderPaid ? 'green' : 'red'"
              :size="16"
              :tile-size="36"
              :radius="10"
              :tile="true"
            />
            <text>{{ paymentStatusText }}</text>
          </view>
        </view>

        <view class="info-card">
          <view class="info-head">
            <view class="name-wrap">
              <text class="customer-name">{{ order.customerName || '未知客户' }}</text>
              <text v-if="isTemporaryOrder" class="temp-tag">临时</text>
            </view>
            <text v-if="order.sourceLabel" class="source-tag">{{ order.sourceLabel }}</text>
          </view>

          <view class="delivery-line">
            <text class="delivery-label">配送时间：</text>
            <text class="delivery-value">{{ deliveryTimeText }}</text>
          </view>

          <view class="info-grid">
            <view class="info-row">
              <text class="info-label">订单编号</text>
              <text class="info-value">{{ order.orderNo }}</text>
            </view>
            <view class="info-row">
              <text class="info-label">下单时间</text>
              <text class="info-value">{{ formatTime(order.createdAt) }}</text>
            </view>
            <view class="info-row">
              <text class="info-label">收货信息</text>
              <text class="info-value">{{ receiverInfo }}</text>
            </view>
            <view class="info-row">
              <text class="info-label">订单状态</text>
              <text class="info-value status-text">{{ order.statusLabel }}</text>
            </view>
            <view v-if="order.pickedByWorkerCode" class="info-row">
              <text class="info-label">拣单配送员</text>
              <text class="info-value">{{ order.pickedByWorkerCode }}</text>
            </view>
            <view class="info-row">
              <text class="info-label">下单金额</text>
              <text class="info-value">{{ formatMoney(order.amount) }}</text>
            </view>
            <view class="info-row">
              <text class="info-label">销售金额</text>
              <text class="info-value">{{ formatMoney(order.amount) }}</text>
            </view>
          </view>

          <view v-if="order.remark" class="order-remark">备注：{{ order.remark }}</view>

          <view class="finance-grid">
            <view class="finance-cell">
              <text class="finance-label">出库金额</text>
              <text class="finance-value">{{ formatMoney(order.amount) }}</text>
            </view>
            <view class="finance-cell">
              <text class="finance-label">售后金额</text>
              <text class="finance-value muted">—</text>
            </view>
            <view class="finance-cell">
              <text class="finance-label">优惠</text>
              <text class="finance-value muted">—</text>
            </view>
            <view class="finance-cell">
              <text class="finance-label">已收</text>
              <text class="finance-value">{{ formatMoney(order.paidAmount) }}</text>
            </view>
            <view class="finance-cell highlight-green">
              <text class="finance-label">应收</text>
              <text class="finance-value green">{{ formatMoney(order.receivableAmount ?? order.amount) }}</text>
            </view>
            <view class="finance-cell highlight-red">
              <text class="finance-label">欠款</text>
              <text class="finance-value red">{{ formatMoney(order.outstandingAmount) }}</text>
            </view>
          </view>
        </view>

        <view class="items-card">
          <view class="items-head">
            <view class="items-title-wrap">
              <AppIcon name="order" tone="green" :size="18" :tile-size="40" :radius="10" :tile="true" />
              <text class="items-title">共{{ itemKinds }}种商品</text>
            </view>
          </view>

          <view class="table-head">
            <text class="col-name">商品名</text>
            <text class="col-qty">下单数</text>
            <text class="col-stock">库存</text>
            <text class="col-out">拣单数</text>
            <text class="col-price">单价</text>
            <text class="col-sub">小计</text>
          </view>

          <view
            v-for="line in order.items || []"
            :key="line.id || `${line.productId}-${line.unit}`"
            class="table-row"
          >
            <view class="col-name">
              <text class="line-name">{{ line.productName }}</text>
              <text v-if="line.pickRemark" class="line-remark">备注：{{ line.pickRemark }}</text>
            </view>
            <text class="col-qty">{{ line.orderQty }}{{ line.unit }}</text>
            <text class="col-stock" :class="{ warn: isStockInsufficient(line) }">{{ stockText(line) }}</text>
            <text class="col-out" :class="{ done: line.actualQty != null }">{{ outboundQty(line) }}</text>
            <text class="col-price">{{ line.dealPrice != null ? line.dealPrice : '—' }}</text>
            <text class="col-sub">{{ lineSubtotal(line) }}</text>
          </view>

          <view v-if="order.priceIncomplete" class="price-warn-bar">部分商品未录价</view>
        </view>
      </scroll-view>

      <view class="boss-bottom-bar detail-bar">
        <view class="bar-tools">
          <view class="bar-tool" @tap="showMoreActions = true">
            <AppIcon name="batch" tone="gray" :size="18" :tile-size="44" :radius="12" :tile="true" />
            <text>更多</text>
          </view>
          <view v-if="canEditOrder" class="bar-tool" @tap="goEdit">
            <AppIcon name="pricing" tone="gray" :size="18" :tile-size="44" :radius="12" :tile="true" />
            <text>改单</text>
          </view>
        </view>
        <view class="bar-actions">
          <view
            class="bar-print-btn"
            :class="{ primary: canSendStatement }"
            @tap="goPrint"
          >
            {{ printActionLabel }}
          </view>
          <button
            v-if="showPickAction"
            class="bar-primary"
            :class="{ picked: isFullyPicked && pickButtonDisabled }"
            :disabled="pickButtonDisabled"
            @tap="goPick"
          >
            {{ pickButtonLabel }}
          </button>
          <button
            v-else-if="showPriceButton"
            class="bar-primary"
            @tap="goPricing"
          >
            {{ priceButtonLabel }}
          </button>
          <button
            v-else-if="showPaymentAction"
            class="bar-pay"
            :class="{ paid: isOrderPaid }"
            @tap="handlePaymentTap"
          >
            {{ paymentActionLabel }}
          </button>
        </view>
      </view>

      <canvas
        canvas-id="orderDetailNoteCanvas"
        id="orderDetailNoteCanvas"
        class="hidden-canvas"
        :style="{ width: `${DELIVERY_NOTE_CANVAS.width}px`, height: `${statementCanvasHeight}px` }"
      />
    </template>

    <view v-if="showMoreActions" class="more-mask" @tap="showMoreActions = false" />
    <view v-if="showMoreActions" class="more-sheet">
      <view class="more-grid">
        <view
          v-for="item in moreMenuItems"
          :key="item.key"
          class="more-grid-item"
          @tap="handleMoreMenu(item.key)"
        >
          <AppIcon
            :name="item.icon"
            :tone="item.tone"
            :size="22"
            :tile-size="96"
            :radius="20"
            :tile="true"
          />
          <text class="more-grid-label">{{ item.label }}</text>
        </view>
      </view>
      <view class="more-cancel" @tap="showMoreActions = false">取消</view>
    </view>

    <view v-if="showRemarkEditor" class="more-mask" @tap="showRemarkEditor = false" />
    <view v-if="showRemarkEditor" class="remark-sheet">
      <text class="remark-title">订单备注</text>
      <textarea
        v-model="remarkDraft"
        class="remark-input"
        placeholder="输入备注"
        maxlength="200"
        :auto-height="true"
      />
      <view class="remark-actions">
        <view class="remark-btn ghost" @tap="showRemarkEditor = false">取消</view>
        <view class="remark-btn primary" @tap="saveRemark">保存</view>
      </view>
    </view>

    <view v-if="showMarkPayment" class="pay-mask" @tap="closeMarkPayment">
      <view class="pay-sheet" @tap.stop>
        <view class="pay-head">
          <text class="pay-close" @tap="closeMarkPayment">×</text>
          <text class="pay-title">标记收款</text>
          <text class="pay-head-spacer" />
        </view>
        <text class="pay-sales">销售金额：{{ formatMoney(order?.amount) }}元</text>

        <view class="pay-row">
          <text class="pay-row-label">应收</text>
          <text class="pay-row-value">{{ payReceivableText }}</text>
        </view>
        <view
          class="pay-row editable"
          :class="{ active: payActiveField === 'discount' }"
          @tap="payActiveField = 'discount'"
        >
          <text class="pay-row-label">优惠</text>
          <text class="pay-row-value highlight">{{ payDiscountDraft || '0.00' }}</text>
        </view>
        <view
          class="pay-row editable"
          :class="{ active: payActiveField === 'amount' }"
          @tap="payActiveField = 'amount'"
        >
          <text class="pay-row-label">本次收款</text>
          <text class="pay-row-value highlight">{{ payAmountDraft || '0' }}</text>
        </view>

        <view class="pay-method" @tap="pickPayMethod">
          <text class="pay-method-label">收款方式</text>
          <text class="pay-method-value">{{ payMethodLabel }} ›</text>
        </view>

        <view class="pay-keypad">
          <view class="key" @tap="inputPayKey('1')">1</view>
          <view class="key" @tap="inputPayKey('2')">2</view>
          <view class="key" @tap="inputPayKey('3')">3</view>
          <view class="key fn" @tap="backspacePayKey">⌫</view>

          <view class="key" @tap="inputPayKey('4')">4</view>
          <view class="key" @tap="inputPayKey('5')">5</view>
          <view class="key" @tap="inputPayKey('6')">6</view>
          <view class="key fn" @tap="clearPayField">清零</view>

          <view class="key" @tap="inputPayKey('7')">7</view>
          <view class="key" @tap="inputPayKey('8')">8</view>
          <view class="key" @tap="inputPayKey('9')">9</view>
          <view class="key confirm" @tap="submitMarkPayment">确定</view>

          <view class="key" @tap="inputPayKey('.')">.</view>
          <view class="key" @tap="inputPayKey('0')">0</view>
          <view class="key blank" />
          <view class="key blank" />
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { onLoad, onShow } from '@dcloudio/uni-app'
import { computed, ref } from 'vue'
import {
  confirmBossOrder,
  fetchBossOrderDetail,
  markBossOrderPayment,
  updateBossOrder,
  updateBossOrderStatus,
  type OrderInfo,
  type OrderLineItem,
} from '@common/api/order'
import { completeBossPick, fetchBossPickPrices } from '@common/api/pick'
import { uploadPaymentVoucher } from '@common/api/payment'
import { syncQuoteFromOrder } from '@common/api/quote'
import AppIcon from '@/components/AppIcon.vue'
import { deliveryDateString, useSalesOrderStore } from '@common/stores/salesOrder'
import { useUserStore } from '@common/stores/user'
import {
  buildDeliveryNote,
  DELIVERY_NOTE_CANVAS,
  exportDeliveryNoteImage,
} from '@common/utils/delivery-note'
import { isPaid, isPickDone } from '@common/utils/order-flow'
import { refreshBossOrderAlert } from '@common/utils/boss-order-alert'

const userStore = useUserStore()
const salesOrder = useSalesOrderStore()
const order = ref<OrderInfo | null>(null)
const loading = ref(false)
const syncingPrice = ref(false)
const showMoreActions = ref(false)
const showRemarkEditor = ref(false)
const showMarkPayment = ref(false)
const payDiscountDraft = ref('0')
const payAmountDraft = ref('')
const payActiveField = ref<'discount' | 'amount'>('amount')
const payMethod = ref<'WECHAT' | 'CASH' | 'BANK_TRANSFER' | 'OTHER'>('WECHAT')
const paySubmitting = ref(false)
const remarkDraft = ref('')
const orderId = ref(0)

const PAY_METHOD_LABELS: Record<string, string> = {
  WECHAT: '微信',
  CASH: '现金',
  BANK_TRANSFER: '银行转账',
  OTHER: '其他',
}

type MoreMenuTone = 'green' | 'blue' | 'orange' | 'purple' | 'red' | 'teal' | 'pink' | 'gray' | 'white'

interface MoreMenuItem {
  key: string
  label: string
  icon: string
  tone: MoreMenuTone
}

const isFullyPicked = computed(() => {
  const o = order.value
  if (!o) return false
  const total = o.itemCount || itemKinds.value
  const picked = o.pickedItemCount ?? countPickedFromItems()
  return total > 0 && picked >= total
})

function countPickedFromItems() {
  const items = order.value?.items || []
  return items.filter((line) => line.shortageFlag === 1 || line.actualQty != null).length
}

const pickButtonLabel = computed(() => {
  const o = order.value
  if (!o) return '确认'
  if (o.status === 'PICKED' || (pickButtonDisabled.value && isFullyPicked.value)) {
    return '已拣单'
  }
  if (o.status === 'PENDING_CONFIRM') {
    return '确认'
  }
  return '拣单'
})

const pickButtonDisabled = computed(() => {
  const o = order.value
  if (!o) return true
  if (['PRICED', 'COMPLETED', 'CANCELLED', 'PICKED'].includes(o.status)) return true
  if (o.status === 'PENDING_PRICE' && o.amount != null) return true
  return false
})

const showPriceButton = computed(() => {
  const o = order.value
  if (!o) return false
  if (['CANCELLED', 'COMPLETED', 'PRICED'].includes(o.status)) return false
  if (!isPickDone(o)) return false
  if (o.status === 'PICKED') return true
  if (o.status === 'PENDING_PRICE' && (o.amount == null || o.priceIncomplete)) return true
  return false
})

const priceButtonLabel = computed(() => {
  if (order.value?.priceIncomplete) return '继续录价'
  return '去录价'
})

const showPickAction = computed(() => {
  if (!order.value || showPriceButton.value) return false
  if (['CANCELLED', 'COMPLETED', 'PRICED'].includes(order.value.status)) return false
  return ['PENDING_CONFIRM', 'PENDING_PICK', 'PICKING'].includes(order.value.status)
})

const deliveryTimeText = computed(() => {
  if (!order.value?.deliveryDate) return '—'
  return `${order.value.deliveryDate} 23:00`
})

const receiverInfo = computed(() => {
  const o = order.value
  if (!o) return '—'
  const parts = [o.contactName, o.deliveryAddressShort].filter(Boolean)
  return parts.length ? parts.join('  ') : '—'
})

const canEditOrder = computed(() => {
  if (!order.value) return false
  return !['CANCELLED', 'COMPLETED'].includes(order.value.status)
})

const itemKinds = computed(() =>
  order.value?.items?.length || order.value?.itemCount || 0,
)

const canSendStatement = computed(() => {
  const o = order.value
  if (!o || o.status === 'CANCELLED') return false
  return o.amount != null && !o.priceIncomplete
})

const printActionLabel = computed(() => (canSendStatement.value ? '发送对账单' : '订单详情打印'))

const isOrderPaid = computed(() => (order.value ? isPaid(order.value) : false))

const paymentStatusText = computed(() => {
  if (!order.value) return '待收款'
  if (isOrderPaid.value) return '已收款'
  return order.value.paymentStatusLabel?.includes('部分') ? order.value.paymentStatusLabel : '待收款'
})

const paymentStatusClass = computed(() => (isOrderPaid.value ? 'paid' : 'unpaid'))

const showPaymentAction = computed(() => {
  const o = order.value
  if (!o || o.status === 'CANCELLED' || o.amount == null) return false
  if (showPickAction.value || showPriceButton.value) return false
  if (o.status === 'PENDING_CONFIRM') return false
  if (!o.printed) return false
  return true
})

const paymentActionLabel = computed(() => (isOrderPaid.value ? '已收款' : '标记收款'))

const statementCanvasHeight = computed(() => {
  const rows = order.value?.items?.length || 1
  return DELIVERY_NOTE_CANVAS.calcHeight(rows)
})

const isTemporaryOrder = computed(() => !order.value?.customerId)

const moreMenuItems = computed<MoreMenuItem[]>(() => [
  { key: 'copy', label: '复制订单', icon: 'salesOrder', tone: 'gray' },
  { key: 'share', label: '分享', icon: 'invite', tone: 'gray' },
  { key: 'delete', label: '删单', icon: 'delete', tone: 'red' },
  { key: 'remark', label: '备注', icon: 'pricing', tone: 'gray' },
])

const payMethodLabel = computed(() => {
  const label = PAY_METHOD_LABELS[payMethod.value] || '微信'
  return payMethod.value === 'WECHAT' ? `${label}(默认)` : label
})

const payReceivableText = computed(() => {
  const sales = Number(order.value?.amount || 0)
  const discount = Number(payDiscountDraft.value || 0)
  const receivable = Math.max(0, sales - discount)
  return receivable.toFixed(2)
})

onLoad((query) => {
  if (!userStore.isLoggedIn || !userStore.isBoss) {
    uni.reLaunch({ url: '/packages/common/login/index' })
    return
  }
  orderId.value = Number(query?.id || 0)
})

onShow(async () => {
  if (!orderId.value) return
  await loadOrder()
})

async function loadOrder() {
  if (!orderId.value) return
  loading.value = true
  try {
    order.value = await fetchBossOrderDetail(orderId.value)
  } catch (e) {
    uni.showToast({ title: e instanceof Error ? e.message : '加载失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

function handleMoreMenu(key: string) {
  showMoreActions.value = false
  switch (key) {
    case 'copy':
      copyOrder()
      break
    case 'share':
      shareOrder()
      break
    case 'delete':
      handleDeleteOrder()
      break
    case 'remark':
      openRemarkEditor()
      break
    default:
      break
  }
}

function copyOrder() {
  const o = order.value
  if (!o) return
  salesOrder.reset()
  const name = (o.customerName || o.contactName || '').trim()
  if (o.customerId) {
    salesOrder.setCustomer({ id: o.customerId, name, temporary: false })
  } else if (name) {
    salesOrder.setTemporaryCustomer(name)
  }
  salesOrder.setRemark(o.remark || '')
  if (o.deliveryDate) {
    const tomorrow = deliveryDateString('tomorrow')
    salesOrder.setDeliveryDay(o.deliveryDate === tomorrow ? 'tomorrow' : 'today')
  }
  for (const line of o.items || []) {
    salesOrder.upsertLine({
      productId: line.productId,
      productName: line.productName || '',
      unit: line.unit,
      orderQty: Number(line.orderQty),
      dealPrice: line.dealPrice != null ? Number(line.dealPrice) : undefined,
      pickRemark: line.pickRemark,
    })
  }
  uni.navigateTo({ url: '/pages/boss/sales-order/index' })
}

function shareOrder() {
  const o = order.value
  if (!o) return
  const lines = (o.items || [])
    .map((line) => {
      const pricePart = line.dealPrice != null ? ` ¥${line.dealPrice}` : ''
      return `${line.productName} ${line.orderQty}${line.unit}${pricePart}`
    })
    .join('\n')
  const text = [
    `【${o.customerName || '客户'}】`,
    `订单号：${o.orderNo}`,
    `配送：${o.deliveryDate || '—'}`,
    lines,
    `合计：¥${formatMoney(o.amount)}`,
  ].join('\n')
  uni.setClipboardData({
    data: text,
    success: () => uni.showToast({ title: '已复制，可粘贴分享', icon: 'none' }),
  })
}

function openSyncPrice() {
  uni.showActionSheet({
    itemList: ['从报价单同步到订单', '从订单同步到报价单'],
    success: (res) => {
      if (res.tapIndex === 0) {
        syncPriceFromQuote()
      } else if (res.tapIndex === 1) {
        syncPriceToQuote()
      }
    },
  })
}

async function syncPriceFromQuote() {
  if (syncingPrice.value) return
  syncingPrice.value = true
  try {
    order.value = await fetchBossPickPrices(orderId.value)
    uni.showToast({ title: '已从报价同步到订单', icon: 'success' })
  } catch (e) {
    uni.showToast({ title: e instanceof Error ? e.message : '同步失败', icon: 'none' })
  } finally {
    syncingPrice.value = false
  }
}

async function syncPriceToQuote() {
  const o = order.value
  if (!o?.customerId) {
    uni.showToast({ title: '散客订单请先关联客户', icon: 'none' })
    return
  }
  const items = (o.items || []).filter(
    (line) => line.productId && line.dealPrice != null,
  )
  if (!items.length) {
    uni.showToast({ title: '订单暂无已录价格', icon: 'none' })
    return
  }
  uni.showModal({
    title: '同步价格',
    content: `将 ${items.length} 个商品的成交价写入该客户报价单？`,
    success: async (res) => {
      if (!res.confirm) return
      if (syncingPrice.value) return
      syncingPrice.value = true
      try {
        const result = await syncQuoteFromOrder(orderId.value)
        uni.showToast({ title: `已同步 ${result.syncedCount} 个商品价`, icon: 'success' })
      } catch (e) {
        uni.showToast({ title: e instanceof Error ? e.message : '同步失败', icon: 'none' })
      } finally {
        syncingPrice.value = false
      }
    },
  })
}

function goPayment() {
  const o = order.value
  if (!o) return
  if (!o.customerId && !o.customerName) {
    uni.showToast({ title: '散客订单请先关联客户', icon: 'none' })
    return
  }
  const params: string[] = []
  if (o.customerId) {
    params.push(`customerId=${o.customerId}`)
  }
  if (o.customerName) {
    params.push(`customerName=${encodeURIComponent(o.customerName)}`)
  }
  uni.navigateTo({ url: `/pages/boss/sales-payment/index?${params.join('&')}` })
}

function handlePaymentTap() {
  if (isOrderPaid.value) {
    uni.showToast({ title: '该订单已收款', icon: 'none' })
    return
  }
  openMarkPayment()
}

function openMarkPayment() {
  const o = order.value
  if (!o) return
  if (!o.customerId) {
    uni.showToast({ title: '散客订单请先关联客户', icon: 'none' })
    return
  }
  if (o.amount == null) {
    uni.showToast({ title: '订单尚未录价', icon: 'none' })
    return
  }
  const outstanding = o.outstandingAmount ?? o.receivableAmount ?? o.amount
  if (outstanding != null && Number(outstanding) <= 0) {
    uni.showToast({ title: '该订单已结清', icon: 'none' })
    return
  }
  payDiscountDraft.value = '0'
  payAmountDraft.value = outstanding != null ? String(Number(outstanding)) : ''
  payActiveField.value = 'amount'
  payMethod.value = 'WECHAT'
  showMarkPayment.value = true
}

function closeMarkPayment() {
  showMarkPayment.value = false
}

function pickPayMethod() {
  uni.showActionSheet({
    itemList: ['微信', '现金', '银行转账', '其他'],
    success: (res) => {
      const methods = ['WECHAT', 'CASH', 'BANK_TRANSFER', 'OTHER'] as const
      payMethod.value = methods[res.tapIndex] || 'WECHAT'
    },
  })
}

function currentPayDraft() {
  return payActiveField.value === 'discount' ? payDiscountDraft : payAmountDraft
}

function inputPayKey(key: string) {
  const draft = currentPayDraft()
  let value = draft.value
  if (key === '.') {
    if (value.includes('.')) return
    value = value ? `${value}.` : '0.'
  } else if (key === '0') {
    if (value === '0') return
    value = value ? `${value}0` : '0'
  } else {
    value = value === '0' ? key : `${value}${key}`
  }
  if (value.includes('.')) {
    const [, dec] = value.split('.')
    if (dec && dec.length > 2) return
  }
  draft.value = value
  if (payActiveField.value === 'discount') {
    syncPayAmountAfterDiscount()
  }
}

function backspacePayKey() {
  const draft = currentPayDraft()
  draft.value = draft.value.slice(0, -1)
  if (payActiveField.value === 'discount') {
    syncPayAmountAfterDiscount()
  }
}

function clearPayField() {
  currentPayDraft().value = ''
  if (payActiveField.value === 'discount') {
    syncPayAmountAfterDiscount()
  }
}

function syncPayAmountAfterDiscount() {
  const sales = Number(order.value?.amount || 0)
  const discount = Number(payDiscountDraft.value || 0)
  const paid = Number(order.value?.paidAmount || 0)
  const receivable = Math.max(0, sales - discount)
  const outstanding = Math.max(0, receivable - paid)
  payAmountDraft.value = outstanding > 0 ? String(outstanding) : '0'
}

async function submitMarkPayment() {
  if (paySubmitting.value) return
  const o = order.value
  if (!o) return
  const amount = Number(payAmountDraft.value || 0)
  const discount = Number(payDiscountDraft.value || 0)
  if (!amount || amount <= 0) {
    uni.showToast({ title: '请输入收款金额', icon: 'none' })
    return
  }
  const sales = Number(o.amount || 0)
  const receivable = Math.max(0, sales - discount)
  const paid = Number(o.paidAmount || 0)
  const outstanding = Math.max(0, receivable - paid)
  const willFullyPaid = amount >= outstanding

  paySubmitting.value = true
  try {
    let statementImageUrl: string | undefined
    if (willFullyPaid && o.printed) {
      uni.showLoading({ title: '更新对账单' })
      try {
        const previewOrder: OrderInfo = {
          ...o,
          paidAmount: paid + amount,
          receivableAmount: receivable,
          outstandingAmount: 0,
          paymentStatusLabel: '已收款',
        }
        const note = buildDeliveryNote(previewOrder)
        const filePath = await exportDeliveryNoteImage(note, 'orderDetailNoteCanvas')
        statementImageUrl = await uploadPaymentVoucher(filePath)
      } catch {
        // 收款成功优先，对账单盖章失败不阻断
      } finally {
        uni.hideLoading()
      }
    }

    order.value = await markBossOrderPayment(orderId.value, {
      amount,
      discount: discount > 0 ? discount : undefined,
      method: payMethod.value,
      remark: `订单${o.orderNo || ''}收款`,
      statementImageUrl,
    })
    closeMarkPayment()
    uni.showToast({ title: '已标记收款', icon: 'success' })
  } catch (e) {
    uni.hideLoading()
    uni.showToast({ title: e instanceof Error ? e.message : '收款失败', icon: 'none' })
  } finally {
    paySubmitting.value = false
  }
}

function handleDeleteOrder() {
  if (order.value?.status === 'CANCELLED') {
    uni.showToast({ title: '订单已取消', icon: 'none' })
    return
  }
  uni.showModal({
    title: '删单',
    content: '确认取消该订单？取消后不可恢复。',
    confirmColor: '#c2352a',
    success: async (res) => {
      if (!res.confirm) return
      try {
        order.value = await updateBossOrderStatus(orderId.value, 'CANCELLED')
        uni.showToast({ title: '订单已取消', icon: 'success' })
      } catch (e) {
        uni.showToast({ title: e instanceof Error ? e.message : '取消失败', icon: 'none' })
      }
    },
  })
}

function openRemarkEditor() {
  remarkDraft.value = order.value?.remark || ''
  showRemarkEditor.value = true
}

async function saveRemark() {
  const o = order.value
  if (!o) return
  const remark = remarkDraft.value.trim()
  if (o.status === 'PRICED' || o.amount != null) {
    uni.showToast({ title: '请在改单中修改备注', icon: 'none' })
    showRemarkEditor.value = false
    return
  }
  if (!canEditOrder.value) {
    uni.showToast({ title: '当前状态请在改单中修改备注', icon: 'none' })
    showRemarkEditor.value = false
    return
  }
  const items = (o.items || []).map((line) => ({
    productId: line.productId,
    orderQty: Number(line.orderQty),
    unit: line.unit,
    dealPrice: line.dealPrice != null ? Number(line.dealPrice) : undefined,
    pickRemark: line.pickRemark,
  }))
  try {
    order.value = await updateBossOrder(orderId.value, {
      remark,
      items,
      deliveryDate: o.deliveryDate,
      customerName: o.customerId ? undefined : o.customerName,
    })
    showRemarkEditor.value = false
    uni.showToast({ title: '备注已保存', icon: 'success' })
  } catch (e) {
    uni.showToast({ title: e instanceof Error ? e.message : '保存失败', icon: 'none' })
  }
}

function goEdit() {
  uni.navigateTo({ url: `/pages/boss/sales-order/index?orderId=${orderId.value}` })
}

function goPrint() {
  const mode = canSendStatement.value ? 'send' : 'detail'
  uni.navigateTo({ url: `/pages/boss/orders/print/index?id=${orderId.value}&mode=${mode}` })
}

function goPick() {
  if (pickButtonDisabled.value) return
  const o = order.value
  if (!o) return

  if (o.status === 'PENDING_CONFIRM') {
    uni.showModal({
      title: '确认订单',
      content: '是否确认订单？按确认之后将扣减对应库存，下一步可进行拣单配送',
      success: async (res) => {
        if (!res.confirm) return
        try {
          order.value = await confirmBossOrder(orderId.value)
          await refreshBossOrderAlert({ notify: false })
          uni.showToast({ title: '已确认', icon: 'success' })
        } catch (e) {
          uni.showToast({ title: e instanceof Error ? e.message : '确认失败', icon: 'none' })
        }
      },
    })
    return
  }

  if (o.status === 'PENDING_PICK' || o.status === 'PICKING') {
    uni.showModal({
      title: '完成拣单',
      content: '确认员工已完成装货？将标记为「已拣单」。',
      success: async (res) => {
        if (!res.confirm) return
        try {
          order.value = await completeBossPick(orderId.value)
          await refreshBossOrderAlert({ notify: false })
          uni.showToast({ title: '已拣单', icon: 'success' })
          setTimeout(() => {
            uni.showModal({
              title: '下一步：录价',
              content: '拣单已完成，是否前往录入商品价格？',
              confirmText: '去录价',
              cancelText: '稍后',
              success: (modalRes) => {
                if (modalRes.confirm) goPricing()
              },
            })
          }, 400)
        } catch (e) {
          uni.showToast({ title: e instanceof Error ? e.message : '操作失败', icon: 'none' })
        }
      },
    })
  }
}

function goPricing() {
  uni.navigateTo({ url: `/pages/boss/pricing/detail/index?id=${orderId.value}` })
}

function formatTime(value?: string) {
  if (!value) return '—'
  return value.replace('T', ' ').slice(0, 16)
}

function formatMoney(value?: number | null) {
  if (value == null) return '—'
  return Number(value).toFixed(2)
}

function outboundQty(line: OrderLineItem) {
  if (line.actualQty == null) {
    if (line.shortageFlag === 1) return `0${line.unit}`
    return '未拣单'
  }
  return `${line.actualQty}${line.unit}`
}

function stockText(line: OrderLineItem) {
  if (line.stockQty == null) return '—'
  const qty = Number(line.stockQty)
  if (Number.isNaN(qty)) return '—'
  return `${qty % 1 === 0 ? qty : qty.toFixed(2)}${line.unit}`
}

function isStockInsufficient(line: OrderLineItem) {
  if (line.stockQty == null) return false
  return Number(line.stockQty) < Number(line.orderQty || 0)
}

function lineSubtotal(line: OrderLineItem) {
  if (line.subtotalAmount != null) return Number(line.subtotalAmount).toFixed(2)
  if (line.dealPrice != null && line.actualQty != null) {
    return (Number(line.actualQty) * Number(line.dealPrice)).toFixed(2)
  }
  return '—'
}
</script>

<style scoped lang="scss">
@import '../../../../styles/boss-footer.scss';
@import '../../../../styles/boss-ui.scss';

.page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: linear-gradient(180deg, #e8f8ef 0%, $boss-bg 240rpx);
  box-sizing: border-box;
}

.loading-wrap {
  padding: 80rpx 0;
  text-align: center;
}

.scroll-body {
  flex: 1;
  height: 0;
  padding: 16rpx 20rpx;
  padding-bottom: calc(140rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
}

.status-row {
  display: flex;
  gap: 16rpx;
  margin-bottom: 16rpx;
}

.status-chip {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10rpx;
  padding: 18rpx 16rpx;
  background: $boss-surface;
  border-radius: $boss-radius;
  font-size: 28rpx;
  color: $boss-ink-secondary;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);
}

.status-chip.pay-display.unpaid {
  color: #dc2626;
  font-weight: 600;
}

.status-chip.pay-display.paid {
  color: $boss-green-deep;
  font-weight: 600;
}

.info-card,
.items-card {
  background: $boss-surface;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 16rpx;
  box-shadow: 0 2rpx 16rpx rgba(0, 0, 0, 0.04);
}

.info-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12rpx;
  margin-bottom: 16rpx;
}

.name-wrap {
  display: flex;
  align-items: center;
  gap: 10rpx;
  flex: 1;
  min-width: 0;
}

.customer-name {
  font-size: 36rpx;
  font-weight: 700;
  color: $boss-ink;
}

.temp-tag {
  flex-shrink: 0;
  padding: 4rpx 12rpx;
  font-size: 20rpx;
  color: #e67e22;
  background: #fef5ec;
  border-radius: 999rpx;
}

.source-tag {
  flex-shrink: 0;
  padding: 4rpx 14rpx;
  font-size: 22rpx;
  color: $boss-green-deep;
  border: 1rpx solid $boss-green;
  border-radius: 8rpx;
}

.delivery-line {
  margin-bottom: 20rpx;
  font-size: 28rpx;
}

.delivery-label {
  color: $boss-ink-secondary;
}

.delivery-value {
  color: #e67e22;
  font-weight: 600;
}

.info-grid {
  border-top: 1rpx solid $boss-border;
  padding-top: 8rpx;
}

.info-row {
  display: flex;
  align-items: flex-start;
  padding: 14rpx 0;
  font-size: 28rpx;
  line-height: 1.45;
}

.info-label {
  width: 152rpx;
  flex-shrink: 0;
  color: $boss-ink-muted;
}

.info-value {
  flex: 1;
  color: $boss-ink;
  word-break: break-all;
}

.status-text {
  color: #e67e22;
  font-weight: 600;
}

.order-remark {
  margin-top: 8rpx;
  padding: 16rpx;
  background: #fafbfc;
  border-radius: 12rpx;
  font-size: 26rpx;
  color: #e67e22;
}

.finance-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 1rpx;
  margin-top: 20rpx;
  background: $boss-border;
  border-radius: 12rpx;
  overflow: hidden;
}

.finance-cell {
  padding: 18rpx 20rpx;
  background: #fafbfc;
}

.finance-label {
  display: block;
  font-size: 24rpx;
  color: $boss-ink-muted;
}

.finance-value {
  display: block;
  margin-top: 6rpx;
  font-size: 30rpx;
  font-weight: 700;
  color: $boss-ink;
}

.finance-value.muted {
  color: #ccc;
  font-weight: 400;
}

.finance-value.green {
  color: $boss-green-deep;
}

.finance-value.red {
  color: $boss-danger;
}

.items-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16rpx;
}

.items-title-wrap {
  display: flex;
  align-items: center;
  gap: 10rpx;
}

.items-title {
  font-size: 30rpx;
  font-weight: 700;
  color: $boss-ink;
}

.table-head,
.table-row {
  display: flex;
  align-items: flex-start;
  gap: 4rpx;
  font-size: 22rpx;
}

.table-head {
  padding: 12rpx 0;
  color: $boss-ink-muted;
  border-bottom: 1rpx solid $boss-border;
}

.table-row {
  padding: 18rpx 0;
  border-bottom: 1rpx solid #f2f3f5;
}

.col-name {
  flex: 2;
  min-width: 0;
}

.col-qty,
.col-stock,
.col-out,
.col-price,
.col-sub {
  flex: 1;
  text-align: center;
  flex-shrink: 0;
}

.col-stock.warn {
  color: $boss-warn;
  font-weight: 600;
}

.line-name {
  display: block;
  font-size: 28rpx;
  color: $boss-ink;
  font-weight: 500;
}

.line-remark {
  display: block;
  margin-top: 6rpx;
  font-size: 22rpx;
  color: $boss-ink-muted;
}

.col-out.done {
  color: $boss-green-deep;
  font-weight: 600;
}

.col-price,
.col-sub {
  font-size: 26rpx;
  color: $boss-ink-secondary;
}

.price-warn-bar {
  margin-top: 16rpx;
  padding: 16rpx 0 4rpx;
  text-align: center;
  font-size: 26rpx;
  color: $boss-warn;
  border-top: 1rpx dashed $boss-border;
}

.detail-bar {
  gap: 12rpx;
  align-items: center;
}

.bar-tools {
  display: flex;
  gap: 12rpx;
  flex-shrink: 0;
}

.bar-actions {
  flex: 1;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 12rpx;
  min-width: 0;
}

.bar-tool {
  width: 88rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4rpx;
  flex-shrink: 0;
  font-size: 22rpx;
  color: $boss-ink-secondary;
}

.bar-print-btn {
  flex-shrink: 0;
  height: 88rpx;
  line-height: 88rpx;
  padding: 0 28rpx;
  font-size: 28rpx;
  color: $boss-green-deep;
  background: $boss-surface;
  border: 2rpx solid $boss-green;
  border-radius: $boss-radius;
  font-weight: 600;
  text-align: center;
  box-sizing: border-box;
}

.bar-print-btn.primary {
  min-width: 220rpx;
  padding: 0 36rpx;
  color: #fff;
  background: $boss-green;
  border: none;
  font-size: 30rpx;
  box-shadow: 0 6rpx 20rpx rgba(7, 193, 96, 0.28);
}

.bar-outline {
  flex-shrink: 0;
  height: 88rpx;
  line-height: 88rpx;
  padding: 0 24rpx;
  font-size: 28rpx;
  color: $boss-green-deep;
  background: $boss-surface;
  border: 2rpx solid $boss-green;
  border-radius: $boss-radius;
  font-weight: 600;
}

.bar-primary {
  flex: 0 1 auto;
  min-width: 160rpx;
  max-width: 100%;
  height: 88rpx;
  line-height: 88rpx;
  margin: 0;
  padding: 0 16rpx;
  background: $boss-green;
  color: #fff;
  font-size: 30rpx;
  font-weight: 600;
  border-radius: $boss-radius;
  border: none;
}

.bar-primary.picked {
  background: #b8e6cc;
  color: #fff;
}

.bar-primary::after {
  border: none;
}

.bar-pay {
  flex: 0 1 auto;
  min-width: 160rpx;
  max-width: 100%;
  height: 88rpx;
  line-height: 88rpx;
  margin: 0;
  padding: 0 16rpx;
  background: #dc2626;
  color: #fff;
  font-size: 30rpx;
  font-weight: 600;
  border-radius: $boss-radius;
  border: none;
}

.bar-pay.paid {
  background: $boss-green;
}

.bar-pay::after {
  border: none;
}

.hidden-canvas {
  position: fixed;
  left: -9999px;
  top: 0;
  opacity: 0;
  pointer-events: none;
}

.more-mask {
  position: fixed;
  inset: 0;
  z-index: 1000;
  background: rgba(0, 0, 0, 0.45);
}

.more-sheet {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 1001;
  padding: 28rpx 20rpx calc(20rpx + env(safe-area-inset-bottom));
  background: $boss-surface;
  border-radius: 24rpx 24rpx 0 0;
  box-sizing: border-box;
}

.more-grid {
  display: flex;
  flex-wrap: wrap;
}

.more-grid-item {
  width: 25%;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 18rpx 0 22rpx;
}

.more-grid-label {
  margin-top: 12rpx;
  font-size: 24rpx;
  color: $boss-ink;
  text-align: center;
  line-height: 1.3;
}

.more-cancel {
  margin-top: 8rpx;
  height: 88rpx;
  line-height: 88rpx;
  text-align: center;
  font-size: 30rpx;
  color: $boss-ink-secondary;
  background: #f5f6f7;
  border-radius: $boss-radius;
}

.remark-sheet {
  position: fixed;
  left: 40rpx;
  right: 40rpx;
  top: 50%;
  z-index: 1002;
  transform: translateY(-50%);
  padding: 32rpx 28rpx;
  background: $boss-surface;
  border-radius: 20rpx;
  box-sizing: border-box;
}

.remark-title {
  display: block;
  margin-bottom: 20rpx;
  font-size: 32rpx;
  font-weight: 700;
  color: $boss-ink;
}

.remark-input {
  width: 100%;
  min-height: 160rpx;
  padding: 20rpx;
  font-size: 28rpx;
  color: $boss-ink;
  background: #f7f8f9;
  border-radius: 12rpx;
  box-sizing: border-box;
}

.remark-actions {
  display: flex;
  gap: 16rpx;
  margin-top: 24rpx;
}

.remark-btn {
  flex: 1;
  height: 80rpx;
  line-height: 80rpx;
  text-align: center;
  font-size: 28rpx;
  border-radius: $boss-radius;
}

.remark-btn.ghost {
  color: $boss-ink-secondary;
  background: #f0f1f2;
}

.remark-btn.primary {
  color: #fff;
  font-weight: 600;
  background: $boss-green;
}

.pay-mask {
  position: fixed;
  inset: 0;
  z-index: 1100;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
}

.pay-sheet {
  background: $boss-surface;
  border-radius: 24rpx 24rpx 0 0;
  padding-bottom: env(safe-area-inset-bottom);
}

.pay-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24rpx 28rpx 12rpx;
}

.pay-close {
  width: 72rpx;
  font-size: 44rpx;
  color: $boss-ink-muted;
  line-height: 1;
}

.pay-title {
  font-size: 34rpx;
  font-weight: 700;
  color: $boss-ink;
}

.pay-head-spacer {
  width: 72rpx;
}

.pay-sales {
  display: block;
  padding: 0 28rpx 20rpx;
  font-size: 28rpx;
  color: $boss-ink-secondary;
}

.pay-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 0 28rpx 16rpx;
  padding: 24rpx 28rpx;
  background: #f5f6f7;
  border-radius: 12rpx;
  border: 2rpx solid transparent;
}

.pay-row.editable.active {
  border-color: $boss-green;
  background: #fff;
}

.pay-row-label {
  font-size: 30rpx;
  color: $boss-ink;
}

.pay-row-value {
  font-size: 32rpx;
  font-weight: 600;
  color: $boss-ink;
}

.pay-row-value.highlight {
  color: #e67e22;
}

.pay-method {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 8rpx 28rpx 20rpx;
  padding: 24rpx 28rpx;
  background: #f5f6f7;
  border-radius: 12rpx;
}

.pay-method-label {
  font-size: 30rpx;
  color: $boss-ink;
}

.pay-method-value {
  font-size: 28rpx;
  color: $boss-ink-secondary;
}

.pay-keypad {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  grid-template-rows: repeat(4, 96rpx);
  gap: 8rpx;
  padding: 12rpx;
  background: #eef0f2;
  border-top: 1rpx solid #ddd;
}

.pay-keypad .key {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
  border-radius: 8rpx;
  font-size: 36rpx;
  font-weight: 500;
}

.pay-keypad .key.fn {
  font-size: 26rpx;
  color: #666;
}

.pay-keypad .key.confirm {
  grid-row: 3 / 5;
  grid-column: 4;
  background: $boss-green;
  color: #fff;
  font-size: 32rpx;
  font-weight: 600;
}

.pay-keypad .key.blank {
  visibility: hidden;
  pointer-events: none;
}
</style>
