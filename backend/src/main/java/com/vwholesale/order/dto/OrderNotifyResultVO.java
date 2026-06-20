package com.vwholesale.order.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class OrderNotifyResultVO {
    int notified;
    int failed;
    String message;
}
