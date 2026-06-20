package com.vwholesale.product.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CustomerQuoteSaveRequest {

    @NotEmpty(message = "请至少提交一条报价")
    @Valid
    private List<Item> items;

    @Data
    public static class Item {
        @NotNull
        private Long productId;
        private BigDecimal price;
    }
}
