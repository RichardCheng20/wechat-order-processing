package com.vwholesale.customer.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CustomerCreateRequest {

    @NotBlank(message = "客户名称不能为空")
    private String name;
    private String contactName;
    private String phone;
    private String address;
    private String addressShort;
    private String defaultDeliveryTime;
    private String settlementType;
    private String priceLevel;
    private Boolean autoConfirmOrder;
    private String remark;
}
