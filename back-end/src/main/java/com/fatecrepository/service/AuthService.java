package com.fatecrepository.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.fatecrepository.dto.request.MicrosoftLoginRequest;
import com.fatecrepository.dto.response.AuthResponse;
import com.fatecrepository.exception.UnauthorizedException;
import com.fatecrepository.mapper.ResponseMapper;
import com.fatecrepository.model.User;
import com.fatecrepository.model.UserRole;
import com.fatecrepository.repository.UserRepository;
import com.fatecrepository.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AuthService {

    private static final String DOMINIO_ALUNO = "@aluno.cps.sp.gov.br";
    private static final String DOMINIO_PROFESSOR = "@cps.sp.gov.br";

    @Value("${app.security.msal.tenant-id}")
    private String tenantId;

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final ResponseMapper responseMapper;
    private final MicrosoftGraphService microsoftGraphService;

    @Transactional
    public AuthResponse loginMicrosoft(MicrosoftLoginRequest request) {
        log.info("Tentativa de login via Microsoft");

        validarTenantDaCps(request.getAccessToken());

        MicrosoftGraphService.GraphUser graphUser = microsoftGraphService.getUserInfo(request.getAccessToken());
        String email = resolverEmail(graphUser);
        if (email == null) {
            throw new UnauthorizedException("Token Microsoft inválido ou dados do usuário não encontrados");
        }

        String nome = graphUser.getDisplayName();
        String fotoUrl = microsoftGraphService.getPhotoUrl(request.getAccessToken());

        User user = userRepository.findByEmail(email).orElseGet(() -> {
            log.info("Criando novo usuário Microsoft: {}", email);
            User novoUser = new User();
            novoUser.setNome(nome);
            novoUser.setEmail(email);
            novoUser.setRole(classificarPapel(email));
            novoUser.setFotoUrl(fotoUrl);
            novoUser.setCriadoEm(LocalDateTime.now());
            novoUser.setAtualizadoEm(LocalDateTime.now());
            return userRepository.save(novoUser);
        });

        if (user.getFotoUrl() == null && fotoUrl != null) {
            user.setFotoUrl(fotoUrl);
            user.setAtualizadoEm(LocalDateTime.now());
            userRepository.save(user);
        }

        String token = jwtTokenProvider.generateToken(user);
        log.info("Login Microsoft realizado com sucesso para: {} (papel: {})", email, user.getRole().name());

        return responseMapper.toAuthResponse(token, jwtTokenProvider.getExpirationInSeconds(), user.getNome(), user.getEmail(), user.getFotoUrl(), user.getRole().name());
    }

    /**
     * AD-3: o MSAL e o back-end são restritos ao tenant único da CPS. A checagem acontece
     * antes de qualquer chamada ao Graph, decodificando o token sem verificá-lo — o token já
     * foi aceito pelo app registration single-tenant, então isto é defesa em profundidade.
     */
    private void validarTenantDaCps(String accessToken) {
        String tid = extrairTenant(accessToken);
        if (tid == null) {
            return;
        }
        if (!tid.equalsIgnoreCase(tenantId)) {
            log.warn("Login recusado: token fora do tenant autorizado (tid={})", tid);
            throw new UnauthorizedException("Tenant inválido: o token não pertence ao tenant da CPS");
        }
    }

    private String extrairTenant(String accessToken) {
        try {
            return JWT.decode(accessToken).getClaim("tid").asString();
        } catch (JWTDecodeException e) {
            log.warn("Não foi possível decodificar o token Microsoft para checar o tenant");
            return null;
        }
    }

    /**
     * Contas da CPS podem não ter o campo {@code mail} preenchido; o UPN é o fallback canônico.
     * O valor é aparado e normalizado porque é ele que alimenta {@code classificarPapel}, que
     * compara por {@code endsWith} — e a coluna de e-mail, que é única.
     */
    private String resolverEmail(MicrosoftGraphService.GraphUser graphUser) {
        if (graphUser == null) {
            return null;
        }
        if (temValor(graphUser.getMail())) {
            return graphUser.getMail().trim().toLowerCase(Locale.ROOT);
        }
        if (temValor(graphUser.getUserPrincipalName())) {
            return graphUser.getUserPrincipalName().trim().toLowerCase(Locale.ROOT);
        }
        return null;
    }

    private boolean temValor(String valor) {
        return valor != null && !valor.isBlank();
    }

    private UserRole classificarPapel(String email) {
        if (email.endsWith(DOMINIO_ALUNO)) {
            return UserRole.ALUNO;
        }
        if (email.endsWith(DOMINIO_PROFESSOR)) {
            return UserRole.PROFESSOR;
        }
        return UserRole.ALUNO;
    }
}