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
import com.vwholesale.order.support.OrderItemDisplay;
import com.vwholesale.procurement.dto.ProcurementCustomerLineVO;
import com.vwholesale.procurement.dto.ProcurementProductDetailVO;
import com.vwholesale.procurement.dto.ProcurementPurchasePriceSubmitRequest;
import com.vwholesale.procurement.dto.ProcurementStockUpdateRequest;
import com.vwholesale.procurement.dto.ProcurementSupplierOptionVO;
import com.vwholesale.procurement.dto.ProcurementSupplierOrderLineVO;
import com.vwholesale.procurement.dto.ProcurementTaskItemVO;
import com.vwholesale.procurement.dto.ProcurementTaskVO;
import com.vwholesale.product.entity.Product;
import com.vwholesale.product.entity.ProductCategory;
import com.vwholesale.product.entity.ProductPurchasePriceRecord;
import com.vwholesale.product.mapper.ProductCategoryMapper;
import com.vwholesale.product.mapper.ProductMapper;
import com.vwholesale.product.service.ProductPurchasePriceService;
import com.vwholesale.product.service.ProductService;
import com.vwholesale.product.support.ProductStockSupport;
import com.vwholesale.supplier.entity.Supplier;
import com.vwholesale.supplier.mapper.SupplierMapper;
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
    private final SupplierMapper supplierMapper;
    private final SupplierPurchaseLineService supplierPurchaseLineService;
    private final MerchantContext merchantContext;

    public ProcurementTaskVO listTasks(LocalDate receiveDate, String keyword, Long categoryId) {
        RoleChecker.requireBoss();
        LocalDate date = receiveDate != null ? receiveDate : LocalDate.now();
        ProcurementWorkspace workspace = loadWorkspace(date);
        Map<Long, ProductCategory> categoryMap = workspace.categoryMap();

        List<OrderItem> catalogItems = workspace.items().stream()
                .filter(item -> !OrderItemDisplay.isCustomItem(item))
                .toList();
        List<OrderItem> customItems = workspace.items().stream()
                .filter(OrderItemDisplay::isCustomItem)
                .toList();

        Map<Long, List<OrderItem>> catalogGrouped = catalogItems.stream()
                .collect(Collectors.groupingBy(OrderItem::getProductId));

        Map<Long, Product> productsToShow = loadCatalogProducts(keyword, categoryId, categoryMap);
        for (Map.Entry<Long, List<OrderItem>> entry : catalogGrouped.entrySet()) {
            if (productsToShow.containsKey(entry.getKey())) {
                continue;
            }
            Product product = workspace.productMap().get(entry.getKey());
            if (product == null) {
                product = productMapper.selectById(entry.getKey());
            }
            if (product == null || ProductService.isCustomOrderProduct(product)
                    || !matchesProductFilter(product, keyword, categoryId, categoryMap)) {
                continue;
            }
            productsToShow.put(product.getId(), product);
        }

        List<ProcurementTaskItemVO> taskItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalNeedQty = BigDecimal.ZERO;

        if (!productsToShow.isEmpty()) {
            List<Long> productIds = new ArrayList<>(productsToShow.keySet());
            Map<Long, BigDecimal> defaultPrices = productsToShow.values().stream()
                    .collect(Collectors.toMap(Product::getId,
                            p -> p.getDefaultPurchasePrice() != null ? p.getDefaultPurchasePrice() : BigDecimal.ZERO));
            Map<Long, BigDecimal> purchasePrices = productPurchasePriceService.batchResolvePurchasePrices(
                    productIds, date, defaultPrices);

            for (Product product : productsToShow.values()) {
                List<OrderItem> productItems = catalogGrouped.getOrDefault(product.getId(), List.of());
                ProcurementTaskItemVO taskItem = buildCatalogTaskItem(product, productItems, workspace,
                        purchasePrices.get(product.getId()), date);
                taskItems.add(taskItem);
                totalAmount = totalAmount.add(taskItem.getTotalAmount() != null ? taskItem.getTotalAmount() : BigDecimal.ZERO);
                totalNeedQty = totalNeedQty.add(taskItem.getNeedQty());
            }
        }

        if (categoryId == null && !customItems.isEmpty()) {
            Map<String, List<OrderItem>> customGrouped = customItems.stream()
                    .collect(Collectors.groupingBy(item -> item.getOriginalText().trim()));
            for (Map.Entry<String, List<OrderItem>> entry : customGrouped.entrySet()) {
                String customName = entry.getKey();
                if (StringUtils.hasText(keyword) && !customName.contains(keyword.trim())) {
                    continue;
                }
                ProcurementTaskItemVO taskItem = buildCustomTaskItem(customName, entry.getValue(), workspace);
                taskItems.add(taskItem);
                totalAmount = totalAmount.add(taskItem.getTotalAmount() != null ? taskItem.getTotalAmount() : BigDecimal.ZERO);
                totalNeedQty = totalNeedQty.add(taskItem.getNeedQty());
            }
        }

        if (taskItems.isEmpty()) {
            return emptyTask(date);
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

    private ProcurementTaskItemVO buildCatalogTaskItem(Product product, List<OrderItem> productItems,
                                                         ProcurementWorkspace workspace, BigDecimal purchasePrice,
                                                         LocalDate date) {
        BigDecimal demandQty = productItems.stream()
                .map(this::itemQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal physicalStock = ProductStockSupport.physicalStock(product);
        BigDecimal reservedQty = ProductStockSupport.reservedQty(product);
        BigDecimal availableStock = ProductStockSupport.availableStock(product);
        BigDecimal needQty = demandQty.subtract(physicalStock).max(BigDecimal.ZERO);
        boolean priced = purchasePrice != null && purchasePrice.compareTo(BigDecimal.ZERO) > 0;
        BigDecimal priceForAmount = purchasePrice != null ? purchasePrice : BigDecimal.ZERO;
        BigDecimal lineAmount = priceForAmount.multiply(needQty).setScale(2, RoundingMode.HALF_UP);

        Set<Long> relatedOrderIds = new HashSet<>();
        Set<Long> relatedCustomerIds = new HashSet<>();
        for (OrderItem item : productItems) {
            relatedOrderIds.add(item.getOrderId());
            Order order = workspace.orderMap().get(item.getOrderId());
            if (order != null && order.getCustomerId() != null) {
                relatedCustomerIds.add(order.getCustomerId());
            }
        }

        return ProcurementTaskItemVO.builder()
                .productId(product.getId())
                .customItem(false)
                .productName(product.getName())
                .categoryId(product.getCategoryId())
                .unit(resolveUnit(productItems, product))
                .demandQty(demandQty)
                .stockQty(availableStock)
                .physicalStockQty(physicalStock)
                .reservedQty(reservedQty)
                .needQty(needQty)
                .purchasePrice(priced ? purchasePrice : null)
                .totalAmount(lineAmount)
                .orderCount(relatedOrderIds.size())
                .customerCount(relatedCustomerIds.size())
                .priced(priced)
                .build();
    }

    private ProcurementTaskItemVO buildCustomTaskItem(String customName, List<OrderItem> productItems,
                                                      ProcurementWorkspace workspace) {
        BigDecimal demandQty = productItems.stream()
                .map(this::itemQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal needQty = demandQty;
        long pricedLines = productItems.stream().filter(item -> item.getCostPrice() != null).count();
        boolean priced = pricedLines == productItems.size() && !productItems.isEmpty();
        BigDecimal purchasePrice = productItems.stream()
                .map(OrderItem::getCostPrice)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
        BigDecimal priceForAmount = purchasePrice != null ? purchasePrice : BigDecimal.ZERO;
        BigDecimal lineAmount = priceForAmount.multiply(needQty).setScale(2, RoundingMode.HALF_UP);

        Set<Long> relatedOrderIds = new HashSet<>();
        Set<Long> relatedCustomerIds = new HashSet<>();
        for (OrderItem item : productItems) {
            relatedOrderIds.add(item.getOrderId());
            Order order = workspace.orderMap().get(item.getOrderId());
            if (order != null && order.getCustomerId() != null) {
                relatedCustomerIds.add(order.getCustomerId());
            }
        }

        OrderItem first = productItems.get(0);
        return ProcurementTaskItemVO.builder()
                .productId(first.getProductId())
                .customItem(true)
                .customName(customName)
                .productName(customName)
                .categoryId(null)
                .unit(resolveUnit(productItems, workspace.productMap().get(first.getProductId())))
                .demandQty(demandQty)
                .stockQty(BigDecimal.ZERO)
                .needQty(needQty)
                .purchasePrice(priced ? purchasePrice : null)
                .totalAmount(lineAmount)
                .orderCount(relatedOrderIds.size())
                .customerCount(relatedCustomerIds.size())
                .priced(priced)
                .build();
    }

    public ProcurementProductDetailVO getCustomItemDetail(String customName, LocalDate receiveDate) {
        RoleChecker.requireBoss();
        if (!StringUtils.hasText(customName)) {
            throw BusinessException.of(400, "请指定代采商品名称");
        }
        LocalDate date = receiveDate != null ? receiveDate : LocalDate.now();
        ProcurementWorkspace workspace = loadWorkspace(date);
        String name = customName.trim();
        List<OrderItem> productItems = workspace.items().stream()
                .filter(item -> OrderItemDisplay.isCustomItem(item) && name.equals(item.getOriginalText().trim()))
                .toList();
        if (productItems.isEmpty()) {
            throw BusinessException.of(404, "该代采商品暂无采购记录");
        }

        BigDecimal demandQty = productItems.stream().map(this::itemQuantity).reduce(BigDecimal.ZERO, BigDecimal::add);
        long pricedLines = productItems.stream().filter(item -> item.getCostPrice() != null).count();
        boolean priced = pricedLines == productItems.size();
        BigDecimal purchasePrice = productItems.stream()
                .map(OrderItem::getCostPrice)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
        List<ProcurementCustomerLineVO> customerLines = buildCustomerLines(productItems, workspace);
        OrderItem first = productItems.get(0);
        Product placeholder = workspace.productMap().get(first.getProductId());

        return ProcurementProductDetailVO.builder()
                .productId(first.getProductId())
                .customItem(true)
                .customName(name)
                .productName(name)
                .unit(resolveUnit(productItems, placeholder))
                .receiveDate(date)
                .demandQty(demandQty)
                .stockQty(BigDecimal.ZERO)
                .needQty(demandQty)
                .purchasePrice(priced ? purchasePrice : null)
                .priced(priced)
                .recordedAtPricing(true)
                .customerLines(customerLines)
                .build();
    }

    public ProcurementProductDetailVO getProductDetail(Long productId, LocalDate receiveDate) {
        RoleChecker.requireBoss();
        LocalDate date = receiveDate != null ? receiveDate : LocalDate.now();
        Product product = getProductOrThrow(productId);
        if (ProductService.isCustomOrderProduct(product)) {
            throw BusinessException.of(400, "请从采购列表选择具体代采商品");
        }
        ProcurementWorkspace workspace = loadWorkspace(date);

        List<OrderItem> productItems = workspace.items().stream()
                .filter(item -> Objects.equals(item.getProductId(), productId)
                        && !OrderItemDisplay.isCustomItem(item))
                .toList();

        BigDecimal demandQty = productItems.stream()
                .map(this::itemQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal physicalStock = ProductStockSupport.physicalStock(product);
        BigDecimal reservedQty = ProductStockSupport.reservedQty(product);
        BigDecimal availableStock = ProductStockSupport.availableStock(product);
        BigDecimal needQty = demandQty.subtract(physicalStock).max(BigDecimal.ZERO);
        BigDecimal defaultPrice = product.getDefaultPurchasePrice() != null
                ? product.getDefaultPurchasePrice() : BigDecimal.ZERO;
        BigDecimal purchasePrice = productPurchasePriceService.resolvePurchasePrice(productId, defaultPrice, date);
        boolean priced = purchasePrice != null && purchasePrice.compareTo(BigDecimal.ZERO) > 0;

        List<ProcurementCustomerLineVO> customerLines = buildCustomerLines(productItems, workspace);
        List<ProcurementSupplierOrderLineVO> supplierOrders =
                supplierPurchaseLineService.listForProduct(productId, date);
        BigDecimal totalPurchasedFromSuppliers = supplierOrders.stream()
                .map(ProcurementSupplierOrderLineVO::getPurchasedQty)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return enrichWithSupplier(ProcurementProductDetailVO.builder()
                .productId(productId)
                .productName(product.getName())
                .unit(resolveUnit(productItems, product))
                .receiveDate(date)
                .demandQty(demandQty)
                .stockQty(availableStock)
                .physicalStockQty(physicalStock)
                .reservedQty(reservedQty)
                .needQty(needQty)
                .purchasePrice(priced ? purchasePrice : null)
                .purchasedQtyToday(totalPurchasedFromSuppliers.compareTo(BigDecimal.ZERO) > 0
                        ? totalPurchasedFromSuppliers : null)
                .supplierOrders(supplierOrders)
                .referencePurchasePrice(defaultPrice.compareTo(BigDecimal.ZERO) > 0 ? defaultPrice : null)
                .priced(priced)
                .customerLines(customerLines)
                .build(), product);
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
        getProductOrThrow(productId);

        supplierPurchaseLineService.upsertLine(
                productId,
                request.getSupplierId(),
                date,
                request.getPurchasePrice(),
                request.getPurchasedQty());

        return getProductDetail(productId, date);
    }

    @Transactional
    public ProcurementProductDetailVO deleteSupplierOrder(Long productId, Long lineId, LocalDate receiveDate) {
        RoleChecker.requireBoss();
        LocalDate date = receiveDate != null ? receiveDate : LocalDate.now();
        supplierPurchaseLineService.deleteLine(lineId);
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
            Product refreshed = getProductOrThrow(productId);
            return enrichWithSupplier(ProcurementProductDetailVO.builder()
                    .productId(productId)
                    .productName(refreshed.getName())
                    .unit(refreshed.getUnit())
                    .receiveDate(date)
                    .physicalStockQty(request.getStockQty())
                    .reservedQty(ProductStockSupport.reservedQty(refreshed))
                    .stockQty(ProductStockSupport.availableStock(refreshed))
                    .demandQty(BigDecimal.ZERO)
                    .needQty(BigDecimal.ZERO)
                    .priced(false)
                    .customerLines(List.of())
                    .build(), refreshed);
        }
    }

    private ProcurementProductDetailVO enrichWithSupplier(ProcurementProductDetailVO detail, Product product) {
        List<ProcurementSupplierOptionVO> options = supplierMapper.selectList(
                        new LambdaQueryWrapper<Supplier>()
                                .eq(Supplier::getMerchantId, merchantContext.currentMerchantId())
                                .eq(Supplier::getStatus, 1)
                                .orderByAsc(Supplier::getName))
                .stream()
                .map(s -> ProcurementSupplierOptionVO.builder()
                        .id(s.getId())
                        .supplierNo(s.getSupplierNo())
                        .name(s.getName())
                        .build())
                .toList();

        detail.setSupplierOptions(options);
        if (detail.getSupplierOrders() != null && !detail.getSupplierOrders().isEmpty()) {
            ProcurementSupplierOrderLineVO first = detail.getSupplierOrders().get(0);
            detail.setSupplierId(first.getSupplierId());
            detail.setSupplierName(first.getSupplierName());
            detail.setSupplierNo(first.getSupplierNo());
        }
        return detail;
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
            if (ProductService.isCustomOrderProduct(product)) {
                continue;
            }
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
        if (product != null && product.getUnit() != null) {
            return product.getUnit();
        }
        return "斤";
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
