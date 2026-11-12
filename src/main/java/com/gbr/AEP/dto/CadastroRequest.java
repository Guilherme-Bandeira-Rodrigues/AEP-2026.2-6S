package com.gbr.AEP.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Locale;

public record CadastroRequest(
        @NotBlank @Size(min = 3, max = 100) String nome,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 8, max = 72) String senha
) {
    public CadastroRequest {
        nome = nome == null ? null : nome.strip();
        email = email == null ? null : email.strip().toLowerCase(Locale.ROOT);
    }

    @Override
    public String toString() {
        return "CadastroRequest[credenciais omitidas]";
    }
}
