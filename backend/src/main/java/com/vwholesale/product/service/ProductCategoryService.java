package com.vwholesale.product.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.vwholesale.common.context.MerchantContext;
import com.vwholesale.common.exception.BusinessException;
import com.vwholesale.product.dto.CategoryVO;
import com.vwholesale.product.entity.ProductCategory;
import com.vwholesale.product.mapper.ProductCategoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProductCategoryService {

    private final ProductCategoryMapper categoryMapper;
    private final MerchantContext merchantContext;

    public List<CategoryVO> listEnabled() {
        Long merchantId = merchantContext.currentMerchantId();
        List<ProductCategory> categories = categoryMapper.selectList(
                new LambdaQueryWrapper<ProductCategory>()
                        .eq(ProductCategory::getMerchantId, merchantId)
                        .eq(ProductCategory::getStatus, 1)
                        .orderByAsc(ProductCategory::getSortOrder)
        );
        return categories.stream()
                .map(c -> CategoryVO.builder()
                        .id(c.getId())
                        .name(c.getName())
                        .sortOrder(c.getSortOrder())
                        .build())
                .toList();
    }

    public ProductCategory getById(Long id) {
        ProductCategory category = categoryMapper.selectById(id);
        if (category == null) {
            throw BusinessException.of(404, "分类不存在");
        }
        return category;
    }

    public Map<Long, String> categoryNameMap() {
        return listEnabled().stream()
                .collect(java.util.stream.Collectors.toMap(CategoryVO::getId, CategoryVO::getName, (a, b) -> a));
    }
}
