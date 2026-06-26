package com.vwholesale.procurement.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ProcurementSupplierOrderLineVO {

    private Long id;
    private Long supplierId;
    private String supplierName;
    private String supplierNo;
    private BigDecimal purchasePrice;
    private BigDecimal purchasedQty;
    private BigDecimal lineAmount;
}
