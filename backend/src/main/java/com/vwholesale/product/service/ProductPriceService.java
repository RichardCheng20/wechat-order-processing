package com.vwholesale.product.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.vwholesale.common.context.MerchantContext;
import com.vwholesale.product.entity.ProductPriceRecord;
import com.vwholesale.product.mapper.ProductPriceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductPriceService {

    private final ProductPriceMapper productPriceMapper;
    private final MerchantContext merchantContext;

    public BigDecimal resolveReferencePrice(Long productId, Long customerId, BigDecimal defaultPrice, LocalDate date) {
        Long merchantId = merchantContext.currentMerchantId();

        if (customerId != null) {
            ProductPriceRecord customerPrice = productPriceMapper.selectOne(
                    new LambdaQueryWrapper<ProductPriceRecord>()
                            .eq(ProductPriceRecord::getMerchantId, merchantId)
                            .eq(ProductPriceRecord::getProductId, productId)
                            .eq(ProductPriceRecord::getCustomerId, customerId)
                            .eq(ProductPriceRecord::getEffectiveDate, date)
                            .eq(ProductPriceRecord::getStatus, 1)
                            .last("LIMIT 1")
            );
            if (customerPrice != null) {
                return customerPrice.getPrice();
            }
        }

        ProductPriceRecord basePrice = productPriceMapper.selectOne(
                new LambdaQueryWrapper<ProductPriceRecord>()
                        .eq(ProductPriceRecord::getMerchantId, merchantId)
                        .eq(ProductPriceRecord::getProductId, productId)
                        .isNull(ProductPriceRecord::getCustomerId)
                        .eq(ProductPriceRecord::getEffectiveDate, date)
                        .eq(ProductPriceRecord::getStatus, 1)
                        .last("LIMIT 1")
        );
        if (basePrice != null) {
            return basePrice.getPrice();
        }

        return defaultPrice;
    }

    public Map<Long, BigDecimal> batchResolveReferencePrices(List<Long> productIds, Long customerId,
                                                             Map<Long, BigDecimal> defaultPrices, LocalDate date) {
        if (productIds.isEmpty()) {
            return Map.of();
        }
        Long merchantId = merchantContext.currentMerchantId();
        Map<Long, BigDecimal> result = new HashMap<>();

        if (customerId != null) {
            List<ProductPriceRecord> customerPrices = productPriceMapper.selectList(
                    new LambdaQueryWrapper<ProductPriceRecord>()
                            .eq(ProductPriceRecord::getMerchantId, merchantId)
                            .in(ProductPriceRecord::getProductId, productIds)
                            .eq(ProductPriceRecord::getCustomerId, customerId)
                            .eq(ProductPriceRecord::getEffectiveDate, date)
                            .eq(ProductPriceRecord::getStatus, 1)
            );
            customerPrices.forEach(p -> result.put(p.getProductId(), p.getPrice()));
        }

        List<Long> missing = productIds.stream().filter(id -> !result.containsKey(id)).collect(Collectors.toList());
        if (!missing.isEmpty()) {
            List<ProductPriceRecord> basePrices = productPriceMapper.selectList(
                    new LambdaQueryWrapper<ProductPriceRecord>()
                            .eq(ProductPriceRecord::getMerchantId, merchantId)
                            .in(ProductPriceRecord::getProductId, missing)
                            .isNull(ProductPriceRecord::getCustomerId)
                            .eq(ProductPriceRecord::getEffectiveDate, date)
                            .eq(ProductPriceRecord::getStatus, 1)
            );
            basePrices.forEach(p -> result.putIfAbsent(p.getProductId(), p.getPrice()));
        }

        for (Long productId : productIds) {
            result.putIfAbsent(productId, defaultPrices.get(productId));
        }
        return result;
    }

    /** 查询指定日期的基础价；无当日记录时用 defaultPrice 兜底（便于批量录价对比昨日）。 */
    public Map<Long, BigDecimal> batchGetDailyBasePrices(List<Long> productIds, LocalDate date,
                                                         Map<Long, BigDecimal> fallbackDefaults) {
        if (productIds.isEmpty()) {
            return Map.of();
        }
        Long merchantId = merchantContext.currentMerchantId();
        Map<Long, BigDecimal> result = new HashMap<>();
        List<ProductPriceRecord> records = productPriceMapper.selectList(
                new LambdaQueryWrapper<ProductPriceRecord>()
                        .eq(ProductPriceRecord::getMerchantId, merchantId)
                        .in(ProductPriceRecord::getProductId, productIds)
                        .isNull(ProductPriceRecord::getCustomerId)
                        .eq(ProductPriceRecord::getEffectiveDate, date)
                        .eq(ProductPriceRecord::getStatus, 1)
        );
        records.forEach(r -> result.put(r.getProductId(), r.getPrice()));
        for (Long productId : productIds) {
            if (result.containsKey(productId)) {
                continue;
            }
            BigDecimal fallback = fallbackDefaults.get(productId);
            if (fallback != null && fallback.compareTo(BigDecimal.ZERO) > 0) {
                result.put(productId, fallback);
            }
        }
        return result;
    }

    public void upsertDailyBasePrice(Long productId, BigDecimal price, LocalDate date) {
        if (price == null) {
            return;
        }
        Long merchantId = merchantContext.currentMerchantId();
        ProductPriceRecord existing = productPriceMapper.selectOne(
                new LambdaQueryWrapper<ProductPriceRecord>()
                        .eq(ProductPriceRecord::getMerchantId, merchantId)
                        .eq(ProductPriceRecord::getProductId, productId)
                        .isNull(ProductPriceRecord::getCustomerId)
                        .eq(ProductPriceRecord::getEffectiveDate, date)
                        .last("LIMIT 1")
        );
        if (existing != null) {
            existing.setPrice(price);
            existing.setStatus(1);
            productPriceMapper.updateById(existing);
            return;
        }
        ProductPriceRecord record = new ProductPriceRecord();
        record.setMerchantId(merchantId);
        record.setProductId(productId);
        record.setPrice(price);
        record.setEffectiveDate(date);
        record.setStatus(1);
        productPriceMapper.insert(record);
    }
}
