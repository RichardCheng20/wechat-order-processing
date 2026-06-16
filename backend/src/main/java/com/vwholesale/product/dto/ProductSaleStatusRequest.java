package com.vwholesale.product.dto;

import lombok.Data;

import java.util.List;

@Data
public class ProductSaleStatusRequest {

    private String saleStatus;

    /** 上架时必填：可售单位列表，如 ["斤","个","包"] */
    private List<String> saleUnits;
}
