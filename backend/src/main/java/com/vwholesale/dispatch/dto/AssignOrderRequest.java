package com.vwholesale.dispatch.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AssignOrderRequest {

    @NotNull(message = "员工ID不能为空")
    private Long workerId;
}
