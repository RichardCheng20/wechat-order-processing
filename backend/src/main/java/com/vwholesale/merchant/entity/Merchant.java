package com.vwholesale.merchant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("merchants")
public class Merchant {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String contactName;
    private String phone;
    private String region;
    private Integer status;
    /** 档口开通码，首个微信认领主管理员 */
    private String onboardingToken;
    private Long ownerUserId;
    private String dataPlatformPasswordHash;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
