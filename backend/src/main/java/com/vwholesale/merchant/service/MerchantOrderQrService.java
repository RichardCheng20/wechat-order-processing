package com.vwholesale.merchant.service;

import com.vwholesale.auth.service.WechatClient;
import com.vwholesale.common.config.AppProperties;
import com.vwholesale.common.context.MerchantContext;
import com.vwholesale.common.exception.BusinessException;
import com.vwholesale.common.security.RoleChecker;
import com.vwholesale.merchant.dto.MerchantOrderQrVO;
import com.vwholesale.merchant.entity.Merchant;
import com.vwholesale.merchant.mapper.MerchantMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Base64;

@Service
@RequiredArgsConstructor
@Slf4j
public class MerchantOrderQrService {

    private final MerchantMapper merchantMapper;
    private final MerchantContext merchantContext;
    private final AppProperties appProperties;
    private final WechatClient wechatClient;

    public MerchantOrderQrVO getForBoss() {
        RoleChecker.requireBoss();
        Long merchantId = merchantContext.currentMerchantId();
        Merchant merchant = merchantMapper.selectById(merchantId);
        if (merchant == null) {
            throw BusinessException.of(404, "商户不存在");
        }

        String loginPath = "pages/launch/index?m=" + merchantId;
        String qrCodeBase64 = null;
        String qrErrorHint = null;
        try {
            byte[] png = wechatClient.createLaunchQrCode("m=" + merchantId);
            if (png != null && png.length > 0) {
                qrCodeBase64 = Base64.getEncoder().encodeToString(png);
            } else {
                qrErrorHint = "微信未返回太阳码图片，请检查 AppID/AppSecret 与 env-version 配置";
            }
        } catch (BusinessException ex) {
            qrErrorHint = ex.getMessage();
            log.warn("create stall order wxacode failed: {}", ex.getMessage());
        } catch (Exception ex) {
            String message = ex.getMessage();
            qrErrorHint = StringUtils.hasText(message) ? message : "生成太阳码失败，请检查微信配置";
            log.warn("create stall order wxacode failed", ex);
        }

        String miniProgramName = appProperties.getWechat().getMiniProgramName();
        if (!StringUtils.hasText(miniProgramName)) {
            miniProgramName = "蔬菜批发";
        }

        return MerchantOrderQrVO.builder()
                .merchantId(merchantId)
                .merchantName(merchant.getName())
                .loginPath(loginPath)
                .miniProgramName(miniProgramName.trim())
                .qrCodeBase64(qrCodeBase64)
                .qrErrorHint(qrErrorHint)
                .build();
    }
}
