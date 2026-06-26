package com.vwholesale.product.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.vwholesale.common.context.MerchantContext;
import com.vwholesale.common.exception.BusinessException;
import com.vwholesale.common.security.RoleChecker;
import com.vwholesale.product.dto.InventoryProductVO;
import com.vwholesale.product.dto.InventoryStockUpdateRequest;
import com.vwholesale.product.entity.Product;
import com.vwholesale.product.mapper.ProductMapper;
import com.vwholesale.product.support.ProductStockSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class InventoryStockService {

    private final ProductMapper productMapper;
    private final ProductCategoryService categoryService;
    private final MerchantContext merchantContext;

    public List<InventoryProductVO> listProducts(Long categoryId, String keyword) {
        RoleChecker.requireBoss();
        Long merchantId = merchantContext.currentMerchantId();
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .eq(Product::getMerchantId, merchantId)
                .ne(Product::getName, ProductService.CUSTOM_ORDER_PRODUCT_NAME)
                .orderByAsc(Product::getName);

        if (categoryId != null) {
            List<Long> categoryIds = categoryService.resolveFilterCategoryIds(categoryId);
            wrapper.in(Product::getCategoryId, categoryIds);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Product::getName, keyword).or().like(Product::getAliases, keyword));
        }

        List<Product> products = productMapper.selectList(wrapper);
        Map<Long, String> categoryNames = categoryService.categoryNameMap();
        return products.stream().map(p -> toVO(p, categoryNames.get(p.getCategoryId()))).toList();
    }

    @Transactional
    public InventoryProductVO updateStock(Long productId, InventoryStockUpdateRequest request) {
        RoleChecker.requireBoss();
        Product product = getProductOrThrow(productId);
        if (ProductService.isCustomOrderProduct(product)) {
            throw BusinessException.of(400, "系统商品不可修改库存");
        }
        product.setStockQty(request.getStockQty());
        productMapper.updateById(product);
        Map<Long, String> categoryNames = categoryService.categoryNameMap();
        return toVO(productMapper.selectById(productId), categoryNames.get(product.getCategoryId()));
    }

    private Product getProductOrThrow(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null || !merchantContext.currentMerchantId().equals(product.getMerchantId())) {
            throw BusinessException.of(404, "商品不存在");
        }
        return product;
    }

    private InventoryProductVO toVO(Product product, String categoryName) {
        BigDecimal physical = ProductStockSupport.physicalStock(product);
        BigDecimal reserved = ProductStockSupport.reservedQty(product);
        return InventoryProductVO.builder()
                .id(product.getId())
                .categoryId(product.getCategoryId())
                .categoryName(categoryName)
                .name(product.getName())
                .unit(product.getUnit())
                .physicalStockQty(physical)
                .reservedQty(reserved)
                .availableStockQty(ProductStockSupport.availableStock(product))
                .build();
    }
}
