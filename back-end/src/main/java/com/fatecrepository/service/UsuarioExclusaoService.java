package com.fatecrepository.service;

import com.fatecrepository.dto.response.MensagemResponse;
import com.fatecrepository.model.Projeto;
import com.fatecrepository.model.ProjetoEstado;
import com.fatecrepository.model.User;
import com.fatecrepository.repository.EmailVerificationCodeRepository;
import com.fatecrepository.repository.ProfessorInstituicaoRepository;
import com.fatecrepository.repository.ProjetoRepository;
import com.fatecrepository.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Exclusão da própria conta (SPEC-exclusao-projetos §4.6 / CAP-D3).
 *
 * <p>Regra de negócio: a obra não morre com a conta. Os projetos {@code APROVADOS}
 * permanecem publicados vinculados à conta anonimizada ({@code "[conta removida]"});
 * os não-aprovados são apagados definitivamente, reaproveitando a limpeza de arquivos
 * e integrantes da fase 1.
 *
 * <p>Pendentes em que o usuário é o professor responsável voltam ao autor como
 * {@code ALTERACOES_SOLICITADAS} com o e-mail limpo — assim o reenvio é obrigado a
 * indicar um novo responsável, sem deixar a fila órfã.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UsuarioExclusaoService {

    /** Motivo registrado na devolução automática (texto visível ao aluno). */
    public static final String MOTIVO_CONTA_REMOVIDA = "responsável removido";

    private final UserRepository userRepository;
    private final ProjetoRepository projetoRepository;
    private final ProfessorInstituicaoRepository professorInstituicaoRepository;
    private final EmailVerificationCodeRepository emailVerificationCodeRepository;
    private final ProjetoExclusaoService projetoExclusaoService;

    @Transactional
    public MensagemResponse excluirConta(UUID usuarioId) {
        User usuario = userRepository.findById(usuarioId)
            .orElseThrow(() -> new com.fatecrepository.exception.ResourceNotFoundException("Usuário não encontrado"));
        String email = usuario.getEmail();

        // 1) Obra não-aprovada sai definitivamente; aprovada fica com o autor anonimizado.
        List<Projeto> doUsuario = projetoRepository.findByAutorIdOrderByCriadoEmDesc(usuarioId);
        int excluidos = 0;
        for (Projeto projeto : doUsuario) {
            if (projeto.getEstado() == ProjetoEstado.APROVADO) {
                continue;
            }
            projetoExclusaoService.excluirDefinitivamente(projeto);
            excluidos++;
        }

        // 2) Fila órfã: devolve ao autor e limpa o e-mail do responsável removido.
        int devolvidos = 0;
        if (email != null) {
            List<Projeto> pendentes = projetoRepository
                .findByEmailProfessorResponsavelIgnoreCaseAndEstadoIn(
                    email, List.of(ProjetoEstado.AGUARDANDO_APROVACAO, ProjetoEstado.REENVIADO));
            LocalDateTime agora = LocalDateTime.now();
            for (Projeto pendente : pendentes) {
                pendente.setEstado(ProjetoEstado.ALTERACOES_SOLICITADAS);
                pendente.setMotivoDevolucao(MOTIVO_CONTA_REMOVIDA);
                pendente.setDevolvidoEm(agora);
                pendente.setDevolvidoPorId(usuarioId);
                pendente.setEmailProfessorResponsavel(null);
                pendente.setAtualizadoEm(agora);
                devolvidos++;
            }
            if (!pendentes.isEmpty()) {
                projetoRepository.saveAll(pendentes);
            }
        }

        // 3) Vínculos e códigos de verificação (dados de recuperação da conta).
        professorInstituicaoRepository.deleteByUsuarioId(usuarioId);
        if (email != null) {
            emailVerificationCodeRepository.deleteByEmail(email);
        }

        // 4) Anonimiza a linha: email = null invalida os tokens antigos (filtro JWT
        // recarrega por e-mail e passa a receber 401).
        usuario.setNome("[conta removida]");
        usuario.setEmail(null);
        usuario.setSenhaHash(null);
        usuario.setFotoUrl(null);
        usuario.setAtualizadoEm(LocalDateTime.now());
        userRepository.save(usuario);

        log.info("Conta {} removida: {} projeto(s) excluído(s), {} pendente(s) devolvido(s)",
            usuarioId, excluidos, devolvidos);
        return new MensagemResponse("Conta removida com sucesso. Projetos publicados foram mantidos com autoria anônima.");
    }
}
