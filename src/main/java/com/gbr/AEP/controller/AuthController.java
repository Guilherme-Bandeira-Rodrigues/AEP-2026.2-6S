package com.gbr.AEP.controller;

import com.gbr.AEP.dto.CadastroRequest;
import com.gbr.AEP.dto.CsrfResponse;
import com.gbr.AEP.dto.LoginRequest;
import com.gbr.AEP.dto.UsuarioResponse;
import com.gbr.AEP.service.UsuarioService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticação", description = "Cadastro e autenticação por sessão com proteção CSRF")
public class AuthController {
    private final UsuarioService usuarios;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository contextos;
    private final SessionAuthenticationStrategy sessoes;

    @GetMapping("/csrf")
    public CsrfResponse csrf(CsrfToken token) {
        return new CsrfResponse(token.getHeaderName(), token.getToken());
    }

    @PostMapping("/cadastro")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse cadastrar(@Valid @RequestBody CadastroRequest dados) {
        return usuarios.cadastrar(dados);
    }

    @PostMapping("/login")
    public UsuarioResponse login(@Valid @RequestBody LoginRequest dados,
            HttpServletRequest request, HttpServletResponse response) {
        usuarios.validarTamanhoSenha(dados.senha());
        Authentication autenticacao = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(dados.email(), dados.senha()));
        sessoes.onAuthentication(autenticacao, request, response);
        SecurityContext contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(autenticacao);
        SecurityContextHolder.setContext(contexto);
        contextos.saveContext(contexto, request, response);
        return usuarios.atual();
    }

    @GetMapping("/me")
    public UsuarioResponse atual() {
        return usuarios.atual();
    }
}
