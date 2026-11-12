package com.gbr.AEP.dto;

import com.gbr.AEP.entity.StatusProjeto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.time.LocalDate;

@Builder(toBuilder = true)
public record ProjetoSocialRequest(
        @NotBlank(message = "O nome do projeto é obrigatório")
        @Size(min = 3, max = 120, message = "O nome deve ter entre 3 e 120 caracteres")
        @Schema(example = "Educação para Todos")
        String nome,

        @NotBlank(message = "A descrição é obrigatória")
        @Size(min = 10, max = 1000, message = "A descrição deve ter entre 10 e 1000 caracteres")
        @Schema(example = "Projeto de reforço escolar para crianças da comunidade.")
        String descricao,

        @NotBlank(message = "A organização responsável é obrigatória")
        @Pattern(regexp = "[0-9a-fA-F]{24}", message = "Informe um identificador de organização válido")
        @Schema(example = "66db67818e72c12f8a3c9021")
        String organizacaoId,

        @NotBlank(message = "O e-mail de contato é obrigatório")
        @Email(message = "Informe um e-mail de contato válido")
        @Schema(example = "contato@instituto.org", format = "email")
        String emailContato,

        @NotNull(message = "A data de início é obrigatória")
        @Schema(example = "2026-09-10")
        LocalDate dataInicio,

        @Schema(example = "2027-06-30", nullable = true)
        LocalDate dataFim,

        @NotNull(message = "O status é obrigatório")
        @Schema(example = "PLANEJADO")
        StatusProjeto status
) {
}
