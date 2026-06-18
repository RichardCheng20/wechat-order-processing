<template>
  <u-popup :show="show" mode="bottom" round="16" @close="emit('close')">
    <view class="picker">
      <view class="picker-head">
        <text class="picker-cancel" @tap="emit('close')">取消</text>
        <text class="picker-title">选择时间范围</text>
        <text class="picker-confirm" @tap="confirm">确定</text>
      </view>

      <view class="month-nav">
        <text class="nav-btn" @tap="prevMonth">‹</text>
        <text class="month-label">{{ monthLabel }}</text>
        <text class="nav-btn" @tap="nextMonth">›</text>
      </view>

      <view class="week-row">
        <text v-for="w in weekLabels" :key="w" class="week-cell">{{ w }}</text>
      </view>

      <view class="day-grid">
        <view
          v-for="cell in calendarCells"
          :key="cell.key"
          class="day-cell"
          :class="cellClass(cell)"
          @tap="pickDay(cell)"
        >
          <text v-if="cell.day" class="day-num">{{ cell.day }}</text>
          <text v-if="cell.tag" class="day-tag">{{ cell.tag }}</text>
        </view>
      </view>
    </view>
  </u-popup>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'

const props = defineProps<{
  show: boolean
  startDate?: string
  endDate?: string
}>()

const emit = defineEmits<{
  close: []
  confirm: [payload: { from: string; to: string }]
}>()

const weekLabels = ['日', '一', '二', '三', '四', '五', '六']
const viewYear = ref(0)
const viewMonth = ref(0)
const pickStart = ref('')
const pickEnd = ref('')

watch(
  () => props.show,
  (visible) => {
    if (!visible) return
    pickStart.value = props.startDate || ''
    pickEnd.value = props.endDate || ''
    const base = parseDate(props.startDate || props.endDate || formatDate(new Date()))
    viewYear.value = base.getFullYear()
    viewMonth.value = base.getMonth()
  },
)

const monthLabel = computed(() => `${viewYear.value}年${viewMonth.value + 1}月`)

interface CalendarCell {
  key: string
  day: number
  date: string
  tag?: string
}

const calendarCells = computed<CalendarCell[]>(() => {
  const first = new Date(viewYear.value, viewMonth.value, 1)
  const startWeekday = first.getDay()
  const daysInMonth = new Date(viewYear.value, viewMonth.value + 1, 0).getDate()
  const cells: CalendarCell[] = []
  for (let i = 0; i < startWeekday; i += 1) {
    cells.push({ key: `e-${i}`, day: 0, date: '' })
  }
  for (let day = 1; day <= daysInMonth; day += 1) {
    const date = formatDate(new Date(viewYear.value, viewMonth.value, day))
    cells.push({
      key: date,
      day,
      date,
      tag: rangeTag(date),
    })
  }
  return cells
})

function rangeTag(date: string) {
  if (date === pickStart.value && date === pickEnd.value) return '开始'
  if (date === pickStart.value) return '开始'
  if (date === pickEnd.value) return '结束'
  return undefined
}

function cellClass(cell: CalendarCell) {
  if (!cell.date) return 'empty'
  const inRange = isInRange(cell.date)
  return {
    start: cell.date === pickStart.value,
    end: cell.date === pickEnd.value,
    in: inRange && cell.date !== pickStart.value && cell.date !== pickEnd.value,
    today: cell.date === formatDate(new Date()),
  }
}

function isInRange(date: string) {
  if (!pickStart.value || !pickEnd.value) return false
  return date >= pickStart.value && date <= pickEnd.value
}

function pickDay(cell: CalendarCell) {
  if (!cell.date) return
  if (!pickStart.value || (pickStart.value && pickEnd.value)) {
    pickStart.value = cell.date
    pickEnd.value = ''
    return
  }
  if (cell.date < pickStart.value) {
    pickEnd.value = pickStart.value
    pickStart.value = cell.date
    return
  }
  pickEnd.value = cell.date
}

function prevMonth() {
  if (viewMonth.value === 0) {
    viewYear.value -= 1
    viewMonth.value = 11
    return
  }
  viewMonth.value -= 1
}

function nextMonth() {
  if (viewMonth.value === 11) {
    viewYear.value += 1
    viewMonth.value = 0
    return
  }
  viewMonth.value += 1
}

function confirm() {
  if (!pickStart.value) {
    uni.showToast({ title: '请选择起始日期', icon: 'none' })
    return
  }
  const to = pickEnd.value || pickStart.value
  emit('confirm', { from: pickStart.value, to })
  emit('close')
}

function parseDate(value: string) {
  const [y, m, d] = value.split('-').map(Number)
  return new Date(y, (m || 1) - 1, d || 1)
}

function formatDate(date: Date) {
  const y = date.getFullYear()
  const m = `${date.getMonth() + 1}`.padStart(2, '0')
  const d = `${date.getDate()}`.padStart(2, '0')
  return `${y}-${m}-${d}`
}
</script>

<style scoped lang="scss">
.picker {
  padding-bottom: calc(24rpx + env(safe-area-inset-bottom));
  background: #fff;
}

.picker-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 28rpx 32rpx 16rpx;
}

.picker-cancel,
.picker-confirm {
  min-width: 88rpx;
  font-size: 30rpx;
}

.picker-cancel {
  color: #666;
}

.picker-confirm {
  color: #07c160;
  text-align: right;
  font-weight: 600;
}

.picker-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #111;
}

.month-nav {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 48rpx;
  padding: 8rpx 0 16rpx;
}

.nav-btn {
  width: 56rpx;
  text-align: center;
  font-size: 40rpx;
  color: #666;
}

.month-label {
  font-size: 30rpx;
  font-weight: 600;
  color: #111;
}

.week-row,
.day-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  padding: 0 16rpx;
}

.week-cell {
  text-align: center;
  padding: 12rpx 0;
  font-size: 24rpx;
  color: #999;
}

.day-cell {
  position: relative;
  height: 88rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.day-cell.empty {
  pointer-events: none;
}

.day-num {
  width: 64rpx;
  height: 64rpx;
  line-height: 64rpx;
  text-align: center;
  font-size: 28rpx;
  color: #333;
  border-radius: 12rpx;
}

.day-cell.in .day-num {
  background: #e8f8ef;
  border-radius: 0;
  width: 100%;
}

.day-cell.start .day-num,
.day-cell.end .day-num {
  background: #07c160;
  color: #fff;
  font-weight: 600;
}

.day-cell.today .day-num {
  box-shadow: inset 0 0 0 2rpx #07c160;
}

.day-tag {
  position: absolute;
  bottom: 4rpx;
  font-size: 18rpx;
  color: #fff;
  line-height: 1;
}

.day-cell.in .day-tag {
  color: #07c160;
}
</style>
