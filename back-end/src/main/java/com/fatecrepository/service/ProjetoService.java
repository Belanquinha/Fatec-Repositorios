package com.fatecrepository.service;

import com.fatecrepository.dto.request.IntegranteRequest;
import com.fatecrepository.dto.request.ProjetoCreateRequest;
import com.fatecrepository.dto.response.ProjetoResponse;
import com.fatecrepository.exception.BadRequestException;
import com.fatecrepository.exception.ConflictException;
import com.fatecrepository.exception.ResourceNotFoundException;
import com.fatecrepository.mapper.ResponseMapper;
import com.fatecrepository.model.*;
import com.fatecrepository.repository.InstituicaoRepository;
import com.fatecrepository.repository.ProfessorInstituicaoRepository;
import com.fatecrepository.repository.ProjetoRepository;
import com.fatecrepository.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjetoService {

    private final ProjetoRepository projetoRepository;
    private final InstituicaoRepository instituicaoRepository;
    private final UserRepository userRepository;
    private final ProfessorInstituicaoRepository vinculoRepository;
    private final ResponseMapper responseMapper;

    @Transactional
    public ProjetoResponse criar(ProjetoCreateRequest request, UUID autorId) {
        log.info("Criando novo projeto: {} pelo autorId: {}", request.getTitulo(), autorId);

        User autor = userRepository.findById(autorId)
            .orElseThrow(() -> new ResourceNotFoundException("Usuário autor não encontrado"));

        Instituicao instituicao = instituicaoRepository.findById(request.getInstituicaoId())
            .orElseThrow(() -> new ResourceNotFoundException("Instituição informada não encontrada"));

        if (!instituicao.isAtivo()) {
            throw new BadRequestException("A instituição selecionada está inativa no catálogo");
        }

        if (request.getIntegrantes() == null || request.getIntegrantes().isEmpty()) {
            throw new BadRequestException("O projeto deve conter pelo menos 1 integrante");
        }

        Projeto projeto = new Projeto();
        projeto.setTitulo(request.getTitulo().trim());
        projeto.setDescricaoCurta(request.getDescricaoCurta().trim());
        projeto.setConteudoEditorJs(request.getConteudoEditorJs());
        projeto.setLinkRepositorio(request.getLinkRepositorio() != null ? request.getLinkRepositorio().trim() : null);
        projeto.setImagemCapaUrl(request.getImagemCapaUrl());
        projeto.setPalavrasChave(request.getPalavrasChave() != null ? request.getPalavrasChave() : new ArrayList<>());
        projeto.setAnoPublicado(request.getAnoPublicado() != null ? request.getAnoPublicado() : LocalDate.now().getYear());
        projeto.setEstado(ProjetoEstado.AGUARDANDO_APROVACAO); // Regra AD-7: nasce diretamente aguardando aprovação
        projeto.setMotivoRejeicao(null);
        projeto.setEmailProfessorResponsavel(request.getEmailProfessorResponsavel().trim().toLowerCase()); // Regra AD-6
        projeto.setInstituicao(instituicao);
        projeto.setAutor(autor);
        projeto.setCriadoEm(LocalDateTime.now());
        projeto.setAtualizadoEm(LocalDateTime.now());

        for (IntegranteRequest iReq : request.getIntegrantes()) {
            Integrante integrante = new Integrante();
            integrante.setNome(iReq.getNome().trim());
            integrante.setLinkLinkedin(iReq.getLinkLinkedin() != null ? iReq.getLinkLinkedin().trim() : null);
            integrante.setCriadoEm(LocalDateTime.now());
            projeto.adicionarIntegrante(integrante);
        }

        Projeto salvo = projetoRepository.save(projeto);
        log.info("Projeto criado com sucesso! ID: {}, Estado: {}", salvo.getId(), salvo.getEstado());

        return responseMapper.toProjetoResponse(salvo);
    }

    @Transactional(readOnly = true)
    public ProjetoResponse obterPorId(UUID id) {
        log.info("Buscando projeto por ID: {}", id);
        Projeto projeto = projetoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Projeto não encontrado"));
        return responseMapper.toProjetoResponse(projeto);
    }

    @Transactional(readOnly = true)
    public List<ProjetoResponse> obterMeusProjetos(UUID autorId) {
        log.info("Buscando projetos do autorId: {}", autorId);
        List<Projeto> projetos = projetoRepository.findByAutorIdOrderByCriadoEmDesc(autorId);
        return projetos.stream()
            .map(responseMapper::toProjetoResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<ProjetoResponse> listarPublicos() {
        log.info("Buscando projetos públicos com estado APROVADO");
        List<Projeto> projetos = projetoRepository.findByEstadoOrderByCriadoEmDesc(ProjetoEstado.APROVADO);
        return projetos.stream()
            .map(responseMapper::toProjetoResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<ProjetoResponse> listarPendentesParaProfessor(UUID professorId) {
        User professor = userRepository.findById(professorId)
            .orElseThrow(() -> new ResourceNotFoundException("Professor não encontrado"));
        if (professor.getRole() != UserRole.PROFESSOR) {
            throw new AccessDeniedException("Somente professores podem avaliar projetos");
        }
        List<UUID> instituicoesVinculadas = vinculoRepository.buscarComInstituicao(professorId).stream()
            .map(v -> v.getInstituicao() != null ? v.getInstituicao().getId() : null)
            .filter(id -> id != null)
            .toList();
        if (instituicoesVinculadas.isEmpty()) {
            return List.of();
        }
        List<Projeto> candidatos = projetoRepository
            .findByEstadoAndEmailProfessorResponsavelIgnoreCaseOrderByCriadoEmDesc(
                ProjetoEstado.AGUARDANDO_APROVACAO, professor.getEmail());
        return candidatos.stream()
            .filter(p -> p.getInstituicao() != null && instituicoesVinculadas.contains(p.getInstituicao().getId()))
            .map(responseMapper::toProjetoResponse)
            .toList();
    }

    @Transactional
    public ProjetoResponse aprovar(UUID projetoId, UUID professorId) {
        Projeto projeto = buscarProjeto(projetoId);
        User professor = buscarProfessor(professorId);
        verificarPermissaoAvaliacao(projeto, professor);
        if (projeto.getEstado() != ProjetoEstado.AGUARDANDO_APROVACAO) {
            throw new ConflictException("Projeto já avaliado (estado atual: " + projeto.getEstado().name() + ")");
        }
        projeto.setEstado(ProjetoEstado.APROVADO);
        projeto.setMotivoRejeicao(null);
        projeto.setAtualizadoEm(LocalDateTime.now());
        Projeto salvo = projetoRepository.save(projeto);
        log.info("Projeto {} APROVADO pelo professor {}", projetoId, professorId);
        return responseMapper.toProjetoResponse(salvo);
    }

    @Transactional
    public ProjetoResponse rejeitar(UUID projetoId, UUID professorId, String motivo) {
        if (motivo == null || motivo.trim().isEmpty()) {
            throw new BadRequestException("O motivo da rejeição é obrigatório");
        }
        Projeto projeto = buscarProjeto(projetoId);
        User professor = buscarProfessor(professorId);
        verificarPermissaoAvaliacao(projeto, professor);
        if (projeto.getEstado() != ProjetoEstado.AGUARDANDO_APROVACAO) {
            throw new ConflictException("Projeto já avaliado (estado atual: " + projeto.getEstado().name() + ")");
        }
        projeto.setEstado(ProjetoEstado.REJEITADO);
        projeto.setMotivoRejeicao(motivo.trim());
        projeto.setAtualizadoEm(LocalDateTime.now());
        Projeto salvo = projetoRepository.save(projeto);
        log.info("Projeto {} REJEITADO pelo professor {}", projetoId, professorId);
        return responseMapper.toProjetoResponse(salvo);
    }

    private Projeto buscarProjeto(UUID projetoId) {
        return projetoRepository.findById(projetoId)
            .orElseThrow(() -> new ResourceNotFoundException("Projeto não encontrado"));
    }

    private User buscarProfessor(UUID professorId) {
        User professor = userRepository.findById(professorId)
            .orElseThrow(() -> new ResourceNotFoundException("Professor não encontrado"));
        if (professor.getRole() != UserRole.PROFESSOR) {
            throw new AccessDeniedException("Somente professores podem avaliar projetos");
        }
        return professor;
    }

    /**
     * Regra de avaliação: somente o professor indicado como responsável na submissão
     * ({@code emailProfessorResponsavel}) E vinculado à instituição do projeto pode
     * aprovar/rejeitar. Qualquer outra combinação devolve 403.
     */
    private void verificarPermissaoAvaliacao(Projeto projeto, User professor) {
        if (projeto.getEmailProfessorResponsavel() == null
            || !projeto.getEmailProfessorResponsavel().equalsIgnoreCase(professor.getEmail())) {
            throw new AccessDeniedException("Somente o professor responsável indicado na submissão pode avaliar este projeto");
        }
        if (projeto.getInstituicao() == null
            || !vinculoRepository.existsByUsuarioIdAndInstituicaoId(professor.getId(), projeto.getInstituicao().getId())) {
            throw new AccessDeniedException("Professor não vinculado à instituição deste projeto");
        }
    }
}
