package com.vwholesale.product.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CustomerQuoteLineVO {

    private Long productId;
    private String productName;
    private String unit;
    private Long categoryId;
    private String categoryName;
    private BigDecimal basePrice;
    private BigDecimal customerPrice;
    private Boolean hasQuote;
}
