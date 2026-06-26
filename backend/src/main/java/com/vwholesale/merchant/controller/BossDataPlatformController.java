package com.vwholesale.merchant.controller;

import com.vwholesale.common.response.ApiResponse;
import com.vwholesale.merchant.dto.DataPlatformPasswordSetRequest;
import com.vwholesale.merchant.dto.DataPlatformPasswordStatusVO;
import com.vwholesale.merchant.dto.DataPlatformPasswordVerifyRequest;
import com.vwholesale.merchant.service.DataPlatformPasswordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "老板端-数据平台密码")
@RestController
@RequestMapping("/api/boss/data-platform")
@RequiredArgsConstructor
public class BossDataPlatformController {

    private final DataPlatformPasswordService dataPlatformPasswordService;

    @Operation(summary = "数据平台密码状态")
    @GetMapping("/password/status")
    public ApiResponse<DataPlatformPasswordStatusVO> status() {
        return ApiResponse.ok(dataPlatformPasswordService.status());
    }

    @Operation(summary = "验证数据平台密码")
    @PostMapping("/password/verify")
    public ApiResponse<Void> verify(@Valid @RequestBody DataPlatformPasswordVerifyRequest request) {
        dataPlatformPasswordService.verify(request.getPassword());
        return ApiResponse.ok(null);
    }

    @Operation(summary = "设置或修改数据平台密码")
    @PutMapping("/password")
    public ApiResponse<Void> setPassword(@Valid @RequestBody DataPlatformPasswordSetRequest request) {
        dataPlatformPasswordService.setPassword(request);
        return ApiResponse.ok(null);
    }

    @Operation(summary = "关闭数据平台密码保护")
    @DeleteMapping("/password")
    public ApiResponse<Void> disablePassword(@Valid @RequestBody DataPlatformPasswordVerifyRequest request) {
        dataPlatformPasswordService.disablePassword(request.getPassword());
        return ApiResponse.ok(null);
    }

    @Operation(summary = "忘记原密码时重置（需已登录老板账号，无需原密码）")
    @PostMapping("/password/reset")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody DataPlatformPasswordVerifyRequest request) {
        dataPlatformPasswordService.resetPassword(request.getPassword());
        return ApiResponse.ok(null);
    }
}
