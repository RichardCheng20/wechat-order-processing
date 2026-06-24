package com.vwholesale.mq.consumer;

import com.vwholesale.mq.config.RabbitMqConstants;
import com.vwholesale.mq.model.OrderEventMessage;
import com.vwholesale.mq.service.BossStatsCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.mq.enabled", havingValue = "true", matchIfMissing = true)
public class StatsRefreshConsumer {

    private final BossStatsCacheService bossStatsCacheService;

    @RabbitListener(queues = RabbitMqConstants.STATS_REFRESH_QUEUE)
    public void handle(OrderEventMessage message) {
        if (message == null) {
            return;
        }
        log.debug("refresh stats cache for event {}", message.getEventType());
        bossStatsCacheService.refresh();
    }
}
