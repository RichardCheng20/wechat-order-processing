package com.vwholesale.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vwholesale.payment.entity.Payment;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PaymentMapper extends BaseMapper<Payment> {
}
