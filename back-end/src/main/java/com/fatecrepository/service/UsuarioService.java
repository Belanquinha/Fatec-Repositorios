package com.fatecrepository.service;

import com.fatecrepository.dto.response.InstituicaoResponse;
import com.fatecrepository.dto.response.PerfilPublicoUsuarioResponse;
import com.fatecrepository.dto.response.ProjetoResponse;
import com.fatecrepository.exception.ResourceNotFoundException;
import com.fatecrepository.mapper.ResponseMapper;
import com.fatecrepository.model.Projeto;
import com.fatecrepository.model.ProjetoEstado;
import com.fatecrepository.model.User;
import com.fatecrepository.model.UserRole;
import com.fatecrepository.repository.ProfessorInstituicaoRepository;
import com.fatecrepository.repository.ProjetoRepository;
import com.fatecrepository.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UsuarioService {

    private final UserRepository userRepository;
    private final ProfessorInstituicaoRepository professorInstituicaoRepository;
    private final ProjetoRepository projetoRepository;
    private final ResponseMapper responseMapper;

    public PerfilPublicoUsuarioResponse obterPerfilPublico(UUID usuarioId) {
        log.info("Obtendo perfil público do usuário: {}", usuarioId);

        User usuario = userRepository.findById(usuarioId)
            .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        List<InstituicaoResponse> instituicoes = Collections.emptyList();
        if (usuario.getRole() == UserRole.PROFESSOR) {
            instituicoes = professorInstituicaoRepository.buscarComInstituicao(usuarioId).stream()
                .map(pi -> responseMapper.toInstituicaoResponse(pi.getInstituicao()))
                .toList();
        }

        // LGPD & Regra de Negócio: vitrine pública só traz projetos estritamente APROVADOS.
        // Se aluno: projetos que ele é autor.
        // Se professor: projetos que ele é autor OU onde é o professor avaliador responsável.
        List<Projeto> projetosEntidades = projetoRepository.findProjetosAprovadosPorUsuario(
            usuarioId, usuario.getEmail(), ProjetoEstado.APROVADO
        );

        List<ProjetoResponse> projetos = projetosEntidades.stream()
            .map(p -> {
                ProjetoResponse resp = responseMapper.toProjetoResponse(p);
                enriquecerESanitizarProjeto(resp);
                return resp;
            })
            .toList();

        return PerfilPublicoUsuarioResponse.builder()
            .id(usuario.getId())
            .nome(usuario.getNome())
            .role(usuario.getRole())
            .fotoUrl(usuario.getFotoUrl())
            .instituicoes(instituicoes)
            .projetos(projetos)
            .build();
    }

    private void enriquecerESanitizarProjeto(ProjetoResponse resp) {
        if (resp == null) return;
        if (resp.getEmailProfessorResponsavel() != null) {
            userRepository.findByEmail(resp.getEmailProfessorResponsavel())
                .ifPresent(u -> {
                    resp.setNomeProfessorResponsavel(u.getNome());
                    if (u.getRole() == UserRole.PROFESSOR) {
                        resp.setProfessorResponsavelId(u.getId());
                    }
                });
        }
        // LGPD: remove e-mail e motivo de rejeição na vitrine pública
        resp.setEmailProfessorResponsavel(null);
        resp.setMotivoRejeicao(null);
    }
}
