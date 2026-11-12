package com.gbr.AEP.controller;

import com.gbr.AEP.dto.OrganizacaoRequest;
import com.gbr.AEP.dto.OrganizacaoResponse;
import com.gbr.AEP.service.OrganizacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/organizacoes")
@RequiredArgsConstructor
@Tag(name = "Organizações", description = "Cadastro e gerenciamento das organizações responsáveis pelos projetos")
public class OrganizacaoController {

    private final OrganizacaoService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastrar organização")
    @ApiResponse(responseCode = "201", description = "Organização cadastrada")
    @ApiResponse(responseCode = "400", description = "Dados inválidos")
    public OrganizacaoResponse criar(@Valid @RequestBody OrganizacaoRequest organizacao) {
        return service.criar(organizacao);
    }

    @GetMapping
    @Operation(summary = "Listar organizações")
    public List<OrganizacaoResponse> listarTodos() {
        return service.listarTodos();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar organização por ID")
    @ApiResponse(responseCode = "200", description = "Organização encontrada")
    @ApiResponse(responseCode = "404", description = "Organização não encontrada")
    public OrganizacaoResponse buscarPorId(@PathVariable String id) {
        return service.buscarPorId(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar organização")
    @ApiResponse(responseCode = "200", description = "Organização atualizada")
    @ApiResponse(responseCode = "400", description = "Dados inválidos")
    @ApiResponse(responseCode = "404", description = "Organização não encontrada")
    public OrganizacaoResponse atualizar(@PathVariable String id, @Valid @RequestBody OrganizacaoRequest organizacao) {
        return service.atualizar(id, organizacao);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Excluir organização")
    @ApiResponse(responseCode = "204", description = "Organização excluída")
    @ApiResponse(responseCode = "404", description = "Organização não encontrada")
    @ApiResponse(responseCode = "409", description = "Organização possui projetos vinculados")
    public void excluir(@PathVariable String id) {
        service.excluir(id);
    }
}
