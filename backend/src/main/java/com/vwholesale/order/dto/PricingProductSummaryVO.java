package com.vwholesale.order.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PricingProductSummaryVO {

    private Long productId;
    private Boolean customItem;
    private String customName;
    private String productName;
    private String unit;
    private Integer pendingCount;
    private Integer totalCount;
    private BigDecimal totalQty;
    private Boolean allPriced;
}
