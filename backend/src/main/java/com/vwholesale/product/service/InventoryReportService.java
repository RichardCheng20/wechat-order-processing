package com.vwholesale.product.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.vwholesale.common.context.MerchantContext;
import com.vwholesale.common.enums.OrderStatus;
import com.vwholesale.common.exception.BusinessException;
import com.vwholesale.common.security.RoleChecker;
import com.vwholesale.order.entity.Order;
import com.vwholesale.order.entity.OrderItem;
import com.vwholesale.order.mapper.OrderItemMapper;
import com.vwholesale.order.mapper.OrderMapper;
import com.vwholesale.order.support.OrderItemDisplay;
import com.vwholesale.product.dto.BossInventoryReportVO;
import com.vwholesale.product.dto.InventoryReportRow;
import com.vwholesale.product.dto.InventoryReportSummary;
import com.vwholesale.product.entity.Product;
import com.vwholesale.product.entity.ProductPurchasePriceRecord;
import com.vwholesale.product.mapper.ProductMapper;
import com.vwholesale.product.mapper.ProductPurchasePriceMapper;
import com.vwholesale.product.support.ProductStockSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryReportService {

    private final ProductMapper productMapper;
    private final ProductPurchasePriceMapper productPurchasePriceMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final MerchantContext merchantContext;

    public BossInventoryReportVO report(LocalDate dateFrom, LocalDate dateTo, String dateType) {
        RoleChecker.requireOwnerAdmin();
        validateDateRange(dateFrom, dateTo);
        boolean byOrderDate = "ORDER".equalsIgnoreCase(dateType != null ? dateType.trim() : "DELIVERY");
        Long merchantId = merchantContext.currentMerchantId();

        List<Product> products = productMapper.selectList(new LambdaQueryWrapper<Product>()
                        .eq(Product::getMerchantId, merchantId)
                        .orderByAsc(Product::getId))
                .stream()
                .filter(p -> !ProductService.isCustomOrderProduct(p))
                .toList();

        Map<Long, BigDecimal> inboundMap = loadInboundQty(merchantId, dateFrom, dateTo);
        Map<Long, BigDecimal> outboundMap = loadOutboundQty(merchantId, dateFrom, dateTo, byOrderDate);

        BigDecimal totalInbound = BigDecimal.ZERO;
        BigDecimal totalOutbound = BigDecimal.ZERO;

        List<InventoryReportRow> rows = products.stream()
                .map(product -> {
                    BigDecimal inbound = inboundMap.getOrDefault(product.getId(), BigDecimal.ZERO);
                    BigDecimal outbound = outboundMap.getOrDefault(product.getId(), BigDecimal.ZERO);
                    return InventoryReportRow.builder()
                            .productId(product.getId())
                            .categoryId(product.getCategoryId())
                            .productName(product.getName())
                            .unit(product.getUnit())
                            .inboundQty(inbound)
                            .outboundQty(outbound)
                            .stockQty(ProductStockSupport.physicalStock(product))
                            .availableQty(ProductStockSupport.availableStock(product))
                            .build();
                })
                .sorted(Comparator
                        .comparing(InventoryReportRow::getCategoryId, Comparator.nullsLast(Long::compareTo))
                        .thenComparing(InventoryReportRow::getProductName))
                .toList();

        for (InventoryReportRow row : rows) {
            totalInbound = totalInbound.add(row.getInboundQty());
            totalOutbound = totalOutbound.add(row.getOutboundQty());
        }

        InventoryReportSummary summary = InventoryReportSummary.builder()
                .inboundQty(totalInbound)
                .outboundQty(totalOutbound)
                .productCount(rows.size())
                .build();

        return BossInventoryReportVO.builder()
                .summary(summary)
                .rows(rows)
                .build();
    }

    private Map<Long, BigDecimal> loadInboundQty(Long merchantId, LocalDate dateFrom, LocalDate dateTo) {
        List<ProductPurchasePriceRecord> records = productPurchasePriceMapper.selectList(
                new LambdaQueryWrapper<ProductPurchasePriceRecord>()
                        .eq(ProductPurchasePriceRecord::getMerchantId, merchantId)
                        .eq(ProductPurchasePriceRecord::getStatus, 1)
                        .ge(ProductPurchasePriceRecord::getEffectiveDate, dateFrom)
                        .le(ProductPurchasePriceRecord::getEffectiveDate, dateTo));

        Map<Long, BigDecimal> inboundMap = new HashMap<>();
        for (ProductPurchasePriceRecord record : records) {
            if (record.getProductId() == null || record.getPurchasedQty() == null) {
                continue;
            }
            if (record.getPurchasedQty().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            inboundMap.merge(record.getProductId(), record.getPurchasedQty(), BigDecimal::add);
        }
        return inboundMap;
    }

    private Map<Long, BigDecimal> loadOutboundQty(Long merchantId, LocalDate dateFrom, LocalDate dateTo,
                                                  boolean byOrderDate) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .eq(Order::getMerchantId, merchantId)
                .ne(Order::getStatus, OrderStatus.CANCELLED)
                .ne(Order::getStatus, OrderStatus.PENDING_CONFIRM);
        if (byOrderDate) {
            wrapper.ge(Order::getCreatedAt, dateFrom.atStartOfDay())
                    .lt(Order::getCreatedAt, dateTo.plusDays(1).atStartOfDay());
        } else {
            wrapper.ge(Order::getDeliveryDate, dateFrom)
                    .le(Order::getDeliveryDate, dateTo);
        }

        List<Order> orders = orderMapper.selectList(wrapper);
        if (orders.isEmpty()) {
            return Map.of();
        }

        List<Long> orderIds = orders.stream().map(Order::getId).toList();
        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .in(OrderItem::getOrderId, orderIds));

        Map<Long, BigDecimal> outboundMap = new HashMap<>();
        for (OrderItem item : items) {
            if (OrderItemDisplay.isCustomItem(item) || item.getProductId() == null) {
                continue;
            }
            BigDecimal qty = outboundQty(item);
            if (qty.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            outboundMap.merge(item.getProductId(), qty, BigDecimal::add);
        }
        return outboundMap;
    }

    private BigDecimal outboundQty(OrderItem item) {
        BigDecimal applied = item.getStockAppliedQty() != null ? item.getStockAppliedQty() : BigDecimal.ZERO;
        if (applied.compareTo(BigDecimal.ZERO) > 0) {
            return applied;
        }
        if (item.getActualQty() != null && item.getActualQty().compareTo(BigDecimal.ZERO) > 0) {
            return item.getActualQty();
        }
        return item.getOrderQty() != null ? item.getOrderQty() : BigDecimal.ZERO;
    }

    private void validateDateRange(LocalDate dateFrom, LocalDate dateTo) {
        if (dateFrom == null || dateTo == null) {
            throw BusinessException.of(400, "请选择日期范围");
        }
        if (dateFrom.isAfter(dateTo)) {
            throw BusinessException.of(400, "开始日期不能晚于结束日期");
        }
        if (dateFrom.plusDays(366).isBefore(dateTo)) {
            throw BusinessException.of(400, "日期范围不能超过 366 天");
        }
    }
}
