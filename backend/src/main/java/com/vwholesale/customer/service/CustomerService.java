package com.vwholesale.customer.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.vwholesale.common.config.AppProperties;
import com.vwholesale.common.context.MerchantContext;
import com.vwholesale.common.enums.OrderStatus;
import com.vwholesale.common.exception.BusinessException;
import com.vwholesale.common.security.RoleChecker;
import com.vwholesale.order.support.OrderReceivableRules;
import com.vwholesale.customer.dto.CustomerBindRequest;
import com.vwholesale.customer.dto.CustomerCreateRequest;
import com.vwholesale.customer.dto.CustomerUpdateRequest;
import com.vwholesale.customer.dto.CustomerVO;
import com.vwholesale.customer.dto.InviteCodeVO;
import com.vwholesale.customer.entity.Customer;
import com.vwholesale.customer.mapper.CustomerMapper;
import com.vwholesale.user.entity.User;
import com.vwholesale.user.mapper.UserMapper;
import com.vwholesale.order.mapper.OrderMapper;
import com.vwholesale.order.entity.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String INVITE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final CustomerMapper customerMapper;
    private final UserMapper userMapper;
    private final OrderMapper orderMapper;
    private final MerchantContext merchantContext;
    private final AppProperties appProperties;

    public List<CustomerVO> listForBoss(String keyword) {
        RoleChecker.requireBoss();
        Long merchantId = merchantContext.currentMerchantId();
        LambdaQueryWrapper<Customer> wrapper = new LambdaQueryWrapper<Customer>()
                .eq(Customer::getMerchantId, merchantId)
                .orderByDesc(Customer::getId);
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(Customer::getName, kw)
                    .or().like(Customer::getContactName, kw)
                    .or().like(Customer::getPhone, kw));
        }
        Map<Long, CustomerOrderStats> statsMap = loadOrderStats(merchantId);
        return customerMapper.selectList(wrapper).stream()
                .map(c -> toVO(c, statsMap.get(c.getId())))
                .toList();
    }

    public CustomerVO getById(Long id) {
        RoleChecker.requireBoss();
        Customer customer = getCustomerOrThrow(id);
        Map<Long, CustomerOrderStats> statsMap = loadOrderStats(merchantContext.currentMerchantId());
        return toVO(customer, statsMap.get(customer.getId()));
    }

    @Transactional
    public CustomerVO create(CustomerCreateRequest request) {
        RoleChecker.requireBoss();
        Customer customer = new Customer();
        customer.setMerchantId(merchantContext.currentMerchantId());
        customer.setName(request.getName().trim());
        customer.setContactName(request.getContactName());
        customer.setPhone(request.getPhone());
        customer.setAddress(request.getAddress());
        customer.setAddressShort(request.getAddressShort());
        customer.setDefaultDeliveryTime(request.getDefaultDeliveryTime());
        customer.setSettlementType(StringUtils.hasText(request.getSettlementType()) ? request.getSettlementType() : "CASH");
        customer.setPriceLevel(request.getPriceLevel());
        customer.setAutoConfirmOrder(Boolean.TRUE.equals(request.getAutoConfirmOrder()) ? 1 : 0);
        customer.setBindStatus("NOT_INVITED");
        customer.setRemark(request.getRemark());
        customer.setStatus(1);
        customerMapper.insert(customer);
        return toVO(customer, null);
    }

    @Transactional
    public CustomerVO update(Long id, CustomerUpdateRequest request) {
        RoleChecker.requireBoss();
        Customer customer = getCustomerOrThrow(id);
        if (StringUtils.hasText(request.getName())) {
            customer.setName(request.getName().trim());
        }
        if (request.getContactName() != null) {
            customer.setContactName(request.getContactName());
        }
        if (request.getPhone() != null) {
            customer.setPhone(request.getPhone());
        }
        if (request.getAddress() != null) {
            customer.setAddress(request.getAddress());
        }
        if (request.getAddressShort() != null) {
            customer.setAddressShort(request.getAddressShort());
        }
        if (request.getDefaultDeliveryTime() != null) {
            customer.setDefaultDeliveryTime(request.getDefaultDeliveryTime());
        }
        if (StringUtils.hasText(request.getSettlementType())) {
            customer.setSettlementType(request.getSettlementType());
        }
        if (request.getPriceLevel() != null) {
            customer.setPriceLevel(request.getPriceLevel());
        }
        if (request.getAutoConfirmOrder() != null) {
            customer.setAutoConfirmOrder(Boolean.TRUE.equals(request.getAutoConfirmOrder()) ? 1 : 0);
        }
        if (request.getRemark() != null) {
            customer.setRemark(request.getRemark());
        }
        if (request.getStatus() != null) {
            customer.setStatus(request.getStatus());
        }
        customerMapper.updateById(customer);
        Map<Long, CustomerOrderStats> statsMap = loadOrderStats(merchantContext.currentMerchantId());
        return toVO(customer, statsMap.get(customer.getId()));
    }

    @Transactional
    public void delete(Long id) {
        RoleChecker.requireBoss();
        Customer customer = getCustomerOrThrow(id);
        Long orderCount = orderMapper.selectCount(new LambdaQueryWrapper<Order>()
                .eq(Order::getCustomerId, id)
                .eq(Order::getMerchantId, merchantContext.currentMerchantId()));
        if (orderCount != null && orderCount > 0) {
            throw BusinessException.of(400, "该客户已有订单记录，无法删除");
        }
        if (customer.getBindUserId() != null) {
            User user = userMapper.selectById(customer.getBindUserId());
            if (user != null && id.equals(user.getCustomerId())) {
                user.setCustomerId(null);
                user.setStatus("PENDING_BIND");
                userMapper.updateById(user);
            }
        }
        customerMapper.deleteById(id);
    }

    @Transactional
    public InviteCodeVO generateInviteCode(Long id) {
        RoleChecker.requireBoss();
        Customer customer = getCustomerOrThrow(id);
        if ("BOUND".equals(customer.getBindStatus()) && customer.getBindUserId() != null) {
            throw BusinessException.of(400, "客户已绑定微信，无需重新生成邀请码");
        }

        String code = generateUniqueInviteCode();
        int expireDays = appProperties.getCustomer().getInviteCodeExpireDays();
        customer.setInviteCode(code);
        customer.setInviteExpiredAt(LocalDateTime.now().plusDays(expireDays));
        customer.setBindStatus("INVITED");
        customerMapper.updateById(customer);

        return InviteCodeVO.builder()
                .customerId(customer.getId())
                .customerName(customer.getName())
                .inviteCode(code)
                .inviteExpiredAt(customer.getInviteExpiredAt())
                .build();
    }

    @Transactional
    public CustomerVO bindByInviteCode(CustomerBindRequest request) {
        RoleChecker.requireCustomer();
        long userId = RoleChecker.currentUserId();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.of(401, "用户不存在");
        }
        if (user.getCustomerId() != null) {
            throw BusinessException.of(400, "您已绑定客户档案，无需重复绑定");
        }

        String code = request.getInviteCode().trim().toUpperCase();
        Customer customer = customerMapper.selectOne(new LambdaQueryWrapper<Customer>()
                .eq(Customer::getMerchantId, merchantContext.currentMerchantId())
                .eq(Customer::getInviteCode, code));
        if (customer == null) {
            throw BusinessException.of(400, "邀请码无效");
        }
        if (customer.getInviteExpiredAt() != null && customer.getInviteExpiredAt().isBefore(LocalDateTime.now())) {
            throw BusinessException.of(400, "邀请码已过期，请联系老板重新生成");
        }
        if ("BOUND".equals(customer.getBindStatus()) && customer.getBindUserId() != null) {
            throw BusinessException.of(400, "该邀请码已被使用");
        }

        customer.setBindUserId(userId);
        customer.setBindStatus("BOUND");
        customerMapper.updateById(customer);

        user.setCustomerId(customer.getId());
        user.setStatus("ENABLED");
        userMapper.updateById(user);

        StpUtil.getSession().set("customerId", customer.getId());

        return toVO(customer, null);
    }

    private Map<Long, CustomerOrderStats> loadOrderStats(Long merchantId) {
        List<Order> orders = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .eq(Order::getMerchantId, merchantId)
                .isNotNull(Order::getCustomerId)
                .ne(Order::getStatus, OrderStatus.CANCELLED));
        Map<Long, CustomerOrderStats> statsMap = new HashMap<>();
        for (Order order : orders) {
            if (order.getCustomerId() == null) {
                continue;
            }
            CustomerOrderStats stats = statsMap.computeIfAbsent(order.getCustomerId(), id -> new CustomerOrderStats());
            if (order.getCreatedAt() != null
                    && (stats.lastOrderAt == null || order.getCreatedAt().isAfter(stats.lastOrderAt))) {
                stats.lastOrderAt = order.getCreatedAt();
            }
            if (OrderStatus.COMPLETED.equals(order.getStatus())) {
                BigDecimal receivable = order.getReceivableAmount() != null ? order.getReceivableAmount() : order.getAmount();
                if (receivable != null) {
                    stats.totalSalesAmount = stats.totalSalesAmount.add(receivable);
                }
            }
            if (OrderReceivableRules.countsTowardCustomerDebt(order)) {
                BigDecimal outstanding = OrderReceivableRules.outstandingReceivable(order);
                if (outstanding.compareTo(BigDecimal.ZERO) > 0) {
                    stats.outstandingAmount = stats.outstandingAmount.add(outstanding);
                }
            }
        }
        return statsMap;
    }

    private static class CustomerOrderStats {
        private BigDecimal outstandingAmount = BigDecimal.ZERO;
        private BigDecimal totalSalesAmount = BigDecimal.ZERO;
        private LocalDateTime lastOrderAt;
    }

    private CustomerVO toVO(Customer customer) {
        return toVO(customer, null);
    }

    private CustomerVO toVO(Customer customer, CustomerOrderStats stats) {
        CustomerVO.CustomerVOBuilder builder = CustomerVO.builder()
                .id(customer.getId())
                .name(customer.getName())
                .contactName(customer.getContactName())
                .phone(customer.getPhone())
                .address(customer.getAddress())
                .addressShort(customer.getAddressShort())
                .defaultDeliveryTime(customer.getDefaultDeliveryTime())
                .settlementType(customer.getSettlementType())
                .priceLevel(customer.getPriceLevel())
                .autoConfirmOrder(customer.getAutoConfirmOrder() != null && customer.getAutoConfirmOrder() == 1)
                .bindUserId(customer.getBindUserId())
                .bindStatus(customer.getBindStatus())
                .inviteCode(customer.getInviteCode())
                .inviteExpiredAt(customer.getInviteExpiredAt())
                .remark(customer.getRemark())
                .status(customer.getStatus())
                .createdAt(customer.getCreatedAt());
        if (stats != null) {
            builder.outstandingAmount(stats.outstandingAmount)
                    .totalSalesAmount(stats.totalSalesAmount)
                    .lastOrderAt(stats.lastOrderAt);
        } else {
            builder.outstandingAmount(BigDecimal.ZERO)
                    .totalSalesAmount(BigDecimal.ZERO);
        }
        return builder.build();
    }

    public Map<String, Object> bindStatus() {
        RoleChecker.requireCustomer();
        Long customerId = RoleChecker.currentCustomerId();
        if (customerId == null) {
            User user = userMapper.selectById(RoleChecker.currentUserId());
            if (user != null && user.getCustomerId() != null) {
                customerId = user.getCustomerId();
            }
        }
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("bound", customerId != null);
        result.put("customerId", customerId != null ? customerId : 0);
        if (customerId != null) {
            Customer customer = customerMapper.selectById(customerId);
            if (customer != null) {
                result.put("customerName", customer.getName());
            }
        }
        return result;
    }

    private Customer getCustomerOrThrow(Long id) {
        Customer customer = customerMapper.selectOne(new LambdaQueryWrapper<Customer>()
                .eq(Customer::getId, id)
                .eq(Customer::getMerchantId, merchantContext.currentMerchantId()));
        if (customer == null) {
            throw BusinessException.of(404, "客户不存在");
        }
        return customer;
    }

    private String generateUniqueInviteCode() {
        for (int i = 0; i < 10; i++) {
            String code = randomCode(8);
            Long count = customerMapper.selectCount(new LambdaQueryWrapper<Customer>().eq(Customer::getInviteCode, code));
            if (count == 0) {
                return code;
            }
        }
        throw BusinessException.of(500, "邀请码生成失败，请重试");
    }

    private String randomCode(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(INVITE_CHARS.charAt(RANDOM.nextInt(INVITE_CHARS.length())));
        }
        return sb.toString();
    }
}
