package br.com.demo.backend.identity;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "Informe o e-mail") @Email(message = "E-mail inválido")
        @Size(max = 160, message = "E-mail muito longo") String email,
        @NotBlank(message = "Informe a senha")
        @Size(max = 72, message = "Senha muito longa") String password) {

        public LoginRequest {
                if (email != null) {
                        email = email.trim();
                }
        }
}