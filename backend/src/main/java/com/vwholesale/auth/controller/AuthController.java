package com.vwholesale.auth.controller;

import com.vwholesale.auth.dto.ActivationTokenVO;
import com.vwholesale.auth.dto.DevLoginRequest;
import com.vwholesale.auth.dto.LoginEntryPreviewVO;
import com.vwholesale.auth.dto.LoginResponse;
import com.vwholesale.auth.dto.WechatLoginRequest;
import com.vwholesale.auth.service.ActivationService;
import com.vwholesale.auth.service.AuthService;
import com.vwholesale.common.response.ApiResponse;
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

@Tag(name = "认证")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final ActivationService activationService;

    @Operation(summary = "登录入口预览（档口名、激活码说明）")
    @GetMapping("/entry-preview")
    public ApiResponse<LoginEntryPreviewVO> entryPreview(
            @RequestParam(required = false) Long merchantId,
            @RequestParam(required = false) String activationToken) {
        return ApiResponse.ok(activationService.previewEntry(merchantId, activationToken));
    }

    @Operation(summary = "微信小程序登录")
    @PostMapping("/wechat-login")
    public ApiResponse<LoginResponse> wechatLogin(@Valid @RequestBody WechatLoginRequest request) {
        return ApiResponse.ok(authService.wechatLogin(request));
    }

    @Operation(summary = "开发环境模拟登录（本地调试用）")
    @PostMapping("/dev-login")
    public ApiResponse<LoginResponse> devLogin(@RequestBody DevLoginRequest request) {
        return ApiResponse.ok(authService.devLogin(request));
    }

    @Operation(summary = "获取当前登录用户")
    @GetMapping("/me")
    public ApiResponse<LoginResponse> me() {
        return ApiResponse.ok(authService.currentUser());
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        authService.logout();
        return ApiResponse.ok();
    }
}
