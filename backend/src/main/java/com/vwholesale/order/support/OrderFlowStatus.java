package com.vwholesale.order.support;

import com.vwholesale.common.enums.OrderStatus;
import com.vwholesale.order.entity.Order;

import java.math.BigDecimal;
import java.util.Set;

/**
 * 业务流转：待确认 → 已确认 → 已拣单 → 已录价 → 已对账 → 已收款（+ 已取消）
 */
public final class OrderFlowStatus {

    public static final String LABEL_PENDING_CONFIRM = "待确认";
    public static final String LABEL_CONFIRMED = "已确认";
    public static final String LABEL_PICKED = "已拣单";
    public static final String LABEL_PRICED = "已录价";
    public static final String LABEL_RECONCILED = "已对账";
    public static final String LABEL_PAID = "已收款";
    public static final String LABEL_CANCELLED = "已取消";

    private static final String[] STEP_LABELS = {
            LABEL_PENDING_CONFIRM,
            LABEL_CONFIRMED,
            LABEL_PICKED,
            LABEL_PRICED,
            LABEL_RECONCILED,
            LABEL_PAID
    };

    private static final Set<String> PICK_DONE_STATUSES = Set.of(
            OrderStatus.PICKED,
            OrderStatus.PENDING_PRICE,
            OrderStatus.PRICED,
            OrderStatus.DELIVERING,
            OrderStatus.DELIVERED,
            OrderStatus.COMPLETED
    );

    private OrderFlowStatus() {
    }

    public static int resolveStageIndex(Order order, boolean priceIncomplete, int pickedItemCount, int itemCount) {
        if (order == null) {
            return 0;
        }
        return resolveStageIndex(
                order.getStatus(),
                order.getPrintedAt() != null,
                order.getAmount(),
                priceIncomplete,
                order.getPaidAmount(),
                order.getReceivableAmount(),
                pickedItemCount,
                itemCount
        );
    }

    public static int resolveStageIndex(
            String status,
            boolean printed,
            BigDecimal amount,
            Boolean priceIncomplete,
            BigDecimal paidAmount,
            BigDecimal receivableAmount,
            Integer pickedItemCount,
            Integer itemCount
    ) {
        if (OrderStatus.CANCELLED.equals(status)) {
            return -1;
        }
        BigDecimal receivable = receivableAmount != null ? receivableAmount : amount;
        BigDecimal paid = paidAmount != null ? paidAmount : BigDecimal.ZERO;
        if (receivable != null && receivable.compareTo(BigDecimal.ZERO) > 0 && paid.compareTo(receivable) >= 0) {
            return 5;
        }
        if (printed) {
            return 4;
        }
        boolean priced = amount != null && !Boolean.TRUE.equals(priceIncomplete);
        if (priced || OrderStatus.PRICED.equals(status) || OrderStatus.COMPLETED.equals(status)) {
            return 3;
        }
        if (isPickDone(status, pickedItemCount, itemCount)) {
            return 2;
        }
        if (!OrderStatus.PENDING_CONFIRM.equals(status)) {
            return 1;
        }
        return 0;
    }

    public static String flowStatusLabel(Order order, boolean priceIncomplete, int pickedItemCount, int itemCount) {
        return flowStatusLabel(resolveStageIndex(order, priceIncomplete, pickedItemCount, itemCount));
    }

    public static String flowStatusLabel(
            String status,
            boolean printed,
            BigDecimal amount,
            Boolean priceIncomplete,
            BigDecimal paidAmount,
            BigDecimal receivableAmount,
            Integer pickedItemCount,
            Integer itemCount
    ) {
        return flowStatusLabel(resolveStageIndex(
                status, printed, amount, priceIncomplete, paidAmount, receivableAmount, pickedItemCount, itemCount));
    }

    public static String flowStatusLabel(int stageIndex) {
        if (stageIndex < 0) {
            return LABEL_CANCELLED;
        }
        if (stageIndex >= STEP_LABELS.length) {
            return STEP_LABELS[STEP_LABELS.length - 1];
        }
        return STEP_LABELS[stageIndex];
    }

    /** 客户端展示：避免「待确认/已确认」与老板端混淆 */
    public static String customerStatusLabel(Order order, boolean priceIncomplete, int pickedItemCount, int itemCount) {
        return customerStatusLabel(resolveStageIndex(order, priceIncomplete, pickedItemCount, itemCount));
    }

    public static String customerStatusLabel(int stageIndex) {
        if (stageIndex < 0) {
            return LABEL_CANCELLED;
        }
        if (stageIndex == 0) {
            return "订单待处理";
        }
        if (stageIndex == 4) {
            return "待付款";
        }
        if (stageIndex >= 5) {
            return "已完成";
        }
        return "处理中";
    }

    public static String[] stepLabels() {
        return STEP_LABELS.clone();
    }

    private static boolean isPickDone(String status, Integer pickedItemCount, Integer itemCount) {
        if (status != null && PICK_DONE_STATUSES.contains(status)) {
            return true;
        }
        int total = itemCount != null ? itemCount : 0;
        int picked = pickedItemCount != null ? pickedItemCount : 0;
        return total > 0 && picked >= total;
    }
}
