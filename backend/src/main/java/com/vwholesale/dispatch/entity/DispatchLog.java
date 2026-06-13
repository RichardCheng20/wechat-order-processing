package com.vwholesale.dispatch.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("dispatch_logs")
public class DispatchLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private Long fromWorkerId;
    private Long toWorkerId;
    private String action;
    private Long operatorUserId;
    private String remark;
    private LocalDateTime createdAt;
}
