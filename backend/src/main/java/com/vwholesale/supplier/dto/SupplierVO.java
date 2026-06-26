package com.vwholesale.supplier.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class SupplierVO {

    private Long id;
    private String supplierNo;
    private String name;
    private String contactName;
    private String phone;
    private String remark;
    private Integer status;
    private LocalDateTime createdAt;
    private BigDecimal payableAmount;
    private BigDecimal paidAmount;
    /** 未结清应付 = 累计应付 - 累计已付 */
    private BigDecimal outstandingPayable;
}
