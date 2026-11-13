package br.com.demo.backend.identity;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import br.com.demo.backend.TestcontainersConfiguration;

@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = { "app.admin.email=", "app.admin.password=" })
class AdminBootstrapSemVariaveisTest {
    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwordEncoder;

    @Test
    void semVariaveisAAplicacaoSobeENaoCriaUsuario() {
        assertThat(users.count()).isZero();
    }

    @Test
    void senhaCurtaEhRecusada() {
        var bootstrap = new AdminBootstrap(users, passwordEncoder,
                new AdminProperties("Admin", "admin@example.com", "curta"));

        assertThatThrownBy(() -> bootstrap.run(new DefaultApplicationArguments()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ADMIN_PASSWORD");
        assertThat(users.count()).isZero();
    }

    @Test
    void emailInvalidoEhRecusado() {
        var bootstrap = new AdminBootstrap(users, passwordEncoder,
                new AdminProperties("Admin", "sem-arroba", "SenhaForte123"));

        assertThatThrownBy(() -> bootstrap.run(new DefaultApplicationArguments()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ADMIN_EMAIL");
        assertThat(users.count()).isZero();
    }
}
