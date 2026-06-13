package com.vwholesale.dispatch.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class WorkerItemUpdateRequest {

    private BigDecimal actualQty;
    private Integer shortageFlag;
    private String pickRemark;
}
