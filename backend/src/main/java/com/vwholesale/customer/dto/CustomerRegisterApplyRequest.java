package com.vwholesale.customer.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CustomerRegisterApplyRequest {

    @NotBlank(message = "邀请码不能为空")
    private String registerToken;

    @NotBlank(message = "客户名称不能为空")
    private String shopName;

    private String contactName;
    private String phone;
    private String address;
    private String addressShort;
}
