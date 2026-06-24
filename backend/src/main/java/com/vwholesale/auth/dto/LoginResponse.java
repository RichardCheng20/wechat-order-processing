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
    private String merchantName;
    private Long customerId;
    /** 已绑定客户档案名称 */
    private String customerName;
    /** 微信 openid，用于主管理员白名单配置 */
    private String openid;
    /** 配送员档案 ID */
    private Long workerId;
    /** 配送员编号，如 PS000001 */
    private String workerCode;
}
