package com.gbr.AEP.controller;

import com.gbr.AEP.entity.ProjetoSocial;
import com.gbr.AEP.service.ProjetoSocialService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
public class ProjetoSocialController {

    private final ProjetoSocialService service;

    @PostMapping
    public ResponseEntity<ProjetoSocial> criar(@Valid @RequestBody ProjetoSocial projeto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(projeto));
    }

    @GetMapping
    public ResponseEntity<List<ProjetoSocial>> listarTodos() {
        return ResponseEntity.ok(service.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjetoSocial> buscarPorId(@PathVariable String id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProjetoSocial> atualizar(
            @PathVariable String id,
            @Valid @RequestBody ProjetoSocial projeto) {
        return ResponseEntity.ok(service.atualizar(id, projeto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable String id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
