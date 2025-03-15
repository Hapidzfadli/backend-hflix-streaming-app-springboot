package com.hapidzfadli.hflix.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.jwt")
@Data
public class JwtProperties {
    private String secret;
    private Long expirationsMs;
    private String tokenPrefix;
    private String headerName;
    private String issuer;
}
