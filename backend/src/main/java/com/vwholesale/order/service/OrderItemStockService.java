package com.vwholesale.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.vwholesale.common.context.MerchantContext;
import com.vwholesale.common.enums.OrderStatus;
import com.vwholesale.common.exception.BusinessException;
import com.vwholesale.order.entity.Order;
import com.vwholesale.order.entity.OrderItem;
import com.vwholesale.order.mapper.OrderItemMapper;
import com.vwholesale.order.mapper.OrderMapper;
import com.vwholesale.product.entity.Product;
import com.vwholesale.product.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderItemStockService {

    /** 仅已确认（待分拣/分拣中）可扣减库存 */
    private static final Set<String> STOCK_SYNC_STATUSES = Set.of(
            OrderStatus.PENDING_PICK,
            OrderStatus.PICKING
    );

    private final ProductMapper productMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderMapper orderMapper;
    private final MerchantContext merchantContext;

    /**
     * 老板确认订单时，按下单数量一次性扣减库存。
     */
    public void applyConfirmStock(Long orderId) {
        if (orderId == null) {
            return;
        }
        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, orderId));
        for (OrderItem item : items) {
            BigDecimal orderQty = item.getOrderQty() != null ? item.getOrderQty() : BigDecimal.ZERO;
            BigDecimal applied = item.getStockAppliedQty() != null ? item.getStockAppliedQty() : BigDecimal.ZERO;
            BigDecimal delta = orderQty.subtract(applied);
            if (delta.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            applyStockChange(item.getProductId(), delta);
            item.setStockAppliedQty(orderQty);
            orderItemMapper.updateById(item);
        }
    }

    /**
     * 按「出库数 - 已扣库存数」差量同步库存，用于拣单过程中实拣数与下单数不一致时调整。
     */
    public void syncPickStock(OrderItem item) {
        if (item == null || item.getId() == null) {
            return;
        }
        Order order = orderMapper.selectById(item.getOrderId());
        assertStockSyncAllowed(order);

        BigDecimal actual = item.getActualQty() != null ? item.getActualQty() : BigDecimal.ZERO;
        BigDecimal applied = item.getStockAppliedQty() != null ? item.getStockAppliedQty() : BigDecimal.ZERO;
        BigDecimal delta = actual.subtract(applied);
        if (delta.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }
        applyStockChange(item.getProductId(), delta);
        item.setStockAppliedQty(actual);
        orderItemMapper.updateById(item);
    }

    public void reverseOrderStock(Long orderId) {
        if (orderId == null) {
            return;
        }
        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, orderId));
        for (OrderItem item : items) {
            reversePickStock(item);
        }
    }

    public void reversePickStock(OrderItem item) {
        if (item == null || item.getId() == null) {
            return;
        }
        BigDecimal applied = item.getStockAppliedQty() != null ? item.getStockAppliedQty() : BigDecimal.ZERO;
        if (applied.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }
        applyStockChange(item.getProductId(), applied.negate());
        item.setStockAppliedQty(BigDecimal.ZERO);
        orderItemMapper.updateById(item);
    }

    private void assertStockSyncAllowed(Order order) {
        if (order == null || !STOCK_SYNC_STATUSES.contains(order.getStatus())) {
            throw BusinessException.of(400, "当前订单状态不可出库扣减库存，请改单后重新分拣");
        }
    }

    private void applyStockChange(Long productId, BigDecimal delta) {
        if (productId == null || delta.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }
        Product product = productMapper.selectOne(new LambdaQueryWrapper<Product>()
                .eq(Product::getId, productId)
                .eq(Product::getMerchantId, merchantContext.currentMerchantId()));
        if (product == null) {
            log.warn("sync pick stock skipped: product {} not found for merchant {}", productId,
                    merchantContext.currentMerchantId());
            throw BusinessException.of(400, "商品不存在，无法扣减库存");
        }
        BigDecimal stock = product.getStockQty() != null ? product.getStockQty() : BigDecimal.ZERO;
        product.setStockQty(stock.subtract(delta).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP));
        productMapper.updateById(product);
    }
}
