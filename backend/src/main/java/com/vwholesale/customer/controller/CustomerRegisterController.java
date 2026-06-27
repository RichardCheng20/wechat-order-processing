package com.vwholesale.customer.controller;

import com.vwholesale.common.response.ApiResponse;
import com.vwholesale.customer.dto.CustomerBindRequestVO;
import com.vwholesale.customer.dto.CustomerRegisterApplyRequest;
import com.vwholesale.customer.dto.CustomerRegisterStatusVO;
import com.vwholesale.customer.service.CustomerBindRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "客户端-自助注册")
@RestController
@RequestMapping("/api/customer")
@RequiredArgsConstructor
public class CustomerRegisterController {

    private final CustomerBindRequestService customerBindRequestService;

    @Operation(summary = "自助注册申请状态")
    @GetMapping("/register-status")
    public ApiResponse<CustomerRegisterStatusVO> registerStatus() {
        return ApiResponse.ok(customerBindRequestService.registerStatus());
    }

    @Operation(summary = "提交自助注册绑定申请")
    @PostMapping("/register-apply")
    public ApiResponse<CustomerBindRequestVO> registerApply(@Valid @RequestBody CustomerRegisterApplyRequest request) {
        return ApiResponse.ok(customerBindRequestService.apply(request));
    }
}
