package com.vwholesale.merchant.controller;

import com.vwholesale.common.response.ApiResponse;
import com.vwholesale.merchant.dto.BossProfileUpdateRequest;
import com.vwholesale.merchant.dto.BossProfileVO;
import com.vwholesale.merchant.service.BossProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "老板端-个人信息")
@RestController
@RequestMapping("/api/boss")
@RequiredArgsConstructor
public class BossProfileController {

    private final BossProfileService bossProfileService;

    @Operation(summary = "获取个人信息")
    @GetMapping("/profile")
    public ApiResponse<BossProfileVO> profile() {
        return ApiResponse.ok(bossProfileService.getProfile());
    }

    @Operation(summary = "保存个人信息")
    @PutMapping("/profile")
    public ApiResponse<BossProfileVO> updateProfile(@Valid @RequestBody BossProfileUpdateRequest request) {
        return ApiResponse.ok(bossProfileService.updateProfile(request));
    }
}
