package com.vwholesale.mq.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderEventMessage implements Serializable {

    private OrderEventType eventType;
    private Long merchantId;
    private Long orderId;
    private Long customerId;
    private Long paymentId;

    public String routingKey() {
        return switch (eventType) {
            case ORDER_CREATED -> "order.created";
            case ORDER_CONFIRMED -> "order.confirmed";
            case ORDER_PRICED_PUBLISHED -> "order.priced";
            case ORDER_STATEMENT_SENT -> "order.statement.sent";
            case ORDER_PAYMENT_RECEIVED -> "order.payment.received";
            case PAYMENT_RECEIVED -> "payment.received";
            case NOTIFY_BOSS -> "order.notify.boss";
        };
    }

    public static OrderEventMessage orderCreated(Long merchantId, Long orderId) {
        return OrderEventMessage.builder()
                .eventType(OrderEventType.ORDER_CREATED)
                .merchantId(merchantId)
                .orderId(orderId)
                .build();
    }

    public static OrderEventMessage orderConfirmed(Long merchantId, Long orderId) {
        return OrderEventMessage.builder()
                .eventType(OrderEventType.ORDER_CONFIRMED)
                .merchantId(merchantId)
                .orderId(orderId)
                .build();
    }

    public static OrderEventMessage orderPricedPublished(Long merchantId, Long orderId) {
        return OrderEventMessage.builder()
                .eventType(OrderEventType.ORDER_PRICED_PUBLISHED)
                .merchantId(merchantId)
                .orderId(orderId)
                .build();
    }

    public static OrderEventMessage orderStatementSent(Long merchantId, Long orderId) {
        return OrderEventMessage.builder()
                .eventType(OrderEventType.ORDER_STATEMENT_SENT)
                .merchantId(merchantId)
                .orderId(orderId)
                .build();
    }

    public static OrderEventMessage orderPaymentReceived(Long merchantId, Long orderId) {
        return OrderEventMessage.builder()
                .eventType(OrderEventType.ORDER_PAYMENT_RECEIVED)
                .merchantId(merchantId)
                .orderId(orderId)
                .build();
    }

    public static OrderEventMessage paymentReceived(Long merchantId, Long customerId, Long paymentId) {
        return OrderEventMessage.builder()
                .eventType(OrderEventType.PAYMENT_RECEIVED)
                .merchantId(merchantId)
                .customerId(customerId)
                .paymentId(paymentId)
                .build();
    }

    public static OrderEventMessage notifyBoss(Long merchantId, Long orderId) {
        return OrderEventMessage.builder()
                .eventType(OrderEventType.NOTIFY_BOSS)
                .merchantId(merchantId)
                .orderId(orderId)
                .build();
    }
}
