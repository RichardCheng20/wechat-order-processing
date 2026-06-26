package com.vwholesale.procurement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("supplier_purchase_lines")
public class SupplierPurchaseLine {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long merchantId;
    private Long supplierId;
    private Long productId;
    private LocalDate effectiveDate;
    private BigDecimal purchasePrice;
    private BigDecimal purchasedQty;
    private Integer deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
