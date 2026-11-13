package br.com.demo.backend.identity;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.StringUtils;

@ConfigurationProperties("app.admin")
public record AdminProperties (@DefaultValue("Administrador") String name, String email, String password) {
    boolean hasCredentials() {
        return StringUtils.hasText(email) || StringUtils.hasText(password);
    }
}
