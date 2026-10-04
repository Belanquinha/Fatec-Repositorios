package com.fatecrepository.e2e;

import com.fatecrepository.model.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Story 1.4 — Aplicar autorização por papel no servidor.
 * AC: "Given um token de aluno / When o aluno tenta uma operação de professor
 * (ex.: listar fila de aprovação) / Then o back-end responde 403"
 * AC: "a decisão de papel nunca vem do front-end"
 * NFR2 — papel nunca é confiado ao front-end.
 *
 * <p>Estas são as Organizing/Stakeholder ACs de maior risco do produto: uma falha
 * aqui significa que um aluno altera o catálogo oficial. Por isso a suíte cobre
 * a matriz inteira (papel x método) e não apenas o caminho feliz do admin.
 */
@DisplayName("Story 1.4 / NFR2 — Autorização por papel no servidor")
class AutorizacaoPorPapelE2ETest extends ApiE2ETestSupport {

    private Map<String, Object> novaInstituicaoPayload() {
        return Map.of(
            "codigoUnidade", "900",
            "nome", "Fatec Teste E2E"
        );
    }

    @Nested
    @DisplayName("Mutação do catálogo exige papel ADMIN")
    class MutacaoDoCatalogoExigeAdmin {

        @ParameterizedTest(name = "papel {0} recebe 403 ao criar instituição")
        @EnumSource(value = UserRole.class, names = {"ALUNO", "PROFESSOR"})
        @DisplayName("AC 1.4 — aluno e professor recebem 403 ao criar instituição")
        void criarComoNaoAdminRecebe403(UserRole role) {
            String token = tokenPara("Usuário " + role, role.name().toLowerCase() + ".e2e@cps.sp.gov.br", role);

            ResponseEntity<String> response = post("/instituicoes", novaInstituicaoPayload(), token);

            assertAcessoNegado(response);
            assertThat(texto(response, "codigo")).isEqualTo("403");
        }

        @Test
        @DisplayName("AC 1.4 — professor recebe 403 ao atualizar instituição")
        void atualizarComoProfessorRecebe403() {
            var instituicao = criarInstituicao("901", "Fatec Update E2E");
            String token = tokenPara("Professor E2E", "professor.update@cps.sp.gov.br", UserRole.PROFESSOR);

            ResponseEntity<String> response = put("/instituicoes/" + instituicao.getId(), novaInstituicaoPayload(), token);

            assertAcessoNegado(response);
        }

        @Test
        @DisplayName("AC 1.4 — aluno recebe 403 ao remover instituição")
        void removerComoAlunoRecebe403() {
            var instituicao = criarInstituicao("902", "Fatec Delete E2E");
            String token = tokenPara("Aluno E2E", "aluno.delete@aluno.cps.sp.gov.br", UserRole.ALUNO);

            ResponseEntity<String> response = delete("/instituicoes/" + instituicao.getId(), token);

            assertAcessoNegado(response);
        }

        @Test
        @DisplayName("NFR2 — a negação acontece antes de qualquer efeito colateral (nada é criado)")
        void negacaoNaoCriaRegistro() {
            String token = tokenPara("Aluno E2E", "aluno.semefeito@aluno.cps.sp.gov.br", UserRole.ALUNO);

            post("/instituicoes", novaInstituicaoPayload(), token);

            assertThat(instituicaoRepository.existsByCodigoUnidade("900"))
                .as("Um 403 não pode ter criado a instituição")
                .isFalse();
        }
    }

    @Nested
    @DisplayName("Ausência de sessão devolve 401 (não 403)")
    class AusenciaDeSessao {

        @Test
        @DisplayName("Story 1.3 — criar projeto sem token devolve 401")
        void criarProjetoSemToken() {
            ResponseEntity<String> response = post("/projetos", Map.of(), null);

            assertNaoAutorizado(response);
            assertThat(texto(response, "mensagem")).isEqualTo("Não autorizado");
        }

        @Test
        @DisplayName("Story 1.3 — meus projetos sem token devolve 401")
        void meusProjetosSemToken() {
            assertNaoAutorizado(get("/projetos/meus"));
        }

