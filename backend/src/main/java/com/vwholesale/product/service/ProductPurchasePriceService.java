package com.vwholesale.product.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.vwholesale.common.context.MerchantContext;
import com.vwholesale.product.entity.ProductPurchasePriceRecord;
import com.vwholesale.product.mapper.ProductPurchasePriceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProductPurchasePriceService {

    private final ProductPurchasePriceMapper productPurchasePriceMapper;
    private final MerchantContext merchantContext;

    public BigDecimal resolvePurchasePrice(Long productId, BigDecimal defaultPurchasePrice, LocalDate date) {
        ProductPurchasePriceRecord record = findRecord(productId, date);
        if (record != null) {
            return record.getPurchasePrice();
        }
        return defaultPurchasePrice;
    }

    public Map<Long, BigDecimal> batchResolvePurchasePrices(List<Long> productIds, LocalDate date,
                                                            Map<Long, BigDecimal> defaultPrices) {
        if (productIds.isEmpty()) {
            return Map.of();
        }
        Long merchantId = merchantContext.currentMerchantId();
        List<ProductPurchasePriceRecord> records = productPurchasePriceMapper.selectList(
                new LambdaQueryWrapper<ProductPurchasePriceRecord>()
                        .eq(ProductPurchasePriceRecord::getMerchantId, merchantId)
                        .in(ProductPurchasePriceRecord::getProductId, productIds)
                        .eq(ProductPurchasePriceRecord::getEffectiveDate, date)
                        .eq(ProductPurchasePriceRecord::getStatus, 1));
        Map<Long, BigDecimal> result = new HashMap<>();
        records.forEach(r -> result.put(r.getProductId(), r.getPurchasePrice()));
        for (Long productId : productIds) {
            result.putIfAbsent(productId, defaultPrices.get(productId));
        }
        return result;
    }

    public ProductPurchasePriceRecord findRecord(Long productId, LocalDate date) {
        return productPurchasePriceMapper.selectOne(
                new LambdaQueryWrapper<ProductPurchasePriceRecord>()
                        .eq(ProductPurchasePriceRecord::getMerchantId, merchantContext.currentMerchantId())
                        .eq(ProductPurchasePriceRecord::getProductId, productId)
                        .eq(ProductPurchasePriceRecord::getEffectiveDate, date)
                        .last("LIMIT 1"));
    }

    public void upsertPurchasePrice(Long productId, BigDecimal purchasePrice, BigDecimal purchasedQty, LocalDate date) {
        if (purchasePrice == null) {
            return;
        }
        ProductPurchasePriceRecord existing = findRecord(productId, date);
        if (existing != null) {
            existing.setPurchasePrice(purchasePrice);
            if (purchasedQty != null) {
                existing.setPurchasedQty(purchasedQty);
            }
            existing.setStatus(1);
            productPurchasePriceMapper.updateById(existing);
            return;
        }
        ProductPurchasePriceRecord record = new ProductPurchasePriceRecord();
        record.setMerchantId(merchantContext.currentMerchantId());
        record.setProductId(productId);
        record.setPurchasePrice(purchasePrice);
        record.setPurchasedQty(purchasedQty != null ? purchasedQty : BigDecimal.ZERO);
        record.setEffectiveDate(date);
        record.setStatus(1);
        productPurchasePriceMapper.insert(record);
    }

    public void upsertPurchasePrice(Long productId, BigDecimal purchasePrice, LocalDate date) {
        upsertPurchasePrice(productId, purchasePrice, null, date);
    }

    public void updatePurchasedQty(Long productId, BigDecimal purchasedQty, LocalDate date) {
        ProductPurchasePriceRecord existing = findRecord(productId, date);
        if (existing != null) {
            existing.setPurchasedQty(purchasedQty != null ? purchasedQty : BigDecimal.ZERO);
            productPurchasePriceMapper.updateById(existing);
        }
    }
}
