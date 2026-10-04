package com.fatecrepository.security;

import com.auth0.jwk.JwkException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifica o {@link JwkProviderAdapter} contra o endpoint real do Microsoft. Os demais testes do
 * verificador usam chaves locais para serem determinísticos; este existe porque o caminho do
 * endpoint JWKS já causou um 401 em produção — um erro de URL não pode passar pela suíte.
 *
 * <p>Requer rede, e falhar é o resultado correto quando o Microsoft não responde: silenciar esse
 * erro repetiria exatamente o bug que este teste cobre.
 */
class JwkProviderAdapterIT {

    private static final String JWKS_CPS =
            "https://login.microsoftonline.com/eabe64c5-68f5-4a76-8301-9577a679e449/discovery/v2.0/keys";

    /**
     * Um endpoint válido responde 200 e tem chaves; logo, o único {@code kid} possível é
     * desconhecido e o provider deve dizer isso explicitamente — depois de ter carregado o
     * conjunto. Se a URL estiver errada, o erro vem antes, sem nunca alcançar o "kid não
     * encontrado", e é essa diferença que distingue os dois defeitos.
     */
    @Test
    @DisplayName("Deve carregar o JWKS da CPS e rejeitar kid inexistente (endpoint válido)")
    void deveCarregarJwksDaCps() {
        JwkProviderAdapter provider = new JwkProviderAdapter(JWKS_CPS);

        JwkException erro = assertThrows(JwkException.class, () -> provider.get("kid-que-nao-existe"));

        assertTrue(
                erro.getMessage().contains("kid"),
                "esperava falha por kid desconhecido depois de carregar o conjunto, mas veio: "
                        + erro.getMessage());
    }

    @Test
    @DisplayName("Deve falhar ao buscar chave quando o endpoint está errado (404)")
    void deveFalharParaEndpointInvalido() {
        // '/v2.0/keys' sem '/discovery' é 404 — exatamente o erro cometido em produção.
        JwkProviderAdapter provider = new JwkProviderAdapter(
                "https://login.microsoftonline.com/common/v2.0/keys");

        assertThrows(JwkException.class, () -> provider.get("qualquer"));
    }

    @Test
    @DisplayName("Deve usar o cache: a segunda busca não pode reverter para kid inexistente como falha de rede")
    void deveReaproveitarCache() {
        JwkProviderAdapter provider = new JwkProviderAdapter(JWKS_CPS);

        assertThrows(JwkException.class, () -> provider.get("kid-1"));
        assertThrows(JwkException.class, () -> provider.get("kid-2"));
    }
}