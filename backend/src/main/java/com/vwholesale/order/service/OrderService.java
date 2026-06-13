package com.vwholesale.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.vwholesale.common.context.MerchantContext;
import com.vwholesale.common.enums.OrderStatus;
import com.vwholesale.common.exception.BusinessException;
import com.vwholesale.common.security.RoleChecker;
import com.vwholesale.customer.entity.Customer;
import com.vwholesale.customer.mapper.CustomerMapper;
import com.vwholesale.order.dto.OrderCreateRequest;
import com.vwholesale.order.dto.OrderItemCreateRequest;
import com.vwholesale.order.dto.OrderItemVO;
import com.vwholesale.order.dto.OrderVO;
import com.vwholesale.order.entity.Order;
import com.vwholesale.order.entity.OrderItem;
import com.vwholesale.order.mapper.OrderItemMapper;
import com.vwholesale.order.mapper.OrderMapper;
import com.vwholesale.product.entity.Product;
import com.vwholesale.product.mapper.ProductMapper;
import com.vwholesale.worker.entity.Worker;
import com.vwholesale.worker.mapper.WorkerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final DateTimeFormatter ORDER_NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final CustomerMapper customerMapper;
    private final ProductMapper productMapper;
    private final WorkerMapper workerMapper;
    private final MerchantContext merchantContext;

    @Transactional
    public OrderVO createByCustomer(OrderCreateRequest request) {
        RoleChecker.requireCustomer();
        Long customerId = requireBoundCustomerId();
        Customer customer = getCustomerOrThrow(customerId);
        if (customer.getStatus() != null && customer.getStatus() == 0) {
            throw BusinessException.of(400, "客户档案已停用，无法下单");
        }

        List<OrderItemCreateRequest> items = request.getItems();
        Map<Long, Product> productMap = loadProducts(items);

        String initialStatus = customer.getAutoConfirmOrder() != null && customer.getAutoConfirmOrder() == 1
                ? OrderStatus.PENDING_PICK
                : OrderStatus.PENDING_CONFIRM;

        Order order = new Order();
        order.setMerchantId(merchantContext.currentMerchantId());
        order.setOrderNo(generateOrderNo());
        order.setCustomerId(customerId);
        order.setSource("CUSTOMER_APP");
        order.setStatus(initialStatus);
        order.setDeliveryDate(LocalDate.now());
        order.setDeliveryAddress(customer.getAddress());
        order.setDeliveryAddressShort(customer.getAddressShort());
        order.setContactName(customer.getContactName());
        order.setContactPhone(customer.getPhone());
        order.setRemark(request.getRemark());
        order.setCreatedBy(RoleChecker.currentUserId());
        orderMapper.insert(order);

        for (OrderItemCreateRequest itemReq : items) {
            Product product = productMap.get(itemReq.getProductId());
            OrderItem item = new OrderItem();
            item.setOrderId(order.getId());
            item.setProductId(product.getId());
            item.setOrderQty(itemReq.getOrderQty());
            item.setUnit(product.getUnit());
            item.setShortageFlag(0);
            orderItemMapper.insert(item);
        }

        return getDetail(order.getId());
    }

    public List<OrderVO> listForCustomer() {
        RoleChecker.requireCustomer();
        Long customerId = requireBoundCustomerId();
        return listOrders(new LambdaQueryWrapper<Order>()
                .eq(Order::getMerchantId, merchantContext.currentMerchantId())
                .eq(Order::getCustomerId, customerId)
                .orderByDesc(Order::getId));
    }

    public List<OrderVO> listForBoss(String status) {
        RoleChecker.requireBoss();
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .eq(Order::getMerchantId, merchantContext.currentMerchantId())
                .orderByDesc(Order::getId);
        if (StringUtils.hasText(status)) {
            wrapper.eq(Order::getStatus, status.trim());
        }
        return listOrders(wrapper);
    }

    public OrderVO getDetail(Long id) {
        Order order = getOrderOrThrow(id);
        assertReadable(order);
        return toDetailVO(order);
    }

    @Transactional
    public OrderVO confirmByBoss(Long id) {
        RoleChecker.requireBoss();
        Order order = getOrderOrThrow(id);
        if (!OrderStatus.PENDING_CONFIRM.equals(order.getStatus())) {
            throw BusinessException.of(400, "只有待确认订单可以确认");
        }
        order.setStatus(OrderStatus.PENDING_PICK);
        orderMapper.updateById(order);
        return getDetail(id);
    }

    public Map<String, Long> bossSummary() {
        RoleChecker.requireBoss();
        Long merchantId = merchantContext.currentMerchantId();
        LocalDate today = LocalDate.now();
        List<Order> todayOrders = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .eq(Order::getMerchantId, merchantId)
                .eq(Order::getDeliveryDate, today));

        Map<String, Long> summary = new HashMap<>();
        summary.put("todayTotal", (long) todayOrders.size());
        summary.put("pendingConfirm", todayOrders.stream().filter(o -> OrderStatus.PENDING_CONFIRM.equals(o.getStatus())).count());
        summary.put("pendingPick", todayOrders.stream().filter(o -> OrderStatus.PENDING_PICK.equals(o.getStatus())).count());
        summary.put("pendingPrice", todayOrders.stream().filter(o -> OrderStatus.PENDING_PRICE.equals(o.getStatus())).count());
        return summary;
    }

    private List<OrderVO> listOrders(LambdaQueryWrapper<Order> wrapper) {
        List<Order> orders = orderMapper.selectList(wrapper);
        if (orders.isEmpty()) {
            return List.of();
        }
        Set<Long> customerIds = orders.stream().map(Order::getCustomerId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> customerNames = customerMapper.selectBatchIds(customerIds).stream()
                .collect(Collectors.toMap(Customer::getId, Customer::getName));

        List<Long> orderIds = orders.stream().map(Order::getId).toList();
        Map<Long, Long> itemCounts = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>().in(OrderItem::getOrderId, orderIds))
                .stream()
                .collect(Collectors.groupingBy(OrderItem::getOrderId, Collectors.counting()));

        Set<Long> workerIds = orders.stream().map(Order::getAssignedWorkerId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> workerNames = workerIds.isEmpty()
                ? Map.of()
                : workerMapper.selectBatchIds(workerIds).stream().collect(Collectors.toMap(Worker::getId, Worker::getName));

        return orders.stream()
                .map(order -> toListVO(order, customerNames.get(order.getCustomerId()),
                        itemCounts.getOrDefault(order.getId(), 0L).intValue(),
                        order.getAssignedWorkerId() != null
                                ? workerNames.get(order.getAssignedWorkerId())
                                : null))
                .toList();
    }

    private OrderVO toDetailVO(Order order) {
        Customer customer = customerMapper.selectById(order.getCustomerId());
        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, order.getId())
                .orderByAsc(OrderItem::getId));
        Set<Long> productIds = items.stream().map(OrderItem::getProductId).collect(Collectors.toSet());
        Map<Long, Product> productMap = productIds.isEmpty()
                ? Map.of()
                : productMapper.selectBatchIds(productIds).stream().collect(Collectors.toMap(Product::getId, p -> p));

        List<OrderItemVO> itemVOs = items.stream().map(item -> OrderItemVO.builder()
                .id(item.getId())
                .productId(item.getProductId())
                .productName(productMap.get(item.getProductId()) != null ? productMap.get(item.getProductId()).getName() : "未知商品")
                .orderQty(item.getOrderQty())
                .actualQty(item.getActualQty())
                .unit(item.getUnit())
                .dealPrice(item.getDealPrice())
                .subtotalAmount(item.getSubtotalAmount())
                .shortageFlag(item.getShortageFlag())
                .pickRemark(item.getPickRemark())
                .build()).toList();

        return OrderVO.builder()
                .id(order.getId())
                .orderNo(order.getOrderNo())
                .customerId(order.getCustomerId())
                .customerName(customer != null ? customer.getName() : null)
                .source(order.getSource())
                .status(order.getStatus())
                .statusLabel(statusLabel(order.getStatus()))
                .deliveryDate(order.getDeliveryDate())
                .deliveryAddress(order.getDeliveryAddress())
                .deliveryAddressShort(order.getDeliveryAddressShort())
                .contactName(order.getContactName())
                .contactPhone(order.getContactPhone())
                .amount(order.getAmount())
                .remark(order.getRemark())
                .createdAt(order.getCreatedAt())
                .items(itemVOs)
                .itemCount(itemVOs.size())
                .build();
    }

    private OrderVO toListVO(Order order, String customerName, int itemCount, String workerName) {
        return OrderVO.builder()
                .id(order.getId())
                .orderNo(order.getOrderNo())
                .customerId(order.getCustomerId())
                .customerName(customerName)
                .source(order.getSource())
                .status(order.getStatus())
                .statusLabel(statusLabel(order.getStatus()))
                .deliveryDate(order.getDeliveryDate())
                .deliveryAddressShort(order.getDeliveryAddressShort())
                .contactName(order.getContactName())
                .amount(order.getAmount())
                .remark(order.getRemark())
                .createdAt(order.getCreatedAt())
                .itemCount(itemCount)
                .assignedWorkerId(order.getAssignedWorkerId())
                .assignedWorkerName(workerName)
                .build();
    }

    private String statusLabel(String status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case OrderStatus.PENDING_CONFIRM -> "待确认";
            case OrderStatus.PENDING_PICK -> "待分拣";
            case OrderStatus.PICKING -> "分拣中";
            case OrderStatus.PICKED -> "已拣完";
            case OrderStatus.DELIVERING -> "配送中";
            case OrderStatus.DELIVERED -> "已送达";
            case OrderStatus.PENDING_PRICE -> "待录价";
            case OrderStatus.COMPLETED -> "已完成";
            default -> status;
        };
    }

    private Map<Long, Product> loadProducts(List<OrderItemCreateRequest> items) {
        Set<Long> productIds = items.stream().map(OrderItemCreateRequest::getProductId).collect(Collectors.toSet());
        List<Product> products = productMapper.selectList(new LambdaQueryWrapper<Product>()
                .in(Product::getId, productIds)
                .eq(Product::getMerchantId, merchantContext.currentMerchantId())
                .eq(Product::getSaleStatus, "ON"));
        Map<Long, Product> productMap = products.stream().collect(Collectors.toMap(Product::getId, p -> p));

        List<String> missing = new ArrayList<>();
        for (OrderItemCreateRequest item : items) {
            if (!productMap.containsKey(item.getProductId())) {
                missing.add(String.valueOf(item.getProductId()));
            }
        }
        if (!missing.isEmpty()) {
            throw BusinessException.of(400, "以下商品不可下单或已下架: " + String.join(",", missing));
        }
        return productMap;
    }

    private Long requireBoundCustomerId() {
        Long customerId = RoleChecker.currentCustomerId();
        if (customerId == null) {
            throw BusinessException.of(403, "请先绑定客户档案后再下单");
        }
        return customerId;
    }

    private Customer getCustomerOrThrow(Long customerId) {
        Customer customer = customerMapper.selectOne(new LambdaQueryWrapper<Customer>()
                .eq(Customer::getId, customerId)
                .eq(Customer::getMerchantId, merchantContext.currentMerchantId()));
        if (customer == null) {
            throw BusinessException.of(404, "客户档案不存在");
        }
        return customer;
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

    private void assertReadable(Order order) {
        if (RoleChecker.currentCustomerId() != null) {
            RoleChecker.requireCustomer();
            if (!Objects.equals(order.getCustomerId(), RoleChecker.currentCustomerId())) {
                throw BusinessException.of(403, "无权查看该订单");
            }
            return;
        }
        RoleChecker.requireBoss();
    }

    private String generateOrderNo() {
        return "O" + LocalDateTime.now().format(ORDER_NO_FMT) + String.format("%03d", RANDOM.nextInt(1000));
    }
}
