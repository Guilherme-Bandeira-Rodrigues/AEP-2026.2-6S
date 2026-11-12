package com.gbr.AEP.controller;

import com.gbr.AEP.dto.OrganizacaoRequest;
import com.gbr.AEP.dto.OrganizacaoResponse;
import com.gbr.AEP.service.OrganizacaoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = OrganizacaoController.class)
class OrganizacaoControllerTest {

    private static final String ID = "66db67818e72c12f8a3c9021";
    private static final OrganizacaoRequest DADOS = new OrganizacaoRequest(
            "Instituto Esperança", "Organização comunitária.", "contato@instituto.org");
    private static final OrganizacaoResponse RESPOSTA = new OrganizacaoResponse(
            ID, DADOS.nome(), DADOS.descricao(), DADOS.emailContato());
    private static final String JSON = """
            {"nome":"Instituto Esperança","descricao":"Organização comunitária.","emailContato":"contato@instituto.org"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrganizacaoService service;

    @Test
    void criaEConsultaOrganizacao() throws Exception {
        given(service.criar(DADOS)).willReturn(RESPOSTA);
        given(service.buscarPorId(ID)).willReturn(RESPOSTA);
        given(service.listarTodos()).willReturn(List.of(RESPOSTA));

        mockMvc.perform(post("/api/organizacoes").contentType(MediaType.APPLICATION_JSON).content(JSON))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(ID));
        mockMvc.perform(get("/api/organizacoes/{id}", ID))
                .andExpect(status().isOk()).andExpect(jsonPath("$.emailContato").value(DADOS.emailContato()));
        mockMvc.perform(get("/api/organizacoes"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(ID));
    }

    @Test
    void editaEExcluiOrganizacao() throws Exception {
        given(service.atualizar(ID, DADOS)).willReturn(RESPOSTA);

        mockMvc.perform(put("/api/organizacoes/{id}", ID).contentType(MediaType.APPLICATION_JSON).content(JSON))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nome").value(DADOS.nome()));
        mockMvc.perform(delete("/api/organizacoes/{id}", ID)).andExpect(status().isNoContent());
        then(service).should().excluir(ID);
    }

    @Test
    void rejeitaNomeDescricaoEEmailInvalidos() throws Exception {
        mockMvc.perform(post("/api/organizacoes").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":" ","emailContato":"invalido"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.nome[0]").isString())
                .andExpect(jsonPath("$.erros.descricao[0]").isString())
                .andExpect(jsonPath("$.erros.emailContato[0]").isString());
        then(service).shouldHaveNoInteractions();
    }

    @Test
    void responde409QuandoHaProjetosVinculados() throws Exception {
        willThrow(new ResponseStatusException(HttpStatus.CONFLICT, "A organização possui projetos vinculados"))
                .given(service).excluir(ID);

        mockMvc.perform(delete("/api/organizacoes/{id}", ID))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("A organização possui projetos vinculados"));
    }
}
