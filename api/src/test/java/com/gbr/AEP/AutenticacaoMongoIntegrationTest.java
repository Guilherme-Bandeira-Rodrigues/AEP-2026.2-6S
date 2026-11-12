package com.gbr.AEP;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import static com.mongodb.client.model.Filters.eq;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "AEP_MONGODB_TEST_URI", matches = ".+")
class AutenticacaoMongoIntegrationTest {
    private static final String BANCO = "aep_auth_teste_" + UUID.randomUUID().toString().replace("-", "");
    private static final String SENHA = "SenhaDeTeste123!";

    @Autowired private MockMvc mvc;
    @Autowired private MongoTemplate mongo;
    @Autowired private PasswordEncoder encoder;

    @DynamicPropertySource
    static void bancoDeTeste(DynamicPropertyRegistry registro) {
        registro.add("spring.mongodb.uri", () -> System.getenv("AEP_MONGODB_TEST_URI"));
        registro.add("spring.mongodb.database", () -> BANCO);
    }

    @AfterAll
    static void limpar(@Autowired MongoTemplate mongo) {
        if (BANCO.equals(mongo.getDb().getName())) {
            mongo.getDb().drop();
        }
    }

    @Test
    void fluxoRealComSessaoCsrfPersistenciaEIsolamentoEntreContas() throws Exception {
        Cliente ana = new Cliente();
        Cliente bruno = new Cliente();
        String anaId = ana.cadastrarEEntrar("ana@exemplo.org");
        bruno.cadastrarEEntrar("bruno@exemplo.org");

        var salvo = mongo.getCollection("usuarios").find(eq("email", "ana@exemplo.org")).first();
        assertThat(salvo).isNotNull().doesNotContainKey("senha");
        assertThat(encoder.matches(SENHA, salvo.getString("senhaHash"))).isTrue();

        String organizacao = id(ana.enviar(post("/api/organizacoes"), """
                {"nome":"Instituto Ana","descricao":"Organização comunitária.","emailContato":"ana@exemplo.org",
                 "proprietarioId":"forjado"}
                """, 201));
        assertThat(mongo.getCollection("organizacoes").find(eq("proprietarioId", anaId)).first()).isNotNull();
        String outraOrganizacao = id(bruno.enviar(post("/api/organizacoes"), """
                {"nome":"Instituto Bruno","descricao":"Outra organização.","emailContato":"bruno@exemplo.org"}
                """, 201));
        String projeto = id(ana.enviar(post("/api/projetos"), projeto(organizacao), 201));

        mvc.perform(get("/api/projetos/{id}", projeto)).andExpect(status().isOk())
                .andExpect(jsonPath("$.organizacao.nome").value("Instituto Ana"))
                .andExpect(jsonPath("$.organizacao.proprietarioId").doesNotExist());
        mvc.perform(get("/api/me/projetos")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/projetos").session(new MockHttpSession()))
                .andExpect(status().isForbidden());

        ana.enviar(get("/api/me/organizacoes"), null, 200);
        assertThat(JsonPath.<List<String>>read(ana.enviar(get("/api/me/projetos"), null, 200), "$[*].id"))
                .containsExactly(projeto);
        assertThat(JsonPath.<List<String>>read(bruno.enviar(get("/api/me/projetos"), null, 200), "$[*].id"))
                .isEmpty();
        assertThat(JsonPath.<List<String>>read(bruno.enviar(get("/api/me/organizacoes"), null, 200), "$[*].id"))
                .containsExactly(outraOrganizacao);

        bruno.enviar(put("/api/organizacoes/{id}", organizacao), """
                {"nome":"Invadida","descricao":"Tentativa de edição.","emailContato":"bruno@exemplo.org"}
                """, 403);
        bruno.enviar(delete("/api/organizacoes/{id}", organizacao), null, 403);
        bruno.enviar(post("/api/projetos"), projeto(organizacao), 403);
        bruno.enviar(put("/api/projetos/{id}", projeto), projeto(outraOrganizacao), 403);
        bruno.enviar(delete("/api/projetos/{id}", projeto), null, 403);
        bruno.enviar(post("/api/projetos/{id}/atualizacoes", projeto), "{\"descricao\":\"Indevida\"}", 403);
        ana.enviar(put("/api/projetos/{id}", projeto), projeto(outraOrganizacao), 403);
        ana.enviar(delete("/api/organizacoes/{id}", organizacao), null, 409);

        String publicado = ana.enviar(post("/api/projetos/{id}/atualizacoes", projeto),
                "{\"descricao\":\"Primeira oficina realizada.\"}", 201);
        assertThat(JsonPath.<String>read(publicado, "$.id")).isEqualTo(projeto);
        assertThat(JsonPath.<List<String>>read(publicado, "$.atualizacoes[*].descricao"))
                .containsExactly("Primeira oficina realizada.");
        ana.enviar(put("/api/projetos/{id}", projeto), projeto(organizacao), 200);
        mvc.perform(get("/api/projetos/{id}", projeto)).andExpect(status().isOk())
                .andExpect(jsonPath("$.atualizacoes.length()").value(1));
        ana.enviar(delete("/api/projetos/{id}", projeto), null, 204);
        ana.enviar(delete("/api/organizacoes/{id}", organizacao), null, 204);
        ana.enviar(post("/api/auth/logout"), null, 204);
        assertThat(ana.sessao.isInvalid()).isTrue();
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void indiceUnicoImpedeEmailsDuplicadosEmCadastrosConcorrentes() throws Exception {
        Callable<Integer> cadastro = () -> {
            Cliente cliente = new Cliente();
            cliente.renovarCsrf();
            return mvc.perform(post("/api/auth/cadastro").session(cliente.sessao)
                            .header("X-CSRF-TOKEN", cliente.token).contentType(MediaType.APPLICATION_JSON)
                            .content(cadastro("CONCORRENTE@EXEMPLO.ORG")))
                    .andReturn().getResponse().getStatus();
        };
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var resultados = executor.invokeAll(List.of(cadastro, cadastro));
            assertThat(List.of(resultados.get(0).get(), resultados.get(1).get()))
                    .containsExactlyInAnyOrder(201, 409);
        }
        assertThat(mongo.getCollection("usuarios").countDocuments(eq("email", "concorrente@exemplo.org")))
                .isEqualTo(1);
    }

    private static String id(String json) {
        return JsonPath.read(json, "$.id");
    }

    private static String cadastro(String email) {
        return """
                {"nome":"Pessoa Teste","email":"%s","senha":"%s"}
                """.formatted(email, SENHA);
    }

    private static String projeto(String organizacaoId) {
        return """
                {"nome":"Oficina Comunitária","descricao":"Oficinas abertas à comunidade.",
                 "organizacaoId":"%s","emailContato":"projeto@exemplo.org", "dataInicio":"2026-11-01","status":"PLANEJADO"}
                """.formatted(organizacaoId);
    }

    private class Cliente {
        private MockHttpSession sessao;
        private String token;

        void renovarCsrf() throws Exception {
            var request = get("/api/auth/csrf");
            if (sessao != null) {
                request.session(sessao);
            }
            MvcResult resultado = mvc.perform(request).andExpect(status().isOk()).andReturn();
            sessao = (MockHttpSession) resultado.getRequest().getSession();
            token = JsonPath.read(resultado.getResponse().getContentAsString(), "$.token");
        }

        String cadastrarEEntrar(String email) throws Exception {
            renovarCsrf();
            String usuarioId = id(enviar(post("/api/auth/cadastro"), cadastro(email), 201));
            String antiga = sessao.getId();
            enviar(post("/api/auth/login"), "{\"email\":\"%s\",\"senha\":\"%s\"}".formatted(email, SENHA), 200);
            assertThat(sessao.getId()).isNotEqualTo(antiga);
            // O token anterior ao login deve ser invalidado, mesmo com a nova sessão autenticada.
            enviar(post("/api/organizacoes"), "{}", 403);
            renovarCsrf();
            assertThat(id(enviar(get("/api/auth/me"), null, 200))).isEqualTo(usuarioId);
            return usuarioId;
        }

        String enviar(MockHttpServletRequestBuilder request, String corpo, int statusEsperado) throws Exception {
            request.session(sessao).header("X-CSRF-TOKEN", token);
            if (corpo != null) {
                request.contentType(MediaType.APPLICATION_JSON).content(corpo);
            }
            return mvc.perform(request).andExpect(status().is(statusEsperado)).andReturn().getResponse().getContentAsString();
        }
    }
}
