package com.vwholesale.customer.controller;

import com.vwholesale.common.response.ApiResponse;
import com.vwholesale.customer.dto.CustomerCreateRequest;
import com.vwholesale.customer.dto.CustomerUpdateRequest;
import com.vwholesale.customer.dto.CustomerVO;
import com.vwholesale.customer.dto.CustomerBindRequestRejectRequest;
import com.vwholesale.customer.dto.CustomerBindRequestVO;
import com.vwholesale.customer.dto.CustomerRegisterInviteVO;
import com.vwholesale.customer.dto.InviteCodeVO;
import com.vwholesale.customer.service.CustomerBindRequestService;
import com.vwholesale.customer.service.CustomerRegisterInviteService;
import com.vwholesale.customer.service.CustomerService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Tag(name = "老板端-客户")
@RestController
@RequestMapping("/api/boss")
@RequiredArgsConstructor
public class BossCustomerController {

    private final CustomerService customerService;
    private final CustomerRegisterInviteService customerRegisterInviteService;
    private final CustomerBindRequestService customerBindRequestService;

    @Operation(summary = "客户列表")
    @GetMapping("/customers")
    public ApiResponse<List<CustomerVO>> list(@RequestParam(required = false) String keyword) {
        return ApiResponse.ok(customerService.listForBoss(keyword));
    }

    @Operation(summary = "客户详情")
    @GetMapping("/customers/{id}")
    public ApiResponse<CustomerVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(customerService.getById(id));
    }

    @Operation(summary = "新建客户")
    @PostMapping("/customers")
    public ApiResponse<CustomerVO> create(@Valid @RequestBody CustomerCreateRequest request) {
        return ApiResponse.ok(customerService.create(request));
    }

    @Operation(summary = "更新客户")
    @PutMapping("/customers/{id}")
    public ApiResponse<CustomerVO> update(@PathVariable Long id, @RequestBody CustomerUpdateRequest request) {
        return ApiResponse.ok(customerService.update(id, request));
    }

    @Operation(summary = "删除客户")
    @DeleteMapping("/customers/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        customerService.delete(id);
        return ApiResponse.ok(null);
    }

    @Operation(summary = "解绑客户微信")
    @PostMapping("/customers/{id}/unbind")
    public ApiResponse<CustomerVO> unbind(@PathVariable Long id) {
        return ApiResponse.ok(customerService.unbindWechat(id));
    }

    @Operation(summary = "生成邀请码")
    @PostMapping("/customers/{id}/invite")
    public ApiResponse<InviteCodeVO> invite(@PathVariable Long id) {
        return ApiResponse.ok(customerService.generateInviteCode(id));
    }

    @Operation(summary = "生成档口客户自助注册邀请")
    @PostMapping("/customers/register-invite")
    public ApiResponse<CustomerRegisterInviteVO> registerInvite() {
        return ApiResponse.ok(customerRegisterInviteService.generateForBoss());
    }

    @Operation(summary = "当前有效的档口注册邀请")
    @GetMapping("/customers/register-invite")
    public ApiResponse<CustomerRegisterInviteVO> currentRegisterInvite() {
        return ApiResponse.ok(customerRegisterInviteService.currentForBoss());
    }

    @Operation(summary = "待审核绑定申请数量")
    @GetMapping("/customer-bind-requests/pending-count")
    public ApiResponse<Map<String, Long>> pendingBindCount() {
        return ApiResponse.ok(Map.of("count", customerBindRequestService.countPendingForBoss()));
    }

    @Operation(summary = "客户绑定申请列表")
    @GetMapping("/customer-bind-requests")
    public ApiResponse<List<CustomerBindRequestVO>> bindRequests(
            @RequestParam(required = false) String status) {
        return ApiResponse.ok(customerBindRequestService.listForBoss(status));
    }

    @Operation(summary = "同意客户绑定申请")
    @PostMapping("/customer-bind-requests/{id}/approve")
    public ApiResponse<CustomerVO> approveBindRequest(@PathVariable Long id) {
        return ApiResponse.ok(customerBindRequestService.approve(id));
    }

    @Operation(summary = "拒绝客户绑定申请")
    @PostMapping("/customer-bind-requests/{id}/reject")
    public ApiResponse<Void> rejectBindRequest(
            @PathVariable Long id,
            @RequestBody(required = false) CustomerBindRequestRejectRequest request) {
        customerBindRequestService.reject(id, request);
        return ApiResponse.ok(null);
    }
}
