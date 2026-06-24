package com.vwholesale.auth.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ActivationTokenVO {

    private String token;
    private String targetRole;
    private String targetRoleLabel;
    private Long workerId;
    private String workerName;
    private LocalDateTime expiredAt;
    /** 小程序登录路径提示，如 pages/login/index?m=1&act=XXXX */
    private String loginPath;
}
