package com.gbr.AEP.controller;

import com.gbr.AEP.dto.AtualizacaoProjetoRequest;
import com.gbr.AEP.dto.OrganizacaoResponse;
import com.gbr.AEP.dto.ProjetoSocialRequest;
import com.gbr.AEP.dto.ProjetoSocialResponse;
import com.gbr.AEP.entity.AtualizacaoProjeto;
import com.gbr.AEP.entity.ProjetoSocial;
import com.gbr.AEP.entity.StatusProjeto;
import com.gbr.AEP.service.ProjetoSocialService;
import org.junit.jupiter.api.Test;
import com.gbr.AEP.config.SecurityConfig;
import com.gbr.AEP.security.ApiSecurityHandler;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import({SecurityConfig.class, ApiSecurityHandler.class})
@WithMockUser(username = "usuario-1")
@WebMvcTest(controllers = ProjetoSocialController.class)
class ProjetoSocialControllerTest {

    private static final String ID = "id-1";
    private static final String ORGANIZACAO_ID = "66db67818e72c12f8a3c9021";
    private static final String DADOS = """
            {
              "nome": "Educação para Todos",
              "descricao": "Projeto de reforço escolar para crianças da comunidade.",
              "organizacaoId": "66db67818e72c12f8a3c9021",
              "emailContato": "projeto@instituto.org",
              "dataInicio": "2026-09-10",
              "status": "PLANEJADO"
            }
            """;

    @MockitoBean
    private UserDetailsService usuarios;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProjetoSocialService service;

    private static ProjetoSocialResponse projetoComHistorico() {
        var organizacao = new OrganizacaoResponse(ORGANIZACAO_ID, "Instituto Esperança",
                "Organização comunitária", "contato@instituto.org");
        var projeto = ProjetoSocial.builder().id(ID).organizacaoId(ORGANIZACAO_ID)
                .nome("Educação para Todos").dataInicio(LocalDate.of(2026, 9, 10)).status(StatusProjeto.PLANEJADO)
                .atualizacoes(List.of(new AtualizacaoProjeto(Instant.parse("2026-09-10T16:00:00Z"), "Início")))
                .build();
        return ProjetoSocialResponse.de(projeto, organizacao);
    }

    @Test
    void criaProjetoComOrganizacao() throws Exception {
        given(service.criar(any(ProjetoSocialRequest.class))).willReturn(projetoComHistorico());

        mockMvc.perform(post("/api/projetos").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(DADOS))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(ID))
                .andExpect(jsonPath("$.organizacao.id").value(ORGANIZACAO_ID));
    }

    @Test
    void validaOrganizacaoObrigatoriaEFormatoDoId() throws Exception {
        for (String invalido : List.of("", "identificador-invalido")) {
            mockMvc.perform(post("/api/projetos").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                            .content(DADOS.replace(ORGANIZACAO_ID, invalido)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.erros.organizacaoId[0]").isString());
        }
        then(service).shouldHaveNoInteractions();
    }

    @Test
    void exibeMotivoDaFalhaDeNegocio() throws Exception {
        given(service.criar(any(ProjetoSocialRequest.class))).willThrow(
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Organização não encontrada"));

        mockMvc.perform(post("/api/projetos").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(DADOS))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Organização não encontrada"));
    }

    @Test
    void buscaProjetoComOrganizacaoEHistorico() throws Exception {
        given(service.buscarPorId(ID)).willReturn(projetoComHistorico());

        mockMvc.perform(get("/api/projetos/{id}", ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organizacao.nome").value("Instituto Esperança"))
                .andExpect(jsonPath("$.atualizacoes[0].data").value("2026-09-10T16:00:00Z"))
                .andExpect(jsonPath("$.atualizacoes[0].descricao").value("Início"));
    }

    @Test
    void combinaFiltros() throws Exception {
        given(service.listarTodos("educação", StatusProjeto.PLANEJADO)).willReturn(List.of(projetoComHistorico()));

        mockMvc.perform(get("/api/projetos").param("nome", "educação").param("status", "PLANEJADO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(ID));
    }

    @Test
    void rejeitaStatusInvalido() throws Exception {
        mockMvc.perform(get("/api/projetos").param("status", "INVALIDO"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Valor inválido para o parâmetro 'status'."));
    }

    @Test
    void rejeitaJsonComDataMalformada() throws Exception {
        mockMvc.perform(post("/api/projetos").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(DADOS.replace("2026-09-10", "10/09/2026")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").isString());
    }

    @Test
    void editaProjeto() throws Exception {
        given(service.atualizar(eq(ID), any(ProjetoSocialRequest.class))).willReturn(projetoComHistorico());

        mockMvc.perform(put("/api/projetos/{id}", ID).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(DADOS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.atualizacoes[0].descricao").value("Início"));
    }

    @Test
    void publicaAtualizacaoSemAceitarDataDoCliente() throws Exception {
        var entrada = new AtualizacaoProjetoRequest("Início");
        given(service.publicarAtualizacao(ID, entrada)).willReturn(projetoComHistorico());

        mockMvc.perform(post("/api/projetos/{id}/atualizacoes", ID).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"descricao":"Início","data":"1990-01-01T00:00:00Z"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.atualizacoes[0].data").value("2026-09-10T16:00:00Z"));
        then(service).should().publicarAtualizacao(ID, entrada);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"descricao\":\"  \"}"})
    void rejeitaDescricaoAusenteOuEmBranco(String dados) throws Exception {
        mockMvc.perform(post("/api/projetos/{id}/atualizacoes", ID).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(dados))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.descricao[0]").isString());
        then(service).shouldHaveNoInteractions();
    }

    @Test
    void excluiProjeto() throws Exception {
        mockMvc.perform(delete("/api/projetos/{id}", ID).with(csrf())).andExpect(status().isNoContent());

        then(service).should().excluir(ID);
    }
}
