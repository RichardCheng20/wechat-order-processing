package com.vwholesale.order.controller;

import com.vwholesale.common.response.ApiResponse;
import com.vwholesale.order.dto.OrderImportTextVO;
import com.vwholesale.order.service.OrderImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "客户端-订单导入")
@RestController
@RequestMapping("/api/customer")
@RequiredArgsConstructor
public class CustomerOrderImportController {

    private final OrderImportService orderImportService;

    @Operation(summary = "解析 Excel 订单文本")
    @PostMapping(value = "/orders/import/excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<OrderImportTextVO> importExcel(@RequestParam("file") MultipartFile file) {
        String text = orderImportService.parseExcel(file);
        return ApiResponse.ok(OrderImportTextVO.builder().text(text).build());
    }
}
