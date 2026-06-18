package com.vwholesale.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PurchasePaymentCreateRequest {

    private Long supplierId;

    private String supplierName;

    @NotNull(message = "请输入记账金额")
    @DecimalMin(value = "0.01", message = "请输入有效金额")
    private BigDecimal amount;

    private LocalDateTime paidAt;

    private String method;

    private String remark;

    private List<String> voucherUrls;
}
