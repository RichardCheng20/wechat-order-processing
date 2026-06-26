package com.vwholesale.product.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class InventoryReportSummary {

    /** 区间内采购入库合计 */
    private BigDecimal inboundQty;
    /** 区间内销售出库合计 */
    private BigDecimal outboundQty;
    /** 商品种类数 */
    private Integer productCount;
}
