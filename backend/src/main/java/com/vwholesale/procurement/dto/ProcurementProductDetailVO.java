package com.vwholesale.procurement.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class ProcurementProductDetailVO {

    private Long productId;
    private String productName;
    private String unit;
    private LocalDate receiveDate;
    private BigDecimal demandQty;
    private BigDecimal stockQty;
    private BigDecimal needQty;
    private BigDecimal purchasePrice;
    private BigDecimal purchasedQtyToday;
    private BigDecimal referencePurchasePrice;
    private Boolean priced;
    private List<ProcurementCustomerLineVO> customerLines;
}
