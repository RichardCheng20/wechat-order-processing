package com.vwholesale.customer.controller;

import com.vwholesale.common.response.ApiResponse;
import com.vwholesale.customer.dto.CustomerBindRequest;
import com.vwholesale.customer.dto.CustomerVO;
import com.vwholesale.customer.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "客户端-客户绑定")
@RestController
@RequestMapping("/api/customer")
@RequiredArgsConstructor
public class CustomerBindController {

    private final CustomerService customerService;

    @Operation(summary = "是否已绑定客户档案")
    @GetMapping("/bind-status")
    public ApiResponse<Map<String, Object>> bindStatus() {
        return ApiResponse.ok(customerService.bindStatus());
    }

    @Operation(summary = "邀请码绑定客户档案")
    @PostMapping("/bind")
    public ApiResponse<CustomerVO> bind(@Valid @RequestBody CustomerBindRequest request) {
        return ApiResponse.ok(customerService.bindByInviteCode(request));
    }
}
