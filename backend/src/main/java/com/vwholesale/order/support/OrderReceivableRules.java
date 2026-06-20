package com.vwholesale.order.support;

import com.vwholesale.common.enums.OrderStatus;
import com.vwholesale.order.entity.Order;

import java.math.BigDecimal;

public final class OrderReceivableRules {

    private OrderReceivableRules() {
    }

    /** 已对账或已完成、且有可结算金额的订单计入客户欠款 */
    public static boolean countsTowardCustomerDebt(Order order) {
        if (order == null || OrderStatus.CANCELLED.equals(order.getStatus())) {
            return false;
        }
        if (!hasBillableAmount(order)) {
            return false;
        }
        if (order.getPrintedAt() != null) {
            return true;
        }
        return OrderStatus.COMPLETED.equals(order.getStatus());
    }

    public static boolean hasBillableAmount(Order order) {
        return order.getReceivableAmount() != null || order.getAmount() != null;
    }

    public static BigDecimal outstandingReceivable(Order order) {
        BigDecimal receivable = order.getReceivableAmount() != null
                ? order.getReceivableAmount()
                : order.getAmount();
        if (receivable == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal paid = order.getPaidAmount() != null ? order.getPaidAmount() : BigDecimal.ZERO;
        return receivable.subtract(paid);
    }
}
