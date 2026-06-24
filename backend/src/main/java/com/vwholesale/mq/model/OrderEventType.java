package com.vwholesale.mq.model;

public enum OrderEventType {
    ORDER_CREATED,
    ORDER_CONFIRMED,
    ORDER_PRICED_PUBLISHED,
    ORDER_STATEMENT_SENT,
    ORDER_PAYMENT_RECEIVED,
    PAYMENT_RECEIVED,
    NOTIFY_BOSS
}
