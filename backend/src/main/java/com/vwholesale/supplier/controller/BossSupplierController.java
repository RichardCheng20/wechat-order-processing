package com.vwholesale.supplier.controller;

import com.vwholesale.common.response.ApiResponse;
import com.vwholesale.supplier.dto.SupplierCreateRequest;
import com.vwholesale.supplier.dto.SupplierVO;
import com.vwholesale.supplier.service.SupplierService;
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

@Tag(name = "老板端-供应商")
@RestController
@RequestMapping("/api/boss")
@RequiredArgsConstructor
public class BossSupplierController {

    private final SupplierService supplierService;

    @Operation(summary = "供应商列表")
    @GetMapping("/suppliers")
    public ApiResponse<List<SupplierVO>> list(@RequestParam(required = false) String keyword) {
        return ApiResponse.ok(supplierService.listForBoss(keyword));
    }

    @Operation(summary = "新建供应商")
    @PostMapping("/suppliers")
    public ApiResponse<SupplierVO> create(@Valid @RequestBody SupplierCreateRequest request) {
        return ApiResponse.ok(supplierService.create(request));
    }
}
