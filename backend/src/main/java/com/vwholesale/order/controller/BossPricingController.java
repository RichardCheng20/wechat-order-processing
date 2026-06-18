package com.vwholesale.order.controller;

import com.vwholesale.common.response.ApiResponse;
import com.vwholesale.order.dto.OrderPricingSubmitRequest;
import com.vwholesale.order.dto.OrderPricingVO;
import com.vwholesale.order.dto.PricingProductDetailVO;
import com.vwholesale.order.dto.PricingProductSubmitRequest;
import com.vwholesale.order.dto.PricingProductSummaryVO;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
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

    @Operation(summary = "按商品汇总待录价")
    @GetMapping("/pricing/products")
    public ApiResponse<List<PricingProductSummaryVO>> pricingProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String priceFilter,
            @RequestParam(required = false) LocalDate deliveryFrom,
            @RequestParam(required = false) LocalDate deliveryTo) {
        return ApiResponse.ok(orderPricingService.listPricingProducts(keyword, priceFilter, deliveryFrom, deliveryTo));
    }

    @Operation(summary = "商品录价明细")
    @GetMapping("/pricing/products/{productId}")
    public ApiResponse<PricingProductDetailVO> productPricingDetail(
            @PathVariable Long productId,
            @RequestParam(required = false) String priceFilter,
            @RequestParam(required = false) LocalDate deliveryFrom,
            @RequestParam(required = false) LocalDate deliveryTo) {
        return ApiResponse.ok(orderPricingService.getProductPricingDetail(productId, deliveryFrom, deliveryTo, priceFilter));
    }

    @Operation(summary = "获取商品参考价")
    @PostMapping("/pricing/products/{productId}/fetch-prices")
    public ApiResponse<PricingProductDetailVO> fetchProductPrices(
            @PathVariable Long productId,
            @RequestParam(required = false) String priceFilter,
            @RequestParam(required = false) LocalDate deliveryFrom,
            @RequestParam(required = false) LocalDate deliveryTo) {
        return ApiResponse.ok(orderPricingService.fetchProductReferencePrices(productId, deliveryFrom, deliveryTo, priceFilter));
    }

    @Operation(summary = "提交商品录价")
    @PostMapping("/pricing/products/{productId}/submit")
    public ApiResponse<PricingProductDetailVO> submitProductPricing(
            @PathVariable Long productId,
            @RequestParam(required = false) LocalDate deliveryFrom,
            @RequestParam(required = false) LocalDate deliveryTo,
            @Valid @RequestBody PricingProductSubmitRequest request) {
        return ApiResponse.ok(orderPricingService.submitProductPricing(productId, request, deliveryFrom, deliveryTo));
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

    @Operation(summary = "推送给客户")
    @PostMapping("/orders/{id}/publish")
    public ApiResponse<OrderPricingVO> publish(@PathVariable Long id) {
        return ApiResponse.ok(orderPricingService.publishToCustomer(id));
    }
}
