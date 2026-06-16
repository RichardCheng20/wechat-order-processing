package com.vwholesale.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.vwholesale.common.context.MerchantContext;
import com.vwholesale.common.enums.OrderStatus;
import com.vwholesale.common.exception.BusinessException;
import com.vwholesale.common.security.RoleChecker;
import com.vwholesale.order.dto.BossPickItemUpdateRequest;
import com.vwholesale.order.dto.OrderVO;
import com.vwholesale.order.entity.Order;
import com.vwholesale.order.entity.OrderItem;
import com.vwholesale.order.mapper.OrderItemMapper;
import com.vwholesale.order.mapper.OrderMapper;
import com.vwholesale.product.entity.Product;
import com.vwholesale.product.mapper.ProductMapper;
import com.vwholesale.product.service.ProductPriceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderPickService {

    private static final Set<String> PICKABLE_STATUSES = Set.of(
            OrderStatus.PENDING_CONFIRM,
            OrderStatus.PENDING_PICK,
            OrderStatus.PICKING,
            OrderStatus.PENDING_PRICE
    );

    private static final Set<String> EDITABLE_STATUSES = Set.of(
            OrderStatus.PENDING_CONFIRM,
            OrderStatus.PENDING_PICK,
            OrderStatus.PICKING
    );

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductMapper productMapper;
    private final ProductPriceService productPriceService;
    private final OrderService orderService;
    private final MerchantContext merchantContext;

    public OrderVO getPickDetail(Long orderId) {
        RoleChecker.requireBoss();
        Order order = getOrderOrThrow(orderId);
        assertPickable(order);
        return orderService.getDetail(orderId);
    }

    @Transactional
    public OrderVO startPick(Long orderId) {
        RoleChecker.requireBoss();
        Order order = getOrderOrThrow(orderId);
        assertPickable(order);
        if (OrderStatus.PENDING_CONFIRM.equals(order.getStatus())
                || OrderStatus.PENDING_PICK.equals(order.getStatus())) {
            order.setStatus(OrderStatus.PICKING);
            orderMapper.updateById(order);
        }
        return orderService.getDetail(orderId);
    }

    @Transactional
    public OrderVO updateItem(Long orderId, Long itemId, BossPickItemUpdateRequest request) {
        RoleChecker.requireBoss();
        Order order = getOrderOrThrow(orderId);
        ensureEditing(order);

        OrderItem item = orderItemMapper.selectOne(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getId, itemId)
                .eq(OrderItem::getOrderId, orderId));
        if (item == null) {
            throw BusinessException.of(404, "订单明细不存在");
        }

        if (request.getActualQty() != null) {
            item.setActualQty(request.getActualQty());
        }
        if (request.getDealPrice() != null) {
            if (request.getDealPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw BusinessException.of(400, "单价不能为负数");
            }
            item.setDealPrice(request.getDealPrice());
        }
        if (request.getShortageFlag() != null) {
            item.setShortageFlag(request.getShortageFlag());
            if (request.getShortageFlag() == 1) {
                item.setActualQty(BigDecimal.ZERO);
            }
        }
        orderItemMapper.updateById(item);
        return orderService.getDetail(orderId);
    }

    @Transactional
    public OrderVO fillActualQty(Long orderId) {
        RoleChecker.requireBoss();
        Order order = getOrderOrThrow(orderId);
        ensureEditing(order);
        List<OrderItem> items = listItems(orderId);
        for (OrderItem item : items) {
            if (item.getShortageFlag() != null && item.getShortageFlag() == 1) {
                continue;
            }
            item.setActualQty(item.getOrderQty());
            orderItemMapper.updateById(item);
        }
        return orderService.getDetail(orderId);
    }

    @Transactional
    public OrderVO fetchPrices(Long orderId) {
        RoleChecker.requireBoss();
        Order order = getOrderOrThrow(orderId);
        ensureEditing(order);

        List<OrderItem> items = listItems(orderId);
        Set<Long> productIds = items.stream().map(OrderItem::getProductId).collect(Collectors.toSet());
        Map<Long, Product> productMap = productIds.isEmpty()
                ? Map.of()
                : productMapper.selectBatchIds(productIds).stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        LocalDate priceDate = order.getDeliveryDate() != null ? order.getDeliveryDate() : LocalDate.now();
        Map<Long, BigDecimal> defaultPrices = productMap.values().stream()
                .collect(Collectors.toMap(Product::getId,
                        p -> p.getDefaultPrice() != null ? p.getDefaultPrice() : BigDecimal.ZERO));
        Map<Long, BigDecimal> refPrices = productPriceService.batchResolveReferencePrices(
                productIds.stream().toList(), order.getCustomerId(), defaultPrices, priceDate);

        for (OrderItem item : items) {
            BigDecimal price = refPrices.get(item.getProductId());
            if (price != null) {
                item.setDealPrice(price);
                orderItemMapper.updateById(item);
            }
        }
        return orderService.getDetail(orderId);
    }

    @Transactional
    public OrderVO completePick(Long orderId) {
        RoleChecker.requireBoss();
        Order order = getOrderOrThrow(orderId);
        if (!PICKABLE_STATUSES.contains(order.getStatus())) {
            throw BusinessException.of(400, "当前订单不可完成分拣");
        }
        if (OrderStatus.PENDING_CONFIRM.equals(order.getStatus())
                || OrderStatus.PENDING_PICK.equals(order.getStatus())) {
            order.setStatus(OrderStatus.PICKING);
        }

        List<OrderItem> items = listItems(orderId);
        for (OrderItem item : items) {
            if (item.getShortageFlag() != null && item.getShortageFlag() == 1) {
                if (item.getActualQty() == null) {
                    item.setActualQty(BigDecimal.ZERO);
                }
            } else if (item.getActualQty() == null) {
                item.setActualQty(item.getOrderQty());
            }
            orderItemMapper.updateById(item);
        }

        order.setStatus(OrderStatus.PENDING_PRICE);
        orderMapper.updateById(order);
        return orderService.getDetail(orderId);
    }

    private void assertPickable(Order order) {
        if (!PICKABLE_STATUSES.contains(order.getStatus())) {
            throw BusinessException.of(400, "当前订单状态不支持分拣");
        }
        if (OrderStatus.PENDING_PRICE.equals(order.getStatus()) && order.getAmount() != null) {
            throw BusinessException.of(400, "订单已录价，无需分拣");
        }
    }

    private void ensureEditing(Order order) {
        if (!EDITABLE_STATUSES.contains(order.getStatus()) && !OrderStatus.PICKING.equals(order.getStatus())) {
            if (OrderStatus.PENDING_PRICE.equals(order.getStatus()) && order.getAmount() == null) {
                return;
            }
            throw BusinessException.of(400, "当前订单状态不可编辑分拣信息");
        }
        if (OrderStatus.PENDING_CONFIRM.equals(order.getStatus())
                || OrderStatus.PENDING_PICK.equals(order.getStatus())) {
            order.setStatus(OrderStatus.PICKING);
            orderMapper.updateById(order);
        }
    }

    private List<OrderItem> listItems(Long orderId) {
        return orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, orderId)
                .orderByAsc(OrderItem::getId));
    }

    private Order getOrderOrThrow(Long orderId) {
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getId, orderId)
                .eq(Order::getMerchantId, merchantContext.currentMerchantId()));
        if (order == null) {
            throw BusinessException.of(404, "订单不存在");
        }
        return order;
    }
}
