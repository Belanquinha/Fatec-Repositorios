package com.fatecrepository.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.fatecrepository.model.Gestor;
import com.fatecrepository.model.User;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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

    private Algorithm algorithm;
    private JWTVerifier verifier;

    @PostConstruct
    public void init() {
        this.algorithm = Algorithm.HMAC256(jwtSecret);
        this.verifier = JWT.require(algorithm)
            .withIssuer(jwtIssuer)
            .build();
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

    public String generateToken(Gestor gestor) {
        try {
            Instant now = Instant.now();
            Instant expiryDate = now.plus(tokenExpirationHours, ChronoUnit.HOURS);

            var tokenBuilder = JWT.create()
                .withIssuer(jwtIssuer)
                .withSubject(gestor.getEmail())
                .withClaim("userId", gestor.getId().toString())
                .withClaim("role", "GESTOR")
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(expiryDate));

            if (gestor.getInstituicao() != null && gestor.getInstituicao().getId() != null) {
                tokenBuilder.withClaim("instituicaoId", gestor.getInstituicao().getId().toString());
            }

            String token = tokenBuilder.sign(algorithm);
            log.info("Token gerado com sucesso para gestor: {}", gestor.getEmail());
            return token;
        } catch (Exception e) {
            log.error("Erro ao gerar token JWT para gestor: {}", gestor.getEmail(), e);
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

    public UUID extractInstituicaoId(String token) throws JWTVerificationException {
        String instituicaoId = verifyAndDecode(token).getClaim("instituicaoId").asString();
        return UUID.fromString(instituicaoId);
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
