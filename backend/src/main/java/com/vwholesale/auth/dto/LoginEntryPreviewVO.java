package com.vwholesale.auth.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginEntryPreviewVO {

    private Long merchantId;
    private String merchantName;
    /** MERCHANT_ONBOARD / STAFF / CUSTOMER / NONE */
    private String entryType;
    private String entryHint;
    private String activationRole;
    private String workerName;
}
