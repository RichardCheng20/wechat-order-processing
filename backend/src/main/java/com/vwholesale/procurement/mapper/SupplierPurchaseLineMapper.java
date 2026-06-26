package com.vwholesale.procurement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vwholesale.procurement.entity.SupplierPurchaseLine;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDate;

@Mapper
public interface SupplierPurchaseLineMapper extends BaseMapper<SupplierPurchaseLine> {

    @Select("""
            SELECT id, merchant_id, supplier_id, product_id, effective_date,
                   purchase_price, purchased_qty, deleted, created_at, updated_at
            FROM supplier_purchase_lines
            WHERE merchant_id = #{merchantId}
              AND supplier_id = #{supplierId}
              AND product_id = #{productId}
              AND effective_date = #{effectiveDate}
            LIMIT 1
            """)
    SupplierPurchaseLine findByUniqueKey(@Param("merchantId") Long merchantId,
                                         @Param("supplierId") Long supplierId,
                                         @Param("productId") Long productId,
                                         @Param("effectiveDate") LocalDate effectiveDate);

    @Insert("""
            INSERT INTO supplier_purchase_lines
                (merchant_id, supplier_id, product_id, effective_date, purchase_price, purchased_qty, deleted)
            VALUES
                (#{merchantId}, #{supplierId}, #{productId}, #{effectiveDate}, #{purchasePrice}, #{purchasedQty}, 0)
            ON DUPLICATE KEY UPDATE
                purchase_price = COALESCE(VALUES(purchase_price), purchase_price),
                purchased_qty = VALUES(purchased_qty),
                deleted = 0,
                updated_at = CURRENT_TIMESTAMP
            """)
    void upsertUnique(@Param("merchantId") Long merchantId,
                      @Param("supplierId") Long supplierId,
                      @Param("productId") Long productId,
                      @Param("effectiveDate") LocalDate effectiveDate,
                      @Param("purchasePrice") BigDecimal purchasePrice,
                      @Param("purchasedQty") BigDecimal purchasedQty);

    @Delete("""
            DELETE FROM supplier_purchase_lines
            WHERE id = #{lineId}
              AND merchant_id = #{merchantId}
            """)
    int physicalDelete(@Param("lineId") Long lineId, @Param("merchantId") Long merchantId);
}
