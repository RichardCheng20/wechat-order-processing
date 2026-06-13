package com.vwholesale.worker.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WorkerVO {

    private Long id;
    private Long userId;
    private String name;
    private String phone;
    private Integer status;
}
