package com.vwholesale.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("product_purchase_prices")
public class ProductPurchasePriceRecord {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long merchantId;
    private Long productId;
    private BigDecimal purchasePrice;
    private BigDecimal purchasedQty;
    private LocalDate effectiveDate;
    private Integer status;
    @TableLogic
    private Integer deleted;
    private LocalDateTime createdAt;
}
