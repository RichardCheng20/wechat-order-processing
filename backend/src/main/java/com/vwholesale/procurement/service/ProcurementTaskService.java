package com.vwholesale.procurement.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.vwholesale.common.context.MerchantContext;
import com.vwholesale.common.enums.OrderStatus;
import com.vwholesale.common.exception.BusinessException;
import com.vwholesale.common.security.RoleChecker;
import com.vwholesale.customer.entity.Customer;
import com.vwholesale.customer.mapper.CustomerMapper;
import com.vwholesale.order.entity.Order;
import com.vwholesale.order.entity.OrderItem;
import com.vwholesale.order.mapper.OrderItemMapper;
import com.vwholesale.order.mapper.OrderMapper;
import com.vwholesale.procurement.dto.ProcurementCustomerLineVO;
import com.vwholesale.procurement.dto.ProcurementProductDetailVO;
import com.vwholesale.procurement.dto.ProcurementPurchasePriceSubmitRequest;
import com.vwholesale.procurement.dto.ProcurementStockUpdateRequest;
import com.vwholesale.procurement.dto.ProcurementTaskItemVO;
import com.vwholesale.procurement.dto.ProcurementTaskVO;
import com.vwholesale.product.entity.Product;
import com.vwholesale.product.entity.ProductCategory;
import com.vwholesale.product.entity.ProductPurchasePriceRecord;
import com.vwholesale.product.mapper.ProductCategoryMapper;
import com.vwholesale.product.mapper.ProductMapper;
import com.vwholesale.product.service.ProductPurchasePriceService;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProcurementTaskService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductMapper productMapper;
    private final ProductCategoryMapper productCategoryMapper;
    private final CustomerMapper customerMapper;
    private final ProductPurchasePriceService productPurchasePriceService;
    private final MerchantContext merchantContext;

    public ProcurementTaskVO listTasks(LocalDate receiveDate, String keyword, Long categoryId) {
        RoleChecker.requireBoss();
        LocalDate date = receiveDate != null ? receiveDate : LocalDate.now();
        ProcurementWorkspace workspace = loadWorkspace(date);
        Map<Long, ProductCategory> categoryMap = workspace.categoryMap();

        Map<Long, List<OrderItem>> grouped = workspace.items().stream()
                .collect(Collectors.groupingBy(OrderItem::getProductId));

        Map<Long, Product> productsToShow = loadCatalogProducts(keyword, categoryId, categoryMap);
        for (Map.Entry<Long, List<OrderItem>> entry : grouped.entrySet()) {
            if (productsToShow.containsKey(entry.getKey())) {
                continue;
            }
            Product product = workspace.productMap().get(entry.getKey());
            if (product == null) {
                product = productMapper.selectById(entry.getKey());
            }
            if (product == null || !matchesProductFilter(product, keyword, categoryId, categoryMap)) {
                continue;
            }
            productsToShow.put(product.getId(), product);
        }

        if (productsToShow.isEmpty()) {
            return emptyTask(date);
        }

        List<Long> productIds = new ArrayList<>(productsToShow.keySet());
        Map<Long, BigDecimal> defaultPrices = productsToShow.values().stream()
                .collect(Collectors.toMap(Product::getId,
                        p -> p.getDefaultPurchasePrice() != null ? p.getDefaultPurchasePrice() : BigDecimal.ZERO));
        Map<Long, BigDecimal> purchasePrices = productPurchasePriceService.batchResolvePurchasePrices(
                productIds, date, defaultPrices);

        List<ProcurementTaskItemVO> taskItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalNeedQty = BigDecimal.ZERO;

        for (Product product : productsToShow.values()) {
            List<OrderItem> productItems = grouped.getOrDefault(product.getId(), List.of());
            BigDecimal demandQty = productItems.stream()
                    .map(this::itemQuantity)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal stockQty = product.getStockQty() != null ? product.getStockQty() : BigDecimal.ZERO;
            BigDecimal needQty = demandQty.subtract(stockQty).max(BigDecimal.ZERO);
            BigDecimal purchasePrice = purchasePrices.get(product.getId());
            boolean priced = purchasePrice != null && purchasePrice.compareTo(BigDecimal.ZERO) > 0;
            BigDecimal priceForAmount = purchasePrice != null ? purchasePrice : BigDecimal.ZERO;
            BigDecimal lineAmount = priceForAmount.multiply(needQty).setScale(2, RoundingMode.HALF_UP);
            totalAmount = totalAmount.add(lineAmount);
            totalNeedQty = totalNeedQty.add(needQty);

            Set<Long> relatedOrderIds = new HashSet<>();
            Set<Long> relatedCustomerIds = new HashSet<>();
            for (OrderItem item : productItems) {
                relatedOrderIds.add(item.getOrderId());
                Order order = workspace.orderMap().get(item.getOrderId());
                if (order != null && order.getCustomerId() != null) {
                    relatedCustomerIds.add(order.getCustomerId());
                }
            }

            taskItems.add(ProcurementTaskItemVO.builder()
                    .productId(product.getId())
                    .productName(product.getName())
                    .categoryId(product.getCategoryId())
                    .unit(resolveUnit(productItems, product))
                    .demandQty(demandQty)
                    .stockQty(stockQty)
                    .needQty(needQty)
                    .purchasePrice(priced ? purchasePrice : null)
                    .totalAmount(lineAmount)
                    .orderCount(relatedOrderIds.size())
                    .customerCount(relatedCustomerIds.size())
                    .priced(priced)
                    .build());
        }

        taskItems.sort(Comparator
                .comparing((ProcurementTaskItemVO item) -> item.getDemandQty().compareTo(BigDecimal.ZERO) > 0 ? 0 : 1)
                .thenComparing(ProcurementTaskItemVO::getNeedQty, Comparator.reverseOrder())
                .thenComparing(ProcurementTaskItemVO::getProductName));

        return ProcurementTaskVO.builder()
                .receiveDate(date)
                .totalNeedQty(totalNeedQty.setScale(2, RoundingMode.HALF_UP))
                .totalAmount(totalAmount.setScale(2, RoundingMode.HALF_UP))
                .productCount(taskItems.size())
                .items(taskItems)
                .build();
    }

    public ProcurementProductDetailVO getProductDetail(Long productId, LocalDate receiveDate) {
        RoleChecker.requireBoss();
        LocalDate date = receiveDate != null ? receiveDate : LocalDate.now();
        Product product = getProductOrThrow(productId);
        ProcurementWorkspace workspace = loadWorkspace(date);

        List<OrderItem> productItems = workspace.items().stream()
                .filter(item -> Objects.equals(item.getProductId(), productId))
                .toList();

        BigDecimal demandQty = productItems.stream()
                .map(this::itemQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal stockQty = product.getStockQty() != null ? product.getStockQty() : BigDecimal.ZERO;
        BigDecimal needQty = demandQty.subtract(stockQty).max(BigDecimal.ZERO);
        BigDecimal defaultPrice = product.getDefaultPurchasePrice() != null
                ? product.getDefaultPurchasePrice() : BigDecimal.ZERO;
        BigDecimal purchasePrice = productPurchasePriceService.resolvePurchasePrice(productId, defaultPrice, date);
        boolean priced = purchasePrice != null && purchasePrice.compareTo(BigDecimal.ZERO) > 0;
        ProductPurchasePriceRecord purchaseRecord = productPurchasePriceService.findRecord(productId, date);
        BigDecimal purchasedQtyToday = purchaseRecord != null && purchaseRecord.getPurchasedQty() != null
                ? purchaseRecord.getPurchasedQty() : BigDecimal.ZERO;

        List<ProcurementCustomerLineVO> customerLines = buildCustomerLines(productItems, workspace);

        return ProcurementProductDetailVO.builder()
                .productId(productId)
                .productName(product.getName())
                .unit(resolveUnit(productItems, product))
                .receiveDate(date)
                .demandQty(demandQty)
                .stockQty(stockQty)
                .needQty(needQty)
                .purchasePrice(priced ? purchasePrice : null)
                .purchasedQtyToday(purchasedQtyToday.compareTo(BigDecimal.ZERO) > 0 ? purchasedQtyToday : null)
                .referencePurchasePrice(defaultPrice.compareTo(BigDecimal.ZERO) > 0 ? defaultPrice : null)
                .priced(priced)
                .customerLines(customerLines)
                .build();
    }

    public ProcurementProductDetailVO fetchReferencePurchasePrice(Long productId, LocalDate receiveDate) {
        ProcurementProductDetailVO detail = getProductDetail(productId, receiveDate);
        BigDecimal ref = detail.getReferencePurchasePrice();
        if (ref == null && detail.getPurchasePrice() != null) {
            ref = detail.getPurchasePrice();
        }
        detail.setPurchasePrice(ref);
        return detail;
    }

    @Transactional
    public ProcurementProductDetailVO submitPurchasePrice(Long productId, LocalDate receiveDate,
                                                          ProcurementPurchasePriceSubmitRequest request) {
        RoleChecker.requireBoss();
        LocalDate date = receiveDate != null ? receiveDate : LocalDate.now();
        Product product = getProductOrThrow(productId);

        ProductPurchasePriceRecord existingRecord = productPurchasePriceService.findRecord(productId, date);
        BigDecimal oldPurchasedQty = existingRecord != null && existingRecord.getPurchasedQty() != null
                ? existingRecord.getPurchasedQty() : BigDecimal.ZERO;
        BigDecimal newPurchasedQty = request.getPurchasedQty() != null
                ? request.getPurchasedQty() : oldPurchasedQty;

        productPurchasePriceService.upsertPurchasePrice(productId, request.getPurchasePrice(), newPurchasedQty, date);

        if (request.getPurchasedQty() != null) {
            BigDecimal delta = newPurchasedQty.subtract(oldPurchasedQty);
            if (delta.compareTo(BigDecimal.ZERO) != 0) {
                BigDecimal currentStock = product.getStockQty() != null ? product.getStockQty() : BigDecimal.ZERO;
                product.setStockQty(currentStock.add(delta).max(BigDecimal.ZERO));
                productMapper.updateById(product);
            }
        }

        product.setDefaultPurchasePrice(request.getPurchasePrice());
        productMapper.updateById(product);

        return getProductDetail(productId, date);
    }

    @Transactional
    public ProcurementProductDetailVO updateStock(Long productId, LocalDate receiveDate,
                                                  ProcurementStockUpdateRequest request) {
        RoleChecker.requireBoss();
        Product product = getProductOrThrow(productId);
        product.setStockQty(request.getStockQty());
        productMapper.updateById(product);
        LocalDate date = receiveDate != null ? receiveDate : LocalDate.now();
        try {
            return getProductDetail(productId, date);
        } catch (BusinessException ex) {
            return ProcurementProductDetailVO.builder()
                    .productId(productId)
                    .productName(product.getName())
                    .unit(product.getUnit())
                    .receiveDate(date)
                    .stockQty(request.getStockQty())
                    .demandQty(BigDecimal.ZERO)
                    .needQty(BigDecimal.ZERO)
                    .priced(false)
                    .customerLines(List.of())
                    .build();
        }
    }

    private List<ProcurementCustomerLineVO> buildCustomerLines(List<OrderItem> productItems,
                                                              ProcurementWorkspace workspace) {
        Map<Long, List<OrderItem>> byCustomer = new HashMap<>();
        Map<Long, String> guestNames = new HashMap<>();

        for (OrderItem item : productItems) {
            Order order = workspace.orderMap().get(item.getOrderId());
            if (order == null) {
                continue;
            }
            Long customerKey = order.getCustomerId() != null ? order.getCustomerId() : -order.getId();
            byCustomer.computeIfAbsent(customerKey, k -> new ArrayList<>()).add(item);
            if (order.getCustomerId() == null) {
                guestNames.put(customerKey, order.getGuestCustomerName());
            }
        }

        List<ProcurementCustomerLineVO> lines = new ArrayList<>();
        for (Map.Entry<Long, List<OrderItem>> entry : byCustomer.entrySet()) {
            Long customerKey = entry.getKey();
            List<OrderItem> items = entry.getValue();
            BigDecimal qty = items.stream().map(this::itemQuantity).reduce(BigDecimal.ZERO, BigDecimal::add);
            Set<Long> orderIds = items.stream().map(OrderItem::getOrderId).collect(Collectors.toSet());

            String customerName;
            Long customerId = null;
            if (customerKey > 0) {
                customerId = customerKey;
                Customer customer = workspace.customerMap().get(customerId);
                if (customer == null) {
                    customer = customerMapper.selectById(customerId);
                }
                customerName = customer != null ? customer.getName() : "未知客户";
            } else {
                customerName = guestNames.getOrDefault(customerKey, "散客");
            }

            lines.add(ProcurementCustomerLineVO.builder()
                    .customerId(customerId)
                    .customerName(customerName)
                    .totalQty(qty)
                    .orderCount(orderIds.size())
                    .build());
        }

        lines.sort(Comparator.comparing(ProcurementCustomerLineVO::getCustomerName));
        return lines;
    }

    private ProcurementWorkspace loadWorkspace(LocalDate date) {
        Long merchantId = merchantContext.currentMerchantId();
        Map<Long, ProductCategory> categoryMap = loadCategoryMap();
        List<Order> orders = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .eq(Order::getMerchantId, merchantId)
                .eq(Order::getDeliveryDate, date)
                .ne(Order::getStatus, OrderStatus.CANCELLED)
                .orderByDesc(Order::getId));
        if (orders.isEmpty()) {
            return new ProcurementWorkspace(Map.of(), List.of(), Map.of(), Map.of(), categoryMap);
        }

        Map<Long, Order> orderMap = orders.stream().collect(Collectors.toMap(Order::getId, o -> o));
        List<Long> orderIds = orders.stream().map(Order::getId).toList();
        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .in(OrderItem::getOrderId, orderIds)
                .orderByAsc(OrderItem::getId));

        Set<Long> productIds = items.stream().map(OrderItem::getProductId).collect(Collectors.toSet());
        Map<Long, Product> productMap = productIds.isEmpty()
                ? Map.of()
                : productMapper.selectBatchIds(productIds).stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        Set<Long> customerIds = orders.stream().map(Order::getCustomerId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Customer> customerMap = customerIds.isEmpty()
                ? Map.of()
                : customerMapper.selectBatchIds(customerIds).stream()
                .collect(Collectors.toMap(Customer::getId, c -> c));

        return new ProcurementWorkspace(orderMap, items, productMap, customerMap, categoryMap);
    }

    private Map<Long, ProductCategory> loadCategoryMap() {
        List<ProductCategory> categories = productCategoryMapper.selectList(
                new LambdaQueryWrapper<ProductCategory>()
                        .eq(ProductCategory::getMerchantId, merchantContext.currentMerchantId()));
        return categories.stream().collect(Collectors.toMap(ProductCategory::getId, c -> c));
    }

    private Map<Long, Product> loadCatalogProducts(String keyword, Long categoryId,
                                                   Map<Long, ProductCategory> categoryMap) {
        List<Product> products = productMapper.selectList(new LambdaQueryWrapper<Product>()
                .eq(Product::getMerchantId, merchantContext.currentMerchantId())
                .eq(Product::getSaleStatus, "ON")
                .orderByAsc(Product::getName));
        Map<Long, Product> result = new java.util.LinkedHashMap<>();
        for (Product product : products) {
            if (!matchesProductFilter(product, keyword, categoryId, categoryMap)) {
                continue;
            }
            result.put(product.getId(), product);
        }
        return result;
    }

    private boolean matchesProductFilter(Product product, String keyword, Long categoryId,
                                         Map<Long, ProductCategory> categoryMap) {
        if (!matchCategory(product.getCategoryId(), categoryId, categoryMap)) {
            return false;
        }
        return !StringUtils.hasText(keyword) || product.getName().contains(keyword.trim());
    }

    private boolean matchCategory(Long productCategoryId, Long filterCategoryId,
                                  Map<Long, ProductCategory> categoryMap) {
        if (filterCategoryId == null) {
            return true;
        }
        if (productCategoryId == null) {
            return false;
        }
        if (Objects.equals(productCategoryId, filterCategoryId)) {
            return true;
        }
        ProductCategory category = categoryMap.get(productCategoryId);
        if (category != null && category.getParentId() != null) {
            return Objects.equals(category.getParentId(), filterCategoryId);
        }
        ProductCategory filter = categoryMap.get(filterCategoryId);
        if (filter != null && filter.getParentId() == null) {
            return false;
        }
        return false;
    }

    private Product getProductOrThrow(Long productId) {
        Product product = productMapper.selectOne(new LambdaQueryWrapper<Product>()
                .eq(Product::getId, productId)
                .eq(Product::getMerchantId, merchantContext.currentMerchantId()));
        if (product == null) {
            throw BusinessException.of(404, "商品不存在");
        }
        return product;
    }

    private ProcurementTaskVO emptyTask(LocalDate date) {
        return ProcurementTaskVO.builder()
                .receiveDate(date)
                .totalNeedQty(BigDecimal.ZERO)
                .totalAmount(BigDecimal.ZERO)
                .productCount(0)
                .items(List.of())
                .build();
    }

    private String resolveUnit(List<OrderItem> productItems, Product product) {
        if (!productItems.isEmpty() && productItems.get(0).getUnit() != null) {
            return productItems.get(0).getUnit();
        }
        return product.getUnit();
    }

    private BigDecimal itemQuantity(OrderItem item) {
        if (item.getShortageFlag() != null && item.getShortageFlag() == 1) {
            return item.getActualQty() != null ? item.getActualQty() : BigDecimal.ZERO;
        }
        return item.getActualQty() != null ? item.getActualQty() : item.getOrderQty();
    }

    private record ProcurementWorkspace(
            Map<Long, Order> orderMap,
            List<OrderItem> items,
            Map<Long, Product> productMap,
            Map<Long, Customer> customerMap,
            Map<Long, ProductCategory> categoryMap
    ) {
    }
}
