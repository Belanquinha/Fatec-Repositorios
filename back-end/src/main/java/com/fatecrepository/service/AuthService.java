package com.fatecrepository.service;

import com.fatecrepository.dto.request.MicrosoftLoginRequest;
import com.fatecrepository.dto.response.AuthResponse;
import com.fatecrepository.exception.UnauthorizedException;
import com.fatecrepository.mapper.ResponseMapper;
import com.fatecrepository.model.User;
import com.fatecrepository.security.JwtTokenProvider;
import com.fatecrepository.security.MicrosoftTokenVerifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AuthService {

    private final UserProvisioningService userProvisioningService;
    private final JwtTokenProvider jwtTokenProvider;
    private final ResponseMapper responseMapper;
    private final MicrosoftGraphService microsoftGraphService;
    private final MicrosoftTokenVerifier tokenVerifier;

    @Transactional
    public AuthResponse loginMicrosoft(MicrosoftLoginRequest request) {
        log.info("Tentativa de login via Microsoft");

        // O idToken é a prova de autenticação OIDC: aud=clientId, iss v2 do tenant, assinatura
        // pelas chaves do tenant. É ele que prende o login ao nosso app registration.
        // O access token do Graph NÃO é validado localmente de forma bloqueante: ele é
        // credencial para o Graph (aud=graph, chaves globais que o JWKS do tenant não publica),
        // então um v1 legítimo (iss sts.windows.net) sempre falharia aqui com "assinatura
        // inválida". Quem valida o access token é o próprio Graph, ao responder ao /me.
        if (request.getIdToken() == null || request.getIdToken().isBlank()) {
            throw new UnauthorizedException("Token Microsoft inválido: idToken ausente");
        }
        com.auth0.jwt.interfaces.DecodedJWT idClaims = tokenVerifier.verifyIdToken(request.getIdToken());

        // Best-effort: se o access token verificar contra o JWKS, ótimo; se não (caso v1),
        // segue para o Graph — que é o validador real desse artefato.
        try {
            tokenVerifier.verify(request.getAccessToken());
        } catch (Exception e) {
            log.debug("Access token não verificou localmente (segue para o Graph): {}", e.getMessage());
        }

        MicrosoftGraphService.GraphUser graphUser = microsoftGraphService.getUserInfo(request.getAccessToken());
        String email = resolverEmail(graphUser);
        String nome = graphUser != null ? graphUser.getDisplayName() : null;
        if (email == null) {
            email = emailDoIdToken(idClaims);
            nome = nomeDoIdToken(idClaims, nome);
        }
        if (email == null) {
            throw new UnauthorizedException("Token Microsoft inválido ou dados do usuário não encontrados");
        }
        if (nome == null || nome.isBlank()) {
            nome = parteLocal(email);
        }

        User user = userProvisioningService.provisionar(
                email,
                nome,
                microsoftGraphService.getPhotoUrl(request.getAccessToken())
        );

        String token = jwtTokenProvider.generateToken(user);
        log.info("Login Microsoft realizado com sucesso para: {} (papel: {})", user.getEmail(), user.getRole().name());

        return responseMapper.toAuthResponse(token, jwtTokenProvider.getExpirationInSeconds(), user.getNome(), user.getEmail(), user.getFotoUrl(), user.getRole().name());
    }

    /**
     * Contas da CPS podem não ter o campo {@code mail} preenchido; o UPN é o fallback canônico.
     * O valor é aparado e normalizado porque é ele que alimenta a classificação de papel, que
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

    /**
     * Fallback quando o Graph não responde (rede, permissão, token v1 sem escopo): o idToken
     * verificado já carrega e-mail verificado pela Microsoft. Ordem: mail > upn >
     * preferred_username > email.
     */
    private String emailDoIdToken(com.auth0.jwt.interfaces.DecodedJWT idClaims) {
        if (idClaims == null) {
            return null;
        }
        for (String claim : new String[]{"mail", "upn", "preferred_username", "email"}) {
            String valor = idClaims.getClaim(claim).asString();
            if (temValor(valor)) {
                return valor.trim().toLowerCase(Locale.ROOT);
            }
        }
        return null;
    }

    private String nomeDoIdToken(com.auth0.jwt.interfaces.DecodedJWT idClaims, String atual) {
        if (temValor(atual)) {
            return atual;
        }
        if (idClaims == null) {
            return null;
        }
        String nome = idClaims.getClaim("name").asString();
        return temValor(nome) ? nome : null;
    }

    /**
     * Último recurso para a coluna {@code nome} (NOT NULL): a parte local do e-mail.
     * Evita 500 por constraint quando Graph e idToken não trazem displayName.
     */
    private String parteLocal(String email) {
        int arroba = email.indexOf('@');
        return arroba > 0 ? email.substring(0, arroba) : email;
    }
}