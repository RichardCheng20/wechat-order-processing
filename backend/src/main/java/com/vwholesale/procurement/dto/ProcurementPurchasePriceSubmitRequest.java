package com.vwholesale.procurement.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProcurementPurchasePriceSubmitRequest {

    @NotNull(message = "请填写采购价")
    @DecimalMin(value = "0", message = "采购价不能为负数")
    private BigDecimal purchasePrice;

    @DecimalMin(value = "0", message = "采购数量不能为负数")
    private BigDecimal purchasedQty;
}
