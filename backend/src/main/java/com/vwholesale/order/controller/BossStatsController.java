package com.vwholesale.order.controller;

import com.vwholesale.common.response.ApiResponse;
import com.vwholesale.merchant.service.DataPlatformPasswordService;
import com.vwholesale.order.dto.BossCustomerRankingVO;
import com.vwholesale.order.dto.BossCustomerReportVO;
import com.vwholesale.order.dto.BossProductRankingVO;
import com.vwholesale.order.dto.BossRevenueStatsVO;
import com.vwholesale.order.service.OrderService;
import com.vwholesale.payment.dto.BossPaymentStatsVO;
import com.vwholesale.payment.dto.BossSupplierReportVO;
import com.vwholesale.payment.service.PaymentService;
import com.vwholesale.payment.service.PurchasePaymentService;
import com.vwholesale.product.dto.BossInventoryReportVO;
import com.vwholesale.product.service.InventoryReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Tag(name = "老板端-数据统计")
@RestController
@RequestMapping("/api/boss/stats")
@RequiredArgsConstructor
public class BossStatsController {

    private final OrderService orderService;
    private final PaymentService paymentService;
    private final PurchasePaymentService purchasePaymentService;
    private final InventoryReportService inventoryReportService;
    private final DataPlatformPasswordService dataPlatformPasswordService;

    private void requireDataPlatformAccess(String password) {
        dataPlatformPasswordService.requirePassword(password);
    }

    @Operation(summary = "营收汇总（按日期区间）")
    @GetMapping("/revenue")
    public ApiResponse<BossRevenueStatsVO> revenue(
            @RequestHeader(value = "X-Data-Platform-Password", required = false) String password,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false, defaultValue = "DELIVERY") String dateType) {
        requireDataPlatformAccess(password);
        return ApiResponse.ok(orderService.revenueStats(dateFrom, dateTo, dateType));
    }

    @Operation(summary = "收款汇总（按日期区间）")
    @GetMapping("/payments")
    public ApiResponse<BossPaymentStatsVO> payments(
            @RequestHeader(value = "X-Data-Platform-Password", required = false) String password,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        requireDataPlatformAccess(password);
        return ApiResponse.ok(paymentService.paymentStats(dateFrom, dateTo));
    }

    @Operation(summary = "客户排行（按销售额）")
    @GetMapping("/customer-ranking")
    public ApiResponse<BossCustomerRankingVO> customerRanking(
            @RequestHeader(value = "X-Data-Platform-Password", required = false) String password,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false, defaultValue = "ORDER") String dateType) {
        requireDataPlatformAccess(password);
        return ApiResponse.ok(orderService.customerRanking(dateFrom, dateTo, dateType));
    }

    @Operation(summary = "客户报表（应收/已收）")
    @GetMapping("/customer-report")
    public ApiResponse<BossCustomerReportVO> customerReport(
            @RequestHeader(value = "X-Data-Platform-Password", required = false) String password,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false, defaultValue = "DELIVERY") String dateType) {
        requireDataPlatformAccess(password);
        return ApiResponse.ok(orderService.customerReport(dateFrom, dateTo, dateType));
    }

    @Operation(summary = "供应商报表（采购付款）")
    @GetMapping("/supplier-report")
    public ApiResponse<BossSupplierReportVO> supplierReport(
            @RequestHeader(value = "X-Data-Platform-Password", required = false) String password,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        requireDataPlatformAccess(password);
        return ApiResponse.ok(purchasePaymentService.supplierReport(dateFrom, dateTo));
    }

    @Operation(summary = "库存报表（进销存）")
    @GetMapping("/inventory-report")
    public ApiResponse<BossInventoryReportVO> inventoryReport(
            @RequestHeader(value = "X-Data-Platform-Password", required = false) String password,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false, defaultValue = "DELIVERY") String dateType) {
        requireDataPlatformAccess(password);
        return ApiResponse.ok(inventoryReportService.report(dateFrom, dateTo, dateType));
    }

    @Operation(summary = "商品排行（按销售额）")
    @GetMapping("/product-ranking")
    public ApiResponse<BossProductRankingVO> productRanking(
            @RequestHeader(value = "X-Data-Platform-Password", required = false) String password,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false, defaultValue = "ORDER") String dateType) {
        requireDataPlatformAccess(password);
        return ApiResponse.ok(orderService.productRanking(dateFrom, dateTo, dateType));
    }
}
