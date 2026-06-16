package com.vwholesale.order.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class PricingProductLineVO {

    private Long itemId;
    private Long orderId;
    private String orderNo;
    private Long customerId;
    private String customerName;
    private LocalDate deliveryDate;
    private String orderRemark;
    private String pickRemark;
    private BigDecimal quantity;
    private String unit;
    private BigDecimal referencePrice;
    private BigDecimal dealPrice;
    private Boolean priced;
}
