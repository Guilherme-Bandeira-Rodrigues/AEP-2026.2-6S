package com.gbr.AEP.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AtualizacaoProjetoRequest(
        @NotBlank(message = "A descrição da atualização é obrigatória")
        @Size(max = 1000, message = "A descrição da atualização deve ter no máximo 1000 caracteres")
        @Schema(example = "A primeira turma iniciou as atividades.")
        String descricao
) {
}
