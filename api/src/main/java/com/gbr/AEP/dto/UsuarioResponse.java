package com.gbr.AEP.dto;

import com.gbr.AEP.entity.Usuario;

public record UsuarioResponse(String id, String nome, String email) {
    public static UsuarioResponse de(Usuario usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getNome(), usuario.getEmail());
    }
}
