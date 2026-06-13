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
    private Long assignedWorkerId;
    private String assignedWorkerName;
}
