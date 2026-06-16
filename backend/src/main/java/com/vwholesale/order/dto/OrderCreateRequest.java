package com.vwholesale.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class OrderCreateRequest {

    /** 未绑定客户档案时必填：店铺/客户名称 */
    private String customerName;

    @NotEmpty(message = "请至少选择一件商品")
    @Valid
    private List<OrderItemCreateRequest> items;

    private String remark;
}
