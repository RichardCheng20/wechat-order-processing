package com.vwholesale.customer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vwholesale.customer.entity.CustomerIdSequence;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;

public interface CustomerIdSequenceMapper extends BaseMapper<CustomerIdSequence> {

    @Insert("""
            INSERT INTO customer_id_sequences (merchant_id, seq_date, last_seq)
            VALUES (#{merchantId}, #{seqDate}, 1)
            ON DUPLICATE KEY UPDATE last_seq = LAST_INSERT_ID(last_seq + 1)
            """)
    void allocate(@Param("merchantId") Long merchantId, @Param("seqDate") LocalDate seqDate);

    @Select("SELECT LAST_INSERT_ID()")
    int currentSeq();
}
