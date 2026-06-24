package com.vwholesale.mq.publisher;

import com.vwholesale.common.config.AppProperties;
import com.vwholesale.mq.config.RabbitMqConstants;
import com.vwholesale.mq.model.OrderEventMessage;
import com.vwholesale.mq.model.OrderEventType;
import com.vwholesale.mq.service.BossStatsCacheService;
import com.vwholesale.mq.service.WechatNotifyHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventPublisher {

    private final AppProperties appProperties;
    private final ObjectProvider<RabbitTemplate> rabbitTemplateProvider;
    private final ObjectProvider<WechatNotifyHandler> wechatNotifyHandlerProvider;
    private final ObjectProvider<BossStatsCacheService> bossStatsCacheServiceProvider;

    public void publish(OrderEventMessage message) {
        if (message == null || message.getEventType() == null) {
            return;
        }
        if (!appProperties.getMq().isEnabled()) {
            dispatchLocally(message);
            return;
        }
        RabbitTemplate rabbitTemplate = rabbitTemplateProvider.getIfAvailable();
        if (rabbitTemplate == null) {
            log.warn("RabbitTemplate unavailable, dispatch locally: {}", message.getEventType());
            dispatchLocally(message);
            return;
        }
        rabbitTemplate.convertAndSend(
                RabbitMqConstants.ORDER_EXCHANGE,
                message.routingKey(),
                message,
                msg -> {
                    msg.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                    return msg;
                }
        );
        log.debug("published order event {} for orderId={}", message.getEventType(), message.getOrderId());
    }

    public void publishOrderEvent(OrderEventMessage message) {
        publish(message);
    }

    public void publishNotifyBoss(Long merchantId, Long orderId) {
        publish(OrderEventMessage.notifyBoss(merchantId, orderId));
    }

    private void dispatchLocally(OrderEventMessage message) {
        if (message.getEventType() == OrderEventType.NOTIFY_BOSS) {
            wechatNotifyHandlerProvider.getObject().notifyBoss(message.getOrderId());
            return;
        }
        bossStatsCacheServiceProvider.getObject().refresh();
    }
}
