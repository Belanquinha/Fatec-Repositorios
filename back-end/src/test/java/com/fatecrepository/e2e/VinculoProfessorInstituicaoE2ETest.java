package com.fatecrepository.e2e;

import com.fatecrepository.model.Instituicao;
import com.fatecrepository.model.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Story 2.3 — Vínculo professor↔instituição (GET/PUT /professores/me/instituicoes)")
class VinculoProfessorInstituicaoE2ETest extends ApiE2ETestSupport {

    private Map<String, Object> payload(List<UUID> ids) {
        return Map.of("instituicaoIds", ids.stream().map(UUID::toString).toList());
    }

    private Instituicao novaUnidade(String codigo, String nome) {
        return criarInstituicao(codigo, nome);
    }

    @Nested
    @DisplayName("Caminho feliz + idempotência")
    class CaminhoFeliz {

        @Test
        @DisplayName("PUT 2 válidos → 200; GET seguinte retorna o mesmo conjunto")
        void salvarDoisEReler() {
            String token = tokenPara("Prof E2E", "prof.vinculo@cps.sp.gov.br", UserRole.PROFESSOR);
            var i1 = novaUnidade("801", "Fatec Vinculo A");
            var i2 = novaUnidade("802", "Fatec Vinculo B");

            ResponseEntity<String> put = put("/professores/me/instituicoes", payload(List.of(i1.getId(), i2.getId())), token);
            assertThat(put.getStatusCode().value()).isEqualTo(200);

            ResponseEntity<String> get = get("/professores/me/instituicoes", token);
            assertThat(get.getStatusCode().value()).isEqualTo(200);
            assertThat(get.getBody()).contains("Fatec Vinculo A").contains("Fatec Vinculo B");
        }

        @Test
        @DisplayName("PUT [] → 200 []; GET → [] (limpa tudo, 1º acesso incluso)")
        void salvarZeroLimpa() {
            String token = tokenPara("Prof Zero", "prof.zero@cps.sp.gov.br", UserRole.PROFESSOR);
            var i1 = novaUnidade("803", "Fatec Vinculo C");
            put("/professores/me/instituicoes", payload(List.of(i1.getId())), token);

            ResponseEntity<String> limpa = put("/professores/me/instituicoes", Map.of("instituicaoIds", List.of()), token);
            assertThat(limpa.getStatusCode().value()).isEqualTo(200);
            assertThat(jsonList(limpa)).isEmpty();

            ResponseEntity<String> get = get("/professores/me/instituicoes", token);
            assertThat(jsonList(get)).isEmpty();
        }

        @Test
        @DisplayName("PUT repetido com mesmo corpo é idempotente (200, mesmo resultado)")
        void putIdempotente() {
            String token = tokenPara("Prof Idem", "prof.idem@cps.sp.gov.br", UserRole.PROFESSOR);
            var i1 = novaUnidade("804", "Fatec Vinculo D");

            var corpo = payload(List.of(i1.getId()));
            assertThat(put("/professores/me/instituicoes", corpo, token).getStatusCode().value()).isEqualTo(200);
            assertThat(put("/professores/me/instituicoes", corpo, token).getStatusCode().value()).isEqualTo(200);
            assertThat(jsonList(get("/professores/me/instituicoes", token))).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Validação servidor (teto, duplicado, inexistente)")
    class Validacao {

        @Test
        @DisplayName("PUT 5 ids → 400 na fronteira (@Size) e nada persistido; 409 segue no service")
        void cincoIdsDa409() {
            String token = tokenPara("Prof Teto", "prof.teto@cps.sp.gov.br", UserRole.PROFESSOR);
            List<UUID> cinco = List.of(
                novaUnidade("811", "F1").getId(), novaUnidade("812", "F2").getId(),
                novaUnidade("813", "F3").getId(), novaUnidade("814", "F4").getId(),
                novaUnidade("815", "F5").getId());

            ResponseEntity<String> response = put("/professores/me/instituicoes", payload(cinco), token);

            // A fronteira (@Size max=4 no DTO) barra antes do service: 400 com erro
            // por campo. O 409 "Máximo de 4 instituições por professor" continua
            // valendo no service (ProfessorVinculoServiceTest) para quem o chamar direto.
            assertThat(response.getStatusCode().value()).isEqualTo(400);
            assertThat(json(response).path("erros").has("instituicaoIds")).isTrue();
            assertThat(jsonList(get("/professores/me/instituicoes", token))).isEmpty();
        }

        @Test
        @DisplayName("PUT com duplicado → 400 e nada persistido")
        void duplicadoDa400() {
            String token = tokenPara("Prof Dup", "prof.dup@cps.sp.gov.br", UserRole.PROFESSOR);
            var i1 = novaUnidade("816", "Fatec Dup");

            ResponseEntity<String> response = put("/professores/me/instituicoes",
                payload(List.of(i1.getId(), i1.getId())), token);

            assertThat(response.getStatusCode().value()).isEqualTo(400);
            assertThat(jsonList(get("/professores/me/instituicoes", token))).isEmpty();
        }

        @Test
        @DisplayName("PUT com id inexistente → 404 e nada persistido")
        void inexistenteDa404() {
            String token = tokenPara("Prof Ghost", "prof.ghost@cps.sp.gov.br", UserRole.PROFESSOR);
            var i1 = novaUnidade("817", "Fatec Real");

            ResponseEntity<String> response = put("/professores/me/instituicoes",
                payload(List.of(i1.getId(), UUID.randomUUID())), token);

            assertThat(response.getStatusCode().value()).isEqualTo(404);
            assertThat(jsonList(get("/professores/me/instituicoes", token))).isEmpty();
        }
    }

    @Nested
    @DisplayName("Autorização (contrato AD-VP-4)")
    class Autorizacao {

        @Test
        @DisplayName("Aluno e admin recebem 403 no GET e no PUT")
        void naoProfessorRecebe403() {
            var i1 = novaUnidade("821", "Fatec Auth");
            String aluno = tokenPara("Aluno", "aluno.vinc@aluno.cps.sp.gov.br", UserRole.ALUNO);
            String admin = tokenPara("Admin", "admin.vinc@cps.sp.gov.br", UserRole.ADMIN);

            assertAcessoNegado(get("/professores/me/instituicoes", aluno));
            assertAcessoNegado(get("/professores/me/instituicoes", admin));
            assertAcessoNegado(put("/professores/me/instituicoes", payload(List.of(i1.getId())), aluno));
            assertAcessoNegado(put("/professores/me/instituicoes", payload(List.of(i1.getId())), admin));
        }

        @Test
        @DisplayName("Sem token → 401 no GET e no PUT")
        void semTokenDa401() {
            assertNaoAutorizado(get("/professores/me/instituicoes"));
            assertNaoAutorizado(put("/professores/me/instituicoes", Map.of("instituicaoIds", List.of()), null));
        }

        @Test
        @DisplayName("GET /professores exige sessão (LGPD; autocomplete Story 3.2 autenticado)")
        void listaProfessoresExigeSessao() {
            assertNaoAutorizado(get("/professores"));
        }
    }
}
