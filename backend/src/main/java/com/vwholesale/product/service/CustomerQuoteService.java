package com.vwholesale.product.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.vwholesale.common.context.MerchantContext;
import com.vwholesale.common.exception.BusinessException;
import com.vwholesale.common.security.RoleChecker;
import com.vwholesale.customer.entity.Customer;
import com.vwholesale.customer.mapper.CustomerMapper;
import com.vwholesale.order.entity.Order;
import com.vwholesale.order.entity.OrderItem;
import com.vwholesale.order.mapper.OrderItemMapper;
import com.vwholesale.order.mapper.OrderMapper;
import com.vwholesale.product.dto.CategoryVO;
import com.vwholesale.product.dto.CustomerQuoteDetailVO;
import com.vwholesale.product.dto.CustomerQuoteLineVO;
import com.vwholesale.product.dto.CustomerQuoteSaveRequest;
import com.vwholesale.product.dto.CustomerQuoteSummaryVO;
import com.vwholesale.product.entity.Product;
import com.vwholesale.product.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerQuoteService {

    private final CustomerMapper customerMapper;
    private final ProductMapper productMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductCategoryService categoryService;
    private final ProductPriceService productPriceService;
    private final MerchantContext merchantContext;

    public List<CustomerQuoteSummaryVO> listSummaries(String keyword) {
        RoleChecker.requireBoss();
        Long merchantId = merchantContext.currentMerchantId();
        LambdaQueryWrapper<Customer> wrapper = new LambdaQueryWrapper<Customer>()
                .eq(Customer::getMerchantId, merchantId)
                .eq(Customer::getStatus, 1)
                .orderByDesc(Customer::getId);
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(Customer::getName, kw)
                    .or().like(Customer::getContactName, kw)
                    .or().like(Customer::getPhone, kw));
        }
        Map<Long, Integer> quoteCounts = productPriceService.countQuotedProductsByCustomer(merchantId);
        return customerMapper.selectList(wrapper).stream()
                .map(customer -> CustomerQuoteSummaryVO.builder()
                        .customerId(customer.getId())
                        .customerName(customer.getName())
                        .quotedProductCount(quoteCounts.getOrDefault(customer.getId(), 0))
                        .build())
                .toList();
    }

    public CustomerQuoteDetailVO getQuote(Long customerId, String keyword, Long categoryId, Boolean onlyQuoted) {
        RoleChecker.requireBoss();
        Customer customer = getCustomerOrThrow(customerId);
        Long merchantId = merchantContext.currentMerchantId();
        LocalDate today = LocalDate.now();

        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .eq(Product::getMerchantId, merchantId)
                .eq(Product::getSaleStatus, "ON")
                .orderByAsc(Product::getCategoryId)
                .orderByAsc(Product::getId);
        if (categoryId != null) {
            wrapper.eq(Product::getCategoryId, categoryId);
        }
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(Product::getName, kw).or().like(Product::getAliases, kw));
        }
        List<Product> products = productMapper.selectList(wrapper);
        if (products.isEmpty()) {
            return CustomerQuoteDetailVO.builder()
                    .customerId(customer.getId())
                    .customerName(customer.getName())
                    .lines(List.of())
                    .build();
        }

        List<Long> productIds = products.stream().map(Product::getId).toList();
        Map<Long, BigDecimal> customerPrices = productPriceService.batchLatestCustomerPrices(customerId, productIds, today);
        Map<Long, BigDecimal> defaultPrices = products.stream()
                .collect(Collectors.toMap(Product::getId,
                        p -> p.getDefaultPrice() != null ? p.getDefaultPrice() : BigDecimal.ZERO));
        Map<Long, BigDecimal> basePrices = productPriceService.batchResolveReferencePrices(
                productIds, null, defaultPrices, today);
        Map<Long, CategoryVO> categoryMap = categoryService.listEnabled().stream()
                .flatMap(c -> flattenCategories(c).stream())
                .collect(Collectors.toMap(CategoryVO::getId, c -> c, (a, b) -> a));

        List<CustomerQuoteLineVO> lines = new ArrayList<>();
        for (Product product : products) {
            BigDecimal customerPrice = customerPrices.get(product.getId());
            boolean hasQuote = customerPrice != null;
            if (Boolean.TRUE.equals(onlyQuoted) && !hasQuote) {
                continue;
            }
            CategoryVO category = categoryMap.get(product.getCategoryId());
            lines.add(CustomerQuoteLineVO.builder()
                    .productId(product.getId())
                    .productName(product.getName())
                    .unit(product.getUnit())
                    .categoryId(product.getCategoryId())
                    .categoryName(category != null ? category.getName() : null)
                    .basePrice(basePrices.get(product.getId()))
                    .customerPrice(customerPrice)
                    .hasQuote(hasQuote)
                    .build());
        }

        return CustomerQuoteDetailVO.builder()
                .customerId(customer.getId())
                .customerName(customer.getName())
                .lines(lines)
                .build();
    }

    @Transactional
    public CustomerQuoteDetailVO saveQuote(Long customerId, CustomerQuoteSaveRequest request) {
        RoleChecker.requireBoss();
        getCustomerOrThrow(customerId);
        LocalDate today = LocalDate.now();
        for (CustomerQuoteSaveRequest.Item item : request.getItems()) {
            if (item.getProductId() == null) {
                continue;
            }
            if (item.getPrice() == null || item.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                productPriceService.removeCustomerPrice(customerId, item.getProductId());
            } else {
                productPriceService.upsertCustomerPrice(customerId, item.getProductId(), item.getPrice(), today);
            }
        }
        return getQuote(customerId, null, null, false);
    }

    @Transactional
    public int syncFromOrder(Long orderId) {
        RoleChecker.requireBoss();
        Order order = orderMapper.selectById(orderId);
        if (order == null || !order.getMerchantId().equals(merchantContext.currentMerchantId())) {
            throw BusinessException.of(404, "订单不存在");
        }
        if (order.getCustomerId() == null) {
            throw BusinessException.of(400, "散客订单无法同步报价单");
        }
        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, orderId));
        LocalDate today = LocalDate.now();
        int count = 0;
        for (OrderItem item : items) {
            if (item.getProductId() == null || item.getDealPrice() == null
                    || item.getDealPrice().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            productPriceService.upsertCustomerPrice(
                    order.getCustomerId(), item.getProductId(), item.getDealPrice(), today);
            count++;
        }
        if (count == 0) {
            throw BusinessException.of(400, "订单暂无已录价格");
        }
        return count;
    }

    private Customer getCustomerOrThrow(Long customerId) {
        Customer customer = customerMapper.selectById(customerId);
        if (customer == null || !customer.getMerchantId().equals(merchantContext.currentMerchantId())) {
            throw BusinessException.of(404, "客户不存在");
        }
        return customer;
    }

    private List<CategoryVO> flattenCategories(CategoryVO category) {
        List<CategoryVO> result = new ArrayList<>();
        result.add(category);
        if (category.getChildren() != null) {
            for (CategoryVO child : category.getChildren()) {
                result.addAll(flattenCategories(child));
            }
        }
        return result;
    }
}
