package com.vwholesale.common.security;

import cn.dev33.satoken.stp.StpUtil;
import com.vwholesale.common.enums.UserRole;
import com.vwholesale.common.exception.BusinessException;

public final class RoleChecker {

    private RoleChecker() {
    }

    public static void requireBoss() {
        StpUtil.checkLogin();
        if (!StpUtil.hasRole(UserRole.OWNER_ADMIN.name()) && !StpUtil.hasRole(UserRole.PARTNER_ADMIN.name())) {
            throw BusinessException.of(403, "需要管理员权限");
        }
    }

    public static void requireCustomer() {
        StpUtil.checkLogin();
        if (!StpUtil.hasRole(UserRole.CUSTOMER.name())) {
            throw BusinessException.of(403, "需要客户权限");
        }
    }

    public static void requireWorker() {
        StpUtil.checkLogin();
        if (!StpUtil.hasRole(UserRole.WORKER.name())) {
            throw BusinessException.of(403, "需要工人权限");
        }
    }

    public static Long currentUserId() {
        StpUtil.checkLogin();
        return StpUtil.getLoginIdAsLong();
    }

    public static Long currentCustomerId() {
        Object customerId = StpUtil.getSession().get("customerId");
        if (customerId == null) {
            return null;
        }
        return Long.parseLong(customerId.toString());
    }

    public static Long currentWorkerId() {
        Object workerId = StpUtil.getSession().get("workerId");
        if (workerId == null) {
            return null;
        }
        return Long.parseLong(workerId.toString());
    }
}
