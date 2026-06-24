package com.vwholesale.mq.publisher;

import com.vwholesale.mq.model.OrderEventMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@RequiredArgsConstructor
public class OrderEventAfterCommitPublisher {

    private final OrderEventPublisher orderEventPublisher;

    public void publishAfterCommit(OrderEventMessage message) {
        if (message == null) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    orderEventPublisher.publish(message);
                }
            });
            return;
        }
        orderEventPublisher.publish(message);
    }
}
