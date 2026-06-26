package com.vwholesale.procurement.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProcurementSupplierOptionVO {

    private Long id;
    private String supplierNo;
    private String name;
}
