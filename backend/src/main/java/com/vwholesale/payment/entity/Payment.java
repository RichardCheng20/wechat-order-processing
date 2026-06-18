package com.vwholesale.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("payments")
public class Payment {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long merchantId;
    private Long customerId;
    private Long orderId;
    private BigDecimal amount;
    private String method;
    private LocalDateTime paidAt;
    private Long operatorUserId;
    private String remark;
    private String voucherUrls;
    @TableLogic
    private Integer deleted;
    private LocalDateTime createdAt;
}
