package com.vwholesale.payment.controller;

import com.vwholesale.common.response.ApiResponse;
import com.vwholesale.payment.dto.PaymentCreateRequest;
import com.vwholesale.payment.dto.PaymentVO;
import com.vwholesale.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "老板端-收款")
@RestController
@RequestMapping("/api/boss")
@RequiredArgsConstructor
public class BossPaymentController {

    private final PaymentService paymentService;

    @Operation(summary = "登记销售收款")
    @PostMapping("/payments")
    public ApiResponse<PaymentVO> create(@Valid @RequestBody PaymentCreateRequest request) {
        return ApiResponse.ok(paymentService.create(request));
    }

    @Operation(summary = "收款记录")
    @GetMapping("/payments")
    public ApiResponse<List<PaymentVO>> list(@RequestParam(required = false) Long customerId) {
        return ApiResponse.ok(paymentService.listForBoss(customerId));
    }
}
