package com.vwholesale.procurement.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ProcurementCustomerLineVO {

    private Long customerId;
    private String customerName;
    private BigDecimal totalQty;
    private Integer orderCount;
}
