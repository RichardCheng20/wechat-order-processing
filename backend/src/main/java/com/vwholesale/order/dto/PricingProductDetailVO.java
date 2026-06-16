package com.vwholesale.order.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class PricingProductDetailVO {

    private Long productId;
    private String productName;
    private String unit;
    private Integer orderCount;
    private BigDecimal totalQty;
    private List<PricingProductLineVO> lines;
}
