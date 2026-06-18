package com.vwholesale.supplier.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SupplierCreateRequest {

    @NotBlank(message = "供应商名称不能为空")
    private String name;
    private String contactName;
    private String phone;
    private String remark;
}
