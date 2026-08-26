package com.fatecrepository.mapper;

import com.fatecrepository.dto.response.AuthResponse;
import com.fatecrepository.dto.response.GestorResponse;
import com.fatecrepository.dto.response.InstituicaoResponse;
import com.fatecrepository.dto.response.UsuarioResponse;
import com.fatecrepository.model.Gestor;
import com.fatecrepository.model.Instituicao;
import com.fatecrepository.model.User;
import org.springframework.stereotype.Component;

@Component
public class ResponseMapper {

    public UsuarioResponse toUsuarioResponse(User user) {
        return new UsuarioResponse(
            user.getId(),
            user.getNome(),
            user.getEmail(),
            user.getRole().getValue(),
            user.getCriadoEm(),
            user.getAtualizadoEm()
        );
    }

    public GestorResponse toGestorResponse(Gestor gestor) {
        if (gestor == null) {
            return null;
        }

        return new GestorResponse(
            gestor.getId(),
            gestor.getNome(),
            gestor.getEmail(),
            gestor.getTelefone(),
            gestor.getCriadoEm(),
            gestor.getAtualizadoEm()
        );
    }

    public InstituicaoResponse toInstituicaoResponse(Instituicao instituicao) {
        return new InstituicaoResponse(
            instituicao.getId(),
            instituicao.getNome(),
            instituicao.getCnpj(),
            instituicao.getEndereco(),
            instituicao.getCidade(),
            instituicao.getEstado(),
            toGestorResponse(instituicao.getGestor()),
            instituicao.getCriadoEm(),
            instituicao.getAtualizadoEm()
        );
    }

    public AuthResponse toAuthResponse(String token, long expiresInSeconds) {
        return new AuthResponse(token, "Bearer", expiresInSeconds);
    }
}
