package com.gbr.AEP.entity;

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

@DisplayName("ProjetoSocial (validação)")
class ProjetoSocialValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setupValidator() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    private static ProjetoSocial valido() {
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

    @Test
    @DisplayName("deve passar sem violações quando projeto é válido")
    void devePassarQuandoValido() {
        Set<ConstraintViolation<ProjetoSocial>> violations = validator.validate(valido());

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("deve permitir dataFim nula (projeto sem fim definido)")
    void devePermitirDataFimNula() {
        ProjetoSocial projeto = valido();
        projeto.setDataFim(null);

        assertThat(validator.validate(projeto)).isEmpty();
    }

    @ParameterizedTest(name = "nome inválido: ''{0}''")
    @NullSource
    @ValueSource(strings = {"", "  ", "AB"})
    @DisplayName("deve rejeitar nome blank ou fora do tamanho 3-120")
    void deveRejeitarNomeInvalido(String nome) {
        ProjetoSocial projeto = valido();
        projeto.setNome(nome);

        Set<ConstraintViolation<ProjetoSocial>> violations = validator.validate(projeto);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("nome"));
    }

    @Test
    @DisplayName("deve rejeitar nome com mais de 120 caracteres")
    void deveRejeitarNomeLongo() {
        ProjetoSocial projeto = valido();
        projeto.setNome("A".repeat(121));

        assertThat(validator.validate(projeto))
                .anyMatch(v -> v.getPropertyPath().toString().equals("nome"));
    }

    @ParameterizedTest(name = "descricao inválida: ''{0}''")
    @NullSource
    @ValueSource(strings = {"", "  ", "curta"})
    @DisplayName("deve rejeitar descricao blank ou fora do tamanho 10-1000")
    void deveRejeitarDescricaoInvalida(String descricao) {
        ProjetoSocial projeto = valido();
        projeto.setDescricao(descricao);

        Set<ConstraintViolation<ProjetoSocial>> violations = validator.validate(projeto);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("descricao"));
    }

    @Test
    @DisplayName("deve rejeitar descricao com mais de 1000 caracteres")
    void deveRejeitarDescricaoLonga() {
        ProjetoSocial projeto = valido();
        projeto.setDescricao("A".repeat(1001));

        assertThat(validator.validate(projeto))
                .anyMatch(v -> v.getPropertyPath().toString().equals("descricao"));
    }

    @Test
    @DisplayName("deve rejeitar organizacao blank")
    void deveRejeitarOrganizacaoBlank() {
        ProjetoSocial projeto = valido();
        projeto.setOrganizacaoResponsavel("  ");

        assertThat(validator.validate(projeto))
                .anyMatch(v -> v.getPropertyPath().toString().equals("organizacaoResponsavel"));
    }

    @ParameterizedTest(name = "email inválido: ''{0}''")
    @NullSource
    @ValueSource(strings = {"", "  ", "sem-arroba", "email-invalido", "a@", "@b.com"})
    @DisplayName("deve rejeitar e-mail blank ou malformado")
    void deveRejeitarEmailInvalido(String email) {
        ProjetoSocial projeto = valido();
        projeto.setEmailContato(email);

        assertThat(validator.validate(projeto))
                .anyMatch(v -> v.getPropertyPath().toString().equals("emailContato"));
    }

    @Test
    @DisplayName("deve rejeitar dataInicio nula")
    void deveRejeitarDataInicioNula() {
        ProjetoSocial projeto = valido();
        projeto.setDataInicio(null);

        assertThat(validator.validate(projeto))
                .anyMatch(v -> v.getPropertyPath().toString().equals("dataInicio"));
    }

    @Test
    @DisplayName("deve rejeitar status nulo")
    void deveRejeitarStatusNulo() {
        ProjetoSocial projeto = valido();
        projeto.setStatus(null);

        assertThat(validator.validate(projeto))
                .anyMatch(v -> v.getPropertyPath().toString().equals("status"));
    }

    @Test
    @DisplayName("deve respeitar equals/hashCode/toString do Lombok")
    void deveRespeitarEqualsHashCodeToString() {
        ProjetoSocial a = valido();
        ProjetoSocial b = valido();

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
        assertThat(a.toString()).contains("Educação para Todos");

        b.setNome("Outro nome");
        assertThat(a).isNotEqualTo(b);
    }
}
