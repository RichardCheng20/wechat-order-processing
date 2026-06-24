package com.vwholesale.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("order_items")
public class OrderItem {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private Long productId;
    private String originalText;
    private BigDecimal orderQty;
    private BigDecimal actualQty;
    private BigDecimal stockAppliedQty;
    private String unit;
    private BigDecimal dealPrice;
    /** 代采成本单价，仅老板端可见 */
    private BigDecimal costPrice;
    private BigDecimal subtotalAmount;
    private Integer shortageFlag;
    private Long substituteProductId;
    private String pickRemark;
    @TableLogic
    private Integer deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
