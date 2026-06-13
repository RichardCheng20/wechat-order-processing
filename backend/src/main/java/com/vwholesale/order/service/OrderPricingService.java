package com.vwholesale.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.vwholesale.common.context.MerchantContext;
import com.vwholesale.common.enums.OrderStatus;
import com.vwholesale.common.exception.BusinessException;
import com.vwholesale.common.security.RoleChecker;
import com.vwholesale.customer.entity.Customer;
import com.vwholesale.customer.mapper.CustomerMapper;
import com.vwholesale.order.dto.OrderPricingItemVO;
import com.vwholesale.order.dto.OrderPricingSubmitRequest;
import com.vwholesale.order.dto.OrderPricingVO;
import com.vwholesale.order.dto.PricingItemRequest;
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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderPricingService {

    private static final Set<String> PRICEABLE_STATUSES = Set.of(
            OrderStatus.PENDING_CONFIRM,
            OrderStatus.PENDING_PICK,
            OrderStatus.PENDING_PRICE
    );

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final CustomerMapper customerMapper;
    private final ProductMapper productMapper;
    private final ProductPriceService productPriceService;
    private final MerchantContext merchantContext;

    public List<OrderPricingVO> listPendingPrice() {
        RoleChecker.requireBoss();
        List<Order> orders = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .eq(Order::getMerchantId, merchantContext.currentMerchantId())
                .in(Order::getStatus, PRICEABLE_STATUSES)
                .isNull(Order::getAmount)
                .orderByDesc(Order::getId));
        return orders.stream().map(this::toListVO).toList();
    }

    public List<OrderPricingVO> listPricedUnpublished() {
        RoleChecker.requireBoss();
        List<Order> orders = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .eq(Order::getMerchantId, merchantContext.currentMerchantId())
                .eq(Order::getStatus, OrderStatus.PRICED)
                .orderByDesc(Order::getId));
        return orders.stream().map(this::toListVO).toList();
    }

    public OrderPricingVO getPricingDetail(Long orderId) {
        RoleChecker.requireBoss();
        Order order = getOrderOrThrow(orderId);
        if (!canViewPricing(order)) {
            throw BusinessException.of(400, "当前订单状态不支持录价");
        }
        return toDetailVO(order);
    }

    @Transactional
    public OrderPricingVO submitPricing(Long orderId, OrderPricingSubmitRequest request) {
        RoleChecker.requireBoss();
        Order order = getOrderOrThrow(orderId);
        if (!PRICEABLE_STATUSES.contains(order.getStatus())) {
            throw BusinessException.of(400, "只有待录价订单可以提交录价");
        }

        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, orderId)
                .orderByAsc(OrderItem::getId));
        Map<Long, OrderItem> itemMap = items.stream().collect(Collectors.toMap(OrderItem::getId, Function.identity()));
        Map<Long, PricingItemRequest> priceMap = request.getItems().stream()
                .filter(i -> i.getItemId() != null)
                .collect(Collectors.toMap(PricingItemRequest::getItemId, Function.identity(), (a, b) -> b));

        if (priceMap.size() != items.size()) {
            throw BusinessException.of(400, "请为所有商品填写成交单价");
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (OrderItem item : items) {
            PricingItemRequest priceReq = priceMap.get(item.getId());
            if (priceReq == null || priceReq.getDealPrice() == null) {
                throw BusinessException.of(400, "请为所有商品填写成交单价");
            }
            if (priceReq.getDealPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw BusinessException.of(400, "成交单价不能为负数");
            }

            BigDecimal qty = item.getActualQty() != null ? item.getActualQty() : item.getOrderQty();
            BigDecimal subtotal = priceReq.getDealPrice().multiply(qty).setScale(2, RoundingMode.HALF_UP);

            item.setDealPrice(priceReq.getDealPrice());
            item.setSubtotalAmount(subtotal);
            orderItemMapper.updateById(item);
            totalAmount = totalAmount.add(subtotal);
        }

        order.setAmount(totalAmount.setScale(2, RoundingMode.HALF_UP));
        order.setReceivableAmount(order.getAmount());
        order.setStatus(OrderStatus.PRICED);
        orderMapper.updateById(order);

        return toDetailVO(order);
    }

    @Transactional
    public OrderPricingVO publishToCustomer(Long orderId) {
        RoleChecker.requireBoss();
        Order order = getOrderOrThrow(orderId);
        if (!OrderStatus.PRICED.equals(order.getStatus())) {
            throw BusinessException.of(400, "只有已录价待推送的订单可以推送给客户");
        }
        if (order.getAmount() == null) {
            throw BusinessException.of(400, "订单尚未录价，无法推送");
        }
        order.setStatus(OrderStatus.COMPLETED);
        orderMapper.updateById(order);
        return toDetailVO(order);
    }

    private boolean canViewPricing(Order order) {
        return PRICEABLE_STATUSES.contains(order.getStatus())
                || OrderStatus.PRICED.equals(order.getStatus())
                || OrderStatus.COMPLETED.equals(order.getStatus());
    }

    private OrderPricingVO toListVO(Order order) {
        Customer customer = customerMapper.selectById(order.getCustomerId());
        long itemCount = orderItemMapper.selectCount(new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId()));
        return OrderPricingVO.builder()
                .id(order.getId())
                .orderNo(order.getOrderNo())
                .customerId(order.getCustomerId())
                .customerName(customer != null ? customer.getName() : null)
                .status(order.getStatus())
                .statusLabel(statusLabel(order.getStatus()))
                .deliveryDate(order.getDeliveryDate())
                .deliveryAddressShort(order.getDeliveryAddressShort())
                .amount(order.getAmount())
                .remark(order.getRemark())
                .createdAt(order.getCreatedAt())
                .items(List.of())
                .build();
    }

    private OrderPricingVO toDetailVO(Order order) {
        Customer customer = customerMapper.selectById(order.getCustomerId());
        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, order.getId())
                .orderByAsc(OrderItem::getId));

        Set<Long> productIds = items.stream().map(OrderItem::getProductId).collect(Collectors.toSet());
        Map<Long, Product> productMap = productIds.isEmpty()
                ? Map.of()
                : productMapper.selectBatchIds(productIds).stream().collect(Collectors.toMap(Product::getId, p -> p));

        LocalDate priceDate = order.getDeliveryDate() != null ? order.getDeliveryDate() : LocalDate.now();
        Map<Long, BigDecimal> defaultPrices = productMap.values().stream()
                .collect(Collectors.toMap(Product::getId, p -> p.getDefaultPrice() != null ? p.getDefaultPrice() : BigDecimal.ZERO));
        Map<Long, BigDecimal> referencePrices = productPriceService.batchResolveReferencePrices(
                productIds.stream().toList(), order.getCustomerId(), defaultPrices, priceDate);

        List<OrderPricingItemVO> itemVOs = items.stream().map(item -> {
            Product product = productMap.get(item.getProductId());
            BigDecimal refPrice = referencePrices.get(item.getProductId());
            return OrderPricingItemVO.builder()
                    .id(item.getId())
                    .productId(item.getProductId())
                    .productName(product != null ? product.getName() : "未知商品")
                    .orderQty(item.getOrderQty())
                    .actualQty(item.getActualQty())
                    .unit(item.getUnit())
                    .referencePrice(refPrice)
                    .dealPrice(item.getDealPrice() != null ? item.getDealPrice() : refPrice)
                    .subtotalAmount(item.getSubtotalAmount())
                    .shortageFlag(item.getShortageFlag())
                    .build();
        }).toList();

        return OrderPricingVO.builder()
                .id(order.getId())
                .orderNo(order.getOrderNo())
                .customerId(order.getCustomerId())
                .customerName(customer != null ? customer.getName() : null)
                .status(order.getStatus())
                .statusLabel(statusLabel(order.getStatus()))
                .deliveryDate(order.getDeliveryDate())
                .deliveryAddressShort(order.getDeliveryAddressShort())
                .amount(order.getAmount())
                .remark(order.getRemark())
                .createdAt(order.getCreatedAt())
                .items(itemVOs)
                .build();
    }

    private Order getOrderOrThrow(Long id) {
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getId, id)
                .eq(Order::getMerchantId, merchantContext.currentMerchantId()));
        if (order == null) {
            throw BusinessException.of(404, "订单不存在");
        }
        return order;
    }

    private String statusLabel(String status) {
        if (Objects.equals(status, OrderStatus.PENDING_CONFIRM)) {
            return "待确认";
        }
        if (Objects.equals(status, OrderStatus.PENDING_PICK)) {
            return "待分拣";
        }
        if (Objects.equals(status, OrderStatus.PENDING_PRICE)) {
            return "待录价";
        }
        if (Objects.equals(status, OrderStatus.PRICED)) {
            return "待推送";
        }
        if (Objects.equals(status, OrderStatus.COMPLETED)) {
            return "已完成";
        }
        return status;
    }
}
