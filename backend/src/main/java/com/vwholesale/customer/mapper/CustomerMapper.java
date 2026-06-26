package com.vwholesale.customer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vwholesale.customer.entity.Customer;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CustomerMapper extends BaseMapper<Customer> {

    @Select("""
            SELECT MAX(CAST(RIGHT(customer_no, 3) AS UNSIGNED))
            FROM customers
            WHERE merchant_id = #{merchantId}
              AND customer_no LIKE CONCAT(#{datePrefix}, '%')
              AND LENGTH(customer_no) = 11
              AND deleted = 0
            """)
    Integer selectMaxSeqForDate(@Param("merchantId") Long merchantId, @Param("datePrefix") String datePrefix);
}
