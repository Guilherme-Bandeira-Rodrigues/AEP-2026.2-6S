package com.gbr.AEP.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OrganizacaoRequest(
        @NotBlank(message = "O nome da organização é obrigatório")
        @Size(max = 150, message = "O nome da organização deve ter no máximo 150 caracteres")
        @Schema(example = "Instituto Esperança")
        String nome,

        @NotBlank(message = "A descrição da organização é obrigatória")
        @Size(max = 1000, message = "A descrição da organização deve ter no máximo 1000 caracteres")
        @Schema(example = "Organização que apoia projetos de educação comunitária.")
        String descricao,

        @NotBlank(message = "O e-mail de contato é obrigatório")
        @Email(message = "Informe um e-mail de contato válido")
        @Schema(example = "contato@instituto.org", format = "email")
        String emailContato
) {
}
