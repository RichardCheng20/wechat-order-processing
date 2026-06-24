package com.vwholesale.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("staff_activation_tokens")
public class StaffActivationToken {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long merchantId;
    private String token;
    private String targetRole;
    private Long workerId;
    private LocalDateTime expiredAt;
    private LocalDateTime usedAt;
    private Long usedByUserId;
    private Long createdBy;
    private LocalDateTime createdAt;
}
