package com.vwholesale.order.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PricingItemRequest {

    private Long itemId;
    private BigDecimal dealPrice;
}
