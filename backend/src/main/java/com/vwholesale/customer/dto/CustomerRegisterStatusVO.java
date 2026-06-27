package com.vwholesale.customer.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CustomerRegisterStatusVO {

    private boolean bound;
    private Long customerId;
    private String customerName;
    /** 是否有待审核申请 */
    private boolean pendingReview;
    private Long pendingRequestId;
    /** 最近一次申请状态：PENDING/APPROVED/REJECTED */
    private String lastRequestStatus;
    private String rejectReason;
    private LocalDateTime submittedAt;
}
