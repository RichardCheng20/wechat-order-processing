package com.vwholesale.order.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class OrderPricingItemVO {

    private Long id;
    private Long productId;
    private String productName;
    private BigDecimal orderQty;
    private BigDecimal actualQty;
    private String unit;
    private BigDecimal referencePrice;
    private BigDecimal dealPrice;
    private BigDecimal subtotalAmount;
    private Integer shortageFlag;
    private String pickRemark;
}
