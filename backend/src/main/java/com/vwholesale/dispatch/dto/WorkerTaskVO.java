package com.vwholesale.dispatch.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class WorkerTaskVO {

    private Long id;
    private String orderNo;
    private String customerName;
    private String merchantName;
    private String deliveryAddressShort;
    private String status;
    private String statusLabel;
    private LocalDate deliveryDate;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer itemCount;
    private List<WorkerTaskItemVO> items;
}
