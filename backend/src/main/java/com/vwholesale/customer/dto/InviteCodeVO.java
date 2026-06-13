package com.vwholesale.customer.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class InviteCodeVO {

    private Long customerId;
    private String customerName;
    private String inviteCode;
    private LocalDateTime inviteExpiredAt;
}
