package com.vwholesale.common.enums;

public final class OrderStatus {

    public static final String PENDING_CONFIRM = "PENDING_CONFIRM";
    public static final String PENDING_PICK = "PENDING_PICK";
    public static final String PICKING = "PICKING";
    public static final String PICKED = "PICKED";
    public static final String DELIVERING = "DELIVERING";
    public static final String DELIVERED = "DELIVERED";
    public static final String PENDING_PRICE = "PENDING_PRICE";
    public static final String COMPLETED = "COMPLETED";
    public static final String CANCELLED = "CANCELLED";

    private OrderStatus() {
    }
}
