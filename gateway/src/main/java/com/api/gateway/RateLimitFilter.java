package com.api.gateway;

import com.api.gateway.dto.AllowedEndpoint;
import com.api.gateway.dto.EndpointLimit;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import lombok.Data;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class RateLimitFilter extends AbstractGatewayFilterFactory<RateLimitFilter.Config> {
    private final LettuceBasedProxyManager<byte[]> proxyManager;
    private final FileLicenseService fileLicenseService;

    public RateLimitFilter(LettuceBasedProxyManager<byte[]> proxyManager, FileLicenseService fileLicenseService) {
        super(Config.class);
        this.proxyManager = proxyManager;
        this.fileLicenseService = fileLicenseService;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            // Get clientId from X-API-Key header
            String clientId = exchange.getRequest().getHeaders().getFirst("X-API-Key");
            String requestPath = exchange.getRequest().getPath().toString();

            // Step 1: Check if clientId exists
            if (clientId == null || !fileLicenseService.licenseExists(clientId)) {
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            // Step 2: Check if client is active
            Boolean isActive = fileLicenseService.isActive(clientId);
            if (isActive == null || !isActive) {
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            // Step 3: Check client expiration
            String clientExpiresAt = fileLicenseService.getClientExpiresAt(clientId);
            if (clientExpiresAt != null && isDateExpired(clientExpiresAt)) {
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            // Step 4: Check if endpoint is blocked
            if (fileLicenseService.isEndpointBlocked(clientId, requestPath)) {
                exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                return exchange.getResponse().setComplete();
            }

            // Step 5: Check if endpoint is allowed and get its configuration
            AllowedEndpoint allowedEndpoint = getAllowedEndpointForPath(clientId, requestPath);
            if (allowedEndpoint == null) {
                exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                return exchange.getResponse().setComplete();
            }

            // Step 6: Check endpoint expiration
            String endpointExpiresAt = allowedEndpoint.getEndPointExpiresAt();
            if (endpointExpiresAt != null && isDateExpired(endpointExpiresAt)) {
                exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                return exchange.getResponse().setComplete();
            }

            // Step 7: Apply rate limiting if limits are defined
            EndpointLimit limits = allowedEndpoint.getLimits();
            if (limits != null && hasRateLimits(limits)) {
                String bucketKey = clientId + ":" + allowedEndpoint.getPath();
                Bucket bucket = proxyManager.builder()
                        .build(bucketKey.getBytes(), () -> createBucketConfig(limits));
                if (!bucket.tryConsume(1)) {
                    exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                    return exchange.getResponse().setComplete();
                }
            }

            // All checks passed, proceed with the request
            return chain.filter(exchange);
        };
    }

    private AllowedEndpoint getAllowedEndpointForPath(String clientId, String requestPath) {
        List<AllowedEndpoint> allowedEndpoints = fileLicenseService.getAllowedEndpoints(clientId);
        return allowedEndpoints.stream()
                .filter(endpoint -> matchesPathPattern(requestPath, endpoint.getPath()))
                .findFirst()
                .orElse(null);
    }

    private boolean matchesPathPattern(String requestPath, String pattern) {
        // Escape regex special chars first
        String regex = pattern
                .replace(".", "\\.") // escape dots
                .replace("**", ".*") // ** = match any characters including /
                .replace("*", ".*") // * = match any characters (even across /)
                .replace("?", "."); // ? = match any single char
        // Ensure full match
        return requestPath.matches(regex);
    }

    private boolean isDateExpired(String dateString) {
        try {
            LocalDate expiryDate = LocalDate.parse(dateString, DateTimeFormatter.ISO_DATE);
            return LocalDate.now().isAfter(expiryDate);
        } catch (Exception e) {
            // If date parsing fails, consider it as expired for safety
            return true;
        }
    }

    private boolean hasRateLimits(EndpointLimit limits) {
        // Check if any rate limit is defined (not null and > 0)
        return (limits.getPerSecond() > 0) ||
                (limits.getPerMinute() > 0) ||
                (limits.getPerHour() > 0) ||
                (limits.getPerDay() > 0);
    }

    private BucketConfiguration createBucketConfig(EndpointLimit limits) {
        Bandwidth perSecondLimit = null;
        Bandwidth perMinuteLimit = null;
        Bandwidth perHourLimit = null;
        Bandwidth perDayLimit = null;

        if (limits.getPerSecond() > 0) {
            perSecondLimit = Bandwidth.builder()
                    .capacity(limits.getPerSecond())
                    .refillGreedy(limits.getPerSecond(), Duration.ofSeconds(1))
                    .build();
        }

        if (limits.getPerMinute() > 0) {
            perMinuteLimit = Bandwidth.builder()
                    .capacity(limits.getPerMinute())
                    .refillIntervally(limits.getPerMinute(), Duration.ofMinutes(1))
                    .build();
        }

        if (limits.getPerHour() > 0) {
            perHourLimit = Bandwidth.builder()
                    .capacity(limits.getPerHour())
                    .refillIntervally(limits.getPerHour(), Duration.ofHours(1))
                    .build();
        }

        if (limits.getPerDay() > 0) {
            perDayLimit = Bandwidth.builder()
                    .capacity(limits.getPerDay())
                    .refillIntervally(limits.getPerDay(), Duration.ofDays(1))
                    .build();
        }

        var builder = BucketConfiguration.builder();
        if (perSecondLimit != null) builder.addLimit(perSecondLimit);
        if (perMinuteLimit != null) builder.addLimit(perMinuteLimit);
        if (perHourLimit != null) builder.addLimit(perHourLimit);
        if (perDayLimit != null) builder.addLimit(perDayLimit);

        return builder.build();
    }

    @Data
    public static class Config {
        // Keeping this for backward compatibility, but now we read from JSON
        private String route;
        private int perSecond;
        private int perMinute;
    }
}