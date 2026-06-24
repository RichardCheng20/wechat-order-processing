package com.vwholesale.common.config;

import cn.dev33.satoken.stp.StpUtil;
import com.vwholesale.common.context.MerchantContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class MerchantContextInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (StpUtil.isLogin()) {
            Object merchantId = StpUtil.getSession().get("merchantId");
            if (merchantId != null) {
                MerchantContextHolder.set(Long.parseLong(merchantId.toString()));
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        MerchantContextHolder.clear();
    }
}
