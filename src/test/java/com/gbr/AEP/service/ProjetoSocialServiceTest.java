package com.gbr.AEP.service;

import com.gbr.AEP.dto.AtualizacaoProjetoRequest;
import com.gbr.AEP.dto.OrganizacaoResponse;
import com.gbr.AEP.dto.ProjetoSocialRequest;
import com.gbr.AEP.entity.AtualizacaoProjeto;
import com.gbr.AEP.entity.ProjetoSocial;
import com.gbr.AEP.entity.StatusProjeto;
import com.gbr.AEP.repository.ProjetoSocialRepository;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class ProjetoSocialServiceTest {

    private static final String ORGANIZACAO_ID = "66db67818e72c12f8a3c9021";
    private static final String PROJETO_ID = "id-1";
    private static final OrganizacaoResponse ORGANIZACAO = new OrganizacaoResponse(
            ORGANIZACAO_ID, "Instituto Esperança", "Organização comunitária", "contato@instituto.org");

    @Mock
    private ProjetoSocialRepository repository;

    @Mock
    private OrganizacaoService organizacoes;

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private ProjetoSocialService service;

    private static ProjetoSocialRequest dados(LocalDate inicio, LocalDate fim) {
        return new ProjetoSocialRequest("Educação para Todos", "Projeto de reforço escolar na comunidade.",
                ORGANIZACAO_ID, "projeto@instituto.org", inicio, fim, StatusProjeto.PLANEJADO);
    }

    private static ProjetoSocial projeto() {
        return ProjetoSocial.builder().id(PROJETO_ID).organizacaoId(ORGANIZACAO_ID)
                .nome("Educação para Todos").criadoEm(LocalDateTime.of(2026, 9, 1, 10, 0)).build();
    }

    private void permitirVinculo() {
        given(organizacoes.comOrganizacao(eq(ORGANIZACAO_ID), any())).willAnswer(invocacao ->
                invocacao.<Function<OrganizacaoResponse, ?>>getArgument(1).apply(ORGANIZACAO));
    }

    @Test
    void criaComVinculoETimestamps() {
        permitirVinculo();
        given(repository.save(any(ProjetoSocial.class))).willAnswer(invocacao -> invocacao.getArgument(0));

        LocalDate inicio = LocalDate.of(2026, 9, 10);
        var criado = service.criar(dados(inicio, inicio));

        assertThat(criado.organizacaoId()).isEqualTo(ORGANIZACAO_ID);
        assertThat(criado.organizacao()).isEqualTo(ORGANIZACAO);
        assertThat(criado.criadoEm()).isNotNull().isEqualTo(criado.atualizadoEm());
        assertThat(criado.atualizacoes()).isEmpty();
        ArgumentCaptor<ProjetoSocial> salvo = ArgumentCaptor.forClass(ProjetoSocial.class);
        then(repository).should().save(salvo.capture());
        assertThat(salvo.getValue().getId()).isNull();
    }

    @Test
    void aceitaProjetoSemDataFinal() {
        permitirVinculo();
        given(repository.save(any(ProjetoSocial.class))).willAnswer(invocacao -> invocacao.getArgument(0));

        assertThat(service.criar(dados(LocalDate.of(2026, 9, 10), null)).dataFim()).isNull();
    }

    @Test
    void rejeitaPeriodoInvertidoNaCriacaoEEdicao() {
        given(repository.existsById(PROJETO_ID)).willReturn(true);
        var invertido = dados(LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 9));

        assertThatExceptionOfType(ResponseStatusException.class)
                .isThrownBy(() -> service.criar(invertido))
                .matches(ex -> ex.getStatusCode() == HttpStatus.BAD_REQUEST);
        assertThatExceptionOfType(ResponseStatusException.class)
                .isThrownBy(() -> service.atualizar(PROJETO_ID, invertido))
                .matches(ex -> ex.getStatusCode() == HttpStatus.BAD_REQUEST);
        then(organizacoes).shouldHaveNoInteractions();
    }

    @Test
    void consultaProjetoComOrganizacaoEHistorico() {
        ProjetoSocial existente = projeto();
        var historico = List.of(new AtualizacaoProjeto(Instant.parse("2026-09-10T16:00:00Z"), "Início"));
        existente.setAtualizacoes(historico);
        given(repository.findById(PROJETO_ID)).willReturn(Optional.of(existente));
        given(organizacoes.buscarPorIds(List.of(ORGANIZACAO_ID))).willReturn(Map.of(ORGANIZACAO_ID, ORGANIZACAO));

        var resultado = service.buscarPorId(PROJETO_ID);

        assertThat(resultado.id()).isEqualTo(PROJETO_ID);
        assertThat(resultado.organizacao()).isEqualTo(ORGANIZACAO);
        assertThat(resultado.atualizacoes()).isEqualTo(historico);
    }

    @Test
    void mantemLegadoConsultavelSemVinculoOuHistorico() {
        ProjetoSocial legado = projeto();
        legado.setOrganizacaoId(null);
        legado.setAtualizacoes(null);
        given(repository.findById(PROJETO_ID)).willReturn(Optional.of(legado));
        given(mongoTemplate.find(any(Query.class), eq(ProjetoSocial.class))).willReturn(List.of(legado));

        var resultado = service.buscarPorId(PROJETO_ID);
        assertThat(resultado.organizacao()).isNull();
        assertThat(resultado.atualizacoes()).isEmpty();
        assertThat(service.listarTodos(null, null)).containsExactly(resultado);

        ArgumentCaptor<Query> consulta = ArgumentCaptor.forClass(Query.class);
        then(mongoTemplate).should().find(consulta.capture(), eq(ProjetoSocial.class));
        assertThat(consulta.getValue().getQueryObject()).isEmpty();
    }

    @Test
    void combinaNomeLiteralEStatusECarregaOrganizacoesEmLote() {
        given(mongoTemplate.find(any(Query.class), eq(ProjetoSocial.class))).willReturn(List.of(projeto(), projeto()));
        given(organizacoes.buscarPorIds(List.of(ORGANIZACAO_ID))).willReturn(Map.of(ORGANIZACAO_ID, ORGANIZACAO));

        assertThat(service.listarTodos(" Edu.* ", StatusProjeto.PLANEJADO))
                .extracting(resultado -> resultado.organizacao()).containsExactly(ORGANIZACAO, ORGANIZACAO);

        ArgumentCaptor<Query> consulta = ArgumentCaptor.forClass(Query.class);
        then(mongoTemplate).should().find(consulta.capture(), eq(ProjetoSocial.class));
        Document filtro = consulta.getValue().getQueryObject();
        assertThat(filtro.get("status")).isEqualTo(StatusProjeto.PLANEJADO);
        Pattern nome = (Pattern) filtro.get("nome");
        assertThat(nome.matcher("Projeto edu.*").find()).isTrue();
        assertThat(nome.matcher("Projeto educação").find()).isFalse();
    }

    @Test
    void editaCamposSemSobrescreverHistoricoOuCriacao() {
        ProjetoSocial existente = projeto();
        existente.setAtualizacoes(List.of(new AtualizacaoProjeto(Instant.EPOCH, "Início")));
        given(repository.existsById(PROJETO_ID)).willReturn(true);
        permitirVinculo();
        given(mongoTemplate.findAndModify(any(Query.class), any(Update.class),
                any(FindAndModifyOptions.class), eq(ProjetoSocial.class))).willReturn(existente);

        var resultado = service.atualizar(PROJETO_ID, dados(LocalDate.of(2026, 9, 10), null));

        assertThat(resultado.criadoEm()).isEqualTo(existente.getCriadoEm());
        assertThat(resultado.atualizacoes()).isEqualTo(existente.getAtualizacoes());
        ArgumentCaptor<Update> alteracoes = ArgumentCaptor.forClass(Update.class);
        then(mongoTemplate).should().findAndModify(any(Query.class), alteracoes.capture(),
                any(FindAndModifyOptions.class), eq(ProjetoSocial.class));
        Document campos = alteracoes.getValue().getUpdateObject().get("$set", Document.class);
        assertThat(campos).containsEntry("organizacaoId", ORGANIZACAO_ID)
                .doesNotContainKeys("atualizacoes", "criadoEm");
    }

    @Test
    void publicaAtualizacaoComDataDoServidorEGravacaoAtomica() {
        ProjetoSocial existente = projeto();
        existente.setOrganizacaoId(null);
        given(mongoTemplate.findAndModify(any(Query.class), any(Update.class),
                any(FindAndModifyOptions.class), eq(ProjetoSocial.class))).willReturn(existente);

        Instant antes = Instant.now().minusMillis(1);
        service.publicarAtualizacao(PROJETO_ID, new AtualizacaoProjetoRequest("  Primeira turma iniciada.  "));

        ArgumentCaptor<Update> alteracoes = ArgumentCaptor.forClass(Update.class);
        then(mongoTemplate).should().findAndModify(any(Query.class), alteracoes.capture(),
                any(FindAndModifyOptions.class), eq(ProjetoSocial.class));
        AtualizacaoProjeto publicada = alteracoes.getValue().getUpdateObject()
                .get("$push", Document.class).get("atualizacoes", AtualizacaoProjeto.class);
        assertThat(publicada.descricao()).isEqualTo("Primeira turma iniciada.");
        assertThat(publicada.data()).isBetween(antes, Instant.now());
    }

    @Test
    void excluiProjetoExistente() {
        given(mongoTemplate.findAndRemove(any(Query.class), eq(ProjetoSocial.class))).willReturn(projeto());
        service.excluir(PROJETO_ID);
        then(mongoTemplate).should().findAndRemove(any(Query.class), eq(ProjetoSocial.class));
    }

    @Test
    void responde404QuandoProjetoNaoExiste() {
        var dadosValidos = dados(LocalDate.of(2026, 9, 10), null);
        List<Runnable> operacoes = List.of(
                () -> service.buscarPorId(PROJETO_ID),
                () -> service.atualizar(PROJETO_ID, dadosValidos),
                () -> service.publicarAtualizacao(PROJETO_ID, new AtualizacaoProjetoRequest("Início")),
                () -> service.excluir(PROJETO_ID)
        );

        for (Runnable operacao : operacoes) {
            assertThatExceptionOfType(ResponseStatusException.class).isThrownBy(operacao::run)
                    .matches(ex -> ex.getStatusCode() == HttpStatus.NOT_FOUND);
        }
    }
}
