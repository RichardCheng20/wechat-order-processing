package com.vwholesale.procurement.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ProcurementTaskItemVO {

    private Long productId;
    private Boolean customItem;
    private String customName;
    private String productName;
    private Long categoryId;
    private String unit;
    private BigDecimal demandQty;
    /** 可用库存 = 实物 - 占用 */
    private BigDecimal stockQty;
    private BigDecimal physicalStockQty;
    private BigDecimal reservedQty;
    private BigDecimal needQty;
    private BigDecimal purchasePrice;
    private BigDecimal totalAmount;
    private Integer orderCount;
    private Integer customerCount;
    private Boolean priced;
}
