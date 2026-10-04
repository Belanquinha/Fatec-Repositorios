package com.fatecrepository.e2e;

import com.fatecrepository.model.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class SmokeE2ETest extends ApiE2ETestSupport {

    @Test
    @DisplayName("a aplicação sobe e responde HTTP no perfil de teste")
    void aplicacaoSobe() {
        ResponseEntity<String> response = get("/instituicoes");
        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    @DisplayName("o filtro JWT rejeita recurso protegido sem token")
    void recursoProtegidoSemToken() {
        assertNaoAutorizado(get("/projetos/meus"));
    }

    @Test
    @DisplayName("um token de sessão válido libera o recurso protegido")
    void recursoProtegidoComToken() {
        String token = tokenPara("Aluno Teste", "aluno.teste@aluno.cps.sp.gov.br", UserRole.ALUNO);
        ResponseEntity<String> response = get("/projetos/meus", token);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }
}