package com.vwholesale.payment.controller;

import com.vwholesale.common.response.ApiResponse;
import com.vwholesale.payment.dto.FileUploadVO;
import com.vwholesale.payment.service.FileUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "老板端-文件")
@RestController
@RequestMapping("/api/boss")
@RequiredArgsConstructor
public class BossFileController {

    private final FileUploadService fileUploadService;

    @Operation(summary = "上传凭证图片")
    @PostMapping(value = "/files/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<FileUploadVO> upload(@RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(fileUploadService.upload(file));
    }
}
