package com.vwholesale.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vwholesale.common.config.AppProperties;
import com.vwholesale.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class WechatClient {

    private static final String CODE2SESSION_URL =
            "https://api.weixin.qq.com/sns/jscode2session?appid={appid}&secret={secret}&js_code={code}&grant_type=authorization_code";

    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient = RestClient.create();

    public WechatSessionResponse code2Session(String code) {
        AppProperties.Wechat wechat = appProperties.getWechat();
        if (!StringUtils.hasText(wechat.getAppId()) || !StringUtils.hasText(wechat.getAppSecret())) {
            throw BusinessException.of(400, "未配置微信小程序 AppID/AppSecret，请使用开发登录接口或配置 WECHAT_APP_ID/WECHAT_APP_SECRET");
        }

        String body = restClient.get()
                .uri(CODE2SESSION_URL, wechat.getAppId(), wechat.getAppSecret(), code)
                .retrieve()
                .body(String.class);

        if (!StringUtils.hasText(body)) {
            throw BusinessException.of(400, "微信登录失败：空响应");
        }

        WechatSessionResponse response;
        try {
            response = objectMapper.readValue(body, WechatSessionResponse.class);
        } catch (Exception ex) {
            throw BusinessException.of(400, "微信登录失败：响应解析错误");
        }

        if (response == null || !response.isSuccess()) {
            String msg = response != null && response.getErrMsg() != null ? response.getErrMsg() : "微信登录失败";
            if (response != null && response.getErrCode() != null) {
                msg = msg + " (" + response.getErrCode() + ")";
            }
            throw BusinessException.of(400, msg);
        }
        return response;
    }
}
