package com.fatecrepository.e2e;

import com.fatecrepository.model.Instituicao;
import com.fatecrepository.model.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Casos-limite de contrato das últimas mudanças (não cobertos pelas suítes
 * {@code VinculoProfessorInstituicaoE2ETest} e {@code ProfessorAutocompleteLgpdE2ETest}).
 *
 * <p>Story 2.3 (AD-VP-1/AD-VP-3/AD-VP-5): fronteira exata do teto (4 passa),
 * semântica de substituição total do PUT, isolamento entre professores,
 * GET de primeiro acesso (200 []) e ordenação (nome, depois cidade).
 *
 * <p>Story 3.2 LGPD: corpos inválidos no PUT (400 sem persistir nada) e o
 * contrato do detalhe {@code GET /professores/{id}} (401/404 + id+nome+e-mail
 * só do selecionado, busca case-insensitive).
 */
@DisplayName("Últimas mudanças — casos-limite de contrato (Story 2.3 + Story 3.2 LGPD)")
class VinculoEAutocompleteContratoE2ETest extends ApiE2ETestSupport {

    private Map<String, Object> payload(List<UUID> ids) {
        return Map.of("instituicaoIds", ids.stream().map(UUID::toString).toList());
    }

    private Instituicao novaUnidade(String codigo, String nome) {
        return criarInstituicao(codigo, nome);
    }

    private List<String> nomes(ResponseEntity<String> response) {
        return jsonList(response).stream().map(m -> String.valueOf(m.get("nome"))).toList();
    }

    @Nested
    @DisplayName("Story 2.3 — fronteira, substituição, isolamento e ordenação")
    class FronteiraESemantica {

        @Test
        @DisplayName("PUT com exatamente 4 válidos → 200 (teto é 5+, não 4)")
        void quatroValidosPassa() {
            String token = tokenPara("Prof Quatro", "prof.quatro@cps.sp.gov.br", UserRole.PROFESSOR);
            List<UUID> quatro = List.of(
                novaUnidade("901", "Fatec Q1").getId(), novaUnidade("902", "Fatec Q2").getId(),
                novaUnidade("903", "Fatec Q3").getId(), novaUnidade("904", "Fatec Q4").getId());

            ResponseEntity<String> response = put("/professores/me/instituicoes", payload(quatro), token);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(jsonList(response)).hasSize(4);
            assertThat(jsonList(get("/professores/me/instituicoes", token))).hasSize(4);
        }

        @Test
        @DisplayName("PUT substitui o conjunto (save A,B depois B,C → GET só B,C)")
        void putSubstituiConjunto() {
            String token = tokenPara("Prof Troca", "prof.troca@cps.sp.gov.br", UserRole.PROFESSOR);
            var a = novaUnidade("905", "Fatec Troca A");
            var b = novaUnidade("906", "Fatec Troca B");
            var c = novaUnidade("907", "Fatec Troca C");

            assertThat(put("/professores/me/instituicoes",
                payload(List.of(a.getId(), b.getId())), token).getStatusCode().value()).isEqualTo(200);
            assertThat(put("/professores/me/instituicoes",
                payload(List.of(b.getId(), c.getId())), token).getStatusCode().value()).isEqualTo(200);

            assertThat(nomes(get("/professores/me/instituicoes", token)))
                .containsExactlyInAnyOrder("Fatec Troca B", "Fatec Troca C");
        }

        @Test
        @DisplayName("Vínculos são isolados por professor (A não vê os de B)")
        void isolamentoPorProfessor() {
            var i1 = novaUnidade("908", "Fatec Isolada A");
            var i2 = novaUnidade("909", "Fatec Isolada B");
            String profA = tokenPara("Prof A", "prof.a@cps.sp.gov.br", UserRole.PROFESSOR);
            String profB = tokenPara("Prof B", "prof.b@cps.sp.gov.br", UserRole.PROFESSOR);

            put("/professores/me/instituicoes", payload(List.of(i1.getId())), profA);

            assertThat(nomes(get("/professores/me/instituicoes", profB))).isEmpty();

            put("/professores/me/instituicoes", payload(List.of(i2.getId())), profB);

            assertThat(nomes(get("/professores/me/instituicoes", profA)))
                .containsExactly("Fatec Isolada A");
            assertThat(nomes(get("/professores/me/instituicoes", profB)))
                .containsExactly("Fatec Isolada B");
        }

        @Test
        @DisplayName("GET de primeiro acesso → 200 [] (zero é estado válido, não 404)")
        void primeiroAcessoRetornaVazio() {
            String token = tokenPara("Prof Novo", "prof.novo@cps.sp.gov.br", UserRole.PROFESSOR);

            ResponseEntity<String> response = get("/professores/me/instituicoes", token);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(jsonList(response)).isEmpty();
        }

        @Test
        @DisplayName("GET e PUT respondem ordenados por nome (depois cidade)")
        void ordenacaoPorNome() {
            String token = tokenPara("Prof Ordem", "prof.ordem@cps.sp.gov.br", UserRole.PROFESSOR);
            var zebra = novaUnidade("910", "Fatec Zebra");
            var alfa = novaUnidade("911", "Fatec Alfa");

            ResponseEntity<String> put = put("/professores/me/instituicoes",
                payload(List.of(zebra.getId(), alfa.getId())), token);

            assertThat(put.getStatusCode().value()).isEqualTo(200);
            assertThat(nomes(put)).containsExactly("Fatec Alfa", "Fatec Zebra");
            assertThat(nomes(get("/professores/me/instituicoes", token)))
                .containsExactly("Fatec Alfa", "Fatec Zebra");
        }
    }

