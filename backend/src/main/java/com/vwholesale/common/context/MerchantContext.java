package com.vwholesale.common.context;

import cn.dev33.satoken.exception.NotWebContextException;
import cn.dev33.satoken.stp.StpUtil;
import com.vwholesale.common.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 租户上下文：优先 ThreadLocal（请求拦截器注入），其次登录 session，最后配置默认商户。
 */
@Component
@RequiredArgsConstructor
public class MerchantContext {

    private final AppProperties appProperties;

    public Long currentMerchantId() {
        Long fromHolder = MerchantContextHolder.get();
        if (fromHolder != null) {
            return fromHolder;
        }
        try {
            if (StpUtil.isLogin()) {
                Object merchantId = StpUtil.getSession().get("merchantId");
                if (merchantId != null) {
                    return Long.parseLong(merchantId.toString());
                }
            }
        } catch (NotWebContextException ignored) {
            // MQ / 定时任务等非 Web 线程：回退默认商户
        }
        return appProperties.getMerchant().getDefaultId();
    }

    public void bindSessionMerchantId(Long merchantId) {
        if (merchantId != null && StpUtil.isLogin()) {
            StpUtil.getSession().set("merchantId", merchantId);
        }
    }
}
