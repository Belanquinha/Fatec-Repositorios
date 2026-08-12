package com.fatecrepository.mapper;

import com.fatecrepository.dto.response.AlunoResponse;
import com.fatecrepository.dto.response.AuthResponse;
import com.fatecrepository.dto.response.InstituicaoResponse;
import com.fatecrepository.dto.response.ProfessorResponse;
import com.fatecrepository.dto.response.UsuarioResponse;
import com.fatecrepository.model.Aluno;
import com.fatecrepository.model.Instituicao;
import com.fatecrepository.model.Professor;
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

    public AlunoResponse toAlunoResponse(Aluno aluno) {
        return new AlunoResponse(
            aluno.getId(),
            aluno.getMatricula(),
            aluno.getTelefone(),
            toUsuarioResponse(aluno.getUsuario()),
            aluno.getCriadoEm(),
            aluno.getAtualizadoEm()
        );
    }

    public ProfessorResponse toProfessorResponse(Professor professor) {
        return new ProfessorResponse(
            professor.getId(),
            professor.getTelefone(),
            professor.getAreaEnsino(),
            toUsuarioResponse(professor.getUsuario()),
            professor.getCriadoEm(),
            professor.getAtualizadoEm()
        );
    }

    public InstituicaoResponse toInstituicaoResponse(Instituicao instituicao) {
        return new InstituicaoResponse(
            instituicao.getId(),
            instituicao.getNome(),
            instituicao.getCnpj(),
            instituicao.getEmail(),
            instituicao.getTelefone(),
            instituicao.getEndereco(),
            instituicao.getCidade(),
            instituicao.getEstado(),
            instituicao.getCriadoEm(),
            instituicao.getAtualizadoEm()
        );
    }

    public AuthResponse toAuthResponse(String token, long expiresInSeconds) {
        return new AuthResponse(token, "Bearer", expiresInSeconds);
    }
}
