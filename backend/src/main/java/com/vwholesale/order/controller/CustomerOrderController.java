package com.vwholesale.order.controller;

import com.vwholesale.common.response.ApiResponse;
import com.vwholesale.order.dto.OrderCreateRequest;
import com.vwholesale.order.dto.OrderNotifyResultVO;
import com.vwholesale.order.dto.OrderVO;
import com.vwholesale.order.service.OrderNotifyService;
import com.vwholesale.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "客户端-订单")
@RestController
@RequestMapping("/api/customer")
@RequiredArgsConstructor
public class CustomerOrderController {

    private final OrderService orderService;
    private final OrderNotifyService orderNotifyService;

    @Operation(summary = "提交订单")
    @PostMapping("/orders")
    public ApiResponse<OrderVO> create(@Valid @RequestBody OrderCreateRequest request) {
        return ApiResponse.ok(orderService.createByCustomer(request));
    }

    @Operation(summary = "我的订单列表")
    @GetMapping("/orders")
    public ApiResponse<List<OrderVO>> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false, defaultValue = "PROCESSING") String tabFilter) {
        return ApiResponse.ok(orderService.listForCustomer(dateFrom, dateTo, tabFilter));
    }

    @Operation(summary = "订单详情")
    @GetMapping("/orders/{id}")
    public ApiResponse<OrderVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(orderService.getDetail(id));
    }

    @Operation(summary = "微信提醒老板处理订单")
    @PostMapping("/orders/{id}/notify-boss")
    public ApiResponse<OrderNotifyResultVO> notifyBoss(@PathVariable Long id) {
        return ApiResponse.ok(orderNotifyService.notifyBossByCustomer(id));
    }
}
