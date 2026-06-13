package com.vwholesale.customer.dto;

import lombok.Data;

@Data
public class CustomerUpdateRequest {

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
    private Integer status;
}
