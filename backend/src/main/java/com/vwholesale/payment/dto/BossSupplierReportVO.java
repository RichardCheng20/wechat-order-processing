package com.vwholesale.payment.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class BossSupplierReportVO {

    private SupplierReportSummary summary;
    private List<SupplierReportRow> rows;
}
