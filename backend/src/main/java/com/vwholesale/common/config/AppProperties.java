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
    }
}
