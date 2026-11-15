package br.com.demo.backend.identity;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import java.time.Duration;

@ConfigurationProperties("app.jwt")
public record JwtProperties(
        String secret,
        @DefaultValue("8h") Duration expiration
) { }
