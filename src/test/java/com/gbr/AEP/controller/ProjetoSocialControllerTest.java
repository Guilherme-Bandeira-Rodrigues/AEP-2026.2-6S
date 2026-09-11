package com.gbr.AEP.controller;

import com.gbr.AEP.entity.ProjetoSocial;
import com.gbr.AEP.entity.StatusProjeto;
import com.gbr.AEP.service.ProjetoSocialService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ProjetoSocialController.class)
@DisplayName("ProjetoSocialController")
class ProjetoSocialControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProjetoSocialService service;

    private static ProjetoSocial projetoValido() {
        return ProjetoSocial.builder()
                .id("id-1")
                .nome("Educação para Todos")
                .descricao("Projeto de reforço escolar para crianças da comunidade.")
                .organizacaoResponsavel("Instituto Esperança")
                .emailContato("contato@instituto.org")
                .dataInicio(LocalDate.of(2026, 9, 10))
                .dataFim(LocalDate.of(2027, 6, 30))
                .status(StatusProjeto.PLANEJADO)
                .build();
    }

    @Test
    @DisplayName("deve criar projeto e retornar 201")
    void deveCriarProjeto() throws Exception {
        given(service.criar(any(ProjetoSocial.class))).willReturn(projetoValido());

        mockMvc.perform(post("/api/projetos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "Educação para Todos",
                                  "descricao": "Projeto de reforço escolar para crianças da comunidade.",
                                  "organizacaoResponsavel": "Instituto Esperança",
                                  "emailContato": "contato@instituto.org",
                                  "dataInicio": "2026-09-10",
                                  "dataFim": "2027-06-30",
                                  "status": "PLANEJADO"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("id-1"));

        then(service).should().criar(any(ProjetoSocial.class));
    }

    @Test
    @DisplayName("deve buscar projeto por id e retornar 200")
    void deveBuscarProjetoPorId() throws Exception {
        given(service.buscarPorId("id-1")).willReturn(projetoValido());

        mockMvc.perform(get("/api/projetos/{id}", "id-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Educação para Todos"));
    }

    @Test
    @DisplayName("deve excluir projeto e retornar 204")
    void deveExcluirProjeto() throws Exception {
        mockMvc.perform(delete("/api/projetos/{id}", "id-1"))
                .andExpect(status().isNoContent());

        then(service).should().excluir("id-1");
    }
}
