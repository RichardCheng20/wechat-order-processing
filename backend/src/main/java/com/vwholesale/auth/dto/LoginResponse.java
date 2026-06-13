package com.vwholesale.auth.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponse {

    private String token;
    private Long userId;
    private String role;
    private String nickname;
    private Long merchantId;
    private Long customerId;
    /** 微信 openid，用于主管理员白名单配置 */
    private String openid;
}
