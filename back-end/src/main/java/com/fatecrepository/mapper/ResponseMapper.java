package com.fatecrepository.mapper;

import com.fatecrepository.dto.response.AuthResponse;
import com.fatecrepository.dto.response.InstituicaoResponse;
import com.fatecrepository.dto.response.UsuarioResponse;
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
            user.getFotoUrl(),
            user.getCriadoEm(),
            user.getAtualizadoEm()
        );
    }

    public InstituicaoResponse toInstituicaoResponse(Instituicao instituicao) {
        return new InstituicaoResponse(
            instituicao.getId(),
            instituicao.getCodigoUnidade(),
            instituicao.getNome(),
            instituicao.getEndereco(),
            instituicao.getCidade(),
            instituicao.getEstado(),
            instituicao.getRegiaoAdministrativa(),
            instituicao.getCnpj(),
            instituicao.getTelefone(),
            instituicao.getSite(),
            instituicao.getLinkLogo(),
            instituicao.isAtivo(),
            instituicao.getCriadoEm(),
            instituicao.getAtualizadoEm()
        );
    }

    public AuthResponse toAuthResponse(String token, long expiresInSeconds, String role) {
        return new AuthResponse(token, "Bearer", expiresInSeconds, null, null, null, role);
    }

    public AuthResponse toAuthResponse(String token, long expiresInSeconds, String nome, String email, String fotoUrl, String role) {
        return new AuthResponse(token, "Bearer", expiresInSeconds, nome, email, fotoUrl, role);
    }
}