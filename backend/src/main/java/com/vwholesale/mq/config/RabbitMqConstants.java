package com.vwholesale.mq.config;

public final class RabbitMqConstants {

    public static final String ORDER_EXCHANGE = "vwholesale.order.topic";
    public static final String DLX_EXCHANGE = "vwholesale.dlx";
    public static final String WECHAT_NOTIFY_QUEUE = "vwholesale.queue.wechat.notify";
    public static final String STATS_REFRESH_QUEUE = "vwholesale.queue.stats.refresh";
    public static final String DLQ = "vwholesale.queue.dlq";

    private RabbitMqConstants() {
    }
}
