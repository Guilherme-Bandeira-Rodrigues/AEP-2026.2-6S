package com.gbr.AEP.controller;

import com.gbr.AEP.dto.AtualizacaoProjetoRequest;
import com.gbr.AEP.dto.ProjetoSocialRequest;
import com.gbr.AEP.dto.ProjetoSocialResponse;
import com.gbr.AEP.entity.StatusProjeto;
import com.gbr.AEP.service.ProjetoSocialService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/projetos")
@RequiredArgsConstructor
@Tag(name = "Projetos sociais", description = "Cadastro e gerenciamento de projetos sociais")
public class ProjetoSocialController {

    private final ProjetoSocialService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastrar projeto")
    @ApiResponse(responseCode = "201", description = "Projeto cadastrado")
    @ApiResponse(responseCode = "400", description = "Dados inválidos ou período inconsistente")
    @ApiResponse(responseCode = "404", description = "Organização não encontrada")
    public ProjetoSocialResponse criar(@Valid @RequestBody ProjetoSocialRequest projeto) {
        return service.criar(projeto);
    }

    @GetMapping
    @Operation(summary = "Listar projetos", description = "Permite combinar a pesquisa por nome com o filtro por status.")
    @ApiResponse(responseCode = "200", description = "Projetos retornados")
    @ApiResponse(responseCode = "400", description = "Status inválido")
    public List<ProjetoSocialResponse> listarTodos(
            @Parameter(description = "Trecho literal do nome, sem distinção entre maiúsculas e minúsculas")
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) StatusProjeto status) {
        return service.listarTodos(nome, status);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar projeto por ID")
    @ApiResponse(responseCode = "200", description = "Projeto encontrado")
    @ApiResponse(responseCode = "404", description = "Projeto não encontrado")
    public ProjetoSocialResponse buscarPorId(@PathVariable String id) {
        return service.buscarPorId(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar os dados editáveis do projeto")
    @ApiResponse(responseCode = "200", description = "Projeto atualizado")
    @ApiResponse(responseCode = "400", description = "Dados inválidos ou período inconsistente")
    @ApiResponse(responseCode = "404", description = "Projeto ou organização não encontrado")
    public ProjetoSocialResponse atualizar(@PathVariable String id, @Valid @RequestBody ProjetoSocialRequest projeto) {
        return service.atualizar(id, projeto);
    }

    @PostMapping("/{id}/atualizacoes")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Publicar atualização", description = "Acrescenta uma entrada ao histórico com a data gerada pelo servidor.")
    @ApiResponse(responseCode = "201", description = "Atualização publicada. Retorna o projeto com o histórico completo.")
    @ApiResponse(responseCode = "400", description = "Descrição inválida")
    @ApiResponse(responseCode = "404", description = "Projeto não encontrado")
    public ProjetoSocialResponse publicarAtualizacao(
            @PathVariable String id, @Valid @RequestBody AtualizacaoProjetoRequest atualizacao) {
        return service.publicarAtualizacao(id, atualizacao);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Excluir projeto")
    @ApiResponse(responseCode = "204", description = "Projeto excluído")
    @ApiResponse(responseCode = "404", description = "Projeto não encontrado")
    public void excluir(@PathVariable String id) {
        service.excluir(id);
    }
}
