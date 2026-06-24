package com.vwholesale.auth.dto;

import lombok.Data;

@Data
public class DevLoginRequest {

    private String openid;
    private String nickname;
    private String role;
    private Long merchantId;
    private String activationToken;
    private String inviteCode;
}
