package com.vwholesale.merchant.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BossProfileVO {

    private String merchantName;
    private String region;
    private String contactName;
    private String phone;
}
