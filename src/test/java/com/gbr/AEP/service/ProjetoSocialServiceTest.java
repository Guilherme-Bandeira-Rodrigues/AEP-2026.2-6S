package com.gbr.AEP.service;

import com.gbr.AEP.entity.ProjetoSocial;
import com.gbr.AEP.entity.StatusProjeto;
import com.gbr.AEP.repository.ProjetoSocialRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProjetoSocialService")
class
ProjetoSocialServiceTest {

    @Mock
    private ProjetoSocialRepository repository;

    @InjectMocks
    private ProjetoSocialService service;

    private static ProjetoSocial projetoValido() {
        return ProjetoSocial.builder()
                .nome("Educação para Todos")
                .descricao("Projeto de reforço escolar para crianças da comunidade.")
                .organizacaoResponsavel("Instituto Esperança")
                .emailContato("contato@instituto.org")
                .dataInicio(LocalDate.of(2026, 9, 10))
                .dataFim(LocalDate.of(2027, 6, 30))
                .status(StatusProjeto.PLANEJADO)
                .build();
    }

    @Nested
    @DisplayName("criar")
    class Criar {

        @Test
        @DisplayName("deve salvar com timestamps quando período é válido")
        void deveSalvarComTimestampsQuandoPeriodoValido() {
            ProjetoSocial entrada = projetoValido();
            given(repository.save(any(ProjetoSocial.class)))
                    .willAnswer(inv -> inv.getArgument(0));

            ProjetoSocial resultado = service.criar(entrada);

            assertThat(resultado.getCriadoEm()).isNotNull();
            assertThat(resultado.getAtualizadoEm()).isNotNull();
            assertThat(resultado.getCriadoEm()).isEqualTo(resultado.getAtualizadoEm());

            ArgumentCaptor<ProjetoSocial> captor = ArgumentCaptor.forClass(ProjetoSocial.class);
            then(repository).should().save(captor.capture());
            assertThat(captor.getValue().getId()).isNull();
            assertThat(captor.getValue().getNome()).isEqualTo("Educação para Todos");
        }

        @Test
        @DisplayName("deve zerar id informado pelo cliente")
        void deveZerarIdQuandoIdInformado() {
            ProjetoSocial entrada = projetoValido();
            entrada.setId("id-forjado-pelo-cliente");
            given(repository.save(any(ProjetoSocial.class)))
                    .willAnswer(inv -> inv.getArgument(0));

            service.criar(entrada);

            ArgumentCaptor<ProjetoSocial> captor = ArgumentCaptor.forClass(ProjetoSocial.class);
            then(repository).should().save(captor.capture());
            assertThat(captor.getValue().getId()).isNull();
        }

        @Test
        @DisplayName("deve permitir dataFim nula (projeto em aberto)")
        void devePermitirDataFimNula() {
            ProjetoSocial entrada = projetoValido();
            entrada.setDataFim(null);
            given(repository.save(any(ProjetoSocial.class)))
                    .willAnswer(inv -> inv.getArgument(0));

            ProjetoSocial resultado = service.criar(entrada);

            assertThat(resultado.getDataFim()).isNull();
            then(repository).should().save(any(ProjetoSocial.class));
        }

        @Test
        @DisplayName("deve permitir dataFim igual à dataInicio (borda)")
        void devePermitirDataFimIgualDataInicio() {
            ProjetoSocial entrada = projetoValido();
            entrada.setDataFim(entrada.getDataInicio());
            given(repository.save(any(ProjetoSocial.class)))
                    .willAnswer(inv -> inv.getArgument(0));

            ProjetoSocial resultado = service.criar(entrada);

            assertThat(resultado.getDataFim()).isEqualTo(resultado.getDataInicio());
            then(repository).should().save(any(ProjetoSocial.class));
        }

        @Test
        @DisplayName("deve lançar 400 quando dataFim anterior à dataInicio")
        void deveLancarBadRequestQuandoDataFimAnterior() {
            ProjetoSocial entrada = projetoValido();
            entrada.setDataInicio(LocalDate.of(2026, 9, 10));
            entrada.setDataFim(LocalDate.of(2026, 9, 9));

            assertThatThrownBy(() -> service.criar(entrada))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException rse = (ResponseStatusException) ex;
                        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                        assertThat(rse.getReason())
                                .isEqualTo("A data final nao pode ser anterior a data de inicio");
                    });

            then(repository).should(never()).save(any());
        }
    }

    @Nested
    @DisplayName("buscarPorId")
    class BuscarPorId {

        @Test
        @DisplayName("deve retornar projeto quando existe")
        void deveRetornarQuandoExiste() {
            ProjetoSocial existente = projetoValido();
            existente.setId("abc123");
            given(repository.findById("abc123")).willReturn(Optional.of(existente));

            ProjetoSocial resultado = service.buscarPorId("abc123");

            assertThat(resultado.getId()).isEqualTo("abc123");
            assertThat(resultado.getNome()).isEqualTo("Educação para Todos");
        }

        @Test
        @DisplayName("deve lançar 404 quando não existe")
        void deveLancarNotFoundQuandoAusente() {
            given(repository.findById("inexistente")).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.buscarPorId("inexistente"))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException rse = (ResponseStatusException) ex;
                        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                        assertThat(rse.getReason()).isEqualTo("Projeto social nao encontrado");
                    });
        }
    }

    @Nested
    @DisplayName("atualizar")
    class Atualizar {

        @Test
        @DisplayName("deve atualizar campos editáveis preservando id e criadoEm")
        void deveAtualizarCamposPreservandoIdECriadoEm() {
            ProjetoSocial existente = projetoValido();
            existente.setId("id-1");
            existente.setCriadoEm(LocalDateTime.of(2026, 9, 1, 10, 0));
            existente.setAtualizadoEm(LocalDateTime.of(2026, 9, 1, 10, 0));

            ProjetoSocial novosDados = ProjetoSocial.builder()
                    .nome("Nome atualizado")
                    .descricao("Descricao atualizada com mais de dez caracteres.")
                    .organizacaoResponsavel("Nova ONG")
                    .emailContato("novo@ong.org")
                    .dataInicio(LocalDate.of(2026, 10, 1))
                    .dataFim(LocalDate.of(2027, 1, 31))
                    .status(StatusProjeto.EM_ANDAMENTO)
                    .build();

            given(repository.findById("id-1")).willReturn(Optional.of(existente));
            given(repository.save(any(ProjetoSocial.class)))
                    .willAnswer(inv -> inv.getArgument(0));

            ProjetoSocial resultado = service.atualizar("id-1", novosDados);

            assertThat(resultado.getId()).isEqualTo("id-1");
            assertThat(resultado.getCriadoEm())
                    .isEqualTo(LocalDateTime.of(2026, 9, 1, 10, 0));
            assertThat(resultado.getNome()).isEqualTo("Nome atualizado");
            assertThat(resultado.getDescricao())
                    .isEqualTo("Descricao atualizada com mais de dez caracteres.");
            assertThat(resultado.getOrganizacaoResponsavel()).isEqualTo("Nova ONG");
            assertThat(resultado.getEmailContato()).isEqualTo("novo@ong.org");
            assertThat(resultado.getStatus()).isEqualTo(StatusProjeto.EM_ANDAMENTO);
            assertThat(resultado.getAtualizadoEm()).isNotNull();
            assertThat(resultado.getAtualizadoEm())
                    .isAfterOrEqualTo(LocalDateTime.of(2026, 9, 1, 10, 0));

            then(repository).should().save(existente);
        }

        @Test
        @DisplayName("deve lançar 404 quando id não existe")
        void deveLancarNotFoundQuandoIdInexistente() {
            given(repository.findById("nao-existe")).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.atualizar("nao-existe", projetoValido()))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                            .isEqualTo(HttpStatus.NOT_FOUND));

            then(repository).should(never()).save(any());
        }

        @Test
        @DisplayName("deve lançar 400 quando novo período é inválido")
        void deveLancarBadRequestQuandoPeriodoInvalido() {
            ProjetoSocial existente = projetoValido();
            existente.setId("id-1");
            given(repository.findById("id-1")).willReturn(Optional.of(existente));

            ProjetoSocial novosDados = projetoValido();
            novosDados.setDataInicio(LocalDate.of(2026, 5, 1));
            novosDados.setDataFim(LocalDate.of(2026, 4, 30));

            assertThatThrownBy(() -> service.atualizar("id-1", novosDados))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                            .isEqualTo(HttpStatus.BAD_REQUEST));

            then(repository).should(never()).save(any());
        }
    }

}
