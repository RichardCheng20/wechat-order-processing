package com.vwholesale.order.controller;

import com.vwholesale.common.response.ApiResponse;
import com.vwholesale.order.dto.BossDashboardVO;
import com.vwholesale.order.dto.BossOrderCreateRequest;
import com.vwholesale.order.dto.BossOrderUpdateRequest;
import com.vwholesale.order.dto.OrderMarkPaymentRequest;
import com.vwholesale.order.dto.OrderVO;
import com.vwholesale.order.dto.UpdateOrderStatusRequest;
import com.vwholesale.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
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

    @Operation(summary = "经营数据统计")
    @GetMapping("/dashboard")
    public ApiResponse<BossDashboardVO> dashboard() {
        return ApiResponse.ok(orderService.bossDashboard());
    }

    @Operation(summary = "订单列表")
    @GetMapping("/orders")
    public ApiResponse<List<OrderVO>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean pricingPending,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String pickFilter,
            @RequestParam(required = false) String dateType,
            @RequestParam(required = false) LocalDate dateFrom,
            @RequestParam(required = false) LocalDate dateTo,
            @RequestParam(required = false) LocalDate deliveryFrom,
            @RequestParam(required = false) LocalDate deliveryTo,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String paymentFilter) {
        return ApiResponse.ok(orderService.listForBoss(status, pricingPending, keyword, pickFilter,
                dateType, dateFrom, dateTo, deliveryFrom, deliveryTo, customerId, paymentFilter));
    }

    @Operation(summary = "销售开单")
    @PostMapping("/orders")
    public ApiResponse<OrderVO> create(@Valid @RequestBody BossOrderCreateRequest request) {
        return ApiResponse.ok(orderService.createByBoss(request));
    }

    @Operation(summary = "订单详情")
    @GetMapping("/orders/{id}")
    public ApiResponse<OrderVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(orderService.getDetail(id));
    }

    @Operation(summary = "修改订单（明细/备注/临时客户名）")
    @PutMapping("/orders/{id}")
    public ApiResponse<OrderVO> update(@PathVariable Long id, @Valid @RequestBody BossOrderUpdateRequest request) {
        return ApiResponse.ok(orderService.updateByBoss(id, request));
    }

    @Operation(summary = "确认已交货")
    @PostMapping("/orders/{id}/confirm")
    public ApiResponse<OrderVO> confirm(@PathVariable Long id) {
        return ApiResponse.ok(orderService.confirmByBoss(id));
    }

    @Operation(summary = "标记配送单已打印")
    @PostMapping("/orders/{id}/print")
    public ApiResponse<OrderVO> markPrinted(@PathVariable Long id) {
        return ApiResponse.ok(orderService.markPrinted(id));
    }

    @Operation(summary = "标记订单支付")
    @PostMapping("/orders/{id}/payment")
    public ApiResponse<OrderVO> markPayment(@PathVariable Long id, @Valid @RequestBody OrderMarkPaymentRequest request) {
        return ApiResponse.ok(orderService.markPaymentByBoss(id, request));
    }

    @Operation(summary = "手动修改订单状态")
    @PatchMapping("/orders/{id}/status")
    public ApiResponse<OrderVO> updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateOrderStatusRequest request) {
        return ApiResponse.ok(orderService.updateStatusByBoss(id, request.getStatus()));
    }
}
