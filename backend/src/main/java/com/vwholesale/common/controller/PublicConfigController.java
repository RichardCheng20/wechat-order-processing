package com.vwholesale.common.controller;

import com.vwholesale.common.config.AppProperties;
import com.vwholesale.common.dto.MiniProgramConfigVO;
import com.vwholesale.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "公共配置")
@RestController
@RequestMapping("/api/config")
@RequiredArgsConstructor
public class PublicConfigController {

    private final AppProperties appProperties;

    @Operation(summary = "小程序公开配置")
    @GetMapping("/mini-program")
    public ApiResponse<MiniProgramConfigVO> miniProgram() {
        String templateId = appProperties.getWechat().getOrderNotifyTemplateId();
        return ApiResponse.ok(MiniProgramConfigVO.builder()
                .orderNotifyTemplateId(StringUtils.hasText(templateId) ? templateId.trim() : null)
                .build());
    }
}
