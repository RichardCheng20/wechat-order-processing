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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
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
    private final RestTemplate restTemplate = createBufferedRestTemplate();

    private static RestTemplate createBufferedRestTemplate() {
        RestTemplate template = new RestTemplate();
        template.setRequestFactory(new BufferingClientHttpRequestFactory(template.getRequestFactory()));
        return template;
    }

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
        AppProperties.Wechat wechat = appProperties.getWechat();
        if (!StringUtils.hasText(wechat.getAppSecret()) || wechat.getAppSecret().contains("在此填入")) {
            throw BusinessException.of(400, "未配置有效的微信小程序 AppSecret，请在 application-dev-local.yml 填入真实密钥");
        }
        String accessToken = getAccessToken();
        Map<String, Object> payload = new HashMap<>();
        payload.put("page", "pages/launch/index");
        payload.put("scene", scene);
        payload.put("check_path", false);
        payload.put("width", 430);
        if (StringUtils.hasText(wechat.getEnvVersion())) {
            payload.put("env_version", wechat.getEnvVersion().trim());
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.ALL));
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
        byte[] body;
        try {
            ResponseEntity<byte[]> response = restTemplate.postForEntity(
                    WXACODE_UNLIMIT_URL.replace("{accessToken}", accessToken),
                    entity,
                    byte[].class);
            body = response.getBody();
        } catch (RestClientException ex) {
            log.warn("create wxacode request failed", ex);
            throw BusinessException.of(400, "请求微信太阳码接口失败：" + ex.getMessage());
        }
        return parseWxacodeBody(body);
    }

    private byte[] parseWxacodeBody(byte[] body) {
        if (body == null || body.length == 0) {
            throw BusinessException.of(400, "微信太阳码响应为空，请检查 AppID/AppSecret 与 env-version 配置");
        }
        if (body[0] != '{') {
            return body;
        }
        try {
            JsonNode node = objectMapper.readTree(body);
            int errCode = node.path("errcode").asInt(0);
            String errMsg = node.path("errmsg").asText("未知错误");
            log.warn("create wxacode failed: {} ({})", errMsg, errCode);
            throw BusinessException.of(400, "微信太阳码生成失败：" + errMsg + " (" + errCode + ")");
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("create wxacode parse failed", ex);
            throw BusinessException.of(400, "微信太阳码响应解析失败");
        }
    }
}
