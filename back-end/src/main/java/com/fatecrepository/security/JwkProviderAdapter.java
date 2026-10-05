package com.fatecrepository.security;

import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkException;
import com.auth0.jwk.JwkProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Busca as chaves públicas do Microsoft no endpoint JWKS do tenant.
 *
 * <p>Não usa o {@code JwkProviderBuilder} da biblioteca {@code jwks-rsa}: ele sempre anexa
 * {@code /.well-known/jwks.json} à URL configurada, caminho que o Microsoft não expõe — o
 * endpoint real é {@code /{tenant}/v2.0/keys}. Como o cabeçalho {@code kid} do token só é conhecido
 * na hora da verificação, o conjunto inteiro de chaves é carregado e mantido em cache.
 *
 * <p>O cache existe para não bater na rede a cada login; a Microsoft rotaciona as chaves com
 * pouca frequência e o TTL de uma hora absorve a rotação. Se o {@code kid} não estiver no cache, o
 * conjunto é recarregado uma vez — é assim que uma chave nova passa a valer sem reinício.
 */
@Slf4j
final class JwkProviderAdapter implements JwkProvider {

    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private static final Duration TTL_CACHE = Duration.ofHours(1);

    private final URI endpoint;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    private volatile Map<String, Jwk> cache = Map.of();
    private volatile Instant expiresAt = Instant.EPOCH;

    JwkProviderAdapter(String jwksUri) {
        this.endpoint = URI.create(jwksUri);
        this.httpClient = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public Jwk get(String kid) throws JwkException {
        if (cacheVazioOuExpirado()) {
            carregar();
        }

        Jwk jwk = cache.get(kid);
        if (jwk != null) {
            return jwk;
        }

        // 'kid' desconhecido com cache ainda válido significa rotação de chave: recarrega uma vez.
        carregar();
        jwk = cache.get(kid);
        if (jwk == null) {
            throw new JwkException("Nenhuma chave pública correspondente ao kid informado");
        }
        return jwk;
    }

    /**
     * Todas as chaves do conjunto, para o verificador poder tentar cada uma.
     *
     * <p>Existe para o caso de o {@code kid} do token apontar para uma chave que não assina o
     * token — o que acontece quando o token vem de um endpoint v1 e o JWKS configurado é o v2, ou
     * durante uma rotação. Sem isso, a única saída é recusar o login.
     */
    List<Jwk> todas() throws JwkException {
        if (cacheVazioOuExpirado()) {
            carregar();
        }
        return List.copyOf(cache.values());
    }

    private boolean cacheVazioOuExpirado() {
        return cache.isEmpty() || Instant.now().isAfter(expiresAt);
    }

    private synchronized void carregar() throws JwkException {
        if (!cacheVazioOuExpirado()) {
            return;
        }
        try {
            HttpRequest request = HttpRequest.newBuilder(endpoint)
                    .header("Accept", "application/json")
                    .timeout(TIMEOUT)
                    .GET()
                    .build();
            HttpResponse<String> resposta = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (resposta.statusCode() != 200) {
                throw new JwkException("JWKS respondeu status " + resposta.statusCode());
            }

            JsonNode raiz = objectMapper.readTree(resposta.body()).get("keys");
            if (raiz == null || !raiz.isArray() || raiz.isEmpty()) {
                throw new JwkException("JWKS sem chaves utilizáveis");
            }

            Map<String, Jwk> carregadas = new ConcurrentHashMap<>();
            for (JsonNode no : raiz) {
                try {
                    Jwk jwk = Jwk.fromValues(
                            objectMapper.convertValue(no, Map.class));
                    if (jwk.getId() != null) {
                        carregadas.put(jwk.getId(), jwk);
                    }
                } catch (Exception e) {
                    // Uma chave malformada não pode invalidar o conjunto inteiro.
                    log.debug("Ignorando chave JWKS ilegível: {}", e.getMessage());
                }
            }

            if (carregadas.isEmpty()) {
                throw new JwkException("Nenhuma chave RSA pôde ser lida do JWKS");
            }

            this.cache = Map.copyOf(carregadas);
            this.expiresAt = Instant.now().plus(TTL_CACHE);
            log.debug("JWKS carregado com {} chaves", carregadas.size());
        } catch (JwkException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new JwkException("Interrompido ao buscar o JWKS", e);
        } catch (Exception e) {
            throw new JwkException("Falha ao buscar o JWKS: " + e.getMessage(), e);
        }
    }
}