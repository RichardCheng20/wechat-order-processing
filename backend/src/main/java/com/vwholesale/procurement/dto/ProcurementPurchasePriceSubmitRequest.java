package com.vwholesale.procurement.dto;

import jakarta.validation.constraints.DecimalMin;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProcurementPurchasePriceSubmitRequest {

    @DecimalMin(value = "0", message = "采购价不能为负数")
    private BigDecimal purchasePrice;

    @DecimalMin(value = "0", message = "采购数量不能为负数")
    private BigDecimal purchasedQty;

    private Long supplierId;
}
