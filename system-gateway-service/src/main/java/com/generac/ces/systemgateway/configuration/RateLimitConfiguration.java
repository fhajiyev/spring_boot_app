package com.generac.ces.systemgateway.configuration;

import com.generac.ces.systemgateway.entity.subscription.RateLimit;
import com.generac.ces.systemgateway.repository.subscription.RateLimitRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "rate-limitation")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RateLimitConfiguration {

    private Integer lastDays;

    private Map<String, Integer> rateLimitMap;
    public static final Map<String, Integer> maxDurationMap = new HashMap<>();
    private RateLimitRepository rateLimitRepository;

    @Autowired
    public RateLimitConfiguration(RateLimitRepository rateLimitRepository) {
        this.rateLimitRepository = rateLimitRepository;
        loadRateLimitData();
    }

    private void loadRateLimitData() {
        rateLimitMap = new HashMap<>();
        List<RateLimit> rateLimitList = rateLimitRepository.findAll();

        for (RateLimit rl : rateLimitList) {
            rateLimitMap.put(
                    rl.getSubscriberType() + "-" + rl.getSystemType() + "-" + rl.getResourceType(),
                    rl.getDailyLimitSec());
            maxDurationMap.put(
                    rl.getSubscriberType() + "-" + rl.getSystemType() + "-" + rl.getResourceType(),
                    rl.getMaxDurationSec());
        }
    }
}
