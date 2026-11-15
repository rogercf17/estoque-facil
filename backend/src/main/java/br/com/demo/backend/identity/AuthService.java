package br.com.demo.backend.identity;

import br.com.demo.backend.shared.UnauthorizedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

@Service
public class AuthService {
    static final String INVALID_CREDENTIALS = "E-mail ou senha inválidos";

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final String dummyHash;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.dummyHash = passwordEncoder.encode("senha-descartavel");
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        Optional<User> found = users.findByEmail(request.email().trim());

        if (found.isEmpty()) {
            passwordEncoder.matches(request.password(), dummyHash);
            throw new UnauthorizedException(INVALID_CREDENTIALS);
        }

        User user = found.get();
        boolean passwordOk = passwordEncoder.matches(request.password(), user.getPasswordHash());
        if (!passwordOk || !user.isActive()) {
            throw new UnauthorizedException(INVALID_CREDENTIALS);
        }

        return new LoginResponse(jwtService.generate(user), user.getRole());
    }
}
