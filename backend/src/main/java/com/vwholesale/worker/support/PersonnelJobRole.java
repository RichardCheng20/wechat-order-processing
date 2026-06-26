package com.vwholesale.worker.support;

import java.util.Map;
import java.util.Set;

public final class PersonnelJobRole {

    public static final String STALL_OWNER = "STALL_OWNER";
    public static final String STALL_MANAGER = "STALL_MANAGER";
    public static final String DELIVERY = "DELIVERY";
    /** @deprecated 兼容历史数据，等同 {@link #STALL_MANAGER} */
    public static final String STALL_ADMIN = "STALL_ADMIN";
    public static final String SALES_MANAGER = "SALES_MANAGER";
    public static final String FINANCE = "FINANCE";
    public static final String CASHIER = "CASHIER";
    public static final String CONSIGNOR = "CONSIGNOR";

    private static final Set<String> ASSIGNABLE = Set.of(STALL_OWNER, STALL_MANAGER, DELIVERY);

    private static final Map<String, String> LABELS = Map.ofEntries(
            Map.entry(STALL_OWNER, "档口老板"),
            Map.entry(STALL_MANAGER, "档口经理"),
            Map.entry(STALL_ADMIN, "档口经理"),
            Map.entry(DELIVERY, "配送员"),
            Map.entry(SALES_MANAGER, "配送员"),
            Map.entry(FINANCE, "财务"),
            Map.entry(CASHIER, "收银"),
            Map.entry(CONSIGNOR, "货主")
    );

    private PersonnelJobRole() {
    }

    public static boolean isValid(String role) {
        if (role == null) {
            return false;
        }
        return ASSIGNABLE.contains(role) || STALL_ADMIN.equals(role);
    }

    public static String normalize(String role) {
        if (STALL_ADMIN.equals(role)) {
            return STALL_MANAGER;
        }
        return role;
    }

    public static boolean isStallOwner(String role) {
        return STALL_OWNER.equals(role);
    }

    public static boolean isStallManager(String role) {
        return STALL_MANAGER.equals(role) || STALL_ADMIN.equals(role);
    }

    public static String labelOf(String role) {
        if (role == null) {
            return "员工";
        }
        return LABELS.getOrDefault(role, role);
    }
}
