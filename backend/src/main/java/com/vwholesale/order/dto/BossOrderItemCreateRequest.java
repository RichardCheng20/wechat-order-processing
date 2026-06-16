package com.vwholesale.order.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class BossOrderItemCreateRequest {

    @NotNull(message = "商品ID不能为空")
    private Long productId;

    @NotNull(message = "数量不能为空")
    @DecimalMin(value = "0.01", message = "数量必须大于0")
    private BigDecimal orderQty;

    private String unit;

    @DecimalMin(value = "0", message = "单价不能为负数")
    private BigDecimal dealPrice;

    private String pickRemark;
}
