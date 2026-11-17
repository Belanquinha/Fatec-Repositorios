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
import com.fatecrepository.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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
    private final NotificacaoProjetoService notificacaoProjetoService;

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

        // Story 3.2 + MSAL CPS-only: o responsável precisa ser alcançável pelo login
        // Microsoft (só @cps.sp.gov.br vira PROFESSOR; @aluno.* e externo caem em ALUNO
        // e nunca conseguiriam aprovar — seria órfão permanente). Falha rápido aqui.
        String emailResponsavel = request.getEmailProfessorResponsavel().trim().toLowerCase();
        if (emailResponsavel.endsWith("@aluno.cps.sp.gov.br") || !emailResponsavel.endsWith("@cps.sp.gov.br")) {
            throw new BadRequestException("O e-mail do professor responsável deve ser institucional (@cps.sp.gov.br)");
        }
        userRepository.findByEmail(emailResponsavel).ifPresent(u -> {
            if (u.getRole() != UserRole.PROFESSOR) {
                throw new BadRequestException("O e-mail informado pertence a um usuário que não é professor");
            }
        });

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
        projeto.setEmailProfessorResponsavel(emailResponsavel); // Regra AD-6
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

        registrarNotificacaoPosCommit(salvo);

        ProjetoResponse resp = responseMapper.toProjetoResponse(salvo);
        preencherNomeProfessor(resp);

        return resp;
    }

    /**
     * Detalhe de um projeto.
     *
     * <p>O endpoint é {@code permitAll} (a vitrine pública linka o detalhe de
     * aprovados sem sessão), então a visibilidade é decidida aqui, pelo estado
     * do projeto e por quem chama:
     *
     * <ul>
     *   <li>{@code APROVADO} → público, como antes;
     *   <li>qualquer outro estado → só o autor (acompanhamento) ou o professor
     *       responsável indicado na submissão (revisão/avaliação). ADMIN não é
     *       autor nem avaliador e não enxerga o projeto.
     * </ul>
     *
     * <p>Quem não pode ver recebe 404 e não 403 de propósito: um 403 confirmaria
     * que o UUID existe, e a simples existência de um projeto pendente de
     * aprovação não é informação pública.
     */
    @Transactional(readOnly = true)
    public ProjetoResponse obterPorId(UUID id, CustomUserDetails usuario) {
        log.info("Buscando projeto por ID: {}", id);
        Projeto projeto = projetoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Projeto não encontrado"));

        if (projeto.getEstado() != ProjetoEstado.APROVADO && !podeVerNaoAprovado(projeto, usuario)) {
            log.info("GET /projetos/{} negado para {}: projeto {} não está aprovado",
                id, usuario == null ? "anônimo" : usuario.getUsername(), projeto.getEstado());
            throw new ResourceNotFoundException("Projeto não encontrado");
        }

        ProjetoResponse resp = responseMapper.toProjetoResponse(projeto);
        preencherNomeProfessor(resp);
        if (projeto.getEstado() == ProjetoEstado.APROVADO) {
            // Vitrine pública: nome da conta, nunca e-mail, e motivo de rejeição
            // nenhum — projeto aprovado não tem rejeição para exibir.
            resp.setEmailProfessorResponsavel(null);
            resp.setMotivoRejeicao(null);
        }
        // Visão interna (autor/professor responsável): mantém o e-mail operacional
        // e o motivo da rejeição, que é o retorno que o autor precisa ler.
        return resp;
    }

    /**
     * Autor ou professor responsável. A regra é a mesma da avaliação
     * ({@code emailProfessorResponsavel}): quem indica o responsável é quem
     * revisa, e quem submeteu é quem acompanha. ADMIN fica de fora — é papel de
     * gestão do catálogo, sem fluxo de submissão nem de revisão.
     */
    private boolean podeVerNaoAprovado(Projeto projeto, CustomUserDetails usuario) {
        if (usuario == null) {
            return false;
        }
        if (projeto.getAutor() != null && projeto.getAutor().getId().equals(usuario.getId())) {
            return true;
        }
        return projeto.getEmailProfessorResponsavel() != null
            && projeto.getEmailProfessorResponsavel().equalsIgnoreCase(usuario.getUsername());
    }

    @Transactional(readOnly = true)
    public List<ProjetoResponse> obterMeusProjetos(UUID autorId) {
        log.info("Buscando projetos do autorId: {}", autorId);
        List<Projeto> projetos = projetoRepository.findByAutorIdOrderByCriadoEmDesc(autorId);
        return projetos.stream()
            .map(p -> {
                ProjetoResponse resp = responseMapper.toProjetoResponse(p);
                preencherNomeProfessor(resp);
                return resp;
            })
            .toList();
    }

    @Transactional(readOnly = true)
    public List<ProjetoResponse> listarPublicos() {
        log.info("Buscando projetos públicos com estado APROVADO");
        List<Projeto> projetos = projetoRepository.findByEstadoOrderByCriadoEmDesc(ProjetoEstado.APROVADO);
        return projetos.stream()
            .map(p -> {
                ProjetoResponse resp = responseMapper.toProjetoResponse(p);
                preencherNomeProfessor(resp);
                // Vitrine pública: nome da conta, nunca e-mail (LGPD).
                resp.setEmailProfessorResponsavel(null);
                resp.setMotivoRejeicao(null);
                return resp;
            })
            .toList();
    }

    @Transactional(readOnly = true)
    public List<ProjetoResponse> listarPendentesParaProfessor(UUID professorId) {
        User professor = userRepository.findById(professorId)
            .orElseThrow(() -> new ResourceNotFoundException("Professor não encontrado"));
        if (professor.getRole() != UserRole.PROFESSOR) {
            throw new AccessDeniedException("Somente professores podem avaliar projetos");
        }
        // Regra vigente: fila por e-mail do responsável (sem barreira de vínculo).
        // O vínculo continua como organização da fila no front (aviso "revise a
        // instituição"), não como autorização — o vínculo é auto-declarado e a
        // identidade já é garantida pelo MSAL. Filtro ativo mantido (inativa some da fila).
        List<Projeto> candidatos = projetoRepository
            .findByEstadoAndEmailProfessorResponsavelIgnoreCaseOrderByCriadoEmDesc(
                ProjetoEstado.AGUARDANDO_APROVACAO, professor.getEmail());
        return candidatos.stream()
            .filter(p -> p.getInstituicao() != null && p.getInstituicao().isAtivo())
            .map(p -> {
                ProjetoResponse resp = responseMapper.toProjetoResponse(p);
                preencherNomeProfessor(resp);
                return resp;
            })
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
        ProjetoResponse respAprovado = responseMapper.toProjetoResponse(salvo);
        preencherNomeProfessor(respAprovado);
        return respAprovado;
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
        ProjetoResponse respRejeitado = responseMapper.toProjetoResponse(salvo);
        preencherNomeProfessor(respRejeitado);
        return respRejeitado;
    }

    /**
     * Preenche {@code nomeProfessorResponsavel} a partir da conta cadastrada.
     * "Professor Convidado" (e-mail livre, Story 3.2) fica com nome null — o front
     * exibe o rótulo de convidado nesse caso. Nunca falha: lookup é best-effort.
     */
    private void preencherNomeProfessor(ProjetoResponse resp) {
        if (resp == null || resp.getEmailProfessorResponsavel() == null) {
            return;
        }
        try {
            userRepository.findByEmail(resp.getEmailProfessorResponsavel())
                .ifPresent(u -> resp.setNomeProfessorResponsavel(u.getNome()));
        } catch (Exception e) {
            log.warn("Não foi possível resolver o nome do professor responsável", e);
        }
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
     * ({@code emailProfessorResponsavel}) pode aprovar/rejeitar — sem exigir vínculo
     * com a instituição (o vínculo é auto-declarado; a identidade vem do MSAL).
     * O front exibe aviso para revisar a instituição quando não vinculado.
     */
    private void verificarPermissaoAvaliacao(Projeto projeto, User professor) {
            if (projeto.getEmailProfessorResponsavel() == null
                || !projeto.getEmailProfessorResponsavel().equalsIgnoreCase(professor.getEmail())) {
                throw new AccessDeniedException("Somente o professor responsável indicado na submissão pode avaliar este projeto");
            }
        }

        /**
         * Agenda a notificação de submissão para depois do commit.
         *
         * <p>Pós-commit é o que garante a regra de ouro: se o provedor de e-mail cair,
         * o projeto permanece {@code AGUARDANDO_APROVACAO}. Um rollback por falha de
         * envio nunca pode desfazer uma submissão válida. Os valores são copiados da
         * entidade aqui, ainda dentro da transação, porque a entrega é assíncrona e
         * não haverá mais sessão JPA aberta quando ela rodar.
         */
        private void registrarNotificacaoPosCommit(Projeto projeto) {
            if (!TransactionSynchronizationManager.isSynchronizationActive()) {
                log.warn("Sem sincronização de transação ativa: notificação do projeto {} não será disparada",
                    projeto.getId());
                return;
            }
            ProjetoSubmetidoNotificacao dados = new ProjetoSubmetidoNotificacao(
                projeto.getId(),
                projeto.getTitulo(),
                projeto.getInstituicao() != null ? projeto.getInstituicao().getNome() : null,
                projeto.getEmailProfessorResponsavel(),
                projeto.getCriadoEm() != null ? projeto.getCriadoEm().toLocalDate() : LocalDate.now());
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    notificacaoProjetoService.notificarProjetoSubmetido(dados);
                }
            });
        }
    }
