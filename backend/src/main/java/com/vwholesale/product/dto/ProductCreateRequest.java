package com.vwholesale.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductCreateRequest {

    @NotNull(message = "分类不能为空")
    private Long categoryId;

    @NotBlank(message = "商品名称不能为空")
    private String name;

    private String aliases;
    private String unit = "斤";
    private java.util.List<String> saleUnits;
    private String spec;
    private BigDecimal defaultPrice;
    private String saleStatus = "ON";
}
