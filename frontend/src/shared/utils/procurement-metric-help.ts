export type ProcurementMetricHelpKey = 'demand' | 'available' | 'need' | 'overview'

export const PROCUREMENT_METRIC_HELP: Record<
  ProcurementMetricHelpKey,
  { title: string; content: string }
> = {
  overview: {
    title: '采购指标说明',
    content:
      '以下数据均按页面顶部所选「收货日」统计。\n\n' +
      '【客户需求】\n' +
      '当日所有未取消订单里，该商品数量合计。\n' +
      '已分拣的按实拣数，未分拣的按下单数；标记缺货按实拣数（通常为 0）。\n\n' +
      '【仓存】\n' +
      '仓库里现有的实物数量（可在采购详情修改）。\n' +
      '子项「可用」= 仓存 − 订单占用，仅供参考。\n\n' +
      '【需采购】\n' +
      '需采购 = max(0, 客户需求 − 仓存)。\n' +
      '即除仓库现有库存外，还需额外采购的数量；代采商品按客户需求全额计。',
  },
  demand: {
    title: '客户需求',
    content:
      '统计所选收货日、所有未取消订单中，该商品的数量合计。\n\n' +
      '数量取值：\n' +
      '· 已分拣：用实拣数\n' +
      '· 未分拣：用下单数\n' +
      '· 标记缺货：用实拣数（一般为 0）',
  },
  available: {
    title: '仓存剩余',
    content:
      '仓库里现有的实物数量，可在采购详情点击修改。\n\n' +
      '· 仓存：昨天卖剩 + 已入库的货\n' +
      '· 占用：待确认订单已锁定的数量（不影响仓存显示）\n' +
      '· 可用 = 仓存 − 占用（参考值）',
  },
  need: {
    title: '需采购',
    content:
      '需采购 = max(0, 客户需求 − 仓存)。\n\n' +
      '例：需求 300 斤、仓存 100 斤 → 需采购 200 斤。\n' +
      '例：需求 50 斤、仓存 50 斤 → 需采购 0。\n\n' +
      '代采商品不计库存，需采购 = 客户需求。',
  },
}

export function getProcurementMetricHelp(key: ProcurementMetricHelpKey) {
  return PROCUREMENT_METRIC_HELP[key]
}