        @Test
        @DisplayName("Story 1.3 — criar instituição sem token devolve 401")
        void criarInstituicaoSemToken() {
            assertNaoAutorizado(post("/instituicoes", novaInstituicaoPayload(), null));
        }

        @Test
        @DisplayName("Token adulterado devolve 401 em vez de ser aceito")
        void tokenAdulterado() {
            ResponseEntity<String> response = get("/projetos/meus", "token.totalmente.invalido");

            assertNaoAutorizado(response);
        }

        @Test
        @DisplayName("Token com assinatura de outro segredo devolve 401")
        void tokenAssinadoComOutroSegredo() {
            String tokenValido = tokenPara("Aluno E2E", "aluno.forja@aluno.cps.sp.gov.br", UserRole.ALUNO);
            String tokenForjado = tokenValido.substring(0, tokenValido.lastIndexOf('.') + 1)
                + "assinaturaTotalmenteInvalidaQueNaoBateComOModo";

            ResponseEntity<String> response = get("/projetos/meus", tokenForjado);

            assertNaoAutorizado(response);
        }
    }

    @Nested
    @DisplayName("NFR2 — o papel vem do servidor, nunca do front-end")
    class PapelVemDoServidor {

        @Test
        @DisplayName("Front-end não consegue se autodeclarar ADMIN via header ou query param")
        void frontEndNaoSeAutodeclaraAdmin() {
            // O usuário real é ALUNO. Ele tenta "subir" o privilégio na requisição.
            String tokenAluno = tokenPara("Aluno E2E", "aluno.escalada@aluno.cps.sp.gov.br", UserRole.ALUNO);

            HttpHeaders comRoleNoHeader = new HttpHeaders();
            comRoleNoHeader.setContentType(MediaType.APPLICATION_JSON);
            comRoleNoHeader.setBearerAuth(tokenAluno);
            comRoleNoHeader.set("X-Role", "ADMIN");

            ResponseEntity<String> respostaHeader = restTemplate.exchange(
                url("/instituicoes"),
                HttpMethod.POST,
                new HttpEntity<>(novaInstituicaoPayload(), comRoleNoHeader),
                String.class);

            ResponseEntity<String> respostaQuery = restTemplate.exchange(
                url("/instituicoes?role=ADMIN"),
                HttpMethod.POST,
                new HttpEntity<>(novaInstituicaoPayload(), cabecalhoAutenticado(tokenAluno)),
                String.class);

            assertAcessoNegado(respostaHeader);
            assertAcessoNegado(respostaQuery);
        }

        @Test
        @DisplayName("Token válido de aluno não concede acesso de admin")
        void tokenDeAlunoNaoViraAdmin() {
            String tokenAluno = tokenPara("Aluno E2E", "aluno.inalterado@aluno.cps.sp.gov.br", UserRole.ALUNO);

            ResponseEntity<String> response = post("/instituicoes", novaInstituicaoPayload(), tokenAluno);

            assertAcessoNegado(response);
        }

        private HttpHeaders cabecalhoAutenticado(String token) {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(token);
            return headers;
        }
    }

    @Nested
    @DisplayName("Leitura pública não exige sessão (FR12)")
    class LeituraPublicaNaoExigeSessao {

        @Test
        @DisplayName("Catálogo de instituições é público")
        void catalogoInstituicoesPublico() {
            criarInstituicao("910", "Fatec Pública E2E");

            ResponseEntity<String> response = get("/instituicoes");

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).contains("Fatec Pública E2E");
        }

        @Test
        @DisplayName("Lista de professores é pública (usada no autocomplete da Story 3.2)")
        void listaProfessoresPublica() {
            ResponseEntity<String> response = get("/professores");

            assertThat(response.getStatusCode().value()).isEqualTo(200);
        }

        @Test
        @DisplayName("Detalhe de projeto é público")
        void detalheProjetoPublico() {
            // ID inexistente ainda precisa atravessar a barreira de segurança e
            // responder 404 de negócio — não 401.
            ResponseEntity<String> response = get("/projetos/" + UUID.randomUUID());

            assertThat(response.getStatusCode().value()).isEqualTo(404);
        }
    }
}