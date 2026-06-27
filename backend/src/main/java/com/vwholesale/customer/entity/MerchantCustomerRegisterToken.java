package com.vwholesale.customer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("merchant_customer_register_tokens")
public class MerchantCustomerRegisterToken {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long merchantId;
    private String token;
    private LocalDateTime expiredAt;
    private LocalDateTime revokedAt;
    private Long createdBy;
    private LocalDateTime createdAt;
}
