package com.gbr.AEP.service;

import com.gbr.AEP.dto.OrganizacaoRequest;
import com.gbr.AEP.dto.OrganizacaoResponse;
import com.gbr.AEP.entity.Organizacao;
import com.gbr.AEP.repository.OrganizacaoRepository;
import com.gbr.AEP.repository.ProjetoSocialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrganizacaoService {

    private final OrganizacaoRepository repository;
    private final ProjetoSocialRepository projetos;

    public OrganizacaoResponse criar(OrganizacaoRequest dados) {
        return salvar(null, dados);
    }

    public List<OrganizacaoResponse> listarTodos() {
        return repository.findAll().stream().map(OrganizacaoResponse::de).toList();
    }

    public OrganizacaoResponse buscarPorId(String id) {
        return OrganizacaoResponse.de(obterOrganizacao(id));
    }

    Map<String, OrganizacaoResponse> buscarPorIds(Collection<String> ids) {
        return repository.findAllById(ids).stream().map(OrganizacaoResponse::de)
                .collect(Collectors.toMap(OrganizacaoResponse::id, Function.identity()));
    }

    public synchronized OrganizacaoResponse atualizar(String id, OrganizacaoRequest dados) {
        return salvar(obterOrganizacao(id).getId(), dados);
    }

    public synchronized void excluir(String id) {
        Organizacao organizacao = obterOrganizacao(id);
        if (projetos.existsByOrganizacaoId(organizacao.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A organização possui projetos vinculados");
        }
        repository.delete(organizacao);
    }

    // Mantém a validação e a gravação do vínculo na mesma seção crítica da exclusão.
    synchronized <T> T comOrganizacao(String id, Function<OrganizacaoResponse, T> operacao) {
        return operacao.apply(buscarPorId(id));
    }

    private Organizacao obterOrganizacao(String id) {
        return repository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Organização não encontrada"));
    }

    private OrganizacaoResponse salvar(String id, OrganizacaoRequest dados) {
        return OrganizacaoResponse.de(repository.save(
                new Organizacao(id, dados.nome(), dados.descricao(), dados.emailContato())));
    }
}
