package com.vwholesale.customer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;

@Data
@TableName("customer_id_sequences")
public class CustomerIdSequence {

    @TableId(type = IdType.INPUT)
    private Long merchantId;
    private LocalDate seqDate;
    private Integer lastSeq;
}
