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
@TableName("product_prices")
public class ProductPriceRecord {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long merchantId;
    private Long productId;
    private Long customerId;
    private BigDecimal price;
    private LocalDate effectiveDate;
    private Integer status;
    private Long createdBy;
    @TableLogic
    private Integer deleted;
    private LocalDateTime createdAt;
}
