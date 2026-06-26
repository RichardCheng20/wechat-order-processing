package com.vwholesale.product.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class BossInventoryReportVO {

    private InventoryReportSummary summary;
    private List<InventoryReportRow> rows;
}
