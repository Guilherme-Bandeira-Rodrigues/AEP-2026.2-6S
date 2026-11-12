package com.gbr.AEP.controller;

import com.gbr.AEP.config.SecurityConfig;
import com.gbr.AEP.entity.Usuario;
import com.gbr.AEP.repository.UsuarioRepository;
import com.gbr.AEP.security.ApiSecurityHandler;
import com.gbr.AEP.security.UsuarioAtual;
import com.gbr.AEP.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, ApiSecurityHandler.class, UsuarioService.class, UsuarioAtual.class})
class AuthControllerTest {
    private static final String EMAIL = "pessoa@exemplo.org";
    private static final String CADASTRO = """
            {"nome":"  Ana Silva  ","email":" PESSOA@EXEMPLO.ORG ","senha":"Senha123!"}
            """;
    private static final String LOGIN = """
            {"email":"PESSOA@EXEMPLO.ORG","senha":"Senha123!"}
            """;

    @Autowired private MockMvc mvc;
    @Autowired private PasswordEncoder encoder;
    @MockitoBean private UsuarioRepository repository;
    @MockitoBean private MongoTemplate mongo;

    @BeforeEach
    void indice() {
        given(mongo.indexOps(Usuario.class)).willReturn(mock(IndexOperations.class));
    }

    @Test
    void cadastraComHashSemExporSenhaNemAutenticarAutomaticamente() throws Exception {
        given(repository.insert(any(Usuario.class))).willAnswer(invocacao -> {
            Usuario usuario = invocacao.getArgument(0);
            assertThat(usuario.getEmail()).isEqualTo(EMAIL);
            assertThat(usuario.getNome()).isEqualTo("Ana Silva");
            assertThat(usuario.getSenhaHash()).isNotEqualTo("Senha123!");
            assertThat(encoder.matches("Senha123!", usuario.getSenhaHash())).isTrue();
            usuario.setId("usuario-1");
            return usuario;
        });
        mvc.perform(post("/api/auth/cadastro").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(CADASTRO))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value("usuario-1"))
                .andExpect(jsonPath("$.senha").doesNotExist()).andExpect(jsonPath("$.senhaHash").doesNotExist());
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void rejeitaEmailDuplicadoDadosInvalidosESenhaMaiorQueLimiteBcrypt() throws Exception {
        given(repository.insert(any(Usuario.class))).willThrow(new DuplicateKeyException("email"));
        mvc.perform(post("/api/auth/cadastro").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(CADASTRO))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/auth/cadastro").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"  \",\"email\":\"invalido\",\"senha\":\"123\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.erros.nome").isArray())
                .andExpect(jsonPath("$.erros.email").isArray()).andExpect(jsonPath("$.erros.senha").isArray());
        mvc.perform(post("/api/auth/cadastro").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(CADASTRO.replace("Senha123!", "á".repeat(37))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void autenticaRenovaIdentificadorConsultaSessaoEEncerraNoLogout() throws Exception {
        Usuario usuario = new Usuario("usuario-1", "Ana", EMAIL, encoder.encode("Senha123!"));
        given(repository.findByEmail(EMAIL)).willReturn(Optional.of(usuario));
        given(repository.findById("usuario-1")).willReturn(Optional.of(usuario));
        MockHttpSession sessao = new MockHttpSession();
        String anterior = sessao.getId();
        mvc.perform(post("/api/auth/login").session(sessao).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(LOGIN))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.senhaHash").doesNotExist());
        assertThat(sessao.getId()).isNotEqualTo(anterior);
        mvc.perform(get("/api/auth/me").session(sessao)).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("usuario-1"));
        mvc.perform(post("/api/auth/logout").session(sessao).with(csrf())).andExpect(status().isNoContent());
        assertThat(sessao.isInvalid()).isTrue();
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void rejeitaCredenciaisIncorretasComMensagemGenerica() throws Exception {
        mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(LOGIN))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.detail").isString());
        given(repository.findByEmail(EMAIL)).willReturn(Optional.of(
                new Usuario("usuario-1", "Ana", EMAIL, encoder.encode("OutraSenha!"))));
        mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(LOGIN))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void exigeCsrfNoCadastroLoginELogout() throws Exception {
        mvc.perform(get("/api/auth/csrf")).andExpect(status().isOk())
                .andExpect(jsonPath("$.headerName").value("X-CSRF-TOKEN"))
                .andExpect(jsonPath("$.token").isNotEmpty());
        for (String rota : new String[]{"cadastro", "login", "logout"}) {
            mvc.perform(post("/api/auth/" + rota).contentType(MediaType.APPLICATION_JSON).content(CADASTRO))
                    .andExpect(status().isForbidden()).andExpect(content().contentTypeCompatibleWith("application/problem+json"));
        }
        then(repository).shouldHaveNoInteractions();
    }

    @Test
    void rejeitaCamposAusentesSemExecutarPersistencia() throws Exception {
        for (String rota : new String[]{"cadastro", "login"}) {
            mvc.perform(post("/api/auth/" + rota).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.erros.email").isArray())
                    .andExpect(jsonPath("$.erros.senha").isArray());
        }
        then(repository).shouldHaveNoInteractions();
    }

    @Test
    void rejeitaSessaoCujoUsuarioNaoExisteMais() throws Exception {
        mvc.perform(get("/api/auth/me").with(
                        org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("removido")))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.detail").value("Sessão inválida. Entre novamente"));
    }

    @Test
    void permiteCorsConfiguradoERejeitaOutraOrigem() throws Exception {
        mvc.perform(options("/api/auth/login").header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type,x-csrf-token"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
        mvc.perform(options("/api/auth/login").header("Origin", "https://outro.exemplo")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }
}
