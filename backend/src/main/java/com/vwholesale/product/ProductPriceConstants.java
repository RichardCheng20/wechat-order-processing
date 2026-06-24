package com.vwholesale.product;

public final class ProductPriceConstants {

    /** product_prices 中基础价使用的 customer_id（非真实客户档案） */
    public static final Long BASE_PRICE_CUSTOMER_ID = 0L;

    private ProductPriceConstants() {
    }

    public static boolean isBasePriceCustomer(Long customerId) {
        return BASE_PRICE_CUSTOMER_ID.equals(customerId);
    }
}
