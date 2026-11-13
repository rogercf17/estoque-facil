package br.com.demo.backend.identity;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(AdminProperties.class)
class AdminBootstrap implements ApplicationRunner {
    static final int MIN_PASSWORD_LENGTH = 8;
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}$");
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AdminProperties admin;

    @Override
    public void run(ApplicationArguments args) {
        if (users.count() > 0) {
            return;
        }
        if (!admin.hasCredentials()) {
            log.warn("Nenhum usuário cadastrado e ADMIN_EMAIL/ADMIN_PASSWORD não definidos: "
                    + "ninguém conseguirá entrar. Defina as variáveis no .env e reinicie.");
            return;
        }

        validate();

        User manager = new User(admin.name(), admin.email(),
                passwordEncoder.encode(admin.password()), Role.MANAGER);
        users.save(manager);

        log.info("Administrador inicial criado: {}", manager.getEmail());
    }

    private void validate() {
        String email = admin.email();
        if (!StringUtils.hasText(email) || !EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new IllegalStateException("ADMIN_EMAIL ausente ou inválido");
        }
        String password = admin.password();
        if (!StringUtils.hasText(password) || password.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalStateException(
                    "ADMIN_PASSWORD deve ter pelo menos " + MIN_PASSWORD_LENGTH + " caracteres");
        }
    }
}