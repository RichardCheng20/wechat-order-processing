package com.vwholesale.customer.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.vwholesale.auth.service.WechatClient;
import com.vwholesale.common.config.AppProperties;
import com.vwholesale.common.context.MerchantContext;
import com.vwholesale.common.exception.BusinessException;
import com.vwholesale.common.security.RoleChecker;
import com.vwholesale.customer.dto.CustomerRegisterInviteVO;
import com.vwholesale.customer.entity.MerchantCustomerRegisterToken;
import com.vwholesale.customer.mapper.MerchantCustomerRegisterTokenMapper;
import com.vwholesale.merchant.entity.Merchant;
import com.vwholesale.merchant.mapper.MerchantMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerRegisterInviteService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String TOKEN_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final MerchantCustomerRegisterTokenMapper registerTokenMapper;
    private final MerchantMapper merchantMapper;
    private final MerchantContext merchantContext;
    private final AppProperties appProperties;
    private final WechatClient wechatClient;

    @Transactional
    public CustomerRegisterInviteVO generateForBoss() {
        RoleChecker.requireBoss();
        Long merchantId = merchantContext.currentMerchantId();
        revokeActiveTokens(merchantId);

        MerchantCustomerRegisterToken row = new MerchantCustomerRegisterToken();
        row.setMerchantId(merchantId);
        row.setToken(generateUniqueToken());
        row.setExpiredAt(LocalDateTime.now().plusDays(appProperties.getCustomer().getInviteCodeExpireDays()));
        row.setCreatedBy(RoleChecker.currentUserId());
        registerTokenMapper.insert(row);

        return toVO(row);
    }

    public CustomerRegisterInviteVO currentForBoss() {
        RoleChecker.requireBoss();
        Long merchantId = merchantContext.currentMerchantId();
        MerchantCustomerRegisterToken row = findActiveToken(merchantId);
        if (row == null) {
            return null;
        }
        return toVO(row);
    }

    public MerchantCustomerRegisterToken validateToken(String rawToken, Long merchantId) {
        if (!StringUtils.hasText(rawToken)) {
            throw BusinessException.of(400, "注册邀请码不能为空");
        }
        if (merchantId == null) {
            merchantId = appProperties.getMerchant().getDefaultId();
        }
        Merchant merchant = merchantMapper.selectById(merchantId);
        if (merchant == null || merchant.getStatus() == null || merchant.getStatus() == 0) {
            throw BusinessException.of(400, "档口不存在或已停用");
        }

        String token = rawToken.trim().toUpperCase(Locale.ROOT);
        MerchantCustomerRegisterToken row = registerTokenMapper.selectOne(new LambdaQueryWrapper<MerchantCustomerRegisterToken>()
                .eq(MerchantCustomerRegisterToken::getToken, token)
                .eq(MerchantCustomerRegisterToken::getMerchantId, merchantId)
                .isNull(MerchantCustomerRegisterToken::getRevokedAt));
        if (row == null) {
            throw BusinessException.of(400, "注册邀请码无效");
        }
        if (row.getExpiredAt() != null && row.getExpiredAt().isBefore(LocalDateTime.now())) {
            throw BusinessException.of(400, "注册邀请码已过期，请联系老板重新生成");
        }
        return row;
    }

    private void revokeActiveTokens(Long merchantId) {
        registerTokenMapper.selectList(new LambdaQueryWrapper<MerchantCustomerRegisterToken>()
                        .eq(MerchantCustomerRegisterToken::getMerchantId, merchantId)
                        .isNull(MerchantCustomerRegisterToken::getRevokedAt))
                .forEach(row -> {
                    row.setRevokedAt(LocalDateTime.now());
                    registerTokenMapper.updateById(row);
                });
    }

    private MerchantCustomerRegisterToken findActiveToken(Long merchantId) {
        return registerTokenMapper.selectOne(new LambdaQueryWrapper<MerchantCustomerRegisterToken>()
                .eq(MerchantCustomerRegisterToken::getMerchantId, merchantId)
                .isNull(MerchantCustomerRegisterToken::getRevokedAt)
                .gt(MerchantCustomerRegisterToken::getExpiredAt, LocalDateTime.now())
                .orderByDesc(MerchantCustomerRegisterToken::getId)
                .last("LIMIT 1"));
    }

    private CustomerRegisterInviteVO toVO(MerchantCustomerRegisterToken row) {
        String loginPath = "pages/launch/index?m=" + row.getMerchantId() + "&r=" + row.getToken();
        String qrCodeBase64 = null;
        String qrErrorHint = null;
        try {
            byte[] png = wechatClient.createLaunchQrCode("m=" + row.getMerchantId() + ",r=" + row.getToken());
            if (png != null && png.length > 0) {
                qrCodeBase64 = Base64.getEncoder().encodeToString(png);
            } else {
                qrErrorHint = "微信未返回太阳码图片，请检查 AppID/AppSecret 与 env-version 配置";
            }
        } catch (BusinessException ex) {
            qrErrorHint = ex.getMessage();
            log.warn("create register wxacode failed: {}", ex.getMessage());
        } catch (Exception ex) {
            String message = ex.getMessage();
            qrErrorHint = StringUtils.hasText(message) ? message : "生成太阳码失败，请检查微信配置";
            log.warn("create register wxacode failed", ex);
        }
        String miniProgramName = appProperties.getWechat().getMiniProgramName();
        if (!StringUtils.hasText(miniProgramName)) {
            miniProgramName = "蔬菜批发";
        }
        return CustomerRegisterInviteVO.builder()
                .token(row.getToken())
                .expiredAt(row.getExpiredAt())
                .loginPath(loginPath)
                .miniProgramName(miniProgramName.trim())
                .qrCodeBase64(qrCodeBase64)
                .qrErrorHint(qrErrorHint)
                .build();
    }

    private String generateUniqueToken() {
        for (int i = 0; i < 10; i++) {
            String code = randomCode(8);
            Long count = registerTokenMapper.selectCount(new LambdaQueryWrapper<MerchantCustomerRegisterToken>()
                    .eq(MerchantCustomerRegisterToken::getToken, code));
            if (count == 0) {
                return code;
            }
        }
        throw BusinessException.of(500, "邀请码生成失败，请重试");
    }

    private String randomCode(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(TOKEN_CHARS.charAt(RANDOM.nextInt(TOKEN_CHARS.length())));
        }
        return sb.toString();
    }
}
