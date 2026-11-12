package com.gbr.AEP.dto;

import com.gbr.AEP.entity.AtualizacaoProjeto;
import com.gbr.AEP.entity.ProjetoSocial;
import com.gbr.AEP.entity.StatusProjeto;

import java.time.LocalDate;
import java.time.Instant;

import java.util.List;

public record ProjetoSocialResponse(
        String id,
        String nome,
        String descricao,
        String organizacaoId,
        OrganizacaoResponse organizacao,
        String emailContato,
        LocalDate dataInicio,
        LocalDate dataFim,
        StatusProjeto status,
        Instant criadoEm,
        Instant atualizadoEm,
        List<AtualizacaoProjeto> atualizacoes
) {
    public static ProjetoSocialResponse de(ProjetoSocial projeto, OrganizacaoResponse organizacao) {
        return new ProjetoSocialResponse(
                projeto.getId(), projeto.getNome(), projeto.getDescricao(), projeto.getOrganizacaoId(),
                organizacao, projeto.getEmailContato(), projeto.getDataInicio(), projeto.getDataFim(),
                projeto.getStatus(), projeto.getCriadoEm(), projeto.getAtualizadoEm(),
                projeto.getAtualizacoes() == null ? List.of() : List.copyOf(projeto.getAtualizacoes())
        );
    }
}
