package br.com.demo.backend.identity;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Teste unitário: não sobe Spring nem banco. */
class JwtServiceTest {
    private static final String SECRET = "segredo-somente-para-testes-com-mais-de-32-caracteres";

    private final JwtService service = new JwtService(new JwtProperties(SECRET, Duration.ofHours(8)));

    private static User userWithId(UUID id, Role role) {
        User user = new User("Maria", "maria@example.com", "hash", role);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    @Test
    void tokenTemSubRoleEExpiracaoCorretos() {
        UUID id = UUID.randomUUID();
        Instant antes = Instant.now();

        String token = service.generate(userWithId(id, Role.OPERATOR));

        var data = service.validate(token).orElseThrow();
        assertThat(data.userId()).isEqualTo(id);
        assertThat(data.role()).isEqualTo(Role.OPERATOR);
        assertThat(data.expiresAt())
                .isBetween(antes.plus(Duration.ofHours(8)).minusSeconds(5),
                        Instant.now().plus(Duration.ofHours(8)).plusSeconds(5));
    }

    @Test
    void rejeitaTokenAssinadoComOutraChave() {
        SecretKey outraChave = Keys.hmacShaKeyFor(
                "outro-segredo-totalmente-diferente-com-32-chars".getBytes(StandardCharsets.UTF_8));
        String forjado = Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .claim("role", "MANAGER")
                .expiration(Date.from(Instant.now().plus(Duration.ofHours(1))))
                .signWith(outraChave, Jwts.SIG.HS256)
                .compact();

        assertThat(service.validate(forjado)).isEmpty();
    }

    @Test
    void rejeitaTokenExpirado() {
        Clock ontem = Clock.fixed(Instant.now().minus(Duration.ofDays(1)), ZoneOffset.UTC);
        JwtService emissorNoPassado = new JwtService(new JwtProperties(SECRET, Duration.ofHours(8)), ontem);

        String expirado = emissorNoPassado.generate(userWithId(UUID.randomUUID(), Role.VIEWER));

        assertThat(service.validate(expirado)).isEmpty();
    }

    @Test
    void rejeitaTokenAdulterado() {
        String token = service.generate(userWithId(UUID.randomUUID(), Role.VIEWER));
        String[] partes = token.split("\\.");
        // troca o papel no payload mantendo a assinatura antiga
        String payloadAdulterado = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(
                ("{\"sub\":\"" + UUID.randomUUID() + "\",\"role\":\"MANAGER\",\"exp\":9999999999}")
                        .getBytes(StandardCharsets.UTF_8));

        assertThat(service.validate(partes[0] + "." + payloadAdulterado + "." + partes[2])).isEmpty();
    }

    @Test
    void rejeitaLixoENulo() {
        assertThat(service.validate("isso-nao-e-um-jwt")).isEmpty();
        assertThat(service.validate("")).isEmpty();
        assertThat(service.validate(null)).isEmpty();
    }

    @Test
    void segredoCurtoOuAusenteImpedeAInicializacao() {
        assertThatThrownBy(() -> new JwtService(new JwtProperties("curto", Duration.ofHours(8))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("JWT_SECRET");
        assertThatThrownBy(() -> new JwtService(new JwtProperties("", Duration.ofHours(8))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new JwtService(new JwtProperties(null, Duration.ofHours(8))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void expiracaoInvalidaImpedeAInicializacao() {
        assertThatThrownBy(() -> new JwtService(new JwtProperties(SECRET, Duration.ZERO)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_EXPIRATION");
    }
}
