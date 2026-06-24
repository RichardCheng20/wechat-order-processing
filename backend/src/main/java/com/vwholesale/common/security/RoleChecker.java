package com.vwholesale.common.security;

import cn.dev33.satoken.stp.StpUtil;
import com.vwholesale.common.enums.UserRole;
import com.vwholesale.common.exception.BusinessException;

public final class RoleChecker {

    private RoleChecker() {
    }

    public static void requireBoss() {
        StpUtil.checkLogin();
        if (!isBossRole()) {
            throw BusinessException.of(403, "需要管理员权限");
        }
    }

    /** 仅老板（主管理员）可查看数据平台 / 经营统计 */
    public static void requireOwnerAdmin() {
        StpUtil.checkLogin();
        if (!StpUtil.hasRole(UserRole.OWNER_ADMIN.name())) {
            throw BusinessException.of(403, "仅老板可查看经营数据");
        }
    }

    public static boolean isBossRole() {
        return StpUtil.hasRole(UserRole.OWNER_ADMIN.name())
                || StpUtil.hasRole(UserRole.STALL_MANAGER.name())
                || StpUtil.hasRole("PARTNER_ADMIN");
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
            throw BusinessException.of(403, "需要员工权限");
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
