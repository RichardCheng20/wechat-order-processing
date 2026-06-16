package com.vwholesale.merchant.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BossProfileUpdateRequest {

    @NotBlank(message = "企业名称不能为空")
    private String merchantName;

    private String region;

    @NotBlank(message = "姓名不能为空")
    private String contactName;

    private String phone;
}
