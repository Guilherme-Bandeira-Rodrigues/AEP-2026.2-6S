package com.gbr.AEP.repository;

import com.gbr.AEP.entity.Organizacao;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface OrganizacaoRepository extends MongoRepository<Organizacao, String> {
    List<Organizacao> findByProprietarioId(String proprietarioId);
}
