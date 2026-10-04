package com.fatecrepository.security;

import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.fatecrepository.exception.UnauthorizedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Valida o access token do Microsoft Graph no back-end, antes de qualquer chamada externa.
 *
 * <p>Por que não basta decodificar: {@link JWT#decode} não checa assinatura, então um atacante
 * monta um JWT com o {@code tid} que quiser e o back-end o aceitaria como se fosse do tenant da
 * CPS. Com o app registration multi-tenant, a authority do front-end deixou de ser a fronteira de
 * confiança — a fronteira é esta.
 *
 * <p><b>Sobre audience e issuer:</b> o MSAL pode devolver duas formas de token, e elas não são
 * equivalentes. Quando a authority do front-end não traz o sufixo {@code /v2.0}, o MSAL usa o
 * endpoint v1 e o token sai com {@code iss} em {@code https://sts.windows.net/{tid}/} e
 * {@code aud} do Azure AD Graph ({@code 00000003-0000-0000-c000-000000000000}). Com
 * {@code /v2.0}, o token é v2: {@code iss} em {@code login.microsoftonline.com/{tid}/v2.0} e
 * {@code aud} do Microsoft Graph ({@code 00000003-0000-0cc0-000000000000}).
 *
 * <p>Verificar contra um par único (audience, issuer) fixos rejeita o token do outro formato com
 * "assinatura inválida", que é um diagnóstico enganoso — a assinatura está válida, só o formato
 * é outro. Por isso o verificador descobre o formato pelo próprio token e valida o emissor contra
 * o tenant de forma estrita, independentemente da versão.
 *
 * <p>As chaves públicas vêm do JWKS do tenant e ficam em cache; a roção de chaves da Microsoft é
 * absorvida pelo cache, sem nova chamada por request.
 */
@Slf4j
@Component
public class MicrosoftTokenVerifier {

    /**
     * Application ID do Azure AD Graph, usado nos tokens v1.
     */
    private static final String AUDIENCE_AAD_GRAPH_V1 = "00000003-0000-0000-c000-000000000000";

    /**
     * Application ID do Microsoft Graph, usado nos tokens v2.
     */
    private static final String AUDIENCE_GRAPH_V2 = "00000003-0000-0cc0-000000000000";

    /**
     * Prefixo do emissor dos tokens v1. O tenant vem logo depois.
     */
    private static final String ISSUER_PREFIX_V1 = "https://sts.windows.net/";

    /**
     * Prefixo do emissor dos tokens v2. O tenant vem logo depois, com o sufixo '/v2.0'.
     */
    private static final String ISSUER_PREFIX_V2 = "https://login.microsoftonline.com/";

    private static final String ISSUER_SUFFIX_V2 = "/v2.0";

    /**
     * Audience aceita para token de chamada do Graph. Um id token da própria aplicação
     * ({@code aud} = client ID) é criptograficamente válido e passaria numa checagem de assinatura
     * + tenant sozinha; o {@code aud} é o que impede esse reuso.
     */
    private static final Set<String> AUDIENCES_ACEITAS =
            Set.of(AUDIENCE_AAD_GRAPH_V1, AUDIENCE_GRAPH_V2);

    private final JwkProvider jwkProvider;

    @Value("${app.security.msal.tenant-id}")
    private String tenantId;

    public MicrosoftTokenVerifier(
            @Value("${app.security.msal.jwks-uri}") String jwksUri) {
        this.jwkProvider = new JwkProviderAdapter(jwksUri);
    }

    /**
     * Verifica assinatura, emissor, audience e tenant. Falha fechada em toda uncertainty: um token
     * que não dá para verificar é recusado, nunca aceito por omissão.
     */
    public void verify(String accessToken) {
        DecodedJWT decoded = decodificar(accessToken);

        // Só metadados: o token em si nunca é logado nem gravado. Um access token
        // do Microsoft em disco vale um login válido para quem o leia.
        log.info("Token recebido: aud={} iss={} tid={} kid={} alg={} len={}",
                decoded.getAudience(), decoded.getIssuer(), decoded.getClaim("tid").asString(),
                decoded.getKeyId(), decoded.getAlgorithm(), accessToken.length());

        if (decoded.getKeyId() == null || decoded.getKeyId().isBlank()) {
            throw new UnauthorizedException("Token Microsoft inválido: header sem 'kid'");
        }

        validarIssuer(decoded);
        validarAudience(decoded);

        RSAPublicKey chave = chavePublica(decoded.getKeyId());
        JWTVerifier verificacao = JWT.require(Algorithm.RSA256(chave, null)).build();

        try {
            verificacao.verify(accessToken);
        } catch (JWTVerificationException e) {
            log.warn("Login recusado: token Microsoft não passou na verificação ({})", e.getMessage());
            throw new UnauthorizedException("Token Microsoft inválido");
        }

        validarTenant(decoded);
    }

    /**
     * O emissor tem que apontar para o tenant da CPS, na grafia da versão que emitiu o token.
     * Checar o emissor é o que amarra a assinatura ao tenant: a chave vem do JWKS do tenant, mas
     * é o {@code iss} que declara de qual diretório ela foi usada.
     */
    private void validarIssuer(DecodedJWT decoded) {
        String issuer = decoded.getIssuer();

        if (issuer == null || issuer.isBlank()) {
            throw new UnauthorizedException("Token Microsoft inválido: sem claim 'iss'");
        }

        // v1: https://sts.windows.net/{tid}/     v2: https://login.microsoftonline.com/{tid}/v2.0
        // A barra final do v1 é opcional na prática; comparar os dois formatos normalizados
        // evita recusar por grafia.
        String issuerNormalizado = normalizar(issuer);

        String esperadoV1 = normalizar(ISSUER_PREFIX_V1 + tenantId + "/");
        String esperadoV2 = normalizar(ISSUER_PREFIX_V2 + tenantId + ISSUER_SUFFIX_V2);

        if (!issuerNormalizado.equalsIgnoreCase(esperadoV1)
                && !issuerNormalizado.equalsIgnoreCase(esperadoV2)) {
            log.warn("Login recusado: emissor fora do tenant autorizado (iss={})", issuer);
            throw new UnauthorizedException("Token Microsoft inválido: emissor não autorizado");
        }
    }

    private String normalizar(String issuer) {
        String normalizado = issuer.trim();
        while (normalizado.endsWith("/")) {
            normalizado = normalizado.substring(0, normalizado.length() - 1);
        }
        return normalizado;
    }

    private void validarAudience(DecodedJWT decoded) {
        List<String> audiences = decoded.getAudience();

        if (audiences == null || audiences.isEmpty()) {
            throw new UnauthorizedException("Token Microsoft inválido: sem claim 'aud'");
        }

        boolean algumaAceita = audiences.stream()
                .anyMatch(aud -> aud != null && AUDIENCES_ACEITAS.contains(aud.toLowerCase(Locale.ROOT)));

        if (!algumaAceita) {
            log.warn("Login recusado: audience não é a de um token de chamada do Graph (aud={})", audiences);
            throw new UnauthorizedException("Token Microsoft inválido: recurso não autorizado");
        }
    }

    /**
     * AD-3: o tenant é o diretório, não o domínio do e-mail. O check é estrito e a ausência do
     * claim é recusada — antes o código aceitava token sem {@code tid} e seguia, o que só era
     * seguro porque o app era single-tenant.
     */
    private void validarTenant(DecodedJWT decoded) {
        String tid = decoded.getClaim("tid").asString();

        if (tid == null || tid.isBlank()) {
            log.warn("Login recusado: token Microsoft sem claim 'tid'");
            throw new UnauthorizedException("Tenant inválido: o token não informa o tenant de origem");
        }

        if (!tid.equalsIgnoreCase(tenantId)) {
            log.warn("Login recusado: token fora do tenant autorizado (tid={})", tid);
            throw new UnauthorizedException("Tenant inválido: o token não pertence ao tenant da CPS");
        }
    }

    private DecodedJWT decodificar(String accessToken) {
        try {
            return JWT.decode(accessToken);
        } catch (JWTVerificationException | IllegalArgumentException e) {
            throw new UnauthorizedException("Token Microsoft inválido");
        }
    }

    private RSAPublicKey chavePublica(String kid) {
        try {
            Jwk jwk = jwkProvider.get(kid);
            Object chave = jwk.getPublicKey();
            if (!(chave instanceof RSAPublicKey rsa)) {
                throw new UnauthorizedException("Token Microsoft inválido: chave pública inesperada");
            }
            return Objects.requireNonNull(rsa);
        } catch (UnauthorizedException e) {
            throw e;
        } catch (Exception e) {
            // Inclui JwkException e RateLimitedJwkProviderException: indisponibilidade do JWKS
            // vira 401, não um bypass silencioso.
            log.warn("Login recusado: não foi possível obter a chave pública do Microsoft ({})", e.getMessage());
            throw new UnauthorizedException("Token Microsoft inválido");
        }
    }
}