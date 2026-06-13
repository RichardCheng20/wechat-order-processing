package com.vwholesale.order.controller;

import com.vwholesale.common.response.ApiResponse;
import com.vwholesale.order.dto.OrderVO;
import com.vwholesale.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Tag(name = "老板端-订单")
@RestController
@RequestMapping("/api/boss")
@RequiredArgsConstructor
public class BossOrderController {

    private final OrderService orderService;

    @Operation(summary = "今日订单概览")
    @GetMapping("/orders/summary")
    public ApiResponse<Map<String, Long>> summary() {
        return ApiResponse.ok(orderService.bossSummary());
    }

    @Operation(summary = "订单列表")
    @GetMapping("/orders")
    public ApiResponse<List<OrderVO>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean pricingPending) {
        return ApiResponse.ok(orderService.listForBoss(status, pricingPending));
    }

    @Operation(summary = "订单详情")
    @GetMapping("/orders/{id}")
    public ApiResponse<OrderVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(orderService.getDetail(id));
    }

    @Operation(summary = "确认订单")
    @PostMapping("/orders/{id}/confirm")
    public ApiResponse<OrderVO> confirm(@PathVariable Long id) {
        return ApiResponse.ok(orderService.confirmByBoss(id));
    }
}
