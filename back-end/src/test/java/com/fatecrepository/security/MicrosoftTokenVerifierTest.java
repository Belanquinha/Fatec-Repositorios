package com.fatecrepository.security;

import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.JwkException;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.fatecrepository.exception.UnauthorizedException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercita a verificação com um par de chaves RSA real: o token é assinado de verdade e o
 * {@code JwkProvider} devolve a chave pública correspondente, então a checagem de assinatura roda
 * como em produção em vez de ser contornada por mock.
 *
 * <p>Cobre os dois formatos de token que o MSAL pode emitir, porque o par authority+audience é que
 * define o formato: sem {@code /v2.0} na authority o token sai v1 ({@code sts.windows.net}), e um
 * verificador fixo no par v2 rejeita um token v1 legítimo com "assinatura inválida".
 */
class MicrosoftTokenVerifierTest {

    private static final String TENANT_CPS = "eabe64c5-68f5-4a76-8301-9577a679e449";
    private static final String TENANT_EXTERNO = "11111111-2222-3333-4444-555555555555";
    private static final String AUD_AAD_GRAPH_V1 = "00000003-0000-0000-c000-000000000000";
    private static final String AUD_GRAPH_V2 = "00000003-0000-0cc0-000000000000";
    private static final String AUD_GRAPH_V2_URI = "https://graph.microsoft.com";
    private static final String KID = "chave-de-teste";

    private static final String ISS_V1 = "https://sts.windows.net/" + TENANT_CPS + "/";
    private static final String ISS_V2 = "https://login.microsoftonline.com/" + TENANT_CPS + "/v2.0";

    private static RSAPrivateKey chavePrivada;
    private static RSAPublicKey chavePublica;

    @BeforeAll
    static void gerarChaves() throws Exception {
        KeyPairGenerator gerador = KeyPairGenerator.getInstance("RSA");
        gerador.initialize(2048);
        KeyPair par = gerador.generateKeyPair();
        chavePrivada = (RSAPrivateKey) par.getPrivate();
        chavePublica = (RSAPublicKey) par.getPublic();
    }

    /**
     * Verificador com a chave pública do par gerado, para que a verificação de assinatura rode de
     * verdade mantendo o resto (emissor, audience, tenant) intacto.
     */
    private MicrosoftTokenVerifier verificador() {
        Jwk jwk = Jwk.fromValues(Map.of(
                "kty", "RSA",
                "kid", KID,
                "use", "sig",
                "alg", "RS256",
                "n", base64Url(chavePublica.getModulus()),
                "e", base64Url(chavePublica.getPublicExponent())
        ));
        JwkProvider provider = id -> {
            if (KID.equals(id)) {
                return jwk;
            }
            throw new JwkException("Chave desconhecida: " + id);
        };
        MicrosoftTokenVerifier v = new MicrosoftTokenVerifier(
                "https://login.microsoftonline.com/common/discovery/v2.0/keys");
        ReflectionTestUtils.setField(v, "jwkProvider", provider);
        ReflectionTestUtils.setField(v, "tenantId", TENANT_CPS);
        ReflectionTestUtils.setField(v, "clientId", "146c36f9-abf3-48b0-a533-5f462e5e4eed");
        return v;
    }

