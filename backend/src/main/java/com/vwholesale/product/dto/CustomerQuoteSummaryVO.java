package com.vwholesale.product.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CustomerQuoteSummaryVO {

    private Long customerId;
    private String customerName;
    private Integer quotedProductCount;
}
