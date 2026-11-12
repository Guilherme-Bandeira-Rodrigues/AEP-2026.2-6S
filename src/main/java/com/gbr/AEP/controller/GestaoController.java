package com.gbr.AEP.controller;

import com.gbr.AEP.dto.OrganizacaoResponse;
import com.gbr.AEP.dto.ProjetoSocialResponse;
import com.gbr.AEP.service.OrganizacaoService;
import com.gbr.AEP.service.ProjetoSocialService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
@Tag(name = "Minha gestão", description = "Registros pertencentes à conta autenticada")
public class GestaoController {
    private final OrganizacaoService organizacoes;
    private final ProjetoSocialService projetos;

    @GetMapping("/organizacoes")
    public List<OrganizacaoResponse> organizacoes() {
        return organizacoes.listarMinhas();
    }

    @GetMapping("/projetos")
    public List<ProjetoSocialResponse> projetos() {
        return projetos.listarMeus();
    }
}
