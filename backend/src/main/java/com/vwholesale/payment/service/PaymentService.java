package com.vwholesale.payment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.vwholesale.common.context.MerchantContext;
import com.vwholesale.common.enums.OrderStatus;
import com.vwholesale.common.exception.BusinessException;
import com.vwholesale.common.security.RoleChecker;
import com.vwholesale.customer.entity.Customer;
import com.vwholesale.customer.mapper.CustomerMapper;
import com.vwholesale.order.entity.Order;
import com.vwholesale.order.mapper.OrderMapper;
import com.vwholesale.order.support.OrderReceivableRules;
import com.vwholesale.payment.dto.PaymentCreateRequest;
import com.vwholesale.payment.dto.PaymentVO;
import com.vwholesale.payment.dto.BossPaymentStatsVO;
import com.vwholesale.payment.dto.PaymentDailyRow;
import com.vwholesale.payment.entity.Payment;
import com.vwholesale.payment.mapper.PaymentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentMapper paymentMapper;
    private final OrderMapper orderMapper;
    private final CustomerMapper customerMapper;
    private final MerchantContext merchantContext;

    @Transactional
    public PaymentVO create(PaymentCreateRequest request) {
        RoleChecker.requireBoss();
        Long merchantId = merchantContext.currentMerchantId();

        Customer customer = customerMapper.selectById(request.getCustomerId());
        if (customer == null || !merchantId.equals(customer.getMerchantId())) {
            throw BusinessException.of(400, "客户不存在");
        }
        if (customer.getStatus() != null && customer.getStatus() == 0) {
            throw BusinessException.of(400, "客户档案已停用");
        }

        BigDecimal amount = request.getAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw BusinessException.of(400, "请输入有效金额");
        }

        String method = StringUtils.hasText(request.getMethod()) ? request.getMethod().trim().toUpperCase() : "OTHER";
        LocalDateTime paidAt = request.getPaidAt() != null ? request.getPaidAt() : LocalDateTime.now();

        Payment payment = new Payment();
        payment.setMerchantId(merchantId);
        payment.setCustomerId(customer.getId());
        payment.setAmount(amount);
        payment.setMethod(method);
        payment.setPaidAt(paidAt);
        payment.setOperatorUserId(RoleChecker.currentUserId());
        payment.setRemark(trimRemark(request.getRemark()));
        if (request.getVoucherUrls() != null && !request.getVoucherUrls().isEmpty()) {
            payment.setVoucherUrls(String.join(",", request.getVoucherUrls()));
        }
        paymentMapper.insert(payment);

        allocateToOrders(customer.getId(), merchantId, amount, payment);

        return toVO(payment, customer.getName());
    }

    public List<PaymentVO> listForBoss(Long customerId) {
        RoleChecker.requireBoss();
        if (customerId != null) {
            return listByCustomer(customerId);
        }
        Long merchantId = merchantContext.currentMerchantId();
        List<Payment> payments = paymentMapper.selectList(new LambdaQueryWrapper<Payment>()
                .eq(Payment::getMerchantId, merchantId)
                .orderByDesc(Payment::getPaidAt)
                .orderByDesc(Payment::getId)
                .last("LIMIT 200"));
        if (payments.isEmpty()) {
            return List.of();
        }
        Set<Long> customerIds = payments.stream().map(Payment::getCustomerId).collect(Collectors.toSet());
        Map<Long, String> customerNames = customerMapper.selectBatchIds(customerIds).stream()
                .collect(Collectors.toMap(Customer::getId, Customer::getName));
        return payments.stream()
                .map(p -> toVO(p, customerNames.getOrDefault(p.getCustomerId(), "未知客户")))
                .toList();
    }

    public BossPaymentStatsVO paymentStats(LocalDate dateFrom, LocalDate dateTo) {
        RoleChecker.requireBoss();
        if (dateFrom == null || dateTo == null) {
            throw BusinessException.of(400, "请选择日期范围");
        }
        if (dateFrom.isAfter(dateTo)) {
            throw BusinessException.of(400, "开始日期不能晚于结束日期");
        }
        Long merchantId = merchantContext.currentMerchantId();
        List<Payment> payments = paymentMapper.selectList(new LambdaQueryWrapper<Payment>()
                .eq(Payment::getMerchantId, merchantId)
                .ge(Payment::getPaidAt, dateFrom.atStartOfDay())
                .lt(Payment::getPaidAt, dateTo.plusDays(1).atStartOfDay()));

        Map<LocalDate, BigDecimal> grouped = new HashMap<>();
        for (Payment payment : payments) {
            if (payment.getPaidAt() == null) {
                continue;
            }
            LocalDate day = payment.getPaidAt().toLocalDate();
            grouped.merge(day, payment.getAmount(), BigDecimal::add);
        }

        List<PaymentDailyRow> rows = new ArrayList<>();
        BigDecimal totalReceived = BigDecimal.ZERO;
        for (LocalDate day = dateFrom; !day.isAfter(dateTo); day = day.plusDays(1)) {
            BigDecimal amount = grouped.getOrDefault(day, BigDecimal.ZERO);
            rows.add(PaymentDailyRow.builder().date(day).amount(amount).build());
            totalReceived = totalReceived.add(amount);
        }

        return BossPaymentStatsVO.builder()
                .totalReceived(totalReceived)
                .rows(rows)
                .build();
    }

    public List<PaymentVO> listByCustomer(Long customerId) {
        RoleChecker.requireBoss();
        Long merchantId = merchantContext.currentMerchantId();
        Customer customer = customerMapper.selectById(customerId);
        if (customer == null || !merchantId.equals(customer.getMerchantId())) {
            throw BusinessException.of(400, "客户不存在");
        }
        List<Payment> payments = paymentMapper.selectList(new LambdaQueryWrapper<Payment>()
                .eq(Payment::getMerchantId, merchantId)
                .eq(Payment::getCustomerId, customerId)
                .orderByDesc(Payment::getPaidAt)
                .orderByDesc(Payment::getId));
        return payments.stream()
                .map(p -> toVO(p, customer.getName()))
                .toList();
    }

    private void allocateToOrders(Long customerId, Long merchantId, BigDecimal amount, Payment payment) {
        List<Order> orders = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .eq(Order::getCustomerId, customerId)
                .eq(Order::getMerchantId, merchantId)
                .ne(Order::getStatus, OrderStatus.CANCELLED)
                .orderByAsc(Order::getDeliveryDate)
                .orderByAsc(Order::getCreatedAt))
                .stream()
                .filter(OrderReceivableRules::countsTowardCustomerDebt)
                .toList();

        BigDecimal remaining = amount;
        Long linkedOrderId = null;
        int affectedOrders = 0;

        for (Order order : orders) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            BigDecimal outstanding = OrderReceivableRules.outstandingReceivable(order);
            if (outstanding.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal apply = remaining.min(outstanding);
            BigDecimal paid = order.getPaidAmount() != null ? order.getPaidAmount() : BigDecimal.ZERO;
            order.setPaidAmount(paid.add(apply));
            orderMapper.updateById(order);
            remaining = remaining.subtract(apply);
            if (linkedOrderId == null) {
                linkedOrderId = order.getId();
            }
            affectedOrders++;
        }

        if (linkedOrderId != null && affectedOrders == 1) {
            payment.setOrderId(linkedOrderId);
            paymentMapper.updateById(payment);
        }
    }

    private String trimRemark(String remark) {
        if (!StringUtils.hasText(remark)) {
            return null;
        }
        String trimmed = remark.trim();
        return trimmed.length() > 200 ? trimmed.substring(0, 200) : trimmed;
    }

    private PaymentVO toVO(Payment payment, String customerName) {
        return PaymentVO.builder()
                .id(payment.getId())
                .customerId(payment.getCustomerId())
                .customerName(customerName)
                .orderId(payment.getOrderId())
                .amount(payment.getAmount())
                .method(payment.getMethod())
                .paidAt(payment.getPaidAt())
                .remark(payment.getRemark())
                .voucherUrls(parseVoucherUrls(payment.getVoucherUrls()))
                .createdAt(payment.getCreatedAt())
                .build();
    }

    private List<String> parseVoucherUrls(String raw) {
        if (!StringUtils.hasText(raw)) {
            return Collections.emptyList();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }
}
