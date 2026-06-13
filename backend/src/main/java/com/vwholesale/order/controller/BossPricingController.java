package com.vwholesale.order.controller;

import com.vwholesale.common.response.ApiResponse;
import com.vwholesale.order.dto.OrderPricingSubmitRequest;
import com.vwholesale.order.dto.OrderPricingVO;
import com.vwholesale.order.service.OrderPricingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "老板端-录价")
@RestController
@RequestMapping("/api/boss")
@RequiredArgsConstructor
public class BossPricingController {

    private final OrderPricingService orderPricingService;

    @Operation(summary = "待录价订单列表")
    @GetMapping("/orders/pending-price")
    public ApiResponse<List<OrderPricingVO>> pendingPrice() {
        return ApiResponse.ok(orderPricingService.listPendingPrice());
    }

    @Operation(summary = "录价详情")
    @GetMapping("/orders/{id}/pricing")
    public ApiResponse<OrderPricingVO> pricingDetail(@PathVariable Long id) {
        return ApiResponse.ok(orderPricingService.getPricingDetail(id));
    }

    @Operation(summary = "提交录价")
    @PostMapping("/orders/{id}/pricing")
    public ApiResponse<OrderPricingVO> submitPricing(
            @PathVariable Long id,
            @Valid @RequestBody OrderPricingSubmitRequest request) {
        return ApiResponse.ok(orderPricingService.submitPricing(id, request));
    }
}