    @Nested
    @DisplayName("Story 2.3 — corpo inválido no PUT → 400 sem persistir")
    class CorpoInvalido {

        @Test
        @DisplayName("JSON malformado → 400 com envelope e nada persistido")
        void jsonMalformadoDa400() {
            String token = tokenPara("Prof Mal", "prof.mal@cps.sp.gov.br", UserRole.PROFESSOR);

            ResponseEntity<String> response = put("/professores/me/instituicoes", "{invalido", token);

            assertThat(response.getStatusCode().value()).isEqualTo(400);
            assertThat(texto(response, "codigo")).isEqualTo("400");
            assertThat(texto(response, "mensagem")).isEqualTo("Corpo da requisição inválido ou malformado");
            assertThat(jsonList(get("/professores/me/instituicoes", token))).isEmpty();
        }

        @Test
        @DisplayName("Campo instituicaoIds ausente → 400 e nada persistido")
        void campoAusenteDa400() {
            String token = tokenPara("Prof SemCampo", "prof.semcampo@cps.sp.gov.br", UserRole.PROFESSOR);

            ResponseEntity<String> response =
                put("/professores/me/instituicoes", Map.of("outroCampo", List.of()), token);

            assertThat(response.getStatusCode().value()).isEqualTo(400);
            assertThat(texto(response, "codigo")).isEqualTo("400");
            assertThat(jsonList(get("/professores/me/instituicoes", token))).isEmpty();
        }

        @Test
        @DisplayName("UUID inválido (string) → 400 e nada persistido")
        void uuidInvalidoDa400() {
            String token = tokenPara("Prof Uuid", "prof.uuid@cps.sp.gov.br", UserRole.PROFESSOR);

            ResponseEntity<String> response = put("/professores/me/instituicoes",
                Map.of("instituicaoIds", List.of("nao-uuid")), token);

            assertThat(response.getStatusCode().value()).isEqualTo(400);
            assertThat(jsonList(get("/professores/me/instituicoes", token))).isEmpty();
        }

        @Test
        @DisplayName("Id nulo na lista → 400 e nada persistido")
        void idNuloDa400() {
            String token = tokenPara("Prof Nulo", "prof.nulo@cps.sp.gov.br", UserRole.PROFESSOR);
            Map<String, Object> corpo = new HashMap<>();
            corpo.put("instituicaoIds", Collections.singletonList(null));

            ResponseEntity<String> response = put("/professores/me/instituicoes", corpo, token);

            assertThat(response.getStatusCode().value()).isEqualTo(400);
            assertThat(jsonList(get("/professores/me/instituicoes", token))).isEmpty();
        }
    }

    @Nested
    @DisplayName("Story 3.2 — contrato do detalhe GET /professores/{id}")
    class DetalheDoSelecionado {

        @Test
        @DisplayName("Detalhe sem token → 401 (LGPD: e-mail só com sessão)")
        void detalheSemTokenDa401() {
            var prof = criarUsuario("Maria Silva", "maria.detalhe@cps.sp.gov.br", UserRole.PROFESSOR);

            assertNaoAutorizado(get("/professores/" + prof.getId()));
        }

        @Test
        @DisplayName("Detalhe com id inexistente → 404")
        void detalheInexistenteDa404() {
            String token = tokenPara("Aluno", "aluno.detalhe@aluno.cps.sp.gov.br", UserRole.ALUNO);

            ResponseEntity<String> response = get("/professores/" + UUID.randomUUID(), token);

            assertThat(response.getStatusCode().value()).isEqualTo(404);
        }

        @Test
        @DisplayName("Detalhe de usuário não-professor → 404 (não vaza e-mail de aluno)")
        void detalheDeNaoProfessorDa404() {
            String token = tokenPara("Aluno", "aluno.detalhe2@aluno.cps.sp.gov.br", UserRole.ALUNO);
            var aluno = criarUsuario("João Aluno", "joao@aluno.cps.sp.gov.br", UserRole.ALUNO);

            ResponseEntity<String> response = get("/professores/" + aluno.getId(), token);

            assertThat(response.getStatusCode().value()).isEqualTo(404);
            assertThat(response.getBody()).doesNotContain("joao@aluno.cps.sp.gov.br");
        }

        @Test
        @DisplayName("Detalhe do selecionado retorna id+nome+e-mail")
        void detalheRetornaIdNomeEmail() {
            String token = tokenPara("Aluno", "aluno.detalhe3@aluno.cps.sp.gov.br", UserRole.ALUNO);
            var prof = criarUsuario("Maria Silva", "maria.contrato@cps.sp.gov.br", UserRole.PROFESSOR);

            ResponseEntity<String> response = get("/professores/" + prof.getId(), token);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(texto(response, "id")).isEqualTo(prof.getId().toString());
            assertThat(texto(response, "nome")).isEqualTo("Maria Silva");
            assertThat(texto(response, "email")).isEqualTo("maria.contrato@cps.sp.gov.br");
        }

        @Test
        @DisplayName("Busca é case-insensitive (?q=MAR encontra Maria)")
        void buscaCaseInsensitive() {
            String token = tokenPara("Aluno", "aluno.detalhe4@aluno.cps.sp.gov.br", UserRole.ALUNO);
            criarUsuario("Maria Silva", "maria.case@cps.sp.gov.br", UserRole.PROFESSOR);

            List<Map<String, Object>> lista = jsonList(get("/professores?q=MAR", token));

            assertThat(lista).hasSize(1);
            assertThat(lista.get(0).get("nome")).isEqualTo("Maria Silva");
        }
    }
}
