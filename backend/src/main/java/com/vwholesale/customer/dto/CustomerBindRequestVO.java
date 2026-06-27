package com.vwholesale.customer.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CustomerBindRequestVO {

    private Long id;
    private Long userId;
    private String shopName;
    private String contactName;
    private String phone;
    private String address;
    private String addressShort;
    private String status;
    private Long customerId;
    private String rejectReason;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
    /** 申请人微信昵称 */
    private String applicantNickname;
}
