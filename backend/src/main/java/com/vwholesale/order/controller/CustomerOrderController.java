package com.vwholesale.order.controller;

import com.vwholesale.common.response.ApiResponse;
import com.vwholesale.order.dto.OrderCreateRequest;
import com.vwholesale.order.dto.OrderVO;
import com.vwholesale.order.service.OrderService;
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

@Tag(name = "客户端-订单")
@RestController
@RequestMapping("/api/customer")
@RequiredArgsConstructor
public class CustomerOrderController {

    private final OrderService orderService;

    @Operation(summary = "提交订单")
    @PostMapping("/orders")
    public ApiResponse<OrderVO> create(@Valid @RequestBody OrderCreateRequest request) {
        return ApiResponse.ok(orderService.createByCustomer(request));
    }

    @Operation(summary = "我的订单列表")
    @GetMapping("/orders")
    public ApiResponse<List<OrderVO>> list() {
        return ApiResponse.ok(orderService.listForCustomer());
    }

    @Operation(summary = "订单详情")
    @GetMapping("/orders/{id}")
    public ApiResponse<OrderVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(orderService.getDetail(id));
    }
}
