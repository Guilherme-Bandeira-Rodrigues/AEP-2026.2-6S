package com.gbr.AEP.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "projetos_sociais")
public class ProjetoSocial {

    @Id
    private String id;
    private String nome;
    private String descricao;
    private String organizacaoId;
    private String emailContato;
    private LocalDate dataInicio;
    private LocalDate dataFim;
    private StatusProjeto status;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;

    @Builder.Default
    private List<AtualizacaoProjeto> atualizacoes = List.of();
}
