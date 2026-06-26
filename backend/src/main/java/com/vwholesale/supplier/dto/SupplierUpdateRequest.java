package com.vwholesale.supplier.dto;

import lombok.Data;

@Data
public class SupplierUpdateRequest {

    private String name;
    private String contactName;
    private String phone;
    private String remark;
    private Integer status;
}
