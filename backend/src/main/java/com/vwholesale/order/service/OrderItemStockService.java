package com.vwholesale.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.vwholesale.common.context.MerchantContext;
import com.vwholesale.common.enums.OrderStatus;
import com.vwholesale.common.exception.BusinessException;
import com.vwholesale.order.entity.Order;
import com.vwholesale.order.entity.OrderItem;
import com.vwholesale.order.mapper.OrderItemMapper;
import com.vwholesale.order.mapper.OrderMapper;
import com.vwholesale.order.support.OrderItemDisplay;
import com.vwholesale.product.entity.Product;
import com.vwholesale.product.mapper.ProductMapper;
import com.vwholesale.product.service.ProductService;
import com.vwholesale.product.support.ProductStockSupport;
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

    /** 仅已确认（待分拣/分拣中）可调整实物出库 */
    private static final Set<String> STOCK_SYNC_STATUSES = Set.of(
            OrderStatus.PENDING_PICK,
            OrderStatus.PICKING
    );

    private static final Set<String> RESERVATION_STATUSES = Set.of(
            OrderStatus.PENDING_CONFIRM
    );

    private final ProductMapper productMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderMapper orderMapper;
    private final MerchantContext merchantContext;

    /**
     * 下单时占用可用库存（仅待确认订单）。
     * 库存不足时不拦截下单：先卖后采场景下，库存仅代表仓内剩余，客户需求优先。
     */
    public void applyOrderReservation(Long orderId) {
        if (orderId == null) {
            return;
        }
        Order order = orderMapper.selectById(orderId);
        if (order == null || !RESERVATION_STATUSES.contains(order.getStatus())) {
            return;
        }
        List<OrderItem> items = listTrackableItems(orderId);
        for (OrderItem item : items) {
            reserveItemStock(item);
        }
    }

    /**
     * 老板确认订单：释放占用并扣减实物库存。
     */
    public void applyConfirmStock(Long orderId) {
        if (orderId == null) {
            return;
        }
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            return;
        }
        List<OrderItem> items = listTrackableItems(orderId);
        for (OrderItem item : items) {
            BigDecimal orderQty = orderQty(item);
            BigDecimal applied = appliedQty(item);
            if (orderQty.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            Product product = loadProductOrThrow(item.getProductId());
            if (applied.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal reservedRelease = applied.min(orderQty);
                product.setReservedQty(ProductStockSupport.reservedQty(product)
                        .subtract(reservedRelease)
                        .max(BigDecimal.ZERO)
                        .setScale(2, RoundingMode.HALF_UP));
                product.setStockQty(ProductStockSupport.physicalStock(product)
                        .subtract(orderQty)
                        .max(BigDecimal.ZERO)
                        .setScale(2, RoundingMode.HALF_UP));
                productMapper.updateById(product);
                item.setStockAppliedQty(orderQty);
                orderItemMapper.updateById(item);
            } else {
                applyPhysicalDeduction(product, item, orderQty);
            }
        }
    }

    /**
     * 改单后直接进入已确认状态：按下单数扣减实物库存。
     */
    public void applyPhysicalStock(Long orderId) {
        if (orderId == null) {
            return;
        }
        List<OrderItem> items = listTrackableItems(orderId);
        for (OrderItem item : items) {
            BigDecimal orderQty = orderQty(item);
            BigDecimal applied = appliedQty(item);
            BigDecimal delta = orderQty.subtract(applied);
            if (delta.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            Product product = loadProductOrThrow(item.getProductId());
            applyPhysicalDeduction(product, item, orderQty);
        }
    }

    /**
     * 按「出库数 - 已扣实物数」差量同步库存，用于拣单过程中实拣数与下单数不一致时调整。
     */
    public void syncPickStock(OrderItem item) {
        if (item == null || item.getId() == null) {
            return;
        }
        Order order = orderMapper.selectById(item.getOrderId());
        assertStockSyncAllowed(order);
        if (OrderItemDisplay.isCustomItem(item)) {
            return;
        }

        BigDecimal actual = item.getActualQty() != null ? item.getActualQty() : BigDecimal.ZERO;
        BigDecimal applied = appliedQty(item);
        BigDecimal delta = actual.subtract(applied);
        if (delta.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }
        Product product = loadProductOrThrow(item.getProductId());
        product.setStockQty(ProductStockSupport.physicalStock(product)
                .subtract(delta)
                .max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP));
        productMapper.updateById(product);
        item.setStockAppliedQty(actual);
        orderItemMapper.updateById(item);
    }

    public void reverseOrderStock(Long orderId) {
        if (orderId == null) {
            return;
        }
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            return;
        }
        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, orderId));
        for (OrderItem item : items) {
            reverseLineStock(order, item);
        }
    }

    public void reversePickStock(OrderItem item) {
        if (item == null || item.getId() == null) {
            return;
        }
        Order order = orderMapper.selectById(item.getOrderId());
        if (order == null) {
            return;
        }
        reverseLineStock(order, item);
    }

    private void reserveItemStock(OrderItem item) {
        BigDecimal orderQty = orderQty(item);
        if (orderQty.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        BigDecimal applied = appliedQty(item);
        BigDecimal pending = orderQty.subtract(applied);
        if (pending.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        Product product = loadProductOrThrow(item.getProductId());
        BigDecimal available = ProductStockSupport.availableStock(product).max(BigDecimal.ZERO);
        BigDecimal reserveQty = pending.min(available);
        if (reserveQty.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        product.setReservedQty(ProductStockSupport.reservedQty(product)
                .add(reserveQty)
                .setScale(2, RoundingMode.HALF_UP));
        productMapper.updateById(product);
        item.setStockAppliedQty(applied.add(reserveQty));
        orderItemMapper.updateById(item);
    }

    private void applyPhysicalDeduction(Product product, OrderItem item, BigDecimal targetQty) {
        BigDecimal applied = appliedQty(item);
        BigDecimal delta = targetQty.subtract(applied);
        if (delta.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        product.setStockQty(ProductStockSupport.physicalStock(product)
                .subtract(delta)
                .max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP));
        productMapper.updateById(product);
        item.setStockAppliedQty(targetQty);
        orderItemMapper.updateById(item);
    }

    private void reverseLineStock(Order order, OrderItem item) {
        if (OrderItemDisplay.isCustomItem(item)) {
            return;
        }
        BigDecimal applied = appliedQty(item);
        if (applied.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }
        Product product = loadProductOrThrow(item.getProductId());
        if (RESERVATION_STATUSES.contains(order.getStatus())) {
            product.setReservedQty(ProductStockSupport.reservedQty(product)
                    .subtract(applied)
                    .max(BigDecimal.ZERO)
                    .setScale(2, RoundingMode.HALF_UP));
        } else {
            product.setStockQty(ProductStockSupport.physicalStock(product)
                    .add(applied)
                    .setScale(2, RoundingMode.HALF_UP));
        }
        productMapper.updateById(product);
        item.setStockAppliedQty(BigDecimal.ZERO);
        orderItemMapper.updateById(item);
    }

    private List<OrderItem> listTrackableItems(Long orderId) {
        return orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                        .eq(OrderItem::getOrderId, orderId))
                .stream()
                .filter(item -> !OrderItemDisplay.isCustomItem(item))
                .toList();
    }

    private void assertStockSyncAllowed(Order order) {
        if (order == null || !STOCK_SYNC_STATUSES.contains(order.getStatus())) {
            throw BusinessException.of(400, "当前订单状态不可出库扣减库存，请改单后重新分拣");
        }
    }

    private Product loadProductOrThrow(Long productId) {
        Product product = productMapper.selectOne(new LambdaQueryWrapper<Product>()
                .eq(Product::getId, productId)
                .eq(Product::getMerchantId, merchantContext.currentMerchantId()));
        if (product == null || ProductService.isCustomOrderProduct(product)) {
            log.warn("stock sync skipped: product {} not found for merchant {}", productId,
                    merchantContext.currentMerchantId());
            throw BusinessException.of(400, "商品不存在，无法同步库存");
        }
        return product;
    }

    private static BigDecimal orderQty(OrderItem item) {
        return item.getOrderQty() != null ? item.getOrderQty() : BigDecimal.ZERO;
    }

    private static BigDecimal appliedQty(OrderItem item) {
        return item.getStockAppliedQty() != null ? item.getStockAppliedQty() : BigDecimal.ZERO;
    }
}
