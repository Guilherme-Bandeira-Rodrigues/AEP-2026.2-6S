package com.gbr.AEP.repository;

import com.gbr.AEP.entity.Organizacao;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface OrganizacaoRepository extends MongoRepository<Organizacao, String> {
}
