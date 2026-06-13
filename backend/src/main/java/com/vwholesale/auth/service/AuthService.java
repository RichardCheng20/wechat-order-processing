package com.vwholesale.auth.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.vwholesale.auth.dto.DevLoginRequest;
import com.vwholesale.auth.dto.LoginResponse;
import com.vwholesale.common.config.AppProperties;
import com.vwholesale.common.context.MerchantContext;
import com.vwholesale.common.enums.UserRole;
import com.vwholesale.common.exception.BusinessException;
import com.vwholesale.user.entity.User;
import com.vwholesale.user.mapper.UserMapper;
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
    private final WechatClient wechatClient;
    private final AppProperties appProperties;
    private final MerchantContext merchantContext;
    private final WorkerService workerService;

    @Transactional
    public LoginResponse wechatLogin(String code) {
        WechatSessionResponse session = wechatClient.code2Session(code);
        return loginByOpenid(session.getOpenid(), session.getUnionid(), null);
    }

    /**
     * 本地开发用：模拟微信登录，无需真实 code2session。
     */
    @Transactional
    public LoginResponse devLogin(DevLoginRequest request) {
        if (!StringUtils.hasText(request.getOpenid())) {
            throw BusinessException.of(400, "openid 不能为空");
        }
        return loginByOpenid(request.getOpenid(), null, request.getNickname(), request.getRole());
    }

    private LoginResponse loginByOpenid(String openid, String unionid, String nickname) {
        return loginByOpenid(openid, unionid, nickname, null);
    }

    private LoginResponse loginByOpenid(String openid, String unionid, String nickname, String roleOverride) {
        Long merchantId = merchantContext.currentMerchantId();
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getOpenid, openid));

        if (user == null) {
            user = new User();
            user.setMerchantId(merchantId);
            user.setOpenid(openid);
            user.setUnionid(unionid);
            user.setNickname(StringUtils.hasText(nickname) ? nickname : "微信用户");
            user.setRole(resolveInitialRole(openid, roleOverride));
            user.setStatus(UserRole.CUSTOMER.name().equals(user.getRole()) ? "PENDING_BIND" : "ENABLED");
            user.setLastLoginAt(LocalDateTime.now());
            userMapper.insert(user);
            if (UserRole.WORKER.name().equals(user.getRole())) {
                workerService.ensureWorkerProfile(user);
            }
        } else {
            if (StringUtils.hasText(unionid)) {
                user.setUnionid(unionid);
            }
            if (StringUtils.hasText(nickname)) {
                user.setNickname(nickname);
            }
            if (StringUtils.hasText(roleOverride)) {
                user.setRole(roleOverride);
                user.setStatus(UserRole.CUSTOMER.name().equals(roleOverride) ? "PENDING_BIND" : "ENABLED");
            }
            maybeUpgradeToOwnerAdmin(user, openid);
            user.setLastLoginAt(LocalDateTime.now());
            userMapper.updateById(user);
        }

        StpUtil.login(user.getId());
        StpUtil.getSession().set("role", user.getRole());
        StpUtil.getSession().set("merchantId", user.getMerchantId());
        if (user.getCustomerId() != null) {
            StpUtil.getSession().set("customerId", user.getCustomerId());
        }
        if (UserRole.WORKER.name().equals(user.getRole())) {
            Long workerId = workerService.ensureWorkerProfile(user);
            if (workerId != null) {
                StpUtil.getSession().set("workerId", workerId);
            }
        }

        return LoginResponse.builder()
                .token(StpUtil.getTokenValue())
                .userId(user.getId())
                .role(user.getRole())
                .nickname(user.getNickname())
                .merchantId(user.getMerchantId())
                .customerId(user.getCustomerId())
                .openid(user.getOpenid())
                .build();
    }

    private String resolveInitialRole(String openid, String roleOverride) {
        if (StringUtils.hasText(roleOverride)) {
            return roleOverride;
        }
        if (isWhitelistedAdmin(openid)) {
            return UserRole.OWNER_ADMIN.name();
        }
        return UserRole.CUSTOMER.name();
    }

    private void maybeUpgradeToOwnerAdmin(User user, String openid) {
        AppProperties.Admin admin = appProperties.getAdmin();
        if (!admin.isAutoUpgradeEnabled()) {
            return;
        }
        if (!isWhitelistedAdmin(openid)) {
            return;
        }
        if (UserRole.OWNER_ADMIN.name().equals(user.getRole())) {
            return;
        }
        user.setRole(UserRole.OWNER_ADMIN.name());
    }

    private boolean isWhitelistedAdmin(String openid) {
        return appProperties.getAdmin().getOpenidWhitelist().contains(openid);
    }

    public LoginResponse currentUser() {
        long userId = StpUtil.getLoginIdAsLong();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.of(401, "用户不存在");
        }
        return LoginResponse.builder()
                .token(StpUtil.getTokenValue())
                .userId(user.getId())
                .role(user.getRole())
                .nickname(user.getNickname())
                .merchantId(user.getMerchantId())
                .customerId(user.getCustomerId())
                .openid(user.getOpenid())
                .build();
    }

    public void logout() {
        StpUtil.logout();
    }
}
