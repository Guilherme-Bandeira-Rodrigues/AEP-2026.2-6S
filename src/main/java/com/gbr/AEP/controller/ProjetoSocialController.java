package com.gbr.AEP.controller;

import com.gbr.AEP.entity.ProjetoSocial;
import com.gbr.AEP.service.ProjetoSocialService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/projetos")
@RequiredArgsConstructor
@Tag(name = "Projetos sociais", description = "Cadastro e gerenciamento de projetos sociais")
public class ProjetoSocialController {

    private final ProjetoSocialService service;

    @PostMapping
    @Operation(summary = "Cadastrar projeto", description = "Cadastra um novo projeto social na plataforma.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Projeto cadastrado com sucesso", content = @Content(schema = @Schema(implementation = ProjetoSocial.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou período inconsistente", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<ProjetoSocial> criar(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Dados do projeto social", required = true,
                    content = @Content(schema = @Schema(implementation = ProjetoSocial.class)))
            @Valid @RequestBody ProjetoSocial projeto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(projeto));
    }

    @GetMapping
    @Operation(summary = "Listar projetos", description = "Retorna todos os projetos sociais cadastrados.")
    @ApiResponse(responseCode = "200", description = "Projetos retornados com sucesso")
    public ResponseEntity<List<ProjetoSocial>> listarTodos() {
        return ResponseEntity.ok(service.listarTodos());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar projeto por ID", description = "Retorna um projeto social pelo identificador do MongoDB.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Projeto encontrado", content = @Content(schema = @Schema(implementation = ProjetoSocial.class))),
            @ApiResponse(responseCode = "404", description = "Projeto não encontrado", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<ProjetoSocial> buscarPorId(
            @Parameter(description = "Identificador do projeto", example = "66db67818e72c12f8a3c9021", required = true)
            @PathVariable String id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar projeto", description = "Substitui os dados editáveis de um projeto social existente.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Projeto atualizado com sucesso", content = @Content(schema = @Schema(implementation = ProjetoSocial.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou período inconsistente", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Projeto não encontrado", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<ProjetoSocial> atualizar(
            @Parameter(description = "Identificador do projeto", example = "66db67818e72c12f8a3c9021", required = true)
            @PathVariable String id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Novos dados do projeto social", required = true,
                    content = @Content(schema = @Schema(implementation = ProjetoSocial.class)))
            @Valid @RequestBody ProjetoSocial projeto) {
        return ResponseEntity.ok(service.atualizar(id, projeto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir projeto", description = "Remove definitivamente um projeto social.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Projeto excluído com sucesso"),
            @ApiResponse(responseCode = "404", description = "Projeto não encontrado", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<Void> excluir(
            @Parameter(description = "Identificador do projeto", example = "66db67818e72c12f8a3c9021", required = true)
            @PathVariable String id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
