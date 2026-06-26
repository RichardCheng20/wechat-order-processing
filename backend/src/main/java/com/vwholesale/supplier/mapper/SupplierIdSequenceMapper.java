package com.vwholesale.supplier.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vwholesale.supplier.entity.SupplierIdSequence;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;

public interface SupplierIdSequenceMapper extends BaseMapper<SupplierIdSequence> {

    @Insert("""
            INSERT INTO supplier_id_sequences (merchant_id, seq_date, last_seq)
            VALUES (#{merchantId}, #{seqDate}, LAST_INSERT_ID(1))
            ON DUPLICATE KEY UPDATE last_seq = LAST_INSERT_ID(last_seq + 1)
            """)
    void allocate(@Param("merchantId") Long merchantId, @Param("seqDate") LocalDate seqDate);

    @Insert("""
            INSERT INTO supplier_id_sequences (merchant_id, seq_date, last_seq)
            VALUES (#{merchantId}, #{seqDate}, #{minSeq})
            ON DUPLICATE KEY UPDATE last_seq = GREATEST(last_seq, VALUES(last_seq))
            """)
    void syncMin(@Param("merchantId") Long merchantId, @Param("seqDate") LocalDate seqDate, @Param("minSeq") int minSeq);

    @Select("SELECT LAST_INSERT_ID()")
    int currentSeq();
}
