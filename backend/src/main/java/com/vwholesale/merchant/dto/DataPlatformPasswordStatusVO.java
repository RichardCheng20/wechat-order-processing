package com.vwholesale.merchant.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DataPlatformPasswordStatusVO {

    /** 是否已设置查看密码 */
    private boolean passwordEnabled;
}
