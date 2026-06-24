package com.vwholesale.worker.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WorkerVO {

    private Long id;
    private Long userId;
    /** 配送员编号，如 PS000001 */
    private String workerCode;
    private String name;
    private String phone;
    private String jobRole;
    private Integer status;
    private Boolean bound;
    private String jobRoleLabel;
}
