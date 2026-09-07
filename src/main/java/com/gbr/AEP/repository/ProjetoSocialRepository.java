package com.gbr.AEP.repository;

import com.gbr.AEP.entity.ProjetoSocial;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProjetoSocialRepository extends MongoRepository<ProjetoSocial, String> {
}
