package com.vwholesale.order.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class OrderVO {

    private Long id;
    private String orderNo;
    private Long customerId;
    private String customerName;
    private String source;
    private String status;
    private String statusLabel;
    private LocalDate deliveryDate;
    private String deliveryAddress;
    private String deliveryAddressShort;
    private String contactName;
    private String contactPhone;
    private BigDecimal amount;
    private String remark;
    private LocalDateTime createdAt;
    private List<OrderItemVO> items;
    private Integer itemCount;
    private Integer pickedItemCount;
    private BigDecimal paidAmount;
    private BigDecimal receivableAmount;
    private BigDecimal outstandingAmount;
    private Boolean priceIncomplete;
    private String sourceLabel;
    private String paymentStatusLabel;
    private Boolean printed;
    private String statementImageUrl;
    /** 该客户当前累计欠款（已对账/已完成未结清合计） */
    private BigDecimal customerOutstandingAmount;
    private Long assignedWorkerId;
    private String assignedWorkerName;
}
