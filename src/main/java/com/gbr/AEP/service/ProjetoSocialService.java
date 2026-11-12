package com.gbr.AEP.service;

import com.gbr.AEP.dto.AtualizacaoProjetoRequest;
import com.gbr.AEP.dto.OrganizacaoResponse;
import com.gbr.AEP.dto.ProjetoSocialRequest;
import com.gbr.AEP.dto.ProjetoSocialResponse;
import com.gbr.AEP.entity.AtualizacaoProjeto;
import com.gbr.AEP.entity.ProjetoSocial;
import com.gbr.AEP.entity.StatusProjeto;
import com.gbr.AEP.repository.ProjetoSocialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ProjetoSocialService {

    private final ProjetoSocialRepository repository;
    private final OrganizacaoService organizacoes;
    private final MongoTemplate mongoTemplate;

    public ProjetoSocialResponse criar(ProjetoSocialRequest dados) {
        validarPeriodo(dados);
        return organizacoes.comOrganizacao(dados.organizacaoId(), organizacao -> {
            LocalDateTime agora = LocalDateTime.now();
            ProjetoSocial projeto = ProjetoSocial.builder()
                    .nome(dados.nome())
                    .descricao(dados.descricao())
                    .organizacaoId(organizacao.id())
                    .emailContato(dados.emailContato())
                    .dataInicio(dados.dataInicio())
                    .dataFim(dados.dataFim())
                    .status(dados.status())
                    .criadoEm(agora)
                    .atualizadoEm(agora)
                    .build();
            return ProjetoSocialResponse.de(repository.save(projeto), organizacao);
        });
    }

    public List<ProjetoSocialResponse> listarTodos(String nome, StatusProjeto status) {
        Query consulta = new Query();
        if (StringUtils.hasText(nome)) {
            consulta.addCriteria(Criteria.where("nome").regex(Pattern.quote(nome.strip()), "i"));
        }
        if (status != null) {
            consulta.addCriteria(Criteria.where("status").is(status));
        }
        List<ProjetoSocial> projetos = mongoTemplate.find(consulta, ProjetoSocial.class);
        List<String> ids = projetos.stream().map(ProjetoSocial::getOrganizacaoId)
                .filter(Objects::nonNull).distinct().toList();
        Map<String, OrganizacaoResponse> vinculadas = organizacoes.buscarPorIds(ids);
        return projetos.stream().map(projeto -> ProjetoSocialResponse.de(projeto,
                projeto.getOrganizacaoId() == null ? null : vinculadas.get(projeto.getOrganizacaoId()))).toList();
    }

    public ProjetoSocialResponse buscarPorId(String id) {
        return comOrganizacao(obterProjeto(id));
    }

    public ProjetoSocialResponse atualizar(String id, ProjetoSocialRequest dados) {
        if (!repository.existsById(id)) {
            throw projetoNaoEncontrado();
        }
        validarPeriodo(dados);
        return organizacoes.comOrganizacao(dados.organizacaoId(), organizacao -> {
            Update alteracoes = new Update()
                    .set("nome", dados.nome())
                    .set("descricao", dados.descricao())
                    .set("organizacaoId", organizacao.id())
                    .set("emailContato", dados.emailContato())
                    .set("dataInicio", dados.dataInicio())
                    .set("dataFim", dados.dataFim())
                    .set("status", dados.status())
                    .max("atualizadoEm", LocalDateTime.now())
                    .unset("organizacaoResponsavel");
            return ProjetoSocialResponse.de(modificar(id, alteracoes), organizacao);
        });
    }

    public ProjetoSocialResponse publicarAtualizacao(String id, AtualizacaoProjetoRequest dados) {
        Instant agora = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        AtualizacaoProjeto atualizacao = new AtualizacaoProjeto(agora, dados.descricao().strip());
        Update alteracoes = new Update()
                .push("atualizacoes", atualizacao)
                .max("atualizadoEm", LocalDateTime.ofInstant(agora, ZoneId.systemDefault()));
        return comOrganizacao(modificar(id, alteracoes));
    }

    public void excluir(String id) {
        ProjetoSocial removido = mongoTemplate.findAndRemove(porId(id), ProjetoSocial.class);
        if (removido == null) {
            throw projetoNaoEncontrado();
        }
    }

    private ProjetoSocial modificar(String id, Update alteracoes) {
        ProjetoSocial alterado = mongoTemplate.findAndModify(porId(id), alteracoes,
                FindAndModifyOptions.options().returnNew(true), ProjetoSocial.class);
        if (alterado == null) {
            throw projetoNaoEncontrado();
        }
        return alterado;
    }

    private ProjetoSocial obterProjeto(String id) {
        return repository.findById(id).orElseThrow(this::projetoNaoEncontrado);
    }

    private Query porId(String id) {
        return Query.query(Criteria.where("id").is(id));
    }

    private ResponseStatusException projetoNaoEncontrado() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Projeto social nao encontrado");
    }

    private ProjetoSocialResponse comOrganizacao(ProjetoSocial projeto) {
        String id = projeto.getOrganizacaoId();
        OrganizacaoResponse organizacao = id == null ? null : organizacoes.buscarPorIds(List.of(id)).get(id);
        return ProjetoSocialResponse.de(projeto, organizacao);
    }

    private void validarPeriodo(ProjetoSocialRequest dados) {
        if (dados.dataInicio() != null && dados.dataFim() != null && dados.dataFim().isBefore(dados.dataInicio())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "A data final nao pode ser anterior a data de inicio");
        }
    }
}
