package com.vwholesale.customer.dto;

import lombok.Builder;
import lombok.Data;

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
}
