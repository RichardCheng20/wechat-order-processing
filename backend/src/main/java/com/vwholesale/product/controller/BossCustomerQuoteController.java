package com.vwholesale.product.controller;

import com.vwholesale.common.response.ApiResponse;
import com.vwholesale.common.security.RoleChecker;
import com.vwholesale.product.dto.CustomerQuoteDetailVO;
import com.vwholesale.product.dto.CustomerQuoteSaveRequest;
import com.vwholesale.product.dto.CustomerQuoteSummaryVO;
import com.vwholesale.product.service.CustomerQuoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Tag(name = "老板端-报价单")
@RestController
@RequestMapping("/api/boss/customer-quotes")
@RequiredArgsConstructor
public class BossCustomerQuoteController {

    private final CustomerQuoteService customerQuoteService;

    @Operation(summary = "客户报价单列表")
    @GetMapping
    public ApiResponse<List<CustomerQuoteSummaryVO>> list(
            @RequestParam(required = false) String keyword) {
        RoleChecker.requireBoss();
        return ApiResponse.ok(customerQuoteService.listSummaries(keyword));
    }

    @Operation(summary = "客户报价单详情")
    @GetMapping("/{customerId}")
    public ApiResponse<CustomerQuoteDetailVO> detail(
            @PathVariable Long customerId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Boolean onlyQuoted) {
        RoleChecker.requireBoss();
        return ApiResponse.ok(customerQuoteService.getQuote(customerId, keyword, categoryId, onlyQuoted));
    }

    @Operation(summary = "保存客户报价")
    @PutMapping("/{customerId}")
    public ApiResponse<CustomerQuoteDetailVO> save(
            @PathVariable Long customerId,
            @Valid @RequestBody CustomerQuoteSaveRequest request) {
        RoleChecker.requireBoss();
        return ApiResponse.ok(customerQuoteService.saveQuote(customerId, request));
    }

    @Operation(summary = "从订单同步到报价单")
    @PostMapping("/sync-from-order/{orderId}")
    public ApiResponse<Map<String, Object>> syncFromOrder(@PathVariable Long orderId) {
        RoleChecker.requireBoss();
        int count = customerQuoteService.syncFromOrder(orderId);
        return ApiResponse.ok(Map.of("syncedCount", count));
    }
}
