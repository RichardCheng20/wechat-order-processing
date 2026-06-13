package com.vwholesale.dispatch.controller;

import com.vwholesale.common.response.ApiResponse;
import com.vwholesale.dispatch.dto.AssignOrderRequest;
import com.vwholesale.dispatch.dto.WorkerItemUpdateRequest;
import com.vwholesale.dispatch.dto.WorkerTaskVO;
import com.vwholesale.dispatch.service.DispatchService;
import com.vwholesale.worker.dto.WorkerVO;
import com.vwholesale.worker.service.WorkerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "老板端-派单")
@RestController
@RequestMapping("/api/boss")
@RequiredArgsConstructor
public class BossDispatchController {

    private final DispatchService dispatchService;
    private final WorkerService workerService;

    @Operation(summary = "工人列表")
    @GetMapping("/workers")
    public ApiResponse<List<WorkerVO>> workers() {
        return ApiResponse.ok(workerService.listForBoss());
    }

    @Operation(summary = "派单给工人")
    @PostMapping("/orders/{id}/assign")
    public ApiResponse<WorkerTaskVO> assign(@PathVariable Long id, @Valid @RequestBody AssignOrderRequest request) {
        return ApiResponse.ok(dispatchService.assignOrder(id, request.getWorkerId()));
    }
}
