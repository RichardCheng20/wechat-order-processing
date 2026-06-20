package com.vwholesale.order.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderMarkPaymentRequest {

    @NotNull(message = "请输入收款金额")
    @DecimalMin(value = "0.01", message = "请输入有效金额")
    private BigDecimal amount;

    private BigDecimal discount;

    private String method;

    private String remark;

    private String statementImageUrl;
}
