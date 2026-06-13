package com.vwholesale.product.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.vwholesale.common.context.MerchantContext;
import com.vwholesale.common.exception.BusinessException;
import com.vwholesale.product.dto.ProductCreateRequest;
import com.vwholesale.product.dto.ProductUpdateRequest;
import com.vwholesale.product.dto.ProductVO;
import com.vwholesale.product.entity.Product;
import com.vwholesale.product.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductMapper productMapper;
    private final ProductCategoryService categoryService;
    private final ProductPriceService productPriceService;
    private final MerchantContext merchantContext;

    public List<ProductVO> listForBoss(Long categoryId, String keyword) {
        return toVOList(queryProducts(categoryId, keyword, null), null, false);
    }

    public List<ProductVO> listForCustomer(Long categoryId, String keyword, Long customerId) {
        return toVOList(queryProducts(categoryId, keyword, "ON"), customerId, false);
    }

    @Transactional
    public ProductVO create(ProductCreateRequest request) {
        categoryService.getById(request.getCategoryId());

        Product product = new Product();
        product.setMerchantId(merchantContext.currentMerchantId());
        product.setCategoryId(request.getCategoryId());
        product.setName(request.getName().trim());
        product.setAliases(request.getAliases());
        product.setUnit(StringUtils.hasText(request.getUnit()) ? request.getUnit() : "斤");
        product.setSpec(request.getSpec());
        product.setDefaultPrice(request.getDefaultPrice());
        product.setSaleStatus(StringUtils.hasText(request.getSaleStatus()) ? request.getSaleStatus() : "ON");
        productMapper.insert(product);
        return toVO(product, null, false);
    }

    @Transactional
    public ProductVO update(Long id, ProductUpdateRequest request) {
        Product product = getProductOrThrow(id);

        if (request.getCategoryId() != null) {
            categoryService.getById(request.getCategoryId());
            product.setCategoryId(request.getCategoryId());
        }
        if (StringUtils.hasText(request.getName())) {
            product.setName(request.getName().trim());
        }
        if (request.getAliases() != null) {
            product.setAliases(request.getAliases());
        }
        if (StringUtils.hasText(request.getUnit())) {
            product.setUnit(request.getUnit());
        }
        if (request.getSpec() != null) {
            product.setSpec(request.getSpec());
        }
        if (request.getDefaultPrice() != null) {
            product.setDefaultPrice(request.getDefaultPrice());
        }
        if (StringUtils.hasText(request.getSaleStatus())) {
            product.setSaleStatus(request.getSaleStatus());
        }
        productMapper.updateById(product);
        return toVO(product, null, false);
    }

    @Transactional
    public ProductVO updateSaleStatus(Long id, String saleStatus) {
        if (!"ON".equals(saleStatus) && !"OFF".equals(saleStatus)) {
            throw BusinessException.of(400, "上下架状态无效");
        }
        Product product = getProductOrThrow(id);
        product.setSaleStatus(saleStatus);
        productMapper.updateById(product);
        return toVO(product, null, false);
    }

    private List<Product> queryProducts(Long categoryId, String keyword, String saleStatus) {
        Long merchantId = merchantContext.currentMerchantId();
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .eq(Product::getMerchantId, merchantId)
                .orderByDesc(Product::getId);

        if (categoryId != null) {
            wrapper.eq(Product::getCategoryId, categoryId);
        }
        if (StringUtils.hasText(saleStatus)) {
            wrapper.eq(Product::getSaleStatus, saleStatus);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Product::getName, keyword).or().like(Product::getAliases, keyword));
        }
        return productMapper.selectList(wrapper);
    }

    private Product getProductOrThrow(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null || !merchantContext.currentMerchantId().equals(product.getMerchantId())) {
            throw BusinessException.of(404, "商品不存在");
        }
        return product;
    }

    private List<ProductVO> toVOList(List<Product> products, Long customerId, boolean withReferencePrice) {
        Map<Long, String> categoryNames = categoryService.categoryNameMap();

        Map<Long, BigDecimal> priceMap = Map.of();
        if (withReferencePrice && !products.isEmpty()) {
            Map<Long, BigDecimal> defaults = products.stream()
                    .collect(Collectors.toMap(Product::getId, Product::getDefaultPrice, (a, b) -> a));
            priceMap = productPriceService.batchResolveReferencePrices(
                    products.stream().map(Product::getId).toList(),
                    customerId,
                    defaults,
                    LocalDate.now()
            );
        }

        Map<Long, BigDecimal> finalPriceMap = priceMap;
        return products.stream()
                .map(p -> buildVO(p, categoryNames.get(p.getCategoryId()), finalPriceMap.get(p.getId()), withReferencePrice))
                .toList();
    }

    private ProductVO toVO(Product product, Long customerId, boolean withReferencePrice) {
        Map<Long, String> categoryNames = categoryService.categoryNameMap();
        BigDecimal referencePrice = null;
        if (withReferencePrice) {
            referencePrice = productPriceService.resolveReferencePrice(
                    product.getId(), customerId, product.getDefaultPrice(), LocalDate.now());
        }
        return buildVO(product, categoryNames.get(product.getCategoryId()), referencePrice, withReferencePrice);
    }

    private ProductVO buildVO(Product product, String categoryName, BigDecimal referencePrice,
                              boolean withReferencePrice) {
        return ProductVO.builder()
                .id(product.getId())
                .categoryId(product.getCategoryId())
                .categoryName(categoryName)
                .name(product.getName())
                .aliases(product.getAliases())
                .unit(product.getUnit())
                .spec(product.getSpec())
                .defaultPrice(withReferencePrice ? product.getDefaultPrice() : null)
                .referencePrice(withReferencePrice ? referencePrice : null)
                .saleStatus(product.getSaleStatus())
                .build();
    }
}
