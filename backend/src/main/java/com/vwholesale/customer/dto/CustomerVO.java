package com.vwholesale.customer.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class CustomerVO {

    private Long id;
    private String name;
    private String contactName;
    private String phone;
    private String address;
    private String addressShort;
    private String defaultDeliveryTime;
    private String settlementType;
    private String priceLevel;
    private Boolean autoConfirmOrder;
    private Long bindUserId;
    private String bindStatus;
    private String inviteCode;
    private LocalDateTime inviteExpiredAt;
    private String remark;
    private Integer status;
    private LocalDateTime createdAt;
    /** 未结清欠款合计（已完成订单应收 - 已付） */
    private BigDecimal outstandingAmount;
    /** 最近下单时间 */
    private LocalDateTime lastOrderAt;
}
