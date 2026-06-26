package com.vwholesale.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import lombok.Data;
import org.springframework.util.StringUtils;

import java.util.List;

@Data
public class OrderCreateRequest {

    /** 未绑定客户档案时必填：店铺/客户名称 */
    private String customerName;

    @Valid
    private List<OrderItemCreateRequest> items;

    private String remark;

    /** 客户拍照下单时的原始图片 URL（先上传 /api/customer/files/upload） */
    private String sourceImageUrl;

    @AssertTrue(message = "请上传图片或选择至少一件商品")
    public boolean isItemsOrImageValid() {
        boolean hasImage = StringUtils.hasText(sourceImageUrl);
        boolean hasItems = items != null && !items.isEmpty();
        return hasImage || hasItems;
    }
}
