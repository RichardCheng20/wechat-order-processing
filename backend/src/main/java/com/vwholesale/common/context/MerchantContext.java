package com.vwholesale.common.context;

import com.vwholesale.common.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 租户上下文。MVP 阶段固定 merchant_id = 1，后续从登录态解析。
 */
@Component
@RequiredArgsConstructor
public class MerchantContext {

    private final AppProperties appProperties;

    public Long currentMerchantId() {
        return appProperties.getMerchant().getDefaultId();
    }
}
