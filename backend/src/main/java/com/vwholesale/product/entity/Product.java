package com.vwholesale.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("products")
public class Product {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long merchantId;
    private Long categoryId;
    private Long supplierId;
    private String name;
    private String aliases;
    private String unit;
    private String saleUnits;
    private String spec;
    private String imageUrl;
    private BigDecimal defaultPrice;
    private BigDecimal defaultPurchasePrice;
    private BigDecimal stockQty;
    private BigDecimal reservedQty;
    private String saleStatus;
    @TableLogic
    private Integer deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
