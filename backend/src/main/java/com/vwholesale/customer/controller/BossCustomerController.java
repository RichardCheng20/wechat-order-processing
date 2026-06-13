package com.vwholesale.customer.controller;

import com.vwholesale.common.response.ApiResponse;
import com.vwholesale.customer.dto.CustomerCreateRequest;
import com.vwholesale.customer.dto.CustomerUpdateRequest;
import com.vwholesale.customer.dto.CustomerVO;
import com.vwholesale.customer.dto.InviteCodeVO;
import com.vwholesale.customer.service.CustomerService;
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

@Tag(name = "老板端-客户")
@RestController
@RequestMapping("/api/boss")
@RequiredArgsConstructor
public class BossCustomerController {

    private final CustomerService customerService;

    @Operation(summary = "客户列表")
    @GetMapping("/customers")
    public ApiResponse<List<CustomerVO>> list(@RequestParam(required = false) String keyword) {
        return ApiResponse.ok(customerService.listForBoss(keyword));
    }

    @Operation(summary = "客户详情")
    @GetMapping("/customers/{id}")
    public ApiResponse<CustomerVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(customerService.getById(id));
    }

    @Operation(summary = "新建客户")
    @PostMapping("/customers")
    public ApiResponse<CustomerVO> create(@Valid @RequestBody CustomerCreateRequest request) {
        return ApiResponse.ok(customerService.create(request));
    }

    @Operation(summary = "更新客户")
    @PutMapping("/customers/{id}")
    public ApiResponse<CustomerVO> update(@PathVariable Long id, @RequestBody CustomerUpdateRequest request) {
        return ApiResponse.ok(customerService.update(id, request));
    }

    @Operation(summary = "生成邀请码")
    @PostMapping("/customers/{id}/invite")
    public ApiResponse<InviteCodeVO> invite(@PathVariable Long id) {
        return ApiResponse.ok(customerService.generateInviteCode(id));
    }
}
