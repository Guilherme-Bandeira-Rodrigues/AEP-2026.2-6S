package com.gbr.AEP.dto;

import com.gbr.AEP.entity.StatusProjeto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ProjetoSocialRequest (validação)")
class ProjetoSocialRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setupValidator() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    private static ProjetoSocialRequest valido() {
        return ProjetoSocialRequest.builder()
                .nome("Educação para Todos")
                .descricao("Projeto de reforço escolar para crianças da comunidade.")
                .organizacaoId("66db67818e72c12f8a3c9021")
                .emailContato("contato@instituto.org")
                .dataInicio(LocalDate.of(2026, 9, 10))
                .dataFim(LocalDate.of(2027, 6, 30))
                .status(StatusProjeto.PLANEJADO)
                .build();
    }

    @Test
    @DisplayName("deve passar sem violações quando projeto é válido")
    void devePassarQuandoValido() {
        Set<ConstraintViolation<ProjetoSocialRequest>> violations = validator.validate(valido());

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("deve permitir dataFim nula (projeto sem fim definido)")
    void devePermitirDataFimNula() {
        ProjetoSocialRequest projeto = valido();
        projeto = projeto.toBuilder().dataFim(null).build();

        assertThat(validator.validate(projeto)).isEmpty();
    }

    @ParameterizedTest(name = "nome inválido: ''{0}''")
    @NullSource
    @ValueSource(strings = {"", "  ", "AB"})
    @DisplayName("deve rejeitar nome blank ou fora do tamanho 3-120")
    void deveRejeitarNomeInvalido(String nome) {
        ProjetoSocialRequest projeto = valido();
        projeto = projeto.toBuilder().nome(nome).build();

        Set<ConstraintViolation<ProjetoSocialRequest>> violations = validator.validate(projeto);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("nome"));
    }

    @Test
    @DisplayName("deve rejeitar nome com mais de 120 caracteres")
    void deveRejeitarNomeLongo() {
        ProjetoSocialRequest projeto = valido();
        projeto = projeto.toBuilder().nome("A".repeat(121)).build();

        assertThat(validator.validate(projeto))
                .anyMatch(v -> v.getPropertyPath().toString().equals("nome"));
    }

    @ParameterizedTest(name = "descricao inválida: ''{0}''")
    @NullSource
    @ValueSource(strings = {"", "  ", "curta"})
    @DisplayName("deve rejeitar descricao blank ou fora do tamanho 10-1000")
    void deveRejeitarDescricaoInvalida(String descricao) {
        ProjetoSocialRequest projeto = valido();
        projeto = projeto.toBuilder().descricao(descricao).build();

        Set<ConstraintViolation<ProjetoSocialRequest>> violations = validator.validate(projeto);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("descricao"));
    }

    @Test
    @DisplayName("deve rejeitar descricao com mais de 1000 caracteres")
    void deveRejeitarDescricaoLonga() {
        ProjetoSocialRequest projeto = valido();
        projeto = projeto.toBuilder().descricao("A".repeat(1001)).build();

        assertThat(validator.validate(projeto))
                .anyMatch(v -> v.getPropertyPath().toString().equals("descricao"));
    }

    @Test
    @DisplayName("deve rejeitar organizacao blank")
    void deveRejeitarOrganizacaoBlank() {
        ProjetoSocialRequest projeto = valido();
        projeto = projeto.toBuilder().organizacaoId("  ").build();

        assertThat(validator.validate(projeto))
                .anyMatch(v -> v.getPropertyPath().toString().equals("organizacaoId"));
    }

    @ParameterizedTest(name = "email inválido: ''{0}''")
    @NullSource
    @ValueSource(strings = {"", "  ", "sem-arroba", "email-invalido", "a@", "@b.com"})
    @DisplayName("deve rejeitar e-mail blank ou malformado")
    void deveRejeitarEmailInvalido(String email) {
        ProjetoSocialRequest projeto = valido();
        projeto = projeto.toBuilder().emailContato(email).build();

        assertThat(validator.validate(projeto))
                .anyMatch(v -> v.getPropertyPath().toString().equals("emailContato"));
    }

    @Test
    @DisplayName("deve rejeitar dataInicio nula")
    void deveRejeitarDataInicioNula() {
        ProjetoSocialRequest projeto = valido();
        projeto = projeto.toBuilder().dataInicio(null).build();

        assertThat(validator.validate(projeto))
                .anyMatch(v -> v.getPropertyPath().toString().equals("dataInicio"));
    }

    @Test
    @DisplayName("deve rejeitar status nulo")
    void deveRejeitarStatusNulo() {
        ProjetoSocialRequest projeto = valido();
        projeto = projeto.toBuilder().status(null).build();

        assertThat(validator.validate(projeto))
                .anyMatch(v -> v.getPropertyPath().toString().equals("status"));
    }

}
