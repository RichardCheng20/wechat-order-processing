package com.vwholesale.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vwholesale.product.entity.ProductPriceRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProductPriceMapper extends BaseMapper<ProductPriceRecord> {
}
