package com.gbr.AEP;

import com.jayway.jsonpath.JsonPath;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import static com.mongodb.client.model.Filters.eq;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "AEP_MONGODB_TEST_URI", matches = ".+")
class MongoFluxoIntegrationTest {

    private static final String BANCO = "aep_teste_" + UUID.randomUUID().toString().replace("-", "");

    @Autowired
    private MockMvc mvc;

    @Autowired
    private MongoTemplate mongo;

    @DynamicPropertySource
    static void bancoDeTeste(DynamicPropertyRegistry registro) {
        registro.add("spring.mongodb.uri", () -> System.getenv("AEP_MONGODB_TEST_URI"));
        registro.add("spring.mongodb.database", () -> BANCO);
    }

    @AfterAll
    static void removeBancoTemporario(@Autowired MongoTemplate mongo) {
        if (BANCO.equals(mongo.getDb().getName())) {
            mongo.getDb().drop();
        }
    }

    @Test
    void persisteVinculoEHistoricoSemPerderAtualizacoesConcorrentes() throws Exception {
        String organizacaoId = cadastrarOrganizacao();
        String projetoId = JsonPath.read(mvc.perform(post("/api/projetos")
                        .contentType(MediaType.APPLICATION_JSON).content(projeto(organizacaoId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.atualizacoes").isEmpty())
                .andReturn().getResponse().getContentAsString(), "$.id");

        mvc.perform(delete("/api/organizacoes/{id}", organizacaoId)).andExpect(status().isConflict());

        Callable<Void> editar = () -> {
            mvc.perform(put("/api/projetos/{id}", projetoId).contentType(MediaType.APPLICATION_JSON)
                            .content(projeto(organizacaoId).replace("PLANEJADO", "EM_ANDAMENTO")))
                    .andExpect(status().isOk());
            return null;
        };
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var tarefas = List.of(publicar(projetoId, "Primeira turma iniciada."),
                    publicar(projetoId, "Segunda turma iniciada."), editar);
            for (var tarefa : executor.invokeAll(tarefas)) {
                tarefa.get();
            }
        }

        mvc.perform(get("/api/projetos").param("nome", "EDU.*").param("status", "EM_ANDAMENTO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(projetoId))
                .andExpect(jsonPath("$[0].organizacao.nome").value("Instituto"))
                .andExpect(jsonPath("$[0].atualizacoes.length()").value(2));
        mvc.perform(get("/api/projetos").param("nome", "EDU.*").param("status", "PLANEJADO"))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());

        Document salvo = mongo.getCollection("projetos_sociais").find(eq("_id", new ObjectId(projetoId))).first();
        assertThat(salvo).isNotNull();
        assertThat(salvo.get("organizacaoId")).isEqualTo(organizacaoId);
        assertThat(salvo.getList("atualizacoes", Document.class))
                .extracting(entrada -> entrada.getString("descricao"))
                .containsExactlyInAnyOrder("Primeira turma iniciada.", "Segunda turma iniciada.");
        assertThat(salvo.getList("atualizacoes", Document.class))
                .allSatisfy(entrada -> assertThat(entrada.getDate("data")).isAfter(Date.from(Instant.EPOCH)));

        mvc.perform(delete("/api/projetos/{id}", projetoId)).andExpect(status().isNoContent());
        mvc.perform(delete("/api/organizacoes/{id}", organizacaoId)).andExpect(status().isNoContent());
    }

    @Test
    void permiteConsultarEReassociarLegadoSemHistorico() throws Exception {
        ObjectId projetoId = new ObjectId();
        mongo.getCollection("projetos_sociais").insertOne(new Document("_id", projetoId)
                .append("nome", "Projeto legado")
                .append("organizacaoResponsavel", "Instituto"));

        mvc.perform(get("/api/projetos/{id}", projetoId.toHexString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organizacao").isEmpty())
                .andExpect(jsonPath("$.atualizacoes").isEmpty());

        String organizacaoId = cadastrarOrganizacao();
        mvc.perform(put("/api/projetos/{id}", projetoId.toHexString()).contentType(MediaType.APPLICATION_JSON)
                        .content(projeto(organizacaoId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organizacaoId").value(organizacaoId));
        publicar(projetoId.toHexString(), "Atualização do projeto legado.").call();

        Document migrado = mongo.getCollection("projetos_sociais").find(eq("_id", projetoId)).first();
        assertThat(migrado).doesNotContainKey("organizacaoResponsavel");
        assertThat(migrado.getList("atualizacoes", Document.class)).hasSize(1);
    }

    private Callable<Void> publicar(String id, String descricao) {
        return () -> {
            mvc.perform(post("/api/projetos/{id}/atualizacoes", id).contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"descricao":"%s"}
                                    """.formatted(descricao)))
                    .andExpect(status().isCreated());
            return null;
        };
    }

    private String cadastrarOrganizacao() throws Exception {
        return JsonPath.read(mvc.perform(post("/api/organizacoes").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Instituto","descricao":"Organização comunitária.","emailContato":"contato@instituto.org"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), "$.id");
    }

    private String projeto(String organizacaoId) {
        return """
                {
                  "nome": "Projeto Edu.*",
                  "descricao": "Projeto comunitário de reforço escolar.",
                  "organizacaoId": "%s",
                  "emailContato": "projeto@instituto.org",
                  "dataInicio": "2026-09-10",
                  "status": "PLANEJADO"
                }
                """.formatted(organizacaoId);
    }
}
