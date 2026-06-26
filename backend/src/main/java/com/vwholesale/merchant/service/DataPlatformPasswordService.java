package com.vwholesale.merchant.service;

import com.vwholesale.common.context.MerchantContext;
import com.vwholesale.common.exception.BusinessException;
import com.vwholesale.common.security.RoleChecker;
import com.vwholesale.merchant.dto.DataPlatformPasswordSetRequest;
import com.vwholesale.merchant.dto.DataPlatformPasswordStatusVO;
import com.vwholesale.merchant.entity.Merchant;
import com.vwholesale.merchant.mapper.MerchantMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class DataPlatformPasswordService {

    private static final String PASSWORD_PATTERN = "\\d{6}";

    private final MerchantMapper merchantMapper;
    private final MerchantContext merchantContext;

    public DataPlatformPasswordStatusVO status() {
        RoleChecker.requireOwnerAdmin();
        Merchant merchant = getMerchantOrThrow();
        return DataPlatformPasswordStatusVO.builder()
                .passwordEnabled(isPasswordEnabled(merchant))
                .build();
    }

    public void verify(String password) {
        RoleChecker.requireOwnerAdmin();
        validateFormat(password);
        Merchant merchant = getMerchantOrThrow();
        if (!isPasswordEnabled(merchant)) {
            throw BusinessException.of(400, "尚未设置数据平台密码");
        }
        if (!matches(merchant, password)) {
            throw BusinessException.of(400, "密码错误");
        }
    }

    @Transactional
    public void setPassword(DataPlatformPasswordSetRequest request) {
        RoleChecker.requireOwnerAdmin();
        validateFormat(request.getPassword());
        Merchant merchant = getMerchantOrThrow();
        boolean enabled = isPasswordEnabled(merchant);
        if (enabled) {
            if (!StringUtils.hasText(request.getOldPassword())) {
                throw BusinessException.of(400, "请输入原密码");
            }
            validateFormat(request.getOldPassword());
            if (!matches(merchant, request.getOldPassword())) {
                throw BusinessException.of(400, "原密码错误");
            }
        }
        merchant.setDataPlatformPasswordHash(hash(merchant.getId(), request.getPassword()));
        merchantMapper.updateById(merchant);
    }

    @Transactional
    public void disablePassword(String oldPassword) {
        RoleChecker.requireOwnerAdmin();
        validateFormat(oldPassword);
        Merchant merchant = getMerchantOrThrow();
        if (!isPasswordEnabled(merchant)) {
            return;
        }
        if (!matches(merchant, oldPassword)) {
            throw BusinessException.of(400, "原密码错误");
        }
        merchant.setDataPlatformPasswordHash(null);
        merchantMapper.updateById(merchant);
    }

    /** 已登录老板忘记原密码时，凭当前账号身份直接重置（无需原密码、无短信验证） */
    @Transactional
    public void resetPassword(String password) {
        RoleChecker.requireOwnerAdmin();
        validateFormat(password);
        Merchant merchant = getMerchantOrThrow();
        if (!isPasswordEnabled(merchant)) {
            throw BusinessException.of(400, "尚未设置数据平台密码");
        }
        merchant.setDataPlatformPasswordHash(hash(merchant.getId(), password));
        merchantMapper.updateById(merchant);
    }

    public void requirePassword(String password) {
        RoleChecker.requireOwnerAdmin();
        Merchant merchant = getMerchantOrThrow();
        if (!isPasswordEnabled(merchant)) {
            throw BusinessException.of(403, "请先在设置中配置数据平台查看密码");
        }
        validateFormat(password);
        if (!matches(merchant, password)) {
            throw BusinessException.of(403, "数据平台密码错误");
        }
    }

    private Merchant getMerchantOrThrow() {
        Merchant merchant = merchantMapper.selectById(merchantContext.currentMerchantId());
        if (merchant == null) {
            throw BusinessException.of(404, "商户不存在");
        }
        return merchant;
    }

    private boolean isPasswordEnabled(Merchant merchant) {
        return StringUtils.hasText(merchant.getDataPlatformPasswordHash());
    }

    private void validateFormat(String password) {
        if (!StringUtils.hasText(password) || !password.matches(PASSWORD_PATTERN)) {
            throw BusinessException.of(400, "密码必须为 6 位数字");
        }
    }

    private boolean matches(Merchant merchant, String password) {
        if (!StringUtils.hasText(password) || !StringUtils.hasText(merchant.getDataPlatformPasswordHash())) {
            return false;
        }
        return merchant.getDataPlatformPasswordHash().equals(hash(merchant.getId(), password));
    }

    private String hash(Long merchantId, String password) {
        try {
            String raw = merchantId + ":" + password + ":vwholesale-data-platform";
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw BusinessException.of(500, "密码处理失败");
        }
    }
}
