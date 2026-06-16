package com.vwholesale.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class BossOrderCreateRequest {

    /** 已建档客户 ID，与 customerName 二选一 */
    private Long customerId;

    /** 临时客户名称（未建档），与 customerId 二选一 */
    private String customerName;

    @NotEmpty(message = "请至少选择一件商品")
    @Valid
    private List<BossOrderItemCreateRequest> items;

    private String remark;

    private LocalDate deliveryDate;
}
