package com.vwholesale.procurement.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ProcurementTaskItemVO {

    private Long productId;
    private String productName;
    private Long categoryId;
    private String unit;
    private BigDecimal demandQty;
    private BigDecimal stockQty;
    private BigDecimal needQty;
    private BigDecimal purchasePrice;
    private BigDecimal totalAmount;
    private Integer orderCount;
    private Integer customerCount;
    private Boolean priced;
}
