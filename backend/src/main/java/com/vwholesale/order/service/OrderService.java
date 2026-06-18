package com.vwholesale.order.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.vwholesale.common.context.MerchantContext;
import com.vwholesale.common.enums.OrderStatus;
import com.vwholesale.common.enums.UserRole;
import com.vwholesale.common.exception.BusinessException;
import com.vwholesale.common.security.RoleChecker;
import com.vwholesale.customer.entity.Customer;
import com.vwholesale.customer.mapper.CustomerMapper;
import com.vwholesale.order.dto.BossDashboardVO;
import com.vwholesale.order.dto.BossOrderCreateRequest;
import com.vwholesale.order.dto.BossOrderItemCreateRequest;
import com.vwholesale.order.dto.BossOrderUpdateRequest;
import com.vwholesale.order.dto.OrderCreateRequest;
import com.vwholesale.order.dto.OrderItemCreateRequest;
import com.vwholesale.order.dto.OrderItemVO;
import com.vwholesale.order.dto.OrderMarkPaymentRequest;
import com.vwholesale.order.dto.OrderVO;
import com.vwholesale.order.entity.Order;
import com.vwholesale.order.entity.OrderItem;
import com.vwholesale.order.mapper.OrderItemMapper;
import com.vwholesale.order.mapper.OrderMapper;
import com.vwholesale.payment.entity.Payment;
import com.vwholesale.payment.mapper.PaymentMapper;
import com.vwholesale.product.entity.Product;
import com.vwholesale.product.mapper.ProductMapper;
import com.vwholesale.product.service.ProductService;
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
    private final PaymentMapper paymentMapper;
    private final MerchantContext merchantContext;

    @Transactional
    public OrderVO createByCustomer(OrderCreateRequest request) {
        RoleChecker.requireCustomer();
        Long customerId = RoleChecker.currentCustomerId();

        List<OrderItemCreateRequest> items = request.getItems();
        Map<Long, Product> productMap = loadProducts(items);

        Order order = new Order();
        order.setMerchantId(merchantContext.currentMerchantId());
        order.setOrderNo(generateOrderNo());
        order.setSource("CUSTOMER_APP");
        order.setDeliveryDate(LocalDate.now());
        order.setRemark(request.getRemark());
        order.setCreatedBy(RoleChecker.currentUserId());

        if (customerId != null) {
            Customer customer = getCustomerOrThrow(customerId);
            if (customer.getStatus() != null && customer.getStatus() == 0) {
                throw BusinessException.of(400, "客户档案已停用，无法下单");
            }
            String initialStatus = customer.getAutoConfirmOrder() != null && customer.getAutoConfirmOrder() == 1
                    ? OrderStatus.PENDING_PRICE
                    : OrderStatus.PENDING_CONFIRM;
            order.setCustomerId(customerId);
            order.setStatus(initialStatus);
            order.setDeliveryAddress(customer.getAddress());
            order.setDeliveryAddressShort(customer.getAddressShort());
            order.setContactName(customer.getContactName());
            order.setContactPhone(customer.getPhone());
        } else {
            String guestName = resolveGuestCustomerName(request.getCustomerName());
            order.setCustomerId(null);
            order.setGuestCustomerName(guestName);
            order.setStatus(OrderStatus.PENDING_CONFIRM);
            order.setContactName(guestName);
            order.setDeliveryAddressShort(guestName);
        }

        orderMapper.insert(order);

        for (OrderItemCreateRequest itemReq : items) {
            Product product = productMap.get(itemReq.getProductId());
            OrderItem item = new OrderItem();
            item.setOrderId(order.getId());
            item.setProductId(product.getId());
            item.setOrderQty(itemReq.getOrderQty());
            item.setUnit(resolveCustomerItemUnit(product, itemReq.getUnit()));
            item.setShortageFlag(0);
            orderItemMapper.insert(item);
        }

        return getDetail(order.getId());
    }

    private String resolveCustomerItemUnit(Product product, String requestedUnit) {
        List<String> allowed = ProductService.parseSaleUnits(product.getSaleUnits(), product.getUnit());
        if (StringUtils.hasText(requestedUnit)) {
            String unit = requestedUnit.trim();
            if (allowed.contains(unit)) {
                return unit;
            }
        }
        return allowed.isEmpty() ? product.getUnit() : allowed.get(0);
    }

    @Transactional
    public OrderVO createByBoss(BossOrderCreateRequest request) {
        RoleChecker.requireBoss();
        validateBossOrderCustomer(request);

        List<BossOrderItemCreateRequest> items = request.getItems();
        Map<Long, Product> productMap = loadBossProducts(items);

        Order order = new Order();
        order.setMerchantId(merchantContext.currentMerchantId());
        order.setOrderNo(generateOrderNo());
        order.setSource("BOSS_MANUAL");
        order.setDeliveryDate(request.getDeliveryDate() != null ? request.getDeliveryDate() : LocalDate.now());
        order.setRemark(request.getRemark());
        order.setCreatedBy(RoleChecker.currentUserId());

        if (request.getCustomerId() != null) {
            Customer customer = getCustomerOrThrow(request.getCustomerId());
            if (customer.getStatus() != null && customer.getStatus() == 0) {
                throw BusinessException.of(400, "客户档案已停用，无法开单");
            }
            String initialStatus = customer.getAutoConfirmOrder() != null && customer.getAutoConfirmOrder() == 1
                    ? OrderStatus.PENDING_PRICE
                    : OrderStatus.PENDING_CONFIRM;
            order.setCustomerId(customer.getId());
            order.setStatus(initialStatus);
            order.setDeliveryAddress(customer.getAddress());
            order.setDeliveryAddressShort(customer.getAddressShort());
            order.setContactName(customer.getContactName());
            order.setContactPhone(customer.getPhone());
        } else {
            String guestName = request.getCustomerName().trim();
            order.setCustomerId(null);
            order.setGuestCustomerName(guestName);
            order.setStatus(OrderStatus.PENDING_CONFIRM);
            order.setContactName(guestName);
            order.setDeliveryAddressShort(guestName);
        }

        orderMapper.insert(order);

        for (BossOrderItemCreateRequest itemReq : items) {
            Product product = productMap.get(itemReq.getProductId());
            OrderItem item = new OrderItem();
            item.setOrderId(order.getId());
            item.setProductId(product.getId());
            item.setOrderQty(itemReq.getOrderQty());
            item.setUnit(StringUtils.hasText(itemReq.getUnit()) ? itemReq.getUnit().trim() : product.getUnit());
            item.setPickRemark(itemReq.getPickRemark());
            if (itemReq.getDealPrice() != null) {
                item.setDealPrice(itemReq.getDealPrice());
            }
            item.setShortageFlag(0);
            orderItemMapper.insert(item);
        }

        return getDetail(order.getId());
    }

    private static final Set<String> BOSS_EDITABLE_STATUSES = Set.of(
            OrderStatus.PENDING_CONFIRM,
            OrderStatus.PENDING_PICK,
            OrderStatus.PICKING,
            OrderStatus.PICKED,
            OrderStatus.PENDING_PRICE,
            OrderStatus.PRICED
    );

    @Transactional
    public OrderVO updateByBoss(Long id, BossOrderUpdateRequest request) {
        RoleChecker.requireBoss();
        Order order = getOrderOrThrow(id);
        if (OrderStatus.CANCELLED.equals(order.getStatus())) {
            throw BusinessException.of(400, "已取消订单不可修改");
        }
        if (OrderStatus.COMPLETED.equals(order.getStatus())) {
            throw BusinessException.of(400, "已完成订单不可修改");
        }
        if (!BOSS_EDITABLE_STATUSES.contains(order.getStatus())) {
            throw BusinessException.of(400, "当前状态不可修改订单明细");
        }

        String previousStatus = order.getStatus();
        List<BossOrderItemCreateRequest> items = request.getItems();
        Map<Long, Product> productMap = loadProductsForBossUpdate(items);

        if (order.getCustomerId() == null) {
            if (StringUtils.hasText(request.getCustomerName())) {
                String guestName = resolveGuestCustomerName(request.getCustomerName());
                order.setGuestCustomerName(guestName);
                order.setContactName(guestName);
                order.setDeliveryAddressShort(guestName);
            } else if (!StringUtils.hasText(order.getGuestCustomerName())) {
                throw BusinessException.of(400, "请输入临时客户名称");
            }
        }

        if (request.getDeliveryDate() != null) {
            order.setDeliveryDate(request.getDeliveryDate());
        }
        if (request.getRemark() != null) {
            order.setRemark(request.getRemark());
        }

        orderItemMapper.delete(new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId()));

        for (BossOrderItemCreateRequest itemReq : items) {
            Product product = productMap.get(itemReq.getProductId());
            OrderItem item = new OrderItem();
            item.setOrderId(order.getId());
            item.setProductId(product.getId());
            item.setOrderQty(itemReq.getOrderQty());
            item.setUnit(StringUtils.hasText(itemReq.getUnit()) ? itemReq.getUnit().trim() : product.getUnit());
            item.setPickRemark(itemReq.getPickRemark());
            item.setShortageFlag(0);
            orderItemMapper.insert(item);
        }

        String nextStatus = order.getStatus();
        if (OrderStatus.PRICED.equals(previousStatus)
                || OrderStatus.PICKED.equals(previousStatus)
                || OrderStatus.PENDING_PRICE.equals(previousStatus)
                || OrderStatus.PICKING.equals(previousStatus)) {
            nextStatus = OrderStatus.PENDING_PICK;
        }

        LambdaUpdateWrapper<Order> updateWrapper = new LambdaUpdateWrapper<Order>()
                .eq(Order::getId, order.getId())
                .set(Order::getAmount, null)
                .set(Order::getReceivableAmount, null)
                .set(Order::getStatus, nextStatus);
        if (request.getDeliveryDate() != null) {
            updateWrapper.set(Order::getDeliveryDate, request.getDeliveryDate());
        }
        if (request.getRemark() != null) {
            updateWrapper.set(Order::getRemark, request.getRemark());
        }
        if (order.getCustomerId() == null && StringUtils.hasText(order.getGuestCustomerName())) {
            updateWrapper
                    .set(Order::getGuestCustomerName, order.getGuestCustomerName())
                    .set(Order::getContactName, order.getContactName())
                    .set(Order::getDeliveryAddressShort, order.getDeliveryAddressShort());
        }
        orderMapper.update(null, updateWrapper);
        return getDetail(order.getId());
    }

    public List<OrderVO> listForCustomer() {
        RoleChecker.requireCustomer();
        Long customerId = RoleChecker.currentCustomerId();
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .eq(Order::getMerchantId, merchantContext.currentMerchantId())
                .ne(Order::getStatus, OrderStatus.CANCELLED)
                .orderByDesc(Order::getId);
        if (customerId != null) {
            wrapper.eq(Order::getCustomerId, customerId);
        } else {
            wrapper.isNull(Order::getCustomerId)
                    .eq(Order::getCreatedBy, RoleChecker.currentUserId());
        }
        return listOrders(wrapper)
                .stream()
                .map(this::maskPriceForCustomer)
                .toList();
    }

    public List<OrderVO> listForBoss(String status, Boolean pricingPending, String keyword,
                                     String pickFilter, String dateType,
                                     LocalDate dateFrom, LocalDate dateTo,
                                     LocalDate deliveryFrom, LocalDate deliveryTo,
                                     Long customerId, String paymentFilter) {
        RoleChecker.requireBoss();
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .eq(Order::getMerchantId, merchantContext.currentMerchantId())
                .ne(Order::getStatus, OrderStatus.CANCELLED)
                .orderByDesc(Order::getId);
        if (customerId != null) {
            wrapper.eq(Order::getCustomerId, customerId);
        }
        if (Boolean.TRUE.equals(pricingPending)) {
            wrapper.in(Order::getStatus, OrderStatus.PENDING_CONFIRM, OrderStatus.PENDING_PICK, OrderStatus.PENDING_PRICE)
                    .isNull(Order::getAmount);
        } else if (StringUtils.hasText(status)) {
            wrapper.eq(Order::getStatus, status.trim());
        }
        applyBossDateFilter(wrapper, dateType, dateFrom, dateTo, deliveryFrom, deliveryTo);
        if (StringUtils.hasText(keyword)) {
            List<Long> customerIds = customerMapper.selectList(new LambdaQueryWrapper<Customer>()
                            .eq(Customer::getMerchantId, merchantContext.currentMerchantId())
                            .like(Customer::getName, keyword.trim()))
                    .stream()
                    .map(Customer::getId)
                    .toList();
            String kw = keyword.trim();
            if (customerIds.isEmpty()) {
                wrapper.like(Order::getGuestCustomerName, kw);
            } else {
                wrapper.and(w -> w.in(Order::getCustomerId, customerIds)
                        .or()
                        .like(Order::getGuestCustomerName, kw));
            }
        }
        List<OrderVO> orders = listOrders(wrapper);
        if (StringUtils.hasText(paymentFilter) && !"ALL".equalsIgnoreCase(paymentFilter.trim())) {
            orders = orders.stream()
                    .filter(vo -> matchesPaymentFilter(vo, paymentFilter.trim()))
                    .toList();
        }
        if (!StringUtils.hasText(pickFilter) || "ALL".equalsIgnoreCase(pickFilter)) {
            return orders;
        }
        return orders.stream()
                .filter(vo -> matchesPickFilter(vo, pickFilter))
                .toList();
    }

    private boolean matchesPaymentFilter(OrderVO vo, String paymentFilter) {
        if (vo.getAmount() == null) {
            return false;
        }
        BigDecimal receivable = vo.getReceivableAmount() != null ? vo.getReceivableAmount() : vo.getAmount();
        BigDecimal paid = vo.getPaidAmount() != null ? vo.getPaidAmount() : BigDecimal.ZERO;
        BigDecimal outstanding = receivable.subtract(paid);
        String filter = paymentFilter.toUpperCase();
        return switch (filter) {
            case "UNPAID" -> outstanding.compareTo(BigDecimal.ZERO) > 0;
            case "PENDING" -> outstanding.compareTo(BigDecimal.ZERO) > 0
                    && paid.compareTo(BigDecimal.ZERO) <= 0;
            case "PARTIAL" -> paid.compareTo(BigDecimal.ZERO) > 0
                    && outstanding.compareTo(BigDecimal.ZERO) > 0;
            case "PAID", "SETTLED" -> outstanding.compareTo(BigDecimal.ZERO) <= 0;
            default -> true;
        };
    }

    private boolean matchesPickFilter(OrderVO vo, String pickFilter) {
        int total = vo.getItemCount() != null ? vo.getItemCount() : 0;
        int picked = vo.getPickedItemCount() != null ? vo.getPickedItemCount() : 0;
        if ("PICKED".equalsIgnoreCase(pickFilter)) {
            return total > 0 && picked >= total;
        }
        if ("UNPICKED".equalsIgnoreCase(pickFilter)) {
            return total == 0 || picked < total;
        }
        return true;
    }

    private void applyBossDateFilter(LambdaQueryWrapper<Order> wrapper, String dateType,
                                     LocalDate dateFrom, LocalDate dateTo,
                                     LocalDate deliveryFrom, LocalDate deliveryTo) {
        LocalDate from = dateFrom != null ? dateFrom : deliveryFrom;
        LocalDate to = dateTo != null ? dateTo : deliveryTo;
        if (from == null) {
            return;
        }
        LocalDate end = to != null ? to : from;
        String type = StringUtils.hasText(dateType) ? dateType.trim() : "DELIVERY";
        if ("ORDER".equalsIgnoreCase(type)) {
            wrapper.ge(Order::getCreatedAt, from.atStartOfDay());
            wrapper.lt(Order::getCreatedAt, end.plusDays(1).atStartOfDay());
            return;
        }
        wrapper.ge(Order::getDeliveryDate, from);
        wrapper.le(Order::getDeliveryDate, end);
    }

    public OrderVO getDetail(Long id) {
        Order order = getOrderOrThrow(id);
        assertReadable(order);
        OrderVO vo = toDetailVO(order);
        if (isCustomerViewer()) {
            return maskPriceForCustomer(vo);
        }
        return vo;
    }

    @Transactional
    public OrderVO confirmByBoss(Long id) {
        RoleChecker.requireBoss();
        Order order = getOrderOrThrow(id);
        if (!OrderStatus.PENDING_CONFIRM.equals(order.getStatus())) {
            throw BusinessException.of(400, "只有待确认订单可以确认");
        }
        order.setStatus(OrderStatus.PENDING_PRICE);
        orderMapper.updateById(order);
        return getDetail(id);
    }

    private static final Set<String> BOSS_SETTABLE_STATUSES = Set.of(
            OrderStatus.PENDING_CONFIRM,
            OrderStatus.PENDING_PICK,
            OrderStatus.PICKING,
            OrderStatus.PENDING_PRICE,
            OrderStatus.PRICED,
            OrderStatus.COMPLETED,
            OrderStatus.CANCELLED
    );

    private static final Set<String> CLEAR_PRICING_STATUSES = Set.of(
            OrderStatus.PENDING_CONFIRM,
            OrderStatus.PENDING_PICK,
            OrderStatus.PICKING,
            OrderStatus.PENDING_PRICE
    );

    @Transactional
    public OrderVO markPrinted(Long id) {
        RoleChecker.requireBoss();
        Order order = getOrderOrThrow(id);
        if (OrderStatus.CANCELLED.equals(order.getStatus())) {
            throw BusinessException.of(400, "已取消订单不可打印");
        }
        order.setPrintedAt(LocalDateTime.now());
        orderMapper.updateById(order);
        return getDetail(id);
    }

    @Transactional
    public OrderVO updateStatusByBoss(Long id, String newStatus) {
        RoleChecker.requireBoss();
        if (!BOSS_SETTABLE_STATUSES.contains(newStatus)) {
            throw BusinessException.of(400, "不支持的目标状态");
        }
        Order order = getOrderOrThrow(id);
        if (OrderStatus.CANCELLED.equals(order.getStatus())) {
            throw BusinessException.of(400, "已取消订单不可修改");
        }
        if (newStatus.equals(order.getStatus())) {
            return getDetail(id);
        }
        if (OrderStatus.PRICED.equals(newStatus) && order.getAmount() == null) {
            throw BusinessException.of(400, "未录价订单不能设为待推送，请先录价");
        }
        if (OrderStatus.COMPLETED.equals(newStatus) && order.getAmount() == null) {
            throw BusinessException.of(400, "未录价订单不能标记为已完成");
        }
        if (CLEAR_PRICING_STATUSES.contains(newStatus)) {
            clearOrderPricing(order);
        }
        if (OrderStatus.PENDING_CONFIRM.equals(newStatus)) {
            order.setAssignedWorkerId(null);
        }
        order.setStatus(newStatus);
        orderMapper.updateById(order);
        return getDetail(id);
    }

    @Transactional
    public OrderVO markPaymentByBoss(Long id, OrderMarkPaymentRequest request) {
        RoleChecker.requireBoss();
        Order order = getOrderOrThrow(id);
        if (OrderStatus.CANCELLED.equals(order.getStatus())) {
            throw BusinessException.of(400, "已取消订单不可收款");
        }
        if (order.getCustomerId() == null) {
            throw BusinessException.of(400, "散客订单请先关联客户");
        }
        if (order.getAmount() == null) {
            throw BusinessException.of(400, "订单尚未录价，无法标记支付");
        }

        BigDecimal discount = request.getDiscount() != null ? request.getDiscount() : BigDecimal.ZERO;
        if (discount.compareTo(BigDecimal.ZERO) < 0) {
            throw BusinessException.of(400, "优惠金额无效");
        }
        if (discount.compareTo(order.getAmount()) > 0) {
            throw BusinessException.of(400, "优惠不能超过销售金额");
        }

        BigDecimal receivable = order.getAmount().subtract(discount);
        order.setReceivableAmount(receivable);

        BigDecimal amount = request.getAmount();
        BigDecimal paid = order.getPaidAmount() != null ? order.getPaidAmount() : BigDecimal.ZERO;
        BigDecimal outstanding = receivable.subtract(paid);
        if (outstanding.compareTo(BigDecimal.ZERO) <= 0) {
            throw BusinessException.of(400, "该订单已结清");
        }
        if (amount.compareTo(outstanding) > 0) {
            throw BusinessException.of(400, "收款不能超过欠款");
        }

        order.setPaidAmount(paid.add(amount));
        orderMapper.updateById(order);

        String method = StringUtils.hasText(request.getMethod()) ? request.getMethod().trim().toUpperCase() : "WECHAT";
        Payment payment = new Payment();
        payment.setMerchantId(order.getMerchantId());
        payment.setCustomerId(order.getCustomerId());
        payment.setOrderId(order.getId());
        payment.setAmount(amount);
        payment.setMethod(method);
        payment.setPaidAt(LocalDateTime.now());
        payment.setOperatorUserId(RoleChecker.currentUserId());
        if (StringUtils.hasText(request.getRemark())) {
            String remark = request.getRemark().trim();
            payment.setRemark(remark.length() > 200 ? remark.substring(0, 200) : remark);
        }
        paymentMapper.insert(payment);

        return getDetail(id);
    }

    private void clearOrderPricing(Order order) {
        orderMapper.update(null, new LambdaUpdateWrapper<Order>()
                .eq(Order::getId, order.getId())
                .set(Order::getAmount, null)
                .set(Order::getReceivableAmount, null));
        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, order.getId()));
        for (OrderItem item : items) {
            orderItemMapper.update(null, new LambdaUpdateWrapper<OrderItem>()
                    .eq(OrderItem::getId, item.getId())
                    .set(OrderItem::getDealPrice, null)
                    .set(OrderItem::getSubtotalAmount, null));
        }
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
        summary.put("pendingPick", todayOrders.stream().filter(o ->
                OrderStatus.PENDING_PICK.equals(o.getStatus())
                        || OrderStatus.PICKING.equals(o.getStatus())
                        || OrderStatus.PICKED.equals(o.getStatus())).count());
        summary.put("pendingPrice", todayOrders.stream()
                .filter(o -> o.getAmount() == null
                        && (OrderStatus.PENDING_CONFIRM.equals(o.getStatus())
                        || OrderStatus.PENDING_PICK.equals(o.getStatus())
                        || OrderStatus.PENDING_PRICE.equals(o.getStatus())))
                .count());
        summary.put("pendingPublish", todayOrders.stream().filter(o -> OrderStatus.PRICED.equals(o.getStatus())).count());
        return summary;
    }

    public BossDashboardVO bossDashboard() {
        RoleChecker.requireBoss();
        Long merchantId = merchantContext.currentMerchantId();
        LocalDate today = LocalDate.now();

        List<Order> todayOrders = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .eq(Order::getMerchantId, merchantId)
                .eq(Order::getDeliveryDate, today));

        BigDecimal todaySales = todayOrders.stream()
                .filter(o -> OrderStatus.COMPLETED.equals(o.getStatus()) && o.getAmount() != null)
                .map(Order::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Order> completedOrders = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .eq(Order::getMerchantId, merchantId)
                .eq(Order::getStatus, OrderStatus.COMPLETED));

        BigDecimal totalReceivable = completedOrders.stream()
                .map(this::outstandingReceivable)
                .filter(v -> v.compareTo(BigDecimal.ZERO) > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return BossDashboardVO.builder()
                .todaySales(todaySales)
                .todayProfit(BigDecimal.ZERO)
                .totalReceivable(totalReceivable)
                .totalPayable(BigDecimal.ZERO)
                .build();
    }

    private BigDecimal outstandingReceivable(Order order) {
        BigDecimal receivable = order.getReceivableAmount() != null ? order.getReceivableAmount() : order.getAmount();
        if (receivable == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal paid = order.getPaidAmount() != null ? order.getPaidAmount() : BigDecimal.ZERO;
        return receivable.subtract(paid);
    }

    private List<OrderVO> listOrders(LambdaQueryWrapper<Order> wrapper) {
        List<Order> orders = orderMapper.selectList(wrapper);
        if (orders.isEmpty()) {
            return List.of();
        }
        Set<Long> customerIds = orders.stream().map(Order::getCustomerId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> customerNames = customerIds.isEmpty()
                ? Map.of()
                : customerMapper.selectBatchIds(customerIds).stream()
                        .collect(Collectors.toMap(Customer::getId, Customer::getName));

        List<Long> orderIds = orders.stream().map(Order::getId).toList();
        List<OrderItem> allItems = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>().in(OrderItem::getOrderId, orderIds));
        Map<Long, Long> itemCounts = allItems.stream()
                .collect(Collectors.groupingBy(OrderItem::getOrderId, Collectors.counting()));
        Map<Long, Long> pickedCounts = allItems.stream()
                .filter(OrderService::isItemPicked)
                .collect(Collectors.groupingBy(OrderItem::getOrderId, Collectors.counting()));
        Map<Long, Boolean> priceIncompleteMap = allItems.stream()
                .collect(Collectors.groupingBy(OrderItem::getOrderId,
                        Collectors.collectingAndThen(Collectors.toList(), OrderService::isPriceIncomplete)));

        Set<Long> workerIds = orders.stream().map(Order::getAssignedWorkerId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> workerNames = workerIds.isEmpty()
                ? Map.of()
                : workerMapper.selectBatchIds(workerIds).stream().collect(Collectors.toMap(Worker::getId, Worker::getName));

        return orders.stream()
                .map(order -> toListVO(order, resolveCustomerName(order, customerNames),
                        itemCounts.getOrDefault(order.getId(), 0L).intValue(),
                        pickedCounts.getOrDefault(order.getId(), 0L).intValue(),
                        priceIncompleteMap.getOrDefault(order.getId(), true),
                        order.getAssignedWorkerId() != null
                                ? workerNames.get(order.getAssignedWorkerId())
                                : null))
                .toList();
    }

    private String resolveCustomerName(Order order, Map<Long, String> customerNames) {
        if (order.getCustomerId() != null) {
            String name = customerNames.get(order.getCustomerId());
            if (name != null) {
                return name;
            }
            Customer customer = customerMapper.selectById(order.getCustomerId());
            return customer != null ? customer.getName() : null;
        }
        return order.getGuestCustomerName();
    }

    private void validateBossOrderCustomer(BossOrderCreateRequest request) {
        if (request.getCustomerId() != null && StringUtils.hasText(request.getCustomerName())) {
            throw BusinessException.of(400, "请选择客户或输入临时客户名称，不可同时填写");
        }
        if (request.getCustomerId() == null && !StringUtils.hasText(request.getCustomerName())) {
            throw BusinessException.of(400, "请选择客户或输入临时客户名称");
        }
        if (request.getCustomerId() == null) {
            String name = request.getCustomerName().trim();
            if (name.length() > 100) {
                throw BusinessException.of(400, "客户名称过长");
            }
            request.setCustomerName(name);
        }
    }

    private static boolean isItemPicked(OrderItem item) {
        if (item.getShortageFlag() != null && item.getShortageFlag() == 1) {
            return true;
        }
        return item.getActualQty() != null;
    }

    private static boolean isPriceIncomplete(List<OrderItem> items) {
        return items.stream().anyMatch(item -> item.getDealPrice() == null);
    }

    private OrderVO toDetailVO(Order order) {
        Customer customer = order.getCustomerId() != null ? customerMapper.selectById(order.getCustomerId()) : null;
        String displayName = customer != null ? customer.getName() : order.getGuestCustomerName();
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

        int pickedItemCount = (int) items.stream().filter(OrderService::isItemPicked).count();

        return OrderVO.builder()
                .id(order.getId())
                .orderNo(order.getOrderNo())
                .customerId(order.getCustomerId())
                .customerName(displayName)
                .source(order.getSource())
                .status(order.getStatus())
                .statusLabel(statusLabel(order.getStatus()))
                .deliveryDate(order.getDeliveryDate())
                .deliveryAddress(order.getDeliveryAddress())
                .deliveryAddressShort(order.getDeliveryAddressShort())
                .contactName(order.getContactName())
                .contactPhone(order.getContactPhone())
                .amount(order.getAmount())
                .paidAmount(order.getPaidAmount())
                .receivableAmount(receivableAmount(order))
                .outstandingAmount(outstandingReceivable(order))
                .priceIncomplete(order.getAmount() == null || isPriceIncomplete(items))
                .paymentStatusLabel(paymentStatusLabel(order))
                .remark(order.getRemark())
                .createdAt(order.getCreatedAt())
                .items(itemVOs)
                .itemCount(itemVOs.size())
                .pickedItemCount(pickedItemCount)
                .printed(order.getPrintedAt() != null)
                .build();
    }

    private OrderVO toListVO(Order order, String customerName, int itemCount, int pickedItemCount,
                             boolean priceIncomplete, String workerName) {
        return OrderVO.builder()
                .id(order.getId())
                .orderNo(order.getOrderNo())
                .customerId(order.getCustomerId())
                .customerName(customerName)
                .source(order.getSource())
                .sourceLabel(sourceLabel(order.getSource()))
                .status(order.getStatus())
                .statusLabel(statusLabel(order.getStatus()))
                .deliveryDate(order.getDeliveryDate())
                .deliveryAddressShort(order.getDeliveryAddressShort())
                .contactName(order.getContactName())
                .amount(order.getAmount())
                .paidAmount(order.getPaidAmount())
                .receivableAmount(receivableAmount(order))
                .outstandingAmount(outstandingReceivable(order))
                .priceIncomplete(order.getAmount() == null || priceIncomplete)
                .paymentStatusLabel(paymentStatusLabel(order))
                .remark(order.getRemark())
                .createdAt(order.getCreatedAt())
                .itemCount(itemCount)
                .pickedItemCount(pickedItemCount)
                .assignedWorkerId(order.getAssignedWorkerId())
                .assignedWorkerName(workerName)
                .printed(order.getPrintedAt() != null)
                .build();
    }

    private String sourceLabel(String source) {
        if (source == null) {
            return null;
        }
        return switch (source) {
            case "BOSS_MANUAL" -> "代客下单";
            case "CUSTOMER_APP" -> "客户下单";
            case "IMAGE" -> "图片下单";
            case "TEXT" -> "文字下单";
            case "VOICE" -> "语音下单";
            case "COPY_HISTORY" -> "复制历史";
            default -> source;
        };
    }

    private String paymentStatusLabel(Order order) {
        BigDecimal receivable = receivableAmount(order);
        if (receivable == null) {
            return "未支付";
        }
        BigDecimal paid = order.getPaidAmount() != null ? order.getPaidAmount() : BigDecimal.ZERO;
        if (paid.compareTo(receivable) >= 0) {
            return "已支付";
        }
        if (paid.compareTo(BigDecimal.ZERO) > 0) {
            return "部分支付";
        }
        return "未支付";
    }

    private BigDecimal receivableAmount(Order order) {
        return order.getReceivableAmount() != null ? order.getReceivableAmount() : order.getAmount();
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
            case OrderStatus.PRICED -> "已录价";
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

    private Map<Long, Product> loadBossProducts(List<BossOrderItemCreateRequest> items) {
        List<OrderItemCreateRequest> normalized = items.stream().map(i -> {
            OrderItemCreateRequest req = new OrderItemCreateRequest();
            req.setProductId(i.getProductId());
            req.setOrderQty(i.getOrderQty());
            return req;
        }).toList();
        return loadProducts(normalized);
    }

    private Map<Long, Product> loadProductsForBossUpdate(List<BossOrderItemCreateRequest> items) {
        Set<Long> productIds = items.stream().map(BossOrderItemCreateRequest::getProductId).collect(Collectors.toSet());
        List<Product> products = productMapper.selectList(new LambdaQueryWrapper<Product>()
                .in(Product::getId, productIds)
                .eq(Product::getMerchantId, merchantContext.currentMerchantId()));
        Map<Long, Product> productMap = products.stream().collect(Collectors.toMap(Product::getId, p -> p));

        List<String> missing = new ArrayList<>();
        for (BossOrderItemCreateRequest item : items) {
            if (!productMap.containsKey(item.getProductId())) {
                missing.add(String.valueOf(item.getProductId()));
            }
        }
        if (!missing.isEmpty()) {
            throw BusinessException.of(400, "以下商品不存在: " + String.join(",", missing));
        }
        return productMap;
    }

    private String resolveGuestCustomerName(String customerName) {
        if (!StringUtils.hasText(customerName)) {
            throw BusinessException.of(400, "请输入店铺/客户名称");
        }
        String name = customerName.trim();
        if (name.length() > 100) {
            throw BusinessException.of(400, "客户名称过长");
        }
        return name;
    }

    private boolean isCustomerViewer() {
        return StpUtil.isLogin() && StpUtil.hasRole(UserRole.CUSTOMER.name());
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
        if (isCustomerViewer()) {
            RoleChecker.requireCustomer();
            Long customerId = RoleChecker.currentCustomerId();
            if (customerId != null) {
                if (!Objects.equals(order.getCustomerId(), customerId)) {
                    throw BusinessException.of(403, "无权查看该订单");
                }
                return;
            }
            if (order.getCustomerId() != null
                    || !Objects.equals(order.getCreatedBy(), RoleChecker.currentUserId())) {
                throw BusinessException.of(403, "无权查看该订单");
            }
            return;
        }
        RoleChecker.requireBoss();
    }

    private OrderVO maskPriceForCustomer(OrderVO vo) {
        vo.setStatusLabel(customerStatusLabel(vo.getStatus()));
        if (!OrderStatus.COMPLETED.equals(vo.getStatus())) {
            vo.setAmount(null);
            if (vo.getItems() != null) {
                vo.getItems().forEach(item -> {
                    item.setDealPrice(null);
                    item.setSubtotalAmount(null);
                });
            }
        }
        return vo;
    }

    private String customerStatusLabel(String status) {
        if (status == null) {
            return "处理中";
        }
        return switch (status) {
            case OrderStatus.PENDING_CONFIRM -> "待商家确认";
            case OrderStatus.PENDING_PICK, OrderStatus.PICKING -> "备货中";
            case OrderStatus.PICKED, OrderStatus.DELIVERING, OrderStatus.DELIVERED -> "配送中";
            case OrderStatus.PENDING_PRICE, OrderStatus.PRICED -> "待确认账单";
            case OrderStatus.COMPLETED -> "已完成";
            case OrderStatus.CANCELLED -> "已取消";
            default -> "处理中";
        };
    }

    private String generateOrderNo() {
        return "O" + LocalDateTime.now().format(ORDER_NO_FMT) + String.format("%03d", RANDOM.nextInt(1000));
    }
}
