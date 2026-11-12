package com.gbr.AEP.service;

import com.gbr.AEP.dto.CadastroRequest;
import com.gbr.AEP.dto.UsuarioResponse;
import com.gbr.AEP.entity.Usuario;
import com.gbr.AEP.repository.UsuarioRepository;
import com.gbr.AEP.security.UsuarioAtual;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class UsuarioService implements UserDetailsService {
    private final UsuarioRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final MongoTemplate mongoTemplate;
    private final UsuarioAtual usuarioAtual;

    public UsuarioResponse cadastrar(CadastroRequest dados) {
        validarTamanhoSenha(dados.senha());
        // O índice único protege também cadastros simultâneos, antes de qualquer inserção.
        mongoTemplate.indexOps(Usuario.class).ensureIndex(new Index().on("email", Sort.Direction.ASC)
                .unique().named("usuario_email_unico"));
        Usuario usuario = new Usuario(null, dados.nome(), dados.email(), passwordEncoder.encode(dados.senha()));
        try {
            return UsuarioResponse.de(repository.insert(usuario));
        } catch (DuplicateKeyException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe uma conta com este e-mail");
        }
    }

    public UsuarioResponse atual() {
        return UsuarioResponse.de(repository.findById(usuarioAtual.id()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sessão inválida. Entre novamente")));
    }

    @Override
    public UserDetails loadUserByUsername(String email) {
        Usuario usuario = repository.findByEmail(email.strip().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new UsernameNotFoundException("Credenciais inválidas"));
        return User.withUsername(usuario.getId()).password(usuario.getSenhaHash()).roles("RESPONSAVEL").build();
    }

    public void validarTamanhoSenha(String senha) {
        if (senha.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A senha deve ter no máximo 72 bytes em UTF-8");
        }
    }
}
