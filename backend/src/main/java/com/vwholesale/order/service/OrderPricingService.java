package com.vwholesale.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.vwholesale.common.context.MerchantContext;
import com.vwholesale.common.enums.OrderStatus;
import com.vwholesale.common.exception.BusinessException;
import com.vwholesale.common.security.RoleChecker;
import com.vwholesale.customer.entity.Customer;
import com.vwholesale.customer.mapper.CustomerMapper;
import com.vwholesale.mq.model.OrderEventMessage;
import com.vwholesale.mq.publisher.OrderEventAfterCommitPublisher;
import com.vwholesale.order.dto.OrderPricingItemVO;
import com.vwholesale.order.dto.OrderPricingSubmitRequest;
import com.vwholesale.order.dto.OrderPricingVO;
import com.vwholesale.order.dto.PricingItemRequest;
import com.vwholesale.order.dto.PricingProductDetailVO;
import com.vwholesale.order.dto.PricingProductLineVO;
import com.vwholesale.order.dto.PricingProductSubmitRequest;
import com.vwholesale.order.dto.PricingProductSummaryVO;
import com.vwholesale.order.entity.Order;
import com.vwholesale.order.entity.OrderItem;
import com.vwholesale.order.mapper.OrderItemMapper;
import com.vwholesale.order.support.OrderItemDisplay;
import com.vwholesale.order.mapper.OrderMapper;
import com.vwholesale.order.support.OrderFlowStatus;
import com.vwholesale.product.entity.Product;
import com.vwholesale.product.mapper.ProductMapper;
import com.vwholesale.product.service.ProductPriceService;
import com.vwholesale.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
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
            OrderStatus.PICKING,
            OrderStatus.PICKED,
            OrderStatus.PENDING_PRICE
    );

    private static final Set<String> PRICED_ORDER_STATUSES = Set.of(
            OrderStatus.PRICED,
            OrderStatus.COMPLETED,
            OrderStatus.DELIVERING,
            OrderStatus.DELIVERED
    );

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final CustomerMapper customerMapper;
    private final ProductMapper productMapper;
    private final ProductPriceService productPriceService;
    private final MerchantContext merchantContext;
    private final OrderEventAfterCommitPublisher orderEventAfterCommitPublisher;

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
        Map<Long, Product> productMap = loadProductMap(items);
        for (OrderItem item : items) {
            PricingItemRequest priceReq = priceMap.get(item.getId());
            applyPricingFromRequest(item, productMap.get(item.getProductId()), priceReq);
            orderItemMapper.updateById(item);
            totalAmount = totalAmount.add(item.getSubtotalAmount());
        }

        order.setAmount(totalAmount.setScale(2, RoundingMode.HALF_UP));
        order.setReceivableAmount(order.getAmount());
        order.setStatus(OrderStatus.PRICED);
        orderMapper.updateById(order);

        return toDetailVO(order);
    }

    public List<PricingProductSummaryVO> listPricingProducts(String keyword, String priceFilter,
                                                             LocalDate deliveryFrom, LocalDate deliveryTo) {
        RoleChecker.requireBoss();
        PricingWorkspace workspace = loadPricingWorkspace(deliveryFrom, deliveryTo, priceFilter);
        if (workspace.items().isEmpty()) {
            return List.of();
        }

        Map<Long, List<OrderItem>> catalogGrouped = workspace.items().stream()
                .filter(item -> !OrderItemDisplay.isCustomItem(item))
                .collect(Collectors.groupingBy(OrderItem::getProductId));
        Map<String, List<OrderItem>> customGrouped = workspace.items().stream()
                .filter(OrderItemDisplay::isCustomItem)
                .collect(Collectors.groupingBy(item -> item.getOriginalText().trim()));

        List<PricingProductSummaryVO> summaries = new ArrayList<>();
        for (Map.Entry<Long, List<OrderItem>> entry : catalogGrouped.entrySet()) {
            Product product = workspace.productMap().get(entry.getKey());
            if (product == null || ProductService.isCustomOrderProduct(product)) {
                continue;
            }
            PricingProductSummaryVO summary = buildProductSummary(entry.getKey(), product.getName(), entry.getValue(),
                    keyword, priceFilter, false, null);
            if (summary != null) {
                summaries.add(summary);
            }
        }
        for (Map.Entry<String, List<OrderItem>> entry : customGrouped.entrySet()) {
            PricingProductSummaryVO summary = buildProductSummary(
                    entry.getValue().get(0).getProductId(),
                    entry.getKey(),
                    entry.getValue(),
                    keyword,
                    priceFilter,
                    true,
                    entry.getKey());
            if (summary != null) {
                summaries.add(summary);
            }
        }

        summaries.sort(Comparator
                .comparing(PricingProductSummaryVO::getPendingCount, Comparator.reverseOrder())
                .thenComparing(PricingProductSummaryVO::getProductName));
        return summaries;
    }

    private PricingProductSummaryVO buildProductSummary(Long productId, String productName, List<OrderItem> productItems,
                                                        String keyword, String priceFilter, boolean customItem,
                                                        String customName) {
        if (StringUtils.hasText(keyword) && !productName.contains(keyword.trim())) {
            return null;
        }
        int pendingCount = (int) productItems.stream().filter(item -> !isPricingComplete(item)).count();
        int totalCount = productItems.size();
        boolean allPriced = pendingCount == 0;
        if ("UNPRICED".equalsIgnoreCase(priceFilter) && allPriced) {
            return null;
        }
        if ("PRICED".equalsIgnoreCase(priceFilter) && !allPriced) {
            return null;
        }
        BigDecimal totalQty = productItems.stream()
                .map(this::itemQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        String unit = productItems.get(0).getUnit();
        return PricingProductSummaryVO.builder()
                .productId(productId)
                .customItem(customItem)
                .customName(customName)
                .productName(productName)
                .unit(unit)
                .pendingCount(pendingCount)
                .totalCount(totalCount)
                .totalQty(totalQty)
                .allPriced(allPriced)
                .build();
    }

    public PricingProductDetailVO getProductPricingDetail(Long productId, String customName, LocalDate deliveryFrom,
                                                          LocalDate deliveryTo, String priceFilter) {
        RoleChecker.requireBoss();
        PricingWorkspace workspace = loadPricingWorkspace(deliveryFrom, deliveryTo, priceFilter);
        if (StringUtils.hasText(customName)) {
            return buildCustomProductPricingDetail(customName.trim(), workspace);
        }
        return buildProductPricingDetail(productId, workspace);
    }

    public PricingProductDetailVO getProductPricingDetail(Long productId, LocalDate deliveryFrom, LocalDate deliveryTo,
                                                          String priceFilter) {
        return getProductPricingDetail(productId, null, deliveryFrom, deliveryTo, priceFilter);
    }

    public PricingProductDetailVO fetchProductReferencePrices(Long productId, String customName, LocalDate deliveryFrom,
                                                              LocalDate deliveryTo, String priceFilter) {
        RoleChecker.requireBoss();
        PricingProductDetailVO detail = getProductPricingDetail(productId, customName, deliveryFrom, deliveryTo, priceFilter);
        if (Boolean.TRUE.equals(detail.getCustomItem())) {
            return detail;
        }
        List<PricingProductLineVO> filled = detail.getLines().stream()
                .map(line -> line.getDealPrice() != null
                        ? line
                        : PricingProductLineVO.builder()
                        .itemId(line.getItemId())
                        .orderId(line.getOrderId())
                        .orderNo(line.getOrderNo())
                        .customerId(line.getCustomerId())
                        .customerName(line.getCustomerName())
                        .deliveryDate(line.getDeliveryDate())
                        .orderRemark(line.getOrderRemark())
                        .pickRemark(line.getPickRemark())
                        .quantity(line.getQuantity())
                        .unit(line.getUnit())
                        .customItem(line.getCustomItem())
                        .customName(line.getCustomName())
                        .referencePrice(line.getReferencePrice())
                        .costPrice(line.getCostPrice())
                        .dealPrice(line.getReferencePrice())
                        .priced(false)
                        .build())
                .toList();
        detail.setLines(filled);
        return detail;
    }

    public PricingProductDetailVO fetchProductReferencePrices(Long productId, LocalDate deliveryFrom,
                                                              LocalDate deliveryTo, String priceFilter) {
        return fetchProductReferencePrices(productId, null, deliveryFrom, deliveryTo, priceFilter);
    }

    @Transactional
    public PricingProductDetailVO submitProductPricing(Long productId, String customName,
                                                       PricingProductSubmitRequest request,
                                                       LocalDate deliveryFrom, LocalDate deliveryTo,
                                                       String priceFilter) {
        RoleChecker.requireBoss();
        String filter = StringUtils.hasText(priceFilter) ? priceFilter : "ALL";
        PricingWorkspace workspace = loadPricingWorkspace(deliveryFrom, deliveryTo, filter);
        List<OrderItem> productItems = filterProductItems(productId, customName, workspace);
        if (productItems.isEmpty()) {
            throw BusinessException.of(404, "该商品暂无录价明细，请刷新后重试");
        }

        Set<Long> allowedItemIds = productItems.stream().map(OrderItem::getId).collect(Collectors.toSet());
        Map<Long, Product> productMap = loadProductMap(productItems);

        Set<Long> affectedOrderIds = new HashSet<>();
        for (PricingItemRequest priceReq : request.getItems()) {
            if (priceReq.getItemId() == null || !allowedItemIds.contains(priceReq.getItemId())) {
                throw BusinessException.of(400, "存在无效的订单明细");
            }
            OrderItem item = workspace.itemMap().get(priceReq.getItemId());
            applyPricingFromRequest(item, productMap.get(item.getProductId()), priceReq);
            orderItemMapper.updateById(item);
            affectedOrderIds.add(item.getOrderId());
        }

        for (Long orderId : affectedOrderIds) {
            Order order = workspace.orderMap().get(orderId);
            if (order != null && order.getAmount() == null) {
                tryFinalizeOrder(orderId);
            } else {
                recalculateOrderAmount(orderId);
            }
        }
        if (StringUtils.hasText(customName)) {
            return buildCustomProductPricingDetail(customName.trim(),
                    loadPricingWorkspace(deliveryFrom, deliveryTo, filter));
        }
        return buildProductPricingDetail(productId, loadPricingWorkspace(deliveryFrom, deliveryTo, filter), true);
    }

    public PricingProductDetailVO submitProductPricing(Long productId, PricingProductSubmitRequest request,
                                                       LocalDate deliveryFrom, LocalDate deliveryTo,
                                                       String priceFilter) {
        return submitProductPricing(productId, null, request, deliveryFrom, deliveryTo, priceFilter);
    }

    private PricingProductDetailVO buildCustomProductPricingDetail(String customName, PricingWorkspace workspace) {
        List<OrderItem> productItems = workspace.items().stream()
                .filter(item -> OrderItemDisplay.isCustomItem(item)
                        && customName.equals(item.getOriginalText().trim()))
                .toList();
        if (productItems.isEmpty()) {
            throw BusinessException.of(404, "该代采商品暂无待录价明细，请调整配送日期后刷新");
        }
        List<PricingProductLineVO> lines = buildProductLines(productItems, workspace);
        Set<Long> orderIds = productItems.stream().map(OrderItem::getOrderId).collect(Collectors.toSet());
        BigDecimal totalQty = productItems.stream().map(this::itemQuantity).reduce(BigDecimal.ZERO, BigDecimal::add);
        return PricingProductDetailVO.builder()
                .productId(productItems.get(0).getProductId())
                .customItem(true)
                .customName(customName)
                .productName(customName)
                .unit(productItems.get(0).getUnit())
                .orderCount(orderIds.size())
                .totalQty(totalQty)
                .lines(lines)
                .build();
    }

    private List<OrderItem> filterProductItems(Long productId, String customName, PricingWorkspace workspace) {
        if (StringUtils.hasText(customName)) {
            String name = customName.trim();
            return workspace.items().stream()
                    .filter(item -> OrderItemDisplay.isCustomItem(item)
                            && name.equals(item.getOriginalText().trim())
                            && Objects.equals(item.getProductId(), productId))
                    .toList();
        }
        return workspace.items().stream()
                .filter(item -> Objects.equals(item.getProductId(), productId)
                        && !OrderItemDisplay.isCustomItem(item))
                .toList();
    }

    private void applyPricingFromRequest(OrderItem item, Product product, PricingItemRequest priceReq) {
        if (priceReq == null || priceReq.getDealPrice() == null) {
            throw BusinessException.of(400, "请为所有商品填写成交单价");
        }
        if (priceReq.getDealPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw BusinessException.of(400, "成交单价不能为负数");
        }
        if (OrderItemDisplay.isCustomItem(item)) {
            if (priceReq.getCostPrice() == null) {
                throw BusinessException.of(400, "代采商品请填写成本价");
            }
            if (priceReq.getCostPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw BusinessException.of(400, "成本价不能为负数");
            }
            item.setCostPrice(priceReq.getCostPrice());
        }
        BigDecimal qty = itemQuantity(item);
        item.setDealPrice(priceReq.getDealPrice());
        item.setSubtotalAmount(priceReq.getDealPrice().multiply(qty).setScale(2, RoundingMode.HALF_UP));
    }

    private boolean isPricingComplete(OrderItem item) {
        if (OrderItemDisplay.isCustomItem(item)) {
            return item.getDealPrice() != null && item.getCostPrice() != null;
        }
        return item.getDealPrice() != null;
    }

    private Map<Long, Product> loadProductMap(List<OrderItem> items) {
        Set<Long> productIds = items.stream().map(OrderItem::getProductId).collect(Collectors.toSet());
        if (productIds.isEmpty()) {
            return Map.of();
        }
        return productMapper.selectBatchIds(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
    }

    private PricingProductDetailVO buildProductPricingDetail(Long productId, PricingWorkspace workspace) {
        return buildProductPricingDetail(productId, workspace, false);
    }

    private PricingProductDetailVO buildProductPricingDetail(Long productId, PricingWorkspace workspace,
                                                             boolean allowEmpty) {
        List<OrderItem> productItems = workspace.items().stream()
                .filter(item -> Objects.equals(item.getProductId(), productId)
                        && !OrderItemDisplay.isCustomItem(item))
                .toList();
        if (productItems.isEmpty()) {
            Product product = productMapper.selectById(productId);
            if (product == null || !merchantContext.currentMerchantId().equals(product.getMerchantId())) {
                throw BusinessException.of(404, "商品不存在");
            }
            if (allowEmpty) {
                return PricingProductDetailVO.builder()
                        .productId(productId)
                        .productName(product.getName())
                        .unit(product.getUnit())
                        .orderCount(0)
                        .totalQty(BigDecimal.ZERO)
                        .lines(List.of())
                        .build();
            }
            throw BusinessException.of(404, "该商品暂无待录价明细，请调整配送日期后刷新");
        }

        Product product = workspace.productMap().get(productId);
        if (product == null) {
            product = productMapper.selectById(productId);
        }
        if (product == null) {
            throw BusinessException.of(404, "商品不存在");
        }

        List<PricingProductLineVO> lines = buildProductLines(productItems, workspace);
        Set<Long> orderIds = productItems.stream().map(OrderItem::getOrderId).collect(Collectors.toSet());
        BigDecimal totalQty = productItems.stream().map(this::itemQuantity).reduce(BigDecimal.ZERO, BigDecimal::add);

        return PricingProductDetailVO.builder()
                .productId(productId)
                .productName(product.getName())
                .unit(productItems.get(0).getUnit())
                .orderCount(orderIds.size())
                .totalQty(totalQty)
                .lines(lines)
                .build();
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
        orderEventAfterCommitPublisher.publishAfterCommit(
                OrderEventMessage.orderPricedPublished(order.getMerchantId(), order.getId()));
        return toDetailVO(order);
    }

    private boolean canViewPricing(Order order) {
        return PRICEABLE_STATUSES.contains(order.getStatus())
                || OrderStatus.PRICED.equals(order.getStatus())
                || OrderStatus.COMPLETED.equals(order.getStatus());
    }

    private OrderPricingVO toListVO(Order order) {
        long itemCount = orderItemMapper.selectCount(new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId()));
        return OrderPricingVO.builder()
                .id(order.getId())
                .orderNo(order.getOrderNo())
                .customerId(order.getCustomerId())
                .customerName(resolveOrderCustomerName(order, null))
                .status(order.getStatus())
                .statusLabel(OrderFlowStatus.flowStatusLabel(order, order.getAmount() == null, 0, (int) itemCount))
                .deliveryDate(order.getDeliveryDate())
                .deliveryAddressShort(order.getDeliveryAddressShort())
                .amount(order.getAmount())
                .remark(order.getRemark())
                .createdAt(order.getCreatedAt())
                .items(List.of())
                .build();
    }

    private OrderPricingVO toDetailVO(Order order) {
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
            BigDecimal refPrice = OrderItemDisplay.isCustomItem(item)
                    ? null
                    : referencePrices.get(item.getProductId());
            return OrderPricingItemVO.builder()
                    .id(item.getId())
                    .productId(item.getProductId())
                    .customItem(OrderItemDisplay.isCustomItem(item))
                    .customName(OrderItemDisplay.customName(item))
                    .productName(OrderItemDisplay.productName(item, product))
                    .orderQty(item.getOrderQty())
                    .actualQty(item.getActualQty())
                    .unit(item.getUnit())
                    .referencePrice(refPrice)
                    .costPrice(item.getCostPrice())
                    .dealPrice(item.getDealPrice() != null ? item.getDealPrice() : refPrice)
                    .subtotalAmount(item.getSubtotalAmount())
                    .shortageFlag(item.getShortageFlag())
                    .pickRemark(item.getPickRemark())
                    .build();
        }).toList();

        int customItemCount = (int) items.stream().filter(OrderItemDisplay::isCustomItem).count();
        boolean priceIncomplete = order.getAmount() == null
                || items.stream().anyMatch(item -> !isPricingComplete(item));
        int pickedItemCount = (int) items.stream()
                .filter(item -> item.getShortageFlag() != null && item.getShortageFlag() == 1
                        || item.getActualQty() != null)
                .count();

        return OrderPricingVO.builder()
                .id(order.getId())
                .orderNo(order.getOrderNo())
                .customerId(order.getCustomerId())
                .customerName(resolveOrderCustomerName(order, null))
                .status(order.getStatus())
                .statusLabel(OrderFlowStatus.flowStatusLabel(order, priceIncomplete, pickedItemCount, items.size()))
                .deliveryDate(order.getDeliveryDate())
                .deliveryAddressShort(order.getDeliveryAddressShort())
                .amount(order.getAmount())
                .remark(order.getRemark())
                .createdAt(order.getCreatedAt())
                .customItemCount(customItemCount)
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

    private void tryFinalizeOrder(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || order.getAmount() != null) {
            return;
        }
        if (!PRICEABLE_STATUSES.contains(order.getStatus())) {
            return;
        }
        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, orderId));
        if (items.isEmpty() || items.stream().anyMatch(item -> !isPricingComplete(item))) {
            return;
        }
        applyOrderAmount(order, items);
        order.setStatus(OrderStatus.PRICED);
        orderMapper.updateById(order);
    }

    private void recalculateOrderAmount(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || order.getAmount() == null) {
            return;
        }
        if (!PRICEABLE_STATUSES.contains(order.getStatus())
                && !PRICED_ORDER_STATUSES.contains(order.getStatus())) {
            return;
        }
        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, orderId));
        if (items.isEmpty() || items.stream().anyMatch(item -> !isPricingComplete(item))) {
            return;
        }
        applyOrderAmount(order, items);
        orderMapper.updateById(order);
    }

    private void applyOrderAmount(Order order, List<OrderItem> items) {
        BigDecimal totalAmount = items.stream()
                .map(item -> item.getSubtotalAmount() != null
                        ? item.getSubtotalAmount()
                        : item.getDealPrice().multiply(itemQuantity(item)).setScale(2, RoundingMode.HALF_UP))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setAmount(totalAmount.setScale(2, RoundingMode.HALF_UP));
        order.setReceivableAmount(order.getAmount());
    }

    private PricingWorkspace loadPricingWorkspace(LocalDate deliveryFrom, LocalDate deliveryTo, String priceFilter) {
        List<Order> orders = listPricingOrders(deliveryFrom, deliveryTo, priceFilter);
        if (orders.isEmpty()) {
            return new PricingWorkspace(List.of(), Map.of(), Map.of(), List.of(), Map.of(), Map.of());
        }
        Map<Long, Order> orderMap = orders.stream().collect(Collectors.toMap(Order::getId, Function.identity()));
        List<Long> orderIds = orders.stream().map(Order::getId).toList();
        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .in(OrderItem::getOrderId, orderIds)
                .orderByAsc(OrderItem::getId));
        Map<Long, OrderItem> itemMap = items.stream().collect(Collectors.toMap(OrderItem::getId, Function.identity()));

        Set<Long> productIds = items.stream().map(OrderItem::getProductId).collect(Collectors.toSet());
        Map<Long, Product> productMap = productIds.isEmpty()
                ? Map.of()
                : productMapper.selectBatchIds(productIds).stream().collect(Collectors.toMap(Product::getId, p -> p));

        Set<Long> customerIds = orders.stream().map(Order::getCustomerId).collect(Collectors.toSet());
        Map<Long, Customer> customerMap = customerIds.isEmpty()
                ? Map.of()
                : customerMapper.selectBatchIds(customerIds).stream().collect(Collectors.toMap(Customer::getId, c -> c));

        return new PricingWorkspace(orders, orderMap, itemMap, items, productMap, customerMap);
    }

    private List<Order> listPricingOrders(LocalDate deliveryFrom, LocalDate deliveryTo, String priceFilter) {
        if ("UNPRICED".equalsIgnoreCase(priceFilter)) {
            return listPendingPriceOrders(deliveryFrom, deliveryTo);
        }
        if ("PRICED".equalsIgnoreCase(priceFilter)) {
            List<Order> pending = listPendingPriceOrders(deliveryFrom, deliveryTo);
            List<Order> priced = listPricedOrders(deliveryFrom, deliveryTo);
            return mergeOrders(pending, priced);
        }
        List<Order> pending = listPendingPriceOrders(deliveryFrom, deliveryTo);
        List<Order> priced = listPricedOrders(deliveryFrom, deliveryTo);
        return mergeOrders(pending, priced);
    }

    private List<Order> mergeOrders(List<Order> first, List<Order> second) {
        Map<Long, Order> merged = new java.util.LinkedHashMap<>();
        for (Order order : first) {
            merged.put(order.getId(), order);
        }
        for (Order order : second) {
            merged.put(order.getId(), order);
        }
        return new ArrayList<>(merged.values());
    }

    private List<Order> listPricedOrders(LocalDate deliveryFrom, LocalDate deliveryTo) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .eq(Order::getMerchantId, merchantContext.currentMerchantId())
                .in(Order::getStatus, PRICED_ORDER_STATUSES)
                .isNotNull(Order::getAmount)
                .orderByDesc(Order::getId);
        if (deliveryFrom != null) {
            wrapper.ge(Order::getDeliveryDate, deliveryFrom);
        }
        if (deliveryTo != null) {
            wrapper.le(Order::getDeliveryDate, deliveryTo);
        }
        return orderMapper.selectList(wrapper);
    }

    private List<Order> listPendingPriceOrders(LocalDate deliveryFrom, LocalDate deliveryTo) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .eq(Order::getMerchantId, merchantContext.currentMerchantId())
                .in(Order::getStatus, PRICEABLE_STATUSES)
                .isNull(Order::getAmount)
                .orderByDesc(Order::getId);
        if (deliveryFrom != null) {
            wrapper.ge(Order::getDeliveryDate, deliveryFrom);
        }
        if (deliveryTo != null) {
            wrapper.le(Order::getDeliveryDate, deliveryTo);
        }
        return orderMapper.selectList(wrapper);
    }

    private List<PricingProductLineVO> buildProductLines(List<OrderItem> productItems, PricingWorkspace workspace) {
        Map<Long, BigDecimal> referencePriceCache = new HashMap<>();
        List<PricingProductLineVO> lines = new ArrayList<>();
        for (OrderItem item : productItems) {
            Order order = workspace.orderMap().get(item.getOrderId());
            if (order == null) {
                continue;
            }
            Product product = workspace.productMap().get(item.getProductId());
            BigDecimal referencePrice = OrderItemDisplay.isCustomItem(item)
                    ? null
                    : resolveReferencePrice(order, item, product, referencePriceCache);
            lines.add(PricingProductLineVO.builder()
                    .itemId(item.getId())
                    .orderId(order.getId())
                    .orderNo(order.getOrderNo())
                    .customerId(order.getCustomerId())
                    .customerName(resolveOrderCustomerName(order, workspace.customerMap()))
                    .deliveryDate(order.getDeliveryDate())
                    .orderRemark(order.getRemark())
                    .pickRemark(item.getPickRemark())
                    .quantity(itemQuantity(item))
                    .unit(item.getUnit())
                    .customItem(OrderItemDisplay.isCustomItem(item))
                    .customName(OrderItemDisplay.customName(item))
                    .referencePrice(referencePrice)
                    .costPrice(item.getCostPrice())
                    .dealPrice(item.getDealPrice())
                    .priced(isPricingComplete(item))
                    .build());
        }
        return lines;
    }

    private BigDecimal resolveReferencePrice(Order order, OrderItem item, Product product,
                                             Map<Long, BigDecimal> cache) {
        if (item.getProductId() == null) {
            return BigDecimal.ZERO;
        }
        if (cache.containsKey(item.getId())) {
            return cache.get(item.getId());
        }
        LocalDate priceDate = order.getDeliveryDate() != null ? order.getDeliveryDate() : LocalDate.now();
        BigDecimal defaultPrice = product != null && product.getDefaultPrice() != null
                ? product.getDefaultPrice()
                : BigDecimal.ZERO;
        Map<Long, BigDecimal> refPrices = productPriceService.batchResolveReferencePrices(
                List.of(item.getProductId()), order.getCustomerId(), Map.of(item.getProductId(), defaultPrice), priceDate);
        BigDecimal price = refPrices.getOrDefault(item.getProductId(), defaultPrice);
        cache.put(item.getId(), price);
        return price;
    }

    private BigDecimal itemQuantity(OrderItem item) {
        if (item.getShortageFlag() != null && item.getShortageFlag() == 1) {
            return item.getActualQty() != null ? item.getActualQty() : BigDecimal.ZERO;
        }
        return item.getActualQty() != null ? item.getActualQty() : item.getOrderQty();
    }

    private String resolveOrderCustomerName(Order order, Map<Long, Customer> customerMap) {
        if (order.getCustomerId() != null) {
            Customer customer = customerMap != null ? customerMap.get(order.getCustomerId()) : null;
            if (customer == null) {
                customer = customerMapper.selectById(order.getCustomerId());
            }
            if (customer != null) {
                return customer.getName();
            }
        }
        return order.getGuestCustomerName();
    }

    private record PricingWorkspace(
            List<Order> orders,
            Map<Long, Order> orderMap,
            Map<Long, OrderItem> itemMap,
            List<OrderItem> items,
            Map<Long, Product> productMap,
            Map<Long, Customer> customerMap
    ) {
    }
}
