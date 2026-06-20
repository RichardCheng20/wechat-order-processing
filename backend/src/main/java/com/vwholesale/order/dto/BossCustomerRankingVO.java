package com.vwholesale.order.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class BossCustomerRankingVO {

    private List<CustomerRankingRow> rows;
}
