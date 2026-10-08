package com.fatecrepository.e2e;

import com.fatecrepository.model.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LGPD — GET /professores não pode vazar e-mails em massa sem sessão.
 *
 * <p>RED: este teste falha contra o comportamento atual (permitAll + dump
 * completo com e-mail). GREEN exige: 401 sem token, busca autenticada com
 * {@code ?q} (mín. 2 chars), teto de 10, lista só com id+nome, e-mail só no
 * detalhe {@code /professores/{id}}.
 */
@DisplayName("LGPD — autocomplete de professor sem dump de e-mails (Story 3.2)")
class ProfessorAutocompleteLgpdE2ETest extends ApiE2ETestSupport {

    @Nested
    @DisplayName("Controle de acesso")
    class ControleDeAcesso {

        @Test
        @DisplayName("Sem token → 401 (não mais 200 público)")
        void semTokenDa401() {
            assertNaoAutorizado(get("/professores"));
        }

        @Test
        @DisplayName("Com token de aluno → 200 (postagem Story 3.2)")
        void comTokenDeAlunoDa200() {
            String token = tokenPara("Aluno", "aluno@aluno.cps.sp.gov.br", UserRole.ALUNO);
            assertThat(get("/professores?q=ma", token).getStatusCode().value()).isEqualTo(200);
        }
    }

    @Nested
    @DisplayName("Minimização (art. 6º LGPD)")
    class Minimizacao {

        @Test
        @DisplayName("Sem ?q ou q curto (<2) → [] (sem dump completo)")
        void semQueryOuCurtaRetornaVazio() {
            String token = tokenPara("Aluno", "aluno2@aluno.cps.sp.gov.br", UserRole.ALUNO);
            criarUsuario("Maria Silva", "maria@cps.sp.gov.br", UserRole.PROFESSOR);

            assertThat(jsonList(get("/professores", token))).isEmpty();
            assertThat(jsonList(get("/professores?q=m", token))).isEmpty();
        }

        @Test
        @DisplayName("Lista expõe só id+nome — nunca e-mail em massa")
        void listaNaoExpoeEmail() {
            String token = tokenPara("Aluno", "aluno3@aluno.cps.sp.gov.br", UserRole.ALUNO);
            criarUsuario("Maria Silva", "maria@cps.sp.gov.br", UserRole.PROFESSOR);

            ResponseEntity<String> response = get("/professores?q=mar", token);
            assertThat(response.getStatusCode().value()).isEqualTo(200);
            List<Map<String, Object>> lista = jsonList(response);
            assertThat(lista).hasSize(1);
            assertThat(lista.get(0)).containsKeys("id", "nome");
            assertThat(lista.get(0)).doesNotContainKeys("email");
            assertThat(response.getBody()).doesNotContain("maria@cps.sp.gov.br");
        }

        @Test
        @DisplayName("Teto de 10 resultados por busca")
        void tetoDeDez() {
            String token = tokenPara("Aluno", "aluno4@aluno.cps.sp.gov.br", UserRole.ALUNO);
            for (int i = 0; i < 12; i++) {
                criarUsuario("Prof Teste " + i, "profteste" + i + "@cps.sp.gov.br", UserRole.PROFESSOR);
            }
            List<Map<String, Object>> lista = jsonList(get("/professores?q=prof%20teste", token));
            assertThat(lista.size()).isLessThanOrEqualTo(10);
        }

        @Test
        @DisplayName("Detalhe /professores/{id} autenticado retorna e-mail do selecionado")
        void detalheRetornaEmail() {
            String token = tokenPara("Aluno", "aluno5@aluno.cps.sp.gov.br", UserRole.ALUNO);
            var prof = criarUsuario("Maria Silva", "maria@cps.sp.gov.br", UserRole.PROFESSOR);

            ResponseEntity<String> response = get("/professores/" + prof.getId(), token);
            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).contains("maria@cps.sp.gov.br");
        }
    }
}
