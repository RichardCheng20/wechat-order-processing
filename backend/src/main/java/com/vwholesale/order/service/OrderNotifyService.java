package com.vwholesale.order.service;

import com.vwholesale.common.config.AppProperties;
import com.vwholesale.common.context.MerchantContext;
import com.vwholesale.common.exception.BusinessException;
import com.vwholesale.common.security.RoleChecker;
import com.vwholesale.mq.publisher.OrderEventPublisher;
import com.vwholesale.order.dto.OrderNotifyResultVO;
import com.vwholesale.order.dto.OrderVO;
import com.vwholesale.mq.service.WechatNotifyHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderNotifyService {

    private final OrderService orderService;
    private final OrderEventPublisher orderEventPublisher;
    private final WechatNotifyHandler wechatNotifyHandler;
    private final AppProperties appProperties;
    private final MerchantContext merchantContext;

    public OrderNotifyResultVO notifyBossByCustomer(Long orderId) {
        RoleChecker.requireCustomer();
        OrderVO order = orderService.getDetail(orderId);
        if (!"PENDING_CONFIRM".equals(order.getStatus())) {
            throw BusinessException.of(400, "仅待商家确认订单可提醒老板");
        }

        if (!appProperties.getMq().isEnabled()) {
            return wechatNotifyHandler.notifyBoss(orderId);
        }

        orderEventPublisher.publishNotifyBoss(merchantContext.currentMerchantId(), orderId);
        return OrderNotifyResultVO.builder()
                .notified(0)
                .failed(0)
                .message("提醒已提交，将推送给老板")
                .build();
    }
}
