package com.vwholesale.merchant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class DataPlatformPasswordVerifyRequest {

    @NotBlank(message = "请输入密码")
    @Pattern(regexp = "\\d{6}", message = "密码必须为 6 位数字")
    private String password;
}
