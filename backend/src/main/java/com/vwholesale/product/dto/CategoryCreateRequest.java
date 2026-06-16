package com.vwholesale.product.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CategoryCreateRequest {

    @NotBlank(message = "分类名称不能为空")
    private String name;

    /** 上级分类 ID，为空表示一级分类 */
    private Long parentId;
}
