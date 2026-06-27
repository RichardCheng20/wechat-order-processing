package com.vwholesale.auth.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vwholesale.common.config.AppProperties;
import com.vwholesale.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class WechatClient {

    private static final String CODE2SESSION_URL =
            "https://api.weixin.qq.com/sns/jscode2session?appid={appid}&secret={secret}&js_code={code}&grant_type=authorization_code";
    private static final String ACCESS_TOKEN_URL =
            "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid={appid}&secret={secret}";
    private static final String SUBSCRIBE_SEND_URL =
            "https://api.weixin.qq.com/cgi-bin/message/subscribe/send?access_token={accessToken}";
    private static final String WXACODE_UNLIMIT_URL =
            "https://api.weixin.qq.com/wxa/getwxacodeunlimit?access_token={accessToken}";

    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient = RestClient.create();

    private String cachedAccessToken;
    private long accessTokenExpireAtMs;

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

    public synchronized String getAccessToken() {
        AppProperties.Wechat wechat = appProperties.getWechat();
        if (!StringUtils.hasText(wechat.getAppId()) || !StringUtils.hasText(wechat.getAppSecret())) {
            throw BusinessException.of(400, "未配置微信小程序 AppID/AppSecret");
        }
        long now = System.currentTimeMillis();
        if (StringUtils.hasText(cachedAccessToken) && now < accessTokenExpireAtMs) {
            return cachedAccessToken;
        }

        String body = restClient.get()
                .uri(ACCESS_TOKEN_URL, wechat.getAppId(), wechat.getAppSecret())
                .retrieve()
                .body(String.class);
        try {
            JsonNode node = objectMapper.readTree(body);
            if (node.hasNonNull("errcode") && node.get("errcode").asInt() != 0) {
                throw BusinessException.of(400, "获取微信 access_token 失败：" + node.path("errmsg").asText());
            }
            cachedAccessToken = node.path("access_token").asText();
            int expiresIn = node.path("expires_in").asInt(7200);
            accessTokenExpireAtMs = now + Math.max(60, expiresIn - 120) * 1000L;
            return cachedAccessToken;
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("parse access_token failed", ex);
            throw BusinessException.of(400, "获取微信 access_token 失败");
        }
    }

    public void sendSubscribeMessage(String openid, String templateId, String page,
                                     Map<String, Map<String, String>> data) {
        String accessToken = getAccessToken();
        Map<String, Object> payload = Map.of(
                "touser", openid,
                "template_id", templateId,
                "page", page,
                "miniprogram_state", "formal",
                "lang", "zh_CN",
                "data", data
        );
        String body = restClient.post()
                .uri(SUBSCRIBE_SEND_URL, accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(String.class);
        try {
            JsonNode node = objectMapper.readTree(body);
            int errCode = node.path("errcode").asInt(0);
            if (errCode != 0) {
                String errMsg = node.path("errmsg").asText("发送失败");
                throw BusinessException.of(400, errMsg + " (" + errCode + ")");
            }
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("parse subscribe send response failed", ex);
            throw BusinessException.of(400, "发送微信订阅消息失败");
        }
    }

    /** 生成小程序启动页太阳码；scene 最长 32 字符，如 m=1,r=ABCD1234 */
    public byte[] createLaunchQrCode(String scene) {
        if (!StringUtils.hasText(scene) || scene.length() > 32) {
            throw BusinessException.of(400, "二维码场景参数无效");
        }
        String accessToken = getAccessToken();
        AppProperties.Wechat wechat = appProperties.getWechat();
        Map<String, Object> payload = new HashMap<>();
        payload.put("page", "pages/launch/index");
        payload.put("scene", scene);
        payload.put("check_path", false);
        payload.put("width", 430);
        payload.put("auto_color", true);
        if (StringUtils.hasText(wechat.getEnvVersion())) {
            payload.put("env_version", wechat.getEnvVersion().trim());
        }
        byte[] body = restClient.post()
                .uri(WXACODE_UNLIMIT_URL, accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(byte[].class);
        if (body == null || body.length == 0) {
            return null;
        }
        if (body[0] == '{') {
            try {
                JsonNode node = objectMapper.readTree(body);
                int errCode = node.path("errcode").asInt(0);
                if (errCode != 0) {
                    log.warn("create wxacode failed: {}", node.path("errmsg").asText());
                    return null;
                }
            } catch (Exception ex) {
                log.warn("create wxacode parse failed", ex);
                return null;
            }
        }
        return body;
    }
}
