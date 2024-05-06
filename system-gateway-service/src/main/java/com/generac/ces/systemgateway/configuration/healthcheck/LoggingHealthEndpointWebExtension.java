package com.generac.ces.systemgateway.configuration.healthcheck;

import java.util.Map;
import java.util.TreeMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.endpoint.ApiVersion;
import org.springframework.boot.actuate.endpoint.SecurityContext;
import org.springframework.boot.actuate.endpoint.web.WebEndpointResponse;
import org.springframework.boot.actuate.endpoint.web.WebServerNamespace;
import org.springframework.boot.actuate.endpoint.web.annotation.EndpointWebExtension;
import org.springframework.boot.actuate.health.CompositeHealth;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthContributorRegistry;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.HealthEndpointGroups;
import org.springframework.boot.actuate.health.HealthEndpointWebExtension;
import org.springframework.boot.actuate.health.Status;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@EndpointWebExtension(endpoint = HealthEndpoint.class)
public class LoggingHealthEndpointWebExtension extends HealthEndpointWebExtension {

    public LoggingHealthEndpointWebExtension(
            HealthContributorRegistry registry, HealthEndpointGroups groups) {
        super(registry, groups);
    }

    @Override
    public WebEndpointResponse<HealthComponent> health(
            ApiVersion apiVersion,
            WebServerNamespace serverNamespace,
            SecurityContext securityContext,
            boolean showAll,
            String... path) {
        WebEndpointResponse<HealthComponent> response =
                super.health(apiVersion, serverNamespace, securityContext, showAll, path);
        HealthComponent health = response.getBody();
        if (health == null) {
            return response;
        }

        Status status = health.getStatus();
        if (!Status.UP.equals(status)) {
            Map<String, HealthComponent> components = new TreeMap<>();
            if (health instanceof CompositeHealth) {
                Map<String, HealthComponent> details = ((CompositeHealth) health).getComponents();
                if (details != null) {
                    components.putAll(details);
                }
            }
            log.warn("Health endpoints {} returned {}. components={}", path, status, components);
        }

        return response;
    }
}
