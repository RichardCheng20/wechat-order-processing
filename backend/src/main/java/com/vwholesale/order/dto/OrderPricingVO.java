package com.vwholesale.order.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class OrderPricingVO {

    private Long id;
    private String orderNo;
    private Long customerId;
    private String customerName;
    private String status;
    private String statusLabel;
    private LocalDate deliveryDate;
    private String deliveryAddressShort;
    private BigDecimal amount;
    private String remark;
    private LocalDateTime createdAt;
    private Integer customItemCount;
    private List<OrderPricingItemVO> items;
}
