package com.generac.ces.systemgateway.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "subscription-kinesis-sink")
@Data
public class KinesisConfiguration {
    private String endpoint;
    private String region;
    private String streamName;
}
