package com.fatecrepository.e2e;

import com.fatecrepository.model.UserRole;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Story 2.1 — Admin mantém o catálogo de instituições.
 *
 * <p>AC: "consigo adicionar, editar e remover instituições (CRUD simples no MVP)"
 * AC: "sem papel admin, qualquer alteração de catálogo é negada (403)"  (a parte de
 *     403 está em {@code AutorizacaoPorPapelE2ETest})
 * NFR4 — instituições são dado de catálogo; nada de texto livre.
 *
 * <p>Esta é a story em andamento no sprint (status {@code in-progress}), então a
 * suíte precisa provar o ciclo completo de vida do registro, não só a criação.
 */
@DisplayName("Story 2.1 — CRUD do catálogo de instituições")
class CatalogoInstituicoesE2ETest extends ApiE2ETestSupport {

    private String tokenAdmin;

    @BeforeEach
    void criarAdmin() {
        tokenAdmin = tokenPara("Admin E2E", "admin.e2e@cps.sp.gov.br", UserRole.ADMIN);
    }

    private Map<String, Object> payload(String codigo, String nome) {
        Map<String, Object> body = new HashMap<>();
        body.put("codigoUnidade", codigo);
        body.put("nome", nome);
        body.put("cidade", "São Paulo");
        body.put("estado", "SP");
        body.put("ativo", true);
        return body;
    }

    @Nested
    @DisplayName("AC — posso adicionar, editar e remover instituições")
    class CicloDeVidaCompleto {

        @Test
        @DisplayName("Criar devolve 201 e o registro fica consultável")
        void criarInstituicao() {
            ResponseEntity<String> response = post("/instituicoes", payload("301", "Fatec E2E Criada"), tokenAdmin);

            assertThat(response.getStatusCode().value()).isEqualTo(201);
            JsonNode body = json(response);
            assertThat(body.path("codigoUnidade").asText()).isEqualTo("301");
            assertThat(body.path("nome").asText()).isEqualTo("Fatec E2E Criada");
            assertThat(body.path("ativo").asBoolean()).isTrue();

            String id = body.path("id").asText();
            ResponseEntity<String> consulta = get("/instituicoes/" + id);
            assertThat(consulta.getStatusCode().value()).isEqualTo(200);
            assertThat(texto(consulta, "nome")).isEqualTo("Fatec E2E Criada");
        }

        @Test
        @DisplayName("Editar devolve 200 e persiste a alteração")
        void editarInstituicao() {
            String id = json(post("/instituicoes", payload("302", "Fatec E2E Original"), tokenAdmin))
                .path("id").asText();

            ResponseEntity<String> response = put("/instituicoes/" + id,
                payload("302", "Fatec E2E Editada"), tokenAdmin);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(texto(response, "nome")).isEqualTo("Fatec E2E Editada");
            assertThat(texto(get("/instituicoes/" + id), "nome")).isEqualTo("Fatec E2E Editada");
        }

        @Test
        @DisplayName("Remover devolve 204 e o registro deixa de existir")
        void removerInstituicao() {
            String id = json(post("/instituicoes", payload("303", "Fatec E2E Temporária"), tokenAdmin))
                .path("id").asText();

            ResponseEntity<String> response = delete("/instituicoes/" + id, tokenAdmin);

            assertThat(response.getStatusCode().value()).isEqualTo(204);
            assertThat(response.getBody()).isNull();
            assertThat(get("/instituicoes/" + id).getStatusCode().value()).isEqualTo(404);
        }

        @Test
        @DisplayName("Ciclo completo criar → editar → remover numa única instituição")
        void cicloCompleto() {
            String id = json(post("/instituicoes", payload("304", "Fatec Ciclo"), tokenAdmin))
                .path("id").asText();
            assertThat(get("/instituicoes/" + id).getStatusCode().value()).isEqualTo(200);

            assertThat(put("/instituicoes/" + id, payload("304", "Fatec Ciclo Editada"), tokenAdmin)
                .getStatusCode().value()).isEqualTo(200);
            assertThat(texto(get("/instituicoes/" + id), "nome")).isEqualTo("Fatec Ciclo Editada");

            assertThat(delete("/instituicoes/" + id, tokenAdmin).getStatusCode().value()).isEqualTo(204);
            assertThat(get("/instituicoes/" + id).getStatusCode().value()).isEqualTo(404);
        }
    }

    @Nested
    @DisplayName("Regras de negócio do catálogo")
    class RegrasDeNegocio {

        @Test
        @DisplayName("Código da unidade duplicado devolve 400")
        void codigoDuplicadoDevolve400() {
            post("/instituicoes", payload("305", "Fatec E2E Original"), tokenAdmin);

            ResponseEntity<String> response = post("/instituicoes", payload("305", "Fatec E2E Duplicada"), tokenAdmin);

            assertThat(response.getStatusCode().value()).isEqualTo(400);
            assertThat(texto(response, "mensagem")).contains("305");
        }

