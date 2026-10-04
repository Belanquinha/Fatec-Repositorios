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

        // AD-3: assinatura, emissor, audience e tenant são conferidos aqui, antes de qualquer
        // chamada externa. Com o app registration multi-tenant a authority do front-end não é
        // mais a fronteira de confiança — esta é.
        tokenVerifier.verify(request.getAccessToken());

        MicrosoftGraphService.GraphUser graphUser = microsoftGraphService.getUserInfo(request.getAccessToken());
        String email = resolverEmail(graphUser);
        if (email == null) {
            throw new UnauthorizedException("Token Microsoft inválido ou dados do usuário não encontrados");
        }

        User user = userProvisioningService.provisionar(
                email,
                graphUser.getDisplayName(),
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
}