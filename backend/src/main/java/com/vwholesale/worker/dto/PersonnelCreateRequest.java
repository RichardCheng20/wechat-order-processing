package com.vwholesale.worker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class PersonnelCreateRequest {

    @NotBlank(message = "请输入人员姓名")
    private String name;

    @NotBlank(message = "请输入电话号码")
    @Pattern(regexp = "^1\\d{10}$", message = "请输入11位手机号")
    private String phone;

    @NotBlank(message = "请选择人员角色")
    private String jobRole;
}