        @Test
        @DisplayName("Editar mantendo o próprio código é permitido")
        void editarMantendoCodigo() {
            String id = json(post("/instituicoes", payload("306", "Fatec E2E Mesma"), tokenAdmin))
                .path("id").asText();

            ResponseEntity<String> response = put("/instituicoes/" + id, payload("306", "Fatec E2E Renomeada"), tokenAdmin);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(texto(response, "nome")).isEqualTo("Fatec E2E Renomeada");
        }

        @Test
        @DisplayName("Editar para o código de outra instituição devolve 400")
        void editarParaCodigoConcorrente() {
            post("/instituicoes", payload("307", "Fatec E2E A"), tokenAdmin);
            String idB = json(post("/instituicoes", payload("308", "Fatec E2E B"), tokenAdmin))
                .path("id").asText();

            ResponseEntity<String> response = put("/instituicoes/" + idB, payload("307", "Fatec E2E B"), tokenAdmin);

            assertThat(response.getStatusCode().value()).isEqualTo(400);
        }

        @Test
        @DisplayName("Instituição ausente devolve 404 na leitura, edição e remoção")
        void instituicaoInexistenteDevolve404() {
            UUID fantasma = UUID.randomUUID();

            assertThat(get("/instituicoes/" + fantasma).getStatusCode().value()).isEqualTo(404);
            assertThat(put("/instituicoes/" + fantasma, payload("309", "Fatec"), tokenAdmin)
                .getStatusCode().value()).isEqualTo(404);
            assertThat(delete("/instituicoes/" + fantasma, tokenAdmin).getStatusCode().value()).isEqualTo(404);
        }

        @Test
        @DisplayName("Estado em branco assume o default SP")
        void estadoEmBrancoAssumeSp() {
            Map<String, Object> body = new HashMap<>();
            body.put("codigoUnidade", "310");
            body.put("nome", "Fatec E2E Sem Estado");
            body.put("estado", "");

            ResponseEntity<String> response = post("/instituicoes", body, tokenAdmin);

            assertThat(response.getStatusCode().value()).isEqualTo(201);
            assertThat(texto(response, "estado")).isEqualTo("SP");
        }
    }

    @Nested
    @DisplayName("Validação do payload (NFR4 — sem entrada livre)")
    class ValidacaoDoPayload {

        @Test
        @DisplayName("Código da unidade ausente devolve 400 com erro por campo")
        void codigoAusente() {
            Map<String, Object> body = new HashMap<>();
            body.put("nome", "Fatec E2E Sem Código");

            ResponseEntity<String> response = post("/instituicoes", body, tokenAdmin);

            assertThat(response.getStatusCode().value()).isEqualTo(400);
            assertThat(json(response).path("erros").has("codigoUnidade")).isTrue();
        }

        @Test
        @DisplayName("Nome ausente devolve 400 com erro por campo")
        void nomeAusente() {
            Map<String, Object> body = new HashMap<>();
            body.put("codigoUnidade", "311");

            ResponseEntity<String> response = post("/instituicoes", body, tokenAdmin);

            assertThat(response.getStatusCode().value()).isEqualTo(400);
            assertThat(json(response).path("erros").has("nome")).isTrue();
        }

        @Test
        @DisplayName("Código e nome em branco contam como ausentes")
        void camposEmBranco() {
            Map<String, Object> body = new HashMap<>();
            body.put("codigoUnidade", "   ");
            body.put("nome", "   ");

            ResponseEntity<String> response = post("/instituicoes", body, tokenAdmin);

            assertThat(response.getStatusCode().value()).isEqualTo(400);
            JsonNode erros = json(response).path("erros");
            assertThat(erros.has("codigoUnidade")).isTrue();
            assertThat(erros.has("nome")).isTrue();
        }
    }

    @Nested
    @DisplayName("Leitura do catálogo")
    class LeituraDoCatalogo {

        @Test
        @DisplayName("A listagem devolve todas as instituições cadastradas")
        void listarTodas() {
            criarInstituicao("320", "Fatec E2E Listada 1");
            criarInstituicao("321", "Fatec E2E Listada 2");

            ResponseEntity<String> response = get("/instituicoes");

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).contains("Fatec E2E Listada 1", "Fatec E2E Listada 2");
        }

        @Test
        @DisplayName("Catálogo vazio devolve lista vazia, não erro")
        void catalogoVazio() {
            ResponseEntity<String> response = get("/instituicoes");

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).isEqualTo("[]");
        }
    }
}