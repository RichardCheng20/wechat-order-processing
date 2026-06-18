package com.vwholesale.procurement.controller;

import com.vwholesale.common.response.ApiResponse;
import com.vwholesale.procurement.dto.ProcurementProductDetailVO;
import com.vwholesale.procurement.dto.ProcurementPurchasePriceSubmitRequest;
import com.vwholesale.procurement.dto.ProcurementStockUpdateRequest;
import com.vwholesale.procurement.dto.ProcurementTaskVO;
import com.vwholesale.procurement.service.ProcurementTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Tag(name = "老板端-采购任务")
@RestController
@RequestMapping("/api/boss/procurement")
@RequiredArgsConstructor
public class BossProcurementController {

    private final ProcurementTaskService procurementTaskService;

    @Operation(summary = "采购任务（按商品汇总）")
    @GetMapping("/tasks")
    public ApiResponse<ProcurementTaskVO> tasks(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate receiveDate,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId) {
        return ApiResponse.ok(procurementTaskService.listTasks(receiveDate, keyword, categoryId));
    }

    @Operation(summary = "商品采购详情")
    @GetMapping("/products/{productId}")
    public ApiResponse<ProcurementProductDetailVO> productDetail(
            @PathVariable Long productId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate receiveDate) {
        return ApiResponse.ok(procurementTaskService.getProductDetail(productId, receiveDate));
    }

    @Operation(summary = "获取参考进价")
    @PostMapping("/products/{productId}/fetch-prices")
    public ApiResponse<ProcurementProductDetailVO> fetchPrices(
            @PathVariable Long productId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate receiveDate) {
        return ApiResponse.ok(procurementTaskService.fetchReferencePurchasePrice(productId, receiveDate));
    }

    @Operation(summary = "提交采购价")
    @PostMapping("/products/{productId}/submit-price")
    public ApiResponse<ProcurementProductDetailVO> submitPrice(
            @PathVariable Long productId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate receiveDate,
            @Valid @RequestBody ProcurementPurchasePriceSubmitRequest request) {
        return ApiResponse.ok(procurementTaskService.submitPurchasePrice(productId, receiveDate, request));
    }

    @Operation(summary = "更新商品库存")
    @PatchMapping("/products/{productId}/stock")
    public ApiResponse<ProcurementProductDetailVO> updateStock(
            @PathVariable Long productId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate receiveDate,
            @Valid @RequestBody ProcurementStockUpdateRequest request) {
        return ApiResponse.ok(procurementTaskService.updateStock(productId, receiveDate, request));
    }
}
