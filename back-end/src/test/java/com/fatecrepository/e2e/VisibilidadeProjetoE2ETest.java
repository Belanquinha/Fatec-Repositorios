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
 * Pedido do usuário: "com a url qualquer usuario pode ver o projeto mesmo sem
 * ele estar aprovado ainda".
 *
 * <p>Antes disto, {@code SecurityConfig} tinha {@code GET /projetos/**} como
 * {@code permitAll()} — e {@code ProjetoService.obterPorId} devolvia o projeto
 * em qualquer estado. Bastava saber o UUID para ler um projeto pendente de
 * aprovação (ou rejeitado, com o motivo da rejeição), sem sessão.
 *
 * <p>Regra agora: o detalhe é público <b>somente quando o projeto está
 * APROVADO</b>. Fora disso o acesso é de professor (revisão/avestão) ou do
 * autor (acompanhamento) — visitante anônimo, aluno que não é o autor e ADMIN
 * recebem 404 (não 403: 403 confirmaria que o projeto existe).
 *
 * <p>O 404 para "não existe ou você não pode ver" é proposital: um 403
 * diferenciaria "projeto inexistente" de "projeto existe mas não é seu", e a
 * existência de um UUID da plataforma é informação sensível em catálogo acadêmico.
 */
@DisplayName("Visibilidade do detalhe do projeto (GET /projetos/{id})")
class VisibilidadeProjetoE2ETest extends ApiE2ETestSupport {

    private User aluno;
    private String tokenAluno;
    private User professor;
    private String tokenProfessor;
    private com.fatecrepository.model.Instituicao instituicao;
    private java.util.UUID cursoId;

    @BeforeEach
    void prepararCenario() {
        aluno = criarUsuario("Aluno Visibilidade", "aluno.visibilidade@aluno.cps.sp.gov.br", UserRole.ALUNO);
        tokenAluno = tokenDe(aluno);
        professor = criarUsuario("Prof Visibilidade", "prof.visibilidade@cps.sp.gov.br", UserRole.PROFESSOR);
        tokenProfessor = tokenDe(professor);
        instituicao = criarInstituicao("810", "Fatec Visibilidade");
        cursoId = criarCursoVinculado(instituicao, "Análise e Desenvolvimento de Sistemas").getId();
    }

    private Map<String, Object> payloadPendente() {
        Map<String, Object> body = new HashMap<>();
        body.put("titulo", "Projeto Visibilidade");
        body.put("descricaoCurta", "Submetido e ainda nao aprovado");
        body.put("conteudoEditorJs", "{\"blocks\":[]}");
        body.put("instituicaoId", instituicao.getId().toString());
        body.put("cursoId", cursoId.toString());
        body.put("emailProfessorResponsavel", professor.getEmail());
        body.put("integrantes", List.of(Map.of("nome", "Ana Integrante")));
        return body;
    }

    private String submeterPendente() {
        ResponseEntity<String> response = post("/projetos", payloadPendente(), tokenAluno);
        assertThat(response.getStatusCode().value()).isEqualTo(201);
        return json(response).path("id").asText();
    }

    @Nested
    @DisplayName("Projeto pendente de aprovação não é público")
    class ProjetoPendenteNaoEPublico {

        @Test
        @DisplayName("visitante anônimo recebe 404 (não 200, não 401)")
        void anonimoNaoVePendente() {
            String id = submeterPendente();

            ResponseEntity<String> response = get("/projetos/" + id);

            assertThat(response.getStatusCode().value())
                .as("Bastar a URL não pode abrir projeto pendente: a vitrine pública é que divulga aprovados")
                .isEqualTo(404);
        }

        @Test
        @DisplayName("aluno que não é o autor recebe 404")
        void outroAlunoNaoVePendente() {
            String id = submeterPendente();
            String tokenOutro = tokenPara("Aluno Outro", "aluno.outro@aluno.cps.sp.gov.br", UserRole.ALUNO);

            assertThat(get("/projetos/" + id, tokenOutro).getStatusCode().value()).isEqualTo(404);
        }

        @Test
        @DisplayName("ADMIN não vê pendente (não é autor nem professor da revisão)")
        void adminNaoVePendente() {
            String id = submeterPendente();
            // ADMIN é domínio de gestão do catálogo; o e-mail segue a regra de
            // provisionamento (UserProvisioningService), não um papel inventado.
            String tokenAdmin = tokenPara("Admin Visibilidade", "admin.visibilidade@cps.sp.gov.br", UserRole.ADMIN);

            assertThat(get("/projetos/" + id, tokenAdmin).getStatusCode().value()).isEqualTo(404);
        }

        @Test
        @DisplayName("professor responsável vê o pendente para revisão")
        void professorResponsavelVePendente() {
            String id = submeterPendente();

            ResponseEntity<String> response = get("/projetos/" + id, tokenProfessor);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(json(response).path("estado").asText()).isEqualTo("AGUARDANDO_APROVACAO");
        }

        @Test
        @DisplayName("autor acompanha o próprio projeto pendente")
        void autorVeProprioPendente() {
            String id = submeterPendente();

            assertThat(get("/projetos/" + id, tokenAluno).getStatusCode().value()).isEqualTo(200);
        }
    }

    @Nested
    @DisplayName("Projeto rejeitado também não é público")
    class ProjetoRejeitadoNaoEPublico {

        private String submeterERejeitar() {
            String id = submeterPendente();
            ResponseEntity<String> rejeicao = patch("/projetos/" + id + "/rejeitar",
                Map.of("motivo", "Fora do escopo do curso"), tokenProfessor);
            assertThat(rejeicao.getStatusCode().value()).isEqualTo(200);
            return id;
        }

        @Test
        @DisplayName("visitante anônimo recebe 404 (nem o motivo vaza)")
        void anonimoNaoVeRejeitado() {
            String id = submeterERejeitar();

            ResponseEntity<String> response = get("/projetos/" + id);

            assertThat(response.getStatusCode().value()).isEqualTo(404);
            assertThat(response.getBody()).doesNotContain("Fora do escopo do curso");
        }

        @Test
        @DisplayName("outro aluno recebe 404")
        void outroAlunoNaoVeRejeitado() {
            String id = submeterERejeitar();
            String tokenOutro = tokenPara("Aluno Espiao", "aluno.espiao@aluno.cps.sp.gov.br", UserRole.ALUNO);

            assertThat(get("/projetos/" + id, tokenOutro).getStatusCode().value()).isEqualTo(404);
        }

        @Test
        @DisplayName("professor responsável vê o rejeitado com o motivo")
        void professorResponsavelVeRejeitado() {
            String id = submeterERejeitar();

            ResponseEntity<String> response = get("/projetos/" + id, tokenProfessor);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(json(response).path("motivoRejeicao").asText()).isEqualTo("Fora do escopo do curso");
        }
    }

    @Nested
    @DisplayName("Projeto aprovado continua público (vitrine)")
    class ProjetoAprovadoEPublico {

        @Test
        @DisplayName("visitante anônimo lê o aprovado (catálogo público)")
        void anonimoVeAprovado() {
            String id = submeterPendente();
            assertThat(patch("/projetos/" + id + "/aprovar", Map.of(), tokenProfessor).getStatusCode().value())
                .isEqualTo(200);

            ResponseEntity<String> response = get("/projetos/" + id);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(json(response).path("estado").asText()).isEqualTo("APROVADO");
        }

        @Test
        @DisplayName("o detalhe público não expõe e-mail do professor (LGPD)")
        void detalhePublicoSemEmail() {
            String id = submeterPendente();
            patch("/projetos/" + id + "/aprovar", Map.of(), tokenProfessor);

            assertThat(json(get("/projetos/" + id)).path("emailProfessorResponsavel").isNull()).isTrue();
        }
    }

    @Nested
    @DisplayName("A fila de pendentes do professor não é afetada")
    class FilaDePendentesPermanece {

        @Test
        @DisplayName("GET /projetos/pendentes continua restrito a PROFESSOR")
        void pendentesContinuaRestrito() {
            submeterPendente();

            assertAcessoNegado(get("/projetos/pendentes", tokenAluno));
            assertThat(get("/projetos/pendentes", tokenProfessor).getStatusCode().value()).isEqualTo(200);
        }
    }
}
