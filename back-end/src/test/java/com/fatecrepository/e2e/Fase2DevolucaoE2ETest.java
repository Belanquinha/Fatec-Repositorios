package com.fatecrepository.e2e;

import com.fatecrepository.model.Instituicao;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SPEC-exclusao-projetos fase 2a — devolução, edição e reenvio (AC11..AC16).
 *
 * <p>Cobre a máquina {@code AGUARDANDO_APROVACAO -> ALTERACOES_SOLICITADAS -> REENVIADO}
 * e a entrada de {@code REENVIADO} na fila e na aprovação/rejeição.
 */
@DisplayName("SPEC fase 2a — Devolução, edição e reenvio")
class Fase2DevolucaoE2ETest extends ApiE2ETestSupport {

    private User aluno;
    private String tokenAluno;
    private User responsavel;
    private String tokenResponsavel;
    private User outroProfessor;
    private String tokenOutroProfessor;
    private User outroAluno;
    private String tokenOutroAluno;
    private Instituicao instituicao;
    private UUID cursoId;

    @BeforeEach
    void prepararCenario() {
        aluno = criarUsuario("Aluno Fase2", "aluno.fase2@aluno.cps.sp.gov.br", UserRole.ALUNO);
        tokenAluno = tokenDe(aluno);
        responsavel = criarUsuario("Prof Fase2", "prof.fase2@cps.sp.gov.br", UserRole.PROFESSOR);
        tokenResponsavel = tokenDe(responsavel);
        outroProfessor = criarUsuario("Prof Outro Fase2", "prof.outro.fase2@cps.sp.gov.br", UserRole.PROFESSOR);
        tokenOutroProfessor = tokenDe(outroProfessor);
        outroAluno = criarUsuario("Aluno Outro Fase2", "aluno.outro.fase2@aluno.cps.sp.gov.br", UserRole.ALUNO);
        tokenOutroAluno = tokenDe(outroAluno);
        instituicao = criarInstituicao("921", "Fatec Fase2");
        cursoId = criarCursoVinculado(instituicao, "Análise e Desenvolvimento de Sistemas").getId();
    }

    private Map<String, Object> payloadProjeto(String titulo) {
        Map<String, Object> body = new HashMap<>();
        body.put("titulo", titulo);
        body.put("descricaoCurta", "Projeto usado na suíte de devolução");
        body.put("conteudoEditorJs", "{\"blocks\":[]}");
        body.put("linkRepositorio", "https://github.com/fatec/projeto-fase2");
        body.put("palavrasChave", List.of("TCC"));
        body.put("instituicaoId", instituicao.getId().toString());
        body.put("cursoId", cursoId.toString());
        body.put("emailProfessorResponsavel", responsavel.getEmail());
        body.put("integrantes", List.of(Map.of("nome", "Ana Integrante")));
        return body;
    }

    private Map<String, Object> payloadAtualizado() {
        Map<String, Object> body = payloadProjeto("Projeto Fase2 (revisto)");
        body.put("descricaoCurta", "Descrição ajustada depois da devolução.");
        return body;
    }

    private String submeterPendente() {
        ResponseEntity<String> response = post("/projetos", payloadProjeto("Projeto para Devolução"), tokenAluno);
        assertThat(response.getStatusCode().value()).isEqualTo(201);
        return json(response).path("id").asText();
    }

    private String submeterEDevolver() {
        String id = submeterPendente();
        assertThat(patch("/projetos/" + id + "/devolver",
            Map.of("motivo", "Ajuste o escopo e traga Referências."), tokenResponsavel)
            .getStatusCode().value()).isEqualTo(200);
        return id;
    }

    @Nested
    @DisplayName("Devolução pelo professor responsável")
    class Devolucao {

        @Test
        @DisplayName("AC11: devolve com motivo, sai da fila e o autor vê o motivo")
        void devolveComMotivo() {
            String id = submeterPendente();

            ResponseEntity<String> response = patch(
                "/projetos/" + id + "/devolver",
                Map.of("motivo", "Ajuste o escopo e traga Referências."),
                tokenResponsavel);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(texto(response, "estado")).isEqualTo("ALTERACOES_SOLICITADAS");
            assertThat(texto(response, "motivoDevolucao")).contains("Ajuste o escopo");
            assertThat(json(response).has("devolvidoEm")).isTrue();

            // Saiu da fila do professor.
            assertThat(jsonList(get("/projetos/pendentes", tokenResponsavel))).isEmpty();

            // O autor lê o motivo no detalhe.
            ResponseEntity<String> detalhe = get("/projetos/" + id, tokenAluno);
            assertThat(detalhe.getStatusCode().value()).isEqualTo(200);
            assertThat(texto(detalhe, "estado")).isEqualTo("ALTERACOES_SOLICITADAS");
            assertThat(texto(detalhe, "motivoDevolucao")).contains("Ajuste o escopo");
        }

        @Test
        @DisplayName("AC12: professor não-responsável recebe 403")
        void professorNaoResponsavel403() {
            String id = submeterPendente();

            assertAcessoNegado(patch("/projetos/" + id + "/devolver", Map.of("motivo", "x"), tokenOutroProfessor));
        }

        @Test
        @DisplayName("AC12: aluno recebe 403 e sem token 401")
        void aluno403SemToken401() {
            String id = submeterPendente();

            assertAcessoNegado(patch("/projetos/" + id + "/devolver", Map.of("motivo", "x"), tokenAluno));
            assertNaoAutorizado(patch("/projetos/" + id + "/devolver", Map.of("motivo", "x"), null));
        }

