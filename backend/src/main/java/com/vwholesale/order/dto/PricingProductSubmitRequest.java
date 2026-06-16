package com.vwholesale.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class PricingProductSubmitRequest {

    @NotEmpty(message = "请填写商品价格")
    @Valid
    private List<PricingItemRequest> items;
}
