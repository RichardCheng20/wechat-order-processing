package com.vwholesale.procurement.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class ProcurementTaskVO {

    private LocalDate receiveDate;
    private BigDecimal totalNeedQty;
    private BigDecimal totalAmount;
    private Integer productCount;
    private List<ProcurementTaskItemVO> items;
}
