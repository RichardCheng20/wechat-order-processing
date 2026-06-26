package com.vwholesale.procurement.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class ProcurementProductDetailVO {

    private Long productId;
    private Boolean customItem;
    private String customName;
    private String productName;
    private String unit;
    /** 代采商品进价已在录价环节录入 */
    private Boolean recordedAtPricing;
    private LocalDate receiveDate;
    private BigDecimal demandQty;
    /** 可用库存 = 实物 - 占用 */
    private BigDecimal stockQty;
    private BigDecimal physicalStockQty;
    private BigDecimal reservedQty;
    private BigDecimal needQty;
    private BigDecimal purchasePrice;
    private BigDecimal purchasedQtyToday;
    private BigDecimal referencePurchasePrice;
    private Boolean priced;
    private List<ProcurementCustomerLineVO> customerLines;
    /** 当前商品默认/已选供应商 */
    private Long supplierId;
    private String supplierName;
    private String supplierNo;
    /** 可选供应商列表 */
    private List<ProcurementSupplierOptionVO> supplierOptions;
    /** 当日已向各供应商下单明细 */
    private List<ProcurementSupplierOrderLineVO> supplierOrders;
}