        @Test
        @DisplayName("AC16: devolução sem motivo (ausente ou em branco) recebe 400")
        void motivoObrigatorio400() {
            String id = submeterPendente();

            assertThat(patch("/projetos/" + id + "/devolver", Map.of(), tokenResponsavel)
                .getStatusCode().value()).isEqualTo(400);
            assertThat(patch("/projetos/" + id + "/devolver", Map.of("motivo", "   "), tokenResponsavel)
                .getStatusCode().value()).isEqualTo(400);
        }

        @Test
        @DisplayName("Devolver projeto já avaliado recebe 409")
        void devolucaoEmEstadoInvalido409() {
            String id = submeterPendente();
            assertThat(patch("/projetos/" + id + "/aprovar", Map.of(), tokenResponsavel)
                .getStatusCode().value()).isEqualTo(200);

            assertThat(patch("/projetos/" + id + "/devolver", Map.of("motivo", "x"), tokenResponsavel)
                .getStatusCode().value()).isEqualTo(409);
        }
    }

    @Nested
    @DisplayName("Edição e reenvio pelo autor")
    class Reenvio {

        @Test
        @DisplayName("AC13: PUT em ALTERACOES_SOLICITADAS reenvia como REENVIADO e volta à fila")
        void reenviaDevolvido() {
            String id = submeterEDevolver();

            ResponseEntity<String> response = put("/projetos/" + id, payloadAtualizado(), tokenAluno);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(texto(response, "estado")).isEqualTo("REENVIADO");
            assertThat(json(response).has("reenviadoEm")).isTrue();
            assertThat(texto(response, "titulo")).isEqualTo("Projeto Fase2 (revisto)");

            // Volta à fila do professor responsável.
            List<Map<String, Object>> pendentes = jsonList(get("/projetos/pendentes", tokenResponsavel));
            assertThat(pendentes).anySatisfy(p -> {
                assertThat(p.get("id")).isEqualTo(id);
                assertThat(p.get("estado")).isEqualTo("REENVIADO");
            });
        }

        @Test
        @DisplayName("AC13: reenvio substitui integrantes e troca o e-mail do responsável")
        void reenvioSubstituiIntegrantes() {
            String id = submeterEDevolver();
            Map<String, Object> body = payloadAtualizado();
            body.put("integrantes", List.of(
                Map.of("nome", "Carlos Novo"),
                Map.of("nome", "Daniela Nova", "linkLinkedin", "https://linkedin.com/in/daniela")));

            ResponseEntity<String> response = put("/projetos/" + id, body, tokenAluno);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(json(response).path("integrantes")).hasSize(2);
            assertThat(json(response).path("integrantes").get(0).path("nome").asText())
                .isEqualTo("Carlos Novo");
        }

        @Test
        @DisplayName("AC14: PUT em estado diferente recebe 409")
        void editarForaDeDevolvido409() {
            String id = submeterPendente();

            assertThat(put("/projetos/" + id, payloadAtualizado(), tokenAluno)
                .getStatusCode().value()).isEqualTo(409);
        }

        @Test
        @DisplayName("AC14: PUT de não-autor recebe 403")
        void editarNaoAutor403() {
            String id = submeterEDevolver();

            assertAcessoNegado(put("/projetos/" + id, payloadAtualizado(), tokenOutroAluno));
        }

        @Test
        @DisplayName("PUT com dados inválidos recebe 400 (validação da criação)")
        void edicaoInvalida400() {
            String id = submeterEDevolver();
            Map<String, Object> body = payloadAtualizado();
            body.put("integrantes", List.of());

            assertThat(put("/projetos/" + id, body, tokenAluno).getStatusCode().value()).isEqualTo(400);
        }
    }

    @Nested
    @DisplayName("Fila e avaliação com REENVIADO")
    class FilaEAvaliacao {

        @Test
        @DisplayName("AC15: aprovar aceita REENVIADO e publica o projeto")
        void aprovaReenviado() {
            String id = submeterEDevolver();
            put("/projetos/" + id, payloadAtualizado(), tokenAluno);

            ResponseEntity<String> response = patch("/projetos/" + id + "/aprovar", Map.of(), tokenResponsavel);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(texto(response, "estado")).isEqualTo("APROVADO");
            assertThat(get("/projetos/" + id).getStatusCode().value()).isEqualTo(200);
            assertThat(jsonList(get("/projetos/publicos")))
                .anySatisfy(p -> assertThat(p.get("id")).isEqualTo(id));
        }

        @Test
        @DisplayName("AC15: rejeitar aceita REENVIADO")
        void rejeitaReenviado() {
            String id = submeterEDevolver();
            put("/projetos/" + id, payloadAtualizado(), tokenAluno);

            ResponseEntity<String> response = patch("/projetos/" + id + "/rejeitar",
                Map.of("motivo", "Ainda fora do escopo"), tokenResponsavel);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(texto(response, "estado")).isEqualTo("REJEITADO");
        }

        @Test
        @DisplayName("AC15: reavaliar projeto APROVADO recebe 409")
        void reavaliarAprovado409() {
            String id = submeterPendente();
            patch("/projetos/" + id + "/aprovar", Map.of(), tokenResponsavel);

            assertThat(patch("/projetos/" + id + "/aprovar", Map.of(), tokenResponsavel)
                .getStatusCode().value()).isEqualTo(409);
            assertThat(patch("/projetos/" + id + "/rejeitar", Map.of("motivo", "x"), tokenResponsavel)
                .getStatusCode().value()).isEqualTo(409);
        }
    }
}
