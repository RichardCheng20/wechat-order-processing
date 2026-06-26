package com.vwholesale.procurement.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.vwholesale.common.context.MerchantContext;
import com.vwholesale.common.exception.BusinessException;
import com.vwholesale.common.security.RoleChecker;
import com.vwholesale.procurement.dto.ProcurementSupplierOrderLineVO;
import com.vwholesale.procurement.entity.SupplierPurchaseLine;
import com.vwholesale.procurement.mapper.SupplierPurchaseLineMapper;
import com.vwholesale.product.entity.Product;
import com.vwholesale.product.mapper.ProductMapper;
import com.vwholesale.product.service.ProductPurchasePriceService;
import com.vwholesale.supplier.dto.SupplierPurchaseLineVO;
import com.vwholesale.supplier.entity.Supplier;
import com.vwholesale.supplier.mapper.SupplierMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SupplierPurchaseLineService {

    private final SupplierPurchaseLineMapper lineMapper;
    private final SupplierMapper supplierMapper;
    private final ProductMapper productMapper;
    private final ProductPurchasePriceService productPurchasePriceService;
    private final MerchantContext merchantContext;

    public List<ProcurementSupplierOrderLineVO> listForProduct(Long productId, LocalDate date) {
        RoleChecker.requireBoss();
        return toProcurementVOList(listLinesByProduct(productId, date));
    }

    public List<SupplierPurchaseLineVO> listForSupplier(Long supplierId, LocalDate dateFrom, LocalDate dateTo) {
        RoleChecker.requireBoss();
        Long merchantId = merchantContext.currentMerchantId();
        getSupplierOrThrow(supplierId);

        LambdaQueryWrapper<SupplierPurchaseLine> wrapper = new LambdaQueryWrapper<SupplierPurchaseLine>()
                .eq(SupplierPurchaseLine::getMerchantId, merchantId)
                .eq(SupplierPurchaseLine::getSupplierId, supplierId)
                .eq(SupplierPurchaseLine::getDeleted, 0)
                .orderByDesc(SupplierPurchaseLine::getEffectiveDate)
                .orderByDesc(SupplierPurchaseLine::getId);
        if (dateFrom != null) {
            wrapper.ge(SupplierPurchaseLine::getEffectiveDate, dateFrom);
        }
        if (dateTo != null) {
            wrapper.le(SupplierPurchaseLine::getEffectiveDate, dateTo);
        }

        List<SupplierPurchaseLine> lines = lineMapper.selectList(wrapper);
        if (lines.isEmpty()) {
            return List.of();
        }

        Set<Long> productIds = lines.stream().map(SupplierPurchaseLine::getProductId).collect(Collectors.toSet());
        Map<Long, Product> productMap = productMapper.selectBatchIds(productIds).stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        return lines.stream()
                .map(line -> toSupplierVO(line, productMap.get(line.getProductId())))
                .toList();
    }

    @Transactional
    public List<ProcurementSupplierOrderLineVO> upsertLine(Long productId, Long supplierId, LocalDate date,
                                                           BigDecimal purchasePrice, BigDecimal purchasedQty) {
        RoleChecker.requireBoss();
        if (supplierId == null) {
            throw BusinessException.of(400, "请选择供应商");
        }
        if (purchasePrice != null && purchasePrice.compareTo(BigDecimal.ZERO) < 0) {
            throw BusinessException.of(400, "采购价不能为负数");
        }
        if (purchasedQty == null || purchasedQty.compareTo(BigDecimal.ZERO) <= 0) {
            throw BusinessException.of(400, "请输入采购数量");
        }

        Long merchantId = merchantContext.currentMerchantId();
        Product product = getProductOrThrow(productId);
        getSupplierOrThrow(supplierId);
        LocalDate effectiveDate = date != null ? date : LocalDate.now();

        SupplierPurchaseLine existing = lineMapper.findByUniqueKey(
                merchantId, supplierId, productId, effectiveDate);

        BigDecimal oldQty = existing != null && existing.getPurchasedQty() != null
                ? existing.getPurchasedQty() : BigDecimal.ZERO;

        BigDecimal priceToSave = purchasePrice;
        if (priceToSave == null && existing != null) {
            priceToSave = existing.getPurchasePrice();
        }

        lineMapper.upsertUnique(
                merchantId, supplierId, productId, effectiveDate, priceToSave, purchasedQty);

        BigDecimal delta = purchasedQty.subtract(oldQty);
        if (delta.compareTo(BigDecimal.ZERO) != 0) {
            BigDecimal currentStock = product.getStockQty() != null ? product.getStockQty() : BigDecimal.ZERO;
            product.setStockQty(currentStock.add(delta).max(BigDecimal.ZERO));
            productMapper.updateById(product);
        }

        if (purchasePrice != null && purchasePrice.compareTo(BigDecimal.ZERO) > 0) {
            product.setDefaultPurchasePrice(purchasePrice);
            productMapper.updateById(product);
        }

        syncProductAggregate(productId, effectiveDate);
        return toProcurementVOList(listLinesByProduct(productId, effectiveDate));
    }

    @Transactional
    public List<ProcurementSupplierOrderLineVO> deleteLine(Long lineId) {
        RoleChecker.requireBoss();
        Long merchantId = merchantContext.currentMerchantId();
        SupplierPurchaseLine line = lineMapper.selectOne(new LambdaQueryWrapper<SupplierPurchaseLine>()
                .eq(SupplierPurchaseLine::getId, lineId)
                .eq(SupplierPurchaseLine::getMerchantId, merchantId)
                .eq(SupplierPurchaseLine::getDeleted, 0));
        if (line == null) {
            throw BusinessException.of(404, "采购明细不存在");
        }

        Product product = getProductOrThrow(line.getProductId());
        BigDecimal qty = line.getPurchasedQty() != null ? line.getPurchasedQty() : BigDecimal.ZERO;
        if (qty.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal currentStock = product.getStockQty() != null ? product.getStockQty() : BigDecimal.ZERO;
            product.setStockQty(currentStock.subtract(qty).max(BigDecimal.ZERO));
            productMapper.updateById(product);
        }

        lineMapper.physicalDelete(lineId, merchantId);
        syncProductAggregate(line.getProductId(), line.getEffectiveDate());
        return toProcurementVOList(listLinesByProduct(line.getProductId(), line.getEffectiveDate()));
    }

    public BigDecimal sumPurchasedQty(Long productId, LocalDate date) {
        return listLinesByProduct(productId, date).stream()
                .map(SupplierPurchaseLine::getPurchasedQty)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void syncProductAggregate(Long productId, LocalDate date) {
        List<SupplierPurchaseLine> lines = listLinesByProduct(productId, date);
        if (lines.isEmpty()) {
            productPurchasePriceService.upsertPurchasePrice(productId, BigDecimal.ZERO, BigDecimal.ZERO, date);
            return;
        }
        BigDecimal totalQty = BigDecimal.ZERO;
        BigDecimal pricedQty = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (SupplierPurchaseLine line : lines) {
            BigDecimal qty = line.getPurchasedQty() != null ? line.getPurchasedQty() : BigDecimal.ZERO;
            BigDecimal price = line.getPurchasePrice();
            totalQty = totalQty.add(qty);
            if (price != null && price.compareTo(BigDecimal.ZERO) > 0) {
                pricedQty = pricedQty.add(qty);
                totalAmount = totalAmount.add(price.multiply(qty));
            }
        }
        if (pricedQty.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal avgPrice = totalAmount.divide(pricedQty, 2, RoundingMode.HALF_UP);
            productPurchasePriceService.upsertPurchasePrice(productId, avgPrice, totalQty, date);
        } else {
            productPurchasePriceService.updatePurchasedQty(productId, totalQty, date);
        }
    }

    private List<SupplierPurchaseLine> listLinesByProduct(Long productId, LocalDate date) {
        return lineMapper.selectList(new LambdaQueryWrapper<SupplierPurchaseLine>()
                .eq(SupplierPurchaseLine::getMerchantId, merchantContext.currentMerchantId())
                .eq(SupplierPurchaseLine::getProductId, productId)
                .eq(SupplierPurchaseLine::getEffectiveDate, date)
                .eq(SupplierPurchaseLine::getDeleted, 0)
                .orderByAsc(SupplierPurchaseLine::getId));
    }

    private List<ProcurementSupplierOrderLineVO> toProcurementVOList(List<SupplierPurchaseLine> lines) {
        if (lines.isEmpty()) {
            return List.of();
        }
        Set<Long> supplierIds = lines.stream().map(SupplierPurchaseLine::getSupplierId).collect(Collectors.toSet());
        Map<Long, Supplier> supplierMap = supplierMapper.selectBatchIds(supplierIds).stream()
                .collect(Collectors.toMap(Supplier::getId, s -> s));

        List<ProcurementSupplierOrderLineVO> result = new ArrayList<>();
        for (SupplierPurchaseLine line : lines) {
            Supplier supplier = supplierMap.get(line.getSupplierId());
            BigDecimal qty = line.getPurchasedQty() != null ? line.getPurchasedQty() : BigDecimal.ZERO;
            BigDecimal price = line.getPurchasePrice();
            result.add(ProcurementSupplierOrderLineVO.builder()
                    .id(line.getId())
                    .supplierId(line.getSupplierId())
                    .supplierName(supplier != null ? supplier.getName() : "未知供应商")
                    .supplierNo(supplier != null ? supplier.getSupplierNo() : null)
                    .purchasePrice(price)
                    .purchasedQty(qty)
                    .lineAmount(price != null && price.compareTo(BigDecimal.ZERO) > 0
                            ? price.multiply(qty).setScale(2, RoundingMode.HALF_UP) : null)
                    .build());
        }
        return result;
    }

    private SupplierPurchaseLineVO toSupplierVO(SupplierPurchaseLine line, Product product) {
        BigDecimal qty = line.getPurchasedQty() != null ? line.getPurchasedQty() : BigDecimal.ZERO;
        BigDecimal price = line.getPurchasePrice();
        return SupplierPurchaseLineVO.builder()
                .id(line.getId())
                .productId(line.getProductId())
                .productName(product != null ? product.getName() : "未知商品")
                .unit(product != null ? product.getUnit() : "斤")
                .effectiveDate(line.getEffectiveDate())
                .purchasePrice(price)
                .purchasedQty(qty)
                .lineAmount(price != null && price.compareTo(BigDecimal.ZERO) > 0
                        ? price.multiply(qty).setScale(2, RoundingMode.HALF_UP) : null)
                .build();
    }

    private Product getProductOrThrow(Long productId) {
        Product product = productMapper.selectOne(new LambdaQueryWrapper<Product>()
                .eq(Product::getId, productId)
                .eq(Product::getMerchantId, merchantContext.currentMerchantId()));
        if (product == null) {
            throw BusinessException.of(404, "商品不存在");
        }
        return product;
    }

    private Supplier getSupplierOrThrow(Long supplierId) {
        Supplier supplier = supplierMapper.selectOne(new LambdaQueryWrapper<Supplier>()
                .eq(Supplier::getId, supplierId)
                .eq(Supplier::getMerchantId, merchantContext.currentMerchantId()));
        if (supplier == null || supplier.getStatus() == null || supplier.getStatus() == 0) {
            throw BusinessException.of(400, "供应商不存在或已停用");
        }
        return supplier;
    }
}
