package com.vwholesale.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class OrderCreateRequest {

    @NotEmpty(message = "请至少选择一件商品")
    @Valid
    private List<OrderItemCreateRequest> items;

    private String remark;
}
