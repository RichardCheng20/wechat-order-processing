package com.vwholesale.supplier.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vwholesale.supplier.entity.Supplier;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SupplierMapper extends BaseMapper<Supplier> {

    @Select("""
            SELECT MAX(CAST(RIGHT(supplier_no, 3) AS UNSIGNED))
            FROM suppliers
            WHERE merchant_id = #{merchantId}
              AND supplier_no LIKE CONCAT(#{datePrefix}, '%')
              AND LENGTH(supplier_no) = 11
              AND deleted = 0
            """)
    Integer selectMaxSeqForDate(@Param("merchantId") Long merchantId, @Param("datePrefix") String datePrefix);
}
