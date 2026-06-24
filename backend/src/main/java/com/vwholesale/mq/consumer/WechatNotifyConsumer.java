package com.vwholesale.mq.consumer;

import com.vwholesale.mq.config.RabbitMqConstants;
import com.vwholesale.mq.model.OrderEventMessage;
import com.vwholesale.mq.service.WechatNotifyHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.mq.enabled", havingValue = "true", matchIfMissing = true)
public class WechatNotifyConsumer {

    private final WechatNotifyHandler wechatNotifyHandler;

    @RabbitListener(queues = RabbitMqConstants.WECHAT_NOTIFY_QUEUE)
    public void handle(OrderEventMessage message) {
        if (message == null || message.getOrderId() == null) {
            log.warn("skip invalid wechat notify message");
            return;
        }
        log.info("consume wechat notify, orderId={}", message.getOrderId());
        wechatNotifyHandler.notifyBoss(message.getOrderId());
    }
}
