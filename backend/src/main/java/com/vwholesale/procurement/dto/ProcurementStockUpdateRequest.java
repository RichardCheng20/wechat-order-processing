package com.vwholesale.procurement.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProcurementStockUpdateRequest {

    @NotNull(message = "请填写库存")
    @DecimalMin(value = "0", message = "库存不能为负数")
    private BigDecimal stockQty;
}
