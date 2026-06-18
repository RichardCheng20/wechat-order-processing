package com.vwholesale.payment.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class PaymentVO {

    private Long id;
    private Long customerId;
    private String customerName;
    private Long orderId;
    private BigDecimal amount;
    private String method;
    private LocalDateTime paidAt;
    private String remark;
    private List<String> voucherUrls;
    private LocalDateTime createdAt;
}
