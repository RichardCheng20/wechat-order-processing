package com.vwholesale.customer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("customers")
public class Customer {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long merchantId;
    private String name;
    private String contactName;
    private String phone;
    private String address;
    private String addressShort;
    private String defaultDeliveryTime;
    private String settlementType;
    private String priceLevel;
    private Integer autoConfirmOrder;
    private Long bindUserId;
    private String bindStatus;
    private String inviteCode;
    private LocalDateTime inviteExpiredAt;
    private String remark;
    private Integer status;
    @TableLogic
    private Integer deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
