package com.vwholesale.supplier.controller;

import com.vwholesale.common.response.ApiResponse;
import com.vwholesale.supplier.dto.SupplierCreateRequest;
import com.vwholesale.supplier.dto.SupplierPurchaseLineVO;
import com.vwholesale.supplier.dto.SupplierUpdateRequest;
import com.vwholesale.supplier.dto.SupplierVO;
import com.vwholesale.procurement.service.SupplierPurchaseLineService;
import com.vwholesale.supplier.service.SupplierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "老板端-供应商")
@RestController
@RequestMapping("/api/boss")
@RequiredArgsConstructor
public class BossSupplierController {

    private final SupplierService supplierService;
    private final SupplierPurchaseLineService supplierPurchaseLineService;

    @Operation(summary = "供应商列表")
    @GetMapping("/suppliers")
    public ApiResponse<List<SupplierVO>> list(@RequestParam(required = false) String keyword) {
        return ApiResponse.ok(supplierService.listForBoss(keyword));
    }

    @Operation(summary = "供应商详情")
    @GetMapping("/suppliers/{id}")
    public ApiResponse<SupplierVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(supplierService.getById(id));
    }

    @Operation(summary = "新建供应商")
    @PostMapping("/suppliers")
    public ApiResponse<SupplierVO> create(@Valid @RequestBody SupplierCreateRequest request) {
        return ApiResponse.ok(supplierService.create(request));
    }

    @Operation(summary = "更新供应商")
    @PutMapping("/suppliers/{id}")
    public ApiResponse<SupplierVO> update(@PathVariable Long id, @RequestBody SupplierUpdateRequest request) {
        return ApiResponse.ok(supplierService.update(id, request));
    }

    @Operation(summary = "删除供应商")
    @DeleteMapping("/suppliers/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        supplierService.delete(id);
        return ApiResponse.ok(null);
    }

    @Operation(summary = "供应商采购明细汇总")
    @GetMapping("/suppliers/{id}/purchase-lines")
    public ApiResponse<List<SupplierPurchaseLineVO>> purchaseLines(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        return ApiResponse.ok(supplierPurchaseLineService.listForSupplier(id, dateFrom, dateTo));
    }
}
