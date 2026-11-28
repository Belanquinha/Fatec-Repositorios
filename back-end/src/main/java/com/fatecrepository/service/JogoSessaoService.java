package com.fatecrepository.service;

import com.fatecrepository.dto.response.SessaoJogoMetadataResponse;
import com.fatecrepository.dto.response.SessaoJogoResponse;
import com.fatecrepository.exception.BadRequestException;
import com.fatecrepository.exception.ResourceNotFoundException;
import com.fatecrepository.model.Projeto;
import com.fatecrepository.model.ProjetoEstado;
import com.fatecrepository.model.TipoProjeto;
import com.fatecrepository.repository.ProjetoRepository;
import com.fatecrepository.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class JogoSessaoService {

    private final ProjetoRepository projetoRepository;
    private final JogoStorageService jogoStorageService;

    @Value("${app.jogos.ticket-ttl-segundos:300}")
    private long ticketTtlSegundos;

    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, SessaoAtiva> sessoes = new ConcurrentHashMap<>();

    public record SessaoAtiva(
        UUID projetoId,
        String titulo,
        String arquivoJogoId,
        String nomeOriginal,
        long tamanhoBytes,
        String sha256,
        String executavelPrincipal,
        Instant expiraEm
    ) {}

    @Transactional(readOnly = true)
    public SessaoJogoResponse criarSessao(UUID projetoId, CustomUserDetails usuario) {
        limparExpiradas();

        Projeto projeto = projetoRepository.findById(projetoId)
            .orElseThrow(() -> new ResourceNotFoundException("Projeto não encontrado"));

        if (projeto.getEstado() == ProjetoEstado.REMOVIDO) {
            throw new ResourceNotFoundException("Projeto não encontrado");
        }

        if (projeto.getEstado() != ProjetoEstado.APROVADO && !podeAcessarNaoAprovado(projeto, usuario)) {
            throw new ResourceNotFoundException("Projeto não encontrado");
        }

        if (projeto.getTipoProjeto() != TipoProjeto.JOGO || projeto.getArquivoJogoId() == null) {
            throw new BadRequestException("Este projeto não possui um jogo executável associado.");
        }

        String ticket = gerarTicket();
        Instant expiraEm = Instant.now().plusSeconds(ticketTtlSegundos);
        SessaoAtiva sessao = new SessaoAtiva(
            projeto.getId(),
            projeto.getTitulo(),
            projeto.getArquivoJogoId(),
            projeto.getArquivoJogoNomeOriginal() != null ? projeto.getArquivoJogoNomeOriginal() : "jogo.zip",
            projeto.getArquivoJogoTamanhoBytes() != null ? projeto.getArquivoJogoTamanhoBytes() : 0L,
            projeto.getArquivoJogoSha256(),
            projeto.getExecutavelPrincipal(),
            expiraEm
        );
        sessoes.put(ticket, sessao);
        log.info("Sessão de jogo criada para projeto {} (TTL {}s)", projetoId, ticketTtlSegundos);
        return new SessaoJogoResponse(ticket, ticketTtlSegundos);
    }

    public SessaoJogoMetadataResponse obterMetadados(String ticket) {
        SessaoAtiva sessao = validarTicket(ticket);
        return new SessaoJogoMetadataResponse(
            sessao.projetoId(),
            sessao.titulo(),
            sessao.nomeOriginal(),
            sessao.tamanhoBytes(),
            sessao.sha256(),
            sessao.executavelPrincipal()
        );
    }

    public Resource abrirPacote(String ticket) {
        SessaoAtiva sessao = validarTicket(ticket);
        Resource resource = jogoStorageService.abrirRecurso(sessao.arquivoJogoId());
        if (resource == null || !resource.exists()) {
            throw new ResourceNotFoundException("Arquivo do jogo não encontrado no servidor.");
        }
        return resource;
    }

    private SessaoAtiva validarTicket(String ticket) {
        limparExpiradas();
        if (ticket == null || ticket.isBlank()) {
            throw new ResourceNotFoundException("Sessão de jogo inválida ou expirada.");
        }
        SessaoAtiva sessao = sessoes.get(ticket.trim());
        if (sessao == null || sessao.expiraEm().isBefore(Instant.now())) {
            sessoes.remove(ticket.trim());
            throw new ResourceNotFoundException("Sessão de jogo inválida ou expirada.");
        }
        return sessao;
    }

    private boolean podeAcessarNaoAprovado(Projeto projeto, CustomUserDetails usuario) {
        if (usuario == null) {
            return false;
        }
        if (projeto.getAutor() != null && projeto.getAutor().getId().equals(usuario.getId())) {
            return true;
        }
        return projeto.getEmailProfessorResponsavel() != null
            && projeto.getEmailProfessorResponsavel().equalsIgnoreCase(usuario.getUsername());
    }

    private String gerarTicket() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private void limparExpiradas() {
        Instant agora = Instant.now();
        sessoes.entrySet().removeIf(entry -> entry.getValue().expiraEm().isBefore(agora));
    }
}
