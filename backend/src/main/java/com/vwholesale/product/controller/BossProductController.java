package com.vwholesale.product.controller;

import com.vwholesale.common.response.ApiResponse;
import com.vwholesale.common.security.RoleChecker;
import com.vwholesale.product.dto.CategoryCreateRequest;
import com.vwholesale.product.dto.CategorySortRequest;
import com.vwholesale.product.dto.CategoryVO;
import com.vwholesale.product.dto.ProductCreateRequest;
import com.vwholesale.product.dto.ProductSaleStatusRequest;
import com.vwholesale.product.dto.ProductUpdateRequest;
import com.vwholesale.product.dto.ProductVO;
import com.vwholesale.product.service.ProductCategoryService;
import com.vwholesale.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;

@Tag(name = "老板端-商品")
@RestController
@RequestMapping("/api/boss")
@RequiredArgsConstructor
public class BossProductController {

    private final ProductCategoryService categoryService;
    private final ProductService productService;

    @Operation(summary = "商品分类列表")
    @GetMapping("/product-categories")
    public ApiResponse<List<CategoryVO>> categories() {
        RoleChecker.requireBoss();
        return ApiResponse.ok(categoryService.listEnabled());
    }

    @Operation(summary = "新建商品分类")
    @PostMapping("/product-categories")
    public ApiResponse<CategoryVO> createCategory(@Valid @RequestBody CategoryCreateRequest request) {
        RoleChecker.requireBoss();
        return ApiResponse.ok(categoryService.create(request));
    }

    @Operation(summary = "删除商品分类")
    @DeleteMapping("/product-categories/{id}")
    public ApiResponse<Void> deleteCategory(@PathVariable Long id) {
        RoleChecker.requireBoss();
        categoryService.delete(id);
        return ApiResponse.ok(null);
    }

    @Operation(summary = "调整分类排序")
    @PutMapping("/product-categories/sort")
    public ApiResponse<Void> sortCategories(@Valid @RequestBody CategorySortRequest request) {
        RoleChecker.requireBoss();
        categoryService.reorder(request);
        return ApiResponse.ok(null);
    }

    @Operation(summary = "商品列表")
    @GetMapping("/products")
    public ApiResponse<List<ProductVO>> products(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword) {
        RoleChecker.requireBoss();
        return ApiResponse.ok(productService.listForBoss(categoryId, keyword));
    }

    @Operation(summary = "按日期查询商品基础价（批量录价用）")
    @GetMapping("/products/daily-prices")
    public ApiResponse<Map<Long, BigDecimal>> dailyPrices(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        RoleChecker.requireBoss();
        return ApiResponse.ok(productService.listDailyBasePrices(date));
    }

    @Operation(summary = "新建商品")
    @PostMapping("/products")
    public ApiResponse<ProductVO> create(@Valid @RequestBody ProductCreateRequest request) {
        RoleChecker.requireBoss();
        return ApiResponse.ok(productService.create(request));
    }

    @Operation(summary = "更新商品")
    @PutMapping("/products/{id}")
    public ApiResponse<ProductVO> update(@PathVariable Long id, @RequestBody ProductUpdateRequest request) {
        RoleChecker.requireBoss();
        return ApiResponse.ok(productService.update(id, request));
    }

    @Operation(summary = "商品上下架")
    @PatchMapping("/products/{id}/sale-status")
    public ApiResponse<ProductVO> saleStatus(@PathVariable Long id, @RequestBody ProductSaleStatusRequest request) {
        RoleChecker.requireBoss();
        return ApiResponse.ok(productService.updateSaleStatus(id, request));
    }

    @Operation(summary = "删除商品")
    @DeleteMapping("/products/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        RoleChecker.requireBoss();
        productService.delete(id);
        return ApiResponse.ok(null);
    }
}
