package br.com.demo.backend.identity;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import br.com.demo.backend.TestcontainersConfiguration;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = {
        "app.admin.name=Admin Teste",
        "app.admin.email=Admin@Example.com",
        "app.admin.password=SenhaForte123"
})
class AdminBootstrapTest {
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;
    @Autowired AdminBootstrap bootstrap;
    @Autowired JdbcTemplate jdbc;

    @Test
    void criaManagerAtivoComSenhaEmBcrypt() {
        User admin = users.findByEmail("admin@example.com").orElseThrow();

        assertThat(admin.getRole()).isEqualTo(Role.MANAGER);
        assertThat(admin.isActive()).isTrue();
        assertThat(admin.getName()).isEqualTo("Admin Teste");
        assertThat(admin.getPasswordHash()).isNotEqualTo("SenhaForte123");
        assertThat(encoder.matches("SenhaForte123", admin.getPasswordHash())).isTrue();
    }

    @Test
    void buscaPorEmailIgnoraMaiusculas() {
        assertThat(users.findByEmail("ADMIN@EXAMPLE.COM")).isPresent();
    }

    @Test
    void segundaExecucaoNaoDuplicaNemSobrescreve() {
        User antes = users.findByEmail("admin@example.com").orElseThrow();

        bootstrap.run(new DefaultApplicationArguments());

        User depois = users.findByEmail("admin@example.com").orElseThrow();
        assertThat(users.count()).isEqualTo(1);
        assertThat(depois.getId()).isEqualTo(antes.getId());
        assertThat(depois.getPasswordHash()).isEqualTo(antes.getPasswordHash());
    }

    @Test
    void bancoRecusaEmailDuplicadoIgnorandoMaiusculas() {
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO users (name, email, password_hash, role) VALUES ('X', 'ADMIN@EXAMPLE.COM', 'x', 'VIEWER')"))
                .hasMessageContaining("ux_users_email");
    }
}
