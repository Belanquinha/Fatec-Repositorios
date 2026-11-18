package com.fatecrepository.e2e;

import com.fatecrepository.model.User;
import com.fatecrepository.model.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Epic 4 — Fila de pendentes e avaliação (aprovar/rejeitar).
 *
 * <p>Regra vigente: somente o professor indicado como responsável na submissão
 * ({@code emailProfessorResponsavel}) pode avaliar — sem exigir vínculo com a
 * instituição (o vínculo é auto-declarado; o front exibe aviso para revisar a
 * instituição quando não vinculado). Qualquer outro professor devolve 403.
 * Re-avaliar devolve 409; rejeitar sem motivo devolve 400.
 */
@DisplayName("Epic 4 — Fila de pendentes e avaliação pelo professor")
class AvaliacaoProjetoE2ETest extends ApiE2ETestSupport {

    private User aluno;
    private String tokenAluno;
    private User responsavel;
    private String tokenResponsavel;
    private com.fatecrepository.model.Instituicao instituicao;
    private java.util.UUID cursoId;

    @BeforeEach
    void prepararCenario() {
        aluno = criarUsuario("Aluno Avaliacao", "aluno.avaliacao@aluno.cps.sp.gov.br", UserRole.ALUNO);
        tokenAluno = tokenDe(aluno);
        responsavel = criarUsuario("Prof Responsavel", "prof.responsavel@cps.sp.gov.br", UserRole.PROFESSOR);
        tokenResponsavel = tokenDe(responsavel);
        instituicao = criarInstituicao("701", "Fatec Avaliacao");
        cursoId = criarCursoVinculado(instituicao, "Análise e Desenvolvimento de Sistemas").getId();
        // Responsável vinculado à instituição do projeto (metade da interseção).
        put("/professores/me/instituicoes",
            Map.of("instituicaoIds", List.of(instituicao.getId().toString())), tokenResponsavel);
    }

    private Map<String, Object> payloadProjeto(String emailResponsavel) {
        Map<String, Object> body = new HashMap<>();
        body.put("titulo", "Projeto da Fila E2E");
        body.put("descricaoCurta", "Descricao para a fila de avaliacao");
        body.put("conteudoEditorJs", "{\"blocks\":[]}");
        body.put("instituicaoId", instituicao.getId().toString());
        body.put("cursoId", cursoId.toString());
        body.put("emailProfessorResponsavel", emailResponsavel);
        body.put("integrantes", List.of(Map.of("nome", "Ana Integrante")));
        return body;
    }

    private String submeterParaResponsavel() {
        ResponseEntity<String> response = post("/projetos",
            payloadProjeto("prof.responsavel@cps.sp.gov.br"), tokenAluno);
        assertThat(response.getStatusCode().value()).isEqualTo(201);
        return json(response).path("id").asText();
    }

    @Nested
    @DisplayName("GET /projetos/pendentes — por e-mail do responsável (sem barreira de vínculo)")
    class FilaPendentes {

        @Test
        @DisplayName("Responsável vinculado vê o pendente (200, com e-mail interno + nome)")
        void responsavelVinculadoVePendente() {
            String id = submeterParaResponsavel();

            ResponseEntity<String> response = get("/projetos/pendentes", tokenResponsavel);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).contains(id);
            // Visão interna autenticada mantém o e-mail operacional + nome da conta.
            assertThat(jsonList(response).get(0))
                .containsEntry("emailProfessorResponsavel", "prof.responsavel@cps.sp.gov.br")
                .containsEntry("nomeProfessorResponsavel", "Prof Responsavel");
        }

