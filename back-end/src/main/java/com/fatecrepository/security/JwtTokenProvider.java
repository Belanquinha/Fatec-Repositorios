package com.fatecrepository.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.fatecrepository.model.User;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${app.security.jwt.secret}")
    private String jwtSecret;

    @Value("${app.security.jwt.issuer:fatec-repository-api}")
    private String jwtIssuer;

    @Value("${app.security.jwt.expiration-hours:24}")
    private long tokenExpirationHours;

    /**
     * HS256 usa a chave como segredo HMAC. A força do esquema é a força da chave: o HMAC-SHA256
     * só oferece 256 bits de segurança quando a chave tem esse tamanho. O {@link Algorithm} do
     * java-jwt aceita qualquer string, inclusive {@code "a"} — o que não dá erro, dá uma chave
     * que se quebra por força bruta. Como este é o segredo que assina a sessão de admin, a
     * validação é na subida do contexto: um deploy mal configurado falha em vez de rodar.
     */
    private static final int TAMANHO_MINIMO_SEGREDO = 32;

    private Algorithm algorithm;
    private JWTVerifier verifier;

    @PostConstruct
    public void init() {
        this.algorithm = Algorithm.HMAC256(validarSegredo(jwtSecret));
        this.verifier = JWT.require(algorithm)
            .withIssuer(jwtIssuer)
            .build();
    }

    private String validarSegredo(String segredo) {
        if (segredo == null || segredo.isBlank()) {
            throw new IllegalStateException(
                    "app.security.jwt.secret ausente: o segredo que assina o JWT é obrigatório.");
        }

        int tamanhoEmBytes = segredo.getBytes(StandardCharsets.UTF_8).length;
        if (tamanhoEmBytes < TAMANHO_MINIMO_SEGREDO) {
            throw new IllegalStateException(String.format(
                    "app.security.jwt.secret tem %d bytes; HMAC-SHA256 exige ao menos %d. "
                    + "Gere um valor aleatório, por exemplo: openssl rand -base64 48",
                    tamanhoEmBytes, TAMANHO_MINIMO_SEGREDO));
        }

        return segredo;
    }

    public String generateToken(User user) {
        try {
            Instant now = Instant.now();
            Instant expiryDate = now.plus(tokenExpirationHours, ChronoUnit.HOURS);

            String token = JWT.create()
                .withIssuer(jwtIssuer)
                .withSubject(user.getEmail())
                .withClaim("userId", user.getId().toString())
                .withClaim("role", user.getRole().name())
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(expiryDate))
                .sign(algorithm);

            log.info("Token gerado com sucesso para usuário: {}", user.getEmail());
            return token;
        } catch (Exception e) {
            log.error("Erro ao gerar token JWT para usuário: {}", user.getEmail(), e);
            throw new RuntimeException("Erro ao gerar token JWT", e);
        }
    }

    public String extractEmail(String token) throws JWTVerificationException {
        return verifyAndDecode(token).getSubject();
    }

    public UUID extractUserId(String token) throws JWTVerificationException {
        String userId = verifyAndDecode(token).getClaim("userId").asString();
        return UUID.fromString(userId);
    }

    public String extractRole(String token) throws JWTVerificationException {
        return verifyAndDecode(token).getClaim("role").asString();
    }

    public long getExpirationInSeconds() {
        return Duration.ofHours(tokenExpirationHours).toSeconds();
    }

    public boolean isTokenValid(String token) {
        try {
            verifyAndDecode(token);
            log.debug("Token válido");
            return true;
        } catch (JWTVerificationException e) {
            log.warn("Token inválido: {}", e.getMessage());
            return false;
        }
    }

    private DecodedJWT verifyAndDecode(String token) throws JWTVerificationException {
        return verifier.verify(token);
    }
}