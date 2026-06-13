package com.vwholesale.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("orders")
public class Order {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long merchantId;
    private String orderNo;
    private Long customerId;
    private String source;
    private String status;
    private LocalDate deliveryDate;
    private String deliveryAddress;
    private String deliveryAddressShort;
    private String contactName;
    private String contactPhone;
    private Long assignedWorkerId;
    private BigDecimal amount;
    private BigDecimal paidAmount;
    private BigDecimal receivableAmount;
    private String remark;
    private Long createdBy;
    @TableLogic
    private Integer deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