    private String base64Url(BigInteger valor) {
        byte[] bytes = valor.toByteArray();
        if (bytes.length > 1 && bytes[0] == 0) {
            byte[] semSinal = new byte[bytes.length - 1];
            System.arraycopy(bytes, 1, semSinal, 0, semSinal.length);
            bytes = semSinal;
        }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String token(String issuer, String audience, String tid) {
        return JWT.create()
                .withKeyId(KID)
                .withIssuer(issuer)
                .withAudience(audience)
                .withClaim("tid", tid)
                .withExpiresAt(new Date(System.currentTimeMillis() + 600_000))
                .sign(Algorithm.RSA256(chavePublica, chavePrivada));
    }

    private String tokenV1() {
        return token(ISS_V1, AUD_AAD_GRAPH_V1, TENANT_CPS);
    }

    private String tokenV2() {
        return token(ISS_V2, AUD_GRAPH_V2, TENANT_CPS);
    }

    private String idToken(String audience) {
        return JWT.create()
                .withKeyId(KID)
                .withIssuer(ISS_V2)
                .withAudience(audience)
                .withClaim("tid", TENANT_CPS)
                .withClaim("preferred_username", "aluno@cps.sp.gov.br")
                .withExpiresAt(new Date(System.currentTimeMillis() + 600_000))
                .sign(Algorithm.RSA256(chavePublica, chavePrivada));
    }

    @Test
    @DisplayName("Deve aceitar token v1 da CPS: é o formato emitido sem '/v2.0' na authority")
    void deveAceitarTokenV1DaCps() {
        assertDoesNotThrow(() -> verificador().verify(tokenV1()));
    }

    @Test
    @DisplayName("Deve aceitar token v2 da CPS: é o formato emitido com '/v2.0' na authority")
    void deveAceitarTokenV2DaCps() {
        assertDoesNotThrow(() -> verificador().verify(tokenV2()));
    }

    @Test
    @DisplayName("Deve aceitar o Graph pela URI, que é como o aud sai com escopo na forma extensa")
    void deveAceitarGraphPorUri() {
        // Regressão real: com o escopo pedido como
        // `https://graph.microsoft.com/User.Read`, o Microsoft devolve `aud` na forma de URI. A
        // lista de audiences aceitáveis só tinha os GUIDs, então um token válido era recusado com
        // "recurso não autorizado" — mensagem que não tem nada a ver com assinatura e só aparece
        // depois que o escopo deixa de ser ambíguo.
        assertDoesNotThrow(() -> verificador().verify(token(ISS_V1, AUD_GRAPH_V2_URI, TENANT_CPS)));
    }

    @Test
    @DisplayName("A forma URI do Graph não pode ser confundida com um id token da própria aplicação")
    void formaUriNaoAbrePortaParaIdToken() {
        // Aceitar a URI não pode enfraquecer a barreira do `aud`: o id token da app tem
        // `aud` = client ID e continua precisando ser recusado.
        assertThrows(UnauthorizedException.class,
                () -> verificador().verify(token(ISS_V2, "146c36f9-abf3-48b0-a533-5f462e5e4eed", TENANT_CPS)));
    }

    @Test
    @DisplayName("Deve aceitar token v1 com emissor sem a barra final")
    void deveAceitarEmissorSemBarraFinal() {
        String token = token("https://sts.windows.net/" + TENANT_CPS, AUD_AAD_GRAPH_V1, TENANT_CPS);

        assertDoesNotThrow(() -> verificador().verify(token));
    }

    @Test
    @DisplayName("Deve recusar token de outro tenant (AD-3)")
    void deveRecusarTokenDeTenantExterno() {
        UnauthorizedException erro = assertThrows(
                UnauthorizedException.class,
                () -> verificador().verify(token(ISS_V1, AUD_AAD_GRAPH_V1, TENANT_EXTERNO))
        );
        assertTrue(erro.getMessage().contains("Tenant inválido"));
    }

    @Test
    @DisplayName("Deve recusar token cujo emissor é de outro tenant, mesmo com tid da CPS")
    void deveRecusarEmissorDeTenantExterno() {
        String token = token(
                "https://sts.windows.net/" + TENANT_EXTERNO + "/",
                AUD_AAD_GRAPH_V1,
                TENANT_CPS);

        assertThrows(UnauthorizedException.class, () -> verificador().verify(token));
    }

    @Test
    @DisplayName("Deve recusar token sem claim tid: antes o código aceitava por omissão")
    void deveRecusarTokenSemTid() {
        String semTid = JWT.create()
                .withKeyId(KID)
                .withIssuer(ISS_V1)
                .withAudience(AUD_AAD_GRAPH_V1)
                .withExpiresAt(new Date(System.currentTimeMillis() + 600_000))
                .sign(Algorithm.RSA256(chavePublica, chavePrivada));

        assertThrows(UnauthorizedException.class, () -> verificador().verify(semTid));
    }

    @Test
    @DisplayName("Deve recusar id token da própria aplicação: aud = client ID, não Graph")
    void deveRecusarAudienceDaPropriaAplicacao() {
        String token = token(ISS_V1, "146c36f9-abf3-48b0-a533-5f462e5e4eed", TENANT_CPS);

        assertThrows(UnauthorizedException.class, () -> verificador().verify(token));
    }

    @Test
    @DisplayName("Deve aceitar idToken v2 com aud igual ao clientId")
    void deveAceitarIdTokenDoApp() {
        assertDoesNotThrow(() ->
                verificador().verifyIdToken(idToken("146c36f9-abf3-48b0-a533-5f462e5e4eed")));
    }

    @Test
    @DisplayName("Deve recusar idToken de outro app do mesmo tenant")
    void deveRecusarIdTokenDeOutroApp() {
        assertThrows(UnauthorizedException.class, () ->
                verificador().verifyIdToken(idToken("00000000-0000-0000-0000-000000000000")));
    }

    @Test
    @DisplayName("Deve recusar token expirado")
    void deveRecusarTokenExpirado() {
        String expirado = JWT.create()
                .withKeyId(KID)
                .withIssuer(ISS_V1)
                .withAudience(AUD_AAD_GRAPH_V1)
                .withClaim("tid", TENANT_CPS)
                .withExpiresAt(new Date(System.currentTimeMillis() - 60_000))
                .sign(Algorithm.RSA256(chavePublica, chavePrivada));

        assertThrows(UnauthorizedException.class, () -> verificador().verify(expirado));
    }

    @Test
    @DisplayName("Deve recusar token assinado por chave que não é a do Microsoft")
    void deveRecusarAssinaturaForjada() throws Exception {
        KeyPairGenerator gerador = KeyPairGenerator.getInstance("RSA");
        gerador.initialize(2048);
        KeyPair parAtaque = gerador.generateKeyPair();
        String forjado = JWT.create()
                .withKeyId(KID)
                .withIssuer(ISS_V1)
                .withAudience(AUD_AAD_GRAPH_V1)
                .withClaim("tid", TENANT_CPS)
                .withExpiresAt(new Date(System.currentTimeMillis() + 600_000))
                .sign(Algorithm.RSA256((RSAPublicKey) parAtaque.getPublic(),
                        (RSAPrivateKey) parAtaque.getPrivate()));

        assertThrows(UnauthorizedException.class, () -> verificador().verify(forjado));
    }

    @Test
    @DisplayName("Deve recusar lixo que não é JWT, sem lançar exceção não tratada")
    void deveRecusarTokenMalformado() {
        assertThrows(UnauthorizedException.class, () -> verificador().verify("nao-e-um-jwt"));
        assertThrows(UnauthorizedException.class, () -> verificador().verify(""));
    }

    @Test
    @DisplayName("Deve recusar token cujo kid não existe no JWKS")
    void deveRecusarKidDesconhecido() {
        String outroKid = JWT.create()
                .withKeyId("kid-que-nao-existe")
                .withIssuer(ISS_V1)
                .withAudience(AUD_AAD_GRAPH_V1)
                .withClaim("tid", TENANT_CPS)
                .withExpiresAt(new Date(System.currentTimeMillis() + 600_000))
                .sign(Algorithm.RSA256(chavePublica, chavePrivada));

        assertThrows(UnauthorizedException.class, () -> verificador().verify(outroKid));
    }

    @Test
    @DisplayName("Deve recusar token sem cabeçalho kid: não há como escolher a chave")
    void deveRecusarTokenSemKid() {
        String semKid = JWT.create()
                .withIssuer(ISS_V1)
                .withAudience(AUD_AAD_GRAPH_V1)
                .withClaim("tid", TENANT_CPS)
                .withExpiresAt(new Date(System.currentTimeMillis() + 600_000))
                .sign(Algorithm.RSA256(chavePublica, chavePrivada));

        assertThrows(UnauthorizedException.class, () -> verificador().verify(semKid));
    }
}