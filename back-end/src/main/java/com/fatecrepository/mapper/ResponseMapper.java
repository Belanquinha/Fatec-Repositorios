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

    public com.fatecrepository.dto.response.IntegranteResponse toIntegranteResponse(com.fatecrepository.model.Integrante integrante) {
        return new com.fatecrepository.dto.response.IntegranteResponse(
            integrante.getId(),
            integrante.getNome(),
            integrante.getLinkLinkedin()
        );
    }

    public com.fatecrepository.dto.response.ProjetoResponse toProjetoResponse(com.fatecrepository.model.Projeto projeto) {
        java.util.List<com.fatecrepository.dto.response.IntegranteResponse> integrantes = projeto.getIntegrantes() != null
            ? projeto.getIntegrantes().stream().map(this::toIntegranteResponse).toList()
            : java.util.Collections.emptyList();

        return new com.fatecrepository.dto.response.ProjetoResponse(
            projeto.getId(),
            projeto.getTitulo(),
            projeto.getDescricaoCurta(),
            projeto.getConteudoEditorJs(),
            projeto.getLinkRepositorio(),
            projeto.getImagemCapaUrl(),
            projeto.getPalavrasChave(),
            projeto.getAnoPublicado(),
            projeto.getEstado().name(),
            projeto.getMotivoRejeicao(),
            projeto.getEmailProfessorResponsavel(),
            projeto.getInstituicao() != null ? projeto.getInstituicao().getId() : null,
            projeto.getInstituicao() != null ? projeto.getInstituicao().getNome() : null,
            projeto.getAutor() != null ? projeto.getAutor().getId() : null,
            projeto.getAutor() != null ? projeto.getAutor().getNome() : null,
            integrantes,
            projeto.getCriadoEm(),
            projeto.getAtualizadoEm()
        );
    }
}