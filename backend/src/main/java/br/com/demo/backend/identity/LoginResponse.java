package br.com.demo.backend.identity;

public record LoginResponse(
        String token,
        Role role
) {
}
