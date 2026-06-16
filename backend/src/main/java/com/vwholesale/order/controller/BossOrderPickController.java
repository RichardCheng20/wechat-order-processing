package com.vwholesale.order.controller;

import com.vwholesale.common.response.ApiResponse;
import com.vwholesale.order.dto.BossPickItemUpdateRequest;
import com.vwholesale.order.dto.OrderVO;
import com.vwholesale.order.service.OrderPickService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "老板端-分拣")
@RestController
@RequestMapping("/api/boss/orders")
@RequiredArgsConstructor
public class BossOrderPickController {

    private final OrderPickService orderPickService;

    @Operation(summary = "分拣详情")
    @GetMapping("/{id}/pick")
    public ApiResponse<OrderVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(orderPickService.getPickDetail(id));
    }

    @Operation(summary = "开始分拣")
    @PostMapping("/{id}/pick/start")
    public ApiResponse<OrderVO> start(@PathVariable Long id) {
        return ApiResponse.ok(orderPickService.startPick(id));
    }

    @Operation(summary = "更新分拣明细")
    @PatchMapping("/{id}/pick/items/{itemId}")
    public ApiResponse<OrderVO> updateItem(
            @PathVariable Long id,
            @PathVariable Long itemId,
            @RequestBody BossPickItemUpdateRequest request) {
        return ApiResponse.ok(orderPickService.updateItem(id, itemId, request));
    }

    @Operation(summary = "一键称重")
    @PostMapping("/{id}/pick/fill-qty")
    public ApiResponse<OrderVO> fillQty(@PathVariable Long id) {
        return ApiResponse.ok(orderPickService.fillActualQty(id));
    }

    @Operation(summary = "获取价格")
    @PostMapping("/{id}/pick/fetch-prices")
    public ApiResponse<OrderVO> fetchPrices(@PathVariable Long id) {
        return ApiResponse.ok(orderPickService.fetchPrices(id));
    }

    @Operation(summary = "完成分拣")
    @PostMapping("/{id}/pick/complete")
    public ApiResponse<OrderVO> complete(@PathVariable Long id) {
        return ApiResponse.ok(orderPickService.completePick(id));
    }
}
