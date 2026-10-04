package com.fatecrepository.service;

import com.fatecrepository.dto.response.AuthResponse;
import com.fatecrepository.dto.response.DevAuthAccountResponse;
import com.fatecrepository.mapper.ResponseMapper;
import com.fatecrepository.model.User;
import com.fatecrepository.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/**
 * Login sem Microsoft, para desenvolvimento.
 *
 * <p><b>O que isto é:</b> um atalho que emite o mesmo JWT do login real a partir de um e-mail, para
 * que o ciclo de trabalho não dependa de conta Microsoft, MFA e disponibilidade da identidade da CPS.
 * Trocar de papel (aluno, professor, admin) passa a custar um clique.
 *
 * <p><b>Por que o papel continua sendo decidido pelo domínio:</b> este serviço delega a
 * {@link UserProvisioningService}, o mesmo caminho do login Microsoft. Ele não aceita um papel do
 * cliente — o papel segue a regra de domínio dos dois jeitos, e a única conta ADMIN é a que o
 * {@code init/01-create-admin.sql} semeia no banco.
 *
 * <p><b>A porta que isto abre:</b> qualquer um que alcance este endpoint recebe um JWT válido para
 * o e-mail que digitar, inclusive o admin. Por isso ele só existe com o perfil {@code dev} <b>e</b>
 * com {@code app.security.dev-auth.enabled=true}, que não tem valor padrão — são dois interruptores
 * deliberados, e o padrão dos dois é desligado. Nunca habilite em um ambiente exposto.
 */
@Slf4j
@Service
@Profile("dev")
@ConditionalOnProperty(name = "app.security.dev-auth.enabled", havingValue = "true")
@RequiredArgsConstructor
public class DevAuthService {

    /**
     * Atalhos oferecidos na tela de login. Não são uma lista fechada: {@link #login(String)} aceita
     * qualquer e-mail, estes só evitam digitar. O e-mail do admin precisa ser o mesmo do seed
     * (`init/01-create-admin.sql`), senão a conta é criada como PROFESSOR pela regra de domínio e o
     * atalho não leva ao painel administrativo.
     */
    private static final List<DevAuthAccountResponse> CONTAS = List.of(
            new DevAuthAccountResponse("gabriel@aluno.cps.sp.gov.br", "Aluno"),
            new DevAuthAccountResponse("maria@cps.sp.gov.br", "Professor"),
            new DevAuthAccountResponse("admin@cps.sp.gov.br", "Admin")
    );

    private final UserProvisioningService userProvisioningService;
    private final JwtTokenProvider jwtTokenProvider;
    private final ResponseMapper responseMapper;

    public List<DevAuthAccountResponse> contas() {
        return CONTAS;
    }

    @Transactional
    public AuthResponse login(String email) {
        User user = userProvisioningService.provisionar(email, nomeDerivado(email), null);

        String token = jwtTokenProvider.generateToken(user);
        log.warn("Login de DESENVOLVIMENTO para {} (papel: {}). Não use este caminho em produção.",
                user.getEmail(), user.getRole().name());

        return responseMapper.toAuthResponse(
                token,
                jwtTokenProvider.getExpirationInSeconds(),
                user.getNome(),
                user.getEmail(),
                user.getFotoUrl(),
                user.getRole().name());
    }

    /**
     * Sem o Graph não há {@code displayName}; o prefixo do e-mail é o que há. Capitalizado só para
     * ficar legível na UI — o nome real é preenchido no primeiro login Microsoft.
     */
    private String nomeDerivado(String email) {
        String prefixo = email.trim().split("@")[0];
        if (prefixo.isEmpty()) {
            return "Usuário de desenvolvimento";
        }
        return prefixo.substring(0, 1).toUpperCase(Locale.ROOT) + prefixo.substring(1);
    }
}