package com.gbr.AEP.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "organizacoes")
public class Organizacao {

    @Id
    private String id;
    private String nome;
    private String descricao;
    private String emailContato;
}
