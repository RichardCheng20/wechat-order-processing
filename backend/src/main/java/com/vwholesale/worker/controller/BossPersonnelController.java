package com.vwholesale.worker.controller;

import com.vwholesale.auth.dto.ActivationTokenVO;
import com.vwholesale.auth.service.ActivationService;
import com.vwholesale.common.response.ApiResponse;
import com.vwholesale.common.security.RoleChecker;
import com.vwholesale.worker.dto.PersonnelCreateRequest;
import com.vwholesale.worker.dto.PersonnelUpdateRequest;
import com.vwholesale.worker.dto.WorkerVO;
import com.vwholesale.worker.service.WorkerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "老板端-人员管理")
@RestController
@RequestMapping("/api/boss/personnel")
@RequiredArgsConstructor
public class BossPersonnelController {

    private final WorkerService workerService;
    private final ActivationService activationService;

    @Operation(summary = "人员列表")
    @GetMapping
    public ApiResponse<List<WorkerVO>> list() {
        RoleChecker.requireBoss();
        return ApiResponse.ok(workerService.listPersonnel());
    }

    @Operation(summary = "添加人员")
    @PostMapping
    public ApiResponse<WorkerVO> create(@Valid @RequestBody PersonnelCreateRequest request) {
        return ApiResponse.ok(workerService.createPersonnel(request));
    }

    @Operation(summary = "更新人员")
    @PutMapping("/{id}")
    public ApiResponse<WorkerVO> update(@PathVariable Long id, @Valid @RequestBody PersonnelUpdateRequest request) {
        return ApiResponse.ok(workerService.updatePersonnel(id, request));
    }

    @Operation(summary = "停用人员")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> disable(@PathVariable Long id) {
        workerService.disablePersonnel(id);
        return ApiResponse.ok(null);
    }

    @Operation(summary = "生成员工/管理员微信激活码")
    @PostMapping("/{id}/activation-token")
    public ApiResponse<ActivationTokenVO> activationToken(@PathVariable Long id) {
        return ApiResponse.ok(activationService.createWorkerActivationToken(id));
    }
}
