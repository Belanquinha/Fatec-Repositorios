package com.fatecrepository.service;

import com.fatecrepository.model.Projeto;
import com.fatecrepository.model.ProjetoEstado;
import com.fatecrepository.repository.ProjetoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Purge diário de projetos REMOVIDO com mais de 14 dias: apaga a linha e os arquivos.
 * A anonimização já aconteceu no soft delete; aqui a obra sai definitivamente do sistema.
 */
@Slf4j
@Service
public class ProjetoPurgeService {

    private final ProjetoRepository projetoRepository;
    private final FileStorageService fileStorageService;
    private final JogoStorageService jogoStorageService;
    /** Self-reference via proxy: garante @Transactional nas chamadas do scheduler. */
    private final ProjetoPurgeService self;

    public ProjetoPurgeService(ProjetoRepository projetoRepository,
                               FileStorageService fileStorageService,
                               JogoStorageService jogoStorageService,
                               @Lazy ProjetoPurgeService self) {
        this.projetoRepository = projetoRepository;
        this.fileStorageService = fileStorageService;
        this.jogoStorageService = jogoStorageService;
        this.self = self;
    }

    @Value("${app.projeto.purgacao.enabled:true}")
    private boolean enabled;

    @Value("${app.projeto.purgacao.dias:14}")
    private int dias;

    @Scheduled(cron = "${app.projeto.purgacao.cron:0 17 3 * * *}")
    public void agendarPurge() {
        if (!enabled) {
            log.debug("[purge] purgação de projetos desligada por configuração");
            return;
        }
        self.purgarExpirados();
    }

    @Transactional
    public int purgarExpirados() {
        LocalDateTime limite = LocalDateTime.now().minusDays(dias);
        List<Projeto> expirados = projetoRepository.findByEstadoAndRemovidoEmBefore(ProjetoEstado.REMOVIDO, limite);
        int removidos = 0;
        for (Projeto projeto : expirados) {
            try {
                self.purgarUm(projeto);
                removidos++;
            } catch (Exception e) {
                log.error("[purge] falha ao purgar projeto {}", projeto.getId(), e);
            }
        }
        log.info("[purge] execução concluída: {} projeto(s) removido(s) definitivamente", removidos);
        return removidos;
    }

    /**
     * Um projeto por transação: uma falha de arquivo/deleção não descarta os demais
     * e o rollback não deixa metade do purge aplicada.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void purgarUm(Projeto projeto) {
        List<String> arquivos = nomesArquivosDoProjeto(projeto);
        List<String> pacotesJogo = pacotesJogoDoProjeto(projeto);
        List<UUID> integrantesIds = projeto.getIntegrantes().stream()
            .map(com.fatecrepository.model.Integrante::getId)
            .filter(java.util.Objects::nonNull)
            .toList();
        for (UUID integranteId : integrantesIds) {
            projetoRepository.excluirIntegrantePorId(integranteId);
        }
        projetoRepository.excluirPorId(projeto.getId());
        // Mesma transação da deleção: se a limpeza de arquivo falhar, a linha não
        // some sem dono e o próximo purge tenta de novo.
        self.removerArquivos(projeto.getId(), arquivos);
        if (!pacotesJogo.isEmpty()) {
            jogoStorageService.excluirPacotes(pacotesJogo);
        }
    }

    private List<String> pacotesJogoDoProjeto(Projeto projeto) {
        List<String> pacotes = new ArrayList<>();
        if (projeto.getArquivoJogoId() != null && !projeto.getArquivoJogoId().isBlank()) {
            pacotes.add(projeto.getArquivoJogoId());
        }
        if (projeto.getDadosOriginais() != null && !projeto.getDadosOriginais().isBlank()) {
            try {
                Map<?, ?> dados = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(projeto.getDadosOriginais(), Map.class);
                Object id = dados.get("arquivoJogoId");
                if (id != null && !id.toString().isBlank()) {
                    pacotes.add(id.toString());
                }
            } catch (Exception ignored) {}
        }
        return pacotes.stream().distinct().toList();
    }

    /**
     * A anonimização do soft delete zera capa/conteúdo, então o purge usa os nomes
     * guardados no snapshot de reversão ({@code dadosOriginais.arquivos}).
     */
    private List<String> nomesArquivosDoProjeto(Projeto projeto) {
        List<String> nomes = new ArrayList<>(fileStorageService.nomesArquivosDeProjeto(projeto));
        if (projeto.getDadosOriginais() == null || projeto.getDadosOriginais().isBlank()) {
            return nomes;
        }
        try {
            Map<?, ?> dados = new com.fasterxml.jackson.databind.ObjectMapper()
                .readValue(projeto.getDadosOriginais(), Map.class);
            Object arquivos = dados.get("arquivos");
            if (arquivos instanceof List<?> lista) {
                for (Object item : lista) {
                    if (item != null && !item.toString().isBlank()) {
                        nomes.add(item.toString());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[purge] snapshot de arquivos ilegível no projeto {}", projeto.getId(), e);
        }
        return nomes.stream().distinct().toList();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void removerArquivos(UUID projetoId, List<String> arquivos) {
        try {
            fileStorageService.excluirArquivos(arquivos);
        } catch (RuntimeException e) {
            log.error("[purge] falha ao remover arquivos do projeto {}", projetoId, e);
            throw e;
        }
    }
}
