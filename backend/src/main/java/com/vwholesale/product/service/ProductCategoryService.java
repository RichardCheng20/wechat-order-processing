package com.vwholesale.product.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.vwholesale.common.context.MerchantContext;
import com.vwholesale.common.exception.BusinessException;
import com.vwholesale.product.dto.CategoryCreateRequest;
import com.vwholesale.product.dto.CategorySortRequest;
import com.vwholesale.product.dto.CategoryVO;
import com.vwholesale.product.entity.Product;
import com.vwholesale.product.entity.ProductCategory;
import com.vwholesale.product.mapper.ProductCategoryMapper;
import com.vwholesale.product.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductCategoryService {

    private final ProductCategoryMapper categoryMapper;
    private final ProductMapper productMapper;
    private final MerchantContext merchantContext;

    public List<CategoryVO> listEnabled() {
        Long merchantId = merchantContext.currentMerchantId();
        List<ProductCategory> categories = categoryMapper.selectList(
                new LambdaQueryWrapper<ProductCategory>()
                        .eq(ProductCategory::getMerchantId, merchantId)
                        .eq(ProductCategory::getStatus, 1)
                        .orderByAsc(ProductCategory::getSortOrder)
                        .orderByAsc(ProductCategory::getId)
        );
        Map<Long, List<ProductCategory>> childrenMap = categories.stream()
                .filter(c -> c.getParentId() != null)
                .collect(Collectors.groupingBy(ProductCategory::getParentId));

        return categories.stream()
                .filter(c -> c.getParentId() == null)
                .map(c -> toTreeVO(c, childrenMap))
                .toList();
    }

    public ProductCategory getById(Long id) {
        ProductCategory category = categoryMapper.selectById(id);
        if (category == null || !merchantContext.currentMerchantId().equals(category.getMerchantId())) {
            throw BusinessException.of(404, "分类不存在");
        }
        return category;
    }

    public Map<Long, String> categoryNameMap() {
        Long merchantId = merchantContext.currentMerchantId();
        List<ProductCategory> categories = categoryMapper.selectList(
                new LambdaQueryWrapper<ProductCategory>()
                        .eq(ProductCategory::getMerchantId, merchantId)
                        .eq(ProductCategory::getStatus, 1)
        );
        Map<Long, ProductCategory> idMap = categories.stream()
                .collect(Collectors.toMap(ProductCategory::getId, c -> c, (a, b) -> a));
        Map<Long, String> result = new HashMap<>();
        for (ProductCategory category : categories) {
            result.put(category.getId(), buildFullName(category, idMap));
        }
        return result;
    }

    public List<Long> resolveFilterCategoryIds(Long categoryId) {
        getById(categoryId);
        List<Long> ids = new ArrayList<>();
        ids.add(categoryId);
        categoryMapper.selectList(new LambdaQueryWrapper<ProductCategory>()
                        .eq(ProductCategory::getMerchantId, merchantContext.currentMerchantId())
                        .eq(ProductCategory::getParentId, categoryId))
                .forEach(child -> ids.add(child.getId()));
        return ids;
    }

    public boolean isLeafCategory(Long id) {
        getById(id);
        Long childCount = categoryMapper.selectCount(new LambdaQueryWrapper<ProductCategory>()
                .eq(ProductCategory::getMerchantId, merchantContext.currentMerchantId())
                .eq(ProductCategory::getParentId, id));
        return childCount == null || childCount == 0;
    }

    @Transactional
    public CategoryVO create(CategoryCreateRequest request) {
        String name = request.getName().trim();
        if (!StringUtils.hasText(name)) {
            throw BusinessException.of(400, "分类名称不能为空");
        }
        Long merchantId = merchantContext.currentMerchantId();
        Long parentId = request.getParentId();

        if (parentId != null) {
            ProductCategory parent = getById(parentId);
            if (parent.getParentId() != null) {
                throw BusinessException.of(400, "仅支持二级分类");
            }
        }

        LambdaQueryWrapper<ProductCategory> existsWrapper = new LambdaQueryWrapper<ProductCategory>()
                .eq(ProductCategory::getMerchantId, merchantId)
                .eq(ProductCategory::getName, name);
        if (parentId == null) {
            existsWrapper.isNull(ProductCategory::getParentId);
        } else {
            existsWrapper.eq(ProductCategory::getParentId, parentId);
        }
        Long exists = categoryMapper.selectCount(existsWrapper);
        if (exists != null && exists > 0) {
            throw BusinessException.of(400, "同级分类名称已存在");
        }

        Integer maxSort = categoryMapper.selectList(buildSiblingWrapper(merchantId, parentId)
                        .orderByDesc(ProductCategory::getSortOrder)
                        .last("LIMIT 1"))
                .stream().map(ProductCategory::getSortOrder).findFirst().orElse(0);

        ProductCategory category = new ProductCategory();
        category.setMerchantId(merchantId);
        category.setParentId(parentId);
        category.setName(name);
        category.setSortOrder(maxSort + 1);
        category.setStatus(1);
        categoryMapper.insert(category);

        return CategoryVO.builder()
                .id(category.getId())
                .parentId(category.getParentId())
                .name(category.getName())
                .sortOrder(category.getSortOrder())
                .children(List.of())
                .build();
    }

    @Transactional
    public void delete(Long id) {
        ProductCategory category = getById(id);
        Long childCount = categoryMapper.selectCount(new LambdaQueryWrapper<ProductCategory>()
                .eq(ProductCategory::getParentId, id));
        if (childCount != null && childCount > 0) {
            throw BusinessException.of(400, "请先删除下级分类");
        }
        Long productCount = productMapper.selectCount(new LambdaQueryWrapper<Product>()
                .eq(Product::getMerchantId, merchantContext.currentMerchantId())
                .eq(Product::getCategoryId, id));
        if (productCount != null && productCount > 0) {
            throw BusinessException.of(400, "该分类下仍有商品，无法删除");
        }
        categoryMapper.deleteById(category.getId());
    }

    @Transactional
    public void reorder(CategorySortRequest request) {
        Long merchantId = merchantContext.currentMerchantId();
        Long parentId = request.getParentId();
        List<Long> orderedIds = request.getOrderedIds();

        List<ProductCategory> siblings = categoryMapper.selectList(
                buildSiblingWrapper(merchantId, parentId)
                        .eq(ProductCategory::getStatus, 1)
        );
        Set<Long> siblingIds = siblings.stream().map(ProductCategory::getId).collect(Collectors.toSet());
        Set<Long> orderedSet = new HashSet<>(orderedIds);
        if (orderedIds.size() != siblingIds.size() || !siblingIds.equals(orderedSet)) {
            throw BusinessException.of(400, "分类列表不完整或无效");
        }

        int order = 1;
        for (Long id : orderedIds) {
            ProductCategory update = new ProductCategory();
            update.setId(id);
            update.setSortOrder(order++);
            categoryMapper.updateById(update);
        }
    }

    private LambdaQueryWrapper<ProductCategory> buildSiblingWrapper(Long merchantId, Long parentId) {
        LambdaQueryWrapper<ProductCategory> wrapper = new LambdaQueryWrapper<ProductCategory>()
                .eq(ProductCategory::getMerchantId, merchantId);
        if (parentId == null) {
            wrapper.isNull(ProductCategory::getParentId);
        } else {
            wrapper.eq(ProductCategory::getParentId, parentId);
        }
        return wrapper;
    }

    private CategoryVO toTreeVO(ProductCategory category, Map<Long, List<ProductCategory>> childrenMap) {
        List<CategoryVO> children = childrenMap.getOrDefault(category.getId(), List.of()).stream()
                .sorted(Comparator.comparing(ProductCategory::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(ProductCategory::getId))
                .map(child -> CategoryVO.builder()
                        .id(child.getId())
                        .parentId(child.getParentId())
                        .name(child.getName())
                        .sortOrder(child.getSortOrder())
                        .children(List.of())
                        .build())
                .toList();
        return CategoryVO.builder()
                .id(category.getId())
                .parentId(category.getParentId())
                .name(category.getName())
                .sortOrder(category.getSortOrder())
                .children(children)
                .build();
    }

    private String buildFullName(ProductCategory category, Map<Long, ProductCategory> idMap) {
        if (category.getParentId() == null) {
            return category.getName();
        }
        ProductCategory parent = idMap.get(category.getParentId());
        if (parent == null) {
            return category.getName();
        }
        return parent.getName() + "/" + category.getName();
    }
}
