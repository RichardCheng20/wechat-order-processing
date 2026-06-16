package com.vwholesale.order.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class BossPickItemUpdateRequest {

    private BigDecimal actualQty;

    private BigDecimal dealPrice;

    private Integer shortageFlag;
}
