package com.vwholesale.customer.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.vwholesale.common.context.MerchantContext;
import com.vwholesale.common.exception.BusinessException;
import com.vwholesale.common.security.RoleChecker;
import com.vwholesale.customer.dto.CustomerBindRequestRejectRequest;
import com.vwholesale.customer.dto.CustomerBindRequestVO;
import com.vwholesale.customer.dto.CustomerRegisterApplyRequest;
import com.vwholesale.customer.dto.CustomerRegisterStatusVO;
import com.vwholesale.customer.dto.CustomerVO;
import com.vwholesale.customer.entity.CustomerBindRequest;
import com.vwholesale.customer.entity.Customer;
import com.vwholesale.customer.mapper.CustomerBindRequestMapper;
import com.vwholesale.customer.mapper.CustomerMapper;
import com.vwholesale.customer.support.CustomerNoGenerator;
import com.vwholesale.user.entity.User;
import com.vwholesale.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomerBindRequestService {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";

    private final CustomerBindRequestMapper bindRequestMapper;
    private final CustomerMapper customerMapper;
    private final CustomerNoGenerator customerNoGenerator;
    private final UserMapper userMapper;
    private final MerchantContext merchantContext;
    private final CustomerRegisterInviteService registerInviteService;
    private final CustomerService customerService;

    @Transactional
    public CustomerBindRequestVO apply(CustomerRegisterApplyRequest request) {
        RoleChecker.requireCustomer();
        User user = userMapper.selectById(RoleChecker.currentUserId());
        if (user == null) {
            throw BusinessException.of(401, "用户不存在");
        }
        if (user.getCustomerId() != null) {
            throw BusinessException.of(400, "您已绑定客户档案");
        }

        registerInviteService.validateToken(request.getRegisterToken(), user.getMerchantId());

        CustomerBindRequest pending = findLatestPending(user.getId());
        if (pending != null) {
            throw BusinessException.of(400, "您已有待审核申请，请等待老板处理");
        }

        CustomerBindRequest row = new CustomerBindRequest();
        row.setMerchantId(user.getMerchantId());
        row.setUserId(user.getId());
        row.setShopName(request.getShopName().trim());
        row.setContactName(trimToNull(request.getContactName()));
        row.setPhone(trimToNull(request.getPhone()));
        row.setAddress(trimToNull(request.getAddress()));
        row.setAddressShort(trimToNull(request.getAddressShort()));
        row.setStatus(STATUS_PENDING);
        bindRequestMapper.insert(row);
        return toVO(row, user.getNickname());
    }

    public CustomerRegisterStatusVO registerStatus() {
        RoleChecker.requireCustomer();
        User user = userMapper.selectById(RoleChecker.currentUserId());
        if (user == null) {
            throw BusinessException.of(401, "用户不存在");
        }

        CustomerRegisterStatusVO.CustomerRegisterStatusVOBuilder builder = CustomerRegisterStatusVO.builder()
                .bound(user.getCustomerId() != null);

        if (user.getCustomerId() != null) {
            Customer customer = customerMapper.selectById(user.getCustomerId());
            if (customer != null) {
                builder.customerId(customer.getId()).customerName(customer.getName());
            }
            return builder.build();
        }

        CustomerBindRequest latest = bindRequestMapper.selectOne(new LambdaQueryWrapper<CustomerBindRequest>()
                .eq(CustomerBindRequest::getUserId, user.getId())
                .orderByDesc(CustomerBindRequest::getId)
                .last("LIMIT 1"));

        if (latest == null) {
            return builder.pendingReview(false).build();
        }

        builder.lastRequestStatus(latest.getStatus())
                .submittedAt(latest.getCreatedAt())
                .pendingReview(STATUS_PENDING.equals(latest.getStatus()))
                .pendingRequestId(STATUS_PENDING.equals(latest.getStatus()) ? latest.getId() : null)
                .rejectReason(latest.getRejectReason());
        return builder.build();
    }

    public long countPendingForBoss() {
        RoleChecker.requireBoss();
        Long count = bindRequestMapper.selectCount(new LambdaQueryWrapper<CustomerBindRequest>()
                .eq(CustomerBindRequest::getMerchantId, merchantContext.currentMerchantId())
                .eq(CustomerBindRequest::getStatus, STATUS_PENDING));
        return count != null ? count : 0;
    }

    public List<CustomerBindRequestVO> listForBoss(String status) {
        RoleChecker.requireBoss();
        LambdaQueryWrapper<CustomerBindRequest> wrapper = new LambdaQueryWrapper<CustomerBindRequest>()
                .eq(CustomerBindRequest::getMerchantId, merchantContext.currentMerchantId())
                .orderByDesc(CustomerBindRequest::getId);
        if (StringUtils.hasText(status)) {
            wrapper.eq(CustomerBindRequest::getStatus, status.trim().toUpperCase());
        }
        List<CustomerBindRequest> rows = bindRequestMapper.selectList(wrapper);
        Map<Long, String> nicknames = loadNicknames(rows);
        return rows.stream().map(row -> toVO(row, nicknames.get(row.getUserId()))).toList();
    }

    @Transactional
    public CustomerVO approve(Long id) {
        RoleChecker.requireBoss();
        CustomerBindRequest row = getRequestOrThrow(id);
        if (!STATUS_PENDING.equals(row.getStatus())) {
            throw BusinessException.of(400, "该申请已处理");
        }
        User user = userMapper.selectById(row.getUserId());
        if (user == null) {
            throw BusinessException.of(400, "申请人不存在");
        }
        if (user.getCustomerId() != null) {
            throw BusinessException.of(400, "该用户已绑定其他客户档案");
        }

        Customer customer = new Customer();
        customer.setMerchantId(row.getMerchantId());
        customer.setName(row.getShopName());
        customer.setContactName(row.getContactName());
        customer.setPhone(row.getPhone());
        customer.setAddress(row.getAddress());
        customer.setAddressShort(row.getAddressShort());
        customer.setSettlementType("CASH");
        customer.setAutoConfirmOrder(0);
        customer.setBindStatus("BOUND");
        customer.setBindUserId(user.getId());
        customer.setStatus(1);
        customer.setCustomerNo(customerNoGenerator.nextNo(row.getMerchantId()));
        customerMapper.insert(customer);

        user.setCustomerId(customer.getId());
        user.setStatus("ENABLED");
        userMapper.updateById(user);

        row.setStatus(STATUS_APPROVED);
        row.setCustomerId(customer.getId());
        row.setReviewedBy(RoleChecker.currentUserId());
        row.setReviewedAt(LocalDateTime.now());
        bindRequestMapper.updateById(row);

        if (StpUtil.isLogin() && user.getId().equals(StpUtil.getLoginIdAsLong())) {
            // no-op for boss session
        }

        return customerService.getById(customer.getId());
    }

    @Transactional
    public void reject(Long id, CustomerBindRequestRejectRequest request) {
        RoleChecker.requireBoss();
        CustomerBindRequest row = getRequestOrThrow(id);
        if (!STATUS_PENDING.equals(row.getStatus())) {
            throw BusinessException.of(400, "该申请已处理");
        }
        row.setStatus(STATUS_REJECTED);
        row.setReviewedBy(RoleChecker.currentUserId());
        row.setReviewedAt(LocalDateTime.now());
        if (request != null && StringUtils.hasText(request.getReason())) {
            String reason = request.getReason().trim();
            row.setRejectReason(reason.length() > 256 ? reason.substring(0, 256) : reason);
        }
        bindRequestMapper.updateById(row);
    }

    private CustomerBindRequest getRequestOrThrow(Long id) {
        CustomerBindRequest row = bindRequestMapper.selectOne(new LambdaQueryWrapper<CustomerBindRequest>()
                .eq(CustomerBindRequest::getId, id)
                .eq(CustomerBindRequest::getMerchantId, merchantContext.currentMerchantId()));
        if (row == null) {
            throw BusinessException.of(404, "申请不存在");
        }
        return row;
    }

    private CustomerBindRequest findLatestPending(Long userId) {
        return bindRequestMapper.selectOne(new LambdaQueryWrapper<CustomerBindRequest>()
                .eq(CustomerBindRequest::getUserId, userId)
                .eq(CustomerBindRequest::getStatus, STATUS_PENDING)
                .orderByDesc(CustomerBindRequest::getId)
                .last("LIMIT 1"));
    }

    private Map<Long, String> loadNicknames(List<CustomerBindRequest> rows) {
        Map<Long, String> map = new HashMap<>();
        for (CustomerBindRequest row : rows) {
            if (row.getUserId() == null || map.containsKey(row.getUserId())) {
                continue;
            }
            User user = userMapper.selectById(row.getUserId());
            if (user != null) {
                map.put(user.getId(), user.getNickname());
            }
        }
        return map;
    }

    private CustomerBindRequestVO toVO(CustomerBindRequest row, String nickname) {
        return CustomerBindRequestVO.builder()
                .id(row.getId())
                .userId(row.getUserId())
                .shopName(row.getShopName())
                .contactName(row.getContactName())
                .phone(row.getPhone())
                .address(row.getAddress())
                .addressShort(row.getAddressShort())
                .status(row.getStatus())
                .customerId(row.getCustomerId())
                .rejectReason(row.getRejectReason())
                .reviewedAt(row.getReviewedAt())
                .createdAt(row.getCreatedAt())
                .applicantNickname(nickname)
                .build();
    }

    private static String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
