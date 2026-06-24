package com.vwholesale.order.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderItemCreateRequest {

    private Long productId;

    @NotNull(message = "数量不能为空")
    @DecimalMin(value = "0.01", message = "数量必须大于0")
    private BigDecimal orderQty;
    private String unit;
    private String pickRemark;
    /** 档口暂无、需老板配货的商品名称 */
    private String customName;
}
