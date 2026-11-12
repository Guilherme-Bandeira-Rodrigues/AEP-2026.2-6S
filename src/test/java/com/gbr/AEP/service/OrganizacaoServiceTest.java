package com.gbr.AEP.service;

import com.gbr.AEP.dto.OrganizacaoRequest;
import com.gbr.AEP.entity.Organizacao;
import com.gbr.AEP.repository.OrganizacaoRepository;
import com.gbr.AEP.security.UsuarioAtual;
import org.junit.jupiter.api.BeforeEach;
import static org.mockito.Mockito.lenient;
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
    private static final Organizacao ORGANIZACAO = new Organizacao(ID, DADOS.nome(), DADOS.descricao(), DADOS.emailContato(), "usuario-1");

    @Mock
    private OrganizacaoRepository repository;

    @Mock
    private ProjetoSocialRepository projetos;

    @Mock
    private UsuarioAtual usuarioAtual;

    @BeforeEach
    void autenticar() {
        lenient().when(usuarioAtual.id()).thenReturn("usuario-1");
    }

    @InjectMocks
    private OrganizacaoService service;

    @Test
    void cadastraComIdentificadorGeradoEPersisteEdicaoComMesmoIdentificador() {
        given(repository.save(new Organizacao(null, DADOS.nome(), DADOS.descricao(), DADOS.emailContato(), "usuario-1")))
                .willReturn(ORGANIZACAO);
        given(repository.findById(ID)).willReturn(Optional.of(ORGANIZACAO));
        Organizacao alterada = new Organizacao(ID, "Novo nome", DADOS.descricao(), DADOS.emailContato(), "usuario-1");
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

    @Test
    void listaSomenteOrganizacoesDoProprietario() {
        given(repository.findByProprietarioId("usuario-1")).willReturn(List.of(ORGANIZACAO));
        assertThat(service.listarMinhas()).extracting(resposta -> resposta.id()).containsExactly(ID);
        then(repository).should().findByProprietarioId("usuario-1");
    }

    @Test
    void rejeitaAlteracoesDeOutraContaEDadosLegadosSemProprietario() {
        for (String proprietario : new String[]{"outra-conta", null}) {
            given(repository.findById(ID)).willReturn(Optional.of(new Organizacao(
                    ID, DADOS.nome(), DADOS.descricao(), DADOS.emailContato(), proprietario)));
            List<Runnable> operacoes = List.of(
                    () -> service.atualizar(ID, DADOS),
                    () -> service.excluir(ID),
                    () -> service.validarProprietario(ID),
                    () -> service.comOrganizacao(ID, resposta -> resposta));
            for (Runnable operacao : operacoes) {
                assertThatExceptionOfType(ResponseStatusException.class).isThrownBy(operacao::run)
                        .matches(ex -> ex.getStatusCode() == HttpStatus.FORBIDDEN);
            }
        }
        then(projetos).shouldHaveNoInteractions();
    }

    @Test
    void rejeitaProjetoSemOrganizacaoAntesDeConsultarBanco() {
        assertThatExceptionOfType(ResponseStatusException.class)
                .isThrownBy(() -> service.validarProprietario(null))
                .matches(ex -> ex.getStatusCode() == HttpStatus.FORBIDDEN);
        then(repository).shouldHaveNoInteractions();
    }
}
