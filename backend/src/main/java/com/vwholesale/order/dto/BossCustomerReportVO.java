package com.vwholesale.order.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class BossCustomerReportVO {

    private CustomerReportSummary summary;
    private List<CustomerReportRow> rows;
}
