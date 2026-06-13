package com.vwholesale.product.controller;

import com.vwholesale.common.response.ApiResponse;
import com.vwholesale.common.security.RoleChecker;
import com.vwholesale.product.dto.CategoryVO;
import com.vwholesale.product.dto.ProductVO;
import com.vwholesale.product.service.ProductCategoryService;
import com.vwholesale.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "客户端-商品")
@RestController
@RequestMapping("/api/customer")
@RequiredArgsConstructor
public class CustomerProductController {

    private final ProductCategoryService categoryService;
    private final ProductService productService;

    @Operation(summary = "商品分类列表")
    @GetMapping("/product-categories")
    public ApiResponse<List<CategoryVO>> categories() {
        RoleChecker.requireCustomer();
        return ApiResponse.ok(categoryService.listEnabled());
    }

    @Operation(summary = "可下单商品列表（含参考价）")
    @GetMapping("/products")
    public ApiResponse<List<ProductVO>> products(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword) {
        RoleChecker.requireCustomer();
        Long customerId = RoleChecker.currentCustomerId();
        return ApiResponse.ok(productService.listForCustomer(categoryId, keyword, customerId));
    }
}
