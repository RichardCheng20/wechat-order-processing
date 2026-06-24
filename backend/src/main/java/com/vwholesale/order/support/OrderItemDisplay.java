package com.vwholesale.order.support;

import com.vwholesale.order.entity.OrderItem;
import com.vwholesale.product.entity.Product;
import org.springframework.util.StringUtils;

public final class OrderItemDisplay {

    private OrderItemDisplay() {
    }

    public static boolean isCustomItem(OrderItem item) {
        return item != null && StringUtils.hasText(item.getOriginalText());
    }

    public static String customName(OrderItem item) {
        return isCustomItem(item) ? item.getOriginalText().trim() : null;
    }

    public static String productName(OrderItem item, Product product) {
        if (isCustomItem(item)) {
            return item.getOriginalText().trim();
        }
        return product != null ? product.getName() : "未知商品";
    }
}
