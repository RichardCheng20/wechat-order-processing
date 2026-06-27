package com.vwholesale.customer.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CustomerRegisterInviteVO {

    private String token;
    private LocalDateTime expiredAt;
    /** 小程序启动路径，如 pages/launch/index?m=1&r=XXXX */
    private String loginPath;
    /** 小程序名称，展示在邀请二维码旁 */
    private String miniProgramName;
    /** 微信太阳码 Base64（未配置 AppSecret 或生成失败时为 null） */
    private String qrCodeBase64;
}
