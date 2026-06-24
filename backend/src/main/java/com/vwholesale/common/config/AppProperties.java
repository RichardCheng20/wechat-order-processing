package com.vwholesale.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Merchant merchant = new Merchant();
    private Admin admin = new Admin();
    private Customer customer = new Customer();
    private Wechat wechat = new Wechat();
    private Mq mq = new Mq();

    @Data
    public static class Mq {
        private boolean enabled = true;
    }

    @Data
    public static class Merchant {
        private Long defaultId = 1L;
    }

    @Data
    public static class Admin {
        private boolean autoUpgradeEnabled = true;
        private List<String> openidWhitelist = new ArrayList<>();
    }

    @Data
    public static class Customer {
        private int inviteCodeExpireDays = 7;
    }

    @Data
    public static class Wechat {
        private String appId;
        private String appSecret;
        /** 客户提醒老板：订阅消息模板 ID，需与微信公众平台模板字段 thing1/character_string2/thing3/time4 对应 */
        private String orderNotifyTemplateId;
    }
}
