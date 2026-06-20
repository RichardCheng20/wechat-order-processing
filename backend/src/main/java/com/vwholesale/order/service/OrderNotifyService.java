package com.vwholesale.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.vwholesale.common.config.AppProperties;
import com.vwholesale.common.enums.UserRole;
import com.vwholesale.common.exception.BusinessException;
import com.vwholesale.common.security.RoleChecker;
import com.vwholesale.order.dto.OrderItemVO;
import com.vwholesale.order.dto.OrderNotifyResultVO;
import com.vwholesale.order.dto.OrderVO;
import com.vwholesale.auth.service.WechatClient;
import com.vwholesale.user.entity.User;
import com.vwholesale.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderNotifyService {

    private static final String BOSS_ORDER_PAGE = "pages/boss/orders/detail/index?id=";

    private final OrderService orderService;
    private final UserMapper userMapper;
    private final WechatClient wechatClient;
    private final AppProperties appProperties;

    public OrderNotifyResultVO notifyBossByCustomer(Long orderId) {
        RoleChecker.requireCustomer();
        OrderVO order = orderService.getDetail(orderId);
        if (!"PENDING_CONFIRM".equals(order.getStatus())) {
            throw BusinessException.of(400, "仅待商家确认订单可提醒老板");
        }

        String templateId = appProperties.getWechat().getOrderNotifyTemplateId();
        if (!StringUtils.hasText(templateId)) {
            return OrderNotifyResultVO.builder()
                    .notified(0)
                    .failed(0)
                    .message("未配置微信订阅消息模板，请使用「转发给老板」")
                    .build();
        }

        List<String> openids = findBossOpenids();
        if (openids.isEmpty()) {
            return OrderNotifyResultVO.builder()
                    .notified(0)
                    .failed(0)
                    .message("未找到老板微信账号，请使用「转发给老板」")
                    .build();
        }

        Map<String, Map<String, String>> data = buildNotifyData(order);
        String page = BOSS_ORDER_PAGE + order.getId();
        int notified = 0;
        int failed = 0;
        for (String openid : openids) {
            try {
                wechatClient.sendSubscribeMessage(openid, templateId.trim(), page, data);
                notified++;
            } catch (Exception ex) {
                failed++;
                log.warn("send subscribe message failed, openid={}", openid, ex);
            }
        }

        String message;
        if (notified > 0) {
            message = failed > 0
                    ? String.format("已向 %d 位老板发送微信提醒，%d 位未订阅或发送失败", notified, failed)
                    : String.format("已向 %d 位老板发送微信提醒", notified);
        } else {
            message = "老板尚未订阅订单提醒，请让老板在订单页开启提醒，或改用「转发给老板」";
        }
        return OrderNotifyResultVO.builder()
                .notified(notified)
                .failed(failed)
                .message(message)
                .build();
    }

    private List<String> findBossOpenids() {
        Set<String> openids = new LinkedHashSet<>();
        List<User> bosses = userMapper.selectList(new LambdaQueryWrapper<User>()
                .in(User::getRole, UserRole.OWNER_ADMIN.name(), UserRole.PARTNER_ADMIN.name())
                .isNotNull(User::getOpenid)
                .ne(User::getOpenid, ""));
        for (User user : bosses) {
            openids.add(user.getOpenid());
        }
        openids.addAll(appProperties.getAdmin().getOpenidWhitelist());
        return new ArrayList<>(openids);
    }

    private Map<String, Map<String, String>> buildNotifyData(OrderVO order) {
        Map<String, Map<String, String>> data = new LinkedHashMap<>();
        data.put("thing1", field(truncate(order.getCustomerName(), 20)));
        data.put("character_string2", field(truncate(order.getOrderNo(), 32)));
        data.put("thing3", field(truncate(buildItemSummary(order), 20)));
        data.put("time4", field(formatTime(order.getCreatedAt())));
        return data;
    }

    private String buildItemSummary(OrderVO order) {
        List<OrderItemVO> items = order.getItems();
        if (items == null || items.isEmpty()) {
            int count = order.getItemCount() != null ? order.getItemCount() : 0;
            return count > 0 ? count + "种商品" : "新订单待确认";
        }
        String first = items.get(0).getProductName();
        if (items.size() == 1) {
            return first + "等1种";
        }
        return first + "等" + items.size() + "种";
    }

    private Map<String, String> field(String value) {
        return Map.of("value", value);
    }

    private String truncate(String value, int maxLen) {
        if (!StringUtils.hasText(value)) {
            return "-";
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLen ? trimmed : trimmed.substring(0, maxLen);
    }

    private String formatTime(LocalDateTime createdAt) {
        if (createdAt == null) {
            return "-";
        }
        return createdAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }
}
