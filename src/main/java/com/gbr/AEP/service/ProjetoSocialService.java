package com.gbr.AEP.service;

import com.gbr.AEP.entity.ProjetoSocial;
import com.gbr.AEP.repository.ProjetoSocialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjetoSocialService {

    private final ProjetoSocialRepository repository;

    public ProjetoSocial criar(ProjetoSocial projeto) {
        validarPeriodo(projeto);

        LocalDateTime agora = LocalDateTime.now();
        projeto.setId(null);
        projeto.setCriadoEm(agora);
        projeto.setAtualizadoEm(agora);
        return repository.save(projeto);
    }

    public List<ProjetoSocial> listarTodos() {
        return repository.findAll();
    }

    public ProjetoSocial buscarPorId(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Projeto social nao encontrado"));
    }

    public ProjetoSocial atualizar(String id, ProjetoSocial novosDados) {
        ProjetoSocial existente = buscarPorId(id);
        validarPeriodo(novosDados);

        existente.setNome(novosDados.getNome());
        existente.setDescricao(novosDados.getDescricao());
        existente.setOrganizacaoResponsavel(novosDados.getOrganizacaoResponsavel());
        existente.setEmailContato(novosDados.getEmailContato());
        existente.setDataInicio(novosDados.getDataInicio());
        existente.setDataFim(novosDados.getDataFim());
        existente.setStatus(novosDados.getStatus());
        existente.setAtualizadoEm(LocalDateTime.now());

        return repository.save(existente);
    }

    public void excluir(String id) {
        ProjetoSocial projeto = buscarPorId(id);
        repository.delete(projeto);
    }

    private void validarPeriodo(ProjetoSocial projeto) {
        if (projeto.getDataInicio() != null
                && projeto.getDataFim() != null
                && projeto.getDataFim().isBefore(projeto.getDataInicio())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "A data final nao pode ser anterior a data de inicio");
        }
    }
}
