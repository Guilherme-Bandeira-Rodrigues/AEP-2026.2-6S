package com.gbr.AEP.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class UsuarioAtualTest {
    private final UsuarioAtual usuarioAtual = new UsuarioAtual();

    @AfterEach
    void limpar() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void exigeAutenticacaoValida() {
        assertThatExceptionOfType(AuthenticationCredentialsNotFoundException.class).isThrownBy(usuarioAtual::id);
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.unauthenticated("id", "senha"));
        assertThatExceptionOfType(AuthenticationCredentialsNotFoundException.class).isThrownBy(usuarioAtual::id);
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "chave", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
        assertThatExceptionOfType(AuthenticationCredentialsNotFoundException.class).isThrownBy(usuarioAtual::id);
    }

    @Test
    void utilizaIdentificadorDoPrincipalAutenticado() {
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                "usuario-1", null, AuthorityUtils.createAuthorityList("ROLE_RESPONSAVEL")));
        assertThat(usuarioAtual.id()).isEqualTo("usuario-1");
    }
}
