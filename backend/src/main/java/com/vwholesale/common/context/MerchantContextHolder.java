package com.vwholesale.common.context;

public final class MerchantContextHolder {

    private static final ThreadLocal<Long> MERCHANT_ID = new ThreadLocal<>();

    private MerchantContextHolder() {
    }

    public static void set(Long merchantId) {
        if (merchantId == null) {
            MERCHANT_ID.remove();
        } else {
            MERCHANT_ID.set(merchantId);
        }
    }

    public static Long get() {
        return MERCHANT_ID.get();
    }

    public static void clear() {
        MERCHANT_ID.remove();
    }
}
