package com.vwholesale.customer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("customer_bind_requests")
public class CustomerBindRequest {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long merchantId;
    private Long userId;
    private String shopName;
    private String contactName;
    private String phone;
    private String address;
    private String addressShort;
    private String status;
    private Long customerId;
    private String rejectReason;
    private Long reviewedBy;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
