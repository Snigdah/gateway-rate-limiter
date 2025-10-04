package com.api.gateway;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Refill;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RateLimitFilter extends AbstractGatewayFilterFactory<RateLimitFilter.Config> {

    private final LettuceBasedProxyManager<byte[]> proxyManager;

    public RateLimitFilter(LettuceBasedProxyManager<byte[]> proxyManager) {
        super(Config.class);
        this.proxyManager = proxyManager;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            // Use request path or a unique identifier for the bucket key
            String bucketKey = exchange.getRequest().getPath().toString();

            // Create bucket with per-second and per-minute limits
            Bucket bucket = proxyManager.builder()
                    .build(bucketKey.getBytes(), () -> createBucketConfig(config));

            // Try to consume a token
            if (bucket.tryConsume(1)) {
                // Token available, proceed with the request
                return chain.filter(exchange);
            } else {
                // Rate limit exceeded
                exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                return exchange.getResponse().setComplete();
            }
        };
    }

    private BucketConfiguration createBucketConfig(Config config) {
        // Define per-second limit
        Bandwidth perSecondLimit = Bandwidth.classic(
                config.getPerSecond(),
                Refill.intervally(config.getPerSecond(), Duration.ofSeconds(1))
        );

        // Define per-minute limit
        Bandwidth perMinuteLimit = Bandwidth.classic(
                config.getPerMinute(),
                Refill.intervally(config.getPerMinute(), Duration.ofMinutes(1))
        );

        // Build and return the BucketConfiguration
        return BucketConfiguration.builder()
                .addLimit(perSecondLimit)
                .addLimit(perMinuteLimit)
                .build();
    }

    public static class Config {
        private String route;
        private int perSecond;
        private int perMinute;

        // Getters and setters
        public String getRoute() {
            return route;
        }

        public void setRoute(String route) {
            this.route = route;
        }

        public int getPerSecond() {
            return perSecond;
        }

        public void setPerSecond(int perSecond) {
            this.perSecond = perSecond;
        }

        public int getPerMinute() {
            return perMinute;
        }

        public void setPerMinute(int perMinute) {
            this.perMinute = perMinute;
        }
    }
}