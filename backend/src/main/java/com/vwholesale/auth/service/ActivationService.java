package com.vwholesale.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.vwholesale.auth.dto.ActivationTokenVO;
import com.vwholesale.auth.dto.LoginEntryPreviewVO;
import com.vwholesale.auth.entity.StaffActivationToken;
import com.vwholesale.auth.mapper.StaffActivationTokenMapper;
import com.vwholesale.common.config.AppProperties;
import com.vwholesale.common.context.MerchantContext;
import com.vwholesale.common.enums.UserRole;
import com.vwholesale.common.exception.BusinessException;
import com.vwholesale.common.security.RoleChecker;
import com.vwholesale.merchant.entity.Merchant;
import com.vwholesale.merchant.mapper.MerchantMapper;
import com.vwholesale.user.entity.User;
import com.vwholesale.user.mapper.UserMapper;
import com.vwholesale.worker.entity.Worker;
import com.vwholesale.worker.mapper.WorkerMapper;
import com.vwholesale.worker.support.PersonnelJobRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ActivationService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int TOKEN_EXPIRE_DAYS = 7;

    private final StaffActivationTokenMapper activationTokenMapper;
    private final WorkerMapper workerMapper;
    private final MerchantMapper merchantMapper;
    private final UserMapper userMapper;
    private final MerchantContext merchantContext;
    private final AppProperties appProperties;

    @Transactional
    public ActivationTokenVO createWorkerActivationToken(Long workerId) {
        RoleChecker.requireBoss();
        Long merchantId = merchantContext.currentMerchantId();
        Worker worker = workerMapper.selectOne(new LambdaQueryWrapper<Worker>()
                .eq(Worker::getId, workerId)
                .eq(Worker::getMerchantId, merchantId)
                .eq(Worker::getStatus, 1));
        if (worker == null) {
            throw BusinessException.of(404, "人员不存在或已停用");
        }
        if (worker.getUserId() != null) {
            throw BusinessException.of(400, "该人员已绑定微信，无需重复生成激活码");
        }
        String targetRole = PersonnelJobRole.isStallManager(worker.getJobRole())
                ? UserRole.STALL_MANAGER.name()
                : UserRole.WORKER.name();
        return createToken(merchantId, workerId, targetRole, worker.getName());
    }

    private ActivationTokenVO createToken(Long merchantId, Long workerId, String targetRole, String workerName) {
        invalidatePendingTokens(merchantId, workerId);
        StaffActivationToken token = new StaffActivationToken();
        token.setMerchantId(merchantId);
        token.setToken(generateToken());
        token.setTargetRole(targetRole);
        token.setWorkerId(workerId);
        token.setExpiredAt(LocalDateTime.now().plusDays(TOKEN_EXPIRE_DAYS));
        token.setCreatedBy(RoleChecker.currentUserId());
        activationTokenMapper.insert(token);
        return toVO(token, workerName);
    }

    private void invalidatePendingTokens(Long merchantId, Long workerId) {
        activationTokenMapper.selectList(new LambdaQueryWrapper<StaffActivationToken>()
                        .eq(StaffActivationToken::getMerchantId, merchantId)
                        .eq(StaffActivationToken::getWorkerId, workerId)
                        .isNull(StaffActivationToken::getUsedAt))
                .forEach(row -> {
                    row.setUsedAt(LocalDateTime.now());
                    activationTokenMapper.updateById(row);
                });
    }

    @Transactional
    public User applyActivationToken(String rawToken, Long merchantId, User user) {
        if (!StringUtils.hasText(rawToken)) {
            return user;
        }
        String tokenValue = rawToken.trim().toUpperCase(Locale.ROOT);
        Merchant merchant = getActiveMerchant(merchantId);

        if (StringUtils.hasText(merchant.getOnboardingToken())
                && merchant.getOnboardingToken().equalsIgnoreCase(tokenValue)) {
            return applyMerchantOnboarding(merchant, user);
        }

        StaffActivationToken token = activationTokenMapper.selectOne(new LambdaQueryWrapper<StaffActivationToken>()
                .eq(StaffActivationToken::getToken, tokenValue)
                .eq(StaffActivationToken::getMerchantId, merchantId));
        if (token == null) {
            throw BusinessException.of(400, "激活码无效");
        }
        if (token.getUsedAt() != null) {
            throw BusinessException.of(400, "激活码已使用，请联系管理员重新生成");
        }
        if (token.getExpiredAt() != null && token.getExpiredAt().isBefore(LocalDateTime.now())) {
            throw BusinessException.of(400, "激活码已过期，请联系管理员重新生成");
        }

        Worker worker = null;
        if (token.getWorkerId() != null) {
            worker = workerMapper.selectById(token.getWorkerId());
            if (worker == null || !merchantId.equals(worker.getMerchantId())) {
                throw BusinessException.of(400, "激活码关联人员不存在");
            }
            if (worker.getUserId() != null && !worker.getUserId().equals(user.getId())) {
                throw BusinessException.of(400, "该人员已绑定其他微信");
            }
            worker.setUserId(user.getId());
            if (StringUtils.hasText(user.getPhone())) {
                worker.setPhone(user.getPhone());
            }
            workerMapper.updateById(worker);
        }

        user.setRole(token.getTargetRole());
        user.setStatus("ENABLED");
        userMapper.updateById(user);

        token.setUsedAt(LocalDateTime.now());
        token.setUsedByUserId(user.getId());
        activationTokenMapper.updateById(token);
        return userMapper.selectById(user.getId());
    }

    private User applyMerchantOnboarding(Merchant merchant, User user) {
        if (merchant.getOwnerUserId() != null && !merchant.getOwnerUserId().equals(user.getId())) {
            throw BusinessException.of(400, "该档口已有主管理员，无法重复开通");
        }
        user.setRole(UserRole.OWNER_ADMIN.name());
        user.setStatus("ENABLED");
        userMapper.updateById(user);

        merchant.setOwnerUserId(user.getId());
        merchantMapper.updateById(merchant);
        return userMapper.selectById(user.getId());
    }

    public LoginEntryPreviewVO previewEntry(Long merchantId, String activationToken) {
        Merchant merchant = getActiveMerchant(merchantId);
        LoginEntryPreviewVO.LoginEntryPreviewVOBuilder builder = LoginEntryPreviewVO.builder()
                .merchantId(merchant.getId())
                .merchantName(merchant.getName());

        if (!StringUtils.hasText(activationToken)) {
            return builder.entryType("CUSTOMER")
                    .entryHint("登录后可选购下单；有邀请码可在「我的」中绑定客户档案")
                    .build();
        }

        String tokenValue = activationToken.trim().toUpperCase(Locale.ROOT);
        if (StringUtils.hasText(merchant.getOnboardingToken())
                && merchant.getOnboardingToken().equalsIgnoreCase(tokenValue)) {
            if (merchant.getOwnerUserId() != null) {
                return builder.entryType("NONE")
                        .entryHint("该档口已开通，请联系主管理员添加您的账号")
                        .build();
            }
            return builder.entryType("MERCHANT_ONBOARD")
                    .entryHint("首次开通档口，登录后将成为主管理员")
                    .build();
        }

        StaffActivationToken token = activationTokenMapper.selectOne(new LambdaQueryWrapper<StaffActivationToken>()
                .eq(StaffActivationToken::getToken, tokenValue)
                .eq(StaffActivationToken::getMerchantId, merchantId));
        if (token == null) {
            return builder.entryType("NONE").entryHint("激活码无效").build();
        }
        if (token.getUsedAt() != null) {
            return builder.entryType("NONE").entryHint("激活码已使用").build();
        }
        if (token.getExpiredAt() != null && token.getExpiredAt().isBefore(LocalDateTime.now())) {
            return builder.entryType("NONE").entryHint("激活码已过期").build();
        }

        String workerName = null;
        if (token.getWorkerId() != null) {
            Worker worker = workerMapper.selectById(token.getWorkerId());
            workerName = worker != null ? worker.getName() : null;
        }
        String roleLabel = UserRole.WORKER.name().equals(token.getTargetRole()) ? "员工" : "档口经理";
        return builder.entryType("STAFF")
                .entryHint("登录后将绑定为" + roleLabel)
                .activationRole(token.getTargetRole())
                .workerName(workerName)
                .build();
    }

    private Merchant getActiveMerchant(Long merchantId) {
        if (merchantId == null) {
            merchantId = appProperties.getMerchant().getDefaultId();
        }
        Merchant merchant = merchantMapper.selectById(merchantId);
        if (merchant == null || merchant.getStatus() == null || merchant.getStatus() == 0) {
            throw BusinessException.of(400, "档口不存在或已停用");
        }
        return merchant;
    }

    private ActivationTokenVO toVO(StaffActivationToken token, String workerName) {
        String roleLabel = UserRole.WORKER.name().equals(token.getTargetRole()) ? "员工" : "档口经理";
        return ActivationTokenVO.builder()
                .token(token.getToken())
                .targetRole(token.getTargetRole())
                .targetRoleLabel(roleLabel)
                .workerId(token.getWorkerId())
                .workerName(workerName)
                .expiredAt(token.getExpiredAt())
                .loginPath("pages/login/index?m=" + token.getMerchantId() + "&act=" + token.getToken())
                .build();
    }

    private String generateToken() {
        for (int i = 0; i < 20; i++) {
            String code = randomCode(8);
            Long count = activationTokenMapper.selectCount(new LambdaQueryWrapper<StaffActivationToken>()
                    .eq(StaffActivationToken::getToken, code));
            if (count == 0) {
                return code;
            }
        }
        throw BusinessException.of(500, "激活码生成失败，请重试");
    }

    private String randomCode(int length) {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(RANDOM.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
