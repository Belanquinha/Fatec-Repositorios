package com.fatecrepository.service;

import com.fatecrepository.dto.request.IntegranteRequest;
import com.fatecrepository.dto.response.ExclusaoProjetoResponse;
import com.fatecrepository.dto.response.ProjetoRemovidoResponse;
import com.fatecrepository.dto.response.ProjetoResponse;
import com.fatecrepository.exception.BadRequestException;
import com.fatecrepository.exception.ConflictException;
import com.fatecrepository.exception.ResourceNotFoundException;
import com.fatecrepository.mapper.ResponseMapper;
import com.fatecrepository.model.Integrante;
import com.fatecrepository.model.MotivoRemocao;
import com.fatecrepository.model.Projeto;
import com.fatecrepository.model.ProjetoEstado;
import com.fatecrepository.repository.ProjetoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Exclusão, anonimização e reversão de projetos (SPEC-exclusao-projetos fase 1).
 *
 * <p>Dois caminhos deliberadamente distintos:
 * <ul>
 *   <li>não-público ({@code AGUARDANDO_APROVACAO}, {@code REJEITADO}) → hard delete;</li>
 *   <li>público ({@code APROVADO}) → soft delete para {@code REMOVIDO}, com anonimização
 *       imediata e reversão pelo autor em até {@value #PRAZO_REVERSAO_DIAS} dias.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProjetoExclusaoService {

    public static final int PRAZO_REVERSAO_DIAS = 14;

    private final ProjetoRepository projetoRepository;
    private final FileStorageService fileStorageService;
    private final ResponseMapper responseMapper;

    private static final com.fasterxml.jackson.databind.ObjectMapper JSON =
        new com.fasterxml.jackson.databind.ObjectMapper();

    @Transactional
    public ExclusaoProjetoResponse excluir(UUID projetoId, UUID autorId, String motivoRemocao) {
        Projeto projeto = buscarProjetoDoAutor(projetoId, autorId);
        validarMotivoRemocao(motivoRemocao);
        String motivo = normalizarMotivo(motivoRemocao);

        ProjetoEstado estadoAtual = projeto.getEstado();
        if (estadoAtual == ProjetoEstado.REMOVIDO) {
            throw new ResourceNotFoundException("Projeto não encontrado");
        }
        if (estadoAtual == ProjetoEstado.APROVADO) {
            return removerAprovado(projeto, autorId, motivo);
        }
        if (estadoAtual == ProjetoEstado.AGUARDANDO_APROVACAO || estadoAtual == ProjetoEstado.REJEITADO) {
            return hardDelete(projeto, motivo);
        }
        throw new ConflictException("O projeto não pode ser excluído no estado atual: " + estadoAtual.name());
    }

    private ExclusaoProjetoResponse removerAprovado(Projeto projeto, UUID autorId, String motivo) {
        String dadosOriginais = capturarDadosOriginais(projeto);
        LocalDateTime removidoEm = LocalDateTime.now();

        projeto.setDadosOriginais(dadosOriginais);
        projeto.setEstadoAnterior(projeto.getEstado());
        projeto.setRemovidoEm(removidoEm);
        projeto.setRemovidoPorId(autorId);
        projeto.setMotivoRemocaoGenerico(motivo);
        projeto.setEstado(ProjetoEstado.REMOVIDO);
        projeto.setAtualizadoEm(removidoEm);
        anonimizar(projeto);

        Projeto salvo = projetoRepository.save(projeto);
        log.info("Projeto {} removido pelo autor {} (estado anterior {})",
            salvo.getId(), autorId, salvo.getEstadoAnterior());

        return new ExclusaoProjetoResponse(salvo.getId(), salvo.getTitulo(), salvo.getEstado().name(), salvo.getRemovidoEm());
    }

    /**
     * Hard delete definitivo para a exclusão de conta (§4.6): apaga linha e arquivos
     * de <em>qualquer</em> estado do autor — inclusive {@code REMOVIDO}, cujos nomes de
     * arquivo estão apenas no snapshot de reversão. Não valida motivo nem estado:
     * quem chama é a conta sendo encerrada, não o fluxo de exclusão comum.
     */
    @Transactional
    public void excluirDefinitivamente(Projeto projeto) {
        List<String> arquivos = new ArrayList<>(fileStorageService.nomesArquivosDeProjeto(projeto));
        arquivos.addAll(arquivosDoSnapshot(projeto));

        UUID id = projeto.getId();
        // Bulk delete por pai: não depende da coleção lazy estar inicializada
        // (na exclusão de conta o lote vem de entidades potencialmente detachadas).
        projetoRepository.excluirIntegrantesPorProjeto(id);
        projetoRepository.excluirPorId(id);

        fileStorageService.excluirArquivos(arquivos.stream().distinct().toList());
        log.info("Projeto {} removido definitivamente (exclusão de conta)", id);
    }

    /** Nomes guardados em {@code dadosOriginais.arquivos} (pós-anonimização do soft delete). */
    private List<String> arquivosDoSnapshot(Projeto projeto) {
        if (projeto.getDadosOriginais() == null || projeto.getDadosOriginais().isBlank()) {
            return List.of();
        }
        try {
            Map<?, ?> dados = JSON.readValue(projeto.getDadosOriginais(), Map.class);
            Object arquivos = dados.get("arquivos");
            if (arquivos instanceof List<?> lista) {
                return lista.stream()
                    .filter(java.util.Objects::nonNull)
                    .map(Object::toString)
                    .filter(nome -> !nome.isBlank())
                    .toList();
            }
        } catch (Exception e) {
            log.warn("[exclusao-conta] snapshot de arquivos ilegível no projeto {}", projeto.getId(), e);
        }
        return List.of();
    }

    private ExclusaoProjetoResponse hardDelete(Projeto projeto, String motivo) {
        UUID id = projeto.getId();
        String titulo = projeto.getTitulo();

        // Snapshot antes de apagar: o purge/deleção precisa dos nomes de arquivo.
        List<String> arquivos = fileStorageService.nomesArquivosDeProjeto(projeto);
        List<UUID> integrantesIds = projeto.getIntegrantes().stream()
            .map(com.fatecrepository.model.Integrante::getId)
            .filter(java.util.Objects::nonNull)
            .toList();
        // Bulk delete por query: a FK de integrantes não é CASCADE no banco.
        for (UUID integranteId : integrantesIds) {
            projetoRepository.excluirIntegrantePorId(integranteId);
        }
        projetoRepository.excluirPorId(id);

        // Fora da transação de banco, mas ainda dentro do request: falha de arquivo
        // é logada e não desfaz o hard delete (o faxineiro de órfãos é rede de segurança).
        fileStorageService.excluirArquivos(new ArrayList<>(arquivos));

        log.info("Projeto {} hard delete pelo autor (motivo {})", id, motivo);
        return new ExclusaoProjetoResponse(id, titulo, "EXCLUIDO", null);
    }

    private void anonimizar(Projeto projeto) {
        if (projeto.getIntegrantes() != null) {
            for (Integrante integrante : projeto.getIntegrantes()) {
                integrante.setNome("[removido]");
                integrante.setLinkLinkedin(null);
            }
        }
        projeto.setEmailProfessorResponsavel(null);
        projeto.setImagemCapaUrl(null);
        projeto.setConteudoEditorJs(null);
        projeto.setDescricaoCurta(null);
        projeto.setLinkRepositorio(null);
        projeto.setPalavrasChave(new ArrayList<>());
    }

    /** Guarda o necessário para a reversão; nunca é exposto em DTO/response. */
    private String capturarDadosOriginais(Projeto projeto) {
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("integrantes", projeto.getIntegrantes().stream()
            .map(i -> Map.of(
                "id", i.getId() != null ? i.getId().toString() : null,
                "nome", i.getNome(),
                "linkLinkedin", i.getLinkLinkedin() == null ? "" : i.getLinkLinkedin()
            ))
            .toList());
        dados.put("emailProfessorResponsavel", projeto.getEmailProfessorResponsavel());
        dados.put("imagemCapaUrl", projeto.getImagemCapaUrl());
        dados.put("conteudoEditorJs", projeto.getConteudoEditorJs());
        // A anonimização zera capa/conteúdo, então sem registrar os nomes aqui o purge
        // (que roda depois do soft delete) não saberia quais arquivos apagar.
        dados.put("arquivos", fileStorageService.nomesArquivosDeProjeto(projeto));
        dados.put("descricaoCurta", projeto.getDescricaoCurta());
        dados.put("linkRepositorio", projeto.getLinkRepositorio());
        dados.put("palavrasChave", projeto.getPalavrasChave());
        dados.put("estadoAnterior", projeto.getEstado().name());
        try {
            return JSON.writeValueAsString(dados);
        } catch (Exception e) {
            throw new IllegalStateException("Não foi possível guardar os dados originais para reversão", e);
        }
    }

    private void validarMotivoRemocao(String motivoRemocao) {
        if (motivoRemocao == null || motivoRemocao.isBlank()) {
            throw new BadRequestException("Motivo da remoção inválido");
        }
        if (!MotivoRemocao.contem(motivoRemocao)) {
            throw new BadRequestException("Motivo da remoção inválido");
        }
    }

    private String normalizarMotivo(String motivoRemocao) {
        return MotivoRemocao.valueOf(motivoRemocao.trim().toUpperCase()).name();
    }

    @Transactional(readOnly = true)
    public List<ProjetoRemovidoResponse> listarRemovidos(UUID autorId) {
        List<Projeto> removidos = projetoRepository
            .findByAutorIdAndEstadoOrderByRemovidoEmDesc(autorId, ProjetoEstado.REMOVIDO);
        return removidos.stream().map(this::toRemovidoResponse).toList();
    }

    private ProjetoRemovidoResponse toRemovidoResponse(Projeto projeto) {
        long dias = ChronoUnit.DAYS.between(
            projeto.getRemovidoEm().toLocalDate(), LocalDateTime.now().toLocalDate());
        long restantes = Math.max(0, PRAZO_REVERSAO_DIAS - dias);
        return new ProjetoRemovidoResponse(
            projeto.getId(),
            projeto.getTitulo(),
            projeto.getEstadoAnterior() != null ? projeto.getEstadoAnterior().name() : null,
            projeto.getRemovidoEm(),
            projeto.getMotivoRemocaoGenerico(),
            restantes);
    }

    @Transactional
    public ProjetoResponse reverter(UUID projetoId, UUID autorId) {
        Projeto projeto = buscarProjetoDoAutor(projetoId, autorId);
        if (projeto.getEstado() != ProjetoEstado.REMOVIDO) {
            throw new ResourceNotFoundException("Projeto não encontrado");
        }
        // A janela de reversão é fechada ANTES de limpar o snapshot: se a limpeza
        // falhar, os dados originais precisam continuar disponíveis para retry.
        if (projeto.getRemovidoEm() == null
            || projeto.getRemovidoEm().plusDays(PRAZO_REVERSAO_DIAS).isBefore(LocalDateTime.now())) {
            throw new ConflictException("Prazo de reversão expirado; o projeto será removido definitivamente");
        }

        // Anexa o registro antes de mexer nas coleções: sem isso o orphanRemoval
        // tenta remover integrantes já tratados e o lock otimista falha.
        restaurarDadosOriginais(projeto);
        projeto.setEstado(projeto.getEstadoAnterior() != null ? projeto.getEstadoAnterior() : ProjetoEstado.APROVADO);
        projeto.setRemovidoEm(null);
        projeto.setRemovidoPorId(null);
        projeto.setMotivoRemocaoGenerico(null);
        projeto.setEstadoAnterior(null);
        projeto.setAtualizadoEm(LocalDateTime.now());
        projetoRepository.saveAndFlush(projeto);

        // Snapshot só é descartado depois que estado e PII já foram restaurados.
        projeto.setDadosOriginais(null);
        Projeto salvo = projetoRepository.save(projeto);
        log.info("Projeto {} revertido pelo autor {} para {}",
            salvo.getId(), autorId, salvo.getEstado());
        return responseMapper.toProjetoResponse(salvo);
    }

    private void restaurarDadosOriginais(Projeto projeto) {
        if (projeto.getDadosOriginais() == null || projeto.getDadosOriginais().isBlank()) {
            return;
        }
        try {
            Map<?, ?> dados = JSON.readValue(projeto.getDadosOriginais(), Map.class);
            projeto.setEmailProfessorResponsavel(texto(dados.get("emailProfessorResponsavel")));
            projeto.setImagemCapaUrl(texto(dados.get("imagemCapaUrl")));
            projeto.setConteudoEditorJs(texto(dados.get("conteudoEditorJs")));
            projeto.setDescricaoCurta(texto(dados.get("descricaoCurta")));
            projeto.setLinkRepositorio(texto(dados.get("linkRepositorio")));
            projeto.setPalavrasChave(extrairPalavrasChave(dados.get("palavrasChave")));

            List<Integrante> atuais = projeto.getIntegrantes();
            Map<UUID, Integrante> porId = new LinkedHashMap<>();
            for (Integrante integrante : atuais) {
                if (integrante.getId() != null) {
                    porId.put(integrante.getId(), integrante);
                }
            }
            List<Integrante> restaurados = new ArrayList<>();
            java.util.Set<UUID> restauradosIds = new java.util.HashSet<>();
            Object integrantesRaw = dados.get("integrantes");
            if (integrantesRaw instanceof List<?> lista) {
                for (Object item : lista) {
                    if (!(item instanceof Map<?, ?> mapa)) {
                        continue;
                    }
                    UUID id = uuidDe(mapa.get("id"));
                    Integrante integrante = id != null ? porId.get(id) : null;
                    if (integrante == null) {
                        integrante = new Integrante();
                        integrante.setProjeto(projeto);
                    }
                    if (id != null) {
                        restauradosIds.add(id);
                    }
                    integrante.setNome(texto(mapa.get("nome")));
                    integrante.setLinkLinkedin(texto(mapa.get("linkLinkedin")));
                    restaurados.add(integrante);
                }
            }

            // Integrantes atuais que não estavam no snapshot são removidos antes de
            // readicionar a coleção, senão o orphanRemoval e o lock otimista colidem.
            for (Integrante integrante : new ArrayList<>(atuais)) {
                if (integrante.getId() != null && !restauradosIds.contains(integrante.getId())) {
                    projeto.getIntegrantes().remove(integrante);
                    projetoRepository.excluirIntegrantePorId(integrante.getId());
                }
            }
            projeto.getIntegrantes().clear();
            projeto.getPalavrasChave().clear();
            projeto.getPalavrasChave().addAll(extrairPalavrasChave(dados.get("palavrasChave")));

            for (Integrante integrante : restaurados) {
                if (integrante.getId() == null) {
                    projeto.adicionarIntegrante(integrante);
                } else {
                    projeto.getIntegrantes().add(integrante);
                    integrante.setProjeto(projeto);
                }
            }
        } catch (Exception e) {
            log.error("Falha ao restaurar dados originais do projeto {}", projeto.getId(), e);
            throw new ConflictException("Não foi possível restaurar os dados originais do projeto");
        }
    }

    private List<String> extrairPalavrasChave(Object raw) {
        List<String> palavras = new ArrayList<>();
        if (raw instanceof List<?> lista) {
            for (Object item : lista) {
                String valor = texto(item);
                if (!valor.isEmpty()) {
                    palavras.add(valor);
                }
            }
        }
        return palavras;
    }

    private String texto(Object valor) {
        return valor == null ? null : valor.toString();
    }

    private UUID uuidDe(Object valor) {
        String texto = texto(valor);
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(texto);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private Projeto buscarProjetoDoAutor(UUID projetoId, UUID autorId) {
        Projeto projeto = projetoRepository.findById(projetoId)
            .orElseThrow(() -> new ResourceNotFoundException("Projeto não encontrado"));
        if (projeto.getAutor() == null || !projeto.getAutor().getId().equals(autorId)) {
            throw new AccessDeniedException("Somente o autor do projeto pode realizar esta operação");
        }
        return projeto;
    }

    /** Usado pelo purge para apagar arquivos de um registro já anonimizado. */
    @Transactional(readOnly = true)
    public List<String> nomesArquivosDeProjeto(Projeto projeto) {
        return fileStorageService.nomesArquivosDeProjeto(projeto);
    }
}
