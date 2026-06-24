package com.vwholesale.dispatch.controller;

import com.vwholesale.common.response.ApiResponse;
import com.vwholesale.dispatch.dto.WorkerItemUpdateRequest;
import com.vwholesale.dispatch.dto.WorkerTaskVO;
import com.vwholesale.dispatch.service.DispatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "员工端-任务")
@RestController
@RequestMapping("/api/worker")
@RequiredArgsConstructor
public class WorkerTaskController {

    private final DispatchService dispatchService;

    @Operation(summary = "我的任务列表")
    @GetMapping("/tasks")
    public ApiResponse<List<WorkerTaskVO>> tasks(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        return ApiResponse.ok(dispatchService.listTasksForWorker(dateFrom, dateTo));
    }

    @Operation(summary = "我已拣单列表")
    @GetMapping("/picked-tasks")
    public ApiResponse<List<WorkerTaskVO>> pickedTasks(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        return ApiResponse.ok(dispatchService.listPickedTasksForWorker(dateFrom, dateTo));
    }

    @Operation(summary = "任务详情")
    @GetMapping("/tasks/{id}")
    public ApiResponse<WorkerTaskVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(dispatchService.getTaskDetail(id));
    }

    @Operation(summary = "开始分拣")
    @PostMapping("/tasks/{id}/start")
    public ApiResponse<WorkerTaskVO> start(@PathVariable Long id) {
        return ApiResponse.ok(dispatchService.startPicking(id));
    }

    @Operation(summary = "更新分拣明细")
    @PatchMapping("/tasks/{orderId}/items/{itemId}")
    public ApiResponse<WorkerTaskVO> updateItem(
            @PathVariable Long orderId,
            @PathVariable Long itemId,
            @RequestBody WorkerItemUpdateRequest request) {
        return ApiResponse.ok(dispatchService.updateItem(orderId, itemId, request));
    }

    @Operation(summary = "一键按下单数出库")
    @PostMapping("/tasks/{id}/fill-qty")
    public ApiResponse<WorkerTaskVO> fillQty(@PathVariable Long id) {
        return ApiResponse.ok(dispatchService.fillActualQty(id));
    }

    @Operation(summary = "标记已拣完")
    @PostMapping("/tasks/{id}/picked")
    public ApiResponse<WorkerTaskVO> picked(@PathVariable Long id) {
        return ApiResponse.ok(dispatchService.markPicked(id));
    }

    @Operation(summary = "一键完成分拣")
    @PostMapping("/tasks/{id}/complete-pick")
    public ApiResponse<WorkerTaskVO> completePick(@PathVariable Long id) {
        return ApiResponse.ok(dispatchService.completePickQuick(id));
    }

    @Operation(summary = "标记已送达")
    @PostMapping("/tasks/{id}/delivered")
    public ApiResponse<WorkerTaskVO> delivered(@PathVariable Long id) {
        return ApiResponse.ok(dispatchService.markDelivered(id));
    }
}
