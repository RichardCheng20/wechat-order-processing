package com.vwholesale.product.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CustomerQuoteDetailVO {

    private Long customerId;
    private String customerName;
    private List<CustomerQuoteLineVO> lines;
}
