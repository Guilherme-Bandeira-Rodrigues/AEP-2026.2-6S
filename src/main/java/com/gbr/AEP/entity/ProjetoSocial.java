package com.gbr.AEP.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "projetos_sociais")
@Schema(description = "Projeto social cadastrado na plataforma")
public class ProjetoSocial {

    @Id
    @Schema(description = "Identificador gerado pelo MongoDB", example = "66db67818e72c12f8a3c9021", accessMode = Schema.AccessMode.READ_ONLY)
    private String id;

    @NotBlank(message = "O nome do projeto e obrigatorio")
    @Size(min = 3, max = 120, message = "O nome deve ter entre 3 e 120 caracteres")
    @Schema(description = "Nome do projeto social", example = "Educação para Todos", minLength = 3, maxLength = 120)
    private String nome;

    @NotBlank(message = "A descricao e obrigatoria")
    @Size(min = 10, max = 1000, message = "A descricao deve ter entre 10 e 1000 caracteres")
    @Schema(description = "Descrição dos objetivos e atividades do projeto", example = "Projeto de reforço escolar para crianças da comunidade.", minLength = 10, maxLength = 1000)
    private String descricao;

    @NotBlank(message = "A organizacao responsavel e obrigatoria")
    @Size(max = 150, message = "A organizacao deve ter no maximo 150 caracteres")
    @Schema(description = "Organização responsável pelo projeto", example = "Instituto Esperança", maxLength = 150)
    private String organizacaoResponsavel;

    @NotBlank(message = "O e-mail de contato e obrigatorio")
    @Email(message = "Informe um e-mail de contato valido")
    @Schema(description = "E-mail para contato com o projeto", example = "contato@instituto.org", format = "email")
    private String emailContato;

    @NotNull(message = "A data de inicio e obrigatoria")
    @Schema(description = "Data de início do projeto", example = "2026-09-10", type = "string", format = "date")
    private LocalDate dataInicio;

    @Schema(description = "Data prevista ou efetiva de encerramento", example = "2027-06-30", type = "string", format = "date", nullable = true)
    private LocalDate dataFim;

    @NotNull(message = "O status e obrigatorio")
    @Schema(description = "Situação atual do projeto", example = "PLANEJADO", allowableValues = {"PLANEJADO", "EM_ANDAMENTO", "CONCLUIDO", "CANCELADO"})
    private StatusProjeto status;

    @Schema(description = "Data e hora de criação do cadastro", example = "2026-09-07T16:00:00", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime criadoEm;

    @Schema(description = "Data e hora da última atualização", example = "2026-09-07T16:30:00", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime atualizadoEm;
}
