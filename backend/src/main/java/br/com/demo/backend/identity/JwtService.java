package br.com.demo.backend.identity;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Service
@EnableConfigurationProperties(JwtProperties.class)
public class JwtService {
    static final int MIN_SECRET_BYTES = 32;
    static final String ROLE_CLAIM = "role";

    public record TokenData(UUID userId, Role role, Instant expiresAt) {}

    private final SecretKey key;
    private final Duration expiration;
    private final Clock clock;

    @Autowired
    public JwtService(JwtProperties properties) {
        this(properties, Clock.systemUTC());
    }

    JwtService(JwtProperties properties, Clock clock) {
        byte[] secret = properties.secret() == null
                ? new byte[0]
                : properties.secret().getBytes(StandardCharsets.UTF_8);

        if (secret.length < MIN_SECRET_BYTES) {
            throw new IllegalArgumentException(
                    "JWT_SECRET deve ter pelo menos "+MIN_SECRET_BYTES+" caracteres (256 bits).");
        }
        if (properties.expiration() == null || properties.expiration().isZero()
        || properties.expiration().isNegative()) {
            throw new IllegalStateException("JWT_EXPIRATION deve ser uma duração positiva (ex.: 8h)");
        }

        this.key = Keys.hmacShaKeyFor(secret);
        this.expiration = properties.expiration();
        this.clock = clock;
    }

    public String generate(User user) {
        Instant now = clock.instant();

        return Jwts.builder()
                .subject(user.getId().toString())
                .claim(ROLE_CLAIM, user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiration)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public Optional<TokenData> validate(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(new TokenData(
                    UUID.fromString(claims.getSubject()),
                    Role.valueOf(claims.get(ROLE_CLAIM, String.class)),
                    claims.getExpiration().toInstant()
            ));
        }catch (JwtException | IllegalArgumentException | NullPointerException e) {
            return Optional.empty();
        }
    }
}
