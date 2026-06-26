package com.vwholesale.product.controller;

import com.vwholesale.common.response.ApiResponse;
import com.vwholesale.product.dto.InventoryProductVO;
import com.vwholesale.product.dto.InventoryStockUpdateRequest;
import com.vwholesale.product.service.InventoryStockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "老板端-库存管理")
@RestController
@RequestMapping("/api/boss/inventory")
@RequiredArgsConstructor
public class BossInventoryController {

    private final InventoryStockService inventoryStockService;

    @Operation(summary = "库存商品列表")
    @GetMapping("/products")
    public ApiResponse<List<InventoryProductVO>> products(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(inventoryStockService.listProducts(categoryId, keyword));
    }

    @Operation(summary = "更新实物库存")
    @PatchMapping("/products/{id}/stock")
    public ApiResponse<InventoryProductVO> updateStock(
            @PathVariable Long id,
            @Valid @RequestBody InventoryStockUpdateRequest request) {
        return ApiResponse.ok(inventoryStockService.updateStock(id, request));
    }
}
