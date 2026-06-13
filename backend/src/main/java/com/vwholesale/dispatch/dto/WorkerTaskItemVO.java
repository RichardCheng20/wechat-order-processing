package com.vwholesale.dispatch.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class WorkerTaskItemVO {

    private Long id;
    private Long productId;
    private String productName;
    private BigDecimal orderQty;
    private BigDecimal actualQty;
    private String unit;
    private Integer shortageFlag;
    private String pickRemark;
}
