package com.vwholesale.merchant.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MerchantOrderQrVO {

    private Long merchantId;
    private String merchantName;
    /** 小程序启动路径，如 pages/launch/index?m=1 */
    private String loginPath;
    private String miniProgramName;
    /** 微信太阳码 Base64（未配置 AppSecret 或生成失败时为 null） */
    private String qrCodeBase64;
    /** 太阳码未生成时的原因（便于联调排查） */
    private String qrErrorHint;
}
