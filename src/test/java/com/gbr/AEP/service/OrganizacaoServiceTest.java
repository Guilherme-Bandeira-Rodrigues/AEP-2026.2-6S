package com.gbr.AEP.service;

import com.gbr.AEP.dto.OrganizacaoRequest;
import com.gbr.AEP.entity.Organizacao;
import com.gbr.AEP.repository.OrganizacaoRepository;
import com.gbr.AEP.repository.ProjetoSocialRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class OrganizacaoServiceTest {

    private static final String ID = "66db67818e72c12f8a3c9021";
    private static final OrganizacaoRequest DADOS = new OrganizacaoRequest("Instituto Esperança",
            "Organização comunitária.", "contato@instituto.org");
    private static final Organizacao ORGANIZACAO = new Organizacao(ID, DADOS.nome(), DADOS.descricao(), DADOS.emailContato());

    @Mock
    private OrganizacaoRepository repository;

    @Mock
    private ProjetoSocialRepository projetos;

    @InjectMocks
    private OrganizacaoService service;

    @Test
    void cadastraComIdentificadorGeradoEPersisteEdicaoComMesmoIdentificador() {
        given(repository.save(new Organizacao(null, DADOS.nome(), DADOS.descricao(), DADOS.emailContato())))
                .willReturn(ORGANIZACAO);
        given(repository.findById(ID)).willReturn(Optional.of(ORGANIZACAO));
        Organizacao alterada = new Organizacao(ID, "Novo nome", DADOS.descricao(), DADOS.emailContato());
        given(repository.save(alterada)).willReturn(alterada);

        assertThat(service.criar(DADOS).id()).isEqualTo(ID);
        var edicao = service.atualizar(ID, new OrganizacaoRequest("Novo nome", DADOS.descricao(), DADOS.emailContato()));
        assertThat(edicao.id()).isEqualTo(ID);
        assertThat(edicao.nome()).isEqualTo("Novo nome");
    }

    @Test
    void consultaOrganizacaoIndividualEmListaEEmLote() {
        given(repository.findById(ID)).willReturn(Optional.of(ORGANIZACAO));
        given(repository.findAll()).willReturn(List.of(ORGANIZACAO));
        given(repository.findAllById(List.of(ID))).willReturn(List.of(ORGANIZACAO));

        var organizacao = service.buscarPorId(ID);
        assertThat(organizacao.emailContato()).isEqualTo(DADOS.emailContato());
        assertThat(service.listarTodos()).containsExactly(organizacao);
        assertThat(service.buscarPorIds(List.of(ID))).containsEntry(ID, organizacao);
        String idVinculado = service.comOrganizacao(ID, resposta -> resposta.id());
        assertThat(idVinculado).isEqualTo(ID);
    }

    @Test
    void bloqueiaExclusaoComProjetosVinculados() {
        given(repository.findById(ID)).willReturn(Optional.of(ORGANIZACAO));
        given(projetos.existsByOrganizacaoId(ID)).willReturn(true);

        assertThatExceptionOfType(ResponseStatusException.class).isThrownBy(() -> service.excluir(ID))
                .matches(ex -> ex.getStatusCode() == HttpStatus.CONFLICT);
        then(repository).should().findById(ID);
        then(repository).shouldHaveNoMoreInteractions();
    }

    @Test
    void excluiQuandoNaoHaVinculos() {
        given(repository.findById(ID)).willReturn(Optional.of(ORGANIZACAO));

        service.excluir(ID);

        then(repository).should().delete(ORGANIZACAO);
    }

    @Test
    void rejeitaVinculoComOrganizacaoInexistente() {
        assertThatExceptionOfType(ResponseStatusException.class)
                .isThrownBy(() -> service.comOrganizacao(ID, resposta -> resposta))
                .matches(ex -> ex.getStatusCode() == HttpStatus.NOT_FOUND);
    }
}