        @Test
        @DisplayName("Responsável SEM vínculo também vê o pendente (200 — vínculo não é barreira)")
        void responsavelSemVinculoVePendente() {
            submeterParaResponsavel();
            User semVinculo = criarUsuario("Prof Sem Vinculo", "prof.semvinculo@cps.sp.gov.br", UserRole.PROFESSOR);
            // Indica o sem-vínculo como responsável de outro projeto.
            ResponseEntity<String> criado = post("/projetos", payloadProjeto("prof.semvinculo@cps.sp.gov.br"), tokenAluno);
            assertThat(criado.getStatusCode().value()).isEqualTo(201);
            String id = json(criado).path("id").asText();

            ResponseEntity<String> response = get("/projetos/pendentes", tokenDe(semVinculo));

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).contains(id);
        }

        @Test
        @DisplayName("Vinculado mas NÃO responsável não vê o projeto (fila vazia)")
        void vinculadoNaoResponsavelFilaVazia() {
            submeterParaResponsavel();
            User outro = criarUsuario("Prof Outro", "prof.outro@cps.sp.gov.br", UserRole.PROFESSOR);
            put("/professores/me/instituicoes",
                Map.of("instituicaoIds", List.of(instituicao.getId().toString())), tokenDe(outro));

            ResponseEntity<String> response = get("/projetos/pendentes", tokenDe(outro));

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(jsonList(response)).isEmpty();
        }

        @Test
        @DisplayName("Aluno recebe 403 na fila")
        void alunoRecebe403NaFila() {
            assertAcessoNegado(get("/projetos/pendentes", tokenAluno));
        }
    }

    @Nested
    @DisplayName("PATCH aprovar/rejeitar — permissão e estados")
    class Avaliacao {

        @Test
        @DisplayName("Responsável vinculado aprova (200, APROVADO)")
        void aprovaCaminhoFeliz() {
            String id = submeterParaResponsavel();

            ResponseEntity<String> response = patch("/projetos/" + id + "/aprovar", Map.of(), tokenResponsavel);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(texto(response, "estado")).isEqualTo("APROVADO");
        }

        @Test
        @DisplayName("Só-responsável (sem vínculo) aprova (200 — vínculo não é barreira)")
        void soResponsavelSemVinculoAprova() {
            User soResponsavel = criarUsuario("Prof So Resp", "prof.soresp@cps.sp.gov.br", UserRole.PROFESSOR);
            String id = json(post("/projetos", payloadProjeto("prof.soresp@cps.sp.gov.br"), tokenAluno))
                .path("id").asText();

            ResponseEntity<String> response = patch("/projetos/" + id + "/aprovar", Map.of(), tokenDe(soResponsavel));

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(texto(response, "estado")).isEqualTo("APROVADO");
        }

        @Test
        @DisplayName("Só-vinculado (não responsável) recebe 403 ao aprovar")
        void soVinculadoNaoResponsavel403() {
            String id = submeterParaResponsavel();
            User outro = criarUsuario("Prof Vinculado", "prof.vinculado@cps.sp.gov.br", UserRole.PROFESSOR);
            put("/professores/me/instituicoes",
                Map.of("instituicaoIds", List.of(instituicao.getId().toString())), tokenDe(outro));

            assertAcessoNegado(patch("/projetos/" + id + "/aprovar", Map.of(), tokenDe(outro)));
        }

        @Test
        @DisplayName("Re-avaliar devolve 409 (aprovar aprovado)")
        void reavaliarDevolve409() {
            String id = submeterParaResponsavel();
            patch("/projetos/" + id + "/aprovar", Map.of(), tokenResponsavel);

            ResponseEntity<String> response = patch("/projetos/" + id + "/aprovar", Map.of(), tokenResponsavel);

            assertThat(response.getStatusCode().value()).isEqualTo(409);
        }

        @Test
        @DisplayName("Rejeitar sem motivo devolve 400 e mantém pendente")
        void rejeitarSemMotivo400() {
            String id = submeterParaResponsavel();

            ResponseEntity<String> response = patch("/projetos/" + id + "/rejeitar", Map.of(), tokenResponsavel);

            assertThat(response.getStatusCode().value()).isEqualTo(400);
            assertThat(jsonList(get("/projetos/pendentes", tokenResponsavel)))
                .anySatisfy(p -> assertThat(p.get("id")).isEqualTo(id));
        }

        @Test
        @DisplayName("Rejeitar com motivo devolve 200 REJEITADO e sai da fila")
        void rejeitaCaminhoFeliz() {
            String id = submeterParaResponsavel();

            ResponseEntity<String> response = patch("/projetos/" + id + "/rejeitar",
                Map.of("motivo", "Fora do escopo do curso"), tokenResponsavel);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(texto(response, "estado")).isEqualTo("REJEITADO");
            assertThat(jsonList(get("/projetos/pendentes", tokenResponsavel))).isEmpty();
        }
    }

    @Nested
    @DisplayName("Visão pública (LGPD) — nome, nunca e-mail")
    class VisaoPublica {

        @Test
        @DisplayName("Vitrine pública não contém e-mail do professor")
        void vitrineSemEmail() {
            String id = submeterParaResponsavel();
            patch("/projetos/" + id + "/aprovar", Map.of(), tokenResponsavel);

            ResponseEntity<String> response = get("/projetos/publicos");

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).doesNotContain("prof.responsavel@cps.sp.gov.br");
            assertThat(response.getBody()).contains("Prof Responsavel");
        }

        @Test
        @DisplayName("GET por id não expõe e-mail (só nome)")
        void detalheSemEmail() {
            String id = submeterParaResponsavel();
            patch("/projetos/" + id + "/aprovar", Map.of(), tokenResponsavel);

            ResponseEntity<String> response = get("/projetos/" + id);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(json(response).path("emailProfessorResponsavel").isNull()).isTrue();
            assertThat(json(response).path("nomeProfessorResponsavel").asText()).isEqualTo("Prof Responsavel");
        }
    }
}
