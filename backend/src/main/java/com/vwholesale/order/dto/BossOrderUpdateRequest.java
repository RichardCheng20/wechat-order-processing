package com.vwholesale.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class BossOrderUpdateRequest {

    /** 临时客户名称（仅未建档订单可改） */
    private String customerName;

    @NotEmpty(message = "请至少选择一件商品")
    @Valid
    private List<BossOrderItemCreateRequest> items;

    private String remark;

    private LocalDate deliveryDate;
}
