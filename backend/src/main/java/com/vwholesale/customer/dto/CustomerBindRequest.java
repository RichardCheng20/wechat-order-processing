package com.vwholesale.customer.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CustomerBindRequest {

    @NotBlank(message = "邀请码不能为空")
    private String inviteCode;
}
