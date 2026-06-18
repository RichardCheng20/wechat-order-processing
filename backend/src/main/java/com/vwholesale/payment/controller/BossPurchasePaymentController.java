package com.vwholesale.payment.controller;

import com.vwholesale.common.response.ApiResponse;
import com.vwholesale.payment.dto.PurchasePaymentCreateRequest;
import com.vwholesale.payment.dto.PurchasePaymentVO;
import com.vwholesale.payment.service.PurchasePaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "老板端-采购付款")
@RestController
@RequestMapping("/api/boss")
@RequiredArgsConstructor
public class BossPurchasePaymentController {

    private final PurchasePaymentService purchasePaymentService;

    @Operation(summary = "登记采购付款")
    @PostMapping("/purchase-payments")
    public ApiResponse<PurchasePaymentVO> create(@Valid @RequestBody PurchasePaymentCreateRequest request) {
        return ApiResponse.ok(purchasePaymentService.create(request));
    }
}
