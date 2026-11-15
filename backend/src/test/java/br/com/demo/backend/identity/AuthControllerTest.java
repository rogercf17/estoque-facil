package br.com.demo.backend.identity;

import br.com.demo.backend.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
@SpringBootTest(properties = { "app.admin.email=", "app.admin.password=" })
class AuthControllerTest {
    private static final String SENHA = "SenhaForte123";
    private static final String MENSAGEM_GENERICA = "E-mail ou senha inválidos";

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;
    @Autowired JwtService jwtService;
    @Autowired JdbcTemplate jdbc;

    private User maria;

    @BeforeEach
    void setUp() {
        users.deleteAll();
        maria = users.save(new User("Maria", "maria@example.com", encoder.encode(SENHA), Role.OPERATOR));
    }

    private String login(String email, String senha) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + senha + "\"}";
    }

    @Test
    void loginOkDevolveTokenValidoERole() throws Exception {
        String body = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(login("maria@example.com", SENHA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.role").value("OPERATOR"))
                .andReturn().getResponse().getContentAsString();

        String token = com.jayway.jsonpath.JsonPath.read(body, "$.token");
        var data = jwtService.validate(token).orElseThrow();
        assertThat(data.userId()).isEqualTo(maria.getId());
        assertThat(data.role()).isEqualTo(Role.OPERATOR);
        assertThat(data.expiresAt()).isBetween(
                Instant.now().plus(Duration.ofHours(8)).minusSeconds(30),
                Instant.now().plus(Duration.ofHours(8)).plusSeconds(30));
    }

    @Test
    void emailIgnoraMaiusculasEEspacos() throws Exception {
        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(login("  MARIA@Example.com ", SENHA)))
                .andExpect(status().isOk());
    }

    @Test
    void senhaErradaDa401Generico() throws Exception {
        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(login("maria@example.com", "errada-errada")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value(MENSAGEM_GENERICA))
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    void emailInexistenteDa401IgualASenhaErrada() throws Exception {
        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(login("ninguem@example.com", SENHA)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value(MENSAGEM_GENERICA));
    }

    @Test
    void usuarioInativoDa401Generico() throws Exception {
        jdbc.update("UPDATE users SET active = false WHERE id = ?", maria.getId());

        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(login("maria@example.com", SENHA)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value(MENSAGEM_GENERICA));
    }

    @Test
    void corpoInvalidoDa400ComErrosPorCampo() throws Exception {
        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(login("nao-e-email", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    void jsonMalformadoOuAusenteDa400() throws Exception {
        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{isso nao e json"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}
