package com.vwholesale.auth.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class WechatSessionResponse {

    private String openid;
    private String unionid;

    @JsonProperty("session_key")
    private String sessionKey;

    @JsonProperty("errcode")
    private Integer errCode;

    @JsonProperty("errmsg")
    private String errMsg;

    public boolean isSuccess() {
        return openid != null && !openid.isBlank();
    }
}
