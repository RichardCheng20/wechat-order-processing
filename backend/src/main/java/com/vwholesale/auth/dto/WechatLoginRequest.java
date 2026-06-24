package com.vwholesale.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class WechatLoginRequest {

    @NotBlank(message = "code 不能为空")
    private String code;

    /** 档口 ID，默认 1 */
    private Long merchantId;

    /** 员工/合伙人/档口开通激活码 */
    private String activationToken;

    /** 客户邀请码，登录后自动绑定 */
    private String inviteCode;
}
