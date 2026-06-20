package com.vwholesale.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.vwholesale.common.context.MerchantContext;
import com.vwholesale.common.exception.BusinessException;
import com.vwholesale.order.entity.OrderItem;
import com.vwholesale.order.mapper.OrderItemMapper;
import com.vwholesale.product.entity.Product;
import com.vwholesale.product.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderItemStockService {

    private final ProductMapper productMapper;
    private final OrderItemMapper orderItemMapper;
    private final MerchantContext merchantContext;

    /**
     * 按「出库数 - 已扣库存数」差量同步库存，避免重复一键出库时 delta 为 0 而漏扣。
     */
    public void syncPickStock(OrderItem item) {
        if (item == null || item.getId() == null) {
            return;
        }
        BigDecimal actual = item.getActualQty() != null ? item.getActualQty() : BigDecimal.ZERO;
        BigDecimal applied = item.getStockAppliedQty() != null ? item.getStockAppliedQty() : BigDecimal.ZERO;
        BigDecimal delta = actual.subtract(applied);
        if (delta.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }
        applyStockChange(item.getProductId(), delta);
        item.setStockAppliedQty(actual);
        orderItemMapper.updateById(item);
    }

    private void applyStockChange(Long productId, BigDecimal delta) {
        if (productId == null || delta.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }
        Product product = productMapper.selectOne(new LambdaQueryWrapper<Product>()
                .eq(Product::getId, productId)
                .eq(Product::getMerchantId, merchantContext.currentMerchantId()));
        if (product == null) {
            log.warn("sync pick stock skipped: product {} not found for merchant {}", productId,
                    merchantContext.currentMerchantId());
            throw BusinessException.of(400, "商品不存在，无法扣减库存");
        }
        BigDecimal stock = product.getStockQty() != null ? product.getStockQty() : BigDecimal.ZERO;
        product.setStockQty(stock.subtract(delta).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP));
        productMapper.updateById(product);
    }
}
