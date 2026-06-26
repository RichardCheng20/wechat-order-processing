package com.vwholesale.supplier.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class SupplierPurchaseLineVO {

    private Long id;
    private Long productId;
    private String productName;
    private String unit;
    private LocalDate effectiveDate;
    private BigDecimal purchasePrice;
    private BigDecimal purchasedQty;
    private BigDecimal lineAmount;
}
