package com.vwholesale.auth.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.vwholesale.auth.dto.DevLoginRequest;
import com.vwholesale.auth.dto.LoginResponse;
import com.vwholesale.auth.dto.WechatLoginRequest;
import com.vwholesale.common.config.AppProperties;
import com.vwholesale.common.context.MerchantContext;
import com.vwholesale.common.context.MerchantContextHolder;
import com.vwholesale.common.enums.UserRole;
import com.vwholesale.common.exception.BusinessException;
import com.vwholesale.customer.entity.Customer;
import com.vwholesale.customer.mapper.CustomerMapper;
import com.vwholesale.customer.service.CustomerService;
import com.vwholesale.merchant.entity.Merchant;
import com.vwholesale.merchant.mapper.MerchantMapper;
import com.vwholesale.user.entity.User;
import com.vwholesale.user.mapper.UserMapper;
import com.vwholesale.worker.entity.Worker;
import com.vwholesale.worker.service.WorkerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final CustomerMapper customerMapper;
    private final MerchantMapper merchantMapper;
    private final WechatClient wechatClient;
    private final AppProperties appProperties;
    private final MerchantContext merchantContext;
    private final WorkerService workerService;
    private final ActivationService activationService;
    private final CustomerService customerService;

    @Transactional
    public LoginResponse wechatLogin(WechatLoginRequest request) {
        WechatSessionResponse session = wechatClient.code2Session(request.getCode());
        return loginByOpenid(
                session.getOpenid(),
                session.getUnionid(),
                null,
                resolveMerchantId(request.getMerchantId()),
                request.getActivationToken(),
                request.getInviteCode(),
                null
        );
    }

    @Transactional
    public LoginResponse devLogin(DevLoginRequest request) {
        if (!StringUtils.hasText(request.getOpenid())) {
            throw BusinessException.of(400, "openid 不能为空");
        }
        return loginByOpenid(
                request.getOpenid(),
                null,
                request.getNickname(),
                resolveMerchantId(request.getMerchantId()),
                request.getActivationToken(),
                request.getInviteCode(),
                request.getRole()
        );
    }

    private LoginResponse loginByOpenid(
            String openid,
            String unionid,
            String nickname,
            Long merchantId,
            String activationToken,
            String inviteCode,
            String roleOverride
    ) {
        validateMerchant(merchantId);
        MerchantContextHolder.set(merchantId);

        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getMerchantId, merchantId)
                .eq(User::getOpenid, openid));

        if (user == null) {
            user = new User();
            user.setMerchantId(merchantId);
            user.setOpenid(openid);
            user.setUnionid(unionid);
            user.setNickname(StringUtils.hasText(nickname) ? nickname : "微信用户");
            user.setRole(resolveInitialRole(openid, roleOverride, merchantId, activationToken));
            user.setStatus(resolveCustomerStatus(user.getRole(), null));
            user.setLastLoginAt(LocalDateTime.now());
            userMapper.insert(user);
        } else {
            if (StringUtils.hasText(unionid)) {
                user.setUnionid(unionid);
            }
            if (StringUtils.hasText(nickname)) {
                user.setNickname(nickname);
            }
            if (StringUtils.hasText(roleOverride)) {
                user.setRole(roleOverride);
                user.setStatus(resolveCustomerStatus(roleOverride, user.getCustomerId()));
            } else {
                maybeUpgradeToOwnerAdmin(user, openid, merchantId);
            }
            user.setLastLoginAt(LocalDateTime.now());
            userMapper.updateById(user);
        }

        if (StringUtils.hasText(activationToken)) {
            user = activationService.applyActivationToken(activationToken, merchantId, user);
        }

        if (StringUtils.hasText(inviteCode) && user.getCustomerId() == null
                && UserRole.CUSTOMER.name().equals(user.getRole())) {
            customerService.bindByInviteCodeForUser(user, inviteCode.trim());
            user = userMapper.selectById(user.getId());
        }

        if (UserRole.WORKER.name().equals(user.getRole())) {
            workerService.ensureWorkerProfile(user);
        }

        StpUtil.login(user.getId());
        bindSession(user);
        return buildLoginResponse(user);
    }

    private void bindSession(User user) {
        StpUtil.getSession().set("role", user.getRole());
        StpUtil.getSession().set("merchantId", user.getMerchantId());
        if (user.getCustomerId() != null) {
            StpUtil.getSession().set("customerId", user.getCustomerId());
        } else {
            StpUtil.getSession().delete("customerId");
        }
        if (UserRole.WORKER.name().equals(user.getRole())) {
            Long workerId = workerService.ensureWorkerProfile(user);
            if (workerId != null) {
                StpUtil.getSession().set("workerId", workerId);
            } else {
                StpUtil.getSession().delete("workerId");
            }
        } else {
            StpUtil.getSession().delete("workerId");
        }
        merchantContext.bindSessionMerchantId(user.getMerchantId());
    }

    private LoginResponse buildLoginResponse(User user) {
        String customerName = null;
        if (user.getCustomerId() != null) {
            Customer customer = customerMapper.selectById(user.getCustomerId());
            if (customer != null) {
                customerName = customer.getName();
            }
        }
        String merchantName = null;
        Merchant merchant = merchantMapper.selectById(user.getMerchantId());
        if (merchant != null) {
            merchantName = merchant.getName();
        }
        Long workerId = null;
        String workerCode = null;
        if (UserRole.WORKER.name().equals(user.getRole())) {
            Worker worker = workerService.getWorkerByUserId(user.getId());
            if (worker != null) {
                workerId = worker.getId();
                workerCode = worker.getWorkerCode();
            }
        }
        return LoginResponse.builder()
                .token(StpUtil.getTokenValue())
                .userId(user.getId())
                .role(user.getRole())
                .nickname(user.getNickname())
                .merchantId(user.getMerchantId())
                .merchantName(merchantName)
                .customerId(user.getCustomerId())
                .customerName(customerName)
                .openid(user.getOpenid())
                .workerId(workerId)
                .workerCode(workerCode)
                .build();
    }

    private String resolveCustomerStatus(String role, Long customerId) {
        if (!UserRole.CUSTOMER.name().equals(role)) {
            return "ENABLED";
        }
        return customerId != null ? "ENABLED" : "PENDING_BIND";
    }

    private String resolveInitialRole(String openid, String roleOverride, Long merchantId, String activationToken) {
        if (StringUtils.hasText(roleOverride)) {
            return roleOverride;
        }
        if (StringUtils.hasText(activationToken)) {
            return UserRole.CUSTOMER.name();
        }
        if (isWhitelistedAdmin(openid, merchantId)) {
            return UserRole.OWNER_ADMIN.name();
        }
        return UserRole.CUSTOMER.name();
    }

    private void maybeUpgradeToOwnerAdmin(User user, String openid, Long merchantId) {
        AppProperties.Admin admin = appProperties.getAdmin();
        if (!admin.isAutoUpgradeEnabled()) {
            return;
        }
        if (!isWhitelistedAdmin(openid, merchantId)) {
            return;
        }
        if (UserRole.OWNER_ADMIN.name().equals(user.getRole())) {
            return;
        }
        user.setRole(UserRole.OWNER_ADMIN.name());
    }

    private boolean isWhitelistedAdmin(String openid, Long merchantId) {
        Long defaultId = appProperties.getMerchant().getDefaultId();
        if (merchantId != null && !merchantId.equals(defaultId)) {
            return false;
        }
        return appProperties.getAdmin().getOpenidWhitelist().contains(openid);
    }

    private Long resolveMerchantId(Long merchantId) {
        return merchantId != null ? merchantId : appProperties.getMerchant().getDefaultId();
    }

    private void validateMerchant(Long merchantId) {
        Merchant merchant = merchantMapper.selectById(merchantId);
        if (merchant == null || merchant.getStatus() == null || merchant.getStatus() == 0) {
            throw BusinessException.of(400, "档口不存在或已停用");
        }
    }

    public LoginResponse currentUser() {
        long userId = StpUtil.getLoginIdAsLong();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.of(401, "用户不存在");
        }
        return buildLoginResponse(user);
    }

    public void logout() {
        StpUtil.logout();
    }
}
