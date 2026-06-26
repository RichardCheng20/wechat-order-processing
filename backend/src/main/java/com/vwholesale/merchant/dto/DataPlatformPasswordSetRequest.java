package com.vwholesale.merchant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class DataPlatformPasswordSetRequest {

    @NotBlank(message = "请输入新密码")
    @Pattern(regexp = "\\d{6}", message = "密码必须为 6 位数字")
    private String password;

    /** 修改密码时必填 */
    @Pattern(regexp = "\\d{6}", message = "原密码必须为 6 位数字")
    private String oldPassword;
}
