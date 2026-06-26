package com.vwholesale.product.support;

import com.vwholesale.product.entity.Product;

import java.math.BigDecimal;

public final class ProductStockSupport {

    private ProductStockSupport() {
    }

    public static BigDecimal physicalStock(Product product) {
        if (product == null || product.getStockQty() == null) {
            return BigDecimal.ZERO;
        }
        return product.getStockQty();
    }

    public static BigDecimal reservedQty(Product product) {
        if (product == null || product.getReservedQty() == null) {
            return BigDecimal.ZERO;
        }
        return product.getReservedQty();
    }

    public static BigDecimal availableStock(Product product) {
        return physicalStock(product).subtract(reservedQty(product));
    }
}
