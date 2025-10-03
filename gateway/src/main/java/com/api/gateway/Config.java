package com.api.gateway;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

@Configuration
public class Config
{
    @Bean
    KeyResolver userKeyResolver() {
        return exchange -> {

            String apiKey = exchange.getRequest().getHeaders().getFirst("X-API-Key");
            if (apiKey != null) {
                return Mono.just(apiKey);  // Rate limit by API Key
            }

            // 2nd Priority: If no API key, rate limit by IP address
            return Mono.just(exchange.getRequest()
                                     .getRemoteAddress()
                                     .getAddress()
                                     .getHostAddress()
                                    );
        };
    }
}
