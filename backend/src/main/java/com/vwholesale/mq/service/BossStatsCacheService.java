package com.vwholesale.mq.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vwholesale.order.dto.BossDashboardVO;
import com.vwholesale.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class BossStatsCacheService {

    private static final String CACHE_KEY = "vwholesale:stats:boss-dashboard";
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final ObjectProvider<OrderService> orderServiceProvider;

    public BossDashboardVO getCachedDashboard() {
        String json = stringRedisTemplate.opsForValue().get(CACHE_KEY);
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, BossDashboardVO.class);
        } catch (JsonProcessingException ex) {
            log.warn("invalid dashboard cache, clearing", ex);
            invalidate();
            return null;
        }
    }

    public void refresh() {
        try {
            BossDashboardVO dashboard = orderServiceProvider.getObject().computeBossDashboard();
            String json = objectMapper.writeValueAsString(dashboard);
            stringRedisTemplate.opsForValue().set(CACHE_KEY, json, CACHE_TTL);
            log.debug("boss dashboard cache refreshed");
        } catch (Exception ex) {
            log.warn("refresh boss dashboard cache failed", ex);
            invalidate();
        }
    }

    public void invalidate() {
        stringRedisTemplate.delete(CACHE_KEY);
    }
}
