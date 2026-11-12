package com.gbr.AEP.dto;

import com.gbr.AEP.entity.Organizacao;

public record OrganizacaoResponse(String id, String nome, String descricao, String emailContato) {

    public static OrganizacaoResponse de(Organizacao organizacao) {
        return new OrganizacaoResponse(organizacao.getId(), organizacao.getNome(),
                organizacao.getDescricao(), organizacao.getEmailContato());
    }
}
