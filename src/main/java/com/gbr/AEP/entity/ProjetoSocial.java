package com.gbr.AEP.entity;

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
public class ProjetoSocial {

    @Id
    private String id;

    @NotBlank(message = "O nome do projeto e obrigatorio")
    @Size(min = 3, max = 120, message = "O nome deve ter entre 3 e 120 caracteres")
    private String nome;

    @NotBlank(message = "A descricao e obrigatoria")
    @Size(min = 10, max = 1000, message = "A descricao deve ter entre 10 e 1000 caracteres")
    private String descricao;

    @NotBlank(message = "A organizacao responsavel e obrigatoria")
    @Size(max = 150, message = "A organizacao deve ter no maximo 150 caracteres")
    private String organizacaoResponsavel;

    @NotBlank(message = "O e-mail de contato e obrigatorio")
    @Email(message = "Informe um e-mail de contato valido")
    private String emailContato;

    @NotNull(message = "A data de inicio e obrigatoria")
    private LocalDate dataInicio;

    private LocalDate dataFim;

    @NotNull(message = "O status e obrigatorio")
    private StatusProjeto status;

    private LocalDateTime criadoEm;

    private LocalDateTime atualizadoEm;
}
