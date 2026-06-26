package com.vwholesale.supplier.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;

@Data
@TableName("supplier_id_sequences")
public class SupplierIdSequence {

    @TableId(type = IdType.INPUT)
    private Long merchantId;
    private LocalDate seqDate;
    private Integer lastSeq;
}
