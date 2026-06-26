package com.vwholesale.common.enums;

public enum UserRole {
    CUSTOMER,
    OWNER_ADMIN,
    /** 档口老板：老板端业务权限，可查看全部经营数据（家人协管） */
    STALL_OWNER,
    /** 档口经理：老板端业务权限，不可查看数据平台 */
    STALL_MANAGER,
    WORKER
}
