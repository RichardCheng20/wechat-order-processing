package com.vwholesale.worker.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("workers")
public class Worker {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long merchantId;
    private Long userId;
    /** 配送员编号，如 PS000001 */
    private String workerCode;
    private String name;
    private String phone;
    private String jobRole;
    private Integer status;
    private String remark;
    @TableLogic
    private Integer deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
