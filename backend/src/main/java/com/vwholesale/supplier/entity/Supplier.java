package com.vwholesale.supplier.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("suppliers")
public class Supplier {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long merchantId;
    private String supplierNo;
    private String name;
    private String contactName;
    private String phone;
    private String remark;
    private BigDecimal payableAmount;
    private BigDecimal paidAmount;
    private Integer status;
    @TableLogic
    private Integer deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
